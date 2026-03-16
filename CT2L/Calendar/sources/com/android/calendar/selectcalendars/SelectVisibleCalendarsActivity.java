package com.android.calendar.selectcalendars;

import android.app.FragmentTransaction;
import android.content.Intent;
import android.database.ContentObserver;
import android.os.Bundle;
import android.os.Handler;
import android.provider.CalendarContract;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import com.android.calendar.AbstractCalendarActivity;
import com.android.calendar.CalendarController;
import com.android.calendar.R;
import com.android.calendar.Utils;

public class SelectVisibleCalendarsActivity extends AbstractCalendarActivity {
    private CalendarController mController;
    private SelectVisibleCalendarsFragment mFragment;
    private final ContentObserver mObserver = new ContentObserver(new Handler()) {
        @Override
        public boolean deliverSelfNotifications() {
            return true;
        }

        @Override
        public void onChange(boolean selfChange) {
            SelectVisibleCalendarsActivity.this.mController.sendEvent(this, 128L, null, null, -1L, 0);
        }
    };

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.simple_frame_layout);
        this.mController = CalendarController.getInstance(this);
        this.mFragment = (SelectVisibleCalendarsFragment) getFragmentManager().findFragmentById(R.id.main_frame);
        if (this.mFragment == null) {
            this.mFragment = new SelectVisibleCalendarsFragment(R.layout.calendar_sync_item);
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.replace(R.id.main_frame, this.mFragment);
            ft.show(this.mFragment);
            ft.commit();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        getContentResolver().registerContentObserver(CalendarContract.Events.CONTENT_URI, true, this.mObserver);
    }

    @Override
    public void onPause() {
        super.onPause();
        getContentResolver().unregisterContentObserver(this.mObserver);
    }

    public void handleSelectSyncedCalendarsClicked(View v) {
        Intent intent = new Intent("android.intent.action.VIEW");
        intent.setClass(this, SelectSyncedCalendarsMultiAccountActivity.class);
        intent.setFlags(537001984);
        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getActionBar().setDisplayOptions(4, 4);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                Utils.returnToCalendarHome(this);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
