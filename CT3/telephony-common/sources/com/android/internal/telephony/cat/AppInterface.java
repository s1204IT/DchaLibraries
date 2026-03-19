package com.android.internal.telephony.cat;

import com.google.android.mms.pdu.CharacterSets;

public interface AppInterface {
    public static final String ALPHA_STRING = "alpha_string";
    public static final String CARD_STATUS = "card_status";
    public static final String CAT_ALPHA_NOTIFY_ACTION = "android.intent.action.stk.alpha_notify";
    public static final String CAT_CMD_ACTION = "android.intent.action.stk.command";
    public static final String CAT_ICC_STATUS_CHANGE = "android.intent.action.stk.icc_status_change";
    public static final String CAT_SESSION_END_ACTION = "android.intent.action.stk.session_end";
    public static final String CLEAR_DISPLAY_TEXT_CMD = "android.intent.action.stk.clear_display_text";
    public static final String REFRESH_RESULT = "refresh_result";
    public static final String STK_PERMISSION = "android.permission.RECEIVE_STK_COMMANDS";

    boolean isCallDisConnReceived();

    void onCmdResponse(CatResponseMessage catResponseMessage);

    void onDBHandler(int i);

    void onEventDownload(CatResponseMessage catResponseMessage);

    void onLaunchCachedSetupMenu();

    void setAllCallDisConn(boolean z);

    public enum CommandType {
        DISPLAY_TEXT(33),
        GET_INKEY(34),
        GET_INPUT(35),
        LAUNCH_BROWSER(21),
        PLAY_TONE(32),
        REFRESH(1),
        SELECT_ITEM(36),
        SEND_SS(17),
        SEND_USSD(18),
        SEND_SMS(19),
        SEND_DTMF(20),
        SET_UP_EVENT_LIST(5),
        SET_UP_IDLE_MODE_TEXT(40),
        SET_UP_MENU(37),
        SET_UP_CALL(16),
        PROVIDE_LOCAL_INFORMATION(38),
        MORE_TIME(2),
        POLL_INTERVAL(3),
        POLLING_OFF(4),
        TIMER_MANAGEMENT(39),
        PERFORM_CARD_APDU(48),
        POWER_ON_CARD(49),
        POWER_OFF_CARD(50),
        GET_READER_STATUS(51),
        RUN_AT_COMMAND(52),
        LANGUAGE_NOTIFICATION(53),
        OPEN_CHANNEL(64),
        CLOSE_CHANNEL(65),
        RECEIVE_DATA(66),
        SEND_DATA(67),
        GET_CHANNEL_STATUS(68),
        SERVICE_SEARCH(69),
        GET_SERVICE_INFORMATION(70),
        DECLARE_SERVICE(71),
        SET_FRAME(80),
        GET_FRAME_STATUS(81),
        RETRIEVE_MULTIMEDIA_MESSAGE(96),
        SUBMIT_MULTIMEDIA_MESSAGE(97),
        DISPLAY_MULTIMEDIA_MESSAGE(98),
        ACTIVATE(CharacterSets.ISO_8859_16),
        CALLCTRL_RSP_MSG(255);

        private int mValue;

        public static CommandType[] valuesCustom() {
            return values();
        }

        CommandType(int value) {
            this.mValue = value;
        }

        public int value() {
            return this.mValue;
        }

        public static CommandType fromInt(int value) {
            for (CommandType e : valuesCustom()) {
                if (e.mValue == value) {
                    return e;
                }
            }
            return null;
        }
    }
}
