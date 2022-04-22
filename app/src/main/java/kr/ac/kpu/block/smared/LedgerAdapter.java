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

    // 데이터베이스 관련
    private FirebaseUser user;
    private DatabaseReference myRef;

    private Context context;
    private List<Ledger> mLedger;
    private String selectChatuid = "";

    // 리스트의 각 요소마다 뷰를 만들어서 뷰홀더에 저장해두는 것으로 findViewById가 매번 호출되는 것을 방지한다.
    public static class ViewHolder extends RecyclerView.ViewHolder {
        public Button btnDelete;
        public Button btnEdit;
        public Button btnDay;
        public TextView tvUseitem;
        public TextView tvPrice;
        public TextView tvPaymemo;
        public TextView tvChoice;

        public ViewHolder(ListLedgerBinding viewBinding) {
            super(viewBinding.getRoot());
            this.btnDelete = viewBinding.btnDelete;
            this.btnEdit = viewBinding.btnEdit;
            this.btnDay = viewBinding.btnDay;
            this.tvUseitem = viewBinding.tvUseitem;
            this.tvPrice = viewBinding.tvPrice;
            this.tvPaymemo = viewBinding.tvPaymemo;
            this.tvChoice = viewBinding.tvChoice;
        }

        public ViewHolder(ListContentBinding viewBinding) {
            super(viewBinding.getRoot());
            this.btnDelete = viewBinding.btnDelete;
            this.btnEdit = viewBinding.btnEdit;
            this.tvUseitem = viewBinding.tvUseitem;
            this.tvPrice = viewBinding.tvPrice;
            this.tvPaymemo = viewBinding.tvPaymemo;
            this.tvChoice = viewBinding.tvChoice;
        }
    }

    public LedgerAdapter(List<Ledger> mLedger , Context context, String selectChatuid) {
        this.mLedger = mLedger;
        this.context = context;
        this.selectChatuid = selectChatuid;
    }

    // 목록에 날짜가 다른 요소가 있다면 뷰 타입은 2이고 다른 경우는 1이다.
    @Override
    public int getItemViewType(int position) {
        return hasDifferentDate(mLedger, position, position-1) ? 2 : 1;
    }

    // ViewHolder를 새로 만들어야 할 때 호출되는 메서드이다.
    // 이 메서드를 통해 각 아이템을 위한 XML 레이아웃을 이용한 뷰 객체를 생성하고 뷰 홀더에 담아 리턴한다.
    // 이때는 뷰의 콘텐츠를 채우지 않는다. 왜냐하면 아직 ViewHolder가 특정 데이터에 바인딩된 상태가 아니기 때문이다.
    @Override
    public LedgerAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        myRef = FirebaseDatabase.getInstance().getReference("users");
        user = FirebaseAuth.getInstance().getCurrentUser();

        ListLedgerBinding viewBinding = ListLedgerBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        ListContentBinding viewBinding2 = ListContentBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return (viewType == 1) ? new ViewHolder(viewBinding) : new ViewHolder(viewBinding2);
    }

    // ViewHolder를 데이터와 연결할 때 호출되는 메서드이다.
    // 이 메서드를 통해 뷰홀더의 레이아웃을 채우게 된다.
    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {
        if (hasDifferentDate(mLedger, position, position-1)) {
            String buttonText = mLedger.get(position).getYear() + "-" + mLedger.get(position).getMonth() + "-" + mLedger.get(position).getDay();
            holder.btnDay.setText(buttonText);
        }

        holder.tvChoice.setText("[ " + mLedger.get(position).getClassfy() + " ]");
        holder.tvUseitem.setText("분류 : " + mLedger.get(position).getUseItem());
        holder.tvPrice.setText("가격 : " + mLedger.get(position).getPrice() + "원");
        holder.tvPaymemo.setText("내용 : " + mLedger.get(position).getPaymemo());

        // 수정 버튼
        holder.btnEdit.setOnClickListener(v -> {
            EditDialog dialogs = new EditDialog(context, mLedger, position, selectChatuid);
            dialogs.show();
        });

        // 삭제 버튼
        holder.btnDelete.setOnClickListener(v -> {
            AlertDialog.Builder alertdialog = new AlertDialog.Builder(context);
            alertdialog.setMessage("정말 삭제 하시겠습니까?");

            // 확인 버튼
            alertdialog.setPositiveButton("확인", (dialog, which) -> {
                if (!mLedger.get(position).getClassfy().equals("지출") && !mLedger.get(position).getClassfy().equals("수입")) {
                    logger.writeLog("Error : getClassfy() - 잘못된 값이 입력됨");
                    return;
                }

                // 가계부에서 선택한 데이터 삭제
                myRef.child(user.getUid()).child("Ledger")
                    .child(mLedger.get(position).getYear())
                    .child(mLedger.get(position).getMonth())
                    .child(mLedger.get(position).getDay())
                    .child(mLedger.get(position).getClassfy())
                    .child(mLedger.get(position).getTimes())
                    .removeValue();

                Toast.makeText(context, "삭제되었습니다", Toast.LENGTH_SHORT).show();
            });

            // 취소 버튼
            alertdialog.setNegativeButton("취소", (dialog, which) -> { });

            // 알럿 다이얼로그 생성
            alertdialog.create().show();
        });
    }

    @Override
    public int getItemCount() {
        return mLedger.size();
    }

    private boolean hasDifferentDate(List<Ledger> ledger, int positionA, int positionB) {
        if (positionA < 0 || positionB < 0)
            return true;

        if (!ledger.get(positionA).getYear().equals(ledger.get(positionB).getYear()))
            return true;

        if (!ledger.get(positionA).getMonth().equals(ledger.get(positionB).getMonth()))
            return true;

        if (!ledger.get(positionA).getDay().equals(ledger.get(positionB).getDay()))
            return true;

        return false;
    }
}