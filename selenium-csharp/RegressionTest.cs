// Copyright 2025 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

using NUnit.Framework;
using OpenQA.Selenium;
using OpenQA.Selenium.Chrome;
using System;
using System.IO;

namespace RegressionTest;

public class Tests
{
    [Test]
    public void ShouldThrowExceptionWhenNavigatingWithServiceWorkerWindowType()
    {
        var options = new ChromeOptions();
        options.AddArgument("--headless");
        options.AddArgument("--no-sandbox");
        options.BrowserVersion = "stable";
        
        // Configuration to reproduce the bug: include "service_worker" in windowTypes
        options.AddWindowTypes("webview", "service_worker");

        var service = ChromeDriverService.CreateDefaultService();
        service.LogPath = "chromedriver.log";
        service.EnableVerboseLogging = true;

        IWebDriver driver = null;
        try
        {
            driver = new ChromeDriver(service, options);

            // Bug 42323033: Navigation fails with "web view not found" when service_worker windowType is enabled
            // We expect this to SUCCEED if the bug is fixed.
            // If the bug exists, this line will throw WebDriverException: unknown error: web view not found
            driver.Navigate().GoToUrl("https://mdn.github.io/dom-examples/service-worker/simple-service-worker/");
            
            // Verify navigation succeeded
            Assert.That(driver.Url, Does.Contain("mdn.github.io"), "Navigation failed or redirected unexpectedly.");
        }
        finally
        {
            driver?.Quit();
        }
    }
}
