package android.ddm;

import android.os.Debug;
import android.os.Process;
import android.os.UserHandle;
import dalvik.system.VMRuntime;
import java.nio.ByteBuffer;
import org.apache.harmony.dalvik.ddmc.Chunk;
import org.apache.harmony.dalvik.ddmc.ChunkHandler;
import org.apache.harmony.dalvik.ddmc.DdmServer;

public class DdmHandleHello extends ChunkHandler {
    public static final int CHUNK_HELO = type("HELO");
    public static final int CHUNK_WAIT = type("WAIT");
    public static final int CHUNK_FEAT = type("FEAT");
    private static DdmHandleHello mInstance = new DdmHandleHello();
    private static final String[] FRAMEWORK_FEATURES = {"opengl-tracing", "view-hierarchy"};

    private DdmHandleHello() {
    }

    public static void register() {
        DdmServer.registerHandler(CHUNK_HELO, mInstance);
        DdmServer.registerHandler(CHUNK_FEAT, mInstance);
    }

    public void connected() {
    }

    public void disconnected() {
    }

    public Chunk handleChunk(Chunk chunk) {
        int i = chunk.type;
        if (i == CHUNK_HELO) {
            return handleHELO(chunk);
        }
        if (i == CHUNK_FEAT) {
            return handleFEAT(chunk);
        }
        throw new RuntimeException("Unknown packet " + ChunkHandler.name(i));
    }

    private Chunk handleHELO(Chunk chunk) {
        wrapChunk(chunk).getInt();
        String str = System.getProperty("java.vm.name", "?") + " v" + System.getProperty("java.vm.version", "?");
        String appName = DdmHandleAppName.getAppName();
        VMRuntime runtime = VMRuntime.getRuntime();
        String str2 = runtime.is64Bit() ? "64-bit" : "32-bit";
        String strVmInstructionSet = runtime.vmInstructionSet();
        if (strVmInstructionSet != null && strVmInstructionSet.length() > 0) {
            str2 = str2 + " (" + strVmInstructionSet + ")";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("CheckJNI=");
        sb.append(runtime.isCheckJniEnabled() ? "true" : "false");
        String string = sb.toString();
        boolean zIsNativeDebuggable = runtime.isNativeDebuggable();
        ByteBuffer byteBufferAllocate = ByteBuffer.allocate(28 + (str.length() * 2) + (appName.length() * 2) + (str2.length() * 2) + (string.length() * 2) + 1);
        byteBufferAllocate.order(ChunkHandler.CHUNK_ORDER);
        byteBufferAllocate.putInt(1);
        byteBufferAllocate.putInt(Process.myPid());
        byteBufferAllocate.putInt(str.length());
        byteBufferAllocate.putInt(appName.length());
        putString(byteBufferAllocate, str);
        putString(byteBufferAllocate, appName);
        byteBufferAllocate.putInt(UserHandle.myUserId());
        byteBufferAllocate.putInt(str2.length());
        putString(byteBufferAllocate, str2);
        byteBufferAllocate.putInt(string.length());
        putString(byteBufferAllocate, string);
        byteBufferAllocate.put(zIsNativeDebuggable ? (byte) 1 : (byte) 0);
        Chunk chunk2 = new Chunk(CHUNK_HELO, byteBufferAllocate);
        if (Debug.waitingForDebugger()) {
            sendWAIT(0);
        }
        return chunk2;
    }

    private Chunk handleFEAT(Chunk chunk) {
        String[] vmFeatureList = Debug.getVmFeatureList();
        int length = 4 + ((vmFeatureList.length + FRAMEWORK_FEATURES.length) * 4);
        for (int length2 = vmFeatureList.length - 1; length2 >= 0; length2--) {
            length += vmFeatureList[length2].length() * 2;
        }
        for (int length3 = FRAMEWORK_FEATURES.length - 1; length3 >= 0; length3--) {
            length += FRAMEWORK_FEATURES[length3].length() * 2;
        }
        ByteBuffer byteBufferAllocate = ByteBuffer.allocate(length);
        byteBufferAllocate.order(ChunkHandler.CHUNK_ORDER);
        byteBufferAllocate.putInt(vmFeatureList.length + FRAMEWORK_FEATURES.length);
        for (int length4 = vmFeatureList.length - 1; length4 >= 0; length4--) {
            byteBufferAllocate.putInt(vmFeatureList[length4].length());
            putString(byteBufferAllocate, vmFeatureList[length4]);
        }
        for (int length5 = FRAMEWORK_FEATURES.length - 1; length5 >= 0; length5--) {
            byteBufferAllocate.putInt(FRAMEWORK_FEATURES[length5].length());
            putString(byteBufferAllocate, FRAMEWORK_FEATURES[length5]);
        }
        return new Chunk(CHUNK_FEAT, byteBufferAllocate);
    }

    public static void sendWAIT(int i) {
        DdmServer.sendChunk(new Chunk(CHUNK_WAIT, new byte[]{(byte) i}, 0, 1));
    }
}
