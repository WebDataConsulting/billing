package com.example.tests;

import java.util.regex.Pattern;
import java.util.concurrent.TimeUnit;
import org.junit.*;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;
import org.openqa.selenium.*;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.support.ui.Select;

public class ConfiguringCollection {

  private static final WebDriver driver= Instance.getInstance();
  private String Instance.BASE_URL;
  private boolean acceptNextAlert = true;
  private StringBuffer verificationErrors = new StringBuffer();

  @Test
  public void test23() throws Exception {	    
	    
	    driver.get(Instance.BASE_URL + "/Configuration");
	    driver.findElement(By.linkText("Configuration")).click();
	    driver.findElement(By.linkText("Collections")).click();

	    //driver.manage().timeouts().implicitlyWait(30, TimeUnit.SECONDS);

			    

	    driver.findElement(By.id("obj[0].statusStr")).clear();
	    driver.findElement(By.id("obj[0].statusStr")).sendKeys("payment due");
		Thread.sleep(1000);
	    driver.findElement(By.id("obj[0].days")).clear();
	    driver.findElement(By.id("obj[0].days")).sendKeys("0");
		Thread.sleep(1000);
	    driver.findElement(By.id("obj[0].paymentRetry")).click();
	    
	    Thread.sleep(1000);
	    
	    driver.findElement(By.id("obj[1].statusStr")).clear();
	    driver.findElement(By.id("obj[1].statusStr")).sendKeys("Grace period");
	    driver.findElement(By.id("obj[1].days")).clear();
	    driver.findElement(By.id("obj[1].days")).sendKeys("2");
	    driver.findElement(By.id("obj[1].sendNotification")).click();
	    
	    Thread.sleep(1000);
	    
	    driver.findElement(By.id("obj[2].statusStr")).clear();
	    driver.findElement(By.id("obj[2].statusStr")).sendKeys("First Retry");
	    driver.findElement(By.id("obj[2].days")).clear();
	    driver.findElement(By.id("obj[2].days")).sendKeys("3");
	    driver.findElement(By.id("obj[2].paymentRetry")).click();
	    
	    Thread.sleep(1000);
	    
	    driver.findElement(By.id("obj[3].statusStr")).clear();
	    driver.findElement(By.id("obj[3].statusStr")).sendKeys("Suspended");
	    driver.findElement(By.id("obj[3].days")).clear();
	    driver.findElement(By.id("obj[3].days")).sendKeys("7");
	    driver.findElement(By.id("obj[3].suspended")).click();
	    
	    Thread.sleep(1000);
	    
	    driver.findElement(By.cssSelector("a.submit.save > span")).click();
	    
	    String str = driver.findElement(By.cssSelector("div.msg-box.successfully >p")).getText();
	    System.out.println(str+"****");
	    Assert.assertEquals(driver.getPageSource().contains(str), true);
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
