package me.timelessnesses.completionist;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class TaskAlarmDismissReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        TaskAlarmNotifier.dismiss(context, intent.getIntExtra("notification_id", 0));
    }
}
