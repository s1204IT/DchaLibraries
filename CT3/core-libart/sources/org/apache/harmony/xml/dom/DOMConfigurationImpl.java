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
            public Object get(DOMConfigurationImpl config) {
                return Boolean.valueOf(config.cdataSections);
            }

            @Override
            public void set(DOMConfigurationImpl config, Object value) {
                config.cdataSections = ((Boolean) value).booleanValue();
            }
        });
        PARAMETERS.put("check-character-normalization", new FixedParameter(false));
        PARAMETERS.put("comments", new BooleanParameter() {
            @Override
            public Object get(DOMConfigurationImpl config) {
                return Boolean.valueOf(config.comments);
            }

            @Override
            public void set(DOMConfigurationImpl config, Object value) {
                config.comments = ((Boolean) value).booleanValue();
            }
        });
        PARAMETERS.put("datatype-normalization", new BooleanParameter() {
            @Override
            public Object get(DOMConfigurationImpl config) {
                return Boolean.valueOf(config.datatypeNormalization);
            }

            @Override
            public void set(DOMConfigurationImpl config, Object value) {
                if (((Boolean) value).booleanValue()) {
                    config.datatypeNormalization = true;
                    config.validate = true;
                } else {
                    config.datatypeNormalization = false;
                }
            }
        });
        PARAMETERS.put("element-content-whitespace", new FixedParameter(true));
        PARAMETERS.put("entities", new BooleanParameter() {
            @Override
            public Object get(DOMConfigurationImpl config) {
                return Boolean.valueOf(config.entities);
            }

            @Override
            public void set(DOMConfigurationImpl config, Object value) {
                config.entities = ((Boolean) value).booleanValue();
            }
        });
        PARAMETERS.put("error-handler", new Parameter() {
            @Override
            public Object get(DOMConfigurationImpl config) {
                return config.errorHandler;
            }

            @Override
            public void set(DOMConfigurationImpl config, Object value) {
                config.errorHandler = (DOMErrorHandler) value;
            }

            @Override
            public boolean canSet(DOMConfigurationImpl config, Object value) {
                if (value != null) {
                    return value instanceof DOMErrorHandler;
                }
                return true;
            }
        });
        PARAMETERS.put("infoset", new BooleanParameter() {
            @Override
            public Object get(DOMConfigurationImpl config) {
                return Boolean.valueOf((config.entities || config.datatypeNormalization || config.cdataSections || !config.wellFormed || !config.comments) ? false : config.namespaces);
            }

            @Override
            public void set(DOMConfigurationImpl config, Object value) {
                if (!((Boolean) value).booleanValue()) {
                    return;
                }
                config.entities = false;
                config.datatypeNormalization = false;
                config.cdataSections = false;
                config.wellFormed = true;
                config.comments = true;
                config.namespaces = true;
            }
        });
        PARAMETERS.put("namespaces", new BooleanParameter() {
            @Override
            public Object get(DOMConfigurationImpl config) {
                return Boolean.valueOf(config.namespaces);
            }

            @Override
            public void set(DOMConfigurationImpl config, Object value) {
                config.namespaces = ((Boolean) value).booleanValue();
            }
        });
        PARAMETERS.put("namespace-declarations", new FixedParameter(true));
        PARAMETERS.put("normalize-characters", new FixedParameter(false));
        PARAMETERS.put("schema-location", new Parameter() {
            @Override
            public Object get(DOMConfigurationImpl config) {
                return config.schemaLocation;
            }

            @Override
            public void set(DOMConfigurationImpl config, Object value) {
                config.schemaLocation = (String) value;
            }

            @Override
            public boolean canSet(DOMConfigurationImpl config, Object value) {
                if (value != null) {
                    return value instanceof String;
                }
                return true;
            }
        });
        PARAMETERS.put("schema-type", new Parameter() {
            @Override
            public Object get(DOMConfigurationImpl config) {
                return config.schemaType;
            }

            @Override
            public void set(DOMConfigurationImpl config, Object value) {
                config.schemaType = (String) value;
            }

            @Override
            public boolean canSet(DOMConfigurationImpl config, Object value) {
                if (value != null) {
                    return value instanceof String;
                }
                return true;
            }
        });
        PARAMETERS.put("split-cdata-sections", new BooleanParameter() {
            @Override
            public Object get(DOMConfigurationImpl config) {
                return Boolean.valueOf(config.splitCdataSections);
            }

            @Override
            public void set(DOMConfigurationImpl config, Object value) {
                config.splitCdataSections = ((Boolean) value).booleanValue();
            }
        });
        PARAMETERS.put("validate", new BooleanParameter() {
            @Override
            public Object get(DOMConfigurationImpl config) {
                return Boolean.valueOf(config.validate);
            }

            @Override
            public void set(DOMConfigurationImpl config, Object value) {
                config.validate = ((Boolean) value).booleanValue();
            }
        });
        PARAMETERS.put("validate-if-schema", new FixedParameter(false));
        PARAMETERS.put("well-formed", new BooleanParameter() {
            @Override
            public Object get(DOMConfigurationImpl config) {
                return Boolean.valueOf(config.wellFormed);
            }

            @Override
            public void set(DOMConfigurationImpl config, Object value) {
                config.wellFormed = ((Boolean) value).booleanValue();
            }
        });
    }

    static class FixedParameter implements Parameter {
        final Object onlyValue;

        FixedParameter(Object onlyValue) {
            this.onlyValue = onlyValue;
        }

        @Override
        public Object get(DOMConfigurationImpl config) {
            return this.onlyValue;
        }

        @Override
        public void set(DOMConfigurationImpl config, Object value) {
            if (this.onlyValue.equals(value)) {
            } else {
                throw new DOMException((short) 9, "Unsupported value: " + value);
            }
        }

        @Override
        public boolean canSet(DOMConfigurationImpl config, Object value) {
            return this.onlyValue.equals(value);
        }
    }

    static abstract class BooleanParameter implements Parameter {
        BooleanParameter() {
        }

        @Override
        public boolean canSet(DOMConfigurationImpl config, Object value) {
            return value instanceof Boolean;
        }
    }

    @Override
    public boolean canSetParameter(String name, Object value) {
        Parameter parameter = PARAMETERS.get(name);
        if (parameter != null) {
            return parameter.canSet(this, value);
        }
        return false;
    }

    @Override
    public void setParameter(String name, Object value) throws DOMException {
        Parameter parameter = PARAMETERS.get(name);
        if (parameter == null) {
            throw new DOMException((short) 8, "No such parameter: " + name);
        }
        try {
            parameter.set(this, value);
        } catch (ClassCastException e) {
            throw new DOMException((short) 17, "Invalid type for " + name + PluralRules.KEYWORD_RULE_SEPARATOR + value.getClass());
        } catch (NullPointerException e2) {
            throw new DOMException((short) 17, "Null not allowed for " + name);
        }
    }

    @Override
    public Object getParameter(String name) throws DOMException {
        Parameter parameter = PARAMETERS.get(name);
        if (parameter == null) {
            throw new DOMException((short) 8, "No such parameter: " + name);
        }
        return parameter.get(this);
    }

    @Override
    public DOMStringList getParameterNames() {
        final String[] result = (String[]) PARAMETERS.keySet().toArray(new String[PARAMETERS.size()]);
        return new DOMStringList() {
            @Override
            public String item(int index) {
                if (index < result.length) {
                    return result[index];
                }
                return null;
            }

            @Override
            public int getLength() {
                return result.length;
            }

            @Override
            public boolean contains(String str) {
                return DOMConfigurationImpl.PARAMETERS.containsKey(str);
            }
        };
    }

    public void normalize(Node node) {
        Node child;
        TextImpl text;
        switch (node.getNodeType()) {
            case 1:
                ElementImpl element = (ElementImpl) node;
                NamedNodeMap attributes = element.getAttributes();
                for (int i = 0; i < attributes.getLength(); i++) {
                    normalize(attributes.item(i));
                }
                child = node.getFirstChild();
                while (child != null) {
                    Node next = child.getNextSibling();
                    normalize(child);
                    child = next;
                }
                return;
            case 2:
                checkTextValidity(((AttrImpl) node).getValue());
                return;
            case 3:
                text = ((TextImpl) node).minimize();
                if (text != null) {
                    return;
                }
                checkTextValidity(text.buffer);
                return;
            case 4:
                CDATASectionImpl cdata = (CDATASectionImpl) node;
                if (this.cdataSections) {
                    if (cdata.needsSplitting()) {
                        if (this.splitCdataSections) {
                            cdata.split();
                            report((short) 1, "cdata-sections-splitted");
                        } else {
                            report((short) 2, "wf-invalid-character");
                        }
                    }
                    checkTextValidity(cdata.buffer);
                    return;
                }
                node = cdata.replaceWithText();
                text = ((TextImpl) node).minimize();
                if (text != null) {
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
                CommentImpl comment = (CommentImpl) node;
                if (!this.comments) {
                    comment.getParentNode().removeChild(comment);
                    return;
                }
                if (comment.containsDashDash()) {
                    report((short) 2, "wf-invalid-character");
                }
                checkTextValidity(comment.buffer);
                return;
            case 9:
            case 11:
                child = node.getFirstChild();
                while (child != null) {
                }
                return;
            default:
                throw new DOMException((short) 9, "Unsupported node type " + ((int) node.getNodeType()));
        }
    }

    private void checkTextValidity(CharSequence s) {
        if (!this.wellFormed || isValid(s)) {
            return;
        }
        report((short) 2, "wf-invalid-character");
    }

    private boolean isValid(CharSequence text) {
        boolean valid;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\t' || c == '\n' || c == '\r' || (c >= ' ' && c <= 55295)) {
                valid = true;
            } else {
                valid = c >= 57344 && c <= 65533;
            }
            if (!valid) {
                return false;
            }
        }
        return true;
    }

    private void report(short severity, String type) {
        if (this.errorHandler == null) {
            return;
        }
        this.errorHandler.handleError(new DOMErrorImpl(severity, type));
    }
}
