package io.github.spaceage_capitalist;

import java.util.ArrayList;

// this is the flashcard, this class call on other methods to create the flashcard set
public class Flashcards {

    private String name;

    // this is what stores the cards in order 
    private ArrayList<Card> cards = new ArrayList<>();
    
    // constructor that creates the flashcards object
    public Flashcards(String name) {
        this.name=name;
    }
    public String getName() {
        return name;
    }

    // this creates a new card with a front and back attached
    public void addCard(String front, String back) {
        Card card = new Card(front, back);
        cards.add(card);
    }
    public Card getCard(int index) {
        return cards.get(index);
    }
    
    // returns how many cards are in the set as a integer
    public int getCardCount() {
        return cards.size();
    }
    // deletes a card
    public void removeCard(int index) {
        cards.remove(index);
    }

    public static class Card {
        private String front;
        private String back;

        // contructor
        public Card(String front, String back){
            this.front = front;
            this.back = back;
        }

        // getters
        public String getFront() {
            return front;
        }
        public String getBack() {
            return back;
        }

        // setters
        public void setFront(String front) {
            this.front = front;
        }
        public void setBack(String back) {
            this.back = back;
        }


    }

}
