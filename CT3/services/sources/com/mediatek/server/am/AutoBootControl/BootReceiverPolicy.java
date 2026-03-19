package com.mediatek.server.am.AutoBootControl;

import android.content.Context;
import android.util.Log;
import com.mediatek.internal.R;
import java.util.ArrayList;
import java.util.List;

public class BootReceiverPolicy {
    private static BootReceiverPolicy sInstance = null;
    private String TAG = "BootReceiverPolicy";
    private List<String> mBootIntentFilter = new ArrayList();
    private Context mContext;

    public static BootReceiverPolicy getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new BootReceiverPolicy(context);
        }
        return sInstance;
    }

    private BootReceiverPolicy(Context context) {
        this.mContext = null;
        this.mContext = context;
        initBootIntentFilterList();
    }

    private void initBootIntentFilterList() {
        String[] intentList;
        if (this.mContext == null || (intentList = this.mContext.getResources().getStringArray(R.array.config_auto_boot_policy_intent_list)) == null) {
            return;
        }
        for (String intent : intentList) {
            Log.d(this.TAG, "initBootIntentFilterList() - add monitored intent = " + intent);
            this.mBootIntentFilter.add(intent);
        }
    }

    public List<String> getBootPolicy() {
        return this.mBootIntentFilter;
    }

    public boolean match(String intent) {
        return this.mBootIntentFilter.contains(intent);
    }
}
