package com.android.launcher3.folder;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.util.Property;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import com.android.launcher3.Alarm;
import com.android.launcher3.AppInfo;
import com.android.launcher3.BubbleTextView;
import com.android.launcher3.CellLayout;
import com.android.launcher3.CheckLongPressHelper;
import com.android.launcher3.DeviceProfile;
import com.android.launcher3.DropTarget;
import com.android.launcher3.FolderInfo;
import com.android.launcher3.ItemInfo;
import com.android.launcher3.Launcher;
import com.android.launcher3.OnAlarmListener;
import com.android.launcher3.R;
import com.android.launcher3.ShortcutInfo;
import com.android.launcher3.SimpleOnStylusPressListener;
import com.android.launcher3.StylusEventHelper;
import com.android.launcher3.Utilities;
import com.android.launcher3.Workspace;
import com.android.launcher3.anim.Interpolators;
import com.android.launcher3.badge.BadgeRenderer;
import com.android.launcher3.badge.FolderBadgeInfo;
import com.android.launcher3.dragndrop.BaseItemDragListener;
import com.android.launcher3.dragndrop.DragLayer;
import com.android.launcher3.dragndrop.DragView;
import com.android.launcher3.touch.ItemClickHandler;
import com.android.launcher3.widget.PendingAddShortcutInfo;
import java.util.ArrayList;
import java.util.List;

/* loaded from: classes.dex */
public class FolderIcon extends FrameLayout implements FolderInfo.FolderListener {
    static final int DROP_IN_ANIMATION_DURATION = 400;
    private static final int ON_OPEN_DELAY = 800;
    public static final boolean SPRING_LOADING_ENABLED = true;
    boolean mAnimating;
    PreviewBackground mBackground;
    private boolean mBackgroundIsVisible;
    private FolderBadgeInfo mBadgeInfo;
    private BadgeRenderer mBadgeRenderer;
    private float mBadgeScale;
    private List<BubbleTextView> mCurrentPreviewItems;
    Folder mFolder;
    BubbleTextView mFolderName;
    private FolderInfo mInfo;
    Launcher mLauncher;
    private CheckLongPressHelper mLongPressHelper;
    OnAlarmListener mOnOpenListener;
    private Alarm mOpenAlarm;
    private PreviewItemManager mPreviewItemManager;
    ClippedFolderIconLayoutRule mPreviewLayoutRule;
    FolderIconPreviewVerifier mPreviewVerifier;
    private float mSlop;
    private StylusEventHelper mStylusEventHelper;
    private Rect mTempBounds;
    private Point mTempSpaceForBadgeOffset;
    private PreviewItemDrawingParams mTmpParams;
    static boolean sStaticValuesDirty = true;
    private static final Property<FolderIcon, Float> BADGE_SCALE_PROPERTY = new Property<FolderIcon, Float>(Float.TYPE, "badgeScale") { // from class: com.android.launcher3.folder.FolderIcon.1
        AnonymousClass1(Class cls, String str) {
            super(cls, str);
        }

        /* JADX DEBUG: Method merged with bridge method: get(Ljava/lang/Object;)Ljava/lang/Object; */
        @Override // android.util.Property
        public Float get(FolderIcon folderIcon) {
            return Float.valueOf(folderIcon.mBadgeScale);
        }

        /* JADX DEBUG: Method merged with bridge method: set(Ljava/lang/Object;Ljava/lang/Object;)V */
        @Override // android.util.Property
        public void set(FolderIcon folderIcon, Float f) {
            folderIcon.mBadgeScale = f.floatValue();
            folderIcon.invalidate();
        }
    };

    /* renamed from: com.android.launcher3.folder.FolderIcon$1 */
    class AnonymousClass1 extends Property<FolderIcon, Float> {
        AnonymousClass1(Class cls, String str) {
            super(cls, str);
        }

        /* JADX DEBUG: Method merged with bridge method: get(Ljava/lang/Object;)Ljava/lang/Object; */
        @Override // android.util.Property
        public Float get(FolderIcon folderIcon) {
            return Float.valueOf(folderIcon.mBadgeScale);
        }

        /* JADX DEBUG: Method merged with bridge method: set(Ljava/lang/Object;Ljava/lang/Object;)V */
        @Override // android.util.Property
        public void set(FolderIcon folderIcon, Float f) {
            folderIcon.mBadgeScale = f.floatValue();
            folderIcon.invalidate();
        }
    }

