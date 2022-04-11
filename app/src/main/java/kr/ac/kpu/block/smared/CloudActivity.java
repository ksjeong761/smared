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
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.Spinner;
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

public class CloudActivity extends Activity {
    private static final String CLOUD_VISION_API_KEY = "AIzaSyC6FyPlYCwLuwVhE8s3Td_zbbbwcMr41Oc";
    private static final String ANDROID_CERT_HEADER = "X-Android-Cert";
    private static final String ANDROID_PACKAGE_HEADER = "X-Android-Package";
    
    private static final int MAX_LABEL_RESULTS = 10;
    private static final int MAX_DIMENSION = 1200;

    private static final String TAG = CloudActivity.class.getSimpleName();

    Bitmap image; //사용되는 이미지
    File imgFile = new File("/storage/emulated/0/SmaRed/s2.jpg");

    FirebaseDatabase database;
    DatabaseReference myRef;
    FirebaseUser user;
    Context context;

    String stUseItem;
    String stPrice;
    String stPaymemo;

    Calendar c = Calendar.getInstance(); // Firebase내에 날짜로 저장
    SimpleDateFormat year = new SimpleDateFormat("yyyy");
    SimpleDateFormat month = new SimpleDateFormat("MM");
    SimpleDateFormat day = new SimpleDateFormat("dd");
    String stYear = year.format(c.getTime());
    String stMonth = month.format(c.getTime());
    String stDay = day.format(c.getTime());

    static EditText etPaymemo ;
    static CalendarView cvCalender;
    static ArrayAdapter<String> spinneradapter;
    static ArrayAdapter<String> spinneradapterMemo;
    static Spinner spnPrice;
    static Spinner spnPaymemo;
    static List<String> listItems = new ArrayList<String>();
    static List<String> memoItems = new ArrayList<String>();

    static List<String> koreanitems = new ArrayList<String>();
    static List<String> numberitems = new ArrayList<String>();

    Button btnFinish;
    Button btnOCRResult;

