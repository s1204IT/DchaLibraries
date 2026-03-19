package com.android.internal.telephony.cat;

import android.net.Network;
import com.android.internal.telephony.cat.Channel;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.ServerSocket;
import java.net.Socket;

class TcpServerChannel extends Channel {
    private boolean mCloseBackToTcpListen;
    protected DataInputStream mInput;
    protected BufferedOutputStream mOutput;
    protected ServerSocket mSSocket;
    protected Socket mSocket;
    private Thread rt;

    TcpServerChannel(int cid, int linkMode, int protocolType, int port, int bufferSize, CatService handler, BipService bipManager) {
        super(cid, linkMode, protocolType, null, port, bufferSize, handler, bipManager);
        this.mSSocket = null;
        this.mSocket = null;
        this.mInput = null;
        this.mOutput = null;
        this.rt = null;
        this.mCloseBackToTcpListen = false;
    }

    @Override
    public int openChannel(BipCmdMessage cmdMsg, Network network) {
        this.mNetwork = network;
        CatLog.d("[BIP]", "[UICC]openChannel mLinkMode:" + this.mLinkMode);
        try {
            CatLog.d("[BIP]", "[UICC]New server socket.mChannelStatus:" + this.mChannelStatus + ",port:" + this.mPort);
            this.mSSocket = new ServerSocket(this.mPort, 0, Inet4Address.LOOPBACK);
            if (this.mChannelStatus == 0 || this.mChannelStatus == 2) {
                setTcpStatus(BipUtils.TCP_STATUS_LISTEN, false);
                this.mChannelStatus = 4;
                this.rt = new Thread(new Channel.UICCServerThread(this));
                this.rt.start();
            }
            int ret = checkBufferSize();
            if (ret == 3) {
                CatLog.d("[BIP]", "[UICC]openChannel: buffer size is modified");
                cmdMsg.mBufferSize = this.mBufferSize;
            }
            cmdMsg.mChannelStatusData.mChannelStatus = getTcpStatus();
            this.mRxBuffer = new byte[this.mBufferSize];
            this.mTxBuffer = new byte[this.mBufferSize];
            return ret;
        } catch (IOException e) {
            CatLog.d("[BIP]", "[UICC]IOEX to create server socket");
            return 5;
        } catch (Exception e2) {
            CatLog.d("[BIP]", "[UICC]EX to create server socket " + e2);
            return 5;
        }
    }

    @Override
    public int closeChannel() {
        CatLog.d("[BIP]", "[UICC]closeChannel.");
        if (this.mCloseBackToTcpListen) {
            try {
                if (-128 == this.mChannelStatusData.mChannelStatus) {
                    try {
                        this.mChannelStatusData.mChannelStatus = 64;
                        if (this.mInput != null) {
                            this.mInput.close();
                        }
                        if (this.mOutput != null) {
                            this.mOutput.close();
                        }
                        if (this.mSocket != null) {
                            this.mSocket.close();
                        }
                        this.rt.interrupt();
                        this.mSocket = null;
                        this.mRxBuffer = null;
                    } catch (IOException e) {
                        CatLog.e("[BIP]", "[UICC]IOEX closeChannel back to tcp listen.");
                        this.mSocket = null;
                        this.mRxBuffer = null;
                    }
                    this.mTxBuffer = null;
                }
            } finally {
            }
        } else {
            if (this.rt != null) {
                requestStop();
                this.rt = null;
            }
            try {
                try {
                    if (this.mInput != null) {
                        this.mInput.close();
                    }
                    if (this.mOutput != null) {
                        this.mOutput.close();
                    }
                    if (this.mSocket != null) {
                        this.mSocket.close();
                    }
                    if (this.mSSocket != null) {
                        this.mSSocket.close();
                    }
                    this.mSocket = null;
                    this.mRxBuffer = null;
                } catch (IOException e2) {
                    CatLog.e("[BIP]", "[UICC]IOEX closeChannel");
                    this.mSocket = null;
                    this.mRxBuffer = null;
                }
                this.mTxBuffer = null;
            } finally {
            }
        }
        return 0;
    }

