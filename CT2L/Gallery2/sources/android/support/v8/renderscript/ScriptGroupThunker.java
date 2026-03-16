package android.support.v8.renderscript;

import android.renderscript.ScriptGroup;
import android.support.v8.renderscript.Script;

class ScriptGroupThunker extends ScriptGroup {
    android.renderscript.ScriptGroup mN;

    @Override
    android.renderscript.ScriptGroup getNObj() {
        return this.mN;
    }

    ScriptGroupThunker(int id, RenderScript rs) {
        super(id, rs);
    }

    @Override
    public void setInput(Script.KernelID s, Allocation a) {
        AllocationThunker at = (AllocationThunker) a;
        try {
            this.mN.setInput(s.mN, at.getNObj());
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }

    @Override
    public void setOutput(Script.KernelID s, Allocation a) {
        AllocationThunker at = (AllocationThunker) a;
        try {
            this.mN.setOutput(s.mN, at.getNObj());
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }

    @Override
    public void execute() {
        try {
            this.mN.execute();
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }

    public static final class Builder {
        ScriptGroup.Builder bN;
        RenderScript mRS;

        Builder(RenderScript rs) {
            RenderScriptThunker rst = (RenderScriptThunker) rs;
            this.mRS = rs;
            try {
                this.bN = new ScriptGroup.Builder(rst.mN);
            } catch (android.renderscript.RSRuntimeException e) {
                throw ExceptionThunker.convertException(e);
            }
        }

        public Builder addKernel(Script.KernelID k) {
            try {
                this.bN.addKernel(k.mN);
                return this;
            } catch (android.renderscript.RSRuntimeException e) {
                throw ExceptionThunker.convertException(e);
            }
        }

        public Builder addConnection(Type t, Script.KernelID from, Script.FieldID to) {
            TypeThunker tt = (TypeThunker) t;
            try {
                this.bN.addConnection(tt.getNObj(), from.mN, to.mN);
                return this;
            } catch (android.renderscript.RSRuntimeException e) {
                throw ExceptionThunker.convertException(e);
            }
        }

        public Builder addConnection(Type t, Script.KernelID from, Script.KernelID to) {
            TypeThunker tt = (TypeThunker) t;
            try {
                this.bN.addConnection(tt.getNObj(), from.mN, to.mN);
                return this;
            } catch (android.renderscript.RSRuntimeException e) {
                throw ExceptionThunker.convertException(e);
            }
        }

        public ScriptGroupThunker create() {
            ScriptGroupThunker sg = new ScriptGroupThunker(0, this.mRS);
            try {
                sg.mN = this.bN.create();
                return sg;
            } catch (android.renderscript.RSRuntimeException e) {
                throw ExceptionThunker.convertException(e);
            }
        }
    }
}
