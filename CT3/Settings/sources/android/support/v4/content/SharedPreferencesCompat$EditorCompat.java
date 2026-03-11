package android.support.v4.content;

import android.content.SharedPreferences;
import android.os.Build;
import android.support.annotation.NonNull;

public final class SharedPreferencesCompat$EditorCompat {
    private static SharedPreferencesCompat$EditorCompat sInstance;
    private final Helper mHelper;

    private interface Helper {
        void apply(@NonNull SharedPreferences.Editor editor);
    }

    private static class EditorHelperBaseImpl implements Helper {
        EditorHelperBaseImpl(EditorHelperBaseImpl editorHelperBaseImpl) {
            this();
        }

        private EditorHelperBaseImpl() {
        }

        @Override
        public void apply(@NonNull SharedPreferences.Editor editor) {
            editor.commit();
        }
    }

    private static class EditorHelperApi9Impl implements Helper {
        EditorHelperApi9Impl(EditorHelperApi9Impl editorHelperApi9Impl) {
            this();
        }

        private EditorHelperApi9Impl() {
        }

        @Override
        public void apply(@NonNull SharedPreferences.Editor editor) {
            EditorCompatGingerbread.apply(editor);
        }
    }

    private SharedPreferencesCompat$EditorCompat() {
        EditorHelperApi9Impl editorHelperApi9Impl = null;
        Object[] objArr = 0;
        if (Build.VERSION.SDK_INT >= 9) {
            this.mHelper = new EditorHelperApi9Impl(editorHelperApi9Impl);
        } else {
            this.mHelper = new EditorHelperBaseImpl(objArr == true ? 1 : 0);
        }
    }

    public static SharedPreferencesCompat$EditorCompat getInstance() {
        if (sInstance == null) {
            sInstance = new SharedPreferencesCompat$EditorCompat();
        }
        return sInstance;
    }

    public void apply(@NonNull SharedPreferences.Editor editor) {
        this.mHelper.apply(editor);
    }
}
