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

public class GeneratingAndPayingInvoice {
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
  public void test102() throws Exception {
	    
	    //generating and paying invoices
	    driver.get(baseUrl + "/customer/index");
	    driver.findElement(By.linkText("Customers")).click();
	    driver.findElement(By.linkText("Sarah Wilson")).click();
	    driver.findElement(By.cssSelector("a.submit.order > span")).click();
	    new Select(driver.findElement(By.id("orderPeriod"))).selectByVisibleText("Monthly");
	    driver.findElement(By.id("billingTypeId")).click();
	    new Select(driver.findElement(By.id("billingTypeId"))).selectByVisibleText("pre paid");
	    driver.findElement(By.id("ui-id-8")).click();
	    driver.findElement(By.cssSelector("a.cell.double >strong")).click();
	    
	    driver.findElement(By.id("change--3.useItem")).click();
	    driver.findElement(By.id("change--3.description")).click();
	    driver.findElement(By.id("change--3.description")).clear();
	    driver.findElement(By.id("change--3.description")).sendKeys("Product  Description");
	    driver.findElement(By.cssSelector("#change--3-update-form > div.btn-box > a.submit.save > span")).click();
	    driver.findElement(By.linkText("SAVE CHANGES")).click(); 
	    
	    Thread.sleep(1000);
	    
	    
	    driver.findElement(By.linkText("Orders")).click();
	    driver.findElement(By.cssSelector("a.cell.double >strong")).click(); 
	    driver.findElement(By.linkText("GENERATE INVOICE")).click();
	    
	    driver.findElement(By.linkText("Invoices")).click();
	    driver.findElement(By.cssSelector("a.cell.double >strong")).click(); 
	    driver.findElement(By.linkText("PAY INVOICE")).click();
	    driver.findElement(By.id("processNow")).click();
	    driver.findElement(By.linkText("REVIEW PAYMENT")).click();
	    driver.findElement(By.linkText("MAKE PAYMENT")).click();
	    
	    String str = new String();
	    str = driver.findElement(By.cssSelector("div.msg-box.successfully >p")).getText();
	    System.out.println(str +"******");
	   
	    
	   //driver.findElement(By.linkText("Invoices")).click();
	   driver.findElement(By.cssSelector("a.cell.double >strong")).click();
	   Assert.assertEquals(driver.getPageSource().contains("Paid"), true);
	   
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