package app.core;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class IncludeUsageService {
    private final Path appRoot, includesRoot, unusedTarget;
    private final boolean applyChanges;
    private final java.util.function.Consumer<String> log;
    private final Report report;
    private final Refactorer refactorer;

    private static final List<String> EXTS = List.of(".jsx", ".tsx", ".js");

    // Pattern più robusto per catturare import/require
    private static final Pattern IMPORT_PATTERN = Pattern.compile(
            "(?m)^\\s*(?:" +
                    "import\\s+(?:[{][^}]*[}]|[^{][^;]*?)\\s+from\\s+['\"]([^'\"]+)['\"]|" +
                    "import\\s*\\(\\s*['\"]([^'\"]+)['\"]\\s*\\)|" +
                    "require\\s*\\(\\s*['\"]([^'\"]+)['\"]\\s*\\)" +
                    ");?\\s*$"
    );

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

        // 1) Indicizza tutti gli include
        Map<String, Path> includes = indexIncludes();
        log.accept("Include indicizzati: " + includes.size());

        Map<Path, String> pathToName = includes.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey, (a, b) -> a, LinkedHashMap::new));

        Set<Path> includePaths = new LinkedHashSet<>(includes.values());

        // 2) Analizza usi diretti dai file dell'app (esclusi gli include)
        Map<String, Set<Path>> directUsages = analyzeDirectUsages(includes, pathToName, includePaths);

        // 3) Costruisci grafo delle dipendenze include-to-include
        Map<Path, Set<Path>> includeDependencies = buildIncludeDependencyGraph(includePaths);

        // 4) Determina include utilizzati attraverso analisi transitiva
        Set<Path> transitivelyUsed = computeTransitiveUsage(directUsages, includeDependencies, pathToName);

        // 5) Genera report e sposta file inutilizzati
        generateReportAndMoveFiles(includes, directUsages, transitivelyUsed);

        log.accept("Analisi completata.");
    }

    private Map<String, Set<Path>> analyzeDirectUsages(Map<String, Path> includes,
                                                       Map<Path, String> pathToName,
                                                       Set<Path> includePaths) throws IOException {
        Map<String, Set<Path>> usedBy = includes.keySet().stream()
                .collect(Collectors.toMap(k -> k, k -> new LinkedHashSet<>(), (a, b) -> a, LinkedHashMap::new));

        List<Path> appFiles = listSources(appRoot, Set.of(unusedTarget));

        for (Path file : appFiles) {
            // Salta file che sono dentro la cartella includes
            if (isUnder(file, includesRoot)) {
                continue;
            }

            analyzeFileImports(file, includePaths, pathToName, usedBy);
        }

        return usedBy;
    }

    private void analyzeFileImports(Path file, Set<Path> includePaths,
                                    Map<Path, String> pathToName,
                                    Map<String, Set<Path>> usedBy) {
        try {
            String content = Files.readString(file, StandardCharsets.UTF_8);
            Set<String> imports = extractImports(content);

            for (String importSpec : imports) {
                Path resolvedPath = resolveImport(file.getParent(), importSpec);
                if (resolvedPath == null || !includePaths.contains(resolvedPath)) {
                    continue;
                }

                // Evita auto-import
                if (isSameFile(file, resolvedPath)) {
                    continue;
                }

                String includeName = pathToName.get(resolvedPath);
                if (includeName != null) {
                    usedBy.get(includeName).add(file);
                }
            }

            log.accept("Analizzato: " + rel(file));

        } catch (IOException e) {
            log.accept("Errore lettura file " + rel(file) + ": " + e.getMessage());
        }
    }

    private Set<String> extractImports(String content) {
        Set<String> imports = new LinkedHashSet<>();
        var matcher = IMPORT_PATTERN.matcher(content);

        while (matcher.find()) {
            // Controlla tutti i gruppi di cattura per diversi tipi di import
            for (int i = 1; i <= matcher.groupCount(); i++) {
                String importPath = matcher.group(i);
                if (importPath != null && !importPath.trim().isEmpty()) {
                    imports.add(importPath.trim());
                    break; // Prendi solo il primo match non-null
                }
            }
        }

        return imports;
    }

    private Map<Path, Set<Path>> buildIncludeDependencyGraph(Set<Path> includePaths) {
        Map<Path, Set<Path>> dependencies = includePaths.stream()
                .collect(Collectors.toMap(p -> p, p -> new LinkedHashSet<>(), (a, b) -> a, LinkedHashMap::new));

        for (Path includePath : includePaths) {
            try {
                String content = Files.readString(includePath, StandardCharsets.UTF_8);
                Set<String> imports = extractImports(content);

                for (String importSpec : imports) {
                    Path resolvedPath = resolveImport(includePath.getParent(), importSpec);
                    if (resolvedPath == null || !includePaths.contains(resolvedPath)) {
                        continue;
                    }

                    // Evita dipendenze circolari immediate
                    if (isSameFile(includePath, resolvedPath)) {
                        continue;
                    }

                    dependencies.get(includePath).add(resolvedPath);
                }

            } catch (IOException e) {
                log.accept("Errore analisi dipendenze per " + rel(includePath) + ": " + e.getMessage());
            }
        }

        return dependencies;
    }

    private Set<Path> computeTransitiveUsage(Map<String, Set<Path>> directUsages,
                                             Map<Path, Set<Path>> dependencies,
                                             Map<Path, String> pathToName) {

        // Trova tutti gli include usati direttamente dall'app
        Set<Path> directlyUsed = directUsages.entrySet().stream()
                .filter(entry -> !entry.getValue().isEmpty())
                .map(entry -> {
                    String includeName = entry.getKey();
                    return pathToName.entrySet().stream()
                            .filter(pathEntry -> pathEntry.getValue().equals(includeName))
                            .map(Map.Entry::getKey)
                            .findFirst()
                            .orElse(null);
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // Esegui BFS per trovare tutti gli include raggiungibili
        Set<Path> transitivelyUsed = new LinkedHashSet<>();
        Queue<Path> queue = new ArrayDeque<>(directlyUsed);

        while (!queue.isEmpty()) {
            Path current = queue.poll();

            if (!transitivelyUsed.add(current)) {
                continue; // Già visitato
            }

            // Aggiungi tutte le dipendenze alla coda
            Set<Path> deps = dependencies.getOrDefault(current, Collections.emptySet());
            for (Path dependency : deps) {
                if (!transitivelyUsed.contains(dependency)) {
                    queue.offer(dependency);
                }
            }
        }

        return transitivelyUsed;
    }

    private void generateReportAndMoveFiles(Map<String, Path> includes,
                                            Map<String, Set<Path>> directUsages,
                                            Set<Path> transitivelyUsed) {

        for (Map.Entry<String, Path> entry : includes.entrySet()) {
            String includeName = entry.getKey();
            Path includePath = entry.getValue();
            Set<Path> directUsers = directUsages.getOrDefault(includeName, Collections.emptySet());

            if (transitivelyUsed.contains(includePath)) {
                if (directUsers.isEmpty()) {
                    log.accept("USATO (indirettamente): " + includeName);
                } else {
                    log.accept("USATO (direttamente): " + includeName + " in " + directUsers.size() + " file");
                }
            } else {
                log.accept("NON USATO: " + includeName + " -> " + rel(includePath));
                report.addUnused(includeName, rel(includePath));

                if (applyChanges && Files.exists(includePath)) {
                    moveUnused(includePath);
                }
            }
        }
    }

    // ===== Helper Methods =====

    private Map<String, Path> indexIncludes() throws IOException {
        Map<String, Path> includes = new LinkedHashMap<>();

        try (var stream = Files.walk(includesRoot)) {
            stream.filter(Files::isRegularFile)
                    .filter(this::hasValidExtension)
                    .forEach(path -> {
                        String fileName = path.getFileName().toString();
                        String baseName = getBaseName(fileName);
                        includes.put(baseName, path.toAbsolutePath().normalize());
                    });
        }

        return includes;
    }

    private boolean hasValidExtension(Path path) {
        String fileName = path.getFileName().toString().toLowerCase();
        return EXTS.stream().anyMatch(fileName::endsWith);
    }

    private String getBaseName(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        return lastDot > 0 ? fileName.substring(0, lastDot) : fileName;
    }

    private List<Path> listSources(Path root, Set<Path> excludePaths) throws IOException {
        Set<Path> excludeAbsolute = excludePaths.stream()
                .map(p -> p.toAbsolutePath().normalize())
                .collect(Collectors.toSet());

        try (var stream = Files.walk(root)) {
            return stream.filter(path -> {
                if (!Files.isRegularFile(path)) {
                    return false;
                }

                Path absolute = path.toAbsolutePath().normalize();

                // Escludi file in directory escluse
                for (Path exclude : excludeAbsolute) {
                    if (absolute.startsWith(exclude)) {
                        return false;
                    }
                }

                return hasValidExtension(path);
            }).collect(Collectors.toList());
        }
    }

    private Path resolveImport(Path baseDir, String importSpec) {
        List<String> candidates = Arrays.asList(
                importSpec,
                importSpec + ".jsx",
                importSpec + ".tsx",
                importSpec + ".js",
                importSpec + "/index.jsx",
                importSpec + "/index.tsx",
                importSpec + "/index.js"
        );

        for (String candidate : candidates) {
            try {
                Path resolved = baseDir.resolve(candidate).normalize();
                if (Files.exists(resolved)) {
                    return resolved;
                }
            } catch (Exception ignored) {
                // Continua con il prossimo candidato
            }
        }

        return null;
    }

    private void moveUnused(Path includePath) {
        try {
            refactorer.moveIncludeFile(includePath, unusedTarget);
            log.accept("Spostato: " + rel(includePath));
        } catch (Exception e) {
            log.accept("Errore durante lo spostamento di " + rel(includePath) + ": " + e.getMessage());
        }
    }

    private boolean isUnder(Path path, Path root) {
        Path absolutePath = path.toAbsolutePath().normalize();
        Path absoluteRoot = root.toAbsolutePath().normalize();
        return absolutePath.startsWith(absoluteRoot);
    }

    private boolean isSameFile(Path path1, Path path2) {
        try {
            return Files.isSameFile(path1, path2);
        } catch (IOException e) {
            return false;
        }
    }

    private String rel(Path path) {
        try {
            return appRoot.relativize(path.toAbsolutePath().normalize()).toString();
        } catch (IllegalArgumentException e) {
            return path.toString();
        }
    }
}