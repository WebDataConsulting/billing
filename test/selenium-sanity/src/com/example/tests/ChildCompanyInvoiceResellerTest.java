package com.example.tests;


import com.google.common.base.Verify;
import com.thoughtworks.selenium.Selenium;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.internal.FindsByTagName;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.NoAlertPresentException;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.testng.Assert;

import com.thoughtworks.selenium.webdriven.WebDriverBackedSelenium;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import static org.apache.commons.lang3.StringUtils.join;

public class ChildCompanyInvoiceResellerTest {
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
	  public void test14() throws Exception {
	    
	    driver.get(baseUrl + "/signup");
	    driver.findElement(By.id("user.userName")).clear();
	    driver.findElement(By.id("user.userName")).sendKeys("Test1.3");
	    
	    driver.findElement(By.id("contact.firstName")).clear();
	    driver.findElement(By.id("contact.firstName")).sendKeys("Child1");
	    
	    driver.findElement(By.id("contact.lastName")).clear();
	    driver.findElement(By.id("contact.lastName")).sendKeys("entity");
	    
	    driver.findElement(By.id("contact.phoneCountryCode1")).clear();
	    driver.findElement(By.id("contact.phoneCountryCode1")).sendKeys("112");
	    driver.findElement(By.id("contact.phoneAreaCode")).clear();
	    driver.findElement(By.id("contact.phoneAreaCode")).sendKeys("081");
	    driver.findElement(By.id("contact.phoneNumber")).clear();
	    driver.findElement(By.id("contact.phoneNumber")).sendKeys("3333");
	    
	    driver.findElement(By.id("contact.email")).clear();
	    driver.findElement(By.id("contact.email")).sendKeys("abc@gmail.com");
	    
	    driver.findElement(By.id("contact.organizationName")).clear();
	    driver.findElement(By.id("contact.organizationName")).sendKeys("WDT Child2");
	    
	    driver.findElement(By.id("contact.invoiceAsReseller")).click();
	    
	    driver.findElement(By.id("contact.address1")).clear();
	    driver.findElement(By.id("contact.address1")).sendKeys("Elements Mall");
	    
	    driver.findElement(By.id("contact.stateProvince")).clear();
	    driver.findElement(By.id("contact.stateProvince")).sendKeys("Rajasthan");
	    
	    new Select(driver.findElement(By.id("contact.countryCode"))).selectByVisibleText("India");
	    
	    driver.findElement(By.id("contact.postalCode")).clear();
	    driver.findElement(By.id("contact.postalCode")).sendKeys("11123");
	    driver.findElement(By.linkText("SAVE CHANGES")).click();
	    
		Assert.assertEquals(driver.getPageSource().contains("Child1 entity"), true);
		
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