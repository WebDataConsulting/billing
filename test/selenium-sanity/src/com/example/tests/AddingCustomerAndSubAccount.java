package com.example.tests;

import static org.junit.Assert.fail;

import java.io.File;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.NoAlertPresentException;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.support.ui.Select;
import org.testng.Assert;

public class AddingCustomerAndSubAccount {
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
  public void test62() throws Exception {
	    
	    driver.get(baseUrl + "/customer/edit");
	    driver.findElement(By.cssSelector("span")).click();
	    driver.findElement(By.cssSelector("div.btn-box > a.submit.add > span")).click();
	    new Select(driver.findElement(By.id("accountTypeId"))).selectByVisibleText("Direct Customer");
	    driver.findElement(By.cssSelector("a.submit.save > span")).click();
	    
	    Thread.sleep(1000);
	    
	    driver.findElement(By.id("user.userName")).clear();
	    driver.findElement(By.id("user.userName")).sendKeys("Brian Smith");
	    driver.findElement(By.id("user.isParent")).click();
	    driver.findElement(By.xpath("//label[text()='E-mail']")).sendKeys("bsmith@abc.com");
	    new Select(driver.findElement(By.id("paymentMethod_0.paymentMethodTypeId"))).selectByVisibleText("Credit card");
	    driver.findElement(By.id("paymentMethod_0.processingOrder")).clear();
	    driver.findElement(By.id("paymentMethod_0.processingOrder")).sendKeys("1");
	    driver.findElement(By.id("0_metaField_33.value")).clear();
	    driver.findElement(By.id("0_metaField_33.value")).sendKeys("Ashish");
	    
	    driver.findElement(By.id("0_metaField_29.value")).clear();
	    driver.findElement(By.id("0_metaField_29.value")).sendKeys("4111111111111152");
	    
	    driver.findElement(By.id("0_metaField_31.value")).clear();
	    driver.findElement(By.id("0_metaField_31.value")).sendKeys("02/2020");
	    driver.findElement(By.id("user-edit-form")).submit();
	    
	    Thread.sleep(1000);
	    
	    //ERROR: Caught exception [unknown command []]
	    driver.findElement(By.linkText("ADD SUB-ACCOUNT")).click();
	    new Select(driver.findElement(By.id("accountTypeId"))).selectByVisibleText("Direct Customer");
	    driver.findElement(By.cssSelector("a.submit.save > span")).click();
	    driver.findElement(By.id("user.userName")).clear();
	    driver.findElement(By.id("user.userName")).sendKeys("Sarah Wilson");
	    driver.findElement(By.xpath("//label[text()='E-mail']")).sendKeys("swilson@abc.com");
	    new Select(driver.findElement(By.id("paymentMethod_0.paymentMethodTypeId"))).selectByVisibleText("Credit card");
	    driver.findElement(By.id("0_metaField_33.value")).clear();
	    driver.findElement(By.id("0_metaField_33.value")).sendKeys("Ashish");
	    
	    driver.findElement(By.id("0_metaField_29.value")).clear();
	    driver.findElement(By.id("0_metaField_29.value")).sendKeys("4111111111111152");
	    
	    driver.findElement(By.id("0_metaField_31.value")).clear();
	    driver.findElement(By.id("0_metaField_31.value")).sendKeys("02/2020");
	    
	    driver.findElement(By.id("paymentMethod_0.processingOrder")).clear();
	    driver.findElement(By.id("paymentMethod_0.processingOrder")).sendKeys("1");
	    driver.findElement(By.cssSelector("li > a.submit.save > span")).click();
	    //driver.findElement(By.cssSelector("a.cell.double>span")).click();
	    
	   
	   /* driver.get("http://localhost:8080/jbilling/customer/list");
	    File scrFile = ((TakesScreenshot)driver).getScreenshotAs(OutputType.FILE);
	    FileUtils.copyFile(scrFile, new File("/home/amit/Desktop/screenshots/screenshot.png"));*/
	    
	    String str = driver.findElement(By.cssSelector("div.msg-box.successfully >p")).getText();
	    System.out.println(str+"***");
	    Assert.assertEquals(driver.getPageSource().contains("str"), true);
	    
	    driver.findElement(By.linkText("Customers")).click();
	    driver.findElement(By.linkText("Brian Smith")).click();
	    
	    Assert.assertEquals(driver.getPageSource().contains("Sarah Wilson"), true);
	   
    
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