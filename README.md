# XML Parser & Database Updater

A console application for parsing an XML file (actually in YML format ^-^) and synchronizing data with a PostgreSQL database.

The project runs entirely in Docker and does not require Java or PostgreSQL installed locally.

---

## Features

* Parse XML with sections:

  * `currencies`
  * `categories`
  * `offers`
* Automatic creation of database tables
* Table structure validation before updating
* Update a single table or all tables at once
* View list of tables
* Generate table DDL

---

## Technology Stack

* Java (via Groovy script))
* PostgreSQL
* Docker / Docker Compose
* JDBC

---

## ⚙️ Requirements

The only software needed on your computer:

* Docker
* Docker Compose

No other dependencies required.

---

## How to Download and Run the Project Locally

* Clone the repository
```bash
git clone https://github.com/egortelushkin/xml-parser-test-sm.git
cd xml-parser-test-sm
```

## ▶️ Running the Project

### Build Docker Image

```bash
docker compose build --no-cache
```

---

### Launch the Application

```bash
docker compose run --rm -it updater
```

After launching, the application will display an interactive menu.

---

## Console Menu

```
1. Show tables
2. Show table DDL
3. Update table
4. Update all tables
5. Exit
```

### Menu Options Description:

* **1 Show tables**
  Displays the list of tables available in the database

* **2 Show table DDL**
  Shows the SQL structure of the selected table

* **3 Update table**
  Updates data in a single selected table

* **4 Sequentially updates ALL SECTIONS**
  Sequentially updates ALL SECTIONS

* **5 Exit**

---

## Implementation Notes

### Normalizing User Input

To ensure correct operation on Windows + Docker, input is cleaned by:

* Removing BOM and special characters
* Converting to lowercase

This prevents errors like:

```
Unknown table: offers
```

---

## Full Environment Reset (if something goes wrong)

```bash
docker compose down -v
docker system prune -af
docker compose build --no-cache
docker compose run --rm -it updater
```

---

## Project Purpose

Created as a test assignment
---

## Possible Improvements

* Save <param> of offer as JSON
* Log errors per row
* Add REST API on top of the data

---

