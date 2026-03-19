package com.android.deskclock;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.app.Fragment;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.StringRes;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.deskclock.FabContainer;
import com.android.deskclock.LabelDialogFragment;
import com.android.deskclock.actionbarmenu.MenuItemControllerFactory;
import com.android.deskclock.actionbarmenu.NightModeMenuItemController;
import com.android.deskclock.actionbarmenu.OptionsMenuManager;
import com.android.deskclock.actionbarmenu.SettingsMenuItemController;
import com.android.deskclock.data.DataModel;
import com.android.deskclock.data.OnSilentSettingsListener;
import com.android.deskclock.events.Events;
import com.android.deskclock.provider.Alarm;
import com.android.deskclock.uidata.TabListener;
import com.android.deskclock.uidata.UiDataModel;
import com.android.deskclock.widget.toast.SnackbarManager;

public class DeskClock extends BaseActivity implements FabContainer, LabelDialogFragment.AlarmLabelDialogHandler {
    private final AnimatorListenerAdapter mAutoStartShowListener;
    private DropShadowController mDropShadowController;
    private ImageView mFab;
    private ViewPager mFragmentTabPager;
    private FragmentTabPagerAdapter mFragmentTabPagerAdapter;
    private Button mLeftButton;
    private boolean mRecreateActivity;
    private Button mRightButton;
    private Runnable mShowSilentSettingSnackbarRunnable;
    private final OnSilentSettingsListener mSilentSettingChangeWatcher;
    private View mSnackbarAnchor;
    private final TabListener mTabChangeWatcher;
    private TabLayout mTabLayout;
    private final OptionsMenuManager mOptionsMenuManager = new OptionsMenuManager();
    private final AnimatorSet mHideAnimation = new AnimatorSet();
    private final AnimatorSet mShowAnimation = new AnimatorSet();
    private final AnimatorSet mUpdateFabOnlyAnimation = new AnimatorSet();
    private final AnimatorSet mUpdateButtonsOnlyAnimation = new AnimatorSet();
    private FabState mFabState = FabState.SHOWING;

    private enum FabState {
        SHOWING,
        HIDE_ARMED,
        HIDING
    }

