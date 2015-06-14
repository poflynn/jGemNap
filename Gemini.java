import java.io.*;
import java.util.*;
import javax.comm.*;

public class Gemini implements Runnable, SerialPortEventListener {
//class Gemini {
    Thread listenThread;
//    private static CommPortIdentifier portId;

    private static OutputStream outputStream;
    private static InputStream inputStream;
    private static SerialPort serialPort;
    private static byte [] RcvBuf;// = new byte[200];
    private static int SndCkSum;
    private static int SndKey;
    private static int SndKeyOff;
    private static int CkSum;
    private static int Key;
    private static int keyoff;
    private static int i;


    public static String keyPadMsg1="", keyPadMsg2="";

    public static boolean debug=true;
    public static boolean connected=false;
    public static String passCode;

    public static void main(String[] args) {

      passCode="123456";
      if (Connect()) {
//        Rcv();

//          GetLog();
//        GetStatus(2); //open zones
//        GetStatus(3); //protected zones
//        System.out.println("And the version is:" + GetPanelType());
//        if (!Arm())
//          System.out.println("Arm failed!");
  //      if (!DisArm())
    //      System.out.println("Disarm failed!");

//        GetZoneDescrs();
//        Rcv();
        Disconnect();
      }
      else
        System.out.println("Failed to connect to gemini!!");
    }


    public static boolean Connect() {
      if ((passCode.length() != 6) || (serialPort==null))
        return (false);
      else {


                  if (debug)
                    System.out.println ("\nAbout to send security cmd.\n");

                  if (serialPort.isCTS() == false) {
                    i=0;
                    if (debug)
                      System.out.println("CTS false, waiting in connect");
                    while (serialPort.isCTS() == false)
                      i++;
                    if (debug)
                      System.out.println("CTS took "+ i);
                  }

                  SendByte(0x86);

                  if (serialPort.isCTS() == true) {
                    i=0;
                    if (debug)
                      System.out.println("CTS true, waiting in connect");
                    while (serialPort.isCTS() == true)
                      i++;
                    if (debug)
                      System.out.println("CTS took "+ i);
                  }

                  serialPort.setRTS(true);

                  byte[] num= new byte[6];

                  if (passCode.length() != 6)
                    return (false);

                  SndKeyOff=0x05;
                  SendByte(SndKeyOff);
                  SndKey = (SndKeyOff  + 0x35) & 0xFF;
                  SndCkSum = (SndKeyOff + 1) & 0xFF;


                  SndChr(0x1A);
                  SndChr(0x06);
                  SndChr(0x00);
                  SndChr(0x00);
                  SndChr(0x00);
                  SndChr(0x00);
                  num=passCode.getBytes();
                  for (int i=0;i<6;i++) {
                    if (num[i]==48)
                      SndChr(10);
                    else
                      SndChr((int) num[i] - 48);  //-48 to convert "0" ascii (48) to numeric 0
                  }
                  SndChr(SndCkSum);

                  if (debug)
                    System.out.println ("\nDone sending security cmd.\n");

                  boolean result = Rcv() && (RcvBuf[4] == 0);
                  connected=result;
                  return (result);
      }
    }


    public static void Disconnect() {
      serialPort.setRTS(false);

      if (debug)
        System.out.print("Waiting for panel to finish\n");

      i=0;
      while (serialPort.isCTS() == false)
         i++;

      serialPort.close();

      if (debug)
        System.out.println("Panel finished in " + i);
    }

    public static boolean Disarm() {
      if (debug)
        System.out.println ("\nAbout to DisArm.\n");
      SndKeyOff=0x86;
      SendByte(SndKeyOff);
      SndKey = (SndKeyOff  + 0x35) & 0xFF;
      SndCkSum = (SndKeyOff + 1) & 0xFF;


      SndChr(0x19);
      SndChr(0x00);
      SndChr(0x03);
      SndChr(0x00);
      SndChr(0x01);
      SndChr(0x80);
      SndChr(SndCkSum);
      boolean Result=Rcv();
      if (debug)
        System.out.println ("\nDone DisArming.\n");
      return (Result);
    }

