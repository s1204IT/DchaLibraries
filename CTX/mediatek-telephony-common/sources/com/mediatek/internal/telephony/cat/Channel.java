package com.mediatek.internal.telephony.cat;

import android.net.Network;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Arrays;

abstract class Channel {
    protected static final int SOCKET_TIMEOUT = 3000;
    protected InetAddress mAddress;
    protected BipChannelManager mBipChannelManager;
    protected BipService mBipService;
    protected int mBufferSize;
    protected int mChannelId;
    protected ChannelStatus mChannelStatusData;
    private MtkCatService mHandler;
    protected int mLinkMode;
    protected int mPort;
    protected int mProtocolType;
    protected int mChannelStatus = 0;
    protected byte[] mRxBuffer = null;
    protected byte[] mTxBuffer = null;
    protected int mRxBufferCount = 0;
    protected int mRxBufferOffset = 0;
    protected int mTxBufferCount = 0;
    protected int mRxBufferCacheCount = 0;
    protected ReceiveDataResult mRecvDataRet = null;
    protected int needCopy = 0;
    protected boolean isChannelOpened = false;
    protected boolean isReceiveDataTRSent = false;
    protected Network mNetwork = null;
    private volatile boolean mStop = false;
    protected Object mLock = new Object();

    public abstract int closeChannel();

    public abstract int getTxAvailBufferSize();

    public abstract int openChannel(BipCmdMessage bipCmdMessage, Network network);

    public abstract int receiveData(int i, ReceiveDataResult receiveDataResult);

    public abstract ReceiveDataResult receiveData(int i);

    public abstract int sendData(byte[] bArr, int i);

    Channel(int i, int i2, int i3, InetAddress inetAddress, int i4, int i5, MtkCatService mtkCatService, BipService bipService) {
        this.mChannelId = -1;
        this.mLinkMode = 0;
        this.mProtocolType = 0;
        this.mAddress = null;
        this.mPort = 0;
        this.mHandler = null;
        this.mBipService = null;
        this.mBipChannelManager = null;
        this.mBufferSize = 0;
        this.mChannelStatusData = null;
        this.mChannelId = i;
        this.mLinkMode = i2;
        this.mProtocolType = i3;
        this.mAddress = inetAddress;
        this.mPort = i4;
        this.mBufferSize = i5;
        this.mHandler = mtkCatService;
        this.mBipService = bipService;
        this.mBipChannelManager = this.mBipService.getBipChannelManager();
        this.mChannelStatusData = new ChannelStatus(i, 0, 0);
    }

    public void dataAvailable(int i) {
        if (this.mBipService.mCurrentSetupEventCmd == null) {
            MtkCatLog.e(this, "mCurrentSetupEventCmd is null");
            return;
        }
        if (!this.mBipService.hasPsEvent(9)) {
            MtkCatLog.d(this, "No need to send data available.");
            return;
        }
        MtkCatResponseMessage mtkCatResponseMessage = new MtkCatResponseMessage(MtkCatCmdMessage.getCmdMsg(), 9);
        byte[] bArr = new byte[7];
        bArr[0] = -72;
        bArr[1] = 2;
        bArr[2] = (byte) (getChannelId() | this.mChannelStatusData.mChannelStatus);
        bArr[3] = 0;
        bArr[4] = -73;
        bArr[5] = 1;
        if (i > 255) {
            bArr[6] = -1;
        } else {
            bArr[6] = (byte) i;
        }
        mtkCatResponseMessage.setSourceId(130);
        mtkCatResponseMessage.setDestinationId(129);
        mtkCatResponseMessage.setEventDownload(9, bArr);
        mtkCatResponseMessage.setAdditionalInfo(bArr);
        mtkCatResponseMessage.setOneShot(false);
        MtkCatLog.d(this, "onEventDownload for dataAvailable");
        this.mHandler.onEventDownload(mtkCatResponseMessage);
    }

