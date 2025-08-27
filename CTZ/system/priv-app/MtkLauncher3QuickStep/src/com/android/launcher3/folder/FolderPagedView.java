package com.android.launcher3.folder;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.ArrayMap;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewDebug;
import android.view.ViewGroup;
import com.android.launcher3.BubbleTextView;
import com.android.launcher3.CellLayout;
import com.android.launcher3.DeviceProfile;
import com.android.launcher3.ExtendedEditText;
import com.android.launcher3.InvariantDeviceProfile;
import com.android.launcher3.ItemInfo;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.PagedView;
import com.android.launcher3.R;
import com.android.launcher3.ShortcutAndWidgetContainer;
import com.android.launcher3.ShortcutInfo;
import com.android.launcher3.Utilities;
import com.android.launcher3.Workspace;
import com.android.launcher3.anim.Interpolators;
import com.android.launcher3.keyboard.ViewGroupFocusHelper;
import com.android.launcher3.pageindicators.PageIndicatorDots;
import com.android.launcher3.touch.ItemClickHandler;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

/* loaded from: classes.dex */
public class FolderPagedView extends PagedView<PageIndicatorDots> {
    private static final int REORDER_ANIMATION_DURATION = 230;
    private static final float SCROLL_HINT_FRACTION = 0.07f;
    private static final int START_VIEW_REORDER_DELAY = 30;
    private static final String TAG = "FolderPagedView";
    private static final float VIEW_REORDER_DELAY_FACTOR = 0.9f;
    private static final int[] sTmpArray = new int[2];
    private int mAllocatedContentSize;
    private final ViewGroupFocusHelper mFocusIndicatorHelper;
    private Folder mFolder;

    @ViewDebug.ExportedProperty(category = "launcher")
    private int mGridCountX;

    @ViewDebug.ExportedProperty(category = "launcher")
    private int mGridCountY;
    private final LayoutInflater mInflater;
    public final boolean mIsRtl;

    @ViewDebug.ExportedProperty(category = "launcher")
    private final int mMaxCountX;

    @ViewDebug.ExportedProperty(category = "launcher")
    private final int mMaxCountY;

    @ViewDebug.ExportedProperty(category = "launcher")
    private final int mMaxItemsPerPage;
    final ArrayMap<View, Runnable> mPendingAnimations;

