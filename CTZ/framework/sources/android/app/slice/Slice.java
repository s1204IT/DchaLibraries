package android.app.slice;

import android.app.PendingIntent;
import android.app.RemoteInput;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.Preconditions;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public final class Slice implements Parcelable {
    public static final Parcelable.Creator<Slice> CREATOR = new Parcelable.Creator<Slice>() {
        @Override
        public Slice createFromParcel(Parcel parcel) {
            return new Slice(parcel);
        }

        @Override
        public Slice[] newArray(int i) {
            return new Slice[i];
        }
    };
    public static final String EXTRA_RANGE_VALUE = "android.app.slice.extra.RANGE_VALUE";

    @Deprecated
    public static final String EXTRA_SLIDER_VALUE = "android.app.slice.extra.SLIDER_VALUE";
    public static final String EXTRA_TOGGLE_STATE = "android.app.slice.extra.TOGGLE_STATE";
    public static final String HINT_ACTIONS = "actions";
    public static final String HINT_CALLER_NEEDED = "caller_needed";
    public static final String HINT_ERROR = "error";
    public static final String HINT_HORIZONTAL = "horizontal";
    public static final String HINT_KEYWORDS = "keywords";
    public static final String HINT_LARGE = "large";
    public static final String HINT_LAST_UPDATED = "last_updated";
    public static final String HINT_LIST = "list";
    public static final String HINT_LIST_ITEM = "list_item";
    public static final String HINT_NO_TINT = "no_tint";
    public static final String HINT_PARTIAL = "partial";
    public static final String HINT_PERMISSION_REQUEST = "permission_request";
    public static final String HINT_SEE_MORE = "see_more";
    public static final String HINT_SELECTED = "selected";
    public static final String HINT_SHORTCUT = "shortcut";
    public static final String HINT_SUMMARY = "summary";
    public static final String HINT_TITLE = "title";
    public static final String HINT_TOGGLE = "toggle";
    public static final String HINT_TTL = "ttl";
    public static final String SUBTYPE_COLOR = "color";
    public static final String SUBTYPE_CONTENT_DESCRIPTION = "content_description";
    public static final String SUBTYPE_LAYOUT_DIRECTION = "layout_direction";
    public static final String SUBTYPE_MAX = "max";
    public static final String SUBTYPE_MESSAGE = "message";
    public static final String SUBTYPE_MILLIS = "millis";
    public static final String SUBTYPE_PRIORITY = "priority";
    public static final String SUBTYPE_RANGE = "range";

    @Deprecated
    public static final String SUBTYPE_SLIDER = "slider";
    public static final String SUBTYPE_SOURCE = "source";
    public static final String SUBTYPE_TOGGLE = "toggle";
    public static final String SUBTYPE_VALUE = "value";
    private final String[] mHints;
    private final SliceItem[] mItems;
    private SliceSpec mSpec;
    private Uri mUri;

    @Retention(RetentionPolicy.SOURCE)
    public @interface SliceHint {
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface SliceSubtype {
    }

    Slice(ArrayList<SliceItem> arrayList, String[] strArr, Uri uri, SliceSpec sliceSpec) {
        this.mHints = strArr;
        this.mItems = (SliceItem[]) arrayList.toArray(new SliceItem[arrayList.size()]);
        this.mUri = uri;
        this.mSpec = sliceSpec;
    }

    protected Slice(Parcel parcel) {
        this.mHints = parcel.readStringArray();
        int i = parcel.readInt();
        this.mItems = new SliceItem[i];
        for (int i2 = 0; i2 < i; i2++) {
            this.mItems[i2] = SliceItem.CREATOR.createFromParcel(parcel);
        }
        this.mUri = Uri.CREATOR.createFromParcel(parcel);
        this.mSpec = (SliceSpec) parcel.readTypedObject(SliceSpec.CREATOR);
    }

    public SliceSpec getSpec() {
        return this.mSpec;
    }

    public Uri getUri() {
        return this.mUri;
    }

    public List<SliceItem> getItems() {
        return Arrays.asList(this.mItems);
    }

    public List<String> getHints() {
        return Arrays.asList(this.mHints);
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeStringArray(this.mHints);
        parcel.writeInt(this.mItems.length);
        for (int i2 = 0; i2 < this.mItems.length; i2++) {
            this.mItems[i2].writeToParcel(parcel, i);
        }
        this.mUri.writeToParcel(parcel, 0);
        parcel.writeTypedObject(this.mSpec, i);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public boolean hasHint(String str) {
        return ArrayUtils.contains(this.mHints, str);
    }

    public boolean isCallerNeeded() {
        return hasHint(HINT_CALLER_NEEDED);
    }

    public static class Builder {
        private SliceSpec mSpec;
        private final Uri mUri;
        private ArrayList<SliceItem> mItems = new ArrayList<>();
        private ArrayList<String> mHints = new ArrayList<>();

        @Deprecated
        public Builder(Uri uri) {
            this.mUri = uri;
        }

        public Builder(Uri uri, SliceSpec sliceSpec) {
            this.mUri = uri;
            this.mSpec = sliceSpec;
        }

        public Builder(Builder builder) {
            this.mUri = builder.mUri.buildUpon().appendPath("_gen").appendPath(String.valueOf(this.mItems.size())).build();
        }

        public Builder setCallerNeeded(boolean z) {
            if (z) {
                this.mHints.add(Slice.HINT_CALLER_NEEDED);
            } else {
                this.mHints.remove(Slice.HINT_CALLER_NEEDED);
            }
            return this;
        }

        public Builder addHints(List<String> list) {
            this.mHints.addAll(list);
            return this;
        }

        public Builder setSpec(SliceSpec sliceSpec) {
            this.mSpec = sliceSpec;
            return this;
        }

        public Builder addSubSlice(Slice slice, String str) {
            Preconditions.checkNotNull(slice);
            this.mItems.add(new SliceItem(slice, "slice", str, (String[]) slice.getHints().toArray(new String[slice.getHints().size()])));
            return this;
        }

        public Builder addAction(PendingIntent pendingIntent, Slice slice, String str) {
            Preconditions.checkNotNull(pendingIntent);
            Preconditions.checkNotNull(slice);
            List<String> hints = slice.getHints();
            slice.mSpec = null;
            this.mItems.add(new SliceItem(pendingIntent, slice, "action", str, (String[]) hints.toArray(new String[hints.size()])));
            return this;
        }

        public Builder addText(CharSequence charSequence, String str, List<String> list) {
            this.mItems.add(new SliceItem(charSequence, "text", str, list));
            return this;
        }

        public Builder addIcon(Icon icon, String str, List<String> list) {
            Preconditions.checkNotNull(icon);
            this.mItems.add(new SliceItem(icon, SliceItem.FORMAT_IMAGE, str, list));
            return this;
        }

        public Builder addRemoteInput(RemoteInput remoteInput, String str, List<String> list) {
            Preconditions.checkNotNull(remoteInput);
            this.mItems.add(new SliceItem(remoteInput, "input", str, list));
            return this;
        }

        public Builder addInt(int i, String str, List<String> list) {
            this.mItems.add(new SliceItem(Integer.valueOf(i), SliceItem.FORMAT_INT, str, list));
            return this;
        }

        @Deprecated
        public Builder addTimestamp(long j, String str, List<String> list) {
            return addLong(j, str, list);
        }

        public Builder addLong(long j, String str, List<String> list) {
            this.mItems.add(new SliceItem(Long.valueOf(j), "long", str, (String[]) list.toArray(new String[list.size()])));
            return this;
        }

        public Builder addBundle(Bundle bundle, String str, List<String> list) {
            Preconditions.checkNotNull(bundle);
            this.mItems.add(new SliceItem(bundle, SliceItem.FORMAT_BUNDLE, str, list));
            return this;
        }

        public Slice build() {
            return new Slice(this.mItems, (String[]) this.mHints.toArray(new String[this.mHints.size()]), this.mUri, this.mSpec);
        }
    }

    public String toString() {
        return toString("");
    }

    private String toString(String str) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < this.mItems.length; i++) {
            sb.append(str);
            if (Objects.equals(this.mItems[i].getFormat(), "slice")) {
                sb.append("slice:\n");
                sb.append(this.mItems[i].getSlice().toString(str + "   "));
            } else if (Objects.equals(this.mItems[i].getFormat(), "text")) {
                sb.append("text: ");
                sb.append(this.mItems[i].getText());
                sb.append("\n");
            } else {
                sb.append(this.mItems[i].getFormat());
                sb.append("\n");
            }
        }
        return sb.toString();
    }
}
