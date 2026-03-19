package android.hardware.display;

import android.annotation.SystemApi;
import android.content.Context;
import android.graphics.Point;
import android.hardware.display.VirtualDisplay;
import android.media.projection.MediaProjection;
import android.os.Handler;
import android.util.Pair;
import android.util.SparseArray;
import android.view.Display;
import android.view.Surface;
import java.util.ArrayList;
import java.util.List;

public final class DisplayManager {
    public static final String ACTION_WIFI_DISPLAY_STATUS_CHANGED = "android.hardware.display.action.WIFI_DISPLAY_STATUS_CHANGED";
    private static final boolean DEBUG = false;
    public static final String DISPLAY_CATEGORY_PRESENTATION = "android.hardware.display.category.PRESENTATION";
    public static final String EXTRA_WIFI_DISPLAY_STATUS = "android.hardware.display.extra.WIFI_DISPLAY_STATUS";
    private static final String TAG = "DisplayManager";
    public static final int VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR = 16;
    public static final int VIRTUAL_DISPLAY_FLAG_CAN_SHOW_WITH_INSECURE_KEYGUARD = 32;
    public static final int VIRTUAL_DISPLAY_FLAG_DESTROY_CONTENT_ON_REMOVAL = 256;
    public static final int VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY = 8;
    public static final int VIRTUAL_DISPLAY_FLAG_PRESENTATION = 2;
    public static final int VIRTUAL_DISPLAY_FLAG_PUBLIC = 1;
    public static final int VIRTUAL_DISPLAY_FLAG_ROTATES_WITH_CONTENT = 128;
    public static final int VIRTUAL_DISPLAY_FLAG_SECURE = 4;
    public static final int VIRTUAL_DISPLAY_FLAG_SUPPORTS_TOUCH = 64;
    private final Context mContext;
    private final Object mLock = new Object();
    private final SparseArray<Display> mDisplays = new SparseArray<>();
    private final ArrayList<Display> mTempDisplays = new ArrayList<>();
    private final DisplayManagerGlobal mGlobal = DisplayManagerGlobal.getInstance();

    public interface DisplayListener {
        void onDisplayAdded(int i);

        void onDisplayChanged(int i);

        void onDisplayRemoved(int i);
    }

    public DisplayManager(Context context) {
        this.mContext = context;
    }

    public Display getDisplay(int i) {
        Display orCreateDisplayLocked;
        synchronized (this.mLock) {
            orCreateDisplayLocked = getOrCreateDisplayLocked(i, false);
        }
        return orCreateDisplayLocked;
    }

    public Display[] getDisplays() {
        return getDisplays(null);
    }

    public Display[] getDisplays(String str) {
        Display[] displayArr;
        int[] displayIds = this.mGlobal.getDisplayIds();
        synchronized (this.mLock) {
            try {
                if (str == null) {
                    addAllDisplaysLocked(this.mTempDisplays, displayIds);
                } else if (str.equals(DISPLAY_CATEGORY_PRESENTATION)) {
                    addPresentationDisplaysLocked(this.mTempDisplays, displayIds, 3);
                    addPresentationDisplaysLocked(this.mTempDisplays, displayIds, 2);
                    addPresentationDisplaysLocked(this.mTempDisplays, displayIds, 4);
                    addPresentationDisplaysLocked(this.mTempDisplays, displayIds, 5);
                }
                displayArr = (Display[]) this.mTempDisplays.toArray(new Display[this.mTempDisplays.size()]);
                this.mTempDisplays.clear();
            } catch (Throwable th) {
                this.mTempDisplays.clear();
                throw th;
            }
        }
        return displayArr;
    }

    private void addAllDisplaysLocked(ArrayList<Display> arrayList, int[] iArr) {
        for (int i : iArr) {
            Display orCreateDisplayLocked = getOrCreateDisplayLocked(i, true);
            if (orCreateDisplayLocked != null) {
                arrayList.add(orCreateDisplayLocked);
            }
        }
    }

    private void addPresentationDisplaysLocked(ArrayList<Display> arrayList, int[] iArr, int i) {
        for (int i2 : iArr) {
            Display orCreateDisplayLocked = getOrCreateDisplayLocked(i2, true);
            if (orCreateDisplayLocked != null && (orCreateDisplayLocked.getFlags() & 8) != 0 && orCreateDisplayLocked.getType() == i) {
                arrayList.add(orCreateDisplayLocked);
            }
        }
    }

    private Display getOrCreateDisplayLocked(int i, boolean z) {
        Display display = this.mDisplays.get(i);
        if (display != null) {
            if (!z && !display.isValid()) {
                return null;
            }
            return display;
        }
        Display compatibleDisplay = this.mGlobal.getCompatibleDisplay(i, (this.mContext.getDisplay().getDisplayId() == i ? this.mContext : this.mContext.getApplicationContext()).getResources());
        if (compatibleDisplay != null) {
            this.mDisplays.put(i, compatibleDisplay);
            return compatibleDisplay;
        }
        return compatibleDisplay;
    }

