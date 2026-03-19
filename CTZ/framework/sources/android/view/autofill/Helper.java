package android.view.autofill;

import java.util.Collection;

public final class Helper {
    public static boolean sDebug = false;
    public static boolean sVerbose = false;

    public static void appendRedacted(StringBuilder sb, CharSequence charSequence) {
        sb.append(getRedacted(charSequence));
    }

    public static String getRedacted(CharSequence charSequence) {
        if (charSequence == null) {
            return "null";
        }
        return charSequence.length() + "_chars";
    }

    public static void appendRedacted(StringBuilder sb, String[] strArr) {
        if (strArr == null) {
            sb.append("N/A");
            return;
        }
        sb.append("[");
        for (String str : strArr) {
            sb.append(" '");
            appendRedacted(sb, str);
            sb.append("'");
        }
        sb.append(" ]");
    }

    public static AutofillId[] toArray(Collection<AutofillId> collection) {
        if (collection == null) {
            return new AutofillId[0];
        }
        AutofillId[] autofillIdArr = new AutofillId[collection.size()];
        collection.toArray(autofillIdArr);
        return autofillIdArr;
    }

    private Helper() {
        throw new UnsupportedOperationException("contains static members only");
    }
}
