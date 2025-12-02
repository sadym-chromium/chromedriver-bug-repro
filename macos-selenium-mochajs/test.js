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

  afterEach(async function () {
    if (driver) {
      await driver.quit();
    }
  });

  /**
   * This test is intended to verify the setup is correct.
   */
  it('should be able to navigate to google.com', async function () {
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
      `chromedriver-${new Date().toISOString()}.log`
    );

    const service = new chrome.ServiceBuilder(chromedriverBuild.executablePath)
      .loggingTo(chromedriverLogFile)
      .enableVerboseLogging();

    driver = await new Builder()
      .forBrowser('chrome')
      .setChromeOptions(options)
      .setChromeService(service)
      .build();

    await driver.get('https://www.google.com');
    const title = await driver.getTitle();
    expect(title).toBe('Google');
  });

  it('navigator.webdriver should be true even with --remote-debugging-port and enable-automation excluded', async function () {
    // This test reproduces https://crbug.com/chromedriver/4107.
    // The bug causes `navigator.webdriver` to be false when the
    // `enable-automation` switch is excluded and a remote debugging port is
    // set.
    //
    // The test configures the ChromeDriver to launch Chrome with the exact
    // capabilities described in the bug report.
    //
    // 1. Exclude the 'enable-automation' switch.
    // 2. Set a non-zero remote debugging port.
    //
    // It then navigates to a blank page and executes a script to get the value
    // of `navigator.webdriver`.
    //
    // The WebDriver specification states that `navigator.webdriver` should be
    // `true` when the browser is under control of a WebDriver. In this case,
    // even though 'enable-automation' is excluded, ChromeDriver should ensure
    // that `navigator.webdriver` remains `true`. The bug causes this to be
    // `false`.
    //
    // Therefore, this test asserts that the value is `true`, and it is
    // expected to fail if the bug is present.
    const options = new chrome.Options();
    options.addArguments('--headless');
    options.addArguments('--no-sandbox');
    options.addArguments('--remote-debugging-port=9222');
    options.excludeSwitches('enable-automation');
    options.setBinaryPath(chromeBuild.executablePath);

    const chromedriverLogDir = path.join(__dirname, 'logs');
    if (!fs.existsSync(chromedriverLogDir)) {
      fs.mkdirSync(chromedriverLogDir);
    }
    const chromedriverLogFile = path.join(
      chromedriverLogDir,
      `chromedriver-${new Date().toISOString()}.log`
    );

    const service = new chrome.ServiceBuilder(chromedriverBuild.executablePath)
      .loggingTo(chromedriverLogFile)
      .enableVerboseLogging();

    driver = await new Builder()
      .forBrowser('chrome')
      .setChromeOptions(options)
      .setChromeService(service)
      .build();

    await driver.get('about:blank');
    const navigatorWebdriver = await driver.executeScript(
      'return navigator.webdriver'
    );
    expect(navigatorWebdriver).toBe(true);
  });
});
