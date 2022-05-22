package kr.ac.kpu.block.smared;

public class Ledger {
    private String year;
    private String month;
    private String day;
    private String classify;
    private String times;
    private LedgerContent ledgerContent;

    public String getYear() {
        return year;
    }
    public void setYear(String year) {
        this.year = year;
    }

    public String getMonth() {
        return month;
    }
    public void setMonth(String month) {
        this.month = month;
    }

    public String getDay() {
        return day;
    }
    public void setDay(String day) {
        this.day = day;
    }

    public String getClassify() {
        return classify;
    }
    public void setClassify(String classify) {
        this.classify = classify;
    }

    public String getTimes() {
        return times;
    }
    public void setTimes(String time) {
        this.times = time;
    }

    public LedgerContent getLedgerContent() { return ledgerContent; }
    public void setLedgerContent(LedgerContent ledgerContent) { this.ledgerContent = ledgerContent; }

    public Ledger() {
        // Default constructor required for calls to DataSnapshot.getValue(Comment.class)
        this.year = "";
        this.month = "";
        this.day = "";
        this.classify = "";
        this.times = "";
        this.ledgerContent = new LedgerContent();
    }
}
