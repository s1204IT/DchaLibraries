package jp.co.omronsoft.iwnnime.ml.controlpanel;

import android.app.Dialog;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.android.common.speech.LoggingEvents;
import jp.co.omronsoft.iwnnime.ml.IWnnIME;
import jp.co.omronsoft.iwnnime.ml.R;
import jp.co.omronsoft.iwnnime.ml.WnnWord;
import jp.co.omronsoft.iwnnime.ml.iwnn.IWnnUserDictionaryToolsEngineInterface;
import jp.co.omronsoft.iwnnime.ml.iwnn.iWnnEngine;
import jp.co.omronsoft.iwnnime.ml.standardcommon.LanguageManager;

public class UserDictionaryToolsDialog extends Dialog implements View.OnClickListener {
    private static final String DIALOG_TITLE_ICON = "📖";
    private static final int MAX_TEXT_SIZE = 50;
    private static final int RETURN_SAME_WORD = -11;
    private static final int RETURN_USER_DICTIONARY_FULL = -12;
    private static final int STATE_EDIT = 2;
    private static final int STATE_INSERT = 1;
    private static final int STATE_UNKNOWN = 0;
    private static boolean mListUpdated = false;
    private String mAction;
    private WnnWord mBeforeEditWnnWord;
    private Button mCancelButton;
    private String mCandidate;
    private EditText mCandidateEditText;
    private EditText mCurrentEditText;
    protected IWnnUserDictionaryToolsEngineInterface mEngineInterface;
    private Button mEntryButton;
    private TextView mIcon;
    protected boolean mIsNoStroke;
    private int mLanguageType;
    private InputFilter[] mNonSupportCharactersFilter;
    private EditText mReadEditText;
    private int mRequestState;
    private String mStroke;
    private TextView mSubTitle;
    private TextView mTitle;
    private int mWordId;

    public UserDictionaryToolsDialog(Context context, int theme, int languageType, String action, String stroke, String candidate, int wordId) {
        super(context, theme);
        this.mIsNoStroke = false;
        this.mLanguageType = 0;
        this.mAction = "android.intent.action.INSERT";
        this.mStroke = LoggingEvents.EXTRA_CALLING_APP_NAME;
        this.mCandidate = LoggingEvents.EXTRA_CALLING_APP_NAME;
        this.mWordId = -1;
        this.mNonSupportCharactersFilter = new InputFilter[]{new EditFilterCandidate(), new InputFilter.LengthFilter(50)};
        this.mLanguageType = languageType;
        this.mAction = action;
        this.mStroke = stroke;
        this.mCandidate = candidate;
        this.mWordId = wordId;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        int dialogWidth;
        super.onCreate(savedInstanceState);
        Resources res = getContext().getResources();
        if (UserDictionaryToolsActivity.isEditingFromDialog()) {
            this.mEngineInterface = IWnnUserDictionaryToolsEngineInterface.getEngineInterface(this.mLanguageType, hashCode());
        } else {
            this.mEngineInterface = IWnnUserDictionaryToolsEngineInterface.getEngineInterface();
            this.mLanguageType = this.mEngineInterface.getLanguage();
        }
        if (LanguageManager.isNoStroke(this.mLanguageType)) {
            this.mIsNoStroke = true;
        }
        this.mEngineInterface.setDirPath(IWnnIME.getFilesDirPath(getOwnerActivity()));
        DisplayMetrics metrics = res.getDisplayMetrics();
        if (res.getConfiguration().orientation == 1) {
            dialogWidth = (int) (((double) metrics.widthPixels) * 0.9d);
        } else {
            dialogWidth = (int) (((double) metrics.heightPixels) * 0.9d);
        }
        setContentView(R.layout.user_dictionary_tools_edit);
        WindowManager.LayoutParams layout = getWindow().getAttributes();
        layout.width = dialogWidth;
        layout.gravity = 48;
        getWindow().setAttributes(layout);
        this.mIcon = (TextView) findViewById(R.id.dialogIcon);
        this.mTitle = (TextView) findViewById(R.id.dialogTitle);
        this.mSubTitle = (TextView) findViewById(R.id.dialogSubTitle);
        this.mEntryButton = (Button) findViewById(R.id.addButton);
        this.mCancelButton = (Button) findViewById(R.id.cancelButton);
        this.mReadEditText = (EditText) findViewById(R.id.editRead);
        this.mCandidateEditText = (EditText) findViewById(R.id.editCandidate);
        this.mReadEditText.setFilters(this.mNonSupportCharactersFilter);
        this.mCandidateEditText.setFilters(this.mNonSupportCharactersFilter);
        if (this.mIsNoStroke) {
            this.mReadEditText.setVisibility(8);
            findViewById(R.id.labelRead).setVisibility(8);
        }
        this.mEntryButton.setOnClickListener(this);
        this.mCancelButton.setOnClickListener(this);
        this.mRequestState = 0;
        this.mReadEditText.setSingleLine();
        this.mCandidateEditText.setSingleLine();
        setAddButtonControl();
        if ("android.intent.action.EDIT".equals(this.mAction)) {
            this.mEntryButton.setEnabled(true);
            this.mReadEditText.setText(this.mStroke);
            this.mCandidateEditText.setText(this.mCandidate);
            this.mRequestState = 2;
            this.mBeforeEditWnnWord = new WnnWord();
            this.mBeforeEditWnnWord.stroke = this.mStroke;
            this.mBeforeEditWnnWord.candidate = this.mCandidate;
            this.mBeforeEditWnnWord.id = this.mWordId;
        } else {
            this.mEntryButton.setEnabled(false);
            this.mReadEditText.setText(LoggingEvents.EXTRA_CALLING_APP_NAME);
            this.mCandidateEditText.setText(LoggingEvents.EXTRA_CALLING_APP_NAME);
            this.mRequestState = 1;
        }
        setDialogTitle();
    }

