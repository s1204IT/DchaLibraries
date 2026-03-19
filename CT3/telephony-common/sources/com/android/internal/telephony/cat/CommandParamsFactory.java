package com.android.internal.telephony.cat;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Message;
import com.android.internal.telephony.GsmAlphabet;
import com.android.internal.telephony.cat.AppInterface;
import com.android.internal.telephony.uicc.IccFileHandler;
import com.mediatek.internal.telephony.ppl.PplMessageManager;
import com.mediatek.internal.telephony.uicc.UsimPBMemInfo;
import java.util.Iterator;
import java.util.List;

class CommandParamsFactory extends Handler {

    private static final int[] f19x72eb89a2 = null;
    static final int BATTERY_STATE = 10;
    static final int DTTZ_SETTING = 3;
    static final int LANGUAGE_SETTING = 4;
    static final int LOAD_MULTI_ICONS = 2;
    static final int LOAD_NO_ICON = 0;
    static final int LOAD_SINGLE_ICON = 1;
    private static final int MAX_GSM7_DEFAULT_CHARS = 239;
    private static final int MAX_UCS2_CHARS = 118;
    static final int MSG_ID_LOAD_ICON_DONE = 1;
    static final int REFRESH_NAA_INIT = 3;
    static final int REFRESH_NAA_INIT_AND_FILE_CHANGE = 2;
    static final int REFRESH_NAA_INIT_AND_FULL_FILE_CHANGE = 0;
    static final int REFRESH_UICC_RESET = 4;
    private static CommandParamsFactory sInstance = null;
    private RilMessageDecoder mCaller;
    private Context mContext;
    private IconLoader mIconLoader;
    private CommandParams mCmdParams = null;
    private int mIconLoadState = 0;
    private boolean mloadIcon = false;
    int tlvIndex = -1;

    private static int[] m239xe796fd46() {
        if (f19x72eb89a2 != null) {
            return f19x72eb89a2;
        }
        int[] iArr = new int[AppInterface.CommandType.valuesCustom().length];
        try {
            iArr[AppInterface.CommandType.ACTIVATE.ordinal()] = 1;
        } catch (NoSuchFieldError e) {
        }
        try {
            iArr[AppInterface.CommandType.CALLCTRL_RSP_MSG.ordinal()] = 23;
        } catch (NoSuchFieldError e2) {
        }
        try {
            iArr[AppInterface.CommandType.CLOSE_CHANNEL.ordinal()] = 2;
        } catch (NoSuchFieldError e3) {
        }
        try {
            iArr[AppInterface.CommandType.DECLARE_SERVICE.ordinal()] = 24;
        } catch (NoSuchFieldError e4) {
        }
        try {
            iArr[AppInterface.CommandType.DISPLAY_MULTIMEDIA_MESSAGE.ordinal()] = 25;
        } catch (NoSuchFieldError e5) {
        }
        try {
            iArr[AppInterface.CommandType.DISPLAY_TEXT.ordinal()] = 3;
        } catch (NoSuchFieldError e6) {
        }
        try {
            iArr[AppInterface.CommandType.GET_CHANNEL_STATUS.ordinal()] = 4;
        } catch (NoSuchFieldError e7) {
        }
        try {
            iArr[AppInterface.CommandType.GET_FRAME_STATUS.ordinal()] = 26;
        } catch (NoSuchFieldError e8) {
        }
        try {
            iArr[AppInterface.CommandType.GET_INKEY.ordinal()] = 5;
        } catch (NoSuchFieldError e9) {
        }
        try {
            iArr[AppInterface.CommandType.GET_INPUT.ordinal()] = 6;
        } catch (NoSuchFieldError e10) {
        }
        try {
            iArr[AppInterface.CommandType.GET_READER_STATUS.ordinal()] = 27;
        } catch (NoSuchFieldError e11) {
        }
        try {
            iArr[AppInterface.CommandType.GET_SERVICE_INFORMATION.ordinal()] = 28;
        } catch (NoSuchFieldError e12) {
        }
        try {
            iArr[AppInterface.CommandType.LANGUAGE_NOTIFICATION.ordinal()] = 29;
        } catch (NoSuchFieldError e13) {
        }
        try {
            iArr[AppInterface.CommandType.LAUNCH_BROWSER.ordinal()] = 7;
        } catch (NoSuchFieldError e14) {
        }
        try {
            iArr[AppInterface.CommandType.MORE_TIME.ordinal()] = 30;
        } catch (NoSuchFieldError e15) {
        }
        try {
            iArr[AppInterface.CommandType.OPEN_CHANNEL.ordinal()] = 8;
        } catch (NoSuchFieldError e16) {
        }
        try {
            iArr[AppInterface.CommandType.PERFORM_CARD_APDU.ordinal()] = 31;
        } catch (NoSuchFieldError e17) {
        }
        try {
            iArr[AppInterface.CommandType.PLAY_TONE.ordinal()] = 9;
        } catch (NoSuchFieldError e18) {
        }
        try {
            iArr[AppInterface.CommandType.POLLING_OFF.ordinal()] = 32;
        } catch (NoSuchFieldError e19) {
        }
        try {
            iArr[AppInterface.CommandType.POLL_INTERVAL.ordinal()] = 33;
        } catch (NoSuchFieldError e20) {
        }
        try {
            iArr[AppInterface.CommandType.POWER_OFF_CARD.ordinal()] = 34;
        } catch (NoSuchFieldError e21) {
        }
        try {
            iArr[AppInterface.CommandType.POWER_ON_CARD.ordinal()] = 35;
        } catch (NoSuchFieldError e22) {
        }
        try {
            iArr[AppInterface.CommandType.PROVIDE_LOCAL_INFORMATION.ordinal()] = 10;
        } catch (NoSuchFieldError e23) {
        }
        try {
            iArr[AppInterface.CommandType.RECEIVE_DATA.ordinal()] = 11;
        } catch (NoSuchFieldError e24) {
        }
        try {
            iArr[AppInterface.CommandType.REFRESH.ordinal()] = 12;
        } catch (NoSuchFieldError e25) {
        }
        try {
            iArr[AppInterface.CommandType.RETRIEVE_MULTIMEDIA_MESSAGE.ordinal()] = 36;
        } catch (NoSuchFieldError e26) {
        }
        try {
            iArr[AppInterface.CommandType.RUN_AT_COMMAND.ordinal()] = 37;
        } catch (NoSuchFieldError e27) {
        }
        try {
            iArr[AppInterface.CommandType.SELECT_ITEM.ordinal()] = 13;
        } catch (NoSuchFieldError e28) {
        }
        try {
            iArr[AppInterface.CommandType.SEND_DATA.ordinal()] = 14;
        } catch (NoSuchFieldError e29) {
        }
        try {
            iArr[AppInterface.CommandType.SEND_DTMF.ordinal()] = 15;
        } catch (NoSuchFieldError e30) {
        }
        try {
            iArr[AppInterface.CommandType.SEND_SMS.ordinal()] = 16;
        } catch (NoSuchFieldError e31) {
        }
        try {
            iArr[AppInterface.CommandType.SEND_SS.ordinal()] = 17;
        } catch (NoSuchFieldError e32) {
        }
        try {
            iArr[AppInterface.CommandType.SEND_USSD.ordinal()] = 18;
        } catch (NoSuchFieldError e33) {
        }
        try {
            iArr[AppInterface.CommandType.SERVICE_SEARCH.ordinal()] = 38;
        } catch (NoSuchFieldError e34) {
        }
        try {
            iArr[AppInterface.CommandType.SET_FRAME.ordinal()] = 39;
        } catch (NoSuchFieldError e35) {
        }
        try {
            iArr[AppInterface.CommandType.SET_UP_CALL.ordinal()] = 19;
        } catch (NoSuchFieldError e36) {
        }
        try {
            iArr[AppInterface.CommandType.SET_UP_EVENT_LIST.ordinal()] = 20;
        } catch (NoSuchFieldError e37) {
        }
        try {
            iArr[AppInterface.CommandType.SET_UP_IDLE_MODE_TEXT.ordinal()] = 21;
        } catch (NoSuchFieldError e38) {
        }
        try {
            iArr[AppInterface.CommandType.SET_UP_MENU.ordinal()] = 22;
        } catch (NoSuchFieldError e39) {
        }
        try {
            iArr[AppInterface.CommandType.SUBMIT_MULTIMEDIA_MESSAGE.ordinal()] = 40;
        } catch (NoSuchFieldError e40) {
        }
        try {
            iArr[AppInterface.CommandType.TIMER_MANAGEMENT.ordinal()] = 41;
        } catch (NoSuchFieldError e41) {
        }
        f19x72eb89a2 = iArr;
        return iArr;
    }

