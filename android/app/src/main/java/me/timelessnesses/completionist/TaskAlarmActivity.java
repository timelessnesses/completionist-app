package me.timelessnesses.completionist;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONObject;

import java.text.DateFormat;
import java.util.Date;
import java.util.Objects;

public class TaskAlarmActivity extends Activity {
    private int notificationId;

    static Intent intent(Context context, String alarm) {
        return new Intent(context, TaskAlarmActivity.class)
                .putExtra(TaskAlarmScheduler.EXTRA_ALARM, alarm)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setTurnScreenOn(true);
        }
        JSONObject alarm;
        try {
            alarm = new JSONObject(Objects.requireNonNull(getIntent().getStringExtra(TaskAlarmScheduler.EXTRA_ALARM)));
        } catch (Exception error) {
            finish();
            return;
        }
        notificationId = TaskAlarmNotifier.notificationId(alarm);
        setContentView(buildContent(alarm));
    }

    @SuppressLint("SetTextI18n")
    private LinearLayout buildContent(JSONObject alarm) {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(dp(28), dp(64), dp(28), dp(32));
        root.setBackgroundColor(Color.rgb(15, 23, 42));

        TextView label = text("TASK ALARM", 13, Color.rgb(147, 197, 253));
        label.setLetterSpacing(0.14f);
        root.addView(label);

        TextView time = text(DateFormat.getTimeInstance(DateFormat.SHORT).format(new Date()), 54, Color.WHITE);
        time.setTypeface(Typeface.DEFAULT, Typeface.NORMAL);
        root.addView(time, margins(dp(8), dp(32)));

        TextView title = text(alarm.optString("task_name", "Task reminder"), 26, Color.WHITE);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        root.addView(title, margins(dp(0), dp(12)));

        String description = alarm.optString("description", "");
        if (!description.isEmpty() && !"null".equals(description)) {
            TextView body = text(description, 16, Color.rgb(203, 213, 225));
            body.setGravity(Gravity.CENTER);
            root.addView(body, margins(dp(0), dp(28)));
        }

        ViewGroup.LayoutParams spacer = new LinearLayout.LayoutParams(1, 0, 1f);
        TextView stretch = new TextView(this);
        root.addView(stretch, spacer);

        Button dismiss = new Button(this);
        dismiss.setText("Dismiss alarm");
        dismiss.setTextSize(17);
        dismiss.setAllCaps(false);
        dismiss.setOnClickListener(view -> dismissAlarm());
        root.addView(dismiss, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(56)));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                    0,
                    this::onBackPressedThingy
            );
        }
        return root;
    }

    private TextView text(String value, int size, int color) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        return view;
    }

    private LinearLayout.LayoutParams margins(int top, int bottom) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.topMargin = top;
        params.bottomMargin = bottom;
        return params;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void dismissAlarm() {
        TaskAlarmNotifier.dismiss(this, notificationId);
        finishAndRemoveTask();
    }

    public void onBackPressedThingy() {
        dismissAlarm();
    }
}
