package com.android.musicvis.vis5;

import android.renderscript.Element;
import android.renderscript.FieldPacker;
import android.renderscript.RenderScript;
import android.renderscript.Script;
import java.lang.ref.WeakReference;

public class ScriptField_Vertex extends Script.FieldBase {
    private static WeakReference<Element> mElementCache = new WeakReference<>(null);
    private Item[] mItemArray = null;
    private FieldPacker mIOBuffer = null;

    public static class Item {
    }

    public static Element createElement(RenderScript rs) {
        Element.Builder eb = new Element.Builder(rs);
        eb.add(Element.F32_2(rs), "position");
        eb.add(Element.F32_2(rs), "texture0");
        return eb.create();
    }

    public ScriptField_Vertex(RenderScript rs, int count) {
        this.mElement = createElement(rs);
        init(rs, count);
    }
}
