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

    private FragmentLedgerRegShareBinding viewBinding;

    DatabaseReference myRef;
    DatabaseReference chatRef;
    FirebaseUser user;

    String stUseItem;
    String stEmail;
    String stUid;

    String selectedChatRoomName = "";
    String selectedChatUid = "";

    Calendar c = Calendar.getInstance();
    String stYear = new SimpleDateFormat("yyyy").format(c.getTime());
    String stMonth = new SimpleDateFormat("MM").format(c.getTime());
    String stDay = new SimpleDateFormat("dd").format(c.getTime());

    int saveItem;
    List<String> listItems = new ArrayList<>();

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        viewBinding = FragmentLedgerRegShareBinding.inflate(inflater, container, false);

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        myRef = database.getReference("users");
        chatRef = database.getReference("chats");
        user = FirebaseAuth.getInstance().getCurrentUser();

        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("email", Context.MODE_PRIVATE);
        stEmail = sharedPreferences.getString("email","");
        stUid = sharedPreferences.getString("uid","");

        viewLedgerName();

        // 분류 스피너 선택 이벤트
        viewBinding.spnUseitem2.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                stUseItem = (String) adapterView.getItemAtPosition(i);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) { }
        });

        // 달력 선택, 날짜 입력
        viewBinding.cvCalender2.setOnDateChangeListener((calendarView, year, month, day) -> {
            stYear = Integer.toString(year);
            stMonth = String.format("%02d", month+1);
            stDay =  String.format("%02d", day);

           Toast.makeText(getActivity(), stYear + "-" + stMonth + "-" + stDay, Toast.LENGTH_SHORT).show();
        });

        // 저장 버튼 이벤트
        viewBinding.btnSave2.setOnClickListener(view -> {
            if (selectedChatRoomName.isEmpty()) {
                Toast.makeText(getActivity(), "가계부를 선택후 이용해주세요", Toast.LENGTH_SHORT).show();
                return;
            }

            String stPrice = viewBinding.etPrice2.getText().toString();
            String stPaymemo = viewBinding.etPayMemo2.getText().toString();

            String stTime = new SimpleDateFormat("HHmmss").format(Calendar.getInstance().getTime());

            Hashtable<String, String> ledger = new Hashtable<>();
            ledger.put("useItem", stUseItem);
            ledger.put("price", stPrice);
            ledger.put("paymemo", stPaymemo);

            if (stPrice.isEmpty()) {
                Toast.makeText(getActivity(), "금액란을 채워주세요", Toast.LENGTH_SHORT).show();
                return;
            }

            if (viewBinding.rbConsume2.isChecked()) {
                chatRef.child(selectedChatUid).child("Ledger").child(stYear).child(stMonth).child(stDay).child("지출").child(stTime).setValue(ledger);
                Toast.makeText(getActivity(), "저장하였습니다.", Toast.LENGTH_SHORT).show();
            } else {
                chatRef.child(selectedChatUid).child("Ledger").child(stYear).child(stMonth).child(stDay).child("수입").child(stTime).setValue(ledger);
                Toast.makeText(getActivity(), "저장하였습니다.", Toast.LENGTH_SHORT).show();
            }

            viewBinding.etPayMemo2.setText("");
            viewBinding.etPrice2.setText("");
        });

        // 가계부 선택 버튼 이벤트
        viewBinding.btnChoiceLed.setOnClickListener(view -> {
            viewLedgerName();
            final CharSequence[] select = listItems.toArray(new CharSequence[listItems.size()]);

            AlertDialog.Builder alertdialog = new AlertDialog.Builder(getActivity());
            alertdialog.setTitle("가계부를 골라주세요");
            alertdialog.setSingleChoiceItems(select, -1, (dialog, item) -> saveItem = item);

            alertdialog.setNeutralButton("가계부 생성", (dialogInterface, i) -> {
                final EditText editText = new EditText(getActivity());
                AlertDialog.Builder alertdialog1 = new AlertDialog.Builder(getActivity());
                alertdialog1.setTitle("가계부 이름을 설정해주세요");
                alertdialog1.setView(editText);

                alertdialog1.setPositiveButton("확인", (dialogInterface1, i1) -> {
                    DatabaseReference pushedRef = chatRef.push();
                    String chatId = pushedRef.getKey();

                    Hashtable<String, String> makeChat = new Hashtable<>();
                    selectedChatRoomName = editText.getText().toString();
                    makeChat.put("chatname", selectedChatRoomName);

                    chatRef.child(chatId).setValue(makeChat);
                    chatRef.child(chatId).child("user").child(stUid).setValue(stEmail);

                    // 가계부 생성시 초기화 후 다시 가계부 뷰 리스트 채움
                    listItems.clear();
                    viewLedgerName();
                    setChatUid();

                    Toast.makeText(getActivity(), "가계부가 생성되었습니다.", Toast.LENGTH_SHORT).show();
                });

                alertdialog1.setNegativeButton("취소", (dialogInterface2, i2) -> { });
                AlertDialog alert = alertdialog1.create();
                alert.show();
            });

            alertdialog.setPositiveButton("선택", (dialogInterface, i) -> {
                selectedChatRoomName = select[saveItem].toString();
                setChatUid();
            });

            alertdialog.setNegativeButton("취소", (dialogInterface, i) -> { });
            AlertDialog alert = alertdialog.create();
            alert.show();

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
                    int check =0;
                    for (DataSnapshot emailSnapshot : dataSnapshot.getChildren()) {
                        if (editText.getText().toString().equals(emailSnapshot.child("email").getValue(String.class))) {
                            inviteUser(emailSnapshot.child("key").getValue(String.class), emailSnapshot.child("email").getValue(String.class));  // CHATS에 UID 키 저장, 이메일 값 저장
                            Toast.makeText(getActivity(), emailSnapshot.child("nickname").getValue(String.class) + "님을 " + selectedChatRoomName + " 가계부에 초대하였습니다.", Toast.LENGTH_SHORT).show();
                            check = 1;
                        }
                    }

                    if (check == 0) {
                        Toast.makeText(getActivity(), "사용자를 찾지 못하였습니다.", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) { }
            }));

            alertdialog.setNegativeButton("취소", (dialogInterface, i) -> { });
            AlertDialog alert = alertdialog.create();
            alert.show();
        });

        viewBinding.btnOpenChat.setOnClickListener(view -> {
            if (selectedChatRoomName.isEmpty()) {
                Toast.makeText(getActivity(), "가계부를 선택후 이용해주세요", Toast.LENGTH_SHORT).show();
                return;
            }

            myRef.child(user.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    String photo = dataSnapshot.child("photo").getValue(String.class);
                    String nickname = dataSnapshot.child("nickname").getValue(String.class);

                    Intent nextIntent = new Intent(getActivity(), ChatActivity.class);
                    nextIntent.putExtra("chatUid", selectedChatUid);
                    nextIntent.putExtra("chatName", selectedChatRoomName);
                    nextIntent.putExtra("photo",photo);
                    nextIntent.putExtra("nickname",nickname);
                    startActivity(nextIntent);
                }

                @Override
                public void onCancelled(DatabaseError databaseError) { }
            });
        });

        return viewBinding.getRoot();
    }

    public void viewLedgerName() {
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

    public void inviteUser(String uid, String email) {
        Map<String, Object> invite = new HashMap<>();
        invite.put(uid, email);
        chatRef.child(selectedChatUid).child("user").updateChildren(invite);
    }

    public void setChatUid() {
        chatRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot chatSnapshot : dataSnapshot.getChildren()) {
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
