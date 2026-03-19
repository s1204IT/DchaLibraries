package android.widget;

import android.content.Context;
import android.text.Editable;
import android.text.Selection;
import android.text.TextUtils;
import android.text.method.WordIterator;
import android.text.style.SpellCheckSpan;
import android.text.style.SuggestionSpan;
import android.util.Log;
import android.util.LruCache;
import android.view.textservice.SentenceSuggestionsInfo;
import android.view.textservice.SpellCheckerSession;
import android.view.textservice.SuggestionsInfo;
import android.view.textservice.TextInfo;
import android.view.textservice.TextServicesManager;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.GrowingArrayUtils;
import java.util.Locale;

public class SpellChecker implements SpellCheckerSession.SpellCheckerSessionListener {
    public static final int AVERAGE_WORD_LENGTH = 7;
    private static final boolean DBG = false;
    public static final int MAX_NUMBER_OF_WORDS = 50;
    private static final int MIN_SENTENCE_LENGTH = 50;
    private static final int SPELL_PAUSE_DURATION = 400;
    private static final int SUGGESTION_SPAN_CACHE_SIZE = 10;
    private static final String TAG = SpellChecker.class.getSimpleName();
    private static final int USE_SPAN_RANGE = -1;
    public static final int WORD_ITERATOR_INTERVAL = 350;
    final int mCookie;
    private Locale mCurrentLocale;
    private boolean mIsSentenceSpellCheckSupported;
    private int mLength;
    SpellCheckerSession mSpellCheckerSession;
    private Runnable mSpellRunnable;
    private TextServicesManager mTextServicesManager;
    private final TextView mTextView;
    private WordIterator mWordIterator;
    private SpellParser[] mSpellParsers = new SpellParser[0];
    private int mSpanSequenceCounter = 0;
    private final LruCache<Long, SuggestionSpan> mSuggestionSpanCache = new LruCache<>(10);
    private int[] mIds = ArrayUtils.newUnpaddedIntArray(1);
    private SpellCheckSpan[] mSpellCheckSpans = new SpellCheckSpan[this.mIds.length];

    public SpellChecker(TextView textView) {
        this.mTextView = textView;
        setLocale(this.mTextView.getSpellCheckerLocale());
        this.mCookie = hashCode();
    }

    private void resetSession() {
        closeSession();
        this.mTextServicesManager = (TextServicesManager) this.mTextView.getContext().getSystemService(Context.TEXT_SERVICES_MANAGER_SERVICE);
        if (!this.mTextServicesManager.isSpellCheckerEnabled() || this.mCurrentLocale == null || this.mTextServicesManager.getCurrentSpellCheckerSubtype(true) == null) {
            this.mSpellCheckerSession = null;
        } else {
            this.mSpellCheckerSession = this.mTextServicesManager.newSpellCheckerSession(null, this.mCurrentLocale, this, false);
            this.mIsSentenceSpellCheckSupported = true;
        }
        for (int i = 0; i < this.mLength; i++) {
            this.mIds[i] = -1;
        }
        this.mLength = 0;
        this.mTextView.removeMisspelledSpans((Editable) this.mTextView.getText());
        this.mSuggestionSpanCache.evictAll();
    }

    private void setLocale(Locale locale) {
        this.mCurrentLocale = locale;
        resetSession();
        if (locale != null) {
            this.mWordIterator = new WordIterator(locale);
        }
        this.mTextView.onLocaleChanged();
    }

    private boolean isSessionActive() {
        return this.mSpellCheckerSession != null;
    }

    public void closeSession() {
        if (this.mSpellCheckerSession != null) {
            this.mSpellCheckerSession.close();
        }
        int length = this.mSpellParsers.length;
        for (int i = 0; i < length; i++) {
            this.mSpellParsers[i].stop();
        }
        if (this.mSpellRunnable != null) {
            this.mTextView.removeCallbacks(this.mSpellRunnable);
        }
    }

