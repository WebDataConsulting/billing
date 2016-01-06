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
import org.testng.asserts.Assertion;

public class CreatingProduct {
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
  public void test33() throws Exception {
	 
	    driver.get(baseUrl + "/product");
	    
	    //getting id of the element we want to click
	    int index = 1;
	    WebElement baseTable = driver.findElement(By.className("table-scroll"));
	    List<WebElement> tableRows = baseTable.findElements(By.tagName("tr"));
	    String str = tableRows.get(tableRows.size()-1).getAttribute("id");
	    final String id = str.substring(str.lastIndexOf('-')+1);
	    System.out.println(id);
	    
	    
	    //clicking to the specified category 
	    driver.findElement(By.id(id)).click();
	    
	    
	    //Adding product to the category
	    driver.findElement(By.cssSelector("#column2 > div.btn-box > a.submit.add > span")).click();
	    driver.findElement(By.cssSelector("img[alt=\"remove\"]")).click();
	    driver.findElement(By.id("product.descriptions[0].content")).clear();
	    driver.findElement(By.id("product.descriptions[0].content")).sendKeys("Product code1 Description");
	    driver.findElement(By.id("global-checkbox")).click();
	    driver.findElement(By.id("product.number")).clear();
	    driver.findElement(By.id("product.number")).sendKeys("Product Code1");
	    driver.findElement(By.id("product.standardAvailability")).click();
	    
	    Thread.sleep(1000);
	    
	    new Select(driver.findElement(By.id("product.accountTypes"))).selectByVisibleText("Direct Customer");
	    new Select(driver.findElement(By.id("company-select"))).selectByVisibleText("WDT Child");
	    
	    //driver.manage().timeouts().implicitlyWait(30, TimeUnit.SECONDS);
	    
	    driver.findElement(By.id("prices.1")).clear();
	    driver.findElement(By.id("prices.1")).sendKeys("500");
	    driver.findElement(By.cssSelector("a.submit.save > span")).click();
	    
	    Thread.sleep(1000);
	    
	    driver.findElement(By.cssSelector("div.btn-box > a.submit.add > span")).click();
	    driver.findElement(By.cssSelector("img[alt=\"remove\"]")).click();
	    driver.findElement(By.id("product.descriptions[0].content")).clear();
	    driver.findElement(By.id("product.descriptions[0].content")).sendKeys("Test Code Description");
	    driver.findElement(By.id("product.number")).clear();
	    driver.findElement(By.id("product.number")).sendKeys("Test Code");
	    
	    driver.findElement(By.id("product.standardAvailability")).click();
	    new Select(driver.findElement(By.id("company-select"))).selectByVisibleText("WDT Child");
	    
	    driver.findElement(By.id("prices.1")).clear();
	    driver.findElement(By.id("prices.1")).sendKeys("10");
	    driver.findElement(By.cssSelector("a.submit.save > span")).click();
	    
	    Thread.sleep(1000);
	    
	    driver.findElement(By.cssSelector("a.cell.double > strong")).click();
	    driver.findElement(By.cssSelector("a.submit.edit > span")).click();
	    driver.findElement(By.id("prices.1")).clear();
	    driver.findElement(By.id("prices.1")).sendKeys("15.0000000000");
	    driver.findElement(By.cssSelector("a.submit.save > span")).click();
	    
	    
	    //Applying assertion
	    driver.findElement(By.cssSelector("a.cell.double > strong")).click();
	    Assert.assertEquals(driver.getPageSource().contains("15.00000"), true);
  
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