package android.bluetooth;

import android.hardware.contexthub.V1_0.HostEndPoint;
import android.net.LocalSocket;
import android.os.ParcelFileDescriptor;
import android.os.ParcelUuid;
import android.os.RemoteException;
import android.util.Log;
import java.io.Closeable;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Locale;
import java.util.UUID;

public final class BluetoothSocket implements Closeable {
    static final int BTSOCK_FLAG_NO_SDP = 4;
    static final int EADDRINUSE = 98;
    static final int EBADFD = 77;
    static final int MAX_L2CAP_PACKAGE_SIZE = 65535;
    public static final int MAX_RFCOMM_CHANNEL = 30;
    private static final int PROXY_CONNECTION_TIMEOUT = 5000;
    static final int SEC_FLAG_AUTH = 2;
    static final int SEC_FLAG_AUTH_16_DIGIT = 16;
    static final int SEC_FLAG_AUTH_MITM = 8;
    static final int SEC_FLAG_ENCRYPT = 1;
    private static final int SOCK_SIGNAL_SIZE = 20;
    public static final int TYPE_L2CAP = 3;
    public static final int TYPE_L2CAP_BREDR = 3;
    public static final int TYPE_L2CAP_LE = 4;
    public static final int TYPE_RFCOMM = 1;
    public static final int TYPE_SCO = 2;
    private String mAddress;
    private final boolean mAuth;
    private boolean mAuthMitm;
    private BluetoothDevice mDevice;
    private final boolean mEncrypt;
    private boolean mExcludeSdp;
    private int mFd;
    private final BluetoothInputStream mInputStream;
    private ByteBuffer mL2capBuffer;
    private int mMaxRxPacketSize;
    private int mMaxTxPacketSize;
    private boolean mMin16DigitPin;
    private final BluetoothOutputStream mOutputStream;
    private ParcelFileDescriptor mPfd;
    private int mPort;
    private String mServiceName;
    private LocalSocket mSocket;
    private InputStream mSocketIS;
    private OutputStream mSocketOS;
    private volatile SocketState mSocketState;
    private final int mType;
    private final ParcelUuid mUuid;
    private static final String TAG = "BluetoothSocket";
    private static final boolean DBG = Log.isLoggable(TAG, 3);
    private static final boolean VDBG = Log.isLoggable(TAG, 2);

    private enum SocketState {
        INIT,
        CONNECTED,
        LISTENING,
        CLOSED
    }

    BluetoothSocket(int i, int i2, boolean z, boolean z2, BluetoothDevice bluetoothDevice, int i3, ParcelUuid parcelUuid) throws IOException {
        this(i, i2, z, z2, bluetoothDevice, i3, parcelUuid, false, false);
    }

    BluetoothSocket(int i, int i2, boolean z, boolean z2, BluetoothDevice bluetoothDevice, int i3, ParcelUuid parcelUuid, boolean z3, boolean z4) throws IOException {
        this.mExcludeSdp = false;
        this.mAuthMitm = false;
        this.mMin16DigitPin = false;
        this.mL2capBuffer = null;
        this.mMaxTxPacketSize = 0;
        this.mMaxRxPacketSize = 0;
        if (VDBG) {
            Log.d(TAG, "Creating new BluetoothSocket of type: " + i);
        }
        if (i == 1 && parcelUuid == null && i2 == -1 && i3 != -2 && (i3 < 1 || i3 > 30)) {
            throw new IOException("Invalid RFCOMM channel: " + i3);
        }
        if (parcelUuid != null) {
            this.mUuid = parcelUuid;
        } else {
            this.mUuid = new ParcelUuid(new UUID(0L, 0L));
        }
        this.mType = i;
        this.mAuth = z;
        this.mAuthMitm = z3;
        this.mMin16DigitPin = z4;
        this.mEncrypt = z2;
        this.mDevice = bluetoothDevice;
        this.mPort = i3;
        this.mFd = i2;
        this.mSocketState = SocketState.INIT;
        if (bluetoothDevice == null) {
            this.mAddress = BluetoothAdapter.getDefaultAdapter().getAddress();
        } else {
            this.mAddress = bluetoothDevice.getAddress();
        }
        this.mInputStream = new BluetoothInputStream(this);
        this.mOutputStream = new BluetoothOutputStream(this);
    }

