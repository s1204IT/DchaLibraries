package android.appwidget;

import android.R;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.LauncherApps;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.RemoteViews;
import android.widget.RemoteViewsAdapter;
import android.widget.TextView;
import java.util.concurrent.Executor;

public class AppWidgetHostView extends FrameLayout {
    static final boolean CROSSFADE = false;
    static final int FADE_DURATION = 1000;
    static final boolean LOGD = false;
    static final String TAG = "AppWidgetHostView";
    static final int VIEW_MODE_CONTENT = 1;
    static final int VIEW_MODE_DEFAULT = 3;
    static final int VIEW_MODE_ERROR = 2;
    static final int VIEW_MODE_NOINIT = 0;
    static final LayoutInflater.Filter sInflaterFilter = new LayoutInflater.Filter() {
        @Override
        public boolean onLoadClass(Class clazz) {
            return clazz.isAnnotationPresent(RemoteViews.RemoteView.class);
        }
    };
    int mAppWidgetId;
    private Executor mAsyncExecutor;
    Context mContext;
    long mFadeStartTime;
    AppWidgetProviderInfo mInfo;
    private CancellationSignal mLastExecutionSignal;
    int mLayoutId;
    Bitmap mOld;
    Paint mOldPaint;
    private RemoteViews.OnClickHandler mOnClickHandler;
    Context mRemoteContext;
    View mView;
    int mViewMode;

    public AppWidgetHostView(Context context) {
        this(context, R.anim.fade_in, R.anim.fade_out);
    }

    public AppWidgetHostView(Context context, RemoteViews.OnClickHandler handler) {
        this(context, R.anim.fade_in, R.anim.fade_out);
        this.mOnClickHandler = handler;
    }

    public AppWidgetHostView(Context context, int animationIn, int animationOut) {
        super(context);
        this.mViewMode = 0;
        this.mLayoutId = -1;
        this.mFadeStartTime = -1L;
        this.mOldPaint = new Paint();
        this.mContext = context;
        setIsRootNamespace(true);
    }

    public void setOnClickHandler(RemoteViews.OnClickHandler handler) {
        this.mOnClickHandler = handler;
    }

    public void setAppWidget(int appWidgetId, AppWidgetProviderInfo info) {
        this.mAppWidgetId = appWidgetId;
        this.mInfo = info;
        if (info == null) {
            return;
        }
        Rect padding = getDefaultPaddingForWidget(this.mContext, info.provider, null);
        setPadding(padding.left, padding.top, padding.right, padding.bottom);
        updateContentDescription(info);
    }

    public static Rect getDefaultPaddingForWidget(Context context, ComponentName component, Rect padding) {
        PackageManager packageManager = context.getPackageManager();
        if (padding == null) {
            padding = new Rect(0, 0, 0, 0);
        } else {
            padding.set(0, 0, 0, 0);
        }
        try {
            ApplicationInfo appInfo = packageManager.getApplicationInfo(component.getPackageName(), 0);
            if (appInfo.targetSdkVersion >= 14) {
                Resources r = context.getResources();
                padding.left = r.getDimensionPixelSize(17105000);
                padding.right = r.getDimensionPixelSize(17105002);
                padding.top = r.getDimensionPixelSize(17105001);
                padding.bottom = r.getDimensionPixelSize(17105003);
            }
            return padding;
        } catch (PackageManager.NameNotFoundException e) {
            return padding;
        }
    }

    public int getAppWidgetId() {
        return this.mAppWidgetId;
    }

    public AppWidgetProviderInfo getAppWidgetInfo() {
        return this.mInfo;
    }

    @Override
    protected void dispatchSaveInstanceState(SparseArray<Parcelable> container) {
        ParcelableSparseArray jail = new ParcelableSparseArray(null);
        super.dispatchSaveInstanceState(jail);
        container.put(generateId(), jail);
    }

    private int generateId() {
        int id = getId();
        return id == -1 ? this.mAppWidgetId : id;
    }

