package com.android.documentsui.inspector;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.BenesseExtension;
import android.os.Bundle;
import android.view.View;
import com.android.documentsui.DocumentsApplication;
import com.android.documentsui.ProviderExecutor;
import com.android.documentsui.R;
import com.android.documentsui.base.DocumentInfo;
import com.android.documentsui.base.Lookup;
import com.android.documentsui.inspector.actions.Action;
import com.android.documentsui.inspector.actions.ClearDefaultAppAction;
import com.android.documentsui.inspector.actions.ShowInProviderAction;
import com.android.documentsui.roots.ProvidersAccess;
import com.android.documentsui.ui.Snackbars;
import com.android.internal.util.Preconditions;
import java.util.function.Consumer;

public final class InspectorController {
    static final boolean $assertionsDisabled = false;
    private final ActionDisplay mAppDefaults;
    private Bundle mArgs;
    private final Context mContext;
    private final DebugDisplay mDebugView;
    private final DetailsDisplay mDetails;
    private final Runnable mErrorSnackbar;
    private final HeaderDisplay mHeader;
    private final DataSupplier mLoader;
    private final MediaDisplay mMedia;
    private final PackageManager mPackageManager;
    private final ProvidersAccess mProviders;
    private final ActionDisplay mShowProvider;

    public interface ActionDisplay extends Display {
        void init(Action action, View.OnClickListener onClickListener);

        void setAppIcon(Drawable drawable);

        void setAppName(String str);

        void showAction(boolean z);
    }

    public interface DataSupplier {
        void getDocumentMetadata(Uri uri, Consumer<Bundle> consumer);

        void loadDirCount(DocumentInfo documentInfo, Consumer<Integer> consumer);

        void loadDocInfo(Uri uri, Consumer<DocumentInfo> consumer);

        void reset();
    }

    public interface DebugDisplay extends Display {
        void accept(Bundle bundle);

        void accept(DocumentInfo documentInfo);

        boolean isEmpty();
    }

    public interface DetailsDisplay {
        void accept(DocumentInfo documentInfo);

        void setChildrenCount(int i);
    }

    public interface Display {
        void setVisible(boolean z);
    }

    public interface HeaderDisplay {
        void accept(DocumentInfo documentInfo, String str);
    }

    public interface MediaDisplay extends Display {
        void accept(DocumentInfo documentInfo, Bundle bundle, Runnable runnable);

        boolean isEmpty();
    }

    public interface TableDisplay extends Display {
        void put(int i, CharSequence charSequence);

        void put(int i, CharSequence charSequence, View.OnClickListener onClickListener);
    }

    public InspectorController(Context context, DataSupplier dataSupplier, PackageManager packageManager, ProvidersAccess providersAccess, HeaderDisplay headerDisplay, DetailsDisplay detailsDisplay, MediaDisplay mediaDisplay, ActionDisplay actionDisplay, ActionDisplay actionDisplay2, DebugDisplay debugDisplay, Bundle bundle, Runnable runnable) {
        Preconditions.checkArgument(context != null);
        Preconditions.checkArgument(dataSupplier != null);
        Preconditions.checkArgument(packageManager != null);
        Preconditions.checkArgument(providersAccess != null);
        Preconditions.checkArgument(headerDisplay != null);
        Preconditions.checkArgument(detailsDisplay != null);
        Preconditions.checkArgument(mediaDisplay != null);
        Preconditions.checkArgument(actionDisplay != null);
        Preconditions.checkArgument(actionDisplay2 != null);
        Preconditions.checkArgument(debugDisplay != null);
        Preconditions.checkArgument(bundle != null);
        Preconditions.checkArgument(runnable != null);
        this.mContext = context;
        this.mLoader = dataSupplier;
        this.mPackageManager = packageManager;
        this.mProviders = providersAccess;
        this.mHeader = headerDisplay;
        this.mDetails = detailsDisplay;
        this.mMedia = mediaDisplay;
        this.mShowProvider = actionDisplay;
        this.mAppDefaults = actionDisplay2;
        this.mArgs = bundle;
        this.mDebugView = debugDisplay;
        this.mErrorSnackbar = runnable;
    }

    public InspectorController(final Activity activity, DataSupplier dataSupplier, View view, Bundle bundle) {
        this(activity, dataSupplier, activity.getPackageManager(), DocumentsApplication.getProvidersCache(activity), (HeaderView) view.findViewById(R.id.inspector_header_view), (DetailsView) view.findViewById(R.id.inspector_details_view), (MediaView) view.findViewById(R.id.inspector_media_view), (ActionDisplay) view.findViewById(R.id.inspector_show_in_provider_view), (ActionDisplay) view.findViewById(R.id.inspector_app_defaults_view), (DebugView) view.findViewById(R.id.inspector_debug_view), bundle, new Runnable() {
            @Override
            public final void run() {
                Snackbars.showInspectorError(activity);
            }
        });
        if (bundle.getBoolean("com.android.documentsui.SHOW_DEBUG")) {
            ((DebugView) view.findViewById(R.id.inspector_debug_view)).init(new Lookup() {
                @Override
                public final Object lookup(Object obj) {
                    return ProviderExecutor.forAuthority((String) obj);
                }
            });
        }
    }

