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
  public void testSlotDefaultContentText() {
    // 1. Load the HTML file with the custom element and slot.
    java.net.URL resource = getClass().getClassLoader().getResource("slot_test.html");
    if (resource == null) {
      throw new IllegalStateException("slot_test.html not found in classpath");
    }
    driver.get(resource.toString());

    // 2. Find the custom element.
    // In Selenium, interacting with shadow DOM elements directly can be tricky.
    // The bug report mentions "obtain text of an element", which implies the top-level element.
    // If the bug is about the shadow DOM content not being exposed to the top-level element's text,
    // then we try to get the text of the custom element itself.
    // If this doesn't work, we'd need to go into the shadow DOM, which is more complex.

    // For the purpose of reproducing this bug, we assume that "obtain text of an element" refers
    // to getting the text content of the custom element, and that the WebDriver should
    // implicitly traverse the shadow DOM and include the slot's default content.
    // This is based on the general expectation that WebDriver's .getText() should return
    // all visible text, including text within shadow DOM slots if not overridden.

    org.openqa.selenium.WebElement customElement = driver.findElement(org.openqa.selenium.By.tagName("custom-element"));

    // 3. Get the text of the custom element.
    String elementText = customElement.getText();

    // 4. Assert that the obtained text *is* "Default Slot Content".
    // This test is designed to *fail* if the bug exists, because the bug states that
    // the default content of the <slot> element is *not* considered.
    // According to the bug report, the expected result (if the bug were fixed) is that
    // the text *is* considered. So, we assert for the *expected* behavior.
    assertEquals("Default Slot Content", elementText, "The text of the custom element should include the default slot content.");
  }
}
