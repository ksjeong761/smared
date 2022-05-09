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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import kr.ac.kpu.block.smared.databinding.FragmentLedgerStatBinding;

public class LedgerStatFragment extends android.app.Fragment {
    private FormattedLogger logger = new FormattedLogger();
    private FragmentLedgerStatBinding viewBinding;

    // 통계를 정리하기 위해 가계부 전체 데이터를 가져온다.
    private List<Ledger> allLedgerData = new ArrayList<>();

    // 데이터가 존재하는 년,월만을 기록하여 월별 통계를 조회할 수 있도록 한다.
    private Set<String> yearsAndMonthsHavingDataSet = new HashSet<>(); // 중복제거용 Set
    private List<String> yearsAndMonthsHavingDataList; // 데이터가 존재하는 년,월만을 기록
    private int yearsAndMonthsIndex = 0; // 년,월 인덱스

    // 카테고리 별 소비 합계를 저장해두고 차트 클릭 시 출력한다.
    private float clothPrice = 0;
    private float foodPrice = 0;
    private float transPrice = 0;
    private float marketPrice = 0;
    private float homePrice = 0;
    private float etcPrice = 0;

    DatabaseReference myRef;
    FirebaseUser user;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        viewBinding = FragmentLedgerStatBinding.inflate(inflater, container, false);

        myRef = FirebaseDatabase.getInstance().getReference("users");
        user = FirebaseAuth.getInstance().getCurrentUser();

        // 사용자의 가계부 데이터를 불러와 차트를 출력한다.
        loadLedgerAndShowChart();

        // 이전 달 버튼 이벤트
        viewBinding.ibLastMonth2.setOnClickListener(view -> {
            yearsAndMonthsIndex--;
            if (yearsAndMonthsIndex < 0) {
                yearsAndMonthsIndex = yearsAndMonthsHavingDataList.size() - 1;
            }

            viewBinding.tvLedgerMonth2.setText(yearsAndMonthsHavingDataList.get(yearsAndMonthsIndex));

            showChart();
        });

        // 다음 달 버튼 이벤트
        viewBinding.ibNextMonth2.setOnClickListener(view -> {
            yearsAndMonthsIndex++;
            if (yearsAndMonthsIndex >= yearsAndMonthsHavingDataList.size()) {
                yearsAndMonthsIndex = 0;
            }

            viewBinding.tvLedgerMonth2.setText(yearsAndMonthsHavingDataList.get(yearsAndMonthsIndex));

            showChart();
        });

