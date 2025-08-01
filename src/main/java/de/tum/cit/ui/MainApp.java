package de.tum.cit.ui;

import de.tum.cit.ase.ares.api.policy.SecurityPolicyReaderAndDirector;
import de.tum.cit.ui.http.LogServer;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;

/**
 * Phobos bundle generator UI (filesystem edition).
 * <p>
 * Instead of typing short class‑path names, the user now selects concrete
 * files/directories from the host filesystem via standard JavaFX chooser
 * dialogs. All class‑path resolution logic has therefore been removed.
 */
public class MainApp extends Application {

    private LogServer logServer;

    private static final String NAMESPACE = "de/tum/cit/ase/ares/api/";

    private Path policyPath;
    private Path repoPath;
    private Path outputDir;

    private TextField tfPolicy;
    private TextField tfRepo;
    private TextField tfOutput;
    private Button     btnRun;
    private TextArea   log;

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
        Button btnChoosePolicy = new Button("Select policy …");
        btnChoosePolicy.setOnAction(ev -> choosePolicy(stage));

        Button btnChooseRepo   = new Button("Select repo dir …");
        btnChooseRepo.setOnAction(ev -> chooseRepoDir(stage));

        Button btnChooseOutput = new Button("Select output dir …");
        btnChooseOutput.setOnAction(ev -> chooseOutputDir(stage));

        tfPolicy = mkPathField();
        tfRepo   = mkPathField();
        tfOutput = mkPathField();

        log   = new TextArea();
        log.setEditable(false);
        btnRun = new Button("Create Phobos bundle");
        btnRun.setDisable(true);              // enabled once all selections made
        btnRun.setOnAction(ev -> runGeneration());

        GridPane gp = new GridPane();
        gp.setPadding(new Insets(10));
        gp.setVgap(6);
        gp.setHgap(8);

        int r = 0;
        gp.addRow(r++, new Label("Policy YAML:"),   btnChoosePolicy, tfPolicy);
        gp.addRow(r++, new Label("Repo directory:"), btnChooseRepo,   tfRepo);
        gp.addRow(r++, new Label("Output directory:"), btnChooseOutput, tfOutput);
        gp.add(btnRun, 2, r++);
        gp.add(log, 0, r, 3, 1);

        stage.setScene(new Scene(gp, 840, 460));
        stage.setTitle("Phobos bundle generator – filesystem edition");
        stage.show();
    }


    private void choosePolicy(Stage owner) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Select Security Policy YAML file");
        fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("YAML", "*.yaml", "*.yml"),
                new FileChooser.ExtensionFilter("All files", "*.*"));
        File chosen = fc.showOpenDialog(owner);
        if (chosen != null) {
            policyPath = chosen.toPath();
            tfPolicy.setText(policyPath.toString());
            maybeEnableRun();
        }
    }

    private void chooseRepoDir(Stage owner) {
        DirectoryChooser dc = new DirectoryChooser();
        dc.setTitle("Select student repository directory");
        File chosen = dc.showDialog(owner);
        if (chosen != null) {
            repoPath = chosen.toPath();
            tfRepo.setText(repoPath.toString());
            maybeEnableRun();
        }
    }

    private void chooseOutputDir(Stage owner) {
        DirectoryChooser dc = new DirectoryChooser();
        dc.setTitle("Choose output directory (will be created if missing)");
        File chosen = dc.showDialog(owner);
        if (chosen != null) {
            outputDir = chosen.toPath();
            tfOutput.setText(outputDir.toString());
            maybeEnableRun();
        }
    }

    private void maybeEnableRun() {
        btnRun.setDisable(policyPath == null || repoPath == null || outputDir == null);
    }


    private void runGeneration() {
        log.clear();
        try {
            Files.createDirectories(outputDir);

            SecurityPolicyReaderAndDirector.builder()
                    .securityPolicyFilePath(policyPath)
                    .projectFolderPath(repoPath)
                    .build()
                    .createTestCases()
                    .writeTestCases(outputDir);

            verifyArtefacts(outputDir);

        } catch (Exception ex) {
            log.appendText("ERROR: " + ex.getMessage() + "\n");
            ex.printStackTrace();
        }
    }


    private void verifyArtefacts(Path outAbs) throws IOException {
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


    private static TextField mkPathField() {
        TextField tf = new TextField();
        tf.setEditable(false);
        tf.setPrefColumnCount(40);
        tf.setStyle("-fx-font-size: 10px; -fx-text-fill: #666;");
        return tf;
    }

    @Override
    public void stop() {
        if (logServer != null) logServer.stop();
    }
}
