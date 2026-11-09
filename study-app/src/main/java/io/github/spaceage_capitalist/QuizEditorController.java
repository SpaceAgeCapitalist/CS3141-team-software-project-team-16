package io.github.spaceage_capitalist;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/** The page where the user fills in a new quiz's questions and answers. */
public class QuizEditorController {

    @FXML private Label titleLabel;
    @FXML private VBox questionContainer;

    private String spaceName;
    private String quizName;
    private boolean editing = false;
    private final List<QuestionCard> cards = new ArrayList<>();

    /** Called by App right after this page loads. */
    void setQuiz(String spaceName, String quizName) {
        this.spaceName = spaceName;
        this.quizName = quizName;
        titleLabel.setText("New Quiz: " + quizName);
        addQuestion(); // start with one blank question as the pre-made layout
    }
    /** Called by App instead of setQuiz when editing a saved quiz. */
    void editQuiz(String spaceName, Quiz quiz) {
        this.spaceName = spaceName;
        this.quizName = quiz.getName();
        this.editing = true;
        titleLabel.setText("Edit Quiz: " + quizName);
        for (Question q : quiz.getQuestions()) {
            QuestionCard card = new QuestionCard();
            card.load(q);
            cards.add(card);
            questionContainer.getChildren().add(card.root);
        }
        renumber();
    }

    @FXML
    private void addQuestion() {
        QuestionCard card = new QuestionCard();
        cards.add(card);
        questionContainer.getChildren().add(card.root);
        renumber();
    }

    private void removeCard(QuestionCard card) {
        cards.remove(card);
        questionContainer.getChildren().remove(card.root);
        renumber();
    }

    private void renumber() {
        for (int i = 0; i < cards.size(); i++) {
            cards.get(i).setNumber(i + 1);
        }
    }
    
    @FXML
    private void saveQuiz() {
        if (cards.isEmpty()) {
            Dialogs.error("Add at least one question before saving.");
            return;
        }

        // Every question must be complete, including its correct answer, before anything is saved.
        Quiz quiz = new Quiz(quizName);
        for (int i = 0; i < cards.size(); i++) {
            Question q = cards.get(i).toQuestion();
            String problem = q.validate();
            if (problem != null) {
                Dialogs.error("Question " + (i + 1) + " " + problem);
                return;
            }
            quiz.addQuestion(q);
        }

        try {
            if (editing) {
                SpaceStorage.updateQuiz(spaceName, quiz);
            } else if (!SpaceStorage.saveQuiz(spaceName, quiz)) {
                Dialogs.error("A quiz named \"" + quizName + "\" already exists in this study space.");
                return;
            }
            App.showSpace(spaceName);
        } catch (IOException ex) {
            Dialogs.error("Could not save quiz: " + ex.getMessage());
        }
    }

    @FXML
    private void cancel() {
        if (!Dialogs.confirm(editing ? "Discard your changes?" : "Discard this quiz? Nothing will be saved.")) return;
        try {
            App.showSpace(spaceName);
        } catch (IOException ex) {
            Dialogs.error("Could not return to the study space: " + ex.getMessage());
        }
    }

    /** One question's editing controls. Builds a Question from them with toQuestion(). */
    private class QuestionCard {

        final VBox root = new VBox(8);
        private final Label heading = new Label();
        private final TextArea questionText = new TextArea();
        private final ComboBox<Question.Type> typeBox =
                new ComboBox<>(FXCollections.observableArrayList(Question.Type.values()));
        private final VBox answerArea = new VBox(6);

        // Multiple choice: four choices, a radio button marks the correct one
        private final ToggleGroup mcGroup = new ToggleGroup();
        private final RadioButton[] optionRadios = new RadioButton[4];
        private final TextField[] optionFields = new TextField[4];
        private final VBox mcBox = new VBox(6);

        // True / False
        private final ToggleGroup tfGroup = new ToggleGroup();
        private final RadioButton trueButton = new RadioButton("True");
        private final RadioButton falseButton = new RadioButton("False");
        private final VBox tfBox = new VBox(6);

        // Short answer
        private final TextField shortAnswerField = new TextField();
        private final VBox saBox = new VBox(6);

