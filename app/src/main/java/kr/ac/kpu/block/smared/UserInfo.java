package kr.ac.kpu.block.smared;

import java.util.HashMap;
import java.util.Map;

public class UserInfo extends DTO {
    private String email = "";
    private String nickname = "";
    private String photoUri = "";

    private Map<String, Boolean> friendsUid = new HashMap<>();
    private Map<String, Boolean> ledgersUid = new HashMap<>();
    private Map<String, Boolean> chatRoomsUid = new HashMap<>();

    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email;
    }

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }

    public String getPhotoUri() {
        return photoUri;
    }
    public void setPhotoUri(String photoUri) {
        this.photoUri = photoUri;
    }

    public  Map<String, Boolean> getChatRoomsUid() {
        return chatRoomsUid;
    }
    public void setChatRoomsUid( Map<String, Boolean> chatRoomsUid) {
        this.chatRoomsUid = chatRoomsUid;
    }

    public Map<String, Boolean> getFriendsUid() {
        return friendsUid;
    }
    public void setFriendsUid( Map<String, Boolean> friendsUid) {
        this.friendsUid = friendsUid;
    }

    public  Map<String, Boolean> getLedgersUid() {
        return ledgersUid;
    }
    public void setLedgersUid(Map<String, Boolean> ledgersUid) {
        this.ledgersUid = ledgersUid;
    }

    public UserInfo() {
    }

    public UserInfo(String email, String photoUri, String uid, String nickname) {
        this.email = email;
        this.photoUri = photoUri;
        this.nickname = nickname;
    }
}