    @Override
    public ReceiveDataResult receiveData(int requestCount) {
        ReceiveDataResult ret = new ReceiveDataResult();
        ret.buffer = new byte[requestCount];
        CatLog.d("[BIP]", "[UICC]receiveData " + this.mRxBufferCount + "/" + requestCount + "/" + this.mRxBufferOffset);
        if (this.mRxBufferCount >= requestCount) {
            try {
                CatLog.d("[BIP]", "[UICC]Start to copy data from buffer");
                System.arraycopy(this.mRxBuffer, this.mRxBufferOffset, ret.buffer, 0, requestCount);
                this.mRxBufferCount -= requestCount;
                this.mRxBufferOffset += requestCount;
                ret.remainingCount = this.mRxBufferCount;
            } catch (IndexOutOfBoundsException e) {
                CatLog.e("[BIP]", "IOOB-1");
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
                        CatLog.e("[BIP]", "IOOB-2");
                    }
                } else {
                    try {
                        System.arraycopy(Integer.valueOf(this.mRxBufferCount), this.mRxBufferOffset, ret.buffer, countCopied, canCopy);
                        this.mRxBufferOffset += needCopy;
                        countCopied += needCopy;
                        needCopy = 0;
                    } catch (IndexOutOfBoundsException e3) {
                        CatLog.e("[BIP]", "IOOB-3");
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
                        CatLog.e("[BIP]", "IOException");
                        e4.printStackTrace();
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
            CatLog.e("[BIP]", "[UICC]sendData - data null:");
            return 5;
        }
        if (this.mTxBuffer == null) {
            CatLog.e("[BIP]", "[UICC]sendData - mTxBuffer null:");
            return 5;
        }
        int txRemaining = this.mTxBuffer.length - this.mTxBufferCount;
        CatLog.d("[BIP]", "[UICC]sendData: size of buffer:" + data.length + " mode:" + mode);
        CatLog.d("[BIP]", "[UICC]sendData: size of buffer:" + this.mTxBuffer.length + " count:" + this.mTxBufferCount);
        if (this.mTxBufferCount == 0 && 1 == mode) {
            tmpBuffer = data;
            this.mTxBufferCount = data.length;
        } else {
            try {
                if (txRemaining >= data.length) {
                    System.arraycopy(data, 0, this.mTxBuffer, this.mTxBufferCount, data.length);
                    this.mTxBufferCount += data.length;
                } else {
                    CatLog.d("[BIP]", "[UICC]sendData - tx buffer is not enough");
                }
                tmpBuffer = this.mTxBuffer;
            } catch (IndexOutOfBoundsException e) {
                return 5;
            }
        }
        if (mode == 1 && this.mChannelStatus == 4 && this.mChannelStatusData.mChannelStatus == -128) {
            try {
                CatLog.d("[BIP]", "S[UICC]END_DATA_MODE_IMMEDIATE:" + this.mTxBuffer.length + " count:" + this.mTxBufferCount);
                this.mOutput.write(tmpBuffer, 0, this.mTxBufferCount);
                this.mOutput.flush();
                this.mTxBufferCount = 0;
            } catch (IOException e2) {
                e2.printStackTrace();
                return 5;
            } catch (NullPointerException e22) {
                e22.printStackTrace();
                return 5;
            }
        }
        return 0;
    }

    @Override
    public int getTxAvailBufferSize() {
        if (this.mTxBuffer == null) {
            CatLog.e("[BIP]", "[UICC]getTxAvailBufferSize - mTxBuffer null:");
            return 0;
        }
        int txRemaining = this.mTxBuffer.length - this.mTxBufferCount;
        CatLog.d("[BIP]", "[UICC]available tx buffer size:" + txRemaining);
        return txRemaining;
    }

    @Override
    public int receiveData(int requestSize, ReceiveDataResult rdr) {
        CatLog.d("[BIP]", "[UICC]new receiveData method");
        if (rdr == null) {
            CatLog.d("[BIP]", "[UICC]rdr is null");
            return 5;
        }
        CatLog.d("[BIP]", "[UICC]receiveData " + this.mRxBufferCount + "/" + requestSize + "/" + this.mRxBufferOffset);
        rdr.buffer = new byte[requestSize];
        if (this.mRxBufferCount >= requestSize) {
            CatLog.d("[BIP]", "[UICC]rx buffer has enough data");
            try {
                synchronized (this.mLock) {
                    System.arraycopy(this.mRxBuffer, this.mRxBufferOffset, rdr.buffer, 0, requestSize);
                    this.mRxBufferOffset += requestSize;
                    this.mRxBufferCount -= requestSize;
                    if (this.mRxBufferCount == 0) {
                        this.mRxBufferOffset = 0;
                    }
                    rdr.remainingCount = this.mRxBufferCount + this.mRxBufferCacheCount;
                    if (this.mRxBufferCount < this.mBufferSize) {
                        CatLog.d("[BIP]", ">= [UICC]notify to read data more to mRxBuffer");
                        this.mLock.notify();
                    }
                }
                CatLog.d("[BIP]", "[UICC]rx buffer has enough data - end");
                return 0;
            } catch (IndexOutOfBoundsException e) {
                CatLog.d("[BIP]", "[UICC]fail copy rx buffer out 1");
                return 5;
            }
        }
        CatLog.d("[BIP]", "[UICC]rx buffer is insufficient - being");
        try {
            synchronized (this.mLock) {
                System.arraycopy(this.mRxBuffer, this.mRxBufferOffset, rdr.buffer, 0, this.mRxBufferCount);
                this.mRxBufferOffset = 0;
                this.mRxBufferCount = 0;
                if (this.mRxBufferCount < this.mBufferSize) {
                    CatLog.d("[BIP]", "< [UICC]notify to read data more to mRxBuffer");
                    this.mLock.notify();
                }
            }
            rdr.remainingCount = 0;
            CatLog.d("[BIP]", "[UICC]rx buffer is insufficient - end");
            return 9;
        } catch (IndexOutOfBoundsException e2) {
            CatLog.d("[BIP]", "[UICC]fail copy rx buffer out 2");
            return 5;
        }
    }

    public void setTcpStatus(byte status, boolean isPackED) {
        if (this.mChannelStatusData.mChannelStatus == status) {
            return;
        }
        CatLog.d("[BIP]", "[UICC][TCPStatus]" + this.mChannelStatusData.mChannelStatus + "->" + ((int) status));
        this.mChannelStatusData.mChannelStatus = status;
        if (!isPackED) {
            return;
        }
        changeChannelStatus(status);
    }

    public byte getTcpStatus() {
        try {
            return (byte) this.mChannelStatusData.mChannelStatus;
        } catch (NullPointerException e) {
            CatLog.e("[BIP]", "[TCP]getTcpStatus");
            return (byte) 0;
        }
    }

    public void setCloseBackToTcpListen(boolean isBackToTcpListen) {
        this.mCloseBackToTcpListen = isBackToTcpListen;
    }

    public boolean isCloseBackToTcpListen() {
        return this.mCloseBackToTcpListen;
    }
}
