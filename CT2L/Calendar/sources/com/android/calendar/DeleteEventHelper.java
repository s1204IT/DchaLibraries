package com.android.calendar;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract;
import android.text.TextUtils;
import android.text.format.Time;
import android.widget.ArrayAdapter;
import android.widget.Button;
import com.android.calendar.event.EditEventHelper;
import com.android.calendarcommon2.EventRecurrence;
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
        public void onClick(DialogInterface dialog, int button) {
            DeleteEventHelper.this.deleteStarted();
            long id = DeleteEventHelper.this.mModel.mId;
            Uri uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, id);
            DeleteEventHelper.this.mService.startDelete(DeleteEventHelper.this.mService.getNextToken(), null, uri, null, null, 0L);
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
        public void onClick(DialogInterface dialog, int button) {
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
        public void onClick(DialogInterface dialog, int button) {
            DeleteEventHelper.this.mWhichDelete = ((Integer) DeleteEventHelper.this.mWhichIndex.get(button)).intValue();
            Button ok = DeleteEventHelper.this.mAlertDialog.getButton(-1);
            ok.setEnabled(true);
        }
    };
    private DialogInterface.OnClickListener mDeleteRepeatingDialogListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int button) {
            DeleteEventHelper.this.deleteStarted();
            if (DeleteEventHelper.this.mWhichDelete != -1) {
                DeleteEventHelper.this.deleteRepeatingEvent(DeleteEventHelper.this.mWhichDelete);
            }
        }
    };

    public interface DeleteNotifyListener {
        void onDeleteStarted();
    }

    public DeleteEventHelper(Context context, Activity parentActivity, boolean exitWhenDone) {
        if (exitWhenDone && parentActivity == null) {
            throw new IllegalArgumentException("parentActivity is required to exit when done");
        }
        this.mContext = context;
        this.mParent = parentActivity;
        this.mService = new AsyncQueryService(this.mContext) {
            @Override
            protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
                if (cursor != null) {
                    cursor.moveToFirst();
                    CalendarEventModel mModel = new CalendarEventModel();
                    EditEventHelper.setModelFromCursor(mModel, cursor);
                    cursor.close();
                    DeleteEventHelper.this.delete(DeleteEventHelper.this.mStartMillis, DeleteEventHelper.this.mEndMillis, mModel, DeleteEventHelper.this.mWhichDelete);
                }
            }
        };
        this.mExitWhenDone = exitWhenDone;
    }

    public void delete(long begin, long end, long eventId, int which) {
        Uri uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId);
        this.mService.startQuery(this.mService.getNextToken(), null, uri, EditEventHelper.EVENT_PROJECTION, null, null, null);
        this.mStartMillis = begin;
        this.mEndMillis = end;
        this.mWhichDelete = which;
    }

    public void delete(long begin, long end, long eventId, int which, Runnable callback) {
        delete(begin, end, eventId, which);
        this.mCallback = callback;
    }

    public void delete(long begin, long end, CalendarEventModel model, int which) {
        this.mWhichDelete = which;
        this.mStartMillis = begin;
        this.mEndMillis = end;
        this.mModel = model;
        this.mSyncId = model.mSyncId;
        String rRule = model.mRrule;
        String originalEvent = model.mOriginalSyncId;
        if (TextUtils.isEmpty(rRule)) {
            AlertDialog dialog = new AlertDialog.Builder(this.mContext).setMessage(R.string.delete_this_event_title).setIconAttribute(android.R.attr.alertDialogIcon).setNegativeButton(android.R.string.cancel, (DialogInterface.OnClickListener) null).create();
            if (originalEvent == null) {
                dialog.setButton(-1, this.mContext.getText(android.R.string.ok), this.mDeleteNormalDialogListener);
            } else {
                dialog.setButton(-1, this.mContext.getText(android.R.string.ok), this.mDeleteExceptionDialogListener);
            }
            dialog.setOnDismissListener(this.mDismissListener);
            dialog.show();
            this.mAlertDialog = dialog;
            return;
        }
        Resources res = this.mContext.getResources();
        ArrayList<String> labelArray = new ArrayList<>(Arrays.asList(res.getStringArray(R.array.delete_repeating_labels)));
        int[] labelValues = res.getIntArray(R.array.delete_repeating_values);
        ArrayList<Integer> labelIndex = new ArrayList<>();
        for (int val : labelValues) {
            labelIndex.add(Integer.valueOf(val));
        }
        if (this.mSyncId == null) {
            labelArray.remove(0);
            labelIndex.remove(0);
            if (!model.mIsOrganizer) {
                labelArray.remove(0);
                labelIndex.remove(0);
            }
        } else if (!model.mIsOrganizer) {
            labelArray.remove(1);
            labelIndex.remove(1);
        }
        if (which != -1) {
            which = labelIndex.indexOf(Integer.valueOf(which));
        }
        this.mWhichIndex = labelIndex;
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this.mContext, android.R.layout.simple_list_item_single_choice, labelArray);
        AlertDialog dialog2 = new AlertDialog.Builder(this.mContext).setTitle(this.mContext.getString(R.string.delete_recurring_event_title, model.mTitle)).setIconAttribute(android.R.attr.alertDialogIcon).setSingleChoiceItems(adapter, which, this.mDeleteListListener).setPositiveButton(android.R.string.ok, this.mDeleteRepeatingDialogListener).setNegativeButton(android.R.string.cancel, (DialogInterface.OnClickListener) null).show();
        dialog2.setOnDismissListener(this.mDismissListener);
        this.mAlertDialog = dialog2;
        if (which == -1) {
            Button ok = dialog2.getButton(-1);
            ok.setEnabled(false);
        }
    }

    private void deleteExceptionEvent() {
        long id = this.mModel.mId;
        ContentValues values = new ContentValues();
        values.put("eventStatus", (Integer) 2);
        Uri uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, id);
        this.mService.startUpdate(this.mService.getNextToken(), null, uri, values, null, null, 0L);
    }

    private void deleteRepeatingEvent(int which) {
        String rRule = this.mModel.mRrule;
        boolean allDay = this.mModel.mAllDay;
        long dtstart = this.mModel.mStart;
        long id = this.mModel.mId;
        switch (which) {
            case 0:
                if (dtstart == this.mStartMillis) {
                }
                ContentValues values = new ContentValues();
                String title = this.mModel.mTitle;
                values.put("title", title);
                String timezone = this.mModel.mTimezone;
                long calendarId = this.mModel.mCalendarId;
                values.put("eventTimezone", timezone);
                values.put("allDay", Integer.valueOf(allDay ? 1 : 0));
                values.put("originalAllDay", Integer.valueOf(allDay ? 1 : 0));
                values.put("calendar_id", Long.valueOf(calendarId));
                values.put("dtstart", Long.valueOf(this.mStartMillis));
                values.put("dtend", Long.valueOf(this.mEndMillis));
                values.put("original_sync_id", this.mSyncId);
                values.put("original_id", Long.valueOf(id));
                values.put("originalInstanceTime", Long.valueOf(this.mStartMillis));
                values.put("eventStatus", (Integer) 2);
                this.mService.startInsert(this.mService.getNextToken(), null, CalendarContract.Events.CONTENT_URI, values, 0L);
                break;
            case 1:
                if (dtstart == this.mStartMillis) {
                    Uri uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, id);
                    this.mService.startDelete(this.mService.getNextToken(), null, uri, null, null, 0L);
                } else {
                    EventRecurrence eventRecurrence = new EventRecurrence();
                    eventRecurrence.parse(rRule);
                    Time date = new Time();
                    if (allDay) {
                        date.timezone = "UTC";
                    }
                    date.set(this.mStartMillis);
                    date.second--;
                    date.normalize(false);
                    date.switchTimezone("UTC");
                    eventRecurrence.until = date.format2445();
                    ContentValues values2 = new ContentValues();
                    values2.put("dtstart", Long.valueOf(dtstart));
                    values2.put("rrule", eventRecurrence.toString());
                    Uri uri2 = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, id);
                    this.mService.startUpdate(this.mService.getNextToken(), null, uri2, values2, null, null, 0L);
                }
                break;
            case 2:
                Uri uri3 = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, id);
                this.mService.startDelete(this.mService.getNextToken(), null, uri3, null, null, 0L);
                break;
        }
        if (this.mCallback != null) {
            this.mCallback.run();
        }
        if (this.mExitWhenDone) {
            this.mParent.finish();
        }
    }

    public void setDeleteNotificationListener(DeleteNotifyListener listener) {
        this.mDeleteStartedListener = listener;
    }

    private void deleteStarted() {
        if (this.mDeleteStartedListener != null) {
            this.mDeleteStartedListener.onDeleteStarted();
        }
    }

    public void setOnDismissListener(DialogInterface.OnDismissListener listener) {
        if (this.mAlertDialog != null) {
            this.mAlertDialog.setOnDismissListener(listener);
        }
        this.mDismissListener = listener;
    }

    public void dismissAlertDialog() {
        if (this.mAlertDialog != null) {
            this.mAlertDialog.dismiss();
        }
    }
}
