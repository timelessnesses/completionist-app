package me.timelessnesses.completionist;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

final class TaskAlarmRefresher {
    private TaskAlarmRefresher() {}

    static JSONObject refresh(Context context, JSONObject current) throws Exception {
        SharedPreferences preferences = TaskAlarmScheduler.prefs(context);
        String endpoint = preferences.getString(TaskAlarmScheduler.KEY_REFRESH_URL, null);
        String token = preferences.getString(TaskAlarmScheduler.KEY_TOKEN, null);
        if (endpoint == null || token == null) throw new IllegalStateException("Alarm sync is not configured");

        HttpURLConnection connection = (HttpURLConnection) new URL(endpoint).openConnection();
        connection.setRequestMethod("POST");
        connection.setConnectTimeout(4_000);
        connection.setReadTimeout(4_000);
        connection.setRequestProperty("Authorization", "Bearer " + token);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);
        JSONObject request = new JSONObject();
        if (current != null) {
            request.put("task_id", current.optString("task_id"));
            request.put("rule_key", current.optString("rule_key"));
            request.put("occurrence_at", current.optLong("occurrence_at"));
        }
        try (OutputStream output = connection.getOutputStream()) {
            output.write(request.toString().getBytes(StandardCharsets.UTF_8));
        }
        if (connection.getResponseCode() < 200 || connection.getResponseCode() >= 300) {
            throw new IllegalStateException("Alarm refresh returned " + connection.getResponseCode());
        }
        StringBuilder body = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) body.append(line);
        } finally {
            connection.disconnect();
        }

        JSONObject response = new JSONObject(body.toString());
        JSONArray alarms = response.optJSONArray("alarms");
        if (alarms == null) alarms = new JSONArray();
        String nextToken = response.optString("sync_token", token);
        preferences.edit().putString(TaskAlarmScheduler.KEY_TOKEN, nextToken).apply();
        TaskAlarmScheduler.replaceAll(context, alarms);
        if (current == null || !response.optBoolean("current_valid", false)) return null;
        return response.optJSONObject("current_alarm");
    }
}
