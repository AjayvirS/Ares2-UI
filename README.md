# Ares-UI (Ares2 frontend)

A tiny JavaFX application that lets you **point-&-click-generate security test case files** for student repositories using the Phobos sandbox from
[Ares 2](https://github.com/ls1intum/Ares2)  



## Features
* **Three path inputs**  
  1. `policyfile.yaml` – exercise-specific network policy  
  2. `repo` – local checkout of the student assignment  
  3.  `tests/` – folder to place generated Ares2 test resources
* Generates security tests that can be used during exercise evaluation to isolate/verify student code, s.t. remote code cannot gain access to unauthorized resources
* Streams Phobos logs to the GUI **and** to `/localhost:9001/logs`.
* Shows a log and verifies expected artefacts.

---

## Prerequisites
| Tool | Version |
|------|---------|
| Java | **17** or newer |
| Maven| **3.9** or newer (Gradle wrapper supported for student repos) |
| Git  | any modern version |

> **Note** – The core Phobos library is published as  
> `de.tum.cit.ase:ares:2.0.0-Beta-4`.  
> Add the GitHub Packages repo (see below) or run  
> `mvn -Prelease clean install` inside the Ares2 sources once.

---

## Build Ares2 locally (required at least once)

Clone and install the Ares2 library so Ares2‑UI can depend on it:

```bash
git clone https://github.com/ls1intum/Ares2.git
cd Ares2
git checkout feature/phobos-integration
mvn -DskipTests clean install
```

This publishes Ares2 artifacts to your local ~/.m2 repository.

## Build and Run Ares2-UI

```bash
git clone https://github.com/AjayvirS/Ares2-UI.git
cd Ares2-UI
mvn clean javafx:run
```


