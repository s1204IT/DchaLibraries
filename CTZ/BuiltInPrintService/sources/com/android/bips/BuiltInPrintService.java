package com.android.bips;

import android.content.BroadcastReceiver;
import android.net.nsd.NsdManager;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.printservice.PrintJob;
import android.printservice.PrintService;
import android.printservice.PrinterDiscoverySession;
import android.text.TextUtils;
import com.android.bips.discovery.DelayedDiscovery;
import com.android.bips.discovery.DiscoveredPrinter;
import com.android.bips.discovery.Discovery;
import com.android.bips.discovery.ManualDiscovery;
import com.android.bips.discovery.MdnsDiscovery;
import com.android.bips.discovery.MultiDiscovery;
import com.android.bips.discovery.NsdResolveQueue;
import com.android.bips.discovery.P2pDiscovery;
import com.android.bips.ipp.Backend;
import com.android.bips.ipp.CapabilitiesCache;
import com.android.bips.p2p.P2pMonitor;
import com.android.bips.p2p.P2pUtils;
import com.android.bips.util.BroadcastMonitor;
import java.lang.ref.WeakReference;

public class BuiltInPrintService extends PrintService {
    private static final String TAG = BuiltInPrintService.class.getSimpleName();
    private static WeakReference<BuiltInPrintService> sInstance;
    private MultiDiscovery mAllDiscovery;
    private Backend mBackend;
    private CapabilitiesCache mCapabilitiesCache;
    private JobQueue mJobQueue;
    private Handler mMainHandler;
    private ManualDiscovery mManualDiscovery;
    private Discovery mMdnsDiscovery;
    private NsdResolveQueue mNsdResolveQueue;
    private P2pDiscovery mP2pDiscovery;
    private P2pMonitor mP2pMonitor;
    private WifiManager.WifiLock mWifiLock;

    public static BuiltInPrintService getInstance() {
        if (sInstance == null) {
            return null;
        }
        return sInstance.get();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sInstance = new WeakReference<>(this);
        this.mBackend = new Backend(this);
        this.mCapabilitiesCache = new CapabilitiesCache(this, this.mBackend, 3);
        this.mP2pMonitor = new P2pMonitor(this);
        this.mNsdResolveQueue = new NsdResolveQueue(this, (NsdManager) getSystemService("servicediscovery"));
        this.mMdnsDiscovery = new MultiDiscovery(new MdnsDiscovery(this, "ipp"), new DelayedDiscovery(new MdnsDiscovery(this, "ipps"), 0, 150));
        this.mP2pDiscovery = new P2pDiscovery(this);
        this.mManualDiscovery = new ManualDiscovery(this);
        this.mAllDiscovery = new MultiDiscovery(this.mMdnsDiscovery, this.mManualDiscovery, new DelayedDiscovery(this.mP2pDiscovery, 1000, 0));
        this.mJobQueue = new JobQueue();
        this.mMainHandler = new Handler(getMainLooper());
        this.mWifiLock = ((WifiManager) getSystemService("wifi")).createWifiLock(1, TAG);
    }

    @Override
    public void onDestroy() {
        this.mCapabilitiesCache.close();
        this.mP2pMonitor.stopAll();
        this.mBackend.close();
        unlockWifi();
        sInstance = null;
        this.mMainHandler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    @Override
    protected PrinterDiscoverySession onCreatePrinterDiscoverySession() {
        return new LocalDiscoverySession(this);
    }

    @Override
    protected void onPrintJobQueued(PrintJob printJob) {
        this.mJobQueue.print(new LocalPrintJob(this, this.mBackend, printJob));
    }

    @Override
    protected void onRequestCancelPrintJob(PrintJob printJob) {
        this.mJobQueue.cancel(printJob.getId());
    }

    public Discovery getDiscovery() {
        return this.mAllDiscovery;
    }

    public Discovery getMdnsDiscovery() {
        return this.mMdnsDiscovery;
    }

    public ManualDiscovery getManualDiscovery() {
        return this.mManualDiscovery;
    }

    public P2pDiscovery getP2pDiscovery() {
        return this.mP2pDiscovery;
    }

    public P2pMonitor getP2pMonitor() {
        return this.mP2pMonitor;
    }

    public NsdResolveQueue getNsdResolveQueue() {
        return this.mNsdResolveQueue;
    }

    public BroadcastMonitor receiveBroadcasts(BroadcastReceiver broadcastReceiver, String... strArr) {
        return new BroadcastMonitor(this, broadcastReceiver, strArr);
    }

    public CapabilitiesCache getCapabilitiesCache() {
        return this.mCapabilitiesCache;
    }

    public Handler getMainHandler() {
        return this.mMainHandler;
    }

    public DelayedAction delay(int i, final Runnable runnable) {
        this.mMainHandler.postDelayed(runnable, i);
        return new DelayedAction() {
            @Override
            public final void cancel() {
                this.f$0.mMainHandler.removeCallbacks(runnable);
            }
        };
    }

    public String getDescription(DiscoveredPrinter discoveredPrinter) {
        if (P2pUtils.isP2p(discoveredPrinter) || P2pUtils.isOnConnectedInterface(this, discoveredPrinter)) {
            return getString(R.string.wifi_direct);
        }
        String host = discoveredPrinter.getHost();
        if (!TextUtils.isEmpty(discoveredPrinter.location)) {
            return getString(R.string.printer_description, host, discoveredPrinter.location);
        }
        return host;
    }

    public void lockWifi() {
        if (!this.mWifiLock.isHeld()) {
            this.mWifiLock.acquire();
        }
    }

    public void unlockWifi() {
        if (this.mWifiLock.isHeld()) {
            this.mWifiLock.release();
        }
    }
}
