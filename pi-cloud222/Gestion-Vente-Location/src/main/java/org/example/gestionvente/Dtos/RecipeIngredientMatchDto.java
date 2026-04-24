package org.example.gestionvente.Dtos;

public class RecipeIngredientMatchDto {
    private Long produitId;
    private String productName;
    private String ingredientName;
    private Double requestedQuantityKg;
    private Double availableQuantityKg;
    private Double unitPrice;
    private Double estimatedCost;
    private boolean matched;

    public Long getProduitId() { return produitId; }
    public void setProduitId(Long produitId) { this.produitId = produitId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getIngredientName() { return ingredientName; }
    public void setIngredientName(String ingredientName) { this.ingredientName = ingredientName; }

    public Double getRequestedQuantityKg() { return requestedQuantityKg; }
    public void setRequestedQuantityKg(Double requestedQuantityKg) { this.requestedQuantityKg = requestedQuantityKg; }

    public Double getAvailableQuantityKg() { return availableQuantityKg; }
    public void setAvailableQuantityKg(Double availableQuantityKg) { this.availableQuantityKg = availableQuantityKg; }

    public Double getUnitPrice() { return unitPrice; }
    public void setUnitPrice(Double unitPrice) { this.unitPrice = unitPrice; }

    public Double getEstimatedCost() { return estimatedCost; }
    public void setEstimatedCost(Double estimatedCost) { this.estimatedCost = estimatedCost; }

    public boolean isMatched() { return matched; }
    public void setMatched(boolean matched) { this.matched = matched; }
}