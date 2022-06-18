package kr.ac.kpu.block.smared;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import kr.ac.kpu.block.smared.databinding.ActivityLoginBinding;

// 메인 액티비티에서는 로그인을 수행
public class UserLoginActivity extends AppCompatActivity {
    private FormattedLogger logger = new FormattedLogger();
    private ActivityLoginBinding viewBinding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewBinding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(viewBinding.getRoot());

        viewBinding.btnRegister.setOnClickListener(view -> showSignUpDialog());
        viewBinding.btnLogin.setOnClickListener(view -> signIn());
    }

    private void signIn() {
        String email = viewBinding.etEmail.getText().toString();
        String password = viewBinding.etPassword.getText().toString();

        if (email.isEmpty()) {
            Toast.makeText(UserLoginActivity.this, "이메일을 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.isEmpty()) {
            Toast.makeText(UserLoginActivity.this, "비밀번호를 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        // 로그인 중 프로그레스바 화면에 보이기
        viewBinding.pbLogin.setVisibility(View.VISIBLE);

        UserAuthDAO dao = new UserAuthDAO();
        dao.setSuccessCallback(arg -> afterSuccess());
        dao.setFailureCallback(arg -> afterFailure());
        dao.signIn(email, password);
    }

    private void showSignUpDialog() {
        UserRegisterDialog userRegisterDialog = new UserRegisterDialog(UserLoginActivity.this);
        userRegisterDialog.show();
    }

    private void afterSuccess() {
        // 로그인 이후 프로그레스바 숨기기
        viewBinding.pbLogin.setVisibility(View.GONE);
        Toast.makeText(UserLoginActivity.this, "로그인 성공", Toast.LENGTH_SHORT).show();

        // 사용자 정보 읽어오기
        UserInfo user = new UserInfo();

        DAO dao = new DAO();
        dao.setSuccessCallback(arg -> {});
        dao.setFailureCallback(arg -> {});
        dao.readAll(user, UserInfo.class);

        // 사용자 정보를 싱글톤을 이용해 stack 영역에 보존한다.
        UserInfoSingleton userInfoSingleton = UserInfoSingleton.getInstance();
        userInfoSingleton.setCurrentUserInfo(user);

        // 탭 액티비티 화면으로 넘어간다.
        startActivity(new Intent(UserLoginActivity.this, TabActivity.class));
    }

    private void afterFailure() {
        // 로그인 이후 프로그레스바 숨기기
        viewBinding.pbLogin.setVisibility(View.GONE);
        Toast.makeText(UserLoginActivity.this, "로그인 실패", Toast.LENGTH_SHORT).show();
    }
}