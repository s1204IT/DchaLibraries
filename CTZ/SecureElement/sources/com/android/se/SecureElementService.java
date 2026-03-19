package com.android.se;

import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.ServiceSpecificException;
import android.se.omapi.ISecureElementChannel;
import android.se.omapi.ISecureElementListener;
import android.se.omapi.ISecureElementReader;
import android.se.omapi.ISecureElementService;
import android.se.omapi.ISecureElementSession;
import android.util.Log;
import com.android.se.Channel.SecureElementChannel;
import com.android.se.Terminal;
import com.android.se.Terminal.SecureElementReader;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;

public final class SecureElementService extends Service {
    public static final String ESE_TERMINAL = "eSE";
    public static final String UICC_TERMINAL = "SIM";
    private final String mTag = "SecureElementService";
    private LinkedHashMap<String, Terminal> mTerminals = new LinkedHashMap<>();
    private final ISecureElementService.Stub mSecureElementServiceBinder = new ISecureElementService.Stub() {
        public String[] getReaders() throws RemoteException {
            return (String[]) SecureElementService.this.mTerminals.keySet().toArray(new String[SecureElementService.this.mTerminals.size()]);
        }

        public ISecureElementReader getReader(String str) throws RemoteException {
            Log.d("SecureElementService", "getReader() " + str);
            Terminal terminal = SecureElementService.this.getTerminal(str);
            Objects.requireNonNull(terminal);
            return terminal.new SecureElementReader(SecureElementService.this);
        }

        public synchronized boolean[] isNFCEventAllowed(String str, byte[] bArr, String[] strArr) throws RemoteException {
            if (bArr != null) {
                try {
                    if (bArr.length == 0) {
                        bArr = new byte[]{0, 0, 0, 0, 0};
                    }
                } catch (Throwable th) {
                    throw th;
                }
            } else {
                bArr = new byte[]{0, 0, 0, 0, 0};
            }
            if (bArr.length < 5 || bArr.length > 16) {
                throw new IllegalArgumentException("AID out of range");
            }
            if (strArr == null || strArr.length == 0) {
                throw new IllegalArgumentException("package names not specified");
            }
            return SecureElementService.this.getTerminal(str).isNfcEventAllowed(SecureElementService.this.getPackageManager(), bArr, strArr);
        }

        protected void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
            Iterator it = SecureElementService.this.mTerminals.values().iterator();
            while (it.hasNext()) {
                ((Terminal) it.next()).dump(printWriter);
            }
        }
    };

    private Terminal getTerminal(String str) {
        if (str == null) {
            throw new NullPointerException("reader must not be null");
        }
        if (str.equals(UICC_TERMINAL)) {
            str = "SIM1";
        }
        Terminal terminal = this.mTerminals.get(str);
        if (terminal == null) {
            throw new IllegalArgumentException("Reader: " + str + " doesn't exist");
        }
        return terminal;
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.i("SecureElementService", Thread.currentThread().getName() + " onBind");
        if (ISecureElementService.class.getName().equals(intent.getAction())) {
            return this.mSecureElementServiceBinder;
        }
        return null;
    }

    @Override
    public void onCreate() {
        Log.i("SecureElementService", Thread.currentThread().getName() + " onCreate");
        createTerminals();
        ServiceManager.addService("secure_element", this.mSecureElementServiceBinder);
    }

    @Override
    public void onDestroy() {
        Log.i("SecureElementService", "onDestroy");
        for (Terminal terminal : this.mTerminals.values()) {
            terminal.closeChannels();
            terminal.close();
        }
    }

    private void addTerminals(String str) {
        int i = 1;
        String str2 = null;
        while (true) {
            try {
                String str3 = str + Integer.toString(i);
                try {
                    Terminal terminal = new Terminal(str3, this);
                    terminal.initialize();
                    this.mTerminals.put(str3, terminal);
                    i++;
                    if (i > 0) {
                        str2 = str3;
                    } else {
                        return;
                    }
                } catch (RemoteException | RuntimeException e) {
                    str2 = str3;
                    Log.e("SecureElementService", "Error in getService() for " + str2);
                    return;
                } catch (NoSuchElementException e2) {
                    str2 = str3;
                    Log.i("SecureElementService", "No HAL implementation for " + str2);
                    return;
                }
            } catch (RemoteException | RuntimeException e3) {
            } catch (NoSuchElementException e4) {
            }
        }
    }

    private void createTerminals() {
        addTerminals(ESE_TERMINAL);
        addTerminals(UICC_TERMINAL);
    }

    private String getPackageNameFromCallingUid(int i) {
        String[] packagesForUid;
        PackageManager packageManager = getPackageManager();
        if (packageManager != null && (packagesForUid = packageManager.getPackagesForUid(i)) != null && packagesForUid.length > 0) {
            return packagesForUid[0];
        }
        throw new AccessControlException("PackageName can not be determined");
    }

    final class SecureElementSession extends ISecureElementSession.Stub {
        private byte[] mAtr;
        private boolean mIsClosed;
        private final Terminal.SecureElementReader mReader;
        private final List<Channel> mChannels = new ArrayList();
        private final Object mLock = new Object();

        SecureElementSession(Terminal.SecureElementReader secureElementReader) {
            if (secureElementReader == null) {
                throw new NullPointerException("SecureElementReader cannot be null");
            }
            this.mReader = secureElementReader;
            this.mAtr = this.mReader.getAtr();
            this.mIsClosed = false;
        }

        public ISecureElementReader getReader() throws RemoteException {
            return this.mReader;
        }

        public byte[] getAtr() throws RemoteException {
            return this.mAtr;
        }

        public void close() throws RemoteException {
            closeChannels();
            this.mReader.removeSession(this);
            synchronized (this.mLock) {
                this.mIsClosed = true;
            }
        }

        void removeChannel(Channel channel) {
            synchronized (this.mLock) {
                if (this.mChannels != null) {
                    this.mChannels.remove(channel);
                }
            }
        }

        public void closeChannels() throws RemoteException {
            synchronized (this.mLock) {
                while (this.mChannels.size() > 0) {
                    try {
                        this.mChannels.get(0).close();
                    } catch (Exception e) {
                        Log.e("SecureElementService", "SecureElementSession Channel - close Exception " + e.getMessage());
                    }
                }
            }
        }

        public boolean isClosed() throws RemoteException {
            boolean z;
            synchronized (this.mLock) {
                z = this.mIsClosed;
            }
            return z;
        }

        public ISecureElementChannel openBasicChannel(byte[] bArr, byte b, ISecureElementListener iSecureElementListener) throws RemoteException, ServiceSpecificException {
            if (isClosed()) {
                throw new IllegalStateException("Session is closed");
            }
            if (iSecureElementListener == null) {
                throw new NullPointerException("listener must not be null");
            }
            if (this.mReader.getTerminal().getName().startsWith(SecureElementService.UICC_TERMINAL)) {
                return null;
            }
            if (b != 0 && b != 4 && b != 8 && b != 12) {
                throw new UnsupportedOperationException("p2 not supported: " + String.format("%02x ", Integer.valueOf(b & 255)));
            }
            try {
                Channel channelOpenBasicChannel = this.mReader.getTerminal().openBasicChannel(this, bArr, b, iSecureElementListener, SecureElementService.this.getPackageNameFromCallingUid(Binder.getCallingUid()), Binder.getCallingPid());
                if (channelOpenBasicChannel == null) {
                    Log.i("SecureElementService", "OpenBasicChannel() - returning null");
                    return null;
                }
                Log.i("SecureElementService", "Open basic channel success. Channel: " + channelOpenBasicChannel.getChannelNumber());
                this.mChannels.add(channelOpenBasicChannel);
                Objects.requireNonNull(channelOpenBasicChannel);
                return channelOpenBasicChannel.new SecureElementChannel();
            } catch (IOException e) {
                throw new ServiceSpecificException(1, e.getMessage());
            } catch (NoSuchElementException e2) {
                throw new ServiceSpecificException(2, e2.getMessage());
            }
        }

        public ISecureElementChannel openLogicalChannel(byte[] bArr, byte b, ISecureElementListener iSecureElementListener) throws RemoteException, ServiceSpecificException {
            if (isClosed()) {
                throw new IllegalStateException("Session is closed");
            }
            if (iSecureElementListener == null) {
                throw new NullPointerException("listener must not be null");
            }
            if ((bArr == null || bArr.length == 0) && this.mReader.getTerminal().getName().startsWith(SecureElementService.UICC_TERMINAL)) {
                return null;
            }
            if (b != 0 && b != 4 && b != 8 && b != 12) {
                throw new UnsupportedOperationException("p2 not supported: " + String.format("%02x ", Integer.valueOf(b & 255)));
            }
            try {
                Channel channelOpenLogicalChannel = this.mReader.getTerminal().openLogicalChannel(this, bArr, b, iSecureElementListener, SecureElementService.this.getPackageNameFromCallingUid(Binder.getCallingUid()), Binder.getCallingPid());
                if (channelOpenLogicalChannel == null) {
                    Log.i("SecureElementService", "openLogicalChannel() - returning null");
                    return null;
                }
                Log.i("SecureElementService", "openLogicalChannel() Success. Channel: " + channelOpenLogicalChannel.getChannelNumber());
                this.mChannels.add(channelOpenLogicalChannel);
                Objects.requireNonNull(channelOpenLogicalChannel);
                return channelOpenLogicalChannel.new SecureElementChannel();
            } catch (IOException e) {
                throw new ServiceSpecificException(1, e.getMessage());
            } catch (NoSuchElementException e2) {
                throw new ServiceSpecificException(2, e2.getMessage());
            }
        }
    }
}