    public void changeChannelStatus(byte b) {
        if (this.mBipService.mCurrentSetupEventCmd == null) {
            MtkCatLog.e(this, "mCurrentSetupEventCmd is null");
            return;
        }
        if (!this.mBipService.hasPsEvent(10)) {
            MtkCatLog.d(this, "No need to send channel status.");
            return;
        }
        MtkCatResponseMessage mtkCatResponseMessage = new MtkCatResponseMessage(this.mBipService.mCurrentSetupEventCmd, 10);
        MtkCatLog.d("[BIP]", "[Channel]:changeChannelStatus:" + ((int) b));
        byte[] bArr = {-72, 2, (byte) (b | getChannelId()), 0};
        mtkCatResponseMessage.setSourceId(130);
        mtkCatResponseMessage.setDestinationId(129);
        mtkCatResponseMessage.setEventDownload(10, bArr);
        mtkCatResponseMessage.setAdditionalInfo(bArr);
        mtkCatResponseMessage.setOneShot(false);
        this.mHandler.onEventDownload(mtkCatResponseMessage);
    }

    public int getChannelStatus() {
        return this.mChannelStatus;
    }

    public int getChannelId() {
        return this.mChannelId;
    }

    public void clearChannelBuffer(boolean z) {
        if (true == z) {
            Arrays.fill(this.mRxBuffer, (byte) 0);
            Arrays.fill(this.mTxBuffer, (byte) 0);
        } else {
            this.mRxBuffer = null;
            this.mTxBuffer = null;
        }
        this.mRxBufferCount = 0;
        this.mRxBufferOffset = 0;
        this.mTxBufferCount = 0;
    }

    protected int checkBufferSize() {
        int i = 1024;
        int i2 = 1400;
        int i3 = 255;
        if (this.mProtocolType != 5 && this.mProtocolType != 2 && this.mProtocolType != 3 && this.mProtocolType != 4 && this.mProtocolType != 1) {
            i = 0;
            i2 = 0;
            i3 = 0;
        }
        MtkCatLog.d("[BIP]", "mBufferSize:" + this.mBufferSize + " minBufferSize:" + i3 + " maxBufferSize:" + i2);
        if (this.mBufferSize >= i3 && this.mBufferSize <= i2) {
            MtkCatLog.d("[BIP]", "buffer size is normal");
            return 0;
        }
        if (this.mBufferSize > i2) {
            MtkCatLog.d("[BIP]", "buffer size is too large, change it to maximum value");
            this.mBufferSize = i2;
        } else {
            MtkCatLog.d("[BIP]", "buffer size is too small, change it to default value");
            this.mBufferSize = i;
        }
        if (this.mBufferSize < 237) {
            MtkCatLog.d("[BIP]", "buffer size is smaller than 255, change it to MAX_APDU_SIZE");
            this.mBufferSize = BipUtils.MAX_APDU_SIZE;
        }
        return 3;
    }

    protected synchronized void requestStop() {
        this.mStop = true;
        MtkCatLog.d("[BIP]", "requestStop: " + this.mStop);
    }

    protected class UdpReceiverThread implements Runnable {
        DatagramSocket udpSocket;

        UdpReceiverThread(DatagramSocket datagramSocket) {
            this.udpSocket = datagramSocket;
        }

