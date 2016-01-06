package com.example.tests;

import static org.junit.Assert.fail;

import java.util.List;
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
import org.openqa.selenium.remote.server.handler.FindElement;
import org.openqa.selenium.support.ui.Select;
import org.testng.Assert;

import com.thoughtworks.selenium.webdriven.commands.Click;

public class CreatingReportsAndVerifying {
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
  public void testS141Test() throws Exception {
	  
	    //creating new customer
	    driver.get(baseUrl + "/customer/index");
	    driver.findElement(By.cssSelector("span")).click();
	    driver.findElement(By.linkText("ADD NEW")).click();
	    new Select(driver.findElement(By.id("accountTypeId"))).selectByVisibleText("Direct Customer");
	    driver.findElement(By.linkText("SELECT")).click();
	    
	    driver.findElement(By.id("user.userName")).clear();
	    driver.findElement(By.id("user.userName")).sendKeys("Test Customer1");
	    driver.findElement(By.linkText("SAVE CHANGES")).click();
	    
	    Thread.sleep(2000);
	    
	    //creating order with customer
	    driver.findElement(By.linkText("Customers")).click();
	    driver.findElement(By.cssSelector("td > a.cell.double > strong")).click();
	    driver.findElement(By.linkText("CREATE ORDER")).click();
	    driver.findElement(By.id("ui-id-8")).click();
	    driver.findElement(By.cssSelector("td > a.cell.double >strong")).click();
	    driver.findElement(By.cssSelector("#change--3-update-form > div.btn-box > a.submit.save > span")).click();
	    driver.findElement(By.linkText("SAVE CHANGES")).click();
	    
	    
	    
	    
	    //generating invoice
	    driver.findElement(By.linkText("Orders")).click();
	    driver.findElement(By.cssSelector("a.cell.double >strong")).click();
	    driver.findElement(By.linkText("GENERATE INVOICE")).click();
	    //System.out.println(driver.getCurrentUrl());
	    
	    driver.findElement(By.linkText("Customers")).click();
	    driver.findElement(By.cssSelector("a.cell.double > strong")).click();
	    driver.findElement(By.linkText("MAKE PAYMENT")).click();
	    

	    driver.findElement(By.name("invoiceId")).click();
	    
	    //driver.findElement(By.xpath("//label[contains('invoice',.)]")).click();
	    driver.findElement(By.id("paymentMethod_0.processingOrder")).clear();
	    driver.findElement(By.id("paymentMethod_0.processingOrder")).sendKeys("1");
	    
	    driver.findElement(By.xpath("(//input[@type='text'])[5]")).clear();
	    driver.findElement(By.xpath("(//input[@type='text'])[5]")).sendKeys("Rahul");
	    
	    driver.findElement(By.xpath("(//input[@type='text'])[6]")).clear();
	    driver.findElement(By.xpath("(//input[@type='text'])[6]")).sendKeys("4111111111111152");
	    
	    driver.findElement(By.xpath("(//input[@type='text'])[7]")).clear();
	    driver.findElement(By.xpath("(//input[@type='text'])[7]")).sendKeys("12/2020");
	    
	    driver.findElement(By.linkText("REVIEW PAYMENT")).click();
	    driver.findElement(By.linkText("MAKE PAYMENT")).click();
	    
	    
	    //Assertion is applied here
	    driver.findElement(By.linkText("Payments")).click();
	    Assert.assertEquals(driver.getPageSource().contains("Test Customer1"), true);
	    
	    Thread.sleep(1000);
	    
	    driver.findElement(By.linkText("Reports")).click();
	    driver.findElement(By.xpath("//strong[text()='Invoice Reports']")).click();
	    driver.findElement(By.xpath("//strong[text()='Total Amount Invoiced']")).click();
	    driver.findElement(By.id("start_date")).clear();
	    driver.findElement(By.id("start_date")).sendKeys("10/8/2015");
	    
	    driver.findElement(By.id("end_date")).clear();
	    driver.findElement(By.id("end_date")).sendKeys("10/8/2015");
	  
	    new Select(driver.findElement(By.id("period"))).selectByVisibleText("Day");
	    new Select(driver.findElement(By.id("format"))).selectByVisibleText("View as HTML");
	    driver.findElement(By.linkText("RUN REPORT")).click();
	    
	    new Select(driver.findElement(By.id("format"))).selectByVisibleText("Adobe PDF");
	    driver.findElement(By.linkText("RUN REPORT")).click();
	    
	    new Select(driver.findElement(By.id("format"))).selectByVisibleText("Excel");
	    driver.findElement(By.linkText("RUN REPORT")).click();
	    
	    
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