package kr.ac.kpu.block.smared;

import android.annotation.SuppressLint;
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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import static android.view.Gravity.CENTER;

public class ImageProcessingActivity extends AppCompatActivity {
    static {
        System.loadLibrary("opencv_java3");
        System.loadLibrary("native-lib");
    }

    ImageView imageVIewInput;
    ImageView imageVIewOuput;
    Button btnRunOCR;
    private Mat img_input;
    private Mat img_output;
    String ImagePath;
    ProgressBar pbLogins;
    private static final String TAG = "opencv";
    static final int PERMISSION_REQUEST_CODE = 1;
    String[] PERMISSIONS  = {"android.permission.WRITE_EXTERNAL_STORAGE"};
    int fileCheck=0;
    Uri photoUri;

    @SuppressLint("WrongConstant")
    private boolean hasPermissions(String[] permissions) {
        // 필요한 권한들이 모두 허가되었는지 확인
        for (String perms : permissions){
            if (checkCallingOrSelfPermission(perms) != PackageManager.PERMISSION_GRANTED){
                // 권한이 허가되지 않은 경우
                return false;
            }
        }

        // 필요한 모든 권한이 허가된 경우
        return true;
    }

    private void requestNecessaryPermissions(String[] permissions) {
        // 마시멜로( API 23 )이상에서 런타임 권한(Runtime Permission) 요청
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(permissions, PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int permsRequestCode, String[] permissions, int[] grantResults){
        switch(permsRequestCode){
            case PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0) {
                    boolean writeAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (!writeAccepted) {
                            showDialogforPermission("앱을 실행하려면 권한을 허가하셔야합니다.");
                            return;
                        }
                    }
                }

                break;
        }
    }

    private void showDialogforPermission(String msg) {
        final AlertDialog.Builder myDialog = new AlertDialog.Builder(  ImageProcessingActivity.this);
        myDialog.setTitle("알림");
        myDialog.setMessage(msg);
        myDialog.setCancelable(false);
        myDialog.setPositiveButton("예", (arg0, arg1) -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(PERMISSIONS, PERMISSION_REQUEST_CODE);
            }
        });

        myDialog.setNegativeButton("아니오", (arg0, arg1) -> finish());
        myDialog.show();
    }

    public void SaveBitmapToFileCache(Bitmap bitmap, String strFilePath, String filename) {
        File file = new File(strFilePath);

        if (!file.exists()) {
            file.mkdirs();
        }

        File fileCacheItem = new File(strFilePath + filename);
        OutputStream out = null;

        try {
            fileCacheItem.createNewFile();
            out = new FileOutputStream(fileCacheItem);

            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_processing);

        imageVIewInput = findViewById(R.id.imageViewInput);
        imageVIewOuput = findViewById(R.id.imageViewOutput);
        pbLogins = findViewById(R.id.pbLogins);
        pbLogins.setVisibility(View.GONE);
        btnRunOCR = findViewById(R.id.btnRunOCR);
        Intent intent = getIntent();
        ImagePath = intent.getStringExtra("ipath");
        photoUri = intent.getParcelableExtra("input");

        AlertDialog.Builder alertdialog = new AlertDialog.Builder(ImageProcessingActivity.this);

        LinearLayout layout = new LinearLayout(ImageProcessingActivity.this);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        Button scan = new Button(ImageProcessingActivity.this);
        Button camera = new Button(ImageProcessingActivity.this);
        scan.setText("스캔 파일");
        camera.setText("촬영한 파일");
        layout.addView(scan);
        layout.addView(camera);
        layout.setGravity(CENTER);

        alertdialog.setView(layout);
        alertdialog.setTitle("파일 타입을 골라주세요");
        AlertDialog alert = alertdialog.create();
        alert.show();

        scan.setOnClickListener(view -> {
            fileCheck = 1;
            read_image_file();
            imageprocess_and_showResult();
            alert.cancel();
        });

        camera.setOnClickListener(view -> {
            fileCheck = 2;
            read_image_file();
            imageprocess_and_showResult();
            alert.cancel();
        });

        // 필요한 권한이 허가되어 있지 않다면 사용자에게 요청
        if (!hasPermissions(PERMISSIONS)) {
            requestNecessaryPermissions(PERMISSIONS);
        }

        btnRunOCR.setOnClickListener(v -> {
            pbLogins.setVisibility(View.VISIBLE);
            Intent intent1 =new Intent(ImageProcessingActivity.this,CloudActivity.class);
            startActivity(intent1);
            pbLogins.setVisibility(View.GONE);
        });
    }

    private void imageprocess_and_showResult() {
        imageprocessing(img_input.getNativeObjAddr(), img_output.getNativeObjAddr(),fileCheck);
        imageVIewInput.setImageURI(photoUri);
        Bitmap bitmapOutput = Bitmap.createBitmap(img_output.cols(), img_output.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(img_output, bitmapOutput);
        imageVIewOuput.setImageBitmap(bitmapOutput);

        SaveBitmapToFileCache(bitmapOutput, "/storage/emulated/0/SmaRed/", "s2.jpg");
    }

    private void read_image_file() {
        img_input = new Mat();
        img_output = new Mat();

        loadImage(ImagePath, img_input.getNativeObjAddr());
    }

    // NDK를 사용하는 네이티브 메서드 정의
    public native void loadImage(String imageFileName, long img);
    public native void imageprocessing(long inputImage, long outputImage, int fileCheck);
}
