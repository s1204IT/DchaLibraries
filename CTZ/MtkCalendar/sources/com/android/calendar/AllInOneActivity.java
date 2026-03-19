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
import android.widget.Toast;
import com.android.calendar.CalendarController;
import com.android.calendar.CalendarEventModel;
import com.android.calendar.agenda.AgendaFragment;
import com.android.calendar.alerts.AlertUtils;
import com.android.calendar.month.MonthByWeekFragment;
import com.android.calendar.selectcalendars.SelectVisibleCalendarsFragment;
import com.android.calendar.widget.CalendarAppWidgetService;
import com.android.datetimepicker.date.DatePickerDialog;
import com.mediatek.calendar.LogUtil;
import com.mediatek.calendar.PDebug;
import com.mediatek.calendar.extension.ExtensionFactory;
import com.mediatek.calendar.extension.IOptionsMenuExt;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
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
    private Bundle mBundleIcicleOncreate;
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
    private DatePickerDialog mGotoDatePickerDialog;
    private QueryHandler mHandler;
    private String mHideString;
    private TextView mHomeTime;
    private View mMiniMonth;
    private View mMiniMonthContainer;
    private ActionBar.Tab mMonthTab;
    private Intent mNewtent;
    private int mOnCreateRequestPermissionFlag;
    private Menu mOptionsMenu;
    private IOptionsMenuExt mOptionsMenuExt;
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
    private static float mScale = 0.0f;
    private static final String[] STORAGE_PERMISSION = {"android.permission.READ_EXTERNAL_STORAGE"};
    private static final String[] CALENDAR_PERMISSION = {"android.permission.READ_CALENDAR", "android.permission.WRITE_CALENDAR"};
    private static final String[] CONTACTS_PERMISSION = {"android.permission.READ_CONTACTS"};
    private static boolean mIsClearEventsCompleted = false;
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
    private boolean mIsInSearchMode = false;
    private String mSearchString = null;
    private boolean mCheckForAccounts = true;
    private AllInOneMenuExtensionsInterface mExtensions = ExtensionsFactory.getAllInOneMenuExtensions();
    private final Animator.AnimatorListener mSlideAnimationDoneListener = new Animator.AnimatorListener() {
        @Override
        public void onAnimationCancel(Animator animator) {
        }

        @Override
        public void onAnimationEnd(Animator animator) {
            int i = AllInOneActivity.this.mShowSideViews ? 0 : 8;
            AllInOneActivity.this.mMiniMonth.setVisibility(i);
            AllInOneActivity.this.mCalendarsList.setVisibility(i);
            AllInOneActivity.this.mMiniMonthContainer.setVisibility(i);
        }

        @Override
        public void onAnimationRepeat(Animator animator) {
        }

        @Override
        public void onAnimationStart(Animator animator) {
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
        public void onChange(boolean z) {
            AllInOneActivity.this.eventsChanged();
        }
    };
    DatePickerDialog.OnDateSetListener mGotoDateSetlistener = new DatePickerDialog.OnDateSetListener() {
        @Override
        public void onDateSet(DatePickerDialog datePickerDialog, int i, int i2, int i3) {
            LogUtil.d("AllInOneActivity", "date set: " + i + "-" + (i2 + 1) + "-" + i3);
            Time time = new Time(AllInOneActivity.this.mTimeZone);
            time.year = i;
            time.month = i2;
            time.monthDay = i3;
            AllInOneActivity.this.mController.sendEvent(this, 32L, time, null, time, -1L, 0, 10L, null, null);
        }

        @Override
        public void onCancelDialog() {
            Log.d("AllInOneActivity", "do nothing");
        }
    };

    private class QueryHandler extends AsyncQueryHandler {
        public QueryHandler(ContentResolver contentResolver) {
            super(contentResolver);
        }

        @Override
        protected void onQueryComplete(int i, Object obj, Cursor cursor) {
            AllInOneActivity.this.mCheckForAccounts = false;
            if (cursor != null) {
                try {
                    if (cursor.getCount() <= 0) {
                        if (!AllInOneActivity.this.isFinishing()) {
                            if (cursor != null) {
                                cursor.close();
                            }
                            Bundle bundle = new Bundle();
                            bundle.putCharSequence("introMessage", AllInOneActivity.this.getResources().getString(R.string.create_an_account_desc));
                            bundle.putBoolean("allowSkip", true);
                            AccountManager.get(AllInOneActivity.this).addAccount("com.google", "com.android.calendar", null, bundle, AllInOneActivity.this, new AccountManagerCallback<Bundle>() {
                                @Override
                                public void run(AccountManagerFuture<Bundle> accountManagerFuture) {
                                    if (accountManagerFuture.isCancelled()) {
                                        return;
                                    }
                                    try {
                                        if (accountManagerFuture.getResult().getBoolean("setupSkipped")) {
                                            Utils.setSharedPreference((Context) AllInOneActivity.this, "preferences_skip_setup", true);
                                        }
                                    } catch (AuthenticatorException e) {
                                    } catch (OperationCanceledException e2) {
                                    } catch (IOException e3) {
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

    private void continueNewIntent() {
        if ("android.intent.action.VIEW".equals(this.mNewtent.getAction()) && !this.mNewtent.getBooleanExtra("KEY_HOME", false)) {
            long viewAction = parseViewAction(this.mNewtent);
            if (viewAction == -1) {
                viewAction = Utils.timeFromIntentInMillis(this.mNewtent);
            }
            if (viewAction != -1 && this.mViewEventId == -1 && this.mController != null) {
                Time time = new Time(this.mTimeZone);
                time.set(viewAction);
                time.normalize(true);
                this.mController.sendEvent(this, 32L, time, time, -1L, 0);
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        this.mNewtent = intent;
        Log.d("AllInOneActivity", "onNewIntent");
        if (this.mOnCreateRequestPermissionFlag == 1) {
            if (checkPermissions(2) != null) {
                Log.d("AllInOneActivity", "onNewIntent permission denied");
                return;
            } else {
                Log.d("AllInOneActivity", "onNewIntent continued");
                continueNewIntent();
                return;
            }
        }
        Log.d("AllInOneActivity", "onNewIntent no oncreatepermission so request");
        if (checkAndRequestPermission(2)) {
            this.mOnCreateRequestPermissionFlag = 1;
            Log.d("AllInOneActivity", "onNewIntent no oncreatepermission but now has all permission");
            continueonCreateCalendar();
            if (this.mNewtent != null) {
                continueNewIntent();
                this.mNewtent = null;
            }
            continueOnResume();
        }
    }

    private void continueonCreateCalendar() {
        long viewAction;
        int viewTypeFromIntentAndSharedPref;
        long jTimeFromIntentInMillis;
        FragmentManager fragmentManager;
        Fragment fragmentFindFragmentByTag;
        Log.d("AllInOneActivity", "continueonCreateCalendar ");
        if (this.mBundleIcicleOncreate != null) {
            if (this.mBundleIcicleOncreate.containsKey("key_check_for_accounts")) {
                this.mCheckForAccounts = this.mBundleIcicleOncreate.getBoolean("key_check_for_accounts");
            }
            if (this.mBundleIcicleOncreate.containsKey("key_search_mode")) {
                this.mIsInSearchMode = this.mBundleIcicleOncreate.getBoolean("key_search_mode", false);
            }
            if (this.mBundleIcicleOncreate.containsKey("key_search_string")) {
                this.mSearchString = this.mBundleIcicleOncreate.getString("key_search_string", null);
            }
        }
        if (this.mCheckForAccounts && !Utils.getSharedPreference((Context) this, "preferences_skip_setup", false)) {
            this.mHandler = new QueryHandler(getContentResolver());
            this.mHandler.startQuery(0, null, CalendarContract.Calendars.CONTENT_URI, new String[]{"_id"}, null, null, null);
        }
        this.mController = CalendarController.getInstance(this);
        Intent intent = getIntent();
        if (this.mBundleIcicleOncreate != null) {
            jTimeFromIntentInMillis = this.mBundleIcicleOncreate.getLong("key_restore_time");
            viewTypeFromIntentAndSharedPref = this.mBundleIcicleOncreate.getInt("key_restore_view", -1);
        } else {
            if ("android.intent.action.VIEW".equals(intent.getAction())) {
                viewAction = parseViewAction(intent);
            } else {
                viewAction = -1;
            }
            if (viewAction == -1) {
                jTimeFromIntentInMillis = Utils.timeFromIntentInMillis(intent);
                viewTypeFromIntentAndSharedPref = -1;
            } else {
                viewTypeFromIntentAndSharedPref = -1;
                jTimeFromIntentInMillis = viewAction;
            }
        }
        if (viewTypeFromIntentAndSharedPref == -1 || viewTypeFromIntentAndSharedPref > 5) {
            viewTypeFromIntentAndSharedPref = Utils.getViewTypeFromIntentAndSharedPref(this);
        }
        this.mTimeZone = Utils.getTimeZone(this, this.mHomeTimeUpdater);
        new Time(this.mTimeZone).set(jTimeFromIntentInMillis);
        PDebug.EndAndStart("AllInOneActivity.onCreate.restoreState", "AllInOneActivity.onCreate.initVariables");
        Resources resources = getResources();
        this.mHideString = resources.getString(R.string.hide_controls);
        this.mShowString = resources.getString(R.string.show_controls);
        this.mOrientation = resources.getConfiguration().orientation;
        if (this.mOrientation == 2) {
            this.mControlsAnimateWidth = (int) resources.getDimension(R.dimen.calendar_controls_width);
            if (this.mControlsParams == null) {
                this.mControlsParams = new RelativeLayout.LayoutParams(this.mControlsAnimateWidth, 0);
            }
            this.mControlsParams.addRule(11);
        } else {
            this.mControlsAnimateWidth = Math.max((resources.getDisplayMetrics().widthPixels * 45) / 100, (int) resources.getDimension(R.dimen.min_portrait_calendar_controls_width));
            this.mControlsAnimateWidth = Math.min(this.mControlsAnimateWidth, (int) resources.getDimension(R.dimen.max_portrait_calendar_controls_width));
        }
        this.mControlsAnimateHeight = (int) resources.getDimension(R.dimen.calendar_controls_height);
        this.mHideControls = !Utils.getSharedPreference((Context) this, "preferences_show_controls", true);
        mIsMultipane = Utils.getConfigBool(this, R.bool.multiple_pane_config);
        mIsTabletConfig = Utils.getConfigBool(this, R.bool.tablet_config);
        mShowAgendaWithMonth = Utils.getConfigBool(this, R.bool.show_agenda_with_month);
        this.mShowCalendarControls = Utils.getConfigBool(this, R.bool.show_calendar_controls);
        mShowEventDetailsWithAgenda = Utils.getConfigBool(this, R.bool.show_event_details_with_agenda);
        this.mShowEventInfoFullScreenAgenda = Utils.getConfigBool(this, R.bool.agenda_show_event_info_full_screen);
        this.mShowEventInfoFullScreen = Utils.getConfigBool(this, R.bool.show_event_info_full_screen);
        this.mCalendarControlsAnimationTime = resources.getInteger(R.integer.calendar_controls_animation_time);
        Utils.setAllowWeekForDetailView(mIsMultipane);
        PDebug.EndAndStart("AllInOneActivity.onCreate.initVariables", "AllInOneActivity.onCreate.setContentView");
        setContentView(R.layout.all_in_one);
        if (mIsTabletConfig) {
            this.mDateRange = (TextView) findViewById(R.id.date_bar);
            this.mWeekTextView = (TextView) findViewById(R.id.week_num);
        } else {
            this.mDateRange = (TextView) getLayoutInflater().inflate(R.layout.date_range_title, (ViewGroup) null);
        }
        PDebug.EndAndStart("AllInOneActivity.onCreate.setContentView", "AllInOneActivity.onCreate.configureActionBar");
        configureActionBar(viewTypeFromIntentAndSharedPref);
        PDebug.EndAndStart("AllInOneActivity.onCreate.configureActionBar", "AllInOneActivity.onCreate.getViews");
        this.mHomeTime = (TextView) findViewById(R.id.home_time);
        this.mMiniMonth = findViewById(R.id.mini_month);
        if (mIsTabletConfig && this.mOrientation == 1) {
            this.mMiniMonth.setLayoutParams(new RelativeLayout.LayoutParams(this.mControlsAnimateWidth, this.mControlsAnimateHeight));
        }
        this.mCalendarsList = findViewById(R.id.calendar_list);
        this.mMiniMonthContainer = findViewById(R.id.mini_month_container);
        this.mSecondaryPane = findViewById(R.id.secondary_pane);
        this.mController.registerFirstEventHandler(0, this);
        PDebug.End("AllInOneActivity.onCreate.getViews");
        initFragments(jTimeFromIntentInMillis, viewTypeFromIntentAndSharedPref, this.mBundleIcicleOncreate);
        GeneralPreferences.getSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);
        this.mContentResolver = getContentResolver();
        this.mOptionsMenuExt = ExtensionFactory.getAllInOneOptionMenuExt(this);
        boolean configBool = Utils.getConfigBool(this, R.bool.tablet_config);
        if (this.mBundleIcicleOncreate != null && !configBool && (fragmentFindFragmentByTag = (fragmentManager = getFragmentManager()).findFragmentByTag("EventInfoFragment")) != null) {
            FragmentTransaction fragmentTransactionBeginTransaction = fragmentManager.beginTransaction();
            fragmentTransactionBeginTransaction.remove(fragmentFindFragmentByTag);
            fragmentTransactionBeginTransaction.commit();
            long j = this.mBundleIcicleOncreate.getLong("key_event_id");
            long j2 = this.mBundleIcicleOncreate.getLong("key_start_millis");
            long j3 = this.mBundleIcicleOncreate.getLong("key_end_millis");
            CalendarController.getInstance(this).sendEventRelatedEvent(this, 2L, j, j2, j3, 0, 0, j2);
            Log.i("AllInOneActivity", "f=" + fragmentFindFragmentByTag + ", eventId=" + j + ", startMillis=" + j2 + ", endMillis=" + j3);
        }
        PDebug.EndAndStart("AllInOneActivity.onCreate", "AllInOneActivity.onCreate->DayFragment.onCreate");
    }

    protected boolean hasRequiredPermission(String[] strArr) {
        for (String str : strArr) {
            if (checkSelfPermission(str) != 0) {
                return false;
            }
        }
        return true;
    }

    private String[] checkPermissions(int i) {
        boolean z;
        ArrayList arrayList = new ArrayList();
        if (hasRequiredPermission(CALENDAR_PERMISSION)) {
            z = false;
        } else {
            arrayList.add(CALENDAR_PERMISSION[0]);
            arrayList.add(CALENDAR_PERMISSION[1]);
            z = true;
        }
        if (!hasRequiredPermission(STORAGE_PERMISSION)) {
            arrayList.add(STORAGE_PERMISSION[0]);
            z = true;
        }
        if (!hasRequiredPermission(CONTACTS_PERMISSION)) {
            arrayList.add(CONTACTS_PERMISSION[0]);
            z = true;
        }
        if (z) {
            return (String[]) arrayList.toArray(new String[arrayList.size()]);
        }
        return null;
    }

    private boolean checkAndRequestPermission(int i) {
        String[] strArrCheckPermissions = checkPermissions(i);
        if (strArrCheckPermissions != null) {
            Log.d("AllInOneActivity", "checkAndRequestPermission : requesting " + Arrays.toString(strArrCheckPermissions));
            requestPermissions(strArrCheckPermissions, i);
            return false;
        }
        Log.d("AllInOneActivity", "checkAndRequestPermission : Granted ");
        return true;
    }

    @Override
    protected void onCreate(Bundle bundle) {
        Log.d("AllInOneActivity", "onCreate before permission check ");
        PDebug.Start("AllInOneActivity.onCreate");
        PDebug.Start("AllInOneActivity.onCreate.superOnCreate");
        if (Utils.getSharedPreference((Context) this, "preferences_tardis_1", false)) {
            setTheme(R.style.CalendarTheme_WithActionBarWallpaper);
        }
        if (bundle != null && checkPermissions(1) != null) {
            bundle = null;
        }
        super.onCreate(bundle);
        PDebug.EndAndStart("AllInOneActivity.onCreate.superOnCreate", "AllInOneActivity.onCreate.restoreState");
        this.mBundleIcicleOncreate = bundle;
        if (!checkAndRequestPermission(1)) {
            this.mOnCreateRequestPermissionFlag = 2;
            return;
        }
        this.mOnCreateRequestPermissionFlag = 1;
        CalendarAppWidgetService.CALENDAR_PERMISSION_GRANTED = true;
        continueonCreateCalendar();
    }

    @Override
    public void onRequestPermissionsResult(int i, String[] strArr, int[] iArr) {
        Log.d("AllInOneActivity", "onRequestPermissionsResult " + Arrays.toString(strArr));
        Log.d("AllInOneActivity", "onRequestPermissionsResult Requestcode[" + i + "]");
        if (strArr.length == 0) {
        }
        for (int i2 = 0; i2 < strArr.length; i2++) {
            if (iArr[i2] != 0) {
                Toast.makeText(getApplicationContext(), getResources().getString(R.string.denied_required_permission), 1).show();
                finish();
                CalendarAppWidgetService.CALENDAR_PERMISSION_GRANTED = false;
                return;
            }
        }
        CalendarAppWidgetService.CALENDAR_PERMISSION_GRANTED = true;
        switch (i) {
            case 1:
                this.mOnCreateRequestPermissionFlag = 1;
                continueonCreateCalendar();
                continueOnResume();
                break;
            case 2:
                Log.d("AllInOneActivity", "CALENDAR_ONNEWINTENT_PERMISSIONS_REQUEST_CODE");
                this.mOnCreateRequestPermissionFlag = 1;
                continueonCreateCalendar();
                if (this.mNewtent != null) {
                    continueNewIntent();
                    this.mNewtent = null;
                }
                continueOnResume();
                break;
            case 3:
                if (this.mNewtent != null) {
                    continueNewIntent();
                    this.mNewtent = null;
                }
                continueOnResume();
                break;
        }
    }

    private long parseViewAction(Intent intent) {
        Uri data = intent.getData();
        if (data == null || !data.isHierarchical()) {
            return -1L;
        }
        List<String> pathSegments = data.getPathSegments();
        if (pathSegments.size() != 2 || !pathSegments.get(0).equals("events")) {
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
            return this.mIntentEventStartMillis;
        } catch (NumberFormatException e) {
            return -1L;
        }
    }

    private void configureActionBar(int i) {
        createButtonsSpinner(i, mIsTabletConfig);
        if (mIsMultipane) {
            this.mActionBar.setDisplayOptions(18);
        } else {
            this.mActionBar.setDisplayOptions(0);
        }
    }

    private void createButtonsSpinner(int i, boolean z) {
        this.mActionBarMenuSpinnerAdapter = new CalendarViewAdapter(this, i, !z);
        this.mActionBar = getActionBar();
        this.mActionBar.setNavigationMode(1);
        this.mActionBar.setListNavigationCallbacks(this.mActionBarMenuSpinnerAdapter, this);
        switch (i) {
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
        MenuItem menuItemFindItem;
        if (this.mOptionsMenu != null && (menuItemFindItem = this.mOptionsMenu.findItem(R.id.action_cancel)) != null) {
            menuItemFindItem.setVisible(false);
        }
    }

    private void continueOnResume() {
        AllInOneActivity allInOneActivity;
        PDebug.EndAndStart("DayFragment.onCreateView->AllInOneActivity.onResume", "AllInOneActivity.onResume");
        PDebug.Start("AllInOneActivity.onResume.superOnResume");
        PDebug.EndAndStart("AllInOneActivity.onResume.superOnResume", "AllInOneActivity.onResume.updateTitle");
        if (this.mOnSaveInstanceStateCalled && this.mController != null && mIsClearEventsCompleted) {
            mIsClearEventsCompleted = false;
            if (this.mCurrentView == 4) {
                this.mController.sendEvent(this, 128L, null, null, -1L, this.mCurrentView);
                LogUtil.v("AllInOneActivity", "After CLEAR EVENTS COMPLETED, send Event EVENTS_CHANGED.");
            }
        }
        Utils.trySyncAndDisableUpgradeReceiver(this);
        this.mController.registerFirstEventHandler(0, this);
        this.mOnSaveInstanceStateCalled = false;
        this.mContentResolver.registerContentObserver(CalendarContract.Events.CONTENT_URI, true, this.mObserver);
        if (this.mUpdateOnResume) {
            initFragments(this.mController.getTime(), this.mController.getViewType(), null);
            this.mUpdateOnResume = false;
        }
        Time time = new Time(this.mTimeZone);
        time.set(this.mController.getTime());
        this.mController.sendEvent(this, 1024L, time, time, -1L, 0, this.mController.getDateFlags(), null, null);
        if (this.mActionBarMenuSpinnerAdapter != null) {
            this.mActionBarMenuSpinnerAdapter.refresh(this);
        }
        if (this.mControlsMenu != null) {
            this.mControlsMenu.setTitle(this.mHideControls ? this.mShowString : this.mHideString);
        }
        this.mPaused = false;
        PDebug.EndAndStart("AllInOneActivity.onResume.updateTitle", "AllInOneActivity.onResume.invalidateOptionsMenu");
        if (this.mViewEventId != -1 && this.mIntentEventStartMillis != -1 && this.mIntentEventEndMillis != -1) {
            long jCurrentTimeMillis = System.currentTimeMillis();
            this.mController.sendEventRelatedEventWithExtra(this, 2L, this.mViewEventId, this.mIntentEventStartMillis, this.mIntentEventEndMillis, -1, -1, CalendarController.EventInfo.buildViewExtraLong(this.mIntentAttendeeResponse, this.mIntentAllDay), (jCurrentTimeMillis <= this.mIntentEventStartMillis || jCurrentTimeMillis >= this.mIntentEventEndMillis) ? -1L : jCurrentTimeMillis);
            allInOneActivity = this;
            allInOneActivity.mViewEventId = -1L;
            allInOneActivity.mIntentEventStartMillis = -1L;
            allInOneActivity.mIntentEventEndMillis = -1L;
            allInOneActivity.mIntentAllDay = false;
        } else {
            allInOneActivity = this;
        }
        Utils.setMidnightUpdater(allInOneActivity.mHandler, allInOneActivity.mTimeChangesUpdater, allInOneActivity.mTimeZone);
        invalidateOptionsMenu();
        PDebug.End("AllInOneActivity.onResume.invalidateOptionsMenu");
        allInOneActivity.mCalIntentReceiver = Utils.setTimeChangesReceiver(allInOneActivity, allInOneActivity.mTimeChangesUpdater);
        resetGotoDateSetListener();
        PDebug.EndAndStart("AllInOneActivity.onResume", "AllInOneActivity.onResume->DayFragment.onResume");
    }

    @Override
    protected void onResume() {
        Log.d("AllInOneActivity", "onResume ");
        super.onResume();
        if (this.mOnCreateRequestPermissionFlag != 1 || !checkAndRequestPermission(3)) {
            return;
        }
        Log.d("AllInOneActivity", "Resume continued");
        continueOnResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (this.mOnCreateRequestPermissionFlag == 1 && checkPermissions(1) == null) {
            this.mController.deregisterEventHandler(0);
            this.mPaused = true;
            this.mHomeTime.removeCallbacks(this.mHomeTimeUpdater);
            if (this.mActionBarMenuSpinnerAdapter != null) {
                this.mActionBarMenuSpinnerAdapter.onPause();
            }
            this.mContentResolver.unregisterContentObserver(this.mObserver);
            if (isFinishing()) {
                GeneralPreferences.getSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(this);
            }
            if (this.mController.getViewType() != 5) {
                Utils.setDefaultView(this, this.mController.getViewType());
            }
            Utils.resetMidnightUpdater(this.mHandler, this.mTimeChangesUpdater);
            if (this.mCalIntentReceiver != null) {
                Utils.clearTimeChangesReceiver(this, this.mCalIntentReceiver);
            }
        }
    }

    @Override
    protected void onUserLeaveHint() {
        if (this.mOnCreateRequestPermissionFlag == 1 && checkPermissions(1) == null) {
            this.mController.sendEvent(this, 512L, null, null, -1L, 0);
        }
        super.onUserLeaveHint();
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        if (this.mOnCreateRequestPermissionFlag == 1 && checkPermissions(1) == null) {
            this.mOnSaveInstanceStateCalled = true;
            bundle.putLong("key_restore_time", this.mController.getTime());
            bundle.putInt("key_restore_view", this.mCurrentView);
            if (this.mCurrentView == 5) {
                bundle.putLong("key_event_id", this.mController.getEventId());
            } else if (this.mCurrentView == 1) {
                ?? FindFragmentById = getFragmentManager().findFragmentById(R.id.main_pane);
                if (FindFragmentById instanceof AgendaFragment) {
                    bundle.putLong("key_event_id", FindFragmentById.getLastShowEventId());
                }
            }
            bundle.putBoolean("key_check_for_accounts", this.mCheckForAccounts);
            if (this.mSearchMenu == null || !this.mSearchMenu.isActionViewExpanded()) {
                this.mIsInSearchMode = false;
            } else {
                this.mIsInSearchMode = true;
                this.mSearchString = this.mSearchView != null ? this.mSearchView.getQuery().toString() : null;
                bundle.putString("key_search_string", this.mSearchString);
            }
            bundle.putBoolean("key_search_mode", this.mIsInSearchMode);
            Fragment fragmentFindFragmentByTag = getFragmentManager().findFragmentByTag("EventInfoFragment");
            if (fragmentFindFragmentByTag != null) {
                EventInfoFragment eventInfoFragment = (EventInfoFragment) fragmentFindFragmentByTag;
                bundle.putLong("key_event_id", eventInfoFragment.getEventId());
                bundle.putLong("key_start_millis", eventInfoFragment.getStartMillis());
                bundle.putLong("key_end_millis", eventInfoFragment.getEndMillis());
                Log.i("AllInOneActivity", "eventId= " + eventInfoFragment.getEventId() + ", startMillis= " + eventInfoFragment.getStartMillis() + ", endMillis= " + eventInfoFragment.getEndMillis());
            }
        }
        super.onSaveInstanceState(bundle);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        GeneralPreferences.getSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(this);
        if (this.mOnCreateRequestPermissionFlag == 1 && checkPermissions(1) == null) {
            this.mController.deregisterAllEventHandlers();
        }
        CalendarController.removeInstance(this);
    }

    private void initFragments(long j, int i, Bundle bundle) {
        long j2;
        long longExtra;
        long longExtra2;
        PDebug.Start("AllInOneActivity.initFragments");
        FragmentTransaction fragmentTransactionBeginTransaction = getFragmentManager().beginTransaction();
        if (this.mShowCalendarControls) {
            MonthByWeekFragment monthByWeekFragment = new MonthByWeekFragment(j, true);
            fragmentTransactionBeginTransaction.replace(R.id.mini_month, monthByWeekFragment);
            this.mController.registerEventHandler(R.id.mini_month, monthByWeekFragment);
            SelectVisibleCalendarsFragment selectVisibleCalendarsFragment = new SelectVisibleCalendarsFragment();
            fragmentTransactionBeginTransaction.replace(R.id.calendar_list, selectVisibleCalendarsFragment);
            this.mController.registerEventHandler(R.id.calendar_list, selectVisibleCalendarsFragment);
        }
        if (!this.mShowCalendarControls || i == 5) {
            this.mMiniMonth.setVisibility(8);
            this.mCalendarsList.setVisibility(8);
        }
        if (i == 5) {
            this.mPreviousView = GeneralPreferences.getSharedPreferences(this).getInt("preferred_startView", 3);
            Intent intent = getIntent();
            Uri data = intent.getData();
            if (data != null) {
                try {
                    j2 = Long.parseLong(data.getLastPathSegment());
                } catch (NumberFormatException e) {
                    j2 = -1;
                }
                longExtra = intent.getLongExtra("beginTime", -1L);
                longExtra2 = intent.getLongExtra("endTime", -1L);
                CalendarController.EventInfo eventInfo = new CalendarController.EventInfo();
                if (longExtra2 != -1) {
                    eventInfo.endTime = new Time();
                    eventInfo.endTime.set(longExtra2);
                }
                if (longExtra != -1) {
                    eventInfo.startTime = new Time();
                    eventInfo.startTime.set(longExtra);
                }
                eventInfo.id = j2;
                this.mController.setViewType(i);
                this.mController.setEventId(j2);
            } else {
                if (bundle != null && bundle.containsKey("key_event_id")) {
                    j2 = bundle.getLong("key_event_id");
                } else {
                    j2 = -1;
                }
                longExtra = intent.getLongExtra("beginTime", -1L);
                longExtra2 = intent.getLongExtra("endTime", -1L);
                CalendarController.EventInfo eventInfo2 = new CalendarController.EventInfo();
                if (longExtra2 != -1) {
                }
                if (longExtra != -1) {
                }
                eventInfo2.id = j2;
                this.mController.setViewType(i);
                this.mController.setEventId(j2);
            }
        } else if (this.mCurrentView != i) {
            this.mPreviousView = i;
        } else {
            LogUtil.v("AllInOneActivity", "don't modify mPreviousView's value.mCurrentView:" + this.mCurrentView + ",viewType:" + i + ",mPreviousView:" + this.mPreviousView);
        }
        setMainPane(fragmentTransactionBeginTransaction, R.id.main_pane, i, j, true);
        fragmentTransactionBeginTransaction.commit();
        Time time = new Time(this.mTimeZone);
        time.set(j);
        if (i == 1 && bundle != null) {
            this.mController.sendEvent(this, 32L, time, null, bundle.getLong("key_event_id", -1L), i);
        } else if (i != 5) {
            this.mController.sendEvent(this, 32L, time, null, -1L, i);
        }
        PDebug.End("AllInOneActivity.initFragments");
    }

    @Override
    public void onBackPressed() {
        if (this.mOnCreateRequestPermissionFlag == 1 && checkPermissions(1) == null) {
            if (this.mCurrentView == 5 || this.mBackToPreviousView) {
                this.mController.sendEvent(this, 32L, null, null, -1L, this.mPreviousView);
                return;
            } else {
                super.onBackPressed();
                return;
            }
        }
        super.onBackPressed();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        if (this.mOnCreateRequestPermissionFlag == 1 && checkPermissions(1) == null) {
            this.mOptionsMenu = menu;
            getMenuInflater().inflate(R.menu.all_in_one_title_bar, menu);
            Integer extensionMenuResource = this.mExtensions.getExtensionMenuResource(menu);
            if (extensionMenuResource != null) {
                getMenuInflater().inflate(extensionMenuResource.intValue(), menu);
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
            MenuItem menuItemFindItem = menu.findItem(R.id.action_today);
            if (Utils.isJellybeanOrLater()) {
                Utils.setTodayIcon((LayerDrawable) menuItemFindItem.getIcon(), this, this.mTimeZone);
            } else {
                menuItemFindItem.setIcon(R.drawable.ic_menu_today_no_date_holo_light);
            }
            this.mOptionsMenuExt.onCreateOptionsMenu(menu);
            if (this.mIsInSearchMode) {
                enterSearchMode();
                if (this.mSearchView != null) {
                    this.mSearchView.setQuery(this.mSearchString, false);
                }
            }
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() != R.id.action_search) {
            exitSearchMode();
        } else {
            enterSearchMode();
        }
        int itemId = menuItem.getItemId();
        if (itemId == R.id.action_refresh) {
            this.mController.refreshCalendars();
            return true;
        }
        if (itemId == R.id.action_today) {
            Time time = new Time(this.mTimeZone);
            time.setToNow();
            this.mController.sendEvent(this, 32L, time, null, time, -1L, 0, 10L, null, null);
            return true;
        }
        if (itemId == R.id.action_create_event) {
            Time time2 = new Time();
            if (this.mController.getViewType() == 4) {
                time2.setToNow();
            } else {
                time2.set(this.mController.getTime());
            }
            time2.second = 0;
            if (time2.minute > 30) {
                time2.hour++;
                time2.minute = 0;
            } else if (time2.minute > 0 && time2.minute < 30) {
                time2.minute = 30;
            }
            this.mController.sendEventRelatedEvent(this, 1L, -1L, time2.toMillis(true), 0L, 0, 0, -1L);
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
            menuItem.setTitle(this.mHideControls ? this.mShowString : this.mHideString);
            if (!this.mHideControls) {
                this.mMiniMonth.setVisibility(0);
                this.mCalendarsList.setVisibility(0);
                this.mMiniMonthContainer.setVisibility(0);
            }
            int[] iArr = new int[2];
            iArr[0] = this.mHideControls ? 0 : this.mControlsAnimateWidth;
            iArr[1] = this.mHideControls ? this.mControlsAnimateWidth : 0;
            ObjectAnimator objectAnimatorOfInt = ObjectAnimator.ofInt(this, "controlsOffset", iArr);
            objectAnimatorOfInt.setDuration(this.mCalendarControlsAnimationTime);
            ObjectAnimator.setFrameDelay(0L);
            objectAnimatorOfInt.start();
            return true;
        }
        if (itemId == R.id.action_search) {
            return false;
        }
        if (itemId == R.id.action_go_to) {
            launchDatePicker();
            return true;
        }
        if (this.mOptionsMenuExt.onOptionsItemSelected(menuItem.getItemId())) {
            return true;
        }
        return this.mExtensions.handleItemSelected(menuItem, this);
    }

    public void setControlsOffset(int i) {
        if (this.mOrientation == 2) {
            float f = i;
            this.mMiniMonth.setTranslationX(f);
            this.mCalendarsList.setTranslationX(f);
            ((ViewGroup.LayoutParams) this.mControlsParams).width = Math.max(0, this.mControlsAnimateWidth - i);
            this.mMiniMonthContainer.setLayoutParams(this.mControlsParams);
            return;
        }
        float f2 = i;
        this.mMiniMonth.setTranslationY(f2);
        this.mCalendarsList.setTranslationY(f2);
        if (this.mVerticalControlsParams == null) {
            this.mVerticalControlsParams = new LinearLayout.LayoutParams(-1, this.mControlsAnimateHeight);
        }
        ((ViewGroup.LayoutParams) this.mVerticalControlsParams).height = Math.max(0, this.mControlsAnimateHeight - i);
        this.mMiniMonthContainer.setLayoutParams(this.mVerticalControlsParams);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String str) {
        if (this.mOnCreateRequestPermissionFlag == 1 && checkPermissions(1) == null && str.equals("preferences_week_start_day")) {
            if (this.mPaused) {
                this.mUpdateOnResume = true;
            } else {
                initFragments(this.mController.getTime(), this.mController.getViewType(), null);
            }
        }
    }

    private void setMainPane(FragmentTransaction fragmentTransaction, int i, int i2, long j, boolean z) {
        Fragment fragment;
        FragmentTransaction fragmentTransactionBeginTransaction;
        PDebug.Start("AllInOneActivity.setMainPane");
        if (this.mOnSaveInstanceStateCalled) {
            PDebug.End("AllInOneActivity.setMainPane");
            return;
        }
        if (!z && this.mCurrentView == i2) {
            PDebug.End("AllInOneActivity.setMainPane");
            return;
        }
        boolean z2 = true;
        boolean z3 = (i2 == 4 || this.mCurrentView == 4) ? false : true;
        FragmentManager fragmentManager = getFragmentManager();
        if (this.mCurrentView == 1) {
            ?? FindFragmentById = fragmentManager.findFragmentById(i);
            if (FindFragmentById instanceof AgendaFragment) {
                FindFragmentById.removeFragments(fragmentManager);
            }
        }
        if (i2 != this.mCurrentView) {
            if (this.mCurrentView != 5 && this.mCurrentView > 0) {
                this.mPreviousView = this.mCurrentView;
            }
            this.mCurrentView = i2;
        }
        AgendaFragment agendaFragment = null;
        if (i2 != 4) {
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
                    DayFragment dayFragment = new DayFragment(this, j, 1);
                    ExtensionsFactory.getAnalyticsLogger(getBaseContext()).trackView("day");
                    fragment = dayFragment;
                    break;
                default:
                    if (this.mActionBar != null && this.mActionBar.getSelectedTab() != this.mWeekTab) {
                        this.mActionBar.selectTab(this.mWeekTab);
                    }
                    if (this.mActionBarMenuSpinnerAdapter != null) {
                        this.mActionBar.setSelectedNavigationItem(1);
                    }
                    PDebug.Start("AllInOneActivity.setMainPane.newDayFragment");
                    DayFragment dayFragment2 = new DayFragment(this, j, 7);
                    PDebug.End("AllInOneActivity.setMainPane.newDayFragment");
                    ExtensionsFactory.getAnalyticsLogger(getBaseContext()).trackView("week");
                    fragment = dayFragment2;
                    break;
            }
        } else {
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
        if (fragmentTransaction == null) {
            fragmentTransactionBeginTransaction = fragmentManager.beginTransaction();
        } else {
            z2 = false;
            fragmentTransactionBeginTransaction = fragmentTransaction;
        }
        if (z3) {
            fragmentTransactionBeginTransaction.setTransition(4099);
        }
        fragmentTransactionBeginTransaction.replace(i, fragment);
        if (mShowAgendaWithMonth) {
            if (agendaFragment != null) {
                fragmentTransactionBeginTransaction.replace(R.id.secondary_pane, agendaFragment);
                this.mSecondaryPane.setVisibility(0);
            } else {
                this.mSecondaryPane.setVisibility(8);
                Fragment fragmentFindFragmentById = fragmentManager.findFragmentById(R.id.secondary_pane);
                if (fragmentFindFragmentById != null) {
                    fragmentTransactionBeginTransaction.remove(fragmentFindFragmentById);
                }
                this.mController.deregisterEventHandler(Integer.valueOf(R.id.secondary_pane));
            }
        }
        this.mController.registerEventHandler(i, (CalendarController.EventHandler) fragment);
        if (agendaFragment != null) {
            this.mController.registerEventHandler(i, agendaFragment);
        }
        if (z2) {
            fragmentTransactionBeginTransaction.commit();
        }
        PDebug.End("AllInOneActivity.setMainPane");
    }

    private void setTitleInActionBar(CalendarController.EventInfo eventInfo) {
        long millis;
        if (eventInfo.eventType != 1024 || this.mActionBar == null) {
            return;
        }
        long millis2 = eventInfo.startTime.toMillis(false);
        if (eventInfo.endTime != null) {
            millis = eventInfo.endTime.toMillis(false);
        } else {
            millis = millis2;
        }
        String dateRange = Utils.formatDateRange(this, millis2, millis2 > millis ? Utils.getLastDisplayTimeInCalendar(this).toMillis(false) : millis, (int) eventInfo.extraLong);
        CharSequence text = this.mDateRange.getText();
        this.mDateRange.setText(dateRange);
        if (eventInfo.selectedTime != null) {
            millis2 = eventInfo.selectedTime.toMillis(true);
        }
        updateSecondaryTitleFields(millis2);
        if (!TextUtils.equals(text, dateRange)) {
            this.mDateRange.sendAccessibilityEvent(8);
            if (this.mShowWeekNum && this.mWeekTextView != null) {
                this.mWeekTextView.sendAccessibilityEvent(8);
            }
        }
    }

    private void updateSecondaryTitleFields(long j) {
        this.mShowWeekNum = Utils.getShowWeekNumber(this);
        this.mTimeZone = Utils.getTimeZone(this, this.mHomeTimeUpdater);
        if (j != -1) {
            this.mWeekNum = Utils.getWeekNumberFromTime(j, this);
        }
        if (this.mShowWeekNum && this.mCurrentView == 3 && mIsTabletConfig && this.mWeekTextView != null) {
            this.mWeekTextView.setText(getResources().getQuantityString(R.plurals.weekN, this.mWeekNum, Integer.valueOf(this.mWeekNum)));
            this.mWeekTextView.setVisibility(0);
        } else if (j != -1 && this.mWeekTextView != null && this.mCurrentView == 2 && mIsTabletConfig) {
            Time time = new Time(this.mTimeZone);
            time.set(j);
            int julianDay = Time.getJulianDay(j, time.gmtoff);
            time.setToNow();
            this.mWeekTextView.setText(Utils.getDayOfWeekString(julianDay, Time.getJulianDay(time.toMillis(false), time.gmtoff), j, this));
            this.mWeekTextView.setVisibility(0);
        } else if (this.mWeekTextView != null && (!mIsTabletConfig || this.mCurrentView != 2)) {
            this.mWeekTextView.setVisibility(8);
        }
        if (this.mHomeTime == null || (!(this.mCurrentView == 2 || this.mCurrentView == 3 || this.mCurrentView == 1) || TextUtils.equals(this.mTimeZone, Time.getCurrentTimezone()))) {
            if (this.mHomeTime != null) {
                this.mHomeTime.setVisibility(8);
                return;
            }
            return;
        }
        Time time2 = new Time(this.mTimeZone);
        time2.setToNow();
        long millis = time2.toMillis(true);
        this.mHomeTime.setText(Utils.formatDateRange(this, millis, millis, DateFormat.is24HourFormat(this) ? 129 : 1) + " " + TimeZone.getTimeZone(this.mTimeZone).getDisplayName(time2.isDst != 0, 1, Locale.getDefault()));
        this.mHomeTime.setVisibility(0);
        this.mHomeTime.removeCallbacks(this.mHomeTimeUpdater);
        this.mHomeTime.postDelayed(this.mHomeTimeUpdater, 60000 - (millis % 60000));
    }

    @Override
    public long getSupportedEventTypes() {
        if (this.mOnCreateRequestPermissionFlag == 1 && checkPermissions(1) == null) {
            return 1058L;
        }
        return 0L;
    }

    @Override
    public void handleEvent(CalendarController.EventInfo eventInfo) {
        long millis;
        int i;
        long millis2;
        long millis3;
        if (this.mOnCreateRequestPermissionFlag != 1 || checkPermissions(1) != null) {
            return;
        }
        long millis4 = -1;
        if (eventInfo.eventType == 32) {
            if ((eventInfo.extraLong & 4) != 0) {
                this.mBackToPreviousView = true;
            } else if (eventInfo.viewType != this.mController.getPreviousViewType() && eventInfo.viewType != 5) {
                this.mBackToPreviousView = false;
            }
            setMainPane(null, R.id.main_pane, eventInfo.viewType, eventInfo.startTime.toMillis(false), false);
            if (this.mSearchView != null) {
                this.mSearchView.clearFocus();
            }
            if (this.mShowCalendarControls) {
                int i2 = this.mOrientation == 2 ? this.mControlsAnimateWidth : this.mControlsAnimateHeight;
                boolean z = eventInfo.viewType == 4 || eventInfo.viewType == 1;
                if (this.mControlsMenu != null) {
                    this.mControlsMenu.setVisible(!z);
                    this.mControlsMenu.setEnabled(!z);
                }
                if (z || this.mHideControls) {
                    this.mShowSideViews = false;
                    if (!this.mHideControls) {
                        ObjectAnimator objectAnimatorOfInt = ObjectAnimator.ofInt(this, "controlsOffset", 0, i2);
                        objectAnimatorOfInt.addListener(this.mSlideAnimationDoneListener);
                        objectAnimatorOfInt.setDuration(this.mCalendarControlsAnimationTime);
                        ObjectAnimator.setFrameDelay(0L);
                        objectAnimatorOfInt.start();
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
                        ObjectAnimator objectAnimatorOfInt2 = ObjectAnimator.ofInt(this, "controlsOffset", i2, 0);
                        objectAnimatorOfInt2.setDuration(this.mCalendarControlsAnimationTime);
                        ObjectAnimator.setFrameDelay(0L);
                        objectAnimatorOfInt2.start();
                    }
                }
            }
            if (eventInfo.selectedTime != null) {
                millis = eventInfo.selectedTime.toMillis(true);
            } else {
                millis = eventInfo.startTime.toMillis(true);
            }
            if (!mIsTabletConfig) {
                this.mActionBarMenuSpinnerAdapter.setTime(millis);
            }
        } else {
            if (eventInfo.eventType == 2) {
                if (this.mCurrentView == 1 && mShowEventDetailsWithAgenda) {
                    if (eventInfo.startTime != null && eventInfo.endTime != null) {
                        if (eventInfo.isAllDay()) {
                            Utils.convertAlldayUtcToLocal(eventInfo.startTime, eventInfo.startTime.toMillis(false), this.mTimeZone);
                            Utils.convertAlldayUtcToLocal(eventInfo.endTime, eventInfo.endTime.toMillis(false), this.mTimeZone);
                        }
                        this.mController.sendEvent(this, 32L, eventInfo.startTime, eventInfo.endTime, eventInfo.selectedTime, eventInfo.id, 1, 2L, null, null);
                    } else if (eventInfo.selectedTime != null) {
                        this.mController.sendEvent(this, 32L, eventInfo.selectedTime, eventInfo.selectedTime, eventInfo.id, 1);
                    }
                } else {
                    if (eventInfo.selectedTime != null) {
                        i = 1;
                        if (this.mCurrentView != 1) {
                            this.mController.sendEvent(this, 32L, eventInfo.selectedTime, eventInfo.selectedTime, -1L, 0);
                        }
                    } else {
                        i = 1;
                    }
                    int response = eventInfo.getResponse();
                    if ((this.mCurrentView == i && this.mShowEventInfoFullScreenAgenda) || ((this.mCurrentView == 2 || this.mCurrentView == 3 || this.mCurrentView == 4) && this.mShowEventInfoFullScreen)) {
                        Intent intent = new Intent("android.intent.action.VIEW");
                        intent.setData(ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventInfo.id));
                        intent.setClass(this, EventInfoActivity.class);
                        intent.setFlags(537001984);
                        intent.putExtra("beginTime", eventInfo.startTime.toMillis(false));
                        intent.putExtra("endTime", eventInfo.endTime.toMillis(false));
                        intent.putExtra("attendeeStatus", response);
                        startActivity(intent);
                    } else if (eventInfo.startTime != null && eventInfo.endTime != null) {
                        EventInfoFragment eventInfoFragment = new EventInfoFragment((Context) this, eventInfo.id, eventInfo.startTime.toMillis(false), eventInfo.endTime.toMillis(false), response, true, 1, (ArrayList<CalendarEventModel.ReminderEntry>) null);
                        eventInfoFragment.setDialogParams(eventInfo.x, eventInfo.y, this.mActionBar.getHeight());
                        FragmentManager fragmentManager = getFragmentManager();
                        FragmentTransaction fragmentTransactionBeginTransaction = fragmentManager.beginTransaction();
                        Fragment fragmentFindFragmentByTag = fragmentManager.findFragmentByTag("EventInfoFragment");
                        if (fragmentFindFragmentByTag != null && fragmentFindFragmentByTag.isAdded()) {
                            fragmentTransactionBeginTransaction.remove(fragmentFindFragmentByTag);
                        }
                        fragmentTransactionBeginTransaction.add(eventInfoFragment, "EventInfoFragment");
                        fragmentTransactionBeginTransaction.commit();
                    }
                    long j = eventInfo.id;
                    if (eventInfo.startTime != null) {
                        millis2 = eventInfo.startTime.toMillis(false);
                    } else {
                        millis2 = -1;
                    }
                    if (eventInfo.endTime != null) {
                        millis3 = eventInfo.endTime.toMillis(false);
                    } else {
                        millis3 = -1;
                    }
                    AlertUtils.removeEventNotification(this, j, millis2, millis3);
                }
                millis4 = eventInfo.startTime.toMillis(true);
            } else if (eventInfo.eventType == 1024) {
                setTitleInActionBar(eventInfo);
                this.mActionBarMenuSpinnerAdapter.setTime(this.mController.getTime());
            }
            millis = millis4;
        }
        updateSecondaryTitleFields(millis);
    }

    public void handleSelectSyncedCalendarsClicked(View view) {
        if (this.mOnCreateRequestPermissionFlag == 1 && checkPermissions(1) == null) {
            this.mController.sendEvent(this, 64L, null, null, null, 0L, 0, 2L, null, null);
        }
    }

    public void eventsChanged() {
        if (this.mOnCreateRequestPermissionFlag == 1 && checkPermissions(1) == null) {
            this.mController.sendEvent(this, 128L, null, null, -1L, 0);
        }
    }

    @Override
    public boolean onQueryTextChange(String str) {
        return false;
    }

    @Override
    public boolean onQueryTextSubmit(String str) {
        if (this.mOnCreateRequestPermissionFlag == 1 && checkPermissions(1) == null) {
            exitSearchMode();
            this.mController.sendEvent(this, 256L, null, null, -1L, 0, 0L, str, getComponentName());
            return true;
        }
        return false;
    }

    @Override
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
        if (this.mOnCreateRequestPermissionFlag == 1 && checkPermissions(1) == null) {
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
                return;
            }
            if (tab == this.mAgendaTab && this.mCurrentView != 1) {
                this.mController.sendEvent(this, 32L, null, null, -1L, 1);
                return;
            }
            StringBuilder sb = new StringBuilder();
            sb.append("TabSelected event from unknown tab: ");
            sb.append((Object) (tab == null ? "null" : tab.getText()));
            Log.w("AllInOneActivity", sb.toString());
            Log.w("AllInOneActivity", "CurrentView:" + this.mCurrentView + " Tab:" + tab.toString() + " Day:" + this.mDayTab + " Week:" + this.mWeekTab + " Month:" + this.mMonthTab + " Agenda:" + this.mAgendaTab);
            return;
        }
        Log.w("AllInOneActivity", "TabSelected AllInOne= controller null");
    }

    @Override
    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
    }

    @Override
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
    }

    @Override
    public boolean onNavigationItemSelected(int i, long j) {
        if (this.mOnCreateRequestPermissionFlag == 1 && checkPermissions(1) == null) {
            switch (i) {
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
                    Log.w("AllInOneActivity", "ItemSelected event from unknown button: " + i);
                    Log.w("AllInOneActivity", "CurrentView:" + this.mCurrentView + " Button:" + i + " Day:" + this.mDayTab + " Week:" + this.mWeekTab + " Month:" + this.mMonthTab + " Agenda:" + this.mAgendaTab);
                    break;
            }
            return false;
        }
        return false;
    }

    @Override
    public boolean onSuggestionSelect(int i) {
        return false;
    }

    @Override
    public boolean onSuggestionClick(int i) {
        exitSearchMode();
        return false;
    }

    @Override
    public boolean onSearchRequested() {
        enterSearchMode();
        return false;
    }

    public static void setClearEventsCompletedStatus(boolean z) {
        mIsClearEventsCompleted = z;
    }

    private void resetGotoDateSetListener() {
        this.mGotoDatePickerDialog = (DatePickerDialog) getFragmentManager().findFragmentByTag("goto_frag");
        if (this.mGotoDatePickerDialog != null) {
            LogUtil.v("AllInOneActivity", "resetGotoDateSetListener. ");
            this.mGotoDatePickerDialog.setOnDateSetListener(this.mGotoDateSetlistener);
        }
    }

    public void launchDatePicker() {
        Time time = new Time(this.mTimeZone);
        time.setToNow();
        int firstDayOfWeek = Utils.getFirstDayOfWeek(this);
        if (firstDayOfWeek != 6 && firstDayOfWeek == 0) {
        }
        this.mGotoDatePickerDialog = DatePickerDialog.newInstance(this.mGotoDateSetlistener, time.year, time.month, time.monthDay);
        this.mGotoDatePickerDialog.setFirstDayOfWeek(Utils.getFirstDayOfWeekAsCalendar(this));
        this.mGotoDatePickerDialog.setYearRange(1970, 2036);
        this.mGotoDatePickerDialog.show(getFragmentManager(), "goto_frag");
    }

    private void exitSearchMode() {
        this.mIsInSearchMode = false;
        if (this.mSearchMenu != null && this.mSearchMenu.isActionViewExpanded()) {
            this.mSearchMenu.collapseActionView();
        }
    }

    private void enterSearchMode() {
        this.mIsInSearchMode = true;
        if (this.mSearchMenu != null && !this.mSearchMenu.isActionViewExpanded()) {
            this.mSearchMenu.expandActionView();
        }
    }
}
