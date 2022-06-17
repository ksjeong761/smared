package kr.ac.kpu.block.smared;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import java.util.Calendar;

import kr.ac.kpu.block.smared.databinding.FragmentLedgerRegBinding;

// 가계부 기록 화면
public class LedgerRegFragment extends android.app.Fragment {
    private FormattedLogger logger = new FormattedLogger();
    private FragmentLedgerRegBinding viewBinding;

    private Calendar selectedDate = Calendar.getInstance(); //사용자가 선택한 날짜

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        viewBinding = FragmentLedgerRegBinding.inflate(inflater, container, false);

        // UI 이벤트 등록
        viewBinding.cvCalender.setOnDateChangeListener((calendarView, year, month, day) -> selectedDate.set(year, month, day));
        viewBinding.btnOCR.setOnClickListener(view -> startActivity(new Intent(getActivity(), OCRImageLoadActivity.class)));
        viewBinding.btnSMS.setOnClickListener(view -> new SMSParser().parseAllSMS(this.getContext()));
        viewBinding.btnSave.setOnClickListener(view -> insertLedgerToDB());

        return viewBinding.getRoot();
    }

    // UI에 입력된 정보를 모아 가계부 객체를 만든다.
    private Ledger gatherLedgerDataFromUI() {
        if (viewBinding.etTotalPrice.getText().toString().isEmpty()) {
            Toast.makeText(getActivity(), "금액란을 채워주세요", Toast.LENGTH_SHORT).show();
            return null;
        }

        Ledger ledger = new Ledger();
        ledger.setDescription(viewBinding.etDescription.getText().toString());
        ledger.setTotalPrice(Double.parseDouble(viewBinding.etTotalPrice.getText().toString()));
        ledger.setTotalCategory(viewBinding.spnCategory.getSelectedItem().toString());
        ledger.setPaymentTimestamp(selectedDate.getTimeInMillis());

        return ledger;
    }

    // 가계부 객체를 DB에 삽입한다.
    private void insertLedgerToDB() {
        Ledger ledger = gatherLedgerDataFromUI();
        if (ledger == null) {
            return;
        }

        DAO dao = new DAO();
        dao.setSuccessCallback(arg -> afterSuccess());
        dao.setFailureCallback(arg -> afterFailure());
        dao.create(ledger, Ledger.class);
    }

    // DB 삽입 성공 시 동작
    private void afterSuccess() {
        Toast.makeText(getActivity(), "저장하였습니다.", Toast.LENGTH_SHORT).show();
        viewBinding.etTotalPrice.setText("");
        viewBinding.etDescription.setText("");
    }

    // DB 삽입 실패 시 동작
    private void afterFailure() {
        Toast.makeText(getActivity(), "저장에 실패하였습니다.", Toast.LENGTH_SHORT).show();
    }
}
