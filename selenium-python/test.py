#  Copyright 2025 Google LLC
#
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.

import logging
import pytest
import time
import http.server
import socketserver
import threading
import os
from selenium import webdriver
from selenium.webdriver.chrome.options import Options
from selenium.webdriver.chrome.service import Service

TIMEOUT = 5 * 60 * 1000
PORT = 8081

# Server setup
class ThreadedHTTPServer(object):
    def __init__(self, host, port):
        class ThreadingServer(socketserver.ThreadingMixIn, socketserver.TCPServer):
            allow_reuse_address = True
        
        self.server = ThreadingServer((host, port), http.server.SimpleHTTPRequestHandler)
        self.server_thread = threading.Thread(target=self.server.serve_forever)
        self.server_thread.daemon = True

    def start(self):
        self.server_thread.start()

    def stop(self):
        self.server.shutdown()
        self.server.server_close()

@pytest.fixture(scope="module")
def local_server():
    # Change directory to pages so the server serves them at root
    os.chdir("selenium-python/pages")
    server = ThreadedHTTPServer("localhost", PORT)
    server.start()
    yield server
    server.stop()
    # Revert directory
    os.chdir("../..")

@pytest.fixture(scope="module")
def driver():
    browser_version = "stable"
    options = Options()
    options.add_argument("--headless")
    options.add_argument("--no-sandbox")
    options.browser_version = browser_version
    
    # Enable browser logging
    options.set_capability('goog:loggingPrefs', {'browser': 'ALL'})

    service = Service(service_args=["--log-path=chromedriver.log", "--verbose"])
    driver = webdriver.Chrome(options=options, service=service)

    yield driver

    driver.quit()

@pytest.mark.timeout(TIMEOUT)
def test_console_log_from_iframe(driver, local_server):
    """
    Reproduces Bug 42323329: Console log coming from an iframe cannot be accessed by selenium.
    
    Steps:
    1. Navigate to a local page with an iframe that logs to console.
    2. Wait for logs to be generated.
    3. Retrieve browser logs.
    4. Assert that logs from the iframe are present.
    """
    url = f"http://localhost:{PORT}/index.html"
    driver.get(url)
    
    # Allow some time for the iframe to load and generate logs
    time.sleep(2)
    
    logs = driver.get_log("browser")
    
    print("Captured Logs:")
    for log in logs:
        print(log)

    # Filter for logs from the iframe
    iframe_logs = [
        log for log in logs 
        if "Log from iframe" in log.get('message', '') 
    ]
    
    # The bug is that these logs are missing for cross-origin iframes.
    assert len(iframe_logs) > 0, "No logs found from the cross-origin iframe. Bug reproduced."