package kr.ac.kpu.block.smared;

import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.github.mikephil.charting.utils.ColorTemplate;

import com.google.firebase.database.DataSnapshot;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import kr.ac.kpu.block.smared.databinding.FragmentLedgerStatBinding;

public class LedgerStatFragment extends android.app.Fragment {
    private FormattedLogger logger = new FormattedLogger();
    private FragmentLedgerStatBinding viewBinding;

    // 통계를 정리하기 위해 가계부 전체 데이터를 가져온다.
    private List<Ledger> allLedgerData = new ArrayList<>();
    private LocalDateTime displayedDateTime = LocalDateTime.now();

    // 카테고리 별 소비 합계를 저장해두고 차트 클릭 시 출력한다.
    private Map<String, Double> categoryTotalPrice = new HashMap<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        viewBinding = FragmentLedgerStatBinding.inflate(inflater, container, false);

        readLedgerDB();

        viewBinding.ibPreviousMonth.setOnClickListener(view -> displayPreviousMonthData());
        viewBinding.ibNextMonth.setOnClickListener(view -> displayNextMonthData());

        return viewBinding.getRoot();
    }

    // 통계를 계산한 뒤 차트로 출력한다. (mikephil 라이브러리 사용)
    private void showChart(List<Ledger> showingData) {
        if (showingData.size() <= 0) {
            return;
        }

        // 소비 카테고리 별 합계를 구한다.
        for (Ledger ledger : showingData) {
            String category = ledger.getTotalCategory();
            Double price = categoryTotalPrice.get(category);
            if (price == null) {
                price = 0.0;
            }

            price += ledger.getTotalPrice();
            categoryTotalPrice.put(category, price);
        }

        // 백분율을 구하여 차트에 출력한다.
        float total = (float)0.0;
        ArrayList<PieEntry> yValues = new ArrayList<>();
        for (Map.Entry<String, Double> set : categoryTotalPrice.entrySet()) {
            total += set.getValue();
        }
        for (Map.Entry<String, Double> set : categoryTotalPrice.entrySet()) {
            float percent = (float)(set.getValue() / total * 100);
            if (percent > 0.0) {
                yValues.add(new PieEntry(percent, set.getKey()));
            }
        }

        // 파이 차트를 어떻게 출력할 지 설정한다.
        PieDataSet dataSet = new PieDataSet(yValues,"");
        dataSet.setSliceSpace(3f);
        dataSet.setSelectionShift(5f);
        dataSet.setColors(ColorTemplate.JOYFUL_COLORS);
        PieData data = new PieData(dataSet);
        data.setValueTextSize(15f);
        data.setValueTextColor(Color.BLACK);
        viewBinding.pieChart.setData(data);

        Description description = new Description();
        description.setText("소비 분류");
        description.setTextSize(15);
        viewBinding.pieChart.setDescription(description);
        viewBinding.pieChart.getDescription().setEnabled(false);

        viewBinding.pieChart.animateY(1000, Easing.EasingOption.EaseInOutCubic);
        viewBinding.pieChart.setEntryLabelColor(Color.BLACK);
        viewBinding.pieChart.setUsePercentValues(true);
        viewBinding.pieChart.setExtraOffsets(5,10,5,5);
        viewBinding.pieChart.setDragDecelerationFrictionCoef(0.95f);
        viewBinding.pieChart.setTransparentCircleRadius(61f);
        viewBinding.pieChart.setDrawHoleEnabled(false);
        viewBinding.pieChart.setHoleColor(Color.WHITE);

        // 사용자가 차트를 터치했을 때 선택한 카테고리의 소비 합계를 다이얼로그로 출력한다.
        viewBinding.pieChart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, Highlight h) {
                PieEntry pieEntry = (PieEntry) e;
                String selectedCategory = pieEntry.getLabel();
                double selectedCategoryPrice = categoryTotalPrice.get(selectedCategory);

                AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity());
                alertDialog.setMessage(selectedCategory + " 총계 : " + selectedCategoryPrice + "원");
                alertDialog.create().show();
            }

            @Override
            public void onNothingSelected() {}
        });
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

        showChart(oneMonthData);
    }

    private void readLedgerDB() {
        // [TODO] user 객체의 ledger 객체 수만큼 반복
        {
            Ledger ledger = new Ledger();

            DAO dao = new DAO();
            dao.setSuccessCallback(arg -> afterSuccess(arg));
            dao.setFailureCallback(arg -> afterFailure());
            dao.readAll(ledger, Ledger.class);

            allLedgerData.add(ledger);
        }
    }

    // DB 읽기 성공 시 동작
    private void afterSuccess(DataSnapshot userSnapshot) {
        // 전체 데이터를 읽어온다.
        for (DataSnapshot timesSnapshot : userSnapshot.getChildren()) {
            allLedgerData.add(timesSnapshot.getValue(Ledger.class));
        }

        // 이번 달 데이터만 잘라 표시한다.
        displayOneMonthData(allLedgerData, displayedDateTime);
    }

    // DB 읽기 실패 시 동작
    private void afterFailure() {
        Toast.makeText(getActivity(), "Failed to read value.", Toast.LENGTH_SHORT).show();
    }
}