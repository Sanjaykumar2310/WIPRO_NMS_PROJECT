# RESTCONF AI — CRUD Operations with RESTCONF and Java with AI Integration

A Java-based network automation application demonstrating CRUD operations via the RESTCONF protocol with AI-powered configuration analysis.

## 🏗️ System Architecture

```
User → Frontend (HTML/CSS/JS)
         ↓
    Java Spring Boot Backend
         ↓
    ┌─────────────┬──────────────┐
    │ CRUD Service │ AI Service   │
    │      ↓      │      ↓       │
    │ RESTCONF    │ Gemini/OpenAI│
    │ Client      │ /HuggingFace │
    │      ↓      │              │
    │ Network     │              │
    │ Device /    │              │
    │ Simulator   │              │
    └─────────────┴──────────────┘
```

## 🚀 Quick Start

### Prerequisites
- Java 17 or later
- Maven 3.8+

### 1. Clone and Build
```bash
cd nms
mvn clean install -DskipTests
```

### 2. Configure AI (Optional)
```bash
# Copy the example env file
cp .env.example .env

# Edit .env and add your AI API key
# For Google Gemini (recommended, free tier available):
# Get key from https://aistudio.google.com/apikey
```

Set the environment variable before running:
```bash
# Windows PowerShell
$env:GEMINI_API_KEY="your-api-key-here"

# Linux/Mac
export GEMINI_API_KEY="your-api-key-here"
```

> **Note:** AI works in fallback mode (basic rule-based analysis) even without an API key.

### 3. Run the Application
```bash
mvn spring-boot:run
```

### 4. Open the Dashboard
Open your browser and go to: **http://localhost:8080**

## 📁 Project Structure

```
src/main/java/com/example/restconfai/
├── RestconfAiApplication.java          # Main entry point
├── controller/
│   ├── DeviceController.java           # Device management API
│   ├── CrudController.java             # RESTCONF CRUD API
│   └── AIController.java               # AI analysis API
├── service/
│   ├── DeviceService.java              # Device business logic
│   ├── CrudService.java                # CRUD orchestration
│   └── AIAnalysisService.java          # AI integration
├── restconf/
│   ├── RestconfClient.java             # HTTP client for RESTCONF
│   ├── RestconfRequest.java            # Request DTO
│   └── RestconfResponse.java           # Response DTO
├── simulator/
│   ├── RestconfSimulator.java          # Mock RESTCONF server
│   └── SimulatorDataStore.java         # In-memory data store
├── model/
│   ├── Device.java                     # Network device model
│   ├── InterfaceConfig.java            # Interface config model
│   ├── DeviceConfig.java               # Device config model
│   ├── AIAnalysis.java                 # AI analysis result
│   └── OperationHistory.java           # Operation audit log
├── config/
│   ├── RestconfConfig.java             # RESTCONF settings
│   ├── AIConfig.java                   # AI provider settings
│   └── WebConfig.java                  # CORS configuration
├── exception/
│   ├── RestconfException.java          # Custom exception
│   └── GlobalExceptionHandler.java     # Error handling
└── validation/
    └── InputValidator.java             # Input validation

src/main/resources/
├── application.properties              # App configuration
├── static/                             # Frontend files
│   ├── index.html                      # Dashboard HTML
│   ├── css/style.css                   # Styles
│   └── js/app.js                       # Frontend logic
├── data/simulator-data.json            # Simulator data
└── yang/network-device.yang            # YANG model reference
```

## 🔧 Operating Modes

### Simulator Mode (Default)
Used for college demonstration. No physical router needed.
```properties
restconf.mode=simulator
```

### Real RESTCONF Mode
Connect to actual RESTCONF-enabled devices.
```properties
restconf.mode=real
restconf.base-url=https://192.168.1.1:443/restconf
```

## 📡 API Endpoints

### Devices
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/devices` | List all devices |
| POST | `/api/devices` | Add a device |
| GET | `/api/devices/{id}` | Get device details |
| PUT | `/api/devices/{id}` | Update a device |
| DELETE | `/api/devices/{id}` | Remove a device |
| GET | `/api/devices/dashboard/stats` | Dashboard statistics |

### RESTCONF CRUD
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/restconf/create` | CREATE (POST to device) |
| POST | `/api/restconf/read` | READ (GET from device) |
| PUT | `/api/restconf/update` | UPDATE (PUT to device) |
| PATCH | `/api/restconf/update` | PATCH (partial update) |
| POST | `/api/restconf/delete` | DELETE from device |
| POST | `/api/restconf/execute` | Generic operation |

### AI Analysis
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/ai/analyze` | Analyze configuration |
| POST | `/api/ai/explain` | Explain in simple terms |
| POST | `/api/ai/issues` | Detect potential issues |
| POST | `/api/ai/recommend` | Configuration recommendations |
| POST | `/api/ai/troubleshoot` | Troubleshooting suggestions |
| POST | `/api/ai/summarize` | Generate network summary |
| GET | `/api/ai/status` | Check AI service status |

## 🧪 Running Tests
```bash
mvn test
```

## 🔒 Security
- API keys stored as environment variables (never in code)
- Passwords never exposed in API responses (write-only)
- AI calls routed through backend (never from frontend)
- Sensitive data excluded from logs
- Input validation on all user inputs
- RESTCONF credentials configurable via env vars

## 🛠️ Technology Stack
- **Java 17** + Spring Boot 3.2
- **Maven** for build management
- **Jackson** for JSON processing
- **Java HttpClient** for RESTCONF
- **HTML/CSS/JS** for frontend
- **RESTCONF** (RFC 8040)
- **YANG** data modeling
- **AI**: Google Gemini / OpenAI / HuggingFace (configurable)

## 📋 Demo Scenario
1. Start the application (`mvn spring-boot:run`)
2. Open http://localhost:8080
3. View dashboard with pre-loaded devices
4. Navigate to **Devices** → view Router-01, Switch-01, Firewall-01
5. Navigate to **CRUD Operations** → select Router-01
6. Execute **READ** to see device configuration
7. Execute **CREATE** to add a new interface
8. Execute **UPDATE** to modify an interface
9. Execute **DELETE** to remove an interface
10. Click **Analyze with AI** to send data to AI
11. Navigate to **AI Analysis** → run various analyses
12. View **Operation History** for audit trail
