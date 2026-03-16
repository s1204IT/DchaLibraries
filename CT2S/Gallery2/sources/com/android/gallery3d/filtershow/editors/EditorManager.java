package com.android.gallery3d.filtershow.editors;

import com.android.gallery3d.filtershow.EditorPlaceHolder;

public class EditorManager {
    public static void addEditors(EditorPlaceHolder editorPlaceHolder) {
        editorPlaceHolder.addEditor(new EditorGrad());
        editorPlaceHolder.addEditor(new EditorChanSat());
        editorPlaceHolder.addEditor(new EditorZoom());
        editorPlaceHolder.addEditor(new EditorCurves());
        editorPlaceHolder.addEditor(new EditorTinyPlanet());
        editorPlaceHolder.addEditor(new EditorDraw());
        editorPlaceHolder.addEditor(new EditorVignette());
        editorPlaceHolder.addEditor(new EditorColorBorder());
        editorPlaceHolder.addEditor(new EditorMirror());
        editorPlaceHolder.addEditor(new EditorRotate());
        editorPlaceHolder.addEditor(new EditorStraighten());
        editorPlaceHolder.addEditor(new EditorCrop());
    }
}
