package java.io;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;

class DeleteOnExitHook {
    private static LinkedHashSet<String> files = new LinkedHashSet<>();

    static {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                DeleteOnExitHook.runHooks();
            }
        });
    }

    private DeleteOnExitHook() {
    }

    static synchronized void add(String str) {
        if (files == null) {
            throw new IllegalStateException("Shutdown in progress");
        }
        files.add(str);
    }

    static void runHooks() {
        LinkedHashSet<String> linkedHashSet;
        synchronized (DeleteOnExitHook.class) {
            linkedHashSet = files;
            files = null;
        }
        ArrayList arrayList = new ArrayList(linkedHashSet);
        Collections.reverse(arrayList);
        Iterator it = arrayList.iterator();
        while (it.hasNext()) {
            new File((String) it.next()).delete();
        }
    }
}
