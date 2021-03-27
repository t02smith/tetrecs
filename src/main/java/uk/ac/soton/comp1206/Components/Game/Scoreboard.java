package uk.ac.soton.comp1206.Components.Game;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;

import javafx.application.Platform;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import uk.ac.soton.comp1206.Utility.MutablePair;

/**
 * Scoreboard to display a list of scores
 */
public class Scoreboard extends VBox {
    //Name of the scoreboard
    private String name;

    //List of scores
    private SimpleListProperty<MutablePair<String, Integer>> scores;

    //Assigns each user a different colour
    private HashMap<String, Color> colours = new HashMap<>();

    //Different colours the scores can be
    private static final Color[] scoreColors = {
        Color.HOTPINK, Color.RED, Color.ORANGE, Color.YELLOW, Color.YELLOWGREEN, Color.LIME, Color.GREEN, Color.DARKGREEN, Color.CYAN, Color.BLUE
    };

    /**
     * Constructor to create scoreboard from scores
     * @param scores List of scores expect <name>:<score>\n
     */
    public Scoreboard(String name, HashMap<String, Integer> scores) {
        this.name = name;
        this.createScoreList(scores);
        this.build();
    }

    /**
     * Creates a scoreboard from a list of scores as <name>:<score>
     *  This is how it is read from the file and received from online
     * @param name The name of the scoreboard
     * @param scores The list of unformatted scores
     */
    public Scoreboard(String name, Collection<String> scores) {
        this.name = name;

        //Changes the scores list to a hashmap
        var scoreMap = new HashMap<String, Integer>();
        scores.forEach(score -> {
            var scoreSplit = score.split(":");
            scoreMap.put(scoreSplit[0], Integer.parseInt(scoreSplit[1]));
        });

        this.createScoreList(scoreMap);
        this.build();

    }

    /**
     * Builds the scoreboard
     */
    public void build() {
        this.updateScoreboard();

        this.getStyleClass().add("scoreboard");
        this.setAlignment(Pos.TOP_CENTER);
        this.setSpacing(15);

    }

    /**
     * Creates the simplelistproperty from a given array
     * @param scores the list of scores
     */
    private void createScoreList(HashMap<String, Integer> scores) {
        //Collects names and scores from a given set of lines (i.e. from the server)       
        var pairList = new ArrayList<MutablePair<String, Integer>>();

        int counter = 0;
        for (String name: scores.keySet()) {
            pairList.add(
                new MutablePair<String, Integer>(name, scores.get(name))
            );

            //Each user has a unique colour so that it is obvious when
            // scoreboard changes occur
            this.colours.put(name, scoreColors[counter]);
            counter++;
        }
        
        ObservableList<MutablePair<String, Integer>> observableList = FXCollections.observableArrayList(pairList);

        //Sorts the scores highest to lowest
        Comparator<MutablePair<String, Integer>> comparator = Comparator.comparingInt(MutablePair::getValue);
        FXCollections.sort(observableList, comparator.reversed());

        this.scores = new SimpleListProperty<>(observableList);

    }

    /**
     * Updates the score for a user
     * @param name The user's name
     * @param score The user's new score
     */
    public void updateScore(String name, int score) {
        for (MutablePair<String, Integer> user: this.scores) {
            if (user.getKey().equals(name)) {
                user.setValue(score);
                Platform.runLater(() -> this.updateScoreboard());
                break;
            }
        }
    }

    /**
     * When a change is made to the scoreboard update it
     */
    private void updateScoreboard() {
        this.getChildren().clear();

        Comparator<MutablePair<String, Integer>> comparator = Comparator.comparingInt(MutablePair::getValue);
        FXCollections.sort(this.scores, comparator.reversed());

        var scoreBoard = new GridPane();

        //Finds the top 10 or less if there aren't 10
        int max = this.scores.getSize() < 10 ? scores.getSize(): 10;
        for (int i = 0; i < max; i++) {
            var score = this.scores.get(i);
            var colour = this.colours.get(score.getKey());

            //Adds the position, name and score on the scoreboard with a selected colour
            var pos = new Label(String.valueOf(i+1) + " ");
            pos.setStyle("-fx-text-fill: '#" + colour.toString().substring(2) + "';");
            scoreBoard.add(pos, 0, i);

            var username = new Label(score.getKey() + " ");
            username.setStyle("-fx-text-fill: '#" + colour.toString().substring(2) + "';");
            scoreBoard.add(username, 1, i);

            var scoreLbl = new Label(score.getValue().toString());
            scoreLbl.setStyle("-fx-text-fill: '#" + colour.toString().substring(2) + "';");
            scoreBoard.add(scoreLbl, 2, i);
        }

        var title = new Label(this.name);
        title.setStyle("-fx-text-fill: '#ffffff';");

        this.getChildren().addAll(title, scoreBoard);
    }

    /**
     * Returns the highest score
     * @return the high score
     */
    public MutablePair<String, Integer> getHighScore() {
        return this.scores.get(0);
    }

}
