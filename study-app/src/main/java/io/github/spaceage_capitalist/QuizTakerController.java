package io.github.spaceage_capitalist;

import java.util.ArrayList;
import java.util.List;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextArea;
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

        public QuestionCard() {
            heading.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
            questionText.setWrapText(true);
            questionText.setEditable(false);
            typeBox.setDisable(true);

            for (int i = 0; i < 4; i++) {
                optionRadios[i] = new RadioButton();
                optionRadios[i].setToggleGroup(mcGroup);
                optionFields[i] = new TextField();
                optionFields[i].setEditable(false);
                mcBox.getChildren().addAll(optionRadios[i], optionFields[i]);
            }

            trueButton.setToggleGroup(tfGroup);
            falseButton.setToggleGroup(tfGroup);
            tfBox.getChildren().addAll(trueButton, falseButton);

            saBox.getChildren().add(shortAnswerField);
            shortAnswerField.setEditable(false);

            answerArea.getChildren().addAll(mcBox, tfBox, saBox);
            root.getChildren().addAll(heading, questionText, typeBox, answerArea);
        }
        
        public void load(Question q) {
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
    }
}
