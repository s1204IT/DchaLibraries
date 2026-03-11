package com.mediatek.systemui.ext;

import android.content.Context;
import android.content.ContextWrapper;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

public class DefaultStatusBarPlmnPlugin extends ContextWrapper implements IStatusBarPlmnPlugin {
    private static final String TAG = "DefaultStatusBarPlmnPlugin";

    public DefaultStatusBarPlmnPlugin(Context context) {
        super(context);
    }

    @Override
    public boolean supportCustomizeCarrierLabel() {
        Log.d(TAG, "into supportCustomizeCarrierLabel: false");
        return false;
    }

    @Override
    public View customizeCarrierLabel(ViewGroup parentView, View orgCarrierLabel) {
        Log.d(TAG, "into customizeCarrierLabel: null");
        return null;
    }

    @Override
    public void updateCarrierLabelVisibility(boolean force, boolean makeVisible) {
        Log.d(TAG, "into updateCarrierLabelVisibility");
    }

    @Override
    public void updateCarrierLabel(int slotId, boolean isSimInserted, boolean isHasSimService, String[] networkNames) {
        Log.d(TAG, "into updateCarrierLabel, slotId=" + slotId + ", isSimInserted=" + isSimInserted + ", isHasSimService=" + isHasSimService);
    }

    @Override
    public void addPlmn(LinearLayout statusBarContents, Context contx) {
        Log.d(TAG, "into addPlmn");
    }

    @Override
    public void setPlmnVisibility(int visibility) {
        Log.d(TAG, "setPlmnVisibility");
    }
}
