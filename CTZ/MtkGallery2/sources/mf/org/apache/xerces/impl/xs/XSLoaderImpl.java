package mf.org.apache.xerces.impl.xs;

import mf.org.apache.xerces.impl.xs.util.XSGrammarPool;
import mf.org.apache.xerces.xni.grammars.Grammar;
import mf.org.apache.xerces.xni.grammars.XMLGrammarDescription;
import mf.org.apache.xerces.xni.grammars.XSGrammar;
import mf.org.apache.xerces.xni.parser.XMLInputSource;
import mf.org.apache.xerces.xs.LSInputList;
import mf.org.apache.xerces.xs.StringList;
import mf.org.apache.xerces.xs.XSLoader;
import mf.org.apache.xerces.xs.XSModel;
import mf.org.apache.xerces.xs.XSNamedMap;
import mf.org.apache.xerces.xs.XSObjectList;
import mf.org.apache.xerces.xs.XSTypeDefinition;
import mf.org.w3c.dom.DOMConfiguration;
import mf.org.w3c.dom.DOMException;
import mf.org.w3c.dom.DOMStringList;
import mf.org.w3c.dom.ls.LSInput;

public final class XSLoaderImpl implements XSLoader, DOMConfiguration {
    private final XSGrammarPool fGrammarPool = new XSGrammarMerger();
    private final XMLSchemaLoader fSchemaLoader = new XMLSchemaLoader();

    public XSLoaderImpl() {
        this.fSchemaLoader.setProperty("http://apache.org/xml/properties/internal/grammar-pool", this.fGrammarPool);
    }

    @Override
    public DOMConfiguration getConfig() {
        return this;
    }

    @Override
    public XSModel loadURIList(StringList uriList) {
        int length = uriList.getLength();
        try {
            this.fGrammarPool.clear();
            for (int i = 0; i < length; i++) {
                this.fSchemaLoader.loadGrammar(new XMLInputSource(null, uriList.item(i), null));
            }
            return this.fGrammarPool.toXSModel();
        } catch (Exception e) {
            this.fSchemaLoader.reportDOMFatalError(e);
            return null;
        }
    }

    @Override
    public XSModel loadInputList(LSInputList is) {
        int length = is.getLength();
        try {
            this.fGrammarPool.clear();
            for (int i = 0; i < length; i++) {
                this.fSchemaLoader.loadGrammar(this.fSchemaLoader.dom2xmlInputSource(is.item(i)));
            }
            return this.fGrammarPool.toXSModel();
        } catch (Exception e) {
            this.fSchemaLoader.reportDOMFatalError(e);
            return null;
        }
    }

    @Override
    public XSModel loadURI(String uri) {
        try {
            this.fGrammarPool.clear();
            return ((XSGrammar) this.fSchemaLoader.loadGrammar(new XMLInputSource(null, uri, null))).toXSModel();
        } catch (Exception e) {
            this.fSchemaLoader.reportDOMFatalError(e);
            return null;
        }
    }

    @Override
    public XSModel load(LSInput is) {
        try {
            this.fGrammarPool.clear();
            return ((XSGrammar) this.fSchemaLoader.loadGrammar(this.fSchemaLoader.dom2xmlInputSource(is))).toXSModel();
        } catch (Exception e) {
            this.fSchemaLoader.reportDOMFatalError(e);
            return null;
        }
    }

    public void setParameter(String name, Object value) throws DOMException {
        this.fSchemaLoader.setParameter(name, value);
    }

    public Object getParameter(String name) throws DOMException {
        return this.fSchemaLoader.getParameter(name);
    }

    public boolean canSetParameter(String name, Object value) {
        return this.fSchemaLoader.canSetParameter(name, value);
    }

    public DOMStringList getParameterNames() {
        return this.fSchemaLoader.getParameterNames();
    }

