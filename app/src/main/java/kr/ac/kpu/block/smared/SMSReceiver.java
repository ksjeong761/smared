package kr.ac.kpu.block.smared;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.widget.Toast;

// https://tfincoming.tistory.com/1
// BroadcaseReceiver를 사용해 SMS를 받았을 경우 이벤트를 동작시킨다.
public class SMSReceiver extends BroadcastReceiver {
    private FormattedLogger logger = new FormattedLogger();

    @Override
    public void onReceive(Context context, Intent intent) {
        // SMS 수신 이외의 이벤트는 무시한다.
        final String ACTION = "android.provider.Telephony.SMS_RECEIVED";
        if (!intent.getAction().equals(ACTION)) {
            return;
        }

        // SMS 처리
        SmsMessage[] smsMessages = Telephony.Sms.Intents.getMessagesFromIntent(intent);
        for (SmsMessage smsMessage : smsMessages) {
            // 푸시 알림 권한 요청
            NotificationManager notificationManager = (NotificationManager)context.getSystemService(context.NOTIFICATION_SERVICE);
            if (notificationManager == null) {
                return;
            }

            // 푸시 알림을 눌렀을 때 실행할 액티비티, 추가로 수행할 동작을 설정한다.
            Intent nextIntent = new Intent(context.getApplicationContext(), TabActivity.class);
            nextIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP  // 현재 액티비티를 최상단으로 올린다
                    | Intent.FLAG_ACTIVITY_CLEAR_TOP);                      // 최상단 액티비티를 제외하고 모든 액티비티를 제거한다

            // 푸시 알림을 눌렀을 때 실행할 액티비티에 데이터를 전달한다.
            nextIntent.putExtra("sms", smsMessage.getMessageBody());
            nextIntent.putExtra("smsdate", smsMessage.getTimestampMillis());
            PendingIntent pendnoti = PendingIntent.getActivity(context, 0, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT);

            // 푸시 알림 상세를 설정한다.
            Notification.Builder builder = new Notification.Builder(context.getApplicationContext());
            builder.setContentIntent(pendnoti)                // 푸시 알림 터치시 실행할 작업 인텐트 설정
                .setSmallIcon(R.drawable.logo)                  // 푸시 알림 왼쪽 아이콘
                .setWhen(System.currentTimeMillis())       // 푸시 알림 시간 miliSecond 단위 설정
                .setNumber(1)                                           // 확인하지 않은 알림 개수 표시 설정
                .setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE)    // 소리와 진동으로 알림
                .setAutoCancel(true)                                // 푸시 알림 터치시 자동 삭제 설정
                .setOngoing(true)                                    // 푸시 알림을 지속적으로 띄울 것인지 설정
                .setTicker("Ticker")                                    // 푸시 알림 발생시 잠깐 나오는 텍스트
                .setContentTitle("SmaRed")                       // 푸시 알림 상단 텍스트(제목)
                .setContentText("[신한체크카드 사용] 가계부에 추가하시겠습니까?");                   // 푸시 알림 내용

            // 푸시 알림 보내기
            notificationManager.notify(1, builder.build());

            // [DEBUG]
            //Toast.makeText(context, smsMessage.getMessageBody(), Toast.LENGTH_SHORT).show();
        }
    }
}







