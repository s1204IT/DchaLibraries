package com.android.launcher3.qsb;

import android.app.Activity;
import android.app.Fragment;
import android.app.SearchManager;
import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.os.BenesseExtension;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import com.android.launcher3.AppWidgetResizeFrame;
import com.android.launcher3.InvariantDeviceProfile;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.LauncherSettings;
import com.android.launcher3.R;
import com.android.launcher3.Utilities;

/* loaded from: classes.dex */
public class QsbContainerView extends FrameLayout {
    public QsbContainerView(Context context) {
        super(context);
    }

    public QsbContainerView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    public QsbContainerView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
    }

    @Override // android.view.View
    public void setPadding(int i, int i2, int i3, int i4) {
        super.setPadding(0, 0, 0, 0);
    }

    protected void setPaddingUnchecked(int i, int i2, int i3, int i4) {
        super.setPadding(i, i2, i3, i4);
    }

    public static class QsbFragment extends Fragment implements View.OnClickListener {
        private static final String QSB_WIDGET_ID = "qsb_widget_id";
        private static final int REQUEST_BIND_QSB = 1;
        private int mOrientation;
        private QsbWidgetHostView mQsb;
        private QsbWidgetHost mQsbWidgetHost;
        private AppWidgetProviderInfo mWidgetInfo;
        private FrameLayout mWrapper;

        @Override // android.app.Fragment
        public void onCreate(Bundle bundle) {
            super.onCreate(bundle);
            this.mQsbWidgetHost = new QsbWidgetHost(getActivity());
            this.mOrientation = getContext().getResources().getConfiguration().orientation;
        }

        @Override // android.app.Fragment
        public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
            this.mWrapper = new FrameLayout(getActivity());
            if (isQsbEnabled()) {
                this.mWrapper.addView(createQsb(this.mWrapper));
            }
            return this.mWrapper;
        }

        private View createQsb(ViewGroup viewGroup) {
            Activity activity = getActivity();
            this.mWidgetInfo = QsbContainerView.getSearchWidgetProvider(activity);
            if (this.mWidgetInfo == null) {
                return QsbWidgetHostView.getDefaultView(viewGroup);
            }
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(activity);
            InvariantDeviceProfile idp = LauncherAppState.getIDP(activity);
            Bundle bundle = new Bundle();
            Rect widgetSizeRanges = AppWidgetResizeFrame.getWidgetSizeRanges(activity, idp.numColumns, 1, null);
            bundle.putInt("appWidgetMinWidth", widgetSizeRanges.left);
            bundle.putInt("appWidgetMinHeight", widgetSizeRanges.top);
            bundle.putInt("appWidgetMaxWidth", widgetSizeRanges.right);
            bundle.putInt("appWidgetMaxHeight", widgetSizeRanges.bottom);
            bundle.putString("attached-launcher-identifier", "com.android.launcher3");
            bundle.putString("requested-widget-style", "cqsb");
            int i = Utilities.getPrefs(activity).getInt(QSB_WIDGET_ID, -1);
            AppWidgetProviderInfo appWidgetInfo = appWidgetManager.getAppWidgetInfo(i);
            boolean zBindAppWidgetIdIfAllowed = appWidgetInfo != null && appWidgetInfo.provider.equals(this.mWidgetInfo.provider);
            if (!zBindAppWidgetIdIfAllowed) {
                if (i > -1) {
                    this.mQsbWidgetHost.deleteHost();
                }
                int iAllocateAppWidgetId = this.mQsbWidgetHost.allocateAppWidgetId();
                zBindAppWidgetIdIfAllowed = appWidgetManager.bindAppWidgetIdIfAllowed(iAllocateAppWidgetId, this.mWidgetInfo.getProfile(), this.mWidgetInfo.provider, bundle);
                if (!zBindAppWidgetIdIfAllowed) {
                    this.mQsbWidgetHost.deleteAppWidgetId(iAllocateAppWidgetId);
                    iAllocateAppWidgetId = -1;
                }
                if (i != iAllocateAppWidgetId) {
                    saveWidgetId(iAllocateAppWidgetId);
                }
                i = iAllocateAppWidgetId;
            }
            if (zBindAppWidgetIdIfAllowed) {
                this.mQsb = (QsbWidgetHostView) this.mQsbWidgetHost.createView(activity, i, this.mWidgetInfo);
                this.mQsb.setId(R.id.qsb_widget);
                if (!Utilities.containsAll(AppWidgetManager.getInstance(activity).getAppWidgetOptions(i), bundle)) {
                    this.mQsb.updateAppWidgetOptions(bundle);
                }
                this.mQsb.setPadding(0, 0, 0, 0);
                this.mQsbWidgetHost.startListening();
                return this.mQsb;
            }
            View defaultView = QsbWidgetHostView.getDefaultView(viewGroup);
            View viewFindViewById = defaultView.findViewById(R.id.btn_qsb_setup);
            viewFindViewById.setVisibility(0);
            viewFindViewById.setOnClickListener(this);
            return defaultView;
        }

        private void saveWidgetId(int i) {
            Utilities.getPrefs(getActivity()).edit().putInt(QSB_WIDGET_ID, i).apply();
        }

        @Override // android.view.View.OnClickListener
        public void onClick(View view) {
            if (BenesseExtension.getDchaState() != 0) {
                return;
            }
            Intent intent = new Intent("android.appwidget.action.APPWIDGET_BIND");
            intent.putExtra(LauncherSettings.Favorites.APPWIDGET_ID, this.mQsbWidgetHost.allocateAppWidgetId());
            intent.putExtra(LauncherSettings.Favorites.APPWIDGET_PROVIDER, this.mWidgetInfo.provider);
            startActivityForResult(intent, 1);
        }

        @Override // android.app.Fragment
        public void onActivityResult(int i, int i2, Intent intent) {
            if (i == 1) {
                if (i2 == -1) {
                    saveWidgetId(intent.getIntExtra(LauncherSettings.Favorites.APPWIDGET_ID, -1));
                    rebindFragment();
                } else {
                    this.mQsbWidgetHost.deleteHost();
                }
            }
        }

        @Override // android.app.Fragment
        public void onResume() {
            super.onResume();
            if (this.mQsb != null && this.mQsb.isReinflateRequired(this.mOrientation)) {
                rebindFragment();
            }
        }

        @Override // android.app.Fragment
        public void onDestroy() {
            this.mQsbWidgetHost.stopListening();
            super.onDestroy();
        }

        private void rebindFragment() {
            if (isQsbEnabled() && this.mWrapper != null && getActivity() != null) {
                this.mWrapper.removeAllViews();
                this.mWrapper.addView(createQsb(this.mWrapper));
            }
        }

        public boolean isQsbEnabled() {
            return false;
        }
    }

    public static AppWidgetProviderInfo getSearchWidgetProvider(Context context) {
        ComponentName globalSearchActivity = ((SearchManager) context.getSystemService("search")).getGlobalSearchActivity();
        AppWidgetProviderInfo appWidgetProviderInfo = null;
        if (globalSearchActivity == null) {
            return null;
        }
        String packageName = globalSearchActivity.getPackageName();
        for (AppWidgetProviderInfo appWidgetProviderInfo2 : AppWidgetManager.getInstance(context).getInstalledProviders()) {
            if (appWidgetProviderInfo2.provider.getPackageName().equals(packageName) && appWidgetProviderInfo2.configure == null) {
                if ((appWidgetProviderInfo2.widgetCategory & 4) != 0) {
                    return appWidgetProviderInfo2;
                }
                if (appWidgetProviderInfo == null) {
                    appWidgetProviderInfo = appWidgetProviderInfo2;
                }
            }
        }
        return appWidgetProviderInfo;
    }

    public static void updateDefaultLayout(Context context, AppWidgetProviderInfo appWidgetProviderInfo) {
        ComponentName componentName = appWidgetProviderInfo.provider;
        if (componentName.getClassName().equals("com.google.android.googlequicksearchbox.SearchWidgetProvider")) {
            try {
                int i = context.getPackageManager().getReceiverInfo(componentName, 128).metaData.getInt("com.google.android.gsa.searchwidget.alt_initial_layout_cqsb", -1);
                if (i != -1) {
                    appWidgetProviderInfo.initialLayout = i;
                }
            } catch (Exception e) {
            }
        }
    }

    private static class QsbWidgetHost extends AppWidgetHost {
        private static final int QSB_WIDGET_HOST_ID = 1026;

        public QsbWidgetHost(Context context) {
            super(context, QSB_WIDGET_HOST_ID);
        }

        @Override // android.appwidget.AppWidgetHost
        protected AppWidgetHostView onCreateView(Context context, int i, AppWidgetProviderInfo appWidgetProviderInfo) {
            return new QsbWidgetHostView(context);
        }
    }
}
