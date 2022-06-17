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

import kr.ac.kpu.block.smared.databinding.ListFriendBinding;

public class FriendAdapter extends RecyclerView.Adapter<FriendAdapter.ViewHolder> {
    private FormattedLogger logger = new FormattedLogger();

    private List<UserInfo> friends;
    private Context context;

    // 리스트의 각 요소마다 뷰를 만들어서 뷰홀더에 저장해두는 것으로 findViewById가 매번 호출되는 것을 방지한다.
    public static class ViewHolder extends RecyclerView.ViewHolder {
        private TextView tvNickname;
        private ImageView ivUser;

        public ViewHolder(ListFriendBinding viewBinding) {
            super(viewBinding.getRoot());
            this.tvNickname = viewBinding.tvNickname;
            this.ivUser = viewBinding.ivUser;
        }
    }

    public FriendAdapter(List<UserInfo> friends , Context context) {
        this.friends = friends;
        this.context = context;
    }

    // ViewHolder를 새로 만들어야 할 때 호출되는 메서드이다.
    // 이 메서드를 통해 각 아이템을 위한 XML 레이아웃을 이용한 뷰 객체를 생성하고 뷰 홀더에 담아 리턴한다.
    // 이때는 뷰의 콘텐츠를 채우지 않는다. 왜냐하면 아직 ViewHolder가 특정 데이터에 바인딩된 상태가 아니기 때문이다.
    @Override
    public FriendAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        ListFriendBinding viewBinding = ListFriendBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(viewBinding);
    }

    // ViewHolder를 데이터와 연결할 때 호출되는 메서드이다.
    // 이 메서드를 통해 뷰홀더의 레이아웃을 채우게 된다.
    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {
        holder.tvNickname.setText(friends.get(position).getNickname());
        String photoUri = friends.get(position).getPhotoUri();

        if (!TextUtils.isEmpty(photoUri)) {
            Picasso.with(context).load(photoUri).fit().centerInside().into(holder.ivUser);
        }
    }

    @Override
    public int getItemCount() {
        return friends.size();
    }
}