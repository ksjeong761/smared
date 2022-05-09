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
import android.widget.AdapterView;
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
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import kr.ac.kpu.block.smared.databinding.FragmentLedgerRegShareBinding;

public class ShareLedgerRegFragment extends Fragment {
    private FormattedLogger logger = new FormattedLogger();
    private FragmentLedgerRegShareBinding viewBinding;

    private DatabaseReference chatRef;

    private String selectedSpendCategory;
    private String selectedChatRoomName = "";
    private String selectedChatUid = "";

    private Calendar c = Calendar.getInstance();
    private String stYear = new SimpleDateFormat("yyyy").format(c.getTime());
    private String stMonth = new SimpleDateFormat("MM").format(c.getTime());
    private String stDay = new SimpleDateFormat("dd").format(c.getTime());

    private int saveItem;
    private List<String> listItems = new ArrayList<>();

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        viewBinding = FragmentLedgerRegShareBinding.inflate(inflater, container, false);

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference("users");
        chatRef = database.getReference("chats");
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("email", Context.MODE_PRIVATE);
        String stEmail = sharedPreferences.getString("email","");
        String stUid = sharedPreferences.getString("uid","");

        viewLedgerName(stUid);

        // 드롭다운 메뉴(소비영역 분류) 선택 이벤트 - 선택된 값을 저장한다.
        viewBinding.spnUseitem2.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                selectedSpendCategory = (String) adapterView.getItemAtPosition(i);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) { }
        });

        // 날짜 선택 이벤트 - 선택된 날짜를 저장하고 사용자에게 텍스트로 보여준다.
        viewBinding.cvCalender2.setOnDateChangeListener((calendarView, year, month, day) -> {
            stYear = Integer.toString(year);
            stMonth = String.format("%02d", month+1);
            stDay =  String.format("%02d", day);

           Toast.makeText(getActivity(), stYear + "-" + stMonth + "-" + stDay, Toast.LENGTH_SHORT).show();
        });

        // 저장 버튼 이벤트 - UI에 정보가 모두 입력되었다면 DB에 저장한다.
        viewBinding.btnSave2.setOnClickListener(view -> {
            if (selectedChatRoomName.isEmpty()) {
                Toast.makeText(getActivity(), "가계부를 선택후 이용해주세요", Toast.LENGTH_SHORT).show();
                return;
            }

            if (viewBinding.etPrice2.getText().toString().isEmpty()) {
                Toast.makeText(getActivity(), "금액란을 채워주세요", Toast.LENGTH_SHORT).show();
                return;
            }

            Hashtable<String, String> ledger = new Hashtable<>();
            ledger.put("useItem", selectedSpendCategory);
            ledger.put("price", viewBinding.etPrice2.getText().toString());
            ledger.put("paymemo", viewBinding.etPayMemo2.getText().toString());

            String tableName = (viewBinding.rbConsume2.isChecked()) ? "지출" : "수입";
            String stTime = new SimpleDateFormat("HHmmss").format(Calendar.getInstance().getTime());
            chatRef.child(selectedChatUid).child("Ledger").child(stYear).child(stMonth).child(stDay).child(tableName).child(stTime).setValue(ledger);

            Toast.makeText(getActivity(), "저장하였습니다.", Toast.LENGTH_SHORT).show();
            viewBinding.etPayMemo2.setText("");
            viewBinding.etPrice2.setText("");
        });

        // 가계부 선택 버튼 이벤트 - 가계부를 선택할 수 있는 다이얼로그를 출력한다.
        viewBinding.btnChoiceLed.setOnClickListener(view -> {
            viewLedgerName(stUid);
            final CharSequence[] select = listItems.toArray(new CharSequence[listItems.size()]);

            AlertDialog.Builder selectLedgerDialog = new AlertDialog.Builder(getActivity());
            selectLedgerDialog.setTitle("가계부를 골라주세요");
            selectLedgerDialog.setSingleChoiceItems(select, -1, (dialog, item) -> saveItem = item);

            selectLedgerDialog.setNeutralButton("가계부 생성", (dialogInterface, i) -> {
                final EditText editText = new EditText(getActivity());
                AlertDialog.Builder createLedgerDialog = new AlertDialog.Builder(getActivity());
                createLedgerDialog.setTitle("가계부 이름을 설정해주세요");
                createLedgerDialog.setView(editText);

                createLedgerDialog.setPositiveButton("확인", (dialogInterface1, i1) -> {
                    DatabaseReference pushedRef = chatRef.push();
                    String chatId = pushedRef.getKey();

                    Hashtable<String, String> makeChat = new Hashtable<>();
                    selectedChatRoomName = editText.getText().toString();
                    makeChat.put("chatname", selectedChatRoomName);

                    chatRef.child(chatId).setValue(makeChat);
                    chatRef.child(chatId).child("user").child(stUid).setValue(stEmail);

                    // 가계부 생성시 초기화 후 다시 가계부 뷰 리스트 채움
                    viewLedgerName(stUid);
                    setChatUid();

                    Toast.makeText(getActivity(), "가계부가 생성되었습니다.", Toast.LENGTH_SHORT).show();
                });

                createLedgerDialog.setNegativeButton("취소", (dialogInterface2, i2) -> { });
                createLedgerDialog.create().show();
            });

            selectLedgerDialog.setPositiveButton("선택", (dialogInterface, i) -> {
                selectedChatRoomName = select[saveItem].toString();
                setChatUid();
            });

            selectLedgerDialog.setNegativeButton("취소", (dialogInterface, i) -> { });
            selectLedgerDialog.create().show();
            listItems.clear();
        });

        // 초대 버튼 이벤트
        viewBinding.btnInvite.setOnClickListener(view -> {
            if (selectedChatRoomName.isEmpty()) {
                Toast.makeText(getActivity(), "가계부를 선택후 이용해주세요", Toast.LENGTH_SHORT).show();
                return;
            }

            AlertDialog.Builder alertdialog = new AlertDialog.Builder(getActivity());
            final EditText editText = new EditText(getActivity());
            alertdialog.setTitle("초대할 이메일을 입력해주세요");
            alertdialog.setView(editText);
            alertdialog.setPositiveButton("초대", (dialogInterface, i) -> myRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    for (DataSnapshot emailSnapshot : dataSnapshot.getChildren()) {
                        if (!editText.getText().toString().equals(emailSnapshot.child("email").getValue(String.class))) {
                            continue;
                        }

                        // CHATS에 UID 키 저장, 이메일 값 저장
                        Map<String, Object> invite = new HashMap<>();
                        invite.put(emailSnapshot.child("key").getValue(String.class), emailSnapshot.child("email").getValue(String.class));
                        chatRef.child(selectedChatUid).child("user").updateChildren(invite);
                        Toast.makeText(getActivity(), emailSnapshot.child("nickname").getValue(String.class) + "님을 " + selectedChatRoomName + " 가계부에 초대하였습니다.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Toast.makeText(getActivity(), "사용자를 찾지 못하였습니다.", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onCancelled(DatabaseError databaseError) { }
            }));

            alertdialog.setNegativeButton("취소", (dialogInterface, i) -> { });
            alertdialog.create().show();
        });

        viewBinding.btnOpenChat.setOnClickListener(view -> {
            if (selectedChatRoomName.isEmpty()) {
                Toast.makeText(getActivity(), "가계부를 선택후 이용해주세요", Toast.LENGTH_SHORT).show();
                return;
            }

            myRef.child(user.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    Intent nextIntent = new Intent(getActivity(), ChatActivity.class);
                    nextIntent.putExtra("chatUid", selectedChatUid);
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

    // 현재 참여중인 채팅방(가계부) 이름 읽어오기
    // DB에서 Uid에 해당하는 채팅방 이름을 읽어온다.
    public void viewLedgerName(String stUid) {
        chatRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot chatSnapshot : dataSnapshot.getChildren()) {
                    for (DataSnapshot userSnapshot : chatSnapshot.getChildren()) {
                        for (DataSnapshot uidSnapshot : userSnapshot.getChildren()) {
                            if (uidSnapshot.getKey().equals(stUid)) {
                                listItems.add(chatSnapshot.child("chatname").getValue(String.class));
                            }
                        }
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) { }
        });
    }

    public void setChatUid() {
        chatRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot chatSnapshot : dataSnapshot.getChildren()) {
                    // 채팅방 이름으로 가계부 이름을 사용하므로 선택된 가계부명을 찾는다.
                    if (chatSnapshot.child("chatname").getValue(String.class).equals(selectedChatRoomName)) {
                        selectedChatUid = chatSnapshot.getKey();
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) { }
        });
    }
}
