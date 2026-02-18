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
    // The bug was reported for headless mode.
    // It reproduces in both '--headless' (new) and '--headless=old' in Chrome 119.
    options.addArguments("--headless");
    options.addArguments("--no-sandbox");

    // Reproduces in Chrome 119 (as reported).
    // Note: This issue appears to be fixed in later versions (tested with 145).
    options.setBrowserVersion("119");

    ChromeDriverService service = new ChromeDriverService.Builder()
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
  public void reproduce_issue_42323674() {
    // Navigate to the reproduction URL
    driver.get("https://pegelonline.wsv.de/webservice/dokuRestapi");

    // Locate a link that contains date parameters.
    // The bug causes ISO-8601 dates in the 'href' attribute to be reformatted
    // into human-readable strings (e.g., 'Tue Jan 27 09:00:00 CET 2026') in
    // headless mode.
    org.openqa.selenium.WebElement element = driver
        .findElement(org.openqa.selenium.By.xpath("//a[contains(@href, 'start=')]"));
    String href = element.getAttribute("href");

    System.out.println("Retrieved href: " + href);

    // We expect the ISO-8601 format to be preserved (e.g., 2023-10-26T09:00).
    // A reformatted date would typically contain month names like 'Jan', 'Feb',
    // etc.
    boolean isReformatted = href.matches(".*(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec).*");

    // Assert that it is NOT reformatted. This assertion will FAIL if the bug is
    // present.
    org.junit.jupiter.api.Assertions.assertFalse(isReformatted,
        "The href attribute was reformatted into a human-readable date string in headless mode.\nActual href: " + href);

    // Also check for the expected ISO pattern to ensure it matches what's expected.
    boolean matchesExpected = href.matches(".*start=\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}.*");
    org.junit.jupiter.api.Assertions.assertTrue(matchesExpected,
        "The href attribute does not match the expected ISO-8601 format.\nActual href: " + href);
  }
}
