package com.example.tests;

import java.util.regex.Pattern;
import java.util.concurrent.TimeUnit;
import org.junit.*;
import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;
import org.openqa.selenium.*;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.support.ui.Select;

public class CreatingOrderOfDifferentPeriods {
  private WebDriver driver;
  private String baseUrl;
  private boolean acceptNextAlert = true;
  private StringBuffer verificationErrors = new StringBuffer();
  private String str = new String();

  @Before
  public void setUp() throws Exception {
	  baseUrl = "http://localhost:8080/jbilling";
	  driver = Instance.getInstance();
	  //Instance.initialize(driver);
  }

  @Test
  public void testS111() throws Exception {
	    
	    //Creating two customers
	    driver.get(baseUrl + "/customer/list");
	    driver.findElement(By.cssSelector("span")).click();
	    driver.findElement(By.linkText("ADD NEW")).click();
	    new Select(driver.findElement(By.id("accountTypeId"))).selectByVisibleText("Direct Customer");
	    driver.findElement(By.linkText("SELECT")).click();
	    driver.findElement(By.id("user.userName")).clear();
	    driver.findElement(By.id("user.userName")).sendKeys("Billing Customer 1");
	    driver.findElement(By.linkText("SAVE CHANGES")).click();
	    driver.findElement(By.cssSelector("div.btn-box > a.submit.add > span")).click();
	    new Select(driver.findElement(By.id("accountTypeId"))).selectByVisibleText("Direct Customer");
	    driver.findElement(By.linkText("SELECT")).click();
	    driver.findElement(By.id("user.userName")).clear();
	    driver.findElement(By.id("user.userName")).sendKeys("Billing Customer 2");
	    driver.findElement(By.id("user.dueDateValue")).clear();
	    driver.findElement(By.id("user.dueDateValue")).sendKeys("15");
	    driver.findElement(By.cssSelector("li > a.submit.save > span")).click();
	    
	    Thread.sleep(2000);
	    
	    //Creating new category of products
	    driver.findElement(By.xpath("//ul[@id='navList']/li[9]/a/span")).click();
	    driver.findElement(By.linkText("ADD CATEGORY")).click();
	    
	    driver.findElement(By.id("description")).clear();
	    driver.findElement(By.id("description")).sendKeys("Billing Category");
	    new Select(driver.findElement(By.id("company-select"))).selectByVisibleText("WDT Jaipur");
	    driver.findElement(By.linkText("SAVE CHANGES")).click();
	    
	    Thread.sleep(1000);
	    
	    //creating product1
	    driver.findElement(By.linkText("Products")).click();
	    driver.findElement(By.cssSelector("td.small > a.cell > span")).click();
	    driver.findElement(By.cssSelector("#column2 > div.btn-box > a.submit.add > span")).click();
	    driver.findElement(By.cssSelector("img[alt=\"remove\"]")).click();
	    driver.findElement(By.id("product.descriptions[0].content")).clear();
	    driver.findElement(By.id("product.descriptions[0].content")).sendKeys("Billing Flat");
	    driver.findElement(By.id("product.number")).clear();
	    driver.findElement(By.id("product.number")).sendKeys("B_F");
	    driver.findElement(By.id("prices.1")).clear();
	    driver.findElement(By.id("prices.1")).sendKeys("20");
	    driver.findElement(By.cssSelector("a.submit.save > span")).click();
	    
	    Thread.sleep(2000);
	    
	    //creating product2
	    driver.findElement(By.linkText("Products")).click();
	    driver.findElement(By.cssSelector("td.small > a.cell > span")).click();
	    driver.findElement(By.cssSelector("#column2 > div.btn-box > a.submit.add > span")).click();
	    driver.findElement(By.cssSelector("img[alt=\"remove\"]")).click();
	    driver.findElement(By.id("product.descriptions[0].content")).clear();
	    driver.findElement(By.id("product.descriptions[0].content")).sendKeys("Billing Graduated");
	    driver.findElement(By.id("product.number")).clear();
	    driver.findElement(By.id("product.number")).sendKeys("B_G");
	    driver.findElement(By.id("prices.1")).clear();
	    driver.findElement(By.id("prices.1")).sendKeys("5");
	    driver.findElement(By.cssSelector("a.submit.save > span")).click();
	    
	    Thread.sleep(2000);
	    
	    //creating order to the customer "BILLING CUSTOMER2" with Billing Graduated 
	    driver.findElement(By.linkText("Customers")).click();
	    driver.findElement(By.linkText("Billing Customer 2")).click();
	    driver.findElement(By.linkText("CREATE ORDER")).click();
	    driver.findElement(By.id("activeSince")).clear();
	    driver.findElement(By.id("activeSince")).sendKeys("01/01/2001");
	    driver.findElement(By.xpath("//form[@id='order-details-form']/div/div[5]")).click();
	    driver.findElement(By.id("ui-id-8")).click();
	    driver.findElement(By.cssSelector("a.cell.double > strong")).click();
	    driver.findElement(By.id("change--3.quantityAsDecimal")).click();
	    driver.findElement(By.id("change--3.quantityAsDecimal")).clear();
	    driver.findElement(By.id("change--3.quantityAsDecimal")).sendKeys("14");
	    driver.findElement(By.id("change--3.startDate")).click();
	    driver.findElement(By.id("change--3.startDate")).clear();
	    driver.findElement(By.id("change--3.startDate")).sendKeys("01/01/2001");
	    driver.findElement(By.xpath("//form[@id='change--3-update-form']/div/div/div[5]")).click();
	    driver.findElement(By.cssSelector("#change--3-update-form > div.btn-box > a.submit.save > span")).click();
	    driver.findElement(By.linkText("SAVE CHANGES")).click();
	    
	    str = driver.findElement(By.cssSelector("div.msg-box.successfully >p")).getText();
	    System.out.println(str+" Assertion 1 ");
	    Assert.assertEquals(driver.getPageSource().contains("str"), true);
	    
	    Thread.sleep(1000);
	    
	    //creating order to the customer "BILLING CUSTOMER2" with Billing Flat
	    driver.findElement(By.linkText("Customers")).click();
	    driver.findElement(By.linkText("Billing Customer 2")).click();
	    driver.findElement(By.linkText("CREATE ORDER")).click();
	    new Select(driver.findElement(By.id("orderPeriod"))).selectByVisibleText("Monthly");
	    //driver.findElement(By.cssSelector("option[value=\"200\"]")).click();
	    driver.findElement(By.id("billingTypeId")).click();
	    new Select(driver.findElement(By.id("billingTypeId"))).selectByVisibleText("pre paid");
	    //driver.findElement(By.cssSelector("#billingTypeId > option[value=\"1\"]")).click();
	    driver.findElement(By.id("activeSince")).clear();
	    driver.findElement(By.id("activeSince")).sendKeys("01/01/2001");
	    driver.findElement(By.xpath("//form[@id='order-details-form']/div/div[5]")).click();
	    driver.findElement(By.id("ui-id-8")).click();
	    driver.findElement(By.xpath("//strong[contains(.,'Billing Flat')]")).click();//xpath....==========>
	    driver.findElement(By.id("change--3.quantityAsDecimal")).click();
	    driver.findElement(By.id("change--3.quantityAsDecimal")).click();
	    driver.findElement(By.id("change--3.quantityAsDecimal")).clear();
	    driver.findElement(By.id("change--3.quantityAsDecimal")).sendKeys("3");
	    driver.findElement(By.id("change--3.startDate")).click();
	    driver.findElement(By.id("change--3.startDate")).clear();
	    driver.findElement(By.id("change--3.startDate")).sendKeys("01/01/2001");
	    driver.findElement(By.xpath("//form[@id='change--3-update-form']/div/div/div[4]")).click();
	    driver.findElement(By.cssSelector("#change--3-update-form > div.btn-box > a.submit.save > span")).click();
	    driver.findElement(By.linkText("SAVE CHANGES")).click();
	    
	    str = driver.findElement(By.cssSelector("div.msg-box.successfully >p")).getText();
	    System.out.println(str+" Assertion 2 ");
	    Assert.assertEquals(driver.getPageSource().contains("str"), true);
	    
	    Thread.sleep(1000);
	    
	  //creating order to the customer "BILLING CUSTOMER1" with Billing Flat
	    driver.findElement(By.linkText("Customers")).click();
	    driver.findElement(By.linkText("Billing Customer 1")).click();
	    driver.findElement(By.linkText("CREATE ORDER")).click();
	    new Select(driver.findElement(By.id("orderPeriod"))).selectByVisibleText("Monthly");
	    //driver.findElement(By.cssSelector("option[value=\"200\"]")).click();
	    driver.findElement(By.id("billingTypeId")).click();
	    new Select(driver.findElement(By.id("billingTypeId"))).selectByVisibleText("post paid");
	    //driver.findElement(By.cssSelector("#billingTypeId > option[value=\"1\"]")).click();
	    driver.findElement(By.id("activeSince")).clear();
	    driver.findElement(By.id("activeSince")).sendKeys("01/01/2001");
	    driver.findElement(By.xpath("//form[@id='order-details-form']/div/div[5]")).click();
	    driver.findElement(By.id("ui-id-8")).click();
	    driver.findElement(By.cssSelector("a.cell.double > strong")).click();
	    driver.findElement(By.id("change--3.quantityAsDecimal")).click();
	    driver.findElement(By.id("change--3.quantityAsDecimal")).click();
	    driver.findElement(By.id("change--3.quantityAsDecimal")).clear();
	    driver.findElement(By.id("change--3.quantityAsDecimal")).sendKeys("14");
	    driver.findElement(By.id("change--3.startDate")).click();
	    driver.findElement(By.id("change--3.startDate")).clear();
	    driver.findElement(By.id("change--3.startDate")).sendKeys("01/01/2001");
	    driver.findElement(By.xpath("//form[@id='change--3-update-form']/div/div/div[4]")).click();
	    driver.findElement(By.cssSelector("#change--3-update-form > div.btn-box > a.submit.save > span")).click();
	    driver.findElement(By.linkText("SAVE CHANGES")).click();
	    
	    str = driver.findElement(By.cssSelector("div.msg-box.successfully >p")).getText();
	    System.out.println(str+" Assertion 3 ");
	    Assert.assertEquals(driver.getPageSource().contains("str"), true);
	    
	    Thread.sleep(1000);
	    
	    //creating order to the customer "BILLING CUSTOMER1" with Billing Graduated
	    driver.findElement(By.linkText("Customers")).click();
	    driver.findElement(By.linkText("Billing Customer 1")).click();
	    driver.findElement(By.linkText("CREATE ORDER")).click();
	    new Select(driver.findElement(By.id("orderPeriod"))).selectByVisibleText("Monthly");
	    //driver.findElement(By.cssSelector("option[value=\"200\"]")).click();
	    driver.findElement(By.id("billingTypeId")).click();
	    new Select(driver.findElement(By.id("billingTypeId"))).selectByVisibleText("post paid");
	    //driver.findElement(By.cssSelector("#billingTypeId > option[value=\"1\"]")).click();
	    driver.findElement(By.id("activeSince")).clear();
	    driver.findElement(By.id("activeSince")).sendKeys("01/01/2001");
	    driver.findElement(By.xpath("//form[@id='order-details-form']/div/div[5]")).click();
	    driver.findElement(By.id("ui-id-8")).click();
	    driver.findElement(By.xpath("//strong[contains(.,'Billing Flat')]")).click();
	    driver.findElement(By.id("change--3.quantityAsDecimal")).click();
	    driver.findElement(By.id("change--3.quantityAsDecimal")).click();
	    driver.findElement(By.id("change--3.quantityAsDecimal")).clear();
	    driver.findElement(By.id("change--3.quantityAsDecimal")).sendKeys("2");
	    driver.findElement(By.id("change--3.startDate")).click();
	    driver.findElement(By.id("change--3.startDate")).clear();
	    driver.findElement(By.id("change--3.startDate")).sendKeys("01/01/2001");
	    driver.findElement(By.xpath("//form[@id='change--3-update-form']/div/div/div[4]")).click();
	    driver.findElement(By.cssSelector("#change--3-update-form > div.btn-box > a.submit.save > span")).click();
	    driver.findElement(By.linkText("SAVE CHANGES")).click();
	    
	    str = driver.findElement(By.cssSelector("div.msg-box.successfully >p")).getText();
	    System.out.println(str+" Assertion 4 ");
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