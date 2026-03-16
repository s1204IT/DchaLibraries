package com.android.contacts.editor;

import com.android.contacts.common.model.RawContactDelta;
import com.android.contacts.common.model.ValuesDelta;
import com.android.contacts.common.model.dataitem.DataKind;

public interface Editor {

    public interface EditorListener {
        void onDeleteRequested(Editor editor);

        void onRequest(int i);
    }

    void clearAllFields();

    void deleteEditor();

    boolean isEmpty();

    void setDeletable(boolean z);

    void setEditorListener(EditorListener editorListener);

    void setValues(DataKind dataKind, ValuesDelta valuesDelta, RawContactDelta rawContactDelta, boolean z, ViewIdGenerator viewIdGenerator);
}
