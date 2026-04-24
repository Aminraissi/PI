package org.example.gestioninventaire.services;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.gestioninventaire.dtos.request.CreateProductRequest;
import org.example.gestioninventaire.dtos.request.UpdateProductRequest;
import org.example.gestioninventaire.dtos.response.InventoryProductResponse;
import org.example.gestioninventaire.entities.InventoryProduct;
import org.example.gestioninventaire.exceptions.BadRequestException;
import org.example.gestioninventaire.exceptions.ResourceNotFoundException;
import org.example.gestioninventaire.mappers.InventoryMapper;
import org.example.gestioninventaire.repositories.BatchRepository;
import org.example.gestioninventaire.repositories.InventoryProductRepository;
import org.example.gestioninventaire.repositories.StockMovementRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductCrudService {

    private final InventoryProductRepository productRepository;
    private final InventoryMapper inventoryMapper;
    private final StockMovementRepository stockMovementRepository;
    private final BatchRepository batchRepository;

    private static final String UPLOAD_DIR = "uploads/products/";

    public InventoryProductResponse create(CreateProductRequest request) {
        if (productRepository.existsByNomAndOwnerId(request.getNom(), request.getOwnerId())) {
            throw new BadRequestException("Un produit avec ce nom existe déjà pour cet utilisateur");
        }

        InventoryProduct product = InventoryProduct.builder()
                .nom(request.getNom())
                .categorie(request.getCategorie())
                .unit(request.getUnit())
                .isPerishable(request.getIsPerishable())
                .currentQuantity(request.getCurrentQuantity())
                .minThreshold(request.getMinThreshold())
                .ownerId(request.getOwnerId())
                .enBoutique(false)
                .build();

        productRepository.save(product);
        return inventoryMapper.toProductResponse(product);
    }

    public InventoryProductResponse update(Long id, UpdateProductRequest request) {
        InventoryProduct product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Produit non trouvé"));

        product.setNom(request.getNom());
        product.setCategorie(request.getCategorie());
        product.setUnit(request.getUnit());
        product.setIsPerishable(request.getIsPerishable());
        product.setCurrentQuantity(request.getCurrentQuantity());
        product.setMinThreshold(request.getMinThreshold());

        // Boutique fields (update only if provided)
        if (request.getPrixVente() != null) product.setPrixVente(request.getPrixVente());
        if (request.getDescription() != null) product.setDescription(request.getDescription());
        if (request.getImageUrl() != null) product.setImageUrl(request.getImageUrl());
        if (request.getEnBoutique() != null) product.setEnBoutique(request.getEnBoutique());

        productRepository.save(product);
        return inventoryMapper.toProductResponse(product);
    }

    /** Update boutique fields only (prix, description, image, enBoutique) */
    @Transactional
    public InventoryProductResponse updateBoutiqueInfo(Long id, Long vetId,
                                                       Double prixVente, String description, Boolean enBoutique, MultipartFile image) throws IOException {

        InventoryProduct product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Produit non trouvé"));

        if (!product.getOwnerId().equals(vetId)) {
            throw new BadRequestException("Ce produit ne vous appartient pas");
        }

        if (prixVente != null)   product.setPrixVente(prixVente);
        if (description != null) product.setDescription(description);
        if (enBoutique != null)  product.setEnBoutique(enBoutique);

        if (image != null && !image.isEmpty()) {
            String url = saveImage(image);
            product.setImageUrl(url);
        }

        productRepository.save(product);
        return inventoryMapper.toProductResponse(product);
    }

    /** Toggle enBoutique on/off */
    @Transactional
    public InventoryProductResponse toggleBoutique(Long id, Long vetId) {
        InventoryProduct product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Produit non trouvé"));

        if (!product.getOwnerId().equals(vetId)) {
            throw new BadRequestException("Ce produit ne vous appartient pas");
        }

        product.setEnBoutique(!Boolean.TRUE.equals(product.getEnBoutique()));
        productRepository.save(product);
        return inventoryMapper.toProductResponse(product);
    }

    public InventoryProductResponse getById(Long id) {
        return inventoryMapper.toProductResponse(
                productRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Produit non trouvé"))
        );
    }

    public List<InventoryProductResponse> getAll() {
        return productRepository.findAll().stream().map(inventoryMapper::toProductResponse).toList();
    }

    public List<InventoryProductResponse> getByOwner(Long ownerId) {
        return productRepository.findByOwnerId(ownerId).stream().map(inventoryMapper::toProductResponse).toList();
    }

    /** Public: produits en boutique d'un vétérinaire (pour les agriculteurs) */
    public List<InventoryProductResponse> getPublicShop(Long vetId) {
        return productRepository.findByOwnerIdAndEnBoutiqueTrue(vetId)
                .stream().map(inventoryMapper::toProductResponse).toList();
    }

    @Transactional
    public void delete(Long id) {
        InventoryProduct product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Produit introuvable"));
        stockMovementRepository.deleteByProductId(id);
        batchRepository.deleteByProductId(id);
        productRepository.delete(product);
    }

    private String saveImage(MultipartFile file) throws IOException {
        Files.createDirectories(Paths.get(UPLOAD_DIR));
        String ext = StringUtils.getFilenameExtension(file.getOriginalFilename());
        String fileName = UUID.randomUUID() + "." + (ext != null ? ext : "jpg");
        Files.copy(file.getInputStream(), Paths.get(UPLOAD_DIR + fileName),
                StandardCopyOption.REPLACE_EXISTING);
        return "/inventaires/api/products/images/" + fileName;
    }
}
