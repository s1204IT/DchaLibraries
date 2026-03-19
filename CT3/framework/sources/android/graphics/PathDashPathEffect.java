package android.graphics;

public class PathDashPathEffect extends PathEffect {
    private static native long nativeCreate(long j, float f, float f2, int i);

    public enum Style {
        TRANSLATE(0),
        ROTATE(1),
        MORPH(2);

        int native_style;

        public static Style[] valuesCustom() {
            return values();
        }

        Style(int value) {
            this.native_style = value;
        }
    }

    public PathDashPathEffect(Path shape, float advance, float phase, Style style) {
        this.native_instance = nativeCreate(shape.ni(), advance, phase, style.native_style);
    }
}
