package org.gsma.joyn.gsh;

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
import org.gsma.joyn.Logger;
import org.gsma.joyn.Permissions;
import org.gsma.joyn.chat.Geoloc;
import org.gsma.joyn.gsh.IGeolocSharing;
import org.gsma.joyn.gsh.IGeolocSharingService;

public class GeolocSharingService extends JoynService {
    public static final String TAG = "TAPI-GeolocSharingService";
    private IGeolocSharingService api;
    private ServiceConnection apiConnection;

    public GeolocSharingService(Context ctx, JoynServiceListener listener) {
        super(ctx, listener);
        this.api = null;
        this.apiConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName className, IBinder service) {
                Logger.i(GeolocSharingService.TAG, "onServiceConnected entry " + className);
                ICoreServiceWrapper mCoreServiceWrapperBinder = ICoreServiceWrapper.Stub.asInterface(service);
                IBinder binder = null;
                try {
                    binder = mCoreServiceWrapperBinder.getGeolocServiceBinder();
                } catch (RemoteException e1) {
                    e1.printStackTrace();
                }
                GeolocSharingService.this.setApi(IGeolocSharingService.Stub.asInterface(binder));
                if (((JoynService) GeolocSharingService.this).serviceListener != null) {
                    ((JoynService) GeolocSharingService.this).serviceListener.onServiceConnected();
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName className) {
                Logger.i(GeolocSharingService.TAG, "onServiceDisconnected entry " + className);
                GeolocSharingService.this.setApi(null);
                if (((JoynService) GeolocSharingService.this).serviceListener != null) {
                    ((JoynService) GeolocSharingService.this).serviceListener.onServiceDisconnected(2);
                }
            }
        };
    }

    @Override
    public void connect() {
        Logger.i(TAG, "GeolocSharing connect() entry");
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
        this.api = (IGeolocSharingService) api;
    }

    public GeolocSharing shareGeoloc(String contact, Geoloc geoloc, GeolocSharingListener listener) throws JoynServiceException {
        if (this.ctx.checkCallingOrSelfPermission(Permissions.RCS_LOCATION_SEND) != 0) {
            throw new SecurityException(" Required permission RCS_LOCATION_SEND");
        }
        if (this.ctx.checkCallingOrSelfPermission(Permissions.RCS_USE_CHAT) != 0) {
            throw new SecurityException(" Required permission RCS_USE_CHAT");
        }
        Logger.i(TAG, "shareGeoloc() entry contact=" + contact + " geoloc =" + geoloc + " listener =" + listener);
        if (this.api != null) {
            try {
                IGeolocSharing sharingIntf = this.api.shareGeoloc(contact, geoloc, listener);
                if (sharingIntf != null) {
                    return new GeolocSharing(sharingIntf);
                }
                return null;
            } catch (Exception e) {
                throw new JoynServiceException(e.getMessage());
            }
        }
        throw new JoynServiceNotAvailableException();
    }

    public Set<GeolocSharing> getGeolocSharings() throws JoynServiceException {
        Logger.i(TAG, "getGeolocSharings entry ");
        if (this.api != null) {
            try {
                Set<GeolocSharing> result = new HashSet<>();
                List<IBinder> ishList = this.api.getGeolocSharings();
                for (IBinder binder : ishList) {
                    GeolocSharing sharing = new GeolocSharing(IGeolocSharing.Stub.asInterface(binder));
                    result.add(sharing);
                }
                Logger.i(TAG, "getGeolocSharings returning " + result);
                return result;
            } catch (Exception e) {
                throw new JoynServiceException(e.getMessage());
            }
        }
        throw new JoynServiceNotAvailableException();
    }

    public GeolocSharing getGeolocSharing(String sharingId) throws JoynServiceException {
        Logger.i(TAG, "getGeolocSharing entry " + sharingId);
        if (this.api != null) {
            try {
                IGeolocSharing sharingIntf = this.api.getGeolocSharing(sharingId);
                if (sharingIntf != null) {
                    return new GeolocSharing(sharingIntf);
                }
                return null;
            } catch (Exception e) {
                throw new JoynServiceException(e.getMessage());
            }
        }
        throw new JoynServiceNotAvailableException();
    }

    public GeolocSharing getGeolocSharingFor(Intent intent) throws JoynServiceException {
        Logger.i(TAG, "getGeolocSharingFor entry " + intent);
        if (this.api != null) {
            try {
                String sharingId = intent.getStringExtra("sharingId");
                if (sharingId != null) {
                    return getGeolocSharing(sharingId);
                }
                return null;
            } catch (Exception e) {
                throw new JoynServiceException(e.getMessage());
            }
        }
        throw new JoynServiceNotAvailableException();
    }

    public void addNewGeolocSharingListener(NewGeolocSharingListener listener) throws JoynServiceException {
        Logger.i(TAG, "addNewGeolocSharingListener entry " + listener);
        if (this.api != null) {
            try {
                this.api.addNewGeolocSharingListener(listener);
                return;
            } catch (Exception e) {
                throw new JoynServiceException(e.getMessage());
            }
        }
        throw new JoynServiceNotAvailableException();
    }

    public void removeNewGeolocSharingListener(NewGeolocSharingListener listener) throws JoynServiceException {
        Logger.i(TAG, "removeNewGeolocSharingListener entry " + listener);
        if (this.api != null) {
            try {
                this.api.removeNewGeolocSharingListener(listener);
                return;
            } catch (Exception e) {
                throw new JoynServiceException(e.getMessage());
            }
        }
        throw new JoynServiceNotAvailableException();
    }
}
