package app.core;

import java.io.IOException;
import java.nio.file.*;

public class Refactorer {

    public void moveIncludeFile(Path includeFile, Path targetDir) throws IOException {
        Files.createDirectories(targetDir);
        Path target = targetDir.resolve(includeFile.getFileName().toString());
        int i = 1;
        while (Files.exists(target)) {
            String name = includeFile.getFileName().toString();
            int dot = name.lastIndexOf('.');
            String base = dot > 0 ? name.substring(0, dot) : name;
            String ext  = dot > 0 ? name.substring(dot) : "";
            target = targetDir.resolve(base + "_" + i + ext);
            i++;
        }
        Files.move(includeFile, target, StandardCopyOption.REPLACE_EXISTING);
    }
}
