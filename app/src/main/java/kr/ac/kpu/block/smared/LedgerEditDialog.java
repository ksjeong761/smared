package kr.ac.kpu.block.smared;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.Window;
import android.widget.Toast;

import java.util.List;

import kr.ac.kpu.block.smared.databinding.DialogLedgerEditBinding;

public class LedgerEditDialog extends Dialog {
    private FormattedLogger logger = new FormattedLogger();
    private DialogLedgerEditBinding viewBinding;

    private List<Ledger> ledgerData;
    private int selectedLedgerIndex;

    public LedgerEditDialog(Context context, List<Ledger> ledgerData, int selectedLedgerIndex) {
        super(context);
        this.ledgerData = ledgerData;
        this.selectedLedgerIndex = selectedLedgerIndex;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewBinding = DialogLedgerEditBinding.inflate(getLayoutInflater());
        setContentView(viewBinding.getRoot());
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        // UI 초기화
        viewBinding.etDate.setText(ledgerData.get(selectedLedgerIndex).getFormattedTimestamp("yyyy-MM-dd"));
        viewBinding.etTotalPrice.setText(String.valueOf(ledgerData.get(selectedLedgerIndex).getTotalPrice()));
        viewBinding.etDescription.setText(ledgerData.get(selectedLedgerIndex).getDescription());
        int categoryIndex = ledgerData.get(selectedLedgerIndex).getCategoryIndex();
        if (categoryIndex >= 0) {
            viewBinding.spnCategory.setSelection(categoryIndex);
        }

        // 이벤트 등록
        viewBinding.btnSubmit.setOnClickListener(view -> updateLedgerDB());
        viewBinding.btnCancel.setOnClickListener(view -> dismiss());
    }

    private void updateLedgerDB() {
        // 불러온 기존 내역을 수정한다.
        Ledger ledger = ledgerData.get(selectedLedgerIndex);
        ledger.setTotalCategory(viewBinding.spnCategory.getSelectedItem().toString());
        ledger.setTotalPrice(Double.parseDouble(viewBinding.etTotalPrice.getText().toString()));
        ledger.setDescription(viewBinding.etDescription.getText().toString());

        DAO dao = new DAO();
        dao.setSuccessCallback(arg -> afterSuccess());
        dao.setFailureCallback(arg -> afterFailure());
        dao.update(ledger, Ledger.class);

        dismiss();
    }

    private void afterSuccess() {
        Toast.makeText(getContext(), "가계부가 수정되었습니다", Toast.LENGTH_SHORT).show();
    }

    private void afterFailure() {
        Toast.makeText(getContext(), "가계부 수정에 실패하였습니다.", Toast.LENGTH_SHORT).show();
    }
}