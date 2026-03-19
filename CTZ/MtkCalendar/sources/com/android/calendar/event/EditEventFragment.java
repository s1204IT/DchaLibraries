package com.android.calendar.event;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.AsyncQueryHandler;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.provider.CalendarContract;
import android.text.TextUtils;
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;
import android.widget.Toast;
import com.android.calendar.AsyncQueryService;
import com.android.calendar.CalendarController;
import com.android.calendar.CalendarEventModel;
import com.android.calendar.DeleteEventHelper;
import com.android.calendar.R;
import com.android.calendar.Utils;
import com.android.calendar.event.EditEventHelper;
import com.android.colorpicker.ColorPickerSwatch;
import com.android.colorpicker.HsvColorComparator;
import com.mediatek.calendar.LogUtil;
import com.mediatek.calendar.MTKUtils;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;

public class EditEventFragment extends Fragment implements CalendarController.EventHandler, ColorPickerSwatch.OnColorSelectedListener {
    private final View.OnClickListener mActionBarListener;
    private Activity mActivity;
    private long mBegin;
    private long mCalendarId;
    private EventColorPickerDialog mColorPickerDialog;
    private boolean mDateSelectedWasStartDate;
    private long mEnd;
    private final CalendarController.EventInfo mEvent;
    private EventBundle mEventBundle;
    private int mEventColor;
    private boolean mEventColorInitialized;
    QueryHandler mHandler;
    EditEventHelper mHelper;
    private InputMethodManager mInputMethodManager;
    private final Intent mIntent;
    private boolean mIsReadOnly;
    private boolean mIsSaveInstanceState;
    CalendarEventModel mModel;
    int mModification;
    private AlertDialog mModifyDialog;
    private View.OnClickListener mOnColorPickerClicked;
    private final Done mOnDone;
    CalendarEventModel mOriginalModel;
    private int mOutstandingQueries;
    private ArrayList<CalendarEventModel.ReminderEntry> mReminders;
    CalendarEventModel mRestoreModel;
    private boolean mSaveOnDetach;
    private int[] mSelectedDate;
    private boolean mShowColorPalette;
    private boolean mShowDatePicker;
    public boolean mShowModifyDialogOnLaunch;
    private boolean mTimeSelectedWasStartTime;
    private Uri mUri;
    private boolean mUseCustomActionBar;
    EditEventView mView;

    private class QueryHandler extends AsyncQueryHandler {
        @Override
        public void handleMessage(Message message) {
            if (message.what == 1073741824) {
                EditEventFragment.this.mView.requestFocus();
            } else {
                super.handleMessage(message);
            }
        }

        public QueryHandler(ContentResolver contentResolver) {
            super(contentResolver);
        }

