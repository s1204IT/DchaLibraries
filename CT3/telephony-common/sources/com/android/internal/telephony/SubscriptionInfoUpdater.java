package com.android.internal.telephony;

import android.app.ActivityManagerNative;
import android.app.IUserSwitchObserver;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.IPackageManager;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.IRemoteCallback;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserManager;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.provider.Telephony;
import android.telephony.CarrierConfigManager;
import android.telephony.Rlog;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.cat.CatService;
import com.android.internal.telephony.uicc.IccCardProxy;
import com.android.internal.telephony.uicc.IccConstants;
import com.android.internal.telephony.uicc.IccFileHandler;
import com.android.internal.telephony.uicc.IccRecords;
import com.android.internal.telephony.uicc.IccUtils;
import com.android.internal.telephony.uicc.SpnOverride;
import com.mediatek.internal.telephony.DefaultSmsSimSettings;
import com.mediatek.internal.telephony.DefaultVoiceCallSubSettings;
import com.mediatek.internal.telephony.uicc.UsimPBMemInfo;
import com.mediatek.internal.telephony.worldphone.IWorldPhone;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReferenceArray;

public class SubscriptionInfoUpdater extends Handler {
    private static final String COMMON_SLOT_PROPERTY = "ro.mtk_sim_hot_swap_common_slot";
    public static final String CURR_SUBID = "curr_subid";
    private static final int EVENT_GET_NETWORK_SELECTION_MODE_DONE = 2;
    private static final int EVENT_RADIO_AVAILABLE = 101;
    private static final int EVENT_RADIO_UNAVAILABLE = 102;
    private static final int EVENT_SIM_ABSENT = 4;
    private static final int EVENT_SIM_IO_ERROR = 6;
    private static final int EVENT_SIM_LOADED = 3;
    private static final int EVENT_SIM_LOCKED = 5;
    private static final int EVENT_SIM_LOCKED_QUERY_ICCID_DONE = 1;
    private static final int EVENT_SIM_NO_CHANGED = 103;
    private static final int EVENT_SIM_PLUG_OUT = 105;
    private static final int EVENT_SIM_READY = 100;
    private static final int EVENT_SIM_UNKNOWN = 7;
    private static final int EVENT_TRAY_PLUG_IN = 104;
    private static final String ICCID_STRING_FOR_NO_SIM = "N/A";
    private static final String LOG_TAG = "SubscriptionInfoUpdater";
    public static final int SIM_CHANGED = -1;
    public static final int SIM_NEW = -2;
    public static final int SIM_NOT_CHANGE = 0;
    public static final int SIM_NOT_INSERT = -99;
    public static final int SIM_REPOSITION = -3;
    public static final int STATUS_NO_SIM_INSERTED = 0;
    public static final int STATUS_SIM1_INSERTED = 1;
    public static final int STATUS_SIM2_INSERTED = 2;
    public static final int STATUS_SIM3_INSERTED = 4;
    public static final int STATUS_SIM4_INSERTED = 8;
    private static Phone[] mPhone = null;
    private static final int sReadICCID_retry_time = 1000;
    private CarrierServiceBindHelper mCarrierServiceBindHelper;
    private CommandsInterface[] mCis;
    private int mCurrentlyActiveUserId;
    private IPackageManager mPackageManager;
    private SubscriptionManager mSubscriptionManager;
    private UserManager mUserManager;
    private static final int PROJECT_SIM_NUM = TelephonyManager.getDefault().getPhoneCount();
    private static Context mContext = null;
    private static IccFileHandler[] sFh = new IccFileHandler[PROJECT_SIM_NUM];
    protected static SubscriptionInfoUpdater sSubInfoUpdater = null;
    private static String[] mIccId = new String[PROJECT_SIM_NUM];
    private static int[] mInsertSimState = new int[PROJECT_SIM_NUM];
    private static int[] sIsUpdateAvailable = new int[PROJECT_SIM_NUM];
    private static final String[] PROPERTY_ICCID_SIM = {"ril.iccid.sim1", "ril.iccid.sim2", "ril.iccid.sim3", "ril.iccid.sim4"};
    private static final boolean MTK_FLIGHTMODE_POWEROFF_MD_SUPPORT = "1".equals(SystemProperties.get("ro.mtk_flight_mode_power_off_md"));
    private Map<Integer, Intent> rebroadcastIntentsOnUnlock = new HashMap();
    protected AtomicReferenceArray<IccRecords> mIccRecords = new AtomicReferenceArray<>(PROJECT_SIM_NUM);
    private int mReadIccIdCount = 0;
    protected final Object mLock = new Object();
    private boolean mCommonSlotResetDone = false;
    private final BroadcastReceiver sReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            SubscriptionInfoUpdater.this.logd("[Receiver]+");
            String action = intent.getAction();
            SubscriptionInfoUpdater.this.logd("Action: " + action);
            if (action.equals("android.intent.action.USER_UNLOCKED")) {
                Iterator iterator = SubscriptionInfoUpdater.this.rebroadcastIntentsOnUnlock.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry pair = (Map.Entry) iterator.next();
                    Intent i = (Intent) pair.getValue();
                    iterator.remove();
                    SubscriptionInfoUpdater.this.logd("Broadcasting intent ACTION_SIM_STATE_CHANGED for mCardIndex: " + pair.getKey());
                    ActivityManagerNative.broadcastStickyIntent(i, "android.permission.READ_PHONE_STATE", -1);
                }
                SubscriptionInfoUpdater.this.rebroadcastIntentsOnUnlock = null;
                SubscriptionInfoUpdater.this.logd("[Receiver]-");
                return;
            }
            if (!action.equals("android.intent.action.SIM_STATE_CHANGED") && !action.equals(IccCardProxy.ACTION_INTERNAL_SIM_STATE_CHANGED) && !action.equals(IWorldPhone.ACTION_SHUTDOWN_IPO) && !action.equals("com.mediatek.phone.ACTION_COMMON_SLOT_NO_CHANGED") && !action.equals("android.intent.action.LOCALE_CHANGED")) {
                return;
            }
            int slotId = intent.getIntExtra("phone", -1);
            SubscriptionInfoUpdater.this.logd("slotId: " + slotId);
            if (slotId == -1 && (action.equals("android.intent.action.SIM_STATE_CHANGED") || action.equals(IccCardProxy.ACTION_INTERNAL_SIM_STATE_CHANGED))) {
                return;
            }
            String simStatus = intent.getStringExtra("ss");
            SubscriptionInfoUpdater.this.logd("simStatus: " + simStatus);
            if (action.equals("android.intent.action.SIM_STATE_CHANGED")) {
                if ("ABSENT".equals(simStatus)) {
                    SubscriptionInfoUpdater.this.sendMessage(SubscriptionInfoUpdater.this.obtainMessage(4, slotId, -1));
                } else if ("UNKNOWN".equals(simStatus)) {
                    SubscriptionInfoUpdater.this.sendMessage(SubscriptionInfoUpdater.this.obtainMessage(7, slotId, -1));
                } else if ("CARD_IO_ERROR".equals(simStatus)) {
                    SubscriptionInfoUpdater.this.sendMessage(SubscriptionInfoUpdater.this.obtainMessage(6, slotId, -1));
                } else if ("READY".equals(simStatus)) {
                    SubscriptionInfoUpdater.this.sendMessage(SubscriptionInfoUpdater.this.obtainMessage(100, slotId, -1));
                } else {
                    SubscriptionInfoUpdater.this.logd("Ignoring simStatus: " + simStatus);
                }
            } else if (action.equals(IccCardProxy.ACTION_INTERNAL_SIM_STATE_CHANGED)) {
                if ("LOCKED".equals(simStatus)) {
                    String reason = intent.getStringExtra("reason");
                    SubscriptionInfoUpdater.this.sendMessage(SubscriptionInfoUpdater.this.obtainMessage(5, slotId, -1, reason));
                } else if ("LOADED".equals(simStatus)) {
                    SubscriptionInfoUpdater.this.sendMessage(SubscriptionInfoUpdater.this.obtainMessage(3, slotId, -1));
                    SubscriptionInfoUpdater.this.mReadIccIdCount = 10;
                } else {
                    SubscriptionInfoUpdater.this.logd("Ignoring simStatus: " + simStatus);
                }
            } else if (action.equals(IWorldPhone.ACTION_SHUTDOWN_IPO)) {
                for (int i2 = 0; i2 < SubscriptionInfoUpdater.PROJECT_SIM_NUM; i2++) {
                    SubscriptionInfoUpdater.this.clearIccId(i2);
                }
                SubscriptionInfoUpdater.this.mSubscriptionManager.clearSubscriptionInfo();
                SubscriptionController.getInstance().removeStickyIntent();
            } else if (action.equals("android.intent.action.LOCALE_CHANGED")) {
                int[] subIdList = SubscriptionInfoUpdater.this.mSubscriptionManager.getActiveSubscriptionIdList();
                for (int subId : subIdList) {
                    SubscriptionInfoUpdater.this.updateSubName(subId);
                }
            } else if (action.equals("com.mediatek.phone.ACTION_COMMON_SLOT_NO_CHANGED")) {
                int slotId2 = intent.getIntExtra("phone", -1);
                SubscriptionInfoUpdater.this.logd("[Common Slot] NO_CHANTED, slotId: " + slotId2);
                SubscriptionInfoUpdater.this.sendMessage(SubscriptionInfoUpdater.this.obtainMessage(SubscriptionInfoUpdater.EVENT_SIM_NO_CHANGED, slotId2, -1));
            }
            SubscriptionInfoUpdater.this.logd("[Receiver]-");
        }
    };
    private Runnable mReadIccIdPropertyRunnable = new Runnable() {
        @Override
        public void run() {
            SubscriptionInfoUpdater.this.mReadIccIdCount++;
            if (SubscriptionInfoUpdater.this.mReadIccIdCount > 10) {
                return;
            }
            if (!SubscriptionInfoUpdater.this.checkAllIccIdReady()) {
                SubscriptionInfoUpdater.this.postDelayed(SubscriptionInfoUpdater.this.mReadIccIdPropertyRunnable, 1000L);
            } else {
                SubscriptionInfoUpdater.this.updateSubscriptionInfoIfNeed();
            }
        }
    };

    public SubscriptionInfoUpdater(Context context, Phone[] phone, CommandsInterface[] ci) {
        this.mCis = null;
        this.mSubscriptionManager = null;
        logd("Constructor invoked");
        mContext = context;
        mPhone = phone;
        this.mSubscriptionManager = SubscriptionManager.from(mContext);
        this.mPackageManager = IPackageManager.Stub.asInterface(ServiceManager.getService(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME));
        this.mUserManager = (UserManager) mContext.getSystemService("user");
        sSubInfoUpdater = this;
        this.mCis = ci;
        for (int i = 0; i < PROJECT_SIM_NUM; i++) {
            sIsUpdateAvailable[i] = 0;
            mIccId[i] = SystemProperties.get(PROPERTY_ICCID_SIM[i], UsimPBMemInfo.STRING_NOT_SET);
            if (mIccId[i].length() == 3) {
                logd("No SIM insert :" + i);
            }
            logd("mIccId[" + i + "]:" + SubscriptionInfo.givePrintableIccid(mIccId[i]));
        }
        if (isAllIccIdQueryDone()) {
            new Thread() {
                @Override
                public void run() {
                    SubscriptionInfoUpdater.this.updateSubscriptionInfoByIccId();
                }
            }.start();
        }
        IntentFilter intentFilter = new IntentFilter("android.intent.action.SIM_STATE_CHANGED");
        intentFilter.addAction(IccCardProxy.ACTION_INTERNAL_SIM_STATE_CHANGED);
        intentFilter.addAction(IWorldPhone.ACTION_SHUTDOWN_IPO);
        intentFilter.addAction("com.mediatek.phone.ACTION_COMMON_SLOT_NO_CHANGED");
        if ("OP09".equals(SystemProperties.get("persist.operator.optr")) && ("SEGDEFAULT".equals(SystemProperties.get("persist.operator.seg")) || "SEGC".equals(SystemProperties.get("persist.operator.seg")))) {
            intentFilter.addAction("android.intent.action.LOCALE_CHANGED");
        }
        for (int i2 = 0; i2 < this.mCis.length; i2++) {
            Integer index = new Integer(i2);
            this.mCis[i2].registerForNotAvailable(this, 102, index);
            this.mCis[i2].registerForAvailable(this, 101, index);
            if (SystemProperties.get(COMMON_SLOT_PROPERTY).equals("1")) {
                this.mCis[i2].registerForTrayPlugIn(this, 104, index);
                this.mCis[i2].registerForSimPlugOut(this, 105, index);
            }
        }
        intentFilter.addAction("android.intent.action.USER_UNLOCKED");
        mContext.registerReceiver(this.sReceiver, intentFilter);
        this.mCarrierServiceBindHelper = new CarrierServiceBindHelper(mContext);
        initializeCarrierApps();
    }

    private void initializeCarrierApps() {
        this.mCurrentlyActiveUserId = 0;
        try {
            ActivityManagerNative.getDefault().registerUserSwitchObserver(new IUserSwitchObserver.Stub() {
                public void onUserSwitching(int newUserId, IRemoteCallback reply) throws RemoteException {
                    SubscriptionInfoUpdater.this.mCurrentlyActiveUserId = newUserId;
                    CarrierAppUtils.disableCarrierAppsUntilPrivileged(SubscriptionInfoUpdater.mContext.getOpPackageName(), SubscriptionInfoUpdater.this.mPackageManager, TelephonyManager.getDefault(), SubscriptionInfoUpdater.this.mCurrentlyActiveUserId);
                    if (reply == null) {
                        return;
                    }
                    try {
                        reply.sendResult((Bundle) null);
                    } catch (RemoteException e) {
                    }
                }

                public void onUserSwitchComplete(int newUserId) {
                }

                public void onForegroundProfileSwitch(int newProfileId) throws RemoteException {
                }
            });
            this.mCurrentlyActiveUserId = ActivityManagerNative.getDefault().getCurrentUser().id;
        } catch (RemoteException e) {
            logd("Couldn't get current user ID; guessing it's 0: " + e.getMessage());
        }
        CarrierAppUtils.disableCarrierAppsUntilPrivileged(mContext.getOpPackageName(), this.mPackageManager, TelephonyManager.getDefault(), this.mCurrentlyActiveUserId);
    }

    private boolean isAllIccIdQueryDone() {
        for (int i = 0; i < PROJECT_SIM_NUM; i++) {
            if (mIccId[i] == null || mIccId[i].equals(UsimPBMemInfo.STRING_NOT_SET)) {
                logd("Wait for SIM" + (i + 1) + " IccId");
                return false;
            }
        }
        logd("All IccIds query complete");
        return true;
    }

    public void setDisplayNameForNewSub(String newSubName, int subId, int newNameSource) {
        SubscriptionInfo subInfo = this.mSubscriptionManager.getActiveSubscriptionInfo(subId);
        if (subInfo == null) {
            logd("SUB" + (subId + 1) + " SubInfo not created yet");
            return;
        }
        int oldNameSource = subInfo.getNameSource();
        CharSequence oldSubName = subInfo.getDisplayName();
        logd("[setDisplayNameForNewSub] subId = " + subInfo.getSubscriptionId() + ", oldSimName = " + oldSubName + ", oldNameSource = " + oldNameSource + ", newSubName = " + newSubName + ", newNameSource = " + newNameSource);
        if (oldSubName == null || ((oldNameSource == 0 && newSubName != null) || !(oldNameSource != 1 || newSubName == null || newSubName.equals(oldSubName)))) {
            this.mSubscriptionManager.setDisplayName(newSubName, subInfo.getSubscriptionId(), newNameSource);
        }
    }

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case 1:
                AsyncResult ar = (AsyncResult) msg.obj;
                QueryIccIdUserObj uObj = (QueryIccIdUserObj) ar.userObj;
                int slotId = uObj.slotId;
                logd("handleMessage : <EVENT_SIM_LOCKED_QUERY_ICCID_DONE> SIM" + (slotId + 1));
                if (ar.exception == null) {
                    if (ar.result != null) {
                        byte[] data = (byte[]) ar.result;
                        mIccId[slotId] = IccUtils.parseIccIdToString(data, 0, data.length);
                    } else {
                        logd("Null ar");
                        mIccId[slotId] = ICCID_STRING_FOR_NO_SIM;
                    }
                } else {
                    if ((ar.exception instanceof CommandException) && ((CommandException) ar.exception).getCommandError() == CommandException.Error.RADIO_NOT_AVAILABLE) {
                        mIccId[slotId] = UsimPBMemInfo.STRING_NOT_SET;
                    } else {
                        mIccId[slotId] = ICCID_STRING_FOR_NO_SIM;
                    }
                    logd("Query IccId fail: " + ar.exception);
                }
                logd("sIccId[" + slotId + "] = " + mIccId[slotId]);
                if (isAllIccIdQueryDone()) {
                    updateSubscriptionInfoByIccId();
                }
                broadcastSimStateChanged(slotId, "LOCKED", uObj.reason);
                if (!ICCID_STRING_FOR_NO_SIM.equals(mIccId[slotId])) {
                    updateCarrierServices(slotId, "LOCKED");
                }
                SubscriptionUpdatorThread updatorThread = new SubscriptionUpdatorThread(new QueryIccIdUserObj(uObj.reason, slotId), 2);
                updatorThread.start();
                break;
            case 2:
                AsyncResult ar2 = (AsyncResult) msg.obj;
                Integer slotId2 = (Integer) ar2.userObj;
                if (ar2.exception == null && ar2.result != null) {
                    int[] modes = (int[]) ar2.result;
                    if (modes[0] == 1) {
                        mPhone[slotId2.intValue()].setNetworkSelectionModeAutomatic(null);
                    }
                } else {
                    logd("EVENT_GET_NETWORK_SELECTION_MODE_DONE: error getting network mode.");
                }
                break;
            case 3:
                SubscriptionUpdatorThread updatorThread2 = new SubscriptionUpdatorThread(new QueryIccIdUserObj(null, msg.arg1), 1);
                updatorThread2.start();
                break;
            case 4:
                SubscriptionUpdatorThread updatorThread3 = new SubscriptionUpdatorThread(new QueryIccIdUserObj(null, msg.arg1), 0);
                updatorThread3.start();
                break;
            case 5:
                handleSimLocked(msg.arg1, (String) msg.obj);
                break;
            case 6:
                updateCarrierServices(msg.arg1, "CARD_IO_ERROR");
                break;
            case 7:
                updateCarrierServices(msg.arg1, "UNKNOWN");
                break;
            case 100:
                SubscriptionUpdatorThread updatorThread4 = new SubscriptionUpdatorThread(new QueryIccIdUserObj(null, msg.arg1), 3);
                updatorThread4.start();
                break;
            case 101:
                Integer index = getCiIndex(msg);
                logd("handleMessage : <EVENT_RADIO_AVAILABLE> SIM" + (index.intValue() + 1));
                sIsUpdateAvailable[index.intValue()] = 1;
                if (checkIsAvailable()) {
                    this.mReadIccIdCount = 0;
                    if (!checkAllIccIdReady()) {
                        postDelayed(this.mReadIccIdPropertyRunnable, 1000L);
                    } else {
                        updateSubscriptionInfoIfNeed();
                    }
                }
                break;
            case 102:
                Integer index2 = getCiIndex(msg);
                logd("handleMessage : <EVENT_RADIO_UNAVAILABLE> SIM" + (index2.intValue() + 1));
                sIsUpdateAvailable[index2.intValue()] = 0;
                if (SystemProperties.get(COMMON_SLOT_PROPERTY).equals("1")) {
                    logd("[Common slot] reset mCommonSlotResetDone in EVENT_RADIO_UNAVAILABLE");
                    this.mCommonSlotResetDone = false;
                }
                break;
            case EVENT_SIM_NO_CHANGED:
                SubscriptionUpdatorThread updatorThread5 = new SubscriptionUpdatorThread(new QueryIccIdUserObj(null, msg.arg1), 4);
                updatorThread5.start();
                break;
            case 104:
                logd("[Common Slot] handle EVENT_TRAY_PLUG_IN " + this.mCommonSlotResetDone);
                if (!this.mCommonSlotResetDone) {
                    this.mCommonSlotResetDone = true;
                    for (int i = 0; i < PROJECT_SIM_NUM; i++) {
                        TelephonyManager.getDefault();
                        String vsimEnabled = TelephonyManager.getTelephonyProperty(i, "gsm.external.sim.enabled", "0");
                        if (vsimEnabled.length() == 0) {
                            vsimEnabled = "0";
                        }
                        logd("vsimEnabled[" + i + "]: (" + vsimEnabled + ")");
                        try {
                            if ("0".equals(vsimEnabled)) {
                                logd("[Common Slot] reset mIccId[" + i + "] to empty.");
                                mIccId[i] = UsimPBMemInfo.STRING_NOT_SET;
                            }
                        } catch (NumberFormatException e) {
                            logd("[Common Slot] NumberFormatException, reset mIccId[" + i + "] to empty.");
                            mIccId[i] = UsimPBMemInfo.STRING_NOT_SET;
                        }
                    }
                }
                break;
            case 105:
                logd("[Common Slot] handle EVENT_SIM_PLUG_OUT " + this.mCommonSlotResetDone);
                this.mCommonSlotResetDone = false;
                break;
            default:
                logd("Unknown msg:" + msg.what);
                break;
        }
    }

    private static class QueryIccIdUserObj {
        public String reason;
        public int slotId;

        QueryIccIdUserObj(String reason, int slotId) {
            this.reason = reason;
            this.slotId = slotId;
        }
    }

    private class SubscriptionUpdatorThread extends Thread {
        public static final int SIM_ABSENT = 0;
        public static final int SIM_LOADED = 1;
        public static final int SIM_LOCKED = 2;
        public static final int SIM_NO_CHANGED = 4;
        public static final int SIM_READY = 3;
        private int mEventId;
        private QueryIccIdUserObj mUserObj;

        SubscriptionUpdatorThread(QueryIccIdUserObj userObj, int eventId) {
            this.mUserObj = userObj;
            this.mEventId = eventId;
        }

        @Override
        public void run() {
            switch (this.mEventId) {
                case 0:
                    SubscriptionInfoUpdater.this.handleSimAbsent(this.mUserObj.slotId);
                    break;
                case 1:
                    SubscriptionInfoUpdater.this.handleSimLoaded(this.mUserObj.slotId);
                    break;
                case 2:
                    if (SubscriptionInfoUpdater.this.isAllIccIdQueryDone()) {
                        SubscriptionInfoUpdater.this.updateSubscriptionInfoByIccId();
                    }
                    SubscriptionInfoUpdater.this.broadcastSimStateChanged(this.mUserObj.slotId, "LOCKED", this.mUserObj.reason);
                    break;
                case 3:
                    if (SubscriptionInfoUpdater.this.checkAllIccIdReady()) {
                        SubscriptionInfoUpdater.this.updateSubscriptionInfoIfNeed();
                    }
                    break;
                case 4:
                    SubscriptionInfoUpdater.this.logd("[Common Slot]SubscriptionUpdatorThread run for SIM_NO_CHANGED.");
                    if (SubscriptionInfoUpdater.this.checkAllIccIdReady()) {
                        SubscriptionInfoUpdater.this.updateSubscriptionInfoIfNeed();
                    } else {
                        SubscriptionInfoUpdater.mIccId[this.mUserObj.slotId] = SubscriptionInfoUpdater.ICCID_STRING_FOR_NO_SIM;
                        SubscriptionInfoUpdater.this.logd("case SIM_NO_CHANGED: set N/A for slot" + this.mUserObj.slotId);
                        SubscriptionInfoUpdater.this.mReadIccIdCount = 0;
                        SubscriptionInfoUpdater.this.postDelayed(SubscriptionInfoUpdater.this.mReadIccIdPropertyRunnable, 1000L);
                    }
                    break;
                default:
                    SubscriptionInfoUpdater.this.logd("SubscriptionUpdatorThread run with invalid event id.");
                    break;
            }
        }
    }

    private void handleSimLocked(int slotId, String reason) {
        synchronized (this.mLock) {
            if (mIccId[slotId] != null && mIccId[slotId].equals(ICCID_STRING_FOR_NO_SIM)) {
                logd("SIM" + (slotId + 1) + " hot plug in");
                mIccId[slotId] = null;
            }
            IccFileHandler fileHandler = mPhone[slotId].getIccCard() != null ? mPhone[slotId].getIccCard().getIccFileHandler() : null;
            if (fileHandler != null) {
                String iccId = mIccId[slotId];
                if (iccId == null || iccId.equals(UsimPBMemInfo.STRING_NOT_SET)) {
                    mIccId[slotId] = SystemProperties.get(PROPERTY_ICCID_SIM[slotId], UsimPBMemInfo.STRING_NOT_SET);
                    if (mIccId[slotId] != null && !mIccId[slotId].equals(UsimPBMemInfo.STRING_NOT_SET)) {
                        logd("Use Icc ID system property for performance enhancement");
                        SubscriptionUpdatorThread updatorThread = new SubscriptionUpdatorThread(new QueryIccIdUserObj(reason, slotId), 2);
                        updatorThread.start();
                    } else {
                        logd("Querying IccId");
                        fileHandler.loadEFTransparent(IccConstants.EF_ICCID, obtainMessage(1, new QueryIccIdUserObj(reason, slotId)));
                    }
                } else {
                    logd("NOT Querying IccId its already set sIccid[" + slotId + "]=" + SubscriptionInfo.givePrintableIccid(iccId));
                    String tempIccid = SystemProperties.get(PROPERTY_ICCID_SIM[slotId], UsimPBMemInfo.STRING_NOT_SET);
                    logd("tempIccid:" + SubscriptionInfo.givePrintableIccid(tempIccid) + ", mIccId[slotId]:" + SubscriptionInfo.givePrintableIccid(mIccId[slotId]));
                    if (MTK_FLIGHTMODE_POWEROFF_MD_SUPPORT && !checkAllIccIdReady() && !tempIccid.equals(mIccId[slotId])) {
                        logd("All iccids are not ready and iccid changed");
                        mIccId[slotId] = null;
                        this.mSubscriptionManager.clearSubscriptionInfo();
                    }
                    updateCarrierServices(slotId, "LOCKED");
                    broadcastSimStateChanged(slotId, "LOCKED", reason);
                }
            } else {
                logd("sFh[" + slotId + "] is null, ignore");
            }
        }
    }

    private void handleSimLoaded(int slotId) {
        String nameToSet;
        logd("handleSimStateLoadedInternal: slotId: " + slotId);
        boolean needUpdate = false;
        IccRecords records = mPhone[slotId].getIccCard().getIccRecords();
        if (records == null) {
            logd("onRecieve: IccRecords null");
            return;
        }
        if (records.getIccId() == null) {
            logd("onRecieve: IccID null");
            return;
        }
        String iccId = SystemProperties.get(PROPERTY_ICCID_SIM[slotId], UsimPBMemInfo.STRING_NOT_SET);
        if (!iccId.equals(mIccId[slotId])) {
            logd("NeedUpdate");
            needUpdate = true;
            mIccId[slotId] = iccId;
        }
        if (isAllIccIdQueryDone() && needUpdate) {
            updateSubscriptionInfoByIccId();
        }
        int subId = Integer.MAX_VALUE;
        int[] subIds = SubscriptionController.getInstance().getSubId(slotId);
        if (subIds != null) {
            subId = subIds[0];
        }
        if (SubscriptionManager.isValidSubscriptionId(subId)) {
            TelephonyManager tm = TelephonyManager.from(mContext);
            String operator = tm.getSimOperatorNumericForPhone(slotId);
            if (!TextUtils.isEmpty(operator)) {
                if (subId == SubscriptionController.getInstance().getDefaultSubId()) {
                    MccTable.updateMccMncConfiguration(mContext, operator, false);
                }
                SubscriptionController.getInstance().setMccMnc(operator, subId);
            } else {
                logd("EVENT_RECORDS_LOADED Operator name is null");
            }
            String msisdn = tm.getLine1Number(subId);
            mContext.getContentResolver();
            if (msisdn != null) {
                SubscriptionController.getInstance().setDisplayNumber(msisdn, subId);
            }
            SubscriptionInfo subInfo = this.mSubscriptionManager.getActiveSubscriptionInfo(subId);
            String simCarrierName = tm.getSimOperatorName(subId);
            new ContentValues(1);
            if (subInfo != null && subInfo.getNameSource() != 2) {
                String simNumeric = tm.getSimOperatorNumeric(subId);
                String simMvnoName = SpnOverride.getInstance().lookupOperatorNameForDisplayName(subId, simNumeric, true, mContext);
                logd("[handleSimLoaded]- simNumeric: " + simNumeric + ", simMvnoName: " + simMvnoName);
                if (!TextUtils.isEmpty(simMvnoName)) {
                    nameToSet = simMvnoName;
                } else if (!TextUtils.isEmpty(simCarrierName)) {
                    nameToSet = simCarrierName;
                } else {
                    nameToSet = "CARD " + Integer.toString(slotId + 1);
                }
                this.mSubscriptionManager.setDisplayName(nameToSet, subId);
                logd("[handleSimLoaded] subId = " + subId + ", sim name = " + nameToSet);
            }
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(mContext);
            int storedSubId = sp.getInt(CURR_SUBID + slotId, -1);
            if (storedSubId != subId) {
                int networkType = Settings.Global.getInt(mPhone[slotId].getContext().getContentResolver(), "preferred_network_mode" + subId, RILConstants.PREFERRED_NETWORK_MODE);
                logd("Possibly a new IMSI. Set sub(" + subId + ") networkType to " + networkType);
                Rlog.d(LOG_TAG, "check persist.radio.lte.chip : " + SystemProperties.get("persist.radio.lte.chip"));
                if (SystemProperties.get("persist.radio.lte.chip").equals(Phone.ACT_TYPE_UTRAN) && (networkType == 8 || networkType == 9 || networkType == 10 || networkType == 11 || networkType == 12 || networkType == 15 || networkType == 17 || networkType == 19 || networkType == 20 || networkType == 22 || networkType == 30 || networkType == 31)) {
                    if (SystemProperties.get("ro.boot.opt_c2k_support").equals("1")) {
                        networkType = 7;
                    } else {
                        networkType = 0;
                    }
                    logd("Chip limit access 4G,modify PREFERRED_NETWORK_MODE init value to " + networkType + ",subId = " + subId);
                }
                Settings.Global.putInt(mPhone[slotId].getContext().getContentResolver(), "preferred_network_mode" + subId, networkType);
                mPhone[slotId].getNetworkSelectionMode(obtainMessage(2, new Integer(slotId)));
                SharedPreferences.Editor editor = sp.edit();
                editor.putInt(CURR_SUBID + slotId, subId);
                editor.apply();
            }
        } else {
            logd("Invalid subId, could not update ContentResolver, send sim loaded.");
            SubscriptionController subCtrl = SubscriptionController.getInstance();
            if (subCtrl != null) {
                if (!subCtrl.isReady()) {
                    if (sSubInfoUpdater != null) {
                        SystemClock.sleep(100L);
                        sSubInfoUpdater.sendMessage(obtainMessage(3, slotId, -1));
                    } else {
                        logd("sSubInfoUpdater is null");
                    }
                }
            } else {
                logd("subCtrl is null");
            }
        }
        CarrierAppUtils.disableCarrierAppsUntilPrivileged(mContext.getOpPackageName(), this.mPackageManager, TelephonyManager.getDefault(), this.mCurrentlyActiveUserId);
        broadcastSimStateChanged(slotId, "LOADED", null);
        updateCarrierServices(slotId, "LOADED");
    }

    private void updateCarrierServices(int slotId, String simState) {
        CarrierConfigManager configManager = (CarrierConfigManager) mContext.getSystemService("carrier_config");
        configManager.updateConfigForPhoneId(slotId, simState);
        this.mCarrierServiceBindHelper.updateForPhoneId(slotId, simState);
    }

    private void handleSimAbsent(int slotId) {
        if (mIccId[slotId] != null && !mIccId[slotId].equals(ICCID_STRING_FOR_NO_SIM)) {
            logd("SIM" + (slotId + 1) + " hot plug out");
        }
        if (mIccId[slotId] != null && mIccId[slotId].equals(ICCID_STRING_FOR_NO_SIM)) {
            logd("SIM" + (slotId + 1) + " absent - card state no changed.");
            updateCarrierServices(slotId, "ABSENT");
            return;
        }
        if (SystemProperties.get(COMMON_SLOT_PROPERTY).equals("1")) {
            if (checkAllIccIdReady()) {
                updateSubscriptionInfoIfNeed();
            }
        } else {
            mIccId[slotId] = ICCID_STRING_FOR_NO_SIM;
            if (isAllIccIdQueryDone()) {
                updateSubscriptionInfoByIccId();
            }
        }
        updateCarrierServices(slotId, "ABSENT");
    }

    private synchronized void updateSubscriptionInfoByIccId() {
        synchronized (this.mLock) {
            logd("updateSubscriptionInfoByIccId:+ Start");
            if (isAllIccIdQueryDone()) {
                this.mCommonSlotResetDone = false;
                boolean skipCapabilitySwitch = false;
                for (int i = 0; i < PROJECT_SIM_NUM; i++) {
                    mInsertSimState[i] = 0;
                    int simState = TelephonyManager.from(mContext).getSimState(i);
                    if (simState == 2 || simState == 3 || simState == 4 || simState == 6) {
                        logd("skipCapabilitySwitch = " + skipCapabilitySwitch);
                        skipCapabilitySwitch = true;
                    }
                }
                int insertedSimCount = PROJECT_SIM_NUM;
                for (int i2 = 0; i2 < PROJECT_SIM_NUM; i2++) {
                    if (ICCID_STRING_FOR_NO_SIM.equals(mIccId[i2])) {
                        insertedSimCount--;
                        mInsertSimState[i2] = -99;
                    }
                }
                logd("insertedSimCount = " + insertedSimCount);
                for (int i3 = 0; i3 < PROJECT_SIM_NUM; i3++) {
                    if (mInsertSimState[i3] != -99) {
                        int index = 2;
                        for (int j = i3 + 1; j < PROJECT_SIM_NUM; j++) {
                            if (mInsertSimState[j] == 0 && mIccId[i3].equals(mIccId[j])) {
                                mInsertSimState[i3] = 1;
                                mInsertSimState[j] = index;
                                index++;
                            }
                        }
                    }
                }
                ContentResolver contentResolver = mContext.getContentResolver();
                String[] oldIccId = new String[PROJECT_SIM_NUM];
                for (int i4 = 0; i4 < PROJECT_SIM_NUM; i4++) {
                    oldIccId[i4] = null;
                    List<SubscriptionInfo> oldSubInfo = SubscriptionController.getInstance().getSubInfoUsingSlotIdWithCheck(i4, false, mContext.getOpPackageName());
                    if (oldSubInfo != null) {
                        oldIccId[i4] = oldSubInfo.get(0).getIccId();
                        logd("updateSubscriptionInfoByIccId: oldSubId = " + oldSubInfo.get(0).getSubscriptionId());
                        if (mInsertSimState[i4] == 0 && !mIccId[i4].equals(oldIccId[i4])) {
                            mInsertSimState[i4] = -1;
                        }
                        if (mInsertSimState[i4] != 0) {
                            SubscriptionController.getInstance().clearSubInfoUsingPhoneId(i4);
                            logd("updateSubscriptionInfoByIccId: clearSubInfoUsingPhoneId phoneId = " + i4);
                            ContentValues value = new ContentValues(1);
                            value.put("sim_id", (Integer) (-1));
                            contentResolver.update(SubscriptionManager.CONTENT_URI, value, "_id=" + Integer.toString(oldSubInfo.get(0).getSubscriptionId()), null);
                        }
                    } else {
                        if (mInsertSimState[i4] == 0) {
                            mInsertSimState[i4] = -1;
                        }
                        SubscriptionController.getInstance().clearSubInfoUsingPhoneId(i4);
                        logd("updateSubscriptionInfoByIccId: clearSubInfoUsingPhoneId phoneId = " + i4);
                        oldIccId[i4] = ICCID_STRING_FOR_NO_SIM;
                        logd("updateSubscriptionInfoByIccId: No SIM in slot " + i4 + " last time");
                    }
                }
                for (int i5 = 0; i5 < PROJECT_SIM_NUM; i5++) {
                    logd("updateSubscriptionInfoByIccId: oldIccId[" + i5 + "] = " + SubscriptionInfo.givePrintableIccid(oldIccId[i5]) + ", sIccId[" + i5 + "] = " + SubscriptionInfo.givePrintableIccid(mIccId[i5]));
                }
                int nNewCardCount = 0;
                int nNewSimStatus = 0;
                for (int i6 = 0; i6 < PROJECT_SIM_NUM; i6++) {
                    if (mInsertSimState[i6] == -99) {
                        logd("updateSubscriptionInfoByIccId: No SIM inserted in slot " + i6 + " this time");
                    } else {
                        if (mInsertSimState[i6] > 0) {
                            this.mSubscriptionManager.addSubscriptionInfoRecord(mIccId[i6] + Integer.toString(mInsertSimState[i6]), i6);
                            logd("SUB" + (i6 + 1) + " has invalid IccId");
                        } else {
                            this.mSubscriptionManager.addSubscriptionInfoRecord(mIccId[i6], i6);
                        }
                        if (isNewSim(mIccId[i6], oldIccId)) {
                            nNewCardCount++;
                            switch (i6) {
                                case 0:
                                    nNewSimStatus |= 1;
                                    break;
                                case 1:
                                    nNewSimStatus |= 2;
                                    break;
                                case 2:
                                    nNewSimStatus |= 4;
                                    break;
                            }
                            mInsertSimState[i6] = -2;
                        }
                    }
                }
                for (int i7 = 0; i7 < PROJECT_SIM_NUM; i7++) {
                    if (mInsertSimState[i7] == -1) {
                        mInsertSimState[i7] = -3;
                    }
                    logd("updateSubscriptionInfoByIccId: sInsertSimState[" + i7 + "] = " + mInsertSimState[i7]);
                }
                List<SubscriptionInfo> subInfos = this.mSubscriptionManager.getActiveSubscriptionInfoList();
                int nSubCount = subInfos == null ? 0 : subInfos.size();
                logd("updateSubscriptionInfoByIccId: nSubCount = " + nSubCount);
                for (int i8 = 0; i8 < nSubCount; i8++) {
                    SubscriptionInfo temp = subInfos.get(i8);
                    String msisdn = TelephonyManager.from(mContext).getLine1Number(temp.getSubscriptionId());
                    if (msisdn != null) {
                        SubscriptionController.getInstance().setDisplayNumber(msisdn, temp.getSubscriptionId());
                    }
                }
                setAllDefaultSub(subInfos);
                boolean hasSimRemoved = false;
                int i9 = 0;
                while (true) {
                    if (i9 < PROJECT_SIM_NUM) {
                        if (mIccId[i9] == null || !mIccId[i9].equals(ICCID_STRING_FOR_NO_SIM) || oldIccId[i9].equals(ICCID_STRING_FOR_NO_SIM)) {
                            i9++;
                        } else {
                            hasSimRemoved = true;
                        }
                    }
                }
                Intent intent = null;
                if (nNewCardCount != 0) {
                    logd("New SIM detected");
                    intent = setUpdatedData(1, nSubCount, nNewSimStatus);
                } else if (hasSimRemoved) {
                    int i10 = 0;
                    while (true) {
                        if (i10 < PROJECT_SIM_NUM) {
                            if (mInsertSimState[i10] == -3) {
                                logd("No new SIM detected and SIM repositioned");
                                intent = setUpdatedData(3, nSubCount, nNewSimStatus);
                            } else {
                                i10++;
                            }
                        }
                    }
                    if (i10 == PROJECT_SIM_NUM) {
                        logd("No new SIM detected and SIM removed");
                        intent = setUpdatedData(2, nSubCount, nNewSimStatus);
                    }
                } else {
                    int i11 = 0;
                    while (true) {
                        if (i11 < PROJECT_SIM_NUM) {
                            if (mInsertSimState[i11] == -3) {
                                logd("No new SIM detected and SIM repositioned");
                                intent = setUpdatedData(3, nSubCount, nNewSimStatus);
                            } else {
                                i11++;
                            }
                        }
                    }
                    if (i11 == PROJECT_SIM_NUM) {
                        logd("[updateSimInfoByIccId] All SIM inserted into the same slot");
                        intent = setUpdatedData(4, nSubCount, nNewSimStatus);
                    }
                }
                if (PROJECT_SIM_NUM > 1) {
                    if (skipCapabilitySwitch) {
                        SubscriptionManager subscriptionManager = this.mSubscriptionManager;
                        SubscriptionManager subscriptionManager2 = this.mSubscriptionManager;
                        subscriptionManager.setDefaultDataSubIdWithoutCapabilitySwitch(SubscriptionManager.getDefaultDataSubscriptionId());
                    } else {
                        SubscriptionManager subscriptionManager3 = this.mSubscriptionManager;
                        SubscriptionManager subscriptionManager4 = this.mSubscriptionManager;
                        subscriptionManager3.setDefaultDataSubId(SubscriptionManager.getDefaultDataSubscriptionId());
                    }
                }
                SubscriptionController.getInstance().notifySubscriptionInfoChanged(intent);
                logd("updateSubscriptionInfoByIccId:- SsubscriptionInfo update complete");
            }
        }
    }

    private Intent setUpdatedData(int detectedType, int subCount, int newSimStatus) {
        Intent intent = new Intent("android.intent.action.ACTION_SUBINFO_RECORD_UPDATED");
        logd("[setUpdatedData]+ ");
        if (detectedType == 1) {
            intent.putExtra(CatService.AnonymousClass4.INTENT_KEY_DETECT_STATUS, 1);
            intent.putExtra("simCount", subCount);
            intent.putExtra("newSIMSlot", newSimStatus);
        } else if (detectedType == 3) {
            intent.putExtra(CatService.AnonymousClass4.INTENT_KEY_DETECT_STATUS, 3);
            intent.putExtra("simCount", subCount);
        } else if (detectedType == 2) {
            intent.putExtra(CatService.AnonymousClass4.INTENT_KEY_DETECT_STATUS, 2);
            intent.putExtra("simCount", subCount);
        } else if (detectedType == 4) {
            intent.putExtra(CatService.AnonymousClass4.INTENT_KEY_DETECT_STATUS, 4);
        }
        logd("[setUpdatedData]- [" + detectedType + ", " + subCount + ", " + newSimStatus + "]");
        return intent;
    }

    private boolean isNewSim(String iccId, String[] oldIccId) {
        boolean newSim = true;
        int i = 0;
        while (true) {
            if (i < PROJECT_SIM_NUM) {
                if (iccId == null || oldIccId[i] == null || oldIccId[i].indexOf(iccId) != 0) {
                    i++;
                } else {
                    newSim = false;
                    break;
                }
            } else {
                break;
            }
        }
        logd("newSim = " + newSim);
        return newSim;
    }

    private void broadcastSimStateChanged(int slotId, String state, String reason) {
        Intent i = new Intent("android.intent.action.SIM_STATE_CHANGED");
        i.addFlags(67108864);
        i.putExtra("phoneName", "Phone");
        i.putExtra("ss", state);
        i.putExtra("reason", reason);
        SubscriptionManager.putPhoneIdAndSubIdExtra(i, slotId);
        logd("Broadcasting intent ACTION_SIM_STATE_CHANGED " + state + " reason " + reason + " for mCardIndex: " + slotId);
        ActivityManagerNative.broadcastStickyIntent(i, "android.permission.READ_PHONE_STATE", -1);
        if (this.mUserManager.isUserUnlocked()) {
            return;
        }
        this.rebroadcastIntentsOnUnlock.put(Integer.valueOf(slotId), i);
    }

    public void dispose() {
        logd("[dispose]");
        mContext.unregisterReceiver(this.sReceiver);
    }

    private void logd(String message) {
        Rlog.d(LOG_TAG, message);
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("SubscriptionInfoUpdater:");
        this.mCarrierServiceBindHelper.dump(fd, pw, args);
    }

    private void setAllDefaultSub(List<SubscriptionInfo> subInfos) {
        logd("[setAllDefaultSub]+ ");
        DefaultSmsSimSettings.setSmsTalkDefaultSim(subInfos, mContext);
        logd("[setSmsTalkDefaultSim]- ");
        DefaultVoiceCallSubSettings.setVoiceCallDefaultSub(subInfos);
        logd("[setVoiceCallDefaultSub]- ");
    }

    private void clearIccId(int slotId) {
        synchronized (this.mLock) {
            logd("[clearIccId], slotId = " + slotId);
            sFh[slotId] = null;
            mIccId[slotId] = null;
        }
    }

    private boolean checkAllIccIdReady() {
        logd("checkAllIccIdReady +, retry_count = " + this.mReadIccIdCount);
        for (int i = 0; i < PROJECT_SIM_NUM; i++) {
            String iccId = SystemProperties.get(PROPERTY_ICCID_SIM[i], UsimPBMemInfo.STRING_NOT_SET);
            if (iccId.length() == 3) {
                logd("No SIM insert :" + i);
            }
            if (iccId == null || iccId.equals(UsimPBMemInfo.STRING_NOT_SET)) {
                return false;
            }
            logd("iccId[" + i + "] = " + SubscriptionInfo.givePrintableIccid(iccId));
        }
        return true;
    }

    private void updateSubscriptionInfoIfNeed() {
        logd("[updateSubscriptionInfoIfNeed]+");
        boolean needUpdate = false;
        for (int i = 0; i < PROJECT_SIM_NUM; i++) {
            if (mIccId[i] == null || !mIccId[i].equals(SystemProperties.get(PROPERTY_ICCID_SIM[i], UsimPBMemInfo.STRING_NOT_SET))) {
                logd("[updateSubscriptionInfoIfNeed] icc id change, slot[" + i + "]");
                mIccId[i] = SystemProperties.get(PROPERTY_ICCID_SIM[i], UsimPBMemInfo.STRING_NOT_SET);
                needUpdate = true;
            }
        }
        if (isAllIccIdQueryDone() && needUpdate) {
            new Thread() {
                @Override
                public void run() {
                    SubscriptionInfoUpdater.this.updateSubscriptionInfoByIccId();
                }
            }.start();
        }
        logd("[updateSubscriptionInfoIfNeed]- return: " + needUpdate);
    }

    private Integer getCiIndex(Message msg) {
        Integer index = new Integer(0);
        if (msg != null) {
            if (msg.obj != null && (msg.obj instanceof Integer)) {
                return (Integer) msg.obj;
            }
            if (msg.obj != null && (msg.obj instanceof AsyncResult)) {
                AsyncResult ar = (AsyncResult) msg.obj;
                if (ar.userObj != null && (ar.userObj instanceof Integer)) {
                    return (Integer) ar.userObj;
                }
                return index;
            }
            return index;
        }
        return index;
    }

    private boolean checkIsAvailable() {
        boolean result = true;
        int i = 0;
        while (true) {
            if (i >= PROJECT_SIM_NUM) {
                break;
            }
            if (sIsUpdateAvailable[i] > 0) {
                i++;
            } else {
                logd("sIsUpdateAvailable[" + i + "] = " + sIsUpdateAvailable[i]);
                result = false;
                break;
            }
        }
        logd("checkIsAvailable result = " + result);
        return result;
    }

    private void updateSubName(int subId) {
        String nameToSet;
        SubscriptionInfo subInfo = this.mSubscriptionManager.getSubscriptionInfo(subId);
        if (subInfo == null || subInfo.getNameSource() == 2) {
            return;
        }
        SpnOverride spnOverride = SpnOverride.getInstance();
        String carrierName = TelephonyManager.getDefault().getSimOperator(subId);
        int slotId = SubscriptionManager.getSlotId(subId);
        logd("updateSubName, carrierName = " + carrierName + ", subId = " + subId);
        if (!SubscriptionManager.isValidSlotId(slotId)) {
            return;
        }
        if (spnOverride.containsCarrierEx(carrierName)) {
            nameToSet = spnOverride.lookupOperatorName(subId, carrierName, true, mContext);
            logd("SPN found, name = " + nameToSet);
        } else {
            nameToSet = "CARD " + Integer.toString(slotId + 1);
            logd("SPN not found, set name to " + nameToSet);
        }
        this.mSubscriptionManager.setDisplayName(nameToSet, subId);
    }
}
