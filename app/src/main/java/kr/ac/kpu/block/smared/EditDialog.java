package kr.ac.kpu.block.smared;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Hashtable;
import java.util.List;

public class EditDialog extends Dialog {

    List<Ledger> mLedger;
    int position;
    String selectChatuid="";
    FirebaseDatabase database;
    DatabaseReference myRef;
    DatabaseReference chatRef;
    FirebaseUser user;

    RadioButton rbIncome;
    RadioButton rbConsume;
    TextView date;
    Spinner useitem;
    EditText price;
    EditText payMemo;
    Button submit;
    Button dismiss;

    String stClassfy = "";
    String stUseitem = "";
    String stPrice = "";
    String stPaymemo = "";

    public EditDialog(Context context, List<Ledger> mLedger, int position, String selectChatuid) {
        super(context);
        this.mLedger = mLedger;
        this.position = position;
        this.selectChatuid = selectChatuid;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE); //타이틀 바 삭제
        setContentView(R.layout.dialog_edit);

        rbIncome = findViewById(R.id.rbIncome);
        rbConsume = findViewById(R.id.rbConsume);
        date = findViewById(R.id.date);
        useitem = findViewById(R.id.useitem);
        price = findViewById(R.id.price);
        payMemo = findViewById(R.id.payMemo);
        submit = findViewById(R.id.submit);
        dismiss = findViewById(R.id.dismiss);

        database = FirebaseDatabase.getInstance();
        myRef = database.getReference("users");
        chatRef = database.getReference("chats");
        user = FirebaseAuth.getInstance().getCurrentUser();

        if (mLedger.get(position).getClassfy().equals("지출")) {
            rbConsume.setChecked(true);
        } else {
            rbIncome.setChecked(true);
        }

        setSpinner();
        date.setText(mLedger.get(position).getYear() + "-" + mLedger.get(position).getMonth() + "-" + mLedger.get(position).getDay());
        price.setText(mLedger.get(position).getPrice());
        payMemo.setText(mLedger.get(position).getPaymemo());

        //가계부 수정 버튼 이벤트 - 사용자로부터 데이터를 입력받아 가계부 DB를 수정한다.
        submit.setOnClickListener(view -> {
            Hashtable<String, String> ledger = new Hashtable<>();
            ledger.put("useItem", useitem.getSelectedItem().toString());
            ledger.put("price", price.getText().toString());
            ledger.put("paymemo",payMemo.getText().toString());

            if (rbConsume.isChecked()) {
                stClassfy = "[ 지출 ]";
            }
            else {
                stClassfy = "[ 수입 ]";
            }

            stUseitem = useitem.getSelectedItem().toString();
            stPrice = price.getText().toString();
            stPaymemo = payMemo.getText().toString();

            if (selectChatuid.isEmpty()) {
                if (rbConsume.isChecked()) {
                    myRef.child(user.getUid()).child("Ledger").child(mLedger.get(position).getYear())
                            .child(mLedger.get(position).getMonth())
                            .child(mLedger.get(position).getDay())
                            .child("수입")
                            .child(mLedger.get(position).getTimes())
                            .removeValue();
                    myRef.child(user.getUid()).child("Ledger").child(mLedger.get(position).getYear())
                            .child(mLedger.get(position).getMonth())
                            .child(mLedger.get(position).getDay())
                            .child("지출")
                            .child(mLedger.get(position).getTimes())
                            .setValue(ledger);
                } else {
                    myRef.child(user.getUid()).child("Ledger").child(mLedger.get(position).getYear())
                            .child(mLedger.get(position).getMonth())
                            .child(mLedger.get(position).getDay())
                            .child("지출")
                            .child(mLedger.get(position).getTimes())
                            .removeValue();
                    myRef.child(user.getUid()).child("Ledger").child(mLedger.get(position).getYear())
                            .child(mLedger.get(position).getMonth())
                            .child(mLedger.get(position).getDay())
                            .child("수입")
                            .child(mLedger.get(position).getTimes())
                            .setValue(ledger);
                }
            } else {
                if (rbConsume.isChecked()) {
                    chatRef.child(selectChatuid).child("Ledger").child(mLedger.get(position).getYear())
                            .child(mLedger.get(position).getMonth())
                            .child(mLedger.get(position).getDay())
                            .child("수입")
                            .child(mLedger.get(position).getTimes())
                            .removeValue();
                    chatRef.child(selectChatuid).child("Ledger").child(mLedger.get(position).getYear())
                            .child(mLedger.get(position).getMonth())
                            .child(mLedger.get(position).getDay())
                            .child("지출")
                            .child(mLedger.get(position).getTimes())
                            .setValue(ledger);
                } else {
                    chatRef.child(selectChatuid).child("Ledger").child(mLedger.get(position).getYear())
                            .child(mLedger.get(position).getMonth())
                            .child(mLedger.get(position).getDay())
                            .child("지출")
                            .child(mLedger.get(position).getTimes())
                            .removeValue();
                    chatRef.child(selectChatuid).child("Ledger").child(mLedger.get(position).getYear())
                            .child(mLedger.get(position).getMonth())
                            .child(mLedger.get(position).getDay())
                            .child("수입")
                            .child(mLedger.get(position).getTimes())
                            .setValue(ledger);
                }
            }

            Toast.makeText(getContext(), "가계부가 수정되었습니다", Toast.LENGTH_SHORT).show();
            dismiss();
        });

        dismiss.setOnClickListener(view -> dismiss());
    }

    public void setSpinner() {
        if(mLedger.get(position).getUseItem().equals("의류비")) {
            useitem.setSelection(0);
        } else if(mLedger.get(position).getUseItem().equals("식비")) {
            useitem.setSelection(1);
        } else if (mLedger.get(position).getUseItem().equals("주거비")) {
            useitem.setSelection(2);
        } else if (mLedger.get(position).getUseItem().equals("교통비")) {
            useitem.setSelection(3);
        } else if (mLedger.get(position).getUseItem().equals("생필품")) {
            useitem.setSelection(4);
        } else if (mLedger.get(position).getUseItem().equals("기타")) {
            useitem.setSelection(5);
        }
    }
}