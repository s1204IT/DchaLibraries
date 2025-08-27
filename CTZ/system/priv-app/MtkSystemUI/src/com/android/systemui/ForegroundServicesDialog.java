package com.android.systemui;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.BenesseExtension;
import android.os.Bundle;
import android.util.IconDrawableFactory;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import com.android.internal.app.AlertActivity;
import com.android.internal.app.AlertController;
import com.android.internal.logging.MetricsLogger;
import java.util.ArrayList;

/* loaded from: classes.dex */
public final class ForegroundServicesDialog extends AlertActivity implements DialogInterface.OnClickListener, AdapterView.OnItemSelectedListener, AlertController.AlertParams.OnPrepareListViewListener {
    private PackageItemAdapter mAdapter;
    private DialogInterface.OnClickListener mAppClickListener = new DialogInterface.OnClickListener() { // from class: com.android.systemui.ForegroundServicesDialog.1
        @Override // android.content.DialogInterface.OnClickListener
        public void onClick(DialogInterface dialogInterface, int i) {
            if (BenesseExtension.getDchaState() == 0) {
                String str = ForegroundServicesDialog.this.mAdapter.getItem(i).packageName;
                Intent intent = new Intent("android.settings.APPLICATION_DETAILS_SETTINGS");
                intent.setData(Uri.fromParts("package", str, null));
                ForegroundServicesDialog.this.startActivity(intent);
            }
            ForegroundServicesDialog.this.finish();
        }
    };
    LayoutInflater mInflater;
    private MetricsLogger mMetricsLogger;
    private String[] mPackages;

    /* JADX DEBUG: Multi-variable search result rejected for r3v0, resolved type: com.android.systemui.ForegroundServicesDialog */
    /* JADX WARN: Multi-variable type inference failed */
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        Dependency.initDependencies(getApplicationContext());
        this.mMetricsLogger = (MetricsLogger) Dependency.get(MetricsLogger.class);
        this.mInflater = LayoutInflater.from(this);
        this.mAdapter = new PackageItemAdapter(this);
        AlertController.AlertParams alertParams = this.mAlertParams;
        alertParams.mAdapter = this.mAdapter;
        alertParams.mOnClickListener = this.mAppClickListener;
        alertParams.mCustomTitleView = this.mInflater.inflate(R.layout.foreground_service_title, (ViewGroup) null);
        alertParams.mIsSingleChoice = true;
        alertParams.mOnItemSelectedListener = this;
        alertParams.mPositiveButtonText = getString(android.R.string.capability_title_canRequestFilterKeyEvents);
        alertParams.mPositiveButtonListener = this;
        alertParams.mOnPrepareListViewListener = this;
        updateApps(getIntent());
        if (this.mPackages == null) {
            Log.w("ForegroundServicesDialog", "No packages supplied");
            finish();
        } else {
            setupAlert();
        }
    }

    protected void onResume() {
        super.onResume();
        this.mMetricsLogger.visible(944);
    }

    protected void onPause() {
        super.onPause();
        this.mMetricsLogger.hidden(944);
    }

    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        updateApps(intent);
    }

    protected void onStop() {
        super.onStop();
        if (!isChangingConfigurations()) {
            finish();
        }
    }

    void updateApps(Intent intent) {
        this.mPackages = intent.getStringArrayExtra("packages");
        if (this.mPackages != null) {
            this.mAdapter.setPackages(this.mPackages);
        }
    }

    public void onPrepareListView(ListView listView) {
    }

    @Override // android.content.DialogInterface.OnClickListener
    public void onClick(DialogInterface dialogInterface, int i) {
        finish();
    }

    @Override // android.widget.AdapterView.OnItemSelectedListener
    public void onItemSelected(AdapterView adapterView, View view, int i, long j) {
    }

    @Override // android.widget.AdapterView.OnItemSelectedListener
    public void onNothingSelected(AdapterView adapterView) {
    }

    private static class PackageItemAdapter extends ArrayAdapter<ApplicationInfo> {
        final IconDrawableFactory mIconDrawableFactory;
        final LayoutInflater mInflater;
        final PackageManager mPm;

        public PackageItemAdapter(Context context) {
            super(context, R.layout.foreground_service_item);
            this.mPm = context.getPackageManager();
            this.mInflater = LayoutInflater.from(context);
            this.mIconDrawableFactory = IconDrawableFactory.newInstance(context, true);
        }

        public void setPackages(String[] strArr) {
            clear();
            ArrayList arrayList = new ArrayList();
            for (String str : strArr) {
                try {
                    arrayList.add(this.mPm.getApplicationInfo(str, 4202496));
                } catch (PackageManager.NameNotFoundException e) {
                }
            }
            arrayList.sort(new ApplicationInfo.DisplayNameComparator(this.mPm));
            addAll(arrayList);
        }

        @Override // android.widget.ArrayAdapter, android.widget.Adapter
        public View getView(int i, View view, ViewGroup viewGroup) {
            if (view == null) {
                view = this.mInflater.inflate(R.layout.foreground_service_item, viewGroup, false);
            }
            ((ImageView) view.findViewById(R.id.app_icon)).setImageDrawable(this.mIconDrawableFactory.getBadgedIcon(getItem(i)));
            ((TextView) view.findViewById(R.id.app_name)).setText(getItem(i).loadLabel(this.mPm));
            return view;
        }
    }
}
