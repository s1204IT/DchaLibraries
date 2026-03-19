package javax.obex;

import android.util.Log;
import java.io.IOException;
import java.io.InputStream;

public class ObexPacket {
    private static final String TAG = "ObexPacket";
    private static final boolean V = ObexHelper.VDBG;
    public int mHeaderId;
    public int mLength;
    public byte[] mPayload = null;

    private ObexPacket(int headerId, int length) {
        this.mHeaderId = headerId;
        this.mLength = length;
    }

    public static ObexPacket read(InputStream is) throws IOException {
        int headerId = is.read();
        return read(headerId, is);
    }

    public static ObexPacket read(int headerId, InputStream is) throws IOException {
        int length = (is.read() << 8) + is.read();
        if (V) {
            Log.d(TAG, "read packet length = " + length);
        }
        ObexPacket newPacket = new ObexPacket(headerId, length);
        byte[] temp = null;
        if (length > 3) {
            temp = new byte[length - 3];
            int bytesReceived = is.read(temp);
            while (bytesReceived != temp.length) {
                bytesReceived += is.read(temp, bytesReceived, temp.length - bytesReceived);
            }
        }
        newPacket.mPayload = temp;
        return newPacket;
    }
}
