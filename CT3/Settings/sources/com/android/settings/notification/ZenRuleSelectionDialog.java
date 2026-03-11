package com.android.settings.notification;

import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.service.notification.ZenModeConfig;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.android.settings.R;
import com.android.settings.utils.ZenServiceListing;
import java.lang.ref.WeakReference;
import java.text.Collator;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

public abstract class ZenRuleSelectionDialog {
    private static final boolean DEBUG = ZenModeSettings.DEBUG;
    private static final Comparator<ZenRuleInfo> RULE_TYPE_COMPARATOR = new Comparator<ZenRuleInfo>() {
        private final Collator mCollator = Collator.getInstance();

        @Override
        public int compare(ZenRuleInfo lhs, ZenRuleInfo rhs) {
            int byAppName = this.mCollator.compare(lhs.packageLabel, rhs.packageLabel);
            if (byAppName != 0) {
                return byAppName;
            }
            return this.mCollator.compare(lhs.title, rhs.title);
        }
    };
    private final Context mContext;
    private final AlertDialog mDialog;
    private NotificationManager mNm;
    private final PackageManager mPm;
    private final LinearLayout mRuleContainer;
    private final ZenServiceListing mServiceListing;
    private final ZenServiceListing.Callback mServiceListingCallback = new ZenServiceListing.Callback() {
        @Override
        public void onServicesReloaded(Set<ServiceInfo> services) {
            if (ZenRuleSelectionDialog.DEBUG) {
                Log.d("ZenRuleSelectionDialog", "Services reloaded: count=" + services.size());
            }
            Set<ZenRuleInfo> externalRuleTypes = new TreeSet<>((Comparator<? super ZenRuleInfo>) ZenRuleSelectionDialog.RULE_TYPE_COMPARATOR);
            for (ServiceInfo serviceInfo : services) {
                ZenRuleInfo ri = ZenModeAutomationSettings.getRuleInfo(ZenRuleSelectionDialog.this.mPm, serviceInfo);
                if (ri != null && ri.configurationActivity != null && ZenRuleSelectionDialog.this.mNm.isNotificationPolicyAccessGrantedForPackage(ri.packageName) && (ri.ruleInstanceLimit <= 0 || ri.ruleInstanceLimit >= ZenRuleSelectionDialog.this.mNm.getRuleInstanceCount(serviceInfo.getComponentName()) + 1)) {
                    externalRuleTypes.add(ri);
                }
            }
            ZenRuleSelectionDialog.this.bindExternalRules(externalRuleTypes);
        }
    };

    public abstract void onExternalRuleSelected(ZenRuleInfo zenRuleInfo);

    public abstract void onSystemRuleSelected(ZenRuleInfo zenRuleInfo);

    public ZenRuleSelectionDialog(Context context, ZenServiceListing serviceListing) {
        this.mContext = context;
        this.mPm = context.getPackageManager();
        this.mNm = (NotificationManager) context.getSystemService("notification");
        this.mServiceListing = serviceListing;
        View v = LayoutInflater.from(context).inflate(R.layout.zen_rule_type_selection, (ViewGroup) null, false);
        this.mRuleContainer = (LinearLayout) v.findViewById(R.id.rule_container);
        if (this.mServiceListing != null) {
            bindType(defaultNewEvent());
            bindType(defaultNewSchedule());
            this.mServiceListing.addZenCallback(this.mServiceListingCallback);
            this.mServiceListing.reloadApprovedServices();
        }
        this.mDialog = new AlertDialog.Builder(context).setTitle(R.string.zen_mode_choose_rule_type).setView(v).setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                if (ZenRuleSelectionDialog.this.mServiceListing == null) {
                    return;
                }
                ZenRuleSelectionDialog.this.mServiceListing.removeZenCallback(ZenRuleSelectionDialog.this.mServiceListingCallback);
            }
        }).setNegativeButton(R.string.cancel, (DialogInterface.OnClickListener) null).create();
    }

    public void show() {
        this.mDialog.show();
    }

    private void bindType(final ZenRuleInfo ri) {
        try {
            ApplicationInfo info = this.mPm.getApplicationInfo(ri.packageName, 0);
            LinearLayout v = (LinearLayout) LayoutInflater.from(this.mContext).inflate(R.layout.zen_rule_type, (ViewGroup) null, false);
            LoadIconTask task = new LoadIconTask((ImageView) v.findViewById(R.id.icon));
            task.execute(info);
            ((TextView) v.findViewById(R.id.title)).setText(ri.title);
            if (!ri.isSystem) {
                TextView subtitle = (TextView) v.findViewById(R.id.subtitle);
                subtitle.setText(info.loadLabel(this.mPm));
                subtitle.setVisibility(0);
            }
            v.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v2) {
                    ZenRuleSelectionDialog.this.mDialog.dismiss();
                    if (ri.isSystem) {
                        ZenRuleSelectionDialog.this.onSystemRuleSelected(ri);
                    } else {
                        ZenRuleSelectionDialog.this.onExternalRuleSelected(ri);
                    }
                }
            });
            this.mRuleContainer.addView(v);
        } catch (PackageManager.NameNotFoundException e) {
        }
    }

    private ZenRuleInfo defaultNewSchedule() {
        ZenModeConfig.ScheduleInfo schedule = new ZenModeConfig.ScheduleInfo();
        schedule.days = ZenModeConfig.ALL_DAYS;
        schedule.startHour = 22;
        schedule.endHour = 7;
        ZenRuleInfo rt = new ZenRuleInfo();
        rt.settingsAction = "android.settings.ZEN_MODE_SCHEDULE_RULE_SETTINGS";
        rt.title = this.mContext.getString(R.string.zen_schedule_rule_type_name);
        rt.packageName = ZenModeConfig.getEventConditionProvider().getPackageName();
        rt.defaultConditionId = ZenModeConfig.toScheduleConditionId(schedule);
        rt.serviceComponent = ZenModeConfig.getScheduleConditionProvider();
        rt.isSystem = true;
        return rt;
    }

    private ZenRuleInfo defaultNewEvent() {
        ZenModeConfig.EventInfo event = new ZenModeConfig.EventInfo();
        event.calendar = null;
        event.reply = 0;
        ZenRuleInfo rt = new ZenRuleInfo();
        rt.settingsAction = "android.settings.ZEN_MODE_EVENT_RULE_SETTINGS";
        rt.title = this.mContext.getString(R.string.zen_event_rule_type_name);
        rt.packageName = ZenModeConfig.getScheduleConditionProvider().getPackageName();
        rt.defaultConditionId = ZenModeConfig.toEventConditionId(event);
        rt.serviceComponent = ZenModeConfig.getEventConditionProvider();
        rt.isSystem = true;
        return rt;
    }

    public void bindExternalRules(Set<ZenRuleInfo> externalRuleTypes) {
        for (ZenRuleInfo ri : externalRuleTypes) {
            bindType(ri);
        }
    }

    private class LoadIconTask extends AsyncTask<ApplicationInfo, Void, Drawable> {
        private final WeakReference<ImageView> viewReference;

        public LoadIconTask(ImageView view) {
            this.viewReference = new WeakReference<>(view);
        }

        @Override
        public Drawable doInBackground(ApplicationInfo... params) {
            return params[0].loadIcon(ZenRuleSelectionDialog.this.mPm);
        }

        @Override
        public void onPostExecute(Drawable icon) {
            ImageView view;
            if (icon == null || (view = this.viewReference.get()) == null) {
                return;
            }
            view.setImageDrawable(icon);
        }
    }
}
