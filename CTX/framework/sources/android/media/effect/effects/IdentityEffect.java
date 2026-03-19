package android.media.effect.effects;

import android.filterfw.core.Frame;
import android.media.effect.EffectContext;
import android.media.effect.FilterEffect;

public class IdentityEffect extends FilterEffect {
    public IdentityEffect(EffectContext effectContext, String str) {
        super(effectContext, str);
    }

    @Override
    public void apply(int i, int i2, int i3, int i4) {
        beginGLEffect();
        Frame frameFrameFromTexture = frameFromTexture(i, i2, i3);
        Frame frameFrameFromTexture2 = frameFromTexture(i4, i2, i3);
        frameFrameFromTexture2.setDataFromFrame(frameFrameFromTexture);
        frameFrameFromTexture.release();
        frameFrameFromTexture2.release();
        endGLEffect();
    }

    @Override
    public void setParameter(String str, Object obj) {
        throw new IllegalArgumentException("Unknown parameter " + str + " for IdentityEffect!");
    }

    @Override
    public void release() {
    }
}
