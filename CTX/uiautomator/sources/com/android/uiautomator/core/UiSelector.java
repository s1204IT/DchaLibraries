package com.android.uiautomator.core;

import android.util.SparseArray;
import android.view.accessibility.AccessibilityNodeInfo;
import java.util.regex.Pattern;

@Deprecated
public class UiSelector {
    static final int SELECTOR_CHECKABLE = 30;
    static final int SELECTOR_CHECKED = 15;
    static final int SELECTOR_CHILD = 19;
    static final int SELECTOR_CLASS = 4;
    static final int SELECTOR_CLASS_REGEX = 26;
    static final int SELECTOR_CLICKABLE = 14;
    static final int SELECTOR_CONTAINER = 20;
    static final int SELECTOR_CONTAINS_DESCRIPTION = 7;
    static final int SELECTOR_CONTAINS_TEXT = 3;
    static final int SELECTOR_COUNT = 23;
    static final int SELECTOR_DESCRIPTION = 5;
    static final int SELECTOR_DESCRIPTION_REGEX = 27;
    static final int SELECTOR_ENABLED = 10;
    static final int SELECTOR_FOCUSABLE = 12;
    static final int SELECTOR_FOCUSED = 11;
    static final int SELECTOR_ID = 17;
    static final int SELECTOR_INDEX = 8;
    static final int SELECTOR_INSTANCE = 9;
    static final int SELECTOR_LONG_CLICKABLE = 24;
    static final int SELECTOR_NIL = 0;
    static final int SELECTOR_PACKAGE_NAME = 18;
    static final int SELECTOR_PACKAGE_NAME_REGEX = 28;
    static final int SELECTOR_PARENT = 22;
    static final int SELECTOR_PATTERN = 21;
    static final int SELECTOR_RESOURCE_ID = 29;
    static final int SELECTOR_RESOURCE_ID_REGEX = 31;
    static final int SELECTOR_SCROLLABLE = 13;
    static final int SELECTOR_SELECTED = 16;
    static final int SELECTOR_START_DESCRIPTION = 6;
    static final int SELECTOR_START_TEXT = 2;
    static final int SELECTOR_TEXT = 1;
    static final int SELECTOR_TEXT_REGEX = 25;
    private SparseArray<Object> mSelectorAttributes;

    public UiSelector() {
        this.mSelectorAttributes = new SparseArray<>();
    }

    UiSelector(UiSelector uiSelector) {
        this.mSelectorAttributes = new SparseArray<>();
        this.mSelectorAttributes = uiSelector.cloneSelector().mSelectorAttributes;
    }

    protected UiSelector cloneSelector() {
        UiSelector uiSelector = new UiSelector();
        uiSelector.mSelectorAttributes = this.mSelectorAttributes.clone();
        if (hasChildSelector()) {
            uiSelector.mSelectorAttributes.put(SELECTOR_CHILD, new UiSelector(getChildSelector()));
        }
        if (hasParentSelector()) {
            uiSelector.mSelectorAttributes.put(SELECTOR_PARENT, new UiSelector(getParentSelector()));
        }
        if (hasPatternSelector()) {
            uiSelector.mSelectorAttributes.put(SELECTOR_PATTERN, new UiSelector(getPatternSelector()));
        }
        return uiSelector;
    }

    static UiSelector patternBuilder(UiSelector uiSelector) {
        if (!uiSelector.hasPatternSelector()) {
            return new UiSelector().patternSelector(uiSelector);
        }
        return uiSelector;
    }

    static UiSelector patternBuilder(UiSelector uiSelector, UiSelector uiSelector2) {
        return new UiSelector(new UiSelector().containerSelector(uiSelector).patternSelector(uiSelector2));
    }

    public UiSelector text(String str) {
        return buildSelector(1, str);
    }

    public UiSelector textMatches(String str) {
        return buildSelector(SELECTOR_TEXT_REGEX, Pattern.compile(str));
    }

    public UiSelector textStartsWith(String str) {
        return buildSelector(2, str);
    }

    public UiSelector textContains(String str) {
        return buildSelector(SELECTOR_CONTAINS_TEXT, str);
    }

