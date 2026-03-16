package com.android.printspooler.util;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ServiceInfo;
import android.print.PrintManager;
import android.printservice.PrintServiceInfo;
import java.util.List;

public class PrintOptionUtils {
    public static String getAdvancedOptionsActivityName(Context context, ComponentName serviceName) {
        PrintManager printManager = (PrintManager) context.getSystemService("print");
        List<PrintServiceInfo> printServices = printManager.getEnabledPrintServices();
        int printServiceCount = printServices.size();
        for (int i = 0; i < printServiceCount; i++) {
            PrintServiceInfo printServiceInfo = printServices.get(i);
            ServiceInfo serviceInfo = printServiceInfo.getResolveInfo().serviceInfo;
            if (serviceInfo.name.equals(serviceName.getClassName()) && serviceInfo.packageName.equals(serviceName.getPackageName())) {
                return printServiceInfo.getAdvancedOptionsActivityName();
            }
        }
        return null;
    }
}
