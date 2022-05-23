package kr.ac.kpu.block.smared;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.List;

import kr.ac.kpu.block.smared.databinding.MyTextViewBinding;
import kr.ac.kpu.block.smared.databinding.TheyTextViewBinding;

public class MyAdapter extends RecyclerView.Adapter<MyAdapter.ViewHolder> {
    private FormattedLogger logger = new FormattedLogger();

    private Context context;
    private List<Chat> chatMessages;
    private String email;

    // 리스트의 각 요소마다 뷰를 만들어서 뷰홀더에 저장해두는 것으로 findViewById가 매번 호출되는 것을 방지한다.
    public static class ViewHolder extends RecyclerView.ViewHolder {
        private TextView mTextView;
        private TextView tvChatid;
        private ImageView ivChatimage;

        public ViewHolder(MyTextViewBinding viewBinding) {
            super(viewBinding.getRoot());
            this.mTextView = viewBinding.mTextView;
            this.tvChatid = viewBinding.tvChatid;
            this.ivChatimage = viewBinding.ivChatimage;
        }

        public ViewHolder(TheyTextViewBinding viewBinding) {
            super(viewBinding.getRoot());
            this.mTextView = viewBinding.mTextView;
            this.tvChatid = viewBinding.tvChatid;
            this.ivChatimage = viewBinding.ivChatimage;
        }
    }

    public MyAdapter(List<Chat> chatMessages, String email, Context context) {
        this.chatMessages = chatMessages;
        this.email = email;
        this.context = context;
    }

    // 자신의 뷰 타입은 1 다른 사람은 2
    @Override
    public int getItemViewType(int position) {
        return (chatMessages.get(position).getUserInfo().getEmail().equals(email)) ? 1 : 2;
    }

    // ViewHolder를 새로 만들어야 할 때 호출되는 메서드이다.
    // 이 메서드를 통해 각 아이템을 위한 XML 레이아웃을 이용한 뷰 객체를 생성하고 뷰 홀더에 담아 리턴한다.
    // 이때는 뷰의 콘텐츠를 채우지 않는다. 왜냐하면 아직 ViewHolder가 특정 데이터에 바인딩된 상태가 아니기 때문이다.
    @Override
    public MyAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        MyTextViewBinding viewBinding = MyTextViewBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        TheyTextViewBinding viewBinding2 = TheyTextViewBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return  (viewType == 1) ? new ViewHolder(viewBinding) : new ViewHolder(viewBinding2);
    }

    // ViewHolder를 데이터와 연결할 때 호출되는 메서드이다.
    // 이 메서드를 통해 뷰홀더의 레이아웃을 채우게 된다.
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.mTextView.setText(chatMessages.get(position).getMessage());
        holder.tvChatid.setText(chatMessages.get(position).getUserInfo().getNickname());
        if (!TextUtils.isEmpty(chatMessages.get(position).getUserInfo().getPhotoUri())) {
            Picasso.with(context).load(chatMessages.get(position).getUserInfo().getPhotoUri()).fit().centerInside().into(holder.ivChatimage);
        }
    }

    @Override
    public int getItemCount() {
        return chatMessages.size();
    }
}