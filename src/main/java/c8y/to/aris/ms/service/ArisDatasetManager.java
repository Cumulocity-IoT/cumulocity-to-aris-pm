package c8y.to.aris.ms.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import c8y.to.aris.ms.rest.model.SourceTable;
import c8y.to.aris.ms.rest.model.SourceTableColumn;

@Component
public class ArisDatasetManager {
	
	public static String DATASET_NAMESPACE = "default";
	public static String ACTIVITY_TABLE_NAME = "cumulocityMeasurements";
	public static String ENHANCEMENT_TABLE_NAME = "cumulocityDevicesDetails";
	
	public SourceTable createActivitySourceTableStructure()
	{
		SourceTable activitySourceTable = new SourceTable();
		List<SourceTableColumn> columns = new ArrayList<SourceTableColumn>();
		
		SourceTableColumn stbCaseId = new SourceTableColumn();
		stbCaseId.setDataType("STRING");
		stbCaseId.setName("DeviceId");
		columns.add(stbCaseId);
		
		SourceTableColumn stbFunction = new SourceTableColumn();
		stbFunction.setDataType("STRING");
		stbFunction.setName("MeasurementType");
		columns.add(stbFunction);
		
		SourceTableColumn stbTime = new SourceTableColumn();
		stbTime.setDataType("FORMATTED_TIMESTAMP");
		stbTime.setName("Time");
		stbTime.setFormat("yyyy-MM-dd HH:mm:ss.SSS");
		columns.add(stbTime);
		
		SourceTableColumn stbValue = new SourceTableColumn();
		stbValue.setDataType("DOUBLE");
		stbValue.setName("MeasurementValue");
		columns.add(stbValue);
		
		SourceTableColumn stbUnit = new SourceTableColumn();
		stbUnit.setDataType("STRING");
		stbUnit.setName("MeasurementUnit");
		columns.add(stbUnit);
		
		activitySourceTable.setColumns(columns);
		activitySourceTable.setName(ACTIVITY_TABLE_NAME);
		activitySourceTable.setNamespace(DATASET_NAMESPACE);

		return activitySourceTable;
	}
	
	public SourceTable createEnhancementSourceTableStructure()
	{
		SourceTable enhancementSourceTable = new SourceTable();
		List<SourceTableColumn> columns = new ArrayList<SourceTableColumn>();
		
		SourceTableColumn stbCaseId = new SourceTableColumn();
		stbCaseId.setDataType("STRING");
		stbCaseId.setName("DeviceId");
		columns.add(stbCaseId);
		
		SourceTableColumn stbType = new SourceTableColumn();
		stbType.setDataType("STRING");
		stbType.setName("DeviceType");
		columns.add(stbType);
		
		SourceTableColumn stbName = new SourceTableColumn();
		stbName.setDataType("STRING");
		stbName.setName("DeviceName");
		columns.add(stbName);
		
		SourceTableColumn stbCity = new SourceTableColumn();
		stbCity.setDataType("STRING");
		stbCity.setName("City");
		columns.add(stbCity);
	
		SourceTableColumn stbMajorAlarms = new SourceTableColumn();
		stbMajorAlarms.setDataType("LONG");
		stbMajorAlarms.setName("MajorAlarms");
		columns.add(stbMajorAlarms);
		
		SourceTableColumn stbCritAlarms = new SourceTableColumn();
		stbCritAlarms.setDataType("LONG");
		stbCritAlarms.setName("CriticalAlarms");
		columns.add(stbCritAlarms);
		
		enhancementSourceTable.setColumns(columns);
		enhancementSourceTable.setName(ENHANCEMENT_TABLE_NAME);
		enhancementSourceTable.setNamespace(DATASET_NAMESPACE);

		return enhancementSourceTable;
	}
}
