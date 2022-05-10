package kr.ac.kpu.block.smared;

import java.util.Hashtable;

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

    public Hashtable<String, String> toHashtable() {
        Hashtable<String, String> hashtable = new Hashtable<>();

        hashtable.put("useItem", useItem);
        hashtable.put("price", price);
        hashtable.put("paymemo", payMemo);

        return hashtable;
    }
}