    private BluetoothSocket(BluetoothSocket bluetoothSocket) {
        this.mExcludeSdp = false;
        this.mAuthMitm = false;
        this.mMin16DigitPin = false;
        this.mL2capBuffer = null;
        this.mMaxTxPacketSize = 0;
        this.mMaxRxPacketSize = 0;
        if (VDBG) {
            Log.d(TAG, "Creating new Private BluetoothSocket of type: " + bluetoothSocket.mType);
        }
        this.mUuid = bluetoothSocket.mUuid;
        this.mType = bluetoothSocket.mType;
        this.mAuth = bluetoothSocket.mAuth;
        this.mEncrypt = bluetoothSocket.mEncrypt;
        this.mPort = bluetoothSocket.mPort;
        this.mInputStream = new BluetoothInputStream(this);
        this.mOutputStream = new BluetoothOutputStream(this);
        this.mMaxRxPacketSize = bluetoothSocket.mMaxRxPacketSize;
        this.mMaxTxPacketSize = bluetoothSocket.mMaxTxPacketSize;
        this.mServiceName = bluetoothSocket.mServiceName;
        this.mExcludeSdp = bluetoothSocket.mExcludeSdp;
        this.mAuthMitm = bluetoothSocket.mAuthMitm;
        this.mMin16DigitPin = bluetoothSocket.mMin16DigitPin;
    }

    private BluetoothSocket acceptSocket(String str) throws IOException {
        BluetoothSocket bluetoothSocket = new BluetoothSocket(this);
        bluetoothSocket.mSocketState = SocketState.CONNECTED;
        FileDescriptor[] ancillaryFileDescriptors = this.mSocket.getAncillaryFileDescriptors();
        if (DBG) {
            Log.d(TAG, "socket fd passed by stack fds: " + Arrays.toString(ancillaryFileDescriptors));
        }
        if (ancillaryFileDescriptors == null || ancillaryFileDescriptors.length != 1) {
            Log.e(TAG, "socket fd passed from stack failed, fds: " + Arrays.toString(ancillaryFileDescriptors));
            bluetoothSocket.close();
            throw new IOException("bt socket acept failed");
        }
        bluetoothSocket.mPfd = new ParcelFileDescriptor(ancillaryFileDescriptors[0]);
        bluetoothSocket.mSocket = LocalSocket.createConnectedLocalSocket(ancillaryFileDescriptors[0]);
        bluetoothSocket.mSocketIS = bluetoothSocket.mSocket.getInputStream();
        bluetoothSocket.mSocketOS = bluetoothSocket.mSocket.getOutputStream();
        bluetoothSocket.mAddress = str;
        bluetoothSocket.mDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(str);
        return bluetoothSocket;
    }

    private BluetoothSocket(int i, int i2, boolean z, boolean z2, String str, int i3) throws IOException {
        this(i, i2, z, z2, new BluetoothDevice(str), i3, null, false, false);
    }

    protected void finalize() throws Throwable {
        try {
            close();
        } finally {
            super.finalize();
        }
    }

    private int getSecurityFlags() {
        int i;
        if (this.mAuth) {
            i = 2;
        } else {
            i = 0;
        }
        if (this.mEncrypt) {
            i |= 1;
        }
        if (this.mExcludeSdp) {
            i |= 4;
        }
        if (this.mAuthMitm) {
            i |= 8;
        }
        if (this.mMin16DigitPin) {
            return i | 16;
        }
        return i;
    }

    public BluetoothDevice getRemoteDevice() {
        return this.mDevice;
    }

    public InputStream getInputStream() throws IOException {
        return this.mInputStream;
    }

    public OutputStream getOutputStream() throws IOException {
        return this.mOutputStream;
    }

    public boolean isConnected() {
        return this.mSocketState == SocketState.CONNECTED;
    }

    void setServiceName(String str) {
        this.mServiceName = str;
    }

    public void connect() throws IOException {
        if (this.mDevice == null) {
            throw new IOException("Connect is called on null device");
        }
        try {
            if (this.mSocketState == SocketState.CLOSED) {
                throw new IOException("socket closed");
            }
            IBluetooth bluetoothService = BluetoothAdapter.getDefaultAdapter().getBluetoothService(null);
            if (bluetoothService == null) {
                throw new IOException("Bluetooth is off");
            }
            this.mPfd = bluetoothService.getSocketManager().connectSocket(this.mDevice, this.mType, this.mUuid, this.mPort, getSecurityFlags());
            synchronized (this) {
                if (DBG) {
                    Log.d(TAG, "connect(), SocketState: " + this.mSocketState + ", mPfd: " + this.mPfd);
                }
                if (this.mSocketState == SocketState.CLOSED) {
                    throw new IOException("socket closed");
                }
                if (this.mPfd == null) {
                    throw new IOException("bt socket connect failed");
                }
                this.mSocket = LocalSocket.createConnectedLocalSocket(this.mPfd.getFileDescriptor());
                this.mSocketIS = this.mSocket.getInputStream();
                this.mSocketOS = this.mSocket.getOutputStream();
            }
            int i = readInt(this.mSocketIS);
            if (i <= 0) {
                throw new IOException("bt socket connect failed");
            }
            this.mPort = i;
            waitSocketSignal(this.mSocketIS);
            synchronized (this) {
                if (this.mSocketState == SocketState.CLOSED) {
                    throw new IOException("bt socket closed");
                }
                this.mSocketState = SocketState.CONNECTED;
            }
        } catch (RemoteException e) {
            Log.e(TAG, Log.getStackTraceString(new Throwable()));
            throw new IOException("unable to send RPC: " + e.getMessage());
        }
    }

