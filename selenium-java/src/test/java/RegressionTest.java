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
  public void issue42321530_getTextFromTableDoesNotReturnEmptyText() {
    // This test reproduces https://crbug.com/42321530
    // The issue is that getText() returns an empty string for an element
    // that is not fully visible due to an `overflow:hidden` on a parent element.
    // The 15th row is partially hidden, and getText() on its cell returns an empty string.
    // The expected behavior is that getText() should return the text content of the element
    // regardless of its visibility.
    java.io.File htmlFile = new java.io.File("bug_42321530.html");
    driver.get("file://" + htmlFile.getAbsolutePath());

    org.openqa.selenium.WebElement baseTable = driver.findElement(org.openqa.selenium.By.tagName("tbody"));
    java.util.List<org.openqa.selenium.WebElement> rows = baseTable.findElements(org.openqa.selenium.By.tagName("tr"));
    java.util.List<String> texts = new java.util.ArrayList<>();
    for (org.openqa.selenium.WebElement row : rows) {
      java.util.List<org.openqa.selenium.WebElement> cells = row.findElements(org.openqa.selenium.By.xpath(".//td"));
      org.openqa.selenium.WebElement element = cells.get(0);
      String text = element.getText();
      texts.add(text);
    }
    // The 15th element is the first that is not fully visible and returns empty string.
    assertEquals("15", texts.get(14));
  }
}
