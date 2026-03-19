package com.android.ims.internal.uce;

import com.android.ims.internal.uce.common.UceLong;
import com.android.ims.internal.uce.options.IOptionsListener;
import com.android.ims.internal.uce.options.IOptionsService;
import com.android.ims.internal.uce.presence.IPresenceListener;
import com.android.ims.internal.uce.presence.IPresenceService;
import com.android.ims.internal.uce.uceservice.IUceListener;
import com.android.ims.internal.uce.uceservice.IUceService;

public abstract class UceServiceBase {
    private UceServiceBinder mBinder;

    private final class UceServiceBinder extends IUceService.Stub {
        private UceServiceBinder() {
        }

        @Override
        public boolean startService(IUceListener iUceListener) {
            return UceServiceBase.this.onServiceStart(iUceListener);
        }

        @Override
        public boolean stopService() {
            return UceServiceBase.this.onStopService();
        }

        @Override
        public boolean isServiceStarted() {
            return UceServiceBase.this.onIsServiceStarted();
        }

        @Override
        public int createOptionsService(IOptionsListener iOptionsListener, UceLong uceLong) {
            return UceServiceBase.this.onCreateOptionsService(iOptionsListener, uceLong);
        }

        @Override
        public void destroyOptionsService(int i) {
            UceServiceBase.this.onDestroyOptionsService(i);
        }

        @Override
        public int createPresenceService(IPresenceListener iPresenceListener, UceLong uceLong) {
            return UceServiceBase.this.onCreatePresService(iPresenceListener, uceLong);
        }

        @Override
        public void destroyPresenceService(int i) {
            UceServiceBase.this.onDestroyPresService(i);
        }

        @Override
        public boolean getServiceStatus() {
            return UceServiceBase.this.onGetServiceStatus();
        }

        @Override
        public IPresenceService getPresenceService() {
            return UceServiceBase.this.onGetPresenceService();
        }

        @Override
        public IOptionsService getOptionsService() {
            return UceServiceBase.this.onGetOptionsService();
        }
    }

    public UceServiceBinder getBinder() {
        if (this.mBinder == null) {
            this.mBinder = new UceServiceBinder();
        }
        return this.mBinder;
    }

    protected boolean onServiceStart(IUceListener iUceListener) {
        return false;
    }

    protected boolean onStopService() {
        return false;
    }

    protected boolean onIsServiceStarted() {
        return false;
    }

    protected int onCreateOptionsService(IOptionsListener iOptionsListener, UceLong uceLong) {
        return 0;
    }

    protected void onDestroyOptionsService(int i) {
    }

    protected int onCreatePresService(IPresenceListener iPresenceListener, UceLong uceLong) {
        return 0;
    }

    protected void onDestroyPresService(int i) {
    }

    protected boolean onGetServiceStatus() {
        return false;
    }

    protected IPresenceService onGetPresenceService() {
        return null;
    }

    protected IOptionsService onGetOptionsService() {
        return null;
    }
}
