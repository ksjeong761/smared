package kr.ac.kpu.block.smared;

import java.util.HashMap;

public class UserInfo {
    private String email;
    private String photoUri;
    private String uid;
    private String nickname;

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

    public String getUid() {
        return uid;
    }
    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }

    public UserInfo() {
        this.email = "";
        this.photoUri = "";
        this.uid = "";
        this.nickname = "";
    }

    public UserInfo(String email, String photoUri, String uid, String nickname) {
        this.email = email;
        this.photoUri = photoUri;
        this.uid = uid;
        this.nickname = nickname;
    }

    public HashMap<String, String> toHashMap() {
        HashMap<String, String> hashMap = new HashMap<>();

        hashMap.put("email", email);
        hashMap.put("photo", photoUri);
        hashMap.put("key", uid);
        hashMap.put("nickname", nickname);

        return hashMap;
    }
}