    int bindListen() {
        if (this.mSocketState == SocketState.CLOSED) {
            return 77;
        }
        IBluetooth bluetoothService = BluetoothAdapter.getDefaultAdapter().getBluetoothService(null);
        if (bluetoothService == null) {
            Log.e(TAG, "bindListen fail, reason: bluetooth is off");
            return -1;
        }
        try {
            if (DBG) {
                Log.d(TAG, "bindListen(): mPort=" + this.mPort + ", mType=" + this.mType);
            }
            this.mPfd = bluetoothService.getSocketManager().createSocketChannel(this.mType, this.mServiceName, this.mUuid, this.mPort, getSecurityFlags());
            try {
                synchronized (this) {
                    if (DBG) {
                        Log.d(TAG, "bindListen(), SocketState: " + this.mSocketState + ", mPfd: " + this.mPfd);
                    }
                    if (this.mSocketState != SocketState.INIT) {
                        return 77;
                    }
                    if (this.mPfd == null) {
                        return -1;
                    }
                    FileDescriptor fileDescriptor = this.mPfd.getFileDescriptor();
                    if (fileDescriptor == null) {
                        Log.e(TAG, "bindListen(), null file descriptor");
                        return -1;
                    }
                    if (DBG) {
                        Log.d(TAG, "bindListen(), Create LocalSocket");
                    }
                    this.mSocket = LocalSocket.createConnectedLocalSocket(fileDescriptor);
                    if (DBG) {
                        Log.d(TAG, "bindListen(), new LocalSocket.getInputStream()");
                    }
                    this.mSocketIS = this.mSocket.getInputStream();
                    this.mSocketOS = this.mSocket.getOutputStream();
                    if (DBG) {
                        Log.d(TAG, "bindListen(), readInt mSocketIS: " + this.mSocketIS);
                    }
                    int i = readInt(this.mSocketIS);
                    synchronized (this) {
                        if (this.mSocketState == SocketState.INIT) {
                            this.mSocketState = SocketState.LISTENING;
                        }
                    }
                    if (DBG) {
                        Log.d(TAG, "bindListen(): channel=" + i + ", mPort=" + this.mPort);
                    }
                    if (this.mPort <= -1) {
                        this.mPort = i;
                        return 0;
                    }
                    return 0;
                }
            } catch (IOException e) {
                if (this.mPfd != null) {
                    try {
                        this.mPfd.close();
                    } catch (IOException e2) {
                        Log.e(TAG, "bindListen, close mPfd: " + e2);
                    }
                    this.mPfd = null;
                }
                Log.e(TAG, "bindListen, fail to get port number, exception: " + e);
                return -1;
            }
        } catch (RemoteException e3) {
            Log.e(TAG, Log.getStackTraceString(new Throwable()));
            return -1;
        }
    }

    BluetoothSocket accept(int i) throws IOException {
        BluetoothSocket bluetoothSocketAcceptSocket;
        if (this.mSocketState != SocketState.LISTENING) {
            throw new IOException("bt socket is not in listen state");
        }
        if (i > 0) {
            Log.d(TAG, "accept() set timeout (ms):" + i);
            this.mSocket.setSoTimeout(i);
        }
        String strWaitSocketSignal = waitSocketSignal(this.mSocketIS);
        if (i > 0) {
            this.mSocket.setSoTimeout(0);
        }
        synchronized (this) {
            if (this.mSocketState != SocketState.LISTENING) {
                throw new IOException("bt socket is not in listen state");
            }
            bluetoothSocketAcceptSocket = acceptSocket(strWaitSocketSignal);
        }
        return bluetoothSocketAcceptSocket;
    }

    int available() throws IOException {
        if (VDBG) {
            Log.d(TAG, "available: " + this.mSocketIS);
        }
        return this.mSocketIS.available();
    }

