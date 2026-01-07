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
import psutil
import time
from selenium import webdriver
from selenium.webdriver.chrome.options import Options
from selenium.webdriver.chrome.service import Service
from selenium.webdriver.common.by import By
from selenium.common.exceptions import NoSuchElementException

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
def test_should_be_able_to_navigate_to_google_com(driver):
    """This test is intended to verify the setup is correct."""
    driver.get("https://www.google.com")
    logging.info(driver.title)
    assert driver.title == "Google"


@pytest.mark.timeout(TIMEOUT)
def test_issue_reproduction(driver):
    """Reproduces the memory leak when repeatedly finding a non-existent element."""
    driver.get("data:text/html,<html><body><h1>Test</h1></body></html>")
    
    # helper to get total memory of chromedriver and its children (chrome processes)
    def get_memory_usage():
        try:
            parent = psutil.Process(driver.service.process.pid)
            children = parent.children(recursive=True)
            total_rss = parent.memory_info().rss
            for child in children:
                try:
                    total_rss += child.memory_info().rss
                except (psutil.NoSuchProcess, psutil.AccessDenied):
                    pass
            return total_rss / (1024 * 1024) # MB
        except (psutil.NoSuchProcess, psutil.AccessDenied):
            return 0

    # Wait for everything to settle
    time.sleep(2)
    initial_memory = get_memory_usage()
    print(f"\nInitial Memory: {initial_memory:.2f} MB")

    iterations = 2000
    print(f"Running {iterations} iterations of find_element...")
    
    for i in range(iterations):
        try:
            driver.find_element(By.CLASS_NAME, "menu")
        except NoSuchElementException:
            pass
        
        if (i + 1) % 500 == 0:
             print(f"Iteration {i + 1}: {get_memory_usage():.2f} MB")

    # Wait a bit after loop
    time.sleep(2)
    final_memory = get_memory_usage()
    print(f"Final Memory: {final_memory:.2f} MB")
    
    diff = final_memory - initial_memory
    print(f"Memory Difference: {diff:.2f} MB")
    
    # Assert that memory growth is within reasonable limits.
    # If the bug exists, this assertion should fail.
    # Setting a threshold of 50MB growth for 2000 iterations.
    assert diff < 50, f"Memory leaked significantly! Increased by {diff:.2f} MB"