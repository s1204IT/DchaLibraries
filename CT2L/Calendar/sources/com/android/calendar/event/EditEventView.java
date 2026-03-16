package com.android.calendar.event;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.FragmentManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.os.BenesseExtension;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.text.util.Rfc822Tokenizer;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.MultiAutoCompleteTextView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ResourceCursorAdapter;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import com.android.calendar.CalendarEventModel;
import com.android.calendar.EmailAddressAdapter;
import com.android.calendar.EventInfoFragment;
import com.android.calendar.EventRecurrenceFormatter;
import com.android.calendar.GeneralPreferences;
import com.android.calendar.R;
import com.android.calendar.RecipientAdapter;
import com.android.calendar.Utils;
import com.android.calendar.event.EditEventHelper;
import com.android.calendar.recurrencepicker.RecurrencePickerDialog;
import com.android.calendarcommon2.EventRecurrence;
import com.android.common.Rfc822InputFilter;
import com.android.common.Rfc822Validator;
import com.android.datetimepicker.date.DatePickerDialog;
import com.android.datetimepicker.time.RadialPickerLayout;
import com.android.datetimepicker.time.TimePickerDialog;
import com.android.ex.chips.AccountSpecifier;
import com.android.ex.chips.BaseRecipientAdapter;
import com.android.ex.chips.ChipsUtil;
import com.android.ex.chips.RecipientEditTextView;
import com.android.timezonepicker.TimeZoneInfo;
import com.android.timezonepicker.TimeZonePickerDialog;
import com.android.timezonepicker.TimeZonePickerUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.TimeZone;

public class EditEventView implements DialogInterface.OnCancelListener, DialogInterface.OnClickListener, View.OnClickListener, AdapterView.OnItemSelectedListener, RecurrencePickerDialog.OnRecurrenceSetListener, TimeZonePickerDialog.OnTimeZoneSetListener {
    Spinner mAccessLevelSpinner;
    private Activity mActivity;
    private AccountSpecifier mAddressAdapter;
    private boolean mAllDayChangingAvailability;
    CheckBox mAllDayCheckBox;
    View mAttendeesGroup;
    MultiAutoCompleteTextView mAttendeesList;
    private ArrayAdapter<String> mAvailabilityAdapter;
    private int mAvailabilityCurrentlySelected;
    private boolean mAvailabilityExplicitlySet;
    private ArrayList<String> mAvailabilityLabels;
    Spinner mAvailabilitySpinner;
    private ArrayList<Integer> mAvailabilityValues;
    View mCalendarSelectorGroup;
    View mCalendarSelectorWrapper;
    View mCalendarStaticGroup;
    private Cursor mCalendarsCursor;
    Spinner mCalendarsSpinner;
    View mColorPickerExistingEvent;
    View mColorPickerNewEvent;
    private DatePickerDialog mDatePickerDialog;
    public boolean mDateSelectedWasStartDate;
    private int mDefaultReminderMinutes;
    View mDescriptionGroup;
    TextView mDescriptionTextView;
    private EditEventHelper.EditDoneRunnable mDone;
    private Rfc822Validator mEmailValidator;
    Button mEndDateButton;
    TextView mEndDateHome;
    View mEndHomeGroup;
    private Time mEndTime;
    Button mEndTimeButton;
    TextView mEndTimeHome;
    private TimePickerDialog mEndTimePickerDialog;
    public boolean mIsMultipane;
    private ProgressDialog mLoadingCalendarsDialog;
    TextView mLoadingMessage;
    EventLocationAdapter mLocationAdapter;
    View mLocationGroup;
    AutoCompleteTextView mLocationTextView;
    private CalendarEventModel mModel;
    private AlertDialog mNoCalendarsDialog;
    View mOrganizerGroup;
    private ArrayList<String> mOriginalAvailabilityLabels;
    private ArrayList<String> mReminderMethodLabels;
    private ArrayList<Integer> mReminderMethodValues;
    private ArrayList<String> mReminderMinuteLabels;
    private ArrayList<Integer> mReminderMinuteValues;
    LinearLayout mRemindersContainer;
    View mRemindersGroup;
    View mResponseGroup;
    RadioGroup mResponseRadioGroup;
    private String mRrule;
    Button mRruleButton;
    ScrollView mScrollView;
    Button mStartDateButton;
    TextView mStartDateHome;
    View mStartHomeGroup;
    private Time mStartTime;
    Button mStartTimeButton;
    TextView mStartTimeHome;
    private TimePickerDialog mStartTimePickerDialog;
    public boolean mTimeSelectedWasStartTime;
    private String mTimezone;
    Button mTimezoneButton;
    TextView mTimezoneLabel;
    View mTimezoneRow;
    TextView mTimezoneTextView;
    TextView mTitleTextView;
    private TimeZonePickerUtils mTzPickerUtils;
    private View mView;
    TextView mWhenView;
    private static StringBuilder mSB = new StringBuilder(50);
    private static Formatter mF = new Formatter(mSB, Locale.getDefault());
    private static InputFilter[] sRecipientFilters = {new Rfc822InputFilter()};
    ArrayList<View> mEditOnlyList = new ArrayList<>();
    ArrayList<View> mEditViewList = new ArrayList<>();
    ArrayList<View> mViewOnlyList = new ArrayList<>();
    private int[] mOriginalPadding = new int[4];
    private boolean mSaveAfterQueryComplete = false;
    private boolean mAllDay = false;
    private int mModification = 0;
    private EventRecurrence mEventRecurrence = new EventRecurrence();
    private ArrayList<LinearLayout> mReminderItems = new ArrayList<>(0);
    private ArrayList<CalendarEventModel.ReminderEntry> mUnsupportedReminders = new ArrayList<>();

    private class TimeListener implements TimePickerDialog.OnTimeSetListener {
        private View mView;

        public TimeListener(View view) {
            this.mView = view;
        }

        @Override
        public void onTimeSet(RadialPickerLayout view, int hourOfDay, int minute) {
            long startMillis;
            Time startTime = EditEventView.this.mStartTime;
            Time endTime = EditEventView.this.mEndTime;
            if (this.mView == EditEventView.this.mStartTimeButton) {
                int hourDuration = endTime.hour - startTime.hour;
                int minuteDuration = endTime.minute - startTime.minute;
                startTime.hour = hourOfDay;
                startTime.minute = minute;
                startMillis = startTime.normalize(true);
                endTime.hour = hourOfDay + hourDuration;
                endTime.minute = minute + minuteDuration;
                EditEventView.this.populateTimezone(startMillis);
            } else {
                startMillis = startTime.toMillis(true);
                endTime.hour = hourOfDay;
                endTime.minute = minute;
                if (endTime.before(startTime)) {
                    endTime.monthDay = startTime.monthDay + 1;
                }
            }
            long endMillis = endTime.normalize(true);
            EditEventView.this.setDate(EditEventView.this.mEndDateButton, endMillis);
            EditEventView.this.setTime(EditEventView.this.mStartTimeButton, startMillis);
            EditEventView.this.setTime(EditEventView.this.mEndTimeButton, endMillis);
            EditEventView.this.updateHomeTime();
        }
    }

    private class TimeClickListener implements View.OnClickListener {
        private Time mTime;

        public TimeClickListener(Time time) {
            this.mTime = time;
        }

