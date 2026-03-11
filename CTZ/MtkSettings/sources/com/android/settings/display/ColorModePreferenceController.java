package com.android.settings.display;

import android.content.Context;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.app.ColorDisplayController;
import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

public class ColorModePreferenceController extends BasePreferenceController {
    private static final String KEY_COLOR_MODE = "color_mode";
    private static final int SURFACE_FLINGER_TRANSACTION_QUERY_WIDE_COLOR = 1024;
    private static final String TAG = "ColorModePreference";
    private ColorDisplayController mColorDisplayController;
    private final ConfigurationWrapper mConfigWrapper;

    public ColorModePreferenceController(Context context) {
        super(context, KEY_COLOR_MODE);
        this.mConfigWrapper = new ConfigurationWrapper();
    }

    @Override
    public int getAvailabilityStatus() {
        return (!this.mConfigWrapper.isScreenWideColorGamut() || getColorDisplayController().getAccessibilityTransformActivated()) ? 3 : 0;
    }

    @Override
    public CharSequence getSummary() {
        int colorMode = getColorDisplayController().getColorMode();
        if (colorMode == 3) {
            return this.mContext.getText(R.string.color_mode_option_automatic);
        }
        if (colorMode == 2) {
            return this.mContext.getText(R.string.color_mode_option_saturated);
        }
        if (colorMode == 1) {
            return this.mContext.getText(R.string.color_mode_option_boosted);
        }
        return this.mContext.getText(R.string.color_mode_option_natural);
    }

    @VisibleForTesting
    ColorDisplayController getColorDisplayController() {
        if (this.mColorDisplayController == null) {
            this.mColorDisplayController = new ColorDisplayController(this.mContext);
        }
        return this.mColorDisplayController;
    }

    @VisibleForTesting
    static class ConfigurationWrapper {
        private final IBinder mSurfaceFlinger = ServiceManager.getService("SurfaceFlinger");

        ConfigurationWrapper() {
        }

        boolean isScreenWideColorGamut() {
            if (this.mSurfaceFlinger != null) {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                parcelObtain.writeInterfaceToken("android.ui.ISurfaceComposer");
                try {
                    this.mSurfaceFlinger.transact(ColorModePreferenceController.SURFACE_FLINGER_TRANSACTION_QUERY_WIDE_COLOR, parcelObtain, parcelObtain2, 0);
                    return parcelObtain2.readBoolean();
                } catch (RemoteException e) {
                    Log.e(ColorModePreferenceController.TAG, "Failed to query wide color support", e);
                } finally {
                    parcelObtain.recycle();
                    parcelObtain2.recycle();
                }
            }
            return false;
        }
    }
}
