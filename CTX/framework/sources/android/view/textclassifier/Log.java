package android.view.textclassifier;

import android.util.Slog;

final class Log {
    private static final boolean ENABLE_FULL_LOGGING = false;

    private Log() {
    }

    public static void d(String str, String str2) {
        Slog.d(str, str2);
    }

    public static void w(String str, String str2) {
        Slog.w(str, str2);
    }

    public static void e(String str, String str2, Throwable th) {
        Slog.d(str, String.format("%s (%s)", str2, th != null ? th.getClass().getSimpleName() : "??"));
    }
}
