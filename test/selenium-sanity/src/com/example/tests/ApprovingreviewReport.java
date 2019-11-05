package com.example.tests;

import java.util.regex.Pattern;
import java.util.concurrent.TimeUnit;
import org.junit.*;
import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;
import org.openqa.selenium.*;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.support.ui.Select;

public class ApprovingreviewReport {
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
  public void testS112() throws Exception {
	  
	  
	    driver.get(baseUrl + "/billingconfiguration/index");
	    driver.findElement(By.cssSelector("div.row > div.row")).click();
	    driver.findElement(By.linkText("Configuration")).click();
	    driver.findElement(By.linkText("Billing Process")).click();
	    driver.findElement(By.id("nextRunDate")).clear();
	    driver.findElement(By.id("nextRunDate")).sendKeys("01/01/2001");
	    driver.findElement(By.linkText("SAVE CHANGES")).click();
	    driver.findElement(By.linkText("RUN BILLING")).click();
	    
	    Thread.sleep(2000);
	    
	    
	    driver.findElement(By.linkText("Billing")).click();
	    driver.findElement(By.cssSelector("td.medium > a.cell")).click();
	    driver.findElement(By.linkText("SHOW ORDERS")).click();
	    
	    
	    driver.findElement(By.linkText("Billing")).click();
	    driver.findElement(By.cssSelector("td.medium > a.cell")).click();
	    driver.findElement(By.linkText("SHOW ORDERS")).click();
	    
	    driver.findElement(By.linkText("Billing")).click();
	    driver.findElement(By.cssSelector("td.medium > a.cell")).click();
	    driver.findElement(By.linkText("APPROVE")).click();
	    driver.findElement(By.xpath("(//button[@type='button'])[3]")).click();
	    
	    String str = new String();
	    str = driver.findElement(By.cssSelector("div.msg-box.error.wide > strong")).getText();
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