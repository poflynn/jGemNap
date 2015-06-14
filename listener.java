import java.io.*;
import java.util.*;
import javax.comm.*;

public class listener implements Runnable, SerialPortEventListener {
//public class listener {

    public static String keyPadMsgs;
    public static boolean listening;
    static CommPortIdentifier portId;
    static Enumeration portList;
/*    static OutputStream outputStream;
    static InputStream inputStream;
    static SerialPort serialPort;    */
    static OutputStream outputStream;
    static InputStream inputStream;
    static SerialPort serialPort;
    Thread readThread=null;
    static PrintWriter outWml;
    static PrintWriter outHtml;
    static int SndCkSum;
    static int SndKey;
    static int SndKeyOff;
    static int CkSum;
    static int Key;
    static int keyoff;
    static int i;


    public static void main(String[] args) {
//        listener test = new listener();
        listening=false;
        portList = CommPortIdentifier.getPortIdentifiers();

        while (portList.hasMoreElements()) {
            portId = (CommPortIdentifier) portList.nextElement();
            if (portId.getPortType() == CommPortIdentifier.PORT_SERIAL) {
                if (portId.getName().equals("COM1")) {
                //if (portId.getName().equals("/dev/term/a")) {
//                    listener reader = new listener();
        try {
            serialPort = (SerialPort) portId.open("SimpleReadApp", 2000);
        } catch (PortInUseException e) {}

        try {
            outputStream = serialPort.getOutputStream();
        } catch (IOException e) {}
        try {
            inputStream = serialPort.getInputStream();
        } catch (IOException e) {}
        try {
            serialPort.setSerialPortParams(9600,
                       SerialPort.DATABITS_8,
                       SerialPort.STOPBITS_1,
                       SerialPort.PARITY_EVEN);
        } catch (UnsupportedCommOperationException e) {}

        while (serialPort.isCTS() == false) {
            System.out.print("CTS false, looping\n");
        }

        listener reader = new listener();

                }
            }
        }
    }




/************ Thread ***********/

    public listener() {
      if (!listening) {
          System.out.println("Starting thread");
          try {
              serialPort.addEventListener(this);
          } catch (TooManyListenersException e) {}
          serialPort.notifyOnDataAvailable(true);
          readThread = new Thread(this);
          readThread.start();
          listening=true;
      }
      else
          System.out.println("Not starting thread");
    }

    public void run() {
        try {
            Thread.sleep(20000);
        } catch (InterruptedException e) {}
    }

    public void serialEvent(SerialPortEvent event) {
        switch(event.getEventType()) {
        case SerialPortEvent.BI:
        case SerialPortEvent.OE:
        case SerialPortEvent.FE:
        case SerialPortEvent.PE:
        case SerialPortEvent.CD:
        case SerialPortEvent.CTS:
        case SerialPortEvent.DSR:
        case SerialPortEvent.RI:
        case SerialPortEvent.OUTPUT_BUFFER_EMPTY:
            break;
        case SerialPortEvent.DATA_AVAILABLE:
            byte[] readByte = new byte[1];
            byte[] readBuffer = new byte[34];
            byte[] keyPad = new byte[32];
            byte checkSum;
            byte counter=0;
            byte i=0;
            boolean notDone=true;
            boolean panel;
            boolean status;

            try {
                panel=false;
                status=false;
                int numBytes = inputStream.read(readByte);

                  while (notDone && (inputStream.available() > 0)) {
                    if (readByte[0] == 3) {
                      inputStream.read(readByte);
                      if (readByte[0] == 0x25) {
                        notDone=false;
                        panel=true;
                      }
                    }

                    if (readByte[0] == 1) {
                      inputStream.read(readByte);
                      if (readByte[0] == 6) {
                        notDone=false;
                        status=true;
                      }
                    }
                    if (notDone) {
                      // Dump unrecognized data.
                      System.out.print(" Dumping " + Integer.toHexString((int) readByte[0] & 0xFF) );
                      inputStream.read(readByte);
                    }
                  }


                if (notDone)
                  System.out.println("\nRan out of data??");

                if (status) {
                  // Real time status message
                  i=0;
                  while (inputStream.available() < 4) {
                    try {
                      Thread.sleep(10);
                    } catch (InterruptedException e) {}
                    counter++;
                    if (counter>100)
                      break;
                  }
                  if (counter<100) {
                    while (i < 4) {
                      inputStream.read(readByte);
                      readBuffer[i++]=readByte[0];
                    }
                    System.out.println("\nYeah!! Got a status msg!");
                    System.out.println(Integer.toHexString((int) readBuffer[0] & 0xFF) + " " + Integer.toHexString((int) readBuffer[1] & 0xFF) + " " + Integer.toHexString((int) readBuffer[2] & 0xFF) + " " + Integer.toHexString((int) readBuffer[3] & 0xFF));
                    System.out.println("Yeah!! Finished status msg!\n");
                  }
                  else
                    System.out.println("Timeout in status!");
                }


                if (panel) {
                  // Change in panel display
                  numBytes = inputStream.read(readByte); // Dump these two
                  numBytes = inputStream.read(readByte); // Dump these two
                  i=0;
                  while (inputStream.available() < keyPad.length) {
                    try {
                      Thread.sleep(10);
                    } catch (InterruptedException e) {}
                    counter++;
                    if (counter>100)
                      break;
                  }
                  if (inputStream.available() >= keyPad.length + 1) {
                    numBytes = inputStream.read(keyPad);
                    inputStream.read(readByte);
                    checkSum=readByte[0];
                  }

                  if (counter<100) {
                    keyPadMsgs = new String(keyPad);
                    String firstLine = keyPadMsgs.substring(0,16);
                    String secondLine = keyPadMsgs.substring(16);

/*                    outWml = new PrintWriter(
                                    new OutputStreamWriter(
                                        new FileOutputStream("C:\\Program Files\\Apache Group\\Apache\\htdocs\\a.wml")));
                    outHtml = new PrintWriter(
                                    new OutputStreamWriter(
                                        new FileOutputStream("C:\\Program Files\\Apache Group\\Apache\\htdocs\\a.html")));
                    outHtml.write(firstLine + "<br>" + secondLine);
                    outWml.write("<?xml version=\"1.0\"?>");
                    outWml.write("<!DOCTYPE wml PUBLIC \"-//PHONE.COM//DTD WML 1.1//EN\"");
                    outWml.write("\"http://www.phone.com/dtd/wml11.dtd\"><wml>");
                    outWml.write("<head>");
                    outWml.write("<meta http-equiv=\"Cache_Control\" content=\"max-age=0\"/>");
                    outWml.write("</head>");
                    outWml.write("<card ontimer='a.wml'><timer value='30'/><p>");
                    outWml.write(firstLine + "<br/>" + secondLine);
                    outWml.write("</p></card></wml>");
                    outWml.close();
                    outHtml.close();
                    System.out.print(firstLine + "\n" + secondLine);
                    System.out.println("\n****************");*/
                  }
                }
              } catch (IOException e) {}
              break;
        }

    }
}

// System.out.print (Integer.toHexString((int) readBuffer[i] & 0xFF) + "->" + (char) readBuffer[i] + " ");
