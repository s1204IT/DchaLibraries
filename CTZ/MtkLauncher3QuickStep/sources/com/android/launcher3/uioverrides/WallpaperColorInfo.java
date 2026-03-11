package com.android.launcher3.uioverrides;

import android.annotation.TargetApi;
import android.app.WallpaperColors;
import android.app.WallpaperManager;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import com.android.systemui.shared.system.TonalCompat;
import java.util.ArrayList;

@TargetApi(28)
public class WallpaperColorInfo implements WallpaperManager.OnColorsChangedListener {
    private static WallpaperColorInfo sInstance;
    private static final Object sInstanceLock = new Object();
    private TonalCompat.ExtractionInfo mExtractionInfo;
    private final ArrayList<OnChangeListener> mListeners = new ArrayList<>();
    private OnChangeListener[] mTempListeners = new OnChangeListener[0];
    private final TonalCompat mTonalCompat;
    private final WallpaperManager mWallpaperManager;

    public interface OnChangeListener {
        void onExtractedColorsChanged(WallpaperColorInfo wallpaperColorInfo);
    }

    public static WallpaperColorInfo getInstance(Context context) {
        WallpaperColorInfo wallpaperColorInfo;
        synchronized (sInstanceLock) {
            if (sInstance == null) {
                sInstance = new WallpaperColorInfo(context.getApplicationContext());
            }
            wallpaperColorInfo = sInstance;
        }
        return wallpaperColorInfo;
    }

    private WallpaperColorInfo(Context context) {
        this.mWallpaperManager = (WallpaperManager) context.getSystemService(WallpaperManager.class);
        this.mTonalCompat = new TonalCompat(context);
        this.mWallpaperManager.addOnColorsChangedListener(this, new Handler(Looper.getMainLooper()));
        update(this.mWallpaperManager.getWallpaperColors(1));
    }

    public int getMainColor() {
        return this.mExtractionInfo.mainColor;
    }

    public int getSecondaryColor() {
        return this.mExtractionInfo.secondaryColor;
    }

    public boolean isDark() {
        return this.mExtractionInfo.supportsDarkTheme;
    }

    public boolean supportsDarkText() {
        return this.mExtractionInfo.supportsDarkText;
    }

    @Override
    public void onColorsChanged(WallpaperColors wallpaperColors, int i) {
        if ((i & 1) != 0) {
            update(wallpaperColors);
            notifyChange();
        }
    }

    private void update(WallpaperColors wallpaperColors) {
        this.mExtractionInfo = this.mTonalCompat.extractDarkColors(wallpaperColors);
    }

    public void addOnChangeListener(OnChangeListener onChangeListener) {
        this.mListeners.add(onChangeListener);
    }

    public void removeOnChangeListener(OnChangeListener onChangeListener) {
        this.mListeners.remove(onChangeListener);
    }

    private void notifyChange() {
        this.mTempListeners = (OnChangeListener[]) this.mListeners.toArray(this.mTempListeners);
        for (OnChangeListener onChangeListener : this.mTempListeners) {
            if (onChangeListener != null) {
                onChangeListener.onExtractedColorsChanged(this);
            }
        }
    }
}
