package android.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.PointF;
import android.graphics.RectF;
import android.os.AsyncTask;
import android.os.LocaleList;
import android.provider.Telephony;
import android.text.Layout;
import android.text.Selection;
import android.text.Spannable;
import android.text.TextUtils;
import android.text.util.Linkify;
import android.util.EventLog;
import android.util.Log;
import android.view.ActionMode;
import android.view.textclassifier.SelectionEvent;
import android.view.textclassifier.SelectionSessionLogger;
import android.view.textclassifier.TextClassification;
import android.view.textclassifier.TextClassificationConstants;
import android.view.textclassifier.TextClassificationManager;
import android.view.textclassifier.TextClassifier;
import android.view.textclassifier.TextSelection;
import android.widget.Editor;
import android.widget.SelectionActionModeHelper;
import android.widget.SmartSelectSprite;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.Preconditions;
import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;

@VisibleForTesting(visibility = VisibleForTesting.Visibility.PACKAGE)
public final class SelectionActionModeHelper {
    private static final String LOG_TAG = "SelectActionModeHelper";
    private final Editor mEditor;
    private final SelectionTracker mSelectionTracker;
    private final SmartSelectSprite mSmartSelectSprite;
    private TextClassification mTextClassification;
    private AsyncTask mTextClassificationAsyncTask;
    private final TextClassificationHelper mTextClassificationHelper;
    private final TextView mTextView;

    SelectionActionModeHelper(Editor editor) {
        this.mEditor = (Editor) Preconditions.checkNotNull(editor);
        this.mTextView = this.mEditor.getTextView();
        Context context = this.mTextView.getContext();
        TextView textView = this.mTextView;
        Objects.requireNonNull(textView);
        this.mTextClassificationHelper = new TextClassificationHelper(context, new $$Lambda$yIdmBO6ZxaY03PGN08RySVVQXuE(textView), getText(this.mTextView), 0, 1, this.mTextView.getTextLocales());
        this.mSelectionTracker = new SelectionTracker(this.mTextView);
        if (getTextClassificationSettings().isSmartSelectionAnimationEnabled()) {
            Context context2 = this.mTextView.getContext();
            int i = editor.getTextView().mHighlightColor;
            final TextView textView2 = this.mTextView;
            Objects.requireNonNull(textView2);
            this.mSmartSelectSprite = new SmartSelectSprite(context2, i, new Runnable() {
                @Override
                public final void run() {
                    textView2.invalidate();
                }
            });
            return;
        }
        this.mSmartSelectSprite = null;
    }

    public void startSelectionActionModeAsync(boolean z) {
        Supplier __lambda_aogbsmc_jnvtdjezylrtz35napi;
        boolean zIsSmartSelectionEnabled = z & getTextClassificationSettings().isSmartSelectionEnabled();
        this.mSelectionTracker.onOriginalSelection(getText(this.mTextView), this.mTextView.getSelectionStart(), this.mTextView.getSelectionEnd(), false);
        cancelAsyncTask();
        if (skipTextClassification()) {
            startSelectionActionMode(null);
            return;
        }
        resetTextClassificationHelper();
        TextView textView = this.mTextView;
        int timeoutDuration = this.mTextClassificationHelper.getTimeoutDuration();
        if (zIsSmartSelectionEnabled) {
            final TextClassificationHelper textClassificationHelper = this.mTextClassificationHelper;
            Objects.requireNonNull(textClassificationHelper);
            __lambda_aogbsmc_jnvtdjezylrtz35napi = new Supplier() {
                @Override
                public final Object get() {
                    return textClassificationHelper.suggestSelection();
                }
            };
        } else {
            TextClassificationHelper textClassificationHelper2 = this.mTextClassificationHelper;
            Objects.requireNonNull(textClassificationHelper2);
            __lambda_aogbsmc_jnvtdjezylrtz35napi = new $$Lambda$aOGBsMC_jnvTDjezYLRtz35nAPI(textClassificationHelper2);
        }
        Supplier supplier = __lambda_aogbsmc_jnvtdjezylrtz35napi;
        Consumer consumer = this.mSmartSelectSprite != null ? new Consumer() {
            @Override
            public final void accept(Object obj) {
                this.f$0.startSelectionActionModeWithSmartSelectAnimation((SelectionActionModeHelper.SelectionResult) obj);
            }
        } : new Consumer() {
            @Override
            public final void accept(Object obj) {
                this.f$0.startSelectionActionMode((SelectionActionModeHelper.SelectionResult) obj);
            }
        };
        TextClassificationHelper textClassificationHelper3 = this.mTextClassificationHelper;
        Objects.requireNonNull(textClassificationHelper3);
        this.mTextClassificationAsyncTask = new TextClassificationAsyncTask(textView, timeoutDuration, supplier, consumer, new $$Lambda$etfJkiCJnT2dqM2O4M2TCm9i_oA(textClassificationHelper3)).execute(new Void[0]);
    }

