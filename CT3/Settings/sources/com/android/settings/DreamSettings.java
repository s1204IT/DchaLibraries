package com.android.settings;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.Switch;
import android.widget.TextView;
import com.android.settings.widget.SwitchBar;
import com.android.settingslib.dream.DreamBackend;
import java.util.List;

public class DreamSettings extends SettingsPreferenceFragment implements SwitchBar.OnSwitchChangeListener {
    private static final String TAG = DreamSettings.class.getSimpleName();
    private DreamBackend mBackend;
    private Context mContext;
    private MenuItem[] mMenuItemsWhenEnabled;
    private final PackageReceiver mPackageReceiver = new PackageReceiver(this, null);
    private boolean mRefreshing;
    private SwitchBar mSwitchBar;

    @Override
    public int getHelpResource() {
        return R.string.help_url_dreams;
    }

    @Override
    public void onAttach(Activity activity) {
        logd("onAttach(%s)", activity.getClass().getSimpleName());
        super.onAttach(activity);
        this.mContext = activity;
    }

    @Override
    protected int getMetricsCategory() {
        return 47;
    }

    @Override
    public void onCreate(Bundle icicle) {
        logd("onCreate(%s)", icicle);
        super.onCreate(icicle);
        this.mBackend = new DreamBackend(getActivity());
        setHasOptionsMenu(true);
    }

    @Override
    public void onSwitchChanged(Switch switchView, boolean isChecked) {
        if (this.mRefreshing) {
            return;
        }
        this.mBackend.setEnabled(isChecked);
        refreshFromBackend();
    }

    @Override
    public void onStart() {
        logd("onStart()", new Object[0]);
        super.onStart();
    }

    @Override
    public void onDestroyView() {
        logd("onDestroyView()", new Object[0]);
        super.onDestroyView();
        this.mSwitchBar.removeOnSwitchChangeListener(this);
        this.mSwitchBar.hide();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        logd("onActivityCreated(%s)", savedInstanceState);
        super.onActivityCreated(savedInstanceState);
        TextView emptyView = (TextView) getView().findViewById(android.R.id.empty);
        emptyView.setText(R.string.screensaver_settings_disabled_prompt);
        setEmptyView(emptyView);
        SettingsActivity sa = (SettingsActivity) getActivity();
        this.mSwitchBar = sa.getSwitchBar();
        this.mSwitchBar.addOnSwitchChangeListener(this);
        this.mSwitchBar.show();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        logd("onCreateOptionsMenu()", new Object[0]);
        boolean isEnabled = this.mBackend.isEnabled();
        MenuItem start = createMenuItem(menu, R.string.screensaver_settings_dream_start, 0, isEnabled, new Runnable() {
            @Override
            public void run() {
                DreamSettings.this.mBackend.startDreaming();
            }
        });
        super.onCreateOptionsMenu(menu, inflater);
        this.mMenuItemsWhenEnabled = new MenuItem[]{start};
    }

