package com.android.launcher3.allapps.search;

import android.content.Context;
import android.graphics.Rect;
import android.text.Selection;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.method.TextKeyListener;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import com.android.launcher3.DeviceProfile;
import com.android.launcher3.ExtendedEditText;
import com.android.launcher3.Insettable;
import com.android.launcher3.Launcher;
import com.android.launcher3.R;
import com.android.launcher3.allapps.AllAppsContainerView;
import com.android.launcher3.allapps.AllAppsStore;
import com.android.launcher3.allapps.AlphabeticalAppsList;
import com.android.launcher3.allapps.SearchUiManager;
import com.android.launcher3.allapps.search.AllAppsSearchBarController;
import com.android.launcher3.graphics.TintedDrawableSpan;
import com.android.launcher3.util.ComponentKey;
import java.util.ArrayList;

/* loaded from: classes.dex */
public class AppsSearchContainerLayout extends ExtendedEditText implements SearchUiManager, AllAppsSearchBarController.Callbacks, AllAppsStore.OnUpdateListener, Insettable {
    private AlphabeticalAppsList mApps;
    private AllAppsContainerView mAppsView;
    private final float mFixedTranslationY;
    private final Launcher mLauncher;
    private final float mMarginTopAdjusting;
    private final AllAppsSearchBarController mSearchBarController;
    private final SpannableStringBuilder mSearchQueryBuilder;

    public AppsSearchContainerLayout(Context context) {
        this(context, null);
    }

    public AppsSearchContainerLayout(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public AppsSearchContainerLayout(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mLauncher = Launcher.getLauncher(context);
        this.mSearchBarController = new AllAppsSearchBarController();
        this.mSearchQueryBuilder = new SpannableStringBuilder();
        Selection.setSelection(this.mSearchQueryBuilder, 0);
        this.mFixedTranslationY = getTranslationY();
        this.mMarginTopAdjusting = this.mFixedTranslationY - getPaddingTop();
        SpannableString spannableString = new SpannableString("  " + ((Object) getHint()));
        spannableString.setSpan(new TintedDrawableSpan(getContext(), R.drawable.ic_allapps_search), 0, 1, 34);
        setHint(spannableString);
    }

    @Override // android.widget.TextView, android.view.View
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        this.mLauncher.getAppsView().getAppsStore().addUpdateListener(this);
    }

    @Override // android.view.View
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        this.mLauncher.getAppsView().getAppsStore().removeUpdateListener(this);
    }

    @Override // android.widget.TextView, android.view.View
    protected void onMeasure(int i, int i2) {
        DeviceProfile deviceProfile = this.mLauncher.getDeviceProfile();
        int size = (View.MeasureSpec.getSize(i) - this.mAppsView.getActiveRecyclerView().getPaddingLeft()) - this.mAppsView.getActiveRecyclerView().getPaddingRight();
        super.onMeasure(View.MeasureSpec.makeMeasureSpec((size - (DeviceProfile.calculateCellWidth(size, deviceProfile.inv.numHotseatIcons) - Math.round(0.92f * deviceProfile.iconSizePx))) + getPaddingLeft() + getPaddingRight(), 1073741824), i2);
    }

    @Override // com.android.launcher3.ExtendedEditText, android.widget.TextView, android.view.View
    protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
        super.onLayout(z, i, i2, i3, i4);
        View view = (View) getParent();
        setTranslationX((view.getPaddingLeft() + ((((view.getWidth() - view.getPaddingLeft()) - view.getPaddingRight()) - (i3 - i)) / 2)) - i);
    }

    @Override // com.android.launcher3.allapps.SearchUiManager
    public void initialize(AllAppsContainerView allAppsContainerView) {
        this.mApps = allAppsContainerView.getApps();
        this.mAppsView = allAppsContainerView;
        this.mSearchBarController.initialize(new DefaultAppSearchAlgorithm(this.mApps.getApps()), this, this.mLauncher, this);
    }

    @Override // com.android.launcher3.allapps.AllAppsStore.OnUpdateListener
    public void onAppsUpdated() {
        this.mSearchBarController.refreshSearchResult();
    }

    @Override // com.android.launcher3.allapps.SearchUiManager
    public void resetSearch() {
        this.mSearchBarController.reset();
    }

    @Override // com.android.launcher3.allapps.SearchUiManager
    public void preDispatchKeyEvent(KeyEvent keyEvent) {
        if (!this.mSearchBarController.isSearchFieldFocused() && keyEvent.getAction() == 0) {
            int unicodeChar = keyEvent.getUnicodeChar();
            if (((unicodeChar <= 0 || Character.isWhitespace(unicodeChar) || Character.isSpaceChar(unicodeChar)) ? false : true) && TextKeyListener.getInstance().onKeyDown(this, this.mSearchQueryBuilder, keyEvent.getKeyCode(), keyEvent) && this.mSearchQueryBuilder.length() > 0) {
                this.mSearchBarController.focusSearchField();
            }
        }
    }

    @Override // com.android.launcher3.allapps.search.AllAppsSearchBarController.Callbacks
    public void onSearchResult(String str, ArrayList<ComponentKey> arrayList) {
        if (arrayList != null) {
            this.mApps.setOrderedFilter(arrayList);
            notifyResultChanged();
            this.mAppsView.setLastSearchQuery(str);
        }
    }

    @Override // com.android.launcher3.allapps.search.AllAppsSearchBarController.Callbacks
    public void clearSearchResult() {
        if (this.mApps.setOrderedFilter(null)) {
            notifyResultChanged();
        }
        this.mSearchQueryBuilder.clear();
        this.mSearchQueryBuilder.clearSpans();
        Selection.setSelection(this.mSearchQueryBuilder, 0);
        this.mAppsView.onClearSearchResult();
    }

    private void notifyResultChanged() {
        this.mAppsView.onSearchResultsChanged();
    }

    @Override // com.android.launcher3.Insettable
    public void setInsets(Rect rect) {
        ((ViewGroup.MarginLayoutParams) getLayoutParams()).topMargin = Math.round(Math.max(-this.mFixedTranslationY, rect.top - this.mMarginTopAdjusting));
        requestLayout();
        if (this.mLauncher.getDeviceProfile().isVerticalBarLayout()) {
            this.mLauncher.getAllAppsController().setScrollRangeDelta(0.0f);
        } else {
            this.mLauncher.getAllAppsController().setScrollRangeDelta(rect.bottom + r0.topMargin + this.mFixedTranslationY);
        }
    }
}
