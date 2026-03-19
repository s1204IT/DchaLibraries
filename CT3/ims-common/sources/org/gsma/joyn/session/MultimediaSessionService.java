package org.gsma.joyn.session;

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
import org.gsma.joyn.session.IMultimediaSession;
import org.gsma.joyn.session.IMultimediaSessionService;

public class MultimediaSessionService extends JoynService {
    private IMultimediaSessionService api;
    private ServiceConnection apiConnection;

    public MultimediaSessionService(Context ctx, JoynServiceListener listener) {
        super(ctx, listener);
        this.api = null;
        this.apiConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName className, IBinder service) {
                ICoreServiceWrapper mCoreServiceWrapperBinder = ICoreServiceWrapper.Stub.asInterface(service);
                IBinder binder = null;
                try {
                    binder = mCoreServiceWrapperBinder.getMultimediaSessionServiceBinder();
                } catch (RemoteException e1) {
                    e1.printStackTrace();
                }
                MultimediaSessionService.this.setApi(IMultimediaSessionService.Stub.asInterface(binder));
                if (((JoynService) MultimediaSessionService.this).serviceListener != null) {
                    ((JoynService) MultimediaSessionService.this).serviceListener.onServiceConnected();
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName className) {
                MultimediaSessionService.this.setApi(null);
                if (((JoynService) MultimediaSessionService.this).serviceListener != null) {
                    ((JoynService) MultimediaSessionService.this).serviceListener.onServiceDisconnected(2);
                }
            }
        };
    }

    @Override
    public void connect() {
        Intent intent = new Intent();
        ComponentName cmp = new ComponentName("com.orangelabs.rcs", "com.orangelabs.rcs.service.RcsCoreService");
        intent.setComponent(cmp);
        this.ctx.bindService(intent, this.apiConnection, 0);
    }

    @Override
    public void disconnect() {
        try {
            this.ctx.unbindService(this.apiConnection);
        } catch (IllegalArgumentException e) {
        }
    }

    @Override
    protected void setApi(IInterface api) {
        super.setApi(api);
        this.api = (IMultimediaSessionService) api;
    }

    public MultimediaSession initiateSession(String serviceId, String contact, MultimediaSessionListener listener) throws JoynServiceException {
        if (this.api != null) {
            try {
                IMultimediaSession sessionIntf = this.api.initiateSession(serviceId, contact, listener);
                if (sessionIntf != null) {
                    return new MultimediaSession(sessionIntf);
                }
                return null;
            } catch (Exception e) {
                throw new JoynServiceException(e.getMessage());
            }
        }
        throw new JoynServiceNotAvailableException();
    }

    public Set<MultimediaSession> getSessions(String serviceId) throws JoynServiceException {
        if (this.api != null) {
            try {
                Set<MultimediaSession> result = new HashSet<>();
                List<IBinder> mmsList = this.api.getSessions(serviceId);
                for (IBinder binder : mmsList) {
                    MultimediaSession session = new MultimediaSession(IMultimediaSession.Stub.asInterface(binder));
                    result.add(session);
                }
                return result;
            } catch (Exception e) {
                throw new JoynServiceException(e.getMessage());
            }
        }
        throw new JoynServiceNotAvailableException();
    }

    public MultimediaSession getSession(String sessionId) throws JoynServiceException {
        if (this.api != null) {
            try {
                IMultimediaSession sessionIntf = this.api.getSession(sessionId);
                if (sessionIntf != null) {
                    return new MultimediaSession(sessionIntf);
                }
                return null;
            } catch (Exception e) {
                throw new JoynServiceException(e.getMessage());
            }
        }
        throw new JoynServiceNotAvailableException();
    }

    public MultimediaSession getSessionFor(Intent intent) throws JoynServiceException {
        if (this.api != null) {
            try {
                String sessionId = intent.getStringExtra(MultimediaSessionIntent.EXTRA_SESSION_ID);
                if (sessionId != null) {
                    return getSession(sessionId);
                }
                return null;
            } catch (Exception e) {
                throw new JoynServiceException(e.getMessage());
            }
        }
        throw new JoynServiceNotAvailableException();
    }

    public boolean sendMessage(String serviceId, String contact, byte[] content) throws JoynServiceException {
        if (this.api != null) {
            try {
                return this.api.sendMessage(serviceId, contact, content);
            } catch (Exception e) {
                throw new JoynServiceException(e.getMessage());
            }
        }
        throw new JoynServiceNotAvailableException();
    }
}
