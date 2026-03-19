package com.android.systemui.qs.customize;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
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
import com.android.settingslib.Utils;
import com.android.systemui.Dependency;
import com.android.systemui.R;
import com.android.systemui.plugins.qs.QS;
import com.android.systemui.plugins.qs.QSTile;
import com.android.systemui.qs.QSDetailClipper;
import com.android.systemui.qs.QSTileHost;
import com.android.systemui.statusbar.phone.LightBarController;
import com.android.systemui.statusbar.phone.NotificationsQuickSettingsContainer;
import com.android.systemui.statusbar.policy.KeyguardMonitor;
import com.mediatek.systemui.ext.OpSystemUICustomizationFactoryBase;
import java.util.ArrayList;
import java.util.Iterator;

public class QSCustomizer extends LinearLayout implements Toolbar.OnMenuItemClickListener {
    private boolean isShown;
    private final QSDetailClipper mClipper;
    private final Animator.AnimatorListener mCollapseAnimationListener;
    private boolean mCustomizing;
    private final Animator.AnimatorListener mExpandAnimationListener;
    private QSTileHost mHost;
    private boolean mIsShowingNavBackdrop;
    private final KeyguardMonitor.Callback mKeyguardCallback;
    private final LightBarController mLightBarController;
    private NotificationsQuickSettingsContainer mNotifQsContainer;
    private boolean mOpening;
    private QS mQs;
    private RecyclerView mRecyclerView;
    private TileAdapter mTileAdapter;
    private final TileQueryHelper mTileQueryHelper;
    private Toolbar mToolbar;
    private int mX;
    private int mY;

    public QSCustomizer(Context context, AttributeSet attributeSet) {
        super(new ContextThemeWrapper(context, R.style.edit_theme), attributeSet);
        this.mKeyguardCallback = new KeyguardMonitor.Callback() {
            @Override
            public final void onKeyguardShowingChanged() {
                QSCustomizer.lambda$new$0(this.f$0);
            }
        };
        this.mExpandAnimationListener = new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animator) {
                if (QSCustomizer.this.isShown) {
                    QSCustomizer.this.setCustomizing(true);
                }
                QSCustomizer.this.mOpening = false;
                QSCustomizer.this.mNotifQsContainer.setCustomizerAnimating(false);
            }

