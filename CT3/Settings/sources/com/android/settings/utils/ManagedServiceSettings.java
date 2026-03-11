package com.android.settings.utils;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageItemInfo;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.os.Bundle;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.view.View;
import com.android.settings.R;
import com.android.settings.notification.EmptyTextSettings;
import com.android.settings.utils.ServiceListing;
import java.util.Collections;
import java.util.List;

public abstract class ManagedServiceSettings extends EmptyTextSettings {
    private final Config mConfig = getConfig();
    protected Context mContext;
    private PackageManager mPM;
    protected ServiceListing mServiceListing;

    public static class Config {
        public int emptyText;
        public String intentAction;
        public String noun;
        public String permission;
        public String secondarySetting;
        public String setting;
        public String tag;
        public int warningDialogSummary;
        public int warningDialogTitle;
    }

    protected abstract Config getConfig();

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        this.mContext = getActivity();
        this.mPM = this.mContext.getPackageManager();
        this.mServiceListing = new ServiceListing(this.mContext, this.mConfig);
        this.mServiceListing.addCallback(new ServiceListing.Callback() {
            @Override
            public void onServicesReloaded(List<ServiceInfo> services) {
                ManagedServiceSettings.this.updateList(services);
            }
        });
        setPreferenceScreen(getPreferenceManager().createPreferenceScreen(this.mContext));
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setEmptyText(this.mConfig.emptyText);
    }

    @Override
    public void onResume() {
        super.onResume();
        this.mServiceListing.reload();
        this.mServiceListing.setListening(true);
    }

    @Override
    public void onPause() {
        super.onPause();
        this.mServiceListing.setListening(false);
    }

    public void updateList(List<ServiceInfo> services) {
        PreferenceScreen screen = getPreferenceScreen();
        screen.removeAll();
        Collections.sort(services, new PackageItemInfo.DisplayNameComparator(this.mPM));
        for (ServiceInfo service : services) {
            final ComponentName cn = new ComponentName(service.packageName, service.name);
            final String title = service.loadLabel(this.mPM).toString();
            SwitchPreference pref = new SwitchPreference(getPrefContext());
            pref.setPersistent(false);
            pref.setIcon(service.loadIcon(this.mPM));
            pref.setTitle(title);
            pref.setChecked(this.mServiceListing.isEnabled(cn));
            pref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    boolean enable = ((Boolean) newValue).booleanValue();
                    return ManagedServiceSettings.this.setEnabled(cn, title, enable);
                }
            });
            screen.addPreference(pref);
        }
    }

    protected boolean setEnabled(ComponentName service, String title, boolean enable) {
        if (!enable) {
            this.mServiceListing.setEnabled(service, false);
            return true;
        }
        if (this.mServiceListing.isEnabled(service)) {
            return true;
        }
        new ScaryWarningDialogFragment().setServiceInfo(service, title).show(getFragmentManager(), "dialog");
        return false;
    }

    public class ScaryWarningDialogFragment extends DialogFragment {
        public ScaryWarningDialogFragment() {
        }

        public ScaryWarningDialogFragment setServiceInfo(ComponentName cn, String label) {
            Bundle args = new Bundle();
            args.putString("c", cn.flattenToString());
            args.putString("l", label);
            setArguments(args);
            return this;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            Bundle args = getArguments();
            String label = args.getString("l");
            final ComponentName cn = ComponentName.unflattenFromString(args.getString("c"));
            String title = getResources().getString(ManagedServiceSettings.this.mConfig.warningDialogTitle, label);
            String summary = getResources().getString(ManagedServiceSettings.this.mConfig.warningDialogSummary, label);
            return new AlertDialog.Builder(ManagedServiceSettings.this.mContext).setMessage(summary).setTitle(title).setCancelable(true).setPositiveButton(R.string.allow, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                    ManagedServiceSettings.this.mServiceListing.setEnabled(cn, true);
                }
            }).setNegativeButton(R.string.deny, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                }
            }).create();
        }
    }
}
