package org.gsma.joyn.ipcall;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.IInterface;
import android.os.RemoteException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.gsma.joyn.ICoreServiceWrapper;
import org.gsma.joyn.JoynService;
import org.gsma.joyn.JoynServiceException;
import org.gsma.joyn.JoynServiceListener;
import org.gsma.joyn.JoynServiceNotAvailableException;
import org.gsma.joyn.JoynServiceRegistrationListener;
import org.gsma.joyn.Logger;
import org.gsma.joyn.Permissions;
import org.gsma.joyn.ipcall.IIPCall;
import org.gsma.joyn.ipcall.IIPCallService;

public class IPCallService extends JoynService {
    public static final String TAG = "IPCallService";
    private IIPCallService api;
    private ServiceConnection apiConnection;

    public IPCallService(Context ctx, JoynServiceListener listener) {
        super(ctx, listener);
        this.api = null;
        this.apiConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName className, IBinder service) {
                Logger.i(IPCallService.TAG, "onServiceConnected() entry");
                ICoreServiceWrapper mCoreServiceWrapperBinder = ICoreServiceWrapper.Stub.asInterface(service);
                IBinder binder = null;
                try {
                    binder = mCoreServiceWrapperBinder.getIPCallServiceBinder();
                } catch (RemoteException e1) {
                    e1.printStackTrace();
                }
                IPCallService.this.setApi(IIPCallService.Stub.asInterface(binder));
                if (((JoynService) IPCallService.this).serviceListener != null) {
                    ((JoynService) IPCallService.this).serviceListener.onServiceConnected();
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName className) {
                Logger.i(IPCallService.TAG, "onServiceDisconnected() entry");
                IPCallService.this.setApi(null);
                if (((JoynService) IPCallService.this).serviceListener != null) {
                    ((JoynService) IPCallService.this).serviceListener.onServiceDisconnected(2);
                }
            }
        };
    }

    @Override
    public void connect() {
        if (this.ctx.checkCallingOrSelfPermission(Permissions.RCS_USE_IPCALL) != 0) {
            throw new SecurityException(" Required permission RCS_USE_IPCALL");
        }
        Logger.i(TAG, "connect() entry");
        Intent intent = new Intent();
        ComponentName cmp = new ComponentName("com.orangelabs.rcs", "com.orangelabs.rcs.service.RcsCoreService");
        intent.setComponent(cmp);
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
        Logger.i(TAG, "setApi() entry" + api);
        this.api = (IIPCallService) api;
    }

    public IPCallServiceConfiguration getConfiguration() throws JoynServiceException {
        if (this.api != null) {
            Logger.i(TAG, "getConfiguration() entry");
            try {
                return this.api.getConfiguration();
            } catch (Exception e) {
                throw new JoynServiceException(e.getMessage());
            }
        }
        throw new JoynServiceNotAvailableException();
    }

    public IPCall initiateCall(String contact, IPCallPlayer player, IPCallRenderer renderer, IPCallListener listener) throws JoynServiceException {
        if (this.api != null) {
            Logger.i(TAG, "initiateCall() entrycontact" + contact + "player" + player + "renderer" + renderer + "listener" + listener);
            try {
                IIPCall callIntf = this.api.initiateCall(contact, player, renderer, listener);
                if (callIntf != null) {
                    return new IPCall(callIntf);
                }
                return null;
            } catch (Exception e) {
                throw new JoynServiceException(e.getMessage());
            }
        }
        throw new JoynServiceNotAvailableException();
    }

    public IPCall initiateVisioCall(String contact, IPCallPlayer player, IPCallRenderer renderer, IPCallListener listener) throws JoynServiceException {
        if (this.api != null) {
            try {
                Logger.i(TAG, "initiateVisioCall() entrycontact" + contact + "player" + player + "renderer" + renderer + "listener" + listener);
                IIPCall callIntf = this.api.initiateVisioCall(contact, player, renderer, listener);
                if (callIntf != null) {
                    return new IPCall(callIntf);
                }
                return null;
            } catch (Exception e) {
                throw new JoynServiceException(e.getMessage());
            }
        }
        throw new JoynServiceNotAvailableException();
    }

    public Set<IPCall> getIPCalls() throws JoynServiceException {
        if (this.api != null) {
            try {
                Set<IPCall> result = new HashSet<>();
                List<IBinder> vshList = this.api.getIPCalls();
                for (IBinder binder : vshList) {
                    IPCall call = new IPCall(IIPCall.Stub.asInterface(binder));
                    result.add(call);
                }
                Logger.i(TAG, "getIPCalls() value - " + result);
                return result;
            } catch (Exception e) {
                throw new JoynServiceException(e.getMessage());
            }
        }
        throw new JoynServiceNotAvailableException();
    }

    public IPCall getIPCall(String callId) throws JoynServiceException {
        if (this.api != null) {
            Logger.i(TAG, "getIPCall() entry");
            try {
                IIPCall callIntf = this.api.getIPCall(callId);
                if (callIntf != null) {
                    return new IPCall(callIntf);
                }
                return null;
            } catch (Exception e) {
                throw new JoynServiceException(e.getMessage());
            }
        }
        throw new JoynServiceNotAvailableException();
    }

    public IPCall getIPCallFor(Intent intent) throws JoynServiceException {
        if (this.api != null) {
            try {
                Logger.i(TAG, "getIPCallFor() entry");
                String callId = intent.getStringExtra(IPCallIntent.EXTRA_CALL_ID);
                if (callId != null) {
                    return getIPCall(callId);
                }
                return null;
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

    @Override
    public int getServiceVersion() throws JoynServiceException {
        Logger.i(TAG, "getServiceVersion() entry");
        if (this.api != null) {
            if (this.version == null) {
                try {
                    this.version = Integer.valueOf(this.api.getServiceVersion());
                    Logger.i(TAG, "getServiceVersion() value" + this.version);
                } catch (Exception e) {
                    throw new JoynServiceException(e.getMessage());
                }
            }
            return this.version.intValue();
        }
        throw new JoynServiceNotAvailableException();
    }

    @Override
    public boolean isServiceRegistered() throws JoynServiceException {
        if (this.api != null) {
            Logger.i(TAG, "isServiceRegistered() entry");
            try {
                boolean serviceStatus = this.api.isServiceRegistered();
                Logger.i(TAG, "isServiceRegistered() value" + serviceStatus);
                return serviceStatus;
            } catch (Exception e) {
                throw new JoynServiceException(e.getMessage());
            }
        }
        throw new JoynServiceNotAvailableException();
    }

    public void addNewIPCallListener(NewIPCallListener listener) throws JoynServiceException {
        if (this.api != null) {
            Logger.i(TAG, "addNewIPCallListener() entry" + listener);
            try {
                this.api.addNewIPCallListener(listener);
                return;
            } catch (Exception e) {
                throw new JoynServiceException(e.getMessage());
            }
        }
        throw new JoynServiceNotAvailableException();
    }

    public void removeNewIPCallListener(NewIPCallListener listener) throws JoynServiceException {
        if (this.api != null) {
            Logger.i(TAG, "removeNewIPCallListener() entry" + listener);
            try {
                this.api.removeNewIPCallListener(listener);
                return;
            } catch (Exception e) {
                throw new JoynServiceException(e.getMessage());
            }
        }
        throw new JoynServiceNotAvailableException();
    }
}
