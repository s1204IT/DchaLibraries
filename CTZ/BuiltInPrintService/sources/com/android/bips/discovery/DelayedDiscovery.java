package com.android.bips.discovery;

import android.net.Uri;
import com.android.bips.DelayedAction;
import com.android.bips.discovery.DelayedDiscovery;
import com.android.bips.discovery.Discovery;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class DelayedDiscovery extends Discovery {
    private static final String TAG = DelayedDiscovery.class.getSimpleName();
    private final Discovery mChild;
    private final Discovery.Listener mChildListener;
    private DelayedAction mDelayedStart;
    private final Map<Uri, DiscoveredPrinter> mPending;
    private final int mStartDelay;

    public DelayedDiscovery(Discovery discovery, int i, int i2) {
        super(discovery.getPrintService());
        this.mPending = new HashMap();
        this.mChild = discovery;
        this.mStartDelay = i;
        this.mChildListener = new AnonymousClass1(i2);
    }

    class AnonymousClass1 implements Discovery.Listener {
        final int val$findDelay;

        AnonymousClass1(int i) {
            this.val$findDelay = i;
        }

        @Override
        public void onPrinterFound(final DiscoveredPrinter discoveredPrinter) {
            if (this.val$findDelay <= 0) {
                DelayedDiscovery.this.printerFound(discoveredPrinter);
            } else if (((DiscoveredPrinter) DelayedDiscovery.this.mPending.put(discoveredPrinter.getUri(), discoveredPrinter)) == null) {
                DelayedDiscovery.this.getHandler().postDelayed(new Runnable() {
                    @Override
                    public final void run() {
                        DelayedDiscovery.AnonymousClass1.lambda$onPrinterFound$0(this.f$0, discoveredPrinter);
                    }
                }, this.val$findDelay);
            }
        }

        public static void lambda$onPrinterFound$0(AnonymousClass1 anonymousClass1, DiscoveredPrinter discoveredPrinter) {
            DiscoveredPrinter discoveredPrinter2 = (DiscoveredPrinter) DelayedDiscovery.this.mPending.remove(discoveredPrinter.getUri());
            if (discoveredPrinter2 != null) {
                DelayedDiscovery.this.printerFound(discoveredPrinter2);
            }
        }

        @Override
        public void onPrinterLost(DiscoveredPrinter discoveredPrinter) {
            DelayedDiscovery.this.mPending.remove(discoveredPrinter.getUri());
            DelayedDiscovery.this.printerLost(discoveredPrinter.getUri());
        }
    }

    @Override
    void onStart() {
        if (this.mStartDelay == 0) {
            this.mChild.start(this.mChildListener);
        } else {
            this.mDelayedStart = getPrintService().delay(this.mStartDelay, new Runnable() {
                @Override
                public final void run() {
                    DelayedDiscovery.lambda$onStart$0(this.f$0);
                }
            });
        }
    }

    public static void lambda$onStart$0(DelayedDiscovery delayedDiscovery) {
        if (!delayedDiscovery.isStarted()) {
            return;
        }
        delayedDiscovery.mChild.start(delayedDiscovery.mChildListener);
    }

    @Override
    void onStop() {
        if (this.mDelayedStart != null) {
            this.mDelayedStart.cancel();
        }
        this.mChild.stop(this.mChildListener);
        this.mPending.clear();
    }

    @Override
    Collection<Discovery> getChildren() {
        return Collections.singleton(this.mChild);
    }
}
