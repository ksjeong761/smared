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
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
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

public class ProfileFragment extends Fragment {

    ImageView ivUser;
    TextView tvNickname;
    Button btnChangePhoto;
    Button btnChangeNickname;
    Button btnLogout;
    Button btnWithdrawal;
    private StorageReference mStorageRef;
    Bitmap bitmap;
    String stUid;
    String stEmail;
    String stNickname;
    String TAG = getClass().getSimpleName();
    int regStatus = 1;
    ProgressBar pbLogin;

    FirebaseDatabase database;
    DatabaseReference myRef;
    DatabaseReference chatRef;
    FirebaseUser user;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_profile, container, false);
        tvNickname = v.findViewById(R.id.tvNickname);
        btnChangePhoto = v.findViewById(R.id.btnChangePhoto);
        btnLogout = v.findViewById(R.id.btnLogout);
        btnChangeNickname = v.findViewById(R.id.btnChangeNickname);
        btnWithdrawal = v.findViewById(R.id.btnWithdrawal);

        ivUser  = v.findViewById(R.id.ivUser);

        user = FirebaseAuth.getInstance().getCurrentUser();
        mStorageRef = FirebaseStorage.getInstance().getReference();
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("email",Context.MODE_PRIVATE);
        stUid = sharedPreferences.getString("uid","");
        stEmail = sharedPreferences.getString("email","");

        pbLogin = v.findViewById(R.id.pbLogin);

        database = FirebaseDatabase.getInstance();
        myRef = database.getReference();
        chatRef = database.getReference("chats");

        // 데이터 변경 및 취소 이벤트
        myRef.child("users").child(stUid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (regStatus==0) {
                    getActivity().finish();
                    return;
                }

                String value = dataSnapshot.getValue().toString();
                String stPhoto = dataSnapshot.child("photo").getValue().toString();
                stNickname = dataSnapshot.child("nickname").getValue().toString();
                tvNickname.setText(stNickname);

                Log.d(TAG, "Value is: " + value);

                if (TextUtils.isEmpty(stPhoto)) {
                    pbLogin.setVisibility(getView().GONE);
                    return;
                }

                Picasso.with(getActivity()).load(stPhoto).fit().centerInside().into(ivUser, new Callback.EmptyCallback() {
                    @Override
                    public void onSuccess() {
                        // Index 0 is the image view.
                        Log.d(TAG, "SUCCESS");
                        pbLogin.setVisibility(getView().GONE);
                    }
                });
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value
                Log.w(TAG, "Failed to read value.", error.toException());
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
        btnChangePhoto.setOnClickListener(view -> {
            Intent i = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(i,1);
            pbLogin.setVisibility(getView().VISIBLE);
        });

        // 로그아웃 버튼 이벤트
        btnLogout.setOnClickListener(view -> {
            FirebaseAuth.getInstance().signOut();
            Toast.makeText(getActivity(),"로그아웃 되었습니다",Toast.LENGTH_SHORT).show();
            getActivity().finish();
        });

        // 닉네임 변경 버튼 이벤트
        btnChangeNickname.setOnClickListener(view -> {
            AlertDialog.Builder alertdialog = new AlertDialog.Builder(getActivity());
            final EditText etNickname = new EditText(getActivity());
            alertdialog.setTitle("닉네임 변경");
            alertdialog.setView(etNickname);

            alertdialog.setPositiveButton("확인", (dialog, which) -> {
                stNickname = etNickname.getText().toString();
                tvNickname.setText("닉네임 : "+ stNickname);
                myRef.child("users").child(stUid).child("nickname").setValue(stNickname);
            });

            alertdialog.setNegativeButton("취소", (dialog, which) -> { });
            alertdialog.show();
        });

        // 회원탈퇴 버튼 이벤트
        btnWithdrawal.setOnClickListener(view -> {
            AlertDialog.Builder alertdialog = new AlertDialog.Builder(getActivity());
            alertdialog.setMessage("정말 탈퇴하시겠습니까?");

            alertdialog.setPositiveButton("예", (dialog, which) -> user.delete().addOnCompleteListener(task -> {
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

            alertdialog.setNegativeButton("아니오", (dialog, which) -> { });
            alertdialog.show();
        });

        return v;
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
            pbLogin.setVisibility(getView().GONE);
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
        StorageReference profileRef = mStorageRef.child("users").child(stUid+".jpg");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] data = baos.toByteArray();

        UploadTask uploadTask = profileRef.putBytes(data);
        // Handle unsuccessful uploads
        uploadTask.addOnFailureListener(exception -> { }).addOnSuccessListener(taskSnapshot -> {
            // taskSnapshot.getMetadata() contains file metadata such as size, content-type, and download URL.
            Uri downloadUrl = taskSnapshot.getDownloadUrl();
            String photoUrl = String.valueOf(downloadUrl);
            Log.d("url",photoUrl);

            FirebaseDatabase database = FirebaseDatabase.getInstance();
            DatabaseReference myRef = database.getReference("users");

            Hashtable<String, Object> profile = new Hashtable<>();
            profile.put("email", stEmail);
            profile.put("key",stUid);
            profile.put("photo",photoUrl);
            profile.put("nickname",stNickname);

            myRef.child(stUid).updateChildren(profile);
            myRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    String s = dataSnapshot.getValue().toString();
                    Log.d("profile",s);
                    if (dataSnapshot == null ) {
                        return;
                    }

                    Toast.makeText(getActivity(), "사진 업로드 완료",Toast.LENGTH_SHORT).show();
                    ivUser.setImageBitmap(bitmap);
                }

                @Override
                public void onCancelled(DatabaseError databaseError) { }
            });
        });
    }
}
