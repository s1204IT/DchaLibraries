package com.mediatek.calendar.clearevents;

import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;
import com.android.calendar.AbstractCalendarActivity;
import com.android.calendar.CalendarController;
import com.android.calendar.R;
import com.android.calendar.Utils;
import com.android.calendar.selectcalendars.SelectSyncedCalendarsMultiAccountActivity;

public class SelectClearableCalendarsActivity extends AbstractCalendarActivity {
    private SelectClearableCalendarsFragment mFragment;
    private static final String[] STORAGE_PERMISSION = {"android.permission.READ_EXTERNAL_STORAGE"};
    private static final String[] CALENDAR_PERMISSION = {"android.permission.READ_CALENDAR", "android.permission.WRITE_CALENDAR"};
    private static final String[] CONTACTS_PERMISSION = {"android.permission.READ_CONTACTS"};

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.simple_frame_layout);
        this.mFragment = (SelectClearableCalendarsFragment) getFragmentManager().findFragmentById(R.id.main_frame);
        if (this.mFragment == null) {
            this.mFragment = new SelectClearableCalendarsFragment(R.layout.calendar_sync_item);
            FragmentTransaction fragmentTransactionBeginTransaction = getFragmentManager().beginTransaction();
            fragmentTransactionBeginTransaction.replace(R.id.main_frame, this.mFragment);
            fragmentTransactionBeginTransaction.show(this.mFragment);
            fragmentTransactionBeginTransaction.commit();
        }
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
        if (16908332 == menuItem.getItemId()) {
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
        super.onResume();
        if (!hasRequiredPermission(CALENDAR_PERMISSION) || !hasRequiredPermission(STORAGE_PERMISSION) || !hasRequiredPermission(CONTACTS_PERMISSION)) {
            Toast.makeText(getApplicationContext(), getResources().getString(R.string.denied_required_permission), 1).show();
            finish();
        }
    }
}
