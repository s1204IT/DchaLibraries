package jp.co.omronsoft.iwnnime.ml.controlpanel;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import com.android.common.speech.LoggingEvents;
import jp.co.omronsoft.iwnnime.ml.R;

public class UserDictionaryToolsActivity extends Activity {
    public static final String DIALOG_CANDIDATE_KEY = "user_dic_candidate";
    public static final String DIALOG_ID_KEY = "user_dic_id";
    public static final String DIALOG_STROKE_KEY = "user_dic_stroke";
    public static final String DIALOG_THEME_KEY = "user_dic_theme";
    public static final String DIALOG_WORD_ID_KEY = "word_id";
    public static final String EDIT_FROM_DIALOG_KEY = "edit_from_dialog";
    public static final String LANGUAGE_TYPE_KEY = "language_type";
    private static boolean mEditing = false;
    private static boolean mEditingFromDialog = false;
    private boolean mTemporaryEditingFromDialog = false;

    private class UserDictionaryToolsDialogFragment extends DialogFragment {
        UserDictionaryToolsDialog mDialog;

        private UserDictionaryToolsDialogFragment() {
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            Intent intent = UserDictionaryToolsActivity.this.getIntent();
            Bundle bundle = intent.getExtras();
            int theme = R.style.UserDictionaryTools;
            int languageType = 0;
            String action = "android.intent.action.INSERT";
            String stroke = LoggingEvents.EXTRA_CALLING_APP_NAME;
            String candidate = LoggingEvents.EXTRA_CALLING_APP_NAME;
            int wordId = -1;
            if (bundle != null) {
                theme = bundle.getInt(UserDictionaryToolsActivity.DIALOG_THEME_KEY, R.style.UserDictionaryTools);
                languageType = bundle.getInt(UserDictionaryToolsActivity.LANGUAGE_TYPE_KEY, 0);
                action = bundle.getString(UserDictionaryToolsActivity.DIALOG_ID_KEY, "android.intent.action.INSERT");
                stroke = bundle.getString(UserDictionaryToolsActivity.DIALOG_STROKE_KEY, LoggingEvents.EXTRA_CALLING_APP_NAME);
                candidate = bundle.getString(UserDictionaryToolsActivity.DIALOG_CANDIDATE_KEY, LoggingEvents.EXTRA_CALLING_APP_NAME);
                wordId = bundle.getInt(UserDictionaryToolsActivity.DIALOG_WORD_ID_KEY, -1);
            }
            this.mDialog = new UserDictionaryToolsDialog(UserDictionaryToolsActivity.this, theme, languageType, action, stroke, candidate, wordId);
            return this.mDialog;
        }

        @Override
        public void onStart() {
            super.onStart();
            this.mDialog.setCursorPosition();
        }

        @Override
        public void onStop() {
            this.mDialog.holdCursorPosition();
            super.onStop();
        }

        @Override
        public void onDismiss(DialogInterface dialog) {
            UserDictionaryToolsActivity.this.finish();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(1);
        mEditing = true;
        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        if (bundle != null) {
            this.mTemporaryEditingFromDialog = bundle.getBoolean(EDIT_FROM_DIALOG_KEY);
        }
        mEditingFromDialog = this.mTemporaryEditingFromDialog;
        FragmentManager manager = getFragmentManager();
        UserDictionaryToolsDialogFragment dialog = new UserDictionaryToolsDialogFragment();
        dialog.show(manager, "dialog");
    }

    @Override
    public void onStart() {
        super.onStart();
        mEditing = true;
        mEditingFromDialog = this.mTemporaryEditingFromDialog;
    }

    @Override
    public void onResume() {
        super.onResume();
        mEditing = true;
    }

    @Override
    public void onPause() {
        super.onPause();
        mEditing = false;
    }

    public static boolean isEditing() {
        return mEditing;
    }

    public static boolean isEditingFromDialog() {
        return mEditingFromDialog;
    }
}
