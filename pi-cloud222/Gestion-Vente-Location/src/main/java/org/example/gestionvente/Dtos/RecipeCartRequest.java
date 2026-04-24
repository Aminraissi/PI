package org.example.gestionvente.Dtos;

public class RecipeCartRequest {
    private Long userId;
    private String prompt;
    private boolean addToCart;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getPrompt() { return prompt; }
    public void setPrompt(String prompt) { this.prompt = prompt; }

    public boolean isAddToCart() { return addToCart; }
    public void setAddToCart(boolean addToCart) { this.addToCart = addToCart; }
}
