package kr.ac.kpu.block.smared;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

public class PermissionChecker {
    private Activity callerActivity;
    private String[] necessaryPermissions;

    private final int MULTIPLE_PERMISSIONS = 101;

    public PermissionChecker(Activity callerActivity, String[] necessaryPermissions) {
        this.callerActivity = callerActivity;
        this.necessaryPermissions = necessaryPermissions;
    }

    // 기능 실행에 부족한 권한이 있다면 요청한다.
    public void requestLackingPermissions() {
        String[] lackingPermissions = collectLackingPermissions();
        if (lackingPermissions.length > 0) {
            ActivityCompat.requestPermissions(callerActivity, lackingPermissions, MULTIPLE_PERMISSIONS);
        }
    }

    // 권한 요청 후 응답을 받는 콜백 함수에서 요청했던 모든 권한이 허용되었는지 다시 확인한다.
    public boolean isPermissionRequestSuccessful(int[] requestResults) {
        for (int requestResult : requestResults) {
            if (requestResult != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }

        return true;
    }

    private boolean isPermissionGranted(String permission) {
        return (ContextCompat.checkSelfPermission(callerActivity, permission) == PackageManager.PERMISSION_GRANTED);
    }

    private String[] collectLackingPermissions() {
        List<String> lackingPermissions = new ArrayList<>();
        for (String necessaryPermission : necessaryPermissions) {
            if (!isPermissionGranted(necessaryPermission)) {
                lackingPermissions.add(necessaryPermission);
            }
        }

        return lackingPermissions.toArray(new String[lackingPermissions.size()]);
    }
}
