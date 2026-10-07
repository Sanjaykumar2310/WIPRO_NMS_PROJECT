# RESTCONF AI — Viva Preparation & Technical Guide

This document contains complete preparation material for project reviews, viva voce, and live demonstrations of the **"CRUD Operations with RESTCONF and Java with AI Integration"** project.

---

## 📚 Table of Contents
1. [Core Technical Concepts](#1-core-technical-concepts)
2. [RESTCONF vs NETCONF vs SNMP vs REST API](#2-restconf-vs-netconf-vs-snmp-vs-rest-api)
3. [YANG Data Modeling Guide](#3-yang-data-modeling-guide)
4. [RFC 8040 HTTP Methods Mapping](#4-rfc-8040-http-methods-mapping)
5. [System Architecture & Data Flow](#5-system-architecture--data-flow)
6. [Top 25 Viva Questions & Model Answers](#6-top-25-viva-questions--model-answers)
7. [Live Demonstration Script for Examiners](#7-live-demonstration-script-for-examiners)

---

## 1. Core Technical Concepts

### What is RESTCONF?
**RESTCONF** (defined in **RFC 8040**) is an HTTP-based protocol that provides a programmatic interface for accessing data defined in YANG modules. It uses RESTful principles and HTTP methods (`GET`, `POST`, `PUT`, `PATCH`, `DELETE`) over TLS to interact with configuration datastores (`running`, `candidate`) and operational state data on network devices.

### Key Characteristics of RESTCONF:
* **Data Model Driven:** All data structured according to YANG data models.
* **HTTP/REST Transport:** Uses standard HTTP protocols and ports (80/443).
* **Data Formats:** Supports both **JSON** (`application/yang-data+json`) and **XML** (`application/yang-data+xml`).
* **Stateless Connections:** Unlike NETCONF which uses persistent SSH sessions, RESTCONF uses stateless HTTP connections.

---

## 2. RESTCONF vs NETCONF vs SNMP vs REST API

| Feature | RESTCONF (RFC 8040) | NETCONF (RFC 6241) | SNMP (v1/v2c/v3) | Standard REST API |
| :--- | :--- | :--- | :--- | :--- |
| **Transport** | HTTPS | SSH / TLS | UDP (161/162) | HTTP / HTTPS |
| **Protocol Type** | RESTful HTTP | RPC-based | Polling / Traps | RESTful HTTP |
| **Data Format** | JSON / XML | XML | BER (Binary) | JSON / XML / Custom |
| **Data Modeling** | YANG | YANG | MIB (SMIv2) | Custom / OpenAPI |
| **Statefulness** | Stateless | Stateful (SSH session) | Stateless | Stateless |
| **Datastore Access**| `/restconf/data/` | `<get-config>`, `<edit-config>` | OID lookup | Custom endpoints |
| **Primary Use Case**| Web applications, SDN controllers | Large-scale orchestration | Monitoring / Alerts | General web services |

---

## 3. YANG Data Modeling Guide

### What is YANG?
**YANG** (Yet Another Next Generation, defined in **RFC 6020 / RFC 7950**) is a data modeling language used to model configuration data, state data, Remote Procedure Calls (RPCs), and notifications for network management protocols like RESTCONF and NETCONF.

### Structure of `network-device.yang` in this Project:
```yang
module network-device {
  yang-version 1.1;
  namespace "urn:example:network-device";
  prefix nd;

  container network-device {
    leaf device-id { type string; }
    leaf hostname { type string; }
    
    container interfaces {
      list interface {
        key "name";
        leaf name { type string; }
        leaf enabled { type boolean; }
        leaf ipv4-address { type string; }
      }
    }
  }
}
```

### Mapping YANG to JSON (RFC 7951):
* YANG `container` $\rightarrow$ JSON Object `{}`
* YANG `list` $\rightarrow$ JSON Array `[]`
* YANG `leaf` $\rightarrow$ JSON Key-Value pair `"key": "value"`
* Namespace prefixes map to JSON keys: `"network-device:interfaces"`

---

## 4. RFC 8040 HTTP Methods Mapping

| Operation | HTTP Method | RESTCONF Target URI | RFC 8040 Behavior |
| :--- | :--- | :--- | :--- |
| **READ** | `GET` | `/restconf/data/ietf-interfaces:interfaces` | Retrieves target configuration or state data. |
| **CREATE** | `POST` | `/restconf/data/ietf-interfaces:interfaces` | Creates a child node within the parent resource. |
| **UPDATE (Replace)** | `PUT` | `/restconf/data/ietf-interfaces:interfaces/interface=GigabitEthernet0/1` | Replaces target node completely or creates it if missing. |
| **UPDATE (Merge)** | `PATCH` | `/restconf/data/ietf-interfaces:interfaces/interface=GigabitEthernet0/1` | Modifies existing target fields without replacing non-mentioned fields. |
| **DELETE** | `DELETE` | `/restconf/data/ietf-interfaces:interfaces/interface=GigabitEthernet0/1` | Removes the target node from the configuration datastore. |

---

## 5. System Architecture & Data Flow

```
[ Web Browser ]
      │
      │ 1. HTTP REST Requests (JSON)
      ▼
[ Spring Boot Controller Layer ]
  (DeviceController / CrudController / AIController)
      │
      │ 2. DTOs & Validation (InputValidator)
      ▼
[ Service Layer ]
  ├── DeviceService (Inventory & Audit Trail)
  ├── CrudService (RFC 8040 Dispatcher & URI Parsing)
  └── AIAnalysisService (Gemini API Integration & Heuristics)
      │
      ├──────────────────────────────┐
      │ 3a. RestconfClient (HTTPS)   │ 3b. Gemini REST API (HTTPS)
      ▼                              ▼
[ Network Device / Simulator ]  [ Google Gemini AI Cloud ]
```

---

## 6. Top 25 Viva Questions & Model Answers

### Q1: What is the main difference between RESTCONF and NETCONF?
**Answer:** NETCONF uses SSH connections and XML payload RPC messages (`<get-config>`, `<edit-config>`), requiring persistent session management. RESTCONF uses HTTP/1.1 or HTTP/2 over TLS, standard HTTP methods (`GET`, `POST`, `PUT`, `DELETE`), and supports both JSON and XML, making it lightweight and easily consumable by web applications.

### Q2: What is YANG, and why is it essential for RESTCONF?
**Answer:** YANG is a formal data modeling language (RFC 7950). RESTCONF requires YANG because YANG defines the hierarchy, data types, constraints, and operational structure of the device configuration. Without a YANG model, network configuration lacks standardized structure.

### Q3: What is the difference between `PUT` and `PATCH` in RESTCONF?
**Answer:** `PUT` replaces the entire resource at the target URI (or creates it if it doesn't exist). `PATCH` merges partial updates into the existing resource without overwriting non-specified attributes.

### Q4: How does your application handle AI API failures or missing API keys?
**Answer:** The project implements a **Fail-Safe Heuristic Engine**. If the Gemini API key is missing or an HTTP error/timeout occurs, `AIAnalysisService` seamlessly falls back to a deterministic rule-based analysis algorithm that inspects interface states, IP configurations, and device metrics to produce structured JSON recommendations.

### Q5: How is security handled in this project?
**Answer:**
1. API keys are stored in system environment variables (`GEMINI_API_KEY`), never hardcoded.
2. Device passwords are classified as `@JsonProperty(access = WRITE_ONLY)` so they are never returned in GET responses.
3. AI API requests are proxied via the Java backend; credentials are never exposed to the client browser.
4. Input validation sanitizes all device parameters and URLs against regex rules.

### Q6: What media types are standard for RESTCONF JSON?
**Answer:**
* `application/yang-data+json` for data payloads.
* `application/yang-data+xml` for XML payloads.

### Q7: What is the significance of the `key` leaf in a YANG list?
**Answer:** In YANG, a `list` node represents an array of entries. The `key` leaf uniquely identifies each entry in the list (similar to a primary key in a database table). In RESTCONF URIs, the key value identifies the resource (e.g., `/interface=GigabitEthernet0/1`).

### Q8: What design pattern did you use for the HTTP client?
**Answer:** The project uses Java 11+ native `HttpClient` combined with the **Builder Pattern** for request construction, dependency injection via Spring Boot `@Service`, and standard DTO (Data Transfer Object) patterns for request/response serialization.

### Q9: Explain how CRUD map to RESTCONF operations.
**Answer:**
* **Create** $\rightarrow$ `POST` (create child resource)
* **Read** $\rightarrow$ `GET` (retrieve configuration/state)
* **Update** $\rightarrow$ `PUT` (replace) or `PATCH` (merge)
* **Delete** $\rightarrow$ `DELETE` (remove resource)

### Q10: How does the Simulator Mode work in your project?
**Answer:** `RestconfSimulator` mimics a real RESTCONF server inside the Spring Boot container. It loads default YANG-compliant network device data from `simulator-data.json` into an in-memory `ConcurrentHashMap`. Standard RESTCONF operations manipulate this datastore in real time, making the application fully demonstrable without real hardware.

---

## 7. Live Demonstration Script for Examiners

Follow this step-by-step walkthrough during your project demonstration:

### Step 1: System Overview & Launch
1. Open PowerShell and run `mvn spring-boot:run` (or launch the jar).
2. Open browser at `http://localhost:8080`.
3. Point out the **Dashboard**, showing active devices, simulated RESTCONF endpoint status, and recent operations log.

### Step 2: Device Inventory Management
1. Navigate to **Device Inventory**.
2. Show pre-configured devices (`Router-01`, `Switch-01`, `Firewall-01`).
3. Click **Add Device** to register a new Cisco/Juniper router with IP, SSH port, and credentials.

### Step 3: Performing RESTCONF CRUD Operations
1. Navigate to **CRUD Operations**.
2. Select target device **Router-01** and module `ietf-interfaces`.
3. **READ (GET):** Click **Execute Operation**. Show the returned JSON configuration payload representing router interfaces.
4. **CREATE (POST):** Add a new interface `GigabitEthernet0/3` with IP `192.168.3.1/24`. Click execute.
5. **READ (GET):** Execute GET again to verify `GigabitEthernet0/3` now exists in the datastore.
6. **UPDATE (PUT/PATCH):** Change IP of `GigabitEthernet0/3` to `192.168.3.254/24` or toggle enabled status.
7. **DELETE:** Execute DELETE for `GigabitEthernet0/3` and verify removal.

### Step 4: AI Analysis & Intelligent Recommendations
1. Click **Analyze with AI** on the device configuration view.
2. Show the **AI Analysis Dashboard** with tabs:
   * **Full Analysis:** Risk score, status summary, and recommendations.
   * **Issue Detection:** Detects disabled interfaces, missing IP addresses, default passwords, or security flags.
   * **Plain English Explanation:** Translates YANG JSON into non-technical terms.
   * **Troubleshooting Steps:** Step-by-step resolution steps generated by AI/Heuristics.

### Step 5: Audit Trail & Operation History
1. Navigate to **Operation History**.
2. Show the chronological record of all performed CRUD actions with timestamp, HTTP status codes, execution duration (ms), and target URIs.
