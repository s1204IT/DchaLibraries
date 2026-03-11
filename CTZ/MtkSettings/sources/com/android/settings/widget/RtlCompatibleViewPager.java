package com.android.settings.widget;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import java.util.Locale;

public final class RtlCompatibleViewPager extends ViewPager {
    public RtlCompatibleViewPager(Context context) {
        this(context, null);
    }

    public RtlCompatibleViewPager(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    @Override
    public int getCurrentItem() {
        return getRtlAwareIndex(super.getCurrentItem());
    }

    @Override
    public void setCurrentItem(int i) {
        super.setCurrentItem(getRtlAwareIndex(i));
    }

    @Override
    public Parcelable onSaveInstanceState() {
        RtlSavedState rtlSavedState = new RtlSavedState(super.onSaveInstanceState());
        rtlSavedState.position = getCurrentItem();
        return rtlSavedState;
    }

    @Override
    public void onRestoreInstanceState(Parcelable parcelable) {
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
        public static final Parcelable.ClassLoaderCreator<RtlSavedState> CREATOR = new Parcelable.ClassLoaderCreator<RtlSavedState>() {
            @Override
            public RtlSavedState createFromParcel(Parcel parcel, ClassLoader classLoader) {
                return new RtlSavedState(parcel, classLoader);
            }

            @Override
            public RtlSavedState createFromParcel(Parcel parcel) {
                return new RtlSavedState(parcel, null);
            }

            @Override
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

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            super.writeToParcel(parcel, i);
            parcel.writeInt(this.position);
        }
    }
}
