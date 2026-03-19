package com.android.se;

import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceSpecificException;
import android.se.omapi.ISecureElementChannel;
import android.se.omapi.ISecureElementListener;
import android.util.Log;
import com.android.se.SecureElementService;
import com.android.se.security.ChannelAccess;
import com.android.se.security.arf.ASN1;
import java.io.IOException;

public class Channel implements IBinder.DeathRecipient {
    private byte[] mAid;
    private IBinder mBinder;
    private final int mChannelNumber;
    private boolean mIsClosed;
    private byte[] mSelectResponse;
    private SecureElementService.SecureElementSession mSession;
    private Terminal mTerminal;
    private final String mTag = "SecureElement-Channel";
    private final Object mLock = new Object();
    private ChannelAccess mChannelAccess = null;
    private int mCallingPid = 0;

    Channel(SecureElementService.SecureElementSession secureElementSession, Terminal terminal, int i, byte[] bArr, byte[] bArr2, ISecureElementListener iSecureElementListener) {
        this.mBinder = null;
        this.mAid = null;
        if (terminal == null) {
            throw new IllegalArgumentException("Arguments can't be null");
        }
        this.mSession = secureElementSession;
        this.mTerminal = terminal;
        this.mIsClosed = false;
        this.mSelectResponse = bArr;
        this.mChannelNumber = i;
        this.mAid = bArr2;
        if (iSecureElementListener != null) {
            try {
                this.mBinder = iSecureElementListener.asBinder();
                this.mBinder.linkToDeath(this, 0);
            } catch (RemoteException e) {
                Log.e("SecureElement-Channel", "Failed to register client listener");
            }
        }
    }

    @Override
    public void binderDied() {
        try {
            Log.e("SecureElement-Channel", Thread.currentThread().getName() + " Client " + this.mBinder.toString() + " died");
            close();
        } catch (Exception e) {
        }
    }

    public synchronized void close() {
        synchronized (this.mLock) {
            if (isBasicChannel()) {
                Log.i("SecureElement-Channel", "Close basic channel - Select without AID ...");
                this.mTerminal.selectDefaultApplication();
            }
            this.mTerminal.closeChannel(this);
            this.mIsClosed = true;
            if (this.mBinder != null) {
                this.mBinder.unlinkToDeath(this, 0);
            }
            if (this.mSession != null) {
                this.mSession.removeChannel(this);
            }
        }
    }

    public byte[] transmit(byte[] bArr) throws IOException {
        byte[] bArrTransmit;
        if (isClosed()) {
            throw new IllegalStateException("Channel is closed");
        }
        if (bArr == null) {
            throw new NullPointerException("Command must not be null");
        }
        if (this.mChannelAccess == null) {
            throw new SecurityException("Channel access not set");
        }
        if (this.mChannelAccess.getCallingPid() != this.mCallingPid) {
            throw new SecurityException("Wrong Caller PID.");
        }
        CommandApduValidator.execute(bArr);
        if ((bArr[0] & ASN1.TAG_ContextSpecPrim0) == 0 && (bArr[0] & 96) != 32) {
            if (bArr[1] == 112) {
                throw new SecurityException("MANAGE CHANNEL command not allowed");
            }
            if (bArr[1] == -92 && bArr[2] == 4) {
                throw new SecurityException("SELECT by DF name command not allowed");
            }
        }
        checkCommand(bArr);
        synchronized (this.mLock) {
            bArr[0] = setChannelToClassByte(bArr[0], this.mChannelNumber);
            bArrTransmit = this.mTerminal.transmit(bArr);
        }
        return bArrTransmit;
    }

