package com.android.printspooler.ui;

import android.app.ListActivity;
import android.app.LoaderManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.content.Loader;
import android.content.pm.ComponentInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.DataSetObserver;
import android.net.Uri;
import android.os.BenesseExtension;
import android.os.Bundle;
import android.print.PrintManager;
import android.print.PrintServiceRecommendationsLoader;
import android.print.PrintServicesLoader;
import android.printservice.PrintServiceInfo;
import android.printservice.recommendation.RecommendationInfo;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.internal.logging.MetricsLogger;
import com.android.printspooler.R;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class AddPrinterActivity extends ListActivity implements AdapterView.OnItemClickListener {
    private DisabledServicesAdapter mDisabledServicesAdapter;
    private EnabledServicesAdapter mEnabledServicesAdapter;
    private boolean mHasVending;
    private NoPrintServiceMessageAdapter mNoPrintServiceMessageAdapter;
    private RecommendedServicesAdapter mRecommendedServicesAdapter;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.add_printer_activity);
        try {
            getPackageManager().getPackageInfo("com.android.vending", 0);
            this.mHasVending = true;
        } catch (PackageManager.NameNotFoundException e) {
            this.mHasVending = false;
        }
        this.mEnabledServicesAdapter = new EnabledServicesAdapter();
        this.mDisabledServicesAdapter = new DisabledServicesAdapter();
        if (this.mHasVending) {
            this.mRecommendedServicesAdapter = new RecommendedServicesAdapter();
        } else {
            this.mNoPrintServiceMessageAdapter = new NoPrintServiceMessageAdapter();
        }
        ArrayList arrayList = new ArrayList(3);
        arrayList.add(this.mEnabledServicesAdapter);
        if (this.mHasVending) {
            arrayList.add(this.mRecommendedServicesAdapter);
        }
        arrayList.add(this.mDisabledServicesAdapter);
        if (!this.mHasVending) {
            arrayList.add(this.mNoPrintServiceMessageAdapter);
        }
        setListAdapter(new CombinedAdapter(arrayList));
        getListView().setOnItemClickListener(this);
        PrintServiceInfoLoaderCallbacks printServiceInfoLoaderCallbacks = new PrintServiceInfoLoaderCallbacks();
        getLoaderManager().initLoader(1, null, printServiceInfoLoaderCallbacks);
        getLoaderManager().initLoader(2, null, printServiceInfoLoaderCallbacks);
        if (this.mHasVending) {
            getLoaderManager().initLoader(3, null, new PrintServicePrintServiceRecommendationLoaderCallbacks());
        }
        getLoaderManager().initLoader(4, null, printServiceInfoLoaderCallbacks);
    }

    @Override
    protected void onDestroy() {
        if (isFinishing()) {
            MetricsLogger.action(this, 504, this.mEnabledServicesAdapter.getCount());
        }
        super.onDestroy();
    }

    private class PrintServiceInfoLoaderCallbacks implements LoaderManager.LoaderCallbacks<List<PrintServiceInfo>> {
        private PrintServiceInfoLoaderCallbacks() {
        }

        @Override
        public Loader<List<PrintServiceInfo>> onCreateLoader(int i, Bundle bundle) {
            if (i != 4) {
                switch (i) {
                    case 1:
                        return new PrintServicesLoader((PrintManager) AddPrinterActivity.this.getSystemService("print"), AddPrinterActivity.this, 1);
                    case 2:
                        return new PrintServicesLoader((PrintManager) AddPrinterActivity.this.getSystemService("print"), AddPrinterActivity.this, 2);
                    default:
                        return null;
                }
            }
            return new PrintServicesLoader((PrintManager) AddPrinterActivity.this.getSystemService("print"), AddPrinterActivity.this, 3);
        }

        @Override
        public void onLoadFinished(Loader<List<PrintServiceInfo>> loader, List<PrintServiceInfo> list) {
            int id = loader.getId();
            if (id != 4) {
                switch (id) {
                    case 1:
                        AddPrinterActivity.this.mEnabledServicesAdapter.updateData(list);
                        break;
                    case 2:
                        AddPrinterActivity.this.mDisabledServicesAdapter.updateData(list);
                        break;
                }
            }
            if (AddPrinterActivity.this.mHasVending) {
                AddPrinterActivity.this.mRecommendedServicesAdapter.updateInstalledServices(list);
            } else {
                AddPrinterActivity.this.mNoPrintServiceMessageAdapter.updateInstalledServices(list);
            }
        }

        @Override
        public void onLoaderReset(Loader<List<PrintServiceInfo>> loader) {
            if (!AddPrinterActivity.this.isFinishing()) {
                int id = loader.getId();
                if (id != 4) {
                    switch (id) {
                        case 1:
                            AddPrinterActivity.this.mEnabledServicesAdapter.updateData(null);
                            break;
                        case 2:
                            AddPrinterActivity.this.mDisabledServicesAdapter.updateData(null);
                            break;
                    }
                }
                if (AddPrinterActivity.this.mHasVending) {
                    AddPrinterActivity.this.mRecommendedServicesAdapter.updateInstalledServices(null);
                } else {
                    AddPrinterActivity.this.mNoPrintServiceMessageAdapter.updateInstalledServices(null);
                }
            }
        }
    }

    private class PrintServicePrintServiceRecommendationLoaderCallbacks implements LoaderManager.LoaderCallbacks<List<RecommendationInfo>> {
        private PrintServicePrintServiceRecommendationLoaderCallbacks() {
        }

        @Override
        public Loader<List<RecommendationInfo>> onCreateLoader(int i, Bundle bundle) {
            return new PrintServiceRecommendationsLoader((PrintManager) AddPrinterActivity.this.getSystemService("print"), AddPrinterActivity.this);
        }

        @Override
        public void onLoadFinished(Loader<List<RecommendationInfo>> loader, List<RecommendationInfo> list) {
            AddPrinterActivity.this.mRecommendedServicesAdapter.updateRecommendations(list);
        }

        @Override
        public void onLoaderReset(Loader<List<RecommendationInfo>> loader) {
            if (!AddPrinterActivity.this.isFinishing()) {
                AddPrinterActivity.this.mRecommendedServicesAdapter.updateRecommendations(null);
            }
        }
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long j) {
        ((ActionAdapter) getListAdapter()).performAction(i);
    }

    private abstract class ActionAdapter extends BaseAdapter {
        abstract void performAction(int i);

        private ActionAdapter() {
        }

        @Override
        public boolean areAllItemsEnabled() {
            return false;
        }
    }

    private class CombinedAdapter extends ActionAdapter {
        private final ArrayList<ActionAdapter> mAdapters;

        CombinedAdapter(ArrayList<ActionAdapter> arrayList) {
            super();
            this.mAdapters = arrayList;
            int size = this.mAdapters.size();
            for (int i = 0; i < size; i++) {
                this.mAdapters.get(i).registerDataSetObserver(new DataSetObserver() {
                    @Override
                    public void onChanged() {
                        CombinedAdapter.this.notifyDataSetChanged();
                    }

                    @Override
                    public void onInvalidated() {
                        CombinedAdapter.this.notifyDataSetChanged();
                    }
                });
            }
        }

        @Override
        public int getCount() {
            int size = this.mAdapters.size();
            int count = 0;
            for (int i = 0; i < size; i++) {
                count += this.mAdapters.get(i).getCount();
            }
            return count;
        }

        private Pair<ActionAdapter, Integer> getSubAdapter(int i) {
            int size = this.mAdapters.size();
            for (int i2 = 0; i2 < size; i2++) {
                ActionAdapter actionAdapter = this.mAdapters.get(i2);
                if (i < actionAdapter.getCount()) {
                    return new Pair<>(actionAdapter, Integer.valueOf(i));
                }
                i -= actionAdapter.getCount();
            }
            throw new IllegalArgumentException("Invalid position");
        }

        @Override
        public int getItemViewType(int i) {
            int size = this.mAdapters.size();
            int viewTypeCount = 0;
            for (int i2 = 0; i2 < size; i2++) {
                ActionAdapter actionAdapter = this.mAdapters.get(i2);
                if (i < actionAdapter.getCount()) {
                    return viewTypeCount + actionAdapter.getItemViewType(i);
                }
                viewTypeCount += actionAdapter.getViewTypeCount();
                i -= actionAdapter.getCount();
            }
            throw new IllegalArgumentException("Invalid position");
        }

        @Override
        public int getViewTypeCount() {
            int size = this.mAdapters.size();
            int viewTypeCount = 0;
            for (int i = 0; i < size; i++) {
                viewTypeCount += this.mAdapters.get(i).getViewTypeCount();
            }
            return viewTypeCount;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            Pair<ActionAdapter, Integer> subAdapter = getSubAdapter(i);
            return ((ActionAdapter) subAdapter.first).getView(((Integer) subAdapter.second).intValue(), view, viewGroup);
        }

        @Override
        public Object getItem(int i) {
            Pair<ActionAdapter, Integer> subAdapter = getSubAdapter(i);
            return ((ActionAdapter) subAdapter.first).getItem(((Integer) subAdapter.second).intValue());
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public boolean isEnabled(int i) {
            Pair<ActionAdapter, Integer> subAdapter = getSubAdapter(i);
            return ((ActionAdapter) subAdapter.first).isEnabled(((Integer) subAdapter.second).intValue());
        }

        @Override
        public void performAction(int i) {
            Pair<ActionAdapter, Integer> subAdapter = getSubAdapter(i);
            ((ActionAdapter) subAdapter.first).performAction(((Integer) subAdapter.second).intValue());
        }
    }

    private abstract class PrintServiceInfoAdapter extends ActionAdapter {
        private List<PrintServiceInfo> mServices;

        PrintServiceInfoAdapter() {
            super();
            this.mServices = Collections.emptyList();
        }

        void updateData(List<PrintServiceInfo> list) {
            if (list == null || list.isEmpty()) {
                this.mServices = Collections.emptyList();
            } else {
                this.mServices = list;
            }
            notifyDataSetChanged();
        }

        @Override
        public int getViewTypeCount() {
            return 2;
        }

        @Override
        public int getItemViewType(int i) {
            if (i == 0) {
                return 0;
            }
            return 1;
        }

        @Override
        public int getCount() {
            if (this.mServices.isEmpty()) {
                return 0;
            }
            return this.mServices.size() + 1;
        }

        @Override
        public Object getItem(int i) {
            if (i == 0) {
                return null;
            }
            return this.mServices.get(i - 1);
        }

        @Override
        public boolean isEnabled(int i) {
            return i != 0;
        }

        @Override
        public long getItemId(int i) {
            return i;
        }
    }

    private class EnabledServicesAdapter extends PrintServiceInfoAdapter {
        private EnabledServicesAdapter() {
            super();
        }

        @Override
        public void performAction(int i) {
            Intent addPrinterIntent = getAddPrinterIntent((PrintServiceInfo) getItem(i));
            if (addPrinterIntent != null) {
                try {
                    AddPrinterActivity.this.startActivity(addPrinterIntent);
                } catch (ActivityNotFoundException | SecurityException e) {
                    Log.e("AddPrinterActivity", "Cannot start add printers activity", e);
                }
            }
        }

        private Intent getAddPrinterIntent(PrintServiceInfo printServiceInfo) {
            String addPrintersActivityName = printServiceInfo.getAddPrintersActivityName();
            if (!TextUtils.isEmpty(addPrintersActivityName)) {
                Intent intent = new Intent("android.intent.action.MAIN");
                intent.setComponent(new ComponentName(printServiceInfo.getComponentName().getPackageName(), addPrintersActivityName));
                List<ResolveInfo> listQueryIntentActivities = AddPrinterActivity.this.getPackageManager().queryIntentActivities(intent, 0);
                if (!listQueryIntentActivities.isEmpty() && ((ComponentInfo) listQueryIntentActivities.get(0).activityInfo).exported) {
                    return intent;
                }
                return null;
            }
            return null;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            if (i == 0) {
                if (view == null) {
                    view = AddPrinterActivity.this.getLayoutInflater().inflate(R.layout.add_printer_list_header, viewGroup, false);
                }
                ((TextView) view.findViewById(R.id.text)).setText(R.string.enabled_services_title);
                return view;
            }
            if (view == null) {
                view = AddPrinterActivity.this.getLayoutInflater().inflate(R.layout.enabled_print_services_list_item, viewGroup, false);
            }
            PrintServiceInfo printServiceInfo = (PrintServiceInfo) getItem(i);
            TextView textView = (TextView) view.findViewById(R.id.title);
            ImageView imageView = (ImageView) view.findViewById(R.id.icon);
            TextView textView2 = (TextView) view.findViewById(R.id.subtitle);
            textView.setText(printServiceInfo.getResolveInfo().loadLabel(AddPrinterActivity.this.getPackageManager()));
            imageView.setImageDrawable(printServiceInfo.getResolveInfo().loadIcon(AddPrinterActivity.this.getPackageManager()));
            if (getAddPrinterIntent(printServiceInfo) == null) {
                textView2.setText(AddPrinterActivity.this.getString(R.string.cannot_add_printer));
            } else {
                textView2.setText(AddPrinterActivity.this.getString(R.string.select_to_add_printers));
            }
            return view;
        }
    }

    private class DisabledServicesAdapter extends PrintServiceInfoAdapter {
        private DisabledServicesAdapter() {
            super();
        }

        @Override
        public void performAction(int i) {
            ((PrintManager) AddPrinterActivity.this.getSystemService("print")).setPrintServiceEnabled(((PrintServiceInfo) getItem(i)).getComponentName(), true);
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            if (i == 0) {
                if (view == null) {
                    view = AddPrinterActivity.this.getLayoutInflater().inflate(R.layout.add_printer_list_header, viewGroup, false);
                }
                ((TextView) view.findViewById(R.id.text)).setText(R.string.disabled_services_title);
                return view;
            }
            if (view == null) {
                view = AddPrinterActivity.this.getLayoutInflater().inflate(R.layout.disabled_print_services_list_item, viewGroup, false);
            }
            PrintServiceInfo printServiceInfo = (PrintServiceInfo) getItem(i);
            TextView textView = (TextView) view.findViewById(R.id.title);
            ImageView imageView = (ImageView) view.findViewById(R.id.icon);
            textView.setText(printServiceInfo.getResolveInfo().loadLabel(AddPrinterActivity.this.getPackageManager()));
            imageView.setImageDrawable(printServiceInfo.getResolveInfo().loadIcon(AddPrinterActivity.this.getPackageManager()));
            return view;
        }
    }

    private class RecommendedServicesAdapter extends ActionAdapter {
        private List<RecommendationInfo> mFilteredRecommendations;
        private final ArraySet<String> mInstalledServices;
        private List<RecommendationInfo> mRecommendations;

        private RecommendedServicesAdapter() {
            super();
            this.mInstalledServices = new ArraySet<>();
        }

        @Override
        public int getCount() {
            if (this.mFilteredRecommendations == null) {
                return 2;
            }
            return this.mFilteredRecommendations.size() + 2;
        }

        @Override
        public int getViewTypeCount() {
            return 3;
        }

        private int getAllServicesPos() {
            return getCount() - 1;
        }

        @Override
        public int getItemViewType(int i) {
            if (i == 0) {
                return 0;
            }
            if (getAllServicesPos() == i) {
                return 1;
            }
            return 2;
        }

        @Override
        public Object getItem(int i) {
            if (i == 0 || i == getAllServicesPos()) {
                return null;
            }
            return this.mFilteredRecommendations.get(i - 1);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            if (i == 0) {
                if (view == null) {
                    view = AddPrinterActivity.this.getLayoutInflater().inflate(R.layout.add_printer_list_header, viewGroup, false);
                }
                ((TextView) view.findViewById(R.id.text)).setText(R.string.recommended_services_title);
                return view;
            }
            if (i == getAllServicesPos()) {
                if (view == null) {
                    return AddPrinterActivity.this.getLayoutInflater().inflate(R.layout.all_print_services_list_item, viewGroup, false);
                }
                return view;
            }
            RecommendationInfo recommendationInfo = (RecommendationInfo) getItem(i);
            if (view == null) {
                view = AddPrinterActivity.this.getLayoutInflater().inflate(R.layout.print_service_recommendations_list_item, viewGroup, false);
            }
            ((TextView) view.findViewById(R.id.title)).setText(recommendationInfo.getName());
            ((TextView) view.findViewById(R.id.subtitle)).setText(AddPrinterActivity.this.getResources().getQuantityString(R.plurals.print_services_recommendation_subtitle, recommendationInfo.getNumDiscoveredPrinters(), Integer.valueOf(recommendationInfo.getNumDiscoveredPrinters())));
            return view;
        }

        @Override
        public boolean isEnabled(int i) {
            return i != 0;
        }

        @Override
        public void performAction(int i) {
            if (i == getAllServicesPos()) {
                String string = Settings.Secure.getString(AddPrinterActivity.this.getContentResolver(), "print_service_search_uri");
                if (string != null) {
                    try {
                        if (BenesseExtension.getDchaState() == 0) {
                            AddPrinterActivity.this.startActivity(new Intent("android.intent.action.VIEW", Uri.parse(string)));
                            return;
                        }
                        return;
                    } catch (ActivityNotFoundException e) {
                        Log.e("AddPrinterActivity", "Cannot start market", e);
                        return;
                    }
                }
                return;
            }
            RecommendationInfo recommendationInfo = (RecommendationInfo) getItem(i);
            MetricsLogger.action(AddPrinterActivity.this, 512, recommendationInfo.getPackageName().toString());
            try {
                if (BenesseExtension.getDchaState() == 0) {
                    AddPrinterActivity.this.startActivity(new Intent("android.intent.action.VIEW", Uri.parse(AddPrinterActivity.this.getString(R.string.uri_package_details, recommendationInfo.getPackageName()))));
                }
            } catch (ActivityNotFoundException e2) {
                Log.e("AddPrinterActivity", "Cannot start market", e2);
            }
        }

        private void filterRecommendations() {
            if (this.mRecommendations == null) {
                this.mFilteredRecommendations = null;
            } else {
                this.mFilteredRecommendations = new ArrayList();
                int size = this.mRecommendations.size();
                for (int i = 0; i < size; i++) {
                    RecommendationInfo recommendationInfo = this.mRecommendations.get(i);
                    if (!this.mInstalledServices.contains(recommendationInfo.getPackageName())) {
                        this.mFilteredRecommendations.add(recommendationInfo);
                    }
                }
            }
            notifyDataSetChanged();
        }

        public void updateInstalledServices(List<PrintServiceInfo> list) {
            this.mInstalledServices.clear();
            if (list != null) {
                int size = list.size();
                for (int i = 0; i < size; i++) {
                    this.mInstalledServices.add(list.get(i).getComponentName().getPackageName());
                }
            }
            filterRecommendations();
        }

        public void updateRecommendations(List<RecommendationInfo> list) {
            if (list != null) {
                final Collator collator = Collator.getInstance();
                Collections.sort(list, new Comparator<RecommendationInfo>() {
                    @Override
                    public int compare(RecommendationInfo recommendationInfo, RecommendationInfo recommendationInfo2) {
                        if (recommendationInfo.getNumDiscoveredPrinters() != recommendationInfo2.getNumDiscoveredPrinters()) {
                            return recommendationInfo2.getNumDiscoveredPrinters() - recommendationInfo.getNumDiscoveredPrinters();
                        }
                        if (recommendationInfo.recommendsMultiVendorService() != recommendationInfo2.recommendsMultiVendorService()) {
                            if (recommendationInfo.recommendsMultiVendorService()) {
                                return 1;
                            }
                            return -1;
                        }
                        return collator.compare(recommendationInfo.getName().toString(), recommendationInfo2.getName().toString());
                    }
                });
            }
            this.mRecommendations = list;
            filterRecommendations();
        }
    }

    private class NoPrintServiceMessageAdapter extends ActionAdapter {
        private boolean mHasPrintService;

        private NoPrintServiceMessageAdapter() {
            super();
        }

        void updateInstalledServices(List<PrintServiceInfo> list) {
            if (list == null || list.isEmpty()) {
                this.mHasPrintService = false;
            } else {
                this.mHasPrintService = true;
            }
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return !this.mHasPrintService ? 1 : 0;
        }

        @Override
        public int getViewTypeCount() {
            return 1;
        }

        @Override
        public int getItemViewType(int i) {
            return 0;
        }

        @Override
        public Object getItem(int i) {
            return null;
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            if (view == null) {
                return AddPrinterActivity.this.getLayoutInflater().inflate(R.layout.no_print_services_message, viewGroup, false);
            }
            return view;
        }

        @Override
        public boolean isEnabled(int i) {
            return i != 0;
        }

        @Override
        public void performAction(int i) {
        }
    }
}
