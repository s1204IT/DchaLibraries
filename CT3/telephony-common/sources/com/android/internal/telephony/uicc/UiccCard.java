package com.android.internal.telephony.uicc;

import android.R;
import android.app.AlertDialog;
import android.app.usage.UsageStatsManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.content.res.Resources;
import android.net.Uri;
import android.os.AsyncResult;
import android.os.Binder;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.Registrant;
import android.os.RegistrantList;
import android.os.SystemProperties;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.telephony.Rlog;
import android.text.TextUtils;
import android.util.LocalLog;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.TelephonyEventLog;
import com.android.internal.telephony.cat.CatService;
import com.android.internal.telephony.uicc.IccCardApplicationStatus;
import com.android.internal.telephony.uicc.IccCardStatus;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

public class UiccCard {
    protected static final boolean DBG = true;
    private static final int EVENT_CARD_ADDED = 14;
    private static final int EVENT_CARD_REMOVED = 13;
    private static final int EVENT_CARRIER_PRIVILIGES_LOADED = 20;
    private static final int EVENT_CLOSE_LOGICAL_CHANNEL_DONE = 16;
    private static final int EVENT_GET_ATR_DONE = 100;
    private static final int EVENT_OPEN_CHANNEL_WITH_SW_DONE = 101;
    private static final int EVENT_OPEN_LOGICAL_CHANNEL_DONE = 15;
    private static final int EVENT_SIM_IO_DONE = 19;
    private static final int EVENT_TRANSMIT_APDU_BASIC_CHANNEL_DONE = 18;
    private static final int EVENT_TRANSMIT_APDU_LOGICAL_CHANNEL_DONE = 17;
    public static final String EXTRA_ICC_CARD_ADDED = "com.android.internal.telephony.uicc.ICC_CARD_ADDED";
    protected static final String LOG_TAG = "UiccCard";
    private static final String OPERATOR_BRAND_OVERRIDE_PREFIX = "operator_branding_";
    private RegistrantList mAbsentRegistrants;
    private IccCardStatus.CardState mCardState;
    private RegistrantList mCarrierPrivilegeRegistrants;
    private UiccCarrierPrivilegeRules mCarrierPrivilegeRules;
    private CatService mCatService;
    private int mCdmaSubscriptionAppIndex;
    private CommandsInterface mCi;
    private Context mContext;
    private int mGsmUmtsSubscriptionAppIndex;
    protected Handler mHandler;
    private String mIccType;
    private int mImsSubscriptionAppIndex;
    private CommandsInterface.RadioState mLastRadioState;
    private final Object mLock;
    private int mPhoneId;
    private UiccCardApplication[] mUiccApplications;
    private IccCardStatus.PinState mUniversalPinState;
    private static final LocalLog mLocalLog = new LocalLog(100);
    static final String[] UICCCARD_PROPERTY_RIL_UICC_TYPE = {"gsm.ril.uicctype", "gsm.ril.uicctype.2", "gsm.ril.uicctype.3", "gsm.ril.uicctype.4"};
    private static final String[] PROPERTY_RIL_FULL_UICC_TYPE = {"gsm.ril.fulluicctype", "gsm.ril.fulluicctype.2", "gsm.ril.fulluicctype.3", "gsm.ril.fulluicctype.4"};

