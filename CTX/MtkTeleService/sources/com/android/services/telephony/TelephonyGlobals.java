package com.android.services.telephony;

import android.content.Context;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import java.util.ArrayList;
import java.util.List;

public class TelephonyGlobals {
    private static TelephonyGlobals sInstance;
    private final Context mContext;
    private List<TtyManager> mTtyManagers = new ArrayList();

    public TelephonyGlobals(Context context) {
        this.mContext = context.getApplicationContext();
    }

    public static synchronized TelephonyGlobals getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new TelephonyGlobals(context);
        }
        return sInstance;
    }

    public void onCreate() {
        for (Phone phone : PhoneFactory.getPhones()) {
            this.mTtyManagers.add(new TtyManager(this.mContext, phone));
        }
        TelecomAccountRegistry.getInstance(this.mContext).setupOnBoot();
    }
}
