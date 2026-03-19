package com.android.bips;

import android.print.PrintManager;
import android.print.PrinterId;
import android.print.PrinterInfo;
import android.printservice.PrintServiceInfo;
import android.printservice.PrinterDiscoverySession;
import android.printservice.recommendation.RecommendationInfo;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Log;
import com.android.bips.discovery.DiscoveredPrinter;
import com.android.bips.discovery.Discovery;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

class LocalDiscoverySession extends PrinterDiscoverySession implements PrintManager.PrintServiceRecommendationsChangeListener, PrintManager.PrintServicesChangeListener, Discovery.Listener {
    private static final String TAG = LocalDiscoverySession.class.getSimpleName();
    private DelayedAction mExpirePrinters;
    private final LocalDiscoverySessionInfo mInfo;
    private PrintManager mPrintManager;
    private final BuiltInPrintService mPrintService;
    private final Map<PrinterId, LocalPrinter> mPrinters = new HashMap();
    private final Set<PrinterId> mTrackingIds = new HashSet();
    private ArraySet<String> mEnabledServices = new ArraySet<>();
    private final ArrayMap<InetAddress, ArrayList<String>> mPrintersOfOtherService = new ArrayMap<>();

    LocalDiscoverySession(BuiltInPrintService builtInPrintService) {
        this.mPrintService = builtInPrintService;
        this.mPrintManager = (PrintManager) this.mPrintService.getSystemService(PrintManager.class);
        this.mInfo = new LocalDiscoverySessionInfo(builtInPrintService);
    }

    @Override
    public void onStartPrinterDiscovery(List<PrinterId> list) {
        Iterator<LocalPrinter> it = this.mPrinters.values().iterator();
        while (it.hasNext()) {
            it.next().notFound();
        }
        monitorExpiredPrinters();
        this.mPrintService.getDiscovery().start(this);
        this.mPrintManager.addPrintServicesChangeListener(this, null);
        onPrintServicesChanged();
        this.mPrintManager.addPrintServiceRecommendationsChangeListener(this, null);
        onPrintServiceRecommendationsChanged();
    }

    @Override
    public void onStopPrinterDiscovery() {
        this.mPrintService.getDiscovery().stop(this);
        PrintManager printManager = (PrintManager) this.mPrintService.getSystemService(PrintManager.class);
        printManager.removePrintServicesChangeListener(this);
        printManager.removePrintServiceRecommendationsChangeListener(this);
        if (this.mExpirePrinters != null) {
            this.mExpirePrinters.cancel();
            this.mExpirePrinters = null;
        }
    }

    @Override
    public void onValidatePrinters(List<PrinterId> list) {
    }

    @Override
    public void onStartPrinterStateTracking(PrinterId printerId) {
        LocalPrinter localPrinter = this.mPrinters.get(printerId);
        this.mTrackingIds.add(printerId);
        if (localPrinter == null || !localPrinter.isFound()) {
            return;
        }
        localPrinter.track();
    }

    @Override
    public void onStopPrinterStateTracking(PrinterId printerId) {
        LocalPrinter localPrinter = this.mPrinters.get(printerId);
        if (localPrinter != null) {
            localPrinter.stopTracking();
        }
        this.mTrackingIds.remove(printerId);
    }

    @Override
    public void onDestroy() {
        this.mInfo.save();
    }

    @Override
    public void onPrinterFound(final DiscoveredPrinter discoveredPrinter) {
        if (isDestroyed()) {
            Log.w(TAG, "Destroyed; ignoring");
            return;
        }
        PrinterId id = discoveredPrinter.getId(this.mPrintService);
        LocalPrinter localPrinterComputeIfAbsent = this.mPrinters.computeIfAbsent(id, new Function() {
            @Override
            public final Object apply(Object obj) {
                return LocalDiscoverySession.lambda$onPrinterFound$0(this.f$0, discoveredPrinter, (PrinterId) obj);
            }
        });
        localPrinterComputeIfAbsent.found(discoveredPrinter);
        if (this.mTrackingIds.contains(id)) {
            localPrinterComputeIfAbsent.track();
        }
    }

    public static LocalPrinter lambda$onPrinterFound$0(LocalDiscoverySession localDiscoverySession, DiscoveredPrinter discoveredPrinter, PrinterId printerId) {
        return new LocalPrinter(localDiscoverySession.mPrintService, localDiscoverySession, discoveredPrinter);
    }

    @Override
    public void onPrinterLost(DiscoveredPrinter discoveredPrinter) {
        this.mPrintService.getCapabilitiesCache().remove(discoveredPrinter.path);
        LocalPrinter localPrinter = this.mPrinters.get(discoveredPrinter.getId(this.mPrintService));
        if (localPrinter == null) {
            return;
        }
        localPrinter.notFound();
        handlePrinter(localPrinter);
        monitorExpiredPrinters();
    }

