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
    options.addArguments('--headless=new');
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
    // Suppress error if driver is already quit/killed
    try {
      await driver.quit();
    } catch (e) {
      // ignore
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

  it('ISSUE REPRODUCTION', async function () {
    const { execSync } = require('child_process');
    
    // Function to get process details: PID, PPID, CMD
    function getProcesses() {
      const output = execSync('ps -A -o pid,ppid,args').toString();
      const lines = output.split('\n').slice(1); // Skip header
      return lines.map(line => {
        const parts = line.trim().split(/\s+/);
        // pid is index 0, ppid is index 1, args are the rest
        if (parts.length < 3) return null;
        return {
          pid: parseInt(parts[0]),
          ppid: parseInt(parts[1]),
          cmd: parts.slice(2).join(' ')
        };
      }).filter(p => p !== null);
    }

    // 1. Find the chromedriver process that is a child of this node process
    const processes = getProcesses();
    const currentPid = process.pid;
    
    const chromedriverProcess = processes.find(p => 
      p.ppid === currentPid && p.cmd.includes('chromedriver')
    );

    if (!chromedriverProcess) {
      console.log('Current PID:', currentPid);
      console.log('Processes:', processes);
      throw new Error('Could not find chromedriver process started by this test');
    }

    console.log(`Found Chromedriver PID: ${chromedriverProcess.pid}`);

    // 2. Find the Chrome process that is a child of the chromedriver process
    // Note: Chrome might spawn multiple processes (renderer, gpu, etc.). 
    // We look for the main process which should be a direct child of chromedriver.
    const chromeProcess = processes.find(p => 
      p.ppid === chromedriverProcess.pid && p.cmd.toLowerCase().includes('chrome')
    );

    if (!chromeProcess) {
      throw new Error('Could not find Google Chrome process started by chromedriver');
    }

    console.log(`Found Chrome PID: ${chromeProcess.pid}`);

    // 3. Send SIGINT to chromedriver
    console.log(`Sending SIGINT to Chromedriver (PID ${chromedriverProcess.pid})...`);
    process.kill(chromedriverProcess.pid, 'SIGINT');

    // 4. Wait for cleanup
    await new Promise(resolve => setTimeout(resolve, 5000));

    // 5. Check if Chrome process is still running
    // We refresh the process list
    const currentProcesses = getProcesses();
    const isChromeRunning = currentProcesses.some(p => p.pid === chromeProcess.pid);

    if (isChromeRunning) {
        throw new Error(`Chrome process (PID ${chromeProcess.pid}) is still running after Chromedriver SIGINT! Orphaned process detected.`);
    } else {
        console.log('Chrome process successfully terminated.');
    }
  });
});
