package com.android.calendar;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.CalendarContract;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;
import com.android.calendar.CalendarEventModel;
import com.android.calendar.widget.CalendarAppWidgetService;
import com.mediatek.calendar.nfc.NfcHandler;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class EventInfoActivity extends Activity {
    private Bundle mBundleIcicleOncreate;
    private long mEndMillis;
    private long mEventId;
    private EventInfoFragment mInfoFragment;
    private final ContentObserver mObserver = new ContentObserver(new Handler()) {
        @Override
        public boolean deliverSelfNotifications() {
            return false;
        }

        @Override
        public void onChange(boolean z) {
            if (!z && EventInfoActivity.this.mInfoFragment != null) {
                EventInfoActivity.this.mInfoFragment.reloadEvents();
            }
        }
    };
    private int mOnCreateRequestPermissionFlag;
    private long mStartMillis;
    private static final String[] STORAGE_PERMISSION = {"android.permission.READ_EXTERNAL_STORAGE"};
    private static final String[] CALENDAR_PERMISSION = {"android.permission.READ_CALENDAR", "android.permission.WRITE_CALENDAR"};
    private static final String[] CONTACTS_PERMISSION = {"android.permission.READ_CONTACTS"};

    public void ContinueOnCreateEventInfo() {
        boolean z;
        int i;
        Log.d("EventInfoActivity", "continueonCreateCalendar ");
        Intent intent = getIntent();
        this.mEventId = -1L;
        ArrayList<CalendarEventModel.ReminderEntry> remindersFromBundle = null;
        if (this.mBundleIcicleOncreate != null) {
            this.mEventId = this.mBundleIcicleOncreate.getLong("key_event_id");
            this.mStartMillis = this.mBundleIcicleOncreate.getLong("key_start_millis");
            this.mEndMillis = this.mBundleIcicleOncreate.getLong("key_end_millis");
            int i2 = this.mBundleIcicleOncreate.getInt("key_attendee_response");
            boolean z2 = this.mBundleIcicleOncreate.getBoolean("key_fragment_is_dialog");
            remindersFromBundle = Utils.readRemindersFromBundle(this.mBundleIcicleOncreate);
            i = i2;
            z = z2;
        } else if (intent == null || !"android.intent.action.VIEW".equals(intent.getAction())) {
            z = false;
            i = 0;
        } else {
            this.mStartMillis = intent.getLongExtra("beginTime", 0L);
            this.mEndMillis = intent.getLongExtra("endTime", 0L);
            int intExtra = intent.getIntExtra("attendeeStatus", 0);
            Uri data = intent.getData();
            if (data != null) {
                try {
                    List<String> pathSegments = data.getPathSegments();
                    int size = pathSegments.size();
                    if (size <= 2 || !"EventTime".equals(pathSegments.get(2))) {
                        this.mEventId = Long.parseLong(data.getLastPathSegment());
                    } else {
                        this.mEventId = Long.parseLong(pathSegments.get(1));
                        if (size > 4) {
                            this.mStartMillis = Long.parseLong(pathSegments.get(3));
                            this.mEndMillis = Long.parseLong(pathSegments.get(4));
                        }
                    }
                } catch (NumberFormatException e) {
                    if (this.mEventId != -1 && (this.mStartMillis == 0 || this.mEndMillis == 0)) {
                        this.mStartMillis = 0L;
                        this.mEndMillis = 0L;
                    }
                }
            }
            i = intExtra;
            z = false;
        }
        ArrayList<CalendarEventModel.ReminderEntry> arrayList = remindersFromBundle;
        if (this.mEventId == -1) {
            Log.w("EventInfoActivity", "No event id");
            Toast.makeText(this, R.string.event_not_found, 0).show();
            finish();
        }
        Resources resources = getResources();
        if (resources.getBoolean(R.bool.agenda_show_event_info_full_screen) || resources.getBoolean(R.bool.show_event_info_full_screen)) {
            setContentView(R.layout.simple_frame_layout);
            this.mInfoFragment = (EventInfoFragment) getFragmentManager().findFragmentById(R.id.main_frame);
            ActionBar actionBar = getActionBar();
            if (actionBar != null) {
                actionBar.setDisplayOptions(6);
            }
            if (this.mInfoFragment == null) {
                FragmentTransaction fragmentTransactionBeginTransaction = getFragmentManager().beginTransaction();
                this.mInfoFragment = new EventInfoFragment(this, this.mEventId, this.mStartMillis, this.mEndMillis, i, z, z ? 1 : 0, arrayList);
                fragmentTransactionBeginTransaction.replace(R.id.main_frame, this.mInfoFragment);
                fragmentTransactionBeginTransaction.commit();
            }
            NfcHandler.register(this, this.mInfoFragment);
            return;
        }
        CalendarController.getInstance(this).launchViewEvent(this.mEventId, this.mStartMillis, this.mEndMillis, i);
        finish();
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
    protected void onCreate(Bundle bundle) {
        Log.d("EventInfoActivity", "onCreate before permission check for eventInfo ");
        super.onCreate(bundle);
        this.mBundleIcicleOncreate = bundle;
        ContinueOnCreateEventInfo();
    }

    @Override
    public void onRequestPermissionsResult(int i, String[] strArr, int[] iArr) {
        Log.d("EventInfoActivity", "onRequestPermissionsResult " + Arrays.toString(strArr));
        Log.d("EventInfoActivity", "onRequestPermissionsResult Requestcode[" + i + "]");
        for (int i2 = 0; i2 < strArr.length; i2++) {
            if (iArr[i2] != 0) {
                Toast.makeText(getApplicationContext(), getResources().getString(R.string.denied_required_permission), 1).show();
                finish();
                CalendarAppWidgetService.CALENDAR_PERMISSION_GRANTED = false;
                return;
            }
        }
        CalendarAppWidgetService.CALENDAR_PERMISSION_GRANTED = true;
        if (i == 1) {
            this.mOnCreateRequestPermissionFlag = 1;
            ContinueOnCreateEventInfo();
            onResume();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        return super.onOptionsItemSelected(menuItem);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        Fragment fragmentFindFragmentById = getFragmentManager().findFragmentById(R.id.main_frame);
        if (fragmentFindFragmentById != null) {
            EventInfoFragment eventInfoFragment = (EventInfoFragment) fragmentFindFragmentById;
            bundle.putLong("key_event_id", eventInfoFragment.getEventId());
            bundle.putLong("key_start_millis", eventInfoFragment.getStartMillis());
            bundle.putLong("key_end_millis", eventInfoFragment.getEndMillis());
            Log.i("EventInfoActivity", "eventId= " + eventInfoFragment.getEventId() + ", startMillis= " + eventInfoFragment.getStartMillis() + ", endMillis= " + eventInfoFragment.getEndMillis());
        }
        super.onSaveInstanceState(bundle);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!hasRequiredPermission(CALENDAR_PERMISSION) || !hasRequiredPermission(STORAGE_PERMISSION) || !hasRequiredPermission(CONTACTS_PERMISSION)) {
            Toast.makeText(getApplicationContext(), getResources().getString(R.string.denied_required_permission), 1).show();
            finish();
        } else {
            getContentResolver().registerContentObserver(CalendarContract.Events.CONTENT_URI, true, this.mObserver);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        getContentResolver().unregisterContentObserver(this.mObserver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        CalendarController.removeInstance(this);
    }
}
