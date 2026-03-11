package com.android.launcher3;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.TimeInterpolator;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.os.Bundle;
import android.text.Selection;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ActionMode;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.android.launcher3.CellLayout;
import com.android.launcher3.DragController;
import com.android.launcher3.DragLayer;
import com.android.launcher3.DropTarget;
import com.android.launcher3.ExtendedEditText;
import com.android.launcher3.FolderInfo;
import com.android.launcher3.Stats;
import com.android.launcher3.UninstallDropTarget;
import com.android.launcher3.Workspace;
import com.android.launcher3.accessibility.LauncherAccessibilityDelegate;
import com.android.launcher3.config.FeatureFlags;
import com.android.launcher3.util.UiThreadCircularReveal;
import com.mediatek.launcher3.LauncherLog;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class Folder extends LinearLayout implements DragSource, View.OnClickListener, View.OnLongClickListener, DropTarget, FolderInfo.FolderListener, TextView.OnEditorActionListener, View.OnFocusChangeListener, DragController.DragListener, UninstallDropTarget.UninstallSource, LauncherAccessibilityDelegate.AccessibilityDragSource, Stats.LaunchSourceProvider {
    private static String sDefaultFolderName;
    private static String sHintText;
    FolderPagedView mContent;
    View mContentWrapper;
    private ShortcutInfo mCurrentDragInfo;
    private View mCurrentDragView;
    int mCurrentScrollDir;
    private boolean mDeferDropAfterUninstall;
    Runnable mDeferredAction;
    private boolean mDeleteFolderOnDropCompleted;
    private boolean mDestroyed;
    protected DragController mDragController;
    private boolean mDragInProgress;
    int mEmptyCellRank;
    private final int mExpandDuration;
    FolderIcon mFolderIcon;
    float mFolderIconPivotX;
    float mFolderIconPivotY;
    ExtendedEditText mFolderName;
    private View mFooter;
    private int mFooterHeight;
    protected FolderInfo mInfo;
    private final InputMethodManager mInputMethodManager;
    private boolean mIsEditingName;
    private boolean mIsExternalDrag;
    private boolean mItemAddedBackToSelfViaIcon;
    final ArrayList<View> mItemsInReadingOrder;
    boolean mItemsInvalidated;
    protected final Launcher mLauncher;
    private final int mMaterialExpandDuration;
    private final int mMaterialExpandStagger;
    private final Alarm mOnExitAlarm;
    OnAlarmListener mOnExitAlarmListener;
    private final Alarm mOnScrollHintAlarm;
    int mPrevTargetRank;
    private boolean mRearrangeOnClose;
    private final Alarm mReorderAlarm;
    OnAlarmListener mReorderAlarmListener;
    private int mScrollAreaOffset;
    int mScrollHintDir;
    final Alarm mScrollPauseAlarm;
    int mState;
    private boolean mSuppressFolderDeletion;
    boolean mSuppressOnAdd;
    int mTargetRank;
    private boolean mUninstallSuccessful;
    private static final Rect sTempRect = new Rect();
    public static final Comparator<ItemInfo> ITEM_POS_COMPARATOR = new Comparator<ItemInfo>() {
        @Override
        public int compare(ItemInfo lhs, ItemInfo rhs) {
            if (lhs.rank != rhs.rank) {
                return lhs.rank - rhs.rank;
            }
            if (lhs.cellY != rhs.cellY) {
                return lhs.cellY - rhs.cellY;
            }
            return lhs.cellX - rhs.cellX;
        }
    };

    public Folder(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mReorderAlarm = new Alarm();
        this.mOnExitAlarm = new Alarm();
        this.mOnScrollHintAlarm = new Alarm();
        this.mScrollPauseAlarm = new Alarm();
        this.mItemsInReadingOrder = new ArrayList<>();
        this.mState = -1;
        this.mRearrangeOnClose = false;
        this.mItemsInvalidated = false;
        this.mSuppressOnAdd = false;
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
        this.mInputMethodManager = (InputMethodManager) getContext().getSystemService("input_method");
        Resources res = getResources();
        this.mExpandDuration = res.getInteger(R.integer.config_folderExpandDuration);
        this.mMaterialExpandDuration = res.getInteger(R.integer.config_materialFolderExpandDuration);
        this.mMaterialExpandStagger = res.getInteger(R.integer.config_materialFolderExpandStagger);
        if (sDefaultFolderName == null) {
            sDefaultFolderName = res.getString(R.string.folder_name);
        }
        if (sHintText == null) {
            sHintText = res.getString(R.string.folder_hint_text);
        }
        this.mLauncher = (Launcher) context;
        setFocusableInTouchMode(true);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mContentWrapper = findViewById(R.id.folder_content_wrapper);
        this.mContent = (FolderPagedView) findViewById(R.id.folder_content);
        this.mContent.setFolder(this);
        this.mFolderName = (ExtendedEditText) findViewById(R.id.folder_name);
        this.mFolderName.setOnBackKeyListener(new ExtendedEditText.OnBackKeyListener() {
            @Override
            public boolean onBackKey() {
                Folder.this.doneEditingFolderName(true);
                return false;
            }
        });
        this.mFolderName.setOnFocusChangeListener(this);
        if (!Utilities.ATLEAST_MARSHMALLOW) {
            this.mFolderName.setCustomSelectionActionModeCallback(new ActionMode.Callback() {
                @Override
                public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                    return false;
                }

                @Override
                public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                    return false;
                }

                @Override
                public void onDestroyActionMode(ActionMode mode) {
                }

                @Override
                public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                    return false;
                }
            });
        }
        this.mFolderName.setOnEditorActionListener(this);
        this.mFolderName.setSelectAllOnFocus(true);
        this.mFolderName.setInputType(this.mFolderName.getInputType() | 524288 | 8192);
        this.mFooter = findViewById(R.id.folder_footer);
        this.mFooter.measure(0, 0);
        this.mFooterHeight = this.mFooter.getMeasuredHeight();
    }

    @Override
    public void onClick(View v) {
        Object tag = v.getTag();
        if (LauncherLog.DEBUG) {
            LauncherLog.d("Launcher.Folder", "onClick: v = " + v + ", tag = " + tag);
        }
        if (!(tag instanceof ShortcutInfo)) {
            return;
        }
        this.mLauncher.onClick(v);
    }

    @Override
    public boolean onLongClick(View v) {
        if (LauncherLog.DEBUG) {
            LauncherLog.d("Launcher.Folder", "onLongClick: v = " + v + ", tag = " + v.getTag());
        }
        if (this.mLauncher.isDraggingEnabled()) {
            return beginDrag(v, false);
        }
        return true;
    }

    private boolean beginDrag(View v, boolean accessible) {
        Object tag = v.getTag();
        if (tag instanceof ShortcutInfo) {
            ShortcutInfo item = (ShortcutInfo) tag;
            if (!v.isInTouchMode()) {
                return false;
            }
            this.mLauncher.getWorkspace().beginDragShared(v, new Point(), this, accessible);
            this.mCurrentDragInfo = item;
            this.mEmptyCellRank = item.rank;
            this.mCurrentDragView = v;
            this.mContent.removeItem(this.mCurrentDragView);
            this.mInfo.remove(this.mCurrentDragInfo);
            this.mDragInProgress = true;
            this.mItemAddedBackToSelfViaIcon = false;
        }
        return true;
    }

    @Override
    public void startDrag(CellLayout.CellInfo cellInfo, boolean accessible) {
        beginDrag(cellInfo.cell, accessible);
    }

    @Override
    public void enableAccessibleDrag(boolean enable) {
        this.mLauncher.getSearchDropTargetBar().enableAccessibleDrag(enable);
        for (int i = 0; i < this.mContent.getChildCount(); i++) {
            this.mContent.getPageAt(i).enableAccessibleDrag(enable, 1);
        }
        this.mFooter.setImportantForAccessibility(enable ? 4 : 0);
        this.mLauncher.getWorkspace().setAddNewPageOnDrag(enable ? false : true);
    }

    public boolean isEditingName() {
        return this.mIsEditingName;
    }

    public void startEditingFolderName() {
        this.mFolderName.setHint("");
        this.mIsEditingName = true;
    }

    public void dismissEditingName() {
        this.mInputMethodManager.hideSoftInputFromWindow(getWindowToken(), 0);
        doneEditingFolderName(true);
    }

    public void doneEditingFolderName(boolean commit) {
        this.mFolderName.setHint(sHintText);
        String newTitle = this.mFolderName.getText().toString();
        this.mInfo.setTitle(newTitle);
        LauncherModel.updateItemInDatabase(this.mLauncher, this.mInfo);
        if (commit) {
            sendCustomAccessibilityEvent(32, String.format(getContext().getString(R.string.folder_renamed), newTitle));
        }
        this.mFolderName.clearFocus();
        Selection.setSelection(this.mFolderName.getText(), 0, 0);
        this.mIsEditingName = false;
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (actionId == 6) {
            dismissEditingName();
            return true;
        }
        return false;
    }

    public View getEditTextRegion() {
        return this.mFolderName;
    }

    @Override
    @SuppressLint({"ClickableViewAccessibility"})
    public boolean onTouchEvent(MotionEvent ev) {
        return true;
    }

    public void setDragController(DragController dragController) {
        this.mDragController = dragController;
    }

    public void setFolderIcon(FolderIcon icon) {
        this.mFolderIcon = icon;
    }

    @Override
    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
        return true;
    }

    public FolderInfo getInfo() {
        return this.mInfo;
    }

    void bind(FolderInfo info) {
        this.mInfo = info;
        ArrayList<ShortcutInfo> children = info.contents;
        Collections.sort(children, ITEM_POS_COMPARATOR);
        ArrayList<ShortcutInfo> overflow = this.mContent.bindItems(children);
        for (ShortcutInfo item : overflow) {
            this.mInfo.remove(item);
            LauncherModel.deleteItemFromDatabase(this.mLauncher, item);
        }
        if (((DragLayer.LayoutParams) getLayoutParams()) == null) {
            DragLayer.LayoutParams lp = new DragLayer.LayoutParams(0, 0);
            lp.customPosition = true;
            setLayoutParams(lp);
        }
        centerAboutIcon();
        this.mItemsInvalidated = true;
        updateTextViewFocus();
        this.mInfo.addListener(this);
        if (!sDefaultFolderName.contentEquals(this.mInfo.title)) {
            this.mFolderName.setText(this.mInfo.title);
        } else {
            this.mFolderName.setText("");
        }
        this.mFolderIcon.post(new Runnable() {
            @Override
            public void run() {
                if (Folder.this.getItemCount() > 1) {
                    return;
                }
                Folder.this.replaceFolderWithFinalItem();
            }
        });
    }

    @SuppressLint({"InflateParams"})
    static Folder fromXml(Launcher launcher) {
        return (Folder) launcher.getLayoutInflater().inflate(FeatureFlags.LAUNCHER3_ICON_NORMALIZATION ? R.layout.user_folder_icon_normalized : R.layout.user_folder, (ViewGroup) null);
    }

    private void positionAndSizeAsIcon() {
        if (getParent() instanceof DragLayer) {
            setScaleX(0.8f);
            setScaleY(0.8f);
            setAlpha(0.0f);
            this.mState = 0;
        }
    }

    private void prepareReveal() {
        setScaleX(1.0f);
        setScaleY(1.0f);
        setAlpha(1.0f);
        this.mState = 0;
    }

    public void animateOpen() {
        Animator openFolderAnim;
        final Runnable onCompleteRunnable;
        if (getParent() instanceof DragLayer) {
            this.mContent.completePendingPageChanges();
            if (!this.mDragInProgress) {
                this.mContent.snapToPageImmediately(0);
            }
            this.mDeleteFolderOnDropCompleted = false;
            if (!Utilities.ATLEAST_LOLLIPOP) {
                positionAndSizeAsIcon();
                centerAboutIcon();
                PropertyValuesHolder alpha = PropertyValuesHolder.ofFloat("alpha", 1.0f);
                PropertyValuesHolder scaleX = PropertyValuesHolder.ofFloat("scaleX", 1.0f);
                PropertyValuesHolder scaleY = PropertyValuesHolder.ofFloat("scaleY", 1.0f);
                ObjectAnimator oa = LauncherAnimUtils.ofPropertyValuesHolder(this, alpha, scaleX, scaleY);
                oa.setDuration(this.mExpandDuration);
                openFolderAnim = oa;
                setLayerType(2, null);
                onCompleteRunnable = new Runnable() {
                    @Override
                    public void run() {
                        Folder.this.setLayerType(0, null);
                    }
                };
            } else {
                prepareReveal();
                centerAboutIcon();
                AnimatorSet anim = LauncherAnimUtils.createAnimatorSet();
                int width = getPaddingLeft() + getPaddingRight() + this.mContent.getDesiredWidth();
                int height = getFolderHeight();
                float transX = (-0.075f) * ((width / 2) - getPivotX());
                float transY = (-0.075f) * ((height / 2) - getPivotY());
                setTranslationX(transX);
                setTranslationY(transY);
                PropertyValuesHolder tx = PropertyValuesHolder.ofFloat("translationX", transX, 0.0f);
                PropertyValuesHolder ty = PropertyValuesHolder.ofFloat("translationY", transY, 0.0f);
                Animator drift = ObjectAnimator.ofPropertyValuesHolder(this, tx, ty);
                drift.setDuration(this.mMaterialExpandDuration);
                drift.setStartDelay(this.mMaterialExpandStagger);
                drift.setInterpolator(new LogDecelerateInterpolator(100, 0));
                int rx = (int) Math.max(Math.max(width - getPivotX(), 0.0f), getPivotX());
                int ry = (int) Math.max(Math.max(height - getPivotY(), 0.0f), getPivotY());
                float radius = (float) Math.hypot(rx, ry);
                boolean isHardwareAccelerated = isHardwareAccelerated();
                Animator reveal = null;
                if (isHardwareAccelerated) {
                    reveal = UiThreadCircularReveal.createCircularReveal(this, (int) getPivotX(), (int) getPivotY(), 0.0f, radius);
                    reveal.setDuration(this.mMaterialExpandDuration);
                    reveal.setInterpolator(new LogDecelerateInterpolator(100, 0));
                }
                this.mContentWrapper.setAlpha(0.0f);
                Animator iconsAlpha = ObjectAnimator.ofFloat(this.mContentWrapper, "alpha", 0.0f, 1.0f);
                iconsAlpha.setDuration(this.mMaterialExpandDuration);
                iconsAlpha.setStartDelay(this.mMaterialExpandStagger);
                iconsAlpha.setInterpolator(new AccelerateInterpolator(1.5f));
                this.mFooter.setAlpha(0.0f);
                Animator textAlpha = ObjectAnimator.ofFloat(this.mFooter, "alpha", 0.0f, 1.0f);
                textAlpha.setDuration(this.mMaterialExpandDuration);
                textAlpha.setStartDelay(this.mMaterialExpandStagger);
                textAlpha.setInterpolator(new AccelerateInterpolator(1.5f));
                anim.play(drift);
                anim.play(iconsAlpha);
                anim.play(textAlpha);
                if (isHardwareAccelerated) {
                    anim.play(reveal);
                }
                openFolderAnim = anim;
                this.mContentWrapper.setLayerType(2, null);
                this.mFooter.setLayerType(2, null);
                onCompleteRunnable = new Runnable() {
                    @Override
                    public void run() {
                        Folder.this.mContentWrapper.setLayerType(0, null);
                        Folder.this.mContentWrapper.setLayerType(0, null);
                    }
                };
            }
            openFolderAnim.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(Animator animation) {
                    Folder.this.sendCustomAccessibilityEvent(32, Folder.this.mContent.getAccessibilityDescription());
                    Folder.this.mState = 1;
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    Folder.this.mState = 2;
                    onCompleteRunnable.run();
                    Folder.this.mContent.setFocusOnFirstChild();
                }
            });
            if (this.mContent.getPageCount() > 1 && !this.mInfo.hasOption(4)) {
                int footerWidth = (this.mContent.getDesiredWidth() - this.mFooter.getPaddingLeft()) - this.mFooter.getPaddingRight();
                float textWidth = this.mFolderName.getPaint().measureText(this.mFolderName.getText().toString());
                float translation = (footerWidth - textWidth) / 2.0f;
                ExtendedEditText extendedEditText = this.mFolderName;
                if (this.mContent.mIsRtl) {
                    translation = -translation;
                }
                extendedEditText.setTranslationX(translation);
                this.mContent.setMarkerScale(0.0f);
                boolean updateAnimationFlag = !this.mDragInProgress;
                final boolean z = updateAnimationFlag;
                openFolderAnim.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        TimeInterpolator logDecelerateInterpolator;
                        ViewPropertyAnimator viewPropertyAnimatorTranslationX = Folder.this.mFolderName.animate().setDuration(633L).translationX(0.0f);
                        if (Utilities.ATLEAST_LOLLIPOP) {
                            logDecelerateInterpolator = AnimationUtils.loadInterpolator(Folder.this.mLauncher, android.R.interpolator.fast_out_slow_in);
                        } else {
                            logDecelerateInterpolator = new LogDecelerateInterpolator(100, 0);
                        }
                        viewPropertyAnimatorTranslationX.setInterpolator(logDecelerateInterpolator);
                        Folder.this.mContent.animateMarkers();
                        if (!z) {
                            return;
                        }
                        Folder.this.mInfo.setOption(4, true, Folder.this.mLauncher);
                    }
                });
            } else {
                this.mFolderName.setTranslationX(0.0f);
                this.mContent.setMarkerScale(1.0f);
            }
            openFolderAnim.start();
            if (this.mDragController.isDragging()) {
                this.mDragController.forceTouchMove();
            }
            FolderPagedView pages = this.mContent;
            pages.verifyVisibleHighResIcons(pages.getNextPage());
        }
    }

    public void beginExternalDrag(ShortcutInfo item) {
        this.mCurrentDragInfo = item;
        this.mEmptyCellRank = this.mContent.allocateRankForNewItem(item);
        this.mIsExternalDrag = true;
        this.mDragInProgress = true;
        this.mDragController.addDragListener(this);
    }

    @Override
    public void onDragStart(DragSource source, Object info, int dragAction) {
    }

    @Override
    public void onDragEnd() {
        if (this.mIsExternalDrag && this.mDragInProgress) {
            completeDragExit();
        }
        this.mDragController.removeDragListener(this);
    }

    void sendCustomAccessibilityEvent(int type, String text) {
        AccessibilityManager accessibilityManager = (AccessibilityManager) getContext().getSystemService("accessibility");
        if (!accessibilityManager.isEnabled()) {
            return;
        }
        AccessibilityEvent event = AccessibilityEvent.obtain(type);
        onInitializeAccessibilityEvent(event);
        event.getText().add(text);
        accessibilityManager.sendAccessibilityEvent(event);
    }

    public void animateClosed() {
        if (LauncherLog.DEBUG) {
            LauncherLog.d("Launcher.Folder", "animateClosed: parent = " + getParent());
        }
        if (getParent() instanceof DragLayer) {
            PropertyValuesHolder alpha = PropertyValuesHolder.ofFloat("alpha", 0.0f);
            PropertyValuesHolder scaleX = PropertyValuesHolder.ofFloat("scaleX", 0.9f);
            PropertyValuesHolder scaleY = PropertyValuesHolder.ofFloat("scaleY", 0.9f);
            ObjectAnimator oa = LauncherAnimUtils.ofPropertyValuesHolder(this, alpha, scaleX, scaleY);
            oa.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    Folder.this.setLayerType(0, null);
                    Folder.this.close(true);
                }

                @Override
                public void onAnimationStart(Animator animation) {
                    Folder.this.sendCustomAccessibilityEvent(32, Folder.this.getContext().getString(R.string.folder_closed));
                    Folder.this.mState = 1;
                }
            });
            oa.setDuration(this.mExpandDuration);
            setLayerType(2, null);
            oa.start();
        }
    }

    public void close(boolean wasAnimated) {
        DragLayer parent = (DragLayer) getParent();
        if (parent != null) {
            parent.removeView(this);
        }
        this.mDragController.removeDropTarget(this);
        clearFocus();
        if (wasAnimated) {
            this.mFolderIcon.requestFocus();
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
    }

    @Override
    public boolean acceptDrop(DropTarget.DragObject d) {
        if (LauncherLog.DEBUG) {
            LauncherLog.d("Launcher.Folder", "acceptDrop: DragObject = " + d);
        }
        ItemInfo item = (ItemInfo) d.dragInfo;
        int itemType = item.itemType;
        return (itemType == 0 || itemType == 1) && !isFull();
    }

    @Override
    public void onDragEnter(DropTarget.DragObject d) {
        this.mPrevTargetRank = -1;
        this.mOnExitAlarm.cancelAlarm();
        this.mScrollAreaOffset = (d.dragView.getDragRegionWidth() / 2) - d.xOffset;
    }

    @TargetApi(17)
    public boolean isLayoutRtl() {
        return getLayoutDirection() == 1;
    }

    @Override
    public void onDragOver(DropTarget.DragObject d) {
        onDragOver(d, 250);
    }

    private int getTargetRank(DropTarget.DragObject d, float[] recycle) {
        float[] recycle2 = d.getVisualCenter(recycle);
        return this.mContent.findNearestArea(((int) recycle2[0]) - getPaddingLeft(), ((int) recycle2[1]) - getPaddingTop());
    }

    void onDragOver(DropTarget.DragObject d, int reorderDelay) {
        if (this.mScrollPauseAlarm.alarmPending()) {
            return;
        }
        float[] r = new float[2];
        this.mTargetRank = getTargetRank(d, r);
        if (this.mTargetRank != this.mPrevTargetRank) {
            this.mReorderAlarm.cancelAlarm();
            this.mReorderAlarm.setOnAlarmListener(this.mReorderAlarmListener);
            this.mReorderAlarm.setAlarm(250L);
            this.mPrevTargetRank = this.mTargetRank;
            if (d.stateAnnouncer != null) {
                d.stateAnnouncer.announce(getContext().getString(R.string.move_to_position, Integer.valueOf(this.mTargetRank + 1)));
            }
        }
        float x = r[0];
        int currentPage = this.mContent.getNextPage();
        float cellOverlap = this.mContent.getCurrentCellLayout().getCellWidth() * 0.45f;
        boolean isOutsideLeftEdge = x < cellOverlap;
        boolean isOutsideRightEdge = x > ((float) getWidth()) - cellOverlap;
        if (currentPage > 0) {
            if (this.mContent.mIsRtl ? isOutsideRightEdge : isOutsideLeftEdge) {
                showScrollHint(0, d);
                return;
            }
        }
        if (currentPage < this.mContent.getPageCount() - 1) {
            if (!this.mContent.mIsRtl) {
                isOutsideLeftEdge = isOutsideRightEdge;
            }
            if (isOutsideLeftEdge) {
                showScrollHint(1, d);
                return;
            }
        }
        this.mOnScrollHintAlarm.cancelAlarm();
        if (this.mScrollHintDir == -1) {
            return;
        }
        this.mContent.clearScrollHint();
        this.mScrollHintDir = -1;
    }

    private void showScrollHint(int direction, DropTarget.DragObject d) {
        if (this.mScrollHintDir != direction) {
            this.mContent.showScrollHint(direction);
            this.mScrollHintDir = direction;
        }
        if (this.mOnScrollHintAlarm.alarmPending() && this.mCurrentScrollDir == direction) {
            return;
        }
        this.mCurrentScrollDir = direction;
        this.mOnScrollHintAlarm.cancelAlarm();
        this.mOnScrollHintAlarm.setOnAlarmListener(new OnScrollHintListener(d));
        this.mOnScrollHintAlarm.setAlarm(500L);
        this.mReorderAlarm.cancelAlarm();
        this.mTargetRank = this.mEmptyCellRank;
    }

    public void completeDragExit() {
        if (this.mInfo.opened) {
            this.mLauncher.closeFolder();
            this.mRearrangeOnClose = true;
        } else if (this.mState == 1) {
            this.mRearrangeOnClose = true;
        } else {
            rearrangeChildren();
            clearDragInfo();
        }
    }

    private void clearDragInfo() {
        this.mCurrentDragInfo = null;
        this.mCurrentDragView = null;
        this.mSuppressOnAdd = false;
        this.mIsExternalDrag = false;
    }

    @Override
    public void onDragExit(DropTarget.DragObject d) {
        if (LauncherLog.DEBUG) {
            LauncherLog.d("Launcher.Folder", "onDragExit: DragObject = " + d);
        }
        if (!d.dragComplete) {
            this.mOnExitAlarm.setOnAlarmListener(this.mOnExitAlarmListener);
            this.mOnExitAlarm.setAlarm(400L);
        }
        this.mReorderAlarm.cancelAlarm();
        this.mOnScrollHintAlarm.cancelAlarm();
        this.mScrollPauseAlarm.cancelAlarm();
        if (this.mScrollHintDir == -1) {
            return;
        }
        this.mContent.clearScrollHint();
        this.mScrollHintDir = -1;
    }

    @Override
    public void prepareAccessibilityDrop() {
        if (!this.mReorderAlarm.alarmPending()) {
            return;
        }
        this.mReorderAlarm.cancelAlarm();
        this.mReorderAlarmListener.onAlarm(this.mReorderAlarm);
    }

    @Override
    public void onDropCompleted(final View target, final DropTarget.DragObject d, final boolean isFlingToDelete, final boolean success) {
        if (LauncherLog.DEBUG) {
            LauncherLog.d("Launcher.Folder", "onDropCompleted: View = " + target + ", DragObject = " + d + ", isFlingToDelete = " + isFlingToDelete + ", success = " + success);
        }
        if (this.mDeferDropAfterUninstall) {
            Log.d("Launcher.Folder", "Deferred handling drop because waiting for uninstall.");
            this.mDeferredAction = new Runnable() {
                @Override
                public void run() {
                    Folder.this.onDropCompleted(target, d, isFlingToDelete, success);
                    Folder.this.mDeferredAction = null;
                }
            };
            return;
        }
        boolean beingCalledAfterUninstall = this.mDeferredAction != null;
        boolean successfulDrop = success ? beingCalledAfterUninstall ? this.mUninstallSuccessful : true : false;
        if (successfulDrop) {
            if (this.mDeleteFolderOnDropCompleted && !this.mItemAddedBackToSelfViaIcon && target != this) {
                replaceFolderWithFinalItem();
            }
        } else {
            ShortcutInfo info = (ShortcutInfo) d.dragInfo;
            View icon = (this.mCurrentDragView == null || this.mCurrentDragView.getTag() != info) ? this.mContent.createNewView(info) : this.mCurrentDragView;
            ArrayList<View> views = getItemsInReadingOrder();
            views.add(info.rank, icon);
            this.mContent.arrangeChildren(views, views.size());
            this.mItemsInvalidated = true;
            this.mSuppressOnAdd = true;
            this.mFolderIcon.onDrop(d);
            this.mSuppressOnAdd = false;
        }
        if (target != this && this.mOnExitAlarm.alarmPending()) {
            this.mOnExitAlarm.cancelAlarm();
            if (!successfulDrop) {
                this.mSuppressFolderDeletion = true;
            }
            this.mScrollPauseAlarm.cancelAlarm();
            completeDragExit();
        }
        this.mDeleteFolderOnDropCompleted = false;
        this.mDragInProgress = false;
        this.mItemAddedBackToSelfViaIcon = false;
        this.mCurrentDragInfo = null;
        this.mCurrentDragView = null;
        this.mSuppressOnAdd = false;
        updateItemLocationsInDatabaseBatch();
        if (getItemCount() > this.mContent.itemsPerPage()) {
            return;
        }
        this.mInfo.setOption(4, false, this.mLauncher);
    }

    @Override
    public void deferCompleteDropAfterUninstallActivity() {
        this.mDeferDropAfterUninstall = true;
    }

    @Override
    public void onUninstallActivityReturned(boolean success) {
        this.mDeferDropAfterUninstall = false;
        this.mUninstallSuccessful = success;
        if (this.mDeferredAction == null) {
            return;
        }
        this.mDeferredAction.run();
    }

    @Override
    public float getIntrinsicIconScaleFactor() {
        return 1.0f;
    }

    @Override
    public boolean supportsFlingToDelete() {
        return true;
    }

    @Override
    public boolean supportsAppInfoDropTarget() {
        return false;
    }

    @Override
    public boolean supportsDeleteDropTarget() {
        return true;
    }

    @Override
    public void onFlingToDelete(DropTarget.DragObject d, PointF vec) {
    }

    @Override
    public void onFlingToDeleteCompleted() {
    }

    private void updateItemLocationsInDatabaseBatch() {
        ArrayList<View> list = getItemsInReadingOrder();
        ArrayList<ItemInfo> items = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            View v = list.get(i);
            ItemInfo info = (ItemInfo) v.getTag();
            info.rank = i;
            items.add(info);
        }
        LauncherModel.moveItemsInDatabase(this.mLauncher, items, this.mInfo.id, 0);
    }

    public void notifyDrop() {
        if (!this.mDragInProgress) {
            return;
        }
        this.mItemAddedBackToSelfViaIcon = true;
    }

    @Override
    public boolean isDropEnabled() {
        return true;
    }

    public boolean isFull() {
        return this.mContent.isFull();
    }

    private void centerAboutIcon() {
        DragLayer.LayoutParams lp = (DragLayer.LayoutParams) getLayoutParams();
        DragLayer parent = (DragLayer) this.mLauncher.findViewById(R.id.drag_layer);
        int width = getPaddingLeft() + getPaddingRight() + this.mContent.getDesiredWidth();
        int height = getFolderHeight();
        float scale = parent.getDescendantRectRelativeToSelf(this.mFolderIcon, sTempRect);
        DeviceProfile grid = this.mLauncher.getDeviceProfile();
        int centerX = (int) (sTempRect.left + ((sTempRect.width() * scale) / 2.0f));
        int centerY = (int) (sTempRect.top + ((sTempRect.height() * scale) / 2.0f));
        int centeredLeft = centerX - (width / 2);
        int centeredTop = centerY - (height / 2);
        this.mLauncher.getWorkspace().getPageAreaRelativeToDragLayer(sTempRect);
        int left = Math.min(Math.max(sTempRect.left, centeredLeft), (sTempRect.left + sTempRect.width()) - width);
        int top = Math.min(Math.max(sTempRect.top, centeredTop), (sTempRect.top + sTempRect.height()) - height);
        if (grid.isPhone && grid.availableWidthPx - width < grid.iconSizePx) {
            left = (grid.availableWidthPx - width) / 2;
        } else if (width >= sTempRect.width()) {
            left = sTempRect.left + ((sTempRect.width() - width) / 2);
        }
        if (height >= sTempRect.height()) {
            top = sTempRect.top + ((sTempRect.height() - height) / 2);
        }
        Log.d("Launcher.Folder", "centerAboutIcon now, after sTempRect = " + sTempRect + "mFolderIcon = " + this.mFolderIcon);
        int folderPivotX = (width / 2) + (centeredLeft - left);
        int folderPivotY = (height / 2) + (centeredTop - top);
        setPivotX(folderPivotX);
        setPivotY(folderPivotY);
        this.mFolderIconPivotX = (int) (this.mFolderIcon.getMeasuredWidth() * ((folderPivotX * 1.0f) / width));
        this.mFolderIconPivotY = (int) (this.mFolderIcon.getMeasuredHeight() * ((folderPivotY * 1.0f) / height));
        lp.width = width;
        lp.height = height;
        lp.x = left;
        lp.y = top;
    }

    float getPivotXForIconAnimation() {
        return this.mFolderIconPivotX;
    }

    float getPivotYForIconAnimation() {
        return this.mFolderIconPivotY;
    }

    private int getContentAreaHeight() {
        DeviceProfile grid = this.mLauncher.getDeviceProfile();
        Rect workspacePadding = grid.getWorkspacePadding(this.mContent.mIsRtl);
        int maxContentAreaHeight = ((grid.availableHeightPx - workspacePadding.top) - workspacePadding.bottom) - this.mFooterHeight;
        int height = Math.min(maxContentAreaHeight, this.mContent.getDesiredHeight());
        return Math.max(height, 5);
    }

    private int getContentAreaWidth() {
        return Math.max(this.mContent.getDesiredWidth(), 5);
    }

    private int getFolderHeight() {
        return getFolderHeight(getContentAreaHeight());
    }

    private int getFolderHeight(int contentAreaHeight) {
        return getPaddingTop() + getPaddingBottom() + contentAreaHeight + this.mFooterHeight;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int contentWidth = getContentAreaWidth();
        int contentHeight = getContentAreaHeight();
        int contentAreaWidthSpec = View.MeasureSpec.makeMeasureSpec(contentWidth, 1073741824);
        int contentAreaHeightSpec = View.MeasureSpec.makeMeasureSpec(contentHeight, 1073741824);
        this.mContent.setFixedSize(contentWidth, contentHeight);
        this.mContentWrapper.measure(contentAreaWidthSpec, contentAreaHeightSpec);
        if (this.mContent.getChildCount() > 0) {
            int cellIconGap = (this.mContent.getPageAt(0).getCellWidth() - this.mLauncher.getDeviceProfile().iconSizePx) / 2;
            this.mFooter.setPadding(this.mContent.getPaddingLeft() + cellIconGap, this.mFooter.getPaddingTop(), this.mContent.getPaddingRight() + cellIconGap, this.mFooter.getPaddingBottom());
        }
        this.mFooter.measure(contentAreaWidthSpec, View.MeasureSpec.makeMeasureSpec(this.mFooterHeight, 1073741824));
        int folderWidth = getPaddingLeft() + getPaddingRight() + contentWidth;
        int folderHeight = getFolderHeight(contentHeight);
        setMeasuredDimension(folderWidth, folderHeight);
    }

    public void rearrangeChildren() {
        rearrangeChildren(-1);
    }

    public void rearrangeChildren(int itemCount) {
        ArrayList<View> views = getItemsInReadingOrder();
        this.mContent.arrangeChildren(views, Math.max(itemCount, views.size()));
        this.mItemsInvalidated = true;
    }

    public int getItemCount() {
        return this.mContent.getItemCount();
    }

    void replaceFolderWithFinalItem() {
        Runnable onCompleteRunnable = new Runnable() {
            @Override
            public void run() {
                int itemCount = Folder.this.mInfo.contents.size();
                if (itemCount > 1) {
                    return;
                }
                View view = null;
                if (itemCount == 1) {
                    CellLayout cellLayout = Folder.this.mLauncher.getCellLayout(Folder.this.mInfo.container, Folder.this.mInfo.screenId);
                    ShortcutInfo finalItem = Folder.this.mInfo.contents.remove(0);
                    View newIcon = Folder.this.mLauncher.createShortcut(cellLayout, finalItem);
                    LauncherModel.addOrMoveItemInDatabase(Folder.this.mLauncher, finalItem, Folder.this.mInfo.container, Folder.this.mInfo.screenId, Folder.this.mInfo.cellX, Folder.this.mInfo.cellY);
                    view = newIcon;
                }
                Folder.this.mLauncher.removeItem(Folder.this.mFolderIcon, Folder.this.mInfo, true);
                if (Folder.this.mFolderIcon instanceof DropTarget) {
                    Folder.this.mDragController.removeDropTarget((DropTarget) Folder.this.mFolderIcon);
                }
                if (view == null) {
                    return;
                }
                Folder.this.mLauncher.getWorkspace().addInScreenFromBind(view, Folder.this.mInfo.container, Folder.this.mInfo.screenId, Folder.this.mInfo.cellX, Folder.this.mInfo.cellY, Folder.this.mInfo.spanX, Folder.this.mInfo.spanY);
                view.requestFocus();
            }
        };
        View finalChild = this.mContent.getLastItem();
        if (finalChild != null) {
            this.mFolderIcon.performDestroyAnimation(finalChild, onCompleteRunnable);
        } else {
            onCompleteRunnable.run();
        }
        this.mDestroyed = true;
    }

    boolean isDestroyed() {
        return this.mDestroyed;
    }

    public void updateTextViewFocus() {
        View firstChild = this.mContent.getFirstItem();
        final View lastChild = this.mContent.getLastItem();
        if (firstChild == null || lastChild == null) {
            return;
        }
        this.mFolderName.setNextFocusDownId(lastChild.getId());
        this.mFolderName.setNextFocusRightId(lastChild.getId());
        this.mFolderName.setNextFocusLeftId(lastChild.getId());
        this.mFolderName.setNextFocusUpId(lastChild.getId());
        this.mFolderName.setNextFocusForwardId(firstChild.getId());
        setNextFocusDownId(firstChild.getId());
        setNextFocusRightId(firstChild.getId());
        setNextFocusLeftId(firstChild.getId());
        setNextFocusUpId(firstChild.getId());
        setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                boolean isShiftPlusTab = keyCode == 61 ? event.hasModifiers(1) : false;
                if (isShiftPlusTab && Folder.this.isFocused()) {
                    return lastChild.requestFocus();
                }
                return false;
            }
        });
    }

    @Override
    public void onDrop(DropTarget.DragObject d) {
        View currentDragView;
        Runnable cleanUpRunnable = null;
        if (d.dragSource != this.mLauncher.getWorkspace() && !(d.dragSource instanceof Folder)) {
            cleanUpRunnable = new Runnable() {
                @Override
                public void run() {
                    Folder.this.mLauncher.exitSpringLoadedDragModeDelayed(true, 300, null);
                }
            };
        }
        if (LauncherLog.DEBUG) {
            LauncherLog.d("Launcher.Folder", "onDrop: DragObject = " + d);
        }
        if (!this.mContent.rankOnCurrentPage(this.mEmptyCellRank)) {
            this.mTargetRank = getTargetRank(d, null);
            this.mReorderAlarmListener.onAlarm(this.mReorderAlarm);
            this.mOnScrollHintAlarm.cancelAlarm();
            this.mScrollPauseAlarm.cancelAlarm();
        }
        this.mContent.completePendingPageChanges();
        ShortcutInfo si = this.mCurrentDragInfo;
        if (this.mIsExternalDrag) {
            currentDragView = this.mContent.createAndAddViewForRank(si, this.mEmptyCellRank);
            LauncherModel.addOrMoveItemInDatabase(this.mLauncher, si, this.mInfo.id, 0L, si.cellX, si.cellY);
            if (d.dragSource != this) {
                updateItemLocationsInDatabaseBatch();
            }
            this.mIsExternalDrag = false;
        } else {
            currentDragView = this.mCurrentDragView;
            this.mContent.addViewForRank(currentDragView, si, this.mEmptyCellRank);
        }
        if (d.dragView.hasDrawn()) {
            float scaleX = getScaleX();
            float scaleY = getScaleY();
            setScaleX(1.0f);
            setScaleY(1.0f);
            this.mLauncher.getDragLayer().animateViewIntoPosition(d.dragView, currentDragView, cleanUpRunnable, null);
            setScaleX(scaleX);
            setScaleY(scaleY);
        } else {
            d.deferDragViewCleanupPostAnimation = false;
            currentDragView.setVisibility(0);
        }
        this.mItemsInvalidated = true;
        rearrangeChildren();
        this.mSuppressOnAdd = true;
        this.mInfo.add(si);
        this.mSuppressOnAdd = false;
        this.mCurrentDragInfo = null;
        this.mDragInProgress = false;
        if (this.mContent.getPageCount() <= 1) {
            return;
        }
        this.mInfo.setOption(4, true, this.mLauncher);
    }

    public void hideItem(ShortcutInfo info) {
        View v = getViewForInfo(info);
        v.setVisibility(4);
    }

    public void showItem(ShortcutInfo info) {
        View v = getViewForInfo(info);
        v.setVisibility(0);
    }

    @Override
    public void onAdd(ShortcutInfo item) {
        if (LauncherLog.DEBUG) {
            LauncherLog.d("Launcher.Folder", "onAdd item = " + item);
        }
        if (this.mSuppressOnAdd) {
            return;
        }
        this.mContent.createAndAddViewForRank(item, this.mContent.allocateRankForNewItem(item));
        this.mItemsInvalidated = true;
        LauncherModel.addOrMoveItemInDatabase(this.mLauncher, item, this.mInfo.id, 0L, item.cellX, item.cellY);
    }

    @Override
    public void onRemove(ShortcutInfo item) {
        if (LauncherLog.DEBUG) {
            LauncherLog.d("Launcher.Folder", "onRemove item = " + item);
        }
        this.mItemsInvalidated = true;
        if (item == this.mCurrentDragInfo) {
            return;
        }
        View v = getViewForInfo(item);
        this.mContent.removeItem(v);
        if (this.mState == 1) {
            this.mRearrangeOnClose = true;
        } else {
            rearrangeChildren();
        }
        if (getItemCount() > 1) {
            return;
        }
        if (this.mInfo.opened) {
            this.mLauncher.closeFolder(this, true);
        } else {
            replaceFolderWithFinalItem();
        }
    }

    private View getViewForInfo(final ShortcutInfo item) {
        return this.mContent.iterateOverItems(new Workspace.ItemOperator() {
            @Override
            public boolean evaluate(ItemInfo info, View view, View parent) {
                return info == item;
            }
        });
    }

    @Override
    public void onItemsChanged() {
        updateTextViewFocus();
    }

    @Override
    public void onTitleChanged(CharSequence title) {
    }

    public ArrayList<View> getItemsInReadingOrder() {
        if (this.mItemsInvalidated) {
            this.mItemsInReadingOrder.clear();
            this.mContent.iterateOverItems(new Workspace.ItemOperator() {
                @Override
                public boolean evaluate(ItemInfo info, View view, View parent) {
                    Folder.this.mItemsInReadingOrder.add(view);
                    return false;
                }
            });
            this.mItemsInvalidated = false;
        }
        return this.mItemsInReadingOrder;
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        if (v != this.mFolderName) {
            return;
        }
        if (hasFocus) {
            startEditingFolderName();
        } else {
            dismissEditingName();
        }
    }

    @Override
    public void getHitRectRelativeToDragLayer(Rect outRect) {
        getHitRect(outRect);
        outRect.left -= this.mScrollAreaOffset;
        outRect.right += this.mScrollAreaOffset;
    }

    @Override
    public void fillInLaunchSourceData(View v, Bundle sourceData) {
        Stats.LaunchSourceUtils.populateSourceDataFromAncestorProvider(this.mFolderIcon, sourceData);
        sourceData.putString("sub_container", "folder");
        sourceData.putInt("sub_container_page", this.mContent.getCurrentPage());
    }

    private class OnScrollHintListener implements OnAlarmListener {
        private final DropTarget.DragObject mDragObject;

        OnScrollHintListener(DropTarget.DragObject object) {
            this.mDragObject = object;
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

        OnScrollFinishedListener(DropTarget.DragObject object) {
            this.mDragObject = object;
        }

        @Override
        public void onAlarm(Alarm alarm) {
            Folder.this.onDragOver(this.mDragObject, 1);
        }
    }
}