    private MenuItem createMenuItem(Menu menu, int titleRes, int actionEnum, boolean isEnabled, final Runnable onClick) {
        MenuItem item = menu.add(titleRes);
        item.setShowAsAction(actionEnum);
        item.setEnabled(isEnabled);
        item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item2) {
                onClick.run();
                return true;
            }
        });
        return item;
    }

    @Override
    public Dialog onCreateDialog(int dialogId) {
        logd("onCreateDialog(%s)", Integer.valueOf(dialogId));
        if (dialogId == 1) {
            return createWhenToDreamDialog();
        }
        return super.onCreateDialog(dialogId);
    }

    private Dialog createWhenToDreamDialog() {
        int initialSelection;
        CharSequence[] items = {this.mContext.getString(R.string.screensaver_settings_summary_dock), this.mContext.getString(R.string.screensaver_settings_summary_sleep), this.mContext.getString(R.string.screensaver_settings_summary_either_short)};
        if (this.mBackend.isActivatedOnDock() && this.mBackend.isActivatedOnSleep()) {
            initialSelection = 2;
        } else if (this.mBackend.isActivatedOnDock()) {
            initialSelection = 0;
        } else {
            initialSelection = this.mBackend.isActivatedOnSleep() ? 1 : -1;
        }
        return new AlertDialog.Builder(this.mContext).setTitle(R.string.screensaver_settings_when_to_dream).setSingleChoiceItems(items, initialSelection, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {
                boolean z = true;
                DreamSettings.this.mBackend.setActivatedOnDock(item == 0 || item == 2);
                DreamBackend dreamBackend = DreamSettings.this.mBackend;
                if (item != 1 && item != 2) {
                    z = false;
                }
                dreamBackend.setActivatedOnSleep(z);
                dialog.dismiss();
            }
        }).create();
    }

    @Override
    public void onPause() {
        logd("onPause()", new Object[0]);
        super.onPause();
        this.mContext.unregisterReceiver(this.mPackageReceiver);
    }

    @Override
    public void onResume() {
        logd("onResume()", new Object[0]);
        super.onResume();
        refreshFromBackend();
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.PACKAGE_ADDED");
        filter.addAction("android.intent.action.PACKAGE_CHANGED");
        filter.addAction("android.intent.action.PACKAGE_REMOVED");
        filter.addAction("android.intent.action.PACKAGE_REPLACED");
        filter.addDataScheme("package");
        this.mContext.registerReceiver(this.mPackageReceiver, filter);
    }

    public static CharSequence getSummaryTextWithDreamName(Context context) {
        DreamBackend backend = new DreamBackend(context);
        boolean isEnabled = backend.isEnabled();
        if (!isEnabled) {
            return context.getString(R.string.screensaver_settings_summary_off);
        }
        return backend.getActiveDreamName();
    }

    public void refreshFromBackend() {
        logd("refreshFromBackend()", new Object[0]);
        this.mRefreshing = true;
        boolean dreamsEnabled = this.mBackend.isEnabled();
        if (this.mSwitchBar.isChecked() != dreamsEnabled) {
            this.mSwitchBar.setChecked(dreamsEnabled);
        }
        if (getPreferenceScreen() == null) {
            setPreferenceScreen(getPreferenceManager().createPreferenceScreen(getContext()));
        }
        getPreferenceScreen().removeAll();
        if (dreamsEnabled) {
            List<DreamBackend.DreamInfo> dreamInfos = this.mBackend.getDreamInfos();
            int N = dreamInfos.size();
            for (int i = 0; i < N; i++) {
                getPreferenceScreen().addPreference(new DreamInfoPreference(getPrefContext(), dreamInfos.get(i)));
            }
        }
        if (this.mMenuItemsWhenEnabled != null) {
            for (MenuItem menuItem : this.mMenuItemsWhenEnabled) {
                menuItem.setEnabled(dreamsEnabled);
            }
        }
        this.mRefreshing = false;
    }

    public static void logd(String msg, Object... args) {
    }

    private class DreamInfoPreference extends Preference {
        private final DreamBackend.DreamInfo mInfo;

        public DreamInfoPreference(Context context, DreamBackend.DreamInfo info) {
            super(context);
            this.mInfo = info;
            setLayoutResource(R.layout.dream_info_row);
            setTitle(this.mInfo.caption);
            setIcon(this.mInfo.icon);
        }

        @Override
        public void onBindViewHolder(final PreferenceViewHolder holder) {
            super.onBindViewHolder(holder);
            RadioButton radioButton = (RadioButton) holder.findViewById(android.R.id.button1);
            radioButton.setChecked(this.mInfo.isActive);
            radioButton.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    holder.itemView.onTouchEvent(event);
                    return false;
                }
            });
            boolean showSettings = this.mInfo.settingsComponentName != null;
            View settingsDivider = holder.findViewById(R.id.divider);
            settingsDivider.setVisibility(showSettings ? 0 : 4);
            ImageView settingsButton = (ImageView) holder.findViewById(android.R.id.button2);
            settingsButton.setVisibility(showSettings ? 0 : 4);
            settingsButton.setAlpha(this.mInfo.isActive ? 1.0f : 0.4f);
            settingsButton.setEnabled(this.mInfo.isActive);
            settingsButton.setFocusable(this.mInfo.isActive);
            settingsButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    DreamSettings.this.mBackend.launchSettings(DreamInfoPreference.this.mInfo);
                }
            });
        }

        @Override
        public void performClick() {
            if (this.mInfo.isActive) {
                return;
            }
            for (int i = 0; i < DreamSettings.this.getPreferenceScreen().getPreferenceCount(); i++) {
                DreamInfoPreference preference = (DreamInfoPreference) DreamSettings.this.getPreferenceScreen().getPreference(i);
                preference.mInfo.isActive = false;
                preference.notifyChanged();
            }
            this.mInfo.isActive = true;
            DreamSettings.this.mBackend.setActiveDream(this.mInfo.componentName);
            notifyChanged();
        }
    }

    private class PackageReceiver extends BroadcastReceiver {
        PackageReceiver(DreamSettings this$0, PackageReceiver packageReceiver) {
            this();
        }

        private PackageReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            DreamSettings.logd("PackageReceiver.onReceive", new Object[0]);
            DreamSettings.this.refreshFromBackend();
        }
    }
}
