package libcore.net.event;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class NetworkEventDispatcher {
    private static final NetworkEventDispatcher instance = new NetworkEventDispatcher();
    private final List<NetworkEventListener> listeners = new CopyOnWriteArrayList();

    public static NetworkEventDispatcher getInstance() {
        return instance;
    }

    protected NetworkEventDispatcher() {
    }

    public void addListener(NetworkEventListener networkEventListener) {
        if (networkEventListener == null) {
            throw new NullPointerException("toAdd == null");
        }
        this.listeners.add(networkEventListener);
    }

    public void removeListener(NetworkEventListener networkEventListener) {
        for (NetworkEventListener networkEventListener2 : this.listeners) {
            if (networkEventListener2 == networkEventListener) {
                this.listeners.remove(networkEventListener2);
                return;
            }
        }
    }

    public void onNetworkConfigurationChanged() {
        Iterator<NetworkEventListener> it = this.listeners.iterator();
        while (it.hasNext()) {
            try {
                it.next().onNetworkConfigurationChanged();
            } catch (RuntimeException e) {
                System.logI("Exception thrown during network event propagation", e);
            }
        }
    }
}