    public static boolean Arm() {
        if (debug)
          System.out.println ("\nAbout to Arm.\n");
        SndKeyOff=0x86;
        SendByte(SndKeyOff);
        SndKey = (SndKeyOff  + 0x35) & 0xFF;
        SndCkSum = (SndKeyOff + 1) & 0xFF;


        SndChr(0x19);
        SndChr(0x00);
        SndChr(0x01);
        SndChr(0x00);
        SndChr(0x01);
        SndChr(0x80);
        SndChr(SndCkSum);
        boolean Result = Rcv();
        if (debug)
          System.out.println ("\nDone Arming.\n");
        return (Result);
      }

    public static void GetLog() {
      if (debug)
        System.out.println ("\nAbout to get log.\n");
      SndKeyOff=0x86;
      SendByte(SndKeyOff);
      SndKey = (SndKeyOff  + 0x35) & 0xFF;
      SndCkSum = (SndKeyOff + 1) & 0xFF;


      SndChr(0x32);
      SndChr(0x04);
      SndChr(0x1f);
      SndChr(0xfc);
      SndChr(0x00);
      SndChr(0x00);
      SndChr(SndCkSum);

      Rcv(); // Returns # of log entries, we'll ignore for now

      SndChr(0x32);
      SndChr(0x80);
      SndChr(0x03);
      SndChr(0x02);
      SndChr(0x00);
      SndChr(0x00);
      SndChr(SndCkSum);
      if (debug)
        System.out.println ("\nDone getting log.\n");
    }

    public static byte GetPanelType() {
      if (debug)
        System.out.println ("\nAbout to send version check.\n");
      SndKeyOff=0x86;
      SendByte(SndKeyOff);
      SndKey = (SndKeyOff  + 0x35) & 0xFF;
      SndCkSum = (SndKeyOff + 1) & 0xFF;

      SndChr(0x1C);
      SndChr(0x00);
      SndChr(0x00);
      SndChr(0x00);
      SndChr(0x00);
      SndChr(0x00);
      SndChr(SndCkSum);
      if (debug)
        System.out.println ("\nDone sending version check.\n");
      if (!Rcv())
        return(0);
      else
        return(RcvBuf[5]);
    }

    public static void GetZoneDescrs() {
      if (debug)
        System.out.println ("\nAbout to zone descriptions.\n");
      SndKeyOff=0x86;
      SendByte(SndKeyOff);
      SndKey = (SndKeyOff  + 0x35) & 0xFF;
      SndCkSum = (SndKeyOff + 1) & 0xFF;

      SndChr(0x05);
      SndChr(0x00);
      SndChr(0x00);
      SndChr(0x00);
      SndChr(0x00);
      SndChr(0x00);
      SndChr(SndCkSum);
      if (debug)
        System.out.println ("\nDone sending zone cmd.\n");
    }


    public static void GetStatus(int status) {
      /* Status values as follows:
       0 = time in seconds since 1/1/70
       1 = shorted zones
       2 = open zones
       3 = protected zones
       4 = manually bypassed zones
       5 = remotely bypassed zones
       6 = RF open zones
       7 = RF shorted zones
      */

      if (debug)
        System.out.println ("\nAbout to get status.\n");
      SndKeyOff=0x86;
      SendByte(SndKeyOff);
      SndKey = (SndKeyOff  + 0x35) & 0xFF;
      SndCkSum = (SndKeyOff + 1) & 0xFF;


      SndChr(0x39);
      SndChr(0x00);
      SndChr(0x00);
      SndChr((byte) status);
      SndChr(0x00);
      SndChr(0x00);
      SndChr(SndCkSum);
      if (debug)
        System.out.println ("\nDone getting status.\n");
    }



/*  Send and receive a byte at a time */

    private static void SendByte(int info) {
      try {
          outputStream.write((byte) (info) & 0xFF);
    //     System.out.print ("Raw sent: " + Integer.toHexString((byte) (info) & 0xFF));
      } catch (IOException e) {}
    }

