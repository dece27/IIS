package iis.spacenode;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.position.NmeaPosition;
import org.eclipse.kura.position.PositionService;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SpaceNode implements ConfigurableComponent  
{	
	private static final Logger s_logger = LoggerFactory.getLogger(SpaceNode.class);
	private static final String APP_ID = "IIS-SpaceNode";

	//GPS
	private PositionService             m_positionService;
	private NmeaPosition				position;
	private double 						latitude, longitude, altitude;

	private ScheduledExecutorService    m_worker;
	private ScheduledFuture<?>          m_handle;

	//Properties
	private Map<String, Object>         m_properties;
	private static final String   		SAMPLING_RATE_PROP_NAME   = "sampling.rate";

	//SHT31
	//	private SHT31						dev_SHT31;
	private double						SHT31_t, SHT31_h;

	//BMP280
	private BMP280						dev_BMP280;
	private double						BMP280_t, BMP280_p, BMP280_a;

	// ----------------------------------------------------------------
	//
	//   Dependencies
	//
	// ----------------------------------------------------------------

	public SpaceNode() 
	{
		super();
		m_worker = Executors.newSingleThreadScheduledExecutor();
	}

	public void setPositionService(PositionService positionService) {
		m_positionService = positionService;
	}

	public void unsetPositionService(PositionService positionService) {
		m_positionService = null;
	}


	// ----------------------------------------------------------------
	//
	//   Activation APIs
	//
	// ----------------------------------------------------------------

	protected void activate(ComponentContext componentContext, Map<String,Object> properties) 
	{
		s_logger.info("Activating " + APP_ID + "...");

		m_properties = properties;
		for (String s : properties.keySet()) {
			s_logger.info("Activate - "+s+": "+properties.get(s));
		}
		try  {
			//			dev_SHT31 = new SHT31();
			//			dev_SHT31.heater(true);
			dev_BMP280 = new BMP280();
			//			position = m_positionService.getNmeaPosition();
			//			dev_BMP280.calcSeaLevelhPa(position.getAltitude());
			doUpdate();
		}
		catch (Exception e) {
			s_logger.error("Error during component activation", e);
			try {
				dev_BMP280.close();
				//				dev_SHT31.close();
			} catch (Exception e1) {
				e1.printStackTrace();
				s_logger.error("Error during devices closing", e1);
			}
			throw new ComponentException(e);
		}
		s_logger.info("Activating " + APP_ID + "...Done.");
	}


	protected void deactivate(ComponentContext componentContext) 
	{
		s_logger.info("Deactivating " + APP_ID + "...");

		//				try {
		//					dev_SHT31.heater(false);
		//				} catch (Exception e) {
		//					e.printStackTrace();
		//					s_logger.error("Error disabling SHT31-D Heater", e);
		//				}
		try {
			dev_BMP280.close();
			//			dev_SHT31.close();
		} catch (Exception e) {
			e.printStackTrace();
			s_logger.error("Error during devices closing", e);
		}
		m_worker.shutdown();

		s_logger.info("Deactivating " + APP_ID + "...Done.");
	}	


	public void updated(Map<String,Object> properties)
	{
		s_logger.info("Updated " + APP_ID + "...");

		m_properties = properties;
		for (String s : properties.keySet()) {
			s_logger.info("Update - "+s+": "+properties.get(s));
		}

		doUpdate();
		s_logger.info("Updated " + APP_ID + "...Done.");
	}


	// ----------------------------------------------------------------
	//
	//   Private Methods
	//
	// ----------------------------------------------------------------


	private void doUpdate() 
	{
		if (m_handle != null) {
			m_handle.cancel(true);
		}

		if (!m_properties.containsKey(SAMPLING_RATE_PROP_NAME)) {
			s_logger.info("Update " + APP_ID + " - Ignore as properties do not contain SAMPLING_RATE_PROP_NAME.");
			return;
		}

		int sampling = (Integer) m_properties.get(SAMPLING_RATE_PROP_NAME);
		m_handle = m_worker.scheduleAtFixedRate(new Runnable() {		
			@Override
			public void run() {
				Thread.currentThread().setName(getClass().getSimpleName());
				doSample();
			}
		}, 0, sampling, TimeUnit.SECONDS);
	}


	private void doSample() 
	{
		//GPS
		//		position = m_positionService.getNmeaPosition();
		//		latitude = position.getLatitude();
		//		longitude = position.getLongitude();
		//		altitude = position.getAltitude();
		//		System.out.println("GPS: Lat = " + latitude + " deg, Lon = " + longitude + " deg, Alt = " + altitude + " m");

		//SHT31-D
		try {
			//			SHT31_t = dev_SHT31.getTemperature();
			//			SHT31_h = dev_SHT31.getHumidity();
			ProcessBuilder pb = new ProcessBuilder("/usr/bin/python", "/home/pi/sht31.py");
			Process p = pb.start();
			BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
			String ret[] = in.readLine().trim().split(";");
			in.close();
			SHT31_t = Double.parseDouble(ret[0]);
			SHT31_h = Double.parseDouble(ret[1]);
			System.out.println("SHT31: Temp = " + SHT31_t + " °C, Hum = " + SHT31_h + " %");
		} catch(Exception e) {
			e.printStackTrace();
			s_logger.error("Error during SHT31-D reading", e);
		}

		//BMP280
		try {
			BMP280_t = dev_BMP280.getTemperature();
			BMP280_p = dev_BMP280.getPressure();
			BMP280_a = dev_BMP280.getAltitude();
			System.out.println("BMP280: Temp = " + BMP280_t + " °C, Press = " + BMP280_p + " Pa, Alt = " + BMP280_a + " m");
		} catch(Exception e) {
			e.printStackTrace();
			s_logger.error("Error during BMP280 reading", e);
			try {
				dev_BMP280.close();
			} catch (Exception e1) {
				e1.printStackTrace();
				s_logger.error("Error during BMP280 closing", e1);
			}
		}
	}
}
