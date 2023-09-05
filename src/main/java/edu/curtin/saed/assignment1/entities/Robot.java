package edu.curtin.saed.assignment1.entities;

import java.util.Random;

import edu.curtin.saed.assignment1.GameEngine;
import edu.curtin.saed.assignment1.misc.Coordinates;

public class Robot implements Runnable
{
    private final int MIN_MOVE_DELAY_MILLISECONDS = 500;
    private final int MAX_MOVE_DELAY_MILLISECONDS = 2000;

    private final int id;
    private final long moveDelayMilliseconds;
    private GameEngine gameEngine;
    private Coordinates coordinates;
    
    public Robot(int id, GameEngine gameEngine)
    {
        this.id = id;

        Random rand = new Random();
        this.moveDelayMilliseconds = rand.nextLong(
            MIN_MOVE_DELAY_MILLISECONDS,
            MAX_MOVE_DELAY_MILLISECONDS + 1
        );
        
        this.gameEngine = gameEngine;
        this.coordinates = null;
    }

    public void setCoordinates(Coordinates coordinates)
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

                //Sort possible moves based on ordered-randomness

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

    public Coordinates getCoordinates()
    {
        return new Coordinates(this.coordinates);
    }














    
    

}
