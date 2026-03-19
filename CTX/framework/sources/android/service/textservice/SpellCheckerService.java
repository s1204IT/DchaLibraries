package android.service.textservice;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Process;
import android.os.RemoteException;
import android.text.TextUtils;
import android.text.method.WordIterator;
import android.view.textservice.SentenceSuggestionsInfo;
import android.view.textservice.SuggestionsInfo;
import android.view.textservice.TextInfo;
import com.android.internal.textservice.ISpellCheckerService;
import com.android.internal.textservice.ISpellCheckerServiceCallback;
import com.android.internal.textservice.ISpellCheckerSession;
import com.android.internal.textservice.ISpellCheckerSessionListener;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Locale;

public abstract class SpellCheckerService extends Service {
    private static final boolean DBG = false;
    public static final String SERVICE_INTERFACE = "android.service.textservice.SpellCheckerService";
    private static final String TAG = SpellCheckerService.class.getSimpleName();
    private final SpellCheckerServiceBinder mBinder = new SpellCheckerServiceBinder(this);

    public abstract Session createSession();

    @Override
    public final IBinder onBind(Intent intent) {
        return this.mBinder;
    }

    public static abstract class Session {
        private InternalISpellCheckerSession mInternalSession;
        private volatile SentenceLevelAdapter mSentenceLevelAdapter;

        public abstract void onCreate();

        public abstract SuggestionsInfo onGetSuggestions(TextInfo textInfo, int i);

        public final void setInternalISpellCheckerSession(InternalISpellCheckerSession internalISpellCheckerSession) {
            this.mInternalSession = internalISpellCheckerSession;
        }

        public SuggestionsInfo[] onGetSuggestionsMultiple(TextInfo[] textInfoArr, int i, boolean z) {
            int length = textInfoArr.length;
            SuggestionsInfo[] suggestionsInfoArr = new SuggestionsInfo[length];
            for (int i2 = 0; i2 < length; i2++) {
                suggestionsInfoArr[i2] = onGetSuggestions(textInfoArr[i2], i);
                suggestionsInfoArr[i2].setCookieAndSequence(textInfoArr[i2].getCookie(), textInfoArr[i2].getSequence());
            }
            return suggestionsInfoArr;
        }

        public SentenceSuggestionsInfo[] onGetSentenceSuggestionsMultiple(TextInfo[] textInfoArr, int i) {
            if (textInfoArr == null || textInfoArr.length == 0) {
                return SentenceLevelAdapter.EMPTY_SENTENCE_SUGGESTIONS_INFOS;
            }
            if (this.mSentenceLevelAdapter == null) {
                synchronized (this) {
                    if (this.mSentenceLevelAdapter == null) {
                        String locale = getLocale();
                        if (!TextUtils.isEmpty(locale)) {
                            this.mSentenceLevelAdapter = new SentenceLevelAdapter(new Locale(locale));
                        }
                    }
                }
            }
            if (this.mSentenceLevelAdapter == null) {
                return SentenceLevelAdapter.EMPTY_SENTENCE_SUGGESTIONS_INFOS;
            }
            int length = textInfoArr.length;
            SentenceSuggestionsInfo[] sentenceSuggestionsInfoArr = new SentenceSuggestionsInfo[length];
            for (int i2 = 0; i2 < length; i2++) {
                SentenceLevelAdapter.SentenceTextInfoParams splitWords = this.mSentenceLevelAdapter.getSplitWords(textInfoArr[i2]);
                ArrayList<SentenceLevelAdapter.SentenceWordItem> arrayList = splitWords.mItems;
                int size = arrayList.size();
                TextInfo[] textInfoArr2 = new TextInfo[size];
                for (int i3 = 0; i3 < size; i3++) {
                    textInfoArr2[i3] = arrayList.get(i3).mTextInfo;
                }
                sentenceSuggestionsInfoArr[i2] = SentenceLevelAdapter.reconstructSuggestions(splitWords, onGetSuggestionsMultiple(textInfoArr2, i, true));
            }
            return sentenceSuggestionsInfoArr;
        }

