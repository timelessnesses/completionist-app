package me.timelessnesses.completionist;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import com.getcapacitor.JSArray;

import org.json.JSONArray;
import org.json.JSONObject;

final class TaskAlarmScheduler {
    static final String PREFS = "completionist_task_alarms";
    static final String KEY_ALARMS = "alarms";
    static final String KEY_TOKEN = "sync_token";
    static final String KEY_REFRESH_URL = "refresh_url";
    static final String EXTRA_ALARM = "task_alarm";

    private TaskAlarmScheduler() {}

    static void storeConnection(Context context, String token, String refreshUrl) {
        prefs(context).edit().putString(KEY_TOKEN, token).putString(KEY_REFRESH_URL, refreshUrl).apply();
    }

    static int replaceAll(Context context, JSONArray alarms) {
        cancelStored(context);
        prefs(context).edit().putString(KEY_ALARMS, alarms.toString()).apply();
        int scheduled = 0;
        for (int index = 0; index < alarms.length(); index++) {
            JSONObject alarm = alarms.optJSONObject(index);
            if (alarm != null && schedule(context, alarm)) scheduled++;
        }
        return scheduled;
    }

    static int replaceAll(Context context, JSArray alarms) {
        try {
            return replaceAll(context, new JSONArray(alarms.toString()));
        } catch (Exception error) {
            return 0;
        }
    }

    static void restoreStored(Context context) {
        try {
            replaceAll(context, new JSONArray(prefs(context).getString(KEY_ALARMS, "[]")));
        } catch (Exception ignored) {}
    }

    private static boolean schedule(Context context, JSONObject alarm) {
        long at = alarm.optLong("occurrence_at", 0L);
        String id = alarm.optString("id", "");
        if (id.isEmpty() || at <= System.currentTimeMillis() + 500L) return false;
        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (manager == null) return false;

        PendingIntent operation = alarmIntent(context, id, alarm.toString(), PendingIntent.FLAG_UPDATE_CURRENT);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || manager.canScheduleExactAlarms()) {
            PendingIntent showIntent = PendingIntent.getActivity(
                    context,
                    requestCode(id + ":show"),
                    TaskAlarmActivity.intent(context, alarm.toString()),
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            manager.setAlarmClock(new AlarmManager.AlarmClockInfo(at, showIntent), operation);
        } else {
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, operation);
        }
        return true;
    }

    private static void cancelStored(Context context) {
        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (manager == null) return;
        try {
            JSONArray existing = new JSONArray(prefs(context).getString(KEY_ALARMS, "[]"));
            for (int index = 0; index < existing.length(); index++) {
                JSONObject alarm = existing.optJSONObject(index);
                if (alarm == null) continue;
                String id = alarm.optString("id", "");
                if (!id.isEmpty()) manager.cancel(alarmIntent(context, id, null, PendingIntent.FLAG_NO_CREATE));
            }
        } catch (Exception ignored) {}
    }

    private static PendingIntent alarmIntent(Context context, String id, String payload, int flag) {
        Intent intent = new Intent(context, TaskAlarmReceiver.class).setAction("completionist.TASK_ALARM." + id);
        if (payload != null) intent.putExtra(EXTRA_ALARM, payload);
        return PendingIntent.getBroadcast(
                context,
                requestCode(id),
                intent,
                flag | PendingIntent.FLAG_IMMUTABLE
        );
    }

    static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    static int requestCode(String value) {
        return value.hashCode() & 0x7fffffff;
    }
}
