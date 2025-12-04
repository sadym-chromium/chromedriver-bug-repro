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

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Action;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.Keys;

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
    /*
     * This test reproduces the bug reported in crbug.com/42322721.
     * The bug describes that when using Selenium Actions to type '@' (Shift + 2)
     * into a textarea or input field, the '@' symbol gets highlighted and erased.
     *
     * Reproduction Steps:
     * 1. Navigate to Google.com
     * 2. Locate the search input field.
     * 3. Perform the action sequence: moveToElement, click, keyDown(SHIFT), sendKeys("2"), keyUp(SHIFT).
     * 4. Verify that the '@' symbol is present in the input field.
     *
     * Expected Failure:
     * If the bug exists, the '@' symbol will be highlighted and erased,
     * leading to the input field not containing '@'.
     * The assertion `assertTrue(searchBox.getAttribute("value").contains("@"))`
     * will fail if the bug is present, thus confirming the bug's existence.
     */
    driver.get("https://www.google.com");
    WebElement searchBox = driver.findElement(By.name("q"));

    Actions builder = new Actions(driver);
    Action seriesOfActions = builder
        .moveToElement(searchBox)
        .click()
        .keyDown(searchBox, Keys.SHIFT)
        .sendKeys(searchBox, "2")
        .keyUp(searchBox, Keys.SHIFT)
        .build();
    seriesOfActions.perform();

    // Assert that the search box contains the "@" symbol.
    // If the bug exists, the "@" symbol will be highlighted and erased,
    // so this assertion will fail.
    assertEquals("@", searchBox.getAttribute("value"));
  }
}
