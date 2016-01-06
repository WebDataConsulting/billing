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
import org.testng.Assert;

public class MakingPaymentOnGeneratedInvoices {
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
  public void testS121() throws Exception {
	  
	    //Making payments on generated invoice
	    driver.get(baseUrl + "/invoice/index");
	    driver.findElement(By.linkText("Ashish Kumar")).click();
	    driver.findElement(By.linkText("PAY INVOICE")).click();
	    
	    
	    //new Select(driver.findElement(By.id("paymentMethod_0.paymentMethodTypeId"))).selectByVisibleText("Credit Card");
	    
	    
	    /*driver.findElement(By.id("0_metaField_124.value")).clear();
	    driver.findElement(By.id("0_metaField_124.value")).sendKeys("Ashish");
	    driver.findElement(By.id("0_metaField_122.value")).clear();
	    driver.findElement(By.id("0_metaField_122.value")).sendKeys("4111111111111152");
	    driver.findElement(By.id("0_metaField_121.value")).clear();
	    driver.findElement(By.id("0_metaField_121.value")).sendKeys("02/2020");
	    driver.findElement(By.id("paymentMethod_0.processingOrder")).clear();
	    driver.findElement(By.id("paymentMethod_0.processingOrder")).sendKeys("1");*/
	    
	    driver.findElement(By.linkText("REVIEW PAYMENT")).click();
	    driver.findElement(By.linkText("MAKE PAYMENT")).click();
	    
	    //Applying assertion
	    String str = driver.findElement(By.cssSelector("div.msg-box.info > p")).getText();
	    System.out.println(str);
	    Assert.assertEquals(driver.getPageSource().contains("str"), true);
	    
	    
	    
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