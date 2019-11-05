package com.example.tests;

import static org.junit.Assert.fail;

import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.NoAlertPresentException;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.internal.FindsByTagName;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;

import com.thoughtworks.selenium.Selenium;
import com.thoughtworks.selenium.condition.Presence;

public class CreatingChildCompanyTest {
  private WebDriver driver;
  private Presence presence;
  private Selenium selenium;
  private FindsByTagName findbyname;
  private String baseUrl;
  private boolean acceptNextAlert = true;
  private StringBuffer verificationErrors = new StringBuffer();

  @Before
  public void setUp() throws Exception {
	  driver = Instance.getInstance();
	  //Instance.initialize(driver);
  }

  @Test
  public void test13() throws Exception {
	  
	  Thread.sleep(2000);

	driver.findElement(By.id("impersonate")).click();
    new Select(driver.findElement(By.id("impersonation-select"))).selectByVisibleText("WDT Child");
    driver.findElement(By.id("impersonation-button")).click();
   
  
   Assert.assertEquals(driver.getPageSource().contains("WDT Child"), true);
  }
  
  @After
  public void tearDown() throws Exception {
    //driver.quit();
    String verificationErrorString = verificationErrors.toString();
    if (!"".equals(verificationErrorString)) {
      fail(verificationErrorString);
    }
  }

  private boolean isElementPresent(By by) {
    try {
      driver.findElement(by);
      return true;
    } catch (NoSuchElementException e) {
      return false;
    }
  }

  private boolean isAlertPresent() {
    try {
      driver.switchTo().alert();
      return true;
    } catch (NoAlertPresentException e) {
      return false;
    }
  }

  private String closeAlertAndGetItsText() {
    try {
      Alert alert = driver.switchTo().alert();
      String alertText = alert.getText();
      if (acceptNextAlert) {
        alert.accept();
      } else {
        alert.dismiss();
      }
      return alertText;
    } finally {
      acceptNextAlert = true;
    }
  }
}