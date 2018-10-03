import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import javax.naming.InitialContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import javax.sql.DataSource;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.*;

/**
 * Created by merdeev on 14/9/17.
 */
@WebServlet( name = "RESTfulAppli", urlPatterns = {"/", "/upload"} )
@MultipartConfig(fileSizeThreshold = 1024*1024+6, // 6 MB
        maxFileSize = 1024*1024*10, // 10 MB
        maxRequestSize = 1024*1024*20 // 20 MB
)
public class MainActivity extends HttpServlet
{
    private static Connection conn;
    private static final String JNDI = "java:/magenta/datasource/test-distance-calculator";
    private static final String UPLOAD_DIR = "uploads";
    private static final String PN_CALCULATION_TYPE = "Calculation Type";
    private static final String PN_FROM_CITY = "From City";
    private static final String PN_TO_CITY = "To City";
    private static final String PV_CROWFLIGHT = "Crowflight";
    private static final String PV_DISTANCE_MATRIX = "Distance Matrix";
    private static final String PV_ALL = "All";
    private static final String TAG_CITY = "city";
    private static final String TAG_NAME = "name";
    private static final String TAG_LATITUDE = "latitude";
    private static final String TAG_LONGITUDE = "longitude";
    private static final String TAG_DISTANCE = "distance";
    private static final String TAG_FROM_CITY = "from_city";
    private static final String TAG_TO_CITY = "to_city";
    private static final String TAG_VALUE = "value";

    @Override
    protected void doGet( HttpServletRequest req, HttpServletResponse resp ) throws IOException
    {
        try {
            conn = ((DataSource) new InitialContext().lookup(JNDI)).getConnection();
            resp.getWriter().write(makeHtml());
        }
        catch (Exception ex) {
            ex.printStackTrace(resp.getWriter());
        }
        finally {
            try {
                if (conn != null) conn.close();
            }
            catch (SQLException ex) {
                ex.printStackTrace(resp.getWriter());
            }
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            conn = ((DataSource) new InitialContext().lookup(JNDI)).getConnection();
            String contentType = req.getContentType();
            if (contentType.equals("application/x-www-form-urlencoded")) {
                parseParameters(req, resp);
            }
            else if (contentType.contains("multipart/form-data")) {
                uploadFiles(req, resp);
            }
            else {
                resp.setStatus(HttpServletResponse.SC_NOT_IMPLEMENTED);
            }
        }
        catch (Exception ex) {
            ex.printStackTrace(resp.getWriter());
        }
        finally {
            try {
                if (conn != null) conn.close();
            }
            catch (SQLException ex) {
                ex.printStackTrace(resp.getWriter());
            }
        }
    }

    private void parseParameters (HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String calculationType = req.getParameter(PN_CALCULATION_TYPE);
        String fromCity = req.getParameter(PN_FROM_CITY);
        String toCity = req.getParameter(PN_TO_CITY);
        float crowflight;
        float matrix;

        try {
            switch (calculationType) {
                case PV_CROWFLIGHT:
                    crowflight = getCrowflight(fromCity, toCity);
                    resp.getWriter().write(PV_CROWFLIGHT + ": " + crowflight + " km;\n");
                    break;
                case PV_DISTANCE_MATRIX:
                    matrix = getDistanceMatrix(fromCity, toCity);
                    resp.getWriter().write(PV_DISTANCE_MATRIX + ": " + matrix + " km;\n");
                    break;
                case PV_ALL:
                    crowflight = getCrowflight(fromCity, toCity);
                    matrix = getDistanceMatrix(fromCity, toCity);
                    resp.getWriter().write(PV_CROWFLIGHT + ": " + crowflight + " km;\n");
                    resp.getWriter().write(PV_DISTANCE_MATRIX + ": " + matrix + " km;\n");
                    break;
                default:
                    resp.setStatus(HttpServletResponse.SC_NOT_IMPLEMENTED);
                    break;
            }
        }
        catch (Exception ex) {
                resp.getWriter().write("Distance can not be displayed");
        }
    }

    private float getCrowflight(String fromCity, String toCity) throws Exception {
        ResultSet rs;
        rs= conn.createStatement().executeQuery("select " + TAG_LATITUDE + ", " + TAG_LONGITUDE + " from " + TAG_CITY + " where " + TAG_NAME + " = '" + fromCity + "'");
        if (!rs.next()) {
            throw new NoSuchElementException();
        }
        double from_latitude = ((double)rs.getFloat(TAG_LATITUDE) * Math.PI)/180d;
        double from_longitude = ((double)rs.getFloat(TAG_LONGITUDE) * Math.PI)/180d;

        rs = conn.createStatement().executeQuery("select " + TAG_LATITUDE + ", " + TAG_LONGITUDE + " from " + TAG_CITY + " where " + TAG_NAME + " = '" + toCity + "'");
        if (!rs.next()) {
            throw new NoSuchElementException();
        }
        double to_latitude = ((double)rs.getFloat(TAG_LATITUDE) * Math.PI)/180d;
        double to_longitude = ((double)rs.getFloat(TAG_LONGITUDE) * Math.PI)/180d;
        double distance = 6365.56d * Math.acos(Math.sin(from_latitude)*Math.sin(to_latitude) + Math.cos(from_latitude)*Math.cos(to_latitude)*Math.cos(to_longitude-from_longitude));
        return (float)(Math.round(distance*10))/10f;
    }

