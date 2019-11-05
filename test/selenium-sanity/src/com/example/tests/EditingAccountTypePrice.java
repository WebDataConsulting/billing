package com.example.tests;

import java.util.List;
import java.util.regex.Pattern;
import java.util.concurrent.TimeUnit;
import org.junit.*;
import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;
import org.openqa.selenium.*;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.support.ui.Select;

public class EditingAccountTypePrice {
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
  public void test35() throws Exception {
	    
	    driver.get(baseUrl + "/accountType/list");
	    
	    driver.findElement(By.xpath("//strong[text()='Direct Customer']")).click();
	    
	    driver.findElement(By.cssSelector("a.submit.edit > span")).click();
	    driver.findElement(By.id("creditLimitAsDecimal")).clear();
	    driver.findElement(By.id("creditLimitAsDecimal")).sendKeys("300.00");
	    driver.findElement(By.cssSelector("a.submit.save > span")).click();
	    driver.findElement(By.xpath("//strong[text()='Direct Customer']")).click();
	    driver.findElement(By.cssSelector("a.submit.edit > span")).click();
	    driver.findElement(By.id("creditLimitAsDecimal")).clear();
	    driver.findElement(By.id("creditLimitAsDecimal")).sendKeys("200.00");
	    driver.findElement(By.cssSelector("a.submit.save > span")).click();
	    driver.findElement(By.xpath("//strong[text()='Direct Customer']")).click();
    
    Assert.assertEquals(driver.getPageSource().contains("US$200.00"), true);
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