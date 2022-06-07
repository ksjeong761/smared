package kr.ac.kpu.block.smared;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;

import java.util.HashMap;
import java.util.Map;

import kr.ac.kpu.block.smared.databinding.ActivityTabBinding;

public class TabActivity extends AppCompatActivity  {
    private FormattedLogger logger = new FormattedLogger();
    private ActivityTabBinding viewBinding;
    private PermissionChecker permissionChecker;

    // 뒤로 가기를 연속으로 했을 경우에만 프로그램을 종료하도록 하기 위해 시간을 저장
    private long lastPressedTime;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewBinding = ActivityTabBinding.inflate(getLayoutInflater());
        setContentView(viewBinding.getRoot());

        final String[] necessaryPermissions = {
                android.Manifest.permission.READ_SMS,
                android.Manifest.permission.RECEIVE_SMS
        };
        permissionChecker = new PermissionChecker(this, necessaryPermissions);

        // SMSReceiver에서 등록한 푸시 알림을 터치했을 경우 PendingIndent를 통해 데이터를 넘겨받는다.
        Intent previousIntent = getIntent();
        String smsMessage = previousIntent.getStringExtra("sms"); // 문자 메시지
        long smsReceivedDate = previousIntent.getLongExtra("smsdate",0); // 문자 메시지 수신 시간

        // 결제 문자가 있다면 SMSEditDialog로 넘어간다.
        if (smsMessage != null) {
            SMSEditDialog editDialog = new SMSEditDialog(TabActivity.this, smsMessage, smsReceivedDate);
            editDialog.show();
        }

        // 가계부 화면이 기본으로 보여진다.
        switchFragment(new LedgerHomeFragment());

        // 하단 NavigationView 조작으로 다른 Fragment 화면을 보여준다.
        viewBinding.navigation.setOnNavigationItemSelectedListener(menuItem -> {
            switch (menuItem.getItemId()) {
                // 가계부 화면
                case R.id.navigation_home:
                    switchFragment(new LedgerHomeFragment());
                    return true;

                // 사용자 프로필 화면
                case R.id.navigation_profile:
                    switchFragment(new UserProfileFragment());
                    return true;

                default:
                    return false;
            }
        });
    }

    // 매개변수로 받은 Fragment를 화면에 보여준다.
    private void switchFragment(Fragment fragment) {
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.replace(R.id.list, fragment);
        transaction.commit();
    }

    // 1.5초 이내에 뒤로가기가 연속으로 눌렸다면 액티비티를 종료한다.
    @Override
    public void onBackPressed() {
        if (System.currentTimeMillis() - lastPressedTime < 1500) {
            finish();
        }

        lastPressedTime = System.currentTimeMillis();
        Toast.makeText(this,"한번 더 누르시면 종료됩니다.",Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int grantResults[]) {
        if (!permissionChecker.isPermissionRequestSuccessful(grantResults)) {
            Toast.makeText(this, "권한 요청에 동의 해주셔야 이용 가능합니다. 설정에서 권한을 허용해주시기 바랍니다.", Toast.LENGTH_SHORT).show();
            finish();
        }
    }
}