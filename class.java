import java.util.Objects;

enum Currency {
    RUB("RUB", "₽", 100),
    USD("USD", "$", 9_600),
    EUR("EUR", "€", 10_300),
    CNY("CNY", "¥", 1_320);

    private final String code;
    private final String symbol;
    private final long priceInRublesKopecks;

    Currency(String code, String symbol, long priceInRublesKopecks) {
        this.code = code;
        this.symbol = symbol;
        this.priceInRublesKopecks = priceInRublesKopecks;
    }

    public String getCode() { return code; }
    public String getSymbol() { return symbol; }
    public long getPriceInRublesKopecks() { return priceInRublesKopecks; }
}

final class Balance {
    private final long amountKopecks;
    private final Currency currency;

    public Balance(long amountKopecks, Currency currency) {
        if (currency == null) {
            throw new IllegalArgumentException("Валюта не может быть null");
        }
        if (amountKopecks < 0) {
            throw new IllegalArgumentException("Сумма не может быть отрицательной: " + amountKopecks);
        }
        this.amountKopecks = amountKopecks;
        this.currency = currency;
    }

    public long getAmountKopecks() { return amountKopecks; }
    public Currency getCurrency() { return currency; }

    public Balance add(long kopecks) {
        if (kopecks < 0) {
            throw new IllegalArgumentException("Нельзя прибавлять отрицательную сумму: " + kopecks);
        }
        return new Balance(Math.addExact(amountKopecks, kopecks), currency);
    }

    public Balance subtract(long kopecks) {
        if (kopecks < 0) {
            throw new IllegalArgumentException("Нельзя вычитать отрицательную сумму: " + kopecks);
        }
        if (kopecks > amountKopecks) {
            throw new IllegalArgumentException(
                    "Недостаточно средств: попытка списать " + kopecks + ", доступно " + amountKopecks);
        }
        return new Balance(amountKopecks - kopecks, currency);
    }

    public Balance convert(Currency target) {
        if (target == null) {
            throw new IllegalArgumentException("Целевая валюта не может быть null");
        }
        if (target == currency) {
            return this;
        }
        long inRubles = amountKopecks * currency.getPriceInRublesKopecks();
        long inTarget = inRubles / target.getPriceInRublesKopecks();
        return new Balance(inTarget, target);
    }

    @Override
    public String toString() {
        long major = amountKopecks / 100;
        long minor = amountKopecks % 100;
        return String.format("%d,%02d %s", major, minor, currency.getSymbol());
    }
}

final class BankAccount {
    private final String owner;
    private Balance balance;
    private final long withdrawalLimitKopecks;
    private int transferCount;

    public BankAccount(String owner, Balance initialBalance, long withdrawalLimitKopecks) {
        if (owner == null || owner.isBlank()) {
            throw new IllegalArgumentException("Владелец не может быть пустым");
        }
        if (initialBalance == null) {
            throw new IllegalArgumentException("Начальный баланс не может быть null");
        }
        if (withdrawalLimitKopecks < 0) {
            throw new IllegalArgumentException("Лимит не может быть отрицательным");
        }
        this.owner = owner;
        this.balance = initialBalance;
        this.withdrawalLimitKopecks = withdrawalLimitKopecks;
    }

    public String getOwner() { return owner; }
    public Balance getBalance() { return balance; }
    public long getWithdrawalLimitKopecks() { return withdrawalLimitKopecks; }
    public int getTransferCount() { return transferCount; }

    public void deposit(long kopecks) {
        balance = balance.add(kopecks);
    }

    public void withdraw(long kopecks) {
        if (kopecks < 0) {
            throw new IllegalArgumentException("Сумма снятия не может быть отрицательной");
        }
        if (kopecks > withdrawalLimitKopecks) {
            throw new IllegalArgumentException(
                    "Превышен лимит на вывод: " + kopecks + " > " + withdrawalLimitKopecks);
        }
        balance = balance.subtract(kopecks);
    }

    public void transfer(BankAccount to, long kopecks) {
        if (to == null) {
            throw new IllegalArgumentException("Получатель не может быть null");
        }
        if (to == this) {
            throw new IllegalArgumentException("Нельзя перевести самому себе");
        }
        if (kopecks < 0) {
            throw new IllegalArgumentException("Сумма перевода не может быть отрицательной");
        }
        if (kopecks > withdrawalLimitKopecks) {
            throw new IllegalArgumentException("Перевод превышает лимит на вывод отправителя");
        }
        this.balance = this.balance.subtract(kopecks);
        Balance converted = new Balance(kopecks, this.balance.getCurrency()).convert(to.balance.getCurrency());
        to.balance = to.balance.add(converted.getAmountKopecks());
        this.transferCount++;
    }

    @Override
    public String toString() {
        return "BankAccount{" +
                "owner='" + owner + '\'' +
                ", balance=" + balance +
                ", withdrawalLimit=" + new Balance(withdrawalLimitKopecks, balance.getCurrency()) +
                ", transferCount=" + transferCount +
                '}';
    }
}

public class Main {
    public static void main(String[] args) {
        BankAccount alice = new BankAccount("Alice",
                new Balance(100_000, Currency.RUB), 50_000);
        BankAccount bob = new BankAccount("Bob",
                new Balance(0, Currency.USD), 1_000_000);

        System.out.println("=== Старт ===");
        System.out.println(alice);
        System.out.println(bob);

        alice.deposit(20_000);
        System.out.println("\nПосле пополнения: " + alice);

        alice.withdraw(30_000);
        System.out.println("После снятия 300 ₽: " + alice);

        alice.transfer(bob, 40_000);
        System.out.println("\nПосле перевода 400 ₽ от Alice:");
        System.out.println(alice);
        System.out.println(bob);

        System.out.println("\nБаланс Bob в рублях: "
                + bob.getBalance().convert(Currency.RUB));

        System.out.println("\n=== Проверка ошибок ===");

        try {
            alice.withdraw(1_000_000);
        } catch (IllegalArgumentException e) {
            System.out.println("Ошибка снятия: " + e.getMessage());
        }

        try {
            alice.deposit(-100);
        } catch (IllegalArgumentException e) {
            System.out.println("Ошибка пополнения: " + e.getMessage());
        }

        try {
            new Balance(-1, Currency.RUB);
        } catch (IllegalArgumentException e) {
            System.out.println("Ошибка создания Balance: " + e.getMessage());
        }

        try {
            alice.transfer(null, 100);
        } catch (IllegalArgumentException e) {
            System.out.println("Ошибка перевода: " + e.getMessage());
        }

        try {
            alice.transfer(alice, 100);
        } catch (IllegalArgumentException e) {
            System.out.println("Ошибка перевода себе: " + e.getMessage());
        }
    }
}