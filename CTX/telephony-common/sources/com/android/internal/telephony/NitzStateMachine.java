package com.android.internal.telephony;

import android.content.ContentResolver;
import android.content.Context;
import android.os.PowerManager;
import android.os.SystemProperties;
import android.provider.Settings;
import android.telephony.Rlog;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.LocalLog;
import android.util.TimeUtils;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.TimeServiceHelper;
import com.android.internal.telephony.TimeZoneLookupHelper;
import com.android.internal.telephony.metrics.TelephonyMetrics;
import com.android.internal.telephony.util.TimeStampedValue;
import com.android.internal.util.IndentingPrintWriter;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.TimeZone;

public class NitzStateMachine {
    private static final boolean DBG = true;
    private static final String LOG_TAG = "SST";
    private static final String WAKELOCK_TAG = "NitzStateMachine";
    private final DeviceState mDeviceState;
    private boolean mGotCountryCode;
    private TimeStampedValue<NitzData> mLatestNitzSignal;
    private boolean mNeedCountryCodeForNitz;
    private boolean mNitzTimeZoneDetectionSuccessful;
    private final GsmCdmaPhone mPhone;
    private TimeStampedValue<Long> mSavedNitzTime;
    private String mSavedTimeZoneId;
    private final LocalLog mTimeLog;
    private final TimeServiceHelper mTimeServiceHelper;
    private final LocalLog mTimeZoneLog;
    private final TimeZoneLookupHelper mTimeZoneLookupHelper;
    private final PowerManager.WakeLock mWakeLock;

    public static class DeviceState {
        private static final int NITZ_UPDATE_DIFF_DEFAULT = 2000;
        private static final int NITZ_UPDATE_SPACING_DEFAULT = 600000;
        private final ContentResolver mCr;
        private final int mNitzUpdateDiff;
        private final int mNitzUpdateSpacing;
        private final GsmCdmaPhone mPhone;
        private final TelephonyManager mTelephonyManager;

        public DeviceState(GsmCdmaPhone gsmCdmaPhone) {
            this.mPhone = gsmCdmaPhone;
            Context context = gsmCdmaPhone.getContext();
            this.mTelephonyManager = (TelephonyManager) context.getSystemService("phone");
            this.mCr = context.getContentResolver();
            this.mNitzUpdateSpacing = SystemProperties.getInt("ro.nitz_update_spacing", NITZ_UPDATE_SPACING_DEFAULT);
            this.mNitzUpdateDiff = SystemProperties.getInt("ro.nitz_update_diff", NITZ_UPDATE_DIFF_DEFAULT);
        }

        public int getNitzUpdateSpacingMillis() {
            return Settings.Global.getInt(this.mCr, "nitz_update_spacing", this.mNitzUpdateSpacing);
        }

        public int getNitzUpdateDiffMillis() {
            return Settings.Global.getInt(this.mCr, "nitz_update_diff", this.mNitzUpdateDiff);
        }

        public boolean getIgnoreNitz() {
            String str = SystemProperties.get("gsm.ignore-nitz");
            return str != null && str.equals("yes");
        }

        public String getNetworkCountryIsoForPhone() {
            return this.mTelephonyManager.getNetworkCountryIsoForPhone(this.mPhone.getPhoneId());
        }
    }

    public NitzStateMachine(GsmCdmaPhone gsmCdmaPhone) {
        this(gsmCdmaPhone, new TimeServiceHelper(gsmCdmaPhone.getContext()), new DeviceState(gsmCdmaPhone), new TimeZoneLookupHelper());
    }

    @VisibleForTesting
    public NitzStateMachine(GsmCdmaPhone gsmCdmaPhone, TimeServiceHelper timeServiceHelper, DeviceState deviceState, TimeZoneLookupHelper timeZoneLookupHelper) {
        this.mNeedCountryCodeForNitz = false;
        this.mGotCountryCode = false;
        this.mNitzTimeZoneDetectionSuccessful = false;
        this.mTimeLog = new LocalLog(15);
        this.mTimeZoneLog = new LocalLog(15);
        this.mPhone = gsmCdmaPhone;
        this.mWakeLock = ((PowerManager) gsmCdmaPhone.getContext().getSystemService("power")).newWakeLock(1, WAKELOCK_TAG);
        this.mDeviceState = deviceState;
        this.mTimeZoneLookupHelper = timeZoneLookupHelper;
        this.mTimeServiceHelper = timeServiceHelper;
        this.mTimeServiceHelper.setListener(new TimeServiceHelper.Listener() {
            @Override
            public void onTimeDetectionChange(boolean z) {
                if (z) {
                    NitzStateMachine.this.handleAutoTimeEnabled();
                }
            }

            @Override
            public void onTimeZoneDetectionChange(boolean z) {
                if (z) {
                    NitzStateMachine.this.handleAutoTimeZoneEnabled();
                }
            }
        });
    }

