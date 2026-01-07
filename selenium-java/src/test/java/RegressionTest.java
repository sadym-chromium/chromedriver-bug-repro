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
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

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
    options.addArguments("--headless=new");
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
  public void ISSUE_REPRODUCTION() throws Exception {
    // Reproducing Bug 405934870: ChromeDriver hangs connecting to an existing browser with open tabs.
    // The issue occurs when ChromeDriver attempts to connect to an existing Chrome instance 
    // (launched with --remote-debugging-port) that has multiple specific tabs open.
    // The expected behavior is that ChromeDriver connects successfully.
    // The observed bug is that the connection hangs indefinitely.

    // Quit the driver initialized in setUp() as we need a custom environment
    if (driver != null) {
      driver.quit();
      driver = null;
    }

    int port = 9222;
    Path userDataDir = Files.createTempDirectory("chrome-user-data");
    String chromeBinary = "/usr/bin/google-chrome";
    
    // Launch Chrome with remote debugging and multiple tabs.
    // The URLs below are taken from the bug report as they were reported to trigger the issue.
    ProcessBuilder pb = new ProcessBuilder(
        chromeBinary,
        "--remote-debugging-port=" + port,
        "--user-data-dir=" + userDataDir.toAbsolutePath().toString(),
        "--headless=new",
        "--no-sandbox",
        "--disable-gpu",
        "--disable-dev-shm-usage",
        "https://johnsad.ventures/2002/07/18/learning_to_fail/",
        "https://www.youtube.com/watch?v=GlPa1wM41fQ",
        "https://www.google.com/search?q=web%20component%20dynamically%20update%20value",
        "https://developer.mozilla.org/en-US/docs/Web/HTML/Element/template",
        "https://kinsta.com/blog/web-components/",
        "https://github.com/pulumi/actions"
    );
    
    pb.redirectErrorStream(true);
    // pb.inheritIO(); // Uncomment to see Chrome output in console

    System.out.println("Starting Chrome...");
    Process chromeProcess = pb.start();

    try {
        // Wait for Chrome to start and load tabs
        Thread.sleep(5000);

        System.setProperty("webdriver.chrome.verboseLogging", "true");
        ChromeOptions options = new ChromeOptions();
        options.setExperimentalOption("debuggerAddress", "127.0.0.1:" + port);
        
        // The bug causes the connection to hang.
        // We use assertTimeoutPreemptively to enforce a timeout and fail the test if it hangs.
        // A timeout of 20 seconds is sufficient to detect the hang (connection usually takes < 1s).
        assertTimeoutPreemptively(Duration.ofSeconds(20), () -> {
            System.out.println("Attempting to connect to Chrome...");
            WebDriver client = new ChromeDriver(options);
            System.out.println("Connected successfully!");
            client.quit();
        }, "ChromeDriver hung while connecting to an existing browser with open tabs (Bug 405934870)");

    } finally {
        if (chromeProcess.isAlive()) {
            chromeProcess.destroy();
            chromeProcess.waitFor(5, TimeUnit.SECONDS);
            if (chromeProcess.isAlive()) {
                chromeProcess.destroyForcibly();
            }
        }
        deleteDirectory(userDataDir);
    }
  }

  private void deleteDirectory(Path path) throws IOException {
      try (Stream<Path> walk = Files.walk(path)) {
          walk.sorted(Comparator.reverseOrder())
              .map(Path::toFile)
              .forEach(File::delete);
      }
  }
}
