package com.android.printservice.recommendation.util;

import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.util.ArrayMap;
import android.util.Log;
import java.util.ArrayList;
import java.util.Iterator;

public class DiscoveryListenerMultiplexer {
    private static final ArrayMap<String, DiscoveryListenerSet> sListeners = new ArrayMap<>();

    public static void addListener(NsdManager nsdManager, String str, NsdManager.DiscoveryListener discoveryListener) {
        synchronized (sListeners) {
            DiscoveryListenerSet discoveryListenerSet = sListeners.get(str);
            if (discoveryListenerSet == null) {
                ArrayList arrayList = new ArrayList(1);
                DiscoveryListenerSet discoveryListenerSet2 = new DiscoveryListenerSet(arrayList, new MultiListener(arrayList));
                sListeners.put(str, discoveryListenerSet2);
                discoveryListenerSet = discoveryListenerSet2;
            }
            synchronized (discoveryListenerSet.subListeners) {
                if (discoveryListenerSet.subListeners.isEmpty()) {
                    nsdManager.discoverServices(str, 1, discoveryListenerSet.mainListener);
                }
                discoveryListenerSet.subListeners.add(discoveryListener);
            }
        }
    }

    public static boolean removeListener(NsdManager nsdManager, NsdManager.DiscoveryListener discoveryListener) {
        boolean z;
        boolean zRemove;
        synchronized (sListeners) {
            Iterator<DiscoveryListenerSet> it = sListeners.values().iterator();
            z = false;
            while (true) {
                if (!it.hasNext()) {
                    break;
                }
                DiscoveryListenerSet next = it.next();
                synchronized (next) {
                    zRemove = next.subListeners.remove(discoveryListener);
                    if (zRemove) {
                        break;
                    }
                }
                z = zRemove;
                z = zRemove;
            }
        }
        return z;
    }

    private static class DiscoveryListenerSet {
        final MultiListener mainListener;
        final ArrayList<NsdManager.DiscoveryListener> subListeners;

        private DiscoveryListenerSet(ArrayList<NsdManager.DiscoveryListener> arrayList, MultiListener multiListener) {
            this.subListeners = arrayList;
            this.mainListener = multiListener;
        }
    }

    private static class MultiListener implements NsdManager.DiscoveryListener {
        private final ArrayList<NsdManager.DiscoveryListener> mListeners;

        public MultiListener(ArrayList<NsdManager.DiscoveryListener> arrayList) {
            this.mListeners = arrayList;
        }

        @Override
        public void onStartDiscoveryFailed(String str, int i) {
            Log.w("DiscoveryListenerMx", "Failed to start network discovery for type " + str + ": " + i);
        }

        @Override
        public void onStopDiscoveryFailed(String str, int i) {
            Log.w("DiscoveryListenerMx", "Failed to stop network discovery for type " + str + ": " + i);
        }

        @Override
        public void onDiscoveryStarted(String str) {
        }

        @Override
        public void onDiscoveryStopped(String str) {
        }

        @Override
        public void onServiceFound(NsdServiceInfo nsdServiceInfo) {
            synchronized (this.mListeners) {
                int size = this.mListeners.size();
                for (int i = 0; i < size; i++) {
                    this.mListeners.get(i).onServiceFound(nsdServiceInfo);
                }
            }
        }

        @Override
        public void onServiceLost(NsdServiceInfo nsdServiceInfo) {
            synchronized (this.mListeners) {
                int size = this.mListeners.size();
                for (int i = 0; i < size; i++) {
                    this.mListeners.get(i).onServiceLost(nsdServiceInfo);
                }
            }
        }
    }
}
