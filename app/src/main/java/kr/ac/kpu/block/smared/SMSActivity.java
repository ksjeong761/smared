package kr.ac.kpu.block.smared;

import android.Manifest;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import kr.ac.kpu.block.smared.databinding.ActivitySmsBinding;

public class SMSActivity extends AppCompatActivity {
    private FormattedLogger logger = new FormattedLogger();
    private ActivitySmsBinding viewBinding;

    private final int SMS_RECEIVE_PERMISSION = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewBinding = ActivitySmsBinding.inflate(getLayoutInflater());
        setContentView(viewBinding.getRoot());

        viewBinding.rvSMS.setHasFixedSize(true);
        viewBinding.rvSMS.setLayoutManager(new LinearLayoutManager(this));
        List<SMS> smsList = new ArrayList<>();
        SMSAdapter smsAdapter = new SMSAdapter(smsList, this);
        viewBinding.rvSMS.setAdapter(smsAdapter);

        // SMS 읽기 권한이 부여되어 있는지 확인
        if (PackageManager.PERMISSION_GRANTED != ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_SMS)) {
            Toast.makeText(getApplicationContext(), "SMS 읽기 권한 없음", Toast.LENGTH_SHORT).show();

            // Dialog를 통해 요청된 권한을 사용자가 거부했을 경우 알림 메시지를 보여준다.
            // 사용자가 "Don't ask again"을 체크한 경우 알림 메시지는 보여지지 않는다.
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.READ_SMS)){
                Toast.makeText(getApplicationContext(), "SMS 읽기 권한이 필요합니다", Toast.LENGTH_SHORT).show();
            }

            // 다시 권한을 요청한다.
            ActivityCompat.requestPermissions(this, new String[]{ android.Manifest.permission.READ_SMS}, SMS_RECEIVE_PERMISSION);
        }

        // SMS 수신 권한이 부여되어 있는지 확인
        if (PackageManager.PERMISSION_GRANTED != ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS)) {
            Toast.makeText(getApplicationContext(), "SMS 수신 권한 없음", Toast.LENGTH_SHORT).show();

            // Dialog를 통해 요청된 권한을 사용자가 거부했을 경우 알림 메시지를 보여준다.
            // 사용자가 "Don't ask again"을 체크한 경우 알림 메시지는 보여지지 않는다.
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECEIVE_SMS)) {
                Toast.makeText(getApplicationContext(), "SMS 수신 권한이 필요합니다", Toast.LENGTH_SHORT).show();
            }

            // 다시 권한을 요청한다.
            ActivityCompat.requestPermissions(this, new String[]{ Manifest.permission.RECEIVE_SMS}, SMS_RECEIVE_PERMISSION);
        }

        // SMS 불러오기 버튼 이벤트 -> 모든 SMS를 확인해서 신한 체크카드 결제 문자인 경우 파싱 후 리스트로 화면에 보여준다.
        viewBinding.btnLoadSMS.setOnClickListener(view -> {
            // 한 번만 불러오게 하기
            if (smsList.size() > 0) {
                return;
            }

            // 전체 문자 받아오기
            Cursor smsCursor = getContentResolver().query(Uri.parse("content://sms/inbox"), null, null, null, null);
            while (smsCursor.moveToNext()) {
                // 현재 신한 체크카드 메시지만 처리할 수 있다.
                String msg = smsCursor.getString(smsCursor.getColumnIndex("body"));
                if (!msg.contains("신한체크승인")) {
                    continue;
                }

                // 결제 금액을 파싱한다.
                StringTokenizer tokenizer = new StringTokenizer(msg, " ");
                tokenizer.nextToken();
                tokenizer.nextToken();
                tokenizer.nextToken();
                tokenizer.nextToken();

                // 결제 금액 문자열에서 숫자만 남긴다.
                String price = tokenizer.nextToken();
                price.trim();
                price = price.replace(",","");
                price = price.replace("원","");

                // 결제 내역 상세를 파싱한다.
                String description = tokenizer.nextToken();
                String storeName = tokenizer.nextToken();
                if (!storeName.contains("잔액")) {
                    description += storeName;
                }

                // 날짜를 파싱한다.
                long smsReceiptDate = smsCursor.getLong(smsCursor.getColumnIndex("date"));
                String timeToParse = new SimpleDateFormat("yyyyMMdd HH:mm:ss").format(smsReceiptDate);

                SMS sms = new SMS();
                sms.setDescription(description);
                sms.setPrice(price);
                sms.setYear(timeToParse.substring(0,4));
                sms.setMonth(timeToParse.substring(4,6));
                sms.setDay(timeToParse.substring(6,8));
                sms.setTime(timeToParse.substring(9, 17));

                // RecyclerView에 문자 목록을 보여준다.
                smsList.add(sms);
                smsAdapter.notifyItemInserted(smsList.size() - 1);
                viewBinding.rvSMS.scrollToPosition(0);
            }

            viewBinding.tvCountSMS.setText(smsList.size() + "건의 기록이 확인되었습니다.");
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int grantResults[]) {
        switch (requestCode) {
            case SMS_RECEIVE_PERMISSION:
                if (grantResults.length > 0 && PackageManager.PERMISSION_GRANTED == grantResults[0]) {
                    Toast.makeText(getApplicationContext(), "SMS권한 승인함", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getApplicationContext(), "SMS권한 거부함", Toast.LENGTH_SHORT).show();
                }

                break;
        }
    }
}
