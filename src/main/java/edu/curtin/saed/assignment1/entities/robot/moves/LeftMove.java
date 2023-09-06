package edu.curtin.saed.assignment1.entities.robot.moves;

import edu.curtin.saed.assignment1.misc.Vector2d;

public class LeftMove implements Move
{
    private Vector2d moveVec = new Vector2d(-1, 0);
    private Vector2d vecToCitadel; // The vector from the robot's startPos -> citadel
    private double distanceToCitadel;

    public LeftMove(Vector2d startPos, Vector2d citadelPos)
    {
        Vector2d endPos = startPos.plus(moveVec);
        this.vecToCitadel = citadelPos.minus(endPos);
        this.distanceToCitadel = vecToCitadel.size();
    }

    @Override
    public Vector2d getMoveVec()
    {
        return new Vector2d(moveVec);
    }

    @Override
    public Vector2d getVecToCitadel()
    {
        return new Vector2d(vecToCitadel);
    }

    @Override
    public double getDistanceToCitadel()
    {
        return distanceToCitadel;
    }

}
