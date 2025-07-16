package de.tum.cit.ui;

import de.tum.cit.ase.ares.api.policy.SecurityPolicyReaderAndDirector;
import de.tum.cit.ui.http.LogServer;
import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;

/**
 * Phobos bundle generator UI.
 * User types short names; classpath bases are prepended automatically.
 * Output directory (third field) is created at the classpath root (not inside the repo).
 */
public class MainApp extends Application {

    private LogServer logServer;

    /** Where Ares places generated artefacts relative to the output directory. */
    private static final String NAMESPACE = "de/tum/cit/ase/ares/api/";

    /* Classpath bases (no leading slash) */
    private static final String POLICY_BASE = "policyfiles/";
    private static final String REPO_BASE   = "example-student-test-repos/";

    /* Short-name defaults */
    private static final String DEFAULT_POLICY_SHORT = "phobos_access_test_policy.yaml";
    private static final String DEFAULT_REPO_SHORT   = "test-phobos";
    private static final String DEFAULT_OUTPUT_SHORT = "tests";  // user‑chosen output dir at classpath root

    @Override
    public void init() throws IOException {
        int port = getParameters().getUnnamed().stream()
                .filter(s -> s.startsWith("--logPort="))
                .mapToInt(s -> Integer.parseInt(s.substring(10)))
                .findFirst().orElse(9001);
        logServer = LogServer.start(port);
    }

    @Override
    public void start(Stage stage) {
        /* short-name inputs */
        TextField tfPolicyShort = new TextField(DEFAULT_POLICY_SHORT);
        TextField tfRepoShort   = new TextField(DEFAULT_REPO_SHORT);
        TextField tfOutShort    = new TextField(DEFAULT_OUTPUT_SHORT);

        /* resolved full paths (read-only labels) */
        Label lblPolicyFull = new Label();
        Label lblRepoFull   = new Label();
        Label lblOutFull    = new Label();
        styleResolvedLabel(lblPolicyFull);
        styleResolvedLabel(lblRepoFull);
        styleResolvedLabel(lblOutFull);

        TextArea log = new TextArea();
        log.setEditable(false);

        Button btnRun = new Button("Create Phobos bundle");

        /* layout */
        GridPane gp = new GridPane();
        gp.setPadding(new Insets(10));
        gp.setVgap(6);
        gp.setHgap(8);

        int r = 0;
        gp.addRow(r++, new Label("Policy file name:"), tfPolicyShort);
        gp.add(lblPolicyFull, 1, r++);
        gp.addRow(r++, new Label("Repo dir (classpath):"), tfRepoShort);
        gp.add(lblRepoFull, 1, r++);
        gp.addRow(r++, new Label("Output dir @ classpath root:"), tfOutShort);
        gp.add(lblOutFull, 1, r++);
        gp.add(btnRun, 1, r++);
        gp.add(log, 0, r, 2, 1);

        /* live resolution of labels */
        ChangeListener<String> updater = (obs, o, n) -> {
            try {
                Path policy = resolvePolicy(tfPolicyShort.getText().trim());
                lblPolicyFull.setText(policy.toString());
            } catch (Exception ex) {
                lblPolicyFull.setText(warn("not found"));
            }
            try {
                Path repo = resolveRepo(tfRepoShort.getText().trim());
                lblRepoFull.setText(repo.toString());
            } catch (Exception ex) {
                lblRepoFull.setText(warn("not found"));
            }
            try {
                Path out = resolveOutput(tfOutShort.getText().trim());
                lblOutFull.setText(out.toString());
            } catch (Exception ex) {
                lblOutFull.setText(warn("not found / unwritable"));
            }
        };
        tfPolicyShort.textProperty().addListener(updater);
        tfRepoShort.textProperty().addListener(updater);
        tfOutShort.textProperty().addListener(updater);
        updater.changed(null, null, null); // initial fill

        /* action */
        btnRun.setOnAction(ev -> {
            log.clear();
            try {
                Path policy = dropRoot(resolvePolicy(tfPolicyShort.getText().trim()));
                Path repo   = dropRoot(resolveRepo(tfRepoShort.getText().trim()));
                Path outAbs = dropRoot(resolveOutput(tfOutShort.getText().trim()));
                Files.createDirectories(outAbs);  // ensure exists


                // Call into Ares: projectFolderPath = resolved repo; outputPath = resolved output root
                SecurityPolicyReaderAndDirector.builder()
                        .securityPolicyFilePath(policy)
                        .projectFolderPath(repo)
                        .build()
                        .createTestCases()
                        .writeTestCases(outAbs);

                // Verify some key artefacts
                verifyArtefacts(outAbs, log);

            } catch (Exception ex) {
                log.appendText("ERROR: " + ex.getMessage() + "\n");
                ex.printStackTrace();
            }
        });

        stage.setScene(new Scene(gp, 760, 420));
        stage.setTitle("Phobos bundle generator");
        stage.show();
    }

