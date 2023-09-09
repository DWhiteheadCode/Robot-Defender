package edu.curtin.saed.assignment1.misc;

/*
 * Represents a 2d vector with double (real) x and y components
 */
public class Vector2d 
{
    private double x;
    private double y;

    public Vector2d(double x, double y)
    {
        this.x = x;
        this.y = y;
    }

    public Vector2d(Vector2d copy)
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
    
    public double x()
    {
        return x;
    }

    public double y()
    {
        return y;
    }

    @Override
    public String toString()
    {
        return "(" + x + ", " + y + ")"; 
    }

    // Returns the vector resulting from (this - vec)
    public Vector2d minus(Vector2d vec)
    {
        double xDelta = this.x - vec.x();
        double yDelta = this.y - vec.y();

        return new Vector2d( xDelta, yDelta );
    }

    public Vector2d plus(Vector2d vec)
    {
        double newX = this.x + vec.x();
        double newY = this.y + vec.y();

        return new Vector2d( newX, newY );
    }

    // Returns the vector resulting from (this / vec)
    public Vector2d divide(double denominator)
    {
        double newX = this.x / denominator;
        double newY = this.y / denominator;

        return new Vector2d( newX, newY );
    }

    // Returns the length of this vector
    public double size()
    {
        return Math.sqrt( (x*x) + (y*y) );
    }

}
