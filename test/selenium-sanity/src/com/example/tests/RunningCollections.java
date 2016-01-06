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
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.support.ui.Select;

public class RunningCollections {
  private WebDriver driver;
  private String baseUrl;
  private boolean acceptNextAlert = true;
  private StringBuffer verificationErrors = new StringBuffer();

  @Before
  public void setUp() throws Exception {
	  baseUrl = "http://localhost:8080/jbilling";
	  driver = Instance.getInstance();
	  //Instance.initialize(driver);
  }

  @Test
  public void testS131() throws Exception {
	  
	    driver.get(baseUrl + "/invoice/index");
	    driver.findElement(By.linkText("Configuration")).click();
	    driver.findElement(By.linkText("Collections")).click();
	    driver.findElement(By.id("collectionsRunDate")).clear();
	    driver.findElement(By.id("collectionsRunDate")).sendKeys("03/01/2001");
	    driver.findElement(By.cssSelector("#collectionsRun > div.btn-row")).click();
	    driver.findElement(By.id("run")).click();
	    driver.findElement(By.xpath("(//button[@type='button'])[3]")).click();
	    driver.findElement(By.linkText("Customers")).click();
	    driver.findElement(By.linkText("Billing Customer 1")).click();
	    driver.findElement(By.linkText("Billing Customer 2")).click();
	    
	    Thread.sleep(1000);
	    

	    
	    
	    driver.findElement(By.linkText("Configuration")).click();
	    driver.findElement(By.linkText("Collections")).click();
	    driver.findElement(By.id("collectionsRunDate")).clear();
	    driver.findElement(By.id("collectionsRunDate")).sendKeys("03/20/2001");
	    driver.findElement(By.cssSelector("#collectionsRun > fieldset > div.form-columns.single")).click();
	    driver.findElement(By.id("run")).click();
	    driver.findElement(By.xpath("(//button[@type='button'])[3]")).click();
	    driver.findElement(By.linkText("Customers")).click();
	    driver.findElement(By.linkText("Billing Customer 1")).click();
	    driver.findElement(By.linkText("Billing Customer 2")).click();
	    
	    
	    
	    driver.findElement(By.linkText("Configuration")).click();
	    driver.findElement(By.linkText("Collections")).click();
	    driver.findElement(By.id("collectionsRunDate")).sendKeys("03/25/2001");
	    driver.findElement(By.cssSelector("#collectionsRun > div.btn-row")).click();
	    driver.findElement(By.id("run")).click();
	    driver.findElement(By.xpath("(//button[@type='button'])[3]")).click();
	    driver.findElement(By.linkText("Customers")).click();
	    driver.findElement(By.linkText("Billing Customer 1")).click();
	    driver.findElement(By.linkText("Billing Customer 2")).click();
	    
	    
	    
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