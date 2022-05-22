package kr.ac.kpu.block.smared;

import java.util.HashMap;

public class Chat {
    private String message;
    private UserInfo userInfo;

    public String getMessage() {
        return message;
    }
    public void setMessage(String message) {
        this.message = message;
    }

    public UserInfo getUserInfo() {
        return userInfo;
    }
    public void setUserInfo(UserInfo userInfo) {
        this.userInfo = userInfo;
    }

    public Chat() {
        // Default constructor required for calls to DataSnapshot.getValue(Comment.class)
        this.message = "";
        this.userInfo = new UserInfo();
    }

    public Chat(String message, UserInfo userInfo) {
        this.message = message;
        this.userInfo = userInfo;
    }

    public HashMap<String, String> toHashMap() {
        HashMap<String, String> hashMap = new HashMap<>();

        hashMap.put("text", message);
        hashMap.put("email", userInfo.getEmail());
        hashMap.put("photo", userInfo.getPhotoUri());
        hashMap.put("nickname", userInfo.getNickname());

        return hashMap;
    }
}