    public void startLinkActionModeAsync(int i, int i2) {
        this.mSelectionTracker.onOriginalSelection(getText(this.mTextView), i, i2, true);
        cancelAsyncTask();
        if (skipTextClassification()) {
            startLinkActionMode(null);
            return;
        }
        resetTextClassificationHelper(i, i2);
        TextView textView = this.mTextView;
        int timeoutDuration = this.mTextClassificationHelper.getTimeoutDuration();
        TextClassificationHelper textClassificationHelper = this.mTextClassificationHelper;
        Objects.requireNonNull(textClassificationHelper);
        $$Lambda$aOGBsMC_jnvTDjezYLRtz35nAPI __lambda_aogbsmc_jnvtdjezylrtz35napi = new $$Lambda$aOGBsMC_jnvTDjezYLRtz35nAPI(textClassificationHelper);
        Consumer consumer = new Consumer() {
            @Override
            public final void accept(Object obj) {
                this.f$0.startLinkActionMode((SelectionActionModeHelper.SelectionResult) obj);
            }
        };
        TextClassificationHelper textClassificationHelper2 = this.mTextClassificationHelper;
        Objects.requireNonNull(textClassificationHelper2);
        this.mTextClassificationAsyncTask = new TextClassificationAsyncTask(textView, timeoutDuration, __lambda_aogbsmc_jnvtdjezylrtz35napi, consumer, new $$Lambda$etfJkiCJnT2dqM2O4M2TCm9i_oA(textClassificationHelper2)).execute(new Void[0]);
    }

    public void invalidateActionModeAsync() {
        cancelAsyncTask();
        if (skipTextClassification()) {
            invalidateActionMode(null);
            return;
        }
        resetTextClassificationHelper();
        TextView textView = this.mTextView;
        int timeoutDuration = this.mTextClassificationHelper.getTimeoutDuration();
        TextClassificationHelper textClassificationHelper = this.mTextClassificationHelper;
        Objects.requireNonNull(textClassificationHelper);
        $$Lambda$aOGBsMC_jnvTDjezYLRtz35nAPI __lambda_aogbsmc_jnvtdjezylrtz35napi = new $$Lambda$aOGBsMC_jnvTDjezYLRtz35nAPI(textClassificationHelper);
        Consumer consumer = new Consumer() {
            @Override
            public final void accept(Object obj) {
                this.f$0.invalidateActionMode((SelectionActionModeHelper.SelectionResult) obj);
            }
        };
        TextClassificationHelper textClassificationHelper2 = this.mTextClassificationHelper;
        Objects.requireNonNull(textClassificationHelper2);
        this.mTextClassificationAsyncTask = new TextClassificationAsyncTask(textView, timeoutDuration, __lambda_aogbsmc_jnvtdjezylrtz35napi, consumer, new $$Lambda$etfJkiCJnT2dqM2O4M2TCm9i_oA(textClassificationHelper2)).execute(new Void[0]);
    }

    public void onSelectionAction(int i) {
        this.mSelectionTracker.onSelectionAction(this.mTextView.getSelectionStart(), this.mTextView.getSelectionEnd(), getActionType(i), this.mTextClassification);
    }

    public void onSelectionDrag() {
        this.mSelectionTracker.onSelectionAction(this.mTextView.getSelectionStart(), this.mTextView.getSelectionEnd(), 106, this.mTextClassification);
    }

    public void onTextChanged(int i, int i2) {
        this.mSelectionTracker.onTextChanged(i, i2, this.mTextClassification);
    }

    public boolean resetSelection(int i) {
        if (this.mSelectionTracker.resetSelection(i, this.mEditor)) {
            invalidateActionModeAsync();
            return true;
        }
        return false;
    }

    public TextClassification getTextClassification() {
        return this.mTextClassification;
    }

    public void onDestroyActionMode() {
        cancelSmartSelectAnimation();
        this.mSelectionTracker.onSelectionDestroyed();
        cancelAsyncTask();
    }

    public void onDraw(Canvas canvas) {
        if (isDrawingHighlight() && this.mSmartSelectSprite != null) {
            this.mSmartSelectSprite.draw(canvas);
        }
    }

    public boolean isDrawingHighlight() {
        return this.mSmartSelectSprite != null && this.mSmartSelectSprite.isAnimationActive();
    }

    private TextClassificationConstants getTextClassificationSettings() {
        return TextClassificationManager.getSettings(this.mTextView.getContext());
    }

