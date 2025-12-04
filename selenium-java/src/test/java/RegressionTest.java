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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import java.time.Duration;

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
  public void BUG_NOT_REPRODUCIBLE_42322970() {
    // This test confirms that crbug.com/42322970 is NOT reproducible with the current
    // ChromeDriver/Chrome/Selenium versions. The original bug reported that alert.sendKeys("value")
    // does not type the value into the prompt dialog.

    String testText = "Test Value";

    // 1. Navigate to the URL that contains the prompt dialog.
    driver.get("https://demoqa.com/alerts");

    // 2. Click the button that triggers the prompt dialog.
    // Use JavascriptExecutor to click the button to bypass ElementClickInterceptedException.
    // This is often necessary when an overlay or other element obscures the target element.
    new WebDriverWait(driver, Duration.ofSeconds(10))
        .until(ExpectedConditions.elementToBeClickable(By.id("promtButton")));
    ((org.openqa.selenium.JavascriptExecutor) driver).executeScript("arguments[0].click();", driver.findElement(By.id("promtButton")));

    // 3. Switch to the alert (prompt dialog).
    // Use an explicit wait for the alert to be present.
    Alert alert = new WebDriverWait(driver, Duration.ofSeconds(10))
        .until(ExpectedConditions.alertIsPresent());

    // 4. Send keys to the prompt dialog.
    // In the context of the original bug, this action was reported to not correctly input the text.
    alert.sendKeys(testText);

    // 5. Accept the alert.
    alert.accept();

    // 6. Get the text from the result element.
    // The demoqa.com site displays the entered text in an element with id "promptResult".
    String resultText = new WebDriverWait(driver, Duration.ofSeconds(10))
        .until(ExpectedConditions.visibilityOfElementLocated(By.id("promptResult")))
        .getText();

    // 7. Assert that the result text *does* contain the sent text.
    // This assertion is expected to PASS, confirming that the bug is NOT reproducible (i.e., sendKeys worked correctly).
    assertTrue(resultText.contains(testText),
        "BUG 42322970: The text '" + testText + "' was NOT correctly entered into the prompt dialog. The bug might still be present.");
  }
}
