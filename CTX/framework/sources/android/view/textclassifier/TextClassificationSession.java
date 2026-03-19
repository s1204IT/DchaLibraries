package android.view.textclassifier;

import android.view.textclassifier.SelectionSessionLogger;
import android.view.textclassifier.TextClassification;
import android.view.textclassifier.TextLinks;
import android.view.textclassifier.TextSelection;
import com.android.internal.util.Preconditions;

final class TextClassificationSession implements TextClassifier {
    static final boolean DEBUG_LOG_ENABLED = true;
    private static final String LOG_TAG = "TextClassificationSession";
    private final TextClassificationContext mClassificationContext;
    private final TextClassifier mDelegate;
    private boolean mDestroyed;
    private final SelectionEventHelper mEventHelper;
    private final TextClassificationSessionId mSessionId = new TextClassificationSessionId();

    TextClassificationSession(TextClassificationContext textClassificationContext, TextClassifier textClassifier) {
        this.mClassificationContext = (TextClassificationContext) Preconditions.checkNotNull(textClassificationContext);
        this.mDelegate = (TextClassifier) Preconditions.checkNotNull(textClassifier);
        this.mEventHelper = new SelectionEventHelper(this.mSessionId, this.mClassificationContext);
        initializeRemoteSession();
    }

    @Override
    public TextSelection suggestSelection(TextSelection.Request request) {
        checkDestroyed();
        return this.mDelegate.suggestSelection(request);
    }

    private void initializeRemoteSession() {
        if (this.mDelegate instanceof SystemTextClassifier) {
            ((SystemTextClassifier) this.mDelegate).initializeRemoteSession(this.mClassificationContext, this.mSessionId);
        }
    }

    @Override
    public TextClassification classifyText(TextClassification.Request request) {
        checkDestroyed();
        return this.mDelegate.classifyText(request);
    }

    @Override
    public TextLinks generateLinks(TextLinks.Request request) {
        checkDestroyed();
        return this.mDelegate.generateLinks(request);
    }

    @Override
    public void onSelectionEvent(SelectionEvent selectionEvent) {
        checkDestroyed();
        Preconditions.checkNotNull(selectionEvent);
        if (this.mEventHelper.sanitizeEvent(selectionEvent)) {
            this.mDelegate.onSelectionEvent(selectionEvent);
        }
    }

    @Override
    public void destroy() {
        this.mEventHelper.endSession();
        this.mDelegate.destroy();
        this.mDestroyed = true;
    }

    @Override
    public boolean isDestroyed() {
        return this.mDestroyed;
    }

    private void checkDestroyed() {
        if (this.mDestroyed) {
            throw new IllegalStateException("This TextClassification session has been destroyed");
        }
    }

    private static final class SelectionEventHelper {
        private final TextClassificationContext mContext;
        private int mInvocationMethod = 0;
        private SelectionEvent mPrevEvent;
        private final TextClassificationSessionId mSessionId;
        private SelectionEvent mSmartEvent;
        private SelectionEvent mStartEvent;

        SelectionEventHelper(TextClassificationSessionId textClassificationSessionId, TextClassificationContext textClassificationContext) {
            this.mSessionId = (TextClassificationSessionId) Preconditions.checkNotNull(textClassificationSessionId);
            this.mContext = (TextClassificationContext) Preconditions.checkNotNull(textClassificationContext);
        }

        boolean sanitizeEvent(SelectionEvent selectionEvent) {
            updateInvocationMethod(selectionEvent);
            modifyAutoSelectionEventType(selectionEvent);
            if (selectionEvent.getEventType() != 1 && this.mStartEvent == null) {
                Log.d(TextClassificationSession.LOG_TAG, "Selection session not yet started. Ignoring event");
                return false;
            }
            long jCurrentTimeMillis = System.currentTimeMillis();
            switch (selectionEvent.getEventType()) {
                case 1:
                    Preconditions.checkArgument(selectionEvent.getAbsoluteEnd() == selectionEvent.getAbsoluteStart() + 1);
                    selectionEvent.setSessionId(this.mSessionId);
                    this.mStartEvent = selectionEvent;
                    break;
                case 2:
                case 5:
                    if (this.mPrevEvent != null && this.mPrevEvent.getAbsoluteStart() == selectionEvent.getAbsoluteStart() && this.mPrevEvent.getAbsoluteEnd() == selectionEvent.getAbsoluteEnd()) {
                        return false;
                    }
                    break;
                case 3:
                case 4:
                    this.mSmartEvent = selectionEvent;
                    break;
            }
            selectionEvent.setEventTime(jCurrentTimeMillis);
            if (this.mStartEvent != null) {
                selectionEvent.setSessionId(this.mStartEvent.getSessionId()).setDurationSinceSessionStart(jCurrentTimeMillis - this.mStartEvent.getEventTime()).setStart(selectionEvent.getAbsoluteStart() - this.mStartEvent.getAbsoluteStart()).setEnd(selectionEvent.getAbsoluteEnd() - this.mStartEvent.getAbsoluteStart());
            }
            if (this.mSmartEvent != null) {
                selectionEvent.setResultId(this.mSmartEvent.getResultId()).setSmartStart(this.mSmartEvent.getAbsoluteStart() - this.mStartEvent.getAbsoluteStart()).setSmartEnd(this.mSmartEvent.getAbsoluteEnd() - this.mStartEvent.getAbsoluteStart());
            }
            if (this.mPrevEvent != null) {
                selectionEvent.setDurationSincePreviousEvent(jCurrentTimeMillis - this.mPrevEvent.getEventTime()).setEventIndex(this.mPrevEvent.getEventIndex() + 1);
            }
            this.mPrevEvent = selectionEvent;
            return true;
        }

        void endSession() {
            this.mPrevEvent = null;
            this.mSmartEvent = null;
            this.mStartEvent = null;
        }

        private void updateInvocationMethod(SelectionEvent selectionEvent) {
            selectionEvent.setTextClassificationSessionContext(this.mContext);
            if (selectionEvent.getInvocationMethod() == 0) {
                selectionEvent.setInvocationMethod(this.mInvocationMethod);
            } else {
                this.mInvocationMethod = selectionEvent.getInvocationMethod();
            }
        }

        private void modifyAutoSelectionEventType(SelectionEvent selectionEvent) {
            switch (selectionEvent.getEventType()) {
                case 3:
                case 4:
                case 5:
                    if (isPlatformLocalTextClassifierSmartSelection(selectionEvent.getResultId())) {
                        if (selectionEvent.getAbsoluteEnd() - selectionEvent.getAbsoluteStart() > 1) {
                            selectionEvent.setEventType(4);
                        } else {
                            selectionEvent.setEventType(3);
                        }
                    } else {
                        selectionEvent.setEventType(5);
                    }
                    break;
            }
        }

        private static boolean isPlatformLocalTextClassifierSmartSelection(String str) {
            return TextClassifier.DEFAULT_LOG_TAG.equals(SelectionSessionLogger.SignatureParser.getClassifierId(str));
        }
    }
}
