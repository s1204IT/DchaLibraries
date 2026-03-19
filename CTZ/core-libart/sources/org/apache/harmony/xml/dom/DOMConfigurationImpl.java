package org.apache.harmony.xml.dom;

import android.icu.text.PluralRules;
import java.util.Map;
import java.util.TreeMap;
import org.w3c.dom.DOMConfiguration;
import org.w3c.dom.DOMErrorHandler;
import org.w3c.dom.DOMException;
import org.w3c.dom.DOMStringList;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

public final class DOMConfigurationImpl implements DOMConfiguration {
    private static final Map<String, Parameter> PARAMETERS = new TreeMap(String.CASE_INSENSITIVE_ORDER);
    private DOMErrorHandler errorHandler;
    private String schemaLocation;
    private String schemaType;
    private boolean cdataSections = true;
    private boolean comments = true;
    private boolean datatypeNormalization = false;
    private boolean entities = true;
    private boolean namespaces = true;
    private boolean splitCdataSections = true;
    private boolean validate = false;
    private boolean wellFormed = true;

    interface Parameter {
        boolean canSet(DOMConfigurationImpl dOMConfigurationImpl, Object obj);

        Object get(DOMConfigurationImpl dOMConfigurationImpl);

        void set(DOMConfigurationImpl dOMConfigurationImpl, Object obj);
    }