    public void handleNetworkCountryCodeSet(boolean z) {
        this.mGotCountryCode = true;
        String networkCountryIsoForPhone = this.mDeviceState.getNetworkCountryIsoForPhone();
        if (!TextUtils.isEmpty(networkCountryIsoForPhone) && !this.mNitzTimeZoneDetectionSuccessful && this.mTimeServiceHelper.isTimeZoneDetectionEnabled()) {
            updateTimeZoneByNetworkCountryCode(networkCountryIsoForPhone);
        }
        if (z || this.mNeedCountryCodeForNitz) {
            boolean zIsTimeZoneSettingInitialized = this.mTimeServiceHelper.isTimeZoneSettingInitialized();
            Rlog.d(LOG_TAG, "handleNetworkCountryCodeSet: isTimeZoneSettingInitialized=" + zIsTimeZoneSettingInitialized + " mLatestNitzSignal=" + this.mLatestNitzSignal + " isoCountryCode=" + networkCountryIsoForPhone);
            String id = null;
            if (TextUtils.isEmpty(networkCountryIsoForPhone) && this.mNeedCountryCodeForNitz) {
                TimeZoneLookupHelper.OffsetResult offsetResultLookupByNitz = this.mTimeZoneLookupHelper.lookupByNitz(this.mLatestNitzSignal.mValue);
                Rlog.d(LOG_TAG, "handleNetworkCountryCodeSet: guessZoneIdByNitz() returned lookupResult=" + offsetResultLookupByNitz);
                if (offsetResultLookupByNitz != null) {
                    id = offsetResultLookupByNitz.zoneId;
                }
            } else if (this.mLatestNitzSignal == null) {
                Rlog.d(LOG_TAG, "handleNetworkCountryCodeSet: No cached NITZ data available, not setting zone");
            } else if (nitzOffsetMightBeBogus(this.mLatestNitzSignal.mValue) && zIsTimeZoneSettingInitialized && !countryUsesUtc(networkCountryIsoForPhone, this.mLatestNitzSignal)) {
                TimeZone timeZone = TimeZone.getDefault();
                Rlog.d(LOG_TAG, "handleNetworkCountryCodeSet: NITZ looks bogus, maybe using current default zone to adjust the system clock, mNeedCountryCodeForNitz=" + this.mNeedCountryCodeForNitz + " mLatestNitzSignal=" + this.mLatestNitzSignal + " zone=" + timeZone);
                id = timeZone.getID();
                if (this.mNeedCountryCodeForNitz) {
                    NitzData nitzData = this.mLatestNitzSignal.mValue;
                    try {
                        this.mWakeLock.acquire();
                        long currentTimeInMillis = nitzData.getCurrentTimeInMillis() + (this.mTimeServiceHelper.elapsedRealtime() - this.mLatestNitzSignal.mElapsedRealtime);
                        long offset = timeZone.getOffset(currentTimeInMillis);
                        Rlog.d(LOG_TAG, "handleNetworkCountryCodeSet: tzOffset=" + offset + " delayAdjustedCtm=" + TimeUtils.logTimeOfDay(currentTimeInMillis));
                        if (this.mTimeServiceHelper.isTimeDetectionEnabled()) {
                            long j = currentTimeInMillis - offset;
                            setAndBroadcastNetworkSetTime("handleNetworkCountryCodeSet: setting time timeZoneAdjustedCtm=" + TimeUtils.logTimeOfDay(j), j);
                        } else {
                            this.mSavedNitzTime = new TimeStampedValue<>(Long.valueOf(this.mSavedNitzTime.mValue.longValue() - offset), this.mSavedNitzTime.mElapsedRealtime);
                            Rlog.d(LOG_TAG, "handleNetworkCountryCodeSet:adjusting time mSavedNitzTime=" + this.mSavedNitzTime);
                        }
                    } finally {
                        this.mWakeLock.release();
                    }
                }
            } else {
                NitzData nitzData2 = this.mLatestNitzSignal.mValue;
                TimeZoneLookupHelper.OffsetResult offsetResultLookupByNitzCountry = this.mTimeZoneLookupHelper.lookupByNitzCountry(nitzData2, networkCountryIsoForPhone);
                Rlog.d(LOG_TAG, "handleNetworkCountryCodeSet: using guessZoneIdByNitzCountry(nitzData, isoCountryCode), nitzData=" + nitzData2 + " isoCountryCode=" + networkCountryIsoForPhone + " lookupResult=" + offsetResultLookupByNitzCountry);
                if (offsetResultLookupByNitzCountry != null) {
                    id = offsetResultLookupByNitzCountry.zoneId;
                }
            }
            this.mTimeZoneLog.log("handleNetworkCountryCodeSet: isTimeZoneSettingInitialized=" + zIsTimeZoneSettingInitialized + " mLatestNitzSignal=" + this.mLatestNitzSignal + " isoCountryCode=" + networkCountryIsoForPhone + " mNeedCountryCodeForNitz=" + this.mNeedCountryCodeForNitz + " zoneId=" + id);
            if (id != null) {
                Rlog.d(LOG_TAG, "handleNetworkCountryCodeSet: zoneId != null, zoneId=" + id);
                if (this.mTimeServiceHelper.isTimeZoneDetectionEnabled()) {
                    setAndBroadcastNetworkSetTimeZone(id);
                } else {
                    Rlog.d(LOG_TAG, "handleNetworkCountryCodeSet: skip changing zone as isTimeZoneDetectionEnabled() is false");
                }
                if (this.mNeedCountryCodeForNitz) {
                    this.mSavedTimeZoneId = id;
                }
            } else {
                Rlog.d(LOG_TAG, "handleNetworkCountryCodeSet: lookupResult == null, do nothing");
            }
            this.mNeedCountryCodeForNitz = false;
        }
    }

