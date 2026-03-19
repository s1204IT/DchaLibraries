package com.android.documentsui.dirlist;

import android.content.Context;
import android.database.Cursor;
import android.support.v7.widget.RecyclerView;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;
import android.widget.ImageView;
import com.android.documentsui.selection.ItemDetailsLookup;

public abstract class DocumentHolder extends RecyclerView.ViewHolder implements View.OnKeyListener {
    static final boolean $assertionsDisabled = false;
    protected final Context mContext;
    private final DocumentItemDetails mDetails;
    private KeyboardEventListener mKeyEventListener;
    protected String mModelId;

    public abstract void bind(Cursor cursor, String str);

    public DocumentHolder(Context context, ViewGroup viewGroup, int i) {
        this(context, inflateLayout(context, viewGroup, i));
    }

    public DocumentHolder(Context context, View view) {
        super(view);
        this.itemView.setOnKeyListener(this);
        this.mContext = context;
        this.mDetails = new DocumentItemDetails();
    }

    public String getModelId() {
        return this.mModelId;
    }

    public void setSelected(boolean z, boolean z2) {
        this.itemView.setActivated(z);
        this.itemView.setSelected(z);
    }

    public void setEnabled(boolean z) {
        setEnabledRecursive(this.itemView, z);
    }

    @Override
    public boolean onKey(View view, int i, KeyEvent keyEvent) {
        if (getItemDetails() == null) {
            return false;
        }
        return this.mKeyEventListener.onKey(getItemDetails(), i, keyEvent);
    }

    public void addKeyEventListener(KeyboardEventListener keyboardEventListener) {
        this.mKeyEventListener = keyboardEventListener;
    }

    public boolean inDragRegion(MotionEvent motionEvent) {
        return false;
    }

    public boolean inSelectRegion(MotionEvent motionEvent) {
        return false;
    }

    public ItemDetailsLookup.ItemDetails getItemDetails() {
        return this.mDetails;
    }

    static void setEnabledRecursive(View view, boolean z) {
        if (view == null || view.isEnabled() == z) {
            return;
        }
        view.setEnabled(z);
        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            for (int childCount = viewGroup.getChildCount() - 1; childCount >= 0; childCount--) {
                setEnabledRecursive(viewGroup.getChildAt(childCount), z);
            }
        }
    }

    private static <V extends View> V inflateLayout(Context context, ViewGroup viewGroup, int i) {
        return (V) LayoutInflater.from(context).inflate(i, viewGroup, false);
    }

    static ViewPropertyAnimator fade(ImageView imageView, float f) {
        return imageView.animate().setDuration(100L).alpha(f);
    }

    private final class DocumentItemDetails extends ItemDetailsLookup.ItemDetails {
        private DocumentItemDetails() {
        }

        @Override
        public int getPosition() {
            return DocumentHolder.this.getAdapterPosition();
        }

        @Override
        public String getStableId() {
            return DocumentHolder.this.getModelId();
        }

        @Override
        public int getItemViewType() {
            return DocumentHolder.this.getItemViewType();
        }

        @Override
        public boolean inDragRegion(MotionEvent motionEvent) {
            return DocumentHolder.this.inDragRegion(motionEvent);
        }

        @Override
        public boolean inSelectionHotspot(MotionEvent motionEvent) {
            return DocumentHolder.this.inSelectRegion(motionEvent);
        }
    }
}