    private static final class XSGrammarMerger extends XSGrammarPool {
        @Override
        public void putGrammar(Grammar grammar) {
            SchemaGrammar cachedGrammar = toSchemaGrammar(super.getGrammar(grammar.getGrammarDescription()));
            if (cachedGrammar != null) {
                SchemaGrammar newGrammar = toSchemaGrammar(grammar);
                if (newGrammar != null) {
                    mergeSchemaGrammars(cachedGrammar, newGrammar);
                    return;
                }
                return;
            }
            super.putGrammar(grammar);
        }

        private SchemaGrammar toSchemaGrammar(Grammar grammar) {
            if (grammar instanceof SchemaGrammar) {
                return grammar;
            }
            return null;
        }

        private void mergeSchemaGrammars(SchemaGrammar cachedGrammar, SchemaGrammar newGrammar) {
            XSNamedMap map = newGrammar.getComponents((short) 2);
            int length = map.getLength();
            for (int i = 0; i < length; i++) {
                XSElementDecl decl = (XSElementDecl) map.item(i);
                if (cachedGrammar.getGlobalElementDecl(decl.getName()) == null) {
                    cachedGrammar.addGlobalElementDecl(decl);
                }
            }
            XSNamedMap map2 = newGrammar.getComponents((short) 1);
            int length2 = map2.getLength();
            for (int i2 = 0; i2 < length2; i2++) {
                XSAttributeDecl decl2 = (XSAttributeDecl) map2.item(i2);
                if (cachedGrammar.getGlobalAttributeDecl(decl2.getName()) == null) {
                    cachedGrammar.addGlobalAttributeDecl(decl2);
                }
            }
            XSNamedMap map3 = newGrammar.getComponents((short) 3);
            int length3 = map3.getLength();
            for (int i3 = 0; i3 < length3; i3++) {
                XSTypeDefinition decl3 = (XSTypeDefinition) map3.item(i3);
                if (cachedGrammar.getGlobalTypeDecl(decl3.getName()) == null) {
                    cachedGrammar.addGlobalTypeDecl(decl3);
                }
            }
            XSNamedMap map4 = newGrammar.getComponents((short) 5);
            int length4 = map4.getLength();
            for (int i4 = 0; i4 < length4; i4++) {
                XSAttributeGroupDecl decl4 = (XSAttributeGroupDecl) map4.item(i4);
                if (cachedGrammar.getGlobalAttributeGroupDecl(decl4.getName()) == null) {
                    cachedGrammar.addGlobalAttributeGroupDecl(decl4);
                }
            }
            XSNamedMap map5 = newGrammar.getComponents((short) 7);
            int length5 = map5.getLength();
            for (int i5 = 0; i5 < length5; i5++) {
                XSGroupDecl decl5 = (XSGroupDecl) map5.item(i5);
                if (cachedGrammar.getGlobalGroupDecl(decl5.getName()) == null) {
                    cachedGrammar.addGlobalGroupDecl(decl5);
                }
            }
            XSNamedMap map6 = newGrammar.getComponents((short) 11);
            int length6 = map6.getLength();
            for (int i6 = 0; i6 < length6; i6++) {
                XSNotationDecl decl6 = (XSNotationDecl) map6.item(i6);
                if (cachedGrammar.getGlobalNotationDecl(decl6.getName()) == null) {
                    cachedGrammar.addGlobalNotationDecl(decl6);
                }
            }
            XSObjectList annotations = newGrammar.getAnnotations();
            int length7 = annotations.getLength();
            for (int i7 = 0; i7 < length7; i7++) {
                cachedGrammar.addAnnotation((XSAnnotationImpl) annotations.item(i7));
            }
        }

        @Override
        public boolean containsGrammar(XMLGrammarDescription desc) {
            return false;
        }

        @Override
        public Grammar getGrammar(XMLGrammarDescription desc) {
            return null;
        }

        @Override
        public Grammar retrieveGrammar(XMLGrammarDescription desc) {
            return null;
        }

        @Override
        public Grammar[] retrieveInitialGrammarSet(String grammarType) {
            return new Grammar[0];
        }
    }
}
