package com.android.launcher3.dragndrop;

import android.annotation.TargetApi;
import android.app.ActivityOptions;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.Intent;
import android.content.pm.LauncherApps;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import com.android.launcher3.BaseActivity;
import com.android.launcher3.InstallShortcutReceiver;
import com.android.launcher3.InvariantDeviceProfile;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.LauncherAppWidgetHost;
import com.android.launcher3.LauncherAppWidgetProviderInfo;
import com.android.launcher3.LauncherSettings;
import com.android.launcher3.R;
import com.android.launcher3.compat.AppWidgetManagerCompat;
import com.android.launcher3.compat.LauncherAppsCompatVO;
import com.android.launcher3.logging.LoggerUtils;
import com.android.launcher3.model.WidgetItem;
import com.android.launcher3.shortcuts.ShortcutInfoCompat;
import com.android.launcher3.util.InstantAppResolver;
import com.android.launcher3.widget.PendingAddShortcutInfo;
import com.android.launcher3.widget.PendingAddWidgetInfo;
import com.android.launcher3.widget.WidgetHostViewLoader;
import com.android.launcher3.widget.WidgetImageView;

@TargetApi(26)
/* loaded from: classes.dex */
public class AddItemActivity extends BaseActivity implements View.OnLongClickListener, View.OnTouchListener {
    private static final int REQUEST_BIND_APPWIDGET = 1;
    private static final int SHADOW_SIZE = 10;
    private static final String STATE_EXTRA_WIDGET_ID = "state.widget.id";
    private LauncherAppState mApp;
    private LauncherAppWidgetHost mAppWidgetHost;
    private AppWidgetManagerCompat mAppWidgetManager;
    private InvariantDeviceProfile mIdp;
    private InstantAppResolver mInstantAppResolver;
    private int mPendingBindWidgetId;
    private PendingAddWidgetInfo mPendingWidgetInfo;
    private LauncherApps.PinItemRequest mRequest;
    private LivePreviewWidgetCell mWidgetCell;
    private Bundle mWidgetOptions;
    private final PointF mLastTouchPos = new PointF();
    private boolean mFinishOnPause = false;

