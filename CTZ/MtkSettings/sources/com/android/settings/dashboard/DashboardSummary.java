package com.android.settings.dashboard;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.service.settings.suggestions.Suggestion;
import android.support.v7.widget.LinearLayoutManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.android.settings.R;
import com.android.settings.core.InstrumentedFragment;
import com.android.settings.dashboard.conditional.Condition;
import com.android.settings.dashboard.conditional.ConditionManager;
import com.android.settings.dashboard.conditional.FocusRecyclerView;
import com.android.settings.dashboard.suggestions.SuggestionFeatureProvider;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.widget.ActionBarShadowController;
import com.android.settingslib.drawer.DashboardCategory;
import com.android.settingslib.drawer.SettingsDrawerActivity;
import com.android.settingslib.suggestions.SuggestionControllerMixin;
import com.android.settingslib.utils.ThreadUtils;
import java.util.List;

public class DashboardSummary extends InstrumentedFragment implements ConditionManager.ConditionListener, FocusRecyclerView.DetachListener, FocusRecyclerView.FocusListener, SettingsDrawerActivity.CategoryListener, SuggestionControllerMixin.SuggestionControllerHost {
    private DashboardAdapter mAdapter;
    private ConditionManager mConditionManager;
    private FocusRecyclerView mDashboard;
    private DashboardFeatureProvider mDashboardFeatureProvider;
    private final Handler mHandler = new Handler();
    boolean mIsOnCategoriesChangedCalled;
    private LinearLayoutManager mLayoutManager;
    private boolean mOnConditionsChangedCalled;
    private DashboardCategory mStagingCategory;
    private List<Suggestion> mStagingSuggestions;
    private SuggestionControllerMixin mSuggestionControllerMixin;
    private SummaryLoader mSummaryLoader;

    @Override
    public int getMetricsCategory() {
        return 35;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Log.d("DashboardSummary", "Creating SuggestionControllerMixin");
        SuggestionFeatureProvider suggestionFeatureProvider = FeatureFactory.getFactory(context).getSuggestionFeatureProvider(context);
        if (suggestionFeatureProvider.isSuggestionEnabled(context)) {
            this.mSuggestionControllerMixin = new SuggestionControllerMixin(context, this, getLifecycle(), suggestionFeatureProvider.getSuggestionServiceComponent());
        }
    }

    @Override
    public LoaderManager getLoaderManager() {
        if (!isAdded()) {
            return null;
        }
        return super.getLoaderManager();
    }

    @Override
    public void onCreate(Bundle bundle) {
        System.currentTimeMillis();
        super.onCreate(bundle);
        Log.d("DashboardSummary", "Starting DashboardSummary");
        Activity activity = getActivity();
        this.mDashboardFeatureProvider = FeatureFactory.getFactory(activity).getDashboardFeatureProvider(activity);
        this.mSummaryLoader = new SummaryLoader(activity, "com.android.settings.category.ia.homepage");
        this.mConditionManager = ConditionManager.get(activity, false);
        getLifecycle().addObserver(this.mConditionManager);
        if (bundle != null) {
            this.mIsOnCategoriesChangedCalled = bundle.getBoolean("categories_change_called");
        }
    }

    @Override
    public void onDestroy() {
        this.mSummaryLoader.release();
        super.onDestroy();
    }

