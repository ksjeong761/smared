package kr.ac.kpu.block.smared;

import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

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

import kr.ac.kpu.block.smared.databinding.FragmentLedgerStatShareBinding;

import static android.content.ContentValues.TAG;

public class ShareLedgerStatFragment extends android.app.Fragment {
    private FormattedLogger logger = new FormattedLogger();
    private FragmentLedgerStatShareBinding viewBinding;

    private DatabaseReference chatRef;
    private FirebaseUser user;

    private int monthIndex = 0;  // 년,월 인덱스
    private Set<String> selectMonth = new HashSet<>(); // 년,월 중복제거용
    private List<String> monthList; // 중복 제거된 년,월 저장
    private List<Ledger> mLedger = new ArrayList<>();

    private String selectChatuid="";
    private String joinChatname;
    private String selectedChatRoomName = "";
    private ArrayAdapter<String> spinnerAdapter;
    private List<String> listItems = new ArrayList<>();

    private float clothPrice=0;
    private float foodPrice=0;
    private float transPrice=0;
    private float etcPrice=0;
    private float marketPrice=0;
    private float homePrice=0;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        viewBinding = FragmentLedgerStatShareBinding.inflate(inflater, container, false);

        chatRef = FirebaseDatabase.getInstance().getReference("chats");
        user = FirebaseAuth.getInstance().getCurrentUser();

        // 이전 달 버튼 이벤트
        viewBinding.ibLastMonth2.setOnClickListener(view -> {
            if (mLedger.size() <= 0) {
                return;
            }

            if (monthIndex != 0) { // 년,월이 제일 처음이 아니면
                monthIndex--;
            } else {   // 년,월이 처음이면
                monthIndex = monthList.size() - 1;
            }

            viewBinding.tvLedgerMonth2.setText(monthList.get(monthIndex));
            String parsing= monthList.get(monthIndex).replaceAll("[^0-9]", ""); // 날짜를 20182 이런형식으로 파싱

            List<Ledger> tempLedger = new ArrayList<>();
            for (int j=0; j<mLedger.size(); j++) {
                if( parsing.equals(mLedger.get(j).getYear() + mLedger.get(j).getMonth()) ) {
                    tempLedger.add(mLedger.get(j));
                }
            }

            selectChart();
        });

        // 다음 달 버튼 이벤트
        viewBinding.ibNextMonth2.setOnClickListener(view -> {
            if (mLedger.size() != 0) {
                return;
            }

            if (monthIndex != monthList.size() - 1) { // 년, 월이 마지막이 아니면
                monthIndex++;
            } else {   // 년,월이 마지막이면
                monthIndex = 0;
            }

            viewBinding.tvLedgerMonth2.setText(monthList.get(monthIndex));
            String parsing = monthList.get(monthIndex).replaceAll("[^0-9]", "");

            List<Ledger> tempLedger = new ArrayList<>();
            for (int j = 0; j < mLedger.size(); j++) {
                if (parsing.equals(mLedger.get(j).getYear() + mLedger.get(j).getMonth())) {
                    tempLedger.add(mLedger.get(j));
                }
            }

            selectChart();
        });

        viewLedgerName("init");

        // 스피너 선택 이벤트
        spinnerAdapter = new ArrayAdapter<>(getActivity(), R.layout.support_simple_spinner_dropdown_item, listItems);
        viewBinding.spnSelectLedger.setAdapter(spinnerAdapter);
        viewBinding.spnSelectLedger.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
              @Override
              public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                  selectedChatRoomName = (String)parent.getItemAtPosition(position);

                  mLedger.clear(); // 가계부 초기화
                  listItems.clear(); // 참여중인 가계부 목록 초기화
                  selectMonth.clear();
                  monthList.clear(); // 년,월 선택 초기화
                  viewLedgerName(selectedChatRoomName);
              }

              @Override
              public void onNothingSelected(AdapterView<?> parent) { }
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

    // 선택된 가계부 이름으로부터 가계부 키를 찾고 화면 출력
    public void setChatUid() {
        chatRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot chatSnapshot : dataSnapshot.getChildren()) {
                    if (!chatSnapshot.child("chatname").getValue(String.class).equals(selectedChatRoomName)) {
                        continue;
                    }

                    selectChatuid = chatSnapshot.getKey();

                    chatRef.child(selectChatuid).child("Ledger").addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            viewBinding.tvLedgerMonth2.setText("전체 가계부");
                            ledgerView(dataSnapshot); // 유저 가계부 전체 리스트 생성
                            selectChart();
                            monthList = new ArrayList(selectMonth); // 년 월만 빼서 따로 리스트 생성
                            Collections.sort(monthList);

                            if (!monthList.isEmpty()) {
                                monthIndex = monthList.size() - 1;
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError error) {
                            Log.w(TAG, "Failed to read value.", error.toException());
                        }
                    });
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) { }
        });
    }

    // 현재 참여중인 가계부 이름 읽어오기
    public void viewLedgerName(final CharSequence chatname) {
        chatRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot chatSnapshot : dataSnapshot.getChildren()) {
                    for (DataSnapshot userSnapshot : chatSnapshot.getChildren()) {
                        for (DataSnapshot uidSnapshot : userSnapshot.getChildren()) {
                            if (!uidSnapshot.getKey().equals(user.getUid())) {
                                continue;
                            }

                            joinChatname = chatSnapshot.child("chatname").getValue(String.class);
                            listItems.add(joinChatname);
                            spinnerAdapter.notifyDataSetChanged();
                            selectedChatRoomName = chatname.equals("init") ? listItems.get(0) : chatname.toString();
                        }
                    }
                }

                setChatUid();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) { }
        });
    }
}
