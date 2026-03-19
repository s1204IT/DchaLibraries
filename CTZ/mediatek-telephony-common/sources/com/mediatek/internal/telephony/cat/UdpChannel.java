package com.mediatek.internal.telephony.cat;

import android.net.Network;
import com.mediatek.internal.telephony.cat.Channel;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

class UdpChannel extends Channel {
    private static final int UDP_SOCKET_TIMEOUT = 3000;
    DatagramSocket mSocket;
    Thread rt;

    UdpChannel(int i, int i2, int i3, InetAddress inetAddress, int i4, int i5, MtkCatService mtkCatService, BipService bipService) {
        super(i, i2, i3, inetAddress, i4, i5, mtkCatService, bipService);
        this.mSocket = null;
        this.rt = null;
    }

    @Override
    public int openChannel(BipCmdMessage bipCmdMessage, Network network) {
        this.mNetwork = network;
        if (this.mLinkMode == 0) {
            try {
                this.mSocket = new DatagramSocket();
                this.mNetwork.bindSocket(this.mSocket);
                this.mChannelStatus = 4;
                this.mChannelStatusData.mChannelStatus = 128;
                this.rt = new Thread(new Channel.UdpReceiverThread(this.mSocket));
                this.rt.start();
                MtkCatLog.d("[BIP]", "[UDP]: sock status:" + this.mChannelStatus);
            } catch (Exception e) {
                e.printStackTrace();
            }
            int iCheckBufferSize = checkBufferSize();
            if (iCheckBufferSize == 3) {
                MtkCatLog.d("[BIP]", "[UDP]openChannel: buffer size is modified");
                bipCmdMessage.mBufferSize = this.mBufferSize;
            }
            this.mRxBuffer = new byte[this.mBufferSize];
            this.mTxBuffer = new byte[this.mBufferSize];
            return iCheckBufferSize;
        }
        return 0;
    }

    @Override
    public int closeChannel() {
        MtkCatLog.d("[BIP]", "[UDP]closeChannel.");
        if (this.rt != null) {
            requestStop();
            this.rt = null;
        }
        if (this.mSocket != null) {
            MtkCatLog.d("[BIP]", "[UDP]closeSocket.");
            this.mSocket.close();
            this.mChannelStatus = 2;
            this.mSocket = null;
            this.mRxBuffer = null;
            this.mTxBuffer = null;
            return 0;
        }
        return 0;
    }