    private int nextSpellCheckSpanIndex() {
        for (int i = 0; i < this.mLength; i++) {
            if (this.mIds[i] < 0) {
                return i;
            }
        }
        this.mIds = GrowingArrayUtils.append(this.mIds, this.mLength, 0);
        this.mSpellCheckSpans = (SpellCheckSpan[]) GrowingArrayUtils.append(this.mSpellCheckSpans, this.mLength, new SpellCheckSpan());
        this.mLength++;
        return this.mLength - 1;
    }

    private void addSpellCheckSpan(Editable editable, int i, int i2) {
        int iNextSpellCheckSpanIndex = nextSpellCheckSpanIndex();
        SpellCheckSpan spellCheckSpan = this.mSpellCheckSpans[iNextSpellCheckSpanIndex];
        editable.setSpan(spellCheckSpan, i, i2, 33);
        spellCheckSpan.setSpellCheckInProgress(false);
        int[] iArr = this.mIds;
        int i3 = this.mSpanSequenceCounter;
        this.mSpanSequenceCounter = i3 + 1;
        iArr[iNextSpellCheckSpanIndex] = i3;
    }

    public void onSpellCheckSpanRemoved(SpellCheckSpan spellCheckSpan) {
        for (int i = 0; i < this.mLength; i++) {
            if (this.mSpellCheckSpans[i] == spellCheckSpan) {
                this.mIds[i] = -1;
                return;
            }
        }
    }

    public void onSelectionChanged() {
        spellCheck();
    }

    public void spellCheck(int i, int i2) {
        Locale spellCheckerLocale = this.mTextView.getSpellCheckerLocale();
        boolean zIsSessionActive = isSessionActive();
        if (spellCheckerLocale == null || this.mCurrentLocale == null || !this.mCurrentLocale.equals(spellCheckerLocale)) {
            setLocale(spellCheckerLocale);
            i2 = this.mTextView.getText().length();
            i = 0;
        } else if (zIsSessionActive != this.mTextServicesManager.isSpellCheckerEnabled()) {
            resetSession();
        }
        if (zIsSessionActive) {
            int length = this.mSpellParsers.length;
            for (int i3 = 0; i3 < length; i3++) {
                SpellParser spellParser = this.mSpellParsers[i3];
                if (spellParser.isFinished()) {
                    spellParser.parse(i, i2);
                    return;
                }
            }
            SpellParser[] spellParserArr = new SpellParser[length + 1];
            System.arraycopy(this.mSpellParsers, 0, spellParserArr, 0, length);
            this.mSpellParsers = spellParserArr;
            SpellParser spellParser2 = new SpellParser();
            this.mSpellParsers[length] = spellParser2;
            spellParser2.parse(i, i2);
        }
    }

    private void spellCheck() {
        TextInfo[] textInfoArr;
        if (this.mSpellCheckerSession == null) {
            return;
        }
        Editable editable = (Editable) this.mTextView.getText();
        int selectionStart = Selection.getSelectionStart(editable);
        int selectionEnd = Selection.getSelectionEnd(editable);
        TextInfo[] textInfoArr2 = new TextInfo[this.mLength];
        int i = 0;
        for (int i2 = 0; i2 < this.mLength; i2++) {
            SpellCheckSpan spellCheckSpan = this.mSpellCheckSpans[i2];
            if (this.mIds[i2] >= 0 && !spellCheckSpan.isSpellCheckInProgress()) {
                int spanStart = editable.getSpanStart(spellCheckSpan);
                int spanEnd = editable.getSpanEnd(spellCheckSpan);
                int i3 = spanEnd + 1;
                boolean z = !(selectionStart == i3 && WordIterator.isMidWordPunctuation(this.mCurrentLocale, Character.codePointBefore(editable, i3))) && (!this.mIsSentenceSpellCheckSupported ? !(selectionEnd < spanStart || selectionStart > spanEnd) : !(selectionEnd <= spanStart || selectionStart > spanEnd));
                if (spanStart >= 0 && spanEnd > spanStart && z) {
                    spellCheckSpan.setSpellCheckInProgress(true);
                    textInfoArr2[i] = new TextInfo(editable, spanStart, spanEnd, this.mCookie, this.mIds[i2]);
                    i++;
                }
            }
        }
        if (i > 0) {
            if (i < textInfoArr2.length) {
                textInfoArr = new TextInfo[i];
                System.arraycopy(textInfoArr2, 0, textInfoArr, 0, i);
            } else {
                textInfoArr = textInfoArr2;
            }
            if (!this.mIsSentenceSpellCheckSupported) {
                this.mSpellCheckerSession.getSuggestions(textInfoArr, 5, false);
            } else {
                this.mSpellCheckerSession.getSentenceSuggestions(textInfoArr, 5);
            }
        }
    }

