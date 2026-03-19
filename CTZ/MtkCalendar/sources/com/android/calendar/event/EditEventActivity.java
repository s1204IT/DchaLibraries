package com.android.calendar.event;

import android.app.FragmentTransaction;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.format.Time;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;
import com.android.calendar.AbstractCalendarActivity;
import com.android.calendar.CalendarController;
import com.android.calendar.CalendarEventModel;
import com.android.calendar.R;
import com.android.calendar.Utils;
import java.util.ArrayList;

public class EditEventActivity extends AbstractCalendarActivity {
    private static boolean mIsMultipane;
    private EditEventFragment mEditFragment;
    private int mEventColor;
    private boolean mEventColorInitialized;
    private CalendarController.EventInfo mEventInfo;
    private ArrayList<CalendarEventModel.ReminderEntry> mReminders;
    private static final String[] STORAGE_PERMISSION = {"android.permission.READ_EXTERNAL_STORAGE"};
    private static final String[] CALENDAR_PERMISSION = {"android.permission.READ_CALENDAR", "android.permission.WRITE_CALENDAR"};
    private static final String[] CONTACTS_PERMISSION = {"android.permission.READ_CONTACTS"};

    @Override
    protected void onCreate(Bundle bundle) {
        Log.d("EditEventActivity", "[Calendar] onCreate() of EditEventActivity");
        super.onCreate(bundle);
        setContentView(R.layout.simple_frame_layout);
        this.mEventInfo = getEventInfoFromIntent(bundle);
        this.mReminders = getReminderEntriesFromIntent();
        this.mEventColorInitialized = getIntent().hasExtra("event_color");
        this.mEventColor = getIntent().getIntExtra("event_color", -1);
        this.mEditFragment = (EditEventFragment) getFragmentManager().findFragmentById(R.id.main_frame);
        mIsMultipane = Utils.getConfigBool(this, R.bool.multiple_pane_config);
        if (mIsMultipane) {
            getActionBar().setDisplayOptions(8, 14);
            getActionBar().setTitle(this.mEventInfo.id == -1 ? R.string.event_create : R.string.event_edit);
        } else {
            getActionBar().setDisplayOptions(16, 30);
        }
        if (this.mEditFragment == null) {
            Intent intent = null;
            if (this.mEventInfo.id == -1) {
                intent = getIntent();
            }
            this.mEditFragment = new EditEventFragment(this.mEventInfo, this.mReminders, this.mEventColorInitialized, this.mEventColor, false, intent);
            this.mEditFragment.mShowModifyDialogOnLaunch = getIntent().getBooleanExtra("editMode", false);
            FragmentTransaction fragmentTransactionBeginTransaction = getFragmentManager().beginTransaction();
            fragmentTransactionBeginTransaction.replace(R.id.main_frame, this.mEditFragment);
            fragmentTransactionBeginTransaction.show(this.mEditFragment);
            fragmentTransactionBeginTransaction.commit();
        }
    }

    private ArrayList<CalendarEventModel.ReminderEntry> getReminderEntriesFromIntent() {
        return (ArrayList) getIntent().getSerializableExtra("reminders");
    }

    private CalendarController.EventInfo getEventInfoFromIntent(Bundle bundle) {
        long j;
        CalendarController.EventInfo eventInfo = new CalendarController.EventInfo();
        Intent intent = getIntent();
        Uri data = intent.getData();
        if (data != null) {
            try {
                j = Long.parseLong(data.getLastPathSegment());
            } catch (NumberFormatException e) {
                j = -1;
            }
        } else if (bundle != null && bundle.containsKey("key_event_id")) {
            j = bundle.getLong("key_event_id");
        } else {
            j = -1;
        }
        boolean booleanExtra = intent.getBooleanExtra("allDay", false);
        long longExtra = intent.getLongExtra("beginTime", -1L);
        long longExtra2 = intent.getLongExtra("endTime", -1L);
        if (longExtra2 != -1) {
            eventInfo.endTime = new Time();
            if (booleanExtra) {
                eventInfo.endTime.timezone = "UTC";
            }
            eventInfo.endTime.set(longExtra2);
        }
        if (longExtra != -1) {
            eventInfo.startTime = new Time();
            if (booleanExtra) {
                eventInfo.startTime.timezone = "UTC";
            }
            eventInfo.startTime.set(longExtra);
        }
        eventInfo.id = j;
        eventInfo.eventTitle = intent.getStringExtra("title");
        eventInfo.calendarId = intent.getLongExtra("calendar_id", -1L);
        if (booleanExtra) {
            eventInfo.extraLong = 16L;
        } else {
            eventInfo.extraLong = 0L;
        }
        return eventInfo;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() == 16908332) {
            Utils.returnToCalendarHome(this);
            return true;
        }
        return super.onOptionsItemSelected(menuItem);
    }

    @Override
    protected void onDestroy() {
        Log.d("EditEventActivity", "[Calendar] onDestroy() of EditEventActivity");
        super.onDestroy();
        CalendarController.removeInstance(this);
    }

    protected boolean hasRequiredPermission(String[] strArr) {
        for (String str : strArr) {
            if (checkSelfPermission(str) != 0) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected void onResume() {
        Log.d("EditEventActivity", "[Calendar] onResume() of EditEventActivity");
        super.onResume();
        if (!hasRequiredPermission(CALENDAR_PERMISSION) || !hasRequiredPermission(STORAGE_PERMISSION) || !hasRequiredPermission(CONTACTS_PERMISSION)) {
            Toast.makeText(getApplicationContext(), getResources().getString(R.string.denied_required_permission), 1).show();
            this.mEditFragment.mView.mAttendeesList.setText((CharSequence) null);
            finish();
        }
    }
}
