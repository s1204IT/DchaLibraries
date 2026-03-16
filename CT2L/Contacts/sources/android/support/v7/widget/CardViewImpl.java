package android.support.v7.widget;

import android.content.Context;

interface CardViewImpl {
    float getMinHeight(CardViewDelegate cardViewDelegate);

    float getMinWidth(CardViewDelegate cardViewDelegate);

    void initStatic();

    void initialize(CardViewDelegate cardViewDelegate, Context context, int i, float f, float f2, float f3);
}
