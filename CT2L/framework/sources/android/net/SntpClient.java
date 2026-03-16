package android.net;

import android.os.SystemClock;
import com.android.internal.telephony.GsmAlphabet;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class SntpClient {
    private static final int NTP_MODE_CLIENT = 3;
    private static final int NTP_PACKET_SIZE = 48;
    private static final int NTP_PORT = 123;
    private static final int NTP_VERSION = 3;
    private static final long OFFSET_1900_TO_1970 = 2208988800L;
    private static final int ORIGINATE_TIME_OFFSET = 24;
    private static final int RECEIVE_TIME_OFFSET = 32;
    private static final int REFERENCE_TIME_OFFSET = 16;
    private static final String TAG = "SntpClient";
    private static final int TRANSMIT_TIME_OFFSET = 40;
    private long mNtpTime;
    private long mNtpTimeReference;
    private long mRoundTripTime;

    public boolean requestTime(String host, int timeout) throws Throwable {
        DatagramSocket socket = null;
        try {
            DatagramSocket socket2 = new DatagramSocket();
            try {
                socket2.setSoTimeout(timeout);
                InetAddress address = InetAddress.getByName(host);
                byte[] buffer = new byte[48];
                DatagramPacket request = new DatagramPacket(buffer, buffer.length, address, 123);
                buffer[0] = GsmAlphabet.GSM_EXTENDED_ESCAPE;
                long requestTime = System.currentTimeMillis();
                long requestTicks = SystemClock.elapsedRealtime();
                writeTimeStamp(buffer, 40, requestTime);
                socket2.send(request);
                DatagramPacket response = new DatagramPacket(buffer, buffer.length);
                socket2.receive(response);
                long responseTicks = SystemClock.elapsedRealtime();
                long responseTime = requestTime + (responseTicks - requestTicks);
                long originateTime = readTimeStamp(buffer, 24);
                long receiveTime = readTimeStamp(buffer, 32);
                long transmitTime = readTimeStamp(buffer, 40);
                long roundTripTime = (responseTicks - requestTicks) - (transmitTime - receiveTime);
                long clockOffset = ((receiveTime - originateTime) + (transmitTime - responseTime)) / 2;
                this.mNtpTime = responseTime + clockOffset;
                this.mNtpTimeReference = responseTicks;
                this.mRoundTripTime = roundTripTime;
                if (socket2 != null) {
                    socket2.close();
                }
                return true;
            } catch (Exception e) {
                socket = socket2;
                if (socket == null) {
                    return false;
                }
                socket.close();
                return false;
            } catch (Throwable th) {
                th = th;
                socket = socket2;
                if (socket != null) {
                    socket.close();
                }
                throw th;
            }
        } catch (Exception e2) {
        } catch (Throwable th2) {
            th = th2;
        }
    }

    public long getNtpTime() {
        return this.mNtpTime;
    }

    public long getNtpTimeReference() {
        return this.mNtpTimeReference;
    }

    public long getRoundTripTime() {
        return this.mRoundTripTime;
    }

    private long read32(byte[] buffer, int offset) {
        byte b0 = buffer[offset];
        byte b1 = buffer[offset + 1];
        byte b2 = buffer[offset + 2];
        byte b3 = buffer[offset + 3];
        int i0 = (b0 & 128) == 128 ? (b0 & 127) + 128 : b0;
        int i1 = (b1 & 128) == 128 ? (b1 & 127) + 128 : b1;
        int i2 = (b2 & 128) == 128 ? (b2 & 127) + 128 : b2;
        int i3 = (b3 & 128) == 128 ? (b3 & 127) + 128 : b3;
        return (((long) i0) << 24) + (((long) i1) << 16) + (((long) i2) << 8) + ((long) i3);
    }

    private long readTimeStamp(byte[] buffer, int offset) {
        long seconds = read32(buffer, offset);
        long fraction = read32(buffer, offset + 4);
        return ((seconds - OFFSET_1900_TO_1970) * 1000) + ((1000 * fraction) / 4294967296L);
    }

    private void writeTimeStamp(byte[] buffer, int offset, long time) {
        long seconds = time / 1000;
        long milliseconds = time - (1000 * seconds);
        long seconds2 = seconds + OFFSET_1900_TO_1970;
        int offset2 = offset + 1;
        buffer[offset] = (byte) (seconds2 >> 24);
        int offset3 = offset2 + 1;
        buffer[offset2] = (byte) (seconds2 >> 16);
        int offset4 = offset3 + 1;
        buffer[offset3] = (byte) (seconds2 >> 8);
        int offset5 = offset4 + 1;
        buffer[offset4] = (byte) (seconds2 >> 0);
        long fraction = (4294967296L * milliseconds) / 1000;
        int offset6 = offset5 + 1;
        buffer[offset5] = (byte) (fraction >> 24);
        int offset7 = offset6 + 1;
        buffer[offset6] = (byte) (fraction >> 16);
        int offset8 = offset7 + 1;
        buffer[offset7] = (byte) (fraction >> 8);
        int i = offset8 + 1;
        buffer[offset8] = (byte) (Math.random() * 255.0d);
    }
}
