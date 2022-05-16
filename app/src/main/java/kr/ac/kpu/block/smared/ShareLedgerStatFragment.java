package kr.ac.kpu.block.smared;

import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
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

public class ShareLedgerStatFragment extends android.app.Fragment {
    private FormattedLogger logger = new FormattedLogger();
    private FragmentLedgerStatShareBinding viewBinding;

    private DatabaseReference chatRef;
    private FirebaseUser user;

    // 현재 참여중인 채팅방 목록
    private List<String> joiningChatRooms = new ArrayList<>();

    // 통계를 정리하기 위해 가계부 전체 데이터를 가져온다.
    private List<Ledger> allLedgerData = new ArrayList<>();

    // 데이터가 존재하는 년,월만을 기록하여 월별 통계를 조회할 수 있도록 한다.
    private Set<String> yearsAndMonthsHavingDataSet = new HashSet<>(); // 중복제거용 Set
    private List<String> yearsAndMonthsHavingDataList = new ArrayList<>(); // 데이터가 존재하는 년,월만을 기록
    private int yearsAndMonthsIndex = 0; // 년,월 인덱스

    // 카테고리 별 소비 합계를 저장해두고 차트 클릭 시 출력한다.
    private float clothPrice = 0;
    private float foodPrice = 0;
    private float transPrice = 0;
    private float marketPrice = 0;
    private float homePrice = 0;
    private float etcPrice = 0;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        viewBinding = FragmentLedgerStatShareBinding.inflate(inflater, container, false);

        chatRef = FirebaseDatabase.getInstance().getReference("chats");
        user = FirebaseAuth.getInstance().getCurrentUser();

        // 스피너에 출력할 채팅방 목록을 불러온다.
        loadJoiningChatRooms();

        // 스피너 목록 중 가장 위에 있는 채팅방의 가계부 데이터를 불러와 차트를 출력한다.
        if (!joiningChatRooms.isEmpty()) {
            loadLedgerAndShowChart(joiningChatRooms.get(0));
        }

        // 채팅방 목록을 스피너에 출력한다.
        viewBinding.spnSelectLedger.setAdapter(new ArrayAdapter<>(getActivity(), R.layout.support_simple_spinner_dropdown_item, joiningChatRooms));
        viewBinding.spnSelectLedger.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            // 스피너 선택 이벤트 - 선택한 채팅방의 가계부 데이터를 불러와 차트를 출력한다.
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                allLedgerData.clear();
                yearsAndMonthsHavingDataSet.clear();
                yearsAndMonthsHavingDataList.clear();

                loadLedgerAndShowChart(joiningChatRooms.get(position));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });

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

    // 현재 사용자가 참여중인 채팅방 목록을 DB에서 읽어온다.
    private void loadJoiningChatRooms() {
        joiningChatRooms.clear();
        chatRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot chatSnapshot : dataSnapshot.getChildren()) {
                    for (DataSnapshot userSnapshot : chatSnapshot.getChildren()) {
                        for (DataSnapshot uidSnapshot : userSnapshot.getChildren()) {
                            if (uidSnapshot.getKey().equals(user.getUid())) {
                                joiningChatRooms.add(chatSnapshot.child("chatname").getValue(String.class));
                            }
                        }
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                logger.writeLog("Failed to read value : " + error.toException().getMessage());
            }
        });
    }

    // 사용자의 전체 가계부 내역을 읽어온다.
    private void loadLedger(DataSnapshot dataSnapshot) {
        allLedgerData.clear();
        yearsAndMonthsHavingDataSet.clear();
        for (DataSnapshot yearSnapshot : dataSnapshot.getChildren()) { // 년
            for (DataSnapshot monthSnapshot : yearSnapshot.getChildren()) { // 월
                for (DataSnapshot daySnapshot : monthSnapshot.getChildren()) { // 일
                    for (DataSnapshot classifySnapshot : daySnapshot.getChildren()) { // 분류
                        for (DataSnapshot timesSnapshot : classifySnapshot.getChildren()) { // 시간
                            Ledger ledger = new Ledger();
                            LedgerContent ledgerContent = timesSnapshot.getValue(LedgerContent.class);
                            ledger.setClassify(classifySnapshot.getKey()); // 분류
                            ledger.setYear(yearSnapshot.getKey()); // 년
                            ledger.setMonth(monthSnapshot.getKey()); // 월
                            ledger.setDay(daySnapshot.getKey()); // 일
                            ledger.setTimes(timesSnapshot.getKey()); // 시간
                            ledger.setLedgerContent(ledgerContent); // 내용, 금액, 물품 분류

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
        for (int ledgerDataIndex = 0; ledgerDataIndex < allLedgerData.size(); ledgerDataIndex++) {
            int price = Integer.parseInt(allLedgerData.get(ledgerDataIndex).getLedgerContent().getPrice());
            String category = allLedgerData.get(ledgerDataIndex).getLedgerContent().getCategory();
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

    // 선택한 채팅방의 가계부 데이터를 불러와 차트를 출력한다.
    private void loadLedgerAndShowChart(String selectedChatRoomName) {
        chatRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // 채팅방 이름으로 uid를 조회한다.
                String selectedChatRoomUid = "";
                for (DataSnapshot chatSnapshot : dataSnapshot.getChildren()) {
                    if (chatSnapshot.child("chatname").getValue(String.class).equals(selectedChatRoomName)) {
                        selectedChatRoomUid = chatSnapshot.getKey();
                        break;
                    }
                }

                // 가계부 목록을 불러와 통계를 출력한다.
                chatRef.child(selectedChatRoomUid).child("Ledger").addListenerForSingleValueEvent(new ValueEventListener() {
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

            @Override
            public void onCancelled(DatabaseError databaseError) { }
        });
    }
}