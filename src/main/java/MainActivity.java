import javax.naming.InitialContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.IOException;
import java.sql.*;

/**
 * Created by merdeev on 14/9/17.
 */
@WebServlet( name = "RESTfulAppli", urlPatterns = "/" )
public class MainActivity extends HttpServlet
{
    private static Connection conn;

    @Override protected void doGet( HttpServletRequest req, HttpServletResponse resp ) throws IOException
    {
//        try {
//            InitialContext initialContext = new InitialContext();
//            DataSource ds = (DataSource) initialContext.lookup("java:/magenta/datasource/test-distance-calculator");
//            conn = ds.getConnection();
//            resp.getWriter().write( "connected\n" );
//        } catch (Exception Ex) {
//            resp.getWriter().write( "can't connect: " + Ex.getMessage() + "\n" );
//        } finally {
//            try {
//                conn.close();
//                resp.getWriter().write( "disconnected\n" );
//            } catch(Exception Ex) {
//                resp.getWriter().write( "can't close: "  + Ex.getMessage() + "\n" );
//            }
//        }


        resp.getWriter().write( "<html>\n" +
                "\t<head>\n" +
                "\t\t<meta charset=\"utf-8\" />\n" +
                "\t\t<title>Distance calculator</title>\n" +
                "\t</head>\n" +
                "\t<body>\n" +
                "\t\t<form action=\"\" method=\"post\">\n" +
                "\t\t\t<p><strong>Calculation Type</strong></p>\n" +
                "\t\t\t<p><select name=\"Calculation Type\">\n" +
                "\t\t\t\t<option value=\"Crowflight\">Crowflight</option>\n" +
                "\t\t\t\t<option value=\"Distance Matrix\">Distance Matrix</option>\n" +
                "\t\t\t\t<option value=\"All\">All</option>\n" +
                "\t\t\t</select></p>\n" +
                "\t\t\t\n" +
                "\t\t\t<p><strong>From City</strong></p>\n" +
                "\t\t\t<p><select name=\"From City\">\n" +
                "\t\t\t\t<option value=\"Togliatti\">Togliatti</option>\n" +
                "\t\t\t\t<option value=\"Moscow\">Moscow</option>\n" +
                "\t\t\t\t<option value=\"New York\">New York</option>\n" +
                "\t\t\t</select></p>\n" +
                "\t\t\t\n" +
                "\t\t\t<p><strong>To City</strong></p>\n" +
                "\t\t\t<p><select name=\"To City\">\n" +
                "\t\t\t\t<option value=\"Togliatti\">Togliatti</option>\n" +
                "\t\t\t\t<option value=\"Moscow\">Moscow</option>\n" +
                "\t\t\t\t<option value=\"New York\">New York</option>\n" +
                "\t\t\t</select></p>\n" +
                "\t\t\t<p><input type=\"submit\" value=\"Calculate\"></p>\n" +
                "\t\t</form>\n" +
                "\t\t\n" +
                "\t\t<form action=\"\" method=\"post\" enctype=\"multipart/form-data\">\n" +
                "\t\t\t<p><strong>Upload Data</strong></p>\n" +
                "\t\t\t<p><input type=\"file\" name=\"file\">\n" +
                "\t\t\t<p><button type=\"submit\">Upload</button>\n" +
                "\t\t</form>\n" +
                "\t</body>\n" +
                "</html>" );

    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        BufferedReader r = req.getReader();
        while (r.ready()) {
            resp.getWriter().write(r.readLine());
        }
    }
}