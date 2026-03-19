package com.android.bips.ui;

import android.content.Context;
import android.preference.Preference;
import com.android.bips.BuiltInPrintService;
import com.android.bips.R;
import com.android.bips.discovery.DiscoveredPrinter;

class PrinterPreference extends Preference {
    private boolean mAdding;
    private final BuiltInPrintService mPrintService;
    private DiscoveredPrinter mPrinter;

    PrinterPreference(Context context, BuiltInPrintService builtInPrintService, DiscoveredPrinter discoveredPrinter, boolean z) {
        super(context);
        this.mAdding = false;
        this.mPrintService = builtInPrintService;
        this.mPrinter = discoveredPrinter;
        this.mAdding = z;
        updatePrinter(discoveredPrinter);
    }

    void updatePrinter(DiscoveredPrinter discoveredPrinter) {
        this.mPrinter = discoveredPrinter;
        setLayoutResource(R.layout.printer_item);
        if (this.mAdding) {
            setTitle(getContext().getString(R.string.add_named, discoveredPrinter.name));
        } else {
            setTitle(discoveredPrinter.name);
        }
        setSummary(this.mPrintService.getDescription(discoveredPrinter));
        setIcon(R.drawable.ic_printer);
    }

    DiscoveredPrinter getPrinter() {
        return this.mPrinter;
    }
}
