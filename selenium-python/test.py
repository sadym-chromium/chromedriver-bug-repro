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
    options.add_argument("--headless")
    options.add_argument("--no-sandbox")
    options.browser_version = browser_version

    service = Service(service_args=["--log-path=chromedriver.log", "--verbose"])

    driver = webdriver.Chrome(options=options, service=service)

    yield driver

    driver.quit()


@pytest.mark.timeout(TIMEOUT)
def test_w3c_is_default_session(driver):
    """
    This test verifies that a W3C compliant session is created by default.
    It checks for the presence of a W3C-specific capability.
    """
    logging.info(f"Default session capabilities: {driver.capabilities}")
    assert 'goog:chromeOptions' in driver.capabilities, "The default session should be W3C compliant."


@pytest.mark.timeout(TIMEOUT)
def test_legacy_protocol_is_still_supported():
    """
    Bug: Legacy non-W3C compliant mode is still supported.
    This test attempts to create a legacy protocol session.
    It is EXPECTED TO FAIL if the bug is present, because a non-W3C
    session will be created successfully.
    """
    options = Options()
    options.add_argument("--headless")
    options.add_argument("--no-sandbox")
    
    # Set the 'w3c' capability to False to request a legacy session.
    options.set_capability("w3c", False)

    try:
        legacy_driver = webdriver.Chrome(options=options)
        logging.info(f"Legacy session capabilities: {legacy_driver.capabilities}")
        
        # If a driver is created, the bug is present.
        # We verify it's a non-W3C session by checking the absence of a W3C-specific capability.
        is_w3c = 'goog:chromeOptions' in legacy_driver.capabilities
        
        legacy_driver.quit()
        
        if not is_w3c:
            pytest.fail("BUG REPRODUCED: A legacy non-W3C session was successfully created.")
        else:
            pytest.fail("A W3C session was created even when legacy mode was requested.")

    except Exception as e:
        # If session creation fails, it means legacy protocol is not supported,
        # which would mean the bug is fixed. This test would then pass.
        logging.info(f"Session creation with w3c:False failed, which is the desired behavior. Exception: {e}")
        assert "session not created" in str(e)
