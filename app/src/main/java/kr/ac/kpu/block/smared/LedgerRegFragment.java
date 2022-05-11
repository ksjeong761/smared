package kr.ac.kpu.block.smared;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Map;

import kr.ac.kpu.block.smared.databinding.FragmentLedgerRegBinding;

// 가계부 기록 화면
public class LedgerRegFragment extends android.app.Fragment {
    private FormattedLogger logger = new FormattedLogger();
    private FragmentLedgerRegBinding viewBinding;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        viewBinding = FragmentLedgerRegBinding.inflate(inflater, container, false);

        DatabaseReference myRef = FirebaseDatabase.getInstance().getReference("users");
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        // 저장 버튼 이벤트 - UI에 정보가 모두 입력되었다면 DB에 저장한다.
        viewBinding.btnSave.setOnClickListener(view -> {
            if (viewBinding.etPrice.getText().toString().isEmpty()) {
                Toast.makeText(getActivity(), "금액란을 채워주세요", Toast.LENGTH_SHORT).show();
                return;
            }

            // DB 삽입용 Hashtable DTO
            String usedItem = viewBinding.spnUseitem.getSelectedItem().toString();
            String stPrice = viewBinding.etPrice.getText().toString();
            String stPayMemo = viewBinding.etPaymemo.getText().toString();
            Map<String, String> ledger = new LedgerContent(usedItem, stPrice, stPayMemo).toHashMap();

            // 삽입할 DB 경로 지정
            long selectedDate = viewBinding.cvCalender.getDate();
            String stYear = new SimpleDateFormat("yyyy").format(selectedDate);
            String stMonth = new SimpleDateFormat("MM").format(selectedDate);
            String stDay = new SimpleDateFormat("dd").format(selectedDate);
            String tableName = (viewBinding.rbConsume.isChecked()) ? "지출" : "수입";
            String stTime = new SimpleDateFormat("HHmmss").format(Calendar.getInstance().getTime());

            // DB 삽입
            myRef.child(user.getUid())
                    .child("Ledger")
                    .child(stYear)
                    .child(stMonth)
                    .child(stDay)
                    .child(tableName)
                    .child(stTime)
                    .setValue(ledger);

            // 입력 받는 부분 UI 초기화
            viewBinding.etPrice.setText("");
            viewBinding.etPaymemo.setText("");
            Toast.makeText(getActivity(), "저장하였습니다.", Toast.LENGTH_SHORT).show();
        });

        // OCR 버튼 이벤트 - ImageActivity로 이동한다.
        viewBinding.btnOcr.setOnClickListener(view -> startActivity(new Intent(getActivity(), ImageActivity.class)));

        // SMS 버튼 이벤트 - SMSActivity로 이동한다.
        viewBinding.btnSMS.setOnClickListener(view -> startActivity(new Intent(getActivity(), SMSActivity.class)));

        return viewBinding.getRoot();
    }
}