    static {
        PARAMETERS.put("canonical-form", new FixedParameter(false));
        PARAMETERS.put("cdata-sections", new BooleanParameter() {
            @Override
            public Object get(DOMConfigurationImpl dOMConfigurationImpl) {
                return Boolean.valueOf(dOMConfigurationImpl.cdataSections);
            }

            @Override
            public void set(DOMConfigurationImpl dOMConfigurationImpl, Object obj) {
                dOMConfigurationImpl.cdataSections = ((Boolean) obj).booleanValue();
            }
        });
        PARAMETERS.put("check-character-normalization", new FixedParameter(false));
        PARAMETERS.put("comments", new BooleanParameter() {
            @Override
            public Object get(DOMConfigurationImpl dOMConfigurationImpl) {
                return Boolean.valueOf(dOMConfigurationImpl.comments);
            }

            @Override
            public void set(DOMConfigurationImpl dOMConfigurationImpl, Object obj) {
                dOMConfigurationImpl.comments = ((Boolean) obj).booleanValue();
            }
        });
        PARAMETERS.put("datatype-normalization", new BooleanParameter() {
            @Override
            public Object get(DOMConfigurationImpl dOMConfigurationImpl) {
                return Boolean.valueOf(dOMConfigurationImpl.datatypeNormalization);
            }

            @Override
            public void set(DOMConfigurationImpl dOMConfigurationImpl, Object obj) {
                if (((Boolean) obj).booleanValue()) {
                    dOMConfigurationImpl.datatypeNormalization = true;
                    dOMConfigurationImpl.validate = true;
                } else {
                    dOMConfigurationImpl.datatypeNormalization = false;
                }
            }
        });
        PARAMETERS.put("element-content-whitespace", new FixedParameter(true));
        PARAMETERS.put("entities", new BooleanParameter() {
            @Override
            public Object get(DOMConfigurationImpl dOMConfigurationImpl) {
                return Boolean.valueOf(dOMConfigurationImpl.entities);
            }

            @Override
            public void set(DOMConfigurationImpl dOMConfigurationImpl, Object obj) {
                dOMConfigurationImpl.entities = ((Boolean) obj).booleanValue();
            }
        });
        PARAMETERS.put("error-handler", new Parameter() {
            @Override
            public Object get(DOMConfigurationImpl dOMConfigurationImpl) {
                return dOMConfigurationImpl.errorHandler;
            }

            @Override
            public void set(DOMConfigurationImpl dOMConfigurationImpl, Object obj) {
                dOMConfigurationImpl.errorHandler = (DOMErrorHandler) obj;
            }

            @Override
            public boolean canSet(DOMConfigurationImpl dOMConfigurationImpl, Object obj) {
                return obj == null || (obj instanceof DOMErrorHandler);
            }
        });
        PARAMETERS.put("infoset", new BooleanParameter() {
            @Override
            public Object get(DOMConfigurationImpl dOMConfigurationImpl) {
                return Boolean.valueOf(!dOMConfigurationImpl.entities && !dOMConfigurationImpl.datatypeNormalization && !dOMConfigurationImpl.cdataSections && dOMConfigurationImpl.wellFormed && dOMConfigurationImpl.comments && dOMConfigurationImpl.namespaces);
            }

            @Override
            public void set(DOMConfigurationImpl dOMConfigurationImpl, Object obj) {
                if (((Boolean) obj).booleanValue()) {
                    dOMConfigurationImpl.entities = false;
                    dOMConfigurationImpl.datatypeNormalization = false;
                    dOMConfigurationImpl.cdataSections = false;
                    dOMConfigurationImpl.wellFormed = true;
                    dOMConfigurationImpl.comments = true;
                    dOMConfigurationImpl.namespaces = true;
                }
            }
        });
        PARAMETERS.put("namespaces", new BooleanParameter() {
            @Override
            public Object get(DOMConfigurationImpl dOMConfigurationImpl) {
                return Boolean.valueOf(dOMConfigurationImpl.namespaces);
            }

            @Override
            public void set(DOMConfigurationImpl dOMConfigurationImpl, Object obj) {
                dOMConfigurationImpl.namespaces = ((Boolean) obj).booleanValue();
            }
        });
        PARAMETERS.put("namespace-declarations", new FixedParameter(true));
        PARAMETERS.put("normalize-characters", new FixedParameter(false));
        PARAMETERS.put("schema-location", new Parameter() {
            @Override
            public Object get(DOMConfigurationImpl dOMConfigurationImpl) {
                return dOMConfigurationImpl.schemaLocation;
            }

            @Override
            public void set(DOMConfigurationImpl dOMConfigurationImpl, Object obj) {
                dOMConfigurationImpl.schemaLocation = (String) obj;
            }

            @Override
            public boolean canSet(DOMConfigurationImpl dOMConfigurationImpl, Object obj) {
                return obj == null || (obj instanceof String);
            }
        });
        PARAMETERS.put("schema-type", new Parameter() {
            @Override
            public Object get(DOMConfigurationImpl dOMConfigurationImpl) {
                return dOMConfigurationImpl.schemaType;
            }

            @Override
            public void set(DOMConfigurationImpl dOMConfigurationImpl, Object obj) {
                dOMConfigurationImpl.schemaType = (String) obj;
            }

            @Override
            public boolean canSet(DOMConfigurationImpl dOMConfigurationImpl, Object obj) {
                return obj == null || (obj instanceof String);
            }
        });
        PARAMETERS.put("split-cdata-sections", new BooleanParameter() {
            @Override
            public Object get(DOMConfigurationImpl dOMConfigurationImpl) {
                return Boolean.valueOf(dOMConfigurationImpl.splitCdataSections);
            }

            @Override
            public void set(DOMConfigurationImpl dOMConfigurationImpl, Object obj) {
                dOMConfigurationImpl.splitCdataSections = ((Boolean) obj).booleanValue();
            }
        });
        PARAMETERS.put("validate", new BooleanParameter() {
            @Override
            public Object get(DOMConfigurationImpl dOMConfigurationImpl) {
                return Boolean.valueOf(dOMConfigurationImpl.validate);
            }

            @Override
            public void set(DOMConfigurationImpl dOMConfigurationImpl, Object obj) {
                dOMConfigurationImpl.validate = ((Boolean) obj).booleanValue();
            }
        });
        PARAMETERS.put("validate-if-schema", new FixedParameter(false));
        PARAMETERS.put("well-formed", new BooleanParameter() {
            @Override
            public Object get(DOMConfigurationImpl dOMConfigurationImpl) {
                return Boolean.valueOf(dOMConfigurationImpl.wellFormed);
            }

            @Override
            public void set(DOMConfigurationImpl dOMConfigurationImpl, Object obj) {
                dOMConfigurationImpl.wellFormed = ((Boolean) obj).booleanValue();
            }
        });
    }

    static class FixedParameter implements Parameter {
        final Object onlyValue;

        FixedParameter(Object obj) {
            this.onlyValue = obj;
        }

        @Override
        public Object get(DOMConfigurationImpl dOMConfigurationImpl) {
            return this.onlyValue;
        }

        @Override
        public void set(DOMConfigurationImpl dOMConfigurationImpl, Object obj) {
            if (!this.onlyValue.equals(obj)) {
                throw new DOMException((short) 9, "Unsupported value: " + obj);
            }
        }

        @Override
        public boolean canSet(DOMConfigurationImpl dOMConfigurationImpl, Object obj) {
            return this.onlyValue.equals(obj);
        }
    }

    static abstract class BooleanParameter implements Parameter {
        BooleanParameter() {
        }

        @Override
        public boolean canSet(DOMConfigurationImpl dOMConfigurationImpl, Object obj) {
            return obj instanceof Boolean;
        }
    }

    @Override
    public boolean canSetParameter(String str, Object obj) {
        Parameter parameter = PARAMETERS.get(str);
        return parameter != null && parameter.canSet(this, obj);
    }

