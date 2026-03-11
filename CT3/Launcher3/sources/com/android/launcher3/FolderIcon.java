package com.android.launcher3;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Looper;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.launcher3.CellLayout;
import com.android.launcher3.DropTarget;
import com.android.launcher3.FolderInfo;
import com.android.launcher3.compat.PackageInstallerCompat;
import com.mediatek.launcher3.LauncherLog;
import java.util.ArrayList;

public class FolderIcon extends FrameLayout implements FolderInfo.FolderListener {
    PreviewItemDrawingParams mAnimParams;
    boolean mAnimating;
    private int mAvailableSpaceInPreview;
    private float mBaselineIconScale;
    private int mBaselineIconSize;
    ItemInfo mDragInfo;
    Folder mFolder;
    BubbleTextView mFolderName;
    FolderRingAnimator mFolderRingAnimator;
    ArrayList<ShortcutInfo> mHiddenItems;
    private FolderInfo mInfo;
    private int mIntrinsicIconSize;
    Launcher mLauncher;
    private CheckLongPressHelper mLongPressHelper;
    private float mMaxPerspectiveShift;
    private Rect mOldBounds;
    OnAlarmListener mOnOpenListener;
    private Alarm mOpenAlarm;
    private PreviewItemDrawingParams mParams;
    ImageView mPreviewBackground;
    private int mPreviewOffsetX;
    private int mPreviewOffsetY;
    private float mSlop;
    private StylusEventHelper mStylusEventHelper;
    private int mTotalWidth;
    static boolean sStaticValuesDirty = true;
    public static Drawable sSharedFolderLeaveBehind = null;

    public FolderIcon(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mFolderRingAnimator = null;
        this.mTotalWidth = -1;
        this.mAnimating = false;
        this.mOldBounds = new Rect();
        this.mParams = new PreviewItemDrawingParams(0.0f, 0.0f, 0.0f, 0.0f);
        this.mAnimParams = new PreviewItemDrawingParams(0.0f, 0.0f, 0.0f, 0.0f);
        this.mHiddenItems = new ArrayList<>();
        this.mOpenAlarm = new Alarm();
        this.mOnOpenListener = new OnAlarmListener() {
            @Override
            public void onAlarm(Alarm alarm) {
                ShortcutInfo item;
                if (FolderIcon.this.mDragInfo instanceof AppInfo) {
                    item = ((AppInfo) FolderIcon.this.mDragInfo).makeShortcut();
                    item.spanX = 1;
                    item.spanY = 1;
                } else {
                    if (FolderIcon.this.mDragInfo instanceof PendingAddItemInfo) {
                        if (LauncherLog.DEBUG) {
                            LauncherLog.d("FolderIcon", "onAlarm: mDragInfo instanceof PendingAddItemInfo");
                            return;
                        }
                        return;
                    }
                    item = (ShortcutInfo) FolderIcon.this.mDragInfo;
                }
                FolderIcon.this.mFolder.beginExternalDrag(item);
                FolderIcon.this.mLauncher.openFolder(FolderIcon.this);
            }
        };
        init();
    }

    public FolderIcon(Context context) {
        super(context);
        this.mFolderRingAnimator = null;
        this.mTotalWidth = -1;
        this.mAnimating = false;
        this.mOldBounds = new Rect();
        this.mParams = new PreviewItemDrawingParams(0.0f, 0.0f, 0.0f, 0.0f);
        this.mAnimParams = new PreviewItemDrawingParams(0.0f, 0.0f, 0.0f, 0.0f);
        this.mHiddenItems = new ArrayList<>();
        this.mOpenAlarm = new Alarm();
        this.mOnOpenListener = new OnAlarmListener() {
            @Override
            public void onAlarm(Alarm alarm) {
                ShortcutInfo item;
                if (FolderIcon.this.mDragInfo instanceof AppInfo) {
                    item = ((AppInfo) FolderIcon.this.mDragInfo).makeShortcut();
                    item.spanX = 1;
                    item.spanY = 1;
                } else {
                    if (FolderIcon.this.mDragInfo instanceof PendingAddItemInfo) {
                        if (LauncherLog.DEBUG) {
                            LauncherLog.d("FolderIcon", "onAlarm: mDragInfo instanceof PendingAddItemInfo");
                            return;
                        }
                        return;
                    }
                    item = (ShortcutInfo) FolderIcon.this.mDragInfo;
                }
                FolderIcon.this.mFolder.beginExternalDrag(item);
                FolderIcon.this.mLauncher.openFolder(FolderIcon.this);
            }
        };
        init();
    }

