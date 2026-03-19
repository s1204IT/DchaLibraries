package android.service.textclassifier;

import android.Manifest;
import android.annotation.SystemApi;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.os.CancellationSignal;
import android.os.IBinder;
import android.os.RemoteException;
import android.service.textclassifier.ITextClassifierService;
import android.text.TextUtils;
import android.util.Slog;
import android.view.textclassifier.SelectionEvent;
import android.view.textclassifier.TextClassification;
import android.view.textclassifier.TextClassificationContext;
import android.view.textclassifier.TextClassificationManager;
import android.view.textclassifier.TextClassificationSessionId;
import android.view.textclassifier.TextClassifier;
import android.view.textclassifier.TextLinks;
import android.view.textclassifier.TextSelection;
import com.android.internal.util.Preconditions;

@SystemApi
public abstract class TextClassifierService extends Service {
    private static final String LOG_TAG = "TextClassifierService";

    @SystemApi
    public static final String SERVICE_INTERFACE = "android.service.textclassifier.TextClassifierService";
    private final ITextClassifierService.Stub mBinder = new ITextClassifierService.Stub() {
        private final CancellationSignal mCancellationSignal = new CancellationSignal();

        @Override
        public void onSuggestSelection(TextClassificationSessionId textClassificationSessionId, TextSelection.Request request, final ITextSelectionCallback iTextSelectionCallback) throws RemoteException {
            Preconditions.checkNotNull(request);
            Preconditions.checkNotNull(iTextSelectionCallback);
            TextClassifierService.this.onSuggestSelection(request.getText(), request.getStartIndex(), request.getEndIndex(), TextSelection.Options.from(textClassificationSessionId, request), this.mCancellationSignal, new Callback<TextSelection>() {
                @Override
                public void onSuccess(TextSelection textSelection) {
                    try {
                        iTextSelectionCallback.onSuccess(textSelection);
                    } catch (RemoteException e) {
                        Slog.d(TextClassifierService.LOG_TAG, "Error calling callback");
                    }
                }

                @Override
                public void onFailure(CharSequence charSequence) {
                    try {
                        if (iTextSelectionCallback.asBinder().isBinderAlive()) {
                            iTextSelectionCallback.onFailure();
                        }
                    } catch (RemoteException e) {
                        Slog.d(TextClassifierService.LOG_TAG, "Error calling callback");
                    }
                }
            });
        }

        @Override
        public void onClassifyText(TextClassificationSessionId textClassificationSessionId, TextClassification.Request request, final ITextClassificationCallback iTextClassificationCallback) throws RemoteException {
            Preconditions.checkNotNull(request);
            Preconditions.checkNotNull(iTextClassificationCallback);
            TextClassifierService.this.onClassifyText(request.getText(), request.getStartIndex(), request.getEndIndex(), TextClassification.Options.from(textClassificationSessionId, request), this.mCancellationSignal, new Callback<TextClassification>() {
                @Override
                public void onSuccess(TextClassification textClassification) {
                    try {
                        iTextClassificationCallback.onSuccess(textClassification);
                    } catch (RemoteException e) {
                        Slog.d(TextClassifierService.LOG_TAG, "Error calling callback");
                    }
                }

                @Override
                public void onFailure(CharSequence charSequence) {
                    try {
                        iTextClassificationCallback.onFailure();
                    } catch (RemoteException e) {
                        Slog.d(TextClassifierService.LOG_TAG, "Error calling callback");
                    }
                }
            });
        }

        @Override
        public void onGenerateLinks(TextClassificationSessionId textClassificationSessionId, TextLinks.Request request, final ITextLinksCallback iTextLinksCallback) throws RemoteException {
            Preconditions.checkNotNull(request);
            Preconditions.checkNotNull(iTextLinksCallback);
            TextClassifierService.this.onGenerateLinks(request.getText(), TextLinks.Options.from(textClassificationSessionId, request), this.mCancellationSignal, new Callback<TextLinks>() {
                @Override
                public void onSuccess(TextLinks textLinks) {
                    try {
                        iTextLinksCallback.onSuccess(textLinks);
                    } catch (RemoteException e) {
                        Slog.d(TextClassifierService.LOG_TAG, "Error calling callback");
                    }
                }

                @Override
                public void onFailure(CharSequence charSequence) {
                    try {
                        iTextLinksCallback.onFailure();
                    } catch (RemoteException e) {
                        Slog.d(TextClassifierService.LOG_TAG, "Error calling callback");
                    }
                }
            });
        }

        @Override
        public void onSelectionEvent(TextClassificationSessionId textClassificationSessionId, SelectionEvent selectionEvent) throws RemoteException {
            Preconditions.checkNotNull(selectionEvent);
            TextClassifierService.this.onSelectionEvent(textClassificationSessionId, selectionEvent);
        }

        @Override
        public void onCreateTextClassificationSession(TextClassificationContext textClassificationContext, TextClassificationSessionId textClassificationSessionId) throws RemoteException {
            Preconditions.checkNotNull(textClassificationContext);
            Preconditions.checkNotNull(textClassificationSessionId);
            TextClassifierService.this.onCreateTextClassificationSession(textClassificationContext, textClassificationSessionId);
        }

        @Override
        public void onDestroyTextClassificationSession(TextClassificationSessionId textClassificationSessionId) throws RemoteException {
            TextClassifierService.this.onDestroyTextClassificationSession(textClassificationSessionId);
        }
    };