    private SpellCheckSpan onGetSuggestionsInternal(SuggestionsInfo suggestionsInfo, int i, int i2) {
        int i3;
        int i4;
        if (suggestionsInfo == null || suggestionsInfo.getCookie() != this.mCookie) {
            return null;
        }
        Editable editable = (Editable) this.mTextView.getText();
        int sequence = suggestionsInfo.getSequence();
        boolean z = false;
        for (int i5 = 0; i5 < this.mLength; i5++) {
            if (sequence == this.mIds[i5]) {
                int suggestionsAttributes = suggestionsInfo.getSuggestionsAttributes();
                boolean z2 = (suggestionsAttributes & 1) > 0;
                if ((suggestionsAttributes & 2) > 0) {
                    z = true;
                }
                SpellCheckSpan spellCheckSpan = this.mSpellCheckSpans[i5];
                if (!z2 && z) {
                    createMisspelledSuggestionSpan(editable, suggestionsInfo, spellCheckSpan, i, i2);
                } else if (this.mIsSentenceSpellCheckSupported) {
                    int spanStart = editable.getSpanStart(spellCheckSpan);
                    int spanEnd = editable.getSpanEnd(spellCheckSpan);
                    if (i != -1 && i2 != -1) {
                        i3 = i + spanStart;
                        i4 = i2 + i3;
                    } else {
                        i3 = spanStart;
                        i4 = spanEnd;
                    }
                    if (spanStart >= 0 && spanEnd > spanStart && i4 > i3) {
                        Long lValueOf = Long.valueOf(TextUtils.packRangeInLong(i3, i4));
                        Object obj = (SuggestionSpan) this.mSuggestionSpanCache.get(lValueOf);
                        if (obj != null) {
                            editable.removeSpan(obj);
                            this.mSuggestionSpanCache.remove(lValueOf);
                        }
                    }
                }
                return spellCheckSpan;
            }
        }
        return null;
    }

    @Override
    public void onGetSuggestions(SuggestionsInfo[] suggestionsInfoArr) {
        Editable editable = (Editable) this.mTextView.getText();
        for (SuggestionsInfo suggestionsInfo : suggestionsInfoArr) {
            SpellCheckSpan spellCheckSpanOnGetSuggestionsInternal = onGetSuggestionsInternal(suggestionsInfo, -1, -1);
            if (spellCheckSpanOnGetSuggestionsInternal != null) {
                editable.removeSpan(spellCheckSpanOnGetSuggestionsInternal);
            }
        }
        scheduleNewSpellCheck();
    }

