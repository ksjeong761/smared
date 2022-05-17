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

import java.util.List;
import java.util.Map;

import kr.ac.kpu.block.smared.databinding.DialogLedgerEditBinding;

public class LedgerEditDialog extends Dialog {
    private FormattedLogger logger = new FormattedLogger();
    private DialogLedgerEditBinding viewBinding;

    private List<Ledger> ledgerData;
    private int position;

    private FirebaseUser user;

    public LedgerEditDialog(Context context, List<Ledger> ledgerData, int position) {
        super(context);
        this.ledgerData = ledgerData;
        this.position = position;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewBinding = DialogLedgerEditBinding.inflate(getLayoutInflater());
        setContentView(viewBinding.getRoot());

        requestWindowFeature(Window.FEATURE_NO_TITLE); //타이틀 바 삭제

        DatabaseReference myRef = FirebaseDatabase.getInstance().getReference("users");
        user = FirebaseAuth.getInstance().getCurrentUser();

        if (ledgerData.get(position).getClassify().equals("지출")) {
            viewBinding.rbConsume.setChecked(true);
        } else {
            viewBinding.rbIncome.setChecked(true);
        }

        if (ledgerData.get(position).getLedgerContent().getCategory().equals("의류비")) {
            viewBinding.category.setSelection(0);
        } else if (ledgerData.get(position).getLedgerContent().getCategory().equals("식비")) {
            viewBinding.category.setSelection(1);
        } else if (ledgerData.get(position).getLedgerContent().getCategory().equals("주거비")) {
            viewBinding.category.setSelection(2);
        } else if (ledgerData.get(position).getLedgerContent().getCategory().equals("교통비")) {
            viewBinding.category.setSelection(3);
        } else if (ledgerData.get(position).getLedgerContent().getCategory().equals("생필품")) {
            viewBinding.category.setSelection(4);
        } else if (ledgerData.get(position).getLedgerContent().getCategory().equals("기타")) {
            viewBinding.category.setSelection(5);
        }

        viewBinding.date.setText(ledgerData.get(position).getYear() + "-" + ledgerData.get(position).getMonth() + "-" + ledgerData.get(position).getDay());
        viewBinding.price.setText(ledgerData.get(position).getLedgerContent().getPrice());
        viewBinding.description.setText(ledgerData.get(position).getLedgerContent().getDescription());

        // 가계부 수정 버튼 이벤트 - 사용자로부터 데이터를 입력받아 가계부 DB를 수정한다.
        viewBinding.submit.setOnClickListener(view -> {
            String category = viewBinding.category.getSelectedItem().toString();
            String price = viewBinding.price.getText().toString();
            String description = viewBinding.description.getText().toString();
            Map<String, String> ledger = new LedgerContent(category, price, description).toHashMap();

            // 기존 내역을 삭제하고
            String removeTargetTable = (viewBinding.rbConsume.isChecked()) ? "수입" : "지출";
            myRef.child(user.getUid()).child("Ledger").child(ledgerData.get(position).getYear())
                    .child(ledgerData.get(position).getMonth())
                    .child(ledgerData.get(position).getDay())
                    .child(removeTargetTable)
                    .child(ledgerData.get(position).getTimes())
                    .removeValue();

            // 새로 추가한다.
            String addTargetTable = (viewBinding.rbConsume.isChecked()) ? "지출" : "수입";
            myRef.child(user.getUid()).child("Ledger").child(ledgerData.get(position).getYear())
                    .child(ledgerData.get(position).getMonth())
                    .child(ledgerData.get(position).getDay())
                    .child(addTargetTable)
                    .child(ledgerData.get(position).getTimes())
                    .setValue(ledger);

            Toast.makeText(getContext(), "가계부가 수정되었습니다", Toast.LENGTH_SHORT).show();
            dismiss();
        });

        viewBinding.dismiss.setOnClickListener(view -> dismiss());
    }
}