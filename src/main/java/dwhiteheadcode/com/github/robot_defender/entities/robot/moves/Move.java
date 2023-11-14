package dwhiteheadcode.com.github.robot_defender.entities.robot.moves;

import dwhiteheadcode.com.github.robot_defender.misc.Vector2d;

/*
 * An interface to represent a possible move a Robot could make
 */
public interface Move 
{
    Vector2d getMoveVec(); // The vector to add to the robot's original position to get its new position after making this move
    Vector2d getVecToCitadel(); // The vector needed to be added to the robot's position *after* this move to get to the citadel
    double getDistanceToCitadel(); // The robot's distance from the citadel *after* making this move
}
