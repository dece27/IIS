package iis.spacenode;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import jdk.dio.DeviceManager;
import jdk.dio.i2cbus.I2CDevice;
import jdk.dio.i2cbus.I2CDeviceConfig;

public class BMP280 {
	// ----------------------------------------------------------------
	//
	//   Constants
	//
	// ----------------------------------------------------------------
	public static final int BMP280_ADDRESS					   = 0x77 & 0xFFFF;
	public static final int BMP280_REGISTER_DIG_T1             = 0x88 & 0xFFFF;
	public static final int BMP280_REGISTER_DIG_T2             = 0x8A & 0xFFFF;
	public static final int BMP280_REGISTER_DIG_T3             = 0x8C & 0xFFFF;
	public static final int BMP280_REGISTER_DIG_P1             = 0x8E & 0xFFFF;
	public static final int BMP280_REGISTER_DIG_P2             = 0x90 & 0xFFFF;
	public static final int BMP280_REGISTER_DIG_P3             = 0x92 & 0xFFFF;
	public static final int BMP280_REGISTER_DIG_P4             = 0x94 & 0xFFFF;
	public static final int BMP280_REGISTER_DIG_P5             = 0x96 & 0xFFFF;
	public static final int BMP280_REGISTER_DIG_P6             = 0x98 & 0xFFFF;
	public static final int BMP280_REGISTER_DIG_P7             = 0x9A & 0xFFFF;
	public static final int BMP280_REGISTER_DIG_P8             = 0x9C & 0xFFFF;
	public static final int BMP280_REGISTER_DIG_P9             = 0x9E & 0xFFFF;
	public static final int BMP280_REGISTER_CHIPID             = 0xD0 & 0xFFFF;
	public static final int BMP280_REGISTER_VERSION            = 0xD1 & 0xFFFF;
	public static final int BMP280_REGISTER_SOFTRESET          = 0xE0 & 0xFFFF;
	public static final int BMP280_REGISTER_CAL26              = 0xE1 & 0xFFFF;  // R calibration stored in 0xE1-0xF0
	public static final int BMP280_REGISTER_CONTROL            = 0xF4 & 0xFFFF;
	public static final int BMP280_REGISTER_CONFIG             = 0xF5 & 0xFFFF;
	public static final int BMP280_REGISTER_PRESSUREDATA       = 0xF7 & 0xFFFF;
	public static final int BMP280_REGISTER_TEMPDATA           = 0xFA & 0xFFFF;


	// ----------------------------------------------------------------
	//
	//   Variables
	//
	// ----------------------------------------------------------------
	private int dig_T1;
	private int dig_T2;
	private int dig_T3;
	private int dig_P1;
	private int dig_P2;
	private int dig_P3;
	private int dig_P4;
	private int dig_P5;
	private int dig_P6;
	private int dig_P7;
	private int dig_P8;
	private int dig_P9;

	private int address;
	private I2CDevice device;
	private double temperature;
	private double pressure;
	private double altitude;
	private int t_fine;
	private double seaLevelhPa;


	// ----------------------------------------------------------------
	//
	//   Constructors
	//
	// ----------------------------------------------------------------
	public BMP280(int addr) throws Exception {
		ByteBuffer chipIdBuffer = ByteBuffer.allocate(1);
		this.address = addr;
		I2CDeviceConfig config = new I2CDeviceConfig(
				I2CDeviceConfig.DEFAULT,           	//I2C bus index
				this.address,          				//I2C device address
				I2CDeviceConfig.ADDR_SIZE_7,       	//Number of bits in the address
				1000000       						//I2C Clock Frequency (Fast Mode)
				);
		device = (I2CDevice) DeviceManager.open(I2CDevice.class, config);
		device.read(BMP280_REGISTER_CHIPID, 1, chipIdBuffer);
		if(chipIdBuffer.get() != 0x58)
			throw new InstantiationException("Wrong chip ID");
		readCoefficients();
		device.write(BMP280_REGISTER_CONTROL, 1, ByteBuffer.allocate(1).put((byte) 0x3F));
	}

	public BMP280() throws Exception {
		this(BMP280_ADDRESS);
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
		readTemperature();
		return temperature;
	}

	public double getPressure() throws Exception {
		readPressure();
		return pressure;
	}
	
	public double getSeaLevelhPa() {
		return seaLevelhPa;
	}
	
	public void setSeaLevelhPa(double seaLevelhPa) {
		this.seaLevelhPa = seaLevelhPa;
	}
	
	public double getAltitude() throws Exception {
		readAltitude();
		return altitude;
	}


