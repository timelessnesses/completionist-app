package me.timelessnesses.completionist;

import android.text.format.DateFormat;

import androidx.appcompat.app.AppCompatActivity;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.concurrent.atomic.AtomicBoolean;

@CapacitorPlugin(name = "ModernPicker")
public class ModernPickerPlugin extends Plugin {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;

    @PluginMethod
    public void pickDate(PluginCall call) {
        getActivity().runOnUiThread(() -> {
            AppCompatActivity activity = (AppCompatActivity) getActivity();
            MaterialDatePicker.Builder<Long> builder = MaterialDatePicker.Builder.datePicker()
                    .setTheme(com.google.android.material.R.style.ThemeOverlay_Material3_MaterialCalendar)
                    .setTitleText(call.getString("title", "Select date"));

            String currentValue = call.getString("value");
            if (currentValue != null && !currentValue.isBlank()) {
                try {
                    long selectedDay = LocalDate.parse(currentValue, DATE_FORMAT)
                            .atStartOfDay(ZoneOffset.UTC)
                            .toInstant()
                            .toEpochMilli();
                    builder.setSelection(selectedDay);
                } catch (DateTimeParseException ignored) {
                    // MaterialDatePicker will use its default selection.
                }
            }

            MaterialDatePicker<Long> picker = builder.build();
            AtomicBoolean finished = new AtomicBoolean(false);
            picker.addOnPositiveButtonClickListener(selection -> {
                if (selection == null) {
                    resolveCancelled(call, finished);
                    return;
                }
                if (!finished.compareAndSet(false, true)) return;
                LocalDate selectedDate = Instant.ofEpochMilli(selection)
                        .atZone(ZoneOffset.UTC)
                        .toLocalDate();
                resolveValue(call, DATE_FORMAT.format(selectedDate));
            });
            picker.addOnNegativeButtonClickListener(view -> resolveCancelled(call, finished));
            picker.addOnCancelListener(dialog -> resolveCancelled(call, finished));
            picker.show(activity.getSupportFragmentManager(), "completionist-date-picker");
        });
    }

    @PluginMethod
    public void pickTime(PluginCall call) {
        getActivity().runOnUiThread(() -> {
            AppCompatActivity activity = (AppCompatActivity) getActivity();
            int hour = 9;
            int minute = 0;
            String currentValue = call.getString("value");
            if (currentValue != null) {
                String[] parts = currentValue.split(":", 3);
                try {
                    if (parts.length >= 2) {
                        hour = Math.max(0, Math.min(23, Integer.parseInt(parts[0])));
                        minute = Math.max(0, Math.min(59, Integer.parseInt(parts[1])));
                    }
                } catch (NumberFormatException ignored) {
                    hour = 9;
                    minute = 0;
                }
            }

            int timeFormat = DateFormat.is24HourFormat(getContext())
                    ? TimeFormat.CLOCK_24H
                    : TimeFormat.CLOCK_12H;
            MaterialTimePicker picker = new MaterialTimePicker.Builder()
                    .setTheme(com.google.android.material.R.style.ThemeOverlay_Material3_MaterialTimePicker)
                    .setTimeFormat(timeFormat)
                    .setHour(hour)
                    .setMinute(minute)
                    .setTitleText(call.getString("title", "Select time"))
                    .build();
            AtomicBoolean finished = new AtomicBoolean(false);
            picker.addOnPositiveButtonClickListener(view -> {
                if (!finished.compareAndSet(false, true)) return;
                resolveValue(call, String.format("%02d:%02d", picker.getHour(), picker.getMinute()));
            });
            picker.addOnNegativeButtonClickListener(view -> resolveCancelled(call, finished));
            picker.addOnCancelListener(dialog -> resolveCancelled(call, finished));
            picker.show(activity.getSupportFragmentManager(), "completionist-time-picker");
        });
    }

    private void resolveValue(PluginCall call, String value) {
        JSObject result = new JSObject();
        result.put("value", value);
        result.put("cancelled", false);
        call.resolve(result);
    }

    private void resolveCancelled(PluginCall call, AtomicBoolean finished) {
        if (!finished.compareAndSet(false, true)) return;
        JSObject result = new JSObject();
        result.put("cancelled", true);
        call.resolve(result);
    }
}
