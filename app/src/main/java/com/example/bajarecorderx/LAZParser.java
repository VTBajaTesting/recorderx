import java.io.File;
// import .LASReader;

public class LAZParser {

    private File lazFile;
    private LASReader reader;

    /**
     * Creates a new LAZParser
     *
     * @param lazStream a stream of laz data to parse
     * @return Returns a LAZParser
     */
    public LAZParser(String lazStream) {
        lazfile = new File("./temp.laz");
        FileWriter writer = new FileWriter("./temp.laz");
        writer.write(lazStream);
        reader = new LASReader(lazFile);
    }

    private void getLASPoint(double xMin, double yMin, double xMax, double yMax) {
        ArrayList<LASPoint> points = new ArrayList<LASPoint>();
        while (points.size() > 4) {
            reader = reader.insideRectangle(xMin, yMin, xMax, yMax);
            points = reader.getPoints();
            xMin += 0.5;
            yMin += 0.5;
            xMax -= 0.5;
            yMax -= 0.5;
            
        }
    }

    public void getElevation() {
        reader.insideRectangle();
    }


    public void close () {
        lazfile.close();
    }

}
