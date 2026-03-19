package android.preference;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.Preference;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import com.android.internal.R;

public final class PreferenceScreen extends PreferenceGroup implements AdapterView.OnItemClickListener, DialogInterface.OnDismissListener {
    private Dialog mDialog;
    private Drawable mDividerDrawable;
    private boolean mDividerSpecified;
    private int mLayoutResId;
    private ListView mListView;
    private ListAdapter mRootAdapter;

    public PreferenceScreen(Context context, AttributeSet attributeSet) {
        super(context, attributeSet, 16842891);
        this.mLayoutResId = R.layout.preference_list_fragment;
        TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(null, R.styleable.PreferenceScreen, 16842891, 0);
        this.mLayoutResId = typedArrayObtainStyledAttributes.getResourceId(1, this.mLayoutResId);
        if (typedArrayObtainStyledAttributes.hasValueOrEmpty(0)) {
            this.mDividerDrawable = typedArrayObtainStyledAttributes.getDrawable(0);
            this.mDividerSpecified = true;
        }
        typedArrayObtainStyledAttributes.recycle();
    }

    public ListAdapter getRootAdapter() {
        if (this.mRootAdapter == null) {
            this.mRootAdapter = onCreateRootAdapter();
        }
        return this.mRootAdapter;
    }

    protected ListAdapter onCreateRootAdapter() {
        return new PreferenceGroupAdapter(this);
    }

    public void bind(ListView listView) {
        listView.setOnItemClickListener(this);
        listView.setAdapter(getRootAdapter());
        onAttachedToActivity();
    }

    @Override
    protected void onClick() {
        if (getIntent() != null || getFragment() != null || getPreferenceCount() == 0) {
            return;
        }
        showDialog(null);
    }

    private void showDialog(Bundle bundle) {
        Context context = getContext();
        if (this.mListView != null) {
            this.mListView.setAdapter((ListAdapter) null);
        }
        View viewInflate = ((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(this.mLayoutResId, (ViewGroup) null);
        View viewFindViewById = viewInflate.findViewById(16908310);
        this.mListView = (ListView) viewInflate.findViewById(16908298);
        if (this.mDividerSpecified) {
            this.mListView.setDivider(this.mDividerDrawable);
        }
        bind(this.mListView);
        CharSequence title = getTitle();
        Dialog dialog = new Dialog(context, context.getThemeResId());
        this.mDialog = dialog;
        if (TextUtils.isEmpty(title)) {
            if (viewFindViewById != null) {
                viewFindViewById.setVisibility(8);
            }
            dialog.getWindow().requestFeature(1);
        } else if (viewFindViewById instanceof TextView) {
            ((TextView) viewFindViewById).setText(title);
            viewFindViewById.setVisibility(0);
        } else {
            dialog.setTitle(title);
        }
        dialog.setContentView(viewInflate);
        dialog.setOnDismissListener(this);
        if (bundle != null) {
            dialog.onRestoreInstanceState(bundle);
        }
        getPreferenceManager().addPreferencesScreen(dialog);
        dialog.show();
    }

    @Override
    public void onDismiss(DialogInterface dialogInterface) {
        this.mDialog = null;
        getPreferenceManager().removePreferencesScreen(dialogInterface);
    }

    public Dialog getDialog() {
        return this.mDialog;
    }

    @Override
    public void onItemClick(AdapterView adapterView, View view, int i, long j) {
        if (adapterView instanceof ListView) {
            i -= ((ListView) adapterView).getHeaderViewsCount();
        }
        Object item = getRootAdapter().getItem(i);
        if (item instanceof Preference) {
            ((Preference) item).performClick(this);
        }
    }

    @Override
    protected boolean isOnSameScreenAsChildren() {
        return false;
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable parcelableOnSaveInstanceState = super.onSaveInstanceState();
        Dialog dialog = this.mDialog;
        if (dialog == null || !dialog.isShowing()) {
            return parcelableOnSaveInstanceState;
        }
        SavedState savedState = new SavedState(parcelableOnSaveInstanceState);
        savedState.isDialogShowing = true;
        savedState.dialogBundle = dialog.onSaveInstanceState();
        return savedState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable parcelable) {
        if (parcelable == null || !parcelable.getClass().equals(SavedState.class)) {
            super.onRestoreInstanceState(parcelable);
            return;
        }
        SavedState savedState = (SavedState) parcelable;
        super.onRestoreInstanceState(savedState.getSuperState());
        if (savedState.isDialogShowing) {
            showDialog(savedState.dialogBundle);
        }
    }

    private static class SavedState extends Preference.BaseSavedState {
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
        Bundle dialogBundle;
        boolean isDialogShowing;

        public SavedState(Parcel parcel) {
            super(parcel);
            this.isDialogShowing = parcel.readInt() == 1;
            this.dialogBundle = parcel.readBundle();
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            super.writeToParcel(parcel, i);
            parcel.writeInt(this.isDialogShowing ? 1 : 0);
            parcel.writeBundle(this.dialogBundle);
        }

        public SavedState(Parcelable parcelable) {
            super(parcelable);
        }
    }
}
