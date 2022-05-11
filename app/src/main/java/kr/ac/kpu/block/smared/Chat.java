package kr.ac.kpu.block.smared;

import java.util.HashMap;

public class Chat {
    private String message;
    private String email;
    private String photoUri;
    private String nickname;

    public String getMessage() {
        return message;
    }
    public void setMessage(String message) {
        this.message = message;
    }

    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhotoUri() {
        return photoUri;
    }
    public void setPhotoUri(String photoUri) {
        this.photoUri = photoUri;
    }

    public String getNickname() {
        return nickname;
    }
    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public Chat() {
        // Default constructor required for calls to DataSnapshot.getValue(Comment.class)
        this.message = "";
        this.email = "";
        this.photoUri = "";
        this.nickname = "";
    }

    public Chat(String text, String email, String photo, String nickname) {
        this.message = text;
        this.email = email;
        this.photoUri = photo;
        this.nickname = nickname;
    }

    public HashMap<String, String> toHashMap() {
        HashMap<String, String> hashMap = new HashMap<>();

        hashMap.put("text", message);
        hashMap.put("email", email);
        hashMap.put("photo", photoUri);
        hashMap.put("nickname", nickname);

        return hashMap;
    }
}