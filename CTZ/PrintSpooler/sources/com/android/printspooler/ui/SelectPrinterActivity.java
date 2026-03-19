package com.android.printspooler.ui;

import android.R;
import android.app.Activity;
import android.app.LoaderManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentSender;
import android.content.Loader;
import android.database.DataSetObserver;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.print.PrintManager;
import android.print.PrintServicesLoader;
import android.print.PrinterId;
import android.print.PrinterInfo;
import android.printservice.PrintServiceInfo;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.Menu;
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
import android.widget.TextView;
import android.widget.Toast;
import com.android.internal.logging.MetricsLogger;
import com.android.printspooler.ui.PrinterRegistry;
import com.android.printspooler.ui.SelectPrinterActivity;
import java.util.ArrayList;
import java.util.List;

public final class SelectPrinterActivity extends Activity implements LoaderManager.LoaderCallbacks<List<PrintServiceInfo>> {
    private AnnounceFilterResult mAnnounceFilterResult;
    private boolean mDidSearch;
    private ArrayMap<ComponentName, PrintServiceInfo> mEnabledPrintServices;
    private ListView mListView;
    private PrinterInfo mPrinterForInfoIntent;
    private PrinterRegistry mPrinterRegistry;

    private void startAddPrinterActivity() {
        MetricsLogger.action(this, 510);
        startActivity(new Intent(this, (Class<?>) AddPrinterActivity.class));
    }

    @Override
    public void onCreate(Bundle bundle) {
        boolean z;
        super.onCreate(bundle);
        getActionBar().setIcon(R.drawable.ic_media_route_connected_light_03_mtrl);
        setContentView(com.android.printspooler.R.layout.select_printer_activity);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        this.mEnabledPrintServices = new ArrayMap<>();
        this.mPrinterRegistry = new PrinterRegistry(this, null, 1, 2);
        this.mListView = (ListView) findViewById(R.id.list);
        final DestinationAdapter destinationAdapter = new DestinationAdapter();
        destinationAdapter.registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                if (!SelectPrinterActivity.this.isFinishing() && destinationAdapter.getCount() <= 0) {
                    SelectPrinterActivity.this.updateEmptyView(destinationAdapter);
                }
            }

