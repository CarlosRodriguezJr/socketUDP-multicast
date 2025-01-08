package socketudp.multicast;

/**
 *
 * @author carlos
 */
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

public class SocketUDPMulticast extends JFrame {

    private static final String MULTICAST_ADDRESS = "224.0.0.0";
    private static final int PORT = 6789;
    private InetAddress group;
    private MulticastSocket socket;
    private JTextArea chatArea;
    private JTextField inputField;
    private ExecutorService executor;

    private String nombreAnfitrion;
    private volatile boolean running = true;

    public SocketUDPMulticast() {
        nombreAnfitrion = JOptionPane.showInputDialog(this, "Ingresa tu nombre:", "Nombre de usuario", JOptionPane.PLAIN_MESSAGE);
    
        if (nombreAnfitrion == null || nombreAnfitrion.trim().isEmpty()) {
            System.exit(0);
        }

        setTitle("Multicast Chat");
        setSize(400, 300);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setLocationRelativeTo(null);

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(chatArea);

        inputField = new JTextField();
        inputField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendMessage(inputField.getText());
                inputField.setText("");
            }
        });

        add(scrollPane, BorderLayout.CENTER);
        add(inputField, BorderLayout.SOUTH);

        executor = Executors.newSingleThreadExecutor();

        try {
            group = InetAddress.getByName(MULTICAST_ADDRESS);
            socket = new MulticastSocket(PORT);

            NetworkInterface netIf = NetworkInterface.getByInetAddress(InetAddress.getLocalHost());
            socket.joinGroup(new InetSocketAddress(group, PORT), netIf);

            sendNotification(nombreAnfitrion + " se ha unido al chat");

            executor.submit(this::receiveMessages);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendMessage(String message) {
        try {
            String fullMessage = nombreAnfitrion + ": " + message;
            byte[] buffer = fullMessage.getBytes();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, group, PORT);
            socket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendNotification(String notification) {
        try {
            byte[] buffer = notification.getBytes();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, group, PORT);
            socket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void receiveMessages() {
        try {
            byte[] buffer = new byte[1024];
            while (running) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                String message = new String(packet.getData(), 0, packet.getLength());

                SwingUtilities.invokeLater(() -> chatArea.append(message + "\n"));
            }
        } catch (IOException e) {
            if (running) {
                e.printStackTrace();
            }
        } finally {
            try {
                socket.leaveGroup(new InetSocketAddress(group, PORT), NetworkInterface.getByInetAddress(InetAddress.getLocalHost()));
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void dispose() {
        running = false;
        sendNotification(nombreAnfitrion + " ha abandonado el chat");
        super.dispose();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            SocketUDPMulticast chat = new SocketUDPMulticast();
            chat.setVisible(true);
            chat.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        });
    }
}
