package com.android.calendar;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.provider.CalendarContract;
import android.provider.SearchRecentSuggestions;
import android.text.format.Time;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.SearchView;
import com.android.calendar.CalendarController;
import com.android.calendar.CalendarEventModel;
import com.android.calendar.agenda.AgendaFragment;
import com.android.calendar.alerts.AlertUtils;
import com.mediatek.calendar.LogUtil;
import java.util.ArrayList;

public class SearchActivity extends Activity implements MenuItem.OnActionExpandListener, SearchView.OnQueryTextListener, CalendarController.EventHandler {
    private static final String TAG = SearchActivity.class.getSimpleName();
    private static boolean mIsMultipane;
    private ContentResolver mContentResolver;
    private CalendarController mController;
    private DeleteEventHelper mDeleteEventHelper;
    private EventInfoFragment mEventInfoFragment;
    private Handler mHandler;
    private String mQuery;
    private SearchView mSearchView;
    private boolean mShowEventDetailsWithAgenda;
    private BroadcastReceiver mTimeChangesReceiver;
    private long mCurrentEventId = -1;
    private final ContentObserver mObserver = new ContentObserver(new Handler()) {
        @Override
        public boolean deliverSelfNotifications() {
            return true;
        }

        @Override
        public void onChange(boolean z) {
            SearchActivity.this.eventsChanged();
        }
    };
    private final Runnable mTimeChangesUpdater = new Runnable() {
        @Override
        public void run() {
            Utils.setMidnightUpdater(SearchActivity.this.mHandler, SearchActivity.this.mTimeChangesUpdater, Utils.getTimeZone(SearchActivity.this, SearchActivity.this.mTimeChangesUpdater));
            SearchActivity.this.invalidateOptionsMenu();
        }
    };

    @Override
    protected void onCreate(Bundle bundle) {
        long jTimeFromIntentInMillis;
        String stringExtra;
        super.onCreate(bundle);
        this.mController = CalendarController.getInstance(this);
        this.mHandler = new Handler();
        mIsMultipane = Utils.getConfigBool(this, R.bool.multiple_pane_config);
        this.mShowEventDetailsWithAgenda = Utils.getConfigBool(this, R.bool.show_event_details_with_agenda);
        setContentView(R.layout.search);
        setDefaultKeyMode(3);
        this.mContentResolver = getContentResolver();
        if (mIsMultipane) {
            getActionBar().setDisplayOptions(4, 4);
        } else {
            getActionBar().setDisplayOptions(0, 6);
        }
        this.mController.registerEventHandler(0, this);
        this.mDeleteEventHelper = new DeleteEventHelper(this, this, false);
        if (bundle != null) {
            jTimeFromIntentInMillis = bundle.getLong("key_restore_time");
        } else {
            jTimeFromIntentInMillis = 0;
        }
        if (jTimeFromIntentInMillis == 0) {
            jTimeFromIntentInMillis = Utils.timeFromIntentInMillis(getIntent());
        }
        Intent intent = getIntent();
        if ("android.intent.action.SEARCH".equals(intent.getAction())) {
            if (bundle != null && bundle.containsKey("key_restore_search_query")) {
                stringExtra = bundle.getString("key_restore_search_query");
            } else {
                stringExtra = intent.getStringExtra("query");
            }
            if ("TARDIS".equalsIgnoreCase(stringExtra)) {
                Utils.tardis();
            }
            initFragments(jTimeFromIntentInMillis, stringExtra);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.mController.deregisterAllEventHandlers();
        CalendarController.removeInstance(this);
    }

    private void initFragments(long j, String str) {
        FragmentTransaction fragmentTransactionBeginTransaction = getFragmentManager().beginTransaction();
        AgendaFragment agendaFragment = new AgendaFragment(j, true);
        fragmentTransactionBeginTransaction.replace(R.id.search_results, agendaFragment);
        this.mController.registerEventHandler(R.id.search_results, agendaFragment);
        fragmentTransactionBeginTransaction.commit();
        Time time = new Time();
        time.set(j);
        search(str, time);
    }

    private void showEventInfo(CalendarController.EventInfo eventInfo) {
        long millis;
        long millis2;
        long millis3;
        if (this.mShowEventDetailsWithAgenda) {
            FragmentTransaction fragmentTransactionBeginTransaction = getFragmentManager().beginTransaction();
            this.mEventInfoFragment = new EventInfoFragment((Context) this, eventInfo.id, eventInfo.startTime.toMillis(false), eventInfo.endTime.toMillis(false), eventInfo.getResponse(), false, 1, (ArrayList<CalendarEventModel.ReminderEntry>) null);
            fragmentTransactionBeginTransaction.replace(R.id.agenda_event_info, this.mEventInfoFragment);
            fragmentTransactionBeginTransaction.commitAllowingStateLoss();
        } else {
            Intent intent = new Intent("android.intent.action.VIEW");
            intent.setData(ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventInfo.id));
            intent.setClass(this, EventInfoActivity.class);
            if (eventInfo.startTime != null) {
                millis = eventInfo.startTime.toMillis(true);
            } else {
                millis = -1;
            }
            intent.putExtra("beginTime", millis);
            if (eventInfo.endTime != null) {
                millis2 = eventInfo.endTime.toMillis(true);
            } else {
                millis2 = -1;
            }
            intent.putExtra("endTime", millis2);
            startActivity(intent);
            long j = eventInfo.id;
            if (eventInfo.startTime != null) {
                millis3 = eventInfo.startTime.toMillis(false);
            } else {
                millis3 = -1;
            }
            AlertUtils.removeEventNotification(this, j, millis3, eventInfo.endTime != null ? eventInfo.endTime.toMillis(false) : -1L);
        }
        this.mCurrentEventId = eventInfo.id;
    }