    void flush() throws IOException {
        if (this.mSocketOS == null) {
            throw new IOException("flush is called on null OutputStream");
        }
        if (VDBG) {
            Log.d(TAG, "flush: " + this.mSocketOS);
        }
        this.mSocketOS.flush();
    }

    int read(byte[] bArr, int i, int i2) throws IOException {
        int i3;
        if (VDBG) {
            Log.d(TAG, "read in:  " + this.mSocketIS + " len: " + i2);
        }
        if (this.mType == 3 || this.mType == 4) {
            if (VDBG) {
                Log.v(TAG, "l2cap: read(): offset: " + i + " length:" + i2 + "mL2capBuffer= " + this.mL2capBuffer);
            }
            if (this.mL2capBuffer == null) {
                createL2capRxBuffer();
            }
            if (this.mL2capBuffer.remaining() == 0) {
                if (VDBG) {
                    Log.v(TAG, "l2cap buffer empty, refilling...");
                }
                if (fillL2capRxBuffer() == -1) {
                    return -1;
                }
            }
            if (i2 > this.mL2capBuffer.remaining()) {
                i2 = this.mL2capBuffer.remaining();
            }
            if (VDBG) {
                Log.v(TAG, "get(): offset: " + i + " bytesToRead: " + i2);
            }
            this.mL2capBuffer.get(bArr, i, i2);
            i3 = i2;
        } else {
            if (VDBG) {
                Log.v(TAG, "default: read(): offset: " + i + " length:" + i2);
            }
            i3 = this.mSocketIS.read(bArr, i, i2);
        }
        if (i3 < 0) {
            throw new IOException("bt socket closed, read return: " + i3);
        }
        if (VDBG) {
            Log.d(TAG, "read out:  " + this.mSocketIS + " ret: " + i3);
        }
        return i3;
    }

    int write(byte[] bArr, int i, int i2) throws IOException {
        int i3;
        if (VDBG) {
            Log.d(TAG, "write: " + this.mSocketOS + " length: " + i2);
        }
        if ((this.mType != 3 && this.mType != 4) || i2 <= this.mMaxTxPacketSize) {
            this.mSocketOS.write(bArr, i, i2);
        } else {
            if (DBG) {
                Log.w(TAG, "WARNING: Write buffer larger than L2CAP packet size!\nPacket will be divided into SDU packets of size " + this.mMaxTxPacketSize);
            }
            int i4 = i;
            int i5 = i2;
            while (i5 > 0) {
                if (i5 > this.mMaxTxPacketSize) {
                    i3 = this.mMaxTxPacketSize;
                } else {
                    i3 = i5;
                }
                this.mSocketOS.write(bArr, i4, i3);
                i4 += i3;
                i5 -= i3;
            }
        }
        if (VDBG) {
            Log.d(TAG, "write out: " + this.mSocketOS + " length: " + i2);
        }
        return i2;
    }

    @Override
    public void close() throws IOException {
        Log.d(TAG, "close() this: " + this + ", channel: " + this.mPort + ", mSocketIS: " + this.mSocketIS + ", mSocketOS: " + this.mSocketOS + "mSocket: " + this.mSocket + ", mSocketState: " + this.mSocketState);
        if (this.mSocketState == SocketState.CLOSED) {
            return;
        }
        synchronized (this) {
            if (this.mSocketState == SocketState.CLOSED) {
                return;
            }
            this.mSocketState = SocketState.CLOSED;
            if (this.mSocket != null) {
                if (DBG) {
                    Log.d(TAG, "Closing mSocket: " + this.mSocket);
                }
                this.mSocket.shutdownInput();
                this.mSocket.shutdownOutput();
                this.mSocket.close();
                this.mSocket = null;
            }
            if (this.mPfd != null) {
                this.mPfd.close();
                this.mPfd = null;
            }
        }
    }

    void removeChannel() {
    }

    int getPort() {
        return this.mPort;
    }

    public int getMaxTransmitPacketSize() {
        return this.mMaxTxPacketSize;
    }

    public int getMaxReceivePacketSize() {
        return this.mMaxRxPacketSize;
    }

    public int getConnectionType() {
        return this.mType;
    }

    public void setExcludeSdp(boolean z) {
        this.mExcludeSdp = z;
    }

