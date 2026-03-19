package android.app.admin;

import com.android.server.LocalServices;

public abstract class DevicePolicyCache {
    public abstract boolean getScreenCaptureDisabled(int i);

    protected DevicePolicyCache() {
    }

    public static DevicePolicyCache getInstance() {
        DevicePolicyManagerInternal devicePolicyManagerInternal = (DevicePolicyManagerInternal) LocalServices.getService(DevicePolicyManagerInternal.class);
        return devicePolicyManagerInternal != null ? devicePolicyManagerInternal.getDevicePolicyCache() : EmptyDevicePolicyCache.INSTANCE;
    }

    private static class EmptyDevicePolicyCache extends DevicePolicyCache {
        private static final EmptyDevicePolicyCache INSTANCE = new EmptyDevicePolicyCache();

        private EmptyDevicePolicyCache() {
        }

        @Override
        public boolean getScreenCaptureDisabled(int i) {
            return false;
        }
    }
}
