package com.android.settings.print;

import android.app.LoaderManager;
import android.content.ActivityNotFoundException;
import android.content.AsyncTaskLoader;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
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
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import com.android.settings.R;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.utils.ProfileSettingsPreferenceFragment;
import java.util.ArrayList;
import java.util.List;

/* loaded from: classes.dex */
public class PrintSettingsFragment extends ProfileSettingsPreferenceFragment implements View.OnClickListener, Indexable {
    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER = new BaseSearchIndexProvider() { // from class: com.android.settings.print.PrintSettingsFragment.1
        @Override // com.android.settings.search.BaseSearchIndexProvider, com.android.settings.search.Indexable.SearchIndexProvider
        public List<SearchIndexableResource> getXmlResourcesToIndex(Context context, boolean z) {
            ArrayList arrayList = new ArrayList();
            SearchIndexableResource searchIndexableResource = new SearchIndexableResource(context);
            searchIndexableResource.xmlResId = R.xml.print_settings;
            arrayList.add(searchIndexableResource);
            return arrayList;
        }
    };
    private PreferenceCategory mActivePrintJobsCategory;
    private Button mAddNewServiceButton;
    private PrintJobsController mPrintJobsController;
    private PreferenceCategory mPrintServicesCategory;
    private PrintServicesController mPrintServicesController;

    @Override // com.android.settingslib.core.instrumentation.Instrumentable
    public int getMetricsCategory() {
        return 80;
    }

    @Override // com.android.settings.support.actionbar.HelpResourceProvider
    public int getHelpResource() {
        return R.string.help_uri_printing;
    }

