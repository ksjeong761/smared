package kr.ac.kpu.block.smared;

import java.util.ArrayList;
import java.util.List;

public class UserInfo extends DTO {
    private String email = "";
    private String nickname = "";
    private String photoUri = "";

    private List<String> friendsUid = new ArrayList<>();
    private List<String> ledgersUid = new ArrayList<>();
    private List<String> chatRoomsUid = new ArrayList<>();

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

    public List<String> getChatRoomsUid() {
        return chatRoomsUid;
    }
    public void setChatRoomsUid(List<String> chatRoomsUid) {
        this.chatRoomsUid = chatRoomsUid;
    }

    public List<String> getFriendsUid() {
        return friendsUid;
    }
    public void setFriendsUid(List<String> friendsUid) {
        this.friendsUid = friendsUid;
    }

    public List<String> getLedgersUid() {
        return ledgersUid;
    }
    public void setLedgersUid(List<String> ledgersUid) {
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