    private boolean selectNext() throws IOException {
        int i;
        if (isClosed()) {
            throw new IllegalStateException("Channel is closed");
        }
        if (this.mChannelAccess == null) {
            throw new IllegalStateException("Channel access not set.");
        }
        if (this.mChannelAccess.getCallingPid() != this.mCallingPid) {
            throw new SecurityException("Wrong Caller PID.");
        }
        if (this.mAid == null || this.mAid.length == 0) {
            throw new UnsupportedOperationException("No aid given");
        }
        byte[] bArr = new byte[this.mAid.length + 5];
        bArr[0] = 0;
        bArr[1] = ASN1.TAG_Certificate;
        bArr[2] = 4;
        bArr[3] = 2;
        bArr[4] = (byte) this.mAid.length;
        System.arraycopy(this.mAid, 0, bArr, 5, this.mAid.length);
        bArr[0] = setChannelToClassByte(bArr[0], this.mChannelNumber);
        byte[] bArrTransmit = this.mTerminal.transmit(bArr);
        if (bArrTransmit.length < 2) {
            throw new UnsupportedOperationException("Transmit failed");
        }
        int i2 = ((bArrTransmit[bArrTransmit.length - 2] & 255) << 8) | (bArrTransmit[bArrTransmit.length - 1] & 255);
        if ((61440 & i2) == 36864 || (i = i2 & 65280) == 25088 || i == 25344) {
            this.mSelectResponse = (byte[]) bArrTransmit.clone();
            return true;
        }
        if (i == 27136) {
            return false;
        }
        throw new UnsupportedOperationException("Unsupported operation");
    }

    private byte setChannelToClassByte(byte b, int i) {
        if (i < 4) {
            return (byte) ((b & 188) | i);
        }
        if (i < 20) {
            boolean z = (b & 12) != 0;
            byte b2 = (byte) ((b & 176) | 64 | (i - 4));
            if (z) {
                return (byte) (b2 | 32);
            }
            return b2;
        }
        throw new IllegalArgumentException("Channel number must be within [0..19]");
    }

    public ChannelAccess getChannelAccess() {
        return this.mChannelAccess;
    }

    public void setChannelAccess(ChannelAccess channelAccess) {
        this.mChannelAccess = channelAccess;
    }

    private void setCallingPid(int i) {
        this.mCallingPid = i;
    }

    private void checkCommand(byte[] bArr) {
        if (this.mTerminal.getAccessControlEnforcer() != null) {
            this.mTerminal.getAccessControlEnforcer().checkCommand(this, bArr);
            return;
        }
        throw new SecurityException("Access Controller not set for Terminal: " + this.mTerminal.getName());
    }

    public boolean hasSelectedAid() {
        return this.mAid != null;
    }

    public int getChannelNumber() {
        return this.mChannelNumber;
    }

    public byte[] getSelectResponse() {
        if (hasSelectedAid()) {
            return this.mSelectResponse;
        }
        return null;
    }

    public boolean isBasicChannel() {
        return this.mChannelNumber == 0;
    }

    public boolean isClosed() {
        return this.mIsClosed;
    }

    final class SecureElementChannel extends ISecureElementChannel.Stub {
        SecureElementChannel() {
        }

        public void close() throws RemoteException {
            Channel.this.close();
        }

        public boolean isClosed() throws RemoteException {
            return Channel.this.isClosed();
        }

        public boolean isBasicChannel() throws RemoteException {
            return Channel.this.isBasicChannel();
        }

        public byte[] getSelectResponse() throws RemoteException {
            return Channel.this.getSelectResponse();
        }

        public byte[] transmit(byte[] bArr) throws RemoteException, ServiceSpecificException {
            Channel.this.setCallingPid(Binder.getCallingPid());
            try {
                return Channel.this.transmit(bArr);
            } catch (IOException e) {
                throw new ServiceSpecificException(1, e.getMessage());
            }
        }

        public boolean selectNext() throws RemoteException, ServiceSpecificException {
            Channel.this.setCallingPid(Binder.getCallingPid());
            try {
                return Channel.this.selectNext();
            } catch (IOException e) {
                throw new ServiceSpecificException(1, e.getMessage());
            }
        }
    }
}