    private void cancelAsyncTask() {
        if (this.mTextClassificationAsyncTask != null) {
            this.mTextClassificationAsyncTask.cancel(true);
            this.mTextClassificationAsyncTask = null;
        }
        this.mTextClassification = null;
    }

    private boolean skipTextClassification() {
        return this.mTextView.usesNoOpTextClassifier() || (this.mTextView.getSelectionEnd() == this.mTextView.getSelectionStart()) || (this.mTextView.hasPasswordTransformationMethod() || TextView.isPasswordInputType(this.mTextView.getInputType()));
    }

    private void startLinkActionMode(SelectionResult selectionResult) {
        startActionMode(2, selectionResult);
    }

    private void startSelectionActionMode(SelectionResult selectionResult) {
        startActionMode(0, selectionResult);
    }

    private void startActionMode(@Editor.TextActionMode int i, SelectionResult selectionResult) {
        CharSequence text = getText(this.mTextView);
        if (selectionResult != null && (text instanceof Spannable) && (this.mTextView.isTextSelectable() || this.mTextView.isTextEditable())) {
            if (!getTextClassificationSettings().isModelDarkLaunchEnabled()) {
                Selection.setSelection((Spannable) text, selectionResult.mStart, selectionResult.mEnd);
                this.mTextView.invalidate();
            }
            this.mTextClassification = selectionResult.mClassification;
        } else if (selectionResult == null || i != 2) {
            this.mTextClassification = null;
        } else {
            this.mTextClassification = selectionResult.mClassification;
        }
        if (this.mEditor.startActionModeInternal(i)) {
            Editor.SelectionModifierCursorController selectionController = this.mEditor.getSelectionController();
            if (selectionController != null && (this.mTextView.isTextSelectable() || this.mTextView.isTextEditable())) {
                selectionController.show();
            }
            if (selectionResult != null) {
                if (i == 0) {
                    this.mSelectionTracker.onSmartSelection(selectionResult);
                } else if (i == 2) {
                    this.mSelectionTracker.onLinkSelected(selectionResult);
                }
            }
        }
        this.mEditor.setRestartActionModeOnNextRefresh(false);
        this.mTextClassificationAsyncTask = null;
    }

    private void startSelectionActionModeWithSmartSelectAnimation(final SelectionResult selectionResult) {
        Layout layout = this.mTextView.getLayout();
        Runnable runnable = new Runnable() {
            @Override
            public final void run() {
                SelectionActionModeHelper.lambda$startSelectionActionModeWithSmartSelectAnimation$0(this.f$0, selectionResult);
            }
        };
        if (!((selectionResult == null || (this.mTextView.getSelectionStart() == selectionResult.mStart && this.mTextView.getSelectionEnd() == selectionResult.mEnd)) ? false : true)) {
            runnable.run();
        } else {
            List<SmartSelectSprite.RectangleWithTextSelectionLayout> listConvertSelectionToRectangles = convertSelectionToRectangles(layout, selectionResult.mStart, selectionResult.mEnd);
            this.mSmartSelectSprite.startAnimation(movePointInsideNearestRectangle(new PointF(this.mEditor.getLastUpPositionX(), this.mEditor.getLastUpPositionY()), listConvertSelectionToRectangles, $$Lambda$ChL7kntlZCrPaPVdRfaSzGdk1JU.INSTANCE), listConvertSelectionToRectangles, runnable);
        }
    }

    public static void lambda$startSelectionActionModeWithSmartSelectAnimation$0(SelectionActionModeHelper selectionActionModeHelper, SelectionResult selectionResult) {
        if (selectionResult == null || selectionResult.mStart < 0 || selectionResult.mEnd > getText(selectionActionModeHelper.mTextView).length() || selectionResult.mStart > selectionResult.mEnd) {
            selectionResult = null;
        }
        selectionActionModeHelper.startSelectionActionMode(selectionResult);
    }

    private List<SmartSelectSprite.RectangleWithTextSelectionLayout> convertSelectionToRectangles(Layout layout, int i, int i2) {
        final ArrayList arrayList = new ArrayList();
        layout.getSelection(i, i2, new Layout.SelectionRectangleConsumer() {
            @Override
            public final void accept(float f, float f2, float f3, float f4, int i3) {
                SelectionActionModeHelper.mergeRectangleIntoList(arrayList, new RectF(f, f2, f3, f4), $$Lambda$ChL7kntlZCrPaPVdRfaSzGdk1JU.INSTANCE, new Function() {
                    @Override
                    public final Object apply(Object obj) {
                        return SelectionActionModeHelper.lambda$convertSelectionToRectangles$1(i3, (RectF) obj);
                    }
                });
            }
        });
        arrayList.sort(Comparator.comparing($$Lambda$ChL7kntlZCrPaPVdRfaSzGdk1JU.INSTANCE, SmartSelectSprite.RECTANGLE_COMPARATOR));
        return arrayList;
    }

