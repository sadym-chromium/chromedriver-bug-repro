/*
 * Copyright 2026 Google LLC
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
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.options.UiAutomator2Options;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.interactions.Sequence;

public class RegressionTest {

  private AndroidDriver driver;

  @BeforeEach
  public void setUp() throws MalformedURLException {
    // Configure UiAutomator2Options for Appium to automate Chrome on an Android emulator
    UiAutomator2Options options = new UiAutomator2Options()
        .setPlatformName("Android")
        .setDeviceName("Android Emulator")
        .setAutomationName("UiAutomator2")
        .withBrowserName("Chrome")
        .setNoReset(true)
        .setUiautomator2ServerInstallTimeout(java.time.Duration.ofMillis(60000));

    // Initialize AndroidDriver, connecting to the Appium server (usually http://127.0.0.1:4723)
    driver = new AndroidDriver(new URL("http://127.0.0.1:4723"), options);
    driver.manage().timeouts().pageLoadTimeout(java.time.Duration.ofSeconds(60));
    driver.manage().timeouts().implicitlyWait(java.time.Duration.ofSeconds(30));
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
    // This test reproduces crbug.com/42321409 (chromedriver:2332)
    // Description: testTouchScrollElement fails with Webview
    // The test navigates to a page with a scrollable area and performs a touch scroll action.
    // It is expected to fail if scrollLeft/scrollTop remains 0 after the action.

    String html = "<!DOCTYPE html>" +
        "<html lang='en'>" +
        "  <head>" +
        "    <meta charset='UTF-8'>" +
        "    <meta name='viewport' content='width=device-width,minimum-scale=1'>" +
        "    <title>Touch Action Test Page</title>" +
        "  </head>" +
        "  <body>" +
        "    <div id='target' style='height: 100px; width: 100px; background: red;'>Events are logged when tests touch this div.</div>" +
        "    <div id='events' style='white-space:nowrap;'>events: </div>" +
        "    <div id='padding' style='border: solid; height: 2000px; width: 2000px;'>Padding</div>" +
        "    <script>" +
        "      events = document.getElementById('events');" +
        "      var eventTypes = ['touchstart', 'touchend', 'touchmove', 'touchcancel'];" +
        "      var eventListener = function(evt) {" +
        "        events.innerHTML += ' ' + evt.type;" +
        "      };" +
        "      var target = document.getElementById('target');" +
        "      for (var i = 0; i < eventTypes.length; i++) {" +
        "        target.addEventListener(eventTypes[i], eventListener);" +
        "      }" +
        "    </script>" +
        "  </body>" +
        "</html>";
    String encodedHtml = java.util.Base64.getEncoder().encodeToString(html.getBytes());
    driver.get("data:text/html;base64," + encodedHtml);

    // Verify initial scroll position is 0
    long scrollLeftBefore = ((Number) driver.executeScript("return document.documentElement.scrollLeft || document.body.scrollLeft;")).longValue();
    long scrollTopBefore = ((Number) driver.executeScript("return document.documentElement.scrollTop || document.body.scrollTop;")).longValue();
    assertEquals(0, scrollLeftBefore);
    assertEquals(0, scrollTopBefore);

    WebElement target = driver.findElement(By.id("target"));
    
    // Simulate a touch scroll by swiping from the target element.
    // We move the finger from target center to a position moved by (47, 53).
    // Note: To scroll DOWN and RIGHT, we need to swipe UP and LEFT in viewport coordinates if we were dragging the content,
    // but TouchScroll usually means "scroll the view". 
    // In Python ChromeDriver, TouchScroll(target, 47, 53) scrolls the element or page.
    
    PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
    Sequence scroll = new Sequence(finger, 0);
    scroll.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.fromElement(target), 0, 0));
    scroll.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
    // Swipe to perform scroll
    scroll.addAction(finger.createPointerMove(Duration.ofMillis(500), PointerInput.Origin.fromElement(target), -47, -53));
    scroll.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
    driver.perform(Collections.singletonList(scroll));

    // Wait a bit for scroll to finish
    try { Thread.sleep(1000); } catch (InterruptedException e) {}

    long scrollLeftAfter = ((Number) driver.executeScript("return document.documentElement.scrollLeft || document.body.scrollLeft;")).longValue();
    long scrollTopAfter = ((Number) driver.executeScript("return document.documentElement.scrollTop || document.body.scrollTop;")).longValue();
    
    System.out.println("Scroll Left After: " + scrollLeftAfter);
    System.out.println("Scroll Top After: " + scrollTopAfter);

    // If the bug exists, scrollLeftAfter will be 0 (or close to it), failing the assertion.
    assertTrue(scrollLeftAfter > 0, "Expected scrollLeft to be > 0, but was " + scrollLeftAfter);
    assertTrue(scrollTopAfter > 0, "Expected scrollTop to be > 0, but was " + scrollTopAfter);
  }
}
