package com.android.systemui.statusbar.policy;

import com.android.systemui.Dumpable;
import java.util.Set;

public interface CastController extends Dumpable, CallbackController<Callback> {

    public interface Callback {
        void onCastDevicesChanged();
    }

    public static final class CastDevice {
        public String description;
        public String id;
        public String name;
        public int state = 0;
        public Object tag;
    }

    Set<CastDevice> getCastDevices();

    void setCurrentUserId(int i);

    void setDiscovering(boolean z);

    void startCasting(CastDevice castDevice);

    void stopCasting(CastDevice castDevice);
}