    public UiSelector className(String str) {
        return buildSelector(SELECTOR_CLASS, str);
    }

    public UiSelector classNameMatches(String str) {
        return buildSelector(SELECTOR_CLASS_REGEX, Pattern.compile(str));
    }

    public <T> UiSelector className(Class<T> cls) {
        return buildSelector(SELECTOR_CLASS, cls.getName());
    }

    public UiSelector description(String str) {
        return buildSelector(SELECTOR_DESCRIPTION, str);
    }

    public UiSelector descriptionMatches(String str) {
        return buildSelector(SELECTOR_DESCRIPTION_REGEX, Pattern.compile(str));
    }

    public UiSelector descriptionStartsWith(String str) {
        return buildSelector(SELECTOR_START_DESCRIPTION, str);
    }

    public UiSelector descriptionContains(String str) {
        return buildSelector(SELECTOR_CONTAINS_DESCRIPTION, str);
    }

    public UiSelector resourceId(String str) {
        return buildSelector(SELECTOR_RESOURCE_ID, str);
    }

    public UiSelector resourceIdMatches(String str) {
        return buildSelector(SELECTOR_RESOURCE_ID_REGEX, Pattern.compile(str));
    }

    public UiSelector index(int i) {
        return buildSelector(SELECTOR_INDEX, Integer.valueOf(i));
    }

    public UiSelector instance(int i) {
        return buildSelector(SELECTOR_INSTANCE, Integer.valueOf(i));
    }

    public UiSelector enabled(boolean z) {
        return buildSelector(SELECTOR_ENABLED, Boolean.valueOf(z));
    }

    public UiSelector focused(boolean z) {
        return buildSelector(SELECTOR_FOCUSED, Boolean.valueOf(z));
    }

    public UiSelector focusable(boolean z) {
        return buildSelector(SELECTOR_FOCUSABLE, Boolean.valueOf(z));
    }

    public UiSelector scrollable(boolean z) {
        return buildSelector(SELECTOR_SCROLLABLE, Boolean.valueOf(z));
    }

    public UiSelector selected(boolean z) {
        return buildSelector(SELECTOR_SELECTED, Boolean.valueOf(z));
    }

    public UiSelector checked(boolean z) {
        return buildSelector(SELECTOR_CHECKED, Boolean.valueOf(z));
    }

    public UiSelector clickable(boolean z) {
        return buildSelector(SELECTOR_CLICKABLE, Boolean.valueOf(z));
    }

    public UiSelector checkable(boolean z) {
        return buildSelector(SELECTOR_CHECKABLE, Boolean.valueOf(z));
    }

    public UiSelector longClickable(boolean z) {
        return buildSelector(SELECTOR_LONG_CLICKABLE, Boolean.valueOf(z));
    }

    public UiSelector childSelector(UiSelector uiSelector) {
        return buildSelector(SELECTOR_CHILD, uiSelector);
    }

    private UiSelector patternSelector(UiSelector uiSelector) {
        return buildSelector(SELECTOR_PATTERN, uiSelector);
    }

    private UiSelector containerSelector(UiSelector uiSelector) {
        return buildSelector(SELECTOR_CONTAINER, uiSelector);
    }

    public UiSelector fromParent(UiSelector uiSelector) {
        return buildSelector(SELECTOR_PARENT, uiSelector);
    }

    public UiSelector packageName(String str) {
        return buildSelector(SELECTOR_PACKAGE_NAME, str);
    }

    public UiSelector packageNameMatches(String str) {
        return buildSelector(SELECTOR_PACKAGE_NAME_REGEX, Pattern.compile(str));
    }

    private UiSelector buildSelector(int i, Object obj) {
        UiSelector uiSelector = new UiSelector(this);
        if (i == SELECTOR_CHILD || i == SELECTOR_PARENT) {
            uiSelector.getLastSubSelector().mSelectorAttributes.put(i, obj);
        } else {
            uiSelector.mSelectorAttributes.put(i, obj);
        }
        return uiSelector;
    }

    UiSelector getChildSelector() {
        UiSelector uiSelector = (UiSelector) this.mSelectorAttributes.get(SELECTOR_CHILD, null);
        if (uiSelector != null) {
            return new UiSelector(uiSelector);
        }
        return null;
    }

