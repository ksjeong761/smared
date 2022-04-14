package kr.ac.kpu.block.smared;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Hashtable;

// 가계부 기록 화면
public class LedgerRegFragment extends android.app.Fragment {
    // [Refactoring] 값을 사용하지 않는다.
    Context context;

    //데이터베이스 관련
    FirebaseDatabase database;
    DatabaseReference myRef;
    FirebaseUser user;

    //사용자로부터 입력받을 값들
    String stUseItem;
    String stPrice;
    String stPaymemo;

    //사용자로부터 입력받을 날짜
    Calendar c = Calendar.getInstance();
    SimpleDateFormat years = new SimpleDateFormat("yyyy");
    SimpleDateFormat months = new SimpleDateFormat("MM");
    SimpleDateFormat days = new SimpleDateFormat("dd");
    String stYear = years.format(c.getTime());
    String stMonth = months.format(c.getTime());
    String stDay = days.format(c.getTime());

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // 사용자 정보 DB에 접근하기 위한 객체
        database = FirebaseDatabase.getInstance();
        myRef = database.getReference("users");

        // 현재 로그인한 사용자 가져오기
        user = FirebaseAuth.getInstance().getCurrentUser();

        // Fragment를 화면에 출력하기 위해 뷰를 생성한다.
        View v = inflater.inflate(R.layout.fragment_ledger_reg, container, false);

        final Spinner spnUseitem = v.findViewById(R.id.spnUseitem);
        final EditText etPrice = v.findViewById(R.id.etPrice);
        final EditText etPaymemo = v.findViewById(R.id.etPaymemo);
        CalendarView cvCalender = v.findViewById(R.id.cvCalender);
        final RadioButton rbConsume = v.findViewById(R.id.rbConsume);
        Button btnSave = v.findViewById(R.id.btnSave);
        Button btnOcr = v.findViewById(R.id.btnOcr);
        Button btnSMS = v.findViewById(R.id.btnSMS);

        // 드롭다운 메뉴(소비영역 분류) 선택 이벤트 - 선택된 값을 저장한다.
        spnUseitem.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                stUseItem = (String) adapterView.getItemAtPosition(i);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {}
        });

        //날짜 선택 이벤트 - 선택된 날짜를 저장하고 사용자에게 텍스트로 보여준다.
        cvCalender.setOnDateChangeListener((calendarView, year, month, day) -> {
            stYear = Integer.toString(year);
            stMonth = String.format("%02d",month+1);
            stDay =  String.format("%02d",day);

            Toast.makeText(getActivity(), stYear+"-"+stMonth+"-"+stDay, Toast.LENGTH_SHORT).show();
        });

        //저장 버튼 이벤트 - UI에 정보가 모두 입력되었다면 DB에 저장한다.
        btnSave.setOnClickListener(view -> {
            stPrice = etPrice.getText().toString();
            stPaymemo = etPaymemo.getText().toString();
            c = Calendar.getInstance();
            SimpleDateFormat time = new SimpleDateFormat("HHmmss");
            String stTime = time.format(c.getTime());

            // HashTable로 연결
            Hashtable<String, String> ledger = new Hashtable<String, String>();
            ledger.put("useItem", stUseItem);
            ledger.put("price", stPrice);
            ledger.put("paymemo",stPaymemo);

            if (stPrice.isEmpty()) {
                Toast.makeText(getActivity(), "금액란을 채워주세요", Toast.LENGTH_SHORT).show();
            } else {
                if (rbConsume.isChecked()) {
                    myRef.child(user.getUid()).child("Ledger").child(stYear).child(stMonth).child(stDay).child("지출").child(stTime).setValue(ledger);
                    Toast.makeText(getActivity(), "저장하였습니다.", Toast.LENGTH_SHORT).show();
                } else {
                    myRef.child(user.getUid()).child("Ledger").child(stYear).child(stMonth).child(stDay).child("수입").child(stTime).setValue(ledger);
                    Toast.makeText(getActivity(), "저장하였습니다.", Toast.LENGTH_SHORT).show();
                }
            }

            etPrice.setText("");
            etPaymemo.setText("");
        });

        // OCR 버튼 이벤트 - ImageActivity로 이동한다.
        btnOcr.setOnClickListener(view -> {
            Intent intent = new Intent(getActivity(), ImageActivity.class);
            startActivity(intent);
        });

        // SMS 버튼 이벤트 - SMSActivity로 이동한다.
        btnSMS.setOnClickListener(view -> {
            Intent intent = new Intent(getActivity(), SMSActivity.class);
            startActivity(intent);
        });

        return v;
    }
}
