package edu.curtin.saed.assignment1.entities.robot;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import edu.curtin.saed.assignment1.entities.robot.moves.*;
import edu.curtin.saed.assignment1.game_engine.GameEngine;
import edu.curtin.saed.assignment1.game_engine.MoveRequest;
import edu.curtin.saed.assignment1.misc.Vector2d;

public class Robot implements Runnable
{
    public static final String IMAGE_FILE = "1554047213.png";

    private final int MIN_MOVE_DELAY_MILLISECONDS = 500;
    private final int MAX_MOVE_DELAY_MILLISECONDS = 2000;
    private final int MOVE_DURATION_MILLISECONDS = 400;
    private final int MOVE_ANIMATION_INTERVAL_MILLISECONDS = 40;

    private final int id;
    private final long moveDelayMilliseconds;
    private GameEngine gameEngine;
    private Vector2d coordinates;
    
    private RobotMoveCallback moveCallback; 

    public Robot(int id, GameEngine gameEngine)
    {
        this.id = id;

        Random rand = new Random();
        this.moveDelayMilliseconds = rand.nextLong(
            MIN_MOVE_DELAY_MILLISECONDS,
            (MAX_MOVE_DELAY_MILLISECONDS + 1)
        );
        
        this.gameEngine = gameEngine;
        this.coordinates = null;
    }


    // Runs in Robot thread (called by GameEngine.requestMove(), which itself runs in robot's thread)
    public void setMoveCallback(RobotMoveCallback callback)
    {
        this.moveCallback = callback;
    }

    public void setCoordinates(Vector2d coordinates)
    {
        this.coordinates = coordinates;
    }

    //TODO - called when robot runs into a wall
    public void destroy()
    {

    }

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
            boolean dead = false;
            do
            {       
                Thread.sleep(this.moveDelayMilliseconds);
                
                Vector2d citadelPos = gameEngine.getCitadel();

                //Sort possible moves based on weighted-randomness
                List<Move> allMoves = allMoves(citadelPos);
                List<Move> moveOrder = generateMoveOrder(allMoves);

                //Attempt to make moves until one succeeds, or none left
                Move moveToMake = requestMoves(moveOrder);

                // If a move was approved, make it
                if(moveToMake != null)
                {
                    makeMove(moveToMake);
                }                
            }
            while(!dead);
        }
        catch(InterruptedException iE)
        {
            //TODO
        }

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
            MoveRequest request = new MoveRequest(this, m.getMoveVec());
                        
            if(gameEngine.requestMove(request))
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

        for(int i = 0; i < numIntervals; i++)
        {
            Vector2d newPos = this.coordinates.plus(intervalMoveVec);

            gameEngine.updateRobotPos(this, newPos);

            Thread.sleep(MOVE_ANIMATION_INTERVAL_MILLISECONDS);
        }

        // Correct any rounding issues
        double newX = Math.round( coordinates.x() );
        double newY = Math.round( coordinates.y() );

        this.coordinates = new Vector2d(newX, newY);
        gameEngine.updateRobotPos(this, getCoordinates());

        // Tell the game engine that the move completed
        moveCallback.moveComplete();
        moveCallback = null;
    }


    public int getId()
    {
        return this.id;
    }

    public Vector2d getCoordinates()
    {
        return new Vector2d(this.coordinates);
    }

    

    // Calculates the displacement from the citadel after making each possible move
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
     * Sort list of moves based on weighted randomness, where distanceToCitadel is the weight
     * 
     * Returned list prioritises moves with smaller distanceToCitadel at the front of the list
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

        while(!unorderedMoves.isEmpty()) // Each iteration randomly selects one Move from unorderedMoves, and adds it to orderedMoves
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
