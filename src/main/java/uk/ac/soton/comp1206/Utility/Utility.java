package uk.ac.soton.comp1206.Utility;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javafx.scene.image.Image;

/**
 * Utility class to handle file IO
 */
public class Utility {
    protected static final Logger logger = LogManager.getLogger(Utility.class);
    
    /**
     * Gets a stylesheet from the style folder
     * @param filename the name of the stylesheet
     * @return the stylesheet
     */
    public static String getStyle(String filename) {
        logger.info("Getting stylesheet '{}'", filename);
        return Utility.class.getResource("/style/" + filename).toExternalForm();
    }

    /**
     * Gets an image from the images folder
     * @param filename the name of the image
     * @return the image
     */
    public static Image getImage(String filename) {
        logger.info("Getting image '{}'", filename);
        return new Image(Utility.class.getResource("/images/" + filename).toExternalForm());
    }

    /**
     * Reads the data from a file and stores each line
     * @param filename the name of the file
     * @return an arraylist of the files lines
     */
    public static ArrayList<String> readFromFile(String filename) {
        try {
            BufferedReader br = new BufferedReader(
                new FileReader(
                    new File(Utility.class.getResource(filename).toURI())
                )
            );

            String line;
            var output = new ArrayList<String>();

            //Reads file line by line
            while ((line = br.readLine()) != null) output.add(line);

            return output;
        } catch (FileNotFoundException | URISyntaxException e) {
            logger.error("File {} not found", filename);
        } catch (IOException e) {
            logger.error("Error reading file {}", filename);
        }

        return null;
    }

    /**
     * Writes a given string to a file
     * @param filename the file to write to
     * @param lines the string to write to the file
     */
    public static void writeToFile(String filename, String lines) {
        try {
            var fw = new FileWriter(
                new File(Utility.class.getResource(filename).toURI())
            );
            
            fw.write(lines);
            fw.close();
            logger.info("Writing {} to '{}'", lines, filename);

        } catch (URISyntaxException | IOException e) {
            logger.error("Error writing to file {}", filename);
        }

    }
}
