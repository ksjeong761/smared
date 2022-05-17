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

import java.text.SimpleDateFormat;
import java.util.Map;
import java.util.StringTokenizer;

import kr.ac.kpu.block.smared.databinding.DialogLedgerEditBinding;

public class SMSEditDialog extends Dialog {
    private FormattedLogger logger = new FormattedLogger();
    private DialogLedgerEditBinding viewBinding;

    // 데이터베이스 관련
    private FirebaseDatabase database;
    private DatabaseReference myRef;
    private FirebaseUser user;

    // SMS
    private String smsMessage = "";         // 문자 메시지
    private long smsReceiptDateTime = 0;        // 문자 메시지 수신 시간

    public SMSEditDialog(Context context, String smsMessage, long smsReceiptDateTime) {
        super(context);
        this.smsMessage = smsMessage;
        this.smsReceiptDateTime = smsReceiptDateTime;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewBinding = DialogLedgerEditBinding.inflate(getLayoutInflater());
        setContentView(viewBinding.getRoot());

        // 타이틀 바 제거
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        database = FirebaseDatabase.getInstance();
        myRef = database.getReference("users");
        user = FirebaseAuth.getInstance().getCurrentUser();

        // SMS에서 날짜를 파싱한다.
        String smsReceiptDate = new SimpleDateFormat("yyyy-MM-dd").format(smsReceiptDateTime);
        String year = smsReceiptDate.substring(0, 4);
        String month = smsReceiptDate.substring(5, 7);
        String day = smsReceiptDate.substring(8, 10);

        // SMS에서 결제 금액을 파싱한다.
        StringTokenizer tokenizer = new StringTokenizer(smsMessage, " ");
        tokenizer.nextToken();
        tokenizer.nextToken();
        tokenizer.nextToken();
        tokenizer.nextToken();
        String smsPrice = tokenizer.nextToken();

        // 결제 금액 문자열에서 숫자만 남긴다.
        smsPrice.trim();
        smsPrice = smsPrice.replace(",","");
        smsPrice = smsPrice.replace("원","");

        // SMS에서 결제 내역 상세와 가게명을 파싱한다.
        String smsDescription = tokenizer.nextToken();
        String smsStoreName = tokenizer.nextToken();
        if (!smsStoreName.contains("잔액")) {
            smsDescription += smsStoreName;
        }

        // 화면에 파싱한 정보를 보여준다.
        viewBinding.price.setText(smsPrice);
        viewBinding.description.setText(smsDescription);
        viewBinding.date.setText(smsReceiptDate);

        // 등록 버튼 이벤트 -> 받은 문자에서 파싱한 정보를 가계부 DB에 등록한다.
        viewBinding.submit.setOnClickListener(view -> {
            String category = viewBinding.category.getSelectedItem().toString();
            String price = viewBinding.price.getText().toString();
            String description = viewBinding.description.getText().toString();
            Map<String, String> ledger = new LedgerContent(category, price, description).toHashMap();

            String incomeOrExpenditure = (viewBinding.rbConsume.isChecked()) ? "지출" : "수입";
            String smsReceiptTime = new SimpleDateFormat("HH:mm:ss").format(smsReceiptDateTime);
            myRef.child(user.getUid()).child("Ledger").child(year).child(month).child(day).child(incomeOrExpenditure).child(smsReceiptTime).setValue(ledger);

            Toast.makeText(getContext(), "가계부가 추가되었습니다", Toast.LENGTH_SHORT).show();
            dismiss();
        });

        // 취소 버튼 이벤트 - 다이얼로그를 닫는다.
        viewBinding.dismiss.setOnClickListener(view -> dismiss());
    }
}
