package org.gsma.joyn.ish;

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
import org.gsma.joyn.ish.IImageSharing;
import org.gsma.joyn.ish.IImageSharingService;

public class ImageSharingService extends JoynService {
    public static final String TAG = "ImageSharingService";
    private IImageSharingService api;
    private ServiceConnection apiConnection;

    public ImageSharingService(Context ctx, JoynServiceListener listener) {
        super(ctx, listener);
        this.api = null;
        this.apiConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName className, IBinder service) {
                ICoreServiceWrapper mCoreServiceWrapperBinder = ICoreServiceWrapper.Stub.asInterface(service);
                IBinder binder = null;
                try {
                    binder = mCoreServiceWrapperBinder.getImageSharingServiceBinder();
                } catch (RemoteException e1) {
                    e1.printStackTrace();
                }
                ImageSharingService.this.setApi(IImageSharingService.Stub.asInterface(binder));
                if (((JoynService) ImageSharingService.this).serviceListener != null) {
                    ((JoynService) ImageSharingService.this).serviceListener.onServiceConnected();
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName className) {
                ImageSharingService.this.setApi(null);
                if (((JoynService) ImageSharingService.this).serviceListener != null) {
                    ((JoynService) ImageSharingService.this).serviceListener.onServiceDisconnected(2);
                }
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
        this.api = (IImageSharingService) api;
    }

    public ImageSharingServiceConfiguration getConfiguration() throws JoynServiceException {
        if (this.api != null) {
            try {
                return this.api.getConfiguration();
            } catch (Exception e) {
                throw new JoynServiceException(e.getMessage());
            }
        }
        throw new JoynServiceNotAvailableException();
    }

    public ImageSharing shareImage(String contact, String filename, ImageSharingListener listener) throws JoynServiceException {
        if (this.api != null) {
            try {
                IImageSharing sharingIntf = this.api.shareImage(contact, filename, listener);
                if (sharingIntf != null) {
                    return new ImageSharing(sharingIntf);
                }
                return null;
            } catch (Exception e) {
                throw new JoynServiceException(e.getMessage());
            }
        }
        throw new JoynServiceNotAvailableException();
    }

    public Set<ImageSharing> getImageSharings() throws JoynServiceException {
        if (this.api != null) {
            try {
                Set<ImageSharing> result = new HashSet<>();
                List<IBinder> ishList = this.api.getImageSharings();
                for (IBinder binder : ishList) {
                    ImageSharing sharing = new ImageSharing(IImageSharing.Stub.asInterface(binder));
                    result.add(sharing);
                }
                return result;
            } catch (Exception e) {
                throw new JoynServiceException(e.getMessage());
            }
        }
        throw new JoynServiceNotAvailableException();
    }

    public ImageSharing getImageSharing(String sharingId) throws JoynServiceException {
        if (this.api != null) {
            try {
                IImageSharing sharingIntf = this.api.getImageSharing(sharingId);
                if (sharingIntf != null) {
                    return new ImageSharing(sharingIntf);
                }
                return null;
            } catch (Exception e) {
                throw new JoynServiceException(e.getMessage());
            }
        }
        throw new JoynServiceNotAvailableException();
    }

    public ImageSharing getImageSharingFor(Intent intent) throws JoynServiceException {
        if (this.api != null) {
            try {
                String sharingId = intent.getStringExtra("sharingId");
                if (sharingId != null) {
                    return getImageSharing(sharingId);
                }
                return null;
            } catch (Exception e) {
                throw new JoynServiceException(e.getMessage());
            }
        }
        throw new JoynServiceNotAvailableException();
    }

    public String getJoynAccountViaNumber(String number) throws JoynServiceException {
        if (this.api != null) {
            try {
                return this.api.getJoynAccountViaNumber(number);
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

    public void addNewImageSharingListener(NewImageSharingListener listener) throws JoynServiceException {
        if (this.api != null) {
            try {
                this.api.addNewImageSharingListener(listener);
                return;
            } catch (Exception e) {
                throw new JoynServiceException(e.getMessage());
            }
        }
        throw new JoynServiceNotAvailableException();
    }

    public void removeNewImageSharingListener(NewImageSharingListener listener) throws JoynServiceException {
        if (this.api != null) {
            try {
                this.api.removeNewImageSharingListener(listener);
                return;
            } catch (Exception e) {
                throw new JoynServiceException(e.getMessage());
            }
        }
        throw new JoynServiceNotAvailableException();
    }
}
