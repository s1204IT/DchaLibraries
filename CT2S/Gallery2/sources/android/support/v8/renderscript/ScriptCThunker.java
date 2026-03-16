package android.support.v8.renderscript;

import android.content.res.Resources;
import android.renderscript.Script;
import android.support.v8.renderscript.Script;

class ScriptCThunker extends android.renderscript.ScriptC {
    private static final String TAG = "ScriptC";

    protected ScriptCThunker(RenderScriptThunker rs, Resources resources, int resourceID) {
        super(rs.mN, resources, resourceID);
    }

    Script.KernelID thunkCreateKernelID(int slot, int sig, Element ein, Element eout) {
        android.renderscript.Element nein = null;
        android.renderscript.Element neout = null;
        if (ein != null) {
            nein = ((ElementThunker) ein).mN;
        }
        if (eout != null) {
            neout = ((ElementThunker) eout).mN;
        }
        try {
            Script.KernelID kid = createKernelID(slot, sig, nein, neout);
            return kid;
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }

    void thunkInvoke(int slot) {
        try {
            invoke(slot);
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }

    void thunkBindAllocation(Allocation va, int slot) {
        android.renderscript.Allocation nva = null;
        if (va != null) {
            nva = ((AllocationThunker) va).mN;
        }
        try {
            bindAllocation(nva, slot);
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }

    void thunkSetTimeZone(String timeZone) {
        try {
            setTimeZone(timeZone);
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }

    void thunkInvoke(int slot, FieldPacker v) {
        try {
            android.renderscript.FieldPacker nfp = new android.renderscript.FieldPacker(v.getData());
            invoke(slot, nfp);
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }

    void thunkForEach(int slot, Allocation ain, Allocation aout, FieldPacker v) {
        android.renderscript.Allocation nin = null;
        android.renderscript.Allocation nout = null;
        android.renderscript.FieldPacker nfp = null;
        if (ain != null) {
            nin = ((AllocationThunker) ain).mN;
        }
        if (aout != null) {
            nout = ((AllocationThunker) aout).mN;
        }
        if (v != null) {
            try {
                android.renderscript.FieldPacker nfp2 = new android.renderscript.FieldPacker(v.getData());
                nfp = nfp2;
            } catch (android.renderscript.RSRuntimeException e) {
                throw ExceptionThunker.convertException(e);
            }
        }
        forEach(slot, nin, nout, nfp);
    }

    void thunkForEach(int slot, Allocation ain, Allocation aout, FieldPacker v, Script.LaunchOptions sc) {
        Script.LaunchOptions lo;
        Script.LaunchOptions lo2 = null;
        if (sc != null) {
            try {
                lo = new Script.LaunchOptions();
            } catch (android.renderscript.RSRuntimeException e) {
                e = e;
            }
            try {
                if (sc.getXEnd() > 0) {
                    lo.setX(sc.getXStart(), sc.getXEnd());
                }
                if (sc.getYEnd() > 0) {
                    lo.setY(sc.getYStart(), sc.getYEnd());
                }
                if (sc.getZEnd() > 0) {
                    lo.setZ(sc.getZStart(), sc.getZEnd());
                }
                lo2 = lo;
            } catch (android.renderscript.RSRuntimeException e2) {
                e = e2;
                throw ExceptionThunker.convertException(e);
            }
        }
        android.renderscript.Allocation nin = null;
        android.renderscript.Allocation nout = null;
        android.renderscript.FieldPacker nfp = null;
        if (ain != null) {
            nin = ((AllocationThunker) ain).mN;
        }
        if (aout != null) {
            nout = ((AllocationThunker) aout).mN;
        }
        if (v != null) {
            nfp = new android.renderscript.FieldPacker(v.getData());
        }
        forEach(slot, nin, nout, nfp, lo2);
    }

    void thunkSetVar(int index, float v) {
        try {
            setVar(index, v);
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }

    void thunkSetVar(int index, double v) {
        try {
            setVar(index, v);
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }

    void thunkSetVar(int index, int v) {
        try {
            setVar(index, v);
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }

    void thunkSetVar(int index, long v) {
        try {
            setVar(index, v);
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }

    void thunkSetVar(int index, boolean v) {
        try {
            setVar(index, v);
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }

    void thunkSetVar(int index, BaseObj o) {
        if (o == null) {
            try {
                setVar(index, 0);
            } catch (android.renderscript.RSRuntimeException e) {
                throw ExceptionThunker.convertException(e);
            }
        } else {
            try {
                setVar(index, o.getNObj());
            } catch (android.renderscript.RSRuntimeException e2) {
                throw ExceptionThunker.convertException(e2);
            }
        }
    }

    void thunkSetVar(int index, FieldPacker v) {
        try {
            android.renderscript.FieldPacker nfp = new android.renderscript.FieldPacker(v.getData());
            setVar(index, nfp);
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }

    void thunkSetVar(int index, FieldPacker v, Element e, int[] dims) {
        try {
            android.renderscript.FieldPacker nfp = new android.renderscript.FieldPacker(v.getData());
            ElementThunker et = (ElementThunker) e;
            setVar(index, nfp, et.mN, dims);
        } catch (android.renderscript.RSRuntimeException exc) {
            throw ExceptionThunker.convertException(exc);
        }
    }

    Script.FieldID thunkCreateFieldID(int slot, Element e) {
        try {
            ElementThunker et = (ElementThunker) e;
            return createFieldID(slot, et.getNObj());
        } catch (android.renderscript.RSRuntimeException exc) {
            throw ExceptionThunker.convertException(exc);
        }
    }
}