        @Override
        protected void onQueryComplete(int i, Object obj, Cursor cursor) {
            if (cursor == null) {
                return;
            }
            Activity activity = EditEventFragment.this.getActivity();
            if (activity == null || activity.isFinishing()) {
                return;
            }
            boolean z = true;
            if (i == 4) {
                while (cursor.moveToNext()) {
                    try {
                        CalendarEventModel.ReminderEntry reminderEntryValueOf = CalendarEventModel.ReminderEntry.valueOf(cursor.getInt(1), cursor.getInt(2));
                        EditEventFragment.this.mModel.mReminders.add(reminderEntryValueOf);
                        EditEventFragment.this.mOriginalModel.mReminders.add(reminderEntryValueOf);
                    } finally {
                    }
                }
                Collections.sort(EditEventFragment.this.mModel.mReminders);
                Collections.sort(EditEventFragment.this.mOriginalModel.mReminders);
                cursor.close();
                EditEventFragment.this.setModelIfDone(4);
                return;
            }
            if (i == 8) {
                try {
                    if (EditEventFragment.this.mUri != null && EditEventFragment.this.mModel.mId != -1) {
                        EditEventHelper.setModelFromCalendarCursor(EditEventFragment.this.mModel, cursor);
                        EditEventHelper.setModelFromCalendarCursor(EditEventFragment.this.mOriginalModel, cursor);
                    } else {
                        MatrixCursor matrixCursorMatrixCursorFromCursor = Utils.matrixCursorFromCursor(cursor);
                        EditEventView editEventView = EditEventFragment.this.mView;
                        if (!EditEventFragment.this.isAdded() || !EditEventFragment.this.isResumed()) {
                            z = false;
                        }
                        editEventView.setCalendarsCursor(matrixCursorMatrixCursorFromCursor, z, EditEventFragment.this.mCalendarId);
                    }
                    cursor.close();
                    EditEventFragment.this.setModelIfDone(8);
                    return;
                } finally {
                }
            }
            if (i != 16) {
                switch (i) {
                    case 1:
                        if (cursor.getCount() == 0) {
                            cursor.close();
                            EditEventFragment.this.mOnDone.setDoneCode(1);
                            EditEventFragment.this.mSaveOnDetach = false;
                            EditEventFragment.this.mOnDone.run();
                            return;
                        }
                        EditEventFragment.this.mOriginalModel = new CalendarEventModel();
                        EditEventHelper.setModelFromCursor(EditEventFragment.this.mOriginalModel, cursor);
                        EditEventHelper.setModelFromCursor(EditEventFragment.this.mModel, cursor);
                        cursor.close();
                        EditEventFragment.this.mOriginalModel.mUri = EditEventFragment.this.mUri.toString();
                        EditEventFragment.this.mModel.mUri = EditEventFragment.this.mUri.toString();
                        EditEventFragment.this.mModel.mOriginalStart = EditEventFragment.this.mBegin;
                        EditEventFragment.this.mModel.mOriginalEnd = EditEventFragment.this.mEnd;
                        EditEventFragment.this.mModel.mIsFirstEventInSeries = EditEventFragment.this.mBegin == EditEventFragment.this.mOriginalModel.mStart;
                        EditEventFragment.this.mModel.mStart = EditEventFragment.this.mBegin;
                        EditEventFragment.this.mModel.mEnd = EditEventFragment.this.mEnd;
                        if (EditEventFragment.this.mEventColorInitialized) {
                            EditEventFragment.this.mModel.setEventColor(EditEventFragment.this.mEventColor);
                        }
                        long j = EditEventFragment.this.mModel.mId;
                        if (!EditEventFragment.this.mModel.mHasAttendeeData || j == -1) {
                            EditEventFragment.this.setModelIfDone(2);
                        } else {
                            EditEventFragment.this.mHandler.startQuery(2, null, CalendarContract.Attendees.CONTENT_URI, EditEventHelper.ATTENDEES_PROJECTION, "event_id=? AND attendeeEmail IS NOT NULL", new String[]{Long.toString(j)}, null);
                        }
                        if (!EditEventFragment.this.mModel.mHasAlarm || EditEventFragment.this.mReminders != null) {
                            if (EditEventFragment.this.mReminders == null) {
                                EditEventFragment.this.mReminders = new ArrayList();
                            } else {
                                Collections.sort(EditEventFragment.this.mReminders);
                            }
                            EditEventFragment.this.mOriginalModel.mReminders = EditEventFragment.this.mReminders;
                            EditEventFragment.this.mModel.mReminders = (ArrayList) EditEventFragment.this.mReminders.clone();
                            EditEventFragment.this.setModelIfDone(4);
                        } else {
                            EditEventFragment.this.mHandler.startQuery(4, null, CalendarContract.Reminders.CONTENT_URI, EditEventHelper.REMINDERS_PROJECTION, "event_id=?", new String[]{Long.toString(j)}, null);
                        }
                        EditEventFragment.this.mHandler.startQuery(8, null, CalendarContract.Calendars.CONTENT_URI, EditEventHelper.CALENDARS_PROJECTION, "_id=?", new String[]{Long.toString(EditEventFragment.this.mModel.mCalendarId)}, null);
                        EditEventFragment.this.mHandler.startQuery(16, null, CalendarContract.Colors.CONTENT_URI, EditEventHelper.COLORS_PROJECTION, "color_type=1", null, null);
                        EditEventFragment.this.setModelIfDone(1);
                        return;
                    case 2:
                        break;
                    default:
                        return;
                }
                while (cursor.moveToNext()) {
                    try {
                        String string = cursor.getString(1);
                        String string2 = cursor.getString(2);
                        int i2 = cursor.getInt(4);
                        if (cursor.getInt(3) == 2) {
                            if (string2 != null) {
                                EditEventFragment.this.mModel.mOrganizer = string2;
                                EditEventFragment.this.mModel.mIsOrganizer = EditEventFragment.this.mModel.mOwnerAccount.equalsIgnoreCase(string2);
                                EditEventFragment.this.mOriginalModel.mOrganizer = string2;
                                EditEventFragment.this.mOriginalModel.mIsOrganizer = EditEventFragment.this.mOriginalModel.mOwnerAccount.equalsIgnoreCase(string2);
                            }
                            if (TextUtils.isEmpty(string)) {
                                EditEventFragment.this.mModel.mOrganizerDisplayName = EditEventFragment.this.mModel.mOrganizer;
                                EditEventFragment.this.mOriginalModel.mOrganizerDisplayName = EditEventFragment.this.mOriginalModel.mOrganizer;
                            } else {
                                EditEventFragment.this.mModel.mOrganizerDisplayName = string;
                                EditEventFragment.this.mOriginalModel.mOrganizerDisplayName = string;
                            }
                        }
                        if (string2 != null && EditEventFragment.this.mModel.mOwnerAccount != null && EditEventFragment.this.mModel.mOwnerAccount.equalsIgnoreCase(string2)) {
                            int i3 = cursor.getInt(0);
                            EditEventFragment.this.mModel.mOwnerAttendeeId = i3;
                            EditEventFragment.this.mModel.mSelfAttendeeStatus = i2;
                            EditEventFragment.this.mOriginalModel.mOwnerAttendeeId = i3;
                            EditEventFragment.this.mOriginalModel.mSelfAttendeeStatus = i2;
                        } else {
                            CalendarEventModel.Attendee attendee = new CalendarEventModel.Attendee(string, string2);
                            attendee.mStatus = i2;
                            EditEventFragment.this.mModel.addAttendee(attendee);
                            EditEventFragment.this.mOriginalModel.addAttendee(attendee);
                        }
                    } finally {
                    }
                }
                cursor.close();
                EditEventFragment.this.setModelIfDone(2);
                return;
            }
            if (cursor.moveToFirst()) {
                EventColorCache eventColorCache = new EventColorCache();
                do {
                    eventColorCache.insertColor(cursor.getString(1), cursor.getString(2), Utils.getDisplayColorFromColor(cursor.getInt(3)), cursor.getInt(4));
                } while (cursor.moveToNext());
                eventColorCache.sortPalettes(new HsvColorComparator());
                EditEventFragment.this.mModel.mEventColorCache = eventColorCache;
                EditEventFragment.this.mView.mColorPickerNewEvent.setOnClickListener(EditEventFragment.this.mOnColorPickerClicked);
                EditEventFragment.this.mView.mColorPickerExistingEvent.setOnClickListener(EditEventFragment.this.mOnColorPickerClicked);
            }
            if (cursor != null) {
            }
            if (EditEventFragment.this.mModel.mCalendarAccountName == null || EditEventFragment.this.mModel.mCalendarAccountType == null) {
                EditEventFragment.this.mView.setColorPickerButtonStates(EditEventFragment.this.mShowColorPalette);
            } else {
                EditEventFragment.this.mView.setColorPickerButtonStates(EditEventFragment.this.mModel.getCalendarEventColors());
            }
            EditEventFragment.this.setModelIfDone(16);
        }
    }

