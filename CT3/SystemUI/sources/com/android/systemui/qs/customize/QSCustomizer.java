package com.android.systemui.qs.customize;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.res.Configuration;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toolbar;
import com.android.internal.logging.MetricsLogger;
import com.android.systemui.R;
import com.android.systemui.qs.QSContainer;
import com.android.systemui.qs.QSDetailClipper;
import com.android.systemui.qs.QSTile;
import com.android.systemui.statusbar.phone.NotificationsQuickSettingsContainer;
import com.android.systemui.statusbar.phone.PhoneStatusBar;
import com.android.systemui.statusbar.phone.QSTileHost;
import com.android.systemui.statusbar.policy.KeyguardMonitor;
import com.mediatek.systemui.PluginManager;
import com.mediatek.systemui.ext.IQuickSettingsPlugin;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class QSCustomizer extends LinearLayout implements Toolbar.OnMenuItemClickListener {
    private boolean isShown;
    private final QSDetailClipper mClipper;
    private final Animator.AnimatorListener mCollapseAnimationListener;
    private boolean mCustomizing;
    private final Animator.AnimatorListener mExpandAnimationListener;
    private QSTileHost mHost;
    private final KeyguardMonitor.Callback mKeyguardCallback;
    private NotificationsQuickSettingsContainer mNotifQsContainer;
    private PhoneStatusBar mPhoneStatusBar;
    private QSContainer mQsContainer;
    private RecyclerView mRecyclerView;
    private TileAdapter mTileAdapter;
    private Toolbar mToolbar;

    public QSCustomizer(Context context, AttributeSet attrs) {
        super(new ContextThemeWrapper(context, R.style.edit_theme), attrs);
        this.mKeyguardCallback = new KeyguardMonitor.Callback() {
            @Override
            public void onKeyguardChanged() {
                if (!QSCustomizer.this.mHost.getKeyguardMonitor().isShowing()) {
                    return;
                }
                QSCustomizer.this.hide(0, 0);
            }
        };
        this.mExpandAnimationListener = new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                QSCustomizer.this.setCustomizing(true);
                QSCustomizer.this.mNotifQsContainer.setCustomizerAnimating(false);
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                QSCustomizer.this.mNotifQsContainer.setCustomizerAnimating(false);
            }
        };
        this.mCollapseAnimationListener = new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (!QSCustomizer.this.isShown) {
                    QSCustomizer.this.setVisibility(8);
                }
                QSCustomizer.this.mNotifQsContainer.setCustomizerAnimating(false);
                QSCustomizer.this.mRecyclerView.setAdapter(QSCustomizer.this.mTileAdapter);
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                if (!QSCustomizer.this.isShown) {
                    QSCustomizer.this.setVisibility(8);
                }
                QSCustomizer.this.mNotifQsContainer.setCustomizerAnimating(false);
            }
        };
        this.mClipper = new QSDetailClipper(this);
        LayoutInflater.from(getContext()).inflate(R.layout.qs_customize_panel_content, this);
        this.mToolbar = (Toolbar) findViewById(android.R.id.messaging_group_sending_progress_container);
        TypedValue value = new TypedValue();
        this.mContext.getTheme().resolveAttribute(android.R.attr.homeAsUpIndicator, value, true);
        this.mToolbar.setNavigationIcon(getResources().getDrawable(value.resourceId, this.mContext.getTheme()));
        this.mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                QSCustomizer.this.hide(((int) v.getX()) + (v.getWidth() / 2), ((int) v.getY()) + (v.getHeight() / 2));
            }
        });
        this.mToolbar.setOnMenuItemClickListener(this);
        this.mToolbar.getMenu().add(0, 1, 0, this.mContext.getString(android.R.string.global_action_lockdown));
        this.mToolbar.setTitle(R.string.qs_edit);
        this.mRecyclerView = (RecyclerView) findViewById(android.R.id.list);
        this.mTileAdapter = new TileAdapter(getContext());
        this.mRecyclerView.setAdapter(this.mTileAdapter);
        this.mTileAdapter.getItemTouchHelper().attachToRecyclerView(this.mRecyclerView);
        GridLayoutManager layout = new GridLayoutManager(getContext(), 3);
        layout.setSpanSizeLookup(this.mTileAdapter.getSizeLookup());
        this.mRecyclerView.setLayoutManager(layout);
        this.mRecyclerView.addItemDecoration(this.mTileAdapter.getItemDecoration());
        DefaultItemAnimator animator = new DefaultItemAnimator();
        animator.setMoveDuration(150L);
        this.mRecyclerView.setItemAnimator(animator);
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        boolean shouldShow = true;
        super.onConfigurationChanged(newConfig);
        View navBackdrop = findViewById(R.id.nav_bar_background);
        if (navBackdrop == null) {
            return;
        }
        if (newConfig.smallestScreenWidthDp < 600 && newConfig.orientation == 2) {
            shouldShow = false;
        }
        navBackdrop.setVisibility(shouldShow ? 0 : 8);
    }

    public void setHost(QSTileHost host) {
        this.mHost = host;
        this.mPhoneStatusBar = host.getPhoneStatusBar();
        this.mTileAdapter.setHost(host);
    }

    public void setContainer(NotificationsQuickSettingsContainer notificationsQsContainer) {
        this.mNotifQsContainer = notificationsQsContainer;
    }

    public void setQsContainer(QSContainer qsContainer) {
        this.mQsContainer = qsContainer;
    }

    public void show(int x, int y) {
        if (this.isShown) {
            return;
        }
        MetricsLogger.visible(getContext(), 358);
        this.isShown = true;
        setTileSpecs();
        setVisibility(0);
        this.mClipper.animateCircularClip(x, y, true, this.mExpandAnimationListener);
        new TileQueryHelper(this.mContext, this.mHost).setListener(this.mTileAdapter);
        this.mNotifQsContainer.setCustomizerAnimating(true);
        this.mNotifQsContainer.setCustomizerShowing(true);
        announceForAccessibility(this.mContext.getString(R.string.accessibility_desc_quick_settings_edit));
        this.mHost.getKeyguardMonitor().addCallback(this.mKeyguardCallback);
    }

    public void hide(int x, int y) {
        if (!this.isShown) {
            return;
        }
        MetricsLogger.hidden(getContext(), 358);
        this.isShown = false;
        this.mToolbar.dismissPopupMenus();
        setCustomizing(false);
        save();
        this.mClipper.animateCircularClip(x, y, false, this.mCollapseAnimationListener);
        this.mNotifQsContainer.setCustomizerAnimating(true);
        this.mNotifQsContainer.setCustomizerShowing(false);
        announceForAccessibility(this.mContext.getString(R.string.accessibility_desc_quick_settings));
        this.mHost.getKeyguardMonitor().removeCallback(this.mKeyguardCallback);
    }

    public void setCustomizing(boolean customizing) {
        this.mCustomizing = customizing;
        this.mQsContainer.notifyCustomizeChanged();
    }

    public boolean isCustomizing() {
        return this.mCustomizing;
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case 1:
                MetricsLogger.action(getContext(), 359);
                reset();
                break;
        }
        return false;
    }

    private void reset() {
        ArrayList<String> tiles = new ArrayList<>();
        String defTiles = this.mContext.getString(R.string.quick_settings_tiles_default);
        IQuickSettingsPlugin quickSettingsPlugin = PluginManager.getQuickSettingsPlugin(this.mContext);
        for (String tile : quickSettingsPlugin.customizeQuickSettingsTileOrder(defTiles).split(",")) {
            tiles.add(tile);
        }
        this.mTileAdapter.setTileSpecs(tiles);
    }

    private void setTileSpecs() {
        List<String> specs = new ArrayList<>();
        Iterator tile$iterator = this.mHost.getTiles().iterator();
        while (tile$iterator.hasNext()) {
            QSTile tile = (QSTile) tile$iterator.next();
            specs.add(tile.getTileSpec());
        }
        this.mTileAdapter.setTileSpecs(specs);
        this.mRecyclerView.setAdapter(this.mTileAdapter);
    }

    private void save() {
        this.mTileAdapter.saveSpecs(this.mHost);
    }
}
