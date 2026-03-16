package com.android.settings.print;

import android.R;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.LoaderManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.ContentObserver;
import android.database.DataSetObserver;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.print.PrintManager;
import android.print.PrinterDiscoverySession;
import android.print.PrinterId;
import android.print.PrinterInfo;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityManager;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.Switch;
import android.widget.TextView;
import com.android.settings.SettingsActivity;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.widget.SwitchBar;
import com.android.settings.widget.ToggleSwitch;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PrintServiceSettingsFragment extends SettingsPreferenceFragment implements DialogInterface.OnClickListener, SwitchBar.OnSwitchChangeListener {
    private Intent mAddPrintersIntent;
    private CharSequence mAddPrintersTitle;
    private ComponentName mComponentName;
    private CharSequence mEnableWarningMessage;
    private CharSequence mEnableWarningTitle;
    private int mLastUnfilteredItemCount;
    private CharSequence mOldActivityTitle;
    private String mPreferenceKey;
    private PrintersAdapter mPrintersAdapter;
    private SearchView mSearchView;
    private boolean mServiceEnabled;
    private Intent mSettingsIntent;
    private CharSequence mSettingsTitle;
    private SwitchBar mSwitchBar;
    private ToggleSwitch mToggleSwitch;
    private final SettingsContentObserver mSettingsContentObserver = new SettingsContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange, Uri uri) {
            PrintServiceSettingsFragment.this.updateUiForServiceState();
        }
    };
    private final DataSetObserver mDataObserver = new DataSetObserver() {
        @Override
        public void onChanged() {
            invalidateOptionsMenuIfNeeded();
            PrintServiceSettingsFragment.this.updateEmptyView();
        }

        @Override
        public void onInvalidated() {
            invalidateOptionsMenuIfNeeded();
        }

        private void invalidateOptionsMenuIfNeeded() {
            int unfilteredItemCount = PrintServiceSettingsFragment.this.mPrintersAdapter.getUnfilteredCount();
            if ((PrintServiceSettingsFragment.this.mLastUnfilteredItemCount <= 0 && unfilteredItemCount > 0) || (PrintServiceSettingsFragment.this.mLastUnfilteredItemCount > 0 && unfilteredItemCount <= 0)) {
                PrintServiceSettingsFragment.this.getActivity().invalidateOptionsMenu();
            }
            PrintServiceSettingsFragment.this.mLastUnfilteredItemCount = unfilteredItemCount;
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        this.mSettingsContentObserver.register(getContentResolver());
        updateEmptyView();
        updateUiForServiceState();
    }

    @Override
    public void onPause() {
        this.mSettingsContentObserver.unregister(getContentResolver());
        if (this.mSearchView != null) {
            this.mSearchView.setOnQueryTextListener(null);
        }
        super.onPause();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initComponents();
        updateUiForArguments();
    }

    @Override
    public void onDestroyView() {
        if (this.mOldActivityTitle != null) {
            getActivity().getActionBar().setTitle(this.mOldActivityTitle);
        }
        super.onDestroyView();
        this.mSwitchBar.removeOnSwitchChangeListener(this);
        this.mSwitchBar.hide();
    }

    private void onPreferenceToggled(String preferenceKey, boolean enabled) {
        ComponentName service = ComponentName.unflattenFromString(preferenceKey);
        List<ComponentName> services = PrintSettingsUtils.readEnabledPrintServices(getActivity());
        if (enabled) {
            services.add(service);
        } else {
            services.remove(service);
        }
        PrintSettingsUtils.writeEnabledPrintServices(getActivity(), services);
    }

    @Override
    public Dialog onCreateDialog(int dialogId) {
        switch (dialogId) {
            case 1:
                CharSequence title = this.mEnableWarningTitle;
                CharSequence message = this.mEnableWarningMessage;
                return new AlertDialog.Builder(getActivity()).setTitle(title).setMessage(message).setCancelable(true).setPositiveButton(R.string.ok, this).setNegativeButton(R.string.cancel, this).create();
            default:
                throw new IllegalArgumentException();
        }
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        switch (which) {
            case -2:
                this.mSwitchBar.setCheckedInternal(false);
                getArguments().putBoolean("EXTRA_CHECKED", false);
                onPreferenceToggled(this.mPreferenceKey, false);
                return;
            case -1:
                this.mSwitchBar.setCheckedInternal(true);
                getArguments().putBoolean("EXTRA_CHECKED", true);
                onPreferenceToggled(this.mPreferenceKey, true);
                return;
            default:
                throw new IllegalArgumentException();
        }
    }

    private void updateEmptyView() {
        ListView listView = getListView();
        ViewGroup contentRoot = (ViewGroup) listView.getParent();
        View emptyView = listView.getEmptyView();
        if (!this.mToggleSwitch.isChecked()) {
            if (emptyView != null && emptyView.getId() != com.android.settings.R.id.empty_print_state) {
                contentRoot.removeView(emptyView);
                emptyView = null;
            }
            if (emptyView == null) {
                View emptyView2 = getActivity().getLayoutInflater().inflate(com.android.settings.R.layout.empty_print_state, contentRoot, false);
                ImageView iconView = (ImageView) emptyView2.findViewById(com.android.settings.R.id.icon);
                iconView.setContentDescription(getString(com.android.settings.R.string.print_service_disabled));
                TextView textView = (TextView) emptyView2.findViewById(com.android.settings.R.id.message);
                textView.setText(com.android.settings.R.string.print_service_disabled);
                contentRoot.addView(emptyView2);
                listView.setEmptyView(emptyView2);
                return;
            }
            return;
        }
        if (this.mPrintersAdapter.getUnfilteredCount() <= 0) {
            if (emptyView != null && emptyView.getId() != com.android.settings.R.id.empty_printers_list_service_enabled) {
                contentRoot.removeView(emptyView);
                emptyView = null;
            }
            if (emptyView == null) {
                View emptyView3 = getActivity().getLayoutInflater().inflate(com.android.settings.R.layout.empty_printers_list_service_enabled, contentRoot, false);
                contentRoot.addView(emptyView3);
                listView.setEmptyView(emptyView3);
                return;
            }
            return;
        }
        if (this.mPrintersAdapter.getCount() <= 0) {
            if (emptyView != null && emptyView.getId() != com.android.settings.R.id.empty_print_state) {
                contentRoot.removeView(emptyView);
                emptyView = null;
            }
            if (emptyView == null) {
                View emptyView4 = getActivity().getLayoutInflater().inflate(com.android.settings.R.layout.empty_print_state, contentRoot, false);
                ImageView iconView2 = (ImageView) emptyView4.findViewById(com.android.settings.R.id.icon);
                iconView2.setContentDescription(getString(com.android.settings.R.string.print_no_printers_found));
                TextView textView2 = (TextView) emptyView4.findViewById(com.android.settings.R.id.message);
                textView2.setText(com.android.settings.R.string.print_no_printers_found);
                contentRoot.addView(emptyView4);
                listView.setEmptyView(emptyView4);
            }
        }
    }

    private void updateUiForServiceState() {
        List<ComponentName> services = PrintSettingsUtils.readEnabledPrintServices(getActivity());
        this.mServiceEnabled = services.contains(this.mComponentName);
        if (this.mServiceEnabled) {
            this.mSwitchBar.setCheckedInternal(true);
            this.mPrintersAdapter.enable();
        } else {
            this.mSwitchBar.setCheckedInternal(false);
            this.mPrintersAdapter.disable();
        }
        getActivity().invalidateOptionsMenu();
    }

    private void initComponents() {
        this.mPrintersAdapter = new PrintersAdapter();
        this.mPrintersAdapter.registerDataSetObserver(this.mDataObserver);
        SettingsActivity activity = (SettingsActivity) getActivity();
        this.mSwitchBar = activity.getSwitchBar();
        this.mSwitchBar.addOnSwitchChangeListener(this);
        this.mSwitchBar.show();
        this.mToggleSwitch = this.mSwitchBar.getSwitch();
        this.mToggleSwitch.setOnBeforeCheckedChangeListener(new ToggleSwitch.OnBeforeCheckedChangeListener() {
            @Override
            public boolean onBeforeCheckedChanged(ToggleSwitch toggleSwitch, boolean checked) {
                if (checked) {
                    if (!TextUtils.isEmpty(PrintServiceSettingsFragment.this.mEnableWarningMessage)) {
                        PrintServiceSettingsFragment.this.mSwitchBar.setCheckedInternal(false);
                        PrintServiceSettingsFragment.this.getArguments().putBoolean("EXTRA_CHECKED", false);
                        PrintServiceSettingsFragment.this.showDialog(1);
                        return true;
                    }
                    PrintServiceSettingsFragment.this.onPreferenceToggled(PrintServiceSettingsFragment.this.mPreferenceKey, true);
                } else {
                    PrintServiceSettingsFragment.this.onPreferenceToggled(PrintServiceSettingsFragment.this.mPreferenceKey, false);
                }
                return false;
            }
        });
        getListView().setSelector(new ColorDrawable(0));
        getListView().setAdapter((ListAdapter) this.mPrintersAdapter);
    }

    @Override
    public void onSwitchChanged(Switch switchView, boolean isChecked) {
        updateEmptyView();
    }

    private void updateUiForArguments() {
        Bundle arguments = getArguments();
        this.mPreferenceKey = arguments.getString("EXTRA_PREFERENCE_KEY");
        boolean enabled = arguments.getBoolean("EXTRA_CHECKED");
        this.mSwitchBar.setCheckedInternal(enabled);
        String settingsTitle = arguments.getString("EXTRA_SETTINGS_TITLE");
        String settingsComponentName = arguments.getString("EXTRA_SETTINGS_COMPONENT_NAME");
        if (!TextUtils.isEmpty(settingsTitle) && !TextUtils.isEmpty(settingsComponentName)) {
            Intent settingsIntent = new Intent("android.intent.action.MAIN").setComponent(ComponentName.unflattenFromString(settingsComponentName.toString()));
            List<ResolveInfo> resolvedActivities = getPackageManager().queryIntentActivities(settingsIntent, 0);
            if (!resolvedActivities.isEmpty() && resolvedActivities.get(0).activityInfo.exported) {
                this.mSettingsTitle = settingsTitle;
                this.mSettingsIntent = settingsIntent;
            }
        }
        String addPrintersTitle = arguments.getString("EXTRA_ADD_PRINTERS_TITLE");
        String addPrintersComponentName = arguments.getString("EXTRA_ADD_PRINTERS_COMPONENT_NAME");
        if (!TextUtils.isEmpty(addPrintersTitle) && !TextUtils.isEmpty(addPrintersComponentName)) {
            Intent addPritnersIntent = new Intent("android.intent.action.MAIN").setComponent(ComponentName.unflattenFromString(addPrintersComponentName.toString()));
            List<ResolveInfo> resolvedActivities2 = getPackageManager().queryIntentActivities(addPritnersIntent, 0);
            if (!resolvedActivities2.isEmpty() && resolvedActivities2.get(0).activityInfo.exported) {
                this.mAddPrintersTitle = addPrintersTitle;
                this.mAddPrintersIntent = addPritnersIntent;
            }
        }
        this.mEnableWarningTitle = arguments.getCharSequence("EXTRA_ENABLE_WARNING_TITLE");
        this.mEnableWarningMessage = arguments.getCharSequence("EXTRA_ENABLE_WARNING_MESSAGE");
        this.mComponentName = ComponentName.unflattenFromString(arguments.getString("EXTRA_SERVICE_COMPONENT_NAME"));
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(com.android.settings.R.menu.print_service_settings, menu);
        MenuItem addPrinters = menu.findItem(com.android.settings.R.id.print_menu_item_add_printer);
        if (this.mServiceEnabled && !TextUtils.isEmpty(this.mAddPrintersTitle) && this.mAddPrintersIntent != null) {
            addPrinters.setIntent(this.mAddPrintersIntent);
        } else {
            menu.removeItem(com.android.settings.R.id.print_menu_item_add_printer);
        }
        MenuItem settings = menu.findItem(com.android.settings.R.id.print_menu_item_settings);
        if (this.mServiceEnabled && !TextUtils.isEmpty(this.mSettingsTitle) && this.mSettingsIntent != null) {
            settings.setIntent(this.mSettingsIntent);
        } else {
            menu.removeItem(com.android.settings.R.id.print_menu_item_settings);
        }
        MenuItem searchItem = menu.findItem(com.android.settings.R.id.print_menu_item_search);
        if (this.mServiceEnabled && this.mPrintersAdapter.getUnfilteredCount() > 0) {
            this.mSearchView = (SearchView) searchItem.getActionView();
            this.mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    return true;
                }

                @Override
                public boolean onQueryTextChange(String searchString) {
                    ((Filterable) PrintServiceSettingsFragment.this.getListView().getAdapter()).getFilter().filter(searchString);
                    return true;
                }
            });
            this.mSearchView.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
                @Override
                public void onViewAttachedToWindow(View view) {
                    if (AccessibilityManager.getInstance(PrintServiceSettingsFragment.this.getActivity()).isEnabled()) {
                        view.announceForAccessibility(PrintServiceSettingsFragment.this.getString(com.android.settings.R.string.print_search_box_shown_utterance));
                    }
                }

                @Override
                public void onViewDetachedFromWindow(View view) {
                    Activity activity = PrintServiceSettingsFragment.this.getActivity();
                    if (activity != null && !activity.isFinishing() && AccessibilityManager.getInstance(activity).isEnabled()) {
                        view.announceForAccessibility(PrintServiceSettingsFragment.this.getString(com.android.settings.R.string.print_search_box_hidden_utterance));
                    }
                }
            });
            return;
        }
        menu.removeItem(com.android.settings.R.id.print_menu_item_search);
    }

    private static abstract class SettingsContentObserver extends ContentObserver {
        public SettingsContentObserver(Handler handler) {
            super(handler);
        }

        public void register(ContentResolver contentResolver) {
            contentResolver.registerContentObserver(Settings.Secure.getUriFor("enabled_print_services"), false, this);
        }

        public void unregister(ContentResolver contentResolver) {
            contentResolver.unregisterContentObserver(this);
        }
    }

    private final class PrintersAdapter extends BaseAdapter implements LoaderManager.LoaderCallbacks<List<PrinterInfo>>, Filterable {
        private final List<PrinterInfo> mFilteredPrinters;
        private CharSequence mLastSearchString;
        private final Object mLock;
        private final List<PrinterInfo> mPrinters;

        private PrintersAdapter() {
            this.mLock = new Object();
            this.mPrinters = new ArrayList();
            this.mFilteredPrinters = new ArrayList();
        }

        public void enable() {
            PrintServiceSettingsFragment.this.getLoaderManager().initLoader(1, null, this);
        }

        public void disable() {
            PrintServiceSettingsFragment.this.getLoaderManager().destroyLoader(1);
            this.mPrinters.clear();
        }

        public int getUnfilteredCount() {
            return this.mPrinters.size();
        }

        @Override
        public Filter getFilter() {
            return new Filter() {
                @Override
                protected Filter.FilterResults performFiltering(CharSequence constraint) {
                    Filter.FilterResults results;
                    synchronized (PrintersAdapter.this.mLock) {
                        if (TextUtils.isEmpty(constraint)) {
                            results = null;
                        } else {
                            results = new Filter.FilterResults();
                            List<PrinterInfo> filteredPrinters = new ArrayList<>();
                            String constraintLowerCase = constraint.toString().toLowerCase();
                            int printerCount = PrintersAdapter.this.mPrinters.size();
                            for (int i = 0; i < printerCount; i++) {
                                PrinterInfo printer = (PrinterInfo) PrintersAdapter.this.mPrinters.get(i);
                                if (printer.getName().toLowerCase().contains(constraintLowerCase)) {
                                    filteredPrinters.add(printer);
                                }
                            }
                            results.values = filteredPrinters;
                            results.count = filteredPrinters.size();
                        }
                    }
                    return results;
                }

                @Override
                protected void publishResults(CharSequence constraint, Filter.FilterResults results) {
                    synchronized (PrintersAdapter.this.mLock) {
                        PrintersAdapter.this.mLastSearchString = constraint;
                        PrintersAdapter.this.mFilteredPrinters.clear();
                        if (results == null) {
                            PrintersAdapter.this.mFilteredPrinters.addAll(PrintersAdapter.this.mPrinters);
                        } else {
                            List<PrinterInfo> printers = (List) results.values;
                            PrintersAdapter.this.mFilteredPrinters.addAll(printers);
                        }
                    }
                    PrintersAdapter.this.notifyDataSetChanged();
                }
            };
        }

        @Override
        public int getCount() {
            int size;
            synchronized (this.mLock) {
                size = this.mFilteredPrinters.size();
            }
            return size;
        }

        @Override
        public Object getItem(int position) {
            PrinterInfo printerInfo;
            synchronized (this.mLock) {
                printerInfo = this.mFilteredPrinters.get(position);
            }
            return printerInfo;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = PrintServiceSettingsFragment.this.getActivity().getLayoutInflater().inflate(com.android.settings.R.layout.printer_dropdown_item, parent, false);
            }
            PrinterInfo printer = (PrinterInfo) getItem(position);
            CharSequence title = printer.getName();
            CharSequence subtitle = null;
            Drawable icon = null;
            try {
                PackageInfo packageInfo = PrintServiceSettingsFragment.this.getPackageManager().getPackageInfo(printer.getId().getServiceName().getPackageName(), 0);
                subtitle = packageInfo.applicationInfo.loadLabel(PrintServiceSettingsFragment.this.getPackageManager());
                icon = packageInfo.applicationInfo.loadIcon(PrintServiceSettingsFragment.this.getPackageManager());
            } catch (PackageManager.NameNotFoundException e) {
            }
            TextView titleView = (TextView) convertView.findViewById(com.android.settings.R.id.title);
            titleView.setText(title);
            TextView subtitleView = (TextView) convertView.findViewById(com.android.settings.R.id.subtitle);
            if (!TextUtils.isEmpty(subtitle)) {
                subtitleView.setText(subtitle);
                subtitleView.setVisibility(0);
            } else {
                subtitleView.setText((CharSequence) null);
                subtitleView.setVisibility(8);
            }
            ImageView iconView = (ImageView) convertView.findViewById(com.android.settings.R.id.icon);
            if (icon != null) {
                iconView.setImageDrawable(icon);
                iconView.setVisibility(0);
            } else {
                iconView.setVisibility(8);
            }
            return convertView;
        }

        @Override
        public boolean isEnabled(int position) {
            return false;
        }

        @Override
        public Loader<List<PrinterInfo>> onCreateLoader(int id, Bundle args) {
            if (id == 1) {
                return new PrintersLoader(PrintServiceSettingsFragment.this.getActivity());
            }
            return null;
        }

        @Override
        public void onLoadFinished(Loader<List<PrinterInfo>> loader, List<PrinterInfo> printers) {
            synchronized (this.mLock) {
                this.mPrinters.clear();
                int printerCount = printers.size();
                for (int i = 0; i < printerCount; i++) {
                    PrinterInfo printer = printers.get(i);
                    if (printer.getId().getServiceName().equals(PrintServiceSettingsFragment.this.mComponentName)) {
                        this.mPrinters.add(printer);
                    }
                }
                this.mFilteredPrinters.clear();
                this.mFilteredPrinters.addAll(this.mPrinters);
                if (!TextUtils.isEmpty(this.mLastSearchString)) {
                    getFilter().filter(this.mLastSearchString);
                }
            }
            notifyDataSetChanged();
        }

        @Override
        public void onLoaderReset(Loader<List<PrinterInfo>> loader) {
            synchronized (this.mLock) {
                this.mPrinters.clear();
                this.mFilteredPrinters.clear();
                this.mLastSearchString = null;
            }
            notifyDataSetInvalidated();
        }
    }

    private static class PrintersLoader extends Loader<List<PrinterInfo>> {
        private PrinterDiscoverySession mDiscoverySession;
        private final Map<PrinterId, PrinterInfo> mPrinters;

        public PrintersLoader(Context context) {
            super(context);
            this.mPrinters = new LinkedHashMap();
        }

        @Override
        public void deliverResult(List<PrinterInfo> printers) {
            if (isStarted()) {
                super.deliverResult(printers);
            }
        }

        @Override
        protected void onStartLoading() {
            if (!this.mPrinters.isEmpty()) {
                deliverResult((List<PrinterInfo>) new ArrayList(this.mPrinters.values()));
            }
            onForceLoad();
        }

        @Override
        protected void onStopLoading() {
            onCancelLoad();
        }

        @Override
        protected void onForceLoad() {
            loadInternal();
        }

        @Override
        protected boolean onCancelLoad() {
            return cancelInternal();
        }

        @Override
        protected void onReset() {
            onStopLoading();
            this.mPrinters.clear();
            if (this.mDiscoverySession != null) {
                this.mDiscoverySession.destroy();
                this.mDiscoverySession = null;
            }
        }

        @Override
        protected void onAbandon() {
            onStopLoading();
        }

        private boolean cancelInternal() {
            if (this.mDiscoverySession == null || !this.mDiscoverySession.isPrinterDiscoveryStarted()) {
                return false;
            }
            this.mDiscoverySession.stopPrinterDiscovery();
            return true;
        }

        private void loadInternal() {
            if (this.mDiscoverySession == null) {
                PrintManager printManager = (PrintManager) getContext().getSystemService("print");
                this.mDiscoverySession = printManager.createPrinterDiscoverySession();
                this.mDiscoverySession.setOnPrintersChangeListener(new PrinterDiscoverySession.OnPrintersChangeListener() {
                    public void onPrintersChanged() {
                        PrintersLoader.this.deliverResult((List<PrinterInfo>) new ArrayList(PrintersLoader.this.mDiscoverySession.getPrinters()));
                    }
                });
            }
            this.mDiscoverySession.startPrinterDiscovery((List) null);
        }
    }
}
