package ooga.data;

import ooga.cardtable.*;
import ooga.data.rules.ISettings;
import ooga.data.rules.Settings;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

//TODO: ADD DOCUMENTATION
public class DeckFactory {
    public static final String RESOURCE_PACKAGE = PhaseMachineFactory.RESOURCE_PACKAGE;
    private static final String DECK = "deck";
    private static final ResourceBundle resources = ResourceBundle.getBundle(RESOURCE_PACKAGE+DECK);

    public static String INVALID_ERROR = "INVALID_FILE";
    public static String MISSING_ERROR = "MISSING_ATTRIBUTE";

    private static final String DECK_PATH = "DeckPath";
    private static final String DECK_NAME = "DeckName";
    private static final String CARD = "Card";
    private static final String SHUFFLE = "Shuffle";
    private static final String YES = "yes";
    private static final String DEFAULT_SHUFFLE = YES;
    private static final String NAME = "Name";
    private static final String VALUE = "Value";
    private static final String COLOR = "Color";
    private static final String SUIT = "Suit";

    private static DocumentBuilder documentBuilder;

    public DeckFactory() { documentBuilder = XMLHelper.getDocumentBuilder();}

    public static IDeck getDeck(Element root) {
        try {
            Node deck = root.getElementsByTagName(DECK).item(0);
            if (deck.hasChildNodes()) {
                NodeList nodeList = deck.getChildNodes();
                String pathToDeck = resources.getString(DECK_PATH);
                String deckPath = XMLHelper.getTextValue((Element)deck, pathToDeck);
                if (deckPath != "") {
                    return findStoredDeck(deckPath);
                } else {
                    //return buildDeck(nodeList);
                    return buildDeck(deck);
                }

            } else {
                throw new XMLException(MISSING_ERROR + "," + DECK);
            }
        } catch (Exception e) {
            throw new XMLException(e, MISSING_ERROR + "," + DECK);
        }
    }

    private static IDeck findStoredDeck(String filePath) {
        try {
            File f = new File(filePath);
            Element root = XMLHelper.getRootAndCheck(f, DECK, INVALID_ERROR);
            Node deck = root.getElementsByTagName(DECK).item(0);
            //return buildDeck(deck.getChildNodes());
            return buildDeck(deck);
        } catch (Exception e) {
            throw new XMLException(e, MISSING_ERROR + "," + DECK);
        }
    }

    //private static IDeck buildDeck(NodeList nodeList) {
    private static IDeck buildDeck(Node node) {
        Element deck = (Element)node;
        String shuffle = XMLHelper.getTextValue(deck, resources.getString(SHUFFLE));
        String deckName = XMLHelper.getTextValue(deck, resources.getString(DECK_NAME));
        NodeList nodeList = deck.getElementsByTagName(resources.getString(CARD));

        List<ICard> cardList = new ArrayList<>();
        for (int k = 0; k < nodeList.getLength(); k ++) {
            Node n = nodeList.item(k);
            cardList.add(buildCard(n));
        }
        IDeck myDeck = new Deck(deckName, cardList);
        if (shuffle.equals(YES)) {
            myDeck.shuffle();
        }
        return myDeck;
    }

    private static ICard buildCard(Node node) {
        String cardName = getVal(node, NAME);
        Integer val = Integer.parseInt(getVal(node, VALUE));
        IColor color = new Color(getVal(node, COLOR));
        ISuit suit = new Suit(getVal(node, SUIT), color);
        IValue value = new Value(val + suit.getName(), val);
        return new Card(suit, value);
    }

    private static String getVal(Node n, String tagRef) {
        return XMLHelper.getTextValue((Element)n, resources.getString(tagRef), () -> {throw new XMLException(MISSING_ERROR + "," + tagRef);});
    }
}