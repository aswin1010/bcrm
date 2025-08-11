// Main.java
// Single-file Banking CRM with optional SQLite persistence and in-memory fallback.

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

// --------------------------
// Models (POJOs)
// --------------------------
class Customer {
    private String customerId;
    private String name;
    private String email;
    private String kycDetails;

    public Customer(String customerId, String name, String email, String kycDetails) {
        this.customerId = customerId;
        this.name = name;
        this.email = email;
        this.kycDetails = kycDetails;
    }

    // Getters
    public String getCustomerId() { return customerId; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getKycDetails() { return kycDetails; }

    @Override
    public String toString() {
        return "Customer{" + "id='" + customerId + '\'' + ", name='" + name + '\'' + ", email='" + email + '\'' + '}';
    }
}

class Account {
    private String accountId;
    private String customerId;
    private String accountType;
    private double balance;

    public Account(String accountId, String customerId, String accountType, double balance) {
        this.accountId = accountId;
        this.customerId = customerId;
        this.accountType = accountType;
        this.balance = balance;
    }

    // Getters & setters
    public String getAccountId() { return accountId; }
    public String getCustomerId() { return customerId; }
    public String getAccountType() { return accountType; }
    public double getBalance() { return balance; }
    public void setBalance(double newBalance) { this.balance = newBalance; }

    @Override
    public String toString() {
        return "Account{" + "id='" + accountId + '\'' + ", customerId='" + customerId + '\'' + ", type='" + accountType + '\'' + ", balance=" + balance + '}';
    }
}

class TransactionRecord {
    private String transactionId;
    private String accountId;
    private String transactionType;
    private double amount;
    private LocalDateTime transactionDate;

    public TransactionRecord(String transactionId, String accountId, String transactionType, double amount, LocalDateTime transactionDate) {
        this.transactionId = transactionId;
        this.accountId = accountId;
        this.transactionType = transactionType;
        this.amount = amount;
        this.transactionDate = transactionDate;
    }

    public String getTransactionId() { return transactionId; }
    public String getAccountId() { return accountId; }
    public String getTransactionType() { return transactionType; }
    public double getAmount() { return amount; }
    public LocalDateTime getTransactionDate() { return transactionDate; }

    @Override
    public String toString() {
        return "Transaction{" + "id='" + transactionId + '\'' + ", accountId='" + accountId + '\'' + ", type='" + transactionType + '\'' + ", amount=" + amount + ", date=" + transactionDate + '}';
    }
}

class ServiceRequest {
    private String requestId;
    private String customerId;
    private String requestType;
    private String description;
    private String status;
    private String assignedStaffId;
    private LocalDateTime creationDate;

    public ServiceRequest(String requestId, String customerId, String requestType, String description, String status, String assignedStaffId, LocalDateTime creationDate) {
        this.requestId = requestId;
        this.customerId = customerId;
        this.requestType = requestType;
        this.description = description;
        this.status = status;
        this.assignedStaffId = assignedStaffId;
        this.creationDate = creationDate;
    }

    public String getRequestId() { return requestId; }
    public String getCustomerId() { return customerId; }
    public String getRequestType() { return requestType; }
    public String getDescription() { return description; }
    public String getStatus() { return status; }
    public String getAssignedStaffId() { return assignedStaffId; }
    public LocalDateTime getCreationDate() { return creationDate; }

    public void setAssignedStaffId(String staffId) { this.assignedStaffId = staffId; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public String toString() {
        return "ServiceRequest{" + "id='" + requestId + '\'' + ", customerId='" + customerId + '\'' + ", type='" + requestType + '\'' + ", status='" + status + '\'' + ", assignedStaffId='" + assignedStaffId + '\'' + ", created=" + creationDate + '}';
    }
}

class Staff {
    private String staffId;
    private String name;
    private String role;

    public Staff(String staffId, String name, String role) {
        this.staffId = staffId;
        this.name = name;
        this.role = role;
    }

    public String getStaffId() { return staffId; }
    public String getName() { return name; }
    public String getRole() { return role; }

    @Override
    public String toString() {
        return "Staff{" + "id='" + staffId + '\'' + ", name='" + name + '\'' + ", role='" + role + '\'' + '}';
    }
}

// --------------------------
// DatabaseManager
// - Tries to connect to SQLite; if not available, falls back to in-memory lists.
// --------------------------
class DatabaseManager {
    private static final String SQLITE_URL = "jdbc:sqlite:bank_crm.db";
    private Connection conn = null;

