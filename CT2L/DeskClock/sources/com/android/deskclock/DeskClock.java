package com.android.deskclock;

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Outline;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.ImageButton;
import android.widget.TextView;
import com.android.deskclock.LabelDialogFragment;
import com.android.deskclock.alarms.AlarmStateManager;
import com.android.deskclock.provider.Alarm;
import com.android.deskclock.stopwatch.StopwatchFragment;
import com.android.deskclock.stopwatch.StopwatchService;
import com.android.deskclock.timer.TimerFragment;
import com.android.deskclock.timer.TimerObj;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;
import java.util.TimeZone;

public class DeskClock extends Activity implements LabelDialogFragment.AlarmLabelDialogHandler, LabelDialogFragment.TimerLabelDialogHandler {
    private static final ViewOutlineProvider OVAL_OUTLINE_PROVIDER = new ViewOutlineProvider() {
        @Override
        public void getOutline(View view, Outline outline) {
            outline.setOval(0, 0, view.getWidth(), view.getHeight());
        }
    };
    private ActionBar mActionBar;
    private ActionBar.Tab mAlarmTab;
    private ActionBar.Tab mClockTab;
    private ImageButton mFab;
    private Handler mHander;
    private ImageButton mLeftButton;
    private Menu mMenu;
    private ImageButton mRightButton;
    private int mSelectedTab;
    private ActionBar.Tab mStopwatchTab;
    private TabsAdapter mTabsAdapter;
    private ActionBar.Tab mTimerTab;
    private ViewPager mViewPager;
    private boolean mIsFirstLaunch = true;
    private int mLastHourColor = 0;
    private final Runnable mBackgroundColorChanger = new Runnable() {
        @Override
        public void run() {
            DeskClock.this.setBackgroundColor();
            DeskClock.this.mHander.postDelayed(this, 60000L);
        }
    };

    @Override
    protected void onStart() {
        super.onStart();
        if (this.mHander == null) {
            this.mHander = new Handler();
        }
        this.mHander.postDelayed(this.mBackgroundColorChanger, 60000L);
    }

    @Override
    protected void onStop() {
        super.onStop();
        this.mHander.removeCallbacks(this.mBackgroundColorChanger);
    }

    @Override
    public void onNewIntent(Intent newIntent) {
        super.onNewIntent(newIntent);
        setIntent(newIntent);
        int tab = newIntent.getIntExtra("deskclock.select.tab", -1);
        if (tab != -1 && this.mActionBar != null) {
            this.mActionBar.setSelectedNavigationItem(tab);
        }
    }

