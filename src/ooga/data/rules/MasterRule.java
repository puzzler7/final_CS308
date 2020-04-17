package ooga.data.rules;

import ooga.cardtable.GameState;
import ooga.cardtable.IGameState;
import ooga.cardtable.IMove;
import ooga.cardtable.IPlayer;

import java.util.List;

public class MasterRule implements IMasterRule {

    private List<IRule> myRules;
    private List<IRule> myAutoRules;
    private List<ICardAction> myCardActions;
    private List<IControlAction> myControlActions;
    private String MASTER_RULE = "This is a Master Rule";

    public MasterRule(List<IRule> rules, List<IRule> autoRules, List<ICardAction> cardActions, List<IControlAction> controlActions) {
        myRules = rules;
        myAutoRules = autoRules;
        myCardActions = cardActions;
        myControlActions = controlActions;
    }

    @Override
    public IGameState executeMove(IMove move) {
        if (checkValidMove(move)) {
            System.out.println("time to act");
            for (ICardAction action: myCardActions) {
                System.out.println("\t let's do it");
                action.execute(move);
            }
        }
        return GameState.WAITING;
    }

    @Override
    public IPhaseArrow executeAutoActions(IPlayer player, IMove move) {
        //TODO: USE AUTORULES FOR THIS CONDITIONAL
        if (checkAutoRules(move)) {
            IPhaseArrow lastArrow = null;
            for (IControlAction action : myControlActions) {
                lastArrow = action.execute(player);
            }
            return lastArrow;
        }
        return null;
    }
    
    @Override
    public boolean checkAutoRules(IMove move) {
        boolean flag = true;
        for (IRule rule: myAutoRules) {
            System.out.println("yabbadabbadoo");
            if (!rule.checkValidMove(move)) { //TODO: VERIFY THIS NULL WORKS
                System.out.println("yabbadabbadont1");
                return false;
            }
            System.out.println("yabbadabbadont");
        }
        return flag;
    }

    @Override
    public boolean checkValidMove(IMove move) {
        //TODO: MAVERICK IS AN IDIOT AND A GENIUS
        return true;/*
        boolean flag = true;
        for (IRule rule: myRules) {
            if (!rule.checkValidMove(move)) {
                System.out.println(rule.getName());
                System.out.println("I hereby declare this move:" + false);
                return false;
            }
        }
        System.out.println("I hereby declare this move:" + true);
        return flag;*/
    }

    @Override
    public String getName() {
        return MASTER_RULE;
    }
}
