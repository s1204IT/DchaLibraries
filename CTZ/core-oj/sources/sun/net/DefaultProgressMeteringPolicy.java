package sun.net;

import java.net.URL;

class DefaultProgressMeteringPolicy implements ProgressMeteringPolicy {
    DefaultProgressMeteringPolicy() {
    }

    @Override
    public boolean shouldMeterInput(URL url, String str) {
        return false;
    }

    @Override
    public int getProgressUpdateThreshold() {
        return 8192;
    }
}
