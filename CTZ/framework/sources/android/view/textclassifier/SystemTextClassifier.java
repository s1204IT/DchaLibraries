package android.view.textclassifier;

import android.content.Context;
import android.os.Looper;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.service.textclassifier.ITextClassificationCallback;
import android.service.textclassifier.ITextClassifierService;
import android.service.textclassifier.ITextLinksCallback;
import android.service.textclassifier.ITextSelectionCallback;
import android.view.textclassifier.TextClassification;
import android.view.textclassifier.TextClassifier;
import android.view.textclassifier.TextLinks;
import android.view.textclassifier.TextSelection;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.Preconditions;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@VisibleForTesting(visibility = VisibleForTesting.Visibility.PACKAGE)
public final class SystemTextClassifier implements TextClassifier {
    private static final String LOG_TAG = "SystemTextClassifier";
    private final TextClassifier mFallback;
    private final ITextClassifierService mManagerService = ITextClassifierService.Stub.asInterface(ServiceManager.getServiceOrThrow(Context.TEXT_CLASSIFICATION_SERVICE));
    private final String mPackageName;
    private TextClassificationSessionId mSessionId;
    private final TextClassificationConstants mSettings;

    public SystemTextClassifier(Context context, TextClassificationConstants textClassificationConstants) throws ServiceManager.ServiceNotFoundException {
        this.mSettings = (TextClassificationConstants) Preconditions.checkNotNull(textClassificationConstants);
        this.mFallback = ((TextClassificationManager) context.getSystemService(TextClassificationManager.class)).getTextClassifier(0);
        this.mPackageName = (String) Preconditions.checkNotNull(context.getPackageName());
    }

    @Override
    public TextSelection suggestSelection(TextSelection.Request request) {
        Preconditions.checkNotNull(request);
        TextClassifier.Utils.checkMainThread();
        try {
            TextSelectionCallback textSelectionCallback = new TextSelectionCallback();
            this.mManagerService.onSuggestSelection(this.mSessionId, request, textSelectionCallback);
            TextSelection textSelection = textSelectionCallback.mReceiver.get();
            if (textSelection != null) {
                return textSelection;
            }
        } catch (RemoteException | InterruptedException e) {
            Log.e(LOG_TAG, "Error suggesting selection for text. Using fallback.", e);
        }
        return this.mFallback.suggestSelection(request);
    }

    @Override
    public TextClassification classifyText(TextClassification.Request request) {
        Preconditions.checkNotNull(request);
        TextClassifier.Utils.checkMainThread();
        try {
            TextClassificationCallback textClassificationCallback = new TextClassificationCallback();
            this.mManagerService.onClassifyText(this.mSessionId, request, textClassificationCallback);
            TextClassification textClassification = textClassificationCallback.mReceiver.get();
            if (textClassification != null) {
                return textClassification;
            }
        } catch (RemoteException | InterruptedException e) {
            Log.e(LOG_TAG, "Error classifying text. Using fallback.", e);
        }
        return this.mFallback.classifyText(request);
    }

    @Override
    public TextLinks generateLinks(TextLinks.Request request) {
        Preconditions.checkNotNull(request);
        TextClassifier.Utils.checkMainThread();
        if (!this.mSettings.isSmartLinkifyEnabled() && request.isLegacyFallback()) {
            return TextClassifier.Utils.generateLegacyLinks(request);
        }
        try {
            request.setCallingPackageName(this.mPackageName);
            TextLinksCallback textLinksCallback = new TextLinksCallback();
            this.mManagerService.onGenerateLinks(this.mSessionId, request, textLinksCallback);
            TextLinks textLinks = textLinksCallback.mReceiver.get();
            if (textLinks != null) {
                return textLinks;
            }
        } catch (RemoteException | InterruptedException e) {
            Log.e(LOG_TAG, "Error generating links. Using fallback.", e);
        }
        return this.mFallback.generateLinks(request);
    }

    @Override
    public void onSelectionEvent(SelectionEvent selectionEvent) {
        Preconditions.checkNotNull(selectionEvent);
        TextClassifier.Utils.checkMainThread();
        try {
            this.mManagerService.onSelectionEvent(this.mSessionId, selectionEvent);
        } catch (RemoteException e) {
            Log.e(LOG_TAG, "Error reporting selection event.", e);
        }
    }

    @Override
    public int getMaxGenerateLinksTextLength() {
        return this.mFallback.getMaxGenerateLinksTextLength();
    }

    @Override
    public void destroy() {
        try {
            if (this.mSessionId != null) {
                this.mManagerService.onDestroyTextClassificationSession(this.mSessionId);
            }
        } catch (RemoteException e) {
            Log.e(LOG_TAG, "Error destroying classification session.", e);
        }
    }

    void initializeRemoteSession(TextClassificationContext textClassificationContext, TextClassificationSessionId textClassificationSessionId) {
        this.mSessionId = (TextClassificationSessionId) Preconditions.checkNotNull(textClassificationSessionId);
        try {
            this.mManagerService.onCreateTextClassificationSession(textClassificationContext, this.mSessionId);
        } catch (RemoteException e) {
            Log.e(LOG_TAG, "Error starting a new classification session.", e);
        }
    }

    private static final class TextSelectionCallback extends ITextSelectionCallback.Stub {
        final ResponseReceiver<TextSelection> mReceiver;

        private TextSelectionCallback() {
            this.mReceiver = new ResponseReceiver<>();
        }

        @Override
        public void onSuccess(TextSelection textSelection) {
            this.mReceiver.onSuccess(textSelection);
        }

        @Override
        public void onFailure() {
            this.mReceiver.onFailure();
        }
    }

    private static final class TextClassificationCallback extends ITextClassificationCallback.Stub {
        final ResponseReceiver<TextClassification> mReceiver;

        private TextClassificationCallback() {
            this.mReceiver = new ResponseReceiver<>();
        }

        @Override
        public void onSuccess(TextClassification textClassification) {
            this.mReceiver.onSuccess(textClassification);
        }

        @Override
        public void onFailure() {
            this.mReceiver.onFailure();
        }
    }

    private static final class TextLinksCallback extends ITextLinksCallback.Stub {
        final ResponseReceiver<TextLinks> mReceiver;

        private TextLinksCallback() {
            this.mReceiver = new ResponseReceiver<>();
        }

        @Override
        public void onSuccess(TextLinks textLinks) {
            this.mReceiver.onSuccess(textLinks);
        }

        @Override
        public void onFailure() {
            this.mReceiver.onFailure();
        }
    }

    private static final class ResponseReceiver<T> {
        private final CountDownLatch mLatch;
        private T mResponse;

        private ResponseReceiver() {
            this.mLatch = new CountDownLatch(1);
        }

        public void onSuccess(T t) {
            this.mResponse = t;
            this.mLatch.countDown();
        }

        public void onFailure() {
            Log.e(SystemTextClassifier.LOG_TAG, "Request failed.", null);
            this.mLatch.countDown();
        }

        public T get() throws InterruptedException {
            if (Looper.myLooper() != Looper.getMainLooper()) {
                this.mLatch.await(2L, TimeUnit.SECONDS);
            }
            return this.mResponse;
        }
    }
}
