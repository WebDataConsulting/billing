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

public class TearDownProcess {
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
	  public void testS131() throws Exception {
		  
		  
		  String str;
		  
		  //Deleting payments
		  driver.findElement(By.linkText("Payments")).click();
		  driver.findElement(By.cssSelector("a.cell.double > strong")).click();
		  
		  /*driver.findElement(By.linkText("Unlink")).click();
		  driver.findElement(By.xpath("(//button[@type='button'])[5]")).click();*/
		  
		  driver.findElement(By.linkText("DELETE")).click();
		  driver.findElement(By.xpath("(//button[@type='button'])[8]")).click();

		  //Assertion
		  /*str = driver.findElement(By.cssSelector("div.msg-box.successfully >p")).getText();
		  System.out.println("Deleting Payments : "+str);
		  Assert.assertEquals(driver.getPageSource().contains("str"), true);*/
		  
		  
		  
		  
		  //Deleting Invoices 
		  driver.findElement(By.linkText("Invoices")).click();
		  driver.findElement(By.cssSelector("a.cell.double > strong")).click();
		  driver.findElement(By.linkText("DELETE INVOICE")).click();
		  driver.findElement(By.xpath("(//button[@type='button'])[8]")).click();
		  
		  //Assertion
		  /*str = driver.findElement(By.cssSelector("div.msg-box.successfully >p")).getText();
		  System.out.println("Deleting Invoices : "+str);
		  Assert.assertEquals(driver.getPageSource().contains("str"), true);*/
		  
		  
		  
		  
		  
		  //Deleting Orders
		  driver.findElement(By.linkText("Orders")).click();
		  driver.findElement(By.cssSelector("td > a.cell.double >strong")).click();
		  driver.findElement(By.linkText("DELETE")).click();
		  driver.findElement(By.xpath("(//button[@type='button'])[5]")).click();
		  
		  //Assertion
		  /*str = driver.findElement(By.cssSelector("div.msg-box.successfully >p")).getText();
		  System.out.println("Deleting Orders : "+str);
		  Assert.assertEquals(driver.getPageSource().contains("str"), true);*/
		  
		  
		  
		  
		  
		  
		  //Deleting Products
		  driver.findElement(By.linkText("Products")).click();
		  driver.findElement(By.cssSelector("td.small > a.cell >strong")).click();
			  driver.findElement(By.cssSelector("td.medium > a.cell >strong")).click();
			  driver.findElement(By.linkText("DELETE")).click();
			  driver.findElement(By.xpath("(//button[@type='button'])[8]")).click();
			  
			  driver.findElement(By.cssSelector("td.small > a.cell >strong")).click();
		  	  driver.findElement(By.linkText("DELETE CATEGORY")).click();
		      driver.findElement(By.xpath("(//button[@type='button'])[8]")).click();
		 
		  
		  
		  
		  //Assertion
		 /* str = driver.findElement(By.cssSelector("div.msg-box.successfully >p")).getText();
		  System.out.println("Deleting Products : "+str);
		  Assert.assertEquals(driver.getPageSource().contains("str"), true);*/
		  
		  
		  
		  //Deleting Customer
		  driver.findElement(By.linkText("Customers")).click();
		  driver.findElement(By.cssSelector("a.cell.double >strong")).click();
		  driver.findElement(By.linkText("DELETE")).click();
		  driver.findElement(By.xpath("(//button[@type='button'])[8]")).click();
		  
		  //Assertion
		 /* str = driver.findElement(By.cssSelector("div.msg-box.successfully >p")).getText();
		  System.out.println("Deleting Customers : "+str);
		  Assert.assertEquals(driver.getPageSource().contains("str"), true);*/

		  
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