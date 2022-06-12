package kr.ac.kpu.block.smared;

import com.google.firebase.database.DataSnapshot;

import java.lang.reflect.Field;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

public class Ledger extends DTO {
    private long paymentTimestamp = 0; // 결제 시간
    private String paymentMethod = "";    // 결제 수단

    private String storeName = "";           // 가게 이름
    private String storeAddress = "";        // 가게 주소
    private String storeContact = "";        // 가게 연락처

    private String totalCategory = "";        // 소비 카테고리
    private double totalPrice = 0;             // 총 소비 금액

    private String description = "";           // 비고

    private List<String> membersUid = new ArrayList<>();
    private List<String> productsUid = new ArrayList<>();

    public long getPaymentTimestamp() {
        return paymentTimestamp;
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

    public String getTotalCategory() {
        return totalCategory;
    }
    public void setTotalCategory(String totalCategory) {
        this.totalCategory = totalCategory;
    }

    public double getTotalPrice() {
        return totalPrice;
    }
    public void setTotalPrice(double totalPrice) {
        this.totalPrice = totalPrice;
    }

    public List<String> getProductsUid() {
        return productsUid;
    }
    public void setProductsUid(List<String> productsUid) {
        this.productsUid = productsUid;
    }

    public List<String> getMembersUid() {
        return membersUid;
    }
    public void setMembersUid(List<String> membersUid) {
        this.membersUid = membersUid;
    }

    public String getStoreContact() {
        return storeContact;
    }
    public void setStoreContact(String storeContact) {
        this.storeContact = storeContact;
    }

    public Ledger() {
    }

//    public Ledger(DataSnapshot ledgerUidSnapshot) {
//        for (DataSnapshot ledgerFieldSnapshot : ledgerUidSnapshot.getChildren()) {
//            String fieldName = ledgerFieldSnapshot.getKey();
//            Object fieldValue = ledgerFieldSnapshot.getValue();
//
//            Ledger ledger = new Ledger();
//
//
//
//            Field[] fields = this.getClass().getFields();
//            for (Field field : fields) {
//
//            }
//        }
//    }

    public String getFormattedTimestamp(String format) {
        LocalDateTime localDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(paymentTimestamp), TimeZone.getDefault().toZoneId());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
        return localDateTime.format(formatter);
    }

    public int compareDate(Ledger otherLedgerData) {
        LocalDate dateA = LocalDateTime.ofInstant(Instant.ofEpochMilli(this.paymentTimestamp), TimeZone.getDefault().toZoneId()).toLocalDate();
        LocalDate dateB = LocalDateTime.ofInstant(Instant.ofEpochMilli(otherLedgerData.paymentTimestamp), TimeZone.getDefault().toZoneId()).toLocalDate();

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

        Integer categoryIndex = map.get(totalCategory);
        return (categoryIndex == null) ? -1 : categoryIndex;
    }
}
