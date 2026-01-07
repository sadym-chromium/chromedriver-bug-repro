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

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.openqa.selenium.chrome.ChromeOptions;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class RegressionTest {

  private WebDriver driver;
  private File downloadDir;

  @BeforeEach
  public void setUp() {
    downloadDir = new File(System.getProperty("user.dir"), "downloads");
    if (!downloadDir.exists()) {
      downloadDir.mkdirs();
    }
    
    // Clean up directory
    if (downloadDir.listFiles() != null) {
        for (File file : downloadDir.listFiles()) {
            file.delete();
        }
    }

    Map<String, Object> prefs = new HashMap<>();
    prefs.put("download.default_directory", downloadDir.getAbsolutePath());
    prefs.put("download.prompt_for_download", false);
    prefs.put("safebrowsing.enabled", true); // User explicitly sets this to true in the report
    
    ChromeOptions options = new ChromeOptions();
    options.setExperimentalOption("prefs", prefs);
    options.addArguments("--headless=new");
    options.addArguments("--no-sandbox");
    options.addArguments("start-maximized");
    options.addArguments("--safebrowsing-disable-download-protection");
    options.addArguments("safebrowsing-disable-extension"); // Keeping the typo as per user report
    // Note: Even correcting the typo above to "--safebrowsing-disable-extension"
    // and setting "safebrowsing.enabled" to false does NOT resolve the issue.
    // The Safe Browsing blocking persists.
    
    options.setBrowserVersion("stable");
    
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
  public void ISSUE_REPRODUCTION() throws InterruptedException {
    // Attempt to download EICAR test file which should trigger Safe Browsing/Malware warning
    String fileUrl = "https://secure.eicar.org/eicar.com"; 
    String expectedFileName = "eicar.com";
    
    System.out.println("Attempting to download: " + fileUrl);
    try {
        driver.get(fileUrl);
    } catch (Exception e) {
        System.out.println("Navigation failed (expected for some blocked downloads): " + e.getMessage());
    }

    // Wait for download to complete
    File downloadedFile = new File(downloadDir, expectedFileName);
    boolean downloaded = false;
    for (int i = 0; i < 30; i++) { // Wait up to 30 seconds
        if (downloadedFile.exists() && downloadedFile.length() > 0) {
            downloaded = true;
            break;
        }
        Thread.sleep(1000);
    }
    
    if (!downloaded) {
        System.out.println("File not found in: " + downloadDir.getAbsolutePath());
        if (downloadDir.listFiles() != null) {
            System.out.println("Files in dir:");
            for (File f : downloadDir.listFiles()) {
                System.out.println(" - " + f.getName());
            }
        }
    }

    assertTrue(downloaded, "File should be downloaded. If failed, Safe Browsing likely blocked it.");
  }
}