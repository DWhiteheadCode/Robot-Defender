package edu.curtin.saed.assignment1.entities.robot;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import edu.curtin.saed.assignment1.entities.robot.moves.*;
import edu.curtin.saed.assignment1.game_engine.GameEngine;
import edu.curtin.saed.assignment1.misc.Vector2d;

public class Robot implements Runnable
{
    public static final String IMAGE_FILE = "1554047213.png";

    private static final int MIN_MOVE_DELAY_MILLISECONDS = 500;
    private static final int MAX_MOVE_DELAY_MILLISECONDS = 2000;
    private static final int MOVE_DURATION_MILLISECONDS = 400;
    private static final int MOVE_ANIMATION_INTERVAL_MILLISECONDS = 40;

    private final int id;
    private final long moveDelayMilliseconds;
    private GameEngine gameEngine;
    private Vector2d coordinates;
    
    private RobotMoveCallback moveCallback;

    public Robot(int id, GameEngine gameEngine)
    {
        this.id = id;

        // Generate a random moveDelay between MIN and MAX move delays (inclusive)
        Random rand = new Random();
        this.moveDelayMilliseconds = rand.nextLong(
            MIN_MOVE_DELAY_MILLISECONDS,
            (MAX_MOVE_DELAY_MILLISECONDS + 1)
        );
        
        this.gameEngine = gameEngine;
        this.coordinates = null; // Coordinates must be set when the Robot is placed into the map by the gameEngine
    }


    /*
     * Defines the Robot's logic, as follows:
     *     - Sleeps for moveDelayMilliseconds
     *     - Determines its movePreferenceOrder based on citadel distance
     *     - Attempts to make moves until all moves have been tried, or GameEngine approves one
     *         - If a move was accepted, that move is made
     * 
     */
    @Override
    public void run() 
    {
        if(this.gameEngine == null)
        {
            throw new IllegalStateException("Can't start robot before setting its game engine.");
        }

        if(this.coordinates == null)
        {
            throw new IllegalStateException("Can't start robot before setting its coordinates.");
        }

        try
        {
            while(true)
            {       
                Thread.sleep(this.moveDelayMilliseconds);
                
                Vector2d citadelPos = gameEngine.getCitadel();

                // Sort possible moves based on weighted-randomness, with preference for moves that 
                // result in the robot being closer to the citadel
                List<Move> allMoves = allMoves(citadelPos);
                List<Move> movePreferenceOrder = generateMoveOrder(allMoves);

                //Attempt to make moves until one succeeds, or none left
                Move moveToMake = requestMoves(movePreferenceOrder);

                // If a move was approved, make it
                if(moveToMake != null)
                {
                    makeMove(moveToMake);
                }                
            }
        }
        catch(InterruptedException iE)
        {
            // Nothing needed here 
        }

    }


    /*
     * Sets a callback for this Robot to run when it finishes its move
     * 
     * Thread: Runs in the robot's thread, called by GameEngine.requestMove(), which is called by Robot.run()
     */
    public void setMoveCallback(RobotMoveCallback callback)
    {
        if(this.moveCallback != null)
        {
            throw new IllegalStateException("Can't set moveCallback for a robot as it already has one.");
        }

        this.moveCallback = callback;          
    }

    /*
     * Set the coordinates of this Robot
     * 
     * Thread: Called by robot-spawn-consumer thread initially, but only by this Robot's thread after that.
     */
    public void setCoordinates(Vector2d coordinates)
    {
        this.coordinates = coordinates;
    }


    /*
     * Try to make each move in "moves" in order.
     * 
     * If a move is successful, return it; otherwise return null
     */
    private Move requestMoves(List<Move> moves) throws InterruptedException
    {
        for(Move m : moves)
        {             
            if(  gameEngine.requestMove(this, m.getMoveVec())  )
            {
                return m;
            }
        }

        return null;
    }

