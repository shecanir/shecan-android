package ir.shecan;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class MyFirebaseMessagingService extends FirebaseMessagingService {
    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        if (remoteMessage.getNotification() != null) {
//            showNotification(remoteMessage.notification?.title, remoteMessage.notification?.body)
            Log.d("FCM", "Notification Title: ${remoteMessage.notification?.title}");
            Log.d("FCM", "Notification Body: ${remoteMessage.notification?.body}");
        }
    }

    @Override
    public void onNewToken(@NonNull String s) {
        super.onNewToken(s);
        Log.d("FCM", "New token: $token");
    }
}
