package com.mediatek.vcalendar;

import android.content.ContentUris;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import com.android.common.speech.LoggingEvents;
import com.mediatek.vcalendar.component.Component;
import com.mediatek.vcalendar.component.VCalendar;
import com.mediatek.vcalendar.component.VEvent;
import com.mediatek.vcalendar.database.DatabaseHelper;
import com.mediatek.vcalendar.property.Property;
import com.mediatek.vcalendar.property.Version;
import com.mediatek.vcalendar.utils.LogUtil;
import com.mediatek.vcalendar.utils.StringUtil;
import com.mediatek.vcalendar.utils.VCalFileReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

public class VCalParser {
    protected static final String BUNDLE_KEY_END_MILLIS = "key_end_millis";
    protected static final String BUNDLE_KEY_EVENT_ID = "key_event_id";
    protected static final String BUNDLE_KEY_START_MILLIS = "key_start_millis";
    private static final String TAG = "VCalParser";
    private long mCalendarId;
    private boolean mCancelRequest;
    private final Context mContext;
    private String mCurrentAccountName;
    private int mCurrentCnt;
    private Uri mCurrentUri;
    private DatabaseHelper mDbOperationHelper;
    private VCalFileReader mFileReader;
    private final VCalStatusChangeOperator mListener;
    private int mTotalCnt;
    private String mVcsString;

    public VCalParser(Uri uri, Context context, VCalStatusChangeOperator vCalStatusChangeOperator) {
        this(uri, DatabaseHelper.ACCOUNT_PC_SYNC, context, vCalStatusChangeOperator);
    }

    public VCalParser(Uri uri, String str, Context context, VCalStatusChangeOperator vCalStatusChangeOperator) {
        this.mCancelRequest = false;
        this.mTotalCnt = -1;
        this.mCalendarId = -1L;
        this.mCurrentUri = uri;
        this.mCurrentAccountName = str;
        this.mContext = context;
        this.mListener = vCalStatusChangeOperator;
    }

    public VCalParser(String str, Context context, VCalStatusChangeOperator vCalStatusChangeOperator) {
        this.mCancelRequest = false;
        this.mTotalCnt = -1;
        this.mCalendarId = -1L;
        this.mListener = vCalStatusChangeOperator;
        this.mContext = context;
        this.mCurrentAccountName = DatabaseHelper.ACCOUNT_PC_SYNC;
        this.mVcsString = str;
    }

    public void startParse() {
        LogUtil.d(TAG, "startParse(): started.");
        this.mCancelRequest = false;
        if (!initFileReaderAndDbHelper()) {
            LogUtil.e(TAG, "startParse(): initFileReaderAndDbHelper failed.");
            return;
        }
        this.mTotalCnt = this.mFileReader.getComponentsCount();
        this.mListener.vCalOperationStarted(this.mTotalCnt);
        LogUtil.d(TAG, "startParse(): components total count:" + this.mTotalCnt);
        if (this.mTotalCnt == -1) {
            this.mListener.vCalOperationExceptionOccured(0, -1, 2);
        }
        this.mCurrentCnt = 0;
        long j = -1;
        try {
            ArrayList arrayList = new ArrayList(this.mTotalCnt);
            while (!this.mCancelRequest && this.mFileReader.hasNextComponent()) {
                Component component = ComponentParser.parse(this.mFileReader.readNextComponent());
                if (component != null) {
                    component.setContext(this.mContext);
                    SingleComponentContentValues singleComponentContentValues = new SingleComponentContentValues();
                    singleComponentContentValues.contentValues.put("calendar_id", String.valueOf(this.mCalendarId));
                    try {
                        component.writeInfoToContentValues(singleComponentContentValues);
                        long dtStart = component.getDtStart();
                        try {
                            this.mListener.vCalProcessStatusUpdate(this.mCurrentCnt, this.mTotalCnt);
                            arrayList.add(this.mCurrentCnt, singleComponentContentValues);
                            this.mCurrentCnt++;
                            j = dtStart;
                        } catch (IOException e) {
                            e = e;
                            j = dtStart;
                            LogUtil.e(TAG, "startParse(): parse components failed", e);
                            Bundle bundle = new Bundle();
                            bundle.putLong(BUNDLE_KEY_START_MILLIS, j);
                            this.mListener.vCalOperationFinished(this.mCurrentCnt, this.mTotalCnt, bundle);
                            VCalendar.TIMEZONE_LIST.clear();
                            this.mFileReader.close();
                        }
                    } catch (VCalendarException e2) {
                        this.mListener.vCalOperationExceptionOccured(this.mCurrentCnt, this.mTotalCnt, 0);
                        LogUtil.e(TAG, "startParse(): write component info to contentvalues failed", e2);
                    }
                }
            }
            this.mDbOperationHelper.insert(arrayList);
        } catch (IOException e3) {
            e = e3;
        }
        Bundle bundle2 = new Bundle();
        bundle2.putLong(BUNDLE_KEY_START_MILLIS, j);
        this.mListener.vCalOperationFinished(this.mCurrentCnt, this.mTotalCnt, bundle2);
        VCalendar.TIMEZONE_LIST.clear();
        try {
            this.mFileReader.close();
        } catch (IOException e4) {
            LogUtil.e(TAG, "startParse(): failed to close VCalFileReader", e4);
        }
    }

