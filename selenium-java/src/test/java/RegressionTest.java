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
  public void issue42323069_shouldThrowWebDriverExceptionWhenSendingNonEnglishKey() {
    // This test reproduces crbug.com/42323069, where a WebDriverException
    // is thrown when attempting to send keys with a non-English keyboard layout.
    // Specifically, the '@' symbol with a German keyboard layout was reported
    // to cause this issue.

    // Expected behavior: A WebDriverException should be thrown.
    // If the exception is NOT thrown, it means the bug is fixed, and the test should fail.
    try {
      driver.get("https://www.google.com");
      org.openqa.selenium.WebElement searchBox = driver.findElement(org.openqa.selenium.By.name("q"));
      searchBox.sendKeys("@");
      // If no exception is thrown, the bug is fixed, so fail the test.
      org.junit.jupiter.api.Assertions.fail("WebDriverException was not thrown, bug 42323069 might be fixed.");
    } catch (org.openqa.selenium.WebDriverException e) {
      // Assert that the exception message contains the expected text.
      String expectedErrorMessage = "unknown error: Cannot construct KeyEvent from non-typeable key";
      org.junit.jupiter.api.Assertions.assertTrue(e.getMessage().contains(expectedErrorMessage),
          "Expected WebDriverException with message containing: '" + expectedErrorMessage + "', but got: " + e.getMessage());
    }
  }
}
