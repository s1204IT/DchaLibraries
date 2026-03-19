package android.ddm;

import java.nio.ByteBuffer;
import org.apache.harmony.dalvik.ddmc.Chunk;
import org.apache.harmony.dalvik.ddmc.ChunkHandler;
import org.apache.harmony.dalvik.ddmc.DdmServer;

public class DdmHandleAppName extends ChunkHandler {
    public static final int CHUNK_APNM = type("APNM");
    private static volatile String mAppName = "";
    private static DdmHandleAppName mInstance = new DdmHandleAppName();

    private DdmHandleAppName() {
    }

    public static void register() {
    }

    public void connected() {
    }

    public void disconnected() {
    }

    public Chunk handleChunk(Chunk chunk) {
        return null;
    }

    public static void setAppName(String str, int i) {
        if (str == null || str.length() == 0) {
            return;
        }
        mAppName = str;
        sendAPNM(str, i);
    }

    public static String getAppName() {
        return mAppName;
    }

    private static void sendAPNM(String str, int i) {
        ByteBuffer byteBufferAllocate = ByteBuffer.allocate((str.length() * 2) + 4 + 4);
        byteBufferAllocate.order(ChunkHandler.CHUNK_ORDER);
        byteBufferAllocate.putInt(str.length());
        putString(byteBufferAllocate, str);
        byteBufferAllocate.putInt(i);
        DdmServer.sendChunk(new Chunk(CHUNK_APNM, byteBufferAllocate));
    }
}
