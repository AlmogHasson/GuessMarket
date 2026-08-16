package engine;
import generated.GMOptions;

import java.util.ArrayList;
import java.util.List;

public class Option {
    protected String option;
    private int sharesBought=0;

    public Option(String gmOption) {
        this.option = gmOption;
        this.sharesBought=0;
    }

    //getters
    public String getOption() {
        return option;
    }

    public int getSharesBought() {
        return sharesBought;
    }
    public void addSharesBought(int shares) {
        this.sharesBought += shares;
    }
}
