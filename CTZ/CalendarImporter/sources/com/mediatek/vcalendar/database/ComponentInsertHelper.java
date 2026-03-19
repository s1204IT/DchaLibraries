package com.mediatek.vcalendar.database;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import com.mediatek.vcalendar.SingleComponentContentValues;
import com.mediatek.vcalendar.utils.LogUtil;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public abstract class ComponentInsertHelper {
    private static final String TAG = "ComponentInsertHelper";

    abstract Uri insertContentValues(SingleComponentContentValues singleComponentContentValues);

    abstract Uri insertMultiComponentContentValues(List<SingleComponentContentValues> list);

    protected ComponentInsertHelper() {
    }

    static ComponentInsertHelper buildInsertHelper(Context context, SingleComponentContentValues singleComponentContentValues) {
        LogUtil.i(TAG, "buildInsertHelper(): component type: " + singleComponentContentValues.componentType);
        if (singleComponentContentValues.componentType.equals("VEVENT")) {
            return new VEventInsertHelper(context);
        }
        return null;
    }

    protected void buildMemberCVList(LinkedList<ContentValues> linkedList, String str, String str2) {
        Iterator<ContentValues> it = linkedList.iterator();
        while (it.hasNext()) {
            it.next().put(str2, str);
        }
    }
}
