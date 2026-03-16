package android.provider;

import android.content.Context;

public class SearchIndexableResource extends SearchIndexableData {
    public int xmlResId;

    public SearchIndexableResource(int rank, int xmlResId, String className, int iconResId) {
        this.rank = rank;
        this.xmlResId = xmlResId;
        this.className = className;
        this.iconResId = iconResId;
    }

    public SearchIndexableResource(Context context) {
        super(context);
    }

    @Override
    public String toString() {
        return "SearchIndexableResource[" + super.toString() + ", xmlResId: " + this.xmlResId + "]";
    }
}
