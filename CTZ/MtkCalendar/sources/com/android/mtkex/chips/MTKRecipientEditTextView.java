package com.android.mtkex.chips;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.SystemProperties;
import android.os.Trace;
import android.telephony.PhoneNumberUtils;
import android.text.BoringLayout;
import android.text.Editable;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.QwertyKeyListener;
import android.text.util.Rfc822Token;
import android.text.util.Rfc822Tokenizer;
import android.util.AttributeSet;
import android.util.Log;
import android.util.LogPrinter;
import android.util.TypedValue;
import android.view.ActionMode;
import android.view.Choreographer;
import android.view.DragEvent;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewTreeObserver;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.Filterable;
import android.widget.FrameLayout;
import android.widget.ListAdapter;
import android.widget.ListPopupWindow;
import android.widget.ListView;
import android.widget.MultiAutoCompleteTextView;
import android.widget.ScrollView;
import android.widget.TextView;
import com.android.mtkex.chips.BaseRecipientAdapter;
import com.android.mtkex.chips.MTKContactObserver;
import com.android.mtkex.chips.RecipientAlternatesAdapter;
import com.android.mtkex.chips.recipientchip.DrawableRecipientChip;
import com.android.mtkex.chips.recipientchip.InvisibleRecipientChip;
import com.android.mtkex.chips.recipientchip.ReplacementDrawableSpan;
import com.android.mtkex.chips.recipientchip.VisibleRecipientChip;
import dalvik.system.VMRuntime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public class MTKRecipientEditTextView extends MultiAutoCompleteTextView implements DialogInterface.OnDismissListener, ActionMode.Callback, GestureDetector.OnGestureListener, View.OnClickListener, AdapterView.OnItemClickListener, TextView.OnEditorActionListener, RecipientAlternatesAdapter.OnCheckedItemChangedListener {
    private boolean bPasted;
    private boolean bTouchedAfterPasted;
    private ArrayList<String> changedChipAddresses;
    private boolean isRegisterVSync;
    private int mActionBarHeight;
    private final Runnable mAddTextWatcher;
    private ListPopupWindow mAddressPopup;
    private int mAlternatesLayout;
    private AdapterView.OnItemClickListener mAlternatesListener;
    private ListPopupWindow mAlternatesPopup;
    private View mAnchorView;
    private boolean mAttachedToWindow;
    private Set<Bitmap> mBitMapSet;
    private int mCheckedItem;
    private Drawable mChipBackground;
    private Drawable mChipBackgroundPressed;
    private ArrayList<ChipWatcher> mChipChangedListeners;
    private Drawable mChipDelete;
    private float mChipFontSize;
    private float mChipHeight;
    private int mChipLimit;
    private int mChipPadding;
    private TextPaint mChipPaint;
    private ChipProcessListener mChipProcessListener;
    private PROCESSING_MODE mChipProcessingMode;
    private HashMap<String, ArrayList<DrawableRecipientChip>> mChipsMap;
    private Choreographer mChoreographer;
    private MTKContactObserver.ContactListener mContactListener;
    private Context mContext;
    private String mCopyAddress;
    private AlertDialog mCopyDialog;
    private int mCurrentWidth;
    private Bitmap mDefaultContactPhoto;
    private float mDefaultTextSize;
    private Runnable mDelayedShrink;
    private Paint mDeletePaint;
    private boolean mDisableBringPointIntoView;
    private float mDownPosY;
    private boolean mDragEnabled;
    private boolean mDuringAccelerateRemoveChip;
    private boolean mDuringReplaceDupChips;
    private final Object mEllipsizedChipLock;
    private boolean mEnableScrollAddText;
    private boolean mForceEnableBringPointIntoView;
    private GestureDetector mGestureDetector;
    private ViewTreeObserver.OnGlobalLayoutListener mGlobalLayoutListener;
    private Runnable mHandlePendingChips;
    private Handler mHandler;
    private boolean mHandlingAlternatesDropDown;
    private boolean mHasEllipsizedFirstChip;
    private boolean mHasScrolled;
    private int mIconWidth;
    private IndividualReplacementTask mIndividualReplacements;
    private Drawable mInvalidChipBackground;
    private boolean mIsAutoTesting;
    private boolean mJustExpanded;
    private boolean mLastStringChanged;
    private int mLimitedWidthForSpan;
    private int mLineOfSelectedChip;
    private float mLineSpacingExtra;
    private int mMaxLines;
    private ReplacementDrawableSpan mMoreChip;
    private TextView mMoreItem;
    private boolean mMoveCursorToVisible;
    private boolean mNoChips;
    private ArrayList<DrawableRecipientChip> mPedingReplaceChips;
    private ArrayList<RecipientEntry> mPedingReplaceEntries;
    private final ArrayList<String> mPendingChips;
    private int mPendingChipsCount;
    private PhoneNumberQueryAndReplacementTask mPhoneNumberQueryAndReplacementTask;
    private boolean mRETVDiscardNextActionUp;
    private ArrayList<DrawableRecipientChip> mRemovedSpans;
    private HashMap<String, ArrayList<DrawableRecipientChip>> mRemovedSpansMap;
    private ScrollView mScrollView;
    private DrawableRecipientChip mSelectedChip;
    private boolean mShouldShrink;
    AsyncTask<Void, Void, ListAdapter> mShowAlternatesTask;
    private String mStringToBeRestore;
    private ArrayList<DrawableRecipientChip> mTemporaryRecipients;
    private TextWatcher mTextWatcher;
    private MultiAutoCompleteTextView.Tokenizer mTokenizer;
    private boolean mTriedGettingScrollView;
    private AutoCompleteTextView.Validator mValidator;
    private Paint mWorkPaint;
    private Runnable notifyChipChangedRunnable;
    private static final String SEPARATOR = String.valueOf(',') + String.valueOf(' ');
    private static boolean DEBUG_THREADING_LOG = true;
    private static boolean DEBUG_LOG = true;
    private static int DISMISS = "dismiss".hashCode();
    private static int sSelectedTextColor = -1;
    private static boolean piLoggable = true ^ "user".equals(SystemProperties.get("ro.build.type", "user"));
    public static final Pattern PHONE_EX = Pattern.compile("(\\+[0-9\\(\\)]+[\\- \\.]*)?(\\([0-9\\(\\)]+\\)[\\- \\.]*)?([0-9\\(\\)][0-9\\(\\)\\- \\.][0-9\\(\\)\\- \\.]+[0-9\\(\\)])(,? *)?");
    private static int sExcessTopPadding = -1;
    private static MTKContactObserver sContactObserver = null;
    private static int mFeatureSet = 0;

    public interface ChipProcessListener {
        void onChipProcessDone();
    }

    public interface ChipWatcher {
        void onChipChanged(ArrayList<RecipientEntry> arrayList, ArrayList<String> arrayList2, String str);
    }

    private enum PROCESSING_MODE {
        NONE,
        COMMIT,
        REMOVE,
        REMOVE_LAST,
        REPLACE,
        REPLACE_LAST
    }

    public MTKRecipientEditTextView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mChipBackground = null;
        this.mDownPosY = 0.0f;
        this.mChipDelete = null;
        this.mPendingChips = new ArrayList<>();
        this.mPendingChipsCount = 0;
        this.mNoChips = false;
        this.mChipProcessingMode = PROCESSING_MODE.NONE;
        this.mShouldShrink = true;
        this.mDragEnabled = false;
        this.mBitMapSet = new HashSet();
        this.mAddTextWatcher = new Runnable() {
            @Override
            public void run() {
                MTKRecipientEditTextView.this.printDebugLog("RecipientEditTextView", "[mAddTextWatcher.run]");
                if (MTKRecipientEditTextView.this.mTextWatcher == null) {
                    MTKRecipientEditTextView.this.mTextWatcher = new RecipientTextWatcher();
                    MTKRecipientEditTextView.this.addTextChangedListener(MTKRecipientEditTextView.this.mTextWatcher);
                }
            }
        };
        this.mHandlePendingChips = new Runnable() {
            @Override
            public void run() {
                MTKRecipientEditTextView.this.printDebugLog("RecipientEditTextView", "[mHandlePendingChips.run]");
                Trace.traceBegin(8L, "handlePendingChips");
                MTKRecipientEditTextView.this.handlePendingChips();
                Trace.traceEnd(8L);
            }
        };
        this.mDelayedShrink = new Runnable() {
            @Override
            public void run() {
                MTKRecipientEditTextView.this.shrink();
            }
        };
        this.mHandlingAlternatesDropDown = false;
        this.mWorkPaint = new Paint();
        this.mDeletePaint = new Paint();
        this.mContactListener = new MTKContactObserver.ContactListener() {
            @Override
            public void onContactChange(Set set) {
                Log.d("client", set.toString());
                MTKRecipientEditTextView.this.handleContactChange(set);
            }
        };
        this.mHasScrolled = false;
        this.mAnchorView = null;
        this.mChipProcessListener = null;
        this.mEnableScrollAddText = true;
        this.mRETVDiscardNextActionUp = false;
        this.bPasted = false;
        this.bTouchedAfterPasted = false;
        this.mLimitedWidthForSpan = -1;
        this.mHasEllipsizedFirstChip = false;
        this.mEllipsizedChipLock = new Object();
        this.mMoveCursorToVisible = false;
        this.mDisableBringPointIntoView = true;
        this.mForceEnableBringPointIntoView = false;
        this.mLineOfSelectedChip = 0;
        this.mGlobalLayoutListener = null;
        this.mCurrentWidth = 0;
        this.mDefaultTextSize = 0.0f;
        this.mJustExpanded = false;
        this.mDuringAccelerateRemoveChip = false;
        this.mStringToBeRestore = null;
        this.mDuringReplaceDupChips = false;
        this.mIsAutoTesting = false;
        this.changedChipAddresses = new ArrayList<String>() {
            @Override
            public boolean add(String str) {
                if (MTKRecipientEditTextView.this.changedChipAddresses.size() == 0) {
                    MTKRecipientEditTextView.this.registerVSync();
                }
                if (!contains(str)) {
                    return super.add(str);
                }
                return false;
            }
        };
        this.notifyChipChangedRunnable = new Runnable() {
            @Override
            public void run() {
                if (MTKRecipientEditTextView.this.changedChipAddresses.size() != 0 || MTKRecipientEditTextView.this.mLastStringChanged) {
                    Log.d("RecipientEditTextView", "[notifyChipChanged] changedChipAddresses.size(): " + MTKRecipientEditTextView.this.changedChipAddresses.size());
                    MTKRecipientEditTextView.this.notifyChipChanged();
                    MTKRecipientEditTextView.this.changedChipAddresses.clear();
                    if (true == MTKRecipientEditTextView.this.mLastStringChanged) {
                        MTKRecipientEditTextView.this.mLastStringChanged = false;
                    }
                }
                MTKRecipientEditTextView.this.isRegisterVSync = false;
            }
        };
        this.mContext = context;
        printDebugLog("RecipientEditTextView", "[MTKRecipientEditTextView] constructor");
        setChipDimensions(context, attributeSet);
        if (sSelectedTextColor == -1) {
            sSelectedTextColor = context.getResources().getColor(android.R.color.white);
        }
        this.mChipPaint = new TextPaint(getPaint());
        this.mChipPaint.setAlpha(255);
        this.mDefaultTextSize = this.mChipPaint.getTextSize();
        this.mAlternatesPopup = new ListPopupWindow(context);
        this.mAddressPopup = new ListPopupWindow(context);
        this.mAlternatesListener = new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long j) {
                if (!MTKRecipientEditTextView.this.mHandlingAlternatesDropDown) {
                    MTKRecipientEditTextView.this.mHandlingAlternatesDropDown = true;
                    MTKRecipientEditTextView.this.mAlternatesPopup.setOnItemClickListener(null);
                    if (MTKRecipientEditTextView.this.mSelectedChip != null) {
                        MTKRecipientEditTextView.this.replaceChip(MTKRecipientEditTextView.this.mSelectedChip, ((RecipientAlternatesAdapter) adapterView.getAdapter()).getRecipientEntry(i));
                        Message messageObtain = Message.obtain(MTKRecipientEditTextView.this.mHandler, MTKRecipientEditTextView.DISMISS);
                        messageObtain.obj = MTKRecipientEditTextView.this.mAlternatesPopup;
                        MTKRecipientEditTextView.this.mHandler.sendMessageDelayed(messageObtain, 300L);
                        MTKRecipientEditTextView.this.clearComposingText();
                    }
                }
            }
        };
        setInputType(getInputType() | 524288);
        setOnItemClickListener(this);
        setCustomInsertionActionModeCallback(new ActionMode.Callback() {
            @Override
            public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
                MTKRecipientEditTextView.this.printDebugLog("RecipientEditTextView", "[InsertionActionModeCallback] onPrepareActionMode");
                menu.removeItem(android.R.id.selectAll);
                return true;
            }

            @Override
            public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
                return false;
            }

            @Override
            public void onDestroyActionMode(ActionMode actionMode) {
            }
        });
        setCustomSelectionActionModeCallback(this);
        this.mHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                if (message.what == MTKRecipientEditTextView.DISMISS) {
                    ((ListPopupWindow) message.obj).dismiss();
                } else {
                    super.handleMessage(message);
                }
            }
        };
        this.mTextWatcher = new RecipientTextWatcher();
        addTextChangedListener(this.mTextWatcher);
        this.mGestureDetector = new GestureDetector(context, this);
        setOnEditorActionListener(this);
        DEBUG_THREADING_LOG = true;
        this.mPedingReplaceChips = new ArrayList<>();
        this.mPedingReplaceEntries = new ArrayList<>();
        Log.d("RecipientEditTextView", "setForceEnableBringPointIntoView(true) in construct of MTKRecipientEditTextView");
        setForceEnableBringPointIntoView(true);
        configFeatures(context);
        if (isPhoneQuery()) {
            this.mChipLimit = 5;
        } else {
            this.mChipLimit = 2;
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        this.mAttachedToWindow = false;
        if ((mFeatureSet & 1) != 0) {
            sContactObserver.removeContactListener(this.mContactListener);
        }
    }

    @Override
    protected void onAttachedToWindow() {
        this.mAttachedToWindow = true;
        if ((1 & mFeatureSet) != 0) {
            sContactObserver.addContactListener(this.mContactListener);
        }
    }

    @Override
    public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
        printDebugLog("RecipientEditTextView", "[onEditorAction] " + keyEvent);
        if (i == 6) {
            if (commitDefault()) {
                return true;
            }
            if (this.mSelectedChip == null) {
                return focusNext();
            }
            clearSelectedChip();
            return true;
        }
        return false;
    }

    @Override
    public InputConnection onCreateInputConnection(EditorInfo editorInfo) {
        InputConnection inputConnectionOnCreateInputConnection = super.onCreateInputConnection(editorInfo);
        int i = editorInfo.imeOptions & 255;
        if ((i & 6) != 0) {
            editorInfo.imeOptions = i ^ editorInfo.imeOptions;
            editorInfo.imeOptions |= 6;
        }
        if ((editorInfo.imeOptions & 1073741824) != 0) {
            editorInfo.imeOptions &= -1073741825;
        }
        editorInfo.actionId = 6;
        editorInfo.actionLabel = getContext().getString(R.string.done);
        return inputConnectionOnCreateInputConnection;
    }

    DrawableRecipientChip getLastChip() {
        DrawableRecipientChip[] sortedRecipients = getSortedRecipients();
        if (sortedRecipients != null && sortedRecipients.length > 0) {
            return sortedRecipients[sortedRecipients.length - 1];
        }
        return null;
    }

    private int getChipCound() {
        return getSortedRecipients().length;
    }

    @Override
    public void onSelectionChanged(int i, int i2) {
        int iMin;
        boolean z = getResources().getConfiguration().orientation == 1;
        DrawableRecipientChip lastChip = getLastChip();
        Spannable spannable = getSpannable();
        int chipCound = getChipCound();
        int spanStart = spannable.getSpanStart(lastChip);
        int spanEnd = spannable.getSpanEnd(lastChip);
        int length = getText().length();
        Log.d("RecipientEditTextView", "onSelectionChanged, start = " + i + ", end = " + i2 + ", isPortrait = " + z + ", chipCound = " + chipCound);
        if (z || (chipCound == 1 && (spanEnd - spanStart) + 1 == length)) {
            Log.d("RecipientEditTextView", "onSelectionChanged, last = " + lastChip + ", lastStart = " + spanStart + ", lastEnd = " + spanEnd + ", textLen = " + length);
            if (lastChip != null && i <= getSpannable().getSpanEnd(lastChip)) {
                setSelection(Math.min(getSpannable().getSpanEnd(lastChip) + 1, getText().length()));
            }
            if (this.mNoChips && this.mJustExpanded) {
                Editable text = getText();
                setSelection((text == null || text.length() <= 0) ? 0 : text.length());
                this.mJustExpanded = false;
            }
            super.onSelectionChanged(i, i2);
            return;
        }
        Log.d("RecipientEditTextView", "onSelectionChanged, last = " + lastChip + ", lastStart = " + spanStart + ", lastEnd = " + spanEnd + ", textLen = " + length);
        if (lastChip != null && i2 >= spanStart && i2 <= spanEnd) {
            iMin = Math.min(spanEnd + 1, length);
        } else {
            iMin = i2;
        }
        Log.d("RecipientEditTextView", "onSelectionChanged, mNoChips = " + this.mNoChips + ", mJustExpanded = " + this.mJustExpanded);
        if (this.mNoChips && this.mJustExpanded) {
            Log.d("RecipientEditTextView", "onSelectionChanged, setPos = " + ((getText() == null || length <= 0) ? 0 : length));
            iMin = Math.min(spanEnd + 1, length);
            this.mJustExpanded = false;
        }
        if (iMin != i2) {
            Log.d("RecipientEditTextView", "reset end to " + iMin);
            setSelection(i, iMin);
            return;
        }
        super.onSelectionChanged(i, i2);
    }

    public static class RecipientSavedState extends View.BaseSavedState {
        public static final Parcelable.Creator<RecipientSavedState> CREATOR = new Parcelable.Creator<RecipientSavedState>() {
            @Override
            public RecipientSavedState createFromParcel(Parcel parcel) {
                return new RecipientSavedState(parcel);
            }

            @Override
            public RecipientSavedState[] newArray(int i) {
                return new RecipientSavedState[i];
            }
        };
        boolean frozenWithFocus;

        RecipientSavedState(Parcelable parcelable) {
            super(parcelable);
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            super.writeToParcel(parcel, i);
            parcel.writeInt(this.frozenWithFocus ? 1 : 0);
        }

        public String toString() {
            return "RecipientEditTextView.RecipientSavedState{" + Integer.toHexString(System.identityHashCode(this)) + " frozenWithFocus=" + this.frozenWithFocus + ")";
        }

        private RecipientSavedState(Parcel parcel) {
            super(parcel);
            this.frozenWithFocus = parcel.readInt() != 0;
        }
    }

    @Override
    public void onRestoreInstanceState(Parcelable parcelable) {
        int iFindTokenEnd;
        RecipientSavedState recipientSavedState = (RecipientSavedState) parcelable;
        boolean z = recipientSavedState.frozenWithFocus;
        this.mPendingChips.clear();
        if (!TextUtils.isEmpty(getText())) {
            super.onRestoreInstanceState(null);
        } else {
            super.onRestoreInstanceState(recipientSavedState.getSuperState());
        }
        Log.d("RecipientEditTextView", "[onRestore] Text->" + ((Object) getText()));
        if (!TextUtils.isEmpty(getText()) && this.mTokenizer != null) {
            if (this.mTextWatcher != null) {
                removeTextChangedListener(this.mTextWatcher);
            }
            String string = getText().toString();
            int length = string.length();
            printThreadingDebugLog("MTKRecip", "[onRestoreInstanceState] delete");
            int i = 0;
            getText().delete(0, length);
            MTKRecipientList mTKRecipientList = new MTKRecipientList();
            while (true) {
                iFindTokenEnd = this.mTokenizer.findTokenEnd(string, i);
                if (iFindTokenEnd >= string.length()) {
                    break;
                }
                String strSubstring = string.substring(i, iFindTokenEnd);
                int i2 = iFindTokenEnd + 2;
                String str = tokenizeName(strSubstring);
                if (!isPhoneNumber(strSubstring)) {
                    strSubstring = tokenizeAddress(strSubstring);
                }
                mTKRecipientList.addRecipient(str, strSubstring);
                i = i2;
            }
            appendList(mTKRecipientList);
            if (i < iFindTokenEnd) {
                String strSubstring2 = string.substring(i, iFindTokenEnd);
                if (mTKRecipientList.getRecipientCount() != 0) {
                    this.mStringToBeRestore = strSubstring2;
                } else {
                    getText().append((CharSequence) strSubstring2);
                }
            }
            this.mHandler.post(this.mAddTextWatcher);
        }
    }

    @Override
    public Parcelable onSaveInstanceState() {
        clearSelectedChip();
        RecipientSavedState recipientSavedState = new RecipientSavedState(super.onSaveInstanceState());
        if (isFocused()) {
            recipientSavedState.frozenWithFocus = true;
        } else {
            recipientSavedState.frozenWithFocus = false;
        }
        printSensitiveDebugLog("RecipientEditTextView", "[onSave] Text ->" + ((Object) getText()));
        return recipientSavedState;
    }

    @Override
    public void append(CharSequence charSequence, int i, int i2) {
        if (this.mTextWatcher != null) {
            removeTextChangedListener(this.mTextWatcher);
        }
        super.append(charSequence, i, i2);
        if (!TextUtils.isEmpty(charSequence) && TextUtils.getTrimmedLength(charSequence) > 0) {
            String string = charSequence.toString();
            if (!string.trim().endsWith(String.valueOf(','))) {
                super.append(SEPARATOR, 0, SEPARATOR.length());
                string = string + SEPARATOR;
            }
            if (!TextUtils.isEmpty(string) && TextUtils.getTrimmedLength(string) > 0) {
                this.mPendingChipsCount++;
                this.mPendingChips.add(string);
            }
        }
        if (this.mPendingChipsCount > 0) {
            postHandlePendingChips();
        }
        this.mHandler.post(this.mAddTextWatcher);
    }

    public void appendList(MTKRecipientList mTKRecipientList) {
        String strSubstring;
        int iIndexOf;
        if (mTKRecipientList == null || mTKRecipientList.getRecipientCount() <= 0) {
            return;
        }
        Trace.traceBegin(8L, "appendList");
        int recipientCount = mTKRecipientList.getRecipientCount();
        printDebugLog("RecipientEditTextView", "[appendList] Start, count: " + recipientCount);
        String str = "";
        for (int i = 0; i < recipientCount; i++) {
            str = str + mTKRecipientList.getRecipient(i).getFormatString();
        }
        if (this.mTextWatcher != null) {
            removeTextChangedListener(this.mTextWatcher);
        }
        SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(getText());
        spannableStringBuilder.append((CharSequence) str);
        setText(spannableStringBuilder);
        if (Log.isLoggable("RecipientEditTextView", 3)) {
            StringBuilder sb = new StringBuilder();
            for (int i2 = 0; i2 < recipientCount; i2++) {
                MTKRecipient recipient = mTKRecipientList.getRecipient(i2);
                sb.append("[appendList] Recipient -> Name = " + recipient.getDisplayName() + " & Dest = " + recipient.getDestination() + "\n");
            }
            Log.d("RecipientEditTextView", sb.toString());
        }
        StringBuilder sb2 = new StringBuilder();
        for (int i3 = 0; i3 < recipientCount; i3++) {
            MTKRecipient recipient2 = mTKRecipientList.getRecipient(i3);
            String formatString = recipient2.getFormatString();
            if (!TextUtils.isEmpty(formatString) && TextUtils.getTrimmedLength(formatString) > 0) {
                sb2.append("[appendList] adding pending chips, index: " + i3 + ", " + formatString + "\n");
                String string = formatString.toString();
                int iLastIndexOf = string.lastIndexOf(44);
                if (iLastIndexOf > -1 && (iIndexOf = (strSubstring = string.substring(iLastIndexOf)).indexOf(34)) > iLastIndexOf) {
                    strSubstring.lastIndexOf(44, iIndexOf);
                }
                if (!TextUtils.isEmpty(string) && TextUtils.getTrimmedLength(string) > 0) {
                    this.mPendingChipsCount++;
                    this.mPendingChips.add(formatString.toString());
                }
            }
            if ((mFeatureSet & 2) != 0) {
                this.changedChipAddresses.add(recipient2.getDestination());
            }
        }
        printDebugLog("RecipientEditTextView", sb2.toString());
        if (this.mPendingChipsCount > 0) {
            postHandlePendingChips();
        }
        this.mHandler.post(this.mAddTextWatcher);
        Trace.traceEnd(8L);
    }

    @Override
    public boolean performLongClick() {
        Log.d("RecipientEditTextView", " setDisableBringPointIntoView(false) in performLongClick() ");
        setDisableBringPointIntoView(false);
        return super.performLongClick();
    }

    @Override
    public void onFocusChanged(boolean z, int i, Rect rect) {
        printDebugLog("RecipientEditTextView", "[onFocusChanged] hasFocus: " + z);
        super.onFocusChanged(z, i, rect);
        if (!z) {
            shrink();
        } else {
            expand();
        }
    }

    @Override
    public <T extends ListAdapter & Filterable> void setAdapter(T t) {
        super.setAdapter(t);
        ((BaseRecipientAdapter) t).registerUpdateObserver(new BaseRecipientAdapter.EntriesUpdatedObserver() {
            @Override
            public void onChanged(List<RecipientEntry> list) {
                if (list != null && list.size() > 0) {
                    MTKRecipientEditTextView.this.scrollBottomIntoView();
                }
            }
        });
    }

    private void scrollBottomIntoView() {
        if (this.mScrollView != null && this.mShouldShrink) {
            int[] iArr = new int[2];
            getLocationOnScreen(iArr);
            int height = iArr[1] + getHeight();
            int excessTopPadding = ((int) this.mChipHeight) + this.mActionBarHeight + getExcessTopPadding();
            if (height > excessTopPadding) {
                this.mScrollView.scrollBy(0, height - excessTopPadding);
            }
        }
    }

    private int getExcessTopPadding() {
        if (sExcessTopPadding == -1) {
            sExcessTopPadding = (int) (this.mChipHeight + this.mLineSpacingExtra);
        }
        return sExcessTopPadding;
    }

    @Override
    public void performValidation() {
    }

    private void shrink() {
        if (this.mTokenizer == null) {
            return;
        }
        Editable text = getText();
        if (this.mNoChips) {
            if (countTokens(text) < 100) {
                this.mNoChips = false;
            } else {
                printDebugLog("RecipientEditTextView", "[shrink] mNoChips");
                createMoreChip();
                return;
            }
        }
        if (text.length() == 0) {
            printDebugLog("RecipientEditTextView", "[shrink] empty, return");
            return;
        }
        if (isPhoneQuery()) {
            setSelection(text.length());
        }
        long contactId = this.mSelectedChip != null ? this.mSelectedChip.getEntry().getContactId() : -1L;
        if (this.mSelectedChip != null && contactId != -1 && ((!isPhoneQuery() && contactId != -2) || isPhoneQuery())) {
            printDebugLog("RecipientEditTextView", "[shrink] selecting chip");
            clearSelectedChip();
        } else {
            if (getWidth() <= 0) {
                this.mHandler.removeCallbacks(this.mDelayedShrink);
                this.mHandler.post(this.mDelayedShrink);
                return;
            }
            if (this.mPendingChipsCount > 0) {
                printDebugLog("RecipientEditTextView", "[shrink] mPendingChipsCount > 0");
                postHandlePendingChips();
            } else {
                printDebugLog("RecipientEditTextView", "[shrink] mPendingChipsCount = 0");
                boolean zTextIsAllBlank = textIsAllBlank(text);
                int selectionEnd = getSelectionEnd();
                int iFindTokenStart = this.mTokenizer.findTokenStart(text, selectionEnd);
                DrawableRecipientChip[] drawableRecipientChipArr = (DrawableRecipientChip[]) getSpannable().getSpans(iFindTokenStart, selectionEnd, DrawableRecipientChip.class);
                if (drawableRecipientChipArr == null || drawableRecipientChipArr.length == 0) {
                    Editable text2 = getText();
                    int iFindTokenEnd = this.mTokenizer.findTokenEnd(text2, iFindTokenStart);
                    if (iFindTokenEnd < text2.length() && text2.charAt(iFindTokenEnd) == ',') {
                        iFindTokenEnd = movePastTerminators(iFindTokenEnd);
                    }
                    if (iFindTokenEnd != getSelectionEnd() && !zTextIsAllBlank) {
                        handleEdit(iFindTokenStart, iFindTokenEnd);
                    } else {
                        commitChip(iFindTokenStart, selectionEnd, text);
                    }
                }
            }
            this.mHandler.post(this.mAddTextWatcher);
        }
        createMoreChip();
    }

    private boolean textIsAllBlank(Editable editable) {
        if (editable == null) {
            return false;
        }
        for (int i = 0; i < editable.length(); i++) {
            if (editable.charAt(i) != ' ') {
                return false;
            }
        }
        return true;
    }

    private void expand() {
        DrawableRecipientChip[] sortedRecipients;
        StringBuilder sb = new StringBuilder();
        sb.append("[expand] start, pending chip count: ");
        sb.append(this.mPedingReplaceChips.size());
        sb.append(", TemporaryRecipients count: ");
        sb.append(this.mTemporaryRecipients == null ? null : Integer.valueOf(this.mTemporaryRecipients.size()));
        printDebugLog("RecipientEditTextView", sb.toString());
        setForceEnableBringPointIntoView(true);
        if (this.mShouldShrink && !isPhoneQuery()) {
            setMaxLines(Integer.MAX_VALUE);
        }
        if (isPhoneQuery() && (sortedRecipients = getSortedRecipients()) != null && sortedRecipients.length > 0 && this.mHasEllipsizedFirstChip) {
            replaceChipOnSameTextRange(sortedRecipients[0], -1);
            this.mHasEllipsizedFirstChip = false;
        }
        removeMoreChip();
        if (isPhoneQuery() && this.mPedingReplaceChips.size() != 0 && this.mPedingReplaceEntries.size() != 0) {
            this.mDuringReplaceDupChips = true;
            int size = this.mPedingReplaceChips.size();
            for (int i = 0; i < size; i++) {
                DrawableRecipientChip drawableRecipientChip = this.mPedingReplaceChips.get(i);
                RecipientEntry recipientEntry = this.mPedingReplaceEntries.get(i);
                replaceChip(drawableRecipientChip, recipientEntry);
                printDebugLog("RecipientEditTextView", "[expand] Expand and replace contact from " + drawableRecipientChip.getEntry().getContactId() + " to " + recipientEntry.getContactId());
            }
            this.mPedingReplaceChips.clear();
            this.mPedingReplaceEntries.clear();
            this.mDuringReplaceDupChips = false;
        }
        setCursorVisible(true);
        Editable text = getText();
        setSelection((text == null || text.length() <= 0) ? 0 : text.length());
        if (this.mTemporaryRecipients != null && this.mTemporaryRecipients.size() > 0) {
            printDebugLog("RecipientEditTextView", "[expand] execute RecipientReplacementTask, mTemporaryRecipients.size: " + this.mTemporaryRecipients.size());
            new RecipientReplacementTask().execute(new Void[0]);
            clearTemporaryRecipients();
        }
        setForceEnableBringPointIntoView(false);
        setDisableBringPointIntoView(false);
        if (this.mNoChips) {
            this.mJustExpanded = true;
        }
    }

    private void addTemporaryRecipients(DrawableRecipientChip drawableRecipientChip) {
        if (this.mTemporaryRecipients == null) {
            this.mTemporaryRecipients = new ArrayList<>();
        }
        this.mTemporaryRecipients.add(drawableRecipientChip);
        printDebugLog("RecipientEditTextView", "[addItemToTemporaryRecipients] count: " + this.mTemporaryRecipients.size());
    }

    private void clearTemporaryRecipients() {
        printDebugLog("RecipientEditTextView", "[clearTemporaryRecipients]");
        this.mTemporaryRecipients = null;
    }

    private CharSequence ellipsizeText(CharSequence charSequence, TextPaint textPaint, float f) {
        textPaint.setTextSize(this.mChipFontSize);
        if (f <= 0.0f && Log.isLoggable("RecipientEditTextView", 3)) {
            Log.d("RecipientEditTextView", "Max width is negative: " + f);
        }
        return TextUtils.ellipsize(charSequence, textPaint, f, TextUtils.TruncateAt.END);
    }

    private Bitmap createSelectedChip(RecipientEntry recipientEntry, TextPaint textPaint) {
        int i = (int) this.mChipHeight;
        float[] fArr = new float[1];
        textPaint.getTextWidths(" ", fArr);
        float f = i;
        CharSequence charSequenceEllipsizeText = ellipsizeText(createChipDisplayText(recipientEntry), textPaint, (calculateAvailableWidth() - f) - fArr[0]);
        printDebugLog("RecipientEditTextView", "[createSelectedChip] " + ((Object) charSequenceEllipsizeText));
        int iMax = Math.max(0, ((int) Math.floor((double) textPaint.measureText(charSequenceEllipsizeText, 0, charSequenceEllipsizeText.length()))) + (this.mChipPadding * 2) + i);
        Bitmap bitmapCreateBitmap = Bitmap.createBitmap(iMax, i, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmapCreateBitmap);
        if (this.mChipBackgroundPressed != null) {
            textPaint.setARGB(250, 118, 178, 14);
            float f2 = i / 2;
            canvas.drawRoundRect(new RectF(0.0f, 0.0f, iMax, f), f2, f2, textPaint);
            this.mChipBackgroundPressed.draw(canvas);
            textPaint.setColor(sSelectedTextColor);
            canvas.drawText(charSequenceEllipsizeText, 0, charSequenceEllipsizeText.length(), this.mChipPadding, getTextYOffset((String) charSequenceEllipsizeText, textPaint, i), textPaint);
            Rect rect = new Rect();
            this.mChipBackgroundPressed.getPadding(rect);
            Bitmap bitmap = ((BitmapDrawable) this.mChipDelete).getBitmap();
            this.mIconWidth = bitmap.getWidth();
            this.mChipDelete = new BitmapDrawable(toRoundBitmap(bitmap));
            this.mChipDelete.setBounds((iMax - i) + rect.left, 0 + rect.top, iMax - rect.right, i - rect.bottom);
            this.mChipDelete.draw(canvas);
        } else {
            Log.w("RecipientEditTextView", "Unable to draw a background for the chips as it was never set");
        }
        return bitmapCreateBitmap;
    }

    private Bitmap createUnselectedChip(RecipientEntry recipientEntry, TextPaint textPaint, boolean z) {
        float fCalculateAvailableWidth;
        Bitmap bitmap;
        int i;
        Bitmap bitmap2;
        Matrix matrix;
        int i2;
        boolean z2;
        Bitmap bitmapDecodeByteArray;
        Bitmap roundBitmap;
        int i3 = (int) this.mChipHeight;
        boolean z3 = true;
        float[] fArr = new float[1];
        textPaint.getTextWidths(" ", fArr);
        String strCreateChipDisplayText = createChipDisplayText(recipientEntry);
        if (this.mLimitedWidthForSpan == -1) {
            fCalculateAvailableWidth = (calculateAvailableWidth() - i3) - fArr[0];
        } else {
            fCalculateAvailableWidth = (this.mLimitedWidthForSpan - i3) - fArr[0];
        }
        CharSequence charSequenceEllipsizeText = ellipsizeText(strCreateChipDisplayText, textPaint, fCalculateAvailableWidth);
        printSensitiveDebugLog("RecipientEditTextView", "[createUnselectedChip] start, " + ((Object) charSequenceEllipsizeText) + ", ID: " + recipientEntry.getContactId());
        int iFloor = (int) Math.floor((double) textPaint.measureText(charSequenceEllipsizeText, 0, charSequenceEllipsizeText.length()));
        int i4 = (this.mChipPadding * 2) + iFloor;
        Drawable chipBackground = getChipBackground(recipientEntry);
        if (chipBackground != null) {
            long contactId = recipientEntry.getContactId();
            if (isPhoneQuery()) {
                int i5 = (contactId > (-1L) ? 1 : (contactId == (-1L) ? 0 : -1));
            } else if (contactId != -1 && contactId != -2) {
                TextUtils.isEmpty(recipientEntry.getDisplayName());
            }
            if (contactId != -1) {
                byte[] photoBytes = recipientEntry.getPhotoBytes();
                Trace.traceBegin(8L, "getPhoto " + recipientEntry.getContactId());
                if (photoBytes == null && recipientEntry.getPhotoThumbnailUri() != null) {
                    ((BaseRecipientAdapter) getAdapter()).fetchPhoto(recipientEntry, recipientEntry.getPhotoThumbnailUri());
                    photoBytes = recipientEntry.getPhotoBytes();
                }
                Trace.traceEnd(8L);
                Trace.traceBegin(8L, "decodePhoto");
                if (photoBytes != null) {
                    bitmapDecodeByteArray = BitmapFactory.decodeByteArray(photoBytes, 0, photoBytes.length);
                } else {
                    bitmapDecodeByteArray = this.mDefaultContactPhoto;
                }
                Trace.traceEnd(8L);
                if (bitmapDecodeByteArray != null) {
                    i4 = (this.mChipPadding * 2) + iFloor + i3;
                    if (bitmapDecodeByteArray.getWidth() <= bitmapDecodeByteArray.getHeight()) {
                        this.mIconWidth = bitmapDecodeByteArray.getHeight();
                    } else {
                        this.mIconWidth = bitmapDecodeByteArray.getWidth();
                    }
                    RectF rectF = new RectF(0.0f, 0.0f, this.mIconWidth, this.mIconWidth);
                    this.mChipBackground.getPadding(new Rect());
                    RectF rectF2 = new RectF(0.0f, 0 + r12.top, i3, i3 - r12.bottom);
                    matrix = new Matrix();
                    matrix.setRectToRect(rectF, rectF2, Matrix.ScaleToFit.FILL);
                    roundBitmap = toRoundBitmap(bitmapDecodeByteArray);
                } else {
                    z3 = false;
                    matrix = null;
                    roundBitmap = bitmapDecodeByteArray;
                }
                i = i3;
                int i6 = i4;
                z2 = z3;
                bitmap2 = roundBitmap;
                i2 = i6;
            } else if (!z || isPhoneQuery()) {
                i = 0;
                bitmap2 = null;
                matrix = null;
                i2 = i4;
                z2 = false;
            } else {
                i = i3;
                bitmap2 = null;
                i2 = i4;
                z2 = false;
                matrix = null;
            }
            Bitmap bitmapCreateBitmap = Bitmap.createBitmap(i2, i3, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmapCreateBitmap);
            this.mWorkPaint.reset();
            this.mWorkPaint.setARGB(63, 153, 153, 153);
            float f = i3 / 2;
            bitmap = bitmapCreateBitmap;
            canvas.drawRoundRect(new RectF(0.0f, 0.0f, i2, i3), f, f, this.mWorkPaint);
            chipBackground.draw(canvas);
            if (bitmap2 != null && matrix != null) {
                canvas.drawBitmap(bitmap2, matrix, textPaint);
            }
            textPaint.setColor(getContext().getResources().getColor(android.R.color.black));
            if (z2) {
                int i7 = this.mChipPadding;
            } else {
                int i8 = this.mChipPadding;
                int i9 = ((i2 - (this.mChipPadding * 2)) - iFloor) / 2;
            }
            i3 = i3;
            canvas.drawText(charSequenceEllipsizeText, 0, charSequenceEllipsizeText.length(), i + this.mChipPadding, getTextYOffset((String) charSequenceEllipsizeText, textPaint, i3), textPaint);
            i4 = i2;
        } else {
            Log.w("RecipientEditTextView", "Unable to draw a background for the chips as it was never set");
            bitmap = null;
        }
        if (bitmap == null) {
            return Bitmap.createBitmap(i4, i3, Bitmap.Config.ARGB_8888);
        }
        return bitmap;
    }

    private int calculateUnselectedChipWidth(RecipientEntry recipientEntry) {
        float fCalculateAvailableWidth;
        TextPaint textPaint = this.mChipPaint;
        int i = (int) this.mChipHeight;
        boolean z = true;
        float[] fArr = new float[1];
        textPaint.getTextWidths(" ", fArr);
        long contactId = recipientEntry.getContactId();
        if (!isPhoneQuery() ? contactId == -1 || contactId == -2 || TextUtils.isEmpty(recipientEntry.getDisplayName()) : contactId == -1) {
            z = false;
        }
        if (!z) {
            i = 0;
        }
        float textSize = textPaint.getTextSize();
        String strCreateChipDisplayText = createChipDisplayText(recipientEntry);
        if (this.mLimitedWidthForSpan == -1) {
            fCalculateAvailableWidth = (calculateAvailableWidth() - i) - fArr[0];
        } else {
            fCalculateAvailableWidth = (this.mLimitedWidthForSpan - i) - fArr[0];
        }
        CharSequence charSequenceEllipsizeText = ellipsizeText(strCreateChipDisplayText, textPaint, fCalculateAvailableWidth);
        int iFloor = ((int) Math.floor(textPaint.measureText(charSequenceEllipsizeText, 0, charSequenceEllipsizeText.length()))) + (this.mChipPadding * 2) + i;
        textPaint.setTextSize(textSize);
        return iFloor;
    }

    Drawable getChipBackground(RecipientEntry recipientEntry) {
        return (recipientEntry.isValid() && isValid(recipientEntry.getDestination())) ? this.mChipBackground : this.mInvalidChipBackground;
    }

    private static float getTextYOffset(String str, TextPaint textPaint, int i) {
        textPaint.getTextBounds(str, 0, str.length(), new Rect());
        return i - ((i - ((r0.bottom - r0.top) - ((int) textPaint.descent()))) / 2);
    }

    private DrawableRecipientChip constructChipSpan(RecipientEntry recipientEntry, boolean z, boolean z2) throws NullPointerException {
        Bitmap bitmapCreateUnselectedChip;
        printDebugLog("RecipientEditTextView", "[constructChipSpan] pressed: " + z);
        if (this.mChipBackground == null) {
            throw new NullPointerException("Unable to render any chips as setChipDimensions was not called.");
        }
        Trace.traceBegin(8L, "constructChipSpan");
        TextPaint textPaint = this.mChipPaint;
        float textSize = textPaint.getTextSize();
        int color = textPaint.getColor();
        if (z) {
            bitmapCreateUnselectedChip = createSelectedChip(recipientEntry, textPaint);
            this.mBitMapSet.add(bitmapCreateUnselectedChip);
        } else {
            bitmapCreateUnselectedChip = createUnselectedChip(recipientEntry, textPaint, z2);
            this.mBitMapSet.add(bitmapCreateUnselectedChip);
        }
        BitmapDrawable bitmapDrawable = new BitmapDrawable(getResources(), bitmapCreateUnselectedChip);
        bitmapDrawable.setBounds(0, 0, bitmapCreateUnselectedChip.getWidth(), bitmapCreateUnselectedChip.getHeight());
        VisibleRecipientChip visibleRecipientChip = new VisibleRecipientChip(bitmapDrawable, recipientEntry);
        textPaint.setTextSize(textSize);
        textPaint.setColor(color);
        Trace.traceEnd(8L);
        return visibleRecipientChip;
    }

    private int calculateOffsetFromBottom(int i) {
        return (-(((getLineCount() - (i + 1)) * ((int) this.mChipHeight)) + getPaddingBottom() + getPaddingTop())) + getDropDownVerticalOffset();
    }

    private float calculateAvailableWidth() {
        return ((getWidth() - getPaddingLeft()) - getPaddingRight()) - (this.mChipPadding * 2);
    }

    private void setChipDimensions(Context context, AttributeSet attributeSet) {
        TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R.styleable.RecipientEditTextView, 0, 0);
        Resources resources = getContext().getResources();
        this.mChipBackground = typedArrayObtainStyledAttributes.getDrawable(R.styleable.RecipientEditTextView_chipBackground);
        if (this.mChipBackground == null) {
            this.mChipBackground = resources.getDrawable(R.drawable.chip_background);
        }
        this.mChipBackgroundPressed = typedArrayObtainStyledAttributes.getDrawable(R.styleable.RecipientEditTextView_chipBackgroundPressed);
        if (this.mChipBackgroundPressed == null) {
            this.mChipBackgroundPressed = resources.getDrawable(R.drawable.chip_background_selected);
        }
        this.mChipDelete = typedArrayObtainStyledAttributes.getDrawable(R.styleable.RecipientEditTextView_chipDelete);
        if (this.mChipDelete == null) {
            this.mChipDelete = resources.getDrawable(R.drawable.chip_delete);
        }
        this.mChipPadding = typedArrayObtainStyledAttributes.getDimensionPixelSize(R.styleable.RecipientEditTextView_chipPadding, -1);
        if (this.mChipPadding == -1) {
            this.mChipPadding = (int) resources.getDimension(R.dimen.chip_padding);
        }
        this.mAlternatesLayout = typedArrayObtainStyledAttributes.getResourceId(R.styleable.RecipientEditTextView_chipAlternatesLayout, -1);
        if (this.mAlternatesLayout == -1) {
            this.mAlternatesLayout = R.layout.chips_alternate_item;
        }
        this.mDefaultContactPhoto = BitmapFactory.decodeResource(resources, R.drawable.ic_default_contact);
        this.mMoreItem = (TextView) LayoutInflater.from(getContext()).inflate(R.layout.more_item, (ViewGroup) null);
        this.mChipHeight = typedArrayObtainStyledAttributes.getDimensionPixelSize(R.styleable.RecipientEditTextView_chipHeight, -1);
        if (this.mChipHeight == -1.0f) {
            this.mChipHeight = resources.getDimension(R.dimen.chip_height);
        }
        this.mChipFontSize = typedArrayObtainStyledAttributes.getDimensionPixelSize(R.styleable.RecipientEditTextView_chipFontSize, -1);
        if (this.mChipFontSize == -1.0f) {
            this.mChipFontSize = resources.getDimension(R.dimen.chip_text_size);
        }
        this.mInvalidChipBackground = typedArrayObtainStyledAttributes.getDrawable(R.styleable.RecipientEditTextView_invalidChipBackground);
        if (this.mInvalidChipBackground == null) {
            this.mInvalidChipBackground = resources.getDrawable(R.drawable.chip_background_invalid);
        }
        this.mLineSpacingExtra = context.getResources().getDimension(R.dimen.line_spacing_extra);
        this.mMaxLines = resources.getInteger(R.integer.chips_max_lines);
        TypedValue typedValue = new TypedValue();
        if (context.getTheme().resolveAttribute(android.R.attr.actionBarSize, typedValue, true)) {
            this.mActionBarHeight = TypedValue.complexToDimensionPixelSize(typedValue.data, getResources().getDisplayMetrics());
        }
        typedArrayObtainStyledAttributes.recycle();
    }

    public void setOnFocusListShrinkRecipients(boolean z) {
        this.mShouldShrink = z;
    }

    @Override
    public void onSizeChanged(int i, int i2, int i3, int i4) {
        super.onSizeChanged(i, i2, i3, i4);
        printDebugLog("RecipientEditTextView", "[onSizeChanged] w: " + i + ", h: " + i2 + ", oldw: " + i3 + ", oldh: " + i4);
        if (i != 0 && i2 != 0) {
            if (this.mPendingChipsCount > 0) {
                postHandlePendingChips();
            } else {
                checkChipWidths();
            }
        }
        setDisableBringPointIntoView(false);
        bringPointIntoView(getSelectionStart());
        if (this.mScrollView == null && !this.mTriedGettingScrollView) {
            ViewParent parent = getParent();
            while (parent != null && !(parent instanceof ScrollView)) {
                parent = parent.getParent();
            }
            if (parent != null) {
                this.mScrollView = (ScrollView) parent;
            }
            this.mTriedGettingScrollView = true;
        }
    }

    private void postHandlePendingChips() {
        printDebugLog("RecipientEditTextView", "[postHandlePendingChips] count: " + this.mPendingChipsCount);
        this.mHandler.removeCallbacks(this.mHandlePendingChips);
        this.mHandler.post(this.mHandlePendingChips);
    }

    private void checkChipWidths() {
        DrawableRecipientChip[] sortedRecipients = getSortedRecipients();
        if (sortedRecipients != null) {
            for (DrawableRecipientChip drawableRecipientChip : sortedRecipients) {
                Rect bounds = drawableRecipientChip.getBounds();
                if (getWidth() > 0 && bounds.right - bounds.left > (getWidth() - getPaddingLeft()) - getPaddingRight()) {
                    replaceChip(drawableRecipientChip, drawableRecipientChip.getEntry());
                }
            }
        }
    }

    void handlePendingChips() {
        int spanEnd;
        boolean z;
        printDebugLog("RecipientEditTextView", "[handlePendingChips] Start, pending chips count: " + this.mPendingChipsCount);
        setForceEnableBringPointIntoView(true);
        if (getViewWidth() <= 0) {
            printDebugLog("RecipientEditTextView", "[handlePendingChips] getViewWidth() <= 0, return");
            return;
        }
        synchronized (this.mPendingChips) {
            if (this.mPendingChipsCount <= 0) {
                return;
            }
            if (this.mPendingChipsCount <= 100) {
                SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(getText());
                int i = 0;
                int i2 = 0;
                while (i < this.mPendingChips.size()) {
                    String str = this.mPendingChips.get(i);
                    int iIndexOf = spannableStringBuilder.toString().indexOf(str, i2);
                    int length = (str.length() + iIndexOf) - 1;
                    printDebugLog("RecipientEditTextView", "[handlePendingChips] index: " + i + ", " + str + ", tokenStart:" + iIndexOf + ", tokenEnd:" + length);
                    if (iIndexOf >= 0) {
                        if (length < spannableStringBuilder.length() - 2 && spannableStringBuilder.charAt(length) == ',') {
                            length++;
                        }
                        Trace.traceBegin(8L, "createRepChip");
                        createReplacementChip(iIndexOf, length, spannableStringBuilder, i < this.mChipLimit || !this.mShouldShrink);
                        Trace.traceEnd(8L);
                    }
                    DrawableRecipientChip[] drawableRecipientChipArr = (DrawableRecipientChip[]) spannableStringBuilder.getSpans(iIndexOf, spannableStringBuilder.length(), DrawableRecipientChip.class);
                    if (drawableRecipientChipArr != null && drawableRecipientChipArr.length > 0) {
                        int i3 = 0;
                        while (true) {
                            if (i3 < drawableRecipientChipArr.length) {
                                if (spannableStringBuilder.getSpanStart(drawableRecipientChipArr[i3]) != iIndexOf) {
                                    i3++;
                                } else {
                                    spanEnd = spannableStringBuilder.getSpanEnd(drawableRecipientChipArr[i3]);
                                    z = true;
                                    break;
                                }
                            } else {
                                spanEnd = i2;
                                z = false;
                                break;
                            }
                        }
                        if (!z) {
                            spanEnd = 0;
                        }
                        i2 = spanEnd;
                    } else {
                        i2 = 0;
                    }
                    this.mPendingChipsCount--;
                    i++;
                }
                setText(spannableStringBuilder);
                sanitizeBetween();
                sanitizeEnd();
            } else {
                this.mNoChips = true;
            }
            if (this.mStringToBeRestore != null) {
                Log.d("RecipientEditTextView", "[handlePendingChips] Restore text ->" + this.mStringToBeRestore);
                getText().append(this.mStringToBeRestore);
                this.mStringToBeRestore = null;
            }
            StringBuilder sb = new StringBuilder();
            sb.append("[handlePendingChips] phase 1 completed, mTemporaryRecipients.size: ");
            sb.append(this.mTemporaryRecipients == null ? 0 : this.mTemporaryRecipients.size());
            printDebugLog("RecipientEditTextView", sb.toString());
            if (this.mTemporaryRecipients != null && this.mTemporaryRecipients.size() > 0 && this.mTemporaryRecipients.size() <= 100) {
                if (hasFocus()) {
                    printDebugLog("RecipientEditTextView", "[handlePendingChips] execute RecipientReplacementTask, count: " + this.mTemporaryRecipients.size());
                    new RecipientReplacementTask().execute(new Void[0]);
                    this.mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            MTKRecipientEditTextView.this.setForceEnableBringPointIntoView(false);
                        }
                    });
                    clearTemporaryRecipients();
                } else {
                    int iMin = Math.min(this.mTemporaryRecipients.size(), calculateNumChipsCanShow());
                    DrawableRecipientChip[] sortedRecipients = getSortedRecipients();
                    if (sortedRecipients != null && sortedRecipients.length <= iMin) {
                        printDebugLog("RecipientEditTextView", "[handlePendingChips] execute RecipientReplacementTask, count: " + this.mTemporaryRecipients.size());
                        new RecipientReplacementTask().execute(new Void[0]);
                        clearTemporaryRecipients();
                    } else {
                        printDebugLog("RecipientEditTextView", "[handlePendingChips] execute IndividualReplacementTask, count: " + this.mTemporaryRecipients.size() + ", canShow: " + iMin);
                        if (!isPhoneQuery() && iMin < this.mChipLimit && this.mTemporaryRecipients.size() >= this.mChipLimit) {
                            iMin = this.mChipLimit;
                        }
                        this.mIndividualReplacements = new IndividualReplacementTask();
                        this.mIndividualReplacements.execute(new ArrayList(this.mTemporaryRecipients.subList(0, iMin)));
                        if (this.mTemporaryRecipients.size() > iMin) {
                            this.mTemporaryRecipients = new ArrayList<>(this.mTemporaryRecipients.subList(iMin, this.mTemporaryRecipients.size()));
                            printDebugLog("RecipientEditTextView", "[handlePendingChips] update mTemporaryRecipients count: " + this.mTemporaryRecipients.size() + ", canShow: " + iMin);
                        } else {
                            clearTemporaryRecipients();
                        }
                        createMoreChip();
                    }
                }
            } else {
                StringBuilder sb2 = new StringBuilder();
                sb2.append("[handlePendingChips] fall back to show addresses. count: ");
                sb2.append(this.mTemporaryRecipients == null ? 0 : this.mTemporaryRecipients.size());
                printDebugLog("RecipientEditTextView", sb2.toString());
                clearTemporaryRecipients();
                if (!hasFocus() && this.mMoreChip == null) {
                    createMoreChip();
                }
                setForceEnableBringPointIntoView(false);
            }
            this.mPendingChipsCount = 0;
            this.mPendingChips.clear();
        }
    }

    int getViewWidth() {
        return getWidth();
    }

    void sanitizeEnd() {
        int spanEnd;
        if (this.mPendingChipsCount > 0) {
            return;
        }
        DrawableRecipientChip[] sortedRecipients = getSortedRecipients();
        Spannable spannable = getSpannable();
        if (sortedRecipients != null && sortedRecipients.length > 0) {
            this.mMoreChip = getMoreChip();
            if (this.mMoreChip != null) {
                spanEnd = spannable.getSpanEnd(this.mMoreChip);
            } else {
                spanEnd = getSpannable().getSpanEnd(getLastChip());
            }
            Editable text = getText();
            int length = text.length();
            if (length > spanEnd) {
                printThreadingDebugLog("MTKRecip", "[sanitizeEnd] delete " + ((Object) text));
                text.delete(spanEnd + 1, length);
            }
        }
    }

    private void createReplacementChip(int i, int i2, Editable editable, boolean z) {
        DrawableRecipientChip invisibleRecipientChip;
        printDebugLog("RecipientEditTextView", "[createReplacementChip] tokenStart:" + i + ", tokenEnd:" + i2 + ", visible:" + z);
        if (alreadyHasChip(i, i2)) {
            return;
        }
        String strSubstring = editable.toString().substring(i, i2);
        String strTrim = strSubstring.trim();
        int iLastIndexOf = strTrim.lastIndexOf(44);
        boolean z2 = true;
        if (iLastIndexOf != -1 && iLastIndexOf == strTrim.length() - 1) {
            strSubstring = strTrim.substring(0, strTrim.length() - 1);
        }
        RecipientEntry recipientEntryCreateTokenizedEntry = createTokenizedEntry(strSubstring);
        if (recipientEntryCreateTokenizedEntry != null) {
            DrawableRecipientChip drawableRecipientChip = null;
            setChipProcessingMode(PROCESSING_MODE.COMMIT);
            try {
                if (!this.mNoChips) {
                    if (!TextUtils.isEmpty(recipientEntryCreateTokenizedEntry.getDisplayName()) && !TextUtils.equals(recipientEntryCreateTokenizedEntry.getDisplayName(), recipientEntryCreateTokenizedEntry.getDestination())) {
                        z2 = false;
                    }
                    if (z) {
                        invisibleRecipientChip = constructChipSpan(recipientEntryCreateTokenizedEntry, false, z2);
                    } else {
                        invisibleRecipientChip = new InvisibleRecipientChip(recipientEntryCreateTokenizedEntry);
                    }
                    drawableRecipientChip = invisibleRecipientChip;
                }
            } catch (NullPointerException e) {
                Log.e("RecipientEditTextView", e.getMessage(), e);
            }
            printThreadingDebugLog("MTKRecip", "[createReplacementChip] replace");
            editable.setSpan(drawableRecipientChip, i, i2, 33);
            if (drawableRecipientChip != null) {
                drawableRecipientChip.setOriginalText(strTrim);
                addTemporaryRecipients(drawableRecipientChip);
            }
            setChipProcessingMode(PROCESSING_MODE.NONE);
            if ((mFeatureSet & 2) != 0) {
                this.changedChipAddresses.add(recipientEntryCreateTokenizedEntry.getDestination());
            }
        }
    }

    private static boolean isPhoneNumber(String str) {
        if (TextUtils.isEmpty(str)) {
            return false;
        }
        return PHONE_EX.matcher(str).matches();
    }

    private RecipientEntry createTokenizedEntry(String str) {
        String string;
        printSensitiveDebugLog("RecipientEditTextView", "[createTokenizedEntry] token:" + str);
        if (TextUtils.isEmpty(str)) {
            return null;
        }
        boolean z = false;
        if (isPhoneQuery() && isPhoneNumber(str)) {
            String strTrim = str.trim();
            if (strTrim.endsWith(",")) {
                strTrim = strTrim.substring(0, strTrim.length() - 1);
            }
            return RecipientEntry.constructFakeEntry(strTrim);
        }
        Rfc822Token[] rfc822TokenArr = Rfc822Tokenizer.tokenize(str);
        boolean zIsValid = isValid(str);
        if (zIsValid && rfc822TokenArr != null && rfc822TokenArr.length > 0) {
            String name = rfc822TokenArr[0].getName();
            if (!TextUtils.isEmpty(name)) {
                printDebugLog("RecipientEditTextView", "[createTokenizedEntry] RecipientEntry.constructGeneratedEntry()");
                return RecipientEntry.constructGeneratedEntry(name, rfc822TokenArr[0].getAddress(), zIsValid);
            }
            String address = rfc822TokenArr[0].getAddress();
            if (!TextUtils.isEmpty(address)) {
                printDebugLog("RecipientEditTextView", "[createTokenizedEntry] RecipientEntry.constructFakeEntry()");
                return RecipientEntry.constructFakeEntry(address, zIsValid);
            }
        }
        if (this.mValidator != null && !zIsValid) {
            string = this.mValidator.fixText(str).toString();
            if (!TextUtils.isEmpty(string)) {
                if (string.contains(str)) {
                    Rfc822Token[] rfc822TokenArr2 = Rfc822Tokenizer.tokenize(string);
                    if (rfc822TokenArr2.length > 0) {
                        string = rfc822TokenArr2[0].getAddress();
                        z = true;
                    } else {
                        z = zIsValid;
                    }
                } else {
                    string = null;
                }
            }
            if (!TextUtils.isEmpty(string)) {
                str = string;
            }
            return RecipientEntry.constructFakeEntry(str, z);
        }
        string = null;
        z = zIsValid;
        if (!TextUtils.isEmpty(string)) {
        }
        return RecipientEntry.constructFakeEntry(str, z);
    }

    private boolean isValid(String str) {
        if (this.mValidator == null) {
            return true;
        }
        return this.mValidator.isValid(str);
    }

    private String tokenizeName(String str) {
        Rfc822Token[] rfc822TokenArr = Rfc822Tokenizer.tokenize(str);
        if (rfc822TokenArr != null && rfc822TokenArr.length > 0) {
            return rfc822TokenArr[0].getName();
        }
        return str;
    }

    private static String tokenizeAddress(String str) {
        Rfc822Token[] rfc822TokenArr = Rfc822Tokenizer.tokenize(str);
        if (rfc822TokenArr != null && rfc822TokenArr.length > 0) {
            return rfc822TokenArr[0].getAddress();
        }
        return str;
    }

    private static String tokenizeAndNormalizeAddress(String str) {
        String str2 = tokenizeAddress(str);
        if (isPhoneNumber(str2)) {
            return PhoneNumberUtils.normalizeNumber(str2);
        }
        return str2;
    }

    @Override
    public void setTokenizer(MultiAutoCompleteTextView.Tokenizer tokenizer) {
        this.mTokenizer = tokenizer;
        super.setTokenizer(this.mTokenizer);
    }

    @Override
    public void setValidator(AutoCompleteTextView.Validator validator) {
        this.mValidator = validator;
        super.setValidator(validator);
    }

    @Override
    protected void replaceText(CharSequence charSequence) {
    }

    @Override
    public boolean onKeyPreIme(int i, KeyEvent keyEvent) {
        if (i == 4 && this.mSelectedChip != null) {
            clearSelectedChip();
            return true;
        }
        return super.onKeyPreIme(i, keyEvent);
    }

    @Override
    public boolean onKeyUp(int i, KeyEvent keyEvent) {
        printDebugLog("RecipientEditTextView", "[onKeyUp] " + keyEvent);
        if (i == 61 && keyEvent.hasNoModifiers()) {
            if (this.mSelectedChip != null) {
                clearSelectedChip();
            } else {
                commitDefault();
            }
        }
        return super.onKeyUp(i, keyEvent);
    }

    private boolean focusNext() {
        View viewFocusSearch = focusSearch(130);
        if (viewFocusSearch != null) {
            viewFocusSearch.requestFocus();
            return true;
        }
        return false;
    }

    private boolean commitDefault() {
        if (this.mTokenizer == null) {
            return false;
        }
        setDisableBringPointIntoView(true);
        Editable text = getText();
        int selectionEnd = getSelectionEnd();
        int iFindTokenStart = this.mTokenizer.findTokenStart(text, selectionEnd);
        boolean zShouldCreateChip = shouldCreateChip(iFindTokenStart, selectionEnd);
        printDebugLog("RecipientEditTextView", "[commitDefault] start: " + iFindTokenStart + ", end: " + selectionEnd + ", shouldCreateChip: " + zShouldCreateChip);
        if (zShouldCreateChip) {
            int iMovePastTerminators = movePastTerminators(this.mTokenizer.findTokenEnd(getText(), iFindTokenStart));
            if (iMovePastTerminators != getSelectionEnd()) {
                handleEdit(iFindTokenStart, iMovePastTerminators);
                return true;
            }
            return commitChip(iFindTokenStart, selectionEnd, text);
        }
        this.mLastStringChanged = true;
        registerVSync();
        return false;
    }

    private void commitByCharacter() {
        if (this.mTokenizer == null) {
            return;
        }
        Editable text = getText();
        int selectionEnd = getSelectionEnd();
        int iFindTokenStart = this.mTokenizer.findTokenStart(text, selectionEnd);
        boolean zShouldCreateChip = shouldCreateChip(iFindTokenStart, selectionEnd);
        printDebugLog("RecipientEditTextView", "[commitByCharacter] start: " + iFindTokenStart + ", end: " + selectionEnd + ", shouldCreateChip: " + zShouldCreateChip);
        if (selectionEnd - iFindTokenStart > 1 && zShouldCreateChip) {
            commitChip(iFindTokenStart, selectionEnd, text);
        }
        setSelection(getText().length());
    }

    private boolean commitChip(int i, int i2, Editable editable) {
        int count;
        char cCharAt;
        ListAdapter adapter = getAdapter();
        if (adapter != null && adapter.getCount() > 0 && enoughToFilter() && i2 == getSelectionEnd() && !isPhoneQuery()) {
            printDebugLog("RecipientEditTextView", "[commitChip] submit 1st item, start: " + i + ", end: " + i2);
            submitItemAtPosition(0);
            dismissDropDown();
            return true;
        }
        int iFindTokenEnd = this.mTokenizer.findTokenEnd(editable, i);
        int i3 = iFindTokenEnd + 1;
        if (editable.length() > i3 && ((cCharAt = editable.charAt(i3)) == ',' || cCharAt == ';' || cCharAt == 65292 || cCharAt == 65307)) {
            iFindTokenEnd = i3;
        }
        String strTrim = editable.toString().substring(i, iFindTokenEnd).trim();
        printSensitiveDebugLog("RecipientEditTextView", "[commitChip] trying to match item. text: " + strTrim + ", start: " + i + ", end: " + i2);
        if (isPhoneQuery() && adapter != null && enoughToFilter() && i2 == getSelectionEnd() && (count = getAdapter().getCount()) > 0) {
            for (int i4 = 0; i4 < count; i4++) {
                RecipientEntry recipientEntry = (RecipientEntry) getAdapter().getItem(i4);
                String lowerCase = recipientEntry.getDisplayName().toLowerCase();
                if (strTrim.equals(recipientEntry.getDestination()) || strTrim.toLowerCase().equals(lowerCase)) {
                    printDebugLog("RecipientEditTextView", "[commitChip] submit item: " + i4);
                    submitItemAtPosition(i4);
                    dismissDropDown();
                    return true;
                }
            }
            for (int i5 = 0; i5 < count; i5++) {
                RecipientEntry recipientEntry2 = (RecipientEntry) getAdapter().getItem(i5);
                recipientEntry2.getDisplayName().toLowerCase();
                String destination = recipientEntry2.getDestination();
                if (recipientEntry2.getDestinationKind() == 2 && PhoneNumberUtils.compare(PhoneNumberUtils.normalizeNumber(strTrim), PhoneNumberUtils.normalizeNumber(destination))) {
                    printDebugLog("RecipientEditTextView", "[commitChip] match normalized destination. submit item: " + i5);
                    submitItemAtPosition(i5);
                    dismissDropDown();
                    return true;
                }
            }
        }
        clearComposingText();
        if (strTrim == null || strTrim.length() <= 0 || strTrim.equals(" ")) {
            return false;
        }
        RecipientEntry recipientEntryCreateTokenizedEntry = createTokenizedEntry(strTrim);
        if (recipientEntryCreateTokenizedEntry != null) {
            setChipProcessingMode(PROCESSING_MODE.COMMIT);
            QwertyKeyListener.markAsReplaced(editable, i, i2, "");
            CharSequence charSequenceCreateChip = createChip(recipientEntryCreateTokenizedEntry, false);
            if (charSequenceCreateChip != null && i > -1 && i2 > -1) {
                printThreadingDebugLog("MTKRecip", "[commitChip] replace");
                if (i2 + 1 < editable.length()) {
                    int i6 = i2 + 2;
                    if (TextUtils.equals(editable.toString().substring(i2, i6), ", ")) {
                        i2 = i6;
                    }
                }
                editable.replace(i, i2, charSequenceCreateChip);
            }
            dismissDropDown();
            if (getAdapter().getCount() == 0) {
                this.mPhoneNumberQueryAndReplacementTask = new PhoneNumberQueryAndReplacementTask();
                printSensitiveDebugLog("RecipientEditTextView", "mPhoneNumberQueryAndReplacementTask.execute:  entry = " + recipientEntryCreateTokenizedEntry.getDisplayName());
                this.mPhoneNumberQueryAndReplacementTask.execute(recipientEntryCreateTokenizedEntry);
            }
            setChipProcessingMode(PROCESSING_MODE.NONE);
        }
        if (i2 == getSelectionEnd()) {
            dismissDropDown();
        }
        sanitizeBetween();
        if ((mFeatureSet & 2) != 0) {
            this.changedChipAddresses.add(recipientEntryCreateTokenizedEntry.getDestination());
        }
        return true;
    }

    void sanitizeBetween() {
        DrawableRecipientChip[] sortedRecipients;
        if (this.mPendingChipsCount <= 0 && this.mMoreChip == null && (sortedRecipients = getSortedRecipients()) != null && sortedRecipients.length > 0) {
            DrawableRecipientChip drawableRecipientChip = sortedRecipients[sortedRecipients.length - 1];
            DrawableRecipientChip drawableRecipientChip2 = null;
            if (sortedRecipients.length > 1) {
                drawableRecipientChip2 = sortedRecipients[sortedRecipients.length - 2];
            }
            int spanEnd = 0;
            int spanStart = getSpannable().getSpanStart(drawableRecipientChip);
            if (drawableRecipientChip2 != null) {
                spanEnd = getSpannable().getSpanEnd(drawableRecipientChip2);
                Editable text = getText();
                if (spanEnd == -1 || spanEnd > text.length() - 1) {
                    return;
                }
                if (text.charAt(spanEnd) == ' ') {
                    spanEnd++;
                }
            }
            if (spanEnd >= 0 && spanStart >= 0 && spanEnd < spanStart) {
                printDebugLog("RecipientEditTextView", "[sanitizeBetween] delete, start: " + spanEnd + ", end: " + spanStart);
                getText().delete(spanEnd, spanStart);
            }
        }
    }

    private boolean shouldCreateChip(int i, int i2) {
        return !this.mNoChips && hasFocus() && enoughToFilter() && !alreadyHasChip(i, i2);
    }

    private boolean alreadyHasChip(int i, int i2) {
        if (this.mNoChips) {
            return true;
        }
        DrawableRecipientChip[] drawableRecipientChipArr = (DrawableRecipientChip[]) getSpannable().getSpans(i, i2, DrawableRecipientChip.class);
        return (drawableRecipientChipArr == null || drawableRecipientChipArr.length == 0) ? false : true;
    }

    private void handleEdit(int i, int i2) {
        printDebugLog("RecipientEditTextView", "[handleEdit] start: " + i + ", end: " + i2);
        if (i == -1 || i2 == -1) {
            dismissDropDown();
            return;
        }
        Editable text = getText();
        setSelection(i2);
        String strSubstring = getText().toString().substring(i, i2);
        if (!TextUtils.isEmpty(strSubstring)) {
            RecipientEntry recipientEntryConstructFakeEntry = RecipientEntry.constructFakeEntry(strSubstring, isValid(strSubstring));
            QwertyKeyListener.markAsReplaced(text, i, i2, "");
            CharSequence charSequenceCreateChip = createChip(recipientEntryConstructFakeEntry, false);
            int selectionEnd = getSelectionEnd();
            if (i2 + 1 >= text.length() || TextUtils.equals(text.toString().substring(i2, i2 + 2), ", ")) {
            }
            if (charSequenceCreateChip != null && i > -1 && selectionEnd > -1) {
                printThreadingDebugLog("MTKRecip", "[handleEdit] replace");
                text.replace(i, selectionEnd, charSequenceCreateChip);
            }
            if ((mFeatureSet & 2) != 0) {
                this.changedChipAddresses.add(recipientEntryConstructFakeEntry.getDestination());
            }
        }
        dismissDropDown();
    }

    @Override
    public boolean onKeyDown(int i, KeyEvent keyEvent) {
        if (i == 66 && keyEvent.hasNoModifiers()) {
            return true;
        }
        if ((i == 23 || i == 66) && keyEvent.hasNoModifiers()) {
            if (commitDefault()) {
                return true;
            }
            if (this.mSelectedChip != null) {
                clearSelectedChip();
                return true;
            }
            if (focusNext()) {
                return true;
            }
        }
        if (i == 67) {
            boolean zOnKeyDown = super.onKeyDown(i, keyEvent);
            if (!this.mLastStringChanged && (mFeatureSet & 2) != 0) {
                this.mLastStringChanged = true;
                registerVSync();
            }
            return zOnKeyDown;
        }
        return super.onKeyDown(i, keyEvent);
    }

    Spannable getSpannable() {
        return getText();
    }

    private int getChipStart(DrawableRecipientChip drawableRecipientChip) {
        return getSpannable().getSpanStart(drawableRecipientChip);
    }

    private int getChipEnd(DrawableRecipientChip drawableRecipientChip) {
        return getSpannable().getSpanEnd(drawableRecipientChip);
    }

    @Override
    protected void performFiltering(CharSequence charSequence, int i) {
        if (this.mChipProcessingMode != PROCESSING_MODE.NONE) {
            return;
        }
        boolean zIsCompletedToken = isCompletedToken(charSequence);
        if (enoughToFilter() && charSequence != null && !zIsCompletedToken) {
            int selectionEnd = getSelectionEnd();
            DrawableRecipientChip[] drawableRecipientChipArr = (DrawableRecipientChip[]) getSpannable().getSpans(this.mTokenizer.findTokenStart(charSequence, selectionEnd), selectionEnd, DrawableRecipientChip.class);
            if (drawableRecipientChipArr != null && drawableRecipientChipArr.length > 0) {
                dismissDropDown();
                return;
            }
        } else if (zIsCompletedToken) {
            dismissDropDown();
            return;
        }
        super.performFiltering(charSequence, i);
    }

    @Override
    public void onFilterComplete(int i) {
        if (!this.bTouchedAfterPasted) {
            super.onFilterComplete(i);
        }
        this.bPasted = false;
        this.bTouchedAfterPasted = false;
    }

    boolean isCompletedToken(CharSequence charSequence) {
        if (TextUtils.isEmpty(charSequence)) {
            return false;
        }
        int length = charSequence.length();
        String strTrim = charSequence.toString().substring(this.mTokenizer.findTokenStart(charSequence, length), length).trim();
        if (TextUtils.isEmpty(strTrim)) {
            return false;
        }
        char cCharAt = strTrim.charAt(strTrim.length() - 1);
        return cCharAt == ',' || cCharAt == ';' || cCharAt == 65292 || cCharAt == 65307;
    }

    private void clearSelectedChip() {
        if (this.mSelectedChip != null) {
            printDebugLog("RecipientEditTextView", " clearSelectedChip mSelectedChip = " + this.mSelectedChip);
            unselectChip(this.mSelectedChip);
        }
        setCursorVisible(true);
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        boolean z;
        boolean zShouldShowEditableText;
        float y;
        int action = motionEvent.getAction();
        boolean z2 = false;
        if (action == 0) {
            this.mDownPosY = motionEvent.getY();
            this.mHasScrolled = false;
        } else if (action == 2 && Math.abs(this.mDownPosY - motionEvent.getY()) > 5.0f) {
            this.mHasScrolled = true;
        }
        if (!isFocused()) {
            return super.onTouchEvent(motionEvent);
        }
        if (this.bPasted) {
            this.bTouchedAfterPasted = true;
            dismissDropDown();
        }
        if (this.mForceEnableBringPointIntoView) {
            return super.onTouchEvent(motionEvent);
        }
        int iPutOffsetInRange = -1;
        int action2 = motionEvent.getAction();
        DrawableRecipientChip drawableRecipientChipFindChip = null;
        float x = -1.0f;
        if (action2 == 1) {
            x = motionEvent.getX();
            y = motionEvent.getY();
            if (x < getTotalPaddingLeft() || getWidth() - getTotalPaddingRight() < x || y < getTotalPaddingTop() || getHeight() - getTotalPaddingBottom() < y) {
                Log.d("RecipientEditTextView", "setDisableBringPointIntoView(true) in the onTouchEvent() when out of View");
                setDisableBringPointIntoView(true);
                printDebugLog("RecipientEditTextView", "[onTouchEvent] out of view, X: " + x + ", Y: " + y);
                z = true;
            } else {
                z = false;
            }
            if (z && this.mSelectedChip != null) {
                unselectChip(this.mSelectedChip);
            }
            if (this.mCopyAddress == null && !z) {
                iPutOffsetInRange = putOffsetInRange(x, y);
                boolean zIsTouchPointInChip = isTouchPointInChip(x, y);
                boolean zIsTouchPointInChipVertical = isTouchPointInChipVertical(y);
                if (zIsTouchPointInChip && zIsTouchPointInChipVertical) {
                    drawableRecipientChipFindChip = findChip(iPutOffsetInRange);
                }
                if (drawableRecipientChipFindChip != null) {
                    zShouldShowEditableText = shouldShowEditableText(drawableRecipientChipFindChip);
                    if (!zShouldShowEditableText) {
                        super.setShowSoftInputOnFocus(false);
                    }
                } else {
                    zShouldShowEditableText = true;
                }
                StringBuilder sb = new StringBuilder();
                sb.append(" setDisableBringPointIntoView(result) in the onTouchEvent() when result = ");
                sb.append(zIsTouchPointInChip || this.mSelectedChip != null);
                Log.d("RecipientEditTextView", sb.toString());
                setDisableBringPointIntoView(zIsTouchPointInChip || this.mSelectedChip != null);
            } else {
                zShouldShowEditableText = true;
            }
        } else {
            z = false;
            zShouldShowEditableText = true;
            y = -1.0f;
        }
        boolean zOnTouchEvent = super.onTouchEvent(motionEvent);
        if (this.mSelectedChip == null) {
            this.mGestureDetector.onTouchEvent(motionEvent);
        }
        if (getEnableDiscardNextActionUp() && action2 == 1) {
            setEnableDiscardNextActionUp(false);
            if (!zShouldShowEditableText) {
                super.setShowSoftInputOnFocus(true);
            }
            return zOnTouchEvent;
        }
        if (this.mCopyAddress == null && action2 == 1) {
            printDebugLog("RecipientEditTextView", "[onTouchEvent] ACTION_UP");
            if (isPhoneQuery() && this.mMoveCursorToVisible) {
                this.mMoveCursorToVisible = false;
                if (!zShouldShowEditableText) {
                    super.setShowSoftInputOnFocus(true);
                }
                return true;
            }
            if (!z && drawableRecipientChipFindChip != null) {
                if (action2 == 1 && !this.mHasScrolled) {
                    if (this.mSelectedChip != null && this.mSelectedChip != drawableRecipientChipFindChip) {
                        clearSelectedChip();
                        this.mSelectedChip = selectChip(drawableRecipientChipFindChip);
                    } else if (this.mSelectedChip == null) {
                        setSelection(getText().length());
                        commitDefault();
                        this.mSelectedChip = selectChip(drawableRecipientChipFindChip);
                    } else {
                        onClick(this.mSelectedChip, iPutOffsetInRange, x, y);
                    }
                    if (this.mSelectedChip != null) {
                        this.mLineOfSelectedChip = getLineOfChip(this.mSelectedChip);
                    }
                }
                z2 = true;
                zOnTouchEvent = true;
            } else if (this.mSelectedChip != null && shouldShowEditableText(this.mSelectedChip)) {
                z2 = true;
            }
        }
        if (action2 == 1 && !z2) {
            clearSelectedChip();
        }
        if ((action2 == 3 || this.mHasScrolled) && this.mSelectedChip != null) {
            clearSelectedChip();
        }
        if (!zShouldShowEditableText) {
            super.setShowSoftInputOnFocus(true);
        }
        return zOnTouchEvent;
    }

    private void scrollLineIntoView(int i) {
        if (this.mScrollView != null) {
            this.mScrollView.smoothScrollBy(0, calculateOffsetFromBottom(i));
        }
    }

    private void adjustAnchorView(DrawableRecipientChip drawableRecipientChip) {
        if (this.mAnchorView == null) {
            this.mAnchorView = new View(getContext());
            this.mAnchorView.setVisibility(4);
            ((ViewGroup) getRootView()).addView(this.mAnchorView);
        }
        int[] iArr = new int[2];
        getLocationInWindow(iArr);
        int paddingTop = ((iArr[1] + getPaddingTop()) + getLayout().getLineTop(getLineOfChip(drawableRecipientChip))) - getScrollY();
        int paddingLeft = ((iArr[0] + getPaddingLeft()) + ((int) getLayout().getPrimaryHorizontal(getChipStart(drawableRecipientChip)))) - getScrollX();
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(1, (int) this.mChipHeight, 112);
        layoutParams.setMargins(paddingLeft - 1, paddingTop + 28, 0, 0);
        this.mAnchorView.setLayoutParams(layoutParams);
    }

    private void showAlternates(final DrawableRecipientChip drawableRecipientChip, final ListPopupWindow listPopupWindow, final int i) {
        StringBuilder sb = new StringBuilder();
        sb.append("[showAlternates] ");
        sb.append((Object) (drawableRecipientChip == null ? null : drawableRecipientChip.getValue()));
        printDebugLog("RecipientEditTextView", sb.toString());
        if (this.mShowAlternatesTask != null) {
            this.mShowAlternatesTask.cancel(true);
        }
        adjustAnchorView(drawableRecipientChip);
        this.mShowAlternatesTask = new AsyncTask<Void, Void, ListAdapter>() {
            @Override
            protected ListAdapter doInBackground(Void... voidArr) {
                return MTKRecipientEditTextView.this.createAlternatesAdapter(drawableRecipientChip);
            }

            @Override
            protected void onPostExecute(ListAdapter listAdapter) {
                if (!MTKRecipientEditTextView.this.mAttachedToWindow) {
                    MTKRecipientEditTextView.this.printDebugLog("RecipientEditTextView", "[mShowAlternatesTask][onPostExecute] !mAttachedToWindow, return");
                    return;
                }
                MTKRecipientEditTextView.this.printDebugLog("RecipientEditTextView", "[mShowAlternatesTask][onPostExecute]");
                MTKRecipientEditTextView.this.mHandlingAlternatesDropDown = false;
                listPopupWindow.setWidth(i);
                listPopupWindow.setAnchorView(MTKRecipientEditTextView.this.mAnchorView);
                listPopupWindow.setAdapter(listAdapter);
                listPopupWindow.setOnItemClickListener(MTKRecipientEditTextView.this.mAlternatesListener);
                MTKRecipientEditTextView.this.mCheckedItem = -1;
                listPopupWindow.show();
                ListView listView = listPopupWindow.getListView();
                listView.setChoiceMode(1);
                if (MTKRecipientEditTextView.this.mCheckedItem != -1) {
                    Log.d("RecipientEditTextView", " alternatesPopup setItemChecked mCheckedItem = " + MTKRecipientEditTextView.this.mCheckedItem);
                    listView.setItemChecked(MTKRecipientEditTextView.this.mCheckedItem, true);
                    MTKRecipientEditTextView.this.mCheckedItem = -1;
                }
                MTKRecipientEditTextView.this.mShowAlternatesTask = null;
            }
        };
        this.mShowAlternatesTask.execute((Void[]) null);
    }

    private ListAdapter createAlternatesAdapter(DrawableRecipientChip drawableRecipientChip) {
        Log.d("RecipientEditTextView", "createAlternatesAdapter start getShowPhoneAndEmail() = " + ((BaseRecipientAdapter) getAdapter()).getShowPhoneAndEmail());
        if (isPhoneQuery()) {
            return new RecipientAlternatesAdapter(getContext(), drawableRecipientChip.getContactId(), drawableRecipientChip.getDataId(), ((BaseRecipientAdapter) getAdapter()).getQueryType(), this, ((BaseRecipientAdapter) getAdapter()).getShowPhoneAndEmail());
        }
        return new RecipientAlternatesAdapter(getContext(), drawableRecipientChip.getContactId(), drawableRecipientChip.getDataId(), ((BaseRecipientAdapter) getAdapter()).getQueryType(), this);
    }

    private ListAdapter createSingleAddressAdapter(DrawableRecipientChip drawableRecipientChip) {
        return new SingleRecipientArrayAdapter(getContext(), this.mAlternatesLayout, drawableRecipientChip.getEntry());
    }

    @Override
    public void onCheckedItemChanged(int i) {
        ListView listView = this.mAlternatesPopup.getListView();
        if (listView == null) {
            Log.w("RecipientEditTextView", " onCheckedItemChanged listView = null");
        }
        if (listView != null && listView.getCheckedItemCount() != 0) {
            Log.d("RecipientEditTextView", " onCheckedItemChanged FAIL  " + listView.getCheckedItemCount());
        }
        if (listView != null) {
            listView.setItemChecked(i, true);
            Log.d("RecipientEditTextView", " onCheckedItemChanged mCheckedItem = " + this.mCheckedItem + " ; position = " + i);
        }
        this.mCheckedItem = i;
    }

    private int putOffsetInRange(float f, float f2) {
        int iSupportGetOffsetForPosition;
        if (Build.VERSION.SDK_INT >= 14) {
            iSupportGetOffsetForPosition = getOffsetForPosition(f, f2);
        } else {
            iSupportGetOffsetForPosition = supportGetOffsetForPosition(f, f2);
        }
        return putOffsetInRange(iSupportGetOffsetForPosition);
    }

    private int putOffsetInRange(int i) {
        Editable text = getText();
        int length = text.length();
        for (int i2 = length - 1; i2 >= 0 && text.charAt(i2) == ' '; i2--) {
            length--;
        }
        if (i >= length) {
            return i;
        }
        Editable text2 = getText();
        while (i >= 0 && findText(text2, i) == -1 && findChip(i) == null) {
            i--;
        }
        return i;
    }

    private static int findText(Editable editable, int i) {
        if (editable.charAt(i) != ' ') {
            return i;
        }
        return -1;
    }

    private DrawableRecipientChip findChip(int i) {
        for (DrawableRecipientChip drawableRecipientChip : (DrawableRecipientChip[]) getSpannable().getSpans(0, getText().length(), DrawableRecipientChip.class)) {
            int chipStart = getChipStart(drawableRecipientChip);
            int chipEnd = getChipEnd(drawableRecipientChip);
            if (i >= chipStart && i <= chipEnd) {
                return drawableRecipientChip;
            }
        }
        return null;
    }

    String createAddressText(RecipientEntry recipientEntry) {
        String strTrim;
        Rfc822Token[] rfc822TokenArr;
        String displayName = recipientEntry.getDisplayName();
        String destination = recipientEntry.getDestination();
        if (TextUtils.isEmpty(displayName) || TextUtils.equals(displayName, destination)) {
            displayName = null;
        }
        if (isPhoneQuery() && isPhoneNumber(destination)) {
            strTrim = destination.trim();
        } else {
            if (destination != null && (rfc822TokenArr = Rfc822Tokenizer.tokenize(destination)) != null && rfc822TokenArr.length > 0) {
                destination = rfc822TokenArr[0].getAddress();
            }
            strTrim = new Rfc822Token(displayName, destination, null).toString().trim();
        }
        int iIndexOf = strTrim.indexOf(",");
        if (this.mTokenizer == null || TextUtils.isEmpty(strTrim) || iIndexOf >= strTrim.length() - 1) {
            return strTrim;
        }
        return (String) this.mTokenizer.terminateToken(strTrim);
    }

    String createChipDisplayText(RecipientEntry recipientEntry) {
        String displayName = recipientEntry.getDisplayName();
        String destination = recipientEntry.getDestination();
        if (TextUtils.isEmpty(displayName) || TextUtils.equals(displayName, destination)) {
            displayName = null;
        }
        if (!TextUtils.isEmpty(displayName)) {
            return displayName;
        }
        if (!TextUtils.isEmpty(destination)) {
            String strReplaceAll = destination.replaceAll("([, ]+$)|([; ]+$)", "");
            if (!PHONE_EX.matcher(strReplaceAll).matches()) {
                Rfc822Token[] rfc822TokenArr = Rfc822Tokenizer.tokenize(strReplaceAll);
                return rfc822TokenArr.length > 0 ? rfc822TokenArr[0].getAddress() : strReplaceAll;
            }
            return strReplaceAll;
        }
        return new Rfc822Token(displayName, destination, null).toString();
    }

    private CharSequence createChip(RecipientEntry recipientEntry, boolean z) {
        String strCreateAddressText = createAddressText(recipientEntry);
        printSensitiveDebugLog("RecipientEditTextView", "[createChip] displayText: " + strCreateAddressText + ", pressed: " + z);
        if (TextUtils.isEmpty(strCreateAddressText)) {
            return null;
        }
        int length = strCreateAddressText.length() - 1;
        SpannableString spannableString = new SpannableString(strCreateAddressText);
        if (!this.mNoChips) {
            try {
                DrawableRecipientChip drawableRecipientChipConstructChipSpan = constructChipSpan(recipientEntry, z, false);
                spannableString.setSpan(drawableRecipientChipConstructChipSpan, 0, length, 33);
                drawableRecipientChipConstructChipSpan.setOriginalText(spannableString.toString());
            } catch (NullPointerException e) {
                Log.e("RecipientEditTextView", e.getMessage(), e);
                return null;
            }
        }
        return spannableString;
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long j) {
        if (i < 0) {
            return;
        }
        submitItemAtPosition(i);
    }

    private void submitItemAtPosition(int i) {
        DrawableRecipientChip[] drawableRecipientChipArr;
        RecipientEntry recipientEntryCreateValidatedEntry = createValidatedEntry((RecipientEntry) getAdapter().getItem(i));
        if (recipientEntryCreateValidatedEntry == null) {
            return;
        }
        clearComposingText();
        int selectionEnd = getSelectionEnd();
        int iFindTokenStart = this.mTokenizer.findTokenStart(getText(), selectionEnd);
        Editable text = getText();
        QwertyKeyListener.markAsReplaced(text, iFindTokenStart, selectionEnd, "");
        CharSequence charSequenceCreateChip = createChip(recipientEntryCreateValidatedEntry, false);
        if (charSequenceCreateChip != null && iFindTokenStart >= 0 && selectionEnd >= 0) {
            printThreadingDebugLog("MTKRecip", "[submitItemAtPosition] replace");
            text.replace(iFindTokenStart, selectionEnd, charSequenceCreateChip);
        }
        sanitizeBetween();
        if ((mFeatureSet & 2) != 0) {
            this.changedChipAddresses.add(recipientEntryCreateValidatedEntry.getDestination());
        }
        if (!isPhoneQuery() || charSequenceCreateChip == null || (drawableRecipientChipArr = (DrawableRecipientChip[]) getSpannable().getSpans(iFindTokenStart, (charSequenceCreateChip.length() + iFindTokenStart) - 1, DrawableRecipientChip.class)) == null || drawableRecipientChipArr.length == 0) {
            return;
        }
        new DuplicateContactReplacementTask().execute(drawableRecipientChipArr[0]);
    }

    private RecipientEntry createValidatedEntry(RecipientEntry recipientEntry) {
        if (recipientEntry == null) {
            return null;
        }
        String destination = recipientEntry.getDestination();
        if (recipientEntry.getContactId() == -2) {
            return RecipientEntry.constructGeneratedEntry(recipientEntry.getDisplayName(), destination, recipientEntry.isValid());
        }
        if (RecipientEntry.isCreatedRecipient(recipientEntry.getContactId())) {
            if (TextUtils.isEmpty(recipientEntry.getDisplayName()) || TextUtils.equals(recipientEntry.getDisplayName(), destination) || (this.mValidator != null && !this.mValidator.isValid(destination))) {
                return RecipientEntry.constructFakeEntry(destination, recipientEntry.isValid());
            }
            return recipientEntry;
        }
        return recipientEntry;
    }

    DrawableRecipientChip[] getSortedRecipients() {
        try {
            Object[] spans = getSpannable().getSpans(0, getText().length(), DrawableRecipientChip.class);
            boolean z = false;
            for (Object obj : spans) {
                if (!(obj instanceof DrawableRecipientChip)) {
                    z = true;
                }
            }
            if (z) {
                for (Object obj2 : spans) {
                    tempLogPrint("getSortedRecipients", obj2);
                }
            }
            try {
                ArrayList arrayList = new ArrayList(Arrays.asList((DrawableRecipientChip[]) getSpannable().getSpans(0, getText().length(), DrawableRecipientChip.class)));
                final Spannable spannable = getSpannable();
                Collections.sort(arrayList, new Comparator<DrawableRecipientChip>() {
                    @Override
                    public int compare(DrawableRecipientChip drawableRecipientChip, DrawableRecipientChip drawableRecipientChip2) {
                        int spanStart = spannable.getSpanStart(drawableRecipientChip);
                        int spanStart2 = spannable.getSpanStart(drawableRecipientChip2);
                        if (spanStart < spanStart2) {
                            return -1;
                        }
                        if (spanStart > spanStart2) {
                            return 1;
                        }
                        return 0;
                    }
                });
                return (DrawableRecipientChip[]) arrayList.toArray(new DrawableRecipientChip[arrayList.size()]);
            } catch (ArrayStoreException e) {
                Log.e("RecipientEditTextView", e.getMessage(), e);
                LogPrinter logPrinter = new LogPrinter(3, "RecipientEditTextView");
                logPrinter.println("[getSortedRecipients] spans:");
                TextUtils.dumpSpans(getText(), logPrinter, "  ");
                throw new ArrayStoreException();
            }
        } catch (ArrayStoreException e2) {
            Log.e("RecipientEditTextView", e2.getMessage(), e2);
            LogPrinter logPrinter2 = new LogPrinter(3, "RecipientEditTextView");
            logPrinter2.println("[getSortedRecipients] spans:");
            TextUtils.dumpSpans(getText(), logPrinter2, "  ");
            throw new ArrayStoreException();
        }
    }

    @Override
    public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
        return false;
    }

    @Override
    public void onDestroyActionMode(ActionMode actionMode) {
    }

    @Override
    public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
        return false;
    }

    @Override
    public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
        return false;
    }

    ReplacementDrawableSpan getMoreChip() {
        MoreImageSpan[] moreImageSpanArr = (MoreImageSpan[]) getSpannable().getSpans(0, getText().length(), MoreImageSpan.class);
        if (moreImageSpanArr == null || moreImageSpanArr.length <= 0) {
            return null;
        }
        return moreImageSpanArr[0];
    }

    private MoreImageSpan createMoreSpan(int i) {
        printDebugLog("RecipientEditTextView", "[createMoreSpan] count: " + i);
        String str = String.format(this.mMoreItem.getText().toString(), Integer.valueOf(i));
        TextPaint textPaint = new TextPaint(this.mChipPaint);
        textPaint.setTextSize(this.mMoreItem.getTextSize());
        textPaint.setColor(this.mMoreItem.getCurrentTextColor());
        int iMeasureText = ((int) textPaint.measureText(str)) + this.mMoreItem.getPaddingLeft() + this.mMoreItem.getPaddingRight();
        float textSize = this.mChipPaint.getTextSize();
        this.mChipPaint.setTextSize(this.mDefaultTextSize);
        int lineHeight = getLineHeight();
        Bitmap bitmapCreateBitmap = Bitmap.createBitmap(iMeasureText, lineHeight, Bitmap.Config.ARGB_8888);
        new Canvas(bitmapCreateBitmap).drawText(str, 0, str.length(), 0.0f, getTextYOffset(str, textPaint, lineHeight), (Paint) textPaint);
        BitmapDrawable bitmapDrawable = new BitmapDrawable(getResources(), bitmapCreateBitmap);
        bitmapDrawable.setBounds(0, 0, iMeasureText, lineHeight);
        this.mChipPaint.setTextSize(textSize);
        return new MoreImageSpan(bitmapDrawable);
    }

    void createMoreChipPlainText() {
        Editable text = getText();
        int iMovePastTerminators = 0;
        for (int i = 0; i < this.mChipLimit; i++) {
            iMovePastTerminators = movePastTerminators(this.mTokenizer.findTokenEnd(text, iMovePastTerminators));
        }
        MoreImageSpan moreImageSpanCreateMoreSpan = createMoreSpan(countTokens(text) - this.mChipLimit);
        SpannableString spannableString = new SpannableString(text.subSequence(iMovePastTerminators, text.length()));
        spannableString.setSpan(moreImageSpanCreateMoreSpan, 0, spannableString.length(), 33);
        printThreadingDebugLog("MTKRecip", "[createMoreChipPlainText] replace");
        text.replace(iMovePastTerminators, text.length(), spannableString);
        this.mMoreChip = moreImageSpanCreateMoreSpan;
        if (!isPhoneQuery() && getLineCount() > this.mMaxLines) {
            setMaxLines(getLineCount());
        }
    }

    int countTokens(Editable editable) {
        int iMovePastTerminators = 0;
        int i = 0;
        while (iMovePastTerminators < editable.length()) {
            iMovePastTerminators = movePastTerminators(this.mTokenizer.findTokenEnd(editable, iMovePastTerminators));
            i++;
            if (iMovePastTerminators >= editable.length()) {
                break;
            }
        }
        return i;
    }

    void createMoreChip() {
        int iCalculateNumChipsCanShow;
        if (this.mNoChips) {
            printDebugLog("RecipientEditTextView", "[createMoreChip] mNoChips, return");
            createMoreChipPlainText();
            return;
        }
        if (!this.mShouldShrink) {
            printDebugLog("RecipientEditTextView", "[createMoreChip] !mShouldShrink, return");
            return;
        }
        removeMoreChip();
        DrawableRecipientChip[] sortedRecipients = getSortedRecipients();
        StringBuilder sb = new StringBuilder();
        sb.append("[createMoreChip] recipients count: ");
        sb.append(sortedRecipients == null ? 0 : sortedRecipients.length);
        printDebugLog("RecipientEditTextView", sb.toString());
        if (sortedRecipients == null || ((!isPhoneQuery() && sortedRecipients.length <= this.mChipLimit) || (isPhoneQuery() && sortedRecipients.length <= 1))) {
            this.mMoreChip = null;
            printDebugLog("RecipientEditTextView", "[createMoreChip] no chip or all chips can be shown, return");
            return;
        }
        int length = sortedRecipients.length;
        if (isPhoneQuery()) {
            iCalculateNumChipsCanShow = length - calculateNumChipsCanShow();
            if (iCalculateNumChipsCanShow <= 0) {
                this.mMoreChip = null;
                printDebugLog("RecipientEditTextView", "[createMoreChip] overage <= 0, return");
                return;
            }
        } else {
            iCalculateNumChipsCanShow = length - this.mChipLimit;
        }
        SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(getText());
        MoreImageSpan moreImageSpanCreateMoreSpan = createMoreSpan(iCalculateNumChipsCanShow);
        this.mRemovedSpans = new ArrayList<>();
        int i = length - iCalculateNumChipsCanShow;
        int length2 = 0;
        int spanStart = 0;
        for (int i2 = i; i2 < sortedRecipients.length; i2++) {
            this.mRemovedSpans.add(sortedRecipients[i2]);
            if (i2 == i) {
                spanStart = spannableStringBuilder.getSpanStart(sortedRecipients[i2]);
            }
            if (i2 == sortedRecipients.length - 1) {
                length2 = spannableStringBuilder.getSpanEnd(sortedRecipients[i2]);
            }
            if (this.mTemporaryRecipients == null || !this.mTemporaryRecipients.contains(sortedRecipients[i2])) {
                sortedRecipients[i2].setOriginalText(spannableStringBuilder.toString().substring(spannableStringBuilder.getSpanStart(sortedRecipients[i2]), spannableStringBuilder.getSpanEnd(sortedRecipients[i2])));
            }
            spannableStringBuilder.removeSpan(sortedRecipients[i2]);
        }
        if (length2 < spannableStringBuilder.length()) {
            length2 = spannableStringBuilder.length();
        }
        int iMax = Math.max(spanStart, length2);
        int iMin = Math.min(spanStart, length2);
        SpannableString spannableString = new SpannableString(spannableStringBuilder.subSequence(iMin, iMax));
        spannableString.setSpan(moreImageSpanCreateMoreSpan, 0, spannableString.length(), 33);
        printDebugLog("RecipientEditTextView", "[createMoreChip] do replace, start: " + iMin + ", end: " + iMax);
        spannableStringBuilder.replace(iMin, iMax, (CharSequence) spannableString);
        setText(spannableStringBuilder);
        this.mMoreChip = moreImageSpanCreateMoreSpan;
        if (!isPhoneQuery() && getLineCount() > this.mMaxLines) {
            setMaxLines(getLineCount());
        }
    }

    void removeMoreChip() {
        StringBuilder sb = new StringBuilder();
        sb.append("[removeMoreChip], more chip span count: ");
        sb.append(this.mRemovedSpans == null ? null : Integer.valueOf(this.mRemovedSpans.size()));
        printDebugLog("RecipientEditTextView", sb.toString());
        if (this.mMoreChip != null) {
            SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(getText());
            int spanStart = spannableStringBuilder.getSpanStart(this.mMoreChip);
            spannableStringBuilder.removeSpan(this.mMoreChip);
            this.mMoreChip = null;
            if (this.mRemovedSpans != null && this.mRemovedSpans.size() > 0) {
                DrawableRecipientChip[] sortedRecipients = getSortedRecipients();
                if (sortedRecipients == null || sortedRecipients.length == 0) {
                    return;
                }
                printDebugLog("RecipientEditTextView", "[removeMoreChip], text = " + spannableStringBuilder.toString());
                for (DrawableRecipientChip drawableRecipientChip : this.mRemovedSpans) {
                    String str = (String) drawableRecipientChip.getOriginalText();
                    int iIndexOf = spannableStringBuilder.toString().indexOf(str, spanStart);
                    int iMin = Math.min(spannableStringBuilder.length(), str.length() + iIndexOf);
                    if (iIndexOf != -1) {
                        spannableStringBuilder.setSpan(drawableRecipientChip, iIndexOf, iMin, 33);
                    }
                    spanStart = iMin;
                }
                this.mRemovedSpans.clear();
            }
            setText(spannableStringBuilder);
        }
    }

    private DrawableRecipientChip selectChip(DrawableRecipientChip drawableRecipientChip) {
        StringBuilder sb = new StringBuilder();
        sb.append("[selectChip] ");
        sb.append((Object) (drawableRecipientChip == null ? null : drawableRecipientChip.getValue()));
        printDebugLog("RecipientEditTextView", sb.toString());
        if (drawableRecipientChip == null) {
            return null;
        }
        setDisableBringPointIntoView(true);
        if (this.mAlternatesPopup.isShowing()) {
            this.mAlternatesPopup.dismiss();
        }
        if (shouldShowEditableText(drawableRecipientChip)) {
            CharSequence value = drawableRecipientChip.getValue();
            Editable text = getText();
            Spannable spannable = getSpannable();
            int spanStart = spannable.getSpanStart(drawableRecipientChip);
            int spanEnd = spannable.getSpanEnd(drawableRecipientChip);
            while (spanEnd >= 0 && spanEnd < text.length() && text.charAt(spanEnd) == ' ') {
                spanEnd++;
            }
            spannable.removeSpan(drawableRecipientChip);
            text.delete(spanStart, spanEnd);
            setCursorVisible(true);
            setSelection(text.length());
            text.append(value);
            Log.d("RecipientEditTextView", " setDisableBringPointIntoView(false) in the selectChip() when shouldShowEditableText ");
            setDisableBringPointIntoView(false);
            return null;
        }
        if (drawableRecipientChip.getContactId() == -2 || drawableRecipientChip.getContactId() == 0) {
            int chipStart = getChipStart(drawableRecipientChip);
            int chipEnd = getChipEnd(drawableRecipientChip);
            getSpannable().removeSpan(drawableRecipientChip);
            try {
                if (this.mNoChips) {
                    return null;
                }
                DrawableRecipientChip drawableRecipientChipConstructChipSpan = constructChipSpan(drawableRecipientChip.getEntry(), true, false);
                Editable text2 = getText();
                QwertyKeyListener.markAsReplaced(text2, chipStart, chipEnd, "");
                if (chipStart == -1 || chipEnd == -1) {
                    Log.d("RecipientEditTextView", "The chip being selected no longer exists but should.");
                } else {
                    printThreadingDebugLog("MTKRecip", "[selectChip] setSpan");
                    text2.setSpan(drawableRecipientChipConstructChipSpan, chipStart, chipEnd, 33);
                }
                drawableRecipientChipConstructChipSpan.setSelected(true);
                if (shouldShowEditableText(drawableRecipientChipConstructChipSpan)) {
                    scrollLineIntoView(getLayout().getLineForOffset(getChipStart(drawableRecipientChipConstructChipSpan)));
                }
                showAddress(drawableRecipientChipConstructChipSpan, this.mAddressPopup, getWidth());
                setCursorVisible(false);
                return drawableRecipientChipConstructChipSpan;
            } catch (NullPointerException e) {
                Log.e("RecipientEditTextView", e.getMessage(), e);
                return null;
            }
        }
        int chipStart2 = getChipStart(drawableRecipientChip);
        int chipEnd2 = getChipEnd(drawableRecipientChip);
        getSpannable().removeSpan(drawableRecipientChip);
        try {
            DrawableRecipientChip drawableRecipientChipConstructChipSpan2 = constructChipSpan(drawableRecipientChip.getEntry(), true, false);
            Editable text3 = getText();
            QwertyKeyListener.markAsReplaced(text3, chipStart2, chipEnd2, "");
            if (chipStart2 == -1 || chipEnd2 == -1) {
                Log.d("RecipientEditTextView", "The chip being selected no longer exists but should.");
            } else {
                printThreadingDebugLog("MTKRecip", "[selectChip] setSpan");
                text3.setSpan(drawableRecipientChipConstructChipSpan2, chipStart2, chipEnd2, 33);
            }
            if (drawableRecipientChipConstructChipSpan2 != null) {
                drawableRecipientChipConstructChipSpan2.setSelected(true);
            }
            if (shouldShowEditableText(drawableRecipientChipConstructChipSpan2)) {
                scrollLineIntoView(getLayout().getLineForOffset(getChipStart(drawableRecipientChipConstructChipSpan2)));
            }
            showAlternates(drawableRecipientChipConstructChipSpan2, this.mAlternatesPopup, getWidth());
            setCursorVisible(false);
            return drawableRecipientChipConstructChipSpan2;
        } catch (NullPointerException e2) {
            Log.e("RecipientEditTextView", e2.getMessage(), e2);
            return null;
        }
    }

    private boolean shouldShowEditableText(DrawableRecipientChip drawableRecipientChip) {
        long contactId = drawableRecipientChip.getContactId();
        return contactId == -1 || (!isPhoneQuery() && contactId == -2);
    }

    private void showAddress(DrawableRecipientChip drawableRecipientChip, final ListPopupWindow listPopupWindow, int i) {
        StringBuilder sb = new StringBuilder();
        sb.append("[showAddress] ");
        sb.append((Object) (drawableRecipientChip == null ? null : drawableRecipientChip.getValue()));
        printDebugLog("RecipientEditTextView", sb.toString());
        if (!this.mAttachedToWindow || listPopupWindow == null) {
            return;
        }
        int offsetFromBottom = getOffsetFromBottom(getLayout().getLineForOffset(getChipStart(drawableRecipientChip)));
        listPopupWindow.setWidth(i);
        listPopupWindow.setAnchorView(this);
        listPopupWindow.setVerticalOffset(offsetFromBottom);
        listPopupWindow.setAdapter(createSingleAddressAdapter(drawableRecipientChip));
        listPopupWindow.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i2, long j) {
                MTKRecipientEditTextView.this.printDebugLog("RecipientEditTextView", "[showAddress] click ");
                MTKRecipientEditTextView.this.unselectChip(MTKRecipientEditTextView.this.mSelectedChip);
                listPopupWindow.dismiss();
            }
        });
        listPopupWindow.show();
        ListView listView = listPopupWindow.getListView();
        listView.setChoiceMode(1);
        listView.setItemChecked(0, true);
    }

    private void unselectChip(DrawableRecipientChip drawableRecipientChip) {
        StringBuilder sb = new StringBuilder();
        sb.append("[unselectChip] ");
        sb.append((Object) (drawableRecipientChip == null ? null : drawableRecipientChip.getValue()));
        printDebugLog("RecipientEditTextView", sb.toString());
        int chipStart = getChipStart(drawableRecipientChip);
        int chipEnd = getChipEnd(drawableRecipientChip);
        Editable text = getText();
        this.mSelectedChip = null;
        if (chipStart == -1 || chipEnd == -1) {
            Log.w("RecipientEditTextView", "The chip doesn't exist or may be a chip a user was editing");
            setSelection(text.length());
            commitDefault();
        } else {
            getSpannable().removeSpan(drawableRecipientChip);
            QwertyKeyListener.markAsReplaced(text, chipStart, chipEnd, "");
            text.removeSpan(drawableRecipientChip);
            try {
                if (!this.mNoChips) {
                    printThreadingDebugLog("MTKRecip", "[unSelectChip] setSpan");
                    text.setSpan(constructChipSpan(drawableRecipientChip.getEntry(), false, false), chipStart, chipEnd, 33);
                }
            } catch (NullPointerException e) {
                Log.e("RecipientEditTextView", e.getMessage(), e);
            }
        }
        setCursorVisible(true);
        setSelection(text.length());
        if (this.mAlternatesPopup != null && this.mAlternatesPopup.isShowing()) {
            this.mAlternatesPopup.dismiss();
        }
        if (this.mShowAlternatesTask != null) {
            this.mShowAlternatesTask.cancel(true);
        }
        if (this.mAddressPopup != null && this.mAddressPopup.isShowing()) {
            this.mAddressPopup.dismiss();
        }
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                MTKRecipientEditTextView.this.printDebugLog("RecipientEditTextView", "unselectChip setDisableBringPointIntoView(false)");
                MTKRecipientEditTextView.this.setDisableBringPointIntoView(false);
                MTKRecipientEditTextView.this.bringPointIntoView(MTKRecipientEditTextView.this.getSelectionStart());
            }
        });
    }

    private boolean isInDelete(DrawableRecipientChip drawableRecipientChip, int i, float f, float f2) {
        return this.mContext.getResources().getConfiguration().getLayoutDirection() == 1 ? drawableRecipientChip.isSelected() && i == getChipStart(drawableRecipientChip) : drawableRecipientChip.isSelected() && i == getChipEnd(drawableRecipientChip);
    }

    void removeChip(DrawableRecipientChip drawableRecipientChip) {
        StringBuilder sb = new StringBuilder();
        sb.append("[removeChip] ");
        sb.append((Object) (drawableRecipientChip == null ? null : drawableRecipientChip.getValue()));
        printDebugLog("RecipientEditTextView", sb.toString());
        SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(getSpannable());
        int spanStart = spannableStringBuilder.getSpanStart(drawableRecipientChip);
        int spanEnd = spannableStringBuilder.getSpanEnd(drawableRecipientChip);
        getScrollY();
        setChipProcessingMode(spanEnd == spannableStringBuilder.length() - 1 ? PROCESSING_MODE.REMOVE_LAST : PROCESSING_MODE.REMOVE);
        int lineCount = getLayout() != null ? getLayout().getLineCount() : 0;
        boolean z = drawableRecipientChip == this.mSelectedChip;
        if (z) {
            printDebugLog("RecipientEditTextView", "[removeChip] wasSelected = " + z);
            this.mSelectedChip = null;
        }
        while (spanEnd >= 0 && spanEnd < spannableStringBuilder.length() && spannableStringBuilder.charAt(spanEnd) == ' ') {
            spanEnd++;
        }
        spannableStringBuilder.removeSpan(drawableRecipientChip);
        if (spanStart >= 0 && spanEnd > 0) {
            spannableStringBuilder.delete(spanStart, spanEnd);
            printThreadingDebugLog("MTKRecip", "[removeChip] after delete text = " + ((Object) spannableStringBuilder));
        }
        if (z) {
            setCursorVisible(true);
            if (this.mLineOfSelectedChip >= lineCount - 2) {
                if (lineCount != (getLayout() != null ? getLayout().getLineCount() : 0)) {
                    Log.d("RecipientEditTextView", " setDisableBringPointIntoView(false) in the removeChip() when preLineCount != postLineCount ");
                    setDisableBringPointIntoView(false);
                    scrollBottomIntoView();
                }
            }
        }
        setText(spannableStringBuilder);
        getLayout().getLineTop(getLineCount() - 1);
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                MTKRecipientEditTextView.this.printDebugLog("RecipientEditTextView", "[removeChip][run]");
                MTKRecipientEditTextView.this.setDisableBringPointIntoView(false);
                MTKRecipientEditTextView.this.bringPointIntoView(MTKRecipientEditTextView.this.getSelectionStart());
            }
        });
        if (this.mShowAlternatesTask != null) {
            this.mShowAlternatesTask.cancel(true);
        }
        if (this.mAlternatesPopup != null && this.mAlternatesPopup.isShowing()) {
            this.mAlternatesPopup.dismiss();
        }
        setChipProcessingMode(PROCESSING_MODE.NONE);
        if ((mFeatureSet & 2) != 0) {
            this.changedChipAddresses.add(drawableRecipientChip.getEntry().getDestination());
        }
    }

    void replaceChip(DrawableRecipientChip drawableRecipientChip, RecipientEntry recipientEntry) {
        boolean z = drawableRecipientChip == this.mSelectedChip;
        if (z) {
            this.mSelectedChip = null;
        }
        int chipStart = getChipStart(drawableRecipientChip);
        int chipEnd = getChipEnd(drawableRecipientChip);
        DrawableRecipientChip[] sortedRecipients = getSortedRecipients();
        printDebugLog("RecipientEditTextView", "[replaceChip] start: " + chipStart + ", end: " + chipEnd + ", chip: " + drawableRecipientChip.getEntry());
        if (chipStart == -1 || chipEnd == -1) {
            RecipientEntry entry = drawableRecipientChip.getEntry();
            if (sortedRecipients != null && sortedRecipients.length > 0 && compareEntries(entry, sortedRecipients[0].getEntry())) {
                printDebugLog("RecipientEditTextView", "[replaceChip] The first chip is changed, update to the new one");
                drawableRecipientChip = sortedRecipients[0];
                chipStart = getChipStart(drawableRecipientChip);
                chipEnd = getChipEnd(drawableRecipientChip);
            }
            if ((chipStart == -1 || chipEnd == -1) && this.mRemovedSpans != null && this.mRemovedSpans.size() > 0) {
                int iMin = Math.min(5, this.mRemovedSpans.size());
                for (int i = 0; i < iMin; i++) {
                    if (compareEntries(entry, this.mRemovedSpans.get(i).getEntry())) {
                        printDebugLog("RecipientEditTextView", "[replaceChip] Found in mRemovedSpans, index:" + i + ", ignore this replacing action");
                        return;
                    }
                }
            }
        }
        getSpannable().removeSpan(drawableRecipientChip);
        Editable text = getText();
        if (!this.mDuringReplaceDupChips) {
            setChipProcessingMode(chipEnd == text.length() - 1 ? PROCESSING_MODE.REPLACE_LAST : PROCESSING_MODE.REPLACE);
        }
        CharSequence charSequenceCreateChip = createChip(recipientEntry, false);
        printDebugLog("RecipientEditTextView", "[replaceChip] start: " + chipStart + ", end: " + chipEnd + ", chip: " + drawableRecipientChip.getEntry() + ", chipText: " + ((Object) charSequenceCreateChip));
        if (charSequenceCreateChip != null) {
            if (chipStart == -1 || chipEnd == -1) {
                Log.w("RecipientEditTextView", "[WARNING] The chip to replace does not exist but should.");
            } else if (!TextUtils.isEmpty(charSequenceCreateChip)) {
                while (chipEnd >= 0 && chipEnd < text.length() && text.charAt(chipEnd) == ' ') {
                    chipEnd++;
                }
                printThreadingDebugLog("MTKRecip", "[replaceChip] replace, start=" + chipStart + "toReplace=" + chipEnd);
                text.replace(chipStart, chipEnd, charSequenceCreateChip);
            }
        }
        setCursorVisible(true);
        if (z) {
            clearSelectedChip();
        }
        setChipProcessingMode(PROCESSING_MODE.NONE);
        if ((mFeatureSet & 2) != 0) {
            this.changedChipAddresses.add(recipientEntry.getDestination());
        }
    }

    boolean compareEntries(RecipientEntry recipientEntry, RecipientEntry recipientEntry2) {
        return recipientEntry != null && recipientEntry2 != null && recipientEntry.getContactId() == recipientEntry2.getContactId() && recipientEntry.getDisplayName().equals(recipientEntry2.getDisplayName()) && recipientEntry.getDestination().equals(recipientEntry2.getDestination());
    }

    public void onClick(DrawableRecipientChip drawableRecipientChip, int i, float f, float f2) {
        StringBuilder sb = new StringBuilder();
        sb.append("[onClick] ");
        sb.append((Object) (drawableRecipientChip == null ? null : drawableRecipientChip.getValue()));
        printDebugLog("RecipientEditTextView", sb.toString());
        if (drawableRecipientChip != null && drawableRecipientChip.isSelected()) {
            if (isInDelete(drawableRecipientChip, i, f, f2)) {
                removeChip(drawableRecipientChip);
            } else {
                clearSelectedChip();
            }
        }
        if (this.mShowAlternatesTask != null) {
            this.mShowAlternatesTask.cancel(true);
        }
        if (this.mAlternatesPopup != null && this.mAlternatesPopup.isShowing()) {
            this.mAlternatesPopup.dismiss();
        }
    }

    private boolean chipsPending() {
        return this.mPendingChipsCount > 0 || (this.mRemovedSpans != null && this.mRemovedSpans.size() > 0);
    }

    @Override
    public void removeTextChangedListener(TextWatcher textWatcher) {
        if (textWatcher == this.mTextWatcher) {
            this.mTextWatcher = null;
        }
        super.removeTextChangedListener(textWatcher);
    }

    private class RecipientTextWatcher implements TextWatcher {
        private String mLastChangeText;

        private RecipientTextWatcher() {
            this.mLastChangeText = "";
        }

        @Override
        public void afterTextChanged(Editable editable) {
            char cCharAt;
            MTKRecipientEditTextView.this.printDebugLog("RecipientEditTextView", "[RecipientTextWatcher.afterTextChanged]");
            String string = editable.toString();
            if (string.equals(this.mLastChangeText)) {
                MTKRecipientEditTextView.this.printDebugLog("RecipientEditTextView", "[RecipientTextWatcher.afterTextChanged] equal");
                return;
            }
            this.mLastChangeText = string;
            MTKRecipientEditTextView.this.setCursorVisible(true);
            if (TextUtils.isEmpty(editable)) {
                MTKRecipientEditTextView.this.printDebugLog("RecipientEditTextView", "[RecipientTextWatcher.afterTextChanged] text is empty");
                Spannable spannable = MTKRecipientEditTextView.this.getSpannable();
                for (DrawableRecipientChip drawableRecipientChip : (DrawableRecipientChip[]) spannable.getSpans(0, MTKRecipientEditTextView.this.getText().length(), DrawableRecipientChip.class)) {
                    spannable.removeSpan(drawableRecipientChip);
                }
                if (MTKRecipientEditTextView.this.mMoreChip != null) {
                    spannable.removeSpan(MTKRecipientEditTextView.this.mMoreChip);
                }
                MTKRecipientEditTextView.this.mNoChips = false;
                if (MTKRecipientEditTextView.this.mRemovedSpans != null && MTKRecipientEditTextView.this.mRemovedSpans.size() > 0) {
                    MTKRecipientEditTextView.this.mRemovedSpans.clear();
                }
                if (MTKRecipientEditTextView.this.mTemporaryRecipients != null && MTKRecipientEditTextView.this.mTemporaryRecipients.size() > 0) {
                    MTKRecipientEditTextView.this.printDebugLog("RecipientEditTextView", "[RecipientTextWatcher.afterTextChanged] mTemporaryRecipients is not empty, count: " + MTKRecipientEditTextView.this.mTemporaryRecipients.size());
                    MTKRecipientEditTextView.this.clearTemporaryRecipients();
                }
                if (MTKRecipientEditTextView.this.mAlternatesPopup != null && MTKRecipientEditTextView.this.mAlternatesPopup.isShowing()) {
                    MTKRecipientEditTextView.this.mAlternatesPopup.dismiss();
                    MTKRecipientEditTextView.this.mSelectedChip = null;
                }
                if (MTKRecipientEditTextView.this.mAddressPopup != null && MTKRecipientEditTextView.this.mAddressPopup.isShowing()) {
                    MTKRecipientEditTextView.this.mAddressPopup.dismiss();
                    MTKRecipientEditTextView.this.mSelectedChip = null;
                    return;
                }
                return;
            }
            if (!MTKRecipientEditTextView.this.chipsPending()) {
                if (MTKRecipientEditTextView.this.mSelectedChip != null) {
                    if (!MTKRecipientEditTextView.this.shouldShowEditableText(MTKRecipientEditTextView.this.mSelectedChip)) {
                        MTKRecipientEditTextView.this.setCursorVisible(true);
                        MTKRecipientEditTextView.this.setSelection(MTKRecipientEditTextView.this.getText().length());
                        MTKRecipientEditTextView.this.clearSelectedChip();
                    } else {
                        return;
                    }
                }
                if (!MTKRecipientEditTextView.this.mDuringAccelerateRemoveChip && editable.length() > 1) {
                    if (MTKRecipientEditTextView.this.lastCharacterIsCommitCharacter(editable)) {
                        MTKRecipientEditTextView.this.commitByCharacter();
                        return;
                    }
                    int selectionEnd = MTKRecipientEditTextView.this.getSelectionEnd() != 0 ? MTKRecipientEditTextView.this.getSelectionEnd() - 1 : 0;
                    int length = MTKRecipientEditTextView.this.length() - 1;
                    if (selectionEnd != length) {
                        cCharAt = editable.charAt(selectionEnd);
                    } else {
                        cCharAt = editable.charAt(length);
                    }
                    if (cCharAt == 65292 || cCharAt == 65307) {
                        if (cCharAt == 65292) {
                            MTKRecipientEditTextView.this.getText().replace(selectionEnd, selectionEnd + 1, Character.toString(','));
                            return;
                        } else {
                            if (cCharAt == 65307) {
                                MTKRecipientEditTextView.this.getText().replace(selectionEnd, selectionEnd + 1, Character.toString(';'));
                                return;
                            }
                            return;
                        }
                    }
                    if (cCharAt == ' ' && !MTKRecipientEditTextView.this.isPhoneQuery()) {
                        String string2 = MTKRecipientEditTextView.this.getText().toString();
                        int iFindTokenStart = MTKRecipientEditTextView.this.mTokenizer.findTokenStart(string2, MTKRecipientEditTextView.this.getSelectionEnd());
                        String strSubstring = string2.substring(iFindTokenStart, MTKRecipientEditTextView.this.mTokenizer.findTokenEnd(string2, iFindTokenStart));
                        if (!TextUtils.isEmpty(strSubstring) && MTKRecipientEditTextView.this.mValidator != null && MTKRecipientEditTextView.this.mValidator.isValid(strSubstring)) {
                            MTKRecipientEditTextView.this.commitByCharacter();
                        }
                    }
                }
            }
        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            MTKRecipientEditTextView.printSensitiveDebugLog("RecipientEditTextView", "[RecipientTextWatcher.onTextChanged] start: " + i + ", before: " + i2 + ", count: " + i3 + ", processing mode: " + MTKRecipientEditTextView.this.mChipProcessingMode + ", text: " + ((Object) charSequence));
            MTKRecipientEditTextView.this.setDisableBringPointIntoView((MTKRecipientEditTextView.this.mChipProcessingMode == PROCESSING_MODE.NONE || MTKRecipientEditTextView.this.mChipProcessingMode == PROCESSING_MODE.COMMIT || MTKRecipientEditTextView.this.mChipProcessingMode == PROCESSING_MODE.REMOVE_LAST || MTKRecipientEditTextView.this.mChipProcessingMode == PROCESSING_MODE.REPLACE_LAST) ? false : true);
            if (i2 - i3 < 1) {
                if (i3 > i2 && !MTKRecipientEditTextView.this.mDisableBringPointIntoView) {
                    if (!MTKRecipientEditTextView.this.mLastStringChanged && (MTKRecipientEditTextView.mFeatureSet & 2) != 0) {
                        MTKRecipientEditTextView.this.mLastStringChanged = true;
                        MTKRecipientEditTextView.this.registerVSync();
                    }
                    if (MTKRecipientEditTextView.this.mSelectedChip != null && MTKRecipientEditTextView.this.shouldShowEditableText(MTKRecipientEditTextView.this.mSelectedChip) && MTKRecipientEditTextView.this.lastCharacterIsCommitCharacter(charSequence)) {
                        MTKRecipientEditTextView.this.commitByCharacter();
                        return;
                    }
                    return;
                }
                return;
            }
            int selectionStart = MTKRecipientEditTextView.this.getSelectionStart();
            DrawableRecipientChip[] drawableRecipientChipArr = (DrawableRecipientChip[]) MTKRecipientEditTextView.this.getSpannable().getSpans(selectionStart, selectionStart, DrawableRecipientChip.class);
            MTKRecipientEditTextView.this.printDebugLog("RecipientEditTextView", "[RecipientTextWatcher.onTextChanged] selStart: " + selectionStart + ", repl.length: " + drawableRecipientChipArr.length);
            if (drawableRecipientChipArr.length <= 0) {
                if (!MTKRecipientEditTextView.this.mLastStringChanged && (MTKRecipientEditTextView.mFeatureSet & 2) != 0) {
                    MTKRecipientEditTextView.this.mLastStringChanged = true;
                    MTKRecipientEditTextView.this.registerVSync();
                    return;
                }
                return;
            }
            Editable text = MTKRecipientEditTextView.this.getText();
            DrawableRecipientChip drawableRecipientChip = drawableRecipientChipArr[0];
            int chipStart = MTKRecipientEditTextView.this.getChipStart(drawableRecipientChip);
            int chipEnd = MTKRecipientEditTextView.this.getChipEnd(drawableRecipientChip);
            MTKRecipientEditTextView.this.printDebugLog("RecipientEditTextView", "[RecipientTextWatcher.onTextChanged] tokenStart:" + chipStart + ", tokenEnd:" + chipEnd);
            if (MTKRecipientEditTextView.this.mSelectedChip != null) {
                MTKRecipientEditTextView.this.printDebugLog("RecipientEditTextView", "[RecipientTextWatcher.onTextChanged] mSelectedChip != null");
                if (chipStart == MTKRecipientEditTextView.this.getChipStart(MTKRecipientEditTextView.this.mSelectedChip) && chipEnd == MTKRecipientEditTextView.this.getChipEnd(MTKRecipientEditTextView.this.mSelectedChip)) {
                    if (MTKRecipientEditTextView.this.mAlternatesPopup != null && MTKRecipientEditTextView.this.mAlternatesPopup.isShowing()) {
                        MTKRecipientEditTextView.this.mAlternatesPopup.dismiss();
                    }
                    if (MTKRecipientEditTextView.this.mAddressPopup != null && MTKRecipientEditTextView.this.mAddressPopup.isShowing()) {
                        MTKRecipientEditTextView.this.mAddressPopup.dismiss();
                    }
                    MTKRecipientEditTextView.this.mSelectedChip = null;
                }
            }
            MTKRecipientEditTextView.this.printDebugLog("RecipientEditTextView", "[RecipientTextWatcher.onTextChanged] delete a chip, tokenStart:" + chipStart + ", tokenEnd:" + chipEnd);
            text.delete(chipStart, chipEnd);
            MTKRecipientEditTextView.this.getSpannable().removeSpan(drawableRecipientChipArr[0]);
            if ((MTKRecipientEditTextView.mFeatureSet & 2) != 0) {
                MTKRecipientEditTextView.this.changedChipAddresses.add(drawableRecipientChipArr[0].getEntry().getDestination());
            }
        }

        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
        }
    }

    public boolean lastCharacterIsCommitCharacter(CharSequence charSequence) {
        int selectionEnd;
        char cCharAt;
        if (getSelectionEnd() != 0) {
            selectionEnd = getSelectionEnd() - 1;
        } else {
            selectionEnd = 0;
        }
        int length = length() - 1;
        if (selectionEnd != length) {
            cCharAt = charSequence.charAt(selectionEnd);
        } else {
            cCharAt = charSequence.charAt(length);
        }
        return cCharAt == ',' || cCharAt == ';';
    }

    private int getLineOfChip(DrawableRecipientChip drawableRecipientChip) {
        if (getLayout() == null) {
            return 0;
        }
        return getLayout().getLineForOffset(getChipStart(drawableRecipientChip));
    }

    private void handlePasteClip(ClipData clipData) {
        int chipEnd;
        if (!hasFocus() || this.mMoreChip != null) {
            printDebugLog("RecipientEditTextView", "[handlePasteClip] in shrink state, return");
            return;
        }
        printDebugLog("RecipientEditTextView", "[handlePasteClip] start");
        removeTextChangedListener(this.mTextWatcher);
        DrawableRecipientChip lastChip = getLastChip();
        if (lastChip != null) {
            chipEnd = getChipEnd(lastChip) + 1;
        } else {
            chipEnd = 0;
        }
        int selectionEnd = getSelectionEnd();
        if (selectionEnd > chipEnd && selectionEnd != 0) {
            String string = getText().toString();
            int i = selectionEnd;
            while (i > chipEnd && string.charAt(i - 1) == ' ') {
                i--;
            }
            if (i - 1 > chipEnd && i < string.length() - 1 && i <= selectionEnd - 1 && string.charAt(i) == ' ') {
                i++;
            }
            printThreadingDebugLog("MTKRecip", "[handlePasteClip] delete");
            getText().delete(i, selectionEnd);
        }
        if (clipData != null && (clipData.getDescription().hasMimeType("text/plain") || clipData.getDescription().hasMimeType("text/html"))) {
            for (int i2 = 0; i2 < clipData.getItemCount(); i2++) {
                CharSequence text = clipData.getItemAt(i2).getText();
                if (text != null && !TextUtils.isEmpty(text)) {
                    CharSequence charSequenceFilterInvalidCharacter = filterInvalidCharacter(text);
                    int selectionStart = getSelectionStart();
                    int selectionEnd2 = getSelectionEnd();
                    printDebugLog("RecipientEditTextView", "[handlePasteClip] filtered text: " + ((Object) charSequenceFilterInvalidCharacter) + ", start: " + selectionStart + ", end: " + selectionEnd2);
                    Editable text2 = getText();
                    if (selectionStart == selectionEnd2 && selectionEnd2 < text2.length() && text2.charAt(selectionStart) == ' ') {
                        selectionStart++;
                        selectionEnd2++;
                    }
                    setDisableBringPointIntoView(false);
                    if (selectionStart >= 0 && selectionEnd2 >= 0 && selectionStart != selectionEnd2) {
                        printThreadingDebugLog("MTKRecip", "[handlePasteClip] replace");
                        text2.replace(selectionStart, selectionEnd2, charSequenceFilterInvalidCharacter);
                        setSelection(text2.length());
                    } else {
                        text2.insert(selectionEnd2, charSequenceFilterInvalidCharacter);
                    }
                    handlePasteAndReplace();
                } else {
                    printDebugLog("RecipientEditTextView", "[handlePasteClip] pasted text is empty, ignore");
                }
            }
        }
        this.mHandler.post(this.mAddTextWatcher);
    }

    private CharSequence filterInvalidCharacter(CharSequence charSequence) {
        return charSequence.toString().replaceAll("\n", " ").replace((char) 65292, ',').replace((char) 65307, ';').replaceAll("^( *,)+", "").replaceAll("( *,)+", ",").replaceAll("(, *)+", ", ").replaceAll("^( *;)+", "").replaceAll("( *;)+", ";").replaceAll("(; *)+", "; ").replaceAll("^\\s+", "");
    }

    @Override
    public boolean onTextContextMenuItem(int i) {
        if (i == 16908322) {
            this.bPasted = true;
            handlePasteClip(((ClipboardManager) getContext().getSystemService("clipboard")).getPrimaryClip());
            return true;
        }
        return super.onTextContextMenuItem(i);
    }

    private void handlePasteAndReplace() {
        printDebugLog("RecipientEditTextView", "[handlePasteAndReplace]");
        ArrayList<DrawableRecipientChip> arrayListHandlePaste = handlePaste();
        if (arrayListHandlePaste != null && arrayListHandlePaste.size() > 0) {
            this.mIndividualReplacements = new IndividualReplacementTask();
            this.mIndividualReplacements.execute(arrayListHandlePaste);
        }
    }

    ArrayList<DrawableRecipientChip> handlePaste() {
        printDebugLog("RecipientEditTextView", "[handlePaste]");
        String string = getText().toString();
        int iFindTokenStart = this.mTokenizer.findTokenStart(string, getSelectionEnd());
        String strSubstring = string.substring(iFindTokenStart);
        ArrayList<DrawableRecipientChip> arrayList = new ArrayList<>();
        if (iFindTokenStart != 0) {
            DrawableRecipientChip drawableRecipientChip = null;
            int i = 0;
            int i2 = iFindTokenStart;
            while (i2 != 0 && drawableRecipientChip == null && i2 != i) {
                int iFindTokenStart2 = this.mTokenizer.findTokenStart(string, i2);
                DrawableRecipientChip drawableRecipientChipFindChip = findChip(iFindTokenStart2);
                if ((iFindTokenStart2 != iFindTokenStart || drawableRecipientChipFindChip != null) && i2 != iFindTokenStart2) {
                    i = i2;
                    i2 = iFindTokenStart2;
                    drawableRecipientChip = drawableRecipientChipFindChip;
                } else {
                    i = i2;
                    i2 = iFindTokenStart2;
                    drawableRecipientChip = drawableRecipientChipFindChip;
                    break;
                }
            }
            if (i2 != iFindTokenStart) {
                if (drawableRecipientChip != null) {
                    i2 = i;
                }
                int length = string.length() - iFindTokenStart;
                int i3 = i2;
                int length2 = iFindTokenStart;
                iFindTokenStart = i3;
                while (iFindTokenStart < length2) {
                    commitChip(iFindTokenStart, movePastTerminators(this.mTokenizer.findTokenEnd(getText().toString(), iFindTokenStart)), getText());
                    DrawableRecipientChip drawableRecipientChipFindChip2 = findChip(iFindTokenStart);
                    if (drawableRecipientChipFindChip2 == null) {
                        break;
                    }
                    iFindTokenStart = getSpannable().getSpanEnd(drawableRecipientChipFindChip2) + 1;
                    arrayList.add(drawableRecipientChipFindChip2);
                    length2 = getText().length() - length;
                }
            } else {
                iFindTokenStart = i2;
            }
        }
        if (isCompletedToken(strSubstring)) {
            Editable text = getText();
            int iIndexOf = text.toString().indexOf(strSubstring, iFindTokenStart);
            commitChip(iIndexOf, text.length(), text);
            arrayList.add(findChip(iIndexOf));
        } else if (!this.mLastStringChanged && (mFeatureSet & 2) != 0) {
            this.mLastStringChanged = true;
            registerVSync();
        }
        if ((mFeatureSet & 2) != 0) {
            Iterator<DrawableRecipientChip> it = arrayList.iterator();
            while (it.hasNext()) {
                this.changedChipAddresses.add(it.next().getEntry().getDestination());
            }
        }
        return arrayList;
    }

    int movePastTerminators(int i) {
        if (i >= length()) {
            return i;
        }
        char cCharAt = getText().toString().charAt(i);
        if (cCharAt == ',' || cCharAt == ';') {
            i++;
        }
        if (i < length() && getText().toString().charAt(i) == ' ') {
            return i + 1;
        }
        return i;
    }

    private class RecipientReplacementTask extends AsyncTask<Void, Void, Void> {
        private float mDefaultHeapUtilization;

        private RecipientReplacementTask() {
            this.mDefaultHeapUtilization = 0.0f;
        }

        private void adjustHeapUtilization() {
            VMRuntime runtime = VMRuntime.getRuntime();
            this.mDefaultHeapUtilization = runtime.getTargetHeapUtilization();
            MTKRecipientEditTextView.this.printDebugLog("RecipientEditTextView", "defaultUtilization = " + this.mDefaultHeapUtilization);
            String str = "";
            if ("".length() == 0) {
                str = "0.4";
            }
            float f = this.mDefaultHeapUtilization;
            try {
                f = Float.parseFloat(str);
            } catch (NumberFormatException e) {
                Log.d("RecipientEditTextView", "Invalid format of propery string: " + str);
            }
            runtime.setTargetHeapUtilization(f);
            float targetHeapUtilization = runtime.getTargetHeapUtilization();
            MTKRecipientEditTextView.this.printDebugLog("RecipientEditTextView", "newUtilization = " + targetHeapUtilization);
        }

        private void restoreHeapUtilization() {
            VMRuntime.getRuntime().setTargetHeapUtilization(this.mDefaultHeapUtilization);
        }

        private DrawableRecipientChip createFreeChip(RecipientEntry recipientEntry) {
            MTKRecipientEditTextView.this.printDebugLog("RecipientEditTextView", "[RecipientReplacementTask.createFreeChip]");
            try {
                if (!MTKRecipientEditTextView.this.mNoChips) {
                    return MTKRecipientEditTextView.this.constructChipSpan(recipientEntry, false, false);
                }
                return null;
            } catch (NullPointerException e) {
                Log.e("RecipientEditTextView", e.getMessage(), e);
                return null;
            }
        }

        @Override
        protected void onPreExecute() {
            ArrayList arrayList = new ArrayList();
            DrawableRecipientChip[] sortedRecipients = MTKRecipientEditTextView.this.getSortedRecipients();
            Trace.traceBegin(8L, "onPreExceute");
            adjustHeapUtilization();
            MTKRecipientEditTextView.this.printDebugLog("RecipientEditTextView", "[RecipientReplacementTask.onPreExecute] start, recipient count: " + sortedRecipients.length);
            for (DrawableRecipientChip drawableRecipientChip : sortedRecipients) {
                arrayList.add(drawableRecipientChip);
            }
            if (MTKRecipientEditTextView.this.mRemovedSpans != null) {
                arrayList.addAll(MTKRecipientEditTextView.this.mRemovedSpans);
            }
            ArrayList arrayList2 = new ArrayList(arrayList.size());
            for (DrawableRecipientChip drawableRecipientChip2 : arrayList) {
                if (RecipientEntry.isCreatedRecipient(drawableRecipientChip2.getEntry().getContactId()) && MTKRecipientEditTextView.this.getSpannable().getSpanStart(drawableRecipientChip2) != -1) {
                    arrayList2.add(createFreeChip(drawableRecipientChip2.getEntry()));
                } else {
                    arrayList2.add(null);
                }
            }
            processReplacements(arrayList, arrayList2);
            restoreHeapUtilization();
            Trace.traceEnd(8L);
        }

        @Override
        protected Void doInBackground(Void... voidArr) throws Throwable {
            Trace.traceBegin(8L, "doInBackground");
            if (MTKRecipientEditTextView.this.mIndividualReplacements != null) {
                MTKRecipientEditTextView.this.printDebugLog("RecipientEditTextView", "[RecipientReplacementTask.doInBackground] mIndividualReplacements.cancel()");
                MTKRecipientEditTextView.this.mIndividualReplacements.cancel(true);
                MTKRecipientEditTextView.this.mIndividualReplacements = null;
            }
            final ArrayList arrayList = new ArrayList();
            DrawableRecipientChip[] sortedRecipients = MTKRecipientEditTextView.this.getSortedRecipients();
            MTKRecipientEditTextView.this.printDebugLog("RecipientEditTextView", "[RecipientReplacementTask.doInBackground] start, recipient count: " + sortedRecipients.length);
            for (int i = 0; i < sortedRecipients.length; i++) {
                if (sortedRecipients[i] != null) {
                    arrayList.add(sortedRecipients[i]);
                }
            }
            if (MTKRecipientEditTextView.this.mRemovedSpans != null) {
                MTKRecipientEditTextView.this.printDebugLog("RecipientEditTextView", "[RecipientReplacementTask.doInBackground] removed recipient count: " + MTKRecipientEditTextView.this.mRemovedSpans.size());
                arrayList.addAll(MTKRecipientEditTextView.this.mRemovedSpans);
            }
            ArrayList arrayList2 = new ArrayList();
            HashSet hashSet = new HashSet();
            for (int i2 = 0; i2 < arrayList.size(); i2++) {
                DrawableRecipientChip drawableRecipientChip = (DrawableRecipientChip) arrayList.get(i2);
                if (drawableRecipientChip != null) {
                    arrayList2.add(drawableRecipientChip.getEntry().getDestination());
                    Log.d("RecipientEditTextView", "ADD existChipsNameSet " + drawableRecipientChip.getEntry().getDestination());
                    hashSet.add(drawableRecipientChip.getEntry().getDestination());
                }
            }
            BaseRecipientAdapter baseRecipientAdapter = (BaseRecipientAdapter) MTKRecipientEditTextView.this.getAdapter();
            RecipientAlternatesAdapter.getMatchingRecipients(hashSet, MTKRecipientEditTextView.this.getContext(), baseRecipientAdapter, arrayList2, baseRecipientAdapter.getAccount(), new RecipientAlternatesAdapter.RecipientMatchCallback() {
                @Override
                public void matchesFound(Map<String, RecipientEntry> map) {
                    RecipientEntry recipientEntryCreateValidatedEntry;
                    MTKRecipientEditTextView.this.printDebugLog("RecipientEditTextView", "[RecipientReplacementTask.doInBackground][matchesFound] start, recipients count: " + arrayList.size() + ", entries size: " + map.size());
                    ArrayList arrayList3 = new ArrayList();
                    new PreloadPhotoTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, map.values());
                    for (DrawableRecipientChip drawableRecipientChip2 : arrayList) {
                        if (drawableRecipientChip2 != null) {
                            long contactId = drawableRecipientChip2.getEntry().getContactId();
                            int spanStart = MTKRecipientEditTextView.this.getSpannable().getSpanStart(drawableRecipientChip2);
                            MTKRecipientEditTextView.this.printDebugLog("RecipientEditTextView", "[matchesFound] chip: " + ((Object) drawableRecipientChip2.getValue()) + ", contactID: " + contactId + ", spanStart: " + spanStart);
                            if (RecipientEntry.isCreatedRecipient(contactId)) {
                                if (!MTKRecipientEditTextView.this.isPhoneQuery()) {
                                    recipientEntryCreateValidatedEntry = MTKRecipientEditTextView.this.createValidatedEntry(map.get(MTKRecipientEditTextView.tokenizeAddress(drawableRecipientChip2.getEntry().getDestination())));
                                    MTKRecipientEditTextView.this.printDebugLog("RecipientEditTextView", "[matchesFound] email case, entry: " + recipientEntryCreateValidatedEntry);
                                } else {
                                    RecipientEntry recipientEntry = map.get(MTKRecipientEditTextView.tokenizeAddress(drawableRecipientChip2.getEntry().getDestination()));
                                    MTKRecipientEditTextView.this.printDebugLog("RecipientEditTextView", "[matchesFound] phone case, entry: " + recipientEntry);
                                    if (recipientEntry == null) {
                                        recipientEntry = map.get(drawableRecipientChip2.getEntry().getDestination());
                                    }
                                    recipientEntryCreateValidatedEntry = recipientEntry;
                                }
                                if (spanStart == -1 && recipientEntryCreateValidatedEntry != null) {
                                    MTKRecipientEditTextView.this.addTemporaryRecipients(RecipientReplacementTask.this.createFreeChip(recipientEntryCreateValidatedEntry));
                                    recipientEntryCreateValidatedEntry = null;
                                }
                                if (recipientEntryCreateValidatedEntry == null) {
                                }
                            } else {
                                recipientEntryCreateValidatedEntry = null;
                                if (recipientEntryCreateValidatedEntry == null) {
                                    arrayList3.add(RecipientReplacementTask.this.createFreeChip(recipientEntryCreateValidatedEntry));
                                } else {
                                    arrayList3.add(null);
                                }
                            }
                        }
                    }
                    RecipientReplacementTask.this.processReplacements(arrayList, arrayList3);
                }

                @Override
                public void matchesNotFound(Set<String> set) {
                    MTKRecipientEditTextView.this.printDebugLog("RecipientEditTextView", "[RecipientReplacementTask.doInBackground][matchesNotFound] start, unfound count: " + set.size());
                    if (set.size() > 0) {
                        ArrayList arrayList3 = new ArrayList(set.size());
                        for (DrawableRecipientChip drawableRecipientChip2 : arrayList) {
                            if (drawableRecipientChip2 != null && RecipientEntry.isCreatedRecipient(drawableRecipientChip2.getEntry().getContactId()) && MTKRecipientEditTextView.this.getSpannable().getSpanStart(drawableRecipientChip2) != -1) {
                                if (set.contains(drawableRecipientChip2.getEntry().getDestination())) {
                                    arrayList3.add(RecipientReplacementTask.this.createFreeChip(drawableRecipientChip2.getEntry()));
                                } else {
                                    arrayList3.add(null);
                                }
                            } else {
                                arrayList3.add(null);
                            }
                        }
                        RecipientReplacementTask.this.processReplacements(arrayList, arrayList3);
                    }
                }
            });
            MTKRecipientEditTextView.this.setForceEnableBringPointIntoView(false);
            MTKRecipientEditTextView.this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    MTKRecipientEditTextView.this.printDebugLog("RecipientEditTextView", "[RecipientReplacementTask IndividualTask][run] setDisableBringPointIntoView(false)");
                    MTKRecipientEditTextView.this.setDisableBringPointIntoView(false);
                    MTKRecipientEditTextView.this.bringPointIntoView(MTKRecipientEditTextView.this.getSelectionStart());
                }
            });
            Trace.traceEnd(8L);
            return null;
        }

        private void processReplacements(final List<DrawableRecipientChip> list, final List<DrawableRecipientChip> list2) {
            MTKRecipientEditTextView.this.printDebugLog("RecipientEditTextView", "[RecipientReplacementTask.processReplacements] start");
            if (list2 != null && list2.size() > 0) {
                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        DrawableRecipientChip drawableRecipientChip;
                        int iMin;
                        Trace.traceBegin(8L, "processRep");
                        RecipientReplacementTask.this.adjustHeapUtilization();
                        SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(MTKRecipientEditTextView.this.getText());
                        int size = list2.size();
                        MTKRecipientEditTextView.this.printDebugLog("RecipientEditTextView", "[RecipientReplacementTask.processReplacements][run] start, recipients count: " + list.size() + ", replacements count: " + list2.size());
                        int i = 0;
                        for (DrawableRecipientChip drawableRecipientChip2 : list) {
                            DrawableRecipientChip drawableRecipientChip3 = (DrawableRecipientChip) list2.get(i);
                            if (drawableRecipientChip3 != null) {
                                RecipientEntry entry = drawableRecipientChip2.getEntry();
                                RecipientEntry entry2 = drawableRecipientChip3.getEntry();
                                boolean z = RecipientAlternatesAdapter.getBetterRecipient(entry, entry2) == entry2;
                                MTKRecipientEditTextView.this.printDebugLog("RecipientEditTextView", "[run] index: " + i + ", old: " + entry + ", new: " + entry2 + ", isBetter: " + z + ", spanStart: " + spannableStringBuilder.getSpanStart(drawableRecipientChip2));
                                if (z) {
                                    int spanStart = spannableStringBuilder.getSpanStart(drawableRecipientChip2);
                                    if (spanStart == -1) {
                                        MTKRecipientEditTextView.this.printDebugLog("RecipientEditTextView", "[run] Can't find the chip to be replaced!!");
                                        DrawableRecipientChip[] sortedRecipients = MTKRecipientEditTextView.this.getSortedRecipients();
                                        int length = sortedRecipients.length;
                                        for (int i2 = 0; i2 < length; i2++) {
                                            drawableRecipientChip = sortedRecipients[i2];
                                            if (MTKRecipientEditTextView.this.compareEntries(drawableRecipientChip2.getEntry(), drawableRecipientChip.getEntry())) {
                                                MTKRecipientEditTextView.this.printDebugLog("RecipientEditTextView", "[run] Found the missing chip");
                                                spanStart = spannableStringBuilder.getSpanStart(drawableRecipientChip);
                                                break;
                                            }
                                        }
                                        drawableRecipientChip = drawableRecipientChip2;
                                        if (spanStart != -1) {
                                            if (!MTKRecipientEditTextView.this.hasFocus()) {
                                                MTKRecipientEditTextView.this.printDebugLog("RecipientEditTextView", "[run] !hasFocus, add chip back to mTemporaryRecipients");
                                                MTKRecipientEditTextView.this.addTemporaryRecipients(drawableRecipientChip3);
                                            }
                                        } else {
                                            int iMin2 = Math.min(spannableStringBuilder.getSpanEnd(drawableRecipientChip) + 1, spannableStringBuilder.length());
                                            spannableStringBuilder.removeSpan(drawableRecipientChip);
                                            String strCreateAddressText = MTKRecipientEditTextView.this.createAddressText(drawableRecipientChip3.getEntry());
                                            if (strCreateAddressText != null) {
                                                SpannableString spannableString = new SpannableString(strCreateAddressText.trim() + " ");
                                                spannableString.setSpan(drawableRecipientChip3, 0, spannableString.length() - 1, 33);
                                                if (spannableStringBuilder.charAt(Math.min(iMin2, spannableStringBuilder.length() - 1)) == ' ') {
                                                    iMin = Math.min(iMin2 + 1, spannableStringBuilder.length());
                                                } else {
                                                    iMin = iMin2;
                                                }
                                                MTKRecipientEditTextView.this.printDebugLog("RecipientEditTextView", "[run] replace text, start: " + spanStart + ", end: " + iMin2 + ", text: " + ((Object) spannableString));
                                                spannableStringBuilder.replace(spanStart, iMin, (CharSequence) spannableString);
                                                drawableRecipientChip3.setOriginalText(spannableString.toString());
                                                list2.set(i, null);
                                                list.set(i, drawableRecipientChip3);
                                                if ((MTKRecipientEditTextView.mFeatureSet & 2) != 0) {
                                                    MTKRecipientEditTextView.this.changedChipAddresses.add(drawableRecipientChip3.getEntry().getDestination());
                                                }
                                            }
                                        }
                                    } else {
                                        drawableRecipientChip = drawableRecipientChip2;
                                        if (spanStart != -1) {
                                        }
                                    }
                                }
                            }
                            i++;
                        }
                        MTKRecipientEditTextView.this.setText(spannableStringBuilder);
                        if (size > 0) {
                            MTKRecipientEditTextView.this.recoverLayout();
                        }
                        RecipientReplacementTask.this.restoreHeapUtilization();
                        Trace.traceEnd(8L);
                    }
                };
                if (Looper.myLooper() == Looper.getMainLooper()) {
                    MTKRecipientEditTextView.this.printDebugLog("RecipientEditTextView", "[processReplacements] call runnable.run()");
                    runnable.run();
                } else {
                    MTKRecipientEditTextView.this.printDebugLog("RecipientEditTextView", "[processReplacements] post runnable");
                    MTKRecipientEditTextView.this.mHandler.post(runnable);
                }
            }
        }

        @Override
        protected void onPostExecute(Void r1) {
            if (MTKRecipientEditTextView.this.mChipProcessListener != null) {
                MTKRecipientEditTextView.this.mChipProcessListener.onChipProcessDone();
            }
        }
    }

    private class PhoneNumberQueryAndReplacementTask extends AsyncTask<RecipientEntry, Void, Void> {
        private int offset;
        private String text;

        private PhoneNumberQueryAndReplacementTask() {
        }

        @Override
        protected Void doInBackground(RecipientEntry... recipientEntryArr) {
            final RecipientEntry recipientEntry = recipientEntryArr[0];
            MTKRecipientEditTextView.printSensitiveDebugLog("RecipientEditTextView", "[PhoneNumberQueryAndReplacementTask.doInBackground] start " + recipientEntry.getDestination());
            final RecipientEntry recipientEntryByPhoneNumber = RecipientAlternatesAdapter.getRecipientEntryByPhoneNumber(MTKRecipientEditTextView.this.getContext(), recipientEntry.getDestination());
            if (recipientEntryByPhoneNumber != null) {
                MTKRecipientEditTextView.this.printDebugLog("RecipientEditTextView", "[PhoneNumberQueryAndReplacementTask.doInBackground] query result != null ");
                MTKRecipientEditTextView.this.mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        PhoneNumberQueryAndReplacementTask.this.text = MTKRecipientEditTextView.this.getText().toString();
                        PhoneNumberQueryAndReplacementTask.this.offset = 1;
                        if (PhoneNumberQueryAndReplacementTask.this.text != null) {
                            PhoneNumberQueryAndReplacementTask.this.offset = PhoneNumberQueryAndReplacementTask.this.text.indexOf(recipientEntry.getDestination());
                        }
                        MTKRecipientEditTextView.this.printDebugLog("RecipientEditTextView", "[PhoneNumberQueryAndReplacementTask][run] replaceChip");
                        if (MTKRecipientEditTextView.this.findChip(PhoneNumberQueryAndReplacementTask.this.offset) != null) {
                            MTKRecipientEditTextView.this.replaceChip(MTKRecipientEditTextView.this.findChip(PhoneNumberQueryAndReplacementTask.this.offset), recipientEntryByPhoneNumber);
                            return;
                        }
                        MTKRecipientEditTextView.this.expand();
                        if (MTKRecipientEditTextView.this.findChip(PhoneNumberQueryAndReplacementTask.this.offset) != null) {
                            MTKRecipientEditTextView.this.replaceChip(MTKRecipientEditTextView.this.findChip(PhoneNumberQueryAndReplacementTask.this.offset), recipientEntryByPhoneNumber);
                        }
                        MTKRecipientEditTextView.this.shrink();
                    }
                });
                return null;
            }
            return null;
        }
    }

    private class IndividualReplacementTask extends AsyncTask<ArrayList<DrawableRecipientChip>, Void, Void> {
        private IndividualReplacementTask() {
        }

        @Override
        protected Void doInBackground(ArrayList<DrawableRecipientChip>... arrayListArr) throws Throwable {
            final ArrayList<DrawableRecipientChip> arrayList = arrayListArr[0];
            MTKRecipientEditTextView.this.printDebugLog("RecipientEditTextView", "[IndividualReplacementTask.doInBackground] start, recipient count: " + arrayList.size());
            ArrayList arrayList2 = new ArrayList();
            HashSet hashSet = new HashSet();
            for (int i = 0; i < arrayList.size(); i++) {
                DrawableRecipientChip drawableRecipientChip = arrayList.get(i);
                if (drawableRecipientChip != null) {
                    arrayList2.add(MTKRecipientEditTextView.this.createAddressText(drawableRecipientChip.getEntry()));
                    hashSet.add(drawableRecipientChip.getEntry().getDestination());
                }
            }
            BaseRecipientAdapter baseRecipientAdapter = (BaseRecipientAdapter) MTKRecipientEditTextView.this.getAdapter();
            if (baseRecipientAdapter == null) {
                return null;
            }
            RecipientAlternatesAdapter.getMatchingRecipients(hashSet, MTKRecipientEditTextView.this.getContext(), baseRecipientAdapter, arrayList2, baseRecipientAdapter.getAccount(), new RecipientAlternatesAdapter.RecipientMatchCallback() {
                @Override
                public void matchesFound(final Map<String, RecipientEntry> map) {
                    MTKRecipientEditTextView.this.printDebugLog("RecipientEditTextView", "[IndividualReplacementTask.doInBackground][matchesFound] entries size: " + map.size());
                    for (String str : map.keySet()) {
                        MTKRecipientEditTextView.this.printDebugLog("RecipientEditTextView", "entry: " + map.get(str));
                    }
                    MTKRecipientEditTextView.this.mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            DrawableRecipientChip drawableRecipientChip2;
                            boolean z;
                            int i2 = 0;
                            for (DrawableRecipientChip drawableRecipientChip3 : arrayList) {
                                if (drawableRecipientChip3 != null && RecipientEntry.isCreatedRecipient(drawableRecipientChip3.getEntry().getContactId())) {
                                    MTKRecipientEditTextView.this.printDebugLog("RecipientEditTextView", "the temp chip start at : " + MTKRecipientEditTextView.this.getSpannable().getSpanStart(drawableRecipientChip3));
                                    if (MTKRecipientEditTextView.this.getSpannable().getSpanStart(drawableRecipientChip3) == -1) {
                                        synchronized (MTKRecipientEditTextView.this.mEllipsizedChipLock) {
                                            DrawableRecipientChip[] sortedRecipients = MTKRecipientEditTextView.this.getSortedRecipients();
                                            if (sortedRecipients != null) {
                                                for (int i3 = 0; i3 < sortedRecipients.length && i3 < arrayList.size(); i3++) {
                                                    if (MTKRecipientEditTextView.this.compareEntries(drawableRecipientChip3.getEntry(), sortedRecipients[i3].getEntry())) {
                                                        DrawableRecipientChip drawableRecipientChip4 = sortedRecipients[i3];
                                                        MTKRecipientEditTextView.this.printDebugLog("RecipientEditTextView", "[matchesFound] Chip may be replaced due to replaceChipOnSameTextRange()");
                                                        drawableRecipientChip2 = drawableRecipientChip4;
                                                        z = true;
                                                        break;
                                                    }
                                                }
                                                drawableRecipientChip2 = drawableRecipientChip3;
                                                z = false;
                                                if (!z) {
                                                    MTKRecipientEditTextView.this.printDebugLog("RecipientEditTextView", "[matchesFound] [WARNING] Can't find the chip to replace");
                                                } else {
                                                    drawableRecipientChip3 = drawableRecipientChip2;
                                                }
                                            }
                                        }
                                    }
                                    String destination = drawableRecipientChip3.getEntry().getDestination();
                                    if (!MTKRecipientEditTextView.isPhoneNumber(destination)) {
                                        destination = MTKRecipientEditTextView.tokenizeAddress(destination);
                                    }
                                    RecipientEntry recipientEntryCreateValidatedEntry = MTKRecipientEditTextView.this.createValidatedEntry((RecipientEntry) map.get(destination));
                                    MTKRecipientEditTextView.this.printDebugLog("RecipientEditTextView", "[matchesFound] destination: " + destination + ", entry created: " + recipientEntryCreateValidatedEntry);
                                    if (recipientEntryCreateValidatedEntry == null) {
                                        recipientEntryCreateValidatedEntry = drawableRecipientChip3.getEntry();
                                    }
                                    if (recipientEntryCreateValidatedEntry != null) {
                                        boolean z2 = i2 == 0;
                                        MTKRecipientEditTextView.this.printDebugLog("RecipientEditTextView", "[IndividualReplacementTask.doInBackground][matchesFound.run] old: " + drawableRecipientChip3.getEntry() + ", new: " + recipientEntryCreateValidatedEntry);
                                        if (MTKRecipientEditTextView.this.mIndividualReplacements != null) {
                                            MTKRecipientEditTextView.this.replaceChip(drawableRecipientChip3, recipientEntryCreateValidatedEntry);
                                            if (z2 && MTKRecipientEditTextView.this.mHasEllipsizedFirstChip && MTKRecipientEditTextView.this.mRemovedSpans != null) {
                                                int iCalculateAvailableWidth = ((int) MTKRecipientEditTextView.this.calculateAvailableWidth()) - MTKRecipientEditTextView.this.getMeasuredMoreSpanWidth(MTKRecipientEditTextView.this.mRemovedSpans.size());
                                                DrawableRecipientChip[] sortedRecipients2 = MTKRecipientEditTextView.this.getSortedRecipients();
                                                if (sortedRecipients2 != null && sortedRecipients2.length > 0 && MTKRecipientEditTextView.this.getChipWidth(sortedRecipients2[0]) > iCalculateAvailableWidth) {
                                                    MTKRecipientEditTextView.this.printDebugLog("RecipientEditTextView", "The first chip must be ellipsized again");
                                                    MTKRecipientEditTextView.this.replaceChipOnSameTextRange(sortedRecipients2[0], iCalculateAvailableWidth);
                                                }
                                            }
                                            if (MTKRecipientEditTextView.this.calculateUnselectedChipWidth(recipientEntryCreateValidatedEntry) > MTKRecipientEditTextView.this.getChipWidth(drawableRecipientChip3) && MTKRecipientEditTextView.this.mRemovedSpans != null && MTKRecipientEditTextView.this.mRemovedSpans.size() != 0) {
                                                MTKRecipientEditTextView.this.printDebugLog("RecipientEditTextView", "[IndividualReplacementTask.doInBackground][matchesFound.run] the new chip is more wide");
                                                MTKRecipientEditTextView.this.createMoreChip();
                                            }
                                        }
                                    }
                                }
                                i2++;
                            }
                        }
                    });
                }

                @Override
                public void matchesNotFound(Set<String> set) {
                    MTKRecipientEditTextView.this.printDebugLog("RecipientEditTextView", "[IndividualReplacementTask.doInBackground][matchesNotFound]");
                }
            });
            MTKRecipientEditTextView.this.setForceEnableBringPointIntoView(false);
            MTKRecipientEditTextView.this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    MTKRecipientEditTextView.this.printDebugLog("RecipientEditTextView", "[IndividualTask][run] setDisableBringPointIntoView(false)");
                    MTKRecipientEditTextView.this.setDisableBringPointIntoView(false);
                    MTKRecipientEditTextView.this.bringPointIntoView(MTKRecipientEditTextView.this.getSelectionStart());
                }
            });
            return null;
        }

        @Override
        protected void onPostExecute(Void r1) {
            if (MTKRecipientEditTextView.this.mChipProcessListener != null) {
                MTKRecipientEditTextView.this.mChipProcessListener.onChipProcessDone();
            }
        }
    }

    private class MoreImageSpan extends ReplacementDrawableSpan {
        public MoreImageSpan(Drawable drawable) {
            super(drawable);
        }
    }

    @Override
    public boolean onDown(MotionEvent motionEvent) {
        return false;
    }

    @Override
    public boolean onFling(MotionEvent motionEvent, MotionEvent motionEvent2, float f, float f2) {
        return false;
    }

    @Override
    public void onLongPress(MotionEvent motionEvent) {
        if (this.mSelectedChip != null) {
            return;
        }
        float x = motionEvent.getX();
        float y = motionEvent.getY();
        DrawableRecipientChip drawableRecipientChipFindChip = isTouchPointInChip(x, y) ? findChip(putOffsetInRange(x, y)) : null;
        if (drawableRecipientChipFindChip != null) {
            if (this.mDragEnabled) {
                startDrag(drawableRecipientChipFindChip);
            } else {
                showCopyDialog(drawableRecipientChipFindChip.getEntry().getDestination());
            }
        }
    }

    private int supportGetOffsetForPosition(float f, float f2) {
        if (getLayout() == null) {
            return -1;
        }
        return supportGetOffsetAtCoordinate(supportGetLineAtCoordinate(f2), f);
    }

    private float supportConvertToLocalHorizontalCoordinate(float f) {
        return Math.min((getWidth() - getTotalPaddingRight()) - 1, Math.max(0.0f, f - getTotalPaddingLeft())) + getScrollX();
    }

    private int supportGetLineAtCoordinate(float f) {
        return getLayout().getLineForVertical((int) (Math.min((getHeight() - getTotalPaddingBottom()) - 1, Math.max(0.0f, f - getTotalPaddingLeft())) + getScrollY()));
    }

    private int supportGetOffsetAtCoordinate(int i, float f) {
        return getLayout().getOffsetForHorizontal(i, supportConvertToLocalHorizontalCoordinate(f));
    }

    private void startDrag(DrawableRecipientChip drawableRecipientChip) {
        String destination = drawableRecipientChip.getEntry().getDestination();
        startDrag(ClipData.newPlainText(destination, destination + ','), new RecipientChipShadow(drawableRecipientChip), null, 0);
        removeChip(drawableRecipientChip);
    }

    @Override
    public boolean onDragEvent(DragEvent dragEvent) {
        int action = dragEvent.getAction();
        if (action == 1) {
            return dragEvent.getClipDescription().hasMimeType("text/plain");
        }
        if (action == 3) {
            handlePasteClip(dragEvent.getClipData());
            return true;
        }
        if (action == 5) {
            requestFocus();
            return true;
        }
        return false;
    }

    private final class RecipientChipShadow extends View.DragShadowBuilder {
        private final DrawableRecipientChip mChip;

        public RecipientChipShadow(DrawableRecipientChip drawableRecipientChip) {
            this.mChip = drawableRecipientChip;
        }

        @Override
        public void onProvideShadowMetrics(Point point, Point point2) {
            Rect bounds = this.mChip.getBounds();
            point.set(bounds.width(), bounds.height());
            point2.set(bounds.centerX(), bounds.centerY());
        }

        @Override
        public void onDrawShadow(Canvas canvas) {
            this.mChip.draw(canvas);
        }
    }

    private void showCopyDialog(String str) {
        int i;
        printDebugLog("RecipientEditTextView", "[showCopyDialog] address: " + str);
        if (!this.mAttachedToWindow) {
            return;
        }
        this.mCopyAddress = str;
        if (this.mCopyDialog != null) {
            this.mCopyDialog.dismiss();
        }
        if (isPhoneQuery()) {
            i = R.string.copy_number;
        } else {
            i = R.string.copy_email;
        }
        this.mCopyDialog = new AlertDialog.Builder(this.mContext).setTitle(str).setCancelable(true).setItems(new String[]{getContext().getResources().getString(i)}, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i2) {
                MTKRecipientEditTextView.this.printDebugLog("RecipientEditTextView", "[showCopyDialog] click!");
                ((ClipboardManager) MTKRecipientEditTextView.this.getContext().getSystemService("clipboard")).setPrimaryClip(ClipData.newPlainText("", MTKRecipientEditTextView.this.mCopyAddress));
                dialogInterface.dismiss();
            }
        }).create();
        this.mCopyDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                MTKRecipientEditTextView.this.printDebugLog("RecipientEditTextView", "[showCopyDialog] dimiss!");
                MTKRecipientEditTextView.this.mCopyAddress = null;
                MTKRecipientEditTextView.this.mCopyDialog = null;
            }
        });
        this.mCopyDialog.setCanceledOnTouchOutside(true);
        this.mCopyDialog.show();
    }

    @Override
    public boolean onScroll(MotionEvent motionEvent, MotionEvent motionEvent2, float f, float f2) {
        return false;
    }

    @Override
    public void onShowPress(MotionEvent motionEvent) {
    }

    @Override
    public boolean onSingleTapUp(MotionEvent motionEvent) {
        return false;
    }

    @Override
    public void onDismiss(DialogInterface dialogInterface) {
        this.mCopyAddress = null;
    }

    @Override
    public void onClick(View view) {
        ((ClipboardManager) getContext().getSystemService("clipboard")).setPrimaryClip(ClipData.newPlainText("", this.mCopyAddress));
        this.mCopyDialog.dismiss();
    }

    protected boolean isPhoneQuery() {
        return getAdapter() != null && ((BaseRecipientAdapter) getAdapter()).getQueryType() == 1;
    }

    protected void setEnableDiscardNextActionUp(boolean z) {
        this.mRETVDiscardNextActionUp = z;
    }

    protected boolean getEnableDiscardNextActionUp() {
        return this.mRETVDiscardNextActionUp;
    }

    private int getChipWidth(DrawableRecipientChip drawableRecipientChip) {
        int iWidth = drawableRecipientChip.getBounds().width();
        if (iWidth == 0) {
            return calculateUnselectedChipWidth(drawableRecipientChip.getEntry());
        }
        return iWidth;
    }

    private int getChipInterval() {
        return 5;
    }

    private int getMeasuredMoreSpanWidth(int i) {
        String str = String.format(this.mMoreItem.getText().toString(), Integer.valueOf(i));
        TextPaint textPaint = new TextPaint(this.mChipPaint);
        textPaint.setTextSize(this.mMoreItem.getTextSize());
        return ((int) textPaint.measureText(str)) + this.mMoreItem.getPaddingLeft() + this.mMoreItem.getPaddingRight();
    }

    private void replaceChipOnSameTextRange(DrawableRecipientChip drawableRecipientChip, int i) {
        boolean z;
        printDebugLog("RecipientEditTextView", "[replaceChipOnSameTextRange]");
        int chipStart = getChipStart(drawableRecipientChip);
        int chipEnd = getChipEnd(drawableRecipientChip);
        this.mLimitedWidthForSpan = i;
        RecipientEntry entry = drawableRecipientChip.getEntry();
        RecipientEntry entry2 = drawableRecipientChip.getEntry();
        if (!TextUtils.isEmpty(entry.getDisplayName()) && !TextUtils.equals(entry.getDisplayName(), entry.getDestination())) {
            z = false;
        } else {
            z = true;
        }
        DrawableRecipientChip drawableRecipientChipConstructChipSpan = constructChipSpan(entry2, false, z);
        this.mLimitedWidthForSpan = -1;
        synchronized (this.mEllipsizedChipLock) {
            getSpannable().removeSpan(drawableRecipientChip);
            Editable text = getText();
            QwertyKeyListener.markAsReplaced(text, chipStart, chipEnd, "");
            text.setSpan(drawableRecipientChipConstructChipSpan, chipStart, chipEnd, 33);
        }
    }

    private int calculateNumChipsCanShow() {
        int chipWidth;
        boolean z;
        DrawableRecipientChip[] sortedRecipients = getSortedRecipients();
        if (sortedRecipients == null || sortedRecipients.length == 0) {
            return 0;
        }
        int iCalculateAvailableWidth = (int) calculateAvailableWidth();
        int length = sortedRecipients.length;
        int chipInterval = getChipInterval();
        int i = 0;
        int chipWidth2 = 0;
        while (true) {
            if (i < length) {
                chipWidth2 += getChipWidth(sortedRecipients[i]) + (chipInterval * 2);
                if (chipWidth2 <= (this.mChipPadding * 2) + iCalculateAvailableWidth) {
                    i++;
                } else {
                    chipWidth = chipWidth2;
                    z = false;
                    break;
                }
            } else {
                chipWidth = chipWidth2;
                z = true;
                break;
            }
        }
        if (z) {
            return length;
        }
        if (i == length && !z) {
            i--;
        }
        int measuredMoreSpanWidth = iCalculateAvailableWidth - (getMeasuredMoreSpanWidth(length - i) + chipInterval);
        while (i >= 0) {
            chipWidth -= getChipWidth(sortedRecipients[i]) + chipInterval;
            if (chipWidth < measuredMoreSpanWidth) {
                break;
            }
            i--;
        }
        if (i == 0) {
            if (getChipWidth(sortedRecipients[0]) > measuredMoreSpanWidth) {
                replaceChipOnSameTextRange(sortedRecipients[0], measuredMoreSpanWidth);
                this.mHasEllipsizedFirstChip = true;
            }
            return 1;
        }
        return i;
    }

    @Override
    public boolean moveCursorToVisibleOffset() {
        if (isPhoneQuery() && !this.mMoveCursorToVisible) {
            this.mMoveCursorToVisible = true;
            return false;
        }
        return super.moveCursorToVisibleOffset();
    }

    private void setDisableBringPointIntoView(boolean z) {
        printDebugLog("RecipientEditTextView", "[setDisableBringPointIntoView] " + z);
        this.mDisableBringPointIntoView = z;
    }

    private void setForceEnableBringPointIntoView(boolean z) {
        printDebugLog("RecipientEditTextView", "[setForceEnableBringPointIntoView] " + z);
        this.mForceEnableBringPointIntoView = z;
    }

    @Override
    public boolean bringPointIntoView(int i) {
        Log.d("RecipientEditTextView", "bringPointIntoView = " + i);
        if (this.mForceEnableBringPointIntoView) {
            return super.bringPointIntoView(i);
        }
        if (this.mDisableBringPointIntoView || this.mSelectedChip != null) {
            return false;
        }
        return super.bringPointIntoView(i);
    }

    @Override
    public boolean onPreDraw() {
        boolean zOnPreDraw = super.onPreDraw();
        Log.d("RecipientEditTextView", " setDisableBringPointIntoView(result) in the onPreDraw() ");
        setDisableBringPointIntoView(true);
        return zOnPreDraw;
    }

    private int getOffsetFromBottom(int i) {
        if (i == getLineCount() - 1) {
            return 0;
        }
        return -(getHeight() - ((getPaddingTop() + getLayout().getLineTop(i + 1)) - getScrollY()));
    }

    private boolean isEndChip() {
        Editable text = getText();
        int selectionEnd = getSelectionEnd();
        int i = selectionEnd - 1;
        while (i > 0) {
            char cCharAt = text.charAt(i);
            if (cCharAt == ',' || cCharAt == ';') {
                break;
            }
            i--;
        }
        if (selectionEnd - i <= 2 && i != 0) {
            return true;
        }
        return false;
    }

    @Override
    public void showDropDown() {
        try {
            if (!isPhoneQuery() || !isEndChip()) {
                super.showDropDown();
            } else {
                dismissDropDown();
            }
        } catch (Exception e) {
            Log.d("RecipientEditTextView", "showDropDown fail");
        }
    }

    protected boolean isTouchPointInChip(float f, float f2) {
        Layout layout = getLayout();
        if (layout != null) {
            float primaryHorizontal = layout.getPrimaryHorizontal(layout.getLineEnd(layout.getLineForOffset(getOffsetForPosition(f, f2))) - 1);
            float fMin = Math.min((getWidth() - getTotalPaddingRight()) - 1, Math.max(0.0f, f - getTotalPaddingLeft())) + getScrollX();
            boolean z = getResources().getConfiguration().getLayoutDirection() == 1;
            if (z && !isInputtingNumber()) {
                z = false;
            }
            if (z && fMin < primaryHorizontal) {
                return false;
            }
            if (fMin > primaryHorizontal && !z) {
                return false;
            }
        }
        return true;
    }

    private boolean isInputtingNumber() {
        int chipEnd;
        DrawableRecipientChip lastChip = getLastChip();
        if (lastChip != null) {
            chipEnd = getChipEnd(lastChip) + 1;
        } else {
            chipEnd = 0;
        }
        return chipEnd < getText().length() && (this.mRemovedSpans == null || this.mRemovedSpans.size() == 0);
    }

    protected boolean isTouchPointInChipVertical(float f) {
        Layout layout = getLayout();
        if (layout != null) {
            float totalPaddingTop = f - getTotalPaddingTop();
            float lineBottom = layout.getLineBottom(0) - layout.getLineTop(0);
            if ((totalPaddingTop + getScrollY()) % lineBottom <= lineBottom - this.mChipHeight) {
                return false;
            }
            return true;
        }
        return true;
    }

    @Override
    protected void onConfigurationChanged(Configuration configuration) {
        printDebugLog("RecipientEditTextView", "[onConfigurationChanged] current view width=" + getWidth() + ", height=" + getHeight() + ", line count=" + getLineCount());
        if (isPhoneQuery()) {
            registerGlobalLayoutListener();
        }
        if (isFocused() && this.mSelectedChip != null && !shouldShowEditableText(this.mSelectedChip)) {
            clearSelectedChip();
        }
    }

    private void registerGlobalLayoutListener() {
        ViewTreeObserver viewTreeObserver = getViewTreeObserver();
        if (this.mGlobalLayoutListener == null) {
            this.mGlobalLayoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    MTKRecipientEditTextView.this.printDebugLog("RecipientEditTextView", "[onGlobalLayout] current view width=" + MTKRecipientEditTextView.this.getWidth() + ", height=" + MTKRecipientEditTextView.this.getHeight() + ", line count=" + MTKRecipientEditTextView.this.getLineCount());
                    if (MTKRecipientEditTextView.this.mCurrentWidth != MTKRecipientEditTextView.this.getWidth()) {
                        MTKRecipientEditTextView.this.mCurrentWidth = MTKRecipientEditTextView.this.getWidth();
                        if (!MTKRecipientEditTextView.this.isFocused()) {
                            if (MTKRecipientEditTextView.this.getResources().getConfiguration().orientation == 1) {
                                MTKRecipientEditTextView.this.rotateToPortrait();
                            } else {
                                MTKRecipientEditTextView.this.rotateToLandscape();
                            }
                            MTKRecipientEditTextView.this.requestLayout();
                        }
                    }
                    MTKRecipientEditTextView.this.mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            MTKRecipientEditTextView.this.printDebugLog("RecipientEditTextView", "[onGlobalLayout][run] setDisableBringPointIntoView(false)");
                            MTKRecipientEditTextView.this.setDisableBringPointIntoView(false);
                            MTKRecipientEditTextView.this.bringPointIntoView(MTKRecipientEditTextView.this.getSelectionStart());
                        }
                    });
                    MTKRecipientEditTextView.this.unRegisterGlobalLayoutListener();
                }
            };
            viewTreeObserver.addOnGlobalLayoutListener(this.mGlobalLayoutListener);
        }
    }

    private void unRegisterGlobalLayoutListener() {
        if (this.mGlobalLayoutListener != null) {
            getViewTreeObserver().removeGlobalOnLayoutListener(this.mGlobalLayoutListener);
            this.mGlobalLayoutListener = null;
        }
    }

    private void rotateToPortrait() {
        printDebugLog("RecipientEditTextView", "[rotateToPortrait] current view width=" + getWidth() + ", height=" + getHeight() + ", line count=" + getLineCount());
        DrawableRecipientChip[] sortedRecipients = getSortedRecipients();
        int length = sortedRecipients.length;
        if (sortedRecipients == null || length == 0) {
            return;
        }
        if (this.mMoreChip == null) {
            createMoreChip();
            return;
        }
        int iCalculateAvailableWidth = (int) calculateAvailableWidth();
        int measuredMoreSpanWidth = getMeasuredMoreSpanWidth(this.mRemovedSpans.size());
        int chipInterval = getChipInterval();
        int i = iCalculateAvailableWidth - measuredMoreSpanWidth;
        int chipWidth = i;
        int i2 = 0;
        while (i2 < length) {
            chipWidth -= getChipWidth(sortedRecipients[i2]) + chipInterval;
            if (chipWidth <= 0) {
                break;
            } else {
                i2++;
            }
        }
        if (i2 == length) {
            if (chipWidth >= 0) {
                return;
            } else {
                i2--;
            }
        }
        if (length == 1) {
            if (i2 == 0 && chipWidth < 0) {
                replaceChipOnSameTextRange(sortedRecipients[0], i);
                this.mHasEllipsizedFirstChip = true;
                return;
            }
            return;
        }
        if (i2 == 0) {
            i2++;
            if (chipWidth < 0) {
                replaceChipOnSameTextRange(sortedRecipients[0], i);
                this.mHasEllipsizedFirstChip = true;
            }
        }
        Spannable spannable = getSpannable();
        Editable text = getText();
        int spanStart = spannable.getSpanStart(sortedRecipients[i2]);
        int spanEnd = spannable.getSpanEnd(this.mMoreChip);
        int i3 = 0;
        while (i2 < length) {
            int i4 = i3 + 1;
            this.mRemovedSpans.add(i3, sortedRecipients[i2]);
            if (this.mTemporaryRecipients == null || !this.mTemporaryRecipients.contains(sortedRecipients[i2])) {
                sortedRecipients[i2].setOriginalText(text.toString().substring(spannable.getSpanStart(sortedRecipients[i2]), spannable.getSpanEnd(sortedRecipients[i2])));
            }
            spannable.removeSpan(sortedRecipients[i2]);
            i2++;
            i3 = i4;
        }
        spannable.removeSpan(this.mMoreChip);
        MoreImageSpan moreImageSpanCreateMoreSpan = createMoreSpan(this.mRemovedSpans.size());
        SpannableString spannableString = new SpannableString(text.subSequence(spanStart, spanEnd));
        spannableString.setSpan(moreImageSpanCreateMoreSpan, 0, spannableString.length(), 33);
        printThreadingDebugLog("MTKRecip", "[rotateToPortrait] replace");
        text.replace(spanStart, spanEnd, spannableString);
        this.mMoreChip = moreImageSpanCreateMoreSpan;
    }

    private DrawableRecipientChip convertToVisibleChip(DrawableRecipientChip drawableRecipientChip) {
        boolean z;
        if (drawableRecipientChip instanceof InvisibleRecipientChip) {
            RecipientEntry entry = drawableRecipientChip.getEntry();
            if (TextUtils.isEmpty(entry.getDisplayName()) || TextUtils.equals(entry.getDisplayName(), entry.getDestination())) {
                z = true;
            } else {
                z = false;
            }
            return constructChipSpan(entry, false, z);
        }
        return drawableRecipientChip;
    }

    private void rotateToLandscape() {
        printDebugLog("RecipientEditTextView", "[rotateToLandscape] current view width=" + getWidth() + ", height=" + getHeight() + ", line count=" + getLineCount());
        DrawableRecipientChip[] sortedRecipients = getSortedRecipients();
        int length = sortedRecipients.length;
        if (sortedRecipients == null || length == 0) {
            return;
        }
        replaceChipOnSameTextRange(sortedRecipients[0], -1);
        this.mHasEllipsizedFirstChip = false;
        DrawableRecipientChip[] sortedRecipients2 = getSortedRecipients();
        if (this.mMoreChip == null) {
            return;
        }
        int iCalculateAvailableWidth = (int) calculateAvailableWidth();
        int measuredMoreSpanWidth = getMeasuredMoreSpanWidth(this.mRemovedSpans.size());
        int chipInterval = getChipInterval();
        if (length == 1) {
            int i = iCalculateAvailableWidth - (measuredMoreSpanWidth + chipInterval);
            if (i - getChipWidth(sortedRecipients2[0]) < 0) {
                replaceChipOnSameTextRange(sortedRecipients2[0], i);
                this.mHasEllipsizedFirstChip = true;
                return;
            }
        }
        int chipWidth = iCalculateAvailableWidth;
        for (int i2 = 0; i2 < length; i2++) {
            chipWidth -= getChipWidth(sortedRecipients2[i2]) + chipInterval;
            if (chipWidth <= 0) {
                break;
            }
        }
        int i3 = 0;
        while (i3 < this.mRemovedSpans.size() && (chipWidth = chipWidth - (getChipWidth(this.mRemovedSpans.get(i3)) + chipInterval)) > 0) {
            i3++;
        }
        if (i3 == this.mRemovedSpans.size()) {
            if (chipWidth >= 0) {
                expand();
                return;
            }
            i3--;
        }
        int chipWidth2 = chipWidth - measuredMoreSpanWidth;
        while (i3 >= 0) {
            chipWidth2 += getChipWidth(this.mRemovedSpans.get(i3)) + chipInterval;
            if (chipWidth2 >= 0) {
                break;
            } else {
                i3--;
            }
        }
        Spannable spannable = getSpannable();
        Editable text = getText();
        spannable.getSpanStart(this.mMoreChip);
        int spanEnd = spannable.getSpanEnd(this.mMoreChip);
        spannable.removeSpan(this.mMoreChip);
        int spanEnd2 = spannable.getSpanEnd(sortedRecipients2[length - 1]);
        ArrayList arrayList = new ArrayList();
        int i4 = spanEnd2;
        int i5 = 0;
        while (i5 < i3) {
            DrawableRecipientChip drawableRecipientChip = this.mRemovedSpans.get(0);
            String str = (String) drawableRecipientChip.getOriginalText();
            int iIndexOf = text.toString().indexOf(str, i4);
            int length2 = str.length() + iIndexOf;
            if (iIndexOf != -1) {
                printThreadingDebugLog("MTKRecip", "[rotateToLandscape] setSpan");
                DrawableRecipientChip drawableRecipientChipConvertToVisibleChip = convertToVisibleChip(drawableRecipientChip);
                text.setSpan(drawableRecipientChipConvertToVisibleChip, iIndexOf, length2, 33);
                arrayList.add(drawableRecipientChipConvertToVisibleChip);
            }
            this.mRemovedSpans.remove(0);
            i5++;
            i4 = length2;
        }
        MoreImageSpan moreImageSpanCreateMoreSpan = createMoreSpan(this.mRemovedSpans.size());
        int i6 = i4 + 1;
        SpannableString spannableString = new SpannableString(getText().subSequence(i6, spanEnd));
        spannableString.setSpan(moreImageSpanCreateMoreSpan, 0, spannableString.length(), 33);
        printThreadingDebugLog("MTKRecip", "[rotateToLandscape] replace");
        getText().replace(i6, spanEnd, spannableString);
        this.mMoreChip = moreImageSpanCreateMoreSpan;
    }

    private void setChipProcessingMode(PROCESSING_MODE processing_mode) {
        printDebugLog("RecipientEditTextView", "[setChipProcessingMode] from: " + this.mChipProcessingMode + ", to: " + processing_mode);
        this.mChipProcessingMode = processing_mode;
    }

    private void tempLogPrint(String str, Object obj) {
        Spannable spannable = getSpannable();
        if (spannable == null || obj == null) {
            return;
        }
        int spanStart = spannable.getSpanStart(obj);
        int spanEnd = spannable.getSpanEnd(obj);
        int spanFlags = spannable.getSpanFlags(obj);
        String name = obj.getClass().getName();
        Log.d("RecipientEditTextView", "[Debug] " + str + " ---> spanStart=" + spanStart + ", spanEnd=" + spanEnd + ", spanFlag=" + spanFlags + ", spanID=" + obj.hashCode() + ", spanName=" + name);
    }

    private void recoverLayout() {
        printDebugLog("RecipientEditTextView", "[recoverLayout]");
        if (this.mIsAutoTesting) {
            return;
        }
        setSelection(getText().length());
        int width = getLayout() == null ? 0 : getLayout().getWidth();
        makeNewLayout(width, width, new BoringLayout.Metrics(), new BoringLayout.Metrics(), (getWidth() - getCompoundPaddingLeft()) - getCompoundPaddingRight(), false);
        requestLayout();
        invalidate();
    }

    private class DuplicateContactReplacementTask extends AsyncTask<Object, Void, Void> {
        private DuplicateContactReplacementTask() {
        }

        @Override
        protected Void doInBackground(Object... objArr) throws Throwable {
            MTKRecipientEditTextView.this.printDebugLog("RecipientEditTextView", "[DuplicateContactReplacementTask] start.");
            final DrawableRecipientChip drawableRecipientChip = (DrawableRecipientChip) objArr[0];
            final RecipientEntry entry = drawableRecipientChip.getEntry();
            final String destination = entry.getDestination();
            ArrayList arrayList = new ArrayList();
            HashSet hashSet = new HashSet();
            hashSet.add(drawableRecipientChip.getEntry().getDestination());
            arrayList.add(destination);
            BaseRecipientAdapter baseRecipientAdapter = (BaseRecipientAdapter) MTKRecipientEditTextView.this.getAdapter();
            RecipientAlternatesAdapter.getMatchingRecipients(hashSet, MTKRecipientEditTextView.this.getContext(), baseRecipientAdapter, arrayList, baseRecipientAdapter.getAccount(), new RecipientAlternatesAdapter.RecipientMatchCallback() {
                @Override
                public void matchesFound(Map<String, RecipientEntry> map) {
                    final RecipientEntry recipientEntry = map.get(destination);
                    if (recipientEntry != null && recipientEntry.getContactId() != entry.getContactId()) {
                        MTKRecipientEditTextView.this.printDebugLog("RecipientEditTextView", "[DuplicateContactReplacement] Post handleReplaceDuplicateChip.");
                        MTKRecipientEditTextView.this.mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                MTKRecipientEditTextView.this.handleReplaceDuplicateChip(drawableRecipientChip, recipientEntry);
                            }
                        });
                    }
                }

                @Override
                public void matchesNotFound(Set<String> set) {
                }
            });
            return null;
        }
    }

    private void handleReplaceDuplicateChip(DrawableRecipientChip drawableRecipientChip, RecipientEntry recipientEntry) {
        if (hasFocus() || this.mRemovedSpans == null || this.mRemovedSpans.size() == 0) {
            replaceChip(drawableRecipientChip, recipientEntry);
            printDebugLog("RecipientEditTextView", "[DuplicateContactReplacement] Replace contact from " + drawableRecipientChip.getEntry().getContactId() + " to " + recipientEntry.getContactId());
            return;
        }
        this.mPedingReplaceChips.add(drawableRecipientChip);
        this.mPedingReplaceEntries.add(recipientEntry);
        printDebugLog("RecipientEditTextView", "[DuplicateContactReplacement] Replace contact later.");
    }

    private void printThreadingDebugLog(String str, String str2) {
        if (DEBUG_THREADING_LOG) {
            Log.d(str, str2);
        }
    }

    private void printDebugLog(String str, String str2) {
        if (DEBUG_LOG) {
            Log.d(str, str2);
        }
    }

    private void registerVSync() {
        if (!this.isRegisterVSync && this.mChoreographer != null) {
            this.mChoreographer.postCallback(0, this.notifyChipChangedRunnable, null);
            this.isRegisterVSync = true;
        }
    }

    private void notifyChipChanged() {
        int chipEnd;
        Log.d("RecipientEditTextView", "[notifyChipChanged] begin");
        if (this.mChipChangedListeners == null || this.mChipChangedListeners.size() == 0) {
            return;
        }
        Trace.traceBegin(8L, "notifyChipChanged");
        Trace.traceBegin(8L, "notify-prepare");
        ArrayList<RecipientEntry> arrayList = new ArrayList<>();
        DrawableRecipientChip[] sortedRecipients = getSortedRecipients();
        if (sortedRecipients != null && sortedRecipients.length > 0) {
            for (DrawableRecipientChip drawableRecipientChip : sortedRecipients) {
                arrayList.add(drawableRecipientChip.getEntry());
            }
        }
        StringBuilder sb = new StringBuilder();
        if (this.mRemovedSpans != null && this.mRemovedSpans.size() > 0) {
            sb.append("[notifyChipChanged] mRemovedSpans.size()=" + this.mRemovedSpans.size() + "\n");
            for (int i = 0; i < this.mRemovedSpans.size(); i++) {
                arrayList.add(this.mRemovedSpans.get(i).getEntry());
            }
        }
        Log.d("RecipientEditTextView", sb.toString());
        String strTrim = "";
        DrawableRecipientChip lastChip = getLastChip();
        if (lastChip != null) {
            chipEnd = getChipEnd(lastChip) + 1;
        } else {
            chipEnd = 0;
        }
        if (chipEnd < getText().length() && (this.mRemovedSpans == null || this.mRemovedSpans.size() == 0)) {
            strTrim = getText().toString().substring(chipEnd, getText().length()).trim();
            printSensitiveDebugLog("RecipientEditTextView", "[notifyChipChanged] last string: " + strTrim);
        }
        if (this.mPendingChips != null && this.mPendingChips.size() > 0) {
            Log.d("RecipientEditTextView", "[notifyChipChanged] mPendingChips.size()=" + this.mPendingChips.size());
            for (int i2 = 0; i2 < this.mPendingChips.size(); i2++) {
                Rfc822Token[] rfc822TokenArr = Rfc822Tokenizer.tokenize(this.mPendingChips.get(i2));
                if (rfc822TokenArr != null && rfc822TokenArr.length > 0) {
                    arrayList.add(RecipientEntry.constructGeneratedEntry(rfc822TokenArr[0].getName(), rfc822TokenArr[0].getAddress(), true));
                }
            }
            strTrim = "";
        }
        Log.d("RecipientEditTextView", "[notifyChipChanged] allChips.size(): " + arrayList.size());
        StringBuilder sb2 = new StringBuilder();
        for (int i3 = 0; i3 < arrayList.size(); i3++) {
            sb2.append("[notifyChipChanged] allChips.get(" + i3 + "): " + arrayList.get(i3).getDestination() + "\n");
        }
        for (int i4 = 0; i4 < this.changedChipAddresses.size(); i4++) {
            sb2.append("[notifyChipChanged]" + i4 + " : " + this.changedChipAddresses.get(i4));
        }
        printSensitiveDebugLog("RecipientEditTextView", sb2.toString());
        Trace.traceEnd(8L);
        if (this.mChipChangedListeners != null) {
            ArrayList<ChipWatcher> arrayList2 = this.mChipChangedListeners;
            for (int i5 = 0; i5 < arrayList2.size(); i5++) {
                arrayList2.get(i5).onChipChanged(arrayList, this.changedChipAddresses, strTrim);
            }
        }
        Trace.traceEnd(8L);
    }

    private List<DrawableRecipientChip> getChipsByContactID(long j) {
        DrawableRecipientChip[] sortedRecipients = getSortedRecipients();
        ArrayList arrayList = new ArrayList();
        for (DrawableRecipientChip drawableRecipientChip : sortedRecipients) {
            if (drawableRecipientChip.getEntry().getContactId() == j) {
                arrayList.add(drawableRecipientChip);
            }
        }
        return arrayList;
    }

    private void handleContactChange(Set set) {
        DrawableRecipientChip invisibleRecipientChip;
        Iterator it = set.iterator();
        ArrayList arrayList = new ArrayList();
        ArrayList arrayList2 = new ArrayList();
        DrawableRecipientChip[] sortedRecipients = getSortedRecipients();
        ArrayList arrayList3 = new ArrayList();
        for (DrawableRecipientChip drawableRecipientChip : sortedRecipients) {
            arrayList3.add(tokenizeAndNormalizeAddress(drawableRecipientChip.getEntry().getDestination()));
        }
        ArrayList arrayList4 = new ArrayList();
        try {
            if (this.mRemovedSpans != null) {
                arrayList4.addAll(this.mRemovedSpans);
            }
        } catch (ConcurrentModificationException e) {
            e.printStackTrace();
        }
        if (arrayList4.size() > 0) {
            Iterator it2 = arrayList4.iterator();
            while (it2.hasNext()) {
                arrayList3.add(tokenizeAndNormalizeAddress(((DrawableRecipientChip) it2.next()).getEntry().getDestination()));
            }
        }
        while (it.hasNext()) {
            MTKContactObserver.DirtyContactEvent dirtyContactEvent = (MTKContactObserver.DirtyContactEvent) it.next();
            if (dirtyContactEvent.eventType != 0) {
                for (RecipientEntry recipientEntry : RecipientAlternatesAdapter.getRecipientEntryByContactID(getContext(), dirtyContactEvent.CID, isPhoneQuery())) {
                    Uri photoThumbnailUri = recipientEntry.getPhotoThumbnailUri();
                    if (photoThumbnailUri != null && ((BaseRecipientAdapter) getAdapter()) != null) {
                        ((BaseRecipientAdapter) getAdapter()).updatePhotoCacheByUri(photoThumbnailUri);
                    }
                    if (arrayList3.contains(tokenizeAndNormalizeAddress(recipientEntry.getDestination()))) {
                        try {
                            invisibleRecipientChip = constructChipSpan(recipientEntry, false, false);
                        } catch (NullPointerException e2) {
                            e2.printStackTrace();
                            invisibleRecipientChip = new InvisibleRecipientChip(recipientEntry);
                        }
                    } else {
                        invisibleRecipientChip = new InvisibleRecipientChip(recipientEntry);
                    }
                    arrayList.add(invisibleRecipientChip);
                }
            } else {
                arrayList2.add(Long.valueOf(dirtyContactEvent.CID));
            }
        }
        if (set.isEmpty()) {
        }
        switch (((MTKContactObserver.DirtyContactEvent) set.iterator().next()).eventType) {
            case 0:
                postHandleContactDelete(arrayList2);
                break;
            case 1:
                postHandleContactUpdate(arrayList);
                break;
            case 2:
                postHandleContactAdd(arrayList);
                break;
        }
    }

    private void postHandleContactDelete(final List<Long> list) {
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                Log.d("MTKRecipContactObserver", "+delete+");
                MTKRecipientEditTextView.this.handleContactDeleteAsync(list);
                Log.d("MTKRecipContactObserver", "-delete-");
            }
        });
    }

    private class DeleteContactTask extends AsyncTask<List<Long>, Object, HashMap<DrawableRecipientChip, DrawableRecipientChip>> {
        List<DrawableRecipientChip> mChipsNeedToReplace;
        private boolean mHasFocusInPreExe;
        private List<Long> mIDs;
        List<DrawableRecipientChip> mSpansNeedToReplace;

        private DeleteContactTask() {
            this.mChipsNeedToReplace = new ArrayList();
            this.mSpansNeedToReplace = new ArrayList();
            this.mHasFocusInPreExe = false;
        }

        public void setDeleteIDs(List<Long> list) {
            this.mIDs = list;
        }

        @Override
        protected void onPreExecute() {
            this.mHasFocusInPreExe = MTKRecipientEditTextView.this.hasFocus();
            Iterator<Long> it = this.mIDs.iterator();
            while (it.hasNext()) {
                long jLongValue = it.next().longValue();
                this.mChipsNeedToReplace.addAll(MTKRecipientEditTextView.this.getChipsByContactID(jLongValue));
                if (MTKRecipientEditTextView.this.mRemovedSpans != null) {
                    for (DrawableRecipientChip drawableRecipientChip : MTKRecipientEditTextView.this.mRemovedSpans) {
                        if (drawableRecipientChip.getEntry().getContactId() == jLongValue || drawableRecipientChip.getEntry().getContactId() == -2) {
                            if (!this.mSpansNeedToReplace.contains(drawableRecipientChip)) {
                                this.mSpansNeedToReplace.add(drawableRecipientChip);
                            }
                        }
                    }
                }
            }
        }

        @Override
        protected HashMap<DrawableRecipientChip, DrawableRecipientChip> doInBackground(List<Long>... listArr) {
            HashMap<DrawableRecipientChip, DrawableRecipientChip> map = new HashMap<>();
            ArrayList<DrawableRecipientChip> arrayList = new ArrayList();
            arrayList.addAll(this.mChipsNeedToReplace);
            arrayList.addAll(this.mSpansNeedToReplace);
            for (DrawableRecipientChip drawableRecipientChip : arrayList) {
                RecipientEntry recipientEntryByPhoneNumber = RecipientAlternatesAdapter.getRecipientEntryByPhoneNumber(MTKRecipientEditTextView.this.getContext(), MTKRecipientEditTextView.tokenizeAddress(drawableRecipientChip.getEntry().getDestination()));
                if (recipientEntryByPhoneNumber == null) {
                    recipientEntryByPhoneNumber = RecipientEntry.constructFakePhoneEntry(drawableRecipientChip.getEntry().getDestination(), true);
                }
                try {
                    map.put(drawableRecipientChip, MTKRecipientEditTextView.this.constructChipSpan(recipientEntryByPhoneNumber, false, false));
                } catch (NullPointerException e) {
                    e.printStackTrace();
                }
            }
            return map;
        }

        @Override
        protected void onPostExecute(HashMap<DrawableRecipientChip, DrawableRecipientChip> map) {
            boolean zHasFocus = MTKRecipientEditTextView.this.hasFocus();
            MTKRecipientEditTextView.this.removeTextChangedListener(MTKRecipientEditTextView.this.mTextWatcher);
            SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(MTKRecipientEditTextView.this.getText());
            ArrayList<DrawableRecipientChip> arrayList = new ArrayList();
            arrayList.addAll(this.mChipsNeedToReplace);
            if (!this.mHasFocusInPreExe && zHasFocus) {
                arrayList.addAll(this.mSpansNeedToReplace);
            }
            for (DrawableRecipientChip drawableRecipientChip : arrayList) {
                MTKRecipientEditTextView.this.replaceChip(drawableRecipientChip, map.get(drawableRecipientChip), spannableStringBuilder);
            }
            MTKRecipientEditTextView.this.mTextWatcher = new RecipientTextWatcher();
            MTKRecipientEditTextView.this.addTextChangedListener(MTKRecipientEditTextView.this.mTextWatcher);
            MTKRecipientEditTextView.this.setText(spannableStringBuilder);
            MTKRecipientEditTextView.this.setSelection(MTKRecipientEditTextView.this.getText().length());
            arrayList.clear();
            arrayList.addAll(this.mSpansNeedToReplace);
            if (this.mHasFocusInPreExe && !zHasFocus) {
                arrayList.addAll(this.mChipsNeedToReplace);
            }
            for (DrawableRecipientChip drawableRecipientChip2 : arrayList) {
                MTKRecipientEditTextView.this.replaceChipsInRemovedSpans(drawableRecipientChip2, map.get(drawableRecipientChip2));
            }
            MTKRecipientEditTextView.this.tryToAdjustChips();
        }
    }

    private void handleContactDeleteAsync(List<Long> list) {
        DeleteContactTask deleteContactTask = new DeleteContactTask();
        deleteContactTask.setDeleteIDs(list);
        deleteContactTask.execute(list);
    }

    private void handleContactDeleteSync(List<Long> list) {
        removeTextChangedListener(this.mTextWatcher);
        Iterator<Long> it = list.iterator();
        while (it.hasNext()) {
            long jLongValue = it.next().longValue();
            for (DrawableRecipientChip drawableRecipientChip : getChipsByContactID(jLongValue)) {
                replaceChip(drawableRecipientChip, RecipientEntry.constructFakePhoneEntry(drawableRecipientChip.getEntry().getDestination(), true));
            }
            setSelection(getText().length());
            if (this.mRemovedSpans == null) {
                return;
            }
            for (DrawableRecipientChip drawableRecipientChip2 : this.mRemovedSpans) {
                if (drawableRecipientChip2.getEntry().getContactId() == jLongValue) {
                    replaceChipsInRemovedSpans(drawableRecipientChip2, RecipientEntry.constructFakePhoneEntry(drawableRecipientChip2.getEntry().getDestination(), true));
                }
            }
        }
        this.mTextWatcher = new RecipientTextWatcher();
        addTextChangedListener(this.mTextWatcher);
    }

    private void postHandleContactAdd(final List<DrawableRecipientChip> list) {
        if (list.size() == 0) {
            return;
        }
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                Log.d("MTKRecipContactObserver", "+add+");
                MTKRecipientEditTextView.this.handleContactAdd(list);
                Log.d("MTKRecipContactObserver", "-add-");
            }
        });
    }

    private void handleContactAdd(List<DrawableRecipientChip> list) {
        this.mChipsMap = new HashMap<>();
        Log.d("RecipientEditTextView", "construct chips map begain");
        DrawableRecipientChip[] sortedRecipients = getSortedRecipients();
        if (sortedRecipients != null) {
            for (DrawableRecipientChip drawableRecipientChip : sortedRecipients) {
                String str = tokenizeAndNormalizeAddress(drawableRecipientChip.getEntry().getDestination());
                if (str != null && !str.isEmpty()) {
                    ArrayList<DrawableRecipientChip> arrayList = this.mChipsMap.get(str);
                    if (arrayList == null) {
                        arrayList = new ArrayList<>();
                    }
                    arrayList.add(drawableRecipientChip);
                    this.mChipsMap.put(str, arrayList);
                }
            }
        }
        this.mRemovedSpansMap = new HashMap<>();
        Log.d("RecipientEditTextView", "construct span map begain");
        if (this.mRemovedSpans != null) {
            for (DrawableRecipientChip drawableRecipientChip2 : this.mRemovedSpans) {
                String str2 = tokenizeAndNormalizeAddress(drawableRecipientChip2.getEntry().getDestination());
                if (str2 != null && !str2.isEmpty()) {
                    ArrayList<DrawableRecipientChip> arrayList2 = this.mRemovedSpansMap.get(str2);
                    if (arrayList2 == null) {
                        arrayList2 = new ArrayList<>();
                    }
                    arrayList2.add(drawableRecipientChip2);
                    this.mRemovedSpansMap.put(str2, arrayList2);
                }
            }
        }
        removeTextChangedListener(this.mTextWatcher);
        Editable text = getText();
        Log.d("RecipientEditTextView", "oriEditable = " + ((Object) text));
        Editable spannableStringBuilder = new SpannableStringBuilder(text);
        Iterator<DrawableRecipientChip> it = list.iterator();
        while (it.hasNext()) {
            handleContactAdd(it.next(), spannableStringBuilder);
        }
        Editable text2 = getText();
        Log.d("RecipientEditTextView", "currText = " + ((Object) new SpannableStringBuilder(text2)));
        setText(spannableStringBuilder);
        this.mTextWatcher = new RecipientTextWatcher();
        addTextChangedListener(this.mTextWatcher);
        setText(text2);
        setSelection(getText().length());
        tryToAdjustChips();
    }

    boolean addressMatches(String str, String str2) {
        if (str.equals(str2)) {
            return true;
        }
        Rfc822Token[] rfc822TokenArr = Rfc822Tokenizer.tokenize(str);
        if (rfc822TokenArr != null && rfc822TokenArr.length > 0) {
            str = rfc822TokenArr[0].getAddress();
        }
        Rfc822Token[] rfc822TokenArr2 = Rfc822Tokenizer.tokenize(str2);
        if (rfc822TokenArr2 != null && rfc822TokenArr2.length > 0) {
            str2 = rfc822TokenArr2[0].getAddress();
        }
        if (str.equals(str2)) {
            return true;
        }
        return isPhoneQuery() && PhoneNumberUtils.compare(PhoneNumberUtils.normalizeNumber(str), PhoneNumberUtils.normalizeNumber(str2));
    }

    private void handleContactAdd(DrawableRecipientChip drawableRecipientChip, Editable editable) {
        int chipEnd;
        ArrayList<DrawableRecipientChip> arrayList;
        Log.d("RecipientEditTextView", "handleContactAdd begin chip-->" + drawableRecipientChip);
        if (this.mTokenizer == null) {
            return;
        }
        Editable text = getText();
        String str = tokenizeAndNormalizeAddress(drawableRecipientChip.getEntry().getDestination());
        if (str == null || str.isEmpty()) {
            return;
        }
        ArrayList<DrawableRecipientChip> arrayList2 = this.mChipsMap.get(str);
        if (arrayList2 != null) {
            for (DrawableRecipientChip drawableRecipientChip2 : arrayList2) {
                if (RecipientEntry.isCreatedRecipient(drawableRecipientChip2.getEntry().getContactId()) && !(drawableRecipientChip2 instanceof InvisibleRecipientChip)) {
                    replaceChip(drawableRecipientChip2, drawableRecipientChip, text);
                }
            }
        }
        String strTrim = "";
        DrawableRecipientChip lastChip = getLastChip();
        if (lastChip != null) {
            chipEnd = getChipEnd(lastChip) + 1;
        } else {
            chipEnd = 0;
        }
        if (chipEnd < getText().length() && (this.mRemovedSpans == null || this.mRemovedSpans.size() == 0)) {
            strTrim = getText().toString().substring(chipEnd, getText().length()).trim();
        }
        if (!strTrim.isEmpty() && addressMatches(strTrim, str)) {
            drawableRecipientChip = convertToVisibleChip(drawableRecipientChip);
            String strCreateAddressText = createAddressText(drawableRecipientChip.getEntry());
            int length = strCreateAddressText.length() - 1;
            SpannableString spannableString = new SpannableString(strCreateAddressText);
            spannableString.setSpan(drawableRecipientChip, 0, length, 33);
            drawableRecipientChip.setOriginalText(spannableString.toString());
            printThreadingDebugLog("RecipientEditTextView", "[handleContactAdd] replace");
            text.replace(chipEnd, strTrim.length() + chipEnd, spannableString);
            if ((mFeatureSet & 2) != 0) {
                this.changedChipAddresses.add(drawableRecipientChip.getEntry().getDestination());
            }
        }
        if (this.mRemovedSpans == null) {
            return;
        }
        String str2 = tokenizeAndNormalizeAddress(drawableRecipientChip.getEntry().getDestination());
        Log.d("RecipientEditTextView", "handleContactAdd replace spans begain");
        if (str2 == null || str2.isEmpty() || (arrayList = this.mRemovedSpansMap.get(str2)) == null) {
            return;
        }
        Iterator<DrawableRecipientChip> it = arrayList.iterator();
        while (it.hasNext()) {
            replaceChipsInRemovedSpans(it.next(), drawableRecipientChip);
        }
    }

    private void postHandleContactUpdate(final List<DrawableRecipientChip> list) {
        final ArrayList arrayList = new ArrayList();
        if (list.size() == 0) {
            return;
        }
        Iterator<DrawableRecipientChip> it = list.iterator();
        while (it.hasNext()) {
            arrayList.add(Long.valueOf(it.next().getEntry().getContactId()));
        }
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                Log.d("MTKRecipContactObserver", "+update+");
                MTKRecipientEditTextView.this.performFiltering(MTKRecipientEditTextView.this.getText(), 0);
                MTKRecipientEditTextView.this.handleContactDeleteSync(arrayList);
                MTKRecipientEditTextView.this.handleContactAdd(list);
                Log.d("MTKRecipContactObserver", "-update-");
            }
        });
    }

    private void replaceChipsInRemovedSpans(DrawableRecipientChip drawableRecipientChip, DrawableRecipientChip drawableRecipientChip2) {
        if (this.mRemovedSpans == null || this.mRemovedSpans.size() <= 0) {
            return;
        }
        try {
            DrawableRecipientChip drawableRecipientChipConvertToVisibleChip = convertToVisibleChip(drawableRecipientChip2);
            int iIndexOf = this.mRemovedSpans.indexOf(drawableRecipientChip);
            if (iIndexOf == -1) {
                return;
            }
            drawableRecipientChipConvertToVisibleChip.setOriginalText(drawableRecipientChip.getOriginalText().toString());
            this.mRemovedSpans.set(iIndexOf, drawableRecipientChipConvertToVisibleChip);
            if ((mFeatureSet & 2) != 0) {
                this.changedChipAddresses.add(drawableRecipientChipConvertToVisibleChip.getEntry().getDestination());
            }
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    private void replaceChipsInRemovedSpans(DrawableRecipientChip drawableRecipientChip, RecipientEntry recipientEntry) {
        if (this.mRemovedSpans == null || this.mRemovedSpans.size() <= 0) {
            return;
        }
        try {
            DrawableRecipientChip drawableRecipientChipConstructChipSpan = constructChipSpan(recipientEntry, false, false);
            drawableRecipientChipConstructChipSpan.setOriginalText(drawableRecipientChip.getOriginalText().toString());
            this.mRemovedSpans.set(this.mRemovedSpans.indexOf(drawableRecipientChip), drawableRecipientChipConstructChipSpan);
            if ((mFeatureSet & 2) != 0) {
                this.changedChipAddresses.add(recipientEntry.getDestination());
            }
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    void replaceChip(DrawableRecipientChip drawableRecipientChip, DrawableRecipientChip drawableRecipientChip2, Editable editable) {
        int iMin;
        try {
            DrawableRecipientChip drawableRecipientChipConvertToVisibleChip = convertToVisibleChip(drawableRecipientChip2);
            int spanStart = editable.getSpanStart(drawableRecipientChip);
            if (spanStart != -1) {
                int iMin2 = Math.min(editable.getSpanEnd(drawableRecipientChip) + 1, editable.length());
                editable.removeSpan(drawableRecipientChip);
                String strCreateAddressText = createAddressText(drawableRecipientChipConvertToVisibleChip.getEntry());
                if (strCreateAddressText != null) {
                    SpannableString spannableString = new SpannableString(strCreateAddressText.trim() + " ");
                    spannableString.setSpan(drawableRecipientChipConvertToVisibleChip, 0, spannableString.length() + (-1), 33);
                    if (editable.charAt(Math.min(iMin2, editable.length() - 1)) == ' ') {
                        iMin = Math.min(iMin2 + 1, editable.length());
                    } else {
                        iMin = iMin2;
                    }
                    printDebugLog("RecipientEditTextView", "[run] replace text, start: " + spanStart + ", end: " + iMin2 + ", text: " + ((Object) spannableString));
                    editable.replace(spanStart, iMin, spannableString);
                    drawableRecipientChipConvertToVisibleChip.setOriginalText(spannableString.toString());
                    if ((mFeatureSet & 2) != 0) {
                        this.changedChipAddresses.add(drawableRecipientChipConvertToVisibleChip.getEntry().getDestination());
                    }
                }
            }
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    private void tryToAdjustChips() {
        printDebugLog("RecipientEditTextView", "tryToAdjustChips");
        DrawableRecipientChip[] sortedRecipients = getSortedRecipients();
        int length = sortedRecipients.length;
        if (sortedRecipients == null || length == 0 || this.mRemovedSpans == null || this.mRemovedSpans.size() <= 0) {
            return;
        }
        int iCalculateAvailableWidth = (int) calculateAvailableWidth();
        int measuredMoreSpanWidth = getMeasuredMoreSpanWidth(this.mRemovedSpans.size());
        int chipInterval = getChipInterval();
        int i = iCalculateAvailableWidth - measuredMoreSpanWidth;
        int chipWidth = i;
        int i2 = 0;
        while (i2 < length) {
            chipWidth -= getChipWidth(sortedRecipients[i2]) + chipInterval;
            if (chipWidth <= 0) {
                break;
            } else {
                i2++;
            }
        }
        if (i2 == length) {
            if (chipWidth >= 0) {
                return;
            } else {
                i2--;
            }
        }
        if (length == 1) {
            if (i2 == 0 && chipWidth < 0) {
                replaceChipOnSameTextRange(sortedRecipients[0], i);
                this.mHasEllipsizedFirstChip = true;
                return;
            }
            return;
        }
        if (i2 == 0) {
            i2++;
            if (chipWidth < 0) {
                replaceChipOnSameTextRange(sortedRecipients[0], i);
                this.mHasEllipsizedFirstChip = true;
            }
        }
        Spannable spannable = getSpannable();
        Editable text = getText();
        int spanStart = spannable.getSpanStart(sortedRecipients[i2]);
        int spanEnd = spannable.getSpanEnd(this.mMoreChip);
        int i3 = 0;
        while (i2 < length) {
            this.mRemovedSpans.add(i3, sortedRecipients[i2]);
            spannable.removeSpan(sortedRecipients[i2]);
            i2++;
            i3++;
        }
        spannable.removeSpan(this.mMoreChip);
        MoreImageSpan moreImageSpanCreateMoreSpan = createMoreSpan(this.mRemovedSpans.size());
        SpannableString spannableString = new SpannableString(text.subSequence(spanStart, spanEnd));
        spannableString.setSpan(moreImageSpanCreateMoreSpan, 0, spannableString.length(), 33);
        printThreadingDebugLog("MTKRecip", "[rotateToPortrait] replace");
        text.replace(spanStart, spanEnd, spannableString);
        this.mMoreChip = moreImageSpanCreateMoreSpan;
    }

    private void configFeatures(Context context) {
        if ("com.android.mms".equals(context.getPackageName()) || "com.mediatek.rcs.message".equals(context.getPackageName())) {
            mFeatureSet |= 3;
        }
        if ((mFeatureSet & 1) != 0 && sContactObserver == null) {
            sContactObserver = new MTKContactObserver(getContext());
        }
        if ((mFeatureSet & 2) != 0) {
            this.mLastStringChanged = false;
            this.mChoreographer = Choreographer.getInstance();
            this.mChoreographer.postCallback(0, this.notifyChipChangedRunnable, null);
            this.isRegisterVSync = true;
        }
    }

    private class PreloadPhotoTask extends AsyncTask<Collection<RecipientEntry>, Void, Void> {
        private PreloadPhotoTask() {
        }

        @Override
        protected Void doInBackground(Collection<RecipientEntry>... collectionArr) {
            for (RecipientEntry recipientEntry : collectionArr[0]) {
                if (recipientEntry.getPhotoThumbnailUri() != null) {
                    Trace.traceBegin(8L, "preload " + recipientEntry.getContactId());
                    ((BaseRecipientAdapter) MTKRecipientEditTextView.this.getAdapter()).fetchPhoto(recipientEntry, recipientEntry.getPhotoThumbnailUri());
                    Trace.traceEnd(8L);
                }
            }
            return null;
        }
    }

    private Bitmap toRoundBitmap(Bitmap bitmap) {
        int i = this.mIconWidth;
        int i2 = this.mIconWidth;
        int i3 = i / 2;
        float f = i;
        Bitmap bitmapCreateBitmap = Bitmap.createBitmap(i, i, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmapCreateBitmap);
        Paint paint = new Paint(1);
        int i4 = (int) 0.0f;
        int i5 = (int) f;
        Rect rect = new Rect(i4, i4, i5, i5);
        Rect rect2 = new Rect(i4, i4, i5, i5);
        RectF rectF = new RectF(rect2);
        paint.setAntiAlias(true);
        canvas.setDrawFilter(new PaintFlagsDrawFilter(0, 3));
        canvas.drawARGB(0, 0, 0, 0);
        canvas.drawOval(rectF, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect2, paint);
        return bitmapCreateBitmap;
    }

    private static void printSensitiveDebugLog(String str, String str2) {
        if (piLoggable) {
            Log.d(str, str2);
        }
    }
}
