package com.android.bips.ui;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.net.wifi.p2p.WifiP2pDevice;
import android.os.Bundle;
import android.view.ViewGroup;
import com.android.bips.BuiltInPrintService;
import com.android.bips.R;
import com.android.bips.discovery.ConnectionListener;
import com.android.bips.discovery.DiscoveredPrinter;
import com.android.bips.p2p.P2pPrinterConnection;

public class AddP2pPrinterDialog extends AlertDialog implements ConnectionListener {
    private final Fragment mFragment;
    private final WifiP2pDevice mPeer;
    private final BuiltInPrintService mPrintService;
    private P2pPrinterConnection mValidating;

    AddP2pPrinterDialog(Fragment fragment, BuiltInPrintService builtInPrintService, WifiP2pDevice wifiP2pDevice) {
        super(fragment.getContext());
        this.mFragment = fragment;
        this.mPrintService = builtInPrintService;
        this.mPeer = wifiP2pDevice;
    }

    @Override
    @SuppressLint({"InflateParams"})
    protected void onCreate(Bundle bundle) {
        setView(getLayoutInflater().inflate(R.layout.manual_printer_add, (ViewGroup) null));
        setTitle(getContext().getString(R.string.connecting_to, this.mPeer.deviceName));
        setButton(-2, getContext().getString(android.R.string.cancel), (DialogInterface.OnClickListener) null);
        super.onCreate(bundle);
        findViewById(R.id.labelHostname).setVisibility(8);
        findViewById(R.id.hostname).setVisibility(8);
        findViewById(R.id.progress).setVisibility(0);
        setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public final void onDismiss(DialogInterface dialogInterface) {
                this.f$0.mValidating.close();
            }
        });
        this.mValidating = new P2pPrinterConnection(this.mPrintService, this.mPeer, this);
    }

    @Override
    public void onConnectionComplete(final DiscoveredPrinter discoveredPrinter) {
        if (discoveredPrinter != null) {
            this.mPrintService.getMainHandler().post(new Runnable() {
                @Override
                public final void run() {
                    AddP2pPrinterDialog.lambda$onConnectionComplete$1(this.f$0, discoveredPrinter);
                }
            });
            dismiss();
        } else {
            fail();
        }
    }

    public static void lambda$onConnectionComplete$1(AddP2pPrinterDialog addP2pPrinterDialog, DiscoveredPrinter discoveredPrinter) {
        addP2pPrinterDialog.mValidating.close();
        addP2pPrinterDialog.mPrintService.getP2pDiscovery().addValidPrinter(discoveredPrinter);
        addP2pPrinterDialog.mFragment.getActivity().finish();
    }

    @Override
    public void onConnectionDelayed(boolean z) {
        findViewById(R.id.connect_hint).setVisibility(z ? 0 : 8);
    }

    private void fail() {
        cancel();
        new AlertDialog.Builder(getContext()).setTitle(getContext().getString(R.string.failed_connection, this.mPeer.deviceName)).setPositiveButton(android.R.string.ok, (DialogInterface.OnClickListener) null).show();
    }
}
