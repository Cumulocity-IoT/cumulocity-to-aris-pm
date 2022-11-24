package c8y.to.aris.data.continuous.ms.rest.model;

import java.util.List;

public class SourceTable {
	private String name = null;
	private String namespace = null;
	private List<SourceTableColumn> columns = null;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	public List<SourceTableColumn> getColumns() {
		return columns;
	}

	public void setColumns(List<SourceTableColumn> columns) {
		this.columns = columns;
	}

}
