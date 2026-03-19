package com.android.gallery3d.filtershow.filters;

import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.FieldPacker;
import android.renderscript.RSRuntimeException;
import android.renderscript.RenderScript;
import android.renderscript.Script;
import android.renderscript.ScriptC;
import android.renderscript.Type;

public class ScriptC_saturation extends ScriptC {
    private Element __F32;
    private Element __I32;
    private Element __U8_4;
    private int[] mExportVar_saturation;

    public ScriptC_saturation(RenderScript renderScript) {
        super(renderScript, "saturation", saturationBitCode.getBitCode32(), saturationBitCode.getBitCode64());
        this.__I32 = Element.I32(renderScript);
        this.__F32 = Element.F32(renderScript);
        this.__U8_4 = Element.U8_4(renderScript);
    }

    public synchronized void set_saturation(int[] iArr) {
        this.mExportVar_saturation = iArr;
        FieldPacker fieldPacker = new FieldPacker(28);
        for (int i = 0; i < 7; i++) {
            fieldPacker.addI32(iArr[i]);
        }
        setVar(3, fieldPacker, this.__I32, new int[]{7});
    }

    public void forEach_selectiveAdjust(Allocation allocation, Allocation allocation2, Script.LaunchOptions launchOptions) {
        if (!allocation.getType().getElement().isCompatible(this.__U8_4)) {
            throw new RSRuntimeException("Type mismatch with U8_4!");
        }
        if (!allocation2.getType().getElement().isCompatible(this.__U8_4)) {
            throw new RSRuntimeException("Type mismatch with U8_4!");
        }
        Type type = allocation.getType();
        Type type2 = allocation2.getType();
        if (type.getCount() != type2.getCount() || type.getX() != type2.getX() || type.getY() != type2.getY() || type.getZ() != type2.getZ() || type.hasFaces() != type2.hasFaces() || type.hasMipmaps() != type2.hasMipmaps()) {
            throw new RSRuntimeException("Dimension mismatch between parameters ain and aout!");
        }
        forEach(1, allocation, allocation2, (FieldPacker) null, launchOptions);
    }

    public void invoke_setupGradParams() {
        invoke(0);
    }
}
