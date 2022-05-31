package kr.ac.kpu.block.smared;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

public class Ledger {
    private long paymentTimestamp; // 결제 시간
    private String paymentMethod;    // 결제 수단

    private String storeName;           // 가게 이름
    private String storeAddress;        // 가게 주소

    private String description;           // 비고
    private String category;               // 소비 카테고리
    private double totalPrice;             // 총 소비 금액

    private Product[] products;         // 구매한 상품 목록

    public long getPaymentTimestamp() {
        return paymentTimestamp;
    }
    public String getPaymentTimestamp(String format) {
        LocalDateTime localDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(paymentTimestamp), TimeZone.getDefault().toZoneId());
        return localDateTime.format(DateTimeFormatter.ofPattern(format));
    }
    public void setPaymentTimestamp(long paymentTimestamp) {
        this.paymentTimestamp = paymentTimestamp;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }
    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getStoreAddress() {
        return storeAddress;
    }
    public void setStoreAddress(String storeAddress) {
        this.storeAddress = storeAddress;
    }

    public String getStoreName() {
        return storeName;
    }
    public void setStoreName(String storeName) {
        this.storeName = storeName;
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
    public void setCategory(String category) {
        this.category = category;
    }

    public Product[] getProducts() {
        return products;
    }
    public void setProducts(Product[] products) {
        this.products = products;
    }

    public double getTotalPrice() {
        return totalPrice;
    }
    public void setTotalPrice(double totalPrice) {
        this.totalPrice = totalPrice;
    }
    public void setTotalPrice(String totalPriceString) {
        this.totalPrice = Double.parseDouble(totalPriceString);
    }

    public Ledger() {
        // Default constructor required for calls to DataSnapshot.getValue(Comment.class)
        paymentTimestamp = 0;
        paymentMethod = "";

        storeName = "";
        storeAddress = "";

        description = "";
        category = "";
        totalPrice = 0;

        products = new Product[0];         // 구매한 상품 목록
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();

        map.put("paymentTimestamp", paymentTimestamp);
        map.put("paymentMethod", paymentMethod);

        map.put("storeName", storeName);
        map.put("storeAddress", storeAddress);

        map.put("description", description);
        map.put("category", category);

        map.put("productCount", products.length);
        for (int i = 0; i < products.length; i++) {
            map.put("product" + i, products[i].toMap());
        }

        return map;
    }

    public int compareDate(Ledger that) {
        LocalDate dateA = LocalDateTime.ofInstant(Instant.ofEpochMilli(this.paymentTimestamp), TimeZone.getDefault().toZoneId()).toLocalDate();
        LocalDate dateB = LocalDateTime.ofInstant(Instant.ofEpochMilli(that.paymentTimestamp), TimeZone.getDefault().toZoneId()).toLocalDate();

        return dateA.compareTo(dateB);
    }

    public int getCategoryIndex() {
        Map<String, Integer> map = new HashMap<>();
        map.put("의류비", 0);
        map.put("식비", 1);
        map.put("주거비", 2);
        map.put("교통비", 3);
        map.put("생필품", 4);
        map.put("기타", 5);

        Integer categoryIndex = map.get(category);
        return (categoryIndex != null) ? categoryIndex : -1;
    }
}
