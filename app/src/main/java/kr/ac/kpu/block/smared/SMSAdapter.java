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

public class SMSAdapter extends RecyclerView.Adapter<SMSAdapter.ViewHolder> {
    private FormattedLogger logger = new FormattedLogger();

    // 데이터베이스 관련
    private FirebaseDatabase database;
    private DatabaseReference myRef;
    private FirebaseUser user;

    private Context context;
    private List<SMS> mBody;
    private String stUseitem;

    // 리스트의 각 요소마다 뷰를 만들어서 뷰홀더에 저장해두는 것으로 findViewById가 매번 호출되는 것을 방지한다.
    public static class ViewHolder extends RecyclerView.ViewHolder {

        public Button btnSMSDay;
        public TextView tvSMSPaymemo;
        public TextView tvSMSPrice;
        public TextView tvSMSTime;
        public Button btnAddSMS;
        public Spinner smsUseitem;

        public ViewHolder(View itemView) {
            super(itemView);

            btnSMSDay = itemView.findViewById(R.id.btnSMSDay);
            tvSMSPaymemo= itemView.findViewById(R.id.tvSMSPaymemo);
            tvSMSPrice= itemView.findViewById(R.id.tvSMSPrice);
            tvSMSTime= itemView.findViewById(R.id.tvSMSTime);
            btnAddSMS = itemView.findViewById(R.id.btnAddSMS);
            smsUseitem = itemView.findViewById(R.id.smsUseitem);
        }
    }

    public SMSAdapter(List<SMS> mBody , Context context) {
        this.mBody = mBody;
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

        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_sms, parent, false);
        ViewHolder vh = new ViewHolder(v);
        return vh;
    }

    // ViewHolder를 데이터와 연결할 때 호출되는 메서드이다.
    // 이 메서드를 통해 뷰홀더의 레이아웃을 채우게 된다.
    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {
      holder.tvSMSPaymemo.setText(mBody.get(position).getPayMemo());
      holder.btnSMSDay.setText(mBody.get(position).getYear() + "-" + mBody.get(position).getMonth() + "-" + mBody.get(position).getDay());
      holder.tvSMSPrice.setText("-" + mBody.get(position).getPrice() + "원");
      holder.tvSMSTime.setText("[신한체크]" + mBody.get(position).getTime());

      holder.smsUseitem.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
          @Override
          public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
              stUseitem = (String) parent.getItemAtPosition(position);
          }

          @Override
          public void onNothingSelected(AdapterView<?> parent) { }
      });

      holder.btnAddSMS.setOnClickListener(v -> {
          LedgerContent mledgerContent = new LedgerContent();
          mledgerContent.setPaymemo(mBody.get(position).getPayMemo());
          mledgerContent.setPrice(mBody.get(position).getPrice());
          mledgerContent.setUseItem(stUseitem);

          myRef.child(user.getUid()).child("Ledger")
              .child(mBody.get(position).getYear())
              .child(mBody.get(position).getMonth())
              .child(mBody.get(position).getDay())
              .child("지출")
              .child(mBody.get(position).getTime())
              .setValue(mledgerContent);

          Toast.makeText(context, "가계부에 추가되었습니다.", Toast.LENGTH_SHORT).show();
      });
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mBody.size();
    }
}