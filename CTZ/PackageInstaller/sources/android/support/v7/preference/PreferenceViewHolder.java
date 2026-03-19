package android.support.v7.preference;

import android.support.v7.widget.RecyclerView;
import android.util.SparseArray;
import android.view.View;

public class PreferenceViewHolder extends RecyclerView.ViewHolder {
    private final SparseArray<View> mCachedViews;
    private boolean mDividerAllowedAbove;
    private boolean mDividerAllowedBelow;

    PreferenceViewHolder(View itemView) {
        super(itemView);
        this.mCachedViews = new SparseArray<>(4);
        this.mCachedViews.put(android.R.id.title, itemView.findViewById(android.R.id.title));
        this.mCachedViews.put(android.R.id.summary, itemView.findViewById(android.R.id.summary));
        this.mCachedViews.put(android.R.id.icon, itemView.findViewById(android.R.id.icon));
        this.mCachedViews.put(R.id.icon_frame, itemView.findViewById(R.id.icon_frame));
        this.mCachedViews.put(android.R.id.icon_frame, itemView.findViewById(android.R.id.icon_frame));
    }

    public static PreferenceViewHolder createInstanceForTests(View itemView) {
        return new PreferenceViewHolder(itemView);
    }

    public View findViewById(int id) {
        View cachedView = this.mCachedViews.get(id);
        if (cachedView != null) {
            return cachedView;
        }
        View v = this.itemView.findViewById(id);
        if (v != null) {
            this.mCachedViews.put(id, v);
        }
        return v;
    }

    public boolean isDividerAllowedAbove() {
        return this.mDividerAllowedAbove;
    }

    public void setDividerAllowedAbove(boolean allowed) {
        this.mDividerAllowedAbove = allowed;
    }

    public boolean isDividerAllowedBelow() {
        return this.mDividerAllowedBelow;
    }

    public void setDividerAllowedBelow(boolean allowed) {
        this.mDividerAllowedBelow = allowed;
    }
}
