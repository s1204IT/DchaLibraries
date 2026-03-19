package com.mediatek.common;

import android.content.Context;
import android.content.res.Resources;
import android.util.Log;
import android.view.LayoutInflater;

public final class ResourceHelper {
    private static final String TAG = "PluginResourceHelper";
    Context mContext;
    LayoutInflater mLayoutInflater;
    String mPackageName;
    Resources mResources;

    public ResourceHelper(String str, Context context) {
        if (context != null) {
            try {
                this.mContext = context.createPackageContext(str, 3);
                this.mPackageName = str;
                this.mResources = this.mContext.getResources();
                this.mLayoutInflater = LayoutInflater.from(this.mContext);
            } catch (Exception e) {
                Log.e(TAG, "ResourceHelper", e);
            }
        }
    }

    public Resources getResources() {
        return this.mResources;
    }

    public LayoutInflater getLayoutInflater() {
        return this.mLayoutInflater;
    }

    public int getResourceId(String str) {
        if (this.mResources == null) {
            return -1;
        }
        try {
            String[] strArrSplit = str.split("\\.");
            return this.mResources.getIdentifier(strArrSplit[2], strArrSplit[1], this.mPackageName);
        } catch (Exception e) {
            Log.e(TAG, "ResourceId", e);
            return -1;
        }
    }
}
