package edu.curtin.saed.assignment1.entities.robot.moves;

import edu.curtin.saed.assignment1.misc.Vector2d;

public interface Move 
{
    Vector2d getMoveVec();
    Vector2d getVecToCitadel();
    double getDistanceToCitadel();
}
