package kr.ac.kpu.block.smared;

public class Friend {

    private String email;
    private String photo;
    private String key;
    private String nickname;

    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhoto() {
        return photo;
    }
    public void setPhoto(String photo) { this.photo = photo; }

    public String getKey() {
        return key;
    }
    public void setKey(String key) {
        this.key = key;
    }

    public String getNickname() {
        return nickname;
    }
    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public Friend() {
        // Default constructor required for calls to DataSnapshot.getValue(Comment.class)
    }
}
