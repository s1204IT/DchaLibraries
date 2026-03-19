package com.android.server;

import android.R;
import android.content.Context;
import android.hardware.ISerialManager;
import android.os.ParcelFileDescriptor;
import java.io.File;
import java.util.ArrayList;

public class SerialService extends ISerialManager.Stub {
    private final Context mContext;
    private final String[] mSerialPorts;

    private native ParcelFileDescriptor native_open(String str);

    public SerialService(Context context) {
        this.mContext = context;
        this.mSerialPorts = context.getResources().getStringArray(R.array.config_defaultNotificationVibeWaveform);
    }

    public String[] getSerialPorts() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.SERIAL_PORT", null);
        ArrayList arrayList = new ArrayList();
        for (int i = 0; i < this.mSerialPorts.length; i++) {
            String str = this.mSerialPorts[i];
            if (new File(str).exists()) {
                arrayList.add(str);
            }
        }
        String[] strArr = new String[arrayList.size()];
        arrayList.toArray(strArr);
        return strArr;
    }

    public ParcelFileDescriptor openSerialPort(String str) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.SERIAL_PORT", null);
        for (int i = 0; i < this.mSerialPorts.length; i++) {
            if (this.mSerialPorts[i].equals(str)) {
                return native_open(str);
            }
        }
        throw new IllegalArgumentException("Invalid serial port " + str);
    }
}
