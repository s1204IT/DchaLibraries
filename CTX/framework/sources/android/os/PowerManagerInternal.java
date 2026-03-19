package android.os;

import java.util.function.Consumer;

public abstract class PowerManagerInternal {
    public static final int WAKEFULNESS_ASLEEP = 0;
    public static final int WAKEFULNESS_AWAKE = 1;
    public static final int WAKEFULNESS_DOZING = 3;
    public static final int WAKEFULNESS_DREAMING = 2;

    public interface LowPowerModeListener {
        int getServiceType();

        void onLowPowerModeChanged(PowerSaveState powerSaveState);
    }

    public abstract void finishUidChanges();

    public abstract PowerSaveState getLowPowerState(int i);

    public abstract void powerHint(int i, int i2);

    public abstract void registerLowPowerModeObserver(LowPowerModeListener lowPowerModeListener);

    public abstract boolean setDeviceIdleMode(boolean z);

    public abstract void setDeviceIdleTempWhitelist(int[] iArr);

    public abstract void setDeviceIdleWhitelist(int[] iArr);

    public abstract void setDozeOverrideFromDreamManager(int i, int i2);

    public abstract void setDrawWakeLockOverrideFromSidekick(boolean z);

    public abstract boolean setLightDeviceIdleMode(boolean z);

    public abstract void setMaximumScreenOffTimeoutFromDeviceAdmin(int i, long j);

    public abstract void setScreenBrightnessOverrideFromWindowManager(int i);

    public abstract void setUserActivityTimeoutOverrideFromWindowManager(long j);

    public abstract void setUserInactiveOverrideFromWindowManager();

    public abstract void startUidChanges();

    public abstract void uidActive(int i);

    public abstract void uidGone(int i);

    public abstract void uidIdle(int i);

    public abstract void updateUidProcState(int i, int i2);

    public static String wakefulnessToString(int i) {
        switch (i) {
            case 0:
                return "Asleep";
            case 1:
                return "Awake";
            case 2:
                return "Dreaming";
            case 3:
                return "Dozing";
            default:
                return Integer.toString(i);
        }
    }

    public static int wakefulnessToProtoEnum(int i) {
        switch (i) {
            case 0:
                return 0;
            case 1:
                return 1;
            case 2:
                return 2;
            case 3:
                return 3;
            default:
                return i;
        }
    }

    public static boolean isInteractive(int i) {
        return i == 1 || i == 2;
    }

    public void registerLowPowerModeObserver(final int i, final Consumer<PowerSaveState> consumer) {
        registerLowPowerModeObserver(new LowPowerModeListener() {
            @Override
            public int getServiceType() {
                return i;
            }

            @Override
            public void onLowPowerModeChanged(PowerSaveState powerSaveState) {
                consumer.accept(powerSaveState);
            }
        });
    }
}
