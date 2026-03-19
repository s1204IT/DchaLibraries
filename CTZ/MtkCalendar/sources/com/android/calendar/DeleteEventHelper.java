package com.android.calendar;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.database.Cursor;
import android.provider.CalendarContract;
import android.text.TextUtils;
import android.text.format.Time;
import android.widget.ArrayAdapter;
import com.android.calendar.event.EditEventHelper;
import com.android.calendarcommon2.EventRecurrence;
import com.mediatek.calendar.LogUtil;
import java.util.ArrayList;
import java.util.Arrays;

public class DeleteEventHelper {
    private AlertDialog mAlertDialog;
    private Runnable mCallback;
    private Context mContext;
    private DialogInterface.OnDismissListener mDismissListener;
    private long mEndMillis;
    private boolean mExitWhenDone;
    private CalendarEventModel mModel;
    private final Activity mParent;
    private AsyncQueryService mService;
    private long mStartMillis;
    private String mSyncId;
    private int mWhichDelete;
    private ArrayList<Integer> mWhichIndex;
    private DeleteNotifyListener mDeleteStartedListener = null;
    private DialogInterface.OnClickListener mDeleteNormalDialogListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
            DeleteEventHelper.this.deleteStarted();
            DeleteEventHelper.this.mService.startDelete(DeleteEventHelper.this.mService.getNextToken(), null, ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, DeleteEventHelper.this.mModel.mId), null, null, 0L);
            if (DeleteEventHelper.this.mCallback != null) {
                DeleteEventHelper.this.mCallback.run();
            }
            if (DeleteEventHelper.this.mExitWhenDone) {
                DeleteEventHelper.this.mParent.finish();
            }
        }
    };
    private DialogInterface.OnClickListener mDeleteExceptionDialogListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
            DeleteEventHelper.this.deleteStarted();
            DeleteEventHelper.this.deleteExceptionEvent();
            if (DeleteEventHelper.this.mCallback != null) {
                DeleteEventHelper.this.mCallback.run();
            }
            if (DeleteEventHelper.this.mExitWhenDone) {
                DeleteEventHelper.this.mParent.finish();
            }
        }
    };
    private DialogInterface.OnClickListener mDeleteListListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
            DeleteEventHelper.this.mWhichDelete = ((Integer) DeleteEventHelper.this.mWhichIndex.get(i)).intValue();
            DeleteEventHelper.this.mAlertDialog.getButton(-1).setEnabled(true);
        }
    };
    private DialogInterface.OnClickListener mDeleteRepeatingDialogListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
            DeleteEventHelper.this.deleteStarted();
            if (DeleteEventHelper.this.mWhichDelete != -1) {
                DeleteEventHelper.this.deleteRepeatingEvent(DeleteEventHelper.this.mWhichDelete);
            }
        }
    };

    public interface DeleteNotifyListener {
        void onDeleteStarted();
    }

    public DeleteEventHelper(Context context, Activity activity, boolean z) {
        if (z && activity == null) {
            throw new IllegalArgumentException("parentActivity is required to exit when done");
        }
        this.mContext = context;
        this.mParent = activity;
        this.mService = new AsyncQueryService(this.mContext) {
            @Override
            protected void onQueryComplete(int i, Object obj, Cursor cursor) {
                if (cursor == null || cursor.getCount() != 1 || (DeleteEventHelper.this.mParent != null && !DeleteEventHelper.this.mParent.isResumed())) {
                    if (cursor != null) {
                        cursor.close();
                    }
                } else {
                    cursor.moveToFirst();
                    CalendarEventModel calendarEventModel = new CalendarEventModel();
                    EditEventHelper.setModelFromCursor(calendarEventModel, cursor);
                    cursor.close();
                    DeleteEventHelper.this.delete(DeleteEventHelper.this.mStartMillis, DeleteEventHelper.this.mEndMillis, calendarEventModel, DeleteEventHelper.this.mWhichDelete);
                }
            }

            @Override
            protected void onUpdateComplete(int i, Object obj, int i2) {
                LogUtil.d("DeleteEventHelper", "onUpdateComplete, in DeleteEventHelper.");
            }
        };
        this.mExitWhenDone = z;
    }

    public void delete(long j, long j2, long j3, int i) {
        this.mService.startQuery(this.mService.getNextToken(), null, ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, j3), EditEventHelper.EVENT_PROJECTION, null, null, null);
        this.mStartMillis = j;
        this.mEndMillis = j2;
        this.mWhichDelete = i;
    }

    public void delete(long j, long j2, long j3, int i, Runnable runnable) {
        delete(j, j2, j3, i);
        this.mCallback = runnable;
    }

    public void delete(long j, long j2, CalendarEventModel calendarEventModel, int i) {
        this.mWhichDelete = i;
        this.mStartMillis = j;
        this.mEndMillis = j2;
        this.mModel = calendarEventModel;
        this.mSyncId = calendarEventModel.mSyncId;
        String str = calendarEventModel.mRrule;
        String str2 = calendarEventModel.mOriginalSyncId;
        if (TextUtils.isEmpty(str)) {
            AlertDialog alertDialogCreate = new AlertDialog.Builder(this.mContext).setMessage(R.string.delete_this_event_title).setIconAttribute(android.R.attr.alertDialogIcon).setNegativeButton(android.R.string.cancel, (DialogInterface.OnClickListener) null).create();
            if (str2 == null) {
                alertDialogCreate.setButton(-1, this.mContext.getText(android.R.string.ok), this.mDeleteNormalDialogListener);
            } else {
                alertDialogCreate.setButton(-1, this.mContext.getText(android.R.string.ok), this.mDeleteExceptionDialogListener);
            }
            alertDialogCreate.setOnDismissListener(this.mDismissListener);
            if (this.mParent != null && this.mParent.isFinishing()) {
                LogUtil.w("DeleteEventHelper", "delete failed, parent is finishing.");
                return;
            } else {
                alertDialogCreate.show();
                this.mAlertDialog = alertDialogCreate;
                return;
            }
        }
        Resources resources = this.mContext.getResources();
        ArrayList arrayList = new ArrayList(Arrays.asList(resources.getStringArray(R.array.delete_repeating_labels)));
        int[] intArray = resources.getIntArray(R.array.delete_repeating_values);
        ArrayList<Integer> arrayList2 = new ArrayList<>();
        for (int i2 : intArray) {
            arrayList2.add(Integer.valueOf(i2));
        }
        if (this.mSyncId == null) {
            arrayList.remove(0);
            arrayList2.remove(0);
            if (!calendarEventModel.mIsOrganizer) {
                arrayList.remove(0);
                arrayList2.remove(0);
            }
        } else if (!calendarEventModel.mIsOrganizer) {
            arrayList.remove(1);
            arrayList2.remove(1);
        }
        if (i != -1) {
            i = arrayList2.indexOf(Integer.valueOf(i));
        }
        this.mWhichIndex = arrayList2;
        AlertDialog alertDialogCreate2 = new AlertDialog.Builder(this.mContext).setTitle(this.mContext.getString(R.string.delete_recurring_event_title, calendarEventModel.mTitle)).setIconAttribute(android.R.attr.alertDialogIcon).setSingleChoiceItems(new ArrayAdapter(this.mContext, android.R.layout.simple_list_item_single_choice, arrayList), i, this.mDeleteListListener).setPositiveButton(android.R.string.ok, this.mDeleteRepeatingDialogListener).setNegativeButton(android.R.string.cancel, (DialogInterface.OnClickListener) null).create();
        if (this.mParent != null && this.mParent.isFinishing()) {
            LogUtil.w("DeleteEventHelper", "delete failed, parent is finishing.");
            return;
        }
        alertDialogCreate2.show();
        alertDialogCreate2.setOnDismissListener(this.mDismissListener);
        this.mAlertDialog = alertDialogCreate2;
        if (i == -1) {
            alertDialogCreate2.getButton(-1).setEnabled(false);
        }
    }

    private void deleteExceptionEvent() {
        long j = this.mModel.mId;
        ContentValues contentValues = new ContentValues();
        contentValues.put("eventStatus", (Integer) 2);
        this.mService.startUpdate(this.mService.getNextToken(), null, ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, j), contentValues, null, null, 0L);
    }

    private void deleteRepeatingEvent(int i) {
        String str = this.mModel.mRrule;
        boolean z = this.mModel.mAllDay;
        long j = this.mModel.mStart;
        long j2 = this.mModel.mId;
        switch (i) {
            case 0:
                long j3 = this.mStartMillis;
                ContentValues contentValues = new ContentValues();
                contentValues.put("title", this.mModel.mTitle);
                String str2 = this.mModel.mTimezone;
                long j4 = this.mModel.mCalendarId;
                contentValues.put("eventTimezone", str2);
                contentValues.put("allDay", Integer.valueOf(z ? 1 : 0));
                contentValues.put("originalAllDay", Integer.valueOf(z ? 1 : 0));
                contentValues.put("calendar_id", Long.valueOf(j4));
                contentValues.put("dtstart", Long.valueOf(this.mStartMillis));
                contentValues.put("dtend", Long.valueOf(this.mEndMillis));
                contentValues.put("original_sync_id", this.mSyncId);
                contentValues.put("original_id", Long.valueOf(j2));
                contentValues.put("originalInstanceTime", Long.valueOf(this.mStartMillis));
                contentValues.put("eventStatus", (Integer) 2);
                this.mService.startInsert(this.mService.getNextToken(), null, CalendarContract.Events.CONTENT_URI, contentValues, 0L);
                break;
            case 1:
                if (j == this.mStartMillis) {
                    this.mService.startDelete(this.mService.getNextToken(), null, ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, j2), null, null, 0L);
                } else {
                    EventRecurrence eventRecurrence = new EventRecurrence();
                    eventRecurrence.parse(str);
                    Time time = new Time();
                    if (z) {
                        time.timezone = "UTC";
                    }
                    time.set(this.mStartMillis);
                    time.second--;
                    time.normalize(false);
                    time.switchTimezone("UTC");
                    eventRecurrence.until = time.format2445();
                    ContentValues contentValues2 = new ContentValues();
                    contentValues2.put("dtstart", Long.valueOf(j));
                    contentValues2.put("rrule", eventRecurrence.toString());
                    this.mService.startUpdate(this.mService.getNextToken(), null, ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, j2), contentValues2, null, null, 0L);
                }
                break;
            case 2:
                this.mService.startDelete(this.mService.getNextToken(), null, ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, j2), null, null, 0L);
                break;
        }
        if (this.mCallback != null) {
            this.mCallback.run();
        }
        if (this.mExitWhenDone) {
            this.mParent.finish();
        }
    }

    public void setDeleteNotificationListener(DeleteNotifyListener deleteNotifyListener) {
        this.mDeleteStartedListener = deleteNotifyListener;
    }

    private void deleteStarted() {
        if (this.mDeleteStartedListener != null) {
            this.mDeleteStartedListener.onDeleteStarted();
        }
    }

    public void setOnDismissListener(DialogInterface.OnDismissListener onDismissListener) {
        if (this.mAlertDialog != null) {
            this.mAlertDialog.setOnDismissListener(onDismissListener);
        }
        this.mDismissListener = onDismissListener;
    }

    public void dismissAlertDialog() {
        if (this.mAlertDialog != null) {
            this.mAlertDialog.dismiss();
        }
    }

    public int getWhichDelete() {
        return this.mWhichDelete;
    }
}
