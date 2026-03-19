package com.mediatek.vcalendar.component;

import com.mediatek.vcalendar.utils.LogUtil;

public class VTimezone extends Component {
    private static final String TAG = "VTimezone";
    public static final String VTIMEZONE_BEGIN = "BEGIN:VTIMEZONE";
    public static final String VTIMEZONE_END = "END:VTIMEZONE";

    public VTimezone() {
        super("VTIMEZONE", null);
        LogUtil.d(TAG, "Constructor: VTIMEZONE component created!");
    }
}