    private float getDistanceMatrix(String fromCity, String toCity) throws Exception {
        ResultSet rs;
        rs= conn.createStatement().executeQuery("select " + TAG_VALUE + " from " + TAG_DISTANCE + " where " + TAG_FROM_CITY + " = '" + fromCity + "' and " + TAG_TO_CITY + " = '" + toCity + "'");
        if (!rs.next()) {
            rs= conn.createStatement().executeQuery("select " + TAG_VALUE + " from " + TAG_DISTANCE + " where " + TAG_FROM_CITY + " = '" + toCity + "' and " + TAG_TO_CITY + " = '" + fromCity + "'");
            if (!rs.next()) {
                throw new NoSuchElementException();
            }
        }

        return rs.getFloat(TAG_VALUE);
    }

    private void uploadFiles (HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException, ParserConfigurationException, SAXException, SQLException {
        // gets absolute path of the web application
        String applicationPath = req.getServletContext().getRealPath("");
        // constructs path of the directory to save uploaded file
        String uploadFolderPath = applicationPath + File.separator + UPLOAD_DIR;
        // creates upload folder if it does not exists
        File uploadFolder = new File(uploadFolderPath);
        if (!uploadFolder.exists()) {
            if (!uploadFolder.mkdirs()) {
                throw new IOException();
            }
        }

        // write all files in upload folder
        for (Part part : req.getParts()) {
            if (part != null && part.getSize() > 0) {
                String fileName = part.getSubmittedFileName();
                String contentType = part.getContentType();

                // allows only XML files to be uploaded
                if (!contentType.equalsIgnoreCase("text/xml")) {
                    continue;
                }

                String uploadFilePath = uploadFolderPath + File.separator + fileName;
                File uploadFile = new File(uploadFilePath);
                if (uploadFile.exists()) {
                    if (!uploadFile.delete()) {
                        throw new IOException();
                    }
                }
                part.write(uploadFilePath);

                parseFile(uploadFile);
            }
        }

        resp.setStatus(HttpServletResponse.SC_OK);
    }

    private void parseFile (File file) throws IOException, ParserConfigurationException, SAXException, SQLException {
        Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file);
        Node root = document.getDocumentElement();

        NodeList tables = root.getChildNodes();
        for (int i = 0; i < tables.getLength(); i++) {
            Node table = tables.item(i);
            String table_name = table.getNodeName();
            // If node is not a text
            if (table.getNodeType() != Node.TEXT_NODE) {
                NodeList attributes = table.getChildNodes();
                if (table_name.equals(TAG_CITY)) {
                    String name = null;
                    String latitude = null;
                    String longitude = null;
                    for (int j = 0; j < attributes.getLength(); j++) {
                        Node attribute = attributes.item(j);
                        String attribute_name = attribute.getNodeName();
                        // If node is not a text
                        if (attribute.getNodeType() != Node.TEXT_NODE) {
                            switch (attribute_name) {
                                case TAG_NAME:
                                    name = attribute.getChildNodes().item(0).getTextContent();
                                    break;
                                case TAG_LATITUDE:
                                    latitude = attribute.getChildNodes().item(0).getTextContent();
                                    break;
                                case TAG_LONGITUDE:
                                    longitude = attribute.getChildNodes().item(0).getTextContent();
                                    break;
                            }
                        }
                    }

                    if (name != null && latitude != null && longitude != null) {
                        insertCity(name, latitude, longitude);
                    }
                }
                else if (table_name.equals(TAG_DISTANCE)) {
                    String from_city = null;
                    String to_city = null;
                    String value = null;
                    for (int j = 0; j < attributes.getLength(); j++) {
                        Node attribute = attributes.item(j);
                        String attribute_name = attribute.getNodeName();
                        // If node is not a text
                        if (attribute.getNodeType() != Node.TEXT_NODE) {
                            switch (attribute_name) {
                                case TAG_FROM_CITY:
                                    from_city = attribute.getChildNodes().item(0).getTextContent();
                                    break;
                                case TAG_TO_CITY:
                                    to_city = attribute.getChildNodes().item(0).getTextContent();
                                    break;
                                case TAG_VALUE:
                                    value = attribute.getChildNodes().item(0).getTextContent();
                                    break;
                            }
                        }
                    }

                    if (from_city != null && to_city != null && value != null) {
                        insertDistance(from_city, to_city, value);
                    }
                }
            }
        }
    }