        @Override
        public void run() {
            byte[] bArr = new byte[1400];
            MtkCatLog.d("[BIP]", "[UDP]RecTr run");
            DatagramPacket datagramPacket = new DatagramPacket(bArr, bArr.length);
            while (true) {
                try {
                    if (Channel.this.mStop) {
                        break;
                    }
                    MtkCatLog.d("[BIP]", "[UDP]RecTr: Wait data from network");
                    try {
                        Arrays.fill(bArr, (byte) 0);
                        this.udpSocket.receive(datagramPacket);
                        int length = datagramPacket.getLength();
                        MtkCatLog.d("[BIP]", "[UDP]RecTr: recvLen:" + length);
                        if (length >= 0) {
                            synchronized (Channel.this.mLock) {
                                MtkCatLog.d("[BIP]", "[UDP]RecTr: mRxBufferCount:" + Channel.this.mRxBufferCount);
                                if (Channel.this.mRxBufferCount == 0) {
                                    if (length > Channel.this.mBufferSize && Channel.this.mBufferSize < 1024) {
                                        Channel.this.mRxBuffer = new byte[1024];
                                    }
                                    System.arraycopy(bArr, 0, Channel.this.mRxBuffer, 0, length);
                                    Channel.this.mRxBufferCount = length;
                                    Channel.this.mRxBufferOffset = 0;
                                    Channel.this.dataAvailable(Channel.this.mRxBufferCount);
                                    try {
                                        Channel.this.mLock.wait();
                                    } catch (InterruptedException e) {
                                        MtkCatLog.e("[BIP]", "[UDP]RecTr: InterruptedException !!!");
                                        e.printStackTrace();
                                    }
                                } else if (Channel.this.mRxBufferCount > 0) {
                                    do {
                                        Channel.this.dataAvailable(Channel.this.mRxBufferCount);
                                        try {
                                            Channel.this.mLock.wait();
                                        } catch (InterruptedException e2) {
                                            MtkCatLog.e("[BIP]", "[UDP]RecTr: InterruptedException !!!");
                                            e2.printStackTrace();
                                        }
                                    } while (Channel.this.mRxBufferCount > 0);
                                    if (length > 0) {
                                        System.arraycopy(bArr, 0, Channel.this.mRxBuffer, 0, length);
                                        Channel.this.mRxBufferCount = length;
                                        Channel.this.mRxBufferOffset = 0;
                                        Channel.this.dataAvailable(Channel.this.mRxBufferCount);
                                        try {
                                            Channel.this.mLock.wait();
                                        } catch (InterruptedException e3) {
                                            MtkCatLog.e("[BIP]", "[UDP]RecTr: InterruptedException !!!");
                                            e3.printStackTrace();
                                        }
                                    }
                                }
                            }
                        } else {
                            MtkCatLog.e("[BIP]", "[UDP]RecTr: end of file or server is disconnected.");
                            break;
                        }
                    } catch (IOException e4) {
                        MtkCatLog.e("[BIP]", "[UDP]RecTr:read io exception.");
                        Arrays.fill(bArr, (byte) 0);
                        Channel.this.mChannelStatusData.mChannelStatus = 0;
                        Channel.this.clearChannelBuffer(false);
                    }
                } catch (Exception e5) {
                    MtkCatLog.d("[BIP]", "[UDP]RecTr:Error.");
                    e5.printStackTrace();
                    return;
                }
            }
            if (Channel.this.mStop) {
                MtkCatLog.d("[BIP]", "[UDP]RecTr: stop");
            }
        }
    }

    protected class TcpReceiverThread implements Runnable {
        DataInputStream di;

        TcpReceiverThread(DataInputStream dataInputStream) {
            this.di = dataInputStream;
        }

