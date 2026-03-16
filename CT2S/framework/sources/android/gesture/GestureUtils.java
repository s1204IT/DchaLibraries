package android.gesture;

import android.graphics.RectF;
import android.util.Log;
import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;

public final class GestureUtils {
    private static final float NONUNIFORM_SCALE = (float) Math.sqrt(2.0d);
    private static final float SCALING_THRESHOLD = 0.26f;

    private GestureUtils() {
    }

    static void closeStream(Closeable stream) {
        if (stream != null) {
            try {
                stream.close();
            } catch (IOException e) {
                Log.e(GestureConstants.LOG_TAG, "Could not close stream", e);
            }
        }
    }

    public static float[] spatialSampling(Gesture gesture, int bitmapSize) {
        return spatialSampling(gesture, bitmapSize, false);
    }

    public static float[] spatialSampling(Gesture gesture, int bitmapSize, boolean keepAspectRatio) {
        float targetPatchSize = bitmapSize - 1;
        float[] sample = new float[bitmapSize * bitmapSize];
        Arrays.fill(sample, 0.0f);
        RectF rect = gesture.getBoundingBox();
        float gestureWidth = rect.width();
        float gestureHeight = rect.height();
        float sx = targetPatchSize / gestureWidth;
        float sy = targetPatchSize / gestureHeight;
        if (keepAspectRatio) {
            float scale = sx < sy ? sx : sy;
            sx = scale;
            sy = scale;
        } else {
            float aspectRatio = gestureWidth / gestureHeight;
            if (aspectRatio > 1.0f) {
                aspectRatio = 1.0f / aspectRatio;
            }
            if (aspectRatio < SCALING_THRESHOLD) {
                float scale2 = sx < sy ? sx : sy;
                sx = scale2;
                sy = scale2;
            } else if (sx > sy) {
                float scale3 = sy * NONUNIFORM_SCALE;
                if (scale3 < sx) {
                    sx = scale3;
                }
            } else {
                float scale4 = sx * NONUNIFORM_SCALE;
                if (scale4 < sy) {
                    sy = scale4;
                }
            }
        }
        float preDx = -rect.centerX();
        float preDy = -rect.centerY();
        float postDx = targetPatchSize / 2.0f;
        float postDy = targetPatchSize / 2.0f;
        ArrayList<GestureStroke> strokes = gesture.getStrokes();
        int count = strokes.size();
        for (int index = 0; index < count; index++) {
            GestureStroke stroke = strokes.get(index);
            float[] strokepoints = stroke.points;
            int size = strokepoints.length;
            float[] pts = new float[size];
            for (int i = 0; i < size; i += 2) {
                pts[i] = ((strokepoints[i] + preDx) * sx) + postDx;
                pts[i + 1] = ((strokepoints[i + 1] + preDy) * sy) + postDy;
            }
            float segmentEndX = -1.0f;
            float segmentEndY = -1.0f;
            for (int i2 = 0; i2 < size; i2 += 2) {
                float segmentStartX = pts[i2] < 0.0f ? 0.0f : pts[i2];
                float segmentStartY = pts[i2 + 1] < 0.0f ? 0.0f : pts[i2 + 1];
                if (segmentStartX > targetPatchSize) {
                    segmentStartX = targetPatchSize;
                }
                if (segmentStartY > targetPatchSize) {
                    segmentStartY = targetPatchSize;
                }
                plot(segmentStartX, segmentStartY, sample, bitmapSize);
                if (segmentEndX != -1.0f) {
                    if (segmentEndX > segmentStartX) {
                        float slope = (segmentEndY - segmentStartY) / (segmentEndX - segmentStartX);
                        for (float xpos = (float) Math.ceil(segmentStartX); xpos < segmentEndX; xpos += 1.0f) {
                            float ypos = ((xpos - segmentStartX) * slope) + segmentStartY;
                            plot(xpos, ypos, sample, bitmapSize);
                        }
                    } else if (segmentEndX < segmentStartX) {
                        float slope2 = (segmentEndY - segmentStartY) / (segmentEndX - segmentStartX);
                        for (float xpos2 = (float) Math.ceil(segmentEndX); xpos2 < segmentStartX; xpos2 += 1.0f) {
                            float ypos2 = ((xpos2 - segmentStartX) * slope2) + segmentStartY;
                            plot(xpos2, ypos2, sample, bitmapSize);
                        }
                    }
                    if (segmentEndY > segmentStartY) {
                        float invertSlope = (segmentEndX - segmentStartX) / (segmentEndY - segmentStartY);
                        for (float ypos3 = (float) Math.ceil(segmentStartY); ypos3 < segmentEndY; ypos3 += 1.0f) {
                            float xpos3 = ((ypos3 - segmentStartY) * invertSlope) + segmentStartX;
                            plot(xpos3, ypos3, sample, bitmapSize);
                        }
                    } else if (segmentEndY < segmentStartY) {
                        float invertSlope2 = (segmentEndX - segmentStartX) / (segmentEndY - segmentStartY);
                        for (float ypos4 = (float) Math.ceil(segmentEndY); ypos4 < segmentStartY; ypos4 += 1.0f) {
                            float xpos4 = ((ypos4 - segmentStartY) * invertSlope2) + segmentStartX;
                            plot(xpos4, ypos4, sample, bitmapSize);
                        }
                    }
                }
                segmentEndX = segmentStartX;
                segmentEndY = segmentStartY;
            }
        }
        return sample;
    }

