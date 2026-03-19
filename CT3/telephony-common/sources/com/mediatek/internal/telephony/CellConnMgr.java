package com.mediatek.internal.telephony;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.provider.Settings;
import android.telephony.Rlog;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import com.android.internal.telephony.ITelephony;
import com.mediatek.internal.telephony.ITelephonyEx;
import java.util.ArrayList;

public class CellConnMgr {
    private static final String INTENT_SET_RADIO_POWER = "com.mediatek.internal.telephony.RadioManager.intent.action.FORCE_SET_RADIO_POWER";
    public static final int STATE_FLIGHT_MODE = 1;
    public static final int STATE_RADIO_OFF = 2;
    public static final int STATE_READY = 0;
    public static final int STATE_ROAMING = 8;
    public static final int STATE_SIM_LOCKED = 4;
    private static final String TAG = "CellConnMgr";
    private Context mContext;

    public CellConnMgr(Context context) {
        this.mContext = context;
        if (this.mContext != null) {
        } else {
            throw new RuntimeException("CellConnMgr must be created by indicated context");
        }
    }

    public int getCurrentState(int subId, int requestType) {
        int state;
        int flightMode = Settings.Global.getInt(this.mContext.getContentResolver(), "airplane_mode_on", -1);
        boolean zIsRadioOffBySimManagement = !isRadioOn(subId) ? isRadioOffBySimManagement(subId) : false;
        int slotId = SubscriptionManager.getSlotId(subId);
        TelephonyManager telephonyMgr = TelephonyManager.getDefault();
        boolean isLocked = 2 == telephonyMgr.getSimState(slotId) || 3 == telephonyMgr.getSimState(slotId) || 4 == telephonyMgr.getSimState(slotId);
        Rlog.d(TAG, "[getCurrentState]subId: " + subId + ", requestType:" + requestType + "; (flight mode, radio off, locked, roaming) = (" + flightMode + "," + zIsRadioOffBySimManagement + "," + isLocked + ",false)");
        switch (requestType) {
            case 1:
                state = flightMode != 1 ? 0 : 1;
                break;
            case 2:
                state = !zIsRadioOffBySimManagement ? 0 : 2;
                break;
            case 3:
            case 5:
            case 6:
            case 7:
            default:
                state = (flightMode == 1 ? 1 : 0) | (zIsRadioOffBySimManagement ? 2 : 0) | (isLocked ? 4 : 0) | (0 != 0 ? 8 : 0);
                break;
            case 4:
                state = (flightMode == 1 ? 1 : 0) | (zIsRadioOffBySimManagement ? 2 : 0) | (isLocked ? 4 : 0);
                break;
            case 8:
                state = (flightMode == 1 ? 1 : 0) | (zIsRadioOffBySimManagement ? 2 : 0) | (0 != 0 ? 8 : 0);
                break;
        }
        Rlog.d(TAG, "[getCurrentState] state:" + state);
        return state;
    }

    public ArrayList<String> getStringUsingState(int subId, int state) {
        ArrayList<String> stringList = new ArrayList<>();
        Rlog.d(TAG, "[getStringUsingState] subId: " + subId + ", state:" + state);
        if ((state & 3) == 3) {
            stringList.add(Resources.getSystem().getString(134545564));
            stringList.add(Resources.getSystem().getString(134545565));
            stringList.add(Resources.getSystem().getString(134545570));
            stringList.add(Resources.getSystem().getString(134545571));
            Rlog.d(TAG, "[getStringUsingState] STATE_FLIGHT_MODE + STATE_RADIO_OFF");
        } else if ((state & 1) == 1) {
            stringList.add(Resources.getSystem().getString(134545549));
            stringList.add(Resources.getSystem().getString(134545550));
            stringList.add(Resources.getSystem().getString(134545573));
            stringList.add(Resources.getSystem().getString(134545571));
            Rlog.d(TAG, "[getStringUsingState] STATE_FLIGHT_MODE");
        } else if ((state & 2) == 2) {
            stringList.add(Resources.getSystem().getString(134545551));
            stringList.add(Resources.getSystem().getString(134545563));
            stringList.add(Resources.getSystem().getString(134545574));
            stringList.add(Resources.getSystem().getString(134545571));
            Rlog.d(TAG, "[getStringUsingState] STATE_RADIO_OFF");
        } else if ((state & 4) == 4) {
            stringList.add(Resources.getSystem().getString(134545566));
            stringList.add(Resources.getSystem().getString(134545567));
            stringList.add(Resources.getSystem().getString(134545572));
            stringList.add(Resources.getSystem().getString(134545571));
            Rlog.d(TAG, "[getStringUsingState] STATE_SIM_LOCKED");
        }
        Rlog.d(TAG, "[getStringUsingState]stringList size: " + stringList.size());
        return (ArrayList) stringList.clone();
    }