    static SmartSelectSprite.RectangleWithTextSelectionLayout lambda$convertSelectionToRectangles$1(int i, RectF rectF) {
        return new SmartSelectSprite.RectangleWithTextSelectionLayout(rectF, i);
    }

    @VisibleForTesting
    public static <T> void mergeRectangleIntoList(List<T> list, RectF rectF, Function<T, RectF> function, Function<RectF, T> function2) {
        if (rectF.isEmpty()) {
            return;
        }
        int size = list.size();
        int i = 0;
        while (true) {
            boolean z = true;
            if (i < size) {
                RectF rectFApply = function.apply(list.get(i));
                if (rectFApply.contains(rectF)) {
                    return;
                }
                if (rectF.contains(rectFApply)) {
                    rectFApply.setEmpty();
                } else {
                    boolean z2 = rectF.left == rectFApply.right || rectF.right == rectFApply.left;
                    if (rectF.top != rectFApply.top || rectF.bottom != rectFApply.bottom || (!RectF.intersects(rectF, rectFApply) && !z2)) {
                        z = false;
                    }
                    if (z) {
                        rectF.union(rectFApply);
                        rectFApply.setEmpty();
                    }
                }
                i++;
            } else {
                for (int i2 = size - 1; i2 >= 0; i2--) {
                    if (function.apply(list.get(i2)).isEmpty()) {
                        list.remove(i2);
                    }
                }
                list.add(function2.apply(rectF));
                return;
            }
        }
    }

    @VisibleForTesting
    public static <T> PointF movePointInsideNearestRectangle(PointF pointF, List<T> list, Function<T, RectF> function) {
        float f;
        PointF pointF2 = pointF;
        int size = list.size();
        float f2 = -1.0f;
        int i = 0;
        double d = Double.MAX_VALUE;
        float f3 = -1.0f;
        while (i < size) {
            RectF rectFApply = function.apply(list.get(i));
            float fCenterY = rectFApply.centerY();
            if (pointF2.x > rectFApply.right) {
                f = rectFApply.right;
            } else if (pointF2.x < rectFApply.left) {
                f = rectFApply.left;
            } else {
                f = pointF2.x;
            }
            int i2 = size;
            double dPow = Math.pow(pointF2.x - f, 2.0d) + Math.pow(pointF2.y - fCenterY, 2.0d);
            if (dPow < d) {
                f2 = f;
                f3 = fCenterY;
                d = dPow;
            }
            i++;
            size = i2;
            pointF2 = pointF;
        }
        return new PointF(f2, f3);
    }

    private void invalidateActionMode(SelectionResult selectionResult) {
        TextClassification textClassification;
        cancelSmartSelectAnimation();
        if (selectionResult == null) {
            textClassification = null;
        } else {
            textClassification = selectionResult.mClassification;
        }
        this.mTextClassification = textClassification;
        ActionMode textActionMode = this.mEditor.getTextActionMode();
        if (textActionMode != null) {
            textActionMode.invalidate();
        }
        this.mSelectionTracker.onSelectionUpdated(this.mTextView.getSelectionStart(), this.mTextView.getSelectionEnd(), this.mTextClassification);
        this.mTextClassificationAsyncTask = null;
    }

    private void resetTextClassificationHelper(int i, int i2) {
        if (i < 0 || i2 < 0) {
            i = this.mTextView.getSelectionStart();
            i2 = this.mTextView.getSelectionEnd();
        }
        int i3 = i;
        int i4 = i2;
        TextClassificationHelper textClassificationHelper = this.mTextClassificationHelper;
        TextView textView = this.mTextView;
        Objects.requireNonNull(textView);
        textClassificationHelper.init(new $$Lambda$yIdmBO6ZxaY03PGN08RySVVQXuE(textView), getText(this.mTextView), i3, i4, this.mTextView.getTextLocales());
    }

    private void resetTextClassificationHelper() {
        resetTextClassificationHelper(-1, -1);
    }

    private void cancelSmartSelectAnimation() {
        if (this.mSmartSelectSprite != null) {
            this.mSmartSelectSprite.cancelAnimation();
        }
    }

    private static final class SelectionTracker {
        private boolean mAllowReset;
        private final LogAbandonRunnable mDelayedLogAbandon = new LogAbandonRunnable();
        private SelectionMetricsLogger mLogger;
        private int mOriginalEnd;
        private int mOriginalStart;
        private int mSelectionEnd;
        private int mSelectionStart;
        private final TextView mTextView;

