package ooga.View;

public interface FrontendExternalAPI {

    /**
     * setCellData() is called regularly by the Controller to pass the correct state of the board
     * to the front end from the back end. This is done by sending a list of cell objects which
     * represent groups of cards and their associated state (i.e. face up/down, staggered/even, card type)
     */
    void setCellData(List<Cell>);

    /**
     * Returns a boolean indicating whether a user has made a change since the Controller's last
     * call to getUserInput().
     */
    boolean isUserInput();

    /**
     * getUserInput() is called regularly by the Controller to obtain the new move made by
     * any player.
     *
     * Sets isUserInput() to false, pending a new move from any player.
     @return a map from the clicked on object to the released on object of the user's action
     * TODO: Decide what object this should be with backend
     */
    Map<Object, Object> getUserInput();


    // TODO: ask backend how setStyle, setDeck, setLayout info will be passed, filename?
    // TODO: or controller can make a style object, deck object, layout object etc to pass to fe

    /**
     * Sets the style of the game, including color of table, location of menu/its display elements,
     * font type, font size, text colors, margins, etc.
     */
    void setStyle(String filename);

    /**
     * Sets the deck to use, including a folder containing images for card faces.
     */
    void setDeck(String filename);

    /**
     * Sets the locations of all cell types and the framework for creating new cell locations if applicable.
     */
    void setLayout(String filename);

}
