package kr.ac.kpu.block.smared;

import android.app.AlertDialog;
import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.List;

public class LedgerAdapter extends RecyclerView.Adapter<LedgerAdapter.ViewHolder> {

    Context context;

    // 데이터베이스 관련
    FirebaseDatabase database;
    FirebaseUser user;
    DatabaseReference myRef;
    DatabaseReference chatRef;

    List<Ledger> mLedger;
    String selectChatuid = "";

    public LedgerAdapter(List<Ledger> mLedger , Context context) {
        this.mLedger = mLedger;
        this.context = context;
    }

    public LedgerAdapter(List<Ledger> mLedger , Context context, String selectChatuid) {
        this.mLedger = mLedger;
        this.context = context;
        this.selectChatuid = selectChatuid;
    }

    // 리스트의 각 요소마다 뷰를 만들어서 뷰홀더에 저장해두는 것으로 findViewById가 매번 호출되는 것을 방지한다.
    public static class ViewHolder extends RecyclerView.ViewHolder {
        public Button btnDelete;
        public Button btnEdit;
        public Button btnDay;
        public TextView tvUseitem;
        public TextView tvPrice;
        public TextView tvPaymemo;
        public TextView tvChoice;

        public ViewHolder(View itemView) {
            super(itemView);
            btnDay = itemView.findViewById(R.id.btnDay);
            tvUseitem= itemView.findViewById(R.id.tvUseitem);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            tvPaymemo = itemView.findViewById(R.id.tvPaymemo);
            tvChoice = itemView.findViewById(R.id.tvChoice);
            btnDelete = itemView.findViewById(R.id.btnDelete);
            btnEdit = itemView.findViewById(R.id.btnEdit);
        }
    }

    // 목록에 날짜가 다른 요소가 있다면 뷰 타입은 1이고 다은 경우는 2이다.
    @Override
    public int getItemViewType(int position) {
        if (position == 0) {
            return 2;
        } else {
            if (mLedger.get(position).getYear().equals(mLedger.get(position - 1).getYear())
                    && mLedger.get(position).getMonth().equals(mLedger.get(position - 1).getMonth())
                    && mLedger.get(position).getDay().equals(mLedger.get(position - 1).getDay())) {
                return 1;
            } else {
                return 2;
            }
        }
    }

    // ViewHolder를 새로 만들어야 할 때 호출되는 메서드이다.
    // 이 메서드를 통해 각 아이템을 위한 XML 레이아웃을 이용한 뷰 객체를 생성하고 뷰 홀더에 담아 리턴한다.
    // 이때는 뷰의 콘텐츠를 채우지 않는다. 왜냐하면 아직 ViewHolder가 특정 데이터에 바인딩된 상태가 아니기 때문이다.
    // Create new views (invoked by the layout manager)
    @Override
    public LedgerAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        database = FirebaseDatabase.getInstance();
        myRef = database.getReference("users");
        chatRef = database.getReference("chats");
        user = FirebaseAuth.getInstance().getCurrentUser();

        View v;
        if (viewType == 1) {
            v = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_content, parent, false);
        } else {
            v = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_ledger, parent, false);
        }
        ViewHolder vh = new ViewHolder(v);
        return vh;
    }

    // ViewHolder를 데이터와 연결할 때 호출되는 메서드이다.
    // 이 메서드를 통해 뷰홀더의 레이아웃을 채우게 된다.
    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {
        // [Refactor] 중첩 if문 단순화
        if (position == 0) {
            holder.btnDay.setText(mLedger.get(position).getYear() + "-" + mLedger.get(position).getMonth() + "-" + mLedger.get(position).getDay());
        } else {
            if (mLedger.get(position).getYear().equals(mLedger.get(position - 1).getYear()) &&
                    mLedger.get(position).getMonth().equals(mLedger.get(position - 1).getMonth()) &&
                    mLedger.get(position).getDay().equals(mLedger.get(position - 1).getDay())) {
            } else {
                holder.btnDay.setText(mLedger.get(position).getYear() + "-" + mLedger.get(position).getMonth() + "-" + mLedger.get(position).getDay());
            }
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
                // [Refactor] 에러 로그 찍기
                // 지출, 수입이 아닌 문자열은 잘못된 것이다.
                if (!mLedger.get(position).getClassfy().equals("지출") && !mLedger.get(position).getClassfy().equals("수입")) {
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

                // [Refactor] 삭제 성공 여부 확인해야 함
                Toast.makeText(context, "삭제되었습니다", Toast.LENGTH_SHORT).show();
            });

            // 취소 버튼
            alertdialog.setNegativeButton("취소", (dialog, which) -> { });

            // 알럿 다이얼로그 생성
            alertdialog.create().show();
        });
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mLedger.size();
    }
}