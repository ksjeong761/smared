package kr.ac.kpu.block.smared;

import java.util.HashMap;

public class LedgerContent {
    private String category;
    private String price;
    private String description;

    public String getPrice() {
        return price;
    }
    public void setPrice(String price) {
        this.price = price;
    }

    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }
    public void setCategory(String category) { this.category = category; }

    public LedgerContent() {
        // Default constructor required for calls to DataSnapshot.getValue(Comment.class)
        this.category = "";
        this.price = "";
        this.description = "";
    }

    public LedgerContent(String category, String price, String description) {
        this.category = category;
        this.price = price;
        this.description = description;
    }

    public HashMap<String, String> toHashMap() {
        HashMap<String, String> hashMap = new HashMap<>();

        hashMap.put("category", category);
        hashMap.put("price", price);
        hashMap.put("description", description);

        return hashMap;
    }
}