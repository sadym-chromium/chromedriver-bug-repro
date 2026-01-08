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

import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.nativekey.AndroidKey;
import io.appium.java_client.android.nativekey.KeyEvent;
import io.appium.java_client.android.options.UiAutomator2Options;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WindowType;

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
    // 1. Open initial page
    driver.get("https://www.google.com");

    // 2. Open a new tab (Simulate opening a link in a new webview)
    driver.switchTo().newWindow(WindowType.TAB);
    driver.get("https://example.com");
    
    // 3. Switch to NATIVE_APP
    driver.context("NATIVE_APP");
    
    // 4. Press BACK button to close the current tab
    // In Chrome, if you are on the first page of a tab, Back often closes it.
    driver.pressKey(new KeyEvent(AndroidKey.BACK));
    try { Thread.sleep(2000); } catch (InterruptedException e) {}

    // 5. Open another new tab (Simulate opening a second link)
    // We need to switch context back to WEBVIEW first to use Selenium commands to open a window
    // BUT the bug is that switching to WEBVIEW fails or finding elements fails.
    // If we use driver.switchTo().newWindow() it might mask the issue because it handles context.
    
    // Let's try to find the webview context again.
    Set<String> contexts = driver.getContextHandles();
    String webviewContext = null;
    for (String context : contexts) {
      if (!"NATIVE_APP".equals(context)) {
        webviewContext = context;
        break;
      }
    }
    
    if (webviewContext != null) {
        driver.context(webviewContext);
    }
    
    // Open the new "link"
    driver.switchTo().newWindow(WindowType.TAB);
    driver.get("https://www.google.com");

    // 6. Try to find an element. 
    // If the driver is confused about the windows because of the native close, this might fail.
    driver.findElement(By.name("q"));
  }
}
