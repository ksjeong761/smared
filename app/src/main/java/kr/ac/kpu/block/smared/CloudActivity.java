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
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import kr.ac.kpu.block.smared.databinding.ActivityCloudBinding;

public class CloudActivity extends Activity {
    private static final String CLOUD_VISION_API_KEY = "AIzaSyC6FyPlYCwLuwVhE8s3Td_zbbbwcMr41Oc";
    private static final String ANDROID_CERT_HEADER = "X-Android-Cert";
    private static final String ANDROID_PACKAGE_HEADER = "X-Android-Package";
    
    private static final int MAX_LABEL_RESULTS = 10;
    private static final int MAX_DIMENSION = 1200;

    private ActivityCloudBinding viewBinding;

    FirebaseDatabase database;
    DatabaseReference myRef;
    FirebaseUser user;

    String stUseItem;
    String stPrice;

    Calendar calendar = Calendar.getInstance(); // Firebase내에 날짜로 저장
    String stYear = new SimpleDateFormat("yyyy").format(calendar.getTime());
    String stMonth = new SimpleDateFormat("MM").format(calendar.getTime());
    String stDay = new SimpleDateFormat("dd").format(calendar.getTime());

    ArrayAdapter<String> spinneradapter;
    ArrayAdapter<String> spinneradapterMemo;
    List<String> listItems = new ArrayList<>();
    List<String> memoItems = new ArrayList<>();

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

        database = FirebaseDatabase.getInstance();
        myRef = database.getReference("users");
        user = FirebaseAuth.getInstance().getCurrentUser();

