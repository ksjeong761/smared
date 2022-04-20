package kr.ac.kpu.block.smared;

import android.util.Log;

public class FormattedLogger {
    private FormattedLogger logger = new FormattedLogger();
    private String TAG = getClass().getSimpleName();

    private String getCallerClassName() {
        return Thread.currentThread().getStackTrace()[4].getClass().getSimpleName();
    }

    private String getCallerMethodName() {
        return Thread.currentThread().getStackTrace()[4].getMethodName();
    }

    public void writeLog(String logMessage) {
        Log.d(TAG, String.format("<Smared> [%s - %s()] %s", getCallerClassName(), getCallerMethodName(), logMessage));
    }

    // 로그 메시지를 작성하지 않았을 경우 디폴트 값을 채워서 로깅 함수 호출
    public void writeLog() {
        writeLog("");
    }
}
