package com.example.tests;

import java.util.List;
import java.util.regex.Pattern;
import java.util.concurrent.TimeUnit;
import org.junit.*;
import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;
import org.openqa.selenium.*;
import org.openqa.selenium.By.ByClassName;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;

public class CreatingPaymentMethod {
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
  public void test21() throws Exception {
    
    driver.findElement(By.linkText("Configuration")).click();
    driver.findElement(By.linkText("Payment Method")).click();
    driver.findElement(By.linkText("ADD NEW")).click();
    driver.findElement(By.linkText("SELECT")).click();
    
    Thread.sleep(2000);
    
    driver.findElement(By.id("methodName")).sendKeys("Debit Card");
    driver.findElement(By.id("isRecurring")).click();
   
    new Select(driver.findElement(By.id("accountTypes"))).selectByVisibleText("Distributor Account");
    driver.findElement(By.xpath("//span[text()='Save Changes']")).click();
    
    
    
    
    String str = driver.findElement(By.cssSelector("div.msg-box.successfully > p")).getText();
    System.out.println(str+"***");
    Assert.assertEquals(driver.getPageSource().contains(str), true);
    
    
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