        SelectionTracker(TextView textView) {
            this.mTextView = (TextView) Preconditions.checkNotNull(textView);
            this.mLogger = new SelectionMetricsLogger(textView);
        }

        public void onOriginalSelection(CharSequence charSequence, int i, int i2, boolean z) {
            this.mDelayedLogAbandon.flush();
            this.mSelectionStart = i;
            this.mOriginalStart = i;
            this.mSelectionEnd = i2;
            this.mOriginalEnd = i2;
            this.mAllowReset = false;
            maybeInvalidateLogger();
            this.mLogger.logSelectionStarted(this.mTextView.getTextClassificationSession(), charSequence, i, z ? 2 : 1);
        }

        public void onSmartSelection(SelectionResult selectionResult) {
            onClassifiedSelection(selectionResult);
            this.mLogger.logSelectionModified(selectionResult.mStart, selectionResult.mEnd, selectionResult.mClassification, selectionResult.mSelection);
        }

        public void onLinkSelected(SelectionResult selectionResult) {
            onClassifiedSelection(selectionResult);
        }

        private void onClassifiedSelection(SelectionResult selectionResult) {
            if (!isSelectionStarted()) {
                return;
            }
            this.mSelectionStart = selectionResult.mStart;
            this.mSelectionEnd = selectionResult.mEnd;
            this.mAllowReset = (this.mSelectionStart == this.mOriginalStart && this.mSelectionEnd == this.mOriginalEnd) ? false : true;
        }

        public void onSelectionUpdated(int i, int i2, TextClassification textClassification) {
            if (isSelectionStarted()) {
                this.mSelectionStart = i;
                this.mSelectionEnd = i2;
                this.mAllowReset = false;
                this.mLogger.logSelectionModified(i, i2, textClassification, null);
            }
        }

        public void onSelectionDestroyed() {
            this.mAllowReset = false;
            this.mDelayedLogAbandon.schedule(100);
        }

        public void onSelectionAction(int i, int i2, int i3, TextClassification textClassification) {
            if (isSelectionStarted()) {
                this.mAllowReset = false;
                this.mLogger.logSelectionAction(i, i2, i3, textClassification);
            }
        }

        public boolean resetSelection(int i, Editor editor) {
            TextView textView = editor.getTextView();
            if (!isSelectionStarted() || !this.mAllowReset || i < this.mSelectionStart || i > this.mSelectionEnd || !(SelectionActionModeHelper.getText(textView) instanceof Spannable)) {
                return false;
            }
            this.mAllowReset = false;
            boolean zSelectCurrentWord = editor.selectCurrentWord();
            if (zSelectCurrentWord) {
                this.mSelectionStart = editor.getTextView().getSelectionStart();
                this.mSelectionEnd = editor.getTextView().getSelectionEnd();
                this.mLogger.logSelectionAction(textView.getSelectionStart(), textView.getSelectionEnd(), 201, null);
            }
            return zSelectCurrentWord;
        }

        public void onTextChanged(int i, int i2, TextClassification textClassification) {
            if (isSelectionStarted() && i == this.mSelectionStart && i2 == this.mSelectionEnd) {
                onSelectionAction(i, i2, 100, textClassification);
            }
        }

        private void maybeInvalidateLogger() {
            if (this.mLogger.isEditTextLogger() != this.mTextView.isTextEditable()) {
                this.mLogger = new SelectionMetricsLogger(this.mTextView);
            }
        }

        private boolean isSelectionStarted() {
            return this.mSelectionStart >= 0 && this.mSelectionEnd >= 0 && this.mSelectionStart != this.mSelectionEnd;
        }

        private final class LogAbandonRunnable implements Runnable {
            private boolean mIsPending;

            private LogAbandonRunnable() {
            }

            void schedule(int i) {
                if (this.mIsPending) {
                    Log.e(SelectionActionModeHelper.LOG_TAG, "Force flushing abandon due to new scheduling request");
                    flush();
                }
                this.mIsPending = true;
                SelectionTracker.this.mTextView.postDelayed(this, i);
            }

            void flush() {
                SelectionTracker.this.mTextView.removeCallbacks(this);
                run();
            }

            @Override
            public void run() {
                if (this.mIsPending) {
                    SelectionTracker.this.mLogger.logSelectionAction(SelectionTracker.this.mSelectionStart, SelectionTracker.this.mSelectionEnd, 107, null);
                    SelectionTracker.this.mSelectionStart = SelectionTracker.this.mSelectionEnd = -1;
                    SelectionTracker.this.mLogger.endTextClassificationSession();
                    this.mIsPending = false;
                }
            }
        }
    }

