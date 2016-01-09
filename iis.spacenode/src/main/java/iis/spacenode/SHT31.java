package iis.spacenode;

import java.nio.ByteBuffer;

import jdk.dio.DeviceManager;
import jdk.dio.i2cbus.I2CDevice;
import jdk.dio.i2cbus.I2CDeviceConfig;

public class SHT31 {
	// ----------------------------------------------------------------
	//
	//   Constants
	//
	// ----------------------------------------------------------------
	public static final int SHT31_DEFAULT_ADDR 		   	= 0x41   & 0xFFFF;
	public static final int SHT31_MEAS_HIGHREP_STRETCH 	= 0x2C06 & 0xFFFF;
	public static final int SHT31_MEAS_MEDREP_STRETCH  	= 0x2C0D & 0xFFFF;
	public static final int SHT31_MEAS_LOWREP_STRETCH  	= 0x2C10 & 0xFFFF;
	public static final int SHT31_MEAS_HIGHREP         	= 0x2400 & 0xFFFF;
	public static final int SHT31_MEAS_MEDREP          	= 0x240B & 0xFFFF;
	public static final int SHT31_MEAS_LOWREP          	= 0x2416 & 0xFFFF;
	public static final int SHT31_READSTATUS           	= 0xF32D & 0xFFFF;
	public static final int SHT31_CLEARSTATUS          	= 0x3041 & 0xFFFF;
	public static final int SHT31_SOFTRESET            	= 0x30A2 & 0xFFFF;
	public static final int SHT31_HEATEREN             	= 0x306D & 0xFFFF;
	public static final int SHT31_HEATERDIS            	= 0x3066 & 0xFFFF;


	// ----------------------------------------------------------------
	//
	//   Variables
	//
	// ----------------------------------------------------------------
	private int address;
	private I2CDevice device;
	private double temperature;
	private double humidity;


	// ----------------------------------------------------------------
	//
	//   Constructors
	//
	// ----------------------------------------------------------------
	public SHT31(int addr) throws Exception {
		this.address = addr;
		I2CDeviceConfig config = new I2CDeviceConfig(
				I2CDeviceConfig.DEFAULT,           	//I2C bus index
				this.address,          				//I2C device address
				I2CDeviceConfig.ADDR_SIZE_7,       	//Number of bits in the address
				1000000       						//I2C Clock Frequency (Fast Mode)
				);
		device = (I2CDevice) DeviceManager.open(I2CDevice.class, config);
		reset();
	}

	public SHT31() throws Exception {
		this(SHT31_DEFAULT_ADDR);
	}


	// ----------------------------------------------------------------
	//
	//   Getters & Setters
	//
	// ----------------------------------------------------------------
	public int getAddress() {
		return address;
	}

	public double getTemperature() throws Exception {
		readTempHum();
		return temperature;
	}

	public double getHumidity() throws Exception {
		readTempHum();
		return humidity;
	}


	// ----------------------------------------------------------------
	//
	//   Methods
	//
	// ----------------------------------------------------------------
	private void writeCommand(int cmd) throws Exception {
		device.begin();
		device.write(cmd >> 8);				//MSB
		device.write(cmd & 0xFFFFFF);		//LSB
		device.end();
	}

	public void reset() throws Exception {
		writeCommand(SHT31_SOFTRESET);
		Thread.sleep(10);
	}

	public void heater(boolean h) throws Exception {
		if (h)
			writeCommand(SHT31_HEATEREN);
		else
			writeCommand(SHT31_HEATERDIS);
	}

	private void readTempHum() throws Exception {
		ByteBuffer readBuffer = ByteBuffer.allocate(6);
		ByteBuffer ST = ByteBuffer.allocate(2);
		ByteBuffer SRH = ByteBuffer.allocate(2);
		double sTemp, sHum;

		writeCommand(SHT31_MEAS_HIGHREP);
		Thread.sleep(500);
		device.read(readBuffer);

		//Temperature
		ST.put(0, readBuffer.get(0));
		ST.put(1, readBuffer.get(1));
		sTemp = (int)(ST.getShort() & 0xFFFF);
		sTemp *= 175;
		sTemp /= 0xFFFF;
		sTemp = -45 + sTemp;
		temperature = sTemp;

		//Humidity
		SRH.put(0, readBuffer.get(3));
		SRH.put(1, readBuffer.get(4));
		sHum = (int)(SRH.getShort() & 0xFFFF);
		sHum *= 100;
		sHum /= 0xFFFF;
		humidity = sHum;
	}

}
