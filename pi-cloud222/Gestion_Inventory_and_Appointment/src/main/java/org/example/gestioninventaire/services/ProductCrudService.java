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
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductCrudService {

    private static final Path PRODUCT_UPLOAD_DIR = Paths.get("uploads", "products");

    private final InventoryProductRepository productRepository;
    private final InventoryMapper inventoryMapper;
    private final StockMovementRepository stockMovementRepository;
    private final BatchRepository batchRepository;

    public InventoryProductResponse create(CreateProductRequest request) {
        if (productRepository.existsByNomAndOwnerId(request.getNom(), request.getOwnerId())) {
            throw new BadRequestException("Un produit avec ce nom existe deja pour cet utilisateur");
        }

        InventoryProduct product = InventoryProduct.builder()
                .nom(request.getNom())
                .categorie(request.getCategorie())
                .unit(request.getUnit())
                .isPerishable(request.getIsPerishable())
                .currentQuantity(request.getCurrentQuantity())
                .minThreshold(request.getMinThreshold())
                .dateAchat(request.getDateAchat())
                .datePeremption(request.getDatePeremption())
                .prixAchat(request.getPrixAchat())
                .note(request.getNote())
                .ownerId(request.getOwnerId())
                .build();

        productRepository.save(product);
        return inventoryMapper.toProductResponse(product);
    }

    public InventoryProductResponse update(Long id, UpdateProductRequest request) {
        InventoryProduct product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Produit non trouve"));

        product.setNom(request.getNom());
        product.setCategorie(request.getCategorie());
        product.setUnit(request.getUnit());
        product.setIsPerishable(request.getIsPerishable());
        product.setCurrentQuantity(request.getCurrentQuantity());
        product.setMinThreshold(request.getMinThreshold());
        product.setDateAchat(request.getDateAchat());
        product.setDatePeremption(request.getDatePeremption());
        product.setPrixAchat(request.getPrixAchat());
        product.setNote(request.getNote());
        product.setPrixVente(request.getPrixVente());
        product.setImageUrl(request.getImageUrl());
        product.setDescription(request.getDescription());
        if (request.getEnBoutique() != null) {
            product.setEnBoutique(request.getEnBoutique());
        }

        productRepository.save(product);
        return inventoryMapper.toProductResponse(product);
    }

    public InventoryProductResponse getById(Long id) {
        return inventoryMapper.toProductResponse(
                productRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Produit non trouve"))
        );
    }

    public List<InventoryProductResponse> getAll() {
        return productRepository.findAll().stream().map(inventoryMapper::toProductResponse).toList();
    }

    public List<InventoryProductResponse> getByOwner(Long ownerId) {
        return productRepository.findByOwnerId(ownerId).stream().map(inventoryMapper::toProductResponse).toList();
    }

    public List<InventoryProductResponse> getPublicShopByVet(Long ownerId) {
        return productRepository.findByOwnerIdAndEnBoutiqueTrue(ownerId)
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

    public List<InventoryProductResponse> getAllPublicShop() {
        return productRepository.findByEnBoutiqueTrue()
                .stream().map(inventoryMapper::toProductResponse).toList();
    }

    public InventoryProductResponse updateBoutiqueInfo(Long id,
                                                       Double prixVente,
                                                       String description,
                                                       Boolean enBoutique,
                                                       MultipartFile image) {
        InventoryProduct product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Produit non trouve"));

        if (prixVente != null) {
            if (prixVente < 0) {
                throw new BadRequestException("Le prix de vente doit etre superieur ou egal a 0");
            }
            product.setPrixVente(prixVente);
        }
        if (description != null) {
            product.setDescription(description);
        }
        if (enBoutique != null) {
            product.setEnBoutique(enBoutique);
        }
        if (image != null && !image.isEmpty()) {
            product.setImageUrl(storeProductImage(image));
        }

        productRepository.save(product);
        return inventoryMapper.toProductResponse(product);
    }

    public InventoryProductResponse toggleBoutique(Long id) {
        InventoryProduct product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Produit non trouve"));

        boolean current = product.getEnBoutique() != null && product.getEnBoutique();
        product.setEnBoutique(!current);
        productRepository.save(product);
        return inventoryMapper.toProductResponse(product);
    }

    private String storeProductImage(MultipartFile image) {
        String originalName = image.getOriginalFilename() != null ? image.getOriginalFilename() : "product-image";
        String extension = "";
        int dotIndex = originalName.lastIndexOf('.');
        if (dotIndex >= 0 && dotIndex < originalName.length() - 1) {
            extension = "." + originalName.substring(dotIndex + 1).toLowerCase();
        }

        String storedName = UUID.randomUUID() + extension;
        try {
            Files.createDirectories(PRODUCT_UPLOAD_DIR);
            Path normalizedRoot = PRODUCT_UPLOAD_DIR.toAbsolutePath().normalize();
            Path target = normalizedRoot.resolve(storedName).normalize();
            if (!target.startsWith(normalizedRoot)) {
                throw new BadRequestException("Nom de fichier invalide");
            }
            Files.copy(image.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return "/uploads/products/" + storedName;
        } catch (IOException e) {
            throw new RuntimeException("Impossible d'enregistrer l'image du produit", e);
        }
    }
}