        @Override
        public void onClick(View v) {
            TimePickerDialog dialog;
            if (v == EditEventView.this.mStartTimeButton) {
                EditEventView.this.mTimeSelectedWasStartTime = true;
                if (EditEventView.this.mStartTimePickerDialog != null) {
                    EditEventView.this.mStartTimePickerDialog.setStartTime(this.mTime.hour, this.mTime.minute);
                } else {
                    EditEventView.this.mStartTimePickerDialog = TimePickerDialog.newInstance(EditEventView.this.new TimeListener(v), this.mTime.hour, this.mTime.minute, DateFormat.is24HourFormat(EditEventView.this.mActivity));
                }
                dialog = EditEventView.this.mStartTimePickerDialog;
            } else {
                EditEventView.this.mTimeSelectedWasStartTime = false;
                if (EditEventView.this.mEndTimePickerDialog != null) {
                    EditEventView.this.mEndTimePickerDialog.setStartTime(this.mTime.hour, this.mTime.minute);
                } else {
                    EditEventView.this.mEndTimePickerDialog = TimePickerDialog.newInstance(EditEventView.this.new TimeListener(v), this.mTime.hour, this.mTime.minute, DateFormat.is24HourFormat(EditEventView.this.mActivity));
                }
                dialog = EditEventView.this.mEndTimePickerDialog;
            }
            FragmentManager fm = EditEventView.this.mActivity.getFragmentManager();
            fm.executePendingTransactions();
            if (dialog != null && !dialog.isAdded()) {
                dialog.show(fm, "timePickerDialogFragment");
            }
        }
    }

    private class DateListener implements DatePickerDialog.OnDateSetListener {
        View mView;

        public DateListener(View view) {
            this.mView = view;
        }

        @Override
        public void onDateSet(DatePickerDialog view, int year, int month, int monthDay) {
            long startMillis;
            long endMillis;
            Log.d("EditEvent", "onDateSet: " + year + " " + month + " " + monthDay);
            Time startTime = EditEventView.this.mStartTime;
            Time endTime = EditEventView.this.mEndTime;
            if (this.mView == EditEventView.this.mStartDateButton) {
                int yearDuration = endTime.year - startTime.year;
                int monthDuration = endTime.month - startTime.month;
                int monthDayDuration = endTime.monthDay - startTime.monthDay;
                startTime.year = year;
                startTime.month = month;
                startTime.monthDay = monthDay;
                startMillis = startTime.normalize(true);
                endTime.year = year + yearDuration;
                endTime.month = month + monthDuration;
                endTime.monthDay = monthDay + monthDayDuration;
                endMillis = endTime.normalize(true);
                EditEventView.this.populateRepeats();
                EditEventView.this.populateTimezone(startMillis);
            } else {
                startMillis = startTime.toMillis(true);
                endTime.year = year;
                endTime.month = month;
                endTime.monthDay = monthDay;
                endMillis = endTime.normalize(true);
                if (endTime.before(startTime)) {
                    endTime.set(startTime);
                    endMillis = startMillis;
                }
            }
            EditEventView.this.setDate(EditEventView.this.mStartDateButton, startMillis);
            EditEventView.this.setDate(EditEventView.this.mEndDateButton, endMillis);
            EditEventView.this.setTime(EditEventView.this.mEndTimeButton, endMillis);
            EditEventView.this.updateHomeTime();
        }
    }

    private void populateWhen() {
        long startMillis = this.mStartTime.toMillis(false);
        long endMillis = this.mEndTime.toMillis(false);
        setDate(this.mStartDateButton, startMillis);
        setDate(this.mEndDateButton, endMillis);
        setTime(this.mStartTimeButton, startMillis);
        setTime(this.mEndTimeButton, endMillis);
        this.mStartDateButton.setOnClickListener(new DateClickListener(this.mStartTime));
        this.mEndDateButton.setOnClickListener(new DateClickListener(this.mEndTime));
        this.mStartTimeButton.setOnClickListener(new TimeClickListener(this.mStartTime));
        this.mEndTimeButton.setOnClickListener(new TimeClickListener(this.mEndTime));
    }

    @Override
    public void onTimeZoneSet(TimeZoneInfo tzi) {
        setTimezone(tzi.mTzId);
        updateHomeTime();
    }

    private void setTimezone(String timeZone) {
        this.mTimezone = timeZone;
        this.mStartTime.timezone = this.mTimezone;
        long timeMillis = this.mStartTime.normalize(true);
        this.mEndTime.timezone = this.mTimezone;
        this.mEndTime.normalize(true);
        populateTimezone(timeMillis);
    }

    private void populateTimezone(long eventStartTime) {
        if (this.mTzPickerUtils == null) {
            this.mTzPickerUtils = new TimeZonePickerUtils(this.mActivity);
        }
        CharSequence displayName = this.mTzPickerUtils.getGmtDisplayName(this.mActivity, this.mTimezone, eventStartTime, true);
        this.mTimezoneTextView.setText(displayName);
        this.mTimezoneButton.setText(displayName);
    }

    private void showTimezoneDialog() {
        Bundle b = new Bundle();
        b.putLong("bundle_event_start_time", this.mStartTime.toMillis(false));
        b.putString("bundle_event_time_zone", this.mTimezone);
        FragmentManager fm = this.mActivity.getFragmentManager();
        TimeZonePickerDialog tzpd = (TimeZonePickerDialog) fm.findFragmentByTag("timeZonePickerDialogFragment");
        if (tzpd != null) {
            tzpd.dismiss();
        }
        TimeZonePickerDialog tzpd2 = new TimeZonePickerDialog();
        tzpd2.setArguments(b);
        tzpd2.setOnTimeZoneSetListener(this);
        tzpd2.show(fm, "timeZonePickerDialogFragment");
    }

    private void populateRepeats() {
        String repeatString;
        boolean enabled;
        Resources r = this.mActivity.getResources();
        if (!TextUtils.isEmpty(this.mRrule)) {
            repeatString = EventRecurrenceFormatter.getRepeatString(this.mActivity, r, this.mEventRecurrence, true);
            if (repeatString == null) {
                repeatString = r.getString(R.string.custom);
                Log.e("EditEvent", "Can't generate display string for " + this.mRrule);
                enabled = false;
            } else {
                enabled = RecurrencePickerDialog.canHandleRecurrenceRule(this.mEventRecurrence);
                if (!enabled) {
                    Log.e("EditEvent", "UI can't handle " + this.mRrule);
                }
            }
        } else {
            repeatString = r.getString(R.string.does_not_repeat);
            enabled = true;
        }
        this.mRruleButton.setText(repeatString);
        if (this.mModel.mOriginalSyncId != null) {
            enabled = false;
        }
        this.mRruleButton.setOnClickListener(this);
        this.mRruleButton.setEnabled(enabled);
    }

    private class DateClickListener implements View.OnClickListener {
        private Time mTime;

        public DateClickListener(Time time) {
            this.mTime = time;
        }

        @Override
        public void onClick(View v) {
            if (EditEventView.this.mView.hasWindowFocus()) {
                if (v == EditEventView.this.mStartDateButton) {
                    EditEventView.this.mDateSelectedWasStartDate = true;
                } else {
                    EditEventView.this.mDateSelectedWasStartDate = false;
                }
                DateListener listener = EditEventView.this.new DateListener(v);
                if (EditEventView.this.mDatePickerDialog != null) {
                    EditEventView.this.mDatePickerDialog.dismiss();
                }
                EditEventView.this.mDatePickerDialog = DatePickerDialog.newInstance(listener, this.mTime.year, this.mTime.month, this.mTime.monthDay);
                EditEventView.this.mDatePickerDialog.setFirstDayOfWeek(Utils.getFirstDayOfWeekAsCalendar(EditEventView.this.mActivity));
                EditEventView.this.mDatePickerDialog.setYearRange(1970, 2036);
                EditEventView.this.mDatePickerDialog.show(EditEventView.this.mActivity.getFragmentManager(), "datePickerDialogFragment");
            }
        }
    }

