package android.text.style;

import android.text.TextPaint;

public abstract class MetricAffectingSpan extends CharacterStyle implements UpdateLayout {
    public abstract void updateMeasureState(TextPaint textPaint);

    @Override
    public MetricAffectingSpan getUnderlying() {
        return this;
    }

    static class Passthrough extends MetricAffectingSpan {
        private MetricAffectingSpan mStyle;

        Passthrough(MetricAffectingSpan metricAffectingSpan) {
            this.mStyle = metricAffectingSpan;
        }

        @Override
        public void updateDrawState(TextPaint textPaint) {
            this.mStyle.updateDrawState(textPaint);
        }

        @Override
        public void updateMeasureState(TextPaint textPaint) {
            this.mStyle.updateMeasureState(textPaint);
        }

        @Override
        public MetricAffectingSpan getUnderlying() {
            return this.mStyle.getUnderlying();
        }
    }
}
