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

public class VEventInsertHelper extends ComponentInsertHelper {
    private Context mContext;

    protected VEventInsertHelper(Context context) {
        this.mContext = context;
    }

    @Override
    Uri insertContentValues(SingleComponentContentValues singleComponentContentValues) {
        ContentProviderResult[] contentProviderResultArrApplyBatch;
        LogUtil.i("VEventInsertHelper", "insertContentValues()");
        ContentResolver contentResolver = this.mContext.getContentResolver();
        int size = singleComponentContentValues.alarmValuesList.size();
        int size2 = singleComponentContentValues.attendeeValuesList.size();
        LogUtil.d("VEventInsertHelper", "insertContentValues(): Alarms count: " + size);
        LogUtil.d("VEventInsertHelper", "insertContentValues(): Attendees count:" + size2);
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
            LogUtil.v("VEventInsertHelper", "addNextContentValue: insert event=" + uri);
            return uri;
        }
        LogUtil.e("VEventInsertHelper", "addNextContentValue: insert event failed.");
        return null;
    }
}