    public FolderPagedView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mPendingAnimations = new ArrayMap<>();
        InvariantDeviceProfile idp = LauncherAppState.getIDP(context);
        this.mMaxCountX = idp.numFolderColumns;
        this.mMaxCountY = idp.numFolderRows;
        this.mMaxItemsPerPage = this.mMaxCountX * this.mMaxCountY;
        this.mInflater = LayoutInflater.from(context);
        this.mIsRtl = Utilities.isRtl(getResources());
        setImportantForAccessibility(1);
        this.mFocusIndicatorHelper = new ViewGroupFocusHelper(this);
    }

    /* JADX WARN: Type inference failed for: r0v1, types: [T extends android.view.View & com.android.launcher3.pageindicators.PageIndicator, android.view.View] */
    public void setFolder(Folder folder) {
        this.mFolder = folder;
        this.mPageIndicator = folder.findViewById(R.id.folder_page_indicator);
        initParentViews(folder);
    }

    /* JADX WARN: Removed duplicated region for block: B:18:0x002d  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public static void calculateGridSize(int i, int i2, int i3, int i4, int i5, int i6, int[] iArr) {
        int i7;
        int i8;
        boolean z;
        int iMax;
        int iMax2;
        if (i >= i6) {
            i8 = i4;
            i7 = i5;
            z = true;
        } else {
            i7 = i3;
            i8 = i2;
            z = false;
        }
        while (!z) {
            if (i8 * i7 < i) {
                if ((i8 <= i7 || i7 == i5) && i8 < i4) {
                    iMax = i8 + 1;
                } else if (i7 < i5) {
                    iMax2 = i7 + 1;
                    iMax = i8;
                    if (iMax2 == 0) {
                        iMax2++;
                    }
                } else {
                    iMax = i8;
                }
                iMax2 = i7;
                if (iMax2 == 0) {
                }
            } else {
                int i9 = i7 - 1;
                if (i9 * i8 >= i && i7 >= i8) {
                    iMax2 = Math.max(0, i9);
                    iMax = i8;
                } else {
                    int i10 = i8 - 1;
                    iMax = i10 * i7 >= i ? Math.max(0, i10) : i8;
                    iMax2 = i7;
                }
            }
            boolean z2 = iMax == i8 && iMax2 == i7;
            i7 = iMax2;
            z = z2;
            i8 = iMax;
        }
        iArr[0] = i8;
        iArr[1] = i7;
    }

    public void setupContentDimensions(int i) {
        this.mAllocatedContentSize = i;
        calculateGridSize(i, this.mGridCountX, this.mGridCountY, this.mMaxCountX, this.mMaxCountY, this.mMaxItemsPerPage, sTmpArray);
        this.mGridCountX = sTmpArray[0];
        this.mGridCountY = sTmpArray[1];
        for (int pageCount = getPageCount() - 1; pageCount >= 0; pageCount--) {
            getPageAt(pageCount).setGridSize(this.mGridCountX, this.mGridCountY);
        }
    }

    @Override // android.view.ViewGroup, android.view.View
    protected void dispatchDraw(Canvas canvas) {
        this.mFocusIndicatorHelper.draw(canvas);
        super.dispatchDraw(canvas);
    }

    public void bindItems(ArrayList<ShortcutInfo> arrayList) {
        ArrayList<View> arrayList2 = new ArrayList<>();
        Iterator<ShortcutInfo> it = arrayList.iterator();
        while (it.hasNext()) {
            arrayList2.add(createNewView(it.next()));
        }
        arrangeChildren(arrayList2, arrayList2.size(), false);
    }

    public void allocateSpaceForRank(int i) {
        ArrayList<View> arrayList = new ArrayList<>(this.mFolder.getItemsInReadingOrder());
        arrayList.add(i, null);
        arrangeChildren(arrayList, arrayList.size(), false);
    }

    public int allocateRankForNewItem() {
        int itemCount = getItemCount();
        allocateSpaceForRank(itemCount);
        setCurrentPage(itemCount / this.mMaxItemsPerPage);
        return itemCount;
    }

    public View createAndAddViewForRank(ShortcutInfo shortcutInfo, int i) {
        View viewCreateNewView = createNewView(shortcutInfo);
        allocateSpaceForRank(i);
        addViewForRank(viewCreateNewView, shortcutInfo, i);
        return viewCreateNewView;
    }

    public void addViewForRank(View view, ShortcutInfo shortcutInfo, int i) {
        int i2 = i % this.mMaxItemsPerPage;
        int i3 = i / this.mMaxItemsPerPage;
        shortcutInfo.rank = i;
        shortcutInfo.cellX = i2 % this.mGridCountX;
        shortcutInfo.cellY = i2 / this.mGridCountX;
        CellLayout.LayoutParams layoutParams = (CellLayout.LayoutParams) view.getLayoutParams();
        layoutParams.cellX = shortcutInfo.cellX;
        layoutParams.cellY = shortcutInfo.cellY;
        getPageAt(i3).addViewToCellLayout(view, -1, this.mFolder.mLauncher.getViewIdForItem(shortcutInfo), layoutParams, true);
    }

    @SuppressLint({"InflateParams"})
    public View createNewView(ShortcutInfo shortcutInfo) {
        BubbleTextView bubbleTextView = (BubbleTextView) this.mInflater.inflate(R.layout.folder_application, (ViewGroup) null, false);
        bubbleTextView.applyFromShortcutInfo(shortcutInfo);
        bubbleTextView.setHapticFeedbackEnabled(false);
        bubbleTextView.setOnClickListener(ItemClickHandler.INSTANCE);
        bubbleTextView.setOnLongClickListener(this.mFolder);
        bubbleTextView.setOnFocusChangeListener(this.mFocusIndicatorHelper);
        bubbleTextView.setLayoutParams(new CellLayout.LayoutParams(shortcutInfo.cellX, shortcutInfo.cellY, shortcutInfo.spanX, shortcutInfo.spanY));
        return bubbleTextView;
    }

    /* JADX DEBUG: Method merged with bridge method: getPageAt(I)Landroid/view/View; */
    @Override // com.android.launcher3.PagedView
    public CellLayout getPageAt(int i) {
        return (CellLayout) getChildAt(i);
    }

    public CellLayout getCurrentCellLayout() {
        return getPageAt(getNextPage());
    }

    private CellLayout createAndAddNewPage() {
        DeviceProfile deviceProfile = Launcher.getLauncher(getContext()).getDeviceProfile();
        CellLayout cellLayout = (CellLayout) this.mInflater.inflate(R.layout.folder_page, (ViewGroup) this, false);
        cellLayout.setCellDimensions(deviceProfile.folderCellWidthPx, deviceProfile.folderCellHeightPx);
        cellLayout.getShortcutsAndWidgets().setMotionEventSplittingEnabled(false);
        cellLayout.setInvertIfRtl(true);
        cellLayout.setGridSize(this.mGridCountX, this.mGridCountY);
        addView(cellLayout, -1, generateDefaultLayoutParams());
        return cellLayout;
    }

    @Override // com.android.launcher3.PagedView
    protected int getChildGap() {
        return getPaddingLeft() + getPaddingRight();
    }

    public void setFixedSize(int i, int i2) {
        int paddingLeft = i - (getPaddingLeft() + getPaddingRight());
        int paddingTop = i2 - (getPaddingTop() + getPaddingBottom());
        for (int childCount = getChildCount() - 1; childCount >= 0; childCount--) {
            ((CellLayout) getChildAt(childCount)).setFixedSize(paddingLeft, paddingTop);
        }
    }

    public void removeItem(View view) {
        for (int childCount = getChildCount() - 1; childCount >= 0; childCount--) {
            getPageAt(childCount).removeView(view);
        }
    }

    @Override // android.view.View
    protected void onScrollChanged(int i, int i2, int i3, int i4) {
        super.onScrollChanged(i, i2, i3, i4);
        ((PageIndicatorDots) this.mPageIndicator).setScroll(i, this.mMaxScrollX);
    }

    public void arrangeChildren(ArrayList<View> arrayList, int i) {
        arrangeChildren(arrayList, i, true);
    }

    /* JADX DEBUG: Type inference failed for r2v5. Raw type applied. Possible types: T extends android.view.View & com.android.launcher3.pageindicators.PageIndicator */
    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r1v22 */
    /* JADX WARN: Type inference failed for: r1v6, types: [int] */
    /* JADX WARN: Type inference failed for: r1v9 */
    /* JADX WARN: Type inference failed for: r2v6, types: [com.android.launcher3.pageindicators.PageIndicatorDots] */
    @SuppressLint({"RtlHardcoded"})
    private void arrangeChildren(ArrayList<View> arrayList, int i, boolean z) {
        boolean z2;
        int i2 = i;
        ArrayList arrayList2 = new ArrayList();
        for (int i3 = 0; i3 < getChildCount(); i3++) {
            CellLayout cellLayout = (CellLayout) getChildAt(i3);
            cellLayout.removeAllViews();
            arrayList2.add(cellLayout);
        }
        setupContentDimensions(i2);
        Iterator it = arrayList2.iterator();
        FolderIconPreviewVerifier folderIconPreviewVerifier = new FolderIconPreviewVerifier(Launcher.getLauncher(getContext()).getDeviceProfile().inv);
        int i4 = 0;
        CellLayout cellLayoutCreateAndAddNewPage = null;
        int i5 = 0;
        int i6 = 0;
        while (i4 < i2) {
            View view = arrayList.size() > i4 ? arrayList.get(i4) : null;
            if (cellLayoutCreateAndAddNewPage == null || i5 >= this.mMaxItemsPerPage) {
                if (it.hasNext()) {
                    cellLayoutCreateAndAddNewPage = (CellLayout) it.next();
                } else {
                    cellLayoutCreateAndAddNewPage = createAndAddNewPage();
                }
                i5 = 0;
            }
            if (view != null) {
                CellLayout.LayoutParams layoutParams = (CellLayout.LayoutParams) view.getLayoutParams();
                int i7 = i5 % this.mGridCountX;
                int i8 = i5 / this.mGridCountX;
                ItemInfo itemInfo = (ItemInfo) view.getTag();
                if (itemInfo.cellX != i7 || itemInfo.cellY != i8 || itemInfo.rank != i6) {
                    itemInfo.cellX = i7;
                    itemInfo.cellY = i8;
                    itemInfo.rank = i6;
                    if (z) {
                        this.mFolder.mLauncher.getModelWriter().addOrMoveItemInDatabase(itemInfo, this.mFolder.mInfo.id, 0L, itemInfo.cellX, itemInfo.cellY);
                    }
                }
                layoutParams.cellX = itemInfo.cellX;
                layoutParams.cellY = itemInfo.cellY;
                cellLayoutCreateAndAddNewPage.addViewToCellLayout(view, -1, this.mFolder.mLauncher.getViewIdForItem(itemInfo), layoutParams, true);
                if (folderIconPreviewVerifier.isItemInPreview(i6) && (view instanceof BubbleTextView)) {
                    ((BubbleTextView) view).verifyHighRes();
                }
            }
            i6++;
            i5++;
            i4++;
            i2 = i;
        }
        int i9 = 1;
        boolean z3 = false;
        while (it.hasNext()) {
            removeView((View) it.next());
            z3 = true;
        }
        if (z3) {
            z2 = false;
            setCurrentPage(0);
        } else {
            z2 = false;
        }
        setEnableOverscroll(getPageCount() > 1 ? true : z2);
        ?? r2 = (PageIndicatorDots) this.mPageIndicator;
        ?? r1 = z2;
        if (getPageCount() <= 1) {
            r1 = 8;
        }
        r2.setVisibility(r1);
        ExtendedEditText extendedEditText = this.mFolder.mFolderName;
        if (getPageCount() > 1) {
            i9 = this.mIsRtl ? 5 : 3;
        }
        extendedEditText.setGravity(i9);
    }

    public int getDesiredWidth() {
        if (getPageCount() <= 0) {
            return 0;
        }
        return getPaddingRight() + getPageAt(0).getDesiredWidth() + getPaddingLeft();
    }

    public int getDesiredHeight() {
        if (getPageCount() <= 0) {
            return 0;
        }
        return getPaddingBottom() + getPageAt(0).getDesiredHeight() + getPaddingTop();
    }

    public int getItemCount() {
        int childCount = getChildCount() - 1;
        if (childCount < 0) {
            return 0;
        }
        return getPageAt(childCount).getShortcutsAndWidgets().getChildCount() + (childCount * this.mMaxItemsPerPage);
    }

    public int findNearestArea(int i, int i2) {
        int nextPage = getNextPage();
        CellLayout pageAt = getPageAt(nextPage);
        pageAt.findNearestArea(i, i2, 1, 1, sTmpArray);
        if (this.mFolder.isLayoutRtl()) {
            sTmpArray[0] = (pageAt.getCountX() - sTmpArray[0]) - 1;
        }
        return Math.min(this.mAllocatedContentSize - 1, (nextPage * this.mMaxItemsPerPage) + (sTmpArray[1] * this.mGridCountX) + sTmpArray[0]);
    }

    public View getFirstItem() {
        if (getChildCount() < 1) {
            return null;
        }
        ShortcutAndWidgetContainer shortcutsAndWidgets = getCurrentCellLayout().getShortcutsAndWidgets();
        if (this.mGridCountX > 0) {
            return shortcutsAndWidgets.getChildAt(0, 0);
        }
        return shortcutsAndWidgets.getChildAt(0);
    }

    public View getLastItem() {
        if (getChildCount() < 1) {
            return null;
        }
        ShortcutAndWidgetContainer shortcutsAndWidgets = getCurrentCellLayout().getShortcutsAndWidgets();
        int childCount = shortcutsAndWidgets.getChildCount() - 1;
        if (this.mGridCountX > 0) {
            return shortcutsAndWidgets.getChildAt(childCount % this.mGridCountX, childCount / this.mGridCountX);
        }
        return shortcutsAndWidgets.getChildAt(childCount);
    }

    public View iterateOverItems(Workspace.ItemOperator itemOperator) {
        for (int i = 0; i < getChildCount(); i++) {
            CellLayout pageAt = getPageAt(i);
            for (int i2 = 0; i2 < pageAt.getCountY(); i2++) {
                for (int i3 = 0; i3 < pageAt.getCountX(); i3++) {
                    View childAt = pageAt.getChildAt(i3, i2);
                    if (childAt != null && itemOperator.evaluate((ItemInfo) childAt.getTag(), childAt)) {
                        return childAt;
                    }
                }
            }
        }
        return null;
    }

    public String getAccessibilityDescription() {
        return getContext().getString(R.string.folder_opened, Integer.valueOf(this.mGridCountX), Integer.valueOf(this.mGridCountY));
    }

    public void setFocusOnFirstChild() {
        View childAt = getCurrentCellLayout().getChildAt(0, 0);
        if (childAt != null) {
            childAt.requestFocus();
        }
    }

    @Override // com.android.launcher3.PagedView
    protected void notifyPageSwitchListener(int i) {
        super.notifyPageSwitchListener(i);
        if (this.mFolder != null) {
            this.mFolder.updateTextViewFocus();
        }
    }

    public void showScrollHint(int i) {
        int scrollForPage = (getScrollForPage(getNextPage()) + ((int) (((i == 0) ^ this.mIsRtl ? -0.07f : SCROLL_HINT_FRACTION) * getWidth()))) - getScrollX();
        if (scrollForPage != 0) {
            this.mScroller.setInterpolator(Interpolators.DEACCEL);
            this.mScroller.startScroll(getScrollX(), 0, scrollForPage, 0, 500);
            invalidate();
        }
    }

    public void clearScrollHint() {
        if (getScrollX() != getScrollForPage(getNextPage())) {
            snapToPage(getNextPage());
        }
    }

    public void completePendingPageChanges() {
        if (!this.mPendingAnimations.isEmpty()) {
            for (Map.Entry entry : new ArrayMap(this.mPendingAnimations).entrySet()) {
                ((View) entry.getKey()).animate().cancel();
                ((Runnable) entry.getValue()).run();
            }
        }
    }

    public boolean rankOnCurrentPage(int i) {
        return i / this.mMaxItemsPerPage == getNextPage();
    }

    @Override // com.android.launcher3.PagedView
    protected void onPageBeginTransition() {
        super.onPageBeginTransition();
        verifyVisibleHighResIcons(getCurrentPage() - 1);
        verifyVisibleHighResIcons(getCurrentPage() + 1);
    }

    public void verifyVisibleHighResIcons(int i) {
        CellLayout pageAt = getPageAt(i);
        if (pageAt != null) {
            ShortcutAndWidgetContainer shortcutsAndWidgets = pageAt.getShortcutsAndWidgets();
            for (int childCount = shortcutsAndWidgets.getChildCount() - 1; childCount >= 0; childCount--) {
                BubbleTextView bubbleTextView = (BubbleTextView) shortcutsAndWidgets.getChildAt(childCount);
                bubbleTextView.verifyHighRes();
                Drawable drawable = bubbleTextView.getCompoundDrawables()[1];
                if (drawable != null) {
                    drawable.setCallback(bubbleTextView);
                }
            }
        }
    }

    public int getAllocatedContentSize() {
        return this.mAllocatedContentSize;
    }

    public void realTimeReorder(int i, int i2) {
        int i3;
        int i4;
        final int i5 = i;
        completePendingPageChanges();
        int nextPage = getNextPage();
        int i6 = i2 / this.mMaxItemsPerPage;
        int i7 = i2 % this.mMaxItemsPerPage;
        if (i6 != nextPage) {
            Log.e(TAG, "Cannot animate when the target cell is invisible");
        }
        int i8 = i5 % this.mMaxItemsPerPage;
        int i9 = i5 / this.mMaxItemsPerPage;
        if (i2 == i5) {
            return;
        }
        int i10 = -1;
        int i11 = 0;
        if (i2 > i5) {
            if (i9 < nextPage) {
                i10 = nextPage * this.mMaxItemsPerPage;
                i8 = 0;
            } else {
                i5 = -1;
            }
            i4 = 1;
        } else {
            if (i9 > nextPage) {
                i3 = ((nextPage + 1) * this.mMaxItemsPerPage) - 1;
                i8 = this.mMaxItemsPerPage - 1;
            } else {
                i5 = -1;
                i3 = -1;
            }
            i10 = i3;
            i4 = -1;
        }
        while (i5 != i10) {
            int i12 = i5 + i4;
            int i13 = i12 / this.mMaxItemsPerPage;
            int i14 = i12 % this.mMaxItemsPerPage;
            int i15 = i14 % this.mGridCountX;
            int i16 = i14 / this.mGridCountX;
            CellLayout pageAt = getPageAt(i13);
            final View childAt = pageAt.getChildAt(i15, i16);
            if (childAt != null) {
                if (nextPage != i13) {
                    pageAt.removeView(childAt);
                    addViewForRank(childAt, (ShortcutInfo) childAt.getTag(), i5);
                } else {
                    final float translationX = childAt.getTranslationX();
                    Runnable runnable = new Runnable() { // from class: com.android.launcher3.folder.FolderPagedView.1
                        @Override // java.lang.Runnable
                        public void run() {
                            FolderPagedView.this.mPendingAnimations.remove(childAt);
                            childAt.setTranslationX(translationX);
                            ((CellLayout) childAt.getParent().getParent()).removeView(childAt);
                            FolderPagedView.this.addViewForRank(childAt, (ShortcutInfo) childAt.getTag(), i5);
                        }
                    };
                    childAt.animate().translationXBy((i4 > 0) ^ this.mIsRtl ? -childAt.getWidth() : childAt.getWidth()).setDuration(230L).setStartDelay(0L).withEndAction(runnable);
                    this.mPendingAnimations.put(childAt, runnable);
                }
            }
            i5 = i12;
        }
        if ((i7 - i8) * i4 <= 0) {
            return;
        }
        CellLayout pageAt2 = getPageAt(nextPage);
        float f = 30.0f;
        while (i8 != i7) {
            int i17 = i8 + i4;
            View childAt2 = pageAt2.getChildAt(i17 % this.mGridCountX, i17 / this.mGridCountX);
            if (childAt2 != null) {
                ((ItemInfo) childAt2.getTag()).rank -= i4;
            }
            if (pageAt2.animateChildToPosition(childAt2, i8 % this.mGridCountX, i8 / this.mGridCountX, REORDER_ANIMATION_DURATION, i11, true, true)) {
                int i18 = (int) (i11 + f);
                f *= VIEW_REORDER_DELAY_FACTOR;
                i11 = i18;
            }
            i8 = i17;
        }
    }

    public int itemsPerPage() {
        return this.mMaxItemsPerPage;
    }
}
