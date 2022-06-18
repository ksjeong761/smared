package kr.ac.kpu.block.smared;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import kr.ac.kpu.block.smared.databinding.ActivityFriendBinding;

public class UserFriendActivity extends AppCompatActivity {
    private FormattedLogger logger = new FormattedLogger();
    private ActivityFriendBinding viewBinding;

    List<UserInfo> friends = new ArrayList<>();

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewBinding = ActivityFriendBinding.inflate(getLayoutInflater());
        setContentView(viewBinding.getRoot());

        readAllFriendsInfoFromDB();

        viewBinding.rvFriend.setHasFixedSize(true);
        viewBinding.rvFriend.setLayoutManager(new LinearLayoutManager(this));
        viewBinding.rvFriend.setAdapter(new UserFriendAdapter(friends,this));
    }

    // 친구 목록의 사용자 정보를 user에서 가져온다.
    private void readAllFriendsInfoFromDB() {
        UserInfoSingleton userInfoSingleton = UserInfoSingleton.getInstance();
        Map<String, Boolean> friendsUid = userInfoSingleton.getCurrentUserInfo().getFriendsUid();

        friendsUid.forEach((key, value) -> {
            UserInfo friendInfo = new UserInfo();

            DAO dao =  new DAO();
            dao.setSuccessCallback(arg -> afterSuccess(arg));
            dao.setFailureCallback(arg -> afterFailure(arg));
            dao.readAll(friendInfo, UserInfo.class);
        });
    }

    private void afterSuccess(DataSnapshot dataSnapshot) {
        friends.add(dataSnapshot.getValue(UserInfo.class));
    }

    private void afterFailure(DatabaseError databaseError) {
        Toast.makeText(UserFriendActivity.this, "Failed to read value : " + databaseError.toException().getMessage(), Toast.LENGTH_SHORT).show();
        logger.writeLog("Failed to read value : " + databaseError.toException().getMessage());
    }
}
