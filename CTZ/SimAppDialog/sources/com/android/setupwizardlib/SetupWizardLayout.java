package com.android.setupwizardlib;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.android.setupwizardlib.template.ProgressBarMixin;

public class SetupWizardLayout extends TemplateLayout {
    @Override
    protected Parcelable onSaveInstanceState() {
        SavedState savedState = new SavedState(super.onSaveInstanceState());
        savedState.mIsProgressBarShown = isProgressBarShown();
        return savedState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable parcelable) {
        if (!(parcelable instanceof SavedState)) {
            Log.w("SetupWizardLayout", "Ignoring restore instance state " + parcelable);
            super.onRestoreInstanceState(parcelable);
            return;
        }
        super.onRestoreInstanceState(parcelable.getSuperState());
        setProgressBarShown(parcelable.mIsProgressBarShown);
    }

    @Override
    protected View onInflateTemplate(LayoutInflater layoutInflater, int i) {
        if (i == 0) {
            i = R.layout.suw_template;
        }
        return inflateTemplate(layoutInflater, R.style.SuwThemeMaterial_Light, i);
    }

    @Override
    protected ViewGroup findContainer(int i) {
        if (i == 0) {
            i = R.id.suw_layout_content;
        }
        return super.findContainer(i);
    }

    public boolean isProgressBarShown() {
        return ((ProgressBarMixin) getMixin(ProgressBarMixin.class)).isShown();
    }

    public void setProgressBarShown(boolean z) {
        ((ProgressBarMixin) getMixin(ProgressBarMixin.class)).setShown(z);
    }

    protected static class SavedState extends View.BaseSavedState {
        public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel parcel) {
                return new SavedState(parcel);
            }

            @Override
            public SavedState[] newArray(int i) {
                return new SavedState[i];
            }
        };
        boolean mIsProgressBarShown;

        public SavedState(Parcelable parcelable) {
            super(parcelable);
            this.mIsProgressBarShown = false;
        }

        public SavedState(Parcel parcel) {
            super(parcel);
            this.mIsProgressBarShown = false;
            this.mIsProgressBarShown = parcel.readInt() != 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            super.writeToParcel(parcel, i);
            parcel.writeInt(this.mIsProgressBarShown ? 1 : 0);
        }
    }
}
