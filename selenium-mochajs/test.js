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
const { WebSocketServer } = require('ws');

describe('Selenium ChromeDriver', function () {
  let driver;
  let wss;
  let wsClosedCode;
  let wsWasClean;

  // The chrome and chromedriver installation can take some time. 
  // Give 5 minutes to install everything.
  this.timeout(5 * 60 * 1000);

  before(function (done) {
    wss = new WebSocketServer({ port: 8080 });
    wss.on('connection', function connection(ws) {
      ws.on('close', function close(code, reason) {
        wsClosedCode = code;
        wsWasClean = reason.toString() === 'true'; // reason is a Buffer
        console.log(`WebSocket closed: Code ${wsClosedCode}, WasClean: ${wsWasClean}`);
      });
      ws.on('message', function message(data) {
        console.log('received: %s', data);
      });
      ws.send('something');
    });
    wss.on('listening', () => {
      console.log('WebSocket server started on port 8080');
      done();
    });
    wss.on('error', (error) => {
      console.error('WebSocket server error:', error);
      done(error);
    });
  });

  after(function () {
    if (wss) {
      wss.close();
      console.log('WebSocket server closed.');
    }
  });

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
    // driver.quit() is handled within the 'ISSUE REPRODUCTION' test case to allow
    // assertion after browser closure. If other tests are added that don't quit
    // the driver themselves, it should be called here.
  });

  /**
   * This test is intended to verify the setup is correct.
   */
  it('should be able to navigate to google.com', async function () {
    await driver.get('https://www.google.com');
    const title = await driver.getTitle();
    expect(title).toBe('Google');
    await driver.quit(); // Quit driver for this test
  });

  it('ISSUE REPRODUCTION: Websockets are closed abnormally with code 1006', async function () {
    // HTML content for a page that opens a WebSocket connection
    const htmlContent = `
      <!DOCTYPE html>
      <html>
      <head>
          <title>WebSocket Test</title>
      </head>
      <body>
          <h1>WebSocket Test Page</h1>
          <script>
              const ws = new WebSocket('ws://localhost:8080');
              ws.onopen = (event) => {
                  console.log('WebSocket opened');
                  ws.send('Hello from client');
              };
              ws.onmessage = (event) => {
                  console.log('WebSocket message:', event.data);
              };
              ws.onclose = (event) => {
                  console.log('WebSocket closed:', event.code, event.wasClean);
                  // In a real scenario, you might send this info to a server
                  // or store it in a way retrievable by Selenium, but for
                  // reproduction, we rely on the server-side listener.
              };
              ws.onerror = (error) => {
                  console.error('WebSocket error:', error);
              };
          </script>
      </body>
      </html>
    `;

    // Navigate to the HTML page
    await driver.get('data:text/html;charset=utf-8,' + encodeURIComponent(htmlContent));

    // Wait for a short period to allow the WebSocket connection to establish
    await driver.sleep(2000); 

    // Crucially, quit the driver which closes the browser.
    // The server-side WebSocket 'close' event handler will capture the code.
    await driver.quit();

    // The bug states that the closure code is 1006 (abnormal).
    // The test is designed to FAIL if the bug is FIXED (i.e., code is 1001).
    // If the bug is PRESENT, the code will be 1006, and the test will PASS.
    // This confirms the bug reproduction.
    expect(wsClosedCode).toBe(1001);
    expect(wsWasClean).toBe(true);
  });
});