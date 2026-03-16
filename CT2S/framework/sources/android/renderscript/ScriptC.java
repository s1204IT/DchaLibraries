package android.renderscript;

import android.content.res.Resources;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class ScriptC extends Script {
    private static final String CACHE_PATH = "com.android.renderscript.cache";
    private static final String TAG = "ScriptC";
    static String mCachePath;

    protected ScriptC(int id, RenderScript rs) {
        super(id, rs);
    }

    protected ScriptC(long id, RenderScript rs) {
        super(id, rs);
    }

    protected ScriptC(RenderScript rs, Resources resources, int resourceID) {
        super(0L, rs);
        long id = internalCreate(rs, resources, resourceID);
        if (id == 0) {
            throw new RSRuntimeException("Loading of ScriptC script failed.");
        }
        setID(id);
    }

    protected ScriptC(RenderScript rs, String resName, byte[] bitcode32, byte[] bitcode64) {
        long id;
        super(0L, rs);
        if (RenderScript.sPointerSize == 4) {
            id = internalStringCreate(rs, resName, bitcode32);
        } else {
            id = internalStringCreate(rs, resName, bitcode64);
        }
        if (id == 0) {
            throw new RSRuntimeException("Loading of ScriptC script failed.");
        }
        setID(id);
    }

    private static synchronized long internalCreate(RenderScript rs, Resources resources, int resourceID) {
        byte[] pgm;
        int pgmLength;
        String resName;
        InputStream is = resources.openRawResource(resourceID);
        try {
            try {
                pgm = new byte[1024];
                pgmLength = 0;
                while (true) {
                    int bytesLeft = pgm.length - pgmLength;
                    if (bytesLeft == 0) {
                        byte[] buf2 = new byte[pgm.length * 2];
                        System.arraycopy(pgm, 0, buf2, 0, pgm.length);
                        pgm = buf2;
                        bytesLeft = pgm.length - pgmLength;
                    }
                    int bytesRead = is.read(pgm, pgmLength, bytesLeft);
                    if (bytesRead <= 0) {
                        break;
                    }
                    pgmLength += bytesRead;
                }
                resName = resources.getResourceEntryName(resourceID);
                if (mCachePath == null) {
                    File f = new File(RenderScript.mCacheDir, CACHE_PATH);
                    mCachePath = f.getAbsolutePath();
                    f.mkdirs();
                }
            } catch (IOException e) {
                throw new Resources.NotFoundException();
            }
        } finally {
            is.close();
        }
        return rs.nScriptCCreate(resName, mCachePath, pgm, pgmLength);
    }

    private static synchronized long internalStringCreate(RenderScript rs, String resName, byte[] bitcode) {
        if (mCachePath == null) {
            File f = new File(RenderScript.mCacheDir, CACHE_PATH);
            mCachePath = f.getAbsolutePath();
            f.mkdirs();
        }
        return rs.nScriptCCreate(resName, mCachePath, bitcode, bitcode.length);
    }
}
