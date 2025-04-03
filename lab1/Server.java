import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

record Tuple<A, B>(A Id, B Socket) {}
record Address<A, B>(A Address, B Port) {}

public class Server {
    private static final List<Tuple<Integer, Socket>> clientsTCP = new ArrayList<>();
    private static final ConcurrentHashMap<Address<InetAddress, Integer>, Integer> clientsUDPMap = new ConcurrentHashMap<>();
    private static Integer clientId = 0;
    private static final ExecutorService threadPool = Executors.newFixedThreadPool(10);

    public static void main(String[] args) {
        System.out.println("Server started");
        int portNumber = 12345;

        // Otwieranie gniazda serwera TCP
        try (ServerSocket serverSocket = new ServerSocket(portNumber)) {
            // Otwieranie gniazda serwera UDP
            DatagramSocket datagramSocket = new DatagramSocket(portNumber);

            // Dodanie obsługi ctrl+c
            Runtime.getRuntime().addShutdownHook(new Thread(() -> handleQuit(datagramSocket)));

            // Wątek obsługujący komunikację UDP
            threadPool.submit(() -> runClientUDP(datagramSocket));

            while (true) {
                // Akceptowanie nowego klienta
                Socket clientSocket = serverSocket.accept();

                clientsTCP.add(new Tuple<>(clientId++, clientSocket));
                System.out.println("New client id " + clientsTCP.getLast().Id() + " connected");

                // Wiadomość powitalna
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                out.println("ID "+clientsTCP.getLast().Id());
                out.println("Welcome to the chat!\n" +
                        "You are client number " +
                        clientsTCP.getLast().Id() +
                        "\nType /quit to disconnect\n" +
                        "Type /list to see all emojis\n" +
                        "Type M to sign up for a multicast group");

                // Wątek obsługujący komunikację TCP z klientem
                threadPool.submit(() -> runClientTCP(clientsTCP.getLast()));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Obsługa komunikacji z klientem TCP
    private static void runClientTCP(Tuple<Integer, Socket> last) {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(last.Socket().getInputStream()));
            while (true){
                // Odczytanie wiadomości od klienta
                String msg = in.readLine();

                // Sprawdzenie czy klient chce się rozłączyć
                if (msg == null || msg.equals("/quit")) {
                    clientsTCP.removeIf(client -> Objects.equals(client.Id(), last.Id()));
                    System.out.println("Client id " + last.Id() + " disconnected");
                    last.Socket().close();
                    break;
                }

                // Wysyłanie wiadomości do pozostałych klientów
                for (Tuple<Integer, Socket> client : clientsTCP) {
                    if (!Objects.equals(client.Id(), last.Id())) {
                        PrintWriter clientOut = new PrintWriter(client.Socket().getOutputStream(), true);
                        clientOut.println("Client " + last.Id() + ": " + msg);
                    }
                }
        }

    }
        catch (IOException e) {
        e.printStackTrace();
        }

    }

    // Obsługa komunikacji z klientem UDP
    private static void runClientUDP(DatagramSocket datagramSocket) {
        byte[] receiveBuffer = new byte[1024];
        try {
           while (true){
               // Czytanie wiadomości od klienta
               Arrays.fill(receiveBuffer, (byte) 0);
               DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
               datagramSocket.receive(receivePacket);

               // Obsługa wiadomości
               String msg = new String(receivePacket.getData(), 0, receivePacket.getLength()).trim();

               // Rejestracja klienta po kanale UDP
               if (msg.split(" ")[0].equals("PingUDP")){
                   Integer cID = Integer.parseInt(msg.split(" ")[1]);
                   clientsUDPMap.put(new Address<>(receivePacket.getAddress(), receivePacket.getPort()), cID);
               }
               else {
                   // Obsługa wiadomości emoji od klienta
                   String[] lines = msg.split("\n");
                   StringBuilder modifiedMsg = new StringBuilder();
                   Integer clientId = clientsUDPMap.get(new Address<>(receivePacket.getAddress(), receivePacket.getPort()));

                   for (String line : lines)
                       modifiedMsg.append("Client ").append(clientId).append(": ").append(line).append("\n");

                   msg = modifiedMsg.toString().trim();

                   // Wysyłanie wiadomości do pozostałych klientów
                   for (Address<InetAddress, Integer> client : clientsUDPMap.keySet()) {
                      if (!Objects.equals(clientsUDPMap.get(client), clientId)) {
                          byte[] sendBuffer = msg.getBytes();
                          DatagramPacket sendPacket = new DatagramPacket(sendBuffer, sendBuffer.length, client.Address(), client.Port());
                          datagramSocket.send(sendPacket);
                      }
                 }
               }
           }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Obsługa zamknięcia serwera
    private static void handleQuit(DatagramSocket datagramSocket){
        try {
            // Zamknięcie gniazd serwera
            for (Tuple<Integer, Socket> client : clientsTCP) {
                PrintWriter clientOut = new PrintWriter(client.Socket().getOutputStream(), true);
                clientOut.println("Server is shutting down");
                client.Socket().shutdownOutput();
                client.Socket().close();
            }
            threadPool.shutdown();
            datagramSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
