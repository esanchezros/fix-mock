/*
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 * Copyright 2025-2026 the original author or authors.
 */
package io.allune.fixmock.server.http;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "fixmock")
class FixMockServerProperties {
	private String beginString = "FIX.4.4";
	private String senderCompId = "BROKER";
	private String targetCompId = "CLIENT";
	private int acceptorPort = 9899;
	private boolean useDataDictionary = false;
	private String dictionaryPath;
	private int heartBtIntSeconds = 30;
	private boolean resetOnLogon = true;
	private boolean resetOnDisconnect = true;
	private boolean resetOnLogout = true;
	private String recordMode = "OFF";

	public String getBeginString() { return beginString; }
	public void setBeginString(String beginString) { this.beginString = beginString; }
	public String getSenderCompId() { return senderCompId; }
	public void setSenderCompId(String senderCompId) { this.senderCompId = senderCompId; }
	public String getTargetCompId() { return targetCompId; }
	public void setTargetCompId(String targetCompId) { this.targetCompId = targetCompId; }
	public int getAcceptorPort() { return acceptorPort; }
	public void setAcceptorPort(int acceptorPort) { this.acceptorPort = acceptorPort; }
	public boolean isUseDataDictionary() { return useDataDictionary; }
	public void setUseDataDictionary(boolean useDataDictionary) { this.useDataDictionary = useDataDictionary; }
	public String getDictionaryPath() { return dictionaryPath; }
	public void setDictionaryPath(String dictionaryPath) { this.dictionaryPath = dictionaryPath; }
	public int getHeartBtIntSeconds() { return heartBtIntSeconds; }
	public void setHeartBtIntSeconds(int heartBtIntSeconds) { this.heartBtIntSeconds = heartBtIntSeconds; }
	public boolean isResetOnLogon() { return resetOnLogon; }
	public void setResetOnLogon(boolean resetOnLogon) { this.resetOnLogon = resetOnLogon; }
	public boolean isResetOnDisconnect() { return resetOnDisconnect; }
	public void setResetOnDisconnect(boolean resetOnDisconnect) { this.resetOnDisconnect = resetOnDisconnect; }
	public boolean isResetOnLogout() { return resetOnLogout; }
	public void setResetOnLogout(boolean resetOnLogout) { this.resetOnLogout = resetOnLogout; }
	public String getRecordMode() { return recordMode; }
	public void setRecordMode(String recordMode) { this.recordMode = recordMode; }
}
