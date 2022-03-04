package kr.ac.kpu.block.smared;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.Hashtable;

//메인 액티비티에서는 로그인을 수행
public class MainActivity extends AppCompatActivity {
    String TAG = "MainActivity";

    //로그인에 필요한 정보들
    String stEmail;
    String stPassword;
    String stNickname;

    //텍스트 입력받기
    EditText etEmail;
    EditText etPassword;

    //프로그레스바
    ProgressBar pbLogin;

    //데이터베이스를 통한 사용자 인증
    private FirebaseAuth mAuth;
    DatabaseReference myRef;

    //안드로이드 생명주기 onCreate() -> onStart() -> onResume() -> onPause() -> onStop() -> onDestory()
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //레이아웃 xml 파싱해서 뷰를 생성한다.
        setContentView(R.layout.activity_main);

        //파이어베이스 DB에서 users 데이터를 불러온다.
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        myRef = database.getReference("users");

        //파이어베이스 인증 객체 생성
        mAuth = FirebaseAuth.getInstance();

        //뷰에 등록해둔 id와 액티비티 객체를 매칭시킨다.
        etEmail = (EditText) findViewById(R.id.etEmail);
        etPassword = (EditText) findViewById(R.id.etPassword);
        pbLogin = (ProgressBar) findViewById(R.id.pbLogin);

        //화면에 보일 기본 텍스트
        etEmail.setText("test@naver.com");
        etPassword.setText("lookup");

        //회원가입 버튼에 이벤트 등록
        Button btnRegister = (Button) findViewById(R.id.btnRegister);
        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //얼럿 다이얼로그 빌더
                final AlertDialog.Builder alertdialog = new AlertDialog.Builder(MainActivity.this);

                //이메일, 비밀번호, 닉네임 입력받기
                final EditText email = new EditText(MainActivity.this);
                final EditText password = new EditText(MainActivity.this);
                final EditText nickname = new EditText(MainActivity.this);

                //아무것도 입력되지 않았을 때 보이는 기본 메시지
                email.setHint("Email을 입력해주세요");
                password.setHint("비밀번호를 입력해주세요");
                nickname.setHint("닉네임을 입력해주세요");

                //비밀번호는 보이지 않도록 처리
                password.setTransformationMethod(PasswordTransformationMethod.getInstance());

                //addView를 통해 선형 레이아웃에 UI 컴포넌트 추가
                LinearLayout layout = new LinearLayout(MainActivity.this);
                layout.setOrientation(LinearLayout.VERTICAL);
                layout.addView(email);
                layout.addView(password);
                layout.addView(nickname);

                //다이얼로그에 레이아웃을 적용하기 위해 setView 사용
                alertdialog.setView(layout);

                //[Refactoring] 화면에 보이지 않는다.
                //다이얼로그 제목을 설정한다.
                alertdialog.setTitle("회원가입");

