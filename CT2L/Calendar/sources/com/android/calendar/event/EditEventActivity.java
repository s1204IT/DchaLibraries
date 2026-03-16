package com.android.calendar.event;

import android.app.FragmentTransaction;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.format.Time;
import android.view.MenuItem;
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

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.simple_frame_layout);
        this.mEventInfo = getEventInfoFromIntent(icicle);
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
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.replace(R.id.main_frame, this.mEditFragment);
            ft.show(this.mEditFragment);
            ft.commit();
        }
    }

    private ArrayList<CalendarEventModel.ReminderEntry> getReminderEntriesFromIntent() {
        Intent intent = getIntent();
        return (ArrayList) intent.getSerializableExtra("reminders");
    }

    private CalendarController.EventInfo getEventInfoFromIntent(Bundle icicle) {
        CalendarController.EventInfo info = new CalendarController.EventInfo();
        long eventId = -1;
        Intent intent = getIntent();
        Uri data = intent.getData();
        if (data != null) {
            try {
                eventId = Long.parseLong(data.getLastPathSegment());
            } catch (NumberFormatException e) {
            }
        } else if (icicle != null && icicle.containsKey("key_event_id")) {
            eventId = icicle.getLong("key_event_id");
        }
        boolean allDay = intent.getBooleanExtra("allDay", false);
        long begin = intent.getLongExtra("beginTime", -1L);
        long end = intent.getLongExtra("endTime", -1L);
        if (end != -1) {
            info.endTime = new Time();
            if (allDay) {
                info.endTime.timezone = "UTC";
            }
            info.endTime.set(end);
        }
        if (begin != -1) {
            info.startTime = new Time();
            if (allDay) {
                info.startTime.timezone = "UTC";
            }
            info.startTime.set(begin);
        }
        info.id = eventId;
        info.eventTitle = intent.getStringExtra("title");
        info.calendarId = intent.getLongExtra("calendar_id", -1L);
        if (allDay) {
            info.extraLong = 16L;
        } else {
            info.extraLong = 0L;
        }
        return info;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() != 16908332) {
            return super.onOptionsItemSelected(item);
        }
        Utils.returnToCalendarHome(this);
        return true;
    }
}
