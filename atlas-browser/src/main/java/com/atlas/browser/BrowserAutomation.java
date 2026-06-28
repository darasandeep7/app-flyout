package com.atlas.browser;

public interface BrowserAutomation {
    BrowserResult inspect(BrowserRequest request);

    BrowserApplicationResult apply(BrowserApplicationRequest request);
}