    private void insertCity (String name, String latitude, String longitude) throws SQLException {
        ResultSet rs = conn.createStatement().executeQuery("select " + TAG_NAME + " from " + TAG_CITY + " where " + TAG_NAME + " = '" + name + "'");
        if (rs.next()) {
            conn.createStatement().execute("delete from " + TAG_CITY + " where " + TAG_NAME + " = '" + name + "'");
        }
        conn.createStatement().execute( "insert into " + TAG_CITY + " (" + TAG_NAME + ", " + TAG_LATITUDE + ", " + TAG_LONGITUDE + ") values ('" + name + "', " + latitude + ", " + longitude + ")");
    }

    private void insertDistance (String from_city, String to_city, String value) throws SQLException {
        ResultSet rs = conn.createStatement().executeQuery("select " + TAG_FROM_CITY + ", " + TAG_TO_CITY + " from " + TAG_DISTANCE + " where (" + TAG_FROM_CITY + " = '" + from_city + "' and " + TAG_TO_CITY + " = '" + to_city + "') or (" + TAG_FROM_CITY + " = '" + to_city + "' and " + TAG_TO_CITY + " = '" + from_city + "')");
        if (rs.next()) {
            conn.createStatement().execute("delete from " + TAG_DISTANCE + " where (" + TAG_FROM_CITY + " = '" + from_city + "' && " + TAG_TO_CITY + " = '" + to_city + "') || (" + TAG_FROM_CITY + " = '" + to_city + "' && " + TAG_TO_CITY + " = '" + from_city + "')");
        }
        conn.createStatement().execute( "insert into " + TAG_DISTANCE + " (" + TAG_FROM_CITY + ", " + TAG_TO_CITY + ", " + TAG_VALUE + ") values ('" + from_city + "', '" + to_city + "', " + value + ")");
    }

    private String makeHtml() throws SQLException {
        StringBuilder sb = new StringBuilder();

        sb.append("<html>\n" +
                "\t<head>\n" +
                "\t\t<meta charset=\"utf-8\" />\n" +
                "\t\t<title>Distance calculator</title>\n" +
                "\t</head>\n" +
                "\t<body>\n" +
                "\t\t<form action=\"\" method=\"post\">\n" +
                "\t\t\t<p><strong>" + PN_CALCULATION_TYPE + "</strong></p>\n" +
                "\t\t\t<p><select name=\"" + PN_CALCULATION_TYPE + "\">\n" +
                "\t\t\t\t<option value=\"" + PV_CROWFLIGHT + "\">" + PV_CROWFLIGHT + "</option>\n" +
                "\t\t\t\t<option value=\"" + PV_DISTANCE_MATRIX + "\">" + PV_DISTANCE_MATRIX + "</option>\n" +
                "\t\t\t\t<option value=\"" + PV_ALL + "\">" + PV_ALL + "</option>\n" +
                "\t\t\t</select></p>\n" +
                "\t\t\t\n" +
                "\t\t\t<p><strong>" + PN_FROM_CITY + "</strong></p>\n" +
                "\t\t\t<p><select name=\"" + PN_FROM_CITY + "\">");

        ArrayList<String> cities = selectCities();
        for (String city : cities) {
            sb.append("\t\t\t\t<option value=\"" + city + "\">" + city + "</option>");
        }

        sb.append("\t\t\t</select></p>\n" +
                "\t\t\t\n" +
                "\t\t\t<p><strong>" + PN_TO_CITY + "</strong></p>\n" +
                "\t\t\t<p><select name=\"" + PN_TO_CITY + "\">");

        for (String city : cities) {
            sb.append("\t\t\t\t<option value=\"" + city + "\">" + city + "</option>");
        }

        sb.append("\t\t\t</select></p>\n" +
                "\t\t\t<p><input type=\"submit\" value=\"Calculate\"></p>\n" +
                "\t\t</form>\n" +
                "\t\t\n" +
                "\t\t<form action=\"\" method=\"post\" enctype=\"multipart/form-data\">\n" +
                "\t\t\t<p><strong>Upload Data</strong></p>\n" +
                "\t\t\t<p><input type=\"file\" name=\"file\">\n" +
                "\t\t\t<p><button type=\"submit\">Upload</button>\n" +
                "\t\t</form>\n" +
                "\t</body>\n" +
                "</html>");

        return sb.toString();
    }

    private ArrayList<String> selectCities() throws SQLException {
        ArrayList<String> list = new ArrayList<>();

        ResultSet rs = conn.createStatement().executeQuery("select " + TAG_NAME + " from " + TAG_CITY);
        while (rs.next()) {
            list.add(rs.getString(TAG_NAME));
        }

        return list;
    }
}