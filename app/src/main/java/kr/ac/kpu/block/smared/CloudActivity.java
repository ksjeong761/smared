// https://github.com/GoogleCloudPlatform/cloud-vision/blob/master/android/CloudVision/app/src/main/java/com/google/sample/cloudvision/MainActivity.java
/*
 * Copyright 2016 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kr.ac.kpu.block.smared;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.StrictMode;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.vision.v1.Vision;
import com.google.api.services.vision.v1.VisionRequest;
import com.google.api.services.vision.v1.VisionRequestInitializer;
import com.google.api.services.vision.v1.model.AnnotateImageRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesResponse;
import com.google.api.services.vision.v1.model.EntityAnnotation;
import com.google.api.services.vision.v1.model.Feature;
import com.google.api.services.vision.v1.model.Image;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import kr.ac.kpu.block.smared.databinding.ActivityCloudBinding;

public class CloudActivity extends Activity {
    private FormattedLogger logger = new FormattedLogger();
    private ActivityCloudBinding viewBinding;

    private final String CLOUD_VISION_API_KEY = "AIzaSyC6FyPlYCwLuwVhE8s3Td_zbbbwcMr41Oc";
    private final String ANDROID_CERT_HEADER = "X-Android-Cert";
    private final String ANDROID_PACKAGE_HEADER = "X-Android-Package";
    
    private final int MAX_LABEL_RESULTS = 10;
    private final int MAX_RESOLUTION = 1200;

    private String category;
    private String price;

    private Calendar calendar = Calendar.getInstance();
    private String year = new SimpleDateFormat("yyyy").format(calendar.getTime());
    private String month = new SimpleDateFormat("MM").format(calendar.getTime());
    private String day = new SimpleDateFormat("dd").format(calendar.getTime());

    private ArrayAdapter<String> spinnerAdapter;
    private ArrayAdapter<String> spinnerAdapterMemo;
    private List<String> listItems = new ArrayList<>();
    private List<String> memoItems = new ArrayList<>();

    // 뒤로가기 입력 시 이전 액티비티로 이동
    @Override
    public void onBackPressed() {
        super.onBackPressed();

        listItems.clear();
        memoItems.clear();
        spinnerAdapter.notifyDataSetChanged();
        spinnerAdapterMemo.notifyDataSetChanged();

        startActivity(new Intent(CloudActivity.this, TabActivity.class));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewBinding = ActivityCloudBinding.inflate(getLayoutInflater());
        setContentView(viewBinding.getRoot());

        DatabaseReference myRef = FirebaseDatabase.getInstance().getReference("users");
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        // 파싱한 OCR 결과물을 스피너에 출력하는 것으로 사용자가 직접 올바른 데이터를 선택할 수 있도록 한다.
        spinnerAdapter = new ArrayAdapter<>(CloudActivity.this, R.layout.support_simple_spinner_dropdown_item, listItems);
        viewBinding.spnPrice.setAdapter(spinnerAdapter);

        spinnerAdapterMemo = new ArrayAdapter<>(CloudActivity.this, R.layout.support_simple_spinner_dropdown_item, memoItems);
        viewBinding.spnDescription.setAdapter(spinnerAdapterMemo);

        try {
            File imagePath = new File("/storage/emulated/0/SmaRed/s2.jpg");
            Bitmap bitmapImage = BitmapFactory.decodeFile(imagePath.getAbsolutePath());
            bitmapImage = scaleDownBitmap(bitmapImage, MAX_RESOLUTION);

            AsyncTask<Object, Void, String> labelDetectionTask = new LableDetectionTask(this, prepareAnnotationRequest(bitmapImage));
            labelDetectionTask.execute();
        } catch (IOException e) {
            logger.writeLog("failed to make API request because of other IOException " + e.getMessage());
        } catch (Exception e) {
            logger.writeLog("failed to make API request because of other Exception " + e.getMessage());
        }

        // 날짜 선택 이벤트 - 선택된 날짜를 클래스 내부에 기록
        viewBinding.cvCalender.setOnDateChangeListener((calendarView, year, month, day) -> {
            this.year = Integer.toString(year);
            this.month = Integer.toString(month+1);
            this.day = Integer.toString(day);
            Toast.makeText(CloudActivity.this, this.year + "-" + this.month + "-" + this.day, Toast.LENGTH_SHORT).show();
        });

        // 스피너 선택 이벤트 - 선택된 분류를 클래스 내부에 기록
        viewBinding.spnCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                category = (String) adapterView.getItemAtPosition(i);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) { }
        });

        // 스피너 선택 이벤트 - 선택된 가격을 클래스 내부에 기록
        viewBinding.spnPrice.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                price = (String) parent.getItemAtPosition(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });

        // 스피너 선택 이벤트 - 선택된 내용을 클래스 내부에 기록
        viewBinding.spnDescription.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                viewBinding.etDescription.setText((String) parent.getItemAtPosition(position));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });

        // 저장 버튼 - 파싱한 OCR 결과물을 가계부 DB에 저장
        viewBinding.btnSave.setOnClickListener(view -> {
            String description = viewBinding.etDescription.getText().toString();
            Map<String, String> ledger = new LedgerContent(category, price, description).toHashMap();
            String now = new SimpleDateFormat("HHmmss").format(Calendar.getInstance().getTime());
            String incomeOrExpenditure = viewBinding.rbConsume.isChecked() ? "지출" : "수입";
            myRef.child(user.getUid()).child("Ledger").child(year).child(month).child(day).child(incomeOrExpenditure).child(now).setValue(ledger);

            Toast.makeText(CloudActivity.this, "저장하였습니다.", Toast.LENGTH_SHORT).show();
        });

        // 끝내기 버튼 - 데이터를 비우고 TabActivity로 이동
        viewBinding.btnFinish.setOnClickListener(view -> {
            listItems.clear();
            memoItems.clear();
            spinnerAdapter.notifyDataSetChanged();
            spinnerAdapterMemo.notifyDataSetChanged();
            startActivity(new Intent(CloudActivity.this, TabActivity.class));
        });

        // OCR 결과 버튼 - ContentActivity로 이동
        viewBinding.btnOCRResult.setOnClickListener(view -> startActivity(new Intent(this, OCRResultActivity.class)));
    }

    // 기준 해상도에 맞게 이미지를 축소한다.
    private Bitmap scaleDownBitmap(Bitmap bitmap, int maxResolution) {
        int originalWidth = bitmap.getWidth();
        int originalHeight = bitmap.getHeight();
        int resizedWidth = (originalWidth >= originalHeight) ? maxResolution : (int)((float)originalWidth / (float)originalHeight * maxResolution);
        int resizedHeight = (originalHeight >= originalWidth) ? maxResolution : (int)((float)originalHeight / (float)originalWidth * maxResolution);

        return Bitmap.createScaledBitmap(bitmap, resizedWidth, resizedHeight, false);
    }

    // https://cloud.google.com/vision/docs/batch
    // Cloud Vision 텍스트 감지 API 호출 준비
    // 요청 시 필요한 정보 : 패키지명, SHA1 서명, Cloud Vision 키, Base64 인코딩 된 JPEG 이미지
    // 응답 내용 : 감지된 텍스트, 이미지 내 텍스트 좌표 등 정보가 포함된 JSON
    private Vision.Images.Annotate prepareAnnotationRequest(Bitmap bitmap) throws IOException {
        HttpTransport httpTransport = AndroidHttp.newCompatibleTransport();
        JsonFactory jsonFactory = GsonFactory.getDefaultInstance();

        VisionRequestInitializer requestInitializer = new VisionRequestInitializer(CLOUD_VISION_API_KEY) {
            /**
             * We override this so we can inject important identifying fields into the HTTP
             * headers. This enables use of a restricted cloud platform API key.
             */
            @Override
            protected void initializeVisionRequest(VisionRequest<?> visionRequest) throws IOException {
                super.initializeVisionRequest(visionRequest);

                String packageName = getPackageName();
                visionRequest.getRequestHeaders().set(ANDROID_PACKAGE_HEADER, packageName);
                String sig = PackageManagerUtils.getSignature(getPackageManager(), packageName);
                visionRequest.getRequestHeaders().set(ANDROID_CERT_HEADER, sig);
            }
        };

        Vision.Builder builder = new Vision.Builder(httpTransport, jsonFactory, null);
        builder.setVisionRequestInitializer(requestInitializer);
        Vision vision = builder.build();

        BatchAnnotateImagesRequest batchAnnotateImagesRequest = new BatchAnnotateImagesRequest();
        batchAnnotateImagesRequest.setRequests(new ArrayList<AnnotateImageRequest>() {{
            AnnotateImageRequest annotateImageRequest = new AnnotateImageRequest();

            // Add the image
            Image base64EncodedImage = new Image();

            // Convert the bitmap to a JPEG
            // Just in case it's a format that Android understands but Cloud Vision
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, byteArrayOutputStream);
            byte[] imageBytes = byteArrayOutputStream.toByteArray();

            // Base64 encode the JPEG
            base64EncodedImage.encodeContent(imageBytes);
            annotateImageRequest.setImage(base64EncodedImage);

            // add the features we want
            annotateImageRequest.setFeatures(new ArrayList<Feature>() {{
                Feature textDetection = new Feature();
                textDetection.setType("TEXT_DETECTION");
                textDetection.setMaxResults(MAX_LABEL_RESULTS);
                add(textDetection);
            }});

            // Add the list of one thing to the request
            add(annotateImageRequest);
        }});

        Vision.Images.Annotate annotateRequest = vision.images().annotate(batchAnnotateImagesRequest);
        // Due to a bug: requests to Vision API containing large images fail when GZipped.
        annotateRequest.setDisableGZipContent(true);

        return annotateRequest;
    }

    // https://developer.android.com/reference/android/os/AsyncTask
    // 비동기 API 호출을 위한 내부 클래스 (상속 받은 추상 클래스 AsyncTask는 API 30부터 Deprecated 처리됨)
    private class LableDetectionTask extends AsyncTask<Object, Void, String> {
        private final WeakReference<CloudActivity> mActivityWeakReference;
        private Vision.Images.Annotate mRequest;

        LableDetectionTask(CloudActivity activity, Vision.Images.Annotate annotate) {
            mActivityWeakReference = new WeakReference<>(activity);
            mRequest = annotate;
        }

        // Cloud Vision API 요청
        @Override
        protected String doInBackground(Object... params) {
            try {
                logger.writeLog( "created Cloud Vision request object, sending request");
                BatchAnnotateImagesResponse response = mRequest.execute();

                List<EntityAnnotation> labels = response.getResponses().get(0).getTextAnnotations();
                return (labels == null) ? "nothing" : labels.get(0).getDescription();
            } catch (GoogleJsonResponseException e) {
                logger.writeLog("failed to make API request because " + e.getContent());
            } catch (IOException e) {
                logger.writeLog("failed to make API request because of other IOException " +  e.getMessage());
            }

            return "Cloud Vision API request failed. Check logs for details.";
        }

        // OCR 인식 결과물 파싱
        @Override
        protected void onPostExecute(String ocrResultText) {
            // 다이얼로그 호출 전 액티비티 참조 얻기
            CloudActivity activity = mActivityWeakReference.get();
            if (activity == null) return;
            if (!activity.isFinishing()) return;

            // OCR 인식 결과가 있는지 확인
            if (ocrResultText == null) return;
            if (ocrResultText.length() == 0) return;

            // OCR 인식 결과물 파싱
            ReceiptStringParser receiptParser = new ReceiptStringParser();
            List<String> texts = receiptParser.extractKoreanText(ocrResultText);
            List<String> prices = receiptParser.extractPrice(ocrResultText);
            StringBuilder showingResult = new StringBuilder();
            for (String text : texts) {
                showingResult.append(text);
                showingResult.append("\n");
            }
            for (String price : prices) {
                showingResult.append(price);
                showingResult.append("\n");
            }

            // 결과 보여주기
            AlertDialog.Builder alertDialog = new AlertDialog.Builder(activity);
            alertDialog.setTitle("자동 인식 결과");
            alertDialog.setMessage(showingResult);
            alertDialog.setPositiveButton("확인", (dialog, which) -> { });
            alertDialog.setNegativeButton("취소", (dialog, which) -> { });
            alertDialog.create().show();

            // 결과 보여주기 임시
            // String finalResult = "[ 분석 결과 ]\n"
            // + receiptParser.extractDate(ocrResultText)
            // + receiptParser.extractPrice(ocrResultText)
            // + receiptParser.extractProductInfoRow(ocrResultText);
            //
            //String parts[] = dateResult.split("-");
            //Calendar calendar = Calendar.getInstance();
            //calendar.set(Calendar.YEAR, Integer.parseInt(parts[0]));
            //calendar.set(Calendar.MONTH, Integer.parseInt(parts[1])-1);
            //calendar.set(Calendar.DAY_OF_MONTH, Integer.parseInt(parts[2]));
            //viewBinding.cvCalender.setDate(calendar.getTimeInMillis(), true, true);
            //spinneradapterMemo.notifyDataSetChanged();
        }
    }
}