    public void startParsePreview() {
        LogUtil.d(TAG, "startParsePreview(): started");
        if (!initVCalFileReader()) {
            return;
        }
        String firstComponent = this.mFileReader.getFirstComponent();
        if (StringUtil.isNullOrEmpty(firstComponent)) {
            this.mListener.vCalOperationExceptionOccured(0, -1, 0);
            LogUtil.e(TAG, "startParsePreview(): it is not a vcs file.");
            return;
        }
        Component component = ComponentParser.parse(firstComponent);
        if (component == null) {
            this.mListener.vCalOperationExceptionOccured(0, -1, 0);
            LogUtil.e(TAG, "startParsePreview(): parse one component failed");
            return;
        }
        component.setContext(this.mContext);
        ComponentPreviewInfo componentPreviewInfo = new ComponentPreviewInfo();
        this.mTotalCnt = this.mFileReader.getComponentsCount();
        componentPreviewInfo.componentCount = this.mTotalCnt;
        if (componentPreviewInfo.componentCount <= 0) {
            this.mListener.vCalOperationExceptionOccured(0, -1, 2);
            LogUtil.e(TAG, "startParsePreview(): No components exsit in the file.");
        } else {
            try {
                component.fillPreviewInfo(componentPreviewInfo);
            } catch (VCalendarException e) {
                LogUtil.e(TAG, "startParsePreview(): fill preview info of one component failed.", e);
            }
            this.mListener.vCalOperationFinished(this.mTotalCnt, this.mTotalCnt, componentPreviewInfo);
        }
    }

