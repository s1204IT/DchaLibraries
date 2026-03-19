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
import com.mediatek.calendar.MTKUtils;

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
        public void onQueryComplete(int i, Object obj, Cursor cursor) {
            CreateEventDialogFragment.this.setDefaultCalendarView(cursor);
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public CreateEventDialogFragment() {
    }

    public CreateEventDialogFragment(Time time) {
        setDay(time);
    }

    public void setDay(Time time) {
        this.mDateString = time.format("%a, %b %d, %Y");
        this.mDateInMillis = time.toMillis(true);
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        if (bundle != null) {
            this.mDateString = bundle.getString("date_string");
            this.mDateInMillis = bundle.getLong("date_in_millis");
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle bundle) {
        Activity activity = getActivity();
        View viewInflate = ((LayoutInflater) activity.getSystemService("layout_inflater")).inflate(R.layout.create_event_dialog, (ViewGroup) null);
        this.mColor = viewInflate.findViewById(R.id.color);
        this.mCalendarName = (TextView) viewInflate.findViewById(R.id.calendar_name);
        this.mAccountName = (TextView) viewInflate.findViewById(R.id.account_name);
        this.mEventTitle = (EditText) viewInflate.findViewById(R.id.event_title);
        this.mEventTitle.addTextChangedListener(this);
        this.mDate = (TextView) viewInflate.findViewById(R.id.event_day);
        if (this.mDateString != null) {
            this.mDate.setText(this.mDateString);
        }
        this.mAlertDialog = new AlertDialog.Builder(activity).setTitle(R.string.new_event_dialog_label).setView(viewInflate).setPositiveButton(R.string.create_event_dialog_save, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                CreateEventDialogFragment.this.createAllDayEvent();
                CreateEventDialogFragment.this.dismiss();
            }
        }).setNeutralButton(R.string.edit_label, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
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
            this.mButtonAddEvent.setEnabled(this.mEventTitle.getText().toString().trim().length() > 0);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        bundle.putString("date_string", this.mDateString);
        bundle.putLong("date_in_millis", this.mDateInMillis);
    }

    @Override
    public void onActivityCreated(Bundle bundle) {
        super.onActivityCreated(bundle);
        Activity activity = getActivity();
        this.mController = CalendarController.getInstance(getActivity());
        this.mEditEventHelper = new EditEventHelper(activity);
        this.mModel = new CalendarEventModel(activity);
        this.mService = new CalendarQueryService(activity);
        this.mService.startQuery(8, null, CalendarContract.Calendars.CONTENT_URI, EditEventHelper.CALENDARS_PROJECTION, "calendar_access_level>=500 AND visible=1", null, null);
    }

    private void createAllDayEvent() {
        this.mModel.mStart = this.mDateInMillis;
        this.mModel.mEnd = this.mDateInMillis + 86400000;
        this.mModel.mTitle = this.mEventTitle.getText().toString();
        this.mModel.mAllDay = true;
        this.mModel.mCalendarId = this.mCalendarId;
        this.mModel.mOwnerAccount = this.mCalendarOwner;
        if (MTKUtils.isLowStorage(getActivity())) {
            MTKUtils.toastLowStorage(getActivity());
        } else if (this.mEditEventHelper.saveEvent(this.mModel, null, 0)) {
            Toast.makeText(getActivity(), R.string.creating_event, 0).show();
        }
    }

    @Override
    public void afterTextChanged(Editable editable) {
    }

    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
    }

    @Override
    public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
        if (this.mButtonAddEvent != null) {
            this.mButtonAddEvent.setEnabled(charSequence.toString().trim().length() > 0);
        }
    }

    private void setDefaultCalendarView(Cursor cursor) {
        String sharedPreference = null;
        if (cursor == null || cursor.getCount() == 0) {
            dismiss();
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(R.string.no_syncable_calendars).setIconAttribute(android.R.attr.alertDialogIcon).setMessage(R.string.no_calendars_found).setPositiveButton(R.string.add_account, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    Activity activity;
                    if (BenesseExtension.getDchaState() == 0 && (activity = CreateEventDialogFragment.this.getActivity()) != null) {
                        Intent intent = new Intent("android.settings.ADD_ACCOUNT_SETTINGS");
                        intent.putExtra("authorities", new String[]{"com.android.calendar"});
                        intent.addFlags(335544320);
                        activity.startActivity(intent);
                    }
                }
            }).setNegativeButton(android.R.string.no, (DialogInterface.OnClickListener) null);
            builder.show();
            return;
        }
        Activity activity = getActivity();
        if (activity != null) {
            sharedPreference = Utils.getSharedPreference(activity, "preference_defaultCalendar", (String) null);
        } else {
            Log.e("CreateEventDialogFragment", "Activity is null, cannot load default calendar");
        }
        int columnIndexOrThrow = cursor.getColumnIndexOrThrow("ownerAccount");
        int columnIndexOrThrow2 = cursor.getColumnIndexOrThrow("account_name");
        int columnIndexOrThrow3 = cursor.getColumnIndexOrThrow("account_type");
        cursor.moveToPosition(-1);
        while (cursor.moveToNext()) {
            String string = cursor.getString(columnIndexOrThrow);
            if (sharedPreference == null) {
                if (string != null && string.equals(cursor.getString(columnIndexOrThrow2)) && !"LOCAL".equals(cursor.getString(columnIndexOrThrow3))) {
                    setCalendarFields(cursor);
                    return;
                }
            } else if (sharedPreference.equals(string)) {
                setCalendarFields(cursor);
                return;
            }
        }
        cursor.moveToFirst();
        setCalendarFields(cursor);
    }

    private void setCalendarFields(Cursor cursor) {
        int columnIndexOrThrow = cursor.getColumnIndexOrThrow("_id");
        int columnIndexOrThrow2 = cursor.getColumnIndexOrThrow("calendar_color");
        int columnIndexOrThrow3 = cursor.getColumnIndexOrThrow("calendar_displayName");
        int columnIndexOrThrow4 = cursor.getColumnIndexOrThrow("account_name");
        int columnIndexOrThrow5 = cursor.getColumnIndexOrThrow("ownerAccount");
        this.mCalendarId = cursor.getLong(columnIndexOrThrow);
        this.mCalendarOwner = cursor.getString(columnIndexOrThrow5);
        this.mColor.setBackgroundColor(Utils.getDisplayColorFromColor(cursor.getInt(columnIndexOrThrow2)));
        String string = cursor.getString(columnIndexOrThrow4);
        String string2 = cursor.getString(columnIndexOrThrow3);
        this.mCalendarName.setText(string2);
        if (string2.equals(string)) {
            this.mAccountName.setVisibility(8);
        } else {
            this.mAccountName.setVisibility(0);
            this.mAccountName.setText(string);
        }
    }
}
