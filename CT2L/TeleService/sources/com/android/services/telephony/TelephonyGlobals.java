package com.android.services.telephony;

import android.content.Context;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import java.util.ArrayList;
import java.util.List;

public class TelephonyGlobals {
    private final Context mContext;
    private List<TtyManager> mTtyManagers = new ArrayList();

    public TelephonyGlobals(Context context) {
        this.mContext = context.getApplicationContext();
    }

    public void onCreate() {
        Phone[] phones = PhoneFactory.getPhones();
        for (Phone phone : phones) {
            this.mTtyManagers.add(new TtyManager(this.mContext, phone));
        }
        TelecomAccountRegistry.getInstance(this.mContext).setupOnBoot();
    }
}
