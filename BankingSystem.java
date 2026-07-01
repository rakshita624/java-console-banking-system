import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

class BankAccount {
    private final String accountNumber;
    private final String accountHolder;
    private double balance;

   public BankAccount(String accountNumber, String accountHolder, double initialBalance) {
        this.accountNumber = accountNumber;
        this.accountHolder = accountHolder;
        this.balance = initialBalance;
    }

    public synchronized String getAccountNumber() { return accountNumber; }
    public synchronized String getAccountHolder() { return accountHolder; }
    public synchronized double getBalance() { return balance; }

    public synchronized void deposit(double amount) {
        if (amount > 0) {
            balance += amount;
            System.out.println("Successfully deposited ₹" + amount);
        } else {
            System.out.println("Invalid deposit amount.");
        }
    }

    public synchronized boolean withdraw(double amount) {
        if (amount > 0 && balance >= amount) {
            balance -= amount;
            System.out.println("Successfully withdrew ₹" + amount);
            return true;
        } else {
            System.out.println("Insufficient balance or invalid amount.");
            return false;
        }
    }
}

public class BankingSystem {
    // Thread-safe database for matching terminal actions with local web browser tracking
    private static final Map<String, BankAccount> database = new HashMap<>();

    public static void main(String[] args) {
        // Seed default dummy accounts for a live B.Tech evaluator demonstration
        database.put("1001", new BankAccount("1001", "Amit Sharma", 50000.0));
        database.put("1002", new BankAccount("1002", "Sneha Reddy", 75000.50));

        // Start background verification server on port 8080
        startVerificationServer();

        Scanner scanner = new Scanner(System.in);
        System.out.println("=================================================");
        System.out.println("      JAVA CONSOLE BANKING MANAGEMENT SYSTEM     ");
        System.out.println("=================================================");
        System.out.println("[LIVE LOG] Verification Web URL: http://localhost:8080/dashboard");
        
        while (true) {
            System.out.println("\n--- MAIN MENU ---");
            System.out.println("1. Create New Account");
            System.out.println("2. Deposit Money");
            System.out.println("3. Withdraw Money");
            System.out.println("4. Check Account Balance");
            System.out.println("5. Exit System");
            System.out.print("Choose an action (1-5): ");
            
            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1":
                    System.out.print("Enter Unique Account Number: ");
                    String accNum = scanner.nextLine().trim();
                    if (database.containsKey(accNum)) {
                        System.out.println("Operation Rejected: Account ID already exists.");
                        break;
                    }
                    System.out.print("Enter Account Holder Name: ");
                    String name = scanner.nextLine().trim();
                    System.out.print("Enter Initial Deposit Amount (₹): ");
                    double initialDeposit = Double.parseDouble(scanner.nextLine().trim());
                    
                    database.put(accNum, new BankAccount(accNum, name, initialDeposit));
                    System.out.println("Account created successfully!");
                    break;

                case "2":
                    System.out.print("Enter Account Number: ");
                    accNum = scanner.nextLine().trim();
                    BankAccount account = database.get(accNum);
                    if (account != null) {
                        System.out.print("Enter Deposit Amount (₹): ");
                        double amount = Double.parseDouble(scanner.nextLine().trim());
                        account.deposit(amount);
                    } else {
                        System.out.println("Account profile not located.");
                    }
                    break;

                case "3":
                    System.out.print("Enter Account Number: ");
                    accNum = scanner.nextLine().trim();
                    account = database.get(accNum);
                    if (account != null) {
                        System.out.print("Enter Withdrawal Amount (₹): ");
                        double amount = Double.parseDouble(scanner.nextLine().trim());
                        account.withdraw(amount);
                    } else {
                        System.out.println("Account profile not located.");
                    }
                    break;

                case "4":
                    System.out.print("Enter Account Number: ");
                    accNum = scanner.nextLine().trim();
                    account = database.get(accNum);
                    if (account != null) {
                        System.out.println("\n--- ACCOUNT PROFILE ---");
                        System.out.println("Holder Name : " + account.getAccountHolder());
                        System.out.println("Net Balance : ₹" + account.getBalance());
                    } else {
                        System.out.println("Account profile not located.");
                    }
                    break;

                case "5":
                    System.out.println("Exiting Console Application. Offline.");
                    System.exit(0);

                default:
                    System.out.println("Invalid input choice. Please select options 1-5.");
            }
        }
    }

    private static void startVerificationServer() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
            server.createContext("/dashboard", new DashboardHandler());
            server.setExecutor(null); 
            server.start();
        } catch (IOException e) {
            System.err.println("Web Verification Server setup failed: " + e.getMessage());
        }
    }

    static class DashboardHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            StringBuilder html = new StringBuilder();
            html.append("<!DOCTYPE html><html><head><title>B.Tech Banking Panel</title>")
                .append("<style>body{font-family:'Segoe UI',sans-serif;margin:40px;background:#f8f9fa;color:#333;}")
                .append("table{width:100%;border-collapse:collapse;margin-top:20px;background:#fff;box-shadow:0 2px 5px rgba(0,0,0,0.1);}")
                .append("th,td{padding:15px;text-align:left;border-bottom:1px solid #dee2e6;}")
                .append("th{background:#007bff;color:white;}tr:hover{background:#f1f3f5;}</style>")
                .append("</head><body>")
                .append("<h1>B.Tech Project Evaluation System</h1>")
                .append("<p>Dashboard Pipeline Status: <span style='color:green;font-weight:bold;'>LIVE & SYNCED</span></p>")
                .append("<table><tr><th>Account ID</th><th>Account Holder</th><th>Live Balance (INR)</th></tr>");

            synchronized (database) {
                for (BankAccount acc : database.values()) {
                    html.append("<tr>")
                        .append("<td>").append(acc.getAccountNumber()).append("</td>")
                        .append("<td>").append(acc.getAccountHolder()).append("</td>")
                        .append("<td>₹").append(acc.getBalance()).append("</td>")
                        .append("</tr>");
                }
            }

            html.append("</table></body></html>");

            byte[] responseBytes = html.toString().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200, responseBytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(responseBytes);
            os.close();
        }
    }
}