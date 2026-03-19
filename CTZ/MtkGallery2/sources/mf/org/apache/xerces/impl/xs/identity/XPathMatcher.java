package mf.org.apache.xerces.impl.xs.identity;

import mf.org.apache.xerces.impl.Constants;
import mf.org.apache.xerces.impl.xpath.XPath;
import mf.org.apache.xerces.util.IntStack;
import mf.org.apache.xerces.xni.QName;
import mf.org.apache.xerces.xni.XMLAttributes;
import mf.org.apache.xerces.xs.AttributePSVI;
import mf.org.apache.xerces.xs.ShortList;
import mf.org.apache.xerces.xs.XSTypeDefinition;

public class XPathMatcher {
    protected static final boolean DEBUG_ALL = false;
    protected static final boolean DEBUG_ANY = false;
    protected static final boolean DEBUG_MATCH = false;
    protected static final boolean DEBUG_METHODS = false;
    protected static final boolean DEBUG_METHODS2 = false;
    protected static final boolean DEBUG_METHODS3 = false;
    protected static final boolean DEBUG_STACK = false;
    protected static final int MATCHED = 1;
    protected static final int MATCHED_ATTRIBUTE = 3;
    protected static final int MATCHED_DESCENDANT = 5;
    protected static final int MATCHED_DESCENDANT_PREVIOUS = 13;
    private final int[] fCurrentStep;
    private final XPath.LocationPath[] fLocationPaths;
    private final int[] fMatched;
    protected Object fMatchedString;
    private final int[] fNoMatchDepth;
    final QName fQName = new QName();
    private final IntStack[] fStepIndexes;

    public XPathMatcher(XPath xpath) {
        this.fLocationPaths = xpath.getLocationPaths();
        this.fStepIndexes = new IntStack[this.fLocationPaths.length];
        for (int i = 0; i < this.fStepIndexes.length; i++) {
            this.fStepIndexes[i] = new IntStack();
        }
        this.fCurrentStep = new int[this.fLocationPaths.length];
        this.fNoMatchDepth = new int[this.fLocationPaths.length];
        this.fMatched = new int[this.fLocationPaths.length];
    }

    public boolean isMatched() {
        for (int i = 0; i < this.fLocationPaths.length; i++) {
            if ((this.fMatched[i] & 1) == 1 && (this.fMatched[i] & 13) != 13 && (this.fNoMatchDepth[i] == 0 || (this.fMatched[i] & 5) == 5)) {
                return true;
            }
        }
        return false;
    }

    protected void handleContent(XSTypeDefinition type, boolean nillable, Object value, short valueType, ShortList itemValueType) {
    }

    protected void matched(Object actualValue, short valueType, ShortList itemValueType, boolean isNil) {
    }

    public void startDocumentFragment() {
        this.fMatchedString = null;
        for (int i = 0; i < this.fLocationPaths.length; i++) {
            this.fStepIndexes[i].clear();
            this.fCurrentStep[i] = 0;
            this.fNoMatchDepth[i] = 0;
            this.fMatched[i] = 0;
        }
    }

