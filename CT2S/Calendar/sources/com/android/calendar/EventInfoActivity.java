package com.android.calendar;

import android.app.ActionBar;
import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.CalendarContract;
import android.util.Log;
import android.widget.Toast;
import com.android.calendar.CalendarEventModel;
import java.util.ArrayList;
import java.util.List;

public class EventInfoActivity extends Activity {
    private long mEndMillis;
    private long mEventId;
    private EventInfoFragment mInfoFragment;
    private final ContentObserver mObserver = new ContentObserver(new Handler()) {
        @Override
        public boolean deliverSelfNotifications() {
            return false;
        }

        @Override
        public void onChange(boolean selfChange) {
            if (!selfChange && EventInfoActivity.this.mInfoFragment != null) {
                EventInfoActivity.this.mInfoFragment.reloadEvents();
            }
        }
    };
    private long mStartMillis;

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        Intent intent = getIntent();
        int attendeeResponse = 0;
        this.mEventId = -1L;
        boolean isDialog = false;
        ArrayList<CalendarEventModel.ReminderEntry> reminders = null;
        if (icicle != null) {
            this.mEventId = icicle.getLong("key_event_id");
            this.mStartMillis = icicle.getLong("key_start_millis");
            this.mEndMillis = icicle.getLong("key_end_millis");
            attendeeResponse = icicle.getInt("key_attendee_response");
            isDialog = icicle.getBoolean("key_fragment_is_dialog");
            reminders = Utils.readRemindersFromBundle(icicle);
        } else if (intent != null && "android.intent.action.VIEW".equals(intent.getAction())) {
            this.mStartMillis = intent.getLongExtra("beginTime", 0L);
            this.mEndMillis = intent.getLongExtra("endTime", 0L);
            attendeeResponse = intent.getIntExtra("attendeeStatus", 0);
            Uri data = intent.getData();
            if (data != null) {
                try {
                    List<String> pathSegments = data.getPathSegments();
                    int size = pathSegments.size();
                    if (size > 2 && "EventTime".equals(pathSegments.get(2))) {
                        this.mEventId = Long.parseLong(pathSegments.get(1));
                        if (size > 4) {
                            this.mStartMillis = Long.parseLong(pathSegments.get(3));
                            this.mEndMillis = Long.parseLong(pathSegments.get(4));
                        }
                    } else {
                        this.mEventId = Long.parseLong(data.getLastPathSegment());
                    }
                } catch (NumberFormatException e) {
                    if (this.mEventId != -1 && (this.mStartMillis == 0 || this.mEndMillis == 0)) {
                        this.mStartMillis = 0L;
                        this.mEndMillis = 0L;
                    }
                }
            }
        }
        if (this.mEventId == -1) {
            Log.w("EventInfoActivity", "No event id");
            Toast.makeText(this, R.string.event_not_found, 0).show();
            finish();
        }
        Resources res = getResources();
        if (!res.getBoolean(R.bool.agenda_show_event_info_full_screen) && !res.getBoolean(R.bool.show_event_info_full_screen)) {
            CalendarController.getInstance(this).launchViewEvent(this.mEventId, this.mStartMillis, this.mEndMillis, attendeeResponse);
            finish();
            return;
        }
        setContentView(R.layout.simple_frame_layout);
        this.mInfoFragment = (EventInfoFragment) getFragmentManager().findFragmentById(R.id.main_frame);
        ActionBar bar = getActionBar();
        if (bar != null) {
            bar.setDisplayOptions(6);
        }
        if (this.mInfoFragment == null) {
            FragmentManager fragmentManager = getFragmentManager();
            FragmentTransaction ft = fragmentManager.beginTransaction();
            this.mInfoFragment = new EventInfoFragment(this, this.mEventId, this.mStartMillis, this.mEndMillis, attendeeResponse, isDialog, isDialog ? 1 : 0, reminders);
            ft.replace(R.id.main_frame, this.mInfoFragment);
            ft.commit();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        getContentResolver().registerContentObserver(CalendarContract.Events.CONTENT_URI, true, this.mObserver);
    }

    @Override
    protected void onPause() {
        super.onPause();
        getContentResolver().unregisterContentObserver(this.mObserver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
