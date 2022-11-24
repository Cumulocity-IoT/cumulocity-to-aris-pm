package c8y.to.aris.tbl.creation.ms.rest.model;

public class CycleState {
	private String value = null;
	private Boolean successful = null;
	private FailureCause cause = null;
	
	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public FailureCause getCause() {
		return cause;
	}

	public void setCause(FailureCause cause) {
		this.cause = cause;
	}

	public Boolean getSuccessful() {
		return successful;
	}

	public void setSuccessful(Boolean successful) {
		this.successful = successful;
	}
	
	
	
}
