package android.appwidget;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.CancellationSignal;
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
import com.android.internal.R;
import java.util.concurrent.Executor;

public class AppWidgetHostView extends FrameLayout {
    private static final LayoutInflater.Filter INFLATER_FILTER = new LayoutInflater.Filter() {
        @Override
        public final boolean onLoadClass(Class cls) {
            return cls.isAnnotationPresent(RemoteViews.RemoteView.class);
        }
    };
    private static final String KEY_JAILED_ARRAY = "jail";
    static final boolean LOGD = false;
    static final String TAG = "AppWidgetHostView";
    static final int VIEW_MODE_CONTENT = 1;
    static final int VIEW_MODE_DEFAULT = 3;
    static final int VIEW_MODE_ERROR = 2;
    static final int VIEW_MODE_NOINIT = 0;
    int mAppWidgetId;
    private Executor mAsyncExecutor;
    Context mContext;
    AppWidgetProviderInfo mInfo;
    private CancellationSignal mLastExecutionSignal;
    int mLayoutId;
    private RemoteViews.OnClickHandler mOnClickHandler;
    Context mRemoteContext;
    View mView;
    int mViewMode;

    public AppWidgetHostView(Context context) {
        this(context, 17432576, 17432577);
    }

    public AppWidgetHostView(Context context, RemoteViews.OnClickHandler onClickHandler) {
        this(context, 17432576, 17432577);
        this.mOnClickHandler = onClickHandler;
    }

    public AppWidgetHostView(Context context, int i, int i2) {
        super(context);
        this.mViewMode = 0;
        this.mLayoutId = -1;
        this.mContext = context;
        setIsRootNamespace(true);
    }

    public void setOnClickHandler(RemoteViews.OnClickHandler onClickHandler) {
        this.mOnClickHandler = onClickHandler;
    }

    public void setAppWidget(int i, AppWidgetProviderInfo appWidgetProviderInfo) {
        this.mAppWidgetId = i;
        this.mInfo = appWidgetProviderInfo;
        Rect defaultPadding = getDefaultPadding();
        setPadding(defaultPadding.left, defaultPadding.top, defaultPadding.right, defaultPadding.bottom);
        if (appWidgetProviderInfo != null) {
            String strLoadLabel = appWidgetProviderInfo.loadLabel(getContext().getPackageManager());
            if ((appWidgetProviderInfo.providerInfo.applicationInfo.flags & 1073741824) != 0) {
                strLoadLabel = Resources.getSystem().getString(R.string.suspended_widget_accessibility, strLoadLabel);
            }
            setContentDescription(strLoadLabel);
        }
    }

