package hu.gov.nav.xsdparsertool.web;

import hu.gov.nav.xsdparsertool.core.support.SecureFileOperations;
import hu.gov.nav.xsdparsertool.core.support.ExceptionSafeOperations;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.ScrollPaneConstants;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JWindow;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.Window;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A web modul alkalmazási területének közös alkalmazási típusa.
 *
 * <p>A {@code StartupSplashSupport} osztály a web modul alkalmazási területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
 */
public final class StartupSplashSupport {

    private static final Logger log = LoggerFactory.getLogger(StartupSplashSupport.class);

    private static final String APPLICATION_NAME = "M2M XML EDITOR";
    private static final String LOGO_RESOURCE = "/static/images/SET_logo.png";
    private static final String BACKGROUND_RESOURCE = "/static/images/login-background-light.png";
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter FILE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final int MAX_LOG_LINES = 200;

    private static final AtomicBoolean ENABLED = new AtomicBoolean(false);
    private static final AtomicBoolean CLOSING = new AtomicBoolean(false);
    private static final Deque<String> LOG_LINES = new ArrayDeque<>();

    private static volatile JWindow window;
    private static volatile JLabel statusLabel;
    private static volatile JTextArea logTextArea;
    private static volatile JProgressBar progressBar;
    private static volatile JPanel buttonPanel;
    private static volatile SplashLogAppender logAppender;
    private static volatile Path automaticErrorLogPath;

    /**
     * Létrehozza a {@code StartupSplashSupport} példányt, és eltárolja a működéshez szükséges együttműködő komponenseket.
     *
     * <p>A konstruktor nem indít önálló üzleti folyamatot; a kapott függőségeket a későbbi kérések és szolgáltatási műveletek használják.</p>
     */
    private StartupSplashSupport() {
    }

    /**
     * A {@code initialize} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     */
    public static void initialize() {
        if (Boolean.getBoolean(DesktopIntegrationSettings.LEGACY_DISABLED_PROPERTY)) {
            log.info("Startup splash is disabled by system property.");
            return;
        }
        if (GraphicsEnvironment.isHeadless()) {
            log.info("Startup splash is skipped because the environment is headless.");
            return;
        }
        if (!ENABLED.compareAndSet(false, true)) {
            return;
        }

        runOnEdt(() -> {
            try {
                createAndShowWindow();
                installLogAppender();
                appendLogLine("INFO", "Indítás előkészítése...");
            } catch (Exception ex) {
                log.warn("Could not initialize startup splash.", ex);
                ENABLED.set(false);
            }
        });
    }

    /**
     * A {@code updateStatus} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
     *
     * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param text a művelet bemeneti {@code text} értéke
     */
    public static void updateStatus(String text) {
        if (!ENABLED.get() || CLOSING.get()) {
            return;
        }
        appendLogLine("INFO", text);
        runOnEdt(() -> {
            if (statusLabel != null) {
                statusLabel.setText(text);
            }
        });
    }

    /**
     * A {@code showStartupError} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param message a művelet bemeneti {@code message} értéke
     * @param throwable a művelet bemeneti {@code throwable} értéke
     */
    public static void showStartupError(String message, Throwable throwable) {
        if (!ENABLED.get()) {
            if (!GraphicsEnvironment.isHeadless()) {
                initialize();
            }
        }
        appendLogLine("ERROR", message);
        if (throwable != null) {
            appendLogLine("ERROR", throwable.getClass().getSimpleName() + ": " + safeMessage(throwable.getMessage()));
            appendStackTrace(throwable);
        }
        automaticErrorLogPath = writeAutomaticErrorLog();
        runOnEdt(() -> {
            if (statusLabel != null) {
                statusLabel.setText(message + " A részletek a naplóban láthatók.");
                if (automaticErrorLogPath != null) {
                    statusLabel.setToolTipText("Automatikus napló: " + automaticErrorLogPath.toAbsolutePath());
                }
                statusLabel.setForeground(new Color(180, 35, 24));
            }
            if (progressBar != null) {
                progressBar.setIndeterminate(false);
                progressBar.setValue(0);
            }
            if (buttonPanel != null && buttonPanel.getComponentCount() == 0) {
                JButton copyButton = createActionButton("Napló másolása", false);
                copyButton.addActionListener(event -> copyLogToClipboard());

                JButton saveButton = createActionButton("Napló mentése...", true);
                saveButton.addActionListener(event -> saveLogWithChooser());

                JButton exitButton = createActionButton("Kilépés", false);
                exitButton.addActionListener(event -> System.exit(1));

                buttonPanel.add(copyButton);
                buttonPanel.add(saveButton);
                buttonPanel.add(exitButton);
                buttonPanel.revalidate();
                buttonPanel.repaint();
            }
            Window localWindow = window;
            if (localWindow != null) {
                localWindow.setAlwaysOnTop(true);
                localWindow.toFront();
            }
        });
    }

