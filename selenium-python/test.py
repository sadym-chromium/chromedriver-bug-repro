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
import json
import os
import time
from selenium import webdriver
from selenium.webdriver.chrome.options import Options
from selenium.webdriver.chrome.service import Service
from pypdf import PdfReader

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

@pytest.fixture
def print_driver(tmp_path):
    download_dir = str(tmp_path)
    
    app_state = {
        "recentDestinations": [
            {
                "id": "Save as PDF",
                "origin": "local",
                "account": ""
            }
        ],
        "selectedDestinationId": "Save as PDF",
        "isCssBackgroundEnabled": True,
        "isHeaderFooterEnabled": False,
        "isLandscapeEnabled": False,
        "version": 2,
        # A2 Size: 420mm x 594mm (approx 16.5 x 23.4 inches)
        "mediaSize": {"height_microns": 594000, "width_microns": 420000}
    }
    
    prefs = {
        'printing.print_preview_sticky_settings.appState': json.dumps(app_state),
        'savefile.default_directory': download_dir,
        'download.default_directory': download_dir,
        'download.prompt_for_download': False,
        'profile.default_content_settings.popups': 0,
        'profile.default_content_setting_values.automatic_downloads': 1
    }
    
    options = Options()
    options.add_experimental_option('prefs', prefs)
    options.add_argument('--kiosk-printing')
    options.add_argument('--headless=new')
    options.add_argument('--no-sandbox')
    options.add_argument('--disable-dev-shm-usage')
    options.add_argument('--disable-gpu')
    options.browser_version = "stable"
    
    # Use a separate log for this driver
    service = Service(service_args=["--log-path=chromedriver_repro.log", "--verbose"])
    
    driver = webdriver.Chrome(options=options, service=service)
    driver.set_window_size(1920, 1080)
    yield driver, download_dir
    driver.quit()

@pytest.mark.timeout(TIMEOUT)
def test_print_media_size(print_driver):
    """Reproduces crbug.com/42323666: Media size in appState not respected in kiosk printing.
       Note: This test expects the generated PDF to be A2 size (420mm x 594mm).
       If the bug exists, it will likely be Letter size.
    """
    driver, download_dir = print_driver
    
    # Use a simple data URL page
    driver.get("data:text/html,<html><body><h1>A2 Print Test</h1><p>Testing media size.</p></body></html>")
    
    # Execute print
    driver.execute_script('window.print();')
    
    # Wait for the PDF to be saved
    pdf_file = None
    timeout = 10
    end_time = time.time() + timeout
    while time.time() < end_time:
        files = [f for f in os.listdir(download_dir) if f.endswith(".pdf")]
        if files:
            pdf_file = os.path.join(download_dir, files[0])
            break
        time.sleep(0.5)
        
    if not pdf_file:
        # If PDF generation fails (common in some headless envs without printer setup), 
        # we cannot verify the size. But the test structure is correct.
        print(f"Directory contents: {os.listdir(download_dir)}")
    
    assert pdf_file, "PDF file was not generated within timeout. Cannot verify media size."
    
    # Inspect PDF dimensions
    reader = PdfReader(pdf_file)
    page = reader.pages[0]
    box = page.mediabox
    width_pts = float(box.width)
    height_pts = float(box.height)
    
    print(f"Generated PDF dimensions: {width_pts}x{height_pts} pts")
    
    # A2 dimensions in points (1/72 inch)
    # 420mm = 420 / 25.4 * 72 ~= 1190.55
    # 594mm = 594 / 25.4 * 72 ~= 1683.78
    
    expected_width = 1190.55
    expected_height = 1683.78
    tolerance = 50 # Allow some margin
    
    # Check if dimensions match A2 
    matches_a2 = (abs(width_pts - expected_width) < tolerance and abs(height_pts - expected_height) < tolerance)
    
    # If it fails, it will likely be Letter size (approx 612x792)
    assert matches_a2, f"Expected A2 size ({expected_width}x{expected_height}), but got {width_pts}x{height_pts} pts. The bug is reproduced."