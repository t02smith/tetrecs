package uk.ac.soton.comp1206.game;

import java.util.ArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.util.Duration;
import uk.ac.soton.comp1206.Event.KeyBinding;
import uk.ac.soton.comp1206.Network.Communicator;
import uk.ac.soton.comp1206.Network.NetworkProtocol;
import uk.ac.soton.comp1206.Scenes.ChallengeScene;
import uk.ac.soton.comp1206.Scenes.ScoresScene;
import uk.ac.soton.comp1206.Utility.Media;
import uk.ac.soton.comp1206.ui.GameWindow;

public class Game {
    protected static final Logger logger = LogManager.getLogger(Game.class);

    protected final Communicator communicator;

    //Game properties
    protected SimpleIntegerProperty score = new SimpleIntegerProperty(0);
    protected SimpleIntegerProperty level = new SimpleIntegerProperty(0);
    protected SimpleIntegerProperty lives = new SimpleIntegerProperty(3);
    protected SimpleIntegerProperty multiplier = new SimpleIntegerProperty(1);

    //Whether the game is still on going
    protected boolean gameOver;

    //The next piece to be played
    protected GamePiece currentPiece;

    //Piece in reserve that can be switched to
    protected GamePiece reservePiece;

    //Main game loop timeline
    protected Timeline timeline;

    //visuals
    protected GameWindow gameWindow;
    protected ChallengeScene gameScene;
    private ScoresScene scoresScene;

    public Game(GameWindow gameWindow, Communicator communicator) {
        logger.info("Starting game");
        this.gameWindow = gameWindow;
        this.communicator = communicator;

    }

    public void buildGame() {
        this.setupCommunicator();

        this.gameScene = new ChallengeScene(this.gameWindow);
        this.gameWindow.setGameScene(this.gameScene);

        //Creates a score scene so we can collect the scores
        this.scoresScene = this.gameWindow.getScoresScene();
        this.communicator.send("HISCORES");

        this.gameScene.setLocalScores(this.scoresScene.getLocalScoreboard());
        this.gameScene.setHighScore(this.scoresScene.getHighScore());

        //Submits a score
        this.scoresScene.addSubmitScoreListener((name, score) -> {
            logger.info("Submitting new score");
            this.communicator.send(
                String.format("HISCORE %s:%d", name, score)
            );
        });


        //When the game is started//
        this.gameWindow.addGameStartListener(() -> {
            this.nextPiece();
            this.nextPiece();

            this.gameOver = false;

            //Plays background music
            Media.playMusic("game.wav");

            //start timer
            this.gameLoop();
        });

        this.setKeyBindings();
        this.setUserPropertyListeners();
        this.setTileClickListeners();
        this.gameScene.build();

    }

    /**
     * Sets the game's keybindings
     */
    protected void setKeyBindings() {
        KeyBinding.ROTATE_LEFT.setEvent(() -> {
            this.currentPiece.rotateLeft();
            this.gameScene.setNextPiece(this.currentPiece);
        });

        KeyBinding.ROTATE_RIGHT.setEvent(() -> {
            this.currentPiece.rotateRight();
            this.gameScene.setNextPiece(this.currentPiece);
        });

        KeyBinding.SWAP.setEvent(() -> this.swapNextPiece());

        KeyBinding.PLACE.setEvent(() -> this.insertSelected());

        KeyBinding.MOVE_UP.setEvent(() -> this.moveSelected(0, -1));
        KeyBinding.MOVE_LEFT.setEvent(() -> this.moveSelected(-1, 0));
        KeyBinding.MOVE_RIGHT.setEvent(() -> this.moveSelected(1, 0));
        KeyBinding.MOVE_DOWN.setEvent(() -> this.moveSelected(0, 1));


        KeyBinding.QUIT.setEvent(() -> {
            logger.info("Returning to menu");
            this.gameWindow.loadMenu();
            this.resetGame();
        });

        this.gameScene.setOnKeyReleased(event -> {
            KeyBinding.executeEvent(event.getCode());
        });

    }

