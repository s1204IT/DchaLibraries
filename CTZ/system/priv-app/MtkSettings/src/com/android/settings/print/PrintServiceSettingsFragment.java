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
import android.view.LayoutInflater;
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

/* loaded from: classes.dex */
public class PrintServiceSettingsFragment extends SettingsPreferenceFragment implements LoaderManager.LoaderCallbacks<List<PrintServiceInfo>>, SwitchBar.OnSwitchChangeListener {
    private Intent mAddPrintersIntent;
    private ComponentName mComponentName;
    private final DataSetObserver mDataObserver = new DataSetObserver() { // from class: com.android.settings.print.PrintServiceSettingsFragment.1
        @Override // android.database.DataSetObserver
        public void onChanged() {
            invalidateOptionsMenuIfNeeded();
            PrintServiceSettingsFragment.this.updateEmptyView();
        }

        @Override // android.database.DataSetObserver
        public void onInvalidated() {
            invalidateOptionsMenuIfNeeded();
        }

        private void invalidateOptionsMenuIfNeeded() {
            int unfilteredCount = PrintServiceSettingsFragment.this.mPrintersAdapter.getUnfilteredCount();
            if ((PrintServiceSettingsFragment.this.mLastUnfilteredItemCount <= 0 && unfilteredCount > 0) || (PrintServiceSettingsFragment.this.mLastUnfilteredItemCount > 0 && unfilteredCount <= 0)) {
                PrintServiceSettingsFragment.this.getActivity().invalidateOptionsMenu();
            }
            PrintServiceSettingsFragment.this.mLastUnfilteredItemCount = unfilteredCount;
        }
    };
    private int mLastUnfilteredItemCount;
    private String mPreferenceKey;
    private PrintersAdapter mPrintersAdapter;
    private SearchView mSearchView;
    private boolean mServiceEnabled;
    private Intent mSettingsIntent;
    private SwitchBar mSwitchBar;
    private ToggleSwitch mToggleSwitch;

    @Override // com.android.settingslib.core.instrumentation.Instrumentable
    public int getMetricsCategory() {
        return 79;
    }

