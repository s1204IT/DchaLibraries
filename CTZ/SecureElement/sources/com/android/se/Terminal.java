package com.android.se;

import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.secure_element.V1_0.ISecureElement;
import android.hardware.secure_element.V1_0.ISecureElementHalCallback;
import android.hardware.secure_element.V1_0.LogicalChannelResponse;
import android.os.Build;
import android.os.Handler;
import android.os.IHwBinder;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceSpecificException;
import android.se.omapi.ISecureElementListener;
import android.se.omapi.ISecureElementReader;
import android.se.omapi.ISecureElementSession;
import android.util.Log;
import com.android.se.SecureElementService;
import com.android.se.SecureElementService.SecureElementSession;
import com.android.se.internal.ByteArrayConverter;
import com.android.se.security.AccessControlEnforcer;
import com.android.se.security.ChannelAccess;
import com.android.se.security.arf.ASN1;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.NoSuchElementException;
import java.util.Objects;

public class Terminal {
    private static final boolean DEBUG = Build.IS_DEBUGGABLE;
    private static final int EVENT_GET_HAL = 1;
    private static final int GET_SERVICE_DELAY_MILLIS = 4000;
    private AccessControlEnforcer mAccessControlEnforcer;
    private Context mContext;
    private final String mName;
    private ISecureElement mSEHal;
    private final Map<Integer, Channel> mChannels = new HashMap();
    private final Object mLock = new Object();
    public boolean mIsConnected = false;
    private boolean mDefaultApplicationSelectedOnBasicChannel = true;
    private ISecureElementHalCallback.Stub mHalCallback = new ISecureElementHalCallback.Stub() {
        @Override
        public void onStateChange(boolean z) {
            synchronized (Terminal.this.mLock) {
                Log.i(Terminal.this.mTag, "OnStateChange:" + z);
                Terminal.this.mIsConnected = z;
                if (!z) {
                    if (Terminal.this.mAccessControlEnforcer != null) {
                        Terminal.this.mAccessControlEnforcer.reset();
                    }
                } else {
                    Terminal.this.closeChannels();
                    try {
                        Terminal.this.initializeAccessControl();
                    } catch (Exception e) {
                    }
                    Terminal.this.mDefaultApplicationSelectedOnBasicChannel = true;
                }
            }
        }
    };
    private IHwBinder.DeathRecipient mDeathRecipient = new SecureElementDeathRecipient();
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message message) {
            if (message.what == 1) {
                try {
                    Terminal.this.initialize();
                } catch (Exception e) {
                    Log.e(Terminal.this.mTag, Terminal.this.mName + " could not be initialized again");
                    sendMessageDelayed(obtainMessage(1, 0), 4000L);
                }
            }
        }
    };
    private final String mTag = "SecureElement-Terminal-" + getName();

    class SecureElementDeathRecipient implements IHwBinder.DeathRecipient {
        SecureElementDeathRecipient() {
        }

        @Override
        public void serviceDied(long j) {
            Log.e(Terminal.this.mTag, Terminal.this.mName + " died");
            synchronized (Terminal.this.mLock) {
                Terminal.this.mIsConnected = false;
                if (Terminal.this.mAccessControlEnforcer != null) {
                    Terminal.this.mAccessControlEnforcer.reset();
                }
            }
            Terminal.this.mHandler.sendMessageDelayed(Terminal.this.mHandler.obtainMessage(1, 0), 4000L);
        }
    }

    public Terminal(String str, Context context) {
        this.mContext = context;
        this.mName = str;
    }

    public void initialize() throws RemoteException, NoSuchElementException {
        synchronized (this.mLock) {
            this.mSEHal = ISecureElement.getService(this.mName);
            if (this.mSEHal == null) {
                throw new NoSuchElementException("No HAL is provided for " + this.mName);
            }
            this.mSEHal.init(this.mHalCallback);
            this.mSEHal.linkToDeath(this.mDeathRecipient, 0L);
        }
        Log.i(this.mTag, this.mName + " was initialized");
    }

    private ArrayList<Byte> byteArrayToArrayList(byte[] bArr) {
        ArrayList<Byte> arrayList = new ArrayList<>();
        if (bArr == null) {
            return arrayList;
        }
        for (byte b : bArr) {
            arrayList.add(Byte.valueOf(b));
        }
        return arrayList;
    }

    private byte[] arrayListToByteArray(ArrayList<Byte> arrayList) {
        Byte[] bArr = (Byte[]) arrayList.toArray(new Byte[arrayList.size()]);
        byte[] bArr2 = new byte[arrayList.size()];
        int length = bArr.length;
        int i = 0;
        int i2 = 0;
        while (i < length) {
            bArr2[i2] = bArr[i].byteValue();
            i++;
            i2++;
        }
        return bArr2;
    }

    public void closeChannel(Channel channel) {
        if (channel == null) {
            return;
        }
        if (this.mIsConnected) {
            try {
                byte bCloseChannel = this.mSEHal.closeChannel((byte) channel.getChannelNumber());
                if (!channel.isBasicChannel() && bCloseChannel != 0) {
                    Log.e(this.mTag, "Error closing channel " + channel.getChannelNumber());
                }
            } catch (RemoteException e) {
                Log.e(this.mTag, "Exception in closeChannel() " + e);
            }
        }
        synchronized (this.mLock) {
            this.mChannels.remove(Integer.valueOf(channel.getChannelNumber()), channel);
            if (this.mChannels.get(Integer.valueOf(channel.getChannelNumber())) != null) {
                Log.e(this.mTag, "Removing channel failed");
            }
        }
    }

    public synchronized void closeChannels() {
        Collection<Channel> collectionValues = this.mChannels.values();
        for (Channel channel : (Channel[]) collectionValues.toArray(new Channel[collectionValues.size()])) {
            channel.close();
        }
    }

    public void close() {
        synchronized (this.mLock) {
            if (this.mSEHal != null) {
                try {
                    this.mSEHal.unlinkToDeath(this.mDeathRecipient);
                } catch (RemoteException e) {
                }
            }
        }
    }

    public String getName() {
        return this.mName;
    }

    public byte[] getAtr() {
        if (!this.mIsConnected) {
            return null;
        }
        try {
            ArrayList<Byte> atr = this.mSEHal.getAtr();
            if (atr.isEmpty()) {
                return null;
            }
            return arrayListToByteArray(atr);
        } catch (RemoteException e) {
            Log.e(this.mTag, "Exception in getAtr()" + e);
            return null;
        }
    }

    public void selectDefaultApplication() {
        try {
            select(null);
        } catch (NoSuchElementException e) {
            if (getAccessControlEnforcer() != null) {
                try {
                    AccessControlEnforcer accessControlEnforcer = this.mAccessControlEnforcer;
                    select(AccessControlEnforcer.getDefaultAccessControlAid());
                } catch (Exception e2) {
                }
            }
        } catch (Exception e3) {
        }
    }

    private void select(byte[] bArr) throws IOException {
        byte[] bArr2 = new byte[(bArr == null ? 0 : bArr.length) + 5];
        bArr2[0] = 0;
        bArr2[1] = ASN1.TAG_Certificate;
        bArr2[2] = 4;
        bArr2[3] = 0;
        if (bArr != null && bArr.length != 0) {
            bArr2[4] = (byte) bArr.length;
            System.arraycopy(bArr, 0, bArr2, 5, bArr.length);
        } else {
            bArr2[4] = 0;
        }
        byte[] bArrTransmit = transmit(bArr2);
        if (bArrTransmit.length < 2) {
            throw new NoSuchElementException("Response length is too small");
        }
        int i = bArrTransmit[bArrTransmit.length - 2] & 255;
        int i2 = bArrTransmit[bArrTransmit.length - 1] & 255;
        if (i != 144 || i2 != 0) {
            throw new NoSuchElementException("Status word is incorrect");
        }
    }

    public Channel openBasicChannel(SecureElementService.SecureElementSession secureElementSession, byte[] bArr, byte b, ISecureElementListener iSecureElementListener, String str, int i) throws IOException, NoSuchElementException {
        byte[] bArr2;
        if (bArr == null || bArr.length != 0) {
            if (bArr != null && (bArr.length < 5 || bArr.length > 16)) {
                throw new IllegalArgumentException("AID out of range");
            }
            if (!this.mIsConnected) {
                throw new IOException("Secure Element is not connected");
            }
            bArr2 = bArr;
        } else {
            bArr2 = null;
        }
        Log.w(this.mTag, "Enable access control on basic channel for " + str);
        try {
            ChannelAccess upChannelAccess = setUpChannelAccess(bArr2, str, i);
            synchronized (this.mLock) {
                if (this.mChannels.get(0) != null) {
                    Log.e(this.mTag, "basic channel in use");
                    return null;
                }
                if (bArr2 == null && !this.mDefaultApplicationSelectedOnBasicChannel) {
                    Log.e(this.mTag, "default application is not selected");
                    return null;
                }
                final ArrayList arrayList = new ArrayList();
                final byte[] bArr3 = new byte[1];
                try {
                    this.mSEHal.openBasicChannel(byteArrayToArrayList(bArr2), b, new ISecureElement.openBasicChannelCallback() {
                        @Override
                        public void onValues(ArrayList<Byte> arrayList2, byte b2) {
                            bArr3[0] = b2;
                            arrayList.add(Terminal.this.arrayListToByteArray(arrayList2));
                        }
                    });
                    byte[] bArr4 = (byte[]) arrayList.get(0);
                    if (bArr3[0] == 2) {
                        return null;
                    }
                    if (bArr3[0] != 4) {
                        if (bArr3[0] == 5) {
                            throw new IOException("OpenBasicChannel() failed");
                        }
                        if (bArr3[0] == 3) {
                            throw new NoSuchElementException("OpenBasicChannel() failed");
                        }
                        Channel channel = new Channel(secureElementSession, this, 0, bArr4, bArr2, iSecureElementListener);
                        channel.setChannelAccess(upChannelAccess);
                        if (bArr2 != null) {
                            this.mDefaultApplicationSelectedOnBasicChannel = false;
                        }
                        this.mChannels.put(0, channel);
                        return channel;
                    }
                    throw new UnsupportedOperationException("OpenBasicChannel() failed");
                } catch (RemoteException e) {
                    throw new IOException(e.getMessage());
                }
            }
        } catch (MissingResourceException e2) {
            return null;
        }
    }

    public Channel openLogicalChannelWithoutChannelAccess(byte[] bArr) throws IOException, NoSuchElementException {
        return openLogicalChannel(null, bArr, (byte) 0, null, null, 0);
    }

    public Channel openLogicalChannel(SecureElementService.SecureElementSession secureElementSession, byte[] bArr, byte b, ISecureElementListener iSecureElementListener, String str, int i) throws IOException, NoSuchElementException {
        byte[] bArr2;
        ChannelAccess upChannelAccess;
        if (bArr == null || bArr.length != 0) {
            if (bArr != null && (bArr.length < 5 || bArr.length > 16)) {
                throw new IllegalArgumentException("AID out of range");
            }
            if (!this.mIsConnected) {
                throw new IOException("Secure Element is not connected");
            }
            bArr2 = bArr;
        } else {
            bArr2 = null;
        }
        if (str != null) {
            Log.w(this.mTag, "Enable access control on logical channel for " + str);
            try {
                upChannelAccess = setUpChannelAccess(bArr2, str, i);
            } catch (MissingResourceException e) {
                return null;
            }
        } else {
            upChannelAccess = null;
        }
        synchronized (this.mLock) {
            final LogicalChannelResponse[] logicalChannelResponseArr = new LogicalChannelResponse[1];
            final byte[] bArr3 = new byte[1];
            try {
                this.mSEHal.openLogicalChannel(byteArrayToArrayList(bArr2), b, new ISecureElement.openLogicalChannelCallback() {
                    @Override
                    public void onValues(LogicalChannelResponse logicalChannelResponse, byte b2) {
                        bArr3[0] = b2;
                        logicalChannelResponseArr[0] = logicalChannelResponse;
                    }
                });
                if (bArr3[0] == 2) {
                    return null;
                }
                if (bArr3[0] != 4) {
                    if (bArr3[0] == 5) {
                        throw new IOException("OpenLogicalChannel() failed");
                    }
                    if (bArr3[0] == 3) {
                        throw new NoSuchElementException("OpenLogicalChannel() failed");
                    }
                    if (logicalChannelResponseArr[0].channelNumber > 0 && bArr3[0] == 0) {
                        byte b2 = logicalChannelResponseArr[0].channelNumber;
                        Channel channel = new Channel(secureElementSession, this, b2, arrayListToByteArray(logicalChannelResponseArr[0].selectResponse), bArr2, iSecureElementListener);
                        channel.setChannelAccess(upChannelAccess);
                        this.mChannels.put(Integer.valueOf(b2), channel);
                        return channel;
                    }
                    return null;
                }
                throw new UnsupportedOperationException("OpenLogicalChannel() failed");
            } catch (RemoteException e2) {
                throw new IOException(e2.getMessage());
            }
        }
    }

    public boolean isAidSelectable(byte[] bArr) {
        if (bArr == null) {
            throw new NullPointerException("aid must not be null");
        }
        if (!this.mIsConnected) {
            Log.e(this.mTag, "Secure Element is not connected");
            return false;
        }
        synchronized (this.mLock) {
            final LogicalChannelResponse[] logicalChannelResponseArr = new LogicalChannelResponse[1];
            final byte[] bArr2 = new byte[1];
            try {
                this.mSEHal.openLogicalChannel(byteArrayToArrayList(bArr), (byte) 0, new ISecureElement.openLogicalChannelCallback() {
                    @Override
                    public void onValues(LogicalChannelResponse logicalChannelResponse, byte b) {
                        bArr2[0] = b;
                        logicalChannelResponseArr[0] = logicalChannelResponse;
                    }
                });
                if (bArr2[0] != 0) {
                    return false;
                }
                this.mSEHal.closeChannel(logicalChannelResponseArr[0].channelNumber);
                return true;
            } catch (RemoteException e) {
                Log.e(this.mTag, "Error in isAidSelectable() returning false" + e);
                return false;
            }
        }
    }

    public byte[] transmit(byte[] bArr) throws IOException {
        if (!this.mIsConnected) {
            Log.e(this.mTag, "Secure Element is not connected");
            throw new IOException("Secure Element is not connected");
        }
        byte[] bArrTransmitInternal = transmitInternal(bArr);
        int i = bArrTransmitInternal[bArrTransmitInternal.length - 2] & 255;
        int i2 = bArrTransmitInternal[bArrTransmitInternal.length - 1] & 255;
        if (i == 108) {
            bArr[bArr.length - 1] = bArrTransmitInternal[bArrTransmitInternal.length - 1];
            return transmitInternal(bArr);
        }
        if (i != 97) {
            return bArrTransmitInternal;
        }
        while (true) {
            byte[] bArrTransmitInternal2 = transmitInternal(new byte[]{bArr[0], -64, 0, 0, (byte) i2});
            byte[] bArr2 = new byte[(bArrTransmitInternal.length + bArrTransmitInternal2.length) - 2];
            System.arraycopy(bArrTransmitInternal, 0, bArr2, 0, bArrTransmitInternal.length - 2);
            System.arraycopy(bArrTransmitInternal2, 0, bArr2, bArrTransmitInternal.length - 2, bArrTransmitInternal2.length);
            int i3 = bArr2[bArr2.length - 2] & 255;
            int i4 = bArr2[bArr2.length - 1] & 255;
            if (i3 == 97) {
                bArrTransmitInternal = bArr2;
                i2 = i4;
            } else {
                return bArr2;
            }
        }
    }

    private byte[] transmitInternal(byte[] bArr) throws IOException {
        try {
            ArrayList<Byte> arrayListTransmit = this.mSEHal.transmit(byteArrayToArrayList(bArr));
            if (arrayListTransmit.isEmpty()) {
                throw new IOException("Error in transmit()");
            }
            byte[] bArrArrayListToByteArray = arrayListToByteArray(arrayListTransmit);
            if (DEBUG) {
                Log.i(this.mTag, "Sent : " + ByteArrayConverter.byteArrayToHexString(bArr));
                Log.i(this.mTag, "Received : " + ByteArrayConverter.byteArrayToHexString(bArrArrayListToByteArray));
            }
            return bArrArrayListToByteArray;
        } catch (RemoteException e) {
            throw new IOException(e.getMessage());
        }
    }

    public boolean[] isNfcEventAllowed(PackageManager packageManager, byte[] bArr, String[] strArr) {
        boolean z;
        boolean[] zArrIsNfcEventAllowed;
        if (this.mAccessControlEnforcer == null) {
            try {
                initializeAccessControl();
                z = false;
            } catch (Exception e) {
                Log.i(this.mTag, "isNfcEventAllowed Exception: " + e.getMessage());
                return null;
            }
        } else {
            z = true;
        }
        this.mAccessControlEnforcer.setPackageManager(packageManager);
        synchronized (this.mLock) {
            try {
                try {
                    zArrIsNfcEventAllowed = this.mAccessControlEnforcer.isNfcEventAllowed(bArr, strArr, z);
                } catch (Exception e2) {
                    Log.i(this.mTag, "isNfcEventAllowed Exception: " + e2.getMessage());
                    return null;
                }
            } catch (Throwable th) {
                throw th;
            }
        }
        return zArrIsNfcEventAllowed;
    }

    public boolean isSecureElementPresent() {
        try {
            return this.mSEHal.isCardPresent();
        } catch (RemoteException e) {
            Log.e(this.mTag, "Error in isSecureElementPresent() " + e);
            return false;
        }
    }

    private ChannelAccess setUpChannelAccess(byte[] bArr, String str, int i) throws MissingResourceException, IOException {
        boolean z;
        ChannelAccess upChannelAccess;
        if (this.mAccessControlEnforcer == null) {
            initializeAccessControl();
            z = false;
        } else {
            z = true;
        }
        this.mAccessControlEnforcer.setPackageManager(this.mContext.getPackageManager());
        synchronized (this.mLock) {
            try {
                try {
                    try {
                        upChannelAccess = this.mAccessControlEnforcer.setUpChannelAccess(bArr, str, z);
                        upChannelAccess.setCallingPid(i);
                    } catch (IOException | MissingResourceException e) {
                        throw e;
                    }
                } catch (Exception e2) {
                    throw new SecurityException("Exception in setUpChannelAccess()" + e2);
                }
            } catch (Throwable th) {
                throw th;
            }
        }
        return upChannelAccess;
    }

    private synchronized void initializeAccessControl() throws MissingResourceException, IOException {
        synchronized (this.mLock) {
            if (this.mAccessControlEnforcer == null) {
                this.mAccessControlEnforcer = new AccessControlEnforcer(this);
            }
            try {
                this.mAccessControlEnforcer.initialize();
            } catch (IOException | MissingResourceException e) {
                this.mAccessControlEnforcer = null;
                throw e;
            }
        }
    }

    public AccessControlEnforcer getAccessControlEnforcer() {
        return this.mAccessControlEnforcer;
    }

    public void dump(PrintWriter printWriter) {
        printWriter.println("SECURE ELEMENT SERVICE TERMINAL: " + this.mName);
        printWriter.println();
        printWriter.println("mIsConnected:" + this.mIsConnected);
        printWriter.println();
        printWriter.println("List of open channels:");
        for (Channel channel : this.mChannels.values()) {
            printWriter.println("channel " + channel.getChannelNumber() + ": ");
            StringBuilder sb = new StringBuilder();
            sb.append("package: ");
            sb.append(channel.getChannelAccess().getPackageName());
            printWriter.println(sb.toString());
            printWriter.println("pid: " + channel.getChannelAccess().getCallingPid());
            printWriter.println("aid selected: " + channel.hasSelectedAid());
            printWriter.println("basic channel: " + channel.isBasicChannel());
            printWriter.println();
        }
        printWriter.println();
        if (this.mAccessControlEnforcer != null) {
            this.mAccessControlEnforcer.dump(printWriter);
        }
    }

    final class SecureElementReader extends ISecureElementReader.Stub {
        private final SecureElementService mService;
        private final ArrayList<SecureElementService.SecureElementSession> mSessions = new ArrayList<>();

        SecureElementReader(SecureElementService secureElementService) {
            this.mService = secureElementService;
        }

        public byte[] getAtr() {
            return Terminal.this.getAtr();
        }

        public boolean isSecureElementPresent() throws RemoteException {
            return Terminal.this.isSecureElementPresent();
        }

        public void closeSessions() {
            synchronized (Terminal.this.mLock) {
                while (this.mSessions.size() > 0) {
                    try {
                        this.mSessions.get(0).close();
                    } catch (Exception e) {
                    }
                }
                this.mSessions.clear();
            }
        }

        public void removeSession(SecureElementService.SecureElementSession secureElementSession) {
            if (secureElementSession == null) {
                throw new NullPointerException("session is null");
            }
            this.mSessions.remove(secureElementSession);
            synchronized (Terminal.this.mLock) {
                if (this.mSessions.size() == 0) {
                    Terminal.this.mDefaultApplicationSelectedOnBasicChannel = true;
                }
            }
        }

        public ISecureElementSession openSession() throws RemoteException, ServiceSpecificException {
            SecureElementService.SecureElementSession secureElementSession;
            if (isSecureElementPresent()) {
                synchronized (Terminal.this.mLock) {
                    SecureElementService secureElementService = this.mService;
                    Objects.requireNonNull(secureElementService);
                    secureElementSession = secureElementService.new SecureElementSession(this);
                    this.mSessions.add(secureElementSession);
                }
                return secureElementSession;
            }
            throw new ServiceSpecificException(1, "Secure Element is not present.");
        }

        Terminal getTerminal() {
            return Terminal.this;
        }
    }
}
