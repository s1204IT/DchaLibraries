package libcore.io;

import android.icu.text.PluralRules;
import java.util.Base64;

public final class DropBox {
    private static volatile Reporter REPORTER = new DefaultReporter();

    public interface Reporter {
        void addData(String str, byte[] bArr, int i);

        void addText(String str, String str2);
    }

    public static void setReporter(Reporter reporter) {
        if (reporter == null) {
            throw new NullPointerException("reporter == null");
        }
        REPORTER = reporter;
    }

    public static Reporter getReporter() {
        return REPORTER;
    }

    private static final class DefaultReporter implements Reporter {
        private DefaultReporter() {
        }

        @Override
        public void addData(String str, byte[] bArr, int i) {
            System.out.println(str + PluralRules.KEYWORD_RULE_SEPARATOR + Base64.getEncoder().encodeToString(bArr));
        }

        @Override
        public void addText(String str, String str2) {
            System.out.println(str + PluralRules.KEYWORD_RULE_SEPARATOR + str2);
        }
    }

    public static void addData(String str, byte[] bArr, int i) {
        getReporter().addData(str, bArr, i);
    }

    public static void addText(String str, String str2) {
        getReporter().addText(str, str2);
    }
}
