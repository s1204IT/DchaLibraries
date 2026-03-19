package com.android.internal.telephony.cat;

import android.net.Network;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Arrays;

abstract class Channel {
    protected static final int DEFAULT_IOTTEST_VALUE = Integer.valueOf("1").intValue();
    protected static final String DISABLE_IOTTEST_CONFIG = "0";
    protected static final String ENABLE_IOTTEST_CONFIG = "1";
    protected static final String PROPERTY_IOT_TEST = "persist.service.bip.iot.test";
    protected static final int SOCKET_TIMEOUT = 3000;
    protected InetAddress mAddress;
    protected BipChannelManager mBipChannelManager;
    protected BipService mBipService;
    protected int mBufferSize;
    protected int mChannelId;
    protected ChannelStatus mChannelStatusData;
    private CatService mHandler;
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
    protected int mIOTTest = DEFAULT_IOTTEST_VALUE;
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

    Channel(int cid, int linkMode, int protocolType, InetAddress address, int port, int bufferSize, CatService handler, BipService bipManager) {
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
        this.mChannelId = cid;
        this.mLinkMode = linkMode;
        this.mProtocolType = protocolType;
        this.mAddress = address;
        this.mPort = port;
        this.mBufferSize = bufferSize;
        this.mHandler = handler;
        this.mBipService = bipManager;
        this.mBipChannelManager = this.mBipService.getBipChannelManager();
        this.mChannelStatusData = new ChannelStatus(cid, 0, 0);
    }

    public void dataAvailable(int bufferSize) {
        if (this.mBipService.mCurrentSetupEventCmd == null) {
            CatLog.e(this, "mCurrentSetupEventCmd is null");
            return;
        }
        if (!this.mBipService.hasPsEvent(9)) {
            CatLog.d(this, "No need to send data available.");
            return;
        }
        CatResponseMessage resMsg = new CatResponseMessage(9);
        byte[] additionalInfo = new byte[7];
        additionalInfo[0] = -72;
        additionalInfo[1] = 2;
        additionalInfo[2] = (byte) (getChannelId() | this.mChannelStatusData.mChannelStatus);
        additionalInfo[3] = 0;
        additionalInfo[4] = -73;
        additionalInfo[5] = 1;
        if (bufferSize > 255) {
            additionalInfo[6] = -1;
        } else {
            additionalInfo[6] = (byte) bufferSize;
        }
        resMsg.setSourceId(130);
        resMsg.setDestinationId(129);
        resMsg.setEventDownload(9, additionalInfo);
        resMsg.setAdditionalInfo(additionalInfo);
        resMsg.setOneShot(false);
        CatLog.d(this, "onEventDownload for dataAvailable");
        this.mHandler.onEventDownload(resMsg);
    }

    public void changeChannelStatus(byte status) {
        if (this.mBipService.mCurrentSetupEventCmd == null) {
            CatLog.e(this, "mCurrentSetupEventCmd is null");
            return;
        }
        if (!this.mBipService.hasPsEvent(10)) {
            CatLog.d(this, "No need to send channel status.");
            return;
        }
        CatResponseMessage resMsg = new CatResponseMessage(10);
        CatLog.d("[BIP]", "[Channel]:changeChannelStatus:" + ((int) status));
        byte[] additionalInfo = {-72, 2, (byte) (getChannelId() | status), 0};
        resMsg.setSourceId(130);
        resMsg.setDestinationId(129);
        resMsg.setEventDownload(10, additionalInfo);
        resMsg.setAdditionalInfo(additionalInfo);
        resMsg.setOneShot(false);
        this.mHandler.onEventDownload(resMsg);
    }

    public int getChannelStatus() {
        return this.mChannelStatus;
    }

    public int getChannelId() {
        return this.mChannelId;
    }

