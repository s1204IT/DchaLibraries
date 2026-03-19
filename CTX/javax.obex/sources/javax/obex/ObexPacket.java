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

    private ObexPacket(int i, int i2) {
        this.mHeaderId = i;
        this.mLength = i2;
    }

    public static ObexPacket read(InputStream inputStream) throws IOException {
        int i = inputStream.read();
        if (V) {
            Log.d(TAG, "headerId = " + i);
        }
        return read(i, inputStream);
    }

    public static ObexPacket read(int i, InputStream inputStream) throws IOException {
        int i2 = (inputStream.read() << 8) + inputStream.read();
        if (V) {
            Log.d(TAG, "read packet length = " + i2);
        }
        ObexPacket obexPacket = new ObexPacket(i, i2);
        byte[] bArr = null;
        if (i2 > 3) {
            bArr = new byte[i2 - 3];
            int i3 = inputStream.read(bArr);
            while (i3 != bArr.length) {
                i3 += inputStream.read(bArr, i3, bArr.length - i3);
            }
        }
        obexPacket.mPayload = bArr;
        return obexPacket;
    }
}
