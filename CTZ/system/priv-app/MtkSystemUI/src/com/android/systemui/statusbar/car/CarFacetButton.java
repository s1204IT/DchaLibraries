package com.android.systemui.statusbar.car;

import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.os.UserHandle;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import com.android.keyguard.AlphaOptimizedImageButton;
import com.android.systemui.Dependency;
import com.android.systemui.R;
import java.net.URISyntaxException;

/* loaded from: classes.dex */
public class CarFacetButton extends LinearLayout {
    private String[] mComponentNames;
    private Context mContext;
    private String[] mFacetCategories;
    private String[] mFacetPackages;
    private AlphaOptimizedImageButton mIcon;
    private int mIconResourceId;
    private AlphaOptimizedImageButton mMoreIcon;
    private boolean mSelected;
    private float mSelectedAlpha;
    private int mSelectedIconResourceId;
    private float mUnselectedAlpha;
    private boolean mUseMoreIcon;

    public CarFacetButton(Context context, AttributeSet attributeSet) throws URISyntaxException {
        super(context, attributeSet);
        this.mSelected = false;
        this.mUseMoreIcon = true;
        this.mSelectedAlpha = 1.0f;
        this.mUnselectedAlpha = 1.0f;
        this.mContext = context;
        View.inflate(context, R.layout.car_facet_button, this);
        TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R.styleable.CarFacetButton);
        setupIntents(typedArrayObtainStyledAttributes);
        setupIcons(typedArrayObtainStyledAttributes);
        ((CarFacetButtonController) Dependency.get(CarFacetButtonController.class)).addFacetButton(this);
    }

    private void setupIntents(TypedArray typedArray) throws URISyntaxException {
        String string = typedArray.getString(3);
        String string2 = typedArray.getString(4);
        String string3 = typedArray.getString(0);
        String string4 = typedArray.getString(5);
        String string5 = typedArray.getString(1);
        try {
            final Intent uri = Intent.parseUri(string, 1);
            uri.putExtra("filter_id", Integer.toString(getId()));
            if (string4 != null) {
                this.mFacetPackages = string4.split(";");
                uri.putExtra("packages", this.mFacetPackages);
            }
            if (string3 != null) {
                this.mFacetCategories = string3.split(";");
                uri.putExtra("categories", this.mFacetCategories);
            }
            if (string5 != null) {
                this.mComponentNames = string5.split(";");
            }
            setOnClickListener(new View.OnClickListener() { // from class: com.android.systemui.statusbar.car.-$$Lambda$CarFacetButton$i_dU0gknjwIWw7BhdaypNSeIb4I
                @Override // android.view.View.OnClickListener
                public final void onClick(View view) {
                    CarFacetButton.lambda$setupIntents$0(this.f$0, uri, view);
                }
            });
            if (string2 != null) {
                final Intent uri2 = Intent.parseUri(string2, 1);
                setOnLongClickListener(new View.OnLongClickListener() { // from class: com.android.systemui.statusbar.car.-$$Lambda$CarFacetButton$531BxJPdfLOepcWw24yntC-HGBQ
                    @Override // android.view.View.OnLongClickListener
                    public final boolean onLongClick(View view) {
                        return CarFacetButton.lambda$setupIntents$1(this.f$0, uri2, view);
                    }
                });
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to attach intent", e);
        }
    }

    public static /* synthetic */ void lambda$setupIntents$0(CarFacetButton carFacetButton, Intent intent, View view) {
        intent.putExtra("launch_picker", carFacetButton.mSelected);
        carFacetButton.mContext.startActivityAsUser(intent, UserHandle.CURRENT);
    }

    public static /* synthetic */ boolean lambda$setupIntents$1(CarFacetButton carFacetButton, Intent intent, View view) {
        carFacetButton.mContext.startActivityAsUser(intent, UserHandle.CURRENT);
        return true;
    }

    private void setupIcons(TypedArray typedArray) {
        this.mSelectedAlpha = typedArray.getFloat(6, this.mSelectedAlpha);
        this.mUnselectedAlpha = typedArray.getFloat(8, this.mUnselectedAlpha);
        this.mIcon = (AlphaOptimizedImageButton) findViewById(R.id.car_nav_button_icon);
        this.mIcon.setScaleType(ImageView.ScaleType.CENTER);
        this.mIcon.setClickable(false);
        this.mIcon.setAlpha(this.mUnselectedAlpha);
        this.mIconResourceId = typedArray.getResourceId(2, 0);
        this.mIcon.setImageResource(this.mIconResourceId);
        this.mSelectedIconResourceId = typedArray.getResourceId(7, this.mIconResourceId);
        this.mMoreIcon = (AlphaOptimizedImageButton) findViewById(R.id.car_nav_button_more_icon);
        this.mMoreIcon.setClickable(false);
        this.mMoreIcon.setAlpha(this.mSelectedAlpha);
        this.mMoreIcon.setVisibility(8);
        this.mUseMoreIcon = typedArray.getBoolean(9, true);
    }

    public String[] getCategories() {
        if (this.mFacetCategories == null) {
            return new String[0];
        }
        return this.mFacetCategories;
    }

    public String[] getFacetPackages() {
        if (this.mFacetPackages == null) {
            return new String[0];
        }
        return this.mFacetPackages;
    }

    public String[] getComponentName() {
        if (this.mComponentNames == null) {
            return new String[0];
        }
        return this.mComponentNames;
    }

    @Override // android.view.View
    public void setSelected(boolean z) {
        super.setSelected(z);
        setSelected(z, z);
    }

    public void setSelected(boolean z, boolean z2) {
        this.mSelected = z;
        this.mIcon.setAlpha(this.mSelected ? this.mSelectedAlpha : this.mUnselectedAlpha);
        this.mIcon.setImageResource(this.mSelected ? this.mSelectedIconResourceId : this.mIconResourceId);
        if (this.mUseMoreIcon) {
            this.mMoreIcon.setVisibility(z2 ? 0 : 8);
        }
    }
}
