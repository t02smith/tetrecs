package uk.ac.soton.comp1206.Components.multiplayer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import uk.ac.soton.comp1206.Components.Game.Colour;
import uk.ac.soton.comp1206.Components.Game.Grid;
import uk.ac.soton.comp1206.Components.Game.Grid.GridSize;
import uk.ac.soton.comp1206.game.GamePiece;

/**
 * Each user's grid will appear on the screen
 *  so that a user can see what other players are 
 *  doing in real time
 */
public class User extends BorderPane {
    private static final Logger logger = LogManager.getLogger(User.class);

    //That user's grid
    private Grid userGrid;

    //Their name
    private Label name;

    //Their current score
    private Label score;

    private int[][] currentGrid = new int[5][5];

    //Their lives will be shown as a red background that fades down

    public User(String name) {
        this.getStyleClass().add("opponent-grid");
        this.setMaxWidth(GridSize.SMALL.getSideLength()*5.5);
        this.setStyle("-fx-background-color: rgba(255, 0, 0, 0.3);");

        this.name = new Label(name);
        this.name.getStyleClass().add("opponent-name");

        this.score = new Label("0");
        this.score.getStyleClass().add("opponent-score");

        this.userGrid = new Grid(5, 5, GridSize.SMALL);
        this.userGrid.lockSelected();

        this.setCenter(this.userGrid);

        var props = new VBox(this.name, this.score);
        props.setAlignment(Pos.CENTER);

        this.setBottom(props);

        logger.info("User {} created", name);
    }

    /**
     * Updates the grid with the user's new grid
     * @param newGrid the new grid
     */
    public void applyGridChanges(int[][] newGrid) {
        //Empties the grid of all tiles
        //this.userGrid.clearAll();

        int changes = 0;

        //Loops through each square and adds colour where necessary
        for (int y = 0; y < newGrid.length; y++) {
            for (int x = 0; x < newGrid[0].length; x++) {
                if (newGrid[y][x] != this.currentGrid[y][x]) {
                    var colour = newGrid[y][x] == 0 ? 
                        Colour.TRANSPARENT :
                        GamePiece.getByValue(newGrid[y][x] - 1).getColour();
                    this.userGrid.changeTile(colour, x, y);

                    changes++;
                }
            }
        }

        logger.info("{} changes made to {}'s grid", changes, this.name.getText());
        this.currentGrid = newGrid;
    }

    public void setScore(int score) {
        Platform.runLater(() -> this.score.setText(String.valueOf(score)));
    }

    public void setLives(int lives) {
        switch(lives) {
            case 3:
                this.setStyle("-fx-background-color:rgba(255, 0, 0, 0.3);");
                break;
            case 2:
                this.setStyle("-fx-background-color: linear-gradient(to top, rgba(255, 0, 0, 0.3) 66%, rgba(255, 255, 255, 0.3) 66%, rgba(255, 255, 255, 0.3) 100%);");
                break;
            case 1:
                this.setStyle("-fx-background-color: linear-gradient(to top, rgba(255, 0, 0, 0.3) 33%, rgba(255, 255, 255, 0.3) 33%, rgba(255, 255, 255, 0.3) 100%);");
                break;
            case 0:
                this.setStyle("-fx-background-color: rgba(255, 255, 255, 0.3);");
                break;
        }
    }

    public void setName(String name) {
        Platform.runLater(() -> this.name.setText(name));
    }

    public String getName() {
        return this.name.getText();
    }
    
}