    private void initViews() {
        setContentView(R.layout.desk_clock);
        this.mFab = (ImageButton) findViewById(R.id.fab);
        this.mFab.setOutlineProvider(OVAL_OUTLINE_PROVIDER);
        this.mLeftButton = (ImageButton) findViewById(R.id.left_button);
        this.mRightButton = (ImageButton) findViewById(R.id.right_button);
        if (this.mTabsAdapter == null) {
            this.mViewPager = (ViewPager) findViewById(R.id.desk_clock_pager);
            this.mViewPager.setOffscreenPageLimit(3);
            this.mTabsAdapter = new TabsAdapter(this, this.mViewPager);
            createTabs(this.mSelectedTab);
        }
        this.mFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DeskClock.this.getSelectedFragment().onFabClick(view);
            }
        });
        this.mLeftButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DeskClock.this.getSelectedFragment().onLeftButtonClick(view);
            }
        });
        this.mRightButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DeskClock.this.getSelectedFragment().onRightButtonClick(view);
            }
        });
        this.mActionBar.setSelectedNavigationItem(this.mSelectedTab);
    }

    private DeskClockFragment getSelectedFragment() {
        return (DeskClockFragment) this.mTabsAdapter.getItem(getRtlPosition(this.mSelectedTab));
    }

    private void createTabs(int selectedIndex) {
        this.mActionBar = getActionBar();
        if (this.mActionBar != null) {
            this.mActionBar.setDisplayOptions(0);
            this.mActionBar.setNavigationMode(2);
            this.mAlarmTab = this.mActionBar.newTab();
            this.mAlarmTab.setIcon(R.drawable.ic_alarm_animation);
            this.mAlarmTab.setContentDescription(R.string.menu_alarm);
            this.mTabsAdapter.addTab(this.mAlarmTab, AlarmClockFragment.class, 0);
            this.mClockTab = this.mActionBar.newTab();
            this.mClockTab.setIcon(R.drawable.ic_clock_animation);
            this.mClockTab.setContentDescription(R.string.menu_clock);
            this.mTabsAdapter.addTab(this.mClockTab, ClockFragment.class, 1);
            this.mTimerTab = this.mActionBar.newTab();
            this.mTimerTab.setIcon(R.drawable.ic_timer_animation);
            this.mTimerTab.setContentDescription(R.string.menu_timer);
            this.mTabsAdapter.addTab(this.mTimerTab, TimerFragment.class, 2);
            this.mStopwatchTab = this.mActionBar.newTab();
            this.mStopwatchTab.setIcon(R.drawable.ic_stopwatch_animation);
            this.mStopwatchTab.setContentDescription(R.string.menu_stopwatch);
            this.mTabsAdapter.addTab(this.mStopwatchTab, StopwatchFragment.class, 3);
            this.mActionBar.setSelectedNavigationItem(selectedIndex);
            this.mTabsAdapter.notifySelectedPage(selectedIndex);
        }
    }

    @Override
    protected void onCreate(Bundle icicle) {
        int tab;
        super.onCreate(icicle);
        setVolumeControlStream(4);
        this.mIsFirstLaunch = icicle == null;
        getWindow().setBackgroundDrawable(null);
        this.mIsFirstLaunch = true;
        this.mSelectedTab = 1;
        if (icicle != null) {
            this.mSelectedTab = icicle.getInt("selected_tab", 1);
            this.mLastHourColor = icicle.getInt("last_hour_color", 0);
            if (this.mLastHourColor != 0) {
                getWindow().getDecorView().setBackgroundColor(this.mLastHourColor);
            }
        }
        Intent i = getIntent();
        if (i != null && (tab = i.getIntExtra("deskclock.select.tab", -1)) != -1) {
            this.mSelectedTab = tab;
        }
        initViews();
        setHomeTimeZone();
        AlarmStateManager.updateNextAlarm(this);
        ExtensionsFactory.init(getAssets());
    }

    @Override
    protected void onResume() {
        super.onResume();
        setBackgroundColor();
        Intent stopwatchIntent = new Intent(getApplicationContext(), (Class<?>) StopwatchService.class);
        stopwatchIntent.setAction("kill_notification");
        startService(stopwatchIntent);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("notif_app_open", true);
        editor.apply();
        Intent timerIntent = new Intent();
        timerIntent.setAction("notif_in_use_cancel");
        sendBroadcast(timerIntent);
    }

    @Override
    public void onPause() {
        Intent intent = new Intent(getApplicationContext(), (Class<?>) StopwatchService.class);
        intent.setAction("show_notification");
        startService(intent);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("notif_app_open", false);
        editor.apply();
        Utils.showInUseNotifications(this);
        super.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("selected_tab", this.mActionBar.getSelectedNavigationIndex());
        outState.putInt("last_hour_color", this.mLastHourColor);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.mMenu = menu;
        if (getResources().getConfiguration().orientation != 2) {
            return false;
        }
        if (this.mActionBar.getSelectedNavigationIndex() != 0 && this.mActionBar.getSelectedNavigationIndex() != 1) {
            return true;
        }
        menu.clear();
        getMenuInflater().inflate(R.menu.desk_clock_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        updateMenu(menu);
        return true;
    }

    private void updateMenu(Menu menu) {
        MenuItem help = menu.findItem(R.id.menu_item_help);
        if (help != null) {
            Utils.prepareHelpMenuItem(this, help);
        }
        MenuItem nightMode = menu.findItem(R.id.menu_item_night_mode);
        if (this.mActionBar.getSelectedNavigationIndex() == 0) {
            nightMode.setVisible(false);
        } else if (this.mActionBar.getSelectedNavigationIndex() == 1) {
            nightMode.setVisible(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (processMenuClick(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private boolean processMenuClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_item_settings:
                startActivity(new Intent(this, (Class<?>) SettingsActivity.class));
                return true;
            case R.id.menu_item_help:
                Intent i = item.getIntent();
                if (i != null) {
                    try {
                        startActivity(i);
                        break;
                    } catch (ActivityNotFoundException e) {
                    }
                }
                return true;
            case R.id.menu_items:
            default:
                return true;
            case R.id.menu_item_night_mode:
                startActivity(new Intent(this, (Class<?>) ScreensaverActivity.class));
                return true;
        }
    }

    private void setHomeTimeZone() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (prefs.getString("home_time_zone", "").isEmpty()) {
            String homeTimeZone = TimeZone.getDefault().getID();
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("home_time_zone", homeTimeZone);
            editor.apply();
            android.util.Log.v("DeskClock", "Setting home time zone to " + homeTimeZone);
        }
    }

    public void registerPageChangedListener(DeskClockFragment frag) {
        if (this.mTabsAdapter != null) {
            this.mTabsAdapter.registerPageChangedListener(frag);
        }
    }

    public void unregisterPageChangedListener(DeskClockFragment frag) {
        if (this.mTabsAdapter != null) {
            this.mTabsAdapter.unregisterPageChangedListener(frag);
        }
    }

    private void setBackgroundColor() {
        int duration;
        if (this.mLastHourColor == 0) {
            this.mLastHourColor = getResources().getColor(R.color.default_background);
            duration = 3000;
        } else {
            duration = getResources().getInteger(android.R.integer.config_longAnimTime);
        }
        int currHourColor = Utils.getCurrentHourColor();
        if (this.mLastHourColor != currHourColor) {
            ObjectAnimator animator = ObjectAnimator.ofInt(getWindow().getDecorView(), "backgroundColor", this.mLastHourColor, currHourColor);
            animator.setDuration(duration);
            animator.setEvaluator(new ArgbEvaluator());
            animator.start();
            this.mLastHourColor = currHourColor;
        }
    }

    private class TabsAdapter extends FragmentPagerAdapter implements ActionBar.TabListener, ViewPager.OnPageChangeListener {
        Context mContext;
        HashSet<String> mFragmentTags;
        ActionBar mMainActionBar;
        ViewPager mPager;
        private final ArrayList<TabInfo> mTabs;

        final class TabInfo {
            private final Bundle args = new Bundle();
            private final Class<?> clss;

            TabInfo(Class<?> _class, int position) {
                this.clss = _class;
                this.args.putInt("tab_position", position);
            }

            public int getPosition() {
                return this.args.getInt("tab_position", 0);
            }
        }

        public TabsAdapter(Activity activity, ViewPager pager) {
            super(activity.getFragmentManager());
            this.mTabs = new ArrayList<>();
            this.mFragmentTags = new HashSet<>();
            this.mContext = activity;
            this.mMainActionBar = activity.getActionBar();
            this.mPager = pager;
            this.mPager.setAdapter(this);
            this.mPager.setOnPageChangeListener(this);
        }

        @Override
        public Fragment getItem(int position) {
            String name = makeFragmentName(R.id.desk_clock_pager, position);
            Fragment fragment = DeskClock.this.getFragmentManager().findFragmentByTag(name);
            if (fragment == null) {
                TabInfo info = this.mTabs.get(DeskClock.this.getRtlPosition(position));
                fragment = Fragment.instantiate(this.mContext, info.clss.getName(), info.args);
                if (fragment instanceof TimerFragment) {
                    ((TimerFragment) fragment).setFabAppearance();
                    ((TimerFragment) fragment).setLeftRightButtonAppearance();
                }
            }
            return fragment;
        }

        private String makeFragmentName(int viewId, int index) {
            return "android:switcher:" + viewId + ":" + index;
        }

        @Override
        public int getCount() {
            return this.mTabs.size();
        }

        public void addTab(ActionBar.Tab tab, Class<?> clss, int position) {
            TabInfo info = new TabInfo(clss, position);
            tab.setTag(info);
            tab.setTabListener(this);
            this.mTabs.add(info);
            this.mMainActionBar.addTab(tab);
            notifyDataSetChanged();
        }

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        }

        @Override
        public void onPageSelected(int position) {
            this.mMainActionBar.setSelectedNavigationItem(DeskClock.this.getRtlPosition(position));
            notifyPageChanged(position);
            if (DeskClock.this.mMenu != null) {
                if (position == 0 || position == 1) {
                    DeskClock.this.mMenu.setGroupVisible(R.id.menu_items, true);
                    DeskClock.this.onCreateOptionsMenu(DeskClock.this.mMenu);
                } else {
                    DeskClock.this.mMenu.setGroupVisible(R.id.menu_items, false);
                }
            }
        }

        @Override
        public void onPageScrollStateChanged(int state) {
        }

        @Override
        public void onTabReselected(ActionBar.Tab tab, FragmentTransaction arg1) {
        }

        @Override
        public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft) {
            TabInfo info = (TabInfo) tab.getTag();
            int position = info.getPosition();
            int rtlSafePosition = DeskClock.this.getRtlPosition(position);
            DeskClock.this.mSelectedTab = position;
            if (DeskClock.this.mIsFirstLaunch && isClockTab(rtlSafePosition)) {
                DeskClock.this.mLeftButton.setVisibility(4);
                DeskClock.this.mRightButton.setVisibility(4);
                DeskClock.this.mFab.setVisibility(0);
                DeskClock.this.mFab.setImageResource(R.drawable.ic_globe);
                DeskClock.this.mFab.setContentDescription(DeskClock.this.getString(R.string.button_cities));
                DeskClock.this.mIsFirstLaunch = false;
            } else {
                DeskClockFragment f = (DeskClockFragment) getItem(rtlSafePosition);
                f.setFabAppearance();
                f.setLeftRightButtonAppearance();
            }
            this.mPager.setCurrentItem(rtlSafePosition);
        }

        @Override
        public void onTabUnselected(ActionBar.Tab arg0, FragmentTransaction arg1) {
        }

        private boolean isClockTab(int rtlSafePosition) {
            int clockTabIndex = DeskClock.this.isRtl() ? 2 : 1;
            return rtlSafePosition == clockTabIndex;
        }

        public void notifySelectedPage(int page) {
            notifyPageChanged(page);
        }

        private void notifyPageChanged(int newPage) {
            for (String tag : this.mFragmentTags) {
                FragmentManager fm = DeskClock.this.getFragmentManager();
                DeskClockFragment f = (DeskClockFragment) fm.findFragmentByTag(tag);
                if (f != null) {
                    f.onPageChanged(newPage);
                }
            }
        }

        public void registerPageChangedListener(DeskClockFragment frag) {
            String tag = frag.getTag();
            if (this.mFragmentTags.contains(tag)) {
                android.util.Log.wtf("DeskClock", "Trying to add an existing fragment " + tag);
            } else {
                this.mFragmentTags.add(frag.getTag());
            }
            frag.onPageChanged(this.mMainActionBar.getSelectedNavigationIndex());
        }

        public void unregisterPageChangedListener(DeskClockFragment frag) {
            this.mFragmentTags.remove(frag.getTag());
        }
    }

    public static abstract class OnTapListener implements View.OnTouchListener {
        private final float MAX_MOVEMENT_ALLOWED = 20.0f;
        private final long MAX_TIME_ALLOWED = 500;
        private final int mGrayColor;
        private long mLastTouchTime;
        private float mLastTouchX;
        private float mLastTouchY;
        private final TextView mMakePressedTextView;
        private final int mPressedColor;

        protected abstract void processClick(View view);

        public OnTapListener(Activity activity, TextView makePressedView) {
            this.mMakePressedTextView = makePressedView;
            this.mPressedColor = activity.getResources().getColor(Utils.getPressedColorId());
            this.mGrayColor = activity.getResources().getColor(Utils.getGrayColorId());
        }

        @Override
        public boolean onTouch(View v, MotionEvent e) {
            switch (e.getAction()) {
                case 0:
                    this.mLastTouchTime = Utils.getTimeNow();
                    this.mLastTouchX = e.getX();
                    this.mLastTouchY = e.getY();
                    if (this.mMakePressedTextView != null) {
                        this.mMakePressedTextView.setTextColor(this.mPressedColor);
                    }
                    return false;
                case 1:
                    float xDiff = Math.abs(e.getX() - this.mLastTouchX);
                    float yDiff = Math.abs(e.getY() - this.mLastTouchY);
                    long timeDiff = Utils.getTimeNow() - this.mLastTouchTime;
                    if (xDiff < 20.0f && yDiff < 20.0f && timeDiff < 500) {
                        if (this.mMakePressedTextView != null) {
                            v = this.mMakePressedTextView;
                        }
                        processClick(v);
                        resetValues();
                        return true;
                    }
                    resetValues();
                    return false;
                case 2:
                    float xDiff2 = Math.abs(e.getX() - this.mLastTouchX);
                    float yDiff2 = Math.abs(e.getY() - this.mLastTouchY);
                    if (xDiff2 >= 20.0f || yDiff2 >= 20.0f) {
                        resetValues();
                    }
                    return false;
                default:
                    resetValues();
                    return false;
            }
        }

        private void resetValues() {
            this.mLastTouchX = -19.0f;
            this.mLastTouchY = -19.0f;
            this.mLastTouchTime = -499L;
            if (this.mMakePressedTextView != null) {
                this.mMakePressedTextView.setTextColor(this.mGrayColor);
            }
        }
    }

    public void onDialogLabelSet(TimerObj timer, String label, String tag) {
        Fragment frag = getFragmentManager().findFragmentByTag(tag);
        if (frag instanceof TimerFragment) {
            ((TimerFragment) frag).setLabel(timer, label);
        }
    }

    public void onDialogLabelSet(Alarm alarm, String label, String tag) {
        Fragment frag = getFragmentManager().findFragmentByTag(tag);
        if (frag instanceof AlarmClockFragment) {
            ((AlarmClockFragment) frag).setLabel(alarm, label);
        }
    }

    public int getSelectedTab() {
        return this.mSelectedTab;
    }

    private boolean isRtl() {
        return TextUtils.getLayoutDirectionFromLocale(Locale.getDefault()) == 1;
    }

    private int getRtlPosition(int position) {
        if (isRtl()) {
            switch (position) {
            }
            return position;
        }
        return position;
    }

    public ImageButton getFab() {
        return this.mFab;
    }

    public ImageButton getLeftButton() {
        return this.mLeftButton;
    }

    public ImageButton getRightButton() {
        return this.mRightButton;
    }
}
