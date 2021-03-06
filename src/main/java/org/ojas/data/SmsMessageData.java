package org.ojas.data;

/**
 * This class was automatically generated by the data modeler tool.
 */

@javax.persistence.Entity
@javax.xml.bind.annotation.XmlRootElement
public class SmsMessageData implements java.io.Serializable {

	static final long serialVersionUID = 1L;

	@javax.persistence.GeneratedValue(strategy = javax.persistence.GenerationType.AUTO, generator = "SMSMESSAGEDATA_ID_GENERATOR")
	@javax.persistence.Id
	@javax.persistence.SequenceGenerator(sequenceName = "SMSMESSAGEDATA_ID_SEQ", name = "SMSMESSAGEDATA_ID_GENERATOR")
	private java.lang.Long id;

	private java.lang.String msisdn;

	private java.lang.String smsMessage;

	public SmsMessageData() {
	}

	public java.lang.Long getId() {
		return this.id;
	}

	public void setId(java.lang.Long id) {
		this.id = id;
	}

	public java.lang.String getMsisdn() {
		return this.msisdn;
	}

	public void setMsisdn(java.lang.String msisdn) {
		this.msisdn = msisdn;
	}

	public java.lang.String getSmsMessage() {
		return this.smsMessage;
	}

	public void setSmsMessage(java.lang.String smsMessage) {
		this.smsMessage = smsMessage;
	}

	public SmsMessageData(java.lang.Long id, java.lang.String msisdn,
			java.lang.String smsMessage) {
		this.id = id;
		this.msisdn = msisdn;
		this.smsMessage = smsMessage;
	}

}