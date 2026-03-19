package android.accessibilityservice;

import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.RectF;
import android.os.Parcel;
import android.os.Parcelable;
import android.view.MotionEvent;
import java.util.ArrayList;
import java.util.List;

public final class GestureDescription {
    private static final long MAX_GESTURE_DURATION_MS = 60000;
    private static final int MAX_STROKE_COUNT = 10;
    private final List<StrokeDescription> mStrokes;
    private final float[] mTempPos;

    GestureDescription(List strokes, GestureDescription gestureDescription) {
        this(strokes);
    }

    public static int getMaxStrokeCount() {
        return 10;
    }

    public static long getMaxGestureDuration() {
        return MAX_GESTURE_DURATION_MS;
    }

    private GestureDescription() {
        this.mStrokes = new ArrayList();
        this.mTempPos = new float[2];
    }

    private GestureDescription(List<StrokeDescription> strokes) {
        this.mStrokes = new ArrayList();
        this.mTempPos = new float[2];
        this.mStrokes.addAll(strokes);
    }

    public int getStrokeCount() {
        return this.mStrokes.size();
    }

    public StrokeDescription getStroke(int index) {
        return this.mStrokes.get(index);
    }

    private long getNextKeyPointAtLeast(long offset) {
        long nextKeyPoint = Long.MAX_VALUE;
        for (int i = 0; i < this.mStrokes.size(); i++) {
            long thisStartTime = this.mStrokes.get(i).mStartTime;
            if (thisStartTime < nextKeyPoint && thisStartTime >= offset) {
                nextKeyPoint = thisStartTime;
            }
            long thisEndTime = this.mStrokes.get(i).mEndTime;
            if (thisEndTime < nextKeyPoint && thisEndTime >= offset) {
                nextKeyPoint = thisEndTime;
            }
        }
        if (nextKeyPoint == Long.MAX_VALUE) {
            return -1L;
        }
        return nextKeyPoint;
    }

    private int getPointsForTime(long time, TouchPoint[] touchPoints) {
        int numPointsFound = 0;
        for (int i = 0; i < this.mStrokes.size(); i++) {
            StrokeDescription strokeDescription = this.mStrokes.get(i);
            if (strokeDescription.hasPointForTime(time)) {
                touchPoints[numPointsFound].mPathIndex = i;
                touchPoints[numPointsFound].mIsStartOfPath = time == strokeDescription.mStartTime;
                touchPoints[numPointsFound].mIsEndOfPath = time == strokeDescription.mEndTime;
                strokeDescription.getPosForTime(time, this.mTempPos);
                touchPoints[numPointsFound].mX = Math.round(this.mTempPos[0]);
                touchPoints[numPointsFound].mY = Math.round(this.mTempPos[1]);
                numPointsFound++;
            }
        }
        return numPointsFound;
    }

    private static long getTotalDuration(List<StrokeDescription> paths) {
        long latestEnd = Long.MIN_VALUE;
        for (int i = 0; i < paths.size(); i++) {
            StrokeDescription path = paths.get(i);
            latestEnd = Math.max(latestEnd, path.mEndTime);
        }
        return Math.max(latestEnd, 0L);
    }

    public static class Builder {
        private final List<StrokeDescription> mStrokes = new ArrayList();

        public Builder addStroke(StrokeDescription strokeDescription) {
            if (this.mStrokes.size() >= 10) {
                throw new IllegalStateException("Attempting to add too many strokes to a gesture");
            }
            this.mStrokes.add(strokeDescription);
            if (GestureDescription.getTotalDuration(this.mStrokes) > GestureDescription.MAX_GESTURE_DURATION_MS) {
                this.mStrokes.remove(strokeDescription);
                throw new IllegalStateException("Gesture would exceed maximum duration with new stroke");
            }
            return this;
        }

        public GestureDescription build() {
            if (this.mStrokes.size() == 0) {
                throw new IllegalStateException("Gestures must have at least one stroke");
            }
            return new GestureDescription(this.mStrokes, null);
        }
    }

    public static class StrokeDescription {
        long mEndTime;
        Path mPath;
        private PathMeasure mPathMeasure;
        long mStartTime;
        float[] mTapLocation;
        private float mTimeToLengthConversion;

