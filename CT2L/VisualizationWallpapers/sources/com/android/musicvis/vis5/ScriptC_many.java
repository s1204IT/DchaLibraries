package com.android.musicvis.vis5;

import android.content.res.Resources;
import android.renderscript.Allocation;
import android.renderscript.BaseObj;
import android.renderscript.Element;
import android.renderscript.Mesh;
import android.renderscript.ProgramFragment;
import android.renderscript.ProgramRaster;
import android.renderscript.ProgramStore;
import android.renderscript.ProgramVertex;
import android.renderscript.RenderScript;
import android.renderscript.ScriptC;

public class ScriptC_many extends ScriptC {
    private Element __ALLOCATION;
    private Element __F32;
    private Element __I32;
    private Element __MESH;
    private Element __PROGRAM_FRAGMENT;
    private Element __PROGRAM_RASTER;
    private Element __PROGRAM_STORE;
    private Element __PROGRAM_VERTEX;
    private float mExportVar_autorotation;
    private int mExportVar_fadeincounter;
    private int mExportVar_fadeoutcounter;
    private float mExportVar_gAngle;
    private Mesh mExportVar_gCubeMesh;
    private int mExportVar_gIdle;
    private ProgramFragment mExportVar_gPFBackgroundMip;
    private ProgramFragment mExportVar_gPFBackgroundNoMip;
    private ProgramStore mExportVar_gPFSBackground;
    private ProgramRaster mExportVar_gPR;
    private ProgramVertex mExportVar_gPVBackground;
    private int mExportVar_gPeak;
    private Allocation mExportVar_gPointBuffer;
    private ScriptField_Vertex mExportVar_gPoints;
    private float mExportVar_gRotate;
    private float mExportVar_gTilt;
    private Allocation mExportVar_gTlinetexture;
    private Allocation mExportVar_gTvumeter_album;
    private Allocation mExportVar_gTvumeter_background;
    private Allocation mExportVar_gTvumeter_black;
    private Allocation mExportVar_gTvumeter_frame;
    private Allocation mExportVar_gTvumeter_needle;
    private Allocation mExportVar_gTvumeter_peak_off;
    private Allocation mExportVar_gTvumeter_peak_on;
    private int mExportVar_gWaveCounter;
    private int mExportVar_lastuptime;
    private int mExportVar_wave1amp;
    private int mExportVar_wave1pos;
    private int mExportVar_wave2amp;
    private int mExportVar_wave2pos;
    private int mExportVar_wave3amp;
    private int mExportVar_wave3pos;
    private int mExportVar_wave4amp;
    private int mExportVar_wave4pos;
    private int mExportVar_waveCounter;

    public ScriptC_many(RenderScript rs, Resources resources, int id) {
        super(rs, resources, id);
        this.__F32 = Element.F32(rs);
        this.__I32 = Element.I32(rs);
        this.__PROGRAM_VERTEX = Element.PROGRAM_VERTEX(rs);
        this.__PROGRAM_FRAGMENT = Element.PROGRAM_FRAGMENT(rs);
        this.__PROGRAM_RASTER = Element.PROGRAM_RASTER(rs);
        this.__ALLOCATION = Element.ALLOCATION(rs);
        this.__PROGRAM_STORE = Element.PROGRAM_STORE(rs);
        this.__MESH = Element.MESH(rs);
        this.mExportVar_fadeoutcounter = 0;
        this.mExportVar_fadeincounter = 0;
        this.mExportVar_wave1pos = 0;
        this.mExportVar_wave1amp = 0;
        this.mExportVar_wave2pos = 0;
        this.mExportVar_wave2amp = 0;
        this.mExportVar_wave3pos = 0;
        this.mExportVar_wave3amp = 0;
        this.mExportVar_wave4pos = 0;
        this.mExportVar_wave4amp = 0;
        this.mExportVar_waveCounter = 0;
        this.mExportVar_lastuptime = 0;
        this.mExportVar_autorotation = 0.0f;
    }

    public synchronized void set_gAngle(float v) {
        setVar(0, v);
        this.mExportVar_gAngle = v;
    }

    public synchronized void set_gPeak(int v) {
        setVar(1, v);
        this.mExportVar_gPeak = v;
    }

    public synchronized void set_gRotate(float v) {
        setVar(2, v);
        this.mExportVar_gRotate = v;
    }

    public synchronized void set_gTilt(float v) {
        setVar(3, v);
        this.mExportVar_gTilt = v;
    }

    public synchronized void set_gIdle(int v) {
        setVar(4, v);
        this.mExportVar_gIdle = v;
    }

    public synchronized void set_gWaveCounter(int v) {
        setVar(5, v);
        this.mExportVar_gWaveCounter = v;
    }

    public synchronized void set_gPVBackground(ProgramVertex v) {
        setVar(6, (BaseObj) v);
        this.mExportVar_gPVBackground = v;
    }

    public synchronized void set_gPFBackgroundMip(ProgramFragment v) {
        setVar(7, (BaseObj) v);
        this.mExportVar_gPFBackgroundMip = v;
    }

    public synchronized void set_gPFBackgroundNoMip(ProgramFragment v) {
        setVar(8, (BaseObj) v);
        this.mExportVar_gPFBackgroundNoMip = v;
    }

    public synchronized void set_gPR(ProgramRaster v) {
        setVar(9, (BaseObj) v);
        this.mExportVar_gPR = v;
    }

    public synchronized void set_gTvumeter_background(Allocation v) {
        setVar(10, v);
        this.mExportVar_gTvumeter_background = v;
    }

    public synchronized void set_gTvumeter_peak_on(Allocation v) {
        setVar(11, v);
        this.mExportVar_gTvumeter_peak_on = v;
    }

    public synchronized void set_gTvumeter_peak_off(Allocation v) {
        setVar(12, v);
        this.mExportVar_gTvumeter_peak_off = v;
    }

    public synchronized void set_gTvumeter_needle(Allocation v) {
        setVar(13, v);
        this.mExportVar_gTvumeter_needle = v;
    }

    public synchronized void set_gTvumeter_black(Allocation v) {
        setVar(14, v);
        this.mExportVar_gTvumeter_black = v;
    }

    public synchronized void set_gTvumeter_frame(Allocation v) {
        setVar(15, v);
        this.mExportVar_gTvumeter_frame = v;
    }

    public synchronized void set_gTvumeter_album(Allocation v) {
        setVar(16, v);
        this.mExportVar_gTvumeter_album = v;
    }

    public synchronized void set_gPFSBackground(ProgramStore v) {
        setVar(17, (BaseObj) v);
        this.mExportVar_gPFSBackground = v;
    }

    public void bind_gPoints(ScriptField_Vertex v) {
        this.mExportVar_gPoints = v;
        if (v != null) {
            bindAllocation(v.getAllocation(), 18);
        } else {
            bindAllocation(null, 18);
        }
    }

    public synchronized void set_gPointBuffer(Allocation v) {
        setVar(19, v);
        this.mExportVar_gPointBuffer = v;
    }

    public synchronized void set_gTlinetexture(Allocation v) {
        setVar(20, v);
        this.mExportVar_gTlinetexture = v;
    }

    public synchronized void set_gCubeMesh(Mesh v) {
        setVar(21, (BaseObj) v);
        this.mExportVar_gCubeMesh = v;
    }
}
