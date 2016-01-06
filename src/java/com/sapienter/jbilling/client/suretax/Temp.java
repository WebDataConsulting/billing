package com.sapienter.jbilling.client.suretax;

import java.io.File;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;

public class Temp {
	public static void main(String[] args) throws Exception {
		List<String> codes = FileUtils.readLines(new File(
				"C:/jbilling/suretax/codes.txt"));
		String s = 
				"       <insert tableName=\"ENUMERATION_VALUES\">\r\n" + 
				"            <column name=\"ID\" valueComputed=\"(select max(id) + 1 from enumeration_values)\"/>\r\n" + 
				"            <column name=\"ENUMERATION_ID\" valueNumeric=\"25\"/>\r\n" + 
				"            <column name=\"VALUE\" value=\"BLAHBLAH\"/>\r\n" + 
				"            <column name=\"OPTLOCK\" valueNumeric=\"0\"/>\r\n" + 
				"        </insert>";
		for (String code : codes) {
			String tmp = s.replace("BLAHBLAH", code);
			System.out.println(tmp);
		}

	}
}
