package kr.ac.kpu.block.smared;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

import kr.ac.kpu.block.smared.databinding.ActivityFriendBinding;

public class FriendActivity extends AppCompatActivity {
    private FormattedLogger logger = new FormattedLogger();
    private ActivityFriendBinding viewBinding;

    DatabaseReference myRef;
    DatabaseReference chatRef;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewBinding = ActivityFriendBinding.inflate(getLayoutInflater());
        setContentView(viewBinding.getRoot());

        String chatRoomUid = this.getIntent().getStringExtra("chatUid");
        myRef = FirebaseDatabase.getInstance().getReference("users");
        chatRef = FirebaseDatabase.getInstance().getReference("chats").child(chatRoomUid).child("user");

        List<UserInfo> friends = new ArrayList<>();
        FriendAdapter friendAdapter = new FriendAdapter(friends,this);
        viewBinding.rvFriend.setHasFixedSize(true);
        viewBinding.rvFriend.setLayoutManager(new LinearLayoutManager(this));
        viewBinding.rvFriend.setAdapter(friendAdapter);

        // DB에서 친구 목록을 불러와 화면에 출력한다.
        chatRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot chatSnapshot : dataSnapshot.getChildren()) {
                    final String value = chatSnapshot.getKey();
                    myRef.child(value).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            friends.add(dataSnapshot.getValue(UserInfo.class));
                            friendAdapter.notifyItemInserted(friends.size() - 1);
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) { }
                    });
                }
            }

            @Override
            public void onCancelled(DatabaseError error) { }
        });
    }
}
