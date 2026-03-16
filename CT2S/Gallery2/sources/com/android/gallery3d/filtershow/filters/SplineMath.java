package com.android.gallery3d.filtershow.filters;

import java.lang.reflect.Array;

public class SplineMath {
    double[] mDerivatives;
    double[][] mPoints;

    SplineMath(int n) {
        this.mPoints = (double[][]) Array.newInstance((Class<?>) Double.TYPE, 6, 2);
        this.mPoints = (double[][]) Array.newInstance((Class<?>) Double.TYPE, n, 2);
    }

    public void setPoint(int index, double x, double y) {
        this.mPoints[index][0] = x;
        this.mPoints[index][1] = y;
        this.mDerivatives = null;
    }

    public float[][] calculatetCurve(int n) {
        float[][] curve = (float[][]) Array.newInstance((Class<?>) Float.TYPE, n, 2);
        double[][] points = (double[][]) Array.newInstance((Class<?>) Double.TYPE, this.mPoints.length, 2);
        for (int i = 0; i < this.mPoints.length; i++) {
            points[i][0] = this.mPoints[i][0];
            points[i][1] = this.mPoints[i][1];
        }
        double[] derivatives = solveSystem(points);
        float start = (float) points[0][0];
        float end = (float) points[points.length - 1][0];
        curve[0][0] = (float) points[0][0];
        curve[0][1] = (float) points[0][1];
        int last = curve.length - 1;
        curve[last][0] = (float) points[points.length - 1][0];
        curve[last][1] = (float) points[points.length - 1][1];
        for (int i2 = 0; i2 < curve.length; i2++) {
            double x = ((i2 * (end - start)) / (curve.length - 1)) + start;
            int pivot = 0;
            for (int j = 0; j < points.length - 1; j++) {
                if (x >= points[j][0] && x <= points[j + 1][0]) {
                    pivot = j;
                }
            }
            double[] cur = points[pivot];
            double[] next = points[pivot + 1];
            if (x <= next[0]) {
                double x1 = cur[0];
                double x2 = next[0];
                double y1 = cur[1];
                double y2 = next[1];
                double delta = x2 - x1;
                double delta2 = delta * delta;
                double b = (x - x1) / delta;
                double a = 1.0d - b;
                double ta = a * y1;
                double tb = b * y2;
                double tc = (((a * a) * a) - a) * derivatives[pivot];
                double td = (((b * b) * b) - b) * derivatives[pivot + 1];
                double y = ta + tb + ((delta2 / 6.0d) * (tc + td));
                curve[i2][0] = (float) x;
                curve[i2][1] = (float) y;
            } else {
                curve[i2][0] = (float) next[0];
                curve[i2][1] = (float) next[1];
            }
        }
        return curve;
    }

    double[] solveSystem(double[][] points) {
        int n = points.length;
        double[][] system = (double[][]) Array.newInstance((Class<?>) Double.TYPE, n, 3);
        double[] result = new double[n];
        double[] solution = new double[n];
        system[0][1] = 1.0d;
        system[n - 1][1] = 1.0d;
        for (int i = 1; i < n - 1; i++) {
            double deltaPrevX = points[i][0] - points[i - 1][0];
            double deltaX = points[i + 1][0] - points[i - 1][0];
            double deltaNextX = points[i + 1][0] - points[i][0];
            double deltaNextY = points[i + 1][1] - points[i][1];
            double deltaPrevY = points[i][1] - points[i - 1][1];
            system[i][0] = 0.16666666666666666d * deltaPrevX;
            system[i][1] = 0.3333333333333333d * deltaX;
            system[i][2] = 0.16666666666666666d * deltaNextX;
            result[i] = (deltaNextY / deltaNextX) - (deltaPrevY / deltaPrevX);
        }
        for (int i2 = 1; i2 < n; i2++) {
            double m = system[i2][0] / system[i2 - 1][1];
            system[i2][1] = system[i2][1] - (system[i2 - 1][2] * m);
            result[i2] = result[i2] - (result[i2 - 1] * m);
        }
        solution[n - 1] = result[n - 1] / system[n - 1][1];
        for (int i3 = n - 2; i3 >= 0; i3--) {
            solution[i3] = (result[i3] - (system[i3][2] * solution[i3 + 1])) / system[i3][1];
        }
        return solution;
    }
}
