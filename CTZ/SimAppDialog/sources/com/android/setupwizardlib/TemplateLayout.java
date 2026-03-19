package com.android.setupwizardlib;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import com.android.setupwizardlib.template.Mixin;
import com.android.setupwizardlib.util.FallbackThemeWrapper;
import java.util.HashMap;
import java.util.Map;

public class TemplateLayout extends FrameLayout {
    private ViewGroup mContainer;
    private Map<Class<? extends Mixin>, Mixin> mMixins;

    public TemplateLayout(Context context, int i, int i2) {
        super(context);
        this.mMixins = new HashMap();
        init(i, i2, null, R.attr.suwLayoutTheme);
    }

    public TemplateLayout(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mMixins = new HashMap();
        init(0, 0, attributeSet, R.attr.suwLayoutTheme);
    }

    @TargetApi(11)
    public TemplateLayout(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mMixins = new HashMap();
        init(0, 0, attributeSet, i);
    }

    private void init(int i, int i2, AttributeSet attributeSet, int i3) {
        TypedArray typedArrayObtainStyledAttributes = getContext().obtainStyledAttributes(attributeSet, R.styleable.SuwTemplateLayout, i3, 0);
        if (i == 0) {
            i = typedArrayObtainStyledAttributes.getResourceId(R.styleable.SuwTemplateLayout_android_layout, 0);
        }
        if (i2 == 0) {
            i2 = typedArrayObtainStyledAttributes.getResourceId(R.styleable.SuwTemplateLayout_suwContainer, 0);
        }
        inflateTemplate(i, i2);
        typedArrayObtainStyledAttributes.recycle();
    }

    protected <M extends Mixin> void registerMixin(Class<M> cls, M m) {
        this.mMixins.put(cls, m);
    }

    public <T extends View> T findManagedViewById(int i) {
        return (T) findViewById(i);
    }

    public <M extends Mixin> M getMixin(Class<M> cls) {
        return (M) this.mMixins.get(cls);
    }

    @Override
    public void addView(View view, int i, ViewGroup.LayoutParams layoutParams) {
        this.mContainer.addView(view, i, layoutParams);
    }

    private void addViewInternal(View view) {
        super.addView(view, -1, generateDefaultLayoutParams());
    }

    private void inflateTemplate(int i, int i2) {
        addViewInternal(onInflateTemplate(LayoutInflater.from(getContext()), i));
        this.mContainer = findContainer(i2);
        if (this.mContainer == null) {
            throw new IllegalArgumentException("Container cannot be null in TemplateLayout");
        }
        onTemplateInflated();
    }

    protected View onInflateTemplate(LayoutInflater layoutInflater, int i) {
        return inflateTemplate(layoutInflater, 0, i);
    }

    protected final View inflateTemplate(LayoutInflater layoutInflater, int i, int i2) {
        if (i2 == 0) {
            throw new IllegalArgumentException("android:layout not specified for TemplateLayout");
        }
        if (i != 0) {
            layoutInflater = LayoutInflater.from(new FallbackThemeWrapper(layoutInflater.getContext(), i));
        }
        return layoutInflater.inflate(i2, (ViewGroup) this, false);
    }

    protected ViewGroup findContainer(int i) {
        if (i == 0) {
            i = getContainerId();
        }
        return (ViewGroup) findViewById(i);
    }

    protected void onTemplateInflated() {
    }

    @Deprecated
    protected int getContainerId() {
        return 0;
    }
}
