package kr.ac.kpu.block.smared;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.Window;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import kr.ac.kpu.block.smared.databinding.DialogUserRegisterBinding;

public class UserRegisterDialog extends Dialog {
    private FormattedLogger logger = new FormattedLogger();
    private DialogUserRegisterBinding viewBinding;

    public UserRegisterDialog(@NonNull Context context) {
        super(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewBinding = DialogUserRegisterBinding.inflate(getLayoutInflater());
        setContentView(viewBinding.getRoot());
        requestWindowFeature(Window.FEATURE_NO_TITLE); // 타이틀 바 제거

        // 이벤트 등록
        viewBinding.btnSubmit.setOnClickListener(view -> signUp());
        viewBinding.btnCancel.setOnClickListener(view -> dismiss());
    }

    private void signUp() {
        String email = viewBinding.etEmail.getText().toString();
        String password = viewBinding.etPassword.getText().toString();

        if (email.isEmpty()) {
            Toast.makeText(getContext(), "이메일을 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.isEmpty()) {
            Toast.makeText(getContext(), "비밀번호를 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(getContext(), "이메일 형식이 잘못되었습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        UserAuthDAO dao = new UserAuthDAO();
        dao.setSuccessCallback(arg -> afterSuccess());
        dao.setFailureCallback(arg -> afterFailure());
        dao.signUp(email, password);
    }

    // 새로 가입된 사용자 정보를 DB에 추가
    private void afterSuccess() {
        Toast.makeText(getContext(), "회원가입 성공", Toast.LENGTH_SHORT).show();

        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();

        UserInfo newUser = new UserInfo();
        newUser.setUid(firebaseUser.getUid());
        newUser.setEmail(firebaseUser.getEmail());
        newUser.setNickname(viewBinding.etNickname.toString());
        newUser.setPhotoUri(firebaseUser.getPhotoUrl().toString());

        DAO dao = new DAO();
        dao.setSuccessCallback(arg -> {});
        dao.setFailureCallback(arg -> {});
        dao.create(newUser, UserInfo.class);
    }

    private void afterFailure() {
        Toast.makeText(getContext(), "회원가입 실패", Toast.LENGTH_SHORT).show();
    }
}
