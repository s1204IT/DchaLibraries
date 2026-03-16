package com.android.calendar;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.graphics.drawable.LayerDrawable;
import android.net.Uri;
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
        public void onChange(boolean selfChange) {
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
    protected void onCreate(Bundle icicle) {
        String query;
        super.onCreate(icicle);
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
        long millis = 0;
        if (icicle != null) {
            millis = icicle.getLong("key_restore_time");
        }
        if (millis == 0) {
            millis = Utils.timeFromIntentInMillis(getIntent());
        }
        Intent intent = getIntent();
        if ("android.intent.action.SEARCH".equals(intent.getAction())) {
            if (icicle != null && icicle.containsKey("key_restore_search_query")) {
                query = icicle.getString("key_restore_search_query");
            } else {
                query = intent.getStringExtra("query");
            }
            if ("TARDIS".equalsIgnoreCase(query)) {
                Utils.tardis();
            }
            initFragments(millis, query);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.mController.deregisterAllEventHandlers();
        CalendarController.removeInstance(this);
    }

    private void initFragments(long timeMillis, String query) {
        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction ft = fragmentManager.beginTransaction();
        AgendaFragment searchResultsFragment = new AgendaFragment(timeMillis, true);
        ft.replace(R.id.search_results, searchResultsFragment);
        this.mController.registerEventHandler(R.id.search_results, searchResultsFragment);
        ft.commit();
        Time t = new Time();
        t.set(timeMillis);
        search(query, t);
    }

    private void showEventInfo(CalendarController.EventInfo event) {
        if (this.mShowEventDetailsWithAgenda) {
            FragmentManager fragmentManager = getFragmentManager();
            FragmentTransaction ft = fragmentManager.beginTransaction();
            this.mEventInfoFragment = new EventInfoFragment((Context) this, event.id, event.startTime.toMillis(false), event.endTime.toMillis(false), event.getResponse(), false, 1, (ArrayList<CalendarEventModel.ReminderEntry>) null);
            ft.replace(R.id.agenda_event_info, this.mEventInfoFragment);
            ft.commit();
        } else {
            Intent intent = new Intent("android.intent.action.VIEW");
            Uri eventUri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, event.id);
            intent.setData(eventUri);
            intent.setClass(this, EventInfoActivity.class);
            intent.putExtra("beginTime", event.startTime != null ? event.startTime.toMillis(true) : -1L);
            intent.putExtra("endTime", event.endTime != null ? event.endTime.toMillis(true) : -1L);
            startActivity(intent);
        }
        this.mCurrentEventId = event.id;
    }

    private void search(String searchQuery, Time goToTime) {
        SearchRecentSuggestions suggestions = new SearchRecentSuggestions(this, Utils.getSearchAuthority(this), 1);
        suggestions.saveRecentQuery(searchQuery, null);
        CalendarController.EventInfo searchEventInfo = new CalendarController.EventInfo();
        searchEventInfo.eventType = 256L;
        searchEventInfo.query = searchQuery;
        searchEventInfo.viewType = 1;
        if (goToTime != null) {
            searchEventInfo.startTime = goToTime;
        }
        this.mController.sendEvent(this, searchEventInfo);
        this.mQuery = searchQuery;
        if (this.mSearchView != null) {
            this.mSearchView.setQuery(this.mQuery, false);
            this.mSearchView.clearFocus();
        }
    }

    private void deleteEvent(long eventId, long startMillis, long endMillis) {
        this.mDeleteEventHelper.delete(startMillis, endMillis, eventId, -1);
        if (mIsMultipane && this.mEventInfoFragment != null && eventId == this.mCurrentEventId) {
            FragmentManager fragmentManager = getFragmentManager();
            FragmentTransaction ft = fragmentManager.beginTransaction();
            ft.remove(this.mEventInfoFragment);
            ft.commit();
            this.mEventInfoFragment = null;
            this.mCurrentEventId = -1L;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.search_title_bar, menu);
        MenuItem menuItem = menu.findItem(R.id.action_today);
        if (Utils.isJellybeanOrLater()) {
            LayerDrawable icon = (LayerDrawable) menuItem.getIcon();
            Utils.setTodayIcon(icon, this, Utils.getTimeZone(this, this.mTimeChangesUpdater));
        } else {
            menuItem.setIcon(R.drawable.ic_menu_today_no_date_holo_light);
        }
        MenuItem item = menu.findItem(R.id.action_search);
        item.expandActionView();
        item.setOnActionExpandListener(this);
        this.mSearchView = (SearchView) item.getActionView();
        Utils.setUpSearchView(this.mSearchView, this);
        this.mSearchView.setQuery(this.mQuery, false);
        this.mSearchView.clearFocus();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_today) {
            Time t = new Time();
            t.setToNow();
            this.mController.sendEvent(this, 32L, t, null, -1L, 0);
            return true;
        }
        if (itemId == R.id.action_search) {
            return false;
        }
        if (itemId == R.id.action_settings) {
            this.mController.sendEvent(this, 64L, null, null, 0L, 0);
            return true;
        }
        if (itemId == 16908332) {
            Utils.returnToCalendarHome(this);
            return true;
        }
        return false;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if ("android.intent.action.SEARCH".equals(intent.getAction())) {
            String query = intent.getStringExtra("query");
            search(query, null);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong("key_restore_time", this.mController.getTime());
        outState.putString("key_restore_search_query", this.mQuery);
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
    public void handleEvent(CalendarController.EventInfo event) {
        long endTime = event.endTime == null ? -1L : event.endTime.toMillis(false);
        if (event.eventType == 2) {
            showEventInfo(event);
        } else if (event.eventType == 16) {
            deleteEvent(event.id, event.startTime.toMillis(false), endTime);
        }
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        return false;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        this.mQuery = query;
        this.mController.sendEvent(this, 256L, null, null, -1L, 0, 0L, query, getComponentName());
        return false;
    }

    @Override
    public boolean onMenuItemActionExpand(MenuItem item) {
        return true;
    }

    @Override
    public boolean onMenuItemActionCollapse(MenuItem item) {
        Utils.returnToCalendarHome(this);
        return false;
    }
}
