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

import com.thoughtworks.selenium.webdriven.commands.Click;

public class DependentProductOrder {
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
  public void testS152() throws Exception {
		    
		    //creating order
		    
		    driver.findElement(By.cssSelector("span")).click();
		    driver.findElement(By.linkText("Test Customer1")).click();
		    driver.findElement(By.linkText("CREATE ORDER")).click();
		    
		    new Select(driver.findElement(By.id("orderPeriod"))).selectByVisibleText("Monthly");
		    
		    driver.findElement(By.id("billingTypeId")).click();
		    new Select(driver.findElement(By.id("billingTypeId"))).selectByVisibleText("pre paid");
		    
		    driver.findElement(By.id("ui-id-8")).click();
		    
		    driver.findElement(By.xpath("//strong[text()='Date Service']")).click();
		    
		    driver.findElement(By.linkText("REVIEW")).click();
		    driver.findElement(By.linkText("LINE CHANGES")).click();
		    
		    
		    driver.findElement(By.linkText("DEPENDENCY")).click();
		    Thread.sleep(1000);
		    
		    driver.findElement(By.xpath("//table[@id='dependencies-products-change_-3']/tbody/tr[2]/td")).click();
		    driver.findElement(By.xpath("(//button[@type='button'])[17]")).click();
		    
		    
		    driver.findElement(By.xpath("//a[1]/span[contains(text(),'Update')]")).click();
		    Thread.sleep(1000);
		    driver.findElement(By.linkText("SAVE CHANGES")).click();
		    
		    String str1 = driver.findElement(By.cssSelector("div.msg-box.successfully >p")).getText();
		    System.out.println(" created msg is : "+str1);
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