        public StrokeDescription(Path path, long startTime, long duration) {
            if (duration <= 0) {
                throw new IllegalArgumentException("Duration must be positive");
            }
            if (startTime < 0) {
                throw new IllegalArgumentException("Start time must not be negative");
            }
            RectF bounds = new RectF();
            path.computeBounds(bounds, false);
            if (bounds.bottom < 0.0f || bounds.top < 0.0f || bounds.right < 0.0f || bounds.left < 0.0f) {
                throw new IllegalArgumentException("Path bounds must not be negative");
            }
            if (path.isEmpty()) {
                throw new IllegalArgumentException("Path is empty");
            }
            this.mPath = new Path(path);
            this.mPathMeasure = new PathMeasure(path, false);
            if (this.mPathMeasure.getLength() == 0.0f) {
                Path tempPath = new Path(path);
                tempPath.lineTo(-1.0f, -1.0f);
                this.mTapLocation = new float[2];
                PathMeasure pathMeasure = new PathMeasure(tempPath, false);
                pathMeasure.getPosTan(0.0f, this.mTapLocation, null);
            }
            if (this.mPathMeasure.nextContour()) {
                throw new IllegalArgumentException("Path has more than one contour");
            }
            this.mPathMeasure.setPath(this.mPath, false);
            this.mStartTime = startTime;
            this.mEndTime = startTime + duration;
            this.mTimeToLengthConversion = getLength() / duration;
        }

        public Path getPath() {
            return new Path(this.mPath);
        }

        public long getStartTime() {
            return this.mStartTime;
        }

        public long getDuration() {
            return this.mEndTime - this.mStartTime;
        }

        float getLength() {
            return this.mPathMeasure.getLength();
        }

        boolean getPosForTime(long time, float[] pos) {
            if (this.mTapLocation != null) {
                pos[0] = this.mTapLocation[0];
                pos[1] = this.mTapLocation[1];
                return true;
            }
            if (time == this.mEndTime) {
                return this.mPathMeasure.getPosTan(getLength(), pos, null);
            }
            float length = this.mTimeToLengthConversion * (time - this.mStartTime);
            return this.mPathMeasure.getPosTan(length, pos, null);
        }

        boolean hasPointForTime(long time) {
            return time >= this.mStartTime && time <= this.mEndTime;
        }
    }

    public static class TouchPoint implements Parcelable {
        public static final Parcelable.Creator<TouchPoint> CREATOR = new Parcelable.Creator<TouchPoint>() {
            @Override
            public TouchPoint createFromParcel(Parcel in) {
                return new TouchPoint(in);
            }

            @Override
            public TouchPoint[] newArray(int size) {
                return new TouchPoint[size];
            }
        };
        private static final int FLAG_IS_END_OF_PATH = 2;
        private static final int FLAG_IS_START_OF_PATH = 1;
        boolean mIsEndOfPath;
        boolean mIsStartOfPath;
        int mPathIndex;
        float mX;
        float mY;

        public TouchPoint() {
        }

        public TouchPoint(TouchPoint pointToCopy) {
            copyFrom(pointToCopy);
        }

        public TouchPoint(Parcel parcel) {
            this.mPathIndex = parcel.readInt();
            int startEnd = parcel.readInt();
            this.mIsStartOfPath = (startEnd & 1) != 0;
            this.mIsEndOfPath = (startEnd & 2) != 0;
            this.mX = parcel.readFloat();
            this.mY = parcel.readFloat();
        }

        void copyFrom(TouchPoint other) {
            this.mPathIndex = other.mPathIndex;
            this.mIsStartOfPath = other.mIsStartOfPath;
            this.mIsEndOfPath = other.mIsEndOfPath;
            this.mX = other.mX;
            this.mY = other.mY;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(this.mPathIndex);
            int startEnd = this.mIsStartOfPath ? 1 : 0;
            dest.writeInt(startEnd | (this.mIsEndOfPath ? 2 : 0));
            dest.writeFloat(this.mX);
            dest.writeFloat(this.mY);
        }
    }

    public static class GestureStep implements Parcelable {
        public static final Parcelable.Creator<GestureStep> CREATOR = new Parcelable.Creator<GestureStep>() {
            @Override
            public GestureStep createFromParcel(Parcel in) {
                return new GestureStep(in);
            }

            @Override
            public GestureStep[] newArray(int size) {
                return new GestureStep[size];
            }
        };
        public int numTouchPoints;
        public long timeSinceGestureStart;
        public TouchPoint[] touchPoints;

        public GestureStep(long timeSinceGestureStart, int numTouchPoints, TouchPoint[] touchPointsToCopy) {
            this.timeSinceGestureStart = timeSinceGestureStart;
            this.numTouchPoints = numTouchPoints;
            this.touchPoints = new TouchPoint[numTouchPoints];
            for (int i = 0; i < numTouchPoints; i++) {
                this.touchPoints[i] = new TouchPoint(touchPointsToCopy[i]);
            }
        }

        public GestureStep(Parcel parcel) {
            this.timeSinceGestureStart = parcel.readLong();
            Parcelable[] parcelables = parcel.readParcelableArray(TouchPoint.class.getClassLoader());
            this.numTouchPoints = parcelables == null ? 0 : parcelables.length;
            this.touchPoints = new TouchPoint[this.numTouchPoints];
            for (int i = 0; i < this.numTouchPoints; i++) {
                this.touchPoints[i] = (TouchPoint) parcelables[i];
            }
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeLong(this.timeSinceGestureStart);
            dest.writeParcelableArray(this.touchPoints, flags);
        }
    }