    public void registerDisplayListener(DisplayListener displayListener, Handler handler) {
        this.mGlobal.registerDisplayListener(displayListener, handler);
    }

    public void unregisterDisplayListener(DisplayListener displayListener) {
        this.mGlobal.unregisterDisplayListener(displayListener);
    }

    public void startWifiDisplayScan() {
        this.mGlobal.startWifiDisplayScan();
    }

    public void stopWifiDisplayScan() {
        this.mGlobal.stopWifiDisplayScan();
    }

    public void connectWifiDisplay(String str) {
        this.mGlobal.connectWifiDisplay(str);
    }

    public void pauseWifiDisplay() {
        this.mGlobal.pauseWifiDisplay();
    }

    public void resumeWifiDisplay() {
        this.mGlobal.resumeWifiDisplay();
    }

    public void disconnectWifiDisplay() {
        this.mGlobal.disconnectWifiDisplay();
    }

    public void renameWifiDisplay(String str, String str2) {
        this.mGlobal.renameWifiDisplay(str, str2);
    }

    public void forgetWifiDisplay(String str) {
        this.mGlobal.forgetWifiDisplay(str);
    }

    public WifiDisplayStatus getWifiDisplayStatus() {
        return this.mGlobal.getWifiDisplayStatus();
    }

    @SystemApi
    public void setSaturationLevel(float f) {
        this.mGlobal.setSaturationLevel(f);
    }

    public VirtualDisplay createVirtualDisplay(String str, int i, int i2, int i3, Surface surface, int i4) {
        return createVirtualDisplay(str, i, i2, i3, surface, i4, null, null);
    }

    public VirtualDisplay createVirtualDisplay(String str, int i, int i2, int i3, Surface surface, int i4, VirtualDisplay.Callback callback, Handler handler) {
        return createVirtualDisplay(null, str, i, i2, i3, surface, i4, callback, handler, null);
    }

    public VirtualDisplay createVirtualDisplay(MediaProjection mediaProjection, String str, int i, int i2, int i3, Surface surface, int i4, VirtualDisplay.Callback callback, Handler handler, String str2) {
        return this.mGlobal.createVirtualDisplay(this.mContext, mediaProjection, str, i, i2, i3, surface, i4, callback, handler, str2);
    }

    @SystemApi
    public Point getStableDisplaySize() {
        return this.mGlobal.getStableDisplaySize();
    }

    @SystemApi
    public List<BrightnessChangeEvent> getBrightnessEvents() {
        return this.mGlobal.getBrightnessEvents(this.mContext.getOpPackageName());
    }

    @SystemApi
    public List<AmbientBrightnessDayStats> getAmbientBrightnessStats() {
        return this.mGlobal.getAmbientBrightnessStats();
    }

    @SystemApi
    public void setBrightnessConfiguration(BrightnessConfiguration brightnessConfiguration) {
        setBrightnessConfigurationForUser(brightnessConfiguration, this.mContext.getUserId(), this.mContext.getPackageName());
    }

    public void setBrightnessConfigurationForUser(BrightnessConfiguration brightnessConfiguration, int i, String str) {
        this.mGlobal.setBrightnessConfigurationForUser(brightnessConfiguration, i, str);
    }

    @SystemApi
    public BrightnessConfiguration getBrightnessConfiguration() {
        return getBrightnessConfigurationForUser(this.mContext.getUserId());
    }

    public BrightnessConfiguration getBrightnessConfigurationForUser(int i) {
        return this.mGlobal.getBrightnessConfigurationForUser(i);
    }

    @SystemApi
    public BrightnessConfiguration getDefaultBrightnessConfiguration() {
        return this.mGlobal.getDefaultBrightnessConfiguration();
    }

    public void setTemporaryBrightness(int i) {
        this.mGlobal.setTemporaryBrightness(i);
    }

    public void setTemporaryAutoBrightnessAdjustment(float f) {
        this.mGlobal.setTemporaryAutoBrightnessAdjustment(f);
    }

    @SystemApi
    public Pair<float[], float[]> getMinimumBrightnessCurve() {
        return this.mGlobal.getMinimumBrightnessCurve();
    }

    public boolean isSinkEnabled() {
        return this.mGlobal.isSinkEnabled();
    }

    public void enableSink(boolean z) {
        this.mGlobal.enableSink(z);
    }

    public void waitWifiDisplayConnection(Surface surface) {
        this.mGlobal.waitWifiDisplayConnection(surface);
    }

    public void suspendWifiDisplay(boolean z, Surface surface) {
        this.mGlobal.suspendWifiDisplay(z, surface);
    }

    public void sendUibcInputEvent(String str) {
        this.mGlobal.sendUibcInputEvent(str);
    }
}