	// ----------------------------------------------------------------
	//
	//   Methods
	//
	// ----------------------------------------------------------------
	private void readCoefficients() throws Exception {
		ByteBuffer readCoeff = ByteBuffer.allocate(2);
		readCoeff.order(ByteOrder.LITTLE_ENDIAN);
		device.read(BMP280_REGISTER_DIG_T1, 2, readCoeff);
		dig_T1 = readCoeff.getShort() & 0xFFFF;
		device.read(BMP280_REGISTER_DIG_T2, 2, readCoeff);
		dig_T2 = readCoeff.getShort();
		device.read(BMP280_REGISTER_DIG_T3, 2, readCoeff);
		dig_T3 = readCoeff.getShort();
		device.read(BMP280_REGISTER_DIG_P1, 2, readCoeff);
		dig_P1 = readCoeff.getShort() & 0xFFFF;
		device.read(BMP280_REGISTER_DIG_P2, 2, readCoeff);
		dig_P2 = readCoeff.getShort();
		device.read(BMP280_REGISTER_DIG_P3, 2, readCoeff);
		dig_P3 = readCoeff.getShort();
		device.read(BMP280_REGISTER_DIG_P4, 2, readCoeff);
		dig_P4 = readCoeff.getShort();
		device.read(BMP280_REGISTER_DIG_P5, 2, readCoeff);
		dig_P5 = readCoeff.getShort();
		device.read(BMP280_REGISTER_DIG_P6, 2, readCoeff);
		dig_P6 = readCoeff.getShort();
		device.read(BMP280_REGISTER_DIG_P7, 2, readCoeff);
		dig_P7 = readCoeff.getShort();
		device.read(BMP280_REGISTER_DIG_P8, 2, readCoeff);
		dig_P8 = readCoeff.getShort();
		device.read(BMP280_REGISTER_DIG_P9, 2, readCoeff);
		dig_P9 = readCoeff.getShort();
	}

	private void readTemperature() throws Exception {
		ByteBuffer adc_T2_Buffer = ByteBuffer.allocate(2);
		ByteBuffer adc_T1_Buffer = ByteBuffer.allocate(1);
		int var1, var2, adc_T;
		float T;

		device.read(BMP280_REGISTER_TEMPDATA, 2, adc_T2_Buffer);
		adc_T = adc_T2_Buffer.getShort() & 0xFFFF;
		adc_T <<= 8;
		device.read(BMP280_REGISTER_TEMPDATA + 2, 1, adc_T1_Buffer);
		adc_T |= adc_T1_Buffer.get();
		adc_T >>= 4;

		var1  = ((((adc_T>>3) - (dig_T1 <<1))) * (dig_T2)) >> 11;
		var2  = (((((adc_T>>4) - (dig_T1)) * ((adc_T>>4) - (dig_T1))) >> 12) * (dig_T3)) >> 14;
		t_fine = var1 + var2;
		T  = (t_fine * 5 + 128) >> 8;
		temperature = T / 100;
	}

	private void readPressure() throws Exception {
		ByteBuffer adc_P2_Buffer = ByteBuffer.allocate(2);
		ByteBuffer adc_P1_Buffer = ByteBuffer.allocate(1);
		long var1, var2, p;
		int adc_P;

		device.read(BMP280_REGISTER_PRESSUREDATA, 2, adc_P2_Buffer);
		adc_P = adc_P2_Buffer.getShort() & 0xFFFF;
		adc_P <<= 8;
		device.read(BMP280_REGISTER_PRESSUREDATA + 2, 1, adc_P1_Buffer);
		adc_P |= adc_P1_Buffer.get();
		adc_P >>= 4;

		var1 = ((long)t_fine) - 128000;
		var2 = var1 * var1 * (long)dig_P6;
		var2 = var2 + ((var1*(long)dig_P5)<<17);
		var2 = var2 + (((long)dig_P4)<<35);
		var1 = ((var1 * var1 * (long)dig_P3)>>8) + ((var1 * (long)dig_P2)<<12);
		var1 = (((((long)1)<<47)+var1))*((long)dig_P1)>>33;

		if (var1 == 0) {
			pressure = 0;  // avoid exception caused by division by zero
		}
		p = 1048576 - adc_P;
		p = (((p<<31) - var2)*3125) / var1;
		var1 = (((long)dig_P9) * (p>>13) * (p>>13)) >> 25;
		var2 = (((long)dig_P8) * p) >> 19;
		p = ((p + var1 + var2) >> 8) + (((long)dig_P7)<<4);
		pressure =  (double)p/256;
	}

	private void readAltitude() throws Exception {
		double tempPressure;
		
		readPressure();
		tempPressure = pressure;
		tempPressure /= 100;
		altitude = 44330 * (1.0 - Math.pow(tempPressure / seaLevelhPa, 0.1903));
	}
	
	public void calcSeaLevelhPa(double altitude) throws Exception {
		readTemperature();
		readPressure();
		seaLevelhPa = pressure * Math.pow((1 - ((0.0065 * altitude) / (temperature + 0.0065 * altitude + 273.15))), -5.257);
	}

}
