package me.timelessnesses.completionist;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import androidx.core.content.ContextCompat;
import androidx.core.content.IntentCompat;
import androidx.core.content.PackageManagerCompat;
import androidx.core.content.UnusedAppRestrictionsConstants;

import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.google.common.util.concurrent.ListenableFuture;

@CapacitorPlugin(name = "TaskAlarm")
public class TaskAlarmPlugin extends Plugin {
    private static final int UNUSED_APP_RESTRICTIONS_REQUEST = 4102;

    @PluginMethod
    public void sync(PluginCall call) {
        JSArray alarms = call.getArray("alarms", new JSArray());
        String syncToken = call.getString("syncToken");
        String refreshUrl = call.getString("refreshUrl");
        if (syncToken == null || refreshUrl == null) {
            call.reject("syncToken and refreshUrl are required");
            return;
        }

        TaskAlarmScheduler.storeConnection(getContext(), syncToken, refreshUrl);
        int scheduled = TaskAlarmScheduler.replaceAll(getContext(), alarms);
        JSObject result = new JSObject();
        result.put("scheduled", scheduled);
        result.put("notificationsAllowed", TaskAlarmNotifier.notificationsAllowed(getContext()));
        result.put("exactAlarmAllowed", exactAlarmAllowed());
        result.put("fullScreenAllowed", fullScreenAllowed());
        call.resolve(result);
    }

    @PluginMethod
    public void openSettings(PluginCall call) {
        Intent intent;
        if (!TaskAlarmNotifier.notificationsAllowed(getContext())) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                intent = new Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, getContext().getPackageName())
                        .putExtra(Settings.EXTRA_CHANNEL_ID, TaskAlarmNotifier.CHANNEL_ID);
            } else {
                intent = new Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:" + getContext().getPackageName())
                );
            }
        } else if (!exactAlarmAllowed() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            intent = new Intent(
                    Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                    Uri.parse("package:" + getContext().getPackageName())
            );
        } else if (!fullScreenAllowed() && Build.VERSION.SDK_INT >= 34) {
            intent = new Intent(
                    Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
                    Uri.parse("package:" + getContext().getPackageName())
            );
        } else {
            intent = new Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:" + getContext().getPackageName())
            );
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        getContext().startActivity(intent);
        call.resolve();
    }

    @PluginMethod
    public void openUnusedAppSettings(PluginCall call) {
        ListenableFuture<Integer> statusFuture =
                PackageManagerCompat.getUnusedAppRestrictionsStatus(getContext());
        statusFuture.addListener(() -> {
            try {
                int status = statusFuture.get();
                if (status == UnusedAppRestrictionsConstants.FEATURE_NOT_AVAILABLE) {
                    call.reject("Unused app restrictions are not available on this device");
                    return;
                }

                Intent intent = IntentCompat.createManageUnusedAppRestrictionsIntent(
                        getContext(),
                        getContext().getPackageName()
                );
                getActivity().startActivityForResult(intent, UNUSED_APP_RESTRICTIONS_REQUEST);

                JSObject result = new JSObject();
                result.put("alreadyDisabled", status == UnusedAppRestrictionsConstants.DISABLED);
                call.resolve(result);
            } catch (Exception error) {
                call.reject("Unable to open unused app restriction settings", error);
            }
        }, ContextCompat.getMainExecutor(getContext()));
    }

    private boolean exactAlarmAllowed() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true;
        AlarmManager manager = getContext().getSystemService(AlarmManager.class);
        return manager != null && manager.canScheduleExactAlarms();
    }

    private boolean fullScreenAllowed() {
        if (Build.VERSION.SDK_INT < 34) return true;
        NotificationManager manager = getContext().getSystemService(NotificationManager.class);
        return manager != null && manager.canUseFullScreenIntent();
    }
}
