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
   * This test is intended to verify the setup is correct.
   */
  it('should be able to navigate to google.com', async function () {
    await driver.get('https://www.google.com');
    const title = await driver.getTitle();
    expect(title).toBe('Google');
  });

  it('ISSUE REPRODUCTION', async function () {
    // Reproduce crbug.com/42323527
    // Recent chromedriver versions appear to be mangling href properties containing `?`
    
    // Create a link with a javascript: href containing a '?'
    const href = 'javascript:void(0)?foo=bar';
    
    // We can just use a data URL or inject HTML into the current page
    await driver.get('data:text/html,<body></body>');
    
    await driver.executeScript(`
      const a = document.createElement('a');
      a.href = arguments[0];
      a.id = 'test-link';
      a.textContent = 'Click me';
      document.body.appendChild(a);
    `, href);

    const link = await driver.findElement({ id: 'test-link' });
    const actualHref = await link.getAttribute('href');
    
    // The bug states that the property is mangled (URL encoded after '?')
    // javascript:void(0)?foo=bar might become javascript:void(0)?foo%3Dbar
    // The expectation is that it remains unmangled.
    expect(actualHref).toBe(href);
  });
});
