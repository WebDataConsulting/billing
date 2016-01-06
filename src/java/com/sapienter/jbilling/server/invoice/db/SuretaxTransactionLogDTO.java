package com.sapienter.jbilling.server.invoice.db;

import java.sql.Timestamp;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.TableGenerator;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

@Entity
@TableGenerator(name = "suretax_trans_GEN", table = "jbilling_seqs", pkColumnName = "name", valueColumnName = "next_id", pkColumnValue = "sure_tax_txn_log_seq", allocationSize = 100)
@Table(name = "sure_tax_txn_log")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class SuretaxTransactionLogDTO {
	@Id
	@GeneratedValue(strategy = GenerationType.TABLE, generator = "suretax_trans_GEN")
	@Column(name = "id", unique = true, nullable = false)
	private int id;
	@Column(name = "txn_id", unique = true, nullable = false)
	private String transactionId;
	@Column(name = "txn_type", unique = true, nullable = false)
	private String transactionType;
	@Column(name = "txn_data", unique = true, nullable = false)
	private String transactionData;
	@Column(name = "txn_date", unique = true, nullable = false)
	private Timestamp transactionDate;
	@Column(name = "resp_trans_id", unique = true, nullable = true)
	private Integer responseTransactionId;
	@Column(name = "request_type", unique = true, nullable = true)
	private String requestType;

	public SuretaxTransactionLogDTO() {
	}

	public SuretaxTransactionLogDTO(String transactionId,
			String transactionType, String transactionData,
			Timestamp transactionDate, Integer responseTransactionId,
			String requestType) {
		this.transactionId = transactionId;
		this.transactionType = transactionType;
		this.transactionData = transactionData;
		this.transactionDate = transactionDate;
		this.responseTransactionId = responseTransactionId;
		this.requestType = requestType;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getTransactionId() {
		return transactionId;
	}

	public void setTransactionId(String transactionId) {
		this.transactionId = transactionId;
	}

	public String getTransactionType() {
		return transactionType;
	}

	public void setTransactionType(String transactionType) {
		this.transactionType = transactionType;
	}

	public String getTransactionData() {
		return transactionData;
	}

	public void setTransactionData(String transactionData) {
		this.transactionData = transactionData;
	}

	public Timestamp getTransactionDate() {
		return transactionDate;
	}

	public void setTransactionDate(Timestamp transactionDate) {
		this.transactionDate = transactionDate;
	}

	public Integer getResponseTransactionId() {
		return responseTransactionId;
	}

	public void setResponseTransactionId(Integer responseTransactionId) {
		this.responseTransactionId = responseTransactionId;
	}

	public String getRequestType() {
		return requestType;
	}

	public void setRequestType(String requestType) {
		this.requestType = requestType;
	}

}