        @Override
        public void run() {
            byte[] bArr = new byte[1400];
            MtkCatLog.d("[BIP]", "[TCP]RecTr: run");
            while (true) {
                try {
                    if (Channel.this.mStop) {
                        break;
                    }
                    MtkCatLog.d("[BIP]", "[TCP]RecTr: Wait data from network");
                    try {
                        Arrays.fill(bArr, (byte) 0);
                        int i = this.di.read(bArr);
                        MtkCatLog.d("[BIP]", "[TCP]RecTr: recvLen:" + i);
                        if (i >= 0) {
                            synchronized (Channel.this.mLock) {
                                MtkCatLog.d("[BIP]", "[TCP]RecTr: mRxBufferCount:" + Channel.this.mRxBufferCount);
                                if (Channel.this.mRxBufferCount == 0) {
                                    if (i > Channel.this.mBufferSize && Channel.this.mBufferSize < 1024) {
                                        Channel.this.mRxBuffer = new byte[1024];
                                    }
                                    System.arraycopy(bArr, 0, Channel.this.mRxBuffer, 0, i);
                                    Channel.this.mRxBufferCount = i;
                                    Channel.this.mRxBufferOffset = 0;
                                    Channel.this.dataAvailable(Channel.this.mRxBufferCount);
                                    try {
                                        Channel.this.mLock.wait();
                                    } catch (InterruptedException e) {
                                        MtkCatLog.e("[BIP]", "[TCP]RecTr: InterruptedException !!!");
                                        e.printStackTrace();
                                    }
                                } else if (Channel.this.mRxBufferCount > 0) {
                                    do {
                                        Channel.this.dataAvailable(Channel.this.mRxBufferCount);
                                        try {
                                            Channel.this.mLock.wait();
                                        } catch (InterruptedException e2) {
                                            MtkCatLog.e("[BIP]", "[TCP]RecTr: InterruptedException !!!");
                                            e2.printStackTrace();
                                        }
                                    } while (Channel.this.mRxBufferCount > 0);
                                    if (i > 0) {
                                        System.arraycopy(bArr, 0, Channel.this.mRxBuffer, 0, i);
                                        Channel.this.mRxBufferCount = i;
                                        Channel.this.mRxBufferOffset = 0;
                                        Channel.this.dataAvailable(Channel.this.mRxBufferCount);
                                        try {
                                            Channel.this.mLock.wait();
                                        } catch (InterruptedException e3) {
                                            MtkCatLog.e("[BIP]", "[TCP]RecTr: InterruptedException !!!");
                                            e3.printStackTrace();
                                        }
                                    }
                                }
                            }
                        } else {
                            MtkCatLog.e("[BIP]", "[TCP]RecTr: end of file or server is disconnected.");
                            break;
                        }
                    } catch (IOException e4) {
                        MtkCatLog.e("[BIP]", "[TCP]RecTr:read io exception.");
                        Arrays.fill(bArr, (byte) 0);
                        Channel.this.clearChannelBuffer(false);
                    }
                } catch (Exception e5) {
                    MtkCatLog.d("[BIP]", "[TCP]RecTr:Error");
                    e5.printStackTrace();
                    return;
                }
            }
            if (Channel.this.mStop) {
                MtkCatLog.d("[BIP]", "[TCP]RecTr: stop");
            }
        }
    }

    protected class UICCServerThread implements Runnable {
        private static final int RETRY_ACCEPT_SLEEPTIME = 100;
        private static final int RETRY_COUNT = 4;
        TcpServerChannel mTcpServerChannel;
        int mReTryCount = 0;
        DataInputStream di = null;

        UICCServerThread(TcpServerChannel tcpServerChannel) {
            this.mTcpServerChannel = null;
            MtkCatLog.d("[BIP]", "OpenServerSocketThread Init");
            this.mTcpServerChannel = tcpServerChannel;
        }

