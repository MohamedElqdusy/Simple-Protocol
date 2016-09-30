package simpleprotocol;

import java.io.IOException;
import java.net.*;
import java.util.Random;
import java.util.Scanner;

public class Server {
    private static DatagramSocket udpSoc = null;
    private static InetAddress clientAddress = null;
    private static int clientPort;
    static String msgStatement;
    static Scanner sc;
    static String serverSession;
    static Random rand;
    static Message ms;

    /*
     * Main thread sends packet .
     */
    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Must assign <Port number>");
            System.out.println("Try using \"javac Server <Port number>\"");
        } else {
            try {
                int udpPort = Integer.valueOf(args[0]);
                udpSoc = new DatagramSocket(udpPort);

                // get local adddress and port
                String localAddr = InetAddress.getLocalHost().getHostAddress();
                System.out.printf("%S  %S \n", localAddr, udpPort);

                // make another thread to handle Client Response .
                ClientInputHandler respThread = new ClientInputHandler();
                Thread thr1 = new Thread(respThread);
                thr1.start();

                // P0P message
                ms = new Message();

                // Send respond to Client
                sc = new Scanner(System.in);
                while (sc.hasNextLine()) {
                    sendDataPacket(sc.nextLine());
                }
                // close the scanner when we are done with reading
                sc.close();

            } catch (NumberFormatException e) {
                System.out.println("NumberFormat: " + e.getMessage());
            } catch (SocketException e) {
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

    // function to send DATA packet
    public static void sendDataPacket(String s) throws IOException {
        // take input stream and store in Data payload .
        ms.DataPayload = s;
        ms.sequence++; // increment message sequence
        ms.command = 1; // command for data
        sendPacket();
    }

    // function to send Hello packet
    public static void sendHelloPacket() throws IOException {
        ms.DataPayload = "HELLO";
        ms.command = 0; // command for Hello .
        sendPacket();
    }

    // function to send GOODBYE packet .
    public static void sendGoodbyePacket() throws IOException {
        ms.DataPayload = "GOODBYE";
        ms.command = 3; // command for Hello .
        sendPacket();
    }

    // function to send ALIVE packet .
    public static void sendAlivePacket() throws IOException {
        ms.DataPayload = "Alive";
        ms.command = 2; // command for Hello .
        sendPacket();
    }

    // function to send a lost packet message.
    public static void sendLostPacket() throws IOException {
        ms.DataPayload = "Lost packet";
        ms.sequence++; // increment message sequence
        sendPacket();
    }

    // function to send a Duplicate packet message.
    public static void sendDuplicatePacket() throws IOException {
        ms.DataPayload = "Duplicate packet";
        sendPacket();
    }

    // function to send packet .
    public static void sendPacket() throws IOException {
        msgStatement = ms.toString();
        if (clientAddress != null) {
            byte[] requestMessag = msgStatement.getBytes();
            int requestLength = msgStatement.length();
            DatagramPacket requestePacket = new DatagramPacket(requestMessag,
                    requestLength, clientAddress, clientPort);
            udpSoc.send(requestePacket);
        }
    }

    // function to close server connection
    public static void closeServer() {
        System.out.println("Client closed connection \n");
        udpSoc.close();
        sc.close();
    }

    // function to extract sequence number to remove [ ] .
    public static int extractSequence(String s) {
        int n = s.indexOf("]"); // to remove ] from string .
        // start with 1 to remove [ .
        return Integer.valueOf(s.substring(1, n));
    }

    // Thread to receive Client packet
    public static class ClientInputHandler implements Runnable {
        @Override
        public void run() {
            String receivedMsg;
            int seq = 0; // received client sequence .
            int nextSequence = 0;

            // keep handling data until see EOF on stdio
            while (true) {
                // create buffer
                byte[] buff = new byte[1024];
                // create Datagrame packet to handle input from client
                DatagramPacket response = new DatagramPacket(buff, buff.length);
                try {
                    // handle the received data
                    udpSoc.receive(response);

                    if (clientAddress == null
                            || !clientAddress.equals(response.getAddress())) {
                        // get the address and the port number of the client
                        clientAddress = response.getAddress();
                        clientPort = response.getPort();

                        // print out client's address and port number
                        System.out.println("[Connecting from "
                                + clientAddress.getHostAddress() + ":"
                                + clientPort + "]");
                        sendHelloPacket();
                    }

                    receivedMsg = new String(response.getData());

                    // to handle sequence number in substing(18) .
                    seq = extractSequence(receivedMsg.substring(18));

                    /*
                     * Close server connection if the checked magic number and
                     * version aren't true , or if Client sends "GOODBYE" = "3".
                     */
                    if ((!(receivedMsg.substring(0, 6).equals("502731")))
                            || receivedMsg.substring(2).equals("3")) {
                        closeServer();
                    }

                    /*
                     * handle lost packets If the next packet received has a
                     * sequence number greater than the expected number .
                     */
                    if (seq > nextSequence) {
                        System.out.println("############### Lost packet ###############");
                        nextSequence = seq +1;
                        sendLostPacket();                       
                    }

                    // handle Duplicate packets
                    else if (seq < nextSequence-1) {
                        System.out.println("Duplicate Packet \n");
                        sendDuplicatePacket();
                        sendGoodbyePacket();
                        closeServer();
                    }

                    // print the received response
                    else if (seq == nextSequence) {
                        System.out.println(
                                receivedMsg.substring(7));
                        nextSequence = seq +1;
                        System.out.printf("nextSequence = %s \n",nextSequence);
                        sendAlivePacket();
                        
                    }

                    // handle protocol error
                    else {
                        sendGoodbyePacket();
                        closeServer();
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