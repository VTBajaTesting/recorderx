import java.io.IOException;

import org.json.simple.parser.ParseException;

public class Main {
    public static void main(String[] args) throws ParseException, IOException, InterruptedException {

        TopoAPI api = new TopoAPI();


        String lazData = null;
        String dbURL = null;

        dbURL = api.getDatabaseURL(-80.531693, 37.152432);
        // try {
        // } catch (ParseException e) {
        //     System.exit(1);
        // } catch (IOException e) {
        //     System.exit(1);
        // } catch (InterruptedException e) {
        //     System.exit(1);
        // }

        try {
            lazData = api.loadLazData(dbURL);
        } catch (IOException e) {
            System.exit(1);
        } catch (InterruptedException e) {
            System.exit(1);
        }

        // LAZParser lazparser = new LAZParser(lazData);

    }
}
