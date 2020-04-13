package ooga.view;

import javafx.beans.binding.NumberBinding;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.*;
import javafx.scene.shape.Rectangle;
import javafx.util.Pair;
import ooga.cardtable.*;
import javafx.geometry.Point2D;
import java.util.HashMap;
import java.util.Map;

import ooga.cardtable.Offset;

public class DisplayCell {

    private Map<Offset, DisplayCell> myDisplayChildren = new HashMap<>();
    private Cell myCell;
    private Group myGroup = new Group();

    private ImageView myImageView;
    private Image myFaceUp;
    private Image myFaceDown;

    private Map<Offset, Point2D> offsetDirToAmount;

    private Point2D lastXY = null;

    private DisplayTable.MyDragInterface myDragLambda;
    private DisplayTable.MyClickInterface myClickLambda;

    public DisplayCell(DisplayTable.MyDragInterface dragLambda, DisplayTable.MyClickInterface clickLambda, Cell cell, Map<String, String> cardNameToFileName, Pair<NumberBinding, NumberBinding>location, double height, double width, double offset) {
        myDragLambda = dragLambda;
        myClickLambda = clickLambda;

        myCell = cell;
        myFaceDown = new Image(cardNameToFileName.get("faceDown"));
        if(myCell.getDeck().peek() != null) {
            String cardName = myCell.getDeck().peek().getName(); //TODO: ADD TRY CATCH FOR GETTING IMAGE
            myFaceUp = new Image(cardName + ".png");//cardNameToFileName.get(myCell.getDeck().peek().getName()));
            if (myCell.getDeck().peek().isFaceUp()) {
                myImageView = new ImageView(myFaceUp);
            } else {
                myImageView = new ImageView(myFaceDown);
            }
        } else {
            myFaceUp = new Image("celloutline.png");
            myImageView = new ImageView(myFaceUp);
        }

        myImageView.translateXProperty().bind(location.getKey());
        myImageView.translateXProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue,
                Number newValue) {
                System.out.println(newValue);
            }
        });
        myImageView.translateYProperty().bind(location.getValue());
        myImageView.setFitWidth(width);
        myImageView.setFitHeight(height);

        enableDrag(myImageView);
        myGroup.getChildren().add(myImageView);

        offsetDirToAmount = Map.of(Offset.NONE, new Point2D(0,0), Offset.NORTH, new Point2D(0, -offset), Offset.SOUTH, new Point2D(0,offset), Offset.EAST, new Point2D(offset, 0),Offset.WEST, new Point2D(-offset,0), Offset.NORTHEAST, new Point2D(offset,-offset), Offset.SOUTHEAST, new Point2D(offset,offset), Offset.NORTHWEST, new Point2D(-offset,-offset), Offset.SOUTHWEST, new Point2D(-offset,offset));

        /*Cell childCellNone = (Cell) myCell.getAllChildren().get(Offset.NONE);
        if (childCellNone.getDeck().peek()==null) {
            System.out.println("Card in deck returned null");
            DisplayCell childDisplayCellNone = new DisplayCell(childCellNone, cardNameToFileName.get(childCellNone.getDeck().peek().getName()), cardNameToFileName.get("faceDown"), location.add(offsetDirToAmount.get(Offset.NONE)), height, width, offset);
            myDisplayChildren.put(Offset.NONE, childDisplayCellNone);
            myGroup.getChildren().add(childDisplayCellNone.getImageView());
        }
         */

        for (IOffset dir: myCell.getAllChildren().keySet()) {
            Cell childCell = (Cell) myCell.getAllChildren().get(dir);
            if (dir == Offset.NONE) { // && childCell.getDeck().peek() == null
                continue;
            }
            Point2D offsetAmount = offsetDirToAmount.get(dir);
            Pair<NumberBinding, NumberBinding> childOffset = new Pair<>(location.getKey().add(offsetAmount.getX()),location.getValue().add(offsetAmount.getY()));
            DisplayCell childDisplayCell = new DisplayCell(myDragLambda, myClickLambda, childCell, cardNameToFileName, childOffset, height, width, offset);
            myDisplayChildren.put((Offset) dir, childDisplayCell);
            myGroup.getChildren().add(childDisplayCell.getImageView());
        }
    }

    /*public DisplayCell(Cell cell, String faceUp, String faceDown, Point2D location, double height, double width, double offset) {
        myCell = cell;
        myFaceDown = new Image(faceDown);
        myFaceUp = new Image(faceUp);

        if (myCell.getDeck().peek().isFaceUp()) {
            myImageView = new ImageView(myFaceUp);
        } else {
            myImageView = new ImageView(myFaceDown);
        }
        myImageView.setX(location.getX());
        myImageView.setY(location.getY());
        myImageView.setFitWidth(width);
        myImageView.setFitHeight(height);

        myGroup = new Group();
        myGroup.getChildren().add(myImageView);

        enableDrag(myImageView);
    }
     */

    public Map<Offset,DisplayCell> getAllChildren() {
        return myDisplayChildren;
    }

    public Group getGroup() {
        return myGroup;
    }

    public ImageView getImageView() {
        return myImageView;
    }

    public Cell getCell() {
        return myCell;
    }

    public Point2D getLastXY() {
        return lastXY;
    }

    public void setLastXY(Point2D newXY) {
        lastXY = newXY;
    }

    private void enableDrag(ImageView source) {
        source.setOnMouseDragged(event -> {
            event.setDragDetect(false);
            Node on = (Node)event.getTarget();
            moveAll(this, new Point2D(event.getSceneX(), event.getSceneY()));
            event.consume();
        });

        source.setOnMouseReleased(d -> {
            resetAll(this);
            myDragLambda.returnSelectedDisplayCell(this);
        });

        source.setOnMouseClicked( click -> {
            myClickLambda.returnSelectedDisplayCell(this);
        });
    }

    private void resetAll(DisplayCell selectedCell) {
        selectedCell.setLastXY(null);
        for (Offset dir: selectedCell.getAllChildren().keySet()) {
            if (dir == Offset.NONE) {
                continue;
            }
            resetAll(selectedCell.getAllChildren().get(dir));
        }
    }

    private void moveAll(DisplayCell selectedCell, Point2D initDragToXY) {
        moveChildTo(selectedCell,initDragToXY);
        for (Offset dir: selectedCell.getAllChildren().keySet()) {
            if (dir == Offset.NONE) {
                continue;
            }
            moveAll(selectedCell.getAllChildren().get(dir), initDragToXY.add(offsetDirToAmount.get(dir)));
        }
    }

    private void moveChildTo(DisplayCell childCell, Point2D dragToXY) {
        Point2D dragFromXY = childCell.getLastXY();
        if (dragFromXY == null) {
            dragFromXY = dragToXY;
            childCell.setLastXY(dragFromXY);
        }
        Point2D dxdy = dragToXY.subtract(dragFromXY);
        Node on = (Node) childCell.getImageView();
        on.setTranslateX(on.getTranslateX()+dxdy.getX());
        on.setTranslateY(on.getTranslateY()+dxdy.getY());
        on.toFront();
        childCell.setLastXY(dragToXY);
    }

}