    @SystemApi
    public interface Callback<T> {
        void onFailure(CharSequence charSequence);

        void onSuccess(T t);
    }

    public abstract void onClassifyText(TextClassificationSessionId textClassificationSessionId, TextClassification.Request request, CancellationSignal cancellationSignal, Callback<TextClassification> callback);

    public abstract void onGenerateLinks(TextClassificationSessionId textClassificationSessionId, TextLinks.Request request, CancellationSignal cancellationSignal, Callback<TextLinks> callback);

    public abstract void onSuggestSelection(TextClassificationSessionId textClassificationSessionId, TextSelection.Request request, CancellationSignal cancellationSignal, Callback<TextSelection> callback);

    @Override
    public final IBinder onBind(Intent intent) {
        if (SERVICE_INTERFACE.equals(intent.getAction())) {
            return this.mBinder;
        }
        return null;
    }

    public void onSuggestSelection(CharSequence charSequence, int i, int i2, TextSelection.Options options, CancellationSignal cancellationSignal, Callback<TextSelection> callback) {
        TextSelection.Request requestBuild;
        TextClassificationSessionId sessionId = options.getSessionId();
        if (options.getRequest() != null) {
            requestBuild = options.getRequest();
        } else {
            requestBuild = new TextSelection.Request.Builder(charSequence, i, i2).setDefaultLocales(options.getDefaultLocales()).build();
        }
        onSuggestSelection(sessionId, requestBuild, cancellationSignal, callback);
    }

    public void onClassifyText(CharSequence charSequence, int i, int i2, TextClassification.Options options, CancellationSignal cancellationSignal, Callback<TextClassification> callback) {
        TextClassification.Request requestBuild;
        TextClassificationSessionId sessionId = options.getSessionId();
        if (options.getRequest() != null) {
            requestBuild = options.getRequest();
        } else {
            requestBuild = new TextClassification.Request.Builder(charSequence, i, i2).setDefaultLocales(options.getDefaultLocales()).setReferenceTime(options.getReferenceTime()).build();
        }
        onClassifyText(sessionId, requestBuild, cancellationSignal, callback);
    }

    public void onGenerateLinks(CharSequence charSequence, TextLinks.Options options, CancellationSignal cancellationSignal, Callback<TextLinks> callback) {
        TextLinks.Request requestBuild;
        TextClassificationSessionId sessionId = options.getSessionId();
        if (options.getRequest() != null) {
            requestBuild = options.getRequest();
        } else {
            requestBuild = new TextLinks.Request.Builder(charSequence).setDefaultLocales(options.getDefaultLocales()).setEntityConfig(options.getEntityConfig()).build();
        }
        onGenerateLinks(sessionId, requestBuild, cancellationSignal, callback);
    }

    public void onSelectionEvent(TextClassificationSessionId textClassificationSessionId, SelectionEvent selectionEvent) {
    }

    public void onCreateTextClassificationSession(TextClassificationContext textClassificationContext, TextClassificationSessionId textClassificationSessionId) {
    }

    public void onDestroyTextClassificationSession(TextClassificationSessionId textClassificationSessionId) {
    }

    public final TextClassifier getLocalTextClassifier() {
        TextClassificationManager textClassificationManager = (TextClassificationManager) getSystemService(TextClassificationManager.class);
        if (textClassificationManager != null) {
            return textClassificationManager.getTextClassifier(0);
        }
        return TextClassifier.NO_OP;
    }

    public static ComponentName getServiceComponentName(Context context) {
        String systemTextClassifierPackageName = context.getPackageManager().getSystemTextClassifierPackageName();
        if (TextUtils.isEmpty(systemTextClassifierPackageName)) {
            Slog.d(LOG_TAG, "No configured system TextClassifierService");
            return null;
        }
        ResolveInfo resolveInfoResolveService = context.getPackageManager().resolveService(new Intent(SERVICE_INTERFACE).setPackage(systemTextClassifierPackageName), 1048576);
        if (resolveInfoResolveService == null || resolveInfoResolveService.serviceInfo == null) {
            Slog.w(LOG_TAG, String.format("Package or service not found in package %s", systemTextClassifierPackageName));
            return null;
        }
        ServiceInfo serviceInfo = resolveInfoResolveService.serviceInfo;
        if (Manifest.permission.BIND_TEXTCLASSIFIER_SERVICE.equals(serviceInfo.permission)) {
            return serviceInfo.getComponentName();
        }
        Slog.w(LOG_TAG, String.format("Service %s should require %s permission. Found %s permission", serviceInfo.getComponentName(), Manifest.permission.BIND_TEXTCLASSIFIER_SERVICE, serviceInfo.permission));
        return null;
    }
}
