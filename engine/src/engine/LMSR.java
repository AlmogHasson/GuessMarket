package engine;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class LMSR implements Method,Serializable {
    private final int b;

    public LMSR(int b) {
        this.b = b;
    }

    @Override
    public int getValue() {
        return b;
    }

    @Override
    public double activate(User mm, Event event) {
        return calculateBalance(0, 0); // subsidy = b*ln(2), current default option values already 0.5/0.5
    }

    @Override
    public TradeResult executeTrade(Event event, Map<String, User> users, TradeRequest req) {
        User user = users.get(req.userName());
        if (user == null) {
            throw new IllegalArgumentException("User not found");
        }
        List<Option> opts = event.getOptions();
        Option opt = opts.get(req.optionNumber() - 1);

        double before = calculateBalance(opts.get(0).getTotalSharesBought(), opts.get(1).getTotalSharesBought());
        opt.buyShares(req.shares());
        double after = calculateBalance(opts.get(0).getTotalSharesBought(), opts.get(1).getTotalSharesBought());
        double sharesCost = after - before;

        // Calculate the commission based on the event's commission type
        double commission = "on-purchase".equals(event.getCommission().getCommissionType())
                ? sharesCost * event.getCommission().getValue() / 100 : 0.0;
        double totalCost = sharesCost + commission;

        if (user.getAccountBalance() < totalCost)
            throw new IllegalArgumentException("Insufficient funds");
        user.setAccountBalance(user.getAccountBalance() - totalCost);

        EventTradingStatus ets = event.getEventTradingStatus();
        ets.updateAccountBalance(ets.getAccountBalance() + totalCost);
        ets.updateTotalCommissionPaid(ets.getTotalCommissionPaid() + commission);

        double v = calculateOptionValue(opts.get(0).getTotalSharesBought(), opts.get(1).getTotalSharesBought());
        opts.get(0).updateValue(v);
        opts.get(1).updateValue(1 - v);

        event.getOrCreateHolding(user.getName(), req.optionNumber()).applyBuy(req.shares(), totalCost);

        Trade trade = new Trade(user.getName(), opt.getOptionName(), req.shares(), totalCost, Side.BUY, commission);
        ets.updateHistory(trade);

        return new TradeResult(totalCost, commission, List.of(trade), 0);
    }


    @Override
    public void close(Event event, Map<String, User> users, int winningOptionNumber) {
        EventTradingStatus ets = event.getEventTradingStatus();
        boolean onClose = "on-close".equals(event.getCommission().getCommissionType());
        double percentage = event.getCommission().getValue() / 100.0;
        double totalPaidOut = 0, totalCommission = 0;

        for (var entry : event.getUsersHoldings().entrySet()) {
            Holding holding = entry.getValue()[winningOptionNumber - 1];
            if (holding.getShares() <= 0) continue;
            double gross = holding.getShares(); // d=1 always for LMSR
            double commission = onClose ? gross * percentage : 0.0;
            double payout = gross - commission;
            users.get(entry.getKey()).setAccountBalance(users.get(entry.getKey()).getAccountBalance() + payout);
            totalPaidOut += payout;
            totalCommission += commission;
        }
        ets.updateTotalCommissionPaid(ets.getTotalCommissionPaid() + totalCommission);
        double leftover = ets.getAccountBalance() - totalPaidOut - totalCommission; // subsidy remainder
        // commission goes to the MM's account, leftover subsidy also returns to MM — credit both to the MM
        User MM = users.values().stream()
                .filter(user -> user.isMarketMaker(event.getId()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No market maker found for event " + event.getId()));
        MM.setAccountBalance(MM.getAccountBalance() + leftover + totalCommission);
        ets.updateAccountBalance(0);
        event.getOptions().get(winningOptionNumber - 1).setWinner();
        ets.close();
    }

    /**
     * Current value of the FIRST option, i.e. softmax(q1/b, q2/b).
     * The exponents are shifted by their maximum before exponentiating
     * (log-sum-exp trick). Mathematically identical to exp(a)/(exp(a)+exp(c)),
     * because the common factor exp(-m) cancels top and bottom, but the largest
     * term is now exp(0)=1 so nothing can overflow. The smaller term may
     * underflow to 0.0, which is the correct limit rather than an error.
     */
    public double calculateOptionValue(int firstOptionShares, int secondOptionShares) {
        double a = (double) firstOptionShares / b;
        double c = (double) secondOptionShares / b;
        double m = Math.max(a, c);

        double optionExp = Math.exp(a - m);
        double otherExp = Math.exp(c - m);

        return (optionExp / (optionExp + otherExp));
    }

    //amount in the event pool
    /**
     * Amount in the event pool: b * ln(exp(q1/b) + exp(q2/b)).
     * Rewritten as b * (m + ln(exp(a-m) + exp(c-m))) with m = max(a, c).
     * Same identity, but the argument to exp never exceeds 0, so the
     * ln(Infinity) that produced Infinity in the Paid column cannot occur.
     */
    public double calculateBalance(int firstOptionShares, int secondOptionShares) {
        double a = (double) firstOptionShares / b;
        double c = (double) secondOptionShares / b;
        double m = Math.max(a, c);

        return (b * (m + Math.log(Math.exp(a - m) + Math.exp(c - m))));
    }
}
