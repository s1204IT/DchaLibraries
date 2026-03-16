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

public class ScriptC_grad extends ScriptC {
    private Element __BOOLEAN;
    private Element __I32;
    private Element __U32;
    private Element __U8_4;
    private FieldPacker __rs_fp_U32;
    private int[] mExportVar_brightness;
    private int[] mExportVar_contrast;
    private long mExportVar_inputHeight;
    private long mExportVar_inputWidth;
    private boolean[] mExportVar_mask;
    private int[] mExportVar_saturation;
    private int[] mExportVar_xPos1;
    private int[] mExportVar_xPos2;
    private int[] mExportVar_yPos1;
    private int[] mExportVar_yPos2;

    public ScriptC_grad(RenderScript rs, Resources resources, int id) {
        super(rs, resources, id);
        this.__U32 = Element.U32(rs);
        this.__I32 = Element.I32(rs);
        this.__BOOLEAN = Element.BOOLEAN(rs);
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

    public synchronized void set_mask(boolean[] v) {
        this.mExportVar_mask = v;
        FieldPacker fp = new FieldPacker(16);
        for (int ct1 = 0; ct1 < 16; ct1++) {
            fp.addBoolean(v[ct1]);
        }
        int[] __dimArr = {16};
        setVar(3, fp, this.__BOOLEAN, __dimArr);
    }

    public synchronized void set_xPos1(int[] v) {
        this.mExportVar_xPos1 = v;
        FieldPacker fp = new FieldPacker(64);
        for (int ct1 = 0; ct1 < 16; ct1++) {
            fp.addI32(v[ct1]);
        }
        int[] __dimArr = {16};
        setVar(4, fp, this.__I32, __dimArr);
    }

    public synchronized void set_yPos1(int[] v) {
        this.mExportVar_yPos1 = v;
        FieldPacker fp = new FieldPacker(64);
        for (int ct1 = 0; ct1 < 16; ct1++) {
            fp.addI32(v[ct1]);
        }
        int[] __dimArr = {16};
        setVar(5, fp, this.__I32, __dimArr);
    }

    public synchronized void set_xPos2(int[] v) {
        this.mExportVar_xPos2 = v;
        FieldPacker fp = new FieldPacker(64);
        for (int ct1 = 0; ct1 < 16; ct1++) {
            fp.addI32(v[ct1]);
        }
        int[] __dimArr = {16};
        setVar(6, fp, this.__I32, __dimArr);
    }

    public synchronized void set_yPos2(int[] v) {
        this.mExportVar_yPos2 = v;
        FieldPacker fp = new FieldPacker(64);
        for (int ct1 = 0; ct1 < 16; ct1++) {
            fp.addI32(v[ct1]);
        }
        int[] __dimArr = {16};
        setVar(7, fp, this.__I32, __dimArr);
    }

    public synchronized void set_brightness(int[] v) {
        this.mExportVar_brightness = v;
        FieldPacker fp = new FieldPacker(64);
        for (int ct1 = 0; ct1 < 16; ct1++) {
            fp.addI32(v[ct1]);
        }
        int[] __dimArr = {16};
        setVar(9, fp, this.__I32, __dimArr);
    }

    public synchronized void set_contrast(int[] v) {
        this.mExportVar_contrast = v;
        FieldPacker fp = new FieldPacker(64);
        for (int ct1 = 0; ct1 < 16; ct1++) {
            fp.addI32(v[ct1]);
        }
        int[] __dimArr = {16};
        setVar(10, fp, this.__I32, __dimArr);
    }

    public synchronized void set_saturation(int[] v) {
        this.mExportVar_saturation = v;
        FieldPacker fp = new FieldPacker(64);
        for (int ct1 = 0; ct1 < 16; ct1++) {
            fp.addI32(v[ct1]);
        }
        int[] __dimArr = {16};
        setVar(11, fp, this.__I32, __dimArr);
    }

    public void forEach_selectiveAdjust(Allocation ain, Allocation aout, Script.LaunchOptions sc) {
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

    public void invoke_setupGradParams() {
        invoke(0);
    }
}
