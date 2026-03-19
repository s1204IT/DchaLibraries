package android.view.textservice;

import android.content.Context;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.view.textservice.SpellCheckerSession;
import com.android.internal.textservice.ITextServicesManager;
import java.util.Locale;

public final class TextServicesManager {
    private static final boolean DBG = false;
    public static final boolean DISABLE_PER_PROFILE_SPELL_CHECKER = true;
    private static final String TAG = TextServicesManager.class.getSimpleName();
    private static TextServicesManager sInstance;
    private final ITextServicesManager mService = ITextServicesManager.Stub.asInterface(ServiceManager.getServiceOrThrow(Context.TEXT_SERVICES_MANAGER_SERVICE));

    private TextServicesManager() throws ServiceManager.ServiceNotFoundException {
    }

    public static TextServicesManager getInstance() {
        TextServicesManager textServicesManager;
        synchronized (TextServicesManager.class) {
            if (sInstance == null) {
                try {
                    sInstance = new TextServicesManager();
                } catch (ServiceManager.ServiceNotFoundException e) {
                    throw new IllegalStateException(e);
                }
            }
            textServicesManager = sInstance;
        }
        return textServicesManager;
    }

    private static String parseLanguageFromLocaleString(String str) {
        int iIndexOf = str.indexOf(95);
        if (iIndexOf < 0) {
            return str;
        }
        return str.substring(0, iIndexOf);
    }

    public SpellCheckerSession newSpellCheckerSession(Bundle bundle, Locale locale, SpellCheckerSession.SpellCheckerSessionListener spellCheckerSessionListener, boolean z) {
        SpellCheckerSubtype currentSpellCheckerSubtype;
        if (spellCheckerSessionListener == null) {
            throw new NullPointerException();
        }
        if (!z && locale == null) {
            throw new IllegalArgumentException("Locale should not be null if you don't refer settings.");
        }
        if (!z || isSpellCheckerEnabled()) {
            try {
                SpellCheckerInfo currentSpellChecker = this.mService.getCurrentSpellChecker(null);
                if (currentSpellChecker == null) {
                    return null;
                }
                if (z) {
                    currentSpellCheckerSubtype = getCurrentSpellCheckerSubtype(true);
                    if (currentSpellCheckerSubtype == null) {
                        return null;
                    }
                    if (locale != null) {
                        String languageFromLocaleString = parseLanguageFromLocaleString(currentSpellCheckerSubtype.getLocale());
                        if (languageFromLocaleString.length() < 2 || !locale.getLanguage().equals(languageFromLocaleString)) {
                            return null;
                        }
                    }
                } else {
                    String string = locale.toString();
                    int i = 0;
                    SpellCheckerSubtype spellCheckerSubtype = null;
                    while (true) {
                        if (i < currentSpellChecker.getSubtypeCount()) {
                            SpellCheckerSubtype subtypeAt = currentSpellChecker.getSubtypeAt(i);
                            String locale2 = subtypeAt.getLocale();
                            String languageFromLocaleString2 = parseLanguageFromLocaleString(locale2);
                            if (!locale2.equals(string)) {
                                if (languageFromLocaleString2.length() >= 2 && locale.getLanguage().equals(languageFromLocaleString2)) {
                                    spellCheckerSubtype = subtypeAt;
                                }
                                i++;
                            } else {
                                currentSpellCheckerSubtype = subtypeAt;
                                break;
                            }
                        } else {
                            currentSpellCheckerSubtype = spellCheckerSubtype;
                            break;
                        }
                    }
                }
                if (currentSpellCheckerSubtype == null) {
                    return null;
                }
                SpellCheckerSession spellCheckerSession = new SpellCheckerSession(currentSpellChecker, this.mService, spellCheckerSessionListener);
                try {
                    this.mService.getSpellCheckerService(currentSpellChecker.getId(), currentSpellCheckerSubtype.getLocale(), spellCheckerSession.getTextServicesSessionListener(), spellCheckerSession.getSpellCheckerSessionListener(), bundle);
                    return spellCheckerSession;
                } catch (RemoteException e) {
                    throw e.rethrowFromSystemServer();
                }
            } catch (RemoteException e2) {
                return null;
            }
        }
        return null;
    }

    public SpellCheckerInfo[] getEnabledSpellCheckers() {
        try {
            return this.mService.getEnabledSpellCheckers();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public SpellCheckerInfo getCurrentSpellChecker() {
        try {
            return this.mService.getCurrentSpellChecker(null);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public SpellCheckerSubtype getCurrentSpellCheckerSubtype(boolean z) {
        try {
            return this.mService.getCurrentSpellCheckerSubtype(null, z);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean isSpellCheckerEnabled() {
        try {
            return this.mService.isSpellCheckerEnabled();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }
}
