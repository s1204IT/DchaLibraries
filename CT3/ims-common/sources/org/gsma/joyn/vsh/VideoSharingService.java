package org.gsma.joyn.vsh;

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
import org.gsma.joyn.vsh.IVideoSharing;
import org.gsma.joyn.vsh.IVideoSharingService;

public class VideoSharingService extends JoynService {
    public static final String TAG = "VideoSharingService";
    private IVideoSharingService api;
    private ServiceConnection apiConnection;

    public VideoSharingService(Context ctx, JoynServiceListener listener) {
        super(ctx, listener);
        this.api = null;
        this.apiConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName className, IBinder service) {
                ICoreServiceWrapper mCoreServiceWrapperBinder = ICoreServiceWrapper.Stub.asInterface(service);
                IBinder binder = null;
                try {
                    binder = mCoreServiceWrapperBinder.getVideoSharingServiceBinder();
                } catch (RemoteException e1) {
                    e1.printStackTrace();
                }
                VideoSharingService.this.setApi(IVideoSharingService.Stub.asInterface(binder));
                if (((JoynService) VideoSharingService.this).serviceListener != null) {
                    ((JoynService) VideoSharingService.this).serviceListener.onServiceConnected();
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName className) {
                VideoSharingService.this.setApi(null);
                if (((JoynService) VideoSharingService.this).serviceListener == null) {
                    return;
                }
                ((JoynService) VideoSharingService.this).serviceListener.onServiceDisconnected(2);
            }
        };
    }

    @Override
    public void connect() {
        Logger.i(TAG, "connect() entry");
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
        this.api = (IVideoSharingService) api;
    }

    public VideoSharingServiceConfiguration getConfiguration() throws JoynServiceException {
        if (this.api != null) {
            try {
                return this.api.getConfiguration();
            } catch (Exception e) {
                throw new JoynServiceException(e.getMessage());
            }
        }
        throw new JoynServiceNotAvailableException();
    }

    public VideoSharing shareVideo(String contact, VideoPlayer player, VideoSharingListener listener) throws JoynServiceException {
        if (this.api != null) {
            try {
                IVideoSharing sharingIntf = this.api.shareVideo(contact, player, listener);
                if (sharingIntf != null) {
                    return new VideoSharing(sharingIntf);
                }
                return null;
            } catch (Exception e) {
                throw new JoynServiceException(e.getMessage());
            }
        }
        throw new JoynServiceNotAvailableException();
    }

    public Set<VideoSharing> getVideoSharings() throws JoynServiceException {
        if (this.api != null) {
            try {
                Set<VideoSharing> result = new HashSet<>();
                List<IBinder> vshList = this.api.getVideoSharings();
                for (IBinder binder : vshList) {
                    VideoSharing sharing = new VideoSharing(IVideoSharing.Stub.asInterface(binder));
                    result.add(sharing);
                }
                return result;
            } catch (Exception e) {
                throw new JoynServiceException(e.getMessage());
            }
        }
        throw new JoynServiceNotAvailableException();
    }

    public VideoSharing getVideoSharing(String sharingId) throws JoynServiceException {
        if (this.api != null) {
            try {
                IVideoSharing sharingIntf = this.api.getVideoSharing(sharingId);
                if (sharingIntf != null) {
                    return new VideoSharing(sharingIntf);
                }
                return null;
            } catch (Exception e) {
                throw new JoynServiceException(e.getMessage());
            }
        }
        throw new JoynServiceNotAvailableException();
    }

    public VideoSharing getVideoSharingFor(Intent intent) throws JoynServiceException {
        if (this.api != null) {
            try {
                String sharingId = intent.getStringExtra("sharingId");
                if (sharingId != null) {
                    return getVideoSharing(sharingId);
                }
                return null;
            } catch (Exception e) {
                throw new JoynServiceException(e.getMessage());
            }
        }
        throw new JoynServiceNotAvailableException();
    }

    @Override
    public int getServiceVersion() throws JoynServiceException {
        if (this.api != null) {
            if (this.version == null) {
                try {
                    this.version = Integer.valueOf(this.api.getServiceVersion());
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
            try {
                boolean serviceStatus = this.api.isServiceRegistered();
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

    public void addNewVideoSharingListener(NewVideoSharingListener listener) throws JoynServiceException {
        if (this.api != null) {
            try {
                this.api.addNewVideoSharingListener(listener);
                return;
            } catch (Exception e) {
                throw new JoynServiceException(e.getMessage());
            }
        }
        throw new JoynServiceNotAvailableException();
    }

    public void removeNewVideoSharingListener(NewVideoSharingListener listener) throws JoynServiceException {
        if (this.api != null) {
            try {
                this.api.removeNewVideoSharingListener(listener);
                return;
            } catch (Exception e) {
                throw new JoynServiceException(e.getMessage());
            }
        }
        throw new JoynServiceNotAvailableException();
    }
}
