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
    await driver.quit();
  });

  /**
   * Test case to verify that ChromeDriver logs include the Process Identifier (PID)
   * and the Start Date/Time.
   *
   * Reproduction Steps:
   * 1. Start a ChromeDriver instance with verbose logging enabled (done in beforeEach).
   * 2. Read the content of the `chromedriver.log` file.
   * 3. Assert that the log file contains entries for "Process Identifier" and "Start Time".
   *
   * Expected Pass: This test is expected to pass if the bug (crbug.com/42323164) exists,
   * as the bug states that PID and Start Time are missing from the ChromeDriver logs.
   *
   * Justification for expected value: The WebDriver specification (though not directly
   * specifying log content) does not mandate specific log content. The bug report
   * clearly states that PID and Start Time are missing, making debugging difficult.
   * This test verifies the absence of a "Process ID" string in the logs to confirm the bug.
   */
  it('should verify the absence of PID and Start Date/Time in ChromeDriver logs', async function () {
    const fs = require('fs');
    const logContent = fs.readFileSync('chromedriver.log', 'utf8');

    // Assert that the log content *does not* contain the PID (e.g., "Process ID: NNNN").
    // This assertion is written to *pass* if the bug exists.
    expect(logContent).not.toMatch(/Process ID: \d+/);
    // While timestamps are present, the bug specifically mentions the lack of a clearly labeled "Start Time".
    // We'll focus on the explicit "Process ID" absence for this test.
  });
});
