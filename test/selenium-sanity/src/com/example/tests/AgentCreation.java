package com.example.tests;

import java.util.regex.Pattern;
import java.util.concurrent.TimeUnit;
import org.junit.*;
import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;
import org.openqa.selenium.*;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.support.ui.Select;

public class AgentCreation {
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
	 
	    driver.get(baseUrl + "/order/list");
	    driver.findElement(By.xpath("//ul[@id='navList']/li[2]/a/span")).click();
	    driver.findElement(By.linkText("ADD NEW")).click();
	    driver.findElement(By.id("user.userName")).clear();
	    driver.findElement(By.id("user.userName")).sendKeys("Agent A");
	    driver.findElement(By.id("contact-null.email")).clear();
	    driver.findElement(By.id("contact-null.email")).sendKeys("test@gmail.com");
	    new Select(driver.findElement(By.id("type"))).selectByVisibleText("Master");
	    new Select(driver.findElement(By.id("commissionType"))).selectByVisibleText("Invoice");
	    driver.findElement(By.linkText("SAVE CHANGES")).click();
	    
	    Thread.sleep(1000);
	    
	    String str = new String();
	    str = driver.findElement(By.cssSelector("td > a.cell >span")).getText();
	    System.out.println("Id of Agent : "+str);
	    
	    //creating customer B
	    driver.findElement(By.linkText("Customers")).click();
	    driver.findElement(By.cssSelector("div.btn-box > a.submit.add > span")).click();
	    new Select(driver.findElement(By.id("accountTypeId"))).selectByVisibleText("Direct Customer");
	    driver.findElement(By.linkText("SELECT")).click();
	    driver.findElement(By.id("user.userName")).clear();
	    driver.findElement(By.id("user.userName")).sendKeys("Customer B");
	    driver.findElement(By.linkText("SAVE CHANGES")).click();
	    driver.findElement(By.cssSelector("a.submit.edit > span")).click();
	    driver.findElement(By.id("user.partnerId")).clear();
	    driver.findElement(By.id("user.partnerId")).sendKeys(str);
	    driver.findElement(By.linkText("SAVE CHANGES")).click();
	    
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