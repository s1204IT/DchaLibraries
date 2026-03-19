package com.mediatek.internal.telephony.cat;

import android.net.Network;
import com.mediatek.internal.telephony.cat.Channel;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;

class TcpChannel extends Channel {
    private static final int TCP_CONN_TIMEOUT = 15000;
    DataInputStream mInput;
    BufferedOutputStream mOutput;
    Socket mSocket;
    Thread rt;

    TcpChannel(int i, int i2, int i3, InetAddress inetAddress, int i4, int i5, MtkCatService mtkCatService, BipService bipService) {
        super(i, i2, i3, inetAddress, i4, i5, mtkCatService, bipService);
        this.mSocket = null;
        this.mInput = null;
        this.mOutput = null;
    }

    @Override
    public int openChannel(BipCmdMessage bipCmdMessage, Network network) {
        this.mNetwork = network;
        if (this.mLinkMode == 0) {
            new Thread(new Runnable() {
                @Override
                public synchronized void run() {
                    MtkCatLog.d("[BIP]", "[TCP]running TCP channel thread");
                    try {
                        TcpChannel.this.mSocket = TcpChannel.this.mNetwork.getSocketFactory().createSocket();
                        TcpChannel.this.mSocket.setSoLinger(false, 0);
                        try {
                            TcpChannel.this.mSocket.connect(new InetSocketAddress(TcpChannel.this.mAddress, TcpChannel.this.mPort), TcpChannel.TCP_CONN_TIMEOUT);
                        } catch (SocketTimeoutException e) {
                            MtkCatLog.d("[BIP]", "[TCP]Time out of connect " + e + ":" + TcpChannel.TCP_CONN_TIMEOUT + " sec");
                            TcpChannel.this.mChannelStatus = 7;
                            if (TcpChannel.this.mBipService.mIsOpenChannelOverWifi) {
                                TcpChannel.this.mBipService.mIsConnectTimeout = true;
                                TcpChannel.this.mBipService.mIsOpenChannelOverWifi = false;
                            }
                        }
                        if (TcpChannel.this.mSocket.isConnected()) {
                            TcpChannel.this.mChannelStatus = 4;
                            TcpChannel.this.mChannelStatusData.mChannelStatus = 128;
                        } else {
                            MtkCatLog.e("[BIP]", "[TCP]socket is not connected.");
                            TcpChannel.this.mChannelStatus = 7;
                            TcpChannel.this.mSocket.close();
                        }
                    } catch (IOException e2) {
                        MtkCatLog.e("[BIP]", "[TCP]Fail to create socket");
                        e2.printStackTrace();
                        TcpChannel.this.mChannelStatus = 7;
                        if (TcpChannel.this.mBipService.mIsOpenChannelOverWifi) {
                            TcpChannel.this.mBipService.mIsConnectTimeout = true;
                            TcpChannel.this.mBipService.mIsOpenChannelOverWifi = false;
                        }
                    } catch (NullPointerException e3) {
                        MtkCatLog.e("[BIP]", "[TCP]Null pointer tcp socket " + e3);
                        TcpChannel.this.mChannelStatus = 7;
                    }
                    TcpChannel.this.onOpenChannelCompleted();
                }
            }).start();
            return 10;
        }
        if (this.mLinkMode != 1) {
            return 0;
        }
        new Thread(new Runnable() {
            @Override
            public synchronized void run() {
                MtkCatLog.d("[BIP]", "[TCP]running TCP channel thread");
                try {
                    TcpChannel.this.mSocket = new Socket();
                    TcpChannel.this.mSocket.setSoLinger(false, 0);
                    TcpChannel.this.mSocket.setSoTimeout(TcpChannel.TCP_CONN_TIMEOUT);
                } catch (SocketException e) {
                    MtkCatLog.d("[BIP]", "[TCP]Fail to create tcp socket");
                    TcpChannel.this.mChannelStatus = 7;
                }
            }
        }).start();
        this.mChannelStatus = 4;
        int iCheckBufferSize = checkBufferSize();
        if (iCheckBufferSize == 3) {
            MtkCatLog.d("[BIP]", "[TCP]openChannel: buffer size is modified");
            bipCmdMessage.mBufferSize = this.mBufferSize;
        }
        this.mRxBuffer = new byte[this.mBufferSize];
        this.mTxBuffer = new byte[this.mBufferSize];
        return iCheckBufferSize;
    }

