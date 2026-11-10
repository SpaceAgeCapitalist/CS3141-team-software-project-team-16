package io.github.spaceage_capitalist;

import java.io.IOException;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

public class FlashcardController {

    @FXML
    private Label setNameLabel;
    @FXML
    private Label cardNumberLabel;
    @FXML
    private TextArea frontTextArea;
    @FXML
    private TextArea backTextArea;

    private Flashcards flashcardSet;
    private int currentIndex = 0;
    private String spaceName;
    private boolean editingExisting = false;

    // this gets the spacename that the set is in so that later they can be stored properly
    public void setSpaceName(String spaceName) {
        this.spaceName = spaceName;
    }

    // sotres if the user has edited the set yet
    public void setEditingExisting(boolean editingExisting) {
        this.editingExisting = editingExisting;
    }

    /*
    this set:
    - creates a copy of the saved set
    - copies over all the saved cards into the new set
        - it starts at index 0 
    - adds a blank draft if the set has zero cards
    - then displays the first card and name of the set
    */
    public void setFlashcardSet(Flashcards source) {
        this.flashcardSet = new Flashcards(source.getName());

        for (int i = 0; i < source.getCardCount(); i++) {
            Flashcards.Card card = source.getCard(i);
            this.flashcardSet.addCard(card.getFront(), card.getBack());
        }

        currentIndex = 0;

        if (this.flashcardSet.getCardCount() == 0) {
            this.flashcardSet.addCard("", "");
        }

        setNameLabel.setText(this.flashcardSet.getName());
        showCurrentCard();
    }

    // this method uses the currentIndex and retrieves the information for the current card and displayes it
    private void showCurrentCard() {
        Flashcards.Card card = flashcardSet.getCard(currentIndex);

        frontTextArea.setText(card.getFront());
        backTextArea.setText(card.getBack());

        cardNumberLabel.setText(
            "Card " + (currentIndex + 1)
            + " of " + flashcardSet.getCardCount()
        );
    }

    // this updates the current card based off of user input
    private void updateCurrentCard() {
        Flashcards.Card card = flashcardSet.getCard(currentIndex);

        card.setFront(frontTextArea.getText());
        card.setBack(backTextArea.getText());
    }

    // using the previous card button it displays the previous card in the set order
    @FXML
    private void previousCard() {
        updateCurrentCard();

        if (currentIndex > 0) {
            currentIndex--;
            showCurrentCard();
        }
    }

    // using the next card button it displays the next card in the set order
    @FXML
    private void nextCard() {
        updateCurrentCard();

        if (currentIndex == flashcardSet.getCardCount() - 1) {
            flashcardSet.addCard("", "");
        }

        currentIndex++;
        showCurrentCard();
    }

    // this deletes the current card the user is looking at
    @FXML 
    private void deleteCard(){
        flashcardSet.removeCard(currentIndex);

        if(flashcardSet.getCardCount() == 0){
            flashcardSet.addCard("", "");
            currentIndex = 0;
        }
        else if (currentIndex >= flashcardSet.getCardCount()) {
            currentIndex = flashcardSet.getCardCount() - 1;
        }

        showCurrentCard();
    }
    
    
    @FXML
    private void finish() {
        
        updateCurrentCard();
        // this is the set that will be saved after everything
        Flashcards completedSet = new Flashcards(flashcardSet.getName());

        // this for loop cycles through every ccard in the current set
        for (int i = 0; i < flashcardSet.getCardCount(); i++) {
            
            // these 3 lines retrieve the current card and its text
            Flashcards.Card card = flashcardSet.getCard(i);
            String front = card.getFront();
            String back = card.getBack();

            // checks if either side is empty
            boolean frontEmpty = front.trim().isEmpty();
            boolean backEmpty = back.trim().isEmpty();

            // skips empty cards
            if (frontEmpty && backEmpty) {
                continue;
            }

            // this stops the user if one of the cards is missing a front or a back 
            if (frontEmpty || backEmpty) {
                currentIndex = i;
                showCurrentCard();

                Dialogs.error(
                    "Please fill in both sides of card " + (i + 1) + "."
                );
                return;
            }

            completedSet.addCard(front, back);
        }

        // cant save a set that has no cards
        if (completedSet.getCardCount() == 0) {
            Dialogs.error("Please complete at least one flashcard.");
            return;
        }

        // this is the logic for saving the card 
        try {
            if (editingExisting) {
                SpaceStorage.updateFlashcardSet(spaceName, completedSet);
            } else {
                if (!SpaceStorage.addFlashcardSet(spaceName, completedSet)) {
                    Dialogs.error(
                        "A flashcard set with that name already exists "
                        + "in this study space."
                    );
                    return;
                }
            }

            Stage window = (Stage) frontTextArea.getScene().getWindow();
            window.close();

        } 
        // eerror if the method returns false
        catch (IOException ex) {
            Dialogs.error("Could not save flashcards: " + ex.getMessage());
        }
    }
}