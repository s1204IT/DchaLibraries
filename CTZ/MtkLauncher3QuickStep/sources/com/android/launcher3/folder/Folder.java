package com.android.launcher3.folder;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Rect;
import android.text.Selection;
import android.util.AttributeSet;
import android.util.Pair;
import android.view.ActionMode;
import android.view.FocusFinder;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewDebug;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import com.android.launcher3.AbstractFloatingView;
import com.android.launcher3.Alarm;
import com.android.launcher3.AppInfo;
import com.android.launcher3.BubbleTextView;
import com.android.launcher3.CellLayout;
import com.android.launcher3.DeviceProfile;
import com.android.launcher3.DragSource;
import com.android.launcher3.DropTarget;
import com.android.launcher3.ExtendedEditText;
import com.android.launcher3.FolderInfo;
import com.android.launcher3.ItemInfo;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherState;
import com.android.launcher3.OnAlarmListener;
import com.android.launcher3.R;
import com.android.launcher3.ShortcutInfo;
import com.android.launcher3.Utilities;
import com.android.launcher3.Workspace;
import com.android.launcher3.accessibility.AccessibleDragListenerAdapter;
import com.android.launcher3.compat.AccessibilityManagerCompat;
import com.android.launcher3.dragndrop.DragController;
import com.android.launcher3.dragndrop.DragLayer;
import com.android.launcher3.dragndrop.DragOptions;
import com.android.launcher3.logging.LoggerUtils;
import com.android.launcher3.pageindicators.PageIndicatorDots;
import com.android.launcher3.userevent.nano.LauncherLogProto;
import com.android.launcher3.views.BaseDragLayer;
import com.android.launcher3.widget.PendingAddShortcutInfo;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class Folder extends AbstractFloatingView implements DragSource, View.OnLongClickListener, DropTarget, FolderInfo.FolderListener, TextView.OnEditorActionListener, View.OnFocusChangeListener, DragController.DragListener, ExtendedEditText.OnBackKeyListener {
    private static final int FOLDER_NAME_ANIMATION_DURATION = 633;
    private static final float ICON_OVERSCROLL_WIDTH_FACTOR = 0.45f;
    private static final int MIN_CONTENT_DIMEN = 5;
    private static final int ON_EXIT_CLOSE_DELAY = 400;
    private static final int REORDER_DELAY = 250;
    public static final int RESCROLL_DELAY = 900;
    public static final int SCROLL_HINT_DURATION = 500;
    public static final int SCROLL_LEFT = 0;
    public static final int SCROLL_NONE = -1;
    public static final int SCROLL_RIGHT = 1;
    static final int STATE_ANIMATING = 1;
    static final int STATE_NONE = -1;
    static final int STATE_OPEN = 2;
    static final int STATE_SMALL = 0;
    private static final String TAG = "Launcher.Folder";
    private static String sDefaultFolderName;
    private static String sHintText;
    FolderPagedView mContent;
    private AnimatorSet mCurrentAnimator;
    private View mCurrentDragView;
    int mCurrentScrollDir;
    private boolean mDeleteFolderOnDropCompleted;

    @ViewDebug.ExportedProperty(category = "launcher")
    private boolean mDestroyed;
    protected DragController mDragController;
    private boolean mDragInProgress;
    int mEmptyCellRank;
    FolderIcon mFolderIcon;
    float mFolderIconPivotX;
    float mFolderIconPivotY;
    public ExtendedEditText mFolderName;
    private View mFooter;
    private int mFooterHeight;
    public FolderInfo mInfo;
    private boolean mIsEditingName;
    private boolean mIsExternalDrag;
    private boolean mItemAddedBackToSelfViaIcon;
    final ArrayList<View> mItemsInReadingOrder;
    boolean mItemsInvalidated;
    protected final Launcher mLauncher;
    private final Alarm mOnExitAlarm;
    OnAlarmListener mOnExitAlarmListener;
    private final Alarm mOnScrollHintAlarm;
    private PageIndicatorDots mPageIndicator;
    int mPrevTargetRank;

    @ViewDebug.ExportedProperty(category = "launcher")
    private boolean mRearrangeOnClose;
    private final Alarm mReorderAlarm;
    OnAlarmListener mReorderAlarmListener;
    private int mScrollAreaOffset;
    int mScrollHintDir;
    final Alarm mScrollPauseAlarm;

    @ViewDebug.ExportedProperty(category = "launcher", mapping = {@ViewDebug.IntToString(from = -1, to = "STATE_NONE"), @ViewDebug.IntToString(from = 0, to = "STATE_SMALL"), @ViewDebug.IntToString(from = 1, to = "STATE_ANIMATING"), @ViewDebug.IntToString(from = 2, to = "STATE_OPEN")})
    int mState;
    private boolean mSuppressFolderDeletion;
    int mTargetRank;
    private static final Rect sTempRect = new Rect();
    public static final Comparator<ItemInfo> ITEM_POS_COMPARATOR = new Comparator<ItemInfo>() {
        @Override
        public int compare(ItemInfo itemInfo, ItemInfo itemInfo2) {
            if (itemInfo.rank != itemInfo2.rank) {
                return itemInfo.rank - itemInfo2.rank;
            }
            if (itemInfo.cellY != itemInfo2.cellY) {
                return itemInfo.cellY - itemInfo2.cellY;
            }
            return itemInfo.cellX - itemInfo2.cellX;
        }
    };

    public Folder(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mReorderAlarm = new Alarm();
        this.mOnExitAlarm = new Alarm();
        this.mOnScrollHintAlarm = new Alarm();
        this.mScrollPauseAlarm = new Alarm();
        this.mItemsInReadingOrder = new ArrayList<>();
        this.mState = -1;
        this.mRearrangeOnClose = false;
        this.mItemsInvalidated = false;
        this.mDragInProgress = false;
        this.mDeleteFolderOnDropCompleted = false;
        this.mSuppressFolderDeletion = false;
        this.mItemAddedBackToSelfViaIcon = false;
        this.mIsEditingName = false;
        this.mScrollHintDir = -1;
        this.mCurrentScrollDir = -1;
        this.mReorderAlarmListener = new OnAlarmListener() {
            @Override
            public void onAlarm(Alarm alarm) {
                Folder.this.mContent.realTimeReorder(Folder.this.mEmptyCellRank, Folder.this.mTargetRank);
                Folder.this.mEmptyCellRank = Folder.this.mTargetRank;
            }
        };
        this.mOnExitAlarmListener = new OnAlarmListener() {
            @Override
            public void onAlarm(Alarm alarm) {
                Folder.this.completeDragExit();
            }
        };
        setAlwaysDrawnWithCacheEnabled(false);
        Resources resources = getResources();
        if (sDefaultFolderName == null) {
            sDefaultFolderName = resources.getString(R.string.folder_name);
        }
        if (sHintText == null) {
            sHintText = resources.getString(R.string.folder_hint_text);
        }
        this.mLauncher = Launcher.getLauncher(context);
        setFocusableInTouchMode(true);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mContent = (FolderPagedView) findViewById(R.id.folder_content);
        this.mContent.setFolder(this);
        this.mPageIndicator = (PageIndicatorDots) findViewById(R.id.folder_page_indicator);
        this.mFolderName = (ExtendedEditText) findViewById(R.id.folder_name);
        this.mFolderName.setOnBackKeyListener(this);
        this.mFolderName.setOnFocusChangeListener(this);
        if (!Utilities.ATLEAST_MARSHMALLOW) {
            this.mFolderName.setCustomSelectionActionModeCallback(new ActionMode.Callback() {
                @Override
                public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
                    return false;
                }

                @Override
                public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
                    return false;
                }

                @Override
                public void onDestroyActionMode(ActionMode actionMode) {
                }

                @Override
                public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
                    return false;
                }
            });
        }
        this.mFolderName.setOnEditorActionListener(this);
        this.mFolderName.setSelectAllOnFocus(true);
        this.mFolderName.setInputType((this.mFolderName.getInputType() & (-32769) & (-524289)) | 8192);
        this.mFolderName.forceDisableSuggestions(true);
        this.mFooter = findViewById(R.id.folder_footer);
        this.mFooter.measure(0, 0);
        this.mFooterHeight = this.mFooter.getMeasuredHeight();
    }

    @Override
    public boolean onLongClick(View view) {
        if (this.mLauncher.isDraggingEnabled()) {
            return startDrag(view, new DragOptions());
        }
        return true;
    }

    public boolean startDrag(View view, DragOptions dragOptions) {
        Object tag = view.getTag();
        int i = 1;
        if (tag instanceof ShortcutInfo) {
            this.mEmptyCellRank = ((ShortcutInfo) tag).rank;
            this.mCurrentDragView = view;
            this.mDragController.addDragListener(this);
            if (dragOptions.isAccessibleDrag) {
                this.mDragController.addDragListener(new AccessibleDragListenerAdapter(this.mContent, i) {
                    @Override
                    protected void enableAccessibleDrag(boolean z) {
                        int i2;
                        super.enableAccessibleDrag(z);
                        View view2 = Folder.this.mFooter;
                        if (z) {
                            i2 = 4;
                        } else {
                            i2 = 0;
                        }
                        view2.setImportantForAccessibility(i2);
                    }
                });
            }
            this.mLauncher.getWorkspace().beginDragShared(view, this, dragOptions);
        }
        return true;
    }

    @Override
    public void onDragStart(DropTarget.DragObject dragObject, DragOptions dragOptions) throws Exception {
        if (dragObject.dragSource != this) {
            return;
        }
        this.mContent.removeItem(this.mCurrentDragView);
        if (dragObject.dragInfo instanceof ShortcutInfo) {
            this.mItemsInvalidated = true;
            SuppressInfoChanges suppressInfoChanges = new SuppressInfoChanges();
            Throwable th = null;
            try {
                try {
                    this.mInfo.remove((ShortcutInfo) dragObject.dragInfo, true);
                } finally {
                }
            } finally {
                $closeResource(th, suppressInfoChanges);
            }
        }
        this.mDragInProgress = true;
        this.mItemAddedBackToSelfViaIcon = false;
    }

    private static void $closeResource(Throwable th, AutoCloseable autoCloseable) throws Exception {
        if (th == null) {
            autoCloseable.close();
            return;
        }
        try {
            autoCloseable.close();
        } catch (Throwable th2) {
            th.addSuppressed(th2);
        }
    }

    @Override
    public void onDragEnd() {
        if (this.mIsExternalDrag && this.mDragInProgress) {
            completeDragExit();
        }
        this.mDragInProgress = false;
        this.mDragController.removeDragListener(this);
    }

    public boolean isEditingName() {
        return this.mIsEditingName;
    }

    public void startEditingFolderName() {
        post(new Runnable() {
            @Override
            public void run() {
                Folder.this.mFolderName.setHint("");
                Folder.this.mIsEditingName = true;
            }
        });
    }

    @Override
    public boolean onBackKey() {
        String string = this.mFolderName.getText().toString();
        this.mInfo.setTitle(string);
        this.mLauncher.getModelWriter().updateItemInDatabase(this.mInfo);
        this.mFolderName.setHint(sDefaultFolderName.contentEquals(string) ? sHintText : null);
        AccessibilityManagerCompat.sendCustomAccessibilityEvent(this, 32, getContext().getString(R.string.folder_renamed, string));
        this.mFolderName.clearFocus();
        Selection.setSelection(this.mFolderName.getText(), 0, 0);
        this.mIsEditingName = false;
        return true;
    }

    @Override
    public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
        if (i == 6) {
            this.mFolderName.dispatchBackKey();
            return true;
        }
        return false;
    }

    public FolderIcon getFolderIcon() {
        return this.mFolderIcon;
    }

    public void setDragController(DragController dragController) {
        this.mDragController = dragController;
    }

    public void setFolderIcon(FolderIcon folderIcon) {
        this.mFolderIcon = folderIcon;
    }

    @Override
    protected void onAttachedToWindow() {
        requestFocus();
        super.onAttachedToWindow();
    }

    @Override
    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent accessibilityEvent) {
        return true;
    }

    @Override
    public View focusSearch(int i) {
        return FocusFinder.getInstance().findNextFocus(this, null, i);
    }

    public FolderInfo getInfo() {
        return this.mInfo;
    }

    void bind(FolderInfo folderInfo) {
        this.mInfo = folderInfo;
        ArrayList<ShortcutInfo> arrayList = folderInfo.contents;
        Collections.sort(arrayList, ITEM_POS_COMPARATOR);
        this.mContent.bindItems(arrayList);
        if (((BaseDragLayer.LayoutParams) getLayoutParams()) == null) {
            BaseDragLayer.LayoutParams layoutParams = new BaseDragLayer.LayoutParams(0, 0);
            layoutParams.customPosition = true;
            setLayoutParams(layoutParams);
        }
        centerAboutIcon();
        this.mItemsInvalidated = true;
        updateTextViewFocus();
        this.mInfo.addListener(this);
        if (!sDefaultFolderName.contentEquals(this.mInfo.title)) {
            this.mFolderName.setText(this.mInfo.title);
            this.mFolderName.setHint((CharSequence) null);
        } else {
            this.mFolderName.setText("");
            this.mFolderName.setHint(sHintText);
        }
        this.mFolderIcon.post(new Runnable() {
            @Override
            public void run() {
                if (Folder.this.getItemCount() <= 1) {
                    Folder.this.replaceFolderWithFinalItem();
                }
            }
        });
    }

    @SuppressLint({"InflateParams"})
    static Folder fromXml(Launcher launcher) {
        return (Folder) launcher.getLayoutInflater().inflate(R.layout.user_folder_icon_normalized, (ViewGroup) null);
    }

    private void startAnimation(final AnimatorSet animatorSet) {
        if (this.mCurrentAnimator != null && this.mCurrentAnimator.isRunning()) {
            this.mCurrentAnimator.cancel();
        }
        animatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animator) {
                Folder.this.mState = 1;
                Folder.this.mCurrentAnimator = animatorSet;
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                Folder.this.mCurrentAnimator = null;
            }
        });
        animatorSet.start();
    }

    public void animateOpen() {
        Folder open = getOpen(this.mLauncher);
        if (open != null && open != this) {
            open.close(true);
        }
        this.mIsOpen = true;
        DragLayer dragLayer = this.mLauncher.getDragLayer();
        if (getParent() == null) {
            dragLayer.addView(this);
            this.mDragController.addDropTarget(this);
        }
        this.mContent.completePendingPageChanges();
        if (!this.mDragInProgress) {
            this.mContent.snapToPageImmediately(0);
        }
        this.mDeleteFolderOnDropCompleted = false;
        centerAboutIcon();
        AnimatorSet animator = new FolderAnimationManager(this, true).getAnimator();
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animator2) {
                Folder.this.mFolderIcon.setBackgroundVisible(false);
                Folder.this.mFolderIcon.drawLeaveBehindIfExists();
            }

            @Override
            public void onAnimationEnd(Animator animator2) {
                Folder.this.mState = 2;
                Folder.this.announceAccessibilityChanges();
                Folder.this.mLauncher.getUserEventDispatcher().resetElapsedContainerMillis("folder opened");
                Folder.this.mContent.setFocusOnFirstChild();
            }
        });
        if (this.mContent.getPageCount() > 1 && !this.mInfo.hasOption(4)) {
            float desiredWidth = (((this.mContent.getDesiredWidth() - this.mFooter.getPaddingLeft()) - this.mFooter.getPaddingRight()) - this.mFolderName.getPaint().measureText(this.mFolderName.getText().toString())) / 2.0f;
            ExtendedEditText extendedEditText = this.mFolderName;
            if (this.mContent.mIsRtl) {
                desiredWidth = -desiredWidth;
            }
            extendedEditText.setTranslationX(desiredWidth);
            this.mPageIndicator.prepareEntryAnimation();
            final boolean z = true ^ this.mDragInProgress;
            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                @SuppressLint({"InlinedApi"})
                public void onAnimationEnd(Animator animator2) {
                    Folder.this.mFolderName.animate().setDuration(633L).translationX(0.0f).setInterpolator(AnimationUtils.loadInterpolator(Folder.this.mLauncher, android.R.interpolator.fast_out_slow_in));
                    Folder.this.mPageIndicator.playEntryAnimation();
                    if (z) {
                        Folder.this.mInfo.setOption(4, true, Folder.this.mLauncher.getModelWriter());
                    }
                }
            });
        } else {
            this.mFolderName.setTranslationX(0.0f);
        }
        this.mPageIndicator.stopAllAnimations();
        startAnimation(animator);
        if (this.mDragController.isDragging()) {
            this.mDragController.forceTouchMove();
        }
        this.mContent.verifyVisibleHighResIcons(this.mContent.getNextPage());
    }

    public void beginExternalDrag() {
        this.mEmptyCellRank = this.mContent.allocateRankForNewItem();
        this.mIsExternalDrag = true;
        this.mDragInProgress = true;
        this.mDragController.addDragListener(this);
    }

    @Override
    protected boolean isOfType(int i) {
        return (i & 1) != 0;
    }

    @Override
    protected void handleClose(boolean z) {
        this.mIsOpen = false;
        if (isEditingName()) {
            this.mFolderName.dispatchBackKey();
        }
        if (this.mFolderIcon != null) {
            this.mFolderIcon.clearLeaveBehindIfExists();
        }
        if (z) {
            animateClosed();
        } else {
            closeComplete(false);
            post(new Runnable() {
                @Override
                public final void run() {
                    this.f$0.announceAccessibilityChanges();
                }
            });
        }
        this.mLauncher.getDragLayer().sendAccessibilityEvent(32);
    }

    private void animateClosed() {
        AnimatorSet animator = new FolderAnimationManager(this, false).getAnimator();
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animator2) {
                Folder.this.closeComplete(true);
                Folder.this.announceAccessibilityChanges();
            }
        });
        startAnimation(animator);
    }

    @Override
    protected Pair<View, String> getAccessibilityTarget() {
        return Pair.create(this.mContent, this.mIsOpen ? this.mContent.getAccessibilityDescription() : getContext().getString(R.string.folder_closed));
    }

    private void closeComplete(boolean z) {
        DragLayer dragLayer = (DragLayer) getParent();
        if (dragLayer != null) {
            dragLayer.removeView(this);
        }
        this.mDragController.removeDropTarget(this);
        clearFocus();
        if (this.mFolderIcon != null) {
            this.mFolderIcon.setVisibility(0);
            this.mFolderIcon.setBackgroundVisible(true);
            this.mFolderIcon.mFolderName.setTextVisibility(true);
            if (z) {
                this.mFolderIcon.mBackground.fadeInBackgroundShadow();
                this.mFolderIcon.mBackground.animateBackgroundStroke();
                this.mFolderIcon.onFolderClose(this.mContent.getCurrentPage());
                if (this.mFolderIcon.hasBadge()) {
                    this.mFolderIcon.createBadgeScaleAnimator(0.0f, 1.0f).start();
                }
                this.mFolderIcon.requestFocus();
            }
        }
        if (this.mRearrangeOnClose) {
            rearrangeChildren();
            this.mRearrangeOnClose = false;
        }
        if (getItemCount() <= 1) {
            if (!this.mDragInProgress && !this.mSuppressFolderDeletion) {
                replaceFolderWithFinalItem();
            } else if (this.mDragInProgress) {
                this.mDeleteFolderOnDropCompleted = true;
            }
        }
        this.mSuppressFolderDeletion = false;
        clearDragInfo();
        this.mState = 0;
        this.mContent.setCurrentPage(0);
    }

    @Override
    public boolean acceptDrop(DropTarget.DragObject dragObject) {
        int i = dragObject.dragInfo.itemType;
        return i == 0 || i == 1 || i == 6;
    }

    @Override
    public void onDragEnter(DropTarget.DragObject dragObject) {
        this.mPrevTargetRank = -1;
        this.mOnExitAlarm.cancelAlarm();
        this.mScrollAreaOffset = (dragObject.dragView.getDragRegionWidth() / 2) - dragObject.xOffset;
    }

    public boolean isLayoutRtl() {
        return getLayoutDirection() == 1;
    }

    private int getTargetRank(DropTarget.DragObject dragObject, float[] fArr) {
        float[] visualCenter = dragObject.getVisualCenter(fArr);
        return this.mContent.findNearestArea(((int) visualCenter[0]) - getPaddingLeft(), ((int) visualCenter[1]) - getPaddingTop());
    }

    @Override
    public void onDragOver(DropTarget.DragObject dragObject) {
        if (this.mScrollPauseAlarm.alarmPending()) {
            return;
        }
        float[] fArr = new float[2];
        this.mTargetRank = getTargetRank(dragObject, fArr);
        if (this.mTargetRank != this.mPrevTargetRank) {
            this.mReorderAlarm.cancelAlarm();
            this.mReorderAlarm.setOnAlarmListener(this.mReorderAlarmListener);
            this.mReorderAlarm.setAlarm(250L);
            this.mPrevTargetRank = this.mTargetRank;
            if (dragObject.stateAnnouncer != null) {
                dragObject.stateAnnouncer.announce(getContext().getString(R.string.move_to_position, Integer.valueOf(this.mTargetRank + 1)));
            }
        }
        float f = fArr[0];
        int nextPage = this.mContent.getNextPage();
        float cellWidth = this.mContent.getCurrentCellLayout().getCellWidth() * ICON_OVERSCROLL_WIDTH_FACTOR;
        boolean z = f < cellWidth;
        boolean z2 = f > ((float) getWidth()) - cellWidth;
        if (nextPage > 0 && (!this.mContent.mIsRtl ? z : z2)) {
            showScrollHint(0, dragObject);
            return;
        }
        if (nextPage < this.mContent.getPageCount() - 1 && (!this.mContent.mIsRtl ? z2 : z)) {
            showScrollHint(1, dragObject);
            return;
        }
        this.mOnScrollHintAlarm.cancelAlarm();
        if (this.mScrollHintDir != -1) {
            this.mContent.clearScrollHint();
            this.mScrollHintDir = -1;
        }
    }

    private void showScrollHint(int i, DropTarget.DragObject dragObject) {
        if (this.mScrollHintDir != i) {
            this.mContent.showScrollHint(i);
            this.mScrollHintDir = i;
        }
        if (!this.mOnScrollHintAlarm.alarmPending() || this.mCurrentScrollDir != i) {
            this.mCurrentScrollDir = i;
            this.mOnScrollHintAlarm.cancelAlarm();
            this.mOnScrollHintAlarm.setOnAlarmListener(new OnScrollHintListener(dragObject));
            this.mOnScrollHintAlarm.setAlarm(500L);
            this.mReorderAlarm.cancelAlarm();
            this.mTargetRank = this.mEmptyCellRank;
        }
    }

    public void completeDragExit() {
        if (this.mIsOpen) {
            close(true);
            this.mRearrangeOnClose = true;
        } else if (this.mState == 1) {
            this.mRearrangeOnClose = true;
        } else {
            rearrangeChildren();
            clearDragInfo();
        }
    }

    private void clearDragInfo() {
        this.mCurrentDragView = null;
        this.mIsExternalDrag = false;
    }

    @Override
    public void onDragExit(DropTarget.DragObject dragObject) {
        if (!dragObject.dragComplete) {
            this.mOnExitAlarm.setOnAlarmListener(this.mOnExitAlarmListener);
            this.mOnExitAlarm.setAlarm(400L);
        }
        this.mReorderAlarm.cancelAlarm();
        this.mOnScrollHintAlarm.cancelAlarm();
        this.mScrollPauseAlarm.cancelAlarm();
        if (this.mScrollHintDir != -1) {
            this.mContent.clearScrollHint();
            this.mScrollHintDir = -1;
        }
    }

    @Override
    public void prepareAccessibilityDrop() {
        if (this.mReorderAlarm.alarmPending()) {
            this.mReorderAlarm.cancelAlarm();
            this.mReorderAlarmListener.onAlarm(this.mReorderAlarm);
        }
    }

    @Override
    public void onDropCompleted(View view, DropTarget.DragObject dragObject, boolean z) throws Exception {
        Throwable th = null;
        if (z) {
            if (this.mDeleteFolderOnDropCompleted && !this.mItemAddedBackToSelfViaIcon && view != this) {
                replaceFolderWithFinalItem();
            }
        } else {
            ShortcutInfo shortcutInfo = (ShortcutInfo) dragObject.dragInfo;
            View viewCreateNewView = (this.mCurrentDragView == null || this.mCurrentDragView.getTag() != shortcutInfo) ? this.mContent.createNewView(shortcutInfo) : this.mCurrentDragView;
            ArrayList<View> itemsInReadingOrder = getItemsInReadingOrder();
            itemsInReadingOrder.add(shortcutInfo.rank, viewCreateNewView);
            this.mContent.arrangeChildren(itemsInReadingOrder, itemsInReadingOrder.size());
            this.mItemsInvalidated = true;
            SuppressInfoChanges suppressInfoChanges = new SuppressInfoChanges();
            try {
                try {
                    this.mFolderIcon.onDrop(dragObject, true);
                } catch (Throwable th2) {
                    th = th2;
                    throw th;
                }
            } finally {
                $closeResource(th, suppressInfoChanges);
            }
        }
        if (view != this && this.mOnExitAlarm.alarmPending()) {
            this.mOnExitAlarm.cancelAlarm();
            if (!z) {
                this.mSuppressFolderDeletion = true;
            }
            this.mScrollPauseAlarm.cancelAlarm();
            completeDragExit();
        }
        this.mDeleteFolderOnDropCompleted = false;
        this.mDragInProgress = false;
        this.mItemAddedBackToSelfViaIcon = false;
        this.mCurrentDragView = null;
        updateItemLocationsInDatabaseBatch();
        if (getItemCount() <= this.mContent.itemsPerPage()) {
            this.mInfo.setOption(4, false, this.mLauncher.getModelWriter());
        }
    }

    private void updateItemLocationsInDatabaseBatch() {
        ArrayList<View> itemsInReadingOrder = getItemsInReadingOrder();
        ArrayList<ItemInfo> arrayList = new ArrayList<>();
        for (int i = 0; i < itemsInReadingOrder.size(); i++) {
            ItemInfo itemInfo = (ItemInfo) itemsInReadingOrder.get(i).getTag();
            itemInfo.rank = i;
            arrayList.add(itemInfo);
        }
        this.mLauncher.getModelWriter().moveItemsInDatabase(arrayList, this.mInfo.id, 0);
    }

    public void notifyDrop() {
        if (this.mDragInProgress) {
            this.mItemAddedBackToSelfViaIcon = true;
        }
    }

    @Override
    public boolean isDropEnabled() {
        return this.mState != 1;
    }

    private void centerAboutIcon() {
        int iMax;
        DeviceProfile deviceProfile = this.mLauncher.getDeviceProfile();
        BaseDragLayer.LayoutParams layoutParams = (BaseDragLayer.LayoutParams) getLayoutParams();
        DragLayer dragLayer = (DragLayer) this.mLauncher.findViewById(R.id.drag_layer);
        int folderWidth = getFolderWidth();
        int folderHeight = getFolderHeight();
        dragLayer.getDescendantRectRelativeToSelf(this.mFolderIcon, sTempRect);
        int i = folderWidth / 2;
        int iCenterX = sTempRect.centerX() - i;
        int i2 = folderHeight / 2;
        int iCenterY = sTempRect.centerY() - i2;
        if (this.mLauncher.getStateManager().getState().overviewUi) {
            this.mLauncher.getDragLayer().getDescendantRectRelativeToSelf(this.mLauncher.getOverviewPanel(), sTempRect);
        } else {
            this.mLauncher.getWorkspace().getPageAreaRelativeToDragLayer(sTempRect);
        }
        int iMin = Math.min(Math.max(sTempRect.left, iCenterX), sTempRect.right - folderWidth);
        int iMin2 = Math.min(Math.max(sTempRect.top, iCenterY), sTempRect.bottom - folderHeight);
        int paddingLeft = this.mLauncher.getWorkspace().getPaddingLeft() + getPaddingLeft();
        if (deviceProfile.isPhone && deviceProfile.availableWidthPx - folderWidth < 4 * paddingLeft) {
            iMin = (deviceProfile.availableWidthPx - folderWidth) / 2;
        } else if (folderWidth >= sTempRect.width()) {
            iMin = sTempRect.left + ((sTempRect.width() - folderWidth) / 2);
        }
        if (folderHeight >= sTempRect.height()) {
            iMax = sTempRect.top + ((sTempRect.height() - folderHeight) / 2);
        } else {
            Rect absoluteOpenFolderBounds = deviceProfile.getAbsoluteOpenFolderBounds();
            iMin = Math.max(absoluteOpenFolderBounds.left, Math.min(iMin, absoluteOpenFolderBounds.right - folderWidth));
            iMax = Math.max(absoluteOpenFolderBounds.top, Math.min(iMin2, absoluteOpenFolderBounds.bottom - folderHeight));
        }
        setPivotX(i + (iCenterX - iMin));
        setPivotY(i2 + (iCenterY - iMax));
        this.mFolderIconPivotX = (int) (this.mFolderIcon.getMeasuredWidth() * ((r2 * 1.0f) / folderWidth));
        this.mFolderIconPivotY = (int) (this.mFolderIcon.getMeasuredHeight() * ((1.0f * r5) / folderHeight));
        layoutParams.width = folderWidth;
        layoutParams.height = folderHeight;
        layoutParams.x = iMin;
        layoutParams.y = iMax;
    }

    public float getPivotXForIconAnimation() {
        return this.mFolderIconPivotX;
    }

    public float getPivotYForIconAnimation() {
        return this.mFolderIconPivotY;
    }

    private int getContentAreaHeight() {
        DeviceProfile deviceProfile = this.mLauncher.getDeviceProfile();
        return Math.max(Math.min((deviceProfile.availableHeightPx - deviceProfile.getTotalWorkspacePadding().y) - this.mFooterHeight, this.mContent.getDesiredHeight()), 5);
    }

    private int getContentAreaWidth() {
        return Math.max(this.mContent.getDesiredWidth(), 5);
    }

    private int getFolderWidth() {
        return getPaddingLeft() + getPaddingRight() + this.mContent.getDesiredWidth();
    }

    private int getFolderHeight() {
        return getFolderHeight(getContentAreaHeight());
    }

    private int getFolderHeight(int i) {
        return getPaddingTop() + getPaddingBottom() + i + this.mFooterHeight;
    }

    @Override
    protected void onMeasure(int i, int i2) {
        int contentAreaWidth = getContentAreaWidth();
        int contentAreaHeight = getContentAreaHeight();
        int iMakeMeasureSpec = View.MeasureSpec.makeMeasureSpec(contentAreaWidth, 1073741824);
        int iMakeMeasureSpec2 = View.MeasureSpec.makeMeasureSpec(contentAreaHeight, 1073741824);
        this.mContent.setFixedSize(contentAreaWidth, contentAreaHeight);
        this.mContent.measure(iMakeMeasureSpec, iMakeMeasureSpec2);
        if (this.mContent.getChildCount() > 0) {
            int cellWidth = (this.mContent.getPageAt(0).getCellWidth() - this.mLauncher.getDeviceProfile().iconSizePx) / 2;
            this.mFooter.setPadding(this.mContent.getPaddingLeft() + cellWidth, this.mFooter.getPaddingTop(), this.mContent.getPaddingRight() + cellWidth, this.mFooter.getPaddingBottom());
        }
        this.mFooter.measure(iMakeMeasureSpec, View.MeasureSpec.makeMeasureSpec(this.mFooterHeight, 1073741824));
        setMeasuredDimension(getPaddingLeft() + getPaddingRight() + contentAreaWidth, getFolderHeight(contentAreaHeight));
    }

    public void rearrangeChildren() {
        rearrangeChildren(-1);
    }

    public void rearrangeChildren(int i) {
        ArrayList<View> itemsInReadingOrder = getItemsInReadingOrder();
        this.mContent.arrangeChildren(itemsInReadingOrder, Math.max(i, itemsInReadingOrder.size()));
        this.mItemsInvalidated = true;
    }

    public int getItemCount() {
        return this.mContent.getItemCount();
    }

    void replaceFolderWithFinalItem() {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                int size = Folder.this.mInfo.contents.size();
                if (size <= 1) {
                    View viewCreateShortcut = null;
                    if (size == 1) {
                        CellLayout cellLayout = Folder.this.mLauncher.getCellLayout(Folder.this.mInfo.container, Folder.this.mInfo.screenId);
                        ShortcutInfo shortcutInfoRemove = Folder.this.mInfo.contents.remove(0);
                        viewCreateShortcut = Folder.this.mLauncher.createShortcut(cellLayout, shortcutInfoRemove);
                        Folder.this.mLauncher.getModelWriter().addOrMoveItemInDatabase(shortcutInfoRemove, Folder.this.mInfo.container, Folder.this.mInfo.screenId, Folder.this.mInfo.cellX, Folder.this.mInfo.cellY);
                    }
                    Folder.this.mLauncher.removeItem(Folder.this.mFolderIcon, Folder.this.mInfo, true);
                    if (Folder.this.mFolderIcon instanceof DropTarget) {
                        Folder.this.mDragController.removeDropTarget((DropTarget) Folder.this.mFolderIcon);
                    }
                    if (viewCreateShortcut != null) {
                        Folder.this.mLauncher.getWorkspace().addInScreenFromBind(viewCreateShortcut, Folder.this.mInfo);
                        viewCreateShortcut.requestFocus();
                    }
                }
            }
        };
        if (this.mContent.getLastItem() != null) {
            this.mFolderIcon.performDestroyAnimation(runnable);
        } else {
            runnable.run();
        }
        this.mDestroyed = true;
    }

    public boolean isDestroyed() {
        return this.mDestroyed;
    }

    public void updateTextViewFocus() {
        View firstItem = this.mContent.getFirstItem();
        final View lastItem = this.mContent.getLastItem();
        if (firstItem != null && lastItem != null) {
            this.mFolderName.setNextFocusDownId(lastItem.getId());
            this.mFolderName.setNextFocusRightId(lastItem.getId());
            this.mFolderName.setNextFocusLeftId(lastItem.getId());
            this.mFolderName.setNextFocusUpId(lastItem.getId());
            this.mFolderName.setNextFocusForwardId(firstItem.getId());
            sHintText = getResources().getString(R.string.folder_hint_text);
            this.mFolderName.setHint(sHintText);
            setNextFocusDownId(firstItem.getId());
            setNextFocusRightId(firstItem.getId());
            setNextFocusLeftId(firstItem.getId());
            setNextFocusUpId(firstItem.getId());
            setOnKeyListener(new View.OnKeyListener() {
                @Override
                public boolean onKey(View view, int i, KeyEvent keyEvent) {
                    if (!(i == 61 && keyEvent.hasModifiers(1)) || !Folder.this.isFocused()) {
                        return false;
                    }
                    return lastItem.requestFocus();
                }
            });
        }
    }

    @Override
    public void onDrop(DropTarget.DragObject dragObject, DragOptions dragOptions) throws Exception {
        PendingAddShortcutInfo pendingAddShortcutInfo;
        View viewCreateAndAddViewForRank;
        Throwable th = null;
        if (!this.mContent.rankOnCurrentPage(this.mEmptyCellRank)) {
            this.mTargetRank = getTargetRank(dragObject, null);
            this.mReorderAlarmListener.onAlarm(this.mReorderAlarm);
            this.mOnScrollHintAlarm.cancelAlarm();
            this.mScrollPauseAlarm.cancelAlarm();
        }
        this.mContent.completePendingPageChanges();
        if (dragObject.dragInfo instanceof PendingAddShortcutInfo) {
            pendingAddShortcutInfo = (PendingAddShortcutInfo) dragObject.dragInfo;
        } else {
            pendingAddShortcutInfo = null;
        }
        ShortcutInfo shortcutInfoCreateShortcutInfo = pendingAddShortcutInfo != null ? pendingAddShortcutInfo.activityInfo.createShortcutInfo() : null;
        if (pendingAddShortcutInfo != null && shortcutInfoCreateShortcutInfo == null) {
            pendingAddShortcutInfo.container = this.mInfo.id;
            pendingAddShortcutInfo.rank = this.mEmptyCellRank;
            this.mLauncher.addPendingItem(pendingAddShortcutInfo, pendingAddShortcutInfo.container, pendingAddShortcutInfo.screenId, null, pendingAddShortcutInfo.spanX, pendingAddShortcutInfo.spanY);
            dragObject.deferDragViewCleanupPostAnimation = false;
            this.mRearrangeOnClose = true;
        } else {
            if (shortcutInfoCreateShortcutInfo == null) {
                if (dragObject.dragInfo instanceof AppInfo) {
                    shortcutInfoCreateShortcutInfo = ((AppInfo) dragObject.dragInfo).makeShortcut();
                } else {
                    shortcutInfoCreateShortcutInfo = (ShortcutInfo) dragObject.dragInfo;
                }
            }
            if (this.mIsExternalDrag) {
                viewCreateAndAddViewForRank = this.mContent.createAndAddViewForRank(shortcutInfoCreateShortcutInfo, this.mEmptyCellRank);
                this.mLauncher.getModelWriter().addOrMoveItemInDatabase(shortcutInfoCreateShortcutInfo, this.mInfo.id, 0L, shortcutInfoCreateShortcutInfo.cellX, shortcutInfoCreateShortcutInfo.cellY);
                if (dragObject.dragSource != this) {
                    updateItemLocationsInDatabaseBatch();
                }
                this.mIsExternalDrag = false;
            } else {
                viewCreateAndAddViewForRank = this.mCurrentDragView;
                this.mContent.addViewForRank(viewCreateAndAddViewForRank, shortcutInfoCreateShortcutInfo, this.mEmptyCellRank);
            }
            if (dragObject.dragView.hasDrawn()) {
                float scaleX = getScaleX();
                float scaleY = getScaleY();
                setScaleX(1.0f);
                setScaleY(1.0f);
                this.mLauncher.getDragLayer().animateViewIntoPosition(dragObject.dragView, viewCreateAndAddViewForRank, null);
                setScaleX(scaleX);
                setScaleY(scaleY);
            } else {
                dragObject.deferDragViewCleanupPostAnimation = false;
                viewCreateAndAddViewForRank.setVisibility(0);
            }
            this.mItemsInvalidated = true;
            rearrangeChildren();
            SuppressInfoChanges suppressInfoChanges = new SuppressInfoChanges();
            try {
                try {
                    this.mInfo.add(shortcutInfoCreateShortcutInfo, false);
                } catch (Throwable th2) {
                    th = th2;
                    throw th;
                }
            } finally {
                $closeResource(th, suppressInfoChanges);
            }
        }
        this.mDragInProgress = false;
        if (this.mContent.getPageCount() > 1) {
            this.mInfo.setOption(4, true, this.mLauncher.getModelWriter());
        }
        this.mLauncher.getStateManager().goToState(LauncherState.NORMAL, 500L);
        if (dragObject.stateAnnouncer != null) {
            dragObject.stateAnnouncer.completeAction(R.string.item_moved);
        }
    }

    public void hideItem(ShortcutInfo shortcutInfo) {
        getViewForInfo(shortcutInfo).setVisibility(4);
    }

    public void showItem(ShortcutInfo shortcutInfo) {
        getViewForInfo(shortcutInfo).setVisibility(0);
    }

    @Override
    public void onAdd(ShortcutInfo shortcutInfo, int i) {
        View viewCreateAndAddViewForRank = this.mContent.createAndAddViewForRank(shortcutInfo, i);
        this.mLauncher.getModelWriter().addOrMoveItemInDatabase(shortcutInfo, this.mInfo.id, 0L, shortcutInfo.cellX, shortcutInfo.cellY);
        ArrayList<View> arrayList = new ArrayList<>(getItemsInReadingOrder());
        arrayList.add(i, viewCreateAndAddViewForRank);
        this.mContent.arrangeChildren(arrayList, arrayList.size());
        this.mItemsInvalidated = true;
    }

    @Override
    public void onRemove(ShortcutInfo shortcutInfo) {
        this.mItemsInvalidated = true;
        this.mContent.removeItem(getViewForInfo(shortcutInfo));
        if (this.mState == 1) {
            this.mRearrangeOnClose = true;
        } else {
            rearrangeChildren();
        }
        if (getItemCount() <= 1) {
            if (this.mIsOpen) {
                close(true);
            } else {
                replaceFolderWithFinalItem();
            }
        }
    }

    private View getViewForInfo(final ShortcutInfo shortcutInfo) {
        return this.mContent.iterateOverItems(new Workspace.ItemOperator() {
            @Override
            public boolean evaluate(ItemInfo itemInfo, View view) {
                return itemInfo == shortcutInfo;
            }
        });
    }

    @Override
    public void onItemsChanged(boolean z) {
        updateTextViewFocus();
    }

    @Override
    public void prepareAutoUpdate() {
        close(false);
    }

    @Override
    public void onTitleChanged(CharSequence charSequence) {
    }

    public ArrayList<View> getItemsInReadingOrder() {
        if (this.mItemsInvalidated) {
            this.mItemsInReadingOrder.clear();
            this.mContent.iterateOverItems(new Workspace.ItemOperator() {
                @Override
                public boolean evaluate(ItemInfo itemInfo, View view) {
                    Folder.this.mItemsInReadingOrder.add(view);
                    return false;
                }
            });
            this.mItemsInvalidated = false;
        }
        return this.mItemsInReadingOrder;
    }

    public List<BubbleTextView> getItemsOnPage(int i) {
        int i2;
        ArrayList<View> itemsInReadingOrder = getItemsInReadingOrder();
        int pageCount = this.mContent.getPageCount() - 1;
        int size = itemsInReadingOrder.size();
        int iItemsPerPage = this.mContent.itemsPerPage();
        if (i == pageCount) {
            i2 = size - (iItemsPerPage * i);
        } else {
            i2 = iItemsPerPage;
        }
        int i3 = i * iItemsPerPage;
        int iMin = Math.min(i3 + i2, itemsInReadingOrder.size());
        ArrayList arrayList = new ArrayList(i2);
        while (i3 < iMin) {
            arrayList.add((BubbleTextView) itemsInReadingOrder.get(i3));
            i3++;
        }
        return arrayList;
    }

    @Override
    public void onFocusChange(View view, boolean z) {
        if (view == this.mFolderName) {
            if (z) {
                startEditingFolderName();
            } else {
                this.mFolderName.dispatchBackKey();
            }
        }
    }

    @Override
    public void getHitRectRelativeToDragLayer(Rect rect) {
        getHitRect(rect);
        rect.left -= this.mScrollAreaOffset;
        rect.right += this.mScrollAreaOffset;
    }

    @Override
    public void fillInLogContainerData(View view, ItemInfo itemInfo, LauncherLogProto.Target target, LauncherLogProto.Target target2) {
        target.gridX = itemInfo.cellX;
        target.gridY = itemInfo.cellY;
        target.pageIndex = this.mContent.getCurrentPage();
        target2.containerType = 3;
    }

    private class OnScrollHintListener implements OnAlarmListener {
        private final DropTarget.DragObject mDragObject;

        OnScrollHintListener(DropTarget.DragObject dragObject) {
            this.mDragObject = dragObject;
        }

        @Override
        public void onAlarm(Alarm alarm) {
            if (Folder.this.mCurrentScrollDir == 0) {
                Folder.this.mContent.scrollLeft();
                Folder.this.mScrollHintDir = -1;
            } else if (Folder.this.mCurrentScrollDir == 1) {
                Folder.this.mContent.scrollRight();
                Folder.this.mScrollHintDir = -1;
            } else {
                return;
            }
            Folder.this.mCurrentScrollDir = -1;
            Folder.this.mScrollPauseAlarm.setOnAlarmListener(Folder.this.new OnScrollFinishedListener(this.mDragObject));
            Folder.this.mScrollPauseAlarm.setAlarm(900L);
        }
    }

    private class OnScrollFinishedListener implements OnAlarmListener {
        private final DropTarget.DragObject mDragObject;

        OnScrollFinishedListener(DropTarget.DragObject dragObject) {
            this.mDragObject = dragObject;
        }

        @Override
        public void onAlarm(Alarm alarm) {
            Folder.this.onDragOver(this.mDragObject);
        }
    }

    private class SuppressInfoChanges implements AutoCloseable {
        SuppressInfoChanges() {
            Folder.this.mInfo.removeListener(Folder.this);
        }

        @Override
        public void close() {
            Folder.this.mInfo.addListener(Folder.this);
            Folder.this.updateTextViewFocus();
        }
    }

    public static Folder getOpen(Launcher launcher) {
        return (Folder) getOpenView(launcher, 1);
    }

    @Override
    public void logActionCommand(int i) {
        this.mLauncher.getUserEventDispatcher().logActionCommand(i, getFolderIcon(), 3);
    }

    @Override
    public boolean onBackPressed() {
        if (isEditingName()) {
            this.mFolderName.dispatchBackKey();
            return true;
        }
        super.onBackPressed();
        return true;
    }

    @Override
    public boolean onControllerInterceptTouchEvent(MotionEvent motionEvent) {
        if (motionEvent.getAction() == 0) {
            DragLayer dragLayer = this.mLauncher.getDragLayer();
            if (isEditingName()) {
                if (dragLayer.isEventOverView(this.mFolderName, motionEvent)) {
                    return false;
                }
                this.mFolderName.dispatchBackKey();
                return true;
            }
            if (!dragLayer.isEventOverView(this, motionEvent)) {
                if (this.mLauncher.getAccessibilityDelegate().isInAccessibleDrag()) {
                    if (!dragLayer.isEventOverView(this.mLauncher.getDropTargetBar(), motionEvent)) {
                        return true;
                    }
                } else {
                    this.mLauncher.getUserEventDispatcher().logActionTapOutside(LoggerUtils.newContainerTarget(3));
                    close(true);
                    return true;
                }
            }
        }
        return false;
    }
}
