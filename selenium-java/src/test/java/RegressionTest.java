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
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
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
  public void verifySetup_shouldBeAbleToNavigateToGoogleCom() {
    // Navigate to a URL
    driver.get("https://www.google.com");
    // Assert that the navigation was successful
    assertEquals("Google", driver.getTitle());
  }

  @Test
  public void clickableDivOverlaidOnTopOfCrossDomainIframe_shouldBeClickable() throws InterruptedException {
    // This test reproduces https://crbug.com/chromedriver/2758
    // The issue is that Chromedriver is unable to click a given visible element
    // when said element is overlaid on top of a cross domain iframe.
    driver.get("http://jackwellborn.com/playground/webdriverClickBug/clickbug.html");
    // Move the iframe to overlap the clickable div
    ((JavascriptExecutor) driver).executeScript("document.querySelector('body > iframe').style.top = '5px';");
    ((JavascriptExecutor) driver).executeScript("document.querySelector('body > iframe').style.left = '0px';");
    // Wait for the iframe to be positioned
    Thread.sleep(1000);
    WebElement clickableDiv = driver.findElement(By.id("clickableDiv"));
    clickableDiv.click();
    // The bug is that the click does not happen, so the text remains "Click Me"
    // The test is expected to fail until the bug is fixed.
    assertEquals("CLICKED!", clickableDiv.getText());
  }
}
