package com.android.musicvis;

import android.content.res.Resources;
import android.renderscript.Allocation;
import android.renderscript.BaseObj;
import android.renderscript.Element;
import android.renderscript.Mesh;
import android.renderscript.ProgramFragment;
import android.renderscript.ProgramVertex;
import android.renderscript.RenderScript;
import android.renderscript.ScriptC;

public class ScriptC_waveform extends ScriptC {
    private Element __ALLOCATION;
    private Element __F32;
    private Element __I32;
    private Element __MESH;
    private Element __PROGRAM_FRAGMENT;
    private Element __PROGRAM_VERTEX;
    private Mesh mExportVar_gCubeMesh;
    private int mExportVar_gIdle;
    private ProgramFragment mExportVar_gPFBackground;
    private ProgramVertex mExportVar_gPVBackground;
    private Allocation mExportVar_gPointBuffer;
    private ScriptField_Vertex mExportVar_gPoints;
    private Allocation mExportVar_gTlinetexture;
    private int mExportVar_gWaveCounter;
    private int mExportVar_gWidth;
    private float mExportVar_gYRotation;

    public ScriptC_waveform(RenderScript rs, Resources resources, int id) {
        super(rs, resources, id);
        this.__F32 = Element.F32(rs);
        this.__I32 = Element.I32(rs);
        this.__PROGRAM_VERTEX = Element.PROGRAM_VERTEX(rs);
        this.__PROGRAM_FRAGMENT = Element.PROGRAM_FRAGMENT(rs);
        this.__ALLOCATION = Element.ALLOCATION(rs);
        this.__MESH = Element.MESH(rs);
    }

    public synchronized void set_gYRotation(float v) {
        setVar(0, v);
        this.mExportVar_gYRotation = v;
    }

    public synchronized void set_gIdle(int v) {
        setVar(1, v);
        this.mExportVar_gIdle = v;
    }

    public synchronized void set_gWaveCounter(int v) {
        setVar(2, v);
        this.mExportVar_gWaveCounter = v;
    }

    public synchronized void set_gWidth(int v) {
        setVar(3, v);
        this.mExportVar_gWidth = v;
    }

    public synchronized void set_gPVBackground(ProgramVertex v) {
        setVar(4, (BaseObj) v);
        this.mExportVar_gPVBackground = v;
    }

    public synchronized void set_gPFBackground(ProgramFragment v) {
        setVar(5, (BaseObj) v);
        this.mExportVar_gPFBackground = v;
    }

    public void bind_gPoints(ScriptField_Vertex v) {
        this.mExportVar_gPoints = v;
        if (v != null) {
            bindAllocation(v.getAllocation(), 6);
        } else {
            bindAllocation(null, 6);
        }
    }

    public synchronized void set_gPointBuffer(Allocation v) {
        setVar(7, v);
        this.mExportVar_gPointBuffer = v;
    }

    public synchronized void set_gTlinetexture(Allocation v) {
        setVar(8, v);
        this.mExportVar_gTlinetexture = v;
    }

    public synchronized void set_gCubeMesh(Mesh v) {
        setVar(9, (BaseObj) v);
        this.mExportVar_gCubeMesh = v;
    }
}
