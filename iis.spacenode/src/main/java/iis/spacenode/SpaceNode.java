package iis.spacenode;

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

	private Map<String, Object>         m_properties;

	//SHT31
	private SHT31						dev_SHT31;
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
			dev_SHT31 = new SHT31();
			dev_BMP280 = new BMP280();
			doUpdate(false);
		}
		catch (Exception e) {
			s_logger.error("Error during component activation", e);
			throw new ComponentException(e);
		}
		s_logger.info("Activating " + APP_ID + "...Done.");
	}


	protected void deactivate(ComponentContext componentContext) 
	{
		s_logger.info("Deactivating " + APP_ID + "...");

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

		doUpdate(true);
		s_logger.info("Updated " + APP_ID + "...Done.");
	}


	// ----------------------------------------------------------------
	//
	//   Private Methods
	//
	// ----------------------------------------------------------------


	private void doUpdate(boolean onUpdate) 
	{
		if (m_handle != null) {
			m_handle.cancel(true);
		}

		m_handle = m_worker.scheduleAtFixedRate(new Runnable() {		
			@Override
			public void run() {
				Thread.currentThread().setName(getClass().getSimpleName());
				doSample();
			}
		}, 0, 10, TimeUnit.SECONDS); //TODO: Change interval
	}


	private void doSample() 
	{
		//GPS
		position = m_positionService.getNmeaPosition();
		latitude = position.getLatitude();
		longitude = position.getLongitude();
		altitude = position.getAltitude();
		System.out.println("GPS: Lat = " + latitude + " deg, Lon = " + longitude + " deg, Alt = " + altitude + " m");
		
		//SHT31-D
		try {
			SHT31_t = dev_SHT31.getTemperature();
			SHT31_h = dev_SHT31.getHumidity();
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
		}
	}
}
