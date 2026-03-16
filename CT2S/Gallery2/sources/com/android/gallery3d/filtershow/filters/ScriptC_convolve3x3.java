package com.android.gallery3d.filtershow.filters;

import android.content.res.Resources;
import android.support.v8.renderscript.Allocation;
import android.support.v8.renderscript.Element;
import android.support.v8.renderscript.FieldPacker;
import android.support.v8.renderscript.RSRuntimeException;
import android.support.v8.renderscript.RenderScript;
import android.support.v8.renderscript.Script;
import android.support.v8.renderscript.ScriptC;
import android.support.v8.renderscript.Type;

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

    public ScriptC_convolve3x3(RenderScript rs, Resources resources, int id) {
        super(rs, resources, id);
        this.__I32 = Element.I32(rs);
        this.__ALLOCATION = Element.ALLOCATION(rs);
        this.__F32 = Element.F32(rs);
        this.__U8_4 = Element.U8_4(rs);
    }

    public synchronized void set_gWidth(int v) {
        setVar(0, v);
        this.mExportVar_gWidth = v;
    }

    public synchronized void set_gHeight(int v) {
        setVar(1, v);
        this.mExportVar_gHeight = v;
    }

    public void bind_gPixels(Allocation v) {
        this.mExportVar_gPixels = v;
        if (v != null) {
            bindAllocation(v, 2);
        } else {
            bindAllocation(null, 2);
        }
    }

    public synchronized void set_gIn(Allocation v) {
        setVar(3, v);
        this.mExportVar_gIn = v;
    }

    public synchronized void set_gCoeffs(float[] v) {
        this.mExportVar_gCoeffs = v;
        FieldPacker fp = new FieldPacker(36);
        for (int ct1 = 0; ct1 < 9; ct1++) {
            fp.addF32(v[ct1]);
        }
        int[] __dimArr = {9};
        setVar(4, fp, this.__F32, __dimArr);
    }

    public void forEach_root(Allocation ain, Allocation aout) {
        forEach_root(ain, aout, null);
    }

    public void forEach_root(Allocation ain, Allocation aout, Script.LaunchOptions sc) {
        if (!ain.getType().getElement().isCompatible(this.__U8_4)) {
            throw new RSRuntimeException("Type mismatch with U8_4!");
        }
        if (!aout.getType().getElement().isCompatible(this.__U8_4)) {
            throw new RSRuntimeException("Type mismatch with U8_4!");
        }
        Type t0 = ain.getType();
        Type t1 = aout.getType();
        if (t0.getCount() != t1.getCount() || t0.getX() != t1.getX() || t0.getY() != t1.getY() || t0.getZ() != t1.getZ() || t0.hasFaces() != t1.hasFaces() || t0.hasMipmaps() != t1.hasMipmaps()) {
            throw new RSRuntimeException("Dimension mismatch between parameters ain and aout!");
        }
        forEach(0, ain, aout, null, sc);
    }
}
