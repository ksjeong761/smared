package kr.ac.kpu.block.smared;

import java.util.HashMap;
import java.util.Map;

public class ChatRoom extends DTO {
    private String chatRoomName = "";

    private Map<String, Boolean> chatsUid = new HashMap<>();
    private Map<String, Boolean> membersUid = new HashMap<>();

    public String getChatRoomName() {
        return chatRoomName;
    }
    public void setChatRoomName(String chatRoomName) {
        this.chatRoomName = chatRoomName;
    }

    public Map<String, Boolean> getMembersUid() {
        return membersUid;
    }
    public void setMembersUid( Map<String, Boolean> membersUid) {
        this.membersUid = membersUid;
    }

    public  Map<String, Boolean> getChatsUid() {
        return chatsUid;
    }
    public void setChatsUid (Map<String, Boolean> chatsUid) {
        this.chatsUid = chatsUid;
    }
}