    UiSelector getPatternSelector() {
        UiSelector uiSelector = (UiSelector) this.mSelectorAttributes.get(SELECTOR_PATTERN, null);
        if (uiSelector == null) {
            return null;
        }
        return new UiSelector(uiSelector);
    }

    UiSelector getContainerSelector() {
        UiSelector uiSelector = (UiSelector) this.mSelectorAttributes.get(SELECTOR_CONTAINER, null);
        if (uiSelector == null) {
            return null;
        }
        return new UiSelector(uiSelector);
    }

    UiSelector getParentSelector() {
        UiSelector uiSelector = (UiSelector) this.mSelectorAttributes.get(SELECTOR_PARENT, null);
        if (uiSelector == null) {
            return null;
        }
        return new UiSelector(uiSelector);
    }

    int getInstance() {
        return getInt(SELECTOR_INSTANCE);
    }

    String getString(int i) {
        return (String) this.mSelectorAttributes.get(i, null);
    }

    boolean getBoolean(int i) {
        return ((Boolean) this.mSelectorAttributes.get(i, false)).booleanValue();
    }

    int getInt(int i) {
        return ((Integer) this.mSelectorAttributes.get(i, Integer.valueOf(SELECTOR_NIL))).intValue();
    }

    Pattern getPattern(int i) {
        return (Pattern) this.mSelectorAttributes.get(i, null);
    }

