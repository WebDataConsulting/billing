package com.example.tests;

import static org.junit.Assert.fail;

import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.NoAlertPresentException;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.support.ui.Select;

//before running it go to http://localhost:8080/jbilling/orderPeriod/list and delete semi-monthly,weekly and daily order lists

public class ConfigureOrderPeriods {
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
  public void test23() throws Exception {
	  
	    driver.get(baseUrl + "/orderPeriod/list");
	    driver.findElement(By.cssSelector("a.submit.add > span")).click();
	    driver.findElement(By.id("description")).clear();
	    driver.findElement(By.id("description")).sendKeys("Semi monthly");
	    new Select(driver.findElement(By.id("periodUnitId"))).selectByVisibleText("Semi-Monthly");
	    driver.findElement(By.id("value")).clear();
	    driver.findElement(By.id("value")).sendKeys("1");
	    driver.findElement(By.cssSelector("a.submit.save > span")).click();
	    
	    Thread.sleep(2000);
	    
	    driver.findElement(By.cssSelector("a.submit.add")).click();
	    driver.findElement(By.id("description")).clear();
	    driver.findElement(By.id("description")).sendKeys("Weekly");
	    new Select(driver.findElement(By.id("periodUnitId"))).selectByVisibleText("Week");
	    driver.findElement(By.id("value")).clear();
	    driver.findElement(By.id("value")).sendKeys("1");
	    driver.findElement(By.cssSelector("a.submit.save > span")).click();
	    
	    Thread.sleep(2000);
	    
	    driver.findElement(By.cssSelector("a.submit.add")).click();
	    driver.findElement(By.id("description")).clear();
	    driver.findElement(By.id("description")).sendKeys("Daily");
	    new Select(driver.findElement(By.id("periodUnitId"))).selectByVisibleText("Day");
	    driver.findElement(By.id("value")).clear();
	    driver.findElement(By.id("value")).sendKeys("1");
	    driver.findElement(By.cssSelector("a.submit.save > span")).click();
	    
	    Assert.assertEquals(driver.getPageSource().contains("Semi-Monthly"), true);
	    Assert.assertEquals(driver.getPageSource().contains("Week"), true);
	    Assert.assertEquals(driver.getPageSource().contains("Day"), true);
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