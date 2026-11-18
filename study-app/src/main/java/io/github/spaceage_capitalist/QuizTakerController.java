package io.github.spaceage_capitalist;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
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
import javafx.scene.layout.VBox;
import javafx.scene.layout.Region;

/**
 * QuizTakerController
 */
public class QuizTakerController {
    
    @FXML private VBox root;
    @FXML private Label quizNameLabel;
    @FXML private Label questionLabel;
    @FXML private VBox questionContainer;
    @FXML private Button nextButton;

    private String spaceName;
    private String quizName;
    private final List<QuestionCard> cards = new ArrayList<>();
    private int currentQuestionIndex = 0;


   public void takeQuiz(String spaceName, Quiz quiz) {
        this.spaceName = spaceName;
        this.quizName = quiz.getName();
        quizNameLabel.setText("Quiz: " + quizName);
        for (Question question : quiz.getQuestions()) {
            cards.add(new QuestionCard());
        }
        showCurrentQuestion();
    }

    private void showCurrentQuestion() {
        questionContainer.getChildren().clear();
        if (currentQuestionIndex < cards.size()) {
            QuestionCard card = cards.get(currentQuestionIndex);
            questionLabel.setText("Question " + (currentQuestionIndex + 1) + ":");
            card.loadQuestion();
            questionContainer.getChildren().add(card);
        } else {
            questionLabel.setText("Quiz Completed!");
            nextButton.setDisable(true);
        }
    }

    @FXML
    private void handleNextButton() {
        if (currentQuestionIndex < cards.size()) {
            QuestionCard card = cards.get(currentQuestionIndex);
            card.saveAnswer();
            currentQuestionIndex++;
            showCurrentQuestion();
        }
    }

    @FXML 
    private void saveProgress() {
        // Save the user's answers to the quiz
        Quiz quiz = new Quiz(quizName);
        for (QuestionCard card : cards) {
            quiz.addQuestion(card.getQuestion());
        }
        // Here you would implement saving the quiz progress to a file or database
       try{
            SpaceStorage.saveQuiz(spaceName, quiz);
        } catch (Exception e) {
            Dialogs.error("Error saving quiz progress: " + e.getMessage());
       }
    }
    @FXML
    private void cancel() {

        try {
            App.showSpace(spaceName);
        } catch (IOException ex) {
            Dialogs.error("Could not return to the study space: " + ex.getMessage());
        }
    }

    @FXML 
    private void submitQuiz() {
        // Save the user's answers to the quiz
        Quiz quiz = new Quiz(quizName);
        for (QuestionCard card : cards) {
            card.saveAnswer();
            quiz.addQuestion(card.getQuestion());
        }
        // Here you would implement saving the completed quiz to a file or database
        try {
            SpaceStorage.saveQuiz(spaceName, quiz);
            Dialogs.info("Quiz submitted successfully!");
            App.showSpace(spaceName);
        } catch (Exception e) {
            Dialogs.error("Error submitting quiz: " + e.getMessage());
        }
    }

    @FXML 
    private void previousQuestion() {
        if (currentQuestionIndex > 0) {
            currentQuestionIndex--;
            showCurrentQuestion();
        }
    }
    @FXML 
    private void nextQuestion() {
        if (currentQuestionIndex < cards.size() - 1) {
            currentQuestionIndex++;
            showCurrentQuestion();
        }
    }
    @FXML 
    private void showFlashcards() {
        Dialogs.info("Flashcards feature is not yet implemented.");
    }

    private class QuestionCard extends VBox {
        final VBox root = new VBox(8);
        private final Label heading = new Label();
        private final TextArea questionText = new TextArea();
        private final ComboBox<Question.Type> typeBox = new ComboBox<>(FXCollections.observableArrayList(Question.Type.values()));
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

        QuestionCard(){
             heading.setStyle("-fx-font-weight: bold;");

            
            Button saveButton = new Button("Save Progress");
            saveButton.setOnAction(e -> saveProgress());
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            HBox header = new HBox(10, heading, spacer, saveButton);
            header.setAlignment(Pos.CENTER_LEFT);


        }

        void loadQuestion() {
            Question question = getQuestion();
            heading.setText("Question " + (currentQuestionIndex + 1) + ":");
            questionText.setText(question.getText());
            typeBox.setValue(question.getType());
            answerArea.getChildren().clear();
            switch (question.getType()) {
                case MULTIPLE_CHOICE:
                    loadMultipleChoice(question);
                    break;
                case TRUE_FALSE:
                    loadTrueFalse(question);
                    break;
                case SHORT_ANSWER:
                    loadShortAnswer(question);
                    break;
            }
        }

        void loadMultipleChoice(Question question) {
            mcBox.getChildren().clear();
            for (int i = 0; i < 4; i++) {
                if (optionRadios[i] == null) {
                    optionRadios[i] = new RadioButton();
                    optionRadios[i].setToggleGroup(mcGroup);
                }
                if (optionFields[i] == null) {
                    optionFields[i] = new TextField();
                }
                if (i < question.getOptions().size()) {
                    optionFields[i].setText(question.getOptions().get(i));
                    optionRadios[i].setSelected(i == question.getCorrectIndex());
                } else {
                    optionFields[i].setText("");
                    optionRadios[i].setSelected(false);
                }
                HBox optionRow = new HBox(6, optionRadios[i], optionFields[i]);
                mcBox.getChildren().add(optionRow);
            }
            answerArea.getChildren().add(mcBox);
        }
        void loadTrueFalse(Question question) {
            tfBox.getChildren().clear();
            trueButton.setToggleGroup(tfGroup);
            falseButton.setToggleGroup(tfGroup);
            if (question.getCorrectIndex() == 0) {
                trueButton.setSelected(true);
            } else if (question.getCorrectIndex() == 1) {
                falseButton.setSelected(true);
            }
            tfBox.getChildren().addAll(trueButton, falseButton);
            answerArea.getChildren().add(tfBox);
        }
        void loadShortAnswer(Question question) {
            saBox.getChildren().clear();
            shortAnswerField.setText(question.getAnswer());
            saBox.getChildren().add(shortAnswerField);
            answerArea.getChildren().add(saBox);
        }
        Question getQuestion() {
            Question question = new Question();
            question.setText(questionText.getText());
            question.setType(typeBox.getValue());
            switch (question.getType()) {
                case MULTIPLE_CHOICE:
                    List<String> options = new ArrayList<>();
                    for (int i = 0; i < 4; i++) {
                        String optionText = optionFields[i].getText();
                        if (!optionText.isBlank()) {
                            options.add(optionText);
                        }
                    }
                    question.setOptions(options);
                    int selectedIndex = -1;
                    for (int i = 0; i < optionRadios.length; i++) {
                        if (optionRadios[i].isSelected()) {
                            selectedIndex = i;
                            break;
                        }
                    }
                    question.setCorrectIndex(selectedIndex);
                    break;
                case TRUE_FALSE:
                    if (trueButton.isSelected()) {
                        question.setCorrectIndex(0);
                    } else if (falseButton.isSelected()) {
                        question.setCorrectIndex(1);
                    } else {
                        question.setCorrectIndex(-1); // No selection
                    }
                    break;
                case SHORT_ANSWER:
                    question.setAnswer(shortAnswerField.getText());
                    break;
            }
            return question;
        }
        void saveAnswer() {
            Question question = getQuestion();
            cards.set(currentQuestionIndex, this);
        }
        
    }



}

