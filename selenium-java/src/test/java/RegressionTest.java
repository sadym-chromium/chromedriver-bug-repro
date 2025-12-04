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
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.nio.file.Paths;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
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
  public void reproduceStaleElementException() {
    // This test reproduces a bug where a StaleElementReferenceException is thrown
    // because the click() command on a submit button returns before the page has finished
    // navigating. This creates a race condition where the subsequent findElement call
    // may find the element on the old page, which becomes stale after the navigation
    // completes.

    File formHtml = Paths.get("src", "test", "resources", "form.html").toFile();
    driver.get(formHtml.toURI().toString());

    for (int i = 0; i < 10; i++) {
      try {
        String uniqueValue = UUID.randomUUID().toString();

        WebElement textInput = driver.findElement(By.id("text-input"));
        textInput.clear();
        textInput.sendKeys(uniqueValue);

        WebElement submitButton = driver.findElement(By.id("submit-button"));
        submitButton.click();

        WebElement submittedText = driver.findElement(By.id("submitted-text"));
        // This assertion is expected to fail if the bug is present.
        // The click() returns before the page reloads, so we get the element
        // from the old page, and its text is not the uniqueValue.
        // When the page does reload, the element becomes stale, and a
        // StaleElementReferenceException is thrown on the next iteration.
        assertEquals(uniqueValue, submittedText.getText());
      } catch (StaleElementReferenceException e) {
        // If we catch a StaleElementReferenceException, the bug is reproduced.
        // We can fail the test here.
        fail("StaleElementReferenceException thrown, bug is reproduced.", e);
      }
    }
  }
}
