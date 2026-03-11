package com.android.systemui;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import java.io.FileDescriptor;
import java.io.PrintWriter;

public class SystemUISecondaryUserService extends Service {
    @Override
    public void onCreate() {
        super.onCreate();
        ((SystemUIApplication) getApplication()).startSecondaryUserServicesIfNeeded();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        SystemUI[] services = ((SystemUIApplication) getApplication()).getServices();
        if (args == null || args.length == 0) {
            for (SystemUI ui : services) {
                if (ui != null) {
                    pw.println("dumping service: " + ui.getClass().getName());
                    ui.dump(fd, pw, args);
                }
            }
            return;
        }
        String svc = args[0];
        for (SystemUI ui2 : services) {
            if (ui2 != null) {
                String name = ui2.getClass().getName();
                if (name.endsWith(svc)) {
                    ui2.dump(fd, pw, args);
                }
            }
        }
    }
}