    // In-memory fallback stores
    private final Map<String, Customer> memCustomers = new LinkedHashMap<>();
    private final Map<String, Account> memAccounts = new LinkedHashMap<>();
    private final Map<String, TransactionRecord> memTransactions = new LinkedHashMap<>();
    private final Map<String, ServiceRequest> memRequests = new LinkedHashMap<>();
    private final Map<String, Staff> memStaff = new LinkedHashMap<>();

    public DatabaseManager() {
        try {
            // Attempt SQLite connection
            conn = DriverManager.getConnection(SQLITE_URL);
            conn.setAutoCommit(true);
            System.out.println("Connected to SQLite DB at ./bank_crm.db");
            initTables();
        } catch (SQLException e) {
            // If connection fails, print warning and use in-memory fallback
            System.out.println("SQLite JDBC unavailable or connection failed. Running in in-memory simulation mode.");
            conn = null;
        }
    }

    private void initTables() {
        // create tables if not exist
        final String createCustomers = "CREATE TABLE IF NOT EXISTS customers (" +
                "customer_id VARCHAR(36) PRIMARY KEY, name TEXT NOT NULL, email TEXT NOT NULL, kyc_details TEXT)";
        final String createAccounts = "CREATE TABLE IF NOT EXISTS accounts (" +
                "account_id VARCHAR(36) PRIMARY KEY, customer_id VARCHAR(36), account_type TEXT, balance REAL, " +
                "FOREIGN KEY(customer_id) REFERENCES customers(customer_id))";
        final String createTransactions = "CREATE TABLE IF NOT EXISTS transactions (" +
                "transaction_id VARCHAR(36) PRIMARY KEY, account_id VARCHAR(36), transaction_type TEXT, amount REAL, transaction_date TEXT, " +
                "FOREIGN KEY(account_id) REFERENCES accounts(account_id))";
        final String createRequests = "CREATE TABLE IF NOT EXISTS service_requests (" +
                "request_id VARCHAR(36) PRIMARY KEY, customer_id VARCHAR(36), request_type TEXT, description TEXT, status TEXT, assigned_staff_id VARCHAR(36), creation_date TEXT, " +
                "FOREIGN KEY(customer_id) REFERENCES customers(customer_id))";
        final String createStaff = "CREATE TABLE IF NOT EXISTS staff (" +
                "staff_id VARCHAR(36) PRIMARY KEY, name TEXT, role TEXT)";

        try (Statement st = conn.createStatement()) {
            st.execute(createCustomers);
            st.execute(createAccounts);
            st.execute(createTransactions);
            st.execute(createRequests);
            st.execute(createStaff);
        } catch (SQLException e) {
            System.err.println("Error initializing tables: " + e.getMessage());
        }
    }

    // --------------------------
    // Customer operations
    // --------------------------
    public boolean addCustomer(Customer customer) {
        if (conn != null) {
            String sql = "INSERT INTO customers (customer_id, name, email, kyc_details) VALUES (?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, customer.getCustomerId());
                ps.setString(2, customer.getName());
                ps.setString(3, customer.getEmail());
                ps.setString(4, customer.getKycDetails());
                ps.executeUpdate();
                return true;
            } catch (SQLException e) {
                System.err.println("addCustomer(SQL) error: " + e.getMessage());
                return false;
            }
        } else {
            memCustomers.put(customer.getCustomerId(), customer);
            return true;
        }
    }

    public List<Customer> getAllCustomers() {
        if (conn != null) {
            List<Customer> out = new ArrayList<>();
            String sql = "SELECT customer_id, name, email, kyc_details FROM customers";
            try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
                while (rs.next()) {
                    out.add(new Customer(
                            rs.getString("customer_id"),
                            rs.getString("name"),
                            rs.getString("email"),
                            rs.getString("kyc_details")
                    ));
                }
            } catch (SQLException e) {
                System.err.println("getAllCustomers(SQL) error: " + e.getMessage());
            }
            return out;
        } else {
            return new ArrayList<>(memCustomers.values());
        }
    }

