package com.mediatek.vcalendar.database;

import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.net.Uri;
import android.os.RemoteException;
import android.provider.CalendarContract;
import com.mediatek.vcalendar.SingleComponentContentValues;
import com.mediatek.vcalendar.utils.LogUtil;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class VEventInsertHelper extends ComponentInsertHelper {
    private static final int MAX_COMPONENT_COUNT = 500;
    private static final String TAG = "VEventInsertHelper";
    private Context mContext;

    protected VEventInsertHelper(Context context) {
        this.mContext = context;
    }

    @Override
    Uri insertContentValues(SingleComponentContentValues singleComponentContentValues) {
        ContentProviderResult[] contentProviderResultArrApplyBatch;
        LogUtil.i(TAG, "insertContentValues()");
        ContentResolver contentResolver = this.mContext.getContentResolver();
        int size = singleComponentContentValues.alarmValuesList.size();
        int size2 = singleComponentContentValues.attendeeValuesList.size();
        LogUtil.d(TAG, "insertContentValues(): Alarms count: " + size);
        LogUtil.d(TAG, "insertContentValues(): Attendees count:" + size2);
        ArrayList<ContentProviderOperation> arrayList = new ArrayList<>();
        int size3 = arrayList.size();
        arrayList.add(ContentProviderOperation.newInsert(CalendarContract.Events.CONTENT_URI).withValues(singleComponentContentValues.contentValues).build());
        Iterator<ContentValues> it = singleComponentContentValues.alarmValuesList.iterator();
        while (it.hasNext()) {
            ContentProviderOperation.Builder builderWithValues = ContentProviderOperation.newInsert(CalendarContract.Reminders.CONTENT_URI).withValues(it.next());
            builderWithValues.withValueBackReference("event_id", size3);
            arrayList.add(builderWithValues.build());
        }
        Iterator<ContentValues> it2 = singleComponentContentValues.attendeeValuesList.iterator();
        while (it2.hasNext()) {
            ContentProviderOperation.Builder builderWithValues2 = ContentProviderOperation.newInsert(CalendarContract.Attendees.CONTENT_URI).withValues(it2.next());
            builderWithValues2.withValueBackReference("event_id", size3);
            arrayList.add(builderWithValues2.build());
        }
        try {
            contentProviderResultArrApplyBatch = contentResolver.applyBatch("com.android.calendar", arrayList);
        } catch (OperationApplicationException e) {
            e.printStackTrace();
            contentProviderResultArrApplyBatch = null;
        } catch (RemoteException e2) {
            e2.printStackTrace();
            contentProviderResultArrApplyBatch = null;
        }
        if (contentProviderResultArrApplyBatch != null && contentProviderResultArrApplyBatch[0] != null) {
            Uri uri = contentProviderResultArrApplyBatch[0].uri;
            LogUtil.v(TAG, "addNextContentValue: insert event=" + uri);
            return uri;
        }
        LogUtil.e(TAG, "addNextContentValue: insert event failed.");
        return null;
    }

    @Override
    Uri insertMultiComponentContentValues(List<SingleComponentContentValues> list) {
        LogUtil.i(TAG, "insertmultiComponentContentValues()");
        int size = list.size();
        if (size <= 0) {
            LogUtil.e(TAG, "insertMultiComponentContentValues the count is null.");
            return null;
        }
        int i = size / MAX_COMPONENT_COUNT;
        ArrayList arrayList = new ArrayList(MAX_COMPONENT_COUNT);
        Uri uriDoInsertMultiComponentValues = null;
        int i2 = 0;
        for (int i3 = 0; i3 <= i; i3++) {
            for (int i4 = 0; i4 < MAX_COMPONENT_COUNT && i2 < size; i4++) {
                arrayList.add(i4, list.get(i2));
                i2++;
            }
            uriDoInsertMultiComponentValues = doInsertMultiComponentValues(arrayList);
        }
        return uriDoInsertMultiComponentValues;
    }

    private Uri doInsertMultiComponentValues(List<SingleComponentContentValues> list) {
        ContentProviderResult[] contentProviderResultArrApplyBatch;
        ContentResolver contentResolver = this.mContext.getContentResolver();
        ArrayList<ContentProviderOperation> arrayList = new ArrayList<>();
        for (SingleComponentContentValues singleComponentContentValues : list) {
            LogUtil.d(TAG, "insertContentValues(): Alarms count: " + singleComponentContentValues.alarmValuesList.size() + ", Attendees count:" + singleComponentContentValues.attendeeValuesList.size());
            int size = arrayList.size();
            arrayList.add(ContentProviderOperation.newInsert(CalendarContract.Events.CONTENT_URI).withValues(singleComponentContentValues.contentValues).build());
            Iterator<ContentValues> it = singleComponentContentValues.alarmValuesList.iterator();
            while (it.hasNext()) {
                ContentProviderOperation.Builder builderWithValues = ContentProviderOperation.newInsert(CalendarContract.Reminders.CONTENT_URI).withValues(it.next());
                builderWithValues.withValueBackReference("event_id", size);
                arrayList.add(builderWithValues.build());
            }
            Iterator<ContentValues> it2 = singleComponentContentValues.attendeeValuesList.iterator();
            while (it2.hasNext()) {
                ContentProviderOperation.Builder builderWithValues2 = ContentProviderOperation.newInsert(CalendarContract.Attendees.CONTENT_URI).withValues(it2.next());
                builderWithValues2.withValueBackReference("event_id", size);
                arrayList.add(builderWithValues2.build());
            }
        }
        try {
            contentProviderResultArrApplyBatch = contentResolver.applyBatch("com.android.calendar", arrayList);
        } catch (OperationApplicationException e) {
            e.printStackTrace();
            contentProviderResultArrApplyBatch = null;
        } catch (RemoteException e2) {
            e2.printStackTrace();
            contentProviderResultArrApplyBatch = null;
        }
        if (contentProviderResultArrApplyBatch != null && contentProviderResultArrApplyBatch[0] != null) {
            Uri uri = contentProviderResultArrApplyBatch[0].uri;
            LogUtil.v(TAG, "addNextContentValue: insert event=" + uri);
            return uri;
        }
        LogUtil.e(TAG, "addNextContentValue: insert event failed.");
        return null;
    }
}
