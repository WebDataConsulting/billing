package com.sapienter.jbilling.selenium.sanity;

import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;
import java.util.concurrent.TimeUnit;

import junit.framework.TestCase;

import org.junit.*;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

import org.openqa.selenium.*;
import org.openqa.selenium.Alert;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.support.ui.Select;
import org.testng.annotations.Test;

@Test(groups = { "selenium-ui-functional", "sanity" })
public class JBillingSanityTest extends TestCase {
  private WebDriver driver;
  private String baseUrl;
  private boolean acceptNextAlert = true;
  private StringBuffer verificationErrors = new StringBuffer();

  @BeforeClass
  public void setUp() throws Exception {
    driver = new FirefoxDriver();
    baseUrl = "http://localhost:8080/jbilling";
    driver.manage().timeouts().implicitlyWait(5, TimeUnit.MINUTES);
  }

  @Test
  public void testJbillingSanityTest() throws Exception {
	  	
	  	// login
	  	driver.get(baseUrl + "/login/auth");
	    driver.findElement(By.id("j_username")).clear();
	    driver.findElement(By.id("j_username")).sendKeys("admin");
	    driver.findElement(By.id("j_password")).clear();
	    driver.findElement(By.id("j_password")).sendKeys("123qwe");
	    WebElement selectCompany = driver.findElement(By.id("j_client_id"));
	    List<WebElement> companies = selectCompany.findElements(By.tagName("option"));
	    for (WebElement option : companies) {
	    	if("Prancing Pony".equals(option.getText())) {
	    		option.click();
	    		break;
	    	}
	    }

	    driver.findElement(By.cssSelector("a.submit.save > span")).click();
	    
	    Thread.sleep(1000);
	    
	    //Add Category
	    driver.get(baseUrl + "/product/editCategory?add=true");
	    driver.findElement(By.id("description")).clear();
	    driver.findElement(By.id("description")).sendKeys("Test Category"+new Date().getSeconds()); //+new Date().getSeconds()
	    driver.findElement(By.cssSelector("a.submit.save > span")).click();
	    driver.findElement(By.cssSelector("li.active > a > span")).click();
	    
	    Thread.sleep(1000);
	    
	    //Edit Category - below change be changed to a driver.get(url) 
	    driver.findElement(By.cssSelector("li.active > a > span")).click();
	    driver.findElement(By.cssSelector("a.cell.double > strong")).click();
	    Thread.sleep(1000);
	    driver.findElement(By.cssSelector("a.submit.edit > span")).click();
	    driver.findElement(By.id("description")).clear();
	    driver.findElement(By.id("description")).sendKeys("New Test Category"+new Date().getSeconds()); //+new Date().getSeconds()
	    driver.findElement(By.cssSelector("a.submit.save > span")).click();
	    driver.findElement(By.cssSelector("a.cell.double > strong")).click();
	    Thread.sleep(1000);
	    driver.findElement(By.cssSelector("#column2 > div.btn-box > a.submit.add > span")).click();
	    driver.findElement(By.id("product.number")).clear();
	    driver.findElement(By.id("product.number")).sendKeys("Test Code"+new Date().getSeconds()); //+new Date().getSeconds()
	    // Show English description textfield on product UI 
	    driver.findElement(By.cssSelector("img[alt='remove']")).click();

	    driver.findElement(By.id("product.descriptions[0].content")).clear();
	    driver.findElement(By.id("product.descriptions[0].content")).sendKeys("Test Code Description"+new Date().getSeconds()); //+new Date().getSeconds()
	    // Click on save changes Button on product UI
	    driver.findElement(By.cssSelector("a.submit.save > span")).click();
	    
	    Thread.sleep(1000);
	    
	    // driver.findElement(By.xpath("//form[@id='save-product-form']/fieldset/div[2]")).click();
	    // driver.findElement(By.linkText("Save Changes")).click();
	    driver.findElement(By.cssSelector("li.active > a > span")).click();
	    driver.findElement(By.cssSelector("a.cell.double > strong")).click();
	    Thread.sleep(1000);
	    driver.findElement(By.cssSelector("#column2 > div.btn-box > a.submit.add > span")).click();
	    driver.findElement(By.cssSelector("img[alt='remove']")).click();
	    driver.findElement(By.id("product.descriptions[0].content")).clear();
	    driver.findElement(By.id("product.descriptions[0].content")).sendKeys("Test Code Description 1"+new Date().getSeconds()); // +new Date().getSeconds()
	    
	    driver.findElement(By.id("product.number")).clear();
	    driver.findElement(By.id("product.number")).sendKeys("Test Code 1"+new Date().getSeconds()); //+new Date().getSeconds()
	    
	    WebElement select = driver.findElement(By.id("model.0.type"));
	    List<WebElement> priceModelOptions = select.findElements(By.tagName("option"));
	    for (WebElement option : priceModelOptions) {
	    	if("Metered".equals(option.getText())) {
	    		option.click();
	    		break;
	    	}
	    }
	    
	    Thread.sleep(1000);
	    // driver.findElement(By.cssSelector("option[value=\"FLAT\"]")).click();
	    driver.findElement(By.id("model.0.rateAsDecimal")).clear();
	    driver.findElement(By.id("model.0.rateAsDecimal")).sendKeys("12.00");
	    // select Currency from Dropdown list
	    WebElement currencyCode = driver.findElement(By.id("model.0.currencyId"));
	    List<WebElement> currencyOptions = currencyCode.findElements(By.tagName("option"));
	    for (WebElement option : currencyOptions) {
	    	if("United States Dollar".equals(option.getText())) {
	    		option.click();
	    		break;
	    	}
	    }
	    Thread.sleep(1000);
	    driver.findElement(By.cssSelector("a.submit.save > span")).click();
	    
	    Thread.sleep(1000);
	    
	    driver.findElement(By.linkText("Products")).click();
	    driver.findElement(By.cssSelector("#category-2400 > td > a.cell.double > strong")).click();
	    Thread.sleep(1000);
	    driver.findElement(By.cssSelector("#product-3200 > td > a.cell.double > strong")).click();
	    Thread.sleep(1000);
	    driver.findElement(By.cssSelector("#column2 > div.btn-box > a.submit.edit > span")).click();
	    
	    driver.findElement(By.id("product.number")).clear();
	    driver.findElement(By.id("product.number")).sendKeys("New Test Code"+new Date().getSeconds()); //+new Date().getSeconds()
	    driver.findElement(By.id("product.descriptions[0].content")).clear();
	    driver.findElement(By.id("product.descriptions[0].content")).sendKeys("New Test Code Description "+new Date().getSeconds()); // +new Date().getSeconds()
	    driver.findElement(By.id("product.percentageAsDecimal")).clear();
	    driver.findElement(By.id("product.percentageAsDecimal")).sendKeys("15");
	    driver.findElement(By.cssSelector("a.submit.save > span")).click();
	    
	    Thread.sleep(1000);
	    
	    // Customers list
	    driver.findElement(By.linkText("Customers")).click();
	    
	    // Create new customer
	    driver.findElement(By.cssSelector("a[href='/jbilling/customer/edit']")).click();
	    driver.findElement(By.id("user.userName")).clear();
	    driver.findElement(By.id("user.userName")).sendKeys("Ashish"+new Date().getSeconds()); //+new Date().getSeconds()
	    driver.findElement(By.id("newPassword")).clear();
	    driver.findElement(By.id("newPassword")).sendKeys("12345");
	    driver.findElement(By.id("verifiedPassword")).clear();
	    driver.findElement(By.id("verifiedPassword")).sendKeys("12345");
	    driver.findElement(By.id("contact-2.email")).clear();
	    driver.findElement(By.id("contact-2.email")).sendKeys("ashishs@360logica.com");
	
	    // select country from Dropdown list
	    WebElement countryCode = driver.findElement(By.id("contact-2.countryCode"));
	    List<WebElement> countryCodeOptions = countryCode.findElements(By.tagName("option"));
	    for (WebElement option : countryCodeOptions) {
	    	if("India".equals(option.getText())) {
	    		option.click();
	    		break;
	    	}
	    }
	    Thread.sleep(1000);
	    driver.findElement(By.cssSelector("a.submit.save > span")).click();
	    Thread.sleep(1000);
	    
	    // Edit existing customer
	    driver.findElement(By.cssSelector("a.submit.edit > span")).click();
	    Thread.sleep(1000);
	    driver.findElement(By.id("contact-2.email")).clear();
	    driver.findElement(By.id("contact-2.email")).sendKeys("ashishs@gmail.com");
	    driver.findElement(By.id("contact-2.address1")).clear();
	    driver.findElement(By.id("contact-2.address1")).sendKeys("Jaipur");
	    driver.findElement(By.id("contact-2.address2")).clear();
	    driver.findElement(By.id("contact-2.address2")).sendKeys("Elements Mall");
	    driver.findElement(By.id("contact-2.stateProvince")).clear();
	    driver.findElement(By.id("contact-2.stateProvince")).sendKeys("Rajasthan");
	    driver.findElement(By.id("contact-2.city")).clear();
	    driver.findElement(By.id("contact-2.city")).sendKeys("Jaipur");
	    driver.findElement(By.id("contact-2.postalCode")).clear();
	    driver.findElement(By.id("contact-2.postalCode")).sendKeys("302021");
	    
	    driver.findElement(By.cssSelector("a.btn-open > span")).click();
	    Thread.sleep(1000);
	    driver.findElement(By.id("creditCard.name")).clear();
	    driver.findElement(By.id("creditCard.name")).sendKeys("Ashish");
	    driver.findElement(By.id("creditCard.number")).clear();
	    driver.findElement(By.id("creditCard.number")).sendKeys("4111111111111152");
	    driver.findElement(By.id("expiryMonth")).clear();
	    driver.findElement(By.id("expiryMonth")).sendKeys("12");
	    driver.findElement(By.id("expiryYear")).clear();
	    driver.findElement(By.id("expiryYear")).sendKeys("2020");
	    driver.findElement(By.cssSelector("a.submit.save > span")).click();
	    
	    // Create Purchase Order:
	    driver.findElement(By.cssSelector("a.submit.order > span")).click();
	    WebElement selectOrder = driver.findElement(By.id("period"));
	    List<WebElement> order = selectOrder.findElements(By.tagName("option"));
	    for (WebElement option : order) {
	    	if("Monthly".equals(option.getText())) {
	    		option.click();
	    		break;
	    	}
	    }
	   
	    WebElement selectTypeid = driver.findElement(By.id("billingTypeId"));
	    List<WebElement> typeid = selectTypeid.findElements(By.tagName("option"));
	    for (WebElement option : typeid) {
	    	if("pre paid".equals(option.getText())) {
	    		option.click();
	    		break;
	    	}
	    }
	  
	    
	    WebElement selectStatusid = driver.findElement(By.id("statusId"));
	    List<WebElement> statusid = selectStatusid.findElements(By.tagName("option"));
	    for (WebElement option : statusid) {
	    	if("Active".equals(option.getText())) {
	    		option.click();
	    		break;
	    	}
	    }
	    
	    driver.findElement(By.id("activeSince")).click();
	    driver.findElement(By.id("activeSince")).clear();
	    driver.findElement(By.id("activeSince")).sendKeys("01/01/2010");
	    
	    driver.findElement(By.xpath("//form[@id='order-details-form']/div/div[4]")).click();
	    driver.findElement(By.xpath("//a[contains(text(),'Products')]")).click();
	    driver.findElement(By.id("filterBy")).clear();
	    driver.findElement(By.id("filterBy")).sendKeys("New Test Code");
	    driver.findElement(By.cssSelector("strong")).click();
	    driver.findElement(By.cssSelector("a.submit.save > span")).click();
	   
	    // Generate Invoice
	    driver.findElement(By.cssSelector("a.submit.order > span")).click();
	    
	    Thread.sleep(1000);
	    
	    // Pay Invoice
	    driver.findElement(By.cssSelector("a.submit.payment > span")).click();
	    
	    Thread.sleep(1000);
	    
	    //Review Payment
	    driver.findElement(By.id("processNow")).click();
	    driver.findElement(By.cssSelector("a.submit.payment > span")).click();
	    
	    Thread.sleep(1000);
	    
	    //Make Payment
	    driver.findElement(By.cssSelector("a.submit.payment > span")).click();
	    
	    Thread.sleep(1000);
	    
	    //Tear Down process
	    
	    driver.findElement(By.linkText("Unlink")).click();
	    Thread.sleep(1000);
	    driver.findElement(By.xpath("(//button[@type='button'])[2]")).click();
	    Thread.sleep(1000);
	    
	   // Delete Payment
	    driver.findElement(By.cssSelector("a.submit.delete > span")).click();
	    Thread.sleep(1000);
	    driver.findElement(By.xpath("(//button[@type='button'])[4]")).click();
	    
	    // Delete Invoice
	    
	    driver.findElement(By.linkText("Invoices")).click();
	    Thread.sleep(1000);
	    driver.findElement(By.cssSelector("a.cell.double > strong")).click();
	    Thread.sleep(500);
	    driver.findElement(By.cssSelector("a.submit.delete > span")).click();
	    Thread.sleep(1000);
	    driver.findElement(By.xpath("(//button[@type='button'])[4]")).click();
	    driver.findElement(By.xpath("//div[@id='navigation']/ul/li[8]/a/span")).click();
	    Thread.sleep(1000);
	   // driver.findElement(By.xpath("(//button[@type='button'])[6]")).click();
	    
	    //driver.findElement(By.xpath("(//button[@type='button'])[8]")).click();
	    
	    
	    
	    
   //Delete Order
	 
	    driver.findElement(By.linkText("Orders")).click();
	    Thread.sleep(1000);
	    driver.findElement(By.cssSelector("a.cell.double > strong")).click();
	    Thread.sleep(500);
	    driver.findElement(By.cssSelector("a.submit.delete > span")).click();
	    Thread.sleep(1000);
	    driver.findElement(By.xpath("(//button[@type='button'])[2]")).click();
	    Thread.sleep(1000);
	    
//	    //Delete Product
//	    
	    driver.findElement(By.linkText("Products")).click();
	    Thread.sleep(1000);
	    driver.findElement(By.cssSelector("#category-2400 > td > a.cell.double > strong")).click();
	    Thread.sleep(500);
	    driver.findElement(By.cssSelector("#product-3200 > td > a.cell.double > strong")).click();
	    Thread.sleep(500);
	    driver.findElement(By.cssSelector("a.submit.delete > span")).click();
	    Thread.sleep(1000);
	    driver.findElement(By.xpath("(//button[@type='button'])[6]")).click();
	    Thread.sleep(1000);
	   
//	    //Delete Customer
	    
	    driver.findElement(By.linkText("Customers")).click();
	    Thread.sleep(1000);
	    driver.findElement(By.cssSelector("a.cell.double > strong")).click();
	    Thread.sleep(500);
	    driver.findElement(By.cssSelector("a.submit.delete > span")).click();
	    Thread.sleep(1000);
	    driver.findElement(By.xpath("(//button[@type='button'])[2]")).click();
	    Thread.sleep(1000);
	    
//	    
//	    //Logout Jbilling
//	    
	    driver.findElement(By.linkText("Logout")).click();
	    Thread.sleep(1000);
	    
//	    driver.findElement(By.linkText("editProduct")).click();
	    /*driver.findElement(By.cssSelector("li.active > a > span")).click();
	    driver.findElement(By.cssSelector("a.cell.double > em")).click();
	    driver.findElement(By.cssSelector("#product-3200 > td > a.cell.double > strong")).click();
	    driver.findElement(By.cssSelector("a.submit.edit > span")).click();*/
	    
	    //Below Code having failure and need to appropiate invistigation.
	    /*Thread.sleep(1000);
	    driver.findElement(By.id("product.percentageAsDecimal")).clear();
	    driver.findElement(By.id("product.percentageAsDecimal")).sendKeys("15");
	    driver.findElement(By.id("product.description")).clear();
	    driver.findElement(By.id("product.description")).sendKeys("New Test Code Description");
	    driver.findElement(By.id("product.number")).clear();
	    driver.findElement(By.id("product.number")).sendKeys("New Test Code");
	    driver.findElement(By.cssSelector("a.submit.save > span")).click();
	    
	    driver.findElement(By.cssSelector("span")).click();
	    driver.findElement(By.cssSelector("span")).click();
	    driver.findElement(By.cssSelector("div.btn-box > a.submit.add > span")).click();
	    driver.findElement(By.id("user.userName")).clear();
	    driver.findElement(By.id("user.userName")).sendKeys("Ashish");
	    driver.findElement(By.id("newPassword")).clear();
	    driver.findElement(By.id("newPassword")).sendKeys("12345");
	    driver.findElement(By.id("verifiedPassword")).clear();
	    driver.findElement(By.id("verifiedPassword")).sendKeys("12345");
	    driver.findElement(By.id("contact-2.email")).clear();
	    driver.findElement(By.id("contact-2.email")).sendKeys("ashish@gmail.com");
	    driver.findElement(By.cssSelector("a.submit.save > span")).click();
	    driver.findElement(By.cssSelector("span")).click();
	    driver.findElement(By.cssSelector("a.cell.double > strong")).click();
	    driver.findElement(By.linkText("Edit")).click();
	    driver.findElement(By.id("contact-2.email")).clear();
	    driver.findElement(By.id("contact-2.email")).sendKeys("ashish@ymail.com");
	    driver.findElement(By.id("contact-2.address1")).clear();
	    driver.findElement(By.id("contact-2.address1")).sendKeys("Jaipur");
	    driver.findElement(By.id("contact-2.address2")).clear();
	    driver.findElement(By.id("contact-2.address2")).sendKeys("Elements Mall");
	    driver.findElement(By.id("contact-2.stateProvince")).clear();
	    driver.findElement(By.id("contact-2.stateProvince")).sendKeys("Rajasthan");
	    driver.findElement(By.id("contact-2.city")).clear();
	    driver.findElement(By.id("contact-2.city")).sendKeys("Jaipur");
	    driver.findElement(By.id("contact-2.postalCode")).clear();
	    driver.findElement(By.id("contact-2.postalCode")).sendKeys("302021");
	    new Select(driver.findElement(By.id("contact-2.countryCode"))).selectByVisibleText("India");
	    driver.findElement(By.cssSelector("a.btn-open > span")).click();
	    driver.findElement(By.id("creditCard.name")).clear();
	    driver.findElement(By.id("creditCard.name")).sendKeys("Ashish");
	    driver.findElement(By.id("creditCard.number")).clear();
	    driver.findElement(By.id("creditCard.number")).sendKeys("41111111111111");
	    driver.findElement(By.id("expiryMonth")).clear();
	    driver.findElement(By.id("expiryMonth")).sendKeys("12");
	    driver.findElement(By.id("expiryYear")).clear();
	    driver.findElement(By.id("expiryYear")).sendKeys("2020");
	    driver.findElement(By.cssSelector("a.submit.save > span")).click();
	    driver.findElement(By.id("creditCard.number")).clear();
	    driver.findElement(By.id("creditCard.number")).sendKeys("4111111111111152");
	    driver.findElement(By.cssSelector("a.submit.save > span")).click();
	    driver.findElement(By.cssSelector("span")).click();
	    driver.findElement(By.cssSelector("a.cell.double > strong")).click();
	    driver.findElement(By.cssSelector("a.submit.order > span")).click();
	    new Select(driver.findElement(By.id("period"))).selectByVisibleText("Monthly");
	    driver.findElement(By.cssSelector("option[value=\"2\"]")).click();
	    new Select(driver.findElement(By.id("billingTypeId"))).selectByVisibleText("pre paid");
	    driver.findElement(By.cssSelector("#billingTypeId > option[value=\"1\"]")).click();
	    driver.findElement(By.cssSelector("img.ui-datepicker-trigger")).click();
	    driver.findElement(By.cssSelector("span.ui-icon.ui-icon-circle-triangle-w")).click();
	    driver.findElement(By.cssSelector("span.ui-icon.ui-icon-circle-triangle-w")).click();
	    driver.findElement(By.linkText("1")).click();
	    driver.findElement(By.id("activeSince")).click();
	    driver.findElement(By.id("activeSince")).clear();
	    driver.findElement(By.id("activeSince")).sendKeys("01/01/2010");
	    driver.findElement(By.xpath("//form[@id='order-details-form']/div/div[2]")).click();
	    driver.findElement(By.xpath("//a[contains(text(),'Products')]")).click();
	    driver.findElement(By.xpath("//table[@id='products']/tbody/tr[2]/td/a/strong")).click();
	    driver.findElement(By.cssSelector("a.submit.save > span")).click();
	    driver.findElement(By.cssSelector("#review-box > div.btn-box > a.submit.save > span")).click();
	    driver.findElement(By.cssSelector("span")).click();
	    driver.findElement(By.cssSelector("a.cell.double > strong")).click();
	    driver.findElement(By.cssSelector("a.submit.edit > span")).click();
	    driver.findElement(By.cssSelector("span")).click();
	    driver.findElement(By.cssSelector("a.cell.double > strong")).click();
	    driver.findElement(By.cssSelector("a.submit.order > span")).click();
	    new Select(driver.findElement(By.id("period"))).selectByVisibleText("Monthly");
	    driver.findElement(By.cssSelector("option[value=\"2\"]")).click();
	    new Select(driver.findElement(By.id("billingTypeId"))).selectByVisibleText("pre paid");
	    driver.findElement(By.cssSelector("#billingTypeId > option[value=\"1\"]")).click();
	    driver.findElement(By.cssSelector("img.ui-datepicker-trigger")).click();
	    driver.findElement(By.cssSelector("span.ui-icon.ui-icon-circle-triangle-w")).click();
	    driver.findElement(By.cssSelector("span.ui-icon.ui-icon-circle-triangle-w")).click();
	    driver.findElement(By.linkText("1")).click();
	    driver.findElement(By.id("activeSince")).click();
	    driver.findElement(By.id("activeSince")).clear();
	    driver.findElement(By.id("activeSince")).sendKeys("01/01/2010");
	    driver.findElement(By.xpath("//form[@id='order-details-form']/div/div[4]")).click();
	    driver.findElement(By.xpath("//a[contains(text(),'Products')]")).click();
	    driver.findElement(By.cssSelector("strong")).click();
	    driver.findElement(By.cssSelector("a.submit.save > span")).click();
	    driver.findElement(By.cssSelector("#review-box > div.btn-box > a.submit.save > span")).click();
	    driver.findElement(By.cssSelector("a.submit.order > span")).click();
	    driver.findElement(By.xpath("//div[@id='navigation']/ul/li[4]/a/span")).click();
	    driver.findElement(By.xpath("//tr[@id='order-107900']/td[2]/a/strong")).click();
	    driver.findElement(By.cssSelector("a.submit.order > span")).click();
	    driver.findElement(By.xpath("(//a[contains(text(),'04/19/2014')])[2]")).click();
	    driver.findElement(By.cssSelector("a.submit.payment > span")).click();
	    driver.findElement(By.cssSelector("a.submit.payment > span")).click();
	    driver.findElement(By.linkText("Make Payment")).click();
	    driver.findElement(By.cssSelector("li.active > a > span")).click();
	    driver.findElement(By.cssSelector("a.cell.double > strong")).click();
	    driver.findElement(By.linkText("Unlink")).click();
	    driver.findElement(By.linkText("Delete")).click();
	    driver.findElement(By.xpath("(//button[@type='button'])[2]")).click();
	    driver.findElement(By.xpath("//div[@id='navigation']/ul/li[2]/a/span")).click();
	    driver.findElement(By.cssSelector("a.cell.double > strong")).click();
	    driver.findElement(By.cssSelector("a.submit.delete > span")).click();
	    driver.findElement(By.xpath("(//button[@type='button'])[4]")).click();
	    driver.findElement(By.cssSelector("a.cell.double > strong")).click();
	    driver.findElement(By.cssSelector("a.submit.delete > span")).click();
	    driver.findElement(By.xpath("(//button[@type='button'])[4]")).click();
	    driver.findElement(By.xpath("//div[@id='navigation']/ul/li[8]/a/span")).click();
	    driver.findElement(By.cssSelector("a.cell.double > strong")).click();
	    driver.findElement(By.cssSelector("#product-3200 > td > a.cell.double > strong")).click();
	    driver.findElement(By.cssSelector("a.submit.delete > span")).click();
	    driver.findElement(By.xpath("(//button[@type='button'])[6]")).click();
	    driver.findElement(By.cssSelector("a.cell.double > strong")).click();
	    driver.findElement(By.cssSelector("a.submit.delete > span")).click();
	    driver.findElement(By.xpath("(//button[@type='button'])[8]")).click();
	    driver.findElement(By.cssSelector("li.active > a > span")).click();
	    driver.findElement(By.cssSelector("a.cell.double > strong")).click();
	    driver.findElement(By.cssSelector("a.submit.delete > span")).click();
	    driver.findElement(By.xpath("(//button[@type='button'])[4]")).click();
	    driver.findElement(By.cssSelector("span")).click();
	    driver.findElement(By.cssSelector("a.cell.double > strong")).click();
	    driver.findElement(By.cssSelector("a.submit.delete > span")).click();
	    driver.findElement(By.xpath("(//button[@type='button'])[2]")).click();
	    driver.findElement(By.cssSelector("span")).click();
	    driver.findElement(By.linkText("Logout")).click();*/
  }

  @AfterClass
  public void tearDown() throws Exception {
  //  driver.quit();
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
	  System.out.println("In isAlertPresent Method");
    try {
      driver.switchTo().alert();
      return true;
    } catch (NoAlertPresentException e) {
      return false;
    }
  }

  private String closeAlertAndGetItsText() {
	  System.out.println("IN closeAlertAndGetItsText Method");
    try {
      Alert alert = driver.switchTo().alert();
      System.out.println("Alert :: "+alert);
      String alertText = alert.getText();
      System.out.println("Alert text ::: "+alertText);
      if (acceptNextAlert) {
        alert.accept();
        System.out.println("IN IF");
      } else {
        alert.dismiss();
        System.out.println("In else");
      }
      return alertText;
    } finally {
      acceptNextAlert = true;
    }
  }
}
