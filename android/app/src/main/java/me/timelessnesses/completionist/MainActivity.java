package me.timelessnesses.completionist;

import android.os.Bundle;
import android.webkit.WebSettings;

import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    @Override
    public void onStart() {
        super.onStart();
        getBridge().getWebView().getSettings().setUserAgentString(WebSettings.getDefaultUserAgent(this));
        return;
    }

    @Override
    public void onCreate(Bundle savedInstance) {
        registerPlugin(ChangeIconPlugin.class);
        super.onCreate(savedInstance);
    }
}
