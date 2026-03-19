package com.android.internal.telephony.cat;

import android.net.Network;
import android.os.SystemProperties;
import com.android.internal.telephony.cat.Channel;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

class UdpChannel extends Channel {
    private static final int UDP_SOCKET_TIMEOUT = 3000;
    DatagramSocket mSocket;
    Thread rt;

    UdpChannel(int cid, int linkMode, int protocolType, InetAddress address, int port, int bufferSize, CatService handler, BipService bipManager) {
        super(cid, linkMode, protocolType, address, port, bufferSize, handler, bipManager);
        this.mSocket = null;
        this.rt = null;
    }

    @Override
    public int openChannel(BipCmdMessage cmdMsg, Network network) {
        int ret = 0;
        this.mNetwork = network;
        try {
            this.mIOTTest = SystemProperties.getInt("persist.service.bip.iot.test", DEFAULT_IOTTEST_VALUE);
        } catch (IllegalArgumentException e) {
            CatLog.e("[BIP]", "[UDP]key is illegal");
            this.mIOTTest = DEFAULT_IOTTEST_VALUE;
        }
        CatLog.d("[BIP]", "[UDP]link mode:" + this.mLinkMode + ", mIOTTest: " + this.mIOTTest);
        if (this.mLinkMode == 0) {
            try {
                this.mSocket = new DatagramSocket();
                this.mNetwork.bindSocket(this.mSocket);
                this.mChannelStatus = 4;
                this.mChannelStatusData.mChannelStatus = 128;
                this.rt = new Thread(new Channel.UdpReceiverThread(this.mSocket));
                this.rt.start();
                CatLog.d("[BIP]", "[UDP]: sock status:" + this.mChannelStatus);
            } catch (Exception e2) {
                e2.printStackTrace();
            }
            ret = checkBufferSize();
            if (ret == 3) {
                CatLog.d("[BIP]", "[UDP]openChannel: buffer size is modified");
                cmdMsg.mBufferSize = this.mBufferSize;
            }
            this.mRxBuffer = new byte[this.mBufferSize];
            this.mTxBuffer = new byte[this.mBufferSize];
        }
        return ret;
    }

    @Override
    public int closeChannel() {
        CatLog.d("[BIP]", "[UDP]closeChannel.");
        if (this.rt != null) {
            requestStop();
            this.rt = null;
        }
        if (this.mSocket != null) {
            CatLog.d("[BIP]", "[UDP]closeSocket.");
            this.mSocket.close();
            this.mChannelStatus = 2;
            this.mSocket = null;
            this.mRxBuffer = null;
            this.mTxBuffer = null;
        }
        return 0;
    }

