package kr.ac.kpu.block.smared;

import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

import kr.ac.kpu.block.smared.databinding.FragmentLedgerStatBinding;

public class LedgerStatFragment extends android.app.Fragment {
    private FormattedLogger logger = new FormattedLogger();
    private FragmentLedgerStatBinding viewBinding;

    // 통계를 정리하기 위해 가계부 전체 데이터를 가져온다.
    private List<Ledger> allLedgerData = new ArrayList<>();

    // 카테고리 별 소비 합계를 저장해두고 차트 클릭 시 출력한다.
    private float clothPrice = 0;
    private float foodPrice = 0;
    private float transPrice = 0;
    private float marketPrice = 0;
    private float homePrice = 0;
    private float etcPrice = 0;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        viewBinding = FragmentLedgerStatBinding.inflate(inflater, container, false);

        // DB 경로를 지정한다.
        String userUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String databasePath = "ledger" + "/" + userUid;
        DatabaseReference ledgerDBRef = FirebaseDatabase.getInstance().getReference(databasePath);
        ledgerDBRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot userSnapshot) {
                for (DataSnapshot timeSnapshot : userSnapshot.getChildren()) {
                    Ledger ledger = new Ledger();
                    Ledger tempLedger = timeSnapshot.getValue(Ledger.class);

                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                logger.writeLog("기존 내역 불러오기에 실패하였습니다. - " + databaseError.toException());
            }
        });

        //viewBinding.ibLastMonth2.setOnClickListener(view -> showPreviousMonthChart());
        //viewBinding.ibNextMonth2.setOnClickListener(view -> showNextMonthChart());

        return viewBinding.getRoot();
    }

    // 소비 카테고리 별 합계, 백분율을 구한 뒤
    // 차트의 속성을 설정한다.
    private void showChart() {
        if (allLedgerData.size() <= 0) {
            return;
        }

        // 소비 카테고리 별 합계를 구한다.
        for (int ledgerDataIndex = 0; ledgerDataIndex < allLedgerData.size(); ledgerDataIndex++) {
            double price = allLedgerData.get(ledgerDataIndex).getTotalPrice();
            String category = allLedgerData.get(ledgerDataIndex).getCategory();
            switch (category) {
                case "의류비":
                    clothPrice += price;
                    break;
                case "식비":
                    foodPrice += price;
                    break;
                case "주거비":
                    homePrice += price;
                    break;
                case "교통비":
                    transPrice += price;
                    break;
                case "생필품":
                    marketPrice += price;
                    break;
                case "기타":
                    etcPrice += price;
                    break;
            }
        }

        // 백분율을 구한다.
        float totalPrice = clothPrice + foodPrice + homePrice + transPrice + marketPrice + etcPrice;
        float clothPercent = (clothPrice / totalPrice) * 100;
        float foodPercent = (foodPrice / totalPrice) * 100;
        float homePercent = (homePrice / totalPrice) * 100;
        float transPercent = (transPrice / totalPrice) * 100;
        float marketPercent = (marketPrice / totalPrice) * 100;
        float etcPercent = (etcPrice / totalPrice) * 100;

        // 0%가 아니라면 차트에 카테고리명을 표시한다.
        ArrayList<PieEntry> yValues = new ArrayList<>();
        if (clothPercent > 0) yValues.add(new PieEntry(clothPercent, "의류비"));
        if (foodPercent > 0) yValues.add(new PieEntry(foodPercent, "식비"));
        if (homePercent > 0) yValues.add(new PieEntry(homePercent, "주거비"));
        if (transPercent > 0) yValues.add(new PieEntry(transPercent, "교통비"));
        if (marketPercent > 0) yValues.add(new PieEntry(marketPercent, "생필품"));
        if (etcPercent > 0) yValues.add(new PieEntry(etcPercent, "기타"));

        // 파이 차트를 어떻게 출력할 지 설정한다.
        PieDataSet dataSet = new PieDataSet(yValues,"");
        dataSet.setSliceSpace(3f);
        dataSet.setSelectionShift(5f);
        dataSet.setColors(ColorTemplate.JOYFUL_COLORS);

        PieData data = new PieData(dataSet);
        data.setValueTextSize(15f);
        data.setValueTextColor(Color.BLACK);

        Description description = new Description();
        description.setText("소비 분류"); //라벨
        description.setTextSize(15);

        viewBinding.pieChart.setData(data);
        viewBinding.pieChart.setDescription(description);
        viewBinding.pieChart.setEntryLabelColor(Color.BLACK);
        viewBinding.pieChart.animateY(1000, Easing.EasingOption.EaseInOutCubic); //애니메이션
        viewBinding.pieChart.setUsePercentValues(true);
        viewBinding.pieChart.getDescription().setEnabled(false);
        viewBinding.pieChart.setExtraOffsets(5,10,5,5);
        viewBinding.pieChart.setDragDecelerationFrictionCoef(0.95f);
        viewBinding.pieChart.setDrawHoleEnabled(false);
        viewBinding.pieChart.setHoleColor(Color.WHITE);
        viewBinding.pieChart.setTransparentCircleRadius(61f);

        // 사용자가 차트를 터치했을 때 선택한 카테고리의 소비 합계를 다이얼로그로 출력한다.
        viewBinding.pieChart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, Highlight h) {
                int selectedPieChartPrice = 0;
                PieEntry pe = (PieEntry) e;
                String selectedPieChartLabel = pe.getLabel();
                switch (selectedPieChartLabel) {
                    case "의류비":
                        selectedPieChartPrice = (int)clothPrice;
                        break;
                    case "식비":
                        selectedPieChartPrice = (int)foodPrice;
                        break;
                    case "주거비":
                        selectedPieChartPrice = (int)homePrice;
                        break;
                    case "교통비":
                        selectedPieChartPrice = (int)transPrice;
                        break;
                    case "생필품":
                        selectedPieChartPrice = (int)marketPrice;
                        break;
                    case "기타":
                        selectedPieChartPrice = (int)etcPrice;
                        break;
                }

                AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity());
                alertDialog.setMessage(selectedPieChartLabel + " 총계 : " + selectedPieChartPrice + "원");
                alertDialog.create().show();
            }

            @Override
            public void onNothingSelected() {}
        });
    }
}