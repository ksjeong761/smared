package kr.ac.kpu.block.smared;

import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.PieChart;
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
    private FragmentLedgerStatBinding viewBinding;

    int index = 0;  // 년,월 인덱스
    Set<String> selectMonth = new HashSet<>(); // 년,월 중복제거용
    List<String> monthList; // 중복 제거된 년,월 저장
    List<Ledger> mLedger = new ArrayList<>();

    float clothPrice = 0;
    float foodPrice = 0;
    float transPrice = 0;
    float etcPrice = 0;
    float marketPrice = 0;
    float homePrice = 0;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        viewBinding = FragmentLedgerStatBinding.inflate(inflater, container, false);

        DatabaseReference myRef = FirebaseDatabase.getInstance().getReference("users");
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        myRef.child(user.getUid()).child("Ledger").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                ledgerView(dataSnapshot);
                viewBinding.tvLedgerMonth2.setText("전체 가계부");
                selectChart();
                monthList = new ArrayList(selectMonth); // 년 월만 빼서 따로 리스트 생성
                Collections.sort(monthList);

                if (!monthList.isEmpty()) {
                    index = monthList.size() - 1;
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) { }
        });

        viewBinding.ibLastMonth2.setOnClickListener(view -> {
            if (mLedger.size() <= 0) {
                return;
            }

            if (index != 0) { // 년,월이 제일 처음이 아니면
                index--;
            } else {   // 년,월이 처음이면
                index = monthList.size() - 1;
            }

            viewBinding.tvLedgerMonth2.setText(monthList.get(index));
            String parsing = monthList.get(index).replaceAll("[^0-9]", "");

            List<Ledger> tempLedger = new ArrayList<>();
            for (int j = 0; j < mLedger.size(); j++) {
                if (parsing.equals(mLedger.get(j).getYear() + mLedger.get(j).getMonth())) {
                    tempLedger.add(mLedger.get(j));
                }
            }

            selectChart();
        });

        viewBinding.ibNextMonth2.setOnClickListener(view -> {
            if (mLedger.size() <= 0) {
                return;
            }

            if (index != monthList.size() - 1) { // 년, 월이 마지막이 아니면
                index++;
            } else {   // 년,월이 마지막이면
                index = 0;
            }

            viewBinding.tvLedgerMonth2.setText(monthList.get(index));
            String parsing = monthList.get(index).replaceAll("[^0-9]", "");

            List<Ledger> tempLedger = new ArrayList<>();
            for (int j = 0; j < mLedger.size(); j++) {
                if (parsing.equals(mLedger.get(j).getYear() + mLedger.get(j).getMonth())) {
                    tempLedger.add(mLedger.get(j));
                }
            }

            selectChart();
        });

        return viewBinding.getRoot();
    }

    public void ledgerView(DataSnapshot dataSnapshot) {
        for (DataSnapshot yearSnapshot : dataSnapshot.getChildren()) { // 년
            for (DataSnapshot monthSnapshot : yearSnapshot.getChildren()) { // 월
                for (DataSnapshot daySnapshot : monthSnapshot.getChildren()) { // 일
                    for (DataSnapshot classfySnapshot : daySnapshot.getChildren()) { // 분류
                        for (DataSnapshot timesSnapshot : classfySnapshot.getChildren()) { //
                            LedgerContent ledgerContent = timesSnapshot.getValue(LedgerContent.class);

                            Ledger ledger = new Ledger();
                            ledger.setClassfy(classfySnapshot.getKey());
                            ledger.setYear(yearSnapshot.getKey());
                            ledger.setMonth(monthSnapshot.getKey());
                            selectMonth.add(ledger.getYear() + "년 " + ledger.getMonth() + "월");
                            ledger.setDay(daySnapshot.getKey());
                            ledger.setTimes(timesSnapshot.getKey());
                            ledger.setPaymemo(ledgerContent.getPaymemo());
                            ledger.setPrice(ledgerContent.getPrice());
                            ledger.setUseItem(ledgerContent.getUseItem());

                            mLedger.add(ledger);
                        }
                    }
                }
            }
        }
    }

    public void selectChart() {
        for (int j = 0; j < mLedger.size(); j++) {
            if (mLedger.get(j).getUseItem().equals("의류비")) {
                clothPrice += Integer.parseInt(mLedger.get(j).getPrice());
            } else if (mLedger.get(j).getUseItem().equals("식비")) {
                foodPrice += Integer.parseInt(mLedger.get(j).getPrice());
            } else if (mLedger.get(j).getUseItem().equals("주거비")) {
                homePrice += Integer.parseInt(mLedger.get(j).getPrice());
            } else if (mLedger.get(j).getUseItem().equals("교통비")) {
                transPrice += Integer.parseInt(mLedger.get(j).getPrice());
            } else if (mLedger.get(j).getUseItem().equals("생필품")) {
                marketPrice += Integer.parseInt(mLedger.get(j).getPrice());
            } else if (mLedger.get(j).getUseItem().equals("기타")) {
                etcPrice += Integer.parseInt(mLedger.get(j).getPrice());
            }
        }

        float total = clothPrice + foodPrice + homePrice + transPrice + marketPrice + etcPrice;
        float cloth = (clothPrice / total) * 100;
        float food = (foodPrice / total) * 100;
        float home = (homePrice / total) * 100;
        float trans = (transPrice / total) * 100;
        float market = (marketPrice / total) * 100;
        float etc = (etcPrice / total) * 100;

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

        PieDataSet dataSet = new PieDataSet(yValues,"");
        dataSet.setSliceSpace(3f);
        dataSet.setSelectionShift(5f);
        dataSet.setColors(ColorTemplate.JOYFUL_COLORS);

        PieData data = new PieData((dataSet));
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
}
