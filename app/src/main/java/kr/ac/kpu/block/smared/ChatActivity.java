package kr.ac.kpu.block.smared;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
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

public class ChatActivity extends AppCompatActivity {
    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    EditText etText;
    Button btnSend;
    Button btnViewFriend;
    String email;
    String photo;
    String nickname;
    FirebaseDatabase database;
    List<Chat> mChat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        etText = (EditText) findViewById(R.id.etText);
        btnSend = (Button) findViewById(R.id.btnSend);
        btnViewFriend = (Button) findViewById(R.id.btnViewFriend);
        mRecyclerView = (RecyclerView) findViewById(R.id.rvChat);

        // Firebase Database 연결
        database = FirebaseDatabase.getInstance();

        // 유저 정보 추출
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        // 인텐트를 통해 이전 액티비티에서 데이터 전달받기
        Intent in = getIntent();
        final String stChatId = in.getStringExtra("chatUid");
        photo = in.getStringExtra("photo");
        nickname = in.getStringExtra("nickname");
        if (user != null) {
            email = user.getEmail();
        }

        // 친구 목록 보기 버튼 이벤트 - FriendActivity로 이동한다.
        btnViewFriend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent in = new Intent(ChatActivity.this,FriendActivity.class);
                in.putExtra("chatUid",stChatId);
                startActivity(in);
            }
        });

        // 메시지 보내기 버튼 이벤트 - DB에 사용자 정보, 사진, 메시지를 저장한다.
        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String stText = etText.getText().toString();

                if (stText.equals("") || stText.isEmpty()) { // 공백 체크
                    Toast.makeText(ChatActivity.this, "내용을 입력해 주세요.", Toast.LENGTH_SHORT).show();
                } else {
                    Calendar c = Calendar.getInstance(); // Firebase내에 날짜로 저장
                    SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    String formattedDate = df.format(c.getTime());

                    DatabaseReference myRef = database.getReference("chats").child(stChatId).child("chat").child(formattedDate);

                    // HashTable로 연결
                    Hashtable<String, String> chat  = new Hashtable<String, String>();
                    chat.put("email", email);
                    chat.put("text", stText);
                    chat.put("photo", photo);
                    chat.put("nickname", nickname);

                    myRef.setValue(chat);
                    etText.setText("");
                }
            }
        });

        // 닫기 버튼 이벤트
        Button btnFinish = (Button) findViewById(R.id.btnFinish);
        btnFinish.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        // RecyclerView를 통해 채팅 메시지와 이미지를 보여줄 것이다.
        // 메시지가 추가될 때마다 RecyclerView의 크기가 바뀌게 되고
        // 그 때문에 다시 UI가 로딩되는 것을 방지해야 한다.
        mRecyclerView.setHasFixedSize(true);

        // 레이아웃을 Linear로 설정한다.
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);

        // 어댑터를 사용해 데이터를 뷰로 만든다.
        mChat= new ArrayList<>();
        mAdapter = new MyAdapter(mChat, email,ChatActivity.this);

        // 어댑터를 적용한다.
        mRecyclerView.setAdapter(mAdapter);

        // 새 채팅 메시지가 있다면 DB에서 받아오기 위해 이벤트를 등록한다
        DatabaseReference myRef = database.getReference("chats").child(stChatId).child("chat");
        myRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                // 새 메시지를 화면에 보여준다.
                Chat chat = dataSnapshot.getValue(Chat.class);
                mChat.add(chat);

                // 스크롤을 내린다.
                mRecyclerView.scrollToPosition(mChat.size() - 1);

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
