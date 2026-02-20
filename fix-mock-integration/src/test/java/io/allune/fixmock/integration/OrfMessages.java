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
package io.allune.fixmock.integration;

import quickfix.Group;
import quickfix.Message;

/**
 * Factory methods to build FINRA ORF-specific FIX 4.4 messages.
 *
 * <p>Uses raw {@link Message} rather than typed QuickFIX/J classes because ORF extends
 * FIX 4.4 with custom FINRA tags in the 22xxx range that are not in the standard data dictionary.
 * This is how real ORF clients construct messages.
 *
 * <h3>Key FINRA ORF FIX Tags</h3>
 * <ul>
 *   <li>571 — TradeReportID</li>
 *   <li>487 — TradeReportTransType (0=New, 1=Cancel)</li>
 *   <li>856 — TradeReportType (0=Submit, 6=TAS)</li>
 *   <li>55  — Symbol</li>
 *   <li>32  — LastQty</li>
 *   <li>31  — LastPx</li>
 *   <li>552 — NoSides (TrdCapRptSideGrp)</li>
 *   <li>54  — Side (within group)</li>
 *   <li>1   — Account (within group)</li>
 *   <li>453 — NoPartyIDs (nested within each side)</li>
 *   <li>448 — PartyID</li>
 *   <li>447 — PartyIDSource</li>
 *   <li>452 — PartyRole</li>
 *   <li>570 — PreviouslyReported</li>
 *   <li>1003 — TradeID (FINRA-assigned)</li>
 *   <li>22011 — ControlDate (FINRA custom)</li>
 *   <li>22012 — ControlNumber (FINRA custom)</li>
 *   <li>22030 — ReportingObligation (FINRA custom)</li>
 *   <li>22038 — MessageEventSource (FINRA custom: OREN, ORAL, ORCX)</li>
 * </ul>
 */
final class OrfMessages {

	// FINRA custom tags (22xxx range)
	static final int TAG_CONTROL_DATE = 22011;
	static final int TAG_CONTROL_NUMBER = 22012;
	static final int TAG_REPORTING_OBLIGATION = 22030;
	static final int TAG_MESSAGE_EVENT_SOURCE = 22038;

	// Standard FIX tags used in ORF
	static final int TAG_TRADE_REPORT_ID = 571;
	static final int TAG_TRADE_REPORT_TRANS_TYPE = 487;
	static final int TAG_TRADE_REPORT_TYPE = 856;
	static final int TAG_SYMBOL = 55;
	static final int TAG_LAST_QTY = 32;
	static final int TAG_LAST_PX = 31;
	static final int TAG_NO_SIDES = 552;
	static final int TAG_SIDE = 54;
	static final int TAG_ACCOUNT = 1;
	static final int TAG_NO_PARTY_IDS = 453;
	static final int TAG_PARTY_ID = 448;
	static final int TAG_PARTY_ID_SOURCE = 447;
	static final int TAG_PARTY_ROLE = 452;
	static final int TAG_PREVIOUSLY_REPORTED = 570;
	static final int TAG_TRADE_ID = 1003;
	static final int TAG_TRADE_REPORT_REJ_REASON = 751;
	static final int TAG_TEXT = 58;

	// Message event sources
	static final String EVENT_SOURCE_OREN = "OREN"; // ORF Entry acknowledgement
	static final String EVENT_SOURCE_ORAL = "ORAL"; // ORF Alteration acknowledgement
	static final String EVENT_SOURCE_ORCX = "ORCX"; // ORF Cancel acknowledgement

	private OrfMessages() {}