    private void init() {
        this.mLongPressHelper = new CheckLongPressHelper(this);
        this.mStylusEventHelper = new StylusEventHelper(this);
        setAccessibilityDelegate(LauncherAppState.getInstance().getAccessibilityDelegate());
    }

    static FolderIcon fromXml(int resId, Launcher launcher, ViewGroup group, FolderInfo folderInfo, IconCache iconCache) {
        return fromXml(resId, launcher, group, folderInfo, iconCache, false);
    }

    static FolderIcon fromXml(int resId, Launcher launcher, ViewGroup group, FolderInfo folderInfo, IconCache iconCache, boolean fromAllApp) {
        DeviceProfile grid = launcher.getDeviceProfile();
        FolderIcon icon = (FolderIcon) LayoutInflater.from(launcher).inflate(resId, group, false);
        icon.setClipToPadding(false);
        icon.mFolderName = (BubbleTextView) icon.findViewById(R.id.folder_icon_name);
        icon.mFolderName.setText(folderInfo.title);
        icon.mFolderName.setCompoundDrawablePadding(0);
        ((FrameLayout.LayoutParams) icon.mFolderName.getLayoutParams()).topMargin = grid.iconSizePx + grid.iconDrawablePaddingPx;
        icon.mPreviewBackground = (ImageView) icon.findViewById(R.id.preview_background);
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) icon.mPreviewBackground.getLayoutParams();
        lp.topMargin = grid.folderBackgroundOffset;
        lp.width = grid.folderIconSizePx;
        lp.height = grid.folderIconSizePx;
        icon.setTag(folderInfo);
        icon.setOnClickListener(launcher);
        icon.mInfo = folderInfo;
        icon.mLauncher = launcher;
        icon.setContentDescription(String.format(launcher.getString(R.string.folder_name_format), folderInfo.title));
        Folder folder = Folder.fromXml(launcher);
        folder.setDragController(launcher.getDragController());
        folder.setFolderIcon(icon);
        folder.bind(folderInfo);
        icon.mFolder = folder;
        icon.mFolderRingAnimator = new FolderRingAnimator(launcher, icon);
        folderInfo.addListener(icon);
        icon.setOnFocusChangeListener(launcher.mFocusHandler);
        return icon;
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        sStaticValuesDirty = true;
        return super.onSaveInstanceState();
    }

    public static class FolderRingAnimator {
        private ValueAnimator mAcceptAnimator;
        CellLayout mCellLayout;
        public int mCellX;
        public int mCellY;
        public FolderIcon mFolderIcon;
        public float mInnerRingSize;
        private ValueAnimator mNeutralAnimator;
        public float mOuterRingSize;
        public static Drawable sSharedOuterRingDrawable = null;
        public static Drawable sSharedInnerRingDrawable = null;
        public static int sPreviewSize = -1;
        public static int sPreviewPadding = -1;

        public FolderRingAnimator(Launcher launcher, FolderIcon folderIcon) {
            this.mFolderIcon = null;
            this.mFolderIcon = folderIcon;
            Resources res = launcher.getResources();
            if (!FolderIcon.sStaticValuesDirty) {
                return;
            }
            if (Looper.myLooper() != Looper.getMainLooper()) {
                throw new RuntimeException("FolderRingAnimator loading drawables on non-UI thread " + Thread.currentThread());
            }
            DeviceProfile grid = launcher.getDeviceProfile();
            sPreviewSize = grid.folderIconSizePx;
            sPreviewPadding = res.getDimensionPixelSize(R.dimen.folder_preview_padding);
            sSharedOuterRingDrawable = res.getDrawable(R.drawable.portal_ring_outer);
            sSharedInnerRingDrawable = res.getDrawable(R.drawable.portal_ring_inner_nolip);
            FolderIcon.sSharedFolderLeaveBehind = res.getDrawable(R.drawable.portal_ring_rest);
            FolderIcon.sStaticValuesDirty = false;
        }

        public void animateToAcceptState() {
            if (this.mNeutralAnimator != null) {
                this.mNeutralAnimator.cancel();
            }
            this.mAcceptAnimator = LauncherAnimUtils.ofFloat(this.mCellLayout, 0.0f, 1.0f);
            this.mAcceptAnimator.setDuration(100L);
            final int previewSize = sPreviewSize;
            this.mAcceptAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    float percent = ((Float) animation.getAnimatedValue()).floatValue();
                    FolderRingAnimator.this.mOuterRingSize = ((0.3f * percent) + 1.0f) * previewSize;
                    FolderRingAnimator.this.mInnerRingSize = ((0.15f * percent) + 1.0f) * previewSize;
                    if (FolderRingAnimator.this.mCellLayout == null) {
                        return;
                    }
                    FolderRingAnimator.this.mCellLayout.invalidate();
                }
            });
            this.mAcceptAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(Animator animation) {
                    if (FolderRingAnimator.this.mFolderIcon == null) {
                        return;
                    }
                    FolderRingAnimator.this.mFolderIcon.mPreviewBackground.setVisibility(4);
                }
            });
            this.mAcceptAnimator.start();
        }

        public void animateToNaturalState() {
            if (this.mAcceptAnimator != null) {
                this.mAcceptAnimator.cancel();
            }
            this.mNeutralAnimator = LauncherAnimUtils.ofFloat(this.mCellLayout, 0.0f, 1.0f);
            this.mNeutralAnimator.setDuration(100L);
            final int previewSize = sPreviewSize;
            this.mNeutralAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    float percent = ((Float) animation.getAnimatedValue()).floatValue();
                    FolderRingAnimator.this.mOuterRingSize = (((1.0f - percent) * 0.3f) + 1.0f) * previewSize;
                    FolderRingAnimator.this.mInnerRingSize = (((1.0f - percent) * 0.15f) + 1.0f) * previewSize;
                    if (FolderRingAnimator.this.mCellLayout == null) {
                        return;
                    }
                    FolderRingAnimator.this.mCellLayout.invalidate();
                }
            });
            this.mNeutralAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    if (FolderRingAnimator.this.mCellLayout != null) {
                        FolderRingAnimator.this.mCellLayout.hideFolderAccept(FolderRingAnimator.this);
                    }
                    if (FolderRingAnimator.this.mFolderIcon == null) {
                        return;
                    }
                    FolderRingAnimator.this.mFolderIcon.mPreviewBackground.setVisibility(0);
                }
            });
            this.mNeutralAnimator.start();
        }

        public void setCell(int x, int y) {
            this.mCellX = x;
            this.mCellY = y;
        }

        public void setCellLayout(CellLayout layout) {
            this.mCellLayout = layout;
        }

        public float getOuterRingSize() {
            return this.mOuterRingSize;
        }

        public float getInnerRingSize() {
            return this.mInnerRingSize;
        }
    }

    public Folder getFolder() {
        return this.mFolder;
    }

    FolderInfo getFolderInfo() {
        return this.mInfo;
    }

    private boolean willAcceptItem(ItemInfo item) {
        int itemType = item.itemType;
        return ((itemType != 0 && itemType != 1) || this.mFolder.isFull() || item == this.mInfo || this.mInfo.opened) ? false : true;
    }

    public boolean acceptDrop(Object dragInfo) {
        ItemInfo item = (ItemInfo) dragInfo;
        if (this.mFolder.isDestroyed()) {
            return false;
        }
        return willAcceptItem(item);
    }

    public void addItem(ShortcutInfo item) {
        this.mInfo.add(item);
    }

    public void onDragEnter(Object dragInfo) {
        if (this.mFolder.isDestroyed() || !willAcceptItem((ItemInfo) dragInfo)) {
            return;
        }
        CellLayout.LayoutParams lp = (CellLayout.LayoutParams) getLayoutParams();
        CellLayout layout = (CellLayout) getParent().getParent();
        this.mFolderRingAnimator.setCell(lp.cellX, lp.cellY);
        this.mFolderRingAnimator.setCellLayout(layout);
        this.mFolderRingAnimator.animateToAcceptState();
        layout.showFolderAccept(this.mFolderRingAnimator);
        this.mOpenAlarm.setOnAlarmListener(this.mOnOpenListener);
        if ((dragInfo instanceof AppInfo) || (dragInfo instanceof ShortcutInfo)) {
            this.mOpenAlarm.setAlarm(800L);
        }
        this.mDragInfo = (ItemInfo) dragInfo;
    }

    public void performCreateAnimation(ShortcutInfo destInfo, View destView, ShortcutInfo srcInfo, DragView srcView, Rect dstRect, float scaleRelativeToDragLayer, Runnable postAnimationRunnable) {
        Drawable animateDrawable = getTopDrawable((TextView) destView);
        computePreviewDrawingParams(animateDrawable.getIntrinsicWidth(), destView.getMeasuredWidth());
        animateFirstItem(animateDrawable, 350, false, null);
        addItem(destInfo);
        onDrop(srcInfo, srcView, dstRect, scaleRelativeToDragLayer, 1, postAnimationRunnable, null);
    }

    public void performDestroyAnimation(View finalView, Runnable onCompleteRunnable) {
        Drawable animateDrawable = getTopDrawable((TextView) finalView);
        computePreviewDrawingParams(animateDrawable.getIntrinsicWidth(), finalView.getMeasuredWidth());
        animateFirstItem(animateDrawable, 200, true, onCompleteRunnable);
    }

    public void onDragExit(Object dragInfo) {
        onDragExit();
    }

    public void onDragExit() {
        this.mFolderRingAnimator.animateToNaturalState();
        this.mOpenAlarm.cancelAlarm();
    }

    private void onDrop(final ShortcutInfo item, DragView animateView, Rect finalRect, float scaleRelativeToDragLayer, int index, Runnable postAnimationRunnable, DropTarget.DragObject d) {
        if (LauncherLog.DEBUG) {
            LauncherLog.d("FolderIcon", "onDrop: item = " + item + ", animateView = " + animateView + ", finalRect = " + finalRect + ", scaleRelativeToDragLayer = " + scaleRelativeToDragLayer + ", index = " + index + ", d = " + d);
        }
        item.cellX = -1;
        item.cellY = -1;
        if (animateView == null) {
            addItem(item);
            return;
        }
        DragLayer dragLayer = this.mLauncher.getDragLayer();
        Rect from = new Rect();
        dragLayer.getViewRectRelativeToSelf(animateView, from);
        Rect to = finalRect;
        if (finalRect == null) {
            to = new Rect();
            Workspace workspace = this.mLauncher.getWorkspace();
            workspace.setFinalTransitionTransform((CellLayout) getParent().getParent());
            float scaleX = getScaleX();
            float scaleY = getScaleY();
            setScaleX(1.0f);
            setScaleY(1.0f);
            scaleRelativeToDragLayer = dragLayer.getDescendantRectRelativeToSelf(this, to);
            setScaleX(scaleX);
            setScaleY(scaleY);
            workspace.resetTransitionTransform((CellLayout) getParent().getParent());
        }
        int[] center = new int[2];
        float scale = getLocalCenterForIndex(index, center);
        center[0] = Math.round(center[0] * scaleRelativeToDragLayer);
        center[1] = Math.round(center[1] * scaleRelativeToDragLayer);
        to.offset(center[0] - (animateView.getMeasuredWidth() / 2), center[1] - (animateView.getMeasuredHeight() / 2));
        float finalAlpha = index < 3 ? 0.5f : 0.0f;
        float finalScale = scale * scaleRelativeToDragLayer;
        dragLayer.animateView(animateView, from, to, finalAlpha, 1.0f, 1.0f, finalScale, finalScale, 400, new DecelerateInterpolator(2.0f), new AccelerateInterpolator(2.0f), postAnimationRunnable, 0, null);
        addItem(item);
        this.mHiddenItems.add(item);
        this.mFolder.hideItem(item);
        postDelayed(new Runnable() {
            @Override
            public void run() {
                FolderIcon.this.mHiddenItems.remove(item);
                FolderIcon.this.mFolder.showItem(item);
                FolderIcon.this.invalidate();
            }
        }, 400L);
    }

    public void onDrop(DropTarget.DragObject d) {
        ShortcutInfo item;
        if (LauncherLog.DEBUG) {
            LauncherLog.d("FolderIcon", "onDrop: DragObject = " + d);
        }
        if (d.dragInfo instanceof AppInfo) {
            item = ((AppInfo) d.dragInfo).makeShortcut();
        } else {
            item = (ShortcutInfo) d.dragInfo;
        }
        this.mFolder.notifyDrop();
        onDrop(item, d.dragView, null, 1.0f, this.mInfo.contents.size(), d.postAnimationRunnable, d);
    }

    private void computePreviewDrawingParams(int drawableSize, int totalSize) {
        if (this.mIntrinsicIconSize == drawableSize && this.mTotalWidth == totalSize) {
            return;
        }
        DeviceProfile grid = this.mLauncher.getDeviceProfile();
        this.mIntrinsicIconSize = drawableSize;
        this.mTotalWidth = totalSize;
        int previewSize = this.mPreviewBackground.getLayoutParams().height;
        int previewPadding = FolderRingAnimator.sPreviewPadding;
        this.mAvailableSpaceInPreview = previewSize - (previewPadding * 2);
        int adjustedAvailableSpace = (int) ((this.mAvailableSpaceInPreview / 2) * 1.8f);
        int unscaledHeight = (int) (this.mIntrinsicIconSize * 1.1800001f);
        this.mBaselineIconScale = (adjustedAvailableSpace * 1.0f) / unscaledHeight;
        this.mBaselineIconSize = (int) (this.mIntrinsicIconSize * this.mBaselineIconScale);
        this.mMaxPerspectiveShift = this.mBaselineIconSize * 0.18f;
        this.mPreviewOffsetX = (this.mTotalWidth - this.mAvailableSpaceInPreview) / 2;
        this.mPreviewOffsetY = grid.folderBackgroundOffset + previewPadding;
    }

    private void computePreviewDrawingParams(Drawable d) {
        computePreviewDrawingParams(d.getIntrinsicWidth(), getMeasuredWidth());
    }

    class PreviewItemDrawingParams {
        Drawable drawable;
        float overlayAlpha;
        float scale;
        float transX;
        float transY;

        PreviewItemDrawingParams(float transX, float transY, float scale, float overlayAlpha) {
            this.transX = transX;
            this.transY = transY;
            this.scale = scale;
            this.overlayAlpha = overlayAlpha;
        }
    }

    private float getLocalCenterForIndex(int index, int[] center) {
        this.mParams = computePreviewItemDrawingParams(Math.min(3, index), this.mParams);
        this.mParams.transX += this.mPreviewOffsetX;
        this.mParams.transY += this.mPreviewOffsetY;
        float offsetX = this.mParams.transX + ((this.mParams.scale * this.mIntrinsicIconSize) / 2.0f);
        float offsetY = this.mParams.transY + ((this.mParams.scale * this.mIntrinsicIconSize) / 2.0f);
        center[0] = Math.round(offsetX);
        center[1] = Math.round(offsetY);
        return this.mParams.scale;
    }

    private PreviewItemDrawingParams computePreviewItemDrawingParams(int index, PreviewItemDrawingParams params) {
        float r = (((3 - index) - 1) * 1.0f) / 2.0f;
        float scale = 1.0f - ((1.0f - r) * 0.35f);
        float offset = (1.0f - r) * this.mMaxPerspectiveShift;
        float scaledSize = scale * this.mBaselineIconSize;
        float scaleOffsetCorrection = (1.0f - scale) * this.mBaselineIconSize;
        float transY = (this.mAvailableSpaceInPreview - ((offset + scaledSize) + scaleOffsetCorrection)) + getPaddingTop();
        float transX = (this.mAvailableSpaceInPreview - scaledSize) / 2.0f;
        float totalScale = this.mBaselineIconScale * scale;
        float overlayAlpha = ((1.0f - r) * 80.0f) / 255.0f;
        if (params == null) {
            return new PreviewItemDrawingParams(transX, transY, totalScale, overlayAlpha);
        }
        params.transX = transX;
        params.transY = transY;
        params.scale = totalScale;
        params.overlayAlpha = overlayAlpha;
        return params;
    }

    private void drawPreviewItem(Canvas canvas, PreviewItemDrawingParams params) {
        canvas.save();
        canvas.translate(params.transX + this.mPreviewOffsetX, params.transY + this.mPreviewOffsetY);
        canvas.scale(params.scale, params.scale);
        Drawable d = params.drawable;
        if (d != null) {
            this.mOldBounds.set(d.getBounds());
            d.setBounds(0, 0, this.mIntrinsicIconSize, this.mIntrinsicIconSize);
            if (d instanceof FastBitmapDrawable) {
                FastBitmapDrawable fd = (FastBitmapDrawable) d;
                float oldBrightness = fd.getBrightness();
                fd.setBrightness(params.overlayAlpha);
                d.draw(canvas);
                fd.setBrightness(oldBrightness);
            } else {
                d.setColorFilter(Color.argb((int) (params.overlayAlpha * 255.0f), 255, 255, 255), PorterDuff.Mode.SRC_ATOP);
                d.draw(canvas);
                d.clearColorFilter();
            }
            d.setBounds(this.mOldBounds);
        }
        canvas.restore();
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        if (this.mFolder == null) {
            return;
        }
        if (this.mFolder.getItemCount() == 0 && !this.mAnimating) {
            return;
        }
        ArrayList<View> items = this.mFolder.getItemsInReadingOrder();
        if (this.mAnimating) {
            computePreviewDrawingParams(this.mAnimParams.drawable);
        } else {
            Drawable d = getTopDrawable((TextView) items.get(0));
            computePreviewDrawingParams(d);
        }
        int nItemsInPreview = Math.min(items.size(), 3);
        if (!this.mAnimating) {
            for (int i = nItemsInPreview - 1; i >= 0; i--) {
                TextView v = (TextView) items.get(i);
                if (!this.mHiddenItems.contains(v.getTag())) {
                    Drawable d2 = getTopDrawable(v);
                    this.mParams = computePreviewItemDrawingParams(i, this.mParams);
                    this.mParams.drawable = d2;
                    drawPreviewItem(canvas, this.mParams);
                }
            }
            return;
        }
        drawPreviewItem(canvas, this.mAnimParams);
    }

    private Drawable getTopDrawable(TextView v) {
        Drawable d = v.getCompoundDrawables()[1];
        return d instanceof PreloadIconDrawable ? ((PreloadIconDrawable) d).mIcon : d;
    }

    private void animateFirstItem(Drawable d, int duration, final boolean reverse, final Runnable onCompleteRunnable) {
        final PreviewItemDrawingParams finalParams = computePreviewItemDrawingParams(0, null);
        float iconSize = this.mLauncher.getDeviceProfile().iconSizePx;
        final float scale0 = iconSize / d.getIntrinsicWidth();
        final float transX0 = (this.mAvailableSpaceInPreview - iconSize) / 2.0f;
        final float transY0 = ((this.mAvailableSpaceInPreview - iconSize) / 2.0f) + getPaddingTop();
        this.mAnimParams.drawable = d;
        ValueAnimator va = LauncherAnimUtils.ofFloat(this, 0.0f, 1.0f);
        va.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float progress = ((Float) animation.getAnimatedValue()).floatValue();
                if (reverse) {
                    progress = 1.0f - progress;
                    FolderIcon.this.mPreviewBackground.setAlpha(progress);
                }
                FolderIcon.this.mAnimParams.transX = transX0 + ((finalParams.transX - transX0) * progress);
                FolderIcon.this.mAnimParams.transY = transY0 + ((finalParams.transY - transY0) * progress);
                FolderIcon.this.mAnimParams.scale = scale0 + ((finalParams.scale - scale0) * progress);
                FolderIcon.this.invalidate();
            }
        });
        va.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                FolderIcon.this.mAnimating = true;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                FolderIcon.this.mAnimating = false;
                if (onCompleteRunnable == null) {
                    return;
                }
                onCompleteRunnable.run();
            }
        });
        va.setDuration(duration);
        va.start();
    }

    public void setTextVisible(boolean visible) {
        if (visible) {
            this.mFolderName.setVisibility(0);
        } else {
            this.mFolderName.setVisibility(4);
        }
    }

    public boolean getTextVisible() {
        return this.mFolderName.getVisibility() == 0;
    }

    @Override
    public void onItemsChanged() {
        invalidate();
        requestLayout();
    }

    @Override
    public void onAdd(ShortcutInfo item) {
        if (LauncherLog.DEBUG) {
            LauncherLog.d("FolderIcon", "onAdd item = " + item);
        }
        invalidate();
        requestLayout();
    }

    @Override
    public void onRemove(ShortcutInfo item) {
        if (LauncherLog.DEBUG) {
            LauncherLog.d("FolderIcon", "onRemove item = " + item);
        }
        invalidate();
        requestLayout();
    }

    @Override
    public void onTitleChanged(CharSequence title) {
        this.mFolderName.setText(title);
        setContentDescription(String.format(getContext().getString(R.string.folder_name_format), title));
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean result = super.onTouchEvent(event);
        if (this.mStylusEventHelper.checkAndPerformStylusEvent(event)) {
            this.mLongPressHelper.cancelLongPress();
            return true;
        }
        switch (event.getAction()) {
            case PackageInstallerCompat.STATUS_INSTALLED:
                this.mLongPressHelper.postCheckForLongPress();
                return result;
            case PackageInstallerCompat.STATUS_INSTALLING:
            case 3:
                this.mLongPressHelper.cancelLongPress();
                return result;
            case PackageInstallerCompat.STATUS_FAILED:
                if (!Utilities.pointInView(this, event.getX(), event.getY(), this.mSlop)) {
                    this.mLongPressHelper.cancelLongPress();
                }
                return result;
            default:
                return result;
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        this.mSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
    }

    @Override
    public void cancelLongPress() {
        super.cancelLongPress();
        this.mLongPressHelper.cancelLongPress();
    }
}
