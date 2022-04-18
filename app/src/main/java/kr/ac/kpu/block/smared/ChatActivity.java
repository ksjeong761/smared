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
import java.util.Hashtable;
import java.util.List;

import kr.ac.kpu.block.smared.databinding.ActivityChatBinding;

public class ChatActivity extends AppCompatActivity {

    private ActivityChatBinding viewBinding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewBinding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(viewBinding.getRoot());

        // Firebase Database 연결
        FirebaseDatabase database = FirebaseDatabase.getInstance();

        // 유저 정보 추출
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            return;
        }

        // 인텐트를 통해 이전 액티비티에서 데이터 전달받기
        Intent previousIntent = getIntent();
        final String chatUid = previousIntent.getStringExtra("chatUid");
        String photo = previousIntent.getStringExtra("photo");
        String nickname = previousIntent.getStringExtra("nickname");
        String email = user.getEmail();

        // 친구 목록 보기 버튼 이벤트 - FriendActivity로 이동한다.
        viewBinding.btnViewFriend.setOnClickListener(view -> {
            Intent nextIntent = new Intent(ChatActivity.this, FriendActivity.class);
            nextIntent.putExtra("chatUid", chatUid);
            startActivity(nextIntent);
        });

        // 메시지 보내기 버튼 이벤트 - DB에 사용자 정보, 사진, 메시지를 저장한다.
        viewBinding.btnSend.setOnClickListener(view -> {
            String stText = viewBinding.etText.getText().toString();
            if (stText.isEmpty()) {
                Toast.makeText(ChatActivity.this, "내용을 입력해 주세요.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Firebase내에 날짜로 저장
            String formattedDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance().getTime());
            DatabaseReference myRef = database.getReference("chats").child(chatUid).child("chat").child(formattedDate);

            // HashTable로 연결
            Hashtable<String, String> chat  = new Hashtable<>();
            chat.put("email", email);
            chat.put("text", stText);
            chat.put("photo", photo);
            chat.put("nickname", nickname);

            myRef.setValue(chat);
            viewBinding.etText.setText("");
        });

        // 닫기 버튼 이벤트
        viewBinding.btnFinish.setOnClickListener(view -> {
            finish();
        });

        // RecyclerView를 통해 채팅 메시지와 이미지를 보여줄 것이다.
        // 메시지가 추가될 때마다 RecyclerView의 크기가 바뀌게 되고
        // 그 때문에 다시 UI가 로딩되는 것을 방지해야 한다.
        viewBinding.rvChat.setHasFixedSize(true);

        // 레이아웃을 Linear로 설정한다.
        viewBinding.rvChat.setLayoutManager(new LinearLayoutManager(this));

        // 어댑터를 사용해 데이터를 뷰로 만든다.
        List<Chat> mChat = new ArrayList<>();
        RecyclerView.Adapter mAdapter = new MyAdapter(mChat, email,ChatActivity.this);

        // 어댑터를 적용한다.
        viewBinding.rvChat.setAdapter(mAdapter);

        // 새 채팅 메시지가 있다면 DB에서 받아오기 위해 이벤트를 등록한다
        DatabaseReference myRef = database.getReference("chats").child(chatUid).child("chat");
        myRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                // 새 메시지를 화면에 보여준다.
                Chat chat = dataSnapshot.getValue(Chat.class);
                mChat.add(chat);

                // 스크롤을 내린다.
                viewBinding.rvChat.scrollToPosition(mChat.size() - 1);

                // 어댑터에 데이터가 변경되었음을 알린다.
                mAdapter.notifyItemInserted(mChat.size() - 1);
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
