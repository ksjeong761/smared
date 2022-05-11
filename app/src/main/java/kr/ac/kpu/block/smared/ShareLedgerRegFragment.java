package kr.ac.kpu.block.smared;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import kr.ac.kpu.block.smared.databinding.FragmentLedgerRegShareBinding;

public class ShareLedgerRegFragment extends Fragment {
    private FormattedLogger logger = new FormattedLogger();
    private FragmentLedgerRegShareBinding viewBinding;

    private DatabaseReference myRef;
    private DatabaseReference chatRef;
    private FirebaseUser user;

    // 현재 참여중인 채팅방 목록
    private List<String> joiningChatRooms = new ArrayList<>();

    private String selectedChatRoomName = "";
    private String selectedChatRoomUid = "";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        viewBinding = FragmentLedgerRegShareBinding.inflate(inflater, container, false);

        myRef = FirebaseDatabase.getInstance().getReference("users");
        chatRef = FirebaseDatabase.getInstance().getReference("chats");
        user = FirebaseAuth.getInstance().getCurrentUser();

        loadJoiningChatRooms();

        // 저장 버튼 이벤트 - UI에 정보가 모두 입력되었다면 DB에 저장한다.
        viewBinding.btnSave2.setOnClickListener(view -> {
            if (selectedChatRoomName.isEmpty()) {
                Toast.makeText(getActivity(), "가계부를 선택후 이용해주세요", Toast.LENGTH_SHORT).show();
                return;
            }

            if (selectedChatRoomUid.isEmpty()) {
                Toast.makeText(getActivity(), "가계부를 선택후 이용해주세요", Toast.LENGTH_SHORT).show();
                logger.writeLog("ChatRoomUid is empty.");
                return;
            }

            if (viewBinding.etPrice2.getText().toString().isEmpty()) {
                Toast.makeText(getActivity(), "금액란을 채워주세요", Toast.LENGTH_SHORT).show();
                return;
            }

            // DB 삽입용 데이터
            String spendCategory = viewBinding.spnUseitem2.getSelectedItem().toString();
            String stPrice = viewBinding.etPrice2.getText().toString();
            String stPayMemo = viewBinding.etPayMemo2.getText().toString();
            Map<String, String> ledger = new LedgerContent(spendCategory, stPrice, stPayMemo).toHashMap();

            // 삽입할 DB 경로 지정
            long selectedDate = viewBinding.cvCalender2.getDate();
            String stYear = new SimpleDateFormat("yyyy").format(selectedDate);
            String stMonth = new SimpleDateFormat("MM").format(selectedDate);
            String stDay = new SimpleDateFormat("dd").format(selectedDate);
            String tableName = (viewBinding.rbConsume2.isChecked()) ? "지출" : "수입";
            String stTime = new SimpleDateFormat("HHmmss").format(Calendar.getInstance().getTime());

            // DB 삽입
            chatRef.child(selectedChatRoomUid)
                    .child("Ledger")
                    .child(stYear)
                    .child(stMonth)
                    .child(stDay)
                    .child(tableName)
                    .child(stTime)
                    .setValue(ledger);

            // 입력 받는 부분 UI 초기화
            viewBinding.etPayMemo2.setText("");
            viewBinding.etPrice2.setText("");
            Toast.makeText(getActivity(), "저장하였습니다.", Toast.LENGTH_SHORT).show();
        });

        // 가계부 선택 버튼 이벤트 - 가계부를 선택할 수 있는 다이얼로그를 출력한다.
        viewBinding.btnChoiceLed.setOnClickListener(view -> {
            AlertDialog.Builder selectLedgerDialog = new AlertDialog.Builder(getActivity());
            selectLedgerDialog.setTitle("가계부를 선택해주세요");

            final CharSequence[] chatRooms = joiningChatRooms.toArray(new CharSequence[joiningChatRooms.size()]);
            selectLedgerDialog.setSingleChoiceItems(chatRooms, -1, (selectDialog, whichItem) -> {
                selectedChatRoomName = chatRooms[whichItem].toString();
                findUidByChatRoomName(selectedChatRoomName);
            });

            selectLedgerDialog.setPositiveButton("선택", (selectDialog, whichItem) -> { });
            selectLedgerDialog.setNegativeButton("취소", (selectDialog, whichItem) -> { });

            selectLedgerDialog.setNeutralButton("가계부 생성", (selectDialog, whichItem) -> {
                final EditText editText = new EditText(getActivity());
                AlertDialog.Builder createLedgerDialog = new AlertDialog.Builder(getActivity());
                createLedgerDialog.setTitle("가계부 이름을 설정해주세요");
                createLedgerDialog.setView(editText);

                createLedgerDialog.setPositiveButton("확인", (createDialog, item) -> {
                    selectedChatRoomUid = chatRef.push().getKey();

                    // 채팅방 추가
                    Map<String, String> makeChat = new HashMap<>();
                    makeChat.put("chatname", editText.getText().toString());
                    chatRef.child(selectedChatRoomUid).setValue(makeChat);

                    // 채팅방 참가자 목록 갱신
                    SharedPreferences sharedPreferences = getActivity().getSharedPreferences("email", Context.MODE_PRIVATE);
                    String stEmail = sharedPreferences.getString("email", "");
                    chatRef.child(selectedChatRoomUid).child("user").child(user.getUid()).setValue(stEmail);

                    // 채팅방이 새로 추가되었으므로 목록을 다시 불러온다.
                    loadJoiningChatRooms();

                    Toast.makeText(getActivity(), "가계부가 생성되었습니다.", Toast.LENGTH_SHORT).show();
                });

                createLedgerDialog.setNegativeButton("취소", (createDialog, item) -> { });
                createLedgerDialog.create().show();
            });

            selectLedgerDialog.create().show();
            joiningChatRooms.clear();
        });

