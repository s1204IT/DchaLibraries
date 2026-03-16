package com.android.calendar.event;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.BenesseExtension;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.android.calendar.AsyncQueryService;
import com.android.calendar.CalendarController;
import com.android.calendar.CalendarEventModel;
import com.android.calendar.R;
import com.android.calendar.Utils;

public class CreateEventDialogFragment extends DialogFragment implements TextWatcher {
    private TextView mAccountName;
    private AlertDialog mAlertDialog;
    private Button mButtonAddEvent;
    private long mCalendarId = -1;
    private TextView mCalendarName;
    private String mCalendarOwner;
    private View mColor;
    private CalendarController mController;
    private TextView mDate;
    private long mDateInMillis;
    private String mDateString;
    private EditEventHelper mEditEventHelper;
    private EditText mEventTitle;
    private CalendarEventModel mModel;
    private CalendarQueryService mService;

    private class CalendarQueryService extends AsyncQueryService {
        public CalendarQueryService(Context context) {
            super(context);
        }

        @Override
        public void onQueryComplete(int token, Object cookie, Cursor cursor) {
            CreateEventDialogFragment.this.setDefaultCalendarView(cursor);
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public CreateEventDialogFragment() {
    }

    public CreateEventDialogFragment(Time day) {
        setDay(day);
    }

    public void setDay(Time day) {
        this.mDateString = day.format("%a, %b %d, %Y");
        this.mDateInMillis = day.toMillis(true);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            this.mDateString = savedInstanceState.getString("date_string");
            this.mDateInMillis = savedInstanceState.getLong("date_in_millis");
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Activity activity = getActivity();
        LayoutInflater layoutInflater = (LayoutInflater) activity.getSystemService("layout_inflater");
        View view = layoutInflater.inflate(R.layout.create_event_dialog, (ViewGroup) null);
        this.mColor = view.findViewById(R.id.color);
        this.mCalendarName = (TextView) view.findViewById(R.id.calendar_name);
        this.mAccountName = (TextView) view.findViewById(R.id.account_name);
        this.mEventTitle = (EditText) view.findViewById(R.id.event_title);
        this.mEventTitle.addTextChangedListener(this);
        this.mDate = (TextView) view.findViewById(R.id.event_day);
        if (this.mDateString != null) {
            this.mDate.setText(this.mDateString);
        }
        this.mAlertDialog = new AlertDialog.Builder(activity).setTitle(R.string.new_event_dialog_label).setView(view).setPositiveButton(R.string.create_event_dialog_save, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                CreateEventDialogFragment.this.createAllDayEvent();
                CreateEventDialogFragment.this.dismiss();
            }
        }).setNeutralButton(R.string.edit_label, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                CreateEventDialogFragment.this.mController.sendEventRelatedEventWithExtraWithTitleWithCalendarId(this, 1L, -1L, CreateEventDialogFragment.this.mDateInMillis, CreateEventDialogFragment.this.mDateInMillis + 86400000, 0, 0, 16L, -1L, CreateEventDialogFragment.this.mEventTitle.getText().toString(), CreateEventDialogFragment.this.mCalendarId);
                CreateEventDialogFragment.this.dismiss();
            }
        }).setNegativeButton(android.R.string.cancel, (DialogInterface.OnClickListener) null).create();
        return this.mAlertDialog;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (this.mButtonAddEvent == null) {
            this.mButtonAddEvent = this.mAlertDialog.getButton(-1);
            this.mButtonAddEvent.setEnabled(this.mEventTitle.getText().toString().length() > 0);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("date_string", this.mDateString);
        outState.putLong("date_in_millis", this.mDateInMillis);
    }

    @Override
    public void onActivityCreated(Bundle args) {
        super.onActivityCreated(args);
        Context context = getActivity();
        this.mController = CalendarController.getInstance(getActivity());
        this.mEditEventHelper = new EditEventHelper(context);
        this.mModel = new CalendarEventModel(context);
        this.mService = new CalendarQueryService(context);
        this.mService.startQuery(8, null, CalendarContract.Calendars.CONTENT_URI, EditEventHelper.CALENDARS_PROJECTION, "calendar_access_level>=500 AND visible=1", null, null);
    }

    private void createAllDayEvent() {
        this.mModel.mStart = this.mDateInMillis;
        this.mModel.mEnd = this.mDateInMillis + 86400000;
        this.mModel.mTitle = this.mEventTitle.getText().toString();
        this.mModel.mAllDay = true;
        this.mModel.mCalendarId = this.mCalendarId;
        this.mModel.mOwnerAccount = this.mCalendarOwner;
        if (this.mEditEventHelper.saveEvent(this.mModel, null, 0)) {
            Toast.makeText(getActivity(), R.string.creating_event, 0).show();
        }
    }

    @Override
    public void afterTextChanged(Editable s) {
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        if (this.mButtonAddEvent != null) {
            this.mButtonAddEvent.setEnabled(s.length() > 0);
        }
    }

    private void setDefaultCalendarView(Cursor cursor) {
        if (cursor == null || cursor.getCount() == 0) {
            dismiss();
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(R.string.no_syncable_calendars).setIconAttribute(android.R.attr.alertDialogIcon).setMessage(R.string.no_calendars_found).setPositiveButton(R.string.add_account, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Activity activity = CreateEventDialogFragment.this.getActivity();
                    if (activity != null && BenesseExtension.getDchaState() == 0) {
                        Intent nextIntent = new Intent("android.settings.ADD_ACCOUNT_SETTINGS");
                        String[] array = {"com.android.calendar"};
                        nextIntent.putExtra("authorities", array);
                        nextIntent.addFlags(335544320);
                        activity.startActivity(nextIntent);
                    }
                }
            }).setNegativeButton(android.R.string.no, (DialogInterface.OnClickListener) null);
            builder.show();
            return;
        }
        String defaultCalendar = null;
        Activity activity = getActivity();
        if (activity != null) {
            defaultCalendar = Utils.getSharedPreference(activity, "preference_defaultCalendar", (String) null);
        } else {
            Log.e("CreateEventDialogFragment", "Activity is null, cannot load default calendar");
        }
        int calendarOwnerIndex = cursor.getColumnIndexOrThrow("ownerAccount");
        int accountNameIndex = cursor.getColumnIndexOrThrow("account_name");
        int accountTypeIndex = cursor.getColumnIndexOrThrow("account_type");
        cursor.moveToPosition(-1);
        while (cursor.moveToNext()) {
            String calendarOwner = cursor.getString(calendarOwnerIndex);
            if (defaultCalendar == null) {
                if (calendarOwner != null && calendarOwner.equals(cursor.getString(accountNameIndex)) && !"LOCAL".equals(cursor.getString(accountTypeIndex))) {
                    setCalendarFields(cursor);
                    return;
                }
            } else if (defaultCalendar.equals(calendarOwner)) {
                setCalendarFields(cursor);
                return;
            }
        }
        cursor.moveToFirst();
        setCalendarFields(cursor);
    }

    private void setCalendarFields(Cursor cursor) {
        int calendarIdIndex = cursor.getColumnIndexOrThrow("_id");
        int colorIndex = cursor.getColumnIndexOrThrow("calendar_color");
        int calendarNameIndex = cursor.getColumnIndexOrThrow("calendar_displayName");
        int accountNameIndex = cursor.getColumnIndexOrThrow("account_name");
        int calendarOwnerIndex = cursor.getColumnIndexOrThrow("ownerAccount");
        this.mCalendarId = cursor.getLong(calendarIdIndex);
        this.mCalendarOwner = cursor.getString(calendarOwnerIndex);
        this.mColor.setBackgroundColor(Utils.getDisplayColorFromColor(cursor.getInt(colorIndex)));
        String accountName = cursor.getString(accountNameIndex);
        String calendarName = cursor.getString(calendarNameIndex);
        this.mCalendarName.setText(calendarName);
        if (calendarName.equals(accountName)) {
            this.mAccountName.setVisibility(8);
        } else {
            this.mAccountName.setVisibility(0);
            this.mAccountName.setText(accountName);
        }
    }
}
