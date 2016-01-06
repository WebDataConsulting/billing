package com.example.tests;

import java.util.List;
import java.util.regex.Pattern;
import java.util.concurrent.TimeUnit;

import org.apache.bcel.verifier.VerifyDialog;
import org.junit.*;
import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;
import org.openqa.selenium.*;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.support.ui.Select;

public class AddingCustomer {
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
  public void test61() throws Exception {
	    
	    driver.get(baseUrl + "/customer/edit");
	    driver.findElement(By.cssSelector("span")).click();
	    driver.findElement(By.cssSelector("div.btn-box > a.submit.add > span")).click();
	    new Select(driver.findElement(By.id("accountTypeId"))).selectByVisibleText("Direct Customer");
	    driver.findElement(By.cssSelector("a.submit.save >span")).click();
	    
	    driver.findElement(By.id("user.userName")).clear();
	    driver.findElement(By.id("user.userName")).sendKeys("Ashish Kumar");
	    driver.findElement(By.xpath("//label[text()='E-mail']")).sendKeys("ashish@gmail.com");
	    driver.findElement(By.linkText("SAVE CHANGES")).click();
	    
	    Thread.sleep(1000);
	    
	    driver.findElement(By.linkText("Customers")).click();
	    driver.findElement(By.cssSelector("a.cell.double >strong")).click();
	    driver.findElement(By.cssSelector("a.submit.edit > span")).click();
	    
	    Thread.sleep(1000);
	    
	    new Select(driver.findElement(By.id("paymentMethod_0.paymentMethodTypeId"))).selectByVisibleText("Credit card");
	    
	    driver.findElement(By.id("paymentMethod_0.processingOrder")).clear();
	    driver.findElement(By.id("paymentMethod_0.processingOrder")).sendKeys("1");
	    
	    driver.findElement(By.xpath(".//*[@id='0_metaField_33.value']")).clear();
	    driver.findElement(By.xpath(".//*[@id='0_metaField_33.value']")).sendKeys("Ashish");
	    
	    driver.findElement(By.xpath(".//*[@id='0_metaField_29.value']")).clear();
	    driver.findElement(By.xpath(".//*[@id='0_metaField_29.value']")).sendKeys("4111111111111152");
	    
	    driver.findElement(By.xpath(".//*[@id='0_metaField_31.value']")).clear();
	    driver.findElement(By.xpath(".//*[@id='0_metaField_31.value']")).sendKeys("02/2020");
	    
	    
	    driver.findElement(By.linkText("SAVE CHANGES")).click();
    
    
	    /*int index = 1;
	    WebElement baseTable = driver.findElement(By.className("table-box"));
	    List<WebElement> tableRows = baseTable.findElements(By.tagName("tr"));
	    String str = tableRows.get(tableRows.size()-1).getAttribute("id");
	    final String id = str.substring(str.lastIndexOf('-')+1);
	    System.out.println(id);*/
    
	    Thread.sleep(1000);
	    
	    String message = driver.findElement(By.cssSelector("div.msg-box.successfully >p")).getText();
	    System.out.println(message+"***");
	    Assert.assertEquals(driver.getPageSource().contains("message"), true);
	    
    
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