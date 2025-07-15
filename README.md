# Ares-UI (Ares2 frontend)

A tiny JavaFX application that lets you **point-&-click-generate security test case files** for student repositories using the Phobos sandbox from
[Ares 2](https://github.com/ls1intum/Ares2)  



## Features
* **Three path inputs**  
  1. `policyfile.yaml` – exercise-specific network policy  
  2. `repo` – local checkout of the student assignment  
  3. *(optional)* `tests/` – folder to place generated JUnit tests  
* Generates JUnit 5 dynamic tests that can be used during exercise evaluation to isolate/verify, s.t. remote code cannot gain access to unauthorized resources
* Detects **Maven vs. Gradle** and runs the matching build tool.
* Streams Phobos logs to the GUI **and** to `/localhost:9001/logs`.

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

## Quick Start

```bash
# clone & build
git clone https://github.com/<your-org>/ares-ui.git
cd ares-ui
mvn clean package            # pulls javafx + ares

# run the GUI (port 9001 configurable)
mvn javafx:run -Dexec.args="--httpPort=9001"