    public Customer getCustomerById(String customerId) {
        if (conn != null) {
            String sql = "SELECT customer_id, name, email, kyc_details FROM customers WHERE customer_id = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, customerId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return new Customer(rs.getString("customer_id"), rs.getString("name"), rs.getString("email"), rs.getString("kyc_details"));
                    }
                }
            } catch (SQLException e) {
                System.err.println("getCustomerById(SQL) error: " + e.getMessage());
            }
            return null;
        } else {
            return memCustomers.get(customerId);
        }
    }

    // --------------------------
    // Account operations
    // --------------------------
    public boolean addAccount(Account account) {
        if (conn != null) {
            String sql = "INSERT INTO accounts (account_id, customer_id, account_type, balance) VALUES (?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, account.getAccountId());
                ps.setString(2, account.getCustomerId());
                ps.setString(3, account.getAccountType());
                ps.setDouble(4, account.getBalance());
                ps.executeUpdate();
                return true;
            } catch (SQLException e) {
                System.err.println("addAccount(SQL) error: " + e.getMessage());
                return false;
            }
        } else {
            memAccounts.put(account.getAccountId(), account);
            return true;
        }
    }

    public List<Account> getAccountsByCustomerId(String customerId) {
        if (conn != null) {
            List<Account> out = new ArrayList<>();
            String sql = "SELECT account_id, customer_id, account_type, balance FROM accounts WHERE customer_id = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, customerId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        out.add(new Account(
                                rs.getString("account_id"),
                                rs.getString("customer_id"),
                                rs.getString("account_type"),
                                rs.getDouble("balance")
                        ));
                    }
                }
            } catch (SQLException e) {
                System.err.println("getAccountsByCustomerId(SQL) error: " + e.getMessage());
            }
            return out;
        } else {
            List<Account> out = new ArrayList<>();
            for (Account a : memAccounts.values()) if (a.getCustomerId().equals(customerId)) out.add(a);
            return out;
        }
    }

    public Account getAccountById(String accountId) {
        if (conn != null) {
            String sql = "SELECT account_id, customer_id, account_type, balance FROM accounts WHERE account_id = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, accountId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return new Account(rs.getString("account_id"), rs.getString("customer_id"), rs.getString("account_type"), rs.getDouble("balance"));
                    }
                }
            } catch (SQLException e) {
                System.err.println("getAccountById(SQL) error: " + e.getMessage());
            }
            return null;
        } else {
            return memAccounts.get(accountId);
        }
    }

    public boolean updateAccountBalance(String accountId, double newBalance) {
        if (conn != null) {
            String sql = "UPDATE accounts SET balance = ? WHERE account_id = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setDouble(1, newBalance);
                ps.setString(2, accountId);
                int updated = ps.executeUpdate();
                return updated > 0;
            } catch (SQLException e) {
                System.err.println("updateAccountBalance(SQL) error: " + e.getMessage());
                return false;
            }
        } else {
            Account a = memAccounts.get(accountId);
            if (a != null) {
                a.setBalance(newBalance);
                return true;
            } else return false;
        }
    }

    // --------------------------
    // Transaction operations
    // --------------------------
    public boolean addTransaction(TransactionRecord tr) {
        if (conn != null) {
            String sql = "INSERT INTO transactions (transaction_id, account_id, transaction_type, amount, transaction_date) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, tr.getTransactionId());
                ps.setString(2, tr.getAccountId());
                ps.setString(3, tr.getTransactionType());
                ps.setDouble(4, tr.getAmount());
                ps.setString(5, tr.getTransactionDate().toString());
                ps.executeUpdate();
                return true;
            } catch (SQLException e) {
                System.err.println("addTransaction(SQL) error: " + e.getMessage());
                return false;
            }
        } else {
            memTransactions.put(tr.getTransactionId(), tr);
            return true;
        }
    }

    public List<TransactionRecord> getTransactionsByAccountId(String accountId) {
        if (conn != null) {
            List<TransactionRecord> out = new ArrayList<>();
            String sql = "SELECT transaction_id, account_id, transaction_type, amount, transaction_date FROM transactions WHERE account_id = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, accountId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        out.add(new TransactionRecord(rs.getString("transaction_id"), rs.getString("account_id"), rs.getString("transaction_type"), rs.getDouble("amount"), LocalDateTime.parse(rs.getString("transaction_date"))));
                    }
                }
            } catch (SQLException e) {
                System.err.println("getTransactionsByAccountId(SQL) error: " + e.getMessage());
            }
            return out;
        } else {
            List<TransactionRecord> out = new ArrayList<>();
            for (TransactionRecord t : memTransactions.values()) if (t.getAccountId().equals(accountId)) out.add(t);
            return out;
        }
    }

    // --------------------------
    // Service Request operations
    // --------------------------
    public boolean addServiceRequest(ServiceRequest req) {
        if (conn != null) {
            String sql = "INSERT INTO service_requests (request_id, customer_id, request_type, description, status, assigned_staff_id, creation_date) VALUES (?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, req.getRequestId());
                ps.setString(2, req.getCustomerId());
                ps.setString(3, req.getRequestType());
                ps.setString(4, req.getDescription());
                ps.setString(5, req.getStatus());
                ps.setString(6, req.getAssignedStaffId());
                ps.setString(7, req.getCreationDate().toString());
                ps.executeUpdate();
                return true;
            } catch (SQLException e) {
                System.err.println("addServiceRequest(SQL) error: " + e.getMessage());
                return false;
            }
        } else {
            memRequests.put(req.getRequestId(), req);
            return true;
        }
    }

    public List<ServiceRequest> getAllServiceRequests() {
        if (conn != null) {
            List<ServiceRequest> out = new ArrayList<>();
            String sql = "SELECT request_id, customer_id, request_type, description, status, assigned_staff_id, creation_date FROM service_requests";
            try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
                while (rs.next()) {
                    out.add(new ServiceRequest(rs.getString("request_id"), rs.getString("customer_id"), rs.getString("request_type"), rs.getString("description"), rs.getString("status"), rs.getString("assigned_staff_id"), LocalDateTime.parse(rs.getString("creation_date"))));
                }
            } catch (SQLException e) {
                System.err.println("getAllServiceRequests(SQL) error: " + e.getMessage());
            }
            return out;
        } else {
            return new ArrayList<>(memRequests.values());
        }
    }

    public boolean assignStaffToRequest(String requestId, String staffId) {
        if (conn != null) {
            String sql = "UPDATE service_requests SET assigned_staff_id = ?, status = ? WHERE request_id = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, staffId);
                ps.setString(2, "IN_PROGRESS");
                ps.setString(3, requestId);
                int updated = ps.executeUpdate();
                return updated > 0;
            } catch (SQLException e) {
                System.err.println("assignStaffToRequest(SQL) error: " + e.getMessage());
                return false;
            }
        } else {
            ServiceRequest r = memRequests.get(requestId);
            if (r != null) {
                r.setAssignedStaffId(staffId);
                r.setStatus("IN_PROGRESS");
                return true;
            } else return false;
        }
    }

    // --------------------------
    // Staff operations
    // --------------------------
    public boolean addStaff(Staff staff) {
        if (conn != null) {
            String sql = "INSERT INTO staff (staff_id, name, role) VALUES (?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, staff.getStaffId());
                ps.setString(2, staff.getName());
                ps.setString(3, staff.getRole());
                ps.executeUpdate();
                return true;
            } catch (SQLException e) {
                System.err.println("addStaff(SQL) error: " + e.getMessage());
                return false;
            }
        } else {
            memStaff.put(staff.getStaffId(), staff);
            return true;
        }
    }

    public List<Staff> getAllStaff() {
        if (conn != null) {
            List<Staff> out = new ArrayList<>();
            String sql = "SELECT staff_id, name, role FROM staff";
            try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
                while (rs.next()) {
                    out.add(new Staff(rs.getString("staff_id"), rs.getString("name"), rs.getString("role")));
                }
            } catch (SQLException e) {
                System.err.println("getAllStaff(SQL) error: " + e.getMessage());
            }
            return out;
        } else {
            return new ArrayList<>(memStaff.values());
        }
    }
}

