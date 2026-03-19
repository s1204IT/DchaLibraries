package android.renderscript;

import android.content.res.Resources;
import java.io.IOException;
import java.io.InputStream;

public class ScriptC extends Script {
    private static final String TAG = "ScriptC";

    protected ScriptC(int i, RenderScript renderScript) {
        super(i, renderScript);
    }

    protected ScriptC(long j, RenderScript renderScript) {
        super(j, renderScript);
    }

    protected ScriptC(RenderScript renderScript, Resources resources, int i) {
        super(0L, renderScript);
        long jInternalCreate = internalCreate(renderScript, resources, i);
        if (jInternalCreate == 0) {
            throw new RSRuntimeException("Loading of ScriptC script failed.");
        }
        setID(jInternalCreate);
    }

    protected ScriptC(RenderScript renderScript, String str, byte[] bArr, byte[] bArr2) {
        long jInternalStringCreate;
        super(0L, renderScript);
        if (RenderScript.sPointerSize == 4) {
            jInternalStringCreate = internalStringCreate(renderScript, str, bArr);
        } else {
            jInternalStringCreate = internalStringCreate(renderScript, str, bArr2);
        }
        if (jInternalStringCreate == 0) {
            throw new RSRuntimeException("Loading of ScriptC script failed.");
        }
        setID(jInternalStringCreate);
    }

    private static synchronized long internalCreate(RenderScript renderScript, Resources resources, int i) {
        byte[] bArr;
        int i2;
        InputStream inputStreamOpenRawResource = resources.openRawResource(i);
        try {
            try {
                bArr = new byte[1024];
                i2 = 0;
                while (true) {
                    int length = bArr.length - i2;
                    if (length == 0) {
                        byte[] bArr2 = new byte[bArr.length * 2];
                        System.arraycopy(bArr, 0, bArr2, 0, bArr.length);
                        length = bArr2.length - i2;
                        bArr = bArr2;
                    }
                    int i3 = inputStreamOpenRawResource.read(bArr, i2, length);
                    if (i3 > 0) {
                        i2 += i3;
                    }
                }
            } catch (IOException e) {
                throw new Resources.NotFoundException();
            }
        } finally {
            inputStreamOpenRawResource.close();
        }
        return renderScript.nScriptCCreate(resources.getResourceEntryName(i), RenderScript.getCachePath(), bArr, i2);
    }

    private static synchronized long internalStringCreate(RenderScript renderScript, String str, byte[] bArr) {
        return renderScript.nScriptCCreate(str, RenderScript.getCachePath(), bArr, bArr.length);
    }
}
