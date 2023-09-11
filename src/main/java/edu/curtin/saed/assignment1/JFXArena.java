package edu.curtin.saed.assignment1;

import javafx.scene.canvas.*;
import javafx.geometry.VPos;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;

import java.io.*;
import java.util.*;

import edu.curtin.saed.assignment1.entities.robot.*;
import edu.curtin.saed.assignment1.game_engine.GameEngine;
import edu.curtin.saed.assignment1.misc.Vector2d;
import edu.curtin.saed.assignment1.entities.fortress_wall.*;


/**
 * A JavaFX GUI element that displays a grid on which you can draw images, text and lines.
 */
public class JFXArena extends Pane
{
    private GameEngine gameEngine;

    // Represents an image to draw
    private Image robotImage;
    private Image undamagedFortressWallImage;
    private Image damagedFortressWallImage;
    private Image citadelImage;
    
    private static final String CITADEL_IMAGE_FILE = "rg1024-isometric-tower.png";

    // The following values are arbitrary, and you may need to modify them according to the 
    // requirements of your application.
    private int gridCols;
    private int gridRows;

    private double gridSquareSize; // Auto-calculated
    private Canvas canvas; // Used to provide a 'drawing surface'.

    private List<ArenaListener> listeners = null;
    
    /**
     * Creates a new arena object, loading the robot image and initialising a drawing surface.
     */
    public JFXArena(GameEngine gameEngine, int numRows, int numCols)
    {
        this.gridRows = numRows;
        this.gridCols = numCols;

        // Load Images
        this.robotImage = loadImage(Robot.IMAGE_FILE);
        this.undamagedFortressWallImage = loadImage(FortressWall.UNDAMAGED_IMAGE_FILE);
        this.damagedFortressWallImage = loadImage(FortressWall.DAMAGED_IMAGE_FILE);
        this.citadelImage = loadImage(CITADEL_IMAGE_FILE);

        // Set GameEngine
        this.gameEngine = gameEngine;
        
        // Draw UI        
        canvas = new Canvas();
        canvas.widthProperty().bind(widthProperty());
        canvas.heightProperty().bind(heightProperty());
        getChildren().add(canvas);
    }
   


    // Return an Image from a resource filename
    private Image loadImage(String path)
    {
        try(InputStream is = getClass().getClassLoader().getResourceAsStream( path ))
        {
            if(is == null)
            {
                throw new AssertionError("Cannot find image file " + path);
            }
            return new Image(is);
        }
        catch(IOException e)
        {
            throw new AssertionError("Cannot load image file " + path, e);
        }
    }

    
    
