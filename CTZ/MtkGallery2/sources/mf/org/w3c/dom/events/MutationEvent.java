package mf.org.w3c.dom.events;

import mf.org.w3c.dom.Node;

public interface MutationEvent extends Event {
    void initMutationEvent(String str, boolean z, boolean z2, Node node, String str2, String str3, String str4, short s);
}
