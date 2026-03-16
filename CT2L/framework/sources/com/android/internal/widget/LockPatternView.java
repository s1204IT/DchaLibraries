package com.android.internal.widget;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import com.android.internal.R;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

public class LockPatternView extends View {
    private static final int ASPECT_LOCK_HEIGHT = 2;
    private static final int ASPECT_LOCK_WIDTH = 1;
    private static final int ASPECT_SQUARE = 0;
    private static final float DRAG_THRESHHOLD = 0.0f;
    private static final int MILLIS_PER_CIRCLE_ANIMATING = 700;
    private static final boolean PROFILE_DRAWING = false;
    private long mAnimatingPeriodStart;
    private int mAspect;
    private final CellState[][] mCellStates;
    private final Path mCurrentPath;
    private final int mDotSize;
    private final int mDotSizeActivated;
    private boolean mDrawingProfilingStarted;
    private boolean mEnableHapticFeedback;
    private int mErrorColor;
    private Interpolator mFastOutSlowInInterpolator;
    private float mHitFactor;
    private float mInProgressX;
    private float mInProgressY;
    private boolean mInStealthMode;
    private boolean mInputEnabled;
    private final Rect mInvalidate;
    private Interpolator mLinearOutSlowInInterpolator;
    private OnPatternListener mOnPatternListener;
    private Paint mPaint;
    private Paint mPathPaint;
    private final int mPathWidth;
    private ArrayList<Cell> mPattern;
    private DisplayMode mPatternDisplayMode;
    private boolean[][] mPatternDrawLookup;
    private boolean mPatternInProgress;
    private int mRegularColor;
    private float mSquareHeight;
    private float mSquareWidth;
    private int mSuccessColor;
    private final Rect mTmpInvalidateRect;

    public static class CellState {
        public ValueAnimator lineAnimator;
        public float size;
        public float scale = 1.0f;
        public float translateY = 0.0f;
        public float alpha = 1.0f;
        public float lineEndX = Float.MIN_VALUE;
        public float lineEndY = Float.MIN_VALUE;
    }

    public enum DisplayMode {
        Correct,
        Animate,
        Wrong
    }

    public interface OnPatternListener {
        void onPatternCellAdded(List<Cell> list);

        void onPatternCleared();

        void onPatternDetected(List<Cell> list);

        void onPatternStart();
    }

    public static class Cell {
        static Cell[][] sCells = (Cell[][]) Array.newInstance((Class<?>) Cell.class, 3, 3);
        int column;
        int row;

