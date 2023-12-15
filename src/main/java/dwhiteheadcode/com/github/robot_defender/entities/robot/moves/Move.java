package dwhiteheadcode.com.github.robot_defender.entities.robot.moves;

import dwhiteheadcode.com.github.robot_defender.misc.Vector2d;

/*
 * Represents a possible move a Robot could make
 */
public class Move 
{
    private Vector2d moveVec; // The vector to add to the robot's original position to get its new position after making this move
    private double distanceToCitadel; // The robot's distance from the citadel *after* making this move

    public Move(Vector2d moveVec, Vector2d robotStartPos, Vector2d citadelPos)
    {
        this.moveVec = moveVec;
        
        Vector2d robotEndPos = robotStartPos.plus(moveVec); // The robot's pos *after* making this move
        Vector2d vecToCitadel = citadelPos.minus(robotEndPos); // The vector that would need to be added to the robot's pos *after* this move to reach the citadel

        this.distanceToCitadel = vecToCitadel.size();
    }

    public Vector2d getMoveVec()
    {
        return moveVec;
    }

    public double getDistanceToCitadel()
    {
        return distanceToCitadel;
    }
}