    public static Rect getDefaultPaddingForWidget(Context context, ComponentName componentName, Rect rect) {
        ApplicationInfo applicationInfo;
        try {
            applicationInfo = context.getPackageManager().getApplicationInfo(componentName.getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            applicationInfo = null;
        }
        return getDefaultPaddingForWidget(context, applicationInfo, rect);
    }

    private static Rect getDefaultPaddingForWidget(Context context, ApplicationInfo applicationInfo, Rect rect) {
        if (rect == null) {
            rect = new Rect(0, 0, 0, 0);
        } else {
            rect.set(0, 0, 0, 0);
        }
        if (applicationInfo != null && applicationInfo.targetSdkVersion >= 14) {
            Resources resources = context.getResources();
            rect.left = resources.getDimensionPixelSize(R.dimen.default_app_widget_padding_left);
            rect.right = resources.getDimensionPixelSize(R.dimen.default_app_widget_padding_right);
            rect.top = resources.getDimensionPixelSize(R.dimen.default_app_widget_padding_top);
            rect.bottom = resources.getDimensionPixelSize(R.dimen.default_app_widget_padding_bottom);
        }
        return rect;
    }

    private Rect getDefaultPadding() {
        return getDefaultPaddingForWidget(this.mContext, this.mInfo == null ? null : this.mInfo.providerInfo.applicationInfo, (Rect) null);
    }

    public int getAppWidgetId() {
        return this.mAppWidgetId;
    }

    public AppWidgetProviderInfo getAppWidgetInfo() {
        return this.mInfo;
    }

    @Override
    protected void dispatchSaveInstanceState(SparseArray<Parcelable> sparseArray) {
        SparseArray<Parcelable> sparseArray2 = new SparseArray<>();
        super.dispatchSaveInstanceState(sparseArray2);
        Bundle bundle = new Bundle();
        bundle.putSparseParcelableArray(KEY_JAILED_ARRAY, sparseArray2);
        sparseArray.put(generateId(), bundle);
    }

    private int generateId() {
        int id = getId();
        return id == -1 ? this.mAppWidgetId : id;
    }

    @Override
    protected void dispatchRestoreInstanceState(SparseArray<Parcelable> sparseArray) {
        SparseArray<Parcelable> sparseArray2;
        Parcelable parcelable = sparseArray.get(generateId());
        if (parcelable instanceof Bundle) {
            sparseArray2 = ((Bundle) parcelable).getSparseParcelableArray(KEY_JAILED_ARRAY);
        } else {
            sparseArray2 = null;
        }
        if (sparseArray2 == null) {
            sparseArray2 = new SparseArray<>();
        }
        try {
            super.dispatchRestoreInstanceState(sparseArray2);
        } catch (Exception e) {
            StringBuilder sb = new StringBuilder();
            sb.append("failed to restoreInstanceState for widget id: ");
            sb.append(this.mAppWidgetId);
            sb.append(", ");
            sb.append(this.mInfo == null ? "null" : this.mInfo.provider);
            Log.e(TAG, sb.toString(), e);
        }
    }

    @Override
    protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
        try {
            super.onLayout(z, i, i2, i3, i4);
        } catch (RuntimeException e) {
            Log.e(TAG, "Remote provider threw runtime exception, using error view instead.", e);
            removeViewInLayout(this.mView);
            View errorView = getErrorView();
            prepareView(errorView);
            addViewInLayout(errorView, 0, errorView.getLayoutParams());
            measureChild(errorView, View.MeasureSpec.makeMeasureSpec(getMeasuredWidth(), 1073741824), View.MeasureSpec.makeMeasureSpec(getMeasuredHeight(), 1073741824));
            errorView.layout(0, 0, errorView.getMeasuredWidth() + this.mPaddingLeft + this.mPaddingRight, errorView.getMeasuredHeight() + this.mPaddingTop + this.mPaddingBottom);
            this.mView = errorView;
            this.mViewMode = 2;
        }
    }

    public void updateAppWidgetSize(Bundle bundle, int i, int i2, int i3, int i4) {
        updateAppWidgetSize(bundle, i, i2, i3, i4, false);
    }