    private void monitorExpiredPrinters() {
        if (this.mExpirePrinters == null && !this.mPrinters.isEmpty()) {
            this.mExpirePrinters = this.mPrintService.delay(3000, new Runnable() {
                @Override
                public final void run() {
                    LocalDiscoverySession.lambda$monitorExpiredPrinters$1(this.f$0);
                }
            });
        }
    }

    public static void lambda$monitorExpiredPrinters$1(LocalDiscoverySession localDiscoverySession) {
        localDiscoverySession.mExpirePrinters = null;
        ArrayList arrayList = new ArrayList();
        boolean z = true;
        for (LocalPrinter localPrinter : localDiscoverySession.mPrinters.values()) {
            if (localPrinter.isExpired()) {
                arrayList.add(localPrinter.getPrinterId());
            }
            if (!localPrinter.isFound()) {
                z = false;
            }
        }
        Iterator<PrinterId> it = arrayList.iterator();
        while (it.hasNext()) {
            localDiscoverySession.mPrinters.remove(it.next());
        }
        localDiscoverySession.removePrinters(arrayList);
        if (!z) {
            localDiscoverySession.monitorExpiredPrinters();
        }
    }

    void handlePrinter(LocalPrinter localPrinter) {
        PrinterInfo printerInfoCreatePrinterInfo = localPrinter.createPrinterInfo(this.mInfo.isKnownGood(localPrinter.getPrinterId()));
        if (printerInfoCreatePrinterInfo == null) {
            return;
        }
        if (printerInfoCreatePrinterInfo.getStatus() == 1 && localPrinter.getUuid() != null) {
            this.mInfo.setKnownGood(localPrinter.getPrinterId());
        }
        Iterator<PrinterInfo> it = getPrinters().iterator();
        while (it.hasNext()) {
            if (it.next().getId().equals(printerInfoCreatePrinterInfo.getId()) && printerInfoCreatePrinterInfo.getCapabilities() == null) {
                return;
            }
        }
        if (!isHandledByOtherService(localPrinter)) {
            addPrinters(Collections.singletonList(printerInfoCreatePrinterInfo));
        }
    }

    boolean isPriority(PrinterId printerId) {
        return this.mTrackingIds.contains(printerId);
    }

    boolean isKnown(PrinterId printerId) {
        return this.mPrinters.containsKey(printerId);
    }

    private boolean isHandledByOtherService(LocalPrinter localPrinter) {
        ArrayList<String> arrayList;
        if (localPrinter.getAddress() != null && (arrayList = this.mPrintersOfOtherService.get(localPrinter.getAddress())) != null) {
            int size = arrayList.size();
            for (int i = 0; i < size; i++) {
                if (this.mEnabledServices.contains(arrayList.get(i))) {
                    return true;
                }
            }
        }
        return false;
    }

    private void onPrintServicesStateUpdated() {
        ArrayList arrayList = new ArrayList();
        ArrayList arrayList2 = new ArrayList();
        for (LocalPrinter localPrinter : this.mPrinters.values()) {
            PrinterInfo printerInfoCreatePrinterInfo = localPrinter.createPrinterInfo(this.mInfo.isKnownGood(localPrinter.getPrinterId()));
            if (localPrinter.getCapabilities() != null && localPrinter.isFound() && !isHandledByOtherService(localPrinter) && printerInfoCreatePrinterInfo != null) {
                arrayList.add(printerInfoCreatePrinterInfo);
            } else {
                arrayList2.add(localPrinter.getPrinterId());
            }
        }
        removePrinters(arrayList2);
        addPrinters(arrayList);
    }

    public void onPrintServiceRecommendationsChanged() {
        this.mPrintersOfOtherService.clear();
        List printServiceRecommendations = this.mPrintManager.getPrintServiceRecommendations();
        int size = printServiceRecommendations.size();
        for (int i = 0; i < size; i++) {
            RecommendationInfo recommendationInfo = (RecommendationInfo) printServiceRecommendations.get(i);
            String string = recommendationInfo.getPackageName().toString();
            if (!string.equals(this.mPrintService.getPackageName())) {
                for (InetAddress inetAddress : recommendationInfo.getDiscoveredPrinters()) {
                    ArrayList<String> arrayList = this.mPrintersOfOtherService.get(inetAddress);
                    if (arrayList == null) {
                        arrayList = new ArrayList<>(1);
                        this.mPrintersOfOtherService.put(inetAddress, arrayList);
                    }
                    arrayList.add(string);
                }
            }
        }
        onPrintServicesStateUpdated();
    }

    public void onPrintServicesChanged() {
        this.mEnabledServices.clear();
        List printServices = this.mPrintManager.getPrintServices(1);
        int size = printServices.size();
        for (int i = 0; i < size; i++) {
            String packageName = ((PrintServiceInfo) printServices.get(i)).getComponentName().getPackageName();
            if (!packageName.equals(this.mPrintService.getPackageName())) {
                this.mEnabledServices.add(packageName);
            }
        }
        onPrintServicesStateUpdated();
    }
}
