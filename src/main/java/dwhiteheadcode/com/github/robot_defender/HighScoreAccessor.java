package dwhiteheadcode.com.github.robot_defender;

import java.util.Optional;
import java.io.*;

public class HighScoreAccessor 
{
    private static final String HIGHSCORE_FILE_NAME = "HighScore.txt";

    /*
     * Loads the saved highscore. 
     * 
     * Returns either:
     *      An empty Optional if no highscore could be loaded. 
     * 
     *      OR
     * 
     *      An Optional containing the loaded highscore.
     */
    public Optional<Integer> getHighScore()
    {
        File highScoreFile = new File(HIGHSCORE_FILE_NAME);

        if( ! highScoreFile.exists() )
        {
            return Optional.empty();
        }

        try(
            FileReader scoreReader = new FileReader(highScoreFile);
            BufferedReader buffer = new BufferedReader(scoreReader);)
        {
            String line = buffer.readLine();
            int highScore = Integer.valueOf(line);
            
            return Optional.of(highScore);
        }
        catch(IOException | NumberFormatException e )
        {
            return Optional.empty();
        }
    }

    /*
     * Attempts to save the given score to the HighScore file, deleting the previously saved highscore (if there was one).
     */
    public void saveHighScore(int score) throws IOException
    {
        File highScoreFile = new File(HIGHSCORE_FILE_NAME);

        if(highScoreFile.exists())
        {
            highScoreFile.delete();
        }

        OutputStreamWriter osw = new OutputStreamWriter( new FileOutputStream(highScoreFile));
        
        try(BufferedWriter bw = new BufferedWriter(osw))
        {
            String scoreString = String.valueOf(score);

            bw.write(scoreString);
        }        
    }
    
}
