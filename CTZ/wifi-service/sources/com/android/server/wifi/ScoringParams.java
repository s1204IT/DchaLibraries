package com.android.server.wifi;

import android.R;
import android.content.Context;
import android.database.ContentObserver;
import android.os.Handler;
import android.provider.Settings;
import android.util.KeyValueListParser;
import android.util.Log;
import com.android.server.wifi.scanner.ChannelHelper;

public class ScoringParams {
    public static final int BAND2 = 2400;
    public static final int BAND5 = 5000;
    private static final String COMMA_KEY_VAL_STAR = "^(,[A-Za-z_][A-Za-z0-9_]*=[0-9.:+-]+)*$";
    private static final int ENTRY = 1;
    private static final int EXIT = 0;
    private static final int GOOD = 3;
    private static final int MINIMUM_5GHZ_BAND_FREQUENCY_IN_MEGAHERTZ = 5000;
    private static final int SUFFICIENT = 2;
    private static final String TAG = "WifiScoringParams";
    private Values mVal = new Values();

    private class Values {
        public static final String KEY_EXPID = "expid";
        public static final String KEY_HORIZON = "horizon";
        public static final String KEY_NUD = "nud";
        public static final String KEY_PPS = "pps";
        public static final String KEY_RSSI2 = "rssi2";
        public static final String KEY_RSSI5 = "rssi5";
        public static final int MAX_EXPID = Integer.MAX_VALUE;
        public static final int MAX_HORIZON = 60;
        public static final int MAX_NUD = 10;
        public static final int MIN_EXPID = 0;
        public static final int MIN_HORIZON = -9;
        public static final int MIN_NUD = 0;
        public int expid;
        public int horizon;
        public int nud;
        public final int[] pps;
        public final int[] rssi2;
        public final int[] rssi5;

        Values() {
            this.rssi2 = new int[]{-83, -80, -73, -60};
            this.rssi5 = new int[]{-80, -77, -70, -57};
            this.pps = new int[]{0, 1, 100};
            this.horizon = 15;
            this.nud = 8;
            this.expid = 0;
        }

        Values(Values values) {
            this.rssi2 = new int[]{-83, -80, -73, -60};
            this.rssi5 = new int[]{-80, -77, -70, -57};
            this.pps = new int[]{0, 1, 100};
            this.horizon = 15;
            this.nud = 8;
            this.expid = 0;
            for (int i = 0; i < this.rssi2.length; i++) {
                this.rssi2[i] = values.rssi2[i];
            }
            for (int i2 = 0; i2 < this.rssi5.length; i2++) {
                this.rssi5[i2] = values.rssi5[i2];
            }
            for (int i3 = 0; i3 < this.pps.length; i3++) {
                this.pps[i3] = values.pps[i3];
            }
            this.horizon = values.horizon;
            this.nud = values.nud;
            this.expid = values.expid;
        }

        public void validate() throws IllegalArgumentException {
            validateRssiArray(this.rssi2);
            validateRssiArray(this.rssi5);
            validateOrderedNonNegativeArray(this.pps);
            validateRange(this.horizon, -9, 60);
            validateRange(this.nud, 0, 10);
            validateRange(this.expid, 0, MAX_EXPID);
        }

        private void validateRssiArray(int[] iArr) throws IllegalArgumentException {
            int iMin = Math.min(ChannelHelper.SCAN_PERIOD_PER_CHANNEL_MS, -1);
            int i = -126;
            for (int i2 = 0; i2 < iArr.length; i2++) {
                validateRange(iArr[i2], i, iMin);
                i = iArr[i2];
            }
        }

        private void validateRange(int i, int i2, int i3) throws IllegalArgumentException {
            if (i < i2 || i > i3) {
                throw new IllegalArgumentException();
            }
        }

        private void validateOrderedNonNegativeArray(int[] iArr) throws IllegalArgumentException {
            int i = 0;
            for (int i2 = 0; i2 < iArr.length; i2++) {
                if (iArr[i2] < i) {
                    throw new IllegalArgumentException();
                }
                i = iArr[i2];
            }
        }

        public void parseString(String str) throws IllegalArgumentException {
            KeyValueListParser keyValueListParser = new KeyValueListParser(',');
            keyValueListParser.setString(str);
            if (keyValueListParser.size() != ("" + str).split(",").length) {
                throw new IllegalArgumentException("dup keys");
            }
            updateIntArray(this.rssi2, keyValueListParser, KEY_RSSI2);
            updateIntArray(this.rssi5, keyValueListParser, KEY_RSSI5);
            updateIntArray(this.pps, keyValueListParser, KEY_PPS);
            this.horizon = updateInt(keyValueListParser, KEY_HORIZON, this.horizon);
            this.nud = updateInt(keyValueListParser, KEY_NUD, this.nud);
            this.expid = updateInt(keyValueListParser, KEY_EXPID, this.expid);
        }

        private int updateInt(KeyValueListParser keyValueListParser, String str, int i) throws IllegalArgumentException {
            String string = keyValueListParser.getString(str, (String) null);
            if (string == null) {
                return i;
            }
            try {
                return Integer.parseInt(string);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException();
            }
        }

