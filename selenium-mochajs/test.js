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
    // Create a simple HTML page with two textareas: one at the top and one at the bottom.
    // The bottom textarea will require scrolling to be visible.
    const htmlContent = `
      <!DOCTYPE html>
      <html>
      <head>
        <title>SendKeys Bug Reproduction</title>
        <style>
          body {
            height: 2000px; /* Make the page tall enough to require scrolling */
          }
          #topTextarea {
            margin-top: 50px;
          }
          #bottomTextarea {
            margin-top: 1500px;
          }
        </style>
      </head>
      <body>
        <textarea id="topTextarea" rows="4" cols="50"></textarea>
        <textarea id="bottomTextarea" rows="4" cols="50"></textarea>
      </body>
      </html>
    `;

    // Navigate to the HTML page using a data URI.
    await driver.get(`data:text/html;charset=utf-8,${encodeURIComponent(htmlContent)}`);

    // The string to send, containing multiple forward slashes.
    const textToSend = 'Test 1/ 2// 3///';

    // Find the top textarea element.
    const topTextarea = await driver.findElement(By.id('topTextarea'));
    // Send keys to the top textarea.
    await topTextarea.sendKeys(textToSend);
    // Verify content of top textarea - this should work correctly.
    let topText = await topTextarea.getAttribute('value');
    expect(topText).toBe(textToSend);

    // Find the bottom textarea element.
    const bottomTextarea = await driver.findElement(By.id('bottomTextarea'));
    // Send keys to the bottom textarea. This is where the bug is expected to occur.
    await bottomTextarea.sendKeys(textToSend);

    let bottomText = await bottomTextarea.getAttribute('value');
    console.log('Bottom Text Actual:', bottomText);
    const actualSlashes = (bottomText.match(/\//g) || []).length;
    console.log('Actual Slashes:', actualSlashes);
    // According to the bug report (crbug.com/42323689, Comment #4),
    // when sendKeys sends a "/", it can be misinterpreted as a "PageUp" key,
    // especially when the element requires scrolling. This leads to missing slashes.
    // The test is designed to FAIL if the bug is present, meaning the actual text
    // will NOT match the `textToSend`. So, we ASSERT that it *does* match,
    // which will fail if the bug is reproduced (slashes are missing).
    expect(bottomText).toBe(textToSend);
    // To be even more specific based on the bug description:
    // We expect the number of slashes to be equal to the expected number of slashes.
    const expectedSlashes = (textToSend.match(/\//g) || []).length;
    expect(actualSlashes).toBe(expectedSlashes);  });
});
