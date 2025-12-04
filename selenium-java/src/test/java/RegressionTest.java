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

import java.io.File;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
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
  public void reproducesBug42323495() throws InterruptedException {
    // The bug report mentions that Chrome/ChromeDriver hangs when a tab that loads an iFrame is open, while using BiDi.
    // This test reproduces the issue by:
    // 1. Opening a page with a link to a new tab.
    // 2. Clicking the link to open the new tab.
    // 3. The new tab contains an iframe.
    // 4. The test then tries to switch to the new tab and find an element in the iframe.
    // The test is expected to hang on the findElement call.
    driver.get("file://" + new File("src/test/resources/html/window_one.html").getAbsolutePath());

    String originalWindow = driver.getWindowHandle();
    assertEquals(driver.getWindowHandles().size(), 1);

    driver.findElement(By.name("windowTwo")).click();
    Thread.sleep(Duration.ofSeconds(1));
    assertEquals(driver.getWindowHandles().size(), 2);

    for (String windowHandle : driver.getWindowHandles()) {
      if (!originalWindow.contentEquals(windowHandle)) {
        driver.switchTo().window(windowHandle);
        break;
      }
    }

    driver.switchTo().frame("the-iframe");
    driver.findElement(By.id("iframe-header"));
  }
}