    @Override
    public void onGetSentenceSuggestions(SentenceSuggestionsInfo[] sentenceSuggestionsInfoArr) {
        Editable editable = (Editable) this.mTextView.getText();
        for (SentenceSuggestionsInfo sentenceSuggestionsInfo : sentenceSuggestionsInfoArr) {
            if (sentenceSuggestionsInfo != null) {
                SpellCheckSpan spellCheckSpan = null;
                for (int i = 0; i < sentenceSuggestionsInfo.getSuggestionsCount(); i++) {
                    SuggestionsInfo suggestionsInfoAt = sentenceSuggestionsInfo.getSuggestionsInfoAt(i);
                    if (suggestionsInfoAt != null) {
                        SpellCheckSpan spellCheckSpanOnGetSuggestionsInternal = onGetSuggestionsInternal(suggestionsInfoAt, sentenceSuggestionsInfo.getOffsetAt(i), sentenceSuggestionsInfo.getLengthAt(i));
                        if (spellCheckSpan == null && spellCheckSpanOnGetSuggestionsInternal != null) {
                            spellCheckSpan = spellCheckSpanOnGetSuggestionsInternal;
                        }
                    }
                }
                if (spellCheckSpan != null) {
                    editable.removeSpan(spellCheckSpan);
                }
            }
        }
        scheduleNewSpellCheck();
    }

    private void scheduleNewSpellCheck() {
        if (this.mSpellRunnable == null) {
            this.mSpellRunnable = new Runnable() {
                @Override
                public void run() {
                    int length = SpellChecker.this.mSpellParsers.length;
                    for (int i = 0; i < length; i++) {
                        SpellParser spellParser = SpellChecker.this.mSpellParsers[i];
                        if (!spellParser.isFinished()) {
                            spellParser.parse();
                            return;
                        }
                    }
                }
            };
        } else {
            this.mTextView.removeCallbacks(this.mSpellRunnable);
        }
        this.mTextView.postDelayed(this.mSpellRunnable, 400L);
    }

    private void createMisspelledSuggestionSpan(Editable editable, SuggestionsInfo suggestionsInfo, SpellCheckSpan spellCheckSpan, int i, int i2) {
        String[] strArr;
        int spanStart = editable.getSpanStart(spellCheckSpan);
        int spanEnd = editable.getSpanEnd(spellCheckSpan);
        if (spanStart < 0 || spanEnd <= spanStart) {
            return;
        }
        if (i != -1 && i2 != -1) {
            spanStart += i;
            spanEnd = spanStart + i2;
        }
        int suggestionsCount = suggestionsInfo.getSuggestionsCount();
        if (suggestionsCount > 0) {
            strArr = new String[suggestionsCount];
            for (int i3 = 0; i3 < suggestionsCount; i3++) {
                strArr[i3] = suggestionsInfo.getSuggestionAt(i3);
            }
        } else {
            strArr = (String[]) ArrayUtils.emptyArray(String.class);
        }
        SuggestionSpan suggestionSpan = new SuggestionSpan(this.mTextView.getContext(), strArr, 3);
        if (this.mIsSentenceSpellCheckSupported) {
            Long lValueOf = Long.valueOf(TextUtils.packRangeInLong(spanStart, spanEnd));
            SuggestionSpan suggestionSpan2 = this.mSuggestionSpanCache.get(lValueOf);
            if (suggestionSpan2 != null) {
                editable.removeSpan(suggestionSpan2);
            }
            this.mSuggestionSpanCache.put(lValueOf, suggestionSpan);
        }
        editable.setSpan(suggestionSpan, spanStart, spanEnd, 33);
        this.mTextView.invalidateRegion(spanStart, spanEnd, false);
    }

    private class SpellParser {
        private Object mRange;

        private SpellParser() {
            this.mRange = new Object();
        }

        public void parse(int i, int i2) {
            int length = SpellChecker.this.mTextView.length();
            if (i2 > length) {
                Log.w(SpellChecker.TAG, "Parse invalid region, from " + i + " to " + i2);
                i2 = length;
            }
            if (i2 > i) {
                setRangeSpan((Editable) SpellChecker.this.mTextView.getText(), i, i2);
                parse();
            }
        }

        public boolean isFinished() {
            return ((Editable) SpellChecker.this.mTextView.getText()).getSpanStart(this.mRange) < 0;
        }

