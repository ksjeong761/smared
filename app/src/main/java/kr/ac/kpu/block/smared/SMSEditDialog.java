package kr.ac.kpu.block.smared;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.Window;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;

import kr.ac.kpu.block.smared.databinding.DialogLedgerEditBinding;

public class SMSEditDialog extends Dialog {
    private FormattedLogger logger = new FormattedLogger();
    private DialogLedgerEditBinding viewBinding;
    private PermissionChecker permissionChecker;

    private Ledger ledger;

    public SMSEditDialog(Context context, String smsOriginalMessage, long smsReceiptDateTime) {
        super(context);

        SMSParser smsParser =  new SMSParser();
        this.ledger = smsParser.parseSingleSMS(smsOriginalMessage, smsReceiptDateTime);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewBinding = DialogLedgerEditBinding.inflate(getLayoutInflater());
        setContentView(viewBinding.getRoot());
        requestWindowFeature(Window.FEATURE_NO_TITLE); // 타이틀 바 제거

        // UI 출력
        LocalDateTime localDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(ledger.getPaymentTimestamp()), TimeZone.getDefault().toZoneId());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        viewBinding.etDate.setText(localDateTime.format(formatter));
        viewBinding.etTotalPrice.setText(String.valueOf(ledger.getTotalPrice()));
        viewBinding.etDescription.setText(ledger.getDescription());

        // 이벤트 등록
        viewBinding.btnSubmit.setOnClickListener(view -> insertLedgerDB());
        viewBinding.btnCancel.setOnClickListener(view -> dismiss());
    }

    private void insertLedgerDB() {
        // UI에서 수정된 내용을 반영한다.
        ledger.setCategory(viewBinding.spnCategory.getSelectedItem().toString());
        ledger.setTotalPrice(Double.parseDouble(viewBinding.etTotalPrice.getText().toString()));
        ledger.setDescription(viewBinding.etDescription.getText().toString());

        // DB 경로를 지정한다.
        String timestamp = String.valueOf(ledger.getPaymentTimestamp());
        String userUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String databasePath = "ledger" + "/" + userUid + "/" + timestamp;
        DatabaseReference ledgerDBRef = FirebaseDatabase.getInstance().getReference(databasePath);

        // DB에 데이터를 저장한다.
        ledgerDBRef.setValue(ledger.toMap()).addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                Toast.makeText(getContext(), "저장에 실패하였습니다.", Toast.LENGTH_SHORT).show();
                return;
            }
            Toast.makeText(getContext(), "저장하였습니다.", Toast.LENGTH_SHORT).show();
        });

        dismiss();
    }
}
