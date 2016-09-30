package simpleprotocol;

import java.net.*;
import java.io.*;
import java.util.Scanner;

public class Client {
    private static DatagramSocket udpSoc = null;
    private static InetAddress serverAddress = null;
    private static int udpPort;
    static String message ;
    private static Scanner sc ;
    // main thread sends request to Server
    public static void main(String[] args) {

        if (args.length != 2) {
            System.out.println("Must assign <Address> and <Port number>");
            System.out.println(
                    "Try using \"javac Client <Address> <Port number>\"");
        } else {
            try {
                
                udpSoc = new DatagramSocket();

                // get arguments
                serverAddress = InetAddress.getByName(args[0]);
                udpPort = Integer.valueOf(args[1]);

              //P0P message
                Message ms = new Message();
                ms.DataPayload = "Session created" ;
                // send first information in UDP packet to Server
                String meseageContent = ms.toString();
                byte[] msg = meseageContent.getBytes();
                int addPortLen = meseageContent.length();
                DatagramPacket msgPack = new DatagramPacket(msg, addPortLen,
                        serverAddress, udpPort);
                udpSoc.send(msgPack);
                             
                /*
                 * make another thread to handle Server Response while the main
                 * thread sends packet
                 */
                ServerInputHandler respThread = new ServerInputHandler();
                Thread thr1 = new Thread(respThread);
                thr1.start();
                
                  
                // request to Server
                 sc = new Scanner(System.in);
                while (sc.hasNextLine()) {
                    // take input stream and store in Data payload . 
                    ms.DataPayload = sc.nextLine();
                    ms.sequence ++ ; // increment message sequence
                    ms.command = 1; // command for data 
                    message = ms.toString();
                    
                    if (serverAddress != null) {
                        byte[] reqMsg = message.getBytes();
                        int reqLen = message.length();
                        DatagramPacket reqPack = new DatagramPacket(reqMsg,
                                reqLen, serverAddress, udpPort);
                        udpSoc.send(reqPack);
                    }
                }
                // close the scanner when we are done with reading
                sc.close();
            }

            catch (SocketException e) {
                System.out.println("Socket error :" + e.getMessage());
            } catch (UnknownHostException e) {
                System.out.println("UnknownHost: " + e.getMessage());
            } catch (IOException e) {
                System.out.println("IOException error :" + e.getMessage());
            } finally {
                if (udpSoc != null)
                    udpSoc.close();
            }

        }
    }

    //Thread to receive Server packet
    public static class ServerInputHandler implements Runnable {

        @Override
        public void run() {
            String receivedMsg;
            // keep handling data until see EOF on stdio
            while (true) {
                // create buffer
                byte[] buff = new byte[1024];
                // create Datagrame packet to handle input from server
                DatagramPacket response = new DatagramPacket(buff, buff.length);
                try {
                    // handle the received data
                    udpSoc.receive(response);
                    // print the received response
                    receivedMsg = new String(response.getData());
                    System.out.println(receivedMsg.substring(7));

                    /*
                     * Close Client connection if the checked magic number and version aren't true ,
                     *  or Server sends "GOODBYE" == "3".
                     */
                    if ((!(receivedMsg.substring(0, 6).equals("502731"))) || receivedMsg.substring(2).equals("3")) {
                        System.out.println("Server closed connection \n");
                        sc.close();
                        udpSoc.close();
                    }
                } catch (SocketException e) {
                    break;
                } catch (IOException e) {
                    System.out.println("IOException error :" + e.getMessage());

                }

            }

        }

    }

}