    public FolderIcon(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mBackground = new PreviewBackground();
        this.mBackgroundIsVisible = true;
        this.mTmpParams = new PreviewItemDrawingParams(0.0f, 0.0f, 0.0f, 0.0f);
        this.mCurrentPreviewItems = new ArrayList();
        this.mAnimating = false;
        this.mTempBounds = new Rect();
        this.mOpenAlarm = new Alarm();
        this.mBadgeInfo = new FolderBadgeInfo();
        this.mTempSpaceForBadgeOffset = new Point();
        this.mOnOpenListener = new OnAlarmListener() { // from class: com.android.launcher3.folder.FolderIcon.2
            AnonymousClass2() {
            }

            @Override // com.android.launcher3.OnAlarmListener
            public void onAlarm(Alarm alarm) {
                FolderIcon.this.mFolder.beginExternalDrag();
                FolderIcon.this.mFolder.animateOpen();
            }
        };
        init();
    }

    public FolderIcon(Context context) {
        super(context);
        this.mBackground = new PreviewBackground();
        this.mBackgroundIsVisible = true;
        this.mTmpParams = new PreviewItemDrawingParams(0.0f, 0.0f, 0.0f, 0.0f);
        this.mCurrentPreviewItems = new ArrayList();
        this.mAnimating = false;
        this.mTempBounds = new Rect();
        this.mOpenAlarm = new Alarm();
        this.mBadgeInfo = new FolderBadgeInfo();
        this.mTempSpaceForBadgeOffset = new Point();
        this.mOnOpenListener = new OnAlarmListener() { // from class: com.android.launcher3.folder.FolderIcon.2
            AnonymousClass2() {
            }

            @Override // com.android.launcher3.OnAlarmListener
            public void onAlarm(Alarm alarm) {
                FolderIcon.this.mFolder.beginExternalDrag();
                FolderIcon.this.mFolder.animateOpen();
            }
        };
        init();
    }

    private void init() {
        this.mLongPressHelper = new CheckLongPressHelper(this);
        this.mStylusEventHelper = new StylusEventHelper(new SimpleOnStylusPressListener(this), this);
        this.mPreviewLayoutRule = new ClippedFolderIconLayoutRule();
        this.mSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
        this.mPreviewItemManager = new PreviewItemManager(this);
    }

    public static FolderIcon fromXml(int i, Launcher launcher, ViewGroup viewGroup, FolderInfo folderInfo) {
        DeviceProfile deviceProfile = launcher.getDeviceProfile();
        FolderIcon folderIcon = (FolderIcon) LayoutInflater.from(viewGroup.getContext()).inflate(i, viewGroup, false);
        folderIcon.setClipToPadding(false);
        folderIcon.mFolderName = (BubbleTextView) folderIcon.findViewById(R.id.folder_icon_name);
        folderIcon.mFolderName.setText(folderInfo.title);
        folderIcon.mFolderName.setCompoundDrawablePadding(0);
        ((FrameLayout.LayoutParams) folderIcon.mFolderName.getLayoutParams()).topMargin = deviceProfile.iconSizePx + deviceProfile.iconDrawablePaddingPx;
        folderIcon.setTag(folderInfo);
        folderIcon.setOnClickListener(ItemClickHandler.INSTANCE);
        folderIcon.mInfo = folderInfo;
        folderIcon.mLauncher = launcher;
        folderIcon.mBadgeRenderer = launcher.getDeviceProfile().mBadgeRenderer;
        folderIcon.setContentDescription(launcher.getString(R.string.folder_name_format, new Object[]{folderInfo.title}));
        Folder folderFromXml = Folder.fromXml(launcher);
        folderFromXml.setDragController(launcher.getDragController());
        folderFromXml.setFolderIcon(folderIcon);
        folderFromXml.bind(folderInfo);
        folderIcon.setFolder(folderFromXml);
        folderIcon.setAccessibilityDelegate(launcher.getAccessibilityDelegate());
        folderInfo.addListener(folderIcon);
        folderIcon.setOnFocusChangeListener(launcher.mFocusHandler);
        return folderIcon;
    }

    @Override // android.view.View
    protected Parcelable onSaveInstanceState() {
        sStaticValuesDirty = true;
        return super.onSaveInstanceState();
    }

    public Folder getFolder() {
        return this.mFolder;
    }

