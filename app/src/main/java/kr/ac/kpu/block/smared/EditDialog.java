package kr.ac.kpu.block.smared;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.Window;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Hashtable;
import java.util.List;

import kr.ac.kpu.block.smared.databinding.DialogEditBinding;

public class EditDialog extends Dialog {
    private FormattedLogger logger = new FormattedLogger();
    private DialogEditBinding viewBinding;

    private List<Ledger> mLedger;
    private int position;
    private String selectChatuid="";

    private FirebaseUser user;

    public EditDialog(Context context, List<Ledger> mLedger, int position, String selectChatuid) {
        super(context);
        this.mLedger = mLedger;
        this.position = position;
        this.selectChatuid = selectChatuid;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewBinding = DialogEditBinding.inflate(getLayoutInflater());
        setContentView(viewBinding.getRoot());

        requestWindowFeature(Window.FEATURE_NO_TITLE); //타이틀 바 삭제

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference("users");
        user = FirebaseAuth.getInstance().getCurrentUser();

        if (mLedger.get(position).getClassfy().equals("지출")) {
            viewBinding.rbConsume.setChecked(true);
        } else {
            viewBinding.rbIncome.setChecked(true);
        }

        setSpinner();
        viewBinding.date.setText(mLedger.get(position).getYear() + "-" + mLedger.get(position).getMonth() + "-" + mLedger.get(position).getDay());
        viewBinding.price.setText(mLedger.get(position).getPrice());
        viewBinding.payMemo.setText(mLedger.get(position).getPaymemo());

        //가계부 수정 버튼 이벤트 - 사용자로부터 데이터를 입력받아 가계부 DB를 수정한다.
        viewBinding.submit.setOnClickListener(view -> {
            Hashtable<String, String> ledger = new Hashtable<>();
            ledger.put("useItem", viewBinding.useitem.getSelectedItem().toString());
            ledger.put("price", viewBinding.price.getText().toString());
            ledger.put("paymemo", viewBinding.payMemo.getText().toString());

            if (selectChatuid.isEmpty()) {
                selectChatuid = user.getUid();
            }

            String removeTargetTable = (viewBinding.rbConsume.isChecked()) ? "수입" : "지출";
            String addTargetTable = (viewBinding.rbConsume.isChecked()) ? "지출" : "수입";

            myRef.child(user.getUid()).child("Ledger").child(mLedger.get(position).getYear())
                    .child(mLedger.get(position).getMonth())
                    .child(mLedger.get(position).getDay())
                    .child(removeTargetTable)
                    .child(mLedger.get(position).getTimes())
                    .removeValue();

            myRef.child(user.getUid()).child("Ledger").child(mLedger.get(position).getYear())
                    .child(mLedger.get(position).getMonth())
                    .child(mLedger.get(position).getDay())
                    .child(addTargetTable)
                    .child(mLedger.get(position).getTimes())
                    .setValue(ledger);

            Toast.makeText(getContext(), "가계부가 수정되었습니다", Toast.LENGTH_SHORT).show();
            dismiss();
        });

        viewBinding.dismiss.setOnClickListener(view -> dismiss());
    }

    public void setSpinner() {
        if (mLedger.get(position).getUseItem().equals("의류비")) {
            viewBinding.useitem.setSelection(0);
        } else if (mLedger.get(position).getUseItem().equals("식비")) {
            viewBinding.useitem.setSelection(1);
        } else if (mLedger.get(position).getUseItem().equals("주거비")) {
            viewBinding.useitem.setSelection(2);
        } else if (mLedger.get(position).getUseItem().equals("교통비")) {
            viewBinding.useitem.setSelection(3);
        } else if (mLedger.get(position).getUseItem().equals("생필품")) {
            viewBinding.useitem.setSelection(4);
        } else if (mLedger.get(position).getUseItem().equals("기타")) {
            viewBinding.useitem.setSelection(5);
        }
    }
}