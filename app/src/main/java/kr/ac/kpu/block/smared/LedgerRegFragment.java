package kr.ac.kpu.block.smared;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CalendarView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.TimeZone;

import kr.ac.kpu.block.smared.databinding.FragmentLedgerRegBinding;

// 가계부 기록 화면
public class LedgerRegFragment extends android.app.Fragment {
    private FormattedLogger logger = new FormattedLogger();
    private FragmentLedgerRegBinding viewBinding;

    Calendar selectedDate = Calendar.getInstance();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        viewBinding = FragmentLedgerRegBinding.inflate(inflater, container, false);

        viewBinding.cvCalender.setOnDateChangeListener((calendarView, year, month, day) -> selectedDate.set(year, month, day));
        viewBinding.btnSaveLedger.setOnClickListener(view -> insertLedgerDB());
        viewBinding.btnOCR.setOnClickListener(view -> startActivity(new Intent(getActivity(), OCRImageLoadActivity.class)));
        viewBinding.btnSMS.setOnClickListener(view -> startActivity(new Intent(getActivity(), SMSActivity.class)));

        return viewBinding.getRoot();
    }

    public void insertLedgerDB() {
        if (viewBinding.etTotalPrice.getText().toString().isEmpty()) {
            Toast.makeText(getActivity(), "금액란을 채워주세요", Toast.LENGTH_SHORT).show();
            return;
        }

        // 사용자가 입력한 데이터를 객체에 반영한다.
        Ledger ledger = new Ledger();
        ledger.setDescription(viewBinding.etDescription.getText().toString());
        ledger.setTotalPrice(Double.parseDouble(viewBinding.etTotalPrice.getText().toString()));
        ledger.setCategory(viewBinding.spnCategory.getSelectedItem().toString());
        ledger.setPaymentTimestamp(selectedDate.getTimeInMillis());

        // DB 경로를 지정한다.
        String timestamp = String.valueOf(ledger.getPaymentTimestamp());
        String userUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String databasePath = "ledger" + "/" + userUid + "/" + timestamp;
        DatabaseReference ledgerDBRef = FirebaseDatabase.getInstance().getReference(databasePath);

        // DB에 데이터를 저장한다.
        ledgerDBRef.setValue(ledger.toMap()).addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                Toast.makeText(getActivity(), "저장에 실패하였습니다.", Toast.LENGTH_SHORT).show();
                return;
            }

            Toast.makeText(getActivity(), "저장하였습니다.", Toast.LENGTH_SHORT).show();
            viewBinding.etTotalPrice.setText("");
            viewBinding.etDescription.setText("");
        });
    }
}
