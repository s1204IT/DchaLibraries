package com.android.gallery3d.filtershow.filters;

import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.FieldPacker;
import android.renderscript.RSRuntimeException;
import android.renderscript.RenderScript;
import android.renderscript.Script;
import android.renderscript.ScriptC;
import android.renderscript.Type;

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

    public ScriptC_vignette(RenderScript renderScript) {
        super(renderScript, "vignette", vignetteBitCode.getBitCode32(), vignetteBitCode.getBitCode64());
        this.__U32 = Element.U32(renderScript);
        this.__F32 = Element.F32(renderScript);
        this.__U8_4 = Element.U8_4(renderScript);
    }

    public synchronized void set_inputWidth(long j) {
        if (this.__rs_fp_U32 != null) {
            this.__rs_fp_U32.reset();
        } else {
            this.__rs_fp_U32 = new FieldPacker(4);
        }
        this.__rs_fp_U32.addU32(j);
        setVar(0, this.__rs_fp_U32);
        this.mExportVar_inputWidth = j;
    }

    public synchronized void set_inputHeight(long j) {
        if (this.__rs_fp_U32 != null) {
            this.__rs_fp_U32.reset();
        } else {
            this.__rs_fp_U32 = new FieldPacker(4);
        }
        this.__rs_fp_U32.addU32(j);
        setVar(1, this.__rs_fp_U32);
        this.mExportVar_inputHeight = j;
    }

    public synchronized void set_centerx(float f) {
        setVar(2, f);
        this.mExportVar_centerx = f;
    }

    public synchronized void set_centery(float f) {
        setVar(3, f);
        this.mExportVar_centery = f;
    }

    public synchronized void set_radiusx(float f) {
        setVar(4, f);
        this.mExportVar_radiusx = f;
    }

    public synchronized void set_radiusy(float f) {
        setVar(5, f);
        this.mExportVar_radiusy = f;
    }

    public synchronized void set_strength(float f) {
        setVar(6, f);
        this.mExportVar_strength = f;
    }

    public synchronized void set_finalBright(float f) {
        setVar(7, f);
        this.mExportVar_finalBright = f;
    }

    public synchronized void set_finalSaturation(float f) {
        setVar(8, f);
        this.mExportVar_finalSaturation = f;
    }

    public synchronized void set_finalContrast(float f) {
        setVar(9, f);
        this.mExportVar_finalContrast = f;
    }

    public synchronized void set_finalSubtract(float f) {
        setVar(10, f);
        this.mExportVar_finalSubtract = f;
    }

    public void forEach_vignette(Allocation allocation, Allocation allocation2) {
        forEach_vignette(allocation, allocation2, null);
    }

    public void forEach_vignette(Allocation allocation, Allocation allocation2, Script.LaunchOptions launchOptions) {
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

    public void invoke_setupVignetteParams() {
        invoke(0);
    }
}
