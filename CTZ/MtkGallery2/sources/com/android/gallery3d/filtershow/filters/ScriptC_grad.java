package com.android.gallery3d.filtershow.filters;

import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.FieldPacker;
import android.renderscript.RSRuntimeException;
import android.renderscript.RenderScript;
import android.renderscript.Script;
import android.renderscript.ScriptC;
import android.renderscript.Type;

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

    public ScriptC_grad(RenderScript renderScript) {
        super(renderScript, "grad", gradBitCode.getBitCode32(), gradBitCode.getBitCode64());
        this.__U32 = Element.U32(renderScript);
        this.__I32 = Element.I32(renderScript);
        this.__BOOLEAN = Element.BOOLEAN(renderScript);
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

    public synchronized void set_mask(boolean[] zArr) {
        this.mExportVar_mask = zArr;
        FieldPacker fieldPacker = new FieldPacker(16);
        for (int i = 0; i < 16; i++) {
            fieldPacker.addBoolean(zArr[i]);
        }
        setVar(3, fieldPacker, this.__BOOLEAN, new int[]{16});
    }

    public synchronized void set_xPos1(int[] iArr) {
        this.mExportVar_xPos1 = iArr;
        FieldPacker fieldPacker = new FieldPacker(64);
        for (int i = 0; i < 16; i++) {
            fieldPacker.addI32(iArr[i]);
        }
        setVar(4, fieldPacker, this.__I32, new int[]{16});
    }

    public synchronized void set_yPos1(int[] iArr) {
        this.mExportVar_yPos1 = iArr;
        FieldPacker fieldPacker = new FieldPacker(64);
        for (int i = 0; i < 16; i++) {
            fieldPacker.addI32(iArr[i]);
        }
        setVar(5, fieldPacker, this.__I32, new int[]{16});
    }

    public synchronized void set_xPos2(int[] iArr) {
        this.mExportVar_xPos2 = iArr;
        FieldPacker fieldPacker = new FieldPacker(64);
        for (int i = 0; i < 16; i++) {
            fieldPacker.addI32(iArr[i]);
        }
        setVar(6, fieldPacker, this.__I32, new int[]{16});
    }

    public synchronized void set_yPos2(int[] iArr) {
        this.mExportVar_yPos2 = iArr;
        FieldPacker fieldPacker = new FieldPacker(64);
        for (int i = 0; i < 16; i++) {
            fieldPacker.addI32(iArr[i]);
        }
        setVar(7, fieldPacker, this.__I32, new int[]{16});
    }

    public synchronized void set_brightness(int[] iArr) {
        this.mExportVar_brightness = iArr;
        FieldPacker fieldPacker = new FieldPacker(64);
        for (int i = 0; i < 16; i++) {
            fieldPacker.addI32(iArr[i]);
        }
        setVar(9, fieldPacker, this.__I32, new int[]{16});
    }

    public synchronized void set_contrast(int[] iArr) {
        this.mExportVar_contrast = iArr;
        FieldPacker fieldPacker = new FieldPacker(64);
        for (int i = 0; i < 16; i++) {
            fieldPacker.addI32(iArr[i]);
        }
        setVar(10, fieldPacker, this.__I32, new int[]{16});
    }

    public synchronized void set_saturation(int[] iArr) {
        this.mExportVar_saturation = iArr;
        FieldPacker fieldPacker = new FieldPacker(64);
        for (int i = 0; i < 16; i++) {
            fieldPacker.addI32(iArr[i]);
        }
        setVar(11, fieldPacker, this.__I32, new int[]{16});
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
