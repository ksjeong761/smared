package kr.ac.kpu.block.smared;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import kr.ac.kpu.block.smared.databinding.ActivityImageProcessingBinding;

import static android.view.Gravity.CENTER;

public class ImageProcessingActivity extends AppCompatActivity {

    static {
        System.loadLibrary("opencv_java3");
        System.loadLibrary("native-lib");
    }

    // NDK를 사용하는 네이티브 메서드 정의
    public native void loadImage(String imageFileName, long img);
    public native void imageprocessing(long inputImage, long outputImage);

    private ActivityImageProcessingBinding viewBinding;

    static final int PERMISSION_REQUEST_CODE = 1;
    String[] permissions = {
        "android.permission.WRITE_EXTERNAL_STORAGE"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewBinding = ActivityImageProcessingBinding.inflate(getLayoutInflater());
        setContentView(viewBinding.getRoot());

        LinearLayout layout = new LinearLayout(ImageProcessingActivity.this);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setGravity(CENTER);
        Button scan = new Button(ImageProcessingActivity.this);
        Button camera = new Button(ImageProcessingActivity.this);
        scan.setText("스캔 파일");
        camera.setText("촬영한 파일");
        layout.addView(scan);
        layout.addView(camera);

        AlertDialog.Builder alertdialog = new AlertDialog.Builder(ImageProcessingActivity.this);
        alertdialog.setView(layout);
        alertdialog.setTitle("파일 타입을 골라주세요");
        AlertDialog alert = alertdialog.create();
        alert.show();

        for (String perms : permissions){
            if (PackageManager.PERMISSION_GRANTED == checkCallingOrSelfPermission(perms)) {
                continue;
            }

            // 마시멜로( API 23 )이상에서 런타임 권한(Runtime Permission) 요청
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                this.requestPermissions(permissions, PERMISSION_REQUEST_CODE);
            }
        }

        scan.setOnClickListener(view -> {
            imageprocess_and_showResult();
            alert.cancel();
        });

        camera.setOnClickListener(view -> {
            imageprocess_and_showResult();
            alert.cancel();
        });

        viewBinding.btnRunOCR.setOnClickListener(v -> {
            viewBinding.pbLogins.setVisibility(View.VISIBLE);
            startActivity(new Intent(ImageProcessingActivity.this, CloudActivity.class));
            viewBinding.pbLogins.setVisibility(View.GONE);
        });
    }

    @Override
    public void onRequestPermissionsResult(int permsRequestCode, String[] permissions, int[] grantResults){
        switch (permsRequestCode) {
            case PERMISSION_REQUEST_CODE:
                if (grantResults.length == 0) {
                    break;
                }

                if (PackageManager.PERMISSION_GRANTED == grantResults[1]) {
                    break;
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    this.showDialogforPermission("앱을 실행하려면 권한을 허가하셔야합니다.");
                }

                break;
        }
    }

    private void showDialogforPermission(String msg) {
        final AlertDialog.Builder myDialog = new AlertDialog.Builder(ImageProcessingActivity.this);
        myDialog.setTitle("알림");
        myDialog.setMessage(msg);
        myDialog.setCancelable(false);
        myDialog.setPositiveButton("예", (arg0, arg1) -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                this.requestPermissions(permissions, PERMISSION_REQUEST_CODE);
            }
        });

        myDialog.setNegativeButton("아니오", (arg0, arg1) -> finish());
        myDialog.show();
    }

    public void SaveBitmapFile(Bitmap bitmap, String directoryPath, String fileName) {
        // 폴더가 없다면 만든다.
        File directory = new File(directoryPath);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        // 새 파일을 만들고 스트림을 통해 파일에 내용을 쓴다.
        OutputStream outputStream = null;

        try {
            File bitmapFile = new File(directoryPath + fileName);
            bitmapFile.createNewFile();
            outputStream = new FileOutputStream(bitmapFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            try {
                outputStream.close();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void imageprocess_and_showResult() {
        Intent previousIntent = getIntent();
        String ImagePath = previousIntent.getStringExtra("ipath");
        Uri photoUri = previousIntent.getParcelableExtra("input");

        // 원본 이미지 읽기
        Mat img_input = new Mat();
        loadImage(ImagePath, img_input.getNativeObjAddr());

        // 영상처리 결과 이미지 저장
        Mat img_output = new Mat();
        imageprocessing(img_input.getNativeObjAddr(), img_output.getNativeObjAddr());

        // OpenCV Mat -> Bitmap으로 이미지 변환
        Bitmap bitmapOutput = Bitmap.createBitmap(img_output.cols(), img_output.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(img_output, bitmapOutput);

        // 화면에 출력
        viewBinding.imageViewInput.setImageBitmap(bitmapOutput);
        viewBinding.imageViewInput.setImageURI(photoUri);

        // 파일로 저장
        SaveBitmapFile(bitmapOutput, "/storage/emulated/0/SmaRed/", "s2.jpg");
    }
}