            @Override
            public void onInvalidated() {
                if (!SelectPrinterActivity.this.isFinishing()) {
                    SelectPrinterActivity.this.updateEmptyView(destinationAdapter);
                }
            }
        });
        this.mListView.setAdapter((ListAdapter) destinationAdapter);
        this.mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long j) {
                if (((DestinationAdapter) SelectPrinterActivity.this.mListView.getAdapter()).isActionable(i)) {
                    PrinterInfo printerInfo = (PrinterInfo) SelectPrinterActivity.this.mListView.getAdapter().getItem(i);
                    if (printerInfo == null) {
                        SelectPrinterActivity.this.startAddPrinterActivity();
                    } else {
                        SelectPrinterActivity.this.onPrinterSelected(printerInfo);
                    }
                }
            }
        });
        findViewById(com.android.printspooler.R.id.button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SelectPrinterActivity.this.startAddPrinterActivity();
            }
        });
        registerForContextMenu(this.mListView);
        getLoaderManager().initLoader(3, null, this);
        if (bundle == null || !bundle.getBoolean("KEY_NOT_FIRST_CREATE")) {
            List printServices = ((PrintManager) getSystemService("print")).getPrintServices(3);
            int i = 0;
            if (printServices == null) {
                z = false;
            } else {
                int size = printServices.size();
                int i2 = 0;
                z = false;
                while (i < size) {
                    if (((PrintServiceInfo) printServices.get(i)).isEnabled()) {
                        i2 = 1;
                    } else {
                        z = true;
                    }
                    i++;
                }
                i = i2;
            }
            if (i == 0) {
                startAddPrinterActivity();
            } else if (z && !TextUtils.isEmpty(Settings.Secure.getString(getContentResolver(), "disabled_print_services"))) {
                Toast.makeText(this, getString(com.android.printspooler.R.string.print_services_disabled_toast), 1).show();
            }
        }
        if (bundle != null) {
            this.mDidSearch = bundle.getBoolean("DID_SEARCH");
            this.mPrinterForInfoIntent = (PrinterInfo) bundle.getParcelable("KEY_PRINTER_FOR_INFO_INTENT");
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        bundle.putBoolean("KEY_NOT_FIRST_CREATE", true);
        bundle.putBoolean("DID_SEARCH", this.mDidSearch);
        bundle.putParcelable("KEY_PRINTER_FOR_INFO_INTENT", this.mPrinterForInfoIntent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(com.android.printspooler.R.menu.select_printer_activity, menu);
        SearchView searchView = (SearchView) menu.findItem(com.android.printspooler.R.id.action_search).getActionView();
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String str) {
                return true;
            }

            @Override
            public boolean onQueryTextChange(String str) {
                ((DestinationAdapter) SelectPrinterActivity.this.mListView.getAdapter()).getFilter().filter(str);
                return true;
            }
        });
        searchView.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(View view) {
                if (AccessibilityManager.getInstance(SelectPrinterActivity.this).isEnabled()) {
                    view.announceForAccessibility(SelectPrinterActivity.this.getString(com.android.printspooler.R.string.print_search_box_shown_utterance));
                }
            }

            @Override
            public void onViewDetachedFromWindow(View view) {
                if (!SelectPrinterActivity.this.isFinishing() && AccessibilityManager.getInstance(SelectPrinterActivity.this).isEnabled()) {
                    view.announceForAccessibility(SelectPrinterActivity.this.getString(com.android.printspooler.R.string.print_search_box_hidden_utterance));
                }
            }
        });
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() == 16908332) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(menuItem);
    }

    @Override
    public void onCreateContextMenu(ContextMenu contextMenu, View view, ContextMenu.ContextMenuInfo contextMenuInfo) {
        if (view == this.mListView) {
            PrinterInfo printerInfo = (PrinterInfo) this.mListView.getAdapter().getItem(((AdapterView.AdapterContextMenuInfo) contextMenuInfo).position);
            if (printerInfo == null) {
                return;
            }
            contextMenu.setHeaderTitle(printerInfo.getName());
            if (printerInfo.getStatus() != 3) {
                MenuItem menuItemAdd = contextMenu.add(0, com.android.printspooler.R.string.print_select_printer, 0, com.android.printspooler.R.string.print_select_printer);
                Intent intent = new Intent();
                intent.putExtra("EXTRA_PRINTER", printerInfo);
                menuItemAdd.setIntent(intent);
            }
            if (this.mPrinterRegistry.isFavoritePrinter(printerInfo.getId())) {
                MenuItem menuItemAdd2 = contextMenu.add(0, com.android.printspooler.R.string.print_forget_printer, 0, com.android.printspooler.R.string.print_forget_printer);
                Intent intent2 = new Intent();
                intent2.putExtra("EXTRA_PRINTER_ID", printerInfo.getId());
                menuItemAdd2.setIntent(intent2);
            }
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem menuItem) {
        int itemId = menuItem.getItemId();
        if (itemId == com.android.printspooler.R.string.print_forget_printer) {
            this.mPrinterRegistry.forgetFavoritePrinter((PrinterId) menuItem.getIntent().getParcelableExtra("EXTRA_PRINTER_ID"));
            return true;
        }
        if (itemId == com.android.printspooler.R.string.print_select_printer) {
            onPrinterSelected((PrinterInfo) menuItem.getIntent().getParcelableExtra("EXTRA_PRINTER"));
            return true;
        }
        return false;
    }

    private synchronized void onPrintServicesUpdate() {
        updateEmptyView((DestinationAdapter) this.mListView.getAdapter());
        invalidateOptionsMenu();
    }

    @Override
    public void onStart() {
        super.onStart();
        onPrintServicesUpdate();
    }

    @Override
    public void onPause() {
        if (this.mAnnounceFilterResult != null) {
            this.mAnnounceFilterResult.remove();
        }
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        if (isFinishing()) {
            DestinationAdapter destinationAdapter = (DestinationAdapter) this.mListView.getAdapter();
            List<PrinterInfo> printers = destinationAdapter.getPrinters();
            int size = destinationAdapter.getPrinters().size();
            MetricsLogger.action(this, 503, size);
            MetricsLogger.count(this, "printers_listed", size);
            int i = 0;
            int i2 = 0;
            for (int i3 = 0; i3 < size; i3++) {
                PrinterInfo printerInfo = printers.get(i3);
                if (printerInfo.getInfoIntent() != null) {
                    i++;
                }
                if (printerInfo.getHasCustomPrinterIcon()) {
                    i2++;
                }
            }
            MetricsLogger.count(this, "printers_info", i);
            MetricsLogger.count(this, "printers_icon", i2);
        }
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int i, int i2, Intent intent) {
        if (i == 1) {
            if (i2 == -1 && intent != null && intent.getBooleanExtra("android.printservice.extra.SELECT_PRINTER", false) && this.mPrinterForInfoIntent != null && this.mPrinterForInfoIntent.getStatus() != 3) {
                onPrinterSelected(this.mPrinterForInfoIntent);
            }
            this.mPrinterForInfoIntent = null;
        }
    }

    private void onPrinterSelected(PrinterInfo printerInfo) {
        Intent intent = new Intent();
        intent.putExtra("INTENT_EXTRA_PRINTER", printerInfo);
        setResult(-1, intent);
        finish();
    }

    public void updateEmptyView(DestinationAdapter destinationAdapter) {
        if (this.mListView.getEmptyView() == null) {
            this.mListView.setEmptyView(findViewById(com.android.printspooler.R.id.empty_print_state));
        }
        TextView textView = (TextView) findViewById(com.android.printspooler.R.id.title);
        View viewFindViewById = findViewById(com.android.printspooler.R.id.progress_bar);
        if (this.mEnabledPrintServices.size() == 0) {
            textView.setText(com.android.printspooler.R.string.print_no_print_services);
            viewFindViewById.setVisibility(8);
        } else if (destinationAdapter.getUnfilteredCount() <= 0) {
            textView.setText(com.android.printspooler.R.string.print_searching_for_printers);
            viewFindViewById.setVisibility(0);
        } else {
            textView.setText(com.android.printspooler.R.string.print_no_printers);
            viewFindViewById.setVisibility(8);
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

    @Override
    public Loader<List<PrintServiceInfo>> onCreateLoader(int i, Bundle bundle) {
        return new PrintServicesLoader((PrintManager) getSystemService("print"), this, 1);
    }

    @Override
    public void onLoadFinished(Loader<List<PrintServiceInfo>> loader, List<PrintServiceInfo> list) {
        this.mEnabledPrintServices.clear();
        if (list != null && !list.isEmpty()) {
            int size = list.size();
            for (int i = 0; i < size; i++) {
                PrintServiceInfo printServiceInfo = list.get(i);
                this.mEnabledPrintServices.put(printServiceInfo.getComponentName(), printServiceInfo);
            }
        }
        onPrintServicesUpdate();
    }

    @Override
    public void onLoaderReset(Loader<List<PrintServiceInfo>> loader) {
        if (!isFinishing()) {
            onLoadFinished(loader, (List<PrintServiceInfo>) null);
        }
    }

    private final class DestinationAdapter extends BaseAdapter implements Filterable {
        private CharSequence mLastSearchString;
        private final Object mLock = new Object();
        private final List<PrinterInfo> mPrinters = new ArrayList();
        private final List<PrinterInfo> mFilteredPrinters = new ArrayList();

        List<PrinterInfo> getPrinters() {
            return this.mPrinters;
        }

        public DestinationAdapter() {
            SelectPrinterActivity.this.mPrinterRegistry.setOnPrintersChangeListener(new PrinterRegistry.OnPrintersChangeListener() {
                @Override
                public void onPrintersChanged(List<PrinterInfo> list) {
                    synchronized (DestinationAdapter.this.mLock) {
                        DestinationAdapter.this.mPrinters.clear();
                        DestinationAdapter.this.mPrinters.addAll(list);
                        DestinationAdapter.this.mFilteredPrinters.clear();
                        DestinationAdapter.this.mFilteredPrinters.addAll(list);
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
                protected Filter.FilterResults performFiltering(CharSequence charSequence) {
                    synchronized (DestinationAdapter.this.mLock) {
                        if (TextUtils.isEmpty(charSequence)) {
                            return null;
                        }
                        Filter.FilterResults filterResults = new Filter.FilterResults();
                        ArrayList arrayList = new ArrayList();
                        String lowerCase = charSequence.toString().toLowerCase();
                        int size = DestinationAdapter.this.mPrinters.size();
                        for (int i = 0; i < size; i++) {
                            PrinterInfo printerInfo = (PrinterInfo) DestinationAdapter.this.mPrinters.get(i);
                            String description = printerInfo.getDescription();
                            if (printerInfo.getName().toLowerCase().contains(lowerCase) || (description != null && description.toLowerCase().contains(lowerCase))) {
                                arrayList.add(printerInfo);
                            }
                        }
                        filterResults.values = arrayList;
                        filterResults.count = arrayList.size();
                        return filterResults;
                    }
                }

                @Override
                protected void publishResults(CharSequence charSequence, Filter.FilterResults filterResults) {
                    boolean z;
                    synchronized (DestinationAdapter.this.mLock) {
                        int size = DestinationAdapter.this.mFilteredPrinters.size();
                        DestinationAdapter.this.mLastSearchString = charSequence;
                        DestinationAdapter.this.mFilteredPrinters.clear();
                        if (filterResults == null) {
                            DestinationAdapter.this.mFilteredPrinters.addAll(DestinationAdapter.this.mPrinters);
                        } else {
                            DestinationAdapter.this.mFilteredPrinters.addAll((List) filterResults.values);
                        }
                        if (size == DestinationAdapter.this.mFilteredPrinters.size()) {
                            z = false;
                        } else {
                            z = true;
                        }
                    }
                    if (z) {
                        SelectPrinterActivity.this.announceSearchResultIfNeeded();
                    }
                    if (!SelectPrinterActivity.this.mDidSearch) {
                        MetricsLogger.action(SelectPrinterActivity.this, 509);
                        SelectPrinterActivity.this.mDidSearch = true;
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
            synchronized (this.mLock) {
                if (this.mFilteredPrinters.isEmpty()) {
                    return 0;
                }
                return this.mFilteredPrinters.size() + 1;
            }
        }

        @Override
        public int getViewTypeCount() {
            return 2;
        }

        @Override
        public int getItemViewType(int i) {
            if (getItem(i) == null) {
                return 0;
            }
            return 1;
        }

        @Override
        public Object getItem(int i) {
            synchronized (this.mLock) {
                if (i < this.mFilteredPrinters.size()) {
                    return this.mFilteredPrinters.get(i);
                }
                return null;
            }
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getDropDownView(int i, View view, ViewGroup viewGroup) {
            return getView(i, view, viewGroup);
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            String string;
            final PrinterInfo printerInfo = (PrinterInfo) getItem(i);
            if (printerInfo == null) {
                if (view == null) {
                    return SelectPrinterActivity.this.getLayoutInflater().inflate(com.android.printspooler.R.layout.add_printer_list_item, viewGroup, false);
                }
                return view;
            }
            if (view == null) {
                view = SelectPrinterActivity.this.getLayoutInflater().inflate(com.android.printspooler.R.layout.printer_list_item, viewGroup, false);
            }
            view.setEnabled(isActionable(i));
            String name = printerInfo.getName();
            Drawable drawableLoadIcon = printerInfo.loadIcon(SelectPrinterActivity.this);
            PrintServiceInfo printServiceInfo = (PrintServiceInfo) SelectPrinterActivity.this.mEnabledPrintServices.get(printerInfo.getId().getServiceName());
            if (printServiceInfo != null) {
                string = printServiceInfo.getResolveInfo().loadLabel(SelectPrinterActivity.this.getPackageManager()).toString();
            } else {
                string = null;
            }
            String description = printerInfo.getDescription();
            if (!TextUtils.isEmpty(string)) {
                if (!TextUtils.isEmpty(description)) {
                    string = SelectPrinterActivity.this.getString(com.android.printspooler.R.string.printer_extended_description_template, string, description);
                }
            } else {
                string = description;
            }
            ((TextView) view.findViewById(com.android.printspooler.R.id.title)).setText(name);
            TextView textView = (TextView) view.findViewById(com.android.printspooler.R.id.subtitle);
            if (!TextUtils.isEmpty(string)) {
                textView.setText(string);
                textView.setVisibility(0);
            } else {
                textView.setText((CharSequence) null);
                textView.setVisibility(8);
            }
            LinearLayout linearLayout = (LinearLayout) view.findViewById(com.android.printspooler.R.id.more_info);
            if (printerInfo.getInfoIntent() != null) {
                linearLayout.setVisibility(0);
                linearLayout.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public final void onClick(View view2) {
                        SelectPrinterActivity.DestinationAdapter.lambda$getView$0(this.f$0, printerInfo, view2);
                    }
                });
            } else {
                linearLayout.setVisibility(8);
            }
            ImageView imageView = (ImageView) view.findViewById(com.android.printspooler.R.id.icon);
            if (drawableLoadIcon != null) {
                imageView.setVisibility(0);
                if (!isActionable(i)) {
                    drawableLoadIcon.mutate();
                    TypedValue typedValue = new TypedValue();
                    SelectPrinterActivity.this.getTheme().resolveAttribute(R.attr.disabledAlpha, typedValue, true);
                    drawableLoadIcon.setAlpha((int) (typedValue.getFloat() * 255.0f));
                }
                imageView.setImageDrawable(drawableLoadIcon);
            } else {
                imageView.setVisibility(8);
            }
            return view;
        }

        public static void lambda$getView$0(DestinationAdapter destinationAdapter, PrinterInfo printerInfo, View view) {
            Intent intent = new Intent();
            intent.putExtra("android.printservice.extra.CAN_SELECT_PRINTER", true);
            try {
                SelectPrinterActivity.this.mPrinterForInfoIntent = printerInfo;
                SelectPrinterActivity.this.startIntentSenderForResult(printerInfo.getInfoIntent().getIntentSender(), 1, intent, 0, 0, 0);
            } catch (IntentSender.SendIntentException e) {
                SelectPrinterActivity.this.mPrinterForInfoIntent = null;
                Log.e("SelectPrinterFragment", "Could not execute pending info intent: %s", e);
            }
        }

        public boolean isActionable(int i) {
            PrinterInfo printerInfo = (PrinterInfo) getItem(i);
            return printerInfo == null || printerInfo.getStatus() != 3;
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
            String quantityString;
            int count = SelectPrinterActivity.this.mListView.getAdapter().getCount();
            if (count <= 0) {
                quantityString = SelectPrinterActivity.this.getString(com.android.printspooler.R.string.print_no_printers);
            } else {
                quantityString = SelectPrinterActivity.this.getResources().getQuantityString(com.android.printspooler.R.plurals.print_search_result_count_utterance, count, Integer.valueOf(count));
            }
            SelectPrinterActivity.this.mListView.announceForAccessibility(quantityString);
        }
    }
}
