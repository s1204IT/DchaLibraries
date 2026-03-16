package jp.co.omronsoft.iwnnime.ml.controlpanel;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Paint;
import android.os.Bundle;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;
import com.android.common.speech.LoggingEvents;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import jp.co.omronsoft.iwnnime.ml.IWnnIME;
import jp.co.omronsoft.iwnnime.ml.IWnnImeEvent;
import jp.co.omronsoft.iwnnime.ml.KeyboardLanguagePackData;
import jp.co.omronsoft.iwnnime.ml.R;
import jp.co.omronsoft.iwnnime.ml.WnnWord;
import jp.co.omronsoft.iwnnime.ml.cyrillic.ListComparatorCyrillicAlphabet;
import jp.co.omronsoft.iwnnime.ml.hangul.ListComparatorHangul;
import jp.co.omronsoft.iwnnime.ml.iwnn.IWnnUserDictionaryToolsEngineInterface;
import jp.co.omronsoft.iwnnime.ml.jajp.ListComparatorEn;
import jp.co.omronsoft.iwnnime.ml.jajp.ListComparatorJaJp;
import jp.co.omronsoft.iwnnime.ml.latin.ListComparatorLatin;
import jp.co.omronsoft.iwnnime.ml.standardcommon.LanguageManager;
import jp.co.omronsoft.iwnnime.ml.zh.ListComparatorZh;

public class UserDictionaryToolsList extends Activity implements View.OnClickListener, View.OnTouchListener, View.OnFocusChangeListener {
    private static final int DIALOG_CONTROL_DELETE_CONFIRM = 0;
    private static final int DIALOG_CONTROL_INIT_CONFIRM = 1;
    private static final String DIALOG_FRAGMENT_TAG = "user_dic_list_dialog";
    private static final int DOUBLE_TAP_TIME = 300;
    private static final int FOCUS_BACKGROUND_COLOR = -16738680;
    private static final int MAX_LIST_WORD_COUNT = 50;
    public static final int MAX_WORD_COUNT = 500;
    private static final int MENU_ITEM_ADD = 0;
    private static final int MENU_ITEM_DELETE = 2;
    private static final int MENU_ITEM_EDIT = 1;
    private static final int MENU_ITEM_INIT = 3;
    private static final int MIN_WORD_COUNT = 0;
    private static final int WORD_TEXT_SIZE = 16;
    private boolean mAddMenuEnabled;
    private boolean mDeleteMenuEnabled;
    private boolean mEditMenuEnabled;
    protected IWnnUserDictionaryToolsEngineInterface mEngineInterface;
    private boolean mInitMenuEnabled;
    private Menu mMenu;
    private boolean mSelectedWords;
    private WnnWord[] mSortData;
    private TableLayout mTableLayout;
    private static boolean started = false;
    private static int sBeforeSelectedViewID = -1;
    private static long sJustBeforeActionTime = -1;
    private static int mLanguage = -1;
    private UserDictionaryToolsListFocus mFocusingView = null;
    private UserDictionaryToolsListFocus mFocusingPairView = null;
    private int mWordCount = 0;
    private boolean mInitializedMenu = false;
    private ArrayList<WnnWord> mWordList = null;
    private boolean mInit = false;
    private Button mLeftButton = null;
    private Button mRightButton = null;
    protected boolean mIsNoStroke = false;
    private UserDictionaryToolsListDialog mDialogShow = null;

    public static class UserDictionaryToolsListDialog extends DialogFragment {
        public static UserDictionaryToolsListDialog newInstance(int id) {
            UserDictionaryToolsListDialog frag = new UserDictionaryToolsListDialog();
            Bundle args = new Bundle();
            args.putInt(UserDictionaryToolsActivity.DIALOG_ID_KEY, id);
            frag.setArguments(args);
            return frag;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            if (IWnnIME.isDebugging()) {
                Log.d("IWnnIME", "onCreateDialog() : start.");
            }
            Activity act = getActivity();
            int id = getArguments().getInt(UserDictionaryToolsActivity.DIALOG_ID_KEY);
            switch (id) {
                case 0:
                    setCancelable(true);
                    break;
                case 1:
                    setCancelable(true);
                    break;
                default:
                    Log.e("IWnnIME", "onCreateDialog : Invaled Get DialogID. ID=" + id);
                    if (IWnnIME.isDebugging()) {
                        Log.d("IWnnIME", "onCreateDialog() : end.");
                    }
                    break;
            }
            return null;
        }
    }

