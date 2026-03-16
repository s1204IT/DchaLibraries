package com.android.calendar;

import android.content.Context;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ExtensionsFactory {
    private static String TAG = "ExtensionsFactory";
    private static String EXTENSIONS_PROPERTIES = "calendar_extensions.properties";
    private static String ALL_IN_ONE_MENU_KEY = "AllInOneMenuExtensions";
    private static String CLOUD_NOTIFICATION_KEY = "CloudNotificationChannel";
    private static String ANALYTICS_LOGGER_KEY = "AnalyticsLogger";
    private static Properties sProperties = new Properties();
    private static AllInOneMenuExtensionsInterface sAllInOneMenuExtensions = null;
    private static AnalyticsLogger sAnalyticsLogger = null;

    public static void init(AssetManager assetManager) {
        try {
            InputStream fileStream = assetManager.open(EXTENSIONS_PROPERTIES);
            sProperties.load(fileStream);
            fileStream.close();
        } catch (FileNotFoundException e) {
            Log.d(TAG, "No custom extensions.");
        } catch (IOException e2) {
            Log.d(TAG, e2.toString());
        }
    }

    private static <T> T createInstance(String str) {
        try {
            return (T) Class.forName(str).newInstance();
        } catch (ClassNotFoundException e) {
            Log.e(TAG, str + ": unable to create instance.", e);
            return null;
        } catch (IllegalAccessException e2) {
            Log.e(TAG, str + ": unable to create instance.", e2);
            return null;
        } catch (InstantiationException e3) {
            Log.e(TAG, str + ": unable to create instance.", e3);
            return null;
        }
    }

    public static AllInOneMenuExtensionsInterface getAllInOneMenuExtensions() {
        if (sAllInOneMenuExtensions != null) {
            return sAllInOneMenuExtensions;
        }
        String className = sProperties.getProperty(ALL_IN_ONE_MENU_KEY);
        if (className != null) {
            sAllInOneMenuExtensions = (AllInOneMenuExtensionsInterface) createInstance(className);
        } else {
            Log.d(TAG, ALL_IN_ONE_MENU_KEY + " not found in properties file.");
        }
        if (sAllInOneMenuExtensions == null) {
            sAllInOneMenuExtensions = new AllInOneMenuExtensionsInterface() {
                @Override
                public Integer getExtensionMenuResource(Menu menu) {
                    return null;
                }

                @Override
                public boolean handleItemSelected(MenuItem item, Context context) {
                    return false;
                }
            };
        }
        return sAllInOneMenuExtensions;
    }

    public static CloudNotificationBackplane getCloudNotificationBackplane() {
        CloudNotificationBackplane cnb = null;
        String className = sProperties.getProperty(CLOUD_NOTIFICATION_KEY);
        if (className != null) {
            cnb = (CloudNotificationBackplane) createInstance(className);
        } else {
            Log.d(TAG, CLOUD_NOTIFICATION_KEY + " not found in properties file.");
        }
        if (cnb == null) {
            CloudNotificationBackplane cnb2 = new CloudNotificationBackplane() {
                @Override
                public boolean open(Context context) {
                    return true;
                }

                @Override
                public boolean subscribeToGroup(String senderId, String account, String groupId) throws IOException {
                    return true;
                }

                @Override
                public void send(String to, String msgId, Bundle data) {
                }

                @Override
                public void close() {
                }
            };
            return cnb2;
        }
        return cnb;
    }

    public static AnalyticsLogger getAnalyticsLogger(Context context) {
        if (sAnalyticsLogger != null) {
            return sAnalyticsLogger;
        }
        String className = sProperties.getProperty(ANALYTICS_LOGGER_KEY);
        if (className != null) {
            sAnalyticsLogger = (AnalyticsLogger) createInstance(className);
        } else {
            Log.d(TAG, ANALYTICS_LOGGER_KEY + " not found in properties file.");
        }
        if (sAnalyticsLogger == null) {
            sAnalyticsLogger = new AnalyticsLogger() {
                @Override
                public boolean initialize(Context context2) {
                    return true;
                }

                @Override
                public void trackView(String name) {
                }
            };
        }
        sAnalyticsLogger.initialize(context);
        return sAnalyticsLogger;
    }
}
