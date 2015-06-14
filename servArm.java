

import java.io.*;

import javax.servlet.*;
import javax.servlet.http.*;

import Gemini;

/**
 * This is a simple example of an HTTP Servlet.  It responds to the GET
 * and HEAD methods of the HTTP protocol.
 */
public class servArm extends HttpServlet
{
 Gemini t;

    public void doGet (HttpServletRequest request,
                       HttpServletResponse response) {

        boolean twasnull=false;

        if (t==null) {
          twasnull=true;
          t = new Gemini("COM1");
          t.debug=false;
          t.passCode="123456";
        }
        boolean result=false;

        result = t.Connect();
        if (result)
          result = t.Arm();
        t.Disconnect();

        t.Wait (2);

        try
        {
            PrintWriter out;
            String title = "Example Apache JServ Servlet";

            // set content type and other response header fields first
            response.setContentType("text/html");

            // then write the data of the response
            out = response.getWriter();

            out.println("<HTML><HEAD><TITLE>");
            out.println(title);
            out.println("</TITLE></HEAD><BODY bgcolor=\"#FFFFFF\">");
            out.println("<H1>" + title + "</H1>");
            if (!twasnull)
              out.println("<h2>t was null<br>");
            if (result)
              out.println("<h2>Armed baby<br>" + t.keyPadMsg1);
            else
              out.println("<H2>Crap, arm failed<br>" + t.keyPadMsg1);

            out.println("</BODY></HTML>");
            out.close();
        }  catch (Exception e) {}
    }
}

