package kr.ac.kpu.block.smared;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.List;

import kr.ac.kpu.block.smared.databinding.ListSmsBinding;

public class SMSAdapter extends RecyclerView.Adapter<SMSAdapter.ViewHolder> {
    private FormattedLogger logger = new FormattedLogger();

    // 데이터베이스 관련
    private FirebaseDatabase database;
    private DatabaseReference myRef;
    private FirebaseUser user;

    private Context context;
    private List<SMS> smsList;
    private String category = "";

    // 리스트의 각 요소마다 뷰를 만들어서 뷰홀더에 저장해두는 것으로 findViewById가 매번 호출되는 것을 방지한다.
    public static class ViewHolder extends RecyclerView.ViewHolder {
        private Button btnSMSDay;
        private Button btnAddSMS;
        private TextView tvSMSDescription;
        private TextView tvSMSPrice;
        private TextView tvSMSTime;
        private Spinner spnCategory;

        public ViewHolder(ListSmsBinding viewBinding) {
            super(viewBinding.getRoot());
            this.btnSMSDay = viewBinding.btnSMSDay;
            this.btnAddSMS = viewBinding.btnAddSMS;
            this.tvSMSDescription = viewBinding.tvSMSDescription;
            this.tvSMSPrice = viewBinding.tvSMSPrice;
            this.tvSMSTime = viewBinding.tvSMSTime;
            this.spnCategory = viewBinding.spnCategory;
        }
    }

    public SMSAdapter(List<SMS> smsList , Context context) {
        this.smsList = smsList;
        this.context = context;
    }

    // ViewHolder를 새로 만들어야 할 때 호출되는 메서드이다.
    // 이 메서드를 통해 각 아이템을 위한 XML 레이아웃을 이용한 뷰 객체를 생성하고 뷰 홀더에 담아 리턴한다.
    // 이때는 뷰의 콘텐츠를 채우지 않는다. 왜냐하면 아직 ViewHolder가 특정 데이터에 바인딩된 상태가 아니기 때문이다.
    // Create new views (invoked by the layout manager)
    @Override
    public SMSAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        database = FirebaseDatabase.getInstance();
        myRef = database.getReference("users");
        user = FirebaseAuth.getInstance().getCurrentUser();

        ListSmsBinding viewBinding = ListSmsBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(viewBinding);
    }

    // ViewHolder를 데이터와 연결할 때 호출되는 메서드이다.
    // 이 메서드를 통해 뷰홀더의 레이아웃을 채우게 된다.
    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {
      holder.tvSMSDescription.setText(smsList.get(position).getDescription());
      holder.btnSMSDay.setText(smsList.get(position).getYear() + "-" + smsList.get(position).getMonth() + "-" + smsList.get(position).getDay());
      holder.tvSMSPrice.setText("-" + smsList.get(position).getPrice() + "원");
      holder.tvSMSTime.setText("[신한체크]" + smsList.get(position).getTime());

      holder.spnCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
          @Override
          public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
              category = (String) parent.getItemAtPosition(position);
          }

          @Override
          public void onNothingSelected(AdapterView<?> parent) { }
      });

      holder.btnAddSMS.setOnClickListener(view -> {
          LedgerContent ledgerContent = new LedgerContent();
          ledgerContent.setDescription(smsList.get(position).getDescription());
          ledgerContent.setPrice(smsList.get(position).getPrice());
          ledgerContent.setCategory(category);

          myRef.child(user.getUid()).child("Ledger")
              .child(smsList.get(position).getYear())
              .child(smsList.get(position).getMonth())
              .child(smsList.get(position).getDay())
              .child("지출")
              .child(smsList.get(position).getTime())
              .setValue(ledgerContent);

          Toast.makeText(context, "가계부에 추가되었습니다.", Toast.LENGTH_SHORT).show();
      });
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return smsList.size();
    }
}