    private static final class SelectionMetricsLogger {
        private static final String LOG_TAG = "SelectionMetricsLogger";
        private static final Pattern PATTERN_WHITESPACE = Pattern.compile("\\s+");
        private TextClassifier mClassificationSession;
        private final boolean mEditTextLogger;
        private int mStartIndex;
        private String mText;
        private final BreakIterator mTokenIterator;

        SelectionMetricsLogger(TextView textView) {
            Preconditions.checkNotNull(textView);
            this.mEditTextLogger = textView.isTextEditable();
            this.mTokenIterator = SelectionSessionLogger.getTokenIterator(textView.getTextLocale());
        }

        private static String getWidetType(TextView textView) {
            if (textView.isTextEditable()) {
                return TextClassifier.WIDGET_TYPE_EDITTEXT;
            }
            if (textView.isTextSelectable()) {
                return TextClassifier.WIDGET_TYPE_TEXTVIEW;
            }
            return TextClassifier.WIDGET_TYPE_UNSELECTABLE_TEXTVIEW;
        }

        public void logSelectionStarted(TextClassifier textClassifier, CharSequence charSequence, int i, int i2) {
            try {
                Preconditions.checkNotNull(charSequence);
                Preconditions.checkArgumentInRange(i, 0, charSequence.length(), "index");
                if (this.mText == null || !this.mText.contentEquals(charSequence)) {
                    this.mText = charSequence.toString();
                }
                this.mTokenIterator.setText(this.mText);
                this.mStartIndex = i;
                this.mClassificationSession = textClassifier;
                if (hasActiveClassificationSession()) {
                    this.mClassificationSession.onSelectionEvent(SelectionEvent.createSelectionStartedEvent(i2, 0));
                }
            } catch (Exception e) {
                Log.e(LOG_TAG, "" + e.getMessage(), e);
            }
        }

        public void logSelectionModified(int i, int i2, TextClassification textClassification, TextSelection textSelection) {
            try {
                if (hasActiveClassificationSession()) {
                    Preconditions.checkArgumentInRange(i, 0, this.mText.length(), Telephony.BaseMmsColumns.START);
                    Preconditions.checkArgumentInRange(i2, i, this.mText.length(), "end");
                    int[] wordDelta = getWordDelta(i, i2);
                    if (textSelection != null) {
                        this.mClassificationSession.onSelectionEvent(SelectionEvent.createSelectionModifiedEvent(wordDelta[0], wordDelta[1], textSelection));
                    } else if (textClassification != null) {
                        this.mClassificationSession.onSelectionEvent(SelectionEvent.createSelectionModifiedEvent(wordDelta[0], wordDelta[1], textClassification));
                    } else {
                        this.mClassificationSession.onSelectionEvent(SelectionEvent.createSelectionModifiedEvent(wordDelta[0], wordDelta[1]));
                    }
                }
            } catch (Exception e) {
                Log.e(LOG_TAG, "" + e.getMessage(), e);
            }
        }

        public void logSelectionAction(int i, int i2, int i3, TextClassification textClassification) {
            try {
                if (hasActiveClassificationSession()) {
                    Preconditions.checkArgumentInRange(i, 0, this.mText.length(), Telephony.BaseMmsColumns.START);
                    Preconditions.checkArgumentInRange(i2, i, this.mText.length(), "end");
                    int[] wordDelta = getWordDelta(i, i2);
                    if (textClassification != null) {
                        this.mClassificationSession.onSelectionEvent(SelectionEvent.createSelectionActionEvent(wordDelta[0], wordDelta[1], i3, textClassification));
                    } else {
                        this.mClassificationSession.onSelectionEvent(SelectionEvent.createSelectionActionEvent(wordDelta[0], wordDelta[1], i3));
                    }
                    if (SelectionEvent.isTerminal(i3)) {
                        endTextClassificationSession();
                    }
                }
            } catch (Exception e) {
                Log.e(LOG_TAG, "" + e.getMessage(), e);
            }
        }

        public boolean isEditTextLogger() {
            return this.mEditTextLogger;
        }

        public void endTextClassificationSession() {
            if (hasActiveClassificationSession()) {
                this.mClassificationSession.destroy();
            }
        }

        private boolean hasActiveClassificationSession() {
            return (this.mClassificationSession == null || this.mClassificationSession.isDestroyed()) ? false : true;
        }

        private int[] getWordDelta(int i, int i2) {
            int[] iArr = new int[2];
            if (i == this.mStartIndex) {
                iArr[0] = 0;
            } else if (i < this.mStartIndex) {
                iArr[0] = -countWordsForward(i);
            } else {
                iArr[0] = countWordsBackward(i);
                if (!this.mTokenIterator.isBoundary(i) && !isWhitespace(this.mTokenIterator.preceding(i), this.mTokenIterator.following(i))) {
                    iArr[0] = iArr[0] - 1;
                }
            }
            if (i2 == this.mStartIndex) {
                iArr[1] = 0;
            } else if (i2 < this.mStartIndex) {
                iArr[1] = -countWordsForward(i2);
            } else {
                iArr[1] = countWordsBackward(i2);
            }
            return iArr;
        }

