package c8y.to.aris.tbl.creation.ms.integration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.stereotype.Component;

import com.cumulocity.rest.representation.inventory.ManagedObjectRepresentation;
import com.cumulocity.rest.representation.measurement.MeasurementRepresentation;

import c8y.to.aris.tbl.creation.ms.rest.model.FullyQualifiedName;
import c8y.to.aris.tbl.creation.ms.rest.model.IngestionCycleRequest;
import c8y.to.aris.tbl.creation.ms.rest.model.ReadyForIngestionRequest;
import c8y.to.aris.tbl.creation.ms.rest.model.SourceTable;
import c8y.to.aris.tbl.creation.ms.rest.model.SourceTableColumn;


@Component
public class ArisDatasetManager {

	public static String DATASET_NAMESPACE = "default";
	public static String ACTIVITY_TABLE_NAME = "cumulocityMeasurements_ActivityTable";
	public static String ENHANCEMENT_TABLE_NAME = "cumulocityDevicesDetails_EnhancementTable";

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

	public ReadyForIngestionRequest getIngestionRequest() 
	{
		FullyQualifiedName fqn1 = new FullyQualifiedName();
		fqn1.setFullyQualifiedName(DATASET_NAMESPACE + "." + ACTIVITY_TABLE_NAME);
		FullyQualifiedName fqn2 = new FullyQualifiedName();
		fqn2.setFullyQualifiedName(DATASET_NAMESPACE + "." + ENHANCEMENT_TABLE_NAME);
		
		List<FullyQualifiedName> fqns = new ArrayList<FullyQualifiedName>();
		fqns.add(fqn1);
		fqns.add(fqn2);
		
		ReadyForIngestionRequest rfi = new ReadyForIngestionRequest();
		rfi.setDataUploadTargets(fqns);
		
		return rfi;
	}
	
	public IngestionCycleRequest getIngestionCycleRequest() 
	{
		FullyQualifiedName fqn1 = new FullyQualifiedName();
		fqn1.setFullyQualifiedName(DATASET_NAMESPACE + "." + ACTIVITY_TABLE_NAME);
		FullyQualifiedName fqn2 = new FullyQualifiedName();
		fqn2.setFullyQualifiedName(DATASET_NAMESPACE + "." + ENHANCEMENT_TABLE_NAME);
		
		List<FullyQualifiedName> fqns = new ArrayList<FullyQualifiedName>();
		fqns.add(fqn1);
		fqns.add(fqn2);
		
		IngestionCycleRequest icr = new IngestionCycleRequest();
		icr.setDataUploadTargets(fqns);
		
		return icr;
	}
	
	
	public List<List<Object>> buildActivityData(MeasurementRepresentation measurement)
	{
		List<List<Object>> csvLinesCombined = new ArrayList<List<Object>>();
		List<Object> csvLine = new ArrayList<Object>();

		Map<String,Object> fragments = measurement.getAttrs();
		boolean foundSeries = false;
		List<MeasurementSerieRepresentation> allSeries = new ArrayList<MeasurementSerieRepresentation>();

		//search for the measurement serie, unit and value
		//This code could be a lot cleaner if you know for which measurement name and serie you are particularly interested
		//here in order to make it generic for all customers, we need to go trough all the fragments to find the measurement itself.
		for (String fragmentName : fragments.keySet())
		{
			if (fragments.get(fragmentName) instanceof Map<?, ?>) {
				Map<?, ?> potentialMeasurementFragment = (Map<?, ?>) fragments.get(fragmentName);
				for (Object serie : potentialMeasurementFragment.keySet())
				{
					if (serie instanceof String && potentialMeasurementFragment.get(serie) instanceof Map<?,?>)
					{
						Map<?, ?> potentialMeasurementSerie = (Map<?, ?>) potentialMeasurementFragment.get(serie);
						if (potentialMeasurementSerie.containsKey("unit") && potentialMeasurementSerie.containsKey("value"))
						{
							foundSeries = true;
							MeasurementSerieRepresentation msr = new MeasurementSerieRepresentation();
							msr.setMeasurementName(fragmentName);
							msr.setSerieName(serie.toString()); 
							msr.setMeasurementValue(Double.parseDouble(potentialMeasurementSerie.get("value").toString()));
							msr.setMeasurementUnit(potentialMeasurementSerie.get("unit").toString()); 
							allSeries.add(msr);
						}
					}
				}
				if (foundSeries) {
					break;
				}
			}
		}
		if (allSeries.size() == 0 ) {
			//did not found the measurement serie, we return an empty line so it can be ignored
			return csvLinesCombined;
		}
		//We found the measurement serie, we build the csv line for Aris data upload
		//activity table format is DeviceId, MeasurementType, MeasurementTime, MeasurementValue, MeasurementUnit
		for (MeasurementSerieRepresentation msr : allSeries)
		{
			csvLine.clear();
			csvLine.add(measurement.getSource().getId().getValue());
			csvLine.add(msr.getMeasurementName() + "." + msr.getSerieName());
			 
			DateTimeFormatter fmt = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSS");
			csvLine.add(fmt.print(measurement.getDateTime()));
			csvLine.add(msr.getMeasurementValue());
			csvLine.add(msr.getMeasurementUnit());
			csvLinesCombined.add(csvLine);
		}
		return csvLinesCombined;
	}
	
	public List<Object> buildEnhancementData(ManagedObjectRepresentation device)
	{
		List<Object> csvLine = new ArrayList<Object>();

		csvLine.add(device.getId().getValue());
		csvLine.add(device.getType());
		csvLine.add(device.getName());
		if (device.hasProperty("c8y_Address") && device.get("c8y_Address") instanceof Map<?,?>) {
			Map<?,?> address = (Map<?,?>) device.get("c8y_Address");
			if (address.containsKey("city")) {
				csvLine.add(address.get("city").toString());
			} else
			{
				csvLine.add("");
			}
		} else
		{
			csvLine.add("");
		}
		if (device.hasProperty("c8y_ActiveAlarmsStatus") && device.get("c8y_ActiveAlarmsStatus") instanceof Map<?,?>) {
			Map<?,?> alarms = (Map<?,?>) device.get("c8y_ActiveAlarmsStatus");
			if (alarms.containsKey("major")) {
				csvLine.add(Long.parseLong(alarms.get("major").toString()));
			} else
			{
				csvLine.add(0);
			}
			if (alarms.containsKey("critical")) {
				csvLine.add(Long.parseLong(alarms.get("critical").toString()));
			} else
			{
				csvLine.add(0);
			}
		} else
		{
			csvLine.add(0);
			csvLine.add(0);
		}
		return csvLine;
	}
	
	public String getFullyQualifiedNameActivityTable() {
		return DATASET_NAMESPACE + "." + ACTIVITY_TABLE_NAME;
	}
	
	public String getFullyQualifiedNameEnhancementTable() {
		return DATASET_NAMESPACE + "." + ENHANCEMENT_TABLE_NAME;
	}

}