    /* --- resolution helpers ------------------------------------------------ */

    private static Path resolvePolicy(String shortName) throws IOException {
        if (shortName.isEmpty()) shortName = DEFAULT_POLICY_SHORT;
        return Resolver.resolveClassPathFile("classpath:" + POLICY_BASE + shortName).toAbsolutePath();
    }

    private static Path resolveRepo(String shortDir) throws IOException {
        if (shortDir.isEmpty()) shortDir = DEFAULT_REPO_SHORT;
        return Resolver.resolveClassPathDirectory("classpath:" + REPO_BASE + shortDir).toAbsolutePath();
    }

    /**
     * Resolve a user-supplied output directory name to an absolute, writable path
     * at the *classpath root* (exploded resources if in dev; temp if running from JAR).
     */
    private static Path resolveOutput(String outShort) throws IOException {
        if (outShort == null || outShort.isBlank()) outShort = DEFAULT_OUTPUT_SHORT;

        Path root = locateClasspathWritableRoot();
        return root.resolve(outShort).toAbsolutePath();
    }

    /** If the path is absolute on drive C:, drop the "C:" root and return a relative path. */
    private static Path dropRoot(Path p) {
        if (!p.isAbsolute()) {
            return p;
        }
        String s = p.toString();
        if (s.length() >= 2 && s.charAt(1) == ':' && Character.isLetter(s.charAt(0))) {
            // drop the drive + colon
            s = s.substring(2);
            // ensure it starts with a slash/backslash so we keep "rooted" semantics
            if (!(s.startsWith("\\") || s.startsWith("/"))) {
                s = FileSystems.getDefault().getSeparator() + s;
            }
            return Paths.get(s);
        }
        return p;
    }


    /**
     * Locate a writable root corresponding to the top of the application's resources.
     * In dev/IDE runs (exploded), this is typically .../target/classes.
     * In a packaged JAR (read-only), we fall back to a temp directory.
     */
    private static Path locateClasspathWritableRoot() throws IOException {
        URL url = MainApp.class.getResource("/");
        if (url != null && "file".equals(url.getProtocol())) {
            try {
                return Paths.get(url.toURI());
            } catch (URISyntaxException e) {
                throw new IOException(e);
            }
        }
        // JAR or unknown protocol: use temp root
        Path tmp = Files.createTempDirectory("ares-output-");
        tmp.toFile().deleteOnExit();
        return tmp;
    }

    /* --- artefact check ---------------------------------------------------- */

    private static void verifyArtefacts(Path outAbs, TextArea log) throws IOException {
        String[] mustExist = {
                "phobos/BasePhobos.cfg",
                "phobos/TailPhobos.cfg",
                "phobos/allowedList.cfg",
                "phobos/phobos_wrapper.sh",
                "phobos/libnetblocker.so"
        };
        boolean ok = true;
        for (String f : mustExist) {
            Path p = outAbs.resolve(NAMESPACE + f);
            boolean present = Files.exists(p);
            log.appendText("Path " + p + (present ? " exists\n" : " does not exist\n"));
            ok &= present;
        }
        log.appendText(ok ? "\nAll Phobos artefacts present\n"
                : "\nMissing artefacts – see above\n");
    }

    /* --- UI label style helpers ------------------------------------------- */

    private static void styleResolvedLabel(Label lbl) {
        lbl.setWrapText(true);
        lbl.setStyle("-fx-font-size: 10px; -fx-text-fill: #666;");
    }

    private static String warn(String msg) {
        return msg;
    }

    @Override
    public void stop() {
        if (logServer != null) logServer.stop();
    }
}