    /**
     * A {@code close} művelet lezárja, felszabadítja vagy eltávolítja a kijelölt erőforrást a vonatkozó szabályok szerint.
     *
     * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     */
    public static void close() {
        if (!ENABLED.get() || !CLOSING.compareAndSet(false, true)) {
            return;
        }
        uninstallLogAppender();
        runOnEdt(() -> {
            Window localWindow = window;
            window = null;
            statusLabel = null;
            logTextArea = null;
            progressBar = null;
            buttonPanel = null;
            ENABLED.set(false);
            CLOSING.set(false);

            if (localWindow != null) {
                localWindow.setVisible(false);
                localWindow.dispose();
            }
        });
    }

    /**
     * A {@code createAndShowWindow} művelet létrehozza vagy tartósítja a kért állapotváltozást.
     *
     * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     */
    private static void createAndShowWindow() {
        JWindow splashWindow = new JWindow();

        JPanel root = new BackgroundPanel(loadBackgroundImage());
        root.setLayout(new BorderLayout());
        root.setBorder(BorderFactory.createEmptyBorder(30, 34, 30, 34));

        JPanel card = new RoundedCardPanel();
        card.setLayout(new BorderLayout(0, 18));
        card.setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));
        root.add(card, BorderLayout.CENTER);

        JPanel headerPanel = new JPanel(new BorderLayout(22, 0));
        headerPanel.setOpaque(false);

        JLabel logoLabel = new JLabel();
        Image logoImage = loadLogoImage();
        if (logoImage != null) {
            logoLabel.setIcon(new ImageIcon(logoImage));
            logoLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 4));
            headerPanel.add(logoLabel, BorderLayout.WEST);
        }

        JPanel brandPanel = new JPanel();
        brandPanel.setOpaque(false);
        brandPanel.setLayout(new BoxLayout(brandPanel, BoxLayout.Y_AXIS));

        JLabel titleLabel = new JLabel(APPLICATION_NAME);
        titleLabel.setAlignmentX(0.0f);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 24f));
        titleLabel.setForeground(new Color(9, 47, 99));
        brandPanel.add(titleLabel);
        brandPanel.add(Box.createRigidArea(new Dimension(0, 5)));

        JLabel subtitleLabel = new JLabel("Az alkalmazás szolgáltatásainak előkészítése");
        subtitleLabel.setAlignmentX(0.0f);
        subtitleLabel.setForeground(new Color(83, 98, 122));
        subtitleLabel.setFont(subtitleLabel.getFont().deriveFont(Font.PLAIN, 13f));
        brandPanel.add(subtitleLabel);
        brandPanel.add(Box.createRigidArea(new Dimension(0, 16)));

        JLabel currentStatusLabel = new JLabel("Inicializálás...");
        currentStatusLabel.setAlignmentX(0.0f);
        currentStatusLabel.setForeground(new Color(15, 39, 70));
        currentStatusLabel.setFont(currentStatusLabel.getFont().deriveFont(Font.BOLD, 13f));
        brandPanel.add(currentStatusLabel);
        brandPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        JProgressBar currentProgressBar = new JProgressBar();
        currentProgressBar.setIndeterminate(true);
        currentProgressBar.setAlignmentX(0.0f);
        currentProgressBar.setPreferredSize(new Dimension(520, 10));
        currentProgressBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 10));
        currentProgressBar.setBorderPainted(false);
        currentProgressBar.setBackground(new Color(226, 235, 246));
        currentProgressBar.setForeground(new Color(11, 99, 206));
        brandPanel.add(currentProgressBar);

        headerPanel.add(brandPanel, BorderLayout.CENTER);
        card.add(headerPanel, BorderLayout.NORTH);

        JPanel logPanel = new JPanel(new BorderLayout(0, 9));
        logPanel.setBackground(new Color(248, 251, 255));
        logPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(201, 215, 236)),
                BorderFactory.createEmptyBorder(12, 14, 14, 14)
        ));

        JLabel logTitleLabel = new JLabel("Indítási napló");
        logTitleLabel.setForeground(new Color(15, 39, 70));
        logTitleLabel.setFont(logTitleLabel.getFont().deriveFont(Font.BOLD, 12f));
        logPanel.add(logTitleLabel, BorderLayout.NORTH);

        JTextArea currentLogTextArea = new JTextArea(13, 88);
        currentLogTextArea.setEditable(false);
        currentLogTextArea.setLineWrap(true);
        currentLogTextArea.setWrapStyleWord(true);
        currentLogTextArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        currentLogTextArea.setForeground(new Color(40, 48, 58));
        currentLogTextArea.setBackground(new Color(252, 253, 255));
        currentLogTextArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        currentLogTextArea.setMargin(new java.awt.Insets(2, 2, 2, 2));

        JScrollPane scrollPane = new JScrollPane(currentLogTextArea);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(220, 228, 239)));
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setPreferredSize(new Dimension(780, 248));
        logPanel.add(scrollPane, BorderLayout.CENTER);
        card.add(logPanel, BorderLayout.CENTER);

        JPanel currentButtonPanel = new JPanel();
        currentButtonPanel.setOpaque(false);
        currentButtonPanel.setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));
        card.add(currentButtonPanel, BorderLayout.SOUTH);

        splashWindow.setContentPane(root);
        splashWindow.pack();

        int width = Math.max(splashWindow.getWidth(), 900);
        int height = Math.max(splashWindow.getHeight(), 600);

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        width = Math.min(width, Math.max(760, screenSize.width - 80));
        height = Math.min(height, Math.max(520, screenSize.height - 80));
        splashWindow.setSize(width, height);

        int x = (screenSize.width - width) / 2;
        int y = (screenSize.height - height) / 2;
        splashWindow.setLocation(x, y);

        splashWindow.setAlwaysOnTop(true);
        splashWindow.setVisible(true);

        window = splashWindow;
        statusLabel = currentStatusLabel;
        logTextArea = currentLogTextArea;
        progressBar = currentProgressBar;
        buttonPanel = currentButtonPanel;
        refreshLogArea();
    }

    /**
     * A {@code createActionButton} művelet létrehozza vagy tartósítja a kért állapotváltozást.
     *
     * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param text a művelet bemeneti {@code text} értéke
     * @param primary a művelet bemeneti {@code primary} értéke
     * @return a művelet feldolgozási eredménye
     */
    private static JButton createActionButton(String text, boolean primary) {
        JButton button = new JButton(text);
        button.setFocusPainted(false);
        button.setFont(button.getFont().deriveFont(Font.BOLD, 12f));
        button.setForeground(primary ? Color.WHITE : new Color(15, 39, 70));
        button.setBackground(primary ? new Color(11, 99, 206) : new Color(239, 245, 252));
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(primary ? new Color(11, 99, 206) : new Color(190, 208, 232)),
                BorderFactory.createEmptyBorder(8, 14, 8, 14)
        ));
        button.setOpaque(true);
        return button;
    }

    /**
     * A {@code loadBackgroundImage} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @return a feloldott vagy lekért érték
     */
    private static Image loadBackgroundImage() {
        try (InputStream inputStream = StartupSplashSupport.class.getResourceAsStream(BACKGROUND_RESOURCE)) {
            if (inputStream == null) {
                return null;
            }
            return ImageIO.read(inputStream);
        } catch (IOException ex) {
            log.warn("Could not load startup splash background: {}", BACKGROUND_RESOURCE, ex);
            return null;
        }
    }

    /**
     * A {@code loadLogoImage} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @return a feloldott vagy lekért érték
     */
    private static Image loadLogoImage() {
        try (InputStream inputStream = StartupSplashSupport.class.getResourceAsStream(LOGO_RESOURCE)) {
            if (inputStream == null) {
                return null;
            }
            Image image = ImageIO.read(inputStream);
            if (image == null) {
                return null;
            }
            return image.getScaledInstance(190, 73, Image.SCALE_SMOOTH);
        } catch (IOException ex) {
            log.warn("Could not load startup splash logo: {}", LOGO_RESOURCE, ex);
            return null;
        }
    }

    /**
     * A {@code appendStackTrace} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param throwable a művelet bemeneti {@code throwable} értéke
     */
    private static void appendStackTrace(Throwable throwable) {
        StringWriter stringWriter = new StringWriter();
        throwable.printStackTrace(new PrintWriter(stringWriter));
        for (String line : stringWriter.toString().split("\\R")) {
            appendLogLine("ERROR", line);
        }
    }

    /**
     * A {@code safeMessage} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param text a művelet bemeneti {@code text} értéke
     * @return a művelet feldolgozási eredménye
     */
    private static String safeMessage(String text) {
        return text == null || text.isBlank() ? "nincs részletes hibaüzenet" : text;
    }

    /**
     * A {@code appendLogLine} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param level a művelet bemeneti {@code level} értéke
     * @param text a művelet bemeneti {@code text} értéke
     */
    private static void appendLogLine(String level, String text) {
        String line = TIME_FORMATTER.format(LocalTime.now()) + " " + level + " " + safeMessage(text);
        synchronized (LOG_LINES) {
            LOG_LINES.addLast(line);
            while (LOG_LINES.size() > MAX_LOG_LINES) {
                LOG_LINES.removeFirst();
            }
        }
        refreshLogArea();
    }

    /**
     * A {@code refreshLogArea} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     */
    private static void refreshLogArea() {
        if (!ENABLED.get()) {
            return;
        }
        runOnEdt(() -> {
            JTextArea localLogTextArea = logTextArea;
            if (localLogTextArea == null) {
                return;
            }
            StringBuilder builder = new StringBuilder();
            synchronized (LOG_LINES) {
                for (String line : LOG_LINES) {
                    builder.append(line).append(System.lineSeparator());
                }
            }
            localLogTextArea.setText(builder.toString());
            localLogTextArea.setCaretPosition(localLogTextArea.getDocument().getLength());
        });
    }

    /**
     * A {@code currentLogText} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @return a művelet feldolgozási eredménye
     */
    private static String currentLogText() {
        StringBuilder builder = new StringBuilder();
        synchronized (LOG_LINES) {
            for (String line : LOG_LINES) {
                builder.append(line).append(System.lineSeparator());
            }
        }
        return builder.toString();
    }

    /**
     * A {@code writeAutomaticErrorLog} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @return a művelet feldolgozási eredménye
     */
    private static Path writeAutomaticErrorLog() {
        String fileName = "startup-error-" + FILE_TIME_FORMATTER.format(LocalDateTime.now()) + ".log";
        String localAppData = System.getenv("LOCALAPPDATA");
        Path primaryBase = localAppData != null && !localAppData.isBlank()
                ? Path.of(localAppData, "M2M-XML-EDITOR", "logs")
                : Path.of(ExceptionSafeOperations.systemProperty("user.home"), ".m2m-xml-editor", "logs");
        try {
            return writeAutomaticErrorLog(primaryBase, fileName);
        } catch (Exception primaryEx) {
            Path fallbackBase = Path.of(ExceptionSafeOperations.systemProperty("java.io.tmpdir", "."),
                    "M2M-XML-EDITOR", "logs");
            try {
                Path fallback = writeAutomaticErrorLog(fallbackBase, fileName);
                log.warn("Could not write startup error log to {}. Used fallback {}.",
                        primaryBase, fallback, primaryEx);
                return fallback;
            } catch (Exception fallbackEx) {
                primaryEx.addSuppressed(fallbackEx);
                log.warn("Could not write automatic startup error log.", primaryEx);
                return null;
            }
        }
    }

    /**
     * A {@code writeAutomaticErrorLog} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param logDirectory a művelet bemeneti {@code logDirectory} értéke
     * @param fileName a feldolgozásban részt vevő fájl vagy elérési út
     * @return a művelet feldolgozási eredménye
     * @throws java.io.IOException ha a művelet a deklarált technikai vagy üzleti feltétel miatt nem hajtható végre
     */
    private static Path writeAutomaticErrorLog(Path logDirectory, String fileName) throws java.io.IOException {
        ExceptionSafeOperations.createDirectories(logDirectory);
        Path target = logDirectory.resolve(fileName);
        SecureFileOperations.writePrivateString(target, currentLogText(), StandardCharsets.UTF_8);
        return target;
    }

    /**
     * A {@code copyLogToClipboard} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     */
    private static void copyLogToClipboard() {
        try {
            Toolkit.getDefaultToolkit().getSystemClipboard()
                    .setContents(new StringSelection(currentLogText()), null);
            if (statusLabel != null) {
                statusLabel.setText("A napló a vágólapra került.");
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(window,
                    "A napló nem másolható a vágólapra: " + safeMessage(ex.getMessage()),
                    APPLICATION_NAME, JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * A {@code saveLogWithChooser} művelet létrehozza vagy tartósítja a kért állapotváltozást.
     *
     * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     */
    private static void saveLogWithChooser() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Indítási napló mentése");
        chooser.setSelectedFile(new java.io.File(
                "m2m-xml-editor-startup-" + FILE_TIME_FORMATTER.format(LocalDateTime.now()) + ".log"));
        if (chooser.showSaveDialog(window) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        try {
            Path target = chooser.getSelectedFile().toPath();
            SecureFileOperations.writePrivateString(target, currentLogText(), StandardCharsets.UTF_8);
            if (statusLabel != null) {
                statusLabel.setText("A napló mentése sikerült: " + target.toAbsolutePath());
            }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(window,
                    "A napló mentése sikertelen: " + safeMessage(ex.getMessage()),
                    APPLICATION_NAME, JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * A {@code installLogAppender} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     */
    private static void installLogAppender() {
        try {
            org.slf4j.ILoggerFactory loggerFactory = LoggerFactory.getILoggerFactory();
            if (!(loggerFactory instanceof LoggerContext loggerContext)) {
                return;
            }
            ch.qos.logback.classic.Logger rootLogger = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);
            if (rootLogger.getAppender("SPLASH_LOG") != null) {
                return;
            }
            SplashLogAppender appender = new SplashLogAppender();
            appender.setContext(loggerContext);
            appender.setName("SPLASH_LOG");
            appender.start();
            rootLogger.addAppender(appender);
            logAppender = appender;
        } catch (Exception ex) {
            log.warn("Could not attach startup splash log appender.", ex);
        }
    }

    /**
     * A {@code uninstallLogAppender} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     */
    private static void uninstallLogAppender() {
        try {
            org.slf4j.ILoggerFactory loggerFactory = LoggerFactory.getILoggerFactory();
            if (!(loggerFactory instanceof LoggerContext loggerContext)) {
                return;
            }
            ch.qos.logback.classic.Logger rootLogger = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);
            SplashLogAppender appender = logAppender;
            if (appender != null) {
                rootLogger.detachAppender(appender);
                appender.stop();
                logAppender = null;
            }
        } catch (Exception ex) {
            log.warn("Could not detach startup splash log appender.", ex);
        }
    }

    /**
     * A {@code runOnEdt} művelet elindítja vagy végrehajtja a kapcsolódó alkalmazási folyamatot.
     *
     * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param runnable a művelet bemeneti {@code runnable} értéke
     */
    private static void runOnEdt(Runnable runnable) {
        if (SwingUtilities.isEventDispatchThread()) {
            runnable.run();
        } else {
            SwingUtilities.invokeLater(runnable);
        }
    }

    /**
     * A web modul alkalmazási területének közös alkalmazási típusa.
     *
     * <p>A {@code SplashLogAppender} osztály a web modul alkalmazási területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
     */
    private static final class SplashLogAppender extends AppenderBase<ILoggingEvent> {
        /**
         * A {@code append} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
         *
         * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
         * @param eventObject a művelet bemeneti {@code eventObject} értéke
         */
        @Override
        protected void append(ILoggingEvent eventObject) {
            if (eventObject == null || !ENABLED.get() || CLOSING.get()) {
                return;
            }
            Level level = eventObject.getLevel();
            if (level != null && level.isGreaterOrEqual(Level.INFO)) {
                appendLogLine(level.toString(), eventObject.getFormattedMessage());
            }
        }
    }
    /**
     * A web modul alkalmazási területének közös alkalmazási típusa.
     *
     * <p>A {@code BackgroundPanel} osztály a web modul alkalmazási területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
     */
    private static final class BackgroundPanel extends JPanel {
        private final Image backgroundImage;

        /**
         * Létrehozza a {@code BackgroundPanel} példányt, és eltárolja a működéshez szükséges együttműködő komponenseket.
         *
         * <p>A konstruktor nem indít önálló üzleti folyamatot; a kapott függőségeket a későbbi kérések és szolgáltatási műveletek használják.</p>
         * @param backgroundImage a művelet bemeneti {@code backgroundImage} értéke
         */
        private BackgroundPanel(Image backgroundImage) {
            this.backgroundImage = backgroundImage;
            setOpaque(true);
            setBackground(new Color(238, 243, 248));
        }

        /**
         * A {@code paintComponent} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
         *
         * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
         * @param graphics a művelet bemeneti {@code graphics} értéke
         */
        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            if (backgroundImage != null) {
                Graphics2D graphics2D = (Graphics2D) graphics.create();
                try {
                    graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                    graphics2D.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                    graphics2D.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
                } finally {
                    graphics2D.dispose();
                }
            }
        }
    }

    /**
     * A web modul alkalmazási területének közös alkalmazási típusa.
     *
     * <p>A {@code RoundedCardPanel} osztály a web modul alkalmazási területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
     */
    private static final class RoundedCardPanel extends JPanel {
        /**
         * Létrehozza a {@code RoundedCardPanel} példányt, és eltárolja a működéshez szükséges együttműködő komponenseket.
         *
         * <p>A konstruktor nem indít önálló üzleti folyamatot; a kapott függőségeket a későbbi kérések és szolgáltatási műveletek használják.</p>
         */
        private RoundedCardPanel() {
            setOpaque(false);
        }

        /**
         * A {@code paintComponent} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
         *
         * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
         * @param graphics a művelet bemeneti {@code graphics} értéke
         */
        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D graphics2D = (Graphics2D) graphics.create();
            try {
                graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                graphics2D.setColor(new Color(15, 23, 42, 28));
                graphics2D.fillRoundRect(5, 7, Math.max(0, getWidth() - 10), Math.max(0, getHeight() - 12), 28, 28);
                graphics2D.setColor(new Color(255, 255, 255, 246));
                graphics2D.fillRoundRect(1, 1, Math.max(0, getWidth() - 4), Math.max(0, getHeight() - 5), 28, 28);
                graphics2D.setColor(new Color(190, 208, 232));
                graphics2D.drawRoundRect(1, 1, Math.max(0, getWidth() - 4), Math.max(0, getHeight() - 5), 28, 28);
            } finally {
                graphics2D.dispose();
            }
            super.paintComponent(graphics);
        }
    }

}
