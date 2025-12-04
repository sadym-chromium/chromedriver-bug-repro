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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
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
    driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(10));
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
  public void webserverErrorShouldBePropagated() {
    // This test reproduces a bug where unhandled exceptions on a webserver
    // are not clearly reported to the WebDriver client. Instead of a specific
    // error, the client might experience a timeout or receive a generic error message.
    //
    // The test works by:
    // 1. Starting a local HTTP server that immediately throws a RuntimeException
    //    for any incoming request. This simulates an unhandled server-side error.
    // 2. Navigating the WebDriver to this local server.
    //
    // The test is expected to FAIL if the bug is present.
    // A failing test would either time out, or the WebDriverException would have
    // a generic message.
    //
    // The test PASSES if a WebDriverException is caught and its message contains
    // a specific network error, indicating that the server-side problem was
    // propagated to the client.
    //
    // See bug: https://issuetracker.google.com/issues/42323058
    HttpServer server = null;
    try {
      server = HttpServer.create(new InetSocketAddress(0), 0);
      server.createContext(
          "/",
          exchange -> {
            // This handler simulates an unhandled exception on the server
            // by closing the connection without sending a response.
            exchange.close();
          });
      server.start();

      driver.get("http://localhost:" + server.getAddress().getPort());

      // After the get(), check if the page is a chrome error page.
      // If it is, then the error was not propagated as an exception,
      // which is the bug.
      if (driver.getPageSource().contains("neterror")) {
        fail("Navigation resulted in a Chrome error page, but no WebDriverException was thrown.");
      }

    } catch (IOException e) {
      fail("Test setup failed with IOException: " + e.getMessage());
    } catch (WebDriverException e) {
      // Expected behavior for a fixed ChromeDriver.
      // The exception message should indicate a network error from the browser.
      String message = e.getMessage().toLowerCase();
      assertTrue(
          message.contains("net::err_empty_response")
              || message.contains("net::err_connection_reset")
              || message.contains("unable to load page"),
          "The WebDriverException message did not contain an expected network error. Got: "
              + e.getMessage());
    } finally {
      if (server != null) {
        server.stop(0);
      }
    }
  }
}
