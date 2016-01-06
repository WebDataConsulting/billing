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

public class CreatingAndVerifyingCommission {
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
  public void testS164() throws Exception {
	  
	    driver.findElement(By.linkText("Agents")).click();
	    driver.findElement(By.linkText("Agent A")).click();
	    
	    driver.findElement(By.linkText("Customers")).click();
	    driver.findElement(By.cssSelector("a.cell.double > strong")).click();
	    driver.findElement(By.linkText("CREATE ORDER")).click();
	    new Select(driver.findElement(By.id("orderPeriod"))).selectByVisibleText("Monthly");
	    new Select(driver.findElement(By.id("billingTypeId"))).selectByVisibleText("pre paid");
	    driver.findElement(By.id("activeSince")).clear();
	    driver.findElement(By.id("activeSince")).sendKeys("10/08/2015");
	    
	    driver.findElement(By.id("ui-id-8")).click();
	    driver.findElement(By.xpath("//strong[text()='Comm Product']")).click();
	    
	    driver.findElement(By.id("change--3.startDate")).clear();
	    driver.findElement(By.id("change--3.startDate")).sendKeys("10/08/2015");
	    
	    driver.findElement(By.cssSelector("#change--3-update-form > div.btn-box > a.submit.save > span")).click();
	    driver.findElement(By.linkText("SAVE CHANGES")).click();
	    
	    Thread.sleep(1000);
	    
	    
	    driver.findElement(By.linkText("Orders")).click();
	    driver.findElement(By.cssSelector("a.double.cell > strong")).click();
	    driver.findElement(By.linkText("GENERATE INVOICE")).click();
	    
	    driver.findElement(By.linkText("Configuration")).click();
	    driver.findElement(By.linkText("Agent Commission Process")).click();
	    driver.findElement(By.id("nextRunDate")).clear();
	    driver.findElement(By.id("nextRunDate")).sendKeys("10/08/2015");
	    
	    driver.findElement(By.id("periodValue")).clear();
	    driver.findElement(By.id("periodValue")).sendKeys("1");
	    
	    driver.findElement(By.cssSelector("a.submit.save > span")).click();
	    
	    Thread.sleep(1000);
	    
	    String str = driver.findElement(By.cssSelector("div.msg-box.successfully >p")).getText();
	    System.out.println(str);
	    Assert.assertEquals(driver.getPageSource().contains(str), true);
	    
	    driver.findElement(By.linkText("Agents")).click();
	    driver.findElement(By.linkText("Agent A")).click();
	    driver.findElement(By.cssSelector("a.submit.show > span")).click();
	    
	    Assert.assertEquals(driver.getPageSource().contains("US$1"), true);
    
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