    private void setFolder(Folder folder) {
        this.mFolder = folder;
        this.mPreviewVerifier = new FolderIconPreviewVerifier(this.mLauncher.getDeviceProfile().inv);
        updatePreviewItems(false);
    }

    private boolean willAcceptItem(ItemInfo itemInfo) {
        int i = itemInfo.itemType;
        return ((i != 0 && i != 1 && i != 6) || itemInfo == this.mInfo || this.mFolder.isOpen()) ? false : true;
    }

    public boolean acceptDrop(ItemInfo itemInfo) {
        return !this.mFolder.isDestroyed() && willAcceptItem(itemInfo);
    }

    public void addItem(ShortcutInfo shortcutInfo) {
        addItem(shortcutInfo, true);
    }

    public void addItem(ShortcutInfo shortcutInfo, boolean z) {
        this.mInfo.add(shortcutInfo, z);
    }

    public void removeItem(ShortcutInfo shortcutInfo, boolean z) {
        this.mInfo.remove(shortcutInfo, z);
    }

    public void onDragEnter(ItemInfo itemInfo) {
        if (this.mFolder.isDestroyed() || !willAcceptItem(itemInfo)) {
            return;
        }
        CellLayout.LayoutParams layoutParams = (CellLayout.LayoutParams) getLayoutParams();
        this.mBackground.animateToAccept((CellLayout) getParent().getParent(), layoutParams.cellX, layoutParams.cellY);
        this.mOpenAlarm.setOnAlarmListener(this.mOnOpenListener);
        if ((itemInfo instanceof AppInfo) || (itemInfo instanceof ShortcutInfo) || (itemInfo instanceof PendingAddShortcutInfo)) {
            this.mOpenAlarm.setAlarm(800L);
        }
    }

    /* renamed from: com.android.launcher3.folder.FolderIcon$2 */
    class AnonymousClass2 implements OnAlarmListener {
        AnonymousClass2() {
        }

        @Override // com.android.launcher3.OnAlarmListener
        public void onAlarm(Alarm alarm) {
            FolderIcon.this.mFolder.beginExternalDrag();
            FolderIcon.this.mFolder.animateOpen();
        }
    }

    public Drawable prepareCreateAnimation(View view) {
        return this.mPreviewItemManager.prepareCreateAnimation(view);
    }

    public void performCreateAnimation(ShortcutInfo shortcutInfo, View view, ShortcutInfo shortcutInfo2, DragView dragView, Rect rect, float f) {
        prepareCreateAnimation(view);
        addItem(shortcutInfo);
        this.mPreviewItemManager.createFirstItemAnimation(false, null).start();
        onDrop(shortcutInfo2, dragView, rect, f, 1, false);
    }

    public void performDestroyAnimation(Runnable runnable) {
        this.mPreviewItemManager.createFirstItemAnimation(true, runnable).start();
    }

    public void onDragExit() {
        this.mBackground.animateToRest();
        this.mOpenAlarm.cancelAlarm();
    }

