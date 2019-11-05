package com.example.tests;

import java.util.regex.Pattern;
import java.util.concurrent.TimeUnit;
import org.junit.*;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;
import org.openqa.selenium.*;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.support.ui.Select;

public class ImpersonatingChildCompany_Asset {
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
  public void test51() throws Exception {
	
	    driver.get(baseUrl + "/product/index");
	    driver.findElement(By.id("impersonate")).click();
	    driver.findElement(By.id("impersonation-button")).click();
	    
	    Assert.assertEquals(driver.getPageSource().contains("Working as admin"), true);
	    
	    driver.findElement(By.linkText("WDT Child")).click();
	    
	    Thread.sleep(1000);
	    Assert.assertEquals(driver.getPageSource().contains("Hello admin"), true);
	    
	    driver.findElement(By.linkText("Products")).click();
	    driver.findElement(By.xpath("//strong[text()='Asset category1']")).click();
    
	    Assert.assertEquals(driver.getPageSource().contains("SIM-card1"), true);
	    Thread.sleep(1000);
	    
	    driver.findElement(By.xpath("//strong[text()='SIM-card1']")).click();
	    driver.findElement(By.xpath("//span[text()='Show Assets']")).click();
	    
	    Assert.assertEquals(driver.getPageSource().contains("SIM-102"), true);

	    
	    
	    
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