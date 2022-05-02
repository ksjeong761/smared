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
import android.util.Log;
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
import java.util.Hashtable;

import kr.ac.kpu.block.smared.databinding.FragmentProfileBinding;

public class ProfileFragment extends Fragment {
    private FormattedLogger logger = new FormattedLogger();
    private FragmentProfileBinding viewBinding;
    private String TAG = getClass().getSimpleName();

    private DatabaseReference myRef;
    private DatabaseReference chatRef;
    private FirebaseUser user;

    private Bitmap bitmap;
    private String stUid;
    private String stEmail;
    private String stNickname;
    private int regStatus = 1;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        viewBinding = FragmentProfileBinding.inflate(inflater, container, false);

        user = FirebaseAuth.getInstance().getCurrentUser();
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("email", Context.MODE_PRIVATE);
        stUid = sharedPreferences.getString("uid","");
        stEmail = sharedPreferences.getString("email","");

        myRef = FirebaseDatabase.getInstance().getReference();
        chatRef = FirebaseDatabase.getInstance().getReference("chats");

        // 데이터 변경 및 취소 이벤트
        myRef.child("users").child(stUid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (regStatus==0) {
                    getActivity().finish();
                    return;
                }

                logger.writeLog("Value is: " + dataSnapshot.getValue().toString());

                stNickname = dataSnapshot.child("nickname").getValue().toString();
                viewBinding.tvNickname.setText(stNickname);

                String stPhoto = dataSnapshot.child("photo").getValue().toString();
                if (TextUtils.isEmpty(stPhoto)) {
                    viewBinding.pbLogin.setVisibility(getView().GONE);
                    return;
                }

                Picasso.with(getActivity())
                    .load(stPhoto)
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
                // Failed to read value
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
                stNickname = etNickname.getText().toString();
                viewBinding.tvNickname.setText("닉네임 : "+ stNickname);
                myRef.child("users").child(stUid).child("nickname").setValue(stNickname);
            });

            alertDialog.setNegativeButton("취소", (dialog, which) -> { });
            alertDialog.show();
        });

        // 회원탈퇴 버튼 이벤트
        viewBinding.btnWithdrawal.setOnClickListener(view -> {
            AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity());
            alertDialog.setMessage("정말 탈퇴하시겠습니까?");

            alertDialog.setPositiveButton("예", (dialog, which) -> user.delete().addOnCompleteListener(task -> {
                if (!task.isSuccessful()) {
                    return;
                }

                regStatus=0;
                myRef.child("users").child(stUid).removeValue();

                chatRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        for (DataSnapshot chatSnapshot : dataSnapshot.getChildren()) {
                            for (DataSnapshot userSnapshot : chatSnapshot.getChildren()) {
                                for (DataSnapshot uidSnapshot : userSnapshot.getChildren()) {
                                    if (uidSnapshot.getKey().equals(stUid)) {
                                        chatRef.child(chatSnapshot.getKey()).child("user").child(uidSnapshot.getKey()).removeValue();
                                    }
                                }
                            }
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) { }
                });

                Log.d(TAG, "User account deleted.");
                Toast.makeText(getActivity(),"계정이 삭제되었습니다.",Toast.LENGTH_SHORT).show();
                getActivity().finish();
            }));

            alertDialog.setNegativeButton("아니오", (dialog, which) -> { });
            alertDialog.show();
        });

        return viewBinding.getRoot();
    }

    // 이미지뷰 파일 업로드
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        try {
            Uri image = data.getData();
            try {
                bitmap = MediaStore.Images.Media.getBitmap(getActivity().getContentResolver(),image);
                uploadImage();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (NullPointerException e) {
            viewBinding.pbLogin.setVisibility(getView().GONE);
        }
    }

    // 외부저장소 권한 응답
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the contacts-related task you need to do.
                } else {
                    // permission denied, boo! Disable the functionality that depends on this permission.
                }

                return;
            }
        }
    }

    public void uploadImage() {
        ByteArrayOutputStream bitmapByteStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bitmapByteStream);
        byte[] bitmapByte = bitmapByteStream.toByteArray();

        StorageReference profileRef = FirebaseStorage.getInstance().getReference().child("users").child(stUid + ".jpg");
        UploadTask uploadTask = profileRef.putBytes(bitmapByte);
        // Handle unsuccessful uploads
        uploadTask.addOnFailureListener(exception -> { }).addOnSuccessListener(taskSnapshot -> {
            // taskSnapshot.getMetadata() contains file metadata such as size, content-type, and download URL.
            Uri downloadUrl = taskSnapshot.getDownloadUrl();
            String photoUrl = String.valueOf(downloadUrl);
            logger.writeLog(photoUrl);

            Hashtable<String, Object> profile = new Hashtable<>();
            profile.put("email", stEmail);
            profile.put("key", stUid);
            profile.put("photo", photoUrl);
            profile.put("nickname", stNickname);

            DatabaseReference myRef = FirebaseDatabase.getInstance().getReference("users");
            myRef.child(stUid).updateChildren(profile);
            myRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot == null ) {
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
}
