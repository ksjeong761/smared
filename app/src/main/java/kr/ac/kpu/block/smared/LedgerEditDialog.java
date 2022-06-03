package kr.ac.kpu.block.smared;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.Window;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;

import kr.ac.kpu.block.smared.databinding.DialogLedgerEditBinding;

public class LedgerEditDialog extends Dialog {
    private FormattedLogger logger = new FormattedLogger();
    private DialogLedgerEditBinding viewBinding;

    private List<Ledger> ledgerData;
    private int selectedLedgerIndex;

    LedgerEditDialog(Context context, List<Ledger> ledgerData, int selectedLedgerIndex) {
        super(context);
        this.ledgerData = ledgerData;
        this.selectedLedgerIndex = selectedLedgerIndex;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewBinding = DialogLedgerEditBinding.inflate(getLayoutInflater());
        setContentView(viewBinding.getRoot());
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        // UI 초기화
        viewBinding.etDate.setText(ledgerData.get(selectedLedgerIndex).getPaymentTimestamp("yyyy-MM-dd"));
        viewBinding.etTotalPrice.setText(String.valueOf(ledgerData.get(selectedLedgerIndex).getTotalPrice()));
        viewBinding.etDescription.setText(ledgerData.get(selectedLedgerIndex).getDescription());
        int categoryIndex = ledgerData.get(selectedLedgerIndex).getCategoryIndex();
        if (categoryIndex >= 0) {
            viewBinding.spnCategory.setSelection(categoryIndex);
        }

        // 이벤트 등록
        viewBinding.btnSubmit.setOnClickListener(view -> updateLedgerDB());
        viewBinding.btnCancel.setOnClickListener(view -> dismiss());
    }

    private void updateLedgerDB() {
        String timestamp = String.valueOf(ledgerData.get(selectedLedgerIndex).getPaymentTimestamp());
        String userUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String databasePath = "ledger" + "/"+ userUid + "/" + timestamp;
        DatabaseReference ledgerDBRef = FirebaseDatabase.getInstance().getReference(databasePath);

        // DB에서 데이터를 읽어오기 위해 이벤트를 등록해야 한다.
        ledgerDBRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // DB에서 기존 내역을 불러온다.
                Ledger ledger = dataSnapshot.getValue(Ledger.class);
                if (ledger == null) {
                    Toast.makeText(getContext(), "기존 내역 불러오기에 실패하였습니다.", Toast.LENGTH_SHORT).show();
                    return;
                }

                // 불러온 기존 내역을 수정한다.
                ledger.setCategory(viewBinding.spnCategory.getSelectedItem().toString());
                ledger.setTotalPrice(viewBinding.etTotalPrice.getText().toString());
                ledger.setDescription(viewBinding.etDescription.getText().toString());

                // DB에서 수정하기 전 데이터를 삭제한다.
                ledgerDBRef.removeValue().addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Toast.makeText(getContext(), "기존 내역 삭제에 실패하였습니다.", Toast.LENGTH_SHORT).show();
                    }
                });

                // DB에 다시 데이터를 추가하는 것으로 수정한 내용을 반영한다.
                ledgerDBRef.setValue(ledger.toMap()).addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Toast.makeText(getContext(), "가계부 수정에 실패하였습니다.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Toast.makeText(getContext(), "가계부가 수정되었습니다", Toast.LENGTH_SHORT).show();
                    dismiss();
                });
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                logger.writeLog("기존 내역 불러오기에 실패하였습니다. - " + databaseError.toException());
            }
        });
    }
}