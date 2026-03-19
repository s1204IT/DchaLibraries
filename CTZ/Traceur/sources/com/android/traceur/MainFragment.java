package com.android.traceur;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v14.preference.MultiSelectListPreference;
import android.support.v14.preference.PreferenceFragment;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class MainFragment extends PreferenceFragment {
    private AlertDialog mAlertDialog;
    private ListPreference mBufferSize;
    private SharedPreferences mPrefs;
    private BroadcastReceiver mRefreshReceiver;
    private boolean mRefreshing;
    private MultiSelectListPreference mTags;
    private SwitchPreference mTracingOn;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.mPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
        this.mTracingOn = (SwitchPreference) findPreference(getActivity().getString(R.string.pref_key_tracing_on));
        this.mTracingOn.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Receiver.updateTracing(MainFragment.this.getContext());
                return true;
            }
        });
        this.mTags = (MultiSelectListPreference) findPreference(getContext().getString(R.string.pref_key_tags));
        this.mTags.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object obj) {
                if (MainFragment.this.mRefreshing) {
                    return true;
                }
                Set<String> set = (Set) obj;
                TreeMap<String, String> treeMapAtraceListCategories = AtraceUtils.atraceListCategories();
                ArrayList arrayList = new ArrayList(set.size());
                for (String str : set) {
                    if (treeMapAtraceListCategories.containsKey(str)) {
                        arrayList.add(str);
                    }
                }
                set.clear();
                set.addAll(arrayList);
                return true;
            }
        });
        this.mBufferSize = (ListPreference) findPreference(getContext().getString(R.string.pref_key_buffer_size));
        this.mBufferSize.setValue(this.mPrefs.getString(getContext().getString(R.string.pref_key_buffer_size), getContext().getString(R.string.default_buffer_size)));
        findPreference("restore_default_tags").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                MainFragment.this.refreshTags(true);
                Toast.makeText(MainFragment.this.getContext(), MainFragment.this.getContext().getString(R.string.default_categories_restored), 0).show();
                return true;
            }
        });
        findPreference(getString(R.string.pref_key_quick_setting)).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Receiver.updateQuickSettings(MainFragment.this.getContext());
                return true;
            }
        });
        findPreference("clear_saved_traces").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                new AlertDialog.Builder(MainFragment.this.getContext()).setTitle(R.string.clear_saved_traces_question).setMessage(R.string.all_traces_will_be_deleted).setPositiveButton(R.string.clear, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        AtraceUtils.clearSavedTraces();
                    }
                }).setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                }).create().show();
                return true;
            }
        });
        refreshTags();
        this.mRefreshReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                MainFragment.this.refreshTags();
            }
        };
    }

    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        return super.onCreateView(layoutInflater, viewGroup, bundle);
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().registerReceiver(this.mRefreshReceiver, new IntentFilter("com.android.traceur.REFRESH_TAGS"));
        Receiver.updateTracing(getContext());
    }

    @Override
    public void onPause() {
        getActivity().unregisterReceiver(this.mRefreshReceiver);
        if (this.mAlertDialog != null) {
            this.mAlertDialog.cancel();
            this.mAlertDialog = null;
        }
        super.onPause();
    }

    @Override
    public void onCreatePreferences(Bundle bundle, String str) {
        addPreferencesFromResource(R.xml.main);
    }

    private void refreshTags() {
        refreshTags(false);
    }

    private void refreshTags(boolean z) {
        this.mTracingOn.setChecked(this.mTracingOn.getPreferenceManager().getSharedPreferences().getBoolean(this.mTracingOn.getKey(), false));
        Set<Map.Entry<String, String>> setEntrySet = AtraceUtils.atraceListCategories().entrySet();
        ArrayList arrayList = new ArrayList(setEntrySet.size());
        ArrayList arrayList2 = new ArrayList(setEntrySet.size());
        for (Map.Entry<String, String> entry : setEntrySet) {
            arrayList.add(entry.getKey() + ": " + entry.getValue());
            arrayList2.add(entry.getKey());
        }
        this.mRefreshing = true;
        try {
            this.mTags.setEntries((CharSequence[]) arrayList.toArray(new String[0]));
            this.mTags.setEntryValues((CharSequence[]) arrayList2.toArray(new String[0]));
            if (z || !this.mPrefs.contains(getContext().getString(R.string.pref_key_tags))) {
                this.mTags.setValues(Receiver.getDefaultTagList());
            }
        } finally {
            this.mRefreshing = false;
        }
    }
}
