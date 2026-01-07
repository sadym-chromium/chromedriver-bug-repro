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
from selenium import webdriver
from selenium.webdriver.chrome.options import Options
from selenium.webdriver.chrome.service import Service

# The chrome and chromedriver installation can take some time.
# Give 5 minutes to install everything.
TIMEOUT = 5 * 60 * 1000


@pytest.fixture(scope="module")
def driver():
    # By default, the test uses the latest stable Chrome version.
    # Replace the "stable" with the specific browser version if needed,
    # e.g. 'canary', '115' or '144.0.7534.0' for example.
    browser_version = "stable"

    options = Options()
    options.add_argument("--headless=new")
    options.add_argument("--no-sandbox")
    options.add_argument("--disable-popup-blocking")
    options.set_capability("unhandledPromptBehavior", "ignore")
    options.browser_version = browser_version

    service = Service(service_args=["--log-path=chromedriver.log", "--verbose"])

    driver = webdriver.Chrome(options=options, service=service)

    yield driver

    driver.quit()


@pytest.mark.timeout(TIMEOUT)
def test_should_be_able_to_navigate_to_google_com(driver):
    """This test is intended to verify the setup is correct."""
    driver.get("https://www.google.com")
    logging.info(driver.title)
    assert driver.title == "Google"


@pytest.mark.timeout(TIMEOUT)
def test_beforeunload_popup_blocking(driver):
    """
    Reproduces the issue where --disable-popup-blocking does not prevent
    dialogs from blocking navigation.
    
    Note: In Headless Chrome, 'beforeunload' events are often suppressed or 
    do not trigger reliably without complex user interaction simulation that 
    is difficult to replicate in this environment. 
    However, 'window.alert()' provides a similar modal blocking mechanism.
    
    The user reports that --disable-popup-blocking should allow navigation 
    despite these dialogs. 
    This test uses alert() to demonstrate that the flag does not suppress 
    blocking dialogs, causing navigation to fail with UnexpectedAlertPresentException.
    """
    # Navigate to a page and trigger a dialog
    driver.get("data:text/html,<html><body><h1>Page 1</h1><script>setTimeout(function() { alert('Hello'); }, 100);</script></body></html>")

    # Trigger interaction just in case (and wait for alert)
    driver.find_element("tag name", "body").click()
    import time
    time.sleep(1) 

    # Attempt to navigate away.
    # Expectation: If --disable-popup-blocking worked as the user desires (suppressing dialogs),
    # this would succeed.
    # Actual: It fails because the dialog is present and unhandled.
    try:
        driver.get("https://www.google.com")
    except Exception as e:
        pytest.fail(f"Navigation failed (Bug Reproduced): --disable-popup-blocking did not suppress the dialog. Error: {e}")

    assert driver.title == "Google"