    private boolean countryUsesUtc(String str, TimeStampedValue<NitzData> timeStampedValue) {
        return this.mTimeZoneLookupHelper.countryUsesUtc(str, timeStampedValue.mValue.getCurrentTimeInMillis());
    }

    public void handleNetworkAvailable() {
        Rlog.d(LOG_TAG, "handleNetworkAvailable: mNitzTimeZoneDetectionSuccessful=" + this.mNitzTimeZoneDetectionSuccessful + ", Setting mNitzTimeZoneDetectionSuccessful=false");
        this.mNitzTimeZoneDetectionSuccessful = false;
    }

    public void handleNetworkUnavailable() {
        Rlog.d(LOG_TAG, "handleNetworkUnavailable");
        this.mGotCountryCode = false;
        this.mNitzTimeZoneDetectionSuccessful = false;
    }

    private static boolean nitzOffsetMightBeBogus(NitzData nitzData) {
        return nitzData.getLocalOffsetMillis() == 0 && !nitzData.isDst();
    }

    public void handleNitzReceived(TimeStampedValue<NitzData> timeStampedValue) {
        handleTimeZoneFromNitz(timeStampedValue);
        handleTimeFromNitz(timeStampedValue);
    }

    private void handleTimeZoneFromNitz(TimeStampedValue<NitzData> timeStampedValue) {
        try {
            NitzData nitzData = timeStampedValue.mValue;
            String networkCountryIsoForPhone = this.mDeviceState.getNetworkCountryIsoForPhone();
            if (nitzData.getEmulatorHostTimeZone() != null) {
                id = nitzData.getEmulatorHostTimeZone().getID();
            } else if (this.mGotCountryCode) {
                if (!TextUtils.isEmpty(networkCountryIsoForPhone)) {
                    TimeZoneLookupHelper.OffsetResult offsetResultLookupByNitzCountry = this.mTimeZoneLookupHelper.lookupByNitzCountry(nitzData, networkCountryIsoForPhone);
                    id = offsetResultLookupByNitzCountry != null ? offsetResultLookupByNitzCountry.zoneId : null;
                } else {
                    TimeZoneLookupHelper.OffsetResult offsetResultLookupByNitz = this.mTimeZoneLookupHelper.lookupByNitz(nitzData);
                    Rlog.d(LOG_TAG, "handleTimeZoneFromNitz: guessZoneIdByNitz returned lookupResult=" + offsetResultLookupByNitz);
                    if (offsetResultLookupByNitz != null) {
                        id = offsetResultLookupByNitz.zoneId;
                    }
                }
            }
            if (id == null || this.mLatestNitzSignal == null || offsetInfoDiffers(nitzData, this.mLatestNitzSignal.mValue)) {
                this.mNeedCountryCodeForNitz = true;
                this.mLatestNitzSignal = timeStampedValue;
            }
            String str = "handleTimeZoneFromNitz: nitzSignal=" + timeStampedValue + " zoneId=" + id + " iso=" + networkCountryIsoForPhone + " mGotCountryCode=" + this.mGotCountryCode + " mNeedCountryCodeForNitz=" + this.mNeedCountryCodeForNitz + " isTimeZoneDetectionEnabled()=" + this.mTimeServiceHelper.isTimeZoneDetectionEnabled();
            Rlog.d(LOG_TAG, str);
            this.mTimeZoneLog.log(str);
            if (id != null) {
                if (this.mTimeServiceHelper.isTimeZoneDetectionEnabled()) {
                    setAndBroadcastNetworkSetTimeZone(id);
                }
                this.mNitzTimeZoneDetectionSuccessful = true;
                this.mSavedTimeZoneId = id;
            }
        } catch (RuntimeException e) {
            Rlog.e(LOG_TAG, "handleTimeZoneFromNitz: Processing NITZ data nitzSignal=" + timeStampedValue + " ex=" + e);
        }
    }

