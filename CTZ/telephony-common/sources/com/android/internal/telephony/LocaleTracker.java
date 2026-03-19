package com.android.internal.telephony;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.storage.StorageManager;
import android.telephony.CellInfo;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoWcdma;
import android.telephony.Rlog;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.LocalLog;
import com.android.internal.util.CollectionUtils;
import com.android.internal.util.IndentingPrintWriter;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class LocaleTracker extends Handler {
    private static final long CELL_INFO_MAX_DELAY_MS = 600000;
    private static final long CELL_INFO_MIN_DELAY_MS = 2000;
    private static final long CELL_INFO_PERIODIC_POLLING_DELAY_MS = 600000;
    private static final boolean DBG = true;
    private static final int EVENT_GET_CELL_INFO = 1;
    private static final int EVENT_SERVICE_STATE_CHANGED = 3;
    private static final int EVENT_UPDATE_OPERATOR_NUMERIC = 2;
    private static final String TAG = LocaleTracker.class.getSimpleName();
    private final BroadcastReceiver mBroadcastReceiver;
    private List<CellInfo> mCellInfo;
    private String mCurrentCountryIso;
    private int mFailCellInfoCount;
    private int mLastServiceState;
    private final LocalLog mLocalLog;
    private String mOperatorNumeric;
    private final Phone mPhone;
    private int mSimState;

    @Override
    public void handleMessage(Message message) {
        switch (message.what) {
            case 1:
                synchronized (this) {
                    getCellInfo();
                    updateLocale();
                    break;
                }
                return;
            case 2:
                updateOperatorNumericSync((String) message.obj);
                return;
            case 3:
                onServiceStateChanged((ServiceState) ((AsyncResult) message.obj).result);
                return;
            default:
                throw new IllegalStateException("Unexpected message arrives. msg = " + message.what);
        }
    }

    public LocaleTracker(Phone phone, Looper looper) {
        super(looper);
        this.mLastServiceState = -1;
        this.mLocalLog = new LocalLog(50);
        this.mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if ("android.telephony.action.SIM_CARD_STATE_CHANGED".equals(intent.getAction()) && intent.getIntExtra("phone", 0) == LocaleTracker.this.mPhone.getPhoneId()) {
                    LocaleTracker.this.onSimCardStateChanged(intent.getIntExtra("android.telephony.extra.SIM_STATE", 0));
                }
            }
        };
        this.mPhone = phone;
        this.mSimState = 0;
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.telephony.action.SIM_CARD_STATE_CHANGED");
        this.mPhone.getContext().registerReceiver(this.mBroadcastReceiver, intentFilter);
        this.mPhone.registerForServiceStateChanged(this, 3, null);
    }

    public synchronized String getCurrentCountry() {
        return this.mCurrentCountryIso != null ? this.mCurrentCountryIso : "";
    }

    private String getMccFromCellInfo() {
        String mccString;
        if (this.mCellInfo == null) {
            return null;
        }
        HashMap map = new HashMap();
        int i = 0;
        String str = null;
        for (CellInfo cellInfo : this.mCellInfo) {
            if (cellInfo instanceof CellInfoGsm) {
                mccString = ((CellInfoGsm) cellInfo).getCellIdentity().getMccString();
            } else if (cellInfo instanceof CellInfoLte) {
                mccString = ((CellInfoLte) cellInfo).getCellIdentity().getMccString();
            } else if (cellInfo instanceof CellInfoWcdma) {
                mccString = ((CellInfoWcdma) cellInfo).getCellIdentity().getMccString();
            } else {
                mccString = null;
            }
            if (mccString != null) {
                int iIntValue = map.containsKey(mccString) ? 1 + ((Integer) map.get(mccString)).intValue() : 1;
                map.put(mccString, Integer.valueOf(iIntValue));
                if (iIntValue > i) {
                    str = mccString;
                    i = iIntValue;
                }
            }
        }
        return str;
    }

    private synchronized void onSimCardStateChanged(int i) {
        if (this.mSimState != i && i == 1) {
            log("Sim absent. Get latest cell info from the modem.");
            getCellInfo();
            updateLocale();
        }
        this.mSimState = i;
    }

    private void onServiceStateChanged(ServiceState serviceState) {
        int state = serviceState.getState();
        if (state != this.mLastServiceState) {
            if (state == 3 || !TextUtils.isEmpty(this.mOperatorNumeric)) {
                if (state == 3) {
                    if (this.mCellInfo != null) {
                        this.mCellInfo.clear();
                    }
                    stopCellInfoRetry();
                }
            } else {
                String str = "Service state " + ServiceState.rilServiceStateToString(state) + ". Get cell info now.";
                log(str);
                this.mLocalLog.log(str);
                getCellInfo();
            }
            updateLocale();
            this.mLastServiceState = state;
        }
    }

    public synchronized void updateOperatorNumericSync(String str) {
        log("updateOperatorNumericSync. mcc/mnc=" + str);
        if (!Objects.equals(this.mOperatorNumeric, str)) {
            String str2 = "Operator numeric changes to " + str;
            log(str2);
            this.mLocalLog.log(str2);
            this.mOperatorNumeric = str;
            if (TextUtils.isEmpty(this.mOperatorNumeric)) {
                log("Operator numeric unavailable. Get latest cell info from the modem.");
                getCellInfo();
            } else {
                if (this.mCellInfo != null) {
                    this.mCellInfo.clear();
                }
                stopCellInfoRetry();
            }
            updateLocale();
        }
    }

    public void updateOperatorNumericAsync(String str) {
        log("updateOperatorNumericAsync. mcc/mnc=" + str);
        sendMessage(obtainMessage(2, str));
    }

    private long getCellInfoDelayTime(int i) {
        long jPow = ((long) Math.pow(2.0d, i - 1)) * CELL_INFO_MIN_DELAY_MS;
        if (jPow < CELL_INFO_MIN_DELAY_MS) {
            return CELL_INFO_MIN_DELAY_MS;
        }
        if (jPow > 600000) {
            return 600000L;
        }
        return jPow;
    }

    private void stopCellInfoRetry() {
        this.mFailCellInfoCount = 0;
        removeMessages(1);
    }

    private void getCellInfo() {
        if (!this.mPhone.getServiceStateTracker().getDesiredPowerState()) {
            if (this.mCellInfo != null) {
                this.mCellInfo.clear();
            }
            log("Radio is off. Stopped cell info retry. Cleared the previous cached cell info.");
            this.mLocalLog.log("Radio is off. Stopped cell info retry. Cleared the previous cached cell info.");
            stopCellInfoRetry();
            return;
        }
        this.mCellInfo = this.mPhone.getAllCellInfo(null);
        String str = "getCellInfo: cell info=" + this.mCellInfo;
        log(str);
        this.mLocalLog.log(str);
        if (CollectionUtils.isEmpty(this.mCellInfo)) {
            int i = this.mFailCellInfoCount + 1;
            this.mFailCellInfoCount = i;
            long cellInfoDelayTime = getCellInfoDelayTime(i);
            log("Can't get cell info. Try again in " + (cellInfoDelayTime / 1000) + " secs.");
            removeMessages(1);
            sendMessageDelayed(obtainMessage(1), cellInfoDelayTime);
            return;
        }
        stopCellInfoRetry();
        sendMessageDelayed(obtainMessage(1), 600000L);
    }

    private void updateLocale() {
        String strCountryCodeForMcc = "";
        String mccFromCellInfo = null;
        if (!TextUtils.isEmpty(this.mOperatorNumeric)) {
            try {
                String strSubstring = this.mOperatorNumeric.substring(0, 3);
                try {
                    strCountryCodeForMcc = MccTable.countryCodeForMcc(Integer.parseInt(strSubstring));
                    mccFromCellInfo = strSubstring;
                } catch (NumberFormatException | StringIndexOutOfBoundsException e) {
                    mccFromCellInfo = strSubstring;
                    e = e;
                    loge("updateLocale: Can't get country from operator numeric. mcc = " + mccFromCellInfo + ". ex=" + e);
                }
            } catch (NumberFormatException | StringIndexOutOfBoundsException e2) {
                e = e2;
            }
        }
        if (TextUtils.isEmpty(strCountryCodeForMcc)) {
            mccFromCellInfo = getMccFromCellInfo();
            if (!TextUtils.isEmpty(mccFromCellInfo)) {
                try {
                    strCountryCodeForMcc = MccTable.countryCodeForMcc(Integer.parseInt(mccFromCellInfo));
                } catch (NumberFormatException e3) {
                    loge("updateLocale: Can't get country from cell info. mcc = " + mccFromCellInfo + ". ex=" + e3);
                }
            }
        }
        String str = "updateLocale: mcc = " + mccFromCellInfo + ", country = " + strCountryCodeForMcc;
        log(str);
        this.mLocalLog.log(str);
        if (!Objects.equals(strCountryCodeForMcc, this.mCurrentCountryIso)) {
            String str2 = "updateLocale: Change the current country to " + strCountryCodeForMcc;
            log(str2);
            this.mLocalLog.log(str2);
            this.mCurrentCountryIso = strCountryCodeForMcc;
            TelephonyManager.setTelephonyProperty(this.mPhone.getPhoneId(), "gsm.operator.iso-country", this.mCurrentCountryIso);
            if (!StorageManager.inCryptKeeperBounce()) {
                WifiManager wifiManager = (WifiManager) this.mPhone.getContext().getSystemService("wifi");
                if (wifiManager == null) {
                    loge("WifiManager is null");
                } else {
                    wifiManager.setCountryCode(strCountryCodeForMcc);
                }
            }
        }
    }

    private void log(String str) {
        Rlog.d(TAG, str);
    }

    private void loge(String str) {
        Rlog.e(TAG, str);
    }

    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        IndentingPrintWriter indentingPrintWriter = new IndentingPrintWriter(printWriter, "  ");
        printWriter.println("LocaleTracker:");
        indentingPrintWriter.increaseIndent();
        indentingPrintWriter.println("mOperatorNumeric = " + this.mOperatorNumeric);
        indentingPrintWriter.println("mSimState = " + this.mSimState);
        indentingPrintWriter.println("mCellInfo = " + this.mCellInfo);
        indentingPrintWriter.println("mCurrentCountryIso = " + this.mCurrentCountryIso);
        indentingPrintWriter.println("mFailCellInfoCount = " + this.mFailCellInfoCount);
        indentingPrintWriter.println("Local logs:");
        indentingPrintWriter.increaseIndent();
        this.mLocalLog.dump(fileDescriptor, indentingPrintWriter, strArr);
        indentingPrintWriter.decreaseIndent();
        indentingPrintWriter.decreaseIndent();
        indentingPrintWriter.flush();
    }
}
