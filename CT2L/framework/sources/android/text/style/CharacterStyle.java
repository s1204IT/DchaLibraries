package android.text.style;

import android.text.TextPaint;
import android.text.style.MetricAffectingSpan;

public abstract class CharacterStyle {
    public abstract void updateDrawState(TextPaint textPaint);

    public static CharacterStyle wrap(CharacterStyle cs) {
        return cs instanceof MetricAffectingSpan ? new MetricAffectingSpan.Passthrough((MetricAffectingSpan) cs) : new Passthrough(cs);
    }

    public CharacterStyle getUnderlying() {
        return this;
    }

    private static class Passthrough extends CharacterStyle {
        private CharacterStyle mStyle;

        public Passthrough(CharacterStyle cs) {
            this.mStyle = cs;
        }

        @Override
        public void updateDrawState(TextPaint tp) {
            this.mStyle.updateDrawState(tp);
        }

        @Override
        public CharacterStyle getUnderlying() {
            return this.mStyle.getUnderlying();
        }
    }
}
