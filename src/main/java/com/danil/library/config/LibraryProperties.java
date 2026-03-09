package com.danil.library.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "library")
public class LibraryProperties {

    private final Loan loan = new Loan();
    private final Fine fine = new Fine();

    public Loan getLoan() { return loan; }
    public Fine getFine() { return fine; }

    public static class Loan {
        private int defaultDays = 14;
        public int getDefaultDays() { return defaultDays; }
        public void setDefaultDays(int defaultDays) { this.defaultDays = defaultDays; }
    }

    public static class Fine {
        private int daily = 10;
        public int getDaily() { return daily; }
        public void setDaily(int daily) { this.daily = daily; }
    }
}
