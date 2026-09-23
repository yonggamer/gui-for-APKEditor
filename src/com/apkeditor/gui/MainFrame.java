package com.apkeditor.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

public final class MainFrame extends JFrame {
    private final JTextField cliJarField = new JTextField();
    private final JTextField inputField = new JTextField();
    private final JTextField outputField = new JTextField();
    private final JTextField extraArgsField = new JTextField();
    private final JComboBox<CliCommand> commandBox = new JComboBox<>(CommandCatalog.all());
    private final JTextArea outputArea = new JTextArea();
    private final JButton runButton = new JButton("Run");

    public MainFrame() {
        super("APKEditor GUI");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(760, 520));
        setSize(900, 640);
        setLocationByPlatform(true);
        buildUi();
        detectCliJar();
    }

    private void buildUi() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(12, 12, 4, 12));
        addRow(form, 0, "APKEditor JAR", cliJarField, "Browse", this::chooseCliJar);
        addRow(form, 1, "Command", commandBox, null, null);
        addRow(form, 2, "Input", inputField, "Browse", this::chooseInput);
        addRow(form, 3, "Output", outputField, "Browse", this::chooseOutput);
        addRow(form, 4, "Extra options", extraArgsField, null, null);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton clearButton = new JButton("Clear output");
        JButton helpButton = new JButton("Command help");
        clearButton.addActionListener(event -> outputArea.setText(""));
        helpButton.addActionListener(event -> showCommandHelp());
        runButton.addActionListener(event -> runCommand());
        actions.add(clearButton);
        actions.add(helpButton);
        actions.add(runButton);

        outputArea.setEditable(false);
        outputArea.setLineWrap(true);
        outputArea.setWrapStyleWord(true);
        outputArea.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        JScrollPane outputScroll = new JScrollPane(outputArea);
        outputScroll.setBorder(BorderFactory.createTitledBorder("Command output"));

        add(form, BorderLayout.NORTH);
        add(outputScroll, BorderLayout.CENTER);
        add(actions, BorderLayout.SOUTH);
    }

    private void addRow(JPanel panel, int row, String label, java.awt.Component field,
            String buttonLabel, Runnable browseAction) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridy = row;
        constraints.insets = new Insets(4, 4, 4, 4);
        constraints.anchor = GridBagConstraints.LINE_START;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 0;
        panel.add(new JLabel(label), constraints);

        constraints.gridx = 1;
        constraints.weightx = 1;
        panel.add(field, constraints);
        if (buttonLabel != null) {
            constraints.gridx = 2;
            constraints.weightx = 0;
            JButton button = new JButton(buttonLabel);
            button.addActionListener(event -> browseAction.run());
            panel.add(button, constraints);
        }
    }

    private void chooseCliJar() {
        choosePath(cliJarField, JFileChooser.FILES_ONLY, "Select APKEditor JAR");
    }

    private void detectCliJar() {
        try {
            File location = new File(MainFrame.class.getProtectionDomain().getCodeSource()
                    .getLocation().toURI());
            File directory = location.isDirectory() ? location : location.getParentFile();
            if (directory == null) {
                return;
            }
            File[] candidates = directory.listFiles((dir, name) -> name.startsWith("APKEditor")
                    && name.endsWith(".jar") && !name.equals("APKEditor-GUI.jar"));
            if (candidates != null && candidates.length > 0) {
                Arrays.sort(candidates, (left, right) -> left.getName().compareToIgnoreCase(right.getName()));
                cliJarField.setText(candidates[0].getAbsolutePath());
            }
        } catch (Exception ignored) {
        }
    }

    private void chooseInput() {
        choosePath(inputField, JFileChooser.FILES_AND_DIRECTORIES, "Select input");
    }

    private void chooseOutput() {
        choosePath(outputField, JFileChooser.FILES_AND_DIRECTORIES, "Select output");
    }

    private void choosePath(JTextField target, int selectionMode, String title) {
        File selectedFile = FileChooserService.choose(this, selectionMode, title);
        if (selectedFile != null) {
            target.setText(selectedFile.getAbsolutePath());
        }
    }

    private void runCommand() {
        String cliJar = cliJarField.getText().trim();
        String input = inputField.getText().trim();
        if (cliJar.isEmpty() || input.isEmpty()) {
            JOptionPane.showMessageDialog(this, "APKEditor JAR and input are required.",
                    "Missing input", JOptionPane.WARNING_MESSAGE);
            return;
        }

        File cliJarFile = new File(cliJar);
        if (!cliJarFile.isFile() || !cliJarFile.getName().startsWith("APKEditor")
                || !cliJarFile.getName().endsWith(".jar")) {
            JOptionPane.showMessageDialog(this, "Select an external APKEditor*.jar file.",
                    "Invalid APKEditor JAR", JOptionPane.ERROR_MESSAGE);
            return;
        }
        File inputFile = new File(input);
        if (!inputFile.exists()) {
            JOptionPane.showMessageDialog(this, "The selected input does not exist.",
                    "Invalid input", JOptionPane.ERROR_MESSAGE);
            return;
        }

        final List<String> command = createCommand(cliJarFile, inputFile);
        outputArea.append("$ " + String.join(" ", command) + "\n");
        runButton.setEnabled(false);
        Thread worker = new Thread(() -> executeCommand(command), "apkeditor-cli-process");
        worker.start();
    }

    private void executeCommand(List<String> command) {
        try {
            Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                    process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    appendOutput(line + "\n");
                }
            }
            appendOutput("\nProcess finished with exit code " + process.waitFor() + "\n");
        } catch (IOException exception) {
            appendOutput("\nCould not start APKEditor: " + exception.getMessage() + "\n");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            appendOutput("\nProcess interrupted.\n");
        } finally {
            SwingUtilities.invokeLater(() -> runButton.setEnabled(true));
        }
    }

    private List<String> createCommand(File cliJar, File inputFile) {
        List<String> command = new ArrayList<>();
        command.add(new File(System.getProperty("java.home"), "bin/java").getPath());
        command.add("-jar");
        command.add(cliJar.getAbsolutePath());
        command.add(((CliCommand) commandBox.getSelectedItem()).getCode());
        command.add("-i");
        command.add(inputFile.getAbsolutePath());
        String output = outputField.getText().trim();
        if (!output.isEmpty()) {
            command.add("-o");
            command.add(output);
        }
        String extraOptions = extraArgsField.getText().trim();
        if (!extraOptions.isEmpty()) {
            command.addAll(ArgumentParser.split(extraOptions));
        }
        return command;
    }

    private void showCommandHelp() {
        String resourceName = "/" + helpFileName();
        try (InputStream stream = MainFrame.class.getResourceAsStream(resourceName)) {
            if (stream == null) {
                outputArea.setText("Help file not found: " + resourceName + "\n");
                return;
            }
            StringBuilder help = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    help.append(line).append('\n');
                }
            }
            outputArea.setText(help.toString());
            outputArea.setCaretPosition(0);
        } catch (IOException exception) {
            outputArea.setText("Could not read command help: " + exception.getMessage() + "\n");
        }
    }

    private String helpFileName() {
        String code = ((CliCommand) commandBox.getSelectedItem()).getCode();
        switch (code) {
            case "b":
                return "build-help.txt";
            case "d":
                return "decompile-help.txt";
            case "info":
                return "info-help.txt";
            case "m":
                return "merge-help.txt";
            case "p":
                return "protect-help.txt";
            case "x":
                return "refactor-help.txt";
            default:
                return "help.txt";
        }
    }

    private void appendOutput(String text) {
        SwingUtilities.invokeLater(() -> outputArea.append(text));
    }
}
