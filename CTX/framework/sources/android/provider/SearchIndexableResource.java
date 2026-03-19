package android.provider;

import android.annotation.SystemApi;
import android.content.Context;

@SystemApi
public class SearchIndexableResource extends SearchIndexableData {
    public int xmlResId;

    public SearchIndexableResource(int i, int i2, String str, int i3) {
        this.rank = i;
        this.xmlResId = i2;
        this.className = str;
        this.iconResId = i3;
    }

    public SearchIndexableResource(Context context) {
        super(context);
    }

    @Override
    public String toString() {
        return "SearchIndexableResource[" + super.toString() + ", xmlResId: " + this.xmlResId + "]";
    }
}
