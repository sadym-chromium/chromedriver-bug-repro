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
import os
import shutil
from selenium import webdriver
from selenium.webdriver.chrome.options import Options
from selenium.webdriver.chrome.service import Service

# The chrome and chromedriver installation can take some time.
# Give 5 minutes to install everything.
TIMEOUT = 5 * 60 * 1000

@pytest.fixture(scope="module")
def driver():
    # By default, the test uses the latest stable Chrome version.
    options = Options()
    options.add_argument("--headless=new")
    options.add_argument("--no-sandbox")

    # Use standard chromedriver for the sanity check test
    driver = webdriver.Chrome(options=options)

    yield driver

    driver.quit()


@pytest.mark.timeout(TIMEOUT)
def test_should_be_able_to_navigate_to_google_com(driver):
    """This test is intended to verify the setup is correct."""
    driver.get("https://www.google.com")
    logging.info(driver.title)
    assert driver.title == "Google"


@pytest.mark.timeout(TIMEOUT)
def test_issue_reproduction():
    """
    Reproduces issue 390216620: 
    Setting custom user-data-directory AND chromedriver binary path arguments causes Chrome error
    in chrome-headless-shell.
    """
    base_dir = os.getcwd()
    chrome_binary_path = os.path.join(base_dir, "chrome-headless-shell-linux64", "chrome-headless-shell")
    chromedriver_binary_path = os.path.join(base_dir, "chromedriver-linux64", "chromedriver")
    user_data_directory = os.path.join(base_dir, "user_data_dir_repro")
    
    # Clean up user data dir if exists
    if os.path.exists(user_data_directory):
        shutil.rmtree(user_data_directory)
    os.makedirs(user_data_directory)

    print(f"Chrome binary: {chrome_binary_path}")
    print(f"Chromedriver binary: {chromedriver_binary_path}")
    print(f"User data dir: {user_data_directory}")

    options = Options()
    options.binary_location = chrome_binary_path
    options.add_argument(f"--user-data-dir={user_data_directory}")
    options.add_argument("--headless=new")
    
    # Add other arguments mentioned in the bug report to be sure
    options.add_argument('--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36')
    options.add_experimental_option("prefs", {
        "download.directory_upgrade": True,
        "download.prompt_for_download": False,
        "credentials_enable_service": False,
        "profile.password_manager_enabled": False
    })
    options.add_argument('--start-maximized')
    options.add_argument('--disable-notifications')
    options.add_argument("--disable-third-party-cookies")
    options.add_argument("--disable-dev-shm-usage")
    options.add_argument("--no-sandbox")
    options.add_argument("--disable-extensions")
    options.add_experimental_option("excludeSwitches", ["enable-automation", "enable-logging"])
    options.add_experimental_option("useAutomationExtension", False)
    options.add_argument('--log-level=3')

    service = Service(executable_path=chromedriver_binary_path)

    driver = None
    try:
        driver = webdriver.Chrome(service=service, options=options)
        driver.get("https://www.google.com")
        print("Driver initialized successfully")
    except Exception as e:
        pytest.fail(f"Failed to initialize WebDriver: {e}")
    finally:
        if driver:
            driver.quit()
        if os.path.exists(user_data_directory):
            shutil.rmtree(user_data_directory)