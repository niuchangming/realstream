package modelVOs;

public class PaymentVO {
	public long lessonId;
	public String lessonTitle;
	public String transactionId;
	public double amount;
	public String currency;
	public String payDatetime;
	public String status;
	
	public PaymentVO(long lessonId, String lessonTitle, String transactionId, double amount, String currency,
			String payDatetime, String status) {
		super();
		this.lessonId = lessonId;
		this.lessonTitle = lessonTitle;
		this.transactionId = transactionId;
		this.amount = amount;
		this.currency = currency;
		this.payDatetime = payDatetime;
		this.status = status;
	}
	
}
