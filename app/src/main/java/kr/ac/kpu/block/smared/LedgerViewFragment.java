package kr.ac.kpu.block.smared;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.widget.LinearLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import kr.ac.kpu.block.smared.databinding.FragmentLedgerViewBinding;

public class LedgerViewFragment extends android.app.Fragment {
    private FormattedLogger logger = new FormattedLogger();
    private FragmentLedgerViewBinding viewBinding;
    private PermissionChecker permissionChecker;

    // 가계부 전체 데이터
    private List<Ledger> allLedgerData = new ArrayList<>();
    private LocalDateTime displayedDateTime = LocalDateTime.now();

    // 권한을 요청했을 때 결과를 알려주는 콜백 함수
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (!permissionChecker.isPermissionRequestSuccessful(grantResults)) {
            Toast.makeText(getActivity(), "권한 요청에 동의 해주셔야 이용 가능합니다. 설정에서 권한을 허용해주시기 바랍니다.", Toast.LENGTH_SHORT).show();
            getActivity().finish();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        viewBinding = FragmentLedgerViewBinding.inflate(inflater, container, false);

        // 권한 체크
        final String[] necessaryPermissions = {
            android.Manifest.permission.READ_EXTERNAL_STORAGE,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        };
        permissionChecker = new PermissionChecker(getActivity(), necessaryPermissions);
        permissionChecker.requestLackingPermissions();

        // UI 초기화
        viewBinding.rvLedger.setHasFixedSize(true);
        viewBinding.rvLedger.setLayoutManager(new LinearLayoutManager(getActivity()));

        readLedgerDB();

        // 이벤트 등록
        viewBinding.btnExport.setOnClickListener(view -> showExportExcelDialog());
        viewBinding.ibPreviousMonth.setOnClickListener(view -> displayPreviousMonthData());
        viewBinding.ibNextMonth.setOnClickListener(view -> displayNextMonthData());

        return viewBinding.getRoot();
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
                    allLedgerData.get(ledgerIndex).getPaymentTimestamp("yyyy-MM-dd"),
                    allLedgerData.get(ledgerIndex).getCategory(),
                    String.valueOf(allLedgerData.get(ledgerIndex).getTotalPrice()),
                    allLedgerData.get(ledgerIndex).getDescription()
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

    private void showExportExcelDialog() {
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
    }

    private void displayPreviousMonthData() {
        displayedDateTime = displayedDateTime.withMonth(displayedDateTime.getMonthValue() - 1);
        displayOneMonthData(allLedgerData, displayedDateTime);
    }

    private void displayNextMonthData() {
        displayedDateTime = displayedDateTime.withMonth(displayedDateTime.getMonthValue() + 1);
        displayOneMonthData(allLedgerData, displayedDateTime);
    }

    private void displayOneMonthData(List<Ledger> ledgerData, LocalDateTime dateTime) {
        // 월초, 월말 시간을 구하여 월간 데이터를 추출한다.
        Calendar calendar = Calendar.getInstance();
        calendar.set(dateTime.getYear(), dateTime.getMonthValue()-1, 1, 0, 0, 0);
        long startOfMonth = calendar.getTimeInMillis();
        calendar.set(dateTime.getYear(), dateTime.getMonthValue(), 1, 0, 0, 0);
        long endOfMonth = calendar.getTimeInMillis()-1;

        // 한 달간 작성된 가계부 목록을 화면에 출력한다.
        List<Ledger> oneMonthData = new ArrayList<>();
        for (Ledger ledger : ledgerData) {
            if (startOfMonth > ledger.getPaymentTimestamp())
                continue;
            if (endOfMonth < ledger.getPaymentTimestamp())
                break;

            oneMonthData.add(ledger);
        }
        viewBinding.rvLedger.setAdapter(new LedgerAdapter(oneMonthData, getActivity()));
        viewBinding.rvLedger.scrollToPosition(0);

        // 한 달간 수입, 지출 합계를 화면에 출력한다.
        double totalIncome = 0;
        double totalExpenditure = 0;
        for (Ledger ledger : oneMonthData) {
            if (ledger.getTotalPrice() < 0) {
                totalIncome += ledger.getTotalPrice();
            }
            if (ledger.getTotalPrice() > 0) {
                totalExpenditure += ledger.getTotalPrice();
            }
        }
        viewBinding.tvIncome.setText("수입 합계 : " + totalIncome + "원");
        viewBinding.tvExpenditure.setText("지출 합계 : " + totalExpenditure + "원");
        viewBinding.tvResult.setText("총 합계 : " + (totalIncome - totalExpenditure) + "원");
    }

    private void readLedgerDB() {
        // DB 경로 지정
        String userUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String databasePath = "ledger" + "/"+ userUid;
        DatabaseReference ledgerDBRef = FirebaseDatabase.getInstance().getReference(databasePath);

        // DB 읽어오기
        ledgerDBRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot userSnapshot) {
                // 전체 데이터를 읽어온다.
                for (DataSnapshot timesSnapshot : userSnapshot.getChildren()) {
                    allLedgerData.add(timesSnapshot.getValue(Ledger.class));
                }

                // 이번 달 데이터만 잘라 표시한다.
                displayOneMonthData(allLedgerData, displayedDateTime);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(getActivity(), "Failed to read value : " + error.toException().getMessage(), Toast.LENGTH_SHORT).show();
                logger.writeLog("Failed to read value : " + error.toException().getMessage());
            }
        });
    }
}