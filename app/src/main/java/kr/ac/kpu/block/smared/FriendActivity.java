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

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewBinding = ActivityFriendBinding.inflate(getLayoutInflater());
        setContentView(viewBinding.getRoot());

        String stChatId = this.getIntent().getStringExtra("chatUid");
        List<Friend> mFriend= new ArrayList<>();
        FriendAdapter mAdapter = new FriendAdapter(mFriend,this);

        viewBinding.rvFriend.setHasFixedSize(true);
        viewBinding.rvFriend.setLayoutManager(new LinearLayoutManager(this));
        viewBinding.rvFriend.setAdapter(mAdapter);

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference("users");
        DatabaseReference chatRef = database.getReference("chats").child(stChatId).child("user");
        chatRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot dataSnapshot2 : dataSnapshot.getChildren()) {
                    final String value = dataSnapshot2.getKey();
                    myRef.child(value).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            mFriend.add(dataSnapshot.getValue(Friend.class));
                            mAdapter.notifyItemInserted(mFriend.size() - 1);
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