    static Intent ins;
    Button button;
    private ImageView mMainImage;

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        listItems.clear();
        memoItems.clear();
        spinneradapter.notifyDataSetChanged();
        spinneradapterMemo.notifyDataSetChanged();
        Intent in = new Intent(CloudActivity.this, TabActivity.class);
        startActivity(in);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cloud);

        database = FirebaseDatabase.getInstance();
        myRef = database.getReference("users");
        user = FirebaseAuth.getInstance().getCurrentUser();

        etPaymemo = findViewById(R.id.etPaymemo);
        cvCalender = findViewById(R.id.cvCalender);
        final Spinner spnUseitem = findViewById(R.id.spnUseitem);

        Button btnSave = findViewById(R.id.btnSave);
        final RadioButton rbConsume = findViewById(R.id.rbConsume);
        spnPrice = findViewById(R.id.spnPrice);
        spnPaymemo = findViewById(R.id.spnPaymemo);
        btnFinish = findViewById(R.id.btnFinish);
        btnOCRResult = findViewById(R.id.btnOCRResult);
        ins = new Intent(this,ContentActivity.class);

        // [Refactor] oncreate 에서 바로 쓰레드돌릴려고 임시방편으로 넣어둔소스
        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }

        spnUseitem.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                stUseItem = (String) adapterView.getItemAtPosition(i);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) { }
        });

        cvCalender.setOnDateChangeListener((calendarView, year, month, day) -> {
            stYear = Integer.toString(year);
            stMonth = Integer.toString(month+1);
            stDay = Integer.toString(day);
            Toast.makeText(CloudActivity.this, stYear+"-"+stMonth+"-"+stDay, Toast.LENGTH_SHORT).show();
        });

        spnPrice.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                stPrice = (String) parent.getItemAtPosition(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });

        spnPaymemo.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                etPaymemo.setText((String) parent.getItemAtPosition(position));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });

        btnSave.setOnClickListener(view -> {
            stPaymemo = etPaymemo.getText().toString();
            c = Calendar.getInstance();
            SimpleDateFormat time = new SimpleDateFormat("HHmmss");
            String stTime = time.format(c.getTime());
            Hashtable<String, String> ledger = new Hashtable<>();
            ledger.put("useItem", stUseItem);
            ledger.put("price", stPrice);
            ledger.put("paymemo",stPaymemo);

            if (rbConsume.isChecked()) {
                myRef.child(user.getUid()).child("Ledger").child(stYear).child(stMonth).child(stDay).child("지출").child(stTime).setValue(ledger);
            } else {
                myRef.child(user.getUid()).child("Ledger").child(stYear).child(stMonth).child(stDay).child("수입").child(stTime).setValue(ledger);
            }

            Toast.makeText(CloudActivity.this, "저장하였습니다.", Toast.LENGTH_SHORT).show();
        });

        if (imgFile.exists()) {
            image = BitmapFactory.decodeFile(imgFile.getAbsolutePath()); //샘플이미지파일
        }

        btnFinish.setOnClickListener(v -> {
            listItems.clear();
            memoItems.clear();
            spinneradapter.notifyDataSetChanged();
            spinneradapterMemo.notifyDataSetChanged();
            Intent in = new Intent(CloudActivity.this, TabActivity.class);
            startActivity(in);
        });

        btnOCRResult.setOnClickListener(v -> startActivity(ins));
        spinneradapter = new ArrayAdapter<>(CloudActivity.this, R.layout.support_simple_spinner_dropdown_item, listItems);
        spnPrice.setAdapter(spinneradapter);
        spinneradapterMemo = new ArrayAdapter<>(CloudActivity.this, R.layout.support_simple_spinner_dropdown_item, memoItems);
        spnPaymemo.setAdapter(spinneradapterMemo);

        uploadImage(image);
    }

    public void uploadImage(Bitmap image) {
        if (image == null) {
            Log.d(TAG, "Image picker gave us a null image.");
            return;
        }

        try {
            // scale the image to save on bandwidth
            Bitmap bitmap = scaleBitmapDown(image, MAX_DIMENSION);
            callCloudVision(bitmap);
            mMainImage.setImageBitmap(bitmap);
        } catch (Exception e) {
            Log.d(TAG, "Image picking failed because " + e.getMessage());
        }
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
        Log.d(TAG, "created Cloud Vision request object, sending request");

        return annotateRequest;
    }

    private static class LableDetectionTask extends AsyncTask<Object, Void, String> {
        private final WeakReference<CloudActivity> mActivityWeakReference;
        private Vision.Images.Annotate mRequest;

        LableDetectionTask(CloudActivity activity, Vision.Images.Annotate annotate) {
            mActivityWeakReference = new WeakReference<>(activity);
            mRequest = annotate;
        }

        @Override
        protected String doInBackground(Object... params) {
            try {
                Log.d(TAG, "created Cloud Vision request object, sending request");
                BatchAnnotateImagesResponse response = mRequest.execute();
                return convertResponseToString(response);
            } catch (GoogleJsonResponseException e) {
                Log.d(TAG, "failed to make API request because " + e.getContent());
            } catch (IOException e) {
                Log.d(TAG, "failed to make API request because of other IOException " + e.getMessage());
            }

            return "Cloud Vision API request failed. Check logs for details.";
        }

        protected void onPostExecute(String result) {
            CloudActivity activity = mActivityWeakReference.get();

            if (activity == null) return;
            if (!activity.isFinishing()) return;

            String payMemo;
            String payMemoResult="";
            String price;
            String priceResult="";
            String date;
            String dateResult= "";
            String finalResult = "";

            payMemo = result.replaceAll("[^[ㄱ-ㅎㅏ-ㅣ가-힣]\\n]","");
            price = result.replaceAll("[^0-9\\.\\,\\n\\s]","");
            date = result.replaceAll("[^0-9\\.\\,\\-\\n\\/년월일]","");
            dateResult += extractDate(date);
            payMemoResult += extractPaymemo(payMemo);
            extract(result,activity);
            priceResult += extractPrice(price);

            finalResult = "[ 분석 결과 ]\n" + dateResult + priceResult + payMemoResult;
            ins.putExtra("result",result);
            ins.putExtra("finalResult",finalResult);
        }
    }

    private void callCloudVision(final Bitmap bitmap) {
        // Switch text to loading
        // Do the real work in an async task, because we need to use the network anyway
        try {
            AsyncTask<Object, Void, String> labelDetectionTask = new LableDetectionTask(this, prepareAnnotationRequest(bitmap));
            labelDetectionTask.execute();
        } catch (IOException e) {
            Log.d(TAG, "failed to make API request because of other IOException " + e.getMessage());
        }
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

    private static String convertResponseToString(BatchAnnotateImagesResponse response) {
        String message =("");

        List<EntityAnnotation> labels = response.getResponses().get(0).getTextAnnotations();
        if (labels != null) {
            message += labels.get(0).getDescription();
        } else {
            message += "nothing";
        }

        return message;
    }

    public static String extractPaymemo(String str) {
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

            if (result.equals("")) {
                payMemoToast = "내용 미 검출";
            }
        }

        return payMemoToast;
    }

    public static String extractDate(String str) {
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

        if (dateResult.equals("")) {
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
        cvCalender.setDate(milliTime, true, true);

        return dateToast;
    }

    public static String extract(String str,CloudActivity activity) {
        String result="";
        String payMemoToast = "";

        int flag = 0;

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

            if (result.equals("")) {
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

    public static String extractPrice(String str) {
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
}
