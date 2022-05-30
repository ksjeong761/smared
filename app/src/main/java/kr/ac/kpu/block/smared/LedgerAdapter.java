package kr.ac.kpu.block.smared;

import android.app.AlertDialog;
import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.List;

import kr.ac.kpu.block.smared.databinding.ListContentBinding;
import kr.ac.kpu.block.smared.databinding.ListLedgerBinding;

public class LedgerAdapter extends RecyclerView.Adapter<LedgerAdapter.ViewHolder> {
    private FormattedLogger logger = new FormattedLogger();

    private Context parentContext;
    private List<Ledger> ledgerData;

    private final int VIEW_TYPE_LIST_LEDGER = 1;
    private final int VIEW_TYPE_LIST_CONTENT = 2;

    // 리스트의 각 요소마다 뷰를 만들어서 뷰홀더에 저장해두는 것으로 findViewById가 매번 호출되는 것을 방지한다.
    public static class ViewHolder extends RecyclerView.ViewHolder {
        private Button btnDelete;
        private Button btnEdit;
        private Button btnDay;
        private TextView tvCategory;
        private TextView tvPrice;
        private TextView tvDescription;
        private TextView tvChoice;

        public ViewHolder(ListLedgerBinding viewBinding) {
            super(viewBinding.getRoot());
            this.btnDelete = viewBinding.btnDelete;
            this.btnEdit = viewBinding.btnEdit;
            this.btnDay = viewBinding.btnDay;
            this.tvCategory = viewBinding.tvCategory;
            this.tvPrice = viewBinding.tvPrice;
            this.tvDescription = viewBinding.tvDescription;
            this.tvChoice = viewBinding.tvChoice;
        }

        public ViewHolder(ListContentBinding viewBinding) {
            super(viewBinding.getRoot());
            this.btnDelete = viewBinding.btnDelete;
            this.btnEdit = viewBinding.btnEdit;
            this.tvCategory = viewBinding.tvCategory;
            this.tvPrice = viewBinding.tvPrice;
            this.tvDescription = viewBinding.tvDescription;
            this.tvChoice = viewBinding.tvChoice;
        }
    }

    public LedgerAdapter(List<Ledger> ledgerData , Context parentContext) {
        this.ledgerData = ledgerData;
        this.parentContext = parentContext;
    }

    // ViewHolder를 새로 만들어야 할 때 호출되는 메서드이다.
    // 이 메서드를 통해 각 아이템을 위한 XML 레이아웃을 이용한 뷰 객체를 생성하고 뷰 홀더에 담아 리턴한다.
    // 이때는 뷰의 콘텐츠를 채우지 않는다. 왜냐하면 아직 ViewHolder가 특정 데이터에 바인딩된 상태가 아니기 때문이다.
    @Override
    public LedgerAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        ViewHolder listLedgerViewHolder = new ViewHolder(ListLedgerBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
        ViewHolder listContentViewHolder = new ViewHolder(ListContentBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));

        return (viewType == VIEW_TYPE_LIST_LEDGER) ? listLedgerViewHolder : listContentViewHolder;
    }

    // ViewHolder를 데이터와 연결할 때 호출되는 메서드이다.
    // 이 메서드를 통해 뷰홀더의 레이아웃을 채우게 된다.
    @Override
    public void onBindViewHolder(ViewHolder viewHolder, final int index) {
        if (hasDifferentDate(ledgerData, index, index-1)) {
            viewHolder.btnDay.setText(ledgerData.get(index).getPaymentTimestamp("yyyy-MM-dd"));
        }

        viewHolder.tvCategory.setText("소비 분류 : " + ledgerData.get(index).getCategory());
        viewHolder.tvPrice.setText("총 소비 금액 : " + ledgerData.get(index).getTotalPrice() + "원");
        viewHolder.tvDescription.setText("비고 : " + ledgerData.get(index).getDescription());

        // 수정 버튼
        viewHolder.btnEdit.setOnClickListener(view -> new LedgerEditDialog(parentContext, ledgerData, index).show());

        // 삭제 버튼
        viewHolder.btnDelete.setOnClickListener(view -> {
            AlertDialog.Builder alertDialog = new AlertDialog.Builder(parentContext);
            alertDialog.setMessage("정말 삭제 하시겠습니까?");

            // 확인 버튼
            alertDialog.setPositiveButton("확인", (dialog, which) -> {
                // 가계부에서 선택한 데이터 삭제
                DatabaseReference myRef = FirebaseDatabase.getInstance().getReference("ledger");
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                myRef.child(user.getUid())
                        .child(String.valueOf(ledgerData.get(index).getPaymentTimestamp()))
                        .removeValue();

                Toast.makeText(parentContext, "삭제되었습니다", Toast.LENGTH_SHORT).show();
            });

            // 취소 버튼
            alertDialog.setNegativeButton("취소", (dialog, which) -> { });

            // 알럿 다이얼로그 생성
            alertDialog.create().show();
        });
    }

    @Override
    public int getItemCount() {
        return ledgerData.size();
    }

    @Override
    public int getItemViewType(int position) {
        return (hasDifferentDate(ledgerData, position, position-1)) ? VIEW_TYPE_LIST_CONTENT : VIEW_TYPE_LIST_LEDGER;
    }

    private boolean hasDifferentDate(List<Ledger> ledger, int indexA, int indexB) {
        if (indexA < 0 || indexA >= ledger.size())
            return true;
        if (indexB < 0 || indexB >= ledger.size())
            return true;

        return (ledger.get(indexA).compareDate(ledger.get(indexB)) != 0);
    }
}