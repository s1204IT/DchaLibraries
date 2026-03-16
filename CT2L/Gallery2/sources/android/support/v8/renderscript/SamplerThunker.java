package android.support.v8.renderscript;

import android.renderscript.Sampler;
import android.support.v4.app.FragmentManagerImpl;
import android.support.v8.renderscript.Sampler;

class SamplerThunker extends Sampler {
    android.renderscript.Sampler mN;

    protected SamplerThunker(int id, RenderScript rs) {
        super(id, rs);
    }

    @Override
    android.renderscript.BaseObj getNObj() {
        return this.mN;
    }

    static class AnonymousClass1 {
        static final int[] $SwitchMap$android$support$v8$renderscript$Sampler$Value = new int[Sampler.Value.values().length];

        static {
            try {
                $SwitchMap$android$support$v8$renderscript$Sampler$Value[Sampler.Value.NEAREST.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$android$support$v8$renderscript$Sampler$Value[Sampler.Value.LINEAR.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$android$support$v8$renderscript$Sampler$Value[Sampler.Value.LINEAR_MIP_LINEAR.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                $SwitchMap$android$support$v8$renderscript$Sampler$Value[Sampler.Value.LINEAR_MIP_NEAREST.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
            try {
                $SwitchMap$android$support$v8$renderscript$Sampler$Value[Sampler.Value.WRAP.ordinal()] = 5;
            } catch (NoSuchFieldError e5) {
            }
            try {
                $SwitchMap$android$support$v8$renderscript$Sampler$Value[Sampler.Value.CLAMP.ordinal()] = 6;
            } catch (NoSuchFieldError e6) {
            }
            try {
                $SwitchMap$android$support$v8$renderscript$Sampler$Value[Sampler.Value.MIRRORED_REPEAT.ordinal()] = 7;
            } catch (NoSuchFieldError e7) {
            }
        }
    }

    static Sampler.Value convertValue(Sampler.Value v) {
        switch (AnonymousClass1.$SwitchMap$android$support$v8$renderscript$Sampler$Value[v.ordinal()]) {
            case 1:
                return Sampler.Value.NEAREST;
            case 2:
                return Sampler.Value.LINEAR;
            case 3:
                return Sampler.Value.LINEAR_MIP_LINEAR;
            case 4:
                return Sampler.Value.LINEAR_MIP_NEAREST;
            case 5:
                return Sampler.Value.WRAP;
            case FragmentManagerImpl.ANIM_STYLE_FADE_EXIT:
                return Sampler.Value.CLAMP;
            case 7:
                return Sampler.Value.MIRRORED_REPEAT;
            default:
                return null;
        }
    }

    public static class Builder {
        float mAniso;
        RenderScriptThunker mRS;
        Sampler.Value mMin = Sampler.Value.NEAREST;
        Sampler.Value mMag = Sampler.Value.NEAREST;
        Sampler.Value mWrapS = Sampler.Value.WRAP;
        Sampler.Value mWrapT = Sampler.Value.WRAP;
        Sampler.Value mWrapR = Sampler.Value.WRAP;

        public Builder(RenderScriptThunker rs) {
            this.mRS = rs;
        }

        public void setMinification(Sampler.Value v) {
            if (v == Sampler.Value.NEAREST || v == Sampler.Value.LINEAR || v == Sampler.Value.LINEAR_MIP_LINEAR || v == Sampler.Value.LINEAR_MIP_NEAREST) {
                this.mMin = v;
                return;
            }
            throw new IllegalArgumentException("Invalid value");
        }

        public void setMagnification(Sampler.Value v) {
            if (v == Sampler.Value.NEAREST || v == Sampler.Value.LINEAR) {
                this.mMag = v;
                return;
            }
            throw new IllegalArgumentException("Invalid value");
        }

        public void setWrapS(Sampler.Value v) {
            if (v == Sampler.Value.WRAP || v == Sampler.Value.CLAMP || v == Sampler.Value.MIRRORED_REPEAT) {
                this.mWrapS = v;
                return;
            }
            throw new IllegalArgumentException("Invalid value");
        }

        public void setWrapT(Sampler.Value v) {
            if (v == Sampler.Value.WRAP || v == Sampler.Value.CLAMP || v == Sampler.Value.MIRRORED_REPEAT) {
                this.mWrapT = v;
                return;
            }
            throw new IllegalArgumentException("Invalid value");
        }

        public void setAnisotropy(float v) {
            if (v >= 0.0f) {
                this.mAniso = v;
                return;
            }
            throw new IllegalArgumentException("Invalid value");
        }

        public Sampler create() {
            this.mRS.validate();
            try {
                Sampler.Builder b = new Sampler.Builder(this.mRS.mN);
                b.setMinification(SamplerThunker.convertValue(this.mMin));
                b.setMagnification(SamplerThunker.convertValue(this.mMag));
                b.setWrapS(SamplerThunker.convertValue(this.mWrapS));
                b.setWrapT(SamplerThunker.convertValue(this.mWrapT));
                b.setAnisotropy(this.mAniso);
                android.renderscript.Sampler s = b.create();
                SamplerThunker sampler = new SamplerThunker(0, this.mRS);
                sampler.mMin = this.mMin;
                sampler.mMag = this.mMag;
                sampler.mWrapS = this.mWrapS;
                sampler.mWrapT = this.mWrapT;
                sampler.mWrapR = this.mWrapR;
                sampler.mAniso = this.mAniso;
                sampler.mN = s;
                return sampler;
            } catch (android.renderscript.RSRuntimeException e) {
                throw ExceptionThunker.convertException(e);
            }
        }
    }
}
