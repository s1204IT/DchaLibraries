package com.mediatek.internal.telephony.cat;

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
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.os.SystemProperties;
import android.provider.Settings;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.cat.AppInterface;
import com.android.internal.telephony.cat.BIPClientParams;
import com.android.internal.telephony.cat.CallSetupParams;
import com.android.internal.telephony.cat.CatCmdMessage;
import com.android.internal.telephony.cat.CatResponseMessage;
import com.android.internal.telephony.cat.CatService;
import com.android.internal.telephony.cat.CommandDetails;
import com.android.internal.telephony.cat.CommandParams;
import com.android.internal.telephony.cat.DisplayTextParams;
import com.android.internal.telephony.cat.LaunchBrowserParams;
import com.android.internal.telephony.cat.ResultCode;
import com.android.internal.telephony.cat.RilMessage;
import com.android.internal.telephony.uicc.IccCardStatus;
import com.android.internal.telephony.uicc.IccFileHandler;
import com.android.internal.telephony.uicc.IccRecords;
import com.android.internal.telephony.uicc.UiccCard;
import com.android.internal.telephony.uicc.UiccCardApplication;
import com.android.internal.telephony.uicc.UiccProfile;
import com.mediatek.internal.telephony.ModemSwitchHandler;
import com.mediatek.internal.telephony.MtkRIL;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class MtkCatService extends CatService implements MtkAppInterface {
    static final String BIP_STATE_CHANGED = "mediatek.intent.action.BIP_STATE_CHANGED";
    private static final boolean DBG = true;
    private static final int DISABLE_DISPLAY_TEXT_DELAYED_TIME = 30000;
    private static final int IVSR_DELAYED_TIME = 60000;
    public static final int MSG_ID_CACHED_DISPLAY_TEXT_TIMEOUT = 46;
    private static final int MSG_ID_CALL_CTRL = 25;
    public static final int MSG_ID_CONN_RETRY_TIMEOUT = 47;
    static final int MSG_ID_DB_HANDLER = 12;
    private static final int MSG_ID_DISABLE_DISPLAY_TEXT_DELAYED = 15;
    static final int MSG_ID_EVENT_DOWNLOAD = 11;
    private static final int MSG_ID_IVSR_DELAYED = 14;
    static final int MSG_ID_LAUNCH_DB_SETUP_MENU = 13;
    private static final int MSG_ID_SETUP_MENU_RESET = 24;
    private BroadcastReceiver MtkCatServiceReceiver;
    private boolean isDisplayTextDisabled;
    private boolean isIvsrBootUp;
    private BipService mBipService;
    private boolean mIsProactiveCmdResponsed;
    private MtkRIL mMtkCmdIf;
    private boolean mMtkStkAppInstalled;
    private int mPhoneType;
    private boolean mReadFromPreferenceDone;
    public boolean mSaveNewSetUpMenu;
    private boolean mSetUpMenuFromMD;
    Handler mTimeoutHandler;
    private int simIdfromIntent;
    private int simState;
    private static String[] sInstKey = {"sInstanceSim1", "sInstanceSim2", "sInstanceSim3", "sInstanceSim4"};
    protected static Object mLock = new Object();

    void cancelTimeOut(int i) {
        MtkCatLog.d(this, "cancelTimeOut, sim_id: " + this.mSlotId + ", msg id: " + i);
        this.mTimeoutHandler.removeMessages(i);
    }

    void startTimeOut(int i, long j) {
        MtkCatLog.d(this, "startTimeOut, sim_id: " + this.mSlotId + ", msg id: " + i);
        cancelTimeOut(i);
        this.mTimeoutHandler.sendMessageDelayed(this.mTimeoutHandler.obtainMessage(i), j);
    }

    public MtkCatService(CommandsInterface commandsInterface, UiccCardApplication uiccCardApplication, IccRecords iccRecords, Context context, IccFileHandler iccFileHandler, UiccProfile uiccProfile, int i) {
        super(commandsInterface, uiccCardApplication, iccRecords, context, iccFileHandler, uiccProfile, i);
        this.mSaveNewSetUpMenu = false;
        this.mSetUpMenuFromMD = false;
        this.mReadFromPreferenceDone = false;
        this.mMtkStkAppInstalled = false;
        this.mBipService = null;
        this.simState = 0;
        this.simIdfromIntent = 0;
        this.isIvsrBootUp = false;
        this.isDisplayTextDisabled = false;
        this.mIsProactiveCmdResponsed = false;
        this.mPhoneType = 0;
        this.mTimeoutHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                int i2 = message.what;
                if (i2 == 15) {
                    MtkCatLog.d(this, "[Reset Disable Display Text flag because timeout");
                    MtkCatService.this.isDisplayTextDisabled = false;
                } else if (i2 == 46) {
                    MtkCatLog.d(this, "Cache DISPLAY_TEXT time out, sim_id: " + MtkCatService.this.mSlotId);
                }
            }
        };
        this.MtkCatServiceReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                String action = intent.getAction();
                MtkCatLog.d(this, "CatServiceReceiver action: " + action);
                if (action.equals("mediatek.intent.action.IVSR_NOTIFY")) {
                    if (MtkCatService.this.mSlotId == intent.getIntExtra("slot", 0) && intent.getStringExtra("action").equals("start")) {
                        MtkCatLog.d(this, "[IVSR set IVSR flag");
                        MtkCatService.this.isIvsrBootUp = true;
                        MtkCatService.this.sendEmptyMessageDelayed(14, 60000L);
                        return;
                    }
                    return;
                }
                if (action.equals("com.mediatek.phone.ACTION_SIM_RECOVERY_DONE") || action.equals(ModemSwitchHandler.ACTION_MD_TYPE_CHANGE)) {
                    if (action.equals("com.mediatek.phone.ACTION_SIM_RECOVERY_DONE")) {
                        MtkCatLog.d(this, "[Set SIM Recovery flag, sim: " + MtkCatService.this.mSlotId + ", isDisplayTextDisabled: " + (MtkCatService.this.isDisplayTextDisabled ? 1 : 0));
                    } else {
                        MtkCatLog.d(this, "[World phone flag: " + MtkCatService.this.mSlotId + ", isDisplayTextDisabled: " + (MtkCatService.this.isDisplayTextDisabled ? 1 : 0));
                    }
                    MtkCatService.this.startTimeOut(15, 30000L);
                    MtkCatService.this.isDisplayTextDisabled = true;
                    return;
                }
                if (action.equals("android.telephony.action.SIM_CARD_STATE_CHANGED")) {
                    int intExtra = intent.getIntExtra("slot", -1);
                    MtkCatLog.d(this, "SIM state change, id: " + intExtra + ", simId: " + MtkCatService.this.mSlotId);
                    if (intExtra == MtkCatService.this.mSlotId) {
                        MtkCatService.this.simState = intent.getIntExtra("android.telephony.extra.SIM_STATE", 0);
                        MtkCatService.this.simIdfromIntent = intExtra;
                        MtkCatLog.d(this, "simIdfromIntent[" + MtkCatService.this.simIdfromIntent + "],simState[" + MtkCatService.this.simState + "]");
                        if (MtkCatService.this.simState == 1) {
                            if (TelephonyManager.getDefault().hasIccCard(MtkCatService.this.mSlotId)) {
                                MtkCatLog.d(this, "Igonre absent sim state");
                            } else {
                                MtkCatService.this.mSaveNewSetUpMenu = false;
                                MtkCatService.this.handleDBHandler(MtkCatService.this.mSlotId);
                            }
                        }
                    }
                }
            }
        };
        MtkCatLog.d(this, "slotId " + i);
        this.mMtkCmdIf = (MtkRIL) commandsInterface;
        if (!SystemProperties.get("ro.vendor.mtk_ril_mode").equals("c6m_1rild")) {
            this.mBipService = BipService.getInstance(this.mContext, this, this.mSlotId, this.mCmdIf, iccFileHandler);
        }
        IntentFilter intentFilter = new IntentFilter("mediatek.intent.action.IVSR_NOTIFY");
        intentFilter.addAction("com.mediatek.phone.ACTION_SIM_RECOVERY_DONE");
        intentFilter.addAction(ModemSwitchHandler.ACTION_MD_TYPE_CHANGE);
        IntentFilter intentFilter2 = new IntentFilter("android.telephony.action.SIM_CARD_STATE_CHANGED");
        this.mContext.registerReceiver(this.MtkCatServiceReceiver, intentFilter);
        this.mContext.registerReceiver(this.MtkCatServiceReceiver, intentFilter2);
        MtkCatLog.d(this, "CatService: is running");
        this.mMtkCmdIf.setOnStkSetupMenuReset(this, 24, null);
        this.mMtkStkAppInstalled = isMtkStkAppInstalled();
        MtkCatLog.d(this, "MTK STK app installed = " + this.mMtkStkAppInstalled);
    }

    private void sendTerminalResponseByCurrentCmd(CatCmdMessage catCmdMessage) {
        if (catCmdMessage == null) {
            MtkCatLog.e(this, "catCmd is null.");
        }
        AppInterface.CommandType commandTypeFromInt = AppInterface.CommandType.fromInt(catCmdMessage.mCmdDet.typeOfCommand);
        MtkCatLog.d(this, "Send TR for cmd: " + commandTypeFromInt);
        switch (AnonymousClass3.$SwitchMap$com$android$internal$telephony$cat$AppInterface$CommandType[commandTypeFromInt.ordinal()]) {
            case 1:
            case 2:
                sendTerminalResponse(catCmdMessage.mCmdDet, ResultCode.OK, false, 0, null);
                break;
            case 3:
                this.mMtkCmdIf.handleStkCallSetupRequestFromSimWithResCode(false, ResultCode.OK.value(), null);
                break;
            default:
                sendTerminalResponse(catCmdMessage.mCmdDet, ResultCode.UICC_SESSION_TERM_BY_USER, false, 0, null);
                break;
        }
    }

    public void dispose() {
        synchronized (sInstanceLock) {
            MtkCatLog.d(this, "Disposing MtkCatService object : " + this.mSlotId);
            this.mContext.unregisterReceiver(this.MtkCatServiceReceiver);
            if (!this.mIsProactiveCmdResponsed && this.mCurrntCmd != null) {
                MtkCatLog.d(this, "Send TR for the last pending commands.");
                sendTerminalResponseByCurrentCmd(this.mCurrntCmd);
            }
            this.mMtkCmdIf.unSetOnStkSetupMenuReset(this);
            this.mCmdIf.unregisterForIccRefresh(this);
            handleDBHandler(this.mSlotId);
        }
        if (this.mBipService != null) {
            this.mBipService.dispose();
        }
        super.dispose();
    }

    protected void handleRilMsg(RilMessage rilMessage) {
        if (rilMessage == null) {
        }
        switch (rilMessage.mId) {
            case 2:
                if (rilMessage.mId == 2) {
                    this.mIsProactiveCmdResponsed = false;
                }
                try {
                    CommandParams commandParams = (CommandParams) rilMessage.mData;
                    if (commandParams != null) {
                        if (commandParams.getCommandType() == AppInterface.CommandType.SET_UP_MENU) {
                            this.mSetUpMenuFromMD = ((MtkRilMessage) rilMessage).mSetUpMenuFromMD;
                        }
                        if (rilMessage.mResCode == ResultCode.OK || rilMessage.mResCode == ResultCode.PRFRMD_ICON_NOT_DISPLAYED) {
                            handleCommand(commandParams, true);
                        } else {
                            MtkCatLog.d("CAT", "SS-handleMessage: invalid proactive command: " + commandParams.mCmdDet.typeOfCommand);
                            sendTerminalResponse(commandParams.mCmdDet, rilMessage.mResCode, false, 0, null);
                        }
                    }
                } catch (ClassCastException e) {
                    MtkCatLog.d(this, "Fail to parse proactive command");
                    if (this.mCurrntCmd != null) {
                        sendTerminalResponse(this.mCurrntCmd.mCmdDet, ResultCode.CMD_DATA_NOT_UNDERSTOOD, false, 0, null);
                        return;
                    }
                    return;
                }
                break;
            case 3:
                CommandParams commandParams2 = (CommandParams) rilMessage.mData;
                if (commandParams2 != null) {
                    if (rilMessage.mResCode == ResultCode.OK) {
                        handleCommand(commandParams2, false);
                    } else {
                        MtkCatLog.d(this, "event notify error code: " + rilMessage.mResCode);
                        if (rilMessage.mResCode == ResultCode.PRFRMD_ICON_NOT_DISPLAYED && (commandParams2.mCmdDet.typeOfCommand == 17 || commandParams2.mCmdDet.typeOfCommand == 18 || commandParams2.mCmdDet.typeOfCommand == 19 || commandParams2.mCmdDet.typeOfCommand == 20)) {
                            MtkCatLog.d(this, "notify user text message even though get icon fail");
                            handleCommand(commandParams2, false);
                        }
                        if (commandParams2.mCmdDet.typeOfCommand == 64) {
                            MtkCatLog.d(this, "Open Channel with ResultCode");
                            handleCommand(commandParams2, false);
                        }
                    }
                }
                break;
            default:
                super.handleRilMsg(rilMessage);
                break;
        }
    }

    protected void handleCommand(CommandParams commandParams, boolean z) {
        int i;
        boolean z2;
        int i2;
        boolean z3;
        MtkCatLog.d(this, commandParams.getCommandType().name());
        if (z && this.mUiccController != null) {
            this.mUiccController.addCardLog("ProactiveCommand mSlotId=" + this.mSlotId + " cmdParams=" + commandParams);
        }
        MtkCatCmdMessage mtkCatCmdMessage = new MtkCatCmdMessage(commandParams);
        switch (AnonymousClass3.$SwitchMap$com$android$internal$telephony$cat$AppInterface$CommandType[commandParams.getCommandType().ordinal()]) {
            case 1:
                if (removeMenu(mtkCatCmdMessage.getMenu())) {
                    this.mMenuCmd = null;
                } else {
                    this.mMenuCmd = mtkCatCmdMessage;
                }
                MtkCatLog.d("CAT", "mSetUpMenuFromMD: " + this.mSetUpMenuFromMD);
                if (mtkCatCmdMessage.getMenu() != null) {
                    ((MtkMenu) mtkCatCmdMessage.getMenu()).setSetUpMenuFlag(this.mSetUpMenuFromMD ? 1 : 0);
                }
                if (!this.mSetUpMenuFromMD) {
                    this.mIsProactiveCmdResponsed = true;
                } else {
                    this.mSetUpMenuFromMD = false;
                    sendTerminalResponse(commandParams.mCmdDet, commandParams.mLoadIconFailed ? ResultCode.PRFRMD_ICON_NOT_DISPLAYED : ResultCode.OK, false, 0, null);
                }
                break;
            case 2:
                sendTerminalResponse(commandParams.mCmdDet, commandParams.mLoadIconFailed ? ResultCode.PRFRMD_ICON_NOT_DISPLAYED : ResultCode.OK, false, 0, null);
                break;
            case 3:
                CallSetupParams callSetupParams = (CallSetupParams) commandParams;
                if (callSetupParams.mConfirmMsg.text != null && callSetupParams.mConfirmMsg.text.equals("Default Message")) {
                    callSetupParams.mConfirmMsg.text = this.mContext.getText(R.string.config_systemUi).toString();
                }
                break;
            case 4:
                boolean zIsAlarmBoot = isAlarmBoot();
                try {
                    i = Settings.Global.getInt(this.mContext.getContentResolver(), "airplane_mode_on");
                } catch (Settings.SettingNotFoundException e) {
                    MtkCatLog.d(this, "fail to get property from Settings");
                    i = 0;
                }
                z2 = i != 0;
                MtkCatLog.d(this, "isAlarmState = " + zIsAlarmBoot + ", isFlightMode = " + z2 + ", flightMode = " + i);
                if (zIsAlarmBoot && z2) {
                    sendTerminalResponse(commandParams.mCmdDet, ResultCode.OK, false, 0, null);
                    return;
                }
                if (checkSetupWizardInstalled()) {
                    sendTerminalResponse(commandParams.mCmdDet, ResultCode.BACKWARD_MOVE_BY_USER, false, 0, null);
                    return;
                }
                if (this.isIvsrBootUp) {
                    MtkCatLog.d(this, "[IVSR send TR directly");
                    sendTerminalResponse(commandParams.mCmdDet, ResultCode.BACKWARD_MOVE_BY_USER, false, 0, null);
                    return;
                } else if (this.isDisplayTextDisabled) {
                    MtkCatLog.d(this, "[Sim Recovery send TR directly");
                    sendTerminalResponse(commandParams.mCmdDet, ResultCode.BACKWARD_MOVE_BY_USER, false, 0, null);
                    return;
                }
                break;
            case 5:
                this.mIsProactiveCmdResponsed = true;
                commandParams.mCmdDet.typeOfCommand = AppInterface.CommandType.SET_UP_IDLE_MODE_TEXT.value();
                break;
            case 6:
                if (this.mBipService != null) {
                    this.mBipService.setSetupEventList(mtkCatCmdMessage);
                }
                this.mIsProactiveCmdResponsed = true;
                return;
            case 7:
                if (commandParams.mCmdDet.commandQualifier == 3) {
                    Calendar calendar = Calendar.getInstance();
                    int i3 = calendar.get(1) - 2000;
                    int i4 = i3 / 10;
                    int i5 = calendar.get(2) + 1;
                    int i6 = calendar.get(5);
                    int i7 = i6 / 10;
                    int i8 = calendar.get(11);
                    int i9 = i8 / 10;
                    int i10 = calendar.get(12);
                    int i11 = i10 / 10;
                    int i12 = calendar.get(13);
                    int i13 = calendar.get(15) / 900000;
                    byte[] bArr = {(byte) (((i3 % 10) << 4) | i4), (byte) (((i5 % 10) << 4) | (i5 / 10)), (byte) (((i6 % 10) << 4) | i7), (byte) (((i8 % 10) << 4) | i9), (byte) (((i10 % 10) << 4) | i11), (byte) (((i12 % 10) << 4) | (i12 / 10)), (byte) (((i13 % 10) << 4) | (i13 / 10))};
                    sendTerminalResponse(commandParams.mCmdDet, ResultCode.OK, false, 0, new MtkProvideLocalInformationResponseData(bArr[0], bArr[1], bArr[2], bArr[3], bArr[4], bArr[5], bArr[6]));
                    return;
                }
                if (commandParams.mCmdDet.commandQualifier == 4) {
                    Locale locale = Locale.getDefault();
                    sendTerminalResponse(commandParams.mCmdDet, ResultCode.OK, false, 0, new MtkProvideLocalInformationResponseData(new byte[]{(byte) locale.getLanguage().charAt(0), (byte) locale.getLanguage().charAt(1)}));
                    return;
                } else {
                    if (commandParams.mCmdDet.commandQualifier == 10) {
                        sendTerminalResponse(commandParams.mCmdDet, ResultCode.OK, false, 0, new MtkProvideLocalInformationResponseData(getBatteryState(this.mContext)));
                        return;
                    }
                    return;
                }
            case 8:
                LaunchBrowserParams launchBrowserParams = (LaunchBrowserParams) commandParams;
                if (launchBrowserParams.mConfirmMsg.text != null && launchBrowserParams.mConfirmMsg.text.equals("Default Message")) {
                    launchBrowserParams.mConfirmMsg.text = this.mContext.getText(R.string.config_wimaxStateTrackerClassname).toString();
                }
                break;
            case 9:
                boolean zIsAlarmBoot2 = isAlarmBoot();
                try {
                    i2 = Settings.Global.getInt(this.mContext.getContentResolver(), "airplane_mode_on");
                } catch (Settings.SettingNotFoundException e2) {
                    MtkCatLog.d(this, "fail to get property from Settings");
                    i2 = 0;
                }
                z2 = i2 != 0;
                MtkCatLog.d(this, "isAlarmState = " + zIsAlarmBoot2 + ", isFlightMode = " + z2 + ", flightMode = " + i2);
                if (zIsAlarmBoot2 && z2) {
                    sendTerminalResponse(commandParams.mCmdDet, ResultCode.UICC_SESSION_TERM_BY_USER, false, 0, null);
                    return;
                }
                break;
            case 10:
            case 11:
                if (this.simState != 11) {
                    sendTerminalResponse(commandParams.mCmdDet, ResultCode.TERMINAL_CRNTLY_UNABLE_TO_PROCESS, false, 0, null);
                    return;
                }
                break;
            case 12:
            case 13:
            case 14:
            case 15:
                this.mIsProactiveCmdResponsed = true;
                DisplayTextParams displayTextParams = (DisplayTextParams) commandParams;
                if (displayTextParams.mTextMsg.text != null && displayTextParams.mTextMsg.text.equals("Default Message")) {
                    displayTextParams.mTextMsg.text = this.mContext.getText(R.string.lockscreen_screen_locked).toString();
                }
                break;
            case 16:
                this.mIsProactiveCmdResponsed = true;
                break;
            case 17:
            case 18:
            case 19:
            case 20:
                BIPClientParams bIPClientParams = (BIPClientParams) commandParams;
                try {
                    z3 = this.mContext.getResources().getBoolean(R.^attr-private.outKeycode);
                } catch (Resources.NotFoundException e3) {
                    z3 = false;
                }
                if (bIPClientParams.mTextMsg.text == null && (bIPClientParams.mHasAlphaId || z3)) {
                    MtkCatLog.d(this, "cmd " + commandParams.getCommandType() + " with null alpha id");
                    if (z) {
                        sendTerminalResponse(commandParams.mCmdDet, ResultCode.OK, false, 0, null);
                        return;
                    } else {
                        if (commandParams.getCommandType() == AppInterface.CommandType.OPEN_CHANNEL) {
                            this.mMtkCmdIf.handleStkCallSetupRequestFromSimWithResCode(true, ResultCode.OK.value(), null);
                            return;
                        }
                        return;
                    }
                }
                if (!this.mStkAppInstalled && !this.mMtkStkAppInstalled) {
                    MtkCatLog.d(this, "No STK application found.");
                    if (z) {
                        sendTerminalResponse(commandParams.mCmdDet, ResultCode.BEYOND_TERMINAL_CAPABILITY, false, 0, null);
                        return;
                    }
                }
                if (z && (commandParams.getCommandType() == AppInterface.CommandType.CLOSE_CHANNEL || commandParams.getCommandType() == AppInterface.CommandType.RECEIVE_DATA || commandParams.getCommandType() == AppInterface.CommandType.SEND_DATA)) {
                    sendTerminalResponse(commandParams.mCmdDet, ResultCode.OK, false, 0, null);
                }
                break;
                break;
            default:
                MtkCatLog.d(this, "HandleCommand Callback to CatService");
                super.handleCommand(commandParams, z);
                return;
        }
        this.mCurrntCmd = mtkCatCmdMessage;
        mtkBroadcastCatCmdIntent(mtkCatCmdMessage);
        broadcastCatCmdIntent(mtkCatCmdMessage.convertToCatCmdMessage(commandParams, mtkCatCmdMessage));
    }

    private void mtkBroadcastCatCmdIntent(CatCmdMessage catCmdMessage) {
        Intent intent = new Intent(MtkAppInterface.MTK_CAT_CMD_ACTION);
        intent.putExtra("STK CMD", (Parcelable) catCmdMessage);
        intent.putExtra("SLOT_ID", this.mSlotId);
        intent.setComponent(AppInterface.getDefaultSTKApplication());
        MtkCatLog.d(this, "mtkBroadcastCatCmdIntent Sending CmdMsg: " + catCmdMessage + " on slotid:" + this.mSlotId);
        this.mContext.sendBroadcast(intent, "android.permission.RECEIVE_STK_COMMANDS");
    }

    protected void onSetResponsedFlag() {
        this.mIsProactiveCmdResponsed = true;
    }

    protected void sendMenuSelection(int i, boolean z) {
        MtkCatLog.d("CatService", "sendMenuSelection SET_UP_MENU");
        super.sendMenuSelection(i, z);
        cancelTimeOut(15);
        this.isDisplayTextDisabled = false;
    }

    public static CatService getInstance(CommandsInterface commandsInterface, Context context, UiccProfile uiccProfile) {
        int phoneId;
        MtkCatLog.d("CatService", "call getInstance 2");
        if (uiccProfile != null) {
            phoneId = uiccProfile.getPhoneId();
            MtkCatLog.d("CatService", "get SIM id from UiccCard. sim id: " + phoneId);
        } else {
            phoneId = 0;
        }
        return CatService.getInstance(commandsInterface, context, uiccProfile, phoneId);
    }

    public static MtkAppInterface getInstance() {
        MtkCatLog.d("CatService", "call getInstance 4");
        return (MtkCatService) getInstance(null, null, null, 0);
    }

    public static MtkAppInterface getInstance(int i) {
        MtkCatLog.d("CatService", "call getInstance 3");
        return (MtkCatService) getInstance(null, null, null, i);
    }

    private static void handleProactiveCmdFromDB(MtkCatService mtkCatService, String str) {
        if (str == null) {
            MtkCatLog.d("MtkCatService", "handleProactiveCmdFromDB: cmd = null");
            return;
        }
        MtkCatLog.d("MtkCatService", " handleProactiveCmdFromDB: cmd = " + str + " from: " + mtkCatService);
        mtkCatService.mMsgDecoder.sendStartDecodingMessageParams(new MtkRilMessage(2, str));
        MtkCatLog.d("MtkCatService", "handleProactiveCmdFromDB: over");
    }

    private boolean isSetUpMenuCmd(String str) {
        if (str == null) {
            return false;
        }
        try {
            if (str.charAt(2) == '8' && str.charAt(3) == '1') {
                if (str.charAt(12) == '2' && str.charAt(13) == '5') {
                    return true;
                }
            } else if (str.charAt(10) == '2' && str.charAt(11) == '5') {
                return true;
            }
            return false;
        } catch (IndexOutOfBoundsException e) {
            MtkCatLog.d(this, "IndexOutOfBoundsException isSetUpMenuCmd: " + str);
            e.printStackTrace();
            return false;
        }
    }

    public static boolean getSaveNewSetUpMenuFlag(int i) {
        if (sInstance != null && sInstance[i] != null) {
            boolean z = ((MtkCatService) sInstance[i]).mSaveNewSetUpMenu;
            MtkCatLog.d("CatService", i + " , mSaveNewSetUpMenu: " + z);
            return z;
        }
        return false;
    }

    public void handleMessage(Message message) {
        MtkCatLog.d(this, "MtkCatservice handleMessage[" + message.what + "]");
        int i = message.what;
        if (i == 5) {
            MtkCatLog.d(this, "ril message arrived, slotid:" + this.mSlotId);
            if (message.obj != null) {
                AsyncResult asyncResult = (AsyncResult) message.obj;
                if (this.mMsgDecoder == null) {
                    MtkCatLog.e(this, "mMsgDecoder == null, return.");
                    return;
                }
                if (asyncResult != null && asyncResult.result != null) {
                    try {
                        String str = (String) asyncResult.result;
                        if (isSetUpMenuCmd(str) && this == sInstance[this.mSlotId]) {
                            saveCmdToPreference(this.mContext, sInstKey[this.mSlotId], str);
                            this.mSaveNewSetUpMenu = true;
                            MtkRilMessage mtkRilMessage = new MtkRilMessage(message.what, str);
                            mtkRilMessage.setSetUpMenuFromMD(true);
                            this.mMsgDecoder.sendStartDecodingMessageParams(mtkRilMessage);
                            return;
                        }
                        if (str.contains("BIP")) {
                            Intent intent = new Intent(BIP_STATE_CHANGED);
                            intent.putExtra("BIP_CMD", str);
                            intent.putExtra("SLOT_ID", this.mSlotId);
                            intent.setPackage("com.mediatek.engineermode");
                            MtkCatLog.d(this, "Broadcast BIP Intent: Sending data: " + str + " on slotid:" + this.mSlotId);
                            this.mContext.sendBroadcast(intent);
                            return;
                        }
                    } catch (ClassCastException e) {
                        return;
                    }
                }
            }
        } else if (i != 24) {
            switch (i) {
                case 1:
                case 2:
                case 3:
                    break;
                default:
                    switch (i) {
                        case 11:
                            handleEventDownload((MtkCatResponseMessage) message.obj);
                            break;
                        case 12:
                            handleDBHandler(message.arg1);
                            break;
                        case 13:
                            MtkCatLog.d(this, "MSG_ID_LAUNCH_DB_SETUP_MENU");
                            String cmdFromPreference = readCmdFromPreference((MtkCatService) sInstance[this.mSlotId], this.mContext, sInstKey[this.mSlotId]);
                            if (sInstance[this.mSlotId] != null && cmdFromPreference != null) {
                                handleProactiveCmdFromDB((MtkCatService) sInstance[this.mSlotId], cmdFromPreference);
                                this.mSaveNewSetUpMenu = true;
                                break;
                            }
                            break;
                        case 14:
                            MtkCatLog.d(this, "[IVSR cancel IVSR flag");
                            this.isIvsrBootUp = false;
                            break;
                    }
                    return;
            }
        } else {
            MtkCatLog.d(this, "SETUP_MENU_RESET : Setup menu reset.");
            AsyncResult asyncResult2 = (AsyncResult) message.obj;
            if (asyncResult2 != null && asyncResult2.exception == null) {
                this.mSaveNewSetUpMenu = false;
                return;
            } else {
                MtkCatLog.d(this, "SETUP_MENU_RESET : AsyncResult null.");
                return;
            }
        }
        super.handleMessage(message);
    }

    public synchronized void onCmdResponse(CatResponseMessage catResponseMessage) {
        MtkCatResponseMessage mtkCatResponseMessage;
        MtkCatLog.d(this, "MtkCatService onCmdResponse");
        if (catResponseMessage == null) {
            return;
        }
        if (MtkCatResponseMessage.class.isInstance(catResponseMessage)) {
            obtainMessage(6, catResponseMessage).sendToTarget();
        } else {
            if (this.mCurrntCmd != null) {
                mtkCatResponseMessage = new MtkCatResponseMessage(this.mCurrntCmd, catResponseMessage);
            } else {
                mtkCatResponseMessage = new MtkCatResponseMessage(MtkCatCmdMessage.getCmdMsg(), catResponseMessage);
            }
            obtainMessage(6, mtkCatResponseMessage).sendToTarget();
        }
    }

    @Override
    public synchronized void onEventDownload(MtkCatResponseMessage mtkCatResponseMessage) {
        if (mtkCatResponseMessage == null) {
            return;
        }
        obtainMessage(11, mtkCatResponseMessage).sendToTarget();
    }

    @Override
    public synchronized void onDBHandler(int i) {
        obtainMessage(12, i, 0).sendToTarget();
    }

    @Override
    public synchronized void onLaunchCachedSetupMenu() {
        obtainMessage(13, this.mSlotId, 0).sendToTarget();
    }

    private void handleEventDownload(MtkCatResponseMessage mtkCatResponseMessage) {
        eventDownload(mtkCatResponseMessage.mEvent, mtkCatResponseMessage.mSourceId, mtkCatResponseMessage.mDestinationId, mtkCatResponseMessage.mAdditionalInfo, mtkCatResponseMessage.mOneShot);
    }

    private void handleDBHandler(int i) {
        MtkCatLog.d(this, "handleDBHandler, sim_id: " + i);
        saveCmdToPreference(this.mContext, sInstKey[i], null);
    }

    protected void handleCmdResponse(CatResponseMessage catResponseMessage) {
        if (!validateResponse(catResponseMessage)) {
            return;
        }
        CommandDetails cmdDetails = catResponseMessage.getCmdDetails();
        AppInterface.CommandType commandTypeFromInt = AppInterface.CommandType.fromInt(cmdDetails.typeOfCommand);
        switch (AnonymousClass3.$SwitchMap$com$android$internal$telephony$cat$ResultCode[catResponseMessage.mResCode.ordinal()]) {
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
            case 6:
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
                if (commandTypeFromInt == AppInterface.CommandType.SET_UP_CALL || commandTypeFromInt == AppInterface.CommandType.OPEN_CHANNEL) {
                    this.mMtkCmdIf.handleStkCallSetupRequestFromSimWithResCode(catResponseMessage.mUsersConfirm, catResponseMessage.mResCode.value(), null);
                    this.mCurrntCmd = null;
                    return;
                }
                break;
            case 18:
                if (commandTypeFromInt == AppInterface.CommandType.SET_UP_CALL) {
                    this.mMtkCmdIf.handleStkCallSetupRequestFromSimWithResCode(catResponseMessage.mUsersConfirm, catResponseMessage.mResCode.value(), null);
                    this.mCurrntCmd = null;
                    return;
                } else if (commandTypeFromInt == AppInterface.CommandType.DISPLAY_TEXT) {
                    sendTerminalResponse(cmdDetails, catResponseMessage.mResCode, catResponseMessage.mIncludeAdditionalInfo, catResponseMessage.mAdditionalInfo, null);
                    this.mCurrntCmd = null;
                    return;
                }
        }
        super.handleCmdResponse(catResponseMessage);
    }

    static class AnonymousClass3 {
        static final int[] $SwitchMap$com$android$internal$telephony$cat$AppInterface$CommandType;
        static final int[] $SwitchMap$com$android$internal$telephony$cat$ResultCode = new int[ResultCode.values().length];

        static {
            try {
                $SwitchMap$com$android$internal$telephony$cat$ResultCode[ResultCode.HELP_INFO_REQUIRED.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$cat$ResultCode[ResultCode.OK.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$cat$ResultCode[ResultCode.PRFRMD_WITH_PARTIAL_COMPREHENSION.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$cat$ResultCode[ResultCode.PRFRMD_WITH_MISSING_INFO.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$cat$ResultCode[ResultCode.PRFRMD_WITH_ADDITIONAL_EFS_READ.ordinal()] = 5;
            } catch (NoSuchFieldError e5) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$cat$ResultCode[ResultCode.PRFRMD_ICON_NOT_DISPLAYED.ordinal()] = 6;
            } catch (NoSuchFieldError e6) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$cat$ResultCode[ResultCode.PRFRMD_MODIFIED_BY_NAA.ordinal()] = 7;
            } catch (NoSuchFieldError e7) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$cat$ResultCode[ResultCode.PRFRMD_LIMITED_SERVICE.ordinal()] = 8;
            } catch (NoSuchFieldError e8) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$cat$ResultCode[ResultCode.PRFRMD_WITH_MODIFICATION.ordinal()] = 9;
            } catch (NoSuchFieldError e9) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$cat$ResultCode[ResultCode.PRFRMD_NAA_NOT_ACTIVE.ordinal()] = 10;
            } catch (NoSuchFieldError e10) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$cat$ResultCode[ResultCode.PRFRMD_TONE_NOT_PLAYED.ordinal()] = 11;
            } catch (NoSuchFieldError e11) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$cat$ResultCode[ResultCode.TERMINAL_CRNTLY_UNABLE_TO_PROCESS.ordinal()] = 12;
            } catch (NoSuchFieldError e12) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$cat$ResultCode[ResultCode.NO_RESPONSE_FROM_USER.ordinal()] = 13;
            } catch (NoSuchFieldError e13) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$cat$ResultCode[ResultCode.UICC_SESSION_TERM_BY_USER.ordinal()] = 14;
            } catch (NoSuchFieldError e14) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$cat$ResultCode[ResultCode.BACKWARD_MOVE_BY_USER.ordinal()] = 15;
            } catch (NoSuchFieldError e15) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$cat$ResultCode[ResultCode.CMD_DATA_NOT_UNDERSTOOD.ordinal()] = 16;
            } catch (NoSuchFieldError e16) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$cat$ResultCode[ResultCode.USER_NOT_ACCEPT.ordinal()] = 17;
            } catch (NoSuchFieldError e17) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$cat$ResultCode[ResultCode.NETWORK_CRNTLY_UNABLE_TO_PROCESS.ordinal()] = 18;
            } catch (NoSuchFieldError e18) {
            }
            $SwitchMap$com$android$internal$telephony$cat$AppInterface$CommandType = new int[AppInterface.CommandType.values().length];
            try {
                $SwitchMap$com$android$internal$telephony$cat$AppInterface$CommandType[AppInterface.CommandType.SET_UP_MENU.ordinal()] = 1;
            } catch (NoSuchFieldError e19) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$cat$AppInterface$CommandType[AppInterface.CommandType.SET_UP_IDLE_MODE_TEXT.ordinal()] = 2;
            } catch (NoSuchFieldError e20) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$cat$AppInterface$CommandType[AppInterface.CommandType.SET_UP_CALL.ordinal()] = 3;
            } catch (NoSuchFieldError e21) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$cat$AppInterface$CommandType[AppInterface.CommandType.DISPLAY_TEXT.ordinal()] = 4;
            } catch (NoSuchFieldError e22) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$cat$AppInterface$CommandType[AppInterface.CommandType.REFRESH.ordinal()] = 5;
            } catch (NoSuchFieldError e23) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$cat$AppInterface$CommandType[AppInterface.CommandType.SET_UP_EVENT_LIST.ordinal()] = 6;
            } catch (NoSuchFieldError e24) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$cat$AppInterface$CommandType[AppInterface.CommandType.PROVIDE_LOCAL_INFORMATION.ordinal()] = 7;
            } catch (NoSuchFieldError e25) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$cat$AppInterface$CommandType[AppInterface.CommandType.LAUNCH_BROWSER.ordinal()] = 8;
            } catch (NoSuchFieldError e26) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$cat$AppInterface$CommandType[AppInterface.CommandType.SELECT_ITEM.ordinal()] = 9;
            } catch (NoSuchFieldError e27) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$cat$AppInterface$CommandType[AppInterface.CommandType.GET_INPUT.ordinal()] = 10;
            } catch (NoSuchFieldError e28) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$cat$AppInterface$CommandType[AppInterface.CommandType.GET_INKEY.ordinal()] = 11;
            } catch (NoSuchFieldError e29) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$cat$AppInterface$CommandType[AppInterface.CommandType.SEND_DTMF.ordinal()] = 12;
            } catch (NoSuchFieldError e30) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$cat$AppInterface$CommandType[AppInterface.CommandType.SEND_SMS.ordinal()] = 13;
            } catch (NoSuchFieldError e31) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$cat$AppInterface$CommandType[AppInterface.CommandType.SEND_SS.ordinal()] = 14;
            } catch (NoSuchFieldError e32) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$cat$AppInterface$CommandType[AppInterface.CommandType.SEND_USSD.ordinal()] = 15;
            } catch (NoSuchFieldError e33) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$cat$AppInterface$CommandType[AppInterface.CommandType.PLAY_TONE.ordinal()] = 16;
            } catch (NoSuchFieldError e34) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$cat$AppInterface$CommandType[AppInterface.CommandType.OPEN_CHANNEL.ordinal()] = 17;
            } catch (NoSuchFieldError e35) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$cat$AppInterface$CommandType[AppInterface.CommandType.CLOSE_CHANNEL.ordinal()] = 18;
            } catch (NoSuchFieldError e36) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$cat$AppInterface$CommandType[AppInterface.CommandType.RECEIVE_DATA.ordinal()] = 19;
            } catch (NoSuchFieldError e37) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$cat$AppInterface$CommandType[AppInterface.CommandType.SEND_DATA.ordinal()] = 20;
            } catch (NoSuchFieldError e38) {
            }
        }
    }

    public Context getContext() {
        return this.mContext;
    }

    public void update(CommandsInterface commandsInterface, Context context, UiccProfile uiccProfile) {
        UiccCardApplication applicationIndex;
        IccRecords iccRecords;
        if (uiccProfile == null) {
            applicationIndex = null;
            iccRecords = null;
        } else {
            int phoneType = getPhoneType();
            MtkCatLog.d("MtkCatService", "update PhoneType : " + phoneType + ", mSlotId: " + this.mSlotId);
            int i = this.mPhoneType;
            this.mPhoneType = phoneType;
            if (i != 0 && i != phoneType) {
                MtkCatLog.d("MtkCatService", "phone type change,reset card state to absent.....");
                this.mCardState = IccCardStatus.CardState.CARDSTATE_ABSENT;
            }
            if (this.mPhoneType == 2) {
                applicationIndex = uiccProfile.getApplication(2);
            } else {
                applicationIndex = uiccProfile.getApplicationIndex(0);
            }
            if (applicationIndex != null) {
                iccRecords = applicationIndex.getIccRecords();
            } else {
                iccRecords = null;
            }
        }
        synchronized (sInstanceLock) {
            if (iccRecords != null) {
                try {
                    if (mIccRecords != iccRecords) {
                        if (mIccRecords != null) {
                            mIccRecords.unregisterForRecordsLoaded(this);
                        }
                        MtkCatLog.d(this, "Reinitialize the Service with SIMRecords and UiccCardApplication");
                        mIccRecords = iccRecords;
                        mUiccApplication = applicationIndex;
                        mIccRecords.registerForRecordsLoaded(this, 20, (Object) null);
                        MtkCatLog.d(this, "registerForRecordsLoaded slotid=" + this.mSlotId + " instance:" + this);
                    }
                } catch (Throwable th) {
                    throw th;
                }
            }
        }
    }

    protected void updateIccAvailability() {
        if (this.mUiccController == null) {
            MtkCatLog.d(this, "updateIccAvailability, mUiccController is null");
            return;
        }
        IccCardStatus.CardState cardState = IccCardStatus.CardState.CARDSTATE_ABSENT;
        UiccCard uiccCard = this.mUiccController.getUiccCard(this.mSlotId);
        if (uiccCard != null) {
            cardState = uiccCard.getCardState();
        }
        IccCardStatus.CardState cardState2 = this.mCardState;
        this.mCardState = cardState;
        MtkCatLog.d(this, "Slot id: " + this.mSlotId + " New Card State = " + cardState + " Old Card State = " + cardState2);
        if (cardState2 == IccCardStatus.CardState.CARDSTATE_PRESENT && cardState != IccCardStatus.CardState.CARDSTATE_PRESENT) {
            broadcastCardStateAndIccRefreshResp(cardState, null);
            return;
        }
        if (cardState2 != IccCardStatus.CardState.CARDSTATE_PRESENT && cardState == IccCardStatus.CardState.CARDSTATE_PRESENT) {
            if (this.mCmdIf.getRadioState() == CommandsInterface.RadioState.RADIO_UNAVAILABLE) {
                MtkCatLog.w(this, "updateIccAvailability(): Radio unavailable");
                this.mCardState = cardState2;
            } else {
                MtkCatLog.d(this, "SIM present. Reporting STK service running now...");
                this.mCmdIf.reportStkServiceIsRunning((Message) null);
            }
        }
    }

    private boolean isAlarmBoot() {
        String str = SystemProperties.get("vendor.sys.boot.reason");
        return str != null && str.equals("1");
    }

    private boolean checkSetupWizardInstalled() {
        boolean z;
        PackageManager packageManager = this.mContext.getPackageManager();
        if (packageManager == null) {
            MtkCatLog.d(this, "fail to get PM");
            return false;
        }
        try {
            packageManager.getInstallerPackageName("com.google.android.setupwizard");
            z = true;
        } catch (IllegalArgumentException e) {
            MtkCatLog.d(this, "fail to get SetupWizard package");
            z = false;
        }
        if (z) {
            int componentEnabledSetting = packageManager.getComponentEnabledSetting(new ComponentName("com.google.android.setupwizard", "com.google.android.setupwizard.SetupWizardActivity"));
            if (componentEnabledSetting == 1 || componentEnabledSetting == 0) {
                MtkCatLog.d(this, "should not show DISPLAY_TEXT immediately");
                return true;
            }
            MtkCatLog.d(this, "Setup Wizard Activity is not activate");
        }
        MtkCatLog.d(this, "isPkgInstalled = false");
        return false;
    }

    @Override
    public IccRecords getIccRecords() {
        IccRecords iccRecords;
        synchronized (sInstanceLock) {
            iccRecords = mIccRecords;
        }
        return iccRecords;
    }

    private static void saveCmdToPreference(Context context, String str, String str2) {
        synchronized (mLock) {
            MtkCatLog.d("MtkCatService", "saveCmdToPreference, key: " + str + ", cmd: " + str2);
            SharedPreferences.Editor editorEdit = context.getSharedPreferences("set_up_menu", 0).edit();
            editorEdit.putString(str, str2);
            editorEdit.apply();
        }
    }

    private static String readCmdFromPreference(MtkCatService mtkCatService, Context context, String str) {
        String strValueOf = String.valueOf("");
        if (mtkCatService == null) {
            MtkCatLog.d("MtkCatService", "readCmdFromPreference with null instance");
            return null;
        }
        synchronized (mLock) {
            if (!mtkCatService.mReadFromPreferenceDone) {
                strValueOf = context.getSharedPreferences("set_up_menu", 0).getString(str, "");
                mtkCatService.mReadFromPreferenceDone = true;
                MtkCatLog.d("MtkCatService", "readCmdFromPreference, key: " + str + ", cmd: " + strValueOf);
            } else {
                MtkCatLog.d("MtkCatService", "readCmdFromPreference, do not read again");
            }
        }
        if (strValueOf.length() == 0) {
            return null;
        }
        return strValueOf;
    }

    public static int getBatteryState(Context context) {
        Intent intentRegisterReceiver = context.registerReceiver(null, new IntentFilter("android.intent.action.BATTERY_CHANGED"));
        int i = 1;
        if (intentRegisterReceiver != null) {
            int intExtra = intentRegisterReceiver.getIntExtra("level", -1);
            int intExtra2 = intentRegisterReceiver.getIntExtra("scale", -1);
            int intExtra3 = intentRegisterReceiver.getIntExtra("status", -1);
            boolean z = intExtra3 == 2 || intExtra3 == 5;
            float f = intExtra / intExtra2;
            MtkCatLog.d("MtkCatService", " batteryPct == " + f + "isCharging:" + z);
            if (!z) {
                double d = f;
                if (d <= 0.05d) {
                    i = 0;
                } else if (d <= 0.05d || d > 0.15d) {
                    if (d > 0.15d && d <= 0.6d) {
                        i = 2;
                    } else if (d > 0.6d && f < 1.0f) {
                        i = 3;
                    } else if (f == 1.0f) {
                        i = 4;
                    }
                }
            } else {
                i = 255;
            }
        }
        MtkCatLog.d("MtkCatService", "getBatteryState() batteryState = " + i);
        return i;
    }

    private boolean isMtkStkAppInstalled() {
        int size;
        List<ResolveInfo> listQueryBroadcastReceivers = this.mContext.getPackageManager().queryBroadcastReceivers(new Intent(MtkAppInterface.MTK_CAT_CMD_ACTION), 128);
        if (listQueryBroadcastReceivers != null) {
            size = listQueryBroadcastReceivers.size();
        } else {
            size = 0;
        }
        return size > 0;
    }

    private int getPhoneType() {
        int[] subId = SubscriptionManager.getSubId(this.mSlotId);
        if (subId != null) {
            int currentPhoneType = TelephonyManager.getDefault().getCurrentPhoneType(subId[0]);
            MtkCatLog.v(this, "getPhoneType phoneType:  " + currentPhoneType + ", mSlotId: " + this.mSlotId);
            return currentPhoneType;
        }
        return 0;
    }
}
