package kr.ac.kpu.block.smared;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import kr.ac.kpu.block.smared.databinding.FragmentProfileBinding;

public class ProfileFragment extends Fragment {
    private FormattedLogger logger = new FormattedLogger();
    private FragmentProfileBinding viewBinding;

    private DatabaseReference myRef;
    private DatabaseReference chatRef;
    private FirebaseUser user;

    private final int EXTERNAL_STORAGE_PERMISSION = 1;

    private String uid;
    private String email;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        viewBinding = FragmentProfileBinding.inflate(inflater, container, false);

        myRef = FirebaseDatabase.getInstance().getReference();
        chatRef = FirebaseDatabase.getInstance().getReference("chats");
        user = FirebaseAuth.getInstance().getCurrentUser();

        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("email", Context.MODE_PRIVATE);
        uid = sharedPreferences.getString("uid","");
        email = sharedPreferences.getString("email","");

        // 사용자 닉네임, 프로필 이미지 URI 읽어오기
        myRef.child("users").child(uid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // 화면에 닉네임 출력
                String nickname = dataSnapshot.child("nickname").getValue().toString();
                viewBinding.tvNickname.setText(nickname);

                String photoUri = dataSnapshot.child("photo").getValue().toString();
                if (TextUtils.isEmpty(photoUri)) {
                    viewBinding.pbLogin.setVisibility(getView().GONE);
                    return;
                }

                // 화면에 프로필 이미지 출력
                Picasso.with(getActivity())
                    .load(photoUri)
                    .fit()
                    .centerInside()
                    .into(viewBinding.ivUser, new Callback.EmptyCallback() {
                        @Override
                        public void onSuccess() {
                            // Index 0 is the image view.
                            logger.writeLog("SUCCESS");
                            viewBinding.pbLogin.setVisibility(getView().GONE);
                        }
                    });
            }

            @Override
            public void onCancelled(DatabaseError error) {
                logger.writeLog("Failed to read value : " + error.toException().getMessage());
            }
        });

        // 외부 저장소 권한 확인
        if (PackageManager.PERMISSION_GRANTED != ContextCompat.checkSelfPermission(getActivity(), android.Manifest.permission.READ_EXTERNAL_STORAGE)) {
            // 사용자가 명시적으로 권한을 거부한 것이 아니라면
            if (!ActivityCompat.shouldShowRequestPermissionRationale(getActivity(), android.Manifest.permission.READ_EXTERNAL_STORAGE)) {
                // 권한을 요청한다.
                ActivityCompat.requestPermissions(getActivity(), new String[]{ android.Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
            }
        }

        // 프로필 사진 변경 버튼 이벤트
        viewBinding.btnChangePhoto.setOnClickListener(view -> {
            startActivityForResult(new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI),1);
            viewBinding.pbLogin.setVisibility(getView().VISIBLE);
        });

        // 로그아웃 버튼 이벤트
        viewBinding.btnLogout.setOnClickListener(view -> {
            FirebaseAuth.getInstance().signOut();
            Toast.makeText(getActivity(),"로그아웃 되었습니다",Toast.LENGTH_SHORT).show();
            getActivity().finish();
        });

        // 닉네임 변경 버튼 이벤트
        viewBinding.btnChangeNickname.setOnClickListener(view -> {
            AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity());
            final EditText etNickname = new EditText(getActivity());
            alertDialog.setTitle("닉네임 변경");
            alertDialog.setView(etNickname);

            alertDialog.setPositiveButton("확인", (dialog, which) -> {
                String nicknameTo = etNickname.getText().toString();
                viewBinding.tvNickname.setText("닉네임 : "+ nicknameTo);
                myRef.child("users").child(uid).child("nickname").setValue(nicknameTo);
            });

            alertDialog.setNegativeButton("취소", (dialog, which) -> { });
            alertDialog.show();
        });

        // 회원 탈퇴 버튼 이벤트
        viewBinding.btnWithdrawal.setOnClickListener(view -> {
            AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity());
            alertDialog.setMessage("정말 탈퇴하시겠습니까?");

            alertDialog.setPositiveButton("예", (dialog, which) -> user.delete().addOnCompleteListener(task -> {
                if (!task.isSuccessful()) {
                    return;
                }

                // 사용자 DB에서 정보를 삭제한다.
                myRef.child("users").child(uid).removeValue();

                // 채팅방 참가자 목록에서도 정보를 삭제한다.
                chatRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        for (DataSnapshot chatSnapshot : dataSnapshot.getChildren()) {
                            for (DataSnapshot userSnapshot : chatSnapshot.getChildren()) {
                                for (DataSnapshot uidSnapshot : userSnapshot.getChildren()) {
                                    if (uidSnapshot.getKey().equals(uid)) {
                                        chatRef.child(chatSnapshot.getKey()).child("user").child(uidSnapshot.getKey()).removeValue();
                                    }
                                }
                            }
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) { }
                });

                Toast.makeText(getActivity(),"계정이 삭제되었습니다.",Toast.LENGTH_SHORT).show();
                getActivity().finish();
            }));

            alertDialog.setNegativeButton("아니오", (dialog, which) -> { });
            alertDialog.show();
        });

        return viewBinding.getRoot();
    }

    // 변경할 프로필 사진 파일 업로드
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent priviousIntent) {
        super.onActivityResult(requestCode, resultCode, priviousIntent);

        try {
            Uri image = priviousIntent.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getActivity().getContentResolver(),image);
                uploadImage(bitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (NullPointerException e) {
            viewBinding.pbLogin.setVisibility(getView().GONE);
        }
    }


    private void uploadImage(Bitmap bitmap) {
        ByteArrayOutputStream bitmapByteStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bitmapByteStream);
        byte[] bitmapByte = bitmapByteStream.toByteArray();

        // 사용자 정보 테이블에 이미지 업로드 시도
        StorageReference profileRef = FirebaseStorage.getInstance().getReference().child("users").child(uid + ".jpg");
        UploadTask uploadTask = profileRef.putBytes(bitmapByte);

        // 업로드 실패 시 다시 한 번 업로드 시도
        uploadTask.addOnFailureListener(exception -> { }).addOnSuccessListener(taskSnapshot -> {
            Map<String, Object> userInfo = new HashMap<>();
            String nickname = viewBinding.tvNickname.getText().toString();
            String photoUrl = String.valueOf(taskSnapshot.getDownloadUrl());
            userInfo.put("email", email);
            userInfo.put("key", uid);
            userInfo.put("photo", photoUrl);
            userInfo.put("nickname", nickname);

            DatabaseReference myRef = FirebaseDatabase.getInstance().getReference("users");
            myRef.child(uid).updateChildren(userInfo);
            myRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot == null ) {
                        Toast.makeText(getActivity(), "사진 업로드 실패!",Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Toast.makeText(getActivity(), "사진 업로드 완료",Toast.LENGTH_SHORT).show();
                    viewBinding.ivUser.setImageBitmap(bitmap);
                }

                @Override
                public void onCancelled(DatabaseError databaseError) { }
            });
        });
    }

    // 외부 저장소 권한 응답
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case EXTERNAL_STORAGE_PERMISSION:
                if (grantResults.length == 0) {
                    break;
                }

                if (PackageManager.PERMISSION_GRANTED == grantResults[0]) {
                    break;
                }

                Toast.makeText(getActivity(), "권한 요청에 동의 해주셔야 이용 가능합니다. 설정에서 권한 허용 하시기 바랍니다.", Toast.LENGTH_SHORT).show();
                getActivity().finish();
                break;
        }
    }
}