            @Override
            public void onAnimationCancel(Animator animator) {
                QSCustomizer.this.mOpening = false;
                QSCustomizer.this.mNotifQsContainer.setCustomizerAnimating(false);
            }
        };
        this.mCollapseAnimationListener = new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animator) {
                if (!QSCustomizer.this.isShown) {
                    QSCustomizer.this.setVisibility(8);
                }
                QSCustomizer.this.mNotifQsContainer.setCustomizerAnimating(false);
                QSCustomizer.this.mRecyclerView.setAdapter(QSCustomizer.this.mTileAdapter);
            }

            @Override
            public void onAnimationCancel(Animator animator) {
                if (!QSCustomizer.this.isShown) {
                    QSCustomizer.this.setVisibility(8);
                }
                QSCustomizer.this.mNotifQsContainer.setCustomizerAnimating(false);
            }
        };
        LayoutInflater.from(getContext()).inflate(R.layout.qs_customize_panel_content, this);
        this.mClipper = new QSDetailClipper(findViewById(R.id.customize_container));
        this.mToolbar = (Toolbar) findViewById(android.R.id.KEYCODE_TV_RADIO_SERVICE);
        TypedValue typedValue = new TypedValue();
        this.mContext.getTheme().resolveAttribute(android.R.attr.homeAsUpIndicator, typedValue, true);
        this.mToolbar.setNavigationIcon(getResources().getDrawable(typedValue.resourceId, this.mContext.getTheme()));
        this.mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                QSCustomizer.this.hide(((int) view.getX()) + (view.getWidth() / 2), ((int) view.getY()) + (view.getHeight() / 2));
            }
        });
        this.mToolbar.setOnMenuItemClickListener(this);
        this.mToolbar.getMenu().add(0, 1, 0, this.mContext.getString(android.R.string.kg_sim_unlock_progress_dialog_message));
        this.mToolbar.setTitle(R.string.qs_edit);
        int colorAttr = Utils.getColorAttr(context, android.R.attr.colorAccent);
        this.mToolbar.setTitleTextColor(colorAttr);
        this.mToolbar.getNavigationIcon().setTint(colorAttr);
        this.mToolbar.getOverflowIcon().setTint(colorAttr);
        this.mRecyclerView = (RecyclerView) findViewById(android.R.id.list);
        this.mTileAdapter = new TileAdapter(getContext());
        this.mTileQueryHelper = new TileQueryHelper(context, this.mTileAdapter);
        this.mRecyclerView.setAdapter(this.mTileAdapter);
        this.mTileAdapter.getItemTouchHelper().attachToRecyclerView(this.mRecyclerView);
        GridLayoutManager gridLayoutManager = new GridLayoutManager(getContext(), 3);
        gridLayoutManager.setSpanSizeLookup(this.mTileAdapter.getSizeLookup());
        this.mRecyclerView.setLayoutManager(gridLayoutManager);
        this.mRecyclerView.addItemDecoration(this.mTileAdapter.getItemDecoration());
        DefaultItemAnimator defaultItemAnimator = new DefaultItemAnimator();
        defaultItemAnimator.setMoveDuration(150L);
        this.mRecyclerView.setItemAnimator(defaultItemAnimator);
        this.mLightBarController = (LightBarController) Dependency.get(LightBarController.class);
        updateNavBackDrop(getResources().getConfiguration());
    }

    @Override
    protected void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        updateNavBackDrop(configuration);
    }

    private void updateNavBackDrop(Configuration configuration) {
        View viewFindViewById = findViewById(R.id.nav_bar_background);
        this.mIsShowingNavBackdrop = configuration.smallestScreenWidthDp >= 600 || configuration.orientation != 2;
        if (viewFindViewById != null) {
            viewFindViewById.setVisibility(this.mIsShowingNavBackdrop ? 0 : 8);
        }
        updateNavColors();
    }

    private void updateNavColors() {
        this.mLightBarController.setQsCustomizing(this.mIsShowingNavBackdrop && this.isShown);
    }

    public void setHost(QSTileHost qSTileHost) {
        this.mHost = qSTileHost;
        this.mTileAdapter.setHost(qSTileHost);
    }

    public void setContainer(NotificationsQuickSettingsContainer notificationsQuickSettingsContainer) {
        this.mNotifQsContainer = notificationsQuickSettingsContainer;
    }

    public void setQs(QS qs) {
        this.mQs = qs;
    }

    public void show(int i, int i2) {
        if (!this.isShown) {
            this.mX = i;
            this.mY = i2;
            MetricsLogger.visible(getContext(), 358);
            this.isShown = true;
            this.mOpening = true;
            setTileSpecs();
            setVisibility(0);
            this.mClipper.animateCircularClip(i, i2, true, this.mExpandAnimationListener);
            queryTiles();
            this.mNotifQsContainer.setCustomizerAnimating(true);
            this.mNotifQsContainer.setCustomizerShowing(true);
            ((KeyguardMonitor) Dependency.get(KeyguardMonitor.class)).addCallback(this.mKeyguardCallback);
            updateNavColors();
        }
    }

    public void showImmediately() {
        if (!this.isShown) {
            setVisibility(0);
            this.mClipper.showBackground();
            this.isShown = true;
            setTileSpecs();
            setCustomizing(true);
            queryTiles();
            this.mNotifQsContainer.setCustomizerAnimating(false);
            this.mNotifQsContainer.setCustomizerShowing(true);
            ((KeyguardMonitor) Dependency.get(KeyguardMonitor.class)).addCallback(this.mKeyguardCallback);
            updateNavColors();
        }
    }

    private void queryTiles() {
        this.mTileQueryHelper.queryTiles(this.mHost);
    }

    public void hide(int i, int i2) {
        if (this.isShown) {
            MetricsLogger.hidden(getContext(), 358);
            this.isShown = false;
            this.mToolbar.dismissPopupMenus();
            setCustomizing(false);
            save();
            this.mClipper.animateCircularClip(this.mX, this.mY, false, this.mCollapseAnimationListener);
            this.mNotifQsContainer.setCustomizerAnimating(true);
            this.mNotifQsContainer.setCustomizerShowing(false);
            ((KeyguardMonitor) Dependency.get(KeyguardMonitor.class)).removeCallback(this.mKeyguardCallback);
            updateNavColors();
        }
    }

    @Override
    public boolean isShown() {
        return this.isShown;
    }

    private void setCustomizing(boolean z) {
        this.mCustomizing = z;
        this.mQs.notifyCustomizeChanged();
    }

    public boolean isCustomizing() {
        return this.mCustomizing || this.mOpening;
    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        if (menuItem.getItemId() == 1) {
            MetricsLogger.action(getContext(), 359);
            reset();
            return false;
        }
        return false;
    }

    private void reset() {
        ArrayList arrayList = new ArrayList();
        for (String str : OpSystemUICustomizationFactoryBase.getOpFactory(this.mContext).makeQuickSettings(this.mContext).customizeQuickSettingsTileOrder(this.mContext.getString(R.string.quick_settings_tiles_default)).split(",")) {
            arrayList.add(str);
        }
        this.mTileAdapter.resetTileSpecs(this.mHost, arrayList);
    }

    private void setTileSpecs() {
        ArrayList arrayList = new ArrayList();
        Iterator<QSTile> it = this.mHost.getTiles().iterator();
        while (it.hasNext()) {
            arrayList.add(it.next().getTileSpec());
        }
        this.mTileAdapter.setTileSpecs(arrayList);
        this.mRecyclerView.setAdapter(this.mTileAdapter);
    }

    private void save() {
        if (this.mTileQueryHelper.isFinished()) {
            this.mTileAdapter.saveSpecs(this.mHost);
        }
    }

    public void saveInstanceState(Bundle bundle) {
        if (this.isShown) {
            ((KeyguardMonitor) Dependency.get(KeyguardMonitor.class)).removeCallback(this.mKeyguardCallback);
        }
        bundle.putBoolean("qs_customizing", this.mCustomizing);
    }

    public void restoreInstanceState(Bundle bundle) {
        if (bundle.getBoolean("qs_customizing")) {
            setVisibility(0);
            addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
                @Override
                public void onLayoutChange(View view, int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8) {
                    QSCustomizer.this.removeOnLayoutChangeListener(this);
                    QSCustomizer.this.showImmediately();
                }
            });
        }
    }

    public void setEditLocation(int i, int i2) {
        this.mX = i;
        this.mY = i2;
    }

    public static void lambda$new$0(QSCustomizer qSCustomizer) {
        if (qSCustomizer.isAttachedToWindow() && ((KeyguardMonitor) Dependency.get(KeyguardMonitor.class)).isShowing() && !qSCustomizer.mOpening) {
            qSCustomizer.hide(0, 0);
        }
    }
}