    /*
     * Performs "move" on this robot in intervals specified by class constants
     * 
     * Updates GameEngine each interval
     */
    private void makeMove(Move move) throws InterruptedException
    {
        int numIntervals = MOVE_DURATION_MILLISECONDS / MOVE_ANIMATION_INTERVAL_MILLISECONDS;
        
        Vector2d intervalMoveVec = move.getMoveVec().divide(numIntervals); // The vector the robot should be moved by each interval

        // One loop represents one animation interval (i.e. one frame)
        for(int i = 0; i < numIntervals; i++)
        {
            Vector2d newPos = this.coordinates.plus(intervalMoveVec);

            gameEngine.updateRobotPos(this, newPos);

            Thread.sleep(MOVE_ANIMATION_INTERVAL_MILLISECONDS);
        }

        // Correct any floating point issues, with one final position update
        double newX = Math.round( coordinates.x() );
        double newY = Math.round( coordinates.y() );

        this.coordinates = new Vector2d(newX, newY);
        gameEngine.updateRobotPos(this, getCoordinates());

        // Tell the game engine that the move completed, and clear the callback for future moves
        moveCallback.moveComplete();
        moveCallback = null;
    }


    public int getId()
    {
        return this.id; // Doesn't change, and thus doesn't need to be synchronised
    }

    public Vector2d getCoordinates()
    {
        return new Vector2d(this.coordinates);
    }

    
    /*
     * Returns a list containing all possible moves the robot could make
     * (Up, down, left, right).
     */
    private List<Move> allMoves(Vector2d citadelPos)
    {
        List<Move> moves = new ArrayList<>();

        moves.add(new UpMove( getCoordinates(), citadelPos ));
        moves.add(new DownMove(getCoordinates(), citadelPos));
        moves.add(new LeftMove(getCoordinates(), citadelPos));
        moves.add(new RightMove(getCoordinates(), citadelPos));

        return moves;
    }

    /*
     * Sorts the list of possible moves based on weighted-randomness.
     * Move weightings are the robot's distance from the citadel after making that move.
     * 
     * E.g.:
     *      Consider the following possible moves, and their corresponding distances from the citadel:
     *          UP:      5
     *          DOWN:    10
     *          LEFT:    3
     *          RIGHT:   7
     * 
     *      On the first iteration, each move would have the following likelihoods of being "chosen"
     *          UP:      5 / (5 + 10 + 3 + 7)
     *          DOWN:    10 / (5 + 10 + 3 + 7)
     *          LEFT:    3 / (5 + 10 + 3 + 7)
     *          RIGHT:   7 / (5 + 10 + 3 + 7)
     * 
     *      After this iteration, the chosen move is added to the front of "orderedMoves", and is removed from "unorderedMoves"
     *          Assuming the most likely move is chosen:
     *              unorderedMoves = {UP, LEFT, RIGHT}  
     *              orderedMoves = {DOWN}
     * 
     *      For the next iteration, the remaining moves have the following probabilities of being chosen:
     *          UP:      5 / (5 + 3 + 7)
     *          LEFT:    3 / (5 + 3 + 7)
     *          RIGHT:   7 / (5 + 3 + 7)
     * 
     *      Assuming the most likely move is chosen:
     *              unorderedMoves = {UP, LEFT}  
     *              orderedMoves = {RIGHT, DOWN}
     *              
     *              Notice how "RIGHT", was placed in front of "DOWN" in the list.
     *                  This ensures the moves with the smallest distance from the citadel are more likely to end up at the start of the list    * 
     */
    private List<Move> generateMoveOrder(List<Move> unorderedMoves)
    {
        double totalDistance = 0;

        for(Move m : unorderedMoves)
        {
            totalDistance += m.getDistanceToCitadel();
        }

        List<Move> orderedMoves = new ArrayList<>();

        Random rand = new Random();

        while(!unorderedMoves.isEmpty()) // Each iteration (weighted) randomly selects one Move from unorderedMoves, and adds it to orderedMoves
        {
            double randNum = rand.nextDouble() * (totalDistance - 1);
            double count = 0;

            for(Move m : unorderedMoves) // Find the unorderedMove with the corresponding weight, and add it to the ordered list
            {
                if( randNum < m.getDistanceToCitadel() + count )
                {
                    // Add the chosen move to the start of the list. 
                    // This means items chosen first will get pushed to the back by new items
                    // Items with larger weights are more likely to be chosen first, thus they will be pushed to the back of the list
                    orderedMoves.add(0, m); 
                    break;
                }

                count += m.getDistanceToCitadel();
                
            }

            totalDistance -= orderedMoves.get(0).getDistanceToCitadel();
            unorderedMoves.remove( orderedMoves.get(0) );            
        }

        return orderedMoves;
    }

    













    
    

}
