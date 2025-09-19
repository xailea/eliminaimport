package app.core;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Pattern;

public class IncludeUsageService {
    private final Path appRoot, includesRoot, unusedTarget;
    private final boolean applyChanges;
    private final java.util.function.Consumer<String> log;
    private final Report report;
    private final Refactorer refactorer;

    private static final List<String> EXTS = List.of(".jsx", ".tsx", ".js");
    private static final Pattern IMPORT_ANY =
            Pattern.compile("(?m)^\\s*import\\s+[^;]*?from\\s+['\"]([^'\"]+)['\"];?\\s*$");

    public IncludeUsageService(Path appRoot, Path includesRoot, Path unusedTarget,
                               boolean applyChanges,
                               java.util.function.Consumer<String> log,
                               Report report,
                               Refactorer refactorer) {
        this.appRoot = appRoot.toAbsolutePath().normalize();
        this.includesRoot = includesRoot.toAbsolutePath().normalize();
        this.unusedTarget = unusedTarget.toAbsolutePath().normalize();
        this.applyChanges = applyChanges;
        this.log = log;
        this.report = report;
        this.refactorer = refactorer;
    }

    public void execute() throws IOException {
        Files.createDirectories(unusedTarget);

        Map<String, Path> includes = indexIncludes();
        log.accept("Include indicizzati: " + includes.size());

        Map<String, Set<Path>> usedBy = new LinkedHashMap<>();
        for (var k : includes.keySet()) usedBy.put(k, new LinkedHashSet<>());

        var appFiles = listJsx(appRoot, Set.of(unusedTarget));
        String marker = "/" + includesRoot.getFileName().toString() + "/";

        for (var file : appFiles) {
            final String content;
            try {
                content = Files.readString(file, StandardCharsets.UTF_8);
            } catch (Exception e) {
                log.accept("Errore lettura: " + rel(file) + " -> " + e.getMessage());
                continue;
            }

            var m = IMPORT_ANY.matcher(content);
            while (m.find()) {
                var spec = m.group(1);
                if (!spec.contains(marker)) continue;

                String tail = normalizeTail(spec.substring(spec.indexOf(marker) + marker.length()));
                if (!includes.containsKey(tail)) continue;

                Path incPath = includes.get(tail);
                Path target  = resolveImport(file.getParent(), spec);

                try {
                    if (target != null && Files.isSameFile(target, incPath) && Files.isSameFile(file, incPath)) continue;
                } catch (Exception ignored) {}

                usedBy.get(tail).add(file);
            }

            log.accept("Scansionato: " + rel(file));
        }

        for (var e : includes.entrySet()) {
            String name = e.getKey();
            Path incPath = e.getValue();
            var users = usedBy.getOrDefault(name, Set.of());

            if (users.isEmpty()) {
                log.accept("NON usato: " + name + " -> " + rel(incPath));
                report.addUnused(name, rel(incPath));
                if (applyChanges && Files.exists(incPath)) {
                    moveUnused(incPath);
                }
            } else {
                log.accept("USATO: " + name + " in " + users.size() + " file");
            }
        }

        log.accept("Completato.");
    }

    private Map<String, Path> indexIncludes() throws IOException {
        Map<String, Path> map = new LinkedHashMap<>();
        try (var s = Files.walk(includesRoot)) {
            s.filter(Files::isRegularFile)
                    .filter(p -> {
                        var n = p.getFileName().toString().toLowerCase();
                        return EXTS.stream().anyMatch(n::endsWith);
                    })
                    .forEach(p -> {
                        var name = p.getFileName().toString();
                        int dot = name.lastIndexOf('.');
                        var base = dot > 0 ? name.substring(0, dot) : name;
                        map.put(base, p.toAbsolutePath().normalize());
                    });
        }
        return map;
    }

    private static List<Path> listJsx(Path root, Set<Path> exclude) throws IOException {
        var exAbs = exclude.stream().map(p -> p.toAbsolutePath().normalize()).toList();
        try (var s = Files.walk(root)) {
            return s.filter(p -> {
                        if (!Files.isRegularFile(p)) return false;
                        var abs = p.toAbsolutePath().normalize();
                        for (var ex : exAbs) if (abs.startsWith(ex)) return false;
                        var n = p.getFileName().toString().toLowerCase();
                        return n.endsWith(".jsx") || n.endsWith(".tsx");
                    })
                    .toList();
        }
    }

    private static Path resolveImport(Path baseDir, String spec) {
        var candidates = List.of(
                spec, spec + ".jsx", spec + ".tsx", spec + ".js",
                spec + "/index.jsx", spec + "/index.js", spec + "/index.tsx"
        );
        for (var c : candidates) {
            try {
                var p = baseDir.resolve(c).normalize();
                if (Files.exists(p)) return p;
            } catch (Exception ignored) {}
        }
        return null;
    }

    private void moveUnused(Path incPath) {
        try {
            refactorer.moveIncludeFile(incPath, unusedTarget);
            log.accept("Spostato: " + rel(incPath));
        } catch (Exception ex) {
            log.accept("Errore spostamento " + rel(incPath) + ": " + ex.getMessage());
        }
    }

    private String normalizeTail(String tail) {
        if (tail.isEmpty()) return tail;
        if (tail.endsWith("/index")) tail = tail.substring(0, tail.length()-6);
        int slash = tail.indexOf('/');
        if (slash > 0) tail = tail.substring(0, slash);
        int dot = tail.lastIndexOf('.');
        if (dot > 0) tail = tail.substring(0, dot);
        return tail;
    }

    private String rel(Path p){ return appRoot.relativize(p.toAbsolutePath().normalize()).toString(); }
}
