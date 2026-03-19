package android.support.v7.widget;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.support.v7.widget.RoundRectDrawableWithShadow;

class CardViewBaseImpl implements CardViewImpl {
    private final RectF mCornerRect = new RectF();

    CardViewBaseImpl() {
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
                    float roundedCornerRadius = cornerRadius + 0.5f;
                    CardViewBaseImpl.this.mCornerRect.set(-roundedCornerRadius, -roundedCornerRadius, roundedCornerRadius, roundedCornerRadius);
                    int saved = canvas.save();
                    canvas.translate(bounds.left + roundedCornerRadius, bounds.top + roundedCornerRadius);
                    canvas.drawArc(CardViewBaseImpl.this.mCornerRect, 180.0f, 90.0f, true, paint);
                    canvas.translate(innerWidth, 0.0f);
                    canvas.rotate(90.0f);
                    canvas.drawArc(CardViewBaseImpl.this.mCornerRect, 180.0f, 90.0f, true, paint);
                    canvas.translate(innerHeight, 0.0f);
                    canvas.rotate(90.0f);
                    canvas.drawArc(CardViewBaseImpl.this.mCornerRect, 180.0f, 90.0f, true, paint);
                    canvas.translate(innerWidth, 0.0f);
                    canvas.rotate(90.0f);
                    canvas.drawArc(CardViewBaseImpl.this.mCornerRect, 180.0f, 90.0f, true, paint);
                    canvas.restoreToCount(saved);
                    canvas.drawRect((bounds.left + roundedCornerRadius) - 1.0f, bounds.top, (bounds.right - roundedCornerRadius) + 1.0f, bounds.top + roundedCornerRadius, paint);
                    canvas.drawRect((bounds.left + roundedCornerRadius) - 1.0f, bounds.bottom - roundedCornerRadius, (bounds.right - roundedCornerRadius) + 1.0f, bounds.bottom, paint);
                }
                canvas.drawRect(bounds.left, bounds.top + cornerRadius, bounds.right, bounds.bottom - cornerRadius, paint);
            }
        };
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
        return (RoundRectDrawableWithShadow) cardView.getCardBackground();
    }
}