        public void onCancel() {
        }

        public void onClose() {
        }

        public String getLocale() {
            return this.mInternalSession.getLocale();
        }

        public Bundle getBundle() {
            return this.mInternalSession.getBundle();
        }
    }

    private static class InternalISpellCheckerSession extends ISpellCheckerSession.Stub {
        private final Bundle mBundle;
        private ISpellCheckerSessionListener mListener;
        private final String mLocale;
        private final Session mSession;

        public InternalISpellCheckerSession(String str, ISpellCheckerSessionListener iSpellCheckerSessionListener, Bundle bundle, Session session) {
            this.mListener = iSpellCheckerSessionListener;
            this.mSession = session;
            this.mLocale = str;
            this.mBundle = bundle;
            session.setInternalISpellCheckerSession(this);
        }

        @Override
        public void onGetSuggestionsMultiple(TextInfo[] textInfoArr, int i, boolean z) {
            int threadPriority = Process.getThreadPriority(Process.myTid());
            try {
                Process.setThreadPriority(10);
                this.mListener.onGetSuggestions(this.mSession.onGetSuggestionsMultiple(textInfoArr, i, z));
            } catch (RemoteException e) {
            } catch (Throwable th) {
                Process.setThreadPriority(threadPriority);
                throw th;
            }
            Process.setThreadPriority(threadPriority);
        }

        @Override
        public void onGetSentenceSuggestionsMultiple(TextInfo[] textInfoArr, int i) {
            try {
                this.mListener.onGetSentenceSuggestions(this.mSession.onGetSentenceSuggestionsMultiple(textInfoArr, i));
            } catch (RemoteException e) {
            }
        }

        @Override
        public void onCancel() {
            int threadPriority = Process.getThreadPriority(Process.myTid());
            try {
                Process.setThreadPriority(10);
                this.mSession.onCancel();
            } finally {
                Process.setThreadPriority(threadPriority);
            }
        }

        @Override
        public void onClose() {
            int threadPriority = Process.getThreadPriority(Process.myTid());
            try {
                Process.setThreadPriority(10);
                this.mSession.onClose();
            } finally {
                Process.setThreadPriority(threadPriority);
                this.mListener = null;
            }
        }

        public String getLocale() {
            return this.mLocale;
        }

        public Bundle getBundle() {
            return this.mBundle;
        }
    }

    private static class SpellCheckerServiceBinder extends ISpellCheckerService.Stub {
        private final WeakReference<SpellCheckerService> mInternalServiceRef;

        public SpellCheckerServiceBinder(SpellCheckerService spellCheckerService) {
            this.mInternalServiceRef = new WeakReference<>(spellCheckerService);
        }

        @Override
        public void getISpellCheckerSession(String str, ISpellCheckerSessionListener iSpellCheckerSessionListener, Bundle bundle, ISpellCheckerServiceCallback iSpellCheckerServiceCallback) {
            InternalISpellCheckerSession internalISpellCheckerSession;
            SpellCheckerService spellCheckerService = this.mInternalServiceRef.get();
            if (spellCheckerService == null) {
                internalISpellCheckerSession = null;
            } else {
                Session sessionCreateSession = spellCheckerService.createSession();
                InternalISpellCheckerSession internalISpellCheckerSession2 = new InternalISpellCheckerSession(str, iSpellCheckerSessionListener, bundle, sessionCreateSession);
                sessionCreateSession.onCreate();
                internalISpellCheckerSession = internalISpellCheckerSession2;
            }
            try {
                iSpellCheckerServiceCallback.onSessionCreated(internalISpellCheckerSession);
            } catch (RemoteException e) {
            }
        }
    }

    private static class SentenceLevelAdapter {
        public static final SentenceSuggestionsInfo[] EMPTY_SENTENCE_SUGGESTIONS_INFOS = new SentenceSuggestionsInfo[0];
        private static final SuggestionsInfo EMPTY_SUGGESTIONS_INFO = new SuggestionsInfo(0, null);
        private final WordIterator mWordIterator;

