package kr.ac.kpu.block.smared;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.List;

public class MyAdapter extends RecyclerView.Adapter<MyAdapter.ViewHolder> {
    private String[] mDataset;
    List<Chat> mChat;
    String stEmail;
    Context context;

    public MyAdapter(List<Chat> mChat, String email, Context context) {
        this.mChat = mChat;
        this.stEmail = email;
        this.context = context;
    }

    //리스트의 각 요소마다 뷰를 만들어서 뷰홀더에 저장해두는 것으로
    //findViewById가 매번 호출되는 것을 방지한다.
    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView mTextView;
        public TextView tvChatid;
        public ImageView ivChatimage;

        public ViewHolder(View itemView) {
            super(itemView);

            mTextView = (TextView) itemView.findViewById(R.id.mTextView);
            tvChatid = (TextView) itemView.findViewById(R.id.tvChatid);
            ivChatimage = (ImageView) itemView.findViewById(R.id.ivChatimage);
        }
    }

    //자신의 뷰 타입은 1 다른 사람은 2
    @Override
    public int getItemViewType(int position) {
        if(mChat.get(position).getEmail().equals(stEmail)) {
            return 1;
        } else {
            return 2;
        }
    }

    // ViewHolder를 새로 만들어야 할 때 호출되는 메서드이다.
    // 이 메서드를 통해 각 아이템을 위한 XML 레이아웃을 이용한 뷰 객체를 생성하고 뷰 홀더에 담아 리턴한다.
    // 이때는 뷰의 콘텐츠를 채우지 않는다. 왜냐하면 아직 ViewHolder가 특정 데이터에 바인딩된 상태가 아니기 때문이다.
    // Create new views (invoked by the layout manager)
    @Override
    public MyAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v;

        //자신의 뷰 타입은 1 다른 사람은 2
        if (viewType == 1) {
            v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.my_text_view, parent, false);
        } else {
            v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.they_text_view, parent, false);
        }

        ViewHolder vh = new ViewHolder(v);
        return vh;
    }

    // ViewHolder를 데이터와 연결할 때 호출되는 메서드이다.
    // 이 메서드를 통해 뷰홀더의 레이아웃을 채우게 된다.
    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        holder.mTextView.setText(mChat.get(position).getText());
        holder.tvChatid.setText(mChat.get(position).getNickname());
        if (TextUtils.isEmpty(mChat.get(position).getPhoto())) {

        } else {
            Picasso.with(context).load(mChat.get(position).getPhoto()).fit().centerInside().into(holder.ivChatimage);
        }
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mChat.size();
    }
}