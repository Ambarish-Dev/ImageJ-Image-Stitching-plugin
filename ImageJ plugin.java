import ij.*;
import ij.process.*;
import java.awt.*;
import java.util.*;

public class ImageStitcher implements PlugIn {
    
    public void run(String arg) {
        
        // Prompt user to select images to stitch
        OpenDialog od = new OpenDialog("Select images to stitch", arg);
        String[] fileNames = od.getFileNames();
        String directory = od.getDirectory();
        
        // Load images and convert them to ImageProcessor objects
        ArrayList<ImageProcessor> processors = new ArrayList<ImageProcessor>();
        for (String fileName : fileNames) {
            ImagePlus imp = new Opener().openImage(directory + fileName);
            ImageProcessor processor = imp.getProcessor();
            processors.add(processor);
        }
        
        // Find image overlaps
        HashMap<String, int[]> overlaps = new HashMap<String, int[]>();
        for (int i = 0; i < processors.size() - 1; i++) {
            for (int j = i + 1; j < processors.size(); j++) {
                int[] overlap = findOverlap(processors.get(i), processors.get(j));
                if (overlap != null) {
                    overlaps.put(i + "," + j, overlap);
                }
            }
        }
        
        // Calculate image positions
        HashMap<Integer, int[]> positions = new HashMap<Integer, int[]>();
        int x = 0;
        int y = 0;
        for (int i = 0; i < processors.size(); i++) {
            int[] position = {x, y};
            positions.put(i, position);
            x += processors.get(i).getWidth() - overlaps.get(i + "," + (i+1))[0];
        }
        
        // Create new image to stitch images into
        int width = x;
        int height = processors.get(0).getHeight();
        ImageProcessor newProcessor = new ColorProcessor(width, height);
        ImagePlus newImp = new ImagePlus("Stitched Image", newProcessor);
        
        // Copy images into new image at correct positions
        for (int i = 0; i < processors.size(); i++) {
            int[] position = positions.get(i);
            int xPosition = position[0];
            int yPosition = position[1];
            ImageProcessor processor = processors.get(i);
            newProcessor.insert(processor, xPosition, yPosition);
        }
        
        // Save the stitched image
        String savePath = directory + "stitched.tif";
        FileSaver fs = new FileSaver(newImp);
        fs.saveAsTiff(savePath);
    }
    
    // Method to find the overlap between two images
    private int[] findOverlap(ImageProcessor ip1, ImageProcessor ip2) {
        int[] overlap = new int[2];
        ImageStatistics stats1 = ImageStatistics.getStatistics(ip1, ImageStatistics.MIN_MAX, null);
        ImageStatistics stats2 = ImageStatistics.getStatistics(ip2, ImageStatistics.MIN_MAX, null);
        int maxOverlap = Math.min(ip1.getWidth(), ip2.getWidth()) / 2;
        for (int i = maxOverlap; i >= 0; i--) {
            boolean match = true;
            for (int j = 0; j < ip1.getHeight(); j++) {
                int pixel1 = ip1.getPixel(ip1.getWidth() - 1 - i, j);
                int pixel2 = ip2.getPixel(i, j);
                if (Math.abs(pixel1 - pixel2) > (stats1.max - stats1.min) / 10) {
                    match = false;
                    break;
                }
            }
            if (match) {
                overlap[0] = i;
                break;
            }
        }
        if (overlap[0] == 0) {
            return null;
        } else {
            overlap[1] = ip1.getWidth() - overlap[0];
            return overlap;
        }
    }
}

