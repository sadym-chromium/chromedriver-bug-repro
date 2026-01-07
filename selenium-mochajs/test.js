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

const { Builder, logging } = require('selenium-webdriver');
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
    options.addArguments('--disable-dev-shm-usage');

    // Enable performance logging
    const prefs = new logging.Preferences();
    prefs.setLevel(logging.Type.PERFORMANCE, logging.Level.ALL);
    options.setLoggingPrefs(prefs);
    options.setPerfLoggingPrefs({enableNetwork: true});

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
      await driver.quit();
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

  /**
   * Test case for crbug.com/42323057
   * 
   * Reproduction steps:
   * 1. Enable performance logging with network domain enabled.
   * 2. Start ChromeDriver in headless mode.
   * 3. Navigate to a site with a signed certificate (e.g., https://www.google.com).
   * 4. Retrieve performance logs.
   * 5. Parse logs for Network.responseReceived events.
   * 6. Check if securityDetails.signedCertificateTimestampList is present and not empty.
   * 
   * Expected behavior: The list should NOT be empty.
   * If the bug exists, the list will be empty, and the assertion will fail.
   */
  it('should have signedCertificateTimestampList in headless mode', async function () {
    await driver.get('https://www.google.com');

    // Get performance logs
    const logs = await driver.manage().logs().get(logging.Type.PERFORMANCE);
    
    let sctFound = false;
    
    for (const entry of logs) {
      const message = JSON.parse(entry.message);
      const method = message.message.method;
      
      // Look for Network.responseReceived event
      if (method === 'Network.responseReceived') {
        const params = message.message.params;
        // Check if securityDetails exists
        if (params.response && params.response.securityDetails) {
            const sctList = params.response.securityDetails.signedCertificateTimestampList;
            
            // Log for debugging (optional)
            // console.log('SCT List found:', sctList);

            // Check if SCT list is not empty
            if (sctList && sctList.length > 0) {
                sctFound = true;
                break;
            }
        }
      }
    }
    
    expect(sctFound).toBe(true);
  });
});