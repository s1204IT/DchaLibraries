package com.android.phone;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import com.android.internal.telephony.DebugService;
import java.io.FileDescriptor;
import java.io.PrintWriter;

public class TelephonyDebugService extends Service {
    private static final boolean DBG = true;
    private static String TAG = "TelephonyDebugService";
    private DebugService mDebugService = new DebugService();

    public TelephonyDebugService() {
        Log.d(TAG, "TelephonyDebugService()");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    protected void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        this.mDebugService.dump(fileDescriptor, printWriter, strArr);
    }
}
