package io.github.spaceage_capitalist;

import java.util.ArrayList;
import java.util.List;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.VBox;
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
    private Quiz quiz;
    private int currentQuestionIndex = 0;

    public void takeQuiz(String spaceName, Quiz quiz) {
        this.spaceName = spaceName;
        this.quizName = quiz.getName();
        questionLabel.setText("Take Quiz: " + quizName);
        for (Question q : quiz.getQuestions()) {
            QuestionCard card = new QuestionCard();
            card.load(q);
            cards.add(card);
            questionContainer.getChildren().add(card.root);
        }
    }

    private void showQuestion() {
        for (int i = 0; i < cards.size(); i++) {
            cards.get(i).root.setVisible(i == currentQuestionIndex);
        }
        nextButton.setDisable(currentQuestionIndex >= cards.size() - 1);
    }    
   

    @FXML
    private void handleNextButton() {
        currentQuestionIndex++;
        showQuestion();
    }
    
    public class QuestionCard {
        @FXML private VBox root;
        @FXML private Label questionTextLabel;
        @FXML private VBox answerContainer;

        private Question question;

        public void load(Question question) {
            this.question = question;
            questionTextLabel.setText(question.getText());
            answerContainer.getChildren().clear();
            switch (question.getType()) {
                case MULTIPLE_CHOICE:
                    ToggleGroup group = new ToggleGroup();
                    for (String option : question.getOptions()) {
                        RadioButton rb = new RadioButton(option);
                        rb.setToggleGroup(group);
                        answerContainer.getChildren().add(rb);
                    }
                    break;
                case TRUE_FALSE:
                    ToggleGroup tfGroup = new ToggleGroup();
                    RadioButton trueRb = new RadioButton("True");
                    trueRb.setToggleGroup(tfGroup);
                    RadioButton falseRb = new RadioButton("False");
                    falseRb.setToggleGroup(tfGroup);
                    answerContainer.getChildren().addAll(trueRb, falseRb);
                    break;
                case SHORT_ANSWER:
                    TextField textField = new TextField();
                    answerContainer.getChildren().add(textField);
                    break;
            }
        }
    }
}
