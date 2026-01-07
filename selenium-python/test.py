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
import concurrent.futures
import time
from selenium import webdriver
from selenium.webdriver.chrome.options import Options
from selenium.webdriver.chrome.service import Service

# The chrome and chromedriver installation can take some time.
# Give 5 minutes to install everything.
TIMEOUT = 5 * 60 * 1000

def setup_driver():
    args = [
        'window-size=960,540',
        'disable-infobars',
        'disable-local-storage',
        'disable-notifications',
        'incognito',
        'no-sandbox',
        'disable-dev-shm-usage',
        'start-fullscreen',
        'headless=new',
        'kiosk'
    ]

    options = Options()
    options.set_capability('goog:loggingPrefs', {'browser': 'ALL', 'performance': 'INFO'})
    # options.platform_name = 'WINDOWS' # Commenting out as we are on Linux
    # options.capabilities['platformName'] = 'WINDOWS'

    options.add_experimental_option('excludeSwitches', ['enable-automation'])
    for arg in args:
        options.add_argument(arg)

    # Use local ChromeDriver instead of Remote
    service = Service(service_args=["--log-path=chromedriver.log", "--verbose"])
    return webdriver.Chrome(options=options, service=service)


def run_script_with_timer(duration):
    driver = setup_driver()
    try:
        start_time = time.time()
        driver.get(url='http://google.com')

        while time.time() - start_time < duration:
            # Check if driver is still alive/responsive
            script_result = driver.execute_script(script='return document.activeElement.accessKey')
            
            # Simulate some work/wait
            time.sleep(1)
            
            remaining_time = max(0, duration - (time.time() - start_time))
            logging.info(f"Remaining Time: {int(remaining_time)} seconds | Script Result: {script_result}")
    except Exception as e:
        logging.error(f"Exception occurred: {e}")
        raise e
    finally:
        driver.quit()


@pytest.mark.timeout(TIMEOUT)
def test_issue_reproduction():
    """
    Reproduces the issue where concurrent WebDriver instances cause a crash or TimeoutException.
    
    This test runs two Selenium WebDriver instances in parallel using ThreadPoolExecutor.
    Based on the bug report (crbug.com/42323747), running multiple drivers concurrently
    with specific options (headless, etc.) causes the browser or driver to become unresponsive
    or crash, leading to a TimeoutException or InvalidSessionIdException.
    
    We expect this test to fail if the bug is present.
    """
    duration = 30 # Reduced from 2400 for quick feedback

    with concurrent.futures.ThreadPoolExecutor(max_workers=2) as executor:
        # Submit two concurrent tasks that run the driver loop
        future1 = executor.submit(run_script_with_timer, duration)
        future2 = executor.submit(run_script_with_timer, duration)

        # Wait for both futures to complete and check for exceptions
        for future in concurrent.futures.as_completed([future1, future2]):
            try:
                future.result()
            except Exception as e:
                # If an exception occurs (like InvalidSessionIdException or TimeoutException),
                # fail the test. This confirms the bug reproduction.
                pytest.fail(f"Test failed with exception: {e}")
