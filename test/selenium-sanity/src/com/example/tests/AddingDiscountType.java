package com.example.tests;

import java.io.File;
import java.util.regex.Pattern;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.junit.*;
import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;
import org.openqa.selenium.*;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.support.ui.Select;

public class AddingDiscountType {
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
  public void test63() throws Exception {
	  
	    
	    driver.get(baseUrl + "/discount/saveDiscount");
	    driver.findElement(By.cssSelector("em")).click();
	    driver.findElement(By.xpath("//ul[@id='navList']/li[8]/a/span")).click();
	    driver.findElement(By.cssSelector("a.submit.add > span")).click();
	    driver.findElement(By.id("discount.code")).clear();
	    driver.findElement(By.id("discount.code")).sendKeys("Test Discount2");
	    driver.findElement(By.cssSelector("img[alt=\"remove\"]")).click();
	    driver.findElement(By.id("discount.descriptions[0].content")).clear();
	    driver.findElement(By.id("discount.descriptions[0].content")).sendKeys("Test Discount1");
	    new Select(driver.findElement(By.id("discount.type"))).selectByVisibleText("ONE_TIME_AMOUNT");
	    driver.findElement(By.cssSelector("option[value=\"ONE_TIME_AMOUNT\"]")).click();
	    driver.findElement(By.id("discount.rate")).clear();
	    driver.findElement(By.id("discount.rate")).sendKeys("5");
	    driver.findElement(By.cssSelector("a.submit.save > span")).click();
	    
	    Thread.sleep(1000);
	    
	    
	    
	    driver.findElement(By.linkText("Customers")).click();
	    driver.findElement(By.linkText("Ashish Kumar")).click();
	    driver.findElement(By.cssSelector("a.submit.order > span")).click();
	    
	    driver.findElement(By.id("ui-id-8")).click();
	    driver.findElement(By.xpath("//strong[text()='Product code1 Description']")).click();
	    Thread.sleep(100);
	    
	    
	    driver.findElement(By.id("ui-id-9")).click();
	    new Select(driver.findElement(By.id("discountableItem.0.lineLevelDetails"))).selectByVisibleText("-- Order Level Discount --");
	    new Select(driver.findElement(By.id("discount.0.id"))).selectByVisibleText("Test Discount2 - Test Discount1");
	    Thread.sleep(1000);
	    
	    driver.findElement(By.cssSelector("#change--3-update-form > div.btn-box >a.submit.save > span")).click();
	    driver.findElement(By.id("ui-id-4")).click();
	    driver.findElement(By.cssSelector("a.submit.save > span")).click();
	    
	    String str = driver.findElement(By.cssSelector("div.msg-box.successfully >p")).getText();
	    System.out.println(str+"***");
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