    @Override
    public void onResume() {
        System.currentTimeMillis();
        super.onResume();
        ((SettingsDrawerActivity) getActivity()).addCategoryListener(this);
        this.mSummaryLoader.setListening(true);
        int metricsCategory = getMetricsCategory();
        for (Condition condition : this.mConditionManager.getConditions()) {
            if (condition.shouldShow()) {
                this.mMetricsFeatureProvider.visible(getContext(), metricsCategory, condition.getMetricsConstant());
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        ((SettingsDrawerActivity) getActivity()).remCategoryListener(this);
        this.mSummaryLoader.setListening(false);
        for (Condition condition : this.mConditionManager.getConditions()) {
            if (condition.shouldShow()) {
                this.mMetricsFeatureProvider.hidden(getContext(), condition.getMetricsConstant());
            }
        }
    }

    @Override
    public void onWindowFocusChanged(boolean z) {
        System.currentTimeMillis();
        if (z) {
            Log.d("DashboardSummary", "Listening for condition changes");
            this.mConditionManager.addListener(this);
            Log.d("DashboardSummary", "conditions refreshed");
            this.mConditionManager.refreshAll();
            return;
        }
        Log.d("DashboardSummary", "Stopped listening for condition changes");
        this.mConditionManager.remListener(this);
    }

    @Override
    public void onDetachedFromWindow() {
        Log.d("DashboardSummary", "Detached from window, stop listening for condition changes");
        this.mConditionManager.remListener(this);
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        if (this.mLayoutManager == null) {
            return;
        }
        bundle.putBoolean("categories_change_called", this.mIsOnCategoriesChangedCalled);
        bundle.putInt("scroll_position", this.mLayoutManager.findFirstVisibleItemPosition());
    }

    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        System.currentTimeMillis();
        View viewInflate = layoutInflater.inflate(R.layout.dashboard, viewGroup, false);
        this.mDashboard = (FocusRecyclerView) viewInflate.findViewById(R.id.dashboard_container);
        this.mLayoutManager = new LinearLayoutManager(getContext());
        this.mLayoutManager.setOrientation(1);
        if (bundle != null) {
            this.mLayoutManager.scrollToPosition(bundle.getInt("scroll_position"));
        }
        this.mDashboard.setLayoutManager(this.mLayoutManager);
        this.mDashboard.setHasFixedSize(true);
        this.mDashboard.setListener(this);
        this.mDashboard.setDetachListener(this);
        this.mDashboard.setItemAnimator(new DashboardItemAnimator());
        this.mAdapter = new DashboardAdapter(getContext(), bundle, this.mConditionManager.getConditions(), this.mSuggestionControllerMixin, getLifecycle());
        this.mDashboard.setAdapter(this.mAdapter);
        this.mSummaryLoader.setSummaryConsumer(this.mAdapter);
        ActionBarShadowController.attachToRecyclerView(getActivity().findViewById(R.id.search_bar_container), getLifecycle(), this.mDashboard);
        rebuildUI();
        return viewInflate;
    }

    void rebuildUI() {
        ThreadUtils.postOnBackgroundThread(new Runnable() {
            @Override
            public final void run() {
                this.f$0.updateCategory();
            }
        });
    }

    @Override
    public void onCategoriesChanged() {
        if (this.mIsOnCategoriesChangedCalled) {
            rebuildUI();
        }
        this.mIsOnCategoriesChangedCalled = true;
    }

    @Override
    public void onConditionsChanged() {
        Log.d("DashboardSummary", "onConditionsChanged");
        if (this.mOnConditionsChangedCalled) {
            boolean z = this.mLayoutManager.findFirstCompletelyVisibleItemPosition() <= 1;
            this.mAdapter.setConditions(this.mConditionManager.getConditions());
            if (z) {
                this.mDashboard.scrollToPosition(0);
                return;
            }
            return;
        }
        this.mOnConditionsChangedCalled = true;
    }

    @Override
    public void onSuggestionReady(List<Suggestion> list) {
        this.mStagingSuggestions = list;
        this.mAdapter.setSuggestions(list);
        if (this.mStagingCategory != null) {
            Log.d("DashboardSummary", "Category has loaded, setting category from suggestionReady");
            this.mHandler.removeCallbacksAndMessages(null);
            this.mAdapter.setCategory(this.mStagingCategory);
        }
    }

    void updateCategory() {
        DashboardCategory tilesForCategory = this.mDashboardFeatureProvider.getTilesForCategory("com.android.settings.category.ia.homepage");
        this.mSummaryLoader.updateSummaryToCache(tilesForCategory);
        this.mStagingCategory = tilesForCategory;
        if (this.mSuggestionControllerMixin == null) {
            ThreadUtils.postOnMainThread(new Runnable() {
                @Override
                public final void run() {
                    DashboardSummary dashboardSummary = this.f$0;
                    dashboardSummary.mAdapter.setCategory(dashboardSummary.mStagingCategory);
                }
            });
        } else if (this.mSuggestionControllerMixin.isSuggestionLoaded()) {
            Log.d("DashboardSummary", "Suggestion has loaded, setting suggestion/category");
            ThreadUtils.postOnMainThread(new Runnable() {
                @Override
                public final void run() {
                    DashboardSummary.lambda$updateCategory$2(this.f$0);
                }
            });
        } else {
            Log.d("DashboardSummary", "Suggestion NOT loaded, delaying setCategory by 3000ms");
            this.mHandler.postDelayed(new Runnable() {
                @Override
                public final void run() {
                    DashboardSummary dashboardSummary = this.f$0;
                    dashboardSummary.mAdapter.setCategory(dashboardSummary.mStagingCategory);
                }
            }, 3000L);
        }
    }

    public static void lambda$updateCategory$2(DashboardSummary dashboardSummary) {
        if (dashboardSummary.mStagingSuggestions != null) {
            dashboardSummary.mAdapter.setSuggestions(dashboardSummary.mStagingSuggestions);
        }
        dashboardSummary.mAdapter.setCategory(dashboardSummary.mStagingCategory);
    }
}
