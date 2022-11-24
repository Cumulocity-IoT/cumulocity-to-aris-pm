package c8y.to.aris.data.continuous.ms.rest.model;

public class SourceTableColumn {
	private String dataType = null;

	private String name = null;
	
	private String format = null;

	public String getDataType() {
		return dataType;
	}

	public void setDataType(String datatype) {
		this.dataType = datatype;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getFormat() {
		return format;
	}

	public void setFormat(String format) {
		this.format = format;
	}

}