    boolean isMatchFor(AccessibilityNodeInfo accessibilityNodeInfo, int i) {
        int size = this.mSelectorAttributes.size();
        for (int i2 = SELECTOR_NIL; i2 < size; i2++) {
            int iKeyAt = this.mSelectorAttributes.keyAt(i2);
            switch (iKeyAt) {
                case 1:
                    CharSequence text = accessibilityNodeInfo.getText();
                    if (text == null || !text.toString().contentEquals(getString(iKeyAt))) {
                        return false;
                    }
                    break;
                    break;
                case 2:
                    CharSequence text2 = accessibilityNodeInfo.getText();
                    if (text2 == null || !text2.toString().toLowerCase().startsWith(getString(iKeyAt).toLowerCase())) {
                        return false;
                    }
                    break;
                    break;
                case SELECTOR_CONTAINS_TEXT:
                    CharSequence text3 = accessibilityNodeInfo.getText();
                    if (text3 == null || !text3.toString().toLowerCase().contains(getString(iKeyAt).toLowerCase())) {
                        return false;
                    }
                    break;
                    break;
                case SELECTOR_CLASS:
                    CharSequence className = accessibilityNodeInfo.getClassName();
                    if (className == null || !className.toString().contentEquals(getString(iKeyAt))) {
                        return false;
                    }
                    break;
                    break;
                case SELECTOR_DESCRIPTION:
                    CharSequence contentDescription = accessibilityNodeInfo.getContentDescription();
                    if (contentDescription == null || !contentDescription.toString().contentEquals(getString(iKeyAt))) {
                        return false;
                    }
                    break;
                    break;
                case SELECTOR_START_DESCRIPTION:
                    CharSequence contentDescription2 = accessibilityNodeInfo.getContentDescription();
                    if (contentDescription2 == null || !contentDescription2.toString().toLowerCase().startsWith(getString(iKeyAt).toLowerCase())) {
                        return false;
                    }
                    break;
                    break;
                case SELECTOR_CONTAINS_DESCRIPTION:
                    CharSequence contentDescription3 = accessibilityNodeInfo.getContentDescription();
                    if (contentDescription3 == null || !contentDescription3.toString().toLowerCase().contains(getString(iKeyAt).toLowerCase())) {
                        return false;
                    }
                    break;
                    break;
                case SELECTOR_INDEX:
                    if (i != getInt(iKeyAt)) {
                        return false;
                    }
                    break;
                    break;
                case SELECTOR_ENABLED:
                    if (accessibilityNodeInfo.isEnabled() != getBoolean(iKeyAt)) {
                        return false;
                    }
                    break;
                    break;
                case SELECTOR_FOCUSED:
                    if (accessibilityNodeInfo.isFocused() != getBoolean(iKeyAt)) {
                        return false;
                    }
                    break;
                    break;
                case SELECTOR_FOCUSABLE:
                    if (accessibilityNodeInfo.isFocusable() != getBoolean(iKeyAt)) {
                        return false;
                    }
                    break;
                    break;
                case SELECTOR_SCROLLABLE:
                    if (accessibilityNodeInfo.isScrollable() != getBoolean(iKeyAt)) {
                        return false;
                    }
                    break;
                    break;
                case SELECTOR_CLICKABLE:
                    if (accessibilityNodeInfo.isClickable() != getBoolean(iKeyAt)) {
                        return false;
                    }
                    break;
                    break;
                case SELECTOR_CHECKED:
                    if (accessibilityNodeInfo.isChecked() != getBoolean(iKeyAt)) {
                        return false;
                    }
                    break;
                    break;
                case SELECTOR_SELECTED:
                    if (accessibilityNodeInfo.isSelected() != getBoolean(iKeyAt)) {
                        return false;
                    }
                    break;
                    break;
                case SELECTOR_PACKAGE_NAME:
                    CharSequence packageName = accessibilityNodeInfo.getPackageName();
                    if (packageName == null || !packageName.toString().contentEquals(getString(iKeyAt))) {
                        return false;
                    }
                    break;
                    break;
                case SELECTOR_LONG_CLICKABLE:
                    if (accessibilityNodeInfo.isLongClickable() != getBoolean(iKeyAt)) {
                        return false;
                    }
                    break;
                    break;
                case SELECTOR_TEXT_REGEX:
                    CharSequence text4 = accessibilityNodeInfo.getText();
                    if (text4 == null || !getPattern(iKeyAt).matcher(text4).matches()) {
                        return false;
                    }
                    break;
                    break;
                case SELECTOR_CLASS_REGEX:
                    CharSequence className2 = accessibilityNodeInfo.getClassName();
                    if (className2 == null || !getPattern(iKeyAt).matcher(className2).matches()) {
                        return false;
                    }
                    break;
                    break;
                case SELECTOR_DESCRIPTION_REGEX:
                    CharSequence contentDescription4 = accessibilityNodeInfo.getContentDescription();
                    if (contentDescription4 == null || !getPattern(iKeyAt).matcher(contentDescription4).matches()) {
                        return false;
                    }
                    break;
                    break;
                case SELECTOR_PACKAGE_NAME_REGEX:
                    CharSequence packageName2 = accessibilityNodeInfo.getPackageName();
                    if (packageName2 == null || !getPattern(iKeyAt).matcher(packageName2).matches()) {
                        return false;
                    }
                    break;
                    break;
                case SELECTOR_RESOURCE_ID:
                    String viewIdResourceName = accessibilityNodeInfo.getViewIdResourceName();
                    if (viewIdResourceName == null || !viewIdResourceName.toString().contentEquals(getString(iKeyAt))) {
                        return false;
                    }
                    break;
                    break;
                case SELECTOR_CHECKABLE:
                    if (accessibilityNodeInfo.isCheckable() != getBoolean(iKeyAt)) {
                        return false;
                    }
                    break;
                    break;
                case SELECTOR_RESOURCE_ID_REGEX:
                    String viewIdResourceName2 = accessibilityNodeInfo.getViewIdResourceName();
                    if (viewIdResourceName2 == null || !getPattern(iKeyAt).matcher(viewIdResourceName2).matches()) {
                        return false;
                    }
                    break;
                    break;
            }
        }
        return matchOrUpdateInstance();
    }

    private boolean matchOrUpdateInstance() {
        int iIntValue;
        int iIntValue2;
        if (this.mSelectorAttributes.indexOfKey(SELECTOR_INSTANCE) >= 0) {
            iIntValue = ((Integer) this.mSelectorAttributes.get(SELECTOR_INSTANCE)).intValue();
        } else {
            iIntValue = SELECTOR_NIL;
        }
        if (this.mSelectorAttributes.indexOfKey(SELECTOR_COUNT) >= 0) {
            iIntValue2 = ((Integer) this.mSelectorAttributes.get(SELECTOR_COUNT)).intValue();
        } else {
            iIntValue2 = SELECTOR_NIL;
        }
        if (iIntValue == iIntValue2) {
            return true;
        }
        if (iIntValue > iIntValue2) {
            this.mSelectorAttributes.put(SELECTOR_COUNT, Integer.valueOf(iIntValue2 + 1));
        }
        return false;
    }