    public static class CalendarsAdapter extends ResourceCursorAdapter {
        public CalendarsAdapter(Context context, int resourceId, Cursor c) {
            super(context, resourceId, c);
            setDropDownViewResource(R.layout.calendars_dropdown_item);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            View colorBar = view.findViewById(R.id.color);
            int colorColumn = cursor.getColumnIndexOrThrow("calendar_color");
            int nameColumn = cursor.getColumnIndexOrThrow("calendar_displayName");
            int ownerColumn = cursor.getColumnIndexOrThrow("ownerAccount");
            if (colorBar != null) {
                colorBar.setBackgroundColor(Utils.getDisplayColorFromColor(cursor.getInt(colorColumn)));
            }
            TextView name = (TextView) view.findViewById(R.id.calendar_name);
            if (name != null) {
                String displayName = cursor.getString(nameColumn);
                name.setText(displayName);
                TextView accountName = (TextView) view.findViewById(R.id.account_name);
                if (accountName != null) {
                    accountName.setText(cursor.getString(ownerColumn));
                    accountName.setVisibility(0);
                }
            }
        }
    }

    public boolean prepareForSave() {
        if (this.mModel == null || (this.mCalendarsCursor == null && this.mModel.mUri == null)) {
            return false;
        }
        return fillModelFromUI();
    }

    @Override
    public void onClick(View view) {
        if (view == this.mRruleButton) {
            Bundle b = new Bundle();
            b.putLong("bundle_event_start_time", this.mStartTime.toMillis(false));
            b.putString("bundle_event_time_zone", this.mStartTime.timezone);
            b.putString("bundle_event_rrule", this.mRrule);
            FragmentManager fm = this.mActivity.getFragmentManager();
            RecurrencePickerDialog rpd = (RecurrencePickerDialog) fm.findFragmentByTag("recurrencePickerDialogFragment");
            if (rpd != null) {
                rpd.dismiss();
            }
            RecurrencePickerDialog rpd2 = new RecurrencePickerDialog();
            rpd2.setArguments(b);
            rpd2.setOnRecurrenceSetListener(this);
            rpd2.show(fm, "recurrencePickerDialogFragment");
            return;
        }
        LinearLayout reminderItem = (LinearLayout) view.getParent();
        LinearLayout parent = (LinearLayout) reminderItem.getParent();
        parent.removeView(reminderItem);
        this.mReminderItems.remove(reminderItem);
        updateRemindersVisibility(this.mReminderItems.size());
        EventViewUtils.updateAddReminderButton(this.mView, this.mReminderItems, this.mModel.mCalendarMaxReminders);
    }

    @Override
    public void onRecurrenceSet(String rrule) {
        Log.d("EditEvent", "Old rrule:" + this.mRrule);
        Log.d("EditEvent", "New rrule:" + rrule);
        this.mRrule = rrule;
        if (this.mRrule != null) {
            this.mEventRecurrence.parse(this.mRrule);
        }
        populateRepeats();
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        if (dialog == this.mLoadingCalendarsDialog) {
            this.mLoadingCalendarsDialog = null;
            this.mSaveAfterQueryComplete = false;
        } else if (dialog == this.mNoCalendarsDialog) {
            this.mDone.setDoneCode(1);
            this.mDone.run();
        }
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (dialog == this.mNoCalendarsDialog) {
            this.mDone.setDoneCode(1);
            this.mDone.run();
            if (which == -1 && BenesseExtension.getDchaState() == 0) {
                Intent nextIntent = new Intent("android.settings.ADD_ACCOUNT_SETTINGS");
                String[] array = {"com.android.calendar"};
                nextIntent.putExtra("authorities", array);
                nextIntent.addFlags(335544320);
                this.mActivity.startActivity(nextIntent);
            }
        }
    }

    private boolean fillModelFromUI() {
        if (this.mModel == null) {
            return false;
        }
        this.mModel.mReminders = EventViewUtils.reminderItemsToReminders(this.mReminderItems, this.mReminderMinuteValues, this.mReminderMethodValues);
        this.mModel.mReminders.addAll(this.mUnsupportedReminders);
        this.mModel.normalizeReminders();
        this.mModel.mHasAlarm = this.mReminderItems.size() > 0;
        this.mModel.mTitle = this.mTitleTextView.getText().toString();
        this.mModel.mAllDay = this.mAllDayCheckBox.isChecked();
        this.mModel.mLocation = this.mLocationTextView.getText().toString();
        this.mModel.mDescription = this.mDescriptionTextView.getText().toString();
        if (TextUtils.isEmpty(this.mModel.mLocation)) {
            this.mModel.mLocation = null;
        }
        if (TextUtils.isEmpty(this.mModel.mDescription)) {
            this.mModel.mDescription = null;
        }
        int status = EventInfoFragment.getResponseFromButtonId(this.mResponseRadioGroup.getCheckedRadioButtonId());
        if (status != 0) {
            this.mModel.mSelfAttendeeStatus = status;
        }
        if (this.mAttendeesList != null) {
            this.mEmailValidator.setRemoveInvalid(true);
            this.mAttendeesList.performValidation();
            this.mModel.mAttendeesList.clear();
            this.mModel.addAttendees(this.mAttendeesList.getText().toString(), this.mEmailValidator);
            this.mEmailValidator.setRemoveInvalid(false);
        }
        if (this.mModel.mUri == null) {
            this.mModel.mCalendarId = this.mCalendarsSpinner.getSelectedItemId();
            int calendarCursorPosition = this.mCalendarsSpinner.getSelectedItemPosition();
            if (this.mCalendarsCursor.moveToPosition(calendarCursorPosition)) {
                String defaultCalendar = this.mCalendarsCursor.getString(2);
                Utils.setSharedPreference(this.mActivity, "preference_defaultCalendar", defaultCalendar);
                this.mModel.mOwnerAccount = defaultCalendar;
                this.mModel.mOrganizer = defaultCalendar;
                this.mModel.mCalendarId = this.mCalendarsCursor.getLong(0);
            }
        }
        if (this.mModel.mAllDay) {
            this.mTimezone = "UTC";
            this.mStartTime.hour = 0;
            this.mStartTime.minute = 0;
            this.mStartTime.second = 0;
            this.mStartTime.timezone = this.mTimezone;
            this.mModel.mStart = this.mStartTime.normalize(true);
            this.mEndTime.hour = 0;
            this.mEndTime.minute = 0;
            this.mEndTime.second = 0;
            this.mEndTime.timezone = this.mTimezone;
            long normalizedEndTimeMillis = this.mEndTime.normalize(true) + 86400000;
            if (normalizedEndTimeMillis < this.mModel.mStart) {
                this.mModel.mEnd = this.mModel.mStart + 86400000;
            } else {
                this.mModel.mEnd = normalizedEndTimeMillis;
            }
        } else {
            this.mStartTime.timezone = this.mTimezone;
            this.mEndTime.timezone = this.mTimezone;
            this.mModel.mStart = this.mStartTime.toMillis(true);
            this.mModel.mEnd = this.mEndTime.toMillis(true);
        }
        this.mModel.mTimezone = this.mTimezone;
        this.mModel.mAccessLevel = this.mAccessLevelSpinner.getSelectedItemPosition();
        this.mModel.mAvailability = this.mAvailabilityValues.get(this.mAvailabilitySpinner.getSelectedItemPosition()).intValue();
        if (this.mModification == 1) {
            this.mModel.mRrule = null;
        } else {
            this.mModel.mRrule = this.mRrule;
        }
        return true;
    }

