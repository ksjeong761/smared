package kr.ac.kpu.block.smared;

public class UserInfoSingleton {
    private static final UserInfoSingleton instance = new UserInfoSingleton();
    private UserInfo currentUserInfo = new UserInfo();

    public UserInfo getCurrentUserInfo() {
        return currentUserInfo;
    }
    public void setCurrentUserInfo(UserInfo currentUserInfo) {
        this.currentUserInfo = currentUserInfo;
    }

    private UserInfoSingleton() {}

    public static UserInfoSingleton getInstance() {
        return instance;
    }
}
