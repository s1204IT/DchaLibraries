package com.android.gallery3d.filtershow.filters;

import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.FieldPacker;
import android.renderscript.RSRuntimeException;
import android.renderscript.RenderScript;
import android.renderscript.Script;
import android.renderscript.ScriptC;
import android.renderscript.Type;

public class ScriptC_convolve3x3 extends ScriptC {
    private Element __ALLOCATION;
    private Element __F32;
    private Element __I32;
    private Element __U8_4;
    private float[] mExportVar_gCoeffs;
    private int mExportVar_gHeight;
    private Allocation mExportVar_gIn;
    private Allocation mExportVar_gPixels;
    private int mExportVar_gWidth;

    public ScriptC_convolve3x3(RenderScript renderScript) {
        super(renderScript, "convolve3x3", convolve3x3BitCode.getBitCode32(), convolve3x3BitCode.getBitCode64());
        this.__I32 = Element.I32(renderScript);
        this.__ALLOCATION = Element.ALLOCATION(renderScript);
        this.__F32 = Element.F32(renderScript);
        this.__U8_4 = Element.U8_4(renderScript);
    }

    public synchronized void set_gWidth(int i) {
        setVar(0, i);
        this.mExportVar_gWidth = i;
    }

    public synchronized void set_gHeight(int i) {
        setVar(1, i);
        this.mExportVar_gHeight = i;
    }

    public void bind_gPixels(Allocation allocation) {
        this.mExportVar_gPixels = allocation;
        if (allocation == null) {
            bindAllocation(null, 2);
        } else {
            bindAllocation(allocation, 2);
        }
    }

    public synchronized void set_gIn(Allocation allocation) {
        setVar(3, allocation);
        this.mExportVar_gIn = allocation;
    }

    public synchronized void set_gCoeffs(float[] fArr) {
        this.mExportVar_gCoeffs = fArr;
        FieldPacker fieldPacker = new FieldPacker(36);
        for (int i = 0; i < 9; i++) {
            fieldPacker.addF32(fArr[i]);
        }
        setVar(4, fieldPacker, this.__F32, new int[]{9});
    }

    public void forEach_root(Allocation allocation, Allocation allocation2) {
        forEach_root(allocation, allocation2, null);
    }

    public void forEach_root(Allocation allocation, Allocation allocation2, Script.LaunchOptions launchOptions) {
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
        forEach(0, allocation, allocation2, (FieldPacker) null, launchOptions);
    }
}
