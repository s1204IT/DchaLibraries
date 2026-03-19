package com.android.internal.telephony.cat;

import android.R;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.SystemProperties;
import android.provider.Settings;
import android.provider.Telephony;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import com.android.internal.telephony.CallFailCause;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.ServiceStateTracker;
import com.android.internal.telephony.cat.AppInterface;
import com.android.internal.telephony.cat.Duration;
import com.android.internal.telephony.uicc.IccCardStatus;
import com.android.internal.telephony.uicc.IccFileHandler;
import com.android.internal.telephony.uicc.IccRecords;
import com.android.internal.telephony.uicc.IccRefreshResponse;
import com.android.internal.telephony.uicc.IccUtils;
import com.android.internal.telephony.uicc.UiccCard;
import com.android.internal.telephony.uicc.UiccCardApplication;
import com.android.internal.telephony.uicc.UiccController;
import com.mediatek.internal.telephony.uicc.UsimPBMemInfo;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

public class CatService extends Handler implements AppInterface {

    private static final int[] f17x72eb89a2 = null;

    private static final int[] f18comandroidinternaltelephonycatResultCodeSwitchesValues = null;
    static final String ACTION_PREBOOT_IPO = "android.intent.action.ACTION_PREBOOT_IPO";
    static final String ACTION_SHUTDOWN_IPO = "android.intent.action.ACTION_SHUTDOWN_IPO";
    private static final boolean DBG = true;
    private static final int DEV_ID_DISPLAY = 2;
    private static final int DEV_ID_EARPIECE = 3;
    private static final int DEV_ID_KEYPAD = 1;
    private static final int DEV_ID_NETWORK = 131;
    private static final int DEV_ID_TERMINAL = 130;
    private static final int DEV_ID_UICC = 129;
    static final String DISPLAY_TEXT_DISABLE_PROPERTY = "persist.service.cat.dt.disable";
    static final int EVENT_LIST_ELEMENT_BROWSER_TERMINATION = 8;
    static final int EVENT_LIST_ELEMENT_CALL_CONNECTED = 1;
    static final int EVENT_LIST_ELEMENT_CALL_DISCONNECTED = 2;
    static final int EVENT_LIST_ELEMENT_CARD_READER_STATUS = 6;
    static final int EVENT_LIST_ELEMENT_IDLE_SCREEN_AVAILABLE = 5;
    static final int EVENT_LIST_ELEMENT_LANGUAGE_SELECTION = 7;
    static final int EVENT_LIST_ELEMENT_LOCATION_STATUS = 3;
    static final int EVENT_LIST_ELEMENT_MT_CALL = 0;
    static final int EVENT_LIST_ELEMENT_USER_ACTIVITY = 4;
    static final String IDLE_SCREEN_ENABLE_KEY = "_enable";
    static final String IDLE_SCREEN_INTENT_NAME = "android.intent.action.IDLE_SCREEN_NEEDED";
    protected static final int MSG_ID_ALPHA_NOTIFY = 9;
    public static final int MSG_ID_CACHED_DISPLAY_TEXT_TIMEOUT = 46;
    private static final int MSG_ID_CALL_CTRL = 25;
    protected static final int MSG_ID_CALL_SETUP = 4;
    public static final int MSG_ID_CONN_RETRY_TIMEOUT = 47;
    static final int MSG_ID_DB_HANDLER = 12;
    private static final int MSG_ID_DISABLE_DISPLAY_TEXT_DELAYED = 15;
    private static final int MSG_ID_EVDL_CALL = 21;
    static final int MSG_ID_EVENT_DOWNLOAD = 11;
    protected static final int MSG_ID_EVENT_NOTIFY = 3;
    protected static final int MSG_ID_ICC_CHANGED = 8;
    private static final int MSG_ID_ICC_RECORDS_LOADED = 20;
    private static final int MSG_ID_ICC_REFRESH = 30;
    private static final int MSG_ID_IVSR_DELAYED = 14;
    static final int MSG_ID_LAUNCH_DB_SETUP_MENU = 13;
    private static final int MSG_ID_MODEM_EVDL_CALL_CONN_TIMEOUT = 22;
    private static final int MSG_ID_MODEM_EVDL_CALL_DISCONN_TIMEOUT = 23;
    protected static final int MSG_ID_PROACTIVE_COMMAND = 2;
    static final int MSG_ID_REFRESH = 5;
    static final int MSG_ID_RESPONSE = 6;
    static final int MSG_ID_RIL_MSG_DECODED = 10;
    protected static final int MSG_ID_SESSION_END = 1;
    private static final int MSG_ID_SETUP_MENU_RESET = 24;
    static final int MSG_ID_SIM_READY = 7;
    static final String STK_DEFAULT = "Default Message";
    private static final int STK_EVDL_CALL_STATE_CALLCONN = 0;
    private static final int STK_EVDL_CALL_STATE_CALLDISCONN = 1;
    static final String USER_ACTIVITY_ENABLE_KEY = "state";
    static final String USER_ACTIVITY_INTENT_NAME = "android.intent.action.stk.USER_ACTIVITY.enable";
    private static final String mEsnTrackUtkMenuSelect = "com.android.internal.telephony.cat.ESN_MENU_SELECTION";
    private static IccRecords mIccRecords;
    private static UiccCardApplication mUiccApplication;
    private BipService mBipService;
    private CommandsInterface mCmdIf;
    private Context mContext;
    private byte[] mEventList;
    private HandlerThread mHandlerThread;
    private RilMessageDecoder mMsgDecoder;
    private int mSlotId;
    private boolean mStkAppInstalled;
    private UiccController mUiccController;
    private static final Object sInstanceLock = new Object();
    private static CatService[] sInstance = null;
    private static String[] sInstKey = {"sInstanceSim1", "sInstanceSim2", "sInstanceSim3", "sInstanceSim4"};
    protected static Object mLock = new Object();
    private static boolean mIsCatServiceDisposed = false;
    private CatCmdMessage mCurrentCmd = null;
    private CatCmdMessage mMenuCmd = null;
    private IccCardStatus.CardState mCardState = IccCardStatus.CardState.CARDSTATE_ABSENT;
    private boolean default_send_setupmenu_tr = true;
    public boolean mGotSetUpMenu = false;
    public boolean mSaveNewSetUpMenu = false;
    private boolean mSetUpMenuFromMD = false;
    private boolean mReadFromPreferenceDone = false;
    private int MODEM_EVDL_TIMEOUT = ServiceStateTracker.NITZ_UPDATE_DIFF_DEFAULT;
    private LinkedList<Integer> mEvdlCallConnObjQ = new LinkedList<>();
    private LinkedList<Integer> mEvdlCallDisConnObjQ = new LinkedList<>();
    private int mEvdlCallObj = 0;
    private String simState = null;
    private int simIdfromIntent = 0;
    private CatCmdMessage mCachedDisplayTextCmd = null;
    private boolean mHasCachedDTCmd = false;
    private boolean isIvsrBootUp = false;
    private final int IVSR_DELAYED_TIME = ServiceStateTracker.DEFAULT_GPRS_CHECK_PERIOD_MILLIS;
    private boolean isDisplayTextDisabled = false;
    private final int DISABLE_DISPLAY_TEXT_DELAYED_TIME = 30000;
    boolean mNeedRegisterAgain = false;
    private LinkedList<EventDownloadCallInfo> mEventDownloadCallDisConnInfo = new LinkedList<>();
    private LinkedList<EventDownloadCallInfo> mEventDownloadCallConnInfo = new LinkedList<>();
    private int mNumEventDownloadCallDisConn = 0;
    private int mNumEventDownloadCallConn = 0;
    private boolean mIsAllCallDisConn = false;
    private boolean mIsProactiveCmdResponsed = false;
    private int CACHED_DISPLAY_TIMEOUT = 120000;
    private final int LTE_DC_PHONE_PROXY_ID = 0;
    Handler mTimeoutHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (!SystemProperties.get("ro.mtk_bsp_package").equals("1")) {
                switch (msg.what) {
                    case 22:
                        CatLog.d(this, "modem MODEM_EVDL_CALL_CONN_TIMEOUT timout");
                        if (CatService.this.mNumEventDownloadCallConn > 0) {
                            CatService catService = CatService.this;
                            catService.mNumEventDownloadCallConn--;
                        }
                        break;
                    case 23:
                        CatLog.d(this, "modem MODEM_EVDL_CALL_DISCONN_TIMEOUT timout");
                        if (CatService.this.mNumEventDownloadCallDisConn > 0) {
                            CatService catService2 = CatService.this;
                            catService2.mNumEventDownloadCallDisConn--;
                        }
                        break;
                    case CatService.MSG_ID_CACHED_DISPLAY_TEXT_TIMEOUT:
                        CatLog.d(this, "Cache DISPLAY_TEXT time out, sim_id: " + CatService.this.mSlotId);
                        CatService.this.clearCachedDisplayText(CatService.this.mSlotId);
                        break;
                }
            }
            if (msg.what != 15) {
                return;
            }
            CatLog.d(this, "[Reset Disable Display Text flag because timeout");
            CatService.this.isDisplayTextDisabled = false;
        }
    };
    private final BroadcastReceiver mStkIdleScreenAvailableReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String evtAction = intent.getAction();
            CatLog.d("CatService", "mStkIdleScreenAvailableReceiver() - evtAction[" + evtAction + "]");
            if (evtAction.equals("android.intent.action.stk.IDLE_SCREEN_AVAILABLE")) {
                CatLog.d("CatService", "mStkIdleScreenAvailableReceiver() - Received[IDLE_SCREEN_AVAILABLE]");
                CatResponseMessage resMsg = new CatResponseMessage();
                resMsg.setEventId(5);
                resMsg.setSourceId(2);
                resMsg.setDestinationId(129);
                resMsg.setAdditionalInfo(null);
                resMsg.setOneShot(true);
                CatLog.d("CatService", "handle Idle Screen Available");
                CatService.this.onEventDownload(resMsg);
                return;
            }
            CatLog.d("CatService", "mStkIdleScreenAvailableReceiver() - Received needn't handle!");
        }
    };
    private final BroadcastReceiver mClearDisplayTextReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (SystemProperties.get("ro.mtk_bsp_package").equals("1") || !AppInterface.CLEAR_DISPLAY_TEXT_CMD.equals(intent.getAction())) {
                return;
            }
            int sim_id = intent.getIntExtra("SIM_ID", -1);
            CatLog.d("CatService", "mClearDisplayTextReceiver, sim_id: " + sim_id);
            CatService.this.clearCachedDisplayText(sim_id);
        }
    };
    private BroadcastReceiver CatServiceReceiver = new BroadcastReceiver() {
        public static final String EXTRA_VALUE_REMOVE_SIM = "REMOVE";
        public static final String INTENT_KEY_DETECT_STATUS = "simDetectStatus";

        @Override
        public void onReceive(Context context, Intent intent) {
            int id;
            String action = intent.getAction();
            CatLog.d(this, "CatServiceReceiver action: " + action);
            if (action.equals("android.intent.action.ACTION_SHUTDOWN_IPO")) {
                CatLog.d(this, "remove event list because of ipo shutdown");
                CatService.this.mEventList = null;
                CatService.this.mSaveNewSetUpMenu = false;
            } else if (action.equals("mediatek.intent.action.IVSR_NOTIFY")) {
                if (CatService.this.mSlotId != intent.getIntExtra("slot", 0)) {
                    return;
                }
                String ivsrAction = intent.getStringExtra("action");
                if (ivsrAction.equals(Telephony.BaseMmsColumns.START)) {
                    CatLog.d(this, "[IVSR set IVSR flag");
                    CatService.this.isIvsrBootUp = true;
                    CatService.this.sendEmptyMessageDelayed(14, 60000L);
                }
            } else if (action.equals("com.android.phone.ACTION_SIM_RECOVERY_DONE") || action.equals("android.intent.action.ACTION_MD_TYPE_CHANGE")) {
                if (action.equals("com.android.phone.ACTION_SIM_RECOVERY_DONE")) {
                    CatLog.d(this, "[Set SIM Recovery flag, sim: " + CatService.this.mSlotId + ", isDisplayTextDisabled: " + (CatService.this.isDisplayTextDisabled ? 1 : 0));
                } else {
                    CatLog.d(this, "[World phone flag: " + CatService.this.mSlotId + ", isDisplayTextDisabled: " + (CatService.this.isDisplayTextDisabled ? 1 : 0));
                }
                CatService.this.startTimeOut(15, 30000L);
                CatService.this.isDisplayTextDisabled = true;
            }
            if ("android.intent.action.SIM_STATE_CHANGED".equals(intent.getAction()) && (id = intent.getIntExtra("slot", -1)) == CatService.this.mSlotId) {
                CatService.this.simState = intent.getStringExtra("ss");
                CatService.this.simIdfromIntent = id;
                CatLog.d(this, "simIdfromIntent[" + CatService.this.simIdfromIntent + "],simState[" + CatService.this.simState + "] , mSlotId: " + CatService.this.mSlotId);
                if (!"ABSENT".equals(CatService.this.simState)) {
                    if ("NOT_READY".equals(CatService.this.simState)) {
                        CatService.this.mCardState = IccCardStatus.CardState.CARDSTATE_ABSENT;
                    }
                } else {
                    if (TelephonyManager.getDefault().hasIccCard(CatService.this.mSlotId)) {
                        CatLog.d(this, "Igonre absent sim state");
                        return;
                    }
                    CatService.this.clearCachedDisplayText(id);
                    CatService.this.mSaveNewSetUpMenu = false;
                    CatService.this.handleDBHandler(CatService.this.mSlotId);
                }
            }
        }
    };
    private ContentObserver mPowerOnSequenceObserver = new ContentObserver(this) {
        @Override
        public void onChange(boolean selfChange, Uri uri) {
            int flightMode;
            if (SystemProperties.get("ro.mtk_bsp_package").equals("1")) {
                return;
            }
            CatLog.d(this, "mPowerOnSequenceObserver onChange");
            int seqValue = Settings.System.getInt(CatService.this.mContext.getContentResolver(), "dialog_sequence_settings", 0);
            CatLog.d(this, "mPowerOnSequenceObserver onChange, " + seqValue);
            if (seqValue == 2) {
                if (CatService.this.mCachedDisplayTextCmd == null) {
                    return;
                }
                boolean isAlarmState = CatService.this.isAlarmBoot();
                try {
                    flightMode = Settings.Global.getInt(CatService.this.mContext.getContentResolver(), "airplane_mode_on");
                } catch (Settings.SettingNotFoundException e) {
                    CatLog.d(this, "fail to get property from Settings");
                    flightMode = 0;
                }
                boolean isFlightMode = flightMode != 0;
                CatLog.d(this, "isAlarmState = " + isAlarmState + ", isFlightMode = " + isFlightMode + ", flightMode = " + flightMode);
                if (isAlarmState && isFlightMode) {
                    CatService.this.resetPowerOnSequenceFlag();
                    CatService.this.sendTerminalResponse(CatService.this.mCachedDisplayTextCmd.mCmdDet, ResultCode.OK, false, 0, null);
                    CatService.this.mCachedDisplayTextCmd = null;
                    CatService.this.unregisterPowerOnSequenceObserver();
                    return;
                }
                if (CatService.this.checkSetupWizardInstalled()) {
                    CatService.this.resetPowerOnSequenceFlag();
                    CatService.this.sendTerminalResponse(CatService.this.mCachedDisplayTextCmd.mCmdDet, ResultCode.BACKWARD_MOVE_BY_USER, false, 0, null);
                    CatService.this.mCachedDisplayTextCmd = null;
                    CatService.this.unregisterPowerOnSequenceObserver();
                    return;
                }
                if (CatService.this.isIvsrBootUp) {
                    CatLog.d(this, "[IVSR send TR directly");
                    CatService.this.resetPowerOnSequenceFlag();
                    CatService.this.sendTerminalResponse(CatService.this.mCachedDisplayTextCmd.mCmdDet, ResultCode.BACKWARD_MOVE_BY_USER, false, 0, null);
                    CatService.this.mCachedDisplayTextCmd = null;
                    CatService.this.unregisterPowerOnSequenceObserver();
                    return;
                }
                if (CatService.this.isDisplayTextDisabled) {
                    CatLog.d(this, "[SIM Recovery send TR directly");
                    CatService.this.resetPowerOnSequenceFlag();
                    CatService.this.sendTerminalResponse(CatService.this.mCachedDisplayTextCmd.mCmdDet, ResultCode.BACKWARD_MOVE_BY_USER, false, 0, null);
                    CatService.this.mCachedDisplayTextCmd = null;
                    CatService.this.unregisterPowerOnSequenceObserver();
                    return;
                }
                CatLog.d(this, "send DISPLAY_TEXT to app");
                CatService.this.broadcastCatCmdIntent(CatService.this.mCachedDisplayTextCmd);
                CatService.this.mCachedDisplayTextCmd = null;
                CatService.this.unregisterPowerOnSequenceObserver();
                return;
            }
            if (seqValue != 0 || CatService.this.mCachedDisplayTextCmd == null) {
                return;
            }
            Settings.System.putInt(CatService.this.mContext.getContentResolver(), "dialog_sequence_settings", 2);
        }

        @Override
        public void onChange(boolean selfChange) {
            onChange(selfChange, null);
        }
    };
    private CatCmdMessage mCmdMessage = null;

    private static int[] m219xe796fd46() {
        if (f17x72eb89a2 != null) {
            return f17x72eb89a2;
        }
        int[] iArr = new int[AppInterface.CommandType.valuesCustom().length];
        try {
            iArr[AppInterface.CommandType.ACTIVATE.ordinal()] = 1;
        } catch (NoSuchFieldError e) {
        }
        try {
            iArr[AppInterface.CommandType.CALLCTRL_RSP_MSG.ordinal()] = 41;
        } catch (NoSuchFieldError e2) {
        }
        try {
            iArr[AppInterface.CommandType.CLOSE_CHANNEL.ordinal()] = 2;
        } catch (NoSuchFieldError e3) {
        }
        try {
            iArr[AppInterface.CommandType.DECLARE_SERVICE.ordinal()] = 42;
        } catch (NoSuchFieldError e4) {
        }
        try {
            iArr[AppInterface.CommandType.DISPLAY_MULTIMEDIA_MESSAGE.ordinal()] = 43;
        } catch (NoSuchFieldError e5) {
        }
        try {
            iArr[AppInterface.CommandType.DISPLAY_TEXT.ordinal()] = 3;
        } catch (NoSuchFieldError e6) {
        }
        try {
            iArr[AppInterface.CommandType.GET_CHANNEL_STATUS.ordinal()] = 44;
        } catch (NoSuchFieldError e7) {
        }
        try {
            iArr[AppInterface.CommandType.GET_FRAME_STATUS.ordinal()] = 45;
        } catch (NoSuchFieldError e8) {
        }
        try {
            iArr[AppInterface.CommandType.GET_INKEY.ordinal()] = 4;
        } catch (NoSuchFieldError e9) {
        }
        try {
            iArr[AppInterface.CommandType.GET_INPUT.ordinal()] = 5;
        } catch (NoSuchFieldError e10) {
        }
        try {
            iArr[AppInterface.CommandType.GET_READER_STATUS.ordinal()] = 46;
        } catch (NoSuchFieldError e11) {
        }
        try {
            iArr[AppInterface.CommandType.GET_SERVICE_INFORMATION.ordinal()] = 47;
        } catch (NoSuchFieldError e12) {
        }
        try {
            iArr[AppInterface.CommandType.LANGUAGE_NOTIFICATION.ordinal()] = 48;
        } catch (NoSuchFieldError e13) {
        }
        try {
            iArr[AppInterface.CommandType.LAUNCH_BROWSER.ordinal()] = 6;
        } catch (NoSuchFieldError e14) {
        }
        try {
            iArr[AppInterface.CommandType.MORE_TIME.ordinal()] = 49;
        } catch (NoSuchFieldError e15) {
        }
        try {
            iArr[AppInterface.CommandType.OPEN_CHANNEL.ordinal()] = 7;
        } catch (NoSuchFieldError e16) {
        }
        try {
            iArr[AppInterface.CommandType.PERFORM_CARD_APDU.ordinal()] = 50;
        } catch (NoSuchFieldError e17) {
        }
        try {
            iArr[AppInterface.CommandType.PLAY_TONE.ordinal()] = 8;
        } catch (NoSuchFieldError e18) {
        }
        try {
            iArr[AppInterface.CommandType.POLLING_OFF.ordinal()] = 51;
        } catch (NoSuchFieldError e19) {
        }
        try {
            iArr[AppInterface.CommandType.POLL_INTERVAL.ordinal()] = 52;
        } catch (NoSuchFieldError e20) {
        }
        try {
            iArr[AppInterface.CommandType.POWER_OFF_CARD.ordinal()] = 53;
        } catch (NoSuchFieldError e21) {
        }
        try {
            iArr[AppInterface.CommandType.POWER_ON_CARD.ordinal()] = 54;
        } catch (NoSuchFieldError e22) {
        }
        try {
            iArr[AppInterface.CommandType.PROVIDE_LOCAL_INFORMATION.ordinal()] = 9;
        } catch (NoSuchFieldError e23) {
        }
        try {
            iArr[AppInterface.CommandType.RECEIVE_DATA.ordinal()] = 10;
        } catch (NoSuchFieldError e24) {
        }
        try {
            iArr[AppInterface.CommandType.REFRESH.ordinal()] = 11;
        } catch (NoSuchFieldError e25) {
        }
        try {
            iArr[AppInterface.CommandType.RETRIEVE_MULTIMEDIA_MESSAGE.ordinal()] = 55;
        } catch (NoSuchFieldError e26) {
        }
        try {
            iArr[AppInterface.CommandType.RUN_AT_COMMAND.ordinal()] = 56;
        } catch (NoSuchFieldError e27) {
        }
        try {
            iArr[AppInterface.CommandType.SELECT_ITEM.ordinal()] = 12;
        } catch (NoSuchFieldError e28) {
        }
        try {
            iArr[AppInterface.CommandType.SEND_DATA.ordinal()] = 13;
        } catch (NoSuchFieldError e29) {
        }
        try {
            iArr[AppInterface.CommandType.SEND_DTMF.ordinal()] = 14;
        } catch (NoSuchFieldError e30) {
        }
        try {
            iArr[AppInterface.CommandType.SEND_SMS.ordinal()] = 15;
        } catch (NoSuchFieldError e31) {
        }
        try {
            iArr[AppInterface.CommandType.SEND_SS.ordinal()] = 16;
        } catch (NoSuchFieldError e32) {
        }
        try {
            iArr[AppInterface.CommandType.SEND_USSD.ordinal()] = 17;
        } catch (NoSuchFieldError e33) {
        }
        try {
            iArr[AppInterface.CommandType.SERVICE_SEARCH.ordinal()] = 57;
        } catch (NoSuchFieldError e34) {
        }
        try {
            iArr[AppInterface.CommandType.SET_FRAME.ordinal()] = 58;
        } catch (NoSuchFieldError e35) {
        }
        try {
            iArr[AppInterface.CommandType.SET_UP_CALL.ordinal()] = 18;
        } catch (NoSuchFieldError e36) {
        }
        try {
            iArr[AppInterface.CommandType.SET_UP_EVENT_LIST.ordinal()] = 19;
        } catch (NoSuchFieldError e37) {
        }
        try {
            iArr[AppInterface.CommandType.SET_UP_IDLE_MODE_TEXT.ordinal()] = 20;
        } catch (NoSuchFieldError e38) {
        }
        try {
            iArr[AppInterface.CommandType.SET_UP_MENU.ordinal()] = 21;
        } catch (NoSuchFieldError e39) {
        }
        try {
            iArr[AppInterface.CommandType.SUBMIT_MULTIMEDIA_MESSAGE.ordinal()] = 59;
        } catch (NoSuchFieldError e40) {
        }
        try {
            iArr[AppInterface.CommandType.TIMER_MANAGEMENT.ordinal()] = 60;
        } catch (NoSuchFieldError e41) {
        }
        f17x72eb89a2 = iArr;
        return iArr;
    }

    private static int[] m220getcomandroidinternaltelephonycatResultCodeSwitchesValues() {
        if (f18comandroidinternaltelephonycatResultCodeSwitchesValues != null) {
            return f18comandroidinternaltelephonycatResultCodeSwitchesValues;
        }
        int[] iArr = new int[ResultCode.valuesCustom().length];
        try {
            iArr[ResultCode.ACCESS_TECH_UNABLE_TO_PROCESS.ordinal()] = 41;
        } catch (NoSuchFieldError e) {
        }
        try {
            iArr[ResultCode.BACKWARD_MOVE_BY_USER.ordinal()] = 1;
        } catch (NoSuchFieldError e2) {
        }
        try {
            iArr[ResultCode.BEYOND_TERMINAL_CAPABILITY.ordinal()] = 42;
        } catch (NoSuchFieldError e3) {
        }
        try {
            iArr[ResultCode.BIP_ERROR.ordinal()] = 43;
        } catch (NoSuchFieldError e4) {
        }
        try {
            iArr[ResultCode.CMD_DATA_NOT_UNDERSTOOD.ordinal()] = 2;
        } catch (NoSuchFieldError e5) {
        }
        try {
            iArr[ResultCode.CMD_NUM_NOT_KNOWN.ordinal()] = 44;
        } catch (NoSuchFieldError e6) {
        }
        try {
            iArr[ResultCode.CMD_TYPE_NOT_UNDERSTOOD.ordinal()] = 45;
        } catch (NoSuchFieldError e7) {
        }
        try {
            iArr[ResultCode.CONTRADICTION_WITH_TIMER.ordinal()] = 46;
        } catch (NoSuchFieldError e8) {
        }
        try {
            iArr[ResultCode.FRAMES_ERROR.ordinal()] = 47;
        } catch (NoSuchFieldError e9) {
        }
        try {
            iArr[ResultCode.HELP_INFO_REQUIRED.ordinal()] = 3;
        } catch (NoSuchFieldError e10) {
        }
        try {
            iArr[ResultCode.LAUNCH_BROWSER_ERROR.ordinal()] = 4;
        } catch (NoSuchFieldError e11) {
        }
        try {
            iArr[ResultCode.MMS_ERROR.ordinal()] = 48;
        } catch (NoSuchFieldError e12) {
        }
        try {
            iArr[ResultCode.MMS_TEMPORARY.ordinal()] = 49;
        } catch (NoSuchFieldError e13) {
        }
        try {
            iArr[ResultCode.MULTI_CARDS_CMD_ERROR.ordinal()] = 50;
        } catch (NoSuchFieldError e14) {
        }
        try {
            iArr[ResultCode.NAA_CALL_CONTROL_TEMPORARY.ordinal()] = 51;
        } catch (NoSuchFieldError e15) {
        }
        try {
            iArr[ResultCode.NETWORK_CRNTLY_UNABLE_TO_PROCESS.ordinal()] = 5;
        } catch (NoSuchFieldError e16) {
        }
        try {
            iArr[ResultCode.NO_RESPONSE_FROM_USER.ordinal()] = 6;
        } catch (NoSuchFieldError e17) {
        }
        try {
            iArr[ResultCode.OK.ordinal()] = 7;
        } catch (NoSuchFieldError e18) {
        }
        try {
            iArr[ResultCode.PRFRMD_ICON_NOT_DISPLAYED.ordinal()] = 8;
        } catch (NoSuchFieldError e19) {
        }
        try {
            iArr[ResultCode.PRFRMD_LIMITED_SERVICE.ordinal()] = 9;
        } catch (NoSuchFieldError e20) {
        }
        try {
            iArr[ResultCode.PRFRMD_MODIFIED_BY_NAA.ordinal()] = 10;
        } catch (NoSuchFieldError e21) {
        }
        try {
            iArr[ResultCode.PRFRMD_NAA_NOT_ACTIVE.ordinal()] = 11;
        } catch (NoSuchFieldError e22) {
        }
        try {
            iArr[ResultCode.PRFRMD_TONE_NOT_PLAYED.ordinal()] = 12;
        } catch (NoSuchFieldError e23) {
        }
        try {
            iArr[ResultCode.PRFRMD_WITH_ADDITIONAL_EFS_READ.ordinal()] = 13;
        } catch (NoSuchFieldError e24) {
        }
        try {
            iArr[ResultCode.PRFRMD_WITH_MISSING_INFO.ordinal()] = 14;
        } catch (NoSuchFieldError e25) {
        }
        try {
            iArr[ResultCode.PRFRMD_WITH_MODIFICATION.ordinal()] = 15;
        } catch (NoSuchFieldError e26) {
        }
        try {
            iArr[ResultCode.PRFRMD_WITH_PARTIAL_COMPREHENSION.ordinal()] = 16;
        } catch (NoSuchFieldError e27) {
        }
        try {
            iArr[ResultCode.REQUIRED_VALUES_MISSING.ordinal()] = 52;
        } catch (NoSuchFieldError e28) {
        }
        try {
            iArr[ResultCode.SMS_RP_ERROR.ordinal()] = 53;
        } catch (NoSuchFieldError e29) {
        }
        try {
            iArr[ResultCode.SS_RETURN_ERROR.ordinal()] = 54;
        } catch (NoSuchFieldError e30) {
        }
        try {
            iArr[ResultCode.TERMINAL_CRNTLY_UNABLE_TO_PROCESS.ordinal()] = 17;
        } catch (NoSuchFieldError e31) {
        }
        try {
            iArr[ResultCode.UICC_SESSION_TERM_BY_USER.ordinal()] = 18;
        } catch (NoSuchFieldError e32) {
        }
        try {
            iArr[ResultCode.USER_CLEAR_DOWN_CALL.ordinal()] = 55;
        } catch (NoSuchFieldError e33) {
        }
        try {
            iArr[ResultCode.USER_NOT_ACCEPT.ordinal()] = 19;
        } catch (NoSuchFieldError e34) {
        }
        try {
            iArr[ResultCode.USIM_CALL_CONTROL_PERMANENT.ordinal()] = 56;
        } catch (NoSuchFieldError e35) {
        }
        try {
            iArr[ResultCode.USSD_RETURN_ERROR.ordinal()] = 57;
        } catch (NoSuchFieldError e36) {
        }
        try {
            iArr[ResultCode.USSD_SS_SESSION_TERM_BY_USER.ordinal()] = 58;
        } catch (NoSuchFieldError e37) {
        }
        f18comandroidinternaltelephonycatResultCodeSwitchesValues = iArr;
        return iArr;
    }

    void cancelTimeOut(int msg) {
        CatLog.d(this, "cancelTimeOut, sim_id: " + this.mSlotId + ", msg id: " + msg);
        this.mTimeoutHandler.removeMessages(msg);
    }

    void startTimeOut(int msg, long delay) {
        CatLog.d(this, "startTimeOut, sim_id: " + this.mSlotId + ", msg id: " + msg);
        cancelTimeOut(msg);
        this.mTimeoutHandler.sendMessageDelayed(this.mTimeoutHandler.obtainMessage(msg), delay);
    }

    private void clearCachedDisplayText(int sim_id) {
        if (SystemProperties.get("ro.mtk_bsp_package").equals("1")) {
            return;
        }
        CatLog.d("CatService", "clearCachedDisplayText, sim_id: " + sim_id + ", mSlotId: " + this.mSlotId + ", mCachedDisplayTextCmd: " + (this.mCachedDisplayTextCmd != null ? 1 : 0));
        if (sim_id == this.mSlotId) {
            if (this.mCachedDisplayTextCmd == null) {
                if (this.mHasCachedDTCmd) {
                    unregisterPowerOnSequenceObserver();
                    resetPowerOnSequenceFlag();
                    return;
                }
                return;
            }
            CatResponseMessage resMsg = new CatResponseMessage(this.mCachedDisplayTextCmd);
            resMsg.setResultCode(ResultCode.UICC_SESSION_TERM_BY_USER);
            handleCmdResponse(resMsg);
            this.mCachedDisplayTextCmd = null;
            unregisterPowerOnSequenceObserver();
        }
    }

    private CatService(CommandsInterface ci, UiccCardApplication ca, IccRecords ir, Context context, IccFileHandler fh, UiccCard ic, int slotId) {
        this.mMsgDecoder = null;
        this.mStkAppInstalled = false;
        this.mBipService = null;
        if (ci == null || ca == null || ir == null || context == null || fh == null || ic == null) {
            throw new NullPointerException("Service: Input parameters must not be null");
        }
        this.mCmdIf = ci;
        this.mContext = context;
        this.mSlotId = slotId;
        this.mHandlerThread = new HandlerThread("Cat Telephony service" + slotId);
        this.mHandlerThread.start();
        CatLog.d(this, "slotId " + slotId);
        this.mMsgDecoder = RilMessageDecoder.getInstance(this, fh, slotId);
        if (this.mMsgDecoder == null) {
            CatLog.d(this, "Null RilMessageDecoder instance");
            return;
        }
        this.mMsgDecoder.start();
        this.mBipService = BipService.getInstance(this.mContext, this, this.mSlotId, this.mCmdIf, fh);
        this.mCmdIf.setOnCatSessionEnd(this, 1, null);
        this.mCmdIf.setOnCatProactiveCmd(this, 2, null);
        this.mCmdIf.setOnCatEvent(this, 3, null);
        this.mCmdIf.setOnCatCallSetUp(this, 4, null);
        if (!SystemProperties.get("ro.mtk_bsp_package").equals("1")) {
            this.mCmdIf.setOnStkEvdlCall(this, 21, null);
        }
        this.mCmdIf.setOnStkSetupMenuReset(this, 24, null);
        this.mCmdIf.registerForIccRefresh(this, 30, null);
        this.mCmdIf.setOnCatCcAlphaNotify(this, 9, null);
        mIccRecords = ir;
        mUiccApplication = ca;
        mUiccApplication.registerForReady(this, 7, null);
        mIccRecords.registerForRecordsLoaded(this, 20, null);
        CatLog.d(this, "registerForRecordsLoaded slotid=" + this.mSlotId + " instance:" + this);
        IntentFilter intentFilter = new IntentFilter("android.intent.action.ACTION_SHUTDOWN_IPO");
        intentFilter.addAction("mediatek.intent.action.IVSR_NOTIFY");
        intentFilter.addAction("com.android.phone.ACTION_SIM_RECOVERY_DONE");
        intentFilter.addAction("android.intent.action.ACTION_MD_TYPE_CHANGE");
        IntentFilter mSIMStateChangeFilter = new IntentFilter("android.intent.action.SIM_STATE_CHANGED");
        mSIMStateChangeFilter.addAction("android.intent.action.RADIO_TECHNOLOGY");
        this.mContext.registerReceiver(this.CatServiceReceiver, intentFilter);
        this.mContext.registerReceiver(this.CatServiceReceiver, mSIMStateChangeFilter);
        IntentFilter mIdleScreenAvailableFilter = new IntentFilter("android.intent.action.stk.IDLE_SCREEN_AVAILABLE");
        this.mContext.registerReceiver(this.mStkIdleScreenAvailableReceiver, mIdleScreenAvailableFilter);
        CatLog.d(this, "CatService: is running");
        this.mUiccController = UiccController.getInstance();
        this.mUiccController.registerForIccChanged(this, 8, null);
        this.mStkAppInstalled = isStkAppInstalled();
        if (!SystemProperties.get("ro.mtk_bsp_package").equals("1") && this.mHasCachedDTCmd) {
            registerPowerOnSequenceObserver();
            IntentFilter ClearDisplayTextFilter = new IntentFilter(AppInterface.CLEAR_DISPLAY_TEXT_CMD);
            this.mContext.registerReceiver(this.mClearDisplayTextReceiver, ClearDisplayTextFilter);
        }
        CatLog.d(this, "Running CAT service on Slotid: " + this.mSlotId + ". STK app installed:" + this.mStkAppInstalled);
    }

    public static CatService getInstance(CommandsInterface ci, Context context, UiccCard ic, int slotId) {
        UiccCardApplication ca = null;
        IccFileHandler fh = null;
        IccRecords ir = null;
        if (ic != null) {
            int phoneType = 1;
            int[] subId = SubscriptionManager.getSubId(slotId);
            if (subId != null) {
                phoneType = TelephonyManager.getDefault().getCurrentPhoneType(subId[0]);
                CatLog.d("CatService", "getInstance phoneType : " + phoneType + "slotid: " + slotId + "subId[0]:" + subId[0]);
            }
            if (phoneType == 2) {
                ca = ic.getApplication(2);
            } else {
                ca = ic.getApplicationIndex(0);
            }
            if (ca != null) {
                fh = ca.getIccFileHandler();
                ir = ca.getIccRecords();
            }
        }
        CatLog.d("CatService", "call getInstance 1");
        synchronized (sInstanceLock) {
            if (sInstance == null) {
                int simCount = TelephonyManager.getDefault().getSimCount();
                sInstance = new CatService[simCount];
                for (int i = 0; i < simCount; i++) {
                    sInstance[i] = null;
                }
            }
            if (sInstance[slotId] == null) {
                if (ci == null || ca == null || ir == null || context == null || fh == null || ic == null) {
                    CatLog.d("CatService", "null parameters, return directly");
                    return null;
                }
                sInstance[slotId] = new CatService(ci, ca, ir, context, fh, ic, slotId);
                CatLog.d(sInstance[slotId], "create instance " + slotId);
            } else if (ir != null && mIccRecords != ir) {
                CatLog.d("CatService", "Reinitialize the Service with SIMRecords");
                mIccRecords = ir;
                CatLog.d("CatService", "read data from sInstSim1");
                String cmd = readCmdFromPreference(sInstance[slotId], context, sInstKey[slotId]);
                if (mIccRecords != null) {
                    mIccRecords.unregisterForRecordsLoaded(sInstance[slotId]);
                }
                mIccRecords = ir;
                mUiccApplication = ca;
                mIccRecords.registerForRecordsLoaded(sInstance[slotId], 20, null);
                handleProactiveCmdFromDB(sInstance[slotId], cmd);
                CatLog.d("CatService", "sr changed reinitialize and return current sInstance");
            } else {
                CatLog.d("CatService", "Return current sInstance");
            }
            sInstance[slotId].registerSATcb();
            return sInstance[slotId];
        }
    }

    private void sendTerminalResponseByCurrentCmd(CatCmdMessage catCmd) {
        if (catCmd == null) {
            CatLog.e(this, "catCmd is null.");
        }
        AppInterface.CommandType cmdType = AppInterface.CommandType.fromInt(catCmd.mCmdDet.typeOfCommand);
        CatLog.d(this, "Send TR for cmd: " + cmdType);
        switch (m219xe796fd46()[cmdType.ordinal()]) {
            case 18:
                this.mCmdIf.handleCallSetupRequestFromSim(false, ResultCode.OK.value(), null);
                break;
            case 19:
            default:
                sendTerminalResponse(catCmd.mCmdDet, ResultCode.UICC_SESSION_TERM_BY_USER, false, 0, null);
                break;
            case 20:
            case 21:
                sendTerminalResponse(catCmd.mCmdDet, ResultCode.OK, false, 0, null);
                break;
        }
    }

    public void dispose() {
        synchronized (sInstanceLock) {
            CatLog.d(this, "Disposing CatService object : " + this.mSlotId);
            mIccRecords.unregisterForRecordsLoaded(this);
            this.mContext.unregisterReceiver(this.CatServiceReceiver);
            this.mContext.unregisterReceiver(this.mStkIdleScreenAvailableReceiver);
            if (!this.mIsProactiveCmdResponsed && this.mCurrentCmd != null) {
                CatLog.d(this, "Send TR for the last pending commands.");
                sendTerminalResponseByCurrentCmd(this.mCurrentCmd);
            }
            broadcastCardStateAndIccRefreshResp(IccCardStatus.CardState.CARDSTATE_ABSENT, null);
            this.mCmdIf.unSetOnCatSessionEnd(this);
            this.mCmdIf.unSetOnCatProactiveCmd(this);
            this.mCmdIf.unSetOnCatEvent(this);
            this.mCmdIf.unSetOnCatCallSetUp(this);
            this.mCmdIf.unSetOnCatCcAlphaNotify(this);
            if (!SystemProperties.get("ro.mtk_bsp_package").equals("1")) {
                this.mCmdIf.unSetOnStkEvdlCall(this);
            }
            this.mCmdIf.unSetOnStkSetupMenuReset(this);
            this.mNeedRegisterAgain = true;
            this.mCmdIf.unregisterForIccRefresh(this);
            if (this.mUiccController != null) {
                this.mUiccController.unregisterForIccChanged(this);
                this.mUiccController = null;
            }
            if (mUiccApplication != null) {
                mUiccApplication.unregisterForReady(this);
            }
            this.mMsgDecoder.dispose();
            this.mMsgDecoder = null;
            this.mHandlerThread.quit();
            this.mHandlerThread = null;
            removeCallbacksAndMessages(null);
            if (this.mBipService != null) {
                this.mBipService.dispose();
            }
            handleDBHandler(this.mSlotId);
            if (sInstance != null) {
                if (SubscriptionManager.isValidSlotId(this.mSlotId)) {
                    sInstance[this.mSlotId] = null;
                } else {
                    CatLog.d(this, "error: invaild slot id: " + this.mSlotId);
                }
            }
        }
    }

    protected void finalize() {
        CatLog.d(this, "Service finalized");
    }

    private void handleRilMsg(RilMessage rilMsg) {
        if (rilMsg == null) {
        }
        switch (rilMsg.mId) {
            case 1:
                handleSessionEnd();
                break;
            case 2:
                if (rilMsg.mId == 2) {
                    this.mIsProactiveCmdResponsed = false;
                }
                try {
                    CommandParams cmdParams = (CommandParams) rilMsg.mData;
                    if (cmdParams != null) {
                        if (rilMsg.mResCode == ResultCode.OK) {
                            this.mSetUpMenuFromMD = rilMsg.mSetUpMenuFromMD;
                            handleCommand(cmdParams, true);
                        } else if (rilMsg.mResCode == ResultCode.PRFRMD_ICON_NOT_DISPLAYED) {
                            this.mSetUpMenuFromMD = rilMsg.mSetUpMenuFromMD;
                            handleCommand(cmdParams, true);
                        } else {
                            CatLog.d("CAT", "SS-handleMessage: invalid proactive command: " + cmdParams.mCmdDet.typeOfCommand);
                            sendTerminalResponse(cmdParams.mCmdDet, rilMsg.mResCode, false, 0, null);
                        }
                    }
                } catch (ClassCastException e) {
                    CatLog.d(this, "Fail to parse proactive command");
                    if (this.mCurrentCmd == null) {
                        return;
                    }
                    sendTerminalResponse(this.mCurrentCmd.mCmdDet, ResultCode.CMD_DATA_NOT_UNDERSTOOD, false, 0, null);
                    return;
                }
                break;
            case 3:
                CommandParams cmdParams2 = (CommandParams) rilMsg.mData;
                if (cmdParams2 != null) {
                    if (rilMsg.mResCode == ResultCode.OK) {
                        handleCommand(cmdParams2, false);
                    } else {
                        CatLog.d(this, "event notify error code: " + rilMsg.mResCode);
                        if (rilMsg.mResCode == ResultCode.PRFRMD_ICON_NOT_DISPLAYED && (cmdParams2.mCmdDet.typeOfCommand == 17 || cmdParams2.mCmdDet.typeOfCommand == 18 || cmdParams2.mCmdDet.typeOfCommand == 19 || cmdParams2.mCmdDet.typeOfCommand == 20)) {
                            CatLog.d(this, "notify user text message even though get icon fail");
                            handleCommand(cmdParams2, false);
                        }
                        if (cmdParams2.mCmdDet.typeOfCommand == 64) {
                            CatLog.d(this, "Open Channel with ResultCode");
                            handleCommand(cmdParams2, false);
                        }
                    }
                }
                break;
            case 5:
                CommandParams cmdParams3 = (CommandParams) rilMsg.mData;
                if (cmdParams3 != null) {
                    handleCommand(cmdParams3, false);
                }
                break;
        }
    }

    private boolean isSupportedSetupEventCommand(CatCmdMessage cmdMsg) {
        boolean flag = true;
        for (int eventVal : cmdMsg.getSetEventList().eventList) {
            CatLog.d(this, "Event: " + eventVal);
            switch (eventVal) {
                case 5:
                case 7:
                    break;
                case 6:
                default:
                    flag = false;
                    break;
            }
        }
        return flag;
    }

    private void handleCommand(CommandParams cmdParams, boolean isProactiveCmd) {
        boolean noAlphaUsrCnf;
        int flightMode;
        int flightMode2;
        CatLog.d(this, cmdParams.getCommandType().name());
        if (isProactiveCmd && this.mUiccController != null) {
            this.mUiccController.addCardLog("ProactiveCommand mSlotId=" + this.mSlotId + " cmdParams=" + cmdParams);
        }
        CatCmdMessage cmdMsg = new CatCmdMessage(cmdParams);
        switch (m219xe796fd46()[cmdParams.getCommandType().ordinal()]) {
            case 1:
                if (1 == ((ActivateParams) cmdParams).mTarget) {
                    CatLog.d(this, "Activate UICC-CLF interface mSlotId: " + this.mSlotId);
                    boolean result = false;
                    try {
                        Class<?> cls = Class.forName("android.nfc.NfcAdapter");
                        Class<?> cls2 = Class.forName("android.nfc.INfcAdapterGsmaExtras");
                        Field field = cls.getField("SIM_1");
                        int sim1 = field.getInt(null);
                        Field field2 = cls.getField("SIM_2");
                        int sim2 = field2.getInt(null);
                        Field field3 = cls.getField("SIM_3");
                        int sim3 = field3.getInt(null);
                        Method getDefaultAdapter = cls.getDeclaredMethod("getDefaultAdapter", Context.class);
                        Object adapter = getDefaultAdapter.invoke(null, this.mContext);
                        if (adapter == null) {
                            CatLog.d(this, "Cannot get NFC Default Adapter !!!");
                            sendTerminalResponse(cmdParams.mCmdDet, ResultCode.TERMINAL_CRNTLY_UNABLE_TO_PROCESS, true, 0, null);
                            return;
                        }
                        Method getNfcAdapterGsmaExtrasInterface = cls.getDeclaredMethod("getNfcAdapterGsmaExtrasInterface", new Class[0]);
                        Object gsmaExtras = getNfcAdapterGsmaExtrasInterface.invoke(adapter, new Object[0]);
                        if (gsmaExtras == null) {
                            CatLog.d(this, "NfcAdapterGsmaExtras service is null !!!");
                            sendTerminalResponse(cmdParams.mCmdDet, ResultCode.TERMINAL_CRNTLY_UNABLE_TO_PROCESS, true, 0, null);
                            return;
                        }
                        Method setNfcSwpActive = cls2.getDeclaredMethod("setNfcSwpActive", Integer.TYPE);
                        if (this.mSlotId == 0) {
                            result = ((Boolean) setNfcSwpActive.invoke(gsmaExtras, Integer.valueOf(sim1))).booleanValue();
                        } else if (1 == this.mSlotId) {
                            result = ((Boolean) setNfcSwpActive.invoke(gsmaExtras, Integer.valueOf(sim2))).booleanValue();
                        } else if (2 == this.mSlotId) {
                            result = ((Boolean) setNfcSwpActive.invoke(gsmaExtras, Integer.valueOf(sim3))).booleanValue();
                        }
                        CatLog.d(this, "setNfcSwpActive result: " + result);
                        if (result) {
                            sendTerminalResponse(cmdParams.mCmdDet, ResultCode.OK, false, 0, null);
                            return;
                        } else {
                            sendTerminalResponse(cmdParams.mCmdDet, ResultCode.TERMINAL_CRNTLY_UNABLE_TO_PROCESS, true, 0, null);
                            return;
                        }
                    } catch (ClassNotFoundException ex) {
                        CatLog.d(this, "Activate UICC-CLF failed !!! " + ex);
                        sendTerminalResponse(cmdParams.mCmdDet, ResultCode.TERMINAL_CRNTLY_UNABLE_TO_PROCESS, true, 0, null);
                        return;
                    } catch (IllegalAccessException ex2) {
                        CatLog.d(this, "Activate UICC-CLF failed !!! " + ex2);
                        sendTerminalResponse(cmdParams.mCmdDet, ResultCode.TERMINAL_CRNTLY_UNABLE_TO_PROCESS, true, 0, null);
                        return;
                    } catch (NoSuchFieldException ex3) {
                        CatLog.d(this, "Activate UICC-CLF failed !!! " + ex3);
                        sendTerminalResponse(cmdParams.mCmdDet, ResultCode.TERMINAL_CRNTLY_UNABLE_TO_PROCESS, true, 0, null);
                        return;
                    } catch (NoSuchMethodException ex4) {
                        CatLog.d(this, "Activate UICC-CLF failed !!! " + ex4);
                        sendTerminalResponse(cmdParams.mCmdDet, ResultCode.TERMINAL_CRNTLY_UNABLE_TO_PROCESS, true, 0, null);
                        return;
                    } catch (InvocationTargetException ex5) {
                        CatLog.d(this, "Activate UICC-CLF failed !!! " + ex5);
                        sendTerminalResponse(cmdParams.mCmdDet, ResultCode.TERMINAL_CRNTLY_UNABLE_TO_PROCESS, true, 0, null);
                        return;
                    }
                }
                CatLog.d(this, "Unsupport target or interface !!!");
                sendTerminalResponse(cmdParams.mCmdDet, ResultCode.BEYOND_TERMINAL_CAPABILITY, false, 0, null);
                return;
            case 2:
            case 7:
            case 10:
            case 13:
                BIPClientParams cmd = (BIPClientParams) cmdParams;
                try {
                    noAlphaUsrCnf = this.mContext.getResources().getBoolean(R.^attr-private.lightRadius);
                } catch (Resources.NotFoundException e) {
                    noAlphaUsrCnf = false;
                }
                if (cmd.mTextMsg.text == null && (cmd.mHasAlphaId || noAlphaUsrCnf)) {
                    CatLog.d(this, "cmd " + cmdParams.getCommandType() + " with null alpha id");
                    if (isProactiveCmd) {
                        sendTerminalResponse(cmdParams.mCmdDet, ResultCode.OK, false, 0, null);
                        return;
                    } else {
                        if (cmdParams.getCommandType() == AppInterface.CommandType.OPEN_CHANNEL) {
                            this.mCmdIf.handleCallSetupRequestFromSim(true, ResultCode.OK.value(), null);
                            return;
                        }
                        return;
                    }
                }
                if (!this.mStkAppInstalled) {
                    CatLog.d(this, "No STK application found.");
                    if (isProactiveCmd) {
                        sendTerminalResponse(cmdParams.mCmdDet, ResultCode.BEYOND_TERMINAL_CAPABILITY, false, 0, null);
                        return;
                    }
                }
                if (isProactiveCmd && (cmdParams.getCommandType() == AppInterface.CommandType.CLOSE_CHANNEL || cmdParams.getCommandType() == AppInterface.CommandType.RECEIVE_DATA || cmdParams.getCommandType() == AppInterface.CommandType.SEND_DATA)) {
                    sendTerminalResponse(cmdParams.mCmdDet, ResultCode.OK, false, 0, null);
                }
                break;
                break;
            case 3:
                if (!SystemProperties.get("ro.mtk_bsp_package").equals("1") && this.mHasCachedDTCmd) {
                    CatLog.d(this, "[CacheDT cache DISPLAY_TEXT");
                    int seqValue = Settings.System.getInt(this.mContext.getContentResolver(), "dialog_sequence_settings", 0);
                    CatLog.d(this, "seqValue in CatService, " + seqValue);
                    if (seqValue != 2) {
                        this.mCachedDisplayTextCmd = cmdMsg;
                        if (seqValue == 0) {
                            Settings.System.putInt(this.mContext.getContentResolver(), "dialog_sequence_settings", 2);
                        }
                        CatLog.d(this, "[CacheDT set current cmd as DISPLAY_TEXT");
                        this.mCurrentCmd = cmdMsg;
                        startTimeOut(46, this.CACHED_DISPLAY_TIMEOUT);
                        return;
                    }
                }
                boolean isAlarmState = isAlarmBoot();
                try {
                    flightMode2 = Settings.Global.getInt(this.mContext.getContentResolver(), "airplane_mode_on");
                } catch (Settings.SettingNotFoundException e2) {
                    CatLog.d(this, "fail to get property from Settings");
                    flightMode2 = 0;
                }
                boolean isFlightMode = flightMode2 != 0;
                CatLog.d(this, "isAlarmState = " + isAlarmState + ", isFlightMode = " + isFlightMode + ", flightMode = " + flightMode2);
                if (isAlarmState && isFlightMode) {
                    sendTerminalResponse(cmdParams.mCmdDet, ResultCode.OK, false, 0, null);
                    return;
                }
                if (checkSetupWizardInstalled()) {
                    sendTerminalResponse(cmdParams.mCmdDet, ResultCode.BACKWARD_MOVE_BY_USER, false, 0, null);
                    return;
                }
                if (this.isIvsrBootUp) {
                    CatLog.d(this, "[IVSR send TR directly");
                    sendTerminalResponse(cmdParams.mCmdDet, ResultCode.BACKWARD_MOVE_BY_USER, false, 0, null);
                    return;
                } else if (this.isDisplayTextDisabled) {
                    CatLog.d(this, "[Sim Recovery send TR directly");
                    sendTerminalResponse(cmdParams.mCmdDet, ResultCode.BACKWARD_MOVE_BY_USER, false, 0, null);
                    return;
                } else if (SystemProperties.get(DISPLAY_TEXT_DISABLE_PROPERTY).equals("1")) {
                    CatLog.d(this, "Filter DISPLAY_TEXT command.");
                    sendTerminalResponse(cmdParams.mCmdDet, ResultCode.BACKWARD_MOVE_BY_USER, false, 0, null);
                    return;
                }
                break;
            case 4:
            case 5:
                if (!((this.simState == null || this.simState.length() == 0 || "READY".equals(this.simState) || "IMSI".equals(this.simState)) ? true : "LOADED".equals(this.simState))) {
                    sendTerminalResponse(cmdParams.mCmdDet, ResultCode.TERMINAL_CRNTLY_UNABLE_TO_PROCESS, false, 0, null);
                    return;
                }
                break;
            case 6:
                if (((LaunchBrowserParams) cmdParams).mConfirmMsg.text != null && ((LaunchBrowserParams) cmdParams).mConfirmMsg.text.equals(STK_DEFAULT)) {
                    CharSequence message = this.mContext.getText(R.string.indeterminate_progress_09);
                    ((LaunchBrowserParams) cmdParams).mConfirmMsg.text = message.toString();
                }
                break;
            case 8:
                this.mIsProactiveCmdResponsed = true;
                break;
            case 9:
                if (cmdParams.mCmdDet.commandQualifier == 3) {
                    Calendar cal = Calendar.getInstance();
                    int temp = cal.get(1) - 2000;
                    int hibyte = temp / 10;
                    int lobyte = (temp % 10) << 4;
                    int temp2 = cal.get(2) + 1;
                    int hibyte2 = temp2 / 10;
                    int lobyte2 = (temp2 % 10) << 4;
                    int temp3 = cal.get(5);
                    int hibyte3 = temp3 / 10;
                    int lobyte3 = (temp3 % 10) << 4;
                    int temp4 = cal.get(11);
                    int hibyte4 = temp4 / 10;
                    int lobyte4 = (temp4 % 10) << 4;
                    int temp5 = cal.get(12);
                    int hibyte5 = temp5 / 10;
                    int lobyte5 = (temp5 % 10) << 4;
                    int temp6 = cal.get(13);
                    int hibyte6 = temp6 / 10;
                    int lobyte6 = (temp6 % 10) << 4;
                    int temp7 = cal.get(15) / 900000;
                    int hibyte7 = temp7 / 10;
                    int lobyte7 = (temp7 % 10) << 4;
                    byte[] datetime = {(byte) (lobyte | hibyte), (byte) (lobyte2 | hibyte2), (byte) (lobyte3 | hibyte3), (byte) (lobyte4 | hibyte4), (byte) (lobyte5 | hibyte5), (byte) (lobyte6 | hibyte6), (byte) (lobyte7 | hibyte7)};
                    ResponseData resp = new ProvideLocalInformationResponseData(datetime[0], datetime[1], datetime[2], datetime[3], datetime[4], datetime[5], datetime[6]);
                    sendTerminalResponse(cmdParams.mCmdDet, ResultCode.OK, false, 0, resp);
                    return;
                }
                if (cmdParams.mCmdDet.commandQualifier == 4) {
                    Locale locale = Locale.getDefault();
                    byte[] lang = {(byte) locale.getLanguage().charAt(0), (byte) locale.getLanguage().charAt(1)};
                    ResponseData resp2 = new ProvideLocalInformationResponseData(lang);
                    sendTerminalResponse(cmdParams.mCmdDet, ResultCode.OK, false, 0, resp2);
                    return;
                }
                if (cmdParams.mCmdDet.commandQualifier == 10) {
                    int batterystate = getBatteryState(this.mContext);
                    ResponseData resp3 = new ProvideLocalInformationResponseData(batterystate);
                    sendTerminalResponse(cmdParams.mCmdDet, ResultCode.OK, false, 0, resp3);
                    return;
                }
                return;
            case 11:
                this.mIsProactiveCmdResponsed = true;
                cmdParams.mCmdDet.typeOfCommand = AppInterface.CommandType.SET_UP_IDLE_MODE_TEXT.value();
                if (cmdParams.mCmdDet.commandQualifier == 4) {
                    CatLog.d(this, "remove event list because of SIM Refresh type 4");
                    this.mEventList = null;
                } else {
                    CatLog.d(this, "Do not to remove event list because SIM Refresh type not 4");
                }
                break;
            case 12:
                boolean isAlarmState2 = isAlarmBoot();
                try {
                    flightMode = Settings.Global.getInt(this.mContext.getContentResolver(), "airplane_mode_on");
                } catch (Settings.SettingNotFoundException e3) {
                    CatLog.d(this, "fail to get property from Settings");
                    flightMode = 0;
                }
                boolean isFlightMode2 = flightMode != 0;
                CatLog.d(this, "isAlarmState = " + isAlarmState2 + ", isFlightMode = " + isFlightMode2 + ", flightMode = " + flightMode);
                if (isAlarmState2 && isFlightMode2) {
                    sendTerminalResponse(cmdParams.mCmdDet, ResultCode.UICC_SESSION_TERM_BY_USER, false, 0, null);
                    return;
                }
                break;
            case 14:
            case 15:
            case 16:
            case 17:
                this.mIsProactiveCmdResponsed = true;
                if (((DisplayTextParams) cmdParams).mTextMsg.text != null && ((DisplayTextParams) cmdParams).mTextMsg.text.equals(STK_DEFAULT)) {
                    CharSequence message2 = this.mContext.getText(R.string.indeterminate_progress_08);
                    ((DisplayTextParams) cmdParams).mTextMsg.text = message2.toString();
                }
                break;
            case 18:
                if (((CallSetupParams) cmdParams).mConfirmMsg.text != null && ((CallSetupParams) cmdParams).mConfirmMsg.text.equals(STK_DEFAULT)) {
                    CharSequence message3 = this.mContext.getText(R.string.indeterminate_progress_10);
                    ((CallSetupParams) cmdParams).mConfirmMsg.text = message3.toString();
                }
                break;
            case 19:
                this.mBipService.setSetupEventList(cmdMsg);
                this.mIsProactiveCmdResponsed = true;
                this.mEventList = ((SetupEventListParams) cmdParams).eventList;
                return;
            case 20:
                ResultCode resultCode = cmdParams.mLoadIconFailed ? ResultCode.PRFRMD_ICON_NOT_DISPLAYED : ResultCode.OK;
                sendTerminalResponse(cmdParams.mCmdDet, resultCode, false, 0, null);
                break;
            case 21:
                if (removeMenu(cmdMsg.getMenu())) {
                    this.mMenuCmd = null;
                } else {
                    this.mMenuCmd = cmdMsg;
                }
                CatLog.d("CAT", "mSetUpMenuFromMD: " + this.mSetUpMenuFromMD);
                if (cmdMsg.getMenu() != null) {
                    cmdMsg.getMenu().setSetUpMenuFlag(this.mSetUpMenuFromMD ? 1 : 0);
                }
                if (!this.mSetUpMenuFromMD) {
                    this.mIsProactiveCmdResponsed = true;
                } else {
                    this.mSetUpMenuFromMD = false;
                    ResultCode resultCode2 = cmdParams.mLoadIconFailed ? ResultCode.PRFRMD_ICON_NOT_DISPLAYED : ResultCode.OK;
                    sendTerminalResponse(cmdParams.mCmdDet, resultCode2, false, 0, null);
                }
                break;
            default:
                CatLog.d(this, "Unsupported command");
                return;
        }
        this.mCurrentCmd = cmdMsg;
        broadcastCatCmdIntent(cmdMsg);
    }

    private void broadcastCatCmdIntent(CatCmdMessage cmdMsg) {
        Intent intent = new Intent(AppInterface.CAT_CMD_ACTION);
        intent.addFlags(67108864);
        intent.putExtra("STK CMD", cmdMsg);
        intent.putExtra("SLOT_ID", this.mSlotId);
        CatLog.d(this, "Sending CmdMsg: " + cmdMsg + " on slotid:" + this.mSlotId);
        this.mContext.sendBroadcast(intent, AppInterface.STK_PERMISSION);
    }

    private void handleSessionEnd() {
        CatLog.d(this, "SESSION END on " + this.mSlotId);
        this.mCurrentCmd = this.mMenuCmd;
        Intent intent = new Intent(AppInterface.CAT_SESSION_END_ACTION);
        intent.putExtra("SLOT_ID", this.mSlotId);
        this.mContext.sendBroadcast(intent, AppInterface.STK_PERMISSION);
    }

    private void sendTerminalResponse(CommandDetails cmdDet, ResultCode resultCode, boolean includeAdditionalInfo, int additionalInfo, ResponseData resp) {
        if (cmdDet == null) {
            CatLog.e(this, "SS-sendTR: cmdDet is null");
            return;
        }
        CatLog.d(this, "SS-sendTR: command type is " + cmdDet.typeOfCommand);
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        Input cmdInput = null;
        if (this.mCurrentCmd != null) {
            cmdInput = this.mCurrentCmd.geInput();
        }
        this.mIsProactiveCmdResponsed = true;
        int tag = ComprehensionTlvTag.COMMAND_DETAILS.value();
        if (cmdDet.compRequired) {
            tag |= 128;
        }
        buf.write(tag);
        buf.write(3);
        buf.write(cmdDet.commandNumber);
        buf.write(cmdDet.typeOfCommand);
        buf.write(cmdDet.commandQualifier);
        buf.write(ComprehensionTlvTag.DEVICE_IDENTITIES.value() | 128);
        buf.write(2);
        buf.write(130);
        buf.write(129);
        int tag2 = ComprehensionTlvTag.RESULT.value();
        if (cmdDet.compRequired) {
            tag2 |= 128;
        }
        buf.write(tag2);
        int length = includeAdditionalInfo ? 2 : 1;
        buf.write(length);
        buf.write(resultCode.value());
        if (includeAdditionalInfo) {
            buf.write(additionalInfo);
        }
        if (resp != null) {
            CatLog.d(this, "SS-sendTR: write response data into TR");
            resp.format(buf);
        } else {
            encodeOptionalTags(cmdDet, resultCode, cmdInput, buf);
        }
        byte[] rawData = buf.toByteArray();
        String hexString = IccUtils.bytesToHexString(rawData);
        CatLog.d(this, "TERMINAL RESPONSE: " + hexString);
        this.mCmdIf.sendTerminalResponse(hexString, null);
    }

    private void encodeOptionalTags(CommandDetails cmdDet, ResultCode resultCode, Input cmdInput, ByteArrayOutputStream buf) {
        AppInterface.CommandType cmdType = AppInterface.CommandType.fromInt(cmdDet.typeOfCommand);
        if (cmdType != null) {
            switch (m219xe796fd46()[cmdType.ordinal()]) {
                case 4:
                    if (resultCode.value() == ResultCode.NO_RESPONSE_FROM_USER.value() && cmdInput != null && cmdInput.duration != null) {
                        getInKeyResponse(buf, cmdInput);
                        break;
                    }
                    break;
                case 9:
                    if (cmdDet.commandQualifier == 4 && resultCode.value() == ResultCode.OK.value()) {
                        getPliResponse(buf);
                        break;
                    }
                    break;
                default:
                    CatLog.d(this, "encodeOptionalTags() Unsupported Cmd details=" + cmdDet);
                    break;
            }
            return;
        }
        CatLog.d(this, "encodeOptionalTags() bad Cmd details=" + cmdDet);
    }

    private void getInKeyResponse(ByteArrayOutputStream buf, Input cmdInput) {
        int tag = ComprehensionTlvTag.DURATION.value();
        buf.write(tag);
        buf.write(2);
        Duration.TimeUnit timeUnit = cmdInput.duration.timeUnit;
        buf.write(Duration.TimeUnit.SECOND.value());
        buf.write(cmdInput.duration.timeInterval);
    }

    private void getPliResponse(ByteArrayOutputStream buf) {
        String lang = Locale.getDefault().getLanguage();
        if (lang == null) {
            return;
        }
        int tag = ComprehensionTlvTag.LANGUAGE.value();
        buf.write(tag);
        ResponseData.writeLength(buf, lang.length());
        buf.write(lang.getBytes(), 0, lang.length());
    }

    private void sendMenuSelection(int menuId, boolean helpRequired) {
        CatLog.d("CatService", "sendMenuSelection SET_UP_MENU");
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        buf.write(211);
        buf.write(0);
        int tag = ComprehensionTlvTag.DEVICE_IDENTITIES.value() | 128;
        buf.write(tag);
        buf.write(2);
        buf.write(1);
        buf.write(129);
        int tag2 = ComprehensionTlvTag.ITEM_ID.value() | 128;
        buf.write(tag2);
        buf.write(1);
        buf.write(menuId);
        if (helpRequired) {
            int tag3 = ComprehensionTlvTag.HELP_REQUEST.value();
            buf.write(tag3);
            buf.write(0);
        }
        byte[] rawData = buf.toByteArray();
        int len = rawData.length - 2;
        rawData[1] = (byte) len;
        String hexString = IccUtils.bytesToHexString(rawData);
        CatLog.d("CatService", "sendMenuSelection before");
        this.mCmdIf.sendEnvelope(hexString, null);
        CatLog.d("CatService", "sendMenuSelection after");
        cancelTimeOut(15);
        CatLog.d(this, "[Reset Disable Display Text flag because MENU_SELECTION");
        this.isDisplayTextDisabled = false;
        if (!SystemProperties.get("persist.sys.esn_track_switch").equals("1")) {
            return;
        }
        this.mContext.sendBroadcast(new Intent(mEsnTrackUtkMenuSelect).putExtra("slot", this.mSlotId));
    }

    private void writeCallDisConnED(ByteArrayOutputStream buffer) {
        if (SystemProperties.get("ro.mtk_bsp_package").equals("1")) {
            return;
        }
        EventDownloadCallInfo evdlcallInfo = this.mEventDownloadCallDisConnInfo.removeFirst();
        if (evdlcallInfo == null) {
            CatLog.d(this, "SS-eventDownload:X null evdlcallInfo");
            return;
        }
        CatLog.d(this, "SS-eventDownload: event is CALL_DISCONNECTED.[" + evdlcallInfo.mIsFarEnd + "," + evdlcallInfo.mTi + "," + evdlcallInfo.mCauseLen + "," + evdlcallInfo.mCause + "]");
        buffer.write(1 == evdlcallInfo.mIsFarEnd ? 131 : 130);
        buffer.write(129);
        int tag = ComprehensionTlvTag.TRANSACTION_ID.value();
        buffer.write(tag);
        buffer.write(1);
        buffer.write(evdlcallInfo.mTi);
        if (evdlcallInfo.mCauseLen == 0) {
            int tag2 = ComprehensionTlvTag.CAUSE.value() | 128;
            buffer.write(tag2);
            buffer.write(0);
        } else {
            if (255 == evdlcallInfo.mCauseLen) {
                CatLog.d(this, "SS-eventDownload:no cause value");
                return;
            }
            int tag3 = ComprehensionTlvTag.CAUSE.value() | 128;
            buffer.write(tag3);
            buffer.write(evdlcallInfo.mCauseLen);
            for (int i = evdlcallInfo.mCauseLen - 1; i >= 0; i--) {
                int temp = (evdlcallInfo.mCause >> (i * 8)) & 255;
                CatLog.d(this, "SS-eventDownload:cause:" + Integer.toHexString(temp));
                buffer.write((evdlcallInfo.mCause >> (i * 8)) & 255);
            }
        }
    }

    private void eventDownload(int event, int sourceId, int destinationId, byte[] additionalInfo, boolean oneShot) {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        if (this.mEventList == null || this.mEventList.length == 0) {
            CatLog.d(this, "SS-eventDownload: event list null");
            return;
        }
        CatLog.d(this, "SS-eventDownload: event list length:" + this.mEventList.length);
        int index = 0;
        do {
            if (index < this.mEventList.length) {
                CatLog.d(this, "SS-eventDownload: event [" + ((int) this.mEventList[index]) + "]");
                if (this.mEventList[index] == event) {
                    if (event == 5) {
                        CatLog.d(this, "SS-eventDownload: event is IDLE_SCREEN_AVAILABLE");
                        CatLog.d(this, "SS-eventDownload: sent intent with idle = false");
                        Intent intent = new Intent(IDLE_SCREEN_INTENT_NAME);
                        intent.putExtra(IDLE_SCREEN_ENABLE_KEY, false);
                        this.mContext.sendBroadcast(intent);
                    } else if (event == 4) {
                        CatLog.d(this, "SS-eventDownload: event is USER_ACTIVITY");
                        Intent intent2 = new Intent(USER_ACTIVITY_INTENT_NAME);
                        intent2.putExtra(USER_ACTIVITY_ENABLE_KEY, false);
                        this.mContext.sendBroadcast(intent2);
                    } else if (event == 1) {
                        CatLog.d(this, "SS-eventDownload: event is CALL_CONNECTED");
                    } else if (event == 2) {
                        CatLog.d(this, "SS-eventDownload: event is CALL_DISCONNECTED");
                    }
                    if (oneShot) {
                        this.mEventList[index] = 0;
                    }
                } else {
                    index++;
                }
            }
            buf.write(BerTlv.BER_EVENT_DOWNLOAD_TAG);
            buf.write(0);
            int tag = ComprehensionTlvTag.EVENT_LIST.value() | 128;
            buf.write(tag);
            buf.write(1);
            buf.write(event);
            int tag2 = ComprehensionTlvTag.DEVICE_IDENTITIES.value() | 128;
            buf.write(tag2);
            buf.write(2);
            if (SystemProperties.get("ro.mtk_bsp_package").equals("1")) {
                buf.write(sourceId);
                buf.write(destinationId);
            } else if (event == 2) {
                if (this.mEventDownloadCallDisConnInfo.size() <= 0) {
                    CatLog.d(this, "SS-eventDownload: Wait 2s for modem CALL_DISCONNECTED");
                    Message msg1 = obtainMessage(23);
                    if (this.mEvdlCallObj > 65535) {
                        this.mEvdlCallObj = 0;
                    }
                    int i = this.mEvdlCallObj;
                    this.mEvdlCallObj = i + 1;
                    msg1.obj = new Integer(i);
                    this.mEvdlCallDisConnObjQ.add((Integer) msg1.obj);
                    this.mTimeoutHandler.sendMessageDelayed(msg1, this.MODEM_EVDL_TIMEOUT);
                    this.mNumEventDownloadCallDisConn++;
                    CatLog.d(this, "SS-eventDownload: mNumEventDownloadCallDisConn ++.[" + this.mNumEventDownloadCallDisConn + "]");
                    return;
                }
                if (this.mIsAllCallDisConn) {
                    while (this.mEventDownloadCallDisConnInfo.size() > 0) {
                        writeCallDisConnED(buf);
                    }
                } else {
                    writeCallDisConnED(buf);
                }
            } else if (event != 1) {
                buf.write(sourceId);
                buf.write(destinationId);
            } else {
                if (this.mEventDownloadCallConnInfo.size() <= 0) {
                    Message msg12 = obtainMessage(22);
                    if (this.mEvdlCallObj > 65535) {
                        this.mEvdlCallObj = 0;
                    }
                    int i2 = this.mEvdlCallObj;
                    this.mEvdlCallObj = i2 + 1;
                    msg12.obj = new Integer(i2);
                    this.mEvdlCallConnObjQ.add((Integer) msg12.obj);
                    this.mTimeoutHandler.sendMessageDelayed(msg12, this.MODEM_EVDL_TIMEOUT);
                    this.mNumEventDownloadCallConn++;
                    CatLog.d(this, "SS-eventDownload: mNumEventDownloadCallConn ++.[" + this.mNumEventDownloadCallConn + "]");
                    return;
                }
                EventDownloadCallInfo evdlcallInfo = this.mEventDownloadCallConnInfo.removeFirst();
                if (evdlcallInfo != null) {
                    CatLog.d(this, "SS-eventDownload: event is CALL_CONNECTED.[" + evdlcallInfo.mIsMTCall + "," + evdlcallInfo.mTi + "]");
                    buf.write(1 == evdlcallInfo.mIsMTCall ? 130 : 131);
                    buf.write(129);
                    int tag3 = ComprehensionTlvTag.TRANSACTION_ID.value();
                    buf.write(tag3);
                    buf.write(1);
                    buf.write(evdlcallInfo.mTi);
                } else {
                    CatLog.d(this, "SS-eventDownload:O null evdlcallInfo");
                }
            }
            if (additionalInfo != null) {
                for (byte b : additionalInfo) {
                    buf.write(b);
                }
            }
            byte[] rawData = buf.toByteArray();
            int len = rawData.length - 2;
            rawData[1] = (byte) len;
            String hexString = IccUtils.bytesToHexString(rawData);
            this.mCmdIf.sendEnvelope(hexString, null);
            return;
        } while (index != this.mEventList.length);
    }

    private void registerSATcb() {
        CatLog.d("CatService", "registerSATcb, mNeedRegisterAgain: " + this.mNeedRegisterAgain);
        if (!this.mNeedRegisterAgain) {
            return;
        }
        this.mCmdIf.setOnCatSessionEnd(this, 1, null);
        this.mCmdIf.setOnCatEvent(this, 3, null);
        this.mCmdIf.setOnCatCallSetUp(this, 4, null);
        if (!SystemProperties.get("ro.mtk_bsp_package").equals("1")) {
            this.mCmdIf.setOnStkEvdlCall(this, 21, null);
        }
        this.mCmdIf.setOnStkSetupMenuReset(this, 24, null);
        this.mCmdIf.setOnCatCcAlphaNotify(this, 9, null);
        this.mNeedRegisterAgain = false;
    }

    public static CatService getInstance(CommandsInterface ci, Context context, UiccCard ic) {
        CatLog.d("CatService", "call getInstance 2");
        int sim_id = 0;
        if (ic != null) {
            sim_id = ic.getPhoneId();
            CatLog.d("CatService", "get SIM id from UiccCard. sim id: " + sim_id);
        }
        return getInstance(ci, context, ic, sim_id);
    }

    public static AppInterface getInstance() {
        CatLog.d("CatService", "call getInstance 4");
        return getInstance(null, null, null, 0);
    }

    public static AppInterface getInstance(int slotId) {
        CatLog.d("CatService", "call getInstance 3");
        return getInstance(null, null, null, slotId);
    }

    private static void handleProactiveCmdFromDB(CatService inst, String data) {
        if (!SystemProperties.get("ro.mtk_bsp_package").equals("1")) {
            if (data == null) {
                CatLog.d("CatService", "handleProactiveCmdFromDB: cmd = null");
                return;
            }
            inst.default_send_setupmenu_tr = false;
            CatLog.d("CatService", " handleProactiveCmdFromDB: cmd = " + data + " from: " + inst);
            RilMessage rilMsg = new RilMessage(2, data);
            inst.mMsgDecoder.sendStartDecodingMessageParams(rilMsg);
            CatLog.d("CatService", "handleProactiveCmdFromDB: over");
            return;
        }
        CatLog.d("CatService", "BSP package does not support db cache.");
    }

    private boolean isSetUpMenuCmd(String cmd) {
        if (cmd == null) {
            return false;
        }
        try {
            if (cmd.charAt(2) == '8' && cmd.charAt(3) == '1') {
                if (cmd.charAt(12) != '2' || cmd.charAt(13) != '5') {
                    return false;
                }
                return true;
            }
            if (cmd.charAt(10) != '2' || cmd.charAt(11) != '5') {
                return false;
            }
            return true;
        } catch (IndexOutOfBoundsException e) {
            CatLog.d(this, "IndexOutOfBoundsException isSetUpMenuCmd: " + cmd);
            e.printStackTrace();
            return false;
        }
    }

    public static boolean getSaveNewSetUpMenuFlag(int sim_id) {
        if (sInstance == null || sInstance[sim_id] == null) {
            return false;
        }
        boolean result = sInstance[sim_id].mSaveNewSetUpMenu;
        CatLog.d("CatService", sim_id + " , mSaveNewSetUpMenu: " + result);
        return result;
    }

    @Override
    public void handleMessage(Message msg) {
        AsyncResult ar;
        AsyncResult ar2;
        CatLog.d(this, "handleMessage[" + msg.what + "]");
        switch (msg.what) {
            case 1:
            case 2:
            case 3:
            case 5:
                CatLog.d(this, "ril message arrived, slotid:" + this.mSlotId);
                String data = null;
                boolean flag = false;
                if (msg.obj != null) {
                    AsyncResult ar3 = (AsyncResult) msg.obj;
                    if (this.mMsgDecoder == null) {
                        CatLog.e(this, "mMsgDecoder == null, return.");
                        return;
                    }
                    if (ar3 != null && ar3.result != null) {
                        try {
                            data = (String) ar3.result;
                            if (SystemProperties.get("ro.mtk_bsp_package").equals("1")) {
                                CatLog.d(this, "BSP package always set SET_UP_MENU from MD.");
                                flag = true;
                            } else {
                                boolean isValid = isSetUpMenuCmd(data);
                                if (isValid && this == sInstance[this.mSlotId]) {
                                    CatLog.d(this, "ril message arrived : save data to db " + this.mSlotId);
                                    saveCmdToPreference(this.mContext, sInstKey[this.mSlotId], data);
                                    this.mSaveNewSetUpMenu = true;
                                    flag = true;
                                }
                            }
                        } catch (ClassCastException e) {
                            return;
                        }
                    }
                    break;
                }
                RilMessage rilMsg = new RilMessage(msg.what, data);
                rilMsg.setSetUpMenuFromMD(flag);
                this.mMsgDecoder.sendStartDecodingMessageParams(rilMsg);
                return;
            case 4:
                this.mMsgDecoder.sendStartDecodingMessageParams(new RilMessage(msg.what, null));
                return;
            case 6:
                handleCmdResponse((CatResponseMessage) msg.obj);
                return;
            case 7:
                CatLog.d(this, "SIM Ready");
                if (SystemProperties.get("ro.mtk_bsp_package").equals("1")) {
                    this.mCmdIf.setStkEvdlCallByAP(1, null);
                    return;
                } else {
                    this.mCmdIf.setStkEvdlCallByAP(0, null);
                    return;
                }
            case 8:
                CatLog.w(this, "MSG_ID_ICC_CHANGED");
                updateIccAvailability();
                return;
            case 9:
                CatLog.d(this, "RIL event Call Ctrl.");
                if (msg.obj == null || (ar = (AsyncResult) msg.obj) == null || ar.result == null) {
                    return;
                }
                String[] callCtrlInfo = (String[]) ar.result;
                try {
                    CatLog.d(this, "callCtrlInfo.length: " + callCtrlInfo.length + "," + callCtrlInfo[0] + "," + callCtrlInfo[1] + "," + callCtrlInfo[2]);
                    if (callCtrlInfo[1] == null || callCtrlInfo[1].length() <= 0) {
                        CatLog.d(this, "Null CC alpha id.");
                    } else {
                        byte[] rawData = IccUtils.hexStringToBytes(callCtrlInfo[1]);
                        try {
                            String alphaId = IccUtils.adnStringFieldToString(rawData, 0, rawData.length);
                            CatLog.d(this, "CC Alpha msg: " + alphaId + ", sim id: " + this.mSlotId);
                            TextMessage textMessage = new TextMessage();
                            CommandDetails cmdDet = new CommandDetails();
                            cmdDet.typeOfCommand = AppInterface.CommandType.CALLCTRL_RSP_MSG.value();
                            textMessage.text = alphaId;
                            CallCtrlBySimParams cmdParams = new CallCtrlBySimParams(cmdDet, textMessage, Integer.parseInt(callCtrlInfo[0]), callCtrlInfo[2]);
                            CatCmdMessage cmdMsg = new CatCmdMessage(cmdParams);
                            broadcastCatCmdIntent(cmdMsg);
                        } catch (IndexOutOfBoundsException e2) {
                            CatLog.d(this, "IndexOutOfBoundsException adnStringFieldToString");
                        }
                    }
                    return;
                } catch (RuntimeException e3) {
                    CatLog.d(this, "CC message drop");
                    return;
                }
            case 10:
                handleRilMsg((RilMessage) msg.obj);
                return;
            case 11:
                handleEventDownload((CatResponseMessage) msg.obj);
                return;
            case 12:
                handleDBHandler(msg.arg1);
                return;
            case 13:
                CatLog.d(this, "MSG_ID_LAUNCH_DB_SETUP_MENU");
                String strCmd = readCmdFromPreference(sInstance[this.mSlotId], this.mContext, sInstKey[this.mSlotId]);
                if (sInstance[this.mSlotId] == null || strCmd == null) {
                    return;
                }
                handleProactiveCmdFromDB(sInstance[this.mSlotId], strCmd);
                return;
            case 14:
                CatLog.d(this, "[IVSR cancel IVSR flag");
                this.isIvsrBootUp = false;
                return;
            case 15:
            case 16:
            case 17:
            case 18:
            case 19:
            case 22:
            case 23:
            case 25:
            case 26:
            case CallFailCause.DESTINATION_OUT_OF_ORDER:
            case CallFailCause.INVALID_NUMBER_FORMAT:
            case CallFailCause.FACILITY_REJECTED:
            default:
                throw new AssertionError("Unrecognized CAT command: " + msg.what);
            case 20:
                return;
            case 21:
                if (SystemProperties.get("ro.mtk_bsp_package").equals("1")) {
                    return;
                }
                CatLog.d(this, "RIL event download for call.");
                if (msg.obj == null || (ar2 = (AsyncResult) msg.obj) == null || ar2.result == null) {
                    return;
                }
                int[] evdlCalldata = (int[]) ar2.result;
                EventDownloadCallInfo eventDownloadCallInfo = new EventDownloadCallInfo(evdlCalldata[0], evdlCalldata[1], evdlCalldata[2], evdlCalldata[3], evdlCalldata[4], evdlCalldata[5]);
                if (255 > eventDownloadCallInfo.mCauseLen) {
                    eventDownloadCallInfo.mCauseLen >>= 1;
                } else {
                    eventDownloadCallInfo.mCauseLen = 255;
                }
                if (evdlCalldata[0] == 0) {
                    this.mEventDownloadCallConnInfo.add(eventDownloadCallInfo);
                    if (this.mNumEventDownloadCallConn > 0) {
                        this.mNumEventDownloadCallConn--;
                        removeMessages(22, this.mEvdlCallConnObjQ.removeFirst());
                        CatLog.d(this, "mNumEventDownloadCallConn --.[" + this.mNumEventDownloadCallConn + "]");
                        eventDownload(1, 0, 0, null, false);
                    }
                } else {
                    this.mEventDownloadCallDisConnInfo.add(eventDownloadCallInfo);
                    if (this.mNumEventDownloadCallDisConn > 0) {
                        this.mNumEventDownloadCallDisConn--;
                        removeMessages(23, this.mEvdlCallDisConnObjQ.removeFirst());
                        CatLog.d(this, "mNumEventDownloadCallDisConn --.[" + this.mNumEventDownloadCallDisConn + "]");
                        eventDownload(2, 0, 0, null, false);
                    }
                }
                CatLog.d(this, "Evdl data:" + evdlCalldata[0] + "," + evdlCalldata[1] + "," + evdlCalldata[2] + "," + evdlCalldata[3] + "," + evdlCalldata[4]);
                return;
            case 24:
                CatLog.d(this, "SETUP_MENU_RESET : Setup menu reset.");
                AsyncResult ar4 = (AsyncResult) msg.obj;
                if (ar4 == null || ar4.exception != null) {
                    CatLog.d(this, "SETUP_MENU_RESET : AsyncResult null.");
                    return;
                } else {
                    this.mSaveNewSetUpMenu = false;
                    return;
                }
            case 30:
                if (msg.obj == null) {
                    CatLog.d(this, "IccRefresh Message is null");
                    return;
                }
                AsyncResult ar5 = (AsyncResult) msg.obj;
                if (ar5 == null || ar5.result == null) {
                    CatLog.d(this, "Icc REFRESH with exception: " + ar5.exception);
                    return;
                } else {
                    broadcastCardStateAndIccRefreshResp(IccCardStatus.CardState.CARDSTATE_PRESENT, (IccRefreshResponse) ar5.result);
                    return;
                }
        }
    }

    private void broadcastCardStateAndIccRefreshResp(IccCardStatus.CardState cardState, IccRefreshResponse iccRefreshState) {
        Intent intent = new Intent(AppInterface.CAT_ICC_STATUS_CHANGE);
        boolean cardPresent = cardState == IccCardStatus.CardState.CARDSTATE_PRESENT;
        if (iccRefreshState != null) {
            intent.putExtra(AppInterface.REFRESH_RESULT, iccRefreshState.refreshResult);
            CatLog.d(this, "Sending IccResult with Result: " + iccRefreshState.refreshResult);
        }
        intent.putExtra(AppInterface.CARD_STATUS, cardPresent);
        CatLog.d(this, "Sending Card Status: " + cardState + " cardPresent: " + cardPresent);
        intent.putExtra("SLOT_ID", this.mSlotId);
        this.mContext.sendBroadcast(intent, AppInterface.STK_PERMISSION);
    }

    private void broadcastAlphaMessage(String alphaString) {
        CatLog.d(this, "Broadcasting CAT Alpha message from card: " + alphaString);
        Intent intent = new Intent(AppInterface.CAT_ALPHA_NOTIFY_ACTION);
        intent.addFlags(268435456);
        intent.putExtra(AppInterface.ALPHA_STRING, alphaString);
        intent.putExtra("SLOT_ID", this.mSlotId);
        this.mContext.sendBroadcast(intent, AppInterface.STK_PERMISSION);
    }

    @Override
    public synchronized void onCmdResponse(CatResponseMessage resMsg) {
        if (resMsg == null) {
            return;
        }
        Message msg = obtainMessage(6, resMsg);
        msg.sendToTarget();
    }

    @Override
    public synchronized void onEventDownload(CatResponseMessage resMsg) {
        if (resMsg == null) {
            return;
        }
        Message msg = obtainMessage(11, resMsg);
        msg.sendToTarget();
    }

    @Override
    public synchronized void onDBHandler(int sim_id) {
        Message msg = obtainMessage(12, sim_id, 0);
        msg.sendToTarget();
    }

    @Override
    public synchronized void onLaunchCachedSetupMenu() {
        Message msg = obtainMessage(13, this.mSlotId, 0);
        msg.sendToTarget();
    }

    private boolean validateResponse(CatResponseMessage resMsg) {
        if (resMsg.mCmdDet.typeOfCommand == AppInterface.CommandType.SET_UP_EVENT_LIST.value() || resMsg.mCmdDet.typeOfCommand == AppInterface.CommandType.SET_UP_MENU.value()) {
            CatLog.d(this, "CmdType: " + resMsg.mCmdDet.typeOfCommand);
            return true;
        }
        if (this.mCurrentCmd == null) {
            return false;
        }
        boolean validResponse = resMsg.mCmdDet.compareTo(this.mCurrentCmd.mCmdDet);
        CatLog.d(this, "isResponse for last valid cmd: " + validResponse);
        return validResponse;
    }

    private boolean removeMenu(Menu menu) {
        try {
            if (menu.items.size() == 1) {
                if (menu.items.get(0) == null) {
                    return true;
                }
            }
            return false;
        } catch (NullPointerException e) {
            CatLog.d(this, "Unable to get Menu's items size");
            return true;
        }
    }

    private void handleEventDownload(CatResponseMessage resMsg) {
        eventDownload(resMsg.mEvent, resMsg.mSourceId, resMsg.mDestinationId, resMsg.mAdditionalInfo, resMsg.mOneShot);
    }

    private void handleDBHandler(int sim_id) {
        CatLog.d(this, "handleDBHandler, sim_id: " + sim_id);
        saveCmdToPreference(this.mContext, sInstKey[sim_id], null);
    }

    private void handleCmdResponse(com.android.internal.telephony.cat.CatResponseMessage r18) {
        if (!validateResponse(r18)) {
            return;
        } else {
            r6 = null;
            r14 = false;
            r2 = r18.getCmdDetails();
            r16 = com.android.internal.telephony.cat.AppInterface.CommandType.fromInt(r2.typeOfCommand);
            switch (m220getcomandroidinternaltelephonycatResultCodeSwitchesValues()[r18.mResCode.ordinal()]) {
                case 1:
                case 2:
                case 6:
                case 18:
                    switch (m219xe796fd46()[r16.ordinal()]) {
                        case 3:
                            if (!android.os.SystemProperties.get("ro.mtk_bsp_package").equals("1") && r17.mHasCachedDTCmd) {
                                resetPowerOnSequenceFlag();
                            }
                            r6 = null;
                            r3 = r18.mResCode;
                            r4 = r18.mIncludeAdditionalInfo;
                            if (!r18.mIncludeAdditionalInfo || r18.mAdditionalInfo == null || r18.mAdditionalInfo.length <= 0) {
                                r5 = 0;
                            } else {
                                r5 = r18.mAdditionalInfo[0];
                            }
                            sendTerminalResponse(r2, r3, r4, r5, r6);
                            r17.mCurrentCmd = null;
                            break;
                        case 7:
                            r17.mCmdIf.handleCallSetupRequestFromSim(false, com.android.internal.telephony.cat.ResultCode.BACKWARD_MOVE_BY_USER.value(), null);
                            r17.mCurrentCmd = null;
                            break;
                        case 18:
                            com.android.internal.telephony.cat.CatLog.d(r17, new java.lang.StringBuilder().append("SS-handleCmdResponse: [BACKWARD_MOVE_BY_USER] userConfirm[").append(r18.mUsersConfirm).append("] resultCode[").append(r18.mResCode.value()).append("]").toString());
                            r17.mCmdIf.handleCallSetupRequestFromSim(false, com.android.internal.telephony.cat.ResultCode.BACKWARD_MOVE_BY_USER.value(), null);
                            r17.mCurrentCmd = null;
                            break;
                        default:
                            r6 = null;
                            r3 = r18.mResCode;
                            r4 = r18.mIncludeAdditionalInfo;
                            if (!r18.mIncludeAdditionalInfo) {
                                r5 = 0;
                            }
                            sendTerminalResponse(r2, r3, r4, r5, r6);
                            r17.mCurrentCmd = null;
                            break;
                    }
                    break;
                case 3:
                    r14 = true;
                    switch (m219xe796fd46()[r16.ordinal()]) {
                        case 3:
                            if (!android.os.SystemProperties.get("ro.mtk_bsp_package").equals("1") && r17.mHasCachedDTCmd) {
                                resetPowerOnSequenceFlag();
                            }
                            r13 = new byte[1];
                            if (r18.mResCode == com.android.internal.telephony.cat.ResultCode.TERMINAL_CRNTLY_UNABLE_TO_PROCESS) {
                                r13[0] = 1;
                                r18.setAdditionalInfo(r13);
                                break;
                            } else {
                                r18.mIncludeAdditionalInfo = false;
                                r13[0] = 0;
                                break;
                            }
                            break;
                        case 4:
                        case 5:
                            r15 = r17.mCurrentCmd.geInput();
                            if (!r15.yesNo) {
                                if (!r14) {
                                    r6 = new com.android.internal.telephony.cat.GetInkeyInputResponseData(r18.mUsersInput, r15.ucs2, r15.packed);
                                    break;
                                }
                            } else {
                                r6 = new com.android.internal.telephony.cat.GetInkeyInputResponseData(r18.mUsersYesNoSelection);
                                break;
                            }
                            break;
                        case 7:
                        case 18:
                            r17.mCmdIf.handleCallSetupRequestFromSim(r18.mUsersConfirm, r18.mResCode.value(), null);
                            r17.mCurrentCmd = null;
                            break;
                        case 12:
                            com.android.internal.telephony.cat.CatLog.d("CatService", "SELECT_ITEM");
                            r6 = new com.android.internal.telephony.cat.SelectItemResponseData(r18.mUsersMenuSelection);
                            break;
                        case 21:
                            com.android.internal.telephony.cat.CatLog.d("CatService", "SET_UP_MENU");
                            if (r18.mResCode == com.android.internal.telephony.cat.ResultCode.HELP_INFO_REQUIRED) {
                                r14 = true;
                            } else {
                                r14 = false;
                            }
                            sendMenuSelection(r18.mUsersMenuSelection, r14);
                            break;
                    }
                    r3 = r18.mResCode;
                    r4 = r18.mIncludeAdditionalInfo;
                    if (!r18.mIncludeAdditionalInfo) {
                    }
                    sendTerminalResponse(r2, r3, r4, r5, r6);
                    r17.mCurrentCmd = null;
                    break;
                case 4:
                    if (r2.typeOfCommand == com.android.internal.telephony.cat.AppInterface.CommandType.LAUNCH_BROWSER.value()) {
                        com.android.internal.telephony.cat.CatLog.d(r17, "send TR for LAUNCH_BROWSER_ERROR");
                        sendTerminalResponse(r2, r18.mResCode, true, 2, null);
                        break;
                    }
                    r3 = r18.mResCode;
                    r4 = r18.mIncludeAdditionalInfo;
                    if (!r18.mIncludeAdditionalInfo) {
                    }
                    sendTerminalResponse(r2, r3, r4, r5, r6);
                    r17.mCurrentCmd = null;
                    break;
                case 5:
                    switch (m219xe796fd46()[r16.ordinal()]) {
                        case 3:
                            if (!android.os.SystemProperties.get("ro.mtk_bsp_package").equals("1") && r17.mHasCachedDTCmd) {
                                resetPowerOnSequenceFlag();
                            }
                            if (r18.mAdditionalInfo != null && r18.mAdditionalInfo.length > 0 && r18.mAdditionalInfo[0] != 0) {
                                sendTerminalResponse(r2, r18.mResCode, true, r18.mAdditionalInfo[0], null);
                                r17.mCurrentCmd = null;
                                break;
                            }
                            break;
                        case 18:
                            r17.mCmdIf.handleCallSetupRequestFromSim(r18.mUsersConfirm, r18.mResCode.value(), null);
                            r17.mCurrentCmd = null;
                            break;
                    }
                    r3 = r18.mResCode;
                    r4 = r18.mIncludeAdditionalInfo;
                    if (!r18.mIncludeAdditionalInfo) {
                    }
                    sendTerminalResponse(r2, r3, r4, r5, r6);
                    r17.mCurrentCmd = null;
                    break;
                case 7:
                case 8:
                case 9:
                case 10:
                case 11:
                case 12:
                case 13:
                case 14:
                case 15:
                case 16:
                case 17:
                    switch (m219xe796fd46()[r16.ordinal()]) {
                    }
                    r3 = r18.mResCode;
                    r4 = r18.mIncludeAdditionalInfo;
                    if (!r18.mIncludeAdditionalInfo) {
                    }
                    sendTerminalResponse(r2, r3, r4, r5, r6);
                    r17.mCurrentCmd = null;
                case 19:
                    switch (m219xe796fd46()[com.android.internal.telephony.cat.AppInterface.CommandType.fromInt(r2.typeOfCommand).ordinal()]) {
                        case 7:
                            com.android.internal.telephony.cat.CatLog.d("[BIP]", "SS-handleCmdResponse: User don't accept open channel");
                            r17.mCmdIf.handleCallSetupRequestFromSim(false, com.android.internal.telephony.cat.ResultCode.USER_NOT_ACCEPT.value(), null);
                            r17.mCurrentCmd = null;
                            break;
                        default:
                            r3 = r18.mResCode;
                            r4 = r18.mIncludeAdditionalInfo;
                            if (!r18.mIncludeAdditionalInfo) {
                            }
                            sendTerminalResponse(r2, r3, r4, r5, r6);
                            r17.mCurrentCmd = null;
                            break;
                    }
            }
            return;
        }
    }

    public Context getContext() {
        return this.mContext;
    }

    private boolean isStkAppInstalled() {
        Intent intent = new Intent(AppInterface.CAT_CMD_ACTION);
        PackageManager pm = this.mContext.getPackageManager();
        List<ResolveInfo> broadcastReceivers = pm.queryBroadcastReceivers(intent, 128);
        int numReceiver = broadcastReceivers == null ? 0 : broadcastReceivers.size();
        return numReceiver > 0;
    }

    public void update(CommandsInterface ci, Context context, UiccCard ic) {
        UiccCardApplication ca = null;
        IccRecords ir = null;
        if (ic != null) {
            int phoneType = 1;
            int[] subId = SubscriptionManager.getSubId(this.mSlotId);
            if (subId != null) {
                phoneType = TelephonyManager.getDefault().getCurrentPhoneType(subId[0]);
                CatLog.d("CatService", "update phoneType : " + phoneType + "mSlotId: " + this.mSlotId + "subId[0]:" + subId[0]);
            }
            if (phoneType == 2) {
                ca = ic.getApplication(2);
            } else {
                ca = ic.getApplicationIndex(0);
            }
            if (ca != null) {
                ir = ca.getIccRecords();
            }
        }
        synchronized (sInstanceLock) {
            if (ir != null) {
                if (mIccRecords != ir) {
                    if (mIccRecords != null) {
                        mIccRecords.unregisterForRecordsLoaded(this);
                    }
                    if (mUiccApplication != null) {
                        CatLog.d(this, "unregisterForReady slotid: " + this.mSlotId + "instance : " + this);
                        mUiccApplication.unregisterForReady(this);
                    }
                    CatLog.d(this, "Reinitialize the Service with SIMRecords and UiccCardApplication");
                    mIccRecords = ir;
                    mUiccApplication = ca;
                    mIccRecords.registerForRecordsLoaded(this, 20, null);
                    CatLog.d(this, "registerForRecordsLoaded slotid=" + this.mSlotId + " instance:" + this);
                }
            }
        }
    }

    void updateIccAvailability() {
        if (this.mUiccController == null) {
            CatLog.d(this, "updateIccAvailability, mUiccController is null");
            return;
        }
        IccCardStatus.CardState newState = IccCardStatus.CardState.CARDSTATE_ABSENT;
        UiccCard newCard = this.mUiccController.getUiccCard(this.mSlotId);
        if (newCard != null) {
            newState = newCard.getCardState();
        }
        IccCardStatus.CardState oldState = this.mCardState;
        this.mCardState = newState;
        CatLog.d(this, "Slot id: " + this.mSlotId + " New Card State = " + newState + " Old Card State = " + oldState);
        if (oldState == IccCardStatus.CardState.CARDSTATE_PRESENT && newState != IccCardStatus.CardState.CARDSTATE_PRESENT) {
            broadcastCardStateAndIccRefreshResp(newState, null);
            return;
        }
        if (oldState == IccCardStatus.CardState.CARDSTATE_PRESENT || newState != IccCardStatus.CardState.CARDSTATE_PRESENT) {
            return;
        }
        if (this.mCmdIf.getRadioState() == CommandsInterface.RadioState.RADIO_UNAVAILABLE || this.mCmdIf.getRadioState() == CommandsInterface.RadioState.RADIO_OFF) {
            CatLog.w(this, "updateIccAvailability(): Radio not on");
            this.mCardState = oldState;
        } else {
            CatLog.d(this, "SIM present. Reporting STK service running now...");
            this.mCmdIf.reportStkServiceIsRunning(null);
        }
    }

    private boolean isAlarmBoot() {
        String bootReason = SystemProperties.get("sys.boot.reason");
        if (bootReason != null) {
            return bootReason.equals("1");
        }
        return false;
    }

    private boolean checkSetupWizardInstalled() {
        PackageManager pm = this.mContext.getPackageManager();
        if (pm == null) {
            CatLog.d(this, "fail to get PM");
            return false;
        }
        boolean isPkgInstalled = true;
        try {
            pm.getInstallerPackageName("com.google.android.setupwizard");
        } catch (IllegalArgumentException e) {
            CatLog.d(this, "fail to get SetupWizard package");
            isPkgInstalled = false;
        }
        if (isPkgInstalled) {
            int pkgEnabledState = pm.getComponentEnabledSetting(new ComponentName("com.google.android.setupwizard", "com.google.android.setupwizard.SetupWizardActivity"));
            if (pkgEnabledState == 1 || pkgEnabledState == 0) {
                CatLog.d(this, "should not show DISPLAY_TEXT immediately");
                return true;
            }
            CatLog.d(this, "Setup Wizard Activity is not activate");
        }
        CatLog.d(this, "isPkgInstalled = false");
        return false;
    }

    private void registerPowerOnSequenceObserver() {
        if (SystemProperties.get("ro.mtk_bsp_package").equals("1")) {
            return;
        }
        CatLog.d(this, "call registerPowerOnSequenceObserver");
        Uri uri = Settings.System.getUriFor("dialog_sequence_settings");
        this.mContext.getContentResolver().registerContentObserver(uri, false, this.mPowerOnSequenceObserver);
        this.mHasCachedDTCmd = true;
    }

    private void unregisterPowerOnSequenceObserver() {
        if (SystemProperties.get("ro.mtk_bsp_package").equals("1")) {
            return;
        }
        CatLog.d(this, "call unregisterPowerOnSequenceObserver");
        this.mContext.getContentResolver().unregisterContentObserver(this.mPowerOnSequenceObserver);
        cancelTimeOut(46);
    }

    private void resetPowerOnSequenceFlag() {
        if (SystemProperties.get("ro.mtk_bsp_package").equals("1")) {
            return;
        }
        int seqValue = Settings.System.getInt(this.mContext.getContentResolver(), "dialog_sequence_settings", 0);
        CatLog.d(this, "call resetPowerOnSequenceFlag, seqValue: " + seqValue);
        if (seqValue == 2) {
            Settings.System.putInt(this.mContext.getContentResolver(), "dialog_sequence_settings", 0);
        }
        this.mHasCachedDTCmd = false;
    }

    public CatCmdMessage getCmdMessage() {
        CatLog.d(this, "getCmdMessage, command type: " + ((this.mCmdMessage == null || this.mCmdMessage.mCmdDet == null) ? -1 : this.mCmdMessage.mCmdDet.typeOfCommand));
        return this.mCmdMessage;
    }

    public IccRecords getIccRecords() {
        IccRecords iccRecords;
        synchronized (sInstanceLock) {
            iccRecords = mIccRecords;
        }
        return iccRecords;
    }

    private static void saveCmdToPreference(Context context, String key, String cmd) {
        synchronized (mLock) {
            CatLog.d("CatService", "saveCmdToPreference, key: " + key + ", cmd: " + cmd);
            SharedPreferences preferences = context.getSharedPreferences("set_up_menu", 0);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString(key, cmd);
            editor.apply();
        }
    }

    private static String readCmdFromPreference(CatService inst, Context context, String key) {
        String cmd = String.valueOf(UsimPBMemInfo.STRING_NOT_SET);
        if (inst == null) {
            CatLog.d("CatService", "readCmdFromPreference with null instance");
            return null;
        }
        synchronized (mLock) {
            if (!inst.mReadFromPreferenceDone) {
                SharedPreferences preferences = context.getSharedPreferences("set_up_menu", 0);
                cmd = preferences.getString(key, UsimPBMemInfo.STRING_NOT_SET);
                inst.mReadFromPreferenceDone = true;
                CatLog.d("CatService", "readCmdFromPreference, key: " + key + ", cmd: " + cmd);
            } else {
                CatLog.d("CatService", "readCmdFromPreference, do not read again");
            }
        }
        if (cmd.length() == 0) {
            return null;
        }
        return cmd;
    }

    @Override
    public void setAllCallDisConn(boolean isDisConn) {
        if (SystemProperties.get("ro.mtk_bsp_package").equals("1")) {
            return;
        }
        this.mIsAllCallDisConn = isDisConn;
    }

    @Override
    public boolean isCallDisConnReceived() {
        return !SystemProperties.get("ro.mtk_bsp_package").equals("1") && this.mEventDownloadCallDisConnInfo.size() > 0;
    }

    public static int getBatteryState(Context context) {
        int batteryState = 255;
        IntentFilter filter = new IntentFilter("android.intent.action.BATTERY_CHANGED");
        Intent batteryStatus = context.registerReceiver(null, filter);
        if (batteryStatus != null) {
            int level = batteryStatus.getIntExtra("level", -1);
            int scale = batteryStatus.getIntExtra("scale", -1);
            int status = batteryStatus.getIntExtra("status", -1);
            boolean isCharging = status == 2 || status == 5;
            float batteryPct = level / scale;
            CatLog.d("CatService", " batteryPct == " + batteryPct + "isCharging:" + isCharging);
            if (isCharging) {
                batteryState = 255;
            } else if (batteryPct <= 0.05d) {
                batteryState = 0;
            } else if (batteryPct > 0.05d && batteryPct <= 0.15d) {
                batteryState = 1;
            } else if (batteryPct > 0.15d && batteryPct <= 0.6d) {
                batteryState = 2;
            } else if (batteryPct > 0.6d && batteryPct < 1.0f) {
                batteryState = 3;
            } else if (batteryPct == 1.0f) {
                batteryState = 4;
            }
        }
        CatLog.d("CatService", "getBatteryState() batteryState = " + batteryState);
        return batteryState;
    }
}
