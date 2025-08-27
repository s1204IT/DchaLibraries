package com.android.launcher3.allapps;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Process;
import android.support.animation.DynamicAnimation;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Selection;
import android.text.SpannableStringBuilder;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import com.android.launcher3.AppInfo;
import com.android.launcher3.DeviceProfile;
import com.android.launcher3.DragSource;
import com.android.launcher3.DropTarget;
import com.android.launcher3.Insettable;
import com.android.launcher3.InsettableFrameLayout;
import com.android.launcher3.ItemInfo;
import com.android.launcher3.Launcher;
import com.android.launcher3.R;
import com.android.launcher3.Utilities;
import com.android.launcher3.allapps.AllAppsStore;
import com.android.launcher3.keyboard.FocusedItemDecorator;
import com.android.launcher3.userevent.nano.LauncherLogProto;
import com.android.launcher3.util.ItemInfoMatcher;
import com.android.launcher3.util.Themes;
import com.android.launcher3.views.BottomUserEducationView;
import com.android.launcher3.views.RecyclerViewFastScroller;
import com.android.launcher3.views.SpringRelativeLayout;
import java.util.Iterator;

/* loaded from: classes.dex */
public class AllAppsContainerView extends SpringRelativeLayout implements DragSource, Insettable, DeviceProfile.OnDeviceProfileChangeListener {
    private static final float FLING_ANIMATION_THRESHOLD = 0.55f;
    private static final float FLING_VELOCITY_MULTIPLIER = 135.0f;
    private final AdapterHolder[] mAH;
    private final AllAppsStore mAllAppsStore;
    private final Point mFastScrollerOffset;
    private FloatingHeaderView mHeader;
    private final Launcher mLauncher;
    private int mNavBarScrimHeight;
    private final Paint mNavBarScrimPaint;
    private final ItemInfoMatcher mPersonalMatcher;
    private View mSearchContainer;
    private boolean mSearchModeWhileUsingTabs;
    private SpannableStringBuilder mSearchQueryBuilder;
    private SearchUiManager mSearchUiManager;
    private RecyclerViewFastScroller mTouchHandler;
    private boolean mUsingTabs;
    private AllAppsPagedView mViewPager;
    private final ItemInfoMatcher mWorkMatcher;

    public AllAppsContainerView(Context context) {
        this(context, null);
    }