    public void handleRequest(int subId, int state) {
        Rlog.d(TAG, "[handleRequest] subId: " + subId + ", state:" + state);
        if ((state & 1) == 1) {
            Settings.Global.putInt(this.mContext.getContentResolver(), "airplane_mode_on", 0);
            this.mContext.sendBroadcastAsUser(new Intent("android.intent.action.AIRPLANE_MODE").putExtra("state", false), UserHandle.ALL);
            Rlog.d(TAG, "[handleRequest] Turn off flight mode.");
        }
        if ((state & 2) == 2) {
            int mSimMode = 0;
            for (int i = 0; i < TelephonyManager.getDefault().getSimCount(); i++) {
                int[] targetSubId = SubscriptionManager.getSubId(i);
                if ((targetSubId != null && isRadioOn(targetSubId[0])) || i == SubscriptionManager.getSlotId(subId)) {
                    mSimMode |= 1 << i;
                }
            }
            Settings.Global.putInt(this.mContext.getContentResolver(), "msim_mode_setting", mSimMode);
            Intent intent = new Intent("com.mediatek.internal.telephony.RadioManager.intent.action.FORCE_SET_RADIO_POWER");
            intent.putExtra("mode", mSimMode);
            this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
            Rlog.d(TAG, "[handleRequest] Turn radio on, MSIM mode:" + mSimMode);
        }
        if ((state & 1) == 1 || (state & 2) == 2 || (state & 4) != 4) {
            return;
        }
        try {
            ITelephonyEx iTelEx = ITelephonyEx.Stub.asInterface(ServiceManager.getService("phoneEx"));
            iTelEx.broadcastIccUnlockIntent(subId);
            Rlog.d(TAG, "[handleRequest] broadcastIccUnlockIntent");
        } catch (RemoteException ex) {
            ex.printStackTrace();
        } catch (NullPointerException ex2) {
            Rlog.d(TAG, "ITelephonyEx is null");
            ex2.printStackTrace();
        }
    }

    private boolean isRadioOffBySimManagement(int subId) {
        ITelephonyEx iTelEx;
        boolean result = true;
        try {
            iTelEx = ITelephonyEx.Stub.asInterface(ServiceManager.getService("phoneEx"));
        } catch (RemoteException ex) {
            ex.printStackTrace();
        }
        if (iTelEx == null) {
            Rlog.d(TAG, "[isRadioOffBySimManagement] iTelEx is null");
            return false;
        }
        result = iTelEx.isRadioOffBySimManagement(subId);
        Rlog.d(TAG, "[isRadioOffBySimManagement]  subId " + subId + ", result = " + result);
        return result;
    }

    private boolean isRadioOn(int subId) {
        ITelephony iTel;
        Rlog.d(TAG, "isRadioOff verify subId " + subId);
        boolean radioOn = true;
        try {
            iTel = ITelephony.Stub.asInterface(ServiceManager.getService("phone"));
        } catch (RemoteException ex) {
            ex.printStackTrace();
        }
        if (iTel == null) {
            Rlog.d(TAG, "isRadioOff iTel is null");
            return false;
        }
        radioOn = iTel.isRadioOnForSubscriber(subId, this.mContext.getOpPackageName());
        Rlog.d(TAG, "isRadioOff subId " + subId + " radio on? " + radioOn);
        return radioOn;
    }
}
