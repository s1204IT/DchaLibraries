package com.mediatek.services.telephony;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.telecom.Conference;
import android.telecom.ConnectionRequest;
import android.telecom.DisconnectCause;
import android.telecom.PhoneAccountHandle;
import android.telephony.PhoneNumberUtils;
import android.telephony.Rlog;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import com.android.internal.telephony.Call;
import com.android.internal.telephony.CallStateException;
import com.android.internal.telephony.Connection;
import com.android.internal.telephony.ITelephony;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.SubscriptionController;
import com.android.internal.telephony.TelephonyDevController;
import com.android.internal.telephony.imsphone.ImsPhone;
import com.android.internal.telephony.uicc.UiccController;
import com.android.phone.PhoneUtils;
import com.android.phone.R;
import com.android.phone.settings.SettingsConstants;
import com.android.services.telephony.DisconnectCauseUtil;
import com.android.services.telephony.ImsConferenceController;
import com.android.services.telephony.Log;
import com.android.services.telephony.TelecomAccountRegistry;
import com.android.services.telephony.TelephonyConnection;
import com.android.services.telephony.TelephonyConnectionService;
import com.mediatek.ims.internal.MtkImsManager;
import com.mediatek.internal.telephony.IMtkTelephonyEx;
import com.mediatek.internal.telephony.MtkGsmCdmaPhone;
import com.mediatek.internal.telephony.MtkLteDataOnlyController;
import com.mediatek.internal.telephony.MtkPhoneNumberUtils;
import com.mediatek.internal.telephony.gsm.MtkGsmMmiCode;
import com.mediatek.internal.telephony.imsphone.MtkImsPhoneMmiCode;
import com.mediatek.phone.MtkSimErrorDialog;
import com.mediatek.settings.TelephonyUtils;
import com.mediatek.telephony.MtkTelephonyManagerEx;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class MtkTelephonyConnectionServiceUtil {
    private static final MtkTelephonyConnectionServiceUtil INSTANCE = new MtkTelephonyConnectionServiceUtil();
    private static final boolean MTK_CT_VOLTE_SUPPORT = SettingsConstants.DUA_VAL_ON.equals(SystemProperties.get("persist.vendor.mtk_ct_volte_support", SettingsConstants.DUAL_VAL_OFF));
    private CellConnMgr mCellConnMgr;
    private int mCellConnMgrCurrentRun;
    private final BroadcastReceiver mCellConnMgrReceiver;
    private int mCellConnMgrState;
    private ArrayList<String> mCellConnMgrStringArray;
    private int mCellConnMgrTargetRun;
    private int mCurrentDialSlotId;
    private int mCurrentDialSubId;
    private String mEccNumber;
    private final BroadcastReceiver mPplReceiver;
    private int mEccPhoneType = 0;
    private int mEccRetryPhoneId = -1;
    private boolean mHasPerformEccRetry = false;
    TelephonyDevController mTelDevController = TelephonyDevController.getInstance();
    private TelephonyConnectionService mService = null;
    private Context mContext = null;
    private MtkSimErrorDialog mSimErrorDialog = null;
    private MtkSuppMessageManager mSuppMessageManager = null;
    private MtkLteDataOnlyController mMtkLteDataOnlyController = null;
    private EmergencyRetryHandler mEccRetryHandler = null;
    private ConcurrentHashMap<Phone, ArrayList<Connection>> mSuppMsgPhoneConnections = new ConcurrentHashMap<>();

    private boolean hasC2kOverImsModem() {
        return (this.mTelDevController == null || this.mTelDevController.getModem(0) == null || !this.mTelDevController.getModem(0).hasC2kOverImsModem()) ? false : true;
    }

    MtkTelephonyConnectionServiceUtil() {
        this.mCellConnMgrReceiver = new TcsBroadcastReceiver();
        this.mPplReceiver = new TcsBroadcastReceiver();
    }

    public static MtkTelephonyConnectionServiceUtil getInstance() {
        return INSTANCE;
    }

    public void setService(TelephonyConnectionService telephonyConnectionService) {
        Log.d(this, "setService: " + telephonyConnectionService, new Object[0]);
        this.mService = telephonyConnectionService;
        this.mContext = this.mService.getApplicationContext();
        this.mEccRetryHandler = null;
        enableSuppMessage(telephonyConnectionService);
        this.mContext.registerReceiver(this.mPplReceiver, new IntentFilter("com.mediatek.ppl.NOTIFY_LOCK"));
        this.mMtkLteDataOnlyController = new MtkLteDataOnlyController(this.mContext);
    }

    public void unsetService() {
        Log.d(this, "unSetService: " + this.mService, new Object[0]);
        this.mService = null;
        this.mEccRetryHandler = null;
        this.mEccPhoneType = 0;
        this.mEccRetryPhoneId = -1;
        this.mHasPerformEccRetry = false;
        disableSuppMessage();
        this.mContext.unregisterReceiver(this.mPplReceiver);
        this.mMtkLteDataOnlyController = null;
    }

    private void enableSuppMessage(TelephonyConnectionService telephonyConnectionService) {
        Log.d(this, "enableSuppMessage for " + telephonyConnectionService, new Object[0]);
        if (this.mSuppMessageManager == null) {
            this.mSuppMessageManager = new MtkSuppMessageManager(telephonyConnectionService);
            this.mSuppMessageManager.registerSuppMessageForPhones();
        }
    }

    private void disableSuppMessage() {
        Log.d(this, "disableSuppMessage", new Object[0]);
        if (this.mSuppMessageManager != null) {
            this.mSuppMessageManager.unregisterSuppMessageForPhones();
            this.mSuppMessageManager = null;
        }
    }

    public void forceSuppMessageUpdate(TelephonyConnection telephonyConnection) {
        Phone phone;
        if (this.mSuppMessageManager != null && (phone = telephonyConnection.getPhone()) != null) {
            Log.d(this, "forceSuppMessageUpdate for " + telephonyConnection + ", " + phone + " phone " + phone.getPhoneId(), new Object[0]);
            this.mSuppMessageManager.forceSuppMessageUpdate(telephonyConnection, phone);
        }
    }

    public boolean isECCExists() {
        if (this.mService == null || this.mService.getFgConnection() == null || this.mService.getFgConnection().getCall() == null || this.mService.getFgConnection().getCall().getEarliestConnection() == null || this.mService.getFgConnection().getCall().getPhone() == null) {
            return false;
        }
        String address = this.mService.getFgConnection().getCall().getEarliestConnection().getAddress();
        boolean z = PhoneNumberUtils.isEmergencyNumber(address) && !MtkPhoneNumberUtils.isSpecialEmergencyNumber(this.mService.getFgConnection().getCall().getPhone().getSubId(), address);
        if (z) {
            Log.d(this, "ECC call exists.", new Object[0]);
        } else {
            Log.d(this, "ECC call doesn't exists.", new Object[0]);
        }
        return z;
    }

    public boolean isDataOnlyMode(Phone phone) {
        if (this.mMtkLteDataOnlyController == null || phone == null || this.mMtkLteDataOnlyController.checkPermission(phone.getSubId())) {
            return false;
        }
        Log.i(this, "isDataOnlyMode, phoneId=" + phone.getPhoneId() + ", phoneType=" + phone.getPhoneType() + ", dataOnly=true", new Object[0]);
        return true;
    }

    private void cellConnMgrRegisterForSubEvent() {
        IntentFilter intentFilter = new IntentFilter("android.intent.action.AIRPLANE_MODE");
        intentFilter.addAction("android.telephony.action.SIM_CARD_STATE_CHANGED");
        this.mContext.registerReceiver(this.mCellConnMgrReceiver, intentFilter);
        if (this.mCurrentDialSlotId != -1) {
            int i = this.mCurrentDialSlotId;
            int simCardState = MtkTelephonyManagerEx.getDefault().getSimCardState(i);
            Log.d(this, "slotId: " + i + " simState: " + simCardState, new Object[0]);
            if (simCardState == 1) {
                Log.d(this, "MtkSimErrorDialog finish due hot plug out of SIM " + (i + 1), new Object[0]);
                this.mSimErrorDialog.dismiss();
            }
        }
    }

    private void cellConnMgrUnregisterForSubEvent() {
        this.mContext.unregisterReceiver(this.mCellConnMgrReceiver);
    }

    private class TcsBroadcastReceiver extends BroadcastReceiver {
        private TcsBroadcastReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            byte b;
            if (isInitialStickyBroadcast()) {
                Log.d(this, "Skip initial sticky broadcast", new Object[0]);
            }
            String action = intent.getAction();
            int iHashCode = action.hashCode();
            if (iHashCode != -1076576821) {
                if (iHashCode != -853584012) {
                    b = (iHashCode == 657207618 && action.equals("android.telephony.action.SIM_CARD_STATE_CHANGED")) ? (byte) 2 : (byte) -1;
                } else if (action.equals("com.mediatek.ppl.NOTIFY_LOCK")) {
                    b = 0;
                }
            } else if (action.equals("android.intent.action.AIRPLANE_MODE")) {
                b = 1;
            }
            switch (b) {
                case 0:
                    Log.d(this, "Receives com.mediatek.ppl.NOTIFY_LOCK", new Object[0]);
                    for (android.telecom.Connection connection : MtkTelephonyConnectionServiceUtil.this.mService.getAllConnections()) {
                        if (connection instanceof TelephonyConnection) {
                            ((TelephonyConnection) connection).onHangupAll();
                            break;
                        }
                    }
                    break;
                case 1:
                    Log.d(this, "MtkSimErrorDialog finish due to ACTION_AIRPLANE_MODE_CHANGED", new Object[0]);
                    MtkTelephonyConnectionServiceUtil.this.mSimErrorDialog.dismiss();
                    break;
                case 2:
                    int intExtra = intent.getIntExtra("android.telephony.extra.SIM_STATE", 0);
                    int intExtra2 = intent.getIntExtra("slot", -1);
                    Log.d(this, "slotId: " + intExtra2 + " simState: " + intExtra, new Object[0]);
                    if (intExtra2 != -1 && intExtra2 == MtkTelephonyConnectionServiceUtil.this.mCurrentDialSlotId && intExtra == 1) {
                        Log.d(this, "MtkSimErrorDialog finish due hot plug out of SIM " + (intExtra2 + 1), new Object[0]);
                        MtkTelephonyConnectionServiceUtil.this.mSimErrorDialog.dismiss();
                        break;
                    }
                    break;
            }
        }
    }

    public void cellConnMgrSetSimErrorDialogActivity(MtkSimErrorDialog mtkSimErrorDialog) {
        if (this.mContext == null) {
            Log.d(this, "cellConnMgrSetSimErrorDialogActivity, mContext is null", new Object[0]);
            return;
        }
        if (this.mSimErrorDialog == mtkSimErrorDialog) {
            Log.d(this, "cellConnMgrSetSimErrorDialogActivity, skip duplicate", new Object[0]);
            return;
        }
        this.mSimErrorDialog = mtkSimErrorDialog;
        if (this.mSimErrorDialog != null) {
            cellConnMgrRegisterForSubEvent();
            Log.d(this, "cellConnMgrRegisterForSubEvent for setSimErrorDialogActivity", new Object[0]);
        } else {
            cellConnMgrUnregisterForSubEvent();
            Log.d(this, "cellConnMgrUnregisterForSubEvent for setSimErrorDialogActivity", new Object[0]);
        }
    }

    public boolean cellConnMgrShowAlerting(int i) {
        if (this.mContext == null) {
            Log.d(this, "cellConnMgrShowAlerting, mContext is null", new Object[0]);
            return false;
        }
        if (MtkTelephonyManagerEx.getDefault().isWifiCallingEnabled(i)) {
            Log.d(this, "cellConnMgrShowAlerting: WiFi calling is enabled, return directly.", new Object[0]);
            return false;
        }
        this.mCellConnMgr = new CellConnMgr(this.mContext);
        this.mCurrentDialSubId = i;
        this.mCurrentDialSlotId = SubscriptionController.getInstance().getSlotIndex(i);
        this.mCellConnMgrState = this.mCellConnMgr.getCurrentState(this.mCurrentDialSubId, 7);
        if (this.mCellConnMgrState != 0) {
            this.mCellConnMgrStringArray = this.mCellConnMgr.getStringUsingState(this.mCurrentDialSubId, this.mCellConnMgrState);
            this.mCellConnMgrCurrentRun = 0;
            this.mCellConnMgrTargetRun = this.mCellConnMgrStringArray.size() / 4;
            Log.d(this, "cellConnMgrShowAlerting, slotId: " + this.mCurrentDialSlotId + " state: " + this.mCellConnMgrState + " size: " + this.mCellConnMgrStringArray.size(), new Object[0]);
            if (this.mCellConnMgrTargetRun > 0) {
                cellConnMgrShowAlertingInternal();
                return true;
            }
        }
        return false;
    }

    public void cellConnMgrHandleEvent() {
        this.mCellConnMgr.handleRequest(this.mCurrentDialSubId, this.mCellConnMgrState);
        this.mCellConnMgrCurrentRun++;
        if (this.mCellConnMgrCurrentRun != this.mCellConnMgrTargetRun) {
            cellConnMgrShowAlertingInternal();
        } else {
            cellConnMgrShowAlertingFinalize();
        }
    }

    private void cellConnMgrShowAlertingInternal() {
        ArrayList arrayList = new ArrayList();
        arrayList.add(this.mCellConnMgrStringArray.get(this.mCellConnMgrCurrentRun * 4));
        arrayList.add(this.mCellConnMgrStringArray.get((this.mCellConnMgrCurrentRun * 4) + 1));
        arrayList.add(this.mCellConnMgrStringArray.get((this.mCellConnMgrCurrentRun * 4) + 2));
        arrayList.add(this.mCellConnMgrStringArray.get((this.mCellConnMgrCurrentRun * 4) + 3));
        for (int i = 0; i < arrayList.size(); i++) {
            Log.d(this, "cellConnMgrShowAlertingInternal, string(" + i + ")=" + ((String) arrayList.get(i)), new Object[0]);
        }
        Log.d(this, "cellConnMgrShowAlertingInternal", new Object[0]);
        if (arrayList.size() < 4) {
            Log.d(this, "cellConnMgrShowAlertingInternal, stringArray is illegle, do nothing.", new Object[0]);
            return;
        }
        if (this.mSimErrorDialog != null) {
            Log.w(this, "cellConnMgrShowAlertingInternal, There's an existing error dialog: " + this.mSimErrorDialog + ", ignore displaying the new error.", new Object[0]);
            return;
        }
        this.mSimErrorDialog = new MtkSimErrorDialog(this.mContext, arrayList);
        Log.d(this, "cellConnMgrShowAlertingInternal, show SimErrorDialog: " + this.mSimErrorDialog, new Object[0]);
        this.mSimErrorDialog.show();
    }

    public void cellConnMgrShowAlertingFinalize() {
        Log.d(this, "cellConnMgrShowAlertingFinalize", new Object[0]);
        this.mCellConnMgrCurrentRun = -1;
        this.mCellConnMgrTargetRun = 0;
        this.mCurrentDialSubId = -1;
        this.mCurrentDialSlotId = -1;
        this.mCellConnMgrState = -1;
        this.mCellConnMgr = null;
    }

    public boolean isCellConnMgrAlive() {
        return this.mCellConnMgr != null;
    }

    private static class CellConnMgr {
        private Context mContext;

        public CellConnMgr(Context context) {
            this.mContext = context;
            if (this.mContext == null) {
                throw new RuntimeException("CellConnMgr must be created by indicated context");
            }
        }

        public int getCurrentState(int i, int i2) {
            int i3 = Settings.Global.getInt(this.mContext.getContentResolver(), "airplane_mode_on", -1);
            boolean z = !isRadioOn(i) && isRadioOffBySimManagement(i);
            int slotIndex = SubscriptionManager.getSlotIndex(i);
            TelephonyManager telephonyManager = TelephonyManager.getDefault();
            Rlog.d("CellConnMgr", "[getCurrentState]subId: " + i + ", requestType:" + i2 + "; (flight mode, radio off, locked, roaming) = (" + i3 + "," + z + "," + (2 == telephonyManager.getSimState(slotIndex) || 3 == telephonyManager.getSimState(slotIndex) || 4 == telephonyManager.getSimState(slotIndex)) + ",false)");
            switch (i2) {
                case 1:
                    i = i3 == 1 ? 1 : 0;
                    break;
                case 2:
                    if (!z) {
                        i = 0;
                    }
                    break;
                default:
                    i = (z ? 2 : 0) | (i3 != 1 ? 0 : 1);
                    break;
            }
            if (i == 0 && (i2 & 4) == 4) {
                i = isImsUnavailableForCTVolte(i) ? 4 : 0;
            }
            Rlog.d("CellConnMgr", "[getCurrentState] state:" + i);
            return i;
        }

        public ArrayList<String> getStringUsingState(int i, int i2) {
            ArrayList arrayList = new ArrayList();
            Rlog.d("CellConnMgr", "[getStringUsingState] subId: " + i + ", state:" + i2);
            if ((i2 & 3) == 3) {
                arrayList.add(Resources.getSystem().getString(134545563));
                arrayList.add(Resources.getSystem().getString(134545564));
                arrayList.add(Resources.getSystem().getString(134545571));
                arrayList.add(Resources.getSystem().getString(134545572));
                Rlog.d("CellConnMgr", "[getStringUsingState] STATE_FLIGHT_MODE + STATE_RADIO_OFF");
            } else if ((i2 & 1) == 1) {
                arrayList.add(Resources.getSystem().getString(134545548));
                arrayList.add(Resources.getSystem().getString(134545549));
                arrayList.add(Resources.getSystem().getString(134545574));
                arrayList.add(Resources.getSystem().getString(134545572));
                Rlog.d("CellConnMgr", "[getStringUsingState] STATE_FLIGHT_MODE");
            } else if ((i2 & 2) == 2) {
                arrayList.add(Resources.getSystem().getString(134545550));
                arrayList.add(Resources.getSystem().getString(134545551));
                arrayList.add(Resources.getSystem().getString(134545575));
                arrayList.add(Resources.getSystem().getString(134545572));
                Rlog.d("CellConnMgr", "[getStringUsingState] STATE_RADIO_OFF");
            } else if ((i2 & 4) == 4) {
                arrayList.add(Resources.getSystem().getString(134545569));
                arrayList.add(this.mContext.getApplicationContext().getString(R.string.alert_volte_no_service, PhoneUtils.getSubDisplayName(i)));
                arrayList.add(Resources.getSystem().getString(134545571));
                arrayList.add(Resources.getSystem().getString(134545572));
                Rlog.d("CellConnMgr", "[getStringUsingState] STATE_NOIMSREG_FOR_CTVOLTE");
            }
            Rlog.d("CellConnMgr", "[getStringUsingState]stringList size: " + arrayList.size());
            return (ArrayList) arrayList.clone();
        }

        public void handleRequest(int i, int i2) {
            Rlog.d("CellConnMgr", "[handleRequest] subId: " + i + ", state:" + i2);
            if ((i2 & 1) == 1) {
                Settings.Global.putInt(this.mContext.getContentResolver(), "airplane_mode_on", 0);
                this.mContext.sendBroadcastAsUser(new Intent("android.intent.action.AIRPLANE_MODE").putExtra("state", false), UserHandle.ALL);
                Rlog.d("CellConnMgr", "[handleRequest] Turn off flight mode.");
            }
            if ((i2 & 2) == 2) {
                int i3 = 0;
                for (int i4 = 0; i4 < TelephonyManager.getDefault().getSimCount(); i4++) {
                    int[] subId = SubscriptionManager.getSubId(i4);
                    if ((subId != null && isRadioOn(subId[0])) || i4 == SubscriptionManager.getSlotIndex(i)) {
                        i3 |= 1 << i4;
                    }
                }
                Intent intent = new Intent("com.mediatek.internal.telephony.RadioManager.intent.action.FORCE_SET_RADIO_POWER");
                intent.putExtra("mode", i3);
                this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
                Rlog.d("CellConnMgr", "[handleRequest] Turn radio on, MSIM mode:" + i3);
            }
            if ((i2 & 4) == 4) {
                MtkImsManager.setEnhanced4gLteModeSetting(this.mContext, false, SubscriptionManager.getPhoneId(i));
                Rlog.d("CellConnMgr", "[handleRequest] Turn off ct volte");
            }
        }

        private boolean isRadioOffBySimManagement(int i) {
            boolean zIsRadioOffBySimManagement;
            IMtkTelephonyEx iMtkTelephonyExAsInterface;
            try {
                iMtkTelephonyExAsInterface = IMtkTelephonyEx.Stub.asInterface(ServiceManager.getService("phoneEx"));
            } catch (RemoteException e) {
                e.printStackTrace();
                zIsRadioOffBySimManagement = true;
            }
            if (iMtkTelephonyExAsInterface == null) {
                Rlog.d("CellConnMgr", "[isRadioOffBySimManagement] iTelEx is null");
                return false;
            }
            zIsRadioOffBySimManagement = iMtkTelephonyExAsInterface.isRadioOffBySimManagement(i);
            Rlog.d("CellConnMgr", "[isRadioOffBySimManagement]  subId " + i + ", result = " + zIsRadioOffBySimManagement);
            return zIsRadioOffBySimManagement;
        }

        private boolean isRadioOn(int i) {
            boolean zIsRadioOnForSubscriber;
            ITelephony iTelephonyAsInterface;
            Rlog.d("CellConnMgr", "isRadioOff verify subId " + i);
            try {
                iTelephonyAsInterface = ITelephony.Stub.asInterface(ServiceManager.getService("phone"));
            } catch (RemoteException e) {
                e.printStackTrace();
                zIsRadioOnForSubscriber = true;
            }
            if (iTelephonyAsInterface == null) {
                Rlog.d("CellConnMgr", "isRadioOff iTel is null");
                return false;
            }
            zIsRadioOnForSubscriber = iTelephonyAsInterface.isRadioOnForSubscriber(i, this.mContext.getOpPackageName());
            Rlog.d("CellConnMgr", "isRadioOff subId " + i + " radio on? " + zIsRadioOnForSubscriber);
            return zIsRadioOnForSubscriber;
        }

        private int getNetworkType(int i) {
            int dataNetworkType = TelephonyManager.getDefault().getDataNetworkType(i);
            int voiceNetworkType = TelephonyManager.getDefault().getVoiceNetworkType(i);
            Rlog.d("CellConnMgr", "updateNetworkType(), dataNetworkType = " + dataNetworkType + ", voiceNetworkType = " + voiceNetworkType);
            if (dataNetworkType != 0) {
                return dataNetworkType;
            }
            if (voiceNetworkType != 0) {
                return voiceNetworkType;
            }
            return 0;
        }

        private boolean isInEcbmMode(int i) {
            String telephonyProperty = TelephonyManager.getTelephonyProperty(i, "vendor.ril.cdma.inecmmode_by_slot", "");
            Rlog.d("CellConnMgr", "[isInEcbmMode] phoneId = " + i + ", ecbmString = " + telephonyProperty);
            return "true".equals(telephonyProperty);
        }

        private boolean isMainPhoneId(int i) {
            return SystemProperties.getInt("persist.vendor.radio.simswitch", 1) - 1 == i;
        }

        private boolean isDualVoLTESupport() {
            return SystemProperties.getInt("persist.vendor.mims_support", 1) != 1;
        }

        private boolean isDualCTCard() {
            for (int i = 0; i < TelephonyManager.getDefault().getSimCount(); i++) {
                int[] subId = SubscriptionManager.getSubId(i);
                if (subId == null || !isCTCard(subId[0])) {
                    return false;
                }
            }
            return true;
        }

        private boolean isImsUnavailableForCTVolte(int i) {
            boolean z;
            if (MtkTelephonyConnectionServiceUtil.MTK_CT_VOLTE_SUPPORT) {
                int phoneId = SubscriptionManager.getPhoneId(i);
                if (!"OP09".equals(SystemProperties.get("persist.vendor.operator.optr"))) {
                    int i2 = Settings.Global.getInt(this.mContext.getContentResolver(), "preferred_network_mode" + i, Phone.PREFERRED_NT_MODE);
                    z = i2 == 10 || i2 == 31;
                    Rlog.d("CellConnMgr", "[isImsUnavailableForCTVolte] enable 4g = " + z);
                } else {
                    z = true;
                }
                if (!isMainPhoneId(phoneId) && isDualCTCard()) {
                    Rlog.d("CellConnMgr", "isImsUnavailableForCTVolte, dual ct case.");
                    return false;
                }
                if (z && ((isMainPhoneId(phoneId) || isDualVoLTESupport()) && isCTCard(i) && !TelephonyManager.getDefault().isNetworkRoaming(i) && MtkImsManager.isEnhanced4gLteModeSettingEnabledByUser(this.mContext, phoneId) && ((isInEcbmMode(phoneId) || getNetworkType(i) == 13 || getNetworkType(i) == 19 || getNetworkType(i) == 0) && !isImsReg(i)))) {
                    Rlog.d("CellConnMgr", "isImsUnavailableForCTVolte ture");
                    return true;
                }
            }
            return false;
        }

        private boolean isImsReg(int i) {
            boolean zIsImsRegistered = MtkTelephonyManagerEx.getDefault().isImsRegistered(i);
            Rlog.d("CellConnMgr", "[isImsReg] isImsReg = " + zIsImsRegistered);
            return zIsImsRegistered;
        }

        private boolean isCTCard(int i) {
            String simSerialNumber = ((TelephonyManager) this.mContext.getSystemService("phone")).getSimSerialNumber(i);
            if (TextUtils.isEmpty(simSerialNumber)) {
                Rlog.d("CellConnMgr", "[isCTCard] iccid is empty, , subId = " + i);
                return false;
            }
            if (!simSerialNumber.startsWith("898603") && !simSerialNumber.startsWith("898611") && !simSerialNumber.startsWith("8985302") && !simSerialNumber.startsWith("8985307")) {
                return false;
            }
            Rlog.d("CellConnMgr", "[isCTCard] iccid matches, subId = " + i);
            return true;
        }
    }

    public void setEccPhoneType(int i) {
        this.mEccPhoneType = i;
        Log.i(this, "ECC retry: setEccPhoneType, phoneType=" + i, new Object[0]);
    }

    public int getEccPhoneType() {
        return this.mEccPhoneType;
    }

    public void setEccRetryPhoneId(int i) {
        this.mEccRetryPhoneId = i;
        Log.i(this, "ECC retry: setEccRetryPhoneId, phoneId=" + i, new Object[0]);
    }

    public int getEccRetryPhoneId() {
        return this.mEccRetryPhoneId;
    }

    public boolean hasPerformEccRetry() {
        return this.mHasPerformEccRetry;
    }

    public boolean isEccRetryOn() {
        boolean z = this.mEccRetryHandler != null;
        Log.i(this, "ECC retry: isEccRetryOn, retryOn=" + z, new Object[0]);
        return z;
    }

    public void setEccRetryParams(ConnectionRequest connectionRequest, int i) {
        if (SystemProperties.getInt("vendor.gsm.gcf.testmode", 0) == 2) {
            Log.i(this, "ECC retry: setEccRetryParams, skip for FTA mode", new Object[0]);
            return;
        }
        if (TelephonyManager.getDefault().getPhoneCount() <= 1 && !MTK_CT_VOLTE_SUPPORT) {
            Log.i(this, "ECC retry: setEccRetryParams, skip for SS project", new Object[0]);
            return;
        }
        Log.i(this, "ECC retry: setEccRetryParams, request=" + connectionRequest + ", initPhoneId=" + i, new Object[0]);
        if (this.mEccRetryHandler == null) {
            this.mEccRetryHandler = new EmergencyRetryHandler(connectionRequest, i);
        }
    }

    public void clearEccRetryParams() {
        Log.i(this, "ECC retry: clearEccRetryParams", new Object[0]);
        this.mEccRetryHandler = null;
    }

    public void setEccRetryCallId(String str) {
        Log.i(this, "ECC retry: setEccRetryCallId, id=" + str, new Object[0]);
        if (this.mEccRetryHandler != null) {
            this.mEccRetryHandler.setCallId(str);
        }
    }

    public boolean eccRetryTimeout() {
        boolean z;
        if (this.mEccRetryHandler != null && this.mEccRetryHandler.isTimeout()) {
            this.mEccRetryHandler = null;
            z = true;
        } else {
            z = false;
        }
        Log.i(this, "ECC retry: eccRetryTimeout, timeout=" + z, new Object[0]);
        return z;
    }

    public void performEccRetry() {
        Log.i(this, "ECC retry: performEccRetry", new Object[0]);
        if (this.mEccRetryHandler == null || this.mService == null) {
            return;
        }
        this.mHasPerformEccRetry = true;
        this.mService.createConnectionInternal(this.mEccRetryHandler.getCallId(), new ConnectionRequest(this.mEccRetryHandler.getNextAccountHandle(), this.mEccRetryHandler.getRequest().getAddress(), this.mEccRetryHandler.getRequest().getExtras(), this.mEccRetryHandler.getRequest().getVideoState()));
    }

    public Phone selectPhoneBySpecialEccRule(PhoneAccountHandle phoneAccountHandle, String str, Phone phone) {
        EmergencyRuleHandler emergencyRuleHandler;
        if (getEccRetryPhoneId() != -1) {
            emergencyRuleHandler = new EmergencyRuleHandler(PhoneUtils.makePstnPhoneAccountHandle(Integer.toString(getEccRetryPhoneId())), str, true, phone);
        } else {
            emergencyRuleHandler = new EmergencyRuleHandler(phoneAccountHandle, str, isEccRetryOn(), phone);
        }
        return emergencyRuleHandler.getPreferredPhone();
    }

    public void setEmergencyNumber(String str) {
        this.mEccNumber = str;
    }

    public void enterEmergencyMode(Phone phone, int i) {
        if ((!hasC2kOverImsModem() && !MtkTelephonyManagerEx.getDefault().useVzwLogic() && !MtkTelephonyManagerEx.getDefault().useATTLogic()) || this.mEccNumber == null || MtkPhoneNumberUtils.isSpecialEmergencyNumber(phone.getSubId(), this.mEccNumber) || !PhoneNumberUtils.isEmergencyNumber(phone.getSubId(), this.mEccNumber)) {
            return;
        }
        if ((phone.getRadioAccessFamily() & 12784) > 0 || MtkTelephonyManagerEx.getDefault().useVzwLogic() || MtkTelephonyManagerEx.getDefault().useATTLogic()) {
            Log.d(this, "Enter Emergency Mode, airplane mode:" + i, new Object[0]);
            ((MtkGsmCdmaPhone) phone).mMtkCi.setCurrentStatus(i, phone.isImsRegistered() ? 1 : 0, (Message) null);
        }
        this.mEccNumber = null;
    }

    public void setInEcc(boolean z) {
        MtkTelephonyManagerEx mtkTelephonyManagerEx = MtkTelephonyManagerEx.getDefault();
        if (z) {
            if (!mtkTelephonyManagerEx.isEccInProgress()) {
                mtkTelephonyManagerEx.setEccInProgress(true);
                Intent intent = new Intent("android.intent.action.ECC_IN_PROGRESS");
                intent.putExtra("in_progress", z);
                this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
                return;
            }
            return;
        }
        if (mtkTelephonyManagerEx.isEccInProgress()) {
            mtkTelephonyManagerEx.setEccInProgress(false);
            Intent intent2 = new Intent("android.intent.action.ECC_IN_PROGRESS");
            intent2.putExtra("in_progress", z);
            this.mContext.sendBroadcastAsUser(intent2, UserHandle.ALL);
        }
    }

    private android.telecom.Connection createIncomingConferenceHostConnection(Phone phone, ConnectionRequest connectionRequest) {
        Log.i(this, "createIncomingConferenceHostConnection, request: " + connectionRequest, new Object[0]);
        if (this.mService == null || phone == null) {
            return android.telecom.Connection.createFailedConnection(DisconnectCauseUtil.toTelecomDisconnectCause(36));
        }
        Call ringingCall = phone.getRingingCall();
        if (!ringingCall.getState().isRinging()) {
            Log.i(this, "onCreateIncomingConferenceHostConnection, no ringing call", new Object[0]);
            return android.telecom.Connection.createFailedConnection(DisconnectCauseUtil.toTelecomDisconnectCause(1, "Found no ringing call"));
        }
        Connection latestConnection = ringingCall.getState() == Call.State.WAITING ? ringingCall.getLatestConnection() : ringingCall.getEarliestConnection();
        for (android.telecom.Connection connection : this.mService.getAllConnections()) {
            if ((connection instanceof TelephonyConnection) && ((TelephonyConnection) connection).getOriginalConnection() == latestConnection) {
                Log.i(this, "original connection already registered", new Object[0]);
                return android.telecom.Connection.createCanceledConnection();
            }
        }
        return new MtkGsmCdmaConnection(1, latestConnection, null, null, false, false);
    }

    private android.telecom.Connection createOutgoingConferenceHostConnection(Phone phone, ConnectionRequest connectionRequest, List<String> list) {
        Log.i(this, "createOutgoingConferenceHostConnection, request: " + connectionRequest, new Object[0]);
        if (phone == null) {
            Log.d(this, "createOutgoingConferenceHostConnection, phone is null", new Object[0]);
            return android.telecom.Connection.createFailedConnection(DisconnectCauseUtil.toTelecomDisconnectCause(43, "Phone is null"));
        }
        if (getInstance().cellConnMgrShowAlerting(phone.getSubId())) {
            Log.d(this, "createOutgoingConferenceHostConnection, cellConnMgrShowAlerting() check fail", new Object[0]);
            return android.telecom.Connection.createFailedConnection(DisconnectCauseUtil.toTelecomDisconnectCause(1041, "cellConnMgrShowAlerting() check fail"));
        }
        if (phone.getPhoneType() != 1) {
            Log.d(this, "createOutgoingConferenceHostConnection, phone is not GSM Phone", new Object[0]);
            return android.telecom.Connection.createFailedConnection(DisconnectCauseUtil.toTelecomDisconnectCause(43, "Phone not GSM"));
        }
        int state = phone.getServiceState().getState();
        switch (state) {
            case 0:
            case 2:
                MtkGsmCdmaConnection mtkGsmCdmaConnection = new MtkGsmCdmaConnection(1, null, null, null, false, true);
                mtkGsmCdmaConnection.setInitializing();
                mtkGsmCdmaConnection.setVideoState(connectionRequest.getVideoState());
                mtkGsmCdmaConnection.setManageImsConferenceCallSupported(TelecomAccountRegistry.getInstance(this.mContext).isManageImsConferenceCallSupported(PhoneUtils.makePstnPhoneAccountHandle(phone)));
                placeOutgoingConferenceHostConnection(mtkGsmCdmaConnection, phone, connectionRequest, list);
                return mtkGsmCdmaConnection;
            case 1:
                return android.telecom.Connection.createFailedConnection(DisconnectCauseUtil.toTelecomDisconnectCause(18, "ServiceState.STATE_OUT_OF_SERVICE"));
            case 3:
                return android.telecom.Connection.createFailedConnection(DisconnectCauseUtil.toTelecomDisconnectCause(17, "ServiceState.STATE_POWER_OFF"));
            default:
                Log.d(this, "onCreateOutgoingConnection, unknown service state: %d", Integer.valueOf(state));
                return android.telecom.Connection.createFailedConnection(DisconnectCauseUtil.toTelecomDisconnectCause(43, "Unknown service state " + state));
        }
    }

    private void placeOutgoingConferenceHostConnection(TelephonyConnection telephonyConnection, Phone phone, ConnectionRequest connectionRequest, List<String> list) {
        Connection connectionDial;
        try {
            if (phone instanceof MtkGsmCdmaPhone) {
                connectionDial = ((MtkGsmCdmaPhone) phone).dial(list, connectionRequest.getVideoState());
            } else {
                Log.d(this, "Phone is not MtkImsPhone", new Object[0]);
                connectionDial = null;
            }
            if (connectionDial == null) {
                Log.d(this, "placeOutgoingConnection, phone.dial returned null", new Object[0]);
                telephonyConnection.setDisconnected(DisconnectCauseUtil.toTelecomDisconnectCause(43, "Connection is null"));
            } else {
                telephonyConnection.setOriginalConnection(connectionDial);
            }
        } catch (CallStateException e) {
            Log.e((Object) this, (Throwable) e, "placeOutgoingConfHostConnection, phone.dial exception: " + e, new Object[0]);
            telephonyConnection.setDisconnected(DisconnectCauseUtil.toTelecomDisconnectCause(43, e.getMessage()));
        }
    }

    public Conference createConference(ImsConferenceController imsConferenceController, Phone phone, ConnectionRequest connectionRequest, List<String> list, boolean z) {
        android.telecom.Connection connectionCreateOutgoingConferenceHostConnection;
        if (imsConferenceController == null) {
            return null;
        }
        if (z) {
            connectionCreateOutgoingConferenceHostConnection = createIncomingConferenceHostConnection(phone, connectionRequest);
        } else {
            connectionCreateOutgoingConferenceHostConnection = createOutgoingConferenceHostConnection(phone, connectionRequest, list);
        }
        Log.d(this, "onCreateConference, connection: %s", connectionCreateOutgoingConferenceHostConnection);
        if (connectionCreateOutgoingConferenceHostConnection == null) {
            Log.d(this, "onCreateConference, connection: %s", new Object[0]);
            return null;
        }
        if (connectionCreateOutgoingConferenceHostConnection.getState() == 6) {
            Log.d(this, "the host connection is dicsonnected", new Object[0]);
            return createFailedConference(connectionCreateOutgoingConferenceHostConnection.getDisconnectCause());
        }
        if (!(connectionCreateOutgoingConferenceHostConnection instanceof MtkGsmCdmaConnection) || ((MtkGsmCdmaConnection) connectionCreateOutgoingConferenceHostConnection).getPhoneType() != 1) {
            Log.d(this, "abnormal case, the host connection isn't GsmConnection", new Object[0]);
            connectionCreateOutgoingConferenceHostConnection.setDisconnected(DisconnectCauseUtil.toTelecomDisconnectCause(36));
            return createFailedConference(36, "unexpected error");
        }
        if (!(imsConferenceController instanceof ImsConferenceController)) {
            Log.d(this, "abnormal case, not ImsConferenceController", new Object[0]);
            connectionCreateOutgoingConferenceHostConnection.setDisconnected(DisconnectCauseUtil.toTelecomDisconnectCause(36));
            return createFailedConference(36, "Not ImsConferenceController");
        }
        return imsConferenceController.createConference((TelephonyConnection) connectionCreateOutgoingConferenceHostConnection);
    }

    public Conference createFailedConference(int i, String str) {
        return createFailedConference(DisconnectCauseUtil.toTelecomDisconnectCause(i, str));
    }

    public Conference createFailedConference(DisconnectCause disconnectCause) {
        Conference conference = new Conference(null) {
        };
        conference.setDisconnected(disconnectCause);
        return conference;
    }

    public void registerSuppMessageForImsPhone(Phone phone, Connection connection) {
        if (this.mSuppMessageManager == null) {
            return;
        }
        if (!this.mSuppMsgPhoneConnections.containsKey(phone)) {
            ArrayList<Connection> arrayList = new ArrayList<>();
            arrayList.add(connection);
            this.mSuppMsgPhoneConnections.put(phone, arrayList);
            this.mSuppMessageManager.registerSuppMessageForPhone(phone);
            return;
        }
        ArrayList<Connection> arrayList2 = this.mSuppMsgPhoneConnections.get(phone);
        if (arrayList2.size() == 0) {
            Log.d(this, "registerSuppMessageForImsPhone: error, empty connection list", new Object[0]);
        } else {
            arrayList2.add(connection);
            Log.d(this, "registerSuppMessageForImsPhone: phone registered, add connection to list", new Object[0]);
        }
    }

    public void unregisterSuppMessageForImsPhone(Phone phone, Connection connection) {
        if (this.mSuppMessageManager == null) {
            return;
        }
        if (!this.mSuppMsgPhoneConnections.containsKey(phone)) {
            Log.d(this, "unregisterSuppMessageForImsPhone: error, phone not registered yet", new Object[0]);
            return;
        }
        ArrayList<Connection> arrayList = this.mSuppMsgPhoneConnections.get(phone);
        if (arrayList.isEmpty()) {
            Log.d(this, "unregisterSuppMessageForImsPhone: error, empty list", new Object[0]);
            return;
        }
        if (!arrayList.contains(connection)) {
            Log.d(this, "unregisterSuppMessageForImsPhone: error, Connection not in list", new Object[0]);
            return;
        }
        arrayList.remove(connection);
        if (arrayList.isEmpty()) {
            this.mSuppMsgPhoneConnections.remove(phone);
            this.mSuppMessageManager.unregisterSuppMessageForPhone(phone);
        }
    }

    private boolean isBlockedMmi(Phone phone, String str) {
        boolean zIsUtMmiCode;
        if (PhoneNumberUtils.isUriNumber(str)) {
            return false;
        }
        String strExtractNetworkPortionAlt = PhoneNumberUtils.extractNetworkPortionAlt(PhoneNumberUtils.stripSeparators(str));
        if ((!strExtractNetworkPortionAlt.startsWith("*") && !strExtractNetworkPortionAlt.startsWith("#")) || !strExtractNetworkPortionAlt.endsWith("#")) {
            return false;
        }
        ImsPhone imsPhone = phone.getImsPhone();
        boolean z = phone.isImsUseEnabled() && imsPhone != null && imsPhone.isVolteEnabled() && imsPhone.isUtEnabled() && imsPhone.getServiceState().getState() == 0;
        if (z) {
            zIsUtMmiCode = MtkImsPhoneMmiCode.isUtMmiCode(strExtractNetworkPortionAlt, imsPhone);
        } else if (phone.getPhoneType() == 1) {
            zIsUtMmiCode = MtkGsmMmiCode.isUtMmiCode(strExtractNetworkPortionAlt, (MtkGsmCdmaPhone) phone, UiccController.getInstance().getUiccCardApplication(SubscriptionController.getInstance().getSlotIndex(phone.getSubId()), 1));
        } else {
            zIsUtMmiCode = false;
        }
        Log.d(this, "isBlockedMmi = " + zIsUtMmiCode + ", imsUseEnabled = " + z, new Object[0]);
        return zIsUtMmiCode;
    }

    public boolean shouldOpenDataConnection(String str, Phone phone) {
        return isBlockedMmi(phone, str) && TelephonyUtils.shouldShowOpenMobileDataDialog(this.mContext, phone.getSubId());
    }
}
