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

public class CreatingAssetCategory1 {
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
  public void test32() throws Exception {
    
	    //deleting previously created product 
	    /*driver.findElement(By.cssSelector("a.cell > span")).click();
	    driver.findElement(By.linkText("DELETE CATEGORY")).click();
	    driver.findElement(By.xpath("(//button[@type='button'])[8]")).click();*/
    
    
	    //creating new product category
	  	driver.findElement(By.linkText("Products")).click();
	    driver.findElement(By.linkText("ADD CATEGORY")).click();
	    driver.findElement(By.id("description")).clear();
	    driver.findElement(By.id("description")).sendKeys("Asset category1");
	    
	    driver.findElement(By.id("global-checkbox")).click();
	    driver.findElement(By.id("allowAssetManagement")).click();
	    driver.findElement(By.id("lastStatusName")).clear();
	    driver.findElement(By.id("lastStatusName")).sendKeys("Available");
	    
	    Thread.sleep(1000);
	    
	    driver.findElement(By.id("lastStatusAvailable")).click();
	    driver.findElement(By.id("lastStatusDefault")).click();
	    driver.findElement(By.cssSelector("img[alt=\"add\"]")).click();
	    driver.findElement(By.id("lastStatusName")).clear();
	    driver.findElement(By.id("lastStatusName")).sendKeys("In use");
	    driver.findElement(By.id("lastStatusOrderSaved")).click();
	    driver.findElement(By.cssSelector("span.type-metafield-menu > a > img[alt=\"add\"]")).click();
	    
	    Thread.sleep(1000);
	    driver.findElement(By.id("metaField2.name")).clear();
	    driver.findElement(By.id("metaField2.name")).sendKeys("Tax ID");
	    driver.findElement(By.cssSelector("a.submit.save > span")).click();
	    
	    String str = driver.findElement(By.cssSelector("div.msg-box.successfully >p")).getText();
	    System.out.println(str+"*****");
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