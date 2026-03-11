package androidx.car.widget;

import android.car.drivingstate.CarUxRestrictions;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import androidx.car.R;
import androidx.car.widget.ListItem.ViewHolder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public abstract class ListItem<VH extends ViewHolder> {
    private boolean mDirty;
    private boolean mHideDivider;
    private final List<ViewBinder<VH>> mCustomBinders = new ArrayList();
    private final List<ViewBinder<VH>> mCustomBinderCleanUps = new ArrayList();
    private int mTitleTextAppearance = R.style.TextAppearance_Car_Body1;
    private int mBodyTextAppearance = R.style.TextAppearance_Car_Body2;

    public interface ViewBinder<VH> {
        void bind(VH vh);
    }

    public abstract int getViewType();

    protected abstract void onBind(VH vh);

    protected abstract void resolveDirtyState();

    final void bind(VH viewHolder) {
        viewHolder.cleanUp();
        Iterator<ViewBinder<VH>> it = this.mCustomBinderCleanUps.iterator();
        while (it.hasNext()) {
            viewHolder.addCleanUp(it.next());
        }
        if (isDirty()) {
            resolveDirtyState();
            markClean();
        }
        onBind(viewHolder);
        for (ViewBinder<VH> binder : this.mCustomBinders) {
            binder.bind(viewHolder);
        }
    }

    void setTitleTextAppearance(int titleTextAppearance) {
        this.mTitleTextAppearance = titleTextAppearance;
    }

    void setBodyTextAppearance(int bodyTextAppearance) {
        this.mBodyTextAppearance = bodyTextAppearance;
    }

    final int getTitleTextAppearance() {
        return this.mTitleTextAppearance;
    }

    final int getBodyTextAppearance() {
        return this.mBodyTextAppearance;
    }

    protected void markDirty() {
        this.mDirty = true;
    }

    protected void markClean() {
        this.mDirty = false;
    }

    protected boolean isDirty() {
        return this.mDirty;
    }

    public boolean shouldHideDivider() {
        return this.mHideDivider;
    }

    public static abstract class ViewHolder extends RecyclerView.ViewHolder {
        private final List<ViewBinder> mCleanUps;

        protected abstract void applyUxRestrictions(CarUxRestrictions carUxRestrictions);

        public ViewHolder(View itemView) {
            super(itemView);
            this.mCleanUps = new ArrayList();
        }

        public final void cleanUp() {
            for (ViewBinder binder : this.mCleanUps) {
                binder.bind(this);
            }
        }

        public final void addCleanUp(ViewBinder<ViewHolder> cleanUp) {
            if (cleanUp != null) {
                this.mCleanUps.add(cleanUp);
            }
        }
    }
}
