package com.android.internal.telephony.cat;

public enum EventList {
    MT_CALL(0),
    CALL_CONNECTED(1),
    CALL_DISCONNECTED(2),
    LOCATION_SATATUS(3),
    USER_ACTIVITY(4),
    IDLE_SCREEN_AVAILABLE(5),
    CARD_READER_STATUS(6),
    LANGUAGE_SELECTION(7),
    BROWER_TERMINATION(8),
    DATA_AVAILABLE(9),
    CHANNEL_STATUS(10),
    SINGLE_ACCESS_TECHNOLOGY_CHANGE(11),
    DISPLAY_PARAMETERS_CHANGED(12),
    LOCAL_CONNECTION(13),
    NETWORK_SEARCH_MODE_CHANGE(14),
    BROWSING_STATUS(15),
    FRAMES_INFORMATION_CHANGE(16),
    HCI_CONNECTIVITY_EVENT(19),
    MULTIPLE_ACCESS_TECHNOLOGY_CHANGE(20),
    REMOVE_EVENT(21);

    private int mValue;

    EventList(int value) {
        this.mValue = value;
    }

    public int value() {
        return this.mValue;
    }

    public static EventList fromInt(int value) {
        EventList[] arr$ = values();
        for (EventList r : arr$) {
            if (r.mValue == value) {
                return r;
            }
        }
        return null;
    }
}
