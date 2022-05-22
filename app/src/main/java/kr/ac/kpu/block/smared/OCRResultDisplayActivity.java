package kr.ac.kpu.block.smared;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import kr.ac.kpu.block.smared.databinding.ActivityOcrResultDisplayBinding;

public class OCRResultDisplayActivity extends AppCompatActivity  {
    private FormattedLogger logger = new FormattedLogger();
    private ActivityOcrResultDisplayBinding viewBinding;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewBinding = ActivityOcrResultDisplayBinding.inflate(getLayoutInflater());
        setContentView(viewBinding.getRoot());

        Intent previousIntent = getIntent();
        viewBinding.OCRResult.setText("[ OCR 인식 결과 ]\n" + previousIntent.getStringExtra("result"));
        viewBinding.OCRToast.setText(previousIntent.getStringExtra("finalResult"));
    }
}
