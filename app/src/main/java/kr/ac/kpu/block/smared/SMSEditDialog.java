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
import java.util.StringTokenizer;
import java.util.TimeZone;

import kr.ac.kpu.block.smared.databinding.DialogLedgerEditBinding;

public class SMSEditDialog extends Dialog {
    private FormattedLogger logger = new FormattedLogger();
    private DialogLedgerEditBinding viewBinding;

    private String smsOriginalMessage = "";         // 문자 메시지
    private long smsReceiptDateTime = 0;        // 문자 메시지 수신 시간

    public SMSEditDialog(Context context, String smsOriginalMessage, long smsReceiptDateTime) {
        super(context);
        this.smsOriginalMessage = smsOriginalMessage;
        this.smsReceiptDateTime = smsReceiptDateTime;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewBinding = DialogLedgerEditBinding.inflate(getLayoutInflater());
        setContentView(viewBinding.getRoot());
        requestWindowFeature(Window.FEATURE_NO_TITLE); // 타이틀 바 제거

        parseSMSAndShowData();

        // 이벤트 등록
        viewBinding.btnSubmit.setOnClickListener(view -> insertLedgerDB());
        viewBinding.btnCancel.setOnClickListener(view -> dismiss());
    }

    private void parseSMSAndShowData() {
        // SMS를 받은 시간을 기록한다.
        LocalDateTime localDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(smsReceiptDateTime), TimeZone.getDefault().toZoneId());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        viewBinding.etDate.setText(localDateTime.format(formatter));

        // SMS에서 결제 금액에서 숫자만을 파싱한다.
        String smsPrice;
        StringTokenizer tokenizer = new StringTokenizer(smsOriginalMessage, " ");
        tokenizer.nextToken();
        tokenizer.nextToken();
        tokenizer.nextToken();
        tokenizer.nextToken();
        smsPrice = tokenizer.nextToken().trim();
        smsPrice = smsPrice.replace(",","");
        smsPrice = smsPrice.replace("원","");
        viewBinding.etTotalPrice.setText(smsPrice);

        // SMS에서 결제 내역 상세와 가게명을 파싱한다.
        String smsDescription = tokenizer.nextToken();
        String smsStoreName = tokenizer.nextToken();
        if (!smsStoreName.contains("잔액")) {
            smsDescription += smsStoreName;
        }
        viewBinding.etDescription.setText(smsDescription);
    }

    private void insertLedgerDB() {
        // 데이터를 객체에 반영한다.
        Ledger ledger = new Ledger();
        ledger.setCategory(viewBinding.spnCategory.getSelectedItem().toString());
        ledger.setTotalPrice(Double.parseDouble(viewBinding.etTotalPrice.getText().toString()));
        ledger.setDescription(viewBinding.etDescription.getText().toString());
        ledger.setPaymentTimestamp(smsReceiptDateTime);

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
