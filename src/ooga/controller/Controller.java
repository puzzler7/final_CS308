package ooga.controller;

import javafx.application.Application;
import javafx.stage.Stage;
import ooga.cardtable.*;
import ooga.data.XMLException;
import ooga.data.factories.HighScoreFactory;
import ooga.data.factories.PhaseMachineFactory;
import ooga.data.factories.LayoutFactory;
import ooga.data.factories.StyleFactory;
import ooga.data.highscore.IHighScores;
import ooga.data.rules.IPhaseMachine;

import ooga.data.style.IStyle;
import ooga.view.View;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Andrew Krier, Tyler Jang
 */
public class Controller extends Application {

    // TODO: Put the file here


    private static final String DEFAULT_STYLE_FILE = "data/default_style.xml";
    private static final String BACKUP_STYLE_FILE = "data/default_style_orig.xml";

    private static final String DEFAULT_SCORE_FILE = "data/default_score.xml";
    private static final String BACKUP_SCORE_FILE = "data/default_score_orig.xml";


    private static final String DEFAULT_RULE_FILE = "data/solitaire_rules.xml";           //default
    //private static final String DEFAULT_RULE_FILE = "data/solitaire_rules_static_1.xml";  //fixed cards, demo
    //private static final String DEFAULT_RULE_FILE = "data/memory_rules.xml";       //memory debugging
    //private static final String DEFAULT_RULE_FILE = "data/solitaire_rules_static_2.xml";  //almost win state
    //private static final String DEFAULT_RULE_FILE = "data/solitaire_rules_static_3.xml";  //xsd error
    //private static final String DEFAULT_RULE_FILE = "data/solitaire_rules_static_4.xml";  //runtime phase error

    private static final String WIN = "win";
    private static final String LOSS = "loss";
    private static final String INVALID = "invalid";

    private View myView;
    private String currentGame;
    private IHighScores myScores;
    private File myScoresFile;
    private IGameState lastState;
    private IPhaseMachine myCurrentPhaseMachine;
    private Map<String, ICell> myCurrentCells;
    private Map<String, ICell> myPreviousCells;
    private Map<String, ICell> myChangedCells = new HashMap<>();
    private Map<Integer, ITable> myTables;

    @FunctionalInterface
    public
    interface GiveMove {
        void sendMove(IMove move, int gameID);
    }

    public Controller() {
        super();
        myTables = new HashMap<>();
    }

    /**
     * Called at the beginning of the application
     * Initializes view and gives it handlers for notifying controller
     * and a style data class for boot up information
     * @param mainStage
     */
    @Override
    public void start(Stage mainStage) {
        GiveMove gm = (move, gameID) -> {
            try {
                lastState = myTables.get(gameID).update(move);
                if (lastState.equals(GameState.WIN)) {
                    myView.displayMessage(gameID,WIN);
                } else if (lastState.equals(GameState.INVALID)) {
                    myView.displayMessage(gameID,INVALID);
                } else if (lastState.equals(GameState.LOSS)) {
                    myView.displayMessage(gameID,LOSS);
                }
                Double score = myTables.get(gameID).getCurrentPlayer().getScore();
                myView.setScores(gameID,Map.of(1, score));
                updateHighScores(currentGame, score);
                myCurrentCells = myTables.get(gameID).getCellData();
                for (String i : myCurrentCells.keySet()) {
                    if (!myPreviousCells.containsKey(i) || !myPreviousCells.get(i).equals(myCurrentCells.get(i))) {
                        myChangedCells.put(i, myCurrentCells.get(i));
                        myPreviousCells.remove(i);
                    }
                }
                processInvalidMove(move);
                myView.setUpdatesToCellData(gameID,myChangedCells);
                myPreviousCells = myCurrentCells;
                myChangedCells.clear();
            } catch (XMLException e) {
                reportError(e);
            }
            //myView.setCellData(Map.copyOf(myTable.getCellData()));
        };

        IStyle myStyle = extractStyle();
        myScores = extractScores();
        myView = new View(gm, (int gameID)->{
            myTables.get(gameID).restartGame();
            myCurrentCells = myTables.get(gameID).getCellData();
            myView.setUpdatesToCellData(gameID,myCurrentCells); },
            myStyle);
        initializeHandlers(myView);
    }

    private IStyle extractStyle() {
        File myStyleFile;
        try {
            myStyleFile = new File(DEFAULT_STYLE_FILE);
            return StyleFactory.createStyle(myStyleFile);
        } catch (Exception e) {
            reportError(e);
            myStyleFile = new File(BACKUP_STYLE_FILE);
            return StyleFactory.createStyle(myStyleFile, DEFAULT_STYLE_FILE);
        }
    }