    public DeskClock() {
        this.mAutoStartShowListener = new AutoStartShowListener();
        this.mTabChangeWatcher = new TabChangeWatcher();
        this.mSilentSettingChangeWatcher = new SilentSettingChangeWatcher();
    }

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.desk_clock);
        this.mSnackbarAnchor = findViewById(R.id.content);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar supportActionBar = getSupportActionBar();
        int i = 0;
        if (supportActionBar != null) {
            supportActionBar.setDisplayShowTitleEnabled(false);
        }
        this.mOptionsMenuManager.addMenuItemController(new NightModeMenuItemController(this), new SettingsMenuItemController(this));
        this.mOptionsMenuManager.addMenuItemController(MenuItemControllerFactory.getInstance().buildMenuItemControllers(this));
        onCreateOptionsMenu(toolbar.getMenu());
        this.mTabLayout = (TabLayout) findViewById(R.id.tabs);
        int tabCount = UiDataModel.getUiDataModel().getTabCount();
        boolean z = getResources().getBoolean(R.bool.showTabLabel);
        boolean z2 = getResources().getBoolean(R.bool.showTabHorizontally);
        while (true) {
            if (i < tabCount) {
                UiDataModel.Tab tab = UiDataModel.getUiDataModel().getTab(i);
                int labelResId = tab.getLabelResId();
                TabLayout.Tab contentDescription = this.mTabLayout.newTab().setTag(tab).setIcon(tab.getIconResId()).setContentDescription(labelResId);
                if (z) {
                    contentDescription.setText(labelResId);
                    contentDescription.setCustomView(R.layout.tab_item);
                    TextView textView = (TextView) contentDescription.getCustomView().findViewById(android.R.id.text1);
                    textView.setTextColor(this.mTabLayout.getTabTextColors());
                    Drawable icon = contentDescription.getIcon();
                    if (z2) {
                        contentDescription.setIcon((Drawable) null);
                        textView.setCompoundDrawablesRelativeWithIntrinsicBounds(icon, (Drawable) null, (Drawable) null, (Drawable) null);
                    } else {
                        textView.setCompoundDrawablesRelativeWithIntrinsicBounds((Drawable) null, icon, (Drawable) null, (Drawable) null);
                    }
                }
                this.mTabLayout.addTab(contentDescription);
                i++;
            } else {
                this.mFab = (ImageView) findViewById(R.id.fab);
                this.mLeftButton = (Button) findViewById(R.id.left_button);
                this.mRightButton = (Button) findViewById(R.id.right_button);
                this.mFab.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        DeskClock.this.getSelectedDeskClockFragment().onFabClick(DeskClock.this.mFab);
                    }
                });
                this.mLeftButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        DeskClock.this.getSelectedDeskClockFragment().onLeftButtonClick(DeskClock.this.mLeftButton);
                    }
                });
                this.mRightButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        DeskClock.this.getSelectedDeskClockFragment().onRightButtonClick(DeskClock.this.mRightButton);
                    }
                });
                long shortAnimationDuration = UiDataModel.getUiDataModel().getShortAnimationDuration();
                ValueAnimator scaleAnimator = AnimatorUtils.getScaleAnimator(this.mFab, 1.0f, 0.0f);
                ValueAnimator scaleAnimator2 = AnimatorUtils.getScaleAnimator(this.mFab, 0.0f, 1.0f);
                ValueAnimator scaleAnimator3 = AnimatorUtils.getScaleAnimator(this.mLeftButton, 1.0f, 0.0f);
                ValueAnimator scaleAnimator4 = AnimatorUtils.getScaleAnimator(this.mRightButton, 1.0f, 0.0f);
                ValueAnimator scaleAnimator5 = AnimatorUtils.getScaleAnimator(this.mLeftButton, 0.0f, 1.0f);
                ValueAnimator scaleAnimator6 = AnimatorUtils.getScaleAnimator(this.mRightButton, 0.0f, 1.0f);
                scaleAnimator.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animator) {
                        DeskClock.this.getSelectedDeskClockFragment().onUpdateFab(DeskClock.this.mFab);
                    }
                });
                scaleAnimator3.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animator) {
                        DeskClock.this.getSelectedDeskClockFragment().onUpdateFabButtons(DeskClock.this.mLeftButton, DeskClock.this.mRightButton);
                    }
                });
                this.mHideAnimation.setDuration(shortAnimationDuration).play(scaleAnimator).with(scaleAnimator3).with(scaleAnimator4);
                this.mShowAnimation.setDuration(shortAnimationDuration).play(scaleAnimator2).with(scaleAnimator5).with(scaleAnimator6);
                this.mUpdateFabOnlyAnimation.setDuration(shortAnimationDuration).play(scaleAnimator2).after(scaleAnimator);
                this.mUpdateButtonsOnlyAnimation.setDuration(shortAnimationDuration).play(scaleAnimator5).with(scaleAnimator6).after(scaleAnimator3).after(scaleAnimator4);
                this.mFragmentTabPagerAdapter = new FragmentTabPagerAdapter(this);
                this.mFragmentTabPager = (ViewPager) findViewById(R.id.desk_clock_pager);
                this.mFragmentTabPager.setOffscreenPageLimit(3);
                this.mFragmentTabPager.setAccessibilityDelegate(null);
                this.mFragmentTabPager.addOnPageChangeListener(new PageChangeWatcher());
                this.mFragmentTabPager.setAdapter(this.mFragmentTabPagerAdapter);
                this.mTabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
                    @Override
                    public void onTabSelected(TabLayout.Tab tab2) {
                        UiDataModel.getUiDataModel().setSelectedTab((UiDataModel.Tab) tab2.getTag());
                    }

                    @Override
                    public void onTabUnselected(TabLayout.Tab tab2) {
                    }

                    @Override
                    public void onTabReselected(TabLayout.Tab tab2) {
                    }
                });
                UiDataModel.getUiDataModel().addTabListener(this.mTabChangeWatcher);
                return;
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        DataModel.getDataModel().addSilentSettingsListener(this.mSilentSettingChangeWatcher);
        DataModel.getDataModel().setApplicationInForeground(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        this.mDropShadowController = new DropShadowController(findViewById(R.id.drop_shadow), UiDataModel.getUiDataModel(), this.mSnackbarAnchor.findViewById(R.id.tab_hairline));
        updateCurrentTab();
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        if (this.mRecreateActivity) {
            this.mRecreateActivity = false;
            this.mFragmentTabPager.post(new Runnable() {
                @Override
                public void run() {
                    DeskClock.this.recreate();
                }
            });
        }
    }

    @Override
    public void onPause() {
        if (this.mDropShadowController != null) {
            this.mDropShadowController.stop();
            this.mDropShadowController = null;
        }
        super.onPause();
    }

    @Override
    protected void onStop() {
        DataModel.getDataModel().removeSilentSettingsListener(this.mSilentSettingChangeWatcher);
        if (!isChangingConfigurations()) {
            DataModel.getDataModel().setApplicationInForeground(false);
        }
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        UiDataModel.getUiDataModel().removeTabListener(this.mTabChangeWatcher);
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.mOptionsMenuManager.onCreateOptionsMenu(menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        this.mOptionsMenuManager.onPrepareOptionsMenu(menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        return this.mOptionsMenuManager.onOptionsItemSelected(menuItem) || super.onOptionsItemSelected(menuItem);
    }

    @Override
    public void onDialogLabelSet(Alarm alarm, String str, String str2) {
        Fragment fragmentFindFragmentByTag = getFragmentManager().findFragmentByTag(str2);
        if (fragmentFindFragmentByTag instanceof AlarmClockFragment) {
            ((AlarmClockFragment) fragmentFindFragmentByTag).setLabel(alarm, str);
        }
    }

    @Override
    public boolean onKeyDown(int i, KeyEvent keyEvent) {
        return getSelectedDeskClockFragment().onKeyDown(i, keyEvent) || super.onKeyDown(i, keyEvent);
    }

    @Override
    public void updateFab(@FabContainer.UpdateFabFlag int i) {
        DeskClockFragment selectedDeskClockFragment = getSelectedDeskClockFragment();
        switch (i & 3) {
            case 1:
                selectedDeskClockFragment.onUpdateFab(this.mFab);
                break;
            case 2:
                this.mUpdateFabOnlyAnimation.start();
                break;
            case 3:
                selectedDeskClockFragment.onMorphFab(this.mFab);
                break;
        }
        if ((i & 4) == 4) {
            this.mFab.requestFocus();
        }
        int i2 = i & 24;
        if (i2 == 8) {
            selectedDeskClockFragment.onUpdateFabButtons(this.mLeftButton, this.mRightButton);
        } else if (i2 == 16) {
            this.mUpdateButtonsOnlyAnimation.start();
        }
        if ((i & 32) == 32) {
            this.mLeftButton.setClickable(false);
            this.mRightButton.setClickable(false);
        }
        int i3 = i & FabContainer.FAB_AND_BUTTONS_SHRINK_EXPAND_MASK;
        if (i3 == 64) {
            this.mShowAnimation.start();
        } else if (i3 == 128) {
            this.mHideAnimation.start();
        }
    }

    @Override
    protected void onActivityResult(int i, int i2, Intent intent) {
        if (i == 1 && i2 == -1) {
            this.mRecreateActivity = true;
        }
    }

    private void updateCurrentTab() {
        UiDataModel.Tab selectedTab = UiDataModel.getUiDataModel().getSelectedTab();
        int i = 0;
        while (true) {
            if (i >= this.mTabLayout.getTabCount()) {
                break;
            }
            TabLayout.Tab tabAt = this.mTabLayout.getTabAt(i);
            if (tabAt == null || tabAt.getTag() != selectedTab || tabAt.isSelected()) {
                i++;
            } else {
                tabAt.select();
                break;
            }
        }
        for (int i2 = 0; i2 < this.mFragmentTabPagerAdapter.getCount(); i2++) {
            if (this.mFragmentTabPagerAdapter.getDeskClockFragment(i2).isTabSelected() && this.mFragmentTabPager.getCurrentItem() != i2) {
                this.mFragmentTabPager.setCurrentItem(i2);
                return;
            }
        }
    }

    private DeskClockFragment getSelectedDeskClockFragment() {
        for (int i = 0; i < this.mFragmentTabPagerAdapter.getCount(); i++) {
            DeskClockFragment deskClockFragment = this.mFragmentTabPagerAdapter.getDeskClockFragment(i);
            if (deskClockFragment.isTabSelected()) {
                return deskClockFragment;
            }
        }
        throw new IllegalStateException("Unable to locate selected fragment (" + UiDataModel.getUiDataModel().getSelectedTab() + ")");
    }

    private Snackbar createSnackbar(@StringRes int i) {
        return Snackbar.make(this.mSnackbarAnchor, i, 5000);
    }

    private final class PageChangeWatcher implements ViewPager.OnPageChangeListener {
        private int mPriorState;

        private PageChangeWatcher() {
            this.mPriorState = 0;
        }

        @Override
        public void onPageScrolled(int i, float f, int i2) {
            if (DeskClock.this.mFabState == FabState.HIDE_ARMED && i2 != 0) {
                DeskClock.this.mFabState = FabState.HIDING;
                DeskClock.this.mHideAnimation.start();
            }
        }

        @Override
        public void onPageScrollStateChanged(int i) {
            if (this.mPriorState == 0 && i == 2) {
                DeskClock.this.mHideAnimation.addListener(DeskClock.this.mAutoStartShowListener);
                DeskClock.this.mHideAnimation.start();
                DeskClock.this.mFabState = FabState.HIDING;
            } else if (this.mPriorState == 2 && i == 1) {
                if (DeskClock.this.mShowAnimation.isStarted()) {
                    DeskClock.this.mShowAnimation.cancel();
                }
                if (DeskClock.this.mHideAnimation.isStarted()) {
                    DeskClock.this.mHideAnimation.removeListener(DeskClock.this.mAutoStartShowListener);
                } else {
                    DeskClock.this.mHideAnimation.start();
                    DeskClock.this.mHideAnimation.end();
                }
                DeskClock.this.mFabState = FabState.HIDING;
            } else if (i != 1 && DeskClock.this.mFabState == FabState.HIDING) {
                if (DeskClock.this.mHideAnimation.isStarted()) {
                    DeskClock.this.mHideAnimation.addListener(DeskClock.this.mAutoStartShowListener);
                } else {
                    DeskClock.this.updateFab(9);
                    DeskClock.this.mShowAnimation.start();
                    DeskClock.this.mFabState = FabState.SHOWING;
                }
            } else if (i == 1) {
                DeskClock.this.mFabState = FabState.HIDE_ARMED;
            }
            this.mPriorState = i;
        }

        @Override
        public void onPageSelected(int i) {
            DeskClock.this.mFragmentTabPagerAdapter.getDeskClockFragment(i).selectTab();
        }
    }

    private final class AutoStartShowListener extends AnimatorListenerAdapter {
        private AutoStartShowListener() {
        }

        @Override
        public void onAnimationEnd(Animator animator) {
            DeskClock.this.mHideAnimation.removeListener(DeskClock.this.mAutoStartShowListener);
            DeskClock.this.updateFab(9);
            DeskClock.this.mShowAnimation.start();
            DeskClock.this.mFabState = FabState.SHOWING;
        }
    }

    private final class SilentSettingChangeWatcher implements OnSilentSettingsListener {
        private SilentSettingChangeWatcher() {
        }

        @Override
        public void onSilentSettingsChange(DataModel.SilentSetting silentSetting, DataModel.SilentSetting silentSetting2) {
            if (DeskClock.this.mShowSilentSettingSnackbarRunnable != null) {
                DeskClock.this.mSnackbarAnchor.removeCallbacks(DeskClock.this.mShowSilentSettingSnackbarRunnable);
                DeskClock.this.mShowSilentSettingSnackbarRunnable = null;
            }
            if (silentSetting2 == null) {
                SnackbarManager.dismiss();
                return;
            }
            DeskClock.this.mShowSilentSettingSnackbarRunnable = new ShowSilentSettingSnackbarRunnable(silentSetting2);
            DeskClock.this.mSnackbarAnchor.postDelayed(DeskClock.this.mShowSilentSettingSnackbarRunnable, 1000L);
        }
    }

    private final class ShowSilentSettingSnackbarRunnable implements Runnable {
        private final DataModel.SilentSetting mSilentSetting;

        private ShowSilentSettingSnackbarRunnable(DataModel.SilentSetting silentSetting) {
            this.mSilentSetting = silentSetting;
        }

        @Override
        public void run() {
            Snackbar snackbarCreateSnackbar = DeskClock.this.createSnackbar(this.mSilentSetting.getLabelResId());
            if (this.mSilentSetting.isActionEnabled(DeskClock.this)) {
                snackbarCreateSnackbar.setAction(this.mSilentSetting.getActionResId(), this.mSilentSetting.getActionListener());
            }
            SnackbarManager.show(snackbarCreateSnackbar);
        }
    }

    private final class TabChangeWatcher implements TabListener {
        private TabChangeWatcher() {
        }

        @Override
        public void selectedTabChanged(UiDataModel.Tab tab, UiDataModel.Tab tab2) {
            DeskClock.this.updateCurrentTab();
            if (DataModel.getDataModel().isApplicationInForeground()) {
                switch (tab2) {
                    case ALARMS:
                        Events.sendAlarmEvent(R.string.action_show, R.string.label_deskclock);
                        break;
                    case CLOCKS:
                        Events.sendClockEvent(R.string.action_show, R.string.label_deskclock);
                        break;
                    case TIMERS:
                        Events.sendTimerEvent(R.string.action_show, R.string.label_deskclock);
                        break;
                    case STOPWATCH:
                        Events.sendStopwatchEvent(R.string.action_show, R.string.label_deskclock);
                        break;
                }
            }
            if (!DeskClock.this.mHideAnimation.isStarted()) {
                DeskClock.this.updateFab(9);
            }
        }
    }
}
