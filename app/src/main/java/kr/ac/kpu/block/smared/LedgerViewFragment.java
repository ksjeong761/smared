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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
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

    private DatabaseReference myRef;
    private FirebaseUser user;

    // 가계부 전체 데이터
    private List<Ledger> allLedgerData = new ArrayList<>();

    // 데이터가 존재하는 년,월만을 기록하여 월별 데이터를 조회할 수 있도록 한다.
    private Set<String> yearsAndMonthsHavingDataSet = new HashSet<>(); // 중복제거용 Set
    private List<String> yearsAndMonthsHavingDataList = new ArrayList<>(); // 데이터가 존재하는 년,월만을 기록
    private int yearsAndMonthsIndex = 0; // 년,월 인덱스

    private static final int MULTIPLE_PERMISSIONS = 101;
    private String[] permissions = {
        android.Manifest.permission.READ_EXTERNAL_STORAGE,
        android.Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        viewBinding = FragmentLedgerViewBinding.inflate(inflater, container, false);

        myRef = FirebaseDatabase.getInstance().getReference("users");
        user = FirebaseAuth.getInstance().getCurrentUser();

        checkPermissions();

        // 가계부 데이터를 출력한다.
        loadLedgerAndShowList();

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

                ledgerToExcelFileAndExport(ledgerName);
            });

            alertDialog.setNegativeButton("취소", (dialog, which) -> {});
            alertDialog.create().show();
        });

        // 이전 달 버튼 이벤트
        viewBinding.ibLastMonth.setOnClickListener(view -> {
            yearsAndMonthsIndex--;
            if (yearsAndMonthsIndex < 0) {
                yearsAndMonthsIndex = yearsAndMonthsHavingDataList.size() - 1;
            }

            showListByMonth();
        });

        // 다음 달 버튼 이벤트
        viewBinding.ibNextMonth.setOnClickListener(view -> {
            yearsAndMonthsIndex++;
            if (yearsAndMonthsIndex > yearsAndMonthsHavingDataList.size() - 1) {
                yearsAndMonthsIndex = 0;
            }

            showListByMonth();
        });

        return viewBinding.getRoot();
    }

    // 선택한 채팅방의 가계부 데이터를 불러와 화면에 출력한다.
    private void loadLedgerAndShowList() {
        myRef.child(user.getUid()).child("Ledger").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // 사용자의 전체 가계부 목록을 불러온다.
                viewBinding.tvLedgerMonth.setText("전체 가계부");
                loadLedger(dataSnapshot);

                // 가계부 목록을 출력한다.
                showListByMonth();

                // 년 월만 빼서 따로 리스트 생성
                yearsAndMonthsHavingDataList = new ArrayList(yearsAndMonthsHavingDataSet);
                Collections.sort(yearsAndMonthsHavingDataList);
                if (!yearsAndMonthsHavingDataList.isEmpty()) {
                    yearsAndMonthsIndex = yearsAndMonthsHavingDataList.size() - 1;
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                logger.writeLog("Failed to read value : " + error.toException().getMessage());
            }
        });
    }

    private void showListByMonth() {
        viewBinding.tvLedgerMonth.setText(yearsAndMonthsHavingDataList.get(yearsAndMonthsIndex));
        String parsing = yearsAndMonthsHavingDataList.get(yearsAndMonthsIndex).replaceAll("[^0-9]", "");

        int totalIncome = 0;
        int totalExpenditure = 0;
        List<Ledger> oneMonthLedgerData = new ArrayList<>();

        for (int ledgerDataIndex = 0; ledgerDataIndex < allLedgerData.size(); ledgerDataIndex++) {
            if (!parsing.equals(allLedgerData.get(ledgerDataIndex).getYear() + allLedgerData.get(ledgerDataIndex).getMonth())) {
                continue;
            }

            oneMonthLedgerData.add(allLedgerData.get(ledgerDataIndex));
            if (allLedgerData.get(ledgerDataIndex).getClassify().equals("지출")) {
                totalExpenditure += Integer.parseInt(allLedgerData.get(ledgerDataIndex).getLedgerContent().getPrice());
            } else if (allLedgerData.get(ledgerDataIndex).getClassify().equals("수입")) {
                totalIncome += Integer.parseInt(allLedgerData.get(ledgerDataIndex).getLedgerContent().getPrice());
            }
        }

        viewBinding.tvTotalincome.setText("수입 합계 : " + totalIncome + "원");
        viewBinding.tvTotalconsume.setText("지출 합계 : " + totalExpenditure + "원");
        viewBinding.tvPlusMinus.setText("수익 : " + (totalIncome - totalExpenditure) + "원");

        viewBinding.rvLedger.setHasFixedSize(true);
        viewBinding.rvLedger.setLayoutManager(new LinearLayoutManager(getActivity()));
        viewBinding.rvLedger.setAdapter(new LedgerAdapter(oneMonthLedgerData, getActivity()));
        viewBinding.rvLedger.scrollToPosition(0);
    }

    // DB에서 사용자의 전체 가계부 목록을 얻어오고 총 수입,지출을 EditText에 출력한다.
    private void loadLedger(DataSnapshot dataSnapshot) {
        int totalIncome = 0;
        int totalExpenditure = 0;

        allLedgerData.clear();
        yearsAndMonthsHavingDataSet.clear();
        for (DataSnapshot yearSnapshot : dataSnapshot.getChildren()) { // 년
            for (DataSnapshot monthSnapshot : yearSnapshot.getChildren()) { // 월
                for (DataSnapshot daySnapshot : monthSnapshot.getChildren()) { // 일
                    for (DataSnapshot classifySnapshot : daySnapshot.getChildren()) { // 소비 분류
                        for (DataSnapshot timesSnapshot : classifySnapshot.getChildren()) { // 가계부 정보
                            Ledger ledger = new Ledger();
                            LedgerContent ledgerContent = timesSnapshot.getValue(LedgerContent.class);
                            ledger.setClassify(classifySnapshot.getKey()); // 분류
                            ledger.setYear(yearSnapshot.getKey()); // 년
                            ledger.setMonth(monthSnapshot.getKey()); // 월
                            ledger.setDay(daySnapshot.getKey()); // 일
                            ledger.setTimes(timesSnapshot.getKey()); // 시간
                            ledger.setLedgerContent(ledgerContent); // 내용, 금액, 물품 분류

                            if (ledger.getClassify().equals("지출")) {
                                totalExpenditure += Integer.parseInt(ledger.getLedgerContent().getPrice());
                            } else if (ledger.getClassify().equals("수입")) {
                                totalIncome += Integer.parseInt(ledger.getLedgerContent().getPrice());
                            }

                            yearsAndMonthsHavingDataSet.add(ledger.getYear() + "년 " + ledger.getMonth() + "월");
                            allLedgerData.add(ledger);
                        }
                    }
                }
            }
        }

        viewBinding.tvTotalincome.setText("수입 합계 : " + totalIncome + "원");
        viewBinding.tvTotalconsume.setText("지출 합계 : " + totalExpenditure + "원");
        viewBinding.tvPlusMinus.setText("수익 : " + (totalIncome - totalExpenditure) + "원");
    }

    // Apache POI 3.17 라이브러리를 사용해 가계부 정보를 액셀 파일로 만들어 공유한다.
    // https://www.codejava.net/coding/how-to-write-excel-files-in-java-using-apache-poi
    // https://easy-coding.tistory.com/48
    private void ledgerToExcelFileAndExport(String ledgerName) {
        // 엑셀 객체를 생성한다.
        Workbook workbook = new HSSFWorkbook();
        Sheet sheet = workbook.createSheet();

        // 칼럼 제목을 입력한다.
        Row columnHeaderRow = sheet.createRow(0);
        String[] columnHeaders = {"날짜", "소비 분류", "분류", "금액", "내용"};
        for (int columnIndex = 0; columnIndex < columnHeaders.length; columnIndex++) {
            Cell cell = columnHeaderRow.createCell(columnIndex);
            cell.setCellValue(columnHeaders[columnIndex]);
        }

        // 가계부 데이터를 입력한다.
        for (int ledgerIndex = 0; ledgerIndex < allLedgerData.size(); ledgerIndex++) {
            Row row = sheet.createRow(ledgerIndex + 1);
            String[] columnValues = {
                    allLedgerData.get(ledgerIndex).getYear() + "-" + allLedgerData.get(ledgerIndex).getMonth() + "-" + allLedgerData.get(ledgerIndex).getDay(),
                    allLedgerData.get(ledgerIndex).getClassify(),
                    allLedgerData.get(ledgerIndex).getLedgerContent().getUseItem(),
                    allLedgerData.get(ledgerIndex).getLedgerContent().getPrice(),
                    allLedgerData.get(ledgerIndex).getLedgerContent().getPayMemo()
            };
            for (int columnIndex = 0; columnIndex < columnHeaders.length; columnIndex++) {
                Cell cell = row.createCell(columnIndex);
                cell.setCellValue(columnValues[columnIndex]);
            }
        }

        FileOutputStream os = null;
        try {
            // 외부 저장소에 엑셀 파일을 저장할 폴더 생성
            String directoryPath = Environment.getExternalStorageDirectory() + "/SmaRed/Excel";
            String fileName = ledgerName + ".xls";
            File storageDir = new File(directoryPath);
            if (!storageDir.exists()) {
                storageDir.mkdirs();
            }

            // 엑셀 파일 생성
            File xlsFile = new File(directoryPath, fileName);

            // 생성한 엑셀 파일에 가계부 데이터를 쓴다.
            os = new FileOutputStream(xlsFile);
            workbook.write(os);

            Toast.makeText(getActivity(),xlsFile.getAbsolutePath() + "에 엑셀 파일이 저장되었습니다", Toast.LENGTH_SHORT).show();

            // 엑셀 파일 공유
            Uri path = Uri.fromFile(xlsFile);
            Intent exportExcelIntent = new Intent(Intent.ACTION_SEND);
            exportExcelIntent.setType("application/excel");
            exportExcelIntent.putExtra(Intent.EXTRA_STREAM, path);
            startActivity(Intent.createChooser(exportExcelIntent,"엑셀 내보내기"));
        } catch (IOException e) {
            Toast.makeText(getActivity(), "엑셀 파일 생성에 실패했습니다.", Toast.LENGTH_LONG).show();
            logger.writeLog("IOException : " + e.getMessage());
        } catch (Exception e) {
            Toast.makeText(getActivity(), "엑셀 내보내기에 실패했습니다.", Toast.LENGTH_LONG).show();
            logger.writeLog("Exception : " + e.getMessage());
        } finally {
            try {
                os.close();
            } catch (IOException e) {
                logger.writeLog("IOException close stream : " + e.getMessage());
            }
        }
    }

    // 필요한 권한 중 허가되지 않은 것이 하나라도 있을 경우
    // 필요한 모든 권한을 요청하고 실패 처리한다.
    private boolean checkPermissions() {
        // 필요한 권한 중에 허가되지 않은 권한 목록을 수집한다.
        List<String> deniedPermissions = new ArrayList<>();
        for (String permission : permissions) {
            if (PackageManager.PERMISSION_GRANTED != ContextCompat.checkSelfPermission(getActivity(), permission)) {
                deniedPermissions.add(permission);
            }
        }

        // 허가되지 않은 권한이 있을 경우 필요한 모든 권한을 요청하고 실패 처리한다.
        if (!deniedPermissions.isEmpty()) {
            ActivityCompat.requestPermissions(getActivity(), deniedPermissions.toArray(new String[deniedPermissions.size()]), MULTIPLE_PERMISSIONS);
            return false;
        }

        // 필요한 모든 권한이 있으므로 성공 처리한다.
        return true;
    }

    // 권한을 요청했을 때 결과를 알려주는 콜백 함수
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MULTIPLE_PERMISSIONS:
                // 새로 허가된 권한이 없는 경우 사용자에게 알리고 액티비티를 종료한다.
                if (grantResults.length == 0) {
                    showNoPermissionToastAndFinish();
                    return;
                }

                // 새로 허가된 권한이 있다면 필요한 모든 권한이 허가되었는지 확인한다.
                for (int i = 0; i < permissions.length; i++) {
                    for (int j = 0; j < this.permissions.length; j++) {
                        if (permissions[i].equals(this.permissions[j])) {
                            continue;
                        }

                        if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                            showNoPermissionToastAndFinish();
                        }
                    }
                }

                break;
        }
    }

    // 필요한 권한이 허가되지 않았음을 사용자에게 알리고 액티비티를 종료한다.
    private void showNoPermissionToastAndFinish() {
        Toast.makeText(getActivity(), "권한 요청에 동의 해주셔야 이용 가능합니다. 설정에서 권한 허용 하시기 바랍니다.", Toast.LENGTH_SHORT).show();
        getActivity().finish();
    }
}