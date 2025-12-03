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
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;

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
  public void shouldReturnW3CCompliantCaps() {
    // The bug report (crbug.com/42323175) describes an issue where ChromeDriver returns
    // non-W3C compliant capabilities 'networkConnectionEnabled' and 'chrome' without the 'goog:' prefix.

    // We cast to RemoteWebDriver to access capabilities
    Capabilities caps = ((RemoteWebDriver) driver).getCapabilities();

    // Check for 'networkConnectionEnabled'
    // Expected: Should be null (or prefixed with goog:)
    // If bug exists: It returns a value (e.g., false)
    Object network = caps.getCapability("networkConnectionEnabled");
    assertNull(network, "Non-W3C capability 'networkConnectionEnabled' should not be present. It should be prefixed with 'goog:'.");

    // Check for 'chrome'
    // Expected: Should be null (or prefixed with goog:)
    // If bug exists: It returns a map/object
    Object chrome = caps.getCapability("chrome");
    assertNull(chrome, "Non-W3C capability 'chrome' should not be present. It should be prefixed with 'goog:'.");
  }
}
