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
import android.provider.CalendarContract;
import android.text.TextUtils;
import android.text.format.Time;
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
    private boolean mShowColorPalette;
    public boolean mShowModifyDialogOnLaunch;
    private boolean mTimeSelectedWasStartTime;
    private Uri mUri;
    private boolean mUseCustomActionBar;
    EditEventView mView;

    private class QueryHandler extends AsyncQueryHandler {
        public QueryHandler(ContentResolver cr) {
            super(cr);
        }

        @Override
        protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
            if (cursor != null) {
                Activity activity = EditEventFragment.this.getActivity();
                if (activity != null && !activity.isFinishing()) {
                    switch (token) {
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
                            long eventId = EditEventFragment.this.mModel.mId;
                            if (!EditEventFragment.this.mModel.mHasAttendeeData || eventId == -1) {
                                EditEventFragment.this.setModelIfDone(2);
                            } else {
                                Uri attUri = CalendarContract.Attendees.CONTENT_URI;
                                String[] whereArgs = {Long.toString(eventId)};
                                EditEventFragment.this.mHandler.startQuery(2, null, attUri, EditEventHelper.ATTENDEES_PROJECTION, "event_id=? AND attendeeEmail IS NOT NULL", whereArgs, null);
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
                                Uri rUri = CalendarContract.Reminders.CONTENT_URI;
                                String[] remArgs = {Long.toString(eventId)};
                                EditEventFragment.this.mHandler.startQuery(4, null, rUri, EditEventHelper.REMINDERS_PROJECTION, "event_id=?", remArgs, null);
                            }
                            String[] selArgs = {Long.toString(EditEventFragment.this.mModel.mCalendarId)};
                            EditEventFragment.this.mHandler.startQuery(8, null, CalendarContract.Calendars.CONTENT_URI, EditEventHelper.CALENDARS_PROJECTION, "_id=?", selArgs, null);
                            EditEventFragment.this.mHandler.startQuery(16, null, CalendarContract.Colors.CONTENT_URI, EditEventHelper.COLORS_PROJECTION, "color_type=1", null, null);
                            EditEventFragment.this.setModelIfDone(1);
                            return;
                        case 2:
                            while (cursor.moveToNext()) {
                                try {
                                    String name = cursor.getString(1);
                                    String email = cursor.getString(2);
                                    int status = cursor.getInt(4);
                                    int relationship = cursor.getInt(3);
                                    if (relationship == 2) {
                                        if (email != null) {
                                            EditEventFragment.this.mModel.mOrganizer = email;
                                            EditEventFragment.this.mModel.mIsOrganizer = EditEventFragment.this.mModel.mOwnerAccount.equalsIgnoreCase(email);
                                            EditEventFragment.this.mOriginalModel.mOrganizer = email;
                                            EditEventFragment.this.mOriginalModel.mIsOrganizer = EditEventFragment.this.mOriginalModel.mOwnerAccount.equalsIgnoreCase(email);
                                        }
                                        if (TextUtils.isEmpty(name)) {
                                            EditEventFragment.this.mModel.mOrganizerDisplayName = EditEventFragment.this.mModel.mOrganizer;
                                            EditEventFragment.this.mOriginalModel.mOrganizerDisplayName = EditEventFragment.this.mOriginalModel.mOrganizer;
                                        } else {
                                            EditEventFragment.this.mModel.mOrganizerDisplayName = name;
                                            EditEventFragment.this.mOriginalModel.mOrganizerDisplayName = name;
                                        }
                                    }
                                    if (email != null && EditEventFragment.this.mModel.mOwnerAccount != null && EditEventFragment.this.mModel.mOwnerAccount.equalsIgnoreCase(email)) {
                                        int attendeeId = cursor.getInt(0);
                                        EditEventFragment.this.mModel.mOwnerAttendeeId = attendeeId;
                                        EditEventFragment.this.mModel.mSelfAttendeeStatus = status;
                                        EditEventFragment.this.mOriginalModel.mOwnerAttendeeId = attendeeId;
                                        EditEventFragment.this.mOriginalModel.mSelfAttendeeStatus = status;
                                    } else {
                                        CalendarEventModel.Attendee attendee = new CalendarEventModel.Attendee(name, email);
                                        attendee.mStatus = status;
                                        EditEventFragment.this.mModel.addAttendee(attendee);
                                        EditEventFragment.this.mOriginalModel.addAttendee(attendee);
                                    }
                                } finally {
                                }
                            }
                            cursor.close();
                            EditEventFragment.this.setModelIfDone(2);
                            return;
                        case 4:
                            while (cursor.moveToNext()) {
                                try {
                                    int minutes = cursor.getInt(1);
                                    int method = cursor.getInt(2);
                                    CalendarEventModel.ReminderEntry re = CalendarEventModel.ReminderEntry.valueOf(minutes, method);
                                    EditEventFragment.this.mModel.mReminders.add(re);
                                    EditEventFragment.this.mOriginalModel.mReminders.add(re);
                                } finally {
                                }
                            }
                            Collections.sort(EditEventFragment.this.mModel.mReminders);
                            Collections.sort(EditEventFragment.this.mOriginalModel.mReminders);
                            cursor.close();
                            EditEventFragment.this.setModelIfDone(4);
                            return;
                        case 8:
                            try {
                                if (EditEventFragment.this.mModel.mId == -1) {
                                    MatrixCursor matrixCursor = Utils.matrixCursorFromCursor(cursor);
                                    EditEventFragment.this.mView.setCalendarsCursor(matrixCursor, EditEventFragment.this.isAdded() && EditEventFragment.this.isResumed(), EditEventFragment.this.mCalendarId);
                                } else {
                                    EditEventHelper.setModelFromCalendarCursor(EditEventFragment.this.mModel, cursor);
                                    EditEventHelper.setModelFromCalendarCursor(EditEventFragment.this.mOriginalModel, cursor);
                                }
                                cursor.close();
                                EditEventFragment.this.setModelIfDone(8);
                                return;
                            } finally {
                            }
                        case 16:
                            if (cursor.moveToFirst()) {
                                EventColorCache cache = new EventColorCache();
                                do {
                                    int colorKey = cursor.getInt(4);
                                    int rawColor = cursor.getInt(3);
                                    int displayColor = Utils.getDisplayColorFromColor(rawColor);
                                    String accountName = cursor.getString(1);
                                    String accountType = cursor.getString(2);
                                    cache.insertColor(accountName, accountType, displayColor, colorKey);
                                } while (cursor.moveToNext());
                                cache.sortPalettes(new HsvColorComparator());
                                EditEventFragment.this.mModel.mEventColorCache = cache;
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
                            return;
                        default:
                            return;
                    }
                }
            }
        }
    }

    private void setModelIfDone(int queryType) {
        synchronized (this) {
            this.mOutstandingQueries &= queryType ^ (-1);
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
                this.mView.setModification(this.mModification);
            }
        }
    }

    public EditEventFragment() {
        this(null, null, false, -1, false, null);
    }

    public EditEventFragment(CalendarController.EventInfo event, ArrayList<CalendarEventModel.ReminderEntry> reminders, boolean eventColorInitialized, int eventColor, boolean readOnly, Intent intent) {
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
            public void onClick(View v) {
                EditEventFragment.this.onActionBarItemSelected(v.getId());
            }
        };
        this.mOnColorPickerClicked = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int[] colors = EditEventFragment.this.mModel.getCalendarEventColors();
                if (EditEventFragment.this.mColorPickerDialog != null) {
                    EditEventFragment.this.mColorPickerDialog.setCalendarColor(EditEventFragment.this.mModel.getCalendarColor());
                    EditEventFragment.this.mColorPickerDialog.setColors(colors, EditEventFragment.this.mModel.getEventColor());
                } else {
                    EditEventFragment.this.mColorPickerDialog = EventColorPickerDialog.newInstance(colors, EditEventFragment.this.mModel.getEventColor(), EditEventFragment.this.mModel.getCalendarColor(), EditEventFragment.this.mView.mIsMultipane);
                    EditEventFragment.this.mColorPickerDialog.setOnColorSelectedListener(EditEventFragment.this);
                }
                FragmentManager fragmentManager = EditEventFragment.this.getFragmentManager();
                fragmentManager.executePendingTransactions();
                if (!EditEventFragment.this.mColorPickerDialog.isAdded()) {
                    EditEventFragment.this.mColorPickerDialog.show(fragmentManager, "ColorPickerDialog");
                }
            }
        };
        this.mEvent = event;
        this.mIsReadOnly = readOnly;
        this.mIntent = intent;
        this.mReminders = reminders;
        this.mEventColorInitialized = eventColorInitialized;
        if (eventColorInitialized) {
            this.mEventColor = eventColor;
        }
        setHasOptionsMenu(true);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
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
        boolean newEvent = this.mUri == null;
        if (!newEvent) {
            this.mModel.mCalendarAccessLevel = 0;
            this.mOutstandingQueries = 31;
            this.mHandler.startQuery(1, null, this.mUri, EditEventHelper.EVENT_PROJECTION, null, null, null);
            return;
        }
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view;
        if (this.mIsReadOnly) {
            view = inflater.inflate(R.layout.edit_event_single_column, (ViewGroup) null);
        } else {
            view = inflater.inflate(R.layout.edit_event, (ViewGroup) null);
        }
        this.mView = new EditEventView(this.mActivity, view, this.mOnDone, this.mTimeSelectedWasStartTime, this.mDateSelectedWasStartDate);
        startQuery();
        if (this.mUseCustomActionBar) {
            View actionBarButtons = inflater.inflate(R.layout.edit_event_custom_actionbar, (ViewGroup) new LinearLayout(this.mActivity), false);
            View cancelActionView = actionBarButtons.findViewById(R.id.action_cancel);
            cancelActionView.setOnClickListener(this.mActionBarListener);
            View doneActionView = actionBarButtons.findViewById(R.id.action_done);
            doneActionView.setOnClickListener(this.mActionBarListener);
            this.mActivity.getActionBar().setCustomView(actionBarButtons);
        }
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (this.mUseCustomActionBar) {
            this.mActivity.getActionBar().setCustomView((View) null);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey("key_model")) {
                this.mRestoreModel = (CalendarEventModel) savedInstanceState.getSerializable("key_model");
            }
            if (savedInstanceState.containsKey("key_edit_state")) {
                this.mModification = savedInstanceState.getInt("key_edit_state");
            }
            if (savedInstanceState.containsKey("key_edit_on_launch")) {
                this.mShowModifyDialogOnLaunch = savedInstanceState.getBoolean("key_edit_on_launch");
            }
            if (savedInstanceState.containsKey("key_event")) {
                this.mEventBundle = (EventBundle) savedInstanceState.getSerializable("key_event");
            }
            if (savedInstanceState.containsKey("key_read_only")) {
                this.mIsReadOnly = savedInstanceState.getBoolean("key_read_only");
            }
            if (savedInstanceState.containsKey("EditEventView_timebuttonclicked")) {
                this.mTimeSelectedWasStartTime = savedInstanceState.getBoolean("EditEventView_timebuttonclicked");
            }
            if (savedInstanceState.containsKey("date_button_clicked")) {
                this.mDateSelectedWasStartDate = savedInstanceState.getBoolean("date_button_clicked");
            }
            if (savedInstanceState.containsKey("show_color_palette")) {
                this.mShowColorPalette = savedInstanceState.getBoolean("show_color_palette");
            }
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        if (!this.mUseCustomActionBar) {
            inflater.inflate(R.menu.edit_event_title_bar, menu);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return onActionBarItemSelected(item.getItemId());
    }

    private boolean onActionBarItemSelected(int itemId) {
        if (itemId == R.id.action_done) {
            if (EditEventHelper.canModifyEvent(this.mModel) || EditEventHelper.canRespond(this.mModel)) {
                if (this.mView != null && this.mView.prepareForSave()) {
                    if (this.mModification == 0) {
                        this.mModification = 3;
                    }
                    this.mOnDone.setDoneCode(3);
                    this.mOnDone.run();
                } else {
                    this.mOnDone.setDoneCode(1);
                    this.mOnDone.run();
                }
            } else if (EditEventHelper.canAddReminders(this.mModel) && this.mModel.mId != -1 && this.mOriginalModel != null && this.mView.prepareForSave()) {
                saveReminders();
                this.mOnDone.setDoneCode(1);
                this.mOnDone.run();
            } else {
                this.mOnDone.setDoneCode(1);
                this.mOnDone.run();
            }
        } else if (itemId == R.id.action_cancel) {
            this.mOnDone.setDoneCode(1);
            this.mOnDone.run();
        }
        return true;
    }

    private void saveReminders() {
        ArrayList<ContentProviderOperation> ops = new ArrayList<>(3);
        boolean changed = EditEventHelper.saveReminders(ops, this.mModel.mId, this.mModel.mReminders, this.mOriginalModel.mReminders, false);
        if (changed) {
            AsyncQueryService service = new AsyncQueryService(getActivity());
            service.startBatch(0, null, CalendarContract.Calendars.CONTENT_URI.getAuthority(), ops, 0L);
            Uri uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, this.mModel.mId);
            int len = this.mModel.mReminders.size();
            boolean hasAlarm = len > 0;
            if (hasAlarm != this.mOriginalModel.mHasAlarm) {
                ContentValues values = new ContentValues();
                values.put("hasAlarm", Integer.valueOf(hasAlarm ? 1 : 0));
                service.startUpdate(0, null, uri, values, null, null, 0L);
            }
            Toast.makeText(this.mActivity, R.string.saving_event, 0).show();
        }
    }

    protected void displayEditWhichDialog() {
        CharSequence[] items;
        if (this.mModification == 0) {
            final boolean notSynced = TextUtils.isEmpty(this.mModel.mSyncId);
            boolean isFirstEventInSeries = this.mModel.mIsFirstEventInSeries;
            int itemIndex = 0;
            if (notSynced) {
                if (isFirstEventInSeries) {
                    items = new CharSequence[1];
                } else {
                    items = new CharSequence[2];
                }
            } else {
                if (isFirstEventInSeries) {
                    items = new CharSequence[2];
                } else {
                    items = new CharSequence[3];
                }
                items[0] = this.mActivity.getText(R.string.modify_event);
                itemIndex = 0 + 1;
            }
            int itemIndex2 = itemIndex + 1;
            items[itemIndex] = this.mActivity.getText(R.string.modify_all);
            if (!isFirstEventInSeries) {
                int i = itemIndex2 + 1;
                items[itemIndex2] = this.mActivity.getText(R.string.modify_all_following);
            }
            if (this.mModifyDialog != null) {
                this.mModifyDialog.dismiss();
                this.mModifyDialog = null;
            }
            this.mModifyDialog = new AlertDialog.Builder(this.mActivity).setTitle(R.string.edit_event_label).setItems(items, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (which == 0) {
                        EditEventFragment.this.mModification = notSynced ? 3 : 1;
                        if (EditEventFragment.this.mModification == 1) {
                            EditEventFragment.this.mModel.mOriginalSyncId = notSynced ? null : EditEventFragment.this.mModel.mSyncId;
                            EditEventFragment.this.mModel.mOriginalId = EditEventFragment.this.mModel.mId;
                        }
                    } else if (which == 1) {
                        EditEventFragment.this.mModification = notSynced ? 2 : 3;
                    } else if (which == 2) {
                        EditEventFragment.this.mModification = 2;
                    }
                    EditEventFragment.this.mView.setModification(EditEventFragment.this.mModification);
                }
            }).show();
            this.mModifyDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    Activity a = EditEventFragment.this.getActivity();
                    if (a != null) {
                        a.finish();
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
        public void setDoneCode(int code) {
            this.mCode = code;
        }

        @Override
        public void run() {
            int stringResource;
            EditEventFragment.this.mSaveOnDetach = false;
            if (EditEventFragment.this.mModification == 0) {
                EditEventFragment.this.mModification = 3;
            }
            if ((this.mCode & 2) != 0 && EditEventFragment.this.mModel != null && ((EditEventHelper.canRespond(EditEventFragment.this.mModel) || EditEventHelper.canModifyEvent(EditEventFragment.this.mModel)) && EditEventFragment.this.mView.prepareForSave() && !EditEventFragment.this.isEmptyNewEvent() && EditEventFragment.this.mModel.normalizeReminders() && EditEventFragment.this.mHelper.saveEvent(EditEventFragment.this.mModel, EditEventFragment.this.mOriginalModel, EditEventFragment.this.mModification))) {
                if (!EditEventFragment.this.mModel.mAttendeesList.isEmpty()) {
                    if (EditEventFragment.this.mModel.mUri != null) {
                        stringResource = R.string.saving_event_with_guest;
                    } else {
                        stringResource = R.string.creating_event_with_guest;
                    }
                } else if (EditEventFragment.this.mModel.mUri != null) {
                    stringResource = R.string.saving_event;
                } else {
                    stringResource = R.string.creating_event;
                }
                Toast.makeText(EditEventFragment.this.mActivity, stringResource, 0).show();
            } else if ((this.mCode & 2) != 0 && EditEventFragment.this.mModel != null && EditEventFragment.this.isEmptyNewEvent()) {
                Toast.makeText(EditEventFragment.this.mActivity, R.string.empty_event, 0).show();
            }
            if ((this.mCode & 4) != 0 && EditEventFragment.this.mOriginalModel != null && EditEventHelper.canModifyCalendar(EditEventFragment.this.mOriginalModel)) {
                long begin = EditEventFragment.this.mModel.mStart;
                long end = EditEventFragment.this.mModel.mEnd;
                int which = -1;
                switch (EditEventFragment.this.mModification) {
                    case 1:
                        which = 0;
                        break;
                    case 2:
                        which = 1;
                        break;
                    case 3:
                        which = 2;
                        break;
                }
                DeleteEventHelper deleteHelper = new DeleteEventHelper(EditEventFragment.this.mActivity, EditEventFragment.this.mActivity, !EditEventFragment.this.mIsReadOnly);
                deleteHelper.delete(begin, end, EditEventFragment.this.mOriginalModel, which);
            }
            if ((this.mCode & 1) != 0) {
                if ((this.mCode & 2) != 0 && EditEventFragment.this.mActivity != null) {
                    long start = EditEventFragment.this.mModel.mStart;
                    long end2 = EditEventFragment.this.mModel.mEnd;
                    if (EditEventFragment.this.mModel.mAllDay) {
                        String tz = Utils.getTimeZone(EditEventFragment.this.mActivity, null);
                        Time t = new Time("UTC");
                        t.set(start);
                        t.timezone = tz;
                        start = t.toMillis(true);
                        t.timezone = "UTC";
                        t.set(end2);
                        t.timezone = tz;
                        end2 = t.toMillis(true);
                    }
                    CalendarController.getInstance(EditEventFragment.this.mActivity).launchViewEvent(-1L, start, end2, 0);
                }
                Activity a = EditEventFragment.this.getActivity();
                if (a != null) {
                    a.finish();
                }
            }
            View focusedView = EditEventFragment.this.mActivity.getCurrentFocus();
            if (focusedView != null) {
                EditEventFragment.this.mInputMethodManager.hideSoftInputFromWindow(focusedView.getWindowToken(), 0);
                focusedView.clearFocus();
            }
        }
    }

    boolean isEmptyNewEvent() {
        if (this.mOriginalModel == null && this.mModel.mOriginalStart == this.mModel.mStart && this.mModel.mOriginalEnd == this.mModel.mEnd && this.mModel.mAttendeesList.isEmpty()) {
            return this.mModel.isEmpty();
        }
        return false;
    }

    @Override
    public void onPause() {
        Activity act = getActivity();
        if (this.mSaveOnDetach && act != null && !this.mIsReadOnly && !act.isChangingConfigurations() && this.mView.prepareForSave()) {
            this.mOnDone.setDoneCode(2);
            this.mOnDone.run();
        }
        super.onPause();
    }

    @Override
    public void onDestroy() {
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
    public void onSaveInstanceState(Bundle outState) {
        this.mView.prepareForSave();
        outState.putSerializable("key_model", this.mModel);
        outState.putInt("key_edit_state", this.mModification);
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
        outState.putBoolean("key_edit_on_launch", this.mShowModifyDialogOnLaunch);
        outState.putSerializable("key_event", this.mEventBundle);
        outState.putBoolean("key_read_only", this.mIsReadOnly);
        outState.putBoolean("show_color_palette", this.mView.isColorPaletteVisible());
        outState.putBoolean("EditEventView_timebuttonclicked", this.mView.mTimeSelectedWasStartTime);
        outState.putBoolean("date_button_clicked", this.mView.mDateSelectedWasStartDate);
    }

    @Override
    public long getSupportedEventTypes() {
        return 512L;
    }

    @Override
    public void handleEvent(CalendarController.EventInfo event) {
        if (event.eventType == 32 && this.mSaveOnDetach && this.mView != null && this.mView.prepareForSave()) {
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
    public void onColorSelected(int color) {
        if (!this.mModel.isEventColorInitialized() || this.mModel.getEventColor() != color) {
            this.mModel.setEventColor(color);
            this.mView.updateHeadlineColor(this.mModel, color);
        }
    }
}
