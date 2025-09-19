package app.core;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashSet;
import java.util.Set;

public class Report {
    private final Set<String> lines = new LinkedHashSet<>();

    public void addUnused(String includeName, String includeRelPath) {
        lines.add(includeName + " | " + includeRelPath);
    }

    public Path writeUnusedTxt(Path outDir, String baseName) throws Exception {
        Files.createDirectories(outDir);
        var ts  = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").format(LocalDateTime.now());
        var txt = outDir.resolve(baseName + "-" + ts + ".txt");
        try (var w = Files.newBufferedWriter(txt, StandardCharsets.UTF_8)) {
            w.write("# Include NON usati (" + lines.size() + ")\n");
            for (var line : lines) w.write(line + "\n");
        }
        return txt;
    }
}
