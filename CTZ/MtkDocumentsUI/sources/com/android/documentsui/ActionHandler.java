package com.android.documentsui;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.view.DragEvent;
import com.android.documentsui.base.BooleanConsumer;
import com.android.documentsui.base.DocumentInfo;
import com.android.documentsui.base.DocumentStack;
import com.android.documentsui.base.RootInfo;
import com.android.documentsui.selection.ContentLock;
import com.android.documentsui.selection.ItemDetailsLookup;
import java.util.function.Consumer;

public interface ActionHandler {
    void copyToClipboard();

    void cutToClipboard();

    void deleteSelectedDocuments();

    boolean dropOn(DragEvent dragEvent, RootInfo rootInfo);

    void ejectRoot(RootInfo rootInfo, BooleanConsumer booleanConsumer);

    void getRootDocument(RootInfo rootInfo, int i, Consumer<DocumentInfo> consumer);

    void loadDocumentsForCurrentStack();

    void onActivityResult(int i, int i2, Intent intent);

    void openContainerDocument(DocumentInfo documentInfo);

    void openInNewWindow(DocumentStack documentStack);

    boolean openItem(ItemDetailsLookup.ItemDetails itemDetails, int i, int i2);

    void openRoot(ResolveInfo resolveInfo);

    void openRoot(RootInfo rootInfo);

    void openRootDocument(DocumentInfo documentInfo);

    void openSelectedInNewWindow();

    void openSettings(RootInfo rootInfo);

    void pasteIntoFolder(RootInfo rootInfo);

    void refreshDocument(DocumentInfo documentInfo, BooleanConsumer booleanConsumer);

    void registerDisplayStateChangedListener(Runnable runnable);

    DocumentInfo renameDocument(String str, DocumentInfo documentInfo);

    <T extends ActionHandler> T reset(ContentLock contentLock);

    void selectAllFiles();

    void setDebugMode(boolean z);

    void shareSelectedDocuments();

    void showAppDetails(ResolveInfo resolveInfo);

    void showChooserForDoc(DocumentInfo documentInfo);

    void showCreateDirectoryDialog();

    void showDebugMessage();

    void showInspector(DocumentInfo documentInfo);

    void springOpenDirectory(DocumentInfo documentInfo);

    void startAuthentication(PendingIntent pendingIntent);

    void unregisterDisplayStateChangedListener(Runnable runnable);

    void viewInOwner();
}
