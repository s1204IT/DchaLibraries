package com.android.documentsui.inspector;

import android.content.Context;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.AttributeSet;
import android.widget.TextView;
import com.android.documentsui.R;
import com.android.documentsui.base.DocumentInfo;
import com.android.documentsui.base.DummyLookup;
import com.android.documentsui.base.Lookup;
import com.android.documentsui.inspector.InspectorController;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.Executor;

public class DebugView extends TableView implements InspectorController.DebugDisplay {
    static final boolean $assertionsDisabled = false;
    private final Context mContext;
    private Lookup<String, Executor> mExecutors;
    private final Resources mRes;

    public DebugView(Context context) {
        this(context, null);
    }

    public DebugView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public DebugView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mExecutors = new DummyLookup();
        this.mContext = context;
        this.mRes = context.getResources();
    }

    void init(Lookup<String, Executor> lookup) {
        setBackgroundColor(-1);
        this.mExecutors = lookup;
    }

    @Override
    public void accept(final DocumentInfo documentInfo) {
        setTitle(R.string.inspector_debug_section, false);
        put(R.string.debug_content_uri, documentInfo.derivedUri.toString());
        put(R.string.debug_document_id, documentInfo.documentId);
        put(R.string.debug_raw_mimetype, documentInfo.mimeType);
        put(R.string.debug_stream_types, "-");
        put(R.string.debug_raw_size, NumberFormat.getInstance().format(documentInfo.size));
        put(R.string.debug_is_archive, documentInfo.isArchive());
        put(R.string.debug_is_container, documentInfo.isContainer());
        put(R.string.debug_is_partial, documentInfo.isPartial());
        put(R.string.debug_is_virtual, documentInfo.isVirtual());
        put(R.string.debug_supports_create, documentInfo.isCreateSupported());
        put(R.string.debug_supports_delete, documentInfo.isDeleteSupported());
        put(R.string.debug_supports_metadata, documentInfo.isMetadataSupported());
        put(R.string.debug_supports_remove, documentInfo.isRemoveSupported());
        put(R.string.debug_supports_rename, documentInfo.isRenameSupported());
        put(R.string.debug_supports_settings, documentInfo.isSettingsSupported());
        put(R.string.debug_supports_thumbnail, documentInfo.isThumbnailSupported());
        put(R.string.debug_supports_weblink, documentInfo.isWeblinkSupported());
        put(R.string.debug_supports_write, documentInfo.isWriteSupported());
        Executor executorLookup = this.mExecutors.lookup(documentInfo.derivedUri.getAuthority());
        if (executorLookup != null) {
            new AsyncTask<Void, Void, String[]>() {
                @Override
                protected String[] doInBackground(Void... voidArr) {
                    return DebugView.this.mContext.getContentResolver().getStreamTypes(documentInfo.derivedUri, "*/*");
                }

                @Override
                protected void onPostExecute(String[] strArr) {
                    DebugView.this.put(R.string.debug_stream_types, strArr != null ? Arrays.toString(strArr) : "[]");
                }
            }.executeOnExecutor(executorLookup, (Void[]) null);
        }
    }

    @Override
    public void accept(Bundle bundle) {
        String[] stringArray;
        if (bundle == null || (stringArray = bundle.getStringArray("android:documentMetadataType")) == null) {
            return;
        }
        for (String str : stringArray) {
            dumpMetadata(str, bundle.getBundle(str));
        }
    }

    private void dumpMetadata(String str, Bundle bundle) {
        putTitle(String.format(this.mContext.getResources().getString(R.string.inspector_debug_metadata_section), str), true);
        ArrayList<String> arrayList = new ArrayList(bundle.keySet());
        Collections.sort(arrayList);
        for (String str2 : arrayList) {
            put(str2, String.valueOf(bundle.get(str2)));
        }
    }

    private void put(int i, boolean z) {
        ((TextView) put(this.mRes.getString(i), String.valueOf(z)).findViewById(R.id.table_row_value)).setTextColor(z ? -16751616 : -6676448);
    }
}