    @Override // android.app.Activity
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.mRequest = LauncherAppsCompatVO.getPinItemRequest(getIntent());
        if (this.mRequest == null) {
            finish();
            return;
        }
        this.mApp = LauncherAppState.getInstance(this);
        this.mIdp = this.mApp.getInvariantDeviceProfile();
        this.mInstantAppResolver = InstantAppResolver.newInstance(this);
        this.mDeviceProfile = this.mIdp.getDeviceProfile(getApplicationContext());
        setContentView(R.layout.add_item_confirmation_activity);
        this.mWidgetCell = (LivePreviewWidgetCell) findViewById(R.id.widget_cell);
        if (this.mRequest.getRequestType() == 1) {
            setupShortcut();
        } else if (!setupWidget()) {
            finish();
        }
        this.mWidgetCell.setOnTouchListener(this);
        if (bundle == null) {
            logCommand(2);
        }
    }

    @Override // android.view.View.OnTouchListener
    public boolean onTouch(View view, MotionEvent motionEvent) {
        this.mLastTouchPos.set(motionEvent.getX(), motionEvent.getY());
        return false;
    }

    @Override // android.view.View.OnLongClickListener
    public boolean onLongClick(View view) {
        WidgetImageView widgetView = this.mWidgetCell.getWidgetView();
        if (widgetView.getBitmap() == null) {
            return false;
        }
        Rect bitmapBounds = widgetView.getBitmapBounds();
        bitmapBounds.offset(widgetView.getLeft() - ((int) this.mLastTouchPos.x), widgetView.getTop() - ((int) this.mLastTouchPos.y));
        PinItemDragListener pinItemDragListener = new PinItemDragListener(this.mRequest, bitmapBounds, widgetView.getBitmap().getWidth(), widgetView.getWidth());
        Intent intentAddToIntent = pinItemDragListener.addToIntent(new Intent("android.intent.action.MAIN").addCategory("android.intent.category.HOME").setPackage(getPackageName()).setFlags(268435456));
        pinItemDragListener.initWhenReady();
        startActivity(intentAddToIntent, ActivityOptions.makeCustomAnimation(this, 0, android.R.anim.fade_out).toBundle());
        this.mFinishOnPause = true;
        view.startDragAndDrop(new ClipData(new ClipDescription("", new String[]{pinItemDragListener.getMimeType()}), new ClipData.Item("")), new View.DragShadowBuilder(view) { // from class: com.android.launcher3.dragndrop.AddItemActivity.1
            @Override // android.view.View.DragShadowBuilder
            public void onDrawShadow(Canvas canvas) {
            }

            @Override // android.view.View.DragShadowBuilder
            public void onProvideShadowMetrics(Point point, Point point2) {
                point.set(10, 10);
                point2.set(5, 5);
            }
        }, null, 256);
        return false;
    }

    @Override // com.android.launcher3.BaseActivity, android.app.Activity
    protected void onPause() {
        super.onPause();
        if (this.mFinishOnPause) {
            finish();
        }
    }

    private void setupShortcut() {
        PinShortcutRequestActivityInfo pinShortcutRequestActivityInfo = new PinShortcutRequestActivityInfo(this.mRequest, this);
        WidgetItem widgetItem = new WidgetItem(pinShortcutRequestActivityInfo);
        this.mWidgetCell.getWidgetView().setTag(new PendingAddShortcutInfo(pinShortcutRequestActivityInfo));
        this.mWidgetCell.applyFromCellItem(widgetItem, this.mApp.getWidgetCache());
        this.mWidgetCell.ensurePreview();
    }

    private boolean setupWidget() {
        LauncherAppWidgetProviderInfo launcherAppWidgetProviderInfoFromProviderInfo = LauncherAppWidgetProviderInfo.fromProviderInfo(this, this.mRequest.getAppWidgetProviderInfo(this));
        if (launcherAppWidgetProviderInfoFromProviderInfo.minSpanX > this.mIdp.numColumns || launcherAppWidgetProviderInfoFromProviderInfo.minSpanY > this.mIdp.numRows) {
            return false;
        }
        this.mWidgetCell.setPreview(PinItemDragListener.getPreview(this.mRequest));
        this.mAppWidgetManager = AppWidgetManagerCompat.getInstance(this);
        this.mAppWidgetHost = new LauncherAppWidgetHost(this);
        this.mPendingWidgetInfo = new PendingAddWidgetInfo(launcherAppWidgetProviderInfoFromProviderInfo);
        this.mPendingWidgetInfo.spanX = Math.min(this.mIdp.numColumns, launcherAppWidgetProviderInfoFromProviderInfo.spanX);
        this.mPendingWidgetInfo.spanY = Math.min(this.mIdp.numRows, launcherAppWidgetProviderInfoFromProviderInfo.spanY);
        this.mWidgetOptions = WidgetHostViewLoader.getDefaultOptionsForWidget(this, this.mPendingWidgetInfo);
        WidgetItem widgetItem = new WidgetItem(launcherAppWidgetProviderInfoFromProviderInfo, getPackageManager(), this.mIdp);
        this.mWidgetCell.getWidgetView().setTag(this.mPendingWidgetInfo);
        this.mWidgetCell.applyFromCellItem(widgetItem, this.mApp.getWidgetCache());
        this.mWidgetCell.ensurePreview();
        return true;
    }

    public void onCancelClick(View view) {
        logCommand(3);
        finish();
    }

    public void onPlaceAutomaticallyClick(View view) {
        if (this.mRequest.getRequestType() == 1) {
            InstallShortcutReceiver.queueShortcut(new ShortcutInfoCompat(this.mRequest.getShortcutInfo()), this);
            logCommand(4);
            this.mRequest.accept();
            finish();
            return;
        }
        this.mPendingBindWidgetId = this.mAppWidgetHost.allocateAppWidgetId();
        if (this.mAppWidgetManager.bindAppWidgetIdIfAllowed(this.mPendingBindWidgetId, this.mRequest.getAppWidgetProviderInfo(this), this.mWidgetOptions)) {
            acceptWidget(this.mPendingBindWidgetId);
        } else {
            this.mAppWidgetHost.startBindFlow(this, this.mPendingBindWidgetId, this.mRequest.getAppWidgetProviderInfo(this), 1);
        }
    }

    private void acceptWidget(int i) {
        InstallShortcutReceiver.queueWidget(this.mRequest.getAppWidgetProviderInfo(this), i, this);
        this.mWidgetOptions.putInt(LauncherSettings.Favorites.APPWIDGET_ID, i);
        this.mRequest.accept(this.mWidgetOptions);
        logCommand(4);
        finish();
    }

    @Override // android.app.Activity
    public void onBackPressed() {
        logCommand(1);
        super.onBackPressed();
    }

    @Override // com.android.launcher3.BaseActivity, android.app.Activity
    public void onActivityResult(int i, int i2, Intent intent) {
        int intExtra;
        if (i == 1) {
            if (intent != null) {
                intExtra = intent.getIntExtra(LauncherSettings.Favorites.APPWIDGET_ID, this.mPendingBindWidgetId);
            } else {
                intExtra = this.mPendingBindWidgetId;
            }
            if (i2 == -1) {
                acceptWidget(intExtra);
                return;
            } else {
                this.mAppWidgetHost.deleteAppWidgetId(intExtra);
                this.mPendingBindWidgetId = -1;
                return;
            }
        }
        super.onActivityResult(i, i2, intent);
    }

    @Override // android.app.Activity
    protected void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        bundle.putInt(STATE_EXTRA_WIDGET_ID, this.mPendingBindWidgetId);
    }

    @Override // android.app.Activity
    protected void onRestoreInstanceState(Bundle bundle) {
        super.onRestoreInstanceState(bundle);
        this.mPendingBindWidgetId = bundle.getInt(STATE_EXTRA_WIDGET_ID, this.mPendingBindWidgetId);
    }

    private void logCommand(int i) {
        getUserEventDispatcher().dispatchUserEvent(LoggerUtils.newLauncherEvent(LoggerUtils.newCommandAction(i), LoggerUtils.newItemTarget(this.mWidgetCell.getWidgetView(), this.mInstantAppResolver), LoggerUtils.newContainerTarget(10)), null);
    }
}
