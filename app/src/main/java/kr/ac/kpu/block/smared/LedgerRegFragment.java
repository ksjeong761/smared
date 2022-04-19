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

import kr.ac.kpu.block.smared.databinding.FragmentLedgerRegBinding;

// 가계부 기록 화면
public class LedgerRegFragment extends android.app.Fragment {
    private FragmentLedgerRegBinding viewBinding;
    private FormattedLogger logger = new FormattedLogger();

    // [Refactor] 이 전역변수들이 처리하기 곤란한 이유는 값이 바뀔 때 이벤트를 걸어서 값을 저장하기 때문임.
    // 값이 바뀔 때 반응하지 말고 필요할 때 UI 컴포넌트에서 값을 읽어오도록 변경할 필요 있음
    //사용자로부터 입력받을 값들
    String stUseItem;

    //사용자로부터 입력받을 날짜
    Calendar c = Calendar.getInstance();
    String stYear = new SimpleDateFormat("yyyy").format(c.getTime());
    String stMonth = new SimpleDateFormat("MM").format(c.getTime());
    String stDay = new SimpleDateFormat("dd").format(c.getTime());

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        logger.writeLog("viewBinding before");
        viewBinding = FragmentLedgerRegBinding.inflate(inflater, container, false);
        logger.writeLog("viewBinding after");

        // 사용자 정보 DB에 접근하기 위한 객체
        DatabaseReference myRef = FirebaseDatabase.getInstance().getReference("users");

        // 현재 로그인한 사용자 가져오기
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        // 드롭다운 메뉴(소비영역 분류) 선택 이벤트 - 선택된 값을 저장한다.
        viewBinding.spnUseitem.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                stUseItem = (String) adapterView.getItemAtPosition(i);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {}
        });

        //날짜 선택 이벤트 - 선택된 날짜를 저장하고 사용자에게 텍스트로 보여준다.
        viewBinding.cvCalender.setOnDateChangeListener((calendarView, year, month, day) -> {
            stYear = Integer.toString(year);
            stMonth = String.format("%02d",month+1);
            stDay =  String.format("%02d",day);

            Toast.makeText(getActivity(), stYear + "-" + stMonth + "-" + stDay, Toast.LENGTH_SHORT).show();
        });

        //저장 버튼 이벤트 - UI에 정보가 모두 입력되었다면 DB에 저장한다.
        viewBinding.btnSave.setOnClickListener(view -> {
            String stPrice = viewBinding.etPrice.getText().toString();
            String stPaymemo = viewBinding.etPaymemo.getText().toString();
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
                return;
            }

            String tableName = (viewBinding.rbConsume.isChecked()) ? "지출" : "수입";
            myRef.child(user.getUid()).child("Ledger").child(stYear).child(stMonth).child(stDay).child(tableName).child(stTime).setValue(ledger);
            Toast.makeText(getActivity(), "저장하였습니다.", Toast.LENGTH_SHORT).show();
            viewBinding.etPrice.setText("");
            viewBinding.etPaymemo.setText("");
        });

        // OCR 버튼 이벤트 - ImageActivity로 이동한다.
        viewBinding.btnOcr.setOnClickListener(view ->startActivity(new Intent(getActivity(), ImageActivity.class)));

        // SMS 버튼 이벤트 - SMSActivity로 이동한다.
        viewBinding.btnSMS.setOnClickListener(view -> startActivity(new Intent(getActivity(), SMSActivity.class)));

        return viewBinding.getRoot();
    }
}
