package ooga.controller;

import java.util.function.Consumer;
import javafx.application.Application;
import javafx.stage.Stage;
import ooga.cardtable.*;
import ooga.data.XMLException;
import ooga.data.factories.*;
import ooga.data.highscore.IHighScores;
import ooga.data.rules.IPhaseMachine;

import ooga.data.saveconfiguration.ISaveConfiguration;
import ooga.data.style.IStyle;
import ooga.view.View;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import ooga.view.View.SaveGame;

/**
 *
 * @author Andrew Krier, Tyler Jang, Sarah Gregorich, Mariusz Derezinski-Choo
 */
public class Controller extends Application {

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

    private static final String FILEPATH = "data/";
    private static final String RULES_EXTENSION = "_rules.xml";

    private static final String WIN = "win";
    private static final String LOSS = "loss";
    private static final String INVALID = "invalid";

    private View myView;
    private IHighScores myScores;
    private File myScoresFile;
    private File myRuleFile;
    private IGameState lastState;
    private IPhaseMachine myCurrentPhaseMachine;
    private Map<String, ICell> myCurrentCells;
    private Map<String, ICell> myPreviousCells;
    private Map<String, ICell> myChangedCells = new HashMap<>();
    private Map<Integer, ITable> myTables;
    private Map<Integer, File> myRuleFiles;
    private Map<Integer, String> myGameNames;

    @FunctionalInterface
    public
    interface GiveMove {
        void sendMove(IMove move, int gameID);
    }

    public Controller() {
        super();
        myTables = new HashMap<>();
        myRuleFiles = new HashMap<>();
        myGameNames = new HashMap<>();
    }

    /**
     * Called at the beginning of the application
     * Initializes view and gives it handlers for notifying controller
     * and a style data class for boot up information
     * @param mainStage
     */
    @Override
    public void start(Stage mainStage) {
        GiveMove gm = (move, gameID) -> { processMove(move, gameID); };

        IStyle myStyle = extractStyle();
        myScores = extractScores();

        SaveGame processGameSave = (gameID, fileName) -> { saveGame(gameID, fileName); };
        Consumer<String> loadGame = (fileName) -> { loadGame(fileName); };

        myView = new View(gm, (int gameID)->{ restartAndUpdateGame(gameID); }, myStyle, processGameSave,loadGame);

        for(String key : myScores.getSavedGames()){
            myView.updateHighScores(key,myScores.getScore(key));
        }
        initializeHandlers(myView);
    }

    private void restartAndUpdateGame(int gameID) {
        myTables.get(gameID).restartGame();
        myCurrentCells = myTables.get(gameID).getCellData();
        myView.setUpdatesToCellData(gameID,myCurrentCells);
        myView.setScores(gameID, Map.of(1, myTables.get(gameID).getCurrentPlayer().getScore()));
    }

    private void processMove(IMove move, int gameID) {
        try {
            Double score = getAndUpdateScoreForGame(move, gameID);
            myView.setScores(gameID,Map.of(1, score));
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
    }

    private Double getAndUpdateScoreForGame(IMove move, int gameID) {
        Double score = myTables.get(gameID).getCurrentPlayer().getScore();
        lastState = myTables.get(gameID).update(move);
        if (lastState.equals(GameState.WIN)) {
            myView.displayMessage(gameID,WIN);
            updateHighScores(myGameNames.get(gameID), score);
        } else if (lastState.equals(GameState.INVALID)) {
            myView.displayMessage(gameID,INVALID);
        } else if (lastState.equals(GameState.LOSS)) {
            myView.displayMessage(gameID,LOSS);
            updateHighScores(myGameNames.get(gameID), score);
        }
        return score;
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
        myScores.setScore(currentGame, score);
        myView.updateHighScores(currentGame,myScores.getScore(currentGame));
    }

    private void reportError(Exception e) {
        e.printStackTrace();

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

    private void saveGame(int gameID, String destination) {
        ISaveConfiguration saveData = myTables.get(gameID).getSaveData(myGameNames.get(gameID), myRuleFiles.get(gameID).getPath());
        saveData.writeConfiguration(destination);
    }

    private void loadGame(String loadFile) {
        try {
            System.out.println(loadFile);
            ISaveConfiguration load = SaveConfigurationFactory.createSave(new File(loadFile));
            IPhaseMachine pm = PhaseMachineFactory.createPhaseMachine(new File(load.getRulePath()));
            pm.setCellData(load.getCellMap());
            pm.setPhase(load.getCurrentPhase());
            ITable table = new Table(pm);
            table.getCurrentPlayer().setScore(load.getScore());
            myCurrentPhaseMachine = pm;
            int gameID = myView.createGame(load.getGameName());
            myGameNames.put(gameID, load.getGameName());
            myRuleFiles.put(gameID, new File(loadFile));
            attatchView(gameID, table);
        } catch (XMLException e) {
            reportError(e);
        }
    }

    private void initializeHandlers(View v) {
        v.listenForGameChoice((a,b,gameName) -> startTable(gameName));
    }

    private void startTable(String gameName) {
        String ruleFile = FILEPATH + gameName + RULES_EXTENSION;
        try {
            myRuleFile = new File(ruleFile);
        } catch (Exception e) {
            reportError(e);
            myRuleFile = new File(DEFAULT_RULE_FILE);
        }

        try {
            int gameID = myView.createGame(gameName);
            myGameNames.put(gameID,gameName);
            myRuleFiles.put(gameID,myRuleFile);
            myCurrentPhaseMachine = PhaseMachineFactory.createPhaseMachine(myRuleFile);
            ITable table = new Table(myCurrentPhaseMachine);
            attatchView(gameID, table);
        } catch (XMLException e) {
            reportError(e);
        }
    }

    private void attatchView(int gameID, ITable table) {
        Map<String, ICell> myCellMap = table.getCellData();
        File f = new File(myCurrentPhaseMachine.getSettings().getLayout());

        myView.setLayout(gameID, LayoutFactory.createLayout(f));
        myView.setCellData(gameID, myCellMap);
        myPreviousCells = myCellMap;
        myTables.put(gameID,table);
    }

}

