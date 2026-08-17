package engine;

public class Option {
    protected String optionName;
    private int sharesBought=0;

    public Option(String gmOption) {
        this.optionName = gmOption;
        this.sharesBought=0;
    }

    //getters
    public int getSharesBought() {
        return sharesBought;
    }
    public void addSharesBought(int shares) {
        this.sharesBought += shares;
    }

    public String getName() {
        return optionName;
    }

    public String getOptionName() {
        return optionName;
    }
}
