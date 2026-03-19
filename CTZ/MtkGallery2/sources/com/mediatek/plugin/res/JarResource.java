package com.mediatek.plugin.res;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import com.mediatek.plugin.utils.Log;
import java.io.IOException;

public class JarResource implements IResource {
    private static final String TAG = "PluginManager/JarResource";
    private AssetManager mAssertManager;
    private Context mContext;
    protected String mFilePath;
    protected String mPackageProcessName = null;

    public JarResource(Context context, String str) {
        this.mFilePath = null;
        this.mContext = context;
        this.mFilePath = str;
    }

    public AssetManager getAssertManager() {
        return this.mAssertManager;
    }

    @Override
    public String getString(String str) {
        Log.d(TAG, "<getString> value = " + str);
        return str;
    }

    @Override
    public Drawable getDrawable(String str) {
        Log.d(TAG, "<getDrawable> value = " + str);
        try {
            return new BitmapDrawable(this.mContext.getAssets().open(str));
        } catch (IOException e) {
            Log.e(TAG, "IOException", e);
            return null;
        }
    }

    @Override
    public String[] getString(String str, String str2) {
        if (str != null) {
            return str.split(str2);
        }
        return null;
    }
}