    public void clearChannelBuffer(boolean resetBuffer) {
        if (resetBuffer) {
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
        int minBufferSize = 0;
        int maxBufferSize = 0;
        int defaultBufferSize = 0;
        if (this.mProtocolType == 5 || this.mProtocolType == 2 || this.mProtocolType == 3 || this.mProtocolType == 4 || this.mProtocolType == 1) {
            minBufferSize = 255;
            maxBufferSize = 1400;
            defaultBufferSize = 1024;
        }
        CatLog.d("[BIP]", "mBufferSize:" + this.mBufferSize + " minBufferSize:" + minBufferSize + " maxBufferSize:" + maxBufferSize);
        if (this.mBufferSize >= minBufferSize && this.mBufferSize <= maxBufferSize) {
            CatLog.d("[BIP]", "buffer size is normal");
            return 0;
        }
        if (this.mBufferSize > maxBufferSize) {
            CatLog.d("[BIP]", "buffer size is too large, change it to maximum value");
            this.mBufferSize = maxBufferSize;
        } else {
            CatLog.d("[BIP]", "buffer size is too small, change it to default value");
            this.mBufferSize = defaultBufferSize;
        }
        if (this.mBufferSize < 237) {
            CatLog.d("[BIP]", "buffer size is smaller than 255, change it to MAX_APDU_SIZE");
            this.mBufferSize = BipUtils.MAX_APDU_SIZE;
        }
        return 3;
    }

    protected synchronized void requestStop() {
        this.mStop = true;
        CatLog.d("[BIP]", "requestStop: " + this.mStop);
    }

    protected class UdpReceiverThread implements Runnable {
        DatagramSocket udpSocket;

        UdpReceiverThread(DatagramSocket s) {
            this.udpSocket = s;
        }

        @Override
        public void run() {
            byte[] localBuffer = new byte[1400];
            CatLog.d("[BIP]", "[UDP]RecTr run");
            DatagramPacket recvPacket = new DatagramPacket(localBuffer, localBuffer.length);
            while (true) {
                try {
                    if (Channel.this.mStop) {
                        break;
                    }
                    CatLog.d("[BIP]", "[UDP]RecTr: Wait data from network");
                    try {
                        Arrays.fill(localBuffer, (byte) 0);
                        this.udpSocket.receive(recvPacket);
                        int recvLen = recvPacket.getLength();
                        CatLog.d("[BIP]", "[UDP]RecTr: recvLen:" + recvLen);
                        if (recvLen >= 0) {
                            synchronized (Channel.this.mLock) {
                                CatLog.d("[BIP]", "[UDP]RecTr: mRxBufferCount:" + Channel.this.mRxBufferCount);
                                if (Channel.this.mRxBufferCount == 0) {
                                    System.arraycopy(localBuffer, 0, Channel.this.mRxBuffer, 0, recvLen);
                                    Channel.this.mRxBufferCount = recvLen;
                                    Channel.this.mRxBufferOffset = 0;
                                    Channel.this.dataAvailable(Channel.this.mRxBufferCount);
                                    try {
                                        Channel.this.mLock.wait();
                                    } catch (InterruptedException e) {
                                        CatLog.e("[BIP]", "[UDP]RecTr: InterruptedException !!!");
                                        e.printStackTrace();
                                    }
                                } else if (Channel.this.mRxBufferCount > 0) {
                                    do {
                                        Channel.this.dataAvailable(Channel.this.mRxBufferCount);
                                        try {
                                            Channel.this.mLock.wait();
                                        } catch (InterruptedException e2) {
                                            CatLog.e("[BIP]", "[UDP]RecTr: InterruptedException !!!");
                                            e2.printStackTrace();
                                        }
                                    } while (Channel.this.mRxBufferCount > 0);
                                    if (recvLen > 0) {
                                        System.arraycopy(localBuffer, 0, Channel.this.mRxBuffer, 0, recvLen);
                                        Channel.this.mRxBufferCount = recvLen;
                                        Channel.this.mRxBufferOffset = 0;
                                        Channel.this.dataAvailable(Channel.this.mRxBufferCount);
                                        try {
                                            Channel.this.mLock.wait();
                                        } catch (InterruptedException e3) {
                                            CatLog.e("[BIP]", "[UDP]RecTr: InterruptedException !!!");
                                            e3.printStackTrace();
                                        }
                                    }
                                }
                            }
                        } else {
                            CatLog.e("[BIP]", "[UDP]RecTr: end of file or server is disconnected.");
                            break;
                        }
                    } catch (IOException e4) {
                        CatLog.e("[BIP]", "[UDP]RecTr:read io exception.");
                        Arrays.fill(localBuffer, (byte) 0);
                        Channel.this.mChannelStatusData.mChannelStatus = 0;
                        Channel.this.clearChannelBuffer(false);
                    }
                } catch (Exception e5) {
                    CatLog.d("[BIP]", "[UDP]RecTr:Error.");
                    e5.printStackTrace();
                    return;
                }
            }
            if (!Channel.this.mStop) {
                return;
            }
            CatLog.d("[BIP]", "[UDP]RecTr: stop");
        }
    }

    protected class TcpReceiverThread implements Runnable {
        DataInputStream di;

        TcpReceiverThread(DataInputStream s) {
            this.di = s;
        }

        @Override
        public void run() {
            byte[] localBuffer = new byte[1400];
            CatLog.d("[BIP]", "[TCP]RecTr: run");
            while (true) {
                try {
                    if (Channel.this.mStop) {
                        break;
                    }
                    CatLog.d("[BIP]", "[TCP]RecTr: Wait data from network");
                    try {
                        Arrays.fill(localBuffer, (byte) 0);
                        int recvLen = this.di.read(localBuffer);
                        CatLog.d("[BIP]", "[TCP]RecTr: recvLen:" + recvLen);
                        if (recvLen >= 0) {
                            synchronized (Channel.this.mLock) {
                                CatLog.d("[BIP]", "[TCP]RecTr: mRxBufferCount:" + Channel.this.mRxBufferCount);
                                if (Channel.this.mRxBufferCount == 0) {
                                    System.arraycopy(localBuffer, 0, Channel.this.mRxBuffer, 0, recvLen);
                                    Channel.this.mRxBufferCount = recvLen;
                                    Channel.this.mRxBufferOffset = 0;
                                    Channel.this.dataAvailable(Channel.this.mRxBufferCount);
                                    try {
                                        Channel.this.mLock.wait();
                                    } catch (InterruptedException e) {
                                        CatLog.e("[BIP]", "[TCP]RecTr: InterruptedException !!!");
                                        e.printStackTrace();
                                    }
                                } else if (Channel.this.mRxBufferCount > 0) {
                                    do {
                                        Channel.this.dataAvailable(Channel.this.mRxBufferCount);
                                        try {
                                            Channel.this.mLock.wait();
                                        } catch (InterruptedException e2) {
                                            CatLog.e("[BIP]", "[TCP]RecTr: InterruptedException !!!");
                                            e2.printStackTrace();
                                        }
                                    } while (Channel.this.mRxBufferCount > 0);
                                    if (recvLen > 0) {
                                        System.arraycopy(localBuffer, 0, Channel.this.mRxBuffer, 0, recvLen);
                                        Channel.this.mRxBufferCount = recvLen;
                                        Channel.this.mRxBufferOffset = 0;
                                        Channel.this.dataAvailable(Channel.this.mRxBufferCount);
                                        try {
                                            Channel.this.mLock.wait();
                                        } catch (InterruptedException e3) {
                                            CatLog.e("[BIP]", "[TCP]RecTr: InterruptedException !!!");
                                            e3.printStackTrace();
                                        }
                                    }
                                }
                            }
                        } else {
                            CatLog.e("[BIP]", "[TCP]RecTr: end of file or server is disconnected.");
                            break;
                        }
                    } catch (IOException e4) {
                        CatLog.e("[BIP]", "[TCP]RecTr:read io exception.");
                        Arrays.fill(localBuffer, (byte) 0);
                        Channel.this.clearChannelBuffer(false);
                    }
                } catch (Exception e5) {
                    CatLog.d("[BIP]", "[TCP]RecTr:Error");
                    e5.printStackTrace();
                    return;
                }
            }
            if (!Channel.this.mStop) {
                return;
            }
            CatLog.d("[BIP]", "[TCP]RecTr: stop");
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
            CatLog.d("[BIP]", "OpenServerSocketThread Init");
            this.mTcpServerChannel = tcpServerChannel;
        }

        @Override
        public void run() {
            int rSize;
            boolean goOnRead;
            int rSize2;
            byte[] localBuffer = new byte[1400];
            CatLog.d("[BIP]", "[UICC]ServerTr: Run Enter");
            while (true) {
                if (Channel.this.mChannelStatus == 4) {
                    if (this.mTcpServerChannel.getTcpStatus() != 64) {
                        this.mTcpServerChannel.setTcpStatus(BipUtils.TCP_STATUS_LISTEN, true);
                    } else {
                        CatLog.d("[BIP]", "[UICC]ServerTr:TCP status = TCP_STATUS_LISTEN");
                    }
                    try {
                        CatLog.d("[BIP]", "[UICC]ServerTr:Listen to wait client connection...");
                        this.mTcpServerChannel.mSocket = this.mTcpServerChannel.mSSocket.accept();
                        CatLog.d("[BIP]", "[UICC]ServerTr:Receive a client connection.");
                        this.mTcpServerChannel.setTcpStatus(BipUtils.TCP_STATUS_ESTABLISHED, true);
                        if (this.mTcpServerChannel.mInput == null) {
                            try {
                                this.mTcpServerChannel.mInput = new DataInputStream(this.mTcpServerChannel.mSocket.getInputStream());
                                this.di = this.mTcpServerChannel.mInput;
                            } catch (IOException e) {
                                CatLog.e("[BIP]", "[UICC]ServerTr:IOException: getInputStream.");
                            }
                        }
                        if (this.mTcpServerChannel.mOutput == null) {
                            try {
                                this.mTcpServerChannel.mOutput = new BufferedOutputStream(this.mTcpServerChannel.mSocket.getOutputStream());
                            } catch (IOException e2) {
                                CatLog.e("[BIP]", "[UICC]ServerTr:IOException: getOutputStream.");
                            }
                        }
                        while (true) {
                            if (Channel.this.mStop) {
                                break;
                            }
                            CatLog.d("[BIP]", "[UICC]ServerTr: Start to read data from network");
                            try {
                                Arrays.fill(localBuffer, (byte) 0);
                                int recvLen = this.di.read(localBuffer);
                                CatLog.d("[BIP]", "[UICC]ServerTr: Receive data:" + recvLen);
                                if (recvLen >= 0) {
                                    int localBufferOffset = 0;
                                    synchronized (Channel.this.mLock) {
                                        CatLog.d("[BIP]", "[UICC]ServerTr:mRxBufferCount: " + Channel.this.mRxBufferCount);
                                        if (Channel.this.mRxBufferCount == 0) {
                                            System.arraycopy(localBuffer, 0, Channel.this.mRxBuffer, 0, recvLen);
                                            Channel.this.mRxBufferCount = recvLen;
                                            Channel.this.mRxBufferOffset = 0;
                                            Channel.this.dataAvailable(Channel.this.mRxBufferCount);
                                        } else {
                                            System.arraycopy(Channel.this.mRxBuffer, Channel.this.mRxBufferOffset, Channel.this.mRxBuffer, 0, Channel.this.mRxBufferCount);
                                            if (recvLen <= Channel.this.mBufferSize - Channel.this.mRxBufferCount) {
                                                rSize = recvLen;
                                            } else {
                                                rSize = Channel.this.mBufferSize - Channel.this.mRxBufferCount;
                                                localBufferOffset = rSize;
                                                Channel.this.mRxBufferCacheCount = recvLen - rSize;
                                            }
                                            System.arraycopy(localBuffer, 0, Channel.this.mRxBuffer, Channel.this.mRxBufferCount, rSize);
                                            Channel.this.mRxBufferCount += rSize;
                                            Channel.this.mRxBufferOffset = 0;
                                            CatLog.d("[BIP]", "[UICC]ServerTr:rSize: " + rSize + ", mRxBufferCacheCount: " + Channel.this.mRxBufferCacheCount);
                                        }
                                        while (true) {
                                            if (Channel.this.mRxBufferCount >= Channel.this.mBufferSize) {
                                                try {
                                                    CatLog.d("[BIP]", "[UICC]ServerTr:mRxBuffer is full.");
                                                    Channel.this.mLock.wait();
                                                } catch (InterruptedException e3) {
                                                    CatLog.e("[BIP]", "[UICC]ServerTr:IE :mRxBufferCount >= mBufferSize");
                                                    if (this.mTcpServerChannel.isCloseBackToTcpListen()) {
                                                        Channel.this.clearChannelBuffer(true);
                                                        this.mTcpServerChannel.setCloseBackToTcpListen(false);
                                                        goOnRead = false;
                                                        if (goOnRead) {
                                                        }
                                                    }
                                                }
                                                if (Channel.this.mRxBufferCacheCount > 0) {
                                                    if (Channel.this.mRxBufferCount > 0) {
                                                        System.arraycopy(Channel.this.mRxBuffer, Channel.this.mRxBufferOffset, Channel.this.mRxBuffer, 0, Channel.this.mRxBufferCount);
                                                    }
                                                    if (Channel.this.mRxBufferCacheCount <= Channel.this.mBufferSize - Channel.this.mRxBufferCount) {
                                                        rSize2 = Channel.this.mRxBufferCacheCount;
                                                    } else {
                                                        rSize2 = Channel.this.mBufferSize - Channel.this.mRxBufferCount;
                                                    }
                                                    System.arraycopy(localBuffer, localBufferOffset, Channel.this.mRxBuffer, Channel.this.mRxBufferCount, rSize2);
                                                    Channel.this.mRxBufferCount += rSize2;
                                                    Channel.this.mRxBufferCacheCount -= rSize2;
                                                    localBufferOffset += rSize2;
                                                    Channel.this.mRxBufferOffset = 0;
                                                }
                                            } else {
                                                goOnRead = true;
                                                break;
                                            }
                                        }
                                    }
                                    if (goOnRead) {
                                        break;
                                    } else {
                                        CatLog.d("[BIP]", "[UICC]ServerTr: buffer data:" + Channel.this.mRxBufferCount);
                                    }
                                } else {
                                    CatLog.e("[BIP]", "[UICC]ServerTr: client diconnected");
                                    try {
                                        if (this.mTcpServerChannel.mInput != null) {
                                            this.mTcpServerChannel.mInput.close();
                                        }
                                        if (this.mTcpServerChannel.mOutput != null) {
                                            this.mTcpServerChannel.mOutput.close();
                                        }
                                    } catch (IOException e4) {
                                        CatLog.e("[BIP]", "[UICC]ServerTr:len<0,IOException input stream.");
                                    }
                                    Channel.this.clearChannelBuffer(true);
                                    this.mTcpServerChannel.setTcpStatus(BipUtils.TCP_STATUS_LISTEN, true);
                                }
                            } catch (IOException e5) {
                                CatLog.e("[BIP]", "[UICC]ServerTr:read io exception.");
                                Arrays.fill(localBuffer, (byte) 0);
                                try {
                                    if (this.mTcpServerChannel.mInput != null) {
                                        this.mTcpServerChannel.mInput.close();
                                    }
                                    if (this.mTcpServerChannel.mOutput != null) {
                                        this.mTcpServerChannel.mOutput.close();
                                    }
                                    Channel.this.clearChannelBuffer(true);
                                } catch (IOException e6) {
                                    CatLog.e("[BIP]", "[UICC]ServerTr:IOException input stream.");
                                }
                            }
                        }
                        if (Channel.this.mStop) {
                            CatLog.d("[BIP]", "[UICC]ServerTr: stop");
                        }
                    } catch (IOException e7) {
                        CatLog.e("[BIP]", "[UICC]ServerTr:Fail to accept server socket retry:" + this.mReTryCount);
                        if (4 >= this.mReTryCount) {
                            this.mReTryCount++;
                            try {
                                Thread.sleep(100L);
                            } catch (InterruptedException e8) {
                                CatLog.e("[BIP]", "[UICC]ServerTr:IE: sleep for SS accept retry.");
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
                CatLog.e("[BIP]", "[UICC]ServerTr:IOE: input/output stream close.");
            }
            try {
                this.mTcpServerChannel.mSSocket.close();
            } catch (IOException e10) {
                CatLog.e("[BIP]", "[UICC]ServerTr:IOE: socket close.");
            }
            Channel.this.clearChannelBuffer(false);
            Channel.this.closeChannel();
            Channel.this.mBipChannelManager.removeChannel(Channel.this.mChannelId);
            this.mTcpServerChannel.setTcpStatus((byte) 0, true);
        }
    }
}
