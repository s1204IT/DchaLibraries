package com.android.deskclock;

import android.content.Context;
import android.content.res.AssetManager;
import com.android.deskclock.provider.Alarm;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ExtensionsFactory {
    private static String TAG = "ExtensionsFactory";
    private static String EXTENSIONS_PROPERTIES = "deskclock_extensions.properties";
    private static String DESKCLOCKEXTENSIONS_KEY = "DeskclockExtensions";
    private static Properties sProperties = new Properties();
    private static DeskClockExtensions sDeskClockExtensions = null;

    public static void init(AssetManager assetManager) {
        try {
            InputStream fileStream = assetManager.open(EXTENSIONS_PROPERTIES);
            sProperties.load(fileStream);
            fileStream.close();
        } catch (FileNotFoundException e) {
            if (android.util.Log.isLoggable(TAG, 3)) {
                android.util.Log.d(TAG, "No custom extensions.");
            }
        } catch (IOException e2) {
            if (android.util.Log.isLoggable(TAG, 3)) {
                android.util.Log.d(TAG, e2.toString());
            }
        }
    }

    private static <T> T createInstance(String str) {
        try {
            return (T) Class.forName(str).newInstance();
        } catch (ClassNotFoundException e) {
            if (android.util.Log.isLoggable(TAG, 6)) {
                android.util.Log.e(TAG, str + ": unable to create instance.", e);
            }
            return null;
        } catch (IllegalAccessException e2) {
            if (android.util.Log.isLoggable(TAG, 6)) {
                android.util.Log.e(TAG, str + ": unable to create instance.", e2);
            }
            return null;
        } catch (InstantiationException e3) {
            if (android.util.Log.isLoggable(TAG, 6)) {
                android.util.Log.e(TAG, str + ": unable to create instance.", e3);
            }
            return null;
        }
    }

    public static DeskClockExtensions getDeskClockExtensions() {
        if (sDeskClockExtensions != null) {
            return sDeskClockExtensions;
        }
        String className = sProperties.getProperty(DESKCLOCKEXTENSIONS_KEY);
        if (className != null) {
            sDeskClockExtensions = (DeskClockExtensions) createInstance(className);
        } else if (android.util.Log.isLoggable(TAG, 3)) {
            android.util.Log.d(TAG, DESKCLOCKEXTENSIONS_KEY + " not found in properties file.");
        }
        if (sDeskClockExtensions == null) {
            sDeskClockExtensions = new DeskClockExtensions() {
                @Override
                public void addAlarm(Context context, Alarm newAlarm) {
                    if (android.util.Log.isLoggable(ExtensionsFactory.TAG, 3)) {
                        android.util.Log.d(ExtensionsFactory.TAG, "Add alarm: Empty inline implementation called.");
                    }
                }

                @Override
                public void deleteAlarm(Context context, long alarmId) {
                    if (android.util.Log.isLoggable(ExtensionsFactory.TAG, 3)) {
                        android.util.Log.d(ExtensionsFactory.TAG, "Delete alarm: Empty inline implementation called.");
                    }
                }
            };
        }
        return sDeskClockExtensions;
    }
}
