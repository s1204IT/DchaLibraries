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
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.Switch;
import android.widget.TextView;
import com.android.settings.DreamBackend;
import com.android.settings.widget.SwitchBar;
import java.util.List;

public class DreamSettings extends SettingsPreferenceFragment implements SwitchBar.OnSwitchChangeListener {
    private static final String TAG = DreamSettings.class.getSimpleName();
    private DreamInfoAdapter mAdapter;
    private DreamBackend mBackend;
    private Context mContext;
    private MenuItem[] mMenuItemsWhenEnabled;
    private final PackageReceiver mPackageReceiver = new PackageReceiver();
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
    public void onCreate(Bundle icicle) {
        logd("onCreate(%s)", icicle);
        super.onCreate(icicle);
        this.mBackend = new DreamBackend(getActivity());
        setHasOptionsMenu(true);
    }

    @Override
    public void onSwitchChanged(Switch switchView, boolean isChecked) {
        if (!this.mRefreshing) {
            this.mBackend.setEnabled(isChecked);
            refreshFromBackend();
        }
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
        ListView listView = getListView();
        listView.setItemsCanFocus(true);
        TextView emptyView = (TextView) getView().findViewById(android.R.id.empty);
        emptyView.setText(R.string.screensaver_settings_disabled_prompt);
        listView.setEmptyView(emptyView);
        this.mAdapter = new DreamInfoAdapter(this.mContext);
        listView.setAdapter(this.mAdapter);
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
        return dialogId == 1 ? createWhenToDreamDialog() : super.onCreateDialog(dialogId);
    }

    private Dialog createWhenToDreamDialog() {
        int initialSelection = 2;
        CharSequence[] items = {this.mContext.getString(R.string.screensaver_settings_summary_dock), this.mContext.getString(R.string.screensaver_settings_summary_sleep), this.mContext.getString(R.string.screensaver_settings_summary_either_short)};
        if (!this.mBackend.isActivatedOnDock() || !this.mBackend.isActivatedOnSleep()) {
            initialSelection = this.mBackend.isActivatedOnDock() ? 0 : this.mBackend.isActivatedOnSleep() ? 1 : -1;
        }
        return new AlertDialog.Builder(this.mContext).setTitle(R.string.screensaver_settings_when_to_dream).setSingleChoiceItems(items, initialSelection, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {
                DreamSettings.this.mBackend.setActivatedOnDock(item == 0 || item == 2);
                DreamSettings.this.mBackend.setActivatedOnSleep(item == 1 || item == 2);
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
        return !isEnabled ? context.getString(R.string.screensaver_settings_summary_off) : backend.getActiveDreamName();
    }

    private void refreshFromBackend() {
        logd("refreshFromBackend()", new Object[0]);
        this.mRefreshing = true;
        boolean dreamsEnabled = this.mBackend.isEnabled();
        if (this.mSwitchBar.isChecked() != dreamsEnabled) {
            this.mSwitchBar.setChecked(dreamsEnabled);
        }
        this.mAdapter.clear();
        if (dreamsEnabled) {
            List<DreamBackend.DreamInfo> dreamInfos = this.mBackend.getDreamInfos();
            this.mAdapter.addAll(dreamInfos);
        }
        if (this.mMenuItemsWhenEnabled != null) {
            MenuItem[] arr$ = this.mMenuItemsWhenEnabled;
            for (MenuItem menuItem : arr$) {
                menuItem.setEnabled(dreamsEnabled);
            }
        }
        this.mRefreshing = false;
    }

    private static void logd(String msg, Object... args) {
    }

    private class DreamInfoAdapter extends ArrayAdapter<DreamBackend.DreamInfo> {
        private final LayoutInflater mInflater;

        public DreamInfoAdapter(Context context) {
            super(context, 0);
            this.mInflater = (LayoutInflater) context.getSystemService("layout_inflater");
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            DreamBackend.DreamInfo dreamInfo = getItem(position);
            DreamSettings.logd("getView(%s)", dreamInfo.caption);
            final View row = convertView != null ? convertView : createDreamInfoRow(parent);
            row.setTag(dreamInfo);
            ((ImageView) row.findViewById(android.R.id.icon)).setImageDrawable(dreamInfo.icon);
            ((TextView) row.findViewById(android.R.id.title)).setText(dreamInfo.caption);
            RadioButton radioButton = (RadioButton) row.findViewById(android.R.id.button1);
            radioButton.setChecked(dreamInfo.isActive);
            radioButton.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    row.onTouchEvent(event);
                    return false;
                }
            });
            boolean showSettings = dreamInfo.settingsComponentName != null;
            View settingsDivider = row.findViewById(R.id.divider);
            settingsDivider.setVisibility(showSettings ? 0 : 4);
            ImageView settingsButton = (ImageView) row.findViewById(android.R.id.button2);
            settingsButton.setVisibility(showSettings ? 0 : 4);
            settingsButton.setAlpha(dreamInfo.isActive ? 1.0f : 0.4f);
            settingsButton.setEnabled(dreamInfo.isActive);
            settingsButton.setFocusable(dreamInfo.isActive);
            settingsButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    DreamSettings.this.mBackend.launchSettings((DreamBackend.DreamInfo) row.getTag());
                }
            });
            return row;
        }

        private View createDreamInfoRow(ViewGroup parent) {
            final View row = this.mInflater.inflate(R.layout.dream_info_row, parent, false);
            View header = row.findViewById(android.R.id.widget_frame);
            header.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    v.setPressed(true);
                    DreamInfoAdapter.this.activate((DreamBackend.DreamInfo) row.getTag());
                }
            });
            return row;
        }

        private DreamBackend.DreamInfo getCurrentSelection() {
            for (int i = 0; i < getCount(); i++) {
                DreamBackend.DreamInfo dreamInfo = getItem(i);
                if (dreamInfo.isActive) {
                    return dreamInfo;
                }
            }
            return null;
        }

        private void activate(DreamBackend.DreamInfo dreamInfo) {
            if (!dreamInfo.equals(getCurrentSelection())) {
                for (int i = 0; i < getCount(); i++) {
                    getItem(i).isActive = false;
                }
                dreamInfo.isActive = true;
                DreamSettings.this.mBackend.setActiveDream(dreamInfo.componentName);
                notifyDataSetChanged();
            }
        }
    }

    private class PackageReceiver extends BroadcastReceiver {
        private PackageReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            DreamSettings.logd("PackageReceiver.onReceive", new Object[0]);
            DreamSettings.this.refreshFromBackend();
        }
    }
}
