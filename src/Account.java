import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Account implements Serializable {
    private String accountId;
    private double balance;
    private List<Transaction> transactions;
    private String ownerName;
    private String pin;
    private static final Map<String, Double> exchangeRates = Map.of(
            "EUR", 1.13, "USD", 1.24, "AUD", 1.80, "CNY", 8.70, "CHF", 1.07);
    private int failedLoginAttempts = 0;
    private long lastFailedLoginTime = 0;

    public int getFailedLoginAttempts() {
        return failedLoginAttempts;
    }

    public void setFailedLoginAttempts(int attempts) {
        this.failedLoginAttempts = attempts;
    }

    public long getLastFailedLoginTime() {
        return lastFailedLoginTime;
    }

    public void setLastFailedLoginTime(long time) {
        this.lastFailedLoginTime = time;
    }

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
