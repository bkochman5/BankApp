import java.util.*;
import java.util.stream.Collectors;
import java.io.Serializable;
import java.io.ObjectOutputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.util.Map;
import java.util.logging.Logger;


class Account implements Serializable {
    private final String accountId;
    private double balance;
    private final List<Transaction> transactions;
    private final String ownerName;
    private final String pin;
    private static final Map<String, Double> exchangeRates = Map.of(
            "EUR", 1.13, "USD", 1.24, "AUD", 1.80, "CNY", 8.70, "CHF", 1.07);
    private int failedLoginAttempts = 0;
    private long lastFailedLoginTime = 0;

    public Account(String accountId, String ownerName, String pin, double initialBalance) {
        this.accountId = accountId;
        this.ownerName = ownerName;
        this.pin = pin;
        this.balance = initialBalance;
        this.transactions = new ArrayList<>();
        this.transactions.add(new Transaction(new Date(), initialBalance, "Initial Deposit", "GBP"));
    }
    public double getBalance() {
        return balance;
    }

    public void deposit(double amount, String currency) {
        if (amount < 0) {
            System.out.println("Cannot deposit a negative amount.");
            return;
        }
        if (!currency.equals("GBP") && !exchangeRates.containsKey(currency)) {
            System.out.println("Currency not supported.");
            return;
        }
        double convertedAmount = convertCurrency(amount, currency, "GBP");
        if (convertedAmount == 0) {
            System.out.println("Error in currency conversion.");
            return;
        }
        this.balance += convertedAmount;
        this.transactions.add(new Transaction(new Date(), convertedAmount, "Deposit", currency));
    }

    public double convertCurrency(double amount, String fromCurrency, String toCurrency) {
        double rateFrom = exchangeRates.getOrDefault(fromCurrency, 0.0);
        double rateTo = exchangeRates.getOrDefault(toCurrency, 0.0);
        if (rateFrom == 0 || rateTo == 0) return 0;  // Return 0 if currency not found
        return amount * (rateFrom / rateTo);
    }

    public List<Transaction> getTransactions(Date startDate, Date endDate) {
        return transactions.stream()
                .filter(t -> t.getDate().after(startDate) && t.getDate().before(endDate))
                .collect(Collectors.toList());
    }

    public void applyInterest(double rate) {
        double interest = balance * rate;
        balance += interest;
        transactions.add(new Transaction(new Date(), interest, "Interest Applied", "GBP"));
    }


    public boolean withdraw(double amount) {
        if (amount <= balance) {
            this.balance -= amount;
            this.transactions.add(new Transaction(new Date(), -amount, "Withdrawal", "GBP"));
            return true;
        }
        return false;
    }

    public List<Transaction> getTransactions() {
        return transactions;
    }

    public boolean validatePIN(String pinAttempt) {
        return this.pin.equals(pinAttempt);
    }
}

class Transaction implements Serializable {
    private final Date date;
    private final double amount;
    private final String description;
    private final String currency;  // Add this if you're using currency

    public Transaction(Date date, double amount, String description, String currency) {
        this.date = date;
        this.amount = amount;
        this.description = description;
        this.currency = currency;  // Initialize this if you're using currency
    }

    public Date getDate() {
        return date;
    }

    @Override
    public String toString() {
        return date.toString() + ": " + description + " £" + amount + " " + currency;  // Adjusted to include currency
    }
}

class Bank {
    private Map<String, Account> accounts = new HashMap<>();

    public boolean login(String accountId, String pinAttempt) {
        Account account = accounts.get(accountId);
        if (account == null) {
            System.out.println("Account ID not found.");
            return false;
        }

        // Check for lockout condition
        if (account.failedLoginAttempts >= 3 &&
                (System.currentTimeMillis() - account.lastFailedLoginTime < 60000)) {
            System.out.println("Account is temporarily locked. Please try again later.");
            return false;
        }

        if (account.validatePIN(pinAttempt)) {
            System.out.println("Login successful.");
            account.failedLoginAttempts = 0; // Reset failed attempts on successful login
            return true;
        } else {
            account.failedLoginAttempts++;
            account.lastFailedLoginTime = System.currentTimeMillis();
            System.out.println("Invalid PIN. Attempt " + account.failedLoginAttempts + "/3");
            if (account.failedLoginAttempts >= 3) {
                System.out.println("Account is locked. Please try again after one minute.");
            }
            return false;
        }
    }

    public void createAccount(String accountId, String ownerName, String pin, double initialBalance) {
        if (!accounts.containsKey(accountId)) {
            accounts.put(accountId, new Account(accountId, ownerName, pin, initialBalance));
            saveAccountsToFile();  // Save immediately after creating an account
            System.out.println("Account successfully created.");
        } else {
            System.out.println("An account with this ID already exists.");
        }
    }

    public Account getAccount(String accountId) {
        return accounts.get(accountId);
    }

    public boolean transfer(String fromAccountId, String toAccountId, double amount) {
        Account fromAccount = accounts.get(fromAccountId);
        Account toAccount = accounts.get(toAccountId);
        if (fromAccount != null && toAccount != null && fromAccount.withdraw(amount)) {
            toAccount.deposit(amount, "GBP");
            return true;
        }
        return false;
    }

    public void deposit(String accountId, double amount, String currency) {
        Account account = accounts.get(accountId);
        if (account != null) {
            account.deposit(amount, currency);
            saveAccountsToFile();
        }
    }