    private static void SndChr(int TmpByte) {
      int Send;
      SndCkSum = (SndCkSum + TmpByte) & 0xFF;
      Send= TmpByte ^ SndKey;
      SendByte(Send);
      if (debug)
        System.out.println ("  Byte to be sent=" + Integer.toHexString(TmpByte) + " ( " + TmpByte + "), SndKey="+Integer.toHexString(SndKey)  + " Actual byte sent = " + Integer.toHexString(Send));
      SndKey = (SndKey + SndKeyOff) & 0xFF;
    }



/*  Receive response */

    private static boolean Rcv() {
      byte cmdByte, countByte;
      int rcvcksum;

      if (serialPort.isCTS() == true) {
        i=0;
        System.out.println("CTS true, waiting in rcv??");
        while (serialPort.isCTS() == true)
          i++;
        System.out.println("Done waiting in rcv "+i);
      }

      CkSum=1;
      keyoff= ReadByte();
      CkSum=(CkSum+keyoff) & 0xFF;
      Key=(keyoff + 0x35) & 0xFF;

      cmdByte = ReadAndChecksum();
      countByte= ReadAndChecksum();

      RcvBuf = new byte[countByte + 6];
      RcvBuf[0] = cmdByte;
      RcvBuf[1] = countByte;

      if (debug)
        System.out.println ("\nEntering for loop...");

      for (i=2; i<=RcvBuf[1]+5; i++)
        RcvBuf[i] = ReadAndChecksum();

      if (debug)
        System.out.println ("Leaving for loop...\n");

      i = ReadByte();
      rcvcksum = (int) (i ^ Key);

      if (debug)
        System.out.println ("Checksum check is " + (boolean) (rcvcksum==CkSum) + "\n");

      return ((cmdByte!=0x3F) && (rcvcksum==CkSum));
    }

    private static int ReadByte() {
      byte [] readBuffer = new byte[1];
      try {
          int numBytes = inputStream.read(readBuffer);
//          System.out.print ("Numbytes="+numBytes+" buffer=" + readBuffer[0]);
//         System.out.println ("Raw rcv: " + readBuffer[0]);
      }catch (IOException e) {}
      return((int) readBuffer[0] & 0xFF);  //Remove sign bit
    }


    private static byte ReadAndChecksum() {
      byte Info = (byte) (ReadByte() ^ Key);
      if (debug)
        System.out.println ("Byte & checksum read=" + Integer.toHexString(Info & 0xFF) + " char=" + (char) Info);
      CkSum = (CkSum + Info) & 0xFF;
      Key = (Key + keyoff) & 0xFF;
      return (Info);
    }

    public void Wait(int delay) {
      try {
        Thread.currentThread().sleep(delay * 1000);
        Thread.currentThread().yield();
      } catch(InterruptedException e) {}
    }

    public Gemini() {}

/************ Thread ***********/

    public Gemini(String portName) {
      boolean portFound=true;
      CommPortIdentifier portId=null;

      try {
        portId=CommPortIdentifier.getPortIdentifier(portName);
      } catch (NoSuchPortException e) {
        System.out.println("Error! No such port '" + portName + "' found.");
        keyPadMsg1="Error! No such port '" + portName + "' found.";
        portFound=false;
      }

      if (portFound) {
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

/*        try {
            serialPort.addEventListener(this);
        } catch (TooManyListenersException e) {}
        serialPort.notifyOnDataAvailable(true);
        listenThread = new Thread(this);
        listenThread.start();  */
      }
   }

