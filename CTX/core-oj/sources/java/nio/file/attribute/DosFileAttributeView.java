package java.nio.file.attribute;

import java.io.IOException;

public interface DosFileAttributeView extends BasicFileAttributeView {
    @Override
    String name();

    @Override
    DosFileAttributes readAttributes() throws IOException;

    void setArchive(boolean z) throws IOException;

    void setHidden(boolean z) throws IOException;

    void setReadOnly(boolean z) throws IOException;

    void setSystem(boolean z) throws IOException;
}