        private int countWordsBackward(int i) {
            int i2 = 0;
            Preconditions.checkArgument(i >= this.mStartIndex);
            while (i > this.mStartIndex) {
                int iPreceding = this.mTokenIterator.preceding(i);
                if (!isWhitespace(iPreceding, i)) {
                    i2++;
                }
                i = iPreceding;
            }
            return i2;
        }

        private int countWordsForward(int i) {
            int i2 = 0;
            Preconditions.checkArgument(i <= this.mStartIndex);
            while (i < this.mStartIndex) {
                int iFollowing = this.mTokenIterator.following(i);
                if (!isWhitespace(i, iFollowing)) {
                    i2++;
                }
                i = iFollowing;
            }
            return i2;
        }

        private boolean isWhitespace(int i, int i2) {
            return PATTERN_WHITESPACE.matcher(this.mText.substring(i, i2)).matches();
        }
    }

    private static final class TextClassificationAsyncTask extends AsyncTask<Void, Void, SelectionResult> {
        private final String mOriginalText;
        private final Consumer<SelectionResult> mSelectionResultCallback;
        private final Supplier<SelectionResult> mSelectionResultSupplier;
        private final TextView mTextView;
        private final int mTimeOutDuration;
        private final Supplier<SelectionResult> mTimeOutResultSupplier;

        TextClassificationAsyncTask(TextView textView, int i, Supplier<SelectionResult> supplier, Consumer<SelectionResult> consumer, Supplier<SelectionResult> supplier2) {
            super(textView != null ? textView.getHandler() : null);
            this.mTextView = (TextView) Preconditions.checkNotNull(textView);
            this.mTimeOutDuration = i;
            this.mSelectionResultSupplier = (Supplier) Preconditions.checkNotNull(supplier);
            this.mSelectionResultCallback = (Consumer) Preconditions.checkNotNull(consumer);
            this.mTimeOutResultSupplier = (Supplier) Preconditions.checkNotNull(supplier2);
            this.mOriginalText = SelectionActionModeHelper.getText(this.mTextView).toString();
        }

        @Override
        protected SelectionResult doInBackground(Void... voidArr) {
            Runnable runnable = new Runnable() {
                @Override
                public final void run() {
                    this.f$0.onTimeOut();
                }
            };
            this.mTextView.postDelayed(runnable, this.mTimeOutDuration);
            SelectionResult selectionResult = this.mSelectionResultSupplier.get();
            this.mTextView.removeCallbacks(runnable);
            return selectionResult;
        }

        @Override
        protected void onPostExecute(SelectionResult selectionResult) {
            if (!TextUtils.equals(this.mOriginalText, SelectionActionModeHelper.getText(this.mTextView))) {
                selectionResult = null;
            }
            this.mSelectionResultCallback.accept(selectionResult);
        }

        private void onTimeOut() {
            if (getStatus() == AsyncTask.Status.RUNNING) {
                onPostExecute(this.mTimeOutResultSupplier.get());
            }
            cancel(true);
        }
    }

    private static final class TextClassificationHelper {
        private static final int TRIM_DELTA = 120;
        private final Context mContext;
        private LocaleList mDefaultLocales;
        private boolean mHot;
        private LocaleList mLastClassificationLocales;
        private SelectionResult mLastClassificationResult;
        private int mLastClassificationSelectionEnd;
        private int mLastClassificationSelectionStart;
        private CharSequence mLastClassificationText;
        private int mRelativeEnd;
        private int mRelativeStart;
        private int mSelectionEnd;
        private int mSelectionStart;
        private String mText;
        private Supplier<TextClassifier> mTextClassifier;
        private int mTrimStart;
        private CharSequence mTrimmedText;

        TextClassificationHelper(Context context, Supplier<TextClassifier> supplier, CharSequence charSequence, int i, int i2, LocaleList localeList) {
            init(supplier, charSequence, i, i2, localeList);
            this.mContext = (Context) Preconditions.checkNotNull(context);
        }

        public void init(Supplier<TextClassifier> supplier, CharSequence charSequence, int i, int i2, LocaleList localeList) {
            this.mTextClassifier = (Supplier) Preconditions.checkNotNull(supplier);
            this.mText = ((CharSequence) Preconditions.checkNotNull(charSequence)).toString();
            this.mLastClassificationText = null;
            Preconditions.checkArgument(i2 > i);
            this.mSelectionStart = i;
            this.mSelectionEnd = i2;
            this.mDefaultLocales = localeList;
        }