    public void onClickDialogDeleteConfirm() {
        CharSequence focusString = this.mFocusingView.getText();
        WnnWord wnnWordSearch = new WnnWord();
        boolean deleted = false;
        CharSequence focusPairString = this.mFocusingPairView.getText();
        if (this.mFocusingView.getId() > 500) {
            wnnWordSearch.stroke = focusPairString.toString();
            wnnWordSearch.candidate = focusString.toString();
        } else {
            wnnWordSearch.stroke = focusString.toString();
            wnnWordSearch.candidate = focusPairString.toString();
        }
        Bundle bundle = this.mFocusingView.getInputExtras(false);
        if (bundle != null) {
            wnnWordSearch.id = bundle.getInt("wordId", -1);
            deleted = this.mEngineInterface.deleteWord(wnnWordSearch);
        }
        if (deleted) {
            Toast.makeText(getApplicationContext(), R.string.ti_user_dictionary_delete_complete_txt, 0).show();
            this.mWordList = this.mEngineInterface.getWords();
            sortList(this.mWordList);
            int size = this.mWordList.size();
            if (size <= this.mWordCount) {
                int newPos = this.mWordCount - 50;
                if (newPos < 0) {
                    newPos = 0;
                }
                this.mWordCount = newPos;
            }
            updateWordList(true);
            TextView leftText = (TextView) findViewById(R.id.user_dictionary_tools_list_title_words_count);
            leftText.setText(size + "/500");
            if (this.mInitializedMenu) {
                onCreateOptionsMenu(this.mMenu);
                return;
            }
            return;
        }
        Toast.makeText(getApplicationContext(), R.string.ti_user_dictionary_delete_fail_txt, 0).show();
    }