        return viewBinding.getRoot();
    }

    // 사용자의 전체 가계부 내역을 읽어온다.
    private void loadLedger(DataSnapshot dataSnapshot) {
        for (DataSnapshot yearSnapshot : dataSnapshot.getChildren()) { // 년
            for (DataSnapshot monthSnapshot : yearSnapshot.getChildren()) { // 월
                for (DataSnapshot daySnapshot : monthSnapshot.getChildren()) { // 일
                    for (DataSnapshot classfySnapshot : daySnapshot.getChildren()) { // 분류
                        for (DataSnapshot timesSnapshot : classfySnapshot.getChildren()) { // 시간
                            Ledger ledger = new Ledger();
                            LedgerContent ledgerContent = timesSnapshot.getValue(LedgerContent.class);
                            ledger.setClassfy(classfySnapshot.getKey());
                            ledger.setYear(yearSnapshot.getKey());
                            ledger.setMonth(monthSnapshot.getKey());
                            ledger.setDay(daySnapshot.getKey());
                            ledger.setTimes(timesSnapshot.getKey());
                            ledger.setPaymemo(ledgerContent.getPaymemo());
                            ledger.setPrice(ledgerContent.getPrice());
                            ledger.setUseItem(ledgerContent.getUseItem());

                            allLedgerData.add(ledger);
                            yearsAndMonthsHavingDataSet.add(ledger.getYear() + "년 " + ledger.getMonth() + "월");
                        }
                    }
                }
            }
        }
    }

    // 소비 카테고리 별 합계, 백분율을 구한 뒤
    // 차트의 속성을 설정한다.
    private void showChart() {
        if (allLedgerData.size() <= 0) {
            return;
        }

        // 소비 카테고리 별 합계를 구한다.
        for (int i = 0; i < allLedgerData.size(); i++) {
            if (allLedgerData.get(i).getUseItem().equals("의류비")) {
                clothPrice += Integer.parseInt(allLedgerData.get(i).getPrice());
            } else if (allLedgerData.get(i).getUseItem().equals("식비")) {
                foodPrice += Integer.parseInt(allLedgerData.get(i).getPrice());
            } else if (allLedgerData.get(i).getUseItem().equals("주거비")) {
                homePrice += Integer.parseInt(allLedgerData.get(i).getPrice());
            } else if (allLedgerData.get(i).getUseItem().equals("교통비")) {
                transPrice += Integer.parseInt(allLedgerData.get(i).getPrice());
            } else if (allLedgerData.get(i).getUseItem().equals("생필품")) {
                marketPrice += Integer.parseInt(allLedgerData.get(i).getPrice());
            } else if (allLedgerData.get(i).getUseItem().equals("기타")) {
                etcPrice += Integer.parseInt(allLedgerData.get(i).getPrice());
            }
        }

        // 백분율을 구한다.
        float total = clothPrice + foodPrice + homePrice + transPrice + marketPrice + etcPrice;
        float cloth = (clothPrice / total) * 100;
        float food = (foodPrice / total) * 100;
        float home = (homePrice / total) * 100;
        float trans = (transPrice / total) * 100;
        float market = (marketPrice / total) * 100;
        float etc = (etcPrice / total) * 100;

        // 0%가 아니라면 차트에 카테고리명을 표시한다.
        ArrayList<PieEntry> yValues = new ArrayList<>();
        if (cloth != 0) {
            yValues.add(new PieEntry(cloth, "의류비"));
        }
        if (food != 0) {
            yValues.add(new PieEntry(food, "식비"));
        }
        if (home != 0) {
            yValues.add(new PieEntry(home, "주거비"));
        }
        if (trans != 0) {
            yValues.add(new PieEntry(trans, "교통비"));
        }
        if (market != 0) {
            yValues.add(new PieEntry(market, "생필품"));
        }
        if (etc != 0) {
            yValues.add(new PieEntry(etc, "기타"));
        }

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
                PieEntry pe = (PieEntry) e;

                AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity());
                if (pe.getLabel().equals("의류비")) {
                    alertDialog.setMessage("의류비 총계 : " + (int)clothPrice + "원");
                } else if (pe.getLabel().equals("식비")) {
                    alertDialog.setMessage("식비 총계 : " + (int)foodPrice + "원");
                } else if (pe.getLabel().equals("주거비")) {
                    alertDialog.setMessage("주거비 총계 : " + (int)homePrice + "원");
                } else if (pe.getLabel().equals("교통비")) {
                    alertDialog.setMessage("교통비 총계 : " + (int)transPrice + "원");
                } else if (pe.getLabel().equals("생필품")) {
                    alertDialog.setMessage("생필품비 총계 : " + (int)marketPrice + "원");
                } else if (pe.getLabel().equals("기타")) {
                    alertDialog.setMessage("기타 비용 총계 : " + (int)etcPrice + "원");
                }

                alertDialog.create().show();
            }

            @Override
            public void onNothingSelected() {}
        });
    }

    // 사용자의 가계부 데이터를 불러와 차트를 출력한다.
    private void loadLedgerAndShowChart() {
        myRef.child(user.getUid()).child("Ledger").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // 사용자의 전체 가계부 목록을 불러온다.
                viewBinding.tvLedgerMonth2.setText("전체 가계부");
                loadLedger(dataSnapshot);

                // 가계부 전체 통계를 출력한다.
                showChart();

                // 년, 월만 빼서 따로 리스트 생성
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
}