package kr.ac.kpu.block.smared;

public class LedgerContent {
    private String price;
    private String paymemo;
    private String useItem;

    public String getPrice() {
        return price;
    }
    public void setPrice(String price) {
        this.price = price;
    }

    public String getPaymemo() {
        return paymemo;
    }
    public void setPaymemo(String paymemo) {
        this.paymemo = paymemo;
    }

    public String getUseItem() {
        return useItem;
    }
    public void setUseItem(String useItem) { this.useItem = useItem; }

    public LedgerContent() {
        // Default constructor required for calls to DataSnapshot.getValue(Comment.class)
    }
}


