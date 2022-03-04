package kr.ac.kpu.block.smared;

import android.app.Activity;
import android.content.Intent;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

// 프로그램을 실행했을 때 이미지 보여주는 액티비티
public class SplashActivity extends Activity {

    final int SPLASH_TIME = 2000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 프로그램을 실행했을 때 이미지를 보여주고 메인 액티비티로 넘어간다.
        new Handler().postDelayed(()->{
            overridePendingTransition(0, android.R.anim.fade_in);
            startActivity(new Intent(SplashActivity.this, MainActivity.class));
            finish();
        }, SPLASH_TIME);
    }
}
