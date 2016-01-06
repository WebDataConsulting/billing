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

public class CollectionsPlugin {
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
  public void test23() throws Exception {
	    
	    driver.get(baseUrl + "/config/saveAging");
	    driver.findElement(By.linkText("Configuration")).click();
	    driver.findElement(By.linkText("Plug-ins")).click();
	    driver.findElement(By.xpath("//a[@id='17']/em")).click();
	    driver.findElement(By.cssSelector("a.submit.add >span")).click();
	    new Select(driver.findElement(By.id("typeId"))).selectByVisibleText("com.sapienter.jbilling.server.user.tasks.UserAgeingNotificationTask");
	    driver.findElement(By.cssSelector("option[value=\"109\"]")).click();
	    
	    Thread.sleep(1000);
	    driver.findElement(By.id("processingOrder")).clear();
	    driver.findElement(By.id("processingOrder")).sendKeys("9");//we will have to delete after each run
	    driver.findElement(By.id("plgDynamic.1.name")).clear();
	    driver.findElement(By.id("plgDynamic.1.name")).sendKeys("3");
	    driver.findElement(By.id("plgDynamic.1.value")).clear();
	    driver.findElement(By.id("plgDynamic.1.value")).sendKeys("1001");
	    driver.findElement(By.cssSelector("img[alt=\"add\"]")).click();
	    driver.findElement(By.cssSelector("a.submit.save > span")).click();
    
    
	   String str = driver.findElement(By.cssSelector("div.msg-box.successfully >p")).getText();
	   System.out.println(str +"****");
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