package com.android.contacts.logging;

import android.app.Activity;
import com.android.contactsbind.ObjectFactory;

public abstract class Logger {
    public abstract void logEditorEventImpl(EditorEvent editorEvent);

    public abstract void logListEventImpl(ListEvent listEvent);

    public abstract void logQuickContactEventImpl(QuickContactEvent quickContactEvent);

    public abstract void logScreenViewImpl(int i, int i2);

    public abstract void logSearchEventImpl(SearchState searchState);

    private static Logger getInstance() {
        return ObjectFactory.getLogger();
    }

    public static void logScreenView(Activity activity, int i) {
        logScreenView(activity, i, 0);
    }

    public static void logScreenView(Activity activity, int i, int i2) {
        Logger logger = getInstance();
        if (logger != null) {
            logger.logScreenViewImpl(i, i2);
        }
    }

    public static void logSearchEvent(SearchState searchState) {
        Logger logger = getInstance();
        if (logger != null) {
            logger.logSearchEventImpl(searchState);
        }
    }

    public static void logListEvent(int i, int i2, int i3, int i4, int i5) {
        ListEvent listEvent = new ListEvent();
        listEvent.actionType = i;
        listEvent.listType = i2;
        listEvent.count = i3;
        listEvent.clickedIndex = i4;
        listEvent.numSelected = i5;
        Logger logger = getInstance();
        if (logger != null) {
            logger.logListEventImpl(listEvent);
        }
    }

    public static void logQuickContactEvent(String str, int i, int i2, int i3, String str2) {
        Logger logger = getInstance();
        if (logger != null) {
            QuickContactEvent quickContactEvent = new QuickContactEvent();
            if (str == null) {
                str = "Unknown";
            }
            quickContactEvent.referrer = str;
            quickContactEvent.contactType = i;
            quickContactEvent.cardType = i2;
            quickContactEvent.actionType = i3;
            if (str2 == null) {
                str2 = "";
            }
            quickContactEvent.thirdPartyAction = str2;
            logger.logQuickContactEventImpl(quickContactEvent);
        }
    }

    public static void logEditorEvent(int i, int i2) {
        Logger logger = getInstance();
        if (logger != null) {
            EditorEvent editorEvent = new EditorEvent();
            editorEvent.eventType = i;
            editorEvent.numberRawContacts = i2;
            logger.logEditorEventImpl(editorEvent);
        }
    }
}
