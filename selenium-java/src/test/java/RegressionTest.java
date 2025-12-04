/*
 * Copyright 2025 Google LLC
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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.openqa.selenium.chrome.ChromeOptions;

public class RegressionTest {

  private WebDriver driver;

  @BeforeEach
  public void setUp() {
    ChromeOptions options = new ChromeOptions();
    options.addArguments("--headless");
    options.addArguments("--no-sandbox");

    // By default, the test uses the latest stable Chrome version.
    // Replace the "stable" with the specific browser version if needed,
    // e.g. 'canary', '115' or '144.0.7534.0' for example.
    options.setBrowserVersion("stable");

    ChromeDriverService service =
        new ChromeDriverService.Builder()
            .withLogFile(new java.io.File("chromedriver.log"))
            .withVerbose(true)
            .build();

    driver = new ChromeDriver(service, options);
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
    // This test reproduces the bug where sending the plain paste shortcut (Ctrl + Shift + V)
    // fires duplicate paste events on a body tag instead of just one in ChromeDriver.

    // 1. Navigate to a data URL with an onpaste event listener on the body tag.
    // The onpaste event will increment a counter in the JavaScript context.
    driver.get("data:text/html;charset=utf-8,<html><body onpaste=\"window.pasteCount = (window.pasteCount || 0) + 1; console.log('Paste event fired: ' + window.pasteCount);\"></body></html>");

    // 2. Initialize the paste event counter in the JavaScript context.
    ((JavascriptExecutor) driver).executeScript("window.pasteCount = 0;");

    // 3. Find the body element.
    WebElement body = driver.findElement(By.tagName("body"));

    // 4. Perform the plain paste action (Ctrl + Shift + V) on the body element.
    // According to the bug report, this should fire 3 paste events.
    new Actions(driver)
        .keyDown(Keys.CONTROL)
        .keyDown(Keys.SHIFT)
        .sendKeys("v")
        .keyUp(Keys.SHIFT)
        .keyUp(Keys.CONTROL)
        .perform();

    // Give some time for the events to propagate and the counter to update.
    try {
      Thread.sleep(100);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    // 5. Retrieve the value of the paste event counter.
    Long pasteCount = (Long) ((JavascriptExecutor) driver).executeScript("return window.pasteCount;");

    // 6. Assert that the paste event count is not 1.
    // The test is expected to FAIL if the bug exists (i.e., pasteCount will be 3, not 1).
    // The WebDriver spec states that a single paste event should be fired for a paste action.
    // If the bug is fixed, pasteCount should be 1 and this assertion will pass.
    // If the bug exists, pasteCount will be 3 and this assertion will fail as expected for reproduction.
    assertEquals(1L, pasteCount, "Expected 1 paste event, but received " + pasteCount + " events. This indicates the bug is present.");
  }
}
