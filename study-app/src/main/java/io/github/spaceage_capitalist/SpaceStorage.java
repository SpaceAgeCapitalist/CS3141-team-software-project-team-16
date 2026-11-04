package io.github.spaceage_capitalist;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.nio.file.StandardCopyOption;

public class SpaceStorage {

    public static final String QUIZ_EXTENSION = ".quiz";

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

    //Quizzes
    // A quiz is stored as one "<name>.quiz" file inside its study space folder.

    public static boolean isQuizFile(Path file) {
        return file.getFileName().toString().toLowerCase().endsWith(QUIZ_EXTENSION);
    }

    /** The file name without the .quiz extension. */
    public static String quizName(Path file) {
        String n = file.getFileName().toString();
        return n.substring(0, n.length() - QUIZ_EXTENSION.length());
    }

    public static boolean quizExists(String space, String quizName) {
        return Files.exists(ROOT.resolve(space).resolve(quizName.trim() + QUIZ_EXTENSION));
    }

    /** Returns false if a quiz with that name already exists in the space. */
    public static boolean saveQuiz(String space, Quiz quiz) throws IOException {
        Path dest = ROOT.resolve(space).resolve(quiz.getName().trim() + QUIZ_EXTENSION);
        try (Writer w = Files.newBufferedWriter(dest, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW)) {
            quiz.toProperties().store(w, "Study App quiz");
        } catch (FileAlreadyExistsException e) {
            return false;
        }
        return true;
    }

    /** Replaces an existing quiz with new contents. Writes to a temp file first so a crash can't leave a half-written quiz. */
    public static void updateQuiz(String space, Quiz quiz) throws IOException {
        String base = quiz.getName().trim() + QUIZ_EXTENSION;
        Path dest = ROOT.resolve(space).resolve(base);
        Path temp = ROOT.resolve(space).resolve(base + ".tmp");
        try (Writer w = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
            quiz.toProperties().store(w, "Study App quiz");
        }
        Files.move(temp, dest, StandardCopyOption.REPLACE_EXISTING);
    }

    public static Quiz loadQuiz(Path file) throws IOException {
        Properties p = new Properties();
        try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            p.load(r);
        }
        return Quiz.fromProperties(quizName(file), p);
    }
}