    public void requestMaximumTxDataLength() throws IOException {
        if (this.mDevice == null) {
            throw new IOException("requestMaximumTxDataLength is called on null device");
        }
        try {
            if (this.mSocketState == SocketState.CLOSED) {
                throw new IOException("socket closed");
            }
            IBluetooth bluetoothService = BluetoothAdapter.getDefaultAdapter().getBluetoothService(null);
            if (bluetoothService == null) {
                throw new IOException("Bluetooth is off");
            }
            if (DBG) {
                Log.d(TAG, "requestMaximumTxDataLength");
            }
            bluetoothService.getSocketManager().requestMaximumTxDataLength(this.mDevice);
        } catch (RemoteException e) {
            Log.e(TAG, Log.getStackTraceString(new Throwable()));
            throw new IOException("unable to send RPC: " + e.getMessage());
        }
    }

    private String convertAddr(byte[] bArr) {
        return String.format(Locale.US, "%02X:%02X:%02X:%02X:%02X:%02X", Byte.valueOf(bArr[0]), Byte.valueOf(bArr[1]), Byte.valueOf(bArr[2]), Byte.valueOf(bArr[3]), Byte.valueOf(bArr[4]), Byte.valueOf(bArr[5]));
    }

    private String waitSocketSignal(InputStream inputStream) throws IOException {
        byte[] bArr = new byte[20];
        int all = readAll(inputStream, bArr);
        if (VDBG) {
            Log.d(TAG, "waitSocketSignal read 20 bytes signal ret: " + all);
        }
        ByteBuffer byteBufferWrap = ByteBuffer.wrap(bArr);
        byteBufferWrap.order(ByteOrder.nativeOrder());
        short s = byteBufferWrap.getShort();
        if (s != 20) {
            throw new IOException("Connection failure, wrong signal size: " + ((int) s));
        }
        byte[] bArr2 = new byte[6];
        byteBufferWrap.get(bArr2);
        int i = byteBufferWrap.getInt();
        int i2 = byteBufferWrap.getInt();
        this.mMaxTxPacketSize = byteBufferWrap.getShort() & HostEndPoint.BROADCAST;
        this.mMaxRxPacketSize = byteBufferWrap.getShort() & HostEndPoint.BROADCAST;
        String strConvertAddr = convertAddr(bArr2);
        if (VDBG) {
            Log.d(TAG, "waitSocketSignal: sig size: " + ((int) s) + ", remote addr: " + strConvertAddr + ", channel: " + i + ", status: " + i2 + " MaxRxPktSize: " + this.mMaxRxPacketSize + " MaxTxPktSize: " + this.mMaxTxPacketSize);
        }
        if (i2 != 0) {
            throw new IOException("Connection failure, status: " + i2);
        }
        return strConvertAddr;
    }

    private void createL2capRxBuffer() {
        if (this.mType == 3 || this.mType == 4) {
            if (VDBG) {
                Log.v(TAG, "  Creating mL2capBuffer: mMaxPacketSize: " + this.mMaxRxPacketSize);
            }
            this.mL2capBuffer = ByteBuffer.wrap(new byte[this.mMaxRxPacketSize]);
            if (VDBG) {
                Log.v(TAG, "mL2capBuffer.remaining()" + this.mL2capBuffer.remaining());
            }
            this.mL2capBuffer.limit(0);
            if (VDBG) {
                Log.v(TAG, "mL2capBuffer.remaining() after limit(0):" + this.mL2capBuffer.remaining());
            }
        }
    }

    private int readAll(InputStream inputStream, byte[] bArr) throws IOException {
        int length = bArr.length;
        while (length > 0) {
            int i = inputStream.read(bArr, bArr.length - length, length);
            if (i <= 0) {
                throw new IOException("read failed, socket might closed or timeout, read ret: " + i);
            }
            length -= i;
            if (length != 0) {
                Log.w(TAG, "readAll() looping, read partial size: " + (bArr.length - length) + ", expect size: " + bArr.length);
            }
        }
        return bArr.length;
    }

    private int readInt(InputStream inputStream) throws IOException {
        byte[] bArr = new byte[4];
        int all = readAll(inputStream, bArr);
        if (VDBG) {
            Log.d(TAG, "inputStream.read ret: " + all);
        }
        ByteBuffer byteBufferWrap = ByteBuffer.wrap(bArr);
        byteBufferWrap.order(ByteOrder.nativeOrder());
        return byteBufferWrap.getInt();
    }

    private int fillL2capRxBuffer() throws IOException {
        this.mL2capBuffer.rewind();
        int i = this.mSocketIS.read(this.mL2capBuffer.array());
        if (i == -1) {
            this.mL2capBuffer.limit(0);
            return -1;
        }
        this.mL2capBuffer.limit(i);
        return i;
    }
}
