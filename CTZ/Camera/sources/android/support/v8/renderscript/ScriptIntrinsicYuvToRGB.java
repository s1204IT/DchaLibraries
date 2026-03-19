package android.support.v8.renderscript;

import android.os.Build;

public class ScriptIntrinsicYuvToRGB extends ScriptIntrinsic {
    private Allocation mInput;

    ScriptIntrinsicYuvToRGB(long j, RenderScript renderScript) {
        super(j, renderScript);
    }

    public static ScriptIntrinsicYuvToRGB create(RenderScript renderScript, Element element) {
        boolean z = renderScript.isUseNative() && Build.VERSION.SDK_INT < 19;
        ScriptIntrinsicYuvToRGB scriptIntrinsicYuvToRGB = new ScriptIntrinsicYuvToRGB(renderScript.nScriptIntrinsicCreate(6, element.getID(renderScript), z), renderScript);
        scriptIntrinsicYuvToRGB.setIncSupp(z);
        return scriptIntrinsicYuvToRGB;
    }

    public void setInput(Allocation allocation) {
        this.mInput = allocation;
        setVar(0, allocation);
    }

    public void forEach(Allocation allocation) {
        forEach(0, null, allocation, null);
    }
}
