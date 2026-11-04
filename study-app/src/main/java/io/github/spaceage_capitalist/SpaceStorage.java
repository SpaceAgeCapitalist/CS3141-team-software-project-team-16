package io.github.spaceage_capitalist;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SpaceStorage {

    private static final Path ROOT =
            Paths.get(System.getProperty("user.home"), "StudyApp", "spaces");

    static {
        try {
            Files.createDirectories(ROOT);
        } catch (IOException e) {
            throw new RuntimeException("Could not create data folder", e);
        }
    }

    /** Names become file/folder names, so reject anything that could break or escape the folder. */
    public static boolean isValidName(String name) {
        if (name == null) return false;
        String n = name.trim();
        if (n.isEmpty() || n.equals(".") || n.equals("..")) return false;
        return n.chars().noneMatch(c -> "\\/:*?\"<>|".indexOf(c) >= 0);
    }

    public static List<String> listSpaces() throws IOException {
        try (Stream<Path> s = Files.list(ROOT)) {
            return s.filter(Files::isDirectory)
                    .map(p -> p.getFileName().toString())
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .collect(Collectors.toList());
        }
    }

    /** Returns false if a space with that name already exists. */
    public static boolean createSpace(String name) throws IOException {
        Path dir = ROOT.resolve(name.trim());
        if (Files.exists(dir)) return false;
        Files.createDirectory(dir);
        return true;
    }

    public static void deleteSpace(String name) throws IOException {
        Path dir = ROOT.resolve(name);
        try (Stream<Path> s = Files.walk(dir)) {
            for (Path p : s.sorted(Comparator.reverseOrder()).collect(Collectors.toList())) {
                Files.delete(p);
            }
        }
    }

    public static List<Path> listFiles(String space) throws IOException {
        try (Stream<Path> s = Files.list(ROOT.resolve(space))) {
            return s.filter(Files::isRegularFile)
                    .sorted(Comparator.comparing(p -> p.getFileName().toString().toLowerCase()))
                    .collect(Collectors.toList());
        }
    }

    /** Copies the file into the space under the user's chosen name, keeping the original extension. */
    public static boolean addFile(String space, File source, String displayName) throws IOException {
        String original = source.getName();
        int dot = original.lastIndexOf('.');
        String ext = dot >= 0 ? original.substring(dot) : "";
        Path dest = ROOT.resolve(space).resolve(displayName.trim() + ext);
        if (Files.exists(dest)) return false;
        Files.copy(source.toPath(), dest);
        return true;
    }

    public static void deleteFile(Path file) throws IOException {
        Files.delete(file);
    }
}