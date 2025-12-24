package com.example.demo;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Playwright;

@Configuration
public class PlaywrightConfig {

    @Bean(destroyMethod = "close")
    public Playwright playwright() {
        return Playwright.create();
    }

    @Bean(destroyMethod = "close")
    public Browser browser(Playwright playwright) {
        return playwright.chromium().launch(
            new BrowserType.LaunchOptions()
                .setHeadless(true)
                .setArgs(List.of(
                    "--disable-gpu",
                    "--no-sandbox",
                    "--disable-dev-shm-usage",
                    "--disable-extensions",
                    "--disable-background-networking"
                ))
        );
    }
}

