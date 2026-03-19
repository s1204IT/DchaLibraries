package com.mediatek.contacts;

import android.app.Application;
import android.content.Context;
import android.os.SystemProperties;
import com.mediatek.contacts.aassne.SimAasEditor;
import com.mediatek.contacts.aassne.SimSneEditor;
import com.mediatek.contacts.util.Log;

public class GlobalEnv {
    private static Context sContext = null;
    private static SimAasEditor sSimAasEditor = null;
    private static SimSneEditor sSimSneEditor = null;
    private static final boolean IS_TABLET = "tablet".equals(SystemProperties.get("ro.build.characteristics"));

    public static void setApplicationContext(Context context) {
        if (sContext == null) {
            if (context != null && (context instanceof Application)) {
                sContext = context;
                Log.i("GlobalEnv", "[setApplicationContext]sContext: " + sContext);
                return;
            }
            throw new IllegalArgumentException("Only Application context can be set: " + context);
        }
        throw new IllegalStateException("application context could be set only once");
    }

    public static Context getApplicationContext() {
        if (sContext != null) {
            return sContext;
        }
        throw new IllegalStateException("context not set yet");
    }

    public static boolean isUsingTwoPanes() {
        return IS_TABLET;
    }

    public static SimAasEditor getSimAasEditor() {
        return sSimAasEditor;
    }

    public static SimAasEditor setSimAasEditor() {
        if (sSimAasEditor == null) {
            sSimAasEditor = new SimAasEditor(sContext);
            return sSimAasEditor;
        }
        throw new IllegalStateException("sAasExtension context could be set only once");
    }

    public static SimSneEditor getSimSneEditor() {
        return sSimSneEditor;
    }

    public static SimSneEditor setSimSneEditor() {
        if (sSimSneEditor == null) {
            sSimSneEditor = new SimSneEditor(sContext);
            return sSimSneEditor;
        }
        throw new IllegalStateException("sSneExtension context could be set only once");
    }
}