	/**
	 * Builds a new Trade Capture Report (MsgType=AE) for submitting a trade.
	 *
	 * @param tradeReportId unique trade report identifier
	 * @param symbol        ticker symbol
	 * @param lastQty       trade quantity
	 * @param lastPx        trade price
	 * @return the raw FIX message
	 */
	static Message newTradeReport(String tradeReportId, String symbol, int lastQty, double lastPx) {
		Message tcr = new Message();
		tcr.getHeader().setString(35, "AE"); // MsgType = TradeCaptureReport
		tcr.setString(TAG_TRADE_REPORT_ID, tradeReportId);
		tcr.setString(TAG_TRADE_REPORT_TRANS_TYPE, "0"); // New
		tcr.setString(TAG_TRADE_REPORT_TYPE, "0");       // Submit
		tcr.setString(TAG_SYMBOL, symbol);
		tcr.setInt(TAG_LAST_QTY, lastQty);
		tcr.setDouble(TAG_LAST_PX, lastPx);
		tcr.setString(TAG_PREVIOUSLY_REPORTED, "N");
		return tcr;
	}

	/**
	 * Builds a new Trade Capture Report with FINRA custom extension tags.
	 */
	static Message newTradeReportWithFinraTags(String tradeReportId, String symbol,
											   int lastQty, double lastPx) {
		Message tcr = newTradeReport(tradeReportId, symbol, lastQty, lastPx);
		tcr.setString(TAG_REPORTING_OBLIGATION, "Y");
		return tcr;
	}

	/**
	 * Builds a Trade Capture Report with a TrdCapRptSideGrp (tag 552) containing two sides,
	 * each with nested Parties groups (tag 453).
	 */
	static Message newTradeReportWithSidesAndParties(String tradeReportId, String symbol,
													 int lastQty, double lastPx,
													 String reportingMpid, String contraMpid) {
		Message tcr = newTradeReportWithFinraTags(tradeReportId, symbol, lastQty, lastPx);

		// Side 1: Buy (reporting party)
		Group side1 = new Group(TAG_NO_SIDES, TAG_SIDE);
		side1.setChar(TAG_SIDE, '1'); // Buy
		side1.setString(TAG_ACCOUNT, "ACCT-RPT");

		// Nested Parties in side 1
		Group party1 = new Group(TAG_NO_PARTY_IDS, TAG_PARTY_ID);
		party1.setString(TAG_PARTY_ID, reportingMpid);
		party1.setChar(TAG_PARTY_ID_SOURCE, 'D'); // Proprietary
		party1.setInt(TAG_PARTY_ROLE, 1);          // Executing Firm
		side1.addGroup(party1);

		tcr.addGroup(side1);

		// Side 2: Sell (contra party)
		Group side2 = new Group(TAG_NO_SIDES, TAG_SIDE);
		side2.setChar(TAG_SIDE, '2'); // Sell
		side2.setString(TAG_ACCOUNT, "ACCT-CTR");

		// Nested Parties in side 2
		Group party2 = new Group(TAG_NO_PARTY_IDS, TAG_PARTY_ID);
		party2.setString(TAG_PARTY_ID, contraMpid);
		party2.setChar(TAG_PARTY_ID_SOURCE, 'D');
		party2.setInt(TAG_PARTY_ROLE, 17);         // Contra Firm
		side2.addGroup(party2);

		tcr.addGroup(side2);

		return tcr;
	}

	/**
	 * Builds a Trade Capture Report with a single flat Parties group (no nested sides).
	 * Useful for testing flat repeating group matching.
	 */
	static Message newTradeReportWithFlatParties(String tradeReportId, String symbol,
												 int lastQty, double lastPx,
												 String... partyIds) {
		Message tcr = newTradeReportWithFinraTags(tradeReportId, symbol, lastQty, lastPx);

		for (String partyId : partyIds) {
			Group party = new Group(TAG_NO_PARTY_IDS, TAG_PARTY_ID);
			party.setString(TAG_PARTY_ID, partyId);
			party.setChar(TAG_PARTY_ID_SOURCE, 'D');
			party.setInt(TAG_PARTY_ROLE, 1);
			tcr.addGroup(party);
		}

		return tcr;
	}

