package com.android.settings.print;

import android.app.LoaderManager;
import android.content.AsyncTaskLoader;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.UserHandle;
import android.os.UserManager;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.print.PrintJob;
import android.print.PrintJobId;
import android.print.PrintJobInfo;
import android.print.PrintManager;
import android.printservice.PrintServiceInfo;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import com.android.internal.content.PackageMonitor;
import com.android.settings.DialogCreatable;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.UserSpinnerAdapter;
import com.android.settings.Utils;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.search.SearchIndexableRaw;
import java.util.ArrayList;
import java.util.List;

public class PrintSettingsFragment extends SettingsPreferenceFragment implements AdapterView.OnItemSelectedListener, DialogCreatable, Indexable {
    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER = new BaseSearchIndexProvider() {
        @Override
        public List<SearchIndexableRaw> getRawDataToIndex(Context context, boolean enabled) {
            List<SearchIndexableRaw> indexables = new ArrayList<>();
            PackageManager packageManager = context.getPackageManager();
            PrintManager printManager = (PrintManager) context.getSystemService("print");
            String screenTitle = context.getResources().getString(R.string.print_settings);
            SearchIndexableRaw data = new SearchIndexableRaw(context);
            data.title = screenTitle;
            data.screenTitle = screenTitle;
            indexables.add(data);
            List<PrintServiceInfo> services = printManager.getInstalledPrintServices();
            int serviceCount = services.size();
            for (int i = 0; i < serviceCount; i++) {
                PrintServiceInfo service = services.get(i);
                ComponentName componentName = new ComponentName(service.getResolveInfo().serviceInfo.packageName, service.getResolveInfo().serviceInfo.name);
                SearchIndexableRaw data2 = new SearchIndexableRaw(context);
                data2.key = componentName.flattenToString();
                data2.title = service.getResolveInfo().loadLabel(packageManager).toString();
                data2.summaryOn = context.getString(R.string.print_feature_state_on);
                data2.summaryOff = context.getString(R.string.print_feature_state_off);
                data2.screenTitle = screenTitle;
                indexables.add(data2);
            }
            return indexables;
        }

        @Override
        public List<SearchIndexableResource> getXmlResourcesToIndex(Context context, boolean enabled) {
            List<SearchIndexableResource> indexables = new ArrayList<>();
            SearchIndexableResource indexable = new SearchIndexableResource(context);
            indexable.xmlResId = R.xml.print_settings;
            indexables.add(indexable);
            return indexables;
        }
    };
    private PreferenceCategory mActivePrintJobsCategory;
    private PrintJobsController mPrintJobsController;
    private PreferenceCategory mPrintServicesCategory;
    private UserSpinnerAdapter mProfileSpinnerAdapter;
    private Spinner mSpinner;
    private final PackageMonitor mSettingsPackageMonitor = new SettingsPackageMonitor();
    private final Handler mHandler = new Handler() {
        @Override
        public void dispatchMessage(Message msg) {
            PrintSettingsFragment.this.updateServicesPreferences();
        }
    };
    private final SettingsContentObserver mSettingsContentObserver = new SettingsContentObserver(this.mHandler) {
        @Override
        public void onChange(boolean selfChange, Uri uri) {
            PrintSettingsFragment.this.updateServicesPreferences();
        }
    };

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.print_settings);
        this.mActivePrintJobsCategory = (PreferenceCategory) findPreference("print_jobs_category");
        this.mPrintServicesCategory = (PreferenceCategory) findPreference("print_services_category");
        getPreferenceScreen().removePreference(this.mActivePrintJobsCategory);
        this.mPrintJobsController = new PrintJobsController();
        getActivity().getLoaderManager().initLoader(1, null, this.mPrintJobsController);
    }

    @Override
    public void onResume() {
        super.onResume();
        this.mSettingsPackageMonitor.register(getActivity(), getActivity().getMainLooper(), false);
        this.mSettingsContentObserver.register(getContentResolver());
        updateServicesPreferences();
        setHasOptionsMenu(true);
        startSubSettingsIfNeeded();
    }

    @Override
    public void onPause() {
        this.mSettingsPackageMonitor.unregister();
        this.mSettingsContentObserver.unregister(getContentResolver());
        super.onPause();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        String searchUri = Settings.Secure.getString(getContentResolver(), "print_service_search_uri");
        if (!TextUtils.isEmpty(searchUri)) {
            MenuItem menuItem = menu.add(R.string.print_menu_item_add_service);
            menuItem.setShowAsActionFlags(0);
            menuItem.setIntent(new Intent("android.intent.action.VIEW", Uri.parse(searchUri)));
        }
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ViewGroup contentRoot = (ViewGroup) getListView().getParent();
        View emptyView = getActivity().getLayoutInflater().inflate(R.layout.empty_print_state, contentRoot, false);
        TextView textView = (TextView) emptyView.findViewById(R.id.message);
        textView.setText(R.string.print_no_services_installed);
        contentRoot.addView(emptyView);
        getListView().setEmptyView(emptyView);
        UserManager um = (UserManager) getSystemService("user");
        this.mProfileSpinnerAdapter = Utils.createUserSpinnerAdapter(um, getActivity());
        if (this.mProfileSpinnerAdapter != null) {
            this.mSpinner = (Spinner) getActivity().getLayoutInflater().inflate(R.layout.spinner_view, (ViewGroup) null);
            this.mSpinner.setAdapter((SpinnerAdapter) this.mProfileSpinnerAdapter);
            this.mSpinner.setOnItemSelectedListener(this);
            setPinnedHeaderView(this.mSpinner);
        }
    }

    public void updateServicesPreferences() {
        if (getPreferenceScreen().findPreference("print_services_category") == null) {
            getPreferenceScreen().addPreference(this.mPrintServicesCategory);
        } else {
            this.mPrintServicesCategory.removeAll();
        }
        List<ComponentName> enabledServices = PrintSettingsUtils.readEnabledPrintServices(getActivity());
        List<ResolveInfo> installedServices = getActivity().getPackageManager().queryIntentServices(new Intent("android.printservice.PrintService"), 132);
        int installedServiceCount = installedServices.size();
        for (int i = 0; i < installedServiceCount; i++) {
            ResolveInfo installedService = installedServices.get(i);
            PreferenceScreen preference = getPreferenceManager().createPreferenceScreen(getActivity());
            String title = installedService.loadLabel(getPackageManager()).toString();
            preference.setTitle(title);
            ComponentName componentName = new ComponentName(installedService.serviceInfo.packageName, installedService.serviceInfo.name);
            preference.setKey(componentName.flattenToString());
            preference.setOrder(i);
            preference.setFragment(PrintServiceSettingsFragment.class.getName());
            preference.setPersistent(false);
            boolean serviceEnabled = enabledServices.contains(componentName);
            if (serviceEnabled) {
                preference.setSummary(getString(R.string.print_feature_state_on));
            } else {
                preference.setSummary(getString(R.string.print_feature_state_off));
            }
            Bundle extras = preference.getExtras();
            extras.putString("EXTRA_PREFERENCE_KEY", preference.getKey());
            extras.putBoolean("EXTRA_CHECKED", serviceEnabled);
            extras.putString("EXTRA_TITLE", title);
            PrintServiceInfo printServiceInfo = PrintServiceInfo.create(installedService, getActivity());
            CharSequence applicationLabel = installedService.loadLabel(getPackageManager());
            extras.putString("EXTRA_ENABLE_WARNING_TITLE", getString(R.string.print_service_security_warning_title, new Object[]{applicationLabel}));
            extras.putString("EXTRA_ENABLE_WARNING_MESSAGE", getString(R.string.print_service_security_warning_summary, new Object[]{applicationLabel}));
            String settingsClassName = printServiceInfo.getSettingsActivityName();
            if (!TextUtils.isEmpty(settingsClassName)) {
                extras.putString("EXTRA_SETTINGS_TITLE", getString(R.string.print_menu_item_settings));
                extras.putString("EXTRA_SETTINGS_COMPONENT_NAME", new ComponentName(installedService.serviceInfo.packageName, settingsClassName).flattenToString());
            }
            String addPrinterClassName = printServiceInfo.getAddPrintersActivityName();
            if (!TextUtils.isEmpty(addPrinterClassName)) {
                extras.putString("EXTRA_ADD_PRINTERS_TITLE", getString(R.string.print_menu_item_add_printers));
                extras.putString("EXTRA_ADD_PRINTERS_COMPONENT_NAME", new ComponentName(installedService.serviceInfo.packageName, addPrinterClassName).flattenToString());
            }
            extras.putString("EXTRA_SERVICE_COMPONENT_NAME", componentName.flattenToString());
            this.mPrintServicesCategory.addPreference(preference);
        }
        if (this.mPrintServicesCategory.getPreferenceCount() == 0) {
            getPreferenceScreen().removePreference(this.mPrintServicesCategory);
        }
    }

    private void startSubSettingsIfNeeded() {
        String componentName;
        if (getArguments() != null && (componentName = getArguments().getString("EXTRA_PRINT_SERVICE_COMPONENT_NAME")) != null) {
            getArguments().remove("EXTRA_PRINT_SERVICE_COMPONENT_NAME");
            Preference prereference = findPreference(componentName);
            if (prereference != null) {
                prereference.performClick(getPreferenceScreen());
            }
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        UserHandle selectedUser = this.mProfileSpinnerAdapter.getUserHandle(position);
        if (selectedUser.getIdentifier() != UserHandle.myUserId()) {
            Intent intent = new Intent("android.settings.ACTION_PRINT_SETTINGS");
            intent.addFlags(268435456);
            intent.addFlags(32768);
            getActivity().startActivityAsUser(intent, selectedUser);
            this.mSpinner.setSelection(0);
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }

    private class SettingsPackageMonitor extends PackageMonitor {
        private SettingsPackageMonitor() {
        }

        public void onPackageAdded(String packageName, int uid) {
            PrintSettingsFragment.this.mHandler.obtainMessage().sendToTarget();
        }

        public void onPackageAppeared(String packageName, int reason) {
            PrintSettingsFragment.this.mHandler.obtainMessage().sendToTarget();
        }

        public void onPackageDisappeared(String packageName, int reason) {
            PrintSettingsFragment.this.mHandler.obtainMessage().sendToTarget();
        }

        public void onPackageRemoved(String packageName, int uid) {
            PrintSettingsFragment.this.mHandler.obtainMessage().sendToTarget();
        }
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

    private final class PrintJobsController implements LoaderManager.LoaderCallbacks<List<PrintJobInfo>> {
        private PrintJobsController() {
        }

        @Override
        public Loader<List<PrintJobInfo>> onCreateLoader(int id, Bundle args) {
            if (id == 1) {
                return new PrintJobsLoader(PrintSettingsFragment.this.getActivity());
            }
            return null;
        }

        @Override
        public void onLoadFinished(Loader<List<PrintJobInfo>> loader, List<PrintJobInfo> printJobs) {
            if (printJobs == null || printJobs.isEmpty()) {
                PrintSettingsFragment.this.getPreferenceScreen().removePreference(PrintSettingsFragment.this.mActivePrintJobsCategory);
                return;
            }
            if (PrintSettingsFragment.this.getPreferenceScreen().findPreference("print_jobs_category") == null) {
                PrintSettingsFragment.this.getPreferenceScreen().addPreference(PrintSettingsFragment.this.mActivePrintJobsCategory);
            }
            PrintSettingsFragment.this.mActivePrintJobsCategory.removeAll();
            int printJobCount = printJobs.size();
            for (int i = 0; i < printJobCount; i++) {
                PrintJobInfo printJob = printJobs.get(i);
                PreferenceScreen preference = PrintSettingsFragment.this.getPreferenceManager().createPreferenceScreen(PrintSettingsFragment.this.getActivity());
                preference.setPersistent(false);
                preference.setFragment(PrintJobSettingsFragment.class.getName());
                preference.setKey(printJob.getId().flattenToString());
                switch (printJob.getState()) {
                    case 2:
                    case 3:
                        if (printJob.isCancelling()) {
                            preference.setTitle(PrintSettingsFragment.this.getString(R.string.print_cancelling_state_title_template, new Object[]{printJob.getLabel()}));
                        } else {
                            preference.setTitle(PrintSettingsFragment.this.getString(R.string.print_printing_state_title_template, new Object[]{printJob.getLabel()}));
                        }
                        break;
                    case 4:
                        if (printJob.isCancelling()) {
                            preference.setTitle(PrintSettingsFragment.this.getString(R.string.print_cancelling_state_title_template, new Object[]{printJob.getLabel()}));
                        } else {
                            preference.setTitle(PrintSettingsFragment.this.getString(R.string.print_blocked_state_title_template, new Object[]{printJob.getLabel()}));
                        }
                        break;
                    case 6:
                        preference.setTitle(PrintSettingsFragment.this.getString(R.string.print_failed_state_title_template, new Object[]{printJob.getLabel()}));
                        break;
                }
                preference.setSummary(PrintSettingsFragment.this.getString(R.string.print_job_summary, new Object[]{printJob.getPrinterName(), DateUtils.formatSameDayTime(printJob.getCreationTime(), printJob.getCreationTime(), 3, 3)}));
                switch (printJob.getState()) {
                    case 2:
                    case 3:
                        preference.setIcon(R.drawable.ic_print);
                        break;
                    case 4:
                    case 6:
                        preference.setIcon(R.drawable.ic_print_error);
                        break;
                }
                Bundle extras = preference.getExtras();
                extras.putString("EXTRA_PRINT_JOB_ID", printJob.getId().flattenToString());
                PrintSettingsFragment.this.mActivePrintJobsCategory.addPreference(preference);
            }
        }

        @Override
        public void onLoaderReset(Loader<List<PrintJobInfo>> loader) {
            PrintSettingsFragment.this.getPreferenceScreen().removePreference(PrintSettingsFragment.this.mActivePrintJobsCategory);
        }
    }

    private static final class PrintJobsLoader extends AsyncTaskLoader<List<PrintJobInfo>> {
        private PrintManager.PrintJobStateChangeListener mPrintJobStateChangeListener;
        private List<PrintJobInfo> mPrintJobs;
        private final PrintManager mPrintManager;

        public PrintJobsLoader(Context context) {
            super(context);
            this.mPrintJobs = new ArrayList();
            this.mPrintManager = ((PrintManager) context.getSystemService("print")).getGlobalPrintManagerForUser(context.getUserId());
        }

        @Override
        public void deliverResult(List<PrintJobInfo> printJobs) {
            if (isStarted()) {
                super.deliverResult(printJobs);
            }
        }

        @Override
        protected void onStartLoading() {
            if (!this.mPrintJobs.isEmpty()) {
                deliverResult((List<PrintJobInfo>) new ArrayList(this.mPrintJobs));
            }
            if (this.mPrintJobStateChangeListener == null) {
                this.mPrintJobStateChangeListener = new PrintManager.PrintJobStateChangeListener() {
                    public void onPrintJobStateChanged(PrintJobId printJobId) {
                        PrintJobsLoader.this.onForceLoad();
                    }
                };
                this.mPrintManager.addPrintJobStateChangeListener(this.mPrintJobStateChangeListener);
            }
            if (this.mPrintJobs.isEmpty()) {
                onForceLoad();
            }
        }

        @Override
        protected void onStopLoading() {
            onCancelLoad();
        }

        @Override
        protected void onReset() {
            onStopLoading();
            this.mPrintJobs.clear();
            if (this.mPrintJobStateChangeListener != null) {
                this.mPrintManager.removePrintJobStateChangeListener(this.mPrintJobStateChangeListener);
                this.mPrintJobStateChangeListener = null;
            }
        }

        @Override
        public List<PrintJobInfo> loadInBackground() {
            List<PrintJobInfo> printJobInfos = null;
            List<PrintJob> printJobs = this.mPrintManager.getPrintJobs();
            int printJobCount = printJobs.size();
            for (int i = 0; i < printJobCount; i++) {
                PrintJobInfo printJob = printJobs.get(i).getInfo();
                if (shouldShowToUser(printJob)) {
                    if (printJobInfos == null) {
                        printJobInfos = new ArrayList<>();
                    }
                    printJobInfos.add(printJob);
                }
            }
            return printJobInfos;
        }

        private static boolean shouldShowToUser(PrintJobInfo printJob) {
            switch (printJob.getState()) {
                case 2:
                case 3:
                case 4:
                case 6:
                    return true;
                case 5:
                default:
                    return false;
            }
        }
    }
}
