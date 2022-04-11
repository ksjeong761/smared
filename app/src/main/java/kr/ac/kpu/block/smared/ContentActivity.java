package kr.ac.kpu.block.smared;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

public class ContentActivity extends AppCompatActivity  {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_content);

        TextView OCRResult = findViewById(R.id.OCRResult);
        TextView OCRToast = findViewById(R.id.OCRToast);

        Intent in = getIntent();
        OCRResult.setText("[ OCR 인식 결과 ]\n" + in.getStringExtra("result"));
        OCRToast.setText(in.getStringExtra("finalResult"));
    }
}