    public void reset() {
        this.mLoader.reset();
    }

    public void loadInfo(Uri uri) {
        this.mLoader.loadDocInfo(uri, new Consumer() {
            @Override
            public final void accept(Object obj) {
                this.f$0.updateView((DocumentInfo) obj);
            }
        });
    }

    private void updateView(final DocumentInfo documentInfo) {
        if (documentInfo == null) {
            this.mErrorSnackbar.run();
            return;
        }
        this.mHeader.accept(documentInfo, this.mArgs.getString("android.intent.extra.TITLE", documentInfo.displayName));
        this.mDetails.accept(documentInfo);
        if (documentInfo.isDirectory()) {
            this.mLoader.loadDirCount(documentInfo, new Consumer() {
                @Override
                public final void accept(Object obj) {
                    this.f$0.displayChildCount((Integer) obj);
                }
            });
        } else {
            this.mShowProvider.setVisible(documentInfo.isSettingsSupported());
            if (documentInfo.isSettingsSupported()) {
                this.mShowProvider.init(new ShowInProviderAction(this.mContext, this.mPackageManager, documentInfo, this.mProviders), new View.OnClickListener() {
                    @Override
                    public final void onClick(View view) {
                        this.f$0.showInProvider(documentInfo.derivedUri);
                    }
                });
            }
            final ClearDefaultAppAction clearDefaultAppAction = new ClearDefaultAppAction(this.mContext, this.mPackageManager, documentInfo);
            this.mAppDefaults.setVisible(clearDefaultAppAction.canPerformAction());
            if (clearDefaultAppAction.canPerformAction()) {
                this.mAppDefaults.init(clearDefaultAppAction, new View.OnClickListener() {
                    @Override
                    public final void onClick(View view) {
                        this.f$0.clearDefaultApp(clearDefaultAppAction.getPackageName());
                    }
                });
            }
        }
        if (documentInfo.isMetadataSupported()) {
            this.mLoader.getDocumentMetadata(documentInfo.derivedUri, new Consumer() {
                @Override
                public final void accept(Object obj) {
                    this.f$0.onDocumentMetadataLoaded(documentInfo, (Bundle) obj);
                }
            });
        }
        this.mMedia.setVisible(!this.mMedia.isEmpty());
        if (this.mArgs.getBoolean("com.android.documentsui.SHOW_DEBUG")) {
            this.mDebugView.accept(documentInfo);
        }
        this.mDebugView.setVisible(this.mArgs.getBoolean("com.android.documentsui.SHOW_DEBUG") && !this.mDebugView.isEmpty());
    }

    private void onDocumentMetadataLoaded(DocumentInfo documentInfo, Bundle bundle) {
        if (bundle == null) {
            return;
        }
        Runnable runnable = null;
        if (MetadataUtils.hasGeoCoordinates(bundle)) {
            float[] geoCoordinates = MetadataUtils.getGeoCoordinates(bundle);
            final Intent intentCreateGeoIntent = createGeoIntent(geoCoordinates[0], geoCoordinates[1], documentInfo.displayName);
            if (BenesseExtension.getDchaState() == 0 && hasHandler(intentCreateGeoIntent)) {
                runnable = new Runnable() {
                    @Override
                    public final void run() {
                        this.f$0.startActivity(intentCreateGeoIntent);
                    }
                };
            }
        }
        this.mMedia.accept(documentInfo, bundle, runnable);
        if (this.mArgs.getBoolean("com.android.documentsui.SHOW_DEBUG")) {
            this.mDebugView.accept(bundle);
        }
    }

    private void displayChildCount(Integer num) {
        this.mDetails.setChildrenCount(num.intValue());
    }

    private void startActivity(Intent intent) {
        this.mContext.startActivity(intent);
    }

    private boolean hasHandler(Intent intent) {
        return this.mPackageManager.resolveActivity(intent, 0) != null;
    }

    private static Intent createGeoIntent(float f, float f2, String str) {
        if (str == null) {
            str = "";
        }
        return new Intent("android.intent.action.VIEW", Uri.parse("geo:0,0?q=" + f + " " + f2 + "(" + Uri.encode(str) + ")"));
    }

    public void showInProvider(Uri uri) {
        Intent intent = new Intent("android.provider.action.DOCUMENT_SETTINGS");
        intent.setPackage(this.mProviders.getPackageName(uri.getAuthority()));
        intent.addCategory("android.intent.category.DEFAULT");
        intent.setData(uri);
        this.mContext.startActivity(intent);
    }

    public void clearDefaultApp(String str) {
        this.mPackageManager.clearPackagePreferredActivities(str);
        this.mAppDefaults.setAppIcon(null);
        this.mAppDefaults.setAppName(this.mContext.getString(R.string.handler_app_not_selected));
        this.mAppDefaults.showAction(false);
    }
}