                //확인 버튼 만들고 이벤트 등록
                alertdialog.setPositiveButton("확인", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //입력된 텍스트 가져와서
                        stEmail = email.getText().toString();
                        stPassword = password.getText().toString();
                        stNickname = nickname.getText().toString();

                        //[Refactoring] 중첩 if문 단순화 해야한다.
                        //입력값이 없거나 잘못 입력되었으면 경고 띄우고
                        if(stEmail.isEmpty() || stPassword.isEmpty() || stNickname.isEmpty()) {
                            Toast.makeText(MainActivity.this, "양식을 모두 채워주세요", Toast.LENGTH_SHORT).show();
                        } else {
                            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(stEmail).matches()) {
                                Toast.makeText(MainActivity.this, "이메일 형식이 잘못되었습니다.", Toast.LENGTH_SHORT).show();
                            } else {
                                //[Refactoring] 닉네임을 입력받았으나 전달하지 않는다.
                                //회원가입 함수를 호출한다.
                                registerUser(stEmail, stPassword);
                            }
                        }
                    }
                });

                //취소 버튼 만들고 이벤트 등록
                alertdialog.setNegativeButton("취소", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) { }
                });

                //얼럿 다이얼로그 빌더를 통해 객체를 만들고 보여준다.
                AlertDialog alert = alertdialog.create();
                alert.show();
            }
        });

        //로그인 버튼에 이벤트 등록
        Button btnLogin = (Button) findViewById(R.id.btnLogin);
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //텍스트 가져와서
                stEmail = etEmail.getText().toString();
                stPassword = etPassword.getText().toString();

                //값이 없으면 알림 메시지를 보여준다.
                if(stEmail.isEmpty() || stEmail.equals("") || stPassword.isEmpty() || stPassword.equals("") ) {
                    Toast.makeText(MainActivity.this, "입력이 없습니다.", Toast.LENGTH_SHORT).show();
                } else {
                    //값이 있으면 로그인 함수를 호출한다.
                    userLogin(stEmail, stPassword);
                }

                //[Refactoring] 디버깅용 메시지
                //Toast.makeText(MainActivity.this, "LOGIN", Toast.LENGTH_SHORT).show();
            }
        });
    }

    //안드로이드 생명주기 onCreate() -> onStart() -> onResume() -> onPause() -> onStop() -> onDestory()
    @Override
    public void onStart() {
        super.onStart();
        //[Refactoring] 값을 쓰지 않고 있다. https://firebase.google.com/docs/auth/android/start
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        //if(currentUser != null){
        //        reload();
        //}
    }

    // 회원 가입 기능
    //https://firebase.google.com/docs/auth/android/password-auth
    public void registerUser(String email, String password) {
        //파이어베이스를 통해 회원가입하고 콜백 등록
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            //회원가입 성공
                            Log.d(TAG, "createUserWithEmail:success");
                            Toast.makeText(MainActivity.this, "회원가입 성공", Toast.LENGTH_SHORT).show();

                            // HashTable에 회원가입에 필요한 정보 넣고
                            Hashtable<String, String> profile = new Hashtable<String, String>();
                            profile.put("email", stEmail);
                            profile.put("photo","https://firebasestorage.googleapis.com/v0/b/smared-d1166.appspot.com/o/users%2Fnoimage.jpg?alt=media&token=a07b849c-87c6-4840-9364-be7b8ca7d8ef");

                            FirebaseUser user = mAuth.getCurrentUser();
                            profile.put("key", user.getUid());
                            profile.put("nickname", stNickname);

                            //[???] user.getUid() 호출 2번 되는데 문제 안생기나?
                            //DB에 넣기
                            myRef.child(user.getUid()).setValue(profile);
                        } else {
                            //회원가입 실패
                            Log.w(TAG, "createUserWithEmail:failure", task.getException());
                            Toast.makeText(MainActivity.this, "회원가입 실패", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    // 로그인 기능
    private void userLogin(String email, String password) {
        //프로그레스바 표시
        pbLogin.setVisibility(View.VISIBLE);

        //파이어베이스를 통해 로그인하고 콜백 등록
        mAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    pbLogin.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        //로그인 성공 메시지 출력
                        Log.d(TAG, "signInWithEmail:success");
                        Toast.makeText(MainActivity.this, "로그인 성공", Toast.LENGTH_SHORT).show();

                        //로그인에 성공한 사용자 정보를 가져온다.
                        FirebaseUser user = mAuth.getCurrentUser();
                        SharedPreferences sharedPreferences = getSharedPreferences("email",MODE_PRIVATE);
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putString("uid",user.getUid());
                        editor.putString("email",user.getEmail());
                        editor.apply();

                        //탭 액티비티 화면으로 넘어간다.
                        Intent in = new Intent(MainActivity.this, TabActivity.class);
                        startActivity(in);
                    } else {
                        //로그인 실패
                        Log.w(TAG, "signInWithEmail:failure", task.getException());
                        Toast.makeText(MainActivity.this, "로그인 실패", Toast.LENGTH_SHORT).show();
                    }
                }
            });
    }
}