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
import java.util.Hashtable;
import java.util.List;

import kr.ac.kpu.block.smared.databinding.ActivityCloudBinding;

public class CloudActivity extends Activity {
    private FormattedLogger logger = new FormattedLogger();
    private ActivityCloudBinding viewBinding;

    private final String CLOUD_VISION_API_KEY = "AIzaSyC6FyPlYCwLuwVhE8s3Td_zbbbwcMr41Oc";
    private final String ANDROID_CERT_HEADER = "X-Android-Cert";
    private final String ANDROID_PACKAGE_HEADER = "X-Android-Package";
    
    private final int MAX_LABEL_RESULTS = 10;
    private final int MAX_DIMENSION = 1200;

    private String stUseItem;
    private String stPrice;

    private Calendar calendar = Calendar.getInstance(); // Firebase내에 날짜로 저장
    private String stYear = new SimpleDateFormat("yyyy").format(calendar.getTime());
    private String stMonth = new SimpleDateFormat("MM").format(calendar.getTime());
    private String stDay = new SimpleDateFormat("dd").format(calendar.getTime());

    private ArrayAdapter<String> spinneradapter;
    private ArrayAdapter<String> spinneradapterMemo;
    private List<String> listItems = new ArrayList<>();
    private List<String> memoItems = new ArrayList<>();

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        listItems.clear();
        memoItems.clear();
        spinneradapter.notifyDataSetChanged();
        spinneradapterMemo.notifyDataSetChanged();

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

        spinneradapter = new ArrayAdapter<>(CloudActivity.this, R.layout.support_simple_spinner_dropdown_item, listItems);
        viewBinding.spnPrice.setAdapter(spinneradapter);

        spinneradapterMemo = new ArrayAdapter<>(CloudActivity.this, R.layout.support_simple_spinner_dropdown_item, memoItems);
        viewBinding.spnPaymemo.setAdapter(spinneradapterMemo);

        try {
            File imagePath = new File("/storage/emulated/0/SmaRed/s2.jpg");
            Bitmap bitmapImage = BitmapFactory.decodeFile(imagePath.getAbsolutePath());
            bitmapImage = scaleBitmapDown(bitmapImage, MAX_DIMENSION);

            AsyncTask<Object, Void, String> labelDetectionTask = new LableDetectionTask(this, prepareAnnotationRequest(bitmapImage));
            labelDetectionTask.execute();
        }
        catch (IOException e) {
        }
        catch (Exception e) {
        }

        viewBinding.spnUseitem.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                stUseItem = (String) adapterView.getItemAtPosition(i);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) { }
        });

        viewBinding.cvCalender.setOnDateChangeListener((calendarView, year, month, day) -> {
            stYear = Integer.toString(year);
            stMonth = Integer.toString(month+1);
            stDay = Integer.toString(day);
            Toast.makeText(CloudActivity.this, stYear + "-" + stMonth + "-" + stDay, Toast.LENGTH_SHORT).show();
        });

        viewBinding.spnPrice.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                stPrice = (String) parent.getItemAtPosition(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });

        viewBinding.spnPaymemo.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                viewBinding.etPaymemo.setText((String) parent.getItemAtPosition(position));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });

        viewBinding.btnSave.setOnClickListener(view -> {
            Hashtable<String, String> ledger = new Hashtable<>();
            ledger.put("useItem", stUseItem);
            ledger.put("price", stPrice);
            ledger.put("paymemo", viewBinding.etPaymemo.getText().toString());

            String stTime = new SimpleDateFormat("HHmmss").format(Calendar.getInstance().getTime());
            if (viewBinding.rbConsume.isChecked()) {
                myRef.child(user.getUid()).child("Ledger").child(stYear).child(stMonth).child(stDay).child("지출").child(stTime).setValue(ledger);
            } else {
                myRef.child(user.getUid()).child("Ledger").child(stYear).child(stMonth).child(stDay).child("수입").child(stTime).setValue(ledger);
            }

            Toast.makeText(CloudActivity.this, "저장하였습니다.", Toast.LENGTH_SHORT).show();
        });

        viewBinding.btnFinish.setOnClickListener(v -> {
            listItems.clear();
            memoItems.clear();
            spinneradapter.notifyDataSetChanged();
            spinneradapterMemo.notifyDataSetChanged();
            startActivity(new Intent(CloudActivity.this, TabActivity.class));
        });

        viewBinding.btnOCRResult.setOnClickListener(v -> {
            startActivity(new Intent(this, ContentActivity.class));
        });
    }

    private Bitmap scaleBitmapDown(Bitmap bitmap, int maxDimension) {
        int originalWidth = bitmap.getWidth();
        int originalHeight = bitmap.getHeight();
        int resizedWidth = maxDimension;
        int resizedHeight = maxDimension;

        if (originalHeight > originalWidth) {
            resizedHeight = maxDimension;
            resizedWidth = (int) (resizedHeight * (float) originalWidth / (float) originalHeight);
        } else if (originalWidth > originalHeight) {
            resizedHeight = (int) (resizedWidth * (float) originalHeight / (float) originalWidth);
            resizedWidth = maxDimension;
        } else if (originalHeight == originalWidth) {
            resizedHeight = maxDimension;
            resizedWidth = maxDimension;
        }

        return Bitmap.createScaledBitmap(bitmap, resizedWidth, resizedHeight, false);
    }

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

    private class LableDetectionTask extends AsyncTask<Object, Void, String> {
        private final WeakReference<CloudActivity> mActivityWeakReference;
        private Vision.Images.Annotate mRequest;

        LableDetectionTask(CloudActivity activity, Vision.Images.Annotate annotate) {
            mActivityWeakReference = new WeakReference<>(activity);
            mRequest = annotate;
        }

        @Override
        protected String doInBackground(Object... params) {
            try {
                BatchAnnotateImagesResponse response = mRequest.execute();

                List<EntityAnnotation> labels = response.getResponses().get(0).getTextAnnotations();
                return (labels == null) ? "nothing" : labels.get(0).getDescription();
            }
            catch (GoogleJsonResponseException e) {
            }
            catch (IOException e) {
            }

            return "Cloud Vision API request failed. Check logs for details.";
        }

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
            AlertDialog alert = alertDialog.create();
            alert.show();

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
