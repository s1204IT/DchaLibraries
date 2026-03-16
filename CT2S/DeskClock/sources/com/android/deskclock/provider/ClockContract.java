package com.android.deskclock.provider;

import android.net.Uri;
import android.provider.BaseColumns;

public final class ClockContract {

    private interface AlarmSettingColumns extends BaseColumns {
        public static final Uri NO_RINGTONE_URI = Uri.EMPTY;
        public static final String NO_RINGTONE = NO_RINGTONE_URI.toString();
    }

    protected interface AlarmsColumns extends BaseColumns, AlarmSettingColumns {
        public static final Uri CONTENT_URI = Uri.parse("content://com.android.deskclock/alarms");
    }

    protected interface InstancesColumns extends BaseColumns, AlarmSettingColumns {
        public static final Uri CONTENT_URI = Uri.parse("content://com.android.deskclock/instances");
    }
}
