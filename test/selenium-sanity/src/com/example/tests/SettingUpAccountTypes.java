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
import org.openqa.selenium.support.ui.Select;
import org.testng.Assert;

public class SettingUpAccountTypes {
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
  public void testS21() throws Exception {
	  
	driver.get(baseUrl + "/customer/index");
    driver.findElement(By.xpath("//ul[@id='navList']/li[11]/a/span")).click();
    driver.findElement(By.linkText("Account Type")).click();
    driver.findElement(By.cssSelector("a.submit.add > span")).click();
    driver.findElement(By.id("description")).clear();
    driver.findElement(By.id("description")).sendKeys("Direct Customer");
    driver.findElement(By.id("invoiceDesign")).clear();
    driver.findElement(By.id("invoiceDesign")).sendKeys("invoice_design");
    driver.findElement(By.cssSelector("a.submit.save > span")).click();
    driver.findElement(By.cssSelector("a.submit.add > span")).click();
    driver.findElement(By.id("description")).clear();
    driver.findElement(By.id("description")).sendKeys("Distributor");
    driver.findElement(By.id("invoiceDesign")).clear();
    driver.findElement(By.id("invoiceDesign")).sendKeys("invoice_design");
    driver.findElement(By.linkText("SAVE CHANGES")).click();

    int index = 1;
    WebElement baseTable = driver.findElement(By.className("table-box"));
    List<WebElement> tableRows = baseTable.findElements(By.tagName("tr"));
    String str = tableRows.get(tableRows.size()-1).getAttribute("id");
    final String id = str.substring(str.lastIndexOf('-')+1);
    System.out.println(id);
    
    //Thread.sleep(2000);
    
    driver.findElement(By.id(id)).click();
    driver.findElement(By.cssSelector("a.submit.edit > span")).click();
    driver.findElement(By.id("description")).clear();
    driver.findElement(By.id("description")).sendKeys("Distributor Account");
    driver.findElement(By.cssSelector("a.submit.save > span")).click();
    
    String str1 = driver.findElement(By.cssSelector("div.msg-box.successfully >p")).getText();
    System.out.println(str1+"****");
    Assert.assertEquals(driver.getPageSource().contains(str1), true);
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