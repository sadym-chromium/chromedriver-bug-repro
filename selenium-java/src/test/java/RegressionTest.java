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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.List;

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
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import java.time.Duration;

public class RegressionTest {

  private WebDriver driver;
  private HttpServer server1;
  private HttpServer server2;
  private String baseUrl1;
  private String baseUrl2;

  @BeforeEach
  public void setUp() throws IOException {
    // Start local HTTP server 1
    server1 = HttpServer.create(new InetSocketAddress(0), 0);
    server1.createContext("/", new HttpHandler() {
      @Override
      public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String response = "";
        if (path.equals("/")) {
          response = "<html><body>" +
                     "<div id='root-element'>Root</div>" +
                     "<iframe id='my-iframe' name='my-iframe-name' src='" + baseUrl2 + "/iframe'></iframe>" +
                     "</body></html>";
        } else if (path.equals("/other")) {
          response = "<html><body>Other Page</body></html>";
        }
        exchange.sendResponseHeaders(200, response.length());
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
      }
    });
    server1.start();
    baseUrl1 = "http://localhost:" + server1.getAddress().getPort();

    // Start local HTTP server 2 (for cross-origin iframe)
    server2 = HttpServer.create(new InetSocketAddress(0), 0);
    server2.createContext("/iframe", new HttpHandler() {
      @Override
      public void handle(HttpExchange exchange) throws IOException {
        String response = "<html><body>" +
                          "<div id='iframe-element'>Iframe Content</div>" +
                          "</body></html>";
        exchange.sendResponseHeaders(200, response.length());
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
      }
    });
    server2.start();
    baseUrl2 = "http://localhost:" + server2.getAddress().getPort();

    ChromeOptions options = new ChromeOptions();
    options.addArguments("--no-sandbox");
    options.addArguments("--headless");

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
    if (server1 != null) {
      server1.stop(0);
    }
    if (server2 != null) {
      server2.stop(0);
    }
  }

  private void checkIframe(String method) {
    System.out.println("Checking iframe using " + method + "...");
    // We don't use WebDriverWait for root-element here to be as fast as possible
    
    // Switch to iframe
    if (method.equals("element")) {
        WebElement iframe = driver.findElement(By.id("my-iframe"));
        driver.switchTo().frame(iframe);
    } else if (method.equals("index")) {
        driver.switchTo().frame(0);
    } else if (method.equals("name")) {
        driver.switchTo().frame("my-iframe-name");
    }

    // Check iframe element (we still need wait here because iframe might load its content)
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));
    WebElement iframeElement = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("iframe-element")));
    assertNotNull(iframeElement, "Iframe element should be present after switching to iframe");

    // Switch back to default content
    driver.switchTo().defaultContent();
    System.out.println("Iframe check passed.");
  }

  @Test
  public void reproduceBfcacheIframeIssue() throws InterruptedException {
    // 1. Go to the page with iframe
    driver.get(baseUrl1 + "/");
    System.out.println("Initial load");
    checkIframe("element");
    
    // 2. Go to another page
    driver.get(baseUrl1 + "/other");
    System.out.println("Navigated to other page");

    // 3. Navigate back (triggering bfcache)
    driver.navigate().back();
    System.out.println("Navigated back");
    
    // 4. Try interacting with the iframe again immediately
    checkIframe("element");
    System.out.println("Success after back navigation");
  }
}