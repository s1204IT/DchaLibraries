package mf.org.apache.xerces.impl.xs.util;

import java.util.ArrayList;
import mf.org.apache.xerces.impl.xs.SchemaGrammar;
import mf.org.apache.xerces.impl.xs.XSModelImpl;
import mf.org.apache.xerces.util.XMLGrammarPoolImpl;
import mf.org.apache.xerces.xs.XSModel;

public class XSGrammarPool extends XMLGrammarPoolImpl {
    public XSModel toXSModel() {
        return toXSModel((short) 1);
    }

    public XSModel toXSModel(short schemaVersion) {
        ArrayList list = new ArrayList();
        for (int i = 0; i < this.fGrammars.length; i++) {
            for (XMLGrammarPoolImpl.Entry entry = this.fGrammars[i]; entry != null; entry = entry.next) {
                if (entry.desc.getGrammarType().equals("http://www.w3.org/2001/XMLSchema")) {
                    list.add(entry.grammar);
                }
            }
        }
        int size = list.size();
        if (size == 0) {
            return toXSModel(new SchemaGrammar[0], schemaVersion);
        }
        SchemaGrammar[] gs = (SchemaGrammar[]) list.toArray(new SchemaGrammar[size]);
        return toXSModel(gs, schemaVersion);
    }

    protected XSModel toXSModel(SchemaGrammar[] grammars, short schemaVersion) {
        return new XSModelImpl(grammars, schemaVersion);
    }
}
