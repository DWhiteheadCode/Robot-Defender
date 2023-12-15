# Build + Run

This project can be built and run using Gradle. Navigate to the project directory (`Robot-Defender`), then enter the following command based on your OS.

**On Linux:**

`./gradlew run`

**On Windows:**

`gradlew run`


## Code Quality Checking

A PMD ruleset has been provided to check code quality. This can be used with the following command:

**On Linux:**

`./gradlew check`

**On Windows:**

`gradlew check`

# Gameplay

## Basics

Robot Defender takes place on a 2D grid. At the centre of the grid is a square called the "citadel". 

Robots will spawn at a fixed rate in one of the 4 corners (a corner is chosen at random each time a robot spawns). Each robot is assigned a random "move delay" which determines how frequently it will move. Robots can move up, down, left or right; and prefer to move towards the citadel. If a square is occupied by a robot, no other robot can move to that square. If a robot reaches the citadel, the game is over. 

The player can click on squares in the grid to place "Fortress Walls" (or "walls", for short). If a robot moves into a square containing a wall, that robot will be destroyed. When hit by a robot, a wall will become damaged. If a damaged wall is hit by another robot, the wall will be destroyed (along with the robot). 

The player gains points passively, for as long as they survive. They also gain additional points each time a robot hits a wall.

## Wall Details

The player has a limited number of walls that they can place at any one time. There is also a short cooldown after a wall is placed before the next wall can be placed. While the wall cooldown is ongoing, subsequent clicks will result in walls being "queued" for placement (though there is a maximum number of walls that can be queued). Each wall will be placed as soon as the previous wall's cooldown completes. 

When a wall is queued, if its square is occupied at the time it is ready to be placed, that wall will be ignored (though a cooldown will start before the next wall is placed).

Walls can be placed on top of existing walls. This destroys the existing wall, and replaces it with the new wall. This is typically only useful to "refresh" a damaged wall. 

# Project Origin (Design Brief)

This game began as my submission for a University assignment, though it has been extended beyond that point. The version submitted for this assessment can be found in the branch `Assignment-Submission-Archive`. 

The assessment specification detailed the gameplay requirements, including the grid size, the robots' possible movements, all timings (such as robot min/max move delay, robot spawn delay, wall spawn delay, etc), and scoring details. The specification also defined certain criteria that had to be met, such as "the reasonable use of at least one Blocking Queue". 

Provided with the assignment specification was a template for the gameplay UI, basic build files, entity images, and a PMD ruleset; all of which can be seen in the initial commit. 

# Planned Future Work

- Write unit tests

- Add a difficulty setting which impacts the following:

    - Grid size

    - Robot move timings

    - Robot move sets (such as moving 2 squares at a time, or like a horse in chess, etc)

    - Wall cooldown

    - Max number of walls

    - Robot aggression

        - Rewrite movement algorithm to choose the "optimal" move more or less often

- Investigate the benefits/ practicality of converting some threads from Runnables with "Thread.sleep()" to TimerTasks executed by Timers with scheduleAtFixedRate(). 

# References

## Sounds

All sounds were sourced from https://freesound.org/ under the "Creative Commons 0" license. Some sounds were trimmed using Audacity. The sound effects used are as follows:

- Wall placement: https://freesound.org/people/Reitanna/sounds/323725/

- Wall collision: https://freesound.org/people/jadevelloza/sounds/655350/

- Wall destruction: https://freesound.org/people/iwanPlays/sounds/567249/

- Game Over: https://freesound.org/people/Breviceps/sounds/493163/

## Template

As described in `Project Origin (Design Brief)` above, the template for the project was provided by Dr David Cooper, as a part of the assignment specification that this project was based on. 