	/**
	 * Builds a cancel Trade Capture Report (TradeReportTransType=1).
	 */
	static Message cancelTradeReport(String tradeReportId, String symbol, int lastQty, double lastPx) {
		Message tcr = newTradeReport(tradeReportId, symbol, lastQty, lastPx);
		tcr.setString(TAG_TRADE_REPORT_TRANS_TYPE, "1"); // Cancel
		return tcr;
	}

	/**
	 * Builds an ORF acknowledgement response (MsgType=AE with MessageEventSource).
	 *
	 * @param tradeReportId the original trade report ID being acknowledged
	 * @param tradeId       FINRA-assigned trade ID
	 * @param eventSource   OREN, ORAL, or ORCX
	 * @param symbol        ticker symbol
	 * @return the acknowledgement message
	 */
	static Message orfAcknowledgement(String tradeReportId, String tradeId,
									  String eventSource, String symbol) {
		Message ack = new Message();
		ack.getHeader().setString(35, "AE");
		ack.setString(TAG_TRADE_REPORT_ID, tradeReportId);
		ack.setString(TAG_TRADE_ID, tradeId);
		ack.setString(TAG_SYMBOL, symbol);
		ack.setString(TAG_MESSAGE_EVENT_SOURCE, eventSource);
		ack.setString(TAG_CONTROL_DATE, "20260217");
		ack.setString(TAG_CONTROL_NUMBER, "00001");
		return ack;
	}

	/**
	 * Builds a Trade Capture Report Ack/Reject (MsgType=AR).
	 *
	 * @param tradeReportId the report being rejected
	 * @param rejectReason  reject reason code (751)
	 * @param text          human-readable reject text (58)
	 * @return the reject message
	 */
	static Message tradeReportReject(String tradeReportId, int rejectReason, String text) {
		Message reject = new Message();
		reject.getHeader().setString(35, "AR"); // TradeCaptureReportAck
		reject.setString(TAG_TRADE_REPORT_ID, tradeReportId);
		reject.setInt(TAG_TRADE_REPORT_REJ_REASON, rejectReason);
		reject.setString(TAG_TEXT, text);
		return reject;
	}

	/**
	 * Builds a supplier for an ORF acknowledgement (for use with respondWithTemplate).
	 */
	static Message freshOrfAck(String eventSource) {
		Message ack = new Message();
		ack.getHeader().setString(35, "AE");
		ack.setString(TAG_MESSAGE_EVENT_SOURCE, eventSource);
		ack.setString(TAG_TRADE_ID, "TRD-" + System.nanoTime());
		ack.setString(TAG_CONTROL_DATE, "20260217");
		ack.setString(TAG_CONTROL_NUMBER, String.format("%05d", System.nanoTime() % 99999));
		return ack;
	}

	/**
	 * Builds a large Trade Capture Report with many tags for edge case testing.
	 */
	static Message largeTradeReport(String tradeReportId, String symbol) {
		Message tcr = newTradeReportWithFinraTags(tradeReportId, symbol, 5000, 199.99);
		// Add extra tags to bulk up the message
		tcr.setString(58, "Large trade report for stress testing");
		tcr.setString(TAG_CONTROL_DATE, "20260217");
		tcr.setString(TAG_CONTROL_NUMBER, "99999");
		tcr.setString(570, "N"); // PreviouslyReported
		tcr.setInt(822, 1);      // TradeHandlingInstr
		tcr.setString(818, "ABCDEF"); // SecondaryTradeReportID
		tcr.setString(820, "TRD-REF-001"); // TradeLinkID
		tcr.setDouble(669, 200.00); // OfferPx (for illustration)
		tcr.setDouble(668, 199.50); // BidPx (for illustration)

		// Add multiple parties
		for (int i = 0; i < 5; i++) {
			Group party = new Group(TAG_NO_PARTY_IDS, TAG_PARTY_ID);
			party.setString(TAG_PARTY_ID, "PARTY-" + i);
			party.setChar(TAG_PARTY_ID_SOURCE, 'D');
			party.setInt(TAG_PARTY_ROLE, i + 1);
			tcr.addGroup(party);
		}

		return tcr;
	}
}
