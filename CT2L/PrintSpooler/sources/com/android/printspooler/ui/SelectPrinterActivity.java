package com.android.printspooler.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.database.DataSetObserver;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.print.PrintManager;
import android.print.PrinterId;
import android.print.PrinterInfo;
import android.printservice.PrintServiceInfo;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;
import com.android.printspooler.R;
import com.android.printspooler.ui.PrinterRegistry;
import java.util.ArrayList;
import java.util.List;

public final class SelectPrinterActivity extends Activity {
    private final ArrayList<PrintServiceInfo> mAddPrinterServices = new ArrayList<>();
    private AnnounceFilterResult mAnnounceFilterResult;
    private ListView mListView;
    private PrinterRegistry mPrinterRegistry;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActionBar().setIcon(R.drawable.ic_print);
        setContentView(R.layout.select_printer_activity);
        this.mPrinterRegistry = new PrinterRegistry(this, null);
        this.mListView = (ListView) findViewById(android.R.id.list);
        final DestinationAdapter adapter = new DestinationAdapter();
        adapter.registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                if (!SelectPrinterActivity.this.isFinishing() && adapter.getCount() <= 0) {
                    SelectPrinterActivity.this.updateEmptyView(adapter);
                }
            }

            @Override
            public void onInvalidated() {
                if (!SelectPrinterActivity.this.isFinishing()) {
                    SelectPrinterActivity.this.updateEmptyView(adapter);
                }
            }
        });
        this.mListView.setAdapter((ListAdapter) adapter);
        this.mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (((DestinationAdapter) SelectPrinterActivity.this.mListView.getAdapter()).isActionable(position)) {
                    PrinterInfo printer = (PrinterInfo) SelectPrinterActivity.this.mListView.getAdapter().getItem(position);
                    SelectPrinterActivity.this.onPrinterSelected(printer.getId());
                }
            }
        });
        registerForContextMenu(this.mListView);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.select_printer_activity, menu);
        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) searchItem.getActionView();
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return true;
            }

            @Override
            public boolean onQueryTextChange(String searchString) {
                ((DestinationAdapter) SelectPrinterActivity.this.mListView.getAdapter()).getFilter().filter(searchString);
                return true;
            }
        });
        searchView.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(View view) {
                if (AccessibilityManager.getInstance(SelectPrinterActivity.this).isEnabled()) {
                    view.announceForAccessibility(SelectPrinterActivity.this.getString(R.string.print_search_box_shown_utterance));
                }
            }

            @Override
            public void onViewDetachedFromWindow(View view) {
                if (!SelectPrinterActivity.this.isFinishing() && AccessibilityManager.getInstance(SelectPrinterActivity.this).isEnabled()) {
                    view.announceForAccessibility(SelectPrinterActivity.this.getString(R.string.print_search_box_hidden_utterance));
                }
            }
        });
        if (this.mAddPrinterServices.isEmpty()) {
            menu.removeItem(R.id.action_add_printer);
            return true;
        }
        return true;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo menuInfo) {
        if (view == this.mListView) {
            int position = ((AdapterView.AdapterContextMenuInfo) menuInfo).position;
            PrinterInfo printer = (PrinterInfo) this.mListView.getAdapter().getItem(position);
            menu.setHeaderTitle(printer.getName());
            if (printer.getStatus() != 3) {
                MenuItem selectItem = menu.add(0, R.string.print_select_printer, 0, R.string.print_select_printer);
                Intent intent = new Intent();
                intent.putExtra("EXTRA_PRINTER_ID", printer.getId());
                selectItem.setIntent(intent);
            }
            if (this.mPrinterRegistry.isFavoritePrinter(printer.getId())) {
                MenuItem forgetItem = menu.add(0, R.string.print_forget_printer, 0, R.string.print_forget_printer);
                Intent intent2 = new Intent();
                intent2.putExtra("EXTRA_PRINTER_ID", printer.getId());
                forgetItem.setIntent(intent2);
            }
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.string.print_select_printer:
                PrinterId printerId = (PrinterId) item.getIntent().getParcelableExtra("EXTRA_PRINTER_ID");
                onPrinterSelected(printerId);
                break;
            case R.string.print_forget_printer:
                PrinterId printerId2 = (PrinterId) item.getIntent().getParcelableExtra("EXTRA_PRINTER_ID");
                this.mPrinterRegistry.forgetFavoritePrinter(printerId2);
                break;
        }
        return true;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateServicesWithAddPrinterActivity();
        invalidateOptionsMenu();
    }

    @Override
    public void onPause() {
        if (this.mAnnounceFilterResult != null) {
            this.mAnnounceFilterResult.remove();
        }
        super.onPause();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() != R.id.action_add_printer) {
            return super.onOptionsItemSelected(item);
        }
        showAddPrinterSelectionDialog();
        return true;
    }

    private void onPrinterSelected(PrinterId printerId) {
        Intent intent = new Intent();
        intent.putExtra("INTENT_EXTRA_PRINTER_ID", printerId);
        setResult(-1, intent);
        finish();
    }

    private void updateServicesWithAddPrinterActivity() {
        this.mAddPrinterServices.clear();
        PrintManager printManager = (PrintManager) getSystemService("print");
        List<PrintServiceInfo> enabledServices = printManager.getEnabledPrintServices();
        if (!enabledServices.isEmpty()) {
            int enabledServiceCount = enabledServices.size();
            for (int i = 0; i < enabledServiceCount; i++) {
                PrintServiceInfo enabledService = enabledServices.get(i);
                if (!TextUtils.isEmpty(enabledService.getAddPrintersActivityName())) {
                    ServiceInfo serviceInfo = enabledService.getResolveInfo().serviceInfo;
                    ComponentName addPrintersComponentName = new ComponentName(serviceInfo.packageName, enabledService.getAddPrintersActivityName());
                    Intent addPritnersIntent = new Intent().setComponent(addPrintersComponentName);
                    PackageManager pm = getPackageManager();
                    List<ResolveInfo> resolvedActivities = pm.queryIntentActivities(addPritnersIntent, 0);
                    if (!resolvedActivities.isEmpty()) {
                        ActivityInfo activityInfo = resolvedActivities.get(0).activityInfo;
                        if (activityInfo.exported && (activityInfo.permission == null || pm.checkPermission(activityInfo.permission, getPackageName()) == 0)) {
                            this.mAddPrinterServices.add(enabledService);
                        }
                    }
                }
            }
        }
    }

    private void showAddPrinterSelectionDialog() {
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        Fragment oldFragment = getFragmentManager().findFragmentByTag("FRAGMENT_TAG_ADD_PRINTER_DIALOG");
        if (oldFragment != null) {
            transaction.remove(oldFragment);
        }
        AddPrinterAlertDialogFragment newFragment = new AddPrinterAlertDialogFragment();
        Bundle arguments = new Bundle();
        arguments.putParcelableArrayList("FRAGMENT_ARGUMENT_PRINT_SERVICE_INFOS", this.mAddPrinterServices);
        newFragment.setArguments(arguments);
        transaction.add(newFragment, "FRAGMENT_TAG_ADD_PRINTER_DIALOG");
        transaction.commit();
    }

    public void updateEmptyView(DestinationAdapter adapter) {
        if (this.mListView.getEmptyView() == null) {
            View emptyView = findViewById(R.id.empty_print_state);
            this.mListView.setEmptyView(emptyView);
        }
        TextView titleView = (TextView) findViewById(R.id.title);
        View progressBar = findViewById(R.id.progress_bar);
        if (adapter.getUnfilteredCount() <= 0) {
            titleView.setText(R.string.print_searching_for_printers);
            progressBar.setVisibility(0);
        } else {
            titleView.setText(R.string.print_no_printers);
            progressBar.setVisibility(8);
        }
    }

    private void announceSearchResultIfNeeded() {
        if (AccessibilityManager.getInstance(this).isEnabled()) {
            if (this.mAnnounceFilterResult == null) {
                this.mAnnounceFilterResult = new AnnounceFilterResult();
            }
            this.mAnnounceFilterResult.post();
        }
    }

    public static class AddPrinterAlertDialogFragment extends DialogFragment {
        private String mAddPrintServiceItem;

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Intent viewIntent;
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity()).setTitle(R.string.choose_print_service);
            final List<PrintServiceInfo> printServices = getArguments().getParcelableArrayList("FRAGMENT_ARGUMENT_PRINT_SERVICE_INFOS");
            final ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1);
            int printServiceCount = printServices.size();
            for (int i = 0; i < printServiceCount; i++) {
                PrintServiceInfo printService = printServices.get(i);
                adapter.add(printService.getResolveInfo().loadLabel(getActivity().getPackageManager()).toString());
            }
            String searchUri = Settings.Secure.getString(getActivity().getContentResolver(), "print_service_search_uri");
            if (!TextUtils.isEmpty(searchUri)) {
                Intent intent = new Intent("android.intent.action.VIEW", Uri.parse(searchUri));
                if (getActivity().getPackageManager().resolveActivity(intent, 0) != null) {
                    viewIntent = intent;
                    this.mAddPrintServiceItem = getString(R.string.add_print_service_label);
                    adapter.add(this.mAddPrintServiceItem);
                } else {
                    viewIntent = null;
                }
            } else {
                viewIntent = null;
            }
            builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String item = (String) adapter.getItem(which);
                    if (item.equals(AddPrinterAlertDialogFragment.this.mAddPrintServiceItem)) {
                        try {
                            AddPrinterAlertDialogFragment.this.startActivity(viewIntent);
                            return;
                        } catch (ActivityNotFoundException anfe) {
                            Log.w("SelectPrinterFragment", "Couldn't start add printer activity", anfe);
                            return;
                        }
                    }
                    PrintServiceInfo printService2 = (PrintServiceInfo) printServices.get(which);
                    ComponentName componentName = new ComponentName(printService2.getResolveInfo().serviceInfo.packageName, printService2.getAddPrintersActivityName());
                    Intent intent2 = new Intent("android.intent.action.MAIN");
                    intent2.setComponent(componentName);
                    try {
                        AddPrinterAlertDialogFragment.this.startActivity(intent2);
                    } catch (ActivityNotFoundException anfe2) {
                        Log.w("SelectPrinterFragment", "Couldn't start add printer activity", anfe2);
                    }
                }
            });
            return builder.create();
        }
    }

    private final class DestinationAdapter extends BaseAdapter implements Filterable {
        private CharSequence mLastSearchString;
        private final Object mLock = new Object();
        private final List<PrinterInfo> mPrinters = new ArrayList();
        private final List<PrinterInfo> mFilteredPrinters = new ArrayList();

        public DestinationAdapter() {
            SelectPrinterActivity.this.mPrinterRegistry.setOnPrintersChangeListener(new PrinterRegistry.OnPrintersChangeListener() {
                @Override
                public void onPrintersChanged(List<PrinterInfo> printers) {
                    synchronized (DestinationAdapter.this.mLock) {
                        DestinationAdapter.this.mPrinters.clear();
                        DestinationAdapter.this.mPrinters.addAll(printers);
                        DestinationAdapter.this.mFilteredPrinters.clear();
                        DestinationAdapter.this.mFilteredPrinters.addAll(printers);
                        if (!TextUtils.isEmpty(DestinationAdapter.this.mLastSearchString)) {
                            DestinationAdapter.this.getFilter().filter(DestinationAdapter.this.mLastSearchString);
                        }
                    }
                    DestinationAdapter.this.notifyDataSetChanged();
                }

                @Override
                public void onPrintersInvalid() {
                    synchronized (DestinationAdapter.this.mLock) {
                        DestinationAdapter.this.mPrinters.clear();
                        DestinationAdapter.this.mFilteredPrinters.clear();
                    }
                    DestinationAdapter.this.notifyDataSetInvalidated();
                }
            });
        }

        @Override
        public Filter getFilter() {
            return new Filter() {
                @Override
                protected Filter.FilterResults performFiltering(CharSequence constraint) {
                    Filter.FilterResults results;
                    synchronized (DestinationAdapter.this.mLock) {
                        if (TextUtils.isEmpty(constraint)) {
                            results = null;
                        } else {
                            results = new Filter.FilterResults();
                            List<PrinterInfo> filteredPrinters = new ArrayList<>();
                            String constraintLowerCase = constraint.toString().toLowerCase();
                            int printerCount = DestinationAdapter.this.mPrinters.size();
                            for (int i = 0; i < printerCount; i++) {
                                PrinterInfo printer = (PrinterInfo) DestinationAdapter.this.mPrinters.get(i);
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
                    boolean resultCountChanged;
                    synchronized (DestinationAdapter.this.mLock) {
                        int oldPrinterCount = DestinationAdapter.this.mFilteredPrinters.size();
                        DestinationAdapter.this.mLastSearchString = constraint;
                        DestinationAdapter.this.mFilteredPrinters.clear();
                        if (results == null) {
                            DestinationAdapter.this.mFilteredPrinters.addAll(DestinationAdapter.this.mPrinters);
                        } else {
                            List<PrinterInfo> printers = (List) results.values;
                            DestinationAdapter.this.mFilteredPrinters.addAll(printers);
                        }
                        resultCountChanged = oldPrinterCount != DestinationAdapter.this.mFilteredPrinters.size();
                    }
                    if (resultCountChanged) {
                        SelectPrinterActivity.this.announceSearchResultIfNeeded();
                    }
                    DestinationAdapter.this.notifyDataSetChanged();
                }
            };
        }

        public int getUnfilteredCount() {
            int size;
            synchronized (this.mLock) {
                size = this.mPrinters.size();
            }
            return size;
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
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            return getView(position, convertView, parent);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = SelectPrinterActivity.this.getLayoutInflater().inflate(R.layout.printer_list_item, parent, false);
            }
            convertView.setEnabled(isActionable(position));
            PrinterInfo printer = (PrinterInfo) getItem(position);
            CharSequence title = printer.getName();
            CharSequence subtitle = null;
            Drawable icon = null;
            try {
                PackageManager pm = SelectPrinterActivity.this.getPackageManager();
                PackageInfo packageInfo = pm.getPackageInfo(printer.getId().getServiceName().getPackageName(), 0);
                subtitle = packageInfo.applicationInfo.loadLabel(pm);
                icon = packageInfo.applicationInfo.loadIcon(pm);
            } catch (PackageManager.NameNotFoundException e) {
            }
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
            ImageView iconView = (ImageView) convertView.findViewById(R.id.icon);
            if (icon != null) {
                iconView.setImageDrawable(icon);
                iconView.setVisibility(0);
            } else {
                iconView.setVisibility(8);
            }
            return convertView;
        }

        public boolean isActionable(int position) {
            PrinterInfo printer = (PrinterInfo) getItem(position);
            return printer.getStatus() != 3;
        }
    }

    private final class AnnounceFilterResult implements Runnable {
        private AnnounceFilterResult() {
        }

        public void post() {
            remove();
            SelectPrinterActivity.this.mListView.postDelayed(this, 1000L);
        }

        public void remove() {
            SelectPrinterActivity.this.mListView.removeCallbacks(this);
        }

        @Override
        public void run() {
            String text;
            int count = SelectPrinterActivity.this.mListView.getAdapter().getCount();
            if (count <= 0) {
                text = SelectPrinterActivity.this.getString(R.string.print_no_printers);
            } else {
                text = SelectPrinterActivity.this.getResources().getQuantityString(R.plurals.print_search_result_count_utterance, count, Integer.valueOf(count));
            }
            SelectPrinterActivity.this.mListView.announceForAccessibility(text);
        }
    }
}
