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
        public void onChange(boolean z) {
            SelectVisibleCalendarsActivity.this.mController.sendEvent(this, 128L, null, null, -1L, 0);
        }
    };

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.simple_frame_layout);
        this.mController = CalendarController.getInstance(this);
        this.mFragment = (SelectVisibleCalendarsFragment) getFragmentManager().findFragmentById(R.id.main_frame);
        if (this.mFragment == null) {
            this.mFragment = new SelectVisibleCalendarsFragment(R.layout.calendar_sync_item);
            FragmentTransaction fragmentTransactionBeginTransaction = getFragmentManager().beginTransaction();
            fragmentTransactionBeginTransaction.replace(R.id.main_frame, this.mFragment);
            fragmentTransactionBeginTransaction.show(this.mFragment);
            fragmentTransactionBeginTransaction.commit();
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

    public void handleSelectSyncedCalendarsClicked(View view) {
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
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() == 16908332) {
            Utils.returnToCalendarHome(this);
            return true;
        }
        return super.onOptionsItemSelected(menuItem);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        CalendarController.removeInstance(this);
    }
}