    public void startElement(QName element, XMLAttributes attributes) {
        XMLAttributes xMLAttributes = attributes;
        int i = 0;
        while (i < this.fLocationPaths.length) {
            int startStep = this.fCurrentStep[i];
            this.fStepIndexes[i].push(startStep);
            int i2 = 1;
            if ((this.fMatched[i] & 5) == 1 || this.fNoMatchDepth[i] > 0) {
                int[] iArr = this.fNoMatchDepth;
                iArr[i] = iArr[i] + 1;
            } else {
                if ((this.fMatched[i] & 5) == 5) {
                    this.fMatched[i] = 13;
                }
                XPath.Step[] steps = this.fLocationPaths[i].steps;
                while (this.fCurrentStep[i] < steps.length && steps[this.fCurrentStep[i]].axis.type == 3) {
                    int[] iArr2 = this.fCurrentStep;
                    iArr2[i] = iArr2[i] + 1;
                }
                if (this.fCurrentStep[i] == steps.length) {
                    this.fMatched[i] = 1;
                } else {
                    int descendantStep = this.fCurrentStep[i];
                    while (this.fCurrentStep[i] < steps.length && steps[this.fCurrentStep[i]].axis.type == 4) {
                        int[] iArr3 = this.fCurrentStep;
                        iArr3[i] = iArr3[i] + 1;
                    }
                    boolean sawDescendant = this.fCurrentStep[i] > descendantStep;
                    if (this.fCurrentStep[i] == steps.length) {
                        int[] iArr4 = this.fNoMatchDepth;
                        iArr4[i] = iArr4[i] + 1;
                    } else {
                        if ((this.fCurrentStep[i] == startStep || this.fCurrentStep[i] > descendantStep) && steps[this.fCurrentStep[i]].axis.type == 1) {
                            XPath.Step step = steps[this.fCurrentStep[i]];
                            XPath.NodeTest nodeTest = step.nodeTest;
                            if (!matches(nodeTest, element)) {
                                if (this.fCurrentStep[i] > descendantStep) {
                                    this.fCurrentStep[i] = descendantStep;
                                } else {
                                    int[] iArr5 = this.fNoMatchDepth;
                                    iArr5[i] = iArr5[i] + 1;
                                }
                            } else {
                                int[] iArr6 = this.fCurrentStep;
                                iArr6[i] = iArr6[i] + 1;
                            }
                        }
                        if (this.fCurrentStep[i] == steps.length) {
                            if (sawDescendant) {
                                this.fCurrentStep[i] = descendantStep;
                                this.fMatched[i] = 5;
                            } else {
                                this.fMatched[i] = 1;
                            }
                        } else if (this.fCurrentStep[i] < steps.length && steps[this.fCurrentStep[i]].axis.type == 2) {
                            int attrCount = attributes.getLength();
                            if (attrCount > 0) {
                                XPath.NodeTest nodeTest2 = steps[this.fCurrentStep[i]].nodeTest;
                                int aIndex = 0;
                                while (true) {
                                    if (aIndex >= attrCount) {
                                        break;
                                    }
                                    xMLAttributes.getName(aIndex, this.fQName);
                                    if (!matches(nodeTest2, this.fQName)) {
                                        aIndex++;
                                        xMLAttributes = attributes;
                                        i2 = 1;
                                    } else {
                                        int[] iArr7 = this.fCurrentStep;
                                        iArr7[i] = iArr7[i] + i2;
                                        if (this.fCurrentStep[i] == steps.length) {
                                            this.fMatched[i] = 3;
                                            int j = 0;
                                            while (j < i && (this.fMatched[j] & i2) != i2) {
                                                j++;
                                            }
                                            if (j == i) {
                                                AttributePSVI attrPSVI = (AttributePSVI) xMLAttributes.getAugmentations(aIndex).getItem(Constants.ATTRIBUTE_PSVI);
                                                this.fMatchedString = attrPSVI.getActualNormalizedValue();
                                                matched(this.fMatchedString, attrPSVI.getActualNormalizedValueType(), attrPSVI.getItemValueTypes(), false);
                                            }
                                        }
                                    }
                                }
                            }
                            if ((this.fMatched[i] & 1) != 1) {
                                if (this.fCurrentStep[i] > descendantStep) {
                                    this.fCurrentStep[i] = descendantStep;
                                } else {
                                    int[] iArr8 = this.fNoMatchDepth;
                                    iArr8[i] = iArr8[i] + 1;
                                }
                            }
                        }
                    }
                }
            }
            i++;
            xMLAttributes = attributes;
        }
    }

    public void endElement(QName element, XSTypeDefinition type, boolean nillable, Object value, short valueType, ShortList itemValueType) {
        for (int i = 0; i < this.fLocationPaths.length; i++) {
            this.fCurrentStep[i] = this.fStepIndexes[i].pop();
            if (this.fNoMatchDepth[i] > 0) {
                int[] iArr = this.fNoMatchDepth;
                iArr[i] = iArr[i] - 1;
            } else {
                int j = 0;
                while (j < i && (this.fMatched[j] & 1) != 1) {
                    j++;
                }
                if (j >= i && this.fMatched[j] != 0) {
                    if ((this.fMatched[j] & 3) == 3) {
                        this.fMatched[i] = 0;
                    } else {
                        handleContent(type, nillable, value, valueType, itemValueType);
                        this.fMatched[i] = 0;
                    }
                }
            }
        }
    }

    public String toString() {
        StringBuffer str = new StringBuffer();
        String s = super.toString();
        int index2 = s.lastIndexOf(46);
        if (index2 != -1) {
            s = s.substring(index2 + 1);
        }
        str.append(s);
        for (int i = 0; i < this.fLocationPaths.length; i++) {
            str.append('[');
            XPath.Step[] steps = this.fLocationPaths[i].steps;
            for (int j = 0; j < steps.length; j++) {
                if (j == this.fCurrentStep[i]) {
                    str.append('^');
                }
                str.append(steps[j].toString());
                if (j < steps.length - 1) {
                    str.append('/');
                }
            }
            if (this.fCurrentStep[i] == steps.length) {
                str.append('^');
            }
            str.append(']');
            str.append(',');
        }
        return str.toString();
    }

    private String normalize(String s) {
        StringBuffer str = new StringBuffer();
        int length = s.length();
        for (int i = 0; i < length; i++) {
            char c = s.charAt(i);
            if (c == '\n') {
                str.append("\\n");
            } else {
                str.append(c);
            }
        }
        return str.toString();
    }

    private static boolean matches(XPath.NodeTest nodeTest, QName value) {
        if (nodeTest.type == 1) {
            return nodeTest.name.equals(value);
        }
        return nodeTest.type != 4 || nodeTest.name.uri == value.uri;
    }
}