    static synchronized CommandParamsFactory getInstance(RilMessageDecoder caller, IccFileHandler fh) {
        if (sInstance != null) {
            return sInstance;
        }
        if (fh == null) {
            return null;
        }
        return new CommandParamsFactory(caller, fh);
    }

    static synchronized CommandParamsFactory getInstance(RilMessageDecoder caller, IccFileHandler fh, Context context) {
        if (sInstance != null) {
            return sInstance;
        }
        if (fh == null || context == null) {
            return null;
        }
        return new CommandParamsFactory(caller, fh, context);
    }

    private CommandParamsFactory(RilMessageDecoder caller, IccFileHandler fh, Context context) {
        this.mCaller = null;
        this.mCaller = caller;
        this.mIconLoader = IconLoader.getInstance(this, fh, this.mCaller.getSlotId());
        this.mContext = context;
    }

    private CommandParamsFactory(RilMessageDecoder caller, IccFileHandler fh) {
        this.mCaller = null;
        this.mCaller = caller;
        this.mIconLoader = IconLoader.getInstance(this, fh, this.mCaller.getSlotId());
    }

    private CommandDetails processCommandDetails(List<ComprehensionTlv> ctlvs) throws ResultException {
        ComprehensionTlv ctlvCmdDet;
        if (ctlvs == null || (ctlvCmdDet = searchForTag(ComprehensionTlvTag.COMMAND_DETAILS, ctlvs)) == null) {
            return null;
        }
        try {
            CommandDetails cmdDet = ValueParser.retrieveCommandDetails(ctlvCmdDet);
            return cmdDet;
        } catch (ResultException e) {
            CatLog.d(this, "Failed to procees command details");
            throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
        }
    }

