import javax.naming.InitialContext;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
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
        try {
            InitialContext initialContext = new InitialContext();
            DataSource ds = (DataSource) initialContext.lookup("java:/magenta/datasource/test-distance-calculator");
            conn = ds.getConnection();
            resp.getWriter().write( "connected\n" );
        } catch (Exception Ex) {
            resp.getWriter().write( "can't connect: " + Ex.getMessage() + "\n" );
        } finally {
            try {
                conn.close();
                resp.getWriter().write( "disconnected\n" );
            } catch(Exception Ex) {
                resp.getWriter().write( "can't close: "  + Ex.getMessage() + "\n" );
            }
        }
    }
}