    private void onDrop(ShortcutInfo shortcutInfo, DragView dragView, Rect rect, float f, int i, boolean z) {
        Rect rect2;
        float descendantRectRelativeToSelf;
        int i2;
        boolean z2;
        int i3;
        shortcutInfo.cellX = -1;
        shortcutInfo.cellY = -1;
        if (dragView != null) {
            DragLayer dragLayer = this.mLauncher.getDragLayer();
            Rect rect3 = new Rect();
            dragLayer.getViewRectRelativeToSelf(dragView, rect3);
            if (rect == null) {
                rect2 = new Rect();
                Workspace workspace = this.mLauncher.getWorkspace();
                workspace.setFinalTransitionTransform();
                float scaleX = getScaleX();
                float scaleY = getScaleY();
                setScaleX(1.0f);
                setScaleY(1.0f);
                descendantRectRelativeToSelf = dragLayer.getDescendantRectRelativeToSelf(this, rect2);
                setScaleX(scaleX);
                setScaleY(scaleY);
                workspace.resetTransitionTransform();
            } else {
                rect2 = rect;
                descendantRectRelativeToSelf = f;
            }
            int iMin = Math.min(4, i + 1);
            if (z || i >= 4) {
                ArrayList arrayList = new ArrayList(this.mCurrentPreviewItems);
                addItem(shortcutInfo, false);
                this.mCurrentPreviewItems.clear();
                this.mCurrentPreviewItems.addAll(getPreviewItems());
                if (!arrayList.equals(this.mCurrentPreviewItems)) {
                    int i4 = i;
                    for (int i5 = 0; i5 < this.mCurrentPreviewItems.size(); i5++) {
                        if (this.mCurrentPreviewItems.get(i5).getTag().equals(shortcutInfo)) {
                            i4 = i5;
                        }
                    }
                    this.mPreviewItemManager.hidePreviewItem(i4, true);
                    this.mPreviewItemManager.onDrop(arrayList, this.mCurrentPreviewItems, shortcutInfo);
                    i2 = i4;
                    z2 = true;
                } else {
                    removeItem(shortcutInfo, false);
                    i2 = i;
                    z2 = false;
                }
            } else {
                i2 = i;
                z2 = false;
            }
            if (!z2) {
                addItem(shortcutInfo);
            }
            int[] iArr = new int[2];
            float localCenterForIndex = getLocalCenterForIndex(i2, iMin, iArr);
            iArr[0] = Math.round(iArr[0] * descendantRectRelativeToSelf);
            iArr[1] = Math.round(iArr[1] * descendantRectRelativeToSelf);
            rect2.offset(iArr[0] - (dragView.getMeasuredWidth() / 2), iArr[1] - (dragView.getMeasuredHeight() / 2));
            float f2 = descendantRectRelativeToSelf * localCenterForIndex;
            int i6 = i2;
            dragLayer.animateView(dragView, rect3, rect2, i2 < 4 ? 0.5f : 0.0f, 1.0f, 1.0f, f2, f2, DROP_IN_ANIMATION_DURATION, Interpolators.DEACCEL_2, Interpolators.ACCEL_2, null, 0, null);
            this.mFolder.hideItem(shortcutInfo);
            if (z2) {
                i3 = i6;
            } else {
                i3 = i6;
                this.mPreviewItemManager.hidePreviewItem(i3, true);
            }
            postDelayed(new Runnable() { // from class: com.android.launcher3.folder.FolderIcon.3
                final /* synthetic */ int val$finalIndex;
                final /* synthetic */ ShortcutInfo val$item;

                AnonymousClass3(int i32, ShortcutInfo shortcutInfo2) {
                    i = i32;
                    shortcutInfo = shortcutInfo2;
                }

                @Override // java.lang.Runnable
                public void run() {
                    FolderIcon.this.mPreviewItemManager.hidePreviewItem(i, false);
                    FolderIcon.this.mFolder.showItem(shortcutInfo);
                    FolderIcon.this.invalidate();
                }
            }, 400L);
            return;
        }
        addItem(shortcutInfo2);
    }

    /* renamed from: com.android.launcher3.folder.FolderIcon$3 */
    class AnonymousClass3 implements Runnable {
        final /* synthetic */ int val$finalIndex;
        final /* synthetic */ ShortcutInfo val$item;

        AnonymousClass3(int i32, ShortcutInfo shortcutInfo2) {
            i = i32;
            shortcutInfo = shortcutInfo2;
        }

        @Override // java.lang.Runnable
        public void run() {
            FolderIcon.this.mPreviewItemManager.hidePreviewItem(i, false);
            FolderIcon.this.mFolder.showItem(shortcutInfo);
            FolderIcon.this.invalidate();
        }
    }

    public void onDrop(DropTarget.DragObject dragObject, boolean z) {
        ShortcutInfo shortcutInfo;
        if (dragObject.dragInfo instanceof AppInfo) {
            shortcutInfo = ((AppInfo) dragObject.dragInfo).makeShortcut();
        } else if (dragObject.dragSource instanceof BaseItemDragListener) {
            shortcutInfo = new ShortcutInfo((ShortcutInfo) dragObject.dragInfo);
        } else {
            shortcutInfo = (ShortcutInfo) dragObject.dragInfo;
        }
        ShortcutInfo shortcutInfo2 = shortcutInfo;
        this.mFolder.notifyDrop();
        onDrop(shortcutInfo2, dragObject.dragView, null, 1.0f, this.mInfo.contents.size(), z);
    }

    public void setBadgeInfo(FolderBadgeInfo folderBadgeInfo) {
        updateBadgeScale(this.mBadgeInfo.hasBadge(), folderBadgeInfo.hasBadge());
        this.mBadgeInfo = folderBadgeInfo;
    }

    public ClippedFolderIconLayoutRule getLayoutRule() {
        return this.mPreviewLayoutRule;
    }