    private static boolean offsetInfoDiffers(NitzData nitzData, NitzData nitzData2) {
        return (nitzData.getLocalOffsetMillis() == nitzData2.getLocalOffsetMillis() && nitzData.isDst() == nitzData2.isDst()) ? false : true;
    }

    private void handleTimeFromNitz(TimeStampedValue<NitzData> timeStampedValue) {
        try {
            if (this.mDeviceState.getIgnoreNitz()) {
                Rlog.d(LOG_TAG, "handleTimeFromNitz: Not setting clock because gsm.ignore-nitz is set");
                return;
            }
            try {
                this.mWakeLock.acquire();
                long jElapsedRealtime = this.mTimeServiceHelper.elapsedRealtime();
                long j = jElapsedRealtime - timeStampedValue.mElapsedRealtime;
                if (j >= 0 && j <= 2147483647L) {
                    long currentTimeInMillis = timeStampedValue.mValue.getCurrentTimeInMillis() + j;
                    long jCurrentTimeMillis = currentTimeInMillis - this.mTimeServiceHelper.currentTimeMillis();
                    if (this.mTimeServiceHelper.isTimeDetectionEnabled()) {
                        String str = "handleTimeFromNitz: nitzSignal=" + timeStampedValue + " adjustedCurrentTimeMillis=" + currentTimeInMillis + " millisSinceNitzReceived= " + j + " gained=" + jCurrentTimeMillis;
                        if (this.mSavedNitzTime == null) {
                            setAndBroadcastNetworkSetTime(str + ": First update received.", currentTimeInMillis);
                        } else {
                            long jElapsedRealtime2 = this.mTimeServiceHelper.elapsedRealtime() - this.mSavedNitzTime.mElapsedRealtime;
                            int nitzUpdateSpacingMillis = this.mDeviceState.getNitzUpdateSpacingMillis();
                            int nitzUpdateDiffMillis = this.mDeviceState.getNitzUpdateDiffMillis();
                            if (jElapsedRealtime2 <= nitzUpdateSpacingMillis && Math.abs(jCurrentTimeMillis) <= nitzUpdateDiffMillis) {
                                Rlog.d(LOG_TAG, str + ": Update throttled.");
                                return;
                            }
                            setAndBroadcastNetworkSetTime(str + ": New update received.", currentTimeInMillis);
                        }
                    }
                    this.mSavedNitzTime = new TimeStampedValue<>(Long.valueOf(currentTimeInMillis), timeStampedValue.mElapsedRealtime);
                    return;
                }
                Rlog.d(LOG_TAG, "handleTimeFromNitz: not setting time, unexpected elapsedRealtime=" + jElapsedRealtime + " nitzSignal=" + timeStampedValue);
            } finally {
                this.mWakeLock.release();
            }
        } catch (RuntimeException e) {
            Rlog.e(LOG_TAG, "handleTimeFromNitz: Processing NITZ data nitzSignal=" + timeStampedValue + " ex=" + e);
        }
    }

    private void setAndBroadcastNetworkSetTimeZone(String str) {
        Rlog.d(LOG_TAG, "setAndBroadcastNetworkSetTimeZone: zoneId=" + str);
        this.mTimeServiceHelper.setDeviceTimeZone(str);
        Rlog.d(LOG_TAG, "setAndBroadcastNetworkSetTimeZone: called setDeviceTimeZone() zoneId=" + str);
    }

    private void setAndBroadcastNetworkSetTime(String str, long j) {
        if (!this.mWakeLock.isHeld()) {
            Rlog.w(LOG_TAG, "setAndBroadcastNetworkSetTime: Wake lock not held while setting device time (msg=" + str + ")");
        }
        String str2 = "setAndBroadcastNetworkSetTime: [Setting time to time=" + j + "]:" + str;
        Rlog.d(LOG_TAG, str2);
        this.mTimeLog.log(str2);
        this.mTimeServiceHelper.setDeviceTime(j);
        TelephonyMetrics.getInstance().writeNITZEvent(this.mPhone.getPhoneId(), j);
    }

