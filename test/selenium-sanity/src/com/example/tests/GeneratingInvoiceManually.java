package com.example.tests;

import java.util.regex.Pattern;
import java.util.concurrent.TimeUnit;
import org.junit.*;
import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;
import org.openqa.selenium.*;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.support.ui.Select;

public class GeneratingInvoiceManually {
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
  public void test101() throws Exception {
	    
		//creating new invoice     
	    driver.get(baseUrl + "/order/index");
	    driver.findElement(By.cssSelector("a.cell.double > strong")).click();
	    
	    driver.findElement(By.linkText("GENERATE INVOICE")).click();
	    
	    driver.findElement(By.cssSelector("a.cell.double > strong")).click();
	    driver.findElement(By.linkText("DOWNLOAD PDF")).click();
	    
	    String str = driver.findElement(By.cssSelector("div.msg-box.error > p")).getText();
	    System.out.println(str);
	    Assert.assertEquals(driver.getPageSource().contains("str"), true);
	    
	    driver.findElement(By.linkText("PAY INVOICE")).click();
	    
	    //Asserting test case
	    driver.findElement(By.linkText("Payments")).click();
	    Assert.assertEquals(driver.getPageSource().contains("New Payment"), true);
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