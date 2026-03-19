package com.mediatek.vcalendar;

import android.content.ContentUris;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import com.mediatek.vcalendar.component.Component;
import com.mediatek.vcalendar.component.VCalendar;
import com.mediatek.vcalendar.database.DatabaseHelper;
import com.mediatek.vcalendar.utils.LogUtil;

public class VCalParser {
    private final Context mContext;
    private DatabaseHelper mDbOperationHelper;
    private final VCalStatusChangeOperator mListener;
    private String mVcsString;
    private boolean mCancelRequest = false;
    private int mTotalCnt = -1;
    private long mCalendarId = -1;
    private String mCurrentAccountName = "PC Sync";

    public VCalParser(String str, Context context, VCalStatusChangeOperator vCalStatusChangeOperator) {
        this.mListener = vCalStatusChangeOperator;
        this.mContext = context;
        this.mVcsString = str;
    }

    public void startParseVcsContent() {
        long dtStart;
        LogUtil.d("VCalParser", "startParseVcsContent()");
        if (!this.mVcsString.contains("BEGIN:VEVENT") || !this.mVcsString.contains("END:VEVENT")) {
            LogUtil.e("VCalParser", "startParseVcsContent(): the given Content do not contains a VEvent.");
            LogUtil.d("VCalParser", "startParseVcsContent(): The failed string : \n" + this.mVcsString);
            return;
        }
        this.mVcsString = this.mVcsString.replaceAll("\r\n", "\n");
        this.mVcsString = this.mVcsString.replaceAll("\r", "\n");
        parseVCalPropertiesV1(this.mVcsString);
        int i = 0;
        int length = this.mVcsString.length();
        while (i < length) {
            int iIndexOf = this.mVcsString.indexOf("BEGIN:VEVENT", i);
            int iIndexOf2 = this.mVcsString.indexOf("END:VEVENT", i) + "END:VEVENT".length();
            int i2 = iIndexOf2 + 1;
            if (iIndexOf == -1 || iIndexOf2 == -1) {
                LogUtil.w("VCalParser", "end parse or error, start=" + iIndexOf + "; end=" + iIndexOf2);
                break;
            }
            Component component = ComponentParser.parse(this.mVcsString.substring(iIndexOf, iIndexOf2));
            if (component == null) {
                LogUtil.e("VCalParser", "startParseVcsContent(): parse one component failed");
                return;
            }
            component.setContext(this.mContext);
            if (!initDatabaseHelper()) {
                LogUtil.e("VCalParser", "startParseVcsContent(): init DatabaseHelper failed");
                return;
            }
            SingleComponentContentValues singleComponentContentValues = new SingleComponentContentValues();
            singleComponentContentValues.contentValues.put("calendar_id", String.valueOf(this.mCalendarId));
            long dtEnd = -1;
            try {
                component.writeInfoToContentValues(singleComponentContentValues);
                dtStart = component.getDtStart();
            } catch (VCalendarException e) {
                e = e;
                dtStart = -1;
            }
            try {
                dtEnd = component.getDtEnd();
            } catch (VCalendarException e2) {
                e = e2;
                LogUtil.e("VCalParser", "startParseVcsContent(): write component info to contentvalues failed", e);
            }
            Uri uriInsert = this.mDbOperationHelper.insert(singleComponentContentValues);
            Bundle bundle = new Bundle();
            if (uriInsert != null) {
                bundle.putLong("key_event_id", ContentUris.parseId(uriInsert));
            }
            bundle.putLong("key_start_millis", dtStart);
            bundle.putLong("key_end_millis", dtEnd);
            this.mListener.vCalOperationFinished(1, 1, bundle);
            i = i2;
        }
        VCalendar.TIMEZONE_LIST.clear();
    }

    private void parseVCalPropertiesV1(String str) {
        if (str.contains("VERSION")) {
            int iIndexOf = str.indexOf("VERSION");
            String strSubstring = str.substring(iIndexOf, str.indexOf("\n", iIndexOf));
            VCalendar.setVCalendarVersion(strSubstring);
            if (strSubstring.contains("1.0") && str.contains("TZ:")) {
                int iIndexOf2 = str.indexOf("TZ");
                VCalendar.setV10TimeZone(str.substring(iIndexOf2, str.indexOf("\n", iIndexOf2)).replace("TZ:", ""));
                LogUtil.i("VCalParser", "parseVCalProperties_v1: sTz=" + VCalendar.getV10TimeZone());
            }
        }
    }

    private boolean initDatabaseHelper() {
        this.mDbOperationHelper = new DatabaseHelper(this.mContext);
        this.mCalendarId = this.mDbOperationHelper.getCalendarIdForAccount(this.mCurrentAccountName);
        if (this.mCalendarId != -1) {
            return true;
        }
        this.mListener.vCalOperationExceptionOccured(0, 0, 1);
        return false;
    }
}
