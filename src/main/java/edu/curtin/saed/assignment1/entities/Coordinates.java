package edu.curtin.saed.assignment1.entities;

public class Coordinates 
{
    private int x;
    private int y;

    public Coordinates(int x, int y)
    {
        this.x = x;
        this.y = y;
    }

    public void setX(int x)
    {
        this.x = x;
    }

    public void setY(int y)
    {
        this.y = y;
    }
    
    public int x()
    {
        return x;
    }

    public int y()
    {
        return y;
    }

}
