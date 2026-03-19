package org.gsma.joyn.ft;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.IInterface;
import android.os.RemoteException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.gsma.joyn.ICoreServiceWrapper;
import org.gsma.joyn.JoynService;
import org.gsma.joyn.JoynServiceException;
import org.gsma.joyn.JoynServiceListener;
import org.gsma.joyn.JoynServiceNotAvailableException;
import org.gsma.joyn.Logger;
import org.gsma.joyn.ft.FileTransfer;
import org.gsma.joyn.ft.IFileTransfer;
import org.gsma.joyn.ft.IFileTransferService;

public class FileTransferService extends JoynService {
    public static final String TAG = "TAPI-FileTransferService";
    private IFileTransferService api;
    private ServiceConnection apiConnection;

    public FileTransferService(Context ctx, JoynServiceListener listener) {
        super(ctx, listener);
        this.api = null;
        this.apiConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName className, IBinder service) {
                Logger.i(FileTransferService.TAG, "onServiceConnected entry " + className);
                ICoreServiceWrapper mCoreServiceWrapperBinder = ICoreServiceWrapper.Stub.asInterface(service);
                IBinder binder = null;
                try {
                    binder = mCoreServiceWrapperBinder.getFileTransferServiceBinder();
                } catch (RemoteException e1) {
                    e1.printStackTrace();
                }
                FileTransferService.this.setApi(IFileTransferService.Stub.asInterface(binder));
                if (((JoynService) FileTransferService.this).serviceListener != null) {
                    ((JoynService) FileTransferService.this).serviceListener.onServiceConnected();
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName className) {
                Logger.i(FileTransferService.TAG, "onServiceDisconnected entry " + className);
                FileTransferService.this.setApi(null);
                if (((JoynService) FileTransferService.this).serviceListener != null) {
                    ((JoynService) FileTransferService.this).serviceListener.onServiceDisconnected(2);
                }
            }
        };
    }

    @Override
    public void connect() {
        Logger.i(TAG, "FileTransfer connected() entry");
        Intent intent = new Intent();
        ComponentName cmp = new ComponentName("com.orangelabs.rcs", "com.orangelabs.rcs.service.RcsCoreService");
        intent.setComponent(cmp);
        intent.setAction(IFileTransferService.class.getName());
        this.ctx.bindService(intent, this.apiConnection, 0);
    }

    @Override
    public void disconnect() {
        try {
            Logger.i(TAG, "FileTransfer disconnect() entry");
            this.ctx.unbindService(this.apiConnection);
        } catch (IllegalArgumentException e) {
        }
    }

    @Override
    protected void setApi(IInterface api) {
        super.setApi(api);
        this.api = (IFileTransferService) api;
    }

    public FileTransferServiceConfiguration getConfiguration() throws JoynServiceException {
        Logger.i(TAG, "getConfiguration() entry " + this.api);
        if (this.api != null) {
            try {
                return this.api.getConfiguration();
            } catch (Exception e) {
                throw new JoynServiceException(e.getMessage());
            }
        }
        throw new JoynServiceNotAvailableException();
    }

    public FileTransfer transferFile(String contact, String filename, FileTransferListener listener) throws JoynServiceException {
        return transferFile(contact, filename, null, listener);
    }

    public FileTransfer transferFile(String contact, String filename, String fileicon, FileTransferListener listener) throws JoynServiceException {
        Logger.i(TAG, "transferFile() entry contact= " + contact + " filename=" + filename + " fileicon = " + fileicon + " listener=" + listener);
        if (this.api == null) {
            throw new JoynServiceNotAvailableException();
        }
        try {
            IFileTransfer ftIntf = this.api.transferFile(contact, filename, fileicon, listener);
            if (ftIntf != null) {
                return new FileTransfer(ftIntf);
            }
            return null;
        } catch (Exception e) {
            throw new JoynServiceException(e.getMessage());
        }
    }

    public FileTransfer transferFile(String contact, String filename, String fileicon, int duration, FileTransferListener listener) throws JoynServiceException {
        return transferFile(contact, filename, fileicon, duration, FileTransfer.Type.NORMAL, listener);
    }

    public FileTransfer transferFileToMultiple(List<String> contacts, String filename, String fileicon, int duration, FileTransferListener listener) throws JoynServiceException {
        return transferFileToMultiple(contacts, filename, fileicon, duration, FileTransfer.Type.NORMAL, listener);
    }

    public FileTransfer transferFile(String contact, String filename, String fileicon, int duration, String type, FileTransferListener listener) throws JoynServiceException {
        Logger.i(TAG, "transferFile() entry contact= " + contact + " filename=" + filename + " fileicon = " + fileicon + " listener=" + listener);
        if (this.api == null) {
            throw new JoynServiceNotAvailableException();
        }
        try {
            IFileTransfer ftIntf = this.api.transferFileEx(contact, filename, fileicon, duration, type, listener);
            if (ftIntf != null) {
                return new FileTransfer(ftIntf);
            }
            return null;
        } catch (Exception e) {
            throw new JoynServiceException(e.getMessage());
        }
    }

    public FileTransfer transferFileToMultiple(List<String> contacts, String filename, String fileicon, int duration, String type, FileTransferListener listener) throws JoynServiceException {
        Logger.i(TAG, "transferFile() entry contact= " + contacts + " filename=" + filename + " fileicon = " + fileicon + " listener=" + listener);
        if (this.api == null) {
            throw new JoynServiceNotAvailableException();
        }
        try {
            IFileTransfer ftIntf = this.api.transferFileToMultiple(contacts, filename, fileicon, duration, type, listener);
            if (ftIntf != null) {
                return new FileTransfer(ftIntf);
            }
            return null;
        } catch (Exception e) {
            throw new JoynServiceException(e.getMessage());
        }
    }

    public FileTransfer transferFileToSecondaryDevice(String filename, String fileicon, int duration, FileTransferListener listener) throws JoynServiceException {
        return transferFileToSecondaryDevice(filename, fileicon, duration, FileTransfer.Type.NORMAL, listener);
    }

    public FileTransfer transferFileToSecondaryDevice(String filename, String fileicon, int duration, String type, FileTransferListener listener) throws JoynServiceException {
        Logger.i(TAG, "transferFile() entry filename =" + filename + " fileicon= " + fileicon + " type = " + type + " listener=" + listener);
        if (this.api == null) {
            throw new JoynServiceNotAvailableException();
        }
        try {
            IFileTransfer ftIntf = this.api.transferFileToSecondaryDevice(filename, fileicon, duration, type, listener);
            if (ftIntf != null) {
                return new FileTransfer(ftIntf);
            }
            return null;
        } catch (Exception e) {
            throw new JoynServiceException(e.getMessage());
        }
    }

    public FileTransfer resumeFileTransfer(String fileTranferId, FileTransferListener listener) throws JoynServiceException {
        Logger.i(TAG, "resumeFileTransfer() entry fileTranferId=" + fileTranferId);
        if (this.api != null) {
            try {
                IFileTransfer ftIntf = this.api.resumeFileTransfer(fileTranferId, listener);
                if (ftIntf != null) {
                    return new FileTransfer(ftIntf);
                }
                return null;
            } catch (Exception e) {
                throw new JoynServiceException(e.getMessage());
            }
        }
        throw new JoynServiceNotAvailableException();
    }

    public FileTransfer transferFileToGroup(String chatId, Set<String> contacts, String filename, int duration, FileTransferListener listener) throws JoynServiceException {
        return transferFileToGroup(chatId, contacts, filename, (String) null, duration, listener);
    }

    public FileTransfer transferFileToGroup(String chatId, Set<String> contacts, String filename, String fileicon, int duration, FileTransferListener listener) throws JoynServiceException {
        Logger.i(TAG, "transferFileToGroup() chatId=" + chatId + " filename=" + filename + " fileicon = " + fileicon + " timelen:" + duration + " listener=" + listener);
        if (this.api == null) {
            throw new JoynServiceNotAvailableException();
        }
        try {
            List<String> contactList = new ArrayList<>(contacts);
            IFileTransfer ftIntf = this.api.transferFileToGroup(chatId, contactList, filename, fileicon, duration, listener);
            if (ftIntf != null) {
                return new FileTransfer(ftIntf);
            }
            return null;
        } catch (Exception e) {
            throw new JoynServiceException(e.getMessage());
        }
    }

    public FileTransfer transferFileToGroup(String chatId, String filename, int duration, FileTransferListener listener) throws JoynServiceException {
        return transferFileToGroup(chatId, filename, (String) null, duration, FileTransfer.Type.NORMAL, listener);
    }

    public FileTransfer transferFileToGroup(String chatId, String filename, String fileicon, int duration, FileTransferListener listener) throws JoynServiceException {
        return transferFileToGroup(chatId, filename, fileicon, duration, FileTransfer.Type.NORMAL, listener);
    }

    public FileTransfer transferFileToGroup(String chatId, String filename, String fileicon, int duration, String type, FileTransferListener listener) throws JoynServiceException {
        Logger.i(TAG, "transferFileToGroup() chatId=" + chatId + " filename=" + filename + " fileicon = " + fileicon + " timelen:" + duration + " listener=" + listener);
        if (this.api == null) {
            throw new JoynServiceNotAvailableException();
        }
        try {
            IFileTransfer ftIntf = this.api.transferFileToGroupEx(chatId, filename, fileicon, duration, type, listener);
            if (ftIntf != null) {
                return new FileTransfer(ftIntf);
            }
            return null;
        } catch (Exception e) {
            throw new JoynServiceException(e.getMessage());
        }
    }

    public FileTransfer prosecuteFile(String contact, String transferId, FileTransferListener listener) throws JoynServiceException {
        Logger.i(TAG, "prosecuteFile() contact=" + contact + " transferId=" + transferId);
        if (this.api != null) {
            try {
                IFileTransfer ftIntf = this.api.prosecuteFile(contact, transferId, listener);
                if (ftIntf != null) {
                    return new FileTransfer(ftIntf);
                }
                return null;
            } catch (Exception e) {
                throw new JoynServiceException(e.getMessage());
            }
        }
        throw new JoynServiceNotAvailableException();
    }

    public Set<FileTransfer> getFileTransfers() throws JoynServiceException {
        Logger.i(TAG, "getFileTransfers() entry " + this.api);
        if (this.api != null) {
            try {
                Set<FileTransfer> result = new HashSet<>();
                List<IBinder> ftList = this.api.getFileTransfers();
                for (IBinder binder : ftList) {
                    FileTransfer ft = new FileTransfer(IFileTransfer.Stub.asInterface(binder));
                    result.add(ft);
                }
                return result;
            } catch (Exception e) {
                throw new JoynServiceException(e.getMessage());
            }
        }
        throw new JoynServiceNotAvailableException();
    }

    public FileTransfer getFileTransfer(String transferId) throws JoynServiceException {
        Logger.i(TAG, "getFileTransfer() entry " + transferId + " api=" + this.api);
        if (this.api != null) {
            try {
                IFileTransfer ftIntf = this.api.getFileTransfer(transferId);
                if (ftIntf != null) {
                    return new FileTransfer(ftIntf);
                }
                return null;
            } catch (Exception e) {
                throw new JoynServiceException(e.getMessage());
            }
        }
        throw new JoynServiceNotAvailableException();
    }

    public FileTransfer getFileTransferFor(Intent intent) throws JoynServiceException {
        Logger.i(TAG, "getFileTransferFor() entry " + intent + " api=" + this.api);
        if (this.api != null) {
            try {
                String transferId = intent.getStringExtra(FileTransferIntent.EXTRA_TRANSFER_ID);
                if (transferId != null) {
                    return getFileTransfer(transferId);
                }
                return null;
            } catch (Exception e) {
                throw new JoynServiceException(e.getMessage());
            }
        }
        throw new JoynServiceNotAvailableException();
    }

    public void addNewFileTransferListener(NewFileTransferListener listener) throws JoynServiceException {
        Logger.i(TAG, "addNewFileTransferListener() entry " + listener + " api=" + this.api);
        if (this.api != null) {
            try {
                this.api.addNewFileTransferListener(listener);
                return;
            } catch (Exception e) {
                throw new JoynServiceException(e.getMessage());
            }
        }
        throw new JoynServiceNotAvailableException();
    }

    public void removeNewFileTransferListener(NewFileTransferListener listener) throws JoynServiceException {
        Logger.i(TAG, "removeNewFileTransferListener() entry " + listener + " api=" + this.api);
        if (this.api != null) {
            try {
                this.api.removeNewFileTransferListener(listener);
                return;
            } catch (Exception e) {
                throw new JoynServiceException(e.getMessage());
            }
        }
        throw new JoynServiceNotAvailableException();
    }
}
