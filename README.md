# CS122B Project 4: Fabflix - Improved Search & Scalability

## Team Members
* Cesar Gonzalez (21385718)

## Demo Video
https://youtu.be/L9_RLLDq1cs

## Instructions to Run This Project

This guide explains how to get the Fabflix project running.

1.  **Prerequisites:**
   * Three AWS EC2 instances are needed:
      * One for the Load Balancer (running Apache2).
      * One for the Master database (MySQL) and a Tomcat server for Fabflix.
      * One for the Slave database (MySQL) and another Tomcat server for Fabflix.
   * MySQL Server must be installed on both the Master and Slave instances, with master-slave replication configured between them.
   * The `edth` User-Defined Function (UDF) from the Flamingo Toolkit is required for fuzzy search. This involves:
      * Compiling the `edth` function from its C source code (using `make` within the toolkit's directory structure).
      * Copying the resulting `libedth.so` file to the MySQL plugin directory on both the Master and Slave instances.
      * Restarting MySQL on both instances.
      * Running the `CREATE FUNCTION edth RETURNS INTEGER SONAME 'libedth.so';` SQL command (found in the toolkit's `edth.sql` file) in the `moviedb` database on both the Master and Slave instances. (Note: While DDL replication should ideally handle the Slave, I did this step manually on the Slave as well to ensure functionality).
   * Tomcat 10 (or your version) must be installed and running on the Master and Slave instances.
   * Apache2 must be installed and configured as a load balancer on the Load Balancer instance.
   * The `moviedb` database schema should be created (using `create_table.sql`) and populated with initial data (e.g., using `data.sql`) on the Master MySQL instance. This data should then replicate to the Slave.
   * **Important for search functionality:** The `movies` table needs a `FULLTEXT` index on its `title` column. This can be added with: `ALTER TABLE movies ADD FULLTEXT(title);`.

2.  **Building the Project:**
   * Clone or download the project code from the GitHub repository.
   * Ensure Apache Maven is installed.
   * Open a terminal, navigate to the project's root directory (containing `pom.xml`), and run:
       ```bash
       mvn clean package
       ```
   * This command compiles the Java code and packages the web application into a `cs122b-project1-star-example.war` file, located in the `target/` directory.

3.  **Deploying the Application:**
   * Deploy the `cs122b-project1-star-example.war` file to Tomcat. This must be done on **both** the Master instance's Tomcat AND the Slave instance's Tomcat. The Tomcat Manager web interface is typically used for deployment.

4.  **Key Configuration Files:**
   * **`src/main/webapp/META-INF/context.xml`**: This file is included in the `.war` and defines the JNDI DataSources that Tomcat uses to connect to the databases.
      * `jdbc/moviedb_master`: Configured to connect to the Master MySQL database using its private AWS IP address (e.g., `172.31.3.184`).
      * `jdbc/moviedb_slave`: Configured to connect to the Slave MySQL database using its private AWS IP address (e.g., `172.31.12.87`).
   * **Apache Load Balancer Configuration (`000-default.conf`)**: Located on the Load Balancer instance (e.g., at `/etc/apache2/sites-enabled/000-default.conf`), this file directs incoming web traffic to the Tomcat servers on the Master and Slave instances. Sticky sessions are enabled to ensure a user consistently connects to the same backend server during their session.

5.  **Accessing Fabflix:**
   * Once deployed and configured, the Fabflix application can be accessed via the public IP address of your Load Balancer instance, for example: `http://18.116.225.30/cs122b-project1-star-example/`.

---
## Test Accounts
* **Customer Account:** `tshpark@gmail.com` (Password: `2001`)
* **Employee Account:** `classta@email.edu` (Password: `myNewPass123`)

---

## Database Connection Pooling (JDBC)

To efficiently manage database connections and improve performance, I use JDBC Connection Pooling provided by Apache Tomcat.

* **How it works:** Instead of creating a new database connection for every request (which is slow and resource-intensive), Tomcat maintains a "pool" of ready-to-use connections. Our Java code borrows a connection from this pool when needed and returns it after use. This allows connections to be reused, significantly reducing overhead.

* **Configuration (`context.xml`):**
  The connection pools are configured in `src/main/webapp/META-INF/context.xml`. This file defines our two DataSources: `jdbc/moviedb_master` and `jdbc/moviedb_slave`. Tomcat uses these definitions to create and manage the connection pools. I also included `cachePrepStmts=true` in the JDBC URLs within this file, as recommended by the Project 4 PDF, to enable caching of Prepared Statements by the database driver, which can improve performance for frequently executed queries.

* **Usage in Java Code:**
  Our servlets and service classes obtain connections from these pools via JNDI lookup. For instance:
    ```java
    // Example of getting a connection from the slave pool
    Context initCtx = new InitialContext();
    DataSource ds = (DataSource) initCtx.lookup("java:comp/env/jdbc/moviedb_slave");
    Connection conn = ds.getConnection(); 
    // ... use connection ...
    conn.close(); // Returns connection to the pool
    ```
  This approach is used by all Java classes that require database access, including `LoginServlet`, `MovieSuggestionServlet`, `MovieListService` (used by `SearchServlet` and `BrowseServlet`), `SingleMovieServlet`, `SingleStarServlet`, `GenreServlet`, `PlaceOrderServlet`, and `DashboardServlet`. The `movie-list.jsp` page also uses this mechanism for its sub-queries that fetch genre and star information for each displayed movie.

---

## Prepared Statements for SQL Queries

I use `PreparedStatement` for constructing and executing SQL queries in our Java code. This is standard practice for two main reasons:

* **Security:** `PreparedStatement` helps prevent SQL injection vulnerabilities by treating user-supplied input as data rather than executable SQL code.
* **Performance:** For SQL statements that are executed multiple times (even with different parameter values), `PreparedStatement` can offer performance benefits as the database can precompile the statement and reuse the execution plan. This is further aided by the `cachePrepStmts=true` setting in our JDBC URL.

* **How I use them:**
  Instead of directly embedding user input into SQL strings, I use `?` as placeholders for parameters:
    ```java
    // Example from our Java code
    String sql = "SELECT * FROM movies WHERE director LIKE ? AND year = ?";
    PreparedStatement pstmt = conn.prepareStatement(sql);
    pstmt.setString(1, "%" + directorInput + "%"); 
    pstmt.setInt(2, yearInput);          
    ResultSet rs = pstmt.executeQuery();
    ```
  This method is consistently applied across our application: in `MovieListService` for complex search and browse queries (including full-text, `LIKE`, and `edth` conditions), `MovieSuggestionServlet` for autocomplete queries, `LoginServlet` for user authentication, `SingleMovieServlet` and `SingleStarServlet` for detail pages, `PlaceOrderServlet` for processing sales, and `DashboardServlet` for administrative functions. The scriptlets in `movie-list.jsp` that perform sub-queries for genres and stars also use `PreparedStatement`.

---

## Master/Slave Database Architecture for Scalability

To enhance performance and availability, Fabflix uses a MySQL Master/Slave replication setup.

* **System Overview:**
   * **Master Database (`jdbc/moviedb_master`):** This is the primary database where all data modifications (writes like `INSERT`, `UPDATE`, `DELETE`, and schema changes like `CREATE FUNCTION`) occur. It is located on our AWS instance with the private IP `172.31.3.184`.
   * **Slave Database (`jdbc/moviedb_slave`):** This database is a replica of the Master. It automatically receives updates from the Master. It is used primarily for read operations to reduce the load on the Master. It's located on our AWS instance with the private IP `172.31.12.87`.
   * Both DataSources are defined in `src/main/webapp/META-INF/context.xml`, which tells Tomcat how to connect to each.

* **Read/Write Splitting Logic:**
  Our application directs database operations to the appropriate server:
   * **Write Operations (to Master - `jdbc/moviedb_master`):**
      * When a customer places an order (`PlaceOrderServlet`).
      * When an employee adds new movies or stars via the `DashboardServlet`.
      * The initial installation of the `edth` UDF (`CREATE FUNCTION`) was performed on the Master.
      * Any initial data loading (e.g., via `StanfordDataImporter.java` or `data.sql`) is done on the Master.
   * **Read Operations (to Slave - `jdbc/moviedb_slave`):**
      * Customer login (`LoginServlet`).
      * Autocomplete search suggestions (`MovieSuggestionServlet`).
      * Displaying movie lists from search or browse actions (handled by `MovieListService`, which is called by `SearchServlet` and `BrowseServlet`).
      * Fetching details for a single movie (`SingleMovieServlet`) or star (`SingleStarServlet`).
      * Listing all genres (`GenreServlet`).
      * Read-only operations on the `DashboardServlet` (like employee login and displaying schema information).
      * The sub-queries in `movie-list.jsp` for fetching genres and stars also connect to the slave database.

  This distribution of workload helps maintain application responsiveness.

* **Configuration File:**
  The `src/main/webapp/META-INF/context.xml` file is central to this setup, as it defines the separate JNDI resources (`jdbc/moviedb_master`, `jdbc/moviedb_slave`) that allow Tomcat to provide connections to the correct database server.

---

## Fuzzy Search Implementation

To improve the movie search experience, especially for handling typos, I implemented a fuzzy search feature.

* **Search Method:** Our search combines three techniques:
   1.  **MySQL's Full-Text Search (FTS):** For fast word matching in movie titles.
   2.  **SQL `LIKE '%query%'`:** To find titles where the search term is a substring.
   3.  **`edth` Function (Edit Distance):** This User-Defined Function from the Flamingo Toolkit is key for handling misspellings. It finds titles that are a few characters different from the typed query.

* **Setting up the `edth` UDF:**
   * I downloaded the Flamingo Toolkit (`toolkit_2021-05-18.tgz`).
   * On our Master MySQL server, I compiled the `edth` C source code (`edth.c`) into a shared library (`libedth.so`) using the `make` utility.
   * This `libedth.so` file was copied to MySQL's plugin directory.
   * I then executed the `CREATE FUNCTION edth RETURNS INTEGER SONAME 'libedth.so';` SQL command (found in the `edth.sql` file from the toolkit) in MySQL on the Master.
   * **Note on Replication:** While DDL commands like `CREATE FUNCTION` should ideally replicate to the slave, I found it necessary to perform these `edth` installation steps (copying `libedth.so`, restarting MySQL, and running `CREATE FUNCTION`) directly on the Slave MySQL instance as well to ensure the function was available there.

* **Determining "Fuzziness" (`uci122b.util.SearchUtil.java`):**
  The `edth` function requires a "threshold" (maximum allowed typos). I created a Java class `uci122b.util.SearchUtil` with a method `calculateEditDistanceThreshold(String query)`. This method sets a reasonable threshold based on the length of the user's search query.

* **Handling Case Sensitivity with `edth`:**
  The `edth` function is case-sensitive. To ensure it works whether users type in lowercase or if titles are in mixed case, our SQL queries convert both the movie title from the database and the user's query to lowercase before passing them to `edth`. This is done using `edth(LOWER(database_column), LOWER(user_query), threshold)`.

* **Integration into the Application:**
   * **Autocomplete (`MovieSuggestionServlet.java`):** As a user types in the main search bar, this servlet uses the combined search (FTS, `LIKE`, and case-insensitive `edth`) to provide live suggestions.
   * **Main Search Results (`uci122b.service.MovieListService.java`):** When a full search is performed, this service class (used by `SearchServlet.java` and `BrowseServlet.java`) employs the same combined search logic to fetch the movie list.
   * **Frontend Search Bar (`index.html`):** The JavaScript on our main page (`index.html`) includes features like a 300ms delay before sending an autocomplete request, a 3-character minimum for autocomplete, and client-side caching of suggestions to reduce server load.

---
