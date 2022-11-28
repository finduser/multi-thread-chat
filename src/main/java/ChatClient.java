import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.Executors;

class ChatClient {
    void connect() throws IOException {
        Socket socket = new Socket(ChatServer.HOSTNAME, ChatServer.PORT);

        var pool = Executors.newFixedThreadPool(5);

        pool.execute(new IncomingMessagesHandles(new Scanner(socket.getInputStream())));
        pool.execute(new UserMessagesHandler(new PrintWriter(socket.getOutputStream(), true)));g

    }
    private static class UserMessagesHandler implements Runnable {
        private PrintWriter out;
        private Scanner scanner;
        public UserMessagesHandler(PrintWriter out) {
            this.out = out;
            scanner = new Scanner(System.in);
        }

        @Override
        public void run() {
            while (true) {
                out.println(scanner.nextLine());
            }
        }
    }

    private static class IncomingMessagesHandles implements Runnable {
        private Scanner in;

        public IncomingMessagesHandles(Scanner in) {
            this.in = in;
        }

        @Override
        public void run() {
            while(true) {
                System.out.println(in.nextLine());
            }
        }
    }

    public static void main(String[] args) {
        ChatClient client = new ChatClient();
        try {
            client.connect();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
