package com.android.server.wifi.util;

import android.hardware.wifi.supplicant.V1_0.ISupplicantStaIfaceCallback;

public class Matrix {
    public final int m;
    public final double[] mem;
    public final int n;

    public Matrix(int i, int i2) {
        this.n = i;
        this.m = i2;
        this.mem = new double[i * i2];
    }

    public Matrix(int i, double[] dArr) {
        this.n = ((dArr.length + i) - 1) / i;
        this.m = i;
        this.mem = dArr;
        if (this.mem.length != this.n * this.m) {
            throw new IllegalArgumentException();
        }
    }

    public Matrix(Matrix matrix) {
        this.n = matrix.n;
        this.m = matrix.m;
        this.mem = new double[matrix.mem.length];
        for (int i = 0; i < this.mem.length; i++) {
            this.mem[i] = matrix.mem[i];
        }
    }

    public double get(int i, int i2) {
        if (i < 0 || i >= this.n || i2 < 0 || i2 >= this.m) {
            throw new IndexOutOfBoundsException();
        }
        return this.mem[(i * this.m) + i2];
    }

    public void put(int i, int i2, double d) {
        if (i < 0 || i >= this.n || i2 < 0 || i2 >= this.m) {
            throw new IndexOutOfBoundsException();
        }
        this.mem[(i * this.m) + i2] = d;
    }

    public Matrix plus(Matrix matrix) {
        return plus(matrix, new Matrix(this.n, this.m));
    }

    public Matrix plus(Matrix matrix, Matrix matrix2) {
        if (this.n != matrix.n || this.m != matrix.m || this.n != matrix2.n || this.m != matrix2.m) {
            throw new IllegalArgumentException();
        }
        for (int i = 0; i < this.mem.length; i++) {
            matrix2.mem[i] = this.mem[i] + matrix.mem[i];
        }
        return matrix2;
    }

    public Matrix minus(Matrix matrix) {
        return minus(matrix, new Matrix(this.n, this.m));
    }

    public Matrix minus(Matrix matrix, Matrix matrix2) {
        if (this.n != matrix.n || this.m != matrix.m || this.n != matrix2.n || this.m != matrix2.m) {
            throw new IllegalArgumentException();
        }
        for (int i = 0; i < this.mem.length; i++) {
            matrix2.mem[i] = this.mem[i] - matrix.mem[i];
        }
        return matrix2;
    }

    public Matrix times(double d) {
        return times(d, new Matrix(this.n, this.m));
    }

    public Matrix times(double d, Matrix matrix) {
        if (this.n != matrix.n || this.m != matrix.m) {
            throw new IllegalArgumentException();
        }
        for (int i = 0; i < this.mem.length; i++) {
            matrix.mem[i] = this.mem[i] * d;
        }
        return matrix;
    }

    public Matrix dot(Matrix matrix) {
        return dot(matrix, new Matrix(this.n, matrix.m));
    }

    public Matrix dot(Matrix matrix, Matrix matrix2) {
        if (this.n != matrix2.n || this.m != matrix.n || matrix.m != matrix2.m) {
            throw new IllegalArgumentException();
        }
        for (int i = 0; i < this.n; i++) {
            for (int i2 = 0; i2 < matrix.m; i2++) {
                double d = 0.0d;
                for (int i3 = 0; i3 < this.m; i3++) {
                    d += get(i, i3) * matrix.get(i3, i2);
                }
                matrix2.put(i, i2, d);
            }
        }
        return matrix2;
    }

    public Matrix transpose() {
        return transpose(new Matrix(this.m, this.n));
    }

    public Matrix transpose(Matrix matrix) {
        if (this.n != matrix.m || this.m != matrix.n) {
            throw new IllegalArgumentException();
        }
        for (int i = 0; i < this.n; i++) {
            for (int i2 = 0; i2 < this.m; i2++) {
                matrix.put(i2, i, get(i, i2));
            }
        }
        return matrix;
    }

    public Matrix inverse() {
        return inverse(new Matrix(this.n, this.m), new Matrix(this.n, 2 * this.m));
    }