    @Override
    public ReceiveDataResult receiveData(int i) {
        ReceiveDataResult receiveDataResult = new ReceiveDataResult();
        receiveDataResult.buffer = new byte[i];
        MtkCatLog.d("[BIP]", "[UDP]receiveData " + this.mRxBufferCount + "/" + i + "/" + this.mRxBufferOffset);
        if (this.mRxBufferCount >= i) {
            try {
                System.arraycopy(this.mRxBuffer, this.mRxBufferOffset, receiveDataResult.buffer, 0, i);
                this.mRxBufferOffset += i;
                this.mRxBufferCount -= i;
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
                        i4 += i2;
                        i3 -= i2;
                        this.mRxBufferOffset += i2;
                        this.mRxBufferCount -= i2;
                    } catch (IndexOutOfBoundsException e2) {
                    }
                } else {
                    try {
                        System.arraycopy(this.mRxBuffer, this.mRxBufferOffset, receiveDataResult.buffer, i4, i3);
                        this.mRxBufferOffset += i3;
                        this.mRxBufferCount -= i3;
                        i4 += i3;
                        i3 = 0;
                    } catch (IndexOutOfBoundsException e3) {
                    }
                }
                if (i3 == 0) {
                    z = true;
                } else {
                    try {
                        this.mSocket.setSoTimeout(UDP_SOCKET_TIMEOUT);
                        DatagramPacket datagramPacket = new DatagramPacket(this.mRxBuffer, this.mRxBuffer.length);
                        this.mSocket.receive(datagramPacket);
                        this.mRxBufferOffset = 0;
                        this.mRxBufferCount = datagramPacket.getLength();
                    } catch (Exception e4) {
                        e4.printStackTrace();
                    }
                }
            }
        }
        return receiveDataResult;
    }

    @Override
    public int sendData(byte[] bArr, int i) {
        if (bArr == null) {
            MtkCatLog.e("[BIP]", "[UDP]sendData - data null:");
            return 5;
        }
        if (this.mTxBuffer == null) {
            MtkCatLog.e("[BIP]", "[UDP]sendData - mTxBuffer null:");
            return 5;
        }
        int length = this.mTxBuffer.length - this.mTxBufferCount;
        MtkCatLog.d("[BIP]", "[UDP]sendData: size of data:" + bArr.length + " mode:" + i);
        MtkCatLog.d("[BIP]", "[UDP]sendData: size of buffer:" + this.mTxBuffer.length + " count:" + this.mTxBufferCount);
        try {
            if (this.mTxBufferCount == 0 && 1 == i) {
                this.mTxBufferCount = bArr.length;
            } else {
                if (length >= bArr.length) {
                    try {
                        System.arraycopy(bArr, 0, this.mTxBuffer, this.mTxBufferCount, bArr.length);
                        this.mTxBufferCount += bArr.length;
                    } catch (IndexOutOfBoundsException e) {
                        MtkCatLog.e("[BIP]", "[UDP]sendData - IndexOutOfBoundsException");
                    }
                } else {
                    MtkCatLog.d("[BIP]", "[UDP]sendData - tx buffer is not enough:" + length);
                }
                bArr = this.mTxBuffer;
            }
            byte[] bArr2 = bArr;
            if (i == 1) {
                MtkCatLog.d("[BIP]", "[UDP]Send data(" + this.mAddress + ":" + this.mPort + "):" + this.mTxBuffer.length + " count:" + this.mTxBufferCount);
                DatagramPacket datagramPacket = new DatagramPacket(bArr2, 0, this.mTxBufferCount, this.mAddress, this.mPort);
                if (this.mSocket != null) {
                    try {
                        this.mSocket.send(datagramPacket);
                        this.mTxBufferCount = 0;
                    } catch (Exception e2) {
                        MtkCatLog.e("[BIP]", "[UDP]sendData - Exception");
                        this.mChannelStatusData.mChannelStatus = 0;
                        e2.printStackTrace();
                        return 5;
                    }
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
            MtkCatLog.e("[BIP]", "[UDP]getTxAvailBufferSize - mTxBuffer null:");
            return 0;
        }
        int length = this.mTxBuffer.length - this.mTxBufferCount;
        MtkCatLog.d("[BIP]", "[UDP]available tx buffer size:" + length);
        return length;
    }

    @Override
    public int receiveData(int i, ReceiveDataResult receiveDataResult) {
        if (receiveDataResult == null) {
            MtkCatLog.e("[BIP]", "[UDP]rdr is null");
            return 5;
        }
        MtkCatLog.d("[BIP]", "[UDP]receiveData mRxBufferCount:" + this.mRxBufferCount + " requestSize: " + i + " mRxBufferOffset:" + this.mRxBufferOffset);
        receiveDataResult.buffer = new byte[i];
        if (this.mRxBufferCount >= i) {
            try {
                synchronized (this.mLock) {
                    System.arraycopy(this.mRxBuffer, this.mRxBufferOffset, receiveDataResult.buffer, 0, i);
                    this.mRxBufferOffset += i;
                    this.mRxBufferCount -= i;
                    if (this.mRxBufferCount == 0) {
                        this.mRxBufferOffset = 0;
                    }
                    receiveDataResult.remainingCount = this.mRxBufferCount;
                }
                return 0;
            } catch (IndexOutOfBoundsException e) {
                MtkCatLog.e("[BIP]", "[UDP]fail copy rx buffer out 1");
                return 5;
            }
        }
        MtkCatLog.e("[BIP]", "[UDP]rx buffer is insufficient !!!");
        try {
            synchronized (this.mLock) {
                System.arraycopy(this.mRxBuffer, this.mRxBufferOffset, receiveDataResult.buffer, 0, this.mRxBufferCount);
                this.mRxBufferOffset = 0;
                this.mRxBufferCount = 0;
                this.mLock.notify();
            }
            receiveDataResult.remainingCount = 0;
            return 9;
        } catch (IndexOutOfBoundsException e2) {
            MtkCatLog.e("[BIP]", "[UDP]fail copy rx buffer out 2");
            return 5;
        }
    }
}
