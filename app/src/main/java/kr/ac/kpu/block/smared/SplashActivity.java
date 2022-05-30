package kr.ac.kpu.block.smared;

import android.app.Activity;
import android.content.Intent;
import android.os.Handler;
import android.os.Bundle;

// 프로그램을 실행했을 때 타이틀 이미지 보여주는 액티비티
public class SplashActivity extends Activity {
    private FormattedLogger logger = new FormattedLogger();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        startNextActivityAfterAnimation();
    }

    private void startNextActivityAfterAnimation() {
        final int SPLASH_TIME_MILLISECONDS = 2000;

        new Handler().postDelayed(() -> {
            startActivity(new Intent(SplashActivity.this, LoginActivity.class));
            overridePendingTransition(0, android.R.anim.fade_in);
            finish();
        }, SPLASH_TIME_MILLISECONDS);
    }
}
