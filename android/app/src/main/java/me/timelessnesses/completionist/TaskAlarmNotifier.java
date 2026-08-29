package me.timelessnesses.completionist;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import org.json.JSONObject;

final class TaskAlarmNotifier {
    static final String CHANNEL_ID = "task_alarms_v1";

    private TaskAlarmNotifier() {}

    static void show(Context context, JSONObject alarm) {
        createChannel(context);
        int notificationId = notificationId(alarm);
        String raw = alarm.toString();
        PendingIntent fullScreen = PendingIntent.getActivity(
                context,
                notificationId,
                TaskAlarmActivity.intent(context, raw),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        Intent dismissIntent = new Intent(context, TaskAlarmDismissReceiver.class)
                .setAction("completionist.DISMISS_TASK_ALARM." + notificationId)
                .putExtra("notification_id", notificationId);
        PendingIntent dismiss = PendingIntent.getBroadcast(
                context,
                notificationId,
                dismissIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Notification notification = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle(alarm.optString("task_name", "Task reminder"))
                .setContentText("Your scheduled task alarm is ringing")
                .setStyle(new NotificationCompat.BigTextStyle().bigText(alarm.optString("description", "")))
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOngoing(true)
                .setAutoCancel(false)
                .setFullScreenIntent(fullScreen, true)
                .setContentIntent(fullScreen)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Dismiss", dismiss)
                .build();
        notification.flags |= Notification.FLAG_INSISTENT;
        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification);
        } catch (SecurityException ignored) {}
    }

    static void dismiss(Context context, int notificationId) {
        NotificationManagerCompat.from(context).cancel(notificationId);
    }

    static boolean notificationsAllowed(Context context) {
        createChannel(context);
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return false;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return true;
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        NotificationChannel channel = manager == null ? null : manager.getNotificationChannel(CHANNEL_ID);
        return channel != null && channel.getImportance() != NotificationManager.IMPORTANCE_NONE;
    }

    static int notificationId(JSONObject alarm) {
        return TaskAlarmScheduler.requestCode(alarm.optString("id", alarm.toString()));
    }

    static void createChannel(Context context) {
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (manager == null || manager.getNotificationChannel(CHANNEL_ID) != null) return;
        }
        Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        AudioAttributes audio = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        NotificationChannel channel = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Task alarms",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Full-screen alarms for task reminder rules");
            channel.setSound(alarmSound, audio);
            channel.enableVibration(true);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            manager.createNotificationChannel(channel);
        }

    }
}