    public void setAddButtonControl() {
        this.mReadEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (UserDictionaryToolsDialog.this.mReadEditText.getText().toString().length() == 0 || UserDictionaryToolsDialog.this.mCandidateEditText.getText().toString().length() == 0) {
                    UserDictionaryToolsDialog.this.mEntryButton.setEnabled(false);
                } else {
                    UserDictionaryToolsDialog.this.mEntryButton.setEnabled(true);
                }
            }
        });
        this.mCandidateEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if ((UserDictionaryToolsDialog.this.mIsNoStroke || UserDictionaryToolsDialog.this.mReadEditText.getText().toString().length() != 0) && UserDictionaryToolsDialog.this.mCandidateEditText.getText().toString().length() != 0) {
                    UserDictionaryToolsDialog.this.mEntryButton.setEnabled(true);
                } else {
                    UserDictionaryToolsDialog.this.mEntryButton.setEnabled(false);
                }
            }
        });
    }

    public class EditFilterCandidate implements InputFilter {
        public EditFilterCandidate() {
        }

        @Override
        public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
            if (source != null) {
                String word = source.toString();
                if (iWnnEngine.getEngine().hasNonSupportCharacters(word)) {
                    Toast.makeText(UserDictionaryToolsDialog.this.getContext().getApplicationContext(), R.string.ti_string_output_cancel_message_txt, 0).show();
                    return LoggingEvents.EXTRA_CALLING_APP_NAME;
                }
                return source;
            }
            return source;
        }
    }

    @Override
    public void onClick(View v) {
        this.mEntryButton.setEnabled(false);
        this.mCancelButton.setEnabled(false);
        switch (v.getId()) {
            case R.id.cancelButton:
                doRevertAction();
                break;
            case R.id.addButton:
                doSaveAction();
                break;
            default:
                Log.e("IWnnIME", "onClick: Get Invalid ButtonID. ID=" + v.getId());
                break;
        }
    }

    private void doSaveAction() {
        switch (this.mRequestState) {
            case 1:
                if ((this.mIsNoStroke || inputDataCheck(this.mReadEditText)) && inputDataCheck(this.mCandidateEditText)) {
                    String candidate = this.mCandidateEditText.getText().toString();
                    String stroke = this.mIsNoStroke ? candidate : this.mReadEditText.getText().toString();
                    if (addToDictionary(stroke, candidate)) {
                        mListUpdated = true;
                        dismiss();
                    }
                }
                break;
            case 2:
                if ((this.mIsNoStroke || inputDataCheck(this.mReadEditText)) && inputDataCheck(this.mCandidateEditText)) {
                    deleteDictionary(this.mBeforeEditWnnWord);
                    String candidate2 = this.mCandidateEditText.getText().toString();
                    String stroke2 = this.mIsNoStroke ? candidate2 : this.mReadEditText.getText().toString();
                    if (addToDictionary(stroke2, candidate2)) {
                        mListUpdated = true;
                        dismiss();
                    } else {
                        addToDictionary(this.mBeforeEditWnnWord.stroke, this.mBeforeEditWnnWord.candidate);
                    }
                }
                break;
            default:
                Log.e("IWnnIME", "doSaveAction: Invalid Add Status. Status=" + this.mRequestState);
                break;
        }
    }

    private void doRevertAction() {
        dismiss();
    }

    private boolean addToDictionary(String stroke, String candidate) {
        int ret = this.mEngineInterface.addWord(new WnnWord(candidate, stroke));
        if (ret < 0) {
            switch (ret) {
                case -12:
                    Toast.makeText(getContext(), R.string.ti_preference_max_registered_error_txt, 0).show();
                    break;
                case -11:
                    Toast.makeText(getContext(), R.string.ti_user_dictionary_words_duplication_message_txt, 0).show();
                    break;
            }
            this.mEntryButton.setEnabled(true);
            this.mCancelButton.setEnabled(true);
        }
        return ret >= 0;
    }

    private void deleteDictionary(WnnWord word) {
        boolean deleted = this.mEngineInterface.deleteWord(word);
        if (!deleted) {
            Toast.makeText(getContext(), R.string.ti_user_dictionary_delete_fail_txt, 0).show();
        }
    }

    private boolean inputDataCheck(View v) {
        if (!(v instanceof TextView) || ((TextView) v).getText().length() <= 50) {
            return true;
        }
        Toast.makeText(getContext(), R.string.ti_user_dictionary_over_max_text_size_message_txt, 0).show();
        this.mEntryButton.setEnabled(true);
        this.mCancelButton.setEnabled(true);
        Log.e("IWnnIME", "inputDataCheck() : over max string length.");
        return false;
    }

    private void setDialogTitle() {
        String title;
        Context context = getContext();
        int nameId = LanguageManager.getUserDictionaryTitle(this.mLanguageType);
        String subtitle = context.getString(nameId);
        if ("android.intent.action.EDIT".equals(this.mAction)) {
            title = context.getString(R.string.ti_user_dictionary_edit_words_txt);
        } else {
            title = context.getString(R.string.ti_user_dictionary_add_words_txt);
        }
        this.mIcon.setText(DIALOG_TITLE_ICON);
        this.mTitle.setText(title);
        this.mSubTitle.setText(subtitle);
    }

    public static boolean isListUpdated() {
        boolean update = mListUpdated;
        mListUpdated = false;
        return update;
    }

    public void holdCursorPosition() {
        if (this.mReadEditText.isFocused()) {
            this.mCurrentEditText = this.mReadEditText;
        } else {
            this.mCurrentEditText = this.mCandidateEditText;
        }
    }

    public void setCursorPosition() {
        if (this.mCurrentEditText != null) {
            int start = this.mCurrentEditText.getSelectionStart();
            int end = this.mCurrentEditText.getSelectionEnd();
            if (start >= 0 && end >= 0) {
                this.mCurrentEditText.requestFocus();
                this.mCurrentEditText.setSelection(start, end);
            }
        }
    }
}