        // [Refactor] oncreate 에서 바로 쓰레드돌릴려고 임시방편으로 넣어둔소스
        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }

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
            Toast.makeText(CloudActivity.this, stYear+"-"+stMonth+"-"+stDay, Toast.LENGTH_SHORT).show();
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

    public String extractPaymemo(String str) {
        String result = "";
        String payMemoToast = "";
        StringTokenizer stringTokenizer = new StringTokenizer(str,"\n");
        int flag = 0;

        // 금액부터 내용 추출
        while (stringTokenizer.hasMoreTokens()){
            result = stringTokenizer.nextToken();
            if (result.contains("금액") && flag == 0) {
                flag = 1;
            }

            if (flag == 1) {
                if (result.contains("공급") || result.contains("면세") || result.contains("과세") || result.contains("주문")) {
                    break;
                }

                if (!result.contains("금액") && !memoItems.contains(result)) {
                    memoItems.add(result);
                }
            }

            spinneradapterMemo.notifyDataSetChanged();

            if (result.isEmpty()) {
                payMemoToast = "내용 미 검출";
            }
        }

        return payMemoToast;
    }

    public String extractDate(String str) {
        List<String> list = new ArrayList<>();
        String dateResult = "";
        String dateToast = "";
        Matcher matcher;

        if (!str.isEmpty()) {
            String patternStr = "(19|20)\\d{2}[-/.년]*([1-9]|0[1-9]|1[012])[-/.월]*(0[1-9]|[12][0-9]|3[01])"; // 날짜를 패턴으로 지정

            int flags = Pattern.MULTILINE | Pattern.CASE_INSENSITIVE;
            Pattern pattern = Pattern.compile(patternStr, flags);
            matcher = pattern.matcher(str);

            while (matcher.find()) {
                list.add(matcher.group());
            }
        }

        for(int i=0; i<list.size(); i++) {
            dateResult += list.get(i);
        }

        if (dateResult.isEmpty()) {
            dateToast = "날짜 데이터 미 검출\n";
            return dateToast;
        }

        dateResult = dateResult.replaceAll("[년월일/.]","-");

        String parts[] = dateResult.split("-");

        int year = Integer.parseInt(parts[0]);
        int month = Integer.parseInt(parts[1]);
        int day = Integer.parseInt(parts[2]);

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, month-1);
        calendar.set(Calendar.DAY_OF_MONTH, day);

        long milliTime = calendar.getTimeInMillis();
        viewBinding.cvCalender.setDate(milliTime, true, true);

        return dateToast;
    }

    public String extract(String str,CloudActivity activity) {
        String result="";
        String payMemoToast = "";

        int flag = 0;
        List<String> koreanitems = new ArrayList<>();
        List<String> numberitems = new ArrayList<>();

        StringTokenizer stringTokenizer = new StringTokenizer(str,"\n");
        while (stringTokenizer.hasMoreTokens()) {
            String temp = stringTokenizer.nextToken();
            if (flag == 1) {
                if (temp.contains("공급") || temp.contains("면세") || temp.contains("과세") || temp.contains("주문")) {
                    flag = 2;
                } else if (temp.matches(".*[ㄱ-ㅎㅏ-ㅣ가-힣]+.*")) {
                    String korean = temp.replaceAll("[^[ㄱ-ㅎㅏ-ㅣ가-힣]\\n]", "");
                    if (!koreanitems.contains(korean)) {
                        koreanitems.add(korean);
                    }
                }
                else {
                    String number = temp.replaceAll("[^0-9\\.\\,\\n\\s]", "");
                    StringTokenizer spaceTokenizer = new StringTokenizer(number, " ");
                    while (spaceTokenizer.hasMoreTokens()) {
                        String spaceToken = spaceTokenizer.nextToken();
                        char last = spaceToken.charAt(spaceToken.length() - 1);
                        if (last != '0')  continue;

                        spaceToken = spaceToken.replace(",", "");
                        spaceToken = spaceToken.replace(".", "");

                        if (!numberitems.contains(spaceToken)) {
                            numberitems.add(spaceToken);
                        }
                    }
                }
            }

            if (temp.contains("금액") && flag == 0) {
                flag = 1;
            }

            if (result.isEmpty()) {
                payMemoToast = "내용 미 검출";
            }
        }

        for(int i=0; i<koreanitems.size(); i++) {
            result += koreanitems.get(i);
            result += "\n";
        }

        for(int i=0; i<numberitems.size(); i++) {
            result += numberitems.get(i);
            result += "\n";
        }

        AlertDialog.Builder alertDialog = new AlertDialog.Builder(activity);
        alertDialog.setTitle("자동 인식 결과");
        alertDialog.setMessage(result);

        alertDialog.setPositiveButton("확인", (dialog, which) -> {
            numberitems.clear();
            koreanitems.clear();
        });

        alertDialog.setNegativeButton("취소", (dialog, which) -> {
            numberitems.clear();
            koreanitems.clear();
        });

        AlertDialog alert = alertDialog.create();
        alert.show();

        return payMemoToast;
    }

    public String extractPrice(String str) {
        StringTokenizer linefeedTokenizer = new StringTokenizer(str,"\n");

        while (linefeedTokenizer.hasMoreTokens()){
            String linefeedToken = linefeedTokenizer.nextToken();
            if (!linefeedToken.contains("0")) continue;
            if (!linefeedToken.contains(",") && !linefeedToken.contains(".")) continue;

            StringTokenizer spaceTokenizer = new StringTokenizer(linefeedToken, " ");
            while (spaceTokenizer.hasMoreTokens()){
                String spaceToken = spaceTokenizer.nextToken();
                if (spaceToken.charAt(spaceToken.length() - 1) != '0') continue;
                if (!spaceToken.contains(",") && !spaceToken.contains(".")) continue;

                spaceToken = spaceToken.replace(",", "");
                spaceToken = spaceToken.replace(".", "");

                if (!listItems.contains(spaceToken)) {
                    listItems.add(spaceToken);
                }

                spinneradapter.notifyDataSetChanged();
            }
        }

        return listItems.size() == 0 ? "금액 미 검출\n" : "";
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
        protected void onPostExecute(String result) {
            CloudActivity activity = mActivityWeakReference.get();

            if (activity == null) return;
            if (!activity.isFinishing()) return;

            String price = result.replaceAll("[^0-9\\.\\,\\n\\s]","");
            String date = result.replaceAll("[^0-9\\.\\,\\-\\n\\/년월일]","");
            String payMemo = result.replaceAll("[^[ㄱ-ㅎㅏ-ㅣ가-힣]\\n]","");

            extract(result, activity);
            String finalResult = "[ 분석 결과 ]\n" + extractDate(date) + extractPrice(price) + extractPaymemo(payMemo);
        }
    }
}
