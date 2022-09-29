package c8y.to.aris.ms.rest.model;

public class CycleState {
	private String value;
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
	
	
}
