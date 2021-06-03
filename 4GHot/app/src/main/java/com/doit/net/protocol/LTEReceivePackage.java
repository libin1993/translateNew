package com.doit.net.protocol;

import java.util.Arrays;

/**
 * Created by Zxc on 2018/10/18.
 */
public class LTEReceivePackage {
	private String host;
	private short packageLength;//长 2字节
	private short packageCheckNum;//校验位2字节
	private byte[]  packageSequence;//包的序号2字节
	private byte[]  packageSessionID;//对话ID2字节
	private byte packageEquipType;//主类型1字节
	private byte packageReserve;//预留1字节
	private byte packageMainType;//主类型1字节
	private byte packageSubType;//子类型1字节
	private byte[] byteSubContent;//子协议内容
	public short getPackageLength() 
	{
		return packageLength;
	}
	public void setPackageLength(short packageLength) {
		this.packageLength = packageLength;
	}
	public short getPackageCheckNum() {
		return packageCheckNum;
	}
	public void setPackageCheckNum(short packageCheckNum) {
		this.packageCheckNum = packageCheckNum;
	}
	public byte[]  getPackageSequence() {
		return packageSequence;
	}
	public void setPackageSequence(byte[]  packageSequence) {
		this.packageSequence = packageSequence;
	}
	public byte[]  getPackageSessionID() {
		return packageSessionID;
	}
	public void setPackageSessionID(byte[]  packageSessionID) {
		this.packageSessionID = packageSessionID;
	}
	public byte getPackageEquipType() {
		return packageEquipType;
	}
	public void setPackageEquipType(byte packageEquipType) {
		this.packageEquipType = packageEquipType;
	}
	public byte getPackageReserve() {
		return packageReserve;
	}
	public void setPackageReserve(byte packageReserve) {
		this.packageReserve = packageReserve;
	}
	public byte getPackageMainType() {
		return packageMainType;
	}
	public void setPackageMainType(byte packageMainType) {
		this.packageMainType = packageMainType;
	}
	public byte getPackageSubType() {
		return packageSubType;
	}
	public void setPackageSubType(byte packageSubType) {
		this.packageSubType = packageSubType;
	}
	public byte[] getByteSubContent() {
		return byteSubContent;
	}
	public void setByteSubContent(byte[] byteSubContent) {
		this.byteSubContent = byteSubContent;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}


	@Override
	public String toString() {
		return "LTEReceivePackage{" +
				"host='" + host + '\'' +
				", packageLength=" + packageLength +
				", packageCheckNum=" + packageCheckNum +
				", packageSequence=" + packageSequence +
				", packageSessionID=" + packageSessionID +
				", packageEquipType=" + packageEquipType +
				", packageReserve=" + packageReserve +
				", packageMainType=" + packageMainType +
				", packageSubType=" + packageSubType +
				", byteSubContent=" + Arrays.toString(byteSubContent) +
				'}';
	}
}
