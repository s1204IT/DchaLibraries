package android.support.v17.leanback.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.support.v17.leanback.R;
import android.util.AttributeSet;
import android.view.View;

class MediaRowFocusView extends View {
    private final Paint mPaint;
    private final RectF mRoundRectF;
    private int mRoundRectRadius;

    public MediaRowFocusView(Context context) {
        super(context);
        this.mRoundRectF = new RectF();
        this.mPaint = createPaint(context);
    }

    public MediaRowFocusView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mRoundRectF = new RectF();
        this.mPaint = createPaint(context);
    }

    public MediaRowFocusView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.mRoundRectF = new RectF();
        this.mPaint = createPaint(context);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        this.mRoundRectRadius = getHeight() / 2;
        int drawHeight = this.mRoundRectRadius * 2;
        int drawOffset = (drawHeight - getHeight()) / 2;
        this.mRoundRectF.set(0.0f, -drawOffset, getWidth(), getHeight() + drawOffset);
        canvas.drawRoundRect(this.mRoundRectF, this.mRoundRectRadius, this.mRoundRectRadius, this.mPaint);
    }

    private Paint createPaint(Context context) {
        Paint paint = new Paint();
        paint.setColor(context.getResources().getColor(R.color.lb_playback_media_row_highlight_color));
        return paint;
    }
}
