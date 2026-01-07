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
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.NoSuchWindowException;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WindowType;
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

  /**
   * Verified that the issue is NOT reproducible.
   *
   * <p>This test attempts to reproduce the reported bug where ChromeDriver returns null instead of
   * throwing NoSuchWindowException when taking a screenshot of a window closed via JavaScript.
   *
   * <p>If this test PASSES, it means NoSuchWindowException WAS thrown as expected, and the bug
   * is NOT present.
   *
   * @see <a href="https://issuetracker.google.com/issues/42323630">Bug Report</a>
   */
  @Test
  public void testScreenshotOnSelfClosedWindowThrowsNoSuchWindowException_NotReproducible() {
    // Navigate to a URL
    driver.get("https://www.google.com");

    // Open a new window and switch to it
    driver.switchTo().newWindow(WindowType.WINDOW);

    // Close the current window via JavaScript.
    ((org.openqa.selenium.JavascriptExecutor) driver).executeScript("window.close()");

    // Attempting to take a screenshot should now throw a NoSuchWindowException.
    assertThrows(
        NoSuchWindowException.class,
        () -> {
          ((TakesScreenshot) driver).getScreenshotAs(OutputType.BASE64);
        },
        "Expected NoSuchWindowException to be thrown when taking a screenshot of a self-closed window, "
            + "indicating the bug is NOT present. If this failed, the bug (returning null) might be reproduced.");
  }
}