    public void run() {
        try {
  //          while (true)
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


            if (false) {
              panel=false;
              status=false;

              try {
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
                    if (debug) {
                      System.out.println("Yeah!! Got a status msg!");
//                      System.out.println(Integer.toHexString((int) readBuffer[0] & 0xFF) + " " + Integer.toHexString((int) readBuffer[1] & 0xFF) + " " + Integer.toHexString((int) readBuffer[2] & 0xFF) + " " + Integer.toHexString((int) readBuffer[3] & 0xFF));
  //                    System.out.println("Yeah!! Finished status msg!\n");
                    }
                  }
                  else
                    System.out.println("Timeout in status!");
                }


                if (panel) {
                  // Change in panel display
                  inputStream.read(readByte);
                  byte icon1=readByte[0];
                  inputStream.read(readByte);
                  byte icon2=readByte[0];

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
                    String keyPadMsgs = new String(keyPad);
                    keyPadMsg1 = new String(keyPadMsgs.substring(0,16));
                    keyPadMsg2 = new String(keyPadMsgs.substring(16));
                    String iconStr = new String();
                    iconStr=processIcons(icon1, icon2);
                    if (debug)
                      System.out.println("Got a new panel msg!");

/*                    outWml = new PrintWriter(
                                    new OutputStreamWriter(
                                        new FileOutputStream("C:\\Program Files\\Apache Group\\Apache\\htdocs\\a.wml")));
                    outHtml = new PrintWriter(
                                    new OutputStreamWriter(
                                        new FileOutputStream("C:\\Program Files\\Apache Group\\Apache\\htdocs\\a.html")));
                    outHtml.write(firstLine + "<br>" + secondLine + "<br>");
                    if (iconStr != "")
                      outHtml.write(iconStr+"\n");
                    outWml.write("<?xml version=\"1.0\"?>");
                    outWml.write("<!DOCTYPE wml PUBLIC \"-//PHONE.COM//DTD WML 1.1//EN\"\n");
                    outWml.write("\"http://www.phone.com/dtd/wml11.dtd\"><wml>\n");
                    outWml.write("<head>\n");
                    outWml.write("<meta http-equiv=\"Cache_Control\" content=\"max-age=0\"/>\n");
                    outWml.write("</head>\n");
                    outWml.write("<card ontimer='a.wml'><timer value='30'/><p>\n");
                    outWml.write(firstLine + "<br/>" + secondLine+"\n");
                    if (iconStr != "")
                      outWml.write(iconStr+"\n");
                    outWml.write("</p></card></wml>\n");
                    outWml.close();
                    outHtml.close();
                    System.out.println(firstLine + "\n" + secondLine);
                    if (iconStr != "")
                      System.out.println(iconStr);
                    System.out.println("****************");*/
                  }
                }
                } catch (IOException e) {}
              }
              break;
        }

    }
    private static String processIcons(byte icon1, byte icon2) {
      String iconStr = new String("");
      if (icon1!=0) {
        if ((icon1 & 1) ==1)
          iconStr = iconStr + "Red ";
        if ((icon1 & 2) ==2)
          iconStr = iconStr + "Green ";
        if ((icon1 & 4) ==4)
          iconStr = iconStr + "Bypass ";
        if ((icon1 & 8) ==8)
          iconStr = iconStr + "SysTbl ";
        if ((icon1 & 16) ==16)
          iconStr = iconStr + "Fire ";
        if ((icon1 & 32) ==32)
          iconStr = iconStr + "FireTbl ";
        if ((icon1 & 64) ==64)
          iconStr = iconStr + "AudOnCont ";
//        if ((icon1 & 128) ==128)
//          iconStr = iconStr + "E/E ";
        if (icon2!=0 && iconStr != "")
          iconStr+="\n";
      }
      if (icon2!=0) {
        iconStr+="Pulse: ";
        if ((icon2 & 1) ==1)
          iconStr = iconStr + "Alarm ";
        if ((icon2 & 2) ==2)
          iconStr = iconStr + "Green ";
        if ((icon2 & 4) ==4)
          iconStr = iconStr + "Bypass ";
        if ((icon2 & 8) ==8)
          iconStr = iconStr + "SysTbl ";
        if ((icon2 & 16) ==16)
          iconStr = iconStr + "Instant ";
        if ((icon2 & 64) ==64)
          iconStr = iconStr + "AudCont ";
        if ((icon2 & 128) ==128)
          iconStr = iconStr + "Armed ";
      }
      return(iconStr);
    }

}