    private void handleAutoTimeEnabled() {
        Rlog.d(LOG_TAG, "handleAutoTimeEnabled: Reverting to NITZ Time: mSavedNitzTime=" + this.mSavedNitzTime);
        if (this.mSavedNitzTime != null) {
            try {
                this.mWakeLock.acquire();
                long jElapsedRealtime = this.mTimeServiceHelper.elapsedRealtime();
                setAndBroadcastNetworkSetTime("mSavedNitzTime: Reverting to NITZ time elapsedRealtime=" + jElapsedRealtime + " mSavedNitzTime=" + this.mSavedNitzTime, this.mSavedNitzTime.mValue.longValue() + (jElapsedRealtime - this.mSavedNitzTime.mElapsedRealtime));
            } finally {
                this.mWakeLock.release();
            }
        }
    }

    private void handleAutoTimeZoneEnabled() {
        String str = "handleAutoTimeZoneEnabled: Reverting to NITZ TimeZone: mSavedTimeZoneId=" + this.mSavedTimeZoneId;
        Rlog.d(LOG_TAG, str);
        this.mTimeZoneLog.log(str);
        if (this.mSavedTimeZoneId != null) {
            setAndBroadcastNetworkSetTimeZone(this.mSavedTimeZoneId);
            return;
        }
        String networkCountryIsoForPhone = this.mDeviceState.getNetworkCountryIsoForPhone();
        if (!TextUtils.isEmpty(networkCountryIsoForPhone)) {
            updateTimeZoneByNetworkCountryCode(networkCountryIsoForPhone);
        }
    }

    public void dumpState(PrintWriter printWriter) {
        printWriter.println(" mSavedTime=" + this.mSavedNitzTime);
        printWriter.println(" mNeedCountryCodeForNitz=" + this.mNeedCountryCodeForNitz);
        printWriter.println(" mLatestNitzSignal=" + this.mLatestNitzSignal);
        printWriter.println(" mGotCountryCode=" + this.mGotCountryCode);
        printWriter.println(" mSavedTimeZoneId=" + this.mSavedTimeZoneId);
        printWriter.println(" mNitzTimeZoneDetectionSuccessful=" + this.mNitzTimeZoneDetectionSuccessful);
        printWriter.println(" mWakeLock=" + this.mWakeLock);
        printWriter.flush();
    }

    public void dumpLogs(FileDescriptor fileDescriptor, IndentingPrintWriter indentingPrintWriter, String[] strArr) {
        indentingPrintWriter.println(" Time Logs:");
        indentingPrintWriter.increaseIndent();
        this.mTimeLog.dump(fileDescriptor, indentingPrintWriter, strArr);
        indentingPrintWriter.decreaseIndent();
        indentingPrintWriter.println(" Time zone Logs:");
        indentingPrintWriter.increaseIndent();
        this.mTimeZoneLog.dump(fileDescriptor, indentingPrintWriter, strArr);
        indentingPrintWriter.decreaseIndent();
    }

    private void updateTimeZoneByNetworkCountryCode(String str) {
        TimeZoneLookupHelper.CountryResult countryResultLookupByCountry = this.mTimeZoneLookupHelper.lookupByCountry(str, this.mTimeServiceHelper.currentTimeMillis());
        if (countryResultLookupByCountry != null && countryResultLookupByCountry.allZonesHaveSameOffset) {
            String str2 = "updateTimeZoneByNetworkCountryCode: set time lookupResult=" + countryResultLookupByCountry + " iso=" + str;
            Rlog.d(LOG_TAG, str2);
            this.mTimeZoneLog.log(str2);
            setAndBroadcastNetworkSetTimeZone(countryResultLookupByCountry.zoneId);
            return;
        }
        Rlog.d(LOG_TAG, "updateTimeZoneByNetworkCountryCode: no good zone for iso=" + str + " lookupResult=" + countryResultLookupByCountry);
    }

    public boolean getNitzTimeZoneDetectionSuccessful() {
        return this.mNitzTimeZoneDetectionSuccessful;
    }

    public NitzData getCachedNitzData() {
        if (this.mLatestNitzSignal != null) {
            return this.mLatestNitzSignal.mValue;
        }
        return null;
    }

    public String getSavedTimeZoneId() {
        return this.mSavedTimeZoneId;
    }
}
