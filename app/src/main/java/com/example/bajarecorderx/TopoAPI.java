import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.io.File;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.MappedByteBuffer;
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 * Contains methods for retrieving data from USGS Lidar Point Cloud Data
 *
 * @author Teddy Bird
 * @version 02.04.2022
 *
 */
public class TopoAPI {

    static String baseURL = "https://tnmaccess.nationalmap.gov/api/v1/products?";
    static String tileURL = "https://prd-tnm.s3.amazonaws.com/StagedProducts/Elevation/13/TIFF/historical/n38w081/USGS_13_n38w081_20210305.tif";

    /**
     * Gets Lidar Point Cloud (LPC) database url for a specifed latitude and longitude
     *
     * @param lat Latitude coordinate as a double (no hemisphere specifier required)
     * @param lon Longitude coordinate as a double (no hemisphere specifier required
     *
     * @return Returns the a url to a database containing the LPC data
     */
    public String getDatabaseURL(double lat, double lon) throws IOException, InterruptedException, ParseException {

        String bbox  = String.format("bbox=%f,%f,%f,%f", lat, lon, lat, lon);

        String dataSet = "&datasets=Lidar%20Point%20Cloud%20(LPC)";

        String prodFormat = "&prodFormats=LAS,LAZ";


        String requestStr = String.format("%s%s%s%s", baseURL, bbox, dataSet, prodFormat);
        // System.out.println(requestStr);

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(requestStr))
            .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        JSONParser parser = new JSONParser();

        JSONObject databaseInfo = (JSONObject) parser.parse(response.body());

        JSONArray databases = (JSONArray) databaseInfo.get("items");

        String downloadURL = null;
        if (databases.size() != 0) {
            downloadURL = (String) ((JSONObject) databases.get(0)).get("downloadURL");
        } else {
            System.out.println("no data available for region");
            System.exit(1);
        }

        return downloadURL;
    }

    public String loadLazData(String url) throws IOException, InterruptedException{

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        return response.body();
    }

}
