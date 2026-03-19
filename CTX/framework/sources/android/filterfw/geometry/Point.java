package android.filterfw.geometry;

public class Point {
    public float x;
    public float y;

    public Point() {
    }

    public Point(float f, float f2) {
        this.x = f;
        this.y = f2;
    }

    public void set(float f, float f2) {
        this.x = f;
        this.y = f2;
    }

    public boolean IsInUnitRange() {
        return this.x >= 0.0f && this.x <= 1.0f && this.y >= 0.0f && this.y <= 1.0f;
    }

    public Point plus(float f, float f2) {
        return new Point(this.x + f, this.y + f2);
    }

    public Point plus(Point point) {
        return plus(point.x, point.y);
    }

    public Point minus(float f, float f2) {
        return new Point(this.x - f, this.y - f2);
    }

    public Point minus(Point point) {
        return minus(point.x, point.y);
    }

    public Point times(float f) {
        return new Point(this.x * f, this.y * f);
    }

    public Point mult(float f, float f2) {
        return new Point(this.x * f, this.y * f2);
    }

    public float length() {
        return (float) Math.hypot(this.x, this.y);
    }

    public float distanceTo(Point point) {
        return point.minus(this).length();
    }

    public Point scaledTo(float f) {
        return times(f / length());
    }

    public Point normalize() {
        return scaledTo(1.0f);
    }

    public Point rotated90(int i) {
        float f = this.x;
        float f2 = this.y;
        int i2 = 0;
        while (i2 < i) {
            i2++;
            float f3 = f2;
            f2 = -f;
            f = f3;
        }
        return new Point(f, f2);
    }

    public Point rotated(float f) {
        double d = f;
        return new Point((float) ((Math.cos(d) * ((double) this.x)) - (Math.sin(d) * ((double) this.y))), (float) ((Math.sin(d) * ((double) this.x)) + (Math.cos(d) * ((double) this.y))));
    }

    public Point rotatedAround(Point point, float f) {
        return minus(point).rotated(f).plus(point);
    }

    public String toString() {
        return "(" + this.x + ", " + this.y + ")";
    }
}
