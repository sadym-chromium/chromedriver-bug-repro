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
import time
import pytest
from selenium import webdriver
from selenium.webdriver.chrome.options import Options
from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC

# The chrome and chromedriver installation can take some time.
# Give 5 minutes to install everything.
TIMEOUT = 5 * 60 * 1000

def create_driver():
    options = Options()
    options.add_argument("--headless=new") # Use new headless mode
    options.add_argument("--no-sandbox")
    options.browser_version = "stable"
    
    driver = webdriver.Chrome(options=options)
    return driver

@pytest.mark.timeout(TIMEOUT)
def test_raf_in_background():
    """
    Reproduces Issue 42323455: requestAnimationFrame() not being called in background browsers.
    """
    
    html_content = """
    <!DOCTYPE html>
    <html>
    <body>
    <div id="status">Pending</div>
    <script>
        function update() {
            document.getElementById('status').innerText = 'Done';
        }
        function startUpdate() {
            // Use requestAnimationFrame to trigger the update
            requestAnimationFrame(update);
        }
    </script>
    </body>
    </html>
    """
    
    data_url = f"data:text/html;charset=utf-8,{html_content}"

    driver1 = None
    driver2 = None
    
    try:
        logging.info("Starting Driver 1...")
        driver1 = create_driver()
        
        logging.info("Starting Driver 2...")
        driver2 = create_driver()
        
        # Load page in Driver 1
        logging.info("Loading page in Driver 1")
        driver1.get(data_url)
        
        # Minimize Driver 1 to put it in background
        logging.info("Minimizing Driver 1...")
        driver1.minimize_window()
        
        # Verify Driver 1 is hidden
        vis1 = driver1.execute_script("return document.visibilityState;")
        logging.info(f"Driver 1 visibilityState: {vis1}")
        
        # Trigger rAF in Driver 1 (Background)
        logging.info("Triggering rAF in Driver 1 (Background)...")
        driver1.execute_script("startUpdate();")
        
        # Load page in Driver 2 (Foreground)
        logging.info("Loading page in Driver 2")
        driver2.get(data_url)
        logging.info("Triggering rAF in Driver 2 (Foreground)...")
        driver2.execute_script("startUpdate();")

        # Define a wait function
        def wait_for_done(driver, name):
            logging.info(f"Waiting for 'Done' in {name}...")
            try:
                WebDriverWait(driver, 5).until(
                    EC.text_to_be_present_in_element((By.ID, "status"), "Done")
                )
                logging.info(f"{name}: Success - text updated.")
            except Exception as e:
                logging.error(f"{name}: Failed - text did not update. Error: {e}")
                raise e

        # Check Driver 2 first (Foreground - should pass)
        wait_for_done(driver2, "Driver 2")
        
        # Check Driver 1 (Background - expected to fail if bug exists)
        wait_for_done(driver1, "Driver 1")
        
    finally:
        if driver1:
            driver1.quit()
        if driver2:
            driver2.quit()