        // 초대 버튼 이벤트 - 초대할 사람의 이메일을 입력받는 다이얼로그를 출력한다.
        viewBinding.btnInvite.setOnClickListener(view -> {
            if (selectedChatRoomName.isEmpty()) {
                Toast.makeText(getActivity(), "가계부를 선택후 이용해주세요", Toast.LENGTH_SHORT).show();
                return;
            }

            if (selectedChatRoomUid.isEmpty()) {
                Toast.makeText(getActivity(), "가계부를 선택후 이용해주세요", Toast.LENGTH_SHORT).show();
                logger.writeLog("ChatRoomUid is empty.");
                return;
            }

            AlertDialog.Builder inviteUserDialog = new AlertDialog.Builder(getActivity());
            final EditText editText = new EditText(getActivity());
            inviteUserDialog.setTitle("초대할 이메일을 입력해주세요");
            inviteUserDialog.setView(editText);
            inviteUserDialog.setPositiveButton("초대", (inviteDialog, whichItem) -> {
                myRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        String targetUserEmail = editText.getText().toString();
                        String targetUserUid = "";
                        String targetUserNickname = "";

                        // 초대할 사람의 이메일이 존재하는지 확인한다.
                        boolean doesExistUser = false;
                        for (DataSnapshot emailSnapshot : dataSnapshot.getChildren()) {
                            if (targetUserEmail.equals(emailSnapshot.child("email").getValue(String.class))) {
                                targetUserUid = emailSnapshot.child("key").getValue(String.class);
                                targetUserNickname = emailSnapshot.child("nickname").getValue(String.class);
                                doesExistUser = true;
                                break;
                            }
                        }

                        if (!doesExistUser) {
                            Toast.makeText(getActivity(), "사용자를 찾지 못하였습니다.", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        // 초대할 사람의 이메일이 존재한다면 채팅방에 초대한다.
                        Map<String, Object> invite = new HashMap<>();
                        invite.put(targetUserUid, targetUserEmail);
                        chatRef.child(selectedChatRoomUid).child("user").updateChildren(invite);

                        Toast.makeText(getActivity(), targetUserNickname + "님을 " + selectedChatRoomName + " 가계부에 초대하였습니다.", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) { }
                });
            });

            inviteUserDialog.setNegativeButton("취소", (dialogInterface, i) -> { });
            inviteUserDialog.create().show();
        });

        // 채팅방 열기
        viewBinding.btnOpenChat.setOnClickListener(view -> {
            if (selectedChatRoomName.isEmpty()) {
                Toast.makeText(getActivity(), "가계부를 선택후 이용해주세요", Toast.LENGTH_SHORT).show();
                return;
            }

            myRef.child(user.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    Intent nextIntent = new Intent(getActivity(), ChatActivity.class);
                    nextIntent.putExtra("chatUid", selectedChatRoomUid);
                    nextIntent.putExtra("chatName", selectedChatRoomName);
                    nextIntent.putExtra("photo", dataSnapshot.child("photo").getValue(String.class));
                    nextIntent.putExtra("nickname", dataSnapshot.child("nickname").getValue(String.class));
                    startActivity(nextIntent);
                }

                @Override
                public void onCancelled(DatabaseError databaseError) { }
            });
        });

        return viewBinding.getRoot();
    }

    // 사용자가 현재 참여중인 채팅방(가계부) 이름 읽어오기
    private void loadJoiningChatRooms() {
        chatRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot chatSnapshot : dataSnapshot.getChildren()) {
                    for (DataSnapshot userSnapshot : chatSnapshot.getChildren()) {
                        for (DataSnapshot uidSnapshot : userSnapshot.getChildren()) {
                            if (uidSnapshot.getKey().equals(user.getUid())) {
                                joiningChatRooms.add(chatSnapshot.child("chatname").getValue(String.class));
                            }
                        }
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) { }
        });
    }

    // DB에서 채팅방 이름에 해당하는 uid를 읽어온다.
    private void findUidByChatRoomName(String chatRoomName) {
        selectedChatRoomUid = "";

        chatRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // 채팅방 이름으로 uid를 조회한다.
                for (DataSnapshot chatSnapshot : dataSnapshot.getChildren()) {
                    if (chatSnapshot.child("chatname").getValue(String.class).equals(chatRoomName)) {
                        selectedChatRoomUid = chatSnapshot.getKey();
                        break;
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) { }
        });
    }
}