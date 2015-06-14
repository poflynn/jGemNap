

import java.io.*;

import javax.servlet.*;
import javax.servlet.http.*;

import Gemini;

public class servMsgs extends HttpServlet
{
  Gemini t;
  boolean result=false,twasnull=false,action=false, openZones=false, proZones=false;
  int zones;
  String actionDone = new String();

    public void doGet (HttpServletRequest req,
                       HttpServletResponse res) {
        twasnull=false;

        if (t==null) {
          twasnull=true;
          t = new Gemini("COM1");
          t.debug=false;
          t.debug=true;
          t.passCode="480929";
        }

      PrintPage(req,res);
    }


    public void doPost (HttpServletRequest req,
                       HttpServletResponse res) {

        twasnull=false;
        action=false;

        openZones=false;
        proZones=false;

        String cmd = req.getParameter("cmd");

        if (cmd.equals("arm")) {
          action=true;
          actionDone = " (arm) ";
          result = t.Connect();
          if (result)
            result = t.Arm();
          t.Disconnect();
        }

        if(cmd.equals("disarm")) {
          action=true;
          actionDone = " (disarm) ";
          result = t.Connect();
          if (result)
            result = t.Disarm();
          t.Disconnect();
        }

        if(cmd.equals("getopen")) {
          action=true;
          actionDone = " (open zones) ";
          openZones=true;
          result = t.Connect();
          if (result)
            zones = t.GetStatus(2);
            if (zones==-1)
              result=false;
            else
              result=true;
          t.Disconnect();
        }

        if(cmd.equals("getpro")) {
          action=true;
          actionDone = " (protected zones) ";
          proZones=true;
          result = t.Connect();
          if (result)
            zones = t.GetStatus(3);
            if (zones==-1)
              result=false;
            else
              result=true;
          t.Disconnect();
        }

        //Redirect back here but without POST variable.
        try {
          res.sendRedirect(req.getRequestURI());
        }  catch (Exception e) {}
     }


     void PrintPage(HttpServletRequest req, HttpServletResponse res) {
        PrintWriter out;
        String state = new String();
        final String url=req.getScheme() + "://" + req.getServerName() + ":" + req.getServerPort() + req.getRequestURI();
        boolean htmlBrowser;
        try
        {
            out = res.getWriter();
            if (req.getHeader("USER-AGENT").indexOf("Mozilla") != -1)
              htmlBrowser=true;
            else
              htmlBrowser=false;

            // set content type and other response header fields first
            res.setContentType(htmlBrowser? "text/html":"text/vnd.wap.wml");
            res.setHeader("Cache-Control", "no-cache, must-revalidate");
//            res.setHeader("Expires", "Mon, 26 Jul 1997 05:00:00 GM");
//            res.setHeader("pragma", "no-cache");

            if (htmlBrowser) {
              out.print("<HTML><HEAD>");
//              out.print("<META HTTP-EQUIV=\"refresh\" content=\"4;URL=" + url + "\">");

              out.print("<HTML><HEAD><TITLE>My Gemini</TITLE></HEAD>");
              out.print("<BODY><H3>Welcome</H3>");
              if (twasnull)
                out.print("t was null btw<br><br>");

            } else {
              out.print("<?xml version=\"1.0\"?>\n");
              out.print("<!DOCTYPE wml PUBLIC \"-//WAPFORUM//DTD WML 1.1//EN\" \"http://www.wapforum.org/DTD/wml_1.1.xml\">\n");
              out.print("<wml><head><meta forua=\"true\" http-equiv=\"Cache-Control\" content=\"max-age=0\"/></head>\n");
              out.print("<template ontimer='" + url + "'/>\n");
//              out.print("<card id='main' title='Main' ontimer='" + url + "'>\n");
  //            out.print("<timer value='160'/>\n");
//              out.print("<do type='accept' label='MENU'><go href='" + req.getRequestURI() + "/#menu\n");
              out.print("<do type='accept' label='RFRSH'><go href='" + url + "'> </go></do>\n");
              out.print("<do type='options' label='Arm'><go href='" + url + "' method='post'> <postfield name='cmd' value='arm'/></go></do>\n");
              out.print("<do type='options' label='Disarm'><go href='" + url + "' method='post'> <postfield name='cmd' value='disarm'/></go></do>\n");
               out.print("<do type='options' label='Get Open Zones'><go href='" + url + "' method='post'> <postfield name='cmd' value='getopen'/></go></do>\n");
              out.print("<do type='options' label='Get Protected Zones'><go href='" + url + "' method='post'> <postfield name='cmd' value='getpro'/></go></do>\n");
              out.print("<p mode='nowrap'>\n");
            }


            if (action && htmlBrowser) {
              out.print("Last action" + actionDone + "was ");
              if (result)
                out.print("<b>successful</b>");
              else
                out.print("<b>unsuccessful</b>");
                out.print(htmlBrowser?"<br><br>" : "<br/>\n");
            }



            if ((openZones || proZones) & result) {
              result = false;  //Do not display these msgs next time 'round.
              if (openZones) {
                if (htmlBrowser) {
                  out.print("<h2>Open Zones:</h2>");
                  state=" open.<br>";
                } else {
                  out.print("Open Zones:\n");
                  state=" open.<br/>\n";
                }
              }
              if (proZones) {
                if (htmlBrowser) {
                  out.print("<h2>Protected Zones:</h2>");
                  state=" protected.<br>";
                } else {
                  out.print("Protected Zones:\n");
                  state=" protected.<br/>\n";
                }
              }

              if (zones==0)
                out.print("No zones are" + state);
              else {
                for (int i=0; i<8; i++)
                  out.print("Zone " + (i+1) + " is " + (((zones & (int) Math.pow(2,i)) == (int) Math.pow(2,i)) ? "" : " not ") + state);
              }
            }

            out.println(htmlBrowser?"<h2><hr>" + t.keyPadMsg1 + "<br>" + t.keyPadMsg2 + "<hr>":
                                    "<b>" + t.keyPadMsg1 + "<br/>" + t.keyPadMsg2  + "</b></p></card></wml>\n");

            if (htmlBrowser) {
              out.print("<FORM METHOD=POST>");
              out.print("<INPUT TYPE=SUBMIT NAME=cmd VALUE=refresh>");
              out.print("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;");
              out.print("<INPUT TYPE=SUBMIT NAME=cmd VALUE=arm>");
              out.print("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;");
              out.print("<INPUT TYPE=SUBMIT NAME=cmd VALUE=disarm>");
              out.print("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;");
              out.print("<INPUT TYPE=SUBMIT NAME=cmd VALUE=getopen>");
              out.print("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;");
              out.print("<INPUT TYPE=SUBMIT NAME=cmd VALUE=getpro>");
              out.print("</FORM></BODY></HTML>");
            } else {
/*              out.print("<card id='menu'>\n");
              out.print("
              out.print("
              out.print("*/
            }
            out.close();
        }  catch (Exception e) {}
    }

}