    /**
     * Sets up the actions when a tile is clicked
     * What happens depends on what grid
     */
    protected void setTileClickListeners() {
        //On the game grid
        this.gameScene.addTileClickListener("game-grid", (x, y) -> {
            this.insertPiece(x, y);
        });

        //On the next piece grid
        this.gameScene.addTileClickListener("next-piece", (x, y) -> {
            if (x == 0) { //click middle square to rotate
                this.currentPiece.rotateLeft();
                this.gameScene.setNextPiece(this.currentPiece);
            } else {
                this.currentPiece.rotateRight();
                this.gameScene.setNextPiece(this.currentPiece);
            }
        });

        //On the reserve piece grid
        this.gameScene.addTileClickListener("reserve-piece", (x, y) -> {
            this.swapNextPiece();
        });
    }

    /**
     * Sets the properties for altering the user's 
     */
    protected void setUserPropertyListeners() {
        //When the score updates
        this.score.addListener(event -> {
            this.gameScene.updateScore(score.get());
            if (this.score.get()/1000 != this.level.get()) {
                this.level.set(this.score.get()/1000);
                logger.info("level increased to {}", this.level.get());
            } 
        });
        
        //When the user levels up
        this.level.addListener(event -> {
            if (this.timeline == null) return;

            //Decrease the time given to place a piece
            this.timeline.stop();
            this.timeline.getKeyFrames().set(1, this.updateTime());
            logger.info("Time decreased to {}ms", this.getTimerDelay());

        });

        //When a life is lost
        this.lives.addListener(event -> {
            this.gameScene.loseLife();
        });

        //When the multiplier changes
        this.multiplier.addListener(event -> {
            this.gameScene.updateMultiplier(this.multiplier.get());
        });
    }


    /**
     * Game loop to keep track of the time
     */
    protected void gameLoop() {
        var timer = this.gameScene.getTimer();      

        this.timeline = new Timeline(
            new KeyFrame(Duration.ZERO, new KeyValue(timer.progressProperty(), 1)),
            this.updateTime()
        );

        this.timeline.setCycleCount(Animation.INDEFINITE);
        this.timeline.play();
    }

    /**
     * Updates the time after a piece is played
     * @return
     */
    protected KeyFrame updateTime() {
        return new KeyFrame(Duration.millis(this.getTimerDelay()), e -> {
            this.lives.set(this.lives.get() -1);
            logger.info("Life lost! {} remaining", this.lives.get());

            if (this.lives.get() < 0) this.stopGame();
            else {
                this.nextPiece();
                this.multiplier.set(1);
            }
        }, new KeyValue(this.gameScene.getTimer().progressProperty(), 0));
    }

    public void stopGame() {
        logger.info("GAME OVER");
        this.timeline.stop();
        this.gameOver = true;

        //Bring up scoreboard
        this.scoresScene.setUserScore(this.score.get());
        this.scoresScene.setHasPlayed(true);

        //Resets the game for the next round
        this.resetGame();
        this.gameWindow.loadScene(this.scoresScene);
    }

    public void resetGame() {
        logger.info("Resetting game");
        this.timeline.stop();
        this.score.set(0);
        this.lives.set(3);
        this.level.set(0);
        this.multiplier.set(1);
    }

    /**
     * Called when a piece needs to be inserted
     * @param x
     * @param y
     */
    protected void insertPiece(int x, int y) {
        if (this.placePiece(x, y)) {
            this.afterPiece();

            //Grabs the next piece
            this.nextPiece();
    
            //Resets the timer
            this.timeline.playFromStart();
        }
    }

