package com.android.setupwizardlib;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.ScrollView;
import android.widget.TextView;
import com.android.setupwizardlib.template.ButtonFooterMixin;
import com.android.setupwizardlib.template.ColoredHeaderMixin;
import com.android.setupwizardlib.template.HeaderMixin;
import com.android.setupwizardlib.template.IconMixin;
import com.android.setupwizardlib.template.ProgressBarMixin;
import com.android.setupwizardlib.template.RequireScrollMixin;
import com.android.setupwizardlib.template.ScrollViewScrollHandlingDelegate;
import com.android.setupwizardlib.view.StatusBarBackgroundLayout;

public class GlifLayout extends TemplateLayout {
    private ColorStateList mBackgroundBaseColor;
    private boolean mBackgroundPatterned;
    private boolean mLayoutFullscreen;
    private ColorStateList mPrimaryColor;

    public GlifLayout(Context context) {
        this(context, 0, 0);
    }

    public GlifLayout(Context context, int i) {
        this(context, i, 0);
    }

    public GlifLayout(Context context, int i, int i2) {
        super(context, i, i2);
        this.mBackgroundPatterned = true;
        this.mLayoutFullscreen = true;
        init(null, R.attr.suwLayoutTheme);
    }

    public GlifLayout(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mBackgroundPatterned = true;
        this.mLayoutFullscreen = true;
        init(attributeSet, R.attr.suwLayoutTheme);
    }

    @TargetApi(11)
    public GlifLayout(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mBackgroundPatterned = true;
        this.mLayoutFullscreen = true;
        init(attributeSet, i);
    }

    private void init(AttributeSet attributeSet, int i) {
        registerMixin(HeaderMixin.class, new ColoredHeaderMixin(this, attributeSet, i));
        registerMixin(IconMixin.class, new IconMixin(this, attributeSet, i));
        registerMixin(ProgressBarMixin.class, new ProgressBarMixin(this));
        registerMixin(ButtonFooterMixin.class, new ButtonFooterMixin(this));
        RequireScrollMixin requireScrollMixin = new RequireScrollMixin(this);
        registerMixin(RequireScrollMixin.class, requireScrollMixin);
        ScrollView scrollView = getScrollView();
        if (scrollView != null) {
            requireScrollMixin.setScrollHandlingDelegate(new ScrollViewScrollHandlingDelegate(requireScrollMixin, scrollView));
        }
        TypedArray typedArrayObtainStyledAttributes = getContext().obtainStyledAttributes(attributeSet, R.styleable.SuwGlifLayout, i, 0);
        ColorStateList colorStateList = typedArrayObtainStyledAttributes.getColorStateList(R.styleable.SuwGlifLayout_suwColorPrimary);
        if (colorStateList != null) {
            setPrimaryColor(colorStateList);
        }
        setBackgroundBaseColor(typedArrayObtainStyledAttributes.getColorStateList(R.styleable.SuwGlifLayout_suwBackgroundBaseColor));
        setBackgroundPatterned(typedArrayObtainStyledAttributes.getBoolean(R.styleable.SuwGlifLayout_suwBackgroundPatterned, true));
        int resourceId = typedArrayObtainStyledAttributes.getResourceId(R.styleable.SuwGlifLayout_suwFooter, 0);
        if (resourceId != 0) {
            inflateFooter(resourceId);
        }
        int resourceId2 = typedArrayObtainStyledAttributes.getResourceId(R.styleable.SuwGlifLayout_suwStickyHeader, 0);
        if (resourceId2 != 0) {
            inflateStickyHeader(resourceId2);
        }
        this.mLayoutFullscreen = typedArrayObtainStyledAttributes.getBoolean(R.styleable.SuwGlifLayout_suwLayoutFullscreen, true);
        typedArrayObtainStyledAttributes.recycle();
        if (Build.VERSION.SDK_INT >= 21 && this.mLayoutFullscreen) {
            setSystemUiVisibility(1024);
        }
    }

