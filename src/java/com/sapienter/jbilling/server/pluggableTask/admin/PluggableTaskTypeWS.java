package com.sapienter.jbilling.server.pluggableTask.admin;

/**
 * 
 * @author Khurram Cheema
 *
 */
public class PluggableTaskTypeWS implements java.io.Serializable{

	private Integer id;
	private String className;
	private Integer minParameters;
	private Integer categoryId;
	
	public PluggableTaskTypeWS(){
		
	}
	
	@Override
	public String toString(){
		return "PluggableTaskTypeWS [id: "+this.id+",className: "+this.className
				+", minParameters: "+this.minParameters
				+", categoryId: "+this.categoryId+" ]";
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getClassName() {
		return className;
	}

	public void setClassName(String className) {
		this.className = className;
	}

	public Integer getMinParameters() {
		return minParameters;
	}

	public void setMinParameters(Integer minParameters) {
		this.minParameters = minParameters;
	}

	public Integer getCategoryId() {
		return categoryId;
	}

	public void setCategoryId(Integer categoryId) {
		this.categoryId = categoryId;
	}
}
