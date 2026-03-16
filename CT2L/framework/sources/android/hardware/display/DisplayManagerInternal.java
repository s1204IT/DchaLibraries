package android.hardware.display;

import android.hardware.SensorManager;
import android.os.Handler;
import android.view.Display;
import android.view.DisplayInfo;

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

    public abstract void initPowerManagement(DisplayPowerCallbacks displayPowerCallbacks, Handler handler, SensorManager sensorManager);

    public abstract boolean isProximitySensorAvailable();

    public abstract void performTraversalInTransactionFromWindowManager();

    public abstract void registerDisplayTransactionListener(DisplayTransactionListener displayTransactionListener);

    public abstract boolean requestPowerState(DisplayPowerRequest displayPowerRequest, boolean z);

    public abstract void setDisplayInfoOverrideFromWindowManager(int i, DisplayInfo displayInfo);

    public abstract void setDisplayProperties(int i, boolean z, float f, boolean z2);

    public abstract void unregisterDisplayTransactionListener(DisplayTransactionListener displayTransactionListener);

    public static final class DisplayPowerRequest {
        public static final int POLICY_BRIGHT = 3;
        public static final int POLICY_DIM = 2;
        public static final int POLICY_DOZE = 1;
        public static final int POLICY_OFF = 0;
        public boolean blockScreenOn;
        public boolean boostScreenBrightness;
        public int dozeScreenBrightness;
        public int dozeScreenState;
        public boolean lowPowerMode;
        public int policy;
        public float screenAutoBrightnessAdjustment;
        public int screenBrightness;
        public boolean useAutoBrightness;
        public boolean useProximitySensor;

        public DisplayPowerRequest() {
            this.policy = 3;
            this.useProximitySensor = false;
            this.screenBrightness = 255;
            this.screenAutoBrightnessAdjustment = 0.0f;
            this.useAutoBrightness = false;
            this.blockScreenOn = false;
            this.dozeScreenBrightness = -1;
            this.dozeScreenState = 0;
        }

        public DisplayPowerRequest(DisplayPowerRequest other) {
            copyFrom(other);
        }

        public boolean isBrightOrDim() {
            return this.policy == 3 || this.policy == 2;
        }

        public void copyFrom(DisplayPowerRequest other) {
            this.policy = other.policy;
            this.useProximitySensor = other.useProximitySensor;
            this.screenBrightness = other.screenBrightness;
            this.screenAutoBrightnessAdjustment = other.screenAutoBrightnessAdjustment;
            this.useAutoBrightness = other.useAutoBrightness;
            this.blockScreenOn = other.blockScreenOn;
            this.lowPowerMode = other.lowPowerMode;
            this.boostScreenBrightness = other.boostScreenBrightness;
            this.dozeScreenBrightness = other.dozeScreenBrightness;
            this.dozeScreenState = other.dozeScreenState;
        }

        public boolean equals(Object o) {
            return (o instanceof DisplayPowerRequest) && equals((DisplayPowerRequest) o);
        }

        public boolean equals(DisplayPowerRequest other) {
            return other != null && this.policy == other.policy && this.useProximitySensor == other.useProximitySensor && this.screenBrightness == other.screenBrightness && this.screenAutoBrightnessAdjustment == other.screenAutoBrightnessAdjustment && this.useAutoBrightness == other.useAutoBrightness && this.blockScreenOn == other.blockScreenOn && this.lowPowerMode == other.lowPowerMode && this.boostScreenBrightness == other.boostScreenBrightness && this.dozeScreenBrightness == other.dozeScreenBrightness && this.dozeScreenState == other.dozeScreenState;
        }

        public int hashCode() {
            return 0;
        }

        public String toString() {
            return "policy=" + policyToString(this.policy) + ", useProximitySensor=" + this.useProximitySensor + ", screenBrightness=" + this.screenBrightness + ", screenAutoBrightnessAdjustment=" + this.screenAutoBrightnessAdjustment + ", useAutoBrightness=" + this.useAutoBrightness + ", blockScreenOn=" + this.blockScreenOn + ", lowPowerMode=" + this.lowPowerMode + ", boostScreenBrightness=" + this.boostScreenBrightness + ", dozeScreenBrightness=" + this.dozeScreenBrightness + ", dozeScreenState=" + Display.stateToString(this.dozeScreenState);
        }

        public static String policyToString(int policy) {
            switch (policy) {
                case 0:
                    return "OFF";
                case 1:
                    return "DOZE";
                case 2:
                    return "DIM";
                case 3:
                    return "BRIGHT";
                default:
                    return Integer.toString(policy);
            }
        }
    }
}