    public void updateAppWidgetSize(Bundle bundle, int i, int i2, int i3, int i4, boolean z) {
        if (bundle == null) {
            bundle = new Bundle();
        }
        Rect defaultPadding = getDefaultPadding();
        float f = getResources().getDisplayMetrics().density;
        int i5 = (int) ((defaultPadding.left + defaultPadding.right) / f);
        int i6 = (int) ((defaultPadding.top + defaultPadding.bottom) / f);
        boolean z2 = false;
        int i7 = i - (z ? 0 : i5);
        int i8 = i2 - (z ? 0 : i6);
        if (z) {
            i5 = 0;
        }
        int i9 = i3 - i5;
        if (z) {
            i6 = 0;
        }
        int i10 = i4 - i6;
        Bundle appWidgetOptions = AppWidgetManager.getInstance(this.mContext).getAppWidgetOptions(this.mAppWidgetId);
        if (i7 != appWidgetOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH) || i8 != appWidgetOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT) || i9 != appWidgetOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH) || i10 != appWidgetOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT)) {
            z2 = true;
        }
        if (z2) {
            bundle.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, i7);
            bundle.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, i8);
            bundle.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, i9);
            bundle.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, i10);
            updateAppWidgetOptions(bundle);
        }
    }

    public void updateAppWidgetOptions(Bundle bundle) {
        AppWidgetManager.getInstance(this.mContext).updateAppWidgetOptions(this.mAppWidgetId, bundle);
    }

    @Override
    public FrameLayout.LayoutParams generateLayoutParams(AttributeSet attributeSet) {
        return new FrameLayout.LayoutParams(this.mRemoteContext != null ? this.mRemoteContext : this.mContext, attributeSet);
    }

    public void setExecutor(Executor executor) {
        if (this.mLastExecutionSignal != null) {
            this.mLastExecutionSignal.cancel();
            this.mLastExecutionSignal = null;
        }
        this.mAsyncExecutor = executor;
    }

    void resetAppWidget(AppWidgetProviderInfo appWidgetProviderInfo) {
        setAppWidget(this.mAppWidgetId, appWidgetProviderInfo);
        this.mViewMode = 0;
        updateAppWidget(null);
    }

    public void updateAppWidget(RemoteViews remoteViews) {
        applyRemoteViews(remoteViews, true);
    }

    protected void applyRemoteViews(RemoteViews remoteViews, boolean z) {
        View viewApply;
        View view = null;
        RuntimeException runtimeException = null;
        view = null;
        if (this.mLastExecutionSignal != null) {
            this.mLastExecutionSignal.cancel();
            this.mLastExecutionSignal = null;
        }
        boolean z2 = false;
        if (remoteViews == null) {
            if (this.mViewMode == 3) {
                return;
            }
            viewApply = getDefaultView();
            this.mLayoutId = -1;
            this.mViewMode = 3;
        } else {
            if (this.mAsyncExecutor != null && z) {
                inflateAsync(remoteViews);
                return;
            }
            this.mRemoteContext = getRemoteContext();
            int layoutId = remoteViews.getLayoutId();
            if (layoutId == this.mLayoutId) {
                try {
                    remoteViews.reapply(this.mContext, this.mView, this.mOnClickHandler);
                    z2 = true;
                    e = null;
                    view = this.mView;
                } catch (RuntimeException e) {
                    e = e;
                }
            } else {
                e = null;
            }
            if (view == null) {
                try {
                    viewApply = remoteViews.apply(this.mContext, this, this.mOnClickHandler);
                } catch (RuntimeException e2) {
                    View view2 = view;
                    runtimeException = e2;
                    viewApply = view2;
                }
            } else {
                viewApply = view;
            }
            runtimeException = e;
            this.mLayoutId = layoutId;
            this.mViewMode = 1;
        }
        applyContent(viewApply, z2, runtimeException);
    }

    private void applyContent(View view, boolean z, Exception exc) {
        if (view == null) {
            if (this.mViewMode == 2) {
                return;
            }
            if (exc != null) {
                Log.w(TAG, "Error inflating RemoteViews : " + exc.toString());
            }
            view = getErrorView();
            this.mViewMode = 2;
        }
        if (!z) {
            prepareView(view);
            addView(view);
        }
        if (this.mView != view) {
            removeView(this.mView);
            this.mView = view;
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
        if (this.mLastExecutionSignal == null) {
            this.mLastExecutionSignal = remoteViews.applyAsync(this.mContext, this, this.mAsyncExecutor, new ViewApplyListener(remoteViews, layoutId, false), this.mOnClickHandler);
        }
    }

    private class ViewApplyListener implements RemoteViews.OnViewAppliedListener {
        private final boolean mIsReapply;
        private final int mLayoutId;
        private final RemoteViews mViews;

        public ViewApplyListener(RemoteViews remoteViews, int i, boolean z) {
            this.mViews = remoteViews;
            this.mLayoutId = i;
            this.mIsReapply = z;
        }

        @Override
        public void onViewApplied(View view) {
            AppWidgetHostView.this.mLayoutId = this.mLayoutId;
            AppWidgetHostView.this.mViewMode = 1;
            AppWidgetHostView.this.applyContent(view, this.mIsReapply, null);
        }

        @Override
        public void onError(Exception exc) {
            if (!this.mIsReapply) {
                AppWidgetHostView.this.applyContent(null, false, exc);
            } else {
                AppWidgetHostView.this.mLastExecutionSignal = this.mViews.applyAsync(AppWidgetHostView.this.mContext, AppWidgetHostView.this, AppWidgetHostView.this.mAsyncExecutor, AppWidgetHostView.this.new ViewApplyListener(this.mViews, this.mLayoutId, false), AppWidgetHostView.this.mOnClickHandler);
            }
        }
    }

    void viewDataChanged(int i) {
        View viewFindViewById = findViewById(i);
        if (viewFindViewById != null && (viewFindViewById instanceof AdapterView)) {
            AdapterView adapterView = (AdapterView) viewFindViewById;
            Adapter adapter = adapterView.getAdapter();
            if (adapter instanceof BaseAdapter) {
                ((BaseAdapter) adapter).notifyDataSetChanged();
            } else if (adapter == null && (adapterView instanceof RemoteViewsAdapter.RemoteAdapterConnectionCallback)) {
                ((RemoteViewsAdapter.RemoteAdapterConnectionCallback) adapterView).deferNotifyDataSetChanged();
            }
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

    protected void prepareView(View view) {
        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) view.getLayoutParams();
        if (layoutParams == null) {
            layoutParams = new FrameLayout.LayoutParams(-1, -1);
        }
        layoutParams.gravity = 17;
        view.setLayoutParams(layoutParams);
    }

    protected View getDefaultView() {
        View viewInflate;
        int i;
        RuntimeException runtimeException = null;
        try {
            if (this.mInfo != null) {
                Context remoteContext = getRemoteContext();
                this.mRemoteContext = remoteContext;
                LayoutInflater layoutInflaterCloneInContext = ((LayoutInflater) remoteContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).cloneInContext(remoteContext);
                layoutInflaterCloneInContext.setFilter(INFLATER_FILTER);
                Bundle appWidgetOptions = AppWidgetManager.getInstance(this.mContext).getAppWidgetOptions(this.mAppWidgetId);
                int i2 = this.mInfo.initialLayout;
                if (appWidgetOptions.containsKey(AppWidgetManager.OPTION_APPWIDGET_HOST_CATEGORY) && appWidgetOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_HOST_CATEGORY) == 2 && (i = this.mInfo.initialKeyguardLayout) != 0) {
                    i2 = i;
                }
                viewInflate = layoutInflaterCloneInContext.inflate(i2, (ViewGroup) this, false);
            } else {
                Log.w(TAG, "can't inflate defaultView because mInfo is missing");
                viewInflate = null;
            }
        } catch (RuntimeException e) {
            viewInflate = null;
            runtimeException = e;
        }
        if (runtimeException != null) {
            Log.w(TAG, "Error inflating AppWidget " + this.mInfo + ": " + runtimeException.toString());
        }
        if (viewInflate == null) {
            return getErrorView();
        }
        return viewInflate;
    }

    protected View getErrorView() {
        TextView textView = new TextView(this.mContext);
        textView.setText(R.string.gadget_host_error_inflating);
        textView.setBackgroundColor(Color.argb(127, 0, 0, 0));
        return textView;
    }

    @Override
    public void onInitializeAccessibilityNodeInfoInternal(AccessibilityNodeInfo accessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfoInternal(accessibilityNodeInfo);
        accessibilityNodeInfo.setClassName(AppWidgetHostView.class.getName());
    }
}
