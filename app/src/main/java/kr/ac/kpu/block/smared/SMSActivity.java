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
import android.support.v7.widget.RecyclerView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

public class SMSActivity extends AppCompatActivity {

    static final int SMS_RECEIVE_PERMISSION = 1;

    RecyclerView mRecyclerView;
    LinearLayoutManager mLayoutManager;
    SMSAdapter mAdapter;
    Button btnSMSLoad;
    TextView tvCountSMS;

    List<SMS> mBody;
    SMS mSMS;

    boolean isSMSLoaded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sms);

        tvCountSMS = findViewById(R.id.tvCountSMS);
        btnSMSLoad = findViewById(R.id.btnLoadSMS);
        mRecyclerView = findViewById(R.id.rvSMS);

        mRecyclerView.setHasFixedSize(true);
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mBody = new ArrayList<>();
        mAdapter = new SMSAdapter(mBody, this);
        mRecyclerView.setAdapter(mAdapter);

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
            Toast.makeText(getApplicationContext(), "SMS 리시버권한 없음", Toast.LENGTH_SHORT).show();

            // Dialog를 통해 요청된 권한을 사용자가 거부했을 경우 알림 메시지를 보여준다.
            // 사용자가 "Don't ask again"을 체크한 경우 알림 메시지는 보여지지 않는다.
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECEIVE_SMS)) {
                Toast.makeText(getApplicationContext(), "SMS권한이 필요합니다", Toast.LENGTH_SHORT).show();
            }

            // 다시 권한을 요청한다.
            ActivityCompat.requestPermissions(this, new String[]{ Manifest.permission.RECEIVE_SMS}, SMS_RECEIVE_PERMISSION);
        }

        // SMS 불러오기 버튼 이벤트 -> 모든 SMS를 확인해서 신한 체크카드 결제 문자인 경우 파싱 후 리스트로 화면에 보여준다.
        btnSMSLoad.setOnClickListener(view -> {
            // 한 번만 불러오게 하기
            if (isSMSLoaded) {
                return;
            }

            // 전체 문자 받아오기
            Uri allMessage = Uri.parse("content://sms/inbox");
            Cursor cur = getContentResolver().query(allMessage, null, null, null, null);
            int smsCount = 0;

            while (cur.moveToNext()) {
                String msg = cur.getString(cur.getColumnIndex("body"));

                // 현재 신한 체크카드 메시지만 처리할 수 있다.
                if (!msg.contains("신한체크승인")) {
                    continue;
                }

                // SMS에서 날짜를 파싱한다.
                long date = cur.getLong(cur.getColumnIndex("date"));
                String sdate = new SimpleDateFormat("yyyyMMdd HH:mm:ss").format(date);
                String timedate = new SimpleDateFormat("HH:mm:ss").format(date);

                // SMS에서 결제 금액을 파싱한다.
                StringTokenizer tokenizer = new StringTokenizer(msg, " ");
                tokenizer.nextToken();
                tokenizer.nextToken();
                tokenizer.nextToken();
                tokenizer.nextToken();
                String price = tokenizer.nextToken();

                // 결제 금액 문자열에서 숫자만 남긴다.
                price.trim();
                price = price.replace(",","");
                price = price.replace("원","");

                // SMS에서 결제 내역 상세와 가게명을 파싱한다.
                String payMemo = tokenizer.nextToken();
                String store = tokenizer.nextToken();
                if (!store.contains("잔액")) {
                    payMemo += store;
                }

                mSMS = new SMS();
                mSMS.setPayMemo(payMemo);
                mSMS.setPrice(price);
                mSMS.setYear(sdate.substring(0,4));
                mSMS.setMonth(sdate.substring(4,6));
                mSMS.setDay(sdate.substring(6,8));
                mSMS.setTime(timedate);

                // RecyclerView에 SMS 목록을 보여준다.
                mBody.add(mSMS);
                mRecyclerView.scrollToPosition(0);
                mAdapter.notifyItemInserted(mBody.size() - 1);

                smsCount++;
            }

            tvCountSMS.setText(smsCount + "건의 기록이 확인되었습니다.");

            // 한 번만 불러오게 하기
            isSMSLoaded = true;
        });
    }

    // 권한을 요청했을 경우 결과 콜백
    public void onRequestPermissionsResult(int requestCode, String permissions[], int grantResults[]){
        switch(requestCode){
            case SMS_RECEIVE_PERMISSION:
                String toastMessage = "SMS 권한 거부됨";

                if((grantResults.length > 0) && (grantResults[0] == PackageManager.PERMISSION_GRANTED)){
                    toastMessage = "SMS 권한 승인됨";
                }

                Toast.makeText(getApplicationContext(), toastMessage, Toast.LENGTH_SHORT).show();
                break;
        }
    }
}
