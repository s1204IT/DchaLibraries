package com.android.settings.print;

import android.content.ComponentName;
import android.content.Context;
import android.provider.Settings;
import android.text.TextUtils;
import java.util.ArrayList;
import java.util.List;

public class PrintSettingsUtils {
    public static List<ComponentName> readEnabledPrintServices(Context context) {
        List<ComponentName> enabledServices = new ArrayList<>();
        String enabledServicesSetting = Settings.Secure.getString(context.getContentResolver(), "enabled_print_services");
        if (enabledServicesSetting != null) {
            TextUtils.SimpleStringSplitter colonSplitter = new TextUtils.SimpleStringSplitter(':');
            colonSplitter.setString(enabledServicesSetting);
            while (colonSplitter.hasNext()) {
                String componentNameString = colonSplitter.next();
                ComponentName enabledService = ComponentName.unflattenFromString(componentNameString);
                enabledServices.add(enabledService);
            }
        }
        return enabledServices;
    }

    public static void writeEnabledPrintServices(Context context, List<ComponentName> services) {
        StringBuilder builder = new StringBuilder();
        int serviceCount = services.size();
        for (int i = 0; i < serviceCount; i++) {
            ComponentName service = services.get(i);
            if (builder.length() > 0) {
                builder.append(':');
            }
            builder.append(service.flattenToString());
        }
        Settings.Secure.putString(context.getContentResolver(), "enabled_print_services", builder.toString());
    }
}
