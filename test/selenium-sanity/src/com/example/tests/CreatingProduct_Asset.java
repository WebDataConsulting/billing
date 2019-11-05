package com.example.tests;

import java.util.List;
import java.util.regex.Pattern;
import java.util.concurrent.TimeUnit;

import javax.xml.xpath.XPath;

import org.junit.*;
import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;
import org.openqa.selenium.*;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.support.ui.Select;

import com.thoughtworks.selenium.webdriven.commands.Click;

public class CreatingProduct_Asset {
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
	
	    driver.get(baseUrl + "/config/index");
	    driver.findElement(By.cssSelector("li.active > a > span")).click();
	    driver.findElement(By.linkText("Plug-ins")).click();
	    driver.findElement(By.xpath("//a[@id='17']/em")).click();
	    driver.findElement(By.cssSelector("a.submit.add > span")).click();
	    new Select(driver.findElement(By.id("typeId"))).selectByVisibleText("com.sapienter.jbilling.server.item.tasks.AssetUpdatedTask");
	    driver.findElement(By.cssSelector("option[value=\"111\"]")).click();
	    
	    Thread.sleep(1000);
	    //WE WILL HAVE TO DELETE PLUGIN WITH PROCESSING ORDER 10 
	    driver.findElement(By.id("processingOrder")).sendKeys("10");
	    driver.findElement(By.linkText("SAVE PLUG-IN")).click();
	    
	    driver.findElement(By.linkText("Products")).click();
	    driver.findElement(By.xpath("//strong[text()='Asset category1']")).click();
	    driver.findElement(By.cssSelector("#column2 > div.btn-box > a.submit.add > span")).click();
	    driver.findElement(By.cssSelector("img[alt=\"remove\"]")).click();
	    driver.findElement(By.id("product.descriptions[0].content")).clear();
	    driver.findElement(By.id("product.descriptions[0].content")).sendKeys("SIM-card1");
	    driver.findElement(By.id("product.number")).clear();
	    driver.findElement(By.id("product.number")).sendKeys("S001");
	    driver.findElement(By.id("assetManagementEnabled")).click();
	    driver.findElement(By.id("global-checkbox")).click();
	    driver.findElement(By.id("prices.1")).clear();
	    driver.findElement(By.id("prices.1")).sendKeys("2");
	    driver.findElement(By.linkText("SAVE CHANGES")).click();
	    
	    Thread.sleep(1000);
	    
	    driver.findElement(By.linkText("Products")).click();
	    driver.findElement(By.xpath("//strong[text()='Asset category1']")).click();
	    driver.findElement(By.xpath("//strong[text()='SIM-card1']")).click();
	    
	    driver.findElement(By.linkText("ADD ASSET")).click();
	    driver.findElement(By.id("identifier")).clear();
	    driver.findElement(By.id("identifier")).sendKeys("SIM-101");
	    driver.findElement(By.id("global-checkbox")).click();
	    driver.findElement(By.xpath("//label[text()='Tax ID']")).click();
	    driver.findElement(By.xpath("//label[text()='Tax ID']")).sendKeys("T-101");
	    driver.findElement(By.linkText("SAVE CHANGES")).click();
	    
	    Thread.sleep(2000);
		
	    driver.findElement(By.linkText("Products")).click();
		driver.findElement(By.cssSelector("a.cell.double  > strong")).click();
		
		driver.findElement(By.xpath("//strong[text()='SIM-card1']")).click();
		driver.findElement(By.linkText("ADD ASSET")).click();
	    driver.findElement(By.id("identifier")).clear();
	    driver.findElement(By.id("identifier")).sendKeys("SIM-102");
	    new Select(driver.findElement(By.id("company-select"))).selectByVisibleText("WDT Child");
	    driver.findElement(By.xpath("//label[text()='Tax ID']")).click();
	    driver.findElement(By.xpath("//label[text()='Tax ID']")).sendKeys("T-102");
	    driver.findElement(By.linkText("SAVE CHANGES")).click();
	    
	   
	    Assert.assertEquals(driver.getPageSource().contains("SIM-102"), true);
    
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