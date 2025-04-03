import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Client {
    static String id;
    static String hostName = "localhost";
    static String multicastGroup = "230.0.0.1";
    static int portNumber = 12345;
    static int portNumber2 = 12346;
    static MulticastSocket multicastSocket = null;
    static boolean multicastSigned = false;
    static ExecutorService threadPool = Executors.newFixedThreadPool(3);
    static String udpMessage1 = """
            ░░░░░▄▄▄▄▄░▄░▄░▄░▄
            ▄▄▄▄██▄████▀█▀█▀█▀██▄
            ▀▄▀▄▀▄████▄█▄█▄█▄█████
            ▒▀▀▀▀▀▀▀▀██▀▀▀▀██▀▒▄██
            ▒▒▒▒▒▒▒▒▀▀▒▒▒▒▀▀▄▄██▀▒""";
    static String udpMessage2 = """
            ───▄▄▄
            ─▄▀░▄░▀▄
            ─█░█▄▀░█
            ─█░▀▄▄▀█▄█▄▀
            ▄▄█▄▄▄▄███▀""";
    static String udpMessage3 = """
            ───▄▀▀▀▄▄▄▄▄▄▄▀▀▀▄───
            ───█▒▒░░░░░░░░░▒▒█───
            ────█░░█░░░░░█░░█────
            ─▄▄──█░░░▀█▀░░░█──▄▄─
            █░░█─▀▄░░░░░░░▄▀─█░░█""";

    public static void main(String[] args) {
        // Otwieranie gniazda klienta TCP
        try (Socket socket = new Socket(hostName, portNumber)) {
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

            // Otwieranie gniazda klienta UDP
            DatagramSocket udpSocket = new DatagramSocket();
            InetAddress address = InetAddress.getByName("localhost");

            // Pobieranie interfejsu sieciowego
            NetworkInterface networkInterface = NetworkInterface.getByInetAddress(InetAddress.getLocalHost());

            // Odbieranie ID
            id = in.readLine().split(" ")[1];
            byte[] sendBuffer = ("PingUDP " + id).getBytes();

            // Wysyłanie wiadomości UDP w celu zarejestrowania klienta
            DatagramPacket sendPacket = new DatagramPacket(sendBuffer, sendBuffer.length, address, portNumber);
            udpSocket.send(sendPacket);

            // Otwieranie gniazda multicastowego
            multicastSocket = new MulticastSocket(portNumber2);
            
            // Wątki do odbierania wiadomości
            threadPool.submit(() -> gettingMessagesTCP(in, socket));
            threadPool.submit(() -> gettingMessagesUDP(udpSocket));
            threadPool.submit(() -> gettingMessagesMulticast(multicastSocket));

            Scanner scanner = new Scanner(System.in);

            // Wątek do obsługi zakończenia programu
            Runtime.getRuntime().addShutdownHook(new Thread(() -> handleQuit(out, socket, udpSocket, multicastSocket, scanner)));


            while (!socket.isClosed()) {
                // Odczytanie wiadomości od użytkownika
                if (!scanner.hasNext()) break;

                String message = scanner.nextLine();

                // Obsługa komend
                // /quit - zakończenie programu
                if (message.equals("/quit")) {
                    handleQuit(out, socket, udpSocket, multicastSocket, scanner);
                    System.out.flush();
                    break;
                }
                // /list - wyświetlenie dostępnych emoji
                else if (message.equals("/list")) {
                    System.out.println("Type \"U\" and number to send an emoji");
                    System.out.println("number 1: alligator\nnumber 2: snail\nnumber 3: teddy bear");
                    continue;
                }
                // Informacja o błędnej komendzie
                else if (message.charAt(0) == '/') {
                    System.out.println("Wrong command");
                    continue;
                }

                // Wysyłanie wiadomości
                String[] parts = message.split(" ");

                // Wysyłanie emoji przez UDP
                if (parts[0].equals("U") && parts.length == 2) {
                    switch (parts[1]) {
                        case "1" -> message = udpMessage1;
                        case "2" -> message = udpMessage2;
                        case "3" -> message = udpMessage3;
                        default -> {
                            System.out.println("No such emoji");
                            continue;
                        }

                    }
                    sendBuffer = message.getBytes();
                    sendPacket = new DatagramPacket(sendBuffer, sendBuffer.length, address, portNumber);
                    udpSocket.send(sendPacket);
                }
                else if (parts[0].equals("U")) {
                    System.out.println("Wrong number of arguments");
                }

                // Grupa multicastowa
                else if (parts[0].equals("M") && parts.length == 1) {
                    // Zapisanie do grupy multicastowej
                    if (!multicastSigned) {
                        // Informacja o zapisaniu do grupy multicastowej
                        System.out.println("You have signed up for multicast group" +
                                "\nTo send a message to the group type \"M\" and your message");



                        // Dołączenie do grupy multicastowej
                        InetAddress multicastAddress = InetAddress.getByName(multicastGroup);
                        multicastSocket.joinGroup(new InetSocketAddress(multicastAddress, portNumber2), networkInterface);

                        multicastSigned = true;
                    }
                    // Wypisanie z grupy multicastowej
                    else {
                        // Informacja o wypisaniu z grupy multicastowej
                        System.out.println("You have left the multicast group");

                        multicastSigned = false;

                        // Wypisanie z grupy multicastowej
                        multicastSocket.leaveGroup(new InetSocketAddress(InetAddress.getByName(multicastGroup), portNumber2), networkInterface);
                    }

                }
                // Wysyłanie wiadomości do grupy multicastowej
                else if (parts[0].equals("M")){
                    if (multicastSocket != null && multicastSigned) {
                        // Przygotowanie adresu multicastowego
                        InetAddress multicastAddress = InetAddress.getByName(multicastGroup);

                        // Przygotowanie wiadomości
                        String messageContent = message.substring(2);
                        message = id +" (M) Client " + id + ": " + messageContent;
                        sendBuffer = message.getBytes();

                        // Wysłanie wiadomości
                        sendPacket = new DatagramPacket(sendBuffer, sendBuffer.length, multicastAddress, portNumber2);
                        multicastSocket.send(sendPacket);
                    }
                    else System.out.println("You are not signed up for a multicast group");

                }
                // Wysłanie wiadomości TCP
                else
                    out.println(message);
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Server is down");
        }
    }

    // Odbieranie wiadomości TCP
    private static void gettingMessagesTCP(BufferedReader in, Socket socket) {
        try {
            while (!socket.isClosed()) {
                if (in.ready()) {
                    String msg = in.readLine();
                    if (msg == null) {
                        socket.close();
                        break;
                    }
                    System.out.println(msg);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Odbieranie wiadomości UDP
    private static void gettingMessagesUDP(DatagramSocket socket) {
        try {
            byte[] receiveBuffer = new byte[1024];
            DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
            while (!socket.isClosed()) {
                try {
                    socket.receive(receivePacket);
                    String msg = new String(receivePacket.getData(), 0, receivePacket.getLength()).trim();
                    System.out.println(msg);
                } catch (SocketException e) {
                    if (socket.isClosed()) {
                        break;
                    } else {
                        throw e;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            
        }       
        
    }

    // Odbieranie wiadomości z grupy multicastowej
    private static void gettingMessagesMulticast(MulticastSocket multicastSocket) {
        try {
            byte[] receiveBuffer = new byte[1024];
            DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
            while (!multicastSocket.isClosed() ) {
                try {
                    multicastSocket.receive(receivePacket);
                    String msg = new String(receivePacket.getData(), 0, receivePacket.getLength()).trim();

                    // Wyświetlenie wiadomości tylko jeśli nie jest od tego samego klienta
                    String cid = msg.split(" ")[0];
                    if (id.equals(cid)) continue;

                    // Wyświetlenie wiadomości
                    String messageContent = msg.substring(msg.indexOf(' ') + 1);
                    System.out.println(messageContent);
                } catch (SocketException e) {
                    if (multicastSocket.isClosed()) {
                        break;
                    } else {
                        throw e;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();

        }

    }

    private static void handleQuit(PrintWriter out, Socket socket, DatagramSocket udpSocket, MulticastSocket multicastSocket, Scanner scanner) {
        try {
            out.println("/quit");

            threadPool.shutdown();
            socket.close();
            udpSocket.close();
            multicastSocket.close();
            scanner.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}