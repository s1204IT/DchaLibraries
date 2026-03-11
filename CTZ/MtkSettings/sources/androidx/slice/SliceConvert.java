package androidx.slice;

import android.app.slice.Slice;
import android.support.v4.graphics.drawable.IconCompat;
import android.support.v4.util.ArraySet;
import androidx.slice.Slice;
import java.util.Set;

public class SliceConvert {
    public static android.app.slice.Slice unwrap(Slice slice) {
        if (slice == null || slice.getUri() == null) {
            return null;
        }
        Slice.Builder builder = new Slice.Builder(slice.getUri(), unwrap(slice.getSpec()));
        builder.addHints(slice.getHints());
        for (SliceItem item : slice.getItems()) {
            switch (item.getFormat()) {
                case "slice":
                    builder.addSubSlice(unwrap(item.getSlice()), item.getSubType());
                    break;
                case "image":
                    builder.addIcon(item.getIcon().toIcon(), item.getSubType(), item.getHints());
                    break;
                case "input":
                    builder.addRemoteInput(item.getRemoteInput(), item.getSubType(), item.getHints());
                    break;
                case "action":
                    builder.addAction(item.getAction(), unwrap(item.getSlice()), item.getSubType());
                    break;
                case "text":
                    builder.addText(item.getText(), item.getSubType(), item.getHints());
                    break;
                case "int":
                    builder.addInt(item.getInt(), item.getSubType(), item.getHints());
                    break;
                case "long":
                    builder.addLong(item.getLong(), item.getSubType(), item.getHints());
                    break;
            }
        }
        return builder.build();
    }

    private static android.app.slice.SliceSpec unwrap(SliceSpec spec) {
        if (spec == null) {
            return null;
        }
        return new android.app.slice.SliceSpec(spec.getType(), spec.getRevision());
    }

    static Set<android.app.slice.SliceSpec> unwrap(Set<SliceSpec> supportedSpecs) {
        Set<android.app.slice.SliceSpec> ret = new ArraySet<>();
        if (supportedSpecs != null) {
            for (SliceSpec spec : supportedSpecs) {
                ret.add(unwrap(spec));
            }
        }
        return ret;
    }

    public static Slice wrap(android.app.slice.Slice slice) {
        if (slice == null || slice.getUri() == null) {
            return null;
        }
        Slice.Builder builder = new Slice.Builder(slice.getUri());
        builder.addHints(slice.getHints());
        builder.setSpec(wrap(slice.getSpec()));
        for (android.app.slice.SliceItem item : slice.getItems()) {
            switch (item.getFormat()) {
                case "slice":
                    builder.addSubSlice(wrap(item.getSlice()), item.getSubType());
                    break;
                case "image":
                    builder.addIcon(IconCompat.createFromIcon(item.getIcon()), item.getSubType(), item.getHints());
                    break;
                case "input":
                    builder.addRemoteInput(item.getRemoteInput(), item.getSubType(), item.getHints());
                    break;
                case "action":
                    builder.addAction(item.getAction(), wrap(item.getSlice()), item.getSubType());
                    break;
                case "text":
                    builder.addText(item.getText(), item.getSubType(), item.getHints());
                    break;
                case "int":
                    builder.addInt(item.getInt(), item.getSubType(), item.getHints());
                    break;
                case "long":
                    builder.addLong(item.getLong(), item.getSubType(), item.getHints());
                    break;
            }
        }
        return builder.build();
    }

    private static SliceSpec wrap(android.app.slice.SliceSpec spec) {
        if (spec == null) {
            return null;
        }
        return new SliceSpec(spec.getType(), spec.getRevision());
    }

    public static Set<SliceSpec> wrap(Set<android.app.slice.SliceSpec> supportedSpecs) {
        Set<SliceSpec> ret = new ArraySet<>();
        if (supportedSpecs != null) {
            for (android.app.slice.SliceSpec spec : supportedSpecs) {
                ret.add(wrap(spec));
            }
        }
        return ret;
    }
}
