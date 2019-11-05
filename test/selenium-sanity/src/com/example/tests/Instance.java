package com.example.tests;

import java.util.concurrent.TimeUnit;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.support.ui.Select;

public class Instance {
	
		   private static WebDriver instance = null;
		   public static final String BASE_URL = "http://localhost:8080/jbilling";
		   private Instance() {
		   }
		   public static WebDriver getInstance() {
		      if(instance == null) {
		         instance = new FirefoxDriver();
		      }
		      return instance;
		   }
		   
		   public static void initialize(WebDriver instance){
			
			    instance.manage().timeouts().implicitlyWait(30, TimeUnit.SECONDS);
			    
			    //logging in
			    instance.get(BASE_URL + "/login/auth");
			    instance.findElement(By.id("j_username")).clear();
			    instance.findElement(By.id("j_username")).sendKeys("admin");
			    instance.findElement(By.id("j_password")).clear();
			    instance.findElement(By.id("j_password")).sendKeys("Admin111@");
			    new Select(instance.findElement(By.id("j_client_id"))).selectByVisibleText("WDT Jaipur");
			    instance.findElement(By.cssSelector("a.submit.save > span")).click(); 
			    
		   }
		   
		}