// --------------------------
// CRMService: Business Logic layer
// --------------------------
class CRMService {
    private final DatabaseManager db;

    public CRMService(DatabaseManager db) {
        this.db = db;
    }

    public void addCustomer(String name, String email, String kyc) {
        String id = UUID.randomUUID().toString();
        Customer c = new Customer(id, name, email, kyc);
        if (db.addCustomer(c)) System.out.println("Added: " + c);
        else System.out.println("Failed to add customer.");
    }

    public void viewAllCustomers() {
        List<Customer> all = db.getAllCustomers();
        if (all.isEmpty()) System.out.println("No customers found.");
        else all.forEach(System.out::println);
    }

    public void createAccount(String customerId, String accountType, double initialDeposit) {
        // verify customer exists
        Customer c = db.getCustomerById(customerId);
        if (c == null) {
            System.out.println("Customer not found with id: " + customerId);
            return;
        }
        String accId = UUID.randomUUID().toString();
        Account a = new Account(accId, customerId, accountType, initialDeposit);
        if (db.addAccount(a)) {
            // add initial deposit transaction record if deposit > 0
            if (initialDeposit > 0) {
                TransactionRecord tr = new TransactionRecord(UUID.randomUUID().toString(), accId, "DEPOSIT", initialDeposit, LocalDateTime.now());
                db.addTransaction(tr);
            }
            System.out.println("Account created: " + a);
        } else {
            System.out.println("Failed to create account.");
        }
    }

