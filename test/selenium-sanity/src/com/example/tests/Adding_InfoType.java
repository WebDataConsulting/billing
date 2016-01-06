package com.example.tests;

import static org.junit.Assert.fail;


import java.util.List;
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
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.support.ui.Select;

public class Adding_InfoType {
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
  public void test22() throws Exception {
    
	    driver.findElement(By.linkText("Configuration")).click();
	    driver.findElement(By.linkText("Account Type")).click();
	    
	    Thread.sleep(2000);
	    
	    driver.findElement(By.xpath("//strong[text()='Direct Customer']")).click();
	    
	    driver.findElement(By.linkText("ADD INFORMATION TYPE")).click();
	    driver.findElement(By.id("name")).clear();
	    driver.findElement(By.id("name")).sendKeys("E-mail");
	    driver.findElement(By.linkText("ADD NEW METAFIELD")).click();
	    
	    
	    
	    driver.findElement(By.cssSelector("#mf-0 > span")).click();
	    
	    driver.findElement(By.id("mf-0")).click();
	    Thread.sleep(2000);
	    
	    driver.findElement(By.id("metaField0.name")).click();
	    driver.findElement(By.id("metaField0.name")).clear();
	    driver.findElement(By.id("metaField0.name")).sendKeys("E-mail");
	    new Select(driver.findElement(By.id("fieldType0"))).selectByVisibleText("EMAIL");
	    driver.findElement(By.linkText("UPDATE")).submit();
	    driver.findElement(By.cssSelector("#review-box > div.btn-box.ait-btn-box > a.submit.save > span")).click();
	    
	    

	    
	   Assert.assertEquals(driver.getPageSource().contains("E-mail"), true);
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