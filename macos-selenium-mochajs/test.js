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

  /**
   * This test reproduces bug 42322459.
   *
   * The bug occurs when trying to find an element in a new window after the
   * page in the new window redefines `window.Symbol`. The `find_element`
   * command fails with an `InvalidArgumentException` and the message
   * "invalid argument: Unsupported locator strategy: null".
   *
   * This test is expected to fail with this error, thus confirming the
   * presence of the bug. The assertion checks for the element to be found,
   * which is the correct behavior.
   */
  it('reproduces bug 42322459', async function () {
    // Get the absolute path to the index.html file.
    const indexPath = path.resolve(__dirname, 'index.html');

    // 1. Navigate to the local index.html file.
    await driver.get(`file://${indexPath}`);

    // 2. Click the button to open new_window.html in a new window.
    await driver.findElement({ id: 'openWindowBtn' }).click();

    // 3. Switch to the new window.
    const windows = await driver.getAllWindowHandles();
    await driver.switchTo().window(windows[1]);

    // 4. Wait for the new window to load. A long sleep is used in the bug
    //    report, so we use it here as well.
    await driver.sleep(5000);

    // 5. Try to find an element by class name.
    //    This is where the error is expected to happen.
    const elements = await driver.findElements({ className: 'header_area' });

    // The WebDriver spec says that `findElements` should return an empty
    // list if no elements are found. In the presence of the bug, it throws
    // an exception.
    // The assertion below checks for the correct behavior (finding one
    // element). This will fail if the bug is present.
    expect(elements.length).toBe(1);
  });
});
