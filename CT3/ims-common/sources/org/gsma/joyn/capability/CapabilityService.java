package org.gsma.joyn.capability;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.IInterface;
import android.os.RemoteException;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.gsma.joyn.ICoreServiceWrapper;
import org.gsma.joyn.JoynService;
import org.gsma.joyn.JoynServiceConfiguration;
import org.gsma.joyn.JoynServiceException;
import org.gsma.joyn.JoynServiceListener;
import org.gsma.joyn.JoynServiceNotAvailableException;
import org.gsma.joyn.JoynServiceRegistrationListener;
import org.gsma.joyn.Logger;
import org.gsma.joyn.capability.ICapabilityService;

public class CapabilityService extends JoynService {
    public static final String EXTENSION_MIME_TYPE = "org.gsma.joyn";
    public static final String INTENT_EXTENSIONS = "org.gsma.joyn.capability.EXTENSION";
    public static final String TAG = "CapabilityService";
    private ICapabilityService api;
    private ServiceConnection apiConnection;
    public static final ComponentName DEFAULT_IMPL_COMPONENT = new ComponentName("com.orangelabs.rcs", "com.orangelabs.rcs.service.RcsCoreService");
    public static final ComponentName STANDALONE_PRESENCE_IMPL_COMPONENT = new ComponentName("com.mediatek.presence", "com.mediatek.presence.service.RcsCoreService");

