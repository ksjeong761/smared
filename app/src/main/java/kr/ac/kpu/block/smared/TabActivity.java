package kr.ac.kpu.block.smared;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.Hashtable;

public class TabActivity extends AppCompatActivity  {
    //프래그먼트 사용
    Fragment fragment;

    //뒤로 가기를 두 번 연속으로 해야 종료되도록 하기 위해 시간을 저장
    long lastPressed;

    //데이터베이스 관련
    FirebaseDatabase database;
    DatabaseReference myRef;
    FirebaseUser user;

    //결제 문자
    String sms="";
    long smsdate=0;

    //하단 NavigationView 조작으로 다른 Fragment 화면을 보여준다.
    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {
        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                //기본 값으로 가계부 기록 화면을 보여주며 가계부 확인, 통계로 전환이 가능한 화면이다.
                case R.id.navigation_home:
                    fragment = new HomeFragment();
                    switchFragment(fragment);
                    return true;

                //가계부 공유 화면
                case R.id.navigation_share:
                    fragment = new ShareFragment();
                    switchFragment(fragment);
                    return true;

                //사용자 프로필 화면
                case R.id.navigation_profile:
                    fragment = new ProfileFragment();
                    switchFragment(fragment);
                    return true;
            }
            //[Refactoring] default에 넣는게 더 깔끔할 듯 하다
            return false;
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tab);

        //다른 액티비티로부터 데이터를 넘겨받는다.
        Intent in = getIntent();
        sms = in.getStringExtra("sms");
        smsdate = in.getLongExtra("smsdate",0);

        //결제 문자 온 게 있다면 SMSEditDialog로 넘어간다.
        if (sms != null) {
            SMSEditDialog editDialog = new SMSEditDialog(TabActivity.this,sms,smsdate);
            editDialog.show();
        }

        //DB에서 사용자 정보를 가져온다.
        database = FirebaseDatabase.getInstance();
        myRef = database.getReference("users");
        user = FirebaseAuth.getInstance().getCurrentUser();

        //[???] news는 구글링 해서 그대로 가져다 쓴 문자열, 정확히 뭘 구독했었는지?
        FirebaseMessaging.getInstance().subscribeToTopic("news");

        //토큰으로 로그인 상태를 관리한다.
        Hashtable<String, Object> token = new Hashtable<>();
        token.put("fcmToken", FirebaseInstanceId.getInstance().getToken());
        myRef.child(user.getUid()).updateChildren(token);

        //[Refactoring] switchFragment 함수 사용으로 코드 압축 고려
        //홈 액티비티가 기본으로 보여진다.
        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        HomeFragment fragment = new HomeFragment();
        fragmentTransaction.add(R.id.list,fragment);
        fragmentTransaction.commit();

        //하단 NavigationView 조작으로 다른 Fragment 화면을 보여준다.
        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
    }

    @Override
    public void onBackPressed() {
        //1.5초 이내에 뒤로가기가 연속으로 눌렸다면 액티비티를 종료한다.
        if (System.currentTimeMillis() - lastPressed < 1500) {
            finish();
        }

        Toast.makeText(this,"한번 더 누르시면 종료됩니다.",Toast.LENGTH_SHORT).show();
        lastPressed = System.currentTimeMillis();
    }

    public void switchFragment(Fragment fragment) {
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        //[???] list가 뭐였지?? -> 화면 중앙에 출력될 프래그먼트 id, 검색어 : id/list
        transaction.replace(R.id.list, fragment);
        transaction.commit();
    }
}