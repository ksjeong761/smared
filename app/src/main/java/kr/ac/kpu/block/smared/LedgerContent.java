package kr.ac.kpu.block.smared;

import java.util.HashMap;

public class LedgerContent {
    private String useItem;
    private String price;
    private String payMemo;

    public String getPrice() {
        return price;
    }
    public void setPrice(String price) {
        this.price = price;
    }

    public String getPayMemo() {
        return payMemo;
    }
    public void setPayMemo(String payMemo) {
        this.payMemo = payMemo;
    }

    public String getUseItem() {
        return useItem;
    }
    public void setUseItem(String useItem) { this.useItem = useItem; }

    public LedgerContent() {
        // Default constructor required for calls to DataSnapshot.getValue(Comment.class)
        this.useItem = "";
        this.price = "";
        this.payMemo = "";
    }

    public LedgerContent(String useItem, String price, String payMemo) {
        this.useItem = useItem;
        this.price = price;
        this.payMemo = payMemo;
    }

    public HashMap<String, String> toHashMap() {
        HashMap<String, String> hashMap = new HashMap<>();

        hashMap.put("useItem", useItem);
        hashMap.put("price", price);
        hashMap.put("paymemo", payMemo);

        return hashMap;
    }
}


