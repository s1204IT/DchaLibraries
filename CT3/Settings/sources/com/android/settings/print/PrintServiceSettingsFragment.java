package com.android.settings.print;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.Loader;
import android.content.pm.ResolveInfo;
import android.database.DataSetObserver;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.print.PrintManager;
import android.print.PrintServicesLoader;
import android.print.PrinterDiscoverySession;
import android.print.PrinterId;
import android.print.PrinterInfo;
import android.printservice.PrintServiceInfo;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.Switch;
import android.widget.TextView;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.widget.SwitchBar;
import com.android.settings.widget.ToggleSwitch;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PrintServiceSettingsFragment extends SettingsPreferenceFragment implements SwitchBar.OnSwitchChangeListener, LoaderManager.LoaderCallbacks<List<PrintServiceInfo>> {
    private Intent mAddPrintersIntent;
    private ComponentName mComponentName;
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
    private int mLastUnfilteredItemCount;
    private CharSequence mOldActivityTitle;
    private String mPreferenceKey;
    private PrintersAdapter mPrintersAdapter;
    private SearchView mSearchView;
    private boolean mServiceEnabled;
    private Intent mSettingsIntent;
    private SwitchBar mSwitchBar;
    private ToggleSwitch mToggleSwitch;

    @Override
    protected int getMetricsCategory() {
        return 79;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        this.mServiceEnabled = getArguments().getBoolean("EXTRA_CHECKED");
        String title = getArguments().getString("EXTRA_TITLE");
        if (TextUtils.isEmpty(title)) {
            return;
        }
        getActivity().setTitle(title);
    }

    @Override
    public void onStart() {
        super.onStart();
        updateEmptyView();
        updateUiForServiceState();
    }

    @Override
    public void onPause() {
        if (this.mSearchView != null) {
            this.mSearchView.setOnQueryTextListener(null);
        }
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initComponents();
        updateUiForArguments();
        getBackupListView().setVisibility(0);
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

    public void onPreferenceToggled(String preferenceKey, boolean enabled) {
        ((PrintManager) getContext().getSystemService("print")).setPrintServiceEnabled(this.mComponentName, enabled);
    }

    private ListView getBackupListView() {
        return (ListView) getView().findViewById(R.id.backup_list);
    }

    public void updateEmptyView() {
        ViewGroup contentRoot = (ViewGroup) getListView().getParent();
        View emptyView = getBackupListView().getEmptyView();
        if (!this.mToggleSwitch.isChecked()) {
            if (emptyView != null && emptyView.getId() != R.id.empty_print_state) {
                contentRoot.removeView(emptyView);
                emptyView = null;
            }
            if (emptyView != null) {
                return;
            }
            View emptyView2 = getActivity().getLayoutInflater().inflate(R.layout.empty_print_state, contentRoot, false);
            ImageView iconView = (ImageView) emptyView2.findViewById(R.id.icon);
            iconView.setContentDescription(getString(R.string.print_service_disabled));
            TextView textView = (TextView) emptyView2.findViewById(R.id.message);
            textView.setText(R.string.print_service_disabled);
            contentRoot.addView(emptyView2);
            getBackupListView().setEmptyView(emptyView2);
            return;
        }
        if (this.mPrintersAdapter.getUnfilteredCount() <= 0) {
            if (emptyView != null && emptyView.getId() != R.id.empty_printers_list_service_enabled) {
                contentRoot.removeView(emptyView);
                emptyView = null;
            }
            if (emptyView != null) {
                return;
            }
            View emptyView3 = getActivity().getLayoutInflater().inflate(R.layout.empty_printers_list_service_enabled, contentRoot, false);
            contentRoot.addView(emptyView3);
            getBackupListView().setEmptyView(emptyView3);
            return;
        }
        if (this.mPrintersAdapter.getCount() > 0) {
            return;
        }
        if (emptyView != null && emptyView.getId() != R.id.empty_print_state) {
            contentRoot.removeView(emptyView);
            emptyView = null;
        }
        if (emptyView != null) {
            return;
        }
        View emptyView4 = getActivity().getLayoutInflater().inflate(R.layout.empty_print_state, contentRoot, false);
        ImageView iconView2 = (ImageView) emptyView4.findViewById(R.id.icon);
        iconView2.setContentDescription(getString(R.string.print_no_printers_found));
        TextView textView2 = (TextView) emptyView4.findViewById(R.id.message);
        textView2.setText(R.string.print_no_printers_found);
        contentRoot.addView(emptyView4);
        getBackupListView().setEmptyView(emptyView4);
    }

    private void updateUiForServiceState() {
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
        this.mPrintersAdapter = new PrintersAdapter(this, null);
        this.mPrintersAdapter.registerDataSetObserver(this.mDataObserver);
        SettingsActivity activity = (SettingsActivity) getActivity();
        this.mSwitchBar = activity.getSwitchBar();
        this.mSwitchBar.addOnSwitchChangeListener(this);
        this.mSwitchBar.show();
        this.mToggleSwitch = this.mSwitchBar.getSwitch();
        this.mToggleSwitch.setOnBeforeCheckedChangeListener(new ToggleSwitch.OnBeforeCheckedChangeListener() {
            @Override
            public boolean onBeforeCheckedChanged(ToggleSwitch toggleSwitch, boolean checked) {
                PrintServiceSettingsFragment.this.onPreferenceToggled(PrintServiceSettingsFragment.this.mPreferenceKey, checked);
                return false;
            }
        });
        getBackupListView().setSelector(new ColorDrawable(0));
        getBackupListView().setAdapter((ListAdapter) this.mPrintersAdapter);
        getBackupListView().setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                PrinterInfo printer = (PrinterInfo) PrintServiceSettingsFragment.this.mPrintersAdapter.getItem(position);
                if (printer.getInfoIntent() == null) {
                    return;
                }
                try {
                    PrintServiceSettingsFragment.this.getActivity().startIntentSender(printer.getInfoIntent().getIntentSender(), null, 0, 0, 0);
                } catch (IntentSender.SendIntentException e) {
                    Log.e("PrintServiceSettingsFragment", "Could not execute info intent: %s", e);
                }
            }
        });
    }

    @Override
    public void onSwitchChanged(Switch switchView, boolean isChecked) {
        updateEmptyView();
    }

    private void updateUiForArguments() {
        Bundle arguments = getArguments();
        this.mComponentName = ComponentName.unflattenFromString(arguments.getString("EXTRA_SERVICE_COMPONENT_NAME"));
        this.mPreferenceKey = this.mComponentName.flattenToString();
        boolean enabled = arguments.getBoolean("EXTRA_CHECKED");
        this.mSwitchBar.setCheckedInternal(enabled);
        getLoaderManager().initLoader(2, null, this);
        setHasOptionsMenu(true);
    }

    @Override
    public Loader<List<PrintServiceInfo>> onCreateLoader(int id, Bundle args) {
        return new PrintServicesLoader((PrintManager) getContext().getSystemService("print"), getContext(), 3);
    }

    @Override
    public void onLoadFinished(Loader<List<PrintServiceInfo>> loader, List<PrintServiceInfo> services) {
        PrintServiceInfo service = null;
        if (services != null) {
            int numServices = services.size();
            int i = 0;
            while (true) {
                if (i >= numServices) {
                    break;
                }
                if (!services.get(i).getComponentName().equals(this.mComponentName)) {
                    i++;
                } else {
                    service = services.get(i);
                    break;
                }
            }
        }
        if (service == null) {
            finishFragment();
        }
        this.mServiceEnabled = service.isEnabled();
        if (service.getSettingsActivityName() != null) {
            Intent settingsIntent = new Intent("android.intent.action.MAIN");
            settingsIntent.setComponent(new ComponentName(service.getComponentName().getPackageName(), service.getSettingsActivityName()));
            List<ResolveInfo> resolvedActivities = getPackageManager().queryIntentActivities(settingsIntent, 0);
            if (!resolvedActivities.isEmpty() && resolvedActivities.get(0).activityInfo.exported) {
                this.mSettingsIntent = settingsIntent;
            }
        } else {
            this.mSettingsIntent = null;
        }
        if (service.getAddPrintersActivityName() != null) {
            Intent addPrintersIntent = new Intent("android.intent.action.MAIN");
            addPrintersIntent.setComponent(new ComponentName(service.getComponentName().getPackageName(), service.getAddPrintersActivityName()));
            List<ResolveInfo> resolvedActivities2 = getPackageManager().queryIntentActivities(addPrintersIntent, 0);
            if (!resolvedActivities2.isEmpty() && resolvedActivities2.get(0).activityInfo.exported) {
                this.mAddPrintersIntent = addPrintersIntent;
            }
        } else {
            this.mAddPrintersIntent = null;
        }
        updateUiForServiceState();
    }

    @Override
    public void onLoaderReset(Loader<List<PrintServiceInfo>> loader) {
        updateUiForServiceState();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.print_service_settings, menu);
        MenuItem addPrinters = menu.findItem(R.id.print_menu_item_add_printer);
        if (this.mServiceEnabled && this.mAddPrintersIntent != null) {
            addPrinters.setIntent(this.mAddPrintersIntent);
        } else {
            menu.removeItem(R.id.print_menu_item_add_printer);
        }
        MenuItem settings = menu.findItem(R.id.print_menu_item_settings);
        if (this.mServiceEnabled && this.mSettingsIntent != null) {
            settings.setIntent(this.mSettingsIntent);
        } else {
            menu.removeItem(R.id.print_menu_item_settings);
        }
        MenuItem searchItem = menu.findItem(R.id.print_menu_item_search);
        if (this.mServiceEnabled && this.mPrintersAdapter.getUnfilteredCount() > 0) {
            this.mSearchView = (SearchView) searchItem.getActionView();
            this.mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    return true;
                }

                @Override
                public boolean onQueryTextChange(String searchString) {
                    Activity activity = PrintServiceSettingsFragment.this.getActivity();
                    if (activity != null && !activity.isFinishing()) {
                        PrintServiceSettingsFragment.this.mPrintersAdapter.getFilter().filter(searchString);
                        return true;
                    }
                    return true;
                }
            });
            this.mSearchView.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
                @Override
                public void onViewAttachedToWindow(View view) {
                    if (!AccessibilityManager.getInstance(PrintServiceSettingsFragment.this.getActivity()).isEnabled()) {
                        return;
                    }
                    view.announceForAccessibility(PrintServiceSettingsFragment.this.getString(R.string.print_search_box_shown_utterance));
                }

                @Override
                public void onViewDetachedFromWindow(View view) {
                    Activity activity = PrintServiceSettingsFragment.this.getActivity();
                    if (activity == null || activity.isFinishing() || !AccessibilityManager.getInstance(activity).isEnabled()) {
                        return;
                    }
                    view.announceForAccessibility(PrintServiceSettingsFragment.this.getString(R.string.print_search_box_hidden_utterance));
                }
            });
            return;
        }
        menu.removeItem(R.id.print_menu_item_search);
    }

    private final class PrintersAdapter extends BaseAdapter implements LoaderManager.LoaderCallbacks<List<PrinterInfo>>, Filterable {
        private final List<PrinterInfo> mFilteredPrinters;
        private CharSequence mLastSearchString;
        private final Object mLock;
        private final List<PrinterInfo> mPrinters;

        PrintersAdapter(PrintServiceSettingsFragment this$0, PrintersAdapter printersAdapter) {
            this();
        }

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
                    synchronized (PrintersAdapter.this.mLock) {
                        if (TextUtils.isEmpty(constraint)) {
                            return null;
                        }
                        Filter.FilterResults results = new Filter.FilterResults();
                        List<PrinterInfo> filteredPrinters = new ArrayList<>();
                        String constraintLowerCase = constraint.toString().toLowerCase();
                        int printerCount = PrintersAdapter.this.mPrinters.size();
                        for (int i = 0; i < printerCount; i++) {
                            PrinterInfo printer = (PrinterInfo) PrintersAdapter.this.mPrinters.get(i);
                            String name = printer.getName();
                            if (name != null && name.toLowerCase().contains(constraintLowerCase)) {
                                filteredPrinters.add(printer);
                            }
                        }
                        results.values = filteredPrinters;
                        results.count = filteredPrinters.size();
                        return results;
                    }
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

        public boolean isActionable(int position) {
            PrinterInfo printer = (PrinterInfo) getItem(position);
            return printer.getStatus() != 3;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = PrintServiceSettingsFragment.this.getActivity().getLayoutInflater().inflate(R.layout.printer_dropdown_item, parent, false);
            }
            convertView.setEnabled(isActionable(position));
            final PrinterInfo printer = (PrinterInfo) getItem(position);
            CharSequence title = printer.getName();
            CharSequence subtitle = printer.getDescription();
            Drawable icon = printer.loadIcon(PrintServiceSettingsFragment.this.getActivity());
            TextView titleView = (TextView) convertView.findViewById(R.id.title);
            titleView.setText(title);
            TextView subtitleView = (TextView) convertView.findViewById(R.id.subtitle);
            if (!TextUtils.isEmpty(subtitle)) {
                subtitleView.setText(subtitle);
                subtitleView.setVisibility(0);
            } else {
                subtitleView.setText((CharSequence) null);
                subtitleView.setVisibility(8);
            }
            LinearLayout moreInfoView = (LinearLayout) convertView.findViewById(R.id.more_info);
            if (printer.getInfoIntent() != null) {
                moreInfoView.setVisibility(0);
                moreInfoView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        try {
                            PrintServiceSettingsFragment.this.getActivity().startIntentSender(printer.getInfoIntent().getIntentSender(), null, 0, 0, 0);
                        } catch (IntentSender.SendIntentException e) {
                            Log.e("PrintServiceSettingsFragment", "Could not execute pending info intent: %s", e);
                        }
                    }
                });
            } else {
                moreInfoView.setVisibility(8);
            }
            ImageView iconView = (ImageView) convertView.findViewById(R.id.icon);
            if (icon != null) {
                iconView.setVisibility(0);
                if (!isActionable(position)) {
                    icon.mutate();
                    TypedValue value = new TypedValue();
                    PrintServiceSettingsFragment.this.getActivity().getTheme().resolveAttribute(android.R.attr.disabledAlpha, value, true);
                    icon.setAlpha((int) (value.getFloat() * 255.0f));
                }
                iconView.setImageDrawable(icon);
            } else {
                iconView.setVisibility(8);
            }
            return convertView;
        }

        @Override
        public Loader<List<PrinterInfo>> onCreateLoader(int id, Bundle args) {
            if (id == 1) {
                return new PrintersLoader(PrintServiceSettingsFragment.this.getContext());
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
            if (!isStarted()) {
                return;
            }
            super.deliverResult(printers);
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
            if (this.mDiscoverySession == null) {
                return;
            }
            this.mDiscoverySession.destroy();
            this.mDiscoverySession = null;
        }

        @Override
        protected void onAbandon() {
            onStopLoading();
        }

        private boolean cancelInternal() {
            if (this.mDiscoverySession != null && this.mDiscoverySession.isPrinterDiscoveryStarted()) {
                this.mDiscoverySession.stopPrinterDiscovery();
                return true;
            }
            return false;
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