    @Override // com.android.settings.SettingsPreferenceFragment, android.support.v14.preference.PreferenceFragment, android.app.Fragment
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        View viewOnCreateView = super.onCreateView(layoutInflater, viewGroup, bundle);
        addPreferencesFromResource(R.xml.print_settings);
        this.mActivePrintJobsCategory = (PreferenceCategory) findPreference("print_jobs_category");
        this.mPrintServicesCategory = (PreferenceCategory) findPreference("print_services_category");
        getPreferenceScreen().removePreference(this.mActivePrintJobsCategory);
        this.mPrintJobsController = new PrintJobsController();
        getLoaderManager().initLoader(1, null, this.mPrintJobsController);
        this.mPrintServicesController = new PrintServicesController();
        getLoaderManager().initLoader(2, null, this.mPrintServicesController);
        return viewOnCreateView;
    }

    @Override // com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.support.v14.preference.PreferenceFragment, android.app.Fragment
    public void onStart() {
        super.onStart();
        setHasOptionsMenu(true);
        startSubSettingsIfNeeded();
    }

    @Override // com.android.settings.utils.ProfileSettingsPreferenceFragment, android.support.v14.preference.PreferenceFragment, android.app.Fragment
    public void onViewCreated(View view, Bundle bundle) {
        super.onViewCreated(view, bundle);
        ViewGroup viewGroup = (ViewGroup) getListView().getParent();
        View viewInflate = getActivity().getLayoutInflater().inflate(R.layout.empty_print_state, viewGroup, false);
        ((TextView) viewInflate.findViewById(R.id.message)).setText(R.string.print_no_services_installed);
        if (createAddNewServiceIntentOrNull() != null) {
            this.mAddNewServiceButton = (Button) viewInflate.findViewById(R.id.add_new_service);
            this.mAddNewServiceButton.setOnClickListener(this);
            this.mAddNewServiceButton.setVisibility(0);
        }
        viewGroup.addView(viewInflate);
        setEmptyView(viewInflate);
    }

    @Override // com.android.settings.utils.ProfileSettingsPreferenceFragment
    protected String getIntentActionString() {
        return "android.settings.ACTION_PRINT_SETTINGS";
    }

    private final class PrintServicesController implements LoaderManager.LoaderCallbacks<List<PrintServiceInfo>> {
        private PrintServicesController() {
        }

        @Override // android.app.LoaderManager.LoaderCallbacks
        public Loader<List<PrintServiceInfo>> onCreateLoader(int i, Bundle bundle) {
            PrintManager printManager = (PrintManager) PrintSettingsFragment.this.getContext().getSystemService("print");
            if (printManager != null) {
                return new PrintServicesLoader(printManager, PrintSettingsFragment.this.getContext(), 3);
            }
            return null;
        }

        /* JADX DEBUG: Method merged with bridge method: onLoadFinished(Landroid/content/Loader;Ljava/lang/Object;)V */
        @Override // android.app.LoaderManager.LoaderCallbacks
        public void onLoadFinished(Loader<List<PrintServiceInfo>> loader, List<PrintServiceInfo> list) {
            if (list.isEmpty()) {
                PrintSettingsFragment.this.getPreferenceScreen().removePreference(PrintSettingsFragment.this.mPrintServicesCategory);
                return;
            }
            if (PrintSettingsFragment.this.getPreferenceScreen().findPreference("print_services_category") == null) {
                PrintSettingsFragment.this.getPreferenceScreen().addPreference(PrintSettingsFragment.this.mPrintServicesCategory);
            }
            PrintSettingsFragment.this.mPrintServicesCategory.removeAll();
            PackageManager packageManager = PrintSettingsFragment.this.getActivity().getPackageManager();
            Context prefContext = PrintSettingsFragment.this.getPrefContext();
            if (prefContext == null) {
                Log.w("PrintSettingsFragment", "No preference context, skip adding print services");
                return;
            }
            for (PrintServiceInfo printServiceInfo : list) {
                Preference preference = new Preference(prefContext);
                String string = printServiceInfo.getResolveInfo().loadLabel(packageManager).toString();
                preference.setTitle(string);
                ComponentName componentName = printServiceInfo.getComponentName();
                preference.setKey(componentName.flattenToString());
                preference.setFragment(PrintServiceSettingsFragment.class.getName());
                preference.setPersistent(false);
                if (printServiceInfo.isEnabled()) {
                    preference.setSummary(PrintSettingsFragment.this.getString(R.string.print_feature_state_on));
                } else {
                    preference.setSummary(PrintSettingsFragment.this.getString(R.string.print_feature_state_off));
                }
                Drawable drawableLoadIcon = printServiceInfo.getResolveInfo().loadIcon(packageManager);
                if (drawableLoadIcon != null) {
                    preference.setIcon(drawableLoadIcon);
                }
                Bundle extras = preference.getExtras();
                extras.putBoolean("EXTRA_CHECKED", printServiceInfo.isEnabled());
                extras.putString("EXTRA_TITLE", string);
                extras.putString("EXTRA_SERVICE_COMPONENT_NAME", componentName.flattenToString());
                PrintSettingsFragment.this.mPrintServicesCategory.addPreference(preference);
            }
            Preference preferenceNewAddServicePreferenceOrNull = PrintSettingsFragment.this.newAddServicePreferenceOrNull();
            if (preferenceNewAddServicePreferenceOrNull != null) {
                PrintSettingsFragment.this.mPrintServicesCategory.addPreference(preferenceNewAddServicePreferenceOrNull);
            }
        }

        @Override // android.app.LoaderManager.LoaderCallbacks
        public void onLoaderReset(Loader<List<PrintServiceInfo>> loader) {
            PrintSettingsFragment.this.getPreferenceScreen().removePreference(PrintSettingsFragment.this.mPrintServicesCategory);
        }
    }

    private Preference newAddServicePreferenceOrNull() {
        Intent intentCreateAddNewServiceIntentOrNull = createAddNewServiceIntentOrNull();
        if (intentCreateAddNewServiceIntentOrNull == null) {
            return null;
        }
        Preference preference = new Preference(getPrefContext());
        preference.setTitle(R.string.print_menu_item_add_service);
        preference.setIcon(R.drawable.ic_menu_add);
        preference.setOrder(2147483646);
        preference.setIntent(intentCreateAddNewServiceIntentOrNull);
        preference.setPersistent(false);
        return preference;
    }

    private Intent createAddNewServiceIntentOrNull() {
        String string = Settings.Secure.getString(getContentResolver(), "print_service_search_uri");
        if (TextUtils.isEmpty(string)) {
            return null;
        }
        return new Intent("android.intent.action.VIEW", Uri.parse(string));
    }

    private void startSubSettingsIfNeeded() {
        String string;
        if (getArguments() != null && (string = getArguments().getString("EXTRA_PRINT_SERVICE_COMPONENT_NAME")) != null) {
            getArguments().remove("EXTRA_PRINT_SERVICE_COMPONENT_NAME");
            Preference preferenceFindPreference = findPreference(string);
            if (preferenceFindPreference != null) {
                preferenceFindPreference.performClick();
            }
        }
    }

    @Override // android.view.View.OnClickListener
    public void onClick(View view) {
        Intent intentCreateAddNewServiceIntentOrNull;
        if (this.mAddNewServiceButton == view && (intentCreateAddNewServiceIntentOrNull = createAddNewServiceIntentOrNull()) != null) {
            try {
                startActivity(intentCreateAddNewServiceIntentOrNull);
            } catch (ActivityNotFoundException e) {
                Log.w("PrintSettingsFragment", "Unable to start activity", e);
            }
        }
    }

    private final class PrintJobsController implements LoaderManager.LoaderCallbacks<List<PrintJobInfo>> {
        private PrintJobsController() {
        }

        @Override // android.app.LoaderManager.LoaderCallbacks
        public Loader<List<PrintJobInfo>> onCreateLoader(int i, Bundle bundle) {
            if (i == 1) {
                return new PrintJobsLoader(PrintSettingsFragment.this.getContext());
            }
            return null;
        }

        /* JADX DEBUG: Method merged with bridge method: onLoadFinished(Landroid/content/Loader;Ljava/lang/Object;)V */
        /* JADX WARN: Removed duplicated region for block: B:36:0x014a  */
        @Override // android.app.LoaderManager.LoaderCallbacks
        /*
            Code decompiled incorrectly, please refer to instructions dump.
        */
        public void onLoadFinished(Loader<List<PrintJobInfo>> loader, List<PrintJobInfo> list) {
            if (list == null || list.isEmpty()) {
                PrintSettingsFragment.this.getPreferenceScreen().removePreference(PrintSettingsFragment.this.mActivePrintJobsCategory);
                return;
            }
            if (PrintSettingsFragment.this.getPreferenceScreen().findPreference("print_jobs_category") == null) {
                PrintSettingsFragment.this.getPreferenceScreen().addPreference(PrintSettingsFragment.this.mActivePrintJobsCategory);
            }
            PrintSettingsFragment.this.mActivePrintJobsCategory.removeAll();
            Context prefContext = PrintSettingsFragment.this.getPrefContext();
            if (prefContext == null) {
                Log.w("PrintSettingsFragment", "No preference context, skip adding print jobs");
                return;
            }
            for (PrintJobInfo printJobInfo : list) {
                Preference preference = new Preference(prefContext);
                preference.setPersistent(false);
                preference.setFragment(PrintJobSettingsFragment.class.getName());
                preference.setKey(printJobInfo.getId().flattenToString());
                int state = printJobInfo.getState();
                if (state != 6) {
                    switch (state) {
                        case 2:
                        case 3:
                            if (!printJobInfo.isCancelling()) {
                                preference.setTitle(PrintSettingsFragment.this.getString(R.string.print_printing_state_title_template, new Object[]{printJobInfo.getLabel()}));
                                break;
                            } else {
                                preference.setTitle(PrintSettingsFragment.this.getString(R.string.print_cancelling_state_title_template, new Object[]{printJobInfo.getLabel()}));
                                break;
                            }
                        case 4:
                            if (!printJobInfo.isCancelling()) {
                                preference.setTitle(PrintSettingsFragment.this.getString(R.string.print_blocked_state_title_template, new Object[]{printJobInfo.getLabel()}));
                                break;
                            } else {
                                preference.setTitle(PrintSettingsFragment.this.getString(R.string.print_cancelling_state_title_template, new Object[]{printJobInfo.getLabel()}));
                                break;
                            }
                    }
                } else {
                    preference.setTitle(PrintSettingsFragment.this.getString(R.string.print_failed_state_title_template, new Object[]{printJobInfo.getLabel()}));
                }
                preference.setSummary(PrintSettingsFragment.this.getString(R.string.print_job_summary, new Object[]{printJobInfo.getPrinterName(), DateUtils.formatSameDayTime(printJobInfo.getCreationTime(), printJobInfo.getCreationTime(), 3, 3)}));
                TypedArray typedArrayObtainStyledAttributes = PrintSettingsFragment.this.getActivity().obtainStyledAttributes(new int[]{android.R.attr.colorControlNormal});
                int color = typedArrayObtainStyledAttributes.getColor(0, 0);
                typedArrayObtainStyledAttributes.recycle();
                int state2 = printJobInfo.getState();
                if (state2 != 6) {
                    switch (state2) {
                        case 2:
                        case 3:
                            Drawable drawable = PrintSettingsFragment.this.getActivity().getDrawable(android.R.drawable.ic_media_route_connecting_dark_26_mtrl);
                            drawable.setTint(color);
                            preference.setIcon(drawable);
                            break;
                        case 4:
                            Drawable drawable2 = PrintSettingsFragment.this.getActivity().getDrawable(android.R.drawable.ic_media_route_connecting_dark_27_mtrl);
                            drawable2.setTint(color);
                            preference.setIcon(drawable2);
                            break;
                    }
                }
                preference.getExtras().putString("EXTRA_PRINT_JOB_ID", printJobInfo.getId().flattenToString());
                PrintSettingsFragment.this.mActivePrintJobsCategory.addPreference(preference);
            }
        }

        @Override // android.app.LoaderManager.LoaderCallbacks
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

        /* JADX DEBUG: Method merged with bridge method: deliverResult(Ljava/lang/Object;)V */
        @Override // android.content.Loader
        public void deliverResult(List<PrintJobInfo> list) {
            if (isStarted()) {
                super.deliverResult((PrintJobsLoader) list);
            }
        }

        @Override // android.content.Loader
        protected void onStartLoading() {
            if (!this.mPrintJobs.isEmpty()) {
                deliverResult((List<PrintJobInfo>) new ArrayList(this.mPrintJobs));
            }
            if (this.mPrintJobStateChangeListener == null) {
                this.mPrintJobStateChangeListener = new PrintManager.PrintJobStateChangeListener() { // from class: com.android.settings.print.PrintSettingsFragment.PrintJobsLoader.1
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

        @Override // android.content.Loader
        protected void onStopLoading() {
            onCancelLoad();
        }

        @Override // android.content.Loader
        protected void onReset() {
            onStopLoading();
            this.mPrintJobs.clear();
            if (this.mPrintJobStateChangeListener != null) {
                this.mPrintManager.removePrintJobStateChangeListener(this.mPrintJobStateChangeListener);
                this.mPrintJobStateChangeListener = null;
            }
        }

        /* JADX DEBUG: Method merged with bridge method: loadInBackground()Ljava/lang/Object; */
        @Override // android.content.AsyncTaskLoader
        public List<PrintJobInfo> loadInBackground() {
            List<PrintJob> printJobs = this.mPrintManager.getPrintJobs();
            int size = printJobs.size();
            ArrayList arrayList = null;
            for (int i = 0; i < size; i++) {
                PrintJobInfo info = printJobs.get(i).getInfo();
                if (PrintSettingPreferenceController.shouldShowToUser(info)) {
                    if (arrayList == null) {
                        arrayList = new ArrayList();
                    }
                    arrayList.add(info);
                }
            }
            return arrayList;
        }
    }
}