        public void stop() {
            removeRangeSpan((Editable) SpellChecker.this.mTextView.getText());
        }

        private void setRangeSpan(Editable editable, int i, int i2) {
            editable.setSpan(this.mRange, i, i2, 33);
        }

        private void removeRangeSpan(Editable editable) {
            editable.removeSpan(this.mRange);
        }

        public void parse() {
            int spanStart;
            int end;
            int beginning;
            int iFollowing;
            boolean z;
            Editable editable = (Editable) SpellChecker.this.mTextView.getText();
            int i = 50;
            boolean z2 = false;
            if (SpellChecker.this.mIsSentenceSpellCheckSupported) {
                spanStart = Math.max(0, editable.getSpanStart(this.mRange) - 50);
            } else {
                spanStart = editable.getSpanStart(this.mRange);
            }
            int spanEnd = editable.getSpanEnd(this.mRange);
            int iMin = Math.min(spanEnd, spanStart + 350);
            SpellChecker.this.mWordIterator.setCharSequence(editable, spanStart, iMin);
            int iPreceding = SpellChecker.this.mWordIterator.preceding(spanStart);
            if (iPreceding == -1) {
                end = SpellChecker.this.mWordIterator.following(spanStart);
                if (end != -1) {
                    iPreceding = SpellChecker.this.mWordIterator.getBeginning(end);
                }
            } else {
                end = SpellChecker.this.mWordIterator.getEnd(iPreceding);
            }
            if (end == -1) {
                removeRangeSpan(editable);
                return;
            }
            int i2 = spanStart - 1;
            int i3 = spanEnd + 1;
            SpellCheckSpan[] spellCheckSpanArr = (SpellCheckSpan[]) editable.getSpans(i2, i3, SpellCheckSpan.class);
            SuggestionSpan[] suggestionSpanArr = (SuggestionSpan[]) editable.getSpans(i2, i3, SuggestionSpan.class);
            boolean z3 = true;
            if (SpellChecker.this.mIsSentenceSpellCheckSupported) {
                boolean z4 = iMin < spanEnd;
                int iPreceding2 = SpellChecker.this.mWordIterator.preceding(iMin);
                boolean z5 = iPreceding2 != -1;
                if (z5) {
                    iPreceding2 = SpellChecker.this.mWordIterator.getEnd(iPreceding2);
                    z5 = iPreceding2 != -1;
                }
                if (!z5) {
                    removeRangeSpan(editable);
                    return;
                }
                beginning = iPreceding2;
                int i4 = 0;
                while (true) {
                    if (i4 >= SpellChecker.this.mLength) {
                        break;
                    }
                    SpellCheckSpan spellCheckSpan = SpellChecker.this.mSpellCheckSpans[i4];
                    if (SpellChecker.this.mIds[i4] >= 0 && !spellCheckSpan.isSpellCheckInProgress()) {
                        int spanStart2 = editable.getSpanStart(spellCheckSpan);
                        int spanEnd2 = editable.getSpanEnd(spellCheckSpan);
                        if (spanEnd2 >= iPreceding && beginning >= spanStart2) {
                            if (spanStart2 > iPreceding || beginning > spanEnd2) {
                                editable.removeSpan(spellCheckSpan);
                                iPreceding = Math.min(spanStart2, iPreceding);
                                beginning = Math.max(spanEnd2, beginning);
                            } else {
                                z3 = false;
                                break;
                            }
                        }
                    }
                    i4++;
                }
                if (beginning >= spanStart) {
                    if (beginning <= iPreceding) {
                        Log.w(SpellChecker.TAG, "Trying to spellcheck invalid region, from " + spanStart + " to " + spanEnd);
                    } else if (z3) {
                        SpellChecker.this.addSpellCheckSpan(editable, iPreceding, beginning);
                    }
                }
                z2 = z4;
            } else {
                int i5 = iMin;
                int iFollowing2 = end;
                beginning = iPreceding;
                int i6 = 0;
                while (true) {
                    if (beginning > spanEnd) {
                        break;
                    }
                    if (iFollowing2 < spanStart || iFollowing2 <= beginning) {
                        iFollowing = SpellChecker.this.mWordIterator.following(iFollowing2);
                        if (i5 >= spanEnd || (iFollowing != -1 && iFollowing < i5)) {
                            iFollowing2 = iFollowing;
                        } else {
                            int iMin2 = Math.min(spanEnd, iFollowing2 + 350);
                            SpellChecker.this.mWordIterator.setCharSequence(editable, iFollowing2, iMin2);
                            iFollowing2 = SpellChecker.this.mWordIterator.following(iFollowing2);
                            i5 = iMin2;
                        }
                        if (iFollowing2 != -1 || (beginning = SpellChecker.this.mWordIterator.getBeginning(iFollowing2)) == -1) {
                            break;
                        } else {
                            i = 50;
                        }
                    } else if (i6 < i) {
                        if (beginning < spanStart && iFollowing2 > spanStart) {
                            removeSpansAt(editable, spanStart, spellCheckSpanArr);
                            removeSpansAt(editable, spanStart, suggestionSpanArr);
                        }
                        if (beginning < spanEnd && iFollowing2 > spanEnd) {
                            removeSpansAt(editable, spanEnd, spellCheckSpanArr);
                            removeSpansAt(editable, spanEnd, suggestionSpanArr);
                        }
                        if (iFollowing2 == spanStart) {
                            for (SpellCheckSpan spellCheckSpan2 : spellCheckSpanArr) {
                                if (editable.getSpanEnd(spellCheckSpan2) == spanStart) {
                                    z = false;
                                    break;
                                }
                            }
                            z = true;
                            if (beginning == spanEnd) {
                                int i7 = 0;
                                while (true) {
                                    if (i7 >= spellCheckSpanArr.length) {
                                        break;
                                    }
                                    if (editable.getSpanStart(spellCheckSpanArr[i7]) != spanEnd) {
                                        i7++;
                                    } else {
                                        z = false;
                                        break;
                                    }
                                }
                            }
                            if (z) {
                                SpellChecker.this.addSpellCheckSpan(editable, beginning, iFollowing2);
                            }
                            i6++;
                            iFollowing = SpellChecker.this.mWordIterator.following(iFollowing2);
                            if (i5 >= spanEnd) {
                                iFollowing2 = iFollowing;
                                if (iFollowing2 != -1) {
                                    break;
                                } else {
                                    i = 50;
                                }
                            }
                        } else {
                            z = true;
                            if (beginning == spanEnd) {
                            }
                            if (z) {
                            }
                            i6++;
                            iFollowing = SpellChecker.this.mWordIterator.following(iFollowing2);
                            if (i5 >= spanEnd) {
                            }
                        }
                    } else {
                        z2 = true;
                        break;
                    }
                }
            }
            if (z2 && beginning != -1 && beginning <= spanEnd) {
                setRangeSpan(editable, beginning, spanEnd);
            } else {
                removeRangeSpan(editable);
            }
            SpellChecker.this.spellCheck();
        }

        private <T> void removeSpansAt(Editable editable, int i, T[] tArr) {
            for (T t : tArr) {
                if (editable.getSpanStart(t) <= i && editable.getSpanEnd(t) >= i) {
                    editable.removeSpan(t);
                }
            }
        }
    }

    public static boolean haveWordBoundariesChanged(Editable editable, int i, int i2, int i3, int i4) {
        if (i4 != i && i3 != i2) {
            return true;
        }
        if (i4 == i && i < editable.length()) {
            return Character.isLetterOrDigit(Character.codePointAt(editable, i));
        }
        if (i3 == i2 && i2 > 0) {
            return Character.isLetterOrDigit(Character.codePointBefore(editable, i2));
        }
        return false;
    }
}