    boolean isLeaf() {
        if (this.mSelectorAttributes.indexOfKey(SELECTOR_CHILD) < 0 && this.mSelectorAttributes.indexOfKey(SELECTOR_PARENT) < 0) {
            return true;
        }
        return false;
    }

    boolean hasChildSelector() {
        if (this.mSelectorAttributes.indexOfKey(SELECTOR_CHILD) < 0) {
            return false;
        }
        return true;
    }

    boolean hasPatternSelector() {
        if (this.mSelectorAttributes.indexOfKey(SELECTOR_PATTERN) < 0) {
            return false;
        }
        return true;
    }

    boolean hasContainerSelector() {
        if (this.mSelectorAttributes.indexOfKey(SELECTOR_CONTAINER) < 0) {
            return false;
        }
        return true;
    }

    boolean hasParentSelector() {
        if (this.mSelectorAttributes.indexOfKey(SELECTOR_PARENT) < 0) {
            return false;
        }
        return true;
    }

    private UiSelector getLastSubSelector() {
        if (this.mSelectorAttributes.indexOfKey(SELECTOR_CHILD) >= 0) {
            UiSelector uiSelector = (UiSelector) this.mSelectorAttributes.get(SELECTOR_CHILD);
            if (uiSelector.getLastSubSelector() == null) {
                return uiSelector;
            }
            return uiSelector.getLastSubSelector();
        }
        if (this.mSelectorAttributes.indexOfKey(SELECTOR_PARENT) >= 0) {
            UiSelector uiSelector2 = (UiSelector) this.mSelectorAttributes.get(SELECTOR_PARENT);
            if (uiSelector2.getLastSubSelector() == null) {
                return uiSelector2;
            }
            return uiSelector2.getLastSubSelector();
        }
        return this;
    }

    public String toString() {
        return dumpToString(true);
    }