        static {
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    sCells[i][j] = new Cell(i, j);
                }
            }
        }

        private Cell(int row, int column) {
            checkRange(row, column);
            this.row = row;
            this.column = column;
        }

        public int getRow() {
            return this.row;
        }

        public int getColumn() {
            return this.column;
        }

        public static synchronized Cell of(int row, int column) {
            checkRange(row, column);
            return sCells[row][column];
        }

        private static void checkRange(int row, int column) {
            if (row < 0 || row > 2) {
                throw new IllegalArgumentException("row must be in range 0-2");
            }
            if (column < 0 || column > 2) {
                throw new IllegalArgumentException("column must be in range 0-2");
            }
        }

        public String toString() {
            return "(row=" + this.row + ",clmn=" + this.column + ")";
        }
    }

    public LockPatternView(Context context) {
        this(context, null);
    }

    public LockPatternView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mDrawingProfilingStarted = false;
        this.mPaint = new Paint();
        this.mPathPaint = new Paint();
        this.mPattern = new ArrayList<>(9);
        this.mPatternDrawLookup = (boolean[][]) Array.newInstance((Class<?>) Boolean.TYPE, 3, 3);
        this.mInProgressX = -1.0f;
        this.mInProgressY = -1.0f;
        this.mPatternDisplayMode = DisplayMode.Correct;
        this.mInputEnabled = true;
        this.mInStealthMode = false;
        this.mEnableHapticFeedback = true;
        this.mPatternInProgress = false;
        this.mHitFactor = 0.6f;
        this.mCurrentPath = new Path();
        this.mInvalidate = new Rect();
        this.mTmpInvalidateRect = new Rect();
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.LockPatternView);
        String aspect = a.getString(0);
        if ("square".equals(aspect)) {
            this.mAspect = 0;
        } else if ("lock_width".equals(aspect)) {
            this.mAspect = 1;
        } else if ("lock_height".equals(aspect)) {
            this.mAspect = 2;
        } else {
            this.mAspect = 0;
        }
        setClickable(true);
        this.mPathPaint.setAntiAlias(true);
        this.mPathPaint.setDither(true);
        this.mRegularColor = getResources().getColor(R.color.lock_pattern_view_regular_color);
        this.mErrorColor = getResources().getColor(R.color.lock_pattern_view_error_color);
        this.mSuccessColor = getResources().getColor(R.color.lock_pattern_view_success_color);
        this.mRegularColor = a.getColor(2, this.mRegularColor);
        this.mErrorColor = a.getColor(3, this.mErrorColor);
        this.mSuccessColor = a.getColor(4, this.mSuccessColor);
        int pathColor = a.getColor(1, this.mRegularColor);
        this.mPathPaint.setColor(pathColor);
        this.mPathPaint.setStyle(Paint.Style.STROKE);
        this.mPathPaint.setStrokeJoin(Paint.Join.ROUND);
        this.mPathPaint.setStrokeCap(Paint.Cap.ROUND);
        this.mPathWidth = getResources().getDimensionPixelSize(R.dimen.lock_pattern_dot_line_width);
        this.mPathPaint.setStrokeWidth(this.mPathWidth);
        this.mDotSize = getResources().getDimensionPixelSize(R.dimen.lock_pattern_dot_size);
        this.mDotSizeActivated = getResources().getDimensionPixelSize(R.dimen.lock_pattern_dot_size_activated);
        this.mPaint.setAntiAlias(true);
        this.mPaint.setDither(true);
        this.mCellStates = (CellState[][]) Array.newInstance((Class<?>) CellState.class, 3, 3);
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                this.mCellStates[i][j] = new CellState();
                this.mCellStates[i][j].size = this.mDotSize;
            }
        }
        this.mFastOutSlowInInterpolator = AnimationUtils.loadInterpolator(context, 17563661);
        this.mLinearOutSlowInInterpolator = AnimationUtils.loadInterpolator(context, 17563662);
    }

    public CellState[][] getCellStates() {
        return this.mCellStates;
    }

    public boolean isInStealthMode() {
        return this.mInStealthMode;
    }

    public boolean isTactileFeedbackEnabled() {
        return this.mEnableHapticFeedback;
    }

    public void setInStealthMode(boolean inStealthMode) {
        this.mInStealthMode = inStealthMode;
    }

    public void setTactileFeedbackEnabled(boolean tactileFeedbackEnabled) {
        this.mEnableHapticFeedback = tactileFeedbackEnabled;
    }

    public void setOnPatternListener(OnPatternListener onPatternListener) {
        this.mOnPatternListener = onPatternListener;
    }

    public void setPattern(DisplayMode displayMode, List<Cell> pattern) {
        this.mPattern.clear();
        this.mPattern.addAll(pattern);
        clearPatternDrawLookup();
        for (Cell cell : pattern) {
            this.mPatternDrawLookup[cell.getRow()][cell.getColumn()] = true;
        }
        setDisplayMode(displayMode);
    }

    public void setDisplayMode(DisplayMode displayMode) {
        this.mPatternDisplayMode = displayMode;
        if (displayMode == DisplayMode.Animate) {
            if (this.mPattern.size() == 0) {
                throw new IllegalStateException("you must have a pattern to animate if you want to set the display mode to animate");
            }
            this.mAnimatingPeriodStart = SystemClock.elapsedRealtime();
            Cell first = this.mPattern.get(0);
            this.mInProgressX = getCenterXForColumn(first.getColumn());
            this.mInProgressY = getCenterYForRow(first.getRow());
            clearPatternDrawLookup();
        }
        invalidate();
    }

    private void notifyCellAdded() {
        sendAccessEvent(R.string.lockscreen_access_pattern_cell_added);
        if (this.mOnPatternListener != null) {
            this.mOnPatternListener.onPatternCellAdded(this.mPattern);
        }
    }

    private void notifyPatternStarted() {
        sendAccessEvent(R.string.lockscreen_access_pattern_start);
        if (this.mOnPatternListener != null) {
            this.mOnPatternListener.onPatternStart();
        }
    }

    private void notifyPatternDetected() {
        sendAccessEvent(R.string.lockscreen_access_pattern_detected);
        if (this.mOnPatternListener != null) {
            this.mOnPatternListener.onPatternDetected(this.mPattern);
        }
    }

    private void notifyPatternCleared() {
        sendAccessEvent(R.string.lockscreen_access_pattern_cleared);
        if (this.mOnPatternListener != null) {
            this.mOnPatternListener.onPatternCleared();
        }
    }

    public void clearPattern() {
        resetPattern();
    }

    private void resetPattern() {
        this.mPattern.clear();
        clearPatternDrawLookup();
        this.mPatternDisplayMode = DisplayMode.Correct;
        invalidate();
    }

    private void clearPatternDrawLookup() {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                this.mPatternDrawLookup[i][j] = false;
            }
        }
    }

    public void disableInput() {
        this.mInputEnabled = false;
    }

    public void enableInput() {
        this.mInputEnabled = true;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        int width = (w - this.mPaddingLeft) - this.mPaddingRight;
        this.mSquareWidth = width / 3.0f;
        int height = (h - this.mPaddingTop) - this.mPaddingBottom;
        this.mSquareHeight = height / 3.0f;
    }

    private int resolveMeasured(int measureSpec, int desired) {
        int specSize = View.MeasureSpec.getSize(measureSpec);
        switch (View.MeasureSpec.getMode(measureSpec)) {
            case Integer.MIN_VALUE:
                int result = Math.max(specSize, desired);
                return result;
            case 0:
                return desired;
            default:
                return specSize;
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int minimumWidth = getSuggestedMinimumWidth();
        int minimumHeight = getSuggestedMinimumHeight();
        int viewWidth = resolveMeasured(widthMeasureSpec, minimumWidth);
        int viewHeight = resolveMeasured(heightMeasureSpec, minimumHeight);
        switch (this.mAspect) {
            case 0:
                viewHeight = Math.min(viewWidth, viewHeight);
                viewWidth = viewHeight;
                break;
            case 1:
                viewHeight = Math.min(viewWidth, viewHeight);
                break;
            case 2:
                viewWidth = Math.min(viewWidth, viewHeight);
                break;
        }
        setMeasuredDimension(viewWidth, viewHeight);
    }

    private Cell detectAndAddHit(float x, float y) {
        Cell cell = checkForNewHit(x, y);
        if (cell != null) {
            Cell fillInGapCell = null;
            ArrayList<Cell> pattern = this.mPattern;
            if (!pattern.isEmpty()) {
                Cell lastCell = pattern.get(pattern.size() - 1);
                int dRow = cell.row - lastCell.row;
                int dColumn = cell.column - lastCell.column;
                int fillInRow = lastCell.row;
                int fillInColumn = lastCell.column;
                if (Math.abs(dRow) == 2 && Math.abs(dColumn) != 1) {
                    fillInRow = lastCell.row + (dRow > 0 ? 1 : -1);
                }
                if (Math.abs(dColumn) == 2 && Math.abs(dRow) != 1) {
                    fillInColumn = lastCell.column + (dColumn > 0 ? 1 : -1);
                }
                fillInGapCell = Cell.of(fillInRow, fillInColumn);
            }
            if (fillInGapCell != null && !this.mPatternDrawLookup[fillInGapCell.row][fillInGapCell.column]) {
                addCellToPattern(fillInGapCell);
            }
            addCellToPattern(cell);
            if (this.mEnableHapticFeedback) {
                performHapticFeedback(1, 3);
                return cell;
            }
            return cell;
        }
        return null;
    }

    private void addCellToPattern(Cell newCell) {
        this.mPatternDrawLookup[newCell.getRow()][newCell.getColumn()] = true;
        this.mPattern.add(newCell);
        if (!this.mInStealthMode) {
            startCellActivatedAnimation(newCell);
        }
        notifyCellAdded();
    }

    private void startCellActivatedAnimation(Cell cell) {
        final CellState cellState = this.mCellStates[cell.row][cell.column];
        startSizeAnimation(this.mDotSize, this.mDotSizeActivated, 96L, this.mLinearOutSlowInInterpolator, cellState, new Runnable() {
            @Override
            public void run() {
                LockPatternView.this.startSizeAnimation(LockPatternView.this.mDotSizeActivated, LockPatternView.this.mDotSize, 192L, LockPatternView.this.mFastOutSlowInInterpolator, cellState, null);
            }
        });
        startLineEndAnimation(cellState, this.mInProgressX, this.mInProgressY, getCenterXForColumn(cell.column), getCenterYForRow(cell.row));
    }

    private void startLineEndAnimation(final CellState state, final float startX, final float startY, final float targetX, final float targetY) {
        ValueAnimator valueAnimator = ValueAnimator.ofFloat(0.0f, 1.0f);
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float t = ((Float) animation.getAnimatedValue()).floatValue();
                state.lineEndX = ((1.0f - t) * startX) + (targetX * t);
                state.lineEndY = ((1.0f - t) * startY) + (targetY * t);
                LockPatternView.this.invalidate();
            }
        });
        valueAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                state.lineAnimator = null;
            }
        });
        valueAnimator.setInterpolator(this.mFastOutSlowInInterpolator);
        valueAnimator.setDuration(100L);
        valueAnimator.start();
        state.lineAnimator = valueAnimator;
    }

    private void startSizeAnimation(float start, float end, long duration, Interpolator interpolator, final CellState state, final Runnable endRunnable) {
        ValueAnimator valueAnimator = ValueAnimator.ofFloat(start, end);
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                state.size = ((Float) animation.getAnimatedValue()).floatValue();
                LockPatternView.this.invalidate();
            }
        });
        if (endRunnable != null) {
            valueAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    endRunnable.run();
                }
            });
        }
        valueAnimator.setInterpolator(interpolator);
        valueAnimator.setDuration(duration);
        valueAnimator.start();
    }

    private Cell checkForNewHit(float x, float y) {
        int columnHit;
        int rowHit = getRowHit(y);
        if (rowHit >= 0 && (columnHit = getColumnHit(x)) >= 0 && !this.mPatternDrawLookup[rowHit][columnHit]) {
            return Cell.of(rowHit, columnHit);
        }
        return null;
    }

    private int getRowHit(float y) {
        float squareHeight = this.mSquareHeight;
        float hitSize = squareHeight * this.mHitFactor;
        float offset = this.mPaddingTop + ((squareHeight - hitSize) / 2.0f);
        for (int i = 0; i < 3; i++) {
            float hitTop = offset + (i * squareHeight);
            if (y >= hitTop && y <= hitTop + hitSize) {
                return i;
            }
        }
        return -1;
    }

    private int getColumnHit(float x) {
        float squareWidth = this.mSquareWidth;
        float hitSize = squareWidth * this.mHitFactor;
        float offset = this.mPaddingLeft + ((squareWidth - hitSize) / 2.0f);
        for (int i = 0; i < 3; i++) {
            float hitLeft = offset + (i * squareWidth);
            if (x >= hitLeft && x <= hitLeft + hitSize) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public boolean onHoverEvent(MotionEvent event) {
        if (AccessibilityManager.getInstance(this.mContext).isTouchExplorationEnabled()) {
            int action = event.getAction();
            switch (action) {
                case 7:
                    event.setAction(2);
                    break;
                case 9:
                    event.setAction(0);
                    break;
                case 10:
                    event.setAction(1);
                    break;
            }
            onTouchEvent(event);
            event.setAction(action);
        }
        return super.onHoverEvent(event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!this.mInputEnabled || !isEnabled()) {
            return false;
        }
        switch (event.getAction()) {
            case 0:
                handleActionDown(event);
                break;
            case 1:
                handleActionUp(event);
                break;
            case 2:
                handleActionMove(event);
                break;
            case 3:
                if (this.mPatternInProgress) {
                    this.mPatternInProgress = false;
                    resetPattern();
                    notifyPatternCleared();
                }
                break;
        }
        return false;
    }

    private void handleActionMove(MotionEvent event) {
        float radius = this.mPathWidth;
        int historySize = event.getHistorySize();
        this.mTmpInvalidateRect.setEmpty();
        boolean invalidateNow = false;
        int i = 0;
        while (i < historySize + 1) {
            float x = i < historySize ? event.getHistoricalX(i) : event.getX();
            float y = i < historySize ? event.getHistoricalY(i) : event.getY();
            Cell hitCell = detectAndAddHit(x, y);
            int patternSize = this.mPattern.size();
            if (hitCell != null && patternSize == 1) {
                this.mPatternInProgress = true;
                notifyPatternStarted();
            }
            float dx = Math.abs(x - this.mInProgressX);
            float dy = Math.abs(y - this.mInProgressY);
            if (dx > 0.0f || dy > 0.0f) {
                invalidateNow = true;
            }
            if (this.mPatternInProgress && patternSize > 0) {
                ArrayList<Cell> pattern = this.mPattern;
                Cell lastCell = pattern.get(patternSize - 1);
                float lastCellCenterX = getCenterXForColumn(lastCell.column);
                float lastCellCenterY = getCenterYForRow(lastCell.row);
                float left = Math.min(lastCellCenterX, x) - radius;
                float right = Math.max(lastCellCenterX, x) + radius;
                float top = Math.min(lastCellCenterY, y) - radius;
                float bottom = Math.max(lastCellCenterY, y) + radius;
                if (hitCell != null) {
                    float width = this.mSquareWidth * 0.5f;
                    float height = this.mSquareHeight * 0.5f;
                    float hitCellCenterX = getCenterXForColumn(hitCell.column);
                    float hitCellCenterY = getCenterYForRow(hitCell.row);
                    left = Math.min(hitCellCenterX - width, left);
                    right = Math.max(hitCellCenterX + width, right);
                    top = Math.min(hitCellCenterY - height, top);
                    bottom = Math.max(hitCellCenterY + height, bottom);
                }
                this.mTmpInvalidateRect.union(Math.round(left), Math.round(top), Math.round(right), Math.round(bottom));
            }
            i++;
        }
        this.mInProgressX = event.getX();
        this.mInProgressY = event.getY();
        if (invalidateNow) {
            this.mInvalidate.union(this.mTmpInvalidateRect);
            invalidate(this.mInvalidate);
            this.mInvalidate.set(this.mTmpInvalidateRect);
        }
    }

    private void sendAccessEvent(int resId) {
        announceForAccessibility(this.mContext.getString(resId));
    }

    private void handleActionUp(MotionEvent event) {
        if (!this.mPattern.isEmpty()) {
            this.mPatternInProgress = false;
            cancelLineAnimations();
            notifyPatternDetected();
            invalidate();
        }
    }

    private void cancelLineAnimations() {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                CellState state = this.mCellStates[i][j];
                if (state.lineAnimator != null) {
                    state.lineAnimator.cancel();
                    state.lineEndX = Float.MIN_VALUE;
                    state.lineEndY = Float.MIN_VALUE;
                }
            }
        }
    }

    private void handleActionDown(MotionEvent event) {
        resetPattern();
        float x = event.getX();
        float y = event.getY();
        Cell hitCell = detectAndAddHit(x, y);
        if (hitCell != null) {
            this.mPatternInProgress = true;
            this.mPatternDisplayMode = DisplayMode.Correct;
            notifyPatternStarted();
        } else if (this.mPatternInProgress) {
            this.mPatternInProgress = false;
            notifyPatternCleared();
        }
        if (hitCell != null) {
            float startX = getCenterXForColumn(hitCell.column);
            float startY = getCenterYForRow(hitCell.row);
            float widthOffset = this.mSquareWidth / 2.0f;
            float heightOffset = this.mSquareHeight / 2.0f;
            invalidate((int) (startX - widthOffset), (int) (startY - heightOffset), (int) (startX + widthOffset), (int) (startY + heightOffset));
        }
        this.mInProgressX = x;
        this.mInProgressY = y;
    }

    private float getCenterXForColumn(int column) {
        return this.mPaddingLeft + (column * this.mSquareWidth) + (this.mSquareWidth / 2.0f);
    }

    private float getCenterYForRow(int row) {
        return this.mPaddingTop + (row * this.mSquareHeight) + (this.mSquareHeight / 2.0f);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        ArrayList<Cell> pattern = this.mPattern;
        int count = pattern.size();
        boolean[][] drawLookup = this.mPatternDrawLookup;
        if (this.mPatternDisplayMode == DisplayMode.Animate) {
            int oneCycle = (count + 1) * 700;
            int spotInCycle = ((int) (SystemClock.elapsedRealtime() - this.mAnimatingPeriodStart)) % oneCycle;
            int numCircles = spotInCycle / 700;
            clearPatternDrawLookup();
            for (int i = 0; i < numCircles; i++) {
                Cell cell = pattern.get(i);
                drawLookup[cell.getRow()][cell.getColumn()] = true;
            }
            boolean needToUpdateInProgressPoint = numCircles > 0 && numCircles < count;
            if (needToUpdateInProgressPoint) {
                float percentageOfNextCircle = (spotInCycle % 700) / 700.0f;
                Cell currentCell = pattern.get(numCircles - 1);
                float centerX = getCenterXForColumn(currentCell.column);
                float centerY = getCenterYForRow(currentCell.row);
                Cell nextCell = pattern.get(numCircles);
                float dx = percentageOfNextCircle * (getCenterXForColumn(nextCell.column) - centerX);
                float dy = percentageOfNextCircle * (getCenterYForRow(nextCell.row) - centerY);
                this.mInProgressX = centerX + dx;
                this.mInProgressY = centerY + dy;
            }
            invalidate();
        }
        Path currentPath = this.mCurrentPath;
        currentPath.rewind();
        for (int i2 = 0; i2 < 3; i2++) {
            float centerY2 = getCenterYForRow(i2);
            for (int j = 0; j < 3; j++) {
                CellState cellState = this.mCellStates[i2][j];
                float centerX2 = getCenterXForColumn(j);
                float size = cellState.size * cellState.scale;
                float translationY = cellState.translateY;
                drawCircle(canvas, (int) centerX2, ((int) centerY2) + translationY, size, drawLookup[i2][j], cellState.alpha);
            }
        }
        boolean drawPath = !this.mInStealthMode;
        if (drawPath) {
            this.mPathPaint.setColor(getCurrentColor(true));
            boolean anyCircles = false;
            float lastX = 0.0f;
            float lastY = 0.0f;
            for (int i3 = 0; i3 < count; i3++) {
                Cell cell2 = pattern.get(i3);
                if (!drawLookup[cell2.row][cell2.column]) {
                    break;
                }
                anyCircles = true;
                float centerX3 = getCenterXForColumn(cell2.column);
                float centerY3 = getCenterYForRow(cell2.row);
                if (i3 != 0) {
                    CellState state = this.mCellStates[cell2.row][cell2.column];
                    currentPath.rewind();
                    currentPath.moveTo(lastX, lastY);
                    if (state.lineEndX != Float.MIN_VALUE && state.lineEndY != Float.MIN_VALUE) {
                        currentPath.lineTo(state.lineEndX, state.lineEndY);
                    } else {
                        currentPath.lineTo(centerX3, centerY3);
                    }
                    canvas.drawPath(currentPath, this.mPathPaint);
                }
                lastX = centerX3;
                lastY = centerY3;
            }
            if ((this.mPatternInProgress || this.mPatternDisplayMode == DisplayMode.Animate) && anyCircles) {
                currentPath.rewind();
                currentPath.moveTo(lastX, lastY);
                currentPath.lineTo(this.mInProgressX, this.mInProgressY);
                this.mPathPaint.setAlpha((int) (calculateLastSegmentAlpha(this.mInProgressX, this.mInProgressY, lastX, lastY) * 255.0f));
                canvas.drawPath(currentPath, this.mPathPaint);
            }
        }
    }

    private float calculateLastSegmentAlpha(float x, float y, float lastX, float lastY) {
        float diffX = x - lastX;
        float diffY = y - lastY;
        float dist = (float) Math.sqrt((diffX * diffX) + (diffY * diffY));
        float frac = dist / this.mSquareWidth;
        return Math.min(1.0f, Math.max(0.0f, (frac - 0.3f) * 4.0f));
    }

    private int getCurrentColor(boolean partOfPattern) {
        if (!partOfPattern || this.mInStealthMode || this.mPatternInProgress) {
            return this.mRegularColor;
        }
        if (this.mPatternDisplayMode == DisplayMode.Wrong) {
            return this.mErrorColor;
        }
        if (this.mPatternDisplayMode == DisplayMode.Correct || this.mPatternDisplayMode == DisplayMode.Animate) {
            return this.mSuccessColor;
        }
        throw new IllegalStateException("unknown display mode " + this.mPatternDisplayMode);
    }

    private void drawCircle(Canvas canvas, float centerX, float centerY, float size, boolean partOfPattern, float alpha) {
        this.mPaint.setColor(getCurrentColor(partOfPattern));
        this.mPaint.setAlpha((int) (255.0f * alpha));
        canvas.drawCircle(centerX, centerY, size / 2.0f, this.mPaint);
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        return new SavedState(superState, LockPatternUtils.patternToString(this.mPattern), this.mPatternDisplayMode.ordinal(), this.mInputEnabled, this.mInStealthMode, this.mEnableHapticFeedback);
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());
        setPattern(DisplayMode.Correct, LockPatternUtils.stringToPattern(ss.getSerializedPattern()));
        this.mPatternDisplayMode = DisplayMode.values()[ss.getDisplayMode()];
        this.mInputEnabled = ss.isInputEnabled();
        this.mInStealthMode = ss.isInStealthMode();
        this.mEnableHapticFeedback = ss.isTactileFeedbackEnabled();
    }

    private static class SavedState extends View.BaseSavedState {
        public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
        private final int mDisplayMode;
        private final boolean mInStealthMode;
        private final boolean mInputEnabled;
        private final String mSerializedPattern;
        private final boolean mTactileFeedbackEnabled;

        private SavedState(Parcelable superState, String serializedPattern, int displayMode, boolean inputEnabled, boolean inStealthMode, boolean tactileFeedbackEnabled) {
            super(superState);
            this.mSerializedPattern = serializedPattern;
            this.mDisplayMode = displayMode;
            this.mInputEnabled = inputEnabled;
            this.mInStealthMode = inStealthMode;
            this.mTactileFeedbackEnabled = tactileFeedbackEnabled;
        }

        private SavedState(Parcel in) {
            super(in);
            this.mSerializedPattern = in.readString();
            this.mDisplayMode = in.readInt();
            this.mInputEnabled = ((Boolean) in.readValue(null)).booleanValue();
            this.mInStealthMode = ((Boolean) in.readValue(null)).booleanValue();
            this.mTactileFeedbackEnabled = ((Boolean) in.readValue(null)).booleanValue();
        }

        public String getSerializedPattern() {
            return this.mSerializedPattern;
        }

        public int getDisplayMode() {
            return this.mDisplayMode;
        }

        public boolean isInputEnabled() {
            return this.mInputEnabled;
        }

        public boolean isInStealthMode() {
            return this.mInStealthMode;
        }

        public boolean isTactileFeedbackEnabled() {
            return this.mTactileFeedbackEnabled;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeString(this.mSerializedPattern);
            dest.writeInt(this.mDisplayMode);
            dest.writeValue(Boolean.valueOf(this.mInputEnabled));
            dest.writeValue(Boolean.valueOf(this.mInStealthMode));
            dest.writeValue(Boolean.valueOf(this.mTactileFeedbackEnabled));
        }
    }
}
