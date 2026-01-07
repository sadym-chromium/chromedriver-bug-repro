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
    // Options from the bug report
    options.addArguments('--mute-audio');
    options.addArguments('--autoplay-policy=no-user-gesture-required');
    
    // Existing options for stability in CLI environment
    options.addArguments('--headless=new');
    options.addArguments('--no-sandbox');
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

  it('should reproduce "no such execution context" error with MutationObserver', async function () {
    const testUrl = "https://www.youtube.com/watch?v=aqz-KE-bpKQ";
    await driver.get(testUrl);

    // Wait for the player to be located (using a stable selector for YouTube)
    // Note: The original repro used "#player .html5-video-player", but specific IDs might change.
    // We use "#movie_player" which is standard.
    await driver.wait(until.elementLocated(By.id("movie_player")), 20000);

    // The bug report states that this executeScript call triggers the error
    // "WebDriverError: no such execution context".
    // We add 'return' to ensure the driver waits for the Promise to resolve.
    // In this environment, it might timeout if the element doesn't appear,
    // but on affected versions, it throws the specific error.
    await driver.executeScript(`
        let observer;
        function createObserver() {
            return new Promise(function(resolve) {
                observer = new MutationObserver(function() {
                    const panel = document.querySelector('.html5-video-info-panel');
                    if (!panel) return;
                    observer.disconnect();
                    resolve(panel);
                });
                observer.observe(document, { childList: true, subtree: true });
            });
        }
        return (async function() {
            await createObserver();
        })();
    `);
  });
});
