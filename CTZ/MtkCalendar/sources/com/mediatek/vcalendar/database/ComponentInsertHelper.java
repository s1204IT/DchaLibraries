package com.mediatek.vcalendar.database;

import android.content.Context;
import android.net.Uri;
import com.mediatek.vcalendar.SingleComponentContentValues;
import com.mediatek.vcalendar.utils.LogUtil;

public abstract class ComponentInsertHelper {
    abstract Uri insertContentValues(SingleComponentContentValues singleComponentContentValues);

    protected ComponentInsertHelper() {
    }

    static ComponentInsertHelper buildInsertHelper(Context context, SingleComponentContentValues singleComponentContentValues) {
        LogUtil.i("ComponentInsertHelper", "buildInsertHelper(): component type: " + singleComponentContentValues.componentType);
        if (singleComponentContentValues.componentType.equals("VEVENT")) {
            return new VEventInsertHelper(context);
        }
        return null;
    }
}