    private static void plot(float x, float y, float[] sample, int sampleSize) {
        if (x < 0.0f) {
            x = 0.0f;
        }
        if (y < 0.0f) {
            y = 0.0f;
        }
        int xFloor = (int) Math.floor(x);
        int xCeiling = (int) Math.ceil(x);
        int yFloor = (int) Math.floor(y);
        int yCeiling = (int) Math.ceil(y);
        if (x == xFloor && y == yFloor) {
            int index = (yCeiling * sampleSize) + xCeiling;
            if (sample[index] < 1.0f) {
                sample[index] = 1.0f;
                return;
            }
            return;
        }
        double xFloorSq = Math.pow(xFloor - x, 2.0d);
        double yFloorSq = Math.pow(yFloor - y, 2.0d);
        double xCeilingSq = Math.pow(xCeiling - x, 2.0d);
        double yCeilingSq = Math.pow(yCeiling - y, 2.0d);
        float topLeft = (float) Math.sqrt(xFloorSq + yFloorSq);
        float topRight = (float) Math.sqrt(xCeilingSq + yFloorSq);
        float btmLeft = (float) Math.sqrt(xFloorSq + yCeilingSq);
        float btmRight = (float) Math.sqrt(xCeilingSq + yCeilingSq);
        float sum = topLeft + topRight + btmLeft + btmRight;
        float value = topLeft / sum;
        int index2 = (yFloor * sampleSize) + xFloor;
        if (value > sample[index2]) {
            sample[index2] = value;
        }
        float value2 = topRight / sum;
        int index3 = (yFloor * sampleSize) + xCeiling;
        if (value2 > sample[index3]) {
            sample[index3] = value2;
        }
        float value3 = btmLeft / sum;
        int index4 = (yCeiling * sampleSize) + xFloor;
        if (value3 > sample[index4]) {
            sample[index4] = value3;
        }
        float value4 = btmRight / sum;
        int index5 = (yCeiling * sampleSize) + xCeiling;
        if (value4 > sample[index5]) {
            sample[index5] = value4;
        }
    }

    public static float[] temporalSampling(GestureStroke stroke, int numPoints) {
        float increment = stroke.length / (numPoints - 1);
        int vectorLength = numPoints * 2;
        float[] vector = new float[vectorLength];
        float distanceSoFar = 0.0f;
        float[] pts = stroke.points;
        float lstPointX = pts[0];
        float lstPointY = pts[1];
        float currentPointX = Float.MIN_VALUE;
        float currentPointY = Float.MIN_VALUE;
        vector[0] = lstPointX;
        int index = 0 + 1;
        vector[index] = lstPointY;
        int index2 = index + 1;
        int i = 0;
        int count = pts.length / 2;
        while (i < count) {
            if (currentPointX == Float.MIN_VALUE) {
                i++;
                if (i >= count) {
                    break;
                }
                currentPointX = pts[i * 2];
                currentPointY = pts[(i * 2) + 1];
            }
            float deltaX = currentPointX - lstPointX;
            float deltaY = currentPointY - lstPointY;
            float distance = (float) Math.sqrt((deltaX * deltaX) + (deltaY * deltaY));
            if (distanceSoFar + distance >= increment) {
                float ratio = (increment - distanceSoFar) / distance;
                float nx = lstPointX + (ratio * deltaX);
                float ny = lstPointY + (ratio * deltaY);
                vector[index2] = nx;
                int index3 = index2 + 1;
                vector[index3] = ny;
                index2 = index3 + 1;
                lstPointX = nx;
                lstPointY = ny;
                distanceSoFar = 0.0f;
            } else {
                lstPointX = currentPointX;
                lstPointY = currentPointY;
                currentPointX = Float.MIN_VALUE;
                currentPointY = Float.MIN_VALUE;
                distanceSoFar += distance;
            }
        }
        for (int i2 = index2; i2 < vectorLength; i2 += 2) {
            vector[i2] = lstPointX;
            vector[i2 + 1] = lstPointY;
        }
        return vector;
    }

