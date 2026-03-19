package com.android.internal.os;

import android.os.BatteryStats;

public class MediaPowerCalculator extends PowerCalculator {
    private static final int MS_IN_HR = 3600000;
    private final double mAudioAveragePowerMa;
    private final double mVideoAveragePowerMa;

    public MediaPowerCalculator(PowerProfile powerProfile) {
        this.mAudioAveragePowerMa = powerProfile.getAveragePower("audio");
        this.mVideoAveragePowerMa = powerProfile.getAveragePower("video");
    }

    @Override
    public void calculateApp(BatterySipper batterySipper, BatteryStats.Uid uid, long j, long j2, int i) {
        BatteryStats.Timer audioTurnedOnTimer = uid.getAudioTurnedOnTimer();
        if (audioTurnedOnTimer == null) {
            batterySipper.audioTimeMs = 0L;
            batterySipper.audioPowerMah = 0.0d;
        } else {
            long totalTimeLocked = audioTurnedOnTimer.getTotalTimeLocked(j, i) / 1000;
            batterySipper.audioTimeMs = totalTimeLocked;
            batterySipper.audioPowerMah = (totalTimeLocked * this.mAudioAveragePowerMa) / 3600000.0d;
        }
        BatteryStats.Timer videoTurnedOnTimer = uid.getVideoTurnedOnTimer();
        if (videoTurnedOnTimer == null) {
            batterySipper.videoTimeMs = 0L;
            batterySipper.videoPowerMah = 0.0d;
        } else {
            long totalTimeLocked2 = videoTurnedOnTimer.getTotalTimeLocked(j, i) / 1000;
            batterySipper.videoTimeMs = totalTimeLocked2;
            batterySipper.videoPowerMah = (totalTimeLocked2 * this.mVideoAveragePowerMa) / 3600000.0d;
        }
    }
}