    @Override
    public void setParameter(String str, Object obj) throws DOMException {
        Parameter parameter = PARAMETERS.get(str);
        if (parameter == null) {
            throw new DOMException((short) 8, "No such parameter: " + str);
        }
        try {
            parameter.set(this, obj);
        } catch (ClassCastException e) {
            throw new DOMException((short) 17, "Invalid type for " + str + PluralRules.KEYWORD_RULE_SEPARATOR + obj.getClass());
        } catch (NullPointerException e2) {
            throw new DOMException((short) 17, "Null not allowed for " + str);
        }
    }

    @Override
    public Object getParameter(String str) throws DOMException {
        Parameter parameter = PARAMETERS.get(str);
        if (parameter == null) {
            throw new DOMException((short) 8, "No such parameter: " + str);
        }
        return parameter.get(this);
    }

    @Override
    public DOMStringList getParameterNames() {
        return internalGetParameterNames();
    }

    private static DOMStringList internalGetParameterNames() {
        final String[] strArr = (String[]) PARAMETERS.keySet().toArray(new String[PARAMETERS.size()]);
        return new DOMStringList() {
            @Override
            public String item(int i) {
                if (i < strArr.length) {
                    return strArr[i];
                }
                return null;
            }

            @Override
            public int getLength() {
                return strArr.length;
            }

            @Override
            public boolean contains(String str) {
                return DOMConfigurationImpl.PARAMETERS.containsKey(str);
            }
        };
    }

    public void normalize(Node node) {
        Node firstChild;
        TextImpl textImplMinimize;
        switch (node.getNodeType()) {
            case 1:
                NamedNodeMap attributes = ((ElementImpl) node).getAttributes();
                for (int i = 0; i < attributes.getLength(); i++) {
                    normalize(attributes.item(i));
                }
                firstChild = node.getFirstChild();
                while (firstChild != null) {
                    Node nextSibling = firstChild.getNextSibling();
                    normalize(firstChild);
                    firstChild = nextSibling;
                }
                return;
            case 2:
                checkTextValidity(((AttrImpl) node).getValue());
                return;
            case 3:
                textImplMinimize = ((TextImpl) node).minimize();
                if (textImplMinimize == null) {
                    checkTextValidity(textImplMinimize.buffer);
                    return;
                }
                return;
            case 4:
                CDATASectionImpl cDATASectionImpl = (CDATASectionImpl) node;
                if (this.cdataSections) {
                    if (cDATASectionImpl.needsSplitting()) {
                        if (this.splitCdataSections) {
                            cDATASectionImpl.split();
                            report((short) 1, "cdata-sections-splitted");
                        } else {
                            report((short) 2, "wf-invalid-character");
                        }
                    }
                    checkTextValidity(cDATASectionImpl.buffer);
                    return;
                }
                node = cDATASectionImpl.replaceWithText();
                textImplMinimize = ((TextImpl) node).minimize();
                if (textImplMinimize == null) {
                }
                break;
            case 5:
            case 6:
            case 10:
            case 12:
                return;
            case 7:
                checkTextValidity(((ProcessingInstructionImpl) node).getData());
                return;
            case 8:
                CommentImpl commentImpl = (CommentImpl) node;
                if (!this.comments) {
                    commentImpl.getParentNode().removeChild(commentImpl);
                    return;
                }
                if (commentImpl.containsDashDash()) {
                    report((short) 2, "wf-invalid-character");
                }
                checkTextValidity(commentImpl.buffer);
                return;
            case 9:
            case 11:
                firstChild = node.getFirstChild();
                while (firstChild != null) {
                }
                return;
            default:
                throw new DOMException((short) 9, "Unsupported node type " + ((int) node.getNodeType()));
        }
    }

    private void checkTextValidity(CharSequence charSequence) {
        if (this.wellFormed && !isValid(charSequence)) {
            report((short) 2, "wf-invalid-character");
        }
    }

    private boolean isValid(CharSequence charSequence) {
        int i = 0;
        while (true) {
            boolean z = true;
            if (i >= charSequence.length()) {
                return true;
            }
            char cCharAt = charSequence.charAt(i);
            if (cCharAt != '\t' && cCharAt != '\n' && cCharAt != '\r' && ((cCharAt < ' ' || cCharAt > 55295) && (cCharAt < 57344 || cCharAt > 65533))) {
                z = false;
            }
            if (!z) {
                return false;
            }
            i++;
        }
    }

    private void report(short s, String str) {
        if (this.errorHandler != null) {
            this.errorHandler.handleError(new DOMErrorImpl(s, str));
        }
    }
}
