package androidx.slice;

import android.app.PendingIntent;
import android.app.RemoteInput;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.graphics.drawable.IconCompat;
import android.support.v4.os.BuildCompat;
import android.support.v4.util.Preconditions;
import androidx.slice.compat.SliceProviderCompat;
import androidx.versionedparcelable.VersionedParcelable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public final class Slice implements VersionedParcelable {
    String[] mHints;
    SliceItem[] mItems;
    SliceSpec mSpec;
    String mUri;

    Slice(ArrayList<SliceItem> items, String[] hints, Uri uri, SliceSpec spec) {
        this.mItems = new SliceItem[0];
        this.mHints = new String[0];
        this.mHints = hints;
        this.mItems = (SliceItem[]) items.toArray(new SliceItem[items.size()]);
        this.mUri = uri.toString();
        this.mSpec = spec;
    }

    public Slice() {
        this.mItems = new SliceItem[0];
        this.mHints = new String[0];
    }

    public Slice(Bundle in) {
        this.mItems = new SliceItem[0];
        this.mHints = new String[0];
        this.mHints = in.getStringArray("hints");
        Parcelable[] items = in.getParcelableArray("items");
        this.mItems = new SliceItem[items.length];
        for (int i = 0; i < this.mItems.length; i++) {
            if (items[i] instanceof Bundle) {
                this.mItems[i] = new SliceItem((Bundle) items[i]);
            }
        }
        this.mUri = in.getParcelable("uri").toString();
        this.mSpec = in.containsKey("type") ? new SliceSpec(in.getString("type"), in.getInt("revision")) : null;
    }

    public Bundle toBundle() {
        Bundle b = new Bundle();
        b.putStringArray("hints", this.mHints);
        Parcelable[] p = new Parcelable[this.mItems.length];
        for (int i = 0; i < this.mItems.length; i++) {
            p[i] = this.mItems[i].toBundle();
        }
        b.putParcelableArray("items", p);
        b.putParcelable("uri", Uri.parse(this.mUri));
        if (this.mSpec != null) {
            b.putString("type", this.mSpec.getType());
            b.putInt("revision", this.mSpec.getRevision());
        }
        return b;
    }

    public SliceSpec getSpec() {
        return this.mSpec;
    }

    public Uri getUri() {
        return Uri.parse(this.mUri);
    }

    public List<SliceItem> getItems() {
        return Arrays.asList(this.mItems);
    }

    public List<String> getHints() {
        return Arrays.asList(this.mHints);
    }

    public boolean hasHint(String hint) {
        return ArrayUtils.contains(this.mHints, hint);
    }

    public static class Builder {
        private SliceSpec mSpec;
        private final Uri mUri;
        private ArrayList<SliceItem> mItems = new ArrayList<>();
        private ArrayList<String> mHints = new ArrayList<>();

        public Builder(Uri uri) {
            this.mUri = uri;
        }

        public Builder(Builder parent) {
            this.mUri = parent.mUri.buildUpon().appendPath("_gen").appendPath(String.valueOf(this.mItems.size())).build();
        }

        public Builder setSpec(SliceSpec spec) {
            this.mSpec = spec;
            return this;
        }

        public Builder addHints(String... hints) {
            this.mHints.addAll(Arrays.asList(hints));
            return this;
        }

        public Builder addHints(List<String> hints) {
            return addHints((String[]) hints.toArray(new String[hints.size()]));
        }

        public Builder addSubSlice(Slice slice) {
            Preconditions.checkNotNull(slice);
            return addSubSlice(slice, null);
        }

        public Builder addSubSlice(Slice slice, String subType) {
            Preconditions.checkNotNull(slice);
            this.mItems.add(new SliceItem(slice, "slice", subType, (String[]) slice.getHints().toArray(new String[slice.getHints().size()])));
            return this;
        }

        public Builder addAction(PendingIntent action, Slice s, String subType) {
            Preconditions.checkNotNull(action);
            Preconditions.checkNotNull(s);
            String[] hints = s != null ? (String[]) s.getHints().toArray(new String[s.getHints().size()]) : new String[0];
            this.mItems.add(new SliceItem(action, s, "action", subType, hints));
            return this;
        }

        public Builder addText(CharSequence text, String subType, String... hints) {
            this.mItems.add(new SliceItem(text, "text", subType, hints));
            return this;
        }

        public Builder addText(CharSequence text, String subType, List<String> hints) {
            return addText(text, subType, (String[]) hints.toArray(new String[hints.size()]));
        }

        public Builder addIcon(IconCompat icon, String subType, String... hints) {
            Preconditions.checkNotNull(icon);
            this.mItems.add(new SliceItem(icon, "image", subType, hints));
            return this;
        }

        public Builder addIcon(IconCompat icon, String subType, List<String> hints) {
            Preconditions.checkNotNull(icon);
            return addIcon(icon, subType, (String[]) hints.toArray(new String[hints.size()]));
        }

        public Builder addRemoteInput(RemoteInput remoteInput, String subType, List<String> hints) {
            Preconditions.checkNotNull(remoteInput);
            return addRemoteInput(remoteInput, subType, (String[]) hints.toArray(new String[hints.size()]));
        }

        public Builder addRemoteInput(RemoteInput remoteInput, String subType, String... hints) {
            Preconditions.checkNotNull(remoteInput);
            this.mItems.add(new SliceItem(remoteInput, "input", subType, hints));
            return this;
        }

        public Builder addInt(int value, String subType, String... hints) {
            this.mItems.add(new SliceItem(Integer.valueOf(value), "int", subType, hints));
            return this;
        }

        public Builder addInt(int value, String subType, List<String> hints) {
            return addInt(value, subType, (String[]) hints.toArray(new String[hints.size()]));
        }

        public Builder addLong(long time, String subType, String... hints) {
            this.mItems.add(new SliceItem(Long.valueOf(time), "long", subType, hints));
            return this;
        }

        public Builder addLong(long time, String subType, List<String> hints) {
            return addLong(time, subType, (String[]) hints.toArray(new String[hints.size()]));
        }

        @Deprecated
        public Builder addTimestamp(long time, String subType, String... hints) {
            this.mItems.add(new SliceItem(Long.valueOf(time), "long", subType, hints));
            return this;
        }

        public Builder addItem(SliceItem item) {
            this.mItems.add(item);
            return this;
        }

        public Slice build() {
            return new Slice(this.mItems, (String[]) this.mHints.toArray(new String[this.mHints.size()]), this.mUri, this.mSpec);
        }
    }

    public String toString() {
        return toString("");
    }

    public String toString(String indent) {
        StringBuilder sb = new StringBuilder();
        sb.append(indent);
        sb.append("slice ");
        addHints(sb, this.mHints);
        sb.append("{\n");
        String nextIndent = indent + "  ";
        for (int i = 0; i < this.mItems.length; i++) {
            SliceItem item = this.mItems[i];
            sb.append(item.toString(nextIndent));
        }
        sb.append(indent);
        sb.append("}");
        return sb.toString();
    }

    public static void addHints(StringBuilder sb, String[] hints) {
        if (hints == null || hints.length == 0) {
            return;
        }
        sb.append("(");
        int end = hints.length - 1;
        for (int i = 0; i < end; i++) {
            sb.append(hints[i]);
            sb.append(", ");
        }
        sb.append(hints[end]);
        sb.append(") ");
    }

    public static Slice bindSlice(Context context, Uri uri, Set<SliceSpec> supportedSpecs) {
        if (BuildCompat.isAtLeastP()) {
            return callBindSlice(context, uri, supportedSpecs);
        }
        return SliceProviderCompat.bindSlice(context, uri, supportedSpecs);
    }

    private static Slice callBindSlice(Context context, Uri uri, Set<SliceSpec> supportedSpecs) {
        return SliceConvert.wrap(((android.app.slice.SliceManager) context.getSystemService(android.app.slice.SliceManager.class)).bindSlice(uri, SliceConvert.unwrap(supportedSpecs)));
    }
}
