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

const { Builder, By } = require('selenium-webdriver');
const { expect } = require('expect');
const chrome = require('selenium-webdriver/chrome');
const fs = require('fs');
const path = require('path');
const {
  install,
  Browser,
  resolveBuildId,
  detectBrowserPlatform,
  ChromeReleaseChannel,
} = require('@puppeteer/browsers');
const winston = require('winston');

const logger = winston.createLogger({
  level: process.env.LOG_LEVEL || 'info',
  format: winston.format.cli(),
  transports: [new winston.transports.Console()],
});

describe('Selenium chromedriver', function () {
  // The deafult timeout of 2s is not enough for selenium tests.
  this.timeout(60 * 1000);

  let driver;
  let chromedriverBuild;
  let chromeBuild;

  before(async function () {
    // The chrome and chromedriver installation can take some time. Give 5
    // minutes to install everything.
    this.timeout(5 * 60 * 1000);
    /**
     * By default, the test uses the latest Chrome version. Replace with the
     * specific Chromium version if needed, e.g. "144.0.7553.0" or use
     * environment variable like `BROWSER_VERSION=142.0.7444.175`
     */
    const BROWSER_VERSION =
      process.env['BROWSER_VERSION'] ??
      (await resolveBuildId(
        Browser.CHROME,
        detectBrowserPlatform(),
        ChromeReleaseChannel.CANARY,
      ));

    const cacheDir = path.resolve(__dirname, '.cache');

    logger.debug(`Chrome version: ${BROWSER_VERSION}`);

    chromeBuild = await install({
      browser: Browser.CHROME,
      buildId: BROWSER_VERSION,
      cacheDir: cacheDir,
    });

    chromedriverBuild = await install({
      browser: Browser.CHROMEDRIVER,
      buildId: BROWSER_VERSION,
      cacheDir: cacheDir,
    });

    logger.debug(`Chrome installed at: ${chromeBuild.executablePath}`);
    logger.debug(
      `ChromeDriver installed at: ${chromedriverBuild.executablePath}`,
    );
  });

  beforeEach(async function () {
    logger.debug(`Launching Chrome at ${chromeBuild.executablePath}`);

    const options = new chrome.Options();
    options.addArguments('--headless');
    options.addArguments('--no-sandbox');
    options.setBinaryPath(chromeBuild.executablePath);

    const chromedriverLogDir = path.join(__dirname, 'logs');
    if (!fs.existsSync(chromedriverLogDir)) {
      fs.mkdirSync(chromedriverLogDir);
    }
    const chromedriverLogFile = path.join(
      chromedriverLogDir,
      `chromedriver-${new Date().toISOString()}.log`,
    );

    const service = new chrome.ServiceBuilder(chromedriverBuild.executablePath)
      .loggingTo(chromedriverLogFile)
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
   * This test is intended to verify the setup is correct.
   */
  it('should be able to navigate to google.com', async function () {
    await driver.get('https://www.google.com');
    const title = await driver.getTitle();
    expect(title).toBe('Google');
  });

  it('should support pointer button value of 5 for pen eraser', async function () {
    // This test reproduces the bug described in https://b.corp.google.com/issues/42323172.
    // The bug is that ChromeDriver does not support a pointer button value of 5, which is
    // reserved for the pen eraser button according to the W3C Pointer Events specification:
    // https://www.w3.org/TR/pointerevents/#the-button-property
    //
    // The test performs a pointer down action with a button value of 5.
    // If the bug is present, ChromeDriver will throw an "invalid argument" error because
    // it currently only accepts button values between 0 and 4.
    //
    // Therefore, this test is expected to FAIL if the bug exists. A successful run of
    // this test indicates that the bug has been fixed.

    // Navigate to a simple page to perform the action on.
    await driver.get('data:text/html,<html><body></body></html>');

    // Get a reference to the body element.
    const body = await driver.findElement(By.css('body'));

    // Create a pointer action sequence.
    const actions = driver.actions();
    // The selenium-webdriver API does not provide a way to create a 'pen' pointer.
    // As a workaround, we get the 'mouse' pointer and change its type to 'pen'.
    actions.mouse_.pointerType_ = 'pen';

    // The test expects that no error is thrown.
    // If the bug is present, the following call will fail with an "invalid argument" error.
    await actions.move({x: 10, y: 10, origin: body}).press(5).perform();
  });
});
