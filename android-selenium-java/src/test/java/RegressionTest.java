/*
 * Copyright 2026 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.options.UiAutomator2Options;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class RegressionTest {

  private AndroidDriver driver;

  @BeforeEach
  public void setUp() throws MalformedURLException {
    // Configure UiAutomator2Options for Appium to automate Chrome Canary on an Android emulator
    UiAutomator2Options options = new UiAutomator2Options()
        .setPlatformName("Android")
        .setDeviceName("Android Emulator")
        .setAutomationName("UiAutomator2")
        .withBrowserName("Chrome")
        .setNoReset(true)
        .setUiautomator2ServerInstallTimeout(java.time.Duration.ofMillis(60000));

    // Allow overriding via system properties for CI
    String androidPackage = System.getProperty("androidPackage", "com.chrome.canary");
    String chromedriverPath = System.getProperty("chromedriverExecutable", "/usr/local/google/home/sadym/chromedriver-bug-repro/chromedriver/linux-139.0.7258.154/chromedriver-linux64/chromedriver");

    // Force using Chrome Canary (or specified package)
    options.setCapability("appium:chromeOptions", Map.of("androidPackage", androidPackage));
    
    // Set ChromeDriver path if provided
    if (chromedriverPath != null && !chromedriverPath.isEmpty()) {
        options.setCapability("appium:chromedriverExecutable", chromedriverPath);
    }

    // Initialize AndroidDriver, connecting to the Appium server (usually http://127.0.0.1:4723)
    driver = new AndroidDriver(new URL("http://127.0.0.1:4723"), options);
    driver.manage().timeouts().pageLoadTimeout(java.time.Duration.ofSeconds(60));
    driver.manage().timeouts().implicitlyWait(java.time.Duration.ofSeconds(30));
  }

  @AfterEach
  public void tearDown() {
    if (driver != null) {
      driver.quit();
    }
  }

  @Test
  public void verifySetup_shouldBeAbleToNavigateToGoogleCom() {
    // Navigate to a URL
    driver.get("https://www.google.com");
    // Assert that the navigation was successful
    assertEquals("Google", driver.getTitle());
  }

  @Test
  public void ISSUE_REPRODUCTION() {
    // Create a simple HTML page with a button far down the page
    String html = "data:text/html," +
        "<html><body style='margin:0; padding:0;'>" +
        "<div style='height: 2000px; background: lightgrey;'>Spacer</div>" +
        "<button id='login' style='height: 50px; width: 100px;'>Login</button>" +
        "<br><br>" +
        "<input type='radio' id='type_stock'>Stock</input>" +
        "<div style='height: 1000px;'>Footer</div>" +
        "</body></html>";
    driver.get(html);

    System.out.println("Chrome version: " + driver.getCapabilities().getBrowserVersion());

    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

    WebElement loginButton = driver.findElement(By.id("login"));
    
    // Check initial status - it should be false if out of viewport in some mobile contexts?
    // Actually Selenium isDisplayed usually returns true if it's in the DOM and not hidden by CSS
    // regardless of viewport. But mobile rendering can be different.
    System.out.println("Login button isDisplayed (before scroll): " + loginButton.isDisplayed());

    // Attempt to wait for the button to be clickable
    // verified that this fails on affected versions because isDisplayed() returns false
    wait.until(ExpectedConditions.elementToBeClickable(loginButton));
    loginButton.click();

    // Verify radio button as well
    WebElement radioButton = driver.findElement(By.cssSelector("input#type_stock"));
    System.out.println("Radio button isDisplayed: " + radioButton.isDisplayed());
    wait.until(ExpectedConditions.elementToBeClickable(radioButton));
    radioButton.click();
  }
}