    public void viewAccountsForCustomer(String customerId) {
        List<Account> accounts = db.getAccountsByCustomerId(customerId);
        if (accounts.isEmpty()) System.out.println("No accounts for customer " + customerId);
        else accounts.forEach(System.out::println);
    }

    public void transferFunds(String fromAccountId, String toAccountId, double amount) {
        if (amount <= 0) {
            System.out.println("Amount must be positive.");
            return;
        }
        Account from = db.getAccountById(fromAccountId);
        Account to = db.getAccountById(toAccountId);
        if (from == null || to == null) {
            System.out.println("One of the accounts not found.");
            return;
        }
        if (from.getBalance() < amount) {
            System.out.println("Insufficient balance.");
            return;
        }
        double newFrom = from.getBalance() - amount;
        double newTo = to.getBalance() + amount;
        boolean ok1 = db.updateAccountBalance(fromAccountId, newFrom);
        boolean ok2 = db.updateAccountBalance(toAccountId, newTo);
        if (ok1 && ok2) {
            // Add transaction records for both accounts
            db.addTransaction(new TransactionRecord(UUID.randomUUID().toString(), fromAccountId, "TRANSFER_OUT", amount, LocalDateTime.now()));
            db.addTransaction(new TransactionRecord(UUID.randomUUID().toString(), toAccountId, "TRANSFER_IN", amount, LocalDateTime.now()));
            System.out.println("Transfer successful.");
        } else {
            System.out.println("Transfer failed during update.");
        }
    }

    public void raiseServiceRequest(String customerId, String type, String description) {
        Customer c = db.getCustomerById(customerId);
        if (c == null) {
            System.out.println("Customer not found.");
            return;
        }
        String reqId = UUID.randomUUID().toString();
        ServiceRequest req = new ServiceRequest(reqId, customerId, type, description, "PENDING", null, LocalDateTime.now());
        if (db.addServiceRequest(req)) System.out.println("Service request created: " + req);
        else System.out.println("Failed to create service request.");
    }

    public void viewAllServiceRequests() {
        List<ServiceRequest> all = db.getAllServiceRequests();
        if (all.isEmpty()) System.out.println("No service requests.");
        else all.forEach(System.out::println);
    }

    public void addStaff(String name, String role) {
        String id = UUID.randomUUID().toString();
        Staff s = new Staff(id, name, role);
        if (db.addStaff(s)) System.out.println("Added staff: " + s);
        else System.out.println("Failed to add staff.");
    }

    public void viewAllStaff() {
        List<Staff> all = db.getAllStaff();
        if (all.isEmpty()) System.out.println("No staff.");
        else all.forEach(System.out::println);
    }

    public void assignStaffToRequest(String requestId, String staffId) {
        boolean ok = db.assignStaffToRequest(requestId, staffId);
        if (ok) System.out.println("Assigned staff " + staffId + " to request " + requestId);
        else System.out.println("Failed to assign staff (check IDs).");
    }

    public void viewTransactionsForAccount(String accountId) {
        List<TransactionRecord> txns = db.getTransactionsByAccountId(accountId);
        if (txns.isEmpty()) System.out.println("No transactions.");
        else txns.forEach(System.out::println);
    }
}

