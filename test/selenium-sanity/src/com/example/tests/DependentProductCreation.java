package com.example.tests;

import static org.junit.Assert.fail;

import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.NoAlertPresentException;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.support.ui.Select;
import org.testng.Assert;

public class DependentProductCreation {
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
  public void testS151() throws Exception {
	  
	    //adding product category
	    driver.get(baseUrl + "/product/index");
	    driver.findElement(By.linkText("ADD CATEGORY")).click();
	    new Select(driver.findElement(By.id("company-select"))).selectByVisibleText("WDT Jaipur");
	    driver.findElement(By.id("description")).clear();
	    driver.findElement(By.id("description")).sendKeys("Dependent Product1");
	    driver.findElement(By.cssSelector("a.submit.save > span")).click();
	    
	    Thread.sleep(1000);
	    
	    //Adding product to the newly created category
	    driver.findElement(By.cssSelector("td > a.cell.double > strong")).click();
	    driver.findElement(By.cssSelector("#column2 > div.btn-box > a.submit.add > span")).click();
	    driver.findElement(By.cssSelector("img[alt=\"remove\"]")).click();
	    driver.findElement(By.id("product.descriptions[0].content")).clear();
	    driver.findElement(By.id("product.descriptions[0].content")).sendKeys("Installation fee");
	    driver.findElement(By.id("product.number")).clear();
	    driver.findElement(By.id("product.number")).sendKeys("D-01");
	    driver.findElement(By.id("prices.1")).clear();
	    driver.findElement(By.id("prices.1")).sendKeys("50");
	    driver.findElement(By.cssSelector("a.submit.save > span")).click();
	    
	    Thread.sleep(1000);
	    
	    driver.findElement(By.cssSelector("li.active > a > span")).click();
	    driver.findElement(By.cssSelector("td > a.cell.double > strong")).click();
	    driver.findElement(By.cssSelector("#column2 > div.btn-box > a.submit.add > span")).click();
	    driver.findElement(By.cssSelector("img[alt=\"remove\"]")).click();
	    driver.findElement(By.id("product.descriptions[0].content")).clear();
	    driver.findElement(By.id("product.descriptions[0].content")).sendKeys("Date Service");
	    driver.findElement(By.id("product.number")).clear();
	    driver.findElement(By.id("product.number")).sendKeys("D-02");
	    driver.findElement(By.id("prices.1")).clear();
	    driver.findElement(By.id("prices.1")).sendKeys("10");
	    driver.findElement(By.cssSelector("#dependency > div.box-cards-title > a.btn-open > span")).click();
	    new Select(driver.findElement(By.name("product.dependencyItemTypes"))).selectByVisibleText("Dependent Product1");
	    //driver.findElement(By.xpath("(//option[@value='1100'])[3]")).click();
	    driver.findElement(By.name("product.dependencyItems")).click();
	    driver.findElement(By.name("add")).click();
	    driver.findElement(By.cssSelector("a.submit.save > span")).click();
	    
	    
	    
	    //applying assertion
	    String str = new String();
	    str = driver.findElement(By.cssSelector("div.msg-box.successfully > p")).getText();
	    System.out.println(str+"*****");
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