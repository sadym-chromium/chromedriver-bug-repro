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
import threading
import time
from http.server import HTTPServer, BaseHTTPRequestHandler
from selenium import webdriver
from selenium.webdriver.chrome.options import Options
from selenium.webdriver.chrome.service import Service

# The chrome and chromedriver installation can take some time.
# Give 5 minutes to install everything.
TIMEOUT = 5 * 60 * 1000

class DelayedRequestHandler(BaseHTTPRequestHandler):
    def do_GET(self):
        if self.path == '/slow':
            # Simulate a page that hangs/takes a long time to load
            time.sleep(10) 
            self.send_response(200)
            self.end_headers()
            self.wfile.write(b"Done")
        else:
            self.send_response(200)
            self.end_headers()
            self.wfile.write(b"<html><a href='/slow' target='_blank' id='link'>Click me</a></html>")
    
    def log_message(self, format, *args):
        pass # Suppress server logs

@pytest.fixture(scope="module")
def driver():
    # By default, the test uses the latest stable Chrome version.
    # Replace the "stable" with the specific browser version if needed,
    # e.g. 'canary', '115' or '144.0.7534.0' for example.
    browser_version = "stable"

    options = Options()
    options.add_argument("--headless")
    options.add_argument("--no-sandbox")
    options.browser_version = browser_version

    service = Service(service_args=["--log-path=chromedriver.log", "--verbose"])

    driver = webdriver.Chrome(options=options, service=service)

    yield driver

    driver.quit()

@pytest.fixture(scope="module")
def test_server():
    server = HTTPServer(('localhost', 0), DelayedRequestHandler)
    thread = threading.Thread(target=server.serve_forever)
    thread.daemon = True
    thread.start()
    yield server
    server.shutdown()

@pytest.mark.timeout(TIMEOUT)
def test_should_be_able_to_navigate_to_google_com(driver):
    """This test is intended to verify the setup is correct."""
    driver.get("https://www.google.com")
    logging.info(driver.title)
    assert driver.title == "Google"

@pytest.mark.timeout(15) # Timeout shorter than the sleep in DelayedRequestHandler might not be enough if we want to confirm it DOESN'T hang? 
# Wait, if it hangs, it will hang forever (or until socket timeout).
# If we set timeout to 5s, and the page takes 10s to load.
# If the bug exists, window_handles will hang until the page loads (10s) or forever.
# If I set test timeout to 5s, and it hangs, the test fails (Timed out).
# If I set test timeout to 5s, and it works, window_handles returns immediately (even if page is loading).
# So 5s timeout is good.
def test_issue_reproduction(driver, test_server):
    """
    Reproduces crbug.com/42323653: testShouldHandleNewWindowLoadingProperly fails with chrome-headless-shell
    The test stacks on the line that issues GetWindows command (driver.window_handles)
    when a new window is loading a page that doesn't respond immediately.
    
    NOTE: This test is expected to PASS on standard Chrome (headless=new) but FAIL (timeout)
    on chrome-headless-shell (headless=old) if the bug is present.
    """
    port = test_server.server_port
    base_url = f"http://localhost:{port}"
    
    print(f"Server started at {base_url}")

    # 1. Open the main page
    driver.get(base_url)
    
    # 2. Open a new window that takes time to load
    # Use window.open to open a new window/tab
    driver.execute_script(f"window.open('{base_url}/slow');")
    
    # 3. Try to get window handles immediately.
    # This should NOT block waiting for the page to load.
    start_time = time.time()
    handles = driver.window_handles
    end_time = time.time()
    
    print(f"window_handles took {end_time - start_time} seconds")
    
    # If we reach here, check we have at least 2 windows
    assert len(handles) >= 2