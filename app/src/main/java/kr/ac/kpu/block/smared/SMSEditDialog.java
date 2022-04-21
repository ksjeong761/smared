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

import java.text.SimpleDateFormat;
import java.util.Hashtable;
import java.util.StringTokenizer;

import kr.ac.kpu.block.smared.databinding.DialogEditBinding;

public class SMSEditDialog extends Dialog {
    private FormattedLogger logger = new FormattedLogger();
    private DialogEditBinding viewBinding;

    // 데이터베이스 관련
    private FirebaseDatabase database;
    private DatabaseReference myRef;
    private FirebaseUser user;

    // SMS
    private String sms = "";         // 문자 메시지
    private long smsdate = 0;        // 문자 메시지 수신 시간

    public SMSEditDialog(Context context, String sms, long smsdate) {
        super(context);
        this.sms = sms;
        this.smsdate = smsdate;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewBinding = DialogEditBinding.inflate(getLayoutInflater());
        setContentView(viewBinding.getRoot());

        // 타이틀 바 제거
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        database = FirebaseDatabase.getInstance();
        myRef = database.getReference("users");
        user = FirebaseAuth.getInstance().getCurrentUser();

        // SMS에서 날짜를 파싱한다.
        String sdate = new SimpleDateFormat("yyyy-MM-dd").format(smsdate);
        String fdate = new SimpleDateFormat("HH:mm:ss").format(smsdate);
        String stYear = sdate.substring(0, 4);
        String stMonth = sdate.substring(5, 7);
        String stDay = sdate.substring(8, 10);

        // SMS에서 결제 금액을 파싱한다.
        StringTokenizer tokenizer = new StringTokenizer(sms, " ");
        tokenizer.nextToken();
        tokenizer.nextToken();
        tokenizer.nextToken();
        tokenizer.nextToken();
        String smsprice = tokenizer.nextToken();

        // 결제 금액 문자열에서 숫자만 남긴다.
        smsprice.trim();
        smsprice = smsprice.replace(",","");
        smsprice = smsprice.replace("원","");

        // SMS에서 결제 내역 상세와 가게명을 파싱한다.
        String smspayMemo = tokenizer.nextToken();
        String smsstore = tokenizer.nextToken();
        if (!smsstore.contains("잔액")) {
            smspayMemo += smsstore;
        }

        // 화면에 파싱한 정보를 보여준다.
        viewBinding.price.setText(smsprice);
        viewBinding.payMemo.setText(smspayMemo);
        viewBinding.date.setText(sdate);

        // 등록 버튼 이벤트 -> 받은 문자에서 파싱한 정보를 가계부 DB에 등록한다.
        viewBinding.submit.setOnClickListener(v -> {
            Hashtable<String, String> ledger  = new Hashtable<String, String>();
            ledger.put("useItem", viewBinding.useitem.getSelectedItem().toString());
            ledger.put("price", viewBinding.price.getText().toString());
            ledger.put("paymemo", viewBinding.payMemo.getText().toString());

            String stConsume = (viewBinding.rbConsume.isChecked()) ? "지출" : "수입";
            myRef.child(user.getUid()).child("Ledger").child(stYear).child(stMonth).child(stDay).child(stConsume).child(fdate).setValue(ledger);

            Toast.makeText(getContext(), "가계부가 추가되었습니다", Toast.LENGTH_SHORT).show();
            dismiss();
        });

        // 취소 버튼 이벤트 - 다이얼로그를 닫는다.
        viewBinding.dismiss.setOnClickListener(v -> dismiss());
    }
}
