package me.timelessnesses.completionist;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class TaskAlarmBootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        switch (intent.getAction()) {
            case Intent.ACTION_BOOT_COMPLETED -> {
                PendingResult pending = goAsync();
                new Thread(() -> {
                    try {
                        TaskAlarmRefresher.refresh(context, null);
                    } catch (Exception error) {
                        TaskAlarmScheduler.restoreStored(context);
                    } finally {
                        pending.finish();
                    }
                }, "task-alarm-restore").start();
            }
            case null -> {}
            default -> throw new IllegalStateException("Unexpected value: " + intent.getAction());
        }
    }
}