    @Override
    protected void dispatchRestoreInstanceState(SparseArray<Parcelable> container) {
        ParcelableSparseArray parcelableSparseArray = null;
        Parcelable parcelable = container.get(generateId());
        ParcelableSparseArray jail = null;
        if (parcelable != null && (parcelable instanceof ParcelableSparseArray)) {
            jail = (ParcelableSparseArray) parcelable;
        }
        if (jail == null) {
            jail = new ParcelableSparseArray(parcelableSparseArray);
        }
        try {
            super.dispatchRestoreInstanceState(jail);
        } catch (Exception e) {
            Log.e(TAG, "failed to restoreInstanceState for widget id: " + this.mAppWidgetId + ", " + (this.mInfo == null ? "null" : this.mInfo.provider), e);
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        try {
            super.onLayout(changed, left, top, right, bottom);
        } catch (RuntimeException e) {
            Log.e(TAG, "Remote provider threw runtime exception, using error view instead.", e);
            removeViewInLayout(this.mView);
            View child = getErrorView();
            prepareView(child);
            addViewInLayout(child, 0, child.getLayoutParams());
            measureChild(child, View.MeasureSpec.makeMeasureSpec(getMeasuredWidth(), 1073741824), View.MeasureSpec.makeMeasureSpec(getMeasuredHeight(), 1073741824));
            child.layout(0, 0, child.getMeasuredWidth() + this.mPaddingLeft + this.mPaddingRight, child.getMeasuredHeight() + this.mPaddingTop + this.mPaddingBottom);
            this.mView = child;
            this.mViewMode = 2;
        }
    }

    public void updateAppWidgetSize(Bundle newOptions, int minWidth, int minHeight, int maxWidth, int maxHeight) {
        updateAppWidgetSize(newOptions, minWidth, minHeight, maxWidth, maxHeight, false);
    }

    public void updateAppWidgetSize(Bundle newOptions, int minWidth, int minHeight, int maxWidth, int maxHeight, boolean ignorePadding) {
        if (newOptions == null) {
            newOptions = new Bundle();
        }
        Rect padding = new Rect();
        if (this.mInfo != null) {
            padding = getDefaultPaddingForWidget(this.mContext, this.mInfo.provider, padding);
        }
        float density = getResources().getDisplayMetrics().density;
        int xPaddingDips = (int) ((padding.left + padding.right) / density);
        int yPaddingDips = (int) ((padding.top + padding.bottom) / density);
        int newMinWidth = minWidth - (ignorePadding ? 0 : xPaddingDips);
        int newMinHeight = minHeight - (ignorePadding ? 0 : yPaddingDips);
        if (ignorePadding) {
            xPaddingDips = 0;
        }
        int newMaxWidth = maxWidth - xPaddingDips;
        if (ignorePadding) {
            yPaddingDips = 0;
        }
        int newMaxHeight = maxHeight - yPaddingDips;
        AppWidgetManager widgetManager = AppWidgetManager.getInstance(this.mContext);
        Bundle oldOptions = widgetManager.getAppWidgetOptions(this.mAppWidgetId);
        boolean needsUpdate = false;
        if (newMinWidth != oldOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH) || newMinHeight != oldOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT) || newMaxWidth != oldOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH) || newMaxHeight != oldOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT)) {
            needsUpdate = true;
        }
        if (!needsUpdate) {
            return;
        }
        newOptions.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, newMinWidth);
        newOptions.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, newMinHeight);
        newOptions.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, newMaxWidth);
        newOptions.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, newMaxHeight);
        updateAppWidgetOptions(newOptions);
    }

    public void updateAppWidgetOptions(Bundle options) {
        AppWidgetManager.getInstance(this.mContext).updateAppWidgetOptions(this.mAppWidgetId, options);
    }

    @Override
    public FrameLayout.LayoutParams generateLayoutParams(AttributeSet attrs) {
        Context context = this.mRemoteContext != null ? this.mRemoteContext : this.mContext;
        return new FrameLayout.LayoutParams(context, attrs);
    }

    public void setAsyncExecutor(Executor executor) {
        if (this.mLastExecutionSignal != null) {
            this.mLastExecutionSignal.cancel();
            this.mLastExecutionSignal = null;
        }
        this.mAsyncExecutor = executor;
    }

    void resetAppWidget(AppWidgetProviderInfo info) {
        this.mInfo = info;
        this.mViewMode = 0;
        updateAppWidget(null);
    }

    public void updateAppWidget(RemoteViews remoteViews) {
        applyRemoteViews(remoteViews);
    }

    protected void applyRemoteViews(RemoteViews remoteViews) {
        boolean recycled = false;
        View content = null;
        Exception exception = null;
        if (this.mLastExecutionSignal != null) {
            this.mLastExecutionSignal.cancel();
            this.mLastExecutionSignal = null;
        }
        if (remoteViews == null) {
            if (this.mViewMode == 3) {
                return;
            }
            content = getDefaultView();
            this.mLayoutId = -1;
            this.mViewMode = 3;
        } else {
            if (this.mAsyncExecutor != null) {
                inflateAsync(remoteViews);
                return;
            }
            this.mRemoteContext = getRemoteContext();
            int layoutId = remoteViews.getLayoutId();
            if (layoutId == this.mLayoutId) {
                try {
                    remoteViews.reapply(this.mContext, this.mView, this.mOnClickHandler);
                    content = this.mView;
                    recycled = true;
                } catch (RuntimeException e) {
                    exception = e;
                }
            }
            if (content == null) {
                try {
                    content = remoteViews.apply(this.mContext, this, this.mOnClickHandler);
                } catch (RuntimeException e2) {
                    exception = e2;
                }
            }
            this.mLayoutId = layoutId;
            this.mViewMode = 1;
        }
        applyContent(content, recycled, exception);
        updateContentDescription(this.mInfo);
    }

    private void applyContent(View content, boolean recycled, Exception exception) {
        if (content == null) {
            if (this.mViewMode == 2) {
                return;
            }
            Log.w(TAG, "updateAppWidget couldn't find any view, using error view", exception);
            content = getErrorView();
            this.mViewMode = 2;
        }
        if (!recycled) {
            prepareView(content);
            addView(content);
        }
        if (this.mView == content) {
            return;
        }
        removeView(this.mView);
        this.mView = content;
    }

    private void updateContentDescription(AppWidgetProviderInfo info) {
        if (info == null) {
            return;
        }
        LauncherApps launcherApps = (LauncherApps) getContext().getSystemService(LauncherApps.class);
        ApplicationInfo appInfo = launcherApps.getApplicationInfo(info.provider.getPackageName(), 0, info.getProfile());
        if (appInfo != null && (appInfo.flags & 1073741824) != 0) {
            setContentDescription(Resources.getSystem().getString(17040877, info.label));
        } else {
            setContentDescription(info.label);
        }
    }

    private void inflateAsync(RemoteViews remoteViews) {
        this.mRemoteContext = getRemoteContext();
        int layoutId = remoteViews.getLayoutId();
        if (layoutId == this.mLayoutId && this.mView != null) {
            try {
                this.mLastExecutionSignal = remoteViews.reapplyAsync(this.mContext, this.mView, this.mAsyncExecutor, new ViewApplyListener(remoteViews, layoutId, true), this.mOnClickHandler);
            } catch (Exception e) {
            }
        }
        if (this.mLastExecutionSignal != null) {
            return;
        }
        this.mLastExecutionSignal = remoteViews.applyAsync(this.mContext, this, this.mAsyncExecutor, new ViewApplyListener(remoteViews, layoutId, false), this.mOnClickHandler);
    }

    private class ViewApplyListener implements RemoteViews.OnViewAppliedListener {
        private final boolean mIsReapply;
        private final int mLayoutId;
        private final RemoteViews mViews;

        public ViewApplyListener(RemoteViews views, int layoutId, boolean isReapply) {
            this.mViews = views;
            this.mLayoutId = layoutId;
            this.mIsReapply = isReapply;
        }

        public void onViewApplied(View v) {
            AppWidgetHostView.this.mLayoutId = this.mLayoutId;
            AppWidgetHostView.this.mViewMode = 1;
            AppWidgetHostView.this.applyContent(v, this.mIsReapply, null);
        }

        public void onError(Exception e) {
            if (this.mIsReapply) {
                AppWidgetHostView.this.mLastExecutionSignal = this.mViews.applyAsync(AppWidgetHostView.this.mContext, AppWidgetHostView.this, AppWidgetHostView.this.mAsyncExecutor, AppWidgetHostView.this.new ViewApplyListener(this.mViews, this.mLayoutId, false), AppWidgetHostView.this.mOnClickHandler);
            } else {
                AppWidgetHostView.this.applyContent(null, false, e);
            }
        }
    }

    void viewDataChanged(int viewId) {
        RemoteViewsAdapter.RemoteAdapterConnectionCallback remoteAdapterConnectionCallbackFindViewById = findViewById(viewId);
        if (remoteAdapterConnectionCallbackFindViewById == null || !(remoteAdapterConnectionCallbackFindViewById instanceof AdapterView)) {
            return;
        }
        RemoteViewsAdapter.RemoteAdapterConnectionCallback remoteAdapterConnectionCallback = (AdapterView) remoteAdapterConnectionCallbackFindViewById;
        Adapter adapter = remoteAdapterConnectionCallback.getAdapter();
        if (adapter instanceof BaseAdapter) {
            BaseAdapter baseAdapter = (BaseAdapter) adapter;
            baseAdapter.notifyDataSetChanged();
        } else {
            if (adapter != null || !(remoteAdapterConnectionCallback instanceof RemoteViewsAdapter.RemoteAdapterConnectionCallback)) {
                return;
            }
            remoteAdapterConnectionCallback.deferNotifyDataSetChanged();
        }
    }

    protected Context getRemoteContext() {
        try {
            return this.mContext.createApplicationContext(this.mInfo.providerInfo.applicationInfo, 4);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Package name " + this.mInfo.providerInfo.packageName + " not found");
            return this.mContext;
        }
    }

    @Override
    protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
        return super.drawChild(canvas, child, drawingTime);
    }

    protected void prepareView(View view) {
        FrameLayout.LayoutParams requested = (FrameLayout.LayoutParams) view.getLayoutParams();
        if (requested == null) {
            requested = new FrameLayout.LayoutParams(-1, -1);
        }
        requested.gravity = 17;
        view.setLayoutParams(requested);
    }

    protected View getDefaultView() {
        int kgLayoutId;
        View defaultView = null;
        Exception exception = null;
        try {
            if (this.mInfo != null) {
                Context theirContext = getRemoteContext();
                this.mRemoteContext = theirContext;
                LayoutInflater inflater = ((LayoutInflater) theirContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).cloneInContext(theirContext);
                inflater.setFilter(sInflaterFilter);
                AppWidgetManager manager = AppWidgetManager.getInstance(this.mContext);
                Bundle options = manager.getAppWidgetOptions(this.mAppWidgetId);
                int layoutId = this.mInfo.initialLayout;
                if (options.containsKey(AppWidgetManager.OPTION_APPWIDGET_HOST_CATEGORY)) {
                    int category = options.getInt(AppWidgetManager.OPTION_APPWIDGET_HOST_CATEGORY);
                    if (category == 2 && (kgLayoutId = this.mInfo.initialKeyguardLayout) != 0) {
                        layoutId = kgLayoutId;
                    }
                }
                defaultView = inflater.inflate(layoutId, (ViewGroup) this, false);
            } else {
                Log.w(TAG, "can't inflate defaultView because mInfo is missing");
            }
        } catch (RuntimeException e) {
            exception = e;
        }
        if (exception != null) {
            Log.w(TAG, "Error inflating AppWidget " + this.mInfo + ": " + exception.toString());
        }
        if (defaultView == null) {
            return getErrorView();
        }
        return defaultView;
    }

    protected View getErrorView() {
        TextView tv = new TextView(this.mContext);
        tv.setText(17040465);
        tv.setBackgroundColor(Color.argb(127, 0, 0, 0));
        return tv;
    }

    public void onInitializeAccessibilityNodeInfoInternal(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfoInternal(info);
        info.setClassName(AppWidgetHostView.class.getName());
    }

    private static class ParcelableSparseArray extends SparseArray<Parcelable> implements Parcelable {
        public static final Parcelable.Creator<ParcelableSparseArray> CREATOR = new Parcelable.Creator<ParcelableSparseArray>() {
            @Override
            public ParcelableSparseArray createFromParcel(Parcel source) {
                ParcelableSparseArray array = new ParcelableSparseArray(null);
                ClassLoader loader = array.getClass().getClassLoader();
                int count = source.readInt();
                for (int i = 0; i < count; i++) {
                    array.put(source.readInt(), source.readParcelable(loader));
                }
                return array;
            }

            @Override
            public ParcelableSparseArray[] newArray(int size) {
                return new ParcelableSparseArray[size];
            }
        };

        ParcelableSparseArray(ParcelableSparseArray parcelableSparseArray) {
            this();
        }

        private ParcelableSparseArray() {
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            int count = size();
            dest.writeInt(count);
            for (int i = 0; i < count; i++) {
                dest.writeInt(keyAt(i));
                dest.writeParcelable(valueAt(i), 0);
            }
        }
    }
}
