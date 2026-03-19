package com.android.internal.telephony.cat;

import android.net.Network;
import com.android.internal.telephony.cat.Channel;
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

    TcpChannel(int cid, int linkMode, int protocolType, InetAddress address, int port, int bufferSize, CatService handler, BipService bipManager) {
        super(cid, linkMode, protocolType, address, port, bufferSize, handler, bipManager);
        this.mSocket = null;
        this.mInput = null;
        this.mOutput = null;
    }

    @Override
    public int openChannel(BipCmdMessage cmdMsg, Network network) {
        this.mNetwork = network;
        if (this.mLinkMode == 0) {
            Thread t_openChannelThread = new Thread(new Runnable() {
                @Override
                public synchronized void run() {
                    CatLog.d("[BIP]", "[TCP]running TCP channel thread");
                    try {
                        TcpChannel.this.mSocket = TcpChannel.this.mNetwork.getSocketFactory().createSocket();
                        TcpChannel.this.mSocket.setSoLinger(false, 0);
                        InetSocketAddress socketAddress = new InetSocketAddress(TcpChannel.this.mAddress, TcpChannel.this.mPort);
                        try {
                            if (TcpChannel.this.mBipService.isTestSim()) {
                                TcpChannel.this.mSocket.bind(new InetSocketAddress(32000));
                            }
                            try {
                                TcpChannel.this.mSocket.connect(socketAddress, TcpChannel.TCP_CONN_TIMEOUT);
                            } catch (SocketTimeoutException e3) {
                                CatLog.d("[BIP]", "[TCP]Time out of connect " + e3 + ":" + TcpChannel.TCP_CONN_TIMEOUT + " sec");
                                TcpChannel.this.mChannelStatus = 7;
                            }
                            if (TcpChannel.this.mSocket.isConnected()) {
                                TcpChannel.this.mChannelStatus = 4;
                                TcpChannel.this.mChannelStatusData.mChannelStatus = 128;
                            } else {
                                CatLog.e("[BIP]", "[TCP]socket is not connected.");
                                TcpChannel.this.mChannelStatus = 7;
                                TcpChannel.this.mSocket.close();
                            }
                        } catch (IOException e) {
                            e = e;
                            CatLog.e("[BIP]", "[TCP]Fail to create socket");
                            e.printStackTrace();
                            TcpChannel.this.mChannelStatus = 7;
                        } catch (NullPointerException e2) {
                            e2 = e2;
                            CatLog.e("[BIP]", "[TCP]Null pointer tcp socket " + e2);
                            TcpChannel.this.mChannelStatus = 7;
                        }
                    } catch (IOException e4) {
                        e = e4;
                    } catch (NullPointerException e5) {
                        e2 = e5;
                    }
                    TcpChannel.this.onOpenChannelCompleted();
                }
            });
            t_openChannelThread.start();
            return 10;
        }
        if (this.mLinkMode != 1) {
            return 0;
        }
        Thread t_openChannelThread2 = new Thread(new Runnable() {
            @Override
            public synchronized void run() {
                CatLog.d("[BIP]", "[TCP]running TCP channel thread");
                try {
                    TcpChannel.this.mSocket = new Socket();
                    TcpChannel.this.mSocket.setSoLinger(false, 0);
                    TcpChannel.this.mSocket.setSoTimeout(TcpChannel.TCP_CONN_TIMEOUT);
                } catch (SocketException e) {
                    CatLog.d("[BIP]", "[TCP]Fail to create tcp socket");
                    TcpChannel.this.mChannelStatus = 7;
                }
            }
        });
        t_openChannelThread2.start();
        this.mChannelStatus = 4;
        int ret = checkBufferSize();
        if (ret == 3) {
            CatLog.d("[BIP]", "[TCP]openChannel: buffer size is modified");
            cmdMsg.mBufferSize = this.mBufferSize;
        }
        this.mRxBuffer = new byte[this.mBufferSize];
        this.mTxBuffer = new byte[this.mBufferSize];
        return ret;
    }

    private void onOpenChannelCompleted() {
        int ret;
        if (this.mChannelStatus == 4) {
            try {
                CatLog.d("[BIP]", "[TCP]stream is open");
                this.mInput = new DataInputStream(this.mSocket.getInputStream());
                this.mOutput = new BufferedOutputStream(this.mSocket.getOutputStream());
                this.rt = new Thread(new Channel.TcpReceiverThread(this.mInput));
                this.rt.start();
                ret = checkBufferSize();
                this.mRxBuffer = new byte[this.mBufferSize];
                this.mTxBuffer = new byte[this.mBufferSize];
            } catch (IOException e) {
                CatLog.d("[BIP]", "[TCP]Fail to create data stream");
                e.printStackTrace();
                ret = 5;
            }
        } else {
            CatLog.d("[BIP]", "[TCP]socket is not open");
            ret = 5;
        }
        this.mBipService.openChannelCompleted(ret, this);
    }

    @Override
    public int closeChannel() {
        CatLog.d("[BIP]", "[TCP]closeChannel.");
        if (this.rt != null) {
            requestStop();
            this.rt = null;
        }
        Thread closeChannelThread = new Thread(new Runnable() {
            @Override
            public synchronized void run() {
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
                } catch (IOException e) {
                    CatLog.e("[BIP]", "[TCP]closeChannel - IOE");
                } finally {
                    TcpChannel.this.mSocket = null;
                    TcpChannel.this.mRxBuffer = null;
                    TcpChannel.this.mTxBuffer = null;
                    TcpChannel.this.mChannelStatus = 2;
                }
            }
        });
        closeChannelThread.start();
        return 0;
    }

    @Override
    public ReceiveDataResult receiveData(int requestCount) {
        ReceiveDataResult ret = new ReceiveDataResult();
        ret.buffer = new byte[requestCount];
        CatLog.d("[BIP]", "[TCP]receiveData " + this.mRxBufferCount + "/" + requestCount + "/" + this.mRxBufferOffset);
        if (this.mRxBufferCount >= requestCount) {
            try {
                CatLog.d("[BIP]", "[TCP]Start to copy data from buffer");
                System.arraycopy(this.mRxBuffer, this.mRxBufferOffset, ret.buffer, 0, requestCount);
                this.mRxBufferCount -= requestCount;
                this.mRxBufferOffset += requestCount;
                ret.remainingCount = this.mRxBufferCount;
            } catch (IndexOutOfBoundsException e) {
            }
        } else {
            int needCopy = requestCount;
            int canCopy = this.mRxBufferCount;
            int countCopied = 0;
            boolean canExitLoop = false;
            while (!canExitLoop) {
                if (needCopy > canCopy) {
                    try {
                        System.arraycopy(this.mRxBuffer, this.mRxBufferOffset, ret.buffer, countCopied, canCopy);
                        this.mRxBufferOffset += canCopy;
                        this.mRxBufferCount -= canCopy;
                        countCopied += canCopy;
                        needCopy -= canCopy;
                    } catch (IndexOutOfBoundsException e2) {
                    }
                } else {
                    try {
                        System.arraycopy(Integer.valueOf(this.mRxBufferCount), this.mRxBufferOffset, ret.buffer, countCopied, canCopy);
                        this.mRxBufferOffset += needCopy;
                        countCopied += needCopy;
                        needCopy = 0;
                    } catch (IndexOutOfBoundsException e3) {
                    }
                }
                if (needCopy == 0) {
                    canExitLoop = true;
                } else {
                    try {
                        int count = this.mInput.read(this.mRxBuffer, 0, this.mRxBuffer.length);
                        this.mRxBufferCount = count;
                        this.mRxBufferOffset = 0;
                    } catch (IOException e4) {
                        CatLog.e("[BIP]", "[TCP]receiveData - IOE");
                    }
                }
            }
        }
        return ret;
    }

    @Override
    public int sendData(byte[] data, int mode) {
        byte[] tmpBuffer;
        if (data == null) {
            CatLog.e("[BIP]", "[TCP]sendData - data null:");
            return 5;
        }
        if (this.mTxBuffer == null) {
            CatLog.e("[BIP]", "[TCP]sendData - mTxBuffer null:");
            return 5;
        }
        int txRemaining = this.mTxBuffer.length - this.mTxBufferCount;
        try {
            CatLog.d("[BIP]", "[TCP]sendData: size of data:" + data.length + " mode:" + mode);
            CatLog.d("[BIP]", "[TCP]sendData: size of buffer:" + this.mTxBuffer.length + " count:" + this.mTxBufferCount);
            if (this.mTxBufferCount == 0 && 1 == mode) {
                tmpBuffer = data;
                this.mTxBufferCount = data.length;
            } else {
                try {
                    if (txRemaining >= data.length) {
                        System.arraycopy(data, 0, this.mTxBuffer, this.mTxBufferCount, data.length);
                        this.mTxBufferCount += data.length;
                    } else {
                        CatLog.d("[BIP]", "[TCP]sendData - tx buffer is not enough");
                    }
                    tmpBuffer = this.mTxBuffer;
                } catch (IndexOutOfBoundsException e) {
                    return 5;
                }
            }
            if (mode != 1 || this.mChannelStatus != 4) {
                return 0;
            }
            try {
                CatLog.d("[BIP]", "[TCP]SEND_DATA_MODE_IMMEDIATE:" + this.mTxBuffer.length + " count:" + this.mTxBufferCount);
                this.mOutput.write(tmpBuffer, 0, this.mTxBufferCount);
                this.mOutput.flush();
                this.mTxBufferCount = 0;
                return 0;
            } catch (IOException e2) {
                CatLog.e("[BIP]", "[TCP]sendData - Exception");
                e2.printStackTrace();
                return 5;
            }
        } catch (NullPointerException ne) {
            CatLog.d("[BIP]", "[UDP]sendData NE");
            ne.printStackTrace();
            return 5;
        }
    }

    @Override
    public int getTxAvailBufferSize() {
        if (this.mTxBuffer == null) {
            CatLog.e("[BIP]", "[TCP]getTxAvailBufferSize - mTxBuffer null:");
            return 0;
        }
        int txRemaining = this.mTxBuffer.length - this.mTxBufferCount;
        CatLog.d("[BIP]", "[TCP]available tx buffer size:" + txRemaining);
        return txRemaining;
    }

    @Override
    public int receiveData(int requestSize, ReceiveDataResult rdr) {
        CatLog.d("[BIP]", "[TCP]new receiveData method");
        if (rdr == null) {
            CatLog.e("[BIP]", "[TCP]rdr is null");
            return 5;
        }
        CatLog.d("[BIP]", "[TCP]receiveData mRxBufferCount:" + this.mRxBufferCount + " requestSize: " + requestSize + " mRxBufferOffset:" + this.mRxBufferOffset);
        rdr.buffer = new byte[requestSize];
        if (this.mRxBufferCount < requestSize) {
            CatLog.e("[BIP]", "[TCP]rx buffer is insufficient !!!");
            try {
                synchronized (this.mLock) {
                    if (this.mRxBuffer == null || rdr.buffer == null) {
                        CatLog.d("[BIP]", "[TCP]mRxBuffer or rdr.buffer is null 2");
                        return 5;
                    }
                    System.arraycopy(this.mRxBuffer, this.mRxBufferOffset, rdr.buffer, 0, this.mRxBufferCount);
                    this.mRxBufferOffset = 0;
                    this.mRxBufferCount = 0;
                    this.mLock.notify();
                    rdr.remainingCount = 0;
                    return 9;
                }
            } catch (IndexOutOfBoundsException e) {
                CatLog.e("[BIP]", "[TCP]fail copy rx buffer out 2");
                return 5;
            }
        }
        try {
            synchronized (this.mLock) {
                if (this.mRxBuffer == null || rdr.buffer == null) {
                    CatLog.d("[BIP]", "[TCP]mRxBuffer or rdr.buffer is null 1");
                    return 5;
                }
                System.arraycopy(this.mRxBuffer, this.mRxBufferOffset, rdr.buffer, 0, requestSize);
                this.mRxBufferOffset += requestSize;
                this.mRxBufferCount -= requestSize;
                if (this.mRxBufferCount == 0) {
                    this.mRxBufferOffset = 0;
                    this.mLock.notify();
                }
                rdr.remainingCount = this.mRxBufferCount;
                return 0;
            }
        } catch (IndexOutOfBoundsException e2) {
            CatLog.e("[BIP]", "[TCP]fail copy rx buffer out 1");
            return 5;
        }
    }
}