// --------------------------
// Main: CLI
// --------------------------
public class Main {
    public static void main(String[] args) {
        DatabaseManager dbManager = new DatabaseManager();
        CRMService service = new CRMService(dbManager);
        Scanner sc = new Scanner(System.in);
        boolean running = true;

        System.out.println("Welcome to Banking CRM");

        while (running) {
            System.out.println("\nMain Menu:");
            System.out.println("1) Customer Management");
            System.out.println("2) Account Operations");
            System.out.println("3) Service Requests");
            System.out.println("4) Staff Management");
            System.out.println("5) Exit");
            System.out.print("choice> ");
            String ch = sc.nextLine().trim();
            try {
                switch (ch) {
                    case "1": customerMenu(service, sc); break;
                    case "2": accountMenu(service, sc); break;
                    case "3": serviceRequestMenu(service, sc); break;
                    case "4": staffMenu(service, sc); break;
                    case "5": running = false; break;
                    default: System.out.println("Invalid choice.");
                }
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
        sc.close();
        System.out.println("Goodbye!");
    }

    private static void customerMenu(CRMService service, Scanner sc) {
        System.out.println("\nCustomer Menu:");
        System.out.println("1) Add Customer");
        System.out.println("2) View All Customers");
        System.out.println("3) Back");
        System.out.print("choice> ");
        String c = sc.nextLine().trim();
        switch (c) {
            case "1":
                System.out.print("Name: "); String name = sc.nextLine().trim();
                System.out.print("Email: "); String email = sc.nextLine().trim();
                System.out.print("KYC details (brief): "); String kyc = sc.nextLine().trim();
                service.addCustomer(name, email, kyc);
                break;
            case "2":
                service.viewAllCustomers();
                break;
            default:
                break;
        }
    }

    private static void accountMenu(CRMService service, Scanner sc) {
        System.out.println("\nAccount Menu:");
        System.out.println("1) Create account for customer");
        System.out.println("2) View accounts for customer");
        System.out.println("3) Transfer funds");
        System.out.println("4) View transactions for account");
        System.out.println("5) Back");
        System.out.print("choice> ");
        String c = sc.nextLine().trim();
        switch (c) {
            case "1":
                System.out.print("Customer ID: "); String custId = sc.nextLine().trim();
                System.out.print("Account type (SAVINGS/CURRENT): "); String type = sc.nextLine().trim();
                System.out.print("Initial deposit (numeric): "); double dep = Double.parseDouble(sc.nextLine().trim());
                service.createAccount(custId, type, dep);
                break;
            case "2":
                System.out.print("Customer ID: "); String cid = sc.nextLine().trim();
                service.viewAccountsForCustomer(cid);
                break;
            case "3":
                System.out.print("From account ID: "); String from = sc.nextLine().trim();
                System.out.print("To account ID: "); String to = sc.nextLine().trim();
                System.out.print("Amount: "); double amt = Double.parseDouble(sc.nextLine().trim());
                service.transferFunds(from, to, amt);
                break;
            case "4":
                System.out.print("Account ID: "); String acc = sc.nextLine().trim();
                service.viewTransactionsForAccount(acc);
                break;
            default: break;
        }
    }

    private static void serviceRequestMenu(CRMService service, Scanner sc) {
        System.out.println("\nService Requests Menu:");
        System.out.println("1) Raise request");
        System.out.println("2) View all requests");
        System.out.println("3) Assign staff to request");
        System.out.println("4) Back");
        System.out.print("choice> ");
        String c = sc.nextLine().trim();
        switch (c) {
            case "1":
                System.out.print("Customer ID: "); String cust = sc.nextLine().trim();
                System.out.print("Request type: "); String type = sc.nextLine().trim();
                System.out.print("Description: "); String desc = sc.nextLine().trim();
                service.raiseServiceRequest(cust, type, desc);
                break;
            case "2":
                service.viewAllServiceRequests();
                break;
            case "3":
                System.out.print("Request ID: "); String rid = sc.nextLine().trim();
                System.out.print("Staff ID: "); String sid = sc.nextLine().trim();
                service.assignStaffToRequest(rid, sid);
                break;
            default: break;
        }
    }

    private static void staffMenu(CRMService service, Scanner sc) {
        System.out.println("\nStaff Menu:");
        System.out.println("1) Add staff");
        System.out.println("2) View all staff");
        System.out.println("3) Back");
        System.out.print("choice> ");
        String c = sc.nextLine().trim();
        switch (c) {
            case "1":
                System.out.print("Name: "); String name = sc.nextLine().trim();
                System.out.print("Role: "); String role = sc.nextLine().trim();
                service.addStaff(name, role);
                break;
            case "2":
                service.viewAllStaff();
                break;
            default: break;
        }
    }
}