    private void updateBadgeScale(boolean z, boolean z2) {
        float f = z2 ? 1.0f : 0.0f;
        if (!(z ^ z2) || !isShown()) {
            this.mBadgeScale = f;
            invalidate();
        } else {
            createBadgeScaleAnimator(f).start();
        }
    }

    public Animator createBadgeScaleAnimator(float... fArr) {
        return ObjectAnimator.ofFloat(this, BADGE_SCALE_PROPERTY, fArr);
    }

    public boolean hasBadge() {
        return this.mBadgeInfo != null && this.mBadgeInfo.hasBadge();
    }

    private float getLocalCenterForIndex(int i, int i2, int[] iArr) {
        this.mTmpParams = this.mPreviewItemManager.computePreviewItemDrawingParams(Math.min(4, i), i2, this.mTmpParams);
        this.mTmpParams.transX += this.mBackground.basePreviewOffsetX;
        this.mTmpParams.transY += this.mBackground.basePreviewOffsetY;
        float intrinsicIconSize = this.mPreviewItemManager.getIntrinsicIconSize();
        float f = this.mTmpParams.transX + ((this.mTmpParams.scale * intrinsicIconSize) / 2.0f);
        float f2 = this.mTmpParams.transY + ((this.mTmpParams.scale * intrinsicIconSize) / 2.0f);
        iArr[0] = Math.round(f);
        iArr[1] = Math.round(f2);
        return this.mTmpParams.scale;
    }

    public void setFolderBackground(PreviewBackground previewBackground) {
        this.mBackground = previewBackground;
        this.mBackground.setInvalidateDelegate(this);
    }

    public void setBackgroundVisible(boolean z) {
        this.mBackgroundIsVisible = z;
        invalidate();
    }

    public PreviewBackground getFolderBackground() {
        return this.mBackground;
    }

    public PreviewItemManager getPreviewItemManager() {
        return this.mPreviewItemManager;
    }

    @Override // android.view.ViewGroup, android.view.View
    protected void dispatchDraw(Canvas canvas) {
        int iSave;
        super.dispatchDraw(canvas);
        if (this.mBackgroundIsVisible) {
            this.mPreviewItemManager.recomputePreviewDrawingParams();
            if (!this.mBackground.drawingDelegated()) {
                this.mBackground.drawBackground(canvas);
            }
            if (this.mFolder == null) {
                return;
            }
            if (this.mFolder.getItemCount() != 0 || this.mAnimating) {
                if (canvas.isHardwareAccelerated()) {
                    iSave = canvas.saveLayer(0.0f, 0.0f, getWidth(), getHeight(), null);
                } else {
                    iSave = canvas.save();
                    canvas.clipPath(this.mBackground.getClipPath());
                }
                this.mPreviewItemManager.draw(canvas);
                if (canvas.isHardwareAccelerated()) {
                    this.mBackground.clipCanvasHardware(canvas);
                }
                canvas.restoreToCount(iSave);
                if (!this.mBackground.drawingDelegated()) {
                    this.mBackground.drawBackgroundStroke(canvas);
                }
                drawBadge(canvas);
            }
        }
    }

    public void drawBadge(Canvas canvas) {
        if ((this.mBadgeInfo != null && this.mBadgeInfo.hasBadge()) || this.mBadgeScale > 0.0f) {
            int offsetX = this.mBackground.getOffsetX();
            int offsetY = this.mBackground.getOffsetY();
            int i = (int) (this.mBackground.previewSize * this.mBackground.mScale);
            this.mTempBounds.set(offsetX, offsetY, offsetX + i, i + offsetY);
            float fMax = Math.max(0.0f, this.mBadgeScale - this.mBackground.getScaleProgress());
            this.mTempSpaceForBadgeOffset.set(getWidth() - this.mTempBounds.right, this.mTempBounds.top);
            this.mBadgeRenderer.draw(canvas, this.mBackground.getBadgeColor(), this.mTempBounds, fMax, this.mTempSpaceForBadgeOffset);
        }
    }

    public void setTextVisible(boolean z) {
        if (z) {
            this.mFolderName.setVisibility(0);
        } else {
            this.mFolderName.setVisibility(4);
        }
    }

    public boolean getTextVisible() {
        return this.mFolderName.getVisibility() == 0;
    }

    public List<BubbleTextView> getPreviewItems() {
        return getPreviewItemsOnPage(0);
    }

