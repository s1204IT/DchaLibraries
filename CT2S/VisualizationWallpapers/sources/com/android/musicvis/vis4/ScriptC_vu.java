package com.android.musicvis.vis4;

import android.content.res.Resources;
import android.renderscript.Allocation;
import android.renderscript.BaseObj;
import android.renderscript.Element;
import android.renderscript.ProgramFragment;
import android.renderscript.ProgramStore;
import android.renderscript.ProgramVertex;
import android.renderscript.RenderScript;
import android.renderscript.ScriptC;

public class ScriptC_vu extends ScriptC {
    private Element __ALLOCATION;
    private Element __F32;
    private Element __I32;
    private Element __PROGRAM_FRAGMENT;
    private Element __PROGRAM_STORE;
    private Element __PROGRAM_VERTEX;
    private float mExportVar_gAngle;
    private ProgramFragment mExportVar_gPFBackground;
    private ProgramStore mExportVar_gPFSBackground;
    private ProgramVertex mExportVar_gPVBackground;
    private int mExportVar_gPeak;
    private Allocation mExportVar_gTvumeter_background;
    private Allocation mExportVar_gTvumeter_black;
    private Allocation mExportVar_gTvumeter_frame;
    private Allocation mExportVar_gTvumeter_needle;
    private Allocation mExportVar_gTvumeter_peak_off;
    private Allocation mExportVar_gTvumeter_peak_on;

    public ScriptC_vu(RenderScript rs, Resources resources, int id) {
        super(rs, resources, id);
        this.__F32 = Element.F32(rs);
        this.__I32 = Element.I32(rs);
        this.__PROGRAM_VERTEX = Element.PROGRAM_VERTEX(rs);
        this.__PROGRAM_FRAGMENT = Element.PROGRAM_FRAGMENT(rs);
        this.__ALLOCATION = Element.ALLOCATION(rs);
        this.__PROGRAM_STORE = Element.PROGRAM_STORE(rs);
    }

    public synchronized void set_gAngle(float v) {
        setVar(0, v);
        this.mExportVar_gAngle = v;
    }

    public synchronized void set_gPeak(int v) {
        setVar(1, v);
        this.mExportVar_gPeak = v;
    }

    public synchronized void set_gPVBackground(ProgramVertex v) {
        setVar(2, (BaseObj) v);
        this.mExportVar_gPVBackground = v;
    }

    public synchronized void set_gPFBackground(ProgramFragment v) {
        setVar(3, (BaseObj) v);
        this.mExportVar_gPFBackground = v;
    }

    public synchronized void set_gTvumeter_background(Allocation v) {
        setVar(4, v);
        this.mExportVar_gTvumeter_background = v;
    }

    public synchronized void set_gTvumeter_peak_on(Allocation v) {
        setVar(5, v);
        this.mExportVar_gTvumeter_peak_on = v;
    }

    public synchronized void set_gTvumeter_peak_off(Allocation v) {
        setVar(6, v);
        this.mExportVar_gTvumeter_peak_off = v;
    }

    public synchronized void set_gTvumeter_needle(Allocation v) {
        setVar(7, v);
        this.mExportVar_gTvumeter_needle = v;
    }

    public synchronized void set_gTvumeter_black(Allocation v) {
        setVar(8, v);
        this.mExportVar_gTvumeter_black = v;
    }

    public synchronized void set_gTvumeter_frame(Allocation v) {
        setVar(9, v);
        this.mExportVar_gTvumeter_frame = v;
    }

    public synchronized void set_gPFSBackground(ProgramStore v) {
        setVar(10, (BaseObj) v);
        this.mExportVar_gPFSBackground = v;
    }
}