    /**
     * Adds a callback for when the user clicks on a grid square within the arena. The callback 
     * (of type ArenaListener) receives the grid (x,y) coordinates as parameters to the 
     * 'squareClicked()' method.
     */
    public void addListener(ArenaListener newListener)
    {
        if(listeners == null)
        {
            listeners = new LinkedList<>();
            setOnMouseClicked(event ->
            {
                int gridX = (int)(event.getX() / gridSquareSize);
                int gridY = (int)(event.getY() / gridSquareSize);
                
                if(gridX < gridCols && gridY < gridRows)
                {
                    for(ArenaListener listener : listeners)
                    {   
                        listener.squareClicked(gridX, gridY);
                    }
                }
            });
        }
        listeners.add(newListener);
    }
        
        
    /**
     * This method is called in order to redraw the screen, either because the user is manipulating 
     * the window, OR because you've called 'requestLayout()'.
     *
     * You will need to modify the last part of this method; specifically the sequence of calls to
     * the other 'draw...()' methods. You shouldn't need to modify anything else about it.
     */
    @Override
    public void layoutChildren()
    {
        super.layoutChildren(); 
        GraphicsContext gfx = canvas.getGraphicsContext2D();
        gfx.clearRect(0.0, 0.0, canvas.getWidth(), canvas.getHeight());
        
        // First, calculate how big each grid cell should be, in pixels. (We do need to do this
        // every time we repaint the arena, because the size can change.)
        gridSquareSize = Math.min(
            getWidth() / (double) gridCols,
            getHeight() / (double) gridRows);
            
        double arenaPixelWidth = gridCols * gridSquareSize;
        double arenaPixelHeight = gridRows * gridSquareSize;
            
            
        // Draw the arena grid lines. This may help for debugging purposes, and just generally
        // to see what's going on.
        gfx.setStroke(Color.DARKGREY);
        gfx.strokeRect(0.0, 0.0, arenaPixelWidth - 1.0, arenaPixelHeight - 1.0); // Outer edge

        for(int gridX = 1; gridX < gridCols; gridX++) // Internal vertical grid lines
        {
            double x = (double) gridX * gridSquareSize;
            gfx.strokeLine(x, 0.0, x, arenaPixelHeight);
        }
        
        for(int gridY = 1; gridY < gridRows; gridY++) // Internal horizontal grid lines
        {
            double y = (double) gridY * gridSquareSize;
            gfx.strokeLine(0.0, y, arenaPixelWidth, y);
        }

        // Invoke helper methods to draw things at the current location.
        // ** You will need to adapt this to the requirements of your application. **
        List<ReadOnlyRobot> robots = gameEngine.getRobots();
        List<ReadOnlyFortressWall> walls = gameEngine.getPlacedWalls();
        Vector2d citadelPos = gameEngine.getCitadel();

        // Draw Citadel
        drawImage( gfx, citadelImage, citadelPos.x(), citadelPos.y() );

        //Draw all walls
        for(ReadOnlyFortressWall w : walls)
        {
            if(w.isDamaged())
            {
                drawImage( gfx, damagedFortressWallImage, w.getCoordinates().x(), w.getCoordinates().y() );
            }
            else
            {
                drawImage( gfx, undamagedFortressWallImage, w.getCoordinates().x(), w.getCoordinates().y() );
            }
        }

        //Draw all robots
        for(ReadOnlyRobot r : robots)
        {
            //Draw the robot icon
            drawImage( gfx, robotImage, r.getCoordinates().x(), r.getCoordinates().y() );

            //Draw the label
            String label = String.valueOf(r.getId());
            drawLabel( gfx, label, r.getCoordinates().x(), r.getCoordinates().y() );
        }

        

    }
    
    
    /** 
     * Draw an image in a specific grid location. *Only* call this from within layoutChildren(). 
     *
     * Note that the grid location can be fractional, so that (for instance), you can draw an image 
     * at location (3.5,4), and it will appear on the boundary between grid cells (3,4) and (4,4).
     *     
     * You shouldn't need to modify this method.
     */
    private void drawImage(GraphicsContext gfx, Image image, double gridX, double gridY)
    {
        // Get the pixel coordinates representing the centre of where the image is to be drawn. 
        double x = (gridX + 0.5) * gridSquareSize;
        double y = (gridY + 0.5) * gridSquareSize;
        
        // We also need to know how "big" to make the image. The image file has a natural width 
        // and height, but that's not necessarily the size we want to draw it on the screen. We 
        // do, however, want to preserve its aspect ratio.
        double fullSizePixelWidth = image.getWidth(); //TODO This was "robotImage", however I think it should be image. May need to fix later
        double fullSizePixelHeight = image.getHeight(); //TODO same as above
        
        double displayedPixelWidth, displayedPixelHeight;
        if(fullSizePixelWidth > fullSizePixelHeight)
        {
            // Here, the image is wider than it is high, so we'll display it such that it's as 
            // wide as a full grid cell, and the height will be set to preserve the aspect 
            // ratio.
            displayedPixelWidth = gridSquareSize;
            displayedPixelHeight = gridSquareSize * fullSizePixelHeight / fullSizePixelWidth;
        }
        else
        {
            // Otherwise, it's the other way around -- full height, and width is set to 
            // preserve the aspect ratio.
            displayedPixelHeight = gridSquareSize;
            displayedPixelWidth = gridSquareSize * fullSizePixelWidth / fullSizePixelHeight;
        }

        // Actually put the image on the screen.
        gfx.drawImage(image,
            x - displayedPixelWidth / 2.0,  // Top-left pixel coordinates.
            y - displayedPixelHeight / 2.0, 
            displayedPixelWidth,              // Size of displayed image.
            displayedPixelHeight);
    }
    
    
    /**
     * Displays a string of text underneath a specific grid location. *Only* call this from within 
     * layoutChildren(). 
     *     
     * You shouldn't need to modify this method.
     */
    private void drawLabel(GraphicsContext gfx, String label, double gridX, double gridY)
    {
        gfx.setTextAlign(TextAlignment.CENTER);
        gfx.setTextBaseline(VPos.TOP);
        gfx.setStroke(Color.BLUE);
        gfx.strokeText(label, (gridX + 0.5) * gridSquareSize, (gridY + 1.0) * gridSquareSize);
    }
    
    /** 
     * Draws a (slightly clipped) line between two grid coordinates.
     *     
     * You shouldn't need to modify this method.
     */
    /*
    private void drawLine(GraphicsContext gfx, double gridX1, double gridY1, 
                                               double gridX2, double gridY2)
    {
        gfx.setStroke(Color.RED);
        
        // Recalculate the starting coordinate to be one unit closer to the destination, so that it
        // doesn't overlap with any image appearing in the starting grid cell.
        final double radius = 0.5;
        double angle = Math.atan2(gridY2 - gridY1, gridX2 - gridX1);
        double clippedGridX1 = gridX1 + Math.cos(angle) * radius;
        double clippedGridY1 = gridY1 + Math.sin(angle) * radius;
        
        gfx.strokeLine((clippedGridX1 + 0.5) * gridSquareSize, 
                       (clippedGridY1 + 0.5) * gridSquareSize, 
                       (gridX2 + 0.5) * gridSquareSize, 
                       (gridY2 + 0.5) * gridSquareSize);
    }
    */
}
