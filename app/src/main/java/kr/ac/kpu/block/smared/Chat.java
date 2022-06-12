package kr.ac.kpu.block.smared;

public class Chat extends DTO{
    private String userUid;

    private String message;
    private long timestamp;

    public String getUserUid() {
        return userUid;
    }
    public void setUserUid(String userUid) {
        this.userUid = userUid;
    }

    public String getMessage() {
        return message;
    }
    public void setMessage(String message) {
        this.message = message;
    }

    public long getTimestamp() {
        return timestamp;
    }
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public Chat() {
        // Default constructor required for calls to DataSnapshot.getValue(Comment.class)
    }
}