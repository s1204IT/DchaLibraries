package android.hardware.display;

import android.hardware.SensorManager;
import android.os.Handler;
import android.util.IntArray;
import android.util.SparseArray;
import android.view.Display;
import android.view.DisplayInfo;
import android.view.SurfaceControl;

public abstract class DisplayManagerInternal {

    public interface DisplayPowerCallbacks {
        void acquireSuspendBlocker();

        void onDisplayStateChange(int i);

        void onProximityNegative();

        void onProximityPositive();

        void onStateChanged();

        void releaseSuspendBlocker();
    }

    public interface DisplayTransactionListener {
        void onDisplayTransaction();
    }

    public abstract DisplayInfo getDisplayInfo(int i);

    public abstract void getNonOverrideDisplayInfo(int i, DisplayInfo displayInfo);

    public abstract void initPowerManagement(DisplayPowerCallbacks displayPowerCallbacks, Handler handler, SensorManager sensorManager);

    public abstract boolean isProximitySensorAvailable();

    public abstract boolean isUidPresentOnDisplay(int i, int i2);

    public abstract void onOverlayChanged();

    public abstract void performTraversal(SurfaceControl.Transaction transaction);

    public abstract void persistBrightnessTrackerState();

    public abstract void registerDisplayTransactionListener(DisplayTransactionListener displayTransactionListener);

    public abstract boolean requestPowerState(DisplayPowerRequest displayPowerRequest, boolean z);

    public abstract void setDisplayAccessUIDs(SparseArray<IntArray> sparseArray);

    public abstract void setDisplayInfoOverrideFromWindowManager(int i, DisplayInfo displayInfo);

    public abstract void setDisplayOffsets(int i, int i2, int i3);

    public abstract void setDisplayProperties(int i, boolean z, float f, int i2, boolean z2);

    public abstract void unregisterDisplayTransactionListener(DisplayTransactionListener displayTransactionListener);

    public static final class DisplayPowerRequest {
        public static final int POLICY_BRIGHT = 3;
        public static final int POLICY_DIM = 2;
        public static final int POLICY_DOZE = 1;
        public static final int POLICY_OFF = 0;
        public static final int POLICY_VR = 4;
        public boolean blockScreenOn;
        public boolean boostScreenBrightness;
        public int dozeScreenBrightness;
        public int dozeScreenState;
        public boolean lowPowerMode;
        public int policy;
        public float screenAutoBrightnessAdjustmentOverride;
        public int screenBrightnessOverride;
        public float screenLowPowerBrightnessFactor;
        public boolean useAutoBrightness;
        public boolean useProximitySensor;

        public DisplayPowerRequest() {
            this.policy = 3;
            this.useProximitySensor = false;
            this.screenBrightnessOverride = -1;
            this.useAutoBrightness = false;
            this.screenAutoBrightnessAdjustmentOverride = Float.NaN;
            this.screenLowPowerBrightnessFactor = 0.5f;
            this.blockScreenOn = false;
            this.dozeScreenBrightness = -1;
            this.dozeScreenState = 0;
        }

        public DisplayPowerRequest(DisplayPowerRequest displayPowerRequest) {
            copyFrom(displayPowerRequest);
        }

        public boolean isBrightOrDim() {
            return this.policy == 3 || this.policy == 2;
        }

        public boolean isVr() {
            return this.policy == 4;
        }

        public void copyFrom(DisplayPowerRequest displayPowerRequest) {
            this.policy = displayPowerRequest.policy;
            this.useProximitySensor = displayPowerRequest.useProximitySensor;
            this.screenBrightnessOverride = displayPowerRequest.screenBrightnessOverride;
            this.useAutoBrightness = displayPowerRequest.useAutoBrightness;
            this.screenAutoBrightnessAdjustmentOverride = displayPowerRequest.screenAutoBrightnessAdjustmentOverride;
            this.screenLowPowerBrightnessFactor = displayPowerRequest.screenLowPowerBrightnessFactor;
            this.blockScreenOn = displayPowerRequest.blockScreenOn;
            this.lowPowerMode = displayPowerRequest.lowPowerMode;
            this.boostScreenBrightness = displayPowerRequest.boostScreenBrightness;
            this.dozeScreenBrightness = displayPowerRequest.dozeScreenBrightness;
            this.dozeScreenState = displayPowerRequest.dozeScreenState;
        }

        public boolean equals(Object obj) {
            return (obj instanceof DisplayPowerRequest) && equals((DisplayPowerRequest) obj);
        }

        public boolean equals(DisplayPowerRequest displayPowerRequest) {
            return displayPowerRequest != null && this.policy == displayPowerRequest.policy && this.useProximitySensor == displayPowerRequest.useProximitySensor && this.screenBrightnessOverride == displayPowerRequest.screenBrightnessOverride && this.useAutoBrightness == displayPowerRequest.useAutoBrightness && floatEquals(this.screenAutoBrightnessAdjustmentOverride, displayPowerRequest.screenAutoBrightnessAdjustmentOverride) && this.screenLowPowerBrightnessFactor == displayPowerRequest.screenLowPowerBrightnessFactor && this.blockScreenOn == displayPowerRequest.blockScreenOn && this.lowPowerMode == displayPowerRequest.lowPowerMode && this.boostScreenBrightness == displayPowerRequest.boostScreenBrightness && this.dozeScreenBrightness == displayPowerRequest.dozeScreenBrightness && this.dozeScreenState == displayPowerRequest.dozeScreenState;
        }

        private boolean floatEquals(float f, float f2) {
            return f == f2 || (Float.isNaN(f) && Float.isNaN(f2));
        }

        public int hashCode() {
            return 0;
        }

        public String toString() {
            return "policy=" + policyToString(this.policy) + ", useProximitySensor=" + this.useProximitySensor + ", screenBrightnessOverride=" + this.screenBrightnessOverride + ", useAutoBrightness=" + this.useAutoBrightness + ", screenAutoBrightnessAdjustmentOverride=" + this.screenAutoBrightnessAdjustmentOverride + ", screenLowPowerBrightnessFactor=" + this.screenLowPowerBrightnessFactor + ", blockScreenOn=" + this.blockScreenOn + ", lowPowerMode=" + this.lowPowerMode + ", boostScreenBrightness=" + this.boostScreenBrightness + ", dozeScreenBrightness=" + this.dozeScreenBrightness + ", dozeScreenState=" + Display.stateToString(this.dozeScreenState);
        }

        public static String policyToString(int i) {
            switch (i) {
                case 0:
                    return "OFF";
                case 1:
                    return "DOZE";
                case 2:
                    return "DIM";
                case 3:
                    return "BRIGHT";
                case 4:
                    return "VR";
                default:
                    return Integer.toString(i);
            }
        }
    }
}
