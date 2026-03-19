package com.android.services.telephony;

import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import com.android.internal.telephony.Phone;
import com.android.internal.util.Preconditions;

final class PstnPhoneCapabilitiesNotifier {
    private static final int EVENT_VIDEO_CAPABILITIES_CHANGED = 1;
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message message) {
            if (message.what == 1) {
                PstnPhoneCapabilitiesNotifier.this.handleVideoCapabilitesChanged((AsyncResult) message.obj);
            }
        }
    };
    private final Listener mListener;
    private final Phone mPhone;

    public interface Listener {
        void onVideoCapabilitiesChanged(boolean z);
    }

    PstnPhoneCapabilitiesNotifier(Phone phone, Listener listener) {
        Preconditions.checkNotNull(phone);
        this.mPhone = phone;
        this.mListener = listener;
        registerForNotifications();
    }

    void teardown() {
        unregisterForNotifications();
    }

    private void registerForNotifications() {
        if (this.mPhone != null) {
            Log.d(this, "Registering: " + this.mPhone, new Object[0]);
            this.mPhone.registerForVideoCapabilityChanged(this.mHandler, 1, (Object) null);
        }
    }

    private void unregisterForNotifications() {
        if (this.mPhone != null) {
            Log.d(this, "Unregistering: " + this.mPhone, new Object[0]);
            this.mPhone.unregisterForVideoCapabilityChanged(this.mHandler);
        }
    }

    private void handleVideoCapabilitesChanged(AsyncResult asyncResult) {
        try {
            boolean zBooleanValue = ((Boolean) asyncResult.result).booleanValue();
            Log.i(this, "handleVideoCapabilitesChanged. Video capability - " + zBooleanValue, new Object[0]);
            this.mListener.onVideoCapabilitiesChanged(zBooleanValue);
        } catch (Exception e) {
            Log.w(this, "handleVideoCapabilitesChanged. Exception=" + e, new Object[0]);
        }
    }

    private int newCapabilities(int i, int i2, boolean z) {
        if (z) {
            return i | i2;
        }
        return i & (~i2);
    }
}
