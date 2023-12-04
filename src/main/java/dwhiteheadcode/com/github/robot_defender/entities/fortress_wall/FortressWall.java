package dwhiteheadcode.com.github.robot_defender.entities.fortress_wall;

import javafx.scene.media.*;

import java.net.URL;

import dwhiteheadcode.com.github.robot_defender.game_engine.GameEngine;
import dwhiteheadcode.com.github.robot_defender.misc.Vector2d;

public class FortressWall 
{
    public static final String UNDAMAGED_IMAGE_FILE = "images/wall_default.png";
    public static final String DAMAGED_IMAGE_FILE = "images/wall_damaged.png";

    public static final String COLLISION_SOUND_FILE = "sounds/wall_collision.wav";
    public static final String DESTRUCTION_SOUND_FILE = "sounds/wall_destruction.wav";
    public static final String PLACEMENT_SOUND_FILE = "sounds/wall_placement.wav";
    
    private MediaPlayer collisionSoundPlayer;
    private MediaPlayer destructionSoundPlayer;
    private MediaPlayer placementSoundPlayer;

    private final Vector2d coordinates;
    private boolean isDamaged; // Not locked because GameEngine prevents multiple robots from colliding with the wall at the same time.               

    private GameEngine gameEngine;

    public FortressWall(GameEngine gameEngine, Vector2d coordinates)
    {
        this.gameEngine = gameEngine;
        this.isDamaged = false;
        this.coordinates = coordinates;

        loadSounds();
    }

    private void loadSounds()
    {
        // Load Placement Sound
        URL placementSoundUri = getClass().getClassLoader().getResource(PLACEMENT_SOUND_FILE);
        Media placementSound = new Media(placementSoundUri.toString());
        this.placementSoundPlayer = new MediaPlayer(placementSound);
        this.placementSoundPlayer.setVolume(0.3);

        // Load Collision Sound
        URL collisionSoundUri = getClass().getClassLoader().getResource(COLLISION_SOUND_FILE);
        Media collisionSound = new Media(collisionSoundUri.toString());
        this.collisionSoundPlayer = new MediaPlayer(collisionSound);
        this.collisionSoundPlayer.setVolume(0.5);

        // Load Destruction Sound
        URL destructionSoundUri = getClass().getClassLoader().getResource(DESTRUCTION_SOUND_FILE);
        Media destructionSound = new Media(destructionSoundUri.toString());
        this.destructionSoundPlayer = new MediaPlayer(destructionSound);
        this.destructionSoundPlayer.setVolume(0.1);
    }


    /*
     * Returns a copy of this Wall's coordinates
     * 
     * These are never modified, and so don't need to be synchronized
     * 
     * Thread: Called by wall-spawn-consumer thread, or robot thread
     */
    public Vector2d getCoordinates()
    {
        return new Vector2d(this.coordinates);
    }

    /*
     * Returns false if the wall is not damaged, and true if it is.
     * 
     * Thread: Called by UI thread. GameEngine is responsible for locking this.
     */
    public boolean isDamaged()
    {
        return isDamaged;
    }

    public MediaPlayer getPlacementSound()
    {
        return this.placementSoundPlayer;
    }

    /*
     * Called when a robot hits this wall.
     * 
     * If the wall was not damaged prior to this call, it becomes damaged.
     * If it was damaged prior to this call, it tells the gameEngine that it needs to be destroyed
     * 
     * Thread: Called by a robot's thread.
     */
    public void damage()
    {
        if(isDamaged) // Destroy the wall
        {
            this.destructionSoundPlayer.play();   

            gameEngine.destroyWall(this);
        }
        else // Damage the wall
        {
            this.collisionSoundPlayer.play();
           
            this.isDamaged = true;
            gameEngine.updateArenaUi();
        }      
    }
}
