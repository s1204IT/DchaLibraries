package android.text.style;

import android.text.TextPaint;
import android.text.style.MetricAffectingSpan;

public abstract class CharacterStyle {
    public abstract void updateDrawState(TextPaint textPaint);

    public static CharacterStyle wrap(CharacterStyle characterStyle) {
        if (characterStyle instanceof MetricAffectingSpan) {
            return new MetricAffectingSpan.Passthrough((MetricAffectingSpan) characterStyle);
        }
        return new Passthrough(characterStyle);
    }

    public CharacterStyle getUnderlying() {
        return this;
    }

    private static class Passthrough extends CharacterStyle {
        private CharacterStyle mStyle;

        public Passthrough(CharacterStyle characterStyle) {
            this.mStyle = characterStyle;
        }

        @Override
        public void updateDrawState(TextPaint textPaint) {
            this.mStyle.updateDrawState(textPaint);
        }

        @Override
        public CharacterStyle getUnderlying() {
            return this.mStyle.getUnderlying();
        }
    }
}
