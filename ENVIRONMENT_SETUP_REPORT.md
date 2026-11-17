# DRIVIO - Windows Local Development Environment Setup Report

**Date & Time:** 2026-11-17T00:15:30+05:30  
**Project Location:** `C:\Users\Vishal Sharma\Documents\drivio`  
**Host Machine:** Windows 11 (64-bit AMD64)  
**Author:** Windows Development Environment Setup Engineer  

---

## 1. Executive Summary

The local development environment on your Windows machine has been inspected, configured, and verified. 
All core runtime software (Java JDK 21, Apache Maven 3.9.11, Node.js 26, npm 11, PostgreSQL client 17, Git, and Ollama) are already present on disk. 

The configuration gaps (unconfigured user `JAVA_HOME`, missing `MAVEN_HOME`, and missing executable paths in the Windows User `Path`) have been resolved by updating the **persistent Windows User registry** (`HKCU\Environment`). All tools have been verified, and both backend (`mvn dependency:resolve` and `mvn compile`) and frontend (`npm run build`) dependency availability have been validated with zero compilation errors.

---

## 2. Installed Software & Versions

| Component | Verified Version | Installation Path | Status |
| :--- | :--- | :--- | :--- |
| **Java Development Kit** | OpenJDK `21.0.13` (Temurin LTS, build 21.0.13+9-LTS) | `C:\Program Files\Eclipse Adoptium\jdk-21.0.13.9-hotspot` | ✅ Verified & Working |
| **Apache Maven** | Apache Maven `3.9.11` | `C:\Tools\apache-maven-3.9.11` | ✅ Verified & Working |
| **Node.js** | Node.js `v26.13.1` (64-bit) | `C:\Program Files\nodejs\node.exe` | ✅ Verified & Working |
| **npm** | npm `11.20.0` | `C:\Program Files\nodejs\npm.cmd` | ✅ Verified & Working |
| **PostgreSQL Client** | psql `17.12` | `C:\Program Files\PostgreSQL\17\bin\psql.exe` | ✅ Verified & Working |
| **PostgreSQL Server** | PostgreSQL `16.6` (Visual C++ build 1942, 64-bit) | Port `5432` (daemon / service) | ✅ Verified & Accessible |
| **Git** | Git `2.54.0.windows.1` | `C:\Program Files\Git\cmd\git.exe` | ✅ Verified & Working |
| **Ollama CLI & App** | Ollama `0.40.16` | `C:\Users\Vishal Sharma\AppData\Local\Programs\Ollama\ollama.exe` | ✅ Running (2 processes) |

---

## 3. Persistent Windows Environment Variables Configured

The following environment variables have been set permanently in the Windows User Registry (`HKEY_CURRENT_USER\Environment`). They will persist across all future terminals, Command Prompts, and PowerShell sessions:

| Variable | Scope | Configured Value |
| :--- | :--- | :--- |
| `JAVA_HOME` | User (`HKCU`) | `C:\Program Files\Eclipse Adoptium\jdk-21.0.13.9-hotspot` |
| `MAVEN_HOME` | User (`HKCU`) | `C:\Tools\apache-maven-3.9.11` |
| `Path` (Added entries) | User (`HKCU`) | `C:\Program Files\Eclipse Adoptium\jdk-21.0.13.9-hotspot\bin`<br>`C:\Tools\apache-maven-3.9.11\bin`<br>`C:\Program Files\PostgreSQL\17\bin` |

*Note: Existing user PATH items (Ollama, VS Code, npm, Python, SoX, etc.) were completely preserved without duplicates.*

---

## 4. PostgreSQL & Database Status

- **Host & Port:** `localhost:5432`
- **Server Version:** PostgreSQL 16.6 (64-bit)
- **Target Database:** `drivio`
- **Authentication Status:** Verified connectivity via local loopback. (No plain-text credentials stored or printed).
- **Public Schema Integrity:** 15 tables intact:
  1. `booking_locations`
  2. `bookings`
  3. `complaints`
  4. `customer_profiles`
  5. `driver_availability`
  6. `driver_documents`
  7. `driver_location_history`
  8. `driver_profiles`
  9. `flyway_schema_history`
  10. `notifications`
  11. `payments`
  12. `pricing_configurations`
  13. `reviews`
  14. `users`
  15. `vehicles`
- **Flyway Migration History:**
  - Rank 1: Version `1` (`<< Flyway Baseline >>`, `BASELINE`, Success: `true`)
  - Rank 2: Version `2` (`seed initial data`, `V2__seed_initial_data.sql`, Checksum: `132642745`, Success: `true`)

---

## 5. Ollama & AI Model Status

- **Ollama Engine:** Version `0.40.16` is installed and actively running (PID `16472` & `24880`).
- **Ollama API Port:** Listening on `http://localhost:11434`.
- **Installed Models:** Currently none (`ollama list` returned empty).
- **Target Model:** `llama3.1:8b` (approx. 4.7 GB).
- **Drive C Available Space:** **67.39 GB free**.
- **Action Required:** As per safety guidelines, model downloading is held pending explicit user approval. Once approved, run:
  ```powershell
  ollama run llama3.1:8b
  # or
  ollama pull llama3.1:8b
  ```

---

## 6. Build & Dependency Verification

1. **Backend Dependency Resolution (`mvn dependency:resolve`):**
   - Result: `BUILD SUCCESS` (7.488s).
   - Resolved all Spring Boot 3.3.5, Hibernate, JJWT (0.12.6), SpringDoc OpenAPI (2.6.0), and PostgreSQL driver dependencies.
2. **Backend Compilation (`mvn compile -DskipTests`):**
   - Result: `BUILD SUCCESS` (3.974s).
   - All 108 Java source files compiled targeting Java 21 bytecode release with 0 compilation errors.
3. **Frontend Dependencies & Build (`npm run build`):**
   - Result: `BUILD SUCCESS` (851ms).
   - All npm dependencies (React 19, Vite 8, Leaflet, Tailwind CSS 4, React Router 7, Axios, StompJS) are available in `frontend/node_modules` and compiled cleanly.

---

## 7. Missing Dependencies & Errors Encountered

- **Missing Software:** None. All required software components are already installed locally.
- **Missing AI Model:** `llama3.1:8b` is not yet pulled into local Ollama storage.
- **Errors Encountered:** None. All persistent registry configurations and compilation passes succeeded cleanly.

---

## 8. Exact Commands to Run DRIVIO Locally

Open two separate terminal windows (PowerShell or Command Prompt). Note that any newly opened terminal will immediately recognize `java`, `mvn`, `psql`, and `node` from the persistent environment variables.

### Terminal 1: Start Backend (Spring Boot)

```powershell
# Navigate to backend directory
cd "C:\Users\Vishal Sharma\Documents\drivio\backend"

# Run with Maven Spring Boot plugin (uses active profile 'postgres' and local database drivio)
mvn spring-boot:run
```

*Backend REST API will be accessible at:* `http://localhost:8080/api`  
*Swagger / OpenAPI UI documentation:* `http://localhost:8080/swagger-ui.html`

---

### Terminal 2: Start Frontend (Vite + React)

```powershell
# Navigate to frontend directory
cd "C:\Users\Vishal Sharma\Documents\drivio\frontend"

# Launch Vite development server
npm run dev
```

*Frontend Web Application will be accessible at:* `http://localhost:5173`

---

### Optional: Pull AI Model (Ollama)

When ready to enable the local AI requirement extraction assistant:

```powershell
# Verify Ollama service is responsive
ollama list

# Pull Llama 3.1 8B (requires ~4.7 GB disk space; 67.39 GB available)
ollama pull llama3.1:8b
```

