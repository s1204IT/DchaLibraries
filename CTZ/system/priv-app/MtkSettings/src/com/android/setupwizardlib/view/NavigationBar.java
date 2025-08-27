package com.android.setupwizardlib.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import com.android.setupwizardlib.R;

/* loaded from: classes.dex */
public class NavigationBar extends LinearLayout implements View.OnClickListener {
    private Button mBackButton;
    private NavigationBarListener mListener;
    private Button mMoreButton;
    private Button mNextButton;

    public interface NavigationBarListener {
        void onNavigateBack();

        void onNavigateNext();
    }

    private static int getNavbarTheme(Context context) {
        TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(new int[]{R.attr.suwNavBarTheme, android.R.attr.colorForeground, android.R.attr.colorBackground});
        int resourceId = typedArrayObtainStyledAttributes.getResourceId(0, 0);
        if (resourceId == 0) {
            float[] fArr = new float[3];
            float[] fArr2 = new float[3];
            Color.colorToHSV(typedArrayObtainStyledAttributes.getColor(1, 0), fArr);
            Color.colorToHSV(typedArrayObtainStyledAttributes.getColor(2, 0), fArr2);
            resourceId = fArr[2] > fArr2[2] ? R.style.SuwNavBarThemeDark : R.style.SuwNavBarThemeLight;
        }
        typedArrayObtainStyledAttributes.recycle();
        return resourceId;
    }

    private static Context getThemedContext(Context context) {
        return new ContextThemeWrapper(context, getNavbarTheme(context));
    }

    public NavigationBar(Context context) {
        super(getThemedContext(context));
        init();
    }

    public NavigationBar(Context context, AttributeSet attributeSet) {
        super(getThemedContext(context), attributeSet);
        init();
    }

    @TargetApi(11)
    public NavigationBar(Context context, AttributeSet attributeSet, int i) {
        super(getThemedContext(context), attributeSet, i);
        init();
    }

    private void init() {
        View.inflate(getContext(), R.layout.suw_navbar_view, this);
        this.mNextButton = (Button) findViewById(R.id.suw_navbar_next);
        this.mBackButton = (Button) findViewById(R.id.suw_navbar_back);
        this.mMoreButton = (Button) findViewById(R.id.suw_navbar_more);
    }

    public Button getBackButton() {
        return this.mBackButton;
    }

    public Button getNextButton() {
        return this.mNextButton;
    }

    public Button getMoreButton() {
        return this.mMoreButton;
    }

    public void setNavigationBarListener(NavigationBarListener navigationBarListener) {
        this.mListener = navigationBarListener;
        if (this.mListener != null) {
            getBackButton().setOnClickListener(this);
            getNextButton().setOnClickListener(this);
        }
    }

    @Override // android.view.View.OnClickListener
    public void onClick(View view) {
        if (this.mListener != null) {
            if (view == getBackButton()) {
                this.mListener.onNavigateBack();
            } else if (view == getNextButton()) {
                this.mListener.onNavigateNext();
            }
        }
    }
}
