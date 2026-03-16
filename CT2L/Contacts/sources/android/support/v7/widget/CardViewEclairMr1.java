package android.support.v7.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.support.v7.widget.RoundRectDrawableWithShadow;
import android.view.View;

class CardViewEclairMr1 implements CardViewImpl {
    final RectF sCornerRect = new RectF();

    CardViewEclairMr1() {
    }

    @Override
    public void initStatic() {
        RoundRectDrawableWithShadow.sRoundRectHelper = new RoundRectDrawableWithShadow.RoundRectHelper() {
            @Override
            public void drawRoundRect(Canvas canvas, RectF bounds, float cornerRadius, Paint paint) {
                float twoRadius = cornerRadius * 2.0f;
                float innerWidth = (bounds.width() - twoRadius) - 1.0f;
                float innerHeight = (bounds.height() - twoRadius) - 1.0f;
                if (cornerRadius >= 1.0f) {
                    cornerRadius += 0.5f;
                    CardViewEclairMr1.this.sCornerRect.set(-cornerRadius, -cornerRadius, cornerRadius, cornerRadius);
                    int saved = canvas.save();
                    canvas.translate(bounds.left + cornerRadius, bounds.top + cornerRadius);
                    canvas.drawArc(CardViewEclairMr1.this.sCornerRect, 180.0f, 90.0f, true, paint);
                    canvas.translate(innerWidth, 0.0f);
                    canvas.rotate(90.0f);
                    canvas.drawArc(CardViewEclairMr1.this.sCornerRect, 180.0f, 90.0f, true, paint);
                    canvas.translate(innerHeight, 0.0f);
                    canvas.rotate(90.0f);
                    canvas.drawArc(CardViewEclairMr1.this.sCornerRect, 180.0f, 90.0f, true, paint);
                    canvas.translate(innerWidth, 0.0f);
                    canvas.rotate(90.0f);
                    canvas.drawArc(CardViewEclairMr1.this.sCornerRect, 180.0f, 90.0f, true, paint);
                    canvas.restoreToCount(saved);
                    canvas.drawRect((bounds.left + cornerRadius) - 1.0f, bounds.top, 1.0f + (bounds.right - cornerRadius), bounds.top + cornerRadius, paint);
                    canvas.drawRect((bounds.left + cornerRadius) - 1.0f, 1.0f + (bounds.bottom - cornerRadius), 1.0f + (bounds.right - cornerRadius), bounds.bottom, paint);
                }
                canvas.drawRect(bounds.left, Math.max(0.0f, cornerRadius - 1.0f) + bounds.top, bounds.right, 1.0f + (bounds.bottom - cornerRadius), paint);
            }
        };
    }

    @Override
    public void initialize(CardViewDelegate cardView, Context context, int backgroundColor, float radius, float elevation, float maxElevation) {
        RoundRectDrawableWithShadow background = createBackground(context, backgroundColor, radius, elevation, maxElevation);
        background.setAddPaddingForCorners(cardView.getPreventCornerOverlap());
        cardView.setBackgroundDrawable(background);
        updatePadding(cardView);
    }

    RoundRectDrawableWithShadow createBackground(Context context, int backgroundColor, float radius, float elevation, float maxElevation) {
        return new RoundRectDrawableWithShadow(context.getResources(), backgroundColor, radius, elevation, maxElevation);
    }

    public void updatePadding(CardViewDelegate cardViewDelegate) {
        Rect shadowPadding = new Rect();
        getShadowBackground(cardViewDelegate).getMaxShadowAndCornerPadding(shadowPadding);
        ((View) cardViewDelegate).setMinimumHeight((int) Math.ceil(getMinHeight(cardViewDelegate)));
        ((View) cardViewDelegate).setMinimumWidth((int) Math.ceil(getMinWidth(cardViewDelegate)));
        cardViewDelegate.setShadowPadding(shadowPadding.left, shadowPadding.top, shadowPadding.right, shadowPadding.bottom);
    }

    @Override
    public float getMinWidth(CardViewDelegate cardView) {
        return getShadowBackground(cardView).getMinWidth();
    }

    @Override
    public float getMinHeight(CardViewDelegate cardView) {
        return getShadowBackground(cardView).getMinHeight();
    }

    private RoundRectDrawableWithShadow getShadowBackground(CardViewDelegate cardView) {
        return (RoundRectDrawableWithShadow) cardView.getBackground();
    }
}
