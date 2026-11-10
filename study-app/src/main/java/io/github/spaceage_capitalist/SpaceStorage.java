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

import java.util.HashMap;
import java.util.Map;
import java.io.OutputStream;
import java.nio.file.StandardCopyOption;
import java.util.Properties;
import java.io.InputStream;
import java.util.ArrayList;

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

    //map decleration 
    private static Map<String, Map<String, Flashcards>> flashcardSets = new HashMap<>();

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

        //include removing flashcards
        flashcardSets.remove(name);
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

    public static boolean createTextFile(String space, String name) throws IOException {
        Path dest = ROOT.resolve(space).resolve(name.trim() + ".txt");
        if (Files.exists(dest)) return false;
        Files.createFile(dest);
        return true;
    }

    public static void deleteFile(Path file) throws IOException {
        Files.delete(file);
    }

    //start of logic for HashMap
    private static Map<String, Flashcards> getSetsForSpace(String space)
        throws IOException {

        if (!flashcardSets.containsKey(space)) {
            Map<String, Flashcards> sets = readFlashcardSets(space);
            flashcardSets.put(space, sets);
        }

        return flashcardSets.get(space);
    }   

    public static boolean addFlashcardSet(String space, Flashcards set)
        throws IOException {

        Map<String, Flashcards> sets = getSetsForSpace(space);

        if (sets.containsKey(set.getName())) {
            return false;
        }

        Map<String, Flashcards> updatedSets = new HashMap<>(sets);
        updatedSets.put(set.getName(), set);

        writeFlashcardSets(space, updatedSets);
        flashcardSets.put(space, updatedSets);

        return true;
    }

    private static void writeFlashcardSets(String space, 
        Map<String, Flashcards> sets) throws IOException {

        Path spaceFolder = ROOT.resolve(space);

        if(!Files.isDirectory(spaceFolder)) {
            throw new IOException("The study space no longer exists.");
        }
    
        Path folder = spaceFolder.resolve(".flashcards");
        Files.createDirectories(folder);

        Properties data = new Properties();
        data.setProperty("setCount", String.valueOf(sets.size()));

        int setIndex = 0;

        for(Flashcards set : sets.values()) {
            String prefix = "set." + setIndex + ".";

            data.setProperty(prefix + "name", set.getName());
            data.setProperty(prefix + "cardCount", String.valueOf(set.getCardCount()));

            for (int i = 0; i < set.getCardCount(); i++) {
                Flashcards.Card card = set.getCard(i);
                data.setProperty(prefix + "front." + i, card.getFront());
                data.setProperty(prefix + "back." + i, card.getBack());
            }

            setIndex++;
        }

        Path file = folder.resolve("sets.xml");
        Path temporaryFile = Files.createTempFile(folder, "sets-", ".tmp");

        try{
            try (OutputStream output = Files.newOutputStream(temporaryFile)) {
                data.storeToXML(output, "Study App Flashcards", "UTF-8");
            }

            Files.move(
            temporaryFile,
            file,
            StandardCopyOption.REPLACE_EXISTING
        );
        } 
        finally {
            Files.deleteIfExists(temporaryFile);
        }
    }
    private static String readRequiredProperty(Properties data, String key)
        throws IOException {

        String value = data.getProperty(key);

        if (value == null) {
            throw new IOException("Missing flashcard data: " + key);
        }

        return value;
    }

    private static Map<String, Flashcards> readFlashcardSets(String space)
        throws IOException {

        Map<String, Flashcards> sets = new HashMap<>();

        Path file = ROOT.resolve(space)
                .resolve(".flashcards")
                .resolve("sets.xml");

        if (Files.notExists(file)) {
            return sets;
        }

        Properties data = new Properties();

        try (InputStream input = Files.newInputStream(file)) {
            data.loadFromXML(input);
        }

        try {
            int setCount = Integer.parseInt(
                readRequiredProperty(data, "setCount")
            );

            if (setCount < 0) {
                throw new IOException("Invalid flashcard set count.");
            }

            for (int setIndex = 0; setIndex < setCount; setIndex++) {
                String prefix = "set." + setIndex + ".";

                String name = readRequiredProperty(data, prefix + "name");

                if (!isValidName(name) || sets.containsKey(name)) {
                    throw new IOException("Invalid or duplicate set name.");
                }

                Flashcards set = new Flashcards(name);

                int cardCount = Integer.parseInt(
                    readRequiredProperty(data, prefix + "cardCount")
                );

                if (cardCount < 0) {
                    throw new IOException("Invalid flashcard count.");
                }

                for (int i = 0; i < cardCount; i++) {
                    String front = readRequiredProperty(
                        data, prefix + "front." + i
                    );

                    String back = readRequiredProperty(
                        data, prefix + "back." + i
                    );

                    set.addCard(front, back);
                }

                sets.put(name, set);
            }

        } catch (NumberFormatException ex) {
            throw new IOException("Invalid number in flashcard data.", ex);
        }

        return sets;
    }

    public static List<Flashcards> listFlashcardSets(String space)
        throws IOException {

        List<Flashcards> sets = new ArrayList<>(
            getSetsForSpace(space).values()
        );

        sets.sort(Comparator.comparing(
            Flashcards::getName, String.CASE_INSENSITIVE_ORDER
        ));

        return sets;
    }

    public static boolean hasFlashcardSet(String space, String name)
            throws IOException {

        return getSetsForSpace(space).containsKey(name);
    }

    public static void updateFlashcardSet(String space, Flashcards set)
            throws IOException {

        Map<String, Flashcards> sets = getSetsForSpace(space);

        if (!sets.containsKey(set.getName())) {
            throw new IOException("This flashcard set no longer exists.");
        }

        Map<String, Flashcards> updatedSets = new HashMap<>(sets);
        updatedSets.put(set.getName(), set);

        writeFlashcardSets(space, updatedSets);
        flashcardSets.put(space, updatedSets);
    }

    public static void deleteFlashcardSet(String space, String name)
            throws IOException {

        Map<String, Flashcards> sets = getSetsForSpace(space);

        Map<String, Flashcards> updatedSets = new HashMap<>(sets);
        updatedSets.remove(name);

        writeFlashcardSets(space, updatedSets);
        flashcardSets.put(space, updatedSets);
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