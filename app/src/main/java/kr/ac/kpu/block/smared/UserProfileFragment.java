package kr.ac.kpu.block.smared;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import kr.ac.kpu.block.smared.databinding.FragmentUserProfileBinding;

public class UserProfileFragment extends Fragment {
    private FormattedLogger logger = new FormattedLogger();
    private FragmentUserProfileBinding viewBinding;
    private PermissionChecker permissionChecker;

    private UserInfoSingleton userInfoSingleton = UserInfoSingleton.getInstance();
    private UserInfo userInfo = userInfoSingleton.getCurrentUserInfo();

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if (!permissionChecker.isPermissionRequestSuccessful(grantResults)) {
            Toast.makeText(getActivity(), "권한 요청에 동의 해주셔야 이용 가능합니다. 설정에서 권한을 허용해주시기 바랍니다.", Toast.LENGTH_SHORT).show();
            getActivity().finish();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        viewBinding = FragmentUserProfileBinding.inflate(inflater, container, false);

        final String[] necessaryPermissions = {
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        };
        permissionChecker = new PermissionChecker(getActivity(), necessaryPermissions);
        permissionChecker.requestLackingPermissions();

        // UI 출력
        // [TODO] 하드코딩된 다른 정보들 바꾸기
        viewBinding.tvNickname.setText(userInfo.getNickname());

        // 이벤트 등록
        viewBinding.btnChangePhoto.setOnClickListener(view -> startActivityForResult(new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI),1));
        viewBinding.btnLogout.setOnClickListener(view -> logout());
        viewBinding.btnChangeNickname.setOnClickListener(view -> showChangeNicknameDialog());
        viewBinding.btnWithdrawal.setOnClickListener(view -> showWithdrawalDialog());

