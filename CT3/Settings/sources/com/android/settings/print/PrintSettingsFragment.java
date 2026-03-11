package com.android.settings.print;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.ActivityNotFoundException;
import android.content.AsyncTaskLoader;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.print.PrintJob;
import android.print.PrintJobId;
import android.print.PrintJobInfo;
import android.print.PrintManager;
import android.print.PrintServicesLoader;
import android.printservice.PrintServiceInfo;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceScreen;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import com.android.settings.DialogCreatable;
import com.android.settings.R;
import com.android.settings.dashboard.SummaryLoader;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.search.SearchIndexableRaw;
import com.android.settings.utils.ProfileSettingsPreferenceFragment;
import com.mediatek.settings.ext.DefaultWfcSettingsExt;
import java.util.ArrayList;
import java.util.List;

public class PrintSettingsFragment extends ProfileSettingsPreferenceFragment implements DialogCreatable, Indexable, View.OnClickListener {
    private PreferenceCategory mActivePrintJobsCategory;
    private Button mAddNewServiceButton;
    private PrintJobsController mPrintJobsController;
    private PreferenceCategory mPrintServicesCategory;
    private PrintServicesController mPrintServicesController;
    public static final SummaryLoader.SummaryProviderFactory SUMMARY_PROVIDER_FACTORY = new SummaryLoader.SummaryProviderFactory() {
        @Override
        public SummaryLoader.SummaryProvider createSummaryProvider(Activity activity, SummaryLoader summaryLoader) {
            return new PrintSummaryProvider(activity, summaryLoader);
        }
    };
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
            List<PrintServiceInfo> services = printManager.getPrintServices(3);
            if (services != null) {
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

    @Override
    protected int getMetricsCategory() {
        return 80;
    }

    @Override
    protected int getHelpResource() {
        return R.string.help_uri_printing;
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        addPreferencesFromResource(R.xml.print_settings);
        this.mActivePrintJobsCategory = (PreferenceCategory) findPreference("print_jobs_category");
        this.mPrintServicesCategory = (PreferenceCategory) findPreference("print_services_category");
        getPreferenceScreen().removePreference(this.mActivePrintJobsCategory);
        this.mPrintJobsController = new PrintJobsController(this, null);
        getLoaderManager().initLoader(1, null, this.mPrintJobsController);
        this.mPrintServicesController = new PrintServicesController(this, 0 == true ? 1 : 0);
        getLoaderManager().initLoader(2, null, this.mPrintServicesController);
    }

    @Override
    public void onStart() {
        super.onStart();
        setHasOptionsMenu(true);
        startSubSettingsIfNeeded();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ViewGroup contentRoot = (ViewGroup) getListView().getParent();
        View emptyView = getActivity().getLayoutInflater().inflate(R.layout.empty_print_state, contentRoot, false);
        TextView textView = (TextView) emptyView.findViewById(R.id.message);
        textView.setText(R.string.print_no_services_installed);
        Intent addNewServiceIntent = createAddNewServiceIntentOrNull();
        if (addNewServiceIntent != null) {
            this.mAddNewServiceButton = (Button) emptyView.findViewById(R.id.add_new_service);
            this.mAddNewServiceButton.setOnClickListener(this);
            this.mAddNewServiceButton.setVisibility(0);
        }
        contentRoot.addView(emptyView);
        setEmptyView(emptyView);
    }

    @Override
    protected String getIntentActionString() {
        return "android.settings.ACTION_PRINT_SETTINGS";
    }

    private final class PrintServicesController implements LoaderManager.LoaderCallbacks<List<PrintServiceInfo>> {
        PrintServicesController(PrintSettingsFragment this$0, PrintServicesController printServicesController) {
            this();
        }

        private PrintServicesController() {
        }

        @Override
        public Loader<List<PrintServiceInfo>> onCreateLoader(int id, Bundle args) {
            PrintManager printManager = (PrintManager) PrintSettingsFragment.this.getContext().getSystemService("print");
            if (printManager != null) {
                return new PrintServicesLoader(printManager, PrintSettingsFragment.this.getContext(), 3);
            }
            return null;
        }

        @Override
        public void onLoadFinished(Loader<List<PrintServiceInfo>> loader, List<PrintServiceInfo> services) {
            if (services.isEmpty()) {
                PrintSettingsFragment.this.getPreferenceScreen().removePreference(PrintSettingsFragment.this.mPrintServicesCategory);
                return;
            }
            if (PrintSettingsFragment.this.getPreferenceScreen().findPreference("print_services_category") == null) {
                PrintSettingsFragment.this.getPreferenceScreen().addPreference(PrintSettingsFragment.this.mPrintServicesCategory);
            }
            PrintSettingsFragment.this.mPrintServicesCategory.removeAll();
            PackageManager pm = PrintSettingsFragment.this.getActivity().getPackageManager();
            int numServices = services.size();
            for (int i = 0; i < numServices; i++) {
                PrintServiceInfo service = services.get(i);
                PreferenceScreen preference = PrintSettingsFragment.this.getPreferenceManager().createPreferenceScreen(PrintSettingsFragment.this.getActivity());
                String title = service.getResolveInfo().loadLabel(pm).toString();
                preference.setTitle(title);
                ComponentName componentName = service.getComponentName();
                preference.setKey(componentName.flattenToString());
                preference.setFragment(PrintServiceSettingsFragment.class.getName());
                preference.setPersistent(false);
                if (service.isEnabled()) {
                    preference.setSummary(PrintSettingsFragment.this.getString(R.string.print_feature_state_on));
                } else {
                    preference.setSummary(PrintSettingsFragment.this.getString(R.string.print_feature_state_off));
                }
                Drawable drawable = service.getResolveInfo().loadIcon(pm);
                if (drawable != null) {
                    preference.setIcon(drawable);
                }
                Bundle extras = preference.getExtras();
                extras.putBoolean("EXTRA_CHECKED", service.isEnabled());
                extras.putString("EXTRA_TITLE", title);
                extras.putString("EXTRA_SERVICE_COMPONENT_NAME", componentName.flattenToString());
                PrintSettingsFragment.this.mPrintServicesCategory.addPreference(preference);
            }
            Preference addNewServicePreference = PrintSettingsFragment.this.newAddServicePreferenceOrNull();
            if (addNewServicePreference == null) {
                return;
            }
            PrintSettingsFragment.this.mPrintServicesCategory.addPreference(addNewServicePreference);
        }

        @Override
        public void onLoaderReset(Loader<List<PrintServiceInfo>> loader) {
            PrintSettingsFragment.this.getPreferenceScreen().removePreference(PrintSettingsFragment.this.mPrintServicesCategory);
        }
    }

    public Preference newAddServicePreferenceOrNull() {
        Intent addNewServiceIntent = createAddNewServiceIntentOrNull();
        if (addNewServiceIntent == null) {
            return null;
        }
        Preference preference = new Preference(getPrefContext());
        preference.setTitle(R.string.print_menu_item_add_service);
        preference.setIcon(R.drawable.ic_menu_add);
        preference.setOrder(2147483646);
        preference.setIntent(addNewServiceIntent);
        preference.setPersistent(false);
        return preference;
    }

    private Intent createAddNewServiceIntentOrNull() {
        String searchUri = Settings.Secure.getString(getContentResolver(), "print_service_search_uri");
        if (TextUtils.isEmpty(searchUri)) {
            return null;
        }
        return new Intent("android.intent.action.VIEW", Uri.parse(searchUri));
    }

    private void startSubSettingsIfNeeded() {
        String componentName;
        if (getArguments() == null || (componentName = getArguments().getString("EXTRA_PRINT_SERVICE_COMPONENT_NAME")) == null) {
            return;
        }
        getArguments().remove("EXTRA_PRINT_SERVICE_COMPONENT_NAME");
        Preference prereference = findPreference(componentName);
        if (prereference == null) {
            return;
        }
        prereference.performClick();
    }

    @Override
    public void onClick(View v) {
        Intent addNewServiceIntent;
        if (this.mAddNewServiceButton != v || (addNewServiceIntent = createAddNewServiceIntentOrNull()) == null) {
            return;
        }
        try {
            startActivity(addNewServiceIntent);
        } catch (ActivityNotFoundException e) {
            Log.w("PrintSettingsFragment", "Unable to start activity", e);
        }
    }

    private final class PrintJobsController implements LoaderManager.LoaderCallbacks<List<PrintJobInfo>> {
        PrintJobsController(PrintSettingsFragment this$0, PrintJobsController printJobsController) {
            this();
        }

        private PrintJobsController() {
        }

        @Override
        public Loader<List<PrintJobInfo>> onCreateLoader(int id, Bundle args) {
            if (id == 1) {
                return new PrintJobsLoader(PrintSettingsFragment.this.getContext());
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
                    case DefaultWfcSettingsExt.CREATE:
                    case DefaultWfcSettingsExt.DESTROY:
                        if (!printJob.isCancelling()) {
                            preference.setTitle(PrintSettingsFragment.this.getString(R.string.print_printing_state_title_template, new Object[]{printJob.getLabel()}));
                        } else {
                            preference.setTitle(PrintSettingsFragment.this.getString(R.string.print_cancelling_state_title_template, new Object[]{printJob.getLabel()}));
                        }
                        break;
                    case DefaultWfcSettingsExt.CONFIG_CHANGE:
                        if (!printJob.isCancelling()) {
                            preference.setTitle(PrintSettingsFragment.this.getString(R.string.print_blocked_state_title_template, new Object[]{printJob.getLabel()}));
                        } else {
                            preference.setTitle(PrintSettingsFragment.this.getString(R.string.print_cancelling_state_title_template, new Object[]{printJob.getLabel()}));
                        }
                        break;
                    case 6:
                        preference.setTitle(PrintSettingsFragment.this.getString(R.string.print_failed_state_title_template, new Object[]{printJob.getLabel()}));
                        break;
                }
                preference.setSummary(PrintSettingsFragment.this.getString(R.string.print_job_summary, new Object[]{printJob.getPrinterName(), DateUtils.formatSameDayTime(printJob.getCreationTime(), printJob.getCreationTime(), 3, 3)}));
                switch (printJob.getState()) {
                    case DefaultWfcSettingsExt.CREATE:
                    case DefaultWfcSettingsExt.DESTROY:
                        preference.setIcon(R.drawable.ic_print);
                        break;
                    case DefaultWfcSettingsExt.CONFIG_CHANGE:
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
            if (!isStarted()) {
                return;
            }
            super.deliverResult(printJobs);
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
            if (!this.mPrintJobs.isEmpty()) {
                return;
            }
            onForceLoad();
        }

        @Override
        protected void onStopLoading() {
            onCancelLoad();
        }

        @Override
        protected void onReset() {
            onStopLoading();
            this.mPrintJobs.clear();
            if (this.mPrintJobStateChangeListener == null) {
                return;
            }
            this.mPrintManager.removePrintJobStateChangeListener(this.mPrintJobStateChangeListener);
            this.mPrintJobStateChangeListener = null;
        }

        @Override
        public List<PrintJobInfo> loadInBackground() {
            List<PrintJobInfo> printJobInfos = null;
            List<PrintJob> printJobs = this.mPrintManager.getPrintJobs();
            int printJobCount = printJobs.size();
            for (int i = 0; i < printJobCount; i++) {
                PrintJobInfo printJob = printJobs.get(i).getInfo();
                if (PrintSettingsFragment.shouldShowToUser(printJob)) {
                    if (printJobInfos == null) {
                        printJobInfos = new ArrayList<>();
                    }
                    printJobInfos.add(printJob);
                }
            }
            return printJobInfos;
        }
    }

    public static boolean shouldShowToUser(PrintJobInfo printJob) {
        switch (printJob.getState()) {
            case DefaultWfcSettingsExt.CREATE:
            case DefaultWfcSettingsExt.DESTROY:
            case DefaultWfcSettingsExt.CONFIG_CHANGE:
            case 6:
                return true;
            case 5:
            default:
                return false;
        }
    }

    private static class PrintSummaryProvider implements SummaryLoader.SummaryProvider, PrintManager.PrintJobStateChangeListener {
        private final Context mContext;
        private final PrintManager mPrintManager;
        private final SummaryLoader mSummaryLoader;

        public PrintSummaryProvider(Context context, SummaryLoader summaryLoader) {
            this.mContext = context;
            this.mSummaryLoader = summaryLoader;
            this.mPrintManager = ((PrintManager) context.getSystemService("print")).getGlobalPrintManagerForUser(context.getUserId());
        }

        @Override
        public void setListening(boolean isListening) {
            if (this.mPrintManager == null) {
                return;
            }
            if (isListening) {
                this.mPrintManager.addPrintJobStateChangeListener(this);
                onPrintJobStateChanged(null);
            } else {
                this.mPrintManager.removePrintJobStateChangeListener(this);
            }
        }

        public void onPrintJobStateChanged(PrintJobId printJobId) {
            List<PrintJob> printJobs = this.mPrintManager.getPrintJobs();
            int numActivePrintJobs = 0;
            int numPrintJobs = printJobs.size();
            for (int i = 0; i < numPrintJobs; i++) {
                if (PrintSettingsFragment.shouldShowToUser(printJobs.get(i).getInfo())) {
                    numActivePrintJobs++;
                }
            }
            this.mSummaryLoader.setSummary(this, this.mContext.getResources().getQuantityString(R.plurals.print_settings_title, numActivePrintJobs, Integer.valueOf(numActivePrintJobs)));
        }
    }
}
