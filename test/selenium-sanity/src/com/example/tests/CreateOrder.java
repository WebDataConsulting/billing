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

public class CreateOrder {
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
  public void test71() throws Exception {
	   
	    
	    //driver.get(baseUrl + "/orderBuilder/edit?execution=e2s1&userId=96&max=10&offset=0");
		driver.findElement(By.cssSelector("span")).click();
	    driver.findElement(By.linkText("Ashish Kumar")).click();
	    driver.findElement(By.cssSelector("a.submit.order > span")).click();
	    new Select(driver.findElement(By.id("orderPeriod"))).selectByVisibleText("Monthly");
	    new Select(driver.findElement(By.id("billingTypeId"))).selectByVisibleText("pre paid");
	    
	    driver.findElement(By.id("activeSince")).clear();
	    driver.findElement(By.id("activeSince")).sendKeys("01/01/2002");
	    
	    driver.findElement(By.id("ui-id-8")).click();
	    driver.findElement(By.xpath("//strong[text()='Product code1 Description']")).click();
	    
	    driver.findElement(By.id("change--3.quantityAsDecimal")).clear();
	    driver.findElement(By.id("change--3.quantityAsDecimal")).sendKeys("2");
	    
	    driver.findElement(By.cssSelector("#change--3-update-form > div.btn-box > a.submit.save > span")).click();
	    driver.findElement(By.linkText("SAVE CHANGES")).click();
	    
	    Thread.sleep(1000);
	    
	    //Editing the order
	    driver.findElement(By.linkText("Orders")).click();
	    driver.findElement(By.cssSelector("a.cell.double >strong")).click();
	    
	    driver.findElement(By.linkText("EDIT THIS ORDER")).click();
	    driver.findElement(By.id("ui-id-10")).click();
	    
	    driver.findElement(By.cssSelector("span.description")).click();
	    driver.findElement(By.linkText("CHANGE")).click();
	    
	    driver.findElement(By.id("change--2.quantityAsDecimal")).clear();
	    driver.findElement(By.id("change--2.quantityAsDecimal")).sendKeys("2");
	    
	    
	    driver.findElement(By.cssSelector("#change--2-update-form > div.btn-box > a.submit.save > span")).click();
	    driver.findElement(By.linkText("SAVE CHANGES")).click();
    
    
	   String str = new String();
	   str = driver.findElement(By.cssSelector("div.msg-box.successfully > p")).getText();
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