        return viewBinding.getRoot();
    }

    // 변경할 프로필 사진 파일 업로드
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent previousIntent) {
        super.onActivityResult(requestCode, resultCode, previousIntent);

        try {
            Uri image = previousIntent.getData();
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getActivity().getContentResolver(),image);
            viewBinding.ivUser.setImageBitmap(bitmap);
            uploadImage(bitmap);
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private String createImageFileName() {
        StringBuilder imageFileName = new StringBuilder();

        imageFileName.append(System.currentTimeMillis());
        imageFileName.append( ".jpg");

        return imageFileName.toString();
    }

    private void uploadImage(Bitmap bitmap) {
        ByteArrayOutputStream bitmapByteStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bitmapByteStream);
        byte[] bitmapByte = bitmapByteStream.toByteArray();

        // 사용자 정보 테이블에 이미지 업로드 시도
        StorageDAO storageDAO = new StorageDAO();
        storageDAO.setSuccessCallback(arg -> afterSuccess(arg));
        storageDAO.setFailureCallback(arg -> afterFailure());
        storageDAO.upload(userInfo, UserInfo.class, createImageFileName(), bitmapByte);
    }

    private void afterSuccess(UploadTask.TaskSnapshot taskSnapshot) {
        UserInfoSingleton userInfoSingleton = UserInfoSingleton.getInstance();
        UserInfo userInfo = userInfoSingleton.getCurrentUserInfo();
        userInfo.setNickname(viewBinding.tvNickname.getText().toString());
        userInfo.setPhotoUri(String.valueOf(taskSnapshot.getDownloadUrl()));

        DAO dao = new DAO();
        dao.setSuccessCallback(arg -> Toast.makeText(getActivity(), "사진 업로드 완료", Toast.LENGTH_SHORT).show());
        dao.setFailureCallback(arg -> afterFailure());
        dao.update(userInfo, UserInfo.class);
    }

    private void afterFailure() {
        Toast.makeText(getActivity(), "사진 업로드 실패!", Toast.LENGTH_SHORT).show();
    }

    private void logout() {
        UserAuthDAO userAuthDAO = new UserAuthDAO();
        userAuthDAO.setSuccessCallback(arg -> {
            // 싱글톤이 가진 사용자 정보 초기화
            userInfo = new UserInfo();
            Toast.makeText(getActivity(),"로그아웃 되었습니다",Toast.LENGTH_SHORT).show();
            getActivity().finish();
        });
        userAuthDAO.setFailureCallback(arg -> { });
        userAuthDAO.signOut();
    }

    // [TODO] 하드코딩된 연관 테이블 접근 방식을 바꿔야 한다.
    // [TODO] DB 접근 작업이 연달아서 일어나므로 작업 실패 시 트랜잭션 등 롤백할 방법을 찾아야 한다.
    private void withdrawal() {
        // 연관된 DB에서 데이터를 삭제한다.
        userInfo.getLedgersUid().forEach((ledgerUid, dummy) -> {
            DAO dao = new DAO();
            dao.setSuccessCallback(dataSnapshot -> {
                DAO ledgerDAO = new DAO();
                String databasePath = Ledger.class.getSimpleName() + "/" + ledgerUid + "/" + "membersUid" + "/" + userInfo.getUid();
                ledgerDAO.delete(databasePath);
            });
            dao.readAll(ledgerUid, Ledger.class);
        });

        // 연관된 DB에서 데이터를 삭제한다.
        userInfo.getFriendsUid().forEach((friendUid, dummy) -> {
            DAO dao = new DAO();
            dao.setSuccessCallback(dataSnapshot -> {
                DAO friendDAO = new DAO();
                String databasePath = UserInfo.class.getSimpleName() + "/" + friendUid + "/" + "friendsUid" + "/" + userInfo.getUid();
                friendDAO.delete(databasePath);
            });
            dao.readAll(friendUid, UserInfo.class);
        });

        // 연관된 DB에서 데이터를 삭제한다.
        userInfo.getChatRoomsUid().forEach((chatRoomUid, dummy) -> {
            DAO dao = new DAO();
            dao.setSuccessCallback(dataSnapshot -> {
                DAO chatRoomDAO = new DAO();
                String databasePath = ChatRoom.class.getSimpleName() + "/" + chatRoomUid + "/" + "membersUid" + "/" + userInfo.getUid();
                chatRoomDAO.delete(databasePath);
            });
            dao.readAll(chatRoomUid, ChatRoom.class);
        });

        // 사용자 정보 DB에서 데이터를 삭제한다.
        DAO dao = new DAO();
        dao.setSuccessCallback(arg -> {});
        dao.setFailureCallback(arg -> {});
        dao.delete(userInfo, UserInfo.class);

        // 사용자 인증 DB에서 데이터를 삭제한다.
        UserAuthDAO userAuthDAO = new UserAuthDAO();
        userAuthDAO.setSuccessCallback(arg -> {});
        userAuthDAO.setFailureCallback(arg -> {});
        userAuthDAO.removeCurrentAccount();

        // 싱글톤이 가진 사용자 정보 초기화
        userInfo = new UserInfo();
        Toast.makeText(getActivity(),"계정이 삭제되었습니다.",Toast.LENGTH_SHORT).show();
        getActivity().finish();
    }

    private void showWithdrawalDialog() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity());
        alertDialog.setMessage("정말 탈퇴하시겠습니까?");

        alertDialog.setPositiveButton("예", (dialog, which) -> withdrawal());
        alertDialog.setNegativeButton("아니오", (dialog, which) -> {});

        alertDialog.show();
    }

    private void showChangeNicknameDialog() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity());
        final EditText etNickname = new EditText(getActivity());
        alertDialog.setTitle("닉네임 변경");
        alertDialog.setView(etNickname);

        alertDialog.setPositiveButton("확인", (dialog, which) -> {
            String newNickname = etNickname.getText().toString();
            userInfo.setNickname(newNickname);
            viewBinding.tvNickname.setText("닉네임 : " + newNickname);

            DAO dao = new DAO();
            dao.setSuccessCallback((arg) -> {});
            dao.setFailureCallback((arg) -> {});
            dao.update(userInfo, UserInfo.class);
        });

        alertDialog.setNegativeButton("취소", (dialog, which) -> { });
        alertDialog.show();
    }
}
