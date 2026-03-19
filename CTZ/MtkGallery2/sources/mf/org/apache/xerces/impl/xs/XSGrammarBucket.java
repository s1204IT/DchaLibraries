package mf.org.apache.xerces.impl.xs;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

public class XSGrammarBucket {
    Hashtable fGrammarRegistry = new Hashtable();
    SchemaGrammar fNoNSGrammar = null;

    public SchemaGrammar getGrammar(String namespace) {
        if (namespace == null) {
            return this.fNoNSGrammar;
        }
        return (SchemaGrammar) this.fGrammarRegistry.get(namespace);
    }

    public void putGrammar(SchemaGrammar grammar) {
        if (grammar.getTargetNamespace() == null) {
            this.fNoNSGrammar = grammar;
        } else {
            this.fGrammarRegistry.put(grammar.getTargetNamespace(), grammar);
        }
    }

    public boolean putGrammar(SchemaGrammar grammar, boolean deep) {
        SchemaGrammar sg = getGrammar(grammar.fTargetNamespace);
        if (sg != null) {
            return sg == grammar;
        }
        if (!deep) {
            putGrammar(grammar);
            return true;
        }
        Vector currGrammars = grammar.getImportedGrammars();
        if (currGrammars == null) {
            putGrammar(grammar);
            return true;
        }
        Vector grammars = (Vector) currGrammars.clone();
        for (int i = 0; i < grammars.size(); i++) {
            SchemaGrammar sg1 = (SchemaGrammar) grammars.elementAt(i);
            SchemaGrammar sg2 = getGrammar(sg1.fTargetNamespace);
            if (sg2 == null) {
                Vector gs = sg1.getImportedGrammars();
                if (gs != null) {
                    for (int j = gs.size() - 1; j >= 0; j--) {
                        SchemaGrammar sg22 = (SchemaGrammar) gs.elementAt(j);
                        if (!grammars.contains(sg22)) {
                            grammars.addElement(sg22);
                        }
                    }
                }
            } else if (sg2 != sg1) {
                return false;
            }
        }
        putGrammar(grammar);
        for (int i2 = grammars.size() - 1; i2 >= 0; i2--) {
            putGrammar((SchemaGrammar) grammars.elementAt(i2));
        }
        return true;
    }

    public boolean putGrammar(SchemaGrammar grammar, boolean deep, boolean ignoreConflict) {
        Vector currGrammars;
        if (!ignoreConflict) {
            return putGrammar(grammar, deep);
        }
        SchemaGrammar sg = getGrammar(grammar.fTargetNamespace);
        if (sg == null) {
            putGrammar(grammar);
        }
        if (!deep || (currGrammars = grammar.getImportedGrammars()) == null) {
            return true;
        }
        Vector grammars = (Vector) currGrammars.clone();
        for (int i = 0; i < grammars.size(); i++) {
            SchemaGrammar sg1 = (SchemaGrammar) grammars.elementAt(i);
            if (getGrammar(sg1.fTargetNamespace) == null) {
                Vector gs = sg1.getImportedGrammars();
                if (gs != null) {
                    for (int j = gs.size() - 1; j >= 0; j--) {
                        SchemaGrammar sg2 = (SchemaGrammar) gs.elementAt(j);
                        if (!grammars.contains(sg2)) {
                            grammars.addElement(sg2);
                        }
                    }
                }
            } else {
                grammars.remove(sg1);
            }
        }
        int i2 = grammars.size();
        for (int i3 = i2 - 1; i3 >= 0; i3--) {
            putGrammar((SchemaGrammar) grammars.elementAt(i3));
        }
        return true;
    }

    public SchemaGrammar[] getGrammars() {
        int count = this.fGrammarRegistry.size() + (this.fNoNSGrammar == null ? 0 : 1);
        SchemaGrammar[] grammars = new SchemaGrammar[count];
        Enumeration schemas = this.fGrammarRegistry.elements();
        int i = 0;
        while (schemas.hasMoreElements()) {
            grammars[i] = (SchemaGrammar) schemas.nextElement();
            i++;
        }
        if (this.fNoNSGrammar != null) {
            grammars[count - 1] = this.fNoNSGrammar;
        }
        return grammars;
    }

    public void reset() {
        this.fNoNSGrammar = null;
        this.fGrammarRegistry.clear();
    }
}
