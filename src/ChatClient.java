import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.Socket;

public class ChatClient extends JFrame implements Runnable {
    private final Socket socket;
    private final DataInputStream dataInputStream;
    private final DataOutputStream dataOutputStream;
    private final JTextArea outTextArea;
    private final JTextField inTextField;
    private final JButton sendButton;
    volatile boolean flag = true;

    public ChatClient(Socket socket, DataInputStream dataInputStream, DataOutputStream dataOutputStream) {
        super("Client");
        this.socket = socket;
        this.dataInputStream = dataInputStream;
        this.dataOutputStream = dataOutputStream;

        setSize(400, 500);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        outTextArea = new JTextArea();
        outTextArea.setBounds(0,0, getWidth(), getHeight() - 60);
        outTextArea.setEnabled(false);
        add(outTextArea);
        inTextField = new JTextField();
        inTextField.setBounds(0, outTextArea.getHeight(), getWidth() - 117, 20);
        add(inTextField);
        sendButton = new JButton("Send");
        sendButton.setBounds(inTextField.getWidth(), inTextField.getY(), 100, inTextField.getHeight());
        add(sendButton);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                super.windowClosed(e);
                flag = false;
                try {
                    dataOutputStream.close();
                    socket.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        });

        inTextField.addActionListener(e -> {
            enterListener();
        });

        sendButton.addActionListener(e -> enterListener());

        setLayout(null);
        setVisible(true);
        inTextField.requestFocus();
        new Thread(this).start();
    }

    public void enterListener() {
        try {
            dataOutputStream.writeUTF(inTextField.getText());
            dataOutputStream.flush();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        inTextField.setText("");
    }

    public static void main(String[] args) {
        String site = "localhost";
        String port = "8080";

        Socket socket = null;
        DataInputStream dataInputStream = null;
        DataOutputStream dataOutputStream = null;
        try {
            socket = new Socket(site, Integer.parseInt(port));
            dataInputStream = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            dataOutputStream = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
            new ChatClient(socket, dataInputStream, dataOutputStream);
        } catch (IOException e) {
            e.printStackTrace();
            try {
                if (dataOutputStream != null) {
                    dataOutputStream.close();
                }
                if (socket != null) {
                    socket.close();
                }
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
    }

    @Override
    public void run() {
        try {
            while (flag) {
                String line = dataInputStream.readUTF();
                outTextArea.append(line + "\n");
            }
        } catch (IOException e) {
            System.out.println("Клиент вышел из сети");
        } finally {
            inTextField.setEnabled(false);
            sendButton.setEnabled(false);
            validate();
        }
    }
}