    private void setModelIfDone(int i) {
        this.mOutstandingQueries = (~i) & this.mOutstandingQueries;
        if (this.mOutstandingQueries == 0) {
            if (this.mRestoreModel != null) {
                this.mModel = this.mRestoreModel;
            }
            if (this.mShowModifyDialogOnLaunch && this.mModification == 0) {
                if (!TextUtils.isEmpty(this.mModel.mRrule)) {
                    displayEditWhichDialog();
                } else {
                    this.mModification = 3;
                }
            }
            this.mView.setModel(this.mModel);
            if (this.mOriginalModel != null) {
                formatOriginalRrule();
            }
            this.mView.setModification(this.mModification);
            this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(1073741824), 100L);
        }
    }

    public EditEventFragment() {
        this(null, null, false, -1, false, null);
    }

    public EditEventFragment(CalendarController.EventInfo eventInfo, ArrayList<CalendarEventModel.ReminderEntry> arrayList, boolean z, int i, boolean z2, Intent intent) {
        this.mShowDatePicker = false;
        this.mSelectedDate = null;
        this.mOutstandingQueries = Integer.MIN_VALUE;
        this.mModification = 0;
        this.mEventColorInitialized = false;
        this.mCalendarId = -1L;
        this.mOnDone = new Done();
        this.mSaveOnDetach = true;
        this.mIsReadOnly = false;
        this.mShowModifyDialogOnLaunch = false;
        this.mShowColorPalette = false;
        this.mActionBarListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EditEventFragment.this.onActionBarItemSelected(view.getId());
            }
        };
        this.mOnColorPickerClicked = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int[] calendarEventColors = EditEventFragment.this.mModel.getCalendarEventColors();
                if (EditEventFragment.this.mColorPickerDialog != null) {
                    EditEventFragment.this.mColorPickerDialog.setCalendarColor(EditEventFragment.this.mModel.getCalendarColor());
                    EditEventFragment.this.mColorPickerDialog.setColors(calendarEventColors, EditEventFragment.this.mModel.getEventColor());
                } else {
                    EditEventFragment.this.mColorPickerDialog = EventColorPickerDialog.newInstance(calendarEventColors, EditEventFragment.this.mModel.getEventColor(), EditEventFragment.this.mModel.getCalendarColor(), EditEventFragment.this.mView.mIsMultipane);
                    EditEventFragment.this.mColorPickerDialog.setOnColorSelectedListener(EditEventFragment.this);
                }
                FragmentManager fragmentManager = EditEventFragment.this.getFragmentManager();
                fragmentManager.executePendingTransactions();
                if (!EditEventFragment.this.mColorPickerDialog.isAdded()) {
                    EditEventFragment.this.mColorPickerDialog.show(fragmentManager, "ColorPickerDialog");
                }
            }
        };
        this.mIsSaveInstanceState = false;
        this.mEvent = eventInfo;
        this.mIsReadOnly = z2;
        this.mIntent = intent;
        this.mReminders = arrayList;
        this.mEventColorInitialized = z;
        if (z) {
            this.mEventColor = i;
        }
        setHasOptionsMenu(true);
    }

    @Override
    public void onActivityCreated(Bundle bundle) {
        super.onActivityCreated(bundle);
        this.mColorPickerDialog = (EventColorPickerDialog) getActivity().getFragmentManager().findFragmentByTag("ColorPickerDialog");
        if (this.mColorPickerDialog != null) {
            this.mColorPickerDialog.setOnColorSelectedListener(this);
        }
    }

    private void startQuery() {
        this.mUri = null;
        this.mBegin = -1L;
        this.mEnd = -1L;
        if (this.mEvent != null) {
            if (this.mEvent.id != -1) {
                this.mModel.mId = this.mEvent.id;
                this.mUri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, this.mEvent.id);
            } else {
                this.mModel.mAllDay = this.mEvent.extraLong == 16;
            }
            if (this.mEvent.startTime != null) {
                this.mBegin = this.mEvent.startTime.toMillis(true);
            }
            if (this.mEvent.endTime != null) {
                this.mEnd = this.mEvent.endTime.toMillis(true);
            }
            if (this.mEvent.calendarId != -1) {
                this.mCalendarId = this.mEvent.calendarId;
            }
        } else if (this.mEventBundle != null) {
            if (this.mEventBundle.id != -1) {
                this.mModel.mId = this.mEventBundle.id;
                this.mUri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, this.mEventBundle.id);
            }
            this.mBegin = this.mEventBundle.start;
            this.mEnd = this.mEventBundle.end;
        }
        if (this.mReminders != null) {
            this.mModel.mReminders = this.mReminders;
        }
        if (this.mEventColorInitialized) {
            this.mModel.setEventColor(this.mEventColor);
        }
        if (this.mBegin <= 0) {
            this.mBegin = this.mHelper.constructDefaultStartTime(System.currentTimeMillis());
        }
        if (this.mEnd < this.mBegin) {
            this.mEnd = this.mHelper.constructDefaultEndTime(this.mBegin);
        }
        if (this.mUri == null) {
            this.mOutstandingQueries = 24;
            this.mModel.mOriginalStart = this.mBegin;
            this.mModel.mOriginalEnd = this.mEnd;
            this.mModel.mStart = this.mBegin;
            this.mModel.mEnd = this.mEnd;
            this.mModel.mCalendarId = this.mCalendarId;
            this.mModel.mSelfAttendeeStatus = 1;
            this.mHandler.startQuery(8, null, CalendarContract.Calendars.CONTENT_URI, EditEventHelper.CALENDARS_PROJECTION, "calendar_access_level>=500 AND visible=1", null, null);
            this.mHandler.startQuery(16, null, CalendarContract.Colors.CONTENT_URI, EditEventHelper.COLORS_PROJECTION, "color_type=1", null, null);
            this.mModification = 3;
            this.mView.setModification(this.mModification);
            return;
        }
        this.mModel.mCalendarAccessLevel = 0;
        this.mOutstandingQueries = 31;
        this.mHandler.startQuery(1, null, this.mUri, EditEventHelper.EVENT_PROJECTION, null, null, null);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.mActivity = activity;
        this.mHelper = new EditEventHelper(activity, null);
        this.mHandler = new QueryHandler(activity.getContentResolver());
        this.mModel = new CalendarEventModel(activity, this.mIntent);
        this.mInputMethodManager = (InputMethodManager) activity.getSystemService("input_method");
        this.mUseCustomActionBar = !Utils.getConfigBool(this.mActivity, R.bool.multiple_pane_config);
    }

    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        View viewInflate;
        if (this.mIsReadOnly) {
            viewInflate = layoutInflater.inflate(R.layout.edit_event_single_column, (ViewGroup) null);
        } else {
            viewInflate = layoutInflater.inflate(R.layout.edit_event, (ViewGroup) null);
        }
        this.mView = new EditEventView(this.mActivity, viewInflate, this.mOnDone, this.mTimeSelectedWasStartTime, this.mDateSelectedWasStartDate, this.mShowDatePicker, this.mSelectedDate, bundle != null);
        startQuery();
        if (this.mUseCustomActionBar) {
            View viewInflate2 = layoutInflater.inflate(R.layout.edit_event_custom_actionbar, (ViewGroup) new LinearLayout(this.mActivity), false);
            viewInflate2.findViewById(R.id.action_cancel).setOnClickListener(this.mActionBarListener);
            viewInflate2.findViewById(R.id.action_done).setOnClickListener(this.mActionBarListener);
            this.mActivity.getActionBar().setCustomView(viewInflate2);
        }
        return viewInflate;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (this.mUseCustomActionBar) {
            this.mActivity.getActionBar().setCustomView((View) null);
        }
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        if (bundle != null) {
            if (bundle.containsKey("key_model")) {
                this.mRestoreModel = (CalendarEventModel) bundle.getSerializable("key_model");
            }
            if (bundle.containsKey("key_edit_state")) {
                this.mModification = bundle.getInt("key_edit_state");
            }
            if (bundle.containsKey("key_edit_on_launch")) {
                this.mShowModifyDialogOnLaunch = bundle.getBoolean("key_edit_on_launch");
            }
            if (bundle.containsKey("key_event")) {
                this.mEventBundle = (EventBundle) bundle.getSerializable("key_event");
            }
            if (bundle.containsKey("key_read_only")) {
                this.mIsReadOnly = bundle.getBoolean("key_read_only");
            }
            if (bundle.containsKey("EditEventView_timebuttonclicked")) {
                this.mTimeSelectedWasStartTime = bundle.getBoolean("EditEventView_timebuttonclicked");
            }
            if (bundle.containsKey("date_button_clicked")) {
                this.mDateSelectedWasStartDate = bundle.getBoolean("date_button_clicked");
            }
            if (bundle.containsKey("show_color_palette")) {
                this.mShowColorPalette = bundle.getBoolean("show_color_palette");
            }
            if (bundle.containsKey("show_date_picker")) {
                this.mShowDatePicker = bundle.getBoolean("show_date_picker");
            }
            if (bundle.containsKey("selected_date")) {
                this.mSelectedDate = bundle.getIntArray("selected_date");
            }
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        super.onCreateOptionsMenu(menu, menuInflater);
        if (!this.mUseCustomActionBar) {
            menuInflater.inflate(R.menu.edit_event_title_bar, menu);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        return onActionBarItemSelected(menuItem.getItemId());
    }

    private boolean onActionBarItemSelected(int i) {
        if (i == R.id.action_done) {
            if (EditEventHelper.canModifyEvent(this.mModel) || EditEventHelper.canRespond(this.mModel)) {
                if (this.mView == null || !this.mView.prepareForSave()) {
                    this.mOnDone.setDoneCode(1);
                    this.mOnDone.run();
                } else {
                    if (this.mModification == 0) {
                        this.mModification = 3;
                    }
                    this.mOnDone.setDoneCode(3);
                    this.mOnDone.run();
                }
            } else if (!EditEventHelper.canAddReminders(this.mModel) || this.mModel.mId == -1 || this.mOriginalModel == null || !this.mView.prepareForSave()) {
                this.mOnDone.setDoneCode(1);
                this.mOnDone.run();
            } else {
                saveReminders();
                this.mOnDone.setDoneCode(1);
                this.mOnDone.run();
            }
        } else if (i == R.id.action_cancel) {
            this.mOnDone.setDoneCode(1);
            this.mOnDone.run();
        }
        return true;
    }

    private void saveReminders() {
        ArrayList<ContentProviderOperation> arrayList = new ArrayList<>(3);
        if (!EditEventHelper.saveReminders(arrayList, this.mModel.mId, this.mModel.mReminders, this.mOriginalModel.mReminders, false)) {
            return;
        }
        AsyncQueryService asyncQueryService = new AsyncQueryService(getActivity());
        asyncQueryService.startBatch(0, null, CalendarContract.Calendars.CONTENT_URI.getAuthority(), arrayList, 0L);
        Uri uriWithAppendedId = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, this.mModel.mId);
        int i = this.mModel.mReminders.size() > 0 ? 1 : 0;
        if (i != this.mOriginalModel.mHasAlarm) {
            ContentValues contentValues = new ContentValues();
            contentValues.put("hasAlarm", Integer.valueOf(i));
            asyncQueryService.startUpdate(0, null, uriWithAppendedId, contentValues, null, null, 0L);
        }
        Toast.makeText(this.mActivity, R.string.saving_event, 0).show();
    }

    protected void displayEditWhichDialog() {
        CharSequence[] charSequenceArr;
        if (this.mModification == 0) {
            final boolean zIsEmpty = TextUtils.isEmpty(this.mModel.mSyncId);
            boolean z = this.mModel.mIsFirstEventInSeries;
            int i = 0;
            if (zIsEmpty) {
                if (z) {
                    charSequenceArr = new CharSequence[1];
                } else {
                    charSequenceArr = new CharSequence[2];
                }
            } else {
                if (z) {
                    charSequenceArr = new CharSequence[2];
                } else {
                    charSequenceArr = new CharSequence[3];
                }
                charSequenceArr[0] = this.mActivity.getText(R.string.modify_event);
                i = 1;
            }
            int i2 = i + 1;
            charSequenceArr[i] = this.mActivity.getText(R.string.modify_all);
            if (!z) {
                charSequenceArr[i2] = this.mActivity.getText(R.string.modify_all_following);
            }
            if (this.mModifyDialog != null) {
                this.mModifyDialog.dismiss();
                this.mModifyDialog = null;
            }
            this.mModifyDialog = new AlertDialog.Builder(this.mActivity).setTitle(R.string.edit_event_label).setItems(charSequenceArr, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i3) {
                    int i4 = 3;
                    if (i3 == 0) {
                        EditEventFragment editEventFragment = EditEventFragment.this;
                        if (!zIsEmpty) {
                            i4 = 1;
                        }
                        editEventFragment.mModification = i4;
                        if (EditEventFragment.this.mModification == 1) {
                            EditEventFragment.this.mModel.mOriginalSyncId = zIsEmpty ? null : EditEventFragment.this.mModel.mSyncId;
                            EditEventFragment.this.mModel.mOriginalId = EditEventFragment.this.mModel.mId;
                        }
                    } else if (i3 == 1) {
                        EditEventFragment editEventFragment2 = EditEventFragment.this;
                        if (zIsEmpty) {
                            i4 = 2;
                        }
                        editEventFragment2.mModification = i4;
                    } else if (i3 == 2) {
                        EditEventFragment.this.mModification = 2;
                    }
                    EditEventFragment.this.mView.setModification(EditEventFragment.this.mModification);
                }
            }).show();
            this.mModifyDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialogInterface) {
                    Activity activity = EditEventFragment.this.getActivity();
                    if (activity != null) {
                        activity.finish();
                    }
                }
            });
        }
    }

    class Done implements EditEventHelper.EditDoneRunnable {
        private int mCode = -1;

        Done() {
        }

        @Override
        public void setDoneCode(int i) {
            this.mCode = i;
        }

        @Override
        public void run() {
            int i;
            EditEventFragment.this.mSaveOnDetach = false;
            if (EditEventFragment.this.mModification == 0) {
                EditEventFragment.this.mModification = 3;
            }
            if ((this.mCode & 2) != 0 && MTKUtils.isLowStorage(EditEventFragment.this.mActivity)) {
                MTKUtils.toastLowStorage(EditEventFragment.this.mActivity);
                Log.w("EditEventActivity", "Done runnable, in low storage state, not to save!");
                this.mCode &= -3;
            }
            if ((this.mCode & 2) != 0 && EditEventFragment.this.mModel != null && ((EditEventHelper.canRespond(EditEventFragment.this.mModel) || EditEventHelper.canModifyEvent(EditEventFragment.this.mModel)) && EditEventFragment.this.mView.prepareForSave() && !EditEventFragment.this.isEmptyNewEvent() && EditEventFragment.this.mModel.normalizeReminders() && EditEventFragment.this.mHelper.saveEvent(EditEventFragment.this.mModel, EditEventFragment.this.mOriginalModel, EditEventFragment.this.mModification))) {
                Toast.makeText(EditEventFragment.this.mActivity, (EditEventFragment.this.mModel.mAttendeesList.isEmpty() || "LOCAL".equals(EditEventFragment.this.mModel.mAccountType)) ? EditEventFragment.this.mModel.mUri != null ? R.string.saving_event : R.string.creating_event : EditEventFragment.this.mModel.mUri != null ? R.string.saving_event_with_guest : R.string.creating_event_with_guest, 0).show();
            } else if ((this.mCode & 2) != 0 && EditEventFragment.this.mModel != null && EditEventFragment.this.isEmptyNewEvent()) {
                Toast.makeText(EditEventFragment.this.mActivity, R.string.empty_event, 0).show();
            }
            if ((this.mCode & 4) != 0 && EditEventFragment.this.mOriginalModel != null && EditEventHelper.canModifyCalendar(EditEventFragment.this.mOriginalModel)) {
                long j = EditEventFragment.this.mModel.mStart;
                long j2 = EditEventFragment.this.mModel.mEnd;
                switch (EditEventFragment.this.mModification) {
                    case 1:
                        i = 0;
                        break;
                    case 2:
                        i = 1;
                        break;
                    case 3:
                        i = 2;
                        break;
                    default:
                        i = -1;
                        break;
                }
                new DeleteEventHelper(EditEventFragment.this.mActivity, EditEventFragment.this.mActivity, !EditEventFragment.this.mIsReadOnly).delete(j, j2, EditEventFragment.this.mOriginalModel, i);
            }
            if ((this.mCode & 1) != 0) {
                if ((this.mCode & 2) != 0 && EditEventFragment.this.mActivity != null) {
                    long millis = EditEventFragment.this.mModel.mStart;
                    long millis2 = EditEventFragment.this.mModel.mEnd;
                    if (EditEventFragment.this.mModel.mAllDay) {
                        String timeZone = Utils.getTimeZone(EditEventFragment.this.mActivity, null);
                        Time time = new Time("UTC");
                        time.set(millis);
                        time.timezone = timeZone;
                        millis = time.toMillis(true);
                        time.timezone = "UTC";
                        time.set(millis2);
                        time.timezone = timeZone;
                        millis2 = time.toMillis(true);
                    }
                    CalendarController.getInstance(EditEventFragment.this.mActivity).launchViewEvent(-1L, millis, millis2, 0);
                }
                Activity activity = EditEventFragment.this.getActivity();
                if (activity != null) {
                    activity.finish();
                }
            }
            View currentFocus = EditEventFragment.this.mActivity.getCurrentFocus();
            if (currentFocus != null) {
                EditEventFragment.this.mInputMethodManager.hideSoftInputFromWindow(currentFocus.getWindowToken(), 0);
                currentFocus.clearFocus();
            }
        }
    }

    boolean isEmptyNewEvent() {
        if (this.mOriginalModel != null) {
            return false;
        }
        return isEmpty();
    }

    private boolean isEmpty() {
        if (this.mModel.mTitle != null && this.mModel.mTitle.trim().length() > 0) {
            return false;
        }
        if (this.mModel.mLocation == null || this.mModel.mLocation.trim().length() <= 0) {
            return this.mModel.mDescription == null || this.mModel.mDescription.trim().length() <= 0;
        }
        return false;
    }

    @Override
    public void onDestroy() {
        Activity activity = getActivity();
        if (this.mSaveOnDetach && activity != null && !this.mIsReadOnly && !activity.isChangingConfigurations() && this.mView.prepareForSave()) {
            this.mOnDone.setDoneCode(2);
            this.mOnDone.run();
        }
        if (this.mView != null) {
            this.mView.setModel(null);
        }
        if (this.mModifyDialog != null) {
            this.mModifyDialog.dismiss();
            this.mModifyDialog = null;
        }
        super.onDestroy();
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        this.mView.prepareForSave();
        if (this.mOutstandingQueries == 0) {
            bundle.putSerializable("key_model", this.mModel);
        } else {
            bundle.putSerializable("key_model", null);
        }
        bundle.putInt("key_edit_state", this.mModification);
        if (this.mEventBundle == null && this.mEvent != null) {
            this.mEventBundle = new EventBundle();
            this.mEventBundle.id = this.mEvent.id;
            if (this.mEvent.startTime != null) {
                this.mEventBundle.start = this.mEvent.startTime.toMillis(true);
            }
            if (this.mEvent.endTime != null) {
                this.mEventBundle.end = this.mEvent.startTime.toMillis(true);
            }
        }
        bundle.putBoolean("key_edit_on_launch", this.mShowModifyDialogOnLaunch);
        bundle.putSerializable("key_event", this.mEventBundle);
        bundle.putBoolean("key_read_only", this.mIsReadOnly);
        bundle.putBoolean("show_color_palette", this.mView.isColorPaletteVisible());
        bundle.putBoolean("EditEventView_timebuttonclicked", this.mView.mTimeSelectedWasStartTime);
        bundle.putBoolean("date_button_clicked", this.mView.mDateSelectedWasStartDate);
        bundle.putBoolean("show_date_picker", this.mView.mShowDatePicker);
        bundle.putIntArray("selected_date", this.mView.mSelectedDate);
        this.mIsSaveInstanceState = true;
    }

    @Override
    public long getSupportedEventTypes() {
        return 512L;
    }

    @Override
    public void handleEvent(CalendarController.EventInfo eventInfo) {
        if (eventInfo.eventType == 32 && this.mSaveOnDetach && this.mView != null && this.mView.prepareForSave()) {
            this.mOnDone.setDoneCode(2);
            this.mOnDone.run();
        }
    }

    private static class EventBundle implements Serializable {
        private static final long serialVersionUID = 1;
        long end;
        long id;
        long start;

        private EventBundle() {
            this.id = -1L;
            this.start = -1L;
            this.end = -1L;
        }
    }

    @Override
    public void onColorSelected(int i) {
        if (!this.mModel.isEventColorInitialized() || this.mModel.getEventColor() != i) {
            this.mModel.setEventColor(i);
            this.mView.updateHeadlineColor(this.mModel, i);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        extOnResume();
    }

    private void extOnResume() {
        this.mView.doOnResume(this.mCalendarId);
    }

    @Override
    public void onPause() {
        LogUtil.d("EditEventActivity", "onPause");
        this.mView.dismissDatePickerDialog();
        super.onPause();
    }

    private void formatOriginalRrule() {
    }
}
