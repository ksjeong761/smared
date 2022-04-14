package kr.ac.kpu.block.smared;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.view.View;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import kr.ac.kpu.block.smared.databinding.ActivityImageBinding;

public class ImageActivity extends Activity {

    private ActivityImageBinding viewBinding;

    private static final int PICK_FROM_CAMERA = 1;
    private static final int PICK_FROM_ALBUM = 2;
    private static final int CROP_FROM_CAMERA = 3;

    private static final int MULTIPLE_PERMISSIONS = 101;
    private String[] permissions = {
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.CAMERA
    };

    private Uri photoUri;
    private String imagePath = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewBinding = ActivityImageBinding.inflate(getLayoutInflater());
        setContentView(viewBinding.getRoot());

        // 필요한 권한이 있는지 확인
        List<String> notGrantedPermissions = new ArrayList<>();
        for (String permission : permissions) {
            if (PackageManager.PERMISSION_GRANTED != ContextCompat.checkSelfPermission(this, permission)) {
                notGrantedPermissions.add(permission);
            }
        }

        // 필요한 권한이 없다면 요청한다.
        if (!notGrantedPermissions.isEmpty()) {
            ActivityCompat.requestPermissions(this, notGrantedPermissions.toArray(new String[notGrantedPermissions.size()]), MULTIPLE_PERMISSIONS);
        }

        // 카메라 버튼
        viewBinding.btnCamera.setOnClickListener(view -> {
            try {
                File photoFile = createImageFile();
                photoUri = FileProvider.getUriForFile(ImageActivity.this, "kr.ac.kpu.block.smared.provider", photoFile);

                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                startActivityForResult(intent, PICK_FROM_CAMERA);
            }
            catch (IOException e) {
                Toast.makeText(ImageActivity.this, "이미지 처리 오류! 다시 시도해주세요.", Toast.LENGTH_SHORT).show();
                finish();
            }
        });

        viewBinding.btnAlbum.setOnClickListener(view -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType(MediaStore.Images.Media.CONTENT_TYPE);
            startActivityForResult(intent, PICK_FROM_ALBUM);
        });

        viewBinding.btnNext.setOnClickListener(view -> {
            if (imagePath.isEmpty()) {
                return;
            }

            Intent intent = new Intent(ImageActivity.this, ImageProcessingActivity.class);
            intent.putExtra("ipath", imagePath);
            intent.putExtra("input", photoUri);
            startActivity(intent);
        });

        viewBinding.btnCancel.setOnClickListener(view -> {
            finish();
        });
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("HHmmss").format(new Date());
        String imageFileName = "smared_" + timeStamp + "_";
        File storageDir = new File(Environment.getExternalStorageDirectory() + "/SmaRed/");
        if (!storageDir.exists()) {
            storageDir.mkdirs();
        }

        File image = File.createTempFile(imageFileName, ".jpg", storageDir);
        imagePath = image.getName();
        return image;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MULTIPLE_PERMISSIONS: {
                if (grantResults.length == 0) {
                    Toast.makeText(this, "권한 요청에 동의 해주셔야 이용 가능합니다. 설정에서 권한을 허용해주시기 바랍니다.", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }

                for (int i = 0; i < permissions.length; i++) {
                    for (int j = 0; j < this.permissions.length; j++) {
                        if (!permissions[i].equals(this.permissions[j])) {
                            continue;
                        }

                        if (PackageManager.PERMISSION_GRANTED != grantResults[i]) {
                            Toast.makeText(this, "권한 요청에 동의 해주셔야 이용 가능합니다. 설정에서 권한을 허용해주시기 바랍니다.", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    }
                }

                return;
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_OK) {
            Toast.makeText(this, "취소되었습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        switch (requestCode) {
            case PICK_FROM_ALBUM:
                if (data == null) {
                    return;
                }

                photoUri = data.getData();
                cropImage();
                break;

            case PICK_FROM_CAMERA:
                cropImage();
                // 갤러리에 나타나게
                MediaScannerConnection.scanFile(ImageActivity.this, new String[]{photoUri.getPath()}, null, (path, uri) -> { });
                break;
            case CROP_FROM_CAMERA:
                viewBinding.imageInput.setImageURI(null);
                viewBinding.imageInput.setImageURI(photoUri);
                break;
        }
    }

    public void cropImage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            this.grantUriPermission("camera", photoUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }

        Intent intent = new Intent("com.android.camera.action.CROP");
        intent.setDataAndType(photoUri, "image/*");

        List<ResolveInfo> list = getPackageManager().queryIntentActivities(intent, 0);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            this.grantUriPermission(list.get(0).activityInfo.packageName, photoUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }

        if (list.size() == 0) {
            Toast.makeText(this, "취소 되었습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "용량이 큰 사진의 경우 시간이 오래 걸릴 수 있습니다.", Toast.LENGTH_SHORT).show();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        }

        intent.putExtra("crop", "true");
        intent.putExtra("scale", true);

        File croppedFileName = null;
        try {
            croppedFileName = createImageFile();
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        File folder = new File(Environment.getExternalStorageDirectory() + "/SmaRed/");
        File tempFile = new File(folder.toString(), croppedFileName.getName());

        photoUri = FileProvider.getUriForFile(ImageActivity.this, "kr.ac.kpu.block.smared.provider", tempFile);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        }

        intent.putExtra("return-data", false);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
        intent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString());

        Intent i = new Intent(intent);
        ResolveInfo res = list.get(0);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            i.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

            this.grantUriPermission(res.activityInfo.packageName, photoUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }

        i.setComponent(new ComponentName(res.activityInfo.packageName, res.activityInfo.name));
        startActivityForResult(i, CROP_FROM_CAMERA);
    }
}