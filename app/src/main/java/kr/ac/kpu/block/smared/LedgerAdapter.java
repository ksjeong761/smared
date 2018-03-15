package kr.ac.kpu.block.smared;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.List;

public class LedgerAdapter extends RecyclerView.Adapter<LedgerAdapter.ViewHolder> {

    List<Ledger> mLedger;
    Context context;

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public static class ViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case


        public Button btnDay;
        public TextView tvUseitem;
        public TextView tvPrice;
        public TextView tvPaymemo;

        public ViewHolder(View itemView) {
            super(itemView);
            btnDay = (Button) itemView.findViewById(R.id.btnDay);
            tvUseitem= (TextView) itemView.findViewById(R.id.tvUseitem);
            tvPrice = (TextView) itemView.findViewById(R.id.tvPrice);
            tvPaymemo = (TextView) itemView.findViewById(R.id.tvPaymemo);

        }
    }

    public LedgerAdapter(List<Ledger> mLedger , Context context) {
        this.mLedger = mLedger;
        this.context = context;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public LedgerAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                       int viewType) {
        View v;
        v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_ledger, parent, false);
        ViewHolder vh = new ViewHolder(v);
        return vh;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
         holder.btnDay.setText(mLedger.get(position).getYear()+"-"+mLedger.get(position).getMonth()+"-"+mLedger.get(position).getDay());
         holder.tvUseitem.setText("분류 : " + mLedger.get(position).getUseItem());
         holder.tvPrice.setText("가격 : " + mLedger.get(position).getPrice());
         holder.tvPaymemo.setText("내용 : " + mLedger.get(position).getPaymemo());


        }



    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mLedger.size();
    }
}