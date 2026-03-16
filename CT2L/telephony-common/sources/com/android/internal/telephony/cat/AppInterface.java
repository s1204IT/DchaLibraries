package com.android.internal.telephony.cat;

import android.os.SystemProperties;

public interface AppInterface {
    public static final String ALPHA_STRING = "alpha_string";
    public static final String CARD_STATUS = "card_status";
    public static final String CAT_ALPHA_NOTIFY_ACTION = "android.intent.action.stk.alpha_notify";
    public static final String CAT_CALL_CONNECTED_EVENT = "android.intent.action.stk.call_connected_event";
    public static final String CAT_CMD_ACTION = "android.intent.action.stk.command";
    public static final String CAT_EVENT_LIST_ACTION = "android.intent.action.stk.event_list_action";
    public static final String CAT_EVENT_LIST_CALL_CONNECTED = "android.intent.action.stk.event_list_call_connected";
    public static final String CAT_EVENT_LIST_LANGUAGE_SELECT = "android.intent.action.stk.even_list_languages_select";
    public static final String CAT_EVENT_REMOVE_EVENT = "android.intent.action.stk.event_remove_event";
    public static final String CAT_EVENT_SEND_SM_STATUS = "android.intent.action.stk.event_send_sm_status";
    public static final String CAT_ICC_STATUS_CHANGE = "android.intent.action.stk.icc_status_change";
    public static final String CAT_LANGUAGE_SETTING = "android.intent.action.stk.languages_setting";
    public static final String CAT_SCREEN_BUSY = "android.intent.action.stk.screen_busy";
    public static final String CAT_SESSION_END_ACTION = "android.intent.action.stk.session_end";
    public static final String REFRESH_RESULT = "refresh_result";
    public static final boolean STK_ICON_DISABLED;
    public static final String STK_PERMISSION = "android.permission.RECEIVE_STK_COMMANDS";

    void onCmdResponse(CatResponseMessage catResponseMessage);

    void onEventResponse(CatResponseMessage catResponseMessage);

    static {
        STK_ICON_DISABLED = SystemProperties.get("ro.stk.icon.disable", "0").equals("1");
    }

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
        LANGUAGE_NOTIFICATION(53),
        OPEN_CHANNEL(64),
        CLOSE_CHANNEL(65),
        RECEIVE_DATA(66),
        SEND_DATA(67),
        GET_CHANNEL_STATUS(68);

        private int mValue;

        CommandType(int value) {
            this.mValue = value;
        }

        public int value() {
            return this.mValue;
        }

        public static CommandType fromInt(int value) {
            CommandType[] arr$ = values();
            for (CommandType e : arr$) {
                if (e.mValue == value) {
                    return e;
                }
            }
            return null;
        }
    }
}
