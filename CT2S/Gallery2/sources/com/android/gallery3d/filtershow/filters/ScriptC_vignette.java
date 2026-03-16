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

public class ScriptC_vignette extends ScriptC {
    private Element __F32;
    private Element __U32;
    private Element __U8_4;
    private FieldPacker __rs_fp_U32;
    private float mExportVar_centerx;
    private float mExportVar_centery;
    private float mExportVar_finalBright;
    private float mExportVar_finalContrast;
    private float mExportVar_finalSaturation;
    private float mExportVar_finalSubtract;
    private long mExportVar_inputHeight;
    private long mExportVar_inputWidth;
    private float mExportVar_radiusx;
    private float mExportVar_radiusy;
    private float mExportVar_strength;

    public ScriptC_vignette(RenderScript rs, Resources resources, int id) {
        super(rs, resources, id);
        this.__U32 = Element.U32(rs);
        this.__F32 = Element.F32(rs);
        this.__U8_4 = Element.U8_4(rs);
    }

    public synchronized void set_inputWidth(long v) {
        if (this.__rs_fp_U32 != null) {
            this.__rs_fp_U32.reset();
        } else {
            this.__rs_fp_U32 = new FieldPacker(4);
        }
        this.__rs_fp_U32.addU32(v);
        setVar(0, this.__rs_fp_U32);
        this.mExportVar_inputWidth = v;
    }

    public synchronized void set_inputHeight(long v) {
        if (this.__rs_fp_U32 != null) {
            this.__rs_fp_U32.reset();
        } else {
            this.__rs_fp_U32 = new FieldPacker(4);
        }
        this.__rs_fp_U32.addU32(v);
        setVar(1, this.__rs_fp_U32);
        this.mExportVar_inputHeight = v;
    }

    public synchronized void set_centerx(float v) {
        setVar(2, v);
        this.mExportVar_centerx = v;
    }

    public synchronized void set_centery(float v) {
        setVar(3, v);
        this.mExportVar_centery = v;
    }

    public synchronized void set_radiusx(float v) {
        setVar(4, v);
        this.mExportVar_radiusx = v;
    }

    public synchronized void set_radiusy(float v) {
        setVar(5, v);
        this.mExportVar_radiusy = v;
    }

    public synchronized void set_strength(float v) {
        setVar(6, v);
        this.mExportVar_strength = v;
    }

    public synchronized void set_finalBright(float v) {
        setVar(7, v);
        this.mExportVar_finalBright = v;
    }

    public synchronized void set_finalSaturation(float v) {
        setVar(8, v);
        this.mExportVar_finalSaturation = v;
    }

    public synchronized void set_finalContrast(float v) {
        setVar(9, v);
        this.mExportVar_finalContrast = v;
    }

    public synchronized void set_finalSubtract(float v) {
        setVar(10, v);
        this.mExportVar_finalSubtract = v;
    }

    public void forEach_vignette(Allocation ain, Allocation aout) {
        forEach_vignette(ain, aout, null);
    }

    public void forEach_vignette(Allocation ain, Allocation aout, Script.LaunchOptions sc) {
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
        forEach(1, ain, aout, null, sc);
    }

    public void invoke_setupVignetteParams() {
        invoke(0);
    }
}