    private void search(String str, Time time) {
        new SearchRecentSuggestions(this, Utils.getSearchAuthority(this), 1).saveRecentQuery(str, null);
        CalendarController.EventInfo eventInfo = new CalendarController.EventInfo();
        eventInfo.eventType = 256L;
        eventInfo.query = str;
        eventInfo.viewType = 1;
        if (time != null) {
            eventInfo.startTime = time;
        }
        this.mController.sendEvent(this, eventInfo);
        this.mQuery = str;
        if (this.mSearchView != null) {
            this.mSearchView.setQuery(this.mQuery, false);
            this.mSearchView.clearFocus();
        }
    }

    private void deleteEvent(long j, long j2, long j3) {
        this.mDeleteEventHelper.delete(j2, j3, j, -1);
        if (mIsMultipane && this.mEventInfoFragment != null && j == this.mCurrentEventId) {
            FragmentTransaction fragmentTransactionBeginTransaction = getFragmentManager().beginTransaction();
            fragmentTransactionBeginTransaction.remove(this.mEventInfoFragment);
            fragmentTransactionBeginTransaction.commit();
            this.mEventInfoFragment = null;
            this.mCurrentEventId = -1L;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.search_title_bar, menu);
        MenuItem menuItemFindItem = menu.findItem(R.id.action_today);
        if (Utils.isJellybeanOrLater()) {
            Utils.setTodayIcon((LayerDrawable) menuItemFindItem.getIcon(), this, Utils.getTimeZone(this, this.mTimeChangesUpdater));
        } else {
            menuItemFindItem.setIcon(R.drawable.ic_menu_today_no_date_holo_light);
        }
        MenuItem menuItemFindItem2 = menu.findItem(R.id.action_search);
        menuItemFindItem2.expandActionView();
        menuItemFindItem2.setOnActionExpandListener(this);
        this.mSearchView = (SearchView) menuItemFindItem2.getActionView();
        Utils.setUpSearchView(this.mSearchView, this);
        this.mSearchView.setQuery(this.mQuery, false);
        this.mSearchView.clearFocus();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        int itemId = menuItem.getItemId();
        if (itemId == R.id.action_today) {
            Time time = new Time();
            time.setToNow();
            this.mController.sendEvent(this, 32L, time, null, -1L, 0);
            return true;
        }
        if (itemId == R.id.action_search) {
            return false;
        }
        if (itemId == R.id.action_settings) {
            this.mController.sendEvent(this, 64L, null, null, 0L, 0);
            return true;
        }
        if (itemId != 16908332) {
            return false;
        }
        Utils.returnToCalendarHome(this);
        return true;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if ("android.intent.action.SEARCH".equals(intent.getAction())) {
            search(intent.getStringExtra("query"), null);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        bundle.putLong("key_restore_time", this.mController.getTime());
        bundle.putString("key_restore_search_query", this.mQuery);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Utils.setMidnightUpdater(this.mHandler, this.mTimeChangesUpdater, Utils.getTimeZone(this, this.mTimeChangesUpdater));
        invalidateOptionsMenu();
        this.mTimeChangesReceiver = Utils.setTimeChangesReceiver(this, this.mTimeChangesUpdater);
        this.mContentResolver.registerContentObserver(CalendarContract.Events.CONTENT_URI, true, this.mObserver);
        eventsChanged();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Utils.resetMidnightUpdater(this.mHandler, this.mTimeChangesUpdater);
        Utils.clearTimeChangesReceiver(this, this.mTimeChangesReceiver);
        this.mContentResolver.unregisterContentObserver(this.mObserver);
    }

    public void eventsChanged() {
        this.mController.sendEvent(this, 128L, null, null, -1L, 0);
    }

    @Override
    public long getSupportedEventTypes() {
        return 18L;
    }

    @Override
    public void handleEvent(CalendarController.EventInfo eventInfo) {
        long millis = eventInfo.endTime == null ? -1L : eventInfo.endTime.toMillis(false);
        if (eventInfo.eventType == 2) {
            showEventInfo(eventInfo);
        } else if (eventInfo.eventType == 16) {
            deleteEvent(eventInfo.id, eventInfo.startTime.toMillis(false), millis);
        }
    }

    @Override
    public boolean onQueryTextChange(String str) {
        return false;
    }

    @Override
    public boolean onQueryTextSubmit(String str) {
        this.mQuery = str;
        this.mController.sendEvent(this, 256L, null, null, -1L, 0, 0L, str, getComponentName());
        return false;
    }

    @Override
    public boolean onMenuItemActionExpand(MenuItem menuItem) {
        return true;
    }

    @Override
    public boolean onMenuItemActionCollapse(MenuItem menuItem) {
        Utils.returnToCalendarHome(this);
        if (!isFinishing()) {
            finish();
            LogUtil.v(TAG, "onMenuItemActionCollapse start target activity,finish SearchActivity.");
            return false;
        }
        return false;
    }
}