    private void onOpenChannelCompleted() {
        int iCheckBufferSize = 5;
        if (this.mChannelStatus == 4) {
            try {
                MtkCatLog.d("[BIP]", "[TCP]stream is open");
                this.mInput = new DataInputStream(this.mSocket.getInputStream());
                this.mOutput = new BufferedOutputStream(this.mSocket.getOutputStream());
                this.rt = new Thread(new Channel.TcpReceiverThread(this.mInput));
                this.rt.start();
                iCheckBufferSize = checkBufferSize();
                this.mRxBuffer = new byte[this.mBufferSize];
                this.mTxBuffer = new byte[this.mBufferSize];
            } catch (IOException e) {
                MtkCatLog.d("[BIP]", "[TCP]Fail to create data stream");
                e.printStackTrace();
            }
        } else {
            MtkCatLog.d("[BIP]", "[TCP]socket is not open");
        }
        this.mBipService.openChannelCompleted(iCheckBufferSize, this);
    }

    @Override
    public int closeChannel() {
        MtkCatLog.d("[BIP]", "[TCP]closeChannel.");
        if (this.rt != null) {
            requestStop();
            this.rt = null;
        }
        new Thread(new Runnable() {
            @Override
            public synchronized void run() {
                TcpChannel tcpChannel;
                try {
                    try {
                        if (TcpChannel.this.mInput != null) {
                            TcpChannel.this.mInput.close();
                        }
                        if (TcpChannel.this.mOutput != null) {
                            TcpChannel.this.mOutput.close();
                        }
                        if (TcpChannel.this.mSocket != null) {
                            TcpChannel.this.mSocket.close();
                        }
                        TcpChannel.this.mSocket = null;
                        TcpChannel.this.mRxBuffer = null;
                        TcpChannel.this.mTxBuffer = null;
                        tcpChannel = TcpChannel.this;
                    } catch (IOException e) {
                        MtkCatLog.e("[BIP]", "[TCP]closeChannel - IOE");
                        TcpChannel.this.mSocket = null;
                        TcpChannel.this.mRxBuffer = null;
                        TcpChannel.this.mTxBuffer = null;
                        tcpChannel = TcpChannel.this;
                    }
                    tcpChannel.mChannelStatus = 2;
                } catch (Throwable th) {
                    TcpChannel.this.mSocket = null;
                    TcpChannel.this.mRxBuffer = null;
                    TcpChannel.this.mTxBuffer = null;
                    TcpChannel.this.mChannelStatus = 2;
                    throw th;
                }
            }
        }).start();
        return 0;
    }

    @Override
    public ReceiveDataResult receiveData(int i) {
        ReceiveDataResult receiveDataResult = new ReceiveDataResult();
        receiveDataResult.buffer = new byte[i];
        MtkCatLog.d("[BIP]", "[TCP]receiveData " + this.mRxBufferCount + "/" + i + "/" + this.mRxBufferOffset);
        if (this.mRxBufferCount >= i) {
            try {
                MtkCatLog.d("[BIP]", "[TCP]Start to copy data from buffer");
                System.arraycopy(this.mRxBuffer, this.mRxBufferOffset, receiveDataResult.buffer, 0, i);
                this.mRxBufferCount -= i;
                this.mRxBufferOffset += i;
                receiveDataResult.remainingCount = this.mRxBufferCount;
            } catch (IndexOutOfBoundsException e) {
            }
        } else {
            int i2 = this.mRxBufferCount;
            int i3 = i;
            boolean z = false;
            int i4 = 0;
            while (!z) {
                if (i3 > i2) {
                    try {
                        System.arraycopy(this.mRxBuffer, this.mRxBufferOffset, receiveDataResult.buffer, i4, i2);
                        this.mRxBufferOffset += i2;
                        this.mRxBufferCount -= i2;
                        i4 += i2;
                        i3 -= i2;
                    } catch (IndexOutOfBoundsException e2) {
                    }
                } else {
                    try {
                        System.arraycopy(Integer.valueOf(this.mRxBufferCount), this.mRxBufferOffset, receiveDataResult.buffer, i4, i2);
                        this.mRxBufferOffset += i3;
                        i4 += i3;
                        i3 = 0;
                    } catch (IndexOutOfBoundsException e3) {
                    }
                }
                if (i3 == 0) {
                    z = true;
                } else {
                    try {
                        this.mRxBufferCount = this.mInput.read(this.mRxBuffer, 0, this.mRxBuffer.length);
                        this.mRxBufferOffset = 0;
                    } catch (IOException e4) {
                        MtkCatLog.e("[BIP]", "[TCP]receiveData - IOE");
                    }
                }
            }
        }
        return receiveDataResult;
    }