    @Override
    protected View onInflateTemplate(LayoutInflater layoutInflater, int i) {
        if (i == 0) {
            i = R.layout.suw_glif_template;
        }
        return inflateTemplate(layoutInflater, R.style.SuwThemeGlif_Light, i);
    }

    @Override
    protected ViewGroup findContainer(int i) {
        if (i == 0) {
            i = R.id.suw_layout_content;
        }
        return super.findContainer(i);
    }

    public View inflateFooter(int i) {
        ViewStub viewStub = (ViewStub) findManagedViewById(R.id.suw_layout_footer);
        viewStub.setLayoutResource(i);
        return viewStub.inflate();
    }

    public View inflateStickyHeader(int i) {
        ViewStub viewStub = (ViewStub) findManagedViewById(R.id.suw_layout_sticky_header);
        viewStub.setLayoutResource(i);
        return viewStub.inflate();
    }

    public ScrollView getScrollView() {
        View viewFindManagedViewById = findManagedViewById(R.id.suw_scroll_view);
        if (viewFindManagedViewById instanceof ScrollView) {
            return (ScrollView) viewFindManagedViewById;
        }
        return null;
    }

    public TextView getHeaderTextView() {
        return ((HeaderMixin) getMixin(HeaderMixin.class)).getTextView();
    }

    public void setHeaderText(int i) {
        ((HeaderMixin) getMixin(HeaderMixin.class)).setText(i);
    }

    public void setHeaderText(CharSequence charSequence) {
        ((HeaderMixin) getMixin(HeaderMixin.class)).setText(charSequence);
    }

    public CharSequence getHeaderText() {
        return ((HeaderMixin) getMixin(HeaderMixin.class)).getText();
    }

    public void setHeaderColor(ColorStateList colorStateList) {
        ((ColoredHeaderMixin) getMixin(HeaderMixin.class)).setColor(colorStateList);
    }

    public ColorStateList getHeaderColor() {
        return ((ColoredHeaderMixin) getMixin(HeaderMixin.class)).getColor();
    }

    public void setIcon(Drawable drawable) {
        ((IconMixin) getMixin(IconMixin.class)).setIcon(drawable);
    }

    public Drawable getIcon() {
        return ((IconMixin) getMixin(IconMixin.class)).getIcon();
    }

    public void setPrimaryColor(ColorStateList colorStateList) {
        this.mPrimaryColor = colorStateList;
        updateBackground();
        ((ProgressBarMixin) getMixin(ProgressBarMixin.class)).setColor(colorStateList);
    }

    public ColorStateList getPrimaryColor() {
        return this.mPrimaryColor;
    }

    public void setBackgroundBaseColor(ColorStateList colorStateList) {
        this.mBackgroundBaseColor = colorStateList;
        updateBackground();
    }

    public ColorStateList getBackgroundBaseColor() {
        return this.mBackgroundBaseColor;
    }

    public void setBackgroundPatterned(boolean z) {
        this.mBackgroundPatterned = z;
        updateBackground();
    }

    private void updateBackground() {
        Drawable colorDrawable;
        View viewFindManagedViewById = findManagedViewById(R.id.suw_pattern_bg);
        if (viewFindManagedViewById != null) {
            int defaultColor = 0;
            if (this.mBackgroundBaseColor != null) {
                defaultColor = this.mBackgroundBaseColor.getDefaultColor();
            } else if (this.mPrimaryColor != null) {
                defaultColor = this.mPrimaryColor.getDefaultColor();
            }
            if (this.mBackgroundPatterned) {
                colorDrawable = new GlifPatternDrawable(defaultColor);
            } else {
                colorDrawable = new ColorDrawable(defaultColor);
            }
            if (viewFindManagedViewById instanceof StatusBarBackgroundLayout) {
                ((StatusBarBackgroundLayout) viewFindManagedViewById).setStatusBarBackground(colorDrawable);
            } else {
                viewFindManagedViewById.setBackgroundDrawable(colorDrawable);
            }
        }
    }

    public void setProgressBarShown(boolean z) {
        ((ProgressBarMixin) getMixin(ProgressBarMixin.class)).setShown(z);
    }
}
