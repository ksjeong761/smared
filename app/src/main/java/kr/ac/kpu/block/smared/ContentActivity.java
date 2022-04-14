package kr.ac.kpu.block.smared;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import kr.ac.kpu.block.smared.databinding.ActivityContentBinding;

public class ContentActivity extends AppCompatActivity  {
    private ActivityContentBinding viewBinding;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewBinding = ActivityContentBinding.inflate(getLayoutInflater());
        setContentView(viewBinding.getRoot());

        Intent intent = getIntent();
        viewBinding.OCRResult.setText("[ OCR 인식 결과 ]\n" + intent.getStringExtra("result"));
        viewBinding.OCRToast.setText(intent.getStringExtra("finalResult"));
    }
}