    public void depositToAccount(String accountId, double amount, String currency) {
        Account account = getAccount(accountId);
        if (account != null) {
            account.deposit(amount, currency);
            saveAccountsToFile();
        } else {
            System.out.println("Account not found");
        }
    }

    public void applyInterestToAllAccounts(double rate) {
        for (Account account : accounts.values()) {
            account.applyInterest(rate);
        }
    }

    public void saveAccountsToFile() {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream("accounts.dat"))) {
            out.writeObject(accounts);
        } catch (IOException e) {
            System.out.println("Error saving accounts: " + e.getMessage());
        }
    }

    public void loadAccountsFromFile() {
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream("accounts.dat"))) {
            accounts = (Map<String, Account>) in.readObject();
        } catch (FileNotFoundException e) {
            System.out.println("No existing accounts file found.");
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Error loading accounts: " + e.getMessage());
        }
    }

}

public class BankApp {
    private static final Scanner scanner = new Scanner(System.in);
    private static final Logger logger = Logger.getLogger(BankApp.class.getName());

    public static void main(String[] args) {
        Bank bank = new Bank();
        bank.loadAccountsFromFile(); // Load accounts at start




        boolean exit = false;
        while (!exit) {
            System.out.println("Welcome to the Bank! What would you like to do?");
            System.out.println("1. Create Account");
            System.out.println("2. Log In");
            System.out.println("3. Exit");
            System.out.print("Enter your choice: ");
            int choice = scanner.nextInt();
            scanner.nextLine();

            switch (choice) {
                case 1:
                    createAccountUI(bank);
                    break;
                case 2: // Log In
                    System.out.print("Please enter your Account ID: ");
                    String accountId = scanner.nextLine();
                    System.out.print("Enter your PIN: ");
                    String pin = scanner.nextLine();

                    if (bank.login(accountId, pin)) {
                        Account account = bank.getAccount(accountId);
                        accountMenu(account);
                    }
                    break;
                case 3:
                    exit = true;
                    break;
                default:
                    System.out.println("Invalid choice. Please enter 1, 2, or 3.");
                    break;
            }
        }
        bank.saveAccountsToFile(); //saving account before exiting
//        System.out.println("Accounts saved to file.");
        System.out.println("Thank you for using our banking system!");
    }


    private static void createAccountUI(Bank bank) {
        System.out.print("Enter Account ID: ");
        String accountId = scanner.nextLine();
        System.out.print("Enter Owner Name: ");
        String ownerName = scanner.nextLine();
        System.out.print("Create a PIN: ");
        String pin = scanner.nextLine();
        System.out.print("Initial Deposit Amount: £");
        double initialDeposit = scanner.nextDouble();
        scanner.nextLine();

        bank.createAccount(accountId, ownerName, pin, initialDeposit);
    }

    private static void convertCurrencyUI(Account account) {
        System.out.println("Your current balance: £" + account.getBalance());
        System.out.print("Enter the currency to convert to (EUR, USD, AUD, CNY, CHF,): ");
        String targetCurrency = scanner.nextLine().trim().toUpperCase();
        double convertedAmount = account.convertCurrency(account.getBalance(), "GBP", targetCurrency);
        if (convertedAmount != 0) {
            System.out.println("Your balance in " + targetCurrency + ": " + String.format("%.2f", convertedAmount));
        } else {
            System.out.println("Invalid currency or conversion error.");
        }
    }

    private static void accountMenu(Account account) {
        boolean back = false;
        while (!back) {
            System.out.println("Welcome! Your current balance is £" + account.getBalance());
            System.out.println("1. Deposit");
            System.out.println("2. Withdraw");
            System.out.println("3. View Transactions");
            System.out.println("4. Convert Currency");
            System.out.println("5. Check Balance");
            System.out.println("6. Log Out");
            System.out.print("What would you like to do? ");
            int choice = safeNextInt(scanner);
            scanner.nextLine();  // Consume newline left-over

            switch (choice) {
                case 1:
                    System.out.print("Enter the amount to deposit: £");
                    double depositAmount = scanner.nextDouble();
                    scanner.nextLine();  // Consume newline
                    System.out.print("Enter the currency (default GBP): ");
                    String currency = scanner.nextLine();
                    currency = currency.isEmpty() ? "GBP" : currency.trim();
                    account.deposit(depositAmount, currency);
                    System.out.println("Deposited £" + depositAmount + " " + currency);
                    break;
                case 2:
                    System.out.print("Enter the amount to withdraw: £");
                    double withdrawAmount = scanner.nextDouble();
                    if (account.withdraw(withdrawAmount)) {
                        System.out.println("Withdrew £" + withdrawAmount);
                    } else {
                        System.out.println("Insufficient funds.");
                    }
                    break;
                case 3:
                    System.out.println("Transaction History:");
                    for (Transaction transaction : account.getTransactions()) {
                        System.out.println(transaction);
                    }
                    break;
                case 4:
                    convertCurrencyUI(account);
                    break;
                case 5:
                    System.out.println("Current Balance: £" + account.getBalance());
                    break;
                case 6:
                    back = true;
                    break;
                default:
                    System.out.println("Invalid choice. Please choose between 1-5.");
                    break;
            }
        }


    } private static int safeNextInt(Scanner scanner) {
        while (!scanner.hasNextInt()) {
            System.out.println("Invalid input. Please enter a valid number.");
            scanner.next(); // Consume the invalid input
        }
        return scanner.nextInt();
    }

    private static String safeNextLine(Scanner scanner) {
        return scanner.nextLine().trim();
    }
}
