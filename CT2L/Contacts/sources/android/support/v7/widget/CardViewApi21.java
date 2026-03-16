package android.support.v7.widget;

import android.content.Context;
import android.view.View;

class CardViewApi21 implements CardViewImpl {
    CardViewApi21() {
    }

    @Override
    public void initialize(CardViewDelegate cardViewDelegate, Context context, int backgroundColor, float radius, float elevation, float maxElevation) {
        RoundRectDrawable backgroundDrawable = new RoundRectDrawable(backgroundColor, radius);
        cardViewDelegate.setBackgroundDrawable(backgroundDrawable);
        View view = (View) cardViewDelegate;
        view.setClipToOutline(true);
        view.setElevation(elevation);
        setMaxElevation(cardViewDelegate, maxElevation);
    }

    @Override
    public void initStatic() {
    }

    public void setMaxElevation(CardViewDelegate cardView, float maxElevation) {
        ((RoundRectDrawable) cardView.getBackground()).setPadding(maxElevation, cardView.getUseCompatPadding(), cardView.getPreventCornerOverlap());
        updatePadding(cardView);
    }

    public float getMaxElevation(CardViewDelegate cardView) {
        return ((RoundRectDrawable) cardView.getBackground()).getPadding();
    }

    @Override
    public float getMinWidth(CardViewDelegate cardView) {
        return getRadius(cardView) * 2.0f;
    }

    @Override
    public float getMinHeight(CardViewDelegate cardView) {
        return getRadius(cardView) * 2.0f;
    }

    public float getRadius(CardViewDelegate cardView) {
        return ((RoundRectDrawable) cardView.getBackground()).getRadius();
    }

    public void updatePadding(CardViewDelegate cardView) {
        if (!cardView.getUseCompatPadding()) {
            cardView.setShadowPadding(0, 0, 0, 0);
            return;
        }
        float elevation = getMaxElevation(cardView);
        float radius = getRadius(cardView);
        int hPadding = (int) Math.ceil(RoundRectDrawableWithShadow.calculateHorizontalPadding(elevation, radius, cardView.getPreventCornerOverlap()));
        int vPadding = (int) Math.ceil(RoundRectDrawableWithShadow.calculateVerticalPadding(elevation, radius, cardView.getPreventCornerOverlap()));
        cardView.setShadowPadding(hPadding, vPadding, hPadding, vPadding);
    }
}
