package libcore.io;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

public class DeleteOnExit extends Thread {
    private static DeleteOnExit instance;
    private final ArrayList<String> files = new ArrayList<>();

    public static synchronized DeleteOnExit getInstance() {
        if (instance == null) {
            instance = new DeleteOnExit();
            Runtime.getRuntime().addShutdownHook(instance);
        }
        return instance;
    }

    private DeleteOnExit() {
    }

    public void addFile(String filename) {
        synchronized (this.files) {
            if (!this.files.contains(filename)) {
                this.files.add(filename);
            }
        }
    }

    @Override
    public void run() {
        Collections.sort(this.files);
        for (int i = this.files.size() - 1; i >= 0; i--) {
            new File(this.files.get(i)).delete();
        }
    }
}
