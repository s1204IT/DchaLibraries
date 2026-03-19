package android.preference;

import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;
import com.android.internal.util.XmlUtils;
import java.io.IOException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

class PreferenceInflater extends GenericInflater<Preference, PreferenceGroup> {
    private static final String EXTRA_TAG_NAME = "extra";
    private static final String INTENT_TAG_NAME = "intent";
    private static final String TAG = "PreferenceInflater";
    private PreferenceManager mPreferenceManager;

    public PreferenceInflater(Context context, PreferenceManager preferenceManager) {
        super(context);
        init(preferenceManager);
    }

    PreferenceInflater(GenericInflater<Preference, PreferenceGroup> genericInflater, PreferenceManager preferenceManager, Context context) {
        super(genericInflater, context);
        init(preferenceManager);
    }

    @Override
    public GenericInflater<Preference, PreferenceGroup> cloneInContext(Context context) {
        return new PreferenceInflater(this, this.mPreferenceManager, context);
    }

    private void init(PreferenceManager preferenceManager) {
        this.mPreferenceManager = preferenceManager;
        setDefaultPackage("android.preference.");
    }

    @Override
    protected boolean onCreateCustomFromTag(XmlPullParser xmlPullParser, Preference preference, AttributeSet attributeSet) throws XmlPullParserException {
        String name = xmlPullParser.getName();
        if (name.equals("intent")) {
            try {
                Intent intent = Intent.parseIntent(getContext().getResources(), xmlPullParser, attributeSet);
                if (intent != null) {
                    preference.setIntent(intent);
                }
                return true;
            } catch (IOException e) {
                XmlPullParserException xmlPullParserException = new XmlPullParserException("Error parsing preference");
                xmlPullParserException.initCause(e);
                throw xmlPullParserException;
            }
        }
        if (name.equals(EXTRA_TAG_NAME)) {
            getContext().getResources().parseBundleExtra(EXTRA_TAG_NAME, attributeSet, preference.getExtras());
            try {
                XmlUtils.skipCurrentTag(xmlPullParser);
                return true;
            } catch (IOException e2) {
                XmlPullParserException xmlPullParserException2 = new XmlPullParserException("Error parsing preference");
                xmlPullParserException2.initCause(e2);
                throw xmlPullParserException2;
            }
        }
        return false;
    }

    @Override
    protected PreferenceGroup onMergeRoots(PreferenceGroup preferenceGroup, boolean z, PreferenceGroup preferenceGroup2) {
        if (preferenceGroup == null) {
            preferenceGroup2.onAttachedToHierarchy(this.mPreferenceManager);
            return preferenceGroup2;
        }
        return preferenceGroup;
    }
}
