package com.sapienter.jbilling.server.util;



import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;


public class LanguageWS implements Serializable {

    private Integer id;
    @NotNull(message = "validation.error.notnull")
    private String description;
    @NotNull(message = "validation.error.notnull")
    @Size(min = 2, max = 2, message = "validation.error.size.exact,2")
    private String code;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
