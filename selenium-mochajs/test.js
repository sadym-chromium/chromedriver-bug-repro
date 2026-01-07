/**
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

const { Builder } = require('selenium-webdriver');
const { expect } = require('expect');
const chrome = require('selenium-webdriver/chrome');

describe('Selenium ChromeDriver', function () {
  let driver;
  // The chrome and chromedriver installation can take some time. 
  // Give 5 minutes to install everything.
  this.timeout(5 * 60 * 1000);

  beforeEach(async function () {
    const options = new chrome.Options();
    options.addArguments('--headless');
    options.addArguments('--no-sandbox');

    // By default, the test uses the latest stable Chrome version.
    // Replace the "stable" with the specific browser version if needed,
    // e.g. 'canary', '115' or '144.0.7534.0' for example.
    options.setBrowserVersion('stable');

    const service = new chrome.ServiceBuilder()
      .loggingTo('chromedriver.log')
      .enableVerboseLogging();

    driver = await new Builder()
      .forBrowser('chrome')
      .setChromeOptions(options)
      .setChromeService(service)
      .build();
  });

  afterEach(async function () {
    if (driver) {
      try {
        // Attempt to quit with a timeout to prevent hanging if the driver is dead
        await Promise.race([
          driver.quit(),
          new Promise((_, reject) => setTimeout(() => reject(new Error('driver.quit() timed out')), 5000))
        ]);
      } catch (e) {
        console.error('Error quitting driver:', e);
      }
    }
  });

  /**
   * This test is intended to verify the setup is correct.
   */
  it('should be able to navigate to google.com', async function () {
    await driver.get('https://www.google.com');
    const title = await driver.getTitle();
    expect(title).toBe('Google');
  });

  it('should crash when focused tab is closed during command execution', async function () {
    // This test reproduces crbug.com/42323315 where ChromeDriver crashes or hangs
    // when the focused tab is closed while a command is being executed.
    
    this.timeout(10000); // Fail fast if it hangs

    // Navigate to a base page
    await driver.get('data:text/html,<h1>Base Page</h1>');

    // Open a new tab and switch to it
    await driver.executeScript("window.open('data:text/html,<h1>Target Tab</h1>', '_blank');");
    const handles = await driver.getAllWindowHandles();
    await driver.switchTo().window(handles[1]);

    // Schedule the tab to close in 1 second
    // This creates a race condition where the tab closes while we are sending commands
    await driver.executeScript("setTimeout(() => window.close(), 1000);");

    const startTime = Date.now();
    try {
      // Spam commands (Screenshot) to hit the race condition.
      // We expect this loop to eventually fail. If the bug is present, 
      // the driver may hang or crash (Socket hang up).
      while (Date.now() - startTime < 5000) {
        // We race against a timeout because if the driver crashes/hangs, 
        // the promise might never resolve.
        await Promise.race([
            driver.takeScreenshot(),
            new Promise((_, reject) => setTimeout(() => reject(new Error('Screenshot timed out - Driver likely crashed/hung')), 2000))
        ]);
        
        // Small delay to allow the close event to be processed
        await new Promise(r => setTimeout(r, 10)); 
      }
    } catch (e) {
      console.log('Caught expected error during reproduction:', e.message);
      // If we caught the specific timeout or a crash error, that confirms the bug.
      // We re-throw to ensure the test shows as failed/reproduced.
      throw e;
    }
  });
});
