package com.mediatek.calendarimporter;

import android.app.Application;
import com.mediatek.calendarimporter.utils.InjectedServices;

public class CalendarImporterApplication extends Application {
    public static final String TAG = "CalendarImporterApplication";
    private static InjectedServices sInjectedServices;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    public static void injectServices(InjectedServices injectedServices) {
        sInjectedServices = injectedServices;
    }

    @Override
    public Object getSystemService(String str) {
        Object systemService;
        if (sInjectedServices != null && (systemService = sInjectedServices.getSystemService(str)) != null) {
            return systemService;
        }
        return super.getSystemService(str);
    }
}
