package androidx.slice;

import android.app.PendingIntent;
import android.app.RemoteInput;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.graphics.drawable.IconCompat;
import android.support.v4.util.Pair;
import androidx.versionedparcelable.CustomVersionedParcelable;
import java.util.Arrays;
import java.util.List;

public final class SliceItem extends CustomVersionedParcelable {
    String mFormat;
    protected String[] mHints;
    Object mObj;
    String mSubType;

    public interface ActionHandler {
        void onAction(SliceItem sliceItem, Context context, Intent intent);
    }

    public SliceItem(Object obj, String format, String subType, String[] hints) {
        this.mHints = new String[0];
        this.mHints = hints;
        this.mFormat = format;
        this.mSubType = subType;
        this.mObj = obj;
    }

    public SliceItem(Object obj, String format, String subType, List<String> hints) {
        this(obj, format, subType, (String[]) hints.toArray(new String[hints.size()]));
    }

    public SliceItem() {
        this.mHints = new String[0];
    }

    public SliceItem(PendingIntent intent, Slice slice, String format, String subType, String[] hints) {
        this(new Pair(intent, slice), format, subType, hints);
    }

    public List<String> getHints() {
        return Arrays.asList(this.mHints);
    }

    public void addHint(String hint) {
        this.mHints = (String[]) ArrayUtils.appendElement(String.class, this.mHints, hint);
    }

    public String getFormat() {
        return this.mFormat;
    }

    public String getSubType() {
        return this.mSubType;
    }

    public CharSequence getText() {
        return (CharSequence) this.mObj;
    }

    public IconCompat getIcon() {
        return (IconCompat) this.mObj;
    }

    public PendingIntent getAction() {
        return (PendingIntent) ((Pair) this.mObj).first;
    }

    public void fireAction(Context context, Intent i) throws PendingIntent.CanceledException {
        Object action = ((Pair) this.mObj).first;
        if (action instanceof PendingIntent) {
            ((PendingIntent) action).send(context, 0, i, null, null);
        } else {
            ((ActionHandler) action).onAction(this, context, i);
        }
    }

    public RemoteInput getRemoteInput() {
        return (RemoteInput) this.mObj;
    }

    public int getInt() {
        return ((Integer) this.mObj).intValue();
    }

    public Slice getSlice() {
        if ("action".equals(getFormat())) {
            return (Slice) ((Pair) this.mObj).second;
        }
        return (Slice) this.mObj;
    }

    public long getLong() {
        return ((Long) this.mObj).longValue();
    }

    @Deprecated
    public long getTimestamp() {
        return ((Long) this.mObj).longValue();
    }

    public boolean hasHint(String hint) {
        return ArrayUtils.contains(this.mHints, hint);
    }

    public SliceItem(Bundle in) {
        this.mHints = new String[0];
        this.mHints = in.getStringArray("hints");
        this.mFormat = in.getString("format");
        this.mSubType = in.getString("subtype");
        this.mObj = readObj(this.mFormat, in);
    }

    public Bundle toBundle() {
        Bundle b = new Bundle();
        b.putStringArray("hints", this.mHints);
        b.putString("format", this.mFormat);
        b.putString("subtype", this.mSubType);
        writeObj(b, this.mObj, this.mFormat);
        return b;
    }

    public boolean hasAnyHints(String... hints) {
        if (hints == null) {
            return false;
        }
        for (String hint : hints) {
            if (ArrayUtils.contains(this.mHints, hint)) {
                return true;
            }
        }
        return false;
    }

    private void writeObj(Bundle dest, Object obj, String type) {
        switch (type) {
            case "image":
                dest.putBundle("obj", ((IconCompat) obj).toBundle());
                break;
            case "input":
                dest.putParcelable("obj", (Parcelable) obj);
                break;
            case "slice":
                dest.putParcelable("obj", ((Slice) obj).toBundle());
                break;
            case "action":
                dest.putParcelable("obj", (PendingIntent) ((Pair) obj).first);
                dest.putBundle("obj_2", ((Slice) ((Pair) obj).second).toBundle());
                break;
            case "text":
                dest.putCharSequence("obj", (CharSequence) obj);
                break;
            case "int":
                dest.putInt("obj", ((Integer) this.mObj).intValue());
                break;
            case "long":
                dest.putLong("obj", ((Long) this.mObj).longValue());
                break;
        }
    }

    private static Object readObj(String type, Bundle in) {
        switch (type) {
            case "image":
                return IconCompat.createFromBundle(in.getBundle("obj"));
            case "input":
                return in.getParcelable("obj");
            case "slice":
                return new Slice(in.getBundle("obj"));
            case "text":
                return in.getCharSequence("obj");
            case "action":
                return new Pair(in.getParcelable("obj"), new Slice(in.getBundle("obj_2")));
            case "int":
                return Integer.valueOf(in.getInt("obj"));
            case "long":
                return Long.valueOf(in.getLong("obj"));
            default:
                throw new RuntimeException("Unsupported type " + type);
        }
    }

    public static String typeToString(String format) {
        switch (format) {
            case "slice":
                return "Slice";
            case "text":
                return "Text";
            case "image":
                return "Image";
            case "action":
                return "Action";
            case "int":
                return "Int";
            case "long":
                return "Long";
            case "input":
                return "RemoteInput";
            default:
                return "Unrecognized format: " + format;
        }
    }

    public String toString() {
        return toString("");
    }

    public String toString(String indent) {
        StringBuilder sb;
        sb = new StringBuilder();
        switch (getFormat()) {
            case "slice":
                sb.append(getSlice().toString(indent));
                break;
            case "action":
                sb.append(indent);
                sb.append(getAction());
                sb.append(",\n");
                sb.append(getSlice().toString(indent));
                break;
            case "text":
                sb.append(indent);
                sb.append('\"');
                sb.append(getText());
                sb.append('\"');
                break;
            case "image":
                sb.append(indent);
                sb.append(getIcon());
                break;
            case "int":
                sb.append(indent);
                sb.append(getInt());
                break;
            case "long":
                sb.append(indent);
                sb.append(getLong());
                break;
            default:
                sb.append(indent);
                sb.append(typeToString(getFormat()));
                break;
        }
        if (!"slice".equals(getFormat())) {
            sb.append(' ');
            Slice.addHints(sb, this.mHints);
        }
        sb.append(",\n");
        return sb.toString();
    }
}
