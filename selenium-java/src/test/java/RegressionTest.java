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
import org.openqa.selenium.WebElement;
import org.openqa.selenium.By;
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
  public void testGetTextFromInvisibleTableCell_bug42321530() {
    // This test reproduces crbug.com/42321530
    // The bug occurs when trying to get the text of a table cell that is not
    // visible in the viewport due to a parent element with "overflow: hidden".
    // The expected behavior is that ChromeDriver should implicitly scroll the
    // element into view and return the text. The actual behavior is that it
    // returns an empty string.
    // This test loads a local HTML file with a table inside a div with
    // "overflow: hidden". The 15th cell is out of view. The test then
    // asserts that the text of the 15th cell is "15".
    // This assertion is expected to fail.
    driver.get("file:///usr/local/google/home/sadym/chromedriver-bug-repro/selenium-java/src/test/resources/table_test.html");
    WebElement baseTable = driver.findElement(By.tagName("tbody"));
    java.util.List<WebElement> rows = baseTable.findElements(By.tagName("tr"));
    WebElement cell = rows.get(14).findElement(By.xpath(".//td"));
    assertEquals("15", cell.getText());
  }
}
