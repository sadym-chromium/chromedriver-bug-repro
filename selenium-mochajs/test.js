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

const { Builder, By, until } = require('selenium-webdriver');
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
    options.addArguments('--window-size=1920,1080');

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
   * This test is intended to verify the setup is correct.
   */
  it('should be able to navigate to google.com', async function () {
    await driver.get('https://www.google.com');
    const title = await driver.getTitle();
    expect(title).toBe('Google');
  });

  it('should reproduce element not interactable error on cookieyes.com', async function () {
    const url = 'https://app.cookieyes.com/signup';
    await driver.get(url);

    // Wait for the consent banner and click the reject button
    // Selector from the issue: body > div.cky-consent-container.cky-banner-bottom > div > div > div > div.cky-notice-btn-wrapper > button.cky-btn.cky-btn-reject
    const consentButtonSelector = By.css("body > div.cky-consent-container.cky-banner-bottom > div > div > div > div.cky-notice-btn-wrapper > button.cky-btn.cky-btn-reject");
    
    // We try to wait for it, but if it doesn't appear, we might proceed or fail. 
    try {
        const consentButton = await driver.wait(until.elementLocated(consentButtonSelector), 5000);
        await driver.wait(until.elementIsVisible(consentButton), 5000);
        await consentButton.click();
    } catch (e) {
        console.log('Consent button not found or not clickable, proceeding. Error:', e.message);
    }

    // Attempt to interact with the input field
    // The user used #__BVID__10 which might be dynamic. We find the email input dynamically to be robust.
    let input;
    try {
        input = await driver.wait(until.elementLocated(By.css("input[type='email']")), 10000);
    } catch (e) {
        // Fallback to the selector from the issue if generic one fails
        input = await driver.wait(until.elementLocated(By.css("#__BVID__10")), 10000);
    }
    
    // The bug report states that interaction fails with "element not interactable".
    // This implies the element is found but cannot be clicked/typed into.
    // We attempt to click the input, which triggers the bug.
    await input.click(); 
    
    await input.sendKeys("hello world");
  });
});
