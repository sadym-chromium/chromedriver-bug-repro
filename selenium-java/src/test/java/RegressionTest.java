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

    ChromeDriverService service = new ChromeDriverService.Builder()
        .withLogFile(new java.io.File("chromedriver.log"))
        .withVerbose(true)
        .build();

    driver = new ChromeDriver(service, options);
    driver.manage().timeouts().pageLoadTimeout(java.time.Duration.ofSeconds(5));
    driver.manage().timeouts().implicitlyWait(java.time.Duration.ofSeconds(2));
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
  public void testAlertInOtherWindowCausesTimeout() throws InterruptedException {
    // Navigate to a blank page in the initial window
    driver.get("about:blank");
    String originalWindowHandle = driver.getWindowHandle();

    // Open a new window and switch to it
    driver.switchTo().newWindow(org.openqa.selenium.WindowType.TAB);
    String newWindowHandle = driver.getWindowHandle();

    // In the new window, navigate to a URL that triggers an alert
    // This will cause an unhandled alert exception if not handled
    driver.get("data:text/html,<script>alert('Hello from other window!');</script>");

    // Switch back to the original window
    driver.switchTo().window(originalWindowHandle);

    driver.getTitle();
    // If we reach here, the bug is NOT reproduced as the driver call did not
    // timeout.
    // Now, test the second part of the bug: "Simple alert checking in current
    // window also does not work."
    try {
      driver.switchTo().alert();
      org.junit.jupiter.api.Assertions.fail(
          "Expected NoAlertPresentException, but an alert was found or another exception occurred when checking in the current window.");
    } catch (org.openqa.selenium.NoAlertPresentException e) {
      System.out.println("Caught NoAlertPresentException as expected, meaning alert checking works correctly.");
      // The bug is NOT reproduced if NoAlertPresentException is thrown.
    } catch (Exception e) {
      System.out.println("Caught unexpected exception during alert checking: " + e.getMessage());
      org.junit.jupiter.api.Assertions.fail("Caught unexpected exception during alert checking: " + e.getMessage());
    }
  }
}