        @Override
        public void run() {
            int i;
            boolean z;
            int i2;
            byte[] bArr = new byte[1400];
            MtkCatLog.d("[BIP]", "[UICC]ServerTr: Run Enter");
            while (true) {
                if (Channel.this.mChannelStatus == 4) {
                    if (this.mTcpServerChannel.getTcpStatus() != 64) {
                        this.mTcpServerChannel.setTcpStatus(BipUtils.TCP_STATUS_LISTEN, true);
                    } else {
                        MtkCatLog.d("[BIP]", "[UICC]ServerTr:TCP status = TCP_STATUS_LISTEN");
                    }
                    try {
                        MtkCatLog.d("[BIP]", "[UICC]ServerTr:Listen to wait client connection...");
                        this.mTcpServerChannel.mSocket = this.mTcpServerChannel.mSSocket.accept();
                        MtkCatLog.d("[BIP]", "[UICC]ServerTr:Receive a client connection.");
                        this.mTcpServerChannel.setTcpStatus(BipUtils.TCP_STATUS_ESTABLISHED, true);
                        if (this.mTcpServerChannel.mInput == null) {
                            try {
                                this.mTcpServerChannel.mInput = new DataInputStream(this.mTcpServerChannel.mSocket.getInputStream());
                                this.di = this.mTcpServerChannel.mInput;
                            } catch (IOException e) {
                                MtkCatLog.e("[BIP]", "[UICC]ServerTr:IOException: getInputStream.");
                            }
                        }
                        if (this.mTcpServerChannel.mOutput == null) {
                            try {
                                this.mTcpServerChannel.mOutput = new BufferedOutputStream(this.mTcpServerChannel.mSocket.getOutputStream());
                            } catch (IOException e2) {
                                MtkCatLog.e("[BIP]", "[UICC]ServerTr:IOException: getOutputStream.");
                            }
                        }
                        while (true) {
                            if (Channel.this.mStop) {
                                break;
                            }
                            MtkCatLog.d("[BIP]", "[UICC]ServerTr: Start to read data from network");
                            try {
                                Arrays.fill(bArr, (byte) 0);
                                int i3 = this.di.read(bArr);
                                MtkCatLog.d("[BIP]", "[UICC]ServerTr: Receive data:" + i3);
                                if (i3 >= 0) {
                                    synchronized (Channel.this.mLock) {
                                        MtkCatLog.d("[BIP]", "[UICC]ServerTr:mRxBufferCount: " + Channel.this.mRxBufferCount);
                                        if (Channel.this.mRxBufferCount == 0) {
                                            System.arraycopy(bArr, 0, Channel.this.mRxBuffer, 0, i3);
                                            Channel.this.mRxBufferCount = i3;
                                            Channel.this.mRxBufferOffset = 0;
                                            Channel.this.dataAvailable(Channel.this.mRxBufferCount);
                                            i = 0;
                                        } else {
                                            System.arraycopy(Channel.this.mRxBuffer, Channel.this.mRxBufferOffset, Channel.this.mRxBuffer, 0, Channel.this.mRxBufferCount);
                                            if (i3 > Channel.this.mBufferSize - Channel.this.mRxBufferCount) {
                                                i = Channel.this.mBufferSize - Channel.this.mRxBufferCount;
                                                Channel.this.mRxBufferCacheCount = i3 - i;
                                                i3 = i;
                                            } else {
                                                i = 0;
                                            }
                                            System.arraycopy(bArr, 0, Channel.this.mRxBuffer, Channel.this.mRxBufferCount, i3);
                                            Channel.this.mRxBufferCount += i3;
                                            Channel.this.mRxBufferOffset = 0;
                                            MtkCatLog.d("[BIP]", "[UICC]ServerTr:rSize: " + i3 + ", mRxBufferCacheCount: " + Channel.this.mRxBufferCacheCount);
                                        }
                                        while (true) {
                                            if (Channel.this.mRxBufferCount >= Channel.this.mBufferSize) {
                                                try {
                                                    MtkCatLog.d("[BIP]", "[UICC]ServerTr:mRxBuffer is full.");
                                                    Channel.this.mLock.wait();
                                                } catch (InterruptedException e3) {
                                                    MtkCatLog.e("[BIP]", "[UICC]ServerTr:IE :mRxBufferCount >= mBufferSize");
                                                    if (true == this.mTcpServerChannel.isCloseBackToTcpListen()) {
                                                        Channel.this.clearChannelBuffer(true);
                                                        this.mTcpServerChannel.setCloseBackToTcpListen(false);
                                                        z = false;
                                                        if (z) {
                                                        }
                                                    }
                                                }
                                                if (Channel.this.mRxBufferCacheCount > 0) {
                                                    if (Channel.this.mRxBufferCount > 0) {
                                                        System.arraycopy(Channel.this.mRxBuffer, Channel.this.mRxBufferOffset, Channel.this.mRxBuffer, 0, Channel.this.mRxBufferCount);
                                                    }
                                                    if (Channel.this.mRxBufferCacheCount <= Channel.this.mBufferSize - Channel.this.mRxBufferCount) {
                                                        i2 = Channel.this.mRxBufferCacheCount;
                                                    } else {
                                                        i2 = Channel.this.mBufferSize - Channel.this.mRxBufferCount;
                                                    }
                                                    System.arraycopy(bArr, i, Channel.this.mRxBuffer, Channel.this.mRxBufferCount, i2);
                                                    Channel.this.mRxBufferCount += i2;
                                                    Channel.this.mRxBufferCacheCount -= i2;
                                                    i += i2;
                                                    Channel.this.mRxBufferOffset = 0;
                                                }
                                            } else {
                                                z = true;
                                                break;
                                            }
                                        }
                                    }
                                    if (z) {
                                        break;
                                    }
                                    MtkCatLog.d("[BIP]", "[UICC]ServerTr: buffer data:" + Channel.this.mRxBufferCount);
                                } else {
                                    MtkCatLog.e("[BIP]", "[UICC]ServerTr: client diconnected");
                                    try {
                                        if (this.mTcpServerChannel.mInput != null) {
                                            this.mTcpServerChannel.mInput.close();
                                        }
                                        if (this.mTcpServerChannel.mOutput != null) {
                                            this.mTcpServerChannel.mOutput.close();
                                        }
                                    } catch (IOException e4) {
                                        MtkCatLog.e("[BIP]", "[UICC]ServerTr:len<0,IOException input stream.");
                                    }
                                    Channel.this.clearChannelBuffer(true);
                                    this.mTcpServerChannel.setTcpStatus(BipUtils.TCP_STATUS_LISTEN, true);
                                }
                            } catch (IOException e5) {
                                MtkCatLog.e("[BIP]", "[UICC]ServerTr:read io exception.");
                                Arrays.fill(bArr, (byte) 0);
                                try {
                                    if (this.mTcpServerChannel.mInput != null) {
                                        this.mTcpServerChannel.mInput.close();
                                    }
                                    if (this.mTcpServerChannel.mOutput != null) {
                                        this.mTcpServerChannel.mOutput.close();
                                    }
                                    Channel.this.clearChannelBuffer(true);
                                } catch (IOException e6) {
                                    MtkCatLog.e("[BIP]", "[UICC]ServerTr:IOException input stream.");
                                }
                            }
                        }
                        if (Channel.this.mStop) {
                            MtkCatLog.d("[BIP]", "[UICC]ServerTr: stop");
                        }
                    } catch (IOException e7) {
                        MtkCatLog.e("[BIP]", "[UICC]ServerTr:Fail to accept server socket retry:" + this.mReTryCount);
                        if (4 >= this.mReTryCount) {
                            this.mReTryCount++;
                            try {
                                Thread.sleep(100L);
                            } catch (InterruptedException e8) {
                                MtkCatLog.e("[BIP]", "[UICC]ServerTr:IE: sleep for SS accept retry.");
                            }
                        } else {
                            this.mReTryCount = 0;
                            if (this.mTcpServerChannel.mInput != null) {
                            }
                            if (this.mTcpServerChannel.mOutput != null) {
                            }
                            this.mTcpServerChannel.mSSocket.close();
                            Channel.this.clearChannelBuffer(false);
                            Channel.this.closeChannel();
                            Channel.this.mBipChannelManager.removeChannel(Channel.this.mChannelId);
                            this.mTcpServerChannel.setTcpStatus((byte) 0, true);
                        }
                    }
                }
            }
            this.mReTryCount = 0;
            try {
                if (this.mTcpServerChannel.mInput != null) {
                    this.mTcpServerChannel.mInput.close();
                }
                if (this.mTcpServerChannel.mOutput != null) {
                    this.mTcpServerChannel.mOutput.close();
                }
            } catch (IOException e9) {
                MtkCatLog.e("[BIP]", "[UICC]ServerTr:IOE: input/output stream close.");
            }
            try {
                this.mTcpServerChannel.mSSocket.close();
            } catch (IOException e10) {
                MtkCatLog.e("[BIP]", "[UICC]ServerTr:IOE: socket close.");
            }
            Channel.this.clearChannelBuffer(false);
            Channel.this.closeChannel();
            Channel.this.mBipChannelManager.removeChannel(Channel.this.mChannelId);
            this.mTcpServerChannel.setTcpStatus((byte) 0, true);
        }
    }
}