    @Override
    public int sendData(byte[] bArr, int i) {
        if (bArr == null) {
            MtkCatLog.e("[BIP]", "[TCP]sendData - data null:");
            return 5;
        }
        if (this.mTxBuffer == null) {
            MtkCatLog.e("[BIP]", "[TCP]sendData - mTxBuffer null:");
            return 5;
        }
        int length = this.mTxBuffer.length - this.mTxBufferCount;
        try {
            MtkCatLog.d("[BIP]", "[TCP]sendData: size of data:" + bArr.length + " mode:" + i);
            MtkCatLog.d("[BIP]", "[TCP]sendData: size of buffer:" + this.mTxBuffer.length + " count:" + this.mTxBufferCount);
            if (this.mTxBufferCount == 0 && 1 == i) {
                this.mTxBufferCount = bArr.length;
            } else {
                try {
                    if (length >= bArr.length) {
                        System.arraycopy(bArr, 0, this.mTxBuffer, this.mTxBufferCount, bArr.length);
                        this.mTxBufferCount += bArr.length;
                    } else {
                        MtkCatLog.d("[BIP]", "[TCP]sendData - tx buffer is not enough");
                    }
                    bArr = this.mTxBuffer;
                } catch (IndexOutOfBoundsException e) {
                    return 5;
                }
            }
            if (i == 1 && this.mChannelStatus == 4) {
                try {
                    MtkCatLog.d("[BIP]", "[TCP]SEND_DATA_MODE_IMMEDIATE:" + this.mTxBuffer.length + " count:" + this.mTxBufferCount);
                    this.mOutput.write(bArr, 0, this.mTxBufferCount);
                    this.mOutput.flush();
                    this.mTxBufferCount = 0;
                } catch (IOException e2) {
                    MtkCatLog.e("[BIP]", "[TCP]sendData - Exception");
                    e2.printStackTrace();
                    return 5;
                }
            }
            return 0;
        } catch (NullPointerException e3) {
            MtkCatLog.d("[BIP]", "[UDP]sendData NE");
            e3.printStackTrace();
            return 5;
        }
    }

    @Override
    public int getTxAvailBufferSize() {
        if (this.mTxBuffer == null) {
            MtkCatLog.e("[BIP]", "[TCP]getTxAvailBufferSize - mTxBuffer null:");
            return 0;
        }
        int length = this.mTxBuffer.length - this.mTxBufferCount;
        MtkCatLog.d("[BIP]", "[TCP]available tx buffer size:" + length);
        return length;
    }

    @Override
    public int receiveData(int i, ReceiveDataResult receiveDataResult) {
        MtkCatLog.d("[BIP]", "[TCP]new receiveData method");
        if (receiveDataResult == null) {
            MtkCatLog.e("[BIP]", "[TCP]rdr is null");
            return 5;
        }
        MtkCatLog.d("[BIP]", "[TCP]receiveData mRxBufferCount:" + this.mRxBufferCount + " requestSize: " + i + " mRxBufferOffset:" + this.mRxBufferOffset);
        receiveDataResult.buffer = new byte[i];
        if (this.mRxBufferCount >= i) {
            try {
                synchronized (this.mLock) {
                    if (this.mRxBuffer != null && receiveDataResult.buffer != null) {
                        System.arraycopy(this.mRxBuffer, this.mRxBufferOffset, receiveDataResult.buffer, 0, i);
                        this.mRxBufferOffset += i;
                        this.mRxBufferCount -= i;
                        if (this.mRxBufferCount == 0) {
                            this.mRxBufferOffset = 0;
                        }
                        receiveDataResult.remainingCount = this.mRxBufferCount;
                        return 0;
                    }
                    MtkCatLog.d("[BIP]", "[TCP]mRxBuffer or rdr.buffer is null 1");
                    return 5;
                }
            } catch (IndexOutOfBoundsException e) {
                MtkCatLog.e("[BIP]", "[TCP]fail copy rx buffer out 1");
                return 5;
            }
        }
        MtkCatLog.e("[BIP]", "[TCP]rx buffer is insufficient !!!");
        try {
            synchronized (this.mLock) {
                if (this.mRxBuffer != null && receiveDataResult.buffer != null) {
                    System.arraycopy(this.mRxBuffer, this.mRxBufferOffset, receiveDataResult.buffer, 0, this.mRxBufferCount);
                    this.mRxBufferOffset = 0;
                    this.mRxBufferCount = 0;
                    this.mLock.notify();
                    receiveDataResult.remainingCount = 0;
                    return 9;
                }
                MtkCatLog.d("[BIP]", "[TCP]mRxBuffer or rdr.buffer is null 2");
                return 5;
            }
        } catch (IndexOutOfBoundsException e2) {
            MtkCatLog.e("[BIP]", "[TCP]fail copy rx buffer out 2");
            return 5;
        }
    }
}