    /**
     * Attempts to insert a piece at a given square
     * @param x 
     * @param y
     */
    protected boolean placePiece(int x, int y) {
        if (this.gameOver) return false;

        logger.info("Location {} {}", x, y);

        //Blocks that can be added to the game board go here
        //We have to wait for all squares to be checked
        var buffer = new ArrayList<int[]>();

        int[][] pieceBlocks = this.currentPiece.getBlocks();
        var board = this.gameScene.getBoard();

        //Checks the availability of every tile
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                //Checks if the position is going to be filled in
                if (pieceBlocks[row][column] == 1) {
                    //Checks if the position can be filled in
                    if (board.canPlayPiece(x+column-1, y+row-1)) {
                        buffer.add(new int[] {x+column-1, y+row-1});
                    } else {
                        //The whole shape must fit in to insert the block
                        logger.error("Failed to add tile.");
                        return false;
                    }
                }
            }
        }

        this.fillTiles(buffer);
        logger.error("Hello");
        return true;
    }

    /**
     * Called to fill a given set of tiles from the board
     * @param buffer The tiles to be removed
     */
    protected void fillTiles(ArrayList<int[]> buffer) {
        //Fills in the tile
        buffer.forEach(pos -> {
            this.gameScene.getBoard().changeTile(this.currentPiece.getColour(), pos[1], pos[0]);
        });
    }

    /**
     * Called after a piece is played
     */
    protected void afterPiece() {
        var board = this.gameScene.getBoard();

        //Buffers for which rows/columns are full
        //rows and columns share tiles so we can't remove any tiles before checking both
        var rowBuffer = new ArrayList<Integer>();
        var columnBuffer = new ArrayList<Integer>();

        for (int i = 0; i < this.gameScene.getGridHeight(); i++) {
            if (board.checkRow(i)) rowBuffer.add(i);
            if (board.checkColumn(i)) columnBuffer.add(i);
        }

        //Clears the rows and columns that are full
        rowBuffer.forEach(row -> {
            board.clearRow(row);
        });

        columnBuffer.forEach(column -> {
            board.clearColumn(column);
        });

        //Values needed for the score
        int linesCleared = rowBuffer.size()+columnBuffer.size();
        int blocks = columnBuffer.size()*board.getGridHeight() + rowBuffer.size() * (board.getGridWidth() - columnBuffer.size());

        //Update the score
        this.score(linesCleared, blocks); 

        //Updates the multiplier
        if (columnBuffer.size() == 0 && rowBuffer.size() == 0) this.multiplier.set(1);
        else this.multiplier.set(this.multiplier.get() + 1);

        logger.info("Multiplier set to {}", this.multiplier.get());

    }

    /**
     * Updates the score 
     * @param lines How many lines have been cleared
     * @param blocks How many blocks have been cleared
     */
    protected void score(int lines, int blocks) {
        this.score.set(this.score.get() + 10 * lines * blocks * this.multiplier.get());
    }

    /**
     * Swaps the next piece to be played with the reserve
     */
    protected void swapNextPiece() {
        logger.info("Swapping pieces");
        var temp = this.currentPiece;
        this.currentPiece = this.reservePiece;
        this.reservePiece = temp;

        this.gameScene.setNextPiece(this.currentPiece);
        this.gameScene.setReservePiece(this.reservePiece);
    }

    /**
     * Called after a successful move
     * Moves the reserve piece to be played next
     * Randomly gets the next piece
     */
    protected void nextPiece() {
        this.currentPiece = this.reservePiece;
        this.gameScene.setNextPiece(this.currentPiece);

        this.reservePiece = GamePiece.createPiece();
        this.gameScene.setReservePiece(this.reservePiece);
        logger.info("Piece {} added", this.reservePiece);
    }

    /**
     * Moves selected tile
     * @param byX
     * @param byY
     */
    protected void moveSelected(int byX, int byY) {
        this.gameScene.getBoard().moveSelected(byX, byY);
    }
    
    protected void insertSelected() {
        int[] selected = this.gameScene.getBoard().getSelectedPos();
        this.insertPiece(selected[0], selected[1]);
    }

    //Timer//

    /**
     * Returns the delay time in ms
     * @return time to make a move
     */
    protected int getTimerDelay() {
        if (this.level.get() < 19) return (12000 - 500*this.level.get());
        else return 2500;
    }

    //Network//

    /**
     * Sets up the communicator for the game
     */
    protected void setupCommunicator() {
        NetworkProtocol.ERROR.addListener(message -> logger.error(message));
    }
}
