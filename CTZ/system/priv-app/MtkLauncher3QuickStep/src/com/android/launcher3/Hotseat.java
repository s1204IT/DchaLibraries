package com.android.launcher3;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewDebug;
import android.widget.FrameLayout;
import com.android.launcher3.logging.UserEventDispatcher;
import com.android.launcher3.userevent.nano.LauncherLogProto;

/* loaded from: classes.dex */
public class Hotseat extends FrameLayout implements UserEventDispatcher.LogContainerProvider, Insettable {
    private CellLayout mContent;

    @ViewDebug.ExportedProperty(category = "launcher")
    private boolean mHasVerticalHotseat;
    private final Launcher mLauncher;

    public Hotseat(Context context) {
        this(context, null);
    }

    public Hotseat(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public Hotseat(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mLauncher = Launcher.getLauncher(context);
    }

    public CellLayout getLayout() {
        return this.mContent;
    }

    int getOrderInHotseat(int i, int i2) {
        return this.mHasVerticalHotseat ? (this.mContent.getCountY() - i2) - 1 : i;
    }

    int getCellXFromOrder(int i) {
        if (this.mHasVerticalHotseat) {
            return 0;
        }
        return i;
    }

    int getCellYFromOrder(int i) {
        if (this.mHasVerticalHotseat) {
            return this.mContent.getCountY() - (i + 1);
        }
        return 0;
    }

    @Override // android.view.View
    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mContent = (CellLayout) findViewById(R.id.layout);
    }

    void resetLayout(boolean z) {
        this.mContent.removeAllViewsInLayout();
        this.mHasVerticalHotseat = z;
        InvariantDeviceProfile invariantDeviceProfile = this.mLauncher.getDeviceProfile().inv;
        if (z) {
            this.mContent.setGridSize(1, invariantDeviceProfile.numHotseatIcons);
        } else {
            this.mContent.setGridSize(invariantDeviceProfile.numHotseatIcons, 1);
        }
    }

    private /* synthetic */ void lambda$resetLayout$0(View view) {
        if (!this.mLauncher.isInState(LauncherState.ALL_APPS)) {
            this.mLauncher.getUserEventDispatcher().logActionOnControl(0, 1);
            this.mLauncher.getStateManager().goToState(LauncherState.ALL_APPS);
        }
    }

    @Override // android.view.ViewGroup
    public boolean onInterceptTouchEvent(MotionEvent motionEvent) {
        return (this.mLauncher.getWorkspace().workspaceIconsCanBeDragged() || this.mLauncher.getAccessibilityDelegate().isInAccessibleDrag()) ? false : true;
    }

    @Override // com.android.launcher3.logging.UserEventDispatcher.LogContainerProvider
    public void fillInLogContainerData(View view, ItemInfo itemInfo, LauncherLogProto.Target target, LauncherLogProto.Target target2) {
        target.gridX = itemInfo.cellX;
        target.gridY = itemInfo.cellY;
        target2.containerType = 2;
    }

    @Override // com.android.launcher3.Insettable
    public void setInsets(Rect rect) {
        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) getLayoutParams();
        DeviceProfile deviceProfile = this.mLauncher.getDeviceProfile();
        if (deviceProfile.isVerticalBarLayout()) {
            layoutParams.height = -1;
            if (deviceProfile.isSeascape()) {
                layoutParams.gravity = 3;
                layoutParams.width = deviceProfile.hotseatBarSizePx + rect.left + deviceProfile.hotseatBarSidePaddingPx;
            } else {
                layoutParams.gravity = 5;
                layoutParams.width = deviceProfile.hotseatBarSizePx + rect.right + deviceProfile.hotseatBarSidePaddingPx;
            }
        } else {
            layoutParams.gravity = 80;
            layoutParams.width = -1;
            layoutParams.height = deviceProfile.hotseatBarSizePx + rect.bottom;
        }
        Rect hotseatLayoutPadding = deviceProfile.getHotseatLayoutPadding();
        getLayout().setPadding(hotseatLayoutPadding.left, hotseatLayoutPadding.top, hotseatLayoutPadding.right, hotseatLayoutPadding.bottom);
        setLayoutParams(layoutParams);
        InsettableFrameLayout.dispatchInsets(this, rect);
    }
}