    public static class MotionEventGenerator {
        private static final int EVENT_BUTTON_STATE = 0;
        private static final int EVENT_DEVICE_ID = 0;
        private static final int EVENT_EDGE_FLAGS = 0;
        private static final int EVENT_FLAGS = 0;
        private static final int EVENT_META_STATE = 0;
        private static final int EVENT_SOURCE = 4098;
        private static final float EVENT_X_PRECISION = 1.0f;
        private static final float EVENT_Y_PRECISION = 1.0f;
        private static TouchPoint[] sCurrentTouchPoints;
        private static TouchPoint[] sLastTouchPoints;
        private static MotionEvent.PointerCoords[] sPointerCoords;
        private static MotionEvent.PointerProperties[] sPointerProps;

        static List<GestureStep> getGestureStepsFromGestureDescription(GestureDescription description, int sampleTimeMs) {
            List<GestureStep> gestureSteps = new ArrayList<>();
            TouchPoint[] currentTouchPoints = getCurrentTouchPoints(description.getStrokeCount());
            int currentTouchPointSize = 0;
            long timeSinceGestureStart = 0;
            long nextKeyPointTime = description.getNextKeyPointAtLeast(0L);
            while (nextKeyPointTime >= 0) {
                timeSinceGestureStart = currentTouchPointSize == 0 ? nextKeyPointTime : Math.min(nextKeyPointTime, ((long) sampleTimeMs) + timeSinceGestureStart);
                currentTouchPointSize = description.getPointsForTime(timeSinceGestureStart, currentTouchPoints);
                gestureSteps.add(new GestureStep(timeSinceGestureStart, currentTouchPointSize, currentTouchPoints));
                nextKeyPointTime = description.getNextKeyPointAtLeast(1 + timeSinceGestureStart);
            }
            return gestureSteps;
        }

        public static List<MotionEvent> getMotionEventsFromGestureSteps(List<GestureStep> steps) {
            List<MotionEvent> motionEvents = new ArrayList<>();
            int lastTouchPointSize = 0;
            for (int i = 0; i < steps.size(); i++) {
                GestureStep step = steps.get(i);
                int currentTouchPointSize = step.numTouchPoints;
                TouchPoint[] lastTouchPoints = getLastTouchPoints(Math.max(lastTouchPointSize, currentTouchPointSize));
                appendMoveEventIfNeeded(motionEvents, lastTouchPoints, lastTouchPointSize, step.touchPoints, currentTouchPointSize, step.timeSinceGestureStart);
                lastTouchPointSize = appendDownEvents(motionEvents, lastTouchPoints, appendUpEvents(motionEvents, lastTouchPoints, lastTouchPointSize, step.touchPoints, currentTouchPointSize, step.timeSinceGestureStart), step.touchPoints, currentTouchPointSize, step.timeSinceGestureStart);
            }
            return motionEvents;
        }

        private static TouchPoint[] getCurrentTouchPoints(int requiredCapacity) {
            if (sCurrentTouchPoints == null || sCurrentTouchPoints.length < requiredCapacity) {
                sCurrentTouchPoints = new TouchPoint[requiredCapacity];
                for (int i = 0; i < requiredCapacity; i++) {
                    sCurrentTouchPoints[i] = new TouchPoint();
                }
            }
            return sCurrentTouchPoints;
        }

        private static TouchPoint[] getLastTouchPoints(int requiredCapacity) {
            if (sLastTouchPoints == null || sLastTouchPoints.length < requiredCapacity) {
                sLastTouchPoints = new TouchPoint[requiredCapacity];
                for (int i = 0; i < requiredCapacity; i++) {
                    sLastTouchPoints[i] = new TouchPoint();
                }
            }
            return sLastTouchPoints;
        }

        private static MotionEvent.PointerCoords[] getPointerCoords(int requiredCapacity) {
            if (sPointerCoords == null || sPointerCoords.length < requiredCapacity) {
                sPointerCoords = new MotionEvent.PointerCoords[requiredCapacity];
                for (int i = 0; i < requiredCapacity; i++) {
                    sPointerCoords[i] = new MotionEvent.PointerCoords();
                }
            }
            return sPointerCoords;
        }

        private static MotionEvent.PointerProperties[] getPointerProps(int requiredCapacity) {
            if (sPointerProps == null || sPointerProps.length < requiredCapacity) {
                sPointerProps = new MotionEvent.PointerProperties[requiredCapacity];
                for (int i = 0; i < requiredCapacity; i++) {
                    sPointerProps[i] = new MotionEvent.PointerProperties();
                }
            }
            return sPointerProps;
        }