    public CapabilityService(Context ctx, JoynServiceListener listener) {
        super(ctx, listener);
        this.api = null;
        this.apiConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName className, IBinder service) {
                Logger.i(CapabilityService.TAG, "onServiceConnected entry");
                ICoreServiceWrapper mCoreServiceWrapperBinder = ICoreServiceWrapper.Stub.asInterface(service);
                IBinder binder = null;
                try {
                    binder = mCoreServiceWrapperBinder.getCapabilitiesServiceBinder();
                } catch (RemoteException e1) {
                    e1.printStackTrace();
                }
                CapabilityService.this.setApi(ICapabilityService.Stub.asInterface(binder));
                if (((JoynService) CapabilityService.this).serviceListener != null) {
                    ((JoynService) CapabilityService.this).serviceListener.onServiceConnected();
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName className) {
                Logger.i(CapabilityService.TAG, "onServiceDisconnected entry");
                CapabilityService.this.setApi(null);
                if (((JoynService) CapabilityService.this).serviceListener != null) {
                    ((JoynService) CapabilityService.this).serviceListener.onServiceDisconnected(2);
                }
            }
        };
    }

    @Override
    public void connect() {
        ComponentName cmp;
        Logger.i(TAG, "connect() entry");
        Intent intent = new Intent();
        if (JoynServiceConfiguration.isPresenceDiscoverySupported(this.ctx)) {
            cmp = STANDALONE_PRESENCE_IMPL_COMPONENT;
        } else {
            cmp = DEFAULT_IMPL_COMPONENT;
        }
        intent.setComponent(cmp);
        intent.setAction(ICapabilityService.class.getName());
        this.ctx.bindService(intent, this.apiConnection, 0);
    }

    @Override
    public void disconnect() {
        try {
            Logger.i(TAG, "disconnect() entry");
            this.ctx.unbindService(this.apiConnection);
        } catch (IllegalArgumentException e) {
        }
    }

    @Override
    protected void setApi(IInterface api) {
        super.setApi(api);
        Logger.i(TAG, "setApi entry" + api);
        this.api = (ICapabilityService) api;
    }

    public Capabilities getMyCapabilities() throws JoynServiceException {
        Logger.i(TAG, "getMyCapabilities entry");
        if (this.api != null) {
            try {
                return this.api.getMyCapabilities();
            } catch (Exception e) {
                throw new JoynServiceException(e.getMessage());
            }
        }
        throw new JoynServiceNotAvailableException();
    }

    public Capabilities getContactCapabilities(String contact) throws JoynServiceException {
        Logger.i(TAG, "getContactCapabilities entry" + contact);
        if (this.api != null) {
            try {
                return this.api.getContactCapabilities(contact);
            } catch (Exception e) {
                throw new JoynServiceException(e.getMessage());
            }
        }
        throw new JoynServiceNotAvailableException();
    }

    public void requestContactCapabilities(String contact) throws JoynServiceException {
        Logger.i(TAG, "requestContactCapabilities entry" + contact);
        if (this.api != null) {
            try {
                this.api.requestContactCapabilities(contact);
                return;
            } catch (Exception e) {
                throw new JoynServiceException(e.getMessage());
            }
        }
        throw new JoynServiceNotAvailableException();
    }

    public void requestContactCapabilities(Set<String> contacts) throws JoynServiceException {
        Logger.i(TAG, "requestContactCapabilities entry" + contacts);
        Iterator<String> values = contacts.iterator();
        while (values.hasNext()) {
            requestContactCapabilities(values.next());
        }
    }

    public void requestAllContactsCapabilities() throws JoynServiceException {
        Logger.i(TAG, "requestAllContactsCapabilities entry");
        if (this.api != null) {
            try {
                this.api.requestAllContactsCapabilities();
                return;
            } catch (Exception e) {
                throw new JoynServiceException(e.getMessage());
            }
        }
        throw new JoynServiceNotAvailableException();
    }

    @Override
    public int getServiceVersion() throws JoynServiceException {
        Logger.i(TAG, "getServiceVersion entry");
        if (this.api != null) {
            if (this.version == null) {
                try {
                    this.version = Integer.valueOf(this.api.getServiceVersion());
                } catch (Exception e) {
                    throw new JoynServiceException(e.getMessage());
                }
            }
            Logger.i(TAG, "getServiceVersion is" + this.version);
            return this.version.intValue();
        }
        throw new JoynServiceNotAvailableException();
    }

    @Override
    public boolean isServiceRegistered() throws JoynServiceException {
        Logger.i(TAG, "isServiceRegistered entry");
        if (this.api != null) {
            try {
                boolean serviceStatus = this.api.isServiceRegistered();
                Logger.i(TAG, "isServiceRegistered" + serviceStatus);
                return serviceStatus;
            } catch (Exception e) {
                throw new JoynServiceException(e.getMessage());
            }
        }
        throw new JoynServiceNotAvailableException();
    }

    @Override
    public void addServiceRegistrationListener(JoynServiceRegistrationListener listener) throws JoynServiceException {
        Logger.i(TAG, "addServiceRegistrationListener entry" + listener);
        if (this.api != null) {
            try {
                this.api.addServiceRegistrationListener(listener);
                return;
            } catch (Exception e) {
                throw new JoynServiceException(e.getMessage());
            }
        }
        throw new JoynServiceNotAvailableException();
    }

    @Override
    public void removeServiceRegistrationListener(JoynServiceRegistrationListener listener) throws JoynServiceException {
        Logger.i(TAG, "removeServiceRegistrationListener entry" + listener);
        if (this.api != null) {
            try {
                this.api.removeServiceRegistrationListener(listener);
                return;
            } catch (Exception e) {
                throw new JoynServiceException(e.getMessage());
            }
        }
        throw new JoynServiceNotAvailableException();
    }

    public void addCapabilitiesListener(CapabilitiesListener listener) throws JoynServiceException {
        Logger.i(TAG, "addCapabilitiesListener entry" + listener);
        if (this.api != null) {
            try {
                this.api.addCapabilitiesListener(listener);
                return;
            } catch (Exception e) {
                throw new JoynServiceException(e.getMessage());
            }
        }
        throw new JoynServiceNotAvailableException();
    }

    public void removeCapabilitiesListener(CapabilitiesListener listener) throws JoynServiceException {
        Logger.i(TAG, "removeCapabilitiesListener entry" + listener);
        if (this.api != null) {
            try {
                this.api.removeCapabilitiesListener(listener);
                return;
            } catch (Exception e) {
                throw new JoynServiceException(e.getMessage());
            }
        }
        throw new JoynServiceNotAvailableException();
    }

    public void addCapabilitiesListener(Set<String> contacts, CapabilitiesListener listener) throws JoynServiceException {
        Logger.i(TAG, "addCapabilitiesListener entry" + contacts + listener);
        if (this.api != null) {
            try {
                for (String contact : contacts) {
                    this.api.addContactCapabilitiesListener(contact, listener);
                }
                return;
            } catch (Exception e) {
                throw new JoynServiceException(e.getMessage());
            }
        }
        throw new JoynServiceNotAvailableException();
    }

    public void removeCapabilitiesListener(Set<String> contacts, CapabilitiesListener listener) throws JoynServiceException {
        Logger.i(TAG, "removeCapabilitiesListener entry" + contacts + listener);
        if (this.api != null) {
            try {
                for (String contact : contacts) {
                    this.api.removeContactCapabilitiesListener(contact, listener);
                }
                return;
            } catch (Exception e) {
                throw new JoynServiceException(e.getMessage());
            }
        }
        throw new JoynServiceNotAvailableException();
    }

    public void forceRequestContactCapabilities(String contact) throws JoynServiceException {
        Logger.i(TAG, "forceRequestContactCapabilities entry" + contact);
        if (this.api != null) {
            try {
                this.api.forceRequestContactCapabilities(contact);
                return;
            } catch (Exception e) {
                throw new JoynServiceException(e.getMessage());
            }
        }
        throw new JoynServiceNotAvailableException();
    }

    public void requestContactsCapabilities(List<String> contacts) throws JoynServiceException {
        Logger.i(TAG, "requestContactCapabilities by list entry" + contacts);
        if (this.api != null) {
            try {
                this.api.requestContactsCapabilities(contacts);
                return;
            } catch (Exception e) {
                throw new JoynServiceException(e.getMessage());
            }
        }
        throw new JoynServiceNotAvailableException();
    }
}
