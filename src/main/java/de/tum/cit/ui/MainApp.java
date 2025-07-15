package de.tum.cit.ui;

import de.tum.cit.ase.ares.api.policy.SecurityPolicyReaderAndDirector;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

import java.nio.file.*;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) {
        /* ── input fields ──────────────────────────────────────────────── */
        TextField tfPolicy = new TextField();
        TextField tfRepo   = new TextField();
        TextField tfTests  = new TextField();        // NEW – tests folder
        TextArea  log      = new TextArea(); log.setEditable(false);

        Button btnRun = new Button("Create Phobos bundle");

        /* ── simple grid layout ───────────────────────────────────────── */
        GridPane gp = new GridPane();
        gp.setPadding(new Insets(10));
        gp.setVgap(6); gp.setHgap(8);

        int r = 0;
        gp.addRow(r++, new Label("policyfile.yaml:"), tfPolicy);
        gp.addRow(r++, new Label("Source repo path:"), tfRepo);
        gp.addRow(r++, new Label("Tests folder (in repo):"), tfTests);
        gp.add(btnRun, 1, r++);
        gp.add(log, 0, r, 2, 1);

        /* ── click handler ────────────────────────────────────────────── */
        btnRun.setOnAction(ev -> {
            log.clear();
            try {
                /* validate paths */
                Path policy = Path.of(tfPolicy.getText().trim());
                Path repo   = Path.of(tfRepo.getText().trim());

                if (!Files.exists(policy))
                    throw new IllegalArgumentException("Policy file not found: " + policy);
                if (!Files.isDirectory(repo))
                    throw new IllegalArgumentException("Repo path is not a directory: " + repo);

                /* resolve tests dir – default is “test” inside repo */

                Path phobosResources = repo.resolve("phobos");
                Files.createDirectories(phobosResources);

                /* build + write tests via Ares/Phobos */
                SecurityPolicyReaderAndDirector.builder()
                        .securityPolicyFilePath(policy)
                        .projectFolderPath(repo)
                        .build()
                        .createTestCases()
                        .writeTestCases(phobosResources);

                /* artefact existence check */
                String[] mustExist = {
                        "BasePhobos.cfg",
                        "TailPhobos.cfg",
                        "allowedHosts.cfg",
                        "phobos_wrapper.sh",
                        "libnetblocker.so"
                };
                boolean ok = true;
                for (String f : mustExist) {
                    Path p = repo.resolve(f);
                    boolean present = Files.exists(p);
                    log.appendText(p + ("Path" + p + (present ? " exists\n" : "  does not exist\n")));
                    ok &= present;
                }
                log.appendText(ok
                        ? "\nAll Phobos artefacts present\n"
                        : "\nMissing artefacts – see above\n");

            } catch (Exception ex) {
                log.appendText("ERROR: " + ex.getMessage() + "\n");
            }
        });

        /* ── stage setup ──────────────────────────────────────────────── */
        stage.setScene(new Scene(gp, 700, 380));
        stage.setTitle("Phobos bundle generator");
        stage.show();
    }

}
