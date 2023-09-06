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

    private final int MIN_MOVE_DELAY_MILLISECONDS = 500;
    private final int MAX_MOVE_DELAY_MILLISECONDS = 2000;

    private final int id;
    private final long moveDelayMilliseconds;
    private GameEngine gameEngine;
    private Vector2d coordinates;
    
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

    public void setCoordinates(Vector2d coordinates)
    {
        this.coordinates = coordinates;
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
                //Comapre coords with citadel coords
                Vector2d citadelPos = gameEngine.getCitadel();
                                
                //Sort possible moves based on weighted-randomness
                List<Move> allMoves = allMoves(citadelPos);
                List<Move> moveOrder = generateMoveOrder(allMoves);
                
                //Attempt to make moves until one succeeds
                
                
                Thread.sleep(moveDelayMilliseconds);
            }
            while(!dead);
        }
        catch(InterruptedException iE)
        {

        }

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

    // Sort the possible moves based on weighted randomness, with weights corresponding to distance from citadel after making the mvoe
    private List<Move> generateMoveOrder(List<Move> unorderedMoves)
    {
        double totalDistance = 0;

        for(Move m : unorderedMoves)
        {
            totalDistance += m.getDistanceToCitadel();
        }

        List<Move> orderedMoves = new ArrayList<>();

        Random rand = new Random();

        while(!unorderedMoves.isEmpty())
        {
            double randNum = rand.nextDouble() * (totalDistance - 1);
            double count = 0;

            for(Move m : unorderedMoves)
            {
                if( randNum < m.getDistanceToCitadel() + count )
                {
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
