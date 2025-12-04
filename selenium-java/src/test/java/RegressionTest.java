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
  public void newSessionShouldReturnDefaultUserAgent() {
    // This test reproduces https://crbug.com/chromedriver/4719
    // It is expected to fail if the bug is present.
    // The bug is that the userAgent in the session capabilities is the
    // one from mobile emulation, not the default one.
    // The webdriver spec https://github.com/w3c/webdriver/pull/1790
    // says it should be the default one.

    ChromeOptions options = new ChromeOptions();
    options.addArguments("--headless");
    options.addArguments("--no-sandbox");
    options.setBrowserVersion("stable");

    // Enable mobile emulation
    java.util.Map<String, String> mobileEmulation = new java.util.HashMap<>();
    mobileEmulation.put("userAgent", "Mozilla/5.0 (Linux; Android 4.2.1; en-us; Nexus 5 Build/JOP40D) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.166 Mobile Safari/535.19");
    options.setExperimentalOption("mobileEmulation", mobileEmulation);

    ChromeDriverService service =
        new ChromeDriverService.Builder()
            .withLogFile(new java.io.File("chromedriver.log"))
            .withVerbose(true)
            .build();

    driver = new ChromeDriver(service, options);

    // The user agent from the session capabilities should be the default one
    String sessionUserAgent = (String) ((ChromeDriver) driver).getCapabilities().getCapability("userAgent");
    
    // The user agent from the navigator should be the emulated one
    String navigatorUserAgent = (String) ((ChromeDriver) driver).executeScript("return navigator.userAgent");

    // Assert that the session user agent is not the emulated one.
    // This is expected to fail if the bug is present.
    org.junit.jupiter.api.Assertions.assertNotNull(sessionUserAgent);
    org.junit.jupiter.api.Assertions.assertNotEquals(sessionUserAgent, navigatorUserAgent);
  }

  @Test
  public void ISSUE_REPRODUCTION() {
    // Add test reproducing the issue here.
  }
}