    public EditEventView(Activity activity, View view, EditEventHelper.EditDoneRunnable done, boolean timeSelectedWasStartTime, boolean dateSelectedWasStartDate) {
        View v;
        View v2;
        this.mActivity = activity;
        this.mView = view;
        this.mDone = done;
        this.mLoadingMessage = (TextView) view.findViewById(R.id.loading_message);
        this.mScrollView = (ScrollView) view.findViewById(R.id.scroll_view);
        this.mCalendarsSpinner = (Spinner) view.findViewById(R.id.calendars_spinner);
        this.mTitleTextView = (TextView) view.findViewById(R.id.title);
        this.mLocationTextView = (AutoCompleteTextView) view.findViewById(R.id.location);
        this.mDescriptionTextView = (TextView) view.findViewById(R.id.description);
        this.mTimezoneLabel = (TextView) view.findViewById(R.id.timezone_label);
        this.mStartDateButton = (Button) view.findViewById(R.id.start_date);
        this.mEndDateButton = (Button) view.findViewById(R.id.end_date);
        this.mWhenView = (TextView) this.mView.findViewById(R.id.when);
        this.mTimezoneTextView = (TextView) this.mView.findViewById(R.id.timezone_textView);
        this.mStartTimeButton = (Button) view.findViewById(R.id.start_time);
        this.mEndTimeButton = (Button) view.findViewById(R.id.end_time);
        this.mTimezoneButton = (Button) view.findViewById(R.id.timezone_button);
        this.mTimezoneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v3) {
                EditEventView.this.showTimezoneDialog();
            }
        });
        this.mTimezoneRow = view.findViewById(R.id.timezone_button_row);
        this.mStartTimeHome = (TextView) view.findViewById(R.id.start_time_home_tz);
        this.mStartDateHome = (TextView) view.findViewById(R.id.start_date_home_tz);
        this.mEndTimeHome = (TextView) view.findViewById(R.id.end_time_home_tz);
        this.mEndDateHome = (TextView) view.findViewById(R.id.end_date_home_tz);
        this.mAllDayCheckBox = (CheckBox) view.findViewById(R.id.is_all_day);
        this.mRruleButton = (Button) view.findViewById(R.id.rrule);
        this.mAvailabilitySpinner = (Spinner) view.findViewById(R.id.availability);
        this.mAccessLevelSpinner = (Spinner) view.findViewById(R.id.visibility);
        this.mCalendarSelectorGroup = view.findViewById(R.id.calendar_selector_group);
        this.mCalendarSelectorWrapper = view.findViewById(R.id.calendar_selector_wrapper);
        this.mCalendarStaticGroup = view.findViewById(R.id.calendar_group);
        this.mRemindersGroup = view.findViewById(R.id.reminders_row);
        this.mResponseGroup = view.findViewById(R.id.response_row);
        this.mOrganizerGroup = view.findViewById(R.id.organizer_row);
        this.mAttendeesGroup = view.findViewById(R.id.add_attendees_row);
        this.mLocationGroup = view.findViewById(R.id.where_row);
        this.mDescriptionGroup = view.findViewById(R.id.description_row);
        this.mStartHomeGroup = view.findViewById(R.id.from_row_home_tz);
        this.mEndHomeGroup = view.findViewById(R.id.to_row_home_tz);
        this.mAttendeesList = (MultiAutoCompleteTextView) view.findViewById(R.id.attendees);
        this.mColorPickerNewEvent = view.findViewById(R.id.change_color_new_event);
        this.mColorPickerExistingEvent = view.findViewById(R.id.change_color_existing_event);
        this.mTitleTextView.setTag(this.mTitleTextView.getBackground());
        this.mLocationTextView.setTag(this.mLocationTextView.getBackground());
        this.mLocationAdapter = new EventLocationAdapter(activity);
        this.mLocationTextView.setAdapter(this.mLocationAdapter);
        this.mLocationTextView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v3, int actionId, KeyEvent event) {
                if (actionId == 6) {
                    EditEventView.this.mLocationTextView.dismissDropDown();
                    return false;
                }
                return false;
            }
        });
        this.mAvailabilityExplicitlySet = false;
        this.mAllDayChangingAvailability = false;
        this.mAvailabilityCurrentlySelected = -1;
        this.mAvailabilitySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view2, int position, long id) {
                if (EditEventView.this.mAvailabilityCurrentlySelected == -1) {
                    EditEventView.this.mAvailabilityCurrentlySelected = position;
                }
                if (EditEventView.this.mAvailabilityCurrentlySelected == position || EditEventView.this.mAllDayChangingAvailability) {
                    EditEventView.this.mAvailabilityCurrentlySelected = position;
                    EditEventView.this.mAllDayChangingAvailability = false;
                } else {
                    EditEventView.this.mAvailabilityExplicitlySet = true;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });
        this.mDescriptionTextView.setTag(this.mDescriptionTextView.getBackground());
        this.mAttendeesList.setTag(this.mAttendeesList.getBackground());
        this.mOriginalPadding[0] = this.mLocationTextView.getPaddingLeft();
        this.mOriginalPadding[1] = this.mLocationTextView.getPaddingTop();
        this.mOriginalPadding[2] = this.mLocationTextView.getPaddingRight();
        this.mOriginalPadding[3] = this.mLocationTextView.getPaddingBottom();
        this.mEditViewList.add(this.mTitleTextView);
        this.mEditViewList.add(this.mLocationTextView);
        this.mEditViewList.add(this.mDescriptionTextView);
        this.mEditViewList.add(this.mAttendeesList);
        this.mViewOnlyList.add(view.findViewById(R.id.when_row));
        this.mViewOnlyList.add(view.findViewById(R.id.timezone_textview_row));
        this.mEditOnlyList.add(view.findViewById(R.id.all_day_row));
        this.mEditOnlyList.add(view.findViewById(R.id.availability_row));
        this.mEditOnlyList.add(view.findViewById(R.id.visibility_row));
        this.mEditOnlyList.add(view.findViewById(R.id.from_row));
        this.mEditOnlyList.add(view.findViewById(R.id.to_row));
        this.mEditOnlyList.add(this.mTimezoneRow);
        this.mEditOnlyList.add(this.mStartHomeGroup);
        this.mEditOnlyList.add(this.mEndHomeGroup);
        this.mResponseRadioGroup = (RadioGroup) view.findViewById(R.id.response_value);
        this.mRemindersContainer = (LinearLayout) view.findViewById(R.id.reminder_items_container);
        this.mTimezone = Utils.getTimeZone(activity, null);
        this.mIsMultipane = activity.getResources().getBoolean(R.bool.tablet_config);
        this.mStartTime = new Time(this.mTimezone);
        this.mEndTime = new Time(this.mTimezone);
        this.mEmailValidator = new Rfc822Validator(null);
        initMultiAutoCompleteTextView((RecipientEditTextView) this.mAttendeesList);
        setModel(null);
        FragmentManager fm = activity.getFragmentManager();
        RecurrencePickerDialog rpd = (RecurrencePickerDialog) fm.findFragmentByTag("recurrencePickerDialogFragment");
        if (rpd != null) {
            rpd.setOnRecurrenceSetListener(this);
        }
        TimeZonePickerDialog tzpd = (TimeZonePickerDialog) fm.findFragmentByTag("timeZonePickerDialogFragment");
        if (tzpd != null) {
            tzpd.setOnTimeZoneSetListener(this);
        }
        TimePickerDialog tpd = (TimePickerDialog) fm.findFragmentByTag("timePickerDialogFragment");
        if (tpd != null) {
            this.mTimeSelectedWasStartTime = timeSelectedWasStartTime;
            if (timeSelectedWasStartTime) {
                v2 = this.mStartTimeButton;
            } else {
                v2 = this.mEndTimeButton;
            }
            tpd.setOnTimeSetListener(new TimeListener(v2));
        }
        this.mDatePickerDialog = (DatePickerDialog) fm.findFragmentByTag("datePickerDialogFragment");
        if (this.mDatePickerDialog != null) {
            this.mDateSelectedWasStartDate = dateSelectedWasStartDate;
            if (dateSelectedWasStartDate) {
                v = this.mStartDateButton;
            } else {
                v = this.mEndDateButton;
            }
            this.mDatePickerDialog.setOnDateSetListener(new DateListener(v));
        }
    }

    private static ArrayList<Integer> loadIntegerArray(Resources r, int resNum) {
        int[] vals = r.getIntArray(resNum);
        int size = vals.length;
        ArrayList<Integer> list = new ArrayList<>(size);
        for (int i : vals) {
            list.add(Integer.valueOf(i));
        }
        return list;
    }

    private static ArrayList<String> loadStringArray(Resources r, int resNum) {
        String[] labels = r.getStringArray(resNum);
        ArrayList<String> list = new ArrayList<>(Arrays.asList(labels));
        return list;
    }

    private void prepareAvailability() {
        Resources r = this.mActivity.getResources();
        this.mAvailabilityValues = loadIntegerArray(r, R.array.availability_values);
        this.mAvailabilityLabels = loadStringArray(r, R.array.availability);
        this.mOriginalAvailabilityLabels = new ArrayList<>();
        this.mOriginalAvailabilityLabels.addAll(this.mAvailabilityLabels);
        if (this.mModel.mCalendarAllowedAvailability != null) {
            EventViewUtils.reduceMethodList(this.mAvailabilityValues, this.mAvailabilityLabels, this.mModel.mCalendarAllowedAvailability);
        }
        this.mAvailabilityAdapter = new ArrayAdapter<>(this.mActivity, android.R.layout.simple_spinner_item, this.mAvailabilityLabels);
        this.mAvailabilityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        this.mAvailabilitySpinner.setAdapter((SpinnerAdapter) this.mAvailabilityAdapter);
    }

    private void prepareReminders() {
        CalendarEventModel model = this.mModel;
        Resources r = this.mActivity.getResources();
        this.mReminderMinuteValues = loadIntegerArray(r, R.array.reminder_minutes_values);
        this.mReminderMinuteLabels = loadStringArray(r, R.array.reminder_minutes_labels);
        this.mReminderMethodValues = loadIntegerArray(r, R.array.reminder_methods_values);
        this.mReminderMethodLabels = loadStringArray(r, R.array.reminder_methods_labels);
        if (this.mModel.mCalendarAllowedReminders != null) {
            EventViewUtils.reduceMethodList(this.mReminderMethodValues, this.mReminderMethodLabels, this.mModel.mCalendarAllowedReminders);
        }
        int numReminders = 0;
        if (model.mHasAlarm) {
            ArrayList<CalendarEventModel.ReminderEntry> reminders = model.mReminders;
            numReminders = reminders.size();
            for (CalendarEventModel.ReminderEntry re : reminders) {
                if (this.mReminderMethodValues.contains(Integer.valueOf(re.getMethod()))) {
                    EventViewUtils.addMinutesToList(this.mActivity, this.mReminderMinuteValues, this.mReminderMinuteLabels, re.getMinutes());
                }
            }
            this.mUnsupportedReminders.clear();
            for (CalendarEventModel.ReminderEntry re2 : reminders) {
                if (this.mReminderMethodValues.contains(Integer.valueOf(re2.getMethod())) || re2.getMethod() == 0) {
                    EventViewUtils.addReminder(this.mActivity, this.mScrollView, this, this.mReminderItems, this.mReminderMinuteValues, this.mReminderMinuteLabels, this.mReminderMethodValues, this.mReminderMethodLabels, re2, Integer.MAX_VALUE, null);
                } else {
                    this.mUnsupportedReminders.add(re2);
                }
            }
        }
        updateRemindersVisibility(numReminders);
        EventViewUtils.updateAddReminderButton(this.mView, this.mReminderItems, this.mModel.mCalendarMaxReminders);
    }

    public void setModel(CalendarEventModel model) {
        this.mModel = model;
        if (this.mAddressAdapter != null && (this.mAddressAdapter instanceof EmailAddressAdapter)) {
            ((EmailAddressAdapter) this.mAddressAdapter).close();
            this.mAddressAdapter = null;
        }
        if (model == null) {
            this.mLoadingMessage.setVisibility(0);
            this.mScrollView.setVisibility(8);
            return;
        }
        boolean canRespond = EditEventHelper.canRespond(model);
        long begin = model.mStart;
        long end = model.mEnd;
        this.mTimezone = model.mTimezone;
        if (begin > 0) {
            this.mStartTime.timezone = this.mTimezone;
            this.mStartTime.set(begin);
            this.mStartTime.normalize(true);
        }
        if (end > 0) {
            this.mEndTime.timezone = this.mTimezone;
            this.mEndTime.set(end);
            this.mEndTime.normalize(true);
        }
        this.mRrule = model.mRrule;
        if (!TextUtils.isEmpty(this.mRrule)) {
            this.mEventRecurrence.parse(this.mRrule);
        }
        if (this.mEventRecurrence.startDate == null) {
            this.mEventRecurrence.startDate = this.mStartTime;
        }
        if (!model.mHasAttendeeData) {
            this.mAttendeesGroup.setVisibility(8);
        }
        this.mAllDayCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                EditEventView.this.setAllDayViewsVisibility(isChecked);
            }
        });
        boolean prevAllDay = this.mAllDayCheckBox.isChecked();
        this.mAllDay = false;
        if (model.mAllDay) {
            this.mAllDayCheckBox.setChecked(true);
            this.mTimezone = Utils.getTimeZone(this.mActivity, null);
            this.mStartTime.timezone = this.mTimezone;
            this.mEndTime.timezone = this.mTimezone;
            this.mEndTime.normalize(true);
        } else {
            this.mAllDayCheckBox.setChecked(false);
        }
        if (prevAllDay == this.mAllDayCheckBox.isChecked()) {
            setAllDayViewsVisibility(prevAllDay);
        }
        populateTimezone(this.mStartTime.normalize(true));
        SharedPreferences prefs = GeneralPreferences.getSharedPreferences(this.mActivity);
        String defaultReminderString = prefs.getString("preferences_default_reminder", "-1");
        this.mDefaultReminderMinutes = Integer.parseInt(defaultReminderString);
        prepareReminders();
        prepareAvailability();
        View reminderAddButton = this.mView.findViewById(R.id.reminder_add);
        View.OnClickListener addReminderOnClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditEventView.this.addReminder();
            }
        };
        reminderAddButton.setOnClickListener(addReminderOnClickListener);
        if (!this.mIsMultipane) {
            this.mView.findViewById(R.id.is_all_day_label).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    EditEventView.this.mAllDayCheckBox.setChecked(!EditEventView.this.mAllDayCheckBox.isChecked());
                }
            });
        }
        if (model.mTitle != null) {
            this.mTitleTextView.setTextKeepState(model.mTitle);
        }
        if (model.mIsOrganizer || TextUtils.isEmpty(model.mOrganizer) || model.mOrganizer.endsWith("calendar.google.com")) {
            this.mView.findViewById(R.id.organizer_label).setVisibility(8);
            this.mView.findViewById(R.id.organizer).setVisibility(8);
            this.mOrganizerGroup.setVisibility(8);
        } else {
            ((TextView) this.mView.findViewById(R.id.organizer)).setText(model.mOrganizerDisplayName);
        }
        if (model.mLocation != null) {
            this.mLocationTextView.setTextKeepState(model.mLocation);
        }
        if (model.mDescription != null) {
            this.mDescriptionTextView.setTextKeepState(model.mDescription);
        }
        int availIndex = this.mAvailabilityValues.indexOf(Integer.valueOf(model.mAvailability));
        if (availIndex != -1) {
            this.mAvailabilitySpinner.setSelection(availIndex);
        }
        this.mAccessLevelSpinner.setSelection(model.mAccessLevel);
        View responseLabel = this.mView.findViewById(R.id.response_label);
        if (canRespond) {
            int buttonToCheck = EventInfoFragment.findButtonIdForResponse(model.mSelfAttendeeStatus);
            this.mResponseRadioGroup.check(buttonToCheck);
            this.mResponseRadioGroup.setVisibility(0);
            responseLabel.setVisibility(0);
        } else {
            responseLabel.setVisibility(8);
            this.mResponseRadioGroup.setVisibility(8);
            this.mResponseGroup.setVisibility(8);
        }
        if (model.mUri != null) {
            View calendarGroup = this.mView.findViewById(R.id.calendar_selector_group);
            calendarGroup.setVisibility(8);
            ((TextView) this.mView.findViewById(R.id.calendar_textview)).setText(model.mCalendarDisplayName);
            TextView tv = (TextView) this.mView.findViewById(R.id.calendar_textview_secondary);
            if (tv != null) {
                tv.setText(model.mOwnerAccount);
            }
        } else {
            View calendarGroup2 = this.mView.findViewById(R.id.calendar_group);
            calendarGroup2.setVisibility(8);
        }
        if (model.isEventColorInitialized()) {
            updateHeadlineColor(model, model.getEventColor());
        }
        populateWhen();
        populateRepeats();
        updateAttendees(model.mAttendeesList);
        updateView();
        this.mScrollView.setVisibility(0);
        this.mLoadingMessage.setVisibility(8);
        sendAccessibilityEvent();
    }

    public void updateHeadlineColor(CalendarEventModel model, int displayColor) {
        if (model.mUri != null) {
            if (this.mIsMultipane) {
                this.mView.findViewById(R.id.calendar_textview_with_colorpicker).setBackgroundColor(displayColor);
                return;
            } else {
                this.mView.findViewById(R.id.calendar_group).setBackgroundColor(displayColor);
                return;
            }
        }
        setSpinnerBackgroundColor(displayColor);
    }

    private void setSpinnerBackgroundColor(int displayColor) {
        if (this.mIsMultipane) {
            this.mCalendarSelectorWrapper.setBackgroundColor(displayColor);
        } else {
            this.mCalendarSelectorGroup.setBackgroundColor(displayColor);
        }
    }

    private void sendAccessibilityEvent() {
        AccessibilityManager am = (AccessibilityManager) this.mActivity.getSystemService("accessibility");
        if (am.isEnabled() && this.mModel != null) {
            StringBuilder b = new StringBuilder();
            addFieldsRecursive(b, this.mView);
            CharSequence msg = b.toString();
            AccessibilityEvent event = AccessibilityEvent.obtain(8);
            event.setClassName(getClass().getName());
            event.setPackageName(this.mActivity.getPackageName());
            event.getText().add(msg);
            event.setAddedCount(msg.length());
            am.sendAccessibilityEvent(event);
        }
    }

    private void addFieldsRecursive(StringBuilder b, View v) {
        if (v != null && v.getVisibility() == 0) {
            if (v instanceof TextView) {
                CharSequence tv = ((TextView) v).getText();
                if (!TextUtils.isEmpty(tv.toString().trim())) {
                    b.append(((Object) tv) + ". ");
                    return;
                }
                return;
            }
            if (v instanceof RadioGroup) {
                RadioGroup rg = (RadioGroup) v;
                int id = rg.getCheckedRadioButtonId();
                if (id != -1) {
                    b.append(((Object) ((RadioButton) v.findViewById(id)).getText()) + ". ");
                    return;
                }
                return;
            }
            if (v instanceof Spinner) {
                Spinner s = (Spinner) v;
                if (s.getSelectedItem() instanceof String) {
                    String str = ((String) s.getSelectedItem()).trim();
                    if (!TextUtils.isEmpty(str)) {
                        b.append(str + ". ");
                        return;
                    }
                    return;
                }
                return;
            }
            if (v instanceof ViewGroup) {
                ViewGroup vg = (ViewGroup) v;
                int children = vg.getChildCount();
                for (int i = 0; i < children; i++) {
                    addFieldsRecursive(b, vg.getChildAt(i));
                }
            }
        }
    }

    protected void setWhenString() {
        int flags;
        String tz = this.mTimezone;
        if (this.mModel.mAllDay) {
            flags = 16 | 2;
            tz = "UTC";
        } else {
            flags = 16 | 1;
            if (DateFormat.is24HourFormat(this.mActivity)) {
                flags |= 128;
            }
        }
        long startMillis = this.mStartTime.normalize(true);
        long endMillis = this.mEndTime.normalize(true);
        mSB.setLength(0);
        String when = DateUtils.formatDateRange(this.mActivity, mF, startMillis, endMillis, flags, tz).toString();
        this.mWhenView.setText(when);
    }

    public void setCalendarsCursor(Cursor cursor, boolean userVisible, long selectedCalendarId) {
        int selection;
        this.mCalendarsCursor = cursor;
        if (cursor == null || cursor.getCount() == 0) {
            if (this.mSaveAfterQueryComplete) {
                this.mLoadingCalendarsDialog.cancel();
            }
            if (userVisible) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this.mActivity);
                builder.setTitle(R.string.no_syncable_calendars).setIconAttribute(android.R.attr.alertDialogIcon).setMessage(R.string.no_calendars_found).setPositiveButton(R.string.add_account, this).setNegativeButton(android.R.string.no, this).setOnCancelListener(this);
                this.mNoCalendarsDialog = builder.show();
                return;
            }
            return;
        }
        if (selectedCalendarId != -1) {
            selection = findSelectedCalendarPosition(cursor, selectedCalendarId);
        } else {
            selection = findDefaultCalendarPosition(cursor);
        }
        CalendarsAdapter adapter = new CalendarsAdapter(this.mActivity, R.layout.calendars_spinner_item, cursor);
        this.mCalendarsSpinner.setAdapter((SpinnerAdapter) adapter);
        this.mCalendarsSpinner.setOnItemSelectedListener(this);
        this.mCalendarsSpinner.setSelection(selection);
        if (this.mSaveAfterQueryComplete) {
            this.mLoadingCalendarsDialog.cancel();
            if (prepareForSave() && fillModelFromUI()) {
                int exit = userVisible ? 1 : 0;
                this.mDone.setDoneCode(exit | 2);
                this.mDone.run();
            } else if (userVisible) {
                this.mDone.setDoneCode(1);
                this.mDone.run();
            } else if (Log.isLoggable("EditEvent", 3)) {
                Log.d("EditEvent", "SetCalendarsCursor:Save failed and unable to exit view");
            }
        }
    }

    public void updateView() {
        if (this.mModel != null) {
            if (EditEventHelper.canModifyEvent(this.mModel)) {
                setViewStates(this.mModification);
            } else {
                setViewStates(0);
            }
        }
    }

    private void setViewStates(int mode) {
        if (mode == 0 || !EditEventHelper.canModifyEvent(this.mModel)) {
            setWhenString();
            Iterator<View> it = this.mViewOnlyList.iterator();
            while (it.hasNext()) {
                it.next().setVisibility(0);
            }
            Iterator<View> it2 = this.mEditOnlyList.iterator();
            while (it2.hasNext()) {
                it2.next().setVisibility(8);
            }
            for (View v : this.mEditViewList) {
                v.setEnabled(false);
                v.setBackgroundDrawable(null);
            }
            this.mCalendarSelectorGroup.setVisibility(8);
            this.mCalendarStaticGroup.setVisibility(0);
            this.mRruleButton.setEnabled(false);
            if (EditEventHelper.canAddReminders(this.mModel)) {
                this.mRemindersGroup.setVisibility(0);
            } else {
                this.mRemindersGroup.setVisibility(8);
            }
            if (TextUtils.isEmpty(this.mLocationTextView.getText())) {
                this.mLocationGroup.setVisibility(8);
            }
            if (TextUtils.isEmpty(this.mDescriptionTextView.getText())) {
                this.mDescriptionGroup.setVisibility(8);
            }
        } else {
            Iterator<View> it3 = this.mViewOnlyList.iterator();
            while (it3.hasNext()) {
                it3.next().setVisibility(8);
            }
            Iterator<View> it4 = this.mEditOnlyList.iterator();
            while (it4.hasNext()) {
                it4.next().setVisibility(0);
            }
            for (View v2 : this.mEditViewList) {
                v2.setEnabled(true);
                if (v2.getTag() != null) {
                    v2.setBackgroundDrawable((Drawable) v2.getTag());
                    v2.setPadding(this.mOriginalPadding[0], this.mOriginalPadding[1], this.mOriginalPadding[2], this.mOriginalPadding[3]);
                }
            }
            if (this.mModel.mUri == null) {
                this.mCalendarSelectorGroup.setVisibility(0);
                this.mCalendarStaticGroup.setVisibility(8);
            } else {
                this.mCalendarSelectorGroup.setVisibility(8);
                this.mCalendarStaticGroup.setVisibility(0);
            }
            if (this.mModel.mOriginalSyncId == null) {
                this.mRruleButton.setEnabled(true);
            } else {
                this.mRruleButton.setEnabled(false);
                this.mRruleButton.setBackgroundDrawable(null);
            }
            this.mRemindersGroup.setVisibility(0);
            this.mLocationGroup.setVisibility(0);
            this.mDescriptionGroup.setVisibility(0);
        }
        setAllDayViewsVisibility(this.mAllDayCheckBox.isChecked());
    }

    public void setModification(int modifyWhich) {
        this.mModification = modifyWhich;
        updateView();
        updateHomeTime();
    }

    private int findSelectedCalendarPosition(Cursor calendarsCursor, long calendarId) {
        if (calendarsCursor.getCount() <= 0) {
            return -1;
        }
        int calendarIdColumn = calendarsCursor.getColumnIndexOrThrow("_id");
        int position = 0;
        calendarsCursor.moveToPosition(-1);
        while (calendarsCursor.moveToNext()) {
            if (calendarsCursor.getLong(calendarIdColumn) != calendarId) {
                position++;
            } else {
                return position;
            }
        }
        return 0;
    }

    private int findDefaultCalendarPosition(Cursor calendarsCursor) {
        if (calendarsCursor.getCount() <= 0) {
            return -1;
        }
        String defaultCalendar = Utils.getSharedPreference(this.mActivity, "preference_defaultCalendar", (String) null);
        int calendarsOwnerIndex = calendarsCursor.getColumnIndexOrThrow("ownerAccount");
        int accountNameIndex = calendarsCursor.getColumnIndexOrThrow("account_name");
        int accountTypeIndex = calendarsCursor.getColumnIndexOrThrow("account_type");
        int position = 0;
        calendarsCursor.moveToPosition(-1);
        while (calendarsCursor.moveToNext()) {
            String calendarOwner = calendarsCursor.getString(calendarsOwnerIndex);
            if (defaultCalendar == null) {
                if (calendarOwner != null && calendarOwner.equals(calendarsCursor.getString(accountNameIndex)) && !"LOCAL".equals(calendarsCursor.getString(accountTypeIndex))) {
                    return position;
                }
            } else if (defaultCalendar.equals(calendarOwner)) {
                return position;
            }
            position++;
        }
        return 0;
    }

    private void updateAttendees(HashMap<String, CalendarEventModel.Attendee> attendeesList) {
        if (attendeesList != null && !attendeesList.isEmpty()) {
            this.mAttendeesList.setText((CharSequence) null);
            for (CalendarEventModel.Attendee attendee : attendeesList.values()) {
                this.mAttendeesList.append(attendee.mEmail + ", ");
            }
        }
    }

    private void updateRemindersVisibility(int numReminders) {
        if (numReminders == 0) {
            this.mRemindersContainer.setVisibility(8);
        } else {
            this.mRemindersContainer.setVisibility(0);
        }
    }

    private void addReminder() {
        if (this.mDefaultReminderMinutes == -1) {
            EventViewUtils.addReminder(this.mActivity, this.mScrollView, this, this.mReminderItems, this.mReminderMinuteValues, this.mReminderMinuteLabels, this.mReminderMethodValues, this.mReminderMethodLabels, CalendarEventModel.ReminderEntry.valueOf(10), this.mModel.mCalendarMaxReminders, null);
        } else {
            EventViewUtils.addReminder(this.mActivity, this.mScrollView, this, this.mReminderItems, this.mReminderMinuteValues, this.mReminderMinuteLabels, this.mReminderMethodValues, this.mReminderMethodLabels, CalendarEventModel.ReminderEntry.valueOf(this.mDefaultReminderMinutes), this.mModel.mCalendarMaxReminders, null);
        }
        updateRemindersVisibility(this.mReminderItems.size());
        EventViewUtils.updateAddReminderButton(this.mView, this.mReminderItems, this.mModel.mCalendarMaxReminders);
    }

    private MultiAutoCompleteTextView initMultiAutoCompleteTextView(RecipientEditTextView list) {
        if (ChipsUtil.supportsChipsUi()) {
            this.mAddressAdapter = new RecipientAdapter(this.mActivity);
            list.setAdapter((BaseRecipientAdapter) this.mAddressAdapter);
            list.setOnFocusListShrinkRecipients(false);
        } else {
            this.mAddressAdapter = new EmailAddressAdapter(this.mActivity);
            list.setAdapter((EmailAddressAdapter) this.mAddressAdapter);
        }
        list.setTokenizer(new Rfc822Tokenizer());
        list.setValidator(this.mEmailValidator);
        list.setFilters(sRecipientFilters);
        return list;
    }

    private void setDate(TextView view, long millis) {
        String dateString;
        synchronized (TimeZone.class) {
            TimeZone.setDefault(TimeZone.getTimeZone(this.mTimezone));
            dateString = DateUtils.formatDateTime(this.mActivity, millis, 98326);
            TimeZone.setDefault(null);
        }
        view.setText(dateString);
    }

    private void setTime(TextView view, long millis) {
        String timeString;
        int flags = 1 | 5120;
        if (DateFormat.is24HourFormat(this.mActivity)) {
            flags |= 128;
        }
        synchronized (TimeZone.class) {
            TimeZone.setDefault(TimeZone.getTimeZone(this.mTimezone));
            timeString = DateUtils.formatDateTime(this.mActivity, millis, flags);
            TimeZone.setDefault(null);
        }
        view.setText(timeString);
    }

    protected void setAllDayViewsVisibility(boolean isChecked) {
        if (isChecked) {
            if (this.mEndTime.hour == 0 && this.mEndTime.minute == 0) {
                if (this.mAllDay != isChecked) {
                    Time time = this.mEndTime;
                    time.monthDay--;
                }
                long endMillis = this.mEndTime.normalize(true);
                if (this.mEndTime.before(this.mStartTime)) {
                    this.mEndTime.set(this.mStartTime);
                    endMillis = this.mEndTime.normalize(true);
                }
                setDate(this.mEndDateButton, endMillis);
                setTime(this.mEndTimeButton, endMillis);
            }
            this.mStartTimeButton.setVisibility(8);
            this.mEndTimeButton.setVisibility(8);
            this.mTimezoneRow.setVisibility(8);
        } else {
            if (this.mEndTime.hour == 0 && this.mEndTime.minute == 0) {
                if (this.mAllDay != isChecked) {
                    this.mEndTime.monthDay++;
                }
                long endMillis2 = this.mEndTime.normalize(true);
                setDate(this.mEndDateButton, endMillis2);
                setTime(this.mEndTimeButton, endMillis2);
            }
            this.mStartTimeButton.setVisibility(0);
            this.mEndTimeButton.setVisibility(0);
            this.mTimezoneRow.setVisibility(0);
        }
        if (this.mModel.mUri == null && !this.mAvailabilityExplicitlySet) {
            int newAvailabilityValue = isChecked ? 1 : 0;
            if (this.mAvailabilityAdapter != null && this.mAvailabilityValues != null && this.mAvailabilityValues.contains(Integer.valueOf(newAvailabilityValue))) {
                this.mAllDayChangingAvailability = true;
                String newAvailabilityLabel = this.mOriginalAvailabilityLabels.get(newAvailabilityValue);
                int newAvailabilityPos = this.mAvailabilityAdapter.getPosition(newAvailabilityLabel);
                this.mAvailabilitySpinner.setSelection(newAvailabilityPos);
            }
        }
        this.mAllDay = isChecked;
        updateHomeTime();
    }

    public void setColorPickerButtonStates(int[] colorArray) {
        setColorPickerButtonStates(colorArray != null && colorArray.length > 0);
    }

    public void setColorPickerButtonStates(boolean showColorPalette) {
        if (showColorPalette) {
            this.mColorPickerNewEvent.setVisibility(0);
            this.mColorPickerExistingEvent.setVisibility(0);
        } else {
            this.mColorPickerNewEvent.setVisibility(4);
            this.mColorPickerExistingEvent.setVisibility(8);
        }
    }

    public boolean isColorPaletteVisible() {
        return this.mColorPickerNewEvent.getVisibility() == 0 || this.mColorPickerExistingEvent.getVisibility() == 0;
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        Cursor c = (Cursor) parent.getItemAtPosition(position);
        if (c == null) {
            Log.w("EditEvent", "Cursor not set on calendar item");
            return;
        }
        int idColumn = c.getColumnIndexOrThrow("_id");
        long calendarId = c.getLong(idColumn);
        int colorColumn = c.getColumnIndexOrThrow("calendar_color");
        int color = c.getInt(colorColumn);
        int displayColor = Utils.getDisplayColorFromColor(color);
        if (calendarId != this.mModel.mCalendarId || !this.mModel.isCalendarColorInitialized() || displayColor != this.mModel.getCalendarColor()) {
            setSpinnerBackgroundColor(displayColor);
            this.mModel.mCalendarId = calendarId;
            this.mModel.setCalendarColor(displayColor);
            this.mModel.mCalendarAccountName = c.getString(11);
            this.mModel.mCalendarAccountType = c.getString(12);
            this.mModel.setEventColor(this.mModel.getCalendarColor());
            setColorPickerButtonStates(this.mModel.getCalendarEventColors());
            int maxRemindersColumn = c.getColumnIndexOrThrow("maxReminders");
            this.mModel.mCalendarMaxReminders = c.getInt(maxRemindersColumn);
            int allowedRemindersColumn = c.getColumnIndexOrThrow("allowedReminders");
            this.mModel.mCalendarAllowedReminders = c.getString(allowedRemindersColumn);
            int allowedAttendeeTypesColumn = c.getColumnIndexOrThrow("allowedAttendeeTypes");
            this.mModel.mCalendarAllowedAttendeeTypes = c.getString(allowedAttendeeTypesColumn);
            int allowedAvailabilityColumn = c.getColumnIndexOrThrow("allowedAvailability");
            this.mModel.mCalendarAllowedAvailability = c.getString(allowedAvailabilityColumn);
            this.mModel.mReminders.clear();
            this.mModel.mReminders.addAll(this.mModel.mDefaultReminders);
            this.mModel.mHasAlarm = this.mModel.mReminders.size() != 0;
            this.mReminderItems.clear();
            LinearLayout reminderLayout = (LinearLayout) this.mScrollView.findViewById(R.id.reminder_items_container);
            reminderLayout.removeAllViews();
            prepareReminders();
            prepareAvailability();
        }
    }

    private void updateHomeTime() {
        String tz = Utils.getTimeZone(this.mActivity, null);
        if (!this.mAllDayCheckBox.isChecked() && !TextUtils.equals(tz, this.mTimezone) && this.mModification != 0) {
            int flags = 1;
            boolean is24Format = DateFormat.is24HourFormat(this.mActivity);
            if (is24Format) {
                flags = 1 | 128;
            }
            long millisStart = this.mStartTime.toMillis(false);
            long millisEnd = this.mEndTime.toMillis(false);
            boolean isDSTStart = this.mStartTime.isDst != 0;
            boolean isDSTEnd = this.mEndTime.isDst != 0;
            String tzDisplay = TimeZone.getTimeZone(tz).getDisplayName(isDSTStart, 0, Locale.getDefault());
            StringBuilder time = new StringBuilder();
            mSB.setLength(0);
            time.append(DateUtils.formatDateRange(this.mActivity, mF, millisStart, millisStart, flags, tz)).append(" ").append(tzDisplay);
            this.mStartTimeHome.setText(time.toString());
            mSB.setLength(0);
            this.mStartDateHome.setText(DateUtils.formatDateRange(this.mActivity, mF, millisStart, millisStart, 524310, tz).toString());
            if (isDSTEnd != isDSTStart) {
                tzDisplay = TimeZone.getTimeZone(tz).getDisplayName(isDSTEnd, 0, Locale.getDefault());
            }
            int flags2 = 1;
            if (is24Format) {
                flags2 = 1 | 128;
            }
            time.setLength(0);
            mSB.setLength(0);
            time.append(DateUtils.formatDateRange(this.mActivity, mF, millisEnd, millisEnd, flags2, tz)).append(" ").append(tzDisplay);
            this.mEndTimeHome.setText(time.toString());
            mSB.setLength(0);
            this.mEndDateHome.setText(DateUtils.formatDateRange(this.mActivity, mF, millisEnd, millisEnd, 524310, tz).toString());
            this.mStartHomeGroup.setVisibility(0);
            this.mEndHomeGroup.setVisibility(0);
            return;
        }
        this.mStartHomeGroup.setVisibility(8);
        this.mEndHomeGroup.setVisibility(8);
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }
}