    void make(BerTlv berTlv) {
        boolean cmdPending;
        if (berTlv == null) {
            return;
        }
        this.mCmdParams = null;
        this.mIconLoadState = 0;
        if (berTlv.getTag() != 208) {
            CatLog.e(this, "CPF-make: Ununderstood proactive command tag");
            sendCmdParams(ResultCode.CMD_TYPE_NOT_UNDERSTOOD);
            return;
        }
        List<ComprehensionTlv> ctlvs = berTlv.getComprehensionTlvs();
        try {
            CommandDetails cmdDet = processCommandDetails(ctlvs);
            if (cmdDet == null) {
                CatLog.e(this, "CPF-make: No CommandDetails object");
                sendCmdParams(ResultCode.CMD_TYPE_NOT_UNDERSTOOD);
                return;
            }
            AppInterface.CommandType cmdType = AppInterface.CommandType.fromInt(cmdDet.typeOfCommand);
            if (cmdType == null) {
                CatLog.d(this, "CPF-make: Command type can't be found");
                this.mCmdParams = new CommandParams(cmdDet);
                sendCmdParams(ResultCode.BEYOND_TERMINAL_CAPABILITY);
                return;
            }
            if (!berTlv.isLengthValid()) {
                this.mCmdParams = new CommandParams(cmdDet);
                sendCmdParams(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
                return;
            }
            try {
                switch (m239xe796fd46()[cmdType.ordinal()]) {
                    case 1:
                        cmdPending = processActivate(cmdDet, ctlvs);
                        break;
                    case 2:
                    case 8:
                    case 11:
                    case 14:
                        cmdPending = processBIPClient(cmdDet, ctlvs);
                        break;
                    case 3:
                        cmdPending = processDisplayText(cmdDet, ctlvs);
                        break;
                    case 4:
                    case 19:
                        cmdPending = processSetupCall(cmdDet, ctlvs);
                        break;
                    case 5:
                        cmdPending = processGetInkey(cmdDet, ctlvs);
                        break;
                    case 6:
                        cmdPending = processGetInput(cmdDet, ctlvs);
                        break;
                    case 7:
                        cmdPending = processLaunchBrowser(cmdDet, ctlvs);
                        break;
                    case 9:
                        cmdPending = processPlayTone(cmdDet, ctlvs);
                        break;
                    case 10:
                        cmdPending = processProvideLocalInfo(cmdDet, ctlvs);
                        CatLog.d(this, "process ProvideLocalInformation");
                        break;
                    case 12:
                        processRefresh(cmdDet, ctlvs);
                        cmdPending = false;
                        break;
                    case 13:
                        cmdPending = processSelectItem(cmdDet, ctlvs);
                        break;
                    case 15:
                    case 16:
                    case 17:
                    case 18:
                        cmdPending = processEventNotify(cmdDet, ctlvs);
                        break;
                    case 20:
                        cmdPending = processSetUpEventList(cmdDet, ctlvs);
                        break;
                    case 21:
                        cmdPending = processSetUpIdleModeText(cmdDet, ctlvs);
                        break;
                    case 22:
                        cmdPending = processSelectItem(cmdDet, ctlvs);
                        break;
                    default:
                        this.mCmdParams = new CommandParams(cmdDet);
                        CatLog.d(this, "CPF-make: default case");
                        sendCmdParams(ResultCode.BEYOND_TERMINAL_CAPABILITY);
                        return;
                }
                if (cmdPending) {
                    return;
                }
                sendCmdParams(ResultCode.OK);
            } catch (ResultException e) {
                CatLog.d(this, "make: caught ResultException e=" + e);
                this.mCmdParams = new CommandParams(cmdDet);
                sendCmdParams(e.result());
            }
        } catch (ResultException e2) {
            CatLog.e(this, "CPF-make: Except to procees command details : " + e2.result());
            sendCmdParams(e2.result());
        }
    }

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case 1:
                sendCmdParams(setIcons(msg.obj));
                break;
        }
    }

    private ResultCode setIcons(Object data) {
        if (data == null) {
            CatLog.d(this, "Optional Icon data is NULL");
            this.mCmdParams.mLoadIconFailed = true;
            this.mloadIcon = false;
            return ResultCode.OK;
        }
        switch (this.mIconLoadState) {
            case 1:
                this.mCmdParams.setIcon((Bitmap) data);
                break;
            case 2:
                Bitmap[] icons = (Bitmap[]) data;
                for (Bitmap icon : icons) {
                    this.mCmdParams.setIcon(icon);
                    if (icon == null && this.mloadIcon) {
                        CatLog.d(this, "Optional Icon data is NULL while loading multi icons");
                        this.mCmdParams.mLoadIconFailed = true;
                    }
                }
                break;
        }
        return ResultCode.OK;
    }

    private void sendCmdParams(ResultCode resCode) {
        if (this.mCaller == null) {
            return;
        }
        this.mCaller.sendMsgParamsDecoded(resCode, this.mCmdParams);
    }

    private ComprehensionTlv searchForTag(ComprehensionTlvTag tag, List<ComprehensionTlv> ctlvs) {
        Iterator<ComprehensionTlv> iter = ctlvs.iterator();
        return searchForNextTag(tag, iter);
    }

    private ComprehensionTlv searchForNextTag(ComprehensionTlvTag tag, Iterator<ComprehensionTlv> iter) {
        int tagValue = tag.value();
        while (iter.hasNext()) {
            ComprehensionTlv ctlv = iter.next();
            if (ctlv.getTag() == tagValue) {
                return ctlv;
            }
        }
        return null;
    }

    private void resetTlvIndex() {
        this.tlvIndex = -1;
    }

    private ComprehensionTlv searchForNextTagAndIndex(ComprehensionTlvTag tag, Iterator<ComprehensionTlv> iter) {
        if (tag == null || iter == null) {
            CatLog.d(this, "CPF-searchForNextTagAndIndex: Invalid params");
            return null;
        }
        int tagValue = tag.value();
        while (iter.hasNext()) {
            this.tlvIndex++;
            ComprehensionTlv ctlv = iter.next();
            if (ctlv.getTag() == tagValue) {
                return ctlv;
            }
        }
        return null;
    }

    private ComprehensionTlv searchForTagAndIndex(ComprehensionTlvTag tag, List<ComprehensionTlv> ctlvs) {
        resetTlvIndex();
        Iterator<ComprehensionTlv> iter = ctlvs.iterator();
        return searchForNextTagAndIndex(tag, iter);
    }

    private boolean processDisplayText(CommandDetails cmdDet, List<ComprehensionTlv> ctlvs) throws ResultException {
        CatLog.d(this, "process DisplayText");
        TextMessage textMsg = new TextMessage();
        IconId iconId = null;
        ComprehensionTlv ctlv = searchForTag(ComprehensionTlvTag.TEXT_STRING, ctlvs);
        if (ctlv != null) {
            textMsg.text = ValueParser.retrieveTextString(ctlv);
        }
        if (textMsg.text == null) {
            throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
        }
        if (searchForTag(ComprehensionTlvTag.IMMEDIATE_RESPONSE, ctlvs) != null) {
            textMsg.responseNeeded = false;
        }
        ComprehensionTlv ctlv2 = searchForTag(ComprehensionTlvTag.ICON_ID, ctlvs);
        if (ctlv2 != null) {
            try {
                iconId = ValueParser.retrieveIconId(ctlv2);
            } catch (ResultException e) {
                CatLog.e(this, "retrieveIconId ResultException: " + e.result());
            }
            try {
                textMsg.iconSelfExplanatory = iconId.selfExplanatory;
            } catch (NullPointerException e2) {
                CatLog.e(this, "iconId is null.");
            }
        }
        ComprehensionTlv ctlv3 = searchForTag(ComprehensionTlvTag.DURATION, ctlvs);
        if (ctlv3 != null) {
            try {
                textMsg.duration = ValueParser.retrieveDuration(ctlv3);
            } catch (ResultException e3) {
                CatLog.e(this, "retrieveDuration ResultException: " + e3.result());
            }
        }
        textMsg.isHighPriority = (cmdDet.commandQualifier & 1) != 0;
        textMsg.userClear = (cmdDet.commandQualifier & 128) != 0;
        this.mCmdParams = new DisplayTextParams(cmdDet, textMsg);
        if (iconId == null) {
            return false;
        }
        this.mloadIcon = true;
        this.mIconLoadState = 1;
        this.mIconLoader.loadIcon(iconId.recordNumber, obtainMessage(1));
        return true;
    }

    private boolean processSetUpIdleModeText(CommandDetails cmdDet, List<ComprehensionTlv> ctlvs) throws ResultException {
        CatLog.d(this, "process SetUpIdleModeText");
        TextMessage textMsg = new TextMessage();
        IconId iconId = null;
        ComprehensionTlv ctlv = searchForTag(ComprehensionTlvTag.TEXT_STRING, ctlvs);
        if (ctlv != null) {
            textMsg.text = ValueParser.retrieveTextString(ctlv);
        }
        ComprehensionTlv ctlv2 = searchForTag(ComprehensionTlvTag.ICON_ID, ctlvs);
        if (ctlv2 != null) {
            iconId = ValueParser.retrieveIconId(ctlv2);
            textMsg.iconSelfExplanatory = iconId.selfExplanatory;
        }
        if (textMsg.text == null && iconId != null && !textMsg.iconSelfExplanatory) {
            throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
        }
        this.mCmdParams = new DisplayTextParams(cmdDet, textMsg);
        if (iconId != null) {
            this.mloadIcon = true;
            this.mIconLoadState = 1;
            this.mIconLoader.loadIcon(iconId.recordNumber, obtainMessage(1));
            return true;
        }
        return false;
    }

    private boolean processGetInkey(CommandDetails cmdDet, List<ComprehensionTlv> ctlvs) throws ResultException {
        CatLog.d(this, "process GetInkey");
        Input input = new Input();
        IconId iconId = null;
        ComprehensionTlv ctlv = searchForTag(ComprehensionTlvTag.TEXT_STRING, ctlvs);
        if (ctlv != null) {
            input.text = ValueParser.retrieveTextString(ctlv);
            ComprehensionTlv ctlv2 = searchForTag(ComprehensionTlvTag.ICON_ID, ctlvs);
            if (ctlv2 != null) {
                try {
                    iconId = ValueParser.retrieveIconId(ctlv2);
                } catch (ResultException e) {
                    CatLog.e(this, "retrieveIconId ResultException: " + e.result());
                }
                try {
                    input.iconSelfExplanatory = iconId.selfExplanatory;
                } catch (NullPointerException e2) {
                    CatLog.e(this, "iconId is null.");
                }
            }
            ComprehensionTlv ctlv3 = searchForTag(ComprehensionTlvTag.DURATION, ctlvs);
            if (ctlv3 != null) {
                try {
                    input.duration = ValueParser.retrieveDuration(ctlv3);
                } catch (ResultException e3) {
                    CatLog.e(this, "retrieveDuration ResultException: " + e3.result());
                }
            }
            input.minLen = 1;
            input.maxLen = 1;
            input.digitOnly = (cmdDet.commandQualifier & 1) == 0;
            input.ucs2 = (cmdDet.commandQualifier & 2) != 0;
            input.yesNo = (cmdDet.commandQualifier & 4) != 0;
            input.helpAvailable = (cmdDet.commandQualifier & 128) != 0;
            input.echo = true;
            this.mCmdParams = new GetInputParams(cmdDet, input);
            if (iconId == null) {
                return false;
            }
            this.mloadIcon = true;
            this.mIconLoadState = 1;
            this.mIconLoader.loadIcon(iconId.recordNumber, obtainMessage(1));
            return true;
        }
        throw new ResultException(ResultCode.REQUIRED_VALUES_MISSING);
    }

    private boolean processGetInput(CommandDetails cmdDet, List<ComprehensionTlv> ctlvs) throws ResultException {
        CatLog.d(this, "process GetInput");
        Input input = new Input();
        IconId iconId = null;
        ComprehensionTlv ctlv = searchForTag(ComprehensionTlvTag.TEXT_STRING, ctlvs);
        if (ctlv != null) {
            input.text = ValueParser.retrieveTextString(ctlv);
            ComprehensionTlv ctlv2 = searchForTag(ComprehensionTlvTag.RESPONSE_LENGTH, ctlvs);
            if (ctlv2 != null) {
                try {
                    byte[] rawValue = ctlv2.getRawValue();
                    int valueIndex = ctlv2.getValueIndex();
                    input.minLen = rawValue[valueIndex] & PplMessageManager.Type.INVALID;
                    if (input.minLen > MAX_GSM7_DEFAULT_CHARS) {
                        input.minLen = MAX_GSM7_DEFAULT_CHARS;
                    }
                    input.maxLen = rawValue[valueIndex + 1] & PplMessageManager.Type.INVALID;
                    if (input.maxLen > MAX_GSM7_DEFAULT_CHARS) {
                        input.maxLen = MAX_GSM7_DEFAULT_CHARS;
                    }
                    ComprehensionTlv ctlv3 = searchForTag(ComprehensionTlvTag.DEFAULT_TEXT, ctlvs);
                    if (ctlv3 != null) {
                        try {
                            input.defaultText = ValueParser.retrieveTextString(ctlv3);
                        } catch (ResultException e) {
                            CatLog.e(this, "retrieveTextString ResultException: " + e.result());
                        }
                    }
                    ComprehensionTlv ctlv4 = searchForTag(ComprehensionTlvTag.ICON_ID, ctlvs);
                    if (ctlv4 != null) {
                        try {
                            iconId = ValueParser.retrieveIconId(ctlv4);
                        } catch (ResultException e2) {
                            CatLog.e(this, "retrieveIconId ResultException: " + e2.result());
                        }
                        try {
                            input.iconSelfExplanatory = iconId.selfExplanatory;
                        } catch (NullPointerException e3) {
                            CatLog.e(this, "iconId is null.");
                        }
                    }
                    input.digitOnly = (cmdDet.commandQualifier & 1) == 0;
                    input.ucs2 = (cmdDet.commandQualifier & 2) != 0;
                    input.echo = (cmdDet.commandQualifier & 4) == 0;
                    input.packed = (cmdDet.commandQualifier & 8) != 0;
                    input.helpAvailable = (cmdDet.commandQualifier & 128) != 0;
                    if (input.ucs2 && input.maxLen > MAX_UCS2_CHARS) {
                        CatLog.d(this, "UCS2: received maxLen = " + input.maxLen + ", truncating to " + MAX_UCS2_CHARS);
                        input.maxLen = MAX_UCS2_CHARS;
                    } else if (!input.packed && input.maxLen > MAX_GSM7_DEFAULT_CHARS) {
                        CatLog.d(this, "GSM 7Bit Default: received maxLen = " + input.maxLen + ", truncating to " + MAX_GSM7_DEFAULT_CHARS);
                        input.maxLen = MAX_GSM7_DEFAULT_CHARS;
                    }
                    this.mCmdParams = new GetInputParams(cmdDet, input);
                    if (iconId != null) {
                        this.mloadIcon = true;
                        this.mIconLoadState = 1;
                        this.mIconLoader.loadIcon(iconId.recordNumber, obtainMessage(1));
                        return true;
                    }
                    return false;
                } catch (IndexOutOfBoundsException e4) {
                    throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
                }
            }
            throw new ResultException(ResultCode.REQUIRED_VALUES_MISSING);
        }
        throw new ResultException(ResultCode.REQUIRED_VALUES_MISSING);
    }

    private boolean processRefresh(CommandDetails cmdDet, List<ComprehensionTlv> ctlvs) {
        CatLog.d(this, "process Refresh");
        TextMessage textMsg = new TextMessage();
        switch (cmdDet.commandQualifier) {
            case 0:
            case 2:
            case 3:
            case 4:
                textMsg.text = null;
                this.mCmdParams = new DisplayTextParams(cmdDet, textMsg);
                break;
        }
        return false;
    }

    private boolean processSelectItem(CommandDetails cmdDet, List<ComprehensionTlv> ctlvs) throws ResultException {
        CatLog.d(this, "process SelectItem");
        Menu menu = new Menu();
        IconId titleIconId = null;
        ItemsIconId itemsIconId = null;
        Iterator<ComprehensionTlv> iter = ctlvs.iterator();
        ComprehensionTlv ctlv = searchForTag(ComprehensionTlvTag.ALPHA_ID, ctlvs);
        if (ctlv != null) {
            try {
                menu.title = ValueParser.retrieveAlphaId(ctlv);
            } catch (ResultException e) {
                CatLog.e(this, "retrieveAlphaId ResultException: " + e.result());
            }
            CatLog.d(this, "add AlphaId: " + menu.title);
        }
        while (true) {
            ComprehensionTlv ctlv2 = searchForNextTag(ComprehensionTlvTag.ITEM, iter);
            if (ctlv2 == null) {
                break;
            }
            Item item = ValueParser.retrieveItem(ctlv2);
            CatLog.d(this, "add menu item: " + (item == null ? UsimPBMemInfo.STRING_NOT_SET : item.toString()));
            menu.items.add(item);
        }
        if (menu.items.size() == 0) {
            CatLog.d(this, "no menu item");
            throw new ResultException(ResultCode.REQUIRED_VALUES_MISSING);
        }
        ComprehensionTlv ctlv3 = searchForTag(ComprehensionTlvTag.NEXT_ACTION_INDICATOR, ctlvs);
        if (ctlv3 != null) {
            try {
                menu.nextActionIndicator = ValueParser.retrieveNextActionIndicator(ctlv3);
            } catch (ResultException e2) {
                CatLog.e(this, "retrieveNextActionIndicator ResultException: " + e2.result());
            }
            try {
                if (menu.nextActionIndicator.length != menu.items.size()) {
                    CatLog.d(this, "nextActionIndicator.length != number of menu items");
                    menu.nextActionIndicator = null;
                }
            } catch (NullPointerException e3) {
                CatLog.e(this, "nextActionIndicator is null.");
            }
        }
        ComprehensionTlv ctlv4 = searchForTag(ComprehensionTlvTag.ITEM_ID, ctlvs);
        if (ctlv4 != null) {
            try {
                menu.defaultItem = ValueParser.retrieveItemId(ctlv4) - 1;
            } catch (ResultException e4) {
                CatLog.e(this, "retrieveItemId ResultException: " + e4.result());
            }
            CatLog.d(this, "default item: " + menu.defaultItem);
        }
        ComprehensionTlv ctlv5 = searchForTag(ComprehensionTlvTag.ICON_ID, ctlvs);
        if (ctlv5 != null) {
            this.mIconLoadState = 1;
            try {
                titleIconId = ValueParser.retrieveIconId(ctlv5);
            } catch (ResultException e5) {
                CatLog.e(this, "retrieveIconId ResultException: " + e5.result());
            }
            try {
                menu.titleIconSelfExplanatory = titleIconId.selfExplanatory;
            } catch (NullPointerException e6) {
                CatLog.e(this, "titleIconId is null.");
            }
        }
        ComprehensionTlv ctlv6 = searchForTag(ComprehensionTlvTag.ITEM_ICON_ID_LIST, ctlvs);
        if (ctlv6 != null) {
            this.mIconLoadState = 2;
            try {
                itemsIconId = ValueParser.retrieveItemsIconId(ctlv6);
            } catch (ResultException e7) {
                CatLog.e(this, "retrieveItemsIconId ResultException: " + e7.result());
            }
            try {
                menu.itemsIconSelfExplanatory = itemsIconId.selfExplanatory;
            } catch (NullPointerException e8) {
                CatLog.e(this, "itemsIconId is null.");
            }
        }
        boolean presentTypeSpecified = (cmdDet.commandQualifier & 1) != 0;
        if (presentTypeSpecified) {
            if ((cmdDet.commandQualifier & 2) == 0) {
                menu.presentationType = PresentationType.DATA_VALUES;
            } else {
                menu.presentationType = PresentationType.NAVIGATION_OPTIONS;
            }
        }
        menu.softKeyPreferred = (cmdDet.commandQualifier & 4) != 0;
        menu.helpAvailable = (cmdDet.commandQualifier & 128) != 0;
        this.mCmdParams = new SelectItemParams(cmdDet, menu, titleIconId != null);
        switch (this.mIconLoadState) {
            case 0:
                return false;
            case 1:
                if (titleIconId != null && titleIconId.recordNumber > 0) {
                    this.mloadIcon = true;
                    this.mIconLoader.loadIcon(titleIconId.recordNumber, obtainMessage(1));
                    return true;
                }
                return false;
            case 2:
                if (itemsIconId != null) {
                    int[] recordNumbers = itemsIconId.recordNumbers;
                    if (titleIconId != null) {
                        recordNumbers = new int[itemsIconId.recordNumbers.length + 1];
                        recordNumbers[0] = titleIconId.recordNumber;
                        System.arraycopy(itemsIconId.recordNumbers, 0, recordNumbers, 1, itemsIconId.recordNumbers.length);
                    }
                    this.mloadIcon = true;
                    this.mIconLoader.loadIcons(recordNumbers, obtainMessage(1));
                    return true;
                }
                return false;
            default:
                return true;
        }
    }

    private boolean processEventNotify(CommandDetails cmdDet, List<ComprehensionTlv> ctlvs) throws ResultException {
        CatLog.d(this, "process EventNotify");
        TextMessage textMsg = new TextMessage();
        IconId iconId = null;
        ComprehensionTlv ctlv = searchForTag(ComprehensionTlvTag.ALPHA_ID, ctlvs);
        if (ctlv != null) {
            textMsg.text = ValueParser.retrieveAlphaId(ctlv);
        } else {
            textMsg.text = null;
        }
        ComprehensionTlv ctlv2 = searchForTag(ComprehensionTlvTag.ICON_ID, ctlvs);
        if (ctlv2 != null) {
            iconId = ValueParser.retrieveIconId(ctlv2);
            textMsg.iconSelfExplanatory = iconId.selfExplanatory;
        }
        textMsg.responseNeeded = false;
        this.mCmdParams = new DisplayTextParams(cmdDet, textMsg);
        if (iconId == null) {
            return false;
        }
        this.mloadIcon = true;
        this.mIconLoadState = 1;
        this.mIconLoader.loadIcon(iconId.recordNumber, obtainMessage(1));
        return true;
    }

    private boolean processSetUpEventList(CommandDetails cmdDet, List<ComprehensionTlv> ctlvs) throws ResultException {
        CatLog.d(this, "process SetUpEventList");
        ComprehensionTlv ctlv = searchForTag(ComprehensionTlvTag.EVENT_LIST, ctlvs);
        if (ctlv != null) {
            try {
                byte[] rawValue = ctlv.getRawValue();
                int valueIndex = ctlv.getValueIndex();
                int valueLen = ctlv.getLength();
                byte[] eventList = new byte[valueLen];
                int index = 0;
                while (index < valueLen) {
                    eventList[index] = rawValue[valueIndex];
                    CatLog.v(this, "CPF-processSetUpEventList: eventList[" + index + "] = " + ((int) eventList[index]));
                    if (rawValue[valueIndex] == 5) {
                        CatLog.v(this, "CPF-processSetUpEventList: sent intent with idle = true");
                        Intent intent = new Intent("android.intent.action.IDLE_SCREEN_NEEDED");
                        intent.putExtra("_enable", true);
                        this.mContext.sendBroadcast(intent);
                    } else if (rawValue[valueIndex] == 4) {
                        CatLog.v(this, "CPF-processSetUpEventList: sent intent for user activity");
                        Intent intent2 = new Intent("android.intent.action.stk.USER_ACTIVITY.enable");
                        intent2.putExtra("state", true);
                        this.mContext.sendBroadcast(intent2);
                    }
                    index++;
                    valueIndex++;
                }
                this.mCmdParams = new SetupEventListParams(cmdDet, eventList);
                return false;
            } catch (IndexOutOfBoundsException e) {
                throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
            }
        }
        return false;
    }

    private boolean processLaunchBrowser(CommandDetails cmdDet, List<ComprehensionTlv> ctlvs) throws ResultException {
        LaunchBrowserMode mode;
        CatLog.d(this, "process LaunchBrowser");
        TextMessage confirmMsg = new TextMessage();
        IconId iconId = null;
        String url = null;
        ComprehensionTlv ctlv = searchForTag(ComprehensionTlvTag.URL, ctlvs);
        if (ctlv != null) {
            try {
                byte[] rawValue = ctlv.getRawValue();
                int valueIndex = ctlv.getValueIndex();
                int valueLen = ctlv.getLength();
                if (valueLen > 0) {
                    url = GsmAlphabet.gsm8BitUnpackedToString(rawValue, valueIndex, valueLen);
                } else {
                    url = null;
                }
            } catch (IndexOutOfBoundsException e) {
                throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
            }
        }
        ComprehensionTlv ctlv2 = searchForTag(ComprehensionTlvTag.ALPHA_ID, ctlvs);
        if (ctlv2 != null) {
            confirmMsg.text = ValueParser.retrieveAlphaId(ctlv2);
        }
        ComprehensionTlv ctlv3 = searchForTag(ComprehensionTlvTag.ICON_ID, ctlvs);
        if (ctlv3 != null) {
            iconId = ValueParser.retrieveIconId(ctlv3);
            confirmMsg.iconSelfExplanatory = iconId.selfExplanatory;
        }
        switch (cmdDet.commandQualifier) {
            case 2:
                mode = LaunchBrowserMode.USE_EXISTING_BROWSER;
                break;
            case 3:
                mode = LaunchBrowserMode.LAUNCH_NEW_BROWSER;
                break;
            default:
                mode = LaunchBrowserMode.LAUNCH_IF_NOT_ALREADY_LAUNCHED;
                break;
        }
        this.mCmdParams = new LaunchBrowserParams(cmdDet, confirmMsg, url, mode);
        if (iconId == null) {
            return false;
        }
        this.mIconLoadState = 1;
        this.mIconLoader.loadIcon(iconId.recordNumber, obtainMessage(1));
        return true;
    }

    private boolean processPlayTone(CommandDetails cmdDet, List<ComprehensionTlv> ctlvs) throws ResultException {
        CatLog.d(this, "process PlayTone");
        Tone tone = null;
        TextMessage textMsg = new TextMessage();
        Duration duration = null;
        IconId iconId = null;
        ComprehensionTlv ctlv = searchForTag(ComprehensionTlvTag.TONE, ctlvs);
        if (ctlv != null && ctlv.getLength() > 0) {
            try {
                byte[] rawValue = ctlv.getRawValue();
                int valueIndex = ctlv.getValueIndex();
                int toneVal = rawValue[valueIndex];
                tone = Tone.fromInt(toneVal);
            } catch (IndexOutOfBoundsException e) {
                throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
            }
        }
        ComprehensionTlv ctlv2 = searchForTag(ComprehensionTlvTag.ALPHA_ID, ctlvs);
        if (ctlv2 != null) {
            try {
                textMsg.text = ValueParser.retrieveAlphaId(ctlv2);
            } catch (ResultException e2) {
                CatLog.e(this, "retrieveAlphaId ResultException: " + e2.result());
            }
        }
        ComprehensionTlv ctlv3 = searchForTag(ComprehensionTlvTag.DURATION, ctlvs);
        if (ctlv3 != null) {
            try {
                duration = ValueParser.retrieveDuration(ctlv3);
            } catch (ResultException e3) {
                CatLog.e(this, "retrieveDuration ResultException: " + e3.result());
            }
        }
        ComprehensionTlv ctlv4 = searchForTag(ComprehensionTlvTag.ICON_ID, ctlvs);
        if (ctlv4 != null) {
            iconId = ValueParser.retrieveIconId(ctlv4);
            textMsg.iconSelfExplanatory = iconId.selfExplanatory;
        }
        boolean vibrate = (cmdDet.commandQualifier & 1) != 0;
        textMsg.responseNeeded = false;
        this.mCmdParams = new PlayToneParams(cmdDet, textMsg, tone, duration, vibrate);
        if (iconId != null) {
            this.mIconLoadState = 1;
            this.mIconLoader.loadIcon(iconId.recordNumber, obtainMessage(1));
            return true;
        }
        return false;
    }

    private boolean processSetupCall(CommandDetails cmdDet, List<ComprehensionTlv> ctlvs) throws ResultException {
        CatLog.d(this, "process SetupCall");
        ctlvs.iterator();
        TextMessage confirmMsg = new TextMessage();
        TextMessage callMsg = new TextMessage();
        IconId confirmIconId = null;
        IconId callIconId = null;
        int addrIndex = getAddrIndex(ctlvs);
        if (-1 == addrIndex) {
            CatLog.d(this, "fail to get ADDRESS data object");
            return false;
        }
        int alpha1Index = getConfirmationAlphaIdIndex(ctlvs, addrIndex);
        int alpha2Index = getCallingAlphaIdIndex(ctlvs, addrIndex);
        ComprehensionTlv ctlv = getConfirmationAlphaId(ctlvs, addrIndex);
        if (ctlv != null) {
            confirmMsg.text = ValueParser.retrieveAlphaId(ctlv);
        }
        ComprehensionTlv ctlv2 = getConfirmationIconId(ctlvs, alpha1Index, alpha2Index);
        if (ctlv2 != null) {
            confirmIconId = ValueParser.retrieveIconId(ctlv2);
            confirmMsg.iconSelfExplanatory = confirmIconId.selfExplanatory;
        }
        ComprehensionTlv ctlv3 = getCallingAlphaId(ctlvs, addrIndex);
        if (ctlv3 != null) {
            callMsg.text = ValueParser.retrieveAlphaId(ctlv3);
        }
        ComprehensionTlv ctlv4 = getCallingIconId(ctlvs, alpha2Index);
        if (ctlv4 != null) {
            callIconId = ValueParser.retrieveIconId(ctlv4);
            callMsg.iconSelfExplanatory = callIconId.selfExplanatory;
        }
        this.mCmdParams = new CallSetupParams(cmdDet, confirmMsg, callMsg);
        if (confirmIconId != null || callIconId != null) {
            this.mIconLoadState = 2;
            int[] recordNumbers = new int[2];
            recordNumbers[0] = confirmIconId != null ? confirmIconId.recordNumber : -1;
            recordNumbers[1] = callIconId != null ? callIconId.recordNumber : -1;
            this.mIconLoader.loadIcons(recordNumbers, obtainMessage(1));
            return true;
        }
        return false;
    }

    private boolean processProvideLocalInfo(CommandDetails cmdDet, List<ComprehensionTlv> ctlvs) throws ResultException {
        CatLog.d(this, "process ProvideLocalInfo");
        switch (cmdDet.commandQualifier) {
            case 3:
                CatLog.d(this, "PLI [DTTZ_SETTING]");
                this.mCmdParams = new CommandParams(cmdDet);
                return false;
            case 4:
                CatLog.d(this, "PLI [LANGUAGE_SETTING]");
                this.mCmdParams = new CommandParams(cmdDet);
                return false;
            default:
                CatLog.d(this, "PLI[" + cmdDet.commandQualifier + "] Command Not Supported");
                this.mCmdParams = new CommandParams(cmdDet);
                throw new ResultException(ResultCode.BEYOND_TERMINAL_CAPABILITY);
        }
    }

    private boolean processActivate(CommandDetails cmdDet, List<ComprehensionTlv> ctlvs) throws ResultException {
        CatLog.d(this, "process Activate");
        int target = 0;
        ComprehensionTlv ctlv = searchForTag(ComprehensionTlvTag.ACTIVATE_DESCRIPTOR, ctlvs);
        if (ctlv != null) {
            try {
                target = ValueParser.retrieveTarget(ctlv);
                CatLog.d(this, "target: " + target);
            } catch (ResultException e) {
                throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
            }
        }
        this.mCmdParams = new ActivateParams(cmdDet, target);
        return false;
    }

    private int getAddrIndex(List<ComprehensionTlv> list) {
        int addrIndex = 0;
        for (ComprehensionTlv temp : list) {
            if (temp.getTag() == ComprehensionTlvTag.ADDRESS.value()) {
                return addrIndex;
            }
            addrIndex++;
        }
        return -1;
    }

    private int getConfirmationAlphaIdIndex(List<ComprehensionTlv> list, int addrIndex) {
        int alphaIndex = 0;
        for (ComprehensionTlv temp : list) {
            if (temp.getTag() == ComprehensionTlvTag.ALPHA_ID.value() && alphaIndex < addrIndex) {
                return alphaIndex;
            }
            alphaIndex++;
        }
        return -1;
    }

    private int getCallingAlphaIdIndex(List<ComprehensionTlv> list, int addrIndex) {
        int alphaIndex = 0;
        for (ComprehensionTlv temp : list) {
            if (temp.getTag() == ComprehensionTlvTag.ALPHA_ID.value() && alphaIndex > addrIndex) {
                return alphaIndex;
            }
            alphaIndex++;
        }
        return -1;
    }

    private ComprehensionTlv getConfirmationAlphaId(List<ComprehensionTlv> list, int addrIndex) {
        int alphaIndex = 0;
        for (ComprehensionTlv temp : list) {
            if (temp.getTag() == ComprehensionTlvTag.ALPHA_ID.value() && alphaIndex < addrIndex) {
                return temp;
            }
            alphaIndex++;
        }
        return null;
    }

    private ComprehensionTlv getCallingAlphaId(List<ComprehensionTlv> list, int addrIndex) {
        int alphaIndex = 0;
        for (ComprehensionTlv temp : list) {
            if (temp.getTag() == ComprehensionTlvTag.ALPHA_ID.value() && alphaIndex > addrIndex) {
                return temp;
            }
            alphaIndex++;
        }
        return null;
    }

    private ComprehensionTlv getConfirmationIconId(List<ComprehensionTlv> list, int alpha1Index, int alpha2Index) {
        if (-1 == alpha1Index) {
            return null;
        }
        int iconIndex = 0;
        for (ComprehensionTlv temp : list) {
            if (temp.getTag() == ComprehensionTlvTag.ICON_ID.value() && (-1 == alpha2Index || iconIndex < alpha2Index)) {
                return temp;
            }
            iconIndex++;
        }
        return null;
    }

    private ComprehensionTlv getCallingIconId(List<ComprehensionTlv> list, int alpha2Index) {
        if (-1 == alpha2Index) {
            return null;
        }
        int iconIndex = 0;
        for (ComprehensionTlv temp : list) {
            if (temp.getTag() == ComprehensionTlvTag.ICON_ID.value() && iconIndex > alpha2Index) {
                return temp;
            }
            iconIndex++;
        }
        return null;
    }

    private boolean processBIPClient(CommandDetails cmdDet, List<ComprehensionTlv> ctlvs) throws ResultException {
        AppInterface.CommandType commandType = AppInterface.CommandType.fromInt(cmdDet.typeOfCommand);
        if (commandType != null) {
            CatLog.d(this, "process " + commandType.name());
        }
        TextMessage textMsg = new TextMessage();
        IconId iconId = null;
        boolean has_alpha_id = false;
        ComprehensionTlv ctlv = searchForTag(ComprehensionTlvTag.ALPHA_ID, ctlvs);
        if (ctlv != null) {
            textMsg.text = ValueParser.retrieveAlphaId(ctlv);
            CatLog.d(this, "alpha TLV text=" + textMsg.text);
            has_alpha_id = true;
        }
        ComprehensionTlv ctlv2 = searchForTag(ComprehensionTlvTag.ICON_ID, ctlvs);
        if (ctlv2 != null) {
            iconId = ValueParser.retrieveIconId(ctlv2);
            textMsg.iconSelfExplanatory = iconId.selfExplanatory;
        }
        textMsg.responseNeeded = false;
        this.mCmdParams = new BIPClientParams(cmdDet, textMsg, has_alpha_id);
        if (iconId == null) {
            return false;
        }
        this.mIconLoadState = 1;
        this.mIconLoader.loadIcon(iconId.recordNumber, obtainMessage(1));
        return true;
    }

    public void dispose() {
        this.mIconLoader.dispose();
        this.mIconLoader = null;
        this.mCmdParams = null;
        this.mCaller = null;
        sInstance = null;
    }
}