        QuestionCard() {
            heading.setStyle("-fx-font-weight: bold;");
            Button remove = new Button("Remove");
            remove.setOnAction(e -> removeCard(this));
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            HBox header = new HBox(10, heading, spacer, remove);
            header.setAlignment(Pos.CENTER_LEFT);

            questionText.setPromptText("Type your question here");
            questionText.setPrefRowCount(2);
            questionText.setWrapText(true);

            typeBox.setValue(Question.Type.MULTIPLE_CHOICE);
            HBox typeRow = new HBox(10, new Label("Type:"), typeBox);
            typeRow.setAlignment(Pos.CENTER_LEFT);

            mcBox.getChildren().add(new Label("Fill in the choices, then select the correct one."));
            for (int i = 0; i < optionFields.length; i++) {
                optionRadios[i] = new RadioButton();
                optionRadios[i].setToggleGroup(mcGroup);
                optionFields[i] = new TextField();
                optionFields[i].setPromptText("Choice " + (char) ('A' + i));
                HBox.setHgrow(optionFields[i], Priority.ALWAYS);
                HBox row = new HBox(8, optionRadios[i], optionFields[i]);
                row.setAlignment(Pos.CENTER_LEFT);
                mcBox.getChildren().add(row);
            }

            trueButton.setToggleGroup(tfGroup);
            falseButton.setToggleGroup(tfGroup);
            tfBox.getChildren().addAll(new Label("Select the correct answer."),
                    new HBox(15, trueButton, falseButton));

            shortAnswerField.setPromptText("Correct answer");
            saBox.getChildren().addAll(new Label("Type the correct answer."), shortAnswerField);

            typeBox.valueProperty().addListener((obs, oldType, newType) -> showAnswerArea());
            showAnswerArea();

            root.getChildren().addAll(header, questionText, typeRow, answerArea);
            root.setPadding(new Insets(12));
            root.setStyle("-fx-border-color: #bbbbbb; -fx-border-radius: 6; -fx-background-radius: 6;");
        }

        void setNumber(int n) {
            heading.setText("Question " + n);
        }

        /** Fills the controls from an existing question. */
        void load(Question q) {
            questionText.setText(q.getText());
            typeBox.setValue(q.getType());
            switch (q.getType()) {
                case MULTIPLE_CHOICE:
                    for (int i = 0; i < optionFields.length && i < q.getOptions().size(); i++) {
                        optionFields[i].setText(q.getOptions().get(i));
                    }
                    if (q.getCorrectIndex() >= 0 && q.getCorrectIndex() < optionRadios.length) {
                        optionRadios[q.getCorrectIndex()].setSelected(true);
                    }
                    break;
                case TRUE_FALSE:
                    if (q.getCorrectIndex() == 0) {
                        trueButton.setSelected(true);
                    } else if (q.getCorrectIndex() == 1) {
                        falseButton.setSelected(true);
                    }
                    break;
                case SHORT_ANSWER:
                    shortAnswerField.setText(q.getAnswer());
                    break;
            }
        }

        /** Shows only the answer controls that match the chosen question type. */
        private void showAnswerArea() {
            answerArea.getChildren().clear();
            switch (typeBox.getValue()) {
                case MULTIPLE_CHOICE:
                    answerArea.getChildren().add(mcBox);
                    break;
                case TRUE_FALSE:
                    answerArea.getChildren().add(tfBox);
                    break;
                case SHORT_ANSWER:
                    answerArea.getChildren().add(saBox);
                    break;
            }
        }

        /** Reads the controls into a Question. It may be incomplete; call validate() on the result. */
        Question toQuestion() {
            Question q = new Question();
            q.setType(typeBox.getValue());
            q.setText(questionText.getText().trim());
            switch (typeBox.getValue()) {
                case MULTIPLE_CHOICE: {
                    // Blank choices are skipped; the correct index counts only the filled ones.
                    List<String> options = new ArrayList<>();
                    int correct = -1;
                    for (int i = 0; i < optionFields.length; i++) {
                        String t = optionFields[i].getText().trim();
                        if (t.isEmpty()) continue;
                        if (optionRadios[i].isSelected()) correct = options.size();
                        options.add(t);
                    }
                    q.setOptions(options);
                    q.setCorrectIndex(correct);
                    break;
                }
                case TRUE_FALSE:
                    q.setCorrectIndex(trueButton.isSelected() ? 0 : falseButton.isSelected() ? 1 : -1);
                    break;
                case SHORT_ANSWER:
                    q.setAnswer(shortAnswerField.getText().trim());
                    break;
            }
            return q;
        }
    }
}