    public UiccCard(Context c, CommandsInterface ci, IccCardStatus ics) {
        this.mLock = new Object();
        this.mGsmUmtsSubscriptionAppIndex = -1;
        this.mCdmaSubscriptionAppIndex = -1;
        this.mImsSubscriptionAppIndex = -1;
        this.mUiccApplications = new UiccCardApplication[8];
        this.mLastRadioState = CommandsInterface.RadioState.RADIO_UNAVAILABLE;
        this.mAbsentRegistrants = new RegistrantList();
        this.mCarrierPrivilegeRegistrants = new RegistrantList();
        this.mIccType = null;
        this.mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                UiccCard.this.log("Received message " + msg + "[" + msg.what + "]");
                switch (msg.what) {
                    case 13:
                        UiccCard.this.onIccSwap(false);
                        break;
                    case 14:
                        UiccCard.this.onIccSwap(true);
                        break;
                    case 15:
                    case 16:
                    case 17:
                    case 18:
                    case 19:
                    case 100:
                    case 101:
                        AsyncResult ar = (AsyncResult) msg.obj;
                        if (ar.exception != null) {
                            UiccCard.this.loglocal("Exception: " + ar.exception);
                            UiccCard.this.loge("Error in SIM access with exception" + ar.exception);
                        }
                        AsyncResult.forMessage((Message) ar.userObj, ar.result, ar.exception);
                        ((Message) ar.userObj).sendToTarget();
                        break;
                    case 20:
                        UiccCard.this.onCarrierPriviligesLoadedMessage();
                        break;
                    default:
                        UiccCard.this.loge("Unknown Event " + msg.what);
                        break;
                }
            }
        };
        log("Creating");
        this.mCardState = ics.mCardState;
        update(c, ci, ics);
    }

    public UiccCard(Context c, CommandsInterface ci, IccCardStatus ics, int phoneId) {
        this.mLock = new Object();
        this.mGsmUmtsSubscriptionAppIndex = -1;
        this.mCdmaSubscriptionAppIndex = -1;
        this.mImsSubscriptionAppIndex = -1;
        this.mUiccApplications = new UiccCardApplication[8];
        this.mLastRadioState = CommandsInterface.RadioState.RADIO_UNAVAILABLE;
        this.mAbsentRegistrants = new RegistrantList();
        this.mCarrierPrivilegeRegistrants = new RegistrantList();
        this.mIccType = null;
        this.mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                UiccCard.this.log("Received message " + msg + "[" + msg.what + "]");
                switch (msg.what) {
                    case 13:
                        UiccCard.this.onIccSwap(false);
                        break;
                    case 14:
                        UiccCard.this.onIccSwap(true);
                        break;
                    case 15:
                    case 16:
                    case 17:
                    case 18:
                    case 19:
                    case 100:
                    case 101:
                        AsyncResult ar = (AsyncResult) msg.obj;
                        if (ar.exception != null) {
                            UiccCard.this.loglocal("Exception: " + ar.exception);
                            UiccCard.this.loge("Error in SIM access with exception" + ar.exception);
                        }
                        AsyncResult.forMessage((Message) ar.userObj, ar.result, ar.exception);
                        ((Message) ar.userObj).sendToTarget();
                        break;
                    case 20:
                        UiccCard.this.onCarrierPriviligesLoadedMessage();
                        break;
                    default:
                        UiccCard.this.loge("Unknown Event " + msg.what);
                        break;
                }
            }
        };
        this.mCardState = ics.mCardState;
        this.mPhoneId = phoneId;
        log("Creating");
        update(c, ci, ics);
    }

    protected UiccCard() {
        this.mLock = new Object();
        this.mGsmUmtsSubscriptionAppIndex = -1;
        this.mCdmaSubscriptionAppIndex = -1;
        this.mImsSubscriptionAppIndex = -1;
        this.mUiccApplications = new UiccCardApplication[8];
        this.mLastRadioState = CommandsInterface.RadioState.RADIO_UNAVAILABLE;
        this.mAbsentRegistrants = new RegistrantList();
        this.mCarrierPrivilegeRegistrants = new RegistrantList();
        this.mIccType = null;
        this.mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                UiccCard.this.log("Received message " + msg + "[" + msg.what + "]");
                switch (msg.what) {
                    case 13:
                        UiccCard.this.onIccSwap(false);
                        break;
                    case 14:
                        UiccCard.this.onIccSwap(true);
                        break;
                    case 15:
                    case 16:
                    case 17:
                    case 18:
                    case 19:
                    case 100:
                    case 101:
                        AsyncResult ar = (AsyncResult) msg.obj;
                        if (ar.exception != null) {
                            UiccCard.this.loglocal("Exception: " + ar.exception);
                            UiccCard.this.loge("Error in SIM access with exception" + ar.exception);
                        }
                        AsyncResult.forMessage((Message) ar.userObj, ar.result, ar.exception);
                        ((Message) ar.userObj).sendToTarget();
                        break;
                    case 20:
                        UiccCard.this.onCarrierPriviligesLoadedMessage();
                        break;
                    default:
                        UiccCard.this.loge("Unknown Event " + msg.what);
                        break;
                }
            }
        };
    }

    public void dispose() {
        synchronized (this.mLock) {
            log("Disposing card");
            if (this.mCatService != null) {
                this.mCatService.dispose();
            }
            for (UiccCardApplication app : this.mUiccApplications) {
                if (app != null) {
                    app.dispose();
                }
            }
            this.mCatService = null;
            this.mUiccApplications = null;
            this.mCarrierPrivilegeRules = null;
        }
    }

    public void update(Context c, CommandsInterface ci, IccCardStatus ics) {
        synchronized (this.mLock) {
            IccCardStatus.CardState oldState = this.mCardState;
            this.mCardState = ics.mCardState;
            this.mUniversalPinState = ics.mUniversalPinState;
            this.mGsmUmtsSubscriptionAppIndex = ics.mGsmUmtsSubscriptionAppIndex;
            this.mCdmaSubscriptionAppIndex = ics.mCdmaSubscriptionAppIndex;
            this.mImsSubscriptionAppIndex = ics.mImsSubscriptionAppIndex;
            this.mContext = c;
            this.mCi = ci;
            log(ics.mApplications.length + " applications");
            for (int i = 0; i < this.mUiccApplications.length; i++) {
                if (this.mUiccApplications[i] == null) {
                    if (i < ics.mApplications.length) {
                        this.mUiccApplications[i] = new UiccCardApplication(this, ics.mApplications[i], this.mContext, this.mCi);
                    }
                } else if (i >= ics.mApplications.length) {
                    this.mUiccApplications[i].dispose();
                    this.mUiccApplications[i] = null;
                } else {
                    this.mUiccApplications[i].update(ics.mApplications[i], this.mContext, this.mCi);
                }
            }
            createAndUpdateCatService();
            log("Before privilege rules: " + this.mCarrierPrivilegeRules + " : " + this.mCardState);
            if (this.mCarrierPrivilegeRules == null && this.mCardState == IccCardStatus.CardState.CARDSTATE_PRESENT) {
                this.mCarrierPrivilegeRules = new UiccCarrierPrivilegeRules(this, this.mHandler.obtainMessage(20));
            } else if (this.mCarrierPrivilegeRules != null && this.mCardState != IccCardStatus.CardState.CARDSTATE_PRESENT) {
                this.mCarrierPrivilegeRules = null;
            }
            sanitizeApplicationIndexes();
            CommandsInterface.RadioState radioState = this.mCi.getRadioState();
            log("update: radioState=" + radioState + " mLastRadioState=" + this.mLastRadioState);
            if (radioState == CommandsInterface.RadioState.RADIO_ON && this.mLastRadioState == CommandsInterface.RadioState.RADIO_ON) {
                if (oldState != IccCardStatus.CardState.CARDSTATE_ABSENT && this.mCardState == IccCardStatus.CardState.CARDSTATE_ABSENT) {
                    log("update: notify card removed");
                    this.mAbsentRegistrants.notifyRegistrants();
                    this.mHandler.sendMessage(this.mHandler.obtainMessage(13, null));
                } else if (oldState == IccCardStatus.CardState.CARDSTATE_ABSENT && this.mCardState != IccCardStatus.CardState.CARDSTATE_ABSENT) {
                    log("update: notify card added");
                    this.mHandler.sendMessage(this.mHandler.obtainMessage(14, null));
                }
            }
            this.mLastRadioState = radioState;
        }
    }

    protected void createAndUpdateCatService() {
        if (this.mUiccApplications.length > 0 && this.mUiccApplications[0] != null) {
            if (this.mCatService == null) {
                this.mCatService = CatService.getInstance(this.mCi, this.mContext, this, this.mPhoneId);
                return;
            } else {
                this.mCatService.update(this.mCi, this.mContext, this);
                return;
            }
        }
        if (this.mCatService != null) {
            this.mCatService.dispose();
        }
        this.mCatService = null;
    }

    public CatService getCatService() {
        return this.mCatService;
    }

    protected void finalize() {
        log("UiccCard finalized");
    }

    private void sanitizeApplicationIndexes() {
        this.mGsmUmtsSubscriptionAppIndex = checkIndex(this.mGsmUmtsSubscriptionAppIndex, IccCardApplicationStatus.AppType.APPTYPE_SIM, IccCardApplicationStatus.AppType.APPTYPE_USIM);
        this.mCdmaSubscriptionAppIndex = checkIndex(this.mCdmaSubscriptionAppIndex, IccCardApplicationStatus.AppType.APPTYPE_RUIM, IccCardApplicationStatus.AppType.APPTYPE_CSIM);
        this.mImsSubscriptionAppIndex = checkIndex(this.mImsSubscriptionAppIndex, IccCardApplicationStatus.AppType.APPTYPE_ISIM, null);
        log("sanitizeApplicationIndexes  GSM index= " + this.mGsmUmtsSubscriptionAppIndex + "  CDMA index = " + this.mCdmaSubscriptionAppIndex + "  IMS index = " + this.mImsSubscriptionAppIndex);
    }

    private int checkIndex(int index, IccCardApplicationStatus.AppType expectedAppType, IccCardApplicationStatus.AppType altExpectedAppType) {
        if (this.mUiccApplications == null || index >= this.mUiccApplications.length) {
            loge("App index " + index + " is invalid since there are no applications");
            return -1;
        }
        if (index < 0) {
            return -1;
        }
        if (this.mUiccApplications[index] == null) {
            loge("App index " + index + " is null since there are no applications");
            return -1;
        }
        log("checkIndex mUiccApplications[" + index + "].getType()= " + this.mUiccApplications[index].getType());
        if (this.mUiccApplications[index].getType() != expectedAppType && this.mUiccApplications[index].getType() != altExpectedAppType) {
            loge("App index " + index + " is invalid since it's not " + expectedAppType + " and not " + altExpectedAppType);
            return -1;
        }
        return index;
    }

    public void registerForAbsent(Handler h, int what, Object obj) {
        synchronized (this.mLock) {
            Registrant r = new Registrant(h, what, obj);
            this.mAbsentRegistrants.add(r);
            if (this.mCardState == IccCardStatus.CardState.CARDSTATE_ABSENT) {
                r.notifyRegistrant();
            }
        }
    }

    public void unregisterForAbsent(Handler h) {
        synchronized (this.mLock) {
            this.mAbsentRegistrants.remove(h);
        }
    }

    public void registerForCarrierPrivilegeRulesLoaded(Handler h, int what, Object obj) {
        synchronized (this.mLock) {
            Registrant r = new Registrant(h, what, obj);
            this.mCarrierPrivilegeRegistrants.add(r);
            if (areCarrierPriviligeRulesLoaded()) {
                r.notifyRegistrant();
            }
        }
    }

    public void unregisterForCarrierPrivilegeRulesLoaded(Handler h) {
        synchronized (this.mLock) {
            this.mCarrierPrivilegeRegistrants.remove(h);
        }
    }

    private void onIccSwap(boolean isAdded) {
        this.mContext.getResources().getBoolean(R.^attr-private.enableControlView);
        if (1 != 0) {
            log("onIccSwap: isHotSwapSupported is true, don't prompt for rebooting");
        } else {
            log("onIccSwap: isHotSwapSupported is false, prompt for rebooting");
            promptForRestart(isAdded);
        }
    }

    private void promptForRestart(boolean isAdded) {
        synchronized (this.mLock) {
            Resources res = this.mContext.getResources();
            String dialogComponent = res.getString(R.string.config_systemSupervision);
            if (dialogComponent != null) {
                Intent intent = new Intent().setComponent(ComponentName.unflattenFromString(dialogComponent)).addFlags(268435456).putExtra(EXTRA_ICC_CARD_ADDED, isAdded);
                try {
                    this.mContext.startActivity(intent);
                    return;
                } catch (ActivityNotFoundException e) {
                    loge("Unable to find ICC hotswap prompt for restart activity: " + e);
                    DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            synchronized (UiccCard.this.mLock) {
                                if (which == -1) {
                                    UiccCard.this.log("Reboot due to SIM swap");
                                    PowerManager pm = (PowerManager) UiccCard.this.mContext.getSystemService("power");
                                    pm.reboot("SIM is added.");
                                }
                            }
                        }
                    };
                    Resources r = Resources.getSystem();
                    if (!isAdded) {
                    }
                    if (!isAdded) {
                    }
                    String buttonTxt = r.getString(R.string.face_dangling_notification_title);
                    AlertDialog dialog = new AlertDialog.Builder(this.mContext).setTitle(title).setMessage(message).setPositiveButton(buttonTxt, listener).create();
                    dialog.getWindow().setType(TelephonyEventLog.TAG_IMS_CALL_RECEIVE);
                    dialog.show();
                    return;
                }
            }
            DialogInterface.OnClickListener listener2 = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog2, int which) {
                    synchronized (UiccCard.this.mLock) {
                        if (which == -1) {
                            UiccCard.this.log("Reboot due to SIM swap");
                            PowerManager pm = (PowerManager) UiccCard.this.mContext.getSystemService("power");
                            pm.reboot("SIM is added.");
                        }
                    }
                }
            };
            Resources r2 = Resources.getSystem();
            String title = !isAdded ? r2.getString(R.string.face_authenticated_no_confirmation_required) : r2.getString(R.string.face_acquired_too_similar);
            String message = !isAdded ? r2.getString(R.string.face_dangling_notification_msg) : r2.getString(R.string.face_app_setting_name);
            String buttonTxt2 = r2.getString(R.string.face_dangling_notification_title);
            AlertDialog dialog2 = new AlertDialog.Builder(this.mContext).setTitle(title).setMessage(message).setPositiveButton(buttonTxt2, listener2).create();
            dialog2.getWindow().setType(TelephonyEventLog.TAG_IMS_CALL_RECEIVE);
            dialog2.show();
            return;
        }
    }

    private boolean isPackageInstalled(String pkgName) {
        PackageManager pm = this.mContext.getPackageManager();
        try {
            pm.getPackageInfo(pkgName, 1);
            log(pkgName + " is installed.");
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            log(pkgName + " is not installed.");
            return false;
        }
    }

    private class ClickListener implements DialogInterface.OnClickListener {
        String pkgName;

        public ClickListener(String pkgName) {
            this.pkgName = pkgName;
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            synchronized (UiccCard.this.mLock) {
                if (which == -1) {
                    Intent market = new Intent("android.intent.action.VIEW");
                    market.setData(Uri.parse("market://details?id=" + this.pkgName));
                    market.addFlags(268435456);
                    UiccCard.this.mContext.startActivity(market);
                } else if (which == -2) {
                    UiccCard.this.log("Not now clicked for carrier app dialog.");
                }
            }
        }
    }

    private void promptInstallCarrierApp(String pkgName) {
        DialogInterface.OnClickListener listener = new ClickListener(pkgName);
        Resources r = Resources.getSystem();
        String message = r.getString(R.string.face_dialog_default_subtitle);
        String buttonTxt = r.getString(R.string.face_error_canceled);
        String notNowTxt = r.getString(R.string.face_error_hw_not_available);
        AlertDialog dialog = new AlertDialog.Builder(this.mContext).setMessage(message).setNegativeButton(notNowTxt, listener).setPositiveButton(buttonTxt, listener).create();
        dialog.getWindow().setType(TelephonyEventLog.TAG_IMS_CALL_RECEIVE);
        dialog.show();
    }

    private void onCarrierPriviligesLoadedMessage() {
        UsageStatsManager usm = (UsageStatsManager) this.mContext.getSystemService("usagestats");
        if (usm != null) {
            usm.onCarrierPrivilegedAppsChanged();
        }
        synchronized (this.mLock) {
            this.mCarrierPrivilegeRegistrants.notifyRegistrants();
            String whitelistSetting = Settings.Global.getString(this.mContext.getContentResolver(), "carrier_app_whitelist");
            if (TextUtils.isEmpty(whitelistSetting)) {
                return;
            }
            HashSet<String> carrierAppSet = new HashSet<>(Arrays.asList(whitelistSetting.split("\\s*;\\s*")));
            if (carrierAppSet.isEmpty()) {
                return;
            }
            List<String> pkgNames = this.mCarrierPrivilegeRules.getPackageNames();
            for (String pkgName : pkgNames) {
                if (!TextUtils.isEmpty(pkgName) && carrierAppSet.contains(pkgName) && !isPackageInstalled(pkgName)) {
                    promptInstallCarrierApp(pkgName);
                }
            }
        }
    }

    public boolean isApplicationOnIcc(IccCardApplicationStatus.AppType type) {
        synchronized (this.mLock) {
            for (int i = 0; i < this.mUiccApplications.length; i++) {
                if (this.mUiccApplications[i] != null && this.mUiccApplications[i].getType() == type) {
                    return true;
                }
            }
            return false;
        }
    }

    public IccCardStatus.CardState getCardState() {
        IccCardStatus.CardState cardState;
        synchronized (this.mLock) {
            cardState = this.mCardState;
        }
        return cardState;
    }

    public IccCardStatus.PinState getUniversalPinState() {
        IccCardStatus.PinState pinState;
        synchronized (this.mLock) {
            pinState = this.mUniversalPinState;
        }
        return pinState;
    }

    public UiccCardApplication getApplication(int family) {
        synchronized (this.mLock) {
            int index = 8;
            switch (family) {
                case 1:
                    index = this.mGsmUmtsSubscriptionAppIndex;
                    break;
                case 2:
                    index = this.mCdmaSubscriptionAppIndex;
                    break;
                case 3:
                    index = this.mImsSubscriptionAppIndex;
                    break;
            }
            if (index >= 0 && index < this.mUiccApplications.length) {
                return this.mUiccApplications[index];
            }
            return null;
        }
    }

    public UiccCardApplication getApplicationIndex(int index) {
        synchronized (this.mLock) {
            if (index >= 0) {
                if (index < this.mUiccApplications.length) {
                    return this.mUiccApplications[index];
                }
            }
            return null;
        }
    }

    public UiccCardApplication getApplicationByType(int type) {
        synchronized (this.mLock) {
            for (int i = 0; i < this.mUiccApplications.length; i++) {
                if (this.mUiccApplications[i] != null && this.mUiccApplications[i].getType().ordinal() == type) {
                    return this.mUiccApplications[i];
                }
            }
            return null;
        }
    }

    public boolean resetAppWithAid(String aid) {
        boolean changed;
        synchronized (this.mLock) {
            changed = false;
            for (int i = 0; i < this.mUiccApplications.length; i++) {
                if (this.mUiccApplications[i] != null && (aid == null || aid.equals(this.mUiccApplications[i].getAid()))) {
                    this.mUiccApplications[i].dispose();
                    this.mUiccApplications[i] = null;
                    changed = true;
                }
            }
        }
        return changed;
    }

    public void iccOpenLogicalChannel(String AID, Message response) {
        loglocal("Open Logical Channel: " + AID + " by pid:" + Binder.getCallingPid() + " uid:" + Binder.getCallingUid());
        this.mCi.iccOpenLogicalChannel(AID, this.mHandler.obtainMessage(15, response));
    }

    public void iccCloseLogicalChannel(int channel, Message response) {
        loglocal("Close Logical Channel: " + channel);
        this.mCi.iccCloseLogicalChannel(channel, this.mHandler.obtainMessage(16, response));
    }

    public void iccTransmitApduLogicalChannel(int channel, int cla, int command, int p1, int p2, int p3, String data, Message response) {
        this.mCi.iccTransmitApduLogicalChannel(channel, cla, command, p1, p2, p3, data, this.mHandler.obtainMessage(17, response));
    }

    public void iccTransmitApduBasicChannel(int cla, int command, int p1, int p2, int p3, String data, Message response) {
        this.mCi.iccTransmitApduBasicChannel(cla, command, p1, p2, p3, data, this.mHandler.obtainMessage(18, response));
    }

    public void iccExchangeSimIO(int fileID, int command, int p1, int p2, int p3, String pathID, Message response) {
        this.mCi.iccIO(command, fileID, pathID, p1, p2, p3, null, null, this.mHandler.obtainMessage(19, response));
    }

    public void sendEnvelopeWithStatus(String contents, Message response) {
        this.mCi.sendEnvelopeWithStatus(contents, response);
    }

    public int getNumApplications() {
        int count = 0;
        for (UiccCardApplication a : this.mUiccApplications) {
            if (a != null) {
                count++;
            }
        }
        return count;
    }

    public int getPhoneId() {
        return this.mPhoneId;
    }

    public boolean areCarrierPriviligeRulesLoaded() {
        if (this.mCarrierPrivilegeRules != null) {
            return this.mCarrierPrivilegeRules.areCarrierPriviligeRulesLoaded();
        }
        return true;
    }

    public boolean hasCarrierPrivilegeRules() {
        if (this.mCarrierPrivilegeRules != null) {
            return this.mCarrierPrivilegeRules.hasCarrierPrivilegeRules();
        }
        return false;
    }

    public int getCarrierPrivilegeStatus(Signature signature, String packageName) {
        if (this.mCarrierPrivilegeRules == null) {
            return -1;
        }
        return this.mCarrierPrivilegeRules.getCarrierPrivilegeStatus(signature, packageName);
    }

    public int getCarrierPrivilegeStatus(PackageManager packageManager, String packageName) {
        if (this.mCarrierPrivilegeRules == null) {
            return -1;
        }
        return this.mCarrierPrivilegeRules.getCarrierPrivilegeStatus(packageManager, packageName);
    }

    public int getCarrierPrivilegeStatus(PackageInfo packageInfo) {
        if (this.mCarrierPrivilegeRules == null) {
            return -1;
        }
        return this.mCarrierPrivilegeRules.getCarrierPrivilegeStatus(packageInfo);
    }

    public int getCarrierPrivilegeStatusForCurrentTransaction(PackageManager packageManager) {
        if (this.mCarrierPrivilegeRules == null) {
            return -1;
        }
        return this.mCarrierPrivilegeRules.getCarrierPrivilegeStatusForCurrentTransaction(packageManager);
    }

    public List<String> getCarrierPackageNamesForIntent(PackageManager packageManager, Intent intent) {
        if (this.mCarrierPrivilegeRules == null) {
            return null;
        }
        return this.mCarrierPrivilegeRules.getCarrierPackageNamesForIntent(packageManager, intent);
    }

    public boolean setOperatorBrandOverride(String brand) {
        log("setOperatorBrandOverride: " + brand);
        log("current iccId: " + getIccId());
        String iccId = getIccId();
        if (TextUtils.isEmpty(iccId)) {
            return false;
        }
        SharedPreferences.Editor spEditor = PreferenceManager.getDefaultSharedPreferences(this.mContext).edit();
        String key = OPERATOR_BRAND_OVERRIDE_PREFIX + iccId;
        if (brand == null) {
            spEditor.remove(key).commit();
            return true;
        }
        spEditor.putString(key, brand).commit();
        return true;
    }

    public String getOperatorBrandOverride() {
        String iccId = getIccId();
        if (TextUtils.isEmpty(iccId)) {
            return null;
        }
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this.mContext);
        return sp.getString(OPERATOR_BRAND_OVERRIDE_PREFIX + iccId, null);
    }

    public String getIccId() {
        IccRecords ir;
        for (UiccCardApplication app : this.mUiccApplications) {
            if (app != null && (ir = app.getIccRecords()) != null && ir.getIccId() != null) {
                return ir.getIccId();
            }
        }
        return null;
    }

    private void log(String msg) {
        Rlog.d(LOG_TAG, msg + " (phoneId " + this.mPhoneId + ")");
    }

    private void loge(String msg) {
        Rlog.e(LOG_TAG, msg + " (phoneId " + this.mPhoneId + ")");
    }

    private void loglocal(String msg) {
        mLocalLog.log(msg);
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        IccRecords ir;
        pw.println("UiccCard:");
        pw.println(" mCi=" + this.mCi);
        pw.println(" mLastRadioState=" + this.mLastRadioState);
        pw.println(" mCatService=" + this.mCatService);
        pw.println(" mAbsentRegistrants: size=" + this.mAbsentRegistrants.size());
        for (int i = 0; i < this.mAbsentRegistrants.size(); i++) {
            pw.println("  mAbsentRegistrants[" + i + "]=" + ((Registrant) this.mAbsentRegistrants.get(i)).getHandler());
        }
        for (int i2 = 0; i2 < this.mCarrierPrivilegeRegistrants.size(); i2++) {
            pw.println("  mCarrierPrivilegeRegistrants[" + i2 + "]=" + ((Registrant) this.mCarrierPrivilegeRegistrants.get(i2)).getHandler());
        }
        pw.println(" mCardState=" + this.mCardState);
        pw.println(" mUniversalPinState=" + this.mUniversalPinState);
        pw.println(" mGsmUmtsSubscriptionAppIndex=" + this.mGsmUmtsSubscriptionAppIndex);
        pw.println(" mCdmaSubscriptionAppIndex=" + this.mCdmaSubscriptionAppIndex);
        pw.println(" mImsSubscriptionAppIndex=" + this.mImsSubscriptionAppIndex);
        pw.println(" mImsSubscriptionAppIndex=" + this.mImsSubscriptionAppIndex);
        pw.println(" mUiccApplications: length=" + this.mUiccApplications.length);
        for (int i3 = 0; i3 < this.mUiccApplications.length; i3++) {
            if (this.mUiccApplications[i3] == null) {
                pw.println("  mUiccApplications[" + i3 + "]=" + ((Object) null));
            } else {
                pw.println("  mUiccApplications[" + i3 + "]=" + this.mUiccApplications[i3].getType() + " " + this.mUiccApplications[i3]);
            }
        }
        pw.println();
        for (UiccCardApplication app : this.mUiccApplications) {
            if (app != null) {
                app.dump(fd, pw, args);
                pw.println();
            }
        }
        for (UiccCardApplication app2 : this.mUiccApplications) {
            if (app2 != null && (ir = app2.getIccRecords()) != null) {
                ir.dump(fd, pw, args);
                pw.println();
            }
        }
        if (this.mCarrierPrivilegeRules == null) {
            pw.println(" mCarrierPrivilegeRules: null");
        } else {
            pw.println(" mCarrierPrivilegeRules: " + this.mCarrierPrivilegeRules);
            this.mCarrierPrivilegeRules.dump(fd, pw, args);
        }
        pw.println(" mCarrierPrivilegeRegistrants: size=" + this.mCarrierPrivilegeRegistrants.size());
        for (int i4 = 0; i4 < this.mCarrierPrivilegeRegistrants.size(); i4++) {
            pw.println("  mCarrierPrivilegeRegistrants[" + i4 + "]=" + ((Registrant) this.mCarrierPrivilegeRegistrants.get(i4)).getHandler());
        }
        pw.flush();
        pw.println("mLocalLog:");
        mLocalLog.dump(fd, pw, args);
        pw.flush();
    }

    public UiccCard(Context c, CommandsInterface ci, IccCardStatus ics, int phoneId, boolean isUpdateSiminfo) {
        this.mLock = new Object();
        this.mGsmUmtsSubscriptionAppIndex = -1;
        this.mCdmaSubscriptionAppIndex = -1;
        this.mImsSubscriptionAppIndex = -1;
        this.mUiccApplications = new UiccCardApplication[8];
        this.mLastRadioState = CommandsInterface.RadioState.RADIO_UNAVAILABLE;
        this.mAbsentRegistrants = new RegistrantList();
        this.mCarrierPrivilegeRegistrants = new RegistrantList();
        this.mIccType = null;
        this.mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                UiccCard.this.log("Received message " + msg + "[" + msg.what + "]");
                switch (msg.what) {
                    case 13:
                        UiccCard.this.onIccSwap(false);
                        break;
                    case 14:
                        UiccCard.this.onIccSwap(true);
                        break;
                    case 15:
                    case 16:
                    case 17:
                    case 18:
                    case 19:
                    case 100:
                    case 101:
                        AsyncResult ar = (AsyncResult) msg.obj;
                        if (ar.exception != null) {
                            UiccCard.this.loglocal("Exception: " + ar.exception);
                            UiccCard.this.loge("Error in SIM access with exception" + ar.exception);
                        }
                        AsyncResult.forMessage((Message) ar.userObj, ar.result, ar.exception);
                        ((Message) ar.userObj).sendToTarget();
                        break;
                    case 20:
                        UiccCard.this.onCarrierPriviligesLoadedMessage();
                        break;
                    default:
                        UiccCard.this.loge("Unknown Event " + msg.what);
                        break;
                }
            }
        };
        log("Creating simId " + phoneId + ",isUpdateSiminfo" + isUpdateSiminfo);
        this.mCardState = ics.mCardState;
        this.mPhoneId = phoneId;
        update(c, ci, ics, isUpdateSiminfo);
    }

    public void iccExchangeSimIOEx(int fileID, int command, int p1, int p2, int p3, String pathID, String data, String pin2, Message onComplete) {
        this.mCi.iccIO(command, fileID, pathID, p1, p2, p3, data, pin2, this.mHandler.obtainMessage(19, onComplete));
    }

    public void iccGetAtr(Message onComplete) {
        this.mCi.iccGetATR(this.mHandler.obtainMessage(100, onComplete));
    }

    public String getIccCardType() {
        this.mIccType = SystemProperties.get(UICCCARD_PROPERTY_RIL_UICC_TYPE[this.mPhoneId]);
        log("getIccCardType(): iccType = " + this.mIccType + ", slot " + this.mPhoneId);
        return this.mIccType;
    }

    public String[] getFullIccCardType() {
        return SystemProperties.get(PROPERTY_RIL_FULL_UICC_TYPE[this.mPhoneId]).split(",");
    }

    public void iccOpenChannelWithSw(String AID, Message onComplete) {
        this.mCi.iccOpenChannelWithSw(AID, this.mHandler.obtainMessage(101, onComplete));
    }

    public void update(Context c, CommandsInterface ci, IccCardStatus ics, boolean isUpdateSimInfo) {
        synchronized (this.mLock) {
            IccCardStatus.CardState oldState = this.mCardState;
            this.mCardState = ics.mCardState;
            this.mUniversalPinState = ics.mUniversalPinState;
            this.mGsmUmtsSubscriptionAppIndex = ics.mGsmUmtsSubscriptionAppIndex;
            this.mCdmaSubscriptionAppIndex = ics.mCdmaSubscriptionAppIndex;
            this.mImsSubscriptionAppIndex = ics.mImsSubscriptionAppIndex;
            this.mContext = c;
            this.mCi = ci;
            log(ics.mApplications.length + " applications");
            for (int i = 0; i < this.mUiccApplications.length; i++) {
                if (this.mUiccApplications[i] == null) {
                    if (i < ics.mApplications.length) {
                        this.mUiccApplications[i] = new UiccCardApplication(this, ics.mApplications[i], this.mContext, this.mCi);
                    }
                } else if (i >= ics.mApplications.length) {
                    this.mUiccApplications[i].dispose();
                    this.mUiccApplications[i] = null;
                } else {
                    this.mUiccApplications[i].update(ics.mApplications[i], this.mContext, this.mCi);
                }
            }
            createAndUpdateCatService();
            log("Before privilege rules: " + this.mCarrierPrivilegeRules + " : " + this.mCardState);
            if (this.mCarrierPrivilegeRules == null && this.mCardState == IccCardStatus.CardState.CARDSTATE_PRESENT) {
                this.mCarrierPrivilegeRules = new UiccCarrierPrivilegeRules(this, this.mHandler.obtainMessage(20));
            } else if (this.mCarrierPrivilegeRules != null && this.mCardState != IccCardStatus.CardState.CARDSTATE_PRESENT) {
                this.mCarrierPrivilegeRules = null;
            }
            sanitizeApplicationIndexes();
            CommandsInterface.RadioState radioState = this.mCi.getRadioState();
            log("update: radioState=" + radioState + " mLastRadioState=" + this.mLastRadioState + "isUpdateSimInfo= " + isUpdateSimInfo);
            if (isUpdateSimInfo && radioState == CommandsInterface.RadioState.RADIO_ON && this.mLastRadioState == CommandsInterface.RadioState.RADIO_ON) {
                if (oldState != IccCardStatus.CardState.CARDSTATE_ABSENT && this.mCardState == IccCardStatus.CardState.CARDSTATE_ABSENT) {
                    log("update: notify card removed");
                    this.mAbsentRegistrants.notifyRegistrants();
                    this.mHandler.sendMessage(this.mHandler.obtainMessage(13, null));
                } else if (oldState == IccCardStatus.CardState.CARDSTATE_ABSENT && this.mCardState != IccCardStatus.CardState.CARDSTATE_ABSENT) {
                    log("update: notify card added");
                    this.mHandler.sendMessage(this.mHandler.obtainMessage(14, null));
                }
            }
            this.mLastRadioState = radioState;
        }
    }
}
