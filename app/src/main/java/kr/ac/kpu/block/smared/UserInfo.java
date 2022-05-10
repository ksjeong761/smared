package kr.ac.kpu.block.smared;

import java.util.Hashtable;

public class UserInfo {
    private String email;
    private String photoUri;
    private String uid;

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
        return this.uid;
    }
    public void setUid(String uid) {
        this.uid = uid;
    }

    public UserInfo(String email, String photoUri, String uid) {
        this.email = email;
        this.photoUri = photoUri;
        this.uid = uid;
    }

    public Hashtable<String, String> toHashtable() {
        Hashtable<String, String> hashtable = new Hashtable<>();

        hashtable.put("email", email);
        hashtable.put("photo", photoUri);
        hashtable.put("key", uid);

        return hashtable;
    }
}
