package edu.curtin.saed.assignment1.entities;

import edu.curtin.saed.assignment1.GameEngine;

public class Robot implements Runnable
{
    private final int id;
    private final long moveDelayMilliseconds;
    private GameEngine gameEngine;
    private Coordinates coordinates;
    
    public Robot(int id, long moveDelayMilliseconds)
    {
        this.id = id;
        this.moveDelayMilliseconds = moveDelayMilliseconds;
        this.gameEngine = null;
        this.coordinates = null;
    }

    public void setGameEngine(GameEngine engine)
    {
        this.gameEngine = engine;
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















    
    

}