    public List<BubbleTextView> getPreviewItemsOnPage(int i) {
        this.mPreviewVerifier.setFolderInfo(this.mFolder.getInfo());
        ArrayList arrayList = new ArrayList();
        List<BubbleTextView> itemsOnPage = this.mFolder.getItemsOnPage(i);
        int size = itemsOnPage.size();
        for (int i2 = 0; i2 < size; i2++) {
            if (this.mPreviewVerifier.isItemInPreview(i, i2)) {
                arrayList.add(itemsOnPage.get(i2));
            }
            if (arrayList.size() == 4) {
                break;
            }
        }
        return arrayList;
    }

    @Override // android.view.View
    protected boolean verifyDrawable(@NonNull Drawable drawable) {
        return this.mPreviewItemManager.verifyDrawable(drawable) || super.verifyDrawable(drawable);
    }

    @Override // com.android.launcher3.FolderInfo.FolderListener
    public void onItemsChanged(boolean z) {
        updatePreviewItems(z);
        invalidate();
        requestLayout();
    }

    private void updatePreviewItems(boolean z) {
        this.mPreviewItemManager.updatePreviewItems(z);
        this.mCurrentPreviewItems.clear();
        this.mCurrentPreviewItems.addAll(getPreviewItems());
    }

    @Override // com.android.launcher3.FolderInfo.FolderListener
    public void prepareAutoUpdate() {
    }

    @Override // com.android.launcher3.FolderInfo.FolderListener
    public void onAdd(ShortcutInfo shortcutInfo, int i) {
        boolean zHasBadge = this.mBadgeInfo.hasBadge();
        this.mBadgeInfo.addBadgeInfo(this.mLauncher.getBadgeInfoForItem(shortcutInfo));
        updateBadgeScale(zHasBadge, this.mBadgeInfo.hasBadge());
        invalidate();
        requestLayout();
    }

    @Override // com.android.launcher3.FolderInfo.FolderListener
    public void onRemove(ShortcutInfo shortcutInfo) {
        boolean zHasBadge = this.mBadgeInfo.hasBadge();
        this.mBadgeInfo.subtractBadgeInfo(this.mLauncher.getBadgeInfoForItem(shortcutInfo));
        updateBadgeScale(zHasBadge, this.mBadgeInfo.hasBadge());
        invalidate();
        requestLayout();
    }

    @Override // com.android.launcher3.FolderInfo.FolderListener
    public void onTitleChanged(CharSequence charSequence) {
        this.mFolderName.setText(charSequence);
        setContentDescription(getContext().getString(R.string.folder_name_format, charSequence));
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    @Override // android.view.View
    public boolean onTouchEvent(MotionEvent motionEvent) {
        boolean zOnTouchEvent = super.onTouchEvent(motionEvent);
        if (this.mStylusEventHelper.onMotionEvent(motionEvent)) {
            this.mLongPressHelper.cancelLongPress();
            return true;
        }
        switch (motionEvent.getAction()) {
            case 0:
                this.mLongPressHelper.postCheckForLongPress();
                return zOnTouchEvent;
            case 1:
            case 3:
                this.mLongPressHelper.cancelLongPress();
                return zOnTouchEvent;
            case 2:
                if (!Utilities.pointInView(this, motionEvent.getX(), motionEvent.getY(), this.mSlop)) {
                    this.mLongPressHelper.cancelLongPress();
                }
                return zOnTouchEvent;
            default:
                return zOnTouchEvent;
        }
    }

    @Override // android.view.View
    public void cancelLongPress() {
        super.cancelLongPress();
        this.mLongPressHelper.cancelLongPress();
    }

    public void removeListeners() {
        this.mInfo.removeListener(this);
        this.mInfo.removeListener(this.mFolder);
    }

    public void clearLeaveBehindIfExists() {
        ((CellLayout.LayoutParams) getLayoutParams()).canReorder = true;
        if (this.mInfo.container == -101) {
            ((CellLayout) getParent().getParent()).clearFolderLeaveBehind();
        }
    }

    public void drawLeaveBehindIfExists() {
        CellLayout.LayoutParams layoutParams = (CellLayout.LayoutParams) getLayoutParams();
        layoutParams.canReorder = false;
        if (this.mInfo.container == -101) {
            ((CellLayout) getParent().getParent()).setFolderLeaveBehindCell(layoutParams.cellX, layoutParams.cellY);
        }
    }

    public void onFolderClose(int i) {
        this.mPreviewItemManager.onFolderClose(i);
    }
}