    @Override // com.android.settings.SettingsPreferenceFragment, com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.support.v14.preference.PreferenceFragment, android.app.Fragment
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        String string = getArguments().getString("EXTRA_TITLE");
        if (!TextUtils.isEmpty(string)) {
            getActivity().setTitle(string);
        }
    }

    @Override // com.android.settings.SettingsPreferenceFragment, android.support.v14.preference.PreferenceFragment, android.app.Fragment
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        View viewOnCreateView = super.onCreateView(layoutInflater, viewGroup, bundle);
        this.mServiceEnabled = getArguments().getBoolean("EXTRA_CHECKED");
        return viewOnCreateView;
    }

    @Override // com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.support.v14.preference.PreferenceFragment, android.app.Fragment
    public void onStart() {
        super.onStart();
        updateEmptyView();
        updateUiForServiceState();
    }

    @Override // com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.app.Fragment
    public void onPause() {
        if (this.mSearchView != null) {
            this.mSearchView.setOnQueryTextListener(null);
        }
        super.onPause();
    }

    @Override // com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.support.v14.preference.PreferenceFragment, android.app.Fragment
    public void onStop() {
        super.onStop();
    }

    @Override // android.support.v14.preference.PreferenceFragment, android.app.Fragment
    public void onViewCreated(View view, Bundle bundle) {
        super.onViewCreated(view, bundle);
        initComponents();
        updateUiForArguments();
        getListView().setVisibility(8);
        getBackupListView().setVisibility(0);
    }

    @Override // android.support.v14.preference.PreferenceFragment, android.app.Fragment
    public void onDestroyView() {
        super.onDestroyView();
        this.mSwitchBar.removeOnSwitchChangeListener(this);
        this.mSwitchBar.hide();
    }

    private void onPreferenceToggled(String str, boolean z) {
        ((PrintManager) getContext().getSystemService("print")).setPrintServiceEnabled(this.mComponentName, z);
    }

    private ListView getBackupListView() {
        return (ListView) getView().findViewById(R.id.backup_list);
    }

    private void updateEmptyView() {
        ViewGroup viewGroup = (ViewGroup) getListView().getParent();
        View emptyView = getBackupListView().getEmptyView();
        if (!this.mToggleSwitch.isChecked()) {
            if (emptyView != null && emptyView.getId() != R.id.empty_print_state) {
                viewGroup.removeView(emptyView);
                emptyView = null;
            }
            if (emptyView == null) {
                View viewInflate = getActivity().getLayoutInflater().inflate(R.layout.empty_print_state, viewGroup, false);
                ((ImageView) viewInflate.findViewById(R.id.icon)).setContentDescription(getString(R.string.print_service_disabled));
                ((TextView) viewInflate.findViewById(R.id.message)).setText(R.string.print_service_disabled);
                viewGroup.addView(viewInflate);
                getBackupListView().setEmptyView(viewInflate);
                return;
            }
            return;
        }
        if (this.mPrintersAdapter.getUnfilteredCount() <= 0) {
            if (emptyView != null && emptyView.getId() != R.id.empty_printers_list_service_enabled) {
                viewGroup.removeView(emptyView);
                emptyView = null;
            }
            if (emptyView == null) {
                View viewInflate2 = getActivity().getLayoutInflater().inflate(R.layout.empty_printers_list_service_enabled, viewGroup, false);
                viewGroup.addView(viewInflate2);
                getBackupListView().setEmptyView(viewInflate2);
                return;
            }
            return;
        }
        if (this.mPrintersAdapter.getCount() <= 0) {
            if (emptyView != null && emptyView.getId() != R.id.empty_print_state) {
                viewGroup.removeView(emptyView);
                emptyView = null;
            }
            if (emptyView == null) {
                View viewInflate3 = getActivity().getLayoutInflater().inflate(R.layout.empty_print_state, viewGroup, false);
                ((ImageView) viewInflate3.findViewById(R.id.icon)).setContentDescription(getString(R.string.print_no_printers_found));
                ((TextView) viewInflate3.findViewById(R.id.message)).setText(R.string.print_no_printers_found);
                viewGroup.addView(viewInflate3);
                getBackupListView().setEmptyView(viewInflate3);
            }
        }
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
        this.mPrintersAdapter = new PrintersAdapter();
        this.mPrintersAdapter.registerDataSetObserver(this.mDataObserver);
        this.mSwitchBar = ((SettingsActivity) getActivity()).getSwitchBar();
        this.mSwitchBar.addOnSwitchChangeListener(this);
        this.mSwitchBar.show();
        this.mToggleSwitch = this.mSwitchBar.getSwitch();
        this.mToggleSwitch.setOnBeforeCheckedChangeListener(new ToggleSwitch.OnBeforeCheckedChangeListener() { // from class: com.android.settings.print.PrintServiceSettingsFragment.2
            @Override // com.android.settings.widget.ToggleSwitch.OnBeforeCheckedChangeListener
            public boolean onBeforeCheckedChanged(ToggleSwitch toggleSwitch, boolean z) {
                PrintServiceSettingsFragment.this.onPreferenceToggled(PrintServiceSettingsFragment.this.mPreferenceKey, z);
                return false;
            }
        });
        getBackupListView().setSelector(new ColorDrawable(0));
        getBackupListView().setAdapter((ListAdapter) this.mPrintersAdapter);
        getBackupListView().setOnItemClickListener(new AdapterView.OnItemClickListener() { // from class: com.android.settings.print.PrintServiceSettingsFragment.3
            @Override // android.widget.AdapterView.OnItemClickListener
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long j) throws IntentSender.SendIntentException {
                PrinterInfo printerInfo = (PrinterInfo) PrintServiceSettingsFragment.this.mPrintersAdapter.getItem(i);
                if (printerInfo.getInfoIntent() != null) {
                    try {
                        PrintServiceSettingsFragment.this.getActivity().startIntentSender(printerInfo.getInfoIntent().getIntentSender(), null, 0, 0, 0);
                    } catch (IntentSender.SendIntentException e) {
                        Log.e("PrintServiceSettingsFragment", "Could not execute info intent: %s", e);
                    }
                }
            }
        });
    }

    @Override // com.android.settings.widget.SwitchBar.OnSwitchChangeListener
    public void onSwitchChanged(Switch r1, boolean z) {
        updateEmptyView();
    }

    private void updateUiForArguments() {
        Bundle arguments = getArguments();
        this.mComponentName = ComponentName.unflattenFromString(arguments.getString("EXTRA_SERVICE_COMPONENT_NAME"));
        this.mPreferenceKey = this.mComponentName.flattenToString();
        this.mSwitchBar.setCheckedInternal(arguments.getBoolean("EXTRA_CHECKED"));
        getLoaderManager().initLoader(2, null, this);
        setHasOptionsMenu(true);
    }

    @Override // android.app.LoaderManager.LoaderCallbacks
    public Loader<List<PrintServiceInfo>> onCreateLoader(int i, Bundle bundle) {
        return new PrintServicesLoader((PrintManager) getContext().getSystemService("print"), getContext(), 3);
    }

    /* JADX DEBUG: Method merged with bridge method: onLoadFinished(Landroid/content/Loader;Ljava/lang/Object;)V */
    @Override // android.app.LoaderManager.LoaderCallbacks
    public void onLoadFinished(Loader<List<PrintServiceInfo>> loader, List<PrintServiceInfo> list) {
        PrintServiceInfo printServiceInfo;
        if (list != null) {
            int size = list.size();
            for (int i = 0; i < size; i++) {
                if (list.get(i).getComponentName().equals(this.mComponentName)) {
                    printServiceInfo = list.get(i);
                    break;
                }
            }
            printServiceInfo = null;
        } else {
            printServiceInfo = null;
        }
        if (printServiceInfo == null) {
            finishFragment();
        }
        this.mServiceEnabled = printServiceInfo.isEnabled();
        if (printServiceInfo.getSettingsActivityName() != null) {
            Intent intent = new Intent("android.intent.action.MAIN");
            intent.setComponent(new ComponentName(printServiceInfo.getComponentName().getPackageName(), printServiceInfo.getSettingsActivityName()));
            List<ResolveInfo> listQueryIntentActivities = getPackageManager().queryIntentActivities(intent, 0);
            if (!listQueryIntentActivities.isEmpty() && listQueryIntentActivities.get(0).activityInfo.exported) {
                this.mSettingsIntent = intent;
            }
        } else {
            this.mSettingsIntent = null;
        }
        if (printServiceInfo.getAddPrintersActivityName() != null) {
            Intent intent2 = new Intent("android.intent.action.MAIN");
            intent2.setComponent(new ComponentName(printServiceInfo.getComponentName().getPackageName(), printServiceInfo.getAddPrintersActivityName()));
            List<ResolveInfo> listQueryIntentActivities2 = getPackageManager().queryIntentActivities(intent2, 0);
            if (!listQueryIntentActivities2.isEmpty() && listQueryIntentActivities2.get(0).activityInfo.exported) {
                this.mAddPrintersIntent = intent2;
            }
        } else {
            this.mAddPrintersIntent = null;
        }
        updateUiForServiceState();
    }

    @Override // android.app.LoaderManager.LoaderCallbacks
    public void onLoaderReset(Loader<List<PrintServiceInfo>> loader) {
        updateUiForServiceState();
    }

    @Override // com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.app.Fragment
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        super.onCreateOptionsMenu(menu, menuInflater);
        menuInflater.inflate(R.menu.print_service_settings, menu);
        MenuItem menuItemFindItem = menu.findItem(R.id.print_menu_item_add_printer);
        if (this.mServiceEnabled && this.mAddPrintersIntent != null) {
            menuItemFindItem.setIntent(this.mAddPrintersIntent);
        } else {
            menu.removeItem(R.id.print_menu_item_add_printer);
        }
        MenuItem menuItemFindItem2 = menu.findItem(R.id.print_menu_item_settings);
        if (this.mServiceEnabled && this.mSettingsIntent != null) {
            menuItemFindItem2.setIntent(this.mSettingsIntent);
        } else {
            menu.removeItem(R.id.print_menu_item_settings);
        }
        MenuItem menuItemFindItem3 = menu.findItem(R.id.print_menu_item_search);
        if (this.mServiceEnabled && this.mPrintersAdapter.getUnfilteredCount() > 0) {
            this.mSearchView = (SearchView) menuItemFindItem3.getActionView();
            this.mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() { // from class: com.android.settings.print.PrintServiceSettingsFragment.4
                @Override // android.widget.SearchView.OnQueryTextListener
                public boolean onQueryTextSubmit(String str) {
                    return true;
                }

                @Override // android.widget.SearchView.OnQueryTextListener
                public boolean onQueryTextChange(String str) {
                    Activity activity = PrintServiceSettingsFragment.this.getActivity();
                    if (activity != null && !activity.isFinishing()) {
                        PrintServiceSettingsFragment.this.mPrintersAdapter.getFilter().filter(str);
                        return true;
                    }
                    return true;
                }
            });
            this.mSearchView.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() { // from class: com.android.settings.print.PrintServiceSettingsFragment.5
                @Override // android.view.View.OnAttachStateChangeListener
                public void onViewAttachedToWindow(View view) {
                    if (AccessibilityManager.getInstance(PrintServiceSettingsFragment.this.getActivity()).isEnabled()) {
                        view.announceForAccessibility(PrintServiceSettingsFragment.this.getString(R.string.print_search_box_shown_utterance));
                    }
                }

                @Override // android.view.View.OnAttachStateChangeListener
                public void onViewDetachedFromWindow(View view) {
                    Activity activity = PrintServiceSettingsFragment.this.getActivity();
                    if (activity != null && !activity.isFinishing() && AccessibilityManager.getInstance(activity).isEnabled()) {
                        view.announceForAccessibility(PrintServiceSettingsFragment.this.getString(R.string.print_search_box_hidden_utterance));
                    }
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

        @Override // android.widget.Filterable
        public Filter getFilter() {
            return new Filter() { // from class: com.android.settings.print.PrintServiceSettingsFragment.PrintersAdapter.1
                @Override // android.widget.Filter
                protected Filter.FilterResults performFiltering(CharSequence charSequence) {
                    synchronized (PrintersAdapter.this.mLock) {
                        if (TextUtils.isEmpty(charSequence)) {
                            return null;
                        }
                        Filter.FilterResults filterResults = new Filter.FilterResults();
                        ArrayList arrayList = new ArrayList();
                        String lowerCase = charSequence.toString().toLowerCase();
                        int size = PrintersAdapter.this.mPrinters.size();
                        for (int i = 0; i < size; i++) {
                            PrinterInfo printerInfo = (PrinterInfo) PrintersAdapter.this.mPrinters.get(i);
                            String name = printerInfo.getName();
                            if (name != null && name.toLowerCase().contains(lowerCase)) {
                                arrayList.add(printerInfo);
                            }
                        }
                        filterResults.values = arrayList;
                        filterResults.count = arrayList.size();
                        return filterResults;
                    }
                }

                @Override // android.widget.Filter
                protected void publishResults(CharSequence charSequence, Filter.FilterResults filterResults) {
                    synchronized (PrintersAdapter.this.mLock) {
                        PrintersAdapter.this.mLastSearchString = charSequence;
                        PrintersAdapter.this.mFilteredPrinters.clear();
                        if (filterResults == null) {
                            PrintersAdapter.this.mFilteredPrinters.addAll(PrintersAdapter.this.mPrinters);
                        } else {
                            PrintersAdapter.this.mFilteredPrinters.addAll((List) filterResults.values);
                        }
                    }
                    PrintersAdapter.this.notifyDataSetChanged();
                }
            };
        }

        @Override // android.widget.Adapter
        public int getCount() {
            int size;
            synchronized (this.mLock) {
                size = this.mFilteredPrinters.size();
            }
            return size;
        }

        @Override // android.widget.Adapter
        public Object getItem(int i) {
            PrinterInfo printerInfo;
            synchronized (this.mLock) {
                printerInfo = this.mFilteredPrinters.get(i);
            }
            return printerInfo;
        }

        @Override // android.widget.Adapter
        public long getItemId(int i) {
            return i;
        }

        public boolean isActionable(int i) {
            return ((PrinterInfo) getItem(i)).getStatus() != 3;
        }

        @Override // android.widget.Adapter
        public View getView(int i, View view, ViewGroup viewGroup) {
            if (view == null) {
                view = PrintServiceSettingsFragment.this.getActivity().getLayoutInflater().inflate(R.layout.printer_dropdown_item, viewGroup, false);
            }
            view.setEnabled(isActionable(i));
            final PrinterInfo printerInfo = (PrinterInfo) getItem(i);
            String name = printerInfo.getName();
            String description = printerInfo.getDescription();
            Drawable drawableLoadIcon = printerInfo.loadIcon(PrintServiceSettingsFragment.this.getActivity());
            ((TextView) view.findViewById(R.id.title)).setText(name);
            TextView textView = (TextView) view.findViewById(R.id.subtitle);
            if (!TextUtils.isEmpty(description)) {
                textView.setText(description);
                textView.setVisibility(0);
            } else {
                textView.setText((CharSequence) null);
                textView.setVisibility(8);
            }
            LinearLayout linearLayout = (LinearLayout) view.findViewById(R.id.more_info);
            if (printerInfo.getInfoIntent() != null) {
                linearLayout.setVisibility(0);
                linearLayout.setOnClickListener(new View.OnClickListener() { // from class: com.android.settings.print.PrintServiceSettingsFragment.PrintersAdapter.2
                    @Override // android.view.View.OnClickListener
                    public void onClick(View view2) throws IntentSender.SendIntentException {
                        try {
                            PrintServiceSettingsFragment.this.getActivity().startIntentSender(printerInfo.getInfoIntent().getIntentSender(), null, 0, 0, 0);
                        } catch (IntentSender.SendIntentException e) {
                            Log.e("PrintServiceSettingsFragment", "Could not execute pending info intent: %s", e);
                        }
                    }
                });
            } else {
                linearLayout.setVisibility(8);
            }
            ImageView imageView = (ImageView) view.findViewById(R.id.icon);
            if (drawableLoadIcon != null) {
                imageView.setVisibility(0);
                if (!isActionable(i)) {
                    drawableLoadIcon.mutate();
                    TypedValue typedValue = new TypedValue();
                    PrintServiceSettingsFragment.this.getActivity().getTheme().resolveAttribute(android.R.attr.disabledAlpha, typedValue, true);
                    drawableLoadIcon.setAlpha((int) (typedValue.getFloat() * 255.0f));
                }
                imageView.setImageDrawable(drawableLoadIcon);
            } else {
                imageView.setVisibility(8);
            }
            return view;
        }

        @Override // android.app.LoaderManager.LoaderCallbacks
        public Loader<List<PrinterInfo>> onCreateLoader(int i, Bundle bundle) {
            if (i == 1) {
                return new PrintersLoader(PrintServiceSettingsFragment.this.getContext());
            }
            return null;
        }

        /* JADX DEBUG: Method merged with bridge method: onLoadFinished(Landroid/content/Loader;Ljava/lang/Object;)V */
        @Override // android.app.LoaderManager.LoaderCallbacks
        public void onLoadFinished(Loader<List<PrinterInfo>> loader, List<PrinterInfo> list) {
            synchronized (this.mLock) {
                this.mPrinters.clear();
                int size = list.size();
                for (int i = 0; i < size; i++) {
                    PrinterInfo printerInfo = list.get(i);
                    if (printerInfo.getId().getServiceName().equals(PrintServiceSettingsFragment.this.mComponentName)) {
                        this.mPrinters.add(printerInfo);
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

        @Override // android.app.LoaderManager.LoaderCallbacks
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

        /* JADX DEBUG: Method merged with bridge method: deliverResult(Ljava/lang/Object;)V */
        @Override // android.content.Loader
        public void deliverResult(List<PrinterInfo> list) {
            if (isStarted()) {
                super.deliverResult((PrintersLoader) list);
            }
        }

        @Override // android.content.Loader
        protected void onStartLoading() {
            if (!this.mPrinters.isEmpty()) {
                deliverResult((List<PrinterInfo>) new ArrayList(this.mPrinters.values()));
            }
            onForceLoad();
        }

        @Override // android.content.Loader
        protected void onStopLoading() {
            onCancelLoad();
        }

        @Override // android.content.Loader
        protected void onForceLoad() {
            loadInternal();
        }

        @Override // android.content.Loader
        protected boolean onCancelLoad() {
            return cancelInternal();
        }

        @Override // android.content.Loader
        protected void onReset() {
            onStopLoading();
            this.mPrinters.clear();
            if (this.mDiscoverySession != null) {
                this.mDiscoverySession.destroy();
                this.mDiscoverySession = null;
            }
        }

        @Override // android.content.Loader
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
                this.mDiscoverySession = ((PrintManager) getContext().getSystemService("print")).createPrinterDiscoverySession();
                this.mDiscoverySession.setOnPrintersChangeListener(new PrinterDiscoverySession.OnPrintersChangeListener() { // from class: com.android.settings.print.PrintServiceSettingsFragment.PrintersLoader.1
                    public void onPrintersChanged() {
                        PrintersLoader.this.deliverResult((List<PrinterInfo>) new ArrayList(PrintersLoader.this.mDiscoverySession.getPrinters()));
                    }
                });
            }
            this.mDiscoverySession.startPrinterDiscovery((List) null);
        }
    }
}
