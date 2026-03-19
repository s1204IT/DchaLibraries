package android.ddm;

import android.os.Debug;
import android.util.Log;
import java.io.IOException;
import java.nio.ByteBuffer;
import org.apache.harmony.dalvik.ddmc.Chunk;
import org.apache.harmony.dalvik.ddmc.ChunkHandler;
import org.apache.harmony.dalvik.ddmc.DdmServer;
import org.apache.harmony.dalvik.ddmc.DdmVmInternal;

public class DdmHandleHeap extends ChunkHandler {
    public static final int CHUNK_HPIF = type("HPIF");
    public static final int CHUNK_HPSG = type("HPSG");
    public static final int CHUNK_HPDU = type("HPDU");
    public static final int CHUNK_HPDS = type("HPDS");
    public static final int CHUNK_NHSG = type("NHSG");
    public static final int CHUNK_HPGC = type("HPGC");
    public static final int CHUNK_REAE = type("REAE");
    public static final int CHUNK_REAQ = type("REAQ");
    public static final int CHUNK_REAL = type("REAL");
    private static DdmHandleHeap mInstance = new DdmHandleHeap();

    private DdmHandleHeap() {
    }

    public static void register() {
        DdmServer.registerHandler(CHUNK_HPIF, mInstance);
        DdmServer.registerHandler(CHUNK_HPSG, mInstance);
        DdmServer.registerHandler(CHUNK_HPDU, mInstance);
        DdmServer.registerHandler(CHUNK_HPDS, mInstance);
        DdmServer.registerHandler(CHUNK_NHSG, mInstance);
        DdmServer.registerHandler(CHUNK_HPGC, mInstance);
        DdmServer.registerHandler(CHUNK_REAE, mInstance);
        DdmServer.registerHandler(CHUNK_REAQ, mInstance);
        DdmServer.registerHandler(CHUNK_REAL, mInstance);
    }

    public void connected() {
    }

    public void disconnected() {
    }

    public Chunk handleChunk(Chunk chunk) {
        int i = chunk.type;
        if (i == CHUNK_HPIF) {
            return handleHPIF(chunk);
        }
        if (i == CHUNK_HPSG) {
            return handleHPSGNHSG(chunk, false);
        }
        if (i == CHUNK_HPDU) {
            return handleHPDU(chunk);
        }
        if (i == CHUNK_HPDS) {
            return handleHPDS(chunk);
        }
        if (i == CHUNK_NHSG) {
            return handleHPSGNHSG(chunk, true);
        }
        if (i == CHUNK_HPGC) {
            return handleHPGC(chunk);
        }
        if (i == CHUNK_REAE) {
            return handleREAE(chunk);
        }
        if (i == CHUNK_REAQ) {
            return handleREAQ(chunk);
        }
        if (i == CHUNK_REAL) {
            return handleREAL(chunk);
        }
        throw new RuntimeException("Unknown packet " + ChunkHandler.name(i));
    }

    private Chunk handleHPIF(Chunk chunk) {
        if (!DdmVmInternal.heapInfoNotify(wrapChunk(chunk).get())) {
            return createFailChunk(1, "Unsupported HPIF what");
        }
        return null;
    }

    private Chunk handleHPSGNHSG(Chunk chunk, boolean z) {
        ByteBuffer byteBufferWrapChunk = wrapChunk(chunk);
        if (!DdmVmInternal.heapSegmentNotify(byteBufferWrapChunk.get(), byteBufferWrapChunk.get(), z)) {
            return createFailChunk(1, "Unsupported HPSG what/when");
        }
        return null;
    }

    private Chunk handleHPDU(Chunk chunk) {
        ByteBuffer byteBufferWrapChunk = wrapChunk(chunk);
        byte b = -1;
        try {
            Debug.dumpHprofData(getString(byteBufferWrapChunk, byteBufferWrapChunk.getInt()));
            b = 0;
        } catch (IOException e) {
        } catch (UnsupportedOperationException e2) {
            Log.w("ddm-heap", "hprof dumps not supported in this VM");
        } catch (RuntimeException e3) {
        }
        byte[] bArr = {b};
        return new Chunk(CHUNK_HPDU, bArr, 0, bArr.length);
    }

    private Chunk handleHPDS(Chunk chunk) {
        String str;
        wrapChunk(chunk);
        try {
            Debug.dumpHprofDataDdms();
            str = null;
        } catch (UnsupportedOperationException e) {
            str = "hprof dumps not supported in this VM";
        } catch (RuntimeException e2) {
            str = "Exception: " + e2.getMessage();
        }
        if (str == null) {
            return null;
        }
        Log.w("ddm-heap", str);
        return createFailChunk(1, str);
    }

    private Chunk handleHPGC(Chunk chunk) {
        Runtime.getRuntime().gc();
        return null;
    }

    private Chunk handleREAE(Chunk chunk) {
        DdmVmInternal.enableRecentAllocations(wrapChunk(chunk).get() != 0);
        return null;
    }

    private Chunk handleREAQ(Chunk chunk) {
        byte[] bArr = {DdmVmInternal.getRecentAllocationStatus()};
        return new Chunk(CHUNK_REAQ, bArr, 0, bArr.length);
    }

    private Chunk handleREAL(Chunk chunk) {
        byte[] recentAllocations = DdmVmInternal.getRecentAllocations();
        return new Chunk(CHUNK_REAL, recentAllocations, 0, recentAllocations.length);
    }
}
