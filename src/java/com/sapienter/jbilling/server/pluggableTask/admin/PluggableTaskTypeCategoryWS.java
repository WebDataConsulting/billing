package com.sapienter.jbilling.server.pluggableTask.admin;

import java.io.Serializable;

/**
 * 
 * @author Khurram Cheema
 *
 */
public class PluggableTaskTypeCategoryWS implements Serializable{
	
	private Integer id;
	private String interfaceName;
	
	public PluggableTaskTypeCategoryWS(){
		
	}
	
	public Integer getId(){
		return this.id;
	}
	
	public void setId(Integer id){
		this.id = id;
	}
	
	public String getInterfaceName(){
		return this.interfaceName;
	}
	
	public void setInterfaceName(String interfaceName){
		this.interfaceName = interfaceName;
	}
	
	@Override
	public String toString(){
		return "PluggableTaskTypeCategoryWS = [id: "+this.id
				+", interfaceName: "+this.interfaceName+" ]";
	}
}