    public void startParseVcsContent() {
        long dtStart;
        LogUtil.d(TAG, "startParseVcsContent()");
        if (!this.mVcsString.contains(VEvent.VEVENT_BEGIN) || !this.mVcsString.contains(VEvent.VEVENT_END)) {
            LogUtil.e(TAG, "startParseVcsContent(): the given Content do not contains a VEvent.");
            LogUtil.d(TAG, "startParseVcsContent(): The failed string : \n" + this.mVcsString);
            return;
        }
        this.mVcsString = this.mVcsString.replaceAll(Component.NEWLINE, "\n");
        this.mVcsString = this.mVcsString.replaceAll("\r", "\n");
        parseVCalPropertiesV1(this.mVcsString);
        int i = 0;
        int length = this.mVcsString.length();
        while (i < length) {
            int iIndexOf = this.mVcsString.indexOf(VEvent.VEVENT_BEGIN, i);
            int iIndexOf2 = this.mVcsString.indexOf(VEvent.VEVENT_END, i) + VEvent.VEVENT_END.length();
            int i2 = iIndexOf2 + 1;
            if (iIndexOf == -1 || iIndexOf2 == -1) {
                LogUtil.w(TAG, "end parse or error, start=" + iIndexOf + "; end=" + iIndexOf2);
                break;
            }
            Component component = ComponentParser.parse(this.mVcsString.substring(iIndexOf, iIndexOf2));
            if (component == null) {
                LogUtil.e(TAG, "startParseVcsContent(): parse one component failed");
                return;
            }
            component.setContext(this.mContext);
            if (!initDatabaseHelper()) {
                LogUtil.e(TAG, "startParseVcsContent(): init DatabaseHelper failed");
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
                LogUtil.e(TAG, "startParseVcsContent(): write component info to contentvalues failed", e);
            }
            Uri uriInsert = this.mDbOperationHelper.insert(singleComponentContentValues);
            Bundle bundle = new Bundle();
            if (uriInsert != null) {
                bundle.putLong(BUNDLE_KEY_EVENT_ID, ContentUris.parseId(uriInsert));
            }
            bundle.putLong(BUNDLE_KEY_START_MILLIS, dtStart);
            bundle.putLong(BUNDLE_KEY_END_MILLIS, dtEnd);
            this.mListener.vCalOperationFinished(1, 1, bundle);
            i = i2;
        }
        VCalendar.TIMEZONE_LIST.clear();
    }

    private void parseVCalPropertiesV1(String str) {
        if (str.contains(Property.VERSION)) {
            int iIndexOf = str.indexOf(Property.VERSION);
            String strSubstring = str.substring(iIndexOf, str.indexOf("\n", iIndexOf));
            VCalendar.setVCalendarVersion(strSubstring);
            if (strSubstring.contains(Version.VERSION10) && str.contains("TZ:")) {
                int iIndexOf2 = str.indexOf(Property.TZ);
                VCalendar.setV10TimeZone(str.substring(iIndexOf2, str.indexOf("\n", iIndexOf2)).replace("TZ:", LoggingEvents.EXTRA_CALLING_APP_NAME));
                LogUtil.i(TAG, "parseVCalProperties_v1: sTz=" + VCalendar.getV10TimeZone());
            }
        }
    }

    public void cancelCurrentParse() {
        LogUtil.i(TAG, "cancelCurrentParse()");
        this.mCancelRequest = true;
        this.mListener.vCalOperationCanceled(this.mCurrentCnt, this.mTotalCnt);
    }

    public void close() {
        LogUtil.d(TAG, "close()");
        if (this.mFileReader != null) {
            try {
                this.mFileReader.close();
            } catch (IOException e) {
                LogUtil.e(TAG, "close(): failed to close VCalFileReader.", e);
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

    private boolean initVCalFileReader() {
        boolean z;
        try {
            this.mFileReader = new VCalFileReader(this.mCurrentUri, this.mContext);
            z = true;
        } catch (FileNotFoundException e) {
            LogUtil.d(TAG, "initVCalFileReader(): the given Uri cannot be parsed, Uri=" + this.mCurrentUri);
            e.printStackTrace();
            z = false;
        } catch (IOException e2) {
            LogUtil.e(TAG, "initVCalFileReader(): IOException Occured when I/O operation. ");
            e2.printStackTrace();
            z = false;
        } catch (SecurityException e3) {
            LogUtil.d(TAG, "initVCalFileReader(): the given Uri cannot be parsed, Uri=" + this.mCurrentUri + " Exception occurs: " + e3.toString());
            e3.printStackTrace();
            z = false;
        }
        if (!z) {
            LogUtil.w(TAG, "initVCalFileReader(): FILE_READ_EXCEPTION Occured.");
            this.mListener.vCalOperationExceptionOccured(0, -1, 3);
        }
        return z;
    }

    private boolean initFileReaderAndDbHelper() {
        return this.mCurrentUri != null && this.mCurrentAccountName != null && initDatabaseHelper() && initVCalFileReader();
    }
}