    public Matrix inverse(Matrix matrix, Matrix matrix2) {
        if (this.n != this.m || this.n != matrix.n || this.m != matrix.m || this.n != matrix2.n || 2 * this.m != matrix2.m) {
            throw new IllegalArgumentException();
        }
        int i = 0;
        while (i < this.n) {
            int i2 = 0;
            while (i2 < this.m) {
                matrix2.put(i, i2, get(i, i2));
                matrix2.put(i, this.m + i2, i == i2 ? 1.0d : 0.0d);
                i2++;
            }
            i++;
        }
        int i3 = 0;
        while (i3 < this.n) {
            int i4 = i3 + 1;
            int i5 = i3;
            double dAbs = Math.abs(matrix2.get(i3, i3));
            for (int i6 = i4; i6 < this.n; i6++) {
                double dAbs2 = Math.abs(matrix2.get(i6, i3));
                if (dAbs2 > dAbs) {
                    i5 = i6;
                    dAbs = dAbs2;
                }
            }
            if (i5 != i3) {
                for (int i7 = 0; i7 < matrix2.m; i7++) {
                    double d = matrix2.get(i3, i7);
                    matrix2.put(i3, i7, matrix2.get(i5, i7));
                    matrix2.put(i5, i7, d);
                }
            }
            double d2 = matrix2.get(i3, i3);
            if (d2 == 0.0d) {
                throw new ArithmeticException("Singular matrix");
            }
            for (int i8 = 0; i8 < matrix2.m; i8++) {
                matrix2.put(i3, i8, matrix2.get(i3, i8) / d2);
            }
            for (int i9 = i4; i9 < this.n; i9++) {
                double d3 = matrix2.get(i9, i3);
                for (int i10 = 0; i10 < matrix2.m; i10++) {
                    matrix2.put(i9, i10, matrix2.get(i9, i10) - (matrix2.get(i3, i10) * d3));
                }
            }
            i3 = i4;
        }
        for (int i11 = this.n - 1; i11 >= 0; i11--) {
            for (int i12 = 0; i12 < i11; i12++) {
                double d4 = matrix2.get(i12, i11);
                for (int i13 = 0; i13 < matrix2.m; i13++) {
                    matrix2.put(i12, i13, matrix2.get(i12, i13) - (matrix2.get(i11, i13) * d4));
                }
            }
        }
        for (int i14 = 0; i14 < matrix.n; i14++) {
            for (int i15 = 0; i15 < matrix.m; i15++) {
                matrix.put(i14, i15, matrix2.get(i14, this.m + i15));
            }
        }
        return matrix;
    }

    public Matrix dotTranspose(Matrix matrix) {
        return dotTranspose(matrix, new Matrix(this.n, matrix.n));
    }

    public Matrix dotTranspose(Matrix matrix, Matrix matrix2) {
        if (this.n != matrix2.n || this.m != matrix.m || matrix.n != matrix2.m) {
            throw new IllegalArgumentException();
        }
        for (int i = 0; i < this.n; i++) {
            for (int i2 = 0; i2 < matrix.n; i2++) {
                double d = 0.0d;
                for (int i3 = 0; i3 < this.m; i3++) {
                    d += get(i, i3) * matrix.get(i2, i3);
                }
                matrix2.put(i, i2, d);
            }
        }
        return matrix2;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Matrix)) {
            return false;
        }
        Matrix matrix = (Matrix) obj;
        if (this.n != matrix.n || this.m != matrix.m) {
            return false;
        }
        for (int i = 0; i < this.mem.length; i++) {
            if (this.mem[i] != matrix.mem[i]) {
                return false;
            }
        }
        return true;
    }

    public int hashCode() {
        int iHashCode = (this.n * ISupplicantStaIfaceCallback.StatusCode.MAF_LIMIT_EXCEEDED) + this.m;
        for (int i = 0; i < this.mem.length; i++) {
            iHashCode = (iHashCode * 37) + Double.hashCode(this.mem[i]);
        }
        return iHashCode;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder(this.n * this.m * 8);
        sb.append("[");
        for (int i = 0; i < this.mem.length; i++) {
            if (i > 0) {
                sb.append(i % this.m == 0 ? "; " : ", ");
            }
            sb.append(this.mem[i]);
        }
        sb.append("]");
        return sb.toString();
    }
}