    public void onClickDialogInitConfirm() {
        this.mEngineInterface.initializeDictionary();
        Toast.makeText(getApplicationContext(), R.string.ti_user_dictionary_all_delete_complete_txt, 0).show();
        this.mWordList = new ArrayList<>();
        this.mWordCount = 0;
        updateWordList(true);
        TextView leftText = (TextView) findViewById(R.id.user_dictionary_tools_list_title_words_count);
        leftText.setText(this.mWordList.size() + "/500");
        if (this.mInitializedMenu) {
            onCreateOptionsMenu(this.mMenu);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (IWnnIME.isDebugging()) {
            Log.d("IWnnIME", "onCreate() : start.");
        }
        super.onCreate(savedInstanceState);
        started = true;
        setContentView(R.layout.user_dictionary_tools_list);
        this.mTableLayout = (TableLayout) findViewById(R.id.user_dictionary_tools_table);
        Button b = (Button) findViewById(R.id.user_dictionary_left_button);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int pos = UserDictionaryToolsList.this.mWordCount - 50;
                if (pos >= 0) {
                    UserDictionaryToolsList.this.mWordCount = pos;
                    UserDictionaryToolsList.this.updateWordList(false);
                }
            }
        });
        this.mLeftButton = b;
        Button b2 = (Button) findViewById(R.id.user_dictionary_right_button);
        b2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int pos = UserDictionaryToolsList.this.mWordCount + 50;
                if (pos < UserDictionaryToolsList.this.mWordList.size()) {
                    UserDictionaryToolsList.this.mWordCount = pos;
                    UserDictionaryToolsList.this.updateWordList(false);
                }
            }
        });
        this.mRightButton = b2;
        getActionBar().setDisplayHomeAsUpEnabled(true);
        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        mLanguage = 1;
        if (bundle != null) {
            String langtype = bundle.getString("lang_type");
            String langtypeEnOther = getResources().getString(R.string.ti_choose_language_value_en_other_txt);
            if (!langtypeEnOther.equals(langtype)) {
                mLanguage = LanguageManager.getChosenLanguageType(langtype);
            }
        }
        if (LanguageManager.isNoStroke(mLanguage)) {
            this.mIsNoStroke = true;
            findViewById(R.id.user_dictionary_title_read).setVisibility(8);
        }
        this.mEngineInterface = IWnnUserDictionaryToolsEngineInterface.getEngineInterface(mLanguage, hashCode());
        setTitleByLanguage(mLanguage);
        this.mEngineInterface.setDirPath(IWnnIME.getFilesDirPath(getApplicationContext()));
        if (IWnnIME.isDebugging()) {
            Log.d("IWnnIME", "onCreate() : end.");
        }
    }

    @Override
    public void onStart() {
        if (IWnnIME.isDebugging()) {
            Log.d("IWnnIME", "onStart()");
        }
        super.onStart();
        if (!UserDictionaryToolsActivity.isEditing()) {
            initList();
        }
    }

    @Override
    public void onResume() {
        if (IWnnIME.isDebugging()) {
            Log.d("IWnnIME", "onResume()");
        }
        super.onResume();
        onResumeProcess(mLanguage);
        if (UserDictionaryToolsDialog.isListUpdated()) {
            initList();
        }
    }

    @Override
    public void onPause() {
        if (IWnnIME.isDebugging()) {
            Log.d("IWnnIME", "onPause()");
        }
        if (this.mDialogShow != null) {
            this.mDialogShow.dismiss();
            this.mDialogShow = null;
        }
        super.onPause();
    }

    @Override
    public void onStop() {
        if (IWnnIME.isDebugging()) {
            Log.d("IWnnIME", "onStop()");
        }
        super.onStop();
    }

    @Override
    public void onDestroy() {
        if (IWnnIME.isDebugging()) {
            Log.d("IWnnIME", "onDestroy()");
        }
        super.onDestroy();
        started = false;
    }

    private TableLayout.LayoutParams tableCreateParam(int w, int h) {
        return new TableLayout.LayoutParams(w, h);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (IWnnIME.isDebugging()) {
            Log.d("IWnnIME", "onCreateOptionsMenu() : start.");
        }
        menu.clear();
        setOptionsMenuEnabled();
        menu.add(0, 0, 0, R.string.ti_user_dictionary_add_txt).setIcon(android.R.drawable.ic_menu_add).setEnabled(this.mAddMenuEnabled);
        menu.add(0, 1, 0, R.string.ti_user_dictionary_edit_txt).setIcon(android.R.drawable.ic_menu_edit).setEnabled(this.mEditMenuEnabled);
        menu.add(0, 2, 0, R.string.ti_user_dictionary_delete_txt).setIcon(android.R.drawable.ic_menu_delete).setEnabled(this.mDeleteMenuEnabled);
        menu.add(1, 3, 0, R.string.ti_user_dictionary_init_txt).setIcon(android.R.drawable.ic_menu_delete).setEnabled(this.mInitMenuEnabled);
        this.mMenu = menu;
        this.mInitializedMenu = true;
        if (IWnnIME.isDebugging()) {
            Log.d("IWnnIME", "onCreateOptionsMenu() : end.");
        }
        return super.onCreateOptionsMenu(menu);
    }

    private void setOptionsMenuEnabled() {
        if (IWnnIME.isDebugging()) {
            Log.d("IWnnIME", "setOptionsMenuEnabled() : start.");
        }
        if (this.mWordList.size() >= 500) {
            this.mAddMenuEnabled = false;
        } else {
            this.mAddMenuEnabled = true;
        }
        if (this.mWordList.size() <= 0) {
            this.mEditMenuEnabled = false;
            this.mDeleteMenuEnabled = false;
            this.mInitMenuEnabled = false;
        } else {
            if (this.mSelectedWords) {
                this.mEditMenuEnabled = true;
                this.mDeleteMenuEnabled = true;
            } else {
                this.mEditMenuEnabled = false;
                this.mDeleteMenuEnabled = false;
            }
            this.mInitMenuEnabled = true;
        }
        if (IWnnIME.isDebugging()) {
            Log.d("IWnnIME", "setOptionsMenuEnabled() : end.");
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean ret;
        if (IWnnIME.isDebugging()) {
            Log.d("IWnnIME", "onOptionsItemSelected() : start.");
        }
        switch (item.getItemId()) {
            case 0:
                addWord();
                ret = true;
                break;
            case 1:
                editWord();
                ret = true;
                break;
            case 2:
                this.mDialogShow = UserDictionaryToolsListDialog.newInstance(0);
                this.mDialogShow.show(getFragmentManager(), DIALOG_FRAGMENT_TAG);
                ret = true;
                break;
            case 3:
                this.mDialogShow = UserDictionaryToolsListDialog.newInstance(1);
                this.mDialogShow.show(getFragmentManager(), DIALOG_FRAGMENT_TAG);
                ret = true;
                break;
            case android.R.id.home:
                finish();
                ret = true;
                break;
            default:
                ret = false;
                break;
        }
        if (IWnnIME.isDebugging()) {
            Log.d("IWnnIME", "onOptionsItemSelected() : end.");
        }
        return ret;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode != 23) {
            return super.onKeyUp(keyCode, event);
        }
        openOptionsMenu();
        return true;
    }

    @Override
    public void onClick(View arg0) {
    }

    @Override
    public boolean onTouch(View v, MotionEvent e) {
        if (IWnnIME.isDebugging()) {
            Log.d("IWnnIME", "onTouch() : start.");
        }
        if (v instanceof UserDictionaryToolsListFocus) {
            UserDictionaryToolsListFocus view = (UserDictionaryToolsListFocus) v;
            switch (e.getAction()) {
                case 0:
                    if (sBeforeSelectedViewID != view.getId()) {
                        sBeforeSelectedViewID = view.getId();
                    } else if (e.getDownTime() - sJustBeforeActionTime < 300) {
                        this.mFocusingView = view;
                        this.mFocusingPairView = view.getPairView();
                        editWord();
                    }
                    sJustBeforeActionTime = e.getDownTime();
                    break;
            }
        }
        if (IWnnIME.isDebugging()) {
            Log.d("IWnnIME", "onTouch() : end.");
            return false;
        }
        return false;
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        if (IWnnIME.isDebugging()) {
            Log.d("IWnnIME", "onFocusChange() : start.");
        }
        if (v instanceof UserDictionaryToolsListFocus) {
            UserDictionaryToolsListFocus view = (UserDictionaryToolsListFocus) v;
            UserDictionaryToolsListFocus pairView = view.getPairView();
            this.mFocusingView = view;
            this.mFocusingPairView = pairView;
            if (hasFocus) {
                if (IWnnIME.isDebugging()) {
                    Log.d("IWnnIME", "Focused view");
                }
                view.setTextColor(-1);
                view.setBackgroundColor(FOCUS_BACKGROUND_COLOR);
                if (!this.mIsNoStroke) {
                    pairView.setTextColor(-1);
                    pairView.setBackgroundColor(FOCUS_BACKGROUND_COLOR);
                }
                this.mSelectedWords = true;
            } else {
                this.mSelectedWords = false;
                view.setTextColor(IWnnImeEvent.PRIVATE_EVENT_OFFSET);
                view.setBackgroundDrawable(null);
                if (!this.mIsNoStroke) {
                    pairView.setTextColor(IWnnImeEvent.PRIVATE_EVENT_OFFSET);
                    pairView.setBackgroundDrawable(null);
                }
            }
            if (this.mInitializedMenu) {
                onCreateOptionsMenu(this.mMenu);
            }
        }
        if (IWnnIME.isDebugging()) {
            Log.d("IWnnIME", "onFocusChange() : end.");
        }
    }

    public void addWord() {
        if (IWnnIME.isDebugging()) {
            Log.d("IWnnIME", "addWord() : start.");
        }
        callUserDictionary("android.intent.action.INSERT", LoggingEvents.EXTRA_CALLING_APP_NAME, LoggingEvents.EXTRA_CALLING_APP_NAME, -1);
        if (IWnnIME.isDebugging()) {
            Log.d("IWnnIME", "addWord() : end.");
        }
    }

    public void editWord() {
        String stroke;
        String candidate;
        if (IWnnIME.isDebugging()) {
            Log.d("IWnnIME", "editWord() : start.");
        }
        int wordId = -1;
        if (this.mFocusingView.getId() > 500) {
            candidate = this.mFocusingView.getText().toString();
            stroke = this.mFocusingPairView.getText().toString();
        } else {
            stroke = this.mFocusingView.getText().toString();
            candidate = this.mFocusingPairView.getText().toString();
        }
        Bundle bundle = this.mFocusingView.getInputExtras(false);
        if (bundle != null) {
            wordId = bundle.getInt("wordId", -1);
        }
        callUserDictionary("android.intent.action.EDIT", stroke, candidate, wordId);
        if (IWnnIME.isDebugging()) {
            Log.d("IWnnIME", "editWord() : end.");
        }
    }

    private synchronized void callUserDictionary(String action, String stroke, String candidate, int wordId) {
        Context context = getApplicationContext();
        if (context != null) {
            Intent intent = new Intent();
            intent.setClass(context, UserDictionaryToolsActivity.class);
            intent.addFlags(402653184);
            intent.putExtra(UserDictionaryToolsActivity.EDIT_FROM_DIALOG_KEY, false);
            intent.putExtra(UserDictionaryToolsActivity.LANGUAGE_TYPE_KEY, mLanguage);
            intent.putExtra(UserDictionaryToolsActivity.DIALOG_THEME_KEY, R.style.UserDictionaryTools);
            intent.putExtra(UserDictionaryToolsActivity.DIALOG_ID_KEY, action);
            intent.putExtra(UserDictionaryToolsActivity.DIALOG_STROKE_KEY, stroke);
            intent.putExtra(UserDictionaryToolsActivity.DIALOG_CANDIDATE_KEY, candidate);
            intent.putExtra(UserDictionaryToolsActivity.DIALOG_WORD_ID_KEY, wordId);
            startActivity(intent);
        }
    }

    protected void sortList(ArrayList<WnnWord> array) {
        this.mSortData = new WnnWord[array.size()];
        array.toArray(this.mSortData);
        Arrays.sort(this.mSortData, getComparator());
    }

    private void updateWordList(boolean keepFocus) {
        int currentSelectId = getInitViewId();
        if (!this.mInit) {
            this.mInit = true;
            Context context = getApplicationContext();
            UserDictionaryToolsListFocus dummy = new UserDictionaryToolsListFocus(context);
            dummy.setTextSize(16.0f);
            TextPaint paint = dummy.getPaint();
            Paint.FontMetricsInt fontMetrics = paint.getFontMetricsInt();
            int row_hight = (Math.abs(fontMetrics.top) + fontMetrics.bottom) * 2;
            Resources res = getResources();
            for (int i = 1; i <= 50; i++) {
                TableRow row = new TableRow(context);
                UserDictionaryToolsListFocus stroke = new UserDictionaryToolsListFocus(context);
                stroke.setId(i);
                stroke.setWidth(0);
                stroke.setTextSize(16.0f);
                stroke.setTextColor(IWnnImeEvent.PRIVATE_EVENT_OFFSET);
                stroke.setBackgroundDrawable(null);
                stroke.setSingleLine();
                stroke.setPadding(res.getDimensionPixelSize(R.dimen.userdic_list_stroke_padding_left), res.getDimensionPixelSize(R.dimen.userdic_list_stroke_padding_top), res.getDimensionPixelSize(R.dimen.userdic_list_stroke_padding_right), res.getDimensionPixelSize(R.dimen.userdic_list_stroke_padding_bottom));
                stroke.setEllipsize(TextUtils.TruncateAt.END);
                stroke.setClickable(true);
                stroke.setFocusable(true);
                stroke.setFocusableInTouchMode(true);
                stroke.setOnTouchListener(this);
                stroke.setOnFocusChangeListener(this);
                stroke.setHeight(row_hight);
                stroke.setGravity(16);
                if (this.mIsNoStroke) {
                    stroke.setVisibility(8);
                }
                row.addView(stroke);
                UserDictionaryToolsListFocus candidate = new UserDictionaryToolsListFocus(context);
                candidate.setId(i + 500);
                candidate.setWidth(0);
                candidate.setTextSize(16.0f);
                candidate.setTextColor(IWnnImeEvent.PRIVATE_EVENT_OFFSET);
                candidate.setBackgroundDrawable(null);
                candidate.setSingleLine();
                candidate.setPadding(res.getDimensionPixelSize(R.dimen.userdic_list_candidate_padding_left), res.getDimensionPixelSize(R.dimen.userdic_list_candidate_padding_top), res.getDimensionPixelSize(R.dimen.userdic_list_candidate_padding_right), res.getDimensionPixelSize(R.dimen.userdic_list_candidate_padding_bottom));
                candidate.setEllipsize(TextUtils.TruncateAt.END);
                candidate.setClickable(true);
                candidate.setFocusable(true);
                candidate.setFocusableInTouchMode(true);
                candidate.setOnTouchListener(this);
                candidate.setOnFocusChangeListener(this);
                candidate.setHeight(row_hight);
                candidate.setGravity(16);
                stroke.setPairView(candidate);
                candidate.setPairView(stroke);
                row.addView(candidate);
                this.mTableLayout.addView(row, tableCreateParam(-1, -2));
            }
        } else if (keepFocus) {
            currentSelectId = this.mFocusingView.getId();
        }
        int size = this.mWordList.size();
        int start = this.mWordCount;
        TextView t = (TextView) findViewById(R.id.user_dictionary_position_indicator);
        if (size <= 50) {
            ((View) this.mLeftButton.getParent()).setVisibility(8);
        } else {
            ((View) this.mLeftButton.getParent()).setVisibility(0);
            int last = start + 50;
            t.setText((start + 1) + " - " + Math.min(last, size));
            this.mLeftButton.setEnabled(start != 0);
            this.mRightButton.setEnabled(last < size);
        }
        int selectedId = currentSelectId - (500 < currentSelectId ? 500 : 0);
        for (int i2 = 0; i2 < 50; i2++) {
            if (size - 1 < start + i2) {
                if (i2 > 0 && selectedId == i2 + 1) {
                    int id = i2 + (this.mIsNoStroke ? 500 : 0);
                    this.mTableLayout.findViewById(id).requestFocus();
                }
                ((View) this.mTableLayout.findViewById(i2 + 1).getParent()).setVisibility(8);
            } else {
                if (this.mSortData == null) {
                    break;
                }
                WnnWord wnnWordGet = this.mSortData[start + i2];
                int len_stroke = wnnWordGet.stroke.length();
                int len_candidate = wnnWordGet.candidate.length();
                if (len_stroke == 0 || len_candidate == 0) {
                    break;
                }
                if (selectedId == i2 + 1) {
                    int id2 = selectedId + (this.mIsNoStroke ? 500 : 0);
                    this.mTableLayout.findViewById(id2).requestFocus();
                }
                TextView text = (TextView) this.mTableLayout.findViewById(i2 + 1);
                text.setText(wnnWordGet.stroke);
                Bundle bundle_stroke = text.getInputExtras(true);
                if (bundle_stroke != null) {
                    bundle_stroke.putInt("wordId", wnnWordGet.id);
                }
                TextView text2 = (TextView) this.mTableLayout.findViewById(i2 + 1 + 500);
                text2.setText(wnnWordGet.candidate);
                Bundle bundle_candidate = text2.getInputExtras(true);
                if (bundle_candidate != null) {
                    bundle_candidate.putInt("wordId", wnnWordGet.id);
                }
                ((View) text2.getParent()).setVisibility(0);
            }
        }
        if (this.mIsNoStroke) {
            this.mTableLayout.setColumnCollapsed(0, true);
            this.mTableLayout.setColumnStretchable(1, true);
        } else {
            this.mTableLayout.setStretchAllColumns(true);
        }
        this.mTableLayout.requestLayout();
    }

    public static boolean hasStarted() {
        return started;
    }

    private int getInitViewId() {
        return this.mIsNoStroke ? 501 : 1;
    }

    private void setTitleByLanguage(int lang) {
        TextView langView = (TextView) findViewById(R.id.user_dictionary_tools_list_title_language);
        int nameId = LanguageManager.getUserDictionaryTitle(lang);
        langView.setText(nameId);
    }

    private Comparator<WnnWord> getComparator() {
        switch (mLanguage) {
            case 0:
                return new ListComparatorJaJp();
            case 1:
                return new ListComparatorEn();
            case 10:
                return new ListComparatorCyrillicAlphabet();
            case 15:
                return new ListComparatorZh();
            case 18:
                return new ListComparatorHangul();
            default:
                return new ListComparatorLatin();
        }
    }

    public static int getUserDicLanguage() {
        return mLanguage;
    }

    private void restartControlPanel() {
        ControlPanelStandard conpane = ControlPanelStandard.getCurrentControlPanel();
        if (conpane.isMultiPaneMode()) {
            conpane.restart();
        } else {
            conpane.moveToRootActivity();
        }
    }

    private void onResumeProcess(int langType) {
        if (langType != 0 && 1 != langType) {
            KeyboardLanguagePackData langPack = KeyboardLanguagePackData.getInstance();
            if (langPack.getLangPackClassName(getApplicationContext(), langType) == null) {
                restartControlPanel();
            }
        }
    }

    private void initList() {
        sBeforeSelectedViewID = -1;
        sJustBeforeActionTime = -1L;
        this.mWordList = this.mEngineInterface.getWords();
        this.mWordCount = 0;
        sortList(this.mWordList);
        TextView leftText = (TextView) findViewById(R.id.user_dictionary_tools_list_title_words_count);
        leftText.setText(this.mWordList.size() + "/500");
        updateWordList(false);
        if (this.mInitializedMenu && this.mWordList.size() >= 0) {
            onCreateOptionsMenu(this.mMenu);
        }
    }
}
