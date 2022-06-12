package kr.ac.kpu.block.smared;

import java.util.HashMap;
import java.util.Map;

public class Product extends DTO {
    private String name;
    private String category;
    private int quantity;
    private double price;

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public double getPrice() {
        return price;
    }
    public void setPrice(double price) {
        this.price = price;
    }

    public int getQuantity() {
        return quantity;
    }
    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public String getCategory() {
        return category;
    }
    public void setCategory(String category) {
        this.category = category;
    }

    public Product() {
        // Default constructor required for calls to DataSnapshot.getValue(Comment.class)
    }
}