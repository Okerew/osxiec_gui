import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.URI;
import java.util.Properties;

public class OsxiecApp extends JFrame {
    private JTextField directoryField;
    private JTextArea outputArea;
    private JPanel centerPanel;
    private DefaultListModel<String> historyListModel;
    private JList<String> historyList;
    private boolean isDarkMode = false;
    private JPanel topPanel, buttonPanel, historyPanel;
    private static final String CONFIG_FILE = System.getProperty("user.home") + "/.osxiec/config.properties";
    private JTextField commandInputField;
    private Process currentProcess;
    private PrintWriter processInputWriter;
    private JPanel commandInputPanel;

    public OsxiecApp() {
        setTitle("Osxiec App");
        setSize(1000, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        setResizable(true);

        loadConfig();
        setupLookAndFeel();
        setupTopPanel();
        setupCenterPanel();
        setupCommandInputPanel();
        setupButtonPanel();
        setupHistoryPanel();

        // Create a new panel to hold both the command input and button panels
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(commandInputPanel, BorderLayout.NORTH);
        bottomPanel.add(buttonPanel, BorderLayout.CENTER);

        // Add panels to the main frame
        add(topPanel, BorderLayout.NORTH);
        add(centerPanel, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);
        add(historyPanel, BorderLayout.EAST);

        loadHistory();
        updateColors();

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                saveHistory();
                saveConfig();
            }
        });
    }

    private void loadConfig() {
        File tempFile = new File("config_tmp.properties");
        String command = "sudo cp " + CONFIG_FILE + " " + tempFile.getAbsolutePath();
        executeCommand(command);

        if (tempFile.exists()) {
            Properties prop = new Properties();
            try (InputStream input = new FileInputStream(tempFile)) {
                prop.load(input);
                isDarkMode = Boolean.parseBoolean(prop.getProperty("darkMode", "false"));
            } catch (IOException ex) {
                ex.printStackTrace();
            } finally {
                tempFile.delete();
            }
        }
    }

    private void saveConfig() {
        Properties prop = new Properties();
        prop.setProperty("darkMode", String.valueOf(isDarkMode));

        File tempFile = new File("config_tmp.properties");
        try (OutputStream output = new FileOutputStream(tempFile)) {
            prop.store(output, null);
        } catch (IOException io) {
            io.printStackTrace();
        }

        String command = "sudo mv " + tempFile.getAbsolutePath() + " " + CONFIG_FILE;
        executeCommand(command);
    }


    private void setupLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupTopPanel() {
        topPanel = new JPanel(new GridBagLayout());
        topPanel.setBorder(BorderFactory.createTitledBorder("Configuration"));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);

        gbc.gridx = 0;
        gbc.gridy = 0;
        topPanel.add(new JLabel("Working Directory:"), gbc);

        directoryField = new JTextField(20);
        directoryField.setEditable(false);
        gbc.gridx = 1;
        topPanel.add(directoryField, gbc);

        JButton directoryButton = new JButton("Select Directory");
        directoryButton.addActionListener(e -> selectDirectory());
        gbc.gridx = 2;
        topPanel.add(directoryButton, gbc);

        JButton darkModeToggle = new JButton("Dark Mode");
        darkModeToggle.addActionListener(e -> toggleDarkMode());
        gbc.gridx = 3;
        topPanel.add(darkModeToggle, gbc);

        add(topPanel, BorderLayout.NORTH);
    }

    private void setupCenterPanel() {
        centerPanel = new JPanel(new BorderLayout());
        outputArea = new JTextArea();
        outputArea.setEditable(false);
        outputArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(outputArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Output"));
        centerPanel.add(scrollPane, BorderLayout.CENTER);
        centerPanel.setVisible(false);
        add(centerPanel, BorderLayout.CENTER);
    }

    private void setupButtonPanel() {
        buttonPanel = new JPanel(new GridLayout(3, 4, 10, 10));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        addButton(buttonPanel, "Containerize", e -> containerize());
        addButton(buttonPanel, "Execute", e -> execute());
        addButton(buttonPanel, "Run with VLAN Network", e -> runWithVlanNetwork());
        addButton(buttonPanel, "Create VLAN Network", e -> createVlanNetwork());
        addButton(buttonPanel, "Clean", e -> clean());
        addButton(buttonPanel, "Osxiec Hub", e -> openWebpage("https://osxiec.glitch.me"));
        addButton(buttonPanel, "Close Process", e -> closeProcess());
        addButton(buttonPanel, "Deploy", e -> deploy());
        addButton(buttonPanel, "Scan", e -> scan());
        addButton(buttonPanel, "Deploym", e -> deployM());


        add(buttonPanel, BorderLayout.SOUTH);
    }


    private void setupHistoryPanel() {
        historyListModel = new DefaultListModel<>();
        historyList = new JList<>(historyListModel);
        historyList.setFont(new Font("SansSerif", Font.PLAIN, 12));
        JScrollPane historyScrollPane = new JScrollPane(historyList);

        historyScrollPane.setBorder( BorderFactory.createTitledBorder("Execution History"));

        JButton executeFromHistoryButton = new JButton("Execute Selected");
        executeFromHistoryButton.addActionListener(e -> executeFromHistory());

        historyPanel = new JPanel(new BorderLayout());
        historyPanel.add(historyScrollPane, BorderLayout.CENTER);
        historyPanel.add(executeFromHistoryButton, BorderLayout.SOUTH);

        add(historyPanel, BorderLayout.EAST);
    }

    private void setupCommandInputPanel() {
        commandInputPanel = new JPanel(new BorderLayout());
        commandInputField = new JTextField();
        JButton sendCommandButton = new JButton("Send Command");
        sendCommandButton.addActionListener(e -> sendCommand());

        commandInputPanel.add(commandInputField, BorderLayout.CENTER);
        commandInputPanel.add(sendCommandButton, BorderLayout.EAST);
        commandInputPanel.setBorder(BorderFactory.createTitledBorder("Send Command to Process"));
    }

    private void sendCommand() {
        if (processInputWriter != null) {
            String command = commandInputField.getText();
            processInputWriter.println(command);
            processInputWriter.flush();
            commandInputField.setText("");
            outputArea.append("> " + command + "\n");
        } else {
            JOptionPane.showMessageDialog(this, "No active process to send command to.", "No Process", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void addButton(JPanel panel, String title, ActionListener action) {
        JButton button = new JButton(title);
        button.addActionListener(action);
        panel.add(button);
    }

    private void toggleDarkMode() {
        isDarkMode = !isDarkMode;
        updateColors();
        saveConfig();
    }

    private void updateColors() {
        Color bgColor = isDarkMode ? new Color(43, 43, 43) : new Color(240, 240, 240);
        Color fgColor = isDarkMode ? new Color(187, 187, 187) : Color.BLACK;
        Color buttonBgColor = isDarkMode ? new Color(60, 60, 60) : new Color(230, 230, 230);
        Color buttonFgColor = isDarkMode ? new Color(200, 200, 200) : Color.BLACK;
        Color borderTitleColor = isDarkMode ? new Color(187, 187, 187) : Color.BLACK;

        // Update colors for all components
        updateComponentColors(this, bgColor, fgColor, buttonBgColor, buttonFgColor, borderTitleColor);

        // Refresh the UI
        SwingUtilities.updateComponentTreeUI(this);
    }

    private void updateComponentColors(Container container, Color bgColor, Color fgColor, Color buttonBgColor, Color buttonFgColor, Color borderTitleColor) {
        for (Component c : container.getComponents()) {
            if (c instanceof JButton) {
                c.setBackground(buttonBgColor);
                c.setForeground(buttonFgColor);
                ((JButton) c).setOpaque(true);
                ((JButton) c).setBorderPainted(false);
            } else if (c instanceof JLabel) {
                c.setForeground(fgColor);
            } else if (c instanceof JPanel) {
                Border border = ((JPanel) c).getBorder();
                if (border instanceof TitledBorder) {
                    TitledBorder titledBorder = (TitledBorder) border;
                    titledBorder.setTitleColor(borderTitleColor);
                }
                c.setBackground(bgColor);
                c.setForeground(fgColor);
            } else if (c instanceof JScrollPane) {
                Border border = ((JScrollPane) c).getBorder();
                if (border instanceof TitledBorder) {
                    ((TitledBorder) border).setTitleColor(fgColor);
                }
                c.setBackground(bgColor);
                c.setForeground(fgColor);
            } else {
                c.setBackground(bgColor);
                c.setForeground(fgColor);
            }
            if (c instanceof Container) {
                updateComponentColors((Container) c, bgColor, fgColor, buttonBgColor, buttonFgColor, borderTitleColor);
            }
        }

    }

    private void showOutputArea() {
        if (!centerPanel.isVisible()) {
            centerPanel.setVisible(true);
            revalidate();
            repaint();
        }
        outputArea.setText("");
    }

    private void selectDirectory() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int returnValue = fileChooser.showOpenDialog(this);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File selectedDirectory = fileChooser.getSelectedFile();
            directoryField.setText(selectedDirectory.getAbsolutePath());
        }
    }

    private void containerize() {
        showOutputArea();
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int returnValue = fileChooser.showOpenDialog(this);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File selectedFolder = fileChooser.getSelectedFile();
            String command = "sudo /usr/local/bin/osxiec -contain " + selectedFolder.getAbsolutePath() + " " + selectedFolder.getName() + ".bin";
            executeCommand(command);
        }
    }

    private void deployM() {
        showOutputArea();
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        int returnValue = fileChooser.showOpenDialog(this);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            String command = "sudo /usr/local/bin/osxiec -deploym " + selectedFile.getAbsolutePath();
            executeCommand(command);
        }
    }

    private void execute() {
        showOutputArea();
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
            public boolean accept(File f) {
                return f.isDirectory() || f.getName().toLowerCase().endsWith(".bin");
            }
            public String getDescription() {
                return "BIN Files (*.bin)";
            }
        });
        int returnValue = fileChooser.showOpenDialog(this);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            String filePath = selectedFile.getAbsolutePath();
            historyListModel.addElement(filePath);  // Add to history
            String command = "sudo /usr/local/bin/osxiec -execute " + filePath;
            executeCommand(command);
        }
    }

    private void runWithVlanNetwork() {
        showOutputArea();
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
            public boolean accept(File f) {
                return f.isDirectory() || f.getName().toLowerCase().endsWith(".bin");
            }
            public String getDescription() {
                return "BIN Files (*.bin)";
            }
        });
        int returnValue = fileChooser.showOpenDialog(this);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            String filePath = selectedFile.getAbsolutePath();

            JTextField networkNameField = new JTextField(20);
            JPanel panel = new JPanel(new GridLayout(1, 2));
            panel.add(new JLabel("Network Name:"));
            panel.add(networkNameField);

            int result = JOptionPane.showConfirmDialog(null, panel, "Run with VLAN Network", JOptionPane.OK_CANCEL_OPTION);
            if (result == JOptionPane.OK_OPTION) {
                String networkName = networkNameField.getText().trim();
                String command = "sudo /usr/local/bin/osxiec -run " + filePath + " " + networkName;
                executeCommand(command);
            }
        }
    }

    private void createVlanNetwork() {
        showOutputArea();
        JTextField networkNameField = new JTextField(20);
        JTextField vlanIdField = new JTextField(20);
        JPanel panel = new JPanel(new GridLayout(2, 2));
        panel.add(new JLabel("Network Name:"));
        panel.add(networkNameField);
        panel.add(new JLabel("VLAN ID:"));
        panel.add(vlanIdField);

        int result = JOptionPane.showConfirmDialog(null, panel, "Create VLAN Network", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            String networkName = networkNameField.getText().trim();
            String vlanId = vlanIdField.getText().trim();
            String command = "sudo /usr/local/bin/osxiec -network create " + networkName + " " + vlanId;
            executeCommand(command);
        }
    }

    private void executeFromHistory() {
        String selectedItem = historyList.getSelectedValue();
        if (selectedItem != null) {
            showOutputArea();
            String command = "sudo /usr/local/bin/osxiec -execute " + selectedItem;
            executeCommand(command);
        } else {
            JOptionPane.showMessageDialog(this, "Please select an item from the history.", "No Selection", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void clean() {
        showOutputArea();
        String command = "sudo /usr/local/bin/osxiec -clean";
        executeCommand(command);
    }

    private void deploy() {
        showOutputArea();
        JFileChooser fileChooser = new JFileChooser();
        int returnValue = fileChooser.showOpenDialog(this);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            String command = "sudo /usr/local/bin/osxiec -deploy " + selectedFile.getAbsolutePath();
            executeCommand(command);
        }
    }

    private void scan() {
        showOutputArea();
        JFileChooser fileChooser = new JFileChooser();
        int returnValue = fileChooser.showOpenDialog(this);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            String command = "sudo /usr/local/bin/osxiec -scan " + selectedFile.getAbsolutePath();
            executeCommand(command);
        }
    }


    private void openWebpage(String url) {
        try {
            Desktop.getDesktop().browse(new URI(url));
        } catch (Exception e) {
            showOutputArea();
            outputArea.append("Error opening webpage: " + e.getMessage() + "\n");
        }
    }

    private void executeCommand(String command) {
        new Thread(() -> {
            try {
                ProcessBuilder processBuilder = new ProcessBuilder("sudo", "-S", "sh", "-c", command);
                processBuilder.directory(new File(directoryField.getText()));
                processBuilder.redirectErrorStream(true);

                currentProcess = processBuilder.start();

                // Set up the input writer
                processInputWriter = new PrintWriter(new OutputStreamWriter(currentProcess.getOutputStream()), true);

                JPasswordField passwordField = new JPasswordField();
                int option = JOptionPane.showConfirmDialog(this, passwordField, "Enter your sudo password:", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

                if (option == JOptionPane.OK_OPTION) {
                    char[] password = passwordField.getPassword();

                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(currentProcess.getInputStream()))) {
                        processInputWriter.println(new String(password));
                        processInputWriter.flush();

                        String line;
                        while ((line = reader.readLine()) != null) {
                            final String outputLine = line;
                            SwingUtilities.invokeLater(() -> outputArea.append(outputLine + "\n"));
                        }
                    } finally {
                        java.util.Arrays.fill(password, ' ');
                    }

                    final int exitCode = currentProcess.waitFor();
                    SwingUtilities.invokeLater(() -> {
                        outputArea.append("Process exited with code: " + exitCode + "\n");
                        processInputWriter = null;
                    });
                } else {
                    currentProcess.destroy();
                    SwingUtilities.invokeLater(() -> outputArea.append("Command execution cancelled.\n"));
                }
            } catch (IOException | InterruptedException ex) {
                final String errorMessage = ex.getMessage();
                SwingUtilities.invokeLater(() -> outputArea.append("Error executing command: " + errorMessage + "\n"));
            }
        }).start();
    }

    private void saveHistory() {
        File tempFile = new File("history_tmp.txt");
        try (PrintWriter writer = new PrintWriter(new FileWriter(tempFile))) {
            for (int i = 0; i < historyListModel.size(); i++) {
                writer.println(historyListModel.get(i));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        String command = "sudo mv " + tempFile.getAbsolutePath() + " ~/.osxiec/history.txt";
        executeCommand(command);
    }

    private void loadHistory() {
        File historyFile = new File(System.getProperty("user.home"), ".osxiec/history.txt");
        if (!historyFile.exists()) return;
        try (BufferedReader reader = new BufferedReader(new FileReader(historyFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                historyListModel.addElement(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void closeProcess() {
        try {
            // Restart the application
            String javaBin = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
            String classpath = System.getProperty("java.class.path");
            String className = OsxiecApp.class.getCanonicalName();

            ProcessBuilder builder = new ProcessBuilder(javaBin, "-cp", classpath, className);
            builder.start();

            // Exit the current application
            System.exit(0);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            OsxiecApp app = new OsxiecApp();
            app.setVisible(true);
        });
    }
}
