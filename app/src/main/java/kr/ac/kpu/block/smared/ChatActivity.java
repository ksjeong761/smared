package kr.ac.kpu.block.smared;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import kr.ac.kpu.block.smared.databinding.ActivityChatBinding;

public class ChatActivity extends AppCompatActivity {
    private FormattedLogger logger = new FormattedLogger();
    private ActivityChatBinding viewBinding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewBinding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(viewBinding.getRoot());

        // Firebase DB 객체 얻어오기
        FirebaseDatabase database = FirebaseDatabase.getInstance();

        // 유저 정보 추출
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            return;
        }

        // 인텐트를 통해 이전 액티비티에서 데이터 전달받기
        Intent previousIntent = getIntent();
        final String chatUid = previousIntent.getStringExtra("chatUid");
        String photoUri = previousIntent.getStringExtra("photo");
        String nickname = previousIntent.getStringExtra("nickname");
        String email = user.getEmail();

        // 친구 목록 보기 버튼 이벤트 - FriendActivity로 이동한다.
        viewBinding.btnViewFriend.setOnClickListener(view -> {
            Intent nextIntent = new Intent(ChatActivity.this, FriendActivity.class);
            nextIntent.putExtra("chatUid", chatUid);
            startActivity(nextIntent);
        });

        // 메시지 보내기 버튼 이벤트 - 채팅 데이터베이스에 입력받은 채팅 메시지와 사용자 정보를 저장한다.
        viewBinding.btnSend.setOnClickListener(view -> {
            String chatMessage = viewBinding.etText.getText().toString();
            if (chatMessage.isEmpty()) {
                Toast.makeText(ChatActivity.this, "내용을 입력해 주세요.", Toast.LENGTH_SHORT).show();
                return;
            }

            // 현재 시간을 기준으로 채팅 데이터베이스에 기록한다.
            String now = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance().getTime());
            DatabaseReference myRef = database.getReference("chats").child(chatUid).child("chat").child(now);

            // 채팅 데이터베이스에 입력받은 채팅 메시지와 사용자 정보를 저장한다.
            UserInfo userInfo = new UserInfo(email, photoUri, "", nickname);
            Map<String, String> chat  = new Chat(chatMessage, userInfo).toHashMap();
            myRef.setValue(chat);

            // 사용자가 입력한 채팅 메시지를 비워준다.
            viewBinding.etText.setText("");
        });

        // 닫기 버튼 이벤트 - 액티비티를 종료한다.
        viewBinding.btnFinish.setOnClickListener(view -> finish());

        // RecyclerView를 통해 채팅 메시지와 이미지를 보여줄 것이다.
        // LinearLayout을 적용한다.
        viewBinding.rvChat.setLayoutManager(new LinearLayoutManager(this));

        // 메시지가 추가될 때마다 RecyclerView의 크기가 바뀌게 되고 그 때문에 다시 UI가 로딩되는 것을 방지하기 위한 코드이다.
        viewBinding.rvChat.setHasFixedSize(true);

        // 어댑터를 사용해 데이터를 뷰로 만든다.
        List<Chat> chatMessages = new ArrayList<>();
        RecyclerView.Adapter mAdapter = new ChatAdapter(chatMessages, email,ChatActivity.this);
        viewBinding.rvChat.setAdapter(mAdapter);

        // 이벤트를 이용해 새 채팅 메시지가 있다면 DB에서 받아온다.
        DatabaseReference myRef = database.getReference("chats").child(chatUid).child("chat");
        myRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                // 새 메시지를 화면에 보여준다.
                Chat chat = dataSnapshot.getValue(Chat.class);
                chatMessages.add(chat);

                // 스크롤을 내린다.
                viewBinding.rvChat.scrollToPosition(chatMessages.size() - 1);

                // 어댑터에 데이터가 변경되었음을 알린다.
                mAdapter.notifyItemInserted(chatMessages.size() - 1);
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {}

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {}

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {}

            @Override
            public void onCancelled(DatabaseError databaseError) {}
        });
    }
}
