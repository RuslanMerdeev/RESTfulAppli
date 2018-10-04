import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

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
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.*;

/**
 * Created by merdeev on 14/9/17.
 */
@WebServlet( name = "RESTfulAppli", urlPatterns = {"/", "/upload"} )
@MultipartConfig(fileSizeThreshold = 1024*1024+60, // 60 MB
        maxFileSize = 1024*1024*120, // 120 MB
        maxRequestSize = 1024*1024*200 // 200 MB
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
    private static final String TAG_ITEM = "item";

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
                    resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    return;
                }

                String uploadFilePath = uploadFolderPath + File.separator + fileName;
                File uploadFile = new File(uploadFilePath);
                if (uploadFile.exists()) {
                    if (!uploadFile.delete()) {
                        throw new IOException();
                    }
                }
                part.write(uploadFilePath);

                parseFile(uploadFile, resp);
            }
        }

        resp.setStatus(HttpServletResponse.SC_OK);
    }

    private void parseFile (File file, HttpServletResponse resp) throws IOException, ParserConfigurationException, SAXException, SQLException {
        StringBuilder cities_sb = new StringBuilder();
        StringBuilder distances_sb = new StringBuilder();

        SAXParserFactory factory = SAXParserFactory.newInstance();
        SAXParser saxParser = factory.newSAXParser();

        DefaultHandler handler = new DefaultHandler() {

            boolean city = false;
            boolean distance = false;
            String first = null;
            String second = null;
            String third = null;

            public void startElement(String uri, String localName,String qName,
                                     Attributes attributes) throws SAXException {

                if (qName.equals(TAG_CITY)) {
                    city = true;
                    distance = false;
                    first = null;
                    second = null;
                    third = null;
                }

                if (qName.equals(TAG_DISTANCE)) {
                    distance = true;
                    city = false;
                    first = null;
                    second = null;
                    third = null;
                }

                if (qName.equals(TAG_NAME) && city && first == null) {
                    first = "";
                }

                if (qName.equals(TAG_LATITUDE) && city && second == null) {
                    second = "";
                }

                if (qName.equals(TAG_LONGITUDE) && city && third == null) {
                    third = "";
                }

                if (qName.equals(TAG_FROM_CITY) && distance && first == null) {
                    first = "";
                }

                if (qName.equals(TAG_TO_CITY) && distance && second == null) {
                    second = "";
                }

                if (qName.equals(TAG_VALUE) && distance && third == null) {
                    third = "";
                }
            }

            public void endElement(String uri, String localName,
                                   String qName) throws SAXException {

                if (qName.equals(TAG_CITY)) {
                    city = false;
                    if (first != null && second != null && third != null)
                        cities_sb.append(" ('").append(first).append("', ").append(second).append(", ").append(third).append("),");
                }

                if (qName.equals(TAG_DISTANCE)) {
                    distance = false;
                    if (first != null && second != null && third != null)
                        distances_sb.append(" ('").append(first).append("', '").append(second).append("', ").append(third).append("),");
                }
            }

            public void characters(char ch[], int start, int length) throws SAXException {

                if (city || distance) {
                    if (first != null && first.equals("")) {
                        first = new String(ch, start, length);
                    }
                    if (second != null && second.equals("")) {
                        second = new String(ch, start, length);
                    }
                    if (third != null && third.equals("")) {
                        third = new String(ch, start, length);
                    }
                }
            }
        };

        saxParser.parse(file, handler);
//        resp.getWriter().write(distances_sb.toString());



        if (cities_sb.length() > 0) {
            cities_sb.deleteCharAt(cities_sb.length()-1);
            insertCities(cities_sb.toString());
        }

        if (distances_sb.length() > 0) {
            distances_sb.deleteCharAt(distances_sb.length()-1);
            insertDistances(distances_sb.toString());
        }
    }

    private void insertCities (String values) throws SQLException {
        conn.createStatement().execute( "insert into " + TAG_CITY + " (" + TAG_NAME + ", " + TAG_LATITUDE + ", " + TAG_LONGITUDE + ") values" + values);
    }

    private void insertDistances (String values) throws SQLException {
        conn.createStatement().execute( "insert into " + TAG_DISTANCE + " (" + TAG_FROM_CITY + ", " + TAG_TO_CITY + ", " + TAG_VALUE + ") values" + values);
    }

    private String makeHtml() throws SQLException {
        StringBuilder sb = new StringBuilder();

        createCity();
        createDistance();

        HashMap<Integer,String> cities = selectCities();

        sb.append("<html>\n" +
                "\t<head>\n" +
                "\t\t<meta charset=\"utf-8\" />\n" +
                "\t\t<title>Distance calculator</title>\n" +
                "\t</head>\n" +
                "\t<body>\n" +
                "\t\t<form action=\"\" method=\"post\">\n" +
                "\t\t\t<fieldset>\n" +
                "\t\t\t\t<legend><strong>Available Cities</strong></legend>\n" +
                "\t\t\t\t<p><input list=\"cities\"/>\n" +
                "\t\t\t\t<datalist id=\"cities\"/>\n");

        for (Map.Entry<Integer,String> city : cities.entrySet()) {
            sb.append("\t\t\t\t\t<option value=\"").append(city.getValue()).append("\">").append(city.getKey()).append("</option>\n");
        }

        sb.append("\t\t\t\t</datalist></p>\n" +
                "\t\t\t</fieldset>\n" +
                "\t\t</form>\n" +
                "\t\t\t\n" +
                "\t\t<form action=\"\" method=\"post\">\n" +
                "\t\t\t<fieldset>\n" +
                "\t\t\t\t<legend><strong>Calculation Distances</strong></legend>\n" +
                "\t\t\t\t<p>Calculation Type: <select name=\"" + PN_CALCULATION_TYPE + "\">\n" +
                "\t\t\t\t\t<option value=\"" + PV_CROWFLIGHT + "\" selected=\"selected\">" + PV_CROWFLIGHT + "</option>\n" +
                "\t\t\t\t\t<option value=\"" + PV_DISTANCE_MATRIX + "\">" + PV_DISTANCE_MATRIX + "</option>\n" +
                "\t\t\t\t\t<option value=\"" + PV_ALL + "\">" + PV_ALL + "</option>\n" +
                "\t\t\t\t</select></p>\n" +
                "\t\t\t\t\n" +
                "\t\t\t\t<p>" + PN_FROM_CITY + ": <input list=\"" + PN_FROM_CITY + "\" name=\"" + PN_FROM_CITY + "\" required=\"required\"/>\n" +
                "\t\t\t\t<datalist id=\"" + PN_FROM_CITY + "\"/>\n");

        for (Map.Entry<Integer,String> city : cities.entrySet()) {
            sb.append("\t\t\t\t\t<option value=\"").append(city.getValue()).append("\">").append(city.getValue()).append("</option>\n");
        }

        sb.append("\t\t\t\t</datalist></p>\n" +
                "\t\t\t\t\n" +
                "\t\t\t\t<p>" + PN_TO_CITY + ": <input list=\"" + PN_TO_CITY + "\" name=\"" + PN_TO_CITY + "\" required=\"required\"/>\n" +
                "\t\t\t\t<datalist id=\"" + PN_TO_CITY + "\"/>\n");

        for (Map.Entry<Integer,String> city : cities.entrySet()) {
            sb.append("\t\t\t\t\t<option value=\"").append(city.getValue()).append("\">").append(city.getValue()).append("</option>\n");
        }

        sb.append("\t\t\t\t</datalist></p>\n" +
                "\t\t\t\t<p><input type=\"submit\" value=\"Calculate\"></p>\n" +
                "\t\t\t</fieldset>\n" +
                "\t\t</form>\n" +
                "\t\t\n" +
                "\t\t<form action=\"\" method=\"post\" enctype=\"multipart/form-data\">\n" +
                "\t\t\t<fieldset>\n" +
                "\t\t\t\t<legend><strong>Upload Data</strong></legend>\n" +
                "\t\t\t\t<p><input type=\"file\" name=\"file\" required=\"required\">\n" +
                "\t\t\t\t<p><button type=\"submit\">Upload</button>\n" +
                "\t\t\t</fieldset>\n" +
                "\t\t</form>\n" +
                "\t</body>\n" +
                "</html>");

        return sb.toString();
    }

    private void createCity () {
        try {
            conn.createStatement().execute( "create table " + TAG_CITY + " (\n" +
                    "id int not null auto_increment primary key,\n" +
                    TAG_NAME + " varchar(50) not null,\n" +
                    TAG_LATITUDE + " float(8,4) not null,\n" +
                    TAG_LONGITUDE + " float(9,4) not null\n" +
                    ");");
        }
        catch (SQLException ignored) {
        }
    }

    private void createDistance () {
        try {
            conn.createStatement().execute( "create table " + TAG_DISTANCE + " (\n" +
                    "id int not null auto_increment primary key,\n" +
                    TAG_FROM_CITY + " varchar(50) not null,\n" +
                    TAG_TO_CITY + " varchar(50) not null,\n" +
                    TAG_VALUE + " float(7,1) not null\n" +
                    ");");
        }
        catch (SQLException ignored) {
        }
    }

    private HashMap<Integer,String> selectCities() throws SQLException {
        HashMap<Integer,String> map = new HashMap<>();

        ResultSet rs = conn.createStatement().executeQuery("select id, " + TAG_NAME + " from " + TAG_CITY);
        while (rs.next()) {
            map.put(rs.getInt("id"), rs.getString(TAG_NAME));
        }

        return map;
    }
}