        public static class SentenceWordItem {
            public final int mLength;
            public final int mStart;
            public final TextInfo mTextInfo;

            public SentenceWordItem(TextInfo textInfo, int i, int i2) {
                this.mTextInfo = textInfo;
                this.mStart = i;
                this.mLength = i2 - i;
            }
        }

        public static class SentenceTextInfoParams {
            final ArrayList<SentenceWordItem> mItems;
            final TextInfo mOriginalTextInfo;
            final int mSize;

            public SentenceTextInfoParams(TextInfo textInfo, ArrayList<SentenceWordItem> arrayList) {
                this.mOriginalTextInfo = textInfo;
                this.mItems = arrayList;
                this.mSize = arrayList.size();
            }
        }

        public SentenceLevelAdapter(Locale locale) {
            this.mWordIterator = new WordIterator(locale);
        }

        private SentenceTextInfoParams getSplitWords(TextInfo textInfo) {
            WordIterator wordIterator = this.mWordIterator;
            String text = textInfo.getText();
            int cookie = textInfo.getCookie();
            int length = text.length();
            ArrayList arrayList = new ArrayList();
            wordIterator.setCharSequence(text, 0, text.length());
            int iFollowing = wordIterator.following(0);
            int iFollowing2 = iFollowing;
            int beginning = wordIterator.getBeginning(iFollowing);
            while (beginning <= length && iFollowing2 != -1 && beginning != -1) {
                if (iFollowing2 >= 0 && iFollowing2 > beginning) {
                    CharSequence charSequenceSubSequence = text.subSequence(beginning, iFollowing2);
                    arrayList.add(new SentenceWordItem(new TextInfo(charSequenceSubSequence, 0, charSequenceSubSequence.length(), cookie, charSequenceSubSequence.hashCode()), beginning, iFollowing2));
                }
                iFollowing2 = wordIterator.following(iFollowing2);
                if (iFollowing2 == -1) {
                    break;
                }
                beginning = wordIterator.getBeginning(iFollowing2);
            }
            return new SentenceTextInfoParams(textInfo, arrayList);
        }

        public static SentenceSuggestionsInfo reconstructSuggestions(SentenceTextInfoParams sentenceTextInfoParams, SuggestionsInfo[] suggestionsInfoArr) {
            SuggestionsInfo suggestionsInfo;
            if (suggestionsInfoArr == null || suggestionsInfoArr.length == 0 || sentenceTextInfoParams == null) {
                return null;
            }
            int cookie = sentenceTextInfoParams.mOriginalTextInfo.getCookie();
            int sequence = sentenceTextInfoParams.mOriginalTextInfo.getSequence();
            int i = sentenceTextInfoParams.mSize;
            int[] iArr = new int[i];
            int[] iArr2 = new int[i];
            SuggestionsInfo[] suggestionsInfoArr2 = new SuggestionsInfo[i];
            for (int i2 = 0; i2 < i; i2++) {
                SentenceWordItem sentenceWordItem = sentenceTextInfoParams.mItems.get(i2);
                int i3 = 0;
                while (true) {
                    if (i3 < suggestionsInfoArr.length) {
                        suggestionsInfo = suggestionsInfoArr[i3];
                        if (suggestionsInfo == null || suggestionsInfo.getSequence() != sentenceWordItem.mTextInfo.getSequence()) {
                            i3++;
                        } else {
                            suggestionsInfo.setCookieAndSequence(cookie, sequence);
                            break;
                        }
                    } else {
                        suggestionsInfo = null;
                        break;
                    }
                }
                iArr[i2] = sentenceWordItem.mStart;
                iArr2[i2] = sentenceWordItem.mLength;
                if (suggestionsInfo == null) {
                    suggestionsInfo = EMPTY_SUGGESTIONS_INFO;
                }
                suggestionsInfoArr2[i2] = suggestionsInfo;
            }
            return new SentenceSuggestionsInfo(suggestionsInfoArr2, iArr, iArr2);
        }
    }
}
