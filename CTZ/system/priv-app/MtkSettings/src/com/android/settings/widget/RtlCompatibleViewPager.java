package com.android.settings.widget;

import android.content.Context;
import android.content.res.Resources;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import java.util.Locale;

/* loaded from: classes.dex */
public final class RtlCompatibleViewPager extends ViewPager {
    public RtlCompatibleViewPager(Context context) {
        this(context, null);
    }

    public RtlCompatibleViewPager(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    @Override // android.support.v4.view.ViewPager
    public int getCurrentItem() {
        return getRtlAwareIndex(super.getCurrentItem());
    }

    @Override // android.support.v4.view.ViewPager
    public void setCurrentItem(int i) throws Resources.NotFoundException {
        super.setCurrentItem(getRtlAwareIndex(i));
    }

    @Override // android.support.v4.view.ViewPager, android.view.View
    public Parcelable onSaveInstanceState() {
        RtlSavedState rtlSavedState = new RtlSavedState(super.onSaveInstanceState());
        rtlSavedState.position = getCurrentItem();
        return rtlSavedState;
    }

    @Override // android.support.v4.view.ViewPager, android.view.View
    public void onRestoreInstanceState(Parcelable parcelable) throws Resources.NotFoundException {
        RtlSavedState rtlSavedState = (RtlSavedState) parcelable;
        super.onRestoreInstanceState(rtlSavedState.getSuperState());
        setCurrentItem(rtlSavedState.position);
    }

    public int getRtlAwareIndex(int i) {
        if (TextUtils.getLayoutDirectionFromLocale(Locale.getDefault()) == 1) {
            return (getAdapter().getCount() - i) - 1;
        }
        return i;
    }

    static class RtlSavedState extends View.BaseSavedState {
        public static final Parcelable.ClassLoaderCreator<RtlSavedState> CREATOR = new Parcelable.ClassLoaderCreator<RtlSavedState>() { // from class: com.android.settings.widget.RtlCompatibleViewPager.RtlSavedState.1
            /* JADX DEBUG: Method merged with bridge method: createFromParcel(Landroid/os/Parcel;Ljava/lang/ClassLoader;)Ljava/lang/Object; */
            /* JADX WARN: Can't rename method to resolve collision */
            @Override // android.os.Parcelable.ClassLoaderCreator
            public RtlSavedState createFromParcel(Parcel parcel, ClassLoader classLoader) {
                return new RtlSavedState(parcel, classLoader);
            }

            /* JADX DEBUG: Method merged with bridge method: createFromParcel(Landroid/os/Parcel;)Ljava/lang/Object; */
            @Override // android.os.Parcelable.Creator
            public RtlSavedState createFromParcel(Parcel parcel) {
                return new RtlSavedState(parcel, null);
            }

            /* JADX DEBUG: Method merged with bridge method: newArray(I)[Ljava/lang/Object; */
            @Override // android.os.Parcelable.Creator
            public RtlSavedState[] newArray(int i) {
                return new RtlSavedState[i];
            }
        };
        int position;

        public RtlSavedState(Parcelable parcelable) {
            super(parcelable);
        }

        private RtlSavedState(Parcel parcel, ClassLoader classLoader) {
            super(parcel, classLoader);
            this.position = parcel.readInt();
        }

        @Override // android.view.View.BaseSavedState, android.view.AbsSavedState, android.os.Parcelable
        public void writeToParcel(Parcel parcel, int i) {
            super.writeToParcel(parcel, i);
            parcel.writeInt(this.position);
        }
    }
}