        private void updateIntArray(int[] iArr, KeyValueListParser keyValueListParser, String str) throws IllegalArgumentException {
            if (keyValueListParser.getString(str, (String) null) == null) {
                return;
            }
            int[] intArray = keyValueListParser.getIntArray(str, (int[]) null);
            if (intArray == null) {
                throw new IllegalArgumentException();
            }
            if (intArray.length != iArr.length) {
                throw new IllegalArgumentException();
            }
            for (int i = 0; i < iArr.length; i++) {
                iArr[i] = intArray[i];
            }
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            appendKey(sb, KEY_RSSI2);
            appendInts(sb, this.rssi2);
            appendKey(sb, KEY_RSSI5);
            appendInts(sb, this.rssi5);
            appendKey(sb, KEY_PPS);
            appendInts(sb, this.pps);
            appendKey(sb, KEY_HORIZON);
            sb.append(this.horizon);
            appendKey(sb, KEY_NUD);
            sb.append(this.nud);
            appendKey(sb, KEY_EXPID);
            sb.append(this.expid);
            return sb.toString();
        }

        private void appendKey(StringBuilder sb, String str) {
            if (sb.length() != 0) {
                sb.append(",");
            }
            sb.append(str);
            sb.append("=");
        }

        private void appendInts(StringBuilder sb, int[] iArr) {
            int length = iArr.length;
            for (int i = 0; i < length; i++) {
                if (i > 0) {
                    sb.append(":");
                }
                sb.append(iArr[i]);
            }
        }
    }

    public ScoringParams() {
    }

    public ScoringParams(Context context) {
        loadResources(context);
    }

    public ScoringParams(Context context, FrameworkFacade frameworkFacade, Handler handler) {
        loadResources(context);
        setupContentObserver(context, frameworkFacade, handler);
    }

    private void loadResources(Context context) {
        this.mVal.rssi2[0] = context.getResources().getInteger(R.integer.config_lowBatteryCloseWarningBump);
        this.mVal.rssi2[1] = context.getResources().getInteger(R.integer.config_lowMemoryKillerMinFreeKbytesAbsolute);
        this.mVal.rssi2[2] = context.getResources().getInteger(R.integer.config_maxResolverActivityColumns);
        this.mVal.rssi2[3] = context.getResources().getInteger(R.integer.config_maxNumVisibleRecentTasks);
        this.mVal.rssi5[0] = context.getResources().getInteger(R.integer.config_lowBatteryWarningLevel);
        this.mVal.rssi5[1] = context.getResources().getInteger(R.integer.config_lowMemoryKillerMinFreeKbytesAdjust);
        this.mVal.rssi5[2] = context.getResources().getInteger(R.integer.config_maxScanTasksForHomeVisibility);
        this.mVal.rssi5[3] = context.getResources().getInteger(R.integer.config_maxNumVisibleRecentTasks_lowRam);
        try {
            this.mVal.validate();
        } catch (IllegalArgumentException e) {
            Log.wtf(TAG, "Inconsistent config_wifi_framework_ resources: " + this, e);
        }
    }

    private void setupContentObserver(final Context context, final FrameworkFacade frameworkFacade, Handler handler) {
        final String string = toString();
        ContentObserver contentObserver = new ContentObserver(handler) {
            @Override
            public void onChange(boolean z) {
                String stringSetting = frameworkFacade.getStringSetting(context, "wifi_score_params");
                this.update(string);
                if (!this.update(stringSetting)) {
                    Log.e(ScoringParams.TAG, "Error in wifi_score_params: " + ScoringParams.this.sanitize(stringSetting));
                }
                Log.i(ScoringParams.TAG, this.toString());
            }
        };
        frameworkFacade.registerContentObserver(context, Settings.Global.getUriFor("wifi_score_params"), true, contentObserver);
        contentObserver.onChange(false);
    }

    public boolean update(String str) {
        if (str == null || "".equals(str)) {
            return true;
        }
        if (!("," + str).matches(COMMA_KEY_VAL_STAR)) {
            return false;
        }
        Values values = new Values(this.mVal);
        try {
            values.parseString(str);
            values.validate();
            this.mVal = values;
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public String sanitize(String str) {
        if (str == null) {
            return "";
        }
        String strReplaceAll = str.replaceAll("[^A-Za-z_0-9=,:.+-]", "?");
        if (strReplaceAll.length() > 100) {
            return strReplaceAll.substring(0, 98) + "...";
        }
        return strReplaceAll;
    }

    public int getExitRssi(int i) {
        return getRssiArray(i)[0];
    }

    public int getEntryRssi(int i) {
        return getRssiArray(i)[1];
    }

    public int getSufficientRssi(int i) {
        return getRssiArray(i)[2];
    }

    public int getGoodRssi(int i) {
        return getRssiArray(i)[3];
    }

    public int getHorizonSeconds() {
        return this.mVal.horizon;
    }

    public int getYippeeSkippyPacketsPerSecond() {
        return this.mVal.pps[2];
    }

    public int getNudKnob() {
        return this.mVal.nud;
    }

    public int getExperimentIdentifier() {
        return this.mVal.expid;
    }

    private int[] getRssiArray(int i) {
        if (i < 5000) {
            return this.mVal.rssi2;
        }
        return this.mVal.rssi5;
    }

    public String toString() {
        return this.mVal.toString();
    }
}