        public SelectionResult classifyText() {
            this.mHot = true;
            return performClassification(null);
        }

        public SelectionResult suggestSelection() {
            TextSelection textSelectionSuggestSelection;
            this.mHot = true;
            trimText();
            if (this.mContext.getApplicationInfo().targetSdkVersion >= 28) {
                textSelectionSuggestSelection = this.mTextClassifier.get().suggestSelection(new TextSelection.Request.Builder(this.mTrimmedText, this.mRelativeStart, this.mRelativeEnd).setDefaultLocales(this.mDefaultLocales).setDarkLaunchAllowed(true).build());
            } else {
                textSelectionSuggestSelection = this.mTextClassifier.get().suggestSelection(this.mTrimmedText, this.mRelativeStart, this.mRelativeEnd, this.mDefaultLocales);
            }
            if (!isDarkLaunchEnabled()) {
                this.mSelectionStart = Math.max(0, textSelectionSuggestSelection.getSelectionStartIndex() + this.mTrimStart);
                this.mSelectionEnd = Math.min(this.mText.length(), textSelectionSuggestSelection.getSelectionEndIndex() + this.mTrimStart);
            }
            return performClassification(textSelectionSuggestSelection);
        }

        public SelectionResult getOriginalSelection() {
            return new SelectionResult(this.mSelectionStart, this.mSelectionEnd, null, null);
        }

        public int getTimeoutDuration() {
            if (this.mHot) {
                return 200;
            }
            return 500;
        }

        private boolean isDarkLaunchEnabled() {
            return TextClassificationManager.getSettings(this.mContext).isModelDarkLaunchEnabled();
        }

        private SelectionResult performClassification(TextSelection textSelection) {
            TextClassification textClassificationClassifyText;
            if (!Objects.equals(this.mText, this.mLastClassificationText) || this.mSelectionStart != this.mLastClassificationSelectionStart || this.mSelectionEnd != this.mLastClassificationSelectionEnd || !Objects.equals(this.mDefaultLocales, this.mLastClassificationLocales)) {
                this.mLastClassificationText = this.mText;
                this.mLastClassificationSelectionStart = this.mSelectionStart;
                this.mLastClassificationSelectionEnd = this.mSelectionEnd;
                this.mLastClassificationLocales = this.mDefaultLocales;
                trimText();
                if (Linkify.containsUnsupportedCharacters(this.mText)) {
                    EventLog.writeEvent(1397638484, "116321860", -1, "");
                    textClassificationClassifyText = TextClassification.EMPTY;
                } else if (this.mContext.getApplicationInfo().targetSdkVersion >= 28) {
                    textClassificationClassifyText = this.mTextClassifier.get().classifyText(new TextClassification.Request.Builder(this.mTrimmedText, this.mRelativeStart, this.mRelativeEnd).setDefaultLocales(this.mDefaultLocales).build());
                } else {
                    textClassificationClassifyText = this.mTextClassifier.get().classifyText(this.mTrimmedText, this.mRelativeStart, this.mRelativeEnd, this.mDefaultLocales);
                }
                this.mLastClassificationResult = new SelectionResult(this.mSelectionStart, this.mSelectionEnd, textClassificationClassifyText, textSelection);
            }
            return this.mLastClassificationResult;
        }

        private void trimText() {
            this.mTrimStart = Math.max(0, this.mSelectionStart - 120);
            this.mTrimmedText = this.mText.subSequence(this.mTrimStart, Math.min(this.mText.length(), this.mSelectionEnd + 120));
            this.mRelativeStart = this.mSelectionStart - this.mTrimStart;
            this.mRelativeEnd = this.mSelectionEnd - this.mTrimStart;
        }
    }

    private static final class SelectionResult {
        private final TextClassification mClassification;
        private final int mEnd;
        private final TextSelection mSelection;
        private final int mStart;

        SelectionResult(int i, int i2, TextClassification textClassification, TextSelection textSelection) {
            this.mStart = i;
            this.mEnd = i2;
            this.mClassification = textClassification;
            this.mSelection = textSelection;
        }
    }

    private static int getActionType(int i) {
        if (i == 16908337) {
            return 102;
        }
        if (i == 16908341) {
            return 104;
        }
        if (i != 16908353) {
            switch (i) {
                case 16908319:
                    return 200;
                case 16908320:
                    return 103;
                case 16908321:
                    return 101;
                case 16908322:
                    return 102;
                default:
                    return 108;
            }
        }
        return 105;
    }

    private static CharSequence getText(TextView textView) {
        CharSequence text = textView.getText();
        if (text != null) {
            return text;
        }
        return "";
    }
}
