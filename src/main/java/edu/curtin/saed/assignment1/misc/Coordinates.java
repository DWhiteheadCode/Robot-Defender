package edu.curtin.saed.assignment1.misc;

public class Coordinates 
{
    private int x;
    private int y;

    public Coordinates(int x, int y)
    {
        this.x = x;
        this.y = y;
    }

    public Coordinates(Coordinates copy)
    {
        this.x = copy.x();
        this.y = copy.y();
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
