package com.example.tests;

import java.util.List;
import java.util.regex.Pattern;
import java.util.concurrent.TimeUnit;
import org.junit.*;
import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;
import org.openqa.selenium.*;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

public class CreatingCategory {
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
  public void test31() throws Exception {
	    
		//CREATING NEW CATEGORY    
	    driver.get(baseUrl + "/customer/index");
	    driver.findElement(By.xpath("//ul[@id='navList']/li[9]/a/span")).click();
	    driver.findElement(By.cssSelector("div.btn-box > a.submit.add > span")).click();
	    driver.findElement(By.id("description")).clear();
	    driver.findElement(By.id("description")).sendKeys("Test Category1");
	    driver.findElement(By.cssSelector("option[value=\"11\"]")).click();
	    driver.findElement(By.cssSelector("a.submit.save > span")).click();
    
	    Thread.sleep(1000);
    
	   /* int index = 1;
	    WebElement baseTable = driver.findElement(By.className("table-box"));
	    List<WebElement> tableRows = baseTable.findElements(By.tagName("tr"));
	    String str = tableRows.get(tableRows.size()-1).getAttribute("id");
	    final String id = str.substring(str.lastIndexOf('-')+1);
	    System.out.println(id);*/
    
   
    
    
	    driver.get(baseUrl + "/product/index");
	    driver.findElement(By.cssSelector("a.cell.double >strong")).click();
	    driver.findElement(By.cssSelector("a.submit.edit > span")).click();
	    driver.findElement(By.id("description")).clear();
	    driver.findElement(By.id("description")).sendKeys("New Test category1");
	    driver.findElement(By.id("global-checkbox")).click();
	    driver.findElement(By.cssSelector("a.submit.save > span")).click();
	    
	    
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