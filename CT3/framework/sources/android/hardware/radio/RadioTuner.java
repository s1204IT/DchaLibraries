package android.hardware.radio;

import android.hardware.radio.RadioManager;

public abstract class RadioTuner {
    public static final int DIRECTION_DOWN = 1;
    public static final int DIRECTION_UP = 0;
    public static final int ERROR_CANCELLED = 2;
    public static final int ERROR_CONFIG = 4;
    public static final int ERROR_HARDWARE_FAILURE = 0;
    public static final int ERROR_SCAN_TIMEOUT = 3;
    public static final int ERROR_SERVER_DIED = 1;

    public abstract int cancel();

    public abstract void close();

    public abstract int getConfiguration(RadioManager.BandConfig[] bandConfigArr);

    public abstract boolean getMute();

    public abstract int getProgramInformation(RadioManager.ProgramInfo[] programInfoArr);

    public abstract boolean hasControl();

    public abstract boolean isAntennaConnected();

    public abstract int scan(int i, boolean z);

    public abstract int setConfiguration(RadioManager.BandConfig bandConfig);

    public abstract int setMute(boolean z);

    public abstract int step(int i, boolean z);

    public abstract int tune(int i, int i2);

    public static abstract class Callback {
        public void onError(int status) {
        }

        public void onConfigurationChanged(RadioManager.BandConfig config) {
        }

        public void onProgramInfoChanged(RadioManager.ProgramInfo info) {
        }

        public void onMetadataChanged(RadioMetadata metadata) {
        }

        public void onTrafficAnnouncement(boolean active) {
        }

        public void onEmergencyAnnouncement(boolean active) {
        }

        public void onAntennaState(boolean connected) {
        }

        public void onControlChanged(boolean control) {
        }
    }
}
