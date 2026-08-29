package me.timelessnesses.completionist;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.json.JSONObject;

public class TaskAlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String raw = intent.getStringExtra(TaskAlarmScheduler.EXTRA_ALARM);
        if (raw == null) return;
        PendingResult pending = goAsync();
        new Thread(() -> {
            try {
                JSONObject cached = new JSONObject(raw);
                JSONObject verified = TaskAlarmRefresher.refresh(context, cached);
                if (verified != null) TaskAlarmNotifier.show(context, verified);
            } catch (Exception ignored) {
                try {
                    TaskAlarmNotifier.show(context, new JSONObject(raw));
                } catch (Exception ignoredAgain) {}
            } finally {
                pending.finish();
            }
        }, "task-alarm-refresh").start();
    }
}
