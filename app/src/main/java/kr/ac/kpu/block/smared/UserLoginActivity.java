package kr.ac.kpu.block.smared;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Map;

import kr.ac.kpu.block.smared.databinding.ActivityLoginBinding;

// 메인 액티비티에서는 로그인을 수행
public class LoginActivity extends AppCompatActivity {
    private FormattedLogger logger = new FormattedLogger();
    private ActivityLoginBinding viewBinding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewBinding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(viewBinding.getRoot());

        viewBinding.btnRegister.setOnClickListener(view -> signUp());
        viewBinding.btnLogin.setOnClickListener(view -> signIn());
    }

    private void signUp() {
        showSignUpDialog();
    }

    private void signIn() {
        String email = viewBinding.etEmail.getText().toString();
        String password = viewBinding.etPassword.getText().toString();

        if (email.isEmpty()) {
            Toast.makeText(LoginActivity.this, "이메일을 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.isEmpty()) {
            Toast.makeText(LoginActivity.this, "비밀번호를 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        // 로그인 중 프로그레스바 화면에 보이기
        viewBinding.pbLogin.setVisibility(View.VISIBLE);

        AuthDAO dao = new AuthDAO();
        dao.setSuccessCallback(arg -> afterSuccess());
        dao.setFailureCallback(arg -> afterFailure());
        dao.signIn(email, password);
    }

    private void showSignUpDialog() {
        // 얼럿 다이얼로그 빌더
        final AlertDialog.Builder alertdialog = new AlertDialog.Builder(LoginActivity.this);

        // 이메일, 비밀번호, 닉네임 입력받기
        final EditText etEmail = new EditText(LoginActivity.this);
        final EditText etPassword = new EditText(LoginActivity.this);
        final EditText etNickname = new EditText(LoginActivity.this);

        // 아무것도 입력되지 않았을 때 보이는 기본 메시지
        etEmail.setHint("Email을 입력해주세요");
        etPassword.setHint("비밀번호를 입력해주세요");
        etNickname.setHint("닉네임을 입력해주세요");

        // 비밀번호는 보이지 않도록 처리
        etPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());

        // addView를 통해 선형 레이아웃에 UI 컴포넌트 추가
        LinearLayout layout = new LinearLayout(LoginActivity.this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.addView(etEmail);
        layout.addView(etPassword);
        layout.addView(etNickname);

        // 다이얼로그에 레이아웃을 적용하기 위해 setView 사용
        alertdialog.setView(layout);

        // 확인 버튼 만들고 이벤트 등록
        alertdialog.setPositiveButton("확인", (dialog, which) -> {
            // 입력된 텍스트 가져와서
            String email = etEmail.getText().toString();
            String password = etPassword.getText().toString();
            String nickname = etNickname.getText().toString();

            // 입력 값이 없는 경우
            if (email.isEmpty() || password.isEmpty() || nickname.isEmpty()) {
                Toast.makeText(LoginActivity.this, "양식을 모두 채워주세요", Toast.LENGTH_SHORT).show();
                return;
            }

            // 입력 값 형식이 잘못된 경우
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(LoginActivity.this, "이메일 형식이 잘못되었습니다.", Toast.LENGTH_SHORT).show();
                return;
            }

            // 파이어베이스를 통해 회원가입 요청을 보내고 응답이 왔을 때 실행할 콜백 등록
            FirebaseAuth mAuth = FirebaseAuth.getInstance();
            mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(this, task -> {
                // 회원가입 실패
                if (!task.isSuccessful()) {
                    Toast.makeText(LoginActivity.this, "회원가입 실패", Toast.LENGTH_SHORT).show();
                    return;
                }

                // 회원가입 성공
                Toast.makeText(LoginActivity.this, "회원가입 성공", Toast.LENGTH_SHORT).show();
                FirebaseUser user = mAuth.getCurrentUser();

                // HashTable에 회원가입에 필요한 정보 넣고
                String photoUri =  "https://firebasestorage.googleapis.com/v0/b/smared-d1166.appspot.com/o/users%2Fnoimage.jpg?alt=media&token=a07b849c-87c6-4840-9364-be7b8ca7d8ef";
                Map<String, Object> userInfo = new UserInfo(email, photoUri, user.getUid(), "").toMap();

                // DB에 넣기
                FirebaseDatabase.getInstance().getReference("users").child(user.getUid()).setValue(userInfo);
            });
        });

        alertdialog.setNegativeButton("취소", (dialog, which) -> { });

        alertdialog.create().show();
    }

    private void afterSuccess() {
        // 로그인 이후 프로그레스바 숨기기
        viewBinding.pbLogin.setVisibility(View.GONE);
        Toast.makeText(LoginActivity.this, "로그인 성공", Toast.LENGTH_SHORT).show();

        // 사용자 정보 읽어오기
        UserInfo user = new UserInfo();

        DAO dao = new DAO();
        dao.setSuccessCallback(arg -> {});
        dao.setFailureCallback(arg -> {});
        dao.readAll(user, UserInfo.class);

        // 사용자 정보를 싱글톤을 이용해 stack 영역에 보존한다.
        UserInfoSingleton userInfoSingleton = UserInfoSingleton.getInstance();
        userInfoSingleton.setUserInfo(user);

        // 탭 액티비티 화면으로 넘어간다.
        startActivity(new Intent(LoginActivity.this, TabActivity.class));
    }

    private void afterFailure() {
        // 로그인 이후 프로그레스바 숨기기
        viewBinding.pbLogin.setVisibility(View.GONE);
        Toast.makeText(LoginActivity.this, "로그인 실패", Toast.LENGTH_SHORT).show();
    }
}