    public AllAppsContainerView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public AllAppsContainerView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mPersonalMatcher = ItemInfoMatcher.ofUser(Process.myUserHandle());
        this.mWorkMatcher = ItemInfoMatcher.not(this.mPersonalMatcher);
        this.mAllAppsStore = new AllAppsStore();
        this.mNavBarScrimHeight = 0;
        this.mSearchQueryBuilder = null;
        this.mSearchModeWhileUsingTabs = false;
        this.mFastScrollerOffset = new Point();
        this.mLauncher = Launcher.getLauncher(context);
        this.mLauncher.addOnDeviceProfileChangeListener(this);
        this.mSearchQueryBuilder = new SpannableStringBuilder();
        Selection.setSelection(this.mSearchQueryBuilder, 0);
        this.mAH = new AdapterHolder[2];
        this.mAH[0] = new AdapterHolder(false);
        this.mAH[1] = new AdapterHolder(true);
        this.mNavBarScrimPaint = new Paint();
        this.mNavBarScrimPaint.setColor(Themes.getAttrColor(context, R.attr.allAppsNavBarScrimColor));
        this.mAllAppsStore.addUpdateListener(new AllAppsStore.OnUpdateListener() { // from class: com.android.launcher3.allapps.-$$Lambda$AllAppsContainerView$NGv9lLb-21_YfP50YgkH4gojlD8
            @Override // com.android.launcher3.allapps.AllAppsStore.OnUpdateListener
            public final void onAppsUpdated() {
                this.f$0.onAppsUpdated();
            }
        });
        addSpringView(R.id.all_apps_header);
        addSpringView(R.id.apps_list_view);
        addSpringView(R.id.all_apps_tabs_view_pager);
    }

    public AllAppsStore getAppsStore() {
        return this.mAllAppsStore;
    }

    @Override // com.android.launcher3.views.SpringRelativeLayout
    protected void setDampedScrollShift(float f) {
        if (getSearchView() == null) {
            return;
        }
        float height = getSearchView().getHeight() / 2.0f;
        super.setDampedScrollShift(Utilities.boundToRange(f, -height, height));
    }

    @Override // com.android.launcher3.DeviceProfile.OnDeviceProfileChangeListener
    public void onDeviceProfileChanged(DeviceProfile deviceProfile) {
        for (AdapterHolder adapterHolder : this.mAH) {
            if (adapterHolder.recyclerView != null) {
                adapterHolder.recyclerView.swapAdapter(adapterHolder.recyclerView.getAdapter(), true);
                adapterHolder.recyclerView.getRecycledViewPool().clear();
            }
        }
    }

    private void onAppsUpdated() {
        boolean z;
        Iterator<AppInfo> it = this.mAllAppsStore.getApps().iterator();
        while (true) {
            if (it.hasNext()) {
                if (this.mWorkMatcher.matches(it.next(), null)) {
                    z = true;
                    break;
                }
            } else {
                z = false;
                break;
            }
        }
        rebindAdapters(z);
    }

    public boolean shouldContainerScroll(MotionEvent motionEvent) {
        AllAppsRecyclerView activeRecyclerView;
        if ((this.mSearchContainer != null && this.mLauncher.getDragLayer().isEventOverView(this.mSearchContainer, motionEvent)) || (activeRecyclerView = getActiveRecyclerView()) == null) {
            return true;
        }
        if (activeRecyclerView.getScrollbar().getThumbOffsetY() >= 0 && this.mLauncher.getDragLayer().isEventOverView(activeRecyclerView.getScrollbar(), motionEvent)) {
            return false;
        }
        return activeRecyclerView.shouldContainerScroll(motionEvent, this.mLauncher.getDragLayer());
    }

    @Override // android.view.ViewGroup
    public boolean onInterceptTouchEvent(MotionEvent motionEvent) {
        AllAppsRecyclerView activeRecyclerView;
        if (motionEvent.getAction() == 0 && (activeRecyclerView = getActiveRecyclerView()) != null && activeRecyclerView.getScrollbar().isHitInParent(motionEvent.getX(), motionEvent.getY(), this.mFastScrollerOffset)) {
            this.mTouchHandler = activeRecyclerView.getScrollbar();
        }
        if (this.mTouchHandler != null) {
            return this.mTouchHandler.handleTouchEvent(motionEvent, this.mFastScrollerOffset);
        }
        return false;
    }

    @Override // android.view.View
    public boolean onTouchEvent(MotionEvent motionEvent) {
        if (this.mTouchHandler != null) {
            this.mTouchHandler.handleTouchEvent(motionEvent, this.mFastScrollerOffset);
            return true;
        }
        return false;
    }

    public String getDescription() {
        int i;
        if (this.mUsingTabs) {
            if (this.mViewPager.getNextPage() == 0) {
                i = R.string.all_apps_button_personal_label;
            } else {
                i = R.string.all_apps_button_work_label;
            }
        } else {
            i = R.string.all_apps_button_label;
        }
        return getContext().getString(i);
    }

    public AllAppsRecyclerView getActiveRecyclerView() {
        if (!this.mUsingTabs || this.mViewPager.getNextPage() == 0) {
            return this.mAH[0].recyclerView;
        }
        return this.mAH[1].recyclerView;
    }

    public void reset(boolean z) {
        for (int i = 0; i < this.mAH.length; i++) {
            if (this.mAH[i].recyclerView != null) {
                this.mAH[i].recyclerView.scrollToTop();
            }
        }
        if (isHeaderVisible()) {
            this.mHeader.reset(z);
        }
        if (this.mSearchUiManager != null) {
            this.mSearchUiManager.resetSearch();
        }
    }

    @Override // android.view.View
    protected void onFinishInflate() {
        super.onFinishInflate();
        setOnFocusChangeListener(new View.OnFocusChangeListener() { // from class: com.android.launcher3.allapps.-$$Lambda$AllAppsContainerView$Ie_dilnu7rHF372CEXg0SYftSqs
            @Override // android.view.View.OnFocusChangeListener
            public final void onFocusChange(View view, boolean z) {
                AllAppsContainerView.lambda$onFinishInflate$0(this.f$0, view, z);
            }
        });
        this.mHeader = (FloatingHeaderView) findViewById(R.id.all_apps_header);
        rebindAdapters(this.mUsingTabs, true);
        this.mSearchContainer = findViewById(R.id.search_container_all_apps);
        this.mSearchUiManager = (SearchUiManager) this.mSearchContainer;
        if (this.mSearchUiManager != null) {
            this.mSearchUiManager.initialize(this);
        }
    }

    public static /* synthetic */ void lambda$onFinishInflate$0(AllAppsContainerView allAppsContainerView, View view, boolean z) {
        if (z && allAppsContainerView.getActiveRecyclerView() != null) {
            allAppsContainerView.getActiveRecyclerView().requestFocus();
        }
    }

    public SearchUiManager getSearchUiManager() {
        return this.mSearchUiManager;
    }

    @Override // android.view.ViewGroup, android.view.View
    public boolean dispatchKeyEvent(KeyEvent keyEvent) {
        if (this.mSearchUiManager != null) {
            this.mSearchUiManager.preDispatchKeyEvent(keyEvent);
        }
        return super.dispatchKeyEvent(keyEvent);
    }

    @Override // com.android.launcher3.DragSource
    public void onDropCompleted(View view, DropTarget.DragObject dragObject, boolean z) {
    }

    @Override // com.android.launcher3.logging.UserEventDispatcher.LogContainerProvider
    public void fillInLogContainerData(View view, ItemInfo itemInfo, LauncherLogProto.Target target, LauncherLogProto.Target target2) {
    }

    @Override // com.android.launcher3.Insettable
    public void setInsets(Rect rect) {
        DeviceProfile deviceProfile = this.mLauncher.getDeviceProfile();
        int i = deviceProfile.desiredWorkspaceLeftRightMarginPx + deviceProfile.cellLayoutPaddingLeftRightPx;
        for (int i2 = 0; i2 < this.mAH.length; i2++) {
            this.mAH[i2].padding.bottom = rect.bottom;
            Rect rect2 = this.mAH[i2].padding;
            this.mAH[i2].padding.right = i;
            rect2.left = i;
            this.mAH[i2].applyPadding();
        }
        ViewGroup.MarginLayoutParams marginLayoutParams = (ViewGroup.MarginLayoutParams) getLayoutParams();
        if (deviceProfile.isVerticalBarLayout()) {
            marginLayoutParams.leftMargin = rect.left;
            marginLayoutParams.rightMargin = rect.right;
            setPadding(deviceProfile.workspacePadding.left, 0, deviceProfile.workspacePadding.right, 0);
        } else {
            marginLayoutParams.rightMargin = 0;
            marginLayoutParams.leftMargin = 0;
            setPadding(0, 0, 0, 0);
        }
        setLayoutParams(marginLayoutParams);
        this.mNavBarScrimHeight = rect.bottom;
        InsettableFrameLayout.dispatchInsets(this, rect);
    }

    @Override // android.view.ViewGroup, android.view.View
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        if (this.mNavBarScrimHeight > 0) {
            canvas.drawRect(0.0f, getHeight() - this.mNavBarScrimHeight, getWidth(), getHeight(), this.mNavBarScrimPaint);
        }
    }

    private void rebindAdapters(boolean z) {
        rebindAdapters(z, false);
    }

    private void rebindAdapters(boolean z, boolean z2) {
        if (z == this.mUsingTabs && !z2) {
            return;
        }
        replaceRVContainer(z);
        this.mUsingTabs = z;
        this.mAllAppsStore.unregisterIconContainer(this.mAH[0].recyclerView);
        this.mAllAppsStore.unregisterIconContainer(this.mAH[1].recyclerView);
        if (this.mUsingTabs) {
            this.mAH[0].setup(this.mViewPager.getChildAt(0), this.mPersonalMatcher);
            this.mAH[1].setup(this.mViewPager.getChildAt(1), this.mWorkMatcher);
            onTabChanged(this.mViewPager.getNextPage());
        } else {
            this.mAH[0].setup(findViewById(R.id.apps_list_view), null);
            this.mAH[1].recyclerView = null;
        }
        setupHeader();
        this.mAllAppsStore.registerIconContainer(this.mAH[0].recyclerView);
        this.mAllAppsStore.registerIconContainer(this.mAH[1].recyclerView);
    }

    private void replaceRVContainer(boolean z) {
        for (int i = 0; i < this.mAH.length; i++) {
            if (this.mAH[i].recyclerView != null) {
                this.mAH[i].recyclerView.setLayoutManager(null);
            }
        }
        View recyclerViewContainer = getRecyclerViewContainer();
        int iIndexOfChild = indexOfChild(recyclerViewContainer);
        removeView(recyclerViewContainer);
        View viewInflate = LayoutInflater.from(getContext()).inflate(z ? R.layout.all_apps_tabs : R.layout.all_apps_rv_layout, (ViewGroup) this, false);
        addView(viewInflate, iIndexOfChild);
        if (z) {
            this.mViewPager = (AllAppsPagedView) viewInflate;
            this.mViewPager.initParentViews(this);
            this.mViewPager.getPageIndicator().setContainerView(this);
            return;
        }
        this.mViewPager = null;
    }

    public View getRecyclerViewContainer() {
        return this.mViewPager != null ? this.mViewPager : findViewById(R.id.apps_list_view);
    }

    public void onTabChanged(int i) {
        this.mHeader.setMainActive(i == 0);
        reset(true);
        if (this.mAH[i].recyclerView != null) {
            this.mAH[i].recyclerView.bindFastScrollbar();
            findViewById(R.id.tab_personal).setOnClickListener(new View.OnClickListener() { // from class: com.android.launcher3.allapps.-$$Lambda$AllAppsContainerView$B39Kl1fKfqwhUd1leVXWHjoqw68
                @Override // android.view.View.OnClickListener
                public final void onClick(View view) {
                    this.f$0.mViewPager.snapToPage(0);
                }
            });
            findViewById(R.id.tab_work).setOnClickListener(new View.OnClickListener() { // from class: com.android.launcher3.allapps.-$$Lambda$AllAppsContainerView$nL3UFRjyGZF1mrhe66f7640Uuas
                @Override // android.view.View.OnClickListener
                public final void onClick(View view) {
                    this.f$0.mViewPager.snapToPage(1);
                }
            });
        }
        if (i == 1) {
            BottomUserEducationView.showIfNeeded(this.mLauncher);
        }
    }

    public AlphabeticalAppsList getApps() {
        return this.mAH[0].appsList;
    }

    public FloatingHeaderView getFloatingHeaderView() {
        return this.mHeader;
    }

    public View getSearchView() {
        return this.mSearchContainer;
    }

    public View getContentView() {
        return this.mViewPager == null ? getActiveRecyclerView() : this.mViewPager;
    }

    public RecyclerViewFastScroller getScrollBar() {
        AllAppsRecyclerView activeRecyclerView = getActiveRecyclerView();
        if (activeRecyclerView == null) {
            return null;
        }
        return activeRecyclerView.getScrollbar();
    }

    public void setupHeader() {
        this.mHeader.setVisibility(0);
        this.mHeader.setup(this.mAH, this.mAH[1].recyclerView == null);
        int maxTranslation = this.mHeader.getMaxTranslation();
        for (int i = 0; i < this.mAH.length; i++) {
            this.mAH[i].padding.top = maxTranslation;
            this.mAH[i].applyPadding();
        }
    }

    public void setLastSearchQuery(String str) {
        for (int i = 0; i < this.mAH.length; i++) {
            this.mAH[i].adapter.setLastSearchQuery(str);
        }
        if (this.mUsingTabs) {
            this.mSearchModeWhileUsingTabs = true;
            rebindAdapters(false);
        }
    }

    public void onClearSearchResult() {
        if (this.mSearchModeWhileUsingTabs) {
            rebindAdapters(true);
            this.mSearchModeWhileUsingTabs = false;
        }
    }

    public void onSearchResultsChanged() {
        for (int i = 0; i < this.mAH.length; i++) {
            if (this.mAH[i].recyclerView != null) {
                this.mAH[i].recyclerView.onSearchResultsChanged();
            }
        }
    }

    public void setRecyclerViewVerticalFadingEdgeEnabled(boolean z) {
        for (int i = 0; i < this.mAH.length; i++) {
            this.mAH[i].applyVerticalFadingEdgeEnabled(z);
        }
    }

    public void addElevationController(RecyclerView.OnScrollListener onScrollListener) {
        if (!this.mUsingTabs) {
            this.mAH[0].recyclerView.addOnScrollListener(onScrollListener);
        }
    }

    public boolean isHeaderVisible() {
        return this.mHeader != null && this.mHeader.getVisibility() == 0;
    }

    public void onScrollUpEnd() {
        if (this.mUsingTabs) {
            ((PersonalWorkSlidingTabStrip) findViewById(R.id.tabs)).highlightWorkTabIfNecessary();
        }
    }

    /* renamed from: com.android.launcher3.allapps.AllAppsContainerView$1 */
    class AnonymousClass1 implements ValueAnimator.AnimatorUpdateListener {
        boolean shouldSpring = true;
        final /* synthetic */ float val$velocity;

        AnonymousClass1(float f) {
            f = f;
        }

        @Override // android.animation.ValueAnimator.AnimatorUpdateListener
        public void onAnimationUpdate(ValueAnimator valueAnimator) {
            if (!this.shouldSpring || valueAnimator.getAnimatedFraction() < AllAppsContainerView.FLING_ANIMATION_THRESHOLD || AllAppsContainerView.this.getSearchView() == null) {
                return;
            }
            int id = AllAppsContainerView.this.getSearchView().getId();
            AllAppsContainerView.this.addSpringView(id);
            AllAppsContainerView.this.finishWithShiftAndVelocity(1.0f, f * AllAppsContainerView.FLING_VELOCITY_MULTIPLIER, new DynamicAnimation.OnAnimationEndListener() { // from class: com.android.launcher3.allapps.AllAppsContainerView.1.1
                final /* synthetic */ int val$searchViewId;

                C00071(int id2) {
                    i = id2;
                }

                @Override // android.support.animation.DynamicAnimation.OnAnimationEndListener
                public void onAnimationEnd(DynamicAnimation dynamicAnimation, boolean z, float f, float f2) {
                    AllAppsContainerView.this.removeSpringView(i);
                }
            });
            this.shouldSpring = false;
        }

        /* renamed from: com.android.launcher3.allapps.AllAppsContainerView$1$1 */
        class C00071 implements DynamicAnimation.OnAnimationEndListener {
            final /* synthetic */ int val$searchViewId;

            C00071(int id2) {
                i = id2;
            }

            @Override // android.support.animation.DynamicAnimation.OnAnimationEndListener
            public void onAnimationEnd(DynamicAnimation dynamicAnimation, boolean z, float f, float f2) {
                AllAppsContainerView.this.removeSpringView(i);
            }
        }
    }

    public void addSpringFromFlingUpdateListener(ValueAnimator valueAnimator, float f) {
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: com.android.launcher3.allapps.AllAppsContainerView.1
            boolean shouldSpring = true;
            final /* synthetic */ float val$velocity;

            AnonymousClass1(float f2) {
                f = f2;
            }

            @Override // android.animation.ValueAnimator.AnimatorUpdateListener
            public void onAnimationUpdate(ValueAnimator valueAnimator2) {
                if (!this.shouldSpring || valueAnimator2.getAnimatedFraction() < AllAppsContainerView.FLING_ANIMATION_THRESHOLD || AllAppsContainerView.this.getSearchView() == null) {
                    return;
                }
                int id2 = AllAppsContainerView.this.getSearchView().getId();
                AllAppsContainerView.this.addSpringView(id2);
                AllAppsContainerView.this.finishWithShiftAndVelocity(1.0f, f * AllAppsContainerView.FLING_VELOCITY_MULTIPLIER, new DynamicAnimation.OnAnimationEndListener() { // from class: com.android.launcher3.allapps.AllAppsContainerView.1.1
                    final /* synthetic */ int val$searchViewId;

                    C00071(int id22) {
                        i = id22;
                    }

                    @Override // android.support.animation.DynamicAnimation.OnAnimationEndListener
                    public void onAnimationEnd(DynamicAnimation dynamicAnimation, boolean z, float f2, float f22) {
                        AllAppsContainerView.this.removeSpringView(i);
                    }
                });
                this.shouldSpring = false;
            }

            /* renamed from: com.android.launcher3.allapps.AllAppsContainerView$1$1 */
            class C00071 implements DynamicAnimation.OnAnimationEndListener {
                final /* synthetic */ int val$searchViewId;

                C00071(int id22) {
                    i = id22;
                }

                @Override // android.support.animation.DynamicAnimation.OnAnimationEndListener
                public void onAnimationEnd(DynamicAnimation dynamicAnimation, boolean z, float f2, float f22) {
                    AllAppsContainerView.this.removeSpringView(i);
                }
            }
        });
    }

    public class AdapterHolder {
        public static final int MAIN = 0;
        public static final int WORK = 1;
        public final AllAppsGridAdapter adapter;
        final AlphabeticalAppsList appsList;
        final LinearLayoutManager layoutManager;
        final Rect padding = new Rect();
        AllAppsRecyclerView recyclerView;
        boolean verticalFadingEdge;

        AdapterHolder(boolean z) {
            this.appsList = new AlphabeticalAppsList(AllAppsContainerView.this.mLauncher, AllAppsContainerView.this.mAllAppsStore, z);
            this.adapter = new AllAppsGridAdapter(AllAppsContainerView.this.mLauncher, this.appsList);
            this.appsList.setAdapter(this.adapter);
            this.layoutManager = this.adapter.getLayoutManager();
        }

        void setup(@NonNull View view, @Nullable ItemInfoMatcher itemInfoMatcher) {
            this.appsList.updateItemFilter(itemInfoMatcher);
            this.recyclerView = (AllAppsRecyclerView) view;
            this.recyclerView.setEdgeEffectFactory(AllAppsContainerView.this.createEdgeEffectFactory());
            this.recyclerView.setApps(this.appsList, AllAppsContainerView.this.mUsingTabs);
            this.recyclerView.setLayoutManager(this.layoutManager);
            this.recyclerView.setAdapter(this.adapter);
            this.recyclerView.setHasFixedSize(true);
            this.recyclerView.setItemAnimator(null);
            FocusedItemDecorator focusedItemDecorator = new FocusedItemDecorator(this.recyclerView);
            this.recyclerView.addItemDecoration(focusedItemDecorator);
            this.adapter.setIconFocusListener(focusedItemDecorator.getFocusListener());
            applyVerticalFadingEdgeEnabled(this.verticalFadingEdge);
            applyPadding();
        }

        void applyPadding() {
            if (this.recyclerView != null) {
                this.recyclerView.setPadding(this.padding.left, this.padding.top, this.padding.right, this.padding.bottom);
            }
        }

        public void applyVerticalFadingEdgeEnabled(boolean z) {
            this.verticalFadingEdge = z;
            boolean z2 = false;
            AllAppsRecyclerView allAppsRecyclerView = AllAppsContainerView.this.mAH[0].recyclerView;
            if (!AllAppsContainerView.this.mUsingTabs && this.verticalFadingEdge) {
                z2 = true;
            }
            allAppsRecyclerView.setVerticalFadingEdgeEnabled(z2);
        }
    }
}
