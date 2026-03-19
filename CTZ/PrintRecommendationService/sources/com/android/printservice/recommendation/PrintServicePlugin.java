package com.android.printservice.recommendation;

import java.net.InetAddress;
import java.util.List;

public interface PrintServicePlugin {

    public interface PrinterDiscoveryCallback {
        void onChanged(List<InetAddress> list);
    }

    int getName();

    CharSequence getPackageName();

    void start(PrinterDiscoveryCallback printerDiscoveryCallback) throws Exception;

    void stop() throws Exception;
}
