package com.mediatek.vcalendar.component;

import com.mediatek.vcalendar.utils.LogUtil;

public class VTodo extends Component {
    private static final String TAG = "VTimezone";
    public static final String VTODO_BEGIN = "BEGIN:VTODO";
    public static final String VTODO_END = "END:VTODO";

    public VTodo() {
        super("VTODO", null);
        LogUtil.d(TAG, "Constructor: VTODO component created.");
    }
}