    private IHighScores extractScores() {
        try {
            myScoresFile = new File(DEFAULT_SCORE_FILE);
            return HighScoreFactory.createScores(myScoresFile);
        } catch (Exception e) {
            reportError(e);
            myScoresFile = new File(BACKUP_SCORE_FILE);
            return HighScoreFactory.createScores(myScoresFile, DEFAULT_SCORE_FILE);
        }
    }

    private void updateHighScores(String currentGame, Double score) {
        if (myScores.getScore(currentGame) < score) {
            //myScores.setScore(currentGame, score); FIXME: creates error
            //myView.setHighScore(myScores.getScore(currentGame)); //TODO: MARIUSZ display it please
        }
    }

    private void reportError(Exception e) {
        String[] messages = e.getMessage().split(",");
        List<String> tags = new ArrayList<>();
        for (int k = 1; k < messages.length; k ++) {
            tags.add(messages[k]);
        }
        if (myView!= null) {
            myView.reportError(messages[0], tags);
        }
    }

    private void processInvalidMove(IMove move) {
        if (myChangedCells.isEmpty()) {
            List<ICell> resetters = List.of(move.getRecipient(), move.getMover(), move.getDonor());
            for (ICell c: resetters) {
                c=myCurrentCells.get(c.findHead().getName());
                if (c != null) {
                    myChangedCells.put(c.getName(), c);
                }
            }
        }
    }

    //TODO: REPLACE WITH LOGIC REGARDING METHODS AT THE BOTTOM
    private void initializeHandlers(View v) {
        //input is string gamename
        v.listenForGameChoice((a,b,gameName) -> startTable(gameName));
        //() -> newMove());
        /*v.setHandlers((String game) -> createEngine(game), //Consumer
                (String rules) -> setHouseRules(rules), //Consumer
                (int diff) -> setDifficulty(diff), //Consumer
                (IMove move) -> processMove(move), //Function
                (String cell) -> getCell(cell)); //Function/Supplier
                */
        /*View.setHandlers(Consumer engineStart, Consumer ruleSet,  ....);
            myFunction = move;


        MOUSE.setOnClickAndDrag(event -> move.execute(new Move(event.getX, event.getY)));
            */
    }

    private void startTable(String gameName) {
        // TODO: process gamename string to a file path
        //System.out.println(gameName);
        // TODO: Give game name somehow, figure out who's building the phase machine
        currentGame = gameName;
        String ruleFile = "data/" + gameName + "_rules.xml";
        //ruleFile = "data/solitaire_rules_static_2.xml";  //almost win state

        File myRuleFile = new File(ruleFile);
        try {
            int gameID = myView.createGame(gameName);
            myCurrentPhaseMachine = PhaseMachineFactory.createPhaseMachine(myRuleFile);
            ITable table = new Table(myCurrentPhaseMachine);
            Map<String, ICell> myCellMap = table.getCellData();
            File f = new File(myCurrentPhaseMachine.getSettings().getLayout());

            //fixme
            myView.setLayout(gameID,LayoutFactory.createLayout(f));
            myView.setCellData(gameID, myCellMap);
            //myView.setHighScore(myScores.getScore(currentGame));  //TODO: MARIUSZ display it please
            myPreviousCells = myCellMap;
            myTables.put(gameID,table);
            //myView.setCellData(Map.copyOf(myTable.getCellData()));
        } catch (XMLException e) {
            reportError(e);
        }
    }

    private void newMove() {
        IMove myCurrentMove = getMove();
        try {
            //FIXME
            //myTable.update(myCurrentMove);
        } catch (XMLException e) {
            reportError(e);
        }
        //myView.setCellData(Map.copyOf(myTable.getCellData()));
    }

    private IMove getMove() {
        return myView.getUserInput();
    }

    private void createEngine(String gameName) {

    }

    private void setHouseRules(String ruleName) {

    }

    private void setDifficulty(int difficulty) {

    }

    private boolean isMove() {
        return myView.isUserInput();
    }

    private IGameState processMove(IMove move) {

        return null;
    }

    private ICell getCell(String cellName) {
        return null;
    }

    /**void setCellData(Map<String, ICell> cellData);
     void setScores(Map<Integer, Double> playerScores);
     void endGame(Map<Integer, Boolean> playerOutcomes, Map<Integer, Double> playerScores, Map<Integer, Integer> highScores);
     void playerStatusUpdate(Map<Integer, Boolean> playerOutcomes, Map<Integer, Integer> playerScores);
     boolean isUserInput();
     IMove getUserInput();
     void setStyle(Style style);
     void setLayout(Layout layout);
     **/

}