    static float[] computeCentroid(float[] points) {
        float centerX = 0.0f;
        float centerY = 0.0f;
        int count = points.length;
        int i = 0;
        while (i < count) {
            centerX += points[i];
            int i2 = i + 1;
            centerY += points[i2];
            i = i2 + 1;
        }
        float[] center = {(2.0f * centerX) / count, (2.0f * centerY) / count};
        return center;
    }

    private static float[][] computeCoVariance(float[] points) {
        float[][] array = (float[][]) Array.newInstance((Class<?>) Float.TYPE, 2, 2);
        array[0][0] = 0.0f;
        array[0][1] = 0.0f;
        array[1][0] = 0.0f;
        array[1][1] = 0.0f;
        int count = points.length;
        int i = 0;
        while (i < count) {
            float x = points[i];
            int i2 = i + 1;
            float y = points[i2];
            float[] fArr = array[0];
            fArr[0] = fArr[0] + (x * x);
            float[] fArr2 = array[0];
            fArr2[1] = fArr2[1] + (x * y);
            array[1][0] = array[0][1];
            float[] fArr3 = array[1];
            fArr3[1] = fArr3[1] + (y * y);
            i = i2 + 1;
        }
        float[] fArr4 = array[0];
        fArr4[0] = fArr4[0] / (count / 2);
        float[] fArr5 = array[0];
        fArr5[1] = fArr5[1] / (count / 2);
        float[] fArr6 = array[1];
        fArr6[0] = fArr6[0] / (count / 2);
        float[] fArr7 = array[1];
        fArr7[1] = fArr7[1] / (count / 2);
        return array;
    }

    static float computeTotalLength(float[] points) {
        float sum = 0.0f;
        int count = points.length - 4;
        for (int i = 0; i < count; i += 2) {
            float dx = points[i + 2] - points[i];
            float dy = points[i + 3] - points[i + 1];
            sum = (float) (((double) sum) + Math.sqrt((dx * dx) + (dy * dy)));
        }
        return sum;
    }

    static float computeStraightness(float[] points) {
        float totalLen = computeTotalLength(points);
        float dx = points[2] - points[0];
        float dy = points[3] - points[1];
        return ((float) Math.sqrt((dx * dx) + (dy * dy))) / totalLen;
    }

    static float computeStraightness(float[] points, float totalLen) {
        float dx = points[2] - points[0];
        float dy = points[3] - points[1];
        return ((float) Math.sqrt((dx * dx) + (dy * dy))) / totalLen;
    }

    static float squaredEuclideanDistance(float[] vector1, float[] vector2) {
        float squaredDistance = 0.0f;
        int size = vector1.length;
        for (int i = 0; i < size; i++) {
            float difference = vector1[i] - vector2[i];
            squaredDistance += difference * difference;
        }
        return squaredDistance / size;
    }

    static float cosineDistance(float[] vector1, float[] vector2) {
        float sum = 0.0f;
        int len = vector1.length;
        for (int i = 0; i < len; i++) {
            sum += vector1[i] * vector2[i];
        }
        return (float) Math.acos(sum);
    }

    static float minimumCosineDistance(float[] vector1, float[] vector2, int numOrientations) {
        int len = vector1.length;
        float a = 0.0f;
        float b = 0.0f;
        for (int i = 0; i < len; i += 2) {
            a += (vector1[i] * vector2[i]) + (vector1[i + 1] * vector2[i + 1]);
            b += (vector1[i] * vector2[i + 1]) - (vector1[i + 1] * vector2[i]);
        }
        if (a != 0.0f) {
            float tan = b / a;
            double angle = Math.atan(tan);
            if (numOrientations > 2 && Math.abs(angle) >= 3.141592653589793d / ((double) numOrientations)) {
                return (float) Math.acos(a);
            }
            double cosine = Math.cos(angle);
            double sine = cosine * ((double) tan);
            return (float) Math.acos((((double) a) * cosine) + (((double) b) * sine));
        }
        return 1.5707964f;
    }

