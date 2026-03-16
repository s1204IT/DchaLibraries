package com.android.calendar;

import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.app.ActionBar;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.AsyncQueryHandler;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.drawable.LayerDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.CalendarContract;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.text.format.Time;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SearchView;
import android.widget.TextView;
import com.android.calendar.CalendarController;
import com.android.calendar.CalendarEventModel;
import com.android.calendar.agenda.AgendaFragment;
import com.android.calendar.month.MonthByWeekFragment;
import com.android.calendar.selectcalendars.SelectVisibleCalendarsFragment;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class AllInOneActivity extends AbstractCalendarActivity implements ActionBar.OnNavigationListener, ActionBar.TabListener, SharedPreferences.OnSharedPreferenceChangeListener, SearchView.OnQueryTextListener, SearchView.OnSuggestionListener, CalendarController.EventHandler {
    private static boolean mIsMultipane;
    private static boolean mIsTabletConfig;
    private static boolean mShowAgendaWithMonth;
    private static boolean mShowEventDetailsWithAgenda;
    private ActionBar mActionBar;
    private CalendarViewAdapter mActionBarMenuSpinnerAdapter;
    private ActionBar.Tab mAgendaTab;
    BroadcastReceiver mCalIntentReceiver;
    private int mCalendarControlsAnimationTime;
    private View mCalendarsList;
    private ContentResolver mContentResolver;
    private CalendarController mController;
    private int mControlsAnimateHeight;
    private int mControlsAnimateWidth;
    private MenuItem mControlsMenu;
    private RelativeLayout.LayoutParams mControlsParams;
    private int mCurrentView;
    private TextView mDateRange;
    private ActionBar.Tab mDayTab;
    private QueryHandler mHandler;
    private String mHideString;
    private TextView mHomeTime;
    private View mMiniMonth;
    private View mMiniMonthContainer;
    private ActionBar.Tab mMonthTab;
    private Menu mOptionsMenu;
    int mOrientation;
    private int mPreviousView;
    private MenuItem mSearchMenu;
    private SearchView mSearchView;
    private View mSecondaryPane;
    private boolean mShowCalendarControls;
    private boolean mShowEventInfoFullScreen;
    private boolean mShowEventInfoFullScreenAgenda;
    private String mShowString;
    private String mTimeZone;
    private LinearLayout.LayoutParams mVerticalControlsParams;
    private int mWeekNum;
    private ActionBar.Tab mWeekTab;
    private TextView mWeekTextView;
    private boolean mOnSaveInstanceStateCalled = false;
    private boolean mBackToPreviousView = false;
    private boolean mPaused = true;
    private boolean mUpdateOnResume = false;
    private boolean mHideControls = false;
    private boolean mShowSideViews = true;
    private boolean mShowWeekNum = false;
    private long mViewEventId = -1;
    private long mIntentEventStartMillis = -1;
    private long mIntentEventEndMillis = -1;
    private int mIntentAttendeeResponse = 0;
    private boolean mIntentAllDay = false;
    private boolean mCheckForAccounts = true;
    private AllInOneMenuExtensionsInterface mExtensions = ExtensionsFactory.getAllInOneMenuExtensions();
    private final Animator.AnimatorListener mSlideAnimationDoneListener = new Animator.AnimatorListener() {
        @Override
        public void onAnimationCancel(Animator animation) {
        }

        @Override
        public void onAnimationEnd(Animator animation) {
            int visibility = AllInOneActivity.this.mShowSideViews ? 0 : 8;
            AllInOneActivity.this.mMiniMonth.setVisibility(visibility);
            AllInOneActivity.this.mCalendarsList.setVisibility(visibility);
            AllInOneActivity.this.mMiniMonthContainer.setVisibility(visibility);
        }

        @Override
        public void onAnimationRepeat(Animator animation) {
        }

        @Override
        public void onAnimationStart(Animator animation) {
        }
    };
    private final Runnable mHomeTimeUpdater = new Runnable() {
        @Override
        public void run() {
            AllInOneActivity.this.mTimeZone = Utils.getTimeZone(AllInOneActivity.this, AllInOneActivity.this.mHomeTimeUpdater);
            AllInOneActivity.this.updateSecondaryTitleFields(-1L);
            AllInOneActivity.this.invalidateOptionsMenu();
            Utils.setMidnightUpdater(AllInOneActivity.this.mHandler, AllInOneActivity.this.mTimeChangesUpdater, AllInOneActivity.this.mTimeZone);
        }
    };
    private final Runnable mTimeChangesUpdater = new Runnable() {
        @Override
        public void run() {
            AllInOneActivity.this.mTimeZone = Utils.getTimeZone(AllInOneActivity.this, AllInOneActivity.this.mHomeTimeUpdater);
            AllInOneActivity.this.invalidateOptionsMenu();
            Utils.setMidnightUpdater(AllInOneActivity.this.mHandler, AllInOneActivity.this.mTimeChangesUpdater, AllInOneActivity.this.mTimeZone);
        }
    };
    private final ContentObserver mObserver = new ContentObserver(new Handler()) {
        @Override
        public boolean deliverSelfNotifications() {
            return true;
        }

        @Override
        public void onChange(boolean selfChange) {
            AllInOneActivity.this.eventsChanged();
        }
    };

    private class QueryHandler extends AsyncQueryHandler {
        public QueryHandler(ContentResolver cr) {
            super(cr);
        }

        @Override
        protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
            AllInOneActivity.this.mCheckForAccounts = false;
            if (cursor != null) {
                try {
                    if (cursor.getCount() <= 0) {
                        if (!AllInOneActivity.this.isFinishing()) {
                            if (cursor != null) {
                                cursor.close();
                            }
                            Bundle options = new Bundle();
                            options.putCharSequence("introMessage", AllInOneActivity.this.getResources().getString(R.string.create_an_account_desc));
                            options.putBoolean("allowSkip", true);
                            AccountManager am = AccountManager.get(AllInOneActivity.this);
                            am.addAccount("com.google", "com.android.calendar", null, options, AllInOneActivity.this, new AccountManagerCallback<Bundle>() {
                                @Override
                                public void run(AccountManagerFuture<Bundle> future) {
                                    if (!future.isCancelled()) {
                                        try {
                                            Bundle result = future.getResult();
                                            boolean setupSkipped = result.getBoolean("setupSkipped");
                                            if (setupSkipped) {
                                                Utils.setSharedPreference((Context) AllInOneActivity.this, "preferences_skip_setup", true);
                                            }
                                        } catch (AuthenticatorException e) {
                                        } catch (OperationCanceledException e2) {
                                        } catch (IOException e3) {
                                        }
                                    }
                                }
                            }, null);
                            return;
                        }
                    }
                } finally {
                    if (cursor != null) {
                        cursor.close();
                    }
                }
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        String action = intent.getAction();
        if ("android.intent.action.VIEW".equals(action) && !intent.getBooleanExtra("KEY_HOME", false)) {
            long millis = parseViewAction(intent);
            if (millis == -1) {
                millis = Utils.timeFromIntentInMillis(intent);
            }
            if (millis != -1 && this.mViewEventId == -1 && this.mController != null) {
                Time time = new Time(this.mTimeZone);
                time.set(millis);
                time.normalize(true);
                this.mController.sendEvent(this, 32L, time, time, -1L, 0);
            }
        }
    }

    @Override
    protected void onCreate(Bundle icicle) {
        if (Utils.getSharedPreference((Context) this, "preferences_tardis_1", false)) {
            setTheme(R.style.CalendarTheme_WithActionBarWallpaper);
        }
        super.onCreate(icicle);
        if (icicle != null && icicle.containsKey("key_check_for_accounts")) {
            this.mCheckForAccounts = icicle.getBoolean("key_check_for_accounts");
        }
        if (this.mCheckForAccounts && !Utils.getSharedPreference((Context) this, "preferences_skip_setup", false)) {
            this.mHandler = new QueryHandler(getContentResolver());
            this.mHandler.startQuery(0, null, CalendarContract.Calendars.CONTENT_URI, new String[]{"_id"}, null, null, null);
        }
        this.mController = CalendarController.getInstance(this);
        long timeMillis = -1;
        int viewType = -1;
        Intent intent = getIntent();
        if (icicle != null) {
            timeMillis = icicle.getLong("key_restore_time");
            viewType = icicle.getInt("key_restore_view", -1);
        } else {
            String action = intent.getAction();
            if ("android.intent.action.VIEW".equals(action)) {
                timeMillis = parseViewAction(intent);
            }
            if (timeMillis == -1) {
                timeMillis = Utils.timeFromIntentInMillis(intent);
            }
        }
        if (viewType == -1 || viewType > 5) {
            viewType = Utils.getViewTypeFromIntentAndSharedPref(this);
        }
        this.mTimeZone = Utils.getTimeZone(this, this.mHomeTimeUpdater);
        Time t = new Time(this.mTimeZone);
        t.set(timeMillis);
        Resources res = getResources();
        this.mHideString = res.getString(R.string.hide_controls);
        this.mShowString = res.getString(R.string.show_controls);
        this.mOrientation = res.getConfiguration().orientation;
        if (this.mOrientation == 2) {
            this.mControlsAnimateWidth = (int) res.getDimension(R.dimen.calendar_controls_width);
            if (this.mControlsParams == null) {
                this.mControlsParams = new RelativeLayout.LayoutParams(this.mControlsAnimateWidth, 0);
            }
            this.mControlsParams.addRule(11);
        } else {
            this.mControlsAnimateWidth = Math.max((res.getDisplayMetrics().widthPixels * 45) / 100, (int) res.getDimension(R.dimen.min_portrait_calendar_controls_width));
            this.mControlsAnimateWidth = Math.min(this.mControlsAnimateWidth, (int) res.getDimension(R.dimen.max_portrait_calendar_controls_width));
        }
        this.mControlsAnimateHeight = (int) res.getDimension(R.dimen.calendar_controls_height);
        this.mHideControls = !Utils.getSharedPreference((Context) this, "preferences_show_controls", true);
        mIsMultipane = Utils.getConfigBool(this, R.bool.multiple_pane_config);
        mIsTabletConfig = Utils.getConfigBool(this, R.bool.tablet_config);
        mShowAgendaWithMonth = Utils.getConfigBool(this, R.bool.show_agenda_with_month);
        this.mShowCalendarControls = Utils.getConfigBool(this, R.bool.show_calendar_controls);
        mShowEventDetailsWithAgenda = Utils.getConfigBool(this, R.bool.show_event_details_with_agenda);
        this.mShowEventInfoFullScreenAgenda = Utils.getConfigBool(this, R.bool.agenda_show_event_info_full_screen);
        this.mShowEventInfoFullScreen = Utils.getConfigBool(this, R.bool.show_event_info_full_screen);
        this.mCalendarControlsAnimationTime = res.getInteger(R.integer.calendar_controls_animation_time);
        Utils.setAllowWeekForDetailView(mIsMultipane);
        setContentView(R.layout.all_in_one);
        if (mIsTabletConfig) {
            this.mDateRange = (TextView) findViewById(R.id.date_bar);
            this.mWeekTextView = (TextView) findViewById(R.id.week_num);
        } else {
            this.mDateRange = (TextView) getLayoutInflater().inflate(R.layout.date_range_title, (ViewGroup) null);
        }
        configureActionBar(viewType);
        this.mHomeTime = (TextView) findViewById(R.id.home_time);
        this.mMiniMonth = findViewById(R.id.mini_month);
        if (mIsTabletConfig && this.mOrientation == 1) {
            this.mMiniMonth.setLayoutParams(new RelativeLayout.LayoutParams(this.mControlsAnimateWidth, this.mControlsAnimateHeight));
        }
        this.mCalendarsList = findViewById(R.id.calendar_list);
        this.mMiniMonthContainer = findViewById(R.id.mini_month_container);
        this.mSecondaryPane = findViewById(R.id.secondary_pane);
        this.mController.registerFirstEventHandler(0, this);
        initFragments(timeMillis, viewType, icicle);
        SharedPreferences prefs = GeneralPreferences.getSharedPreferences(this);
        prefs.registerOnSharedPreferenceChangeListener(this);
        this.mContentResolver = getContentResolver();
    }

    private long parseViewAction(Intent intent) {
        Uri data = intent.getData();
        if (data == null || !data.isHierarchical()) {
            return -1L;
        }
        List<String> path = data.getPathSegments();
        if (path.size() != 2 || !path.get(0).equals("events")) {
            return -1L;
        }
        try {
            this.mViewEventId = Long.valueOf(data.getLastPathSegment()).longValue();
            if (this.mViewEventId == -1) {
                return -1L;
            }
            this.mIntentEventStartMillis = intent.getLongExtra("beginTime", 0L);
            this.mIntentEventEndMillis = intent.getLongExtra("endTime", 0L);
            this.mIntentAttendeeResponse = intent.getIntExtra("attendeeStatus", 0);
            this.mIntentAllDay = intent.getBooleanExtra("allDay", false);
            long timeMillis = this.mIntentEventStartMillis;
            return timeMillis;
        } catch (NumberFormatException e) {
            return -1L;
        }
    }

    private void configureActionBar(int viewType) {
        createButtonsSpinner(viewType, mIsTabletConfig);
        if (mIsMultipane) {
            this.mActionBar.setDisplayOptions(18);
        } else {
            this.mActionBar.setDisplayOptions(0);
        }
    }

    private void createButtonsSpinner(int viewType, boolean tabletConfig) {
        this.mActionBarMenuSpinnerAdapter = new CalendarViewAdapter(this, viewType, !tabletConfig);
        this.mActionBar = getActionBar();
        this.mActionBar.setNavigationMode(1);
        this.mActionBar.setListNavigationCallbacks(this.mActionBarMenuSpinnerAdapter, this);
        switch (viewType) {
            case 1:
                this.mActionBar.setSelectedNavigationItem(3);
                break;
            case 2:
                this.mActionBar.setSelectedNavigationItem(0);
                break;
            case 3:
                this.mActionBar.setSelectedNavigationItem(1);
                break;
            case 4:
                this.mActionBar.setSelectedNavigationItem(2);
                break;
            default:
                this.mActionBar.setSelectedNavigationItem(0);
                break;
        }
    }

    private void clearOptionsMenu() {
        MenuItem cancelItem;
        if (this.mOptionsMenu != null && (cancelItem = this.mOptionsMenu.findItem(R.id.action_cancel)) != null) {
            cancelItem.setVisible(false);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Utils.trySyncAndDisableUpgradeReceiver(this);
        this.mController.registerFirstEventHandler(0, this);
        this.mOnSaveInstanceStateCalled = false;
        this.mContentResolver.registerContentObserver(CalendarContract.Events.CONTENT_URI, true, this.mObserver);
        if (this.mUpdateOnResume) {
            initFragments(this.mController.getTime(), this.mController.getViewType(), null);
            this.mUpdateOnResume = false;
        }
        Time t = new Time(this.mTimeZone);
        t.set(this.mController.getTime());
        this.mController.sendEvent(this, 1024L, t, t, -1L, 0, this.mController.getDateFlags(), null, null);
        if (this.mActionBarMenuSpinnerAdapter != null) {
            this.mActionBarMenuSpinnerAdapter.refresh(this);
        }
        if (this.mControlsMenu != null) {
            this.mControlsMenu.setTitle(this.mHideControls ? this.mShowString : this.mHideString);
        }
        this.mPaused = false;
        if (this.mViewEventId != -1 && this.mIntentEventStartMillis != -1 && this.mIntentEventEndMillis != -1) {
            long currentMillis = System.currentTimeMillis();
            long selectedTime = -1;
            if (currentMillis > this.mIntentEventStartMillis && currentMillis < this.mIntentEventEndMillis) {
                selectedTime = currentMillis;
            }
            this.mController.sendEventRelatedEventWithExtra(this, 2L, this.mViewEventId, this.mIntentEventStartMillis, this.mIntentEventEndMillis, -1, -1, CalendarController.EventInfo.buildViewExtraLong(this.mIntentAttendeeResponse, this.mIntentAllDay), selectedTime);
            this.mViewEventId = -1L;
            this.mIntentEventStartMillis = -1L;
            this.mIntentEventEndMillis = -1L;
            this.mIntentAllDay = false;
        }
        Utils.setMidnightUpdater(this.mHandler, this.mTimeChangesUpdater, this.mTimeZone);
        invalidateOptionsMenu();
        this.mCalIntentReceiver = Utils.setTimeChangesReceiver(this, this.mTimeChangesUpdater);
    }

    @Override
    protected void onPause() {
        super.onPause();
        this.mController.deregisterEventHandler(0);
        this.mPaused = true;
        this.mHomeTime.removeCallbacks(this.mHomeTimeUpdater);
        if (this.mActionBarMenuSpinnerAdapter != null) {
            this.mActionBarMenuSpinnerAdapter.onPause();
        }
        this.mContentResolver.unregisterContentObserver(this.mObserver);
        if (isFinishing()) {
            SharedPreferences prefs = GeneralPreferences.getSharedPreferences(this);
            prefs.unregisterOnSharedPreferenceChangeListener(this);
        }
        if (this.mController.getViewType() != 5) {
            Utils.setDefaultView(this, this.mController.getViewType());
        }
        Utils.resetMidnightUpdater(this.mHandler, this.mTimeChangesUpdater);
        Utils.clearTimeChangesReceiver(this, this.mCalIntentReceiver);
    }

    @Override
    protected void onUserLeaveHint() {
        this.mController.sendEvent(this, 512L, null, null, -1L, 0);
        super.onUserLeaveHint();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        this.mOnSaveInstanceStateCalled = true;
        super.onSaveInstanceState(outState);
        outState.putLong("key_restore_time", this.mController.getTime());
        outState.putInt("key_restore_view", this.mCurrentView);
        if (this.mCurrentView == 5) {
            outState.putLong("key_event_id", this.mController.getEventId());
        } else if (this.mCurrentView == 1) {
            FragmentManager fm = getFragmentManager();
            Fragment f = fm.findFragmentById(R.id.main_pane);
            if (f instanceof AgendaFragment) {
                outState.putLong("key_event_id", ((AgendaFragment) f).getLastShowEventId());
            }
        }
        outState.putBoolean("key_check_for_accounts", this.mCheckForAccounts);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        SharedPreferences prefs = GeneralPreferences.getSharedPreferences(this);
        prefs.unregisterOnSharedPreferenceChangeListener(this);
        this.mController.deregisterAllEventHandlers();
        CalendarController.removeInstance(this);
    }

    private void initFragments(long timeMillis, int viewType, Bundle icicle) {
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        if (this.mShowCalendarControls) {
            MonthByWeekFragment monthByWeekFragment = new MonthByWeekFragment(timeMillis, true);
            ft.replace(R.id.mini_month, monthByWeekFragment);
            this.mController.registerEventHandler(R.id.mini_month, monthByWeekFragment);
            SelectVisibleCalendarsFragment selectVisibleCalendarsFragment = new SelectVisibleCalendarsFragment();
            ft.replace(R.id.calendar_list, selectVisibleCalendarsFragment);
            this.mController.registerEventHandler(R.id.calendar_list, selectVisibleCalendarsFragment);
        }
        if (!this.mShowCalendarControls || viewType == 5) {
            this.mMiniMonth.setVisibility(8);
            this.mCalendarsList.setVisibility(8);
        }
        if (viewType == 5) {
            this.mPreviousView = GeneralPreferences.getSharedPreferences(this).getInt("preferred_startView", 3);
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
            long begin = intent.getLongExtra("beginTime", -1L);
            long end = intent.getLongExtra("endTime", -1L);
            CalendarController.EventInfo info = new CalendarController.EventInfo();
            if (end != -1) {
                info.endTime = new Time();
                info.endTime.set(end);
            }
            if (begin != -1) {
                info.startTime = new Time();
                info.startTime.set(begin);
            }
            info.id = eventId;
            this.mController.setViewType(viewType);
            this.mController.setEventId(eventId);
        } else {
            this.mPreviousView = viewType;
        }
        setMainPane(ft, R.id.main_pane, viewType, timeMillis, true);
        ft.commit();
        Time t = new Time(this.mTimeZone);
        t.set(timeMillis);
        if (viewType == 1 && icicle != null) {
            this.mController.sendEvent(this, 32L, t, null, icicle.getLong("key_event_id", -1L), viewType);
        } else if (viewType != 5) {
            this.mController.sendEvent(this, 32L, t, null, -1L, viewType);
        }
    }

    @Override
    public void onBackPressed() {
        if (this.mCurrentView == 5 || this.mBackToPreviousView) {
            this.mController.sendEvent(this, 32L, null, null, -1L, this.mPreviousView);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        this.mOptionsMenu = menu;
        getMenuInflater().inflate(R.menu.all_in_one_title_bar, menu);
        Integer extensionMenuRes = this.mExtensions.getExtensionMenuResource(menu);
        if (extensionMenuRes != null) {
            getMenuInflater().inflate(extensionMenuRes.intValue(), menu);
        }
        this.mSearchMenu = menu.findItem(R.id.action_search);
        this.mSearchView = (SearchView) this.mSearchMenu.getActionView();
        if (this.mSearchView != null) {
            Utils.setUpSearchView(this.mSearchView, this);
            this.mSearchView.setOnQueryTextListener(this);
            this.mSearchView.setOnSuggestionListener(this);
        }
        this.mControlsMenu = menu.findItem(R.id.action_hide_controls);
        if (!this.mShowCalendarControls) {
            if (this.mControlsMenu != null) {
                this.mControlsMenu.setVisible(false);
                this.mControlsMenu.setEnabled(false);
            }
        } else if (this.mControlsMenu != null && this.mController != null && (this.mController.getViewType() == 4 || this.mController.getViewType() == 1)) {
            this.mControlsMenu.setVisible(false);
            this.mControlsMenu.setEnabled(false);
        } else if (this.mControlsMenu != null) {
            this.mControlsMenu.setTitle(this.mHideControls ? this.mShowString : this.mHideString);
        }
        MenuItem menuItem = menu.findItem(R.id.action_today);
        if (Utils.isJellybeanOrLater()) {
            LayerDrawable icon = (LayerDrawable) menuItem.getIcon();
            Utils.setTodayIcon(icon, this, this.mTimeZone);
        } else {
            menuItem.setIcon(R.drawable.ic_menu_today_no_date_holo_light);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_refresh) {
            this.mController.refreshCalendars();
            return true;
        }
        if (itemId == R.id.action_today) {
            Time t = new Time(this.mTimeZone);
            t.setToNow();
            long extras = 2 | 8;
            this.mController.sendEvent(this, 32L, t, null, t, -1L, 0, extras, null, null);
            return true;
        }
        if (itemId == R.id.action_create_event) {
            Time t2 = new Time();
            t2.set(this.mController.getTime());
            if (t2.minute > 30) {
                t2.hour++;
                t2.minute = 0;
            } else if (t2.minute > 0 && t2.minute < 30) {
                t2.minute = 30;
            }
            this.mController.sendEventRelatedEvent(this, 1L, -1L, t2.toMillis(true), 0L, 0, 0, -1L);
            return true;
        }
        if (itemId == R.id.action_select_visible_calendars) {
            this.mController.sendEvent(this, 2048L, null, null, 0L, 0);
            return true;
        }
        if (itemId == R.id.action_settings) {
            this.mController.sendEvent(this, 64L, null, null, 0L, 0);
            return true;
        }
        if (itemId == R.id.action_hide_controls) {
            this.mHideControls = !this.mHideControls;
            Utils.setSharedPreference(this, "preferences_show_controls", !this.mHideControls);
            item.setTitle(this.mHideControls ? this.mShowString : this.mHideString);
            if (!this.mHideControls) {
                this.mMiniMonth.setVisibility(0);
                this.mCalendarsList.setVisibility(0);
                this.mMiniMonthContainer.setVisibility(0);
            }
            int[] iArr = new int[2];
            iArr[0] = this.mHideControls ? 0 : this.mControlsAnimateWidth;
            iArr[1] = this.mHideControls ? this.mControlsAnimateWidth : 0;
            ObjectAnimator slideAnimation = ObjectAnimator.ofInt(this, "controlsOffset", iArr);
            slideAnimation.setDuration(this.mCalendarControlsAnimationTime);
            ObjectAnimator.setFrameDelay(0L);
            slideAnimation.start();
            return true;
        }
        if (itemId == R.id.action_search) {
            return false;
        }
        return this.mExtensions.handleItemSelected(item, this);
    }

    public void setControlsOffset(int controlsOffset) {
        if (this.mOrientation == 2) {
            this.mMiniMonth.setTranslationX(controlsOffset);
            this.mCalendarsList.setTranslationX(controlsOffset);
            this.mControlsParams.width = Math.max(0, this.mControlsAnimateWidth - controlsOffset);
            this.mMiniMonthContainer.setLayoutParams(this.mControlsParams);
            return;
        }
        this.mMiniMonth.setTranslationY(controlsOffset);
        this.mCalendarsList.setTranslationY(controlsOffset);
        if (this.mVerticalControlsParams == null) {
            this.mVerticalControlsParams = new LinearLayout.LayoutParams(-1, this.mControlsAnimateHeight);
        }
        this.mVerticalControlsParams.height = Math.max(0, this.mControlsAnimateHeight - controlsOffset);
        this.mMiniMonthContainer.setLayoutParams(this.mVerticalControlsParams);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        if (key.equals("preferences_week_start_day")) {
            if (this.mPaused) {
                this.mUpdateOnResume = true;
            } else {
                initFragments(this.mController.getTime(), this.mController.getViewType(), null);
            }
        }
    }

    private void setMainPane(FragmentTransaction fragmentTransaction, int i, int i2, long j, boolean z) {
        Fragment fragment;
        if (!this.mOnSaveInstanceStateCalled) {
            if (z || this.mCurrentView != i2) {
                boolean z2 = (i2 == 4 || this.mCurrentView == 4) ? false : true;
                FragmentManager fragmentManager = getFragmentManager();
                if (this.mCurrentView == 1) {
                    Fragment fragmentFindFragmentById = fragmentManager.findFragmentById(i);
                    if (fragmentFindFragmentById instanceof AgendaFragment) {
                        ((AgendaFragment) fragmentFindFragmentById).removeFragments(fragmentManager);
                    }
                }
                if (i2 != this.mCurrentView) {
                    if (this.mCurrentView != 5 && this.mCurrentView > 0) {
                        this.mPreviousView = this.mCurrentView;
                    }
                    this.mCurrentView = i2;
                }
                AgendaFragment agendaFragment = null;
                switch (i2) {
                    case 1:
                        if (this.mActionBar != null && this.mActionBar.getSelectedTab() != this.mAgendaTab) {
                            this.mActionBar.selectTab(this.mAgendaTab);
                        }
                        if (this.mActionBarMenuSpinnerAdapter != null) {
                            this.mActionBar.setSelectedNavigationItem(3);
                        }
                        AgendaFragment agendaFragment2 = new AgendaFragment(j, false);
                        ExtensionsFactory.getAnalyticsLogger(getBaseContext()).trackView("agenda");
                        fragment = agendaFragment2;
                        break;
                    case 2:
                        if (this.mActionBar != null && this.mActionBar.getSelectedTab() != this.mDayTab) {
                            this.mActionBar.selectTab(this.mDayTab);
                        }
                        if (this.mActionBarMenuSpinnerAdapter != null) {
                            this.mActionBar.setSelectedNavigationItem(0);
                        }
                        DayFragment dayFragment = new DayFragment(j, 1);
                        ExtensionsFactory.getAnalyticsLogger(getBaseContext()).trackView("day");
                        fragment = dayFragment;
                        break;
                    case 3:
                    default:
                        if (this.mActionBar != null && this.mActionBar.getSelectedTab() != this.mWeekTab) {
                            this.mActionBar.selectTab(this.mWeekTab);
                        }
                        if (this.mActionBarMenuSpinnerAdapter != null) {
                            this.mActionBar.setSelectedNavigationItem(1);
                        }
                        DayFragment dayFragment2 = new DayFragment(j, 7);
                        ExtensionsFactory.getAnalyticsLogger(getBaseContext()).trackView("week");
                        fragment = dayFragment2;
                        break;
                    case 4:
                        if (this.mActionBar != null && this.mActionBar.getSelectedTab() != this.mMonthTab) {
                            this.mActionBar.selectTab(this.mMonthTab);
                        }
                        if (this.mActionBarMenuSpinnerAdapter != null) {
                            this.mActionBar.setSelectedNavigationItem(2);
                        }
                        MonthByWeekFragment monthByWeekFragment = new MonthByWeekFragment(j, false);
                        if (mShowAgendaWithMonth) {
                            agendaFragment = new AgendaFragment(j, false);
                        }
                        ExtensionsFactory.getAnalyticsLogger(getBaseContext()).trackView("month");
                        fragment = monthByWeekFragment;
                        break;
                }
                if (this.mActionBarMenuSpinnerAdapter != null) {
                    this.mActionBarMenuSpinnerAdapter.setMainView(i2);
                    if (!mIsTabletConfig) {
                        this.mActionBarMenuSpinnerAdapter.setTime(j);
                    }
                }
                if (mIsTabletConfig && i2 != 1) {
                    this.mDateRange.setVisibility(0);
                } else {
                    this.mDateRange.setVisibility(8);
                }
                if (i2 != 1) {
                    clearOptionsMenu();
                }
                boolean z3 = false;
                FragmentTransaction fragmentTransactionBeginTransaction = fragmentTransaction;
                if (fragmentTransaction == null) {
                    z3 = true;
                    fragmentTransactionBeginTransaction = fragmentManager.beginTransaction();
                }
                if (z2) {
                    fragmentTransactionBeginTransaction.setTransition(4099);
                }
                fragmentTransactionBeginTransaction.replace(i, fragment);
                if (mShowAgendaWithMonth) {
                    if (agendaFragment != null) {
                        fragmentTransactionBeginTransaction.replace(R.id.secondary_pane, agendaFragment);
                        this.mSecondaryPane.setVisibility(0);
                    } else {
                        this.mSecondaryPane.setVisibility(8);
                        Fragment fragmentFindFragmentById2 = fragmentManager.findFragmentById(R.id.secondary_pane);
                        if (fragmentFindFragmentById2 != null) {
                            fragmentTransactionBeginTransaction.remove(fragmentFindFragmentById2);
                        }
                        this.mController.deregisterEventHandler(Integer.valueOf(R.id.secondary_pane));
                    }
                }
                this.mController.registerEventHandler(i, (CalendarController.EventHandler) fragment);
                if (agendaFragment != null) {
                    this.mController.registerEventHandler(i, agendaFragment);
                }
                if (z3) {
                    fragmentTransactionBeginTransaction.commit();
                }
            }
        }
    }

    private void setTitleInActionBar(CalendarController.EventInfo event) {
        long end;
        if (event.eventType == 1024 && this.mActionBar != null) {
            long start = event.startTime.toMillis(false);
            if (event.endTime != null) {
                end = event.endTime.toMillis(false);
            } else {
                end = start;
            }
            String msg = Utils.formatDateRange(this, start, end, (int) event.extraLong);
            CharSequence oldDate = this.mDateRange.getText();
            this.mDateRange.setText(msg);
            if (event.selectedTime != null) {
                start = event.selectedTime.toMillis(true);
            }
            updateSecondaryTitleFields(start);
            if (!TextUtils.equals(oldDate, msg)) {
                this.mDateRange.sendAccessibilityEvent(8);
                if (this.mShowWeekNum && this.mWeekTextView != null) {
                    this.mWeekTextView.sendAccessibilityEvent(8);
                }
            }
        }
    }

    private void updateSecondaryTitleFields(long visibleMillisSinceEpoch) {
        this.mShowWeekNum = Utils.getShowWeekNumber(this);
        this.mTimeZone = Utils.getTimeZone(this, this.mHomeTimeUpdater);
        if (visibleMillisSinceEpoch != -1) {
            int weekNum = Utils.getWeekNumberFromTime(visibleMillisSinceEpoch, this);
            this.mWeekNum = weekNum;
        }
        if (this.mShowWeekNum && this.mCurrentView == 3 && mIsTabletConfig && this.mWeekTextView != null) {
            String weekString = getResources().getQuantityString(R.plurals.weekN, this.mWeekNum, Integer.valueOf(this.mWeekNum));
            this.mWeekTextView.setText(weekString);
            this.mWeekTextView.setVisibility(0);
        } else if (visibleMillisSinceEpoch != -1 && this.mWeekTextView != null && this.mCurrentView == 2 && mIsTabletConfig) {
            Time time = new Time(this.mTimeZone);
            time.set(visibleMillisSinceEpoch);
            int julianDay = Time.getJulianDay(visibleMillisSinceEpoch, time.gmtoff);
            time.setToNow();
            int todayJulianDay = Time.getJulianDay(time.toMillis(false), time.gmtoff);
            String dayString = Utils.getDayOfWeekString(julianDay, todayJulianDay, visibleMillisSinceEpoch, this);
            this.mWeekTextView.setText(dayString);
            this.mWeekTextView.setVisibility(0);
        } else if (this.mWeekTextView != null && (!mIsTabletConfig || this.mCurrentView != 2)) {
            this.mWeekTextView.setVisibility(8);
        }
        if (this.mHomeTime != null && ((this.mCurrentView == 2 || this.mCurrentView == 3 || this.mCurrentView == 1) && !TextUtils.equals(this.mTimeZone, Time.getCurrentTimezone()))) {
            Time time2 = new Time(this.mTimeZone);
            time2.setToNow();
            long millis = time2.toMillis(true);
            boolean isDST = time2.isDst != 0;
            int flags = 1;
            if (DateFormat.is24HourFormat(this)) {
                flags = 1 | 128;
            }
            String timeString = Utils.formatDateRange(this, millis, millis, flags) + " " + TimeZone.getTimeZone(this.mTimeZone).getDisplayName(isDST, 0, Locale.getDefault());
            this.mHomeTime.setText(timeString);
            this.mHomeTime.setVisibility(0);
            this.mHomeTime.removeCallbacks(this.mHomeTimeUpdater);
            this.mHomeTime.postDelayed(this.mHomeTimeUpdater, 60000 - (millis % 60000));
            return;
        }
        if (this.mHomeTime != null) {
            this.mHomeTime.setVisibility(8);
        }
    }

    @Override
    public long getSupportedEventTypes() {
        return 1058L;
    }

    @Override
    public void handleEvent(CalendarController.EventInfo event) {
        long displayTime = -1;
        if (event.eventType == 32) {
            if ((event.extraLong & 4) != 0) {
                this.mBackToPreviousView = true;
            } else if (event.viewType != this.mController.getPreviousViewType() && event.viewType != 5) {
                this.mBackToPreviousView = false;
            }
            setMainPane(null, R.id.main_pane, event.viewType, event.startTime.toMillis(false), false);
            if (this.mSearchView != null) {
                this.mSearchView.clearFocus();
            }
            if (this.mShowCalendarControls) {
                int animationSize = this.mOrientation == 2 ? this.mControlsAnimateWidth : this.mControlsAnimateHeight;
                boolean noControlsView = event.viewType == 4 || event.viewType == 1;
                if (this.mControlsMenu != null) {
                    this.mControlsMenu.setVisible(!noControlsView);
                    this.mControlsMenu.setEnabled(!noControlsView);
                }
                if (noControlsView || this.mHideControls) {
                    this.mShowSideViews = false;
                    if (!this.mHideControls) {
                        ObjectAnimator slideAnimation = ObjectAnimator.ofInt(this, "controlsOffset", 0, animationSize);
                        slideAnimation.addListener(this.mSlideAnimationDoneListener);
                        slideAnimation.setDuration(this.mCalendarControlsAnimationTime);
                        ObjectAnimator.setFrameDelay(0L);
                        slideAnimation.start();
                    } else {
                        this.mMiniMonth.setVisibility(8);
                        this.mCalendarsList.setVisibility(8);
                        this.mMiniMonthContainer.setVisibility(8);
                    }
                } else {
                    this.mShowSideViews = true;
                    this.mMiniMonth.setVisibility(0);
                    this.mCalendarsList.setVisibility(0);
                    this.mMiniMonthContainer.setVisibility(0);
                    if (!this.mHideControls && (this.mController.getPreviousViewType() == 4 || this.mController.getPreviousViewType() == 1)) {
                        ObjectAnimator slideAnimation2 = ObjectAnimator.ofInt(this, "controlsOffset", animationSize, 0);
                        slideAnimation2.setDuration(this.mCalendarControlsAnimationTime);
                        ObjectAnimator.setFrameDelay(0L);
                        slideAnimation2.start();
                    }
                }
            }
            displayTime = event.selectedTime != null ? event.selectedTime.toMillis(true) : event.startTime.toMillis(true);
            if (!mIsTabletConfig) {
                this.mActionBarMenuSpinnerAdapter.setTime(displayTime);
            }
        } else if (event.eventType == 2) {
            if (this.mCurrentView == 1 && mShowEventDetailsWithAgenda) {
                if (event.startTime != null && event.endTime != null) {
                    if (event.isAllDay()) {
                        Utils.convertAlldayUtcToLocal(event.startTime, event.startTime.toMillis(false), this.mTimeZone);
                        Utils.convertAlldayUtcToLocal(event.endTime, event.endTime.toMillis(false), this.mTimeZone);
                    }
                    this.mController.sendEvent(this, 32L, event.startTime, event.endTime, event.selectedTime, event.id, 1, 2L, null, null);
                } else if (event.selectedTime != null) {
                    this.mController.sendEvent(this, 32L, event.selectedTime, event.selectedTime, event.id, 1);
                }
            } else {
                if (event.selectedTime != null && this.mCurrentView != 1) {
                    this.mController.sendEvent(this, 32L, event.selectedTime, event.selectedTime, -1L, 0);
                }
                int response = event.getResponse();
                if ((this.mCurrentView == 1 && this.mShowEventInfoFullScreenAgenda) || ((this.mCurrentView == 2 || this.mCurrentView == 3 || this.mCurrentView == 4) && this.mShowEventInfoFullScreen)) {
                    Intent intent = new Intent("android.intent.action.VIEW");
                    Uri eventUri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, event.id);
                    intent.setData(eventUri);
                    intent.setClass(this, EventInfoActivity.class);
                    intent.setFlags(537001984);
                    intent.putExtra("beginTime", event.startTime.toMillis(false));
                    intent.putExtra("endTime", event.endTime.toMillis(false));
                    intent.putExtra("attendeeStatus", response);
                    startActivity(intent);
                } else {
                    EventInfoFragment fragment = new EventInfoFragment((Context) this, event.id, event.startTime.toMillis(false), event.endTime.toMillis(false), response, true, 1, (ArrayList<CalendarEventModel.ReminderEntry>) null);
                    fragment.setDialogParams(event.x, event.y, this.mActionBar.getHeight());
                    FragmentManager fm = getFragmentManager();
                    FragmentTransaction ft = fm.beginTransaction();
                    Fragment fOld = fm.findFragmentByTag("EventInfoFragment");
                    if (fOld != null && fOld.isAdded()) {
                        ft.remove(fOld);
                    }
                    ft.add(fragment, "EventInfoFragment");
                    ft.commit();
                }
            }
            displayTime = event.startTime.toMillis(true);
        } else if (event.eventType == 1024) {
            setTitleInActionBar(event);
            if (!mIsTabletConfig) {
                this.mActionBarMenuSpinnerAdapter.setTime(this.mController.getTime());
            }
        }
        updateSecondaryTitleFields(displayTime);
    }

    public void handleSelectSyncedCalendarsClicked(View v) {
        this.mController.sendEvent(this, 64L, null, null, null, 0L, 0, 2L, null, null);
    }

    public void eventsChanged() {
        this.mController.sendEvent(this, 128L, null, null, -1L, 0);
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        return false;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        this.mSearchMenu.collapseActionView();
        this.mController.sendEvent(this, 256L, null, null, -1L, 0, 0L, query, getComponentName());
        return true;
    }

    @Override
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft) {
        Log.w("AllInOneActivity", "TabSelected AllInOne=" + this + " finishing:" + isFinishing());
        if (tab == this.mDayTab && this.mCurrentView != 2) {
            this.mController.sendEvent(this, 32L, null, null, -1L, 2);
            return;
        }
        if (tab == this.mWeekTab && this.mCurrentView != 3) {
            this.mController.sendEvent(this, 32L, null, null, -1L, 3);
            return;
        }
        if (tab == this.mMonthTab && this.mCurrentView != 4) {
            this.mController.sendEvent(this, 32L, null, null, -1L, 4);
        } else if (tab == this.mAgendaTab && this.mCurrentView != 1) {
            this.mController.sendEvent(this, 32L, null, null, -1L, 1);
        } else {
            Log.w("AllInOneActivity", "TabSelected event from unknown tab: " + ((Object) (tab == null ? "null" : tab.getText())));
            Log.w("AllInOneActivity", "CurrentView:" + this.mCurrentView + " Tab:" + tab.toString() + " Day:" + this.mDayTab + " Week:" + this.mWeekTab + " Month:" + this.mMonthTab + " Agenda:" + this.mAgendaTab);
        }
    }

    @Override
    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction ft) {
    }

    @Override
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction ft) {
    }

    @Override
    public boolean onNavigationItemSelected(int itemPosition, long itemId) {
        switch (itemPosition) {
            case 0:
                if (this.mCurrentView != 2) {
                    this.mController.sendEvent(this, 32L, null, null, -1L, 2);
                }
                break;
            case 1:
                if (this.mCurrentView != 3) {
                    this.mController.sendEvent(this, 32L, null, null, -1L, 3);
                }
                break;
            case 2:
                if (this.mCurrentView != 4) {
                    this.mController.sendEvent(this, 32L, null, null, -1L, 4);
                }
                break;
            case 3:
                if (this.mCurrentView != 1) {
                    this.mController.sendEvent(this, 32L, null, null, -1L, 1);
                }
                break;
            default:
                Log.w("AllInOneActivity", "ItemSelected event from unknown button: " + itemPosition);
                Log.w("AllInOneActivity", "CurrentView:" + this.mCurrentView + " Button:" + itemPosition + " Day:" + this.mDayTab + " Week:" + this.mWeekTab + " Month:" + this.mMonthTab + " Agenda:" + this.mAgendaTab);
                break;
        }
        return false;
    }

    @Override
    public boolean onSuggestionSelect(int position) {
        return false;
    }

    @Override
    public boolean onSuggestionClick(int position) {
        this.mSearchMenu.collapseActionView();
        return false;
    }

    @Override
    public boolean onSearchRequested() {
        if (this.mSearchMenu != null) {
            this.mSearchMenu.expandActionView();
            return false;
        }
        return false;
    }
}
