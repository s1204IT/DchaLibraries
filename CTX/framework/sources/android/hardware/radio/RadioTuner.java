package android.hardware.radio;

import android.annotation.SystemApi;
import android.graphics.Bitmap;
import android.hardware.radio.ProgramList;
import android.hardware.radio.RadioManager;
import java.util.List;
import java.util.Map;

@SystemApi
public abstract class RadioTuner {
    public static final int DIRECTION_DOWN = 1;
    public static final int DIRECTION_UP = 0;

    @Deprecated
    public static final int ERROR_BACKGROUND_SCAN_FAILED = 6;

    @Deprecated
    public static final int ERROR_BACKGROUND_SCAN_UNAVAILABLE = 5;

    @Deprecated
    public static final int ERROR_CANCELLED = 2;

    @Deprecated
    public static final int ERROR_CONFIG = 4;

    @Deprecated
    public static final int ERROR_HARDWARE_FAILURE = 0;

    @Deprecated
    public static final int ERROR_SCAN_TIMEOUT = 3;

    @Deprecated
    public static final int ERROR_SERVER_DIED = 1;

    public abstract int cancel();

    public abstract void cancelAnnouncement();

    public abstract void close();

    @Deprecated
    public abstract int getConfiguration(RadioManager.BandConfig[] bandConfigArr);

    public abstract Bitmap getMetadataImage(int i);

    public abstract boolean getMute();

    @Deprecated
    public abstract int getProgramInformation(RadioManager.ProgramInfo[] programInfoArr);

    @Deprecated
    public abstract List<RadioManager.ProgramInfo> getProgramList(Map<String, String> map);

    public abstract boolean hasControl();

    @Deprecated
    public abstract boolean isAnalogForced();

    @Deprecated
    public abstract boolean isAntennaConnected();

    public abstract int scan(int i, boolean z);

    @Deprecated
    public abstract void setAnalogForced(boolean z);

    @Deprecated
    public abstract int setConfiguration(RadioManager.BandConfig bandConfig);

    public abstract int setMute(boolean z);

    public abstract boolean startBackgroundScan();

    public abstract int step(int i, boolean z);

    @Deprecated
    public abstract int tune(int i, int i2);

    public abstract void tune(ProgramSelector programSelector);

    public ProgramList getDynamicProgramList(ProgramList.Filter filter) {
        return null;
    }

    public boolean isConfigFlagSupported(int i) {
        return false;
    }

    public boolean isConfigFlagSet(int i) {
        throw new UnsupportedOperationException();
    }

    public void setConfigFlag(int i, boolean z) {
        throw new UnsupportedOperationException();
    }

    public Map<String, String> setParameters(Map<String, String> map) {
        throw new UnsupportedOperationException();
    }

    public Map<String, String> getParameters(List<String> list) {
        throw new UnsupportedOperationException();
    }

    public static abstract class Callback {
        public void onError(int i) {
        }

        public void onTuneFailed(int i, ProgramSelector programSelector) {
        }

        @Deprecated
        public void onConfigurationChanged(RadioManager.BandConfig bandConfig) {
        }

        public void onProgramInfoChanged(RadioManager.ProgramInfo programInfo) {
        }

        @Deprecated
        public void onMetadataChanged(RadioMetadata radioMetadata) {
        }

        public void onTrafficAnnouncement(boolean z) {
        }

        public void onEmergencyAnnouncement(boolean z) {
        }

        public void onAntennaState(boolean z) {
        }

        public void onControlChanged(boolean z) {
        }

        public void onBackgroundScanAvailabilityChange(boolean z) {
        }

        public void onBackgroundScanComplete() {
        }

        public void onProgramListChanged() {
        }

        public void onParametersUpdated(Map<String, String> map) {
        }
    }
}
