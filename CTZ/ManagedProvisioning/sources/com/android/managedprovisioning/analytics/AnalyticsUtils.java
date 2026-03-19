package com.android.managedprovisioning.analytics;

import android.content.Context;
import android.content.Intent;
import android.nfc.NdefRecord;
import android.os.SystemClock;
import com.android.managedprovisioning.parser.PropertiesProvisioningDataParser;
import com.android.managedprovisioning.task.AbstractProvisioningTask;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class AnalyticsUtils {
    public static String getInstallerPackageName(Context context, String str) {
        try {
            return context.getPackageManager().getInstallerPackageName(str);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public Long elapsedRealTime() {
        return Long.valueOf(SystemClock.elapsedRealtime());
    }

    public static List<String> getAllProvisioningExtras(Intent intent) {
        if (intent == null || "com.android.managedprovisioning.action.RESUME_PROVISIONING".equals(intent.getAction())) {
            return new ArrayList();
        }
        if ("android.nfc.action.NDEF_DISCOVERED".equals(intent.getAction())) {
            return getExtrasFromProperties(intent);
        }
        return getExtrasFromBundle(intent);
    }

    public static String getErrorString(AbstractProvisioningTask abstractProvisioningTask, int i) {
        if (abstractProvisioningTask == null) {
            return null;
        }
        return abstractProvisioningTask.getClass().getSimpleName() + ":" + i;
    }

    private static List<String> getExtrasFromBundle(Intent intent) {
        ArrayList arrayList = new ArrayList();
        if (intent != null && intent.getExtras() != null) {
            for (String str : intent.getExtras().keySet()) {
                if (isValidProvisioningExtra(str)) {
                    arrayList.add(str);
                }
            }
        }
        return arrayList;
    }

    private static List<String> getExtrasFromProperties(Intent intent) {
        ArrayList arrayList = new ArrayList();
        NdefRecord firstNdefRecord = PropertiesProvisioningDataParser.getFirstNdefRecord(intent);
        if (firstNdefRecord != null) {
            try {
                Properties properties = new Properties();
                properties.load(new StringReader(new String(firstNdefRecord.getPayload(), StandardCharsets.UTF_8)));
                for (String str : properties.stringPropertyNames()) {
                    if (isValidProvisioningExtra(str)) {
                        arrayList.add(str);
                    }
                }
            } catch (IOException e) {
            }
        }
        return arrayList;
    }

    private static boolean isValidProvisioningExtra(String str) {
        return str != null && str.startsWith("android.app.extra.PROVISIONING_");
    }
}
