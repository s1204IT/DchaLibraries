package android.view.textservice;

import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;
import com.android.internal.textservice.ISpellCheckerSession;
import com.android.internal.textservice.ISpellCheckerSessionListener;
import com.android.internal.textservice.ITextServicesManager;
import com.android.internal.textservice.ITextServicesSessionListener;
import dalvik.system.CloseGuard;
import java.util.LinkedList;
import java.util.Queue;

public class SpellCheckerSession {
    private static final boolean DBG = false;
    private static final int MSG_ON_GET_SUGGESTION_MULTIPLE = 1;
    private static final int MSG_ON_GET_SUGGESTION_MULTIPLE_FOR_SENTENCE = 2;
    public static final String SERVICE_META_DATA = "android.view.textservice.scs";
    private static final String TAG = SpellCheckerSession.class.getSimpleName();
    private final CloseGuard mGuard = CloseGuard.get();
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case 1:
                    SpellCheckerSession.this.handleOnGetSuggestionsMultiple((SuggestionsInfo[]) message.obj);
                    break;
                case 2:
                    SpellCheckerSession.this.handleOnGetSentenceSuggestionsMultiple((SentenceSuggestionsInfo[]) message.obj);
                    break;
            }
        }
    };
    private final InternalListener mInternalListener;
    private final SpellCheckerInfo mSpellCheckerInfo;
    private final SpellCheckerSessionListener mSpellCheckerSessionListener;
    private final SpellCheckerSessionListenerImpl mSpellCheckerSessionListenerImpl;
    private final ITextServicesManager mTextServicesManager;

    public interface SpellCheckerSessionListener {
        void onGetSentenceSuggestions(SentenceSuggestionsInfo[] sentenceSuggestionsInfoArr);

        void onGetSuggestions(SuggestionsInfo[] suggestionsInfoArr);
    }

    public SpellCheckerSession(SpellCheckerInfo spellCheckerInfo, ITextServicesManager iTextServicesManager, SpellCheckerSessionListener spellCheckerSessionListener) {
        if (spellCheckerInfo == null || spellCheckerSessionListener == null || iTextServicesManager == null) {
            throw new NullPointerException();
        }
        this.mSpellCheckerInfo = spellCheckerInfo;
        this.mSpellCheckerSessionListenerImpl = new SpellCheckerSessionListenerImpl(this.mHandler);
        this.mInternalListener = new InternalListener(this.mSpellCheckerSessionListenerImpl);
        this.mTextServicesManager = iTextServicesManager;
        this.mSpellCheckerSessionListener = spellCheckerSessionListener;
        this.mGuard.open("finishSession");
    }

    public boolean isSessionDisconnected() {
        return this.mSpellCheckerSessionListenerImpl.isDisconnected();
    }

    public SpellCheckerInfo getSpellChecker() {
        return this.mSpellCheckerInfo;
    }

    public void cancel() {
        this.mSpellCheckerSessionListenerImpl.cancel();
    }

    public void close() {
        this.mGuard.close();
        try {
            this.mSpellCheckerSessionListenerImpl.close();
            this.mTextServicesManager.finishSpellCheckerService(this.mSpellCheckerSessionListenerImpl);
        } catch (RemoteException e) {
        }
    }

    public void getSentenceSuggestions(TextInfo[] textInfoArr, int i) {
        this.mSpellCheckerSessionListenerImpl.getSentenceSuggestionsMultiple(textInfoArr, i);
    }

    @Deprecated
    public void getSuggestions(TextInfo textInfo, int i) {
        getSuggestions(new TextInfo[]{textInfo}, i, false);
    }

    @Deprecated
    public void getSuggestions(TextInfo[] textInfoArr, int i, boolean z) {
        this.mSpellCheckerSessionListenerImpl.getSuggestionsMultiple(textInfoArr, i, z);
    }

    private void handleOnGetSuggestionsMultiple(SuggestionsInfo[] suggestionsInfoArr) {
        this.mSpellCheckerSessionListener.onGetSuggestions(suggestionsInfoArr);
    }

    private void handleOnGetSentenceSuggestionsMultiple(SentenceSuggestionsInfo[] sentenceSuggestionsInfoArr) {
        this.mSpellCheckerSessionListener.onGetSentenceSuggestions(sentenceSuggestionsInfoArr);
    }

    private static final class SpellCheckerSessionListenerImpl extends ISpellCheckerSessionListener.Stub {
        private static final int STATE_CLOSED_AFTER_CONNECTION = 2;
        private static final int STATE_CLOSED_BEFORE_CONNECTION = 3;
        private static final int STATE_CONNECTED = 1;
        private static final int STATE_WAIT_CONNECTION = 0;
        private static final int TASK_CANCEL = 1;
        private static final int TASK_CLOSE = 3;
        private static final int TASK_GET_SUGGESTIONS_MULTIPLE = 2;
        private static final int TASK_GET_SUGGESTIONS_MULTIPLE_FOR_SENTENCE = 4;
        private Handler mAsyncHandler;
        private Handler mHandler;
        private ISpellCheckerSession mISpellCheckerSession;
        private final Queue<SpellCheckerParams> mPendingTasks = new LinkedList();
        private int mState = 0;
        private HandlerThread mThread;

        private static String taskToString(int i) {
            switch (i) {
                case 1:
                    return "TASK_CANCEL";
                case 2:
                    return "TASK_GET_SUGGESTIONS_MULTIPLE";
                case 3:
                    return "TASK_CLOSE";
                case 4:
                    return "TASK_GET_SUGGESTIONS_MULTIPLE_FOR_SENTENCE";
                default:
                    return "Unexpected task=" + i;
            }
        }

        private static String stateToString(int i) {
            switch (i) {
                case 0:
                    return "STATE_WAIT_CONNECTION";
                case 1:
                    return "STATE_CONNECTED";
                case 2:
                    return "STATE_CLOSED_AFTER_CONNECTION";
                case 3:
                    return "STATE_CLOSED_BEFORE_CONNECTION";
                default:
                    return "Unexpected state=" + i;
            }
        }

        public SpellCheckerSessionListenerImpl(Handler handler) {
            this.mHandler = handler;
        }

        private static class SpellCheckerParams {
            public final boolean mSequentialWords;
            public ISpellCheckerSession mSession;
            public final int mSuggestionsLimit;
            public final TextInfo[] mTextInfos;
            public final int mWhat;

            public SpellCheckerParams(int i, TextInfo[] textInfoArr, int i2, boolean z) {
                this.mWhat = i;
                this.mTextInfos = textInfoArr;
                this.mSuggestionsLimit = i2;
                this.mSequentialWords = z;
            }
        }

        private void processTask(ISpellCheckerSession iSpellCheckerSession, SpellCheckerParams spellCheckerParams, boolean z) {
            if (z || this.mAsyncHandler == null) {
                switch (spellCheckerParams.mWhat) {
                    case 1:
                        try {
                            iSpellCheckerSession.onCancel();
                        } catch (RemoteException e) {
                            Log.e(SpellCheckerSession.TAG, "Failed to cancel " + e);
                        }
                        break;
                    case 2:
                        try {
                            iSpellCheckerSession.onGetSuggestionsMultiple(spellCheckerParams.mTextInfos, spellCheckerParams.mSuggestionsLimit, spellCheckerParams.mSequentialWords);
                        } catch (RemoteException e2) {
                            Log.e(SpellCheckerSession.TAG, "Failed to get suggestions " + e2);
                        }
                        break;
                    case 3:
                        try {
                            iSpellCheckerSession.onClose();
                        } catch (RemoteException e3) {
                            Log.e(SpellCheckerSession.TAG, "Failed to close " + e3);
                        }
                        break;
                    case 4:
                        try {
                            iSpellCheckerSession.onGetSentenceSuggestionsMultiple(spellCheckerParams.mTextInfos, spellCheckerParams.mSuggestionsLimit);
                        } catch (RemoteException e4) {
                            Log.e(SpellCheckerSession.TAG, "Failed to get suggestions " + e4);
                        }
                        break;
                }
            } else {
                spellCheckerParams.mSession = iSpellCheckerSession;
                this.mAsyncHandler.sendMessage(Message.obtain(this.mAsyncHandler, 1, spellCheckerParams));
            }
            if (spellCheckerParams.mWhat == 3) {
                synchronized (this) {
                    processCloseLocked();
                }
            }
        }

        private void processCloseLocked() {
            this.mISpellCheckerSession = null;
            if (this.mThread != null) {
                this.mThread.quit();
            }
            this.mHandler = null;
            this.mPendingTasks.clear();
            this.mThread = null;
            this.mAsyncHandler = null;
            switch (this.mState) {
                case 0:
                    this.mState = 3;
                    break;
                case 1:
                    this.mState = 2;
                    break;
                default:
                    Log.e(SpellCheckerSession.TAG, "processCloseLocked is called unexpectedly. mState=" + stateToString(this.mState));
                    break;
            }
        }

        public void onServiceConnected(ISpellCheckerSession iSpellCheckerSession) {
            synchronized (this) {
                int i = this.mState;
                if (i != 0) {
                    if (i != 3) {
                        Log.e(SpellCheckerSession.TAG, "ignoring onServiceConnected due to unexpected mState=" + stateToString(this.mState));
                        return;
                    }
                    return;
                }
                if (iSpellCheckerSession == null) {
                    Log.e(SpellCheckerSession.TAG, "ignoring onServiceConnected due to session=null");
                    return;
                }
                this.mISpellCheckerSession = iSpellCheckerSession;
                if ((iSpellCheckerSession.asBinder() instanceof Binder) && this.mThread == null) {
                    this.mThread = new HandlerThread("SpellCheckerSession", 10);
                    this.mThread.start();
                    this.mAsyncHandler = new Handler(this.mThread.getLooper()) {
                        @Override
                        public void handleMessage(Message message) {
                            SpellCheckerParams spellCheckerParams = (SpellCheckerParams) message.obj;
                            SpellCheckerSessionListenerImpl.this.processTask(spellCheckerParams.mSession, spellCheckerParams, true);
                        }
                    };
                }
                this.mState = 1;
                while (!this.mPendingTasks.isEmpty()) {
                    processTask(iSpellCheckerSession, this.mPendingTasks.poll(), false);
                }
            }
        }

        public void cancel() {
            processOrEnqueueTask(new SpellCheckerParams(1, null, 0, false));
        }

        public void getSuggestionsMultiple(TextInfo[] textInfoArr, int i, boolean z) {
            processOrEnqueueTask(new SpellCheckerParams(2, textInfoArr, i, z));
        }

        public void getSentenceSuggestionsMultiple(TextInfo[] textInfoArr, int i) {
            processOrEnqueueTask(new SpellCheckerParams(4, textInfoArr, i, false));
        }

        public void close() {
            processOrEnqueueTask(new SpellCheckerParams(3, null, 0, false));
        }

        public boolean isDisconnected() {
            boolean z;
            synchronized (this) {
                z = true;
                if (this.mState == 1) {
                    z = false;
                }
            }
            return z;
        }

        private void processOrEnqueueTask(SpellCheckerParams spellCheckerParams) {
            synchronized (this) {
                if (spellCheckerParams.mWhat == 3 && (this.mState == 2 || this.mState == 3)) {
                    return;
                }
                if (this.mState != 0 && this.mState != 1) {
                    Log.e(SpellCheckerSession.TAG, "ignoring processOrEnqueueTask due to unexpected mState=" + stateToString(this.mState) + " scp.mWhat=" + taskToString(spellCheckerParams.mWhat));
                    return;
                }
                if (this.mState == 0) {
                    if (spellCheckerParams.mWhat == 3) {
                        processCloseLocked();
                        return;
                    }
                    SpellCheckerParams spellCheckerParams2 = null;
                    if (spellCheckerParams.mWhat == 1) {
                        while (!this.mPendingTasks.isEmpty()) {
                            SpellCheckerParams spellCheckerParamsPoll = this.mPendingTasks.poll();
                            if (spellCheckerParamsPoll.mWhat == 3) {
                                spellCheckerParams2 = spellCheckerParamsPoll;
                            }
                        }
                    }
                    this.mPendingTasks.offer(spellCheckerParams);
                    if (spellCheckerParams2 != null) {
                        this.mPendingTasks.offer(spellCheckerParams2);
                    }
                    return;
                }
                processTask(this.mISpellCheckerSession, spellCheckerParams, false);
            }
        }

        @Override
        public void onGetSuggestions(SuggestionsInfo[] suggestionsInfoArr) {
            synchronized (this) {
                if (this.mHandler != null) {
                    this.mHandler.sendMessage(Message.obtain(this.mHandler, 1, suggestionsInfoArr));
                }
            }
        }

        @Override
        public void onGetSentenceSuggestions(SentenceSuggestionsInfo[] sentenceSuggestionsInfoArr) {
            synchronized (this) {
                if (this.mHandler != null) {
                    this.mHandler.sendMessage(Message.obtain(this.mHandler, 2, sentenceSuggestionsInfoArr));
                }
            }
        }
    }

    private static final class InternalListener extends ITextServicesSessionListener.Stub {
        private final SpellCheckerSessionListenerImpl mParentSpellCheckerSessionListenerImpl;

        public InternalListener(SpellCheckerSessionListenerImpl spellCheckerSessionListenerImpl) {
            this.mParentSpellCheckerSessionListenerImpl = spellCheckerSessionListenerImpl;
        }

        @Override
        public void onServiceConnected(ISpellCheckerSession iSpellCheckerSession) {
            this.mParentSpellCheckerSessionListenerImpl.onServiceConnected(iSpellCheckerSession);
        }
    }

    protected void finalize() throws Throwable {
        try {
            if (this.mGuard != null) {
                this.mGuard.warnIfOpen();
                close();
            }
        } finally {
            super.finalize();
        }
    }

    public ITextServicesSessionListener getTextServicesSessionListener() {
        return this.mInternalListener;
    }

    public ISpellCheckerSessionListener getSpellCheckerSessionListener() {
        return this.mSpellCheckerSessionListenerImpl;
    }
}
