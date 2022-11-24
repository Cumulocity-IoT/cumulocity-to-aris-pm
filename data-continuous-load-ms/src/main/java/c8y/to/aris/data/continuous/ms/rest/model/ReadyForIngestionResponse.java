package c8y.to.aris.data.continuous.ms.rest.model;

public class ReadyForIngestionResponse {
	private boolean ready;
	private FailureCause cause;
	
	public boolean isReady() {
		return ready;
	}
	public void setReady(boolean ready) {
		this.ready = ready;
	}
	public FailureCause getCause() {
		return cause;
	}
	public void setCause(FailureCause cause) {
		this.cause = cause;
	}
	
}
