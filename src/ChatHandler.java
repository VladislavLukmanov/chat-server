import java.io.*;
import java.net.Socket;
import java.util.*;

public class ChatHandler extends Thread {
    private final int MAX_HADLERS_SIZE = 5;
    private volatile boolean flag;
    private final Socket socket;
    DataInputStream dataInputStream;
    DataOutputStream dataOutputStream;
    private static Set<String> abusiveWords;
    private static List<ChatHandler> handlers = Collections.synchronizedList(new ArrayList<>());
    private static List<ChatHandler> passiveHandlers = Collections.synchronizedList(new ArrayList<>());

    public ChatHandler(Socket socket) throws IOException {
        loadAbusiveWords();
        this.socket = socket;
        dataInputStream = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
        dataOutputStream = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
    }

    public void loadAbusiveWords() {
        if (abusiveWords == null) {
            abusiveWords = new HashSet<>();
            try (BufferedReader in = new BufferedReader(new FileReader("Abusive_words.txt"))) {
                String line;
                while ((line = in.readLine()) != null) {
                    abusiveWords.add(line);
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        System.out.println(abusiveWords.size());
    }

    @Override
    public void run() {
        if (handlers.size() < MAX_HADLERS_SIZE) {
            handlers.add(this);
        } else {
            passiveHandlers.add(this);
        }
        flag = true;
        try {
            while (flag) {
                if (handlers.contains(this)) {
                    String message = dataInputStream.readUTF();
                    broadcast(message);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            flag = false;
            handlers.remove(this);
            activateHandlers();
            try {
                dataOutputStream.close();
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void activateHandlers() {
        while (handlers.size() < MAX_HADLERS_SIZE && passiveHandlers.size() > 0) {
            handlers.add(passiveHandlers.get(0));
            passiveHandlers.remove(0);
        }
    }

    private void broadcast(String message) {
        synchronized (handlers) {
            if (messageContainsBadWords(message)) {
                flag = false;
                handlers.remove(this);
                activateHandlers();
            }
            Iterator<ChatHandler> iterator = handlers.iterator();
            while (iterator.hasNext()) {
                ChatHandler chatHandler = iterator.next();
                try {
                    writeMessage(chatHandler.dataOutputStream, message);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private boolean messageContainsBadWords(String message) {
        for (String abusiveWord : abusiveWords) {
            if (message.contains(abusiveWord)) {
                return true;
            }
        }
        return false;
    }

    private void writeMessage(DataOutputStream dataOutputStream, String message) throws IOException {
        synchronized (dataOutputStream) {
            dataOutputStream.writeUTF(message);
        }
        dataOutputStream.flush();
    }
}