    public static OrientedBoundingBox computeOrientedBoundingBox(ArrayList<GesturePoint> originalPoints) {
        int count = originalPoints.size();
        float[] points = new float[count * 2];
        for (int i = 0; i < count; i++) {
            GesturePoint point = originalPoints.get(i);
            int index = i * 2;
            points[index] = point.x;
            points[index + 1] = point.y;
        }
        float[] meanVector = computeCentroid(points);
        return computeOrientedBoundingBox(points, meanVector);
    }

    public static OrientedBoundingBox computeOrientedBoundingBox(float[] originalPoints) {
        int size = originalPoints.length;
        float[] points = new float[size];
        for (int i = 0; i < size; i++) {
            points[i] = originalPoints[i];
        }
        float[] meanVector = computeCentroid(points);
        return computeOrientedBoundingBox(points, meanVector);
    }

    private static OrientedBoundingBox computeOrientedBoundingBox(float[] points, float[] centroid) {
        float angle;
        translate(points, -centroid[0], -centroid[1]);
        float[][] array = computeCoVariance(points);
        float[] targetVector = computeOrientation(array);
        if (targetVector[0] == 0.0f && targetVector[1] == 0.0f) {
            angle = -1.5707964f;
        } else {
            angle = (float) Math.atan2(targetVector[1], targetVector[0]);
            rotate(points, -angle);
        }
        float minx = Float.MAX_VALUE;
        float miny = Float.MAX_VALUE;
        float maxx = Float.MIN_VALUE;
        float maxy = Float.MIN_VALUE;
        int count = points.length;
        int i = 0;
        while (i < count) {
            if (points[i] < minx) {
                minx = points[i];
            }
            if (points[i] > maxx) {
                maxx = points[i];
            }
            int i2 = i + 1;
            if (points[i2] < miny) {
                miny = points[i2];
            }
            if (points[i2] > maxy) {
                maxy = points[i2];
            }
            i = i2 + 1;
        }
        return new OrientedBoundingBox((float) (((double) (180.0f * angle)) / 3.141592653589793d), centroid[0], centroid[1], maxx - minx, maxy - miny);
    }

    private static float[] computeOrientation(float[][] covarianceMatrix) {
        float[] targetVector = new float[2];
        if (covarianceMatrix[0][1] == 0.0f || covarianceMatrix[1][0] == 0.0f) {
            targetVector[0] = 1.0f;
            targetVector[1] = 0.0f;
        }
        float a = (-covarianceMatrix[0][0]) - covarianceMatrix[1][1];
        float b = (covarianceMatrix[0][0] * covarianceMatrix[1][1]) - (covarianceMatrix[0][1] * covarianceMatrix[1][0]);
        float value = a / 2.0f;
        float rightside = (float) Math.sqrt(Math.pow(value, 2.0d) - ((double) b));
        float lambda1 = (-value) + rightside;
        float lambda2 = (-value) - rightside;
        if (lambda1 == lambda2) {
            targetVector[0] = 0.0f;
            targetVector[1] = 0.0f;
        } else {
            float lambda = lambda1 > lambda2 ? lambda1 : lambda2;
            targetVector[0] = 1.0f;
            targetVector[1] = (lambda - covarianceMatrix[0][0]) / covarianceMatrix[0][1];
        }
        return targetVector;
    }

    static float[] rotate(float[] points, float angle) {
        float cos = (float) Math.cos(angle);
        float sin = (float) Math.sin(angle);
        int size = points.length;
        for (int i = 0; i < size; i += 2) {
            float x = (points[i] * cos) - (points[i + 1] * sin);
            float y = (points[i] * sin) + (points[i + 1] * cos);
            points[i] = x;
            points[i + 1] = y;
        }
        return points;
    }

    static float[] translate(float[] points, float dx, float dy) {
        int size = points.length;
        for (int i = 0; i < size; i += 2) {
            points[i] = points[i] + dx;
            int i2 = i + 1;
            points[i2] = points[i2] + dy;
        }
        return points;
    }

    static float[] scale(float[] points, float sx, float sy) {
        int size = points.length;
        for (int i = 0; i < size; i += 2) {
            points[i] = points[i] * sx;
            int i2 = i + 1;
            points[i2] = points[i2] * sy;
        }
        return points;
    }
}
