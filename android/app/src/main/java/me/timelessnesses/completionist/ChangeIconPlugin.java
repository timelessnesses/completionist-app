package me.timelessnesses.completionist;

import android.content.ComponentName;
import android.content.pm.PackageManager;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

@CapacitorPlugin(name = "ChangeIconPlugin")
public class ChangeIconPlugin extends Plugin {
    private static final String DARK_ALIAS = "IconDark";
    private static final String LIGHT_ALIAS = "IconLight";
    private String pendingIcon;

    @PluginMethod
    public void setIcon(PluginCall call) {
        String requested = call.getString("name");

        if (!DARK_ALIAS.equals(requested) && !LIGHT_ALIAS.equals(requested)) {
            call.reject("Unknown app icon: " + requested);
            return;
        }

        PackageManager manager = getContext().getPackageManager();
        String packageName = getContext().getPackageName();

        String other = DARK_ALIAS.equals(requested)
                ? LIGHT_ALIAS
                : DARK_ALIAS;

        if (isAliasEnabled(manager, packageName, requested)
                && !isAliasEnabled(manager, packageName, other)) {
            pendingIcon = null;
        } else {
            // Some Android launchers terminate the foreground task when its active
            // alias is disabled. Apply the change after the user backgrounds the app.
            pendingIcon = requested;
        }

        JSObject result = new JSObject();
        result.put("name", requested);
        call.resolve(result);
    }

    @Override
    protected void handleOnStop() {
        super.handleOnStop();
        String requested = pendingIcon;
        if (requested == null) {
            return;
        }
        pendingIcon = null;

        PackageManager manager = getContext().getPackageManager();
        String packageName = getContext().getPackageName();
        String other = DARK_ALIAS.equals(requested)
                ? LIGHT_ALIAS
                : DARK_ALIAS;

        // Enable the replacement before disabling the current launcher so there
        // is always one launchable component.
        setAliasState(manager, packageName, requested, true);
        setAliasState(manager, packageName, other, false);
    }

    private boolean isAliasEnabled(
            PackageManager manager,
            String packageName,
            String alias
    ) {
        ComponentName component =
                new ComponentName(getContext(), packageName + "." + alias);
        int state = manager.getComponentEnabledSetting(component);
        if (state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
            return true;
        }
        if (state == PackageManager.COMPONENT_ENABLED_STATE_DISABLED) {
            return false;
        }
        return LIGHT_ALIAS.equals(alias);
    }

    private void setAliasState(
            PackageManager manager,
            String packageName,
            String alias,
            boolean enabled
    ) {
        ComponentName component =
                new ComponentName(getContext(), packageName + "." + alias);

        manager.setComponentEnabledSetting(
                component,
                enabled
                        ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                        : PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
        );
    }
}
