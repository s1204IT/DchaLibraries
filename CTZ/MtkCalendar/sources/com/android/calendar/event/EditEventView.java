package com.android.calendar.event;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.FragmentManager;
import android.app.ProgressDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.os.BenesseExtension;
import android.os.Bundle;
import android.os.PowerManager;
import android.text.InputFilter;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.text.util.Rfc822Token;
import android.text.util.Rfc822Tokenizer;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.MultiAutoCompleteTextView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ResourceCursorAdapter;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.TimePicker;
import com.android.calendar.CalendarEventModel;
import com.android.calendar.ChipsAddressTextView;
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
import com.android.datetimepicker.date.MonthAdapter;
import com.android.datetimepicker.time.RadialPickerLayout;
import com.android.datetimepicker.time.TimePickerDialog;
import com.android.mtkex.chips.AccountSpecifier;
import com.android.mtkex.chips.BaseRecipientAdapter;
import com.android.mtkex.chips.ChipsUtil;
import com.android.mtkex.chips.MTKRecipientList;
import com.android.timezonepicker.TimeZoneInfo;
import com.android.timezonepicker.TimeZonePickerDialog;
import com.android.timezonepicker.TimeZonePickerUtils;
import com.mediatek.calendar.LogUtil;
import com.mediatek.calendar.MTKToast;
import com.mediatek.calendar.edittext.IEditTextExt;
import com.mediatek.calendar.ext.IEditEventViewExt;
import com.mediatek.calendar.ext.OpCalendarCustomizationFactoryBase;
import com.mediatek.calendar.extension.ExtensionFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
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
    private IEditEventViewExt mEditEventViewExt;
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
    private Time mPreStartTime;
    private ArrayList<String> mReminderMethodLabels;
    private ArrayList<Integer> mReminderMethodValues;
    private ArrayList<String> mReminderMinuteLabels;
    private ArrayList<Integer> mReminderMinuteValues;
    LinearLayout mRemindersContainer;
    View mRemindersGroup;
    View mResponseGroup;
    RadioGroup mResponseRadioGroup;
    private boolean mRestoredView;
    private String mRrule;
    Button mRruleButton;
    ScrollView mScrollView;
    int[] mSelectedDate;
    public boolean mShowDatePicker;
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
    private static StringBuilder sStringBuilder = new StringBuilder(50);
    private static Formatter sFormatter = new Formatter(sStringBuilder, Locale.getDefault());
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

    private class TimeListener implements TimePickerDialog.OnTimeSetListener, TimePickerDialog.OnTimeSetListener {
        private View mView;

        public TimeListener(View view) {
            this.mView = view;
        }

        private void onTimeSetImpl(int i, int i2) {
            long millis;
            Time time = EditEventView.this.mStartTime;
            Time time2 = EditEventView.this.mEndTime;
            if (this.mView == EditEventView.this.mStartTimeButton) {
                int i3 = time2.hour - time.hour;
                int i4 = time2.minute - time.minute;
                time.hour = i;
                time.minute = i2;
                millis = time.normalize(true);
                time2.hour = i + i3;
                time2.minute = i2 + i4;
                EditEventView.this.populateTimezone(millis);
            } else {
                millis = time.toMillis(true);
                time2.hour = i;
                time2.minute = i2;
                if (time2.before(time)) {
                    time2.monthDay = time.monthDay + 1;
                }
            }
            long jNormalize = time2.normalize(true);
            EditEventView.this.setDate(EditEventView.this.mEndDateButton, jNormalize);
            EditEventView.this.setTime(EditEventView.this.mStartTimeButton, millis);
            EditEventView.this.setTime(EditEventView.this.mEndTimeButton, jNormalize);
            EditEventView.this.updateHomeTime();
        }

        @Override
        public void onTimeSet(RadialPickerLayout radialPickerLayout, int i, int i2) {
            onTimeSetImpl(i, i2);
        }

        @Override
        public void onTimeSet(TimePicker timePicker, int i, int i2) {
            onTimeSetImpl(i, i2);
        }
    }

    private class TimeClickListener implements View.OnClickListener {
        private Time mTime;

        public TimeClickListener(Time time) {
            this.mTime = time;
        }

        @Override
        public void onClick(View view) {
            com.android.datetimepicker.time.TimePickerDialog timePickerDialog;
            if (view == EditEventView.this.mStartTimeButton) {
                EditEventView.this.mTimeSelectedWasStartTime = true;
                if (EditEventView.this.mStartTimePickerDialog != null) {
                    EditEventView.this.mStartTimePickerDialog.setStartTime(this.mTime.hour, this.mTime.minute);
                } else {
                    EditEventView.this.mStartTimePickerDialog = com.android.datetimepicker.time.TimePickerDialog.newInstance(EditEventView.this.new TimeListener(view), this.mTime.hour, this.mTime.minute, DateFormat.is24HourFormat(EditEventView.this.mActivity));
                }
                timePickerDialog = EditEventView.this.mStartTimePickerDialog;
            } else {
                EditEventView.this.mTimeSelectedWasStartTime = false;
                if (EditEventView.this.mEndTimePickerDialog != null) {
                    EditEventView.this.mEndTimePickerDialog.setStartTime(this.mTime.hour, this.mTime.minute);
                } else {
                    EditEventView.this.mEndTimePickerDialog = com.android.datetimepicker.time.TimePickerDialog.newInstance(EditEventView.this.new TimeListener(view), this.mTime.hour, this.mTime.minute, DateFormat.is24HourFormat(EditEventView.this.mActivity));
                }
                timePickerDialog = EditEventView.this.mEndTimePickerDialog;
            }
            EditEventView.this.hideInputMethod();
            FragmentManager fragmentManager = EditEventView.this.mActivity.getFragmentManager();
            fragmentManager.executePendingTransactions();
            if (timePickerDialog != null && !timePickerDialog.isAdded()) {
                try {
                    timePickerDialog.show(fragmentManager, "timePickerDialogFragment");
                } catch (IllegalStateException e) {
                    LogUtil.d("EditEventView", "FRAG_TAG_TIME_PICKER status= ");
                }
            }
        }
    }

    private class DateListener implements DatePickerDialog.OnDateSetListener, DatePickerDialog.OnDateSetListener {
        View mView;

        public DateListener(View view) {
            this.mView = view;
        }

        private void onDateSetImpl(int i, int i2, int i3) {
            Log.d("EditEventView", "onDateSet: " + i + " " + i2 + " " + i3);
            Time time = EditEventView.this.mStartTime;
            Time time2 = EditEventView.this.mEndTime;
            EditEventView.this.mPreStartTime = new Time(EditEventView.this.mStartTime);
            EditEventView.this.mPreStartTime.normalize(false);
            if (this.mView == EditEventView.this.mStartDateButton) {
                int i4 = time2.year - time.year;
                int i5 = time2.month - time.month;
                int i6 = time2.monthDay - time.monthDay;
                time.year = i;
                time.month = i2;
                time.monthDay = i3;
                long jNormalize = time.normalize(true);
                time2.year = i + i4;
                time2.month = i2 + i5;
                time2.monthDay = i3 + i6;
                time2.normalize(true);
                EditEventView.this.populateTimezone(jNormalize);
            } else {
                time.toMillis(true);
                time2.year = i;
                time2.month = i2;
                time2.monthDay = i3;
                time2.normalize(true);
                if (time2.before(time)) {
                    time2.set(time);
                }
            }
            long jCheckDateRange = EditEventView.this.checkDateRange(time);
            long jCheckDateRange2 = EditEventView.this.checkDateRange(time2);
            EditEventView.this.populateRepeats();
            EditEventView.this.mPreStartTime = null;
            EditEventView.this.setDate(EditEventView.this.mStartDateButton, jCheckDateRange);
            EditEventView.this.setDate(EditEventView.this.mEndDateButton, jCheckDateRange2);
            EditEventView.this.setTime(EditEventView.this.mEndTimeButton, jCheckDateRange2);
            EditEventView.this.updateHomeTime();
            EditEventView.this.mShowDatePicker = false;
            EditEventView.this.mDatePickerDialog = null;
            Log.d("EditEventView", "onDateSet: exit making datepicker null & false as user selected done");
        }

        @Override
        public void onDateSet(com.android.datetimepicker.date.DatePickerDialog datePickerDialog, int i, int i2, int i3) {
            onDateSetImpl(i, i2, i3);
        }

        @Override
        public void onDateSet(DatePicker datePicker, int i, int i2, int i3) {
            onDateSetImpl(i, i2, i3);
        }

        @Override
        public void onCancelDialog() {
            EditEventView.this.mShowDatePicker = false;
            EditEventView.this.mDatePickerDialog = null;
            Log.d("EditEventView", "onCancelDialog: exit making datepicker null & false as dialog");
        }
    }

    private void populateWhen() {
        long millis = this.mStartTime.toMillis(false);
        long millis2 = this.mEndTime.toMillis(false);
        setDate(this.mStartDateButton, millis);
        setDate(this.mEndDateButton, millis2);
        setTime(this.mStartTimeButton, millis);
        setTime(this.mEndTimeButton, millis2);
        this.mStartDateButton.setOnClickListener(new DateClickListener(this.mStartTime));
        this.mEndDateButton.setOnClickListener(new DateClickListener(this.mEndTime));
        this.mStartTimeButton.setOnClickListener(new TimeClickListener(this.mStartTime));
        this.mEndTimeButton.setOnClickListener(new TimeClickListener(this.mEndTime));
    }

    @Override
    public void onTimeZoneSet(TimeZoneInfo timeZoneInfo) {
        setTimezone(timeZoneInfo.mTzId);
        updateHomeTime();
    }

    private void setTimezone(String str) {
        this.mTimezone = str;
        this.mStartTime.timezone = this.mTimezone;
        long jNormalize = this.mStartTime.normalize(true);
        this.mEndTime.timezone = this.mTimezone;
        this.mEndTime.normalize(true);
        populateTimezone(jNormalize);
    }

    private void populateTimezone(long j) {
        if (this.mTzPickerUtils == null) {
            this.mTzPickerUtils = new TimeZonePickerUtils(this.mActivity);
        }
        CharSequence gmtDisplayName = this.mTzPickerUtils.getGmtDisplayName(this.mActivity, this.mTimezone, j, true);
        this.mTimezoneTextView.setText(gmtDisplayName);
        this.mTimezoneButton.setText(gmtDisplayName);
    }

    private void showTimezoneDialog() {
        Bundle bundle = new Bundle();
        bundle.putLong("bundle_event_start_time", this.mStartTime.toMillis(false));
        bundle.putString("bundle_event_time_zone", this.mTimezone);
        FragmentManager fragmentManager = this.mActivity.getFragmentManager();
        TimeZonePickerDialog timeZonePickerDialog = (TimeZonePickerDialog) fragmentManager.findFragmentByTag("timeZonePickerDialogFragment");
        if (timeZonePickerDialog != null) {
            timeZonePickerDialog.dismiss();
        }
        TimeZonePickerDialog timeZonePickerDialog2 = new TimeZonePickerDialog();
        timeZonePickerDialog2.setArguments(bundle);
        timeZonePickerDialog2.setOnTimeZoneSetListener(this);
        try {
            timeZonePickerDialog2.show(fragmentManager, "timeZonePickerDialogFragment");
        } catch (IllegalStateException e) {
            LogUtil.d("EditEventView", "FRAG_TAG_TIME_ZONE_PICKER status= ");
        }
    }

    private void populateRepeats() {
        String string;
        Resources resources = this.mActivity.getResources();
        int minRepeatMode = RecurrencePickerDialog.getMinRepeatMode(this.mEndTime.toMillis(false) - this.mStartTime.toMillis(false));
        boolean z = true;
        if (!TextUtils.isEmpty(this.mRrule)) {
            if (minRepeatMode == 0) {
                string = resources.getString(R.string.does_not_repeat);
                this.mRrule = null;
            } else {
                this.mEventRecurrence.setStartDate(this.mStartTime);
                if (this.mEventRecurrence.freq < minRepeatMode) {
                    this.mEventRecurrence.parse(getDefaultRruleByFrequecy(minRepeatMode, this.mStartTime.weekDay));
                } else {
                    if (this.mPreStartTime == null) {
                        this.mPreStartTime = new Time(this.mStartTime);
                        this.mPreStartTime.normalize(false);
                    }
                    recalculateEventRecurrenceRule(this.mPreStartTime, this.mStartTime, this.mEndTime, this.mEventRecurrence);
                }
                this.mRrule = this.mEventRecurrence.toString();
                String repeatString = EventRecurrenceFormatter.getRepeatString(this.mActivity, resources, this.mEventRecurrence, true);
                if (repeatString == null) {
                    string = resources.getString(R.string.custom);
                    Log.e("EditEventView", "Can't generate display string for " + this.mRrule);
                } else {
                    boolean zCanHandleRecurrenceRule = RecurrencePickerDialog.canHandleRecurrenceRule(this.mEventRecurrence);
                    if (!zCanHandleRecurrenceRule) {
                        Log.e("EditEventView", "UI can't handle " + this.mRrule);
                    }
                    z = zCanHandleRecurrenceRule;
                    string = repeatString;
                }
            }
            z = false;
        } else {
            string = resources.getString(R.string.does_not_repeat);
            if (minRepeatMode == 0) {
                z = false;
            }
        }
        this.mRruleButton.setText(string);
        boolean z2 = this.mModel.mOriginalSyncId == null ? z : false;
        this.mRruleButton.setOnClickListener(this);
        this.mRruleButton.setEnabled(z2);
    }

    private class DateClickListener implements View.OnClickListener {
        private Time mTime;

        public DateClickListener(Time time) {
            this.mTime = time;
        }

        @Override
        public void onClick(View view) {
            if (!EditEventView.this.mView.hasWindowFocus()) {
                return;
            }
            if (view == EditEventView.this.mStartDateButton) {
                EditEventView.this.mDateSelectedWasStartDate = true;
            } else {
                EditEventView.this.mDateSelectedWasStartDate = false;
            }
            DateListener dateListener = EditEventView.this.new DateListener(view);
            if (EditEventView.this.mDatePickerDialog != null) {
                EditEventView.this.mDatePickerDialog.dismiss();
            }
            EditEventView.this.hideInputMethod();
            EditEventView.this.createDatePickerDialog(dateListener, this.mTime.year, this.mTime.month, this.mTime.monthDay);
        }
    }

    public static class CalendarsAdapter extends ResourceCursorAdapter {
        public CalendarsAdapter(Context context, int i, Cursor cursor) {
            super(context, i, cursor);
            setDropDownViewResource(R.layout.calendars_dropdown_item);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            View viewFindViewById = view.findViewById(R.id.color);
            int columnIndexOrThrow = cursor.getColumnIndexOrThrow("calendar_color");
            int columnIndexOrThrow2 = cursor.getColumnIndexOrThrow("calendar_displayName");
            int columnIndexOrThrow3 = cursor.getColumnIndexOrThrow("ownerAccount");
            if (viewFindViewById != null) {
                viewFindViewById.setBackgroundColor(Utils.getDisplayColorFromColor(cursor.getInt(columnIndexOrThrow)));
            }
            TextView textView = (TextView) view.findViewById(R.id.calendar_name);
            if (textView != null) {
                textView.setText(cursor.getString(columnIndexOrThrow2));
                TextView textView2 = (TextView) view.findViewById(R.id.account_name);
                if (textView2 != null) {
                    textView2.setText(cursor.getString(columnIndexOrThrow3));
                    textView2.setVisibility(0);
                }
            }
        }
    }

    public boolean prepareForSave() {
        Log.d("EditEventView", "[Calendar] prepareForSave() of EditEventView");
        if (this.mModel == null) {
            return false;
        }
        if (this.mCalendarsCursor == null && this.mModel.mUri == null) {
            return false;
        }
        return fillModelFromUI();
    }

    @Override
    public void onClick(View view) {
        if (view == this.mRruleButton) {
            Bundle bundle = new Bundle();
            if (!this.mAllDay) {
                bundle.putLong("bundle_event_start_time", this.mStartTime.toMillis(false));
                bundle.putLong("budle_event_time_duration", this.mEndTime.toMillis(false) - this.mStartTime.toMillis(false));
            } else {
                Time time = new Time(this.mStartTime);
                Time time2 = new Time(this.mEndTime);
                time.hour = 0;
                time.minute = 0;
                time.second = 0;
                time2.hour = 0;
                time2.minute = 0;
                time2.second = 0;
                bundle.putLong("bundle_event_start_time", time.toMillis(false));
                bundle.putLong("budle_event_time_duration", (time2.toMillis(false) - time.toMillis(false)) + 86400000);
            }
            bundle.putString("bundle_event_time_zone", this.mStartTime.timezone);
            bundle.putString("bundle_event_rrule", this.mRrule);
            FragmentManager fragmentManager = this.mActivity.getFragmentManager();
            RecurrencePickerDialog recurrencePickerDialog = (RecurrencePickerDialog) fragmentManager.findFragmentByTag("recurrencePickerDialogFragment");
            if (recurrencePickerDialog != null) {
                recurrencePickerDialog.dismiss();
            }
            if (this.mActivity.isResumed()) {
                RecurrencePickerDialog recurrencePickerDialog2 = new RecurrencePickerDialog();
                recurrencePickerDialog2.setArguments(bundle);
                recurrencePickerDialog2.setOnRecurrenceSetListener(this);
                recurrencePickerDialog2.show(fragmentManager, "recurrencePickerDialogFragment");
                return;
            }
            Log.w("EditEventView", "error maybe happened, the activity is not resumed.");
            return;
        }
        LinearLayout linearLayout = (LinearLayout) view.getParent();
        ((LinearLayout) linearLayout.getParent()).removeView(linearLayout);
        this.mReminderItems.remove(linearLayout);
        updateRemindersVisibility(this.mReminderItems.size());
        EventViewUtils.updateAddReminderButton(this.mView, this.mReminderItems, this.mModel.mCalendarMaxReminders);
    }

    @Override
    public void onRecurrenceSet(String str) {
        Log.d("EditEventView", "Old rrule:" + this.mRrule);
        Log.d("EditEventView", "New rrule:" + str);
        this.mRrule = str;
        if (this.mRrule != null) {
            this.mEventRecurrence.parse(this.mRrule);
        }
        populateRepeats();
    }

    public void requestFocus() {
        InputMethodManager inputMethodManager = (InputMethodManager) this.mActivity.getSystemService("input_method");
        if (this.mCalendarsCursor == null || this.mCalendarsCursor.getCount() == 0) {
            View currentFocus = this.mActivity.getCurrentFocus();
            if (currentFocus != null) {
                inputMethodManager.hideSoftInputFromWindow(currentFocus.getWindowToken(), 0);
                currentFocus.clearFocus();
            }
            Log.i("EditEventView", "no calendar account, dismiss inputmethod. focusView=" + currentFocus);
            return;
        }
        if (!this.mTitleTextView.isFocused() && !this.mLocationTextView.isFocused() && !this.mDescriptionTextView.isFocused() && !this.mAttendeesList.isFocused()) {
            Log.i("EditEventView", "mTitleTextView no focus, request it.");
            this.mView.requestFocus(2);
            this.mTitleTextView.requestFocus();
            if (!this.mIsMultipane) {
                inputMethodManager.toggleSoftInput(1, 2);
                return;
            }
            return;
        }
        Log.i("EditEventView", "mTitleTextView have focus, just do nothing.");
    }

    @Override
    public void onCancel(DialogInterface dialogInterface) {
        if (dialogInterface == this.mLoadingCalendarsDialog) {
            this.mLoadingCalendarsDialog = null;
            this.mSaveAfterQueryComplete = false;
        } else if (dialogInterface == this.mNoCalendarsDialog) {
            this.mDone.setDoneCode(1);
            this.mDone.run();
        }
    }

    @Override
    public void onClick(DialogInterface dialogInterface, int i) {
        if (dialogInterface == this.mNoCalendarsDialog) {
            this.mDone.setDoneCode(1);
            this.mDone.run();
            if (i != -1 || BenesseExtension.getDchaState() != 0) {
                return;
            }
            Intent intent = new Intent("android.settings.ADD_ACCOUNT_SETTINGS");
            intent.putExtra("authorities", new String[]{"com.android.calendar"});
            intent.addFlags(67108864);
            this.mActivity.startActivity(intent);
        }
    }

    private boolean fillModelFromUI() {
        if (this.mModel == null) {
            return false;
        }
        Log.d("EditEventView", "[Calendar] fillModelFromUI() of EditEventView");
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
        int responseFromButtonId = EventInfoFragment.getResponseFromButtonId(this.mResponseRadioGroup.getCheckedRadioButtonId());
        if (responseFromButtonId != 0) {
            this.mModel.mSelfAttendeeStatus = responseFromButtonId;
        }
        if (this.mAttendeesList != null) {
            this.mEmailValidator.setRemoveInvalid(true);
            this.mAttendeesList.performValidation();
            this.mModel.mAttendeesList.clear();
            String string = this.mAttendeesList.getText().toString();
            if (!TextUtils.isEmpty(string) && isHasInvalidAddress(string, this.mEmailValidator)) {
                MTKToast.toast(this.mActivity, R.string.attendees_invalid_tip);
            }
            this.mModel.addAttendees(this.mAttendeesList.getText().toString(), this.mEmailValidator);
            if (this.mModel.mAttendeesList.size() > 0) {
                this.mModel.mHasAttendeeData = true;
            }
            this.mEmailValidator.setRemoveInvalid(false);
        }
        if (this.mModel.mUri == null) {
            this.mModel.mCalendarId = this.mCalendarsSpinner.getSelectedItemId();
            if (this.mCalendarsCursor.moveToPosition(this.mCalendarsSpinner.getSelectedItemPosition())) {
                String string2 = this.mCalendarsCursor.getString(2);
                Utils.setSharedPreference(this.mActivity, "preference_defaultCalendar", string2);
                this.mModel.mOwnerAccount = string2;
                this.mModel.mOrganizer = string2;
                this.mModel.mCalendarId = this.mCalendarsCursor.getLong(0);
            }
        }
        if (this.mModel.mAllDay) {
            LogUtil.v("EditEventView", "all-day event, mTimezone set to UTC");
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
            long jNormalize = this.mEndTime.normalize(true) + 86400000;
            if (jNormalize < this.mModel.mStart) {
                this.mModel.mEnd = this.mModel.mStart + 86400000;
            } else {
                this.mModel.mEnd = jNormalize;
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

    public EditEventView(Activity activity, View view, EditEventHelper.EditDoneRunnable editDoneRunnable, boolean z, boolean z2, boolean z3, int[] iArr, boolean z4) {
        Button button;
        this.mShowDatePicker = false;
        this.mSelectedDate = new int[]{1971, 1, 1};
        this.mEditEventViewExt = OpCalendarCustomizationFactoryBase.getOpFactory(activity).makeEditEventCalendar(activity);
        this.mActivity = activity;
        this.mView = view;
        this.mDone = editDoneRunnable;
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
            public void onClick(View view2) {
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
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                if (i == 6) {
                    EditEventView.this.mLocationTextView.dismissDropDown();
                    LogUtil.v("EditEventView", "click IME_ACTION_DONE in LocationTextView " + EditEventView.this.mLocationTextView.toString());
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
            public void onItemSelected(AdapterView<?> adapterView, View view2, int i, long j) {
                if (EditEventView.this.mAvailabilityCurrentlySelected == -1) {
                    EditEventView.this.mAvailabilityCurrentlySelected = i;
                }
                if (EditEventView.this.mAvailabilityCurrentlySelected == i || EditEventView.this.mAllDayChangingAvailability) {
                    EditEventView.this.mAvailabilityCurrentlySelected = i;
                    EditEventView.this.mAllDayChangingAvailability = false;
                } else {
                    EditEventView.this.mAvailabilityExplicitlySet = true;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
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
        initMultiAutoCompleteTextView((ChipsAddressTextView) this.mAttendeesList);
        IEditTextExt editTextExt = ExtensionFactory.getEditTextExt();
        editTextExt.setLengthInputFilter((EditText) this.mTitleTextView, this.mActivity, 1600);
        editTextExt.setLengthInputFilter(this.mLocationTextView, this.mActivity, 1600);
        editTextExt.setLengthInputFilter((EditText) this.mDescriptionTextView, this.mActivity, 10000);
        setModel(null);
        FragmentManager fragmentManager = activity.getFragmentManager();
        RecurrencePickerDialog recurrencePickerDialog = (RecurrencePickerDialog) fragmentManager.findFragmentByTag("recurrencePickerDialogFragment");
        if (recurrencePickerDialog != null) {
            recurrencePickerDialog.setOnRecurrenceSetListener(this);
        }
        TimeZonePickerDialog timeZonePickerDialog = (TimeZonePickerDialog) fragmentManager.findFragmentByTag("timeZonePickerDialogFragment");
        if (timeZonePickerDialog != null) {
            timeZonePickerDialog.setOnTimeZoneSetListener(this);
        }
        com.android.datetimepicker.time.TimePickerDialog timePickerDialog = (com.android.datetimepicker.time.TimePickerDialog) fragmentManager.findFragmentByTag("timePickerDialogFragment");
        if (timePickerDialog != null) {
            this.mTimeSelectedWasStartTime = z;
            if (z) {
                button = this.mStartTimeButton;
            } else {
                button = this.mEndTimeButton;
            }
            timePickerDialog.setOnTimeSetListener(new TimeListener(button));
        }
        this.mDatePickerDialog = (com.android.datetimepicker.date.DatePickerDialog) fragmentManager.findFragmentByTag("datePickerDialogFragment");
        if (z3) {
            this.mDateSelectedWasStartDate = z2;
        }
        this.mShowDatePicker = z3;
        if (iArr != null) {
            this.mSelectedDate = iArr;
        }
        this.mRestoredView = z4;
    }

    private static ArrayList<Integer> loadIntegerArray(Resources resources, int i) {
        int[] intArray = resources.getIntArray(i);
        ArrayList<Integer> arrayList = new ArrayList<>(intArray.length);
        for (int i2 : intArray) {
            arrayList.add(Integer.valueOf(i2));
        }
        return arrayList;
    }

    private static ArrayList<String> loadStringArray(Resources resources, int i) {
        return new ArrayList<>(Arrays.asList(resources.getStringArray(i)));
    }

    private void prepareAvailability() {
        Resources resources = this.mActivity.getResources();
        this.mAvailabilityValues = loadIntegerArray(resources, R.array.availability_values);
        this.mAvailabilityLabels = loadStringArray(resources, R.array.availability);
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
        CalendarEventModel calendarEventModel = this.mModel;
        Resources resources = this.mActivity.getResources();
        this.mReminderMinuteValues = loadIntegerArray(resources, R.array.reminder_minutes_values);
        this.mReminderMinuteLabels = loadStringArray(resources, R.array.reminder_minutes_labels);
        this.mReminderMethodValues = loadIntegerArray(resources, R.array.reminder_methods_values);
        this.mReminderMethodLabels = loadStringArray(resources, R.array.reminder_methods_labels);
        if (this.mModel.mCalendarAllowedReminders != null) {
            EventViewUtils.reduceMethodList(this.mReminderMethodValues, this.mReminderMethodLabels, this.mModel.mCalendarAllowedReminders);
        }
        int size = 0;
        if (calendarEventModel.mHasAlarm) {
            ArrayList<CalendarEventModel.ReminderEntry> arrayList = calendarEventModel.mReminders;
            size = arrayList.size();
            for (CalendarEventModel.ReminderEntry reminderEntry : arrayList) {
                if (this.mReminderMethodValues.contains(Integer.valueOf(reminderEntry.getMethod()))) {
                    EventViewUtils.addMinutesToList(this.mActivity, this.mReminderMinuteValues, this.mReminderMinuteLabels, reminderEntry.getMinutes());
                }
            }
            this.mUnsupportedReminders.clear();
            for (CalendarEventModel.ReminderEntry reminderEntry2 : arrayList) {
                if (this.mReminderMethodValues.contains(Integer.valueOf(reminderEntry2.getMethod())) || reminderEntry2.getMethod() == 0) {
                    EventViewUtils.addReminder(this.mActivity, this.mScrollView, this, this.mReminderItems, this.mReminderMinuteValues, this.mReminderMinuteLabels, this.mReminderMethodValues, this.mReminderMethodLabels, reminderEntry2, Integer.MAX_VALUE, null);
                } else {
                    this.mUnsupportedReminders.add(reminderEntry2);
                }
            }
        }
        updateRemindersVisibility(size);
        EventViewUtils.updateAddReminderButton(this.mView, this.mReminderItems, this.mModel.mCalendarMaxReminders);
    }

    public void setModel(CalendarEventModel calendarEventModel) {
        this.mModel = calendarEventModel;
        if (this.mAddressAdapter != null && (this.mAddressAdapter instanceof EmailAddressAdapter)) {
            ((EmailAddressAdapter) this.mAddressAdapter).close();
            this.mAddressAdapter = null;
        }
        if (calendarEventModel == null) {
            this.mLoadingMessage.setVisibility(0);
            this.mScrollView.setVisibility(8);
            return;
        }
        boolean zCanRespond = EditEventHelper.canRespond(calendarEventModel);
        long j = calendarEventModel.mStart;
        long j2 = calendarEventModel.mEnd;
        this.mTimezone = calendarEventModel.mTimezone;
        this.mStartTime.timezone = this.mTimezone;
        this.mStartTime.set(j);
        this.mStartTime.normalize(true);
        this.mEndTime.timezone = this.mTimezone;
        this.mEndTime.set(j2);
        this.mEndTime.normalize(true);
        this.mRrule = calendarEventModel.mRrule;
        if (!TextUtils.isEmpty(this.mRrule)) {
            this.mEventRecurrence.parse(this.mRrule);
        }
        if (this.mEventRecurrence.startDate == null) {
            this.mEventRecurrence.startDate = this.mStartTime;
        }
        setAttendeesGroupVisibility(calendarEventModel.mAccountType);
        if (!calendarEventModel.mHasAttendeeData) {
            this.mAttendeesGroup.setVisibility(8);
        }
        this.mAllDayCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean z) {
                EditEventView.this.setAllDayViewsVisibility(z);
            }
        });
        boolean zIsChecked = this.mAllDayCheckBox.isChecked();
        this.mAllDay = false;
        if (!calendarEventModel.mAllDay) {
            this.mAllDayCheckBox.setChecked(false);
        } else {
            this.mAllDayCheckBox.setChecked(true);
            this.mTimezone = Utils.getTimeZone(this.mActivity, null);
            this.mStartTime.timezone = this.mTimezone;
            this.mEndTime.timezone = this.mTimezone;
            this.mEndTime.normalize(true);
        }
        if (zIsChecked == this.mAllDayCheckBox.isChecked()) {
            setAllDayViewsVisibility(zIsChecked);
        }
        populateTimezone(this.mStartTime.normalize(true));
        this.mDefaultReminderMinutes = Integer.parseInt(GeneralPreferences.getSharedPreferences(this.mActivity).getString("preferences_default_reminder", "-1"));
        prepareReminders();
        prepareAvailability();
        this.mView.findViewById(R.id.reminder_add).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EditEventView.this.addReminder();
            }
        });
        if (!this.mIsMultipane) {
            this.mView.findViewById(R.id.is_all_day_label).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    EditEventView.this.mAllDayCheckBox.setChecked(!EditEventView.this.mAllDayCheckBox.isChecked());
                }
            });
        }
        if (calendarEventModel.mTitle != null) {
            this.mTitleTextView.setTextKeepState(calendarEventModel.mTitle);
        }
        if (calendarEventModel.mIsOrganizer || TextUtils.isEmpty(calendarEventModel.mOrganizer) || calendarEventModel.mOrganizer.endsWith("calendar.google.com")) {
            this.mView.findViewById(R.id.organizer_label).setVisibility(8);
            this.mView.findViewById(R.id.organizer).setVisibility(8);
            this.mOrganizerGroup.setVisibility(8);
        } else {
            ((TextView) this.mView.findViewById(R.id.organizer)).setText(calendarEventModel.mOrganizerDisplayName);
        }
        if (calendarEventModel.mLocation != null) {
            this.mLocationTextView.setTextKeepState(calendarEventModel.mLocation);
        }
        if (calendarEventModel.mDescription != null) {
            this.mDescriptionTextView.setTextKeepState(calendarEventModel.mDescription);
        }
        int iIndexOf = this.mAvailabilityValues.indexOf(Integer.valueOf(calendarEventModel.mAvailability));
        if (iIndexOf != -1) {
            this.mAvailabilitySpinner.setSelection(iIndexOf);
        }
        this.mAccessLevelSpinner.setSelection(calendarEventModel.mAccessLevel);
        View viewFindViewById = this.mView.findViewById(R.id.response_label);
        if (zCanRespond) {
            this.mResponseRadioGroup.check(EventInfoFragment.findButtonIdForResponse(calendarEventModel.mSelfAttendeeStatus));
            this.mResponseRadioGroup.setVisibility(0);
            viewFindViewById.setVisibility(0);
        } else {
            viewFindViewById.setVisibility(8);
            this.mResponseRadioGroup.setVisibility(8);
            this.mResponseGroup.setVisibility(8);
        }
        if (calendarEventModel.mUri != null) {
            this.mView.findViewById(R.id.calendar_selector_group).setVisibility(8);
            ((TextView) this.mView.findViewById(R.id.calendar_textview)).setText(calendarEventModel.mCalendarDisplayName);
            TextView textView = (TextView) this.mView.findViewById(R.id.calendar_textview_secondary);
            if (textView != null) {
                textView.setText(calendarEventModel.mOwnerAccount);
            }
        } else {
            this.mView.findViewById(R.id.calendar_group).setVisibility(8);
        }
        if (calendarEventModel.isEventColorInitialized()) {
            updateHeadlineColor(calendarEventModel, calendarEventModel.getEventColor());
        }
        if (calendarEventModel.mCalendarId != -1 && this.mCalendarsCursor != null) {
            this.mCalendarsSpinner.setSelection(findSelectedCalendarPosition(this.mCalendarsCursor, calendarEventModel.mCalendarId));
        }
        populateWhen();
        populateRepeats();
        if (!this.mRestoredView) {
            updateAttendees(calendarEventModel.mAttendeesList);
        }
        updateView();
        this.mScrollView.setVisibility(0);
        this.mLoadingMessage.setVisibility(8);
        sendAccessibilityEvent();
        setExtUI(calendarEventModel);
    }

    public void updateHeadlineColor(CalendarEventModel calendarEventModel, int i) {
        if (calendarEventModel.mUri != null) {
            if (this.mIsMultipane) {
                this.mView.findViewById(R.id.calendar_textview_with_colorpicker).setBackgroundColor(i);
                return;
            } else {
                this.mView.findViewById(R.id.calendar_group).setBackgroundColor(i);
                return;
            }
        }
        setSpinnerBackgroundColor(i);
    }

    private void setSpinnerBackgroundColor(int i) {
        if (this.mIsMultipane) {
            this.mCalendarSelectorWrapper.setBackgroundColor(i);
        } else {
            this.mCalendarSelectorGroup.setBackgroundColor(i);
        }
    }

    private void sendAccessibilityEvent() {
        AccessibilityManager accessibilityManager = (AccessibilityManager) this.mActivity.getSystemService("accessibility");
        if (!accessibilityManager.isEnabled() || this.mModel == null) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        addFieldsRecursive(sb, this.mView);
        String string = sb.toString();
        AccessibilityEvent accessibilityEventObtain = AccessibilityEvent.obtain(8);
        accessibilityEventObtain.setClassName(getClass().getName());
        accessibilityEventObtain.setPackageName(this.mActivity.getPackageName());
        accessibilityEventObtain.getText().add(string);
        accessibilityEventObtain.setAddedCount(string.length());
        accessibilityManager.sendAccessibilityEvent(accessibilityEventObtain);
    }

    private void addFieldsRecursive(StringBuilder sb, View view) {
        if (view == 0 || view.getVisibility() != 0) {
            return;
        }
        if (view instanceof TextView) {
            CharSequence text = view.getText();
            if (!TextUtils.isEmpty(text.toString().trim())) {
                sb.append(((Object) text) + ". ");
                return;
            }
            return;
        }
        if (view instanceof RadioGroup) {
            int checkedRadioButtonId = view.getCheckedRadioButtonId();
            if (checkedRadioButtonId != -1) {
                sb.append(((Object) ((RadioButton) view.findViewById(checkedRadioButtonId)).getText()) + ". ");
                return;
            }
            return;
        }
        if (!(view instanceof Spinner)) {
            if (view instanceof ViewGroup) {
                int childCount = view.getChildCount();
                for (int i = 0; i < childCount; i++) {
                    addFieldsRecursive(sb, view.getChildAt(i));
                }
                return;
            }
            return;
        }
        if (view.getSelectedItem() instanceof String) {
            String strTrim = ((String) view.getSelectedItem()).trim();
            if (!TextUtils.isEmpty(strTrim)) {
                sb.append(strTrim + ". ");
            }
        }
    }

    protected void setWhenString() {
        String str;
        int i;
        String str2 = this.mTimezone;
        if (!this.mModel.mAllDay) {
            int i2 = 17;
            if (DateFormat.is24HourFormat(this.mActivity)) {
                i2 = 145;
            }
            str = str2;
            i = i2;
        } else {
            i = 18;
            str = "UTC";
        }
        long jNormalize = this.mStartTime.normalize(true);
        long jNormalize2 = this.mEndTime.normalize(true);
        sStringBuilder.setLength(0);
        this.mWhenView.setText(DateUtils.formatDateRange(this.mActivity, sFormatter, jNormalize, jNormalize2, i, str).toString());
    }

    public void setCalendarsCursor(Cursor cursor, boolean z, long j) {
        int iFindDefaultCalendarPosition;
        this.mCalendarsCursor = cursor;
        if (cursor == null || cursor.getCount() == 0) {
            if (this.mSaveAfterQueryComplete) {
                this.mLoadingCalendarsDialog.cancel();
            }
            if (!z) {
                return;
            }
            AlertDialog.Builder builder = new AlertDialog.Builder(this.mActivity);
            builder.setTitle(R.string.no_syncable_calendars).setIconAttribute(android.R.attr.alertDialogIcon).setMessage(R.string.no_calendars_found).setPositiveButton(R.string.add_account, this).setNegativeButton(android.R.string.no, this).setOnCancelListener(this);
            this.mNoCalendarsDialog = builder.show();
            return;
        }
        if (j != -1) {
            iFindDefaultCalendarPosition = findSelectedCalendarPosition(cursor, j);
        } else {
            iFindDefaultCalendarPosition = findDefaultCalendarPosition(cursor);
        }
        this.mCalendarsSpinner.setAdapter((SpinnerAdapter) new CalendarsAdapter(this.mActivity, R.layout.calendars_spinner_item, cursor));
        this.mCalendarsSpinner.setOnItemSelectedListener(this);
        this.mCalendarsSpinner.setSelection(iFindDefaultCalendarPosition);
        if (this.mSaveAfterQueryComplete) {
            this.mLoadingCalendarsDialog.cancel();
            if (prepareForSave() && fillModelFromUI()) {
                this.mDone.setDoneCode((z ? 1 : 0) | 2);
                this.mDone.run();
            } else if (z) {
                this.mDone.setDoneCode(1);
                this.mDone.run();
            } else if (Log.isLoggable("EditEventView", 3)) {
                Log.d("EditEventView", "SetCalendarsCursor:Save failed and unable to exit view");
            }
        }
    }

    public void updateView() {
        if (this.mModel == null) {
            return;
        }
        if (EditEventHelper.canModifyEvent(this.mModel)) {
            setViewStates(this.mModification);
        } else {
            setViewStates(0);
        }
    }

    private void setViewStates(int i) {
        if (i == 0 || !EditEventHelper.canModifyEvent(this.mModel)) {
            setWhenString();
            Iterator<View> it = this.mViewOnlyList.iterator();
            while (it.hasNext()) {
                it.next().setVisibility(0);
            }
            Iterator<View> it2 = this.mEditOnlyList.iterator();
            while (it2.hasNext()) {
                it2.next().setVisibility(8);
            }
            for (View view : this.mEditViewList) {
                view.setEnabled(false);
                view.setBackgroundDrawable(null);
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
            for (View view2 : this.mEditViewList) {
                view2.setEnabled(true);
                if (view2.getTag() != null) {
                    view2.setBackgroundDrawable((Drawable) view2.getTag());
                    view2.setPadding(this.mOriginalPadding[0], this.mOriginalPadding[1], this.mOriginalPadding[2], this.mOriginalPadding[3]);
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
            populateRepeats();
            this.mRemindersGroup.setVisibility(0);
            this.mLocationGroup.setVisibility(0);
            this.mDescriptionGroup.setVisibility(0);
        }
        setAllDayViewsVisibility(this.mAllDayCheckBox.isChecked());
    }

    public void setModification(int i) {
        this.mModification = i;
        updateView();
        updateHomeTime();
    }

    private int findSelectedCalendarPosition(Cursor cursor, long j) {
        if (cursor.getCount() <= 0) {
            return -1;
        }
        int columnIndexOrThrow = cursor.getColumnIndexOrThrow("_id");
        cursor.moveToPosition(-1);
        int i = 0;
        while (cursor.moveToNext()) {
            if (cursor.getLong(columnIndexOrThrow) == j) {
                return i;
            }
            i++;
        }
        return 0;
    }

    private int findDefaultCalendarPosition(Cursor cursor) {
        if (cursor.getCount() <= 0) {
            return -1;
        }
        String sharedPreference = Utils.getSharedPreference(this.mActivity, "preference_defaultCalendar", (String) null);
        int columnIndexOrThrow = cursor.getColumnIndexOrThrow("ownerAccount");
        if (columnIndexOrThrow < 0) {
            LogUtil.w("EditEventView", "getColumnIndexOrThrow(Calendar.OWNER_ACCOUNT) failed, return 0");
            return 0;
        }
        int columnIndexOrThrow2 = cursor.getColumnIndexOrThrow("account_name");
        int columnIndexOrThrow3 = cursor.getColumnIndexOrThrow("account_type");
        cursor.moveToPosition(-1);
        int i = 0;
        while (cursor.moveToNext()) {
            String string = cursor.getString(columnIndexOrThrow);
            if (sharedPreference == null) {
                if (string != null && string.equals(cursor.getString(columnIndexOrThrow2)) && !"LOCAL".equals(cursor.getString(columnIndexOrThrow3))) {
                    return i;
                }
            } else if (sharedPreference.equals(string)) {
                return i;
            }
            i++;
        }
        return 0;
    }

    private void updateAttendees(HashMap<String, CalendarEventModel.Attendee> map) {
        if (map == null || map.isEmpty()) {
            return;
        }
        this.mAttendeesList.setText((CharSequence) null);
        StringBuffer stringBuffer = new StringBuffer();
        Iterator<CalendarEventModel.Attendee> it = map.values().iterator();
        while (it.hasNext()) {
            stringBuffer.append(it.next().getRecipientAddress() + ", ");
        }
        MTKRecipientList mTKRecipientList = new MTKRecipientList();
        LinkedHashSet<Rfc822Token> addressesFromList = EditEventHelper.getAddressesFromList(stringBuffer.toString(), this.mEmailValidator);
        LogUtil.oi("EditEventView", "updateAttendees, addresses=" + addressesFromList);
        Iterator<Rfc822Token> it2 = addressesFromList.iterator();
        while (it2.hasNext()) {
            mTKRecipientList.addRecipient("", it2.next().toString());
        }
        ((ChipsAddressTextView) this.mAttendeesList).appendList(mTKRecipientList);
    }

    private void updateRemindersVisibility(int i) {
        if (i != 0) {
            this.mRemindersContainer.setVisibility(0);
        } else {
            this.mRemindersContainer.setVisibility(8);
        }
        View viewFindViewById = this.mView.findViewById(R.id.reminder_add);
        if (i >= this.mModel.mCalendarMaxReminders) {
            viewFindViewById.setVisibility(8);
        } else {
            viewFindViewById.setVisibility(0);
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

    private MultiAutoCompleteTextView initMultiAutoCompleteTextView(ChipsAddressTextView chipsAddressTextView) {
        if (ChipsUtil.supportsChipsUi()) {
            this.mAddressAdapter = new RecipientAdapter(this.mActivity);
            chipsAddressTextView.setAdapter((BaseRecipientAdapter) this.mAddressAdapter);
            chipsAddressTextView.setOnFocusListShrinkRecipients(true);
        } else {
            this.mAddressAdapter = new EmailAddressAdapter(this.mActivity);
            chipsAddressTextView.setAdapter((EmailAddressAdapter) this.mAddressAdapter);
        }
        chipsAddressTextView.setTokenizer(new Rfc822Tokenizer());
        chipsAddressTextView.setValidator(this.mEmailValidator);
        chipsAddressTextView.setFilters(sRecipientFilters);
        return chipsAddressTextView;
    }

    private void setDate(TextView textView, long j) {
        String dateStringFromMillis;
        synchronized (TimeZone.class) {
            TimeZone.setDefault(TimeZone.getTimeZone(this.mTimezone));
            dateStringFromMillis = this.mEditEventViewExt.getDateStringFromMillis(this.mActivity, j);
            if (TextUtils.isEmpty(dateStringFromMillis)) {
                dateStringFromMillis = DateUtils.formatDateTime(this.mActivity, j, 98326);
            }
            TimeZone.setDefault(null);
        }
        textView.setText(dateStringFromMillis);
    }

    private void setTime(TextView textView, long j) {
        int i;
        String dateTime;
        if (DateFormat.is24HourFormat(this.mActivity)) {
            i = 5249;
        } else {
            i = 5121;
        }
        synchronized (TimeZone.class) {
            TimeZone.setDefault(TimeZone.getTimeZone(this.mTimezone));
            dateTime = DateUtils.formatDateTime(this.mActivity, j, i);
            TimeZone.setDefault(null);
        }
        textView.setText(dateTime);
    }

    protected void setAllDayViewsVisibility(boolean z) {
        if (z) {
            if (this.mEndTime.hour == 0 && this.mEndTime.minute == 0) {
                if (this.mAllDay != z) {
                    this.mEndTime.monthDay--;
                }
                long jNormalize = this.mEndTime.normalize(true);
                if (this.mEndTime.before(this.mStartTime)) {
                    this.mEndTime.set(this.mStartTime);
                    jNormalize = this.mEndTime.normalize(true);
                }
                setDate(this.mEndDateButton, jNormalize);
                setTime(this.mEndTimeButton, jNormalize);
            }
            this.mStartTimeButton.setVisibility(8);
            this.mEndTimeButton.setVisibility(8);
            this.mTimezoneRow.setVisibility(8);
        } else {
            if (this.mEndTime.hour == 0 && this.mEndTime.minute == 0) {
                if (this.mAllDay != z) {
                    this.mEndTime.monthDay++;
                }
                long jNormalize2 = this.mEndTime.normalize(true);
                setDate(this.mEndDateButton, jNormalize2);
                setTime(this.mEndTimeButton, jNormalize2);
            }
            this.mStartTimeButton.setVisibility(0);
            this.mEndTimeButton.setVisibility(0);
            this.mTimezoneRow.setVisibility(0);
        }
        if (this.mModel.mUri == null && !this.mAvailabilityExplicitlySet && this.mAvailabilityAdapter != null && this.mAvailabilityValues != null && this.mAvailabilityValues.contains(Integer.valueOf(z ? 1 : 0))) {
            this.mAllDayChangingAvailability = true;
            this.mAvailabilitySpinner.setSelection(this.mAvailabilityAdapter.getPosition(this.mOriginalAvailabilityLabels.get(z ? 1 : 0)));
        }
        this.mAllDay = z;
        updateHomeTime();
    }

    public void setColorPickerButtonStates(int[] iArr) {
        setColorPickerButtonStates(iArr != null && iArr.length > 0);
    }

    public void setColorPickerButtonStates(boolean z) {
        if (z) {
            this.mColorPickerNewEvent.setVisibility(0);
            this.mColorPickerExistingEvent.setVisibility(0);
        } else {
            this.mColorPickerNewEvent.setVisibility(8);
            this.mColorPickerExistingEvent.setVisibility(8);
        }
    }

    public boolean isColorPaletteVisible() {
        return this.mColorPickerNewEvent.getVisibility() == 0 || this.mColorPickerExistingEvent.getVisibility() == 0;
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long j) {
        Cursor cursor = (Cursor) adapterView.getItemAtPosition(i);
        if (cursor == null) {
            Log.w("EditEventView", "Cursor not set on calendar item");
            return;
        }
        long j2 = cursor.getLong(cursor.getColumnIndexOrThrow("_id"));
        int displayColorFromColor = Utils.getDisplayColorFromColor(cursor.getInt(cursor.getColumnIndexOrThrow("calendar_color")));
        if (j2 == this.mModel.mCalendarId && this.mModel.isCalendarColorInitialized() && displayColorFromColor == this.mModel.getCalendarColor()) {
            return;
        }
        setSpinnerBackgroundColor(displayColorFromColor);
        this.mModel.mCalendarId = j2;
        this.mModel.setCalendarColor(displayColorFromColor);
        this.mModel.mCalendarAccountName = cursor.getString(11);
        this.mModel.mCalendarAccountType = cursor.getString(12);
        this.mModel.setEventColor(this.mModel.getCalendarColor());
        setColorPickerButtonStates(this.mModel.getCalendarEventColors());
        this.mModel.mCalendarMaxReminders = cursor.getInt(cursor.getColumnIndexOrThrow("maxReminders"));
        this.mModel.mCalendarAllowedReminders = cursor.getString(cursor.getColumnIndexOrThrow("allowedReminders"));
        this.mModel.mCalendarAllowedAttendeeTypes = cursor.getString(cursor.getColumnIndexOrThrow("allowedAttendeeTypes"));
        this.mModel.mCalendarAllowedAvailability = cursor.getString(cursor.getColumnIndexOrThrow("allowedAvailability"));
        this.mModel.mReminders.clear();
        this.mModel.mReminders.addAll(this.mModel.mDefaultReminders);
        this.mModel.mHasAlarm = this.mModel.mReminders.size() != 0;
        this.mReminderItems.clear();
        ((LinearLayout) this.mScrollView.findViewById(R.id.reminder_items_container)).removeAllViews();
        prepareReminders();
        prepareAvailability();
        onAccountItemSelected(cursor);
    }

    private void updateHomeTime() {
        int i;
        int i2;
        String timeZone = Utils.getTimeZone(this.mActivity, null);
        if (!this.mAllDayCheckBox.isChecked() && !TextUtils.equals(timeZone, this.mTimezone) && this.mModification != 0) {
            boolean zIs24HourFormat = DateFormat.is24HourFormat(this.mActivity);
            if (!zIs24HourFormat) {
                i = 1;
            } else {
                i = 129;
            }
            long millis = this.mStartTime.toMillis(false);
            long millis2 = this.mEndTime.toMillis(false);
            boolean z = this.mStartTime.isDst != 0;
            boolean z2 = this.mEndTime.isDst != 0;
            String displayName = TimeZone.getTimeZone(timeZone).getDisplayName(z, 1, Locale.getDefault());
            StringBuilder sb = new StringBuilder();
            sStringBuilder.setLength(0);
            boolean z3 = z2;
            String displayName2 = displayName;
            boolean z4 = z;
            sb.append(DateUtils.formatDateRange(this.mActivity, sFormatter, millis, millis, i, timeZone));
            sb.append(" ");
            sb.append(displayName2);
            this.mStartTimeHome.setText(sb.toString());
            sStringBuilder.setLength(0);
            this.mStartDateHome.setText(DateUtils.formatDateRange(this.mActivity, sFormatter, millis, millis, 524310, timeZone).toString());
            if (z3 != z4) {
                i2 = 1;
                displayName2 = TimeZone.getTimeZone(timeZone).getDisplayName(z3, 1, Locale.getDefault());
            } else {
                i2 = 1;
            }
            int i3 = zIs24HourFormat ? 129 : i2;
            sb.setLength(0);
            sStringBuilder.setLength(0);
            sb.append(DateUtils.formatDateRange(this.mActivity, sFormatter, millis2, millis2, i3, timeZone));
            sb.append(" ");
            sb.append(displayName2);
            this.mEndTimeHome.setText(sb.toString());
            sStringBuilder.setLength(0);
            this.mEndDateHome.setText(DateUtils.formatDateRange(this.mActivity, sFormatter, millis2, millis2, 524310, timeZone).toString());
            this.mStartHomeGroup.setVisibility(0);
            this.mEndHomeGroup.setVisibility(0);
            return;
        }
        this.mStartHomeGroup.setVisibility(8);
        this.mEndHomeGroup.setVisibility(8);
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {
    }

    private void setExtUI(CalendarEventModel calendarEventModel) {
        Log.d("EditEventView", "setExtUI radioGroup = " + ((RadioGroup) this.mActivity.findViewById(R.id.switch_date_picker)) + "mExt = " + this.mEditEventViewExt);
        this.mEditEventViewExt.setDatePickerSwitchUi(this.mActivity, this.mModel, this.mStartDateButton, this.mEndDateButton, this.mTimezone, this.mStartTime, this.mEndTime);
        StringBuilder sb = new StringBuilder();
        sb.append("setExtUI mShowDatePicker = ");
        sb.append(this.mShowDatePicker);
        Log.d("EditEventView", sb.toString());
        if (this.mDatePickerDialog != null && this.mDatePickerDialog.isVisible()) {
            dismissDatePickerDialog();
            restoreDatePickerDialog();
        }
        setAttendeesGroupVisibility(calendarEventModel.mAccountType);
    }

    public void doOnResume(long j) {
        Log.d("EditEventView", "doOnResume mModel = " + this.mModel + ";mShowDatePicker = " + this.mShowDatePicker);
        if (this.mCalendarsCursor != null && this.mCalendarsCursor.getCount() == 0 && this.mNoCalendarsDialog == null) {
            setCalendarsCursor(this.mCalendarsCursor, true, j);
        }
        if (this.mShowDatePicker) {
            restoreDatePickerDialog();
        }
    }

    private void restoreDatePickerDialog() {
        Button button;
        if (this.mDateSelectedWasStartDate) {
            button = this.mStartDateButton;
        } else {
            button = this.mEndDateButton;
        }
        if (this.mSelectedDate == null) {
            return;
        }
        createDatePickerDialog(new DateListener(button), this.mSelectedDate[0], this.mSelectedDate[1], this.mSelectedDate[2]);
        if (isScreenOn()) {
            Log.d("EditEventView", "[Calendar] isScreenOn() is true: ");
            this.mShowDatePicker = false;
        } else {
            Log.d("EditEventView", "[Calendar] isScreenOn() is false: ");
        }
    }

    public void createDatePickerDialog(DateListener dateListener, int i, int i2, int i3) {
        if (this.mActivity.isResumed()) {
            this.mDatePickerDialog = this.mEditEventViewExt.createDatePickerDialog(this.mActivity, dateListener, i, i2, i3);
            Log.d("EditEventView", " createDatePickerDialog() mDatePickerDialog: " + this.mDatePickerDialog);
            this.mDatePickerDialog.setFirstDayOfWeek(Utils.getFirstDayOfWeekAsCalendar(this.mActivity));
            this.mDatePickerDialog.setYearRange(1970, 2036);
            this.mDatePickerDialog.show(this.mActivity.getFragmentManager(), "datePickerDialogFragment");
            return;
        }
        LogUtil.w("EditEventView", "can not createDatePickerDialog, for activity is not resumed.");
    }

    private boolean isHasInvalidAddress(String str, Rfc822Validator rfc822Validator) {
        LinkedHashSet linkedHashSet = new LinkedHashSet();
        Rfc822Tokenizer.tokenize(str, linkedHashSet);
        boolean z = false;
        if (rfc822Validator == null) {
            return false;
        }
        Iterator it = linkedHashSet.iterator();
        while (it.hasNext()) {
            if (!rfc822Validator.isValid(((Rfc822Token) it.next()).getAddress())) {
                z = true;
            }
        }
        return z;
    }

    private void onAccountItemSelected(Cursor cursor) {
        String string = cursor.getString(cursor.getColumnIndexOrThrow("account_type"));
        setAttendeesGroupVisibility(string);
        this.mModel.mAccountType = string;
    }

    private void setAttendeesGroupVisibility(String str) {
        if ("LOCAL".equals(str)) {
            this.mAttendeesGroup.setVisibility(8);
        } else {
            this.mAttendeesGroup.setVisibility(0);
        }
    }

    private static void recalculateEventRecurrenceRule(Time time, Time time2, Time time3, EventRecurrence eventRecurrence) {
        if (eventRecurrence.until != null) {
            Time time4 = new Time(time3);
            time4.hour = 0;
            time4.minute = 0;
            time4.second = 0;
            long jNormalize = time4.normalize(false);
            time4.parse(eventRecurrence.until);
            if (jNormalize > time4.normalize(false)) {
                Time time5 = new Time(time2);
                switch (eventRecurrence.freq) {
                    case 4:
                    case 5:
                        time5.month++;
                        break;
                    case 6:
                        time5.month += 3;
                        break;
                    case 7:
                        time5.year += 3;
                        break;
                }
                time5.switchTimezone("UTC");
                time5.normalize(false);
                eventRecurrence.until = time5.format2445();
                eventRecurrence.count = 0;
            }
        }
        switch (eventRecurrence.freq) {
            case 5:
                boolean[] zArr = new boolean[7];
                Arrays.fill(zArr, false);
                for (int i = 0; i < eventRecurrence.bydayCount; i++) {
                    zArr[EventRecurrence.day2TimeDay(eventRecurrence.byday[i])] = true;
                }
                zArr[time.weekDay] = false;
                zArr[time2.weekDay] = true;
                int i2 = 0;
                for (int i3 = 0; i3 < 7; i3++) {
                    if (zArr[i3]) {
                        i2++;
                    }
                }
                if (eventRecurrence.bydayCount < i2 || eventRecurrence.byday == null || eventRecurrence.bydayNum == null) {
                    eventRecurrence.byday = new int[i2];
                    eventRecurrence.bydayNum = new int[i2];
                }
                eventRecurrence.bydayCount = i2;
                for (int i4 = 6; i4 >= 0; i4--) {
                    if (zArr[i4]) {
                        i2--;
                        eventRecurrence.bydayNum[i2] = 0;
                        eventRecurrence.byday[i2] = EventRecurrence.timeDay2Day(i4);
                    }
                }
                break;
            case 6:
                if (eventRecurrence.bymonthCount > 0 && eventRecurrence.bydayCount > 0) {
                    LogUtil.d("EditEventView", "some error accourred, er.bymonthCount=" + eventRecurrence.bymonthCount + ", er.bydayCount=" + eventRecurrence.bydayCount);
                    eventRecurrence.bydayCount = 0;
                    eventRecurrence.byday = null;
                    eventRecurrence.bydayNum = null;
                    eventRecurrence.bymonthCount = 1;
                }
                if (eventRecurrence.bymonthCount > 0) {
                    if (eventRecurrence.bymonthday == null || eventRecurrence.bymonthdayCount < 1) {
                        eventRecurrence.bymonthday = new int[1];
                    }
                    eventRecurrence.bymonthday[0] = time2.monthDay;
                    eventRecurrence.bymonthdayCount = 1;
                } else if (eventRecurrence.bydayCount > 0) {
                    if (eventRecurrence.bydayCount < 1 || eventRecurrence.byday == null || eventRecurrence.bydayNum == null) {
                        eventRecurrence.byday = new int[1];
                        eventRecurrence.bydayNum = new int[1];
                    }
                    eventRecurrence.bydayCount = 1;
                    eventRecurrence.byday[0] = EventRecurrence.timeDay2Day(time2.weekDay);
                    eventRecurrence.bydayNum[0] = (time2.monthDay + 6) / 7;
                }
                break;
        }
    }

    private static String getDefaultRruleByFrequecy(int i, int i2) {
        switch (i) {
            case 4:
                return "FREQ=DAILY;WKST=SU";
            case 5:
                return "FREQ=WEEKLY;WKST=SU;BYDAY=" + weekday2String(i2);
            case 6:
                return "FREQ=MONTHLY;WKST=SU";
            case 7:
                return "FREQ=YEARLY;WKST=SU";
            default:
                Log.w("EditEventView", "bad repeat frequecy  argument: " + i);
                return "FREQ=DAILY;WKST=SU";
        }
    }

    private static String weekday2String(int i) {
        switch (i) {
            case 0:
                return "SU";
            case 1:
                return "MO";
            case 2:
                return "TU";
            case 3:
                return "WE";
            case 4:
                return "TH";
            case 5:
                return "FR";
            case 6:
                return "SA";
            default:
                Log.w("EditEventView", "bad weekday argument: " + i);
                return "SU";
        }
    }

    private long checkDateRange(Time time) {
        if (time.year < 1970) {
            time.set(time.second, time.minute, time.hour, 1, 0, 1970);
        }
        if (time.year > 2036) {
            time.set(time.second, time.minute, time.hour, 31, 11, 2036);
        }
        time.normalize(true);
        return time.toMillis(true);
    }

    public void dismissDatePickerDialog() {
        LogUtil.oi("EditEventView", "dismissDatePickerDialog status= " + this.mActivity.isResumed());
        if (this.mDatePickerDialog != null) {
            LogUtil.oi("EditEventView", "do dismiss DatePickerDialog." + this.mDatePickerDialog);
            MonthAdapter.CalendarDay selectedDay = this.mDatePickerDialog.getSelectedDay();
            this.mSelectedDate[0] = selectedDay.getYear();
            this.mSelectedDate[1] = selectedDay.getMonth();
            this.mSelectedDate[2] = selectedDay.getDay();
            this.mDatePickerDialog.dismiss();
            this.mShowDatePicker = true;
        }
    }

    private void hideInputMethod() {
        InputMethodManager inputMethodManager = (InputMethodManager) this.mActivity.getSystemService("input_method");
        View currentFocus = this.mActivity.getCurrentFocus();
        if (currentFocus != null) {
            inputMethodManager.hideSoftInputFromWindow(currentFocus.getWindowToken(), 0);
        }
    }

    public boolean isScreenOn() {
        if (!((PowerManager) this.mActivity.getSystemService("power")).isScreenOn()) {
            Log.d("EditEventView", "[Calendar] get powerManager service pm.isScreenOn() false: ");
            return false;
        }
        if (!this.mActivity.isResumed()) {
            Log.d("EditEventView", "[Calendar] get powerManager service mActivity.isResumed  false: ");
            return false;
        }
        Log.d("EditEventView", "[Calendar], returning true from isScreenOn() ");
        return true;
    }
}
