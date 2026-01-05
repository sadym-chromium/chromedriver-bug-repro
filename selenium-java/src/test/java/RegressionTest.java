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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.Set;
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
  public void verifySetup_shouldBeAbleToNavigateToGoogleCom() {
    // Navigate to a URL
    driver.get("https://www.google.com");
    // Assert that the navigation was successful
    assertEquals("Google", driver.getTitle());
  }

  @Test
  public void testGetWindowHandlesAndSwitch() {
    // This test reproduces the issue where ChromeDriver fails to get window handles and switch to new windows.
    // The bug report mentions that tests related to window handling are failing on Android.
    // This test attempts to replicate the core of the issue in a Java environment.
    // It is expected to fail if the bug is present, specifically if the new window handle is not found
    // or if the driver cannot switch to it.
    // The assertion `assertNotEquals(originalHandles, newHandles)` is expected to fail if the bug is present.
    // The WebDriver spec states that GetWindowHandles should return all window handles for the current session.
    // See: https://www.w3.org/TR/webdriver/#get-window-handles

    // Navigate to a local HTML file that has a link to open a new window.
    File htmlFile = new File("src/test/resources/open_new_window.html");
    driver.get("file://" + htmlFile.getAbsolutePath());

    // Get the original window handle.
    String originalHandle = driver.getWindowHandle();
    Set<String> originalHandles = driver.getWindowHandles();
    assertEquals(1, originalHandles.size());

    // Click the link to open a new window.
    driver.findElement(By.tagName("a")).click();

    // Get the new window handles.
    Set<String> newHandles = driver.getWindowHandles();

    // The test is expected to fail here if the bug is present.
    // The number of window handles should be 2, but it might be 1 if the bug is present.
    assertEquals(2, newHandles.size());

    // The new window handle should be different from the original one.
    newHandles.removeAll(originalHandles);
    String newHandle = newHandles.iterator().next();
    assertNotEquals(originalHandle, newHandle);

    // Switch to the new window.
    driver.switchTo().window(newHandle);

    // Check if the driver is switched to the new window.
    assertEquals("Google", driver.getTitle());
  }
}
