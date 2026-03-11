package com.mediatek.systemui.statusbar.policy;

import android.content.Context;
import android.util.Log;
import com.mediatek.hotknot.HotKnotAdapter;

public class HotKnotControllerImpl implements HotKnotController {
    private final Context mContext;
    private HotKnotAdapter mHotKnotAdapter;

    public HotKnotControllerImpl(Context context) {
        this.mContext = context;
        Log.d("HotKnotController", "new HotKnotController()");
    }

    @Override
    public boolean isHotKnotOn() {
        if (getAdapter() != null) {
            return getAdapter().isEnabled();
        }
        return false;
    }

    @Override
    public HotKnotAdapter getAdapter() {
        if (this.mHotKnotAdapter == null) {
            try {
                this.mHotKnotAdapter = HotKnotAdapter.getDefaultAdapter(this.mContext);
            } catch (IllegalArgumentException e) {
                Log.e("HotKnotController", "getDefaultAdapter exception");
            }
        }
        return this.mHotKnotAdapter;
    }
}
