package com.example.tests;

import java.util.regex.Pattern;
import java.util.concurrent.TimeUnit;
import org.junit.*;
import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;
import org.openqa.selenium.*;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.support.ui.Select;

public class VerifyInvoice {
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
  public void test1011() throws Exception {
	    
	    //creating new category
	    driver.get(baseUrl + "/product/index");
	    driver.findElement(By.cssSelector("li.active > a > span")).click();
	    driver.findElement(By.linkText("ADD CATEGORY")).click();
	    driver.findElement(By.id("description")).clear();
	    driver.findElement(By.id("description")).sendKeys("Test Code1");
	    driver.findElement(By.cssSelector("option[value=\"10\"]")).click();
	    driver.findElement(By.cssSelector("a.submit.save > span")).click();
    
	    
	    
	    //selecting newly created category and adding product
	    driver.findElement(By.cssSelector("a.cell.double > em")).click();
	    driver.findElement(By.linkText("ADD PRODUCT")).click();
	    driver.findElement(By.cssSelector("img[alt=\"remove\"]")).click();
	    driver.findElement(By.id("product.descriptions[0].content")).clear();
	    driver.findElement(By.id("product.descriptions[0].content")).sendKeys("Test Code");
	    driver.findElement(By.id("product.number")).clear();
	    driver.findElement(By.id("product.number")).sendKeys("Test Code1");
	    driver.findElement(By.id("prices.1")).clear();
	    driver.findElement(By.id("prices.1")).sendKeys("10");
	    driver.findElement(By.cssSelector("a.submit.save > span")).click();
	    
	    Thread.sleep(2000);
	    
	    //selecting customer
	    driver.findElement(By.linkText("Customers")).click();
	    driver.findElement(By.linkText("Ashish Kumar")).click();
	    driver.findElement(By.linkText("CREATE ORDER")).click();
	    
	    Thread.sleep(1000);
	    
	    //creating invoice
	    new Select(driver.findElement(By.id("orderPeriod"))).selectByVisibleText("Monthly");
	    driver.findElement(By.id("billingTypeId")).click();
	    new Select(driver.findElement(By.id("billingTypeId"))).selectByVisibleText("pre paid");
	    driver.findElement(By.id("dueDateValue")).clear();
	    driver.findElement(By.id("dueDateValue")).sendKeys("10");
	    driver.findElement(By.id("ui-id-8")).click();
	    driver.findElement(By.linkText("US$10.00")).click();
	    driver.findElement(By.cssSelector("#change--3-update-form > div.btn-box > a.submit.save > span")).click();
	    driver.findElement(By.linkText("SAVE CHANGES")).click();
	    
	    String str = new String();
	    str = driver.findElement(By.cssSelector("div.msg-box.successfully > p")).getText();
	    System.out.println(str);
	    Assert.assertEquals(driver.getPageSource().contains("str"), true);
	    
	    driver.findElement(By.linkText("GENERATE INVOICE")).click();
	    //driver.findElement(By.cssSelector("a.cell.double >strong")).click();
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