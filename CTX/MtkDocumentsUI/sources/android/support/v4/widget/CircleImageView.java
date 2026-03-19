package android.support.v4.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.os.Build;
import android.support.v4.view.ViewCompat;
import android.view.animation.Animation;
import android.widget.ImageView;

class CircleImageView extends ImageView {
    private Animation.AnimationListener mListener;
    int mShadowRadius;

    CircleImageView(Context context, int color) {
        ShapeDrawable circle;
        super(context);
        float density = getContext().getResources().getDisplayMetrics().density;
        int shadowYOffset = (int) (1.75f * density);
        int shadowXOffset = (int) (0.0f * density);
        this.mShadowRadius = (int) (3.5f * density);
        if (elevationSupported()) {
            circle = new ShapeDrawable(new OvalShape());
            ViewCompat.setElevation(this, 4.0f * density);
        } else {
            OvalShape oval = new OvalShadow(this.mShadowRadius);
            ShapeDrawable circle2 = new ShapeDrawable(oval);
            setLayerType(1, circle2.getPaint());
            circle2.getPaint().setShadowLayer(this.mShadowRadius, shadowXOffset, shadowYOffset, 503316480);
            int padding = this.mShadowRadius;
            setPadding(padding, padding, padding, padding);
            circle = circle2;
        }
        circle.getPaint().setColor(color);
        ViewCompat.setBackground(this, circle);
    }

    private boolean elevationSupported() {
        return Build.VERSION.SDK_INT >= 21;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (!elevationSupported()) {
            setMeasuredDimension(getMeasuredWidth() + (this.mShadowRadius * 2), getMeasuredHeight() + (this.mShadowRadius * 2));
        }
    }

    public void setAnimationListener(Animation.AnimationListener listener) {
        this.mListener = listener;
    }

    @Override
    public void onAnimationStart() {
        super.onAnimationStart();
        if (this.mListener != null) {
            this.mListener.onAnimationStart(getAnimation());
        }
    }

    @Override
    public void onAnimationEnd() {
        super.onAnimationEnd();
        if (this.mListener != null) {
            this.mListener.onAnimationEnd(getAnimation());
        }
    }

    @Override
    public void setBackgroundColor(int color) {
        if (getBackground() instanceof ShapeDrawable) {
            ((ShapeDrawable) getBackground()).getPaint().setColor(color);
        }
    }

    private class OvalShadow extends OvalShape {
        private RadialGradient mRadialGradient;
        private Paint mShadowPaint = new Paint();

        OvalShadow(int shadowRadius) {
            CircleImageView.this.mShadowRadius = shadowRadius;
            updateRadialGradient((int) rect().width());
        }

        @Override
        protected void onResize(float width, float height) {
            super.onResize(width, height);
            updateRadialGradient((int) width);
        }

        @Override
        public void draw(Canvas canvas, Paint paint) {
            int viewWidth = CircleImageView.this.getWidth();
            int viewHeight = CircleImageView.this.getHeight();
            canvas.drawCircle(viewWidth / 2, viewHeight / 2, viewWidth / 2, this.mShadowPaint);
            canvas.drawCircle(viewWidth / 2, viewHeight / 2, (viewWidth / 2) - CircleImageView.this.mShadowRadius, paint);
        }

        private void updateRadialGradient(int diameter) {
            this.mRadialGradient = new RadialGradient(diameter / 2, diameter / 2, CircleImageView.this.mShadowRadius, new int[]{1023410176, 0}, (float[]) null, Shader.TileMode.CLAMP);
            this.mShadowPaint.setShader(this.mRadialGradient);
        }
    }
}
