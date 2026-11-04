package io.github.spaceage_capitalist;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/** One quiz question: its text, its type, and the correct answer. */
public class Question {

    public enum Type {
        MULTIPLE_CHOICE("Multiple choice"),
        TRUE_FALSE("True / False"),
        SHORT_ANSWER("Short answer");

        private final String label;

        Type(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label; // shown in the type drop-down
        }
    }

    private Type type = Type.MULTIPLE_CHOICE;
    private String text = "";
    private List<String> options = new ArrayList<>(); // multiple choice only
    private int correctIndex = -1;                    // multiple choice: option index; true/false: 0 = True, 1 = False
    private String answer = "";                       // short answer only

    public Type getType() { return type; }
    public void setType(Type type) { this.type = type; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public List<String> getOptions() { return options; }
    public void setOptions(List<String> options) { this.options = new ArrayList<>(options); }

    public int getCorrectIndex() { return correctIndex; }
    public void setCorrectIndex(int correctIndex) { this.correctIndex = correctIndex; }

    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }

    /** Returns null if the question is complete, otherwise a message saying what is missing. */
    public String validate() {
        if (text == null || text.isBlank()) {
            return "needs question text.";
        }
        switch (type) {
            case MULTIPLE_CHOICE:
                if (options.size() < 2) {
                    return "needs at least 2 answer choices.";
                }
                if (correctIndex < 0 || correctIndex >= options.size()) {
                    return "needs a correct answer selected (the selected choice must have text).";
                }
                break;
            case TRUE_FALSE:
                if (correctIndex != 0 && correctIndex != 1) {
                    return "needs True or False selected as the answer.";
                }
                break;
            case SHORT_ANSWER:
                if (answer == null || answer.isBlank()) {
                    return "needs a correct answer.";
                }
                break;
        }
        return null;
    }

    /** Writes this question into p using keys that start with prefix (e.g. "q.0."). */
    void writeTo(Properties p, String prefix) {
        p.setProperty(prefix + "type", type.name());
        p.setProperty(prefix + "text", text);
        switch (type) {
            case MULTIPLE_CHOICE:
                p.setProperty(prefix + "optionCount", String.valueOf(options.size()));
                for (int i = 0; i < options.size(); i++) {
                    p.setProperty(prefix + "option." + i, options.get(i));
                }
                p.setProperty(prefix + "correct", String.valueOf(correctIndex));
                break;
            case TRUE_FALSE:
                p.setProperty(prefix + "correct", String.valueOf(correctIndex));
                break;
            case SHORT_ANSWER:
                p.setProperty(prefix + "answer", answer);
                break;
        }
    }

    /** Reads a question back. Throws IllegalArgumentException if the data is missing or malformed. */
    static Question readFrom(Properties p, String prefix) {
        Question q = new Question();
        q.type = Type.valueOf(require(p, prefix + "type"));
        q.text = require(p, prefix + "text");
        switch (q.type) {
            case MULTIPLE_CHOICE:
                int count = Integer.parseInt(require(p, prefix + "optionCount"));
                List<String> opts = new ArrayList<>();
                for (int i = 0; i < count; i++) {
                    opts.add(require(p, prefix + "option." + i));
                }
                q.options = opts;
                q.correctIndex = Integer.parseInt(require(p, prefix + "correct"));
                break;
            case TRUE_FALSE:
                q.correctIndex = Integer.parseInt(require(p, prefix + "correct"));
                break;
            case SHORT_ANSWER:
                q.answer = require(p, prefix + "answer");
                break;
        }
        if (q.validate() != null) {
            throw new IllegalArgumentException("Question is incomplete: " + prefix);
        }
        return q;
    }

    private static String require(Properties p, String key) {
        String v = p.getProperty(key);
        if (v == null) {
            throw new IllegalArgumentException("Missing " + key);
        }
        return v;
    }
}