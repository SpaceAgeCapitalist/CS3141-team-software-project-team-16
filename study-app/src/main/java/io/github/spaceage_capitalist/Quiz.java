package io.github.spaceage_capitalist;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

/** A named list of questions. Converts to and from java.util.Properties for saving. */
public class Quiz {

    private final String name;
    private final List<Question> questions = new ArrayList<>();

    public Quiz(String name) {
        this.name = name;
    }

    public String getName() { return name; }

    public List<Question> getQuestions() { return Collections.unmodifiableList(questions); }

    public void addQuestion(Question q) { questions.add(q); }

    public Properties toProperties() {
        Properties p = new Properties();
        p.setProperty("questionCount", String.valueOf(questions.size()));
        for (int i = 0; i < questions.size(); i++) {
            questions.get(i).writeTo(p, "q." + i + ".");
        }
        return p;
    }

    public static Quiz fromProperties(String name, Properties p) throws IOException {
        try {
            Quiz quiz = new Quiz(name);
            int count = Integer.parseInt(p.getProperty("questionCount", ""));
            for (int i = 0; i < count; i++) {
                quiz.addQuestion(Question.readFrom(p, "q." + i + "."));
            }
            return quiz;
        } catch (IllegalArgumentException e) { // includes NumberFormatException
            throw new IOException("Not a valid quiz file: " + e.getMessage(), e);
        }
    }
}