package com.apkeditor.gui;

import java.awt.GraphicsEnvironment;
import java.util.Arrays;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        if (shouldPrintHelp(args)) {
            printHelp();
            return;
        }
        if (shouldPrintVersion(args)) {
            System.out.println("APKEditor GUI 1.0");
            return;
        }
        if (args.length > 0) {
            System.err.println("Unknown option: " + args[0]);
            printHelp();
            return;
        }

        if (GraphicsEnvironment.isHeadless()) {
            System.err.println("APKEditor GUI cannot open because this Java session has no desktop display. "
                    + "Copy the JAR to Windows, macOS, or Linux desktop and run it there.");
            return;
        }
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }

        SwingUtilities.invokeLater(() -> new MainFrame().setVisible(true));
    }

    private static boolean shouldPrintHelp(String[] args) {
        return Arrays.stream(args).anyMatch(arg -> "-h".equals(arg) || "--help".equals(arg)
                || "help".equalsIgnoreCase(arg));
    }

    private static boolean shouldPrintVersion(String[] args) {
        return Arrays.stream(args).anyMatch(arg -> "-v".equals(arg) || "--version".equals(arg));
    }

    private static void printHelp() {
        System.out.println("Usage: java -jar APKEditor-GUI.jar [options]");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  -h, --help        Show this help message");
        System.out.println("  -v, --version     Print the tool version");
        System.out.println();
        System.out.println("The GUI wraps the following APKEditor command families:");
        for (CliCommand command : CommandCatalog.all()) {
            System.out.println("  " + command.getCode() + "  " + command.getLabel());
        }
        System.out.println();
        System.out.println("Example:");
        System.out.println("  java -jar APKEditor-GUI.jar");
    }
}