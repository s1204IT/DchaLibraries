package com.android.internal.telephony.cat;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import com.android.internal.telephony.GsmAlphabet;
import com.android.internal.telephony.TelephonyComponentFactory;
import com.android.internal.telephony.cat.AppInterface;
import com.android.internal.telephony.uicc.IccFileHandler;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public class CommandParamsFactory extends Handler {
    public static final int DTTZ_SETTING = 3;
    public static final int LANGUAGE_SETTING = 4;
    protected static final int LOAD_MULTI_ICONS = 2;
    protected static final int LOAD_NO_ICON = 0;
    protected static final int LOAD_SINGLE_ICON = 1;
    protected static final int MAX_GSM7_DEFAULT_CHARS = 239;
    protected static final int MAX_UCS2_CHARS = 118;
    protected static final int MSG_ID_LOAD_ICON_DONE = 1;
    static final int NON_SPECIFIC_LANGUAGE = 0;
    protected static final int REFRESH_NAA_INIT = 3;
    protected static final int REFRESH_NAA_INIT_AND_FILE_CHANGE = 2;
    protected static final int REFRESH_NAA_INIT_AND_FULL_FILE_CHANGE = 0;
    public static final int REFRESH_UICC_RESET = 4;
    static final int SPECIFIC_LANGUAGE = 1;
    protected static CommandParamsFactory sInstance = null;
    protected RilMessageDecoder mCaller;
    protected IconLoader mIconLoader;
    private String mRequestedLanguage;
    private String mSavedLanguage;
    protected CommandParams mCmdParams = null;
    protected int mIconLoadState = 0;
    protected boolean mloadIcon = false;

    static synchronized CommandParamsFactory getInstance(RilMessageDecoder rilMessageDecoder, IccFileHandler iccFileHandler) {
        if (sInstance != null) {
            return sInstance;
        }
        if (iccFileHandler != null) {
            return TelephonyComponentFactory.getInstance().makeCommandParamsFactory(rilMessageDecoder, iccFileHandler);
        }
        return null;
    }

    public CommandParamsFactory(RilMessageDecoder rilMessageDecoder, IccFileHandler iccFileHandler) {
        this.mCaller = null;
        this.mCaller = rilMessageDecoder;
        this.mIconLoader = IconLoader.getInstance(this, iccFileHandler);
    }

    private CommandDetails processCommandDetails(List<ComprehensionTlv> list) {
        ComprehensionTlv comprehensionTlvSearchForTag;
        if (list != null && (comprehensionTlvSearchForTag = searchForTag(ComprehensionTlvTag.COMMAND_DETAILS, list)) != null) {
            try {
                return ValueParser.retrieveCommandDetails(comprehensionTlvSearchForTag);
            } catch (ResultException e) {
                CatLog.d(this, "processCommandDetails: Failed to procees command details e=" + e);
            }
        }
        return null;
    }

    public void make(BerTlv berTlv) {
        if (berTlv == null) {
            return;
        }
        this.mCmdParams = null;
        boolean zProcessSelectItem = false;
        this.mIconLoadState = 0;
        if (berTlv.getTag() != 208) {
            sendCmdParams(ResultCode.CMD_TYPE_NOT_UNDERSTOOD);
            return;
        }
        List<ComprehensionTlv> comprehensionTlvs = berTlv.getComprehensionTlvs();
        CommandDetails commandDetailsProcessCommandDetails = processCommandDetails(comprehensionTlvs);
        if (commandDetailsProcessCommandDetails == null) {
            sendCmdParams(ResultCode.CMD_TYPE_NOT_UNDERSTOOD);
            return;
        }
        AppInterface.CommandType commandTypeFromInt = AppInterface.CommandType.fromInt(commandDetailsProcessCommandDetails.typeOfCommand);
        if (commandTypeFromInt == null) {
            this.mCmdParams = new CommandParams(commandDetailsProcessCommandDetails);
            sendCmdParams(ResultCode.BEYOND_TERMINAL_CAPABILITY);
            return;
        }
        if (!berTlv.isLengthValid()) {
            this.mCmdParams = new CommandParams(commandDetailsProcessCommandDetails);
            sendCmdParams(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
            return;
        }
        try {
            switch (commandTypeFromInt) {
                case SET_UP_MENU:
                    zProcessSelectItem = processSelectItem(commandDetailsProcessCommandDetails, comprehensionTlvs);
                    break;
                case SELECT_ITEM:
                    zProcessSelectItem = processSelectItem(commandDetailsProcessCommandDetails, comprehensionTlvs);
                    break;
                case DISPLAY_TEXT:
                    zProcessSelectItem = processDisplayText(commandDetailsProcessCommandDetails, comprehensionTlvs);
                    break;
                case SET_UP_IDLE_MODE_TEXT:
                    zProcessSelectItem = processSetUpIdleModeText(commandDetailsProcessCommandDetails, comprehensionTlvs);
                    break;
                case GET_INKEY:
                    zProcessSelectItem = processGetInkey(commandDetailsProcessCommandDetails, comprehensionTlvs);
                    break;
                case GET_INPUT:
                    zProcessSelectItem = processGetInput(commandDetailsProcessCommandDetails, comprehensionTlvs);
                    break;
                case SEND_DTMF:
                case SEND_SMS:
                case SEND_SS:
                case SEND_USSD:
                    zProcessSelectItem = processEventNotify(commandDetailsProcessCommandDetails, comprehensionTlvs);
                    break;
                case GET_CHANNEL_STATUS:
                case SET_UP_CALL:
                    zProcessSelectItem = processSetupCall(commandDetailsProcessCommandDetails, comprehensionTlvs);
                    break;
                case REFRESH:
                    processRefresh(commandDetailsProcessCommandDetails, comprehensionTlvs);
                    break;
                case LAUNCH_BROWSER:
                    zProcessSelectItem = processLaunchBrowser(commandDetailsProcessCommandDetails, comprehensionTlvs);
                    break;
                case PLAY_TONE:
                    zProcessSelectItem = processPlayTone(commandDetailsProcessCommandDetails, comprehensionTlvs);
                    break;
                case SET_UP_EVENT_LIST:
                    zProcessSelectItem = processSetUpEventList(commandDetailsProcessCommandDetails, comprehensionTlvs);
                    break;
                case PROVIDE_LOCAL_INFORMATION:
                    zProcessSelectItem = processProvideLocalInfo(commandDetailsProcessCommandDetails, comprehensionTlvs);
                    break;
                case LANGUAGE_NOTIFICATION:
                    zProcessSelectItem = processLanguageNotification(commandDetailsProcessCommandDetails, comprehensionTlvs);
                    break;
                case OPEN_CHANNEL:
                case CLOSE_CHANNEL:
                case RECEIVE_DATA:
                case SEND_DATA:
                    zProcessSelectItem = processBIPClient(commandDetailsProcessCommandDetails, comprehensionTlvs);
                    break;
                default:
                    this.mCmdParams = new CommandParams(commandDetailsProcessCommandDetails);
                    sendCmdParams(ResultCode.BEYOND_TERMINAL_CAPABILITY);
                    return;
            }
            if (!zProcessSelectItem) {
                sendCmdParams(ResultCode.OK);
            }
        } catch (ResultException e) {
            CatLog.d(this, "make: caught ResultException e=" + e);
            this.mCmdParams = new CommandParams(commandDetailsProcessCommandDetails);
            sendCmdParams(e.result());
        }
    }

    @Override
    public void handleMessage(Message message) {
        if (message.what == 1 && this.mIconLoader != null) {
            sendCmdParams(setIcons(message.obj));
        }
    }

    protected ResultCode setIcons(Object obj) {
        if (obj == null) {
            CatLog.d(this, "Optional Icon data is NULL");
            this.mCmdParams.mLoadIconFailed = true;
            this.mloadIcon = false;
            return ResultCode.OK;
        }
        switch (this.mIconLoadState) {
            case 1:
                this.mCmdParams.setIcon((Bitmap) obj);
                break;
            case 2:
                for (Bitmap bitmap : (Bitmap[]) obj) {
                    this.mCmdParams.setIcon(bitmap);
                    if (bitmap == null && this.mloadIcon) {
                        CatLog.d(this, "Optional Icon data is NULL while loading multi icons");
                        this.mCmdParams.mLoadIconFailed = true;
                    }
                }
                break;
        }
        return ResultCode.OK;
    }

    protected void sendCmdParams(ResultCode resultCode) {
        this.mCaller.sendMsgParamsDecoded(resultCode, this.mCmdParams);
    }

    protected ComprehensionTlv searchForTag(ComprehensionTlvTag comprehensionTlvTag, List<ComprehensionTlv> list) {
        return searchForNextTag(comprehensionTlvTag, list.iterator());
    }

    protected ComprehensionTlv searchForNextTag(ComprehensionTlvTag comprehensionTlvTag, Iterator<ComprehensionTlv> it) {
        int iValue = comprehensionTlvTag.value();
        while (it.hasNext()) {
            ComprehensionTlv next = it.next();
            if (next.getTag() == iValue) {
                return next;
            }
        }
        return null;
    }

    protected boolean processDisplayText(CommandDetails commandDetails, List<ComprehensionTlv> list) throws ResultException {
        IconId iconIdRetrieveIconId;
        CatLog.d(this, "process DisplayText");
        TextMessage textMessage = new TextMessage();
        ComprehensionTlv comprehensionTlvSearchForTag = searchForTag(ComprehensionTlvTag.TEXT_STRING, list);
        if (comprehensionTlvSearchForTag != null) {
            textMessage.text = ValueParser.retrieveTextString(comprehensionTlvSearchForTag);
        }
        if (textMessage.text == null) {
            throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
        }
        if (searchForTag(ComprehensionTlvTag.IMMEDIATE_RESPONSE, list) != null) {
            textMessage.responseNeeded = false;
        }
        ComprehensionTlv comprehensionTlvSearchForTag2 = searchForTag(ComprehensionTlvTag.ICON_ID, list);
        if (comprehensionTlvSearchForTag2 != null) {
            iconIdRetrieveIconId = ValueParser.retrieveIconId(comprehensionTlvSearchForTag2);
            textMessage.iconSelfExplanatory = iconIdRetrieveIconId.selfExplanatory;
        } else {
            iconIdRetrieveIconId = null;
        }
        ComprehensionTlv comprehensionTlvSearchForTag3 = searchForTag(ComprehensionTlvTag.DURATION, list);
        if (comprehensionTlvSearchForTag3 != null) {
            textMessage.duration = ValueParser.retrieveDuration(comprehensionTlvSearchForTag3);
        }
        textMessage.isHighPriority = (commandDetails.commandQualifier & 1) != 0;
        textMessage.userClear = (commandDetails.commandQualifier & 128) != 0;
        this.mCmdParams = new DisplayTextParams(commandDetails, textMessage);
        if (iconIdRetrieveIconId == null) {
            return false;
        }
        this.mloadIcon = true;
        this.mIconLoadState = 1;
        this.mIconLoader.loadIcon(iconIdRetrieveIconId.recordNumber, obtainMessage(1));
        return true;
    }

    private boolean processSetUpIdleModeText(CommandDetails commandDetails, List<ComprehensionTlv> list) throws ResultException {
        IconId iconIdRetrieveIconId;
        CatLog.d(this, "process SetUpIdleModeText");
        TextMessage textMessage = new TextMessage();
        ComprehensionTlv comprehensionTlvSearchForTag = searchForTag(ComprehensionTlvTag.TEXT_STRING, list);
        if (comprehensionTlvSearchForTag != null) {
            textMessage.text = ValueParser.retrieveTextString(comprehensionTlvSearchForTag);
        }
        ComprehensionTlv comprehensionTlvSearchForTag2 = searchForTag(ComprehensionTlvTag.ICON_ID, list);
        if (comprehensionTlvSearchForTag2 != null) {
            iconIdRetrieveIconId = ValueParser.retrieveIconId(comprehensionTlvSearchForTag2);
            textMessage.iconSelfExplanatory = iconIdRetrieveIconId.selfExplanatory;
        } else {
            iconIdRetrieveIconId = null;
        }
        if (textMessage.text == null && iconIdRetrieveIconId != null && !textMessage.iconSelfExplanatory) {
            throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
        }
        this.mCmdParams = new DisplayTextParams(commandDetails, textMessage);
        if (iconIdRetrieveIconId != null) {
            this.mloadIcon = true;
            this.mIconLoadState = 1;
            this.mIconLoader.loadIcon(iconIdRetrieveIconId.recordNumber, obtainMessage(1));
            return true;
        }
        return false;
    }

    protected boolean processGetInkey(CommandDetails commandDetails, List<ComprehensionTlv> list) throws ResultException {
        IconId iconIdRetrieveIconId;
        CatLog.d(this, "process GetInkey");
        Input input = new Input();
        ComprehensionTlv comprehensionTlvSearchForTag = searchForTag(ComprehensionTlvTag.TEXT_STRING, list);
        if (comprehensionTlvSearchForTag != null) {
            input.text = ValueParser.retrieveTextString(comprehensionTlvSearchForTag);
            ComprehensionTlv comprehensionTlvSearchForTag2 = searchForTag(ComprehensionTlvTag.ICON_ID, list);
            if (comprehensionTlvSearchForTag2 != null) {
                iconIdRetrieveIconId = ValueParser.retrieveIconId(comprehensionTlvSearchForTag2);
                input.iconSelfExplanatory = iconIdRetrieveIconId.selfExplanatory;
            } else {
                iconIdRetrieveIconId = null;
            }
            ComprehensionTlv comprehensionTlvSearchForTag3 = searchForTag(ComprehensionTlvTag.DURATION, list);
            if (comprehensionTlvSearchForTag3 != null) {
                input.duration = ValueParser.retrieveDuration(comprehensionTlvSearchForTag3);
            }
            input.minLen = 1;
            input.maxLen = 1;
            input.digitOnly = (commandDetails.commandQualifier & 1) == 0;
            input.ucs2 = (commandDetails.commandQualifier & 2) != 0;
            input.yesNo = (commandDetails.commandQualifier & 4) != 0;
            input.helpAvailable = (commandDetails.commandQualifier & 128) != 0;
            input.echo = true;
            this.mCmdParams = new GetInputParams(commandDetails, input);
            if (iconIdRetrieveIconId == null) {
                return false;
            }
            this.mloadIcon = true;
            this.mIconLoadState = 1;
            this.mIconLoader.loadIcon(iconIdRetrieveIconId.recordNumber, obtainMessage(1));
            return true;
        }
        throw new ResultException(ResultCode.REQUIRED_VALUES_MISSING);
    }

    protected boolean processGetInput(CommandDetails commandDetails, List<ComprehensionTlv> list) throws ResultException {
        IconId iconIdRetrieveIconId;
        CatLog.d(this, "process GetInput");
        Input input = new Input();
        ComprehensionTlv comprehensionTlvSearchForTag = searchForTag(ComprehensionTlvTag.TEXT_STRING, list);
        if (comprehensionTlvSearchForTag != null) {
            input.text = ValueParser.retrieveTextString(comprehensionTlvSearchForTag);
            ComprehensionTlv comprehensionTlvSearchForTag2 = searchForTag(ComprehensionTlvTag.RESPONSE_LENGTH, list);
            if (comprehensionTlvSearchForTag2 != null) {
                try {
                    byte[] rawValue = comprehensionTlvSearchForTag2.getRawValue();
                    int valueIndex = comprehensionTlvSearchForTag2.getValueIndex();
                    input.minLen = rawValue[valueIndex] & 255;
                    input.maxLen = rawValue[valueIndex + 1] & 255;
                    ComprehensionTlv comprehensionTlvSearchForTag3 = searchForTag(ComprehensionTlvTag.DEFAULT_TEXT, list);
                    if (comprehensionTlvSearchForTag3 != null) {
                        input.defaultText = ValueParser.retrieveTextString(comprehensionTlvSearchForTag3);
                    }
                    ComprehensionTlv comprehensionTlvSearchForTag4 = searchForTag(ComprehensionTlvTag.ICON_ID, list);
                    if (comprehensionTlvSearchForTag4 != null) {
                        iconIdRetrieveIconId = ValueParser.retrieveIconId(comprehensionTlvSearchForTag4);
                        input.iconSelfExplanatory = iconIdRetrieveIconId.selfExplanatory;
                    } else {
                        iconIdRetrieveIconId = null;
                    }
                    ComprehensionTlv comprehensionTlvSearchForTag5 = searchForTag(ComprehensionTlvTag.DURATION, list);
                    if (comprehensionTlvSearchForTag5 != null) {
                        input.duration = ValueParser.retrieveDuration(comprehensionTlvSearchForTag5);
                    }
                    input.digitOnly = (commandDetails.commandQualifier & 1) == 0;
                    input.ucs2 = (commandDetails.commandQualifier & 2) != 0;
                    input.echo = (commandDetails.commandQualifier & 4) == 0;
                    input.packed = (commandDetails.commandQualifier & 8) != 0;
                    input.helpAvailable = (commandDetails.commandQualifier & 128) != 0;
                    if (input.ucs2 && input.maxLen > 118) {
                        CatLog.d(this, "UCS2: received maxLen = " + input.maxLen + ", truncating to 118");
                        input.maxLen = 118;
                    } else if (!input.packed && input.maxLen > MAX_GSM7_DEFAULT_CHARS) {
                        CatLog.d(this, "GSM 7Bit Default: received maxLen = " + input.maxLen + ", truncating to " + MAX_GSM7_DEFAULT_CHARS);
                        input.maxLen = MAX_GSM7_DEFAULT_CHARS;
                    }
                    this.mCmdParams = new GetInputParams(commandDetails, input);
                    if (iconIdRetrieveIconId == null) {
                        return false;
                    }
                    this.mloadIcon = true;
                    this.mIconLoadState = 1;
                    this.mIconLoader.loadIcon(iconIdRetrieveIconId.recordNumber, obtainMessage(1));
                    return true;
                } catch (IndexOutOfBoundsException e) {
                    throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
                }
            }
            throw new ResultException(ResultCode.REQUIRED_VALUES_MISSING);
        }
        throw new ResultException(ResultCode.REQUIRED_VALUES_MISSING);
    }

    protected boolean processRefresh(CommandDetails commandDetails, List<ComprehensionTlv> list) {
        CatLog.d(this, "process Refresh");
        int i = commandDetails.commandQualifier;
        if (i != 0) {
            switch (i) {
            }
            return false;
        }
        this.mCmdParams = new DisplayTextParams(commandDetails, null);
        return false;
    }

    protected boolean processSelectItem(CommandDetails commandDetails, List<ComprehensionTlv> list) throws ResultException {
        IconId iconIdRetrieveIconId;
        CatLog.d(this, "process SelectItem");
        Menu menu = new Menu();
        Iterator<ComprehensionTlv> it = list.iterator();
        AppInterface.CommandType commandTypeFromInt = AppInterface.CommandType.fromInt(commandDetails.typeOfCommand);
        ComprehensionTlv comprehensionTlvSearchForTag = searchForTag(ComprehensionTlvTag.ALPHA_ID, list);
        if (comprehensionTlvSearchForTag != null) {
            menu.title = ValueParser.retrieveAlphaId(comprehensionTlvSearchForTag);
        } else if (commandTypeFromInt == AppInterface.CommandType.SET_UP_MENU) {
            throw new ResultException(ResultCode.REQUIRED_VALUES_MISSING);
        }
        while (true) {
            ComprehensionTlv comprehensionTlvSearchForNextTag = searchForNextTag(ComprehensionTlvTag.ITEM, it);
            if (comprehensionTlvSearchForNextTag == null) {
                break;
            }
            menu.items.add(ValueParser.retrieveItem(comprehensionTlvSearchForNextTag));
        }
        if (menu.items.size() == 0) {
            throw new ResultException(ResultCode.REQUIRED_VALUES_MISSING);
        }
        ComprehensionTlv comprehensionTlvSearchForTag2 = searchForTag(ComprehensionTlvTag.ITEM_ID, list);
        if (comprehensionTlvSearchForTag2 != null) {
            menu.defaultItem = ValueParser.retrieveItemId(comprehensionTlvSearchForTag2) - 1;
        }
        ComprehensionTlv comprehensionTlvSearchForTag3 = searchForTag(ComprehensionTlvTag.ICON_ID, list);
        ItemsIconId itemsIconIdRetrieveItemsIconId = null;
        if (comprehensionTlvSearchForTag3 != null) {
            this.mIconLoadState = 1;
            iconIdRetrieveIconId = ValueParser.retrieveIconId(comprehensionTlvSearchForTag3);
            menu.titleIconSelfExplanatory = iconIdRetrieveIconId.selfExplanatory;
        } else {
            iconIdRetrieveIconId = null;
        }
        ComprehensionTlv comprehensionTlvSearchForTag4 = searchForTag(ComprehensionTlvTag.ITEM_ICON_ID_LIST, list);
        if (comprehensionTlvSearchForTag4 != null) {
            this.mIconLoadState = 2;
            itemsIconIdRetrieveItemsIconId = ValueParser.retrieveItemsIconId(comprehensionTlvSearchForTag4);
            menu.itemsIconSelfExplanatory = itemsIconIdRetrieveItemsIconId.selfExplanatory;
        }
        if ((commandDetails.commandQualifier & 1) != 0) {
            if ((commandDetails.commandQualifier & 2) == 0) {
                menu.presentationType = PresentationType.DATA_VALUES;
            } else {
                menu.presentationType = PresentationType.NAVIGATION_OPTIONS;
            }
        }
        menu.softKeyPreferred = (commandDetails.commandQualifier & 4) != 0;
        menu.helpAvailable = (commandDetails.commandQualifier & 128) != 0;
        this.mCmdParams = new SelectItemParams(commandDetails, menu, iconIdRetrieveIconId != null);
        switch (this.mIconLoadState) {
            case 0:
                return false;
            case 1:
                this.mloadIcon = true;
                this.mIconLoader.loadIcon(iconIdRetrieveIconId.recordNumber, obtainMessage(1));
                return true;
            case 2:
                int[] iArr = itemsIconIdRetrieveItemsIconId.recordNumbers;
                if (iconIdRetrieveIconId != null) {
                    iArr = new int[itemsIconIdRetrieveItemsIconId.recordNumbers.length + 1];
                    iArr[0] = iconIdRetrieveIconId.recordNumber;
                    System.arraycopy(itemsIconIdRetrieveItemsIconId.recordNumbers, 0, iArr, 1, itemsIconIdRetrieveItemsIconId.recordNumbers.length);
                }
                this.mloadIcon = true;
                this.mIconLoader.loadIcons(iArr, obtainMessage(1));
                return true;
            default:
                return true;
        }
    }

    protected boolean processEventNotify(CommandDetails commandDetails, List<ComprehensionTlv> list) throws ResultException {
        IconId iconIdRetrieveIconId;
        CatLog.d(this, "process EventNotify");
        TextMessage textMessage = new TextMessage();
        textMessage.text = ValueParser.retrieveAlphaId(searchForTag(ComprehensionTlvTag.ALPHA_ID, list));
        ComprehensionTlv comprehensionTlvSearchForTag = searchForTag(ComprehensionTlvTag.ICON_ID, list);
        if (comprehensionTlvSearchForTag != null) {
            iconIdRetrieveIconId = ValueParser.retrieveIconId(comprehensionTlvSearchForTag);
            textMessage.iconSelfExplanatory = iconIdRetrieveIconId.selfExplanatory;
        } else {
            iconIdRetrieveIconId = null;
        }
        textMessage.responseNeeded = false;
        this.mCmdParams = new DisplayTextParams(commandDetails, textMessage);
        if (iconIdRetrieveIconId == null) {
            return false;
        }
        this.mloadIcon = true;
        this.mIconLoadState = 1;
        this.mIconLoader.loadIcon(iconIdRetrieveIconId.recordNumber, obtainMessage(1));
        return true;
    }

    protected boolean processSetUpEventList(CommandDetails commandDetails, List<ComprehensionTlv> list) {
        CatLog.d(this, "process SetUpEventList");
        ComprehensionTlv comprehensionTlvSearchForTag = searchForTag(ComprehensionTlvTag.EVENT_LIST, list);
        if (comprehensionTlvSearchForTag != null) {
            try {
                byte[] rawValue = comprehensionTlvSearchForTag.getRawValue();
                int valueIndex = comprehensionTlvSearchForTag.getValueIndex();
                int length = comprehensionTlvSearchForTag.getLength();
                int[] iArr = new int[length];
                int i = 0;
                while (length > 0) {
                    int i2 = rawValue[valueIndex] & 255;
                    valueIndex++;
                    length--;
                    switch (i2) {
                        case 4:
                        case 5:
                        case 7:
                        case 8:
                        case 15:
                            iArr[i] = i2;
                            i++;
                            break;
                    }
                }
                this.mCmdParams = new SetEventListParams(commandDetails, iArr);
            } catch (IndexOutOfBoundsException e) {
                CatLog.e(this, " IndexOutofBoundException in processSetUpEventList");
            }
        }
        return false;
    }

    protected boolean processLaunchBrowser(CommandDetails commandDetails, List<ComprehensionTlv> list) throws ResultException {
        String strGsm8BitUnpackedToString;
        LaunchBrowserMode launchBrowserMode;
        CatLog.d(this, "process LaunchBrowser");
        TextMessage textMessage = new TextMessage();
        ComprehensionTlv comprehensionTlvSearchForTag = searchForTag(ComprehensionTlvTag.URL, list);
        IconId iconIdRetrieveIconId = null;
        if (comprehensionTlvSearchForTag != null) {
            try {
                byte[] rawValue = comprehensionTlvSearchForTag.getRawValue();
                int valueIndex = comprehensionTlvSearchForTag.getValueIndex();
                int length = comprehensionTlvSearchForTag.getLength();
                if (length > 0) {
                    strGsm8BitUnpackedToString = GsmAlphabet.gsm8BitUnpackedToString(rawValue, valueIndex, length);
                } else {
                    strGsm8BitUnpackedToString = null;
                }
            } catch (IndexOutOfBoundsException e) {
                throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
            }
        } else {
            strGsm8BitUnpackedToString = null;
        }
        textMessage.text = ValueParser.retrieveAlphaId(searchForTag(ComprehensionTlvTag.ALPHA_ID, list));
        ComprehensionTlv comprehensionTlvSearchForTag2 = searchForTag(ComprehensionTlvTag.ICON_ID, list);
        if (comprehensionTlvSearchForTag2 != null) {
            iconIdRetrieveIconId = ValueParser.retrieveIconId(comprehensionTlvSearchForTag2);
            textMessage.iconSelfExplanatory = iconIdRetrieveIconId.selfExplanatory;
        }
        switch (commandDetails.commandQualifier) {
            case 2:
                launchBrowserMode = LaunchBrowserMode.USE_EXISTING_BROWSER;
                break;
            case 3:
                launchBrowserMode = LaunchBrowserMode.LAUNCH_NEW_BROWSER;
                break;
            default:
                launchBrowserMode = LaunchBrowserMode.LAUNCH_IF_NOT_ALREADY_LAUNCHED;
                break;
        }
        this.mCmdParams = new LaunchBrowserParams(commandDetails, textMessage, strGsm8BitUnpackedToString, launchBrowserMode);
        if (iconIdRetrieveIconId != null) {
            this.mIconLoadState = 1;
            this.mIconLoader.loadIcon(iconIdRetrieveIconId.recordNumber, obtainMessage(1));
            return true;
        }
        return false;
    }

    protected boolean processPlayTone(CommandDetails commandDetails, List<ComprehensionTlv> list) throws ResultException {
        Tone toneFromInt;
        Duration durationRetrieveDuration;
        CatLog.d(this, "process PlayTone");
        TextMessage textMessage = new TextMessage();
        ComprehensionTlv comprehensionTlvSearchForTag = searchForTag(ComprehensionTlvTag.TONE, list);
        IconId iconIdRetrieveIconId = null;
        if (comprehensionTlvSearchForTag != null && comprehensionTlvSearchForTag.getLength() > 0) {
            try {
                toneFromInt = Tone.fromInt(comprehensionTlvSearchForTag.getRawValue()[comprehensionTlvSearchForTag.getValueIndex()]);
            } catch (IndexOutOfBoundsException e) {
                throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
            }
        } else {
            toneFromInt = null;
        }
        ComprehensionTlv comprehensionTlvSearchForTag2 = searchForTag(ComprehensionTlvTag.ALPHA_ID, list);
        if (comprehensionTlvSearchForTag2 != null) {
            textMessage.text = ValueParser.retrieveAlphaId(comprehensionTlvSearchForTag2);
            if (textMessage.text == null) {
                textMessage.text = "";
            }
        }
        ComprehensionTlv comprehensionTlvSearchForTag3 = searchForTag(ComprehensionTlvTag.DURATION, list);
        if (comprehensionTlvSearchForTag3 == null) {
            durationRetrieveDuration = null;
        } else {
            durationRetrieveDuration = ValueParser.retrieveDuration(comprehensionTlvSearchForTag3);
        }
        ComprehensionTlv comprehensionTlvSearchForTag4 = searchForTag(ComprehensionTlvTag.ICON_ID, list);
        if (comprehensionTlvSearchForTag4 != null) {
            iconIdRetrieveIconId = ValueParser.retrieveIconId(comprehensionTlvSearchForTag4);
            textMessage.iconSelfExplanatory = iconIdRetrieveIconId.selfExplanatory;
        }
        IconId iconId = iconIdRetrieveIconId;
        boolean z = (commandDetails.commandQualifier & 1) != 0;
        textMessage.responseNeeded = false;
        this.mCmdParams = new PlayToneParams(commandDetails, textMessage, toneFromInt, durationRetrieveDuration, z);
        if (iconId == null) {
            return false;
        }
        this.mIconLoadState = 1;
        this.mIconLoader.loadIcon(iconId.recordNumber, obtainMessage(1));
        return true;
    }

    protected boolean processSetupCall(CommandDetails commandDetails, List<ComprehensionTlv> list) throws ResultException {
        IconId iconIdRetrieveIconId;
        int i;
        CatLog.d(this, "process SetupCall");
        Iterator<ComprehensionTlv> it = list.iterator();
        TextMessage textMessage = new TextMessage();
        TextMessage textMessage2 = new TextMessage();
        textMessage.text = ValueParser.retrieveAlphaId(searchForNextTag(ComprehensionTlvTag.ALPHA_ID, it));
        ComprehensionTlv comprehensionTlvSearchForTag = searchForTag(ComprehensionTlvTag.ICON_ID, list);
        IconId iconIdRetrieveIconId2 = null;
        if (comprehensionTlvSearchForTag != null) {
            iconIdRetrieveIconId = ValueParser.retrieveIconId(comprehensionTlvSearchForTag);
            textMessage.iconSelfExplanatory = iconIdRetrieveIconId.selfExplanatory;
        } else {
            iconIdRetrieveIconId = null;
        }
        ComprehensionTlv comprehensionTlvSearchForNextTag = searchForNextTag(ComprehensionTlvTag.ALPHA_ID, it);
        if (comprehensionTlvSearchForNextTag != null) {
            textMessage2.text = ValueParser.retrieveAlphaId(comprehensionTlvSearchForNextTag);
        }
        ComprehensionTlv comprehensionTlvSearchForTag2 = searchForTag(ComprehensionTlvTag.ICON_ID, list);
        if (comprehensionTlvSearchForTag2 != null) {
            iconIdRetrieveIconId2 = ValueParser.retrieveIconId(comprehensionTlvSearchForTag2);
            textMessage2.iconSelfExplanatory = iconIdRetrieveIconId2.selfExplanatory;
        }
        this.mCmdParams = new CallSetupParams(commandDetails, textMessage, textMessage2);
        if (iconIdRetrieveIconId == null && iconIdRetrieveIconId2 == null) {
            return false;
        }
        this.mIconLoadState = 2;
        int[] iArr = new int[2];
        if (iconIdRetrieveIconId == null) {
            i = -1;
        } else {
            i = iconIdRetrieveIconId.recordNumber;
        }
        iArr[0] = i;
        iArr[1] = iconIdRetrieveIconId2 != null ? iconIdRetrieveIconId2.recordNumber : -1;
        this.mIconLoader.loadIcons(iArr, obtainMessage(1));
        return true;
    }

    protected boolean processProvideLocalInfo(CommandDetails commandDetails, List<ComprehensionTlv> list) throws ResultException {
        CatLog.d(this, "process ProvideLocalInfo");
        switch (commandDetails.commandQualifier) {
            case 3:
                CatLog.d(this, "PLI [DTTZ_SETTING]");
                this.mCmdParams = new CommandParams(commandDetails);
                return false;
            case 4:
                CatLog.d(this, "PLI [LANGUAGE_SETTING]");
                this.mCmdParams = new CommandParams(commandDetails);
                return false;
            default:
                CatLog.d(this, "PLI[" + commandDetails.commandQualifier + "] Command Not Supported");
                this.mCmdParams = new CommandParams(commandDetails);
                throw new ResultException(ResultCode.BEYOND_TERMINAL_CAPABILITY);
        }
    }

    protected boolean processLanguageNotification(CommandDetails commandDetails, List<ComprehensionTlv> list) throws ResultException {
        String str;
        CatLog.d(this, "process Language Notification");
        String language = Locale.getDefault().getLanguage();
        String strGsm8BitUnpackedToString = null;
        switch (commandDetails.commandQualifier) {
            case 0:
                if (!TextUtils.isEmpty(this.mSavedLanguage) && !TextUtils.isEmpty(this.mRequestedLanguage) && this.mRequestedLanguage.equals(language)) {
                    CatLog.d(this, "Non-specific language notification changes the language setting back to " + this.mSavedLanguage);
                    str = this.mSavedLanguage;
                } else {
                    str = null;
                }
                this.mSavedLanguage = null;
                this.mRequestedLanguage = null;
                strGsm8BitUnpackedToString = str;
                break;
            case 1:
                ComprehensionTlv comprehensionTlvSearchForTag = searchForTag(ComprehensionTlvTag.LANGUAGE, list);
                if (comprehensionTlvSearchForTag != null) {
                    if (comprehensionTlvSearchForTag.getLength() != 2) {
                        throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
                    }
                    strGsm8BitUnpackedToString = GsmAlphabet.gsm8BitUnpackedToString(comprehensionTlvSearchForTag.getRawValue(), comprehensionTlvSearchForTag.getValueIndex(), 2);
                    if (TextUtils.isEmpty(this.mSavedLanguage) || (!TextUtils.isEmpty(this.mRequestedLanguage) && !this.mRequestedLanguage.equals(language))) {
                        this.mSavedLanguage = language;
                    }
                    this.mRequestedLanguage = strGsm8BitUnpackedToString;
                    CatLog.d(this, "Specific language notification changes the language setting to " + this.mRequestedLanguage);
                }
                break;
            default:
                CatLog.d(this, "LN[" + commandDetails.commandQualifier + "] Command Not Supported");
                break;
        }
        this.mCmdParams = new LanguageParams(commandDetails, strGsm8BitUnpackedToString);
        return false;
    }

    protected boolean processBIPClient(CommandDetails commandDetails, List<ComprehensionTlv> list) throws ResultException {
        boolean z;
        AppInterface.CommandType commandTypeFromInt = AppInterface.CommandType.fromInt(commandDetails.typeOfCommand);
        if (commandTypeFromInt != null) {
            CatLog.d(this, "process " + commandTypeFromInt.name());
        }
        TextMessage textMessage = new TextMessage();
        IconId iconIdRetrieveIconId = null;
        ComprehensionTlv comprehensionTlvSearchForTag = searchForTag(ComprehensionTlvTag.ALPHA_ID, list);
        if (comprehensionTlvSearchForTag == null) {
            z = false;
        } else {
            textMessage.text = ValueParser.retrieveAlphaId(comprehensionTlvSearchForTag);
            CatLog.d(this, "alpha TLV text=" + textMessage.text);
            z = true;
        }
        ComprehensionTlv comprehensionTlvSearchForTag2 = searchForTag(ComprehensionTlvTag.ICON_ID, list);
        if (comprehensionTlvSearchForTag2 != null) {
            iconIdRetrieveIconId = ValueParser.retrieveIconId(comprehensionTlvSearchForTag2);
            textMessage.iconSelfExplanatory = iconIdRetrieveIconId.selfExplanatory;
        }
        textMessage.responseNeeded = false;
        this.mCmdParams = new BIPClientParams(commandDetails, textMessage, z);
        if (iconIdRetrieveIconId == null) {
            return false;
        }
        this.mIconLoadState = 1;
        this.mIconLoader.loadIcon(iconIdRetrieveIconId.recordNumber, obtainMessage(1));
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