    String dumpToString(boolean z) {
        StringBuilder sb = new StringBuilder();
        sb.append(UiSelector.class.getSimpleName() + "[");
        int size = this.mSelectorAttributes.size();
        for (int i = SELECTOR_NIL; i < size; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            int iKeyAt = this.mSelectorAttributes.keyAt(i);
            switch (iKeyAt) {
                case 1:
                    sb.append("TEXT=");
                    sb.append(this.mSelectorAttributes.valueAt(i));
                    break;
                case 2:
                    sb.append("START_TEXT=");
                    sb.append(this.mSelectorAttributes.valueAt(i));
                    break;
                case SELECTOR_CONTAINS_TEXT:
                    sb.append("CONTAINS_TEXT=");
                    sb.append(this.mSelectorAttributes.valueAt(i));
                    break;
                case SELECTOR_CLASS:
                    sb.append("CLASS=");
                    sb.append(this.mSelectorAttributes.valueAt(i));
                    break;
                case SELECTOR_DESCRIPTION:
                    sb.append("DESCRIPTION=");
                    sb.append(this.mSelectorAttributes.valueAt(i));
                    break;
                case SELECTOR_START_DESCRIPTION:
                    sb.append("START_DESCRIPTION=");
                    sb.append(this.mSelectorAttributes.valueAt(i));
                    break;
                case SELECTOR_CONTAINS_DESCRIPTION:
                    sb.append("CONTAINS_DESCRIPTION=");
                    sb.append(this.mSelectorAttributes.valueAt(i));
                    break;
                case SELECTOR_INDEX:
                    sb.append("INDEX=");
                    sb.append(this.mSelectorAttributes.valueAt(i));
                    break;
                case SELECTOR_INSTANCE:
                    sb.append("INSTANCE=");
                    sb.append(this.mSelectorAttributes.valueAt(i));
                    break;
                case SELECTOR_ENABLED:
                    sb.append("ENABLED=");
                    sb.append(this.mSelectorAttributes.valueAt(i));
                    break;
                case SELECTOR_FOCUSED:
                    sb.append("FOCUSED=");
                    sb.append(this.mSelectorAttributes.valueAt(i));
                    break;
                case SELECTOR_FOCUSABLE:
                    sb.append("FOCUSABLE=");
                    sb.append(this.mSelectorAttributes.valueAt(i));
                    break;
                case SELECTOR_SCROLLABLE:
                    sb.append("SCROLLABLE=");
                    sb.append(this.mSelectorAttributes.valueAt(i));
                    break;
                case SELECTOR_CLICKABLE:
                    sb.append("CLICKABLE=");
                    sb.append(this.mSelectorAttributes.valueAt(i));
                    break;
                case SELECTOR_CHECKED:
                    sb.append("CHECKED=");
                    sb.append(this.mSelectorAttributes.valueAt(i));
                    break;
                case SELECTOR_SELECTED:
                    sb.append("SELECTED=");
                    sb.append(this.mSelectorAttributes.valueAt(i));
                    break;
                case SELECTOR_ID:
                    sb.append("ID=");
                    sb.append(this.mSelectorAttributes.valueAt(i));
                    break;
                case SELECTOR_PACKAGE_NAME:
                    sb.append("PACKAGE NAME=");
                    sb.append(this.mSelectorAttributes.valueAt(i));
                    break;
                case SELECTOR_CHILD:
                    if (z) {
                        sb.append("CHILD=");
                        sb.append(this.mSelectorAttributes.valueAt(i));
                    } else {
                        sb.append("CHILD[..]");
                    }
                    break;
                case SELECTOR_CONTAINER:
                    if (z) {
                        sb.append("CONTAINER=");
                        sb.append(this.mSelectorAttributes.valueAt(i));
                    } else {
                        sb.append("CONTAINER[..]");
                    }
                    break;
                case SELECTOR_PATTERN:
                    if (z) {
                        sb.append("PATTERN=");
                        sb.append(this.mSelectorAttributes.valueAt(i));
                    } else {
                        sb.append("PATTERN[..]");
                    }
                    break;
                case SELECTOR_PARENT:
                    if (z) {
                        sb.append("PARENT=");
                        sb.append(this.mSelectorAttributes.valueAt(i));
                    } else {
                        sb.append("PARENT[..]");
                    }
                    break;
                case SELECTOR_COUNT:
                    sb.append("COUNT=");
                    sb.append(this.mSelectorAttributes.valueAt(i));
                    break;
                case SELECTOR_LONG_CLICKABLE:
                    sb.append("LONG_CLICKABLE=");
                    sb.append(this.mSelectorAttributes.valueAt(i));
                    break;
                case SELECTOR_TEXT_REGEX:
                    sb.append("TEXT_REGEX=");
                    sb.append(this.mSelectorAttributes.valueAt(i));
                    break;
                case SELECTOR_CLASS_REGEX:
                    sb.append("CLASS_REGEX=");
                    sb.append(this.mSelectorAttributes.valueAt(i));
                    break;
                case SELECTOR_DESCRIPTION_REGEX:
                    sb.append("DESCRIPTION_REGEX=");
                    sb.append(this.mSelectorAttributes.valueAt(i));
                    break;
                case SELECTOR_PACKAGE_NAME_REGEX:
                    sb.append("PACKAGE_NAME_REGEX=");
                    sb.append(this.mSelectorAttributes.valueAt(i));
                    break;
                case SELECTOR_RESOURCE_ID:
                    sb.append("RESOURCE_ID=");
                    sb.append(this.mSelectorAttributes.valueAt(i));
                    break;
                case SELECTOR_CHECKABLE:
                    sb.append("CHECKABLE=");
                    sb.append(this.mSelectorAttributes.valueAt(i));
                    break;
                case SELECTOR_RESOURCE_ID_REGEX:
                    sb.append("RESOURCE_ID_REGEX=");
                    sb.append(this.mSelectorAttributes.valueAt(i));
                    break;
                default:
                    sb.append("UNDEFINED=" + iKeyAt + " ");
                    sb.append(this.mSelectorAttributes.valueAt(i));
                    break;
            }
        }
        sb.append("]");
        return sb.toString();
    }
}