    @Override
    public ReceiveDataResult receiveData(int requestCount) {
        ReceiveDataResult ret = new ReceiveDataResult();
        ret.buffer = new byte[requestCount];
        CatLog.d("[BIP]", "[UDP]receiveData " + this.mRxBufferCount + "/" + requestCount + "/" + this.mRxBufferOffset);
        if (this.mRxBufferCount >= requestCount) {
            try {
                System.arraycopy(this.mRxBuffer, this.mRxBufferOffset, ret.buffer, 0, requestCount);
                this.mRxBufferOffset += requestCount;
                this.mRxBufferCount -= requestCount;
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
                        countCopied += canCopy;
                        needCopy -= canCopy;
                        this.mRxBufferOffset += canCopy;
                        this.mRxBufferCount -= canCopy;
                    } catch (IndexOutOfBoundsException e2) {
                    }
                } else {
                    try {
                        System.arraycopy(this.mRxBuffer, this.mRxBufferOffset, ret.buffer, countCopied, needCopy);
                        this.mRxBufferOffset += needCopy;
                        this.mRxBufferCount -= needCopy;
                        countCopied += needCopy;
                        needCopy = 0;
                    } catch (IndexOutOfBoundsException e3) {
                    }
                }
                if (needCopy == 0) {
                    canExitLoop = true;
                } else {
                    try {
                        this.mSocket.setSoTimeout(UDP_SOCKET_TIMEOUT);
                        DatagramPacket packet = new DatagramPacket(this.mRxBuffer, this.mRxBuffer.length);
                        this.mSocket.receive(packet);
                        this.mRxBufferOffset = 0;
                        this.mRxBufferCount = packet.getLength();
                    } catch (Exception e4) {
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
            CatLog.e("[BIP]", "[UDP]sendData - data null:");
            return 5;
        }
        if (this.mTxBuffer == null) {
            CatLog.e("[BIP]", "[UDP]sendData - mTxBuffer null:");
            return 5;
        }
        int txRemaining = this.mTxBuffer.length - this.mTxBufferCount;
        CatLog.d("[BIP]", "[UDP]sendData: size of data:" + data.length + " mode:" + mode);
        CatLog.d("[BIP]", "[UDP]sendData: size of buffer:" + this.mTxBuffer.length + " count:" + this.mTxBufferCount);
        try {
            if (this.mTxBufferCount == 0 && 1 == mode) {
                tmpBuffer = data;
                this.mTxBufferCount = data.length;
            } else {
                if (txRemaining >= data.length) {
                    try {
                        System.arraycopy(data, 0, this.mTxBuffer, this.mTxBufferCount, data.length);
                        this.mTxBufferCount += data.length;
                    } catch (IndexOutOfBoundsException e) {
                        CatLog.e("[BIP]", "[UDP]sendData - IndexOutOfBoundsException");
                    }
                } else {
                    CatLog.d("[BIP]", "[UDP]sendData - tx buffer is not enough:" + txRemaining);
                }
                tmpBuffer = this.mTxBuffer;
            }
            if (mode != 1) {
                return 0;
            }
            CatLog.d("[BIP]", "[UDP]Send data(" + this.mAddress + ":" + this.mPort + "):" + this.mTxBuffer.length + " count:" + this.mTxBufferCount);
            DatagramPacket packet = new DatagramPacket(tmpBuffer, 0, this.mTxBufferCount, this.mAddress, this.mPort);
            if (this.mSocket == null) {
                return 0;
            }
            try {
                this.mSocket.send(packet);
                this.mTxBufferCount = 0;
                return 0;
            } catch (Exception e2) {
                CatLog.e("[BIP]", "[UDP]sendData - Exception");
                this.mChannelStatusData.mChannelStatus = 0;
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
            CatLog.e("[BIP]", "[UDP]getTxAvailBufferSize - mTxBuffer null:");
            return 0;
        }
        int txRemaining = this.mTxBuffer.length - this.mTxBufferCount;
        CatLog.d("[BIP]", "[UDP]available tx buffer size:" + txRemaining);
        return txRemaining;
    }

    @Override
    public int receiveData(int requestSize, ReceiveDataResult rdr) {
        if (rdr == null) {
            CatLog.e("[BIP]", "[UDP]rdr is null");
            return 5;
        }
        CatLog.d("[BIP]", "[UDP]receiveData mRxBufferCount:" + this.mRxBufferCount + " requestSize: " + requestSize + " mRxBufferOffset:" + this.mRxBufferOffset);
        rdr.buffer = new byte[requestSize];
        if (this.mRxBufferCount >= requestSize) {
            try {
                synchronized (this.mLock) {
                    System.arraycopy(this.mRxBuffer, this.mRxBufferOffset, rdr.buffer, 0, requestSize);
                    this.mRxBufferOffset += requestSize;
                    this.mRxBufferCount -= requestSize;
                    if (this.mRxBufferCount == 0) {
                        this.mRxBufferOffset = 0;
                    }
                    rdr.remainingCount = this.mRxBufferCount;
                }
                return 0;
            } catch (IndexOutOfBoundsException e) {
                CatLog.e("[BIP]", "[UDP]fail copy rx buffer out 1");
                return 5;
            }
        }
        CatLog.e("[BIP]", "[UDP]rx buffer is insufficient !!!");
        try {
            synchronized (this.mLock) {
                System.arraycopy(this.mRxBuffer, this.mRxBufferOffset, rdr.buffer, 0, this.mRxBufferCount);
                this.mRxBufferOffset = 0;
                this.mRxBufferCount = 0;
                this.mLock.notify();
            }
            rdr.remainingCount = 0;
            return 9;
        } catch (IndexOutOfBoundsException e2) {
            CatLog.e("[BIP]", "[UDP]fail copy rx buffer out 2");
            return 5;
        }
    }
}