        private static void appendMoveEventIfNeeded(List<MotionEvent> motionEvents, TouchPoint[] lastTouchPoints, int lastTouchPointsSize, TouchPoint[] currentTouchPoints, int currentTouchPointsSize, long currentTime) {
            boolean moveFound = false;
            for (int i = 0; i < currentTouchPointsSize; i++) {
                int lastPointsIndex = findPointByPathIndex(lastTouchPoints, lastTouchPointsSize, currentTouchPoints[i].mPathIndex);
                if (lastPointsIndex >= 0) {
                    boolean z = (lastTouchPoints[lastPointsIndex].mX == currentTouchPoints[i].mX && lastTouchPoints[lastPointsIndex].mY == currentTouchPoints[i].mY) ? false : true;
                    moveFound |= z;
                    lastTouchPoints[lastPointsIndex].copyFrom(currentTouchPoints[i]);
                }
            }
            if (!moveFound) {
                return;
            }
            long downTime = motionEvents.get(motionEvents.size() - 1).getDownTime();
            motionEvents.add(obtainMotionEvent(downTime, currentTime, 2, lastTouchPoints, lastTouchPointsSize));
        }

        private static int appendUpEvents(List<MotionEvent> motionEvents, TouchPoint[] lastTouchPoints, int lastTouchPointsSize, TouchPoint[] currentTouchPoints, int currentTouchPointsSize, long currentTime) {
            int indexOfUpEvent;
            for (int i = 0; i < currentTouchPointsSize; i++) {
                if (currentTouchPoints[i].mIsEndOfPath && (indexOfUpEvent = findPointByPathIndex(lastTouchPoints, lastTouchPointsSize, currentTouchPoints[i].mPathIndex)) >= 0) {
                    long downTime = motionEvents.get(motionEvents.size() - 1).getDownTime();
                    int action = lastTouchPointsSize == 1 ? 1 : 6;
                    motionEvents.add(obtainMotionEvent(downTime, currentTime, action | (indexOfUpEvent << 8), lastTouchPoints, lastTouchPointsSize));
                    for (int j = indexOfUpEvent; j < lastTouchPointsSize - 1; j++) {
                        lastTouchPoints[j].copyFrom(lastTouchPoints[j + 1]);
                    }
                    lastTouchPointsSize--;
                }
            }
            return lastTouchPointsSize;
        }

        private static int appendDownEvents(List<MotionEvent> motionEvents, TouchPoint[] lastTouchPoints, int lastTouchPointsSize, TouchPoint[] currentTouchPoints, int currentTouchPointsSize, long currentTime) {
            int lastTouchPointsSize2;
            int i = 0;
            int lastTouchPointsSize3 = lastTouchPointsSize;
            while (i < currentTouchPointsSize) {
                if (currentTouchPoints[i].mIsStartOfPath) {
                    lastTouchPointsSize2 = lastTouchPointsSize3 + 1;
                    lastTouchPoints[lastTouchPointsSize3].copyFrom(currentTouchPoints[i]);
                    int action = lastTouchPointsSize2 == 1 ? 0 : 5;
                    long downTime = action == 0 ? currentTime : motionEvents.get(motionEvents.size() - 1).getDownTime();
                    motionEvents.add(obtainMotionEvent(downTime, currentTime, action | (i << 8), lastTouchPoints, lastTouchPointsSize2));
                } else {
                    lastTouchPointsSize2 = lastTouchPointsSize3;
                }
                i++;
                lastTouchPointsSize3 = lastTouchPointsSize2;
            }
            return lastTouchPointsSize3;
        }

        private static MotionEvent obtainMotionEvent(long downTime, long eventTime, int action, TouchPoint[] touchPoints, int touchPointsSize) {
            MotionEvent.PointerCoords[] pointerCoords = getPointerCoords(touchPointsSize);
            MotionEvent.PointerProperties[] pointerProperties = getPointerProps(touchPointsSize);
            for (int i = 0; i < touchPointsSize; i++) {
                pointerProperties[i].id = touchPoints[i].mPathIndex;
                pointerProperties[i].toolType = 0;
                pointerCoords[i].clear();
                pointerCoords[i].pressure = 1.0f;
                pointerCoords[i].size = 1.0f;
                pointerCoords[i].x = touchPoints[i].mX;
                pointerCoords[i].y = touchPoints[i].mY;
            }
            return MotionEvent.obtain(downTime, eventTime, action, touchPointsSize, pointerProperties, pointerCoords, 0, 0, 1.0f, 1.0f, 0, 0, 4098, 0);
        }

        private static int findPointByPathIndex(TouchPoint[] touchPoints, int touchPointsSize, int pathIndex) {
            for (int i = 0; i < touchPointsSize; i++) {
                if (touchPoints[i].mPathIndex == pathIndex) {
                    return i;
                }
            }
            return -1;
        }
    }
}
