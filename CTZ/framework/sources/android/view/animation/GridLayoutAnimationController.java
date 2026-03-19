package android.view.animation;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.LayoutAnimationController;
import com.android.internal.R;
import java.util.Random;

public class GridLayoutAnimationController extends LayoutAnimationController {
    public static final int DIRECTION_BOTTOM_TO_TOP = 2;
    public static final int DIRECTION_HORIZONTAL_MASK = 1;
    public static final int DIRECTION_LEFT_TO_RIGHT = 0;
    public static final int DIRECTION_RIGHT_TO_LEFT = 1;
    public static final int DIRECTION_TOP_TO_BOTTOM = 0;
    public static final int DIRECTION_VERTICAL_MASK = 2;
    public static final int PRIORITY_COLUMN = 1;
    public static final int PRIORITY_NONE = 0;
    public static final int PRIORITY_ROW = 2;
    private float mColumnDelay;
    private int mDirection;
    private int mDirectionPriority;
    private float mRowDelay;

    public static class AnimationParameters extends LayoutAnimationController.AnimationParameters {
        public int column;
        public int columnsCount;
        public int row;
        public int rowsCount;
    }

    public GridLayoutAnimationController(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R.styleable.GridLayoutAnimation);
        this.mColumnDelay = Animation.Description.parseValue(typedArrayObtainStyledAttributes.peekValue(0)).value;
        this.mRowDelay = Animation.Description.parseValue(typedArrayObtainStyledAttributes.peekValue(1)).value;
        this.mDirection = typedArrayObtainStyledAttributes.getInt(2, 0);
        this.mDirectionPriority = typedArrayObtainStyledAttributes.getInt(3, 0);
        typedArrayObtainStyledAttributes.recycle();
    }

    public GridLayoutAnimationController(Animation animation) {
        this(animation, 0.5f, 0.5f);
    }

    public GridLayoutAnimationController(Animation animation, float f, float f2) {
        super(animation);
        this.mColumnDelay = f;
        this.mRowDelay = f2;
    }

    public float getColumnDelay() {
        return this.mColumnDelay;
    }

    public void setColumnDelay(float f) {
        this.mColumnDelay = f;
    }

    public float getRowDelay() {
        return this.mRowDelay;
    }

    public void setRowDelay(float f) {
        this.mRowDelay = f;
    }

    public int getDirection() {
        return this.mDirection;
    }

    public void setDirection(int i) {
        this.mDirection = i;
    }

    public int getDirectionPriority() {
        return this.mDirectionPriority;
    }

    public void setDirectionPriority(int i) {
        this.mDirectionPriority = i;
    }

    @Override
    public boolean willOverlap() {
        return this.mColumnDelay < 1.0f || this.mRowDelay < 1.0f;
    }

    @Override
    protected long getDelayForView(View view) {
        long j;
        float f;
        AnimationParameters animationParameters = (AnimationParameters) view.getLayoutParams().layoutAnimationParameters;
        if (animationParameters == null) {
            return 0L;
        }
        int transformedColumnIndex = getTransformedColumnIndex(animationParameters);
        int transformedRowIndex = getTransformedRowIndex(animationParameters);
        int i = animationParameters.rowsCount;
        int i2 = animationParameters.columnsCount;
        float duration = this.mAnimation.getDuration();
        float f2 = this.mColumnDelay * duration;
        float f3 = this.mRowDelay * duration;
        if (this.mInterpolator == null) {
            this.mInterpolator = new LinearInterpolator();
        }
        switch (this.mDirectionPriority) {
            case 1:
                j = (long) ((transformedRowIndex * f3) + (transformedColumnIndex * i * f3));
                f = (i * f3) + (i2 * i * f3);
                break;
            case 2:
                j = (long) ((transformedColumnIndex * f2) + (transformedRowIndex * i2 * f2));
                f = (i2 * f2) + (i * i2 * f2);
                break;
            default:
                j = (long) ((transformedColumnIndex * f2) + (transformedRowIndex * f3));
                f = (i2 * f2) + (i * f3);
                break;
        }
        return (long) (this.mInterpolator.getInterpolation(j / f) * f);
    }

    private int getTransformedColumnIndex(AnimationParameters animationParameters) {
        int iNextFloat;
        switch (getOrder()) {
            case 1:
                iNextFloat = (animationParameters.columnsCount - 1) - animationParameters.column;
                break;
            case 2:
                if (this.mRandomizer == null) {
                    this.mRandomizer = new Random();
                }
                iNextFloat = (int) (animationParameters.columnsCount * this.mRandomizer.nextFloat());
                break;
            default:
                iNextFloat = animationParameters.column;
                break;
        }
        if ((this.mDirection & 1) == 1) {
            return (animationParameters.columnsCount - 1) - iNextFloat;
        }
        return iNextFloat;
    }

    private int getTransformedRowIndex(AnimationParameters animationParameters) {
        int iNextFloat;
        switch (getOrder()) {
            case 1:
                iNextFloat = (animationParameters.rowsCount - 1) - animationParameters.row;
                break;
            case 2:
                if (this.mRandomizer == null) {
                    this.mRandomizer = new Random();
                }
                iNextFloat = (int) (animationParameters.rowsCount * this.mRandomizer.nextFloat());
                break;
            default:
                iNextFloat = animationParameters.row;
                break;
        }
        if ((this.mDirection & 2) == 2) {
            return (animationParameters.rowsCount - 1) - iNextFloat;
        }
        return iNextFloat;
    }
}
