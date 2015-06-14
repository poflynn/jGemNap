import java.io.*;
import java.util.*;
import javax.comm.*;

public class myserial implements Runnable, SerialPortEventListener {
//public class myserial {
    static CommPortIdentifier portId;
    static Enumeration portList;

/*    static OutputStream outputStream;
    static InputStream inputStream;
    static SerialPort serialPort;    */
    static OutputStream outputStream;
    static InputStream inputStream;
    static SerialPort serialPort;
    Thread readThread;
    static PrintWriter outWml;
    static PrintWriter outHtml;
    static int SndCkSum;
    static int SndKey;
    static int SndKeyOff;
    static int CkSum;
    static int Key;
    static int keyoff;
    static int i;
    static boolean inSecurity=false;


    public static void main(String[] args) {
//        myserial test = new myserial();
        portList = CommPortIdentifier.getPortIdentifiers();

        while (portList.hasMoreElements()) {
            portId = (CommPortIdentifier) portList.nextElement();
            if (portId.getPortType() == CommPortIdentifier.PORT_SERIAL) {
                if (portId.getName().equals("COM1")) {
                //if (portId.getName().equals("/dev/term/a")) {
//                    myserial reader = new myserial();
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

/*        SendByte(0x86);

        System.out.print("CTS true, waiting\n");
        while (serialPort.isCTS() == true) {
           i++;
        }
        System.out.println("CTS took"+i);
        i=0;

        serialPort.setRTS(true);



        //******* Send and receive all cmds after here.

//        SendSecurityCmd();
  //      Rcv();

//        SendVersionCheck();
  //      Rcv();

//        GetStatus();
  //      Rcv();

        //******* Send and receive all cmds before here.



        serialPort.setRTS(false);

        System.out.print("Waiting for panel to finish\n");
        while (serialPort.isCTS() == false) {
           i++;
        }
        System.out.println("Panel finished in " + i);*/

        myserial reader = new myserial();

                }
            }
        }
    }


/*  Send security command */

    private static void SendSecurityCmd() {
      System.out.println ("\nAbout to send security cmd.\n");
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
      inSecurity=true;
      SndChr(0x04);
      SndChr(0x08);
      SndChr(0x00);
      SndChr(0x09);
      SndChr(0x02);
      SndChr(0x09);
      inSecurity=false;
      SndChr(SndCkSum);
      System.out.println ("\nDone sending security cmd.\n");
    }


    private static void SendVersionCheck() {
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
      System.out.println ("\nDone sending version check.\n");
    }


    private static void GetStatus() {
      System.out.println ("\nAbout to get status.\n");
      SndKeyOff=0x86;
      SendByte(SndKeyOff);
      SndKey = (SndKeyOff  + 0x35) & 0xFF;
      SndCkSum = (SndKeyOff + 1) & 0xFF;


      SndChr(0x39);
      SndChr(0x00);
      SndChr(0x00);
//      SndChr(0x02); //Open zones
      SndChr(0x03); //Protected zones
      SndChr(0x00);
      SndChr(0x00);
      SndChr(SndCkSum);
      System.out.println ("\nDone sending version check.\n");
    }



/*  Send and receive a byte at a time */

    private static void SendByte(int info) {
      try {
          outputStream.write((byte) info);
//         System.out.println ("Raw sent: " + info);
      } catch (IOException e) {}
    }

    private static void SndChr(int TmpByte) {
      int Send;
      if (inSecurity==true & TmpByte==0)
        TmpByte=10;
      SndCkSum = (SndCkSum + TmpByte) & 0xFF;
      Send= TmpByte ^ SndKey;
      SendByte(Send);
      System.out.println ("  Byte to be sent=" + Integer.toHexString(TmpByte) + " ( " + TmpByte + "), SndKey="+Integer.toHexString(SndKey)  + " Actual byte sent = " + Integer.toHexString(Send));
      SndKey = (SndKey + SndKeyOff) & 0xFF;
    }




/*  Receive response */

    private static boolean Rcv() {
      int rcvcksum;
      byte [] RcvBuf = new byte[200];

      while (serialPort.isCTS() == true) {
          System.out.print("CTS true, waiting in rcv\n");
      }

      CkSum=1;
      keyoff= ReadByte();
      CkSum=(CkSum+keyoff) & 0xFF;
      Key=(keyoff + 0x35) & 0xFF;

      RcvBuf[0] = ReadAndChecksum();
      RcvBuf[1] = ReadAndChecksum();

      System.out.println ("\nEntering for loop...");

      for (i=2; i<=RcvBuf[1]+5; i++)
        RcvBuf[i] = ReadAndChecksum();

      System.out.println ("Leaving for loop...\n");

      i = ReadByte();
      rcvcksum = (int) (i ^ Key);
      System.out.println ("Checksum check is " + (boolean) (rcvcksum==CkSum) + "\n");
      return (rcvcksum==CkSum);
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
      System.out.println ("Byte & checksum read=" + Integer.toHexString(Info));
      CkSum = (CkSum + Info) & 0xFF;
      Key = (Key + keyoff) & 0xFF;
      return (Info);
    }

/************ Thread ***********/

    public myserial() {
        try {
            serialPort.addEventListener(this);
        } catch (TooManyListenersException e) {}
        serialPort.notifyOnDataAvailable(true);
        readThread = new Thread(this);
        readThread.start();
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
                    String firstLine = keyPadMsgs.substring(0,16);
                    String secondLine = keyPadMsgs.substring(16);
                    String iconStr = new String();
                    iconStr=processIcons(icon1, icon2);

                    outWml = new PrintWriter(
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
                    System.out.println("****************");
                  }
                }
              } catch (IOException e) {}
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

// System.out.print (Integer.toHexString((int) readBuffer[i] & 0xFF) + "->" + (char) readBuffer[i] + " ");
