package kr.ac.kpu.block.smared;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import kr.ac.kpu.block.smared.databinding.FragmentLedgerViewBinding;

public class LedgerViewFragment extends android.app.Fragment {
    private FormattedLogger logger = new FormattedLogger();
    private FragmentLedgerViewBinding viewBinding;
    private String TAG = getClass().getSimpleName();

    private LedgerAdapter mAdapter;
    private List<Ledger> mLedger = new ArrayList<>(); // 불러온 전체 가계부 목록

    private int index = 0;  // 년,월 인덱스
    private Set<String> selectMonth = new HashSet<>(); // 년,월 중복제거용
    private List<String> monthList; // 중복 제거된 년,월 저장

    private static final int MULTIPLE_PERMISSIONS = 101;
    private String[] permissions = {
        android.Manifest.permission.READ_EXTERNAL_STORAGE,
        android.Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        viewBinding = FragmentLedgerViewBinding.inflate(inflater, container, false);

        //사용자 정보 DB에 접근하기 위한 객체
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference("users");

        //현재 로그인한 사용자 가져오기
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        checkPermissions();

        viewBinding.rvLedger.setHasFixedSize(true);
        viewBinding.rvLedger.setLayoutManager(new LinearLayoutManager(getActivity()));
        mAdapter = new LedgerAdapter(mLedger, getActivity(), "");
        viewBinding.rvLedger.setAdapter(mAdapter);
        viewBinding.rvLedger.scrollToPosition(0);

        // 이전 달 버튼 이벤트
        viewBinding.ibLastMonth.setOnClickListener(view -> {
            if (mLedger.size() <= 0) {
                return;
            }

            if (index != 0) { // 년,월이 제일 처음이 아니면
                index--;
            }
            else {   // 년,월이 처음이면
                index = monthList.size() - 1;
            }

            viewBinding.tvLedgerMonth.setText(monthList.get(index));
            String parsing = monthList.get(index).replaceAll("[^0-9]", "");

            int totalIncome = 0;
            int totalConsume = 0;
            List<Ledger> tempLedger = new ArrayList<>();

            for (int j = 0; j < mLedger.size(); j++) {
                if (parsing.equals(mLedger.get(j).getYear() + mLedger.get(j).getMonth())) {
                    tempLedger.add(mLedger.get(j));
                    if (mLedger.get(j).getClassfy().equals("지출")) {
                        totalConsume += Integer.parseInt(mLedger.get(j).getPrice());
                    } else if (mLedger.get(j).getClassfy().equals("수입")) {
                        totalIncome += Integer.parseInt(mLedger.get(j).getPrice());
                    }
                }
            }

            viewBinding.tvTotalincome.setText("수입 합계 : " + totalIncome + "원");
            viewBinding.tvTotalconsume.setText("지출 합계 : " + totalConsume + "원");
            viewBinding.tvPlusMinus.setText("수익 : " + (totalIncome - totalConsume) + "원");
            viewBinding.rvLedger.setAdapter(new LedgerAdapter(tempLedger, getActivity(), ""));
        });

        // 다음 달 버튼 이벤트
        viewBinding.ibNextMonth.setOnClickListener(view -> {
            if (mLedger.size() <= 0) {
                return;
            }

            if (index != monthList.size() - 1) { // 년, 월이 마지막이 아니면
                index++;
            } else {   // 년,월이 마지막이면
                index = 0;
            }

            viewBinding.tvLedgerMonth.setText(monthList.get(index));
            String parsing = monthList.get(index).replaceAll("[^0-9]", "");

            int totalIncome = 0;
            int totalConsume = 0;
            List<Ledger> tempLedger = new ArrayList<>();

            for (int j = 0; j < mLedger.size(); j++) {
                if (parsing.equals(mLedger.get(j).getYear() + mLedger.get(j).getMonth())) {
                    tempLedger.add(mLedger.get(j));

                    if (mLedger.get(j).getClassfy().equals("지출")) {
                        totalConsume += Integer.parseInt(mLedger.get(j).getPrice());
                    } else if (mLedger.get(j).getClassfy().equals("수입")) {
                        totalIncome += Integer.parseInt(mLedger.get(j).getPrice());
                    }
                }
            }

            viewBinding.tvTotalincome.setText("수입 합계 : " + totalIncome + "원");
            viewBinding.tvTotalconsume.setText("지출 합계 : " + totalConsume + "원");
            viewBinding.tvPlusMinus.setText("수익 : " + (totalIncome - totalConsume) + "원");
            viewBinding.rvLedger.setAdapter(new LedgerAdapter(tempLedger, getActivity(), ""));
        });

        myRef.child(user.getUid()).child("Ledger").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                viewBinding.tvLedgerMonth.setText("전체 가계부");
                mLedger.clear();
                selectMonth.clear();
                mAdapter.notifyDataSetChanged();

                ledgerView(dataSnapshot);                   // 유저 가계부 전체 리스트 생성
                monthList = new ArrayList(selectMonth); // 년 월만 빼서 따로 리스트 생성
                Collections.sort(monthList);              // 정렬
                if (!monthList.isEmpty()) {
                    // 숫자가 아닌 문자를 전부 제거한다.
                    index = monthList.size() - 1;
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.w(TAG, "Failed to read value.", error.toException());
            }
        });

        // 엑셀 내보내기 버튼 이벤트 - 가계부를 지정한 이름의 엑셀 파일로 저장하고 공유한다.
        viewBinding.btnExport.setOnClickListener(view -> {
            AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity());
            alertDialog.setTitle("저장할 가계부 이름을 설정해주세요");
            EditText editName = new EditText(getActivity());
            alertDialog.setView(editName);
            alertDialog.setPositiveButton("확인", (dialog, which) -> {
                String ledgerName = editName.getText().toString();
                if (ledgerName.isEmpty()) {
                    Toast.makeText(getActivity(), "파일 이름을 지정해 주세요", Toast.LENGTH_SHORT).show();
                    return;
                }

                saveExcel(ledgerName);
            });

            alertDialog.setNegativeButton("취소", (dialog, which) -> {});
            AlertDialog alert = alertDialog.create();
            alert.show();
        });

        return viewBinding.getRoot();
    }

    // [Refactor] for문 갯수 줄일 방법 찾아야 함
    // DB에서 사용자의 전체 가계부 목록을 얻어오고 총 수입,지출을 EditText에 출력한다.
    public void ledgerView(DataSnapshot dataSnapshot) {
        int totalIncome = 0;
        int totalConsume = 0;

        for (DataSnapshot yearSnapshot : dataSnapshot.getChildren()) { // 년
            for (DataSnapshot monthSnapshot : yearSnapshot.getChildren()) { // 월
                for (DataSnapshot daySnapshot : monthSnapshot.getChildren()) { // 일
                    for (DataSnapshot classfySnapshot : daySnapshot.getChildren()) { // 소비 분류
                        for (DataSnapshot timesSnapshot : classfySnapshot.getChildren()) {
                            Ledger ledger = new Ledger();
                            ledger.setClassfy(classfySnapshot.getKey());        //분류

                            ledger.setYear(yearSnapshot.getKey());              //년
                            ledger.setMonth(monthSnapshot.getKey());            //월
                            selectMonth.add(ledger.getYear()+"년 "+ledger.getMonth()+"월");
                            ledger.setDay(daySnapshot.getKey());                //일
                            ledger.setTimes(timesSnapshot.getKey());            //시간

                            LedgerContent ledgerContent = timesSnapshot.getValue(LedgerContent.class);
                            ledger.setPaymemo(ledgerContent.getPaymemo()); ;   //내용
                            ledger.setPrice(ledgerContent.getPrice()); ;       //금액
                            ledger.setUseItem(ledgerContent.getUseItem()); ;   //물품 분류

                            if (ledger.getClassfy().equals("지출")) {
                                totalConsume += Integer.parseInt(ledger.getPrice());
                            }
                            else if (ledger.getClassfy().equals("수입")) {
                                totalIncome += Integer.parseInt(ledger.getPrice());
                            }

                            mLedger.add(ledger);
                            mAdapter.notifyItemInserted(mLedger.size() - 1);
                        }
                    }
                }
            }
        }

        viewBinding.tvTotalincome.setText("수입 합계 : " + totalIncome + "원");
        viewBinding.tvTotalconsume.setText("지출 합계 : " + totalConsume + "원");
        viewBinding.tvPlusMinus.setText("수익 : " + (totalIncome - totalConsume) + "원");
    }

    // Apache POI 3.17 라이브러리를 사용해 가계부 정보를 액셀 파일로 만들어 공유한다.
    // https://www.codejava.net/coding/how-to-write-excel-files-in-java-using-apache-poi
    // https://easy-coding.tistory.com/48
    private void saveExcel(String ledgerName){
        Workbook workbook = new HSSFWorkbook();
        Sheet sheet = workbook.createSheet(); // 새로운 시트 생성

        Row row = sheet.createRow(0); // 새로운 행 생성
        Cell cell;

        cell = row.createCell(0); // 1번 셀 생성
        cell.setCellValue("날짜"); // 1번 셀 값 입력

        cell = row.createCell(1); // 2번 셀 생성
        cell.setCellValue("소비 분류"); // 2번 셀 값 입력

        cell = row.createCell(2); // 2번 셀 생성
        cell.setCellValue("분류"); // 2번 셀 값 입력

        cell = row.createCell(3); // 2번 셀 생성
        cell.setCellValue("금액"); // 2번 셀 값 입력

        cell = row.createCell(4); // 2번 셀 생성
        cell.setCellValue("내용"); // 2번 셀 값 입력

        // 가계부 데이터를 엑셀에 입력한다.
        for(int i = 0; i < mLedger.size() ; i++){
            row = sheet.createRow(i+1);
            cell = row.createCell(0);
            cell.setCellValue(mLedger.get(i).getYear() + "-" + mLedger.get(i).getMonth() + "-" + mLedger.get(i).getDay());
            cell = row.createCell(1);
            cell.setCellValue(mLedger.get(i).getClassfy());
            cell = row.createCell(2);
            cell.setCellValue(mLedger.get(i).getUseItem());
            cell = row.createCell(3);
            cell.setCellValue(mLedger.get(i).getPrice());
            cell = row.createCell(4);
            cell.setCellValue(mLedger.get(i).getPaymemo());
        }

        // 외부 저장소에 엑셀 파일을 저장할 폴더 생성
        File storageDir = new File(Environment.getExternalStorageDirectory() + "/SmaRed/Excel");
        if (!storageDir.exists()) {
            storageDir.mkdirs();
        }

        // 외부 저장소에 엑셀 파일 생성
        File xlsFile = new File(Environment.getExternalStorageDirectory() + "/SmaRed/Excel",ledgerName+".xls");

        // 생성한 엑셀 파일의 파일 스트림을 얻어서 workbook에 저장했던 가계부 데이터를 쓴다.
        try {
            FileOutputStream os = new FileOutputStream(xlsFile);
            workbook.write(os);
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        Toast.makeText(getActivity(),xlsFile.getAbsolutePath() + "에 저장되었습니다", Toast.LENGTH_SHORT).show();

        // 데이터가 입력된 엑셀 파일 공유
        Uri path = Uri.fromFile(xlsFile);
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("application/excel");
        shareIntent.putExtra(Intent.EXTRA_STREAM, path);
        startActivity(Intent.createChooser(shareIntent,"엑셀 내보내기"));
    }

    // 필요한 권한 중 허가되지 않은 것이 하나라도 있을 경우
    // 필요한 모든 권한을 요청하고 실패 처리한다.
    private boolean checkPermissions() {
        int result;
        List<String> permissionList = new ArrayList<>();

        // 필요한 권한 중에 허가되지 않은 권한 목록을 수집한다.
        for (String pm : permissions) {
            result = ContextCompat.checkSelfPermission(getActivity(), pm);
            if (result != PackageManager.PERMISSION_GRANTED) {
                permissionList.add(pm);
            }
        }

        // 허가되지 않은 권한이 있을 경우 필요한 모든 권한을 요청하고 실패 처리한다.
        if (!permissionList.isEmpty()) {
            ActivityCompat.requestPermissions(getActivity(), permissionList.toArray(new String[permissionList.size()]), MULTIPLE_PERMISSIONS);
            return false;
        }

        // 필요한 모든 권한이 있으므로 성공 처리한다.
        return true;
    }

    // 권한을 요청했을 때 결과를 알려주는 콜백 함수
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MULTIPLE_PERMISSIONS: {
                // 새로 허가된 권한이 없는 경우 사용자에게 알리고 액티비티를 종료한다.
                if (grantResults.length == 0) {
                    showNoPermissionToastAndFinish();
                    return;
                }

                // 새로 허가된 권한이 있다면 필요한 모든 권한이 허가되었는지 확인한다.
                for (int i = 0; i < permissions.length; i++) {
                    if (permissions[i].equals(this.permissions[0])) {
                        if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                            showNoPermissionToastAndFinish();
                        }
                    }
                    else if (permissions[i].equals(this.permissions[1])) {
                        if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                            showNoPermissionToastAndFinish();
                        }
                    }
                }
                return;
            }
        }
    }

    // 필요한 권한이 허가되지 않았음을 사용자에게 알리고 액티비티를 종료한다.
    private void showNoPermissionToastAndFinish() {
        Toast.makeText(getActivity(), "권한 요청에 동의 해주셔야 이용 가능합니다. 설정에서 권한 허용 하시기 바랍니다.", Toast.LENGTH_SHORT).show();
        getActivity().finish();
    }
}