import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

class ChatServer {
    final static String HOSTNAME = "127.0.0.1";
    final static int PORT = 60000;
    static List<PrintWriter> writers = new ArrayList<>();
    static List<String> users = new ArrayList<>();
    private final Logger log = Logger.getLogger(ChatServer.class.getName());

    void run() {
        try (var serverSocket = new ServerSocket(PORT)) {
            log.info("Server is currently running.");
            var pool = Executors.newFixedThreadPool(200);
            log.info("Initialized thread pool.");
            while (true) {
                log.info("Waiting for new connection...");
                pool.execute(new ChatHandler(serverSocket.accept()));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class ChatHandler implements Runnable {
        private final Socket socket;
        private final Logger log = Logger.getLogger(ChatHandler.class.getName());
        private Scanner in;
        private PrintWriter out;
        private String username;
        private final static String QUIT_SIGNAL = "!q";

        ChatHandler(Socket socket) {
            this.socket = socket;
            log.info("Established new connection.");
        }

        @Override
        public void run() {
            try {
                this.in = new Scanner(socket.getInputStream());
                this.out = new PrintWriter(socket.getOutputStream(), true);

                submitName();
                broadcastNewUser();
                handleUserMessages();

            } catch (IOException | RuntimeException e) {
                out.println(e.getMessage());
            } finally {
                ChatServer.users.remove(this.username);
                this.in.close();
                this.out.close();
            }
        }

        private void handleUserMessages() {
            while (true) {
                String message = in.nextLine();
                if(userWantsToLeave(message)) {
                    break;
                }
                broadcastMessage(message);
            }
        }

        private void broadcastMessage(String message) {
            writers.forEach(writer -> {
                if(!message.isBlank()) {
                    writer.println(username + ": " + message);
                }
            });
        }

        private boolean userWantsToLeave(String message) {
            return QUIT_SIGNAL.equals(message);
        }

        private void broadcastNewUser() {
            writers.forEach(printWriter -> printWriter.println("New user " + this.username + " has joined."));
            writers.add(this.out);

        }

        private void submitName() {
            do {
                out.println("Submit a name to join the chat:");
                this.username = in.next();
                if (Objects.isNull(this.username) || StringUtils.isBlank(this.username)) {
                    throw new RuntimeException("User cannot be null!");
                }
            } while (hasUserGivenAlreadyUsedName(username));

            out.println("Given name: " + this.username);
            ChatServer.users.add(this.username);
        }

        private synchronized boolean hasUserGivenAlreadyUsedName(String name) {
            return users.contains(name);
        }

        public static void main(String[] args) {
            ChatServer server = new ChatServer